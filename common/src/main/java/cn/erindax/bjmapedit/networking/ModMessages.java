package cn.erindax.bjmapedit.networking;

import net.minecraft.server.permissions.Permissions;
import cn.erindax.bjmapedit.networking.payload.DatapackOpPayload;
import cn.erindax.bjmapedit.networking.payload.ExecuteCommandsPayload;
import cn.erindax.bjmapedit.networking.payload.GiveItemPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

public final class ModMessages {
	private ModMessages() {
	}

	public static void giveItem(ServerPlayer player, GiveItemPayload payload) {
		if (!EditorPerms.canEdit(player)) return;
		ItemStack stack = payload.stack().copy();
		if (stack.isEmpty()) return;
		player.getInventory().add(stack);
		if (!stack.isEmpty()) {
			ItemEntity dropped = player.drop(stack, false);
			if (dropped != null) dropped.setNoPickUpDelay();
		}
	}

	public static void executeCommands(ServerPlayer player, ExecuteCommandsPayload payload) {
		if (player.isSpectator() || player.isDeadOrDying()) return;
		if (!player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) return;
		MinecraftServer server = player.level().getServer();
		if (server == null) return;
		var source = player.createCommandSourceStack();
		for (String cmd : payload.commands()) {
			server.getCommands().performPrefixedCommand(source, cmd);
		}
	}

	public static void datapackOp(ServerPlayer player, DatapackOpPayload payload) {
		MinecraftServer server = player.level().getServer();
		if (server == null) return;
		server.execute(() -> DatapackOps.handle(player, payload));
	}
}
