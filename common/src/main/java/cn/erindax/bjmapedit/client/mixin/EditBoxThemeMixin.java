package cn.erindax.bjmapedit.client.mixin;

import cn.erindax.bjmapedit.client.widget.ImeCaret;
import cn.erindax.bjmapedit.client.widget.ThemedEditBox;
import cn.erindax.bjmapedit.client.widget.UiTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EditBox.class)
public abstract class EditBoxThemeMixin {

	@Shadow public abstract void setTextColor(int color);
	@Shadow public abstract void setTextColorUneditable(int color);
	@Shadow public abstract void setMaxLength(int length);
	@Shadow public abstract void setBordered(boolean bordered);
	@Shadow public abstract boolean isVisible();

	private static boolean mapedit$ours() {
		Screen s = Minecraft.getInstance().screen;
		return s != null && s.getClass().getName().startsWith("cn.erindax.bjmapedit");
	}

	@Inject(method = "<init>(Lnet/minecraft/client/gui/Font;IIIILnet/minecraft/network/chat/Component;)V", at = @At("RETURN"))
	private void mapedit$styleMain(CallbackInfo ci) {
		mapedit$apply();
	}

	@Inject(method = "<init>(Lnet/minecraft/client/gui/Font;IIIILnet/minecraft/client/gui/components/EditBox;Lnet/minecraft/network/chat/Component;)V", at = @At("RETURN"))
	private void mapedit$styleCopy(CallbackInfo ci) {
		mapedit$apply();
	}

	private void mapedit$apply() {
		if (!mapedit$ours()) return;
		this.setBordered(false);
		this.setTextColor(UiTheme.cInputText());
		this.setTextColorUneditable(0xFFBBBBBB);
		this.setMaxLength(32767);
	}

	@Inject(method = "renderWidget", at = @At("HEAD"))
	private void mapedit$drawThemeBg(GuiGraphics g, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
		if (!mapedit$ours() || !this.isVisible()) return;
		this.setBordered(false);
		this.setTextColor(UiTheme.cInputText());
		EditBox self = (EditBox) (Object) this;
		ThemedEditBox.renderBackground(g, self);
		g.pose().pushPose();
		int dy = Math.max(0, self.getHeight() - 8 - 1);
		g.pose().translate(3.0F, (float) dy, 0.0F);
	}

	@Inject(method = "renderWidget", at = @At("RETURN"))
	private void mapedit$unlift(GuiGraphics g, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
		if (!mapedit$ours() || !this.isVisible()) return;
		g.pose().popPose();
		EditBox self = (EditBox) (Object) this;
		if (self.isFocused()) {
			ImeCaret.moveToEditBox(self);
		}
	}

	@Redirect(method = "renderWidget", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;fill(Lnet/minecraft/client/renderer/RenderType;IIIII)V"))
	private void mapedit$shortCaret(GuiGraphics g, RenderType type, int x1, int y1, int x2, int y2, int color) {
		if (mapedit$ours()) {
			UiTheme.fillInsertCaret(g, type, x1, y1, x2, y2, color);
		} else {
			g.fill(type, x1, y1, x2, y2, color);
		}
	}
}
