package cn.erindax.bjmapedit.fabric;

import cn.erindax.bjmapedit.client.EditorAccess;
import cn.erindax.bjmapedit.networking.payload.ModPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class PlatformNetworkImpl {
	private PlatformNetworkImpl() {
	}

	public static boolean canSendToServer(ResourceLocation id) {
		return ClientPlayNetworking.canSend(id);
	}

	public static void sendToServer(ModPayload payload) {
		if (!EditorAccess.fitsServerbound(payload)) {
			EditorAccess.tooLarge();
			return;
		}
		ClientPlayNetworking.send(payload.id(), encode(payload));
	}

	public static void sendToPlayer(ServerPlayer player, ModPayload payload) {
		ServerPlayNetworking.send(player, payload.id(), encode(payload));
	}

	private static FriendlyByteBuf encode(ModPayload payload) {
		FriendlyByteBuf buf = PacketByteBufs.create();
		payload.write(buf);
		return buf;
	}
}
