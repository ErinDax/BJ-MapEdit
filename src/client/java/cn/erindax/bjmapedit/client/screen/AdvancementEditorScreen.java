package cn.erindax.bjmapedit.client.screen;

import cn.erindax.bjmapedit.client.DatapackNet;
import cn.erindax.bjmapedit.client.EditorAccess;
import cn.erindax.bjmapedit.client.widget.InvPicker;
import cn.erindax.bjmapedit.client.widget.RenameDeleteMenu;
import cn.erindax.bjmapedit.client.widget.SafeIds;
import cn.erindax.bjmapedit.client.widget.SuggestionPopup;
import cn.erindax.bjmapedit.client.widget.UiTheme;
import cn.erindax.bjmapedit.networking.DatapackOps;
import cn.erindax.bjmapedit.networking.payload.DatapackOpResultPayload;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.Util;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.storage.LevelResource;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Environment(EnvType.CLIENT)
public class AdvancementEditorScreen extends Screen {

	private static final int FOOTER_H = 30;
	private static final int ROW_H = 22;
	private static final int PALETTE_CELL = 20;
	private static final int SELECTED_FILL = 0xFF21262D;
	private static final int HOVER_FILL = 0xFF30363D;
	private static final int DRAG_START = 4;
	private static final int SRC_NONE = -3;
	private static final int SRC_PALETTE = -1;
	private static final int SRC_ICON = -2;
	private static final int SRC_COND = -4;

	private static final String[] TRIGGER_IDS = {
		"minecraft:inventory_changed",
		"minecraft:placed_block",
		"minecraft:consume_item",
		"minecraft:enter_block",
		"minecraft:impossible",
		"minecraft:tick",
		"minecraft:slept_in_bed",
		"minecraft:used_ender_eye",
		"minecraft:nether_travel",
		"minecraft:construct_beacon",
		"minecraft:villager_trade",
		"minecraft:changed_dimension",
		"minecraft:player_killed_entity",
		"minecraft:bred_animals",
		"minecraft:tame_animal",
		"minecraft:recipe_unlocked",
		"minecraft:location"
	};
	private static final String[] TRIGGER_KEYS = {
		"screen.bj_mapedit.adv_trig.inventory_changed",
		"screen.bj_mapedit.adv_trig.placed_block",
		"screen.bj_mapedit.adv_trig.consume_item",
		"screen.bj_mapedit.adv_trig.enter_block",
		"screen.bj_mapedit.adv_trig.impossible",
		"screen.bj_mapedit.adv_trig.tick",
		"screen.bj_mapedit.adv_trig.slept_in_bed",
		"screen.bj_mapedit.adv_trig.used_ender_eye",
		"screen.bj_mapedit.adv_trig.nether_travel",
		"screen.bj_mapedit.adv_trig.construct_beacon",
		"screen.bj_mapedit.adv_trig.villager_trade",
		"screen.bj_mapedit.adv_trig.changed_dimension",
		"screen.bj_mapedit.adv_trig.player_killed_entity",
		"screen.bj_mapedit.adv_trig.bred_animals",
		"screen.bj_mapedit.adv_trig.tame_animal",
		"screen.bj_mapedit.adv_trig.recipe_unlocked",
		"screen.bj_mapedit.adv_trig.location"
	};
	private static final String[] BACKGROUNDS = {
		"minecraft:textures/gui/advancements/backgrounds/stone.png",
		"minecraft:textures/gui/advancements/backgrounds/nether.png",
		"minecraft:textures/gui/advancements/backgrounds/end.png",
		"minecraft:textures/gui/advancements/backgrounds/adventure.png",
		"minecraft:textures/gui/advancements/backgrounds/husbandry.png"
	};
	private static final String[] BACKGROUND_KEYS = {
		"screen.bj_mapedit.adv_bg.stone",
		"screen.bj_mapedit.adv_bg.nether",
		"screen.bj_mapedit.adv_bg.end",
		"screen.bj_mapedit.adv_bg.adventure",
		"screen.bj_mapedit.adv_bg.husbandry"
	};
	private static final String[] DIM_IDS = {
		"minecraft:overworld",
		"minecraft:the_nether",
		"minecraft:the_end"
	};
	private static final String[] DIM_KEYS = {
		"screen.bj_mapedit.adv_dim_overworld",
		"screen.bj_mapedit.adv_dim_nether",
		"screen.bj_mapedit.adv_dim_end"
	};

	private static final List<ResourceLocation> COMMON_ITEMS = List.of(
		BuiltInRegistries.ITEM.getKey(Items.STONE),
		BuiltInRegistries.ITEM.getKey(Items.OAK_PLANKS),
		BuiltInRegistries.ITEM.getKey(Items.STICK),
		BuiltInRegistries.ITEM.getKey(Items.IRON_INGOT),
		BuiltInRegistries.ITEM.getKey(Items.GOLD_INGOT),
		BuiltInRegistries.ITEM.getKey(Items.DIAMOND),
		BuiltInRegistries.ITEM.getKey(Items.REDSTONE),
		BuiltInRegistries.ITEM.getKey(Items.STRING),
		BuiltInRegistries.ITEM.getKey(Items.COBBLESTONE),
		BuiltInRegistries.ITEM.getKey(Items.GLASS),
		BuiltInRegistries.ITEM.getKey(Items.IRON_NUGGET),
		BuiltInRegistries.ITEM.getKey(Items.GOLD_NUGGET),
		BuiltInRegistries.ITEM.getKey(Items.LEATHER),
		BuiltInRegistries.ITEM.getKey(Items.FLINT),
		BuiltInRegistries.ITEM.getKey(Items.FEATHER),
		BuiltInRegistries.ITEM.getKey(Items.PAPER),
		BuiltInRegistries.ITEM.getKey(Items.COAL),
		BuiltInRegistries.ITEM.getKey(Items.COMPASS),
		BuiltInRegistries.ITEM.getKey(Items.CLOCK)
	);

	private int WIDTH = 500;
	private int HEIGHT = 400;
	private int CONTENT_TOP = 30;
	private int leftPos;
	private int topPos;
	private int railX = 8;
	private int railW = 148;
	private int rightRailX = 8;
	private int rightRailW = 148;
	private int refreshX, refreshY;
	private int iconSlotX, iconSlotY;
	private int condSlotX, condSlotY;
	private boolean condSlotVisible;
	private int titleLabelY, descLabelY, parentLabelY, backgroundLabelY, triggerLabelY, condLabelY, xpLabelY, idLabelY;

	private EditBox searchField;
	private EditBox listSearchField;
	private EditBox titleField;
	private EditBox descField;
	private EditBox parentField;
	private EditBox backgroundField;
	private EditBox triggerField;
	private EditBox condField;
	private EditBox xpField;
	private EditBox nameField;
	private Button titleEditBtn;
	private Button descEditBtn;
	private Button taskBtn;
	private Button goalBtn;
	private Button challengeBtn;
	private Button overworldBtn;
	private Button netherBtn;
	private Button endBtn;
	private Button condPickBtn;
	private final InvPicker invPicker = new InvPicker();
	private Checkbox toastBox;
	private Checkbox chatBox;
	private Checkbox hiddenBox;
	private Button copyBtn;
	private Button saveBtn;
	private long saveHintUntil;
	private Button nameOkBtn;
	private Button nameCancelBtn;
	private boolean namingOpen;
	private boolean namingError;
	private boolean namingIsRename;
	private String namingDraft = "";

	private String titleText = "";
	private String descText = "";
	private ResourceLocation iconId = BuiltInRegistries.ITEM.getKey(Items.DIAMOND);
	private int frameIdx;
	private boolean showToast = true;
	private boolean announceChat = true;
	private boolean hidden;
	private String parentText = "";
	private String backgroundText = BACKGROUNDS[0];
	private String triggerText = TRIGGER_IDS[0];
	private String condText = "";
	private String xpText = "";

	private List<ResourceLocation> searchResults = new ArrayList<>();
	private boolean searching;
	private int searchScroll;
	private int maxSearchScroll;
	private boolean draggingPaletteScroll;
	private double paletteScrollGrab;

	private List<ListedAdv> rows = List.of();
	private int listScroll;
	private int listMaxScroll;
	private boolean draggingListScroll;
	private double listScrollGrab;
	private ResourceLocation editingId;
	private final RenameDeleteMenu rowMenu = new RenameDeleteMenu();
	private ListedAdv menuRow;
	private final Set<ResourceLocation> remoteOwned = new LinkedHashSet<>();
	private final LinkedHashMap<ResourceLocation, String> remoteJson = new LinkedHashMap<>();
	private ResourceLocation pendingLoadId;
	private boolean pendingSave;

	private ResourceLocation dragItem;
	private int dragSource = SRC_NONE;
	private boolean dragPending;
	private boolean dragActive;
	private double dragStartX, dragStartY;

	private enum SuggestKind { NONE, PARENT, BACKGROUND, TRIGGER, COND }
	private SuggestKind suggestKind = SuggestKind.NONE;
	private List<Suggest> suggestions = List.of();
	private int suggestionIdx = -1;
	private int suggestionScroll;

	private boolean opened;
	private static Session session;

	private static final class Session {
		String title = "";
		String desc = "";
		ResourceLocation icon;
		int frameIdx;
		boolean showToast = true;
		boolean announceChat = true;
		boolean hidden;
		String parent = "";
		String background = BACKGROUNDS[0];
		String trigger = TRIGGER_IDS[0];
		String cond = "";
		String xp = "";
		String search = "";
		int searchScroll;
		String listSearch = "";
		int listScroll;
		ResourceLocation editingId;
	}

	private static final class ListedAdv {
		final ResourceLocation id;
		final ItemStack icon;
		final String label;
		boolean owned;

		ListedAdv(ResourceLocation id, ItemStack icon, String label, boolean owned) {
			this.id = id;
			this.icon = icon == null ? ItemStack.EMPTY : icon;
			this.label = label == null || label.isBlank() ? id.getPath() : label;
			this.owned = owned;
		}
	}

	private record Suggest(String id, String label) {}

	public AdvancementEditorScreen() {
		super(Component.translatable("screen.bj_mapedit.adv_title"));
	}

	@Override
	protected void init() {
		boolean first = !opened;
		boolean keepNaming = namingOpen;
		String keepDraft = nameField != null ? nameField.getValue() : namingDraft;
		boolean keepErr = namingError;
		opened = true;
		super.init();
		this.clearWidgets();
		layoutPanel();

		searchField = new EditBox(font, 0, 0, 80, 16, Component.empty());
		searchField.setResponder(s -> updateSearchResults());
		this.addRenderableWidget(searchField);

		listSearchField = new EditBox(font, 0, 0, 80, 16, Component.empty());
		listSearchField.setResponder(s -> refreshList());
		this.addRenderableWidget(listSearchField);

		titleField = new EditBox(font, 0, 0, 80, 16, Component.empty());
		titleField.setMaxLength(256);
		titleField.setResponder(s -> titleText = s);
		this.addRenderableWidget(titleField);
		descField = new EditBox(font, 0, 0, 80, 16, Component.empty());
		descField.setMaxLength(512);
		descField.setResponder(s -> descText = s);
		this.addRenderableWidget(descField);

		titleEditBtn = this.addRenderableWidget(Button.builder(
			Component.translatable("screen.bj_mapedit.edit_text"), b -> openTextEditor(true)
		).bounds(0, 0, 72, 18).build());
		descEditBtn = this.addRenderableWidget(Button.builder(
			Component.translatable("screen.bj_mapedit.edit_text"), b -> openTextEditor(false)
		).bounds(0, 0, 72, 18).build());

		taskBtn = this.addRenderableWidget(Button.builder(
			Component.translatable("screen.bj_mapedit.adv_frame_task"), b -> setFrame(0)
		).bounds(0, 0, 52, 18).build());
		goalBtn = this.addRenderableWidget(Button.builder(
			Component.translatable("screen.bj_mapedit.adv_frame_goal"), b -> setFrame(1)
		).bounds(0, 0, 52, 18).build());
		challengeBtn = this.addRenderableWidget(Button.builder(
			Component.translatable("screen.bj_mapedit.adv_frame_challenge"), b -> setFrame(2)
		).bounds(0, 0, 52, 18).build());

		toastBox = this.addRenderableWidget(Checkbox.builder(
			Component.translatable("screen.bj_mapedit.adv_toast"), font).pos(0, 0).selected(showToast).build());
		chatBox = this.addRenderableWidget(Checkbox.builder(
			Component.translatable("screen.bj_mapedit.adv_chat"), font).pos(0, 0).selected(announceChat).build());
		hiddenBox = this.addRenderableWidget(Checkbox.builder(
			Component.translatable("screen.bj_mapedit.adv_hidden"), font).pos(0, 0).selected(hidden).build());

		parentField = new EditBox(font, 0, 0, 80, 16, Component.empty());
		parentField.setMaxLength(128);
		parentField.setResponder(s -> {
			parentText = s;
			updateSuggestions(SuggestKind.PARENT);
			layoutButtons();
		});
		this.addRenderableWidget(parentField);

		backgroundField = new EditBox(font, 0, 0, 80, 16, Component.empty());
		backgroundField.setMaxLength(192);
		backgroundField.setResponder(s -> {
			backgroundText = s;
			updateSuggestions(SuggestKind.BACKGROUND);
		});
		this.addRenderableWidget(backgroundField);

		triggerField = new EditBox(font, 0, 0, 80, 16, Component.empty());
		triggerField.setMaxLength(128);
		triggerField.setResponder(s -> {
			triggerText = s;
			updateSuggestions(SuggestKind.TRIGGER);
			layoutButtons();
		});
		this.addRenderableWidget(triggerField);

		condField = new EditBox(font, 0, 0, 80, 16, Component.empty());
		condField.setMaxLength(128);
		condField.setResponder(s -> {
			condText = s;
			updateSuggestions(SuggestKind.COND);
		});
		this.addRenderableWidget(condField);

		condPickBtn = this.addRenderableWidget(Button.builder(
			Component.translatable("screen.bj_mapedit.inv_pick_btn"), b -> invPicker.open()
		).bounds(0, 0, 24, 16).build());

		overworldBtn = this.addRenderableWidget(Button.builder(
			Component.translatable(DIM_KEYS[0]), b -> setDimension(0)
		).bounds(0, 0, 52, 18).build());
		netherBtn = this.addRenderableWidget(Button.builder(
			Component.translatable(DIM_KEYS[1]), b -> setDimension(1)
		).bounds(0, 0, 52, 18).build());
		endBtn = this.addRenderableWidget(Button.builder(
			Component.translatable(DIM_KEYS[2]), b -> setDimension(2)
		).bounds(0, 0, 52, 18).build());

		xpField = new EditBox(font, 0, 0, 48, 16, Component.empty());
		xpField.setMaxLength(8);
		xpField.setResponder(s -> xpText = s);
		this.addRenderableWidget(xpField);

		nameField = new EditBox(font, 0, -1000, 80, 16, Component.empty());
		nameField.setMaxLength(64);
		nameField.setVisible(false);
		this.addRenderableWidget(nameField);

		copyBtn = this.addRenderableWidget(Button.builder(
			Component.translatable("screen.bj_mapedit.copy_json"), b -> copyJsonToClipboard()
		).bounds(0, 0, 80, 20).build());
		saveBtn = this.addRenderableWidget(Button.builder(
			Component.translatable("screen.bj_mapedit.save"), b -> requestSave()
		).bounds(0, 0, 80, 20).build());
		nameCancelBtn = this.addRenderableWidget(Button.builder(
			Component.translatable("screen.bj_mapedit.cancel"), b -> closeNamePopup()
		).bounds(0, -1000, 72, 20).build());
		nameOkBtn = this.addRenderableWidget(Button.builder(
			Component.translatable("screen.bj_mapedit.save"), b -> confirmNamePopup()
		).bounds(0, -1000, 72, 20).build());
		nameCancelBtn.visible = false;
		nameOkBtn.visible = false;

		if (first && session != null) applySession(session);
		applyFieldsToWidgets();
		syncOptionButtons();
		layoutButtons();
		updateSearchResults();
		refreshList();
		if (EditorAccess.isRemoteWorld()) {
			DatapackNet.send(DatapackOps.LIST, "", "");
		}
		if (keepNaming) {
			namingDraft = keepDraft;
			openNamePopup(false);
			nameField.setValue(keepDraft);
			namingError = keepErr;
			layoutNamePopup();
		} else {
			setFocused(null);
		}
	}

	private void applyFieldsToWidgets() {
		if (titleField != null) titleField.setValue(titleText);
		if (descField != null) descField.setValue(descText);
		if (parentField != null) parentField.setValue(parentText);
		if (backgroundField != null) backgroundField.setValue(backgroundDisplay());
		if (triggerField != null) triggerField.setValue(triggerDisplay(triggerText));
		if (condField != null) condField.setValue(condText);
		if (xpField != null) xpField.setValue(xpText);
		setCheckedIfNeeded(toastBox, showToast);
		setCheckedIfNeeded(chatBox, announceChat);
		setCheckedIfNeeded(hiddenBox, hidden);
	}

	private void setCheckedIfNeeded(Checkbox box, boolean want) {
		if (box != null && box.selected() != want) box.onPress();
	}

	private void openTextEditor(boolean title) {
		pullWidgetState();
		String cur = title ? titleText : descText;
		Minecraft.getInstance().setScreen(new TextEditorScreen(this, cur, text -> {
			if (text != null) {
				if (title) titleText = text;
				else descText = text;
			}
		}, Component.translatable(title ? "screen.bj_mapedit.adv_field_title" : "screen.bj_mapedit.adv_field_desc")));
	}

	private void pullWidgetState() {
		if (titleField != null) titleText = titleField.getValue();
		if (descField != null) descText = descField.getValue();
		if (parentField != null) parentText = parentField.getValue();
		if (backgroundField != null) backgroundText = resolveBackground(backgroundField.getValue());
		if (triggerField != null) triggerText = resolveTrigger(triggerField.getValue());
		if (condField != null) condText = condField.getValue();
		if (xpField != null) xpText = xpField.getValue();
		if (toastBox != null) showToast = toastBox.selected();
		if (chatBox != null) announceChat = chatBox.selected();
		if (hiddenBox != null) hidden = hiddenBox.selected();
	}

	private void setFrame(int idx) {
		if (frameIdx == idx) return;
		frameIdx = idx;
		syncOptionButtons();
		layoutButtons();
	}

	private void setDimension(int idx) {
		condText = DIM_IDS[idx];
		if (condField != null) condField.setValue(condText);
		syncOptionButtons();
		layoutButtons();
	}

	private void syncOptionButtons() {
		if (taskBtn != null) taskBtn.active = frameIdx != 0;
		if (goalBtn != null) goalBtn.active = frameIdx != 1;
		if (challengeBtn != null) challengeBtn.active = frameIdx != 2;
		String dim = resolveDimension(condText);
		if (overworldBtn != null) overworldBtn.active = !DIM_IDS[0].equals(dim);
		if (netherBtn != null) netherBtn.active = !DIM_IDS[1].equals(dim);
		if (endBtn != null) endBtn.active = !DIM_IDS[2].equals(dim);
	}

	private void flashSaveMessage() {
		if (saveBtn == null) return;
		saveBtn.setMessage(Component.translatable("screen.bj_mapedit.saved"));
		saveHintUntil = Util.getMillis() + 2500L;
	}

	private void clearSaveHint() {
		if (saveHintUntil == 0) return;
		saveHintUntil = 0;
		if (saveBtn != null) {
			saveBtn.setMessage(Component.translatable("screen.bj_mapedit.save"));
		}
	}

	@Override
	public void tick() {
		super.tick();
		if (saveHintUntil != 0 && Util.getMillis() >= saveHintUntil) {
			clearSaveHint();
		}
	}

	private void layoutPanel() {
		int margin = 8;
		int gap = 8;
		int availW = Math.max(32, this.width - margin * 2);
		int availH = Math.max(32, this.height - margin * 2);
		railW = Mth.clamp(availW * 22 / 100, 112, 220);
		rightRailW = Mth.clamp(availW * 22 / 100, 112, 220);
		if (availW < 520) {
			railW = Mth.clamp(availW * 24 / 100, 88, 160);
			rightRailW = Mth.clamp(availW * 24 / 100, 88, 160);
		}
		WIDTH = Math.max(8, availW - railW - rightRailW - gap * 2);
		HEIGHT = availH;
		railX = margin;
		leftPos = margin + railW + gap;
		rightRailX = leftPos + WIDTH + gap;
		topPos = margin;
		CONTENT_TOP = UiTheme.HEADER_H + 8;
		refreshX = UiTheme.headerIconX(leftPos, WIDTH);
		refreshY = UiTheme.headerIconY(topPos);
	}

	private void layoutButtons() {
		if (saveBtn == null || titleField == null) return;
		int pad = 12;
		int x = leftPos + pad;
		int inner = Math.max(64, WIDTH - pad * 2);
		int y = topPos + CONTENT_TOP + 2;
		int editW = Math.max(64, font.width(titleEditBtn.getMessage()) + 16);

		titleLabelY = y;
		y += 11;
		titleField.setPosition(x, y);
		titleField.setWidth(Math.max(40, inner - editW - 4));
		titleField.setHeight(16);
		titleEditBtn.setPosition(x + titleField.getWidth() + 4, y - 1);
		titleEditBtn.setWidth(editW);
		titleEditBtn.setHeight(18);
		y += 22;

		descLabelY = y;
		y += 11;
		descField.setPosition(x, y);
		descField.setWidth(titleField.getWidth());
		descField.setHeight(16);
		descEditBtn.setPosition(titleEditBtn.getX(), y - 1);
		descEditBtn.setWidth(editW);
		descEditBtn.setHeight(18);
		y += 22;

		iconSlotX = x + font.width(Component.translatable("screen.bj_mapedit.adv_icon")) + 6;
		iconSlotY = y + 1;
		int fx = iconSlotX + 22;
		int fw = Math.max(44, font.width(taskBtn.getMessage()) + 12);
		int gw = Math.max(44, font.width(goalBtn.getMessage()) + 12);
		int cw = Math.max(44, font.width(challengeBtn.getMessage()) + 12);
		if (fx + fw + 4 + gw + 4 + cw > leftPos + WIDTH - pad) {
			y += 22;
			fx = x;
		}
		taskBtn.setPosition(fx, y);
		taskBtn.setWidth(fw);
		taskBtn.setHeight(18);
		goalBtn.setPosition(fx + fw + 4, y);
		goalBtn.setWidth(gw);
		goalBtn.setHeight(18);
		challengeBtn.setPosition(fx + fw + 4 + gw + 4, y);
		challengeBtn.setWidth(cw);
		challengeBtn.setHeight(18);
		y += 24;

		int toastW = font.width(toastBox.getMessage()) + 24;
		int chatW = font.width(chatBox.getMessage()) + 24;
		int hidW = font.width(hiddenBox.getMessage()) + 24;
		toastBox.setPosition(x, y);
		toastBox.setWidth(toastW);
		if (x + toastW + 8 + chatW + 8 + hidW > leftPos + WIDTH - pad) {
			chatBox.setPosition(x, y + 18);
			hiddenBox.setPosition(x + chatW + 8, y + 18);
			y += 38;
		} else {
			chatBox.setPosition(x + toastW + 8, y);
			hiddenBox.setPosition(x + toastW + 8 + chatW + 8, y);
			y += 20;
		}
		chatBox.setWidth(chatW);
		hiddenBox.setWidth(hidW);

		parentLabelY = y;
		y += 11;
		parentField.setPosition(x, y);
		parentField.setWidth(inner);
		parentField.setHeight(16);
		y += 20;

		boolean root = parentText == null || parentText.trim().isEmpty();
		backgroundField.visible = root;
		if (root) {
			backgroundLabelY = y;
			y += 11;
			backgroundField.setPosition(x, y);
			backgroundField.setWidth(inner);
			backgroundField.setHeight(16);
			y += 20;
		} else {
			backgroundLabelY = -1000;
			backgroundField.setPosition(x, -1000);
		}

		triggerLabelY = y;
		y += 11;
		triggerField.setPosition(x, y);
		triggerField.setWidth(inner);
		triggerField.setHeight(16);
		y += 20;

		boolean dim = condKind() == CondKind.DIM;
		boolean needCond = condKind() != CondKind.NONE;
		overworldBtn.visible = dim;
		netherBtn.visible = dim;
		endBtn.visible = dim;
		condField.visible = needCond && !dim;
		condSlotVisible = condKind() == CondKind.ITEM || condKind() == CondKind.BLOCK;
		condPickBtn.visible = condSlotVisible;
		condPickBtn.setPosition(x, -1000);
		if (needCond) {
			condLabelY = y;
			y += 11;
			if (dim) {
				int dw = Math.max(48, font.width(overworldBtn.getMessage()) + 12);
				int nw = Math.max(48, font.width(netherBtn.getMessage()) + 12);
				int ew = Math.max(48, font.width(endBtn.getMessage()) + 12);
				overworldBtn.setPosition(x, y);
				overworldBtn.setWidth(dw);
				overworldBtn.setHeight(18);
				netherBtn.setPosition(x + dw + 4, y);
				netherBtn.setWidth(nw);
				netherBtn.setHeight(18);
				endBtn.setPosition(x + dw + 4 + nw + 4, y);
				endBtn.setWidth(ew);
				endBtn.setHeight(18);
				condField.setPosition(x, -1000);
				y += 22;
			} else {
				int slot = condSlotVisible ? 22 : 0;
				int pick = 0;
				if (condSlotVisible) {
					pick = Math.max(22, font.width(condPickBtn.getMessage()) + 10);
					condPickBtn.setPosition(x + inner - pick, y);
					condPickBtn.setWidth(pick);
					condPickBtn.setHeight(16);
					pick += 4;
				}
				condSlotX = x;
				condSlotY = y;
				condField.setPosition(x + slot, y);
				condField.setWidth(Math.max(40, inner - slot - pick));
				condField.setHeight(16);
				overworldBtn.setPosition(x, -1000);
				netherBtn.setPosition(x, -1000);
				endBtn.setPosition(x, -1000);
				y += 20;
			}
		} else {
			condLabelY = -1000;
			condField.setPosition(x, -1000);
			overworldBtn.setPosition(x, -1000);
			netherBtn.setPosition(x, -1000);
			endBtn.setPosition(x, -1000);
		}

		xpLabelY = y;
		y += 11;
		xpField.setPosition(x, y);
		xpField.setWidth(Math.min(72, inner));
		xpField.setHeight(16);
		y += 22;
		idLabelY = y;

		searchField.setPosition(railX + 8, topPos + UiTheme.HEADER_H + 6);
		searchField.setWidth(Math.max(24, railW - 16));
		searchField.setHeight(16);
		listSearchField.setPosition(rightRailX + 8, topPos + UiTheme.HEADER_H + 6);
		listSearchField.setWidth(Math.max(24, rightRailW - 16));
		listSearchField.setHeight(16);

		int footerY = topPos + HEIGHT - FOOTER_H + (FOOTER_H - 20) / 2;
		int copyW = Math.max(72, font.width(copyBtn.getMessage()) + 16);
		int saveW = Math.max(72, Math.max(
			font.width(Component.translatable("screen.bj_mapedit.save")),
			font.width(Component.translatable("screen.bj_mapedit.saved"))
		) + 22);
		if (8 + copyW + 8 + saveW + 8 > WIDTH) {
			copyW = Math.max(52, copyW - (8 + copyW + 8 + saveW + 8 - WIDTH));
		}
		copyBtn.setPosition(leftPos + 8, footerY);
		copyBtn.setWidth(copyW);
		copyBtn.setHeight(20);
		saveBtn.setPosition(leftPos + WIDTH - 8 - saveW, footerY);
		saveBtn.setWidth(saveW);
		saveBtn.setHeight(20);
		saveBtn.active = !EditorAccess.isRemoteWorld() || EditorAccess.canSendDatapack();
		if (namingOpen) layoutNamePopup();
		updatePaletteScroll();
		updateListScroll();
	}

	private boolean handleFooterClick(double mx, double my, int button) {
		if (!UiTheme.inFooterBar(mx, my, leftPos, topPos, WIDTH, HEIGHT, FOOTER_H)) return false;
		if (UiTheme.clickWidgets(mx, my, button, copyBtn, saveBtn)) return true;
		if (button == 0) setFocused(null);
		return true;
	}

	private int paletteListTop() {
		return topPos + UiTheme.HEADER_H + 6 + 16 + 8;
	}

	private int paletteListBottom() {
		return topPos + HEIGHT - 8;
	}

	private int paletteListH() {
		return Math.max(1, paletteListBottom() - paletteListTop());
	}

	private int paletteScrollX() {
		return railX + railW - 7;
	}

	private int advListTop() {
		return paletteListTop();
	}

	private int advListBottom() {
		return paletteListBottom();
	}

	private int advListH() {
		return paletteListH();
	}

	private int listScrollBarX() {
		return rightRailX + rightRailW - 7;
	}

	private void updateListScroll() {
		listMaxScroll = Math.max(0, rows.size() * ROW_H - advListH());
		listScroll = Mth.clamp(listScroll, 0, listMaxScroll);
	}

	private int paletteCols() {
		int listW = Math.max(PALETTE_CELL, railW - 16);
		return Math.max(4, listW / PALETTE_CELL);
	}

	private static List<ResourceLocation> ALL_ITEMS;

	private List<ResourceLocation> paletteItems() {
		return searching ? searchResults : allItems();
	}

	private static List<ResourceLocation> allItems() {
		if (ALL_ITEMS != null) return ALL_ITEMS;
		LinkedHashSet<ResourceLocation> set = new LinkedHashSet<>(COMMON_ITEMS);
		for (ResourceLocation id : BuiltInRegistries.ITEM.keySet()) {
			if ("air".equals(id.getPath())) continue;
			set.add(id);
		}
		ALL_ITEMS = List.copyOf(set);
		return ALL_ITEMS;
	}

	private void updatePaletteScroll() {
		int cols = paletteCols();
		int itemRows = (paletteItems().size() + cols - 1) / cols;
		maxSearchScroll = Math.max(0, itemRows * PALETTE_CELL - paletteListH());
		searchScroll = Mth.clamp(searchScroll, 0, maxSearchScroll);
	}

	private void updateSearchResults() {
		String query = searchField == null ? "" : searchField.getValue().trim().toLowerCase(Locale.ROOT);
		searching = !query.isEmpty();
		searchScroll = 0;
		if (!searching) {
			searchResults = List.of();
		} else {
			searchResults = BuiltInRegistries.ITEM.keySet().stream()
				.filter(id -> matchesItem(id, query))
				.limit(800)
				.toList();
		}
		updatePaletteScroll();
	}

	private static boolean matchesItem(ResourceLocation id, String query) {
		if (id.getPath().equals("air")) return false;
		if (id.toString().toLowerCase(Locale.ROOT).contains(query)) return true;
		if (id.getPath().toLowerCase(Locale.ROOT).contains(query)) return true;
		Item item = BuiltInRegistries.ITEM.get(id);
		if (item == Items.AIR) return false;
		ItemStack stack = new ItemStack(item);
		if (containsIgnoreCase(item.getDescription().getString(), query)) return true;
		return containsIgnoreCase(item.getName(stack).getString(), query);
	}

	private static boolean containsIgnoreCase(String text, String query) {
		return text != null && text.toLowerCase(Locale.ROOT).contains(query);
	}

	private enum CondKind { NONE, ITEM, BLOCK, ENTITY, RECIPE, DIM, BIOME }

	private CondKind condKind() {
		return condKindOf(resolveTrigger(triggerText));
	}

	private static CondKind condKindOf(String trigger) {
		String t = stripMc(trigger);
		return switch (t) {
			case "inventory_changed", "consume_item" -> CondKind.ITEM;
			case "placed_block", "enter_block" -> CondKind.BLOCK;
			case "player_killed_entity", "bred_animals", "tame_animal" -> CondKind.ENTITY;
			case "recipe_unlocked" -> CondKind.RECIPE;
			case "changed_dimension" -> CondKind.DIM;
			case "location" -> CondKind.BIOME;
			default -> CondKind.NONE;
		};
	}

	private static String stripMc(String id) {
		if (id == null) return "";
		String s = id.trim();
		if (s.startsWith("minecraft:")) return s.substring("minecraft:".length());
		return s;
	}

	private boolean suggestOpen() {
		return !namingOpen && suggestKind != SuggestKind.NONE && !suggestions.isEmpty() && suggestField() != null && suggestField().isFocused();
	}

	private EditBox suggestField() {
		return switch (suggestKind) {
			case PARENT -> parentField;
			case BACKGROUND -> backgroundField;
			case TRIGGER -> triggerField;
			case COND -> condField;
			default -> null;
		};
	}

	private int suggestListY() {
		EditBox box = suggestField();
		if (box == null) return 0;
		return SuggestionPopup.listY(box, suggestions.size(), topPos + HEIGHT - FOOTER_H);
	}

	private void updateSuggestions(SuggestKind kind) {
		if (namingOpen) {
			suggestions = List.of();
			suggestionIdx = -1;
			suggestKind = SuggestKind.NONE;
			return;
		}
		EditBox box = switch (kind) {
			case PARENT -> parentField;
			case BACKGROUND -> backgroundField;
			case TRIGGER -> triggerField;
			case COND -> condField;
			default -> null;
		};
		if (box == null || !box.isFocused()) {
			if (suggestKind == kind) {
				suggestions = List.of();
				suggestionIdx = -1;
				suggestKind = SuggestKind.NONE;
			}
			return;
		}
		suggestKind = kind;
		String input = box.getValue().trim();
		List<Suggest> all = switch (kind) {
			case PARENT -> parentSuggestions();
			case BACKGROUND -> backgroundSuggestions();
			case TRIGGER -> triggerSuggestions();
			case COND -> condSuggestions();
			default -> List.of();
		};
		if (!input.isEmpty() && completeSuggest(all, input)) {
			suggestions = List.of();
			suggestionIdx = -1;
			suggestKind = SuggestKind.NONE;
			return;
		}
		String lower = input.toLowerCase(Locale.ROOT);
		suggestions = all.stream()
			.filter(s -> input.isEmpty()
				|| s.id.toLowerCase(Locale.ROOT).contains(lower)
				|| s.label.toLowerCase(Locale.ROOT).contains(lower))
			.limit(80)
			.toList();
		suggestionIdx = suggestions.isEmpty() ? -1 : 0;
		suggestionScroll = SuggestionPopup.clampScroll(suggestionScroll, suggestions.size());
	}

	private static boolean completeSuggest(List<Suggest> all, String input) {
		for (Suggest s : all) {
			if (s.label.equals(input) || s.id.equalsIgnoreCase(input)) return true;
		}
		return false;
	}

	private List<Suggest> parentSuggestions() {
		LinkedHashMap<String, String> map = new LinkedHashMap<>();
		for (ListedAdv row : loadedAdvRows()) {
			map.put(row.id.toString(), row.label + "  (" + row.id + ")");
		}
		List<Suggest> out = new ArrayList<>();
		for (var e : map.entrySet()) out.add(new Suggest(e.getKey(), e.getValue()));
		return out;
	}

	private List<Suggest> backgroundSuggestions() {
		List<Suggest> out = new ArrayList<>();
		for (int i = 0; i < BACKGROUNDS.length; i++) {
			out.add(new Suggest(BACKGROUNDS[i], Component.translatable(BACKGROUND_KEYS[i]).getString()));
		}
		return out;
	}

	private List<Suggest> triggerSuggestions() {
		List<Suggest> out = new ArrayList<>();
		for (int i = 0; i < TRIGGER_IDS.length; i++) {
			out.add(new Suggest(TRIGGER_IDS[i], Component.translatable(TRIGGER_KEYS[i]).getString()));
		}
		return out;
	}

	private List<Suggest> condSuggestions() {
		return switch (condKind()) {
			case ITEM, BLOCK -> itemSuggestions();
			case ENTITY -> entitySuggestions();
			case RECIPE -> recipeSuggestions();
			case BIOME -> biomeSuggestions();
			case DIM -> {
				List<Suggest> out = new ArrayList<>();
				for (int i = 0; i < DIM_IDS.length; i++) {
					out.add(new Suggest(DIM_IDS[i], Component.translatable(DIM_KEYS[i]).getString()));
				}
				yield out;
			}
			default -> List.of();
		};
	}

	private List<Suggest> itemSuggestions() {
		List<Suggest> out = new ArrayList<>();
		for (ResourceLocation id : BuiltInRegistries.ITEM.keySet()) {
			if ("air".equals(id.getPath())) continue;
			Item item = BuiltInRegistries.ITEM.get(id);
			if (item == Items.AIR) continue;
			out.add(new Suggest(id.toString(), item.getDescription().getString()));
		}
		return out;
	}

	private List<Suggest> entitySuggestions() {
		List<Suggest> out = new ArrayList<>();
		for (ResourceLocation id : BuiltInRegistries.ENTITY_TYPE.keySet()) {
			EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(id);
			out.add(new Suggest(id.toString(), type.getDescription().getString()));
		}
		return out;
	}

	private List<Suggest> recipeSuggestions() {
		List<Suggest> out = new ArrayList<>();
		RecipeManager rm = recipeManager();
		if (rm == null) return out;
		for (RecipeHolder<?> holder : rm.getRecipes()) {
			out.add(new Suggest(holder.id().toString(), holder.id().toString()));
		}
		return out;
	}

	private List<Suggest> biomeSuggestions() {
		List<Suggest> out = new ArrayList<>();
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) return out;
		var reg = mc.level.registryAccess().registryOrThrow(Registries.BIOME);
		for (var entry : reg.entrySet()) {
			ResourceLocation id = entry.getKey().location();
			out.add(new Suggest(id.toString(), id.toString()));
		}
		return out;
	}

	private RecipeManager recipeManager() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.getSingleplayerServer() != null) return mc.getSingleplayerServer().getRecipeManager();
		return mc.level != null ? mc.level.getRecipeManager() : null;
	}

	private void pickSuggestion(int idx) {
		if (idx < 0 || idx >= suggestions.size()) return;
		Suggest s = suggestions.get(idx);
		EditBox box = suggestField();
		if (box == null) return;
		if (suggestKind == SuggestKind.TRIGGER) {
			triggerText = s.id;
			box.setValue(s.label);
		} else if (suggestKind == SuggestKind.BACKGROUND) {
			backgroundText = s.id;
			box.setValue(s.label);
		} else if (suggestKind == SuggestKind.COND) {
			condText = s.id;
			box.setValue(s.id);
		} else {
			box.setValue(s.id);
			if (suggestKind == SuggestKind.PARENT) parentText = s.id;
		}
		suggestions = List.of();
		suggestionIdx = -1;
		suggestKind = SuggestKind.NONE;
		syncOptionButtons();
		layoutButtons();
		setFocused(box);
		box.setFocused(true);
		box.moveCursorToEnd(false);
	}

	private boolean clickSuggestion(double mx, double my, int button) {
		if (!suggestOpen()) return false;
		EditBox box = suggestField();
		int sx = box.getX();
		int sy = suggestListY();
		int sw = box.getWidth();
		if (button == 0) {
			Integer bar = SuggestionPopup.pressBar(sx, sy, sw, suggestions.size(), suggestionScroll, mx, my);
			if (bar != null) {
				suggestionScroll = bar;
				return true;
			}
		}
		int hit = SuggestionPopup.hitIndex(mx, my, sx, sy, sw, suggestions.size(), suggestionScroll);
		if (hit >= 0) {
			if (button == 0) pickSuggestion(hit);
			return true;
		}
		return SuggestionPopup.contains(mx, my, sx, sy, sw, suggestions.size());
	}

	private boolean overSuggestions(double mx, double my) {
		if (!suggestOpen()) return false;
		EditBox box = suggestField();
		return SuggestionPopup.contains(mx, my, box.getX(), suggestListY(), box.getWidth(), suggestions.size());
	}

	private String triggerDisplay(String raw) {
		String id = resolveTrigger(raw);
		for (int i = 0; i < TRIGGER_IDS.length; i++) {
			if (TRIGGER_IDS[i].equals(id)) return Component.translatable(TRIGGER_KEYS[i]).getString();
		}
		return raw == null ? "" : raw;
	}

	private String resolveTrigger(String raw) {
		if (raw == null) return TRIGGER_IDS[0];
		String s = raw.trim();
		if (s.isEmpty()) return TRIGGER_IDS[0];
		for (int i = 0; i < TRIGGER_IDS.length; i++) {
			if (TRIGGER_IDS[i].equalsIgnoreCase(s)
				|| Component.translatable(TRIGGER_KEYS[i]).getString().equals(s)
				|| stripMc(TRIGGER_IDS[i]).equalsIgnoreCase(s)) {
				return TRIGGER_IDS[i];
			}
		}
		ResourceLocation id = SafeIds.tryParseItem(s);
		return id != null ? id.toString() : s;
	}

	private String backgroundDisplay() {
		String id = resolveBackground(backgroundText);
		for (int i = 0; i < BACKGROUNDS.length; i++) {
			if (BACKGROUNDS[i].equals(id)) return Component.translatable(BACKGROUND_KEYS[i]).getString();
		}
		return backgroundText == null ? "" : backgroundText;
	}

	private String resolveBackground(String raw) {
		if (raw == null) return "";
		String s = raw.trim();
		if (s.isEmpty()) return "";
		for (int i = 0; i < BACKGROUNDS.length; i++) {
			if (BACKGROUNDS[i].equalsIgnoreCase(s)
				|| Component.translatable(BACKGROUND_KEYS[i]).getString().equals(s)) {
				return BACKGROUNDS[i];
			}
		}
		return s;
	}

	private String resolveDimension(String raw) {
		if (raw == null) return "";
		String s = raw.trim();
		for (int i = 0; i < DIM_IDS.length; i++) {
			if (DIM_IDS[i].equalsIgnoreCase(s)
				|| Component.translatable(DIM_KEYS[i]).getString().equals(s)
				|| stripMc(DIM_IDS[i]).equalsIgnoreCase(s)) {
				return DIM_IDS[i];
			}
		}
		ResourceLocation id = SafeIds.tryParseItem(s);
		return id != null ? id.toString() : s;
	}

	private String resolveParent() {
		String s = parentText == null ? "" : parentText.trim();
		if (s.isEmpty()) return "";
		ResourceLocation id = SafeIds.tryParse(s);
		if (id != null) return id.toString();
		id = SafeIds.tryParseItem(s);
		return id != null ? id.toString() : s;
	}

	private static String frameId(int idx) {
		return switch (idx) {
			case 1 -> "goal";
			case 2 -> "challenge";
			default -> "task";
		};
	}

	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		super.renderBackground(g, mouseX, mouseY, partialTick);
		UiTheme.drawPanel(g, railX, topPos, railW, HEIGHT);
		UiTheme.drawPanel(g, leftPos, topPos, WIDTH, HEIGHT);
		UiTheme.drawPanel(g, rightRailX, topPos, rightRailW, HEIGHT);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		this.renderBackground(g, mouseX, mouseY, partialTick);
		UiTheme.drawHeader(g, font, Component.translatable("screen.bj_mapedit.recipe_items"), railX, topPos, railW);
		UiTheme.drawHeader(g, font, this.title, leftPos, topPos, WIDTH);
		UiTheme.drawHeader(g, font, Component.translatable("screen.bj_mapedit.adv_existing"), rightRailX, topPos, rightRailW);
		boolean refreshHover = UiTheme.hitIcon(refreshX, refreshY, mouseX, mouseY);
		UiTheme.drawRefreshIcon(g, refreshX, refreshY, refreshHover);

		renderFormLabels(g);
		renderIconSlot(g, mouseX, mouseY);
		if (condSlotVisible) renderCondSlot(g, mouseX, mouseY);
		renderPalette(g, mouseX, mouseY);
		renderAdvList(g, mouseX, mouseY);

		int fy = topPos + HEIGHT - FOOTER_H;
		g.fill(leftPos + 1, fy, leftPos + WIDTH - 1, topPos + HEIGHT - 1, 0xFF191A1B);
		g.fill(leftPos + 1, fy, leftPos + WIDTH - 1, fy + 1, 0xFF2B2C2D);

		for (var child : this.children()) {
			if (child instanceof AbstractWidget w && !isNamingWidget(w)) {
				w.render(g, mouseX, mouseY, partialTick);
			}
		}

		if (suggestOpen()) {
			UiTheme.pushOverlay(g);
			EditBox box = suggestField();
			SuggestionPopup.draw(g, font, box.getX(), suggestListY(), box.getWidth(), suggestions.size(),
				suggestionScroll, suggestionIdx, mouseX, mouseY, i -> suggestions.get(i).label);
			UiTheme.popOverlay(g);
		}

		if (namingOpen) {
			renderNamePopup(g, mouseX, mouseY, partialTick);
		} else if (invPicker.isOpen()) {
			invPicker.render(g, font, this.width, this.height, mouseX, mouseY);
		} else {
			rowMenu.draw(g, font, mouseX, mouseY);
			if (dragActive && dragItem != null) {
				drawDragGhost(g, mouseX, mouseY);
			} else if (!rowMenu.isOpen()) {
				ResourceLocation tipId = hoveredItem(mouseX, mouseY);
				if (tipId != null) {
					ItemStack stack = stackOf(tipId, 1);
					if (!stack.isEmpty()) {
						g.renderTooltip(font, stack.getHoverName(), mouseX, mouseY);
					}
				} else if (refreshHover) {
					g.renderTooltip(font, Component.translatable("screen.bj_mapedit.reset"), mouseX, mouseY);
				}
			}
		}
	}

	private void renderFormLabels(GuiGraphics g) {
		int x = leftPos + 12;
		UiTheme.label(g, font, Component.translatable("screen.bj_mapedit.adv_field_title"), x, titleLabelY);
		UiTheme.label(g, font, Component.translatable("screen.bj_mapedit.adv_field_desc"), x, descLabelY);
		UiTheme.label(g, font, Component.translatable("screen.bj_mapedit.adv_icon"), x, iconSlotY + 5);
		UiTheme.label(g, font, Component.translatable("screen.bj_mapedit.adv_parent"), x, parentLabelY);
		if (backgroundField != null && backgroundField.visible) {
			UiTheme.label(g, font, Component.translatable("screen.bj_mapedit.adv_background"), x, backgroundLabelY);
		}
		UiTheme.label(g, font, Component.translatable("screen.bj_mapedit.adv_trigger"), x, triggerLabelY);
		if (condKind() != CondKind.NONE) {
			UiTheme.label(g, font, condLabel(), x, condLabelY);
		}
		UiTheme.label(g, font, Component.translatable("screen.bj_mapedit.adv_xp"), x, xpLabelY);
		String id = editingId != null ? editingId.toString() : Component.translatable("screen.bj_mapedit.adv_unsaved").getString();
		UiTheme.muted(g, font, id, x, idLabelY);
	}

	private Component condLabel() {
		return switch (condKind()) {
			case ITEM -> Component.translatable("screen.bj_mapedit.adv_condition_item");
			case BLOCK -> Component.translatable("screen.bj_mapedit.adv_condition_block");
			case ENTITY -> Component.translatable("screen.bj_mapedit.adv_condition_entity");
			case RECIPE -> Component.translatable("screen.bj_mapedit.adv_condition_recipe");
			case DIM -> Component.translatable("screen.bj_mapedit.adv_condition_dim");
			case BIOME -> Component.translatable("screen.bj_mapedit.adv_condition_biome");
			default -> Component.translatable("screen.bj_mapedit.adv_condition");
		};
	}

	private void renderIconSlot(GuiGraphics g, int mouseX, int mouseY) {
		boolean hover = !dragActive && hitIconSlot(mouseX, mouseY);
		if (hover) g.fill(iconSlotX, iconSlotY, iconSlotX + 18, iconSlotY + 18, HOVER_FILL);
		UiTheme.drawSlotBox(g, iconSlotX, iconSlotY, 18);
		ItemStack stack = stackOf(iconId, 1);
		if (!stack.isEmpty() && !(dragActive && dragSource == SRC_ICON)) {
			g.renderItem(stack, iconSlotX + 1, iconSlotY + 1);
			g.renderItemDecorations(font, stack, iconSlotX + 1, iconSlotY + 1);
		}
	}

	private void renderCondSlot(GuiGraphics g, int mouseX, int mouseY) {
		boolean hover = !dragActive && hitCondSlot(mouseX, mouseY);
		if (hover) g.fill(condSlotX, condSlotY, condSlotX + 18, condSlotY + 18, HOVER_FILL);
		UiTheme.drawSlotBox(g, condSlotX, condSlotY, 18);
		ResourceLocation id = SafeIds.tryParseItem(condText);
		ItemStack stack = stackOf(id, 1);
		if (!stack.isEmpty() && !(dragActive && dragSource == SRC_COND)) {
			g.renderItem(stack, condSlotX + 1, condSlotY + 1);
			g.renderItemDecorations(font, stack, condSlotX + 1, condSlotY + 1);
		}
	}

	private void drawDragGhost(GuiGraphics g, int mouseX, int mouseY) {
		ItemStack stack = stackOf(dragItem, 1);
		if (stack.isEmpty()) return;
		UiTheme.pushOverlay(g);
		g.renderItem(stack, mouseX - 8, mouseY - 8);
		g.renderItemDecorations(font, stack, mouseX - 8, mouseY - 8);
		UiTheme.popOverlay(g);
	}

	private void renderPalette(GuiGraphics g, int mouseX, int mouseY) {
		int listTop = paletteListTop();
		int listBottom = paletteListBottom();
		int cols = paletteCols();
		int listX = railX + 6;
		List<ResourceLocation> items = paletteItems();
		g.enableScissor(railX + 1, listTop, railX + railW - 1, listBottom);
		int y0 = listTop - searchScroll;
		for (int i = 0; i < items.size(); i++) {
			int row = i / cols;
			int col = i % cols;
			int cx = listX + col * PALETTE_CELL;
			int cy = y0 + row * PALETTE_CELL;
			if (cy + PALETTE_CELL < listTop || cy >= listBottom) continue;
			ResourceLocation id = items.get(i);
			boolean source = dragActive && dragSource == SRC_PALETTE && id.equals(dragItem);
			boolean hovered = !dragActive && mouseX >= cx && mouseX < cx + PALETTE_CELL
				&& mouseY >= cy && mouseY < cy + PALETTE_CELL
				&& mouseY >= listTop && mouseY < listBottom;
			if (source) {
				g.fill(cx + 1, cy + 1, cx + PALETTE_CELL - 1, cy + PALETTE_CELL - 1, SELECTED_FILL);
			} else if (hovered) {
				g.fill(cx + 1, cy + 1, cx + PALETTE_CELL - 1, cy + PALETTE_CELL - 1, HOVER_FILL);
			}
			UiTheme.drawSlotBox(g, cx + 1, cy + 1, 18);
			ItemStack stack = stackOf(id, 1);
			if (!stack.isEmpty()) {
				g.renderItem(stack, cx + 2, cy + 2);
				g.renderItemDecorations(font, stack, cx + 2, cy + 2);
			}
		}
		g.disableScissor();
		if (searching && items.isEmpty()) {
			UiTheme.muted(g, font, Component.translatable("screen.bj_mapedit.recipe_no_match").getString(),
				railX + 8, listTop + 4);
		}
		UiTheme.drawThinScrollBar(g, paletteScrollX(), listTop, paletteListH(), searchScroll, maxSearchScroll);
	}

	private void renderAdvList(GuiGraphics g, int mouseX, int mouseY) {
		int listTop = advListTop();
		int listBottom = advListBottom();
		int hx = mouseX;
		int hy = mouseY;
		if (rowMenu.covers(mouseX, mouseY)) {
			hx = -10000;
			hy = -10000;
		}
		g.enableScissor(rightRailX + 1, listTop, rightRailX + rightRailW - 1, listBottom);
		if (rows.isEmpty()) {
			UiTheme.muted(g, font, Component.translatable("screen.bj_mapedit.adv_empty").getString(),
				rightRailX + 8, listTop + 4);
		} else {
			int nameRight = rightRailX + rightRailW - 8 - (listMaxScroll > 0 ? 6 : 0);
			for (int i = 0; i < rows.size(); i++) {
				int rowY = listTop + i * ROW_H - listScroll;
				if (rowY + ROW_H < listTop || rowY > listBottom) continue;
				ListedAdv row = rows.get(i);
				boolean sel = editingId != null && editingId.equals(row.id);
				boolean hover = hx >= rightRailX + 1 && hx < rightRailX + rightRailW - 1
					&& hy >= rowY && hy < rowY + ROW_H
					&& hy >= listTop && hy < listBottom;
				if (sel) {
					g.fill(rightRailX + 1, rowY, rightRailX + rightRailW - 1, rowY + ROW_H, 0xFF21262D);
				} else if (hover) {
					g.fill(rightRailX + 1, rowY, rightRailX + rightRailW - 1, rowY + ROW_H, 0xFF191A1B);
				}
				int iconX = rightRailX + 6;
				int iconY = rowY + 3;
				if (!row.icon.isEmpty()) {
					g.renderItem(row.icon, iconX, iconY);
				} else {
					UiTheme.drawTemplateFileIcon(g, rightRailX, rowY, hover || sel);
				}
				int nameX = iconX + 18;
				int nameW = Math.max(24, nameRight - nameX - 4);
				int color = hover || sel ? 0xFFE6EDF3 : UiTheme.cListText();
				UiTheme.clipLabel(g, font, row.label, nameX, rowY + 7, nameW, color);
			}
		}
		g.disableScissor();
		UiTheme.drawThinScrollBar(g, listScrollBarX(), listTop, advListH(), listScroll, listMaxScroll);
	}

	private static ItemStack stackOf(ResourceLocation id, int count) {
		if (id == null) return ItemStack.EMPTY;
		Item item = BuiltInRegistries.ITEM.get(id);
		if (item == Items.AIR) return ItemStack.EMPTY;
		return new ItemStack(item, Math.max(1, count));
	}

	private ResourceLocation hoveredItem(int mouseX, int mouseY) {
		if (overSuggestions(mouseX, mouseY)) return null;
		int listTop = paletteListTop();
		int listBottom = paletteListBottom();
		if (mouseX >= railX && mouseX < railX + railW && mouseY >= listTop && mouseY < listBottom) {
			int idx = paletteIndexAt(mouseX, mouseY);
			if (idx >= 0) return paletteItems().get(idx);
		}
		if (hitIconSlot(mouseX, mouseY)) return iconId;
		if (condSlotVisible && hitCondSlot(mouseX, mouseY)) return SafeIds.tryParseItem(condText);
		if (mouseX >= rightRailX && mouseX < rightRailX + rightRailW && mouseY >= advListTop() && mouseY < advListBottom()) {
			int idx = rowIndexAt(mouseY);
			if (idx >= 0 && !rows.get(idx).icon.isEmpty()) {
				return BuiltInRegistries.ITEM.getKey(rows.get(idx).icon.getItem());
			}
		}
		return null;
	}

	private int paletteIndexAt(double mouseX, double mouseY) {
		int listTop = paletteListTop();
		int listBottom = paletteListBottom();
		if (mouseY < listTop || mouseY >= listBottom) return -1;
		int cols = paletteCols();
		int listX = railX + 6;
		int col = (int) ((mouseX - listX) / PALETTE_CELL);
		if (col < 0 || col >= cols) return -1;
		int row = (int) ((mouseY - listTop + searchScroll) / PALETTE_CELL);
		if (row < 0) return -1;
		int idx = row * cols + col;
		List<ResourceLocation> items = paletteItems();
		if (idx < 0 || idx >= items.size()) return -1;
		return idx;
	}

	private boolean hitIconSlot(double mx, double my) {
		return mx >= iconSlotX && mx < iconSlotX + 18 && my >= iconSlotY && my < iconSlotY + 18;
	}

	private boolean hitCondSlot(double mx, double my) {
		return condSlotVisible && mx >= condSlotX && mx < condSlotX + 18 && my >= condSlotY && my < condSlotY + 18;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (namingOpen) {
			if (button == 0 && hitEditBox(nameField, mouseX, mouseY)) {
				setFocused(nameField);
				nameField.setFocused(true);
				return nameField.mouseClicked(mouseX, mouseY, button);
			}
			if (button == 0 && hitWidget(nameOkBtn, mouseX, mouseY)) {
				return nameOkBtn.mouseClicked(mouseX, mouseY, button);
			}
			if (button == 0 && hitWidget(nameCancelBtn, mouseX, mouseY)) {
				return nameCancelBtn.mouseClicked(mouseX, mouseY, button);
			}
			if (!hitNamePopup(mouseX, mouseY)) closeNamePopup();
			return true;
		}
		if (invPicker.isOpen()) {
			if (button == 0) {
				ItemStack picked = invPicker.stackAt(this.width, this.height, mouseX, mouseY);
				if (!picked.isEmpty()) {
					condText = BuiltInRegistries.ITEM.getKey(picked.getItem()).toString();
					if (condField != null) condField.setValue(condText);
					suggestions = List.of();
					suggestionIdx = -1;
					suggestKind = SuggestKind.NONE;
					invPicker.close();
				} else if (!invPicker.inPanel(this.width, this.height, mouseX, mouseY)) {
					invPicker.close();
				}
			} else if (button == 1) {
				invPicker.close();
			}
			return true;
		}
		if (clickSuggestion(mouseX, mouseY, button)) return true;
		if (handleRowMenuClick(mouseX, mouseY)) return true;
		if (button == 0 && !hitEditBox(searchField, mouseX, mouseY) && !hitEditBox(listSearchField, mouseX, mouseY)
			&& !hitEditBox(titleField, mouseX, mouseY) && !hitEditBox(descField, mouseX, mouseY)
			&& !hitEditBox(parentField, mouseX, mouseY) && !hitEditBox(backgroundField, mouseX, mouseY)
			&& !hitEditBox(triggerField, mouseX, mouseY) && !hitEditBox(condField, mouseX, mouseY)
			&& !hitEditBox(xpField, mouseX, mouseY) && !overSuggestions(mouseX, mouseY)) {
			setFocused(null);
			suggestions = List.of();
			suggestionIdx = -1;
			suggestKind = SuggestKind.NONE;
		}
		if (button == 0 && UiTheme.consumeRefreshClick(refreshX, refreshY, mouseX, mouseY, this::resetFields)) {
			return true;
		}
		if (button == 0) {
			UiTheme.ScrollClick sc = UiTheme.clickBar(paletteScrollX(), paletteListTop(), paletteListH(),
				searchScroll, maxSearchScroll, mouseX, mouseY);
			if (sc != null) {
				draggingPaletteScroll = true;
				paletteScrollGrab = sc.grab;
				searchScroll = sc.scroll;
				return true;
			}
			UiTheme.ScrollClick rsc = UiTheme.clickBar(listScrollBarX(), advListTop(), advListH(),
				listScroll, listMaxScroll, mouseX, mouseY);
			if (rsc != null) {
				draggingListScroll = true;
				listScrollGrab = rsc.grab;
				listScroll = rsc.scroll;
				return true;
			}
		}
		if (handleListClick(mouseX, mouseY, button)) return true;

		if (!overSuggestions(mouseX, mouseY)) {
			int paletteIdx = paletteIndexAt(mouseX, mouseY);
			if (paletteIdx >= 0) {
				if (button == 0) beginDrag(paletteItems().get(paletteIdx), SRC_PALETTE, mouseX, mouseY);
				return true;
			}
			if (hitIconSlot(mouseX, mouseY)) {
				if (button == 1) {
					iconId = null;
					return true;
				}
				if (button == 0 && iconId != null) beginDrag(iconId, SRC_ICON, mouseX, mouseY);
				return true;
			}
			if (hitCondSlot(mouseX, mouseY)) {
				if (button == 1) {
					condText = "";
					if (condField != null) condField.setValue("");
					return true;
				}
				ResourceLocation id = SafeIds.tryParseItem(condText);
				if (button == 0 && id != null) beginDrag(id, SRC_COND, mouseX, mouseY);
				return true;
			}
		}
		if (handleFooterClick(mouseX, mouseY, button)) return true;
		boolean result = super.mouseClicked(mouseX, mouseY, button);
		if (button == 0) {
			if (SuggestionPopup.clickOpens(parentField, mouseX, mouseY)) {
				setFocused(parentField);
				parentField.setFocused(true);
				updateSuggestions(SuggestKind.PARENT);
			} else if (SuggestionPopup.clickOpens(backgroundField, mouseX, mouseY)) {
				setFocused(backgroundField);
				backgroundField.setFocused(true);
				updateSuggestions(SuggestKind.BACKGROUND);
			} else if (SuggestionPopup.clickOpens(triggerField, mouseX, mouseY)) {
				setFocused(triggerField);
				triggerField.setFocused(true);
				updateSuggestions(SuggestKind.TRIGGER);
			} else if (SuggestionPopup.clickOpens(condField, mouseX, mouseY)) {
				setFocused(condField);
				condField.setFocused(true);
				updateSuggestions(SuggestKind.COND);
			}
		}
		return result;
	}

	private boolean hitEditBox(EditBox box, double mouseX, double mouseY) {
		return box != null && box.visible && mouseX >= box.getX() && mouseX < box.getX() + box.getWidth()
			&& mouseY >= box.getY() && mouseY < box.getY() + box.getHeight();
	}

	private boolean hitWidget(AbstractWidget w, double mouseX, double mouseY) {
		return w != null && w.visible && mouseX >= w.getX() && mouseX < w.getX() + w.getWidth()
			&& mouseY >= w.getY() && mouseY < w.getY() + w.getHeight();
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		if (namingOpen) {
			return nameField != null && nameField.mouseDragged(mouseX, mouseY, button, dragX, dragY);
		}
		if (SuggestionPopup.isDragging() && button == 0) {
			suggestionScroll = SuggestionPopup.dragTo(mouseY);
			return true;
		}
		if (draggingPaletteScroll && button == 0) {
			searchScroll = UiTheme.scrollAtGrab(paletteListTop(), paletteListH(), maxSearchScroll, mouseY, paletteScrollGrab);
			return true;
		}
		if (draggingListScroll && button == 0) {
			listScroll = UiTheme.scrollAtGrab(advListTop(), advListH(), listMaxScroll, mouseY, listScrollGrab);
			return true;
		}
		if (button == 0 && updateItemDrag(mouseX, mouseY)) return true;
		return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (namingOpen) {
			return nameField != null && nameField.mouseReleased(mouseX, mouseY, button);
		}
		if (button == 0) {
			boolean dropped = finishItemDrag(mouseX, mouseY);
			draggingPaletteScroll = false;
			draggingListScroll = false;
			SuggestionPopup.endDrag();
			if (dropped) return true;
		}
		return super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (namingOpen || invPicker.isOpen()) return true;
		if (suggestOpen() && overSuggestions(mouseX, mouseY) && SuggestionPopup.maxScroll(suggestions.size()) > 0) {
			suggestionScroll = SuggestionPopup.scrollBy(suggestionScroll, suggestions.size(), scrollY);
			return true;
		}
		if (mouseX >= railX && mouseX < railX + railW && mouseY >= topPos && mouseY < topPos + HEIGHT) {
			searchScroll = Mth.clamp(searchScroll - (int) (scrollY * 20), 0, maxSearchScroll);
			return true;
		}
		if (mouseX >= rightRailX && mouseX < rightRailX + rightRailW && mouseY >= topPos && mouseY < topPos + HEIGHT) {
			listScroll = Mth.clamp(listScroll - (int) (scrollY * 20), 0, listMaxScroll);
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (invPicker.isOpen()) {
			if (keyCode == GLFW.GLFW_KEY_ESCAPE) invPicker.close();
			return true;
		}
		if (rowMenu.isOpen() && keyCode == GLFW.GLFW_KEY_ESCAPE) {
			rowMenu.close();
			menuRow = null;
			return true;
		}
		if (namingOpen) {
			if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
				closeNamePopup();
				return true;
			}
			if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER
				|| (hasControlDown() && keyCode == GLFW.GLFW_KEY_S)) {
				confirmNamePopup();
				return true;
			}
			if (keyCode == GLFW.GLFW_KEY_TAB) {
				setFocused(nameField);
				if (nameField != null) nameField.setFocused(true);
				return true;
			}
			return nameField != null && nameField.keyPressed(keyCode, scanCode, modifiers);
		}
		if (hasControlDown() && keyCode == GLFW.GLFW_KEY_S) {
			requestSave();
			return true;
		}
		if (suggestOpen()) {
			if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER || keyCode == GLFW.GLFW_KEY_TAB) {
				pickSuggestion(Math.max(0, suggestionIdx));
				return true;
			}
			if (keyCode == GLFW.GLFW_KEY_DOWN) {
				suggestionIdx = Math.min(suggestionIdx + 1, suggestions.size() - 1);
				suggestionScroll = SuggestionPopup.keepVisible(suggestionIdx, suggestionScroll, suggestions.size());
				return true;
			}
			if (keyCode == GLFW.GLFW_KEY_UP) {
				suggestionIdx = Math.max(0, suggestionIdx - 1);
				suggestionScroll = SuggestionPopup.keepVisible(suggestionIdx, suggestionScroll, suggestions.size());
				return true;
			}
			if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
				suggestions = List.of();
				suggestionIdx = -1;
				suggestKind = SuggestKind.NONE;
				return true;
			}
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public boolean charTyped(char codePoint, int modifiers) {
		if (invPicker.isOpen()) return true;
		if (namingOpen && nameField != null) {
			if (namingError) {
				namingError = false;
				layoutNamePopup();
			}
			return nameField.charTyped(codePoint, modifiers);
		}
		return super.charTyped(codePoint, modifiers);
	}

	private void beginDrag(ResourceLocation item, int source, double x, double y) {
		dragItem = item;
		dragSource = source;
		dragPending = true;
		dragActive = false;
		dragStartX = x;
		dragStartY = y;
	}

	private boolean updateItemDrag(double x, double y) {
		if (!dragPending && !dragActive) return false;
		if (!dragActive) {
			double dx = x - dragStartX;
			double dy = y - dragStartY;
			if (dx * dx + dy * dy < DRAG_START * DRAG_START) return false;
			dragActive = true;
		}
		return true;
	}

	private boolean finishItemDrag(double x, double y) {
		boolean wasActive = dragActive;
		ResourceLocation item = dragItem;
		int source = dragSource;
		dragPending = false;
		dragActive = false;
		dragItem = null;
		dragSource = SRC_NONE;
		if (item == null) return wasActive;
		if (!wasActive) {
			if (source == SRC_PALETTE) {
				iconId = item;
				return true;
			}
			return false;
		}
		if (hitIconSlot(x, y)) {
			iconId = item;
			return true;
		}
		if (hitCondSlot(x, y)) {
			condText = item.toString();
			if (condField != null) condField.setValue(condText);
			return true;
		}
		if (source == SRC_ICON && !hitIconSlot(x, y)) iconId = null;
		if (source == SRC_COND && !hitCondSlot(x, y)) {
			condText = "";
			if (condField != null) condField.setValue("");
		}
		return true;
	}

	private void resetFields() {
		titleText = "";
		descText = "";
		iconId = BuiltInRegistries.ITEM.getKey(Items.DIAMOND);
		frameIdx = 0;
		showToast = true;
		announceChat = true;
		hidden = false;
		parentText = "";
		backgroundText = BACKGROUNDS[0];
		triggerText = TRIGGER_IDS[0];
		condText = "";
		xpText = "";
		searching = false;
		searchResults = List.of();
		searchScroll = 0;
		if (searchField != null) searchField.setValue("");
		if (listSearchField != null) listSearchField.setValue("");
		suggestions = List.of();
		suggestionIdx = -1;
		suggestKind = SuggestKind.NONE;
		closeNamePopup();
		editingId = null;
		applyFieldsToWidgets();
		syncOptionButtons();
		layoutButtons();
		refreshList();
		setFocused(null);
	}

	private Session captureSession() {
		pullWidgetState();
		Session s = new Session();
		s.title = titleText;
		s.desc = descText;
		s.icon = iconId;
		s.frameIdx = frameIdx;
		s.showToast = showToast;
		s.announceChat = announceChat;
		s.hidden = hidden;
		s.parent = parentText;
		s.background = backgroundText;
		s.trigger = triggerText;
		s.cond = condText;
		s.xp = xpText;
		s.search = searchField != null ? searchField.getValue() : "";
		s.searchScroll = searchScroll;
		s.listSearch = listSearchField != null ? listSearchField.getValue() : "";
		s.listScroll = listScroll;
		s.editingId = editingId;
		return s;
	}

	private void applySession(Session s) {
		if (s == null) return;
		titleText = s.title == null ? "" : s.title;
		descText = s.desc == null ? "" : s.desc;
		iconId = s.icon;
		frameIdx = Mth.clamp(s.frameIdx, 0, 2);
		showToast = s.showToast;
		announceChat = s.announceChat;
		hidden = s.hidden;
		parentText = s.parent == null ? "" : s.parent;
		backgroundText = s.background == null ? "" : s.background;
		triggerText = s.trigger == null ? TRIGGER_IDS[0] : s.trigger;
		condText = s.cond == null ? "" : s.cond;
		xpText = s.xp == null ? "" : s.xp;
		if (searchField != null) searchField.setValue(s.search != null ? s.search : "");
		searchScroll = s.searchScroll;
		if (listSearchField != null) listSearchField.setValue(s.listSearch != null ? s.listSearch : "");
		listScroll = s.listScroll;
		editingId = s.editingId;
	}

	@Override
	public void removed() {
		session = captureSession();
		super.removed();
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return !namingOpen && !rowMenu.isOpen();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private boolean isNamingWidget(AbstractWidget w) {
		return w == nameField || w == nameOkBtn || w == nameCancelBtn;
	}

	private void requestSave() {
		Minecraft mc = Minecraft.getInstance();
		if (EditorAccess.isRemoteWorld() && !EditorAccess.canSendDatapack()) {
			EditorAccess.needMod();
			return;
		}
		pullWidgetState();
		if (editingId != null && isOwned(editingId)) {
			writeAdvFile(editingId);
			return;
		}
		openNamePopup(true);
	}

	private boolean isOwned(ResourceLocation id) {
		if (id == null) return false;
		if (EditorAccess.isRemoteWorld()) return remoteOwned.contains(id);
		return ownedIds().contains(id);
	}

	private void openNamePopup(boolean reset) {
		namingOpen = true;
		if (reset) {
			namingError = false;
			namingIsRename = false;
			namingDraft = "";
			if (nameField != null) nameField.setValue("");
		}
		if (nameField != null) {
			nameField.setVisible(true);
			nameField.setFocused(true);
		}
		if (nameOkBtn != null) nameOkBtn.visible = true;
		if (nameCancelBtn != null) nameCancelBtn.visible = true;
		layoutNamePopup();
		setFocused(nameField);
	}

	private void closeNamePopup() {
		namingOpen = false;
		namingError = false;
		namingIsRename = false;
		namingDraft = "";
		if (nameField != null) {
			nameField.setValue("");
			nameField.setVisible(false);
			nameField.setFocused(false);
			nameField.setY(-1000);
		}
		if (nameOkBtn != null) {
			nameOkBtn.visible = false;
			nameOkBtn.setY(-1000);
		}
		if (nameCancelBtn != null) {
			nameCancelBtn.visible = false;
			nameCancelBtn.setY(-1000);
		}
		setFocused(null);
	}

	private void confirmNamePopup() {
		String typed = nameField != null ? nameField.getValue().trim() : "";
		if (!isValidAdvName(typed)) {
			namingError = true;
			if (nameField != null) {
				nameField.setFocused(true);
				nameField.moveCursorToEnd(false);
				nameField.setHighlightPos(0);
			}
			setFocused(nameField);
			layoutNamePopup();
			return;
		}
		String stem = typed.toLowerCase(Locale.ROOT);
		boolean rename = namingIsRename;
		ListedAdv renameRow = menuRow;
		closeNamePopup();
		if (rename) {
			renameOwned(renameRow, stem);
			return;
		}
		writeAdvFile(ResourceLocation.fromNamespaceAndPath("bjmapedit", stem));
	}

	private static boolean isValidAdvName(String raw) {
		if (raw == null || raw.isEmpty() || raw.length() > 64) return false;
		if (raw.startsWith("/") || raw.endsWith("/") || raw.contains("//")) return false;
		for (int i = 0; i < raw.length(); i++) {
			char c = raw.charAt(i);
			if (!((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
				|| c == '_' || c == '-' || c == '/')) {
				return false;
			}
		}
		return ResourceLocation.tryBuild("bjmapedit", raw.toLowerCase(Locale.ROOT)) != null;
	}

	private void layoutNamePopup() {
		if (nameField == null || nameOkBtn == null || nameCancelBtn == null) return;
		int pw = 300;
		int ph = namingError ? 114 : 96;
		int px = (this.width - pw) / 2;
		int py = (this.height - ph) / 2;
		int pad = 12;
		int fieldY = py + UiTheme.HEADER_H + 10;
		nameField.setPosition(px + pad, fieldY);
		nameField.setWidth(pw - pad * 2);
		nameField.setHeight(16);
		int btnY = py + ph - 28;
		int cancelW = Math.max(64, font.width(nameCancelBtn.getMessage()) + 16);
		int okW = Math.max(64, font.width(nameOkBtn.getMessage()) + 22);
		nameCancelBtn.setPosition(px + pad, btnY);
		nameCancelBtn.setWidth(cancelW);
		nameCancelBtn.setHeight(20);
		nameOkBtn.setPosition(px + pw - pad - okW, btnY);
		nameOkBtn.setWidth(okW);
		nameOkBtn.setHeight(20);
	}

	private boolean hitNamePopup(double mouseX, double mouseY) {
		int pw = 300;
		int ph = namingError ? 114 : 96;
		int px = (this.width - pw) / 2;
		int py = (this.height - ph) / 2;
		return mouseX >= px && mouseX < px + pw && mouseY >= py && mouseY < py + ph;
	}

	private void renderNamePopup(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		int pw = 300;
		int ph = namingError ? 114 : 96;
		int px = (this.width - pw) / 2;
		int py = (this.height - ph) / 2;
		UiTheme.pushOverlay(g);
		g.fill(0, 0, this.width, this.height, 0xB0000000);
		UiTheme.drawPanel(g, px, py, pw, ph);
		UiTheme.drawHeader(g, font, Component.translatable("screen.bj_mapedit.adv_name_hint"), px, py, pw);
		if (nameField != null) nameField.render(g, mouseX, mouseY, partialTick);
		if (namingError) {
			g.drawCenteredString(font, Component.translatable("screen.bj_mapedit.recipe_name_retry"),
				px + pw / 2, nameField.getY() + 20, 0xFFF85149);
		}
		if (nameCancelBtn != null) nameCancelBtn.render(g, mouseX, mouseY, partialTick);
		if (nameOkBtn != null) nameOkBtn.render(g, mouseX, mouseY, partialTick);
		UiTheme.popOverlay(g);
	}

	private static String jsonString(String raw) {
		return raw.replace("\\", "\\\\").replace("\"", "\\\"");
	}

	private static void appendTextComponent(StringBuilder sb, String indent, String text) {
		String t = text == null ? "" : text;
		if (looksLikeTranslate(t)) {
			sb.append(indent).append("{ \"translate\": \"").append(jsonString(t)).append("\" }");
		} else {
			sb.append(indent).append("{ \"text\": \"").append(jsonString(t)).append("\" }");
		}
	}

	private static boolean looksLikeTranslate(String text) {
		if (text == null || text.isEmpty() || text.indexOf(' ') >= 0 || text.indexOf('.') < 0) return false;
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			if (!((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '.' || c == '/')) return false;
		}
		return true;
	}

	private String buildAdvJson() {
		pullWidgetState();
		String title = titleText == null ? "" : titleText;
		String desc = descText == null ? "" : descText;
		if (title.isEmpty() && editingId != null) title = editingId.getPath();
		ResourceLocation icon = iconId != null ? iconId : BuiltInRegistries.ITEM.getKey(Items.DIAMOND);
		String parent = resolveParent();
		String trigger = resolveTrigger(triggerText);
		String cond = condText == null ? "" : condText.trim();
		if (condKindOf(trigger) == CondKind.DIM) cond = resolveDimension(cond);
		else if (condKindOf(trigger) == CondKind.ITEM || condKindOf(trigger) == CondKind.BLOCK) {
			ResourceLocation item = SafeIds.tryParseItem(cond);
			if (item != null) cond = item.toString();
		} else if (!cond.isEmpty()) {
			ResourceLocation id = SafeIds.tryParseItem(cond);
			if (id != null) cond = id.toString();
		}
		int xp = parseXp(xpText);
		String bg = resolveBackground(backgroundText);

		StringBuilder sb = new StringBuilder();
		sb.append("{\n");
		if (!parent.isEmpty()) {
			sb.append("  \"parent\": \"").append(jsonString(parent)).append("\",\n");
		}
		sb.append("  \"display\": {\n");
		sb.append("    \"icon\": {\n");
		sb.append("      \"id\": \"").append(icon).append("\"\n");
		sb.append("    },\n");
		sb.append("    \"title\": ");
		appendTextComponent(sb, "", title);
		sb.append(",\n");
		sb.append("    \"description\": ");
		appendTextComponent(sb, "", desc);
		sb.append(",\n");
		sb.append("    \"frame\": \"").append(frameId(frameIdx)).append("\",\n");
		sb.append("    \"show_toast\": ").append(showToast).append(",\n");
		sb.append("    \"announce_to_chat\": ").append(announceChat).append(",\n");
		sb.append("    \"hidden\": ").append(hidden);
		if (parent.isEmpty() && !bg.isEmpty()) {
			sb.append(",\n");
			sb.append("    \"background\": \"").append(jsonString(bg)).append("\"");
		}
		sb.append("\n  },\n");
		sb.append("  \"criteria\": {\n");
		sb.append("    \"done\": {\n");
		sb.append("      \"trigger\": \"").append(jsonString(trigger)).append("\"");
		appendConditions(sb, trigger, cond);
		sb.append("\n    }\n");
		sb.append("  }");
		if (xp > 0) {
			sb.append(",\n");
			sb.append("  \"rewards\": {\n");
			sb.append("    \"experience\": ").append(xp).append("\n");
			sb.append("  }");
		}
		sb.append("\n}\n");
		return sb.toString();
	}

	private static void appendConditions(StringBuilder sb, String trigger, String cond) {
		if (cond == null || cond.isEmpty()) return;
		String t = stripMc(trigger);
		sb.append(",\n      \"conditions\": {\n");
		switch (t) {
			case "inventory_changed" -> {
				sb.append("        \"items\": [\n");
				sb.append("          { \"items\": \"").append(jsonString(cond)).append("\" }\n");
				sb.append("        ]");
			}
			case "consume_item" -> {
				sb.append("        \"item\": {\n");
				sb.append("          \"items\": \"").append(jsonString(cond)).append("\"\n");
				sb.append("        }");
			}
			case "placed_block" -> {
				sb.append("        \"location\": [\n");
				sb.append("          {\n");
				sb.append("            \"condition\": \"minecraft:block_state_property\",\n");
				sb.append("            \"block\": \"").append(jsonString(cond)).append("\"\n");
				sb.append("          }\n");
				sb.append("        ]");
			}
			case "enter_block" -> sb.append("        \"block\": \"").append(jsonString(cond)).append("\"");
			case "player_killed_entity", "bred_animals", "tame_animal" -> {
				sb.append("        \"entity\": [\n");
				sb.append("          {\n");
				sb.append("            \"condition\": \"minecraft:entity_properties\",\n");
				sb.append("            \"entity\": \"this\",\n");
				sb.append("            \"predicate\": {\n");
				sb.append("              \"type\": \"").append(jsonString(cond)).append("\"\n");
				sb.append("            }\n");
				sb.append("          }\n");
				sb.append("        ]");
			}
			case "recipe_unlocked" -> sb.append("        \"recipe\": \"").append(jsonString(cond)).append("\"");
			case "changed_dimension" -> sb.append("        \"to\": \"").append(jsonString(cond)).append("\"");
			case "location" -> {
				sb.append("        \"player\": [\n");
				sb.append("          {\n");
				sb.append("            \"condition\": \"minecraft:entity_properties\",\n");
				sb.append("            \"entity\": \"this\",\n");
				sb.append("            \"predicate\": {\n");
				sb.append("              \"location\": {\n");
				sb.append("                \"biomes\": \"").append(jsonString(cond)).append("\"\n");
				sb.append("              }\n");
				sb.append("            }\n");
				sb.append("          }\n");
				sb.append("        ]");
			}
			default -> {
				sb.setLength(sb.length() - "\n      \"conditions\": {\n".length());
				return;
			}
		}
		sb.append("\n      }");
	}

	private static int parseXp(String raw) {
		if (raw == null) return 0;
		String s = raw.trim();
		if (s.isEmpty()) return 0;
		try {
			return Math.max(0, Integer.parseInt(s));
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private void copyJsonToClipboard() {
		Minecraft mc = Minecraft.getInstance();
		mc.keyboardHandler.setClipboard(buildAdvJson());
		if (mc.player != null) {
			mc.player.displayClientMessage(Component.translatable("screen.bj_mapedit.copied"), true);
		}
	}

	private void writeAdvFile(ResourceLocation advId) {
		Minecraft mc = Minecraft.getInstance();
		if (EditorAccess.isRemoteWorld()) {
			remoteJson.put(advId, buildAdvJson());
			remoteOwned.add(advId);
			editingId = advId;
			pendingSave = true;
			DatapackNet.send(DatapackOps.WRITE, advRel(advId), remoteJson.get(advId));
			return;
		}
		IntegratedServer server = mc.getSingleplayerServer();
		if (server == null) {
			EditorAccess.needMod();
			return;
		}
		Path packRoot = server.getWorldPath(LevelResource.DATAPACK_DIR).resolve("bjmapedit");
		Path outFile = advPath(packRoot, advId);
		try {
			Files.createDirectories(outFile.getParent());
			writePackMcmeta(packRoot);
			Files.writeString(outFile, buildAdvJson(), StandardCharsets.UTF_8);
			editingId = advId;
			flashSaveMessage();
			refreshList();
			server.execute(() -> enablePackAndReload(server, advId, true));
		} catch (IOException e) {
			if (mc.player != null) {
				mc.player.displayClientMessage(Component.translatable("screen.bj_mapedit.recipe_export_fail"), true);
			}
		}
	}

	private static Path advPath(Path packRoot, ResourceLocation id) {
		return packRoot.resolve("data").resolve(id.getNamespace()).resolve("advancement").resolve(id.getPath() + ".json");
	}

	private int rowIndexAt(double mouseY) {
		if (mouseY < advListTop() || mouseY >= advListBottom()) return -1;
		int idx = (int) ((mouseY - advListTop() + listScroll) / ROW_H);
		if (idx < 0 || idx >= rows.size()) return -1;
		return idx;
	}

	private boolean handleRowMenuClick(double mx, double my) {
		RenameDeleteMenu.Action a = rowMenu.pick(mx, my);
		ListedAdv row = menuRow;
		if (a == RenameDeleteMenu.Action.RENAME && row != null && row.owned) {
			menuRow = row;
			startRename(row);
			return true;
		}
		if (a == RenameDeleteMenu.Action.DELETE && row != null && row.owned) {
			menuRow = null;
			deleteOwned(row);
			return true;
		}
		if (a == RenameDeleteMenu.Action.DISMISS) {
			menuRow = null;
			return true;
		}
		return false;
	}

	private boolean inRightRail(double mouseX, double mouseY) {
		return mouseX >= rightRailX && mouseX < rightRailX + rightRailW
			&& mouseY >= topPos && mouseY < topPos + HEIGHT;
	}

	private boolean handleListClick(double mx, double my, int button) {
		if (!inRightRail(mx, my) || hitEditBox(listSearchField, mx, my)) return false;
		if (UiTheme.hitScrollBar(listScrollBarX(), advListTop(), advListH(), mx, my)) return false;
		int idx = rowIndexAt(my);
		if (idx < 0) return my >= advListTop();
		ListedAdv row = rows.get(idx);
		if (button == 1 && row.owned) {
			menuRow = row;
			rowMenu.show(font, (int) mx, (int) my, this.width, this.height);
			return true;
		}
		if (button == 0 || button == 1) {
			loadListed(row);
			return true;
		}
		return true;
	}

	private void startRename(ListedAdv row) {
		namingIsRename = true;
		namingOpen = true;
		namingError = false;
		if (nameField != null) {
			nameField.setVisible(true);
			String path = row.id.getPath();
			int slash = path.lastIndexOf('/');
			nameField.setValue(slash >= 0 ? path.substring(slash + 1) : path);
			nameField.setFocused(true);
		}
		if (nameOkBtn != null) nameOkBtn.visible = true;
		if (nameCancelBtn != null) nameCancelBtn.visible = true;
		layoutNamePopup();
		setFocused(nameField);
	}

	private void deleteOwned(ListedAdv row) {
		if (EditorAccess.isRemoteWorld()) {
			if (row.id.equals(editingId)) editingId = null;
			remoteOwned.remove(row.id);
			remoteJson.remove(row.id);
			DatapackNet.send(DatapackOps.DELETE, advRel(row.id), "");
			refreshList();
			return;
		}
		Path root = packRoot();
		if (root == null) return;
		Path file = existingAdvFile(root, row.id);
		try {
			if (file != null) Files.deleteIfExists(file);
			if (row.id.equals(editingId)) editingId = null;
			refreshList();
			Minecraft mc = Minecraft.getInstance();
			IntegratedServer server = mc.getSingleplayerServer();
			if (server != null) server.execute(() -> enablePackAndReload(server, row.id, false));
			if (mc.player != null) {
				mc.player.displayClientMessage(Component.translatable("screen.bj_mapedit.deleted"), true);
			}
		} catch (IOException e) {
			Minecraft mc = Minecraft.getInstance();
			if (mc.player != null) {
				mc.player.displayClientMessage(Component.translatable("screen.bj_mapedit.recipe_export_fail"), true);
			}
		}
	}

	private void renameOwned(ListedAdv row, String stem) {
		if (row == null) return;
		String path = row.id.getPath();
		int slash = path.lastIndexOf('/');
		String newPath = slash >= 0 ? path.substring(0, slash + 1) + stem : stem;
		ResourceLocation nid = ResourceLocation.tryBuild(row.id.getNamespace(), newPath);
		if (nid == null) return;
		if (EditorAccess.isRemoteWorld()) {
			String json = remoteJson.getOrDefault(row.id, buildAdvJson());
			remoteOwned.remove(row.id);
			remoteJson.remove(row.id);
			remoteOwned.add(nid);
			remoteJson.put(nid, json);
			if (row.id.equals(editingId)) editingId = nid;
			DatapackNet.send(DatapackOps.RENAME, advRel(row.id), advRel(nid));
			refreshList();
			return;
		}
		Path root = packRoot();
		if (root == null) return;
		Path from = existingAdvFile(root, row.id);
		Path to = advPath(root, nid);
		try {
			Files.createDirectories(to.getParent());
			if (from != null && Files.isRegularFile(from)) {
				Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
			} else {
				Files.writeString(to, buildAdvJson(), StandardCharsets.UTF_8);
			}
			if (row.id.equals(editingId)) editingId = nid;
			refreshList();
			Minecraft mc = Minecraft.getInstance();
			IntegratedServer server = mc.getSingleplayerServer();
			if (server != null) server.execute(() -> enablePackAndReload(server, nid, true));
		} catch (IOException e) {
			Minecraft mc = Minecraft.getInstance();
			if (mc.player != null) {
				mc.player.displayClientMessage(Component.translatable("screen.bj_mapedit.recipe_export_fail"), true);
			}
		}
	}

	private void loadListed(ListedAdv row) {
		rowMenu.close();
		menuRow = null;
		boolean loaded = false;
		if (EditorAccess.isRemoteWorld()) {
			String json = remoteJson.get(row.id);
			if (json != null) loaded = applyAdvJsonText(json);
			else if (row.owned) {
				pendingLoadId = row.id;
				DatapackNet.send(DatapackOps.READ, advRel(row.id), "");
			}
		} else {
			loaded = existingAdvFile(packRoot(), row.id) != null && applyAdvJson(row.id);
		}
		if (!loaded) loaded = applyLoadedHolder(row.id);
		if (!loaded && !EditorAccess.isRemoteWorld()) loaded = applyAdvJson(row.id);
		if (loaded) {
			editingId = row.owned ? row.id : null;
			applyFieldsToWidgets();
			syncOptionButtons();
			layoutButtons();
			setFocused(null);
		}
	}

	private boolean applyLoadedHolder(ResourceLocation id) {
		AdvancementHolder holder = advancementHolder(id);
		if (holder == null) return false;
		Advancement adv = holder.value();
		titleText = "";
		descText = "";
		iconId = BuiltInRegistries.ITEM.getKey(Items.DIAMOND);
		frameIdx = 0;
		showToast = true;
		announceChat = true;
		hidden = false;
		backgroundText = BACKGROUNDS[0];
		parentText = adv.parent().map(ResourceLocation::toString).orElse("");
		xpText = adv.rewards().experience() > 0 ? Integer.toString(adv.rewards().experience()) : "";
		condText = "";
		triggerText = TRIGGER_IDS[0];
		adv.display().ifPresent(this::applyDisplay);
		if (!adv.criteria().isEmpty()) {
			Criterion<?> first = adv.criteria().values().iterator().next();
			ResourceLocation tid = BuiltInRegistries.TRIGGER_TYPES.getKey(first.trigger());
			if (tid != null) triggerText = tid.toString();
		}
		return true;
	}

	private void applyDisplay(DisplayInfo display) {
		titleText = display.getTitle().getString();
		descText = display.getDescription().getString();
		ItemStack icon = display.getIcon();
		if (!icon.isEmpty()) iconId = BuiltInRegistries.ITEM.getKey(icon.getItem());
		AdvancementType type = display.getType();
		if (type == AdvancementType.GOAL) frameIdx = 1;
		else if (type == AdvancementType.CHALLENGE) frameIdx = 2;
		else frameIdx = 0;
		showToast = display.shouldShowToast();
		announceChat = display.shouldAnnounceChat();
		hidden = display.isHidden();
		backgroundText = display.getBackground().map(ResourceLocation::toString).orElse(BACKGROUNDS[0]);
	}

	private AdvancementHolder advancementHolder(ResourceLocation id) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.getSingleplayerServer() != null) {
			return mc.getSingleplayerServer().getAdvancements().get(id);
		}
		if (mc.getConnection() == null) return null;
		return mc.getConnection().getAdvancements().get(id);
	}

	private List<ListedAdv> loadedAdvRows() {
		List<ListedAdv> out = new ArrayList<>();
		Minecraft mc = Minecraft.getInstance();
		if (mc.getConnection() == null) return out;
		ClientAdvancements ca = mc.getConnection().getAdvancements();
		for (AdvancementNode node : ca.getTree().nodes()) {
			AdvancementHolder holder = node.holder();
			ItemStack icon = ItemStack.EMPTY;
			String label = holder.id().getPath();
			if (holder.value().display().isPresent()) {
				DisplayInfo d = holder.value().display().get();
				icon = d.getIcon().copy();
				String title = d.getTitle().getString();
				if (!title.isBlank()) label = title;
			}
			out.add(new ListedAdv(holder.id(), icon, label, false));
		}
		return out;
	}

	private void refreshList() {
		LinkedHashMap<ResourceLocation, ListedAdv> map = new LinkedHashMap<>();
		Set<ResourceLocation> owned = ownedIds();
		for (ListedAdv row : loadedAdvRows()) {
			map.put(row.id, new ListedAdv(row.id, row.icon, row.label, owned.contains(row.id)));
		}
		for (ResourceLocation id : owned) {
			if (map.containsKey(id)) continue;
			ListedAdv row = listedFromDisk(id);
			if (row != null) map.put(id, row);
		}
		List<ListedAdv> all = new ArrayList<>(map.values());
		all.sort((a, b) -> {
			boolean aOwn = a.owned || "bjmapedit".equals(a.id.getNamespace());
			boolean bOwn = b.owned || "bjmapedit".equals(b.id.getNamespace());
			if (aOwn != bOwn) return aOwn ? -1 : 1;
			int byLabel = a.label.compareToIgnoreCase(b.label);
			if (byLabel != 0) return byLabel;
			return a.id.toString().compareToIgnoreCase(b.id.toString());
		});
		String q = listSearchField == null ? "" : listSearchField.getValue().trim().toLowerCase(Locale.ROOT);
		if (!q.isEmpty()) {
			all.removeIf(r -> !r.label.toLowerCase(Locale.ROOT).contains(q)
				&& !r.id.toString().toLowerCase(Locale.ROOT).contains(q)
				&& !r.id.getPath().toLowerCase(Locale.ROOT).contains(q));
		}
		rows = all;
		updateListScroll();
	}

	private ListedAdv listedFromDisk(ResourceLocation id) {
		ItemStack icon = ItemStack.EMPTY;
		String label = id.getPath();
		String json = null;
		if (EditorAccess.isRemoteWorld()) {
			json = remoteJson.get(id);
		} else {
			Path file = existingAdvFile(packRoot(), id);
			if (file != null) {
				try {
					json = Files.readString(file, StandardCharsets.UTF_8);
				} catch (Exception ignored) {}
			}
		}
		if (json != null) {
			try {
				JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
				ResourceLocation iconRl = iconFromJson(obj);
				if (iconRl != null) icon = stackOf(iconRl, 1);
				String title = titleFromJson(obj);
				if (!title.isBlank()) label = title;
			} catch (Exception ignored) {}
		}
		return new ListedAdv(id, icon, label, true);
	}

	private boolean applyAdvJson(ResourceLocation id) {
		if (EditorAccess.isRemoteWorld()) {
			String json = remoteJson.get(id);
			return json != null && applyAdvJsonText(json);
		}
		Path file = existingAdvFile(packRoot(), id);
		if (file == null) return false;
		try {
			return applyAdvJsonText(Files.readString(file, StandardCharsets.UTF_8));
		} catch (Exception e) {
			return false;
		}
	}

	private boolean applyAdvJsonText(String text) {
		try {
			JsonObject json = JsonParser.parseString(text).getAsJsonObject();
			parentText = json.has("parent") ? json.get("parent").getAsString() : "";
			titleText = "";
			descText = "";
			iconId = BuiltInRegistries.ITEM.getKey(Items.DIAMOND);
			frameIdx = 0;
			showToast = true;
			announceChat = true;
			hidden = false;
			backgroundText = BACKGROUNDS[0];
			if (json.has("display") && json.get("display").isJsonObject()) {
				JsonObject display = json.getAsJsonObject("display");
				titleText = textFromComponent(display.get("title"));
				descText = textFromComponent(display.get("description"));
				ResourceLocation icon = iconFromDisplay(display);
				if (icon != null) iconId = icon;
				String frame = display.has("frame") ? display.get("frame").getAsString() : "task";
				frameIdx = "goal".equals(frame) ? 1 : "challenge".equals(frame) ? 2 : 0;
				if (display.has("show_toast")) showToast = display.get("show_toast").getAsBoolean();
				if (display.has("announce_to_chat")) announceChat = display.get("announce_to_chat").getAsBoolean();
				if (display.has("hidden")) hidden = display.get("hidden").getAsBoolean();
				if (display.has("background")) backgroundText = display.get("background").getAsString();
			}
			xpText = "";
			if (json.has("rewards") && json.get("rewards").isJsonObject()) {
				JsonObject rewards = json.getAsJsonObject("rewards");
				if (rewards.has("experience")) xpText = Integer.toString(Math.max(0, rewards.get("experience").getAsInt()));
			}
			triggerText = TRIGGER_IDS[0];
			condText = "";
			if (json.has("criteria") && json.get("criteria").isJsonObject()) {
				JsonObject criteria = json.getAsJsonObject("criteria");
				if (!criteria.entrySet().isEmpty()) {
					JsonElement first = criteria.entrySet().iterator().next().getValue();
					if (first.isJsonObject()) applyCriterion(first.getAsJsonObject());
				}
			}
			return true;
		} catch (Exception ignored) {
			return false;
		}
	}

	private void applyCriterion(JsonObject crit) {
		if (crit.has("trigger")) triggerText = crit.get("trigger").getAsString();
		if (!crit.has("conditions") || !crit.get("conditions").isJsonObject()) return;
		JsonObject cond = crit.getAsJsonObject("conditions");
		String found = firstItemId(cond.get("items"));
		if (found == null && cond.has("item")) found = firstItemId(cond.get("item"));
		if (found == null && cond.has("block")) found = jsonId(cond.get("block"));
		if (found == null && cond.has("recipe")) found = jsonId(cond.get("recipe"));
		if (found == null && cond.has("to")) found = jsonId(cond.get("to"));
		if (found == null) found = entityTypeFrom(cond.get("entity"));
		if (found == null) found = biomeFromPlayer(cond.get("player"));
		if (found == null) found = blockFromLocation(cond.get("location"));
		if (found != null) condText = found;
	}

	private static ResourceLocation iconFromJson(JsonObject json) {
		if (!json.has("display") || !json.get("display").isJsonObject()) return null;
		return iconFromDisplay(json.getAsJsonObject("display"));
	}

	private static ResourceLocation iconFromDisplay(JsonObject display) {
		if (!display.has("icon")) return null;
		JsonElement el = display.get("icon");
		if (el.isJsonPrimitive()) return SafeIds.tryParseItem(el.getAsString());
		if (!el.isJsonObject()) return null;
		JsonObject icon = el.getAsJsonObject();
		if (icon.has("id")) return SafeIds.tryParseItem(icon.get("id").getAsString());
		if (icon.has("item")) return SafeIds.tryParseItem(icon.get("item").getAsString());
		return null;
	}

	private static String titleFromJson(JsonObject json) {
		if (!json.has("display") || !json.get("display").isJsonObject()) return "";
		return textFromComponent(json.getAsJsonObject("display").get("title"));
	}

	private static String textFromComponent(JsonElement el) {
		if (el == null) return "";
		if (el.isJsonPrimitive()) return el.getAsString();
		if (!el.isJsonObject()) return "";
		JsonObject obj = el.getAsJsonObject();
		if (obj.has("text")) return obj.get("text").getAsString();
		if (obj.has("translate")) return obj.get("translate").getAsString();
		return "";
	}

	private static String jsonId(JsonElement el) {
		if (el == null || !el.isJsonPrimitive()) return null;
		return el.getAsString();
	}

	private static String firstItemId(JsonElement el) {
		if (el == null) return null;
		if (el.isJsonPrimitive()) return el.getAsString();
		if (el.isJsonArray()) {
			JsonArray arr = el.getAsJsonArray();
			return arr.isEmpty() ? null : firstItemId(arr.get(0));
		}
		if (!el.isJsonObject()) return null;
		JsonObject obj = el.getAsJsonObject();
		if (obj.has("items")) return firstItemId(obj.get("items"));
		if (obj.has("item")) return firstItemId(obj.get("item"));
		if (obj.has("id")) return jsonId(obj.get("id"));
		return null;
	}

	private static String entityTypeFrom(JsonElement el) {
		if (el == null) return null;
		if (el.isJsonArray()) {
			JsonArray arr = el.getAsJsonArray();
			return arr.isEmpty() ? null : entityTypeFrom(arr.get(0));
		}
		if (!el.isJsonObject()) return null;
		JsonObject obj = el.getAsJsonObject();
		if (obj.has("predicate") && obj.get("predicate").isJsonObject()) {
			JsonObject pred = obj.getAsJsonObject("predicate");
			if (pred.has("type")) return jsonId(pred.get("type"));
		}
		if (obj.has("type")) return jsonId(obj.get("type"));
		return null;
	}

	private static String biomeFromPlayer(JsonElement el) {
		if (el == null) return null;
		if (el.isJsonArray()) {
			JsonArray arr = el.getAsJsonArray();
			return arr.isEmpty() ? null : biomeFromPlayer(arr.get(0));
		}
		if (!el.isJsonObject()) return null;
		JsonObject obj = el.getAsJsonObject();
		JsonObject pred = obj;
		if (obj.has("predicate") && obj.get("predicate").isJsonObject()) pred = obj.getAsJsonObject("predicate");
		if (!pred.has("location") || !pred.get("location").isJsonObject()) return null;
		JsonObject loc = pred.getAsJsonObject("location");
		if (loc.has("biomes")) return firstItemId(loc.get("biomes"));
		if (loc.has("biome")) return jsonId(loc.get("biome"));
		return null;
	}

	private static String blockFromLocation(JsonElement el) {
		if (el == null) return null;
		if (el.isJsonArray()) {
			JsonArray arr = el.getAsJsonArray();
			return arr.isEmpty() ? null : blockFromLocation(arr.get(0));
		}
		if (!el.isJsonObject()) return null;
		JsonObject obj = el.getAsJsonObject();
		if (obj.has("block")) return jsonId(obj.get("block"));
		if (obj.has("predicate") && obj.get("predicate").isJsonObject()) {
			JsonObject pred = obj.getAsJsonObject("predicate");
			if (pred.has("block") && pred.get("block").isJsonObject()) {
				JsonObject block = pred.getAsJsonObject("block");
				if (block.has("blocks")) return firstItemId(block.get("blocks"));
			}
		}
		return null;
	}

	private Path packRoot() {
		if (EditorAccess.isRemoteWorld()) return DatapackNet.virtualRoot().resolve("bjmapedit");
		IntegratedServer server = Minecraft.getInstance().getSingleplayerServer();
		if (server == null) return null;
		return server.getWorldPath(LevelResource.DATAPACK_DIR).resolve("bjmapedit");
	}

	private Path existingAdvFile(Path packRoot, ResourceLocation id) {
		if (packRoot == null || id == null) return null;
		Path a = advPath(packRoot, id);
		Path b = packRoot.resolve("data").resolve(id.getNamespace()).resolve("advancements").resolve(id.getPath() + ".json");
		if (Files.isRegularFile(a)) return a;
		if (Files.isRegularFile(b)) return b;
		return null;
	}

	private Set<ResourceLocation> ownedIds() {
		if (EditorAccess.isRemoteWorld()) return new LinkedHashSet<>(remoteOwned);
		Set<ResourceLocation> out = new LinkedHashSet<>();
		Path root = packRoot();
		if (root == null) return out;
		Path data = root.resolve("data");
		if (!Files.isDirectory(data)) return out;
		try (var nsDirs = Files.newDirectoryStream(data)) {
			for (Path ns : nsDirs) {
				if (!Files.isDirectory(ns)) continue;
				String namespace = ns.getFileName().toString();
				for (String folder : new String[]{"advancement", "advancements"}) {
					Path dir = ns.resolve(folder);
					if (!Files.isDirectory(dir)) continue;
					try (var walk = Files.walk(dir)) {
						walk.filter(p -> Files.isRegularFile(p) && p.getFileName().toString().endsWith(".json")).forEach(p -> {
							String rel = dir.relativize(p).toString().replace('\\', '/');
							if (rel.endsWith(".json")) rel = rel.substring(0, rel.length() - 5);
							ResourceLocation id = ResourceLocation.tryBuild(namespace, rel);
							if (id != null) out.add(id);
						});
					}
				}
			}
		} catch (IOException ignored) {}
		return out;
	}

	public void handleNet(DatapackOpResultPayload payload) {
		if (payload == null) return;
		if (payload.kind() == DatapackOps.KIND_ERR) {
			Minecraft mc = Minecraft.getInstance();
			if (mc.player != null && payload.extra() != null && !payload.extra().isEmpty()) {
				mc.player.displayClientMessage(Component.translatable(payload.extra()), true);
			}
			return;
		}
		if (payload.kind() == DatapackOps.KIND_TREE) {
			ingestAdvTree(payload.extra());
			refreshList();
			return;
		}
		if (payload.kind() == DatapackOps.KIND_FILE) {
			ResourceLocation id = idFromAdvRel(payload.path());
			if (id != null) {
				remoteJson.put(id, payload.extra() == null ? "" : payload.extra());
				remoteOwned.add(id);
				if (id.equals(pendingLoadId)) {
					pendingLoadId = null;
					if (applyAdvJsonText(remoteJson.get(id))) {
						editingId = id;
						applyFieldsToWidgets();
						syncOptionButtons();
						layoutButtons();
					}
				}
				refreshList();
			}
			return;
		}
		if (payload.kind() == DatapackOps.KIND_OK) {
			if (pendingSave) {
				pendingSave = false;
				flashSaveMessage();
				DatapackNet.send(DatapackOps.RELOAD, "bjmapedit", "");
			} else if ("screen.bj_mapedit.deleted".equals(payload.extra())) {
				Minecraft mc = Minecraft.getInstance();
				if (mc.player != null) {
					mc.player.displayClientMessage(Component.translatable(payload.extra()), true);
				}
			}
			refreshList();
		}
	}

	private String advRel(ResourceLocation id) {
		return "bjmapedit/data/" + id.getNamespace() + "/advancement/" + id.getPath() + ".json";
	}

	private void ingestAdvTree(String listing) {
		remoteOwned.clear();
		if (listing == null || listing.isEmpty()) return;
		for (String line : listing.split("\n")) {
			if (!line.startsWith("F\t")) continue;
			ResourceLocation id = idFromAdvRel(line.substring(2));
			if (id != null) remoteOwned.add(id);
		}
	}

	private static ResourceLocation idFromAdvRel(String rel) {
		if (rel == null) return null;
		String path = rel.replace('\\', '/');
		String prefix = "bjmapedit/data/";
		if (!path.startsWith(prefix) || !path.endsWith(".json")) return null;
		String rest = path.substring(prefix.length(), path.length() - 5);
		int slash = rest.indexOf('/');
		if (slash < 0) return null;
		String ns = rest.substring(0, slash);
		String after = rest.substring(slash + 1);
		if (after.startsWith("advancement/")) after = after.substring("advancement/".length());
		else if (after.startsWith("advancements/")) after = after.substring("advancements/".length());
		else return null;
		return ResourceLocation.tryBuild(ns, after);
	}

	private static void writePackMcmeta(Path packRoot) throws IOException {
		Files.writeString(packRoot.resolve("pack.mcmeta"),
			"{\n  \"pack\": {\n    \"pack_format\": 48,\n    \"description\": \"BJ-MapEdit\"\n  }\n}\n",
			StandardCharsets.UTF_8);
	}

	private static void enablePackAndReload(IntegratedServer server, ResourceLocation advId, boolean expectLoaded) {
		var repo = server.getPackRepository();
		repo.reload();
		Collection<String> selected = new ArrayList<>(repo.getSelectedIds());
		if (!selected.contains("file/bjmapedit")) selected.add("file/bjmapedit");
		repo.setSelected(selected);
		server.reloadResources(repo.getSelectedIds()).thenRunAsync(() -> {
			boolean loaded = server.getAdvancements().get(advId) != null;
			Minecraft.getInstance().execute(() -> {
				if (expectLoaded && !loaded) {
					var player = Minecraft.getInstance().player;
					if (player != null) {
						player.displayClientMessage(Component.translatable("screen.bj_mapedit.adv_not_loaded"), true);
					}
				}
				if (Minecraft.getInstance().screen instanceof AdvancementEditorScreen screen) {
					screen.refreshList();
				}
			});
		}, server);
	}
}
