package cn.erindax.bjmapedit.client.mixin;

import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class ScreenFocusMixin {

	@Inject(method = "setInitialFocus()V", at = @At("HEAD"), cancellable = true)
	private void mapedit$noInitialButtonFocus(CallbackInfo ci) {
		if (this.getClass().getName().startsWith("cn.erindax.bjmapedit")) {
			ci.cancel();
		}
	}
}
