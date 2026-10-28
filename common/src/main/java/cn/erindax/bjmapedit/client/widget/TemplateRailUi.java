package cn.erindax.bjmapedit.client.widget;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class TemplateRailUi {
	private TemplateRailUi() {}

	public static int indexAt(double mouseY, int listTop, int listBottom, int scroll, int rowH, int count) {
		if (mouseY < listTop || mouseY >= listBottom) return -1;
		int idx = (int) ((mouseY - listTop + scroll) / rowH);
		if (idx < 0 || idx >= count) return -1;
		return idx;
	}

	public static int renamingRow(List<TemplateOrg.Row> rows, int renamingTemplate, String renamingFolder) {
		if (rows == null) return -1;
		String folder = TemplateOrg.norm(renamingFolder);
		if (!folder.isEmpty()) {
			for (int i = 0; i < rows.size(); i++) {
				TemplateOrg.Row row = rows.get(i);
				if (row.folder && folder.equals(row.folderName)) return i;
			}
			return -1;
		}
		if (renamingTemplate < 0) return -1;
		for (int i = 0; i < rows.size(); i++) {
			TemplateOrg.Row row = rows.get(i);
			if (!row.folder && row.storeIndex == renamingTemplate) return i;
		}
		return -1;
	}

	public static void drawHeader(GuiGraphics g, Font font, String title, int x, int w, int headerY, int mouseX, int mouseY, boolean hasSel) {
		g.fill(x + 1, headerY + 1, x + w - 1, headerY + UiTheme.HEADER_H, UiTheme.cHeaderBg());
		UiTheme.clipLabel(g, font, title, x + 8, headerY + 7, UiTheme.templateHeaderTitleW(w, font, false), UiTheme.cHeaderTitle());
		UiTheme.drawNewFolder(g, font, x, w, headerY, mouseX, mouseY, false);
	}

	public static String dropTarget(
		double mouseX, double mouseY,
		int railX, int railW, int headerY, int height,
		int listTop, int listBottom, int scroll, int rowH,
		List<TemplateOrg.Row> rows
	) {
		if (mouseX < railX || mouseX >= railX + railW || mouseY < headerY || mouseY >= headerY + height) return null;
		if (mouseY < listTop || rows == null || rows.isEmpty()) return "";
		int idx = indexAt(mouseY, listTop, listBottom, scroll, rowH, rows.size());
		if (idx < 0) return "";
		return TemplateOrg.norm(rows.get(idx).folderName);
	}

	public static void drawDragOverlay(GuiGraphics g, Font font, TemplateDrag drag, int mouseX, int mouseY, int railX, int railW, int headerY) {
		if (drag == null || !drag.active) return;
		if (drag.hoverFolder != null && drag.hoverFolder.isEmpty()) {
			g.fill(railX + 1, headerY + 1, railX + railW - 1, headerY + UiTheme.HEADER_H, 0x662D333B);
		}
		int gx = mouseX + 10;
		int gy = mouseY - 6;
		int gw = Math.min(132, font.width(drag.name) + 8);
		g.fill(gx - 3, gy - 3, gx + gw, gy + 13, 0xF021262D);
		UiTheme.clipLabel(g, font, drag.name, gx, gy, gw - 4, 0xFFE6EDF3);
	}

	public static void drawRows(
		GuiGraphics g, Font font,
		int x, int w, int listTop, int listBottom, int rowH, int scroll,
		List<TemplateOrg.Row> rows,
		int selectedTemplate, String selectedFolder,
		int renamingTemplate, String renamingFolder, EditBox renameBox,
		int nameRight,
		int mouseX, int mouseY,
		String dropFolder, int draggingStoreIndex
	) {
		if (rows == null || rows.isEmpty()) {
			UiTheme.muted(g, font, Component.translatable("screen.bj_mapedit.template_empty").getString(), x + 8, listTop + 4);
			return;
		}
		int renameRow = renamingRow(rows, renamingTemplate, renamingFolder);
		g.enableScissor(x + 1, listTop, x + w - 1, listBottom);
		for (int i = 0; i < rows.size(); i++) {
			int rowY = listTop + i * rowH - scroll;
			if (rowY + rowH < listTop || rowY > listBottom) continue;
			TemplateOrg.Row row = rows.get(i);
			boolean drop = row.folder && dropFolder != null && !dropFolder.isEmpty() && TemplateOrg.folderEq(dropFolder, row.folderName);
			boolean dragging = !row.folder && draggingStoreIndex >= 0 && row.storeIndex == draggingStoreIndex;
			boolean hover = mouseX >= x + 1 && mouseX < x + w - 1 && mouseY >= rowY && mouseY < rowY + rowH
				&& mouseY >= listTop && mouseY < listBottom;
			if (drop) {
				g.fill(x + 1, rowY, x + w - 1, rowY + rowH, 0xFF3A3420);
			} else if (hover && draggingStoreIndex < 0) {
				g.fill(x + 1, rowY, x + w - 1, rowY + rowH, 0xFF191A1B);
			}
			int iconX = TemplateOrg.rowIconX(x, row.depth);
			boolean hot = drop || (hover && draggingStoreIndex < 0);
			if (row.folder) {
				UiTheme.drawFolderIcon(g, iconX, rowY + 7, row.open, hot);
			} else {
				UiTheme.drawTemplateFileIcon(g, x, rowY, row.depth, hot && !dragging);
			}
			int nameX = TemplateOrg.rowNameX(x, row.depth);
			int nameW = Math.max(24, nameRight - nameX - 4);
			if (renameRow == i && renameBox != null && renameBox.visible) {
				renameBox.render(g, mouseX, mouseY, 0);
			} else {
				String name = row.name == null || row.name.isBlank() ? (row.folder ? "文件夹" : "模板") : row.name;
				int color = dragging ? 0xFF6E7681 : (hot ? 0xFFE6EDF3 : UiTheme.cListText());
				UiTheme.clipLabel(g, font, name, nameX, rowY + 7, nameW, color);
			}
		}
		g.disableScissor();
	}
}
