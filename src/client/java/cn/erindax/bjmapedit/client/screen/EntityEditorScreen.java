package cn.erindax.bjmapedit.client.screen;

import cn.erindax.bjmapedit.client.widget.EntityTemplateStore;
import cn.erindax.bjmapedit.client.widget.EntityExtraLists;
import cn.erindax.bjmapedit.client.widget.GiveCommands;
import cn.erindax.bjmapedit.client.widget.SafeIds;
import cn.erindax.bjmapedit.client.widget.SuggestionPopup;
import cn.erindax.bjmapedit.client.widget.TemplateDrag;
import cn.erindax.bjmapedit.client.widget.TemplateOrg;
import cn.erindax.bjmapedit.client.widget.RenameDeleteMenu;
import cn.erindax.bjmapedit.client.widget.TemplateRailUi;
import cn.erindax.bjmapedit.client.widget.UiTheme;
import cn.erindax.bjmapedit.networking.payload.ExecuteCommandsPayload;
import com.mojang.blaze3d.platform.Lighting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Environment(EnvType.CLIENT)
public class EntityEditorScreen extends Screen {

	private int WIDTH = 500;
	private int HEIGHT = 400;
	private static final int LABEL_X = 12;
	private static final int ROW_H = 20;
	private static final int FOOTER_H = 30;
	private static final int TEMPLATE_ROW_H = 22;
	private int INPUT_X = 88;
	private int INPUT_W = 160;
	private int RIGHT_X = 280;
	private int CONTENT_TOP = 30;
	private int CONTENT_BOTTOM;
	private int templateRailX = 8;
	private int templateRailW = 148;

	private static final Map<String, String> NBT_KEY_CN = new HashMap<>();
	private static final Set<String> CHECKBOX_KEYS = Set.of(
		"NoAI", "Silent", "NoGravity", "Angry", "IsBaby", "CanBreakDoors",
		"CanPickUpLoot", "powered", "ignited", "Sheared", "Tame",
		"IsImmuneToZombification", "PlayerCreated", "Pumpkin",
		"HasNectar", "HasStung", "IsScreamingGoat", "HasLeftHorn", "HasRightHorn"
	);
	private static final String[] DEFAULT_NBT_KEYS = {
		"CustomName", "Health", "NoAI", "Silent", "NoGravity", "Fire", "Tags"
	};
	private static final Map<String, String[]> ENTITY_EXTRA_KEYS = Map.ofEntries(
		Map.entry("minecraft:cat", new String[]{"variant", "CollarColor"}),
		Map.entry("minecraft:wolf", new String[]{"variant", "CollarColor", "Angry"}),
		Map.entry("minecraft:zombie", new String[]{"IsBaby", "CanBreakDoors", "CanPickUpLoot"}),
		Map.entry("minecraft:husk", new String[]{"IsBaby", "CanBreakDoors", "CanPickUpLoot"}),
		Map.entry("minecraft:drowned", new String[]{"IsBaby", "CanBreakDoors", "CanPickUpLoot"}),
		Map.entry("minecraft:zombie_villager", new String[]{"IsBaby", "CanBreakDoors"}),
		Map.entry("minecraft:skeleton", new String[]{"CanPickUpLoot"}),
		Map.entry("minecraft:stray", new String[]{"CanPickUpLoot"}),
		Map.entry("minecraft:wither_skeleton", new String[]{"CanPickUpLoot"}),
		Map.entry("minecraft:creeper", new String[]{"powered", "ExplosionRadius", "ignited"}),
		Map.entry("minecraft:sheep", new String[]{"Color", "Sheared"}),
		Map.entry("minecraft:horse", new String[]{"Variant", "Tame"}),
		Map.entry("minecraft:donkey", new String[]{"Tame"}),
		Map.entry("minecraft:mule", new String[]{"Tame"}),
		Map.entry("minecraft:parrot", new String[]{"Variant"}),
		Map.entry("minecraft:fox", new String[]{"Type"}),
		Map.entry("minecraft:axolotl", new String[]{"Variant"}),
		Map.entry("minecraft:slime", new String[]{"Size"}),
		Map.entry("minecraft:magma_cube", new String[]{"Size"}),
		Map.entry("minecraft:phantom", new String[]{"Size"}),
		Map.entry("minecraft:ghast", new String[]{"ExplosionPower"}),
		Map.entry("minecraft:enderman", new String[]{"carriedBlockState"}),
		Map.entry("minecraft:piglin", new String[]{"IsBaby", "IsImmuneToZombification"}),
		Map.entry("minecraft:hoglin", new String[]{"IsImmuneToZombification"}),
		Map.entry("minecraft:zoglin", new String[]{"IsBaby"}),
		Map.entry("minecraft:iron_golem", new String[]{"PlayerCreated"}),
		Map.entry("minecraft:snow_golem", new String[]{"Pumpkin"}),
		Map.entry("minecraft:llama", new String[]{"Variant", "Strength"}),
		Map.entry("minecraft:trader_llama", new String[]{"Variant", "Strength"}),
		Map.entry("minecraft:rabbit", new String[]{"RabbitType"}),
		Map.entry("minecraft:panda", new String[]{"MainGene", "HiddenGene"}),
		Map.entry("minecraft:mooshroom", new String[]{"Type"}),
		Map.entry("minecraft:frog", new String[]{"variant"}),
		Map.entry("minecraft:goat", new String[]{"IsScreamingGoat", "HasLeftHorn", "HasRightHorn"}),
		Map.entry("minecraft:shulker", new String[]{"Color"}),
		Map.entry("minecraft:bee", new String[]{"HasNectar", "HasStung"})
	);
	private static final Map<String, String> EXTRA_KEY_CN = Map.ofEntries(
		Map.entry("variant", "变种"),
		Map.entry("CollarColor", "项圈颜色"),
		Map.entry("Angry", "愤怒"),
		Map.entry("IsBaby", "幼年"),
		Map.entry("CanBreakDoors", "能破门"),
		Map.entry("CanPickUpLoot", "拾取物品"),
		Map.entry("powered", "闪电充能"),
		Map.entry("ExplosionRadius", "爆炸半径"),
		Map.entry("ignited", "已点燃"),
		Map.entry("Color", "颜色"),
		Map.entry("Sheared", "已剪毛"),
		Map.entry("Variant", "变种"),
		Map.entry("Tame", "已驯服"),
		Map.entry("Type", "类型"),
		Map.entry("Size", "大小"),
		Map.entry("ExplosionPower", "爆炸威力"),
		Map.entry("carriedBlockState", "手持方块"),
		Map.entry("IsImmuneToZombification", "免疫僵尸化"),
		Map.entry("PlayerCreated", "玩家建造"),
		Map.entry("Pumpkin", "南瓜头"),
		Map.entry("RabbitType", "兔子类型"),
		Map.entry("MainGene", "主基因"),
		Map.entry("HiddenGene", "隐性基因"),
		Map.entry("Strength", "负重"),
		Map.entry("HasNectar", "有花蜜"),
		Map.entry("HasStung", "已蛰针"),
		Map.entry("IsScreamingGoat", "尖叫"),
		Map.entry("HasLeftHorn", "左角"),
		Map.entry("HasRightHorn", "右角")
	);

	static {
		NBT_KEY_CN.put("CustomName", "名称");
		NBT_KEY_CN.put("Health", "生命值");
		NBT_KEY_CN.put("NoAI", "无AI");
		NBT_KEY_CN.put("Silent", "无声");
		NBT_KEY_CN.put("NoGravity", "无重力");
		NBT_KEY_CN.put("Fire", "燃烧时间");
		NBT_KEY_CN.put("Tags", "标签");
	}

	private EditBox entityIdField;
	private final List<NbtFieldRow> nbtFieldRows = new ArrayList<>();
	private Button addNbtFieldBtn;
	private Button entityNameEditorBtn;
	private Button summonBtn;
	private Button copyBtn;
	private Button saveBtn;

	private int leftPos;
	private int topPos;
	private int scrollOffset;
	private int maxScroll;
	private boolean draggingScroll;
	private double scrollGrabOffset;
	private int templateScroll;
	private int templateMaxScroll;
	private boolean draggingTemplateScroll;
	private double templateScrollGrab;
	private int selectedTemplate = -1;
	private String selectedFolder = "";
	private static int sessionSelectedTemplate = -1;
	private static String sessionSelectedFolder = "";
	private static final Set<String> collapsedFolders = new HashSet<>();
	private EditBox templateRenameBox;
	private int renamingTemplate = -1;
	private String renamingFolder = "";
	private final TemplateDrag templateDrag = new TemplateDrag();
	private final RenameDeleteMenu templateMenu = new RenameDeleteMenu();
	private TemplateOrg.Row templateMenuRow;
	private int refreshX, refreshY;
	private boolean opened;

	private Entity previewEntity;
	private String previewEntityTypeId;
	private String lastPreviewSig;
	private float previewRotY;
	private float previewRotX;
	private boolean draggingPreview;
	private double dragStartX, dragStartY;
	private float dragStartRotY, dragStartRotX;
	private String pendingEntityName;
	private String lastExtraType = "";
	private int previewX, previewY, previewW, previewH;

	private record EntitySuggestion(ResourceLocation id, String label) {}
	private List<EntitySuggestion> entitySuggestions = new ArrayList<>();
	private int entitySuggestionIdx = -1;
	private List<EntityExtraLists.Option> extraSuggestions = List.of();
	private int extraSuggestionIdx = -1;
	private EditBox extraSuggestionBox;
	private int suggestionScroll;

	private static SavedState savedState;

	private record SavedState(String entityId, List<String> nbtKeys, List<String> nbtValues) {}

	private static class NbtFieldRow {
		final boolean isDefault;
		final String defaultKey;
		EditBox keyBox;
		EditBox valueBox;
		Checkbox checkBox;
		Button removeBtn;

		NbtFieldRow(boolean isDefault, String defaultKey) {
			this.isDefault = isDefault;
			this.defaultKey = defaultKey;
		}

		boolean isCheckbox() {
			return defaultKey != null && CHECKBOX_KEYS.contains(defaultKey);
		}

		String getKey() {
			if (isDefault) return defaultKey;
			return keyBox != null ? keyBox.getValue().trim() : "";
		}

		String getValue() {
			if (checkBox != null) return checkBox.selected() ? "true" : "false";
			return valueBox != null ? valueBox.getValue().trim() : "";
		}
	}

	public EntityEditorScreen() {
		super(Component.translatable("screen.bj_mapedit.entity2_title"));
	}

	@Override
	protected void init() {
		boolean first = !opened;
		opened = true;
		boolean wasReinit = entityIdField != null;
		SavedState snap = wasReinit ? captureState() : null;
		int snapScroll = scrollOffset;
		super.init();
		layoutPanel();

		entityIdField = new EditBox(font, 0, 0, INPUT_W, 16, Component.empty());
		entityIdField.setResponder(s -> {
			try {
				updateEntitySuggestions();
				updatePreviewEntity();
				syncEntitySpecificFields();
			} catch (Throwable ignored) {
				entitySuggestions = List.of();
				entitySuggestionIdx = -1;
			}
		});
		this.addRenderableWidget(entityIdField);

		nbtFieldRows.clear();
		lastExtraType = "";
		for (String key : DEFAULT_NBT_KEYS) addRow(new NbtFieldRow(true, key));

		addNbtFieldBtn = this.addRenderableWidget(Button.builder(Component.literal("添加自定义"), b -> addNbtField())
			.bounds(0, 0, 88, 16).build());
		entityNameEditorBtn = this.addRenderableWidget(Button.builder(
			Component.translatable("screen.bj_mapedit.edit_text"), b -> openEntityNameTextEditor()
		).bounds(0, 0, 88, 16).build());
		copyBtn = this.addRenderableWidget(Button.builder(
			Component.translatable("screen.bj_mapedit.copy_cmd"), b -> copySummonCommand()
		).bounds(0, 0, 70, 20).build());
		summonBtn = this.addRenderableWidget(Button.builder(
			Component.translatable("screen.bj_mapedit.summon_plain"), b -> summonEntity()
		).bounds(0, 0, 70, 20).build());
		saveBtn = this.addRenderableWidget(Button.builder(
			Component.translatable("screen.bj_mapedit.save_template"), b -> saveTemplate()
		).bounds(0, 0, 70, 20).build());

		templateRenameBox = new EditBox(font, 0, -1000, 80, 16, Component.empty());
		templateRenameBox.setMaxLength(24);
		templateRenameBox.visible = false;
		this.addRenderableWidget(templateRenameBox);

		if (wasReinit && snap != null) applyState(snap);
		else if (first && savedState != null) {
			applyState(savedState);
			selectedTemplate = sessionSelectedTemplate;
			selectedFolder = sessionSelectedFolder;
		}
		scrollOffset = snapScroll;
		repositionAll();
	}

	@Override
	public void added() {
		super.added();
		if (pendingEntityName != null) {
			setCustomName(pendingEntityName);
			pendingEntityName = null;
		}
	}

	private void layoutPanel() {
		int margin = 8;
		int gap = 8;
		int availW = Math.max(32, this.width - margin * 2);
		int availH = Math.max(32, this.height - margin * 2);
		templateRailW = Mth.clamp(availW * 20 / 100, 112, 220);
		if (availW < 420) templateRailW = Mth.clamp(availW * 26 / 100, 88, 140);
		WIDTH = Math.max(8, availW - templateRailW - gap);
		HEIGHT = availH;
		templateRailX = margin;
		leftPos = margin + templateRailW + gap;
		topPos = margin;
		CONTENT_TOP = UiTheme.HEADER_H + 8;
		CONTENT_BOTTOM = HEIGHT - FOOTER_H - 4;
		int labelW = 56;
		if (this.font != null) {
			labelW = Math.max(labelW, this.font.width("生命值") + 10);
			labelW = Math.max(labelW, this.font.width("免疫僵尸化") + 10);
			labelW = Math.max(labelW, this.font.width("项圈颜色") + 10);
			labelW = Math.max(labelW, this.font.width("手持方块") + 10);
		}
		INPUT_X = LABEL_X + labelW;
		int rightW = Math.min(Math.max(150, WIDTH - (INPUT_X + 140) - 8), Math.max(160, WIDTH * 36 / 100));
		if (WIDTH < 480) rightW = Math.max(110, WIDTH * 32 / 100);
		RIGHT_X = Math.max(INPUT_X + 72, WIDTH - rightW - 8);
		INPUT_W = Math.max(48, RIGHT_X - INPUT_X - 14);
		updateTemplateScroll();
		refreshX = UiTheme.headerIconX(leftPos, WIDTH);
		refreshY = UiTheme.headerIconY(topPos);
	}

	private void addRow(NbtFieldRow row) {
		if (row.isCheckbox()) {
			row.checkBox = Checkbox.builder(Component.empty(), font).pos(0, 0).build();
			this.addRenderableWidget(row.checkBox);
		} else {
			if (!row.isDefault) {
				row.keyBox = new EditBox(font, 0, 0, 56, 16, Component.empty());
				this.addRenderableWidget(row.keyBox);
				row.removeBtn = this.addRenderableWidget(Button.builder(Component.literal("-"), b -> removeRow(row))
					.bounds(0, 0, 16, 16).build());
			}
			row.valueBox = new EditBox(font, 0, 0, INPUT_W, 16, Component.empty());
			NbtFieldRow attached = row;
			row.valueBox.setResponder(s -> {
				if (attached.isDefault) updateExtraSuggestions(attached);
			});
			this.addRenderableWidget(row.valueBox);
		}
		nbtFieldRows.add(row);
	}

	private void removeRow(NbtFieldRow row) {
		dropRowWidgets(row);
		nbtFieldRows.remove(row);
		repositionAll();
	}

	private void dropRowWidgets(NbtFieldRow row) {
		if (row.keyBox != null) this.removeWidget(row.keyBox);
		if (row.valueBox != null) this.removeWidget(row.valueBox);
		if (row.checkBox != null) this.removeWidget(row.checkBox);
		if (row.removeBtn != null) this.removeWidget(row.removeBtn);
	}

	private void addNbtField() {
		addRow(new NbtFieldRow(false, null));
		repositionAll();
	}

	private void openEntityNameTextEditor() {
		pendingEntityName = customNameValue();
		Minecraft.getInstance().setScreen(new TextEditorScreen(this, pendingEntityName, newText -> {
			if (newText != null) pendingEntityName = newText;
		}));
	}

	private String customNameValue() {
		for (NbtFieldRow row : nbtFieldRows) {
			if ("CustomName".equals(row.defaultKey) && row.valueBox != null) return row.valueBox.getValue();
		}
		return "";
	}

	private void setCustomName(String text) {
		for (NbtFieldRow row : nbtFieldRows) {
			if ("CustomName".equals(row.defaultKey) && row.valueBox != null) {
				row.valueBox.setValue(text == null ? "" : text);
				return;
			}
		}
	}

	private void repositionAll() {
		layoutPanel();
		if (entityIdField == null || summonBtn == null) return;
		int y = topPos + CONTENT_TOP + 2 - scrollOffset;
		int total = 0;
		placeLeft(entityIdField, y);
		y += ROW_H;
		total += ROW_H;
		for (NbtFieldRow row : nbtFieldRows) {
			if (row.checkBox != null) {
				row.checkBox.setX(leftPos + INPUT_X);
				row.checkBox.setY(y);
				row.checkBox.setWidth(20);
			} else if (row.isDefault && row.valueBox != null) {
				placeLeft(row.valueBox, y);
			} else if (row.valueBox != null) {
				int minus = row.removeBtn != null ? 20 : 0;
				int keyW = 56;
				if (row.keyBox != null) {
					row.keyBox.setX(leftPos + INPUT_X);
					row.keyBox.setY(y);
					row.keyBox.setWidth(keyW);
				}
				row.valueBox.setX(leftPos + INPUT_X + keyW + 4);
				row.valueBox.setY(y);
				row.valueBox.setWidth(Math.max(24, INPUT_W - keyW - 4 - minus));
				if (row.removeBtn != null) {
					row.removeBtn.setPosition(leftPos + INPUT_X + INPUT_W - 16, y);
					row.removeBtn.setWidth(16);
					row.removeBtn.setHeight(16);
				}
			}
			y += ROW_H;
			total += ROW_H;
			if (row.isDefault && "CustomName".equals(row.defaultKey) && entityNameEditorBtn != null) {
				entityNameEditorBtn.setX(leftPos + INPUT_X);
				entityNameEditorBtn.setY(y);
				entityNameEditorBtn.setWidth(Math.min(INPUT_W, Math.max(56, font.width(entityNameEditorBtn.getMessage()) + 12)));
				y += ROW_H;
				total += ROW_H;
			}
		}
		if (addNbtFieldBtn != null) {
			addNbtFieldBtn.setX(leftPos + INPUT_X);
			addNbtFieldBtn.setY(y);
			addNbtFieldBtn.setWidth(Math.min(INPUT_W, Math.max(56, font.width(addNbtFieldBtn.getMessage()) + 12)));
		}
		total += ROW_H;
		maxScroll = Math.max(0, total + 8 - (CONTENT_BOTTOM - CONTENT_TOP));
		scrollOffset = Mth.clamp(scrollOffset, 0, maxScroll);
		layoutPreview();
		layoutActionButtons();
		syncTemplateRenameBox();
	}

	private void placeLeft(EditBox box, int y) {
		box.setX(leftPos + INPUT_X);
		box.setY(y);
		box.setWidth(INPUT_W);
	}

	private void layoutPreview() {
		previewX = leftPos + RIGHT_X;
		previewY = topPos + CONTENT_TOP;
		previewW = Math.max(64, WIDTH - RIGHT_X - 8);
		previewH = Math.max(80, CONTENT_BOTTOM - CONTENT_TOP);
	}

	private void layoutActionButtons() {
		int btnH = 20;
		int footerY = topPos + HEIGHT - FOOTER_H + (FOOTER_H - btnH) / 2;
		int pad = 8;
		int gap = 6;
		int copyW = Math.max(52, font.width(copyBtn.getMessage()) + 22);
		int summonW = Math.max(52, font.width(summonBtn.getMessage()) + 22);
		int saveW = Math.max(52, font.width(saveBtn.getMessage()) + 22);
		int inner = Math.max(8, WIDTH - pad * 2);
		int need = copyW + summonW + saveW + gap * 2;
		if (need > inner) {
			float s = (float) (inner - gap * 2) / (copyW + summonW + saveW);
			copyW = Math.max(36, (int) (copyW * s));
			summonW = Math.max(36, (int) (summonW * s));
			saveW = Math.max(36, (int) (saveW * s));
		}
		int x = leftPos + pad;
		copyBtn.setPosition(x, footerY);
		copyBtn.setWidth(copyW);
		copyBtn.setHeight(btnH);
		x += copyW + gap;
		summonBtn.setPosition(x, footerY);
		summonBtn.setWidth(summonW);
		summonBtn.setHeight(btnH);
		saveBtn.setPosition(leftPos + WIDTH - pad - saveW, footerY);
		saveBtn.setWidth(saveW);
		saveBtn.setHeight(btnH);
	}

	private boolean isChrome(AbstractWidget w) {
		return w == summonBtn || w == copyBtn || w == saveBtn || w == templateRenameBox;
	}

	private void syncEntitySpecificFields() {
		String type = resolvedTypeId();
		if (type == null) type = "";
		String input = entityIdField != null ? entityIdField.getValue().trim() : "";
		if (type.isEmpty() && !input.isEmpty()) return;
		if (type.equals(lastExtraType)) return;
		lastExtraType = type;
		Set<String> keep = new HashSet<>(List.of(DEFAULT_NBT_KEYS));
		String[] extra = ENTITY_EXTRA_KEYS.getOrDefault(type, new String[0]);
		keep.addAll(List.of(extra));
		nbtFieldRows.removeIf(row -> {
			if (!row.isDefault || keep.contains(row.defaultKey)) return false;
			dropRowWidgets(row);
			return true;
		});
		Set<String> have = new HashSet<>();
		for (NbtFieldRow row : nbtFieldRows) {
			if (row.isDefault && row.defaultKey != null) have.add(row.defaultKey);
		}
		for (String key : extra) {
			if (have.contains(key)) continue;
			addRow(new NbtFieldRow(true, key));
		}
		repositionAll();
	}

	private void updateEntitySuggestions() {
		if (entityIdField == null || !entityIdField.isFocused()) {
			entitySuggestions = List.of();
			entitySuggestionIdx = -1;
			return;
		}
		String input = entityIdField.getValue().trim();
		String lower = input.toLowerCase(Locale.ROOT);
		List<EntitySuggestion> out = new ArrayList<>();
		for (ResourceLocation id : BuiltInRegistries.ENTITY_TYPE.keySet()) {
			if (excludedEntity(id)) continue;
			EntityType<?> type = entityTypeOf(id);
			if (type == null || !type.canSummon()) continue;
			String cn = entityCn(type);
			if (cn.isEmpty()) continue;
			if (!input.isEmpty()) {
				boolean hit = cn.toLowerCase(Locale.ROOT).contains(lower)
					|| id.toString().toLowerCase(Locale.ROOT).contains(lower)
					|| id.getPath().toLowerCase(Locale.ROOT).contains(lower);
				if (!hit) continue;
			}
			out.add(new EntitySuggestion(id, cn));
		}
		if (!input.isEmpty()) {
			out.sort(Comparator
				.comparingInt((EntitySuggestion s) -> suggestionRank(s, lower))
				.thenComparing(s -> s.label)
				.thenComparing(s -> s.id.toString()));
		} else {
			out.sort(Comparator.comparing((EntitySuggestion s) -> s.label).thenComparing(s -> s.id.toString()));
		}
		entitySuggestions = out;
		entitySuggestionIdx = out.isEmpty() ? -1 : 0;
		suggestionScroll = SuggestionPopup.clampScroll(suggestionScroll, entitySuggestions.size());
	}

	private static int suggestionRank(EntitySuggestion s, String lower) {
		String cn = s.label.toLowerCase(Locale.ROOT);
		if (cn.equals(lower)) return 0;
		if (cn.startsWith(lower)) return 1;
		String path = s.id.getPath();
		if (path.equals(lower) || s.id.toString().equals(lower)) return 2;
		if (path.startsWith(lower)) return 3;
		return 4;
	}

	private static boolean excludedEntity(ResourceLocation id) {
		String idStr = id.toString();
		return idStr.equals("minecraft:armor_stand") || idStr.equals("minecraft:item_frame")
			|| idStr.equals("minecraft:glow_item_frame") || idStr.equals("minecraft:villager")
			|| idStr.equals("minecraft:player");
	}

	private static EntityType<?> entityTypeOf(ResourceLocation id) {
		try {
			return BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null);
		} catch (Throwable ignored) {
			return null;
		}
	}

	private static String entityCn(EntityType<?> type) {
		try {
			return type.getDescription().getString().trim();
		} catch (Throwable ignored) {
			return "";
		}
	}

	private ResourceLocation resolveEntityId() {
		if (entityIdField == null) return null;
		String input = entityIdField.getValue().trim();
		if (input.isEmpty()) return null;
		if (SafeIds.looksLikeId(input)) {
			ResourceLocation id = SafeIds.tryParse(input);
			if (id == null && input.indexOf(':') < 0) id = SafeIds.tryParseItem(input);
			if (id != null && !excludedEntity(id) && BuiltInRegistries.ENTITY_TYPE.containsKey(id)) return id;
		}
		String lower = input.toLowerCase(Locale.ROOT);
		ResourceLocation exact = null;
		ResourceLocation unique = null;
		int hits = 0;
		for (ResourceLocation id : BuiltInRegistries.ENTITY_TYPE.keySet()) {
			if (excludedEntity(id)) continue;
			EntityType<?> type = entityTypeOf(id);
			if (type == null || !type.canSummon()) continue;
			String cn = entityCn(type);
			if (cn.isEmpty()) continue;
			if (cn.equals(input) || cn.equalsIgnoreCase(input)) {
				exact = id;
				break;
			}
			if (cn.toLowerCase(Locale.ROOT).contains(lower)) {
				hits++;
				unique = id;
			}
		}
		if (exact != null) return exact;
		return hits == 1 ? unique : null;
	}

	private String resolvedTypeId() {
		ResourceLocation id = resolveEntityId();
		return id == null ? null : id.toString();
	}

	private String buildEntityNbt() {
		StringBuilder nbt = new StringBuilder("{");
		boolean hasAny = false;
		for (NbtFieldRow row : nbtFieldRows) {
			String key = row.getKey();
			String value = row.getValue();
			if (key.isEmpty()) continue;
			if (row.isCheckbox() && !"true".equals(value)) continue;
			if (!row.isCheckbox() && value.isEmpty()) continue;
			String extraNbt = EntityExtraLists.toNbtValue(resolvedTypeId(), key, value);
			if (extraNbt != null) {
				if (hasAny) nbt.append(",");
				nbt.append(key).append(":").append(extraNbt);
				hasAny = true;
				continue;
			}
			if (hasAny) nbt.append(",");
			if (key.equals("CustomName")) {
				nbt.append("CustomName:'{\"text\":\"").append(escapeJson(value)).append("\"}'");
			} else if (key.equals("Tags")) {
				StringBuilder tagsArray = new StringBuilder("[");
				boolean firstTag = true;
				for (String part : value.split(",")) {
					String trimmed = part.trim();
					if (trimmed.isEmpty()) continue;
					if (!firstTag) tagsArray.append(",");
					tagsArray.append("\"").append(escapeJson(trimmed)).append("\"");
					firstTag = false;
				}
				tagsArray.append("]");
				nbt.append("Tags:").append(tagsArray);
			} else if (row.isCheckbox()) {
				nbt.append(key).append(":1b");
			} else if (key.equals("Health")) {
				try {
					nbt.append("Health:").append(Float.parseFloat(value)).append("f");
				} catch (NumberFormatException e) {
					nbt.append("Health:").append(value);
				}
			} else if (key.equals("Fire")) {
				try {
					int fire = Integer.parseInt(value);
					if (fire > 0) nbt.append("Fire:").append(fire).append("s");
					else {
						if (hasAny) nbt.setLength(nbt.length() - 1);
						continue;
					}
				} catch (NumberFormatException e) {
					nbt.append("Fire:").append(value);
				}
			} else {
				try {
					Integer.parseInt(value);
					nbt.append(key).append(":").append(value);
				} catch (NumberFormatException e1) {
					try {
						Float.parseFloat(value);
						nbt.append(key).append(":").append(value).append("f");
					} catch (NumberFormatException e2) {
						String lower = value.toLowerCase(Locale.ROOT);
						if (lower.equals("true") || lower.equals("false")) {
							nbt.append(key).append(":").append(lower.equals("true") ? "1b" : "0b");
						} else {
							nbt.append(key).append(":\"").append(escapeJson(value)).append("\"");
						}
					}
				}
			}
			hasAny = true;
		}
		if (collarColorId() >= 0) {
			boolean hasTame = false;
			boolean hasOwner = false;
			for (NbtFieldRow row : nbtFieldRows) {
				if ("Tame".equals(row.getKey()) && "true".equals(row.getValue())) hasTame = true;
				if ("Owner".equals(row.getKey()) && !row.getValue().isEmpty()) hasOwner = true;
			}
			if (!hasTame) {
				if (hasAny) nbt.append(",");
				nbt.append("Tame:1b");
				hasAny = true;
			}
			var player = Minecraft.getInstance().player;
			if (!hasOwner && player != null) {
				if (hasAny) nbt.append(",");
				nbt.append("Owner:").append(uuidSnbt(player.getUUID()));
			}
		}
		nbt.append("}");
		return nbt.length() <= 2 ? "{}" : nbt.toString();
	}

	private static String uuidSnbt(java.util.UUID uuid) {
		long msb = uuid.getMostSignificantBits();
		long lsb = uuid.getLeastSignificantBits();
		return "[I;" + (int) (msb >> 32) + "," + (int) msb + "," + (int) (lsb >> 32) + "," + (int) lsb + "]";
	}

	private String escapeJson(String s) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			switch (c) {
				case '"' -> sb.append("\\\"");
				case '\\' -> sb.append("\\\\");
				case '\'' -> sb.append("\\'");
				case '\n' -> sb.append("\\n");
				case '\r' -> sb.append("\\r");
				case '\t' -> sb.append("\\t");
				default -> sb.append(c);
			}
		}
		return sb.toString();
	}

	private Vec3 lookPos() {
		var player = Minecraft.getInstance().player;
		if (player == null) return Vec3.ZERO;
		HitResult hit = player.pick(64.0, 1.0F, false);
		if (hit instanceof BlockHitResult block && block.getType() == HitResult.Type.BLOCK) {
			return block.getLocation();
		}
		return player.getEyePosition(1.0F).add(player.getViewVector(1.0F).scale(3.0));
	}

	private static String fmt(double v) {
		return String.format(Locale.ROOT, "%.3f", v);
	}

	private void summonEntity() {
		String entityType = resolvedTypeId();
		if (entityType == null || Minecraft.getInstance().player == null) return;
		Vec3 at = lookPos();
		String cmd = "summon " + entityType + " " + fmt(at.x) + " " + fmt(at.y) + " " + fmt(at.z) + " " + buildEntityNbt();
		ClientPlayNetworking.send(new ExecuteCommandsPayload(List.of(cmd)));
	}

	private void copySummonCommand() {
		String entityType = resolvedTypeId();
		if (entityType == null) return;
		Vec3 at = lookPos();
		GiveCommands.copy("/summon " + entityType + " " + fmt(at.x) + " " + fmt(at.y) + " " + fmt(at.z) + " " + buildEntityNbt());
	}

	private SavedState captureState() {
		if (entityIdField == null) return null;
		List<String> keys = new ArrayList<>();
		List<String> values = new ArrayList<>();
		for (NbtFieldRow row : nbtFieldRows) {
			keys.add(row.getKey());
			values.add(row.getValue());
		}
		return new SavedState(entityIdField.getValue(), keys, values);
	}

	private void applyState(SavedState state) {
		if (state == null || entityIdField == null) return;
		entityIdField.setValue(state.entityId() == null ? "" : state.entityId());
		Map<String, String> byKey = new HashMap<>();
		List<String> keys = state.nbtKeys() == null ? List.of() : state.nbtKeys();
		List<String> values = state.nbtValues() == null ? List.of() : state.nbtValues();
		int n = Math.min(keys.size(), values.size());
		List<String[]> custom = new ArrayList<>();
		Set<String> known = new HashSet<>(List.of(DEFAULT_NBT_KEYS));
		String type = resolvedTypeId();
		if (type != null) known.addAll(List.of(ENTITY_EXTRA_KEYS.getOrDefault(type, new String[0])));
		for (int i = 0; i < n; i++) {
			String key = keys.get(i);
			String value = values.get(i);
			if (key == null || key.isEmpty()) continue;
			if ("CatType".equals(key)) key = "variant";
			if (known.contains(key)) byKey.put(key, value);
			else custom.add(new String[]{key, value});
		}
		lastExtraType = "";
		syncEntitySpecificFields();
		for (NbtFieldRow row : nbtFieldRows) {
			if (!row.isDefault) continue;
			String v = byKey.get(row.defaultKey);
			if (v == null) continue;
			if (row.checkBox != null) {
				boolean want = v.equals("true") || v.equals("1") || v.equals("1b");
				if (row.checkBox.selected() != want) row.checkBox.onPress();
			} else if (row.valueBox != null) {
				row.valueBox.setValue(v);
			}
		}
		nbtFieldRows.removeIf(row -> {
			if (row.isDefault) return false;
			dropRowWidgets(row);
			return true;
		});
		for (String[] pair : custom) {
			NbtFieldRow row = new NbtFieldRow(false, null);
			addRow(row);
			if (row.keyBox != null) row.keyBox.setValue(pair[0]);
			if (row.valueBox != null) row.valueBox.setValue(pair[1]);
		}
		repositionAll();
		updatePreviewEntity();
	}

	private void saveTemplate() {
		commitTemplateRename();
		SavedState state = captureState();
		EntityTemplateStore.Entry e = new EntityTemplateStore.Entry();
		e.name = defaultTemplateName(state);
		e.entityId = state.entityId();
		e.keys = new ArrayList<>(state.nbtKeys());
		e.values = new ArrayList<>(state.nbtValues());
		e.folder = TemplateOrg.norm(selectedFolder);
		EntityTemplateStore.add(e);
		selectedTemplate = EntityTemplateStore.all().size() - 1;
		updateTemplateScroll();
	}

	private void loadTemplate(int index) {
		EntityTemplateStore.Entry e = EntityTemplateStore.get(index);
		if (e == null) return;
		selectedTemplate = index;
		selectedFolder = TemplateOrg.norm(e.folder);
		applyState(new SavedState(e.entityId, e.keys, e.values));
	}

	private String defaultTemplateName(SavedState state) {
		String base = "";
		String id = resolvedTypeId();
		if (id != null) {
			ResourceLocation rl = SafeIds.tryParse(id);
			EntityType<?> type = rl != null ? BuiltInRegistries.ENTITY_TYPE.get(rl) : null;
			if (type != null) base = type.getDescription().getString();
		}
		if (base.isEmpty()) base = "实体";
		String name = base;
		int n = 2;
		while (templateNameTaken(name)) name = base + n++;
		return name;
	}

	private boolean templateNameTaken(String name) {
		String folder = TemplateOrg.norm(selectedFolder);
		for (EntityTemplateStore.Entry e : EntityTemplateStore.all()) {
			if (TemplateOrg.folderEq(e.folder, folder) && e.name != null && e.name.equals(name)) return true;
		}
		return false;
	}

	private void resetFields() {
		entityIdField.setValue("");
		for (NbtFieldRow row : List.copyOf(nbtFieldRows)) dropRowWidgets(row);
		nbtFieldRows.clear();
		lastExtraType = "";
		for (String key : DEFAULT_NBT_KEYS) addRow(new NbtFieldRow(true, key));
		savedState = null;
		sessionSelectedTemplate = -1;
		sessionSelectedFolder = "";
		selectedTemplate = -1;
		selectedFolder = "";
		scrollOffset = 0;
		previewEntity = null;
		previewEntityTypeId = null;
		lastPreviewSig = null;
		previewRotY = 0;
		previewRotX = 0;
		entitySuggestions = List.of();
		extraSuggestions = List.of();
		extraSuggestionBox = null;
		extraSuggestionIdx = -1;
		repositionAll();
	}

	@Override
	public void removed() {
		if (entityIdField != null) {
			savedState = captureState();
			sessionSelectedTemplate = selectedTemplate;
			sessionSelectedFolder = selectedFolder;
		}
		super.removed();
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		EditBox box = suggestionAnchor();
		if (box != null) {
			int count = suggestionCount();
			if (SuggestionPopup.maxScroll(count) > 0) {
				suggestionScroll = SuggestionPopup.scrollBy(suggestionScroll, count, scrollY);
				return true;
			}
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
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		super.renderBackground(g, mouseX, mouseY, partialTick);
		UiTheme.drawPanel(g, leftPos, topPos, WIDTH, HEIGHT);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		this.renderBackground(g, mouseX, mouseY, partialTick);
		UiTheme.drawHeader(g, font, this.title, leftPos, topPos, WIDTH);
		refreshX = UiTheme.headerIconX(leftPos, WIDTH);
		refreshY = UiTheme.headerIconY(topPos);
		boolean refreshHover = UiTheme.hitIcon(refreshX, refreshY, mouseX, mouseY);
		UiTheme.drawRefreshIcon(g, refreshX, refreshY, refreshHover);
		drawTemplateRail(g, mouseX, mouseY);
		int fy = topPos + HEIGHT - FOOTER_H;
		g.fill(leftPos + 1, fy, leftPos + WIDTH - 1, topPos + HEIGHT - 1, 0xFF191A1B);
		g.fill(leftPos + 1, fy, leftPos + WIDTH - 1, fy + 1, 0xFF2B2C2D);

		g.enableScissor(leftPos, topPos + CONTENT_TOP - 2, leftPos + RIGHT_X - 8, topPos + CONTENT_BOTTOM + 2);
		for (var child : this.children()) {
			if (child instanceof AbstractWidget w && !isChrome(w) && w.visible && !coversSuggestionList(w)) {
				w.render(g, mouseX, mouseY, partialTick);
			}
		}
		drawFormLabels(g);
		g.disableScissor();

		layoutPreview();
		UiTheme.drawPreviewFrame(g, previewX, previewY, previewW, previewH);
		renderPreviewEntity(g);
		if (copyBtn != null) copyBtn.render(g, mouseX, mouseY, partialTick);
		if (summonBtn != null) summonBtn.render(g, mouseX, mouseY, partialTick);
		if (saveBtn != null) saveBtn.render(g, mouseX, mouseY, partialTick);
		UiTheme.drawThinScrollBar(g, leftPos + RIGHT_X - 6, topPos + CONTENT_TOP, CONTENT_BOTTOM - CONTENT_TOP, scrollOffset, maxScroll);
		drawSuggestions(g, mouseX, mouseY);
		templateMenu.draw(g, font, mouseX, mouseY);
		if (refreshHover) {
			g.renderTooltip(font, Component.translatable("screen.bj_mapedit.reset"), mouseX, mouseY);
		}
	}

	private void drawFormLabels(GuiGraphics g) {
		if (entityIdField != null) {
			int y = entityIdField.getY() + 4;
			if (y + 10 > topPos + CONTENT_TOP && y < topPos + CONTENT_BOTTOM) {
				UiTheme.label(g, font, "实体", leftPos + LABEL_X, y);
			}
		}
		for (NbtFieldRow row : nbtFieldRows) {
			int wy = row.valueBox != null ? row.valueBox.getY() : row.checkBox != null ? row.checkBox.getY() : 0;
			if (wy + 10 <= topPos + CONTENT_TOP || wy >= topPos + CONTENT_BOTTOM) continue;
			if (!row.isDefault) {
				UiTheme.label(g, font, "自定义", leftPos + LABEL_X, wy + 4);
				continue;
			}
			String key = row.getKey();
			if (key.isEmpty()) continue;
			String cn = NBT_KEY_CN.getOrDefault(key, EXTRA_KEY_CN.getOrDefault(key, key));
			UiTheme.label(g, font, cn, leftPos + LABEL_X, wy + 4);
		}
	}

	private void renderPreviewEntity(GuiGraphics g) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) return;
		String entityTypeId = resolvedTypeId();
		if (entityTypeId == null) {
			g.drawCenteredString(font, entityIdField != null && entityIdField.getValue().isBlank() ? "输入实体以预览" : "无法预览",
				previewX + previewW / 2, previewY + previewH / 2 - 4, UiTheme.cMuted());
			return;
		}
		Entity entity = getPreviewEntity(entityTypeId);
		if (entity == null) {
			g.drawCenteredString(font, "无法预览", previewX + previewW / 2, previewY + previewH / 2 - 4, UiTheme.cMuted());
			return;
		}
		int cx = previewX + previewW / 2;
		int cy = previewY + previewH / 2 + 8;
		float scale = Math.max(28.0F, Math.min(previewW, previewH) * 0.38F);
		g.enableScissor(previewX + 1, previewY + 1, previewX + previewW - 1, previewY + previewH - 1);
		try {
			if (entity instanceof LivingEntity living) {
				living.yBodyRot = 180.0F + previewRotY;
				living.setYRot(180.0F + previewRotY);
				living.setXRot(-previewRotX);
				living.yHeadRot = living.getYRot();
				living.yHeadRotO = living.getYRot();
				Quaternionf cam = new Quaternionf().rotateX((float) Math.toRadians(previewRotX));
				Quaternionf pose = new Quaternionf().rotateZ((float) Math.PI);
				pose.mul(cam);
				InventoryScreen.renderEntityInInventory(
					g, cx, cy, scale,
					new Vector3f(0.0F, living.getBbHeight() / 2.0F, 0.0F),
					pose, cam, living
				);
			} else {
				Quaternionf quat = new Quaternionf().rotateZ((float) Math.PI);
				quat.mul(new Quaternionf().rotateY((float) Math.toRadians(180.0F + previewRotY)));
				quat.mul(new Quaternionf().rotateX((float) Math.toRadians(previewRotX)));
				g.pose().pushPose();
				g.pose().translate(cx, cy, 50);
				g.pose().scale(scale, scale, -scale);
				g.pose().translate(0, entity.getBbHeight() / 2.0F, 0);
				g.pose().mulPose(quat);
				Lighting.setupForEntityInInventory();
				mc.getEntityRenderDispatcher().setRenderShadow(false);
				mc.getEntityRenderDispatcher().render(entity, 0.0, 0.0, 0.0, 0.0F, 1.0F, g.pose(), g.bufferSource(), 15728880);
				g.flush();
				mc.getEntityRenderDispatcher().setRenderShadow(true);
				g.pose().popPose();
				Lighting.setupFor3DItems();
			}
		} catch (Throwable ignored) {
			g.drawCenteredString(font, "无法预览", previewX + previewW / 2, previewY + previewH / 2 - 4, UiTheme.cMuted());
		} finally {
			g.disableScissor();
			Lighting.setupFor3DItems();
		}
	}

	private Entity getPreviewEntity(String entityTypeId) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) return null;
		String sig = entityTypeId + "\n" + buildEntityNbt();
		if (previewEntity != null && sig.equals(lastPreviewSig)) return previewEntity;
		previewEntity = null;
		previewEntityTypeId = null;
		lastPreviewSig = sig;
		ResourceLocation id = SafeIds.tryParse(entityTypeId);
		if (id == null) return null;
		try {
			EntityType<?> type = entityTypeOf(id);
			if (type == null) return null;
			Entity created = type.create(mc.level);
			if (created == null) return null;
			created.setPos(0.0, 0.0, 0.0);
			applyPreviewLook(created);
			previewEntity = created;
			previewEntityTypeId = entityTypeId;
			return previewEntity;
		} catch (Throwable ignored) {
			previewEntity = null;
			previewEntityTypeId = null;
			lastPreviewSig = null;
			return null;
		}
	}

	private void applyPreviewNbt(Entity entity) {
		try {
			String snbt = buildEntityNbt();
			if (snbt == null || snbt.equals("{}")) return;
			CompoundTag tag = TagParser.parseTag(snbt);
			if (tag != null) entity.load(tag);
		} catch (Throwable ignored) {
		}
	}

	private void applyPreviewLook(Entity entity) {
		applyPreviewNbt(entity);
		if (collarColorId() < 0) return;
		if (entity instanceof TamableAnimal tame) {
			tame.setTame(true, false);
			var player = Minecraft.getInstance().player;
			if (player != null) tame.setOwnerUUID(player.getUUID());
		}
	}

	private int collarColorId() {
		for (NbtFieldRow row : nbtFieldRows) {
			if (!"CollarColor".equals(row.defaultKey)) continue;
			String nbt = EntityExtraLists.toNbtValue(resolvedTypeId(), "CollarColor", row.getValue());
			if (nbt == null) return -1;
			try {
				return Integer.parseInt(nbt);
			} catch (NumberFormatException ignored) {
				return -1;
			}
		}
		return -1;
	}

	private void updatePreviewEntity() {
		previewEntity = null;
		previewEntityTypeId = null;
		lastPreviewSig = null;
	}

	private void updateExtraSuggestions(NbtFieldRow row) {
		if (row == null || row.valueBox == null || !row.valueBox.isFocused() || !row.isDefault) {
			if (extraSuggestionBox == (row != null ? row.valueBox : extraSuggestionBox)) {
				extraSuggestions = List.of();
				extraSuggestionIdx = -1;
				extraSuggestionBox = null;
			}
			return;
		}
		extraSuggestionBox = row.valueBox;
		extraSuggestions = EntityExtraLists.filter(resolvedTypeId(), row.defaultKey, row.valueBox.getValue());
		extraSuggestionIdx = extraSuggestions.isEmpty() ? -1 : 0;
		suggestionScroll = SuggestionPopup.clampScroll(suggestionScroll, extraSuggestions.size());
	}

	private void refreshSuggestionsAfterClick(double mouseX, double mouseY) {
		if (SuggestionPopup.clickOpens(entityIdField, mouseX, mouseY)) {
			updateEntitySuggestions();
		} else {
			entitySuggestions = List.of();
			entitySuggestionIdx = -1;
		}
		NbtFieldRow extra = focusedExtraRow();
		if (extra != null && SuggestionPopup.clickOpens(extra.valueBox, mouseX, mouseY)) {
			updateExtraSuggestions(extra);
		} else {
			extraSuggestions = List.of();
			extraSuggestionIdx = -1;
			extraSuggestionBox = null;
		}
	}

	private NbtFieldRow focusedExtraRow() {
		for (NbtFieldRow row : nbtFieldRows) {
			if (row.isDefault && row.valueBox != null && row.valueBox.isFocused()) return row;
		}
		return null;
	}

	private boolean coversSuggestionList(AbstractWidget w) {
		EditBox anchor = suggestionAnchor();
		if (anchor == null || w == anchor) return false;
		int sx = anchor.getX();
		int sy = suggestionListY(anchor, suggestionCount());
		int sw = anchor.getWidth();
		int sh = SuggestionPopup.boxH(suggestionCount());
		return w.getX() < sx + sw && w.getX() + w.getWidth() > sx
			&& w.getY() < sy + sh && w.getY() + w.getHeight() > sy;
	}

	private EditBox suggestionAnchor() {
		if (!entitySuggestions.isEmpty() && entityIdField != null && entityIdField.isFocused()) return entityIdField;
		if (!extraSuggestions.isEmpty() && extraSuggestionBox != null && extraSuggestionBox.isFocused()) return extraSuggestionBox;
		return null;
	}

	private int suggestionCount() {
		if (suggestionAnchor() == entityIdField) return entitySuggestions.size();
		return extraSuggestions.size();
	}

	private int suggestionListY(EditBox box, int count) {
		if (box == null) return 0;
		return SuggestionPopup.listY(box, count, topPos + CONTENT_BOTTOM);
	}

	private boolean pressSuggestionBar(double mouseX, double mouseY) {
		EditBox box = suggestionAnchor();
		if (box == null) return false;
		int count = suggestionCount();
		Integer next = SuggestionPopup.pressBar(box.getX(), suggestionListY(box, count), box.getWidth(), count, suggestionScroll, mouseX, mouseY);
		if (next == null) return false;
		suggestionScroll = next;
		return true;
	}

	private void drawSuggestions(GuiGraphics g, int mouseX, int mouseY) {
		EditBox box = suggestionAnchor();
		if (box == null) return;
		int count = suggestionCount();
		int sx = box.getX();
		int sy = suggestionListY(box, count);
		int selected = box == entityIdField ? entitySuggestionIdx : extraSuggestionIdx;
		SuggestionPopup.draw(g, font, sx, sy, box.getWidth(), count, suggestionScroll, selected, mouseX, mouseY,
			i -> box == entityIdField ? entitySuggestions.get(i).label() : extraSuggestions.get(i).label());
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		draggingPreview = false;
		if (button == 0 && UiTheme.consumeRefreshClick(refreshX, refreshY, mouseX, mouseY, this::resetFields)) {
			return true;
		}
		if (handleTemplateMenuClick(mouseX, mouseY)) return true;
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
		}
		if (handleTemplateRailClick(mouseX, mouseY, button)) return true;
		if (button == 0 && pressSuggestionBar(mouseX, mouseY)) return true;
		if (button == 0 && tryEntitySuggestionClick(mouseX, mouseY)) return true;
		if (button == 0 && tryExtraSuggestionClick(mouseX, mouseY)) return true;
		if (button == 0 && mouseX >= previewX && mouseX < previewX + previewW
			&& mouseY >= previewY && mouseY < previewY + previewH) {
			draggingPreview = true;
			dragStartX = mouseX;
			dragStartY = mouseY;
			dragStartRotY = previewRotY;
			dragStartRotX = previewRotX;
			return true;
		}
		if (button == 0) {
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
		boolean result = super.mouseClicked(mouseX, mouseY, button);
		if (button == 0) refreshSuggestionsAfterClick(mouseX, mouseY);
		if (button == 0 && !result) setFocused(null);
		return result;
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

	private boolean tryEntitySuggestionClick(double mouseX, double mouseY) {
		if (entitySuggestions.isEmpty() || entityIdField == null || !entityIdField.isFocused()) return false;
		int sx = entityIdField.getX();
		int sy = suggestionListY(entityIdField, entitySuggestions.size());
		int idx = SuggestionPopup.hitIndex(mouseX, mouseY, sx, sy, entityIdField.getWidth(), entitySuggestions.size(), suggestionScroll);
		if (idx < 0) return false;
		EntitySuggestion pick = entitySuggestions.get(idx);
		entitySuggestions = List.of();
		entitySuggestionIdx = -1;
		entityIdField.setValue(pick.label());
		entitySuggestions = List.of();
		entitySuggestionIdx = -1;
		return true;
	}

	private boolean tryExtraSuggestionClick(double mouseX, double mouseY) {
		if (extraSuggestions.isEmpty() || extraSuggestionBox == null || !extraSuggestionBox.isFocused()) return false;
		int sx = extraSuggestionBox.getX();
		int sy = suggestionListY(extraSuggestionBox, extraSuggestions.size());
		int idx = SuggestionPopup.hitIndex(mouseX, mouseY, sx, sy, extraSuggestionBox.getWidth(), extraSuggestions.size(), suggestionScroll);
		if (idx < 0) return false;
		extraSuggestionBox.setValue(extraSuggestions.get(idx).label());
		extraSuggestions = List.of();
		extraSuggestionIdx = -1;
		return true;
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
		if (!entitySuggestions.isEmpty() && entityIdField != null && entityIdField.isFocused()) {
			if (keyCode == 258 || keyCode == 257) {
				entityIdField.setValue(entitySuggestions.get(Math.max(0, entitySuggestionIdx)).label());
				entitySuggestions = List.of();
				entitySuggestionIdx = -1;
				return true;
			}
			if (keyCode == 264) {
				entitySuggestionIdx = Math.min(entitySuggestionIdx + 1, entitySuggestions.size() - 1);
				suggestionScroll = SuggestionPopup.keepVisible(entitySuggestionIdx, suggestionScroll, entitySuggestions.size());
				return true;
			}
			if (keyCode == 265) {
				entitySuggestionIdx = Math.max(0, entitySuggestionIdx - 1);
				suggestionScroll = SuggestionPopup.keepVisible(entitySuggestionIdx, suggestionScroll, entitySuggestions.size());
				return true;
			}
			if (keyCode == 256) {
				entitySuggestions = List.of();
				entitySuggestionIdx = -1;
				return true;
			}
		}
		if (!extraSuggestions.isEmpty() && extraSuggestionBox != null && extraSuggestionBox.isFocused()) {
			if (keyCode == 258 || keyCode == 257) {
				extraSuggestionBox.setValue(extraSuggestions.get(Math.max(0, extraSuggestionIdx)).label());
				extraSuggestions = List.of();
				extraSuggestionIdx = -1;
				return true;
			}
			if (keyCode == 264) {
				extraSuggestionIdx = Math.min(extraSuggestionIdx + 1, extraSuggestions.size() - 1);
				suggestionScroll = SuggestionPopup.keepVisible(extraSuggestionIdx, suggestionScroll, extraSuggestions.size());
				return true;
			}
			if (keyCode == 265) {
				extraSuggestionIdx = Math.max(0, extraSuggestionIdx - 1);
				suggestionScroll = SuggestionPopup.keepVisible(extraSuggestionIdx, suggestionScroll, extraSuggestions.size());
				return true;
			}
			if (keyCode == 256) {
				extraSuggestions = List.of();
				extraSuggestionIdx = -1;
				return true;
			}
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private boolean inTemplateRail(double mouseX, double mouseY) {
		return mouseX >= templateRailX && mouseX < templateRailX + templateRailW
			&& mouseY >= topPos && mouseY < topPos + HEIGHT;
	}

	private int templateListTop() {
		return topPos + UiTheme.HEADER_H + 6;
	}

	private int templateListBottom() {
		return topPos + HEIGHT - 8;
	}

	private int templateListH() {
		return Math.max(0, templateListBottom() - templateListTop());
	}

	private List<TemplateOrg.Row> templateRows() {
		return EntityTemplateStore.rows(collapsedFolders);
	}

	private boolean isRenamingTemplate() {
		return renamingTemplate >= 0 || !renamingFolder.isEmpty();
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

	private int templateRowY(int index) {
		return templateListTop() + index * TEMPLATE_ROW_H - templateScroll;
	}

	private int templateIndexAt(double mouseY) {
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
		boolean hasSel = TemplateOrg.hasSel(selectedTemplate, selectedFolder);
		TemplateRailUi.drawHeader(g, font, Component.translatable("screen.bj_mapedit.entity_templates").getString(), x, w, topPos, hx, hy, hasSel);
		TemplateRailUi.drawRows(
			g, font, x, w, templateListTop(), templateListBottom(), TEMPLATE_ROW_H, templateScroll,
			templateRows(), selectedTemplate, selectedFolder, renamingTemplate, renamingFolder, templateRenameBox,
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
		if (UiTheme.hitScrollBar(templateScrollBarX(), templateListTop(), templateListH(), mouseX, mouseY)) return false;
		boolean hasSel = TemplateOrg.hasSel(selectedTemplate, selectedFolder);
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
			templateDrag.press(row.storeIndex, row.folderName, row.name, row.storeIndex == selectedTemplate, mouseX, mouseY);
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
			EntityTemplateStore.moveToFolder(index, templateDrag.hoverFolder);
			selectedFolder = TemplateOrg.norm(templateDrag.hoverFolder);
			if (!selectedFolder.isEmpty()) collapsedFolders.remove(selectedFolder);
			updateTemplateScroll();
		} else if (click) {
			loadTemplate(index);
		}
		templateDrag.reset();
		return true;
	}

	private void clearTemplateSelection() {
		selectedTemplate = -1;
		selectedFolder = "";
		sessionSelectedTemplate = -1;
		sessionSelectedFolder = "";
	}

	private void createTemplateFolder() {
		commitTemplateRename();
		String name = EntityTemplateStore.addFolder();
		collapsedFolders.remove(name);
		selectedFolder = name;
		selectedTemplate = -1;
		updateTemplateScroll();
	}

	private void clickTemplateFolder(String name) {
		String folder = TemplateOrg.norm(name);
		selectedTemplate = -1;
		selectedFolder = folder;
		if (!collapsedFolders.add(folder)) collapsedFolders.remove(folder);
	}

	private void deleteTemplateFolder(String name) {
		if (TemplateOrg.folderEq(renamingFolder, name)) cancelTemplateRename();
		else commitTemplateRename();
		EntityTemplateStore.Entry kept = selectedTemplate >= 0 ? EntityTemplateStore.get(selectedTemplate) : null;
		EntityTemplateStore.removeFolder(name);
		collapsedFolders.remove(TemplateOrg.norm(name));
		if (TemplateOrg.folderEq(selectedFolder, name)) {
			selectedFolder = "";
			selectedTemplate = -1;
		} else if (kept != null) {
			selectedTemplate = EntityTemplateStore.all().indexOf(kept);
		}
		updateTemplateScroll();
	}

	private void deleteTemplate(int index) {
		if (renamingTemplate == index) cancelTemplateRename();
		else commitTemplateRename();
		EntityTemplateStore.remove(index);
		if (selectedTemplate == index) selectedTemplate = -1;
		else if (selectedTemplate > index) selectedTemplate--;
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
	}

	private void startTemplateRename(int index) {
		EntityTemplateStore.Entry entry = EntityTemplateStore.get(index);
		if (entry == null || templateRenameBox == null) return;
		if (isRenamingTemplate() && renamingTemplate != index) commitTemplateRename();
		renamingFolder = "";
		renamingTemplate = index;
		templateRenameBox.setValue(entry.name == null ? "" : entry.name);
		templateRenameBox.visible = true;
		syncTemplateRenameBox();
		setFocused(templateRenameBox);
		templateRenameBox.setFocused(true);
		templateRenameBox.moveCursorToEnd(false);
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
			EntityTemplateStore.renameFolder(from, to);
			String normTo = TemplateOrg.norm(to);
			if (!normTo.isEmpty() && !TemplateOrg.folderEq(from, normTo)) {
				TemplateOrg.renameCollapsed(collapsedFolders, from, normTo);
				if (TemplateOrg.folderEq(selectedFolder, from)) selectedFolder = normTo;
			}
			cancelTemplateRename();
			return;
		}
		if (renamingTemplate < 0) return;
		EntityTemplateStore.rename(renamingTemplate, templateRenameBox.getValue());
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
}
