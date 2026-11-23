package cn.erindax.bjmapedit.client.mixin;

import cn.erindax.bjmapedit.client.widget.CommandSuggestionHost;
import com.mojang.brigadier.ParseResults;
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
import java.util.concurrent.CompletableFuture;

@Mixin(CommandSuggestions.class)
public abstract class CommandSuggestionsMixin {

	@Shadow @Final private EditBox input;
	@Shadow @Final private Font font;
	@Shadow @Final private int fillColor;
	@Shadow @Final private List<FormattedCharSequence> commandUsage;
	@Shadow private int commandUsagePosition;
	@Shadow private int commandUsageWidth;
	@Shadow private ParseResults<?> currentParse;
	@Shadow private CompletableFuture<?> pendingSuggestions;

	@Inject(method = "setAllowSuggestions", at = @At("RETURN"))
	private void mapedit$clearWhenDisabled(boolean allow, CallbackInfo ci) {
		if (allow || !(Minecraft.getInstance().screen instanceof CommandSuggestionHost)) return;
		currentParse = null;
		pendingSuggestions = null;
		commandUsage.clear();
	}

	@Inject(method = "renderUsage", at = @At("HEAD"), cancellable = true)
	private void mapedit$usageInEditor(GuiGraphics g, CallbackInfo ci) {
		if (!(Minecraft.getInstance().screen instanceof CommandSuggestionHost ed)) return;
		ci.cancel();
		ed.renderCommandUsage(g, commandUsage, commandUsagePosition, commandUsageWidth, fillColor, font, input);
	}
}
