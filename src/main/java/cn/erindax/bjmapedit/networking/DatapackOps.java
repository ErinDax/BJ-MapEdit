package cn.erindax.bjmapedit.networking;

import cn.erindax.bjmapedit.networking.payload.DatapackOpPayload;
import cn.erindax.bjmapedit.networking.payload.DatapackOpResultPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public final class DatapackOps {

	public static final int LIST = 0;
	public static final int READ = 1;
	public static final int WRITE = 2;
	public static final int MKDIR = 3;
	public static final int CREATE_FILE = 4;
	public static final int DELETE = 5;
	public static final int RENAME = 6;
	public static final int MOVE = 7;
	public static final int CREATE_PACK = 8;
	public static final int RELOAD = 9;

	public static final int KIND_TREE = 0;
	public static final int KIND_FILE = 1;
	public static final int KIND_OK = 2;
	public static final int KIND_ERR = 3;

	public static final int MAX_TEXT = 262144;
	private static final int MAX_TREE = 4000;

	private DatapackOps() {}

	public static void handle(ServerPlayer player, DatapackOpPayload payload) {
		if (!EditorPerms.canEdit(player)) {
			reply(player, KIND_ERR, "", "screen.bj_mapedit.need_op");
			return;
		}
		MinecraftServer server = player.getServer();
		if (server == null) return;
		Path root = server.getWorldPath(LevelResource.DATAPACK_DIR);
		try {
			Files.createDirectories(root);
			switch (payload.op()) {
				case LIST -> reply(player, KIND_TREE, "", listTree(root));
				case READ -> readFile(player, root, payload.path());
				case WRITE -> writeFile(player, server, root, payload.path(), payload.extra());
				case MKDIR -> mkdir(player, root, payload.path());
				case CREATE_FILE -> createFile(player, root, payload.path());
				case DELETE -> deletePath(player, server, root, payload.path());
				case RENAME -> renamePath(player, server, root, payload.path(), payload.extra());
				case MOVE -> movePath(player, server, root, payload.path(), payload.extra());
				case CREATE_PACK -> createPack(player, root, payload.path());
				case RELOAD -> reload(player, server, root, payload.extra());
				default -> reply(player, KIND_ERR, "", "screen.bj_mapedit.recipe_export_fail");
			}
		} catch (IOException e) {
			reply(player, KIND_ERR, "", "screen.bj_mapedit.recipe_export_fail");
		}
	}

	private static void readFile(ServerPlayer player, Path root, String rel) throws IOException {
		Path file = resolveIn(root, rel);
		if (file == null || !Files.isRegularFile(file)) {
			reply(player, KIND_ERR, rel, "screen.bj_mapedit.recipe_export_fail");
			return;
		}
		String content = Files.readString(file, StandardCharsets.UTF_8);
		if (content.length() > MAX_TEXT) content = content.substring(0, MAX_TEXT);
		reply(player, KIND_FILE, rel(root, file), content);
	}

	private static void writeFile(ServerPlayer player, MinecraftServer server, Path root, String rel, String extra) throws IOException {
		Path file = resolveIn(root, rel);
		if (file == null || Files.isDirectory(file)) {
			reply(player, KIND_ERR, rel, "screen.bj_mapedit.recipe_export_fail");
			return;
		}
		String content = extra == null ? "" : extra;
		if (content.length() > MAX_TEXT) content = content.substring(0, MAX_TEXT);
		Files.createDirectories(file.getParent());
		ensureBjmapeditMeta(root, rel);
		Files.writeString(file, content, StandardCharsets.UTF_8);
		reply(player, KIND_OK, rel(root, file), "");
		reply(player, KIND_TREE, "", listTree(root));
	}

	private static void mkdir(ServerPlayer player, Path root, String rel) throws IOException {
		Path dir = available(root, rel, true);
		if (dir == null) {
			reply(player, KIND_ERR, rel, "screen.bj_mapedit.recipe_export_fail");
			return;
		}
		Files.createDirectories(dir);
		reply(player, KIND_OK, rel(root, dir), "");
		reply(player, KIND_TREE, "", listTree(root));
	}

	private static void createFile(ServerPlayer player, Path root, String rel) throws IOException {
		Path file = available(root, rel, false);
		if (file == null) {
			reply(player, KIND_ERR, rel, "screen.bj_mapedit.recipe_export_fail");
			return;
		}
		Files.createDirectories(file.getParent());
		if (!Files.exists(file)) Files.createFile(file);
		reply(player, KIND_OK, rel(root, file), "");
		reply(player, KIND_TREE, "", listTree(root));
	}

	private static void deletePath(ServerPlayer player, MinecraftServer server, Path root, String rel) throws IOException {
		Path target = resolveIn(root, rel);
		if (target == null || target.equals(root.toAbsolutePath().normalize()) || !Files.exists(target)) {
			reply(player, KIND_ERR, rel, "screen.bj_mapedit.recipe_export_fail");
			return;
		}
		deleteRecursive(target);
		reply(player, KIND_OK, "", "screen.bj_mapedit.deleted");
		reply(player, KIND_TREE, "", listTree(root));
		reloadQuiet(server, root, "");
	}

	private static void renamePath(ServerPlayer player, MinecraftServer server, Path root, String fromRel, String toRel) throws IOException {
		Path from = resolveIn(root, fromRel);
		Path to = resolveIn(root, toRel);
		if (from == null || to == null || !Files.exists(from) || from.equals(root.toAbsolutePath().normalize())) {
			reply(player, KIND_ERR, fromRel, "screen.bj_mapedit.recipe_export_fail");
			return;
		}
		Files.createDirectories(to.getParent());
		Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
		reply(player, KIND_OK, rel(root, to), "");
		reply(player, KIND_TREE, "", listTree(root));
		reloadQuiet(server, root, "");
	}

	private static void movePath(ServerPlayer player, MinecraftServer server, Path root, String fromRel, String destRel) throws IOException {
		Path from = resolveIn(root, fromRel);
		Path dest = destRel == null || destRel.isEmpty() ? root.toAbsolutePath().normalize() : resolveIn(root, destRel);
		if (from == null || dest == null || !Files.exists(from) || !Files.isDirectory(dest)) {
			reply(player, KIND_ERR, fromRel, "screen.bj_mapedit.recipe_export_fail");
			return;
		}
		if (Files.isDirectory(from) && dest.startsWith(from)) {
			reply(player, KIND_ERR, fromRel, "screen.bj_mapedit.recipe_export_fail");
			return;
		}
		Path target = dest.resolve(from.getFileName().toString());
		if (Files.exists(target)) {
			target = available(root, rel(root, target), Files.isDirectory(from));
			if (target == null) {
				reply(player, KIND_ERR, fromRel, "screen.bj_mapedit.recipe_export_fail");
				return;
			}
		}
		Files.move(from, target);
		reply(player, KIND_OK, rel(root, target), "");
		reply(player, KIND_TREE, "", listTree(root));
		reloadQuiet(server, root, "");
	}

	private static void createPack(ServerPlayer player, Path root, String name) throws IOException {
		String ns = safeSegment(name);
		if (ns.isEmpty()) ns = "new_datapack";
		Path pack = available(root, ns, true);
		if (pack == null) {
			reply(player, KIND_ERR, name, "screen.bj_mapedit.recipe_export_fail");
			return;
		}
		ns = pack.getFileName().toString();
		Files.createDirectories(pack);
		String mcmeta = "{\n  \"pack\": {\n    \"pack_format\": 48,\n    \"description\": \"My Datapack\"\n  }\n}\n";
		Files.writeString(pack.resolve("pack.mcmeta"), mcmeta, StandardCharsets.UTF_8);
		Path dataDir = pack.resolve("data").resolve(ns);
		Files.createDirectories(dataDir.resolve("function"));
		Files.createDirectories(dataDir.resolve("advancement"));
		Files.createDirectories(dataDir.resolve("recipe"));
		Files.createDirectories(dataDir.resolve("loot_table"));
		Path tags = pack.resolve("data").resolve("minecraft").resolve("tags").resolve("function");
		Files.createDirectories(tags);
		Files.writeString(tags.resolve("load.json"), "{\n  \"values\": [\n    \"" + ns + ":load\"\n  ]\n}\n", StandardCharsets.UTF_8);
		Files.writeString(tags.resolve("tick.json"), "{\n  \"values\": [\n    \"" + ns + ":tick\"\n  ]\n}\n", StandardCharsets.UTF_8);
		Files.writeString(dataDir.resolve("function").resolve("load.mcfunction"), "# Load function\n", StandardCharsets.UTF_8);
		Files.writeString(dataDir.resolve("function").resolve("tick.mcfunction"), "# Tick function\n", StandardCharsets.UTF_8);
		reply(player, KIND_OK, rel(root, pack), "");
		reply(player, KIND_TREE, "", listTree(root));
	}

	private static void reload(ServerPlayer player, MinecraftServer server, Path root, String enablePack) throws IOException {
		reloadQuiet(server, root, enablePack);
		reply(player, KIND_OK, enablePack == null ? "" : enablePack, "screen.bj_mapedit.applied");
		reply(player, KIND_TREE, "", listTree(root));
	}

	private static void reloadQuiet(MinecraftServer server, Path root, String enablePack) {
		server.execute(() -> {
			var repo = server.getPackRepository();
			repo.reload();
			Collection<String> selected = new ArrayList<>(repo.getSelectedIds());
			selected.removeIf(id -> id.startsWith("file/")
				&& !Files.isDirectory(root.resolve(id.substring("file/".length()))));
			String pack = enablePack == null ? "" : safeSegment(enablePack);
			if (!pack.isEmpty()) {
				String id = "file/" + pack;
				if (Files.isDirectory(root.resolve(pack)) && !selected.contains(id)) selected.add(id);
			}
			if (Files.isDirectory(root.resolve("bjmapedit")) && !selected.contains("file/bjmapedit")) {
				selected.add("file/bjmapedit");
			}
			repo.setSelected(selected);
			server.reloadResources(repo.getSelectedIds());
		});
	}

	private static void ensureBjmapeditMeta(Path root, String rel) throws IOException {
		if (rel == null || !rel.replace('\\', '/').startsWith("bjmapedit/")) return;
		Path meta = root.resolve("bjmapedit").resolve("pack.mcmeta");
		if (Files.isRegularFile(meta)) return;
		Files.createDirectories(meta.getParent());
		Files.writeString(meta,
			"{\n  \"pack\": {\n    \"pack_format\": 48,\n    \"description\": \"BJ-MapEdit\"\n  }\n}\n",
			StandardCharsets.UTF_8);
	}

	private static String listTree(Path root) throws IOException {
		List<String> lines = new ArrayList<>();
		walk(root, root, 0, lines);
		lines.sort(Comparator.naturalOrder());
		StringBuilder sb = new StringBuilder();
		for (String line : lines) {
			if (sb.length() > 0) sb.append('\n');
			sb.append(line);
			if (sb.length() > MAX_TEXT - 64) break;
		}
		return sb.toString();
	}

	private static void walk(Path root, Path current, int depth, List<String> out) throws IOException {
		if (out.size() >= MAX_TREE || depth > 10 || !Files.isDirectory(current)) return;
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(current)) {
			for (Path child : stream) {
				if (out.size() >= MAX_TREE) return;
				String rel = rel(root, child);
				if (rel.isEmpty()) continue;
				if (Files.isDirectory(child)) {
					out.add("D\t" + rel);
					walk(root, child, depth + 1, out);
				} else if (Files.isRegularFile(child)) {
					out.add("F\t" + rel);
				}
			}
		}
	}

	private static Path available(Path root, String rel, boolean dir) {
		Path first = resolveIn(root, rel);
		if (first == null) return null;
		if (!Files.exists(first)) return first;
		Path parent = first.getParent();
		String name = first.getFileName().toString();
		String base = name;
		String ext = "";
		if (!dir) {
			int dot = name.lastIndexOf('.');
			if (dot > 0) {
				base = name.substring(0, dot);
				ext = name.substring(dot);
			}
		}
		for (int i = 1; i < 1000; i++) {
			Path next = parent.resolve(base + "_" + i + ext);
			if (!next.startsWith(root.toAbsolutePath().normalize())) return null;
			if (!Files.exists(next)) return next;
		}
		return null;
	}

	public static Path resolveIn(Path root, String relative) {
		Path rootN = root.toAbsolutePath().normalize();
		if (relative == null || relative.isBlank()) return rootN;
		String rel = relative.replace('\\', '/');
		if (rel.contains("..") || rel.startsWith("/") || rel.contains("\0") || rel.length() > 400) return null;
		Path resolved = rootN.resolve(rel).normalize();
		if (!resolved.startsWith(rootN)) return null;
		return resolved;
	}

	public static String rel(Path root, Path path) {
		try {
			return root.toAbsolutePath().normalize().relativize(path.toAbsolutePath().normalize()).toString().replace('\\', '/');
		} catch (Exception e) {
			return "";
		}
	}

	private static String safeSegment(String name) {
		if (name == null) return "";
		String s = name.trim().replace('\\', '/');
		int slash = s.lastIndexOf('/');
		if (slash >= 0) s = s.substring(slash + 1);
		s = s.replaceAll("[^a-zA-Z0-9._-]", "_");
		if (s.isEmpty() || s.equals(".") || s.equals("..")) return "";
		return s;
	}

	private static void deleteRecursive(Path path) throws IOException {
		if (Files.isDirectory(path)) {
			try (DirectoryStream<Path> ds = Files.newDirectoryStream(path)) {
				for (Path child : ds) {
					deleteRecursive(child);
				}
			}
		}
		Files.deleteIfExists(path);
	}

	private static void reply(ServerPlayer player, int kind, String path, String extra) {
		ServerPlayNetworking.send(player, new DatapackOpResultPayload(kind, path == null ? "" : path, extra == null ? "" : extra));
	}
}
