package cn.erindax.bjmapedit.client.screen;

import cn.erindax.bjmapedit.client.EditorAccess;
import cn.erindax.bjmapedit.client.widget.InvPicker;
import cn.erindax.bjmapedit.client.widget.ItemModels;
import cn.erindax.bjmapedit.client.widget.ResourcePacks;
import cn.erindax.bjmapedit.client.widget.UiTheme;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class ItemModelScreen extends Screen {
	private static final String K = "screen.bj_mapedit.";
	private static final int PAD = 10;
	private static final int FOOTER_H = 30;
	private static final int TOP_H = 22;
	private static final int ROW_H = 24;
	private static final int ROOT_H = 36;
	private static final int CELL = 24;
	private static final int OK = 0xFF7EE787;
	private static final int WARN = 0xFFE3B341;
	private static final int ERR = 0xFFFF7B72;
	private static final int LINE = 0xFF3D444D;
	private static final int EDGE = 0xFF2B2C2D;
	private static final int NODE_HOT = 0xFF21262D;
	private static int thumbSeq;

	private static final class Variant {
		int cmd;
		int savedCmd;
		boolean saved;
		boolean changed;
		String model = "";
		String texId;
		Path png;
		Path json;
		Path shown;
	}

	private record Thumb(Identifier id, int w, int h) {}

	private final Screen parent;
	private final String preferredPack;
	private final InvPicker picker = new InvPicker();
	private final List<Variant> variants = new ArrayList<>();
	private final List<Integer> removed = new ArrayList<>();
	private final Map<Path, Thumb> thumbs = new HashMap<>();
	private List<String> packs = new ArrayList<>();
	private List<ItemModels.Texture> choices = new ArrayList<>();
	private int packIdx;
	private ItemStack item = ItemStack.EMPTY;
	private int sel = -1;
	private int scroll;
	private int chooserScroll;
	private boolean opened;
	private boolean syncing;
	private boolean chooser;
	private String armed = "";
	private Component status = Component.empty();
	private int statusColor = OK;

	private EditBox cmdField;
	private Button packMinus;
	private Button packPlus;
	private Button filesBtn;
	private Button pickBtn;
	private Button heldBtn;
	private Button chooseBtn;
	private Button giveBtn;
	private Button backBtn;
	private Button applyBtn;

	private int leftPos;
	private int topPos;
	private int panelW;
	private int panelH;
	private int bodyTop;
	private int bodyBottom;
	private int canvasX;
	private int canvasW;
	private int sideX;
	private int trunkX;
	private int rootW;
	private int sideW;
	private int rowsTop;
	private int viewRows;
	private int previewSize;

	public ItemModelScreen(Screen parent, String preferredPack) {
		super(Component.translatable(K + "rp_title"));
		this.parent = parent;
		this.preferredPack = preferredPack;
	}

	@Override
	protected void init() {
		boolean first = !opened;
		opened = true;
		String keep = first ? preferredPack : currentPack();
		packs = ResourcePacks.folderPacks();
		int kept = keep == null ? -1 : packs.indexOf(keep);
		packIdx = kept >= 0 ? kept : defaultPackIdx();
		layout();

		packMinus = this.addRenderableWidget(Button.builder(Component.literal("<"), b -> cyclePack(-1)).bounds(0, 0, 16, 16).build());
		packPlus = this.addRenderableWidget(Button.builder(Component.literal(">"), b -> cyclePack(1)).bounds(0, 0, 16, 16).build());
		filesBtn = this.addRenderableWidget(Button.builder(Component.translatable(K + "rp_files_btn"), b -> openFiles()).bounds(0, 0, 40, 16).build());
		pickBtn = this.addRenderableWidget(Button.builder(Component.translatable(K + "rp_pick_inv"), b -> {
			if (discardOk("item")) picker.open();
		}).bounds(0, 0, rootW, 16).build());
		heldBtn = this.addRenderableWidget(Button.builder(Component.translatable(K + "rp_use_held"), b -> useHeld()).bounds(0, 0, rootW, 16).build());
		cmdField = new EditBox(font, 0, 0, 46, 16, Component.empty());
		cmdField.setFilter(s -> s.matches("-?\\d{0,10}"));
		cmdField.setResponder(this::cmdTyped);
		this.addRenderableWidget(cmdField);
		chooseBtn = this.addRenderableWidget(Button.builder(Component.translatable(K + "rp_choose"), b -> openChooser()).bounds(0, 0, 40, 16).build());
		giveBtn = this.addRenderableWidget(Button.builder(Component.translatable(K + "rp_give"), b -> give()).bounds(0, 0, 40, 16).build());
		backBtn = this.addRenderableWidget(Button.builder(Component.translatable(K + "rp_back"), b -> onClose()).bounds(0, 0, 60, 20).build());
		applyBtn = this.addRenderableWidget(Button.builder(Component.translatable(K + "rp_apply"), b -> apply()).bounds(0, 0, 60, 20).build());
		placeWidgets();
		if (first) {
			var player = Minecraft.getInstance().player;
			if (player != null && !player.getMainHandItem().isEmpty()) setItem(player.getMainHandItem());
		}
		syncSide();
	}

	private void layout() {
		panelW = Mth.clamp(this.width - 16, 300, 560);
		panelH = Mth.clamp(this.height - 16, 200, 300);
		leftPos = (this.width - panelW) / 2;
		topPos = Math.max(4, (this.height - panelH) / 2);
		bodyTop = topPos + UiTheme.HEADER_H + 6 + TOP_H;
		bodyBottom = topPos + panelH - FOOTER_H - 6;
		sideW = Mth.clamp(panelW * 28 / 100, 88, 148);
		rootW = Mth.clamp(panelW / 5, 64, 100);
		sideX = leftPos + panelW - PAD - sideW;
		canvasX = leftPos + PAD;
		canvasW = sideX - 8 - canvasX;
		trunkX = canvasX + rootW + 8;
		rowsTop = bodyTop + (ROOT_H - ROW_H) / 2;
		viewRows = Math.max(1, (bodyBottom - rowsTop) / ROW_H);
		previewSize = Mth.clamp(Math.min(bodyBottom - bodyTop - 66, sideW), 40, 148);
		scroll = Mth.clamp(scroll, 0, maxScroll());
	}

	private void placeWidgets() {
		int topY = topPos + UiTheme.HEADER_H + 6;
		int filesW = font.width(filesBtn.getMessage()) + 14;
		filesBtn.setPosition(leftPos + panelW - PAD - filesW, topY);
		filesBtn.setWidth(filesW);
		int packX = leftPos + PAD + font.width(I18n.get(K + "rp_pack")) + 8;
		int nameW = Mth.clamp(filesBtn.getX() - 8 - packX - 40, 60, 180);
		packMinus.setPosition(packX, topY);
		packPlus.setPosition(packX + 20 + nameW + 4, topY);
		pickBtn.setPosition(canvasX, bodyTop + ROOT_H + 4);
		heldBtn.setPosition(canvasX, bodyTop + ROOT_H + 22);
		int condY = bodyTop + 12 + previewSize + 4;
		cmdField.setPosition(sideX + sideW - 46, condY + 12);
		int giveW = font.width(giveBtn.getMessage()) + 12;
		giveBtn.setPosition(sideX + sideW - giveW, condY + 32);
		giveBtn.setWidth(giveW);
		chooseBtn.setPosition(sideX, condY + 32);
		chooseBtn.setWidth(sideW - giveW - 4);
		int footerY = topPos + panelH - FOOTER_H + (FOOTER_H - 20) / 2;
		int backW = Math.max(52, font.width(backBtn.getMessage()) + 22);
		int applyW = Math.max(52, font.width(applyBtn.getMessage()) + 22);
		backBtn.setPosition(leftPos + 8, footerY);
		backBtn.setWidth(backW);
		applyBtn.setPosition(leftPos + panelW - 8 - applyW, footerY);
		applyBtn.setWidth(applyW);
	}

	private String currentPack() {
		return packs.isEmpty() ? null : packs.get(Mth.clamp(packIdx, 0, packs.size() - 1));
	}

	private int defaultPackIdx() {
		List<String> selected = new ArrayList<>(Minecraft.getInstance().getResourcePackRepository().getSelectedIds());
		for (int i = selected.size() - 1; i >= 0; i--) {
			String id = selected.get(i);
			if (!id.startsWith("file/")) continue;
			int idx = packs.indexOf(id.substring("file/".length()));
			if (idx >= 0) return idx;
		}
		return 0;
	}

	private Path packDir(String pack) {
		return ResourcePacks.root().resolve(pack);
	}

	private Variant selected() {
		return sel >= 0 && sel < variants.size() ? variants.get(sel) : null;
	}

	private boolean dirty() {
		if (!removed.isEmpty()) return true;
		for (Variant v : variants) {
			if (!v.saved || v.changed) return true;
		}
		return false;
	}

	private boolean discardOk(String action) {
		if (!dirty() || action.equals(armed)) {
			armed = "";
			return true;
		}
		armed = action;
		setStatus(Component.translatable(K + "rp_unsaved"), WARN);
		return false;
	}

	private void touch(Variant v) {
		v.changed = true;
		armed = "";
	}

	private void setStatus(Component text, int color) {
		status = text;
		statusColor = color;
	}

	private void cyclePack(int delta) {
		if (packs.isEmpty() || !discardOk("pack")) return;
		packIdx = Math.floorMod(packIdx + delta, packs.size());
		dropThumbs();
		reload();
		sel = -1;
		scroll = 0;
		syncSide();
	}

	private void openFiles() {
		if (!discardOk("files")) return;
		Minecraft.getInstance().setScreen(parent instanceof DatapackEditorScreen ? parent : new DatapackEditorScreen(true));
	}

	private void useHeld() {
		var player = Minecraft.getInstance().player;
		if (player == null) return;
		ItemStack held = player.getMainHandItem();
		if (held.isEmpty()) {
			setStatus(Component.translatable(K + "held_empty"), ERR);
			return;
		}
		if (discardOk("item")) setItem(held);
	}

	private void setItem(ItemStack stack) {
		item = stack.copyWithCount(1);
		reload();
		int cmd = ItemModels.cmdOf(stack);
		sel = -1;
		for (int i = 0; i < variants.size(); i++) {
			if (cmd != 0 && variants.get(i).cmd == cmd) sel = i;
		}
		scroll = 0;
		reveal();
		status = Component.empty();
		syncSide();
	}

	private void reload() {
		variants.clear();
		removed.clear();
		armed = "";
		String pack = currentPack();
		if (item.isEmpty() || pack == null) return;
		Path dir = packDir(pack);
		for (ItemModels.Mapping m : ItemModels.read(dir, item.getItem())) {
			Variant v = new Variant();
			v.cmd = m.cmd();
			v.savedCmd = m.cmd();
			v.saved = true;
			v.model = m.model();
			v.shown = ItemModels.flatTexture(dir, m.model());
			variants.add(v);
		}
	}

	private void syncSide() {
		Variant v = selected();
		syncing = true;
		cmdField.setVisible(v != null);
		cmdField.setValue(v == null ? "" : String.valueOf(v.cmd));
		syncing = false;
		chooseBtn.visible = v != null;
		giveBtn.visible = v != null;
	}

	private void cmdTyped(String text) {
		Variant v = selected();
		if (syncing || v == null) return;
		try {
			int cmd = Integer.parseInt(text.trim());
			if (cmd != v.cmd) {
				v.cmd = cmd;
				touch(v);
			}
		} catch (NumberFormatException ignored) {}
	}

	private void select(int index) {
		sel = index;
		reveal();
		syncSide();
	}

	private void reveal() {
		if (sel >= 0) {
			if (sel < scroll) scroll = sel;
			if (sel >= scroll + viewRows) scroll = sel - viewRows + 1;
		}
		scroll = Mth.clamp(scroll, 0, maxScroll());
	}

	private int nextFreeCmd() {
		int max = 0;
		for (Variant v : variants) max = Math.max(max, v.cmd);
		return max < Integer.MAX_VALUE ? max + 1 : max;
	}

	private Variant addVariant() {
		Variant v = new Variant();
		v.cmd = nextFreeCmd();
		variants.add(v);
		armed = "";
		return v;
	}

	private void removeVariant(int index) {
		Variant v = variants.remove(index);
		if (v.saved) removed.add(v.savedCmd);
		armed = "";
		if (sel == index) sel = -1;
		else if (sel > index) sel--;
		reveal();
		syncSide();
	}

	private void usePng(Variant v, Path file) {
		forget(file);
		v.png = file;
		v.json = null;
		v.texId = null;
		v.shown = file;
		touch(v);
	}

	private void useJson(Variant v, Path file, Path texture) {
		if (texture != null) forget(texture);
		v.json = file;
		v.png = texture;
		v.texId = null;
		v.shown = texture;
		touch(v);
	}

	private void openChooser() {
		if (selected() == null) return;
		String pack = currentPack();
		choices = pack == null ? new ArrayList<>() : ItemModels.textures(packDir(pack));
		chooserScroll = 0;
		chooser = true;
	}

	private void give() {
		Variant v = selected();
		if (v == null || item.isEmpty()) return;
		String problem = EditorAccess.giveProblem();
		if (problem != null) {
			setStatus(Component.translatable(problem), ERR);
			return;
		}
		EditorAccess.giveItem(ItemModels.withCmd(item, v.cmd));
		if (!v.saved || v.changed) setStatus(Component.translatable(K + "rp_give_unapplied"), WARN);
	}

	private String nameFor(Variant v, Path dir, String ns, Set<String> taken) {
		String prefix = ns + ":item/";
		if (v.saved && v.model.startsWith(prefix)) {
			Path own = ItemModels.flatTexture(dir, v.model);
			boolean shared = false;
			for (Variant other : variants) {
				if (other == v || !other.saved) continue;
				if (other.model.equals(v.model) || own != null && own.equals(ItemModels.flatTexture(dir, other.model))) shared = true;
			}
			String name = v.model.substring(prefix.length());
			if (!shared && taken.add(name)) return name;
		}
		String base = BuiltInRegistries.ITEM.getKey(item.getItem()).getPath() + "_" + v.cmd;
		String name = base;
		for (int n = 2; ItemModels.taken(dir, ns, name) || !taken.add(name); n++) name = base + "_" + n;
		return name;
	}

	private void apply() {
		armed = "";
		if (item.isEmpty()) {
			setStatus(Component.translatable(K + "rp_need_item"), ERR);
			return;
		}
		Variant current = selected();
		if (current != null && !cmdField.getValue().trim().equals(String.valueOf(current.cmd))) {
			setStatus(Component.translatable(K + "rp_bad_cmd_int"), ERR);
			return;
		}
		Set<Integer> seen = new HashSet<>();
		for (int i = 0; i < variants.size(); i++) {
			Variant v = variants.get(i);
			if (!seen.add(v.cmd)) {
				select(i);
				setStatus(Component.translatable(K + "rp_dup_cmd", v.cmd), ERR);
				return;
			}
			if (!v.saved && v.png == null && v.json == null && v.texId == null) {
				select(i);
				setStatus(Component.translatable(K + "rp_need_texture", v.cmd), ERR);
				return;
			}
		}
		if (!dirty()) {
			setStatus(Component.translatable(K + "rp_no_change"), WARN);
			return;
		}
		try {
			for (Variant v : variants) {
				if (v.png != null) ItemModels.checkPng(v.png);
				if (v.json != null) ItemModels.checkModel(v.json);
			}
			String pack = currentPack();
			if (pack == null) {
				pack = ResourcePacks.createPack("bjmapedit_models");
				packs = ResourcePacks.folderPacks();
				packIdx = Math.max(0, packs.indexOf(pack));
			}
			Path dir = packDir(pack);
			String ns = ResourcePacks.namespace(pack);
			Item target = item.getItem();
			for (int cmd : removed) ItemModels.remove(dir, target, cmd);
			for (Variant v : variants) {
				if (v.saved && v.savedCmd != v.cmd) ItemModels.remove(dir, target, v.savedCmd);
			}
			Set<String> taken = new HashSet<>();
			for (Variant v : variants) {
				if (v.saved && !v.changed) continue;
				String model = v.model;
				if (v.json != null || v.png != null || v.texId != null) {
					String name = nameFor(v, dir, ns, taken);
					if (v.json != null) {
						ItemModels.importModel(dir, ns, name, v.json, v.png);
					} else if (v.png != null) {
						ItemModels.copyTexture(v.png, dir, ns, name);
						ItemModels.writeGenerated(dir, ns, name, ItemModels.baseParent(target), ItemModels.resourceId(ns, name));
					} else {
						ItemModels.writeGenerated(dir, ns, name, ItemModels.baseParent(target), v.texId);
					}
					model = ItemModels.resourceId(ns, name);
				}
				ItemModels.put(dir, target, v.cmd, model);
			}
			int keep = current == null ? 0 : current.cmd;
			dropThumbs();
			reload();
			sel = -1;
			for (int i = 0; i < variants.size(); i++) {
				if (current != null && variants.get(i).cmd == keep) sel = i;
			}
			reveal();
			syncSide();
			setStatus(Component.translatable(K + "rp_applied", pack), OK);
			ResourcePacks.enableAndReload(pack);
		} catch (Exception e) {
			setStatus(Component.translatable(K + "rp_failed", String.valueOf(e.getMessage())), ERR);
		}
	}

	@Override
	public void onFilesDrop(List<Path> paths) {
		if (picker.isOpen()) return;
		List<Path> pngs = new ArrayList<>();
		List<Path> jsons = new ArrayList<>();
		for (Path p : paths) {
			String n = p.getFileName().toString().toLowerCase(Locale.ROOT);
			if (n.endsWith(".png")) pngs.add(p);
			else if (n.endsWith(".json")) jsons.add(p);
		}
		if (pngs.isEmpty() && jsons.isEmpty()) return;
		if (item.isEmpty()) {
			setStatus(Component.translatable(K + "rp_need_item"), ERR);
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		double mx = mc.mouseHandler.xpos() * mc.getWindow().getGuiScaledWidth() / Math.max(1, mc.getWindow().getScreenWidth());
		double my = mc.mouseHandler.ypos() * mc.getWindow().getGuiScaledHeight() / Math.max(1, mc.getWindow().getScreenHeight());
		int row = chooser ? sel : rowAt(mx, my);
		boolean single = jsons.size() <= 1 && pngs.size() <= 1;
		chooser = false;
		if (single && row >= 0 && row < variants.size()) {
			Variant v = variants.get(row);
			if (jsons.isEmpty()) usePng(v, pngs.get(0));
			else useJson(v, jsons.get(0), pngs.isEmpty() ? null : pngs.get(0));
			select(row);
			status = Component.empty();
			return;
		}
		int added = 0;
		if (single && !jsons.isEmpty()) {
			useJson(addVariant(), jsons.get(0), pngs.isEmpty() ? null : pngs.get(0));
			added++;
		} else {
			for (Path p : pngs) {
				usePng(addVariant(), p);
				added++;
			}
			for (Path p : jsons) {
				useJson(addVariant(), p, null);
				added++;
			}
		}
		select(variants.size() - 1);
		setStatus(Component.translatable(K + "rp_added", added), OK);
	}

	private Thumb thumb(Path file) {
		if (file == null) return null;
		if (thumbs.containsKey(file)) return thumbs.get(file);
		Thumb loaded = null;
		try (InputStream in = Files.newInputStream(file)) {
			NativeImage image = NativeImage.read(in);
			Identifier id = Identifier.fromNamespaceAndPath("bj_mapedit", "rp_thumb/" + thumbSeq++);
			Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(() -> "bj_mapedit_thumb", image));
			loaded = new Thumb(id, image.getWidth(), image.getHeight());
		} catch (Exception ignored) {}
		thumbs.put(file, loaded);
		return loaded;
	}

	private void forget(Path file) {
		Thumb old = thumbs.remove(file);
		if (old != null) Minecraft.getInstance().getTextureManager().release(old.id());
	}

	private void dropThumbs() {
		var manager = Minecraft.getInstance().getTextureManager();
		for (Thumb t : thumbs.values()) {
			if (t != null) manager.release(t.id());
		}
		thumbs.clear();
	}

	private boolean drawPng(GuiGraphics g, Path file, int x, int y, int size) {
		Thumb t = thumb(file);
		if (t == null) return false;
		int frame = Math.min(t.w(), t.h());
		g.blit(RenderPipelines.GUI_TEXTURED, t.id(), x, y, 0, 0, size, size, frame, frame, t.w(), t.h());
		return true;
	}

	private void drawItem(GuiGraphics g, ItemStack stack, int x, int y, int size) {
		if (stack.isEmpty()) return;
		float scale = size / 16f;
		g.pose().pushMatrix();
		g.pose().translate(x, y);
		g.pose().scale(scale, scale);
		g.renderItem(stack, 0, 0);
		g.pose().popMatrix();
	}

	private void drawLook(GuiGraphics g, Variant v, int x, int y, int size) {
		if (v == null) {
			drawItem(g, ItemModels.withoutCmd(item), x, y, size);
		} else if (v.shown != null && drawPng(g, v.shown, x, y, size)) {
			return;
		} else if (v.saved && v.json == null && v.png == null && v.texId == null) {
			drawItem(g, ItemModels.withCmd(item, v.savedCmd), x, y, size);
		} else {
			g.drawCenteredString(font, "?", x + size / 2, y + size / 2 - 4, UiTheme.cMuted());
		}
	}

	private void tailLabel(GuiGraphics g, String text, int x, int y, int maxW, int color) {
		String shown = text;
		if (font.width(shown) > maxW) shown = "..." + font.plainSubstrByWidth(text, Math.max(8, maxW - font.width("...")), true);
		g.drawString(font, shown, x, y, color, false);
	}

	private static void box(GuiGraphics g, int x, int y, int w, int h, int fill, int edge) {
		g.fill(x, y, x + w, y + h, edge);
		g.fill(x + 1, y + 1, x + w - 1, y + h - 1, fill);
	}

	private static boolean hit(double mx, double my, int x, int y, int w, int h) {
		return mx >= x && mx < x + w && my >= y && my < y + h;
	}

	private static String tail(String id) {
		int cut = Math.max(id.lastIndexOf('/'), id.lastIndexOf(':'));
		return cut >= 0 ? id.substring(cut + 1) : id;
	}

	private String cmdText(Variant v) {
		return panelW < 380 ? "#" + v.cmd : I18n.get(K + "rp_flow_cmd", v.cmd);
	}

	private String nodeLabel(Variant v) {
		if (v.json != null) return v.json.getFileName().toString();
		if (v.png != null) return v.png.getFileName().toString();
		if (v.texId != null) return tail(v.texId);
		if (v.saved) return tail(v.model);
		return I18n.get(K + "rp_flow_none");
	}

	private int rowCount() {
		return item.isEmpty() ? 0 : variants.size() + 1;
	}

	private int maxScroll() {
		return Math.max(0, rowCount() - viewRows);
	}

	private int rowY(int row) {
		return rowsTop + (row - scroll) * ROW_H;
	}

	private int chipX() {
		return trunkX + 8;
	}

	private int chipW() {
		int w = 24;
		for (Variant v : variants) w = Math.max(w, font.width(cmdText(v)) + 10);
		return Math.min(w, 80);
	}

	private int nodeX() {
		return chipX() + chipW() + 6;
	}

	private int nodeW() {
		return canvasX + canvasW - 6 - nodeX();
	}

	private int rowAt(double mx, double my) {
		if (mx < trunkX || mx >= canvasX + canvasW || my < rowsTop || my >= bodyBottom) return -1;
		int row = (int) ((my - rowsTop) / ROW_H) + scroll;
		return row < rowCount() ? row : -1;
	}

	private boolean clickCanvas(double mx, double my) {
		if (hit(mx, my, canvasX, bodyTop, rootW, ROOT_H)) {
			armed = "";
			select(-1);
			return true;
		}
		int row = rowAt(mx, my);
		if (row < 0) return false;
		armed = "";
		if (row == variants.size()) {
			addVariant();
			select(row);
			openChooser();
		} else if (mx >= nodeX() + nodeW() - 14 && mx < nodeX() + nodeW()) {
			removeVariant(row);
		} else if (mx >= nodeX() && mx < nodeX() + nodeW()) {
			select(row);
			openChooser();
		} else {
			select(row);
		}
		UiTheme.playClick();
		return true;
	}

	private int chW() {
		return Math.min(this.width - 40, 292);
	}

	private int chH() {
		return Math.min(this.height - 40, 190);
	}

	private int chX() {
		return (this.width - chW()) / 2;
	}

	private int chY() {
		return (this.height - chH()) / 2;
	}

	private int chCols() {
		return Math.max(1, (chW() - 24) / CELL);
	}

	private int chRows() {
		return Math.max(1, (chH() - 66) / CELL);
	}

	private int chMaxScroll() {
		return Math.max(0, (choices.size() + chCols() - 1) / chCols() - chRows());
	}

	private int choiceAt(double mx, double my) {
		int gx = chX() + 10;
		int gy = chY() + 22;
		if (mx < gx || my < gy || mx >= gx + chCols() * CELL || my >= gy + chRows() * CELL) return -1;
		int idx = ((int) ((my - gy) / CELL) + chooserScroll) * chCols() + (int) ((mx - gx) / CELL);
		return idx < choices.size() ? idx : -1;
	}

	private int chFileW() {
		return font.width(I18n.get(K + "rp_choose_file")) + 14;
	}

	private int chJsonW() {
		return font.width(I18n.get(K + "rp_choose_json")) + 14;
	}

	private void clickChooser(double mx, double my) {
		Variant v = selected();
		if (v == null || !hit(mx, my, chX(), chY(), chW(), chH())) {
			chooser = false;
			return;
		}
		int idx = choiceAt(mx, my);
		int by = chY() + chH() - 24;
		int fx = chX() + 10;
		int jx = fx + chFileW() + 6;
		if (idx >= 0) {
			ItemModels.Texture t = choices.get(idx);
			v.texId = t.id();
			v.png = null;
			v.json = null;
			v.shown = t.file();
			touch(v);
			chooser = false;
			UiTheme.playClick();
		} else if (hit(mx, my, fx, by, chFileW(), 16)) {
			chooser = false;
			UiTheme.playClick();
			ResourcePacks.chooseFile(I18n.get(K + "rp_choose_file"), "*.png", "PNG", p -> {
				if (variants.contains(v)) usePng(v, p);
			});
		} else if (hit(mx, my, jx, by, chJsonW(), 16)) {
			chooser = false;
			UiTheme.playClick();
			ResourcePacks.chooseFile(I18n.get(K + "rp_choose_json"), "*.json", "JSON", p -> {
				if (variants.contains(v)) useJson(v, p, null);
			});
		}
	}

	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		super.renderBackground(g, mouseX, mouseY, partialTick);
		UiTheme.drawPanel(g, leftPos, topPos, panelW, panelH);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		boolean covered = picker.isOpen() || chooser;
		int mx = covered ? -10000 : mouseX;
		int my = covered ? -10000 : mouseY;
		super.render(g, mx, my, partialTick);
		UiTheme.drawHeader(g, font, this.title, leftPos, topPos, panelW);
		int topY = topPos + UiTheme.HEADER_H + 6;
		UiTheme.label(g, font, I18n.get(K + "rp_pack"), leftPos + PAD, topY + 4);
		String pack = currentPack();
		int nameX = packMinus.getX() + 20;
		UiTheme.clipLabel(g, font, pack != null ? pack : I18n.get(K + "rp_pack_new"), nameX, topY + 4, packPlus.getX() - 4 - nameX, UiTheme.cLabel());
		drawCanvas(g, mx, my);
		drawSide(g);
		int fy = topPos + panelH - FOOTER_H;
		g.fill(leftPos + 1, fy, leftPos + panelW - 1, fy + 1, EDGE);
		boolean idle = status.getString().isEmpty();
		int tx = backBtn.getX() + backBtn.getWidth() + 8;
		UiTheme.clipLabel(g, font, idle ? I18n.get(K + "rp_drop_hint") : status.getString(), tx, fy + 11, applyBtn.getX() - 8 - tx, idle ? UiTheme.cMuted() : statusColor);
		picker.render(g, font, this.width, this.height, mouseX, mouseY);
		if (chooser) drawChooser(g, mouseX, mouseY);
	}

	private void drawCanvas(GuiGraphics g, int mx, int my) {
		boolean rootHot = hit(mx, my, canvasX, bodyTop, rootW, ROOT_H);
		box(g, canvasX, bodyTop, rootW, ROOT_H, rootHot ? NODE_HOT : UiTheme.cInputBg(), sel < 0 && !item.isEmpty() ? UiTheme.cFocus() : EDGE);
		if (item.isEmpty()) {
			UiTheme.clipLabel(g, font, I18n.get(K + "rp_no_item"), canvasX + 6, bodyTop + 14, rootW - 12, UiTheme.cMuted());
			return;
		}
		g.renderItem(ItemModels.withoutCmd(item), canvasX + 4, bodyTop + 10);
		UiTheme.clipLabel(g, font, item.getHoverName().getString(), canvasX + 24, bodyTop + 7, rootW - 28, UiTheme.cLabel());
		UiTheme.clipLabel(g, font, I18n.get(K + "rp_flow_default"), canvasX + 24, bodyTop + 19, rootW - 28, UiTheme.cMuted());
		int rows = rowCount();
		int chipX = chipX();
		int chipW = chipW();
		int nodeX = nodeX();
		int nodeW = nodeW();
		int rootMid = bodyTop + ROOT_H / 2;
		int hot = rowAt(mx, my);
		g.fill(canvasX + rootW, rootMid, trunkX + 1, rootMid + 1, LINE);
		g.enableScissor(trunkX, bodyTop, canvasX + canvasW, bodyBottom);
		int first = rowY(0) + ROW_H / 2;
		int last = rowY(rows - 1) + ROW_H / 2;
		g.fill(trunkX, Math.min(first, rootMid), trunkX + 1, Math.max(last, rootMid) + 1, LINE);
		for (int i = scroll; i < rows && rowY(i) < bodyBottom; i++) {
			int y = rowY(i);
			int mid = y + ROW_H / 2;
			g.fill(trunkX, mid, chipX, mid + 1, LINE);
			if (i == variants.size()) {
				String add = I18n.get(K + "rp_flow_add");
				box(g, chipX, y + 4, font.width(add) + 12, 16, hot == i ? NODE_HOT : UiTheme.cInputBg(), EDGE);
				g.drawString(font, add, chipX + 6, y + 8, UiTheme.cLabel(), false);
				continue;
			}
			Variant v = variants.get(i);
			int edge = sel == i ? UiTheme.cFocus() : EDGE;
			boolean overNode = hot == i && mx >= nodeX;
			box(g, chipX, y + 4, chipW, 16, hot == i && !overNode ? NODE_HOT : UiTheme.cInputBg(), edge);
			UiTheme.clipLabel(g, font, cmdText(v), chipX + 5, y + 8, chipW - 8, UiTheme.cLabel());
			g.fill(chipX + chipW, mid, nodeX, mid + 1, LINE);
			box(g, nodeX, y + 2, nodeW, 20, overNode ? NODE_HOT : UiTheme.cInputBg(), edge);
			drawLook(g, v, nodeX + 3, y + 4, 16);
			tailLabel(g, nodeLabel(v), nodeX + 23, y + 8, nodeW - 39, !v.saved || v.changed ? WARN : UiTheme.cLabel());
			g.drawString(font, "\u00d7", nodeX + nodeW - 10, y + 8, overNode && mx >= nodeX + nodeW - 14 ? ERR : UiTheme.cMuted(), false);
		}
		g.disableScissor();
		UiTheme.drawThinScrollBar(g, canvasX + canvasW - 3, bodyTop, bodyBottom - bodyTop, scroll * ROW_H, maxScroll() * ROW_H);
	}

	private void drawSide(GuiGraphics g) {
		g.fill(sideX - 5, bodyTop, sideX - 4, bodyBottom, EDGE);
		UiTheme.label(g, font, I18n.get(K + "rp_preview"), sideX, bodyTop);
		int px = sideX + (sideW - previewSize) / 2;
		int py = bodyTop + 12;
		UiTheme.drawPreviewFrame(g, px, py, previewSize, previewSize);
		Variant v = selected();
		if (!item.isEmpty()) {
			int inner = Math.max(16, (previewSize - 8) / 16 * 16);
			int off = (previewSize - inner) / 2;
			drawLook(g, v, px + off, py + off, inner);
			if (v != null && (!v.saved || v.changed)) {
				g.nextStratum();
				g.drawString(font, I18n.get(K + "rp_pending"), px + 3, py + previewSize - 11, WARN, true);
			}
		}
		int condY = py + previewSize + 4;
		String cond = v == null ? I18n.get(K + "rp_cond_none") : I18n.get(K + "rp_cond_cmd", v.cmd);
		UiTheme.clipLabel(g, font, cond, sideX, condY, sideW, UiTheme.cMuted());
		if (v != null) UiTheme.clipLabel(g, font, I18n.get(K + "rp_cmd"), sideX, condY + 16, sideW - 50, UiTheme.cLabel());
	}

	private void drawChooser(GuiGraphics g, int mouseX, int mouseY) {
		int x = chX();
		int y = chY();
		int w = chW();
		int h = chH();
		int cols = chCols();
		int rows = chRows();
		int gx = x + 10;
		int gy = y + 22;
		Variant v = selected();
		int hot = choiceAt(mouseX, mouseY);
		UiTheme.pushOverlay(g);
		g.fill(0, 0, this.width, this.height, 0xB0121314);
		UiTheme.drawPanel(g, x, y, w, h);
		UiTheme.label(g, font, I18n.get(K + "rp_choose_title"), x + 10, y + 8);
		g.fill(gx, gy, gx + cols * CELL, gy + rows * CELL, UiTheme.cInputBg());
		if (choices.isEmpty()) {
			UiTheme.clipLabel(g, font, I18n.get(K + "rp_choose_empty"), gx + 6, gy + 6, cols * CELL - 12, UiTheme.cMuted());
		}
		for (int i = chooserScroll * cols; i < choices.size() && i < (chooserScroll + rows) * cols; i++) {
			ItemModels.Texture t = choices.get(i);
			int cx = gx + (i % cols) * CELL;
			int cy = gy + (i / cols - chooserScroll) * CELL;
			if (v != null && t.file().equals(v.shown)) box(g, cx, cy, CELL, CELL, UiTheme.cInputBg(), UiTheme.cFocus());
			if (i == hot) g.fill(cx + 1, cy + 1, cx + CELL - 1, cy + CELL - 1, NODE_HOT);
			if (!drawPng(g, t.file(), cx + 4, cy + 4, 16)) g.drawString(font, "?", cx + 9, cy + 8, UiTheme.cMuted(), false);
		}
		UiTheme.drawThinScrollBar(g, gx + cols * CELL - 3, gy, rows * CELL, chooserScroll * CELL, chMaxScroll() * CELL);
		UiTheme.clipLabel(g, font, hot >= 0 ? choices.get(hot).id() : I18n.get(K + "rp_choose_hint"), x + 10, gy + rows * CELL + 6, w - 20, hot >= 0 ? UiTheme.cLabel() : UiTheme.cMuted());
		int by = y + h - 24;
		int fx = x + 10;
		int jx = fx + chFileW() + 6;
		UiTheme.drawButton(g, font, Component.translatable(K + "rp_choose_file"), fx, by, chFileW(), 16, hit(mouseX, mouseY, fx, by, chFileW(), 16), true);
		UiTheme.drawButton(g, font, Component.translatable(K + "rp_choose_json"), jx, by, chJsonW(), 16, hit(mouseX, mouseY, jx, by, chJsonW(), 16), true);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		double mouseX = event.x();
		double mouseY = event.y();
		int button = event.button();
		if (picker.isOpen()) {
			if (button == 0) {
				ItemStack stack = picker.stackAt(this.width, this.height, mouseX, mouseY);
				if (!stack.isEmpty()) setItem(stack);
			}
			picker.close();
			return true;
		}
		if (chooser) {
			if (button == 0) clickChooser(mouseX, mouseY);
			return true;
		}
		if (button == 0 && clickCanvas(mouseX, mouseY)) {
			this.setFocused(null);
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (picker.isOpen()) return true;
		if (chooser) {
			chooserScroll = Mth.clamp(chooserScroll - (int) Math.signum(scrollY), 0, chMaxScroll());
			return true;
		}
		if (mouseX >= canvasX && mouseX < canvasX + canvasW && mouseY >= bodyTop && mouseY < bodyBottom) {
			scroll = Mth.clamp(scroll - (int) Math.signum(scrollY), 0, maxScroll());
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		int keyCode = event.key();
		if (picker.isOpen()) {
			if (keyCode == 256) picker.close();
			return true;
		}
		if (chooser) {
			if (keyCode == 256) chooser = false;
			return true;
		}
		if (keyCode == 261 && selected() != null && !cmdField.isFocused()) {
			removeVariant(sel);
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		if (picker.isOpen() || chooser) return true;
		return super.charTyped(event);
	}

	@Override
	public void onClose() {
		if (!discardOk("close")) return;
		Minecraft.getInstance().setScreen(parent);
	}

	@Override
	public void removed() {
		dropThumbs();
		super.removed();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
