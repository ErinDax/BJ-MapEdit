package cn.erindax.bjmapedit;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

public final class PlatformNetwork {
	private PlatformNetwork() {
	}

	@ExpectPlatform
	public static boolean canSendToServer(CustomPacketPayload.Type<?> type) {
		throw new AssertionError();
	}

	@ExpectPlatform
	public static void sendToServer(CustomPacketPayload payload) {
		throw new AssertionError();
	}

	@ExpectPlatform
	public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
		throw new AssertionError();
	}
}
