package cn.erindax.bjmapedit.networking;

import cn.erindax.bjmapedit.networking.payload.DatapackOpPayload;
import cn.erindax.bjmapedit.networking.payload.ExecuteCommandsPayload;
import cn.erindax.bjmapedit.networking.payload.GiveItemPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class ModMessages {
	private ModMessages() {
	}

	public static void giveItem(ServerPlayer player, GiveItemPayload payload) {
		if (!EditorPerms.canEdit(player)) return;
		player.getInventory().add(payload.stack());
	}

	public static void executeCommands(ServerPlayer player, ExecuteCommandsPayload payload) {
		if (player.isSpectator() || player.isDeadOrDying()) return;
		if (!player.hasPermissions(2)) return;
		MinecraftServer server = player.getServer();
		if (server == null) return;
		var source = player.createCommandSourceStack();
		for (String cmd : payload.commands()) {
			server.getCommands().performPrefixedCommand(source, cmd);
		}
	}

	public static void datapackOp(ServerPlayer player, DatapackOpPayload payload) {
		MinecraftServer server = player.getServer();
		if (server == null) return;
		server.execute(() -> DatapackOps.handle(player, payload));
	}
}
