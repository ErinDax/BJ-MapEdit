package cn.erindax.bjmapedit.client.screen;

import cn.erindax.bjmapedit.PlatformNetwork;
import cn.erindax.bjmapedit.client.widget.InvPicker;
import cn.erindax.bjmapedit.client.widget.ItemModels;
import cn.erindax.bjmapedit.client.widget.ResourcePacks;
import cn.erindax.bjmapedit.client.widget.UiTheme;
import cn.erindax.bjmapedit.networking.payload.GiveItemPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ItemModelScreen extends Screen {
	private static final int ROW_H = 22;
	private static final int PAD = 10;
	private static final int LIST_ROW_H = 14;
	private static final int FOOTER_H = 30;
	private static final int OK = 0xFF7EE787;
	private static final int ERR = 0xFFFF7B72;

	private final Screen parent;
	private final String preferredPack;
	private final InvPicker picker = new InvPicker();
	private List<String> packs = new ArrayList<>();
	private int packIdx;
	private ItemStack item = ItemStack.EMPTY;
	private Path texturePath;
	private Path modelPath;
	private List<ItemModels.Mapping> mappings = new ArrayList<>();
	private int listScroll;
	private int listRows = 5;
	private String autoName = "";
	private Component status = Component.empty();
	private int statusColor = OK;
	private boolean opened;
	private String snapCmd = "";
	private String snapName = "";

	private EditBox cmdField;
	private EditBox nameField;
	private Button packMinus;
	private Button packPlus;
	private Button pickBtn;
	private Button heldBtn;
	private Button textureBtn;
	private Button modelBtn;
	private Button clearModelBtn;
	private Button backBtn;
	private Button giveBtn;
	private Button applyBtn;

	private int leftPos;
	private int topPos;
	private int panelW;
	private int panelH;
	private int inputX;
	private int inputW;
	private int rowY0;
	private int listTop;

	public ItemModelScreen(Screen parent, String preferredPack) {
		super(Component.translatable("screen.bj_mapedit.rp_models_title"));
		this.parent = parent;
		this.preferredPack = preferredPack;
	}

	@Override
	protected void init() {
		boolean first = !opened;
		opened = true;
		if (cmdField != null) {
			snapCmd = cmdField.getValue();
			snapName = nameField.getValue();
		}
		String keep = first ? preferredPack : currentPack();
		packs = ResourcePacks.folderPacks();
		packIdx = keep == null ? 0 : Math.max(0, packs.indexOf(keep));
		layout();

		packMinus = this.addRenderableWidget(Button.builder(Component.literal("<"), b -> cyclePack(-1)).bounds(0, 0, 16, 16).build());
		packPlus = this.addRenderableWidget(Button.builder(Component.literal(">"), b -> cyclePack(1)).bounds(0, 0, 16, 16).build());
		pickBtn = this.addRenderableWidget(Button.builder(Component.translatable("screen.bj_mapedit.rp_pick_inv"), b -> picker.open()).bounds(0, 0, 40, 16).build());
		heldBtn = this.addRenderableWidget(Button.builder(Component.translatable("screen.bj_mapedit.use_held"), b -> useHeld()).bounds(0, 0, 40, 16).build());
		cmdField = new EditBox(font, 0, 0, inputW, 16, Component.empty());
		cmdField.setFilter(s -> s.length() <= 9 && s.chars().allMatch(Character::isDigit));
		cmdField.setValue(snapCmd);
		cmdField.setResponder(s -> {
			updateAutoName(false);
		});
		this.addRenderableWidget(cmdField);
		nameField = new EditBox(font, 0, 0, inputW, 16, Component.empty());
		nameField.setMaxLength(128);
		nameField.setValue(snapName);
		this.addRenderableWidget(nameField);
		textureBtn = this.addRenderableWidget(Button.builder(Component.translatable("screen.bj_mapedit.rp_choose_texture"), b -> chooseTexture()).bounds(0, 0, 40, 16).build());
		modelBtn = this.addRenderableWidget(Button.builder(Component.translatable("screen.bj_mapedit.rp_choose_model"), b -> chooseModel()).bounds(0, 0, 40, 16).build());
		clearModelBtn = this.addRenderableWidget(Button.builder(Component.translatable("screen.bj_mapedit.rp_clear"), b -> {
			modelPath = null;
			texturePath = null;
		}).bounds(0, 0, 40, 16).build());
		backBtn = this.addRenderableWidget(Button.builder(Component.translatable("screen.bj_mapedit.rp_back"), b -> onClose()).bounds(0, 0, 60, 20).build());
		giveBtn = this.addRenderableWidget(Button.builder(Component.translatable("screen.bj_mapedit.give_item"), b -> give()).bounds(0, 0, 60, 20).build());
		applyBtn = this.addRenderableWidget(Button.builder(Component.translatable("screen.bj_mapedit.rp_apply"), b -> apply()).bounds(0, 0, 60, 20).build());
		placeWidgets();
		refreshMappings();
	}

	private void layout() {
		panelW = Mth.clamp(this.width - 16, 300, 460);
		int fixed = UiTheme.HEADER_H + 8 + ROW_H * 6 + 16 + 8 + 12 + 14 + FOOTER_H;
		listRows = Mth.clamp((this.height - 16 - fixed) / LIST_ROW_H, 2, 6);
		panelH = fixed + listRows * LIST_ROW_H;
		leftPos = (this.width - panelW) / 2;
		topPos = Math.max(8, (this.height - panelH) / 2);
		int labelW = 40;
		for (String key : new String[]{"rp_pack", "rp_item", "rp_cmd", "rp_name", "rp_texture", "rp_model"}) {
			labelW = Math.max(labelW, font.width(I18n.get("screen.bj_mapedit." + key)) + 10);
		}
		inputX = leftPos + PAD + labelW;
		inputW = panelW - PAD * 2 - labelW;
		rowY0 = topPos + UiTheme.HEADER_H + 8;
		listTop = rowY0 + ROW_H * 6 + 16;
	}

	private int rowY(int row) {
		return rowY0 + row * ROW_H;
	}

	private void placeWidgets() {
		packMinus.setPosition(inputX, rowY(0));
		packPlus.setPosition(inputX + inputW - 16, rowY(0));
		int heldW = font.width(heldBtn.getMessage()) + 12;
		int pickW = font.width(pickBtn.getMessage()) + 12;
		heldBtn.setPosition(inputX + inputW - heldW, rowY(1));
		heldBtn.setWidth(heldW);
		pickBtn.setPosition(heldBtn.getX() - 4 - pickW, rowY(1));
		pickBtn.setWidth(pickW);
		cmdField.setPosition(inputX, rowY(2));
		cmdField.setWidth(inputW);
		nameField.setPosition(inputX, rowY(3));
		nameField.setWidth(inputW);
		textureBtn.setPosition(inputX, rowY(4));
		textureBtn.setWidth(font.width(textureBtn.getMessage()) + 12);
		modelBtn.setPosition(inputX, rowY(5));
		modelBtn.setWidth(font.width(modelBtn.getMessage()) + 12);
		clearModelBtn.setPosition(modelBtn.getX() + modelBtn.getWidth() + 4, rowY(5));
		clearModelBtn.setWidth(font.width(clearModelBtn.getMessage()) + 12);
		int footerY = topPos + panelH - FOOTER_H + (FOOTER_H - 20) / 2;
		int backW = Math.max(52, font.width(backBtn.getMessage()) + 22);
		int giveW = Math.max(52, font.width(giveBtn.getMessage()) + 22);
		int applyW = Math.max(52, font.width(applyBtn.getMessage()) + 22);
		backBtn.setPosition(leftPos + 8, footerY);
		backBtn.setWidth(backW);
		applyBtn.setPosition(leftPos + panelW - 8 - applyW, footerY);
		applyBtn.setWidth(applyW);
		giveBtn.setPosition(applyBtn.getX() - 6 - giveW, footerY);
		giveBtn.setWidth(giveW);
	}

	private String currentPack() {
		return packs.isEmpty() ? null : packs.get(Mth.clamp(packIdx, 0, packs.size() - 1));
	}

	private Path packDir(String pack) {
		return ResourcePacks.root().resolve(pack);
	}

	private void cyclePack(int delta) {
		if (packs.isEmpty()) return;
		packIdx = Math.floorMod(packIdx + delta, packs.size());
		refreshMappings();
	}

	private void useHeld() {
		var player = Minecraft.getInstance().player;
		if (player == null) return;
		ItemStack held = player.getMainHandItem();
		if (held.isEmpty()) {
			setStatus(Component.translatable("screen.bj_mapedit.held_empty"), ERR);
			return;
		}
		setItem(held);
	}

	private void setItem(ItemStack stack) {
		item = stack.copyWithCount(1);
		refreshMappings();
		int cmd = ItemModels.cmdOf(stack);
		if (cmd <= 0) cmd = nextFreeCmd();
		cmdField.setValue(String.valueOf(cmd));
		updateAutoName(true);
	}

	private int nextFreeCmd() {
		int max = 0;
		for (ItemModels.Mapping m : mappings) max = Math.max(max, m.cmd());
		return max + 1;
	}

	private void updateAutoName(boolean force) {
		if (nameField == null) return;
		String next = item.isEmpty() ? "" : BuiltInRegistries.ITEM.getKey(item.getItem()).getPath() + "_" + cmdField.getValue();
		String now = nameField.getValue();
		if (force || now.isEmpty() || now.equals(autoName)) nameField.setValue(next);
		autoName = next;
	}

	private int parseCmd() {
		try {
			return Integer.parseInt(cmdField.getValue().trim());
		} catch (NumberFormatException e) {
			return -1;
		}
	}

	private void refreshMappings() {
		String pack = currentPack();
		mappings = item.isEmpty() || pack == null ? new ArrayList<>() : ItemModels.read(packDir(pack), item.getItem());
		listScroll = Mth.clamp(listScroll, 0, Math.max(0, mappings.size() - listRows));
	}

	private void chooseTexture() {
		ResourcePacks.chooseFile(I18n.get("screen.bj_mapedit.rp_choose_texture"), "*.png", "PNG", p -> {
			texturePath = p;
			setStatus(Component.translatable("screen.bj_mapedit.rp_picked", p.getFileName().toString()), OK);
		});
	}

	private void chooseModel() {
		ResourcePacks.chooseFile(I18n.get("screen.bj_mapedit.rp_choose_model"), "*.json", "JSON", p -> {
			modelPath = p;
			setStatus(Component.translatable("screen.bj_mapedit.rp_picked", p.getFileName().toString()), OK);
		});
	}

	@Override
	public void onFilesDrop(List<Path> paths) {
		for (Path p : paths) {
			String n = p.getFileName().toString().toLowerCase(Locale.ROOT);
			if (n.endsWith(".png")) texturePath = p;
			else if (n.endsWith(".json")) modelPath = p;
			else continue;
			setStatus(Component.translatable("screen.bj_mapedit.rp_picked", p.getFileName().toString()), OK);
		}
	}

	private void setStatus(Component text, int color) {
		status = text;
		statusColor = color;
	}

	private boolean checkInput() {
		if (item.isEmpty()) {
			setStatus(Component.translatable("screen.bj_mapedit.rp_need_item"), ERR);
			return false;
		}
		if (parseCmd() < 1) {
			setStatus(Component.translatable("screen.bj_mapedit.rp_bad_cmd"), ERR);
			return false;
		}
		return true;
	}

	private void apply() {
		if (!checkInput()) return;
		int cmd = parseCmd();
		String name = ResourcePacks.resourceName(nameField.getValue());
		if (name.isEmpty()) {
			setStatus(Component.translatable("screen.bj_mapedit.rp_bad_name"), ERR);
			return;
		}
		try {
			String pack = currentPack();
			if (pack == null) {
				pack = ResourcePacks.createPack("bjmapedit_models");
				packs = ResourcePacks.folderPacks();
				packIdx = Math.max(0, packs.indexOf(pack));
			}
			Path dir = packDir(pack);
			String ns = ResourcePacks.namespace(pack);
			if (modelPath != null) {
				ItemModels.importModel(dir, ns, name, modelPath, texturePath);
			} else if (texturePath != null) {
				ItemModels.copyTexture(texturePath, dir, ns, name);
				ItemModels.writeGenerated(dir, ns, name, ItemModels.baseParent(item.getItem()), ItemModels.resourceId(ns, name));
			} else if (!Files.isRegularFile(ItemModels.modelFile(dir, ns, name))) {
				setStatus(Component.translatable("screen.bj_mapedit.rp_need_file"), ERR);
				return;
			}
			ItemModels.put(dir, item.getItem(), cmd, ItemModels.resourceId(ns, name));
			texturePath = null;
			modelPath = null;
			refreshMappings();
			setStatus(Component.translatable("screen.bj_mapedit.rp_applied", pack), OK);
			ResourcePacks.enableAndReload(pack);
		} catch (Exception e) {
			setStatus(Component.translatable("screen.bj_mapedit.rp_failed", String.valueOf(e.getMessage())), ERR);
		}
	}

	private void removeMapping(ItemModels.Mapping m) {
		String pack = currentPack();
		if (pack == null || item.isEmpty()) return;
		try {
			ItemModels.remove(packDir(pack), item.getItem(), m.cmd());
			refreshMappings();
			setStatus(Component.translatable("screen.bj_mapedit.rp_removed", m.cmd()), OK);
			ResourcePacks.enableAndReload(pack);
		} catch (Exception e) {
			setStatus(Component.translatable("screen.bj_mapedit.rp_failed", String.valueOf(e.getMessage())), ERR);
		}
	}

	private void loadMapping(ItemModels.Mapping m) {
		cmdField.setValue(String.valueOf(m.cmd()));
		String model = m.model();
		int slash = model.indexOf(":item/");
		nameField.setValue(slash >= 0 ? model.substring(slash + ":item/".length()) : ResourcePacks.resourceName(model));
		autoName = "";
		texturePath = null;
		modelPath = null;
	}

	private void give() {
		if (!checkInput()) return;
		PlatformNetwork.sendToServer(new GiveItemPayload(ItemModels.withCmd(item, parseCmd())));
	}

	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		super.renderBackground(g, mouseX, mouseY, partialTick);
		UiTheme.drawPanel(g, leftPos, topPos, panelW, panelH);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		boolean covered = picker.isOpen();
		int mx = covered ? -10000 : mouseX;
		int my = covered ? -10000 : mouseY;
		super.render(g, mx, my, partialTick);
		UiTheme.drawHeader(g, font, this.title, leftPos, topPos, panelW);
		int fy = topPos + panelH - FOOTER_H;
		g.fill(leftPos + 1, fy, leftPos + panelW - 1, fy + 1, 0xFF2B2C2D);
		label(g, "rp_pack", 0);
		String pack = currentPack();
		String packText = pack != null ? pack : I18n.get("screen.bj_mapedit.rp_pack_new");
		UiTheme.clipLabel(g, font, packText, inputX + 22, rowY(0) + 4, inputW - 44, UiTheme.cLabel());
		label(g, "rp_item", 1);
		int itemRight = pickBtn.getX() - 6;
		if (item.isEmpty()) {
			UiTheme.muted(g, font, I18n.get("screen.bj_mapedit.rp_no_item"), inputX, rowY(1) + 4);
		} else {
			g.renderItem(item, inputX, rowY(1));
			String text = item.getHoverName().getString() + " " + ItemModels.itemId(item.getItem());
			UiTheme.clipLabel(g, font, text, inputX + 20, rowY(1) + 4, Math.max(16, itemRight - inputX - 20), UiTheme.cLabel());
		}
		label(g, "rp_cmd", 2);
		label(g, "rp_name", 3);
		label(g, "rp_texture", 4);
		int texX = textureBtn.getX() + textureBtn.getWidth() + 6;
		String texText = texturePath != null ? texturePath.getFileName().toString() : I18n.get("screen.bj_mapedit.rp_none");
		UiTheme.clipLabel(g, font, texText, texX, rowY(4) + 4, Math.max(16, inputX + inputW - texX), texturePath != null ? UiTheme.cLabel() : UiTheme.cMuted());
		label(g, "rp_model", 5);
		int modX = clearModelBtn.getX() + clearModelBtn.getWidth() + 6;
		String modText = modelPath != null ? modelPath.getFileName().toString()
			: I18n.get(texturePath != null ? "screen.bj_mapedit.rp_model_auto" : "screen.bj_mapedit.rp_none");
		UiTheme.clipLabel(g, font, modText, modX, rowY(5) + 4, Math.max(16, inputX + inputW - modX), modelPath != null ? UiTheme.cLabel() : UiTheme.cMuted());
		drawMappings(g, mx, my);
		int hintY = listTop + listRows * LIST_ROW_H + 6;
		UiTheme.clipLabel(g, font, I18n.get("screen.bj_mapedit.rp_drop_hint"), leftPos + PAD, hintY, panelW - PAD * 2, UiTheme.cMuted());
		UiTheme.clipLabel(g, font, status.getString(), leftPos + PAD, hintY + 12, panelW - PAD * 2, statusColor);
		picker.render(g, font, this.width, this.height, mouseX, mouseY);
	}

	private void label(GuiGraphics g, String key, int row) {
		UiTheme.label(g, font, I18n.get("screen.bj_mapedit." + key), leftPos + PAD, rowY(row) + 4);
	}

	private void drawMappings(GuiGraphics g, int mouseX, int mouseY) {
		UiTheme.label(g, font, I18n.get("screen.bj_mapedit.rp_mappings"), leftPos + PAD, listTop - 12);
		int x0 = leftPos + PAD;
		int x1 = leftPos + panelW - PAD;
		g.fill(x0, listTop, x1, listTop + listRows * LIST_ROW_H, UiTheme.cInputBg());
		if (mappings.isEmpty()) {
			UiTheme.muted(g, font, I18n.get(item.isEmpty() ? "screen.bj_mapedit.rp_no_item" : "screen.bj_mapedit.rp_no_mappings"), x0 + 4, listTop + 3);
			return;
		}
		int current = parseCmd();
		for (int i = 0; i < listRows && i + listScroll < mappings.size(); i++) {
			ItemModels.Mapping m = mappings.get(i + listScroll);
			int y = listTop + i * LIST_ROW_H;
			boolean hover = mouseX >= x0 && mouseX < x1 && mouseY >= y && mouseY < y + LIST_ROW_H;
			if (m.cmd() == current) g.fill(x0, y, x1, y + LIST_ROW_H, 0xFF21262D);
			else if (hover) g.fill(x0, y, x1, y + LIST_ROW_H, 0xFF191A1B);
			String text = m.cmd() + "  \u2192  " + m.model();
			UiTheme.clipLabel(g, font, text, x0 + 4, y + 3, x1 - x0 - 24, UiTheme.cLabel());
			boolean delHover = hover && mouseX >= x1 - 14;
			g.drawString(font, "\u00d7", x1 - 10, y + 3, delHover ? ERR : UiTheme.cMuted(), false);
		}
		UiTheme.drawThinScrollBar(g, x1 - 3, listTop, listRows * LIST_ROW_H, listScroll, Math.max(0, mappings.size() - listRows));
	}

	private boolean clickMappings(double mouseX, double mouseY) {
		int x0 = leftPos + PAD;
		int x1 = leftPos + panelW - PAD;
		if (mouseX < x0 || mouseX >= x1 || mouseY < listTop || mouseY >= listTop + listRows * LIST_ROW_H) return false;
		int idx = (int) ((mouseY - listTop) / LIST_ROW_H) + listScroll;
		if (idx < 0 || idx >= mappings.size()) return true;
		ItemModels.Mapping m = mappings.get(idx);
		if (mouseX >= x1 - 14) removeMapping(m);
		else loadMapping(m);
		return true;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (picker.isOpen()) {
			if (button == 0) {
				ItemStack stack = picker.stackAt(this.width, this.height, mouseX, mouseY);
				if (!stack.isEmpty()) setItem(stack);
			}
			picker.close();
			return true;
		}
		if (button == 0 && clickMappings(mouseX, mouseY)) return true;
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (mouseY >= listTop && mouseY < listTop + listRows * LIST_ROW_H) {
			listScroll = Mth.clamp(listScroll - (int) Math.signum(scrollY), 0, Math.max(0, mappings.size() - listRows));
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (picker.isOpen() && keyCode == 256) {
			picker.close();
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public void onClose() {
		Minecraft.getInstance().setScreen(parent);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
