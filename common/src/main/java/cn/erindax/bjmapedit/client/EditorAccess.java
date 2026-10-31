package cn.erindax.bjmapedit.client;

import cn.erindax.bjmapedit.networking.payload.DatapackOpPayload;
import cn.erindax.bjmapedit.networking.payload.ExecuteCommandsPayload;
import cn.erindax.bjmapedit.networking.payload.GiveItemPayload;
import cn.erindax.bjmapedit.networking.payload.ModPayload;
import cn.erindax.bjmapedit.PlatformNetwork;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
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
		notify("screen.bj_mapedit.need_op");
	}

	public static String commandProblem() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) return "screen.bj_mapedit.need_op";
		if (!PlatformNetwork.canSendToServer(ExecuteCommandsPayload.ID)) return "screen.bj_mapedit.need_mod";
		if (!mc.player.hasPermissions(2)) {
			return mc.hasSingleplayerServer() ? "screen.bj_mapedit.need_cheats" : "screen.bj_mapedit.need_op";
		}
		return null;
	}

	public static String giveProblem() {
		if (Minecraft.getInstance().player == null) return "screen.bj_mapedit.need_op";
		if (!PlatformNetwork.canSendToServer(GiveItemPayload.ID)) return "screen.bj_mapedit.need_mod";
		return canOpen() ? null : "screen.bj_mapedit.need_op";
	}

	public static boolean sendCommands(List<String> commands) {
		String problem = commandProblem();
		if (problem != null) {
			notify(problem);
			return false;
		}
		for (String command : commands) {
			if (command.length() > 32767) {
				tooLarge();
				return false;
			}
		}
		PlatformNetwork.sendToServer(new ExecuteCommandsPayload(commands));
		return true;
	}

	public static boolean giveItem(ItemStack stack) {
		if (stack == null || stack.isEmpty()) return false;
		String problem = giveProblem();
		if (problem != null) {
			notify(problem);
			return false;
		}
		PlatformNetwork.sendToServer(new GiveItemPayload(stack));
		return true;
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
		notify("screen.bj_mapedit.too_large");
	}

	public static void needMod() {
		notify("screen.bj_mapedit.need_mod");
	}

	private static void notify(String key) {
		var player = Minecraft.getInstance().player;
		if (player != null) {
			player.displayClientMessage(Component.translatable(key), true);
		}
	}
}
