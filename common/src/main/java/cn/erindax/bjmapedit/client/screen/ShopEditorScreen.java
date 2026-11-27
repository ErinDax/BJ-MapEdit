package cn.erindax.bjmapedit.client.screen;

import cn.erindax.bjmapedit.client.DatapackNet;
import cn.erindax.bjmapedit.client.EditorAccess;
import cn.erindax.bjmapedit.client.widget.CommandSuggestionHost;
import cn.erindax.bjmapedit.client.widget.GiveCommands;
import cn.erindax.bjmapedit.client.widget.SectionText;
import cn.erindax.bjmapedit.client.widget.ShopCompat;
import cn.erindax.bjmapedit.client.widget.ShopData;
import cn.erindax.bjmapedit.client.widget.ShopGuiArt;
import cn.erindax.bjmapedit.client.widget.ShopPack;
import cn.erindax.bjmapedit.client.widget.UiTheme;
import cn.erindax.bjmapedit.networking.DatapackOps;
import cn.erindax.bjmapedit.networking.payload.DatapackOpPayload;
import cn.erindax.bjmapedit.networking.payload.DatapackOpResultPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.lang.ref.WeakReference;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class ShopEditorScreen extends Screen implements CommandSuggestionHost {
	private static final String K = "screen.bj_mapedit.";
	private static final Identifier CHEST = Identifier.withDefaultNamespace("textures/gui/container/generic_54.png");
	private static final int PAD = 8;
	private static final int CHEST_W = 176;
	private static final int CHEST_H = 167;
	private static final int ROW_H = 12;
	private static final int MENU_ROW_H = 14;
	private static final int TYPE_H = 14;
	private static final int FOOTER_H = 26;
	private static final int FIXED_H = 232;
	private static final int MAX_PRICES = 8;
	private static final int TAB_SLOT = 0;
	private static final int TAB_SHOP = 1;
	private static final int TAB_LIST = 2;
	private static final int OK = 0xFF7EE787;
	private static final int ERR = 0xFFFF7B72;
	private static final int HINT = 0x6E7681;
	private static final int INK = 0xFF404040;
	private static final int SELECTED = 0xFF1F6FEB;
	private static final int WARN = 0xFFD29922;
	private static final int PAGE_BTN = 9;
	private static final int[] TYPES = {
		ShopData.SELL, ShopData.BUY, ShopData.DECOR, ShopData.NAV,
		ShopData.ACTION, ShopData.CYCLE, ShopData.SELECT, ShopData.RESET
	};
	private static final int[] TYPE_COLORS = {
		0, 0xFF6E7681, 0xFF2EA043, 0xFFD29922, 0xFF388BFD,
		0xFFA371F7, 0xFF39C5CF, 0xFFEC6CB9, 0xFFF85149
	};
	private static final String[] TAB_KEYS = {"shop_tab_slot", "shop_tab_shop", "shop_tab_list"};
	private static final String[] MENU_KEYS = {"shop_menu_item", "shop_menu_xp", "shop_menu_score"};
	private static final String[] SHOP_LABELS = {"shop_name", "shop_id", "shop_container", "shop_ok_text", "shop_fail_text", "shop_strict"};
	private static final String[] NAV_KEYS = {"shop_nav_prev", "shop_nav_next", "shop_nav_goto"};
	private static final String[] PAGE_GLYPHS = {"<", ">", "+", "-"};
	private static final String[] PAGE_TIPS = {"shop_page_prev", "shop_page_next", "shop_page_add", "shop_page_delete"};

	private static ShopData draft;
	private static String generatedJson;
	private static WeakReference<Object> draftOwner = new WeakReference<>(null);
	private static int guiPrevSeq;

	private final ItemStack[] icons = new ItemStack[ShopData.BIG_SLOTS];
	private DynamicTexture guiPreviewTex;
	private Identifier guiPreviewId;
	private int guiPreviewH;
	private boolean guiPreviewStale = true;
	private ShopData shop;
	private List<String> shops = new ArrayList<>();
	private List<Component> preview = List.of();
	private ItemStack carried = ItemStack.EMPTY;
	private boolean iconsStale = true;
	private boolean treeLoaded;
	private boolean opened;
	private boolean syncing;
	private boolean busy;
	private boolean pickPrice;
	private boolean pickOption;
	private boolean menuOpen;
	private boolean addingScore;
	private boolean confirmPage;
	private boolean confirmReset;
	private int pendingLost = -1;
	private int busyTicks;
	private int tokenSeq;
	private int sel;
	private int tab;
	private int shopScroll;
	private int priceScroll;
	private int optScroll;
	private int selOpt;
	private int dragSlot = -1;
	private int invPress = -1;
	private int editingPrice = -1;
	private String token = "";
	private String failKey;
	private String pendingJson;
	private String pendingId = "";
	private String loadingPath;
	private String loadingId = "";
	private String deletingId;
	private String confirmDelete;
	private Runnable afterReload;
	private Component status = Component.empty();
	private int statusColor = OK;
	private String snapObjective = "";

	private EditBox idField;
	private EditBox titleField;
	private EditBox countField;
	private EditBox amountField;
	private EditBox objectiveField;
	private EditBox commandField;
	private EditBox offCommandField;
	private EditBox optLabelField;
	private EditBox okField;
	private EditBox failField;
	private CommandSuggestions cmdSuggestions;
	private CommandSuggestions offCmdSuggestions;
	private Button containerBtn;
	private Button soundBtn;
	private Button strictBtn;
	private Button fillBtn;
	private Button copyBtn;
	private Button unbindBtn;
	private Button resetShopBtn;
	private Button clearBtn;
	private Button addPriceBtn;
	private Button addOptBtn;
	private Button selectDefBtn;
	private Button scoreOkBtn;
	private Button newBtn;
	private Button paintBtn;
	private Button placeBtn;

	private int leftPos;
	private int topPos;
	private int panelW;
	private int panelH;
	private int refreshX;
	private int refreshY;
	private int bodyY;
	private int bodyH;
	private int chestX;
	private int chestY;
	private int chestH = CHEST_H;
	private int viewRows = 3;
	private int invOff;
	private int rowScroll;
	private int rightX;
	private int rightW;
	private int contentY;
	private int listRows;
	private int shopRows;
	private int priceTop;
	private int previewTop;
	private int previewH;
	private int footerY;
	private int tabW;
	private int menuW;
	private int pageLabelW;
	private final int[] typeXs = new int[TYPES.length];
	private final int[] typeYs = new int[TYPES.length];
	private final int[] typeWs = new int[TYPES.length];
	private final int[] navXs = new int[NAV_KEYS.length];
	private final int[] navWs = new int[NAV_KEYS.length];
	private int selMode0X, selMode0W, selMode1X, selMode1W, selGrpMinusX, selGrpPlusX;
	private int genX;
	private int genW;

	public ShopEditorScreen() {
		super(Component.translatable(K + "shop_title"));
		Object owner = Minecraft.getInstance().getConnection();
		if (draft == null || draftOwner.get() != owner) {
			draft = new ShopData();
			draftOwner = new WeakReference<>(owner);
			generatedJson = null;
		}
		shop = draft;
		for (int i = 0; i < icons.length; i++) icons[i] = ItemStack.EMPTY;
	}

	@Override
	protected void init() {
		if (objectiveField != null) snapObjective = objectiveField.getValue();
		editingPrice = -1;
		menuOpen = false;
		layout();
		titleField = field(64, "shop_name_hint", 16);
		titleField.setResponder(s -> {
			if (syncing) return;
			shop.title = s;
			changed();
		});
		idField = field(24, "shop_id_hint", 16);
		idField.setFilter(s -> s.matches("[a-z0-9_]{0,24}"));
		idField.setResponder(s -> {
			if (syncing) return;
			shop.id = s;
			changed();
		});
		containerBtn = button("shop_container_chest", this::cycleContainer, 16);
		soundBtn = button("shop_sound_on", this::toggleSound, 16);
		okField = field(128, "shop_ok_hint", 16);
		okField.setResponder(s -> {
			if (syncing) return;
			shop.okText = s;
			changed();
		});
		failField = field(128, "shop_fail_hint", 16);
		failField.setResponder(s -> {
			if (syncing) return;
			shop.failText = s;
			changed();
		});
		strictBtn = button("shop_strict_on", this::toggleStrict, 16);
		fillBtn = button("shop_fill", this::fillEmpty, 16);
		copyBtn = button("shop_copy", this::copyCommand, 16);
		unbindBtn = button("shop_unbind", this::unbind, 16);
		resetShopBtn = button("shop_reset_btn", this::resetCurrentShop, 16);
		clearBtn = button("shop_clear_slot", this::clearSlot, 16);
		countField = field(2, null, TYPE_H);
		countField.setFilter(s -> s.matches("\\d{0,2}"));
		countField.setResponder(s -> {
			if (syncing) return;
			slot().count = ShopData.clampCount(slot().item, parse(s, 1));
			changed();
		});
		addPriceBtn = button("shop_add_price", this::toggleMenu, 15);
		addOptBtn = button("shop_add_opt", this::clickAddOption, 15);
		selectDefBtn = button("shop_sel_def_off", this::toggleSelectDefault, 15);
		objectiveField = field(32, "shop_objective_hint", 15);
		objectiveField.setValue(snapObjective);
		scoreOkBtn = button("done", this::addScore, 15);
		amountField = field(6, null, ROW_H);
		amountField.setFilter(s -> s.matches("\\d{0,6}"));
		amountField.setResponder(s -> {
			if (syncing) return;
			List<ShopData.Price> prices = slot().prices;
			if (editingPrice < 0 || editingPrice >= prices.size()) return;
			prices.get(editingPrice).amount = Mth.clamp(parse(s, 1), 1, ShopData.MAX_AMOUNT);
			changed();
		});
		optLabelField = field(48, "shop_opt_label_hint", 15);
		optLabelField.setResponder(s -> {
			if (syncing) return;
			ShopData.Option o = curOpt();
			if (o != null) {
				o.label = s;
				changed();
			}
		});
		commandField = field(1024, "shop_command_hint", 15);
		cmdSuggestions = new CommandSuggestions(this.minecraft, this, commandField, font, true, false, 1, 6, false, 0xD0000000);
		cmdSuggestions.setAllowSuggestions(false);
		cmdSuggestions.setAllowHiding(false);
		commandField.setResponder(s -> {
			if (!syncing) {
				ShopData.Slot sl = slot();
				if (sl.type == ShopData.CYCLE) {
					ShopData.Option o = curOpt();
					if (o != null) o.command = s;
				} else {
					sl.command = s;
				}
				changed();
			}
			refreshCmdSuggestions();
		});
		offCommandField = field(1024, "shop_off_cmd_hint", 15);
		offCmdSuggestions = new CommandSuggestions(this.minecraft, this, offCommandField, font, true, false, 1, 6, false, 0xD0000000);
		offCmdSuggestions.setAllowSuggestions(false);
		offCmdSuggestions.setAllowHiding(false);
		offCommandField.setResponder(s -> {
			if (!syncing) {
				slot().offCommand = s;
				changed();
			}
			refreshCmdSuggestions();
		});
		newBtn = button("shop_new", this::newShop, 16);
		paintBtn = button("shop_paint_btn", this::openPaint, 20);
		placeBtn = button("shop_place", this::place, 20);
		guiPreviewStale = true;
		placeWidgets();
		syncAll();
		updateVisibility();
		if (!opened) {
			opened = true;
			requestList();
		}
	}

	private EditBox field(int maxLength, String hintKey, int height) {
		EditBox box = new EditBox(font, 0, 0, 40, height, Component.empty());
		box.setMaxLength(maxLength);
		if (hintKey != null) box.setHint(Component.translatable(K + hintKey).withStyle(Style.EMPTY.withColor(HINT)));
		return this.addRenderableWidget(box);
	}

	private Button button(String key, Runnable action, int height) {
		return this.addRenderableWidget(Button.builder(Component.translatable(K + key), b -> action.run()).bounds(0, 0, 40, height).build());
	}

	private void layout() {
		panelW = Mth.clamp(this.width - 16, 400, 480);
		int spare = this.height - 16 - FIXED_H;
		int extra = Mth.clamp(spare / ROW_H, 0, 4);
		viewRows = shop.large() ? Mth.clamp(3 + spare / 18, 3, 6) : 3;
		invOff = (viewRows - 3) * 18;
		chestH = CHEST_H + invOff;
		rowScroll = Mth.clamp(rowScroll, 0, maxRowScroll());
		listRows = 2 + (extra + 1) / 2;
		panelH = Math.max(FIXED_H + extra * ROW_H, FIXED_H + invOff);
		leftPos = (this.width - panelW) / 2;
		topPos = Math.max(4, (this.height - panelH) / 2);
		refreshX = leftPos + panelW - 18;
		refreshY = topPos + 3;
		bodyY = topPos + UiTheme.HEADER_H + 4;
		bodyH = panelH - UiTheme.HEADER_H - FOOTER_H - 8;
		chestX = leftPos + PAD;
		chestY = bodyY + (bodyH - chestH) / 2;
		rightX = chestX + CHEST_W + 8;
		rightW = leftPos + panelW - PAD - rightX;
		contentY = bodyY + 20;
		priceTop = contentY + 67;
		previewTop = priceTop + listRows * ROW_H + 20;
		previewH = bodyY + bodyH - previewTop;
		shopRows = Math.max(1, (bodyH - 40) / ROW_H);
		footerY = topPos + panelH - FOOTER_H + 3;
		tabW = (rightW - 4) / 3;
	}

	private int maxRowScroll() {
		return Math.max(0, shop.size() / 9 - viewRows);
	}

	private void reveal(int index) {
		int row = index / 9;
		if (row < rowScroll) rowScroll = row;
		else if (row >= rowScroll + viewRows) rowScroll = row - viewRows + 1;
		rowScroll = Mth.clamp(rowScroll, 0, maxRowScroll());
	}

	private void reshape() {
		layout();
		placeWidgets();
		sel = Math.min(sel, shop.size() - 1);
		reveal(sel);
	}

	private void put(AbstractWidget widget, int x, int y, int w) {
		widget.setPosition(x, y);
		widget.setWidth(w);
	}

	private int textW(Button button, int pad) {
		return font.width(button.getMessage()) + pad;
	}

	private void placeWidgets() {
		int x1 = rightX + rightW;
		int y = contentY;
		int labelW = 0;
		for (String key : SHOP_LABELS) labelW = Math.max(labelW, font.width(I18n.get(K + key)));
		int fx = rightX + labelW + 6;
		int fw = x1 - fx;
		put(titleField, fx, y, fw);
		put(idField, fx, y + 20, fw);
		int soundW = Math.max(font.width(I18n.get(K + "shop_sound_on")), font.width(I18n.get(K + "shop_sound_off"))) + 14;
		put(soundBtn, x1 - soundW, y + 40, soundW);
		put(containerBtn, fx, y + 40, Math.max(40, soundBtn.getX() - 4 - fx));
		put(okField, fx, y + 60, fw);
		put(failField, fx, y + 80, fw);
		put(strictBtn, fx, y + 100, fw);
		int qtr = (rightW - 9) / 4;
		put(fillBtn, rightX, y + 122, qtr);
		put(copyBtn, rightX + qtr + 3, y + 122, qtr);
		put(unbindBtn, rightX + 2 * (qtr + 3), y + 122, qtr);
		put(resetShopBtn, x1 - qtr, y + 122, qtr);

		int clearW = textW(clearBtn, 12);
		put(clearBtn, x1 - clearW, y, clearW);
		put(countField, x1 - 24, y + 18, 24);
		int row1X = rightX;
		int row2X = rightX;
		for (int i = 0; i < TYPES.length; i++) {
			int w = font.width(I18n.get(K + "shop_type_" + TYPES[i])) + 8;
			typeWs[i] = w;
			if (i < 4) {
				typeXs[i] = row1X;
				typeYs[i] = y + 18;
				row1X += w + 2;
			} else {
				typeXs[i] = row2X;
				typeYs[i] = y + 34;
				row2X += w + 2;
			}
		}
		int navX = rightX;
		for (int i = 0; i < NAV_KEYS.length; i++) {
			navXs[i] = navX;
			navWs[i] = font.width(I18n.get(K + NAV_KEYS[i])) + 12;
			navX += navWs[i] + 2;
		}
		pageLabelW = font.width(ShopData.MAX_PAGES + "/" + ShopData.MAX_PAGES);
		int addW = textW(addPriceBtn, 12);
		put(addPriceBtn, x1 - addW, y + 50, addW);
		int optAddW = textW(addOptBtn, 12);
		put(addOptBtn, x1 - optAddW, y + 50, optAddW);
		int defW = Math.max(font.width(I18n.get(K + "shop_sel_def_on")), font.width(I18n.get(K + "shop_sel_def_off"))) + 10;
		put(selectDefBtn, x1 - defW, y + 50, defW);
		selMode0X = rightX;
		selMode0W = font.width(I18n.get(K + "shop_sel_toggle")) + 10;
		selMode1X = selMode0X + selMode0W + 2;
		selMode1W = font.width(I18n.get(K + "shop_sel_group", 9)) + 10;
		selGrpMinusX = selMode1X + selMode1W + 2;
		selGrpPlusX = selGrpMinusX + 15;

		int okW = textW(scoreOkBtn, 12);
		put(scoreOkBtn, x1 - okW, y + 50, okW);
		int objX = rightX + Math.max(font.width(I18n.get(K + "shop_prices_2")), font.width(I18n.get(K + "shop_prices_3"))) + 6;
		put(objectiveField, objX, y + 50, Math.max(30, scoreOkBtn.getX() - 4 - objX));

		ShopData.Slot s = slot();
		int listBottomY = priceTop + listRows * ROW_H + 2;
		if (s.type == ShopData.CYCLE) {
			int lblW = Math.max(54, rightW * 2 / 5);
			put(optLabelField, rightX, listBottomY, lblW);
			put(commandField, rightX + lblW + 4, listBottomY, rightW - lblW - 4);
			commandField.setHint(Component.translatable(K + "shop_opt_cmd_hint").withStyle(Style.EMPTY.withColor(HINT)));
			previewTop = listBottomY + 18;
		} else if (s.type == ShopData.ACTION || s.type == ShopData.RESET) {
			put(commandField, rightX, y + 65, rightW);
			String hk = s.type == ShopData.ACTION ? "shop_action_cmd_hint" : "shop_reset_cmd_hint";
			commandField.setHint(Component.translatable(K + hk).withStyle(Style.EMPTY.withColor(HINT)));
			previewTop = y + 84;
		} else if (s.type == ShopData.SELECT) {
			put(commandField, rightX, y + 68, rightW);
			commandField.setHint(Component.translatable(K + "shop_sel_on_hint").withStyle(Style.EMPTY.withColor(HINT)));
			put(offCommandField, rightX, y + 86, rightW);
			previewTop = s.group <= 0 ? y + 104 : y + 98;
		} else {
			put(commandField, rightX, listBottomY, rightW);
			commandField.setHint(Component.translatable(K + "shop_command_hint").withStyle(Style.EMPTY.withColor(HINT)));
			previewTop = listBottomY + 18;
		}
		previewH = bodyY + bodyH - previewTop;

		menuW = 0;
		for (String key : MENU_KEYS) menuW = Math.max(menuW, font.width(I18n.get(K + key)) + 14);
		menuW = Math.max(menuW, addW);

		put(newBtn, rightX, bodyY + bodyH - 16, textW(newBtn, 20));

		genW = font.width(I18n.get(K + "shop_generate")) + 20;
		genX = leftPos + panelW - PAD - genW;
		int placeW = textW(placeBtn, 16);
		put(placeBtn, genX - 5 - placeW, footerY, placeW);
		int paintW = textW(paintBtn, 14);
		put(paintBtn, placeBtn.getX() - 5 - paintW, footerY, paintW);
	}

	private CommandSuggestions activeCmdSuggestions() {
		if (commandField != null && commandField.visible && commandField.isFocused()) return cmdSuggestions;
		if (offCommandField != null && offCommandField.visible && offCommandField.isFocused()) return offCmdSuggestions;
		return null;
	}

	private EditBox activeCmdBox() {
		if (commandField != null && commandField.visible && commandField.isFocused()) return commandField;
		if (offCommandField != null && offCommandField.visible && offCommandField.isFocused()) return offCommandField;
		return null;
	}

	private void refreshCmdSuggestions() {
		updateOneSuggestion(cmdSuggestions, commandField);
		updateOneSuggestion(offCmdSuggestions, offCommandField);
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
		if (box == null) box = commandField;
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

	private void show(AbstractWidget widget, boolean visible) {
		if (widget.visible == visible) return;
		widget.visible = visible;
		if (!visible && getFocused() == widget) setFocused(null);
	}

	private void updateVisibility() {
		ShopData.Slot s = slot();
		boolean slotTab = tab == TAB_SLOT && s.shown();
		boolean trading = slotTab && s.trading();
		boolean cyc = slotTab && s.type == ShopData.CYCLE;
		boolean selType = slotTab && s.type == ShopData.SELECT;
		boolean actOrRst = slotTab && (s.type == ShopData.ACTION || s.type == ShopData.RESET);
		boolean shopTab = tab == TAB_SHOP;
		if (!trading) {
			menuOpen = false;
			addingScore = false;
			editingPrice = -1;
		}
		if (!cyc) pickOption = false;
		if (editingPrice >= s.prices.size()) editingPrice = -1;
		boolean editing = editingPrice >= priceScroll && editingPrice < priceScroll + listRows;
		placeWidgets();
		show(clearBtn, slotTab);
		show(countField, slotTab && s.type != ShopData.CYCLE);
		show(addPriceBtn, trading && !addingScore);
		show(addOptBtn, cyc);
		show(selectDefBtn, selType);
		show(objectiveField, trading && addingScore);
		show(scoreOkBtn, trading && addingScore);
		show(optLabelField, cyc && curOpt() != null);
		show(commandField, trading || cyc || selType || actOrRst);
		show(offCommandField, selType && s.group <= 0);
		show(amountField, editing);
		if (editing) put(amountField, rightX + rightW - 66, priceTop + (editingPrice - priceScroll) * ROW_H, 36);
		show(titleField, shopTab);
		show(idField, shopTab);
		show(containerBtn, shopTab);
		show(soundBtn, shopTab);
		show(okField, shopTab);
		show(failField, shopTab);
		show(strictBtn, shopTab);
		show(fillBtn, shopTab);
		show(copyBtn, shopTab);
		show(unbindBtn, shopTab);
		show(resetShopBtn, shopTab);
		show(newBtn, tab == TAB_LIST);
		refreshCmdSuggestions();
	}

	private void setTab(int index) {
		tab = index;
		menuOpen = false;
		if (index != TAB_SLOT) {
			addingScore = false;
			editingPrice = -1;
			pickOption = false;
		}
		updateVisibility();
	}

	private ShopData.Slot slot() {
		return shop.slots[sel];
	}

	private ShopData.Option curOpt() {
		ShopData.Slot s = slot();
		if (s.options.isEmpty()) return null;
		selOpt = Mth.clamp(selOpt, 0, s.options.size() - 1);
		return s.options.get(selOpt);
	}

	private void changed() {
		iconsStale = true;
		confirmDelete = null;
		confirmPage = false;
		confirmReset = false;
	}

	private void drop() {
		carried = ItemStack.EMPTY;
		pickPrice = false;
		pickOption = false;
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

	private void setText(EditBox box, String value) {
		box.setValue(value == null ? "" : value);
		box.setCursorPosition(0);
		box.setHighlightPos(0);
	}

	private void syncAll() {
		syncing = true;
		setText(idField, shop.id);
		setText(titleField, shop.title);
		setText(okField, shop.okText);
		setText(failField, shop.failText);
		syncing = false;
		syncSlot();
	}

	private void syncSlot() {
		ShopData.Slot s = slot();
		if (s.type == ShopData.CYCLE) s.ensureCycleSeeded();
		selOpt = Mth.clamp(selOpt, 0, Math.max(0, s.options.size() - 1));
		syncing = true;
		setText(countField, String.valueOf(s.count));
		if (s.type == ShopData.CYCLE) {
			ShopData.Option o = curOpt();
			setText(optLabelField, o == null ? "" : o.label);
			setText(commandField, o == null ? "" : o.command);
		} else {
			setText(commandField, s.command);
		}
		setText(offCommandField, s.offCommand);
		syncing = false;
		priceScroll = 0;
		optScroll = Mth.clamp(optScroll, 0, Math.max(0, s.options.size() - listRows));
		editingPrice = -1;
		addingScore = false;
		menuOpen = false;
		pickOption = false;
		iconsStale = true;
		refreshButtons();
		updateVisibility();
	}

	private void syncCycleSelection() {
		ShopData.Option o = curOpt();
		syncing = true;
		setText(optLabelField, o == null ? "" : o.label);
		setText(commandField, o == null ? "" : o.command);
		syncing = false;
		iconsStale = true;
		updateVisibility();
	}

	private void refreshButtons() {
		containerBtn.setMessage(Component.translatable(K + "shop_container_" + shop.container));
		soundBtn.setMessage(Component.translatable(K + (shop.sound ? "shop_sound_on" : "shop_sound_off")));
		strictBtn.setMessage(Component.translatable(K + (shop.strict ? "shop_strict_on" : "shop_strict_off")));
		selectDefBtn.setMessage(Component.translatable(K + (slot().defaultOn ? "shop_sel_def_on" : "shop_sel_def_off")));
	}

	private void select(int index) {
		sel = Mth.clamp(index, 0, shop.size() - 1);
		selOpt = 0;
		reveal(sel);
		syncSlot();
	}

	private void gotoPage(int index) {
		int page = Mth.clamp(index, 0, shop.pages.size() - 1);
		confirmPage = false;
		if (page == shop.page) return;
		shop.setPage(page);
		dragSlot = -1;
		syncSlot();
	}

	private void addPage() {
		if (!shop.addPage()) {
			error(K + "shop_page_max", ShopData.MAX_PAGES);
			return;
		}
		dragSlot = -1;
		changed();
		syncSlot();
		setStatus(Component.empty(), OK);
	}

	private void removePage() {
		if (shop.pages.size() <= 1) {
			error(K + "shop_page_last");
			return;
		}
		if (!confirmPage && !shop.pageEmpty(shop.page)) {
			confirmPage = true;
			error(K + "shop_page_delete_confirm");
			return;
		}
		shop.removePage();
		dragSlot = -1;
		changed();
		syncSlot();
		setStatus(Component.translatable(K + "shop_page_deleted"), OK);
	}

	private void clickPageBtn(int which) {
		if (which == 0) gotoPage(shop.page - 1);
		else if (which == 1) gotoPage(shop.page + 1);
		else if (which == 2) addPage();
		else removePage();
	}

	private void toggleStrict() {
		shop.strict = !shop.strict;
		changed();
		refreshButtons();
	}

	private void toggleSelectDefault() {
		ShopData.Slot s = slot();
		s.defaultOn = !s.defaultOn;
		if (s.defaultOn && s.group > 0) {
			int size = shop.size();
			for (ShopData.Slot[] p : shop.pages) {
				for (int i = 0; i < size; i++) {
					if (p[i] != s && p[i].type == ShopData.SELECT && p[i].group == s.group) p[i].defaultOn = false;
				}
			}
		}
		changed();
		refreshButtons();
	}

	private void cycleContainer() {
		int idx = 0;
		for (int i = 0; i < ShopData.CONTAINERS.length; i++) {
			if (ShopData.CONTAINERS[i].equals(shop.container)) idx = i;
		}
		shop.container = ShopData.CONTAINERS[(idx + 1) % ShopData.CONTAINERS.length];
		if (!shop.guiFont.isBlank() || shop.draftGuiPixels != null) shop.guiUnapplied = true;
		dragSlot = -1;
		guiPreviewStale = true;
		changed();
		reshape();
		syncSlot();
	}

	private void setType(int type) {
		ShopData.Slot s = slot();
		s.type = type;
		if (type == ShopData.CYCLE) s.ensureCycleSeeded();
		changed();
		syncSlot();
	}

	private void toggleSound() {
		shop.sound = !shop.sound;
		changed();
		refreshButtons();
	}

	private void putGoods(int index, ItemStack stack, int count) {
		ShopData.Slot s = shop.slots[index];
		s.item = stack.copyWithCount(1);
		s.count = ShopData.clampCount(stack, count);
		if (s.type == ShopData.EMPTY) s.type = ShopData.SELL;
		if (s.type == ShopData.CYCLE && s.options.isEmpty()) s.ensureCycleSeeded();
		changed();
	}

	private void clickAddOption() {
		ShopData.Slot s = slot();
		if (s.options.size() >= ShopData.MAX_OPTIONS) {
			error(K + "shop_too_many_opts", ShopData.MAX_OPTIONS);
			return;
		}
		if (!carried.isEmpty()) {
			appendOption(carried, carried.getCount());
			drop();
			return;
		}
		if (pickOption) {
			pickOption = false;
			appendOption(s.item, s.count);
			setStatus(Component.empty(), OK);
			return;
		}
		pickOption = true;
		pickPrice = false;
		setStatus(Component.empty(), OK);
	}

	private void appendOption(ItemStack stack, int count) {
		ShopData.Slot s = slot();
		if (stack.isEmpty() || s.options.size() >= ShopData.MAX_OPTIONS) return;
		ShopData.Option o = new ShopData.Option();
		o.item = stack.copyWithCount(1);
		o.count = ShopData.clampCount(stack, count);
		o.label = stack.getHoverName().getString() + " #" + (s.options.size() + 1);
		s.options.add(o);
		selOpt = s.options.size() - 1;
		optScroll = Math.max(0, s.options.size() - listRows);
		changed();
		syncCycleSelection();
	}

	private void toggleMenu() {
		if (!menuOpen && slot().prices.size() >= MAX_PRICES) {
			error(K + "shop_too_many_prices", MAX_PRICES);
			return;
		}
		drop();
		menuOpen = !menuOpen;
	}

	private void chooseMenu(int index) {
		menuOpen = false;
		if (index == 0) {
			pickPrice = true;
			setStatus(Component.empty(), OK);
		} else if (index == 1) {
			addPrice(ShopData.PRICE_XP, ItemStack.EMPTY, "", 1);
		} else {
			addingScore = true;
			updateVisibility();
			setFocused(objectiveField);
		}
	}

	private void addScore() {
		String objective = objectiveField.getValue().trim();
		if (!ShopData.validObjective(objective)) {
			error(K + "shop_bad_objective");
			return;
		}
		addingScore = false;
		addPrice(ShopData.PRICE_SCORE, ItemStack.EMPTY, objective, 1);
	}

	private void addPrice(int type, ItemStack item, String objective, int amount) {
		ShopData.Slot s = slot();
		if (!s.shown()) return;
		if (s.prices.size() >= MAX_PRICES) {
			error(K + "shop_too_many_prices", MAX_PRICES);
			updateVisibility();
			return;
		}
		ShopData.Price p = new ShopData.Price();
		p.type = type;
		p.item = item.isEmpty() ? ItemStack.EMPTY : item.copyWithCount(1);
		p.objective = objective;
		p.amount = Mth.clamp(amount, 1, ShopData.MAX_AMOUNT);
		s.prices.add(p);
		if (!s.trading()) s.type = ShopData.SELL;
		priceScroll = Math.max(0, s.prices.size() - listRows);
		editingPrice = -1;
		changed();
		updateVisibility();
	}

	private void editAmount(int index) {
		editingPrice = index;
		syncing = true;
		amountField.setValue(String.valueOf(slot().prices.get(index).amount));
		syncing = false;
		updateVisibility();
		setFocused(amountField);
		amountField.setCursorPosition(amountField.getValue().length());
		amountField.setHighlightPos(0);
	}

	private void fillEmpty() {
		int size = shop.size();
		for (int i = 0; i < size; i++) {
			ShopData.Slot s = shop.slots[i];
			if (s.shown()) continue;
			s.type = ShopData.DECOR;
			s.item = ShopCompat.filler();
			s.count = 1;
			s.command = "";
			s.prices.clear();
		}
		changed();
		syncSlot();
	}

	private void clearSlot() {
		shop.slots[sel] = new ShopData.Slot();
		selOpt = 0;
		changed();
		syncSlot();
	}

	private void resetCurrentShop() {
		if (!confirmReset && !shop.allEmpty()) {
			confirmReset = true;
			error(K + "shop_reset_confirm");
			return;
		}
		confirmReset = false;
		String keepId = shop.id;
		shop = new ShopData();
		shop.id = keepId;
		draft = shop;
		sel = 0;
		selOpt = 0;
		rowScroll = 0;
		drop();
		guiPreviewStale = true;
		changed();
		reshape();
		syncAll();
		setStatus(Component.translatable(K + "shop_reset_done"), OK);
	}

	private void newShop() {
		shop = new ShopData();
		draft = shop;
		generatedJson = null;
		sel = 0;
		selOpt = 0;
		rowScroll = 0;
		drop();
		guiPreviewStale = true;
		changed();
		reshape();
		syncAll();
		setStatus(Component.empty(), OK);
		setTab(TAB_SLOT);
	}

	private void openPaint() {
		drop();
		if (this.minecraft != null) {
			this.minecraft.setScreen(new ShopPaintScreen(this, shop, sel));
		}
	}

	public void onPaintMetaChanged() {
		guiPreviewStale = true;
		changed();
		if (idField != null) syncAll();
	}

	public void onButtonApplied(int index) {
		guiPreviewStale = true;
		changed();
		select(index);
		setTab(TAB_SLOT);
	}

	@Override
	public void onFilesDrop(List<Path> paths) {
		for (Path p : paths) {
			String n = p.getFileName().toString().toLowerCase(Locale.ROOT);
			if (n.endsWith(".png") || n.endsWith(".jpg") || n.endsWith(".jpeg")) {
				drop();
				if (this.minecraft != null) {
					this.minecraft.setScreen(new ShopPaintScreen(this, shop, sel, p));
				}
				return;
			}
		}
	}

	private void requestList() {
		if (!EditorAccess.canSendDatapack()) {
			error(K + "need_mod");
			return;
		}
		DatapackNet.send(DatapackOps.LIST, "", "");
	}

	private boolean ready() {
		if (busy) {
			error(K + "shop_busy");
			return false;
		}
		if (!EditorAccess.canSendDatapack()) {
			error(K + "need_mod");
			return false;
		}
		if (!treeLoaded) {
			requestList();
			error(K + "shop_busy");
			return false;
		}
		return true;
	}

	private static boolean commandValid(String command) {
		var connection = Minecraft.getInstance().getConnection();
		if (connection == null) return true;
		try {
			var parse = connection.getCommands().parse(command, connection.getSuggestionsProvider());
			return !parse.getReader().canRead() && parse.getExceptions().isEmpty();
		} catch (RuntimeException e) {
			return true;
		}
	}

	private boolean validate() {
		if (!ShopData.validId(shop.id)) {
			setTab(TAB_SHOP);
			error(K + "shop_bad_id");
			return false;
		}
		boolean any = shop.hasCustomGui();
		int size = shop.size();
		for (int p = 0; p < shop.pages.size(); p++) {
			ShopData.Slot[] slots = shop.pages.get(p);
			for (int i = 0; i < size; i++) {
				ShopData.Slot s = slots[i];
				if (!s.shown()) continue;
				any = true;
				if (s.nav()) {
					int target = shop.target(p, s);
					if (target < 0 || target >= shop.pages.size()) return fail(p, i, "shop_bad_nav");
					continue;
				}
				if (s.type == ShopData.ACTION) {
					String cmd = ShopPack.command(s.command);
					if (cmd.isEmpty() || !commandValid(cmd)) return fail(p, i, "shop_bad_command");
					continue;
				}
				if (s.type == ShopData.CYCLE) {
					if (s.options.size() < 2) return fail(p, i, "shop_need_cycle_opts");
					for (ShopData.Option o : s.options) {
						String cmd = ShopPack.command(o.command);
						if (!cmd.isEmpty() && !commandValid(cmd)) return fail(p, i, "shop_bad_command");
					}
					continue;
				}
				if (s.type == ShopData.SELECT) {
					String onCmd = ShopPack.command(s.command);
					String offCmd = ShopPack.command(s.offCommand);
					if ((!onCmd.isEmpty() && !commandValid(onCmd)) || (s.group <= 0 && !offCmd.isEmpty() && !commandValid(offCmd))) {
						return fail(p, i, "shop_bad_command");
					}
					continue;
				}
				if (s.type == ShopData.RESET) {
					String cmd = ShopPack.command(s.command);
					if (!cmd.isEmpty() && !commandValid(cmd)) return fail(p, i, "shop_bad_command");
					continue;
				}
				if (!s.trading()) continue;
				if (s.prices.isEmpty()) return fail(p, i, "shop_need_price");
				String command = ShopPack.command(s.command);
				if (!command.isEmpty() && !commandValid(command)) return fail(p, i, "shop_bad_command");
				for (String itemCommand : ShopPack.itemCommands(shop, s)) {
					if (itemCommand == null) return fail(p, i, "shop_strict_failed");
					if (!itemCommandValid(itemCommand)) return fail(p, i, "shop_bad_item");
				}
			}
		}
		if (!any) {
			error(K + "shop_empty");
			return false;
		}
		return true;
	}

	private String where(int page, int index) {
		if (shop.pages.size() <= 1) return I18n.get(K + "shop_slot_n", index + 1);
		return I18n.get(K + "shop_where_page", page + 1, index + 1);
	}

	private boolean fail(int page, int index, String key) {
		gotoPage(page);
		select(index);
		setTab(TAB_SLOT);
		error(K + key, where(page, index));
		return false;
	}

	private static boolean itemCommandValid(String command) {
		var connection = Minecraft.getInstance().getConnection();
		if (connection == null) return true;
		String name = command.substring(0, command.indexOf(' '));
		if (connection.getCommands().getRoot().getChild(name) == null) return true;
		return commandValid(command);
	}

	private void generate(Runnable then) {
		if (!ready() || !validate()) return;
		try {
			ShopGuiArt.flushPendingGui(shop);
		} catch (Exception e) {
			setStatus(Component.translatable(K + "rp_failed", String.valueOf(e.getMessage())), ERR);
			return;
		}
		String json = shop.toJson();
		Set<String> ids = new TreeSet<>(shops);
		ids.add(shop.id);
		Map<String, String> files = new LinkedHashMap<>(ShopPack.globalFiles(ids));
		files.putAll(ShopPack.shopFiles(shop));
		files.put(ShopPack.projectFile(shop.id), json);
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
		pendingId = shop.id;
		pendingLost = shop.unreachablePage();
		token = "bjshop#" + (++tokenSeq);
		for (Map.Entry<String, String> e : files.entrySet()) DatapackNet.send(DatapackOps.WRITE, e.getKey(), e.getValue());
		DatapackNet.send(DatapackOps.RELOAD, token, ShopPack.PACK);
		setStatus(Component.translatable(K + "shop_generating"), UiTheme.cMuted());
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
		if (!shops.contains(pendingId)) {
			shops.add(pendingId);
			shops.sort(null);
		}
		done(Component.translatable(K + "shop_generated", pendingId), pendingLost);
		if (then != null) then.run();
	}

	private void done(Component text, int lost) {
		if (lost < 0) setStatus(text, OK);
		else setStatus(Component.translatable(K + "shop_unreachable", lost + 1).append(" ").append(text), WARN);
	}

	public void handleNet(DatapackOpResultPayload payload) {
		if (payload == null) return;
		int kind = payload.kind();
		String path = payload.path() == null ? "" : payload.path();
		String extra = payload.extra() == null ? "" : payload.extra();
		if (kind == DatapackOps.KIND_TREE) {
			shops = ShopPack.shopIds(extra);
			treeLoaded = true;
			if (!shops.contains(shop.id)) generatedJson = null;
			shopScroll = Mth.clamp(shopScroll, 0, Math.max(0, shops.size() - shopRows));
		} else if (kind == DatapackOps.KIND_FILE) {
			if (!path.equals(loadingPath)) return;
			loadingPath = null;
			ShopData loaded = ShopData.fromJson(extra);
			if (loaded == null) {
				error(K + "shop_load_failed", loadingId);
				return;
			}
			shop = loaded;
			draft = shop;
			generatedJson = loaded.toJson();
			sel = 0;
			selOpt = 0;
			rowScroll = 0;
			drop();
			guiPreviewStale = true;
			changed();
			reshape();
			syncAll();
			setTab(TAB_SLOT);
			setStatus(Component.translatable(K + "shop_loaded", loaded.id), OK);
		} else if (kind == DatapackOps.KIND_ERR) {
			String key = extra.isEmpty() ? K + "file_op_failed" : extra;
			if (loadingPath != null && path.equals(loadingPath)) {
				loadingPath = null;
				error(K + "shop_load_failed", loadingId);
			} else if (!busy) {
				deletingId = null;
				error(key);
			} else if (path.equals(token)) {
				finish(failKey != null ? failKey : key);
			} else if (failKey == null) {
				failKey = key;
			}
		} else if (kind == DatapackOps.KIND_OK) {
			if (busy && path.equals(token)) {
				finish(failKey);
			} else if (deletingId != null && (K + "deleted").equals(extra)) {
				setStatus(Component.translatable(K + "shop_deleted", deletingId), OK);
				deletingId = null;
			}
		}
	}

	private void loadShop(String id) {
		if (busy) {
			error(K + "shop_busy");
			return;
		}
		confirmDelete = null;
		loadingId = id;
		loadingPath = ShopPack.projectFile(id);
		DatapackNet.send(DatapackOps.READ, loadingPath, "");
	}

	private void deleteShop(String id) {
		if (!ready()) return;
		if (!id.equals(confirmDelete)) {
			confirmDelete = id;
			error(K + "shop_delete_confirm", id);
			return;
		}
		confirmDelete = null;
		if (EditorAccess.commandProblem() == null) {
			EditorAccess.sendCommands(List.of("execute as @e[type=minecraft:marker,tag=" + ShopPack.shopTag(id) + "] at @s run function bjshop:unbind"));
		}
		List<String> rest = new ArrayList<>(shops);
		rest.remove(id);
		deletingId = id;
		if (rest.isEmpty()) {
			DatapackNet.send(DatapackOps.DELETE, ShopPack.PACK, "");
		} else {
			for (Map.Entry<String, String> e : ShopPack.globalFiles(rest).entrySet()) DatapackNet.send(DatapackOps.WRITE, e.getKey(), e.getValue());
			DatapackNet.send(DatapackOps.DELETE, ShopPack.shopDir(id), "");
		}
		if (id.equals(shop.id)) generatedJson = null;
	}

	private BlockHitResult target() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null) return null;
		HitResult hit = mc.player.pick(64.0, 1.0F, false);
		if (hit instanceof BlockHitResult block && hit.getType() == HitResult.Type.BLOCK) return block;
		error(K + "shop_no_target");
		return null;
	}

	private static boolean isBox(BlockState state) {
		return state.is(Blocks.CHEST) || state.is(Blocks.TRAPPED_CHEST) || state.is(Blocks.BARREL) || state.getBlock() instanceof ShulkerBoxBlock;
	}

	private static String at(BlockPos pos) {
		return "execute positioned " + pos.getX() + " " + pos.getY() + " " + pos.getZ() + " run ";
	}

	private boolean canCommand() {
		String problem = EditorAccess.commandProblem();
		if (problem == null) return true;
		error(problem);
		return false;
	}

	private void place() {
		if (!ready() || !canCommand() || !validate()) return;
		BlockHitResult hit = target();
		if (hit == null) return;
		Minecraft mc = Minecraft.getInstance();
		BlockState state = mc.level.getBlockState(hit.getBlockPos());
		boolean existing = isBox(state);
		boolean paired = existing && state.hasProperty(ChestBlock.TYPE) && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE;
		if (existing && paired != shop.large()) {
			error(K + (paired ? "shop_double_chest" : "shop_need_double"));
			return;
		}
		BlockPos pos = existing ? hit.getBlockPos() : hit.getBlockPos().relative(hit.getDirection());
		List<String> commands = new ArrayList<>();
		if (!existing) {
			Direction facing = mc.player.getDirection().getOpposite();
			String block = "minecraft:" + shop.block();
			if (shop.large()) {
				BlockPos low = pos.relative(facing.getCounterClockWise());
				if (!mc.level.getBlockState(low).canBeReplaced()) {
					low = pos;
					pos = pos.relative(facing.getClockWise());
					if (!mc.level.getBlockState(pos).canBeReplaced()) {
						error(K + "shop_no_room");
						return;
					}
				}
				commands.add(at(pos) + "setblock ~ ~ ~ " + block + "[type=right,facing=" + facing.getName() + "]");
				commands.add(at(low) + "setblock ~ ~ ~ " + block + "[type=left,facing=" + facing.getName() + "]");
			} else {
				if (!shop.container.equals("shulker_box")) block += "[facing=" + facing.getName() + "]";
				commands.add(at(pos) + "setblock ~ ~ ~ " + block);
			}
		}
		commands.add(at(pos) + "function " + ShopPack.function(shop.id, "bind"));
		String doneKey = K + (existing ? "shop_bound" : "shop_placed");
		int lost = shop.unreachablePage();
		Runnable send = () -> {
			if (EditorAccess.sendCommands(commands)) done(Component.translatable(doneKey), lost);
		};
		if (shop.toJson().equals(generatedJson)) send.run();
		else generate(send);
	}

	private void unbind() {
		if (!ready() || !canCommand()) return;
		if (shops.isEmpty()) {
			error(K + "shop_need_generate");
			return;
		}
		BlockHitResult hit = target();
		if (hit == null) return;
		if (EditorAccess.sendCommands(List.of(at(hit.getBlockPos()) + "function bjshop:unbind"))) {
			setStatus(Component.translatable(K + "shop_unbound"), OK);
		}
	}

	private void copyCommand() {
		if (!ShopData.validId(shop.id)) {
			error(K + "shop_bad_id");
			return;
		}
		if (!shop.toJson().equals(generatedJson)) {
			error(K + "shop_need_generate");
			return;
		}
		GiveCommands.copy("/function " + ShopPack.function(shop.id, "place"));
		setStatus(Component.translatable(K + "copied"), OK);
	}

	@Override
	public void tick() {
		super.tick();
		if (busy && ++busyTicks > 300) finish(K + "reload_failed");
	}

	private static boolean hit(double mouseX, double mouseY, int x, int y, int w, int h) {
		return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
	}

	private int shopX(int index) {
		return chestX + 8 + (index % 9) * 18;
	}

	private int shopY(int index) {
		return chestY + 18 + (index / 9 - rowScroll) * 18;
	}

	private int invX(int index) {
		return chestX + 8 + (index % 9) * 18;
	}

	private int invY(int index) {
		return invOff + (index < 27 ? chestY + 85 + (index / 9) * 18 : chestY + 143);
	}

	private static int invIndex(int index) {
		return index < 27 ? index + 9 : index - 27;
	}

	private int shopSlotAt(double mouseX, double mouseY) {
		if (!hit(mouseX, mouseY, chestX + 7, chestY + 17, 162, viewRows * 18)) return -1;
		int index = ((int) ((mouseY - chestY - 17) / 18) + rowScroll) * 9 + (int) ((mouseX - chestX - 7) / 18);
		return index < shop.size() ? index : -1;
	}

	private int invSlotAt(double mouseX, double mouseY) {
		int col = (int) ((mouseX - chestX - 7) / 18);
		int top = chestY + invOff;
		if (hit(mouseX, mouseY, chestX + 7, top + 84, 162, 54)) return (int) ((mouseY - top - 84) / 18) * 9 + col;
		if (hit(mouseX, mouseY, chestX + 7, top + 142, 162, 18)) return 27 + col;
		return -1;
	}

	private int tabX(int index) {
		return rightX + index * (tabW + 2);
	}

	private int tabAt(double mouseX, double mouseY) {
		for (int i = 0; i < TAB_KEYS.length; i++) {
			if (hit(mouseX, mouseY, tabX(i), bodyY, tabW, 16)) return i;
		}
		return -1;
	}

	private int typeAt(double mouseX, double mouseY) {
		for (int i = 0; i < TYPES.length; i++) {
			if (hit(mouseX, mouseY, typeXs[i], typeYs[i], typeWs[i], TYPE_H)) return i;
		}
		return -1;
	}

	private int pageBtnX(int which) {
		int x1 = chestX + CHEST_W - 8;
		if (which == 0) return x1 - 35 - pageLabelW - PAGE_BTN;
		if (which == 1) return x1 - 22 - PAGE_BTN;
		if (which == 2) return x1 - 10 - PAGE_BTN;
		return x1 - PAGE_BTN;
	}

	private int pageBtnAt(double mouseX, double mouseY) {
		for (int i = 0; i < PAGE_GLYPHS.length; i++) {
			if (hit(mouseX, mouseY, pageBtnX(i), chestY + 4, PAGE_BTN, 11)) return i;
		}
		return -1;
	}

	private boolean pageBtnEnabled(int which) {
		int pages = shop.pages.size();
		if (which == 0) return shop.page > 0;
		if (which == 1) return shop.page < pages - 1;
		if (which == 2) return pages < ShopData.MAX_PAGES;
		return pages > 1;
	}

	private static int navChoice(ShopData.Slot s) {
		return s.target == ShopData.NAV_PREV ? 0 : s.target == ShopData.NAV_NEXT ? 1 : 2;
	}

	private int menuX() {
		return addPriceBtn.getX() + addPriceBtn.getWidth() - menuW;
	}

	private int menuY() {
		return addPriceBtn.getY() + addPriceBtn.getHeight() + 1;
	}

	private int menuAt(double mouseX, double mouseY) {
		if (!hit(mouseX, mouseY, menuX(), menuY(), menuW, MENU_KEYS.length * MENU_ROW_H)) return -1;
		return (int) ((mouseY - menuY()) / MENU_ROW_H);
	}

	private Inventory inventory() {
		var player = Minecraft.getInstance().player;
		return player == null ? null : player.getInventory();
	}

	private Component chestTitle() {
		if (!shop.title.isBlank()) return SectionText.toComponent(shop.title, Style.EMPTY);
		if (shop.container.equals("barrel")) return Component.translatable("container.barrel");
		if (shop.container.equals("shulker_box")) return Component.translatable("container.shulkerBox");
		return Component.translatable(shop.large() ? "container.chestDouble" : "container.chest");
	}

	private void refreshIcons() {
		if (!iconsStale) return;
		iconsStale = false;
		int size = shop.size();
		for (int i = 0; i < icons.length; i++) {
			icons[i] = i < size && shop.slots[i].shown() ? ShopPack.icon(shop, i) : ItemStack.EMPTY;
		}
		ItemStack pv = icons[sel];
		ShopData.Slot s = slot();
		if (s.shown() && s.type == ShopData.CYCLE) {
			List<ItemStack> cyc = ShopPack.cycleIcons(shop, shop.page, sel);
			pv = cyc.get(Mth.clamp(selOpt, 0, cyc.size() - 1));
		}
		preview = pv.isEmpty() ? List.of() : Screen.getTooltipFromItem(Minecraft.getInstance(), pv);
	}

	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		super.renderBackground(g, mouseX, mouseY, partialTick);
		UiTheme.drawPanel(g, leftPos, topPos, panelW, panelH);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		updateVisibility();
		refreshIcons();
		int mx = menuOpen ? -10000 : mouseX;
		int my = menuOpen ? -10000 : mouseY;
		super.render(g, mx, my, partialTick);
		UiTheme.drawHeader(g, font, this.title, leftPos, topPos, panelW);
		boolean refHover = UiTheme.hitIcon(refreshX, refreshY, mx, my);
		UiTheme.drawRefreshIcon(g, refreshX, refreshY, refHover);
		int fy = topPos + panelH - FOOTER_H;
		g.fill(leftPos + 1, fy, leftPos + panelW - 1, fy + 1, 0xFF2B2C2D);
		drawTabs(g, mx, my);
		if (tab == TAB_SLOT) drawSlotTab(g, mx, my);
		else if (tab == TAB_SHOP) drawShopTab(g);
		else drawShops(g, mx, my);
		drawFooter(g, mx, my);
		ItemStack tip = drawChest(g, mx, my);
		if (menuOpen) drawMenu(g, mouseX, mouseY);
		CommandSuggestions cs = activeCmdSuggestions();
		if (cs != null && !menuOpen) {
			UiTheme.pushOverlay(g);
			cs.render(g, mouseX, mouseY);
		}
		boolean moving = drawCarried(g, mouseX, mouseY);
		int pageBtn = pageBtnAt(mx, my);
		if (moving) return;
		if (refHover) g.setTooltipForNextFrame(font, Component.translatable(K + "shop_reset_tip"), mouseX, mouseY);
		else if (pageBtn >= 0) g.setTooltipForNextFrame(font, Component.translatable(K + PAGE_TIPS[pageBtn]), mouseX, mouseY);
		else if (!tip.isEmpty()) g.setTooltipForNextFrame(font, tip, mouseX, mouseY);
	}

	private void drawTabs(GuiGraphics g, int mouseX, int mouseY) {
		for (int i = 0; i < TAB_KEYS.length; i++) {
			int x = tabX(i);
			UiTheme.drawButton(g, font, Component.translatable(K + TAB_KEYS[i]), x, bodyY, tabW, 16, hit(mouseX, mouseY, x, bodyY, tabW, 16), true, i == tab);
		}
	}

	private void drawSlotTab(GuiGraphics g, int mouseX, int mouseY) {
		ShopData.Slot s = slot();
		int y = contentY;
		String slotName = I18n.get(K + "shop_slot_n", sel + 1);
		if (!s.shown()) {
			UiTheme.label(g, font, slotName, rightX, y + 4);
			for (int i = 1; i <= 4; i++) {
				UiTheme.clipLabel(g, font, I18n.get(K + "shop_slot_empty_" + i), rightX, y + 10 + i * 12, rightW, UiTheme.cMuted());
			}
			return;
		}
		g.renderItem(s.item, rightX, y);
		UiTheme.clipLabel(g, font, slotName + "  " + s.item.getHoverName().getString(), rightX + 20, y + 4, Math.max(16, clearBtn.getX() - 6 - rightX - 20), UiTheme.cLabel());
		for (int i = 0; i < TYPES.length; i++) {
			int x = typeXs[i];
			int ty = typeYs[i];
			int w = typeWs[i];
			UiTheme.drawButton(g, font, Component.translatable(K + "shop_type_" + TYPES[i]), x, ty, w, TYPE_H, hit(mouseX, mouseY, x, ty, w, TYPE_H), true, s.type == TYPES[i]);
		}
		if (countField.visible) {
			String count = I18n.get(K + "shop_count");
			UiTheme.label(g, font, count, countField.getX() - 4 - font.width(count), y + 21);
		}
		if (s.trading()) {
			UiTheme.label(g, font, I18n.get(K + "shop_prices_" + s.type), rightX, y + 53);
			drawPrices(g, mouseX, mouseY);
		} else if (s.type == ShopData.NAV) {
			drawNav(g, s, mouseX, mouseY);
		} else if (s.type == ShopData.ACTION) {
			UiTheme.clipLabel(g, font, I18n.get(K + "shop_action_desc"), rightX, y + 53, rightW, UiTheme.cLabel());
		} else if (s.type == ShopData.CYCLE) {
			UiTheme.label(g, font, I18n.get(K + "shop_cycle_title", s.options.size(), ShopData.MAX_OPTIONS), rightX, y + 53);
			drawCycleOptions(g, s, mouseX, mouseY);
		} else if (s.type == ShopData.SELECT) {
			drawSelectPanel(g, s, mouseX, mouseY);
		} else if (s.type == ShopData.RESET) {
			UiTheme.clipLabel(g, font, I18n.get(K + "shop_reset_slot_desc"), rightX, y + 53, rightW, UiTheme.cLabel());
		} else {
			UiTheme.clipLabel(g, font, I18n.get(K + "shop_decor_hint"), rightX, y + 53, rightW, UiTheme.cMuted());
		}
		drawPreview(g);
	}

	private void drawCycleOptions(GuiGraphics g, ShopData.Slot s, int mouseX, int mouseY) {
		int x0 = rightX;
		int x1 = rightX + rightW;
		int h = listRows * ROW_H;
		g.fill(x0, priceTop, x1, priceTop + h, UiTheme.cInputBg());
		for (int i = 0; i < listRows && i + optScroll < s.options.size(); i++) {
			int idx = i + optScroll;
			ShopData.Option o = s.options.get(idx);
			int y = priceTop + i * ROW_H;
			boolean hover = mouseX >= x0 && mouseX < x1 && mouseY >= y && mouseY < y + ROW_H;
			if (idx == selOpt) g.fill(x0, y, x1, y + ROW_H, 0xFF21262D);
			else if (hover) g.fill(x0, y, x1, y + ROW_H, 0xFF191A1B);
			String name = (idx + 1) + ". " + (o.label.isBlank() ? o.item.getHoverName().getString() : o.label)
				+ " (" + o.item.getHoverName().getString() + ")";
			UiTheme.clipLabel(g, font, name, x0 + 4, y + 2, x1 - x0 - 20, idx == selOpt ? 0xFFFFFFFF : UiTheme.cLabel());
			if (s.options.size() > 1) {
				boolean del = hover && mouseX >= x1 - 14;
				g.drawString(font, "\u00d7", x1 - 10, y + 2, del ? ERR : UiTheme.cMuted(), false);
			}
		}
		UiTheme.drawThinScrollBar(g, x1 - 3, priceTop, h, optScroll, Math.max(0, s.options.size() - listRows));
	}

	private boolean clickCycleOptions(double mouseX, double mouseY) {
		int x0 = rightX;
		int x1 = rightX + rightW;
		if (mouseX < x0 || mouseX >= x1 || mouseY < priceTop || mouseY >= priceTop + listRows * ROW_H) return false;
		ShopData.Slot s = slot();
		int idx = (int) ((mouseY - priceTop) / ROW_H) + optScroll;
		if (idx < 0 || idx >= s.options.size()) {
			if (!carried.isEmpty()) {
				UiTheme.playClick();
				appendOption(carried, carried.getCount());
				drop();
				return true;
			}
			return true;
		}
		UiTheme.playClick();
		if (!carried.isEmpty()) {
			ShopData.Option o = s.options.get(idx);
			o.item = carried.copyWithCount(1);
			o.count = ShopData.clampCount(carried, carried.getCount());
			if (idx == 0) {
				s.item = o.item.copyWithCount(1);
				s.count = o.count;
			}
			drop();
			selOpt = idx;
			changed();
			syncCycleSelection();
			return true;
		}
		if (s.options.size() > 1 && mouseX >= x1 - 14) {
			s.options.remove(idx);
			if (idx == 0 && !s.options.isEmpty()) {
				s.item = s.options.get(0).item.copyWithCount(1);
				s.count = s.options.get(0).count;
			}
			selOpt = Mth.clamp(selOpt, 0, s.options.size() - 1);
			optScroll = Mth.clamp(optScroll, 0, Math.max(0, s.options.size() - listRows));
			changed();
			syncCycleSelection();
			return true;
		}
		selOpt = idx;
		syncCycleSelection();
		return true;
	}

	private void drawSelectPanel(GuiGraphics g, ShopData.Slot s, int mouseX, int mouseY) {
		int y = contentY + 50;
		boolean tog = s.group <= 0;
		UiTheme.drawButton(g, font, Component.translatable(K + "shop_sel_toggle"), selMode0X, y, selMode0W, 15, hit(mouseX, mouseY, selMode0X, y, selMode0W, 15), true, tog);
		String grpTxt = I18n.get(K + "shop_sel_group", Math.max(1, s.group));
		UiTheme.drawButton(g, font, Component.literal(grpTxt), selMode1X, y, selMode1W, 15, hit(mouseX, mouseY, selMode1X, y, selMode1W, 15), true, !tog);
		if (!tog) {
			UiTheme.drawButton(g, font, Component.literal("-"), selGrpMinusX, y, 14, 15, hit(mouseX, mouseY, selGrpMinusX, y, 14, 15), true);
			UiTheme.drawButton(g, font, Component.literal("+"), selGrpPlusX, y, 14, 15, hit(mouseX, mouseY, selGrpPlusX, y, 14, 15), true);
			UiTheme.clipLabel(g, font, I18n.get(K + "shop_sel_group_hint"), rightX, contentY + 86, rightW, UiTheme.cMuted());
		}
	}

	private boolean clickSelectPanel(double mouseX, double mouseY) {
		ShopData.Slot s = slot();
		int y = contentY + 50;
		if (hit(mouseX, mouseY, selMode0X, y, selMode0W, 15)) {
			UiTheme.playClick();
			s.group = 0;
			changed();
			updateVisibility();
			return true;
		}
		if (hit(mouseX, mouseY, selMode1X, y, selMode1W, 15)) {
			UiTheme.playClick();
			if (s.group <= 0) s.group = 1;
			changed();
			updateVisibility();
			return true;
		}
		if (s.group > 0) {
			int d = hit(mouseX, mouseY, selGrpMinusX, y, 14, 15) ? -1 : hit(mouseX, mouseY, selGrpPlusX, y, 14, 15) ? 1 : 0;
			if (d != 0) {
				UiTheme.playClick();
				s.group = Mth.clamp(s.group + d, 1, ShopData.MAX_GROUP);
				changed();
				updateVisibility();
				return true;
			}
		}
		return false;
	}

	private void drawNav(GuiGraphics g, ShopData.Slot s, int mouseX, int mouseY) {
		int y = contentY;
		UiTheme.label(g, font, I18n.get(K + "shop_nav_label"), rightX, y + 52);
		int choice = navChoice(s);
		for (int i = 0; i < NAV_KEYS.length; i++) {
			int x = navXs[i];
			int w = navWs[i];
			UiTheme.drawButton(g, font, Component.translatable(K + NAV_KEYS[i]), x, y + 64, w, 16, hit(mouseX, mouseY, x, y + 64, w, 16), true, i == choice);
		}
		int target = shop.target(shop.page, s);
		String info;
		int color = UiTheme.cMuted();
		if (target < 0 || target >= shop.pages.size()) {
			info = I18n.get(K + "shop_nav_none");
			color = ERR;
		} else if (target == shop.page) {
			info = I18n.get(K + "shop_nav_stay");
			color = WARN;
		} else {
			info = I18n.get(K + "shop_nav_to", target + 1);
		}
		int infoX = rightX;
		if (choice == 2) {
			UiTheme.drawButton(g, font, Component.literal("-"), rightX, y + 84, 14, 14, hit(mouseX, mouseY, rightX, y + 84, 14, 14), true);
			UiTheme.drawButton(g, font, Component.literal("+"), rightX + 16, y + 84, 14, 14, hit(mouseX, mouseY, rightX + 16, y + 84, 14, 14), true);
			infoX = rightX + 36;
		}
		UiTheme.clipLabel(g, font, info, infoX, y + 87, rightX + rightW - infoX, color);
		UiTheme.clipLabel(g, font, I18n.get(K + "shop_nav_hint"), rightX, y + 102, rightW, UiTheme.cMuted());
	}

	private boolean clickNav(double mouseX, double mouseY) {
		ShopData.Slot s = slot();
		int y = contentY;
		int last = shop.pages.size() - 1;
		for (int i = 0; i < NAV_KEYS.length; i++) {
			if (!hit(mouseX, mouseY, navXs[i], y + 64, navWs[i], 16)) continue;
			UiTheme.playClick();
			if (i == 0) s.target = ShopData.NAV_PREV;
			else if (i == 1) s.target = ShopData.NAV_NEXT;
			else if (navChoice(s) != 2) s.target = Mth.clamp(shop.target(shop.page, s), 0, last);
			changed();
			return true;
		}
		if (navChoice(s) != 2) return false;
		int step = hit(mouseX, mouseY, rightX, y + 84, 14, 14) ? -1 : hit(mouseX, mouseY, rightX + 16, y + 84, 14, 14) ? 1 : 0;
		if (step == 0) return false;
		UiTheme.playClick();
		int from = s.target >= 0 ? s.target : step > 0 ? -1 : last + 1;
		s.target = Mth.clamp(from + step, 0, last);
		changed();
		return true;
	}

	private void drawPreview(GuiGraphics g) {
		if (previewH < 18 || preview.isEmpty()) return;
		int x0 = rightX;
		int x1 = rightX + rightW;
		int y0 = previewTop;
		int y1 = previewTop + previewH;
		g.fill(x0, y0, x1, y1, 0xF0100010);
		g.fill(x0 + 1, y0 + 1, x1 - 1, y0 + 2, 0x505000FF);
		g.fill(x0 + 1, y1 - 2, x1 - 1, y1 - 1, 0x5028007F);
		g.fill(x0 + 1, y0 + 2, x0 + 2, y1 - 2, 0x505000FF);
		g.fill(x1 - 2, y0 + 2, x1 - 1, y1 - 2, 0x5028007F);
		g.enableScissor(x0 + 2, y0 + 2, x1 - 2, y1 - 2);
		for (int i = 0; i < preview.size(); i++) {
			g.drawString(font, preview.get(i), x0 + 5, y0 + 4 + i * 10, 0xFFFFFFFF, true);
		}
		g.disableScissor();
	}

	private String priceName(ShopData.Price p) {
		if (p.type == ShopData.PRICE_XP) return I18n.get(K + "shop_price_xp");
		if (p.type == ShopData.PRICE_SCORE) return I18n.get(K + "shop_price_score", p.objective);
		return p.item.getHoverName().getString();
	}

	private void drawPrices(GuiGraphics g, int mouseX, int mouseY) {
		int x0 = rightX;
		int x1 = rightX + rightW;
		int h = listRows * ROW_H;
		g.fill(x0, priceTop, x1, priceTop + h, UiTheme.cInputBg());
		List<ShopData.Price> prices = slot().prices;
		if (prices.isEmpty()) {
			UiTheme.clipLabel(g, font, I18n.get(K + "shop_no_prices"), x0 + 4, priceTop + 2, x1 - x0 - 8, UiTheme.cMuted());
			return;
		}
		for (int i = 0; i < listRows && i + priceScroll < prices.size(); i++) {
			ShopData.Price p = prices.get(i + priceScroll);
			int y = priceTop + i * ROW_H;
			boolean hover = mouseX >= x0 && mouseX < x1 && mouseY >= y && mouseY < y + ROW_H;
			if (hover) g.fill(x0, y, x1, y + ROW_H, 0xFF191A1B);
			UiTheme.clipLabel(g, font, priceName(p), x0 + 4, y + 2, x1 - 88 - x0 - 4, UiTheme.cLabel());
			boolean minus = hover && mouseX >= x1 - 84 && mouseX < x1 - 66;
			boolean plus = hover && mouseX >= x1 - 30 && mouseX < x1 - 14;
			boolean del = hover && mouseX >= x1 - 14;
			g.drawString(font, "-", x1 - 77, y + 2, minus ? 0xFFFFFFFF : UiTheme.cMuted(), false);
			if (i + priceScroll != editingPrice) {
				String amount = String.valueOf(p.amount);
				g.drawString(font, amount, x1 - 48 - font.width(amount) / 2, y + 2, UiTheme.cLabel(), false);
			}
			g.drawString(font, "+", x1 - 25, y + 2, plus ? 0xFFFFFFFF : UiTheme.cMuted(), false);
			g.drawString(font, "\u00d7", x1 - 10, y + 2, del ? ERR : UiTheme.cMuted(), false);
		}
		UiTheme.drawThinScrollBar(g, x1 - 3, priceTop, h, priceScroll, Math.max(0, prices.size() - listRows));
	}

	private boolean clickPrices(double mouseX, double mouseY) {
		int x0 = rightX;
		int x1 = rightX + rightW;
		if (mouseX < x0 || mouseX >= x1 || mouseY < priceTop || mouseY >= priceTop + listRows * ROW_H) return false;
		if (!carried.isEmpty()) {
			UiTheme.playClick();
			addPrice(ShopData.PRICE_ITEM, carried, "", carried.getCount());
			drop();
			return true;
		}
		List<ShopData.Price> prices = slot().prices;
		int idx = (int) ((mouseY - priceTop) / ROW_H) + priceScroll;
		if (idx < 0 || idx >= prices.size()) return true;
		ShopData.Price p = prices.get(idx);
		int step = Minecraft.getInstance().hasControlDown() ? 64 : Minecraft.getInstance().hasShiftDown() ? 10 : 1;
		if (mouseX >= x1 - 14) {
			prices.remove(idx);
			priceScroll = Mth.clamp(priceScroll, 0, Math.max(0, prices.size() - listRows));
		} else if (mouseX >= x1 - 30) {
			p.amount = Mth.clamp(p.amount + step, 1, ShopData.MAX_AMOUNT);
		} else if (mouseX >= x1 - 66) {
			UiTheme.playClick();
			editAmount(idx);
			return true;
		} else if (mouseX >= x1 - 84) {
			p.amount = Mth.clamp(p.amount - step, 1, ShopData.MAX_AMOUNT);
		} else {
			return true;
		}
		editingPrice = -1;
		UiTheme.playClick();
		changed();
		updateVisibility();
		return true;
	}

	private void drawShopTab(GuiGraphics g) {
		for (int i = 0; i < SHOP_LABELS.length; i++) {
			UiTheme.label(g, font, I18n.get(K + SHOP_LABELS[i]), rightX, contentY + 4 + i * 20);
		}
	}

	private void drawShops(GuiGraphics g, int mouseX, int mouseY) {
		int x0 = rightX;
		int x1 = rightX + rightW;
		int top = contentY;
		int h = shopRows * ROW_H;
		g.fill(x0, top, x1, top + h, UiTheme.cInputBg());
		if (shops.isEmpty()) {
			UiTheme.clipLabel(g, font, I18n.get(K + "shop_list_empty"), x0 + 4, top + 2, x1 - x0 - 8, UiTheme.cMuted());
			return;
		}
		for (int i = 0; i < shopRows && i + shopScroll < shops.size(); i++) {
			String id = shops.get(i + shopScroll);
			int y = top + i * ROW_H;
			boolean hover = mouseX >= x0 && mouseX < x1 && mouseY >= y && mouseY < y + ROW_H;
			if (id.equals(shop.id)) g.fill(x0, y, x1, y + ROW_H, 0xFF21262D);
			else if (hover) g.fill(x0, y, x1, y + ROW_H, 0xFF191A1B);
			UiTheme.clipLabel(g, font, id, x0 + 4, y + 2, x1 - x0 - 24, UiTheme.cLabel());
			boolean del = hover && mouseX >= x1 - 14;
			g.drawString(font, "\u00d7", x1 - 10, y + 2, del || id.equals(confirmDelete) ? ERR : UiTheme.cMuted(), false);
		}
		UiTheme.drawThinScrollBar(g, x1 - 3, top, h, shopScroll, Math.max(0, shops.size() - shopRows));
	}

	private boolean clickShops(double mouseX, double mouseY) {
		int x0 = rightX;
		int x1 = rightX + rightW;
		if (mouseX < x0 || mouseX >= x1 || mouseY < contentY || mouseY >= contentY + shopRows * ROW_H) return false;
		int idx = (int) ((mouseY - contentY) / ROW_H) + shopScroll;
		if (idx < 0 || idx >= shops.size()) return true;
		String id = shops.get(idx);
		UiTheme.playClick();
		if (mouseX >= x1 - 14) deleteShop(id);
		else loadShop(id);
		return true;
	}

	private void drawFooter(GuiGraphics g, int mouseX, int mouseY) {
		UiTheme.drawButton(g, font, Component.translatable(K + "shop_generate"), genX, footerY, genW, 20, hit(mouseX, mouseY, genX, footerY, genW, 20), true, true);
		String text = status.getString();
		int color = statusColor;
		if (!carried.isEmpty()) {
			text = I18n.get(K + "shop_hint_carry");
			color = UiTheme.cMuted();
		} else if (pickPrice) {
			text = I18n.get(K + "shop_hint_price");
			color = UiTheme.cMuted();
		} else if (pickOption) {
			text = I18n.get(K + "shop_hint_opt");
			color = UiTheme.cMuted();
		} else if (text.isEmpty()) {
			boolean rows = maxRowScroll() > 0 && hit(mouseX, mouseY, chestX, chestY, CHEST_W, 18 + viewRows * 18);
			text = I18n.get(K + (rows ? "shop_hint_rows" : hasGoods() ? "shop_hint_next" : "shop_hint_start"));
			color = UiTheme.cMuted();
		}
		UiTheme.clipLabel(g, font, text, leftPos + PAD, footerY + 6, Math.max(16, paintBtn.getX() - 8 - leftPos - PAD), color);
	}

	private boolean hasGoods() {
		for (ItemStack icon : icons) {
			if (!icon.isEmpty()) return true;
		}
		return false;
	}

	private void refreshGuiPreview() {
		if (!guiPreviewStale) return;
		guiPreviewStale = false;
		releaseGuiPreview();
		ShopGuiArt.Img raw = ShopGuiArt.loadSavedGui(shop);
		if (raw == null) return;
		ShopGuiArt.Img adapted = ShopGuiArt.adaptHeight(raw, shop.container);
		guiPreviewH = adapted.h();
		guiPreviewTex = new DynamicTexture(() -> "bj_shop_gui_pv", ShopGuiArt.toNative(adapted.w(), adapted.h(), adapted.pixels()));
		guiPreviewId = Identifier.fromNamespaceAndPath("bj_mapedit", "shop_gui_pv/" + (++guiPrevSeq));
		Minecraft.getInstance().getTextureManager().register(guiPreviewId, guiPreviewTex);
	}

	private void releaseGuiPreview() {
		if (guiPreviewId != null) {
			Minecraft.getInstance().getTextureManager().release(guiPreviewId);
			guiPreviewId = null;
			guiPreviewTex = null;
		}
	}

	private void blitChest(GuiGraphics g) {
		refreshGuiPreview();
		int top = 17 + viewRows * 18;
		g.blit(RenderPipelines.GUI_TEXTURED, CHEST, chestX, chestY, 0, 0, CHEST_W, top, 256, 256);
		g.blit(RenderPipelines.GUI_TEXTURED, CHEST, chestX, chestY + top, 0, 126, CHEST_W, 96, 256, 256);
		if (guiPreviewId != null) {
			if (shop.large()) {
				g.blit(RenderPipelines.GUI_TEXTURED, guiPreviewId, chestX, chestY, 0, 0, CHEST_W, 17, CHEST_W, guiPreviewH);
				g.blit(RenderPipelines.GUI_TEXTURED, guiPreviewId, chestX, chestY + 17, 0, 17 + rowScroll * 18, CHEST_W, viewRows * 18, CHEST_W, guiPreviewH);
				g.blit(RenderPipelines.GUI_TEXTURED, guiPreviewId, chestX, chestY + top, 0, 125, CHEST_W, 96, CHEST_W, guiPreviewH);
			} else {
				g.blit(RenderPipelines.GUI_TEXTURED, guiPreviewId, chestX, chestY, 0, 0, CHEST_W, 167, CHEST_W, guiPreviewH);
			}
		}
		int max = maxRowScroll();
		if (max <= 0) return;
		int x = chestX + 170;
		int y = chestY + 18;
		int h = viewRows * 18 - 2;
		int thumb = h * viewRows / (viewRows + max);
		int thumbY = y + (h - thumb) * rowScroll / max;
		g.fill(x, y, x + 2, y + h, 0xFF8B8B8B);
		g.fill(x, thumbY, x + 2, thumbY + thumb, INK);
	}

	private void drawStack(GuiGraphics g, ItemStack stack, int x, int y) {
		g.renderItem(stack, x, y);
		g.renderItemDecorations(font, stack, x, y);
	}

	private void drawPageBar(GuiGraphics g, int mouseX, int mouseY) {
		int y = chestY + 4;
		String label = (shop.page + 1) + "/" + shop.pages.size();
		int labelX = pageBtnX(1) - 2 - pageLabelW;
		g.drawString(font, label, labelX + (pageLabelW - font.width(label)) / 2, y + 2, INK, false);
		for (int i = 0; i < PAGE_GLYPHS.length; i++) {
			int x = pageBtnX(i);
			boolean enabled = pageBtnEnabled(i);
			boolean hover = enabled && hit(mouseX, mouseY, x, y, PAGE_BTN, 11);
			g.fill(x, y, x + PAGE_BTN, y + 11, hover ? 0xFFFFFFFF : 0xFF8B8B8B);
			g.fill(x + 1, y + 1, x + PAGE_BTN - 1, y + 10, hover ? 0xFFDCE6FF : 0xFFC6C6C6);
			int color = !enabled ? 0xFF9A9A9A : i == 3 && confirmPage ? 0xFFCF222E : INK;
			g.drawString(font, PAGE_GLYPHS[i], x + (PAGE_BTN + 1 - font.width(PAGE_GLYPHS[i])) / 2, y + 2, color, false);
		}
	}

	private ItemStack drawChest(GuiGraphics g, int mouseX, int mouseY) {
		blitChest(g);
		if (!shop.title.isBlank() || (shop.guiFont.isBlank() && shop.draftGuiPixels == null)) {
			g.enableScissor(chestX + 8, chestY + 4, pageBtnX(0) - 3, chestY + 16);
			g.drawString(font, chestTitle(), chestX + 8, chestY + 6, INK, false);
			g.disableScissor();
		}
		drawPageBar(g, mouseX, mouseY);
		g.drawString(font, Component.translatable("container.inventory"), chestX + 8, chestY + 74 + invOff, INK, false);
		int hoverShop = shopSlotAt(mouseX, mouseY);
		int hoverInv = invSlotAt(mouseX, mouseY);
		int first = rowScroll * 9;
		int last = Math.min(shop.size(), first + viewRows * 9);
		for (int i = first; i < last; i++) {
			if (icons[i].isEmpty() || (i == dragSlot && hoverShop != dragSlot)) continue;
			drawStack(g, icons[i], shopX(i), shopY(i));
		}
		Inventory inv = inventory();
		ItemStack tip = ItemStack.EMPTY;
		if (inv != null) {
			for (int i = 0; i < 36; i++) {
				ItemStack stack = inv.getItem(invIndex(i));
				if (!stack.isEmpty()) drawStack(g, stack, invX(i), invY(i));
			}
			if (hoverInv >= 0) tip = inv.getItem(invIndex(hoverInv));
		}
		if (hoverShop >= 0) tip = icons[hoverShop];
		UiTheme.pushOverlay(g);
		for (int i = first; i < last; i++) {
			int x = shopX(i);
			int y = shopY(i);
			int type = shop.slots[i].type;
			if (type != ShopData.EMPTY) {
				g.fill(x, y, x + 5, y + 5, 0xFF1B1F23);
				g.fill(x, y, x + 4, y + 4, TYPE_COLORS[type]);
			} else if (shop.isHidden(i)) {
				g.fill(x, y, x + 4, y + 4, 0x906E7681);
			}
			if (i != sel) continue;
			g.fill(x - 1, y - 1, x + 17, y, SELECTED);
			g.fill(x - 1, y + 16, x + 17, y + 17, SELECTED);
			g.fill(x - 1, y, x, y + 16, SELECTED);
			g.fill(x + 16, y, x + 17, y + 16, SELECTED);
		}
		if (hoverShop >= 0) g.fill(shopX(hoverShop), shopY(hoverShop), shopX(hoverShop) + 16, shopY(hoverShop) + 16, 0x80FFFFFF);
		if (hoverInv >= 0) g.fill(invX(hoverInv), invY(hoverInv), invX(hoverInv) + 16, invY(hoverInv) + 16, 0x80FFFFFF);
		return tip;
	}

	private void drawMenu(GuiGraphics g, int mouseX, int mouseY) {
		int x = menuX();
		int y = menuY();
		int h = MENU_KEYS.length * MENU_ROW_H;
		int hover = menuAt(mouseX, mouseY);
		UiTheme.pushOverlay(g);
		g.fill(x - 1, y - 1, x + menuW + 1, y + h + 1, UiTheme.cListBorder());
		g.fill(x, y, x + menuW, y + h, UiTheme.cListBg());
		for (int i = 0; i < MENU_KEYS.length; i++) {
			int rowY = y + i * MENU_ROW_H;
			if (i == hover) g.fill(x, rowY, x + menuW, rowY + MENU_ROW_H, UiTheme.cListHover());
			g.drawString(font, I18n.get(K + MENU_KEYS[i]), x + 6, rowY + 3, UiTheme.cListText(), false);
		}
	}

	private boolean drawCarried(GuiGraphics g, int mouseX, int mouseY) {
		ItemStack stack = carried;
		if (stack.isEmpty() && dragSlot >= 0 && shopSlotAt(mouseX, mouseY) != dragSlot) stack = icons[dragSlot];
		if (stack.isEmpty()) return false;
		UiTheme.pushOverlay(g);
		drawStack(g, stack, mouseX - 8, mouseY - 8);
		return true;
	}

	private void clickShopSlot(int index, int button) {
		UiTheme.playClick();
		pickPrice = false;
		pickOption = false;
		if (!carried.isEmpty()) {
			ShopData.Slot s = shop.slots[index];
			if (button == 1 && s.shown() && ShopCompat.sameItem(s.item, carried)) {
				s.count = ShopData.clampCount(s.item, s.count + 1);
				changed();
			} else {
				putGoods(index, carried, button == 1 ? 1 : carried.getCount());
				if (button == 0) carried = ItemStack.EMPTY;
			}
			select(index);
			setTab(TAB_SLOT);
			return;
		}
		select(index);
		setTab(TAB_SLOT);
		if (button == 0 && shop.slots[index].shown()) dragSlot = index;
	}

	private void clickInvSlot(int index) {
		Inventory inv = inventory();
		ItemStack stack = inv == null ? ItemStack.EMPTY : inv.getItem(invIndex(index));
		if (stack.isEmpty()) {
			drop();
			return;
		}
		UiTheme.playClick();
		if (pickPrice) {
			pickPrice = false;
			addPrice(ShopData.PRICE_ITEM, stack, "", stack.getCount());
			return;
		}
		if (pickOption) {
			pickOption = false;
			appendOption(stack, stack.getCount());
			return;
		}
		carried = stack.copy();
		invPress = index;
	}

	private boolean pressed(double mouseX, double mouseY, int button) {
		if (menuOpen) {
			int choice = menuAt(mouseX, mouseY);
			menuOpen = false;
			if (choice >= 0) {
				UiTheme.playClick();
				chooseMenu(choice);
			}
			return true;
		}
		if (button == 0 && UiTheme.consumeRefreshClick(refreshX, refreshY, mouseX, mouseY, this::resetCurrentShop)) {
			return true;
		}
		if (button == 0 || button == 1) {
			int shopSlot = shopSlotAt(mouseX, mouseY);
			if (shopSlot >= 0) {
				clickShopSlot(shopSlot, button);
				return true;
			}
			int invSlot = invSlotAt(mouseX, mouseY);
			if (invSlot >= 0) {
				clickInvSlot(invSlot);
				return true;
			}
		}
		if (button != 0) {
			drop();
			return false;
		}
		int pageBtn = pageBtnAt(mouseX, mouseY);
		if (pageBtn >= 0) {
			UiTheme.playClick();
			clickPageBtn(pageBtn);
			return true;
		}
		if (tab == TAB_SLOT && slot().trading() && !amountField.isMouseOver(mouseX, mouseY) && clickPrices(mouseX, mouseY)) return true;
		if (tab == TAB_SLOT && slot().type == ShopData.CYCLE && clickCycleOptions(mouseX, mouseY)) return true;
		if (!addOptBtn.isMouseOver(mouseX, mouseY)) pickOption = false;
		drop();
		if (hit(mouseX, mouseY, chestX + 8, chestY + 4, pageBtnX(0) - 3 - chestX - 8, 12)) {
			UiTheme.playClick();
			setTab(TAB_SHOP);
			setFocused(titleField);
			return true;
		}
		int tabIndex = tabAt(mouseX, mouseY);
		if (tabIndex >= 0) {
			UiTheme.playClick();
			setTab(tabIndex);
			return true;
		}
		if (hit(mouseX, mouseY, genX, footerY, genW, 20)) {
			UiTheme.playClick();
			generate(null);
			return true;
		}
		if (tab == TAB_SLOT && slot().shown()) {
			int type = typeAt(mouseX, mouseY);
			if (type >= 0) {
				UiTheme.playClick();
				setType(TYPES[type]);
				return true;
			}
			if (slot().type == ShopData.NAV && clickNav(mouseX, mouseY)) return true;
			if (slot().type == ShopData.SELECT && clickSelectPanel(mouseX, mouseY)) return true;
		}
		if (tab == TAB_LIST && clickShops(mouseX, mouseY)) return true;
		if (editingPrice >= 0 && !amountField.isMouseOver(mouseX, mouseY)) {
			editingPrice = -1;
			updateVisibility();
		}
		return false;
	}

	private void released(double mouseX, double mouseY, int button) {
		if (button != 0) return;
		int target = shopSlotAt(mouseX, mouseY);
		if (dragSlot >= 0 && target >= 0 && target != dragSlot) {
			ShopData.Slot moved = shop.slots[dragSlot];
			shop.slots[dragSlot] = shop.slots[target];
			shop.slots[target] = moved;
			changed();
			select(target);
		} else if (invPress >= 0 && target >= 0 && !carried.isEmpty()) {
			putGoods(target, carried, carried.getCount());
			carried = ItemStack.EMPTY;
			select(target);
			setTab(TAB_SLOT);
		}
		dragSlot = -1;
		invPress = -1;
	}

	private boolean scrolled(double mouseX, double mouseY, double scrollY) {
		int delta = (int) Math.signum(scrollY);
		if (tab == TAB_SLOT && slot().trading() && hit(mouseX, mouseY, rightX, priceTop, rightW, listRows * ROW_H)) {
			priceScroll = Mth.clamp(priceScroll - delta, 0, Math.max(0, slot().prices.size() - listRows));
			updateVisibility();
			return true;
		}
		if (tab == TAB_SLOT && slot().type == ShopData.CYCLE && hit(mouseX, mouseY, rightX, priceTop, rightW, listRows * ROW_H)) {
			optScroll = Mth.clamp(optScroll - delta, 0, Math.max(0, slot().options.size() - listRows));
			return true;
		}
		if (tab == TAB_LIST && hit(mouseX, mouseY, rightX, contentY, rightW, shopRows * ROW_H)) {
			shopScroll = Mth.clamp(shopScroll - delta, 0, Math.max(0, shops.size() - shopRows));
			return true;
		}
		if (delta != 0 && hit(mouseX, mouseY, chestX, chestY, CHEST_W, 18 + viewRows * 18)) {
			int max = maxRowScroll();
			int row = Mth.clamp(rowScroll - delta, 0, max);
			if (row != rowScroll) {
				rowScroll = row;
				return true;
			}
			int page = Mth.clamp(shop.page - delta, 0, shop.pages.size() - 1);
			if (page == shop.page) return max > 0 || shop.pages.size() > 1;
			gotoPage(page);
			rowScroll = delta < 0 ? 0 : max;
			return true;
		}
		return false;
	}

	private boolean keyed(int keyCode) {
		if (keyCode == 256) {
			if (menuOpen) {
				menuOpen = false;
				return true;
			}
			if (addingScore || editingPrice >= 0) {
				addingScore = false;
				editingPrice = -1;
				updateVisibility();
				return true;
			}
			if (!carried.isEmpty() || pickPrice || pickOption) {
				drop();
				return true;
			}
			return false;
		}
		if (keyCode == 257 || keyCode == 335) {
			if (addingScore && getFocused() == objectiveField) {
				addScore();
				return true;
			}
			if (editingPrice >= 0 && getFocused() == amountField) {
				editingPrice = -1;
				updateVisibility();
				return true;
			}
			return false;
		}
		if ((keyCode == 266 || keyCode == 267) && !(getFocused() instanceof EditBox)) {
			gotoPage(shop.page + (keyCode == 267 ? 1 : -1));
			return true;
		}
		if (keyCode == 261 && tab == TAB_SLOT && slot().shown() && !(getFocused() instanceof EditBox)) {
			clearSlot();
			return true;
		}
		return false;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		double mouseX = event.x();
		double mouseY = event.y();
		int button = event.button();
		CommandSuggestions cs = activeCmdSuggestions();
		if (!menuOpen && cs != null && cs.mouseClicked(event)) return true;
		boolean handled = pressed(mouseX, mouseY, button) || super.mouseClicked(event, doubleClick);
		refreshCmdSuggestions();
		return handled;
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		double mouseX = event.x();
		double mouseY = event.y();
		int button = event.button();
		released(mouseX, mouseY, button);
		return super.mouseReleased(event);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		CommandSuggestions cs = activeCmdSuggestions();
		if (cs != null && cs.mouseScrolled(Mth.clamp(scrollY, -1.0, 1.0))) return true;
		if (scrolled(mouseX, mouseY, scrollY)) return true;
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		int keyCode = event.key();
		CommandSuggestions cs = activeCmdSuggestions();
		if (cs != null && cs.keyPressed(event)) return true;
		if (keyed(keyCode)) return true;
		return super.keyPressed(event);
	}

	@Override
	public void removed() {
		releaseGuiPreview();
		super.removed();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
