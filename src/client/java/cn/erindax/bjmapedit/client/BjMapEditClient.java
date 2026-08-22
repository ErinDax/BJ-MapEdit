package cn.erindax.bjmapedit.client;

import cn.erindax.bjmapedit.client.screen.BookEditorScreen;
import cn.erindax.bjmapedit.client.screen.DatapackEditorScreen;
import cn.erindax.bjmapedit.client.screen.EntityEditorScreen;
import cn.erindax.bjmapedit.client.screen.FrameStandEditorScreen;
import cn.erindax.bjmapedit.client.screen.GameRuleEditorScreen;
import cn.erindax.bjmapedit.client.screen.ItemEditorScreen;
import cn.erindax.bjmapedit.client.screen.AdvancementEditorScreen;
import cn.erindax.bjmapedit.client.screen.RecipeEditorScreen;
import cn.erindax.bjmapedit.client.screen.VillagerEditorScreen;
import cn.erindax.bjmapedit.networking.DatapackOps;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.world.level.storage.LevelResource;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class BjMapEditClient implements ClientModInitializer {

	public static KeyMapping OPEN_ITEM_EDITOR;
	public static KeyMapping OPEN_BOOK_EDITOR;
	public static KeyMapping OPEN_GAMERULE_EDITOR;
	public static KeyMapping OPEN_FRAME_EDITOR;
	public static KeyMapping OPEN_ENTITY_EDITOR;
	public static KeyMapping OPEN_VILLAGER_EDITOR;
	public static KeyMapping OPEN_DATAPACK_EDITOR;
	public static KeyMapping OPEN_RECIPE_EDITOR;
	public static KeyMapping OPEN_ADVANCEMENT_EDITOR;

	@Override
	public void onInitializeClient() {
		DatapackNet.register();

		OPEN_ITEM_EDITOR = KeyBindingHelper.registerKeyBinding(new KeyMapping(
			"key.bj_mapedit.open",
			GLFW.GLFW_KEY_KP_1,
			"key.categories.bj_mapedit.general"
		));

		OPEN_BOOK_EDITOR = KeyBindingHelper.registerKeyBinding(new KeyMapping(
			"key.bj_mapedit.open_book_editor",
			GLFW.GLFW_KEY_KP_2,
			"key.categories.bj_mapedit.general"
		));

		OPEN_GAMERULE_EDITOR = KeyBindingHelper.registerKeyBinding(new KeyMapping(
			"key.bj_mapedit.open_gamerule_editor",
			GLFW.GLFW_KEY_KP_3,
			"key.categories.bj_mapedit.general"
		));

		OPEN_FRAME_EDITOR = KeyBindingHelper.registerKeyBinding(new KeyMapping(
			"key.bj_mapedit.open_entity_editor",
			GLFW.GLFW_KEY_KP_4,
			"key.categories.bj_mapedit.general"
		));

		OPEN_ENTITY_EDITOR = KeyBindingHelper.registerKeyBinding(new KeyMapping(
			"key.bj_mapedit.open_entity_editor2",
			GLFW.GLFW_KEY_KP_5,
			"key.categories.bj_mapedit.general"
		));

		OPEN_VILLAGER_EDITOR = KeyBindingHelper.registerKeyBinding(new KeyMapping(
			"key.bj_mapedit.open_villager_editor",
			GLFW.GLFW_KEY_KP_6,
			"key.categories.bj_mapedit.general"
		));

		OPEN_DATAPACK_EDITOR = KeyBindingHelper.registerKeyBinding(new KeyMapping(
			"key.bj_mapedit.open_datapack_editor",
			GLFW.GLFW_KEY_KP_7,
			"key.categories.bj_mapedit.general"
		));

		OPEN_RECIPE_EDITOR = KeyBindingHelper.registerKeyBinding(new KeyMapping(
			"key.bj_mapedit.open_recipe_editor",
			GLFW.GLFW_KEY_KP_8,
			"key.categories.bj_mapedit.general"
		));

		OPEN_ADVANCEMENT_EDITOR = KeyBindingHelper.registerKeyBinding(new KeyMapping(
			"key.bj_mapedit.open_advancement_editor",
			GLFW.GLFW_KEY_KP_9,
			"key.categories.bj_mapedit.general"
		));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (OPEN_ITEM_EDITOR.consumeClick()) {
				openScreen(new ItemEditorScreen());
			}
			while (OPEN_BOOK_EDITOR.consumeClick()) {
				openScreen(new BookEditorScreen());
			}
			while (OPEN_GAMERULE_EDITOR.consumeClick()) {
				openScreen(new GameRuleEditorScreen());
			}
			while (OPEN_FRAME_EDITOR.consumeClick()) {
				openScreen(new FrameStandEditorScreen());
			}
			while (OPEN_ENTITY_EDITOR.consumeClick()) {
				openScreen(new EntityEditorScreen());
			}
			while (OPEN_VILLAGER_EDITOR.consumeClick()) {
				openScreen(new VillagerEditorScreen());
			}
			while (OPEN_DATAPACK_EDITOR.consumeClick()) {
				openScreen(new DatapackEditorScreen());
			}
			while (OPEN_RECIPE_EDITOR.consumeClick()) {
				openScreen(new RecipeEditorScreen());
			}
			while (OPEN_ADVANCEMENT_EDITOR.consumeClick()) {
				openScreen(new AdvancementEditorScreen());
			}
		});

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			dispatcher.register(ClientCommandManager.literal("bjedit")
				.then(ClientCommandManager.literal("datapack")
					.then(ClientCommandManager.literal("create")
						.then(ClientCommandManager.argument("name", StringArgumentType.word())
							.executes(ctx -> {
								String name = StringArgumentType.getString(ctx, "name");
								createDatapack(name);
								return 1;
							})
						)
					)
				)
			);
		});
	}

	private static void openScreen(Screen screen) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null) return;
		if (!EditorAccess.canOpen()) {
			EditorAccess.denyOpen();
			return;
		}
		mc.setScreen(screen);
	}

	private static void createDatapack(String name) {
		Minecraft mc = Minecraft.getInstance();
		if (!EditorAccess.canOpen()) {
			EditorAccess.denyOpen();
			return;
		}
		IntegratedServer server = mc.getSingleplayerServer();
		if (server == null) {
			DatapackNet.send(DatapackOps.CREATE_PACK, name, "");
			return;
		}
		Path datapacksPath = server.getWorldPath(LevelResource.DATAPACK_DIR);
		Path packDir = datapacksPath.resolve(name);
		try {
			if (Files.exists(packDir)) {
				if (mc.player != null) {
					mc.player.displayClientMessage(Component.literal("§c[数据包编辑器] 数据包\"" + name + "\" 已存在"), false);
				}
				return;
			}
			Files.createDirectories(packDir);
			String mcmeta = "{\n  \"pack\": {\n    \"pack_format\": 48,\n    \"description\": \"My Datapack\"\n  }\n}\n";
			Files.writeString(packDir.resolve("pack.mcmeta"), mcmeta, StandardCharsets.UTF_8);
			Path dataDir = packDir.resolve("data").resolve(name);
			Files.createDirectories(dataDir.resolve("function"));
			Files.createDirectories(dataDir.resolve("advancement"));
			Files.createDirectories(dataDir.resolve("recipe"));
			Files.createDirectories(dataDir.resolve("loot_table"));
			Path minecraftTagsFunc = packDir.resolve("data").resolve("minecraft").resolve("tags").resolve("function");
			Files.createDirectories(minecraftTagsFunc);
			String loadJson = "{\n  \"values\": [\n    \"" + name + ":load\"\n  ]\n}\n";
			String tickJson = "{\n  \"values\": [\n    \"" + name + ":tick\"\n  ]\n}\n";
			Files.writeString(minecraftTagsFunc.resolve("load.json"), loadJson, StandardCharsets.UTF_8);
			Files.writeString(minecraftTagsFunc.resolve("tick.json"), tickJson, StandardCharsets.UTF_8);
			Files.writeString(dataDir.resolve("function").resolve("load.mcfunction"), "# Load function\n", StandardCharsets.UTF_8);
			Files.writeString(dataDir.resolve("function").resolve("tick.mcfunction"), "# Tick function\n", StandardCharsets.UTF_8);

			if (mc.player != null) {
				mc.player.displayClientMessage(Component.literal("§a[数据包编辑器] 数据包\"" + name + "\" 创建成功！"), false);
			}
		} catch (IOException e) {
			if (mc.player != null) {
				mc.player.displayClientMessage(Component.literal("§c[数据包编辑器] 创建失败: " + e.getMessage()), false);
			}
		}
	}
}
