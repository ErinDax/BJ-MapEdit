package cn.erindax.bjmapedit.networking.payload;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public record GiveItemPayload(ItemStack stack) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<GiveItemPayload> TYPE =
		new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("bj_mapedit", "give_item"));

	public static final StreamCodec<RegistryFriendlyByteBuf, GiveItemPayload> CODEC =
		StreamCodec.composite(
			ItemStack.OPTIONAL_STREAM_CODEC, GiveItemPayload::stack,
			GiveItemPayload::new
		);

	@Override
	public CustomPacketPayload.Type<GiveItemPayload> type() {
		return TYPE;
	}
}
