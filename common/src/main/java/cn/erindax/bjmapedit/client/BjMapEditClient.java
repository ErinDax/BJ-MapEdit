package cn.erindax.bjmapedit.client;

import net.minecraft.ChatFormatting;
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
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
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
import java.util.List;

public final class BjMapEditClient {

	private static final String CATEGORY = "key.categories.bj_mapedit.general";

	public static final KeyMapping OPEN_ITEM_EDITOR = key("key.bj_mapedit.open", GLFW.GLFW_KEY_KP_1);
	public static final KeyMapping OPEN_BOOK_EDITOR = key("key.bj_mapedit.open_book_editor", GLFW.GLFW_KEY_KP_2);
	public static final KeyMapping OPEN_GAMERULE_EDITOR = key("key.bj_mapedit.open_gamerule_editor", GLFW.GLFW_KEY_KP_3);
	public static final KeyMapping OPEN_FRAME_EDITOR = key("key.bj_mapedit.open_entity_editor", GLFW.GLFW_KEY_KP_4);
	public static final KeyMapping OPEN_ENTITY_EDITOR = key("key.bj_mapedit.open_entity_editor2", GLFW.GLFW_KEY_KP_5);
	public static final KeyMapping OPEN_VILLAGER_EDITOR = key("key.bj_mapedit.open_villager_editor", GLFW.GLFW_KEY_KP_6);
	public static final KeyMapping OPEN_DATAPACK_EDITOR = key("key.bj_mapedit.open_datapack_editor", GLFW.GLFW_KEY_KP_7);
	public static final KeyMapping OPEN_RECIPE_EDITOR = key("key.bj_mapedit.open_recipe_editor", GLFW.GLFW_KEY_KP_8);
	public static final KeyMapping OPEN_ADVANCEMENT_EDITOR = key("key.bj_mapedit.open_advancement_editor", GLFW.GLFW_KEY_KP_9);

	public static final List<KeyMapping> KEY_MAPPINGS = List.of(
		OPEN_ITEM_EDITOR,
		OPEN_BOOK_EDITOR,
		OPEN_GAMERULE_EDITOR,
		OPEN_FRAME_EDITOR,
		OPEN_ENTITY_EDITOR,
		OPEN_VILLAGER_EDITOR,
		OPEN_DATAPACK_EDITOR,
		OPEN_RECIPE_EDITOR,
		OPEN_ADVANCEMENT_EDITOR
	);

	private BjMapEditClient() {
	}

	private static KeyMapping key(String name, int keyCode) {
		return new KeyMapping(name, keyCode, CATEGORY);
	}

	public static void tick() {
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
	}

	public static <S> LiteralArgumentBuilder<S> command() {
		return LiteralArgumentBuilder.<S>literal("bjedit")
			.then(LiteralArgumentBuilder.<S>literal("datapack")
				.then(LiteralArgumentBuilder.<S>literal("create")
					.then(RequiredArgumentBuilder.<S, String>argument("name", StringArgumentType.word())
						.executes(ctx -> {
							createDatapack(StringArgumentType.getString(ctx, "name"));
							return 1;
						})
					)
				)
			);
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
					mc.player.displayClientMessage(Component.translatable("screen.bj_mapedit.datapack_exists", name).withStyle(ChatFormatting.RED), false);
				}
				return;
			}
			Files.createDirectories(packDir);
			String mcmeta = "{\n  \"pack\": {\n    \"pack_format\": 61,\n    \"description\": \"My Datapack\"\n  }\n}\n";
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
				mc.player.displayClientMessage(Component.translatable("screen.bj_mapedit.datapack_created", name).withStyle(ChatFormatting.GREEN), false);
			}
		} catch (IOException e) {
			if (mc.player != null) {
				mc.player.displayClientMessage(Component.translatable("screen.bj_mapedit.datapack_create_failed", e.getMessage()).withStyle(ChatFormatting.RED), false);
			}
		}
	}
}
