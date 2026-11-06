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
		Float value = data == null ? null : data.getFloat(0);
		return value == null ? 0 : Math.round(value);
	}

	public static ItemStack withCmd(ItemStack stack, int cmd) {
		ItemStack copy = stack.copyWithCount(1);
		copy.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of((float) cmd), List.of(), List.of(), List.of()));
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
		return pack.resolve("assets").resolve(id.getNamespace()).resolve("items").resolve(id.getPath() + ".json");
	}

	public static List<Mapping> read(Path pack, Item item) {
		List<Mapping> out = new ArrayList<>();
		JsonObject dispatch = dispatch(readJson(mappingFile(pack, item)));
		if (dispatch == null) return out;
		for (JsonObject e : entries(dispatch)) {
			Double t = threshold(e);
			if (t == null) continue;
			out.add(new Mapping((int) Math.round(t), modelName(e.get("model"))));
		}
		out.sort(Comparator.comparingInt(Mapping::cmd));
		return out;
	}

	public static void put(Path pack, Item item, int cmd, String model) throws IOException {
		Path file = mappingFile(pack, item);
		JsonObject root = readJson(file);
		if (root == null) {
			if (Files.exists(file)) throw new IOException(file.getFileName() + " is not a valid model, fix or delete it first");
			root = baseDefinition(item);
		}
		JsonObject dispatch = dispatch(root);
		if (dispatch == null) {
			JsonElement fallback = root.get("model");
			dispatch = new JsonObject();
			dispatch.addProperty("type", "minecraft:range_dispatch");
			dispatch.addProperty("property", "minecraft:custom_model_data");
			dispatch.addProperty("index", 0);
			dispatch.add("entries", new JsonArray());
			if (fallback != null) dispatch.add("fallback", fallback);
			root.add("model", dispatch);
		}
		List<JsonObject> list = without(dispatch, cmd);
		JsonObject entry = new JsonObject();
		entry.addProperty("threshold", cmd);
		JsonObject ref = new JsonObject();
		ref.addProperty("type", "minecraft:model");
		ref.addProperty("model", model);
		entry.add("model", ref);
		list.add(entry);
		writeEntries(file, root, dispatch, list);
	}

	public static void remove(Path pack, Item item, int cmd) throws IOException {
		Path file = mappingFile(pack, item);
		JsonObject root = readJson(file);
		JsonObject dispatch = dispatch(root);
		if (dispatch == null) return;
		writeEntries(file, root, dispatch, without(dispatch, cmd));
	}

	private static List<JsonObject> without(JsonObject dispatch, int cmd) {
		List<JsonObject> list = new ArrayList<>();
		for (JsonObject e : entries(dispatch)) {
			Double t = threshold(e);
			if (t == null || Math.abs(t - cmd) > 1.0E-4) list.add(e);
		}
		return list;
	}

	private static void writeEntries(Path file, JsonObject root, JsonObject dispatch, List<JsonObject> list) throws IOException {
		list.sort(Comparator.comparingDouble(e -> {
			Double t = threshold(e);
			return t == null ? Double.MAX_VALUE : t;
		}));
		if (list.isEmpty() && dispatch.has("fallback")) {
			root.add("model", dispatch.get("fallback"));
		} else {
			JsonArray arr = new JsonArray();
			list.forEach(arr::add);
			dispatch.add("entries", arr);
		}
		writeJson(file, root);
	}

	private static JsonObject dispatch(JsonObject root) {
		if (root == null) return null;
		JsonElement m = root.get("model");
		if (m == null || !m.isJsonObject()) return null;
		JsonObject o = m.getAsJsonObject();
		if (!isId(str(o, "type"), "range_dispatch") || !isId(str(o, "property"), "custom_model_data")) return null;
		JsonElement index = o.get("index");
		try {
			if (index != null && index.getAsInt() != 0) return null;
		} catch (Exception e) {
			return null;
		}
		return o;
	}

	private static List<JsonObject> entries(JsonObject dispatch) {
		List<JsonObject> out = new ArrayList<>();
		JsonElement e = dispatch.get("entries");
		if (e == null || !e.isJsonArray()) return out;
		for (JsonElement x : e.getAsJsonArray()) {
			if (x.isJsonObject()) out.add(x.getAsJsonObject());
		}
		return out;
	}

	private static Double threshold(JsonObject entry) {
		try {
			JsonElement t = entry.get("threshold");
			return t == null ? null : t.getAsDouble();
		} catch (Exception e) {
			return null;
		}
	}

	private static String modelName(JsonElement model) {
		if (model == null || !model.isJsonObject()) return "";
		JsonObject o = model.getAsJsonObject();
		String type = str(o, "type");
		if (isId(type, "model")) {
			String m = str(o, "model");
			return m == null ? "" : m;
		}
		return type == null ? "" : "[" + type + "]";
	}

	private static boolean isId(String value, String path) {
		return value != null && (value.equals(path) || value.equals("minecraft:" + path));
	}

	private static String str(JsonObject o, String key) {
		JsonElement e = o.get(key);
		return e != null && e.isJsonPrimitive() ? e.getAsString() : null;
	}

	private static JsonObject baseDefinition(Item item) {
		ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
		JsonObject def = original(ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "items/" + id.getPath() + ".json"));
		if (def != null && def.has("model")) return def;
		JsonObject fallback = new JsonObject();
		JsonObject model = new JsonObject();
		model.addProperty("type", "minecraft:model");
		model.addProperty("model", id.getNamespace() + ":item/" + id.getPath());
		fallback.add("model", model);
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
			try (BufferedReader reader = stack.get(stack.size() - 1).openAsReader()) {
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
