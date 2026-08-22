package cn.erindax.bjmapedit.client.screen;

import cn.erindax.bjmapedit.client.widget.FrameStandTemplateStore;
import cn.erindax.bjmapedit.client.widget.GiveCommands;
import cn.erindax.bjmapedit.client.widget.SafeIds;
import cn.erindax.bjmapedit.client.widget.SuggestionPopup;
import cn.erindax.bjmapedit.client.widget.TemplateDrag;
import cn.erindax.bjmapedit.client.widget.TemplateOrg;
import cn.erindax.bjmapedit.client.widget.RenameDeleteMenu;
import cn.erindax.bjmapedit.client.widget.TemplateRailUi;
import cn.erindax.bjmapedit.client.widget.UiTheme;
import cn.erindax.bjmapedit.networking.payload.ExecuteCommandsPayload;
import cn.erindax.bjmapedit.networking.payload.GiveItemPayload;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
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
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.GlowItemFrame;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
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

@Environment(EnvType.CLIENT)
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
	private static final Set<String> collapsedFrameFolders = new HashSet<>();
	private static final Set<String> collapsedStandFolders = new HashSet<>();
	private boolean opened;
	private EditBox templateRenameBox;
	private int renamingTemplate = -1;
	private String renamingFolder = "";
	private final TemplateDrag templateDrag = new TemplateDrag();
	private final RenameDeleteMenu templateMenu = new RenameDeleteMenu();
	private TemplateOrg.Row templateMenuRow;

	private Button tabFrameBtn;
	private Button tabStandBtn;
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

	private Checkbox asShowArmsBox;
	private Checkbox asNoBasePlateBox;
	private Checkbox asSmallBox;
	private Checkbox asInvisibleBox;
	private Checkbox asMarkerBox;
	private Checkbox asNoGravityBox;

	private EditBox[] poseFields = new EditBox[18];
	private Button[][] poseMinusBtns = new Button[6][3];
	private Button[][] posePlusBtns = new Button[6][3];

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
	private static final String[] POSE_DISPLAY_CN = {"头部", "身体", "左臂", "右臂", "左腿", "右腿"};
	private static final String[] POSE_AXES = {"X", "Y", "Z"};
	private static final String[] FACING_CN = {"下", "上", "北", "南", "西", "东"};

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
			Component.literal("展示框"), b -> switchTab(0)
		).bounds(0, 0, 80, 18).build());
		tabStandBtn = this.addRenderableWidget(Button.builder(
			Component.literal("盔甲架"), b -> switchTab(1)
		).bounds(0, 0, 80, 18).build());

		buildItemFrameTab();
		buildArmorStandTab();

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
		setShown(ifGlowBox, frame);
		setShown(ifInvisibleBox, frame);
		setShown(ifFixedBox, frame);
		setShown(ifFacingMinus, frame);
		setShown(ifFacingPlus, frame);
		setShown(ifRotMinus, frame);
		setShown(ifRotPlus, frame);
		setShown(ifItemField, frame);
		setShown(asShowArmsBox, !frame);
		setShown(asNoBasePlateBox, !frame);
		setShown(asSmallBox, !frame);
		setShown(asInvisibleBox, !frame);
		setShown(asMarkerBox, !frame);
		setShown(asNoGravityBox, !frame);
		for (int i = 0; i < 18; i++) setShown(poseFields[i], !frame);
		for (int i = 0; i < 6; i++) {
			for (int j = 0; j < 3; j++) {
				setShown(poseMinusBtns[i][j], !frame);
				setShown(posePlusBtns[i][j], !frame);
			}
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
			labelW = Math.max(labelW, this.font.width("内部物品") + 10);
			labelW = Math.max(labelW, this.font.width("显示手臂") + 10);
			labelW = Math.max(labelW, this.font.width("无碰撞") + 10);
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
			poseFields[idx].setValue(String.format("%.1f", val));
		} catch (NumberFormatException e) {
			poseFields[idx].setValue(String.format("%.1f", delta));
		}
	}

	private void repositionAll() {
		layoutPanel();
		if (tabFrameBtn == null || summonBtn == null) return;
		int tabY = topPos + UiTheme.HEADER_H + 6;
		tabFrameBtn.setPosition(leftPos + 10, tabY);
		tabFrameBtn.setWidth(72);
		tabFrameBtn.setHeight(18);
		tabStandBtn.setPosition(leftPos + 86, tabY);
		tabStandBtn.setWidth(72);
		tabStandBtn.setHeight(18);
		tabFrameBtn.active = tab != 0;
		tabStandBtn.active = tab != 1;

		int y = topPos + CONTENT_TOP + 2 - scrollOffset;
		int total = 0;
		if (tab == 0) {
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
	}

	private void placeField(EditBox box, int y) {
		if (box == null) return;
		box.setX(leftPos + INPUT_X);
		box.setY(y);
		box.setWidth(INPUT_W);
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

	private boolean isChrome(AbstractWidget w) {
		return w == tabFrameBtn || w == tabStandBtn || w == summonBtn
			|| w == giveBtn || w == copyGiveBtn || w == saveBtn || w == templateRenameBox;
	}

	private ItemStack buildGiveStack() {
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
		if (raw.isEmpty()) return ItemStack.EMPTY;
		ResourceLocation id = SafeIds.tryParseItem(raw);
		if (id != null && BuiltInRegistries.ITEM.containsKey(id)) {
			return new ItemStack(BuiltInRegistries.ITEM.get(id));
		}
		String lower = raw.toLowerCase(Locale.ROOT);
		ResourceLocation unique = null;
		int hits = 0;
		for (ResourceLocation key : BuiltInRegistries.ITEM.keySet()) {
			Item it = BuiltInRegistries.ITEM.get(key);
			if (it == Items.AIR) continue;
			String label = it.getName(new ItemStack(it)).getString();
			if (label.equals(raw) || label.toLowerCase(Locale.ROOT).equals(lower)) {
				return new ItemStack(it);
			}
			if (label.toLowerCase(Locale.ROOT).contains(lower) || key.toString().toLowerCase(Locale.ROOT).contains(lower)
				|| key.getPath().toLowerCase(Locale.ROOT).contains(lower)) {
				hits++;
				unique = key;
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
			if (x == 0f && y == 0f && z == 0f) continue;
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
		ItemStack stack = buildGiveStack();
		if (!stack.isEmpty()) {
			ClientPlayNetworking.send(new GiveItemPayload(stack));
		}
	}

	private void copyGive() {
		GiveCommands.copy(GiveCommands.give(buildGiveStack()));
	}

	private void summonEntity() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null) return;
		LookTarget at = lookTarget(mc);
		CompoundTag nbt = buildSummonTag();
		if (tab == 0) {
			nbt.putByte("Facing", (byte) at.facing);
			if (at.tile != null) {
				nbt.putInt("TileX", at.tile.getX());
				nbt.putInt("TileY", at.tile.getY());
				nbt.putInt("TileZ", at.tile.getZ());
			}
		}
		String body = nbt.isEmpty() ? "{}" : GiveCommands.snbt(nbt);
		String cmd = "summon " + summonType() + " " + fmt(at.x) + " " + fmt(at.y) + " " + fmt(at.z) + " " + body;
		ClientPlayNetworking.send(new ExecuteCommandsPayload(List.of(cmd)));
	}

	private record LookTarget(double x, double y, double z, int facing, BlockPos tile) {}

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
				return new LookTarget(center.x - ox, center.y - oy, center.z - oz, facing, attach);
			}
			Vec3 loc = block.getLocation();
			return new LookTarget(loc.x, loc.y, loc.z, facing, support);
		}
		Vec3 dest = player.getEyePosition(1.0F).add(player.getViewVector(1.0F).scale(3.0));
		return new LookTarget(dest.x, dest.y, dest.z, frameFacing, null);
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
		return tab == 0 ? selectedFrameTemplate : selectedStandTemplate;
	}

	private void setSelectedTemplate(int index) {
		if (tab == 0) selectedFrameTemplate = index;
		else selectedStandTemplate = index;
	}

	private String selectedFolder() {
		return tab == 0 ? selectedFrameFolder : selectedStandFolder;
	}

	private void setSelectedFolder(String folder) {
		if (tab == 0) selectedFrameFolder = TemplateOrg.norm(folder);
		else selectedStandFolder = TemplateOrg.norm(folder);
	}

	private Set<String> collapsedFolders() {
		return tab == 0 ? collapsedFrameFolders : collapsedStandFolders;
	}

	private void saveTemplate() {
		commitTemplateRename();
		FrameStandTemplateStore.Entry entry = snapshotTemplate(defaultTemplateName());
		entry.folder = selectedFolder();
		FrameStandTemplateStore.add(tab, entry);
		setSelectedTemplate(FrameStandTemplateStore.all(tab).size() - 1);
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
		if (tab == 0) {
			e.glow = ifGlowBox != null && ifGlowBox.selected();
			e.frameInvis = ifInvisibleBox != null && ifInvisibleBox.selected();
			e.fixed = ifFixedBox != null && ifFixedBox.selected();
			e.facing = frameFacing;
			e.rotation = frameRotation;
			e.item = ifItemField != null ? ifItemField.getValue() : "";
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
		if (tab == 0) {
			ItemStack inner = resolveInnerStack();
			base = inner.isEmpty() ? "展示框" : inner.getHoverName().getString();
		} else {
			base = "盔甲架";
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
		return s;
	}

	private void applySession(Session s) {
		if (s == null) return;
		tab = s.tab == 1 ? 1 : 0;
		selectedFrameTemplate = s.selectedFrame;
		selectedStandTemplate = s.selectedStand;
		selectedFrameFolder = TemplateOrg.norm(s.selectedFrameFolder);
		selectedStandFolder = TemplateOrg.norm(s.selectedStandFolder);
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
		if (tab == 0) {
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
		else renderPreviewEntity(g);

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
		if (tab == 0) {
			drawFieldLabel(g, ifGlowBox, "发光");
			drawFieldLabel(g, ifInvisibleBox, "隐形");
			drawFieldLabel(g, ifFixedBox, "固定");
			drawFieldLabel(g, ifFacingMinus, "朝向");
			drawStepperValue(g, ifFacingMinus, ifFacingPlus, FACING_CN[frameFacing]);
			drawFieldLabel(g, ifRotMinus, "旋转");
			drawStepperValue(g, ifRotMinus, ifRotPlus, (frameRotation * 45) + "°");
			drawFieldLabel(g, ifItemField, "内部物品");
			return;
		}
		drawFieldLabel(g, asShowArmsBox, "显示手臂");
		drawFieldLabel(g, asNoBasePlateBox, "无底座");
		drawFieldLabel(g, asSmallBox, "小型");
		drawFieldLabel(g, asInvisibleBox, "隐形");
		drawFieldLabel(g, asMarkerBox, "无碰撞");
		drawFieldLabel(g, asNoGravityBox, "无重力");
		if (asNoGravityBox != null) {
			int poseHeadY = asNoGravityBox.getY() + ROW_H + 8;
			if (poseHeadY + 10 > topPos + CONTENT_TOP && poseHeadY < topPos + CONTENT_BOTTOM) {
				UiTheme.muted(g, font, "姿态", leftPos + LABEL_X, poseHeadY + 2);
				int axisW = Math.max(48, (INPUT_W - 8) / 3);
				for (int j = 0; j < 3; j++) {
					g.drawCenteredString(font, POSE_AXES[j], leftPos + INPUT_X + j * axisW + axisW / 2, poseHeadY + 2, UiTheme.cMuted());
				}
			}
		}
		for (int i = 0; i < 6; i++) {
			if (poseFields[i * 3] == null) continue;
			drawFieldLabel(g, poseFields[i * 3], POSE_DISPLAY_CN[i]);
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

	private void renderFramePreview(GuiGraphics g) {
		Minecraft mc = Minecraft.getInstance();
		ItemFrame frame = getPreviewFrame();
		ItemStack inner = resolveInnerStack();
		boolean invis = ifInvisibleBox != null && ifInvisibleBox.selected();
		if (frame == null || mc.level == null) return;

		if (invis && inner.isEmpty()) {
			g.drawCenteredString(font, "隐形展示框", previewX + previewW / 2, previewY + previewH / 2 - 4, UiTheme.cMuted());
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
		mc.getEntityRenderDispatcher().setRenderShadow(true);
		g.pose().popPose();
		Lighting.setupFor3DItems();
		g.disableScissor();
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
		g.enableScissor(previewX + 1, previewY + 1, previewX + previewW - 1, previewY + previewH - 1);
		InventoryScreen.renderEntityInInventory(
			g, cx, cy, scale,
			new Vector3f(0.0F, visualHeight / 2.0F, 0.0F),
			pose, cam, as
		);
		g.disableScissor();
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
		itemSuggestions = BuiltInRegistries.ITEM.keySet().stream()
			.map(id -> {
				Item it = BuiltInRegistries.ITEM.get(id);
				String label = it.getName(new ItemStack(it)).getString();
				return new ItemSuggestion(id, label);
			})
			.filter(s -> input.isEmpty()
				|| s.id().toString().toLowerCase(Locale.ROOT).contains(lower)
				|| s.label().toLowerCase(Locale.ROOT).contains(lower))
			.sorted(java.util.Comparator.comparing(s -> s.id().toString()))
			.toList();
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
		TemplateRailUi.drawHeader(g, font, Component.translatable(tab == 0
			? "screen.bj_mapedit.frame_templates" : "screen.bj_mapedit.stand_templates").getString(), x, w, topPos, hx, hy, hasSel);
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

	private void clearTemplateSelection() {
		setSelectedTemplate(-1);
		setSelectedFolder("");
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
		try {
			java.lang.reflect.Field f = EditBox.class.getDeclaredField("highlightPos");
			f.setAccessible(true);
			f.set(box, pos);
		} catch (Exception ignored) {}
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
		if (button == 0
			&& mouseX >= previewX && mouseX < previewX + previewW
			&& mouseY >= previewY && mouseY < previewY + previewH) {
			draggingPreview = true;
			dragStartX = mouseX;
			dragStartY = mouseY;
			dragStartRotY = previewRotY;
			dragStartRotX = previewRotX;
			return true;
		}
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
