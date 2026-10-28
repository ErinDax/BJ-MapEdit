package cn.erindax.bjmapedit.client.widget;

import net.minecraft.client.resources.language.I18n;
public final class TemplateDrag {
	private static final double START = 5;

	public int storeIndex = -1;
	public String fromFolder = "";
	public String name = "";
	public boolean pending;
	public boolean active;
	public boolean wasSelected;
	public double startX;
	public double startY;
	public String hoverFolder;

	public void reset() {
		storeIndex = -1;
		fromFolder = "";
		name = "";
		pending = false;
		active = false;
		wasSelected = false;
		startX = 0;
		startY = 0;
		hoverFolder = null;
	}

	public void press(int index, String folder, String rowName, boolean selected, double x, double y) {
		storeIndex = index;
		fromFolder = TemplateOrg.norm(folder);
		name = rowName == null || rowName.isBlank() ? I18n.get("screen.bj_mapedit.template_default") : rowName;
		wasSelected = selected;
		pending = true;
		active = false;
		startX = x;
		startY = y;
		hoverFolder = null;
	}

	public boolean move(double x, double y) {
		if (!pending && !active) return false;
		if (!active) {
			double dx = x - startX;
			double dy = y - startY;
			if (dx * dx + dy * dy < START * START) return false;
			active = true;
		}
		return true;
	}

	public boolean busy() {
		return pending || active;
	}

	public boolean shouldMove() {
		return active && storeIndex >= 0 && hoverFolder != null && !TemplateOrg.folderEq(hoverFolder, fromFolder);
	}

	public boolean wasClick() {
		return pending && !active && storeIndex >= 0;
	}
}
