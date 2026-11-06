package cn.erindax.bjmapedit.client.screen;

import net.minecraft.client.resources.language.I18n;
import cn.erindax.bjmapedit.client.widget.FrameStandTemplateStore;
import cn.erindax.bjmapedit.client.widget.GiveCommands;
import cn.erindax.bjmapedit.client.widget.ItemNames;
import cn.erindax.bjmapedit.client.widget.SafeIds;
import cn.erindax.bjmapedit.client.widget.SuggestionPopup;
import cn.erindax.bjmapedit.client.widget.TemplateDrag;
import cn.erindax.bjmapedit.client.widget.TemplateOrg;
import cn.erindax.bjmapedit.client.widget.RenameDeleteMenu;
import cn.erindax.bjmapedit.client.widget.SignData;
import cn.erindax.bjmapedit.client.widget.SignTexts;
import cn.erindax.bjmapedit.client.widget.TemplateRailUi;
import cn.erindax.bjmapedit.client.widget.UiTheme;
import cn.erindax.bjmapedit.client.EditorAccess;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Rotations;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.GlowItemFrame;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class FrameStandEditorScreen extends Screen {

	private int WIDTH = 500;
	private int HEIGHT = 380;
	private static final int LABEL_X = 12;
	private static final int ROW_H = 20;
	private static final int ACTION_H = 20;
	private static final int ACTION_GAP = 4;
	private static final int TEMPLATE_ROW_H = 22;
	private int INPUT_X = 88;
	private int INPUT_W = 160;
	private int RIGHT_X = 280;
	private int CONTENT_TOP = 50;
	private int CONTENT_BOTTOM;
	private int templateRailX = 8;
	private int templateRailW = 148;

	private int leftPos;
	private int topPos;
	private int tab;
	private int scrollOffset;
	private int maxScroll;
	private boolean draggingScroll;
	private double scrollGrabOffset;
	private int templateScroll;
	private int templateMaxScroll;
	private boolean draggingTemplateScroll;
	private double templateScrollGrab;
	private int selectedFrameTemplate = -1;
	private int selectedStandTemplate = -1;
	private String selectedFrameFolder = "";
	private String selectedStandFolder = "";
	private int selectedSignTemplate = -1;
	private String selectedSignFolder = "";
	private static final Set<String> collapsedFrameFolders = new HashSet<>();
	private static final Set<String> collapsedStandFolders = new HashSet<>();
	private static final Set<String> collapsedSignFolders = new HashSet<>();
	private boolean opened;
	private EditBox templateRenameBox;
	private int renamingTemplate = -1;
	private String renamingFolder = "";
	private final TemplateDrag templateDrag = new TemplateDrag();
	private final RenameDeleteMenu templateMenu = new RenameDeleteMenu();
	private TemplateOrg.Row templateMenuRow;

	private Button tabFrameBtn;
	private Button tabStandBtn;
	private Button tabSignBtn;
	private Button summonBtn;
	private Button giveBtn;
	private Button copyGiveBtn;
	private Button saveBtn;
	private int refreshX, refreshY;

	private Checkbox ifGlowBox;
	private Checkbox ifInvisibleBox;
	private Checkbox ifFixedBox;
	private Button ifFacingMinus;
	private Button ifFacingPlus;
	private Button ifRotMinus;
	private Button ifRotPlus;
	private EditBox ifItemField;
	private String innerKey;
	private ItemStack innerStack = ItemStack.EMPTY;

	private Checkbox asShowArmsBox;
	private Checkbox asNoBasePlateBox;
	private Checkbox asSmallBox;
	private Checkbox asInvisibleBox;
	private Checkbox asMarkerBox;
	private Checkbox asNoGravityBox;

	private EditBox[] poseFields = new EditBox[18];
	private Button[][] poseMinusBtns = new Button[6][3];
	private Button[][] posePlusBtns = new Button[6][3];

	private SignData signData = new SignData();
	private int signSide;
	private Button signReadBtn;
	private Button signWoodMinus;
	private Button signWoodPlus;
	private Checkbox signWaxBox;
	private Button signSideMinus;
	private Button signSidePlus;
	private Button signColorMinus;
	private Button signColorPlus;
	private Checkbox signGlowBox;
	private final EditBox[] signLineFields = new EditBox[SignData.LINES];
	private final EditBox[] signCmdFields = new EditBox[SignData.LINES];
	private Button signTextBtn;

	private boolean snapFrameGlow;
	private boolean snapFrameInvis;
	private boolean snapFrameFixed;
	private int frameFacing = 3;
	private int frameRotation;
	private String snapItem = "";
	private boolean snapArms;
	private boolean snapNoBase;
	private boolean snapSmall;
	private boolean snapStandInvis;
	private boolean snapMarker;
	private boolean snapNoGrav;
	private final String[] snapPoses = new String[18];

	private record ItemSuggestion(ResourceLocation id, String label) {}
	private List<ItemSuggestion> itemSuggestions = new ArrayList<>();
	private int itemSuggestionIdx = -1;
	private int suggestionScroll;

	private ArmorStand previewEntity;
	private ItemFrame previewFrame;
	private boolean previewFrameGlow;
	private float previewRotY;
	private float previewRotX;
	private boolean draggingPreview;
	private double dragStartX, dragStartY;
	private float dragStartRotY, dragStartRotX;
	private int heldPoseIdx = -1;
	private float heldPoseDelta;
	private int heldPoseTimer;
	private int previewX, previewY, previewW, previewH;

	private static final String[] POSE_PARTS = {"Head", "Body", "LeftArm", "RightArm", "LeftLeg", "RightLeg"};
	private static final String[] POSE_DISPLAY_KEYS = {"screen.bj_mapedit.pose.head", "screen.bj_mapedit.pose.body", "screen.bj_mapedit.pose.left_arm", "screen.bj_mapedit.pose.right_arm", "screen.bj_mapedit.pose.left_leg", "screen.bj_mapedit.pose.right_leg"};
	private static final String[] POSE_AXES = {"X", "Y", "Z"};
	private static final float[][] DEFAULT_POSES = {{0f, 0f, 0f}, {0f, 0f, 0f}, {-10f, 0f, -10f}, {-15f, 0f, 10f}, {-1f, 0f, -1f}, {1f, 0f, 1f}};
	private static final String[] FACING_KEYS = {"screen.bj_mapedit.facing.down", "screen.bj_mapedit.facing.up", "screen.bj_mapedit.facing.north", "screen.bj_mapedit.facing.south", "screen.bj_mapedit.facing.west", "screen.bj_mapedit.facing.east"};

	private static final class Session {
		int tab;
		int selectedFrame = -1;
		int selectedStand = -1;
		String selectedFrameFolder = "";
		String selectedStandFolder = "";
		boolean glow, frameInvis, fixed;
		int facing = 3;
		int rotation;
		String item = "";
		boolean arms, noBase, small, standInvis, marker, noGrav;
		String[] poses = new String[18];
		int selectedSign = -1;
		String selectedSignFolder = "";
		SignData sign = new SignData();
		int signSide;
	}

	private static Session session;

	public FrameStandEditorScreen() {
		super(Component.translatable("screen.bj_mapedit.entity_title"));
		for (int i = 0; i < snapPoses.length; i++) snapPoses[i] = "0.0";
	}

	@Override
	protected void init() {
		boolean first = !opened;
		opened = true;
		captureState();
		int snapScroll = scrollOffset;
		super.init();
		layoutPanel();

		tabFrameBtn = this.addRenderableWidget(Button.builder(
			Component.translatable("screen.bj_mapedit.item_frame"), b -> switchTab(0)
		).bounds(0, 0, 80, 18).build());
		tabStandBtn = this.addRenderableWidget(Button.builder(
			Component.translatable("screen.bj_mapedit.armor_stand"), b -> switchTab(1)
		).bounds(0, 0, 80, 18).build());
		tabSignBtn = this.addRenderableWidget(Button.builder(
			Component.translatable("screen.bj_mapedit.sign"), b -> switchTab(2)
		).bounds(0, 0, 80, 18).build());

		buildItemFrameTab();
		buildArmorStandTab();
		buildSignTab();

		summonBtn = this.addRenderableWidget(Button.builder(
			Component.translatable("screen.bj_mapedit.summon_plain"), b -> summonEntity()
		).bounds(0, 0, 70, 20).build());
		giveBtn = this.addRenderableWidget(Button.builder(
			Component.translatable("screen.bj_mapedit.give_item"), b -> giveItem()
		).bounds(0, 0, 70, 20).build());
		copyGiveBtn = this.addRenderableWidget(Button.builder(
			Component.translatable("screen.bj_mapedit.copy_cmd"), b -> copyGive()
		).bounds(0, 0, 70, 20).build());
		saveBtn = this.addRenderableWidget(Button.builder(
			Component.translatable("screen.bj_mapedit.save_template"), b -> saveTemplate()
		).bounds(0, 0, 70, 20).build());

		templateRenameBox = new EditBox(font, 0, -1000, 80, 16, Component.empty());
		templateRenameBox.setMaxLength(24);
		templateRenameBox.visible = false;
		this.addRenderableWidget(templateRenameBox);

		scrollOffset = snapScroll;
		applyTabVisibility();
		repositionAll();
		if (first && session != null) applySession(session);
		updateItemSuggestions();
	}

	private void switchTab(int next) {
		if (tab == next) return;
		commitTemplateRename();
		templateDrag.reset();
		tab = next;
		scrollOffset = 0;
		templateScroll = 0;
		heldPoseIdx = -1;
		itemSuggestions = List.of();
		itemSuggestionIdx = -1;
		setFocused(null);
		applyTabVisibility();
		repositionAll();
	}

	private void applyTabVisibility() {
		boolean frame = tab == 0;
		boolean stand = tab == 1;
		boolean sign = tab == 2;
		setShown(ifGlowBox, frame);
		setShown(ifInvisibleBox, frame);
		setShown(ifFixedBox, frame);
		setShown(ifFacingMinus, frame);
		setShown(ifFacingPlus, frame);
		setShown(ifRotMinus, frame);
		setShown(ifRotPlus, frame);
		setShown(ifItemField, frame);
		setShown(asShowArmsBox, stand);
		setShown(asNoBasePlateBox, stand);
		setShown(asSmallBox, stand);
		setShown(asInvisibleBox, stand);
		setShown(asMarkerBox, stand);
		setShown(asNoGravityBox, stand);
		for (int i = 0; i < 18; i++) setShown(poseFields[i], stand);
		for (int i = 0; i < 6; i++) {
			for (int j = 0; j < 3; j++) {
				setShown(poseMinusBtns[i][j], stand);
				setShown(posePlusBtns[i][j], stand);
			}
		}
		setShown(signReadBtn, sign);
		setShown(signWoodMinus, sign);
		setShown(signWoodPlus, sign);
		setShown(signWaxBox, sign);
		setShown(signSideMinus, sign);
		setShown(signSidePlus, sign);
		setShown(signColorMinus, sign);
		setShown(signColorPlus, sign);
		setShown(signGlowBox, sign);
		for (int i = 0; i < SignData.LINES; i++) {
			setShown(signLineFields[i], sign);
			setShown(signCmdFields[i], sign);
		}
		setShown(signTextBtn, sign);
		if (summonBtn != null) {
			summonBtn.setMessage(Component.translatable(sign ? "screen.bj_mapedit.sign_apply" : "screen.bj_mapedit.summon_plain"));
		}
	}

	private static void setShown(AbstractWidget w, boolean on) {
		if (w == null) return;
		w.visible = on;
		w.active = on;
		if (!on) w.setY(-1000);
	}

	private void layoutPanel() {
		int margin = 8;
		int gap = 8;
		int availW = Math.max(32, this.width - margin * 2);
		int availH = Math.max(32, this.height - margin * 2);
		templateRailW = Mth.clamp(availW * 20 / 100, 112, 220);
		if (availW < 420) {
			templateRailW = Mth.clamp(availW * 26 / 100, 88, 140);
		}
		WIDTH = Math.max(8, availW - templateRailW - gap);
		HEIGHT = availH;
		templateRailX = margin;
		leftPos = margin + templateRailW + gap;
		topPos = margin;
		CONTENT_TOP = UiTheme.HEADER_H + 8 + 24;
		CONTENT_BOTTOM = HEIGHT - 4;
		int labelW = 56;
		if (this.font != null) {
			labelW = Math.max(labelW, this.font.width(I18n.get("screen.bj_mapedit.frame_item")) + 10);
			labelW = Math.max(labelW, this.font.width(I18n.get("screen.bj_mapedit.nbt.ShowArms")) + 10);
			labelW = Math.max(labelW, this.font.width(I18n.get("screen.bj_mapedit.nbt.Marker")) + 10);
			if (tab == 2) {
				for (String key : new String[]{"sign_wood", "sign_waxed", "sign_side", "sign_color", "sign_glow", "sign_command"}) {
					labelW = Math.max(labelW, this.font.width(I18n.get("screen.bj_mapedit." + key)) + 10);
				}
				labelW = Math.max(labelW, this.font.width(I18n.get("screen.bj_mapedit.sign_line", SignData.LINES)) + 10);
			}
		}
		INPUT_X = LABEL_X + labelW;
		int previewNeed = 170;
		int minLeft = INPUT_X + 150;
		int rightW = Math.min(Math.max(150, WIDTH - minLeft - 8), Math.max(previewNeed, WIDTH * 38 / 100));
		if (WIDTH < 480) rightW = Math.max(110, WIDTH * 34 / 100);
		RIGHT_X = Math.max(INPUT_X + 72, WIDTH - rightW - 8);
		INPUT_W = Math.max(48, RIGHT_X - INPUT_X - 14);
		updateTemplateScroll();
		layoutRefreshIcon();
	}

	private void layoutRefreshIcon() {
		refreshX = UiTheme.headerIconX(leftPos, WIDTH);
		refreshY = UiTheme.headerIconY(topPos);
	}

	private void captureState() {
		if (ifGlowBox != null) snapFrameGlow = ifGlowBox.selected();
		if (ifInvisibleBox != null) snapFrameInvis = ifInvisibleBox.selected();
		if (ifFixedBox != null) snapFrameFixed = ifFixedBox.selected();
		if (ifItemField != null) snapItem = ifItemField.getValue();
		if (asShowArmsBox != null) snapArms = asShowArmsBox.selected();
		if (asNoBasePlateBox != null) snapNoBase = asNoBasePlateBox.selected();
		if (asSmallBox != null) snapSmall = asSmallBox.selected();
		if (asInvisibleBox != null) snapStandInvis = asInvisibleBox.selected();
		if (asMarkerBox != null) snapMarker = asMarkerBox.selected();
		if (asNoGravityBox != null) snapNoGrav = asNoGravityBox.selected();
		for (int i = 0; i < 18; i++) {
			if (poseFields[i] != null) snapPoses[i] = poseFields[i].getValue();
		}
		syncSignFromWidgets();
	}

	private void buildSignTab() {
		SignData.Side side = signData.side(signSide);
		signReadBtn = this.addRenderableWidget(Button.builder(
			Component.translatable("screen.bj_mapedit.sign_read"), b -> readTargetSign()
		).bounds(0, 0, 80, 16).build());
		signWoodMinus = this.addRenderableWidget(Button.builder(
			Component.literal("<"), b -> signData.wood = SignTexts.cycle(signData.wood, -1)
		).bounds(0, 0, 16, 16).build());
		signWoodPlus = this.addRenderableWidget(Button.builder(
			Component.literal(">"), b -> signData.wood = SignTexts.cycle(signData.wood, 1)
		).bounds(0, 0, 16, 16).build());
		signWaxBox = Checkbox.builder(Component.empty(), font).pos(0, 0).selected(signData.waxed).build();
		this.addRenderableWidget(signWaxBox);
		signSideMinus = this.addRenderableWidget(Button.builder(
			Component.literal("<"), b -> switchSignSide()
		).bounds(0, 0, 16, 16).build());
		signSidePlus = this.addRenderableWidget(Button.builder(
			Component.literal(">"), b -> switchSignSide()
		).bounds(0, 0, 16, 16).build());
		signColorMinus = this.addRenderableWidget(Button.builder(
			Component.literal("<"), b -> cycleSignColor(-1)
		).bounds(0, 0, 16, 16).build());
		signColorPlus = this.addRenderableWidget(Button.builder(
			Component.literal(">"), b -> cycleSignColor(1)
		).bounds(0, 0, 16, 16).build());
		signGlowBox = Checkbox.builder(Component.empty(), font).pos(0, 0).selected(side.glow).build();
		this.addRenderableWidget(signGlowBox);
		for (int i = 0; i < SignData.LINES; i++) {
			EditBox line = new EditBox(font, 0, 0, INPUT_W, 16, Component.empty());
			line.setValue(side.line(i));
			this.addRenderableWidget(line);
			signLineFields[i] = line;
			EditBox cmd = new EditBox(font, 0, 0, INPUT_W, 16, Component.empty());
			cmd.setValue(side.command(i));
			cmd.setHint(Component.literal("/say hi").withStyle(ChatFormatting.DARK_GRAY));
			this.addRenderableWidget(cmd);
			signCmdFields[i] = cmd;
		}
		signTextBtn = this.addRenderableWidget(Button.builder(
			Component.translatable("screen.bj_mapedit.edit_text"), b -> openSignTextEditor()
		).bounds(0, 0, 80, 16).build());
	}

	private void syncSignFromWidgets() {
		if (signLineFields[0] == null) return;
		SignData.Side side = signData.side(signSide);
		for (int i = 0; i < SignData.LINES; i++) {
			side.set(i, signLineFields[i].getValue(), signCmdFields[i].getValue());
		}
		if (signGlowBox != null) side.glow = signGlowBox.selected();
		if (signWaxBox != null) signData.waxed = signWaxBox.selected();
	}

	private void loadSignWidgets() {
		if (signLineFields[0] == null) return;
		SignData.Side side = signData.side(signSide);
		for (int i = 0; i < SignData.LINES; i++) {
			signLineFields[i].setValue(side.line(i));
			signCmdFields[i].setValue(side.command(i));
		}
		setCheckedIfNeeded(signGlowBox, side.glow);
		setCheckedIfNeeded(signWaxBox, signData.waxed);
	}

	private void switchSignSide() {
		syncSignFromWidgets();
		signSide = 1 - signSide;
		loadSignWidgets();
	}

	private void cycleSignColor(int delta) {
		SignData.Side side = signData.side(signSide);
		side.color = SignTexts.cycleDye(side.color, delta);
	}

	private void openSignTextEditor() {
		syncSignFromWidgets();
		SignData.Side side = signData.side(signSide);
		String[] lines = new String[SignData.LINES];
		for (int i = 0; i < SignData.LINES; i++) lines[i] = side.line(i);
		Minecraft.getInstance().setScreen(new TextEditorScreen(this, String.join("\n", lines), text -> {
			if (text == null || signLineFields[0] == null) return;
			String[] parts = text.split("\n", -1);
			for (int i = 0; i < SignData.LINES; i++) {
				signLineFields[i].setValue(i < parts.length ? parts[i] : "");
			}
		}, Component.translatable("screen.bj_mapedit.sign_edit_text")));
	}

	private BlockPos targetSign() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null) return null;
		HitResult hit = mc.player.pick(64.0, 1.0F, false);
		if (!(hit instanceof BlockHitResult block) || block.getType() != HitResult.Type.BLOCK) return null;
		BlockPos pos = block.getBlockPos();
		return mc.level.getBlockEntity(pos) instanceof SignBlockEntity ? pos : null;
	}

	private void notifySignMissing() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null) {
			mc.player.displayClientMessage(Component.translatable("screen.bj_mapedit.sign_not_targeted"), true);
		}
	}

	private void readTargetSign() {
		Minecraft mc = Minecraft.getInstance();
		BlockPos pos = targetSign();
		if (pos == null || !(mc.level.getBlockEntity(pos) instanceof SignBlockEntity sign)) {
			notifySignMissing();
			return;
		}
		SignData read = SignTexts.read(sign);
		if (read.wood.isEmpty()) read.wood = signData.wood;
		signData = read;
		loadSignWidgets();
	}

	private void applySignToTarget() {
		BlockPos pos = targetSign();
		if (pos == null) {
			notifySignMissing();
			return;
		}
		syncSignFromWidgets();
		String cmd = "data merge block " + pos.getX() + " " + pos.getY() + " " + pos.getZ() + " "
			+ GiveCommands.snbt(SignTexts.blockTag(signData, true));
		EditorAccess.sendCommands(List.of(cmd));
	}

	private void buildItemFrameTab() {
		ifGlowBox = Checkbox.builder(Component.empty(), font).pos(0, 0).selected(snapFrameGlow).build();
		ifInvisibleBox = Checkbox.builder(Component.empty(), font).pos(0, 0).selected(snapFrameInvis).build();
		ifFixedBox = Checkbox.builder(Component.empty(), font).pos(0, 0).selected(snapFrameFixed).build();
		this.addRenderableWidget(ifGlowBox);
		this.addRenderableWidget(ifInvisibleBox);
		this.addRenderableWidget(ifFixedBox);

		ifFacingMinus = this.addRenderableWidget(Button.builder(
			Component.literal("<"), b -> cycleFacing(-1)
		).bounds(0, 0, 16, 16).build());
		ifFacingPlus = this.addRenderableWidget(Button.builder(
			Component.literal(">"), b -> cycleFacing(1)
		).bounds(0, 0, 16, 16).build());
		ifRotMinus = this.addRenderableWidget(Button.builder(
			Component.literal("<"), b -> cycleRotation(-1)
		).bounds(0, 0, 16, 16).build());
		ifRotPlus = this.addRenderableWidget(Button.builder(
			Component.literal(">"), b -> cycleRotation(1)
		).bounds(0, 0, 16, 16).build());

		ifItemField = new EditBox(font, 0, 0, INPUT_W, 16, Component.empty());
		ifItemField.setMaxLength(128);
		ifItemField.setValue(snapItem);
		ifItemField.setResponder(s -> updateItemSuggestions());
		this.addRenderableWidget(ifItemField);
	}

	private void cycleFacing(int delta) {
		frameFacing = Math.floorMod(frameFacing + delta, 6);
	}

	private void cycleRotation(int delta) {
		frameRotation = Math.floorMod(frameRotation + delta, 8);
	}

	private void buildArmorStandTab() {
		asShowArmsBox = Checkbox.builder(Component.empty(), font).pos(0, 0).selected(snapArms).build();
		asNoBasePlateBox = Checkbox.builder(Component.empty(), font).pos(0, 0).selected(snapNoBase).build();
		asSmallBox = Checkbox.builder(Component.empty(), font).pos(0, 0).selected(snapSmall).build();
		asInvisibleBox = Checkbox.builder(Component.empty(), font).pos(0, 0).selected(snapStandInvis).build();
		asMarkerBox = Checkbox.builder(Component.empty(), font).pos(0, 0).selected(snapMarker).build();
		asNoGravityBox = Checkbox.builder(Component.empty(), font).pos(0, 0).selected(snapNoGrav).build();
		this.addRenderableWidget(asShowArmsBox);
		this.addRenderableWidget(asNoBasePlateBox);
		this.addRenderableWidget(asSmallBox);
		this.addRenderableWidget(asInvisibleBox);
		this.addRenderableWidget(asMarkerBox);
		this.addRenderableWidget(asNoGravityBox);

		for (int i = 0; i < 18; i++) {
			EditBox f = new EditBox(font, 0, 0, 32, 14, Component.empty());
			f.setValue(snapPoses[i] == null || snapPoses[i].isEmpty() ? "0.0" : snapPoses[i]);
			this.addRenderableWidget(f);
			poseFields[i] = f;
		}
		for (int i = 0; i < 6; i++) {
			for (int j = 0; j < 3; j++) {
				int idx = i * 3 + j;
				poseMinusBtns[i][j] = this.addRenderableWidget(Button.builder(
					Component.literal("<"), b -> adjustPose(idx, -1.0f)
				).bounds(0, 0, 14, 14).build());
				posePlusBtns[i][j] = this.addRenderableWidget(Button.builder(
					Component.literal(">"), b -> adjustPose(idx, 1.0f)
				).bounds(0, 0, 14, 14).build());
			}
		}
	}

	private void adjustPose(int idx, float delta) {
		if (poseFields[idx] == null) return;
		try {
			float val = Float.parseFloat(poseFields[idx].getValue().trim());
			val += delta;
			poseFields[idx].setValue(String.format(Locale.ROOT, "%.1f", val));
		} catch (NumberFormatException e) {
			poseFields[idx].setValue(String.format(Locale.ROOT, "%.1f", delta));
		}
	}

	private void repositionAll() {
		layoutPanel();
		if (tabFrameBtn == null || summonBtn == null) return;
		int tabY = topPos + UiTheme.HEADER_H + 6;
		int tabW = Mth.clamp((RIGHT_X - 24) / 3 - 4, 40, 72);
		Button[] tabs = {tabFrameBtn, tabStandBtn, tabSignBtn};
		for (int i = 0; i < tabs.length; i++) {
			tabs[i].setPosition(leftPos + 10 + i * (tabW + 4), tabY);
			tabs[i].setWidth(tabW);
			tabs[i].setHeight(18);
			tabs[i].active = tab != i;
		}

		int y = topPos + CONTENT_TOP + 2 - scrollOffset;
		int total = 0;
		if (tab == 2) {
			placeWide(signReadBtn, y);
			y += ROW_H;
			total += ROW_H;
			placeStepper(signWoodMinus, signWoodPlus, y);
			y += ROW_H;
			total += ROW_H;
			placeCheck(signWaxBox, leftPos + INPUT_X, y);
			y += ROW_H;
			total += ROW_H;
			placeStepper(signSideMinus, signSidePlus, y);
			y += ROW_H;
			total += ROW_H;
			placeStepper(signColorMinus, signColorPlus, y);
			y += ROW_H;
			total += ROW_H;
			placeCheck(signGlowBox, leftPos + INPUT_X, y);
			y += ROW_H;
			total += ROW_H;
			for (int i = 0; i < SignData.LINES; i++) {
				y += 4;
				total += 4;
				placeField(signLineFields[i], y);
				y += ROW_H;
				total += ROW_H;
				placeField(signCmdFields[i], y);
				y += ROW_H;
				total += ROW_H;
			}
			y += 4;
			total += 4;
			placeWide(signTextBtn, y);
			y += ROW_H;
			total += ROW_H;
		} else if (tab == 0) {
			int after = placeFlags(y, ifGlowBox, ifInvisibleBox, ifFixedBox);
			total += after - y;
			y = after;
			placeStepper(ifFacingMinus, ifFacingPlus, y);
			y += ROW_H;
			total += ROW_H;
			placeStepper(ifRotMinus, ifRotPlus, y);
			y += ROW_H;
			total += ROW_H;
			placeField(ifItemField, y);
			y += ROW_H;
			total += ROW_H;
		} else {
			int after = placeFlags(y, asShowArmsBox, asNoBasePlateBox, asSmallBox,
				asInvisibleBox, asMarkerBox, asNoGravityBox);
			total += after - y;
			y = after;
			y += 6;
			total += 6;
			y += ROW_H;
			total += ROW_H;
			int axisW = Math.max(48, (INPUT_W - 8) / 3);
			int boxW = Math.max(24, axisW - 32);
			for (int i = 0; i < 6; i++) {
				int rowY = y;
				for (int j = 0; j < 3; j++) {
					int axX = leftPos + INPUT_X + j * axisW;
					if (poseMinusBtns[i][j] != null) {
						poseMinusBtns[i][j].setPosition(axX, rowY);
						poseMinusBtns[i][j].setWidth(14);
						poseMinusBtns[i][j].setHeight(14);
					}
					if (poseFields[i * 3 + j] != null) {
						poseFields[i * 3 + j].setX(axX + 15);
						poseFields[i * 3 + j].setY(rowY);
						poseFields[i * 3 + j].setWidth(boxW);
					}
					if (posePlusBtns[i][j] != null) {
						posePlusBtns[i][j].setPosition(axX + 16 + boxW, rowY);
						posePlusBtns[i][j].setWidth(14);
						posePlusBtns[i][j].setHeight(14);
					}
				}
				y += ROW_H;
				total += ROW_H;
			}
		}

		maxScroll = Math.max(0, total + 8 - (CONTENT_BOTTOM - CONTENT_TOP));
		scrollOffset = Mth.clamp(scrollOffset, 0, maxScroll);
		layoutPreview();
		layoutActionButtons();
		syncTemplateRenameBox();
	}

	private int placeFlags(int y, Checkbox... boxes) {
		for (Checkbox box : boxes) {
			placeCheck(box, leftPos + INPUT_X, y);
			y += ROW_H;
		}
		return y;
	}

	private void placeCheck(Checkbox box, int x, int y) {
		if (box == null) return;
		box.setX(x);
		box.setY(y);
		box.setWidth(20);
		box.setHeight(20);
	}

	private void placeField(EditBox box, int y) {
		if (box == null) return;
		box.setX(leftPos + INPUT_X);
		box.setY(y);
		box.setWidth(INPUT_W);
	}

	private void placeWide(Button b, int y) {
		if (b == null) return;
		b.setPosition(leftPos + LABEL_X, y);
		b.setWidth(INPUT_X + INPUT_W - LABEL_X);
		b.setHeight(16);
	}

	private void placeStepper(Button minus, Button plus, int y) {
		if (minus == null || plus == null) return;
		minus.setPosition(leftPos + INPUT_X, y);
		minus.setWidth(16);
		minus.setHeight(16);
		plus.setPosition(leftPos + INPUT_X + INPUT_W - 16, y);
		plus.setWidth(16);
		plus.setHeight(16);
	}

	private void layoutPreview() {
		previewX = leftPos + RIGHT_X;
		previewY = topPos + CONTENT_TOP;
		previewW = Math.max(64, WIDTH - RIGHT_X - 8);
		int actionsH = ACTION_H * 4 + ACTION_GAP * 3 + 8;
		previewH = Math.max(72, CONTENT_BOTTOM - CONTENT_TOP - actionsH);
	}

	private void layoutActionButtons() {
		int x = previewX;
		int w = previewW;
		int y = previewY + previewH + 8;
		placeAction(copyGiveBtn, x, y, w);
		y += ACTION_H + ACTION_GAP;
		placeAction(giveBtn, x, y, w);
		y += ACTION_H + ACTION_GAP;
		placeAction(summonBtn, x, y, w);
		y += ACTION_H + ACTION_GAP;
		placeAction(saveBtn, x, y, w);
	}

	private void placeAction(Button b, int x, int y, int w) {
		if (b == null) return;
		b.setPosition(x, y);
		b.setWidth(w);
		b.setHeight(ACTION_H);
	}

	private boolean handleActionColumnClick(double mx, double my, int button) {
		if (mx < previewX || mx >= previewX + previewW
			|| my < previewY + previewH || my >= topPos + HEIGHT) {
			return false;
		}
		if (UiTheme.clickWidgets(mx, my, button, copyGiveBtn, giveBtn, summonBtn, saveBtn)) return true;
		if (button == 0) setFocused(null);
		return true;
	}

	private boolean isChrome(AbstractWidget w) {
		return w == tabFrameBtn || w == tabStandBtn || w == tabSignBtn || w == summonBtn
			|| w == giveBtn || w == copyGiveBtn || w == saveBtn || w == templateRenameBox;
	}

	private ItemStack buildGiveStack() {
		if (tab == 2) {
			syncSignFromWidgets();
			ItemStack stack = new ItemStack(SignTexts.item(signData.wood));
			CompoundTag data = SignTexts.blockTag(signData, false);
			data.putString("id", SignTexts.blockEntityId());
			stack.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(data));
			return stack;
		}
		if (tab == 0) {
			boolean glow = ifGlowBox != null && ifGlowBox.selected();
			ItemStack stack = new ItemStack(glow ? Items.GLOW_ITEM_FRAME : Items.ITEM_FRAME);
			CompoundTag data = new CompoundTag();
			data.putString("id", glow ? "minecraft:glow_item_frame" : "minecraft:item_frame");
			if (ifInvisibleBox != null && ifInvisibleBox.selected()) data.putBoolean("Invisible", true);
			if (ifFixedBox != null && ifFixedBox.selected()) data.putBoolean("Fixed", true);
			if (frameRotation != 0) data.putByte("ItemRotation", (byte) frameRotation);
			CompoundTag item = frameItemTag();
			if (item != null) data.put("Item", item);
			stack.set(DataComponents.ENTITY_DATA, CustomData.of(data));
			return stack;
		}
		ItemStack stack = new ItemStack(Items.ARMOR_STAND);
		CompoundTag data = buildStandTag(true);
		stack.set(DataComponents.ENTITY_DATA, CustomData.of(data));
		return stack;
	}

	private CompoundTag buildSummonTag() {
		return tab == 0 ? buildFrameTag(false) : buildStandTag(false);
	}

	private CompoundTag buildFrameTag(boolean forItem) {
		CompoundTag data = new CompoundTag();
		if (forItem) {
			boolean glow = ifGlowBox != null && ifGlowBox.selected();
			data.putString("id", glow ? "minecraft:glow_item_frame" : "minecraft:item_frame");
		}
		if (ifInvisibleBox != null && ifInvisibleBox.selected()) data.putBoolean("Invisible", true);
		if (ifFixedBox != null && ifFixedBox.selected()) data.putBoolean("Fixed", true);
		if (!forItem) data.putByte("Facing", (byte) frameFacing);
		if (frameRotation != 0 || !forItem) data.putByte("ItemRotation", (byte) frameRotation);
		CompoundTag item = frameItemTag();
		if (item != null) data.put("Item", item);
		return data;
	}

	private CompoundTag frameItemTag() {
		ItemStack inner = resolveInnerStack();
		if (inner.isEmpty()) return null;
		ResourceLocation id = BuiltInRegistries.ITEM.getKey(inner.getItem());
		CompoundTag item = new CompoundTag();
		item.putString("id", id.toString());
		item.putInt("count", 1);
		return item;
	}

	private ItemStack resolveInnerStack() {
		if (ifItemField == null) return ItemStack.EMPTY;
		String raw = ifItemField.getValue().trim();
		if (!raw.equals(innerKey)) {
			innerKey = raw;
			innerStack = computeInnerStack(raw);
		}
		return innerStack.copy();
	}

	private static ItemStack computeInnerStack(String raw) {
		if (raw.isEmpty()) return ItemStack.EMPTY;
		ResourceLocation id = SafeIds.tryParseItem(raw);
		if (id != null && BuiltInRegistries.ITEM.containsKey(id)) {
			return new ItemStack(BuiltInRegistries.ITEM.get(id));
		}
		String lower = raw.toLowerCase(Locale.ROOT);
		ResourceLocation unique = null;
		int hits = 0;
		for (ItemNames.Entry e : ItemNames.all()) {
			if (e.name().equals(raw) || e.nameLower().equals(lower)) {
				return new ItemStack(BuiltInRegistries.ITEM.get(e.id()));
			}
			if (e.nameLower().contains(lower) || e.idLower().contains(lower) || e.pathLower().contains(lower)) {
				hits++;
				unique = e.id();
			}
		}
		if (hits == 1 && unique != null) {
			return new ItemStack(BuiltInRegistries.ITEM.get(unique));
		}
		return ItemStack.EMPTY;
	}

	private CompoundTag buildStandTag(boolean forItem) {
		CompoundTag data = new CompoundTag();
		if (forItem) data.putString("id", "minecraft:armor_stand");
		if (asShowArmsBox != null && asShowArmsBox.selected()) data.putBoolean("ShowArms", true);
		if (asNoBasePlateBox != null && asNoBasePlateBox.selected()) data.putBoolean("NoBasePlate", true);
		if (asSmallBox != null && asSmallBox.selected()) data.putBoolean("Small", true);
		if (asInvisibleBox != null && asInvisibleBox.selected()) data.putBoolean("Invisible", true);
		if (asMarkerBox != null && asMarkerBox.selected()) data.putBoolean("Marker", true);
		if (asNoGravityBox != null && asNoGravityBox.selected()) data.putBoolean("NoGravity", true);
		CompoundTag pose = new CompoundTag();
		for (int i = 0; i < 6; i++) {
			float x = getPoseVal(i, 0);
			float y = getPoseVal(i, 1);
			float z = getPoseVal(i, 2);
			float[] def = DEFAULT_POSES[i];
			if (x == def[0] && y == def[1] && z == def[2]) continue;
			ListTag list = new ListTag();
			list.add(FloatTag.valueOf(x));
			list.add(FloatTag.valueOf(y));
			list.add(FloatTag.valueOf(z));
			pose.put(POSE_PARTS[i], list);
		}
		if (!pose.isEmpty()) data.put("Pose", pose);
		return data;
	}

	private void giveItem() {
		EditorAccess.giveItem(buildGiveStack());
	}

	private void copyGive() {
		GiveCommands.copy(GiveCommands.give(buildGiveStack()));
	}

	private void summonEntity() {
		if (tab == 2) {
			applySignToTarget();
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null) return;
		LookTarget at = lookTarget(mc);
		CompoundTag nbt = buildSummonTag();
		if (tab == 0) nbt.putByte("Facing", (byte) at.facing);
		String body = nbt.isEmpty() ? "{}" : GiveCommands.snbt(nbt);
		String cmd = "summon " + summonType() + " " + fmt(at.x) + " " + fmt(at.y) + " " + fmt(at.z) + " " + body;
		EditorAccess.sendCommands(List.of(cmd));
	}

	private record LookTarget(double x, double y, double z, int facing) {}

	private LookTarget lookTarget(Minecraft mc) {
		var player = mc.player;
		HitResult hit = player.pick(64.0, 1.0F, false);
		if (hit instanceof BlockHitResult block && block.getType() == HitResult.Type.BLOCK) {
			Direction face = block.getDirection();
			int facing = face.get3DDataValue();
			BlockPos support = block.getBlockPos();
			if (tab == 0) {
				BlockPos attach = support.relative(face);
				double ox = face.getStepX() * 0.46875;
				double oy = face.getStepY() * 0.46875;
				double oz = face.getStepZ() * 0.46875;
				Vec3 center = Vec3.atCenterOf(attach);
				return new LookTarget(center.x - ox, center.y - oy, center.z - oz, facing);
			}
			Vec3 loc = Vec3.atBottomCenterOf(support.relative(face));
			return new LookTarget(loc.x, loc.y, loc.z, facing);
		}
		Vec3 dest = player.getEyePosition(1.0F).add(player.getViewVector(1.0F).scale(3.0));
		return new LookTarget(dest.x, dest.y, dest.z, frameFacing);
	}

	private static String fmt(double v) {
		return String.format(Locale.ROOT, "%.3f", v);
	}

	private String summonType() {
		if (tab == 0) {
			return ifGlowBox != null && ifGlowBox.selected() ? "minecraft:glow_item_frame" : "minecraft:item_frame";
		}
		return "minecraft:armor_stand";
	}

	private float getPoseVal(int part, int axis) {
		if (poseFields[part * 3 + axis] == null) return 0f;
		return parseFloat(poseFields[part * 3 + axis].getValue().trim(), 0f);
	}

	private static float parseFloat(String s, float def) {
		try {
			return Float.parseFloat(s);
		} catch (NumberFormatException e) {
			return def;
		}
	}

	private int selectedTemplate() {
		return switch (tab) {
			case 0 -> selectedFrameTemplate;
			case 1 -> selectedStandTemplate;
			default -> selectedSignTemplate;
		};
	}

	private void setSelectedTemplate(int index) {
		switch (tab) {
			case 0 -> selectedFrameTemplate = index;
			case 1 -> selectedStandTemplate = index;
			default -> selectedSignTemplate = index;
		}
	}

	private String selectedFolder() {
		return switch (tab) {
			case 0 -> selectedFrameFolder;
			case 1 -> selectedStandFolder;
			default -> selectedSignFolder;
		};
	}

	private void setSelectedFolder(String folder) {
		String norm = TemplateOrg.norm(folder);
		switch (tab) {
			case 0 -> selectedFrameFolder = norm;
			case 1 -> selectedStandFolder = norm;
			default -> selectedSignFolder = norm;
		}
	}

	private Set<String> collapsedFolders() {
		return switch (tab) {
			case 0 -> collapsedFrameFolders;
			case 1 -> collapsedStandFolders;
			default -> collapsedSignFolders;
		};
	}

	private void saveTemplate() {
		commitTemplateRename();
		FrameStandTemplateStore.Entry entry = snapshotTemplate(defaultTemplateName());
		entry.folder = selectedFolder();
		FrameStandTemplateStore.add(tab, entry);
		String savedFolder = TemplateOrg.norm(entry.folder);
		if (!savedFolder.isEmpty()) collapsedFolders().remove(savedFolder);
		setSelectedTemplate(FrameStandTemplateStore.all(tab).size() - 1);
		templateScroll = TemplateRailUi.scrollToTemplate(templateRows(), FrameStandTemplateStore.all(tab).size() - 1, templateScroll, templateListH(), TEMPLATE_ROW_H);
		updateTemplateScroll();
	}

	private void loadTemplate(int index) {
		FrameStandTemplateStore.Entry entry = FrameStandTemplateStore.get(tab, index);
		if (entry == null) return;
		setSelectedTemplate(index);
		setSelectedFolder(entry.folder);
		applyTemplate(entry);
		repositionAll();
	}

	private FrameStandTemplateStore.Entry snapshotTemplate(String name) {
		FrameStandTemplateStore.Entry e = new FrameStandTemplateStore.Entry();
		e.name = name;
		if (tab == 2) {
			syncSignFromWidgets();
			e.sign = signData.copy();
			return e;
		}
		if (tab == 0) {
			e.glow = ifGlowBox != null && ifGlowBox.selected();
			e.frameInvis = ifInvisibleBox != null && ifInvisibleBox.selected();
			e.fixed = ifFixedBox != null && ifFixedBox.selected();
			e.facing = frameFacing;
			e.rotation = frameRotation;
			e.item = ifItemField != null ? ifItemField.getValue() : "";
			ItemStack inner = resolveInnerStack();
			if (!inner.isEmpty()) e.item = BuiltInRegistries.ITEM.getKey(inner.getItem()).toString();
			return e;
		}
		e.arms = asShowArmsBox != null && asShowArmsBox.selected();
		e.noBase = asNoBasePlateBox != null && asNoBasePlateBox.selected();
		e.small = asSmallBox != null && asSmallBox.selected();
		e.standInvis = asInvisibleBox != null && asInvisibleBox.selected();
		e.marker = asMarkerBox != null && asMarkerBox.selected();
		e.noGrav = asNoGravityBox != null && asNoGravityBox.selected();
		e.poses = new String[18];
		for (int i = 0; i < 18; i++) {
			e.poses[i] = poseFields[i] != null ? poseFields[i].getValue() : "0.0";
		}
		return e;
	}

	private void applyTemplate(FrameStandTemplateStore.Entry e) {
		if (tab == 2) {
			signData = e.sign != null ? e.sign.copy() : new SignData();
			loadSignWidgets();
			return;
		}
		if (tab == 0) {
			setCheckedIfNeeded(ifGlowBox, e.glow);
			setCheckedIfNeeded(ifInvisibleBox, e.frameInvis);
			setCheckedIfNeeded(ifFixedBox, e.fixed);
			frameFacing = Mth.clamp(e.facing, 0, 5);
			frameRotation = Mth.clamp(e.rotation, 0, 7);
			if (ifItemField != null) ifItemField.setValue(e.item == null ? "" : e.item);
			return;
		}
		setCheckedIfNeeded(asShowArmsBox, e.arms);
		setCheckedIfNeeded(asNoBasePlateBox, e.noBase);
		setCheckedIfNeeded(asSmallBox, e.small);
		setCheckedIfNeeded(asInvisibleBox, e.standInvis);
		setCheckedIfNeeded(asMarkerBox, e.marker);
		setCheckedIfNeeded(asNoGravityBox, e.noGrav);
		if (e.poses != null) {
			for (int i = 0; i < 18; i++) {
				if (poseFields[i] == null) continue;
				String v = i < e.poses.length && e.poses[i] != null && !e.poses[i].isEmpty() ? e.poses[i] : "0.0";
				poseFields[i].setValue(v);
			}
		}
	}

	private String defaultTemplateName() {
		String base;
		if (tab == 2) {
			syncSignFromWidgets();
			base = "";
			for (int i = 0; i < SignData.LINES && base.isEmpty(); i++) base = SignTexts.plain(signData.side(0).line(i));
			if (base.isEmpty()) base = I18n.get("screen.bj_mapedit.sign");
			if (base.length() > 24) base = base.substring(0, 24);
		} else if (tab == 0) {
			ItemStack inner = resolveInnerStack();
			base = inner.isEmpty() ? I18n.get("screen.bj_mapedit.item_frame") : inner.getHoverName().getString();
		} else {
			base = I18n.get("screen.bj_mapedit.armor_stand");
		}
		String name = base;
		int n = 2;
		while (templateNameTaken(name)) {
			name = base + n++;
		}
		return name;
	}

	private boolean templateNameTaken(String name) {
		String folder = selectedFolder();
		for (FrameStandTemplateStore.Entry e : FrameStandTemplateStore.all(tab)) {
			if (TemplateOrg.folderEq(e.folder, folder) && e.name != null && e.name.equals(name)) return true;
		}
		return false;
	}

	private void setCheckedIfNeeded(Checkbox box, boolean want) {
		if (box != null && box.selected() != want) box.onPress();
	}

	private Session takeSession() {
		Session s = new Session();
		s.tab = tab;
		s.selectedFrame = selectedFrameTemplate;
		s.selectedStand = selectedStandTemplate;
		s.selectedFrameFolder = selectedFrameFolder;
		s.selectedStandFolder = selectedStandFolder;
		s.glow = ifGlowBox != null && ifGlowBox.selected();
		s.frameInvis = ifInvisibleBox != null && ifInvisibleBox.selected();
		s.fixed = ifFixedBox != null && ifFixedBox.selected();
		s.facing = frameFacing;
		s.rotation = frameRotation;
		s.item = ifItemField != null ? ifItemField.getValue() : "";
		s.arms = asShowArmsBox != null && asShowArmsBox.selected();
		s.noBase = asNoBasePlateBox != null && asNoBasePlateBox.selected();
		s.small = asSmallBox != null && asSmallBox.selected();
		s.standInvis = asInvisibleBox != null && asInvisibleBox.selected();
		s.marker = asMarkerBox != null && asMarkerBox.selected();
		s.noGrav = asNoGravityBox != null && asNoGravityBox.selected();
		for (int i = 0; i < 18; i++) {
			s.poses[i] = poseFields[i] != null ? poseFields[i].getValue() : "0.0";
		}
		syncSignFromWidgets();
		s.selectedSign = selectedSignTemplate;
		s.selectedSignFolder = selectedSignFolder;
		s.sign = signData.copy();
		s.signSide = signSide;
		return s;
	}

	private void applySession(Session s) {
		if (s == null) return;
		tab = Mth.clamp(s.tab, 0, 2);
		selectedFrameTemplate = s.selectedFrame;
		selectedStandTemplate = s.selectedStand;
		selectedFrameFolder = TemplateOrg.norm(s.selectedFrameFolder);
		selectedStandFolder = TemplateOrg.norm(s.selectedStandFolder);
		selectedSignTemplate = s.selectedSign;
		selectedSignFolder = TemplateOrg.norm(s.selectedSignFolder);
		signData = s.sign != null ? s.sign.copy() : new SignData();
		signSide = s.signSide == 1 ? 1 : 0;
		loadSignWidgets();
		setCheckedIfNeeded(ifGlowBox, s.glow);
		setCheckedIfNeeded(ifInvisibleBox, s.frameInvis);
		setCheckedIfNeeded(ifFixedBox, s.fixed);
		frameFacing = Mth.clamp(s.facing, 0, 5);
		frameRotation = Mth.clamp(s.rotation, 0, 7);
		if (ifItemField != null) ifItemField.setValue(s.item == null ? "" : s.item);
		setCheckedIfNeeded(asShowArmsBox, s.arms);
		setCheckedIfNeeded(asNoBasePlateBox, s.noBase);
		setCheckedIfNeeded(asSmallBox, s.small);
		setCheckedIfNeeded(asInvisibleBox, s.standInvis);
		setCheckedIfNeeded(asMarkerBox, s.marker);
		setCheckedIfNeeded(asNoGravityBox, s.noGrav);
		if (s.poses != null) {
			for (int i = 0; i < 18; i++) {
				if (poseFields[i] == null) continue;
				String v = i < s.poses.length && s.poses[i] != null && !s.poses[i].isEmpty() ? s.poses[i] : "0.0";
				poseFields[i].setValue(v);
			}
		}
		applyTabVisibility();
		repositionAll();
	}

	@Override
	public void removed() {
		if (ifGlowBox != null) session = takeSession();
		super.removed();
	}

	private void resetFields() {
		if (tab == 2) {
			signData = new SignData();
			signSide = 0;
			loadSignWidgets();
		} else if (tab == 0) {
			setCheckedIfNeeded(ifGlowBox, false);
			setCheckedIfNeeded(ifInvisibleBox, false);
			setCheckedIfNeeded(ifFixedBox, false);
			frameFacing = 3;
			frameRotation = 0;
			if (ifItemField != null) ifItemField.setValue("");
			itemSuggestions = List.of();
			itemSuggestionIdx = -1;
		} else {
			setCheckedIfNeeded(asShowArmsBox, false);
			setCheckedIfNeeded(asNoBasePlateBox, false);
			setCheckedIfNeeded(asSmallBox, false);
			setCheckedIfNeeded(asInvisibleBox, false);
			setCheckedIfNeeded(asMarkerBox, false);
			setCheckedIfNeeded(asNoGravityBox, false);
			for (int i = 0; i < 18; i++) {
				if (poseFields[i] != null) poseFields[i].setValue("0.0");
			}
		}
		setSelectedTemplate(-1);
		setSelectedFolder("");
		previewRotY = 0;
		previewRotX = 0;
		scrollOffset = 0;
		repositionAll();
	}

	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		super.renderBackground(g, mouseX, mouseY, partialTick);
		UiTheme.drawPanel(g, leftPos, topPos, WIDTH, HEIGHT);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		this.renderBackground(g, mouseX, mouseY, partialTick);
		UiTheme.drawHeader(g, font, this.title, leftPos, topPos, WIDTH);
		layoutRefreshIcon();
		boolean refreshHover = UiTheme.hitIcon(refreshX, refreshY, mouseX, mouseY);
		UiTheme.drawRefreshIcon(g, refreshX, refreshY, refreshHover);
		drawTemplateRail(g, mouseX, mouseY);

		if (tabFrameBtn != null) tabFrameBtn.render(g, mouseX, mouseY, partialTick);
		if (tabStandBtn != null) tabStandBtn.render(g, mouseX, mouseY, partialTick);
		if (tabSignBtn != null) tabSignBtn.render(g, mouseX, mouseY, partialTick);

		g.enableScissor(leftPos + 1, topPos + CONTENT_TOP - 2, leftPos + RIGHT_X - 6, topPos + CONTENT_BOTTOM + 2);
		for (var child : this.children()) {
			if (child instanceof AbstractWidget w && !isChrome(w) && w.visible && !coversSuggestionList(w)) {
				w.render(g, mouseX, mouseY, partialTick);
			}
		}
		drawFormLabels(g);
		g.disableScissor();

		layoutPreview();
		layoutActionButtons();
		UiTheme.drawPreviewFrame(g, previewX, previewY, previewW, previewH);
		if (tab == 0) renderFramePreview(g);
		else if (tab == 1) renderPreviewEntity(g);
		else renderSignPreview(g);

		if (copyGiveBtn != null) copyGiveBtn.render(g, mouseX, mouseY, partialTick);
		if (giveBtn != null) giveBtn.render(g, mouseX, mouseY, partialTick);
		if (summonBtn != null) summonBtn.render(g, mouseX, mouseY, partialTick);
		if (saveBtn != null) saveBtn.render(g, mouseX, mouseY, partialTick);
		UiTheme.drawThinScrollBar(g, leftPos + RIGHT_X - 6, topPos + CONTENT_TOP, CONTENT_BOTTOM - CONTENT_TOP, scrollOffset, maxScroll);
		drawItemSuggestions(g, mouseX, mouseY);
		templateMenu.draw(g, font, mouseX, mouseY);
		if (refreshHover) {
			g.renderTooltip(font, Component.translatable("screen.bj_mapedit.reset"), mouseX, mouseY);
		}
	}

	private void drawFormLabels(GuiGraphics g) {
		if (tab == 2) {
			drawFieldLabel(g, signWoodMinus, I18n.get("screen.bj_mapedit.sign_wood"));
			drawStepperValue(g, signWoodMinus, signWoodPlus, new ItemStack(SignTexts.item(signData.wood)).getHoverName().getString());
			drawFieldLabel(g, signWaxBox, I18n.get("screen.bj_mapedit.sign_waxed"));
			drawFieldLabel(g, signSideMinus, I18n.get("screen.bj_mapedit.sign_side"));
			drawStepperValue(g, signSideMinus, signSidePlus, I18n.get(signSide == 0 ? "screen.bj_mapedit.sign_front" : "screen.bj_mapedit.sign_back"));
			drawFieldLabel(g, signColorMinus, I18n.get("screen.bj_mapedit.sign_color"));
			drawSignColorValue(g);
			drawFieldLabel(g, signGlowBox, I18n.get("screen.bj_mapedit.sign_glow"));
			for (int i = 0; i < SignData.LINES; i++) {
				drawFieldLabel(g, signLineFields[i], I18n.get("screen.bj_mapedit.sign_line", i + 1));
				drawFieldLabel(g, signCmdFields[i], I18n.get("screen.bj_mapedit.sign_command"));
			}
			return;
		}
		if (tab == 0) {
			drawFieldLabel(g, ifGlowBox, I18n.get("screen.bj_mapedit.frame_glow"));
			drawFieldLabel(g, ifInvisibleBox, I18n.get("screen.bj_mapedit.nbt.Invisible"));
			drawFieldLabel(g, ifFixedBox, I18n.get("screen.bj_mapedit.nbt.Fixed"));
			drawFieldLabel(g, ifFacingMinus, I18n.get("screen.bj_mapedit.facing"));
			drawStepperValue(g, ifFacingMinus, ifFacingPlus, I18n.get(FACING_KEYS[frameFacing]));
			drawFieldLabel(g, ifRotMinus, I18n.get("screen.bj_mapedit.rotation"));
			drawStepperValue(g, ifRotMinus, ifRotPlus, (frameRotation * 45) + "°");
			drawFieldLabel(g, ifItemField, I18n.get("screen.bj_mapedit.frame_item"));
			return;
		}
		drawFieldLabel(g, asShowArmsBox, I18n.get("screen.bj_mapedit.nbt.ShowArms"));
		drawFieldLabel(g, asNoBasePlateBox, I18n.get("screen.bj_mapedit.nbt.NoBasePlate"));
		drawFieldLabel(g, asSmallBox, I18n.get("screen.bj_mapedit.nbt.Small"));
		drawFieldLabel(g, asInvisibleBox, I18n.get("screen.bj_mapedit.nbt.Invisible"));
		drawFieldLabel(g, asMarkerBox, I18n.get("screen.bj_mapedit.nbt.Marker"));
		drawFieldLabel(g, asNoGravityBox, I18n.get("screen.bj_mapedit.nbt.NoGravity"));
		if (asNoGravityBox != null) {
			int poseHeadY = asNoGravityBox.getY() + ROW_H + 8;
			if (poseHeadY + 10 > topPos + CONTENT_TOP && poseHeadY < topPos + CONTENT_BOTTOM) {
				UiTheme.muted(g, font, I18n.get("screen.bj_mapedit.pose"), leftPos + LABEL_X, poseHeadY + 2);
				int axisW = Math.max(48, (INPUT_W - 8) / 3);
				for (int j = 0; j < 3; j++) {
					g.drawCenteredString(font, POSE_AXES[j], leftPos + INPUT_X + j * axisW + axisW / 2, poseHeadY + 2, UiTheme.cMuted());
				}
			}
		}
		for (int i = 0; i < 6; i++) {
			if (poseFields[i * 3] == null) continue;
			drawFieldLabel(g, poseFields[i * 3], I18n.get(POSE_DISPLAY_KEYS[i]));
		}
	}

	private void drawFieldLabel(GuiGraphics g, AbstractWidget box, String text) {
		if (box == null) return;
		int y = box.getY() + 4;
		if (y + 10 > topPos + CONTENT_TOP && y < topPos + CONTENT_BOTTOM) {
			UiTheme.label(g, font, text, leftPos + LABEL_X, y);
		}
	}

	private void drawStepperValue(GuiGraphics g, Button minus, Button plus, String text) {
		if (minus == null || plus == null) return;
		int y = minus.getY() + 4;
		if (y + 10 > topPos + CONTENT_TOP && y < topPos + CONTENT_BOTTOM) {
			int cx = (minus.getX() + minus.getWidth() + plus.getX()) / 2;
			g.drawCenteredString(font, text, cx, y, UiTheme.cLabel());
		}
	}

	private void drawSignColorValue(GuiGraphics g) {
		if (signColorMinus == null || signColorPlus == null) return;
		int y = signColorMinus.getY() + 4;
		if (y + 10 <= topPos + CONTENT_TOP || y >= topPos + CONTENT_BOTTOM) return;
		DyeColor dye = SignTexts.dye(signData.side(signSide).color);
		String name = I18n.get("color.minecraft." + dye.getName());
		int w = 11 + font.width(name);
		int x = (signColorMinus.getX() + signColorMinus.getWidth() + signColorPlus.getX()) / 2 - w / 2;
		g.fill(x, y - 1, x + 9, y + 8, 0xFF8B949E);
		g.fill(x + 1, y, x + 8, y + 7, SignTexts.textColor(dye));
		g.drawString(font, name, x + 11, y, UiTheme.cLabel(), false);
	}

	private void renderSignPreview(GuiGraphics g) {
		syncSignFromWidgets();
		SignData.Side side = signData.side(signSide);
		int availW = previewW - 16;
		int availH = previewH - 26;
		float p = Mth.clamp(Math.min(availW / 24f, availH / 26f), 1.5f, 6f);
		float cx = previewX + previewW / 2f;
		float cy = previewY + 16 + (availH - 26f * p) / 2f + 6f * p;
		g.enableScissor(previewX + 1, previewY + 1, previewX + previewW - 1, previewY + previewH - 1);
		ResourceLocation tex = SignTexts.texture(SignTexts.item(signData.wood));
		g.pose().pushPose();
		g.pose().translate(cx, cy, 0f);
		g.pose().scale(p, p, 1f);
		g.blit(tex, -12, -6, 2f, 2f, 24, 12, 64, 32);
		g.blit(tex, -1, 6, 2f, 16f, 2, 14, 64, 32);
		g.pose().popPose();

		DyeColor dye = SignTexts.dye(side.color);
		int dark = SignTexts.darkColor(dye, side.glow);
		int main = side.glow ? SignTexts.textColor(dye) : dark;
		float t = p / 4f;
		g.pose().pushPose();
		g.pose().translate(cx, cy, 1f);
		g.pose().scale(t, t, 1f);
		int top = -SignData.LINES * SignTexts.LINE_HEIGHT / 2;
		for (int i = 0; i < SignData.LINES; i++) {
			FormattedCharSequence seq = SignTexts.renderLine(font, side.line(i));
			int x = -font.width(seq) / 2;
			int y = top + i * SignTexts.LINE_HEIGHT;
			if (side.glow) {
				FormattedCharSequence outline = SignTexts.recolor(seq, dark);
				for (int dx = -1; dx <= 1; dx++) {
					for (int dy = -1; dy <= 1; dy++) {
						if (dx != 0 || dy != 0) g.drawString(font, outline, x + dx, y + dy, dark, false);
					}
				}
			}
			g.drawString(font, seq, x, y, main, false);
		}
		g.pose().popPose();
		g.disableScissor();
		UiTheme.muted(g, font, I18n.get(signSide == 0 ? "screen.bj_mapedit.sign_front" : "screen.bj_mapedit.sign_back"), previewX + 6, previewY + 5);
	}

	private void renderFramePreview(GuiGraphics g) {
		Minecraft mc = Minecraft.getInstance();
		ItemFrame frame = getPreviewFrame();
		ItemStack inner = resolveInnerStack();
		boolean invis = ifInvisibleBox != null && ifInvisibleBox.selected();
		if (frame == null || mc.level == null) return;

		if (invis && inner.isEmpty()) {
			g.drawCenteredString(font, I18n.get("screen.bj_mapedit.invisible_frame"), previewX + previewW / 2, previewY + previewH / 2 - 4, UiTheme.cMuted());
			return;
		}

		int cx = previewX + previewW / 2;
		int cy = previewY + previewH / 2;
		g.enableScissor(previewX + 1, previewY + 1, previewX + previewW - 1, previewY + previewH - 1);

		float scale = Math.max(52.0F, Math.min(previewW, previewH) * 0.62F);
		float yaw = previewRotY + facingViewYaw();
		float pitch = previewRotX + facingViewPitch();
		Quaternionf quat = new Quaternionf().rotateZ((float) Math.PI);
		quat.mul(new Quaternionf().rotateY((float) Math.toRadians(yaw)));
		quat.mul(new Quaternionf().rotateX((float) Math.toRadians(pitch)));
		var base = g.pose().last();
		boolean failed = false;
		try {
			g.pose().pushPose();
			g.pose().translate(cx, cy, 80.0);
			g.pose().scale(scale, scale, -scale);
			g.pose().mulPose(quat);
			Lighting.setupForEntityInInventory();
			mc.getEntityRenderDispatcher().setRenderShadow(false);
			if (!invis) {
				frame.setItem(ItemStack.EMPTY, false);
				frame.setRotation(0);
				frame.setInvisible(false);
				mc.getEntityRenderDispatcher().render(frame, 0.0, 0.0, 0.0, 0.0F, 1.0F, g.pose(), g.bufferSource(), 15728880);
				g.flush();
			}
			if (!inner.isEmpty()) {
				renderFramePreviewItem(g, mc, frame, inner);
			}
			g.pose().popPose();
		} catch (Throwable ignored) {
			failed = true;
		} finally {
			while (g.pose().last() != base && !g.pose().clear()) g.pose().popPose();
			mc.getEntityRenderDispatcher().setRenderShadow(true);
			RenderSystem.enableCull();
			Lighting.setupFor3DItems();
			g.disableScissor();
		}
		if (failed) {
			g.drawCenteredString(font, I18n.get("screen.bj_mapedit.preview_unavailable"), previewX + previewW / 2, previewY + previewH / 2 - 4, UiTheme.cMuted());
		}
	}

	private void renderFramePreviewItem(GuiGraphics g, Minecraft mc, ItemFrame frame, ItemStack inner) {
		g.pose().pushPose();
		Direction dir = Direction.SOUTH;
		double d = 0.46875;
		g.pose().translate(dir.getStepX() * d, dir.getStepY() * d, dir.getStepZ() * d);
		g.pose().mulPose(Axis.XP.rotationDegrees(frame.getXRot()));
		g.pose().mulPose(Axis.YP.rotationDegrees(180.0F - frame.getYRot()));
		g.pose().translate(0.0F, 0.0F, 0.5F);
		g.pose().mulPose(Axis.ZP.rotationDegrees(frameRotation * 45.0F));
		g.pose().scale(0.5F, 0.5F, 0.5F);
		var model = mc.getItemRenderer().getModel(inner, mc.level, mc.player, 0);
		boolean solid = model.isGui3d();
		if (solid) {
			Lighting.setupForEntityInInventory();
		} else {
			Lighting.setupForFlatItems();
		}
		RenderSystem.disableCull();
		mc.getItemRenderer().renderStatic(
			inner,
			solid ? ItemDisplayContext.FIXED : ItemDisplayContext.GUI,
			15728880,
			OverlayTexture.NO_OVERLAY,
			g.pose(),
			g.bufferSource(),
			mc.level,
			0
		);
		g.flush();
		RenderSystem.enableCull();
		Lighting.setupForEntityInInventory();
		g.pose().popPose();
	}

	private float facingViewYaw() {
		return switch (frameFacing) {
			case 2 -> 180.0F;
			case 4 -> 90.0F;
			case 5 -> -90.0F;
			default -> 0.0F;
		};
	}

	private float facingViewPitch() {
		return switch (frameFacing) {
			case 0 -> 80.0F;
			case 1 -> -80.0F;
			default -> 0.0F;
		};
	}

	private ItemFrame getPreviewFrame() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) return null;
		boolean glow = ifGlowBox != null && ifGlowBox.selected();
		boolean recreate = previewFrame == null || previewFrameGlow != glow;
		if (recreate) {
			previewFrame = glow
				? new GlowItemFrame(mc.level, BlockPos.ZERO, Direction.SOUTH)
				: new ItemFrame(mc.level, BlockPos.ZERO, Direction.SOUTH);
			previewFrameGlow = glow;
		}
		previewFrame.setItem(ItemStack.EMPTY, false);
		previewFrame.setRotation(0);
		previewFrame.setInvisible(false);
		return previewFrame;
	}

	private void renderPreviewEntity(GuiGraphics g) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) return;
		ArmorStand as = getPreviewEntity();
		updatePreviewEntity(as);
		int cx = previewX + previewW / 2;
		int cy = previewY + previewH / 2 + 8;
		float scale = Math.max(36.0F, Math.min(previewW, previewH) * 0.38F);
		float visualHeight = (asSmallBox != null && asSmallBox.selected()) ? 0.9875F : 1.975F;

		as.yBodyRot = 180.0F + previewRotY;
		as.setYRot(180.0F + previewRotY);
		as.setXRot(-previewRotX);
		as.yHeadRot = as.getYRot();
		as.yHeadRotO = as.getYRot();

		Quaternionf cam = new Quaternionf().rotateX((float) Math.toRadians(previewRotX));
		Quaternionf pose = new Quaternionf().rotateZ((float) Math.PI);
		pose.mul(cam);
		var base = g.pose().last();
		boolean failed = false;
		g.enableScissor(previewX + 1, previewY + 1, previewX + previewW - 1, previewY + previewH - 1);
		try {
			InventoryScreen.renderEntityInInventory(
				g, cx, cy, scale,
				new Vector3f(0.0F, visualHeight / 2.0F, 0.0F),
				pose, cam, as
			);
		} catch (Throwable ignored) {
			failed = true;
		} finally {
			while (g.pose().last() != base && !g.pose().clear()) g.pose().popPose();
			mc.getEntityRenderDispatcher().setRenderShadow(true);
			g.disableScissor();
			Lighting.setupFor3DItems();
		}
		if (failed) {
			g.drawCenteredString(font, I18n.get("screen.bj_mapedit.preview_unavailable"), previewX + previewW / 2, previewY + previewH / 2 - 4, UiTheme.cMuted());
		}
	}

	private ArmorStand getPreviewEntity() {
		Minecraft mc = Minecraft.getInstance();
		if (previewEntity == null && mc.level != null) {
			previewEntity = new ArmorStand(EntityType.ARMOR_STAND, mc.level);
			previewEntity.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.CREEPER_HEAD));
		}
		return previewEntity;
	}

	private void updatePreviewEntity(ArmorStand as) {
		if (as == null) return;
		byte flags = 0;
		if (asSmallBox != null && asSmallBox.selected()) flags |= 1;
		if (asShowArmsBox != null && asShowArmsBox.selected()) flags |= 4;
		if (asNoBasePlateBox != null && asNoBasePlateBox.selected()) flags |= 8;
		as.getEntityData().set(ArmorStand.DATA_CLIENT_FLAGS, flags);
		if (asInvisibleBox != null) as.setInvisible(asInvisibleBox.selected());
		if (asNoGravityBox != null) as.setNoGravity(asNoGravityBox.selected());
		as.setHeadPose(new Rotations(getPoseVal(0, 0), getPoseVal(0, 1), getPoseVal(0, 2)));
		as.setBodyPose(new Rotations(getPoseVal(1, 0), getPoseVal(1, 1), getPoseVal(1, 2)));
		as.setLeftArmPose(new Rotations(getPoseVal(2, 0), getPoseVal(2, 1), getPoseVal(2, 2)));
		as.setRightArmPose(new Rotations(getPoseVal(3, 0), getPoseVal(3, 1), getPoseVal(3, 2)));
		as.setLeftLegPose(new Rotations(getPoseVal(4, 0), getPoseVal(4, 1), getPoseVal(4, 2)));
		as.setRightLegPose(new Rotations(getPoseVal(5, 0), getPoseVal(5, 1), getPoseVal(5, 2)));
	}

	private void updateItemSuggestions() {
		if (tab != 0 || ifItemField == null || !ifItemField.isFocused()) {
			itemSuggestions = List.of();
			itemSuggestionIdx = -1;
			return;
		}
		String input = ifItemField.getValue().trim();
		ResourceLocation exactId = SafeIds.tryParseItem(input);
		if (!input.isEmpty() && exactId != null && input.indexOf(':') >= 0 && BuiltInRegistries.ITEM.containsKey(exactId)) {
			itemSuggestions = List.of();
			itemSuggestionIdx = -1;
			return;
		}
		String lower = input.toLowerCase(Locale.ROOT);
		List<ItemSuggestion> found = new ArrayList<>();
		for (ItemNames.Entry e : ItemNames.all()) {
			if (input.isEmpty() || e.idLower().contains(lower) || e.nameLower().contains(lower)) {
				found.add(new ItemSuggestion(e.id(), e.name()));
			}
		}
		itemSuggestions = found;
		itemSuggestionIdx = itemSuggestions.isEmpty() ? -1 : 0;
		suggestionScroll = SuggestionPopup.clampScroll(suggestionScroll, itemSuggestions.size());
	}

	private boolean coversSuggestionList(AbstractWidget w) {
		if (itemSuggestions.isEmpty() || ifItemField == null || !ifItemField.isFocused() || w == ifItemField) {
			return false;
		}
		int sx = ifItemField.getX();
		int sy = suggestionListY();
		int sw = ifItemField.getWidth();
		int sh = SuggestionPopup.boxH(itemSuggestions.size());
		return w.getX() < sx + sw && w.getX() + w.getWidth() > sx
			&& w.getY() < sy + sh && w.getY() + w.getHeight() > sy;
	}

	private int suggestionListY() {
		return SuggestionPopup.listY(ifItemField, itemSuggestions.size(), topPos + CONTENT_BOTTOM);
	}

	private boolean pressSuggestionBar(double mouseX, double mouseY) {
		if (itemSuggestions.isEmpty() || ifItemField == null || !ifItemField.isFocused()) return false;
		Integer next = SuggestionPopup.pressBar(ifItemField.getX(), suggestionListY(), ifItemField.getWidth(), itemSuggestions.size(), suggestionScroll, mouseX, mouseY);
		if (next == null) return false;
		suggestionScroll = next;
		return true;
	}

	private void drawItemSuggestions(GuiGraphics g, int mouseX, int mouseY) {
		if (tab != 0 || itemSuggestions.isEmpty() || ifItemField == null || !ifItemField.isFocused()) return;
		int sx = ifItemField.getX();
		int sy = suggestionListY();
		SuggestionPopup.draw(g, font, sx, sy, ifItemField.getWidth(), itemSuggestions.size(), suggestionScroll, itemSuggestionIdx, mouseX, mouseY,
			i -> itemSuggestions.get(i).label());
	}

	private boolean tryItemSuggestionClick(double mouseX, double mouseY) {
		if (itemSuggestions.isEmpty() || ifItemField == null || !ifItemField.isFocused()) return false;
		int sx = ifItemField.getX();
		int sy = suggestionListY();
		int idx = SuggestionPopup.hitIndex(mouseX, mouseY, sx, sy, ifItemField.getWidth(), itemSuggestions.size(), suggestionScroll);
		if (idx < 0) return false;
		applyItemSuggestion(itemSuggestions.get(idx));
		return true;
	}

	private void applyItemSuggestion(ItemSuggestion pick) {
		itemSuggestions = List.of();
		itemSuggestionIdx = -1;
		ifItemField.setValue(pick.id().toString());
		itemSuggestions = List.of();
		itemSuggestionIdx = -1;
	}

	private List<TemplateOrg.Row> templateRows() {
		return FrameStandTemplateStore.rows(tab, collapsedFolders());
	}

	private boolean isRenamingTemplate() {
		return renamingTemplate >= 0 || !renamingFolder.isEmpty();
	}

	private boolean inTemplateRail(double mouseX, double mouseY) {
		return mouseX >= templateRailX && mouseX < templateRailX + templateRailW
			&& mouseY >= topPos && mouseY < topPos + HEIGHT;
	}

	private int templateListTop() {
		return topPos + UiTheme.HEADER_H + 6;
	}

	private int templateListBottom() {
		return topPos + HEIGHT - 6;
	}

	private int templateListH() {
		return Math.max(0, templateListBottom() - templateListTop());
	}

	private void updateTemplateScroll() {
		int content = templateRows().size() * TEMPLATE_ROW_H;
		templateMaxScroll = Math.max(0, content - templateListH());
		templateScroll = Mth.clamp(templateScroll, 0, templateMaxScroll);
	}

	private int templateScrollBarX() {
		return templateRailX + templateRailW - 5;
	}

	private int templateActionRight() {
		return templateRailX + templateRailW - 8 - (templateMaxScroll > 0 ? 6 : 0);
	}

	private int templateRowY(int visibleIndex) {
		return templateListTop() + visibleIndex * TEMPLATE_ROW_H - templateScroll;
	}

	private int templateIndexAt(double mouseY) {
		if (mouseY < templateListTop() || mouseY >= templateListBottom()) return -1;
		return TemplateRailUi.indexAt(mouseY, templateListTop(), templateListBottom(), templateScroll, TEMPLATE_ROW_H, templateRows().size());
	}

	private void drawTemplateRail(GuiGraphics g, int mouseX, int mouseY) {
		int hx = mouseX;
		int hy = mouseY;
		if (templateMenu.covers(mouseX, mouseY)) {
			hx = -10000;
			hy = -10000;
		}
		int x = templateRailX;
		int w = templateRailW;
		updateTemplateScroll();
		UiTheme.drawPanel(g, x, topPos, w, HEIGHT);
		boolean hasSel = TemplateOrg.hasSel(selectedTemplate(), selectedFolder());
		String railKey = switch (tab) {
			case 0 -> "screen.bj_mapedit.frame_templates";
			case 1 -> "screen.bj_mapedit.stand_templates";
			default -> "screen.bj_mapedit.sign_templates";
		};
		TemplateRailUi.drawHeader(g, font, Component.translatable(railKey).getString(), x, w, topPos, hx, hy, hasSel);
		TemplateRailUi.drawRows(
			g, font, x, w, templateListTop(), templateListBottom(), TEMPLATE_ROW_H, templateScroll,
			templateRows(), selectedTemplate(), selectedFolder(), renamingTemplate, renamingFolder, templateRenameBox,
			templateActionRight(), hx, hy,
			templateDrag.active ? templateDrag.hoverFolder : null,
			templateDrag.active ? templateDrag.storeIndex : -1
		);
		UiTheme.drawThinScrollBar(g, templateScrollBarX(), templateListTop(), templateListH(), templateScroll, templateMaxScroll);
		TemplateRailUi.drawDragOverlay(g, font, templateDrag, hx, hy, x, w, topPos);
	}

	private boolean handleTemplateMenuClick(double mouseX, double mouseY) {
		RenameDeleteMenu.Action a = templateMenu.pick(mouseX, mouseY);
		TemplateOrg.Row row = templateMenuRow;
		if (a == RenameDeleteMenu.Action.RENAME && row != null) {
			templateMenuRow = null;
			if (row.folder) startFolderRename(row.folderName);
			else startTemplateRename(row.storeIndex);
			return true;
		}
		if (a == RenameDeleteMenu.Action.DELETE && row != null) {
			templateMenuRow = null;
			if (row.folder) deleteTemplateFolder(row.folderName);
			else deleteTemplate(row.storeIndex);
			return true;
		}
		if (a == RenameDeleteMenu.Action.DISMISS) templateMenuRow = null;
		return false;
	}

	private boolean handleTemplateRailClick(double mouseX, double mouseY, int button) {
		if (!inTemplateRail(mouseX, mouseY)) {
			if (isRenamingTemplate()) commitTemplateRename();
			return false;
		}
		if (UiTheme.hitScrollBar(templateScrollBarX(), templateListTop(), templateListH(), mouseX, mouseY)) {
			return false;
		}
		boolean hasSel = TemplateOrg.hasSel(selectedTemplate(), selectedFolder());
		if (mouseY >= topPos && mouseY < topPos + UiTheme.HEADER_H) {
			if (button == 0 && UiTheme.hitNewFolder(templateRailX, templateRailW, topPos, font, mouseX, mouseY, hasSel)) {
				createTemplateFolder();
				return true;
			}
			if (isRenamingTemplate()) commitTemplateRename();
			return true;
		}
		if (isRenamingTemplate() && templateRenameBox != null && templateRenameBox.visible
			&& templateRenameBox.isMouseOver(mouseX, mouseY)) {
			return false;
		}
		List<TemplateOrg.Row> rows = templateRows();
		int idx = templateIndexAt(mouseY);
		if (idx < 0) {
			commitTemplateRename();
			setFocused(null);
			return true;
		}
		TemplateOrg.Row row = rows.get(idx);
		if (button == 1) {
			if (isRenamingTemplate()) commitTemplateRename();
			templateMenuRow = row;
			templateMenu.show(font, (int) mouseX, (int) mouseY, this.width, this.height);
			return true;
		}
		if (button != 0) return true;
		if (row.folder ? TemplateOrg.folderEq(renamingFolder, row.folderName) : renamingTemplate == row.storeIndex) {
			return true;
		}
		commitTemplateRename();
		if (row.folder) {
			clickTemplateFolder(row.folderName);
		} else {
			templateDrag.press(row.storeIndex, row.folderName, row.name, row.storeIndex == selectedTemplate(), mouseX, mouseY);
		}
		setFocused(null);
		return true;
	}

	private boolean updateTemplateDrag(double mouseX, double mouseY) {
		if (!templateDrag.move(mouseX, mouseY)) return false;
		templateDrag.hoverFolder = TemplateRailUi.dropTarget(
			mouseX, mouseY, templateRailX, templateRailW, topPos, HEIGHT,
			templateListTop(), templateListBottom(), templateScroll, TEMPLATE_ROW_H, templateRows()
		);
		return true;
	}

	private boolean finishTemplateDrag() {
		if (!templateDrag.busy()) return false;
		int index = templateDrag.storeIndex;
		boolean click = templateDrag.wasClick();
		if (templateDrag.shouldMove()) {
			FrameStandTemplateStore.moveToFolder(tab, index, templateDrag.hoverFolder);
			setSelectedFolder(templateDrag.hoverFolder);
			if (!selectedFolder().isEmpty()) collapsedFolders().remove(selectedFolder());
			updateTemplateScroll();
		} else if (click) {
			loadTemplate(index);
		}
		templateDrag.reset();
		return true;
	}


	private void createTemplateFolder() {
		commitTemplateRename();
		String name = FrameStandTemplateStore.addFolder(tab);
		collapsedFolders().remove(name);
		setSelectedFolder(name);
		setSelectedTemplate(-1);
		updateTemplateScroll();
	}

	private void clickTemplateFolder(String name) {
		String folder = TemplateOrg.norm(name);
		setSelectedTemplate(-1);
		setSelectedFolder(folder);
		Set<String> collapsed = collapsedFolders();
		if (!collapsed.add(folder)) collapsed.remove(folder);
	}

	private void deleteTemplateFolder(String name) {
		if (TemplateOrg.folderEq(renamingFolder, name)) cancelTemplateRename();
		else commitTemplateRename();
		FrameStandTemplateStore.Entry kept = selectedTemplate() >= 0 ? FrameStandTemplateStore.get(tab, selectedTemplate()) : null;
		FrameStandTemplateStore.removeFolder(tab, name);
		collapsedFolders().remove(TemplateOrg.norm(name));
		if (TemplateOrg.folderEq(selectedFolder(), name)) {
			setSelectedFolder("");
			setSelectedTemplate(-1);
		} else if (kept != null) {
			setSelectedTemplate(FrameStandTemplateStore.all(tab).indexOf(kept));
		}
		updateTemplateScroll();
	}

	private void deleteTemplate(int index) {
		if (renamingTemplate == index) cancelTemplateRename();
		else commitTemplateRename();
		FrameStandTemplateStore.remove(tab, index);
		if (selectedTemplate() == index) setSelectedTemplate(-1);
		else if (selectedTemplate() > index) setSelectedTemplate(selectedTemplate() - 1);
		updateTemplateScroll();
	}

	private void startFolderRename(String name) {
		if (templateRenameBox == null) return;
		if (isRenamingTemplate() && !TemplateOrg.folderEq(renamingFolder, name)) {
			commitTemplateRename();
		}
		renamingTemplate = -1;
		renamingFolder = TemplateOrg.norm(name);
		templateRenameBox.setValue(renamingFolder);
		templateRenameBox.visible = true;
		syncTemplateRenameBox();
		setFocused(templateRenameBox);
		templateRenameBox.setFocused(true);
		templateRenameBox.moveCursorToEnd(false);
		setHighlightPos(templateRenameBox, 0);
	}

	private void startTemplateRename(int index) {
		FrameStandTemplateStore.Entry entry = FrameStandTemplateStore.get(tab, index);
		if (entry == null || templateRenameBox == null) return;
		if (isRenamingTemplate() && renamingTemplate != index) {
			commitTemplateRename();
		}
		renamingFolder = "";
		renamingTemplate = index;
		templateRenameBox.setValue(entry.name == null ? "" : entry.name);
		templateRenameBox.visible = true;
		syncTemplateRenameBox();
		setFocused(templateRenameBox);
		templateRenameBox.setFocused(true);
		templateRenameBox.moveCursorToEnd(false);
		setHighlightPos(templateRenameBox, 0);
	}

	private void syncTemplateRenameBox() {
		List<TemplateOrg.Row> rows = templateRows();
		int rowIdx = TemplateRailUi.renamingRow(rows, renamingTemplate, renamingFolder);
		if (templateRenameBox == null || rowIdx < 0) {
			if (templateRenameBox != null) {
				templateRenameBox.visible = false;
				templateRenameBox.setY(-1000);
			}
			return;
		}
		int x = TemplateOrg.rowNameX(templateRailX, rows.get(rowIdx).depth);
		int y = templateRowY(rowIdx) + 3;
		int w = Math.max(24, templateActionRight() - x - 4);
		templateRenameBox.setX(x);
		templateRenameBox.setY(y);
		templateRenameBox.setWidth(w);
		templateRenameBox.visible = true;
	}

	private void commitTemplateRename() {
		if (templateRenameBox == null) return;
		if (!renamingFolder.isEmpty()) {
			String from = renamingFolder;
			String to = templateRenameBox.getValue();
			if (TemplateRailUi.rejectTakenFolder(FrameStandTemplateStore.folders(tab), from, to)) {
				cancelTemplateRename();
				return;
			}
			FrameStandTemplateStore.renameFolder(tab, from, to);
			String normTo = TemplateOrg.norm(to);
			if (!normTo.isEmpty() && !TemplateOrg.folderEq(from, normTo)) {
				TemplateOrg.renameCollapsed(collapsedFolders(), from, normTo);
				if (TemplateOrg.folderEq(selectedFolder(), from)) setSelectedFolder(normTo);
			}
			cancelTemplateRename();
			return;
		}
		if (renamingTemplate < 0) return;
		FrameStandTemplateStore.rename(tab, renamingTemplate, templateRenameBox.getValue());
		cancelTemplateRename();
	}

	private void cancelTemplateRename() {
		renamingTemplate = -1;
		renamingFolder = "";
		if (templateRenameBox != null) {
			templateRenameBox.visible = false;
			templateRenameBox.setY(-1000);
			if (templateRenameBox.isFocused()) setFocused(null);
		}
	}

	private static void setHighlightPos(EditBox box, int pos) {
		box.setHighlightPos(pos);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (!itemSuggestions.isEmpty() && ifItemField != null && ifItemField.isFocused()
			&& SuggestionPopup.maxScroll(itemSuggestions.size()) > 0) {
			suggestionScroll = SuggestionPopup.scrollBy(suggestionScroll, itemSuggestions.size(), scrollY);
			return true;
		}
		if (inTemplateRail(mouseX, mouseY)) {
			templateScroll = Mth.clamp(templateScroll - (int) (scrollY * 20), 0, templateMaxScroll);
			syncTemplateRenameBox();
			return true;
		}
		if (mouseX >= leftPos && mouseX < leftPos + RIGHT_X - 4
			&& mouseY >= topPos + CONTENT_TOP && mouseY < topPos + CONTENT_BOTTOM) {
			scrollOffset = Mth.clamp(scrollOffset - (int) (scrollY * 20), 0, maxScroll);
			repositionAll();
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		heldPoseIdx = -1;
		heldPoseTimer = 0;
		if (button == 0 && UiTheme.consumeRefreshClick(refreshX, refreshY, mouseX, mouseY, this::resetFields)) {
			return true;
		}
		if (handleTemplateMenuClick(mouseX, mouseY)) return true;
		if (handleTemplateRailClick(mouseX, mouseY, button)) return true;
		if (button == 0 && pressSuggestionBar(mouseX, mouseY)) return true;
		if (button == 0 && tryItemSuggestionClick(mouseX, mouseY)) return true;
		if (button == 0) {
			UiTheme.ScrollClick tsc = UiTheme.clickBar(templateScrollBarX(), templateListTop(), templateListH(), templateScroll, templateMaxScroll, mouseX, mouseY);
			if (tsc != null) {
				draggingTemplateScroll = true;
				templateScrollGrab = tsc.grab;
				if (tsc.scroll != templateScroll) {
					templateScroll = tsc.scroll;
					syncTemplateRenameBox();
				}
				return true;
			}
			UiTheme.ScrollClick sc = UiTheme.clickBar(leftPos + RIGHT_X - 6, topPos + CONTENT_TOP, CONTENT_BOTTOM - CONTENT_TOP, scrollOffset, maxScroll, mouseX, mouseY);
			if (sc != null) {
				draggingScroll = true;
				scrollGrabOffset = sc.grab;
				if (sc.scroll != scrollOffset) {
					scrollOffset = sc.scroll;
					repositionAll();
				}
				return true;
			}
		}
		if (button == 0 && tab != 2
			&& mouseX >= previewX && mouseX < previewX + previewW
			&& mouseY >= previewY && mouseY < previewY + previewH) {
			draggingPreview = true;
			dragStartX = mouseX;
			dragStartY = mouseY;
			dragStartRotY = previewRotY;
			dragStartRotX = previewRotX;
			return true;
		}
		if (handleActionColumnClick(mouseX, mouseY, button)) return true;
		boolean result = super.mouseClicked(mouseX, mouseY, button);
		if (button == 0) {
			if (SuggestionPopup.clickOpens(ifItemField, mouseX, mouseY)) updateItemSuggestions();
			else {
				itemSuggestions = List.of();
				itemSuggestionIdx = -1;
			}
		}
		if (button == 0 && tab == 1) {
			for (int i = 0; i < 6; i++) {
				for (int j = 0; j < 3; j++) {
					int idx = i * 3 + j;
					if (poseMinusBtns[i][j] != null && isMouseOverBtn(poseMinusBtns[i][j], mouseX, mouseY)) {
						heldPoseIdx = idx;
						heldPoseDelta = -1.0f;
						heldPoseTimer = 0;
						return result;
					}
					if (posePlusBtns[i][j] != null && isMouseOverBtn(posePlusBtns[i][j], mouseX, mouseY)) {
						heldPoseIdx = idx;
						heldPoseDelta = 1.0f;
						heldPoseTimer = 0;
						return result;
					}
				}
			}
		}
		if (button == 0 && !result) setFocused(null);
		return result;
	}

	private static boolean isMouseOverBtn(Button btn, double mouseX, double mouseY) {
		return mouseX >= btn.getX() && mouseX < btn.getX() + btn.getWidth()
			&& mouseY >= btn.getY() && mouseY < btn.getY() + btn.getHeight();
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
		if (SuggestionPopup.isDragging() && button == 0) {
			suggestionScroll = SuggestionPopup.dragTo(mouseY);
			return true;
		}
		if (draggingTemplateScroll && button == 0) {
			int next = UiTheme.scrollAtGrab(templateListTop(), templateListH(), templateMaxScroll, mouseY, templateScrollGrab);
			if (next != templateScroll) {
				templateScroll = next;
				syncTemplateRenameBox();
			}
			return true;
		}
		if (draggingScroll && button == 0) {
			int next = UiTheme.scrollAtGrab(topPos + CONTENT_TOP, CONTENT_BOTTOM - CONTENT_TOP, maxScroll, mouseY, scrollGrabOffset);
			if (next != scrollOffset) {
				scrollOffset = next;
				repositionAll();
			}
			return true;
		}
		if (draggingPreview) {
			previewRotY = dragStartRotY - (float) (mouseX - dragStartX) * 0.3f;
			previewRotX = dragStartRotX + (float) (mouseY - dragStartY) * 0.3f;
			return true;
		}
		if (button == 0 && updateTemplateDrag(mouseX, mouseY)) return true;
		return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		heldPoseIdx = -1;
		heldPoseTimer = 0;
		if (button == 0) {
			boolean moved = finishTemplateDrag();
			draggingScroll = false;
			draggingTemplateScroll = false;
			SuggestionPopup.endDrag();
			if (moved) {
				if (draggingPreview) draggingPreview = false;
				return true;
			}
		}
		if (draggingPreview) {
			draggingPreview = false;
			return true;
		}
		return super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (templateMenu.isOpen() && keyCode == 256) {
			templateMenu.close();
			templateMenuRow = null;
			return true;
		}
		if (isRenamingTemplate() && templateRenameBox != null && templateRenameBox.visible) {
			if (keyCode == 257 || keyCode == 335) {
				commitTemplateRename();
				return true;
			}
			if (keyCode == 256) {
				cancelTemplateRename();
				return true;
			}
		}
		if (!itemSuggestions.isEmpty() && ifItemField != null && ifItemField.isFocused()) {
			if (keyCode == 258 || keyCode == 257) {
				applyItemSuggestion(itemSuggestions.get(Math.max(0, itemSuggestionIdx)));
				return true;
			}
			if (keyCode == 264) {
				itemSuggestionIdx = Math.min(itemSuggestionIdx + 1, itemSuggestions.size() - 1);
				suggestionScroll = SuggestionPopup.keepVisible(itemSuggestionIdx, suggestionScroll, itemSuggestions.size());
				return true;
			}
			if (keyCode == 265) {
				itemSuggestionIdx = Math.max(0, itemSuggestionIdx - 1);
				suggestionScroll = SuggestionPopup.keepVisible(itemSuggestionIdx, suggestionScroll, itemSuggestions.size());
				return true;
			}
			if (keyCode == 256) {
				itemSuggestions = List.of();
				itemSuggestionIdx = -1;
				return true;
			}
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void tick() {
		super.tick();
		if (heldPoseIdx >= 0 && heldPoseIdx < 18) {
			heldPoseTimer++;
			if (heldPoseTimer > 12 && heldPoseTimer % 2 == 0) {
				adjustPose(heldPoseIdx, heldPoseDelta);
			}
		}
	}
}
