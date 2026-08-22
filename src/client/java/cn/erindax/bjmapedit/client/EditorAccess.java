package cn.erindax.bjmapedit.client;

import cn.erindax.bjmapedit.networking.payload.DatapackOpPayload;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

@Environment(EnvType.CLIENT)
public final class EditorAccess {

	private EditorAccess() {}

	public static boolean canOpen() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null) return false;
		if (mc.hasSingleplayerServer()) return true;
		return mc.player.hasPermissions(2);
	}

	public static boolean isRemoteWorld() {
		return Minecraft.getInstance().getSingleplayerServer() == null;
	}

	public static boolean canSendDatapack() {
		return ClientPlayNetworking.canSend(DatapackOpPayload.TYPE);
	}

	public static void denyOpen() {
		var player = Minecraft.getInstance().player;
		if (player != null) {
			player.displayClientMessage(Component.translatable("screen.bj_mapedit.need_op"), true);
		}
	}

	public static void needMod() {
		var player = Minecraft.getInstance().player;
		if (player != null) {
			player.displayClientMessage(Component.translatable("screen.bj_mapedit.need_mod"), true);
		}
	}
}
