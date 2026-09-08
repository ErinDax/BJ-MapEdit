package cn.erindax.bjmapedit.client.screen;

import cn.erindax.bjmapedit.client.widget.GiveCommands;
import cn.erindax.bjmapedit.client.widget.InvPicker;
import cn.erindax.bjmapedit.client.widget.SafeIds;
import cn.erindax.bjmapedit.client.widget.SuggestionPopup;
import cn.erindax.bjmapedit.client.widget.TemplateDrag;
import cn.erindax.bjmapedit.client.widget.TemplateOrg;
import cn.erindax.bjmapedit.client.widget.RenameDeleteMenu;
import cn.erindax.bjmapedit.client.widget.TemplateRailUi;
import cn.erindax.bjmapedit.client.widget.UiTheme;
import cn.erindax.bjmapedit.client.widget.VillagerTemplateStore;
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
import net.minecraft.core.component.DataComponentPredicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Environment(EnvType.CLIENT)
public class VillagerEditorScreen extends Screen {

	private int WIDTH = 500;
	private int HEIGHT = 400;
	private static final int LABEL_X = 12;
	private static final int ROW_H = 20;
	private static final int TRADE_H = 66;
	private static final int FOOTER_H = 30;
	private static final int TEMPLATE_ROW_H = 22;
	private int INPUT_X = 72;
	private int INPUT_W = 160;
	private int RIGHT_X = 280;
	private int CONTENT_TOP = 30;
	private int CONTENT_BOTTOM;
	private int templateRailX = 8;
	private int templateRailW = 148;

	private static final Map<String, String> TYPE_CN = Map.of(
		"desert", "沙漠", "jungle", "丛林", "plains", "平原",
		"savanna", "热带草原", "snow", "雪地", "swamp", "沼泽", "taiga", "针叶林"
	);
	private static final Map<String, String> PROF_CN = Map.ofEntries(
		Map.entry("none", "无"), Map.entry("armorer", "盔甲匠"), Map.entry("butcher", "屠夫"),
		Map.entry("cartographer", "制图师"), Map.entry("cleric", "牧师"), Map.entry("farmer", "农民"),
		Map.entry("fisherman", "渔夫"), Map.entry("fletcher", "制箭师"), Map.entry("leatherworker", "皮匠"),
		Map.entry("librarian", "图书管理员"), Map.entry("mason", "石匠"), Map.entry("nitwit", "傻子"),
		Map.entry("shepherd", "牧羊人"), Map.entry("toolsmith", "工具匠"), Map.entry("weaponsmith", "武器匠")
	);

	private EditBox typeField;
	private EditBox professionField;
	private EditBox levelField;
	private EditBox customNameField;
	private Checkbox noAiBox;
	private Checkbox silentBox;
	private Checkbox noGravityBox;
	private Checkbox babyBox;
	private Button customNameEditorBtn;
	private Button addNbtFieldBtn;
	private Button addTradeBtn;
	private Button copyBtn;
	private Button summonBtn;
	private Button saveBtn;

	private final List<NbtRow> nbtRows = new ArrayList<>();
	private final List<TradeEntry> trades = new ArrayList<>();
	private final List<ItemStack> tradeBuyStacks = new ArrayList<>();
	private final List<ItemStack> tradeBuyBStacks = new ArrayList<>();
	private final List<ItemStack> tradeSellStacks = new ArrayList<>();

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
	private boolean restoring;

	private Entity previewEntity;
	private String lastPreviewSig;
	private float previewRotY;
	private float previewRotX;
	private boolean draggingPreview;
	private double dragStartX, dragStartY;
	private float dragStartRotY, dragStartRotX;
	private int previewX, previewY, previewW, previewH;
	private int tradePreviewY, tradePreviewH;
	private String pendingCustomName;

	private int invPickerTarget = -1;
	private TradePick invPickerKind = TradePick.BUY;
	private final InvPicker invPicker = new InvPicker();

	private enum TradePick { BUY, BUY_B, SELL }

	private record Suggestion(String id, String label) {}
	private List<Suggestion> typeSuggestions = List.of();
	private int typeSuggestionIdx = -1;
	private List<Suggestion> profSuggestions = List.of();
	private int profSuggestionIdx = -1;
	private int suggestionScroll;

	private static SavedState savedState;

	private static class NbtRow {
		EditBox keyBox;
		EditBox valueBox;
		Button removeBtn;
	}

	private record TradeEntry(
		EditBox buyItem, EditBox buyCount,
		EditBox buyBItem, EditBox buyBCount,
		EditBox sellItem, EditBox sellCount,
		EditBox maxUses, Button addInvBuyBtn, Button addInvBuyBBtn, Button addInvSellBtn, Button removeBtn
	) {}

	private record SavedState(
		String type, String profession, String level, String customName,
		boolean noAI, boolean silent, boolean noGravity, boolean baby,
		List<String> nbtKeys, List<String> nbtValues,
		List<VillagerTemplateStore.TradeData> trades
	) {}

	public VillagerEditorScreen() {
		super(Component.translatable("screen.bj_mapedit.villager_title"));
	}

	@Override
	protected void init() {
		boolean first = !opened;
		opened = true;
		boolean wasReinit = typeField != null;
		SavedState snap = wasReinit ? captureState() : null;
		int snapScroll = scrollOffset;
		super.init();
		layoutPanel();
		restoring = true;

		nbtRows.clear();
		trades.clear();
		tradeBuyStacks.clear();
		tradeBuyBStacks.clear();
		tradeSellStacks.clear();
		typeSuggestions = List.of();
		profSuggestions = List.of();
		typeSuggestionIdx = -1;
		profSuggestionIdx = -1;

		typeField = new EditBox(font, 0, 0, INPUT_W, 16, Component.empty());
		typeField.setResponder(s -> updateTypeSuggestions());
		this.addRenderableWidget(typeField);

		professionField = new EditBox(font, 0, 0, INPUT_W, 16, Component.empty());
		professionField.setResponder(s -> updateProfessionSuggestions());
		this.addRenderableWidget(professionField);

		levelField = new EditBox(font, 0, 0, INPUT_W, 16, Component.empty());
		levelField.setValue("1");
		this.addRenderableWidget(levelField);

		customNameField = new EditBox(font, 0, 0, INPUT_W, 16, Component.empty());
		this.addRenderableWidget(customNameField);
		customNameEditorBtn = this.addRenderableWidget(Button.builder(
			Component.translatable("screen.bj_mapedit.edit_text"), b -> openCustomNameTextEditor()
		).bounds(0, 0, 88, 16).build());

		noAiBox = Checkbox.builder(Component.empty(), font).pos(0, 0).build();
		silentBox = Checkbox.builder(Component.empty(), font).pos(0, 0).build();
		noGravityBox = Checkbox.builder(Component.empty(), font).pos(0, 0).build();
		babyBox = Checkbox.builder(Component.empty(), font).pos(0, 0).build();
		this.addRenderableWidget(noAiBox);
		this.addRenderableWidget(silentBox);
		this.addRenderableWidget(noGravityBox);
		this.addRenderableWidget(babyBox);

		addNbtFieldBtn = this.addRenderableWidget(Button.builder(Component.literal("添加自定义"), b -> addNbtField())
			.bounds(0, 0, 88, 16).build());
		addTradeBtn = this.addRenderableWidget(Button.builder(Component.literal("添加交易"), b -> addTrade())
			.bounds(0, 0, 88, 16).build());

		copyBtn = this.addRenderableWidget(Button.builder(
			Component.translatable("screen.bj_mapedit.copy_cmd"), b -> copySummonCommand()
		).bounds(0, 0, 70, 20).build());
		summonBtn = this.addRenderableWidget(Button.builder(
			Component.translatable("screen.bj_mapedit.summon_plain"), b -> summonVillager()
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
		restoring = false;
		repositionAll();
	}

	@Override
	public void added() {
		super.added();
		if (pendingCustomName != null && customNameField != null) {
			customNameField.setValue(pendingCustomName);
			pendingCustomName = null;
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
			labelW = Math.max(labelW, this.font.width("图书管理员") + 10);
			labelW = Math.max(labelW, this.font.width("自定义") + 10);
			labelW = Math.max(labelW, this.font.width("买入2") + 10);
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

	private void openCustomNameTextEditor() {
		pendingCustomName = customNameField != null ? customNameField.getValue() : "";
		Minecraft.getInstance().setScreen(new TextEditorScreen(this, pendingCustomName, newText -> {
			if (newText != null) pendingCustomName = newText;
		}));
	}

	private void addNbtField() {
		NbtRow row = new NbtRow();
		row.keyBox = new EditBox(font, 0, 0, 56, 16, Component.empty());
		row.valueBox = new EditBox(font, 0, 0, INPUT_W, 16, Component.empty());
		row.removeBtn = Button.builder(Component.literal("-"), b -> removeNbtRow(row)).bounds(0, 0, 16, 16).build();
		this.addRenderableWidget(row.keyBox);
		this.addRenderableWidget(row.valueBox);
		this.addRenderableWidget(row.removeBtn);
		nbtRows.add(row);
		repositionAll();
	}

	private void removeNbtRow(NbtRow row) {
		this.removeWidget(row.keyBox);
		this.removeWidget(row.valueBox);
		this.removeWidget(row.removeBtn);
		nbtRows.remove(row);
		repositionAll();
	}

	private void addTrade() {
		int rx = leftPos + INPUT_X;
		EditBox buyItem = new EditBox(font, rx, 0, 75, 16, Component.empty());
		EditBox buyCount = new EditBox(font, rx, 0, 24, 16, Component.empty());
		buyCount.setValue("1");
		EditBox buyBItem = new EditBox(font, rx, 0, 75, 16, Component.empty());
		EditBox buyBCount = new EditBox(font, rx, 0, 24, 16, Component.empty());
		buyBCount.setValue("1");
		EditBox sellItem = new EditBox(font, rx, 0, 75, 16, Component.empty());
		EditBox sellCount = new EditBox(font, rx, 0, 24, 16, Component.empty());
		sellCount.setValue("1");
		EditBox maxUses = new EditBox(font, rx, 0, 24, 16, Component.empty());
		maxUses.setValue("16");
		Button addInvBuy = Button.builder(Component.translatable("screen.bj_mapedit.inv_pick_btn"), b -> openInvPicker(b, TradePick.BUY)).bounds(0, 0, 24, 16).build();
		Button addInvBuyB = Button.builder(Component.translatable("screen.bj_mapedit.inv_pick_btn"), b -> openInvPicker(b, TradePick.BUY_B)).bounds(0, 0, 24, 16).build();
		Button addInvSell = Button.builder(Component.translatable("screen.bj_mapedit.inv_pick_btn"), b -> openInvPicker(b, TradePick.SELL)).bounds(0, 0, 24, 16).build();
		Button remove = Button.builder(Component.literal("-"), b -> removeTrade(b)).bounds(0, 0, 16, 16).build();
		this.addRenderableWidget(buyItem);
		this.addRenderableWidget(buyCount);
		this.addRenderableWidget(buyBItem);
		this.addRenderableWidget(buyBCount);
		this.addRenderableWidget(sellItem);
		this.addRenderableWidget(sellCount);
		this.addRenderableWidget(maxUses);
		this.addRenderableWidget(addInvBuy);
		this.addRenderableWidget(addInvBuyB);
		this.addRenderableWidget(addInvSell);
		this.addRenderableWidget(remove);
		trades.add(new TradeEntry(buyItem, buyCount, buyBItem, buyBCount, sellItem, sellCount, maxUses, addInvBuy, addInvBuyB, addInvSell, remove));
		tradeBuyStacks.add(ItemStack.EMPTY);
		tradeBuyBStacks.add(ItemStack.EMPTY);
		tradeSellStacks.add(ItemStack.EMPTY);
		repositionAll();
	}

	private void removeTrade(Button btn) {
		int idx = -1;
		for (int i = 0; i < trades.size(); i++) {
			if (trades.get(i).removeBtn() == btn) {
				idx = i;
				break;
			}
		}
		if (idx < 0) return;
		TradeEntry e = trades.get(idx);
		this.removeWidget(e.buyItem());
		this.removeWidget(e.buyCount());
		this.removeWidget(e.buyBItem());
		this.removeWidget(e.buyBCount());
		this.removeWidget(e.sellItem());
		this.removeWidget(e.sellCount());
		this.removeWidget(e.maxUses());
		this.removeWidget(e.addInvBuyBtn());
		this.removeWidget(e.addInvBuyBBtn());
		this.removeWidget(e.addInvSellBtn());
		this.removeWidget(e.removeBtn());
		trades.remove(idx);
		if (idx < tradeBuyStacks.size()) tradeBuyStacks.remove(idx);
		if (idx < tradeBuyBStacks.size()) tradeBuyBStacks.remove(idx);
		if (idx < tradeSellStacks.size()) tradeSellStacks.remove(idx);
		repositionAll();
	}

	private void openInvPicker(Button src, TradePick kind) {
		for (int i = 0; i < trades.size(); i++) {
			TradeEntry e = trades.get(i);
			boolean hit = switch (kind) {
				case BUY -> e.addInvBuyBtn() == src;
				case BUY_B -> e.addInvBuyBBtn() == src;
				case SELL -> e.addInvSellBtn() == src;
			};
			if (hit) {
				invPickerTarget = i;
				invPickerKind = kind;
				invPicker.open();
				return;
			}
		}
	}

	private void closeInvPicker() {
		invPickerTarget = -1;
		invPicker.close();
	}

	private void applyInvPickerItem(ItemStack stack) {
		if (invPickerTarget < 0 || invPickerTarget >= trades.size() || stack.isEmpty()) return;
		ItemStack picked = stack.copy();
		TradeEntry e = trades.get(invPickerTarget);
		ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
		if (invPickerKind == TradePick.SELL) {
			while (tradeSellStacks.size() <= invPickerTarget) tradeSellStacks.add(ItemStack.EMPTY);
			tradeSellStacks.set(invPickerTarget, picked);
			if (id != null) e.sellItem().setValue(id.toString());
			e.sellCount().setValue(String.valueOf(Math.max(1, stack.getCount())));
		} else if (invPickerKind == TradePick.BUY_B) {
			while (tradeBuyBStacks.size() <= invPickerTarget) tradeBuyBStacks.add(ItemStack.EMPTY);
			tradeBuyBStacks.set(invPickerTarget, picked);
			if (id != null) e.buyBItem().setValue(id.toString());
			e.buyBCount().setValue(String.valueOf(Math.max(1, stack.getCount())));
		} else {
			while (tradeBuyStacks.size() <= invPickerTarget) tradeBuyStacks.add(ItemStack.EMPTY);
			tradeBuyStacks.set(invPickerTarget, picked);
			if (id != null) e.buyItem().setValue(id.toString());
			e.buyCount().setValue(String.valueOf(Math.max(1, stack.getCount())));
		}
		closeInvPicker();
	}

	private void repositionAll() {
		layoutPanel();
		if (typeField == null || saveBtn == null) return;
		int y = topPos + CONTENT_TOP + 2 - scrollOffset;
		int total = 0;
		placeLeft(typeField, y); y += ROW_H; total += ROW_H;
		placeLeft(professionField, y); y += ROW_H; total += ROW_H;
		placeLeft(levelField, y); y += ROW_H; total += ROW_H;
		placeLeft(customNameField, y);
		if (customNameEditorBtn != null) {
			customNameEditorBtn.setX(leftPos + INPUT_X);
			customNameEditorBtn.setY(y + ROW_H);
			customNameEditorBtn.setWidth(Math.min(INPUT_W, Math.max(56, font.width(customNameEditorBtn.getMessage()) + 12)));
		}
		y += ROW_H * 2;
		total += ROW_H * 2;
		placeCheck(noAiBox, y); y += ROW_H; total += ROW_H;
		placeCheck(silentBox, y); y += ROW_H; total += ROW_H;
		placeCheck(noGravityBox, y); y += ROW_H; total += ROW_H;
		placeCheck(babyBox, y); y += ROW_H; total += ROW_H;
		for (NbtRow row : nbtRows) {
			int minus = 20;
			int keyW = 56;
			row.keyBox.setX(leftPos + INPUT_X);
			row.keyBox.setY(y);
			row.keyBox.setWidth(keyW);
			row.valueBox.setX(leftPos + INPUT_X + keyW + 4);
			row.valueBox.setY(y);
			row.valueBox.setWidth(Math.max(24, INPUT_W - keyW - 4 - minus));
			row.removeBtn.setPosition(leftPos + INPUT_X + INPUT_W - 16, y);
			row.removeBtn.setWidth(16);
			row.removeBtn.setHeight(16);
			y += ROW_H;
			total += ROW_H;
		}
		if (addNbtFieldBtn != null) {
			addNbtFieldBtn.setX(leftPos + INPUT_X);
			addNbtFieldBtn.setY(y);
			addNbtFieldBtn.setWidth(Math.min(INPUT_W, Math.max(56, font.width(addNbtFieldBtn.getMessage()) + 12)));
		}
		y += ROW_H;
		total += ROW_H;
		if (addTradeBtn != null) {
			addTradeBtn.setX(leftPos + INPUT_X);
			addTradeBtn.setY(y);
			addTradeBtn.setWidth(Math.min(INPUT_W, Math.max(56, font.width(addTradeBtn.getMessage()) + 12)));
		}
		y += ROW_H;
		total += ROW_H;
		for (TradeEntry e : trades) {
			int x = leftPos + INPUT_X;
			int w = INPUT_W;
			int pick = Math.max(22, font.width(e.addInvBuyBtn().getMessage()) + 10);
			int countW = 28;
			int idW = Math.max(36, w - countW - pick - 8);
			e.buyItem().setX(x);
			e.buyItem().setY(y);
			e.buyItem().setWidth(idW);
			e.buyCount().setX(x + idW + 4);
			e.buyCount().setY(y);
			e.buyCount().setWidth(countW);
			e.addInvBuyBtn().setPosition(x + w - pick, y);
			e.addInvBuyBtn().setWidth(pick);
			e.addInvBuyBtn().setHeight(16);
			int yB = y + 22;
			e.buyBItem().setX(x);
			e.buyBItem().setY(yB);
			e.buyBItem().setWidth(idW);
			e.buyBCount().setX(x + idW + 4);
			e.buyBCount().setY(yB);
			e.buyBCount().setWidth(countW);
			e.addInvBuyBBtn().setPosition(x + w - pick, yB);
			e.addInvBuyBBtn().setWidth(pick);
			e.addInvBuyBBtn().setHeight(16);
			int y2 = y + 44;
			int usesW = 28;
			int sellIdW = Math.max(28, w - countW - usesW - pick * 2 - 16);
			e.sellItem().setX(x);
			e.sellItem().setY(y2);
			e.sellItem().setWidth(sellIdW);
			e.sellCount().setX(x + sellIdW + 4);
			e.sellCount().setY(y2);
			e.sellCount().setWidth(countW);
			e.maxUses().setX(x + sellIdW + countW + 8);
			e.maxUses().setY(y2);
			e.maxUses().setWidth(usesW);
			e.addInvSellBtn().setPosition(x + w - pick * 2 - 4, y2);
			e.addInvSellBtn().setWidth(pick);
			e.addInvSellBtn().setHeight(16);
			e.removeBtn().setPosition(x + w - pick, y2);
			e.removeBtn().setWidth(pick);
			e.removeBtn().setHeight(16);
			y += TRADE_H;
			total += TRADE_H;
		}
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

	private void placeCheck(Checkbox box, int y) {
		box.setX(leftPos + INPUT_X);
		box.setY(y);
		box.setWidth(20);
	}

	private void layoutPreview() {
		previewX = leftPos + RIGHT_X;
		previewY = topPos + CONTENT_TOP;
		previewW = Math.max(64, WIDTH - RIGHT_X - 8);
		int full = Math.max(64, CONTENT_BOTTOM - CONTENT_TOP);
		int gap = 6;
		int tradeMin = 40;
		int villagerMin = 56;
		if (full < villagerMin + gap + tradeMin) {
			tradePreviewH = Math.max(32, full * 38 / 100);
			previewH = Math.max(32, full - gap - tradePreviewH);
		} else {
			previewH = Mth.clamp(full * 58 / 100, villagerMin, full - gap - tradeMin);
			tradePreviewH = full - previewH - gap;
		}
		tradePreviewY = previewY + previewH + gap;
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

	private boolean handleFooterClick(double mx, double my, int button) {
		if (!UiTheme.inFooterBar(mx, my, leftPos, topPos, WIDTH, HEIGHT, FOOTER_H)) return false;
		if (UiTheme.clickWidgets(mx, my, button, copyBtn, summonBtn, saveBtn)) return true;
		if (button == 0) setFocused(null);
		return true;
	}

	private void updateTypeSuggestions() {
		if (restoring || typeField == null || !typeField.isFocused()) {
			typeSuggestions = List.of();
			typeSuggestionIdx = -1;
			return;
		}
		typeSuggestions = filterSuggestions(typeField.getValue(), true);
		typeSuggestionIdx = typeSuggestions.isEmpty() ? -1 : 0;
		suggestionScroll = SuggestionPopup.clampScroll(suggestionScroll, typeSuggestions.size());
	}

	private void updateProfessionSuggestions() {
		if (restoring || professionField == null || !professionField.isFocused()) {
			profSuggestions = List.of();
			profSuggestionIdx = -1;
			return;
		}
		profSuggestions = filterSuggestions(professionField.getValue(), false);
		profSuggestionIdx = profSuggestions.isEmpty() ? -1 : 0;
		suggestionScroll = SuggestionPopup.clampScroll(suggestionScroll, profSuggestions.size());
	}

	private List<Suggestion> filterSuggestions(String input, boolean type) {
		String raw = input == null ? "" : input.trim();
		String lower = raw.toLowerCase(Locale.ROOT);
		List<Suggestion> out = new ArrayList<>();
		if (type) {
			for (ResourceLocation id : BuiltInRegistries.VILLAGER_TYPE.keySet()) {
				String path = id.getPath();
				String cn = TYPE_CN.getOrDefault(path, path);
				if (raw.isEmpty() || hit(cn, id, lower)) out.add(new Suggestion(id.toString(), cn));
			}
		} else {
			for (ResourceLocation id : BuiltInRegistries.VILLAGER_PROFESSION.keySet()) {
				String path = id.getPath();
				String cn = PROF_CN.getOrDefault(path, path);
				if (raw.isEmpty() || hit(cn, id, lower)) out.add(new Suggestion(id.toString(), cn));
			}
		}
		if (!raw.isEmpty()) {
			for (Suggestion s : out) {
				if (s.label.equals(raw) || s.id.equals(raw) || s.id.endsWith(":" + raw)) return List.of();
			}
			out.sort(Comparator.comparingInt((Suggestion s) -> rank(s, lower)).thenComparing(Suggestion::label));
		} else {
			out.sort(Comparator.comparing(Suggestion::label));
		}
		return out;
	}

	private static boolean hit(String cn, ResourceLocation id, String lower) {
		return cn.toLowerCase(Locale.ROOT).contains(lower)
			|| id.toString().toLowerCase(Locale.ROOT).contains(lower)
			|| id.getPath().contains(lower);
	}

	private static int rank(Suggestion s, String lower) {
		String cn = s.label.toLowerCase(Locale.ROOT);
		if (cn.equals(lower)) return 0;
		if (cn.startsWith(lower)) return 1;
		if (s.id.endsWith(":" + lower) || s.id.equals(lower)) return 2;
		return 3;
	}

	private String resolveRegistryId(String input, boolean type, String fallback) {
		String raw = input == null ? "" : input.trim();
		if (raw.isEmpty()) return fallback;
		if (SafeIds.looksLikeId(raw)) {
			ResourceLocation id = SafeIds.tryParse(raw);
			if (id == null && raw.indexOf(':') < 0) id = SafeIds.tryParseItem(raw);
			if (id != null) {
				if (type && BuiltInRegistries.VILLAGER_TYPE.containsKey(id)) return id.toString();
				if (!type && BuiltInRegistries.VILLAGER_PROFESSION.containsKey(id)) return id.toString();
			}
		}
		String lower = raw.toLowerCase(Locale.ROOT);
		ResourceLocation exact = null;
		ResourceLocation unique = null;
		int hits = 0;
		var keys = type ? BuiltInRegistries.VILLAGER_TYPE.keySet() : BuiltInRegistries.VILLAGER_PROFESSION.keySet();
		for (ResourceLocation id : keys) {
			String cn = (type ? TYPE_CN : PROF_CN).getOrDefault(id.getPath(), id.getPath());
			if (cn.equals(raw) || cn.equalsIgnoreCase(raw)) {
				exact = id;
				break;
			}
			if (cn.toLowerCase(Locale.ROOT).contains(lower) || id.getPath().equals(lower)) {
				hits++;
				unique = id;
			}
		}
		if (exact != null) return exact.toString();
		if (hits == 1 && unique != null) return unique.toString();
		return fallback;
	}

	private String resolvedTypeId() {
		return resolveRegistryId(typeField != null ? typeField.getValue() : "", true, "minecraft:plains");
	}

	private String resolvedProfessionId() {
		return resolveRegistryId(professionField != null ? professionField.getValue() : "", false, "minecraft:none");
	}

	private int resolvedLevel() {
		int level = 1;
		try {
			level = Integer.parseInt(levelField != null ? levelField.getValue().trim() : "1");
		} catch (NumberFormatException ignored) {}
		return Mth.clamp(level, 1, 5);
	}

	private ResourceLocation resolveItemId(String raw) {
		if (raw == null) return null;
		String input = raw.trim();
		if (input.isEmpty()) return null;
		if (SafeIds.looksLikeId(input)) {
			ResourceLocation id = SafeIds.tryParseItem(input);
			if (id != null && BuiltInRegistries.ITEM.containsKey(id)) return id;
		}
		String lower = input.toLowerCase(Locale.ROOT);
		ResourceLocation exact = null;
		ResourceLocation unique = null;
		int hits = 0;
		for (ResourceLocation id : BuiltInRegistries.ITEM.keySet()) {
			String cn = BuiltInRegistries.ITEM.get(id).getDescription().getString().trim();
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

	private String buildVillagerNbt() {
		List<String> parts = new ArrayList<>();
		parts.add("VillagerData:{type:\"" + resolvedTypeId() + "\",profession:\"" + resolvedProfessionId()
			+ "\",level:" + resolvedLevel() + "}");
		List<String> recipes = new ArrayList<>();
		for (int i = 0; i < trades.size(); i++) {
			String recipe = buildRecipeNbt(i);
			if (recipe != null) recipes.add(recipe);
		}
		if (!recipes.isEmpty()) {
			parts.add("Offers:{Recipes:[" + String.join(",", recipes) + "]}");
		}
		parts.add("mapedit_keep_offers:1b");
		parts.add("Tags:[\"bj_mapedit.keep_offers\"]");
		parts.add("PersistenceRequired:1b");
		String name = customNameField != null ? customNameField.getValue().trim() : "";
		if (!name.isEmpty()) {
			parts.add("CustomName:'{\"text\":\"" + escapeJson(name) + "\"}'");
		}
		if (noAiBox != null && noAiBox.selected()) parts.add("NoAI:1b");
		if (silentBox != null && silentBox.selected()) parts.add("Silent:1b");
		if (noGravityBox != null && noGravityBox.selected()) parts.add("NoGravity:1b");
		if (babyBox != null && babyBox.selected()) parts.add("Age:-24000");
		for (NbtRow row : nbtRows) {
			String key = row.keyBox != null ? row.keyBox.getValue().trim() : "";
			String value = row.valueBox != null ? row.valueBox.getValue().trim() : "";
			if (key.isEmpty() || value.isEmpty()) continue;
			parts.add(key + ":" + value);
		}
		if (parts.isEmpty()) return "{}";
		return "{" + String.join(",", parts) + "}";
	}

	private String buildRecipeNbt(int i) {
		TradeEntry e = trades.get(i);
		ItemStack buy = resolveTradeStack(e.buyItem().getValue(), e.buyCount().getValue(), stackAt(tradeBuyStacks, i));
		ItemStack buyB = resolveTradeStack(e.buyBItem().getValue(), e.buyBCount().getValue(), stackAt(tradeBuyBStacks, i));
		ItemStack sell = resolveTradeStack(e.sellItem().getValue(), e.sellCount().getValue(), stackAt(tradeSellStacks, i));
		if (buy.isEmpty() || sell.isEmpty()) return null;
		int maxUses = 16;
		try {
			maxUses = Math.max(1, Integer.parseInt(e.maxUses().getValue().trim()));
		} catch (NumberFormatException ignored) {}
		ItemCost buyCost = toCost(buy);
		Optional<ItemCost> buyBCost = buyB.isEmpty() ? Optional.empty() : Optional.of(toCost(buyB));
		MerchantOffer offer = new MerchantOffer(buyCost, buyBCost, sell.copy(), 0, maxUses, 1, 0.05f, 0);
		return encodeOffer(offer);
	}

	private static ItemCost toCost(ItemStack stack) {
		DataComponentPredicate pred = stack.getComponentsPatch().isEmpty()
			? DataComponentPredicate.EMPTY
			: DataComponentPredicate.allOf(stack.getComponents());
		return new ItemCost(stack.getItemHolder(), Math.max(1, stack.getCount()), pred, stack.copy());
	}

	private static String encodeOffer(MerchantOffer offer) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) return null;
		var ops = mc.level.registryAccess().createSerializationContext(NbtOps.INSTANCE);
		Tag tag = MerchantOffer.CODEC.encodeStart(ops, offer).result().orElse(null);
		return tag == null ? null : GiveCommands.snbt(tag);
	}

	private static ItemStack stackAt(List<ItemStack> stacks, int i) {
		if (i < 0 || i >= stacks.size()) return ItemStack.EMPTY;
		ItemStack stack = stacks.get(i);
		return stack == null ? ItemStack.EMPTY : stack;
	}

	private static String stackSnbt(ItemStack stack) {
		Minecraft mc = Minecraft.getInstance();
		if (stack == null || stack.isEmpty() || mc.level == null) return "";
		Tag saved = stack.save(mc.level.registryAccess());
		return saved == null ? "" : GiveCommands.snbt(saved);
	}

	private static ItemStack stackFromSnbt(String snbt) {
		CompoundTag tag = parseTagOrNull(snbt);
		if (tag == null || Minecraft.getInstance().level == null) return ItemStack.EMPTY;
		return ItemStack.parse(Minecraft.getInstance().level.registryAccess(), tag).orElse(ItemStack.EMPTY);
	}

	private static String escapeJson(String s) {
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
			return Vec3.atBottomCenterOf(block.getBlockPos().relative(block.getDirection()));
		}
		return player.getEyePosition(1.0F).add(player.getViewVector(1.0F).scale(3.0));
	}

	private static String fmt(double v) {
		return String.format(Locale.ROOT, "%.3f", v);
	}

	private void summonVillager() {
		if (Minecraft.getInstance().player == null) return;
		Vec3 at = lookPos();
		String cmd = "summon minecraft:villager " + fmt(at.x) + " " + fmt(at.y) + " " + fmt(at.z) + " " + buildVillagerNbt();
		ClientPlayNetworking.send(new ExecuteCommandsPayload(List.of(cmd)));
	}

	private void copySummonCommand() {
		Vec3 at = lookPos();
		GiveCommands.copy("/summon minecraft:villager " + fmt(at.x) + " " + fmt(at.y) + " " + fmt(at.z) + " " + buildVillagerNbt());
	}

	private SavedState captureState() {
		if (typeField == null) return null;
		List<String> keys = new ArrayList<>();
		List<String> values = new ArrayList<>();
		for (NbtRow row : nbtRows) {
			keys.add(row.keyBox != null ? row.keyBox.getValue() : "");
			values.add(row.valueBox != null ? row.valueBox.getValue() : "");
		}
		List<VillagerTemplateStore.TradeData> tradeData = new ArrayList<>();
		for (int i = 0; i < trades.size(); i++) {
			TradeEntry e = trades.get(i);
			VillagerTemplateStore.TradeData t = new VillagerTemplateStore.TradeData();
			t.buyId = e.buyItem().getValue();
			t.buyCount = e.buyCount().getValue();
			t.buyBId = e.buyBItem().getValue();
			t.buyBCount = e.buyBCount().getValue();
			t.sellId = e.sellItem().getValue();
			t.sellCount = e.sellCount().getValue();
			t.maxUses = e.maxUses().getValue();
			t.buyTag = stackSnbt(stackAt(tradeBuyStacks, i));
			t.buyBTag = stackSnbt(stackAt(tradeBuyBStacks, i));
			t.sellTag = stackSnbt(stackAt(tradeSellStacks, i));
			tradeData.add(t);
		}
		return new SavedState(
			typeField.getValue(), professionField.getValue(), levelField.getValue(), customNameField.getValue(),
			noAiBox.selected(), silentBox.selected(), noGravityBox.selected(), babyBox.selected(),
			keys, values, tradeData
		);
	}

	private void applyState(SavedState state) {
		if (state == null || typeField == null) return;
		restoring = true;
		typeField.setValue(state.type() == null ? "" : state.type());
		professionField.setValue(state.profession() == null ? "" : state.profession());
		levelField.setValue(state.level() == null || state.level().isBlank() ? "1" : state.level());
		customNameField.setValue(state.customName() == null ? "" : state.customName());
		setChecked(noAiBox, state.noAI());
		setChecked(silentBox, state.silent());
		setChecked(noGravityBox, state.noGravity());
		setChecked(babyBox, state.baby());
		for (NbtRow row : List.copyOf(nbtRows)) removeNbtRow(row);
		List<String> keys = state.nbtKeys() == null ? List.of() : state.nbtKeys();
		List<String> values = state.nbtValues() == null ? List.of() : state.nbtValues();
		int n = Math.min(keys.size(), values.size());
		for (int i = 0; i < n; i++) {
			addNbtField();
			NbtRow row = nbtRows.get(nbtRows.size() - 1);
			row.keyBox.setValue(keys.get(i));
			row.valueBox.setValue(values.get(i));
		}
		for (TradeEntry e : List.copyOf(trades)) removeTrade(e.removeBtn());
		if (state.trades() != null) {
			for (VillagerTemplateStore.TradeData t : state.trades()) {
				if (t == null) continue;
				addTrade();
				int idx = trades.size() - 1;
				TradeEntry e = trades.get(idx);
				e.buyItem().setValue(t.buyId == null ? "" : t.buyId);
				e.buyCount().setValue(t.buyCount == null || t.buyCount.isBlank() ? "1" : t.buyCount);
				e.buyBItem().setValue(t.buyBId == null ? "" : t.buyBId);
				e.buyBCount().setValue(t.buyBCount == null || t.buyBCount.isBlank() ? "1" : t.buyBCount);
				e.sellItem().setValue(t.sellId == null ? "" : t.sellId);
				e.sellCount().setValue(t.sellCount == null || t.sellCount.isBlank() ? "1" : t.sellCount);
				e.maxUses().setValue(t.maxUses == null || t.maxUses.isBlank() ? "16" : t.maxUses);
				tradeBuyStacks.set(idx, stackFromSnbt(t.buyTag));
				tradeBuyBStacks.set(idx, stackFromSnbt(t.buyBTag));
				tradeSellStacks.set(idx, stackFromSnbt(t.sellTag));
			}
		}
		restoring = false;
		lastPreviewSig = null;
		previewEntity = null;
		repositionAll();
	}

	private static CompoundTag parseTagOrNull(String snbt) {
		if (snbt == null || snbt.isBlank()) return null;
		try {
			return TagParser.parseTag(snbt);
		} catch (Exception ignored) {
			return null;
		}
	}

	private static void setChecked(Checkbox box, boolean want) {
		if (box != null && box.selected() != want) box.onPress();
	}

	private void saveTemplate() {
		commitTemplateRename();
		SavedState state = captureState();
		if (state == null) return;
		VillagerTemplateStore.Entry e = new VillagerTemplateStore.Entry();
		e.name = defaultTemplateName();
		e.type = state.type();
		e.profession = state.profession();
		e.level = state.level();
		e.customName = state.customName();
		e.noAI = state.noAI();
		e.silent = state.silent();
		e.noGravity = state.noGravity();
		e.baby = state.baby();
		e.keys = new ArrayList<>(state.nbtKeys());
		e.values = new ArrayList<>(state.nbtValues());
		e.trades = new ArrayList<>(state.trades());
		e.folder = TemplateOrg.norm(selectedFolder);
		VillagerTemplateStore.add(e);
		selectedTemplate = VillagerTemplateStore.all().size() - 1;
		updateTemplateScroll();
	}

	private void loadTemplate(int index) {
		VillagerTemplateStore.Entry e = VillagerTemplateStore.get(index);
		if (e == null) return;
		selectedTemplate = index;
		selectedFolder = TemplateOrg.norm(e.folder);
		applyState(new SavedState(e.type, e.profession, e.level, e.customName,
			e.noAI, e.silent, e.noGravity, e.baby, e.keys, e.values, e.trades));
	}

	private String defaultTemplateName() {
		ResourceLocation profId = ResourceLocation.tryParse(resolvedProfessionId());
		ResourceLocation typeId = ResourceLocation.tryParse(resolvedTypeId());
		String prof = PROF_CN.getOrDefault(profId != null ? profId.getPath() : "none", "村民");
		String type = TYPE_CN.getOrDefault(typeId != null ? typeId.getPath() : "plains", "");
		String base = (type + prof).isBlank() ? "村民" : type + prof;
		String name = base;
		int n = 2;
		while (templateNameTaken(name)) name = base + n++;
		return name;
	}

	private boolean templateNameTaken(String name) {
		String folder = TemplateOrg.norm(selectedFolder);
		for (VillagerTemplateStore.Entry e : VillagerTemplateStore.all()) {
			if (TemplateOrg.folderEq(e.folder, folder) && e.name != null && e.name.equals(name)) return true;
		}
		return false;
	}

	private void resetFields() {
		if (typeField == null) return;
		restoring = true;
		typeField.setValue("");
		professionField.setValue("");
		levelField.setValue("1");
		customNameField.setValue("");
		setChecked(noAiBox, false);
		setChecked(silentBox, false);
		setChecked(noGravityBox, false);
		setChecked(babyBox, false);
		for (NbtRow row : List.copyOf(nbtRows)) removeNbtRow(row);
		for (TradeEntry e : List.copyOf(trades)) removeTrade(e.removeBtn());
		savedState = null;
		sessionSelectedTemplate = -1;
		sessionSelectedFolder = "";
		selectedTemplate = -1;
		selectedFolder = "";
		scrollOffset = 0;
		previewEntity = null;
		lastPreviewSig = null;
		previewRotY = 0;
		previewRotX = 0;
		typeSuggestions = List.of();
		profSuggestions = List.of();
		closeInvPicker();
		restoring = false;
		repositionAll();
	}

	@Override
	public void removed() {
		if (typeField != null) {
			savedState = captureState();
			sessionSelectedTemplate = selectedTemplate;
			sessionSelectedFolder = selectedFolder;
		}
		super.removed();
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (scrollSuggestionPopup(mouseX, mouseY, scrollY)) return true;
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
		renderPreviewVillager(g);
		UiTheme.drawPreviewFrame(g, previewX, tradePreviewY, previewW, tradePreviewH);
		renderTradePreview(g);
		if (copyBtn != null) copyBtn.render(g, mouseX, mouseY, partialTick);
		if (summonBtn != null) summonBtn.render(g, mouseX, mouseY, partialTick);
		if (saveBtn != null) saveBtn.render(g, mouseX, mouseY, partialTick);
		UiTheme.drawThinScrollBar(g, leftPos + RIGHT_X - 6, topPos + CONTENT_TOP, CONTENT_BOTTOM - CONTENT_TOP, scrollOffset, maxScroll);
		g.flush();
		UiTheme.pushOverlay(g);
		drawSuggestions(g, mouseX, mouseY);
		templateMenu.draw(g, font, mouseX, mouseY);
		UiTheme.popOverlay(g);
		if (invPicker.isOpen()) {
			invPicker.render(g, font, this.width, this.height, mouseX, mouseY);
		} else if (refreshHover) {
			g.renderTooltip(font, Component.translatable("screen.bj_mapedit.reset"), mouseX, mouseY);
		}
	}

	private void drawFormLabels(GuiGraphics g) {
		labelIfVisible(g, "类型", typeField);
		labelIfVisible(g, "职业", professionField);
		labelIfVisible(g, "等级", levelField);
		labelIfVisible(g, "名称", customNameField);
		labelIfVisible(g, "无AI", noAiBox);
		labelIfVisible(g, "无声", silentBox);
		labelIfVisible(g, "无重力", noGravityBox);
		labelIfVisible(g, "幼年", babyBox);
		for (NbtRow row : nbtRows) {
			if (row.valueBox == null) continue;
			int wy = row.valueBox.getY() + 4;
			if (wy + 10 <= topPos + CONTENT_TOP || wy >= topPos + CONTENT_BOTTOM) continue;
			if (overlapsSuggestion(leftPos + LABEL_X, wy, 56, 10)) continue;
			UiTheme.label(g, font, "自定义", leftPos + LABEL_X, wy);
		}
		if (addTradeBtn != null) {
			int wy = addTradeBtn.getY() + 4;
			if (wy + 10 > topPos + CONTENT_TOP && wy < topPos + CONTENT_BOTTOM
				&& !overlapsSuggestion(leftPos + LABEL_X, wy, 56, 10)) {
				UiTheme.label(g, font, "交易", leftPos + LABEL_X, wy);
			}
		}
		for (TradeEntry e : trades) {
			int by = e.buyItem().getY() + 4;
			int bby = e.buyBItem().getY() + 4;
			int sy = e.sellItem().getY() + 4;
			if (by + 10 > topPos + CONTENT_TOP && by < topPos + CONTENT_BOTTOM
				&& !overlapsSuggestion(leftPos + LABEL_X, by, 56, 10)) {
				UiTheme.label(g, font, "买入", leftPos + LABEL_X, by);
			}
			if (bby + 10 > topPos + CONTENT_TOP && bby < topPos + CONTENT_BOTTOM
				&& !overlapsSuggestion(leftPos + LABEL_X, bby, 56, 10)) {
				UiTheme.label(g, font, "买入2", leftPos + LABEL_X, bby);
			}
			if (sy + 10 > topPos + CONTENT_TOP && sy < topPos + CONTENT_BOTTOM
				&& !overlapsSuggestion(leftPos + LABEL_X, sy, 56, 10)) {
				UiTheme.label(g, font, "卖出", leftPos + LABEL_X, sy);
			}
		}
	}

	private void labelIfVisible(GuiGraphics g, String text, AbstractWidget w) {
		if (w == null) return;
		int y = w.getY() + 4;
		if (y + 10 <= topPos + CONTENT_TOP || y >= topPos + CONTENT_BOTTOM) return;
		if (overlapsSuggestion(leftPos + LABEL_X, y, 56, 10)) return;
		UiTheme.label(g, font, text, leftPos + LABEL_X, y);
	}

	private boolean coversSuggestionList(AbstractWidget w) {
		EditBox anchor = suggestionAnchor();
		if (anchor == null || w == anchor) return false;
		return overlapsSuggestion(w.getX(), w.getY(), w.getWidth(), w.getHeight());
	}

	private boolean overlapsSuggestion(int x, int y, int w, int h) {
		EditBox field = suggestionAnchor();
		List<Suggestion> list = suggestionList();
		if (field == null || list.isEmpty()) return false;
		int sx = field.getX();
		int sy = SuggestionPopup.listY(field, list.size(), topPos + CONTENT_BOTTOM);
		int sw = field.getWidth();
		int sh = SuggestionPopup.boxH(list.size());
		return x < sx + sw && x + w > sx && y < sy + sh && y + h > sy;
	}

	private void renderPreviewVillager(GuiGraphics g) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) {
			g.drawCenteredString(font, "无法预览", previewX + previewW / 2, previewY + previewH / 2 - 4, UiTheme.cMuted());
			return;
		}
		Entity entity = getPreviewVillager();
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
			}
		} catch (Throwable ignored) {
			g.drawCenteredString(font, "无法预览", previewX + previewW / 2, previewY + previewH / 2 - 4, UiTheme.cMuted());
		} finally {
			g.disableScissor();
			Lighting.setupFor3DItems();
		}
	}

	private Entity getPreviewVillager() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) return null;
		String sig = buildVillagerNbt();
		if (previewEntity != null && sig.equals(lastPreviewSig)) return previewEntity;
		previewEntity = null;
		lastPreviewSig = sig;
		try {
			Villager created = EntityType.VILLAGER.create(mc.level);
			if (created == null) return null;
			created.setPos(0.0, 0.0, 0.0);
			CompoundTag tag = TagParser.parseTag(sig);
			if (tag != null) created.load(tag);
			previewEntity = created;
			return previewEntity;
		} catch (Throwable ignored) {
			previewEntity = null;
			lastPreviewSig = null;
			return null;
		}
	}

	private void renderTradePreview(GuiGraphics g) {
		int x = previewX;
		int y = tradePreviewY;
		int w = previewW;
		int h = tradePreviewH;
		int clipL = x + 2;
		int clipT = y + 2;
		int clipR = x + w - 2;
		int clipB = y + h - 2;
		g.enableScissor(clipL, clipT, clipR, clipB);
		UiTheme.clipLabel(g, font, "交易预览", x + 6, y + 4, w - 12, UiTheme.cMuted());
		if (trades.isEmpty()) {
			g.drawCenteredString(font, "还没有交易", x + w / 2, y + h / 2 - 2, UiTheme.cMuted());
			g.disableScissor();
			return;
		}
		int slot = 18;
		int pad = w < 80 ? 4 : 8;
		int titleH = 16;
		int more = trades.size() > 1 ? 12 : 4;
		int rowH = slot + 4;
		int top = y + titleH;
		int maxShow = Math.max(0, (clipB - more - top) / rowH);
		int shown = Math.min(trades.size(), maxShow);
		for (int i = 0; i < shown; i++) {
			TradeEntry e = trades.get(i);
			int ly = top + i * rowH;
			if (ly + slot > clipB - more) break;
			ItemStack buy = resolveTradeStack(e.buyItem().getValue(), e.buyCount().getValue(), stackAt(tradeBuyStacks, i));
			ItemStack buyB = resolveTradeStack(e.buyBItem().getValue(), e.buyBCount().getValue(), stackAt(tradeBuyBStacks, i));
			ItemStack sell = resolveTradeStack(e.sellItem().getValue(), e.sellCount().getValue(), stackAt(tradeSellStacks, i));
			boolean two = !buyB.isEmpty();
			int arrowW = 10;
			int used = two ? slot * 3 + 4 + arrowW : slot * 2 + arrowW;
			int buyX = x + Math.max(pad, (w - used) / 2);
			int midX = buyX + slot + (two ? slot + 4 : 0);
			int sellX = midX + arrowW;
			UiTheme.drawSlot(g, buyX, ly);
			if (!buy.isEmpty()) {
				g.renderItem(buy, buyX + 1, ly + 1);
				g.renderItemDecorations(font, buy, buyX + 1, ly + 1);
			}
			if (two) {
				int buyBX = buyX + slot + 2;
				UiTheme.drawSlot(g, buyBX, ly);
				g.renderItem(buyB, buyBX + 1, ly + 1);
				g.renderItemDecorations(font, buyB, buyBX + 1, ly + 1);
			}
			g.drawString(font, ">", midX + 1, ly + 5, UiTheme.cMuted(), false);
			UiTheme.drawSlot(g, sellX, ly);
			if (!sell.isEmpty()) {
				g.renderItem(sell, sellX + 1, ly + 1);
				g.renderItemDecorations(font, sell, sellX + 1, ly + 1);
			}
		}
		if (trades.size() > shown) {
			UiTheme.muted(g, font, "共" + trades.size() + "项", x + 6, y + h - 13);
		}
		g.disableScissor();
	}

	private ItemStack resolveTradeStack(String idText, String countText, ItemStack locked) {
		int count = 1;
		try {
			count = Math.max(1, Integer.parseInt(countText == null ? "1" : countText.trim()));
		} catch (Exception ignored) {}
		if (locked != null && !locked.isEmpty()) {
			ResourceLocation lockedId = BuiltInRegistries.ITEM.getKey(locked.getItem());
			ResourceLocation fieldId = resolveItemId(idText);
			String typed = idText == null ? "" : idText.trim();
			if (fieldId == null || lockedId.equals(fieldId) || lockedId.toString().equals(typed)) {
				return locked.copyWithCount(count);
			}
		}
		ResourceLocation id = resolveItemId(idText);
		if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) return ItemStack.EMPTY;
		return new ItemStack(BuiltInRegistries.ITEM.get(id), count);
	}

	private void drawSuggestions(GuiGraphics g, int mouseX, int mouseY) {
		if (!typeSuggestions.isEmpty() && typeField != null && typeField.isFocused()) {
			drawSuggestionBox(g, typeField, typeSuggestions, typeSuggestionIdx, mouseX, mouseY);
		}
		if (!profSuggestions.isEmpty() && professionField != null && professionField.isFocused()) {
			drawSuggestionBox(g, professionField, profSuggestions, profSuggestionIdx, mouseX, mouseY);
		}
	}

	private void drawSuggestionBox(GuiGraphics g, EditBox field, List<Suggestion> list, int sel, int mouseX, int mouseY) {
		int sx = field.getX();
		int sy = SuggestionPopup.listY(field, list.size(), topPos + CONTENT_BOTTOM);
		SuggestionPopup.draw(g, font, sx, sy, field.getWidth(), list.size(), suggestionScroll, sel, mouseX, mouseY, i -> list.get(i).label());
	}

	private boolean scrollSuggestionPopup(double mouseX, double mouseY, double scrollY) {
		EditBox field = suggestionAnchor();
		List<Suggestion> list = suggestionList();
		if (field == null || list.isEmpty() || SuggestionPopup.maxScroll(list.size()) <= 0) return false;
		suggestionScroll = SuggestionPopup.scrollBy(suggestionScroll, list.size(), scrollY);
		return true;
	}

	private boolean pressSuggestionBar(double mouseX, double mouseY) {
		EditBox field = suggestionAnchor();
		List<Suggestion> list = suggestionList();
		if (field == null || list.isEmpty()) return false;
		int sy = SuggestionPopup.listY(field, list.size(), topPos + CONTENT_BOTTOM);
		Integer next = SuggestionPopup.pressBar(field.getX(), sy, field.getWidth(), list.size(), suggestionScroll, mouseX, mouseY);
		if (next == null) return false;
		suggestionScroll = next;
		return true;
	}

	private EditBox suggestionAnchor() {
		if (!typeSuggestions.isEmpty() && typeField != null && typeField.isFocused()) return typeField;
		if (!profSuggestions.isEmpty() && professionField != null && professionField.isFocused()) return professionField;
		return null;
	}

	private List<Suggestion> suggestionList() {
		if (suggestionAnchor() == typeField) return typeSuggestions;
		if (suggestionAnchor() == professionField) return profSuggestions;
		return List.of();
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		draggingPreview = false;
		if (invPicker.isOpen()) {
			if (button == 0) {
				ItemStack picked = invPicker.stackAt(this.width, this.height, mouseX, mouseY);
				if (!picked.isEmpty()) applyInvPickerItem(picked);
				else if (!invPicker.inPanel(this.width, this.height, mouseX, mouseY)) closeInvPicker();
			} else if (button == 1) {
				closeInvPicker();
			}
			return true;
		}
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
		if (button == 0 && trySuggestionClick(mouseX, mouseY, typeSuggestions, typeField, true)) return true;
		if (button == 0 && trySuggestionClick(mouseX, mouseY, profSuggestions, professionField, false)) return true;
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
		if (handleFooterClick(mouseX, mouseY, button)) return true;
		boolean result = super.mouseClicked(mouseX, mouseY, button);
		if (button == 0) refreshSuggestionsAfterClick(mouseX, mouseY);
		if (button == 0 && !result) setFocused(null);
		return result;
	}

	private void refreshSuggestionsAfterClick(double mouseX, double mouseY) {
		if (SuggestionPopup.clickOpens(typeField, mouseX, mouseY)) updateTypeSuggestions();
		else {
			typeSuggestions = List.of();
			typeSuggestionIdx = -1;
		}
		if (SuggestionPopup.clickOpens(professionField, mouseX, mouseY)) updateProfessionSuggestions();
		else {
			profSuggestions = List.of();
			profSuggestionIdx = -1;
		}
	}

	private boolean trySuggestionClick(double mouseX, double mouseY, List<Suggestion> list, EditBox field, boolean type) {
		if (list.isEmpty() || field == null || !field.isFocused()) return false;
		int sx = field.getX();
		int sy = SuggestionPopup.listY(field, list.size(), topPos + CONTENT_BOTTOM);
		int idx = SuggestionPopup.hitIndex(mouseX, mouseY, sx, sy, field.getWidth(), list.size(), suggestionScroll);
		if (idx < 0) return false;
		field.setValue(list.get(idx).label());
		if (type) {
			typeSuggestions = List.of();
			typeSuggestionIdx = -1;
		} else {
			profSuggestions = List.of();
			profSuggestionIdx = -1;
		}
			return true;
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

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (invPicker.isOpen() && keyCode == 256) {
			closeInvPicker();
			return true;
		}
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
		if (handleSuggestionKeys(keyCode, typeSuggestions, typeField, true)) return true;
		if (handleSuggestionKeys(keyCode, profSuggestions, professionField, false)) return true;
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	private boolean handleSuggestionKeys(int keyCode, List<Suggestion> list, EditBox field, boolean type) {
		if (list.isEmpty() || field == null || !field.isFocused()) return false;
			if (keyCode == 258 || keyCode == 257) {
			int idx = type ? typeSuggestionIdx : profSuggestionIdx;
			field.setValue(list.get(Math.max(0, idx)).label());
			if (type) {
				typeSuggestions = List.of();
				typeSuggestionIdx = -1;
			} else {
				profSuggestions = List.of();
				profSuggestionIdx = -1;
			}
				return true;
			}
			if (keyCode == 264) {
			if (type) typeSuggestionIdx = Math.min(typeSuggestionIdx + 1, list.size() - 1);
			else profSuggestionIdx = Math.min(profSuggestionIdx + 1, list.size() - 1);
			int idx = type ? typeSuggestionIdx : profSuggestionIdx;
			suggestionScroll = SuggestionPopup.keepVisible(idx, suggestionScroll, list.size());
				return true;
			}
			if (keyCode == 265) {
			if (type) typeSuggestionIdx = Math.max(0, typeSuggestionIdx - 1);
			else profSuggestionIdx = Math.max(0, profSuggestionIdx - 1);
			int idx = type ? typeSuggestionIdx : profSuggestionIdx;
			suggestionScroll = SuggestionPopup.keepVisible(idx, suggestionScroll, list.size());
				return true;
			}
		if (keyCode == 256) {
			if (type) {
			typeSuggestions = List.of();
			typeSuggestionIdx = -1;
			} else {
			profSuggestions = List.of();
			profSuggestionIdx = -1;
			}
			return true;
		}
		return false;
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
		return VillagerTemplateStore.rows(collapsedFolders);
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
		TemplateRailUi.drawHeader(g, font, Component.translatable("screen.bj_mapedit.villager_templates").getString(), x, w, topPos, hx, hy, hasSel);
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
			VillagerTemplateStore.moveToFolder(index, templateDrag.hoverFolder);
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
		String name = VillagerTemplateStore.addFolder();
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
		VillagerTemplateStore.Entry kept = selectedTemplate >= 0 ? VillagerTemplateStore.get(selectedTemplate) : null;
		VillagerTemplateStore.removeFolder(name);
		collapsedFolders.remove(TemplateOrg.norm(name));
		if (TemplateOrg.folderEq(selectedFolder, name)) {
			selectedFolder = "";
			selectedTemplate = -1;
		} else if (kept != null) {
			selectedTemplate = VillagerTemplateStore.all().indexOf(kept);
		}
		updateTemplateScroll();
	}

	private void deleteTemplate(int index) {
		if (renamingTemplate == index) cancelTemplateRename();
		else commitTemplateRename();
		VillagerTemplateStore.remove(index);
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
		VillagerTemplateStore.Entry entry = VillagerTemplateStore.get(index);
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
			VillagerTemplateStore.renameFolder(from, to);
			String normTo = TemplateOrg.norm(to);
			if (!normTo.isEmpty() && !TemplateOrg.folderEq(from, normTo)) {
				TemplateOrg.renameCollapsed(collapsedFolders, from, normTo);
				if (TemplateOrg.folderEq(selectedFolder, from)) selectedFolder = normTo;
			}
			cancelTemplateRename();
			return;
		}
		if (renamingTemplate < 0) return;
		VillagerTemplateStore.rename(renamingTemplate, templateRenameBox.getValue());
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
