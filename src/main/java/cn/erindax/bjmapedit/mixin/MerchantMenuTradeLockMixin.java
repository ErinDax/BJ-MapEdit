package cn.erindax.bjmapedit.mixin;

import cn.erindax.bjmapedit.VillagerTradeLock;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MerchantMenu.class)
public abstract class MerchantMenuTradeLockMixin {

	@Inject(method = "removed", at = @At("HEAD"))
	private void mapedit$allowCloseStart(Player player, CallbackInfo ci) {
		VillagerTradeLock.allowStop();
	}

	@Inject(method = "removed", at = @At("RETURN"))
	private void mapedit$allowCloseEnd(Player player, CallbackInfo ci) {
		VillagerTradeLock.endAllow();
	}
}
