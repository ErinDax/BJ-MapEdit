package cn.erindax.bjmapedit.networking.payload;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record ExecuteCommandsPayload(List<String> commands) implements ModPayload {
	public static final ResourceLocation ID = new ResourceLocation("bj_mapedit", "execute_commands");

	public static ExecuteCommandsPayload read(FriendlyByteBuf buf) {
		return new ExecuteCommandsPayload(buf.readList(FriendlyByteBuf::readUtf));
	}

	@Override
	public ResourceLocation id() {
		return ID;
	}

	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeCollection(commands, FriendlyByteBuf::writeUtf);
	}
}
