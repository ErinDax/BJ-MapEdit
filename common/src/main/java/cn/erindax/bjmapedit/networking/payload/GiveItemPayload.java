package cn.erindax.bjmapedit.networking.payload;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public record GiveItemPayload(ItemStack stack) implements ModPayload {
	public static final ResourceLocation ID = new ResourceLocation("bj_mapedit", "give_item");

	public static GiveItemPayload read(FriendlyByteBuf buf) {
		return new GiveItemPayload(buf.readItem());
	}

	@Override
	public ResourceLocation id() {
		return ID;
	}

	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeItem(stack);
	}
}
