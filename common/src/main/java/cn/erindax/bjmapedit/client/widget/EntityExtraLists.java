package cn.erindax.bjmapedit.client.widget;

import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class EntityExtraLists {
	private EntityExtraLists() {}

	public record Option(String value, String label) {}

	private static final String OPTION = "screen.bj_mapedit.option.";
	private static final String[] DYE_COLORS = {
		"white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
		"light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"
	};
	private static final Option[] CAT = ids("cat", "tabby", "black", "red", "siamese", "british_shorthair", "calico",
		"persian", "ragdoll", "white", "jellie", "all_black");
	private static final Option[] WOLF = ids("wolf", "pale", "woods", "ashen", "black", "chestnut", "rusty", "spotted",
		"striped", "snowy");
	private static final Option[] FROG = ids("frog", "temperate", "warm", "cold");
	private static final Option[] PARROT = opts("parrot", "0", "1", "2", "3", "4");
	private static final Option[] AXOLOTL = opts("axolotl", "0", "1", "2", "3", "4");
	private static final Option[] FOX = opts("fox", "red", "snow");
	private static final Option[] MOOSHROOM = opts("mooshroom", "red", "brown");
	private static final Option[] LLAMA = opts("llama", "0", "1", "2", "3");
	private static final Option[] RABBIT = opts("rabbit", "0", "1", "2", "3", "4", "5", "99");
	private static final Option[] PANDA = opts("panda", "normal", "lazy", "worried", "playful", "brown", "weak", "aggressive");
	private static final Option[] SLIME = opts("slime", "0", "1", "3");
	private static final Option[] PHANTOM = opts("phantom", "0", "1", "2", "3");
	private static final Option[] CREEP_R = numbered("radius", 1, 6);
	private static final Option[] GHAST_P = numbered("power", 1, 5);
	private static final Option[] LLAMA_STR = numbered("strength", 1, 5);
	private static final int HORSE_COLORS = 7;
	private static final int HORSE_MARKINGS = 5;

	private static Option[] opts(String group, String... values) {
		Option[] out = new Option[values.length];
		for (int i = 0; i < values.length; i++) {
			out[i] = new Option(values[i], OPTION + group + "." + values[i]);
		}
		return out;
	}

	private static Option[] ids(String group, String... paths) {
		Option[] out = new Option[paths.length];
		for (int i = 0; i < paths.length; i++) {
			out[i] = new Option("minecraft:" + paths[i], OPTION + group + "." + paths[i]);
		}
		return out;
	}

	private static Option[] numbered(String group, int from, int to) {
		Option[] out = new Option[to - from + 1];
		for (int i = from; i <= to; i++) {
			out[i - from] = new Option(String.valueOf(i), OPTION + group);
		}
		return out;
	}

	private static List<Option> translated(Option[] options) {
		List<Option> out = new ArrayList<>(options.length);
		for (Option o : options) {
			out.add(new Option(o.value, I18n.get(o.label, o.value)));
		}
		return out;
	}

	private static List<Option> dyeOptions() {
		List<Option> out = new ArrayList<>(DYE_COLORS.length);
		for (int i = 0; i < DYE_COLORS.length; i++) {
			out.add(new Option(String.valueOf(i), I18n.get("color.minecraft." + DYE_COLORS[i])));
		}
		return out;
	}

	private static List<Option> horseOptions() {
		List<Option> out = new ArrayList<>(HORSE_COLORS * HORSE_MARKINGS);
		for (int mark = 0; mark < HORSE_MARKINGS; mark++) {
			for (int color = 0; color < HORSE_COLORS; color++) {
				out.add(new Option(String.valueOf(color | (mark << 8)),
					I18n.get(OPTION + "horse_color." + color) + " · " + I18n.get(OPTION + "horse_marking." + mark)));
			}
		}
		return out;
	}

	public static List<Option> options(String entityId, String key) {
		if (entityId == null || key == null) return List.of();
		return switch (key) {
			case "CollarColor", "Color" -> dyeOptions();
			case "variant" -> switch (entityId) {
				case "minecraft:cat" -> translated(CAT);
				case "minecraft:wolf" -> translated(WOLF);
				case "minecraft:frog" -> translated(FROG);
				default -> List.of();
			};
			case "Variant" -> switch (entityId) {
				case "minecraft:horse" -> horseOptions();
				case "minecraft:parrot" -> translated(PARROT);
				case "minecraft:axolotl" -> translated(AXOLOTL);
				case "minecraft:llama", "minecraft:trader_llama" -> translated(LLAMA);
				default -> List.of();
			};
			case "Type" -> switch (entityId) {
				case "minecraft:fox" -> translated(FOX);
				case "minecraft:mooshroom" -> translated(MOOSHROOM);
				default -> List.of();
			};
			case "RabbitType" -> translated(RABBIT);
			case "MainGene", "HiddenGene" -> translated(PANDA);
			case "Size" -> entityId.contains("phantom") ? translated(PHANTOM) : translated(SLIME);
			case "ExplosionRadius" -> translated(CREEP_R);
			case "ExplosionPower" -> translated(GHAST_P);
			case "Strength" -> translated(LLAMA_STR);
			case "carriedBlockState" -> blockOptions();
			default -> List.of();
		};
	}

	private static List<Option> cachedBlocks;

	private static List<Option> blockOptions() {
		if (cachedBlocks != null) return cachedBlocks;
		List<Option> out = new ArrayList<>();
		for (Identifier id : BuiltInRegistries.BLOCK.keySet()) {
			Block block = BuiltInRegistries.BLOCK.getValue(id);
			String cn = "";
			try {
				cn = block.getName().getString();
			} catch (Throwable ignored) {
			}
			if (cn.isEmpty()) continue;
			out.add(new Option("{Name:\"" + id + "\"}", cn));
		}
		cachedBlocks = out;
		return out;
	}

	public static List<Option> filter(String entityId, String key, String input) {
		List<Option> all = options(entityId, key);
		if (all.isEmpty() || input == null) return List.of();
		String raw = input.trim();
		if (raw.isEmpty()) return new ArrayList<>(all);
		String q = raw.toLowerCase(Locale.ROOT);
		List<Option> out = new ArrayList<>();
		for (Option o : all) {
			if (o.label.equals(raw) || o.value.equals(raw)) continue;
			if (o.label.toLowerCase(Locale.ROOT).contains(q) || o.value.toLowerCase(Locale.ROOT).contains(q)) {
				out.add(o);
			}
		}
		return out;
	}

	public static String toNbtValue(String entityId, String key, String input) {
		if (input == null || input.isBlank() || key == null) return null;
		List<Option> all = options(entityId, key);
		if (all.isEmpty()) return null;
		String raw = input.trim();
		Option hit = null;
		for (Option o : all) {
			if (o.label.equals(raw) || o.value.equals(raw)) {
				hit = o;
				break;
			}
		}
		if (hit == null) {
			String lower = raw.toLowerCase(Locale.ROOT);
			Option unique = null;
			int n = 0;
			for (Option o : all) {
				if (o.label.toLowerCase(Locale.ROOT).contains(lower) || o.value.toLowerCase(Locale.ROOT).contains(lower)) {
					unique = o;
					n++;
				}
			}
			if (n == 1) hit = unique;
		}
		if (hit == null) {
			if (raw.startsWith("{")) return raw;
			if (raw.matches("-?\\d+")) return raw;
			return "\"" + raw.replace("\"", "\\\"") + "\"";
		}
		if (hit.value.startsWith("{") || hit.value.matches("-?\\d+")) return hit.value;
		return "\"" + hit.value + "\"";
	}
}
