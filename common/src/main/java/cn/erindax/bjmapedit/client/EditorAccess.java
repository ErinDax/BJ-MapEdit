package cn.erindax.bjmapedit.client;

import net.minecraft.server.permissions.Permissions;
import cn.erindax.bjmapedit.networking.payload.DatapackOpPayload;
import cn.erindax.bjmapedit.networking.payload.ExecuteCommandsPayload;
import cn.erindax.bjmapedit.PlatformNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import java.util.List;

public final class EditorAccess {

	private EditorAccess() {}

	public static boolean canOpen() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null) return false;
		if (mc.hasSingleplayerServer()) return true;
		return mc.player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
	}

	public static boolean isRemoteWorld() {
		return Minecraft.getInstance().getSingleplayerServer() == null;
	}

	public static boolean canSendDatapack() {
		return PlatformNetwork.canSendToServer(DatapackOpPayload.TYPE);
	}

	public static void denyOpen() {
		var player = Minecraft.getInstance().player;
		if (player != null) {
			player.displayClientMessage(Component.translatable("screen.bj_mapedit.need_op"), true);
		}
	}

	public static void sendCommands(List<String> commands) {
		for (String command : commands) {
			if (command.length() > 32767) {
				tooLarge();
				return;
			}
		}
		PlatformNetwork.sendToServer(new ExecuteCommandsPayload(commands));
	}
	public static void tooLarge() {
		var player = Minecraft.getInstance().player;
		if (player != null) {
			player.displayClientMessage(Component.translatable("screen.bj_mapedit.too_large"), true);
		}
	}
	public static void needMod() {
		var player = Minecraft.getInstance().player;
		if (player != null) {
			player.displayClientMessage(Component.translatable("screen.bj_mapedit.need_mod"), true);
		}
	}
}
