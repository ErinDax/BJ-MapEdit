package cn.erindax.bjmapedit.client.mixin;

import cn.erindax.bjmapedit.client.widget.UiTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractScrollWidget;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractScrollWidget.class)
public abstract class MultiLineEditBoxScrollMixin {

	private static final int THIN = 3;

	@Shadow
	protected abstract boolean scrollbarVisible();

	@Shadow
	protected abstract double scrollAmount();

	@Shadow
	protected abstract int getMaxScrollAmount();

	@Inject(method = "scrollbarWidth", at = @At("HEAD"), cancellable = true)
	private void mapedit$thinWidth(CallbackInfoReturnable<Integer> cir) {
		if (mapedit$ours()) cir.setReturnValue(THIN);
	}

	@Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true)
	private void mapedit$skipVanillaWell(GuiGraphics g, CallbackInfo ci) {
		if (mapedit$ours()) ci.cancel();
	}

	@Inject(method = "renderScrollBar", at = @At("HEAD"), cancellable = true)
	private void mapedit$thinDraw(GuiGraphics g, CallbackInfo ci) {
		if (!mapedit$ours()) return;
		ci.cancel();
		if (!this.scrollbarVisible()) return;
		AbstractWidget self = (AbstractWidget) (Object) this;
		int pad = 2;
		int x = self.getX() + self.getWidth() - THIN - pad;
		int y = self.getY() + pad;
		int h = self.getHeight() - pad * 2;
		int max = Math.max(1, this.getMaxScrollAmount());
		int thumbH = Math.max(8, (int) (h * (h / (double) (h + max))));
		if (thumbH > h) thumbH = h;
		int thumbY = y + (int) ((h - thumbH) * (this.scrollAmount() / (double) max));
		g.fill(x, y, x + THIN, y + h, UiTheme.cScrollTrack());
		g.fill(x, thumbY, x + THIN, thumbY + thumbH, UiTheme.cScrollThumb());
	}

	private boolean mapedit$ours() {
		Screen s = Minecraft.getInstance().screen;
		return s != null
			&& s.getClass().getName().startsWith("cn.erindax.bjmapedit")
			&& ((Object) this instanceof MultiLineEditBox);
	}
}
