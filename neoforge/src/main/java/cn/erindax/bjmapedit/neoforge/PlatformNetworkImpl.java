package cn.erindax.bjmapedit.neoforge;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.PacketDistributor;

public final class PlatformNetworkImpl {
	private PlatformNetworkImpl() {
	}

	public static boolean canSendToServer(CustomPacketPayload.Type<?> type) {
		ClientPacketListener connection = Minecraft.getInstance().getConnection();
		return connection != null && connection.hasChannel(type);
	}

	public static void sendToServer(CustomPacketPayload payload) {
		if (canSendToServer(payload.type())) {
			ClientPacketDistributor.sendToServer(payload);
		}
	}

	public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
		if (player.connection.hasChannel(payload.type())) {
			PacketDistributor.sendToPlayer(player, payload);
		}
	}
}
