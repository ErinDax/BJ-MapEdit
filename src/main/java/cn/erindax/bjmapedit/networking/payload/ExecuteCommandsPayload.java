package cn.erindax.bjmapedit.networking.payload;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record ExecuteCommandsPayload(List<String> commands) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<ExecuteCommandsPayload> TYPE =
		new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("bj_mapedit", "execute_commands"));

	public static final StreamCodec<RegistryFriendlyByteBuf, ExecuteCommandsPayload> CODEC =
		StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()),
			ExecuteCommandsPayload::commands,
			ExecuteCommandsPayload::new
		);

	@Override
	public CustomPacketPayload.Type<ExecuteCommandsPayload> type() {
		return TYPE;
	}
}
