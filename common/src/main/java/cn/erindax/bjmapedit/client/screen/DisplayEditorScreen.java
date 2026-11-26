package cn.erindax.bjmapedit.client.screen;

import cn.erindax.bjmapedit.client.DatapackNet;
import cn.erindax.bjmapedit.client.EditorAccess;
import cn.erindax.bjmapedit.client.widget.CommandSuggestionHost;
import cn.erindax.bjmapedit.client.widget.DisplayData;
import cn.erindax.bjmapedit.client.widget.DisplayPack;
import cn.erindax.bjmapedit.client.widget.GiveCommands;
import cn.erindax.bjmapedit.client.widget.InvPicker;
import cn.erindax.bjmapedit.client.widget.ItemNames;
import cn.erindax.bjmapedit.client.widget.SectionText;
import cn.erindax.bjmapedit.client.widget.ShopCompat;
import cn.erindax.bjmapedit.client.widget.SuggestionPopup;
import cn.erindax.bjmapedit.client.widget.UiTheme;
import cn.erindax.bjmapedit.networking.DatapackOps;
import cn.erindax.bjmapedit.networking.payload.DatapackOpPayload;
import cn.erindax.bjmapedit.networking.payload.DatapackOpResultPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.lwjgl.glfw.GLFW;

import java.lang.ref.WeakReference;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class DisplayEditorScreen extends Screen implements CommandSuggestionHost {
	private static final String K = "screen.bj_mapedit.";
	private static final int PAD = 8;
	private static final int ROW_H = 12;
	private static final int LINE = 20;
	private static final int FIELD_H = 16;
	private static final int FOOTER_H = 26;
	private static final int LIST_W = 116;
	private static final int OK = 0xFF7EE787;
	private static final int ERR = 0xFFFF7B72;
	private static final int HINT = 0x6E7681;
	private static final String[] PRESET_HEX = {
		"#FFFFFF", "#FFFF55", "#FFAA00", "#FF5555", "#AA0000", "#FF55FF", "#AA00AA", "#5555FF",
		"#55FFFF", "#00AAAA", "#55FF55", "#00AA00", "#AAAAAA", "#555555", "#000000"
	};

	private record BlockEntry(String id, String path, String name, String nameLower) {
		String label() {
			return name + "  " + id;
		}
	}

	private static DisplayData draft;
	private static String generatedJson;
	private static WeakReference<Object> draftOwner = new WeakReference<>(null);
	private static int tokenSeq;

	private DisplayData model;
	private List<String> made = new ArrayList<>();
	private boolean listed;
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
	private String targetUuid;
	private Component status = Component.empty();
	private int statusColor = OK;
	private int madeScroll;
	private int subTab;

	private int panelW;
	private int panelH;
	private int leftPos;
	private int topPos;
	private int bodyY;
	private int bodyH;
	private int step;
	private int listX;
	private int rightX;
	private int rightW;
	private int footerY;
	private int madeTop;
	private int madeRows;
	private int genX;
	private int genW;

	private Button newBtn;
	private Button readBtn;
	private Button applyNearBtn;
	private Button outlineBtn;
	private Button snapBtn;
	private Button respBtn;
	private Button bindInteractBtn;
	private Button brightBtn;
	private Button glowBtn;
	private Button scaleDownBtn;
	private Button scaleUpBtn;
	private Button richTextBtn;
	private Button addLineBtn;
	private Button delLineBtn;
	private Button boldBtn;
	private Button italicBtn;
	private Button underBtn;
	private Button strikeBtn;
	private Button obfBtn;
	private Button shadowBtn;
	private Button seeBtn;
	private Button pickInvBtn;
	private Button summonBtn;
	private Button eggBtn;
	private Button copyBtn;
	private Button clearBtn;

	private EditBox idField;
	private EditBox widthField;
	private EditBox heightField;
	private EditBox labelField;
	private EditBox cdField;
	private EditBox leftCmdField;
	private EditBox rightCmdField;
	private CommandSuggestions leftSuggestions;
	private CommandSuggestions rightSuggestions;

	private EditBox txField;
	private EditBox tyField;
	private EditBox tzField;
	private EditBox sxField;
	private EditBox syField;
	private EditBox szField;
	private EditBox yawField;
	private EditBox pitchField;
	private EditBox rollField;
	private EditBox textColorField;
	private EditBox lineWidthField;
	private EditBox bgHexField;
	private EditBox itemField;
	private EditBox blockField;

	private int kindX, kindY, kindW;
	private int subTab0X, subTab0W, subTab1X, subTab1W, subTabY;
	private int bbX, bbY, bbW;
	private int colorSwatchX, colorSwatchY;
	private int alignX, alignY, alignW;
	private int bgModeX, bgModeY, bgModeW;
	private int itemCtxX, itemCtxY, itemCtxW;
	private int itemSlotX, itemSlotY;

	private final InvPicker invPicker = new InvPicker();
	private List<ItemNames.Entry> itemHits = List.of();
	private int itemIdx;
	private int itemScroll;
	private List<BlockEntry> allBlocks;
	private String blockLang;
	private List<BlockEntry> blockHits = List.of();
	private int blockIdx;
	private int blockScroll;
	private int popupKind;
	private int restX = Integer.MIN_VALUE;
	private int restY;
	private boolean pointerMoved;

	public DisplayEditorScreen() {
		super(Component.translatable(K + "disp_title"));
		Object owner = Minecraft.getInstance().getConnection();
		if (draft == null || draftOwner.get() != owner) {
			draft = new DisplayData();
			draftOwner = new WeakReference<>(owner);
			generatedJson = null;
		}
		model = draft;
	}

	@Override
	protected void init() {
		layout();
		newBtn = button("disp_new", this::newModel, FIELD_H);
		readBtn = button("disp_read", this::readNearest, FIELD_H);
		applyNearBtn = button("disp_apply_live", this::applyToNearest, FIELD_H);
		outlineBtn = button("disp_outline", this::showOutlines, FIELD_H);

		idField = field(24, "aj_id_hint");
		idField.setFilter(s -> s.matches("[a-z0-9_]{0,24}"));
		idField.setResponder(s -> {
			if (!syncing) model.id = s;
		});
		snapBtn = button("disp_snap_on", () -> { model.centerSnap = !model.centerSnap; syncAll(); }, FIELD_H);

		widthField = floatField(false, s -> model.width = s);
		heightField = floatField(false, s -> model.height = s);
		respBtn = button("disp_resp_on", () -> { model.response = !model.response; syncAll(); }, FIELD_H);
		labelField = field(128, "disp_label_hint");
		labelField.setResponder(s -> {
			if (!syncing) model.label = s;
		});
		cdField = field(5, null);
		cdField.setFilter(s -> s.matches(DisplayData.DIGITS_5));
		cdField.setResponder(s -> {
			if (!syncing) model.cooldown = s;
		});
		leftCmdField = field(1024, "disp_left_hint");
		leftSuggestions = new CommandSuggestions(this.minecraft, this, leftCmdField, font, true, false, 1, 6, false, 0xD0000000);
		leftSuggestions.setAllowSuggestions(false);
		leftSuggestions.setAllowHiding(false);
		leftCmdField.setResponder(s -> {
			if (!syncing) model.leftCmd = s;
			refreshCmdSuggestions();
		});
		rightCmdField = field(1024, "disp_right_hint");
		rightSuggestions = new CommandSuggestions(this.minecraft, this, rightCmdField, font, true, false, 1, 6, false, 0xD0000000);
		rightSuggestions.setAllowSuggestions(false);
		rightSuggestions.setAllowHiding(false);
		rightCmdField.setResponder(s -> {
			if (!syncing) model.rightCmd = s;
			refreshCmdSuggestions();
		});
		bindInteractBtn = button("disp_bind_off", () -> { model.bindInteract = !model.bindInteract; syncAll(); }, FIELD_H);

		brightBtn = button("disp_bright_on", () -> { model.fullBright = !model.fullBright; syncAll(); }, FIELD_H);
		glowBtn = button("disp_glow_off", () -> { model.glowing = !model.glowing; syncAll(); }, FIELD_H);
		txField = floatField(true, s -> model.tx = s);
		tyField = floatField(true, s -> model.ty = s);
		tzField = floatField(true, s -> model.tz = s);
		sxField = floatField(false, s -> model.sx = s);
		syField = floatField(false, s -> model.sy = s);
		szField = floatField(false, s -> model.sz = s);
		scaleDownBtn = this.addRenderableWidget(Button.builder(Component.literal("-"), b -> uniformScale(-0.1F)).bounds(0, 0, 14, FIELD_H).build());
		scaleUpBtn = this.addRenderableWidget(Button.builder(Component.literal("+"), b -> uniformScale(0.1F)).bounds(0, 0, 14, FIELD_H).build());
		yawField = floatField(true, s -> model.yaw = s);
		pitchField = floatField(true, s -> model.pitch = s);
		rollField = floatField(true, s -> model.roll = s);

		richTextBtn = button("disp_edit_text", this::openRichTextEditor, FIELD_H);
		addLineBtn = button("disp_add_line", this::addTextLine, FIELD_H);
		delLineBtn = button("disp_del_line", this::removeTextLine, FIELD_H);
		textColorField = field(7, null);
		textColorField.setFilter(s -> s.matches(DisplayData.HEX_PAT));
		textColorField.setResponder(s -> {
			if (!syncing) model.textColor = s;
		});
		boldBtn = styleToggle("B", () -> { model.bold = !model.bold; syncAll(); });
		italicBtn = styleToggle("I", () -> { model.italic = !model.italic; syncAll(); });
		underBtn = styleToggle("U", () -> { model.underline = !model.underline; syncAll(); });
		strikeBtn = styleToggle("S", () -> { model.strikethrough = !model.strikethrough; syncAll(); });
		obfBtn = styleToggle("K", () -> { model.obfuscated = !model.obfuscated; syncAll(); });
		lineWidthField = field(4, null);
		lineWidthField.setFilter(s -> s.matches("\\d{0,4}"));
		lineWidthField.setResponder(s -> {
			if (!syncing) model.lineWidth = s;
		});
		shadowBtn = button("disp_shadow_on", () -> { model.textShadow = !model.textShadow; syncAll(); }, FIELD_H);
		seeBtn = button("disp_see_off", () -> { model.seeThrough = !model.seeThrough; syncAll(); }, FIELD_H);
		bgHexField = field(9, "disp_bg_hint");
		bgHexField.setFilter(s -> s.matches(DisplayData.HEX_PAT));
		bgHexField.setResponder(s -> {
			if (!syncing) model.bgHex = s;
		});

		itemField = field(96, "prop_item_hint");
		itemField.setResponder(s -> {
			if (syncing) return;
			model.itemSnbt = "";
			model.item = resolveItemInput(s.trim());
			updateItemHits();
		});
		pickInvBtn = button("prop_pick_inv", () -> invPicker.open(), FIELD_H);

		blockField = field(160, "disp_block_hint");
		blockField.setResponder(s -> {
			if (syncing) return;
			model.block = resolveBlockInput(s.trim());
			updateBlockHits();
		});

		summonBtn = button("aj_summon", this::summonNow, 20);
		eggBtn = button("disp_egg", this::giveEgg, 20);
		copyBtn = button("aj_copy", this::copyCommand, 20);
		clearBtn = button("aj_clear", this::clearEntities, 20);

		placeWidgets();
		syncAll();
		if (!opened) {
			opened = true;
			requestList();
		}
	}

	private EditBox field(int maxLength, String hintKey) {
		EditBox box = new EditBox(font, 0, 0, 40, FIELD_H, Component.empty());
		box.setMaxLength(maxLength);
		if (hintKey != null) box.setHint(Component.translatable(K + hintKey).withStyle(Style.EMPTY.withColor(HINT)));
		return this.addRenderableWidget(box);
	}

	private EditBox floatField(boolean signed, java.util.function.Consumer<String> onChange) {
		EditBox box = field(8, null);
		box.setFilter(s -> s.matches(signed ? DisplayData.FLOAT_PAT : DisplayData.UFLOAT_PAT));
		box.setResponder(s -> {
			if (!syncing) onChange.accept(s);
		});
		return box;
	}

	private Button button(String key, Runnable action, int height) {
		return this.addRenderableWidget(Button.builder(Component.translatable(K + key), b -> action.run()).bounds(0, 0, 40, height).build());
	}

	private Button styleToggle(String ch, Runnable action) {
		return this.addRenderableWidget(Button.builder(Component.literal(ch), b -> action.run()).bounds(0, 0, 16, FIELD_H).build());
	}

	private void layout() {
		panelW = Mth.clamp(this.width - 16, 400, 460);
		panelH = Mth.clamp(this.height - 8, 232, 298);
		leftPos = (this.width - panelW) / 2;
		topPos = Math.max(4, (this.height - panelH) / 2);
		bodyY = topPos + UiTheme.HEADER_H + 6;
		bodyH = panelH - UiTheme.HEADER_H - FOOTER_H - 10;
		step = bodyH < 194 ? 18 : LINE;
		listX = leftPos + PAD;
		rightX = listX + LIST_W + 10;
		rightW = leftPos + panelW - PAD - rightX;
		footerY = topPos + panelH - FOOTER_H + 3;
		madeTop = bodyY + 2 * step + 12;
		madeRows = Math.max(3, (bodyY + bodyH - 4 - madeTop) / ROW_H);
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
		int halfList = (LIST_W - 4) / 2;
		put(newBtn, listX, bodyY, halfList);
		put(readBtn, listX + halfList + 4, bodyY, LIST_W - halfList - 4);
		put(applyNearBtn, listX, bodyY + step, halfList + 8);
		put(outlineBtn, listX + halfList + 12, bodyY + step, LIST_W - halfList - 12);

		int x1 = rightX + rightW;
		int lw = widest("prop_id", "disp_width", "disp_label", "disp_left", "disp_right", "disp_offset", "disp_scale", "disp_rot", "disp_text", "disp_color", "disp_align", "disp_bg", "prop_item", "disp_ctx", "disp_block");
		int fx = rightX + lw + 6;

		int y = bodyY;
		int snapW = widest("disp_snap_on", "disp_snap_off") + 10;
		put(snapBtn, x1 - snapW, y, snapW);
		kindW = widest("disp_k_0", "disp_k_1", "disp_k_2", "disp_k_3") + 14;
		kindX = snapBtn.getX() - 4 - kindW;
		kindY = y;
		put(idField, fx, y, kindX - 4 - fx);

		y += step;
		subTabY = y;
		subTab0X = rightX;
		subTab0W = font.width(I18n.get(K + "disp_tab_look")) + 12;
		subTab1X = subTab0X + subTab0W + 3;
		subTab1W = font.width(I18n.get(K + "disp_tab_ev_on")) + 12;
		int glowW = widest("disp_glow_on", "disp_glow_off") + 10;
		int brW = widest("disp_bright_on", "disp_bright_off") + 10;
		put(glowBtn, x1 - glowW, y, glowW);
		put(brightBtn, glowBtn.getX() - 3 - brW, y, brW);
		bbX = subTab1X + subTab1W + 4;
		bbY = y;
		bbW = Math.max(36, brightBtn.getX() - 3 - bbX);

		boolean pureInteract = model.kind == DisplayData.KIND_INTERACT;
		int iy = pureInteract ? bodyY + step : bodyY + 2 * step;
		if (!pureInteract) {
			put(bindInteractBtn, rightX, iy, x1 - rightX);
			iy += step;
		}
		int respW = widest("disp_resp_on", "disp_resp_off") + 12;
		put(respBtn, x1 - respW, iy, respW);
		put(widthField, fx, iy, 42);
		int hLabelX = fx + 42 + 8;
		int hLw = widest("disp_height");
		put(heightField, hLabelX + hLw + 4, iy, 42);
		iy += step;
		int unitW = font.width(I18n.get(K + "aj_unit_tick")) + 3;
		put(cdField, x1 - unitW - 34, iy, 34);
		if (pureInteract) {
			int cdLw = widest("prop_cd") + 6;
			put(labelField, fx, iy, cdField.getX() - cdLw - fx);
		}
		iy += step;
		put(leftCmdField, fx, iy, x1 - fx);
		iy += step;
		put(rightCmdField, fx, iy, x1 - fx);

		int dy = bodyY + 2 * step;
		int vecGap = (x1 - fx - 30) / 3;
		put(txField, fx + 8, dy, vecGap - 10);
		put(tyField, fx + vecGap + 12, dy, vecGap - 10);
		put(tzField, fx + 2 * vecGap + 16, dy, x1 - (fx + 2 * vecGap + 16));
		dy += step;
		int scEnd = x1 - 32;
		int scGap = (scEnd - fx - 24) / 3;
		put(sxField, fx + 8, dy, scGap - 8);
		put(syField, fx + scGap + 12, dy, scGap - 8);
		put(szField, fx + 2 * scGap + 16, dy, scEnd - 4 - (fx + 2 * scGap + 16));
		put(scaleDownBtn, x1 - 30, dy, 14);
		put(scaleUpBtn, x1 - 14, dy, 14);
		dy += step;
		put(yawField, fx + 8, dy, vecGap - 10);
		put(pitchField, fx + vecGap + 12, dy, vecGap - 10);
		put(rollField, fx + 2 * vecGap + 16, dy, x1 - (fx + 2 * vecGap + 16));
		dy += step;

		int delLnW = widest("disp_del_line") + 10;
		int addLnW = widest("disp_add_line") + 10;
		put(delLineBtn, x1 - delLnW, dy, delLnW);
		put(addLineBtn, delLineBtn.getX() - 3 - addLnW, dy, addLnW);
		int richW = widest("disp_edit_text") + 12;
		put(richTextBtn, addLineBtn.getX() - 4 - richW, dy, richW);

		int invW = widest("prop_pick_inv") + 12;
		put(pickInvBtn, x1 - invW, dy, invW);
		put(itemField, fx, dy, x1 - invW - 4 - fx);
		put(blockField, fx, dy, x1 - fx);
		dy += step;

		colorSwatchX = fx;
		colorSwatchY = dy;
		put(textColorField, fx + 18, dy, 54);
		int stX = x1 - (5 * 17 - 1);
		put(boldBtn, stX, dy, 15);
		put(italicBtn, stX + 17, dy, 15);
		put(underBtn, stX + 34, dy, 15);
		put(strikeBtn, stX + 51, dy, 15);
		put(obfBtn, stX + 68, dy, 15);

		itemSlotX = x1 - 18;
		itemSlotY = dy - 1;
		itemCtxX = fx;
		itemCtxY = dy;
		itemCtxW = itemSlotX - 6 - fx;
		dy += step;

		int seeW = widest("disp_see_on", "disp_see_off") + 10;
		int shW = widest("disp_shadow_on", "disp_shadow_off") + 10;
		put(seeBtn, x1 - seeW, dy, seeW);
		put(shadowBtn, seeBtn.getX() - 3 - shW, dy, shW);
		put(lineWidthField, shadowBtn.getX() - 4 - 34, dy, 34);
		int lwLblW = widest("disp_line_w") + 4;
		alignX = fx;
		alignY = dy;
		alignW = Math.max(36, lineWidthField.getX() - lwLblW - 4 - fx);
		dy += step;

		bgModeW = widest("disp_bg_0", "disp_bg_1", "disp_bg_2") + 12;
		bgModeX = fx;
		bgModeY = dy;
		put(bgHexField, bgModeX + bgModeW + 4, dy, x1 - (bgModeX + bgModeW + 4));

		int bx = leftPos + panelW - PAD;
		genW = widest("aj_generate") + 16;
		genX = bx - genW;
		bx = genX - 4;
		for (Button b : List.of(summonBtn, eggBtn, copyBtn, clearBtn)) {
			int w = font.width(b.getMessage()) + 10;
			put(b, bx - w, footerY, w);
			bx -= w + 4;
		}
	}

	private static void setText(EditBox box, String text) {
		if (box.getValue().equals(text)) return;
		box.setValue(text);
		box.moveCursorToStart(false);
	}

	private void syncAll() {
		if (model.kind == DisplayData.KIND_INTERACT) subTab = 0;
		syncing = true;
		setText(idField, model.id);
		setText(widthField, model.width);
		setText(heightField, model.height);
		setText(labelField, model.label);
		setText(cdField, model.cooldown);
		setText(leftCmdField, model.leftCmd);
		setText(rightCmdField, model.rightCmd);
		setText(txField, model.tx);
		setText(tyField, model.ty);
		setText(tzField, model.tz);
		setText(sxField, model.sx);
		setText(syField, model.sy);
		setText(szField, model.sz);
		setText(yawField, model.yaw);
		setText(pitchField, model.pitch);
		setText(rollField, model.roll);
		setText(textColorField, model.textColor);
		setText(lineWidthField, model.lineWidth);
		setText(bgHexField, model.bgHex);
		setText(itemField, model.item);
		setText(blockField, model.block);
		syncing = false;

		snapBtn.setMessage(Component.translatable(K + (model.centerSnap ? "disp_snap_on" : "disp_snap_off")));
		respBtn.setMessage(Component.translatable(K + (model.response ? "disp_resp_on" : "disp_resp_off")));
		bindInteractBtn.setMessage(Component.translatable(K + (model.bindInteract ? "disp_bind_on" : "disp_bind_off")));
		brightBtn.setMessage(Component.translatable(K + (model.fullBright ? "disp_bright_on" : "disp_bright_off")));
		glowBtn.setMessage(Component.translatable(K + (model.glowing ? "disp_glow_on" : "disp_glow_off")));
		shadowBtn.setMessage(Component.translatable(K + (model.textShadow ? "disp_shadow_on" : "disp_shadow_off")));
		seeBtn.setMessage(Component.translatable(K + (model.seeThrough ? "disp_see_on" : "disp_see_off")));
		boldBtn.setMessage(Component.literal("B").withStyle(model.bold ? ChatFormatting.GOLD : ChatFormatting.GRAY));
		italicBtn.setMessage(Component.literal("I").withStyle(model.italic ? ChatFormatting.GOLD : ChatFormatting.GRAY));
		underBtn.setMessage(Component.literal("U").withStyle(model.underline ? ChatFormatting.GOLD : ChatFormatting.GRAY));
		strikeBtn.setMessage(Component.literal("S").withStyle(model.strikethrough ? ChatFormatting.GOLD : ChatFormatting.GRAY));
		obfBtn.setMessage(Component.literal("K").withStyle(model.obfuscated ? ChatFormatting.GOLD : ChatFormatting.GRAY));
		updateVisibility();
	}

	private void updateVisibility() {
		boolean pureInter = model.kind == DisplayData.KIND_INTERACT;
		boolean dispSubLook = !pureInter && subTab == 0;
		boolean dispSubEv = !pureInter && subTab == 1;
		boolean showInterFields = pureInter || (dispSubEv && model.bindInteract);

		brightBtn.visible = !pureInter;
		glowBtn.visible = !pureInter;
		bindInteractBtn.visible = dispSubEv;
		widthField.visible = showInterFields;
		heightField.visible = showInterFields;
		respBtn.visible = showInterFields;
		labelField.visible = pureInter;
		cdField.visible = showInterFields;
		leftCmdField.visible = showInterFields;
		rightCmdField.visible = showInterFields;

		txField.visible = dispSubLook;
		tyField.visible = dispSubLook;
		tzField.visible = dispSubLook;
		sxField.visible = dispSubLook;
		syField.visible = dispSubLook;
		szField.visible = dispSubLook;
		scaleDownBtn.visible = dispSubLook;
		scaleUpBtn.visible = dispSubLook;
		yawField.visible = dispSubLook;
		pitchField.visible = dispSubLook;
		rollField.visible = dispSubLook;

		boolean textLook = dispSubLook && model.kind == DisplayData.KIND_TEXT;
		richTextBtn.visible = textLook;
		addLineBtn.visible = textLook;
		delLineBtn.visible = textLook;
		delLineBtn.active = model.lineCount() > 1;
		textColorField.visible = textLook;
		boldBtn.visible = textLook;
		italicBtn.visible = textLook;
		underBtn.visible = textLook;
		strikeBtn.visible = textLook;
		obfBtn.visible = textLook;
		lineWidthField.visible = textLook;
		shadowBtn.visible = textLook;
		seeBtn.visible = textLook;
		bgHexField.visible = textLook && model.bgMode == DisplayData.BG_CUSTOM;

		boolean itemLook = dispSubLook && model.kind == DisplayData.KIND_ITEM;
		itemField.visible = itemLook;
		pickInvBtn.visible = itemLook;

		boolean blockLook = dispSubLook && model.kind == DisplayData.KIND_BLOCK;
		blockField.visible = blockLook;

		placeWidgets();
		refreshCmdSuggestions();
	}

	private void openRichTextEditor() {
		Minecraft.getInstance().setScreen(new TextEditorScreen(this, model.text, updated -> {
			if (updated != null) {
				model.text = updated.replace("\r", "");
				syncAll();
			}
		}, Component.translatable(K + "disp_edit_text")));
	}

	private void addTextLine() {
		model.text = (model.text == null ? "" : model.text) + "\n新的一行";
		syncAll();
	}

	private void removeTextLine() {
		if (model.text == null) return;
		int idx = model.text.lastIndexOf('\n');
		if (idx >= 0) {
			model.text = model.text.substring(0, idx);
			syncAll();
		}
	}

	private void uniformScale(float delta) {
		nudgeBox(sxField, delta, 0.05F, 64.0F);
		nudgeBox(syField, delta, 0.05F, 64.0F);
		nudgeBox(szField, delta, 0.05F, 64.0F);
	}

	private static void nudgeBox(EditBox box, float delta, float min, float max) {
		Float cur = DisplayPack.parseFloatRange(box.getValue(), -1000.0F, 1000.0F);
		float next = Mth.clamp((cur == null ? 0.0F : cur) + delta, min, max);
		String s = new BigDecimal(String.format(Locale.ROOT, "%.2f", next)).stripTrailingZeros().toPlainString();
		box.setValue("-0".equals(s) ? "0" : s);
	}

	private void cycleColorPreset(int dir) {
		int cur = -1;
		for (int i = 0; i < PRESET_HEX.length; i++) {
			if (PRESET_HEX[i].equalsIgnoreCase(model.textColor) || PRESET_HEX[i].substring(1).equalsIgnoreCase(model.textColor)) cur = i;
		}
		model.textColor = PRESET_HEX[Math.floorMod(cur + dir, PRESET_HEX.length)];
		syncAll();
	}

	private String resolveItemInput(String text) {
		for (ItemNames.Entry e : ItemNames.all()) {
			if (e.name().equalsIgnoreCase(text)) return e.idText();
		}
		return text;
	}

	private void updateItemHits() {
		String q = itemField.getValue().trim().toLowerCase(Locale.ROOT);
		List<ItemNames.Entry> out = new ArrayList<>();
		for (ItemNames.Entry e : ItemNames.all()) {
			if (e.nameLower().contains(q) || e.idLower().contains(q)) out.add(e);
		}
		itemHits = out;
		itemIdx = 0;
		itemScroll = 0;
		popupKind = 1;
	}

	private boolean itemOpen() {
		return popupKind == 1 && itemField.visible && itemField.isFocused() && !itemHits.isEmpty();
	}

	private int itemPopY() {
		return SuggestionPopup.listY(itemField, itemHits.size(), this.height);
	}

	private int popX() {
		return popupKind == 2 ? blockField.getX() : itemField.getX();
	}

	private int popW() {
		return rightX + rightW - popX();
	}

	private boolean overItemPop(double mouseX, double mouseY) {
		return itemOpen() && SuggestionPopup.contains(mouseX, mouseY, itemField.getX(), itemPopY(), rightX + rightW - itemField.getX(), itemHits.size());
	}

	private void pickItemHit(int idx) {
		model.itemSnbt = "";
		itemField.setValue(itemHits.get(idx).idText());
		itemHits = List.of();
		popupKind = 0;
	}

	private List<BlockEntry> blocks() {
		String lang = Minecraft.getInstance().getLanguageManager().getSelected();
		if (allBlocks != null && lang.equals(blockLang)) return allBlocks;
		List<BlockEntry> out = new ArrayList<>();
		for (ResourceLocation id : BuiltInRegistries.BLOCK.keySet()) {
			Block b = BuiltInRegistries.BLOCK.getValue(id);
			if (b == Blocks.AIR) continue;
			String name = b.getName().getString().trim();
			out.add(new BlockEntry(id.toString(), id.getPath(), name, name.toLowerCase(Locale.ROOT)));
		}
		out.sort(Comparator.comparing(BlockEntry::id));
		allBlocks = out;
		blockLang = lang;
		return allBlocks;
	}

	private String resolveBlockInput(String text) {
		if (text.indexOf('[') >= 0) return text;
		for (BlockEntry b : blocks()) {
			if (b.name.equalsIgnoreCase(text)) return b.id;
		}
		return text;
	}

	private void updateBlockHits() {
		String raw = blockField.getValue().trim();
		int br = raw.indexOf('[');
		String q = (br >= 0 ? raw.substring(0, br) : raw).toLowerCase(Locale.ROOT);
		List<BlockEntry> out = new ArrayList<>();
		for (BlockEntry b : blocks()) {
			if (b.nameLower.contains(q) || b.id.contains(q)) out.add(b);
		}
		blockHits = out;
		blockIdx = 0;
		blockScroll = 0;
		popupKind = 2;
	}

	private boolean blockOpen() {
		return popupKind == 2 && blockField.visible && blockField.isFocused() && !blockHits.isEmpty();
	}

	private int blockPopY() {
		return SuggestionPopup.listY(blockField, blockHits.size(), this.height);
	}

	private boolean overBlockPop(double mouseX, double mouseY) {
		return blockOpen() && SuggestionPopup.contains(mouseX, mouseY, blockField.getX(), blockPopY(), rightX + rightW - blockField.getX(), blockHits.size());
	}

	private void pickBlockHit(int idx) {
		blockField.setValue(blockHits.get(idx).id());
		blockHits = List.of();
		popupKind = 0;
	}

	private CommandSuggestions activeCmdSuggestions() {
		if (leftCmdField != null && leftCmdField.visible && leftCmdField.isFocused()) return leftSuggestions;
		if (rightCmdField != null && rightCmdField.visible && rightCmdField.isFocused()) return rightSuggestions;
		return null;
	}

	private EditBox activeCmdBox() {
		if (leftCmdField != null && leftCmdField.visible && leftCmdField.isFocused()) return leftCmdField;
		if (rightCmdField != null && rightCmdField.visible && rightCmdField.isFocused()) return rightCmdField;
		return null;
	}

	private void refreshCmdSuggestions() {
		updateOneSuggestion(leftSuggestions, leftCmdField);
		updateOneSuggestion(rightSuggestions, rightCmdField);
	}

	private static void updateOneSuggestion(CommandSuggestions cs, EditBox box) {
		if (cs == null || box == null) return;
		if (box.visible && box.isFocused()) {
			cs.setAllowSuggestions(true);
			try {
				cs.updateCommandInfo();
			} catch (Exception ignored) {}
			return;
		}
		cs.setAllowSuggestions(false);
		cs.hide();
		box.setSuggestion(null);
	}

	@Override
	public int[] placeCommandSuggestions(int x, int w, int count) {
		EditBox box = activeCmdBox();
		if (box == null) box = leftCmdField;
		int listH = Math.max(1, count) * 12;
		w = Mth.clamp(w, 20, Math.max(20, this.width - 4));
		x = Mth.clamp(x, 2, Math.max(2, this.width - 2 - w));
		int below = box.getY() + box.getHeight() + 1;
		int y = below + listH <= this.height - 2 ? below : Math.max(2, box.getY() - listH - 1);
		return new int[]{x, y, w};
	}

	@Override
	public void renderCommandUsage(GuiGraphics g, List<FormattedCharSequence> lines, int x, int w, int color, Font usageFont, EditBox box) {
		if (lines == null || lines.isEmpty() || box == null) return;
		int boxH = lines.size() * 12;
		w = Mth.clamp(w, 20, Math.max(20, this.width - 4));
		x = Mth.clamp(x, 2, Math.max(2, this.width - 2 - w));
		int y = box.getY() - boxH - 2;
		if (y < 2) y = box.getY() + box.getHeight() + 1;
		for (int i = 0; i < lines.size(); i++) {
			int yy = y + i * 12;
			g.fill(x - 1, yy, x + w, yy + 12, color);
			g.drawString(usageFont, lines.get(i), x, yy + 2, 0xFFE6EDF3);
		}
	}

	private void newModel() {
		confirmDelete = null;
		targetUuid = null;
		model = new DisplayData();
		model.id = suggestId("disp_1");
		draft = model;
		generatedJson = null;
		setStatus(Component.empty(), OK);
		syncAll();
	}

	private String suggestId(String base) {
		if (!made.contains(base)) return base;
		for (int i = 2; i < 100; i++) {
			if (!made.contains("disp_" + i)) return "disp_" + i;
		}
		return "";
	}

	private void readNearest() {
		Entity found = DisplayPack.findNearestTarget(-1);
		if (found == null || !DisplayPack.readFromEntity(found, model)) {
			error(K + "disp_none_nearby");
			return;
		}
		targetUuid = found.getStringUUID();
		subTab = 0;
		syncAll();
		setStatus(Component.translatable(K + "disp_read_ok", found.getType().getDescription()), OK);
	}

	private void applyToNearest() {
		if (!canCommand()) return;
		String bad = DisplayPack.problem(model);
		if (bad != null) {
			error(K + bad);
			return;
		}
		Entity target = null;
		if (targetUuid != null && this.minecraft != null && this.minecraft.level != null) {
			for (Entity e : this.minecraft.level.entitiesForRendering()) {
				if (targetUuid.equals(e.getStringUUID()) && DisplayPack.entityKind(e) == model.kind) {
					target = e;
					break;
				}
			}
		}
		if (target == null) target = DisplayPack.findNearestTarget(model.kind);
		if (target == null) {
			error(K + "disp_none_nearby");
			return;
		}
		targetUuid = target.getStringUUID();
		String nbt = model.kind == DisplayData.KIND_INTERACT ? DisplayPack.interactionNbt(model, false) : DisplayPack.displayNbt(model, false);
		if (EditorAccess.sendCommands(List.of("data merge entity " + targetUuid + " " + nbt))) {
			setStatus(Component.translatable(K + "disp_updated_live"), OK);
		}
	}

	private void showOutlines() {
		if (!canCommand()) return;
		if (EditorAccess.sendCommands(DisplayPack.outlineNearbyCommands())) {
			setStatus(Component.translatable(K + "disp_outlined"), OK);
		}
	}

	private void pickFromInventory(ItemStack stack) {
		String snbt = ShopCompat.stackSnbt(stack);
		if (snbt.isEmpty()) return;
		model.itemSnbt = snbt;
		model.item = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
		setStatus(Component.translatable(K + "prop_imported"), OK);
		syncAll();
	}

	private void setStatus(Component text, int color) {
		status = text;
		statusColor = color;
	}

	private void error(String key, Object... args) {
		setStatus(Component.translatable(key, args), ERR);
	}

	private static boolean hit(double mouseX, double mouseY, int x, int y, int w, int h) {
		return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
	}

	private static boolean over(AbstractWidget widget, double mouseX, double mouseY) {
		return widget.visible && hit(mouseX, mouseY, widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight());
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

	private void requestList() {
		if (!EditorAccess.canSendDatapack()) {
			error(K + "need_mod");
			return;
		}
		DatapackNet.send(DatapackOps.LIST, "", "");
	}

	private void generate(Runnable then) {
		if (!ready()) return;
		confirmDelete = null;
		String bad = DisplayPack.problem(model);
		if (bad != null) {
			error(K + bad);
			return;
		}
		String json = model.toJson();
		Set<String> ids = new TreeSet<>(made);
		ids.add(model.id);
		Map<String, String> files = new LinkedHashMap<>(DisplayPack.globalFiles(ids));
		files.putAll(DisplayPack.modelFiles(model));
		files.put(DisplayPack.projectFile(model.id), json);
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
		token = "bjdisp#" + (++tokenSeq);
		for (Map.Entry<String, String> e : files.entrySet()) DatapackNet.send(DatapackOps.WRITE, e.getKey(), e.getValue());
		DatapackNet.send(DatapackOps.RELOAD, token, DisplayPack.PACK);
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

	private void summonNow() {
		if (!canCommand()) return;
		String id = model.id;
		whenGenerated(() -> {
			if (EditorAccess.sendCommands(List.of(DisplayPack.summonFnCommand(id)))) {
				setStatus(Component.translatable(K + "aj_summoned", id), OK);
			}
		});
	}

	private void giveEgg() {
		if (!canCommand()) return;
		String id = model.id;
		whenGenerated(() -> {
			if (EditorAccess.sendCommands(List.of(DisplayPack.eggCommand(id)))) {
				setStatus(Component.translatable(K + "aj_egg_given", id), OK);
			}
		});
	}

	private void copyCommand() {
		String bad = DisplayPack.problem(model);
		if (bad != null) {
			error(K + bad);
			return;
		}
		if (model.needsDatapack()) {
			String id = model.id;
			whenGenerated(() -> {
				GiveCommands.copy("/" + DisplayPack.summonFnCommand(id));
				setStatus(Component.translatable(K + "copied"), OK);
			});
			return;
		}
		List<String> cmds = DisplayPack.directSummonCommands(model);
		GiveCommands.copy("/" + cmds.get(0));
		setStatus(Component.translatable(K + "copied"), OK);
	}

	private void clearEntities() {
		if (!canCommand()) return;
		if (EditorAccess.sendCommands(List.of("kill @e[tag=bjdisp.c." + model.id + "]"))) {
			setStatus(Component.translatable(K + "aj_cleared", model.id), OK);
		}
	}

	private void loadModel(String id) {
		if (busy) {
			error(K + "aj_busy");
			return;
		}
		confirmDelete = null;
		loadingId = id;
		loadingPath = DisplayPack.projectFile(id);
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
		if (EditorAccess.commandProblem() == null) EditorAccess.sendCommands(List.of("kill @e[tag=bjdisp.c." + id + "]"));
		List<String> rest = new ArrayList<>(made);
		rest.remove(id);
		deletingId = id;
		if (rest.isEmpty()) {
			DatapackNet.send(DatapackOps.DELETE, DisplayPack.PACK, "");
		} else {
			for (Map.Entry<String, String> e : DisplayPack.globalFiles(rest).entrySet()) DatapackNet.send(DatapackOps.WRITE, e.getKey(), e.getValue());
			DatapackNet.send(DatapackOps.DELETE, DisplayPack.modelDir(id), "");
			DatapackNet.send(DatapackOps.DELETE, DisplayPack.projectFile(id), "");
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
		if (kind == DatapackOps.KIND_TREE) {
			made = DisplayPack.dispIds(extra);
			listed = true;
			if (!made.contains(model.id)) generatedJson = null;
			madeScroll = Mth.clamp(madeScroll, 0, Math.max(0, made.size() - madeRows));
		} else if (kind == DatapackOps.KIND_FILE) {
			if (!path.equals(loadingPath)) return;
			loadingPath = null;
			DisplayData loaded = DisplayData.fromJson(extra);
			if (loaded == null) {
				error(K + "aj_load_failed", loadingId);
				return;
			}
			model = loaded;
			draft = model;
			subTab = 0;
			generatedJson = loaded.toJson();
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
				requestList();
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
		drawLeftRail(g, mouseX, mouseY);
		drawForm(g, mouseX, mouseY);
		UiTheme.drawButton(g, font, Component.translatable(K + "aj_generate"), genX, footerY, genW, 20, hit(mouseX, mouseY, genX, footerY, genW, 20), true, true);
		drawStatus(g);
		if (itemOpen()) {
			UiTheme.pushOverlay(g);
			SuggestionPopup.draw(g, font, itemField.getX(), itemPopY(), rightX + rightW - itemField.getX(), itemHits.size(), itemScroll, itemIdx, mouseX, mouseY, i -> itemHits.get(i).name() + "  " + itemHits.get(i).idText());
			UiTheme.popOverlay(g);
		} else if (blockOpen()) {
			UiTheme.pushOverlay(g);
			SuggestionPopup.draw(g, font, blockField.getX(), blockPopY(), rightX + rightW - blockField.getX(), blockHits.size(), blockScroll, blockIdx, mouseX, mouseY, i -> blockHits.get(i).label());
			UiTheme.popOverlay(g);
		} else {
			CommandSuggestions cs = activeCmdSuggestions();
			if (cs != null) {
				g.pose().pushPose();
				g.pose().translate(0.0F, 0.0F, 300.0F);
				cs.render(g, mouseX, mouseY);
				g.pose().popPose();
			}
		}
		if (restX == Integer.MIN_VALUE) {
			restX = mouseX;
			restY = mouseY;
		}
		pointerMoved |= mouseX != restX || mouseY != restY;
		if (invPicker.isOpen()) invPicker.render(g, font, this.width, this.height, mouseX, mouseY);
		else if (pointerMoved && !overItemPop(mouseX, mouseY) && !overBlockPop(mouseX, mouseY)) drawTip(g, mouseX, mouseY);
	}

	private void drawLeftRail(GuiGraphics g, int mouseX, int mouseY) {
		int x0 = listX;
		int x1 = listX + LIST_W;
		int h = madeRows * ROW_H;
		UiTheme.clipLabel(g, font, I18n.get(K + "aj_made"), x0, madeTop - 10, LIST_W, UiTheme.cMuted());
		g.fill(x0, madeTop, x1, madeTop + h, UiTheme.cInputBg());
		if (made.isEmpty()) {
			UiTheme.clipLabel(g, font, I18n.get(K + (listed ? "aj_made_empty" : "aj_scanning")), x0 + 4, madeTop + 2, LIST_W - 8, UiTheme.cMuted());
			return;
		}
		for (int i = 0; i < madeRows && i + madeScroll < made.size(); i++) {
			String id = made.get(i + madeScroll);
			int y = madeTop + i * ROW_H;
			boolean hover = hit(mouseX, mouseY, x0, y, LIST_W, ROW_H);
			if (id.equals(model.id)) g.fill(x0, y, x1, y + ROW_H, 0xFF21262D);
			else if (hover) g.fill(x0, y, x1, y + ROW_H, 0xFF191A1B);
			UiTheme.clipLabel(g, font, id, x0 + 4, y + 2, LIST_W - 22, UiTheme.cLabel());
			boolean del = hover && mouseX >= x1 - 14;
			g.drawString(font, "\u00d7", x1 - 10, y + 2, del || id.equals(confirmDelete) ? ERR : UiTheme.cMuted(), false);
		}
		UiTheme.drawThinScrollBar(g, x1 - 3, madeTop, h, madeScroll, Math.max(0, made.size() - madeRows));
	}

	private void label(GuiGraphics g, String key, int x, int y) {
		g.drawString(font, I18n.get(K + key), x, y + 4, UiTheme.cLabel(), false);
	}

	private void drawCycleBox(GuiGraphics g, int x, int y, int w, String text, int mouseX, int mouseY) {
		boolean hover = hit(mouseX, mouseY, x, y, w, FIELD_H);
		UiTheme.drawButton(g, font, Component.empty(), x, y, w, FIELD_H, hover, true);
		int textW = Math.min(font.width(text), w - 8);
		UiTheme.clipLabel(g, font, text, x + (w - textW) / 2, y + 4, Math.max(8, w - 8), UiTheme.cButtonText());
	}

	private void drawForm(GuiGraphics g, int mouseX, int mouseY) {
		label(g, "prop_id", rightX, idField.getY());
		drawCycleBox(g, kindX, kindY, kindW, I18n.get(K + "disp_k_" + model.kind), mouseX, mouseY);

		if (model.kind == DisplayData.KIND_INTERACT) {
			drawInteractLabels(g);
			int descY = rightCmdField.getY() + step;
			List<FormattedCharSequence> lines = font.split(Component.translatable(K + "disp_inter_desc"), rightW);
			for (int i = 0; i < lines.size() && descY + i * 11 + 9 <= bodyY + bodyH - 12; i++) {
				g.drawString(font, lines.get(i), rightX, descY + i * 11 + 2, UiTheme.cMuted(), false);
			}
			return;
		}

		boolean s0 = subTab == 0;
		g.fill(subTab0X, subTabY, subTab0X + subTab0W, subTabY + FIELD_H, s0 ? UiTheme.cAccent() : hit(mouseX, mouseY, subTab0X, subTabY, subTab0W, FIELD_H) ? UiTheme.cButtonHover() : UiTheme.cInputBg());
		g.drawCenteredString(font, I18n.get(K + "disp_tab_look"), subTab0X + subTab0W / 2, subTabY + 4, s0 ? 0xFFFFFFFF : UiTheme.cLabel());
		String evTab = I18n.get(K + (model.bindInteract ? "disp_tab_ev_on" : "disp_tab_ev_off"));
		g.fill(subTab1X, subTabY, subTab1X + subTab1W, subTabY + FIELD_H, !s0 ? UiTheme.cAccent() : hit(mouseX, mouseY, subTab1X, subTabY, subTab1W, FIELD_H) ? UiTheme.cButtonHover() : UiTheme.cInputBg());
		g.drawCenteredString(font, evTab, subTab1X + subTab1W / 2, subTabY + 4, !s0 ? 0xFFFFFFFF : UiTheme.cLabel());
		drawCycleBox(g, bbX, bbY, bbW, I18n.get(K + "disp_bb_" + model.billboard), mouseX, mouseY);

		if (!s0) {
			if (model.bindInteract) drawInteractLabels(g);
			return;
		}

		label(g, "disp_offset", rightX, txField.getY());
		g.drawString(font, "X", txField.getX() - 7, txField.getY() + 4, UiTheme.cMuted(), false);
		g.drawString(font, "Y", tyField.getX() - 7, tyField.getY() + 4, UiTheme.cMuted(), false);
		g.drawString(font, "Z", tzField.getX() - 7, tzField.getY() + 4, UiTheme.cMuted(), false);

		label(g, "disp_scale", rightX, sxField.getY());
		g.drawString(font, "X", sxField.getX() - 7, sxField.getY() + 4, UiTheme.cMuted(), false);
		g.drawString(font, "Y", syField.getX() - 7, syField.getY() + 4, UiTheme.cMuted(), false);
		g.drawString(font, "Z", szField.getX() - 7, szField.getY() + 4, UiTheme.cMuted(), false);

		label(g, "disp_rot", rightX, yawField.getY());
		g.drawString(font, "Y", yawField.getX() - 7, yawField.getY() + 4, UiTheme.cMuted(), false);
		g.drawString(font, "P", pitchField.getX() - 7, pitchField.getY() + 4, UiTheme.cMuted(), false);
		g.drawString(font, "R", rollField.getX() - 7, rollField.getY() + 4, UiTheme.cMuted(), false);

		if (model.kind == DisplayData.KIND_TEXT) {
			label(g, "disp_text", rightX, richTextBtn.getY());
			int lx = rightX + widest("disp_text") + 6;
			UiTheme.clipLabel(g, font, I18n.get(K + "disp_lines_n", model.lineCount()), lx, richTextBtn.getY() + 4, richTextBtn.getX() - 4 - lx, UiTheme.cMuted());

			label(g, "disp_color", rightX, colorSwatchY);
			Integer rgb = DisplayPack.parseRgb(model.textColor);
			int fill = 0xFF000000 | (rgb == null ? 0xFFFFFF : rgb);
			g.fill(colorSwatchX, colorSwatchY, colorSwatchX + 15, colorSwatchY + FIELD_H, 0xFF555555);
			g.fill(colorSwatchX + 1, colorSwatchY + 1, colorSwatchX + 14, colorSwatchY + FIELD_H - 1, fill);

			label(g, "disp_align", rightX, alignY);
			drawCycleBox(g, alignX, alignY, alignW, I18n.get(K + "disp_al_" + model.align), mouseX, mouseY);
			g.drawString(font, I18n.get(K + "disp_line_w"), lineWidthField.getX() - font.width(I18n.get(K + "disp_line_w")) - 3, alignY + 4, UiTheme.cLabel(), false);

			label(g, "disp_bg", rightX, bgModeY);
			drawCycleBox(g, bgModeX, bgModeY, bgModeW, I18n.get(K + "disp_bg_" + model.bgMode), mouseX, mouseY);
			if (model.bgMode != DisplayData.BG_CUSTOM) {
				int px = bgModeX + bgModeW + 6;
				Component preview = DisplayPack.buildTextComponent(model);
				String firstLine = preview.getString().split("\n", -1)[0];
				UiTheme.clipLabel(g, font, firstLine, px, bgModeY + 4, rightX + rightW - px, rgb == null ? 0xFFFFFFFF : fill);
			}
		} else if (model.kind == DisplayData.KIND_ITEM) {
			label(g, "prop_item", rightX, itemField.getY());
			label(g, "disp_ctx", rightX, itemCtxY);
			drawCycleBox(g, itemCtxX, itemCtxY, itemCtxW, I18n.get(K + "disp_ic_" + model.itemContext), mouseX, mouseY);
			UiTheme.drawSlot(g, itemSlotX, itemSlotY);
			ItemStack st = DisplayPack.buildItemStack(model);
			if (!st.isEmpty()) g.renderItem(st, itemSlotX + 1, itemSlotY + 1);
		} else if (model.kind == DisplayData.KIND_BLOCK) {
			label(g, "disp_block", rightX, blockField.getY());
		}
	}

	private void drawInteractLabels(GuiGraphics g) {
		label(g, "disp_width", rightX, widthField.getY());
		g.drawString(font, I18n.get(K + "disp_height"), widthField.getX() + 42 + 8, heightField.getY() + 4, UiTheme.cLabel(), false);
		if (labelField.visible) label(g, "disp_label", rightX, labelField.getY());
		g.drawString(font, I18n.get(K + "prop_cd"), cdField.getX() - font.width(I18n.get(K + "prop_cd")) - 4, cdField.getY() + 4, UiTheme.cLabel(), false);
		g.drawString(font, I18n.get(K + "aj_unit_tick"), cdField.getX() + cdField.getWidth() + 2, cdField.getY() + 4, UiTheme.cMuted(), false);
		label(g, "disp_left", rightX, leftCmdField.getY());
		label(g, "disp_right", rightX, rightCmdField.getY());
	}

	private void drawStatus(GuiGraphics g) {
		String text = status.getString();
		int color = statusColor;
		if (text.isEmpty()) {
			color = UiTheme.cMuted();
			text = I18n.get(K + "disp_hint_wheel");
		}
		UiTheme.clipLabel(g, font, text, rightX, bodyY + bodyH - 9, rightW, color);
	}

	private void drawTip(GuiGraphics g, int mouseX, int mouseY) {
		if (model.kind == DisplayData.KIND_ITEM && subTab == 0 && hit(mouseX, mouseY, itemSlotX, itemSlotY, 18, 18)) {
			ItemStack st = DisplayPack.buildItemStack(model);
			if (!st.isEmpty()) g.renderComponentTooltip(font, getTooltipFromItem(this.minecraft, st), mouseX, mouseY);
			return;
		}
		if (hit(mouseX, mouseY, kindX, kindY, kindW, FIELD_H)
			|| model.kind != DisplayData.KIND_INTERACT && hit(mouseX, mouseY, bbX, bbY, bbW, FIELD_H)) {
			g.renderComponentTooltip(font, List.of(Component.translatable(K + "aj_tip_cycle")), mouseX, mouseY);
		}
	}

	private boolean pressed(double mouseX, double mouseY, int button) {
		if (button != 0 && button != 1) return false;
		int dir = button == 0 ? 1 : -1;
		if (hit(mouseX, mouseY, kindX, kindY, kindW, FIELD_H)) {
			UiTheme.playClick();
			model.kind = Math.floorMod(model.kind + dir, DisplayData.KIND_COUNT);
			setFocused(null);
			syncAll();
			return true;
		}
		if (model.kind != DisplayData.KIND_INTERACT) {
			if (button == 0 && hit(mouseX, mouseY, subTab0X, subTabY, subTab0W, FIELD_H)) {
				UiTheme.playClick();
				subTab = 0;
				setFocused(null);
				syncAll();
				return true;
			}
			if (button == 0 && hit(mouseX, mouseY, subTab1X, subTabY, subTab1W, FIELD_H)) {
				UiTheme.playClick();
				subTab = 1;
				setFocused(null);
				syncAll();
				return true;
			}
			if (hit(mouseX, mouseY, bbX, bbY, bbW, FIELD_H)) {
				UiTheme.playClick();
				model.billboard = Math.floorMod(model.billboard + dir, DisplayData.BILLBOARDS.length);
				return true;
			}
			if (subTab == 0 && model.kind == DisplayData.KIND_TEXT) {
				if (hit(mouseX, mouseY, colorSwatchX, colorSwatchY, 15, FIELD_H)) {
					UiTheme.playClick();
					cycleColorPreset(dir);
					return true;
				}
				if (hit(mouseX, mouseY, alignX, alignY, alignW, FIELD_H)) {
					UiTheme.playClick();
					model.align = Math.floorMod(model.align + dir, DisplayData.ALIGNS.length);
					return true;
				}
				if (hit(mouseX, mouseY, bgModeX, bgModeY, bgModeW, FIELD_H)) {
					UiTheme.playClick();
					model.bgMode = Math.floorMod(model.bgMode + dir, 3);
					syncAll();
					return true;
				}
			}
			if (subTab == 0 && model.kind == DisplayData.KIND_ITEM) {
				if (hit(mouseX, mouseY, itemCtxX, itemCtxY, itemCtxW, FIELD_H)) {
					UiTheme.playClick();
					model.itemContext = Math.floorMod(model.itemContext + dir, DisplayData.ITEM_CONTEXTS.length);
					return true;
				}
				if (button == 0 && hit(mouseX, mouseY, itemSlotX, itemSlotY, 18, 18)) {
					UiTheme.playClick();
					invPicker.open();
					return true;
				}
			}
		}
		if (button != 0) return false;
		if (hit(mouseX, mouseY, genX, footerY, genW, 20)) {
			UiTheme.playClick();
			generate(null);
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
		int sign = scrollY > 0 ? 1 : -1;
		int cycleDir = -sign;
		boolean shift = Screen.hasShiftDown();

		if (hit(mouseX, mouseY, listX, madeTop, LIST_W, madeRows * ROW_H)) {
			madeScroll = Mth.clamp(madeScroll + cycleDir, 0, Math.max(0, made.size() - madeRows));
			return true;
		}
		if (hit(mouseX, mouseY, kindX, kindY, kindW, FIELD_H)) {
			model.kind = Math.floorMod(model.kind + cycleDir, DisplayData.KIND_COUNT);
			syncAll();
			return true;
		}
		for (EditBox b : List.of(widthField, heightField)) {
			if (over(b, mouseX, mouseY)) {
				nudgeBox(b, sign * (shift ? 0.5F : 0.1F), 0.05F, 32.0F);
				return true;
			}
		}
		for (EditBox b : List.of(txField, tyField, tzField)) {
			if (over(b, mouseX, mouseY)) {
				nudgeBox(b, sign * (shift ? 0.5F : 0.1F), -64.0F, 64.0F);
				return true;
			}
		}
		for (EditBox b : List.of(sxField, syField, szField)) {
			if (over(b, mouseX, mouseY)) {
				nudgeBox(b, sign * (shift ? 0.5F : 0.1F), 0.05F, 64.0F);
				return true;
			}
		}
		for (EditBox b : List.of(yawField, pitchField, rollField)) {
			if (over(b, mouseX, mouseY)) {
				nudgeBox(b, sign * (shift ? 45.0F : 5.0F), -360.0F, 360.0F);
				return true;
			}
		}
		if (model.kind != DisplayData.KIND_INTERACT) {
			if (hit(mouseX, mouseY, bbX, bbY, bbW, FIELD_H)) {
				model.billboard = Math.floorMod(model.billboard + cycleDir, DisplayData.BILLBOARDS.length);
				return true;
			}
			if (subTab == 0 && model.kind == DisplayData.KIND_TEXT) {
				if (hit(mouseX, mouseY, colorSwatchX, colorSwatchY, 15, FIELD_H)) {
					cycleColorPreset(cycleDir);
					return true;
				}
				if (hit(mouseX, mouseY, alignX, alignY, alignW, FIELD_H)) {
					model.align = Math.floorMod(model.align + cycleDir, DisplayData.ALIGNS.length);
					return true;
				}
				if (hit(mouseX, mouseY, bgModeX, bgModeY, bgModeW, FIELD_H)) {
					model.bgMode = Math.floorMod(model.bgMode + cycleDir, 3);
					syncAll();
					return true;
				}
			}
			if (subTab == 0 && model.kind == DisplayData.KIND_ITEM && hit(mouseX, mouseY, itemCtxX, itemCtxY, itemCtxW, FIELD_H)) {
				model.itemContext = Math.floorMod(model.itemContext + cycleDir, DisplayData.ITEM_CONTEXTS.length);
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (invPicker.isOpen()) {
			ItemStack picked = button == 0 ? invPicker.stackAt(this.width, this.height, mouseX, mouseY) : ItemStack.EMPTY;
			if (!picked.isEmpty()) {
				UiTheme.playClick();
				invPicker.close();
				pickFromInventory(picked);
			} else if (button == 1 || button == 0 && !invPicker.inPanel(this.width, this.height, mouseX, mouseY)) {
				invPicker.close();
			}
			return true;
		}
		if (button == 0 && overItemPop(mouseX, mouseY)) {
			int total = itemHits.size();
			int w = rightX + rightW - itemField.getX();
			Integer bar = SuggestionPopup.pressBar(itemField.getX(), itemPopY(), w, total, itemScroll, mouseX, mouseY);
			int idx = SuggestionPopup.hitIndex(mouseX, mouseY, itemField.getX(), itemPopY(), w, total, itemScroll);
			if (bar != null) itemScroll = bar;
			else if (idx >= 0) {
				UiTheme.playClick();
				pickItemHit(idx);
			}
			return true;
		}
		if (button == 0 && overBlockPop(mouseX, mouseY)) {
			int total = blockHits.size();
			int w = rightX + rightW - blockField.getX();
			Integer bar = SuggestionPopup.pressBar(blockField.getX(), blockPopY(), w, total, blockScroll, mouseX, mouseY);
			int idx = SuggestionPopup.hitIndex(mouseX, mouseY, blockField.getX(), blockPopY(), w, total, blockScroll);
			if (bar != null) blockScroll = bar;
			else if (idx >= 0) {
				UiTheme.playClick();
				pickBlockHit(idx);
			}
			return true;
		}
		CommandSuggestions cs = activeCmdSuggestions();
		if (cs != null && cs.mouseClicked(mouseX, mouseY, button)) return true;
		boolean handled = pressed(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button);
		if (button == 0 && over(itemField, mouseX, mouseY)) {
			blockHits = List.of();
			updateItemHits();
		} else if (button == 0 && over(blockField, mouseX, mouseY)) {
			itemHits = List.of();
			updateBlockHits();
		} else {
			itemHits = List.of();
			blockHits = List.of();
			popupKind = 0;
		}
		refreshCmdSuggestions();
		return handled;
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		if (button == 0 && SuggestionPopup.isDragging()) {
			if (popupKind == 1) itemScroll = SuggestionPopup.dragTo(mouseY);
			else if (popupKind == 2) blockScroll = SuggestionPopup.dragTo(mouseY);
			return true;
		}
		return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (button == 0) SuggestionPopup.endDrag();
		return super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (invPicker.isOpen()) return true;
		if (overItemPop(mouseX, mouseY)) {
			itemScroll = SuggestionPopup.scrollBy(itemScroll, itemHits.size(), scrollY);
			return true;
		}
		if (overBlockPop(mouseX, mouseY)) {
			blockScroll = SuggestionPopup.scrollBy(blockScroll, blockHits.size(), scrollY);
			return true;
		}
		CommandSuggestions cs = activeCmdSuggestions();
		if (cs != null && cs.mouseScrolled(Mth.clamp(scrollY, -1.0, 1.0))) return true;
		if (scrolled(mouseX, mouseY, scrollY)) return true;
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (invPicker.isOpen()) {
			if (keyCode == GLFW.GLFW_KEY_ESCAPE) invPicker.close();
			return true;
		}
		if (itemOpen()) {
			if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
				itemHits = List.of();
				popupKind = 0;
				return true;
			}
			if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER || keyCode == GLFW.GLFW_KEY_TAB) {
				pickItemHit(itemIdx);
				return true;
			}
			if (keyCode == GLFW.GLFW_KEY_DOWN || keyCode == GLFW.GLFW_KEY_UP) {
				itemIdx = Mth.clamp(itemIdx + (keyCode == GLFW.GLFW_KEY_DOWN ? 1 : -1), 0, itemHits.size() - 1);
				itemScroll = SuggestionPopup.keepVisible(itemIdx, itemScroll, itemHits.size());
				return true;
			}
		}
		if (blockOpen()) {
			if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
				blockHits = List.of();
				popupKind = 0;
				return true;
			}
			if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER || keyCode == GLFW.GLFW_KEY_TAB) {
				pickBlockHit(blockIdx);
				return true;
			}
			if (keyCode == GLFW.GLFW_KEY_DOWN || keyCode == GLFW.GLFW_KEY_UP) {
				blockIdx = Mth.clamp(blockIdx + (keyCode == GLFW.GLFW_KEY_DOWN ? 1 : -1), 0, blockHits.size() - 1);
				blockScroll = SuggestionPopup.keepVisible(blockIdx, blockScroll, blockHits.size());
				return true;
			}
		}
		CommandSuggestions cs = activeCmdSuggestions();
		if (cs != null && cs.keyPressed(keyCode, scanCode, modifiers)) return true;
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public boolean charTyped(char codePoint, int modifiers) {
		if (invPicker.isOpen()) return true;
		return super.charTyped(codePoint, modifiers);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
