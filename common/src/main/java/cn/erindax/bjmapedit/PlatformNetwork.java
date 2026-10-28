package cn.erindax.bjmapedit;

import cn.erindax.bjmapedit.networking.payload.ModPayload;
import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class PlatformNetwork {
	private PlatformNetwork() {
	}

	@ExpectPlatform
	public static boolean canSendToServer(ResourceLocation id) {
		throw new AssertionError();
	}

	@ExpectPlatform
	public static void sendToServer(ModPayload payload) {
		throw new AssertionError();
	}

	@ExpectPlatform
	public static void sendToPlayer(ServerPlayer player, ModPayload payload) {
		throw new AssertionError();
	}
}
