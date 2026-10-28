package cn.erindax.bjmapedit.client;

import cn.erindax.bjmapedit.networking.payload.DatapackOpPayload;
import cn.erindax.bjmapedit.networking.payload.ExecuteCommandsPayload;
import cn.erindax.bjmapedit.networking.payload.ModPayload;
import cn.erindax.bjmapedit.PlatformNetwork;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import java.util.List;

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
		return PlatformNetwork.canSendToServer(DatapackOpPayload.ID);
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
	public static boolean fitsServerbound(ModPayload payload) {
		FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
		try {
			payload.write(buf);
			return Minecraft.getInstance().hasSingleplayerServer() || buf.readableBytes() <= 32000;
		} catch (RuntimeException e) {
			return false;
		} finally {
			buf.release();
		}
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
