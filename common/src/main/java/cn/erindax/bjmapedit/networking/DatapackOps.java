package cn.erindax.bjmapedit.networking;

import cn.erindax.bjmapedit.networking.payload.DatapackOpPayload;
import cn.erindax.bjmapedit.networking.payload.DatapackOpResultPayload;
import cn.erindax.bjmapedit.PlatformNetwork;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

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
	public static final int RULES = 10;

	public static final int KIND_TREE = 0;
	public static final int KIND_FILE = 1;
	public static final int KIND_OK = 2;
	public static final int KIND_ERR = 3;
	public static final int KIND_RULES = 4;

	public static final int MAX_TEXT = 262144;
	private static final int MAX_TREE = 4000;
	public static final String TREE_TRUNCATED = "truncated";
	public static final String PACK_CREATED = "screen.bj_mapedit.pack_created";

	private DatapackOps() {}

	public static void handle(ServerPlayer player, DatapackOpPayload payload) {
		if (!EditorPerms.canEdit(player)) {
			reply(player, KIND_ERR, payload.path(), "screen.bj_mapedit.need_op");
			return;
		}
		MinecraftServer server = player.getServer();
		if (server == null) return;
		Path root = server.getWorldPath(LevelResource.DATAPACK_DIR);
		try {
			Files.createDirectories(root);
			switch (payload.op()) {
				case LIST -> replyTree(player, root);
				case READ -> readFile(player, root, payload.path());
				case WRITE -> writeFile(player, server, root, payload.path(), payload.extra());
				case MKDIR -> mkdir(player, root, payload.path());
				case CREATE_FILE -> createFile(player, root, payload.path());
				case DELETE -> deletePath(player, server, root, payload.path());
				case RENAME -> renamePath(player, server, root, payload.path(), payload.extra());
				case MOVE -> movePath(player, server, root, payload.path(), payload.extra());
				case CREATE_PACK -> createPack(player, root, payload.path());
				case RELOAD -> reload(player, server, root, payload.path(), payload.extra());
				case RULES -> reply(player, KIND_RULES, "", ruleValues(server));
				default -> reply(player, KIND_ERR, payload.path(), "screen.bj_mapedit.file_op_failed");
			}
		} catch (CharacterCodingException e) {
			reply(player, KIND_ERR, payload.path(), "screen.bj_mapedit.rp_not_text");
		} catch (IOException | RuntimeException e) {
			reply(player, KIND_ERR, payload.path(), "screen.bj_mapedit.file_op_failed");
		}
	}

	private static String ruleValues(MinecraftServer server) {
		GameRules rules = server.getGameRules();
		StringBuilder sb = new StringBuilder();
		rules.visitGameRuleTypes(new GameRules.GameRuleTypeVisitor() {
			@Override
			public <T extends GameRules.Value<T>> void visit(GameRules.Key<T> key, GameRules.Type<T> type) {
				if (sb.length() > 0) sb.append('\n');
				sb.append(key.getId()).append('=').append(rules.getRule(key).serialize());
			}
		});
		return sb.toString();
	}

	private static void readFile(ServerPlayer player, Path root, String rel) throws IOException {
		Path file = resolveIn(root, rel);
		if (file == null || !Files.isRegularFile(file)) {
			reply(player, KIND_ERR, rel, "screen.bj_mapedit.file_op_failed");
			return;
		}
		String content = Files.size(file) > (long) MAX_TEXT * 4 ? null : Files.readString(file, StandardCharsets.UTF_8);
		if (content == null || content.length() > MAX_TEXT) {
			reply(player, KIND_ERR, rel, "screen.bj_mapedit.too_large");
			return;
		}
		reply(player, KIND_FILE, rel(root, file), content);
	}

	private static void writeFile(ServerPlayer player, MinecraftServer server, Path root, String rel, String extra) throws IOException {
		Path file = resolveIn(root, rel);
		if (file == null || Files.isDirectory(file)) {
			reply(player, KIND_ERR, rel, "screen.bj_mapedit.file_op_failed");
			return;
		}
		String content = extra == null ? "" : extra;
		if (content.length() > MAX_TEXT) {
			reply(player, KIND_ERR, rel, "screen.bj_mapedit.too_large");
			return;
		}
		boolean existed = Files.exists(file);
		Files.createDirectories(file.getParent());
		boolean metaCreated = ensureBjmapeditMeta(root, rel);
		Files.writeString(file, content, StandardCharsets.UTF_8);
		reply(player, KIND_OK, rel(root, file), "");
		if (!existed || metaCreated) replyTree(player, root);
	}

	private static void mkdir(ServerPlayer player, Path root, String rel) throws IOException {
		Path dir = available(root, rel, true);
		if (dir == null) {
			reply(player, KIND_ERR, rel, "screen.bj_mapedit.file_op_failed");
			return;
		}
		Files.createDirectories(dir);
		reply(player, KIND_OK, rel(root, dir), "");
		replyTree(player, root);
	}

	private static void createFile(ServerPlayer player, Path root, String rel) throws IOException {
		Path file = available(root, rel, false);
		if (file == null) {
			reply(player, KIND_ERR, rel, "screen.bj_mapedit.file_op_failed");
			return;
		}
		Files.createDirectories(file.getParent());
		if (!Files.exists(file)) Files.createFile(file);
		reply(player, KIND_OK, rel(root, file), "");
		replyTree(player, root);
	}

	private static void deletePath(ServerPlayer player, MinecraftServer server, Path root, String rel) throws IOException {
		Path target = resolveIn(root, rel);
		if (target == null || target.equals(root.toAbsolutePath().normalize()) || !Files.exists(target)) {
			reply(player, KIND_ERR, rel, "screen.bj_mapedit.file_op_failed");
			return;
		}
		deleteTree(root, target);
		reply(player, KIND_OK, "", "screen.bj_mapedit.deleted");
		replyTree(player, root);
		reloadQuiet(server, root, "");
	}

	private static void renamePath(ServerPlayer player, MinecraftServer server, Path root, String fromRel, String toRel) throws IOException {
		Path from = resolveIn(root, fromRel);
		Path to = resolveIn(root, toRel);
		if (from == null || to == null || !Files.exists(from) || from.equals(root.toAbsolutePath().normalize())) {
			reply(player, KIND_ERR, fromRel, "screen.bj_mapedit.file_op_failed");
			return;
		}
		if (Files.exists(to) && !Files.isSameFile(from, to)) {
			reply(player, KIND_ERR, fromRel, "screen.bj_mapedit.name_exists");
			return;
		}
		Files.createDirectories(to.getParent());
		Files.move(from, to);
		reply(player, KIND_OK, rel(root, to), "");
		replyTree(player, root);
		reloadQuiet(server, root, "");
	}

	private static void movePath(ServerPlayer player, MinecraftServer server, Path root, String fromRel, String destRel) throws IOException {
		Path from = resolveIn(root, fromRel);
		Path dest = destRel == null || destRel.isEmpty() ? root.toAbsolutePath().normalize() : resolveIn(root, destRel);
		if (from == null || dest == null || !Files.exists(from) || !Files.isDirectory(dest)) {
			reply(player, KIND_ERR, fromRel, "screen.bj_mapedit.file_op_failed");
			return;
		}
		if (Files.isDirectory(from) && dest.startsWith(from)) {
			reply(player, KIND_ERR, fromRel, "screen.bj_mapedit.file_op_failed");
			return;
		}
		Path target = dest.resolve(from.getFileName().toString());
		if (Files.exists(target)) {
			target = available(root, rel(root, target), Files.isDirectory(from));
			if (target == null) {
				reply(player, KIND_ERR, fromRel, "screen.bj_mapedit.file_op_failed");
				return;
			}
		}
		Files.move(from, target);
		reply(player, KIND_OK, rel(root, target), "");
		replyTree(player, root);
		reloadQuiet(server, root, "");
	}

	private static void createPack(ServerPlayer player, Path root, String name) throws IOException {
		String ns = packName(name);
		if (ns.isEmpty()) ns = "new_datapack";
		Path pack = available(root, ns, true);
		if (pack == null) {
			reply(player, KIND_ERR, name, "screen.bj_mapedit.file_op_failed");
			return;
		}
		ns = pack.getFileName().toString();
		Files.createDirectories(pack);
		String mcmeta = "{\n  \"pack\": {\n    \"pack_format\": 61,\n    \"description\": \"My Datapack\"\n  }\n}\n";
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
		reply(player, KIND_OK, rel(root, pack), PACK_CREATED);
		replyTree(player, root);
	}

	private static void reload(ServerPlayer player, MinecraftServer server, Path root, String requestPath, String enablePack) {
		String path = requestPath == null ? "" : requestPath;
		reloadQuiet(server, root, enablePack).whenComplete((v, e) -> server.execute(() -> {
			if (e == null) reply(player, KIND_OK, path, "screen.bj_mapedit.applied");
			else reply(player, KIND_ERR, path, "screen.bj_mapedit.reload_failed");
		}));
	}

	private static CompletableFuture<Void> reloadQuiet(MinecraftServer server, Path root, String enablePack) {
		CompletableFuture<Void> done = new CompletableFuture<>();
		server.execute(() -> {
			try {
				var repo = server.getPackRepository();
				repo.reload();
				String pack = enablePack == null ? "" : safeSegment(enablePack);
				if (!pack.isEmpty() && Files.exists(root.resolve(pack))) {
					repo.addPack("file/" + pack);
				}
				if (Files.exists(root.resolve("bjmapedit"))) {
					repo.addPack("file/bjmapedit");
				}
				server.reloadResources(withNewPacks(server)).whenComplete((v, e) -> {
					if (e == null) done.complete(null);
					else done.completeExceptionally(e);
				});
			} catch (RuntimeException e) {
				done.completeExceptionally(e);
			}
		});
		return done;
	}

	public static List<String> withNewPacks(MinecraftServer server) {
		var repo = server.getPackRepository();
		List<String> ids = new ArrayList<>(repo.getSelectedIds());
		var disabled = server.getWorldData().getDataConfiguration().dataPacks().getDisabled();
		for (String id : repo.getAvailableIds()) {
			if (!disabled.contains(id) && !ids.contains(id)) ids.add(id);
		}
		return ids;
	}

	private static boolean ensureBjmapeditMeta(Path root, String rel) throws IOException {
		if (rel == null || !rel.replace('\\', '/').startsWith("bjmapedit/")) return false;
		Path meta = root.resolve("bjmapedit").resolve("pack.mcmeta");
		if (Files.isRegularFile(meta)) return false;
		Files.createDirectories(meta.getParent());
		Files.writeString(meta,
			"{\n  \"pack\": {\n    \"pack_format\": 61,\n    \"description\": \"BJ-MapEdit\"\n  }\n}\n",
			StandardCharsets.UTF_8);
		return true;
	}

	private static void replyTree(ServerPlayer player, Path root) {
		List<String> lines = new ArrayList<>();
		boolean truncated = walk(root, root, 0, lines);
		lines.sort(Comparator.naturalOrder());
		StringBuilder sb = new StringBuilder();
		for (String line : lines) {
			if (sb.length() + line.length() + 1 > MAX_TEXT) {
				truncated = true;
				break;
			}
			if (sb.length() > 0) sb.append('\n');
			sb.append(line);
		}
		reply(player, KIND_TREE, truncated ? TREE_TRUNCATED : "", sb.toString());
	}

	private static boolean walk(Path root, Path current, int depth, List<String> out) {
		if (depth > 10) return false;
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(current)) {
			for (Path child : stream) {
				if (out.size() >= MAX_TREE) return true;
				String rel = rel(root, child);
				BasicFileAttributes attrs = rel.isEmpty() ? null : attributesInside(root, child);
				if (attrs == null) continue;
				if (attrs.isDirectory()) {
					out.add("D\t" + rel);
					if (walk(root, child, depth + 1, out)) return true;
				} else if (attrs.isRegularFile()) {
					out.add("F\t" + rel);
				}
			}
		} catch (IOException | DirectoryIteratorException e) {
			return false;
		}
		return false;
	}

	private static BasicFileAttributes attributesInside(Path root, Path path) {
		try {
			BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
			if (!attrs.isSymbolicLink() && !attrs.isOther()) return attrs;
			return inside(root, path) ? Files.readAttributes(path, BasicFileAttributes.class) : null;
		} catch (IOException e) {
			return null;
		}
	}

	public static boolean inside(Path root, Path path) {
		try {
			Path realRoot = root.toRealPath();
			Path p = path.toAbsolutePath().normalize();
			while (p != null && !Files.exists(p, LinkOption.NOFOLLOW_LINKS)) p = p.getParent();
			return p != null && p.toRealPath().startsWith(realRoot);
		} catch (IOException e) {
			return false;
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
		Path resolved;
		try {
			resolved = rootN.resolve(rel).normalize();
		} catch (InvalidPathException e) {
			return null;
		}
		if (!resolved.startsWith(rootN) || !inside(rootN, resolved)) return null;
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

	public static String packName(String raw) {
		String s = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
		StringBuilder sb = new StringBuilder(s.length());
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			boolean ok = c >= 'a' && c <= 'z' || c >= '0' && c <= '9' || c == '_' || c == '-' || c == '.';
			sb.append(ok ? c : '_');
		}
		int end = sb.length();
		while (end > 0 && sb.charAt(end - 1) == '.') end--;
		return sb.substring(0, end);
	}

	public static void deleteTree(Path root, Path path) throws IOException {
		BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
		boolean link = attrs.isSymbolicLink() || attrs.isOther();
		if (attrs.isDirectory() && (!link || inside(root, path))) {
			try (DirectoryStream<Path> ds = Files.newDirectoryStream(path)) {
				for (Path child : ds) {
					deleteTree(root, child);
				}
			}
		}
		Files.deleteIfExists(path);
	}

	private static void reply(ServerPlayer player, int kind, String path, String extra) {
		PlatformNetwork.sendToPlayer(player, new DatapackOpResultPayload(kind, path == null ? "" : path, extra == null ? "" : extra));
	}
}
