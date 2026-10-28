package cn.erindax.bjmapedit.client.mixin;

import cn.erindax.bjmapedit.client.widget.UiTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractScrollArea;
import net.minecraft.client.gui.components.AbstractTextAreaWidget;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractTextAreaWidget.class)
public abstract class MultiLineEditBoxScrollMixin extends AbstractScrollArea {

	private static final int THIN = 3;

	private MultiLineEditBoxScrollMixin(int x, int y, int width, int height, Component message) {
		super(x, y, width, height, message);
	}

	@Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true)
	private void mapedit$skipVanillaWell(GuiGraphics g, CallbackInfo ci) {
		if (mapedit$ours()) ci.cancel();
	}

	@Inject(method = "renderDecorations", at = @At("HEAD"), cancellable = true)
	private void mapedit$thinDraw(GuiGraphics g, CallbackInfo ci) {
		if (!mapedit$ours()) return;
		ci.cancel();
		if (!this.scrollbarVisible()) return;
		int pad = 2;
		int x = this.getX() + this.getWidth() - THIN - pad;
		int y = this.getY() + pad;
		int h = this.getHeight() - pad * 2;
		int max = Math.max(1, this.maxScrollAmount());
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
