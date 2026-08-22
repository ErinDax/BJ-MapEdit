package cn.erindax.bjmapedit.networking.payload;

import cn.erindax.bjmapedit.networking.DatapackOps;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record DatapackOpResultPayload(int kind, String path, String extra) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<DatapackOpResultPayload> TYPE =
		new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("bj_mapedit", "datapack_op_result"));

	public static final StreamCodec<RegistryFriendlyByteBuf, DatapackOpResultPayload> CODEC = StreamCodec.of(
		(buf, v) -> {
			buf.writeVarInt(v.kind());
			buf.writeUtf(v.path() == null ? "" : v.path(), 512);
			buf.writeUtf(v.extra() == null ? "" : v.extra(), DatapackOps.MAX_TEXT);
		},
		buf -> new DatapackOpResultPayload(buf.readVarInt(), buf.readUtf(512), buf.readUtf(DatapackOps.MAX_TEXT))
	);

	@Override
	public CustomPacketPayload.Type<DatapackOpResultPayload> type() {
		return TYPE;
	}
}
