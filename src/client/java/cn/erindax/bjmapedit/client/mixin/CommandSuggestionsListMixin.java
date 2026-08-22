package cn.erindax.bjmapedit.client.mixin;

import cn.erindax.bjmapedit.client.screen.DatapackEditorScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.gui.components.CommandSuggestions$SuggestionsList")
public abstract class CommandSuggestionsListMixin {

	@Shadow @Final private Rect2i rect;

	@Inject(method = "<init>", at = @At("RETURN"))
	private void mapedit$placeInEditor(CallbackInfo ci) {
		Screen s = Minecraft.getInstance().screen;
		if (!(s instanceof DatapackEditorScreen ed) || rect == null) return;
		int count = Math.max(1, rect.getHeight() / 12);
		int[] box = ed.placeCommandSuggestions(rect.getX(), rect.getWidth(), count);
		rect.setX(box[0]);
		rect.setY(box[1]);
		rect.setWidth(box[2]);
	}
}
