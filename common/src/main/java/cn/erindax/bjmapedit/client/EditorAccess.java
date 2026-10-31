package cn.erindax.bjmapedit.client;

import net.minecraft.server.permissions.Permissions;
import cn.erindax.bjmapedit.networking.payload.DatapackOpPayload;
import cn.erindax.bjmapedit.networking.payload.ExecuteCommandsPayload;
import cn.erindax.bjmapedit.networking.payload.GiveItemPayload;
import cn.erindax.bjmapedit.PlatformNetwork;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import java.util.List;

public final class EditorAccess {

	private static final int MAX_SERVERBOUND = 2000000;

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
		notify("screen.bj_mapedit.need_op");
	}

	public static String commandProblem() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) return "screen.bj_mapedit.need_op";
		if (!PlatformNetwork.canSendToServer(ExecuteCommandsPayload.TYPE)) return "screen.bj_mapedit.need_mod";
		if (!mc.player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
			return mc.hasSingleplayerServer() ? "screen.bj_mapedit.need_cheats" : "screen.bj_mapedit.need_op";
		}
		return null;
	}

	public static String giveProblem() {
		if (Minecraft.getInstance().player == null) return "screen.bj_mapedit.need_op";
		if (!PlatformNetwork.canSendToServer(GiveItemPayload.TYPE)) return "screen.bj_mapedit.need_mod";
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
		ExecuteCommandsPayload payload = new ExecuteCommandsPayload(commands);
		if (!fits(ExecuteCommandsPayload.CODEC, payload)) {
			tooLarge();
			return false;
		}
		PlatformNetwork.sendToServer(payload);
		return true;
	}

	public static boolean giveItem(ItemStack stack) {
		if (stack == null || stack.isEmpty()) return false;
		String problem = giveProblem();
		if (problem != null) {
			notify(problem);
			return false;
		}
		GiveItemPayload payload = new GiveItemPayload(stack);
		if (!fits(GiveItemPayload.CODEC, payload)) {
			tooLarge();
			return false;
		}
		PlatformNetwork.sendToServer(payload);
		return true;
	}

	public static <T> boolean fits(StreamCodec<? super RegistryFriendlyByteBuf, T> codec, T payload) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.hasSingleplayerServer() || mc.level == null) return true;
		RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), mc.level.registryAccess());
		try {
			codec.encode(buf, payload);
			return buf.readableBytes() <= MAX_SERVERBOUND;
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
