package cn.erindax.bjmapedit.client;

import cn.erindax.bjmapedit.client.screen.AdvancementEditorScreen;
import cn.erindax.bjmapedit.client.screen.AjMobScreen;
import cn.erindax.bjmapedit.client.screen.ClockEditorScreen;
import cn.erindax.bjmapedit.client.screen.DatapackEditorScreen;
import cn.erindax.bjmapedit.client.screen.GameRuleEditorScreen;
import cn.erindax.bjmapedit.client.screen.PropEditorScreen;
import cn.erindax.bjmapedit.client.screen.RecipeEditorScreen;
import cn.erindax.bjmapedit.client.screen.ShopEditorScreen;
import cn.erindax.bjmapedit.networking.DatapackOps;
import cn.erindax.bjmapedit.networking.payload.DatapackOpPayload;
import cn.erindax.bjmapedit.networking.payload.DatapackOpResultPayload;
import cn.erindax.bjmapedit.PlatformNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;

public final class DatapackNet {

	public static Path virtualRoot() {
		return Path.of("bjmapedit-remote", "datapacks");
	}

	private DatapackNet() {}

	public static void send(int op, String path, String extra) {
		if (!EditorAccess.canSendDatapack()) {
			EditorAccess.needMod();
			return;
		}
		DatapackOpPayload payload = new DatapackOpPayload(op, path == null ? "" : path, extra == null ? "" : extra);
		if (!EditorAccess.fitsServerbound(payload)) {
			EditorAccess.tooLarge();
			return;
		}
		PlatformNetwork.sendToServer(payload);
	}

	public static boolean fits(String text) {
		return text == null || text.length() <= DatapackOps.MAX_TEXT;
	}

	public static String rel(Path root, Path path) {
		if (root == null || path == null) return "";
		try {
			return root.toAbsolutePath().normalize().relativize(path.toAbsolutePath().normalize()).toString().replace('\\', '/');
		} catch (Exception e) {
			return "";
		}
	}

	public static Path join(Path root, String rel) {
		Path p = root;
		if (rel == null || rel.isBlank()) return p;
		for (String part : rel.replace('\\', '/').split("/")) {
			if (!part.isEmpty()) p = p.resolve(part);
		}
		return p;
	}

	public static void dispatch(DatapackOpResultPayload payload) {
		Screen screen = Minecraft.getInstance().screen;
		if (screen instanceof DatapackEditorScreen datapack) {
			datapack.handleNet(payload);
		} else if (screen instanceof RecipeEditorScreen recipe) {
			recipe.handleNet(payload);
		} else if (screen instanceof AdvancementEditorScreen advancement) {
			advancement.handleNet(payload);
		} else if (screen instanceof GameRuleEditorScreen rules) {
			rules.handleNet(payload);
		} else if (screen instanceof ShopEditorScreen shop) {
			shop.handleNet(payload);
		} else if (screen instanceof AjMobScreen aj) {
			aj.handleNet(payload);
		} else if (screen instanceof ClockEditorScreen clock) {
			clock.handleNet(payload);
		} else if (screen instanceof PropEditorScreen prop) {
			prop.handleNet(payload);
		} else {
			notifyUnhandled(payload);
		}
	}

	private static void notifyUnhandled(DatapackOpResultPayload payload) {
		var player = Minecraft.getInstance().player;
		if (player == null || payload == null) return;
		if (payload.kind() != DatapackOps.KIND_OK && payload.kind() != DatapackOps.KIND_ERR) return;
		String extra = payload.extra();
		if (extra == null || extra.isEmpty()) return;
		if (DatapackOps.PACK_CREATED.equals(extra)) {
			player.displayClientMessage(Component.translatable("screen.bj_mapedit.datapack_created", payload.path()).withStyle(ChatFormatting.GREEN), false);
			return;
		}
		player.displayClientMessage(Component.translatable(extra), true);
	}
}
