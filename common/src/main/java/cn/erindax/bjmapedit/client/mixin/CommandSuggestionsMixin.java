package cn.erindax.bjmapedit.client.mixin;

import cn.erindax.bjmapedit.client.screen.DatapackEditorScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(CommandSuggestions.class)
public abstract class CommandSuggestionsMixin {

	@Shadow @Final private EditBox input;
	@Shadow @Final private Font font;
	@Shadow @Final private int fillColor;
	@Shadow @Final private List<FormattedCharSequence> commandUsage;
	@Shadow private int commandUsagePosition;
	@Shadow private int commandUsageWidth;

	@Inject(method = "renderUsage", at = @At("HEAD"), cancellable = true)
	private void mapedit$usageInEditor(GuiGraphics g, CallbackInfo ci) {
		if (!(Minecraft.getInstance().screen instanceof DatapackEditorScreen ed)) return;
		ci.cancel();
		ed.renderCommandUsage(g, commandUsage, commandUsagePosition, commandUsageWidth, fillColor, font, input);
	}
}
