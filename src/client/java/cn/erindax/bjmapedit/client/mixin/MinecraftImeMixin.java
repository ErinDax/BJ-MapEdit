package cn.erindax.bjmapedit.client.mixin;

import cn.erindax.bjmapedit.client.widget.ImeCaret;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftImeMixin {

	@Inject(method = "setScreen", at = @At("RETURN"))
	private void mapedit$enableIme(Screen screen, CallbackInfo ci) {
		boolean ours = screen != null && screen.getClass().getName().startsWith("cn.erindax.bjmapedit");
		ImeCaret.setEnabled(ours);
	}
}
