package cn.erindax.bjmapedit.client.widget;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

public class ThemedEditBox extends EditBox {

	public static final int BG = UiTheme.INPUT_BG;
	public static final int BORDER = UiTheme.INPUT_BORDER;
	public static final int BORDER_FOCUS = UiTheme.ACCENT;
	public static final int TEXT = UiTheme.INPUT_TEXT;

	public ThemedEditBox(Font font, int x, int y, int w, int h, Component message) {
		super(font, x, y, w, h, message);
		style(this);
	}

	public static void style(EditBox box) {
		box.setBordered(false);
		box.setTextColor(UiTheme.cInputText());
		box.setTextColorUneditable(0xFFBBBBBB);
		box.setMaxLength(32767);
	}

	public static void renderBackground(GuiGraphics g, EditBox box) {
		if (!box.isVisible()) return;
		UiTheme.drawInputWell(g, box.getX(), box.getY(), box.getWidth(), box.getHeight(), box.isFocused());
	}

	@Override
	public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		renderBackground(g, this);
		super.renderWidget(g, mouseX, mouseY, partialTick);
	}
}
