package cn.erindax.bjmapedit.client.mixin;

import cn.erindax.bjmapedit.client.widget.ImeCaret;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftImeMixin {

	@Unique
	private boolean mapedit$inOurScreen;

	@Inject(method = "setScreen", at = @At("RETURN"))
	private void mapedit$enableIme(Screen screen, CallbackInfo ci) {
		Screen now = ((Minecraft) (Object) this).screen;
		boolean ours = now != null && now.getClass().getName().startsWith("cn.erindax.bjmapedit");
		if (ours) {
			ImeCaret.setEnabled(true);
		} else if (now == null && mapedit$inOurScreen) {
			ImeCaret.setEnabled(false);
		}
		mapedit$inOurScreen = ours;
	}
}
