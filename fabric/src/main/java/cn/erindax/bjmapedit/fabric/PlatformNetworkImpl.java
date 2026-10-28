package cn.erindax.bjmapedit.fabric;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

public final class PlatformNetworkImpl {
	private PlatformNetworkImpl() {
	}

	public static boolean canSendToServer(CustomPacketPayload.Type<?> type) {
		return ClientPlayNetworking.canSend(type);
	}

	public static void sendToServer(CustomPacketPayload payload) {
		ClientPlayNetworking.send(payload);
	}

	public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
		ServerPlayNetworking.send(player, payload);
	}
}
