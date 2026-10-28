package cn.erindax.bjmapedit.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerEventHandler.class)
public abstract class AbstractContainerEventHandlerFocusMixin {

	@Inject(method = "setFocused", at = @At("HEAD"), cancellable = true)
	private void mapedit$skipButtonFocus(GuiEventListener listener, CallbackInfo ci) {
		if (!(listener instanceof AbstractButton)) return;
		Screen s = Minecraft.getInstance().screen;
		if (s == null || !s.getClass().getName().startsWith("cn.erindax.bjmapedit")) return;
		if ((Object) this != s) return;
		ci.cancel();
	}
}
