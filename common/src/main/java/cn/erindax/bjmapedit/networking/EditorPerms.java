package cn.erindax.bjmapedit.networking;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class EditorPerms {

	private EditorPerms() {}

	public static boolean canEdit(ServerPlayer player) {
		if (player == null || player.isSpectator() || player.isDeadOrDying()) return false;
		MinecraftServer server = player.getServer();
		if (server == null) return false;
		if (player.hasPermissions(2)) return true;
		return server.isSingleplayer() && server.isSingleplayerOwner(player.getGameProfile());
	}
}
