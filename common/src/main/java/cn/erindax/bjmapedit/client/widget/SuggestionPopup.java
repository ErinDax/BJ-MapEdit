package cn.erindax.bjmapedit.client.widget;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.util.Mth;

import java.util.function.IntFunction;

public final class SuggestionPopup {
	public static final int ROW = 14;
	public static final int MAX_VIS = 8;
	public static final int BAR_W = 3;
	private static final int PAD = 2;
	private static final int WHEEL_STEP = 3;

	private static boolean dragging;
	private static double grab;
	private static int dragBarY;
	private static int dragBarH;
	private static int dragMaxPx;
	private static int dragTotal;

	private SuggestionPopup() {}

	public static int vis(int total) {
		return Math.min(MAX_VIS, Math.max(0, total));
	}

	public static int boxH(int total) {
		return vis(total) * ROW + 2;
	}

	public static int maxScroll(int total) {
		return Math.max(0, total - vis(total));
	}

	public static int listY(int fieldY, int fieldH, int total, int bottom) {
		int h = boxH(total);
		int below = fieldY + fieldH;
		if (below + h <= bottom) return below;
		return fieldY - h;
	}

	public static int listY(AbstractWidget field, int total, int bottom) {
		return listY(field.getY(), field.getHeight(), total, bottom);
	}

	public static boolean clickOpens(AbstractWidget field, double mx, double my) {
		return field != null && field.isMouseOver(mx, my);
	}

	public static int clampScroll(int scroll, int total) {
		return Mth.clamp(scroll, 0, maxScroll(total));
	}

	public static int keepVisible(int selected, int scroll, int total) {
		int vis = vis(total);
		if (vis <= 0) return 0;
		if (selected < scroll) return selected;
		if (selected >= scroll + vis) return selected - vis + 1;
		return scroll;
	}

	public static boolean contains(double mx, double my, int sx, int sy, int w, int total) {
		int h = boxH(total);
		return mx >= sx && mx < sx + w && my >= sy && my < sy + h;
	}

	public static int barX(int sx, int w) {
		return sx + w - BAR_W - PAD;
	}

	public static int barY(int sy) {
		return sy + PAD;
	}

	public static int barH(int total) {
		return Math.max(0, boxH(total) - PAD * 2);
	}

	public static boolean overBar(double mx, double my, int sx, int sy, int w, int total) {
		if (maxScroll(total) <= 0) return false;
		return UiTheme.hitScrollBar(barX(sx, w), barY(sy), barH(total), mx, my);
	}

	public static int hitIndex(double mx, double my, int sx, int sy, int w, int total, int scroll) {
		if (!contains(mx, my, sx, sy, w, total) || overBar(mx, my, sx, sy, w, total)) return -1;
		int idx = (int) ((my - sy - 1) / ROW);
		int vis = vis(total);
		if (idx < 0 || idx >= vis) return -1;
		int real = idx + clampScroll(scroll, total);
		return real < total ? real : -1;
	}

	public static int scrollBy(int scroll, int total, double wheel) {
		if (wheel == 0 || maxScroll(total) <= 0) return clampScroll(scroll, total);
		int page = vis(total);
		int base = total > page * 3 ? page : WHEEL_STEP;
		int step = Math.max(base, (int) Math.round(Math.abs(wheel) * base));
		return clampScroll(scroll - (int) Math.signum(wheel) * step, total);
	}

	public static Integer pressBar(int sx, int sy, int w, int total, int scroll, double mx, double my) {
		if (maxScroll(total) <= 0) return null;
		UiTheme.ScrollClick sc = UiTheme.clickBar(
			barX(sx, w), barY(sy), barH(total), toPixel(scroll, total), pixelMax(total), mx, my);
		if (sc == null) return null;
		dragging = true;
		grab = sc.grab;
		dragBarY = barY(sy);
		dragBarH = barH(total);
		dragMaxPx = pixelMax(total);
		dragTotal = total;
		return fromPixel(sc.scroll, total);
	}

	public static boolean isDragging() {
		return dragging;
	}

	public static int dragTo(double my) {
		if (!dragging) return 0;
		return fromPixel(UiTheme.scrollAtGrab(dragBarY, dragBarH, dragMaxPx, my, grab), dragTotal);
	}

	public static void endDrag() {
		dragging = false;
	}

	public static void draw(
		GuiGraphics g, Font font,
		int sx, int sy, int w,
		int total, int scroll, int selected,
		int mouseX, int mouseY,
		IntFunction<String> labelAt
	) {
		if (total <= 0) return;
		scroll = clampScroll(scroll, total);
		int vis = vis(total);
		int h = vis * ROW + 2;
		int textW = w - (total > vis ? 8 : 4);
		g.fill(sx, sy, sx + w, sy + h, UiTheme.cListBorder());
		g.fill(sx + 1, sy + 1, sx + w - 1, sy + h - 1, UiTheme.cListBg());
		g.renderOutline(sx, sy, w, h, UiTheme.cListBorder());
		boolean barHot = dragging || overBar(mouseX, mouseY, sx, sy, w, total);
		g.enableScissor(sx + 1, sy + 1, sx + w - 1, sy + h - 1);
		for (int i = 0; i < vis; i++) {
			int real = i + scroll;
			int lineY = sy + 1 + i * ROW;
			boolean mouseRow = !barHot && mouseX >= sx && mouseX < sx + w && mouseY >= lineY && mouseY < lineY + ROW;
			if (mouseRow) {
				g.fill(sx + 1, lineY, sx + w - 1, lineY + ROW, UiTheme.cListHover());
			} else if (real == selected) {
				g.fill(sx + 1, lineY, sx + w - 1, lineY + ROW, 0xFF21262D);
			}
			String text = labelAt.apply(real);
			if (text == null) text = "";
			if (font.width(text) > textW) text = font.plainSubstrByWidth(text, Math.max(8, textW - 6)) + "...";
			g.drawString(font, text, sx + 3, lineY + 3, mouseRow || real == selected ? 0xFFFFFFFF : UiTheme.cListText(), false);
		}
		g.disableScissor();
		if (total > vis) {
			UiTheme.drawThinScrollBar(g, barX(sx, w), barY(sy), barH(total), toPixel(scroll, total), pixelMax(total), barHot);
		}
	}

	private static int pixelMax(int total) {
		return maxScroll(total) * ROW;
	}

	private static int toPixel(int scroll, int total) {
		return clampScroll(scroll, total) * ROW;
	}

	private static int fromPixel(int pixel, int total) {
		if (pixel <= 0) return 0;
		return clampScroll((int) Math.round(pixel / (double) ROW), total);
	}
}
