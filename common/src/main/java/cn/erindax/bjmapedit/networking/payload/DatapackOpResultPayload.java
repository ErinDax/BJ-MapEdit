package cn.erindax.bjmapedit.networking.payload;

import cn.erindax.bjmapedit.networking.DatapackOps;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record DatapackOpResultPayload(int kind, String path, String extra) implements ModPayload {
	public static final ResourceLocation ID = new ResourceLocation("bj_mapedit", "datapack_op_result");

	public static DatapackOpResultPayload read(FriendlyByteBuf buf) {
		return new DatapackOpResultPayload(buf.readVarInt(), buf.readUtf(512), buf.readUtf(DatapackOps.MAX_TEXT));
	}

	@Override
	public ResourceLocation id() {
		return ID;
	}

	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeVarInt(kind);
		buf.writeUtf(path == null ? "" : path, 512);
		buf.writeUtf(extra == null ? "" : extra, DatapackOps.MAX_TEXT);
	}
}
