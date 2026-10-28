package cn.erindax.bjmapedit.networking.payload;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public interface ModPayload {
	ResourceLocation id();

	void write(FriendlyByteBuf buf);
}
