package cn.erindax.bjmapedit.client.widget;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

public interface CommandSuggestionHost {
	int[] placeCommandSuggestions(int x, int w, int count);

	void renderCommandUsage(GuiGraphics g, List<FormattedCharSequence> lines, int x, int w, int color, Font font, EditBox box);
}
