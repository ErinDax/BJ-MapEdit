package cn.erindax.bjmapedit.client.mixin;

import cn.erindax.bjmapedit.client.widget.ImeCaret;
import cn.erindax.bjmapedit.client.widget.UiTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiLineEditBox.class)
public abstract class MultiLineEditBoxCursorMixin {

	private static boolean mapedit$ours() {
		Screen s = Minecraft.getInstance().screen;
		return s != null && s.getClass().getName().startsWith("cn.erindax.bjmapedit");
	}

	@Redirect(method = "renderContents", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V"))
	private void mapedit$shortCaret(GuiGraphics g, int x1, int y1, int x2, int y2, int color) {
		if (mapedit$ours()) {
			UiTheme.fillInsertCaret(g, x1, y1, x2, y2, color);
		} else {
			g.fill(x1, y1, x2, y2, color);
		}
	}

	@Inject(method = "renderContents", at = @At("RETURN"))
	private void mapedit$imeCaret(GuiGraphics g, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
		if (!mapedit$ours()) return;
		MultiLineEditBox self = (MultiLineEditBox) (Object) this;
		if (self.isFocused()) {
			ImeCaret.moveToMultiLine(self, Minecraft.getInstance().font);
		}
	}
}
