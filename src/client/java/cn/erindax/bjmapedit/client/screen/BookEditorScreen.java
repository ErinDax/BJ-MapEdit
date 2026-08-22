package cn.erindax.bjmapedit.client.screen;

import cn.erindax.bjmapedit.client.widget.BookTemplateStore;
import cn.erindax.bjmapedit.client.widget.GiveCommands;
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
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.client.gui.screens.inventory.PageButton;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.network.Filterable;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WritableBookContent;
import net.minecraft.world.item.component.WrittenBookContent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Environment(EnvType.CLIENT)
public class BookEditorScreen extends Screen {

	private int WIDTH = 500;
	private int HEIGHT = 420;
	private static final int LABEL_X = 12;
	private int INPUT_X = 96;
	private int INPUT_W = 136;
	private int RIGHT_X = 268;
	private static final int ROW_H = 20;
	private static final int FOOTER_H = 30;
	private static final int TEMPLATE_ROW_H = 22;
	private int CONTENT_TOP = 30;
	private int CONTENT_BOTTOM;
	private static final int MAX_LINES_PER_PAGE = 14;
	private static final int BOOK_IMG = 192;
	private static final int BOOK_PAGE_TEXT_W = 114;
	private static final float BOOK_SCALE_MAX = 1f;
	private static final int BOOK_PAGE_BTN_Y = 157;
	private static final int BOOK_PAGE_BACK_X = 43;
	private static final int BOOK_PAGE_FWD_X = 116;
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

	private EditBox bookNameField;
	private EditBox authorField;

	private Button prevPageBtn;
	private Button nextPageBtn;
	private Button addPageBtn;
	private Button removePageBtn;
	private PageButton bookBackBtn;
	private PageButton bookFwdBtn;
	private int bookDrawX;
	private int bookDrawY;
	private float bookDrawScale = 1f;
	private Button addLineBtn;
	private Button textEditorBtn;
	private Button bookNameTextEditorBtn;
	private Button giveBtn;
	private Button copyBtn;
	private Button saveBtn;
	private int refreshX, refreshY;

	private List<BookPage> pages = new ArrayList<>();
	private List<EditBox> contentLineBoxes = new ArrayList<>();
	private List<Button> contentRemoveBtns = new ArrayList<>();
	private int currentPage = 0;

	private ItemStack previewStack = ItemStack.EMPTY;

	private static SavedState savedState = null;

	private static class BookPage {
		List<String> contentLines = new ArrayList<>();
	}

	private record SavedState(
		String bookName,
		String author,
		List<String> pageContents
	) {}

	private int leftPos;
	private int topPos;
	private int scrollOffset;
	private int maxScroll;
	private boolean restoring;
	private boolean dragging;
	private boolean draggingScroll;
	private double scrollGrabOffset;
	private EditBox dragEditBox;
	private int dragAnchorPos;
	private boolean contentPendingRebuild;
	private String pendingBookName;

	public BookEditorScreen() {
		super(Component.translatable("screen.bj_mapedit.book_title"));
	}

	@Override
	protected void init() {
		super.init();
		layoutPanel();
		Font font = this.font;

		boolean wasReinit = bookNameField != null;
		String snapName = wasReinit ? bookNameField.getValue() : null;
		String snapAuthor = wasReinit ? authorField.getValue() : null;
		int snapScroll = wasReinit ? scrollOffset : 0;

		bookNameField = new EditBox(font, leftPos + INPUT_X, 0, INPUT_W, 16,
			Component.translatable("screen.bj_mapedit.book_name"));
		bookNameField.setResponder(s -> updatePreview());
		this.addRenderableWidget(bookNameField);

		bookNameTextEditorBtn = Button.builder(
			Component.translatable("screen.bj_mapedit.edit_text"),
			b -> openBookNameTextEditor()
		).bounds(leftPos + INPUT_X, 0, 88, 16).build();
		this.addRenderableWidget(bookNameTextEditorBtn);

		authorField = new EditBox(font, leftPos + INPUT_X, 0, INPUT_W, 16,
			Component.translatable("screen.bj_mapedit.book_author"));
		authorField.setResponder(s -> updatePreview());
		this.addRenderableWidget(authorField);

		if (pages.isEmpty()) {
			pages.add(new BookPage());
		}

		rebuildContentLineBoxes();

		addLineBtn = Button.builder(
			Component.literal("+"),
			b -> addContentLine()
		).bounds(leftPos + INPUT_X, 0, 16, 16).build();
		this.addRenderableWidget(addLineBtn);

		prevPageBtn = Button.builder(
			Component.translatable("screen.bj_mapedit.prev_page"),
			b -> prevPage()
		).bounds(leftPos + INPUT_X, 0, 30, 16).build();
		this.addRenderableWidget(prevPageBtn);

		nextPageBtn = Button.builder(
			Component.translatable("screen.bj_mapedit.next_page"),
			b -> nextPage()
		).bounds(leftPos + INPUT_X + 55, 0, 30, 16).build();
		this.addRenderableWidget(nextPageBtn);

		addPageBtn = Button.builder(
			Component.translatable("screen.bj_mapedit.add_page"),
			b -> addPage()
		).bounds(leftPos + INPUT_X + 90, 0, 30, 16).build();
		this.addRenderableWidget(addPageBtn);

		removePageBtn = Button.builder(
			Component.translatable("screen.bj_mapedit.remove_page"),
			b -> removePage()
		).bounds(leftPos + INPUT_X, 0, 30, 16).build();
		this.addRenderableWidget(removePageBtn);

		bookBackBtn = this.addRenderableWidget(new PageButton(0, 0, false, b -> prevPage(), true));
		bookFwdBtn = this.addRenderableWidget(new PageButton(0, 0, true, b -> nextPage(), true));

		textEditorBtn = Button.builder(
			Component.translatable("screen.bj_mapedit.edit_text"),
			b -> openTextEditor()
		).bounds(leftPos + INPUT_X, 0, 88, 16).build();
		this.addRenderableWidget(textEditorBtn);

		giveBtn = this.addRenderableWidget(Button.builder(
			Component.translatable("screen.bj_mapedit.give"),
			b -> giveItem()
		).bounds(0, 0, 70, 20).build());

		copyBtn = this.addRenderableWidget(Button.builder(
			Component.translatable("screen.bj_mapedit.copy"),
			b -> copyCommand()
		).bounds(0, 0, 70, 20).build());

		saveBtn = this.addRenderableWidget(Button.builder(
			Component.translatable("screen.bj_mapedit.save_template"),
			b -> saveTemplate()
		).bounds(0, 0, 70, 20).build());

		templateRenameBox = new EditBox(font, 0, -1000, 80, 16, Component.empty());
		templateRenameBox.setMaxLength(24);
		templateRenameBox.visible = false;
		this.addRenderableWidget(templateRenameBox);

		repositionAllWidgets();
		layoutActionButtons();

		if (wasReinit) {
			bookNameField.setValue(snapName);
			authorField.setValue(snapAuthor);
			scrollOffset = snapScroll;
			repositionAllWidgets();
		} else if (savedState != null) {
			restoreState();
			selectedTemplate = sessionSelectedTemplate;
			selectedFolder = sessionSelectedFolder;
		}

		if (pendingBookName != null) {
			bookNameField.setValue(pendingBookName);
			pendingBookName = null;
		}
		updatePreview();
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
			labelW = Math.max(labelW, this.font.width("书本名称") + 10);
			labelW = Math.max(labelW, this.font.width("作者") + 10);
			labelW = Math.max(labelW, this.font.width("第 99/99 页") + 10);
		}
		INPUT_X = LABEL_X + labelW;
		int bookNeed = BOOK_IMG + 12;
		int minInputW = WIDTH < 480 ? 72 : 140;
		int minLeft = INPUT_X + minInputW + 14;
		int rightW = Math.min(bookNeed, Math.max(160, WIDTH - minLeft - 8));
		if (WIDTH < 420) {
			rightW = Math.max(96, Math.min(rightW, WIDTH * 42 / 100));
		}
		RIGHT_X = Math.max(INPUT_X + 56, WIDTH - rightW - 8);
		INPUT_W = Math.max(48, RIGHT_X - INPUT_X - 14);
		updateTemplateScroll();
		layoutRefreshIcon();
	}

	private void layoutRefreshIcon() {
		refreshX = UiTheme.headerIconX(leftPos, WIDTH);
		refreshY = UiTheme.headerIconY(topPos);
	}

	@Override
	public void added() {
		super.added();
		if (pendingBookName != null) {
			bookNameField.setValue(pendingBookName);
			pendingBookName = null;
		}
		if (contentPendingRebuild) {
			contentPendingRebuild = false;
			restoring = true;
			rebuildContentLineBoxes();
			restoring = false;
			repositionAllWidgets();
			updatePreview();
		}
	}

	private void repositionAllWidgets() {
		layoutPanel();
		if (bookNameField == null || addLineBtn == null) return;
		int y = topPos + CONTENT_TOP + 2 - scrollOffset;
		int totalHeight = 0;

		int nameEditW = Math.min(INPUT_W, Math.max(56, font.width(bookNameTextEditorBtn.getMessage()) + 12));
		int nameW = Math.max(40, INPUT_W - nameEditW - 4);
		bookNameField.setX(leftPos + INPUT_X);
		bookNameField.setY(y);
		bookNameField.setWidth(nameW);
		bookNameTextEditorBtn.setX(leftPos + INPUT_X + nameW + 4);
		bookNameTextEditorBtn.setY(y);
		bookNameTextEditorBtn.setWidth(nameEditW);
		y += ROW_H; totalHeight += ROW_H;

		placeLeft(authorField, y); y += ROW_H; totalHeight += ROW_H;

		int toolbarH = layoutPageToolbar(y);
		y += toolbarH; totalHeight += toolbarH;

		for (int i = 0; i < contentLineBoxes.size(); i++) {
			contentLineBoxes.get(i).setX(leftPos + INPUT_X);
			boolean showMinus = contentLineBoxes.size() > 1;
			contentLineBoxes.get(i).setWidth(showMinus ? INPUT_W - 20 : INPUT_W);
			contentLineBoxes.get(i).setY(y);
			if (i < contentRemoveBtns.size()) {
				contentRemoveBtns.get(i).setX(leftPos + INPUT_X + INPUT_W - 18);
				contentRemoveBtns.get(i).setY(y);
				contentRemoveBtns.get(i).visible = contentLineBoxes.size() > 1;
			}
			y += ROW_H; totalHeight += ROW_H;
		}

		addLineBtn.setX(leftPos + INPUT_X);
		addLineBtn.setY(y);
		addLineBtn.setWidth(20);
		y += ROW_H; totalHeight += ROW_H;

		layoutBookPreview();

		maxScroll = Math.max(0, totalHeight + 8 - (CONTENT_BOTTOM - CONTENT_TOP));
		scrollOffset = Mth.clamp(scrollOffset, 0, maxScroll);
		layoutActionButtons();
	}

	private int layoutPageToolbar(int y) {
		prevPageBtn.visible = true;
		nextPageBtn.visible = true;
		prevPageBtn.active = currentPage > 0;
		nextPageBtn.active = currentPage < pages.size() - 1;
		removePageBtn.visible = pages.size() > 1;
		int navW = 20;
		int addPageW = Math.min(INPUT_W, Math.max(56, font.width(addPageBtn.getMessage()) + 12));
		int removeW = Math.min(INPUT_W, Math.max(56, font.width(removePageBtn.getMessage()) + 12));
		int editW = Math.min(INPUT_W, Math.max(56, font.width(textEditorBtn.getMessage()) + 12));
		List<Button> rowBtns = new ArrayList<>();
		List<Integer> rowWs = new ArrayList<>();
		rowBtns.add(prevPageBtn);
		rowWs.add(navW);
		rowBtns.add(nextPageBtn);
		rowWs.add(navW);
		rowBtns.add(addPageBtn);
		rowWs.add(addPageW);
		if (removePageBtn.visible) {
			rowBtns.add(removePageBtn);
			rowWs.add(removeW);
		}
		rowBtns.add(textEditorBtn);
		rowWs.add(editW);
		int x = leftPos + INPUT_X;
		int used = 0;
		int height = ROW_H;
		for (int i = 0; i < rowBtns.size(); i++) {
			int bw = rowWs.get(i);
			if (i > 0 && used + 4 + bw > INPUT_W) {
				y += ROW_H;
				height += ROW_H;
				x = leftPos + INPUT_X;
				used = 0;
			}
			if (used > 0) {
				x += 4;
				used += 4;
			}
			Button btn = rowBtns.get(i);
			btn.setX(x);
			btn.setY(y);
			btn.setWidth(Math.min(INPUT_W, bw));
			btn.setHeight(16);
			x += btn.getWidth();
			used += btn.getWidth();
		}
		return height;
	}

	private void placeLeft(EditBox w, int y) {
		w.setX(leftPos + INPUT_X);
		w.setY(y);
		w.setWidth(INPUT_W);
	}

	private void layoutActionButtons() {
		if (giveBtn == null) return;
		int btnH = 20;
		int footerY = topPos + HEIGHT - FOOTER_H + (FOOTER_H - btnH) / 2;
		int pad = 8;
		int gap = 6;
		int copyW = footerBtnW(copyBtn);
		int giveW = footerBtnW(giveBtn);
		int saveW = footerBtnW(saveBtn);
		int inner = Math.max(8, WIDTH - pad * 2);
		int need = copyW + giveW + saveW + gap * 2;
		if (need > inner) {
			float s = (float) (inner - gap * 2) / (copyW + giveW + saveW);
			copyW = Math.max(36, (int) (copyW * s));
			giveW = Math.max(36, (int) (giveW * s));
			saveW = Math.max(36, (int) (saveW * s));
		}
		int x = leftPos + pad;
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

	private void syncContentLinesFromBoxes() {
		if (currentPage < 0 || currentPage >= pages.size()) return;
		BookPage page = pages.get(currentPage);
		page.contentLines.clear();
		for (EditBox box : contentLineBoxes) {
			page.contentLines.add(box.getValue());
		}
	}

	private void rebuildContentLineBoxes() {
		for (EditBox box : contentLineBoxes) this.removeWidget(box);
		for (Button b : contentRemoveBtns) this.removeWidget(b);
		contentLineBoxes.clear();
		contentRemoveBtns.clear();

		if (currentPage < 0 || currentPage >= pages.size()) return;
		BookPage page = pages.get(currentPage);
		if (page.contentLines.isEmpty()) page.contentLines.add("");
		if (page.contentLines.size() > MAX_LINES_PER_PAGE) {
			page.contentLines = new ArrayList<>(page.contentLines.subList(0, MAX_LINES_PER_PAGE));
		}

		for (int i = 0; i < page.contentLines.size(); i++) {
			EditBox box = new EditBox(font, leftPos + INPUT_X, 0, INPUT_W - 20, 16, Component.empty());
			box.setValue(page.contentLines.get(i));
			box.setResponder(s -> {
				syncContentLinesFromBoxes();
				if (!restoring && !dragging) updatePreview();
			});
			this.addRenderableWidget(box);
			contentLineBoxes.add(box);
			Button minus = Button.builder(Component.literal("-"), b -> removeContentLine(box))
				.bounds(leftPos + INPUT_X + INPUT_W - 18, 0, 18, 16).build();
			this.addRenderableWidget(minus);
			contentRemoveBtns.add(minus);
		}
	}

	private void addContentLine() {
		syncContentLinesFromBoxes();
		if (currentPage < 0 || currentPage >= pages.size()) return;
		if (pages.get(currentPage).contentLines.size() >= MAX_LINES_PER_PAGE) return;
		pages.get(currentPage).contentLines.add("");
		rebuildContentLineBoxes();
		repositionAllWidgets();
		updatePreview();
	}

	private void removeContentLine(EditBox box) {
		syncContentLinesFromBoxes();
		int i = contentLineBoxes.indexOf(box);
		if (i < 0 || currentPage < 0 || currentPage >= pages.size()) return;
		BookPage page = pages.get(currentPage);
		if (i < page.contentLines.size()) page.contentLines.remove(i);
		rebuildContentLineBoxes();
		repositionAllWidgets();
		updatePreview();
	}
	private void updatePreview() {
		if (restoring) return;
		previewStack = buildBookStack();
	}

	private Component parseSectionCodes(String text, Style baseStyle) {
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
				ChatFormatting formatting = ChatFormatting.getByCode(code);
				if (formatting != null) {
					if (formatting == ChatFormatting.RESET) {
						currentStyle = baseStyle;
					} else if (formatting.isColor()) {
						Integer fmtColor = formatting.getColor();
						currentStyle = fmtColor != null ? baseStyle.withColor(fmtColor) : baseStyle;
					} else {
						currentStyle = applyFormat(currentStyle, formatting);
					}
				}
				i++;
			} else {
				currentText.append(c);
			}
		}
		if (currentText.length() > 0) {
			result.append(Component.literal(currentText.toString()).withStyle(currentStyle));
		}
		return result;
	}

	private Style applyFormat(Style style, ChatFormatting formatting) {
		return switch (formatting) {
			case BOLD -> style.withBold(true);
			case ITALIC -> style.withItalic(true);
			case UNDERLINE -> style.withUnderlined(true);
			case STRIKETHROUGH -> style.withStrikethrough(true);
			case OBFUSCATED -> style.withObfuscated(true);
			default -> style;
		};
	}

	private ItemStack buildBookStack() {
		syncContentLinesFromBoxes();
		ItemStack stack = new ItemStack(Items.WRITTEN_BOOK);

		String titleText = bookNameField.getValue().trim();
		Filterable<String> filteredTitle = Filterable.passThrough(titleText);

		String authorText = authorField.getValue().trim();
		String authorStr = authorText.isEmpty() ? "" : authorText;

		List<Filterable<Component>> pageComponents = new ArrayList<>();
		for (BookPage page : pages) {
			String fullContent = String.join("\n", page.contentLines);
			Component pageComponent = parseSectionCodes(fullContent, Style.EMPTY);
			pageComponents.add(Filterable.passThrough(pageComponent));
		}

		stack.set(DataComponents.WRITTEN_BOOK_CONTENT,
			new WrittenBookContent(filteredTitle, authorStr, 0, pageComponents, true));

		return stack;
	}

	private void loadPage(int pageIndex) {
		if (currentPage >= 0 && currentPage < pages.size()) {
			syncContentLinesFromBoxes();
		}
		showPage(pageIndex);
	}

	private void showPage(int pageIndex) {
		currentPage = Mth.clamp(pageIndex, 0, Math.max(0, pages.size() - 1));
		restoring = true;
		rebuildContentLineBoxes();
		restoring = false;
		repositionAllWidgets();
		updatePreview();
	}

	private void prevPage() {
		if (currentPage > 0) {
			loadPage(currentPage - 1);
		}
	}

	private void nextPage() {
		if (currentPage < pages.size() - 1) {
			loadPage(currentPage + 1);
		}
	}

	private void addPage() {
		syncContentLinesFromBoxes();
		pages.add(new BookPage());
		loadPage(pages.size() - 1);
	}

	private void removePage() {
		if (pages.size() <= 1) return;
		syncContentLinesFromBoxes();
		int idx = Mth.clamp(currentPage, 0, pages.size() - 1);
		pages.remove(idx);
		showPage(Math.min(idx, pages.size() - 1));
	}

	private void giveItem() {
		syncContentLinesFromBoxes();
		ItemStack stack = buildBookStack();
		if (!stack.isEmpty()) {
			ClientPlayNetworking.send(new GiveItemPayload(stack));
		}
	}

	private void copyCommand() {
		syncContentLinesFromBoxes();
		ItemStack stack = buildBookStack();
		if (stack.isEmpty()) return;
		GiveCommands.copy(GiveCommands.give(stack));
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (inTemplateRail(mouseX, mouseY)) {
			if (templateMaxScroll > 0) {
				templateScroll = Mth.clamp(templateScroll - (int) (scrollY * 20), 0, templateMaxScroll);
				syncTemplateRenameBox();
			}
			return true;
		}
		if (mouseX >= leftPos && mouseX < leftPos + INPUT_X + INPUT_W
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

	private void drawLabel(GuiGraphics g, int widgetY, String cnLabel) {
		if (widgetY + 10 > topPos + CONTENT_TOP && widgetY < topPos + CONTENT_BOTTOM) {
			UiTheme.label(g, font, cnLabel, leftPos + LABEL_X, widgetY + 4);
		}
	}

	private boolean isFooterWidget(AbstractWidget w) {
		return w == giveBtn || w == copyBtn || w == saveBtn;
	}

	private boolean handleFooterClick(double mx, double my, int button) {
		if (!UiTheme.inFooterBar(mx, my, leftPos, topPos, WIDTH, HEIGHT, FOOTER_H)) return false;
		if (UiTheme.clickWidgets(mx, my, button, copyBtn, giveBtn, saveBtn)) return true;
		if (button == 0) setFocused(null);
		return true;
	}

	private boolean isBookNavWidget(AbstractWidget w) {
		return w == bookBackBtn || w == bookFwdBtn;
	}

	private void layoutBookPreview() {
		int frameX = leftPos + RIGHT_X;
		int frameY = topPos + CONTENT_TOP;
		int frameW = Math.max(64, WIDTH - RIGHT_X - 8);
		int frameH = Math.max(80, CONTENT_BOTTOM - CONTENT_TOP);
		int pad = 4;
		float scale = Math.min((frameW - pad * 2) / (float) BOOK_IMG, (frameH - pad * 2) / (float) BOOK_IMG);
		bookDrawScale = scale <= 0f ? 0.01f : Math.min(BOOK_SCALE_MAX, scale);
		int drawW = Math.round(BOOK_IMG * bookDrawScale);
		int drawH = Math.round(BOOK_IMG * bookDrawScale);
		bookDrawX = frameX + (frameW - drawW) / 2;
		bookDrawY = frameY + (frameH - drawH) / 2;
		if (bookBackBtn == null || bookFwdBtn == null) return;
		int btnY = bookDrawY + Math.round(BOOK_PAGE_BTN_Y * bookDrawScale);
		bookBackBtn.setPosition(bookDrawX + Math.round(BOOK_PAGE_BACK_X * bookDrawScale), btnY);
		bookFwdBtn.setPosition(bookDrawX + Math.round(BOOK_PAGE_FWD_X * bookDrawScale), btnY);
		bookBackBtn.visible = currentPage > 0;
		bookFwdBtn.visible = currentPage < pages.size() - 1;
	}

	private void drawVanillaBookPreview(GuiGraphics g) {
		updatePreview();
		layoutBookPreview();
		g.pose().pushPose();
		g.pose().translate(bookDrawX, bookDrawY, 0);
		if (bookDrawScale != 1f) {
			g.pose().scale(bookDrawScale, bookDrawScale, 1f);
		}
		drawBookLeaf(g, currentPage);
		g.pose().popPose();
	}

	private void drawBookLeaf(GuiGraphics g, int pageIndex) {
		g.blit(BookViewScreen.BOOK_LOCATION, 0, 0, 0, 0, BOOK_IMG, BOOK_IMG);
		if (previewStack.isEmpty()) return;
		BookViewScreen.BookAccess access = BookViewScreen.BookAccess.fromItem(previewStack);
		if (pageIndex < 0 || pageIndex >= access.getPageCount()) return;
		List<FormattedCharSequence> lines = font.split(access.getPage(pageIndex), BOOK_PAGE_TEXT_W);
		int maxLines = Math.min(128 / 9, lines.size());
		for (int i = 0; i < maxLines; i++) {
			g.drawString(font, lines.get(i),
				BookViewScreen.PAGE_TEXT_X_OFFSET,
				BookViewScreen.PAGE_TEXT_Y_OFFSET + i * 9,
				0, false);
		}
		Component pageMsg = Component.translatable("book.pageIndicator", pageIndex + 1, Math.max(1, access.getPageCount()));
		int pageW = font.width(pageMsg);
		g.drawString(font, pageMsg, BOOK_IMG - 44 - pageW, BookViewScreen.PAGE_INDICATOR_TEXT_Y_OFFSET, 0, false);
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
			if (child instanceof AbstractWidget w && w != templateRenameBox && !isFooterWidget(w) && !isBookNavWidget(w) && w.getY() < topPos + HEIGHT - FOOTER_H) {
				w.render(g, mouseX, mouseY, partialTick);
			}
		}
		g.disableScissor();
		for (var child : this.children()) {
			if (child instanceof AbstractWidget w && isFooterWidget(w)) {
				w.render(g, mouseX, mouseY, partialTick);
			}
		}

		drawLabel(g, bookNameField.getY(), "书本名称");
		drawLabel(g, authorField.getY(), "作者");
		if (!contentLineBoxes.isEmpty()) {
			drawLabel(g, contentLineBoxes.get(0).getY(), "内容");
		}
		int pgInfoY = prevPageBtn.getY() + 4;
		if (pgInfoY + 10 > topPos + CONTENT_TOP && pgInfoY < topPos + CONTENT_BOTTOM) {
			UiTheme.label(g, font,
				Component.translatable("screen.bj_mapedit.page_info", currentPage + 1, pages.size()).getString(),
				leftPos + LABEL_X, pgInfoY);
		}

		drawVanillaBookPreview(g);
		if (bookBackBtn != null) bookBackBtn.render(g, mouseX, mouseY, partialTick);
		if (bookFwdBtn != null) bookFwdBtn.render(g, mouseX, mouseY, partialTick);

		UiTheme.drawThinScrollBar(g, leftPos + INPUT_X + INPUT_W + 4, topPos + CONTENT_TOP, CONTENT_BOTTOM - CONTENT_TOP, scrollOffset, maxScroll);
		templateMenu.draw(g, font, mouseX, mouseY);
		if (refreshHover) {
			g.renderTooltip(font, Component.translatable("screen.bj_mapedit.reset"), mouseX, mouseY);
		}
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		dragEditBox = null;
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
		boolean result = super.mouseClicked(mouseX, mouseY, button);
		if (button == 0 && !result) setFocused(null);
		if (button == 1) {
			for (EditBox box : contentLineBoxes) {
				if (box.isFocused()) {
					dragEditBox = box;
					dragAnchorPos = box.getCursorPosition();
					setHighlightPos(box, dragAnchorPos);
					break;
				}
			}
		}
		return result;
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
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
		if (dragEditBox != null && dragEditBox.isFocused() && button == 1) {
			dragging = true;
			dragEditBox.onClick(mouseX, mouseY);
			setHighlightPos(dragEditBox, dragAnchorPos);
			dragging = false;
			updatePreview();
			return true;
		}
		if (button == 0 && updateTemplateDrag(mouseX, mouseY)) return true;
		return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		dragEditBox = null;
		if (button == 0) {
			boolean moved = finishTemplateDrag();
			draggingScroll = false;
			draggingTemplateScroll = false;
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
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public boolean charTyped(char codePoint, int modifiers) {
		return super.charTyped(codePoint, modifiers);
	}

	@Override
	public void removed() {
		if (bookNameField != null) {
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

	private SavedState captureState() {
		syncContentLinesFromBoxes();
		List<String> pc = new ArrayList<>();
		for (BookPage page : pages) {
			pc.add(String.join("\n", page.contentLines));
		}
		return new SavedState(bookNameField.getValue(), authorField.getValue(), pc);
	}

	private void saveTemplate() {
		commitTemplateRename();
		SavedState state = captureState();
		savedState = state;
		BookTemplateStore.Entry entry = toTemplateEntry(defaultTemplateName(state), state);
		entry.folder = TemplateOrg.norm(selectedFolder);
		BookTemplateStore.add(entry);
		selectedTemplate = BookTemplateStore.all().size() - 1;
		updateTemplateScroll();
	}

	private void loadTemplate(int index) {
		BookTemplateStore.Entry entry = BookTemplateStore.get(index);
		if (entry == null) return;
		selectedTemplate = index;
		selectedFolder = TemplateOrg.norm(entry.folder);
		savedState = fromTemplateEntry(entry);
		applyState(savedState);
	}

	private String defaultTemplateName(SavedState state) {
		String base = stripSectionCodes(state.bookName());
		if (base.isEmpty()) {
			String author = state.author() == null ? "" : stripSectionCodes(state.author());
			base = author;
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
		for (BookTemplateStore.Entry e : BookTemplateStore.all()) {
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

	private static BookTemplateStore.Entry toTemplateEntry(String name, SavedState state) {
		BookTemplateStore.Entry e = new BookTemplateStore.Entry();
		e.name = name;
		e.bookName = state.bookName() == null ? "" : state.bookName();
		e.author = state.author() == null ? "" : state.author();
		e.pages = state.pageContents() == null ? new ArrayList<>() : new ArrayList<>(state.pageContents());
		return e;
	}

	private static SavedState fromTemplateEntry(BookTemplateStore.Entry e) {
		List<String> pages = e.pages == null ? new ArrayList<>() : new ArrayList<>(e.pages);
		return new SavedState(e.bookName == null ? "" : e.bookName, e.author == null ? "" : e.author, pages);
	}

	private void restoreState() {
		applyState(savedState);
	}

	private void applyState(SavedState state) {
		if (state == null) return;
		restoring = true;
		bookNameField.setValue(state.bookName() == null ? "" : state.bookName());
		authorField.setValue(state.author() == null ? "" : state.author());
		pages.clear();
		List<String> stored = state.pageContents();
		if (stored == null || stored.isEmpty()) {
			pages.add(new BookPage());
		} else {
			for (String text : stored) {
				BookPage page = new BookPage();
				if (text == null || text.isEmpty()) {
					page.contentLines.add("");
				} else {
					for (String line : text.split("\n", -1)) {
						page.contentLines.add(line);
					}
					if (page.contentLines.size() > MAX_LINES_PER_PAGE) {
						page.contentLines = new ArrayList<>(page.contentLines.subList(0, MAX_LINES_PER_PAGE));
					}
				}
				pages.add(page);
			}
		}
		currentPage = 0;
		rebuildContentLineBoxes();
		restoring = false;
		scrollOffset = 0;
		repositionAllWidgets();
		updatePreview();
	}

	private void resetFields() {
		bookNameField.setValue("");
		authorField.setValue("");
		pages.clear();
		pages.add(new BookPage());
		currentPage = 0;
		rebuildContentLineBoxes();
		savedState = null;
		sessionSelectedTemplate = -1;
		sessionSelectedFolder = "";
		selectedTemplate = -1;
		selectedFolder = "";
		scrollOffset = 0;
		repositionAllWidgets();
		updatePreview();
	}

	private void openTextEditor() {
		syncContentLinesFromBoxes();
		Minecraft.getInstance().setScreen(new TextEditorScreen(this, allPagesText(), newText -> {
			if (newText == null) return;
			applyImportedText(newText);
			contentPendingRebuild = true;
		}));
	}

	private String allPagesText() {
		List<String> parts = new ArrayList<>();
		for (BookPage page : pages) {
			parts.add(String.join("\n", page.contentLines));
		}
		return String.join("\n", parts);
	}

	private void applyImportedText(String raw) {
		List<String> lines = wrapToBookLines(this.font, raw == null ? "" : raw);
		pages.clear();
		int maxPages = WritableBookContent.MAX_PAGES;
		for (int i = 0; i < lines.size() && pages.size() < maxPages; i += MAX_LINES_PER_PAGE) {
			BookPage page = new BookPage();
			int end = Math.min(lines.size(), i + MAX_LINES_PER_PAGE);
			page.contentLines.addAll(lines.subList(i, end));
			pages.add(page);
		}
		if (pages.isEmpty()) {
			pages.add(new BookPage());
		}
		currentPage = 0;
	}

	private static List<String> wrapToBookLines(Font font, String raw) {
		String text = stripBookDefaultWhite(raw.replace("\r\n", "\n").replace('\r', '\n'));
		List<String> lines = new ArrayList<>();
		if (text.isEmpty()) {
			lines.add("");
			return lines;
		}
		font.getSplitter().splitLines(text, BOOK_PAGE_TEXT_W, Style.EMPTY, true, (style, start, end) -> {
			if (start < 0 || end < start || start > text.length()) return;
			int a = Math.max(0, start);
			int b = Math.min(end, text.length());
			while (a < b && isBookControl(text.charAt(a))) a++;
			while (b > a && isBookControl(text.charAt(b - 1))) b--;
			lines.add(sanitizeBookLine(a < b ? text.substring(a, b) : ""));
		});
		if (lines.isEmpty()) lines.add("");
		return lines;
	}

	private static boolean isBookControl(char c) {
		return c == '\n' || c == '\r' || c == '\t' || (c < 32 && c != '\u00a7') || c == 127;
	}

	private static String sanitizeBookLine(String line) {
		StringBuilder sb = new StringBuilder(line.length());
		for (int i = 0; i < line.length(); i++) {
			char c = line.charAt(i);
			if (isBookControl(c)) continue;
			sb.append(c);
		}
		return sb.toString();
	}

	private static String stripBookDefaultWhite(String text) {
		StringBuilder sb = new StringBuilder(text.length());
		for (int i = 0; i < text.length(); i++) {
			if (text.charAt(i) == '\u00a7' && i + 1 < text.length()) {
				char code = Character.toLowerCase(text.charAt(i + 1));
				if (code == 'f') {
					i++;
					continue;
				}
			}
			sb.append(text.charAt(i));
		}
		return sb.toString();
	}

	private void openBookNameTextEditor() {
		String currentText = bookNameField.getValue();
		pendingBookName = currentText;
		Minecraft.getInstance().setScreen(new TextEditorScreen(this, currentText, newText -> {
			if (newText != null) pendingBookName = newText;
		}));
	}

	private boolean inTemplateRail(double mouseX, double mouseY) {
		int x = templateRailX;
		return mouseX >= x && mouseX < x + templateRailW && mouseY >= topPos && mouseY < topPos + HEIGHT;
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
		return BookTemplateStore.rows(collapsedFolders);
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
		TemplateRailUi.drawHeader(g, font, Component.translatable("screen.bj_mapedit.book_templates").getString(), x, w, topPos, hx, hy, hasSel);
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
			BookTemplateStore.moveToFolder(index, templateDrag.hoverFolder);
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
		String name = BookTemplateStore.addFolder();
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
		BookTemplateStore.Entry kept = selectedTemplate >= 0 ? BookTemplateStore.get(selectedTemplate) : null;
		BookTemplateStore.removeFolder(name);
		collapsedFolders.remove(TemplateOrg.norm(name));
		if (TemplateOrg.folderEq(selectedFolder, name)) {
			selectedFolder = "";
			selectedTemplate = -1;
		} else if (kept != null) {
			selectedTemplate = BookTemplateStore.all().indexOf(kept);
		}
		updateTemplateScroll();
	}

	private void deleteTemplate(int index) {
		if (renamingTemplate == index) cancelTemplateRename();
		else commitTemplateRename();
		BookTemplateStore.remove(index);
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
		BookTemplateStore.Entry entry = BookTemplateStore.get(index);
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
			BookTemplateStore.renameFolder(from, to);
			String normTo = TemplateOrg.norm(to);
			if (!normTo.isEmpty() && !TemplateOrg.folderEq(from, normTo)) {
				TemplateOrg.renameCollapsed(collapsedFolders, from, normTo);
				if (TemplateOrg.folderEq(selectedFolder, from)) selectedFolder = normTo;
			}
			cancelTemplateRename();
			return;
		}
		if (renamingTemplate < 0) return;
		BookTemplateStore.rename(renamingTemplate, templateRenameBox.getValue());
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
}

