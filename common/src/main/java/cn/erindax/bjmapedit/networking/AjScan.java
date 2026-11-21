package cn.erindax.bjmapedit.networking;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.SharedConstants;
import net.minecraft.server.packs.PackType;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AjScan {
	public static final String PACK = "bjaj";
	public static final String PROJECT_DIR = PACK + "/data/" + PACK + "/bjmapedit";
	private static final int MAX_RIGS = 200;
	private static final int MAX_VISITS = 60000;
	private static final int MAX_DEPTH = 8;
	private static final int HEAD = 32768;
	private static final Pattern NAME = Pattern.compile("[a-z0-9_.-]+");
	private static final Pattern LENGTH = Pattern.compile("matches (\\d+)\\.\\.");
	private static final Pattern TAG = Pattern.compile("tag @s add ([A-Za-z0-9_.+-]+\\.playing)(?![A-Za-z0-9_.+-])");

	private AjScan() {}

	public static String scan(Path root) {
		Map<String, JsonObject> rigs = new TreeMap<>();
		int[] visits = {0};
		List<Path> packs = new ArrayList<>();
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(root)) {
			for (Path pack : stream) packs.add(pack);
		} catch (IOException | DirectoryIteratorException e) {
			packs.clear();
		}
		packs.sort(null);
		for (Path pack : packs) {
			String name = pack.getFileName().toString();
			if (name.equals(PACK)) continue;
			if (Files.isDirectory(pack)) {
				if (DatapackOps.inside(root, pack)) scanPack(name, pack, rigs, visits);
			} else if (name.toLowerCase(Locale.ROOT).endsWith(".zip") && Files.isRegularFile(pack, LinkOption.NOFOLLOW_LINKS)) {
				try (FileSystem fs = FileSystems.newFileSystem(pack)) {
					scanPack(name, fs.getPath("/"), rigs, visits);
				} catch (IOException | RuntimeException e) {
					continue;
				}
			}
		}
		JsonObject out = new JsonObject();
		JsonArray list = new JsonArray();
		int size = 0;
		boolean more = visits[0] > MAX_VISITS;
		for (JsonObject rig : rigs.values()) {
			size += rig.toString().length() + 1;
			if (list.size() >= MAX_RIGS || size > DatapackOps.MAX_TEXT / 2) {
				more = true;
				break;
			}
			list.add(rig);
		}
		out.add("rigs", list);
		out.add("made", made(root));
		out.addProperty("more", more);
		return out.toString();
	}

	private static JsonArray made(Path root) {
		List<String> ids = new ArrayList<>();
		Path dir = DatapackOps.resolveIn(root, PROJECT_DIR);
		if (dir != null && Files.isDirectory(dir)) {
			try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.json")) {
				for (Path file : stream) {
					String name = file.getFileName().toString();
					String id = name.substring(0, name.length() - 5);
					if (id.matches("[a-z0-9_]{1,24}") && Files.isRegularFile(file)) ids.add(id);
				}
			} catch (IOException | DirectoryIteratorException e) {
				ids.clear();
			}
		}
		ids.sort(null);
		JsonArray out = new JsonArray();
		for (String id : ids) out.add(id);
		return out;
	}

	private static void scanPack(String name, Path pack, Map<String, JsonObject> rigs, int[] visits) {
		Map<String, JsonObject> found = new LinkedHashMap<>();
		scanData(name, pack.resolve("data"), found, visits);
		for (String overlay : overlays(pack)) scanData(name, pack.resolve(overlay).resolve("data"), found, visits);
		for (Map.Entry<String, JsonObject> e : found.entrySet()) rigs.putIfAbsent(e.getKey(), e.getValue());
	}

	private static List<String> overlays(Path pack) {
		List<String> out = new ArrayList<>();
		Path meta = pack.resolve("pack.mcmeta");
		if (!Files.isRegularFile(meta)) return out;
		int format = SharedConstants.getCurrentVersion().getPackVersion(PackType.SERVER_DATA);
		try {
			JsonObject json = JsonParser.parseString(Files.readString(meta, StandardCharsets.UTF_8)).getAsJsonObject();
			JsonArray entries = json.getAsJsonObject("overlays").getAsJsonArray("entries");
			for (JsonElement e : entries) {
				JsonObject entry = e.getAsJsonObject();
				String dir = entry.get("directory").getAsString();
				if (dir.matches("[a-z0-9_-]+") && matches(entry.get("formats"), format)) out.add(dir);
			}
		} catch (IOException | RuntimeException e) {
			return out;
		}
		return out;
	}

	private static boolean matches(JsonElement formats, int format) {
		if (formats == null) return false;
		if (formats.isJsonPrimitive()) return formats.getAsInt() == format;
		if (formats.isJsonArray()) {
			JsonArray a = formats.getAsJsonArray();
			return a.size() == 2 && a.get(0).getAsInt() <= format && format <= a.get(1).getAsInt();
		}
		JsonObject o = formats.getAsJsonObject();
		return o.get("min_inclusive").getAsInt() <= format && format <= o.get("max_inclusive").getAsInt();
	}

	private static void scanData(String pack, Path data, Map<String, JsonObject> found, int[] visits) {
		if (!Files.isDirectory(data, LinkOption.NOFOLLOW_LINKS)) return;
		for (Path ns : dirs(data)) {
			String namespace = name(ns);
			if (!namespace.matches("[a-z0-9_.-]+")) continue;
			Path functions = ns.resolve("function");
			if (Files.isDirectory(functions, LinkOption.NOFOLLOW_LINKS)) walk(pack, namespace, functions, "", 0, found, visits);
		}
	}

	private static void walk(String pack, String namespace, Path dir, String rel, int depth, Map<String, JsonObject> found, int[] visits) {
		if (depth > MAX_DEPTH || ++visits[0] > MAX_VISITS) return;
		if (!rel.isEmpty() && Files.isRegularFile(dir.resolve("summon.mcfunction")) && Files.isRegularFile(dir.resolve("remove").resolve("this.mcfunction"))) {
			String head = head(dir.resolve("summon.mcfunction"));
			if (head.contains("aj.global.root")) {
				found.put(namespace + ":" + rel, rig(pack, namespace + ":" + rel, dir, head));
				return;
			}
		}
		for (Path child : dirs(dir)) {
			String name = name(child);
			if (name.equals("zzz") || !NAME.matcher(name).matches()) continue;
			walk(pack, namespace, child, rel.isEmpty() ? name : rel + "/" + name, depth + 1, found, visits);
		}
	}

	private static JsonObject rig(String pack, String id, Path dir, String head) {
		JsonObject rig = new JsonObject();
		rig.addProperty("id", id);
		rig.addProperty("pack", pack);
		rig.addProperty("args", head.contains("$(args)"));
		Path animations = dir.resolve("animations");
		rig.addProperty("pause", Files.isRegularFile(animations.resolve("pause_all.mcfunction")));
		JsonArray anims = new JsonArray();
		for (Path anim : dirs(animations)) {
			String name = name(anim);
			if (!NAME.matcher(name).matches() || !Files.isRegularFile(anim.resolve("play.mcfunction"))) continue;
			JsonObject a = new JsonObject();
			a.addProperty("n", name);
			a.addProperty("l", length(anim.resolve("next_frame.mcfunction")));
			a.addProperty("t", tag(anim.resolve("play.mcfunction")));
			anims.add(a);
		}
		rig.add("anims", anims);
		JsonArray variants = new JsonArray();
		for (Path variant : dirs(dir.resolve("variants"))) {
			String name = name(variant);
			if (NAME.matcher(name).matches() && Files.isRegularFile(variant.resolve("apply.mcfunction"))) variants.add(variant(name, "variants/" + name + "/apply"));
		}
		for (Path variant : files(dir.resolve("apply_variant"))) {
			String file = name(variant);
			if (!file.endsWith(".mcfunction")) continue;
			String name = file.substring(0, file.length() - 11);
			if (NAME.matcher(name).matches()) variants.add(variant(name, "apply_variant/" + name));
		}
		rig.add("vars", variants);
		return rig;
	}

	private static JsonObject variant(String name, String function) {
		JsonObject v = new JsonObject();
		v.addProperty("n", name);
		v.addProperty("f", function);
		return v;
	}

	private static int length(Path file) {
		if (!Files.isRegularFile(file)) return 0;
		Matcher m = LENGTH.matcher(head(file));
		if (!m.find()) return 0;
		try {
			return Math.min(72000, Integer.parseInt(m.group(1)));
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static String tag(Path file) {
		Matcher m = TAG.matcher(head(file));
		return m.find() ? m.group(1) : "";
	}

	private static String head(Path file) {
		char[] buf = new char[HEAD];
		try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
			int total = 0;
			while (total < buf.length) {
				int n = reader.read(buf, total, buf.length - total);
				if (n < 0) break;
				total += n;
			}
			return new String(buf, 0, total);
		} catch (IOException | RuntimeException e) {
			return "";
		}
	}

	private static String name(Path path) {
		Path file = path.getFileName();
		String name = file == null ? "" : file.toString();
		return name.endsWith("/") ? name.substring(0, name.length() - 1) : name;
	}

	private static List<Path> dirs(Path dir) {
		return children(dir, true);
	}

	private static List<Path> files(Path dir) {
		return children(dir, false);
	}

	private static List<Path> children(Path dir, boolean directories) {
		List<Path> out = new ArrayList<>();
		if (!Files.isDirectory(dir, LinkOption.NOFOLLOW_LINKS)) return out;
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
			for (Path child : stream) {
				if (directories ? Files.isDirectory(child, LinkOption.NOFOLLOW_LINKS) : Files.isRegularFile(child, LinkOption.NOFOLLOW_LINKS)) out.add(child);
			}
		} catch (IOException | DirectoryIteratorException e) {
			out.clear();
		}
		out.sort(null);
		return out;
	}
}
