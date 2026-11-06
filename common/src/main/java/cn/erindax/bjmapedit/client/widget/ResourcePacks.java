package cn.erindax.bjmapedit.client.widget;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public final class ResourcePacks {
	private static final Set<String> TEXT_EXTS = Set.of(
		"json", "mcmeta", "mcfunction", "txt", "properties", "lang", "fsh", "vsh", "glsl", "md", "jem", "jpm"
	);

	private ResourcePacks() {}

	public static Path root() {
		return Minecraft.getInstance().getResourcePackDirectory();
	}

	public static String packMcmeta() {
		return "{\n  \"pack\": {\n    \"pack_format\": 34,\n    \"description\": \"BJ-MapEdit\"\n  }\n}\n";
	}

	public static List<String> folderPacks() {
		List<String> out = new ArrayList<>();
		Path root = root();
		if (!Files.isDirectory(root)) return out;
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(root)) {
			for (Path p : stream) {
				if (Files.isDirectory(p) && Files.isRegularFile(p.resolve("pack.mcmeta"))) out.add(p.getFileName().toString());
			}
		} catch (IOException ignored) {}
		out.sort(String.CASE_INSENSITIVE_ORDER);
		return out;
	}

	public static String namespace(String folder) {
		String s = folder == null ? "" : folder.toLowerCase(Locale.ROOT);
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			boolean ok = c >= 'a' && c <= 'z' || c >= '0' && c <= '9' || c == '_' || c == '-' || c == '.';
			sb.append(ok ? c : '_');
		}
		String ns = sb.toString();
		return ns.isEmpty() || ns.equals("minecraft") ? "custom" : ns;
	}

	public static String resourceName(String raw) {
		String s = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT).replace('\\', '/');
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			boolean ok = c >= 'a' && c <= 'z' || c >= '0' && c <= '9' || c == '_' || c == '-' || c == '.' || c == '/';
			sb.append(ok ? c : '_');
		}
		List<String> parts = new ArrayList<>();
		for (String part : sb.toString().split("/")) {
			if (!part.isEmpty() && !part.equals(".") && !part.equals("..")) parts.add(part);
		}
		return String.join("/", parts);
	}

	public static String createPack(String baseName) throws IOException {
		Path root = root();
		Files.createDirectories(root);
		String base = resourceName(baseName).replace('/', '_');
		if (base.isEmpty()) base = "new_resourcepack";
		String name = base;
		for (int i = 1; Files.exists(root.resolve(name)) && i < 1000; i++) name = base + "_" + i;
		Path pack = root.resolve(name);
		Files.createDirectories(pack);
		Files.writeString(pack.resolve("pack.mcmeta"), packMcmeta(), StandardCharsets.UTF_8);
		Path assets = pack.resolve("assets").resolve(namespace(name));
		Files.createDirectories(assets.resolve("models").resolve("item"));
		Files.createDirectories(assets.resolve("textures").resolve("item"));
		return name;
	}

	public static boolean isText(Path path) {
		if (path == null) return false;
		String name = path.getFileName().toString();
		int dot = name.lastIndexOf('.');
		return dot < 0 || TEXT_EXTS.contains(name.substring(dot + 1).toLowerCase(Locale.ROOT));
	}

	public static String packOf(Path root, Path path) {
		if (root == null || path == null) return null;
		try {
			Path rel = root.toAbsolutePath().normalize().relativize(path.toAbsolutePath().normalize());
			if (rel.toString().isEmpty() || rel.startsWith("..")) return null;
			return rel.getName(0).toString();
		} catch (Exception e) {
			return null;
		}
	}

	public static void enableAndReload(String folder) {
		Minecraft mc = Minecraft.getInstance();
		var repo = mc.getResourcePackRepository();
		repo.reload();
		String id = folder == null ? null : "file/" + folder;
		boolean added = id != null && repo.getAvailableIds().contains(id) && !repo.getSelectedIds().contains(id) && repo.addPack(id);
		if (added) {
			mc.options.updateResourcePacks(repo);
		} else {
			mc.reloadResourcePacks();
		}
	}

	public static void notify(Component message) {
		var player = Minecraft.getInstance().player;
		if (player != null) player.displayClientMessage(message, true);
	}

	public static void chooseFile(String title, String pattern, String description, Consumer<Path> onChosen) {
		Minecraft mc = Minecraft.getInstance();
		if (Util.getPlatform() == Util.OS.OSX) {
			Path picked = openDialog(title, pattern, description);
			if (picked != null) onChosen.accept(picked);
			return;
		}
		CompletableFuture.supplyAsync(() -> openDialog(title, pattern, description))
			.thenAcceptAsync(picked -> {
				if (picked != null) onChosen.accept(picked);
			}, mc);
	}

	private static Path openDialog(String title, String pattern, String description) {
		try (MemoryStack stack = MemoryStack.stackPush()) {
			PointerBuffer filters = stack.mallocPointer(1);
			filters.put(stack.UTF8(pattern));
			filters.flip();
			String picked = TinyFileDialogs.tinyfd_openFileDialog(title, "", filters, description, false);
			return picked == null || picked.isBlank() ? null : Path.of(picked);
		} catch (Throwable t) {
			return null;
		}
	}
}
