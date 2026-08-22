package cn.erindax.bjmapedit.client.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;

public final class UiTheme {
	private UiTheme() {}

	public static final int PANEL_BG = 0xF21E1E1E;
	public static final int PANEL_BORDER = 0xFF3C3C3C;
	public static final int HEADER_BG = 0xFF323233;
	public static final int INPUT_BG = 0xFF3C3C3C;
	public static final int INPUT_BORDER = 0xFF3C3C3C;
	public static final int INPUT_TEXT = 0xFFCCCCCC;
	public static final int LABEL = 0xFFCCCCCC;
	public static final int MUTED = 0xFF858585;
	public static final int ACCENT = 0xFF007ACC;
	public static final int SLOT_BG = 0xFF8B8B8B;
	public static final int SLOT_INSET = 0xFF373737;
	public static final int PREVIEW_WELL = 0xFF252526;
	public static final int BUTTON_BG = 0xFF0E639C;
	public static final int BUTTON_HOVER = 0xFF1177BB;
	public static final int BUTTON_DISABLED = 0xFF3A3D41;
	public static final int BUTTON_TEXT = 0xFFFFFFFF;
	public static final int BUTTON_TEXT_DISABLED = 0xFF888888;
	public static final int LIST_BG = 0xFF252526;
	public static final int LIST_HOVER = 0xFF094771;
	public static final int LIST_TEXT = 0xFFCCCCCC;
	public static final int LIST_BORDER = 0xFF454545;
	public static final int HEADER_H = 22;
	public static final int PAD = 10;
	public static final int ICON = 16;
	private static final ResourceLocation REFRESH =
		ResourceLocation.fromNamespaceAndPath("bj_mapedit", "textures/gui/refresh.png");

	public static boolean itemEditor() {
		var s = net.minecraft.client.Minecraft.getInstance().screen;
		if (s == null) return false;
		String n = s.getClass().getSimpleName();
		return n.equals("ItemEditorScreen") || n.equals("TextEditorScreen")
			|| n.equals("BookEditorScreen") || n.equals("GameRuleEditorScreen")
			|| n.equals("FrameStandEditorScreen") || n.equals("EntityEditorScreen")
			|| n.equals("VillagerEditorScreen") || n.equals("DatapackEditorScreen")
			|| n.equals("RecipeEditorScreen") || n.equals("AdvancementEditorScreen");
	}

	public static int cPanelBg() { return itemEditor() ? 0xF2121314 : PANEL_BG; }
	public static int cPanelBorder() { return itemEditor() ? 0xFF2B2C2D : PANEL_BORDER; }
	public static int cHeaderBg() { return itemEditor() ? 0xFF191A1B : HEADER_BG; }
	public static int cInputBg() { return itemEditor() ? 0xFF0E0F10 : INPUT_BG; }
	public static int cInputBorder() { return itemEditor() ? 0xFF2B2C2D : INPUT_BORDER; }
	public static int cInputText() { return itemEditor() ? 0xFFE6EDF3 : INPUT_TEXT; }
	public static int cLabel() { return itemEditor() ? 0xFFD0D7DE : LABEL; }
	public static int cMuted() { return itemEditor() ? 0xFF8B949E : MUTED; }
	public static int cAccent() { return itemEditor() ? 0xFF002FA7 : ACCENT; }
	public static int cPreviewWell() { return itemEditor() ? 0xFF191A1B : PREVIEW_WELL; }
	public static int cButtonBg() { return itemEditor() ? 0xFF21262D : BUTTON_BG; }
	public static int cButtonHover() { return itemEditor() ? 0xFF30363D : BUTTON_HOVER; }
	public static int cButtonDisabled() { return itemEditor() ? 0xFF21262D : BUTTON_DISABLED; }
	public static int cButtonText() { return itemEditor() ? 0xFFE6EDF3 : BUTTON_TEXT; }
	public static int cButtonTextDisabled() { return itemEditor() ? 0xFF6E7681 : BUTTON_TEXT_DISABLED; }
	public static int cListBg() { return itemEditor() ? 0xFF191A1B : LIST_BG; }
	public static int cListHover() { return itemEditor() ? 0xFF001A5C : LIST_HOVER; }
	public static int cListText() { return itemEditor() ? 0xFFE6EDF3 : LIST_TEXT; }
	public static int cListBorder() { return itemEditor() ? 0xFF2B2C2D : LIST_BORDER; }
	public static int cHeaderTitle() { return itemEditor() ? 0xFFE6EDF3 : 0xFFFFFFFF; }
	public static int cButtonEdge() { return itemEditor() ? 0x22FFFFFF : 0x33000000; }
	public static int cScrollTrack() { return itemEditor() ? 0xFF1C1D1E : 0x40000000; }
	public static int cScrollThumb() { return itemEditor() ? 0xFFFFFFFF : ACCENT; }
	public static int cFocus() { return itemEditor() ? 0xFF0E639C : ACCENT; }
	public static int cInputWellBorder(boolean focused) { return focused ? cFocus() : (itemEditor() ? cInputBorder() : 0xFF474747); }

	public static void drawPanel(GuiGraphics g, int x, int y, int w, int h) {
		g.fill(x, y, x + w, y + h, cPanelBg());
		hLine(g, x, y, w, cPanelBorder());
		hLine(g, x, y + h - 1, w, cPanelBorder());
		vLine(g, x, y, h, cPanelBorder());
		vLine(g, x + w - 1, y, h, cPanelBorder());
	}

	public static void drawHeader(GuiGraphics g, Font font, Component title, int x, int y, int w) {
		g.fill(x + 1, y + 1, x + w - 1, y + HEADER_H, cHeaderBg());
		if (!itemEditor()) g.fill(x + 1, y + HEADER_H - 1, x + w - 1, y + HEADER_H, cAccent());
		g.drawCenteredString(font, title, x + w / 2, y + 7, cHeaderTitle());
	}

	public static int headerIconX(int panelX, int panelW) {
		return panelX + panelW - 5 - ICON;
	}

	public static int headerIconY(int panelY) {
		return panelY + (HEADER_H - ICON) / 2;
	}

	public static boolean hitIcon(int x, int y, double mx, double my) {
		return mx >= x && mx < x + ICON && my >= y && my < y + ICON;
	}

	public static void playClick() {
		Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
	}

	public static boolean inFooterBar(double mx, double my, int left, int top, int width, int height, int footerH) {
		return mx >= left && mx < left + width && my >= top + height - footerH && my < top + height;
	}

	public static boolean clickWidgets(double mx, double my, int button, AbstractWidget... widgets) {
		for (AbstractWidget w : widgets) {
			if (w != null && w.visible && w.mouseClicked(mx, my, button)) return true;
		}
		return false;
	}

	public static boolean consumeRefreshClick(int x, int y, double mx, double my, Runnable onReset) {
		if (!hitIcon(x, y, mx, my)) return false;
		playClick();
		onReset.run();
		return true;
	}

	public static void drawRefreshIcon(GuiGraphics g, int x, int y, boolean hovered) {
		if (hovered) fillDisc(g, x, y, ICON, 0x3DFFFFFF);
		float t = hovered ? 1.0F : 0.68F;
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.setShaderColor(t, t, t, 1.0F);
		g.pose().pushPose();
		g.pose().translate(x, y, 0);
		g.pose().scale(0.5F, 0.5F, 1.0F);
		g.blit(REFRESH, 0, 0, 0.0F, 0.0F, 32, 32, 32, 32);
		g.pose().popPose();
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
	}

	private static void fillDisc(GuiGraphics g, int x, int y, int size, int color) {
		float mid = (size - 1) * 0.5F;
		float r2 = mid * mid;
		for (int iy = 0; iy < size; iy++) {
			for (int ix = 0; ix < size; ix++) {
				float dx = ix - mid;
				float dy = iy - mid;
				if (dx * dx + dy * dy <= r2) {
					g.fill(x + ix, y + iy, x + ix + 1, y + iy + 1, color);
				}
			}
		}
	}

	public static void drawSlot(GuiGraphics g, int x, int y) {
		drawSlot(g, x, y, 18);
	}

	public static void drawSlot(GuiGraphics g, int x, int y, int size) {
		g.fill(x, y, x + size, y + size, SLOT_BG);
		g.fill(x, y, x + size - 1, y + size - 1, SLOT_INSET);
		g.fill(x + 1, y + 1, x + size - 1, y + size - 1, SLOT_BG);
		g.fill(x + 1, y + 1, x + size - 2, y + size - 2, SLOT_INSET);
	}

	public static void drawSlotBox(GuiGraphics g, int x, int y, int size) {
		g.fill(x, y, x + size, y + size, SLOT_BG);
		g.fill(x + 1, y + 1, x + size - 1, y + size - 1, SLOT_INSET);
	}

	public static void drawPreviewFrame(GuiGraphics g, int x, int y, int w, int h) {
		g.fill(x, y, x + w, y + h, cPreviewWell());
		hLine(g, x, y, w, cPanelBorder());
		hLine(g, x, y + h - 1, w, cPanelBorder());
		vLine(g, x, y, h, cPanelBorder());
		vLine(g, x + w - 1, y, h, cPanelBorder());
	}

	public static void label(GuiGraphics g, Font font, String cn, int x, int y) {
		g.drawString(font, cn, x, y, cLabel(), false);
	}

	public static void label(GuiGraphics g, Font font, Component cn, int x, int y) {
		g.drawString(font, cn, x, y, cLabel(), false);
	}

	public static void muted(GuiGraphics g, Font font, String text, int x, int y) {
		g.drawString(font, text, x, y, cMuted(), false);
	}

	public static final int FILE_ICON = 8;

	public static int templateRowIconX(int railX) {
		return railX + 6;
	}

	public static int templateRowNameX(int railX) {
		return railX + 6 + FILE_ICON + 3;
	}

	public static void drawTemplateFileIcon(GuiGraphics g, int railX, int rowY, boolean hot) {
		drawTemplateFileIcon(g, railX, rowY, 0, hot);
	}

	public static void drawTemplateFileIcon(GuiGraphics g, int railX, int rowY, int depth, boolean hot) {
		drawFileTypeIcon(g, TemplateOrg.rowIconX(railX, depth), rowY + 7, "json", hot);
	}

	public static void drawFolderIcon(GuiGraphics g, int x, int y, boolean open, boolean hot) {
		int tab = hot ? 0xFFE8C547 : 0xFFC9A227;
		int body = open ? (hot ? 0xFFF0D060 : 0xFFD4A017) : (hot ? 0xFFD4A017 : 0xFFB8860B);
		g.fill(x + 1, y, x + 4, y + 2, tab);
		g.fill(x, y + 2, x + 8, y + 7, body);
		if (open) {
			g.fill(x + 1, y + 3, x + 7, y + 6, hot ? 0xFFF6E08A : 0xFFE0B84A);
		}
	}

	public static void drawFileTypeIcon(GuiGraphics g, int x, int y, String ext, boolean hot) {
		int edge = hot ? 0xFFE6EDF3 : 0xFF8B949E;
		int paper = hot ? 0xFF2B2C2D : 0xFF21262D;
		g.fill(x + 1, y, x + 6, y + 8, paper);
		g.fill(x + 1, y, x + 6, y + 1, edge);
		g.fill(x + 1, y + 7, x + 7, y + 8, edge);
		g.fill(x + 1, y, x + 2, y + 8, edge);
		g.fill(x + 6, y + 2, x + 7, y + 8, edge);
		g.fill(x + 5, y, x + 7, y + 2, edge);
		String kind = ext == null ? "" : ext.toLowerCase();
		int mark = switch (kind) {
			case "mcfunction", "function" -> 0xFFFFCC55;
			case "json" -> 0xFFC084FC;
			case "mcmeta" -> 0xFF67E8F9;
			default -> hot ? 0xFF8B949E : 0xFF6E7681;
		};
		g.fill(x + 3, y + 3, x + 5, y + 4, mark);
		g.fill(x + 3, y + 5, x + 6, y + 6, mark);
	}

	public static void clipLabel(GuiGraphics g, Font font, String text, int x, int y, int maxW, int color) {
		if (text == null) return;
		String ell = "...";
		String shown = text;
		if (font.width(shown) > maxW) {
			shown = font.plainSubstrByWidth(text, Math.max(8, maxW - font.width(ell))) + ell;
		}
		g.drawString(font, shown, x, y, color, false);
	}

	public static Component newFolderLabel() {
		return Component.translatable("screen.bj_mapedit.new_template_folder");
	}

	public static int newFolderW(Font font) {
		return font.width(newFolderLabel());
	}

	public static int headerNewFolderX(int railX, int railW, Font font, boolean selected) {
		return railX + railW - 8 - newFolderW(font);
	}

	public static int templateHeaderTitleW(int railW, Font font, boolean selected) {
		return Math.max(16, railW - 16 - newFolderW(font) - 8);
	}

	public static void drawNewFolder(GuiGraphics g, Font font, int railX, int railW, int headerY, int mouseX, int mouseY, boolean selected) {
		int x = headerNewFolderX(railX, railW, font, selected);
		boolean hover = mouseX >= x && mouseX < x + newFolderW(font)
			&& mouseY >= headerY + 4 && mouseY < headerY + HEADER_H - 2;
		g.drawString(font, newFolderLabel(), x, headerY + 7, hover ? 0xFFE6EDF3 : 0xFF8B949E, false);
	}

	public static boolean hitNewFolder(int railX, int railW, int headerY, Font font, double mouseX, double mouseY, boolean selected) {
		if (mouseY < headerY || mouseY >= headerY + HEADER_H) return false;
		int x = headerNewFolderX(railX, railW, font, selected);
		return mouseX >= x && mouseX < x + newFolderW(font);
	}

	public static final int INSERT_CARET_H = 7;

	public static void fillInsertCaret(GuiGraphics g, int x1, int y1, int x2, int y2, int color) {
		if (x2 != x1 + 1 || y2 - y1 <= INSERT_CARET_H) {
			g.fill(x1, y1, x2, y2, color);
			return;
		}
		int top = (y1 + y2 - INSERT_CARET_H) / 2;
		g.fill(x1, top, x2, top + INSERT_CARET_H, color);
	}

	public static void fillInsertCaret(GuiGraphics g, RenderType type, int x1, int y1, int x2, int y2, int color) {
		if (x2 != x1 + 1 || y2 - y1 <= INSERT_CARET_H) {
			g.fill(type, x1, y1, x2, y2, color);
			return;
		}
		int top = (y1 + y2 - INSERT_CARET_H) / 2;
		g.fill(type, x1, top, x2, top + INSERT_CARET_H, color);
	}

	public static void strokeInputWell(GuiGraphics g, int x, int y, int w, int h, boolean focused) {
		int border = cInputWellBorder(focused);
		hLine(g, x - 1, y - 1, w + 2, border);
		hLine(g, x - 1, y + h, w + 2, border);
		vLine(g, x - 1, y - 1, h + 2, border);
		vLine(g, x + w, y - 1, h + 2, border);
	}

	public static void drawInputWell(GuiGraphics g, int x, int y, int w, int h, boolean focused) {
		int border = cInputWellBorder(focused);
		g.fill(x - 1, y - 1, x + w + 1, y + h + 1, border);
		g.fill(x, y, x + w, y + h, cInputBg());
	}

	public static void drawScrollBar(GuiGraphics g, int x, int y, int h, int scroll, int maxScroll) {
		drawScrollBar(g, x, y, h, scroll, maxScroll, 4);
	}

	public static void drawThinScrollBar(GuiGraphics g, int x, int y, int h, int scroll, int maxScroll) {
		drawThinScrollBar(g, x, y, h, scroll, maxScroll, false);
	}

	public static void drawThinScrollBar(GuiGraphics g, int x, int y, int h, int scroll, int maxScroll, boolean hot) {
		drawScrollBar(g, x, y, h, scroll, maxScroll, 3, hot);
	}

	public static void drawScrollBar(GuiGraphics g, int x, int y, int h, int scroll, int maxScroll, int barW) {
		drawScrollBar(g, x, y, h, scroll, maxScroll, barW, false);
	}

	public static void drawScrollBar(GuiGraphics g, int x, int y, int h, int scroll, int maxScroll, int barW, boolean hot) {
		if (maxScroll <= 0) return;
		int thumbH = thumbHeight(h, maxScroll);
		int thumbY = thumbTop(y, h, scroll, maxScroll);
		g.fill(x, y, x + barW, y + h, hot && itemEditor() ? 0xFF2B2C2D : cScrollTrack());
		g.fill(x, thumbY, x + barW, thumbY + thumbH, cScrollThumb());
	}


	public static void drawButton(GuiGraphics g, Font font, Component label, int x, int y, int w, int h, boolean hovered, boolean active) {
		drawButton(g, font, label, x, y, w, h, hovered, active, false, false);
	}

	public static void drawButton(GuiGraphics g, Font font, Component label, int x, int y, int w, int h, boolean hovered, boolean active, boolean primary) { 		drawButton(g, font, label, x, y, w, h, hovered, active, primary, false); 	}  	public static void drawButton(GuiGraphics g, Font font, Component label, int x, int y, int w, int h, boolean hovered, boolean active, boolean primary, boolean focused) { 		int bg; 		int color; 		if (!active) { 			bg = cButtonDisabled(); 			color = cButtonTextDisabled(); 		} else if (itemEditor() && primary) { 			bg = hovered ? 0xFF1177BB : 0xFF0E639C; 			color = 0xFFFFFFFF; 		} else { 			bg = hovered ? cButtonHover() : cButtonBg(); 			color = cButtonText(); 		} 		g.fill(x, y, x + w, y + h, bg); 		if (itemEditor()) { 			int edge = focused ? 0xFFFFFFFF : 0xFF2B2C2D; 			hLine(g, x, y, w, edge); 			hLine(g, x, y + h - 1, w, edge); 			vLine(g, x, y, h, edge); 			vLine(g, x + w - 1, y, h, edge); 		} else { 			g.fill(x, y, x + w, y + 1, focused ? cFocus() : cButtonEdge()); 			g.fill(x, y + h - 1, x + w, y + h, focused ? cFocus() : cButtonEdge()); 		} 		g.drawCenteredString(font, label, x + w / 2, y + (h - 8) / 2, color); 	} 
	public static void drawDropdown(GuiGraphics g, Font font, java.util.List<String> items, int selected, int x, int y, int w, int mouseX, int mouseY) {
		int rowH = 14;
		int h = items.size() * rowH + 2;
		g.fill(x, y, x + w, y + h, cListBorder());
		g.fill(x + 1, y + 1, x + w - 1, y + h - 1, cListBg());
		for (int i = 0; i < items.size(); i++) {
			int lineY = y + 1 + i * rowH;
			boolean hovered = i == selected || (mouseX >= x && mouseX < x + w && mouseY >= lineY && mouseY < lineY + rowH);
			if (hovered) {
				g.fill(x + 1, lineY, x + w - 1, lineY + rowH, cListHover());
			}
			clipLabel(g, font, items.get(i), x + 4, lineY + 3, w - 8, hovered ? 0xFFFFFFFF : cListText());
		}
	}
	public static int thumbHeight(int h, int maxScroll) {
		if (maxScroll <= 0 || h <= 0) return Math.max(h, 10);
		float ratio = (float) h / (h + maxScroll);
		return Math.max(10, (int) (h * ratio));
	}

	public static int thumbTop(int y, int h, int scroll, int maxScroll) {
		int thumbH = thumbHeight(h, maxScroll);
		if (maxScroll <= 0) return y;
		return y + (int) ((h - thumbH) * ((float) scroll / maxScroll));
	}

	public static boolean hitScrollBar(int x, int y, int h, double mx, double my) {
		return mx >= x - 2 && mx < x + 8 && my >= y && my < y + h;
	}

	public static boolean hitThumb(int x, int y, int h, int scroll, int maxScroll, double mx, double my) {
		if (!hitScrollBar(x, y, h, mx, my) || maxScroll <= 0) return false;
		int thumbH = thumbHeight(h, maxScroll);
		int ty = thumbTop(y, h, scroll, maxScroll);
		return my >= ty && my < ty + thumbH;
	}

	public static double grabOffset(int y, int h, int scroll, int maxScroll, double my) {
		int thumbH = thumbHeight(h, maxScroll);
		double off = my - thumbTop(y, h, scroll, maxScroll);
		if (off < 0) off = 0;
		if (off > thumbH) off = thumbH;
		return off;
	}

	public static int scrollAt(int y, int h, int maxScroll, double my) {
		return scrollAtGrab(y, h, maxScroll, my, thumbHeight(h, maxScroll) / 2.0);
	}

	public static int scrollAtGrab(int y, int h, int maxScroll, double my, double grabFromThumbTop) {
		if (maxScroll <= 0 || h <= 1) return 0;
		int thumbH = thumbHeight(h, maxScroll);
		int travel = h - thumbH;
		if (travel <= 0) return 0;
		double t = (my - y - grabFromThumbTop) / (double) travel;
		if (t < 0) t = 0;
		if (t > 1) t = 1;
		return (int) Math.round(t * maxScroll);
	}

	public static final class ScrollClick {
		public final int scroll;
		public final double grab;
		public ScrollClick(int scroll, double grab) {
			this.scroll = scroll;
			this.grab = grab;
		}
	}

	public static ScrollClick clickBar(int x, int y, int h, int scroll, int maxScroll, double mx, double my) {
		if (maxScroll <= 0 || !hitScrollBar(x, y, h, mx, my)) return null;
		if (hitThumb(x, y, h, scroll, maxScroll, mx, my)) {
			return new ScrollClick(scroll, grabOffset(y, h, scroll, maxScroll, my));
		}
		return new ScrollClick(scrollAt(y, h, maxScroll, my), thumbHeight(h, maxScroll) / 2.0);
	}

	public static void drawThinHScrollBar(GuiGraphics g, int x, int y, int w, int scroll, int maxScroll) {
		if (maxScroll <= 0 || w <= 0) return;
		int thumbW = thumbHeight(w, maxScroll);
		int thumbX = thumbTop(x, w, scroll, maxScroll);
		g.fill(x, y, x + w, y + 3, itemEditor() ? 0xFF1C1D1E : cScrollTrack());
		g.fill(thumbX, y, thumbX + thumbW, y + 3, cScrollThumb());
	}

	public static boolean hitHScrollBar(int x, int y, int w, double mx, double my) {
		return mx >= x && mx < x + w && my >= y - 2 && my < y + 8;
	}

	public static ScrollClick clickHBar(int x, int y, int w, int scroll, int maxScroll, double mx, double my) {
		if (maxScroll <= 0 || !hitHScrollBar(x, y, w, mx, my)) return null;
		int thumbW = thumbHeight(w, maxScroll);
		int tx = thumbTop(x, w, scroll, maxScroll);
		if (mx >= tx && mx < tx + thumbW) {
			return new ScrollClick(scroll, grabOffset(x, w, scroll, maxScroll, mx));
		}
		return new ScrollClick(scrollAt(x, w, maxScroll, mx), thumbW / 2.0);
	}
	public static void pushOverlay(GuiGraphics g) {
		g.pose().pushPose();
		g.pose().translate(0.0F, 0.0F, 400.0F);
	}

	public static void popOverlay(GuiGraphics g) {
		g.pose().popPose();
	}
	private static void hLine(GuiGraphics g, int x, int y, int w, int color) {
		g.fill(x, y, x + w, y + 1, color);
	}

	private static void vLine(GuiGraphics g, int x, int y, int h, int color) {
		g.fill(x, y, x + 1, y + h, color);
	}
}
