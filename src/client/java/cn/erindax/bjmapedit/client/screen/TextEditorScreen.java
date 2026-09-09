package cn.erindax.bjmapedit.client.screen;

import cn.erindax.bjmapedit.client.widget.ImeCaret;
import cn.erindax.bjmapedit.client.widget.SectionText;
import cn.erindax.bjmapedit.client.widget.UiTheme;
import com.mojang.blaze3d.platform.NativeImage;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Environment(EnvType.CLIENT)
public class TextEditorScreen extends Screen {

	private int WIDTH = 460;
	private int HEIGHT = 360;

	private int TEXT_LEFT = 10;
	private int TEXT_TOP = 30;
	private int TEXT_RIGHT = 450;
	private int TEXT_BOTTOM = 210;
	private int toolY;
	private int swatchStartX;
	private int paletteTextX;
	private int paletteTextW;
	private int clearTextX;
	private int clearTextW;
	private int clearAllX;
	private int clearAllW;
	private DynamicTexture pickerTex;
	private int pickerTexSize;
	private float pickerTexHue = -1f;
	private static final ResourceLocation PICKER_RL = ResourceLocation.fromNamespaceAndPath("bj_mapedit", "dynamic/hsv_picker");
	private int lineH() {
		return this.font != null ? this.font.lineHeight + 3 : 12;
	}

	private static final int SWATCH_SIZE = 12;
	private static final int SWATCH_GAP = 3;
	private static final int STYLE_H = 9;
	private static final int TOOL_H = 22;
	private static final int FOOTER_H = 30;
	private static final int TEXT_PAD = 4;
	private static final int OBF_SIZE = 6;

	private static final int BG_COLOR = UiTheme.PANEL_BG;
	private static final int PANEL_BORDER = UiTheme.PANEL_BORDER;
	private static final int TEXT_AREA_COLOR = 0xFF0E0F10;
	private static final int TEXT_DARK = 0xFFE6EDF3;
	private static final int SCROLL_BG = 0x40FFFFFF;
	private static final int SCROLL_THUMB = 0xA0FFFFFF;
	private static final int CARET_COLOR = 0xFFE6EDF3;
	private static final int SEL_COLOR = 0x6055AAFF;
	private static final int TITLE_COLOR = 0xFFFFFFFF;

	private static final int[] MC_COLORS = SectionText.MC_COLORS;
	private static final char[] MC_COLOR_CODES = SectionText.MC_COLOR_CODES;

	private final Screen parent;
	private final Consumer<String> callback;
	private String fullText;

	private int leftPos, topPos;

	private List<String> rawLines = new ArrayList<>();

	private int cursorLine, cursorCol;
	private int selStartLine = -1, selStartCol = -1;
	private int scrollLines;
	private int hScroll;
	private boolean draggingScroll;
	private boolean draggingHScroll;
	private double scrollGrabOffset;
	private double hScrollGrabOffset;
	private int tickCount;
	private long lastClickTime;
	private int clickCount;
	private int clickLine, clickCol;

	private int colorIdx = 15;
	private String customHex;
	private boolean tbBold, tbItalic, tbUnderlined, tbStrikethrough, tbObfuscated;
	private EditBox hexColorField;
	private boolean classicPaletteOpen;
	private boolean pickingGradient;
	private boolean gradientDirty;
	private float pickerHue = 0f;
	private int pickerRgb = 0xFFFFFF;

	private Button doneBtn, cancelBtn;

	private static final String[] TOGGLE_TIPS = {
		"screen.bj_mapedit.tip_bold",
		"screen.bj_mapedit.tip_italic",
		"screen.bj_mapedit.tip_underline",
		"screen.bj_mapedit.tip_strike",
		"screen.bj_mapedit.tip_obfuscated"
	};

	public TextEditorScreen(Screen parent, String initialText, Consumer<String> callback) {
		this(parent, initialText, callback, Component.translatable("screen.bj_mapedit.edit_text"));
	}

	public TextEditorScreen(Screen parent, String initialText, Consumer<String> callback, Component title) {
		super(title);
		this.parent = parent;
		this.callback = callback;
		this.fullText = initialText != null ? initialText : "";
	}

	@Override
	protected void init() {
		super.init();
		layoutPanel();

		doneBtn = Button.builder(Component.translatable("screen.bj_mapedit.done"), b -> {
			if (callback != null) callback.accept(fullText);
			Minecraft.getInstance().setScreen(parent);
		}).bounds(0, 0, 70, 20).build();
		this.addRenderableWidget(doneBtn);

		cancelBtn = Button.builder(Component.translatable("screen.bj_mapedit.cancel"), b -> {
			if (callback != null) callback.accept(null);
			Minecraft.getInstance().setScreen(parent);
		}).bounds(0, 0, 70, 20).build();
		this.addRenderableWidget(cancelBtn);

		hexColorField = new EditBox(font, 0, 0, 48, 16, Component.literal("Hex"));
		hexColorField.setValue("");
		hexColorField.setMaxLength(7);
		hexColorField.setHint(Component.literal("#RRGGBB"));
		this.addRenderableWidget(hexColorField);

		layoutChrome();
		parseLines();
		if (rawLines.isEmpty()) {
			rawLines.add("");
		}
		cursorLine = rawLines.size() - 1;
		cursorCol = rawLines.get(cursorLine).length();
		draggingScroll = false;
		draggingHScroll = false;
		ensureCursorVisible();
		setFocused(null);
	}

	private void layoutPanel() {
		int margin = 8;
		WIDTH = Math.max(8, this.width - margin * 2);
		HEIGHT = Math.max(8, this.height - margin * 2);
		leftPos = margin;
		topPos = margin;
		TEXT_LEFT = 10;
		TEXT_RIGHT = WIDTH - 12;
		TEXT_TOP = UiTheme.HEADER_H + 8;
		TEXT_BOTTOM = HEIGHT - FOOTER_H - TOOL_H - 10;
		if (TEXT_BOTTOM - TEXT_TOP < 48) {
			TEXT_BOTTOM = TEXT_TOP + 48;
		}
		int bandY = topPos + HEIGHT - FOOTER_H - TOOL_H;
		int chipY = bandY + (TOOL_H - SWATCH_SIZE) / 2;
		toolY = chipY + (SWATCH_SIZE - STYLE_H) / 2;
	}

	private void layoutChrome() {
		int barY = topPos + HEIGHT - FOOTER_H;
		int btnH = 20;
		int btnY = barY + (FOOTER_H - btnH) / 2;
		int doneW = Math.max(52, font.width(doneBtn.getMessage()) + 22);
		int cancelW = Math.max(52, font.width(cancelBtn.getMessage()) + 22);
		doneBtn.setPosition(leftPos + WIDTH - 8 - doneW, btnY);
		doneBtn.setWidth(doneW);
		doneBtn.setHeight(btnH);
		cancelBtn.setPosition(doneBtn.getX() - 6 - cancelW, btnY);
		cancelBtn.setWidth(cancelW);
		cancelBtn.setHeight(btnH);

		swatchStartX = styleX(5) + 8;
		paletteTextW = font.width("调色板");
		clearTextW = font.width("清除格式");
		clearAllW = font.width("清除文本");
		paletteTextX = swatchStartX + 16 * SWATCH_SIZE + 15 * SWATCH_GAP + 10;
		clearTextX = paletteTextX + paletteTextW + 10;
		clearAllX = clearTextX + clearTextW + 10;
		int right = leftPos + WIDTH - 8;
		if (clearAllX + clearAllW > right) {
			clearAllX = right - clearAllW;
			clearTextX = clearAllX - 10 - clearTextW;
			paletteTextX = clearTextX - 10 - paletteTextW;
		}
		layoutHexInPalette();
	}

	private void layoutHexInPalette() {
		hexColorField.visible = classicPaletteOpen;
		if (!classicPaletteOpen) {
			if (hexFocused()) setFocused(null);
			return;
		}
		hexColorField.setWidth(56);
		hexColorField.setHeight(16);
		hexColorField.setPosition(classicPaletteX() + 6, classicPaletteY() + classicPaletteH() - 20);
	}

	private Component styleLetter(int i, int rgb) {
		Style s = Style.EMPTY.withColor(rgb);
		return switch (i) {
			case 0 -> Component.literal("B").withStyle(s.withBold(true));
			case 1 -> Component.literal("I").withStyle(s.withItalic(true));
			case 2 -> Component.literal("U").withStyle(s);
			case 3 -> Component.literal("S").withStyle(s.withStrikethrough(true));
			default -> Component.empty();
		};
	}

	private int styleX(int i) {
		return leftPos + 10 + i * (SWATCH_SIZE + SWATCH_GAP);
	}

	private int styleW(int i) {
		return SWATCH_SIZE;
	}

	private int chipY() {
		return toolY - (SWATCH_SIZE - STYLE_H) / 2;
	}

	private boolean hitStyle(int i, double mx, double my) {
		int x = styleX(i);
		int y = chipY();
		return mx >= x && mx < x + SWATCH_SIZE && my >= y && my < y + SWATCH_SIZE;
	}

	private boolean hitPalette(double mx, double my) {
		return mx >= paletteTextX && mx < paletteTextX + paletteTextW
			&& my >= toolY - 2 && my < toolY + STYLE_H + 2;
	}

	private boolean hitClear(double mx, double my) {
		return mx >= clearTextX && mx < clearTextX + clearTextW
			&& my >= toolY - 2 && my < toolY + STYLE_H + 2;
	}

	private boolean hitClearAll(double mx, double my) {
		return mx >= clearAllX && mx < clearAllX + clearAllW
			&& my >= toolY - 2 && my < toolY + STYLE_H + 2;
	}

	private int swatchX(int i) {
		return swatchStartX + i * (SWATCH_SIZE + SWATCH_GAP);
	}

	private int swatchY(int i) {
		return chipY();
	}

	private int scrollBarX() {
		return leftPos + TEXT_RIGHT + 1;
	}

	private int hBarX() {
		return leftPos + TEXT_LEFT;
	}

	private int hBarY() {
		return topPos + TEXT_BOTTOM + 3;
	}

	private int hBarW() {
		return Math.max(1, TEXT_RIGHT - TEXT_LEFT);
	}

	private int textViewW() {
		return Math.max(1, TEXT_RIGHT - TEXT_LEFT - TEXT_PAD * 2);
	}

	private int textDrawX() {
		return textOriginX() - hScroll;
	}

	private int maxHScroll() {
		int maxW = 0;
		for (String line : rawLines) {
			int w = visibleWidthUpTo(line, line.length());
			if (w > maxW) maxW = w;
		}
		return Math.max(0, maxW + 8 - textViewW());
	}

	private void clampHScroll() {
		hScroll = Mth.clamp(hScroll, 0, maxHScroll());
	}

	private int textOriginX() {
		return leftPos + TEXT_LEFT + TEXT_PAD;
	}

	private int activeRgb() {
		if (hexColorField != null) {
			String hex = hexColorField.getValue().trim();
			try {
				if (hex.startsWith("#")) hex = hex.substring(1);
				if (hex.length() == 6) return 0xFF000000 | Integer.parseInt(hex, 16);
			} catch (NumberFormatException ignored) {}
		}
		return MC_COLORS[Mth.clamp(colorIdx, 0, MC_COLORS.length - 1)];
	}

	private boolean hexFocused() {
		return this.getFocused() == hexColorField;
	}

	private void parseLines() {
		rawLines.clear();
		String[] parts = fullText.split("\n", -1);
		for (String p : parts) {
			rawLines.add(p);
		}
	}

	private void fullTextFromLines() {
		fullText = String.join("\n", rawLines);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		this.renderBackground(g, mouseX, mouseY, partialTick);
		UiTheme.drawHeader(g, font, this.title, leftPos, topPos, WIDTH);

		int toolBandY = topPos + HEIGHT - FOOTER_H - TOOL_H;
		g.fill(leftPos + 1, toolBandY, leftPos + WIDTH - 1, toolBandY + TOOL_H, 0xFF161718);
		g.fill(leftPos + 1, toolBandY, leftPos + WIDTH - 1, toolBandY + 1, 0xFF2B2C2D);
		int fy = topPos + HEIGHT - FOOTER_H;
		g.fill(leftPos + 1, fy, leftPos + WIDTH - 1, topPos + HEIGHT - 1, 0xFF191A1B);
		g.fill(leftPos + 1, fy, leftPos + WIDTH - 1, fy + 1, 0xFF2B2C2D);

		renderToolbar(g, mouseX, mouseY);

		for (var child : this.children()) {
			if (child == hexColorField) continue;
			if (child instanceof net.minecraft.client.gui.components.Renderable w) {
				w.render(g, mouseX, mouseY, partialTick);
			}
		}

		int wellX = leftPos + TEXT_LEFT;
		int wellY = topPos + TEXT_TOP;
		int wellW = TEXT_RIGHT - TEXT_LEFT;
		int wellH = TEXT_BOTTOM - TEXT_TOP;
		g.enableScissor(wellX, wellY, wellX + wellW, wellY + wellH);
		renderTextArea(g, mouseX, mouseY);
		renderCursorAndSelection(g);
		g.disableScissor();
		g.renderOutline(wellX - 1, wellY - 1, wellW + 2, wellH + 2, 0xFF2B2C2D);

		int maxScroll = Math.max(0, rawLines.size() - getVisibleLines());
		UiTheme.drawThinScrollBar(g, scrollBarX(), wellY, wellH, scrollLines, maxScroll);
		int maxH = maxHScroll();
		hScroll = Mth.clamp(hScroll, 0, maxH);
		UiTheme.drawThinHScrollBar(g, hBarX(), hBarY(), hBarW(), hScroll, maxH);

		if (classicPaletteOpen) {
			UiTheme.pushOverlay(g);
			renderClassicPalette(g);
			hexColorField.render(g, mouseX, mouseY, partialTick);
			UiTheme.popOverlay(g);
		}
		renderEditorTooltips(g, mouseX, mouseY);
	}

	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		super.renderBackground(g, mouseX, mouseY, partialTick);
		UiTheme.drawPanel(g, leftPos, topPos, WIDTH, HEIGHT);
	}

	private int getVisibleLines() {
		return Math.max(1, (TEXT_BOTTOM - TEXT_TOP) / lineH());
	}

	private void renderToolbar(GuiGraphics g, int mouseX, int mouseY) {
		boolean[] states = {tbBold, tbItalic, tbUnderlined, tbStrikethrough, tbObfuscated};
		for (int i = 0; i < 5; i++) {
			boolean hover = hitStyle(i, mouseX, mouseY);
			int rgb = states[i] ? 0xFFFFFF : (hover ? 0xC8CDD1 : 0x8B949E);
			int x = styleX(i);
			int y = chipY();
			int fill = hover && !states[i] ? 0xFF222324 : 0xFF111111;
			drawSwatch(g, x, y, SWATCH_SIZE, fill, states[i]);
			if (i == 4) {
				int ox = x + (SWATCH_SIZE - OBF_SIZE) / 2;
				int oy = y + (SWATCH_SIZE - OBF_SIZE) / 2;
				drawObfuscatedMark(g, ox, oy, rgb);
			} else {
				Component letter = styleLetter(i, rgb);
				int gw = font.width(letter);
				int dx = x + (SWATCH_SIZE - gw + 1) / 2;
				int dy = y + SWATCH_SIZE - 2 - 7;
				g.drawString(font, letter, dx, dy, rgb, false);
				if (i == 2) {
					int lineY = y + SWATCH_SIZE - 2;
					g.fill(dx, lineY, dx + gw, lineY + 1, 0xFF000000 | (rgb & 0xFFFFFF));
				}
			}
		}

		int splitX = swatchStartX - 5;
		g.fill(splitX, chipY(), splitX + 1, chipY() + SWATCH_SIZE, 0xFF2B2C2D);

		for (int i = 0; i < MC_COLORS.length; i++) {
			drawSwatch(g, swatchX(i), swatchY(i), SWATCH_SIZE, MC_COLORS[i], i == colorIdx);
		}

		boolean palHover = hitPalette(mouseX, mouseY);
		int palColor = classicPaletteOpen || palHover ? 0xFFE6EDF3 : 0xFF8B949E;
		g.drawString(font, "调色板", paletteTextX, toolY, palColor, false);
		int clearColor = hitClear(mouseX, mouseY) ? 0xFFE6EDF3 : 0xFF8B949E;
		g.drawString(font, "清除格式", clearTextX, toolY, clearColor, false);
		int clearAllColor = hitClearAll(mouseX, mouseY) ? 0xFFE6EDF3 : 0xFF8B949E;
		g.drawString(font, "清除文本", clearAllX, toolY, clearAllColor, false);
	}

	private static void drawObfuscatedMark(GuiGraphics g, int x, int y, int rgb) {
		int on = 0xFF000000 | (rgb & 0xFFFFFF);
		int off = (on & 0x00FFFFFF) | 0x40000000;
		for (int row = 0; row < OBF_SIZE; row++) {
			for (int col = 0; col < OBF_SIZE; col++) {
				g.fill(x + col, y + row, x + col + 1, y + row + 1, ((row + col) & 1) == 0 ? on : off);
			}
		}
	}

	private static void drawSwatch(GuiGraphics g, int x, int y, int size, int fill, boolean selected) {
		if (selected) {
			g.fill(x - 1, y - 1, x + size + 1, y + size + 1, 0xFFFFFFFF);
			g.fill(x, y, x + size, y + size, 0xFF111111);
			g.fill(x + 1, y + 1, x + size - 1, y + size - 1, fill);
		} else {
			g.fill(x, y, x + size, y + size, 0xFF3C3C3C);
			g.fill(x + 1, y + 1, x + size - 1, y + size - 1, fill);
		}
	}

	private static void drawSwatch(GuiGraphics g, int x, int y, int size, int fill, int border) {
		g.fill(x, y, x + size, y + size, border);
		g.fill(x + 1, y + 1, x + size - 1, y + size - 1, fill);
	}

	private int classicGridW() {
		return PALETTE_COLS * (PALETTE_CELL + 2) + 4;
	}

	private int classicGridH() {
		return (CLASSIC_COLORS.length / PALETTE_COLS) * (PALETTE_CELL + 2) + 4;
	}

	private int pickerX() {
		return classicPaletteX() + 6 + classicGridW() + 8;
	}

	private int pickerY() {
		return classicPaletteY() + 6;
	}

	private int pickerSize() {
		return Math.max(36, classicGridH() - 2);
	}

	private void renderEditorTooltips(GuiGraphics g, int mouseX, int mouseY) {
		for (int i = 0; i < 5; i++) {
			if (hitStyle(i, mouseX, mouseY)) {
				g.renderTooltip(font, Component.translatable(TOGGLE_TIPS[i]), mouseX, mouseY);
				return;
			}
		}
		for (int i = 0; i < MC_COLORS.length; i++) {
			int cx = swatchX(i);
			int cy = swatchY(i);
			if (mouseX >= cx && mouseX < cx + SWATCH_SIZE && mouseY >= cy && mouseY < cy + SWATCH_SIZE) {
				g.renderTooltip(font, Component.translatable("screen.bj_mapedit.color_" + i), mouseX, mouseY);
				return;
			}
		}
		if (hitPalette(mouseX, mouseY)) {
			g.renderTooltip(font, Component.translatable("screen.bj_mapedit.palette"), mouseX, mouseY);
			return;
		}
		if (hitClear(mouseX, mouseY)) {
			g.renderTooltip(font, Component.translatable("screen.bj_mapedit.clear_fmt"), mouseX, mouseY);
			return;
		}
		if (hitClearAll(mouseX, mouseY)) {
			g.renderTooltip(font, Component.translatable("screen.bj_mapedit.clear_text"), mouseX, mouseY);
			return;
		}
		if (hexColorField != null && hexColorField.visible && mouseX >= hexColorField.getX() && mouseX < hexColorField.getX() + hexColorField.getWidth()
			&& mouseY >= hexColorField.getY() && mouseY < hexColorField.getY() + hexColorField.getHeight()) {
			g.renderTooltip(font, Component.translatable("screen.bj_mapedit.tip_hex"), mouseX, mouseY);
		}
	}

	private void renderGradientPicker(GuiGraphics g, int px, int py) {
		int size = pickerSize();
		ensurePickerTex(size);
		g.fill(px - 1, py - 1, px + size + 10, py + size + 1, 0xFF2B2C2D);
		if (pickerTex != null) {
			g.blit(PICKER_RL, px, py, 0, 0, size + 8, size, size + 8, size);
		}
	}

	private void ensurePickerTex(int size) {
		if (pickerTex != null && pickerTexSize == size && pickerTexHue == pickerHue) return;
		int w = size + 8;
		if (pickerTex == null || pickerTexSize != size) {
			releasePickerTex();
			NativeImage img = new NativeImage(w, size, false);
			pickerTex = new DynamicTexture(img);
			pickerTexSize = size;
			Minecraft.getInstance().getTextureManager().register(PICKER_RL, pickerTex);
		}
		NativeImage img = pickerTex.getPixels();
		if (img == null) return;
		for (int y = 0; y < size; y++) {
			for (int x = 0; x < size; x++) {
				float sat = size <= 1 ? 0f : x / (float) (size - 1);
				float val = size <= 1 ? 1f : 1f - y / (float) (size - 1);
				img.setPixelRGBA(x, y, argbToNative(0xFF000000 | hsvToRgb(pickerHue, sat, val)));
			}
			int hueRgb = hsvToRgb(size <= 1 ? 0f : y / (float) (size - 1), 1f, 1f);
			for (int x = size; x < w; x++) {
				img.setPixelRGBA(x, y, x >= size + 2 && x < size + 8 ? argbToNative(0xFF000000 | hueRgb) : 0);
			}
		}
		pickerTex.upload();
		pickerTexHue = pickerHue;
	}

	private static int argbToNative(int argb) {
		int a = (argb >> 24) & 0xFF;
		int r = (argb >> 16) & 0xFF;
		int g = (argb >> 8) & 0xFF;
		int b = argb & 0xFF;
		return (a << 24) | (b << 16) | (g << 8) | r;
	}

	private void releasePickerTex() {
		if (pickerTex == null) return;
		Minecraft.getInstance().getTextureManager().release(PICKER_RL);
		pickerTex.close();
		pickerTex = null;
		pickerTexSize = 0;
		pickerTexHue = -1f;
	}

	private static int hsvToRgb(float h, float s, float v) {
		float c = v * s;
		float x = c * (1f - Math.abs((h * 6f) % 2f - 1f));
		float m = v - c;
		float r, g, b;
		float hf = h * 6f;
		if (hf < 1) { r = c; g = x; b = 0; }
		else if (hf < 2) { r = x; g = c; b = 0; }
		else if (hf < 3) { r = 0; g = c; b = x; }
		else if (hf < 4) { r = 0; g = x; b = c; }
		else if (hf < 5) { r = x; g = 0; b = c; }
		else { r = c; g = 0; b = x; }
		int ri = Math.min(255, Math.max(0, (int) ((r + m) * 255)));
		int gi = Math.min(255, Math.max(0, (int) ((g + m) * 255)));
		int bi = Math.min(255, Math.max(0, (int) ((b + m) * 255)));
		return (ri << 16) | (gi << 8) | bi;
	}

	private boolean tryGradientClick(double mouseX, double mouseY) {
		int px = pickerX();
		int py = pickerY();
		int size = pickerSize();
		if (mouseX >= px + size + 2 && mouseX < px + size + 8 && mouseY >= py && mouseY < py + size) {
			pickerHue = (float) ((mouseY - py) / (size - 1.0));
			pickingGradient = true;
			return true;
		}
		if (mouseX >= px && mouseX < px + size && mouseY >= py && mouseY < py + size) {
			float sat = (float) ((mouseX - px) / (size - 1.0));
			float val = 1f - (float) ((mouseY - py) / (size - 1.0));
			int rgb = hsvToRgb(pickerHue, sat, val);
			pickerRgb = rgb;
			hexColorField.setValue(String.format("%06x", rgb));
			pickingGradient = true;
			gradientDirty = true;
			return true;
		}
		return false;
	}

	private void renderTextArea(GuiGraphics g, int mouseX, int mouseY) {
		int visible = getVisibleLines();
		g.fill(leftPos + TEXT_LEFT, topPos + TEXT_TOP, leftPos + TEXT_RIGHT, topPos + TEXT_BOTTOM, TEXT_AREA_COLOR);
		for (int i = 0; i < visible; i++) {
			int lineIdx = i + scrollLines;
			if (lineIdx >= rawLines.size()) break;
			int y = topPos + TEXT_TOP + i * lineH();
			renderRawLine(g, rawLines.get(lineIdx), textDrawX(), y);
		}
	}

	private void renderRawLine(GuiGraphics g, String rawLine, int x, int y) {
		if (rawLine.isEmpty()) return;
		int cx = x;
		Style currentStyle = Style.EMPTY;
		StringBuilder currentText = new StringBuilder();
		for (int i = 0; i < rawLine.length(); i++) {
			char c = rawLine.charAt(i);
			if (c == '\u00a7' && i + 1 < rawLine.length()) {
				if (currentText.length() > 0) {
					cx += drawStyledString(g, currentText.toString(), currentStyle, cx, y);
					currentText.setLength(0);
				}
				currentStyle = styleAfter(rawLine, i, currentStyle);
				i += formatCodeLength(rawLine, i) - 1;
			} else {
				currentText.append(c);
			}
		}
		if (currentText.length() > 0) {
			drawStyledString(g, currentText.toString(), currentStyle, cx, y);
		}
	}

	private int drawStyledString(GuiGraphics g, String text, Style style, int x, int y) {
		if (text.isEmpty()) return 0;
		MutableComponent comp = Component.literal(text).withStyle(style);
		int color = style.getColor() != null ? style.getColor().getValue() : TEXT_DARK;
		g.drawString(font, comp, x, y, color, false);
		return font.width(comp);
	}

	private void renderCursorAndSelection(GuiGraphics g) {
		int visible = getVisibleLines();
		int selStartL, selStartC, selEndL, selEndC;
		if (selStartLine >= 0) {
			if (isBefore(selStartLine, selStartCol, cursorLine, cursorCol)) {
				selStartL = selStartLine; selStartC = selStartCol;
				selEndL = cursorLine; selEndC = cursorCol;
			} else {
				selStartL = cursorLine; selStartC = cursorCol;
				selEndL = selStartLine; selEndC = selStartCol;
			}
		} else {
			selStartL = cursorLine; selStartC = cursorCol;
			selEndL = cursorLine; selEndC = cursorCol;
		}

		for (int i = 0; i < visible; i++) {
			int lineIdx = i + scrollLines;
			if (lineIdx >= rawLines.size()) break;
			int y = topPos + TEXT_TOP + i * lineH();
			String rawLine = rawLines.get(lineIdx);

			if (lineIdx >= selStartL && lineIdx <= selEndL) {
				int selX0 = 0, selX1 = 0;
				if (lineIdx == selStartL) selX0 = visibleWidthUpTo(rawLine, selStartC);
				if (lineIdx == selEndL) {
					selX1 = visibleWidthUpTo(rawLine, selEndC);
				} else {
					selX1 = visibleWidthUpTo(rawLine, rawLine.length());
				}
				if (lineIdx > selStartL) selX0 = 0;
				int sx = textDrawX() + selX0;
				int ex = textDrawX() + selX1;
				if (ex > sx) {
					g.fill(sx, y, ex, y + lineH(), SEL_COLOR);
				}
			}
		}

		if (selStartL == selEndL && selStartC == selEndC) {
			int curLine = cursorLine - scrollLines;
			if (curLine >= 0 && curLine < visible && cursorLine < rawLines.size()) {
				int y = topPos + TEXT_TOP + curLine * lineH();
				int cx = textDrawX() + visibleWidthUpTo(rawLines.get(cursorLine), cursorCol);
				boolean caretInWell = cx >= leftPos + TEXT_LEFT && cx < leftPos + TEXT_RIGHT;
				if (caretInWell && (hexColorField == null || !hexColorField.isFocused())) {
					ImeCaret.moveTo(cx, y + lineH());
				}
				if ((tickCount / 10) % 2 == 0) {
					if (atVisibleEnd(rawLines.get(cursorLine), cursorCol)) {
						g.drawString(font, "_", cx, y, CARET_COLOR, false);
					} else {
						g.fill(cx, y + 1, cx + 1, y + 1 + UiTheme.INSERT_CARET_H, CARET_COLOR);
					}
				}
			}
		}
	}

	private boolean atVisibleEnd(String rawLine, int rawPos) {
		for (int i = rawPos; i < rawLine.length(); ) {
			if (rawLine.charAt(i) == '\u00a7' && i + 1 < rawLine.length()) {
				i += formatCodeLength(rawLine, i);
			} else {
				return false;
			}
		}
		return true;
	}

	private boolean isBefore(int l1, int c1, int l2, int c2) {
		if (l1 != l2) return l1 < l2;
		return c1 <= c2;
	}

	private int formatCodeLength(String line, int pos) {
		if (pos >= line.length()) return 0;
		if (line.charAt(pos) != '\u00a7') return 0;
		if (pos + 1 < line.length() && (line.charAt(pos + 1) == 'x' || line.charAt(pos + 1) == 'X')) {
			if (pos + 13 < line.length()) {
				for (int i = pos + 2; i <= pos + 12; i += 2) {
					if (line.charAt(i) != '\u00a7') return 2;
				}
				return 14;
			}
			return 2;
		}
		return 2;
	}

	private Style styleAfter(String line, int pos, Style current) {
		if (pos + 1 >= line.length()) return current;
		char code = line.charAt(pos + 1);
		if (code == 'x' || code == 'X') {
			if (formatCodeLength(line, pos) == 14) {
				StringBuilder rgb = new StringBuilder(6);
				for (int k = pos + 2; k <= pos + 12; k += 2) {
					rgb.append(line.charAt(k + 1));
				}
				try {
					return Style.EMPTY.withColor(Integer.parseInt(rgb.toString(), 16));
				} catch (NumberFormatException ignored) {}
			}
			return current;
		}
		ChatFormatting fmt = ChatFormatting.getByCode(code);
		if (fmt == null) return current;
		if (fmt == ChatFormatting.RESET) return Style.EMPTY;
		if (fmt.isColor()) {
			Integer clr = fmt.getColor();
			return clr != null ? Style.EMPTY.withColor(clr) : Style.EMPTY;
		}
		return SectionText.applyFormat(current, fmt);
	}

	private int styledWidth(String text, Style style) {
		if (text.isEmpty()) return 0;
		return font.width(Component.literal(text).withStyle(style));
	}

	private int visibleWidthUpTo(String rawLine, int rawPos) {
		int limit = Math.min(rawPos, rawLine.length());
		int width = 0;
		Style currentStyle = Style.EMPTY;
		StringBuilder currentText = new StringBuilder();
		for (int i = 0; i < limit; i++) {
			char c = rawLine.charAt(i);
			if (c == '\u00a7' && i + 1 < rawLine.length()) {
				if (currentText.length() > 0) {
					width += styledWidth(currentText.toString(), currentStyle);
					currentText.setLength(0);
				}
				currentStyle = styleAfter(rawLine, i, currentStyle);
				i += formatCodeLength(rawLine, i) - 1;
			} else {
				currentText.append(c);
			}
		}
		if (currentText.length() > 0) {
			width += styledWidth(currentText.toString(), currentStyle);
		}
		return width;
	}

	private int rawPosForX(String rawLine, int pixelX) {
		int rawPos = 0;
		int accumulated = 0;
		Style currentStyle = Style.EMPTY;
		while (rawPos < rawLine.length()) {
			char c = rawLine.charAt(rawPos);
			if (c == '\u00a7' && rawPos + 1 < rawLine.length()) {
				currentStyle = styleAfter(rawLine, rawPos, currentStyle);
				rawPos += formatCodeLength(rawLine, rawPos);
				continue;
			}
			int cw = styledWidth(String.valueOf(c), currentStyle);
			if (accumulated + cw / 2 > pixelX) break;
			accumulated += cw;
			rawPos++;
		}
		return rawPos;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == 0 && classicPaletteOpen && hexColorField.visible && hexColorField.isMouseOver(mouseX, mouseY)) {
			return super.mouseClicked(mouseX, mouseY, button);
		}
		if (button == 0) {
			if (tryToolbarClick(mouseX, mouseY)) return true;
			int maxScroll = Math.max(0, rawLines.size() - getVisibleLines());
			UiTheme.ScrollClick sc = UiTheme.clickBar(scrollBarX(), topPos + TEXT_TOP, TEXT_BOTTOM - TEXT_TOP, scrollLines, maxScroll, mouseX, mouseY);
			if (sc != null) {
				draggingScroll = true;
				scrollGrabOffset = sc.grab;
				scrollLines = sc.scroll;
				setFocused(null);
				return true;
			}
			int maxH = maxHScroll();
			UiTheme.ScrollClick hsc = UiTheme.clickHBar(hBarX(), hBarY(), hBarW(), hScroll, maxH, mouseX, mouseY);
			if (hsc != null) {
				draggingHScroll = true;
				hScrollGrabOffset = hsc.grab;
				hScroll = hsc.scroll;
				setFocused(null);
				return true;
			}
		}
		if (button == 0 || button == 1) {
			if (tryTextClick(mouseX, mouseY, button)) return true;
		}
		boolean handled = super.mouseClicked(mouseX, mouseY, button);
		if (!handled && mouseX >= leftPos && mouseX < leftPos + WIDTH && mouseY >= topPos && mouseY < topPos + HEIGHT) {
			setFocused(null);
		}
		return handled;
	}

	private boolean isWordChar(char c) {
		return Character.isLetterOrDigit(c) || c == '_';
	}

	private int[] wordBounds(int line, int col) {
		String s = rawLines.get(line);
		if (s.isEmpty()) return new int[]{0, 0};
		int start = col;
		while (start > 0 && isWordChar(s.charAt(start - 1))) start--;
		int end = col;
		while (end < s.length() && isWordChar(s.charAt(end))) end++;
		return new int[]{start, end};
	}

	private boolean tryToolbarClick(double mouseX, double mouseY) {
		if (classicPaletteOpen) {
			if (classicPaletteClick(mouseX, mouseY)) return true;
			if (tryGradientClick(mouseX, mouseY)) {
				setFocused(null);
				return true;
			}
			if (mouseX >= classicPaletteX() && mouseX < classicPaletteX() + classicPaletteW()
				&& mouseY >= classicPaletteY() && mouseY < classicPaletteY() + classicPaletteH()) {
				return true;
			}
			if (!hitPalette(mouseX, mouseY)) {
				classicPaletteOpen = false;
				layoutHexInPalette();
				return true;
			}
		}
		if (hitPalette(mouseX, mouseY)) {
			toggleClassicPalette();
			setFocused(null);
			return true;
		}
		if (hitClear(mouseX, mouseY)) {
			setFocused(null);
			resetToPlain();
			return true;
		}
		if (hitClearAll(mouseX, mouseY)) {
			setFocused(null);
			clearAllText();
			return true;
		}
		for (int i = 0; i < MC_COLORS.length; i++) {
			int cx = swatchX(i);
			int cy = swatchY(i);
			if (mouseX >= cx && mouseX < cx + SWATCH_SIZE && mouseY >= cy && mouseY < cy + SWATCH_SIZE) {
				colorIdx = i;
				customHex = null;
				setFocused(null);
				applyFormattingToSelectionOrCursor();
				return true;
			}
		}

		for (int i = 0; i < 5; i++) {
			if (hitStyle(i, mouseX, mouseY)) {
				boolean wasOn = styleOn(i);
				switch (i) {
					case 0 -> tbBold = !tbBold;
					case 1 -> tbItalic = !tbItalic;
					case 2 -> tbUnderlined = !tbUnderlined;
					case 3 -> tbStrikethrough = !tbStrikethrough;
					case 4 -> tbObfuscated = !tbObfuscated;
				}
				setFocused(null);
				applyStyleToggles(wasOn);
				return true;
			}
		}
		return false;
	}

	private boolean tryTextClick(double mouseX, double mouseY, int button) {
		int ty = topPos + TEXT_TOP;

		if (mouseX < leftPos + TEXT_LEFT || mouseX > leftPos + TEXT_RIGHT) return false;
		if (mouseY < topPos + TEXT_TOP || mouseY > topPos + TEXT_BOTTOM) return false;
		setFocused(null);

		int lineIdx = (int)((mouseY - ty) / lineH()) + scrollLines;
		if (lineIdx < 0) lineIdx = 0;
		if (lineIdx >= rawLines.size()) lineIdx = rawLines.size() - 1;

		int px = (int)(mouseX - textDrawX());
		int col = rawPosForX(rawLines.get(lineIdx), px);

		if (button == 0 && isShiftDown()) {
			if (selStartLine < 0) { selStartLine = cursorLine; selStartCol = cursorCol; }
			cursorLine = lineIdx;
			cursorCol = col;
			return true;
		}

		long now = System.currentTimeMillis();
		if (button == 0 && lineIdx == clickLine && Math.abs(col - clickCol) <= 1 && now - lastClickTime < 400) {
			clickCount++;
		} else {
			clickCount = 1;
		}
		lastClickTime = now;
		clickLine = lineIdx;
		clickCol = col;

		cursorLine = lineIdx;
		cursorCol = col;

		if (button == 0) {
			if (clickCount >= 3) {
				selStartLine = lineIdx;
				selStartCol = 0;
				cursorLine = lineIdx;
				cursorCol = rawLines.get(lineIdx).length();
				return true;
			} else if (clickCount == 2) {
				int[] wb = wordBounds(lineIdx, col);
				selStartLine = lineIdx;
				selStartCol = wb[0];
				cursorLine = lineIdx;
				cursorCol = wb[1];
				return true;
			}
			selStartLine = cursorLine;
			selStartCol = cursorCol;
		} else if (button == 1) {
			selStartLine = -1;
			selStartCol = -1;
		}
		return true;
	}

	private boolean isShiftDown() {
		return org.lwjgl.glfw.GLFW.glfwGetKey(Minecraft.getInstance().getWindow().getWindow(), org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT) == 1
			|| org.lwjgl.glfw.GLFW.glfwGetKey(Minecraft.getInstance().getWindow().getWindow(), org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT) == 1;
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
		if (draggingScroll && button == 0) {
			int maxScroll = Math.max(0, rawLines.size() - getVisibleLines());
			scrollLines = UiTheme.scrollAtGrab(topPos + TEXT_TOP, TEXT_BOTTOM - TEXT_TOP, maxScroll, mouseY, scrollGrabOffset);
			return true;
		}
		if (draggingHScroll && button == 0) {
			hScroll = UiTheme.scrollAtGrab(hBarX(), hBarW(), maxHScroll(), mouseX, hScrollGrabOffset);
			return true;
		}
		if (pickingGradient && tryGradientClick(mouseX, mouseY)) return true;
		if (button == 0 && selStartLine >= 0) {
			int ty = topPos + TEXT_TOP;
			if (mouseX < leftPos + TEXT_LEFT || mouseX > leftPos + TEXT_RIGHT) return true;
			if (mouseY < topPos + TEXT_TOP || mouseY > topPos + TEXT_BOTTOM) return true;

			int lineIdx = (int)((mouseY - ty) / lineH()) + scrollLines;
			lineIdx = Mth.clamp(lineIdx, 0, rawLines.size() - 1);
			int px = (int)(mouseX - textDrawX());
			int col = rawPosForX(rawLines.get(lineIdx), px);

			cursorLine = lineIdx;
			cursorCol = col;
			return true;
		}
		return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (gradientDirty) {
			applyHexColor();
		}
		pickingGradient = false;
		gradientDirty = false;
		if (button == 0) {
			draggingScroll = false;
			draggingHScroll = false;
		}
		return super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		double wheelY = scrollY;
		double wheelX = scrollX;
		if (wheelY == 0 && wheelX == 0) return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
		if (classicPaletteOpen) {
			int px = classicPaletteX();
			int py = classicPaletteY();
			if (mouseX >= px && mouseX < px + classicPaletteW()
				&& mouseY >= py && mouseY < py + classicPaletteH()) {
				return true;
			}
		}
		int maxScroll = Math.max(0, rawLines.size() - getVisibleLines());
		int maxH = maxHScroll();
		boolean overH = maxH > 0 && UiTheme.hitHScrollBar(hBarX(), hBarY(), hBarW(), mouseX, mouseY);
		boolean horiz = overH || hasShiftDown() || (Math.abs(wheelX) > Math.abs(wheelY) && wheelX != 0);
		if (horiz && maxH > 0) {
			double wheel = wheelX != 0 ? wheelX : wheelY;
			int delta = (int) (wheel * 24);
			if (delta == 0) delta = (int) Math.signum(wheel) * 12;
			hScroll = Mth.clamp(hScroll - delta, 0, maxH);
			return true;
		}
		int delta = (int) (wheelY * 3);
		if (delta == 0) delta = (int) Math.signum(wheelY != 0 ? wheelY : wheelX);
		scrollLines = Mth.clamp(scrollLines - delta, 0, maxScroll);
		return true;
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (hexFocused()) {
			if (keyCode == 257 || keyCode == 335) {
				applyHexColor();
				setFocused(null);
				return true;
			}
			return super.keyPressed(keyCode, scanCode, modifiers);
		}
		boolean shift = (modifiers & 1) != 0;
		boolean ctrl = (modifiers & 2) != 0;

		if (ctrl) {
			switch (keyCode) {
				case 65 -> { selectAll(); ensureCursorVisible(); return true; }
				case 67 -> { copySelection(); return true; }
				case 86 -> { pasteClipboard(); return true; }
				case 88 -> { copySelection(); deleteSelection(); return true; }
				case 263 -> { moveCursorWordLeft(shift); ensureCursorVisible(); return true; }
				case 262 -> { moveCursorWordRight(shift); ensureCursorVisible(); return true; }
			}
		}

		switch (keyCode) {
			case 263 -> { moveCursorLeft(shift); ensureCursorVisible(); return true; }
			case 262 -> { moveCursorRight(shift); ensureCursorVisible(); return true; }
			case 265 -> { moveCursorUp(shift); ensureCursorVisible(); return true; }
			case 264 -> { moveCursorDown(shift); ensureCursorVisible(); return true; }
			case 268 -> { moveCursorHome(shift); ensureCursorVisible(); return true; }
			case 269 -> { moveCursorEnd(shift); ensureCursorVisible(); return true; }
			case 259 -> { deleteBeforeCursor(); return true; }
			case 261 -> { deleteAfterCursor(); return true; }
			case 257, 335 -> { insertNewline(); return true; }
		}

		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public void insertText(String text, boolean overwrite) {
		insertString(text);
	}

	private void insertString(String text) {
		if (text == null || text.isEmpty()) return;
		deleteSelectionIfAny();
		String normalized = text.replace("\r\n", "\n").replace("\r", "\n");
		for (int i = 0; i < normalized.length(); i++) {
			char c = normalized.charAt(i);
			if (c == '\n') {
				insertNewline();
			} else if (c >= 32 && c != 127) {
				String line = rawLines.get(cursorLine);
				rawLines.set(cursorLine, line.substring(0, cursorCol) + c + line.substring(cursorCol));
				cursorCol++;
			}
		}
		clearSelection();
		fullTextFromLines();
		ensureCursorVisible();
	}

	@Override
	public boolean charTyped(char codePoint, int modifiers) {
		if (hexFocused()) {
			return super.charTyped(codePoint, modifiers);
		}
		if (codePoint == '\u00a7') return false;
		if (codePoint >= 32 && codePoint != 127) {
			deleteSelectionIfAny();
			ensureTypingFormat();
			String line = rawLines.get(cursorLine);
			String newLine = line.substring(0, cursorCol) + codePoint + line.substring(cursorCol);
			rawLines.set(cursorLine, newLine);
			cursorCol++;
			clearSelection();
			fullTextFromLines();
			ensureCursorVisible();
			return true;
		}
		return super.charTyped(codePoint, modifiers);
	}

	private void moveCursorWordLeft(boolean shift) {
		if (!shift) clearSelection();
		if (cursorCol <= 0) {
			if (cursorLine > 0) { cursorLine--; cursorCol = rawLines.get(cursorLine).length(); }
			return;
		}
		String line = rawLines.get(cursorLine);
		int pos = cursorCol - 1;
		boolean wantWord = isWordChar(line.charAt(pos));
		while (pos > 0 && isWordChar(line.charAt(pos - 1)) == wantWord) pos--;
		if (pos == 0 && isWordChar(line.charAt(0)) == wantWord) pos = 0;
		cursorCol = pos;
	}

	private void moveCursorWordRight(boolean shift) {
		if (!shift) clearSelection();
		String line = rawLines.get(cursorLine);
		if (cursorCol >= line.length()) {
			if (cursorLine < rawLines.size() - 1) { cursorLine++; cursorCol = 0; }
			return;
		}
		int pos = cursorCol;
		boolean wantWord = isWordChar(line.charAt(pos));
		while (pos < line.length() && isWordChar(line.charAt(pos)) == wantWord) pos++;
		cursorCol = pos;
	}

	private void moveCursorLeft(boolean shift) {
		if (cursorCol > 0) {
			if (!shift && selStartLine >= 0) {
				int[] s = getOrderedSelection();
				if (isBefore(s[0], s[1], s[2], s[3])) {
					cursorLine = s[0]; cursorCol = s[1];
				}
			} else {
				String line = rawLines.get(cursorLine);
				int start = formatSpanAt(line, cursorCol);
				cursorCol = start >= 0 ? start : cursorCol - 1;
			}
		} else if (cursorLine > 0) {
			cursorLine--;
			cursorCol = rawLines.get(cursorLine).length();
		}
		if (!shift) clearSelection();
	}

	private void moveCursorRight(boolean shift) {
		if (cursorCol < rawLines.get(cursorLine).length()) {
			if (!shift && selStartLine >= 0) {
				int[] s = getOrderedSelection();
				cursorLine = s[2]; cursorCol = s[3];
			} else {
				String line = rawLines.get(cursorLine);
				if (line.charAt(cursorCol) == '\u00a7' && cursorCol + 1 < line.length()) {
					cursorCol += formatCodeLength(line, cursorCol);
				} else {
					cursorCol++;
				}
			}
		} else if (cursorLine < rawLines.size() - 1) {
			cursorLine++;
			cursorCol = 0;
		}
		if (!shift) clearSelection();
	}

	private void moveCursorUp(boolean shift) {
		if (cursorLine > 0) {
			cursorLine--;
			cursorCol = Math.min(cursorCol, rawLines.get(cursorLine).length());
		}
		if (!shift) clearSelection();
	}

	private void moveCursorDown(boolean shift) {
		if (cursorLine < rawLines.size() - 1) {
			cursorLine++;
			cursorCol = Math.min(cursorCol, rawLines.get(cursorLine).length());
		}
		if (!shift) clearSelection();
	}

	private void moveCursorHome(boolean shift) {
		cursorCol = 0;
		if (!shift) clearSelection();
	}

	private void moveCursorEnd(boolean shift) {
		cursorCol = rawLines.get(cursorLine).length();
		if (!shift) clearSelection();
	}

	private int formatSpanAt(String line, int col) {
		int i = 0;
		while (i < line.length()) {
			if (line.charAt(i) == '\u00a7' && i + 1 < line.length()) {
				int len = formatCodeLength(line, i);
				int end = i + len;
				if (col > i && col <= end) return i;
				i = end;
			} else {
				i++;
			}
		}
		return -1;
	}

	private void deleteBeforeCursor() {
		if (hasSelection()) {
			deleteSelection();
			return;
		}
		if (cursorCol > 0) {
			String line = rawLines.get(cursorLine);
			int start = formatSpanAt(line, cursorCol);
			int end = cursorCol;
			if (start >= 0) {
				end = Math.min(line.length(), Math.max(cursorCol, start + formatCodeLength(line, start)));
			} else {
				start = cursorCol - 1;
			}
			if (start < 0) start = 0;
			rawLines.set(cursorLine, line.substring(0, start) + line.substring(end));
			cursorCol = start;
		} else if (cursorLine > 0) {
			String prevLine = rawLines.get(cursorLine - 1);
			String curLine = rawLines.get(cursorLine);
			cursorCol = prevLine.length();
			rawLines.set(cursorLine - 1, prevLine + curLine);
			rawLines.remove(cursorLine);
			cursorLine--;
		}
		clearSelection();
		fullTextFromLines();
		ensureCursorVisible();
	}

	private void deleteAfterCursor() {
		if (hasSelection()) {
			deleteSelection();
			return;
		}
		String line = rawLines.get(cursorLine);
		if (cursorCol < line.length()) {
			int start = cursorCol;
			int end = cursorCol + 1;
			if (line.charAt(cursorCol) == '\u00a7' && cursorCol + 1 < line.length()) {
				end = cursorCol + formatCodeLength(line, cursorCol);
			}
			rawLines.set(cursorLine, line.substring(0, start) + line.substring(end));
			cursorCol = start;
		} else if (cursorLine < rawLines.size() - 1) {
			String curLine = rawLines.get(cursorLine);
			String nextLine = rawLines.get(cursorLine + 1);
			rawLines.set(cursorLine, curLine + nextLine);
			rawLines.remove(cursorLine + 1);
		}
		clearSelection();
		fullTextFromLines();
		ensureCursorVisible();
	}

	private void insertNewline() {
		deleteSelectionIfAny();
		String line = rawLines.get(cursorLine);
		String after = line.substring(cursorCol);
		rawLines.set(cursorLine, line.substring(0, cursorCol));
		rawLines.add(cursorLine + 1, after);
		cursorLine++;
		cursorCol = 0;
		clearSelection();
		fullTextFromLines();
		ensureCursorVisible();
	}

	private void clearAllText() {
		rawLines.clear();
		rawLines.add("");
		fullText = "";
		cursorLine = 0;
		cursorCol = 0;
		scrollLines = 0;
		hScroll = 0;
		clearSelection();
	}

	private void resetToPlain() {
		tbBold = false;
		tbItalic = false;
		tbUnderlined = false;
		tbStrikethrough = false;
		tbObfuscated = false;
		colorIdx = 15;
		customHex = null;
		if (hexColorField != null) hexColorField.setValue("");
		if (hasSelection()) {
			clearFormatting();
			return;
		}
		String line = rawLines.get(cursorLine);
		rawLines.set(cursorLine, line.substring(0, cursorCol) + "\u00a7r" + line.substring(cursorCol));
		cursorCol += 2;
		fullTextFromLines();
	}

	private void clearFormatting() {
		if (!hasSelection()) return;
		int[] s = getOrderedSelection();
		StringBuilder clean = new StringBuilder();
		for (int i = s[0]; i <= s[2]; i++) {
			String line = rawLines.get(i);
			if (i > s[0]) clean.append("\n");
			int start = (i == s[0]) ? s[1] : 0;
			int end = (i == s[2]) ? s[3] : line.length();
			String segment = line.substring(start, end);
			StringBuilder stripped = new StringBuilder();
			for (int k = 0; k < segment.length(); k++) {
				char c = segment.charAt(k);
				if (c == '\u00a7' && k + 1 < segment.length()) {
					k += formatCodeLength(segment, k) - 1;
				} else {
					stripped.append(c);
				}
			}
			clean.append(stripped);
		}
		String cleaned = clean.toString();
		String[] cleanedLines = cleaned.split("\n", -1);
		List<String> newLines = new ArrayList<>();
		newLines.add(rawLines.get(s[0]).substring(0, s[1]) + cleanedLines[0]);
		for (int i = 1; i < cleanedLines.length - 1; i++) {
			newLines.add(cleanedLines[i]);
		}
		if (cleanedLines.length > 1) {
			newLines.add(cleanedLines[cleanedLines.length - 1] + rawLines.get(s[2]).substring(s[3]));
		} else {
			newLines.set(0, newLines.get(0) + rawLines.get(s[2]).substring(s[3]));
		}
		rawLines.subList(s[0], s[2] + 1).clear();
		rawLines.addAll(s[0], newLines);
		clearSelection();
		fullTextFromLines();
	}

	private void selectAll() {
		selStartLine = 0;
		selStartCol = 0;
		cursorLine = rawLines.size() - 1;
		cursorCol = rawLines.get(cursorLine).length();
	}

	private void copySelection() {
		if (!hasSelection()) return;
		int[] s = getOrderedSelection();
		StringBuilder sel = new StringBuilder();
		for (int i = s[0]; i <= s[2]; i++) {
			String line = rawLines.get(i);
			int startC = (i == s[0]) ? s[1] : 0;
			int endC = (i == s[2]) ? s[3] : line.length();
			if (i > s[0]) sel.append("\n");
			sel.append(line, startC, endC);
		}
		Minecraft.getInstance().keyboardHandler.setClipboard(sel.toString());
	}

	private void pasteClipboard() {
		insertString(Minecraft.getInstance().keyboardHandler.getClipboard());
	}

	private static final int PALETTE_CELL = 12;
	private static final int PALETTE_COLS = 8;
	private static final int[] CLASSIC_COLORS = {
		0xFF000000, 0xFF1C1C1C, 0xFF383838, 0xFF545454, 0xFF707070, 0xFF8C8C8C, 0xFFA8A8A8, 0xFFC4C4C4,
		0xFF7F0000, 0xFF990000, 0xFFB30000, 0xFFCC0000, 0xFFE60000, 0xFFFF0000, 0xFFFF3333, 0xFFFF6666,
		0xFF7F3300, 0xFF994C00, 0xFFB36600, 0xFFCC7F00, 0xFFE69900, 0xFFFFAA00, 0xFFFFBB33, 0xFFFFCC66,
		0xFF7F7F00, 0xFF999900, 0xFFB3B300, 0xFFCCCC00, 0xFFE6E600, 0xFFFFFF00, 0xFFFFFF33, 0xFFFFFF66,
		0xFF007F00, 0xFF009900, 0xFF00B300, 0xFF00CC00, 0xFF00E600, 0xFF00FF00, 0xFF33FF33, 0xFF66FF66,
		0xFF007F7F, 0xFF009999, 0xFF00B3B3, 0xFF00CCCC, 0xFF00E6E6, 0xFF00FFFF, 0xFF33FFFF, 0xFF66FFFF,
		0xFF00007F, 0xFF000099, 0xFF0000B3, 0xFF0000CC, 0xFF0000E6, 0xFF0000FF, 0xFF3333FF, 0xFF6666FF,
		0xFF7F007F, 0xFF990099, 0xFFB300B3, 0xFFCC00CC, 0xFFE600E6, 0xFFFF00FF, 0xFFFF33FF, 0xFFFF66FF
	};

	private void toggleClassicPalette() {
		classicPaletteOpen = !classicPaletteOpen;
		layoutHexInPalette();
	}

	private int classicPaletteW() {
		return 6 + classicGridW() + 8 + pickerSize() + 10 + 6;
	}

	private int classicPaletteH() {
		return 6 + classicGridH() + 6 + 22;
	}

	private int classicPaletteX() {
		int w = classicPaletteW();
		int x = paletteTextX + paletteTextW - w;
		if (x < leftPos + 6) x = leftPos + 6;
		if (x + w > leftPos + WIDTH - 6) x = leftPos + WIDTH - 6 - w;
		return x;
	}

	private int classicPaletteY() {
		int h = classicPaletteH();
		int y = toolY - h - 6;
		int minY = topPos + UiTheme.HEADER_H + 2;
		if (y < minY) y = minY;
		return y;
	}

	private void renderClassicPalette(GuiGraphics g) {
		int px = classicPaletteX();
		int py = classicPaletteY();
		int pw = classicPaletteW();
		int ph = classicPaletteH();
		g.fill(px, py, px + pw, py + ph, 0xF0121314);
		g.renderOutline(px, py, pw, ph, 0xFFE6EDF3);
		int gx = px + 6;
		int gy = py + 6;
		for (int i = 0; i < CLASSIC_COLORS.length; i++) {
			int cx = gx + 2 + (i % PALETTE_COLS) * (PALETTE_CELL + 2);
			int cy = gy + 2 + (i / PALETTE_COLS) * (PALETTE_CELL + 2);
			drawSwatch(g, cx, cy, PALETTE_CELL, CLASSIC_COLORS[i], 0xFF3C3C3C);
		}
		renderGradientPicker(g, pickerX(), pickerY());
	}

	private boolean classicPaletteClick(double mouseX, double mouseY) {
		int gx = classicPaletteX() + 6;
		int gy = classicPaletteY() + 6;
		int gridW = classicGridW();
		int gridH = classicGridH();
		int rows = CLASSIC_COLORS.length / PALETTE_COLS;
		if (mouseX < gx || mouseX >= gx + gridW || mouseY < gy || mouseY >= gy + gridH) return false;
		int col = (int)((mouseX - gx - 2) / (PALETTE_CELL + 2));
		int row = (int)((mouseY - gy - 2) / (PALETTE_CELL + 2));
		if (col < 0 || col >= PALETTE_COLS || row < 0 || row >= rows) return false;
		int idx = row * PALETTE_COLS + col;
		int color = CLASSIC_COLORS[idx] & 0x00FFFFFF;
		hexColorField.setValue(String.format("%06x", color));
		classicPaletteOpen = false;
		layoutHexInPalette();
		applyHexColor();
		return true;
	}

	private void applyHexColor() {
		String hex = hexColorField.getValue().trim();
		if (hex.isEmpty()) return;
		try {
			if (hex.startsWith("#")) hex = hex.substring(1);
			if (hex.startsWith("0x") || hex.startsWith("0X")) hex = hex.substring(2);
			if (hex.length() != 6) return;
			int color = Integer.parseInt(hex, 16);
			String hexStr = String.format("%06x", color & 0xFFFFFF);
			customHex = hexStr;
			StringBuilder code = new StringBuilder("\u00a7x");
			for (char c : hexStr.toCharArray()) {
				code.append('\u00a7').append(c);
			}
			String codeStr = code.toString();
			if (hasSelection()) {
				int[] s = getOrderedSelection();
				StringBuilder fmtCode = new StringBuilder(codeStr);
				if (tbBold) fmtCode.append("\u00a7l");
				if (tbItalic) fmtCode.append("\u00a7o");
				if (tbUnderlined) fmtCode.append("\u00a7n");
				if (tbStrikethrough) fmtCode.append("\u00a7m");
				if (tbObfuscated) fmtCode.append("\u00a7k");
				if (s[0] == s[2]) {
					String line = rawLines.get(s[0]);
					rawLines.set(s[0], line.substring(0, s[1]) + fmtCode + line.substring(s[1], s[3]) + "\u00a7r" + line.substring(s[3]));
					cursorCol = s[3] + fmtCode.length() + 2;
					selStartCol = s[1] + fmtCode.length();
				} else {
					rawLines.set(s[0], rawLines.get(s[0]).substring(0, s[1]) + fmtCode + rawLines.get(s[0]).substring(s[1]));
					cursorCol = s[3];
					selStartCol = s[1] + fmtCode.length();
				}
				selStartLine = s[0];
			} else {
				StringBuilder fmtCode = new StringBuilder(codeStr);
				if (tbBold) fmtCode.append("\u00a7l");
				if (tbItalic) fmtCode.append("\u00a7o");
				if (tbUnderlined) fmtCode.append("\u00a7n");
				if (tbStrikethrough) fmtCode.append("\u00a7m");
				if (tbObfuscated) fmtCode.append("\u00a7k");
				String line = rawLines.get(cursorLine);
				String newLine = line.substring(0, cursorCol) + fmtCode + line.substring(cursorCol);
				rawLines.set(cursorLine, newLine);
				cursorCol += fmtCode.length();
				clearSelection();
			}
			fullTextFromLines();
		} catch (NumberFormatException ignored) {}
	}

	private boolean styleOn(int i) {
		return switch (i) {
			case 0 -> tbBold;
			case 1 -> tbItalic;
			case 2 -> tbUnderlined;
			case 3 -> tbStrikethrough;
			case 4 -> tbObfuscated;
			default -> false;
		};
	}

	private boolean anyStyle() {
		return tbBold || tbItalic || tbUnderlined || tbStrikethrough || tbObfuscated;
	}

	private String currentStyleCodes() {
		StringBuilder sb = new StringBuilder();
		if (tbBold) sb.append("\u00a7l");
		if (tbItalic) sb.append("\u00a7o");
		if (tbUnderlined) sb.append("\u00a7n");
		if (tbStrikethrough) sb.append("\u00a7m");
		if (tbObfuscated) sb.append("\u00a7k");
		return sb.toString();
	}

	private String currentColorCode() {
		if (customHex != null && customHex.length() == 6) {
			StringBuilder code = new StringBuilder("\u00a7x");
			for (int i = 0; i < 6; i++) {
				code.append('\u00a7').append(customHex.charAt(i));
			}
			return code.toString();
		}
		return "\u00a7" + MC_COLOR_CODES[Mth.clamp(colorIdx, 0, MC_COLOR_CODES.length - 1)];
	}

	private String currentFormatCodes() {
		return currentColorCode() + currentStyleCodes();
	}

	private int toolbarRgb() {
		if (customHex != null && customHex.length() == 6) {
			try {
				return Integer.parseInt(customHex, 16) & 0xFFFFFF;
			} catch (NumberFormatException ignored) {}
		}
		return MC_COLORS[Mth.clamp(colorIdx, 0, MC_COLORS.length - 1)] & 0xFFFFFF;
	}

	private void applyStyleToggles(boolean turnedOff) {
		StringBuilder codes = new StringBuilder();
		if (turnedOff || !anyStyle()) codes.append("\u00a7r");
		codes.append(currentStyleCodes());
		insertOrWrapFormat(codes.toString());
	}

	private void ensureTypingFormat() {
		Style at = styleAt(rawLines.get(cursorLine), cursorCol);
		if (formatMatchesToolbar(at)) return;
		String codes = currentFormatCodes();
		String line = rawLines.get(cursorLine);
		rawLines.set(cursorLine, line.substring(0, cursorCol) + codes + line.substring(cursorCol));
		cursorCol += codes.length();
	}

	private Style styleAt(String line, int rawPos) {
		Style s = Style.EMPTY;
		int i = 0;
		int limit = Math.min(rawPos, line.length());
		while (i < limit) {
			if (line.charAt(i) == '\u00a7' && i + 1 < line.length()) {
				s = styleAfter(line, i, s);
				i += formatCodeLength(line, i);
			} else {
				i++;
			}
		}
		return s;
	}

	private boolean formatMatchesToolbar(Style s) {
		if (s.getColor() == null || (s.getColor().getValue() & 0xFFFFFF) != toolbarRgb()) return false;
		return flagOn(s.isBold()) == tbBold
			&& flagOn(s.isItalic()) == tbItalic
			&& flagOn(s.isUnderlined()) == tbUnderlined
			&& flagOn(s.isStrikethrough()) == tbStrikethrough
			&& flagOn(s.isObfuscated()) == tbObfuscated;
	}

	private static boolean flagOn(Boolean v) {
		return v != null && v;
	}

	private void applyFormattingToSelectionOrCursor() {
		StringBuilder fmtCode = new StringBuilder();
		fmtCode.append('\u00a7').append(MC_COLOR_CODES[colorIdx]);
		fmtCode.append(currentStyleCodes());
		insertOrWrapFormat(fmtCode.toString());
	}

	private void insertOrWrapFormat(String fmtCode) {
		if (fmtCode.isEmpty()) return;
		if (hasSelection()) {
			int[] s = getOrderedSelection();
			if (s[0] == s[2]) {
				String line = rawLines.get(s[0]);
				rawLines.set(s[0], line.substring(0, s[1]) + fmtCode + line.substring(s[1], s[3]) + "\u00a7r" + line.substring(s[3]));
				cursorCol = s[3] + fmtCode.length() + 2;
				selStartCol = s[1] + fmtCode.length();
			} else {
				rawLines.set(s[0], rawLines.get(s[0]).substring(0, s[1]) + fmtCode + rawLines.get(s[0]).substring(s[1]));
				cursorCol = s[3];
				selStartCol = s[1] + fmtCode.length();
			}
			selStartLine = s[0];
		} else {
			String line = rawLines.get(cursorLine);
			rawLines.set(cursorLine, line.substring(0, cursorCol) + fmtCode + line.substring(cursorCol));
			cursorCol += fmtCode.length();
			clearSelection();
		}
		fullTextFromLines();
	}

	private boolean hasSelection() {
		return selStartLine >= 0 && (selStartLine != cursorLine || selStartCol != cursorCol);
	}

	private void ensureCursorVisible() {
		int visible = Math.max(1, getVisibleLines());
		int maxScroll = Math.max(0, rawLines.size() - visible);
		if (cursorLine < scrollLines) {
			scrollLines = cursorLine;
		} else if (cursorLine >= scrollLines + visible) {
			scrollLines = cursorLine - visible + 1;
		}
		scrollLines = Mth.clamp(scrollLines, 0, maxScroll);
		if (cursorLine >= 0 && cursorLine < rawLines.size()) {
			int cx = visibleWidthUpTo(rawLines.get(cursorLine), cursorCol);
			int viewW = textViewW();
			int pad = 8;
			if (cx < hScroll + pad) {
				hScroll = Math.max(0, cx - pad);
			} else if (cx > hScroll + viewW - pad) {
				hScroll = cx - viewW + pad;
			}
		}
		clampHScroll();
	}
	private void clearSelection() {
		selStartLine = -1;
		selStartCol = -1;
	}

	private void deleteSelectionIfAny() {
		if (hasSelection()) {
			deleteSelection();
		}
	}

	private void deleteSelection() {
		if (!hasSelection()) return;
		int[] s = getOrderedSelection();
		if (s[0] == s[2]) {
			String line = rawLines.get(s[0]);
			rawLines.set(s[0], line.substring(0, s[1]) + line.substring(s[3]));
			cursorLine = s[0];
			cursorCol = s[1];
		} else {
			String firstLine = rawLines.get(s[0]).substring(0, s[1]);
			String lastLine = rawLines.get(s[2]).substring(s[3]);
			rawLines.set(s[0], firstLine + lastLine);
			for (int i = s[2]; i > s[0]; i--) {
				rawLines.remove(i);
			}
			cursorLine = s[0];
			cursorCol = s[1];
		}
		clearSelection();
		fullTextFromLines();
		ensureCursorVisible();
	}

	private int[] getOrderedSelection() {
		if (selStartLine < 0) return new int[] {cursorLine, cursorCol, cursorLine, cursorCol};
		if (isBefore(selStartLine, selStartCol, cursorLine, cursorCol)) {
			return new int[] {selStartLine, selStartCol, cursorLine, cursorCol};
		} else {
			return new int[] {cursorLine, cursorCol, selStartLine, selStartCol};
		}
	}

	@Override
	public void tick() {
		super.tick();
		tickCount++;
	}

	@Override
	public void onClose() {
		if (callback != null) callback.accept(null);
		Minecraft.getInstance().setScreen(parent);
	}

	@Override
	public void removed() {
		releasePickerTex();
		super.removed();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
