package cn.erindax.bjmapedit.client.mixin;

import cn.erindax.bjmapedit.client.widget.UiTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractButton.class)
public abstract class AbstractButtonThemeMixin extends AbstractWidget {

	public AbstractButtonThemeMixin(int x, int y, int w, int h, net.minecraft.network.chat.Component message) {
		super(x, y, w, h, message);
	}

	private static boolean mapedit$ours() {
		Screen s = Minecraft.getInstance().screen;
		return s != null && s.getClass().getName().startsWith("cn.erindax.bjmapedit");
	}

	@Inject(method = "renderWidget", at = @At("HEAD"), cancellable = true)
	private void mapedit$drawThemedButton(GuiGraphics g, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
		if (!mapedit$ours() || (Object) this instanceof Checkbox) return;
		boolean primary = false;
		if (this.getMessage().getContents() instanceof TranslatableContents tc) {
			primary = tc.getKey().endsWith(".save") || tc.getKey().endsWith(".save_template")
				|| tc.getKey().endsWith(".apply_now") || tc.getKey().endsWith(".done");
		}
		UiTheme.drawButton(g, Minecraft.getInstance().font, this.getMessage(),
			this.getX(), this.getY(), this.getWidth(), this.getHeight(),
			this.isHovered(), this.active, primary, this.isFocused());
		ci.cancel();
	}

	@Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
	private void mapedit$noKeyActivate(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
		if (!mapedit$ours()) return;
		cir.setReturnValue(false);
	}
}
