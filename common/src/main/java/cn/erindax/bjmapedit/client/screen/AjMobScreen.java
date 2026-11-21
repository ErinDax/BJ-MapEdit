package cn.erindax.bjmapedit.client.screen;

import cn.erindax.bjmapedit.client.DatapackNet;
import cn.erindax.bjmapedit.client.EditorAccess;
import cn.erindax.bjmapedit.client.widget.AjData;
import cn.erindax.bjmapedit.client.widget.AjPack;
import cn.erindax.bjmapedit.client.widget.GiveCommands;
import cn.erindax.bjmapedit.client.widget.UiTheme;
import cn.erindax.bjmapedit.networking.DatapackOps;
import cn.erindax.bjmapedit.networking.payload.DatapackOpPayload;
import cn.erindax.bjmapedit.networking.payload.DatapackOpResultPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.Mth;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class AjMobScreen extends Screen {
	private static final String K = "screen.bj_mapedit.";
	private static final int PAD = 8;
	private static final int ROW_H = 12;
	private static final int LINE = 20;
	private static final int FIELD_H = 16;
	private static final int FOOTER_H = 26;
	private static final int LIST_W = 124;
	private static final int TICK_W = 34;
	private static final int OK = 0xFF7EE787;
	private static final int ERR = 0xFFFF7B72;
	private static final int HINT = 0x6E7681;
	private static final String[] MODES = {"aj_mode_static", "aj_mode_passive", "aj_mode_hostile"};
	private static final String[] SLOTS = {"aj_idle", "aj_walk", "aj_attack", "aj_hurt", "aj_death"};
	private static final String[] LABELS = {"aj_name", "aj_rig", "aj_carrier", "aj_nbt"};
	private static final String[][] GUESS = {{"idle", "stand"}, {"walk", "run", "move"}, {"attack", "atk"}, {"hurt", "damage"}, {"death", "die", "dead"}};

	private static AjData draft;
	private static String generatedJson;
	private static WeakReference<Object> draftOwner = new WeakReference<>(null);
	private static int tokenSeq;

	private AjData model;
	private List<AjData.Rig> rigs = List.of();
	private List<String> made = new ArrayList<>();
	private boolean scanned;
	private boolean more;
	private boolean opened;
	private boolean syncing;
	private boolean busy;
	private int busyTicks;
	private String token = "";
	private String failKey;
	private Runnable afterReload;
	private String pendingJson;
	private String pendingId;
	private String loadingPath;
	private String loadingId;
	private String deletingId;
	private String confirmDelete;
	private Component status = Component.empty();
	private int statusColor = OK;
	private int rigScroll;
	private int madeScroll;
	private int panelW;
	private int panelH;
	private int leftPos;
	private int topPos;
	private int bodyY;
	private int bodyH;
	private int listX;
	private int rightX;
	private int rightW;
	private int footerY;
	private int rigTop;
	private int rigRows;
	private int madeTop;
	private int madeRows;
	private int labelW;
	private int fieldX;
	private int scaleLabelX;
	private int animY;
	private int colW;
	private int genX;
	private int genW;
	private final int[] slotX = new int[AjData.SLOTS];
	private final int[] slotY = new int[AjData.SLOTS];
	private final int[] slotW = new int[AjData.SLOTS];
	private final EditBox[] tickFields = new EditBox[AjData.SLOTS];
	private EditBox idField;
	private EditBox carrierField;
	private EditBox scaleField;
	private EditBox nbtField;
	private Button modeBtn;
	private Button variantBtn;
	private Button silentBtn;
	private Button loopBtn;
	private Button summonBtn;
	private Button eggBtn;
	private Button copyBtn;
	private Button clearBtn;
	private int restX = Integer.MIN_VALUE;
	private int restY;
	private boolean pointerMoved;

	public AjMobScreen() {
		super(Component.translatable(K + "aj_title"));
		Object owner = Minecraft.getInstance().getConnection();
		if (draft == null || draftOwner.get() != owner) {
			draft = new AjData();
			draftOwner = new WeakReference<>(owner);
			generatedJson = null;
		}
		model = draft;
	}

	@Override
	protected void init() {
		layout();
		idField = field(24, "aj_id_hint");
		idField.setFilter(s -> s.matches("[a-z0-9_]{0,24}"));
		idField.setResponder(s -> {
			if (!syncing) model.id = s;
		});
		modeBtn = button(MODES[0], this::cycleMode, FIELD_H);
		variantBtn = button("aj_variant_default", this::cycleVariant, FIELD_H);
		carrierField = field(96, "aj_carrier_hint");
		carrierField.setResponder(s -> {
			if (!syncing) model.carrier = s.trim();
		});
		scaleField = field(8, null);
		scaleField.setFilter(s -> s.matches("[0-9.]{0,8}"));
		scaleField.setResponder(s -> {
			if (!syncing) model.scale = s;
		});
		silentBtn = button("aj_silent_on", this::toggleSilent, FIELD_H);
		nbtField = field(2048, "aj_nbt_hint");
		nbtField.setResponder(s -> {
			if (!syncing) model.nbt = s;
		});
		for (int i = AjData.ATTACK; i < AjData.SLOTS; i++) {
			int slot = i;
			EditBox box = field(5, null);
			box.setFilter(s -> s.matches("\\d{0,5}"));
			box.setResponder(s -> {
				if (!syncing) model.ticks[slot] = Mth.clamp(parse(s, AjData.defaultTicks(slot)), 1, AjData.MAX_TICKS);
			});
			tickFields[i] = box;
		}
		loopBtn = button("aj_loop_on", this::toggleLoop, FIELD_H);
		summonBtn = button("aj_summon", this::summon, 20);
		eggBtn = button("aj_egg", this::giveEgg, 20);
		copyBtn = button("aj_copy", this::copyCommand, 20);
		clearBtn = button("aj_clear", this::clearEntities, 20);
		placeWidgets();
		syncAll();
		if (!opened) {
			opened = true;
			requestScan();
		}
	}

	private EditBox field(int maxLength, String hintKey) {
		EditBox box = new EditBox(font, 0, 0, 40, FIELD_H, Component.empty());
		box.setMaxLength(maxLength);
		if (hintKey != null) box.setHint(Component.translatable(K + hintKey).withStyle(Style.EMPTY.withColor(HINT)));
		return this.addRenderableWidget(box);
	}

	private Button button(String key, Runnable action, int height) {
		return this.addRenderableWidget(Button.builder(Component.translatable(K + key), b -> action.run()).bounds(0, 0, 40, height).build());
	}

	private void layout() {
		panelW = Mth.clamp(this.width - 16, 400, 460);
		panelH = Mth.clamp(this.height - 16, 222, 262);
		leftPos = (this.width - panelW) / 2;
		topPos = Math.max(4, (this.height - panelH) / 2);
		bodyY = topPos + UiTheme.HEADER_H + 6;
		bodyH = panelH - UiTheme.HEADER_H - FOOTER_H - 10;
		listX = leftPos + PAD;
		rightX = listX + LIST_W + 10;
		rightW = leftPos + panelW - PAD - rightX;
		footerY = topPos + panelH - FOOTER_H + 3;
		int rows = Math.max(4, (bodyH - 28) / ROW_H);
		rigRows = (rows + 1) / 2;
		madeRows = rows - rigRows;
		rigTop = bodyY + 12;
		madeTop = rigTop + rigRows * ROW_H + 16;
	}

	private void put(AbstractWidget widget, int x, int y, int w) {
		widget.setPosition(x, y);
		widget.setWidth(w);
	}

	private int widest(String... keys) {
		int w = 0;
		for (String key : keys) w = Math.max(w, font.width(I18n.get(K + key)));
		return w;
	}

	private void placeWidgets() {
		int x1 = rightX + rightW;
		labelW = Math.max(widest(LABELS), widest(SLOTS));
		fieldX = rightX + labelW + 6;
		int y = bodyY;
		int modeW = widest(MODES) + 14;
		put(modeBtn, x1 - modeW, y, modeW);
		put(idField, fieldX, y, x1 - modeW - 6 - fieldX);
		y += LINE;
		put(variantBtn, x1 - 96, y, 96);
		y += LINE;
		int silentW = widest("aj_silent_on", "aj_silent_off") + 14;
		put(silentBtn, x1 - silentW, y, silentW);
		put(scaleField, x1 - silentW - 6 - 36, y, 36);
		scaleLabelX = scaleField.getX() - 4 - widest("aj_scale");
		put(carrierField, fieldX, y, scaleLabelX - 6 - fieldX);
		y += LINE;
		put(nbtField, fieldX, y, x1 - fieldX);
		y += LINE;
		animY = y;
		colW = (rightW - 10) / 2;
		int bx = leftPos + panelW - PAD;
		genW = widest("aj_generate") + 20;
		genX = bx - genW;
		bx = genX - 4;
		for (Button b : List.of(summonBtn, eggBtn, copyBtn, clearBtn)) {
			int w = font.width(b.getMessage()) + 12;
			put(b, bx - w, footerY, w);
			bx -= w + 4;
		}
	}

	private void placeSlots() {
		int top = model.mob() ? animY : animY - 2 * LINE;
		for (int i = 0; i < AjData.SLOTS; i++) {
			int col = i == AjData.WALK || i == AjData.HURT ? 1 : 0;
			int row = i <= AjData.WALK ? 0 : i <= AjData.HURT ? 1 : 2;
			boolean timed = i >= AjData.ATTACK;
			slotX[i] = rightX + col * (colW + 10) + labelW + 6;
			slotY[i] = top + row * LINE;
			slotW[i] = colW - labelW - 6 - (timed ? TICK_W + 4 : 0);
			if (timed) put(tickFields[i], slotX[i] + slotW[i] + 4, slotY[i], TICK_W);
		}
		put(loopBtn, rightX + colW + 10, model.mob() ? top + 2 * LINE : top, colW);
	}

	private void updateVisibility() {
		boolean mob = model.mob();
		AjData.Rig rig = currentRig();
		carrierField.visible = mob;
		scaleField.visible = mob;
		silentBtn.visible = mob;
		nbtField.visible = mob;
		for (int i = AjData.ATTACK; i < AjData.SLOTS; i++) tickFields[i].visible = mob;
		variantBtn.visible = rig != null && !rig.variants().isEmpty();
		placeSlots();
	}

	private static void setText(EditBox box, String text) {
		if (box.getValue().equals(text)) return;
		box.setValue(text);
		box.moveCursorToStart(false);
	}

	private void syncAll() {
		syncing = true;
		setText(idField, model.id);
		setText(carrierField, model.carrier);
		setText(scaleField, model.scale);
		setText(nbtField, model.nbt);
		for (int i = AjData.ATTACK; i < AjData.SLOTS; i++) setText(tickFields[i], String.valueOf(model.ticks[i]));
		syncing = false;
		modeBtn.setMessage(Component.translatable(K + MODES[model.mode]));
		silentBtn.setMessage(Component.translatable(K + (model.silent ? "aj_silent_on" : "aj_silent_off")));
		loopBtn.setMessage(Component.translatable(K + (model.loop ? "aj_loop_on" : "aj_loop_off")));
		variantBtn.setMessage(variantLabel());
		updateVisibility();
	}

	private Component variantLabel() {
		AjData.Rig rig = currentRig();
		if (rig != null) {
			for (AjData.Variant v : rig.variants()) {
				if (v.function().equals(model.variant)) return Component.translatable(K + "aj_variant", v.name());
			}
		}
		return Component.translatable(K + "aj_variant_default");
	}

	private AjData.Rig currentRig() {
		for (AjData.Rig r : rigs) if (r.id().equals(model.rig)) return r;
		return null;
	}

	private boolean slotShown(int slot) {
		return slot == AjData.IDLE || model.mob();
	}

	private void setStatus(Component text, int color) {
		status = text;
		statusColor = color;
	}

	private void error(String key, Object... args) {
		setStatus(Component.translatable(key, args), ERR);
	}

	private static int parse(String text, int fallback) {
		try {
			return Integer.parseInt(text.trim());
		} catch (NumberFormatException e) {
			return fallback;
		}
	}

	private static boolean hit(double mouseX, double mouseY, int x, int y, int w, int h) {
		return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
	}

	private void cycleMode() {
		model.mode = (model.mode + 1) % MODES.length;
		boolean preset = model.carrier.isBlank();
		for (String carrier : AjData.CARRIERS) preset |= carrier.equals(model.carrier);
		if (model.mob() && preset) model.carrier = AjData.CARRIERS[model.mode];
		syncAll();
	}

	private void cycleVariant() {
		AjData.Rig rig = currentRig();
		if (rig == null || rig.variants().isEmpty()) return;
		List<AjData.Variant> variants = rig.variants();
		int idx = -1;
		for (int i = 0; i < variants.size(); i++) if (variants.get(i).function().equals(model.variant)) idx = i;
		model.variant = idx + 1 >= variants.size() ? "" : variants.get(idx + 1).function();
		syncAll();
	}

	private void toggleSilent() {
		model.silent = !model.silent;
		syncAll();
	}

	private void toggleLoop() {
		model.loop = !model.loop;
		syncAll();
	}

	private void cycleAnim(int slot, int dir) {
		AjData.Rig rig = currentRig();
		if (rig == null) {
			error(K + "aj_no_rig");
			return;
		}
		List<AjData.Anim> anims = rig.anims();
		int idx = -1;
		for (int i = 0; i < anims.size(); i++) if (anims.get(i).name().equals(model.anims[slot])) idx = i;
		int next = Math.floorMod(idx + 1 + dir, anims.size() + 1) - 1;
		model.setAnim(slot, next < 0 ? null : anims.get(next));
		syncAll();
	}

	private void selectRig(AjData.Rig rig) {
		confirmDelete = null;
		if (!rig.id().equals(model.rig)) {
			model = new AjData();
			draft = model;
			generatedJson = null;
			model.use(rig);
			for (int slot = 0; slot < AjData.SLOTS; slot++) {
				for (String word : GUESS[slot]) {
					for (AjData.Anim a : rig.anims()) {
						if (model.anims[slot].isEmpty() && a.name().contains(word)) model.setAnim(slot, a);
					}
				}
			}
			model.id = suggestId(rig.id());
		} else {
			model.use(rig);
		}
		setStatus(Component.empty(), OK);
		syncAll();
	}

	private String suggestId(String rigId) {
		String name = rigId.substring(Math.max(rigId.lastIndexOf(':'), rigId.lastIndexOf('/')) + 1).replaceAll("[^a-z0-9_]", "_");
		if (name.length() > 21) name = name.substring(0, 21);
		if (!AjData.validId(name)) return "";
		if (!made.contains(name)) return name;
		for (int i = 2; i < 100; i++) {
			if (!made.contains(name + "_" + i)) return name + "_" + i;
		}
		return "";
	}

	private void requestScan() {
		if (!EditorAccess.canSendDatapack()) {
			error(K + "need_mod");
			return;
		}
		DatapackNet.send(DatapackOps.AJ_SCAN, "", "");
	}

	private boolean ready() {
		if (busy) {
			error(K + "aj_busy");
			return false;
		}
		if (!EditorAccess.canSendDatapack()) {
			error(K + "need_mod");
			return false;
		}
		return true;
	}

	private boolean canCommand() {
		String problem = EditorAccess.commandProblem();
		if (problem == null) return true;
		error(problem);
		return false;
	}

	private void generate(Runnable then) {
		if (!ready()) return;
		confirmDelete = null;
		String problem = AjPack.problem(model);
		if (problem == null && scanned && !more && currentRig() == null) problem = "aj_rig_missing";
		if (problem != null) {
			error(K + problem, model.rig);
			return;
		}
		String json = model.toJson();
		Set<String> ids = new TreeSet<>(made);
		ids.add(model.id);
		Map<String, String> files = new LinkedHashMap<>(AjPack.globalFiles(ids));
		files.putAll(AjPack.modelFiles(model, I18n.get(K + "aj_missing", model.rig)));
		files.put(AjPack.projectFile(model.id), json);
		for (Map.Entry<String, String> e : files.entrySet()) {
			if (!DatapackNet.fits(e.getValue())
				|| !EditorAccess.fits(DatapackOpPayload.CODEC, new DatapackOpPayload(DatapackOps.WRITE, e.getKey(), e.getValue()))) {
				error(K + "too_large");
				return;
			}
		}
		busy = true;
		busyTicks = 0;
		failKey = null;
		afterReload = then;
		pendingJson = json;
		pendingId = model.id;
		token = "bjaj#" + (++tokenSeq);
		for (Map.Entry<String, String> e : files.entrySet()) DatapackNet.send(DatapackOps.WRITE, e.getKey(), e.getValue());
		DatapackNet.send(DatapackOps.RELOAD, token, AjPack.PACK);
		setStatus(Component.translatable(K + "aj_generating"), UiTheme.cMuted());
	}

	private void finish(String errorKey) {
		busy = false;
		Runnable then = afterReload;
		afterReload = null;
		if (errorKey != null) {
			error(errorKey);
			return;
		}
		generatedJson = pendingJson;
		if (!made.contains(pendingId)) {
			made.add(pendingId);
			made.sort(null);
		}
		setStatus(Component.translatable(K + "aj_generated", pendingId), OK);
		if (then != null) then.run();
	}

	private void whenGenerated(Runnable action) {
		if (!ready()) return;
		if (model.toJson().equals(generatedJson) && made.contains(model.id)) action.run();
		else generate(action);
	}

	private void summon() {
		if (!canCommand()) return;
		String id = model.id;
		whenGenerated(() -> {
			if (EditorAccess.sendCommands(List.of(AjPack.summonAhead(id)))) setStatus(Component.translatable(K + "aj_summoned", id), OK);
		});
	}

	private void giveEgg() {
		if (!canCommand()) return;
		String id = model.id;
		whenGenerated(() -> {
			if (EditorAccess.sendCommands(List.of(AjPack.eggCommand(id)))) setStatus(Component.translatable(K + "aj_egg_given", id), OK);
		});
	}

	private void copyCommand() {
		String id = model.id;
		whenGenerated(() -> {
			GiveCommands.copy("/" + AjPack.summonCommand(id));
			setStatus(Component.translatable(K + "copied"), OK);
		});
	}

	private void clearEntities() {
		if (!ready() || !canCommand()) return;
		if (!made.contains(model.id)) {
			error(K + "aj_need_generate");
			return;
		}
		if (EditorAccess.sendCommands(List.of(AjPack.removeCommand(model.id)))) setStatus(Component.translatable(K + "aj_cleared", model.id), OK);
	}

	private void loadModel(String id) {
		if (busy) {
			error(K + "aj_busy");
			return;
		}
		confirmDelete = null;
		loadingId = id;
		loadingPath = AjPack.projectFile(id);
		DatapackNet.send(DatapackOps.READ, loadingPath, "");
	}

	private void deleteModel(String id) {
		if (!ready()) return;
		if (!id.equals(confirmDelete)) {
			confirmDelete = id;
			error(K + "aj_delete_confirm", id);
			return;
		}
		confirmDelete = null;
		if (EditorAccess.commandProblem() == null) EditorAccess.sendCommands(List.of(AjPack.removeCommand(id)));
		List<String> rest = new ArrayList<>(made);
		rest.remove(id);
		deletingId = id;
		if (rest.isEmpty()) {
			DatapackNet.send(DatapackOps.DELETE, AjPack.PACK, "");
		} else {
			for (Map.Entry<String, String> e : AjPack.globalFiles(rest).entrySet()) DatapackNet.send(DatapackOps.WRITE, e.getKey(), e.getValue());
			DatapackNet.send(DatapackOps.DELETE, AjPack.modelDir(id), "");
			DatapackNet.send(DatapackOps.DELETE, AjPack.projectFile(id), "");
		}
		made = rest;
		madeScroll = Mth.clamp(madeScroll, 0, Math.max(0, made.size() - madeRows));
		if (id.equals(model.id)) generatedJson = null;
	}

	public void handleNet(DatapackOpResultPayload payload) {
		if (payload == null) return;
		int kind = payload.kind();
		String path = payload.path() == null ? "" : payload.path();
		String extra = payload.extra() == null ? "" : payload.extra();
		if (kind == DatapackOps.KIND_AJ) {
			AjData.Scan scan = AjData.parseScan(extra);
			rigs = scan.rigs();
			made = new ArrayList<>(scan.made());
			more = scan.more();
			scanned = true;
			AjData.Rig rig = currentRig();
			if (rig != null) model.use(rig);
			if (!made.contains(model.id)) generatedJson = null;
			rigScroll = Mth.clamp(rigScroll, 0, Math.max(0, rigs.size() - rigRows));
			madeScroll = Mth.clamp(madeScroll, 0, Math.max(0, made.size() - madeRows));
			syncAll();
		} else if (kind == DatapackOps.KIND_FILE) {
			if (!path.equals(loadingPath)) return;
			loadingPath = null;
			AjData loaded = AjData.fromJson(extra);
			if (loaded == null) {
				error(K + "aj_load_failed", loadingId);
				return;
			}
			model = loaded;
			draft = model;
			generatedJson = loaded.toJson();
			AjData.Rig rig = currentRig();
			if (rig != null) model.use(rig);
			syncAll();
			setStatus(Component.translatable(K + "aj_loaded", loaded.id), OK);
		} else if (kind == DatapackOps.KIND_ERR) {
			String key = extra.isEmpty() ? K + "file_op_failed" : extra;
			if (loadingPath != null && path.equals(loadingPath)) {
				loadingPath = null;
				error(K + "aj_load_failed", loadingId);
			} else if (!busy) {
				deletingId = null;
				error(key);
				requestScan();
			} else if (path.equals(token)) {
				finish(failKey != null ? failKey : key);
			} else if (failKey == null) {
				failKey = key;
			}
		} else if (kind == DatapackOps.KIND_OK) {
			if (busy && path.equals(token)) {
				finish(failKey);
			} else if (deletingId != null && (K + "deleted").equals(extra)) {
				setStatus(Component.translatable(K + "aj_deleted", deletingId), OK);
				deletingId = null;
			}
		}
	}

	@Override
	public void tick() {
		super.tick();
		if (busy && ++busyTicks > 300) finish(K + "reload_failed");
	}

	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		super.renderBackground(g, mouseX, mouseY, partialTick);
		UiTheme.drawPanel(g, leftPos, topPos, panelW, panelH);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		super.render(g, mouseX, mouseY, partialTick);
		UiTheme.drawHeader(g, font, this.title, leftPos, topPos, panelW);
		int fy = topPos + panelH - FOOTER_H;
		g.fill(leftPos + 1, fy, leftPos + panelW - 1, fy + 1, 0xFF2B2C2D);
		drawRigs(g, mouseX, mouseY);
		drawMade(g, mouseX, mouseY);
		drawForm(g, mouseX, mouseY);
		UiTheme.drawButton(g, font, Component.translatable(K + "aj_generate"), genX, footerY, genW, 20, hit(mouseX, mouseY, genX, footerY, genW, 20), true, true);
		drawStatus(g);
		if (restX == Integer.MIN_VALUE) {
			restX = mouseX;
			restY = mouseY;
		}
		pointerMoved |= mouseX != restX || mouseY != restY;
		if (pointerMoved) drawTip(g, mouseX, mouseY);
	}

	private int rescanW() {
		return font.width(I18n.get(K + "aj_rescan"));
	}

	private boolean overRescan(double mouseX, double mouseY) {
		int w = rescanW();
		return hit(mouseX, mouseY, listX + LIST_W - w - 2, rigTop - 12, w + 2, 11);
	}

	private static String shortName(String rig) {
		return rig.startsWith("animated_java:") ? rig.substring(14) : rig;
	}

	private void drawRigs(GuiGraphics g, int mouseX, int mouseY) {
		int x0 = listX;
		int x1 = listX + LIST_W;
		int h = rigRows * ROW_H;
		String title = I18n.get(K + "aj_rigs") + (more ? " +" : "");
		UiTheme.clipLabel(g, font, title, x0, rigTop - 10, LIST_W - rescanW() - 6, UiTheme.cMuted());
		g.drawString(font, I18n.get(K + "aj_rescan"), x1 - rescanW(), rigTop - 10, overRescan(mouseX, mouseY) ? UiTheme.cLabel() : UiTheme.cMuted(), false);
		g.fill(x0, rigTop, x1, rigTop + h, UiTheme.cInputBg());
		if (rigs.isEmpty()) {
			UiTheme.clipLabel(g, font, I18n.get(K + (scanned ? "aj_rigs_empty" : "aj_scanning")), x0 + 4, rigTop + 2, LIST_W - 8, UiTheme.cMuted());
			return;
		}
		for (int i = 0; i < rigRows && i + rigScroll < rigs.size(); i++) {
			AjData.Rig rig = rigs.get(i + rigScroll);
			int y = rigTop + i * ROW_H;
			if (rig.id().equals(model.rig)) g.fill(x0, y, x1, y + ROW_H, 0xFF21262D);
			else if (hit(mouseX, mouseY, x0, y, LIST_W, ROW_H)) g.fill(x0, y, x1, y + ROW_H, 0xFF191A1B);
			UiTheme.clipLabel(g, font, shortName(rig.id()), x0 + 4, y + 2, LIST_W - 10, UiTheme.cLabel());
		}
		UiTheme.drawThinScrollBar(g, x1 - 3, rigTop, h, rigScroll, Math.max(0, rigs.size() - rigRows));
	}

	private void drawMade(GuiGraphics g, int mouseX, int mouseY) {
		int x0 = listX;
		int x1 = listX + LIST_W;
		int h = madeRows * ROW_H;
		UiTheme.clipLabel(g, font, I18n.get(K + "aj_made"), x0, madeTop - 10, LIST_W, UiTheme.cMuted());
		g.fill(x0, madeTop, x1, madeTop + h, UiTheme.cInputBg());
		if (made.isEmpty()) {
			UiTheme.clipLabel(g, font, I18n.get(K + "aj_made_empty"), x0 + 4, madeTop + 2, LIST_W - 8, UiTheme.cMuted());
			return;
		}
		for (int i = 0; i < madeRows && i + madeScroll < made.size(); i++) {
			String id = made.get(i + madeScroll);
			int y = madeTop + i * ROW_H;
			boolean hover = hit(mouseX, mouseY, x0, y, LIST_W, ROW_H);
			if (id.equals(model.id)) g.fill(x0, y, x1, y + ROW_H, 0xFF21262D);
			else if (hover) g.fill(x0, y, x1, y + ROW_H, 0xFF191A1B);
			UiTheme.clipLabel(g, font, id, x0 + 4, y + 2, LIST_W - 24, UiTheme.cLabel());
			boolean del = hover && mouseX >= x1 - 14;
			g.drawString(font, "\u00d7", x1 - 10, y + 2, del || id.equals(confirmDelete) ? ERR : UiTheme.cMuted(), false);
		}
		UiTheme.drawThinScrollBar(g, x1 - 3, madeTop, h, madeScroll, Math.max(0, made.size() - madeRows));
	}

	private void label(GuiGraphics g, String key, int x, int y) {
		g.drawString(font, I18n.get(K + key), x, y + 4, UiTheme.cLabel(), false);
	}

	private void drawForm(GuiGraphics g, int mouseX, int mouseY) {
		int y = bodyY;
		label(g, "aj_name", rightX, y);
		y += LINE;
		label(g, "aj_rig", rightX, y);
		int rigW = rightX + rightW - fieldX - (variantBtn.visible ? 102 : 0);
		if (model.rig.isEmpty()) UiTheme.clipLabel(g, font, I18n.get(K + "aj_rig_pick"), fieldX, y + 4, rigW, UiTheme.cMuted());
		else UiTheme.clipLabel(g, font, model.rig, fieldX, y + 4, rigW, scanned && currentRig() == null ? ERR : UiTheme.cLabel());
		y += LINE;
		if (model.mob()) {
			label(g, "aj_carrier", rightX, y);
			label(g, "aj_scale", scaleLabelX, y);
			label(g, "aj_nbt", rightX, y + LINE);
		}
		boolean active = currentRig() != null;
		for (int i = 0; i < AjData.SLOTS; i++) {
			if (!slotShown(i)) continue;
			label(g, SLOTS[i], slotX[i] - labelW - 6, slotY[i]);
			boolean hover = hit(mouseX, mouseY, slotX[i], slotY[i], slotW[i], FIELD_H);
			UiTheme.drawButton(g, font, Component.empty(), slotX[i], slotY[i], slotW[i], FIELD_H, hover, active);
			boolean none = model.anims[i].isEmpty();
			String name = none ? I18n.get(K + "aj_none") : model.anims[i];
			int textW = Math.min(font.width(name), slotW[i] - 8);
			int color = !active ? UiTheme.cButtonTextDisabled() : none ? UiTheme.cMuted() : UiTheme.cButtonText();
			UiTheme.clipLabel(g, font, name, slotX[i] + (slotW[i] - textW) / 2, slotY[i] + 4, slotW[i] - 8, color);
		}
	}

	private void drawStatus(GuiGraphics g) {
		String text = status.getString();
		int color = statusColor;
		if (text.isEmpty()) {
			color = UiTheme.cMuted();
			if (!scanned) text = I18n.get(K + "aj_scanning");
			else if (rigs.isEmpty()) text = I18n.get(K + "aj_hint_empty");
			else if (model.rig.isEmpty()) text = I18n.get(K + "aj_hint_pick");
			else text = I18n.get(K + (model.mob() ? "aj_hint_ticks" : "aj_hint_static"));
		}
		UiTheme.clipLabel(g, font, text, rightX, bodyY + bodyH - 9, rightW, color);
	}

	private void drawTip(GuiGraphics g, int mouseX, int mouseY) {
		if (hit(mouseX, mouseY, listX, rigTop, LIST_W, rigRows * ROW_H)) {
			int idx = (mouseY - rigTop) / ROW_H + rigScroll;
			if (idx < 0 || idx >= rigs.size()) return;
			AjData.Rig rig = rigs.get(idx);
			g.setComponentTooltipForNextFrame(font, List.of(Component.literal(rig.id()), Component.translatable(K + "aj_tip_pack", rig.pack()), Component.translatable(K + "aj_tip_anims", rig.anims().size())), mouseX, mouseY);
			return;
		}
		AjData.Rig rig = currentRig();
		if (rig == null) return;
		for (int i = 0; i < AjData.SLOTS; i++) {
			if (!slotShown(i) || !hit(mouseX, mouseY, slotX[i], slotY[i], slotW[i], FIELD_H)) continue;
			List<Component> lines = new ArrayList<>();
			AjData.Anim anim = rig.anim(model.anims[i]);
			if (anim != null) lines.add(Component.translatable(K + "aj_tip_length", anim.name(), anim.length()));
			lines.add(Component.translatable(K + "aj_tip_cycle"));
			g.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY);
			return;
		}
	}

	private boolean pressed(double mouseX, double mouseY, int button) {
		if (button != 0 && button != 1) return false;
		for (int i = 0; i < AjData.SLOTS; i++) {
			if (slotShown(i) && hit(mouseX, mouseY, slotX[i], slotY[i], slotW[i], FIELD_H)) {
				UiTheme.playClick();
				cycleAnim(i, button == 0 ? 1 : -1);
				return true;
			}
		}
		if (button != 0) return false;
		if (hit(mouseX, mouseY, genX, footerY, genW, 20)) {
			UiTheme.playClick();
			generate(null);
			return true;
		}
		if (overRescan(mouseX, mouseY)) {
			UiTheme.playClick();
			scanned = false;
			rigs = List.of();
			setStatus(Component.empty(), OK);
			requestScan();
			return true;
		}
		if (hit(mouseX, mouseY, listX, rigTop, LIST_W, rigRows * ROW_H)) {
			int idx = (int) ((mouseY - rigTop) / ROW_H) + rigScroll;
			if (idx < 0 || idx >= rigs.size()) return true;
			UiTheme.playClick();
			selectRig(rigs.get(idx));
			return true;
		}
		if (hit(mouseX, mouseY, listX, madeTop, LIST_W, madeRows * ROW_H)) {
			int idx = (int) ((mouseY - madeTop) / ROW_H) + madeScroll;
			if (idx < 0 || idx >= made.size()) return true;
			UiTheme.playClick();
			if (mouseX >= listX + LIST_W - 14) deleteModel(made.get(idx));
			else loadModel(made.get(idx));
			return true;
		}
		return false;
	}

	private boolean scrolled(double mouseX, double mouseY, double scrollY) {
		if (scrollY == 0) return false;
		int dir = scrollY > 0 ? -1 : 1;
		if (hit(mouseX, mouseY, listX, rigTop, LIST_W, rigRows * ROW_H)) {
			rigScroll = Mth.clamp(rigScroll + dir, 0, Math.max(0, rigs.size() - rigRows));
			return true;
		}
		if (hit(mouseX, mouseY, listX, madeTop, LIST_W, madeRows * ROW_H)) {
			madeScroll = Mth.clamp(madeScroll + dir, 0, Math.max(0, made.size() - madeRows));
			return true;
		}
		for (int i = 0; i < AjData.SLOTS; i++) {
			if (slotShown(i) && hit(mouseX, mouseY, slotX[i], slotY[i], slotW[i], FIELD_H)) {
				cycleAnim(i, dir);
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		double mouseX = event.x();
		double mouseY = event.y();
		int button = event.button();
		if (pressed(mouseX, mouseY, button)) return true;
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (scrolled(mouseX, mouseY, scrollY)) return true;
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
