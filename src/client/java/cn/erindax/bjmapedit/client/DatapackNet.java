package cn.erindax.bjmapedit.client;

import cn.erindax.bjmapedit.client.screen.AdvancementEditorScreen;
import cn.erindax.bjmapedit.client.screen.DatapackEditorScreen;
import cn.erindax.bjmapedit.client.screen.RecipeEditorScreen;
import cn.erindax.bjmapedit.networking.payload.DatapackOpPayload;
import cn.erindax.bjmapedit.networking.payload.DatapackOpResultPayload;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.nio.file.Path;

@Environment(EnvType.CLIENT)
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
		ClientPlayNetworking.send(new DatapackOpPayload(op, path == null ? "" : path, extra == null ? "" : extra));
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
		}
		if (screen instanceof RecipeEditorScreen recipe) {
			recipe.handleNet(payload);
		}
		if (screen instanceof AdvancementEditorScreen advancement) {
			advancement.handleNet(payload);
		}
	}

	public static void register() {
		ClientPlayNetworking.registerGlobalReceiver(DatapackOpResultPayload.TYPE, (payload, context) ->
			context.client().execute(() -> dispatch(payload)));
	}
}
