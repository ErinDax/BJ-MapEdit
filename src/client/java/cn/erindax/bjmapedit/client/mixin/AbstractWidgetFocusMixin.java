package cn.erindax.bjmapedit.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractWidget.class)
public abstract class AbstractWidgetFocusMixin {

	@Inject(method = "nextFocusPath", at = @At("HEAD"), cancellable = true)
	private void mapedit$skipButtons(FocusNavigationEvent event, CallbackInfoReturnable<ComponentPath> cir) {
		Screen s = Minecraft.getInstance().screen;
		if (s == null || !s.getClass().getName().startsWith("cn.erindax.bjmapedit")) return;
		if ((Object) this instanceof AbstractButton) {
			cir.setReturnValue(null);
		}
	}
}
