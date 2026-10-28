package cn.erindax.bjmapedit.networking;

import net.minecraft.server.permissions.Permissions;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class EditorPerms {

	private EditorPerms() {}

	public static boolean canEdit(ServerPlayer player) {
		if (player == null || player.isSpectator() || player.isDeadOrDying()) return false;
		MinecraftServer server = player.level().getServer();
		if (server == null) return false;
		if (player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) return true;
		return server.isSingleplayer() && server.isSingleplayerOwner(player.nameAndId());
	}
}
