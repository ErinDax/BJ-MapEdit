package cn.erindax.bjmapedit.client.widget;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public final class RenameDeleteMenu {
	public enum Action { NONE, RENAME, DELETE, DISMISS }

	private static final int ROW = 18;

	private boolean open;
	private int x, y, w, h;

	public boolean isOpen() {
		return open;
	}

	public boolean covers(double mx, double my) {
		return open && mx >= x && mx < x + w && my >= y && my < y + h;
	}

	public void show(Font font, int mx, int my, int screenW, int screenH) {
		open = true;
		int pad = 10;
		int renameW = font.width(Component.translatable("screen.bj_mapedit.template_rename"));
		int deleteW = font.width(Component.translatable("screen.bj_mapedit.template_delete"));
		w = Math.max(renameW, deleteW) + pad * 2;
		h = 4 + ROW * 2;
		x = Mth.clamp(mx, 2, Math.max(2, screenW - w - 2));
		y = Mth.clamp(my, 2, Math.max(2, screenH - h - 2));
	}

	public void close() {
		open = false;
	}

	public Action pick(double mx, double my) {
		if (!open) return Action.NONE;
		int hit = hit(mx, my);
		close();
		if (hit == 0) return Action.RENAME;
		if (hit == 1) return Action.DELETE;
		return Action.DISMISS;
	}

	public void draw(GuiGraphics g, Font font, int mouseX, int mouseY) {
		if (!open) return;
		g.pose().pushPose();
		g.pose().translate(0.0F, 0.0F, 600.0F);
		int bg = UiTheme.cPanelBg() | 0xFF000000;
		int border = UiTheme.cPanelBorder() | 0xFF000000;
		g.fill(x, y, x + w, y + h, bg);
		g.fill(x, y, x + w, y + 1, border);
		g.fill(x, y + h - 1, x + w, y + h, border);
		g.fill(x, y, x + 1, y + h, border);
		g.fill(x + w - 1, y, x + w, y + h, border);
		int hit = hit(mouseX, mouseY);
		for (int i = 0; i < 2; i++) {
			int lineY = y + 2 + i * ROW;
			Component text = i == 0
				? Component.translatable("screen.bj_mapedit.template_rename")
				: Component.translatable("screen.bj_mapedit.template_delete");
			int color;
			if (i == 1) {
				color = hit == i ? 0xFFF85149 : 0xFF8B949E;
			} else {
				color = hit == i ? 0xFFE6EDF3 : 0xFF8B949E;
			}
			g.drawString(font, text, x + (w - font.width(text)) / 2, lineY + 4, color, false);
		}
		g.pose().popPose();
	}

	private int hit(double mx, double my) {
		if (!open || mx < x || mx >= x + w || my < y || my >= y + h) return -1;
		int inner = (int) my - y - 2;
		if (inner < 0 || inner >= ROW * 2) return -1;
		return inner / ROW;
	}
}
