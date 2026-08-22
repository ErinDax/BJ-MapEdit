package cn.erindax.bjmapedit.client.screen;

import cn.erindax.bjmapedit.BjMapEdit;
import cn.erindax.bjmapedit.client.mixin.MultiLineEditBoxAccessor;
import cn.erindax.bjmapedit.client.widget.GiveCommands;
import cn.erindax.bjmapedit.client.widget.ItemTemplateStore;
import cn.erindax.bjmapedit.client.widget.SafeIds;
import cn.erindax.bjmapedit.client.widget.SuggestionPopup;
import cn.erindax.bjmapedit.client.widget.TemplateDrag;
import cn.erindax.bjmapedit.client.widget.TemplateOrg;
import cn.erindax.bjmapedit.client.widget.RenameDeleteMenu;
import cn.erindax.bjmapedit.client.widget.TemplateRailUi;
import cn.erindax.bjmapedit.client.widget.UiTheme;
import cn.erindax.bjmapedit.networking.payload.GiveItemPayload;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.*;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.advancements.critereon.BlockPredicate;
import net.minecraft.world.item.AdventureModePredicate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Environment(EnvType.CLIENT)
public class ItemEditorScreen extends Screen {

	private int WIDTH = 540;
	private int HEIGHT = 430;
	private static final int LABEL_X = 12;
	private int INPUT_X = 100;
	private int INPUT_W = 200;
	private int RIGHT_X = 340;
	private static final int ROW_H = 20;
	private static final int FOOTER_H = 30;
	private static final int TEMPLATE_ROW_H = 22;
	private int loreBoxH = 72;
	private int templateRailX = 8;
	private int templateRailW = 148;
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
	private int CONTENT_TOP = 30;
	private int CONTENT_BOTTOM;

	private EditBox itemField;
	private EditBox itemNameField;
	private AbstractWidget itemNameTextEditorBtn;
	private MultiLineEditBox loreBox;
	private AbstractWidget loreEditorBtn;
	private EditBox maxStackField;
	private EditBox maxDamageField;
	private List<EditBox> customDataLineBoxes = new ArrayList<>();
	private List<AbstractWidget> customDataRemoveBtns = new ArrayList<>();
	private AbstractWidget addCustomDataLineBtn;
	private EditBox customModelDataField;
	private EditBox canPlaceOnField;
	private EditBox enchantField;
	private EditBox attrField;

	private Checkbox unbreakableBox;
	private Checkbox hideEnchBox;
	private Checkbox hideAttrBox;
	private Checkbox hideUnbrBox;
	private Checkbox hideMiscBox;
	private boolean showMaxStack = true;
	private boolean showMaxDamage = true;
	private boolean showUnbreakable = true;
	private boolean showCanPlaceOn = true;
	private boolean showEnchant = true;
	private boolean showAttributes = true;

	private ItemStack previewStack = ItemStack.EMPTY;
	private Button giveBtn;
	private Button copyBtn;
	private Button saveBtn;
	private Button useHeldBtn;
	private int refreshX, refreshY;
	private int previewFrameH = 140;
	private int previewTipScroll;
	private int previewTipMaxScroll;

	private static SavedState savedState = null;

	private record SavedState(
		String item, String itemName, String lore,
		String maxStack, String maxDamage, String customData,
		String customModelData, String canPlaceOn,
		String enchant, String attr,
		boolean unbreakable, boolean hideEnch, boolean hideAttr,
		boolean hideUnbr, boolean hideMisc
	) {}

	private static final int[] MC_COLORS = {
		0xFF000000, 0xFF0000AA, 0xFF00AA00, 0xFF00AAAA,
		0xFFAA0000, 0xFFAA00AA, 0xFFFFAA00, 0xFFAAAAAA,
		0xFF555555, 0xFF5555FF, 0xFF55FF55, 0xFF55FFFF,
		0xFFFF5555, 0xFFFF55FF, 0xFFFFFF55, 0xFFFFFFFF
	};
	private static final char[] MC_COLOR_CODES = {
		'0', '1', '2', '3', '4', '5', '6', '7',
		'8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
	};

	private record ItemSuggestion(ResourceLocation id, String label) {}
	private record CdSuggestion(String key, String snbt, String label) {}
	private record EnchSuggestion(ResourceLocation id, String label) {}

	private List<ItemSuggestion> itemSuggestions = new ArrayList<>();
	private int itemSuggestionIdx = -1;
	private List<CdSuggestion> customDataSuggestions = List.of();
	private int cdSuggestionIdx = -1;
	private List<ResourceLocation> canPlaceOnSuggestions = new ArrayList<>();
	private int cpoSuggestionIdx = -1;
	private List<EnchSuggestion> enchantSuggestions = List.of();
	private int enchSuggestionIdx = -1;
	private int suggestionScroll;

	private int leftPos;
	private int topPos;
	private int scrollOffset;
	private int maxScroll;
	private boolean draggingScroll;
	private double scrollGrabOffset;
	private boolean restoring;
	private String pendingLoreText;
	private String pendingItemNameText;

	public ItemEditorScreen() {
		super(Component.translatable("screen.bj_mapedit.title"));
	}

	@Override
	protected void init() {
		super.init();
		layoutPanel();
		Font font = this.font;

		boolean wasReinit = itemField != null;
		String snapItem = wasReinit ? itemField.getValue() : null;
		String snapItemName = wasReinit ? itemNameField.getValue() : null;
		String snapLore = wasReinit ? loreBox.getValue() : null;
		String snapMaxStack = wasReinit ? maxStackField.getValue() : null;
		String snapMaxDamage = wasReinit ? maxDamageField.getValue() : null;
		List<String> snapCustomData = wasReinit ? new ArrayList<>(customDataLineBoxes.stream().map(EditBox::getValue).toList()) : null;
		String snapModelData = wasReinit ? customModelDataField.getValue() : null;
		String snapCanPlaceOn = wasReinit ? canPlaceOnField.getValue() : null;
		String snapEnchant = wasReinit ? enchantField.getValue() : null;
		String snapAttr = wasReinit ? attrField.getValue() : null;
		boolean snapUnbr = wasReinit && unbreakableBox.selected();
		boolean snapHideEnch = wasReinit && hideEnchBox.selected();
		boolean snapHideAttr = wasReinit && hideAttrBox.selected();
		boolean snapHideUnbr = wasReinit && hideUnbrBox.selected();
		boolean snapHideMisc = wasReinit && hideMiscBox.selected();
		int snapScroll = wasReinit ? scrollOffset : 0;

		itemField = new EditBox(font, leftPos + INPUT_X, 0, INPUT_W, 16,
			Component.translatable("screen.bj_mapedit.item"));
		itemField.setValue("diamond_sword");
		itemField.setResponder(s -> { updatePreview(); updateItemSuggestions(); });
		this.addRenderableWidget(itemField);

		itemNameField = new EditBox(font, leftPos + INPUT_X, 0, INPUT_W, 16,
			Component.translatable("screen.bj_mapedit.item_name"));
		itemNameField.setResponder(s -> updatePreview());
		this.addRenderableWidget(itemNameField);

		itemNameTextEditorBtn = Button.builder(Component.translatable("screen.bj_mapedit.edit_text"), b -> openItemNameTextEditor())
			.bounds(leftPos + INPUT_X, 0, 88, 16).build();
		this.addRenderableWidget(itemNameTextEditorBtn);

		loreBox = new MultiLineEditBox(font, leftPos + INPUT_X, 0, INPUT_W, loreBoxH,
			Component.empty(), Component.translatable("screen.bj_mapedit.lore"));
		loreBox.setCharacterLimit(32767);
		loreBox.setValueListener(s -> updatePreview());
		this.addRenderableWidget(loreBox);

		loreEditorBtn = Button.builder(Component.translatable("screen.bj_mapedit.edit_text"), b -> openLoreTextEditor())
			.bounds(leftPos + INPUT_X, 0, 88, 16).build();
		this.addRenderableWidget(loreEditorBtn);

		maxStackField = new EditBox(font, leftPos + INPUT_X, 0, INPUT_W, 16,
			Component.translatable("screen.bj_mapedit.max_stack_size"));
		maxStackField.setResponder(s -> updatePreview());
		this.addRenderableWidget(maxStackField);

		maxDamageField = new EditBox(font, leftPos + INPUT_X, 0, INPUT_W, 16,
			Component.translatable("screen.bj_mapedit.max_damage"));
		maxDamageField.setResponder(s -> updatePreview());
		this.addRenderableWidget(maxDamageField);

		clearCustomDataLines();
		createCustomDataLine("");

		addCustomDataLineBtn = Button.builder(Component.literal("+"), b -> addCustomDataLine())
			.bounds(leftPos + INPUT_X, 0, 20, 16).build();
		this.addRenderableWidget(addCustomDataLineBtn);

		customModelDataField = new EditBox(font, leftPos + INPUT_X, 0, INPUT_W, 16,
			Component.translatable("screen.bj_mapedit.custom_model_data"));
		customModelDataField.setResponder(s -> updatePreview());
		this.addRenderableWidget(customModelDataField);

		canPlaceOnField = new EditBox(font, leftPos + INPUT_X, 0, INPUT_W, 16,
			Component.translatable("screen.bj_mapedit.can_place_on"));
		canPlaceOnField.setResponder(s -> { updatePreview(); updateCanPlaceOnSuggestions(); });
		this.addRenderableWidget(canPlaceOnField);

		enchantField = new EditBox(font, leftPos + INPUT_X, 0, INPUT_W, 16,
			Component.translatable("screen.bj_mapedit.enchantments_hint"));
		enchantField.setResponder(s -> { updatePreview(); updateEnchantSuggestions(); });
		this.addRenderableWidget(enchantField);

		attrField = new EditBox(font, leftPos + INPUT_X, 0, INPUT_W, 16,
			Component.translatable("screen.bj_mapedit.attributes_hint"));
		attrField.setResponder(s -> updatePreview());
		this.addRenderableWidget(attrField);

		unbreakableBox = Checkbox.builder(Component.translatable("screen.bj_mapedit.unbreakable"), font)
			.pos(leftPos + LABEL_X, 0)
			.onValueChange((box, val) -> updatePreview())
			.build();
		this.addRenderableWidget(unbreakableBox);

		hideEnchBox = Checkbox.builder(Component.translatable("screen.bj_mapedit.hide_enchantments"), font)
			.pos(leftPos + LABEL_X, 0)
			.onValueChange((box, val) -> updatePreview())
			.build();
		this.addRenderableWidget(hideEnchBox);

		hideAttrBox = Checkbox.builder(Component.translatable("screen.bj_mapedit.hide_attributes"), font)
			.pos(leftPos + LABEL_X, 0)
			.onValueChange((box, val) -> updatePreview())
			.build();
		this.addRenderableWidget(hideAttrBox);

		hideUnbrBox = Checkbox.builder(Component.translatable("screen.bj_mapedit.hide_unbreakable"), font)
			.pos(leftPos + LABEL_X, 0)
			.onValueChange((box, val) -> updatePreview())
			.build();
		this.addRenderableWidget(hideUnbrBox);

		hideMiscBox = Checkbox.builder(Component.translatable("screen.bj_mapedit.hide_misc"), font)
			.pos(leftPos + LABEL_X, 0)
			.onValueChange((box, val) -> updatePreview())
			.build();
		this.addRenderableWidget(hideMiscBox);

		repositionAllWidgets();

		giveBtn = this.addRenderableWidget(Button.builder(
			Component.translatable("screen.bj_mapedit.give"),
			b -> giveItem()
		).bounds(leftPos + RIGHT_X, topPos + 132, 132, 20).build());

		copyBtn = this.addRenderableWidget(Button.builder(
			Component.translatable("screen.bj_mapedit.copy"),
			b -> copyCommand()
		).bounds(leftPos + RIGHT_X, topPos + 156, 132, 20).build());

		saveBtn = this.addRenderableWidget(Button.builder(
			Component.translatable("screen.bj_mapedit.save_template"),
			b -> saveTemplate()
		).bounds(leftPos + RIGHT_X, topPos + 180, 132, 20).build());

		useHeldBtn = this.addRenderableWidget(Button.builder(
			Component.translatable("screen.bj_mapedit.use_held"),
			b -> useHeldItem()
		).bounds(leftPos + RIGHT_X, topPos + 228, 132, 20).build());

		templateRenameBox = new EditBox(font, 0, -1000, 80, 16, Component.empty());
		templateRenameBox.setMaxLength(24);
		templateRenameBox.visible = false;
		this.addRenderableWidget(templateRenameBox);
		layoutActionButtons();
		layoutFlagBoxes();

		if (wasReinit) {
			itemField.setValue(snapItem);
			itemNameField.setValue(snapItemName);
			if (snapLore != null) {
				setLoreValue(snapLore);
			}
			maxStackField.setValue(snapMaxStack);
			maxDamageField.setValue(snapMaxDamage);
			if (snapCustomData != null && !snapCustomData.isEmpty()) {
				clearCustomDataLines();
				for (String line : snapCustomData) createCustomDataLine(line);
			}
			customModelDataField.setValue(snapModelData);
			canPlaceOnField.setValue(snapCanPlaceOn);
			enchantField.setValue(snapEnchant);
			attrField.setValue(snapAttr);
			setCheckedIfNeeded(unbreakableBox, snapUnbr);
			setCheckedIfNeeded(hideEnchBox, snapHideEnch);
			setCheckedIfNeeded(hideAttrBox, snapHideAttr);
			setCheckedIfNeeded(hideUnbrBox, snapHideUnbr);
			setCheckedIfNeeded(hideMiscBox, snapHideMisc);
			scrollOffset = snapScroll;
		} else if (savedState != null) {
			restoreState();
			selectedTemplate = sessionSelectedTemplate;
			selectedFolder = sessionSelectedFolder;
		}

		if (pendingItemNameText != null) {
			itemNameField.setValue(pendingItemNameText);
			pendingItemNameText = null;
		}
		if (pendingLoreText != null) {
			setLoreValue(pendingLoreText);
			pendingLoreText = null;
		}

		updatePreview();
		repositionAllWidgets();
	}

	@Override
	public void added() {
		super.added();
		if (pendingItemNameText != null) {
			itemNameField.setValue(pendingItemNameText);
			pendingItemNameText = null;
		}
		if (pendingLoreText != null) {
			setLoreValue(pendingLoreText);
			pendingLoreText = null;
			repositionAllWidgets();
			updatePreview();
		}
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
		CONTENT_TOP = UiTheme.HEADER_H + 8;
		CONTENT_BOTTOM = HEIGHT - FOOTER_H - 4;
		int labelW = 56;
		if (this.font != null) {
			labelW = Math.max(labelW, this.font.width("自定义数据") + 10);
			labelW = Math.max(labelW, this.font.width("可放置方块") + 10);
		}
		INPUT_X = LABEL_X + labelW;
		int rightW = Math.max(96, (WIDTH - 20) * 36 / 100);
		if (WIDTH < 420) rightW = Math.max(84, WIDTH * 32 / 100);
		RIGHT_X = Math.max(INPUT_X + 56, WIDTH - rightW - 8);
		INPUT_W = Math.max(48, RIGHT_X - INPUT_X - 14);
		loreBoxH = Mth.clamp(HEIGHT * 16 / 100, 48, 140);
		updateTemplateScroll();
		layoutRefreshIcon();
	}

	private int templateRailX() {
		return templateRailX;
	}

	private void repositionAllWidgets() {
		if (itemField == null || loreBox == null || addCustomDataLineBtn == null) return;
		int y = topPos + CONTENT_TOP + 2 - scrollOffset;
		int totalHeight = 0;

		placeLeft(itemField, y); y += ROW_H; totalHeight += ROW_H;
		placeLeft(itemNameField, y); y += ROW_H; totalHeight += ROW_H;
		itemNameTextEditorBtn.setX(leftPos + INPUT_X); itemNameTextEditorBtn.setY(y); y += ROW_H; totalHeight += ROW_H;
		loreBox.setX(leftPos + INPUT_X);
		loreBox.setY(y);
		loreBox.setWidth(INPUT_W);
		loreBox.setHeight(loreBoxH);
		y += loreBoxH + 4; totalHeight += loreBoxH + 4;
		loreEditorBtn.setX(leftPos + INPUT_X);
		loreEditorBtn.setY(y); y += ROW_H; totalHeight += ROW_H;
		placeOptional(maxStackField, showMaxStack, y); y += ROW_H; totalHeight += ROW_H;
		placeOptional(maxDamageField, showMaxDamage, y); y += ROW_H; totalHeight += ROW_H;
		for (int i = 0; i < customDataLineBoxes.size(); i++) {
			customDataLineBoxes.get(i).setX(leftPos + INPUT_X);
			boolean cdMinus = customDataLineBoxes.size() > 1;
			customDataLineBoxes.get(i).setWidth(cdMinus ? INPUT_W - 20 : INPUT_W);
			customDataLineBoxes.get(i).setY(y);
			if (i < customDataRemoveBtns.size()) {
				customDataRemoveBtns.get(i).setX(leftPos + INPUT_X + INPUT_W - 18);
				customDataRemoveBtns.get(i).setY(y);
				customDataRemoveBtns.get(i).visible = customDataLineBoxes.size() > 1;
			}
			y += ROW_H; totalHeight += ROW_H;
		}
		addCustomDataLineBtn.setX(leftPos + INPUT_X); addCustomDataLineBtn.setY(y); y += ROW_H; totalHeight += ROW_H;
		placeLeft(customModelDataField, y); y += ROW_H; totalHeight += ROW_H;
		placeOptional(canPlaceOnField, showCanPlaceOn, y); y += ROW_H; totalHeight += ROW_H;
		placeOptional(enchantField, showEnchant, y); y += ROW_H; totalHeight += ROW_H;
		placeOptional(attrField, showAttributes, y); y += ROW_H; totalHeight += ROW_H;


		maxScroll = Math.max(0, totalHeight + 8 - (CONTENT_BOTTOM - CONTENT_TOP));
		scrollOffset = Mth.clamp(scrollOffset, 0, maxScroll);
	}


	private void placeLeft(net.minecraft.client.gui.components.AbstractWidget w, int y) {
		if (w == null) return;
		w.setX(leftPos + INPUT_X);
		w.setY(y);
		w.setWidth(INPUT_W);
	}

	private void placeOptional(net.minecraft.client.gui.components.AbstractWidget w, boolean show, int y) {
		if (w == null) return;
		w.visible = show;
		if (show) { w.setX(leftPos + INPUT_X); w.setWidth(INPUT_W); }
		w.setY(show ? y : -1000);
	}

	private void refreshFieldVisibility(ItemStack stack) {
		Item item = stack.getItem();
		boolean damageable = stack.getMaxDamage() > 0;
		showMaxStack = true;
		showMaxDamage = damageable;
		showUnbreakable = damageable;
		showCanPlaceOn = item instanceof net.minecraft.world.item.BlockItem;
		boolean equipment = item instanceof net.minecraft.world.item.ArmorItem
			|| item instanceof net.minecraft.world.item.DiggerItem
			|| item instanceof net.minecraft.world.item.SwordItem
			|| item instanceof net.minecraft.world.item.ProjectileWeaponItem
			|| item instanceof net.minecraft.world.item.TridentItem;
		showEnchant = damageable || equipment || item instanceof net.minecraft.world.item.EnchantedBookItem;
		showAttributes = equipment || damageable;
		if (maxStackField != null) maxStackField.visible = showMaxStack;
		if (maxDamageField != null) maxDamageField.visible = showMaxDamage;
		if (unbreakableBox != null) unbreakableBox.visible = true;
		if (canPlaceOnField != null) canPlaceOnField.visible = showCanPlaceOn;
		if (enchantField != null) enchantField.visible = showEnchant;
		if (attrField != null) attrField.visible = showAttributes;
		if (hideEnchBox != null) hideEnchBox.visible = true;
		if (hideAttrBox != null) hideAttrBox.visible = true;
		if (hideUnbrBox != null) hideUnbrBox.visible = true;
		if (hideMiscBox != null) hideMiscBox.visible = true;
	}

	private void updatePreview() {
		if (restoring || itemField == null) return;
		boolean prevDmg = showMaxDamage;
		boolean prevPlace = showCanPlaceOn;
		boolean prevEnch = showEnchant;
		boolean prevAttr = showAttributes;
		boolean prevUnbr = showUnbreakable;
		try {
			previewStack = buildItemStack();
		} catch (Exception ignored) {
			previewStack = ItemStack.EMPTY;
		}
		if (prevDmg != showMaxDamage || prevPlace != showCanPlaceOn || prevEnch != showEnchant
			|| prevAttr != showAttributes || prevUnbr != showUnbreakable) {
			repositionAllWidgets();
		}
	}

	private ItemStack buildItemStack() {
		ResourceLocation itemId = SafeIds.tryParseItem(itemField.getValue());
		if (itemId == null) return previewStack;
		Optional<Holder.Reference<Item>> itemHolder = BuiltInRegistries.ITEM.getHolder(itemId);
		if (itemHolder.isEmpty()) return previewStack;
		ItemStack stack = new ItemStack(itemHolder.get());


		Item item = stack.getItem();

		refreshFieldVisibility(stack);

		String itemNameText = itemNameField.getValue().trim();
		if (!itemNameText.isEmpty()) {
			Component nameComp = parseLoreSectionCodes(itemNameText, Style.EMPTY);
			stack.set(DataComponents.CUSTOM_NAME, nameComp);
		}

		String loreText = loreBox.getValue();
		String[] loreLineTexts = loreText.split("\n", -1);
		boolean anyLore = false;
		for (String line : loreLineTexts) {
			if (!line.trim().isEmpty()) { anyLore = true; break; }
		}
		if (anyLore) {
			List<Component> lines = new ArrayList<>();
			Style baseLoreStyle = Style.EMPTY;
			for (String line : loreLineTexts) {
				if (!line.trim().isEmpty()) {
					lines.add(parseLoreSectionCodes(line, baseLoreStyle));
				} else {
					lines.add(Component.empty());
				}
			}
			stack.set(DataComponents.LORE, new ItemLore(lines));
		}

		String maxStackText = maxStackField.getValue().trim();
		if (!maxStackText.isEmpty()) {
			try {
				int val = Integer.parseInt(maxStackText);
				if (val >= 1 && val <= 99) {
					stack.set(DataComponents.MAX_STACK_SIZE, val);
				}
			} catch (NumberFormatException ignored) {}
		}

		String maxDamageText = maxDamageField.getValue().trim();
		if (!maxDamageText.isEmpty() && stack.getMaxDamage() > 0) {
			try {
				int val = Integer.parseInt(maxDamageText);
				if (val > 0) {
					stack.set(DataComponents.MAX_DAMAGE, val);
				}
			} catch (NumberFormatException ignored) {}
		}

		CompoundTag mergedCustomData = new CompoundTag();
		for (EditBox box : customDataLineBoxes) {
			CompoundTag tag = parseCustomDataLine(box.getValue());
			if (tag != null && !tag.isEmpty()) {
				mergedCustomData = mergedCustomData.merge(tag);
			}
		}
		if (!mergedCustomData.isEmpty()) {
			stack.set(DataComponents.CUSTOM_DATA, CustomData.of(mergedCustomData));
		}

		String modelDataText = customModelDataField.getValue().trim();
		if (!modelDataText.isEmpty()) {
			try {
				int val = Integer.parseInt(modelDataText);
				stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(val));
			} catch (NumberFormatException ignored) {}
		}

		String canPlaceOnText = canPlaceOnField.getValue().trim();
		if (!canPlaceOnText.isEmpty()) {
			List<BlockPredicate> predicates = new ArrayList<>();
			for (String part : canPlaceOnText.split(",")) {
				ResourceLocation blockId = ResourceLocation.tryParse(part.trim());
				if (blockId != null) {
					BuiltInRegistries.BLOCK.getHolder(ResourceKey.create(Registries.BLOCK, blockId))
						.ifPresent(holder -> {
							HolderSet<Block> holderSet = HolderSet.direct(holder);
							predicates.add(new BlockPredicate(Optional.of(holderSet), Optional.empty(), Optional.empty()));
						});
				}
			}
			if (!predicates.isEmpty()) {
				stack.set(DataComponents.CAN_PLACE_ON, new AdventureModePredicate(predicates, true));
			}
		}

		if (unbreakableBox.selected()) {
			stack.set(DataComponents.UNBREAKABLE, new Unbreakable(!hideUnbrBox.selected()));
		}

		String enchantText = enchantField.getValue().trim();
		if (!enchantText.isEmpty() && this.minecraft.level != null) {
			Optional<HolderLookup.RegistryLookup<Enchantment>> enchReg = this.minecraft.level.registryAccess().lookup(Registries.ENCHANTMENT);
			if (enchReg.isPresent()) {
				ItemEnchantments.Mutable enchBuilder = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
				for (String part : enchantText.split(",")) {
					ResourceLocation enchId = parseEnchId(part);
					if (enchId == null) continue;
					Optional<Holder.Reference<Enchantment>> enchHolder = enchReg.get()
						.get(ResourceKey.create(Registries.ENCHANTMENT, enchId));
					if (enchHolder.isEmpty()) continue;
					int level = parseEnchLevel(part);
					if (level > 0 && level <= 255) {
						enchBuilder.set(enchHolder.get(), level);
					}
				}
				ItemEnchantments ench = enchBuilder.toImmutable();
				if (!ench.isEmpty()) {
					stack.set(DataComponents.ENCHANTMENTS, hideEnchBox.selected() ? ench.withTooltip(false) : ench);
				}
			}
		}

		String attrText = attrField.getValue().trim();
		if (!attrText.isEmpty()) {
			ItemAttributeModifiers.Builder attrBuilder = ItemAttributeModifiers.builder();
			for (String part : attrText.split(";")) {
				String[] segs = part.trim().split(":");
				if (segs.length >= 3) {
					ResourceLocation attrId = ResourceLocation.tryParse(segs[0].trim());
					if (attrId != null) {
						Optional<Holder.Reference<Attribute>> attrHolder = BuiltInRegistries.ATTRIBUTE.getHolder(
							ResourceKey.create(Registries.ATTRIBUTE, attrId));
						if (attrHolder.isPresent()) {
							try {
								double amount = Double.parseDouble(segs[1].trim());
								AttributeModifier.Operation op = AttributeModifier.Operation.ADD_VALUE;
								if (segs.length >= 3) {
									String opStr = segs[2].trim().toLowerCase(Locale.ROOT);
									if (opStr.contains("multiplied_base") || opStr.contains("multiply_base")) {
										op = AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
									} else if (opStr.contains("multiplied_total") || opStr.contains("multiply_total")) {
										op = AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
									}
								}
								EquipmentSlotGroup slot = EquipmentSlotGroup.ANY;
								if (segs.length >= 4) {
									String slotStr = segs[3].trim().toLowerCase(Locale.ROOT);
									slot = switch (slotStr) {
										case "mainhand" -> EquipmentSlotGroup.MAINHAND;
										case "offhand" -> EquipmentSlotGroup.OFFHAND;
										case "feet" -> EquipmentSlotGroup.FEET;
										case "legs" -> EquipmentSlotGroup.LEGS;
										case "chest" -> EquipmentSlotGroup.CHEST;
										case "head" -> EquipmentSlotGroup.HEAD;
										default -> EquipmentSlotGroup.ANY;
									};
								}
								ResourceLocation modifierId = BjMapEdit.id("custom_attr_" +
									attrHolder.get().value().getDescriptionId().replaceAll("[^a-zA-Z0-9_.-]", "_"));
								AttributeModifier modifier = new AttributeModifier(modifierId, amount, op);
								attrBuilder.add(attrHolder.get(), modifier, slot);
							} catch (NumberFormatException ignored) {}
						}
					}
				}
			}
			ItemAttributeModifiers mods = attrBuilder.build();
			if (!mods.modifiers().isEmpty()) {
				stack.set(DataComponents.ATTRIBUTE_MODIFIERS, hideAttrBox.selected() ? mods.withTooltip(false) : mods);
			}
		}

		if (hideMiscBox.selected()) {
			stack.set(DataComponents.HIDE_ADDITIONAL_TOOLTIP, Unit.INSTANCE);
		}

		return stack;
	}

	private void giveItem() {
		ItemStack stack = buildItemStack();
		if (!stack.isEmpty()) {
			ClientPlayNetworking.send(new GiveItemPayload(stack));
		}
	}

	private void useHeldItem() {
		if (Minecraft.getInstance().player == null) return;
		ItemStack held = Minecraft.getInstance().player.getMainHandItem();
		if (held.isEmpty()) {
			Minecraft.getInstance().player.displayClientMessage(Component.literal("主手没有物品"), true);
			return;
		}
		restoring = true;

		itemField.setValue(BuiltInRegistries.ITEM.getKey(held.getItem()).toString());

		Component customName = held.get(DataComponents.CUSTOM_NAME);
		itemNameField.setValue(customName != null ? componentToSectionText(customName) : "");

		ItemLore lore = held.get(DataComponents.LORE);
		if (lore != null && !lore.lines().isEmpty()) {
			List<String> heldLore = new ArrayList<>();
			for (Component line : lore.lines()) heldLore.add(componentToSectionText(line));
			setLoreValue(String.join("\n", heldLore));
		} else {
			setLoreValue("");
		}

		Integer maxStack = held.get(DataComponents.MAX_STACK_SIZE);
		maxStackField.setValue(maxStack != null ? String.valueOf(maxStack) : "");
		maxDamageField.setValue(held.getMaxDamage() > 0 ? String.valueOf(held.getMaxDamage()) : "");

		CustomData cd = held.get(DataComponents.CUSTOM_DATA);
		clearCustomDataLines();
		EditBox cdBox = createCustomDataLine(cd != null && !cd.copyTag().isEmpty() ? cd.copyTag().toString() : "");

		CustomModelData cmd = held.get(DataComponents.CUSTOM_MODEL_DATA);
		customModelDataField.setValue(cmd != null ? String.valueOf(cmd.value()) : "");

		AdventureModePredicate cpo = held.get(DataComponents.CAN_PLACE_ON);
		if (cpo != null) {
			List<String> ids = new ArrayList<>();
			try {
				java.lang.reflect.Field f = cpo.getClass().getDeclaredField("predicates");
				f.setAccessible(true);
				Object preds = f.get(cpo);
				if (preds instanceof List<?> list) {
					for (Object o : list) {
						if (o instanceof BlockPredicate bp && bp.blocks().isPresent()) {
							for (Holder<Block> h : bp.blocks().get()) {
								ResourceLocation blkId = BuiltInRegistries.BLOCK.getKey(h.value());
								if (blkId != null) ids.add(blkId.toString());
							}
						}
					}
				}
			} catch (Exception ignored) {}
			canPlaceOnField.setValue(String.join(",", ids));
		} else {
			canPlaceOnField.setValue("");
		}

		ItemEnchantments ench = held.get(DataComponents.ENCHANTMENTS);
		if (ench != null && !ench.isEmpty()) {
			List<String> parts = new ArrayList<>();
			for (var e : ench.entrySet()) {
				ResourceLocation enchId = e.getKey().unwrapKey().map(rk -> rk.location()).orElse(null);
				if (enchId != null) parts.add(enchId + ":" + e.getValue());
			}
			enchantField.setValue(String.join(",", parts));
		} else {
			enchantField.setValue("");
		}

		ItemAttributeModifiers attrs = held.get(DataComponents.ATTRIBUTE_MODIFIERS);
		if (attrs != null && !attrs.modifiers().isEmpty()) {
			List<String> parts = new ArrayList<>();
			for (ItemAttributeModifiers.Entry e : attrs.modifiers()) {
				ResourceLocation attrId = e.attribute().unwrapKey().map(rk -> rk.location()).orElse(null);
				if (attrId == null) continue;
				AttributeModifier mod = e.modifier();
				String opStr = switch (mod.operation()) {
					case ADD_VALUE -> "add_value";
					case ADD_MULTIPLIED_BASE -> "multiplied_base";
					case ADD_MULTIPLIED_TOTAL -> "multiplied_total";
				};
				parts.add(attrId + ":" + mod.amount() + ":" + opStr
					+ ":" + e.slot().name().toLowerCase(Locale.ROOT));
			}
			attrField.setValue(String.join(";", parts));
		} else {
			attrField.setValue("");
		}

		Unbreakable unbr = held.get(DataComponents.UNBREAKABLE);
		setCheckedIfNeeded(unbreakableBox, unbr != null);
		setCheckedIfNeeded(hideUnbrBox, unbr != null && !unbr.showInTooltip());
		boolean hasEnch = ench != null && !ench.isEmpty();
		setCheckedIfNeeded(hideEnchBox, hasEnch && !reflectShowInTooltip(ench));
		boolean hasAttr = attrs != null && !attrs.modifiers().isEmpty();
		setCheckedIfNeeded(hideAttrBox, hasAttr && !attrs.showInTooltip());
		setCheckedIfNeeded(hideMiscBox, held.has(DataComponents.HIDE_ADDITIONAL_TOOLTIP));

		restoring = false;
		repositionAllWidgets();
		updatePreview();
	}

	private String componentToSectionText(Component comp) {
		StringBuilder sb = new StringBuilder();
		comp.visit((style, text) -> {
			if (!text.isEmpty()) {
				sb.append(styleToSection(style)).append(text);
			}
			return java.util.Optional.empty();
		}, Style.EMPTY);
		return sb.toString();
	}

	private String styleToSection(Style style) {
		StringBuilder sb = new StringBuilder();
		TextColor color = style.getColor();
		if (color != null) {
			int val = color.getValue();
			char code = nearestMcColorCode(val);
			if (code != '\0') {
				sb.append('\u00a7').append(code);
			} else {
				sb.append("\u00a7x");
				String hex = String.format("%06x", val);
				for (char c : hex.toCharArray()) sb.append('\u00a7').append(c);
			}
		}
		if (style.isBold()) sb.append("\u00a7l");
		if (style.isItalic()) sb.append("\u00a7o");
		if (style.isUnderlined()) sb.append("\u00a7n");
		if (style.isStrikethrough()) sb.append("\u00a7m");
		if (style.isObfuscated()) sb.append("\u00a7k");
		return sb.toString();
	}

	private static boolean reflectShowInTooltip(Object obj) {
		if (obj == null) return true;
		try {
			java.lang.reflect.Field f = obj.getClass().getDeclaredField("showInTooltip");
			f.setAccessible(true);
			return (boolean) f.get(obj);
		} catch (Exception ignored) {}
		return true;
	}

	private char nearestMcColorCode(int rgb) {
		int best = -1;
		int bestDist = Integer.MAX_VALUE;
		for (int i = 0; i < MC_COLORS.length; i++) {
			int c = MC_COLORS[i] & 0xFFFFFF;
			int dr = ((c >> 16) & 0xFF) - ((rgb >> 16) & 0xFF);
			int dg = ((c >> 8) & 0xFF) - ((rgb >> 8) & 0xFF);
			int db = (c & 0xFF) - (rgb & 0xFF);
			int dist = dr * dr + dg * dg + db * db;
			if (dist < bestDist) {
				bestDist = dist;
				best = i;
			}
		}
		return best >= 0 ? MC_COLOR_CODES[best] : '\0';
	}

	private void copyCommand() {
		ItemStack stack = buildItemStack();
		if (stack.isEmpty()) return;
		GiveCommands.copy(GiveCommands.give(stack));
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		AbstractWidget anchor = suggestionAnchor();
		if (anchor != null) {
			int total = suggestionCount(anchor);
			if (SuggestionPopup.maxScroll(total) > 0) {
				suggestionScroll = SuggestionPopup.scrollBy(suggestionScroll, total, scrollY);
				return true;
			}
		}
		int previewX = leftPos + RIGHT_X;
		int previewY = topPos + CONTENT_TOP;
		int previewW = Math.max(64, WIDTH - RIGHT_X - 8);
		if (previewTipMaxScroll > 0
			&& mouseX >= previewX && mouseX < previewX + previewW
			&& mouseY >= previewY && mouseY < previewY + previewFrameH) {
			previewTipScroll = Mth.clamp(previewTipScroll - (int) (scrollY * 10), 0, previewTipMaxScroll);
			return true;
		}
		if (inTemplateRail(mouseX, mouseY)) {
			if (templateMaxScroll > 0) {
				templateScroll = Mth.clamp(templateScroll - (int) (scrollY * 20), 0, templateMaxScroll);
				syncTemplateRenameBox();
			}
			return true;
		}
		if (loreBox != null && loreBox.isMouseOver(mouseX, mouseY)
			&& loreBox.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
			return true;
		}
		if (mouseX >= leftPos && mouseX < leftPos + INPUT_X + INPUT_W + 12
			&& mouseY >= topPos + CONTENT_TOP && mouseY < topPos + CONTENT_BOTTOM) {
			scrollOffset = Mth.clamp(scrollOffset - (int)(scrollY * 20), 0, maxScroll);
			repositionAllWidgets();
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
		layoutRefreshIcon();
		boolean refreshHover = UiTheme.hitIcon(refreshX, refreshY, mouseX, mouseY);
		UiTheme.drawRefreshIcon(g, refreshX, refreshY, refreshHover);
		drawTemplateRail(g, mouseX, mouseY);
		int fy = topPos + HEIGHT - FOOTER_H;
		g.fill(leftPos + 1, fy, leftPos + WIDTH - 1, topPos + HEIGHT - 1, 0xFF191A1B);
		g.fill(leftPos + 1, fy, leftPos + WIDTH - 1, fy + 1, 0xFF2B2C2D);

		g.enableScissor(leftPos, topPos + CONTENT_TOP - 2, leftPos + RIGHT_X - 8, topPos + CONTENT_BOTTOM + 2);
		for (var child : this.children()) {
			if (child instanceof AbstractWidget w && w != templateRenameBox && w.getY() < topPos + HEIGHT - FOOTER_H && w.getX() < leftPos + RIGHT_X - 8 && !coversSuggestionList(w)) {
				if (w == loreBox) {
					UiTheme.drawInputWell(g, w.getX(), w.getY(), w.getWidth(), w.getHeight(), w.isFocused());
				}
				w.render(g, mouseX, mouseY, partialTick);
					if (w == loreBox) UiTheme.strokeInputWell(g, w.getX(), w.getY(), w.getWidth(), w.getHeight(), w.isFocused());
			}
		}
		g.disableScissor();
		for (var child : this.children()) {
			if (child instanceof AbstractWidget w && w != templateRenameBox
				&& (w.getY() >= topPos + HEIGHT - FOOTER_H || w.getX() >= leftPos + RIGHT_X - 8)) {
				w.render(g, mouseX, mouseY, partialTick);
			}
		}

		drawLabel(g, itemField.getY(), "物品");
		drawLabel(g, itemNameField.getY(), "物品名称");
		if (loreBox != null) {
			drawLabel(g, loreBox.getY(), "简介");
		}
		if (showMaxStack) {
			drawLabel(g, maxStackField.getY(), "最大堆叠");
		}
		if (showMaxDamage) {
			drawLabel(g, maxDamageField.getY(), "最大耐久");
		}
		if (!customDataLineBoxes.isEmpty()) {
			drawLabel(g, customDataLineBoxes.get(0).getY(), "自定义数据");
		}
		drawLabel(g, customModelDataField.getY(), "模型数据");
		if (showCanPlaceOn) {
			drawLabel(g, canPlaceOnField.getY(), "可放置方块");
		}
		if (showEnchant) {
			drawLabel(g, enchantField.getY(), "附魔");
		}
		if (showAttributes) {
			drawLabel(g, attrField.getY(), "属性");
		}

		int frameX = leftPos + RIGHT_X;
		int frameY = topPos + CONTENT_TOP;
		int colW = Math.max(64, WIDTH - RIGHT_X - 8);
		List<ClientTooltipComponent> tipParts = previewTooltipParts(colW - 20);
		int tipInnerW = 0;
		int tipInnerH = 0;
		if (!tipParts.isEmpty()) {
			tipInnerH = tipParts.size() == 1 ? -2 : 0;
			for (ClientTooltipComponent part : tipParts) {
				tipInnerW = Math.max(tipInnerW, part.getWidth(font));
				tipInnerH += part.getHeight();
			}
			if (tipParts.size() > 1) {
				tipInnerH += 2;
			}
		}
		int frameW = colW;
		int itemAreaTop = 8;
		int itemPx = Mth.clamp(colW / 3, 32, 72);
		int itemGap = 6;
		int neededH = itemAreaTop + itemPx + itemGap + (tipParts.isEmpty() ? 0 : tipInnerH + 10) + 6;
		int maxFrameH = maxPreviewFrameH();
		int minFrameH = Math.min(78, maxFrameH);
		previewFrameH = Mth.clamp(neededH, minFrameH, maxFrameH);

		layoutActionButtons();
		layoutFlagBoxes();

		UiTheme.drawPreviewFrame(g, frameX, frameY, frameW, previewFrameH);
		int itemX = frameX + (frameW - itemPx) / 2;
		int itemY = frameY + itemAreaTop;
		if (!previewStack.isEmpty()) {
			float scale = itemPx / 16.0F;
			g.pose().pushPose();
			g.pose().translate((float) itemX, (float) itemY, 10.0F);
			g.pose().scale(scale, scale, scale);
			g.renderItem(previewStack, 0, 0);
			g.renderItemDecorations(font, previewStack, 0, 0);
			g.pose().popPose();
		}

		UiTheme.drawScrollBar(g, leftPos + INPUT_X + INPUT_W + 4, topPos + CONTENT_TOP, CONTENT_BOTTOM - CONTENT_TOP, scrollOffset, maxScroll);
		g.flush();
		if (!previewStack.isEmpty() && !tipParts.isEmpty()) {
			int tipX = frameX + 8;
			int tipY = itemY + itemPx + itemGap;
			int clipBottom = frameY + previewFrameH - 4;
			int visibleH = Math.max(0, clipBottom - (tipY - 3));
			previewTipMaxScroll = Math.max(0, (tipInnerH + 6) - visibleH);
			previewTipScroll = Mth.clamp(previewTipScroll, 0, previewTipMaxScroll);
			g.enableScissor(frameX + 3, Math.max(frameY + 3, tipY - 3), frameX + frameW - 3, clipBottom);
			drawPinnedTooltip(g, tipParts, tipX, tipY - previewTipScroll, tipInnerW, tipInnerH);
			g.disableScissor();
		}
		UiTheme.pushOverlay(g);
		drawSuggestions(g, mouseX, mouseY);
		templateMenu.draw(g, font, mouseX, mouseY);
		if (refreshHover) {
			g.renderTooltip(font, Component.translatable("screen.bj_mapedit.reset"), mouseX, mouseY);
		}
		UiTheme.popOverlay(g);
		g.flush();
	}


	private int maxPreviewFrameH() {
		int flagsH = 5 * 18;
		return Math.max(56, HEIGHT - CONTENT_TOP - 8 - flagsH - FOOTER_H);
	}

	private int visibleFlagBoxCount() {
		if (unbreakableBox == null || hideEnchBox == null || hideAttrBox == null
			|| hideUnbrBox == null || hideMiscBox == null) {
			return 5;
		}
		int flags = 0;
		if (unbreakableBox.visible) flags++;
		if (hideEnchBox.visible) flags++;
		if (hideAttrBox.visible) flags++;
		if (hideUnbrBox.visible) flags++;
		if (hideMiscBox.visible) flags++;
		return flags;
	}

	private void layoutFlagBoxes() {
		int x = leftPos + RIGHT_X;
		int colW = Math.max(64, WIDTH - RIGHT_X - 8);
		int y = topPos + CONTENT_TOP + previewFrameH + 6;
		int row = 18;
		Checkbox[] boxes = {unbreakableBox, hideEnchBox, hideAttrBox, hideUnbrBox, hideMiscBox};
		for (Checkbox box : boxes) {
			if (box == null) continue;
			box.setX(x);
			box.setY(y);
			box.setWidth(colW);
			box.setHeight(18);
			y += row;
		}
	}

	private void layoutActionButtons() {
		if (giveBtn == null) {
			return;
		}
		int btnH = 20;
		int footerY = topPos + HEIGHT - FOOTER_H + (FOOTER_H - btnH) / 2;
		int pad = 8;
		int gap = 6;
		int heldW = footerBtnW(useHeldBtn);
		int copyW = footerBtnW(copyBtn);
		int saveW = footerBtnW(saveBtn);
		int giveW = footerBtnW(giveBtn);
		int inner = Math.max(8, WIDTH - pad * 2);
		int need = heldW + copyW + giveW + saveW + gap * 3;
		if (need > inner) {
			float s = (float) (inner - gap * 3) / (heldW + copyW + giveW + saveW);
			heldW = Math.max(36, (int) (heldW * s));
			copyW = Math.max(36, (int) (copyW * s));
			giveW = Math.max(36, (int) (giveW * s));
			saveW = Math.max(36, (int) (saveW * s));
		}
		int x = leftPos + pad;
		useHeldBtn.setPosition(x, footerY);
		useHeldBtn.setWidth(heldW);
		useHeldBtn.setHeight(btnH);
		x += heldW + gap;
		copyBtn.setPosition(x, footerY);
		copyBtn.setWidth(copyW);
		copyBtn.setHeight(btnH);
		x += copyW + gap;
		giveBtn.setPosition(x, footerY);
		giveBtn.setWidth(giveW);
		giveBtn.setHeight(btnH);
		saveBtn.setPosition(leftPos + WIDTH - pad - saveW, footerY);
		saveBtn.setWidth(saveW);
		saveBtn.setHeight(btnH);
	}

	private int footerBtnW(Button b) {
		return Math.max(52, this.font.width(b.getMessage()) + 22);
	}

	private boolean handleFooterClick(double mx, double my, int button) {
		if (!UiTheme.inFooterBar(mx, my, leftPos, topPos, WIDTH, HEIGHT, FOOTER_H)) return false;
		if (UiTheme.clickWidgets(mx, my, button, useHeldBtn, copyBtn, giveBtn, saveBtn)) return true;
		if (button == 0) setFocused(null);
		return true;
	}


	private List<ClientTooltipComponent> previewTooltipParts(int wrapW) {
		List<ClientTooltipComponent> parts = new ArrayList<>();
		if (previewStack.isEmpty()) {
			return parts;
		}
		int max = Math.max(8, wrapW);
		for (Component line : getTooltipFromItem(this.minecraft, previewStack)) {
			for (FormattedCharSequence seq : this.font.split(line, max)) {
				parts.add(ClientTooltipComponent.create(seq));
			}
		}
		previewStack.getTooltipImage().ifPresent(img -> {
			ClientTooltipComponent extra = ClientTooltipComponent.create(img);
			if (parts.isEmpty()) {
				parts.add(extra);
			} else {
				parts.add(Math.min(1, parts.size()), extra);
			}
		});
		return parts;
	}

	private void drawPinnedTooltip(GuiGraphics g, List<ClientTooltipComponent> parts, int x, int y, int width, int height) {
		if (parts.isEmpty()) {
			return;
		}
		g.pose().pushPose();
		g.pose().translate(0.0F, 0.0F, 200.0F);
		TooltipRenderUtil.renderTooltipBackground(g, x, y, width, height, 0);
		int cy = y;
		for (int i = 0; i < parts.size(); i++) {
			ClientTooltipComponent part = parts.get(i);
			part.renderText(this.font, x, cy, g.pose().last().pose(), g.bufferSource());
			cy += part.getHeight() + (i == 0 ? 2 : 0);
		}
		cy = y;
		for (int i = 0; i < parts.size(); i++) {
			ClientTooltipComponent part = parts.get(i);
			part.renderImage(this.font, x, cy, g);
			cy += part.getHeight() + (i == 0 ? 2 : 0);
		}
		g.pose().popPose();
	}

	private SavedState captureState() {
		return new SavedState(
			itemField.getValue(), itemNameField.getValue(),
			loreBox.getValue(),
			maxStackField.getValue(), maxDamageField.getValue(),
			String.join("\n", customDataLineBoxes.stream().map(EditBox::getValue).toList()),
			customModelDataField.getValue(), canPlaceOnField.getValue(),
			enchantField.getValue(), attrField.getValue(),
			unbreakableBox.selected(), hideEnchBox.selected(), hideAttrBox.selected(),
			hideUnbrBox.selected(), hideMiscBox.selected()
		);
	}

	private void saveTemplate() {
		commitTemplateRename();
		SavedState state = captureState();
		savedState = state;
		ItemTemplateStore.Entry entry = toTemplateEntry(defaultTemplateName(state), state);
		entry.folder = TemplateOrg.norm(selectedFolder);
		ItemTemplateStore.add(entry);
		selectedTemplate = ItemTemplateStore.all().size() - 1;
		updateTemplateScroll();
	}

	private void loadTemplate(int index) {
		ItemTemplateStore.Entry entry = ItemTemplateStore.get(index);
		if (entry == null) return;
		selectedTemplate = index;
		selectedFolder = TemplateOrg.norm(entry.folder);
		savedState = fromTemplateEntry(entry);
		applyState(savedState);
	}

	private String defaultTemplateName(SavedState state) {
		String base = stripSectionCodes(state.itemName());
		if (base.isEmpty()) {
			String item = state.item() == null ? "" : state.item().trim();
			int colon = item.lastIndexOf(':');
			base = colon >= 0 ? item.substring(colon + 1) : item;
		}
		if (base.isEmpty()) base = "模板";
		String name = base;
		int n = 2;
		while (templateNameTaken(name)) {
			name = base + n++;
		}
		return name;
	}

	private boolean templateNameTaken(String name) {
		String folder = TemplateOrg.norm(selectedFolder);
		for (ItemTemplateStore.Entry e : ItemTemplateStore.all()) {
			if (TemplateOrg.folderEq(e.folder, folder) && e.name != null && e.name.equals(name)) return true;
		}
		return false;
	}

	private static String stripSectionCodes(String text) {
		if (text == null || text.isEmpty()) return "";
		StringBuilder out = new StringBuilder();
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			if (c == '\u00a7' && i + 1 < text.length()) {
				i++;
				continue;
			}
			out.append(c);
		}
		return out.toString().trim();
	}

	private static ItemTemplateStore.Entry toTemplateEntry(String name, SavedState state) {
		ItemTemplateStore.Entry e = new ItemTemplateStore.Entry();
		e.name = name;
		e.item = state.item();
		e.itemName = state.itemName();
		e.lore = state.lore();
		e.maxStack = state.maxStack();
		e.maxDamage = state.maxDamage();
		e.customData = state.customData();
		e.customModelData = state.customModelData();
		e.canPlaceOn = state.canPlaceOn();
		e.enchant = state.enchant();
		e.attr = state.attr();
		e.unbreakable = state.unbreakable();
		e.hideEnch = state.hideEnch();
		e.hideAttr = state.hideAttr();
		e.hideUnbr = state.hideUnbr();
		e.hideMisc = state.hideMisc();
		return e;
	}

	private static SavedState fromTemplateEntry(ItemTemplateStore.Entry e) {
		return new SavedState(
			nz(e.item), nz(e.itemName), nz(e.lore),
			nz(e.maxStack), nz(e.maxDamage), nz(e.customData),
			nz(e.customModelData), nz(e.canPlaceOn),
			nz(e.enchant), nz(e.attr),
			e.unbreakable, e.hideEnch, e.hideAttr, e.hideUnbr, e.hideMisc
		);
	}

	private static String nz(String s) {
		return s == null ? "" : s;
	}

	private void restoreState() {
		applyState(savedState);
	}

	private void applyState(SavedState state) {
		if (state == null) return;
		restoring = true;
		itemField.setValue(state.item());
		itemNameField.setValue(state.itemName());
		setLoreValue(state.lore() != null ? state.lore() : "");
		maxStackField.setValue(state.maxStack());
		maxDamageField.setValue(state.maxDamage());
		clearCustomDataLines();
		String savedCustomData = state.customData();
		if (savedCustomData != null && !savedCustomData.isEmpty()) {
			for (String line : savedCustomData.split("\\n", -1)) createCustomDataLine(line);
		}
		if (customDataLineBoxes.isEmpty()) createCustomDataLine("");
		customModelDataField.setValue(state.customModelData());
		canPlaceOnField.setValue(state.canPlaceOn());
		enchantField.setValue(state.enchant());
		attrField.setValue(state.attr());

		setCheckedIfNeeded(unbreakableBox, state.unbreakable());
		setCheckedIfNeeded(hideEnchBox, state.hideEnch());
		setCheckedIfNeeded(hideAttrBox, state.hideAttr());
		setCheckedIfNeeded(hideUnbrBox, state.hideUnbr());
		setCheckedIfNeeded(hideMiscBox, state.hideMisc());
		restoring = false;
		repositionAllWidgets();
		updatePreview();
	}

	private void setCheckedIfNeeded(Checkbox box, boolean wantChecked) {
		if (box.selected() != wantChecked) {
			box.onPress();
		}
	}

	private void resetFields() {
		itemField.setValue("diamond_sword");
		itemNameField.setValue("");
		setLoreValue("");
		maxStackField.setValue("");
		maxDamageField.setValue("");
		clearCustomDataLines();
		createCustomDataLine("");
		customModelDataField.setValue("");
		canPlaceOnField.setValue("");
		enchantField.setValue("");
		attrField.setValue("");
		setCheckedIfNeeded(unbreakableBox, false);
		setCheckedIfNeeded(hideEnchBox, false);
		setCheckedIfNeeded(hideAttrBox, false);
		setCheckedIfNeeded(hideUnbrBox, false);
		setCheckedIfNeeded(hideMiscBox, false);
		showMaxStack = true;
		showMaxDamage = true;
		showUnbreakable = true;
		savedState = null;
		sessionSelectedTemplate = -1;
		sessionSelectedFolder = "";
		selectedTemplate = -1;
		selectedFolder = "";
		scrollOffset = 0;
		repositionAllWidgets();
		updatePreview();
	}

	private static final List<CdSuggestion> CD_PRESETS = List.of(
		new CdSuggestion("id", "{id:1}", "id"),
		new CdSuggestion("name", "{name:\"\"}", "名称"),
		new CdSuggestion("type", "{type:\"\"}", "类型"),
		new CdSuggestion("data", "{data:{}}", "数据"),
		new CdSuggestion("value", "{value:0}", "数值"),
		new CdSuggestion("text", "{text:\"\"}", "文本"),
		new CdSuggestion("Tags", "{Tags:[]}", "标签"),
		new CdSuggestion("Damage", "{Damage:0}", "损坏值"),
		new CdSuggestion("RepairCost", "{RepairCost:0}", "修复花费"),
		new CdSuggestion("CustomModelData", "{CustomModelData:0}", "模型数据"),
		new CdSuggestion("HideFlags", "{HideFlags:0}", "隐藏标签"),
		new CdSuggestion("BlockEntityTag", "{BlockEntityTag:{}}", "方块实体"),
		new CdSuggestion("Enchantments", "{Enchantments:[{id:\"minecraft:sharpness\",lvl:1s}]}", "附魔"),
		new CdSuggestion("display", "{display:{Name:'{\"text\":\"\"}'}}", "显示名称"),
		new CdSuggestion("entity_data", "{entity_data:{id:\"minecraft:pig\"}}", "实体数据")
	);

	private void updateItemSuggestions() {
		if (itemField == null || !itemField.isFocused()) {
			itemSuggestions = List.of();
			itemSuggestionIdx = -1;
			return;
		}
		String input = itemField.getValue().trim();
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

	private void updateCustomDataSuggestions() {
		EditBox focused = getFocusedCustomDataBox();
		if (focused == null || !focused.isFocused()) {
			customDataSuggestions = List.of();
			cdSuggestionIdx = -1;
			return;
		}
		String input = focused.getValue().trim();
		String lower = input.toLowerCase(Locale.ROOT);
		String token = cdToken(input);
		String tokenLower = token.toLowerCase(Locale.ROOT);
		List<CdSuggestion> out = new ArrayList<>();
		for (CdSuggestion p : CD_PRESETS) {
			if (p.snbt().equals(input)) continue;
			if (input.isEmpty() || cdMatches(p, lower, tokenLower)) out.add(p);
		}
		if (out.size() < 8 && token.matches("[A-Za-z_][A-Za-z0-9_]*")) {
			for (String snbt : List.of("{" + token + ":1}", "{" + token + ":\"\"}", "{" + token + ":{}}")) {
				if (snbt.equals(input)) continue;
				boolean dup = false;
				for (CdSuggestion s : out) {
					if (s.snbt().equals(snbt)) {
						dup = true;
						break;
					}
				}
				if (!dup) out.add(new CdSuggestion(token, snbt, snbt));
				if (out.size() >= 8) break;
			}
		}
		customDataSuggestions = out;
		cdSuggestionIdx = out.isEmpty() ? -1 : 0;
		suggestionScroll = SuggestionPopup.clampScroll(suggestionScroll, customDataSuggestions.size());
	}

	private static String cdToken(String input) {
		String s = input.trim();
		if (s.startsWith("{")) s = s.substring(1);
		if (s.endsWith("}")) s = s.substring(0, Math.max(0, s.length() - 1));
		int cut = s.length();
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c == ':' || c == '=' || c == ',' || c == ' ' || c == '"') {
				cut = i;
				break;
			}
		}
		return s.substring(0, cut).trim();
	}

	private static boolean cdMatches(CdSuggestion p, String lower, String tokenLower) {
		String snbt = p.snbt().toLowerCase(Locale.ROOT);
		String label = p.label().toLowerCase(Locale.ROOT);
		String key = p.key().toLowerCase(Locale.ROOT);
		if (snbt.contains(lower) || label.contains(lower) || key.contains(lower)) return true;
		return !tokenLower.isEmpty() && (key.contains(tokenLower) || label.contains(tokenLower) || snbt.contains(tokenLower));
	}

	private static CompoundTag parseCustomDataLine(String raw) {
		if (raw == null) return null;
		String text = raw.trim();
		if (text.isEmpty()) return null;
		CompoundTag tag = tryParseTag(text);
		if (tag != null) return tag;
		if (!text.startsWith("{")) {
			tag = tryParseTag("{" + text + "}");
			if (tag != null) return tag;
			int eq = text.indexOf('=');
			if (eq > 0) {
				tag = tryParseTag("{" + text.substring(0, eq) + ":" + text.substring(eq + 1) + "}");
				if (tag != null) return tag;
			}
		}
		return null;
	}

	private static CompoundTag tryParseTag(String text) {
		try {
			return TagParser.parseTag(text);
		} catch (Exception ignored) {
			return null;
		}
	}

	private boolean coversSuggestionList(AbstractWidget w) {
		AbstractWidget anchor = suggestionAnchor();
		if (anchor == null || w == anchor) return false;
		int sx = anchor.getX();
		int sy = suggestionListY(anchor, suggestionCount(anchor));
		int sw = anchor.getWidth();
		int sh = suggestionListHeight(anchor);
		return w.getX() < sx + sw && w.getX() + w.getWidth() > sx
			&& w.getY() < sy + sh && w.getY() + w.getHeight() > sy;
	}

	private AbstractWidget suggestionAnchor() {
		if (!itemSuggestions.isEmpty() && itemField.isFocused()) return itemField;
		EditBox cd = getFocusedCustomDataBox();
		if (cd != null && !customDataSuggestions.isEmpty() && cd.isFocused()) return cd;
		if (!canPlaceOnSuggestions.isEmpty() && canPlaceOnField.isFocused()) return canPlaceOnField;
		if (!enchantSuggestions.isEmpty() && enchantField.isFocused()) return enchantField;
		return null;
	}

	private int suggestionListHeight(AbstractWidget anchor) {
		return SuggestionPopup.boxH(suggestionCount(anchor));
	}

	private int suggestionCount(AbstractWidget anchor) {
		if (anchor == itemField) return itemSuggestions.size();
		if (anchor == canPlaceOnField) return canPlaceOnSuggestions.size();
		if (anchor == enchantField) return enchantSuggestions.size();
		return customDataSuggestions.size();
	}

	private int suggestionListY(AbstractWidget field, int total) {
		return SuggestionPopup.listY(field, total, topPos + CONTENT_BOTTOM);
	}

	private boolean pressSuggestionBar(double mouseX, double mouseY) {
		AbstractWidget anchor = suggestionAnchor();
		if (anchor == null) return false;
		int total = suggestionCount(anchor);
		int sx = anchor.getX();
		int sy = suggestionListY(anchor, total);
		Integer next = SuggestionPopup.pressBar(sx, sy, anchor.getWidth(), total, suggestionScroll, mouseX, mouseY);
		if (next == null) return false;
		suggestionScroll = next;
		return true;
	}

	private void drawSuggestions(GuiGraphics g, int mouseX, int mouseY) {
		drawItemSuggestions(g, mouseX, mouseY);
		drawCustomDataSuggestions(g, mouseX, mouseY);
		drawCPOSuggestions(g, mouseX, mouseY);
		drawEnchSuggestions(g, mouseX, mouseY);
	}

	private void drawItemSuggestions(GuiGraphics g, int mouseX, int mouseY) {
		if (itemSuggestions.isEmpty() || !itemField.isFocused()) return;
		int sx = itemField.getX();
		int sy = suggestionListY(itemField, itemSuggestions.size());
		SuggestionPopup.draw(g, font, sx, sy, itemField.getWidth(), itemSuggestions.size(), suggestionScroll, itemSuggestionIdx, mouseX, mouseY,
			i -> itemSuggestions.get(i).label());
	}

	private void drawCustomDataSuggestions(GuiGraphics g, int mouseX, int mouseY) {
		EditBox focused = getFocusedCustomDataBox();
		if (customDataSuggestions.isEmpty() || focused == null || !focused.isFocused()) return;
		int sx = focused.getX();
		int sy = suggestionListY(focused, customDataSuggestions.size());
		SuggestionPopup.draw(g, font, sx, sy, focused.getWidth(), customDataSuggestions.size(), suggestionScroll, cdSuggestionIdx, mouseX, mouseY,
			i -> customDataSuggestions.get(i).label());
	}

	private void drawCPOSuggestions(GuiGraphics g, int mouseX, int mouseY) {
		if (canPlaceOnSuggestions.isEmpty() || !canPlaceOnField.isFocused()) return;
		int sx = canPlaceOnField.getX();
		int sy = suggestionListY(canPlaceOnField, canPlaceOnSuggestions.size());
		SuggestionPopup.draw(g, font, sx, sy, canPlaceOnField.getWidth(), canPlaceOnSuggestions.size(), suggestionScroll, cpoSuggestionIdx, mouseX, mouseY,
			i -> canPlaceOnSuggestions.get(i).toString());
	}

	private void drawEnchSuggestions(GuiGraphics g, int mouseX, int mouseY) {
		if (enchantSuggestions.isEmpty() || !enchantField.isFocused()) return;
		int sx = enchantField.getX();
		int sy = suggestionListY(enchantField, enchantSuggestions.size());
		SuggestionPopup.draw(g, font, sx, sy, enchantField.getWidth(), enchantSuggestions.size(), suggestionScroll, enchSuggestionIdx, mouseX, mouseY,
			i -> enchantSuggestions.get(i).label());
	}

	private void layoutRefreshIcon() {
		refreshX = UiTheme.headerIconX(leftPos, WIDTH);
		refreshY = UiTheme.headerIconY(topPos);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == 0 && UiTheme.hitIcon(refreshX, refreshY, mouseX, mouseY)) {
			if (this.minecraft != null) {
				this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
			}
			resetFields();
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
		if (button == 0) {
			if (tryItemSuggestionClick(mouseX, mouseY)) return true;
			if (tryCDSuggestionClick(mouseX, mouseY)) return true;
			if (tryCPOSuggestionClick(mouseX, mouseY)) return true;
			if (tryEnchSuggestionClick(mouseX, mouseY)) return true;
		}
		if (button == 0) {
			UiTheme.ScrollClick sc = UiTheme.clickBar(leftPos + INPUT_X + INPUT_W + 4, topPos + CONTENT_TOP, CONTENT_BOTTOM - CONTENT_TOP, scrollOffset, maxScroll, mouseX, mouseY);
			if (sc != null) {
				draggingScroll = true;
				scrollGrabOffset = sc.grab;
				if (sc.scroll != scrollOffset) {
					scrollOffset = sc.scroll;
					repositionAllWidgets();
				}
				return true;
			}
		}
		if (handleFooterClick(mouseX, mouseY, button)) return true;
		boolean loreWasFocused = loreBox != null && loreBox.isFocused();
		boolean result = super.mouseClicked(mouseX, mouseY, button);
		if (loreBox != null && loreBox.isFocused() && !loreWasFocused) {
			moveLoreCursorToEnd();
		}
		if (button == 0) refreshSuggestionsAfterClick(mouseX, mouseY);
		if (button == 0 && !result) setFocused(null);
		return result;
	}

	private void refreshSuggestionsAfterClick(double mouseX, double mouseY) {
		if (SuggestionPopup.clickOpens(itemField, mouseX, mouseY)) updateItemSuggestions();
		else {
			itemSuggestions = List.of();
			itemSuggestionIdx = -1;
		}
		EditBox cd = getFocusedCustomDataBox();
		if (cd != null && SuggestionPopup.clickOpens(cd, mouseX, mouseY)) updateCustomDataSuggestions();
		else {
			customDataSuggestions = List.of();
			cdSuggestionIdx = -1;
		}
		if (SuggestionPopup.clickOpens(canPlaceOnField, mouseX, mouseY)) updateCanPlaceOnSuggestions();
		else {
			canPlaceOnSuggestions = List.of();
			cpoSuggestionIdx = -1;
		}
		if (SuggestionPopup.clickOpens(enchantField, mouseX, mouseY)) updateEnchantSuggestions();
		else {
			enchantSuggestions = List.of();
			enchSuggestionIdx = -1;
		}
	}

	private boolean tryItemSuggestionClick(double mouseX, double mouseY) {
		if (itemSuggestions.isEmpty() || !itemField.isFocused()) return false;
		int sx = itemField.getX();
		int sy = suggestionListY(itemField, itemSuggestions.size());
		int idx = SuggestionPopup.hitIndex(mouseX, mouseY, sx, sy, itemField.getWidth(), itemSuggestions.size(), suggestionScroll);
		if (idx < 0) return false;
		ItemSuggestion pick = itemSuggestions.get(idx);
		itemSuggestions = List.of();
		itemSuggestionIdx = -1;
		itemField.setValue(pick.id().toString());
		itemSuggestions = List.of();
		itemSuggestionIdx = -1;
		return true;
	}

	private boolean tryCDSuggestionClick(double mouseX, double mouseY) {
		EditBox focused = getFocusedCustomDataBox();
		if (customDataSuggestions.isEmpty() || focused == null || !focused.isFocused()) return false;
		int sx = focused.getX();
		int sy = suggestionListY(focused, customDataSuggestions.size());
		int idx = SuggestionPopup.hitIndex(mouseX, mouseY, sx, sy, focused.getWidth(), customDataSuggestions.size(), suggestionScroll);
		if (idx < 0) return false;
		applyCustomDataSuggestion(customDataSuggestions.get(idx).snbt());
		return true;
	}

	private boolean tryCPOSuggestionClick(double mouseX, double mouseY) {
		if (canPlaceOnSuggestions.isEmpty() || !canPlaceOnField.isFocused()) return false;
		int sx = canPlaceOnField.getX();
		int sy = suggestionListY(canPlaceOnField, canPlaceOnSuggestions.size());
		int idx = SuggestionPopup.hitIndex(mouseX, mouseY, sx, sy, canPlaceOnField.getWidth(), canPlaceOnSuggestions.size(), suggestionScroll);
		if (idx >= 0) {
			String cur = canPlaceOnField.getValue();
			int cursorPos = canPlaceOnField.getCursorPosition();
			String before = cur.substring(0, getCpoReplaceStart(cur, cursorPos));
			String replaceVal = canPlaceOnSuggestions.get(idx).toString();
			String after = "";
			int endPos = cur.indexOf(",", before.length());
			if (endPos >= 0) {
				after = cur.substring(endPos);
			}
			canPlaceOnField.setValue(before + replaceVal + after);
			canPlaceOnSuggestions = List.of();
			cpoSuggestionIdx = -1;
			updatePreview();
			return true;
		}
		return false;
	}

	private boolean tryEnchSuggestionClick(double mouseX, double mouseY) {
		if (enchantSuggestions.isEmpty() || !enchantField.isFocused()) return false;
		int sx = enchantField.getX();
		int sy = suggestionListY(enchantField, enchantSuggestions.size());
		int idx = SuggestionPopup.hitIndex(mouseX, mouseY, sx, sy, enchantField.getWidth(), enchantSuggestions.size(), suggestionScroll);
		if (idx >= 0) {
			String cur = enchantField.getValue();
			int cursorPos = enchantField.getCursorPosition();
			String before = cur.substring(0, getEnchReplaceStart(cur, cursorPos));
			String replaceVal = enchInsertValue(enchantSuggestions.get(idx).id());
			String after = "";
			int endPos = cur.indexOf(",", before.length());
			if (endPos >= 0) {
				after = cur.substring(endPos);
			}
			enchantField.setValue(before + replaceVal + after);
			enchantSuggestions = List.of();
			enchSuggestionIdx = -1;
			updatePreview();
			return true;
		}
		return false;
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
				repositionAllWidgets();
			}
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
			if (moved) return true;
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
		if (!itemSuggestions.isEmpty() && itemField.isFocused()) {
			if (keyCode == 258 || keyCode == 257) {
				ItemSuggestion pick = itemSuggestions.get(Math.max(0, itemSuggestionIdx));
				itemSuggestions = List.of();
				itemSuggestionIdx = -1;
				itemField.setValue(pick.id().toString());
				itemSuggestions = List.of();
				itemSuggestionIdx = -1;
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
		}
		if (!customDataSuggestions.isEmpty()) {
			EditBox cdFocused = getFocusedCustomDataBox();
			if (cdFocused != null && cdFocused.isFocused()) {
				if (keyCode == 258 || keyCode == 257) {
					applyCustomDataSuggestion(customDataSuggestions.get(Math.max(0, cdSuggestionIdx)).snbt());
					return true;
				}
				if (keyCode == 264) {
					cdSuggestionIdx = Math.min(cdSuggestionIdx + 1, customDataSuggestions.size() - 1);
					suggestionScroll = SuggestionPopup.keepVisible(cdSuggestionIdx, suggestionScroll, customDataSuggestions.size());
					return true;
				}
				if (keyCode == 265) {
					cdSuggestionIdx = Math.max(0, cdSuggestionIdx - 1);
					suggestionScroll = SuggestionPopup.keepVisible(cdSuggestionIdx, suggestionScroll, customDataSuggestions.size());
					return true;
				}
			}
		}
		if (!canPlaceOnSuggestions.isEmpty() && canPlaceOnField.isFocused()) {
			if (keyCode == 258 || keyCode == 257) {
				applyCPOSuggestion();
				return true;
			}
			if (keyCode == 264) {
				cpoSuggestionIdx = Math.min(cpoSuggestionIdx + 1, canPlaceOnSuggestions.size() - 1);
				suggestionScroll = SuggestionPopup.keepVisible(cpoSuggestionIdx, suggestionScroll, canPlaceOnSuggestions.size());
				return true;
			}
			if (keyCode == 265) {
				cpoSuggestionIdx = Math.max(0, cpoSuggestionIdx - 1);
				suggestionScroll = SuggestionPopup.keepVisible(cpoSuggestionIdx, suggestionScroll, canPlaceOnSuggestions.size());
				return true;
			}
		}
		if (!enchantSuggestions.isEmpty() && enchantField.isFocused()) {
			if (keyCode == 258 || keyCode == 257) {
				applyEnchSuggestion();
				return true;
			}
			if (keyCode == 264) {
				enchSuggestionIdx = Math.min(enchSuggestionIdx + 1, enchantSuggestions.size() - 1);
				suggestionScroll = SuggestionPopup.keepVisible(enchSuggestionIdx, suggestionScroll, enchantSuggestions.size());
				return true;
			}
			if (keyCode == 265) {
				enchSuggestionIdx = Math.max(0, enchSuggestionIdx - 1);
				suggestionScroll = SuggestionPopup.keepVisible(enchSuggestionIdx, suggestionScroll, enchantSuggestions.size());
				return true;
			}
		}
		if (keyCode == 256 && itemField.isFocused() && !itemSuggestions.isEmpty()) {
			itemSuggestions = List.of();
			itemSuggestionIdx = -1;
			return true;
		}
		if (keyCode == 256) {
			EditBox cdFocused = getFocusedCustomDataBox();
			if (cdFocused != null && cdFocused.isFocused() && !customDataSuggestions.isEmpty()) {
				customDataSuggestions = List.of();
				cdSuggestionIdx = -1;
				return true;
			}
		}
		if (keyCode == 256 && canPlaceOnField.isFocused() && !canPlaceOnSuggestions.isEmpty()) {
			canPlaceOnSuggestions = List.of();
			cpoSuggestionIdx = -1;
			return true;
		}
		if (keyCode == 256 && enchantField.isFocused() && !enchantSuggestions.isEmpty()) {
			enchantSuggestions = List.of();
			enchSuggestionIdx = -1;
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}


	private void clearCustomDataLines() {
		for (EditBox box : customDataLineBoxes) this.removeWidget(box);
		for (AbstractWidget b : customDataRemoveBtns) this.removeWidget(b);
		customDataLineBoxes.clear();
		customDataRemoveBtns.clear();
	}


	private EditBox createCustomDataLine(String value) {
		EditBox box = new EditBox(font, leftPos + INPUT_X, 0, INPUT_W - 20, 16, Component.empty());
		if (value != null) box.setValue(value);
		box.setResponder(s -> { updatePreview(); updateCustomDataSuggestions(); });
		this.addRenderableWidget(box);
		customDataLineBoxes.add(box);
		Button minus = Button.builder(Component.literal("-"), b -> removeCustomDataLine(box))
			.bounds(leftPos + INPUT_X + INPUT_W - 18, 0, 18, 16).build();
		this.addRenderableWidget(minus);
		customDataRemoveBtns.add(minus);
		return box;
	}


	private void removeCustomDataLine(EditBox box) {
		int i = customDataLineBoxes.indexOf(box);
		if (i < 0) return;
		this.removeWidget(box);
		this.removeWidget(customDataRemoveBtns.remove(i));
		customDataLineBoxes.remove(i);
		repositionAllWidgets();
		updatePreview();
	}

	private void setLoreValue(String text) {
		if (loreBox == null) return;
		loreBox.setValue(text != null ? text : "");
		moveLoreCursorToEnd();
	}

	private void moveLoreCursorToEnd() {
		if (loreBox == null) return;
		MultiLineEditBoxAccessor box = (MultiLineEditBoxAccessor) loreBox;
		box.mapedit$textField().seekCursor(Whence.END, 0);
		box.mapedit$scrollToCursor();
	}

	private void openLoreTextEditor() {
		String currentText = loreBox.getValue();
		pendingLoreText = currentText;
		Minecraft.getInstance().setScreen(new TextEditorScreen(this, currentText, newText -> {
			if (newText == null) return;
			pendingLoreText = newText;
		}));
	}

	private void openItemNameTextEditor() {
		String currentText = itemNameField.getValue();
		pendingItemNameText = currentText;
		Minecraft.getInstance().setScreen(new TextEditorScreen(this, currentText, newText -> {
			if (newText != null) pendingItemNameText = newText;
		}));
	}

	private void addCustomDataLine() {
		createCustomDataLine("");
		repositionAllWidgets();
		updatePreview();
	}

	private EditBox getFocusedCustomDataBox() {
		for (EditBox box : customDataLineBoxes) {
			if (box.isFocused()) return box;
		}
		return null;
	}

	private void applyCustomDataSuggestion(String snbt) {
		EditBox focused = getFocusedCustomDataBox();
		if (focused == null || snbt == null) return;
		customDataSuggestions = List.of();
		cdSuggestionIdx = -1;
		focused.setValue(snbt);
		customDataSuggestions = List.of();
		cdSuggestionIdx = -1;
		updatePreview();
	}

	private static void setHighlightPos(EditBox box, int pos) {
		try {
			java.lang.reflect.Field f = EditBox.class.getDeclaredField("highlightPos");
			f.setAccessible(true);
			f.set(box, pos);
		} catch (Exception ignored) {}
	}

	private Component parseLoreSectionCodes(String text, Style baseStyle) {
		if (text.isEmpty()) return Component.empty();
		MutableComponent result = Component.empty();
		Style currentStyle = baseStyle;
		StringBuilder currentText = new StringBuilder();

		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			if (c == '\u00a7' && i + 1 < text.length()) {
				if (currentText.length() > 0) {
					result.append(Component.literal(currentText.toString()).withStyle(currentStyle));
					currentText.setLength(0);
				}
				char code = text.charAt(i + 1);
				if (code == 'x' || code == 'X') {
					if (i + 13 < text.length() && text.charAt(i + 2) == '\u00a7'
						&& text.charAt(i + 4) == '\u00a7' && text.charAt(i + 6) == '\u00a7'
						&& text.charAt(i + 8) == '\u00a7' && text.charAt(i + 10) == '\u00a7'
						&& text.charAt(i + 12) == '\u00a7') {
						StringBuilder rgb = new StringBuilder(6);
						rgb.append(text.charAt(i + 3)).append(text.charAt(i + 5)).append(text.charAt(i + 7))
							.append(text.charAt(i + 9)).append(text.charAt(i + 11)).append(text.charAt(i + 13));
						try {
							int rgbVal = Integer.parseInt(rgb.toString(), 16);
							currentStyle = baseStyle.withColor(rgbVal);
						} catch (NumberFormatException ignored) {}
						i += 13;
					} else {
						i++;
					}
				} else {
					ChatFormatting formatting = ChatFormatting.getByCode(code);
					if (formatting != null) {
						if (formatting == ChatFormatting.RESET) {
							currentStyle = baseStyle;
						} else if (formatting.isColor()) {
							Integer fmtColor = formatting.getColor();
							currentStyle = fmtColor != null ? baseStyle.withColor(fmtColor) : baseStyle;
						} else {
							currentStyle = applyLoreFormat(currentStyle, formatting);
						}
					}
					i++;
				}
			} else {
				currentText.append(c);
			}
		}
		if (currentText.length() > 0) {
			result.append(Component.literal(currentText.toString()).withStyle(currentStyle));
		}
		return result;
	}

	private Style applyLoreFormat(Style style, ChatFormatting formatting) {
		return switch (formatting) {
			case BOLD -> style.withBold(true);
			case ITALIC -> style.withItalic(true);
			case UNDERLINE -> style.withUnderlined(true);
			case STRIKETHROUGH -> style.withStrikethrough(true);
			case OBFUSCATED -> style.withObfuscated(true);
			default -> style;
		};
	}

	private void updateCanPlaceOnSuggestions() {
		if (canPlaceOnField == null || !canPlaceOnField.isFocused()) {
			canPlaceOnSuggestions = List.of();
			cpoSuggestionIdx = -1;
			return;
		}
		String input = canPlaceOnField.getValue().trim();
		String lastSegment = getCpoCurrentSegment(input);
		String lower = lastSegment.toLowerCase(Locale.ROOT);
		canPlaceOnSuggestions = BuiltInRegistries.BLOCK.keySet().stream()
			.filter(id -> lastSegment.isEmpty() || id.toString().toLowerCase(Locale.ROOT).contains(lower))
			.sorted(java.util.Comparator.comparing(ResourceLocation::toString))
			.toList();
		cpoSuggestionIdx = canPlaceOnSuggestions.isEmpty() ? -1 : 0;
		suggestionScroll = SuggestionPopup.clampScroll(suggestionScroll, canPlaceOnSuggestions.size());
	}

	private void updateEnchantSuggestions() {
		if (enchantField == null || !enchantField.isFocused() || this.minecraft == null || this.minecraft.level == null) {
			enchantSuggestions = List.of();
			enchSuggestionIdx = -1;
			return;
		}
		String input = enchantField.getValue().trim();
		String enchIdPart = getEnchCurrentIdPart(input);
		Optional<HolderLookup.RegistryLookup<Enchantment>> enchReg = this.minecraft.level.registryAccess().lookup(Registries.ENCHANTMENT);
		if (enchReg.isEmpty()) {
			enchantSuggestions = List.of();
			enchSuggestionIdx = -1;
			return;
		}
		ResourceLocation exactId = parseEnchId(enchIdPart);
		if (!enchIdPart.isEmpty() && exactId != null && isCompleteEnchSegment(enchIdPart, exactId)
			&& enchReg.get().get(ResourceKey.create(Registries.ENCHANTMENT, exactId)).isPresent()) {
			enchantSuggestions = List.of();
			enchSuggestionIdx = -1;
			return;
		}
		String q = enchQuery(enchIdPart).toLowerCase(Locale.ROOT);
		enchantSuggestions = enchReg.get().listElements()
			.map(h -> new EnchSuggestion(h.key().location(), h.value().description().getString()))
			.filter(s -> enchIdPart.isEmpty()
				|| s.id().toString().toLowerCase(Locale.ROOT).contains(q)
				|| s.id().getPath().toLowerCase(Locale.ROOT).contains(q)
				|| s.label().toLowerCase(Locale.ROOT).contains(q))
			.sorted(java.util.Comparator.comparing(s -> s.id().toString()))
			.toList();
		enchSuggestionIdx = enchantSuggestions.isEmpty() ? -1 : 0;
		suggestionScroll = SuggestionPopup.clampScroll(suggestionScroll, enchantSuggestions.size());
	}

	private static String enchQuery(String segment) {
		int last = segment.lastIndexOf(':');
		if (last >= 0 && segment.substring(last + 1).trim().matches("\\d+")) {
			return segment.substring(0, last).trim();
		}
		return segment.trim();
	}

	private static boolean isCompleteEnchSegment(String segment, ResourceLocation id) {
		String s = segment.trim();
		int last = s.lastIndexOf(':');
		if (last >= 0 && s.substring(last + 1).trim().matches("\\d+")) {
			String idPart = s.substring(0, last).trim();
			return idPart.equalsIgnoreCase(id.toString()) || idPart.equalsIgnoreCase(id.getPath());
		}
		return s.equalsIgnoreCase(id.toString());
	}

	private static ResourceLocation parseEnchId(String part) {
		if (part == null) return null;
		String s = part.trim();
		if (s.isEmpty()) return null;
		int last = s.lastIndexOf(':');
		if (last >= 0 && s.substring(last + 1).trim().matches("\\d+")) {
			return ResourceLocation.tryParse(s.substring(0, last).trim());
		}
		return ResourceLocation.tryParse(s);
	}

	private static int parseEnchLevel(String part) {
		if (part == null) return 1;
		String s = part.trim();
		int last = s.lastIndexOf(':');
		if (last >= 0 && s.substring(last + 1).trim().matches("\\d+")) {
			try {
				return Integer.parseInt(s.substring(last + 1).trim());
			} catch (NumberFormatException ignored) {
				return 1;
			}
		}
		return 1;
	}

	private String enchInsertValue(ResourceLocation id) {
		String cur = enchantField.getValue();
		int cursorPos = enchantField.getCursorPosition();
		int start = getEnchReplaceStart(cur, cursorPos);
		int end = cur.indexOf(",", start);
		if (end < 0) end = cur.length();
		String segment = start < end ? cur.substring(start, end) : "";
		return id.toString() + ":" + parseEnchLevel(segment);
	}

	private String getCpoCurrentSegment(String input) {
		int cursorPos = canPlaceOnField.getCursorPosition();
		if (cursorPos <= 0) return input;
		String beforeCursor = input.substring(0, cursorPos);
		int lastComma = beforeCursor.lastIndexOf(",");
		return lastComma >= 0 ? beforeCursor.substring(lastComma + 1).trim() : beforeCursor.trim();
	}

	private int getCpoReplaceStart(String input, int cursorPos) {
		if (cursorPos <= 0) return 0;
		String beforeCursor = input.substring(0, cursorPos);
		int lastComma = beforeCursor.lastIndexOf(",");
		return lastComma >= 0 ? lastComma + 1 : 0;
	}

	private String getEnchCurrentIdPart(String input) {
		int cursorPos = enchantField.getCursorPosition();
		if (cursorPos <= 0) return input;
		String beforeCursor = input.substring(0, cursorPos);
		int lastComma = beforeCursor.lastIndexOf(",");
		String segment = lastComma >= 0 ? beforeCursor.substring(lastComma + 1).trim() : beforeCursor.trim();
		int lastColon = segment.lastIndexOf(":");
		if (lastColon >= 0) {
			String afterColon = segment.substring(lastColon + 1);
			if (afterColon.matches("\\d+")) {
				return segment.substring(0, lastColon).trim();
			}
		}
		return segment;
	}

	private int getEnchReplaceStart(String input, int cursorPos) {
		if (cursorPos <= 0) return 0;
		String beforeCursor = input.substring(0, cursorPos);
		int lastComma = beforeCursor.lastIndexOf(",");
		return lastComma >= 0 ? lastComma + 1 : 0;
	}

	private void applyCPOSuggestion() {
		if (canPlaceOnSuggestions.isEmpty() || !canPlaceOnField.isFocused()) return;
		String cur = canPlaceOnField.getValue();
		int cursorPos = canPlaceOnField.getCursorPosition();
		String before = cur.substring(0, getCpoReplaceStart(cur, cursorPos));
		String replaceVal = canPlaceOnSuggestions.get(Math.max(0, cpoSuggestionIdx)).toString();
		String after = "";
		int endPos = cur.indexOf(",", before.length());
		if (endPos >= 0) {
			after = cur.substring(endPos);
		}
		canPlaceOnField.setValue(before + replaceVal + after);
		canPlaceOnSuggestions = List.of();
		cpoSuggestionIdx = -1;
		updatePreview();
	}

	private void applyEnchSuggestion() {
		if (enchantSuggestions.isEmpty() || !enchantField.isFocused()) return;
		String cur = enchantField.getValue();
		int cursorPos = enchantField.getCursorPosition();
		String before = cur.substring(0, getEnchReplaceStart(cur, cursorPos));
		String replaceVal = enchInsertValue(enchantSuggestions.get(Math.max(0, enchSuggestionIdx)).id());
		String after = "";
		int endPos = cur.indexOf(",", before.length());
		if (endPos >= 0) {
			after = cur.substring(endPos);
		}
		enchantField.setValue(before + replaceVal + after);
		enchantSuggestions = List.of();
		enchSuggestionIdx = -1;
		updatePreview();
	}

	private void drawLabel(GuiGraphics g, int widgetY, String cnLabel) {
		if (widgetY + 10 > topPos + CONTENT_TOP && widgetY < topPos + CONTENT_BOTTOM) {
			UiTheme.label(g, font, cnLabel, leftPos + LABEL_X, widgetY + 4);
		}
	}

	private boolean inTemplateRail(double mouseX, double mouseY) {
		int x = templateRailX();
		return mouseX >= x && mouseX < x + templateRailW && mouseY >= topPos && mouseY < topPos + HEIGHT;
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

	private List<TemplateOrg.Row> templateRows() {
		return ItemTemplateStore.rows(collapsedFolders);
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
		return templateRailX() + templateRailW - 5;
	}

	private int templateActionRight() {
		return templateRailX() + templateRailW - 8 - (templateMaxScroll > 0 ? 6 : 0);
	}

	private int templateRowY(int index) {
		return templateListTop() + index * TEMPLATE_ROW_H - templateScroll;
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
		int x = templateRailX();
		int w = templateRailW;
		updateTemplateScroll();
		UiTheme.drawPanel(g, x, topPos, w, HEIGHT);
		boolean hasSel = TemplateOrg.hasSel(selectedTemplate, selectedFolder);
		TemplateRailUi.drawHeader(g, font, Component.translatable("screen.bj_mapedit.templates").getString(), x, w, topPos, hx, hy, hasSel);
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
		if (UiTheme.hitScrollBar(templateScrollBarX(), templateListTop(), templateListH(), mouseX, mouseY)) {
			return false;
		}
		boolean hasSel = TemplateOrg.hasSel(selectedTemplate, selectedFolder);
		if (mouseY >= topPos && mouseY < topPos + UiTheme.HEADER_H) {
			if (button == 0 && UiTheme.hitNewFolder(templateRailX(), templateRailW, topPos, font, mouseX, mouseY, hasSel)) {
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
			mouseX, mouseY, templateRailX(), templateRailW, topPos, HEIGHT,
			templateListTop(), templateListBottom(), templateScroll, TEMPLATE_ROW_H, templateRows()
		);
		return true;
	}

	private boolean finishTemplateDrag() {
		if (!templateDrag.busy()) return false;
		int index = templateDrag.storeIndex;
		boolean click = templateDrag.wasClick();
		if (templateDrag.shouldMove()) {
			ItemTemplateStore.moveToFolder(index, templateDrag.hoverFolder);
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
		String name = ItemTemplateStore.addFolder();
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
		ItemTemplateStore.Entry kept = selectedTemplate >= 0 ? ItemTemplateStore.get(selectedTemplate) : null;
		ItemTemplateStore.removeFolder(name);
		collapsedFolders.remove(TemplateOrg.norm(name));
		if (TemplateOrg.folderEq(selectedFolder, name)) {
			selectedFolder = "";
			selectedTemplate = -1;
		} else if (kept != null) {
			selectedTemplate = ItemTemplateStore.all().indexOf(kept);
		}
		updateTemplateScroll();
	}

	private void deleteTemplate(int index) {
		if (renamingTemplate == index) cancelTemplateRename();
		else commitTemplateRename();
		ItemTemplateStore.remove(index);
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
		setHighlightPos(templateRenameBox, 0);
	}

	private void startTemplateRename(int index) {
		ItemTemplateStore.Entry entry = ItemTemplateStore.get(index);
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
		int x = TemplateOrg.rowNameX(templateRailX(), rows.get(rowIdx).depth);
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
			ItemTemplateStore.renameFolder(from, to);
			String normTo = TemplateOrg.norm(to);
			if (!normTo.isEmpty() && !TemplateOrg.folderEq(from, normTo)) {
				TemplateOrg.renameCollapsed(collapsedFolders, from, normTo);
				if (TemplateOrg.folderEq(selectedFolder, from)) selectedFolder = normTo;
			}
			cancelTemplateRename();
			return;
		}
		if (renamingTemplate < 0) return;
		ItemTemplateStore.rename(renamingTemplate, templateRenameBox.getValue());
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

	@Override
	public void removed() {
		if (itemField != null) {
			savedState = captureState();
			sessionSelectedTemplate = selectedTemplate;
			sessionSelectedFolder = selectedFolder;
		}
		super.removed();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
