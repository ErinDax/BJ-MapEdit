package cn.erindax.bjmapedit.mixin;

import cn.erindax.bjmapedit.VillagerTradeLock;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.trading.MerchantOffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Villager.class)
public abstract class VillagerKeepOffersMixin {

	private static final String KEEP_OFFERS = "bj_mapedit.keep_offers";
	private static final String KEEP_OFFERS_LEGACY = "mapedit_bj.keep_offers";
	private static final String KEEP_NBT = "mapedit_keep_offers";

	@Unique
	private boolean mapedit$keep;

	@Unique
	private MerchantOffers mapedit$keptOffers;

	@Unique
	private boolean mapedit$keepOffers() {
		Villager self = (Villager) (Object) this;
		return !self.level().isClientSide && (mapedit$keep || mapedit$tagged(self));
	}

	@Unique
	private static boolean mapedit$tagged(Villager self) {
		return self.getTags().contains(KEEP_OFFERS) || self.getTags().contains(KEEP_OFFERS_LEGACY);
	}

	@Inject(method = "readAdditionalSaveData", at = @At("RETURN"))
	private void mapedit$readKeepFlag(CompoundTag nbt, CallbackInfo ci) {
		if (nbt.getBoolean(KEEP_NBT) || mapedit$tagged((Villager) (Object) this)) {
			mapedit$keep = true;
		}
	}

	@Inject(method = "addAdditionalSaveData", at = @At("RETURN"))
	private void mapedit$writeKeepFlag(CompoundTag nbt, CallbackInfo ci) {
		if (mapedit$keep) nbt.putBoolean(KEEP_NBT, true);
	}

	@Inject(method = "setVillagerData", at = @At("HEAD"))
	private void mapedit$keepOffersBeforeProfession(VillagerData data, CallbackInfo ci) {
		if (!mapedit$keepOffers()) return;
		Villager self = (Villager) (Object) this;
		MerchantOffers current = self.getOffers();
		if (current != null && !current.isEmpty()) {
			mapedit$keptOffers = current;
		}
	}

	@Inject(method = "setVillagerData", at = @At("RETURN"))
	private void mapedit$restoreOffersAfterProfession(VillagerData data, CallbackInfo ci) {
		if (mapedit$keptOffers == null) return;
		((Villager) (Object) this).setOffers(mapedit$keptOffers);
		mapedit$keptOffers = null;
	}

	@Inject(method = "updateTrades", at = @At("HEAD"), cancellable = true)
	private void mapedit$skipGeneratedTrades(CallbackInfo ci) {
		if (mapedit$keepOffers()) ci.cancel();
	}

	@Inject(method = "customServerAiStep", at = @At("HEAD"), cancellable = true)
	private void mapedit$pauseBrainWhileTrading(CallbackInfo ci) {
		Villager self = (Villager) (Object) this;
		if (mapedit$keepOffers() && self.isTrading()) ci.cancel();
	}

	@Inject(method = "setTradingPlayer", at = @At("HEAD"), cancellable = true)
	private void mapedit$blockAiCancel(Player player, CallbackInfo ci) {
		if (player != null || VillagerTradeLock.isAllowed() || !mapedit$keepOffers()) return;
		Villager self = (Villager) (Object) this;
		if (!self.isAlive()) return;
		Player cur = self.getTradingPlayer();
		if (cur != null && cur.isAlive() && !cur.isSpectator() && self.distanceToSqr(cur) <= 16.0) {
			ci.cancel();
		}
	}
}
