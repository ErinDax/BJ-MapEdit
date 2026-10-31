package cn.erindax.bjmapedit.client.widget;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ItemModels {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
	private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};

	public record Mapping(int cmd, String model) {}

	private ItemModels() {}

	public static int cmdOf(ItemStack stack) {
		CustomModelData data = stack.get(DataComponents.CUSTOM_MODEL_DATA);
		return data == null ? 0 : data.value();
	}

	public static ItemStack withCmd(ItemStack stack, int cmd) {
		ItemStack copy = stack.copyWithCount(1);
		copy.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(cmd));
		return copy;
	}

	public static String itemId(Item item) {
		return BuiltInRegistries.ITEM.getKey(item).toString();
	}

	public static String resourceId(String ns, String name) {
		return ns + ":item/" + name;
	}

	public static Path modelFile(Path pack, String ns, String name) {
		return pack.resolve("assets").resolve(ns).resolve("models").resolve("item").resolve(name + ".json");
	}

	private static Path textureFile(Path pack, String ns, String name) {
		return pack.resolve("assets").resolve(ns).resolve("textures").resolve("item").resolve(name + ".png");
	}

	private static Path mappingFile(Path pack, Item item) {
		ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
		return pack.resolve("assets").resolve(id.getNamespace()).resolve("models").resolve("item").resolve(id.getPath() + ".json");
	}

	public static List<Mapping> read(Path pack, Item item) {
		List<Mapping> out = new ArrayList<>();
		JsonObject root = readJson(mappingFile(pack, item));
		if (root == null || !root.has("overrides") || !root.get("overrides").isJsonArray()) return out;
		for (JsonElement e : root.getAsJsonArray("overrides")) {
			Integer cmd = cmdOf(e);
			if (cmd == null) continue;
			JsonElement model = e.getAsJsonObject().get("model");
			out.add(new Mapping(cmd, model != null && model.isJsonPrimitive() ? model.getAsString() : ""));
		}
		out.sort(Comparator.comparingInt(Mapping::cmd));
		return out;
	}

	public static void put(Path pack, Item item, int cmd, String model) throws IOException {
		Path file = mappingFile(pack, item);
		JsonObject root = readJson(file);
		if (root == null) root = baseModel(item);
		List<JsonElement> others = new ArrayList<>();
		List<JsonObject> cmds = new ArrayList<>();
		splitOverrides(root, cmd, others, cmds);
		JsonObject entry = new JsonObject();
		JsonObject predicate = new JsonObject();
		predicate.addProperty("custom_model_data", cmd);
		entry.add("predicate", predicate);
		entry.addProperty("model", model);
		cmds.add(entry);
		writeOverrides(file, root, others, cmds);
	}

	public static void remove(Path pack, Item item, int cmd) throws IOException {
		Path file = mappingFile(pack, item);
		JsonObject root = readJson(file);
		if (root == null) return;
		List<JsonElement> others = new ArrayList<>();
		List<JsonObject> cmds = new ArrayList<>();
		splitOverrides(root, cmd, others, cmds);
		writeOverrides(file, root, others, cmds);
	}

	private static void splitOverrides(JsonObject root, int skip, List<JsonElement> others, List<JsonObject> cmds) {
		if (!root.has("overrides") || !root.get("overrides").isJsonArray()) return;
		for (JsonElement e : root.getAsJsonArray("overrides")) {
			Integer c = cmdOf(e);
			if (c == null) others.add(e);
			else if (c != skip) cmds.add(e.getAsJsonObject());
		}
	}

	private static void writeOverrides(Path file, JsonObject root, List<JsonElement> others, List<JsonObject> cmds) throws IOException {
		cmds.sort(Comparator.comparingInt(o -> cmdOf(o)));
		JsonArray all = new JsonArray();
		others.forEach(all::add);
		cmds.forEach(all::add);
		if (all.isEmpty()) root.remove("overrides");
		else root.add("overrides", all);
		writeJson(file, root);
	}

	private static Integer cmdOf(JsonElement e) {
		if (e == null || !e.isJsonObject()) return null;
		JsonElement p = e.getAsJsonObject().get("predicate");
		if (p == null || !p.isJsonObject()) return null;
		JsonObject predicate = p.getAsJsonObject();
		if (predicate.size() != 1 || !predicate.has("custom_model_data")) return null;
		try {
			return (int) Math.round(predicate.get("custom_model_data").getAsDouble());
		} catch (Exception ex) {
			return null;
		}
	}

	private static JsonObject baseModel(Item item) {
		ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
		JsonObject model = original(ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "models/item/" + id.getPath() + ".json"));
		if (model != null) return model;
		JsonObject fallback = new JsonObject();
		fallback.addProperty("parent", "minecraft:item/generated");
		JsonObject textures = new JsonObject();
		textures.addProperty("layer0", id.getNamespace() + ":item/" + id.getPath());
		fallback.add("textures", textures);
		return fallback;
	}

	public static String baseParent(Item item) {
		ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
		JsonObject model = original(ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "models/item/" + id.getPath() + ".json"));
		String parent = model != null && model.has("parent") ? model.get("parent").getAsString() : "";
		if (parent.startsWith("minecraft:")) parent = parent.substring("minecraft:".length());
		return parent.startsWith("item/handheld") ? "minecraft:" + parent : "minecraft:item/generated";
	}

	public static void writeGenerated(Path pack, String ns, String name, String parent, String texture) throws IOException {
		JsonObject model = new JsonObject();
		model.addProperty("parent", parent);
		JsonObject textures = new JsonObject();
		textures.addProperty("layer0", texture);
		model.add("textures", textures);
		writeJson(modelFile(pack, ns, name), model);
	}

	public static void copyTexture(Path source, Path pack, String ns, String name) throws IOException {
		byte[] head = new byte[PNG.length];
		try (InputStream in = Files.newInputStream(source)) {
			if (in.readNBytes(head, 0, head.length) != head.length) throw new IOException(source.getFileName() + " is not a PNG");
		}
		for (int i = 0; i < PNG.length; i++) {
			if (head[i] != PNG[i]) throw new IOException(source.getFileName() + " is not a PNG");
		}
		Path dest = textureFile(pack, ns, name);
		Files.createDirectories(dest.getParent());
		Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING);
		Path meta = source.resolveSibling(source.getFileName() + ".mcmeta");
		if (Files.isRegularFile(meta)) {
			Files.copy(meta, dest.resolveSibling(dest.getFileName() + ".mcmeta"), StandardCopyOption.REPLACE_EXISTING);
		}
	}

	public static void importModel(Path pack, String ns, String name, Path source, Path texture) throws IOException {
		JsonElement parsed = JsonParser.parseString(Files.readString(source, StandardCharsets.UTF_8));
		if (!parsed.isJsonObject()) throw new IOException(source.getFileName() + " is not a model");
		JsonObject model = parsed.getAsJsonObject();
		JsonElement tex = model.get("textures");
		if (tex != null && tex.isJsonObject()) {
			JsonObject fixed = new JsonObject();
			Map<String, String> done = new HashMap<>();
			for (Map.Entry<String, JsonElement> e : tex.getAsJsonObject().entrySet()) {
				String ref = e.getValue().isJsonPrimitive() ? e.getValue().getAsString() : "";
				if (ref.isEmpty() || ref.startsWith("#")) {
					fixed.add(e.getKey(), e.getValue());
					continue;
				}
				String mapped = done.get(ref);
				if (mapped == null) {
					mapped = resolveTexture(pack, ns, name, source, ref, texture);
					done.put(ref, mapped);
				}
				fixed.addProperty(e.getKey(), mapped);
			}
			model.add("textures", fixed);
		} else if (texture != null) {
			copyTexture(texture, pack, ns, name);
		}
		writeJson(modelFile(pack, ns, name), model);
	}

	private static String resolveTexture(Path pack, String ns, String name, Path source, String ref, Path texture) throws IOException {
		String seg = ref.substring(ref.lastIndexOf('/') + 1);
		int colon = seg.indexOf(':');
		if (colon >= 0) seg = seg.substring(colon + 1);
		Path dir = source.getParent();
		Path local = dir == null || seg.isEmpty() ? null : dir.resolve(seg + ".png");
		if (local != null && Files.isRegularFile(local)) {
			String sub = name + "_" + ResourcePacks.resourceName(seg).replace('/', '_');
			copyTexture(local, pack, ns, sub);
			return resourceId(ns, sub);
		}
		if (textureExists(ref)) return ref;
		if (texture != null) {
			copyTexture(texture, pack, ns, name);
			return resourceId(ns, name);
		}
		return ref;
	}

	private static boolean textureExists(String ref) {
		ResourceLocation id = ResourceLocation.tryParse(ref);
		if (id == null) return false;
		ResourceLocation file = ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "textures/" + id.getPath() + ".png");
		return Minecraft.getInstance().getResourceManager().getResource(file).isPresent();
	}

	private static JsonObject original(ResourceLocation file) {
		try {
			List<Resource> stack = Minecraft.getInstance().getResourceManager().getResourceStack(file);
			if (stack.isEmpty()) return null;
			try (BufferedReader reader = stack.get(0).openAsReader()) {
				JsonElement e = JsonParser.parseReader(reader);
				return e.isJsonObject() ? e.getAsJsonObject() : null;
			}
		} catch (Exception e) {
			return null;
		}
	}

	private static JsonObject readJson(Path file) {
		if (!Files.isRegularFile(file)) return null;
		try {
			JsonElement e = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8));
			return e.isJsonObject() ? e.getAsJsonObject() : null;
		} catch (Exception e) {
			return null;
		}
	}

	private static void writeJson(Path file, JsonObject json) throws IOException {
		Files.createDirectories(file.getParent());
		Files.writeString(file, GSON.toJson(json) + "\n", StandardCharsets.UTF_8);
	}
}
