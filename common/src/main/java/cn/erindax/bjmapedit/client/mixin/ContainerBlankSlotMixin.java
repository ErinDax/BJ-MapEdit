package cn.erindax.bjmapedit.client.mixin;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public abstract class ContainerBlankSlotMixin {
	@Shadow
	protected Slot hoveredSlot;

	@Inject(method = "renderTooltip", at = @At("HEAD"), cancellable = true)
	private void bjmapedit$hideBlankSlotTooltip(GuiGraphics graphics, int x, int y, CallbackInfo ci) {
		if (this.hoveredSlot == null || !this.hoveredSlot.hasItem()) return;
		ItemStack stack = this.hoveredSlot.getItem();
		CompoundTag tag = stack.getTag();
		if (tag != null && tag.contains("bjshop", Tag.TAG_COMPOUND) && tag.getCompound("bjshop").getBoolean("b")) {
			ci.cancel();
		}
	}
}
