package cn.erindax.bjmapedit.client.screen;

import cn.erindax.bjmapedit.client.DatapackNet;
import cn.erindax.bjmapedit.client.EditorAccess;
import cn.erindax.bjmapedit.client.widget.RenameDeleteMenu;
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
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.LevelResource;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Environment(EnvType.CLIENT)
public class RecipeEditorScreen extends Screen {

	private static final int FOOTER_H = 30;
	private static final int RECIPE_ROW_H = 22;
	private static final int PALETTE_CELL = 20;
	private static final int SELECTED_FILL = 0xFF21262D;
	private static final int HOVER_FILL = 0xFF30363D;
	private static final int DRAG_START = 4;
	private static final int SRC_NONE = -3;
	private static final int SRC_PALETTE = -1;
	private static final int SRC_RESULT = -2;
	private static final int SRC_INV = 100;
	private static final int TABLE_W = 176;
	private static final int TABLE_H = 166;
	private static final ResourceLocation CRAFTING_TABLE =
		ResourceLocation.withDefaultNamespace("textures/gui/container/crafting_table.png");

	private static final char[] GRID_KEYS = {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I'};
	private static final String[] RECIPE_GROUPS = {
		"wooden_stairs",
		"wooden_slab",
		"wooden_door",
		"wooden_trapdoor",
		"wooden_fence",
		"wooden_fence_gate",
		"wooden_button",
		"wooden_pressure_plate",
		"wooden_sign",
		"hanging_sign",
		"planks",
		"bark",
		"boat",
		"chest_boat",
		"bed",
		"wool",
		"carpet",
		"banner",
		"stained_glass",
		"stained_glass_pane",
		"stained_terracotta",
		"concrete_powder",
		"candle",
		"shulker_box"
	};

	private List<GroupSuggestion> groupSuggestions = List.of();
	private int groupSuggestionIdx = -1;
	private int groupSuggestionScroll;
	private static final String[] CATEGORIES = {"building", "redstone", "equipment", "misc"};
	private static final String[] CATEGORY_KEYS = {
		"screen.bj_mapedit.recipe_cat_building",
		"screen.bj_mapedit.recipe_cat_redstone",
		"screen.bj_mapedit.recipe_cat_equipment",
		"screen.bj_mapedit.recipe_cat_misc"
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
	private int resultX, resultY;
	private int plusX, plusY, minusX, minusY;
	private int countNumX, countNumW;
	private int countBoxY, countBoxH;
	private int tableX, tableY, tableScale = 2;

	private EditBox searchField;
	private EditBox recipeSearchField;
	private EditBox groupField;
	private EditBox nameField;
	private Button shapedBtn;
	private Button shapelessBtn;
	private Button categoryBtn;
	private Button copyBtn;
	private Button saveBtn;
	private long saveHintUntil;
	private Button nameOkBtn;
	private Button nameCancelBtn;
	private boolean namingOpen;
	private boolean namingError;
	private boolean namingIsRename;
	private String namingDraft = "";

	private List<ResourceLocation> searchResults = new ArrayList<>();
	private boolean searching;
	private int searchScroll;
	private int maxSearchScroll;
	private boolean draggingPaletteScroll;
	private double paletteScrollGrab;

	private List<ListedRecipe> recipeRows = List.of();
	private int recipeScroll;
	private int recipeMaxScroll;
	private boolean draggingRecipeScroll;
	private double recipeScrollGrab;
	private ResourceLocation editingId;
	private final RenameDeleteMenu recipeMenu = new RenameDeleteMenu();
	private ListedRecipe recipeMenuRow;
	private final Set<ResourceLocation> remoteOwned = new LinkedHashSet<>();
	private final LinkedHashMap<ResourceLocation, String> remoteJson = new LinkedHashMap<>();
	private ResourceLocation pendingLoadId;
	private boolean pendingRecipeSave;

	private ResourceLocation[] gridItems = new ResourceLocation[9];
	private ResourceLocation resultItem;
	private int resultCount = 1;
	private boolean shaped = true;
	private int categoryIdx = 3;

	private ResourceLocation dragItem;
	private int dragSource = SRC_NONE;
	private boolean dragPending;
	private boolean dragActive;
	private double dragStartX, dragStartY;

	private boolean opened;
	private static Session session;

	private static final class Session {
		ResourceLocation[] grid = new ResourceLocation[9];
		ResourceLocation result;
		int resultCount = 1;
		boolean shaped = true;
		int categoryIdx = 3;
		String group = "";
		String search = "";
		int searchScroll;
		String recipeSearch = "";
		int recipeScroll;
		ResourceLocation editingId;
	}

	private static final class ListedRecipe {
		final ResourceLocation id;
		final ItemStack result;
		final String label;
		boolean owned;

		ListedRecipe(ResourceLocation id, ItemStack result, String label, boolean owned) {
			this.id = id;
			this.result = result == null ? ItemStack.EMPTY : result;
			this.label = label == null || label.isBlank() ? id.getPath() : label;
			this.owned = owned;
		}
	}

	public RecipeEditorScreen() {
		super(Component.translatable("screen.bj_mapedit.recipe_title"));
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

		recipeSearchField = new EditBox(font, 0, 0, 80, 16, Component.empty());
		recipeSearchField.setResponder(s -> refreshRecipeList());
		this.addRenderableWidget(recipeSearchField);

		shapedBtn = this.addRenderableWidget(Button.builder(
			Component.translatable("screen.bj_mapedit.recipe_shaped"), b -> setShaped(true)
		).bounds(0, 0, 80, 18).build());
		shapelessBtn = this.addRenderableWidget(Button.builder(
			Component.translatable("screen.bj_mapedit.recipe_shapeless"), b -> setShaped(false)
		).bounds(0, 0, 80, 18).build());

		categoryBtn = this.addRenderableWidget(Button.builder(categoryLabel(), b -> {
			categoryIdx = (categoryIdx + 1) % CATEGORIES.length;
			b.setMessage(categoryLabel());
			layoutButtons();
		}).bounds(0, 0, 96, 18).build());

		groupField = new EditBox(font, 0, 0, 80, 16, Component.empty());
		groupField.setMaxLength(64);
		groupField.setResponder(s -> updateGroupSuggestions());
		this.addRenderableWidget(groupField);

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
		else syncOptionButtons();

		layoutButtons();
		updateSearchResults();
		refreshRecipeList();
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

	private void setShaped(boolean value) {
		if (shaped == value) return;
		shaped = value;
		syncOptionButtons();
		layoutButtons();
	}

	private Component categoryLabel() {
		return Component.translatable("screen.bj_mapedit.recipe_category_fmt",
			Component.translatable(CATEGORY_KEYS[categoryIdx]));
	}

	private void syncOptionButtons() {
		if (shapedBtn != null) shapedBtn.active = !shaped;
		if (shapelessBtn != null) shapelessBtn.active = shaped;
		if (categoryBtn != null) categoryBtn.setMessage(categoryLabel());
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
		if (saveBtn == null || shapedBtn == null || shapelessBtn == null) return;
		int pad = 12;
		int optY = topPos + CONTENT_TOP + 2;
		int x = leftPos + pad;
		int btnH = 18;
		int shapedW = Math.max(52, font.width(shapedBtn.getMessage()) + 16);
		int shapelessW = Math.max(52, font.width(shapelessBtn.getMessage()) + 16);
		int catW = Math.max(88, font.width(categoryBtn.getMessage()) + 16);
		shapedBtn.setPosition(x, optY);
		shapedBtn.setWidth(shapedW);
		shapedBtn.setHeight(btnH);
		x += shapedW + 4;
		shapelessBtn.setPosition(x, optY);
		shapelessBtn.setWidth(shapelessW);
		shapelessBtn.setHeight(btnH);
		x += shapelessW + 6;
		categoryBtn.setPosition(x, optY);
		categoryBtn.setWidth(catW);
		categoryBtn.setHeight(btnH);
		x += catW + 8;

		int groupW = Math.max(64, leftPos + WIDTH - pad - x);
		if (x + 64 > leftPos + WIDTH - pad) {
			optY += 22;
			x = leftPos + pad;
			groupW = Math.max(64, WIDTH - pad * 2);
		}
		groupField.setPosition(x, optY + 1);
		groupField.setWidth(groupW);
		groupField.setHeight(16);

		int availLeft = leftPos + pad;
		int availRight = leftPos + WIDTH - pad;
		int availTop = optY + 22;
		int availBottom = topPos + HEIGHT - FOOTER_H - 8;
		int availW = Math.max(TABLE_W, availRight - availLeft);
		int availH = Math.max(TABLE_H, availBottom - availTop);
		countBoxH = 14;
		countNumW = font.width("99");
		int stepW = 14;
		int countW = stepW + 6 + countNumW + 6 + stepW;
		int gap = 10;
		tableScale = 2;
		int tw = TABLE_W * tableScale;
		int th = TABLE_H * tableScale;
		if (tw > availW || th > availH) tableScale = 1;
		tw = TABLE_W * tableScale;
		th = TABLE_H * tableScale;
		boolean countRight = availW >= tw + gap + countW;
		int clusterW = countRight ? tw + gap + countW : tw;
		int clusterH = countRight ? th : th + 8 + countBoxH;
		tableX = availLeft + Math.max(0, (availW - clusterW) / 2);
		tableY = availTop + Math.max(0, (availH - clusterH) / 2);
		resultX = tableX + 124 * tableScale;
		resultY = tableY + 35 * tableScale;
		int resultSize = 16 * tableScale;
		if (countRight) {
			minusX = tableX + tw + gap;
			countBoxY = resultY + (resultSize - countBoxH) / 2;
		} else {
			minusX = tableX + (tw - countW) / 2;
			countBoxY = tableY + th + 8;
		}
		minusY = countBoxY;
		countNumX = minusX + stepW + 6;
		plusX = countNumX + countNumW + 6;
		plusY = countBoxY;

		searchField.setPosition(railX + 8, topPos + UiTheme.HEADER_H + 6);
		searchField.setWidth(Math.max(24, railW - 16));
		searchField.setHeight(16);
		if (recipeSearchField != null) {
			recipeSearchField.setPosition(rightRailX + 8, topPos + UiTheme.HEADER_H + 6);
			recipeSearchField.setWidth(Math.max(24, rightRailW - 16));
			recipeSearchField.setHeight(16);
		}

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
		updateRecipeScroll();
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

	private int recipeListTop() {
		return paletteListTop();
	}

	private int recipeListBottom() {
		return paletteListBottom();
	}

	private int recipeListH() {
		return paletteListH();
	}

	private int recipeScrollBarX() {
		return rightRailX + rightRailW - 7;
	}

	private void updateRecipeScroll() {
		recipeMaxScroll = Math.max(0, recipeRows.size() * RECIPE_ROW_H - recipeListH());
		recipeScroll = Mth.clamp(recipeScroll, 0, recipeMaxScroll);
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
		int rows = (paletteItems().size() + cols - 1) / cols;
		maxSearchScroll = Math.max(0, rows * PALETTE_CELL - paletteListH());
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

	private record GroupSuggestion(String id, String label) {}

	private void updateGroupSuggestions() {
		if (groupField == null || !groupField.isFocused() || namingOpen) {
			groupSuggestions = List.of();
			groupSuggestionIdx = -1;
			return;
		}
		String input = groupField.getValue().trim();
		List<GroupSuggestion> all = knownRecipeGroups();
		if (!input.isEmpty() && completeGroup(all, input)) {
			groupSuggestions = List.of();
			groupSuggestionIdx = -1;
			return;
		}
		String lower = input.toLowerCase(Locale.ROOT);
		groupSuggestions = all.stream()
			.filter(s -> input.isEmpty()
				|| s.id.toLowerCase(Locale.ROOT).contains(lower)
				|| s.label.toLowerCase(Locale.ROOT).contains(lower))
			.limit(80)
			.toList();
		groupSuggestionIdx = groupSuggestions.isEmpty() ? -1 : 0;
		groupSuggestionScroll = SuggestionPopup.clampScroll(groupSuggestionScroll, groupSuggestions.size());
	}

	private static boolean completeGroup(List<GroupSuggestion> all, String input) {
		for (GroupSuggestion s : all) {
			if (s.label().equals(input) || s.id().equalsIgnoreCase(input)) return true;
		}
		return false;
	}

	private List<GroupSuggestion> knownRecipeGroups() {
		LinkedHashMap<String, String> map = new LinkedHashMap<>();
		for (String id : RECIPE_GROUPS) {
			String name = groupDisplayName(id);
			if (name != null) map.put(id, name);
		}
		Minecraft mc = Minecraft.getInstance();
		var server = mc.getSingleplayerServer();
		var rm = server != null ? server.getRecipeManager()
			: (mc.level != null ? mc.level.getRecipeManager() : null);
		if (rm != null) {
			for (RecipeHolder<?> holder : rm.getAllRecipesFor(RecipeType.CRAFTING)) {
				String g = holder.value().getGroup();
				if (g == null || g.isEmpty() || map.containsKey(g)) continue;
				String name = groupDisplayName(g);
				if (name != null) map.put(g, name);
			}
		}
		List<GroupSuggestion> list = new ArrayList<>();
		for (var e : map.entrySet()) {
			list.add(new GroupSuggestion(e.getKey(), e.getValue()));
		}
		return list;
	}

	private String groupDisplayName(String id) {
		if (id == null || id.isEmpty()) return null;
		String key = "screen.bj_mapedit.recipe_grp." + id;
		String translated = Component.translatable(key).getString();
		if (!translated.equals(key)) return translated;
		ResourceLocation rl = ResourceLocation.tryParse(id.indexOf(':') >= 0 ? id : "minecraft:" + id);
		if (rl != null && BuiltInRegistries.ITEM.containsKey(rl)) {
			Item item = BuiltInRegistries.ITEM.get(rl);
			if (item != Items.AIR) {
				String name = item.getName(new ItemStack(item)).getString();
				if (name != null && !name.isEmpty()
					&& !name.equals(rl.toString()) && !name.equals(rl.getPath())) {
					return name;
				}
			}
		}
		return null;
	}

	private String groupFieldText(String stored) {
		if (stored == null || stored.isEmpty()) return "";
		for (GroupSuggestion s : knownRecipeGroups()) {
			if (s.id().equalsIgnoreCase(stored) || s.label().equals(stored)) return s.label();
		}
		return stored;
	}

	private String recipeGroupId() {
		String text = groupField == null ? "" : groupField.getValue().trim();
		if (text.isEmpty()) return "";
		for (GroupSuggestion s : knownRecipeGroups()) {
			if (s.label().equals(text) || s.id().equalsIgnoreCase(text)) return s.id();
		}
		if (text.matches("[a-z0-9_./-]+")) return text;
		return "";
	}

	private boolean overGroupSuggestions(double mx, double my) {
		if (groupSuggestions.isEmpty() || groupField == null) return false;
		int sx = groupField.getX();
		int sy = groupListY();
		return SuggestionPopup.contains(mx, my, sx, sy, groupField.getWidth(), groupSuggestions.size());
	}

	private boolean pressGroupSuggestionBar(double mx, double my) {
		if (groupSuggestions.isEmpty() || groupField == null) return false;
		int sx = groupField.getX();
		int sy = groupListY();
		Integer next = SuggestionPopup.pressBar(sx, sy, groupField.getWidth(), groupSuggestions.size(),
			groupSuggestionScroll, mx, my);
		if (next == null) return false;
		groupSuggestionScroll = next;
		return true;
	}

	private boolean clickGroupSuggestion(double mx, double my, int button) {
		if (button != 0 || groupSuggestions.isEmpty() || groupField == null || !groupField.isFocused()) return false;
		int sx = groupField.getX();
		int sy = groupListY();
		int idx = SuggestionPopup.hitIndex(mx, my, sx, sy, groupField.getWidth(), groupSuggestions.size(), groupSuggestionScroll);
		if (idx < 0) return overGroupSuggestions(mx, my);
		pickGroupSuggestion(idx);
		return true;
	}

	private void pickGroupSuggestion(int idx) {
		if (idx < 0 || idx >= groupSuggestions.size() || groupField == null) return;
		String label = groupSuggestions.get(idx).label();
		groupSuggestions = List.of();
		groupSuggestionIdx = -1;
		groupField.setValue(label);
		groupSuggestions = List.of();
		groupSuggestionIdx = -1;
		setFocused(groupField);
		groupField.setFocused(true);
		groupField.moveCursorToEnd(false);
	}

	private boolean groupListOpen() {
		return !namingOpen && !groupSuggestions.isEmpty() && groupField != null && groupField.isFocused();
	}

	private boolean coversGroupList(AbstractWidget w) {
		if (!groupListOpen() || w == groupField) return false;
		int sx = groupField.getX();
		int sy = groupListY();
		int sw = groupField.getWidth();
		int sh = SuggestionPopup.boxH(groupSuggestions.size());
		return w.getX() < sx + sw && w.getX() + w.getWidth() > sx
			&& w.getY() < sy + sh && w.getY() + w.getHeight() > sy;
	}

	private int groupListY() {
		if (groupField == null) return 0;
		return SuggestionPopup.listY(groupField, groupSuggestions.size(), topPos + HEIGHT - FOOTER_H);
	}

	private boolean coveredByGroupList(int lx, int ly, int size) {
		if (!groupListOpen()) return false;
		int x = tableX + lx * tableScale;
		int y = tableY + ly * tableScale;
		int s = size * tableScale;
		int sx = groupField.getX();
		int sy = groupListY();
		int sw = groupField.getWidth();
		int sh = SuggestionPopup.boxH(groupSuggestions.size());
		return x < sx + sw && x + s > sx && y < sy + sh && y + s > sy;
	}

	private void drawCraftItem(GuiGraphics g, ItemStack stack, int lx, int ly) {
		if (stack.isEmpty() || coveredByGroupList(lx, ly, 16)) return;
		g.renderItem(stack, lx, ly);
		g.renderItemDecorations(font, stack, lx, ly);
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
		UiTheme.drawHeader(g, font, Component.translatable("screen.bj_mapedit.recipe_existing"), rightRailX, topPos, rightRailW);
		boolean refreshHover = UiTheme.hitIcon(refreshX, refreshY, mouseX, mouseY);
		UiTheme.drawRefreshIcon(g, refreshX, refreshY, refreshHover);

		renderPalette(g, mouseX, mouseY);
		renderCrafting(g, mouseX, mouseY);
		renderCountSteppers(g, mouseX, mouseY);
		renderRecipeList(g, mouseX, mouseY);

		int fy = topPos + HEIGHT - FOOTER_H;
		g.fill(leftPos + 1, fy, leftPos + WIDTH - 1, topPos + HEIGHT - 1, 0xFF191A1B);
		g.fill(leftPos + 1, fy, leftPos + WIDTH - 1, fy + 1, 0xFF2B2C2D);

		for (var child : this.children()) {
			if (child instanceof AbstractWidget w && !isNamingWidget(w) && !coversGroupList(w)) {
				w.render(g, mouseX, mouseY, partialTick);
			}
		}

		if (groupListOpen()) {
			UiTheme.pushOverlay(g);
			int sx = groupField.getX();
			int sy = groupListY();
			SuggestionPopup.draw(g, font, sx, sy, groupField.getWidth(), groupSuggestions.size(),
				groupSuggestionScroll, groupSuggestionIdx, mouseX, mouseY,
				i -> groupSuggestions.get(i).label());
			UiTheme.popOverlay(g);
		}

		if (namingOpen) {
			renderNamePopup(g, mouseX, mouseY, partialTick);
		} else {
			recipeMenu.draw(g, font, mouseX, mouseY);
			if (dragActive && dragItem != null) {
				drawDragGhost(g, mouseX, mouseY);
			} else if (!recipeMenu.isOpen()) {
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

	private void renderRecipeList(GuiGraphics g, int mouseX, int mouseY) {
		int listTop = recipeListTop();
		int listBottom = recipeListBottom();
		int hx = mouseX;
		int hy = mouseY;
		if (recipeMenu.covers(mouseX, mouseY)) {
			hx = -10000;
			hy = -10000;
		}
		g.enableScissor(rightRailX + 1, listTop, rightRailX + rightRailW - 1, listBottom);
		if (recipeRows.isEmpty()) {
			UiTheme.muted(g, font, Component.translatable("screen.bj_mapedit.recipe_empty").getString(),
				rightRailX + 8, listTop + 4);
		} else {
			int nameRight = rightRailX + rightRailW - 8 - (recipeMaxScroll > 0 ? 6 : 0);
			for (int i = 0; i < recipeRows.size(); i++) {
				int rowY = listTop + i * RECIPE_ROW_H - recipeScroll;
				if (rowY + RECIPE_ROW_H < listTop || rowY > listBottom) continue;
				ListedRecipe row = recipeRows.get(i);
				boolean sel = editingId != null && editingId.equals(row.id);
				boolean hover = hx >= rightRailX + 1 && hx < rightRailX + rightRailW - 1
					&& hy >= rowY && hy < rowY + RECIPE_ROW_H
					&& hy >= listTop && hy < listBottom;
				if (sel) {
					g.fill(rightRailX + 1, rowY, rightRailX + rightRailW - 1, rowY + RECIPE_ROW_H, 0xFF21262D);
				} else if (hover) {
					g.fill(rightRailX + 1, rowY, rightRailX + rightRailW - 1, rowY + RECIPE_ROW_H, 0xFF191A1B);
				}
				int iconX = rightRailX + 6;
				int iconY = rowY + 3;
				if (!row.result.isEmpty()) {
					g.renderItem(row.result, iconX, iconY);
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
		UiTheme.drawThinScrollBar(g, recipeScrollBarX(), listTop, recipeListH(), recipeScroll, recipeMaxScroll);
	}

	private void renderCrafting(GuiGraphics g, int mouseX, int mouseY) {
		g.pose().pushPose();
		g.pose().translate(tableX, tableY, 0);
		if (tableScale != 1) {
			g.pose().scale(tableScale, tableScale, 1.0F);
		}
		g.blit(CRAFTING_TABLE, 0, 0, 0, 0, TABLE_W, TABLE_H);
		g.drawString(font, Component.translatable("container.crafting"), 28, 6, 0x404040, false);
		g.drawString(font, Component.translatable("container.inventory"), 8, 72, 0x404040, false);

		int drop = dragActive ? slotAt(mouseX, mouseY) : SRC_NONE;
		for (int i = 0; i < 9; i++) {
			int sx = 30 + (i % 3) * 18;
			int sy = 17 + (i / 3) * 18;
			if (drop == i || (!dragActive && localHit(sx, sy, 16, mouseX, mouseY))) {
				g.fill(sx, sy, sx + 16, sy + 16, 0x80FFFFFF);
			}
			if (gridItems[i] != null && !(dragActive && dragSource == i)) {
				drawCraftItem(g, stackOf(gridItems[i], 1), sx, sy);
			}
		}

		if (drop == SRC_RESULT || (!dragActive && localHit(124, 35, 16, mouseX, mouseY))) {
			g.fill(124, 35, 140, 51, 0x80FFFFFF);
		}
		if (resultItem != null && !(dragActive && dragSource == SRC_RESULT)) {
			drawCraftItem(g, stackOf(resultItem, resultCount), 124, 35);
		}

		var player = Minecraft.getInstance().player;
		if (player != null) {
			var inv = player.getInventory();
			for (int i = 0; i < 27; i++) {
				int sx = 8 + (i % 9) * 18;
				int sy = 84 + (i / 9) * 18;
				if (!dragActive && localHit(sx, sy, 16, mouseX, mouseY)) {
					g.fill(sx, sy, sx + 16, sy + 16, 0x80FFFFFF);
				}
				drawCraftItem(g, inv.getItem(i + 9), sx, sy);
			}
			for (int i = 0; i < 9; i++) {
				int sx = 8 + i * 18;
				int sy = 142;
				if (!dragActive && localHit(sx, sy, 16, mouseX, mouseY)) {
					g.fill(sx, sy, sx + 16, sy + 16, 0x80FFFFFF);
				}
				drawCraftItem(g, inv.getItem(i), sx, sy);
			}
		}
		g.pose().popPose();
	}

	private void renderCountSteppers(GuiGraphics g, int mouseX, int mouseY) {
		int stepW = 14;
		boolean plusHot = hitRect(plusX, plusY, stepW, countBoxH, mouseX, mouseY);
		boolean minusHot = hitRect(minusX, minusY, stepW, countBoxH, mouseX, mouseY);
		g.fill(minusX, minusY, minusX + stepW, minusY + countBoxH, minusHot ? 0xFF30363D : 0xFF21262D);
		g.fill(plusX, plusY, plusX + stepW, plusY + countBoxH, plusHot ? 0xFF30363D : 0xFF21262D);
		g.fill(countNumX - 3, countBoxY, countNumX + countNumW + 3, countBoxY + countBoxH, 0xFF0E0F10);
		g.drawCenteredString(font, "-", minusX + stepW / 2, minusY + 3, minusHot ? 0xFFFF7B72 : 0xFFF85149);
		g.drawCenteredString(font, String.valueOf(resultCount), countNumX + countNumW / 2, countBoxY + 3, 0xFFE6EDF3);
		g.drawCenteredString(font, "+", plusX + stepW / 2, plusY + 3, plusHot ? 0xFF6EE7A0 : 0xFF3FB950);
	}

	private boolean hitStepper(int x, int y, double mouseX, double mouseY) {
		return hitRect(x, y, 14, countBoxH, mouseX, mouseY);
	}

	private static boolean hitRect(int x, int y, int w, int h, double mouseX, double mouseY) {
		return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
	}

	private boolean hitCountArea(double mouseX, double mouseY) {
		int resultSize = 16 * tableScale;
		int left = Math.min(minusX, resultX);
		int right = Math.max(plusX + 14, resultX + resultSize);
		int top = Math.min(countBoxY, resultY);
		int bottom = Math.max(countBoxY + countBoxH, resultY + resultSize);
		return mouseX >= left && mouseX < right && mouseY >= top && mouseY < bottom;
	}

	private boolean localHit(int lx, int ly, int size, double mouseX, double mouseY) {
		double mx = (mouseX - tableX) / (double) tableScale;
		double my = (mouseY - tableY) / (double) tableScale;
		return mx >= lx && mx < lx + size && my >= ly && my < ly + size;
	}

	private static ItemStack stackOf(ResourceLocation id, int count) {
		if (id == null) return ItemStack.EMPTY;
		Item item = BuiltInRegistries.ITEM.get(id);
		if (item == Items.AIR) return ItemStack.EMPTY;
		return new ItemStack(item, Math.max(1, count));
	}

	private ResourceLocation hoveredItem(int mouseX, int mouseY) {
		if (overGroupSuggestions(mouseX, mouseY)) return null;
		int listTop = paletteListTop();
		int listBottom = paletteListBottom();
		if (mouseX >= railX && mouseX < railX + railW && mouseY >= listTop && mouseY < listBottom) {
			int idx = paletteIndexAt(mouseX, mouseY);
			if (idx >= 0) return paletteItems().get(idx);
		}
		if (mouseX >= rightRailX && mouseX < rightRailX + rightRailW && mouseY >= recipeListTop() && mouseY < recipeListBottom()) {
			int idx = recipeIndexAt(mouseY);
			if (idx >= 0 && !recipeRows.get(idx).result.isEmpty()) {
				return BuiltInRegistries.ITEM.getKey(recipeRows.get(idx).result.getItem());
			}
		}
		for (int i = 0; i < 9; i++) {
			if (localHit(30 + (i % 3) * 18, 17 + (i / 3) * 18, 16, mouseX, mouseY)) {
				return gridItems[i];
			}
		}
		if (localHit(124, 35, 16, mouseX, mouseY)) {
			return resultItem;
		}
		int inv = invAt(mouseX, mouseY);
		if (inv >= 0) return invItemId(inv);
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
		if (clickGroupSuggestion(mouseX, mouseY, button)) return true;
		if (button == 0 && pressGroupSuggestionBar(mouseX, mouseY)) return true;
		if (handleRecipeMenuClick(mouseX, mouseY)) return true;
		if (button == 0 && !hitEditBox(searchField, mouseX, mouseY) && !hitEditBox(groupField, mouseX, mouseY)
			&& !hitEditBox(recipeSearchField, mouseX, mouseY)
			&& !overGroupSuggestions(mouseX, mouseY)) {
			setFocused(null);
			groupSuggestions = List.of();
			groupSuggestionIdx = -1;
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
			UiTheme.ScrollClick rsc = UiTheme.clickBar(recipeScrollBarX(), recipeListTop(), recipeListH(),
				recipeScroll, recipeMaxScroll, mouseX, mouseY);
			if (rsc != null) {
				draggingRecipeScroll = true;
				recipeScrollGrab = rsc.grab;
				recipeScroll = rsc.scroll;
				return true;
			}
		}

		if (handleRecipeListClick(mouseX, mouseY, button)) return true;

		if (button == 0 && !overGroupSuggestions(mouseX, mouseY) && hitStepper(plusX, plusY, mouseX, mouseY)) {
			resultCount = Math.min(99, resultCount + 1);
			UiTheme.playClick();
			return true;
		}
		if (button == 0 && !overGroupSuggestions(mouseX, mouseY) && hitStepper(minusX, minusY, mouseX, mouseY)) {
			resultCount = Math.max(1, resultCount - 1);
			UiTheme.playClick();
			return true;
		}

		if (!hitEditBox(groupField, mouseX, mouseY) && !overGroupSuggestions(mouseX, mouseY)) {
			int paletteIdx = paletteIndexAt(mouseX, mouseY);
			if (paletteIdx >= 0) {
				if (button == 0) beginDrag(paletteItems().get(paletteIdx), SRC_PALETTE, mouseX, mouseY);
				return true;
			}

			int slot = slotAt(mouseX, mouseY);
			if (slot != SRC_NONE) {
				if (button == 1) {
					clearSlot(slot);
					return true;
				}
				if (button == 0) {
					ResourceLocation item = itemIn(slot);
					if (item != null) beginDrag(item, slot, mouseX, mouseY);
					return true;
				}
			}
			int inv = invAt(mouseX, mouseY);
			if (inv >= 0) {
				if (button == 0) {
					ResourceLocation item = invItemId(inv);
					if (item != null) beginDrag(item, SRC_INV + inv, mouseX, mouseY);
				}
				return true;
			}
		}
		if (handleFooterClick(mouseX, mouseY, button)) return true;
		boolean result = super.mouseClicked(mouseX, mouseY, button);
		if (button == 0 && SuggestionPopup.clickOpens(groupField, mouseX, mouseY)) {
			setFocused(groupField);
			groupField.setFocused(true);
			updateGroupSuggestions();
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
			groupSuggestionScroll = SuggestionPopup.dragTo(mouseY);
			return true;
		}
		if (draggingPaletteScroll && button == 0) {
			searchScroll = UiTheme.scrollAtGrab(paletteListTop(), paletteListH(), maxSearchScroll, mouseY, paletteScrollGrab);
			return true;
		}
		if (draggingRecipeScroll && button == 0) {
			recipeScroll = UiTheme.scrollAtGrab(recipeListTop(), recipeListH(), recipeMaxScroll, mouseY, recipeScrollGrab);
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
			draggingRecipeScroll = false;
			SuggestionPopup.endDrag();
			if (dropped) return true;
		}
		return super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (namingOpen) return true;
		if (!groupSuggestions.isEmpty() && groupField != null && groupField.isFocused()
			&& overGroupSuggestions(mouseX, mouseY)
			&& SuggestionPopup.maxScroll(groupSuggestions.size()) > 0) {
			groupSuggestionScroll = SuggestionPopup.scrollBy(groupSuggestionScroll, groupSuggestions.size(), scrollY);
			return true;
		}
		if (hitCountArea(mouseX, mouseY) && scrollY != 0) {
			resultCount = Mth.clamp(resultCount + (scrollY > 0 ? 1 : -1), 1, 99);
			return true;
		}
		if (mouseX >= railX && mouseX < railX + railW
			&& mouseY >= topPos && mouseY < topPos + HEIGHT) {
			searchScroll = Mth.clamp(searchScroll - (int) (scrollY * 20), 0, maxSearchScroll);
			return true;
		}
		if (mouseX >= rightRailX && mouseX < rightRailX + rightRailW
			&& mouseY >= topPos && mouseY < topPos + HEIGHT) {
			recipeScroll = Mth.clamp(recipeScroll - (int) (scrollY * 20), 0, recipeMaxScroll);
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (recipeMenu.isOpen() && keyCode == GLFW.GLFW_KEY_ESCAPE) {
			recipeMenu.close();
			recipeMenuRow = null;
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
		if (!groupSuggestions.isEmpty() && groupField != null && groupField.isFocused()) {
			if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER || keyCode == GLFW.GLFW_KEY_TAB) {
				pickGroupSuggestion(Math.max(0, groupSuggestionIdx));
				return true;
			}
			if (keyCode == GLFW.GLFW_KEY_DOWN) {
				groupSuggestionIdx = Math.min(groupSuggestionIdx + 1, groupSuggestions.size() - 1);
				groupSuggestionScroll = SuggestionPopup.keepVisible(groupSuggestionIdx, groupSuggestionScroll, groupSuggestions.size());
				return true;
			}
			if (keyCode == GLFW.GLFW_KEY_UP) {
				groupSuggestionIdx = Math.max(0, groupSuggestionIdx - 1);
				groupSuggestionScroll = SuggestionPopup.keepVisible(groupSuggestionIdx, groupSuggestionScroll, groupSuggestions.size());
				return true;
			}
			if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
				groupSuggestions = List.of();
				groupSuggestionIdx = -1;
				return true;
			}
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public boolean charTyped(char codePoint, int modifiers) {
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
		if (!wasActive || item == null) return wasActive;
		int target = slotAt(x, y);
		if (target == SRC_NONE) {
			if (!isCopySource(source)) clearSlot(source);
			return true;
		}
		if (target == source) return true;
		ResourceLocation replaced = itemIn(target);
		putItem(target, item);
		if (!isCopySource(source)) {
			putItem(source, replaced);
		}
		return true;
	}

	private static boolean isCopySource(int source) {
		return source == SRC_PALETTE || source >= SRC_INV;
	}

	private boolean inLeftRail(double mouseX, double mouseY) {
		return mouseX >= railX && mouseX < railX + railW
			&& mouseY >= topPos && mouseY < topPos + HEIGHT;
	}

	private boolean inRightRail(double mouseX, double mouseY) {
		return mouseX >= rightRailX && mouseX < rightRailX + rightRailW
			&& mouseY >= topPos && mouseY < topPos + HEIGHT;
	}

	private int slotAt(double mouseX, double mouseY) {
		if (inLeftRail(mouseX, mouseY) || inRightRail(mouseX, mouseY) || overGroupSuggestions(mouseX, mouseY) || hitEditBox(groupField, mouseX, mouseY)) {
			return SRC_NONE;
		}
		double mx = (mouseX - tableX) / (double) tableScale;
		double my = (mouseY - tableY) / (double) tableScale;
		for (int i = 0; i < 9; i++) {
			int sx = 30 + (i % 3) * 18;
			int sy = 17 + (i / 3) * 18;
			if (mx >= sx && mx < sx + 18 && my >= sy && my < sy + 18) return i;
		}
		if (mx >= 124 && mx < 140 && my >= 35 && my < 51) return SRC_RESULT;
		return SRC_NONE;
	}

	private int invAt(double mouseX, double mouseY) {
		if (inLeftRail(mouseX, mouseY) || inRightRail(mouseX, mouseY) || overGroupSuggestions(mouseX, mouseY) || hitEditBox(groupField, mouseX, mouseY)) {
			return -1;
		}
		double mx = (mouseX - tableX) / (double) tableScale;
		double my = (mouseY - tableY) / (double) tableScale;
		for (int i = 0; i < 27; i++) {
			int sx = 8 + (i % 9) * 18;
			int sy = 84 + (i / 9) * 18;
			if (mx >= sx && mx < sx + 18 && my >= sy && my < sy + 18) return i;
		}
		for (int i = 0; i < 9; i++) {
			int sx = 8 + i * 18;
			int sy = 142;
			if (mx >= sx && mx < sx + 18 && my >= sy && my < sy + 18) return 27 + i;
		}
		return -1;
	}

	private static ResourceLocation invItemId(int display) {
		var player = Minecraft.getInstance().player;
		if (player == null) return null;
		int index = display < 27 ? display + 9 : display - 27;
		ItemStack stack = player.getInventory().getItem(index);
		if (stack.isEmpty()) return null;
		return BuiltInRegistries.ITEM.getKey(stack.getItem());
	}

	private ResourceLocation itemIn(int slot) {
		if (slot == SRC_RESULT) return resultItem;
		if (slot >= 0 && slot < 9) return gridItems[slot];
		if (slot >= SRC_INV) return invItemId(slot - SRC_INV);
		return null;
	}

	private void putItem(int slot, ResourceLocation item) {
		if (slot == SRC_RESULT) {
			resultItem = item;
			if (item == null) resultCount = 1;
			return;
		}
		if (slot >= 0 && slot < 9) gridItems[slot] = item;
	}

	private void clearSlot(int slot) {
		putItem(slot, null);
	}

	private void clearGrid() {
		Arrays.fill(gridItems, null);
		resultItem = null;
		resultCount = 1;
	}

	private void resetFields() {
		clearGrid();
		shaped = true;
		categoryIdx = 3;
		searching = false;
		searchResults = List.of();
		searchScroll = 0;
		if (searchField != null) searchField.setValue("");
		if (groupField != null) groupField.setValue("");
		if (recipeSearchField != null) recipeSearchField.setValue("");
		groupSuggestions = List.of();
		groupSuggestionIdx = -1;
		closeNamePopup();
		editingId = null;
		syncOptionButtons();
		layoutButtons();
		refreshRecipeList();
		setFocused(null);
	}

	private Session captureSession() {
		Session s = new Session();
		s.grid = Arrays.copyOf(gridItems, 9);
		s.result = resultItem;
		s.resultCount = resultCount;
		s.shaped = shaped;
		s.categoryIdx = categoryIdx;
		s.group = groupField != null ? groupField.getValue() : "";
		s.search = searchField != null ? searchField.getValue() : "";
		s.searchScroll = searchScroll;
		s.recipeSearch = recipeSearchField != null ? recipeSearchField.getValue() : "";
		s.recipeScroll = recipeScroll;
		s.editingId = editingId;
		return s;
	}

	private void applySession(Session s) {
		if (s == null) return;
		gridItems = s.grid != null ? Arrays.copyOf(s.grid, 9) : new ResourceLocation[9];
		resultItem = s.result;
		resultCount = Mth.clamp(s.resultCount, 1, 99);
		shaped = s.shaped;
		categoryIdx = Mth.clamp(s.categoryIdx, 0, CATEGORIES.length - 1);
		if (groupField != null) groupField.setValue(groupFieldText(s.group));
		if (searchField != null) searchField.setValue(s.search != null ? s.search : "");
		searchScroll = s.searchScroll;
		if (recipeSearchField != null) recipeSearchField.setValue(s.recipeSearch != null ? s.recipeSearch : "");
		recipeScroll = s.recipeScroll;
		editingId = s.editingId;
		syncOptionButtons();
	}

	@Override
	public void removed() {
		session = captureSession();
		super.removed();
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return !namingOpen && !recipeMenu.isOpen();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private boolean hasAnyIngredient() {
		for (ResourceLocation id : gridItems) {
			if (id != null) return true;
		}
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
		if (resultItem == null || !hasAnyIngredient()) {
			if (mc.player != null) {
				mc.player.displayClientMessage(Component.translatable("screen.bj_mapedit.recipe_incomplete"), true);
			}
			return;
		}
		if (editingId != null) {
			writeRecipeFile(editingId);
			return;
		}
		openNamePopup(true);
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
		if (!isValidRecipeName(typed)) {
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
		ListedRecipe renameRow = recipeMenuRow;
		closeNamePopup();
		if (rename) {
			renameOwnedRecipe(renameRow, stem);
			return;
		}
		writeRecipeFile(ResourceLocation.fromNamespaceAndPath("bjmapedit", stem));
	}

	private static boolean isValidRecipeName(String raw) {
		if (raw == null || raw.isEmpty() || raw.length() > 64) return false;
		for (int i = 0; i < raw.length(); i++) {
			char c = raw.charAt(i);
			if (!((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_' || c == '-')) {
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
		UiTheme.drawHeader(g, font, Component.translatable("screen.bj_mapedit.recipe_name_hint"), px, py, pw);
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

	private String buildRecipeJson() {
		StringBuilder sb = new StringBuilder();
		String type = shaped ? "minecraft:crafting_shaped" : "minecraft:crafting_shapeless";
		String group = recipeGroupId();

		if (shaped) {
			java.util.Map<ResourceLocation, Character> itemKeyMap = new java.util.LinkedHashMap<>();
			int keyIdx = 0;
			for (int i = 0; i < 9; i++) {
				if (gridItems[i] != null && !itemKeyMap.containsKey(gridItems[i])) {
					itemKeyMap.put(gridItems[i], GRID_KEYS[keyIdx++]);
				}
			}

			sb.append("{\n");
			sb.append("  \"type\": \"").append(type).append("\",\n");
			if (!group.isEmpty()) {
				sb.append("  \"group\": \"").append(jsonString(group)).append("\",\n");
			}
			sb.append("  \"category\": \"").append(CATEGORIES[categoryIdx]).append("\",\n");
			sb.append("  \"pattern\": [\n");
			for (int r = 0; r < 3; r++) {
				sb.append("    \"");
				for (int c = 0; c < 3; c++) {
					ResourceLocation id = gridItems[r * 3 + c];
					if (id != null) sb.append(itemKeyMap.get(id));
					else sb.append(" ");
				}
				sb.append("\"");
				if (r < 2) sb.append(",");
				sb.append("\n");
			}
			sb.append("  ],\n");
			sb.append("  \"key\": {\n");
			int k = 0;
			for (var entry : itemKeyMap.entrySet()) {
				sb.append("    \"").append(entry.getValue()).append("\": {\n");
				sb.append("      \"item\": \"").append(entry.getKey()).append("\"\n");
				sb.append("    }");
				if (++k < itemKeyMap.size()) sb.append(",");
				sb.append("\n");
			}
			sb.append("  },\n");
			appendResult(sb);
			sb.append("}\n");
		} else {
			List<ResourceLocation> ingredients = new ArrayList<>();
			for (int i = 0; i < 9; i++) {
				if (gridItems[i] != null) ingredients.add(gridItems[i]);
			}
			sb.append("{\n");
			sb.append("  \"type\": \"").append(type).append("\",\n");
			if (!group.isEmpty()) {
				sb.append("  \"group\": \"").append(jsonString(group)).append("\",\n");
			}
			sb.append("  \"category\": \"").append(CATEGORIES[categoryIdx]).append("\",\n");
			sb.append("  \"ingredients\": [\n");
			for (int i = 0; i < ingredients.size(); i++) {
				sb.append("    {\n");
				sb.append("      \"item\": \"").append(ingredients.get(i)).append("\"\n");
				sb.append("    }");
				if (i < ingredients.size() - 1) sb.append(",");
				sb.append("\n");
			}
			sb.append("  ],\n");
			appendResult(sb);
			sb.append("}\n");
		}
		return sb.toString();
	}

	private void appendResult(StringBuilder sb) {
		sb.append("  \"result\": {\n");
		sb.append("    \"id\": \"").append(resultItem != null ? resultItem : "minecraft:stone").append("\",\n");
		sb.append("    \"count\": ").append(Math.max(1, resultCount)).append("\n");
		sb.append("  }\n");
	}

	private void copyJsonToClipboard() {
		Minecraft mc = Minecraft.getInstance();
		mc.keyboardHandler.setClipboard(buildRecipeJson());
		if (mc.player != null) {
			mc.player.displayClientMessage(Component.translatable("screen.bj_mapedit.copied"), true);
		}
	}

	private void writeRecipeFile(ResourceLocation recipeId) {
		Minecraft mc = Minecraft.getInstance();
		if (EditorAccess.isRemoteWorld()) {
			remoteJson.put(recipeId, buildRecipeJson());
			remoteOwned.add(recipeId);
			editingId = recipeId;
			pendingRecipeSave = true;
			DatapackNet.send(DatapackOps.WRITE, recipeRel(recipeId), remoteJson.get(recipeId));
			return;
		}
		IntegratedServer server = mc.getSingleplayerServer();
		if (server == null) {
			EditorAccess.needMod();
			return;
		}
		Path packRoot = server.getWorldPath(LevelResource.DATAPACK_DIR).resolve("bjmapedit");
		Path outFile = recipePath(packRoot, recipeId);
		try {
			Files.createDirectories(outFile.getParent());
			writePackMcmeta(packRoot);
			Files.writeString(outFile, buildRecipeJson(), StandardCharsets.UTF_8);
			editingId = recipeId;
			flashSaveMessage();
			refreshRecipeList();
			server.execute(() -> enablePackAndReload(server, recipeId, true));
		} catch (IOException e) {
			if (mc.player != null) {
				mc.player.displayClientMessage(Component.translatable("screen.bj_mapedit.recipe_export_fail"), true);
			}
		}
	}

	private static Path recipePath(Path packRoot, ResourceLocation id) {
		return packRoot.resolve("data").resolve(id.getNamespace()).resolve("recipe").resolve(id.getPath() + ".json");
	}

	private int recipeIndexAt(double mouseY) {
		if (mouseY < recipeListTop() || mouseY >= recipeListBottom()) return -1;
		int idx = (int) ((mouseY - recipeListTop() + recipeScroll) / RECIPE_ROW_H);
		if (idx < 0 || idx >= recipeRows.size()) return -1;
		return idx;
	}

	private boolean handleRecipeMenuClick(double mx, double my) {
		RenameDeleteMenu.Action a = recipeMenu.pick(mx, my);
		ListedRecipe row = recipeMenuRow;
		if (a == RenameDeleteMenu.Action.RENAME && row != null && row.owned) {
			recipeMenuRow = row;
			startRecipeRename(row);
			return true;
		}
		if (a == RenameDeleteMenu.Action.DELETE && row != null && row.owned) {
			recipeMenuRow = null;
			deleteOwnedRecipe(row);
			return true;
		}
		if (a == RenameDeleteMenu.Action.DISMISS) {
			recipeMenuRow = null;
			return true;
		}
		return false;
	}

	private boolean handleRecipeListClick(double mx, double my, int button) {
		if (!inRightRail(mx, my) || hitEditBox(recipeSearchField, mx, my)) return false;
		if (UiTheme.hitScrollBar(recipeScrollBarX(), recipeListTop(), recipeListH(), mx, my)) return false;
		int idx = recipeIndexAt(my);
		if (idx < 0) return my >= recipeListTop();
		ListedRecipe row = recipeRows.get(idx);
		if (button == 1 && row.owned) {
			recipeMenuRow = row;
			recipeMenu.show(font, (int) mx, (int) my, this.width, this.height);
			return true;
		}
		if (button == 0 || button == 1) {
			loadListedRecipe(row);
			return true;
		}
		return true;
	}

	private void startRecipeRename(ListedRecipe row) {
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

	private void deleteOwnedRecipe(ListedRecipe row) {
		if (EditorAccess.isRemoteWorld()) {
			if (row.id.equals(editingId)) editingId = null;
			remoteOwned.remove(row.id);
			remoteJson.remove(row.id);
			DatapackNet.send(DatapackOps.DELETE, recipeRel(row.id), "");
			refreshRecipeList();
			return;
		}
		Path root = packRoot();
		if (root == null) return;
		Path file = existingRecipeFile(root, row.id);
		try {
			if (file != null) Files.deleteIfExists(file);
			if (row.id.equals(editingId)) editingId = null;
			refreshRecipeList();
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

	private void renameOwnedRecipe(ListedRecipe row, String stem) {
		if (row == null) return;
		String path = row.id.getPath();
		int slash = path.lastIndexOf('/');
		String newPath = slash >= 0 ? path.substring(0, slash + 1) + stem : stem;
		ResourceLocation nid = ResourceLocation.tryBuild(row.id.getNamespace(), newPath);
		if (nid == null) return;
		if (EditorAccess.isRemoteWorld()) {
			String json = remoteJson.getOrDefault(row.id, buildRecipeJson());
			remoteOwned.remove(row.id);
			remoteJson.remove(row.id);
			remoteOwned.add(nid);
			remoteJson.put(nid, json);
			if (row.id.equals(editingId)) editingId = nid;
			DatapackNet.send(DatapackOps.RENAME, recipeRel(row.id), recipeRel(nid));
			refreshRecipeList();
			return;
		}
		Path root = packRoot();
		if (root == null) return;
		Path from = existingRecipeFile(root, row.id);
		Path to = recipePath(root, nid);
		try {
			Files.createDirectories(to.getParent());
			if (from != null && Files.isRegularFile(from)) {
				Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
			} else {
				Files.writeString(to, buildRecipeJson(), StandardCharsets.UTF_8);
			}
			if (row.id.equals(editingId)) editingId = nid;
			refreshRecipeList();
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

	private void loadListedRecipe(ListedRecipe row) {
		recipeMenu.close();
		recipeMenuRow = null;
		boolean loaded = false;
		if (EditorAccess.isRemoteWorld()) {
			String json = remoteJson.get(row.id);
			if (json != null) loaded = applyRecipeJsonText(row.id, json);
			else {
				pendingLoadId = row.id;
				DatapackNet.send(DatapackOps.READ, recipeRel(row.id), "");
			}
		} else {
			loaded = existingRecipeFile(packRoot(), row.id) != null && applyRecipeJson(row.id);
		}
		if (!loaded) loaded = applyLoadedHolder(row.id);
		if (!loaded && !EditorAccess.isRemoteWorld()) loaded = applyRecipeJson(row.id);
		if (loaded) {
			editingId = row.id;
			syncOptionButtons();
			layoutButtons();
			setFocused(null);
		}
	}

	private boolean applyLoadedHolder(ResourceLocation id) {
		RecipeManager rm = recipeManager();
		if (rm == null) return false;
		var opt = rm.byKey(id);
		if (opt.isEmpty()) return false;
		var recipe = opt.get().value();
		var lookup = registryLookup();
		if (lookup == null) return false;
		clearGrid();
		if (recipe instanceof ShapedRecipe shapedRecipe) {
			var ings = shapedRecipe.getIngredients();
			int w = Math.max(1, Math.min(3, shapedRecipe.getWidth()));
			int h = Math.max(1, Math.min(3, shapedRecipe.getHeight()));
			if (ings.size() != w * h) return false;
			shaped = true;
			for (int r = 0; r < h; r++) {
				for (int c = 0; c < w; c++) {
					int i = r * w + c;
					gridItems[r * 3 + c] = itemOf(ings.get(i));
				}
			}
			applyCraftingMeta(shapedRecipe, shapedRecipe.getResultItem(lookup));
			return true;
		}
		if (recipe instanceof ShapelessRecipe shapelessRecipe) {
			shaped = false;
			var ings = shapelessRecipe.getIngredients();
			int n = Math.min(9, ings.size());
			for (int i = 0; i < n; i++) {
				gridItems[i] = itemOf(ings.get(i));
			}
			applyCraftingMeta(shapelessRecipe, shapelessRecipe.getResultItem(lookup));
			return true;
		}
		return false;
	}

	private void applyCraftingMeta(CraftingRecipe recipe, ItemStack result) {
		if (!result.isEmpty()) {
			resultItem = BuiltInRegistries.ITEM.getKey(result.getItem());
			resultCount = Mth.clamp(result.getCount(), 1, 99);
		}
		if (groupField != null) groupField.setValue(groupFieldText(recipe.getGroup()));
		categoryIdx = switch (recipe.category()) {
			case BUILDING -> 0;
			case REDSTONE -> 1;
			case EQUIPMENT -> 2;
			default -> 3;
		};
	}

	private static ResourceLocation itemOf(Ingredient ingredient) {
		if (ingredient == null || ingredient.isEmpty()) return null;
		ItemStack[] stacks = ingredient.getItems();
		if (stacks == null || stacks.length == 0 || stacks[0].isEmpty()) return null;
		return BuiltInRegistries.ITEM.getKey(stacks[0].getItem());
	}

	private void refreshRecipeList() {
		LinkedHashMap<ResourceLocation, ListedRecipe> map = new LinkedHashMap<>();
		Set<ResourceLocation> owned = ownedRecipeIds();
		RecipeManager rm = recipeManager();
		var lookup = registryLookup();
		if (rm != null && lookup != null) {
			for (RecipeHolder<?> holder : rm.getAllRecipesFor(RecipeType.CRAFTING)) {
				var recipe = holder.value();
				if (!(recipe instanceof ShapedRecipe) && !(recipe instanceof ShapelessRecipe)) continue;
				ItemStack result = recipe.getResultItem(lookup).copy();
				String label = result.isEmpty() ? holder.id().getPath() : result.getHoverName().getString();
				map.put(holder.id(), new ListedRecipe(holder.id(), result, label, owned.contains(holder.id())));
			}
		}
		for (ResourceLocation id : owned) {
			if (map.containsKey(id)) continue;
			ListedRecipe row = listedFromDisk(id);
			if (row != null) map.put(id, row);
		}
		List<ListedRecipe> all = new ArrayList<>(map.values());
		all.sort((a, b) -> {
			boolean aOwn = a.owned || "bjmapedit".equals(a.id.getNamespace());
			boolean bOwn = b.owned || "bjmapedit".equals(b.id.getNamespace());
			if (aOwn != bOwn) return aOwn ? -1 : 1;
			int byLabel = a.label.compareToIgnoreCase(b.label);
			if (byLabel != 0) return byLabel;
			return a.id.toString().compareToIgnoreCase(b.id.toString());
		});
		String q = recipeSearchField == null ? "" : recipeSearchField.getValue().trim().toLowerCase(Locale.ROOT);
		if (!q.isEmpty()) {
			all.removeIf(r -> !r.label.toLowerCase(Locale.ROOT).contains(q)
				&& !r.id.toString().toLowerCase(Locale.ROOT).contains(q)
				&& !r.id.getPath().toLowerCase(Locale.ROOT).contains(q));
		}
		recipeRows = all;
		updateRecipeScroll();
	}

	private ListedRecipe listedFromDisk(ResourceLocation id) {
		ItemStack result = ItemStack.EMPTY;
		if (EditorAccess.isRemoteWorld()) {
			String json = remoteJson.get(id);
			if (json != null) {
				try {
					JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
					ResourceLocation resultId = resultIdFromJson(obj);
					if (resultId != null) result = stackOf(resultId, 1);
				} catch (Exception ignored) {}
			}
			String label = result.isEmpty() ? id.getPath() : result.getHoverName().getString();
			return new ListedRecipe(id, result, label, true);
		}
		Path root = packRoot();
		Path file = existingRecipeFile(root, id);
		if (file != null) {
			try {
				JsonObject json = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
				ResourceLocation resultId = resultIdFromJson(json);
				if (resultId != null) result = stackOf(resultId, 1);
			} catch (Exception ignored) {}
		}
		String label = result.isEmpty() ? id.getPath() : result.getHoverName().getString();
		return new ListedRecipe(id, result, label, true);
	}

	private boolean applyRecipeJson(ResourceLocation id) {
		if (EditorAccess.isRemoteWorld()) {
			String json = remoteJson.get(id);
			return json != null && applyRecipeJsonText(id, json);
		}
		Path file = existingRecipeFile(packRoot(), id);
		if (file == null) return false;
		try {
			return applyRecipeJsonText(id, Files.readString(file, StandardCharsets.UTF_8));
		} catch (Exception e) {
			return false;
		}
	}

	private boolean applyRecipeJsonText(ResourceLocation id, String text) {
		try {
			JsonObject json = JsonParser.parseString(text).getAsJsonObject();
			String type = json.has("type") ? json.get("type").getAsString() : "";
			clearGrid();
			shaped = !type.contains("shapeless");
			if (shaped && json.has("pattern") && json.has("key")) {
				JsonArray pattern = json.getAsJsonArray("pattern");
				JsonObject key = json.getAsJsonObject("key");
				int rows = Math.min(3, pattern.size());
				for (int r = 0; r < rows; r++) {
					String line = pattern.get(r).getAsString();
					int cols = Math.min(3, line.length());
					for (int c = 0; c < cols; c++) {
						char ch = line.charAt(c);
						if (ch == ' ' || !key.has(String.valueOf(ch))) continue;
						gridItems[r * 3 + c] = itemIdFromIngredient(key.get(String.valueOf(ch)));
					}
				}
			} else if (json.has("ingredients")) {
				JsonArray ings = json.getAsJsonArray("ingredients");
				int n = Math.min(9, ings.size());
				for (int i = 0; i < n; i++) {
					gridItems[i] = itemIdFromIngredient(ings.get(i));
				}
			}
			ResourceLocation resultId = resultIdFromJson(json);
			if (resultId != null) {
				resultItem = resultId;
				resultCount = 1;
				if (json.has("result") && json.get("result").isJsonObject()) {
					JsonObject result = json.getAsJsonObject("result");
					if (result.has("count")) resultCount = Mth.clamp(result.get("count").getAsInt(), 1, 99);
				}
			}
			if (groupField != null) {
				groupField.setValue(groupFieldText(json.has("group") ? json.get("group").getAsString() : ""));
			}
			String cat = json.has("category") ? json.get("category").getAsString() : "misc";
			categoryIdx = 3;
			for (int i = 0; i < CATEGORIES.length; i++) {
				if (CATEGORIES[i].equals(cat)) {
					categoryIdx = i;
					break;
				}
			}
			return resultItem != null && hasAnyIngredient();
		} catch (Exception ignored) {
			return false;
		}
	}

	private static ResourceLocation resultIdFromJson(JsonObject json) {
		if (!json.has("result")) return null;
		JsonElement el = json.get("result");
		if (el.isJsonPrimitive()) return ResourceLocation.tryParse(el.getAsString());
		if (!el.isJsonObject()) return null;
		JsonObject result = el.getAsJsonObject();
		if (result.has("id")) return ResourceLocation.tryParse(result.get("id").getAsString());
		if (result.has("item")) return ResourceLocation.tryParse(result.get("item").getAsString());
		return null;
	}

	private static ResourceLocation itemIdFromIngredient(JsonElement el) {
		if (el == null) return null;
		if (el.isJsonArray()) {
			JsonArray arr = el.getAsJsonArray();
			return arr.isEmpty() ? null : itemIdFromIngredient(arr.get(0));
		}
		if (el.isJsonPrimitive()) return ResourceLocation.tryParse(el.getAsString());
		if (!el.isJsonObject()) return null;
		JsonObject obj = el.getAsJsonObject();
		if (obj.has("item")) return ResourceLocation.tryParse(obj.get("item").getAsString());
		if (obj.has("id")) return ResourceLocation.tryParse(obj.get("id").getAsString());
		if (obj.has("tag")) {
			ResourceLocation tag = ResourceLocation.tryParse(obj.get("tag").getAsString());
			if (tag == null) return null;
			Ingredient ing = Ingredient.of(TagKey.create(Registries.ITEM, tag));
			return itemOf(ing);
		}
		return null;
	}

	private Path packRoot() {
		if (EditorAccess.isRemoteWorld()) return DatapackNet.virtualRoot().resolve("bjmapedit");
		IntegratedServer server = Minecraft.getInstance().getSingleplayerServer();
		if (server == null) return null;
		return server.getWorldPath(LevelResource.DATAPACK_DIR).resolve("bjmapedit");
	}

	private Path existingRecipeFile(Path packRoot, ResourceLocation id) {
		if (packRoot == null || id == null) return null;
		Path a = recipePath(packRoot, id);
		Path b = packRoot.resolve("data").resolve(id.getNamespace()).resolve("recipes").resolve(id.getPath() + ".json");
		if (Files.isRegularFile(a)) return a;
		if (Files.isRegularFile(b)) return b;
		return null;
	}

	private Set<ResourceLocation> ownedRecipeIds() {
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
				for (String folder : new String[]{"recipe", "recipes"}) {
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

	private RecipeManager recipeManager() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.getSingleplayerServer() != null) return mc.getSingleplayerServer().getRecipeManager();
		return mc.level != null ? mc.level.getRecipeManager() : null;
	}

	private HolderLookup.Provider registryLookup() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.getSingleplayerServer() != null) return mc.getSingleplayerServer().registryAccess();
		return mc.level != null ? mc.level.registryAccess() : null;
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
			ingestRecipeTree(payload.extra());
			refreshRecipeList();
			return;
		}
		if (payload.kind() == DatapackOps.KIND_FILE) {
			ResourceLocation id = idFromRecipeRel(payload.path());
			if (id != null) {
				remoteJson.put(id, payload.extra() == null ? "" : payload.extra());
				remoteOwned.add(id);
				if (id.equals(pendingLoadId)) {
					pendingLoadId = null;
					if (applyRecipeJsonText(id, remoteJson.get(id))) {
						editingId = id;
						syncOptionButtons();
						layoutButtons();
					}
				}
				refreshRecipeList();
			}
			return;
		}
		if (payload.kind() == DatapackOps.KIND_OK) {
			if (pendingRecipeSave) {
				pendingRecipeSave = false;
				flashSaveMessage();
				DatapackNet.send(DatapackOps.RELOAD, "bjmapedit", "");
			} else if ("screen.bj_mapedit.deleted".equals(payload.extra())) {
				Minecraft mc = Minecraft.getInstance();
				if (mc.player != null) {
					mc.player.displayClientMessage(Component.translatable(payload.extra()), true);
				}
			}
			refreshRecipeList();
		}
	}

	private String recipeRel(ResourceLocation id) {
		return "bjmapedit/data/" + id.getNamespace() + "/recipe/" + id.getPath() + ".json";
	}

	private void ingestRecipeTree(String listing) {
		remoteOwned.clear();
		if (listing == null || listing.isEmpty()) return;
		for (String line : listing.split("\n")) {
			if (!line.startsWith("F\t")) continue;
			ResourceLocation id = idFromRecipeRel(line.substring(2));
			if (id != null) remoteOwned.add(id);
		}
	}

	private static ResourceLocation idFromRecipeRel(String rel) {
		if (rel == null) return null;
		String path = rel.replace('\\', '/');
		String prefix = "bjmapedit/data/";
		if (!path.startsWith(prefix) || !path.endsWith(".json")) return null;
		String rest = path.substring(prefix.length(), path.length() - 5);
		int slash = rest.indexOf('/');
		if (slash < 0) return null;
		String ns = rest.substring(0, slash);
		String after = rest.substring(slash + 1);
		if (after.startsWith("recipe/")) after = after.substring("recipe/".length());
		else if (after.startsWith("recipes/")) after = after.substring("recipes/".length());
		else return null;
		return ResourceLocation.tryBuild(ns, after);
	}

	private static void writePackMcmeta(Path packRoot) throws IOException {
		Files.writeString(packRoot.resolve("pack.mcmeta"),
			"{\n  \"pack\": {\n    \"pack_format\": 48,\n    \"description\": \"BJ-MapEdit\"\n  }\n}\n",
			StandardCharsets.UTF_8);
	}

	private static void enablePackAndReload(IntegratedServer server, ResourceLocation recipeId, boolean expectLoaded) {
		var repo = server.getPackRepository();
		repo.reload();
		Collection<String> selected = new ArrayList<>(repo.getSelectedIds());
		if (!selected.contains("file/bjmapedit")) selected.add("file/bjmapedit");
		repo.setSelected(selected);
		server.reloadResources(repo.getSelectedIds()).thenRunAsync(() -> {
			boolean loaded = server.getRecipeManager().byKey(recipeId).isPresent();
			Minecraft.getInstance().execute(() -> {
				if (expectLoaded && !loaded) {
					var player = Minecraft.getInstance().player;
					if (player != null) {
						player.displayClientMessage(Component.translatable("screen.bj_mapedit.recipe_not_loaded"), true);
					}
				}
				if (Minecraft.getInstance().screen instanceof RecipeEditorScreen screen) {
					screen.refreshRecipeList();
				}
			});
		}, server);
	}
}
