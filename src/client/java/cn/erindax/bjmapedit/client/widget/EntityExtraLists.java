package cn.erindax.bjmapedit.client.widget;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class EntityExtraLists {
	private EntityExtraLists() {}

	public record Option(String value, String label) {}

	private static final Option[] DYE = {
		opt("0", "白色"), opt("1", "橙色"), opt("2", "品红色"), opt("3", "淡蓝色"),
		opt("4", "黄色"), opt("5", "黄绿色"), opt("6", "粉红色"), opt("7", "灰色"),
		opt("8", "淡灰色"), opt("9", "青色"), opt("10", "紫色"), opt("11", "蓝色"),
		opt("12", "棕色"), opt("13", "绿色"), opt("14", "红色"), opt("15", "黑色")
	};
	private static final Option[] CAT = {
		id("tabby", "虎斑"), id("black", "西服"), id("red", "红虎斑"), id("siamese", "暹罗"),
		id("british_shorthair", "英国短毛"), id("calico", "花龟"), id("persian", "波斯"),
		id("ragdoll", "布偶"), id("white", "白"), id("jellie", "杰森"), id("all_black", "黑")
	};
	private static final Option[] WOLF = {
		id("pale", "原版"), id("woods", "森林"), id("ashen", "灰白"), id("black", "黑色"),
		id("chestnut", "栗色"), id("rusty", "锈色"), id("spotted", "斑点"),
		id("striped", "条纹"), id("snowy", "雪地")
	};
	private static final Option[] FROG = {
		id("temperate", "温带"), id("warm", "暖带"), id("cold", "寒带")
	};
	private static final Option[] PARROT = {
		opt("0", "红色"), opt("1", "蓝色"), opt("2", "绿色"), opt("3", "青色"), opt("4", "灰色")
	};
	private static final Option[] AXOLOTL = {
		opt("0", "粉色"), opt("1", "野生"), opt("2", "金色"), opt("3", "青色"), opt("4", "蓝色")
	};
	private static final Option[] FOX = {
		opt("red", "红狐"), opt("snow", "雪狐")
	};
	private static final Option[] MOOSHROOM = {
		opt("red", "红色"), opt("brown", "棕色")
	};
	private static final Option[] LLAMA = {
		opt("0", "乳白色"), opt("1", "白色"), opt("2", "棕色"), opt("3", "灰色")
	};
	private static final Option[] RABBIT = {
		opt("0", "棕色"), opt("1", "白色"), opt("2", "黑色"), opt("3", "黑白"),
		opt("4", "金色"), opt("5", "盐胡椒"), opt("99", "杀手兔")
	};
	private static final Option[] PANDA = {
		opt("normal", "普通"), opt("lazy", "懒惰"), opt("worried", "忧郁"),
		opt("playful", "顽皮"), opt("brown", "棕色"), opt("weak", "体弱"), opt("aggressive", "好斗")
	};
	private static final Option[] SLIME = {
		opt("0", "小型"), opt("1", "中型"), opt("3", "大型")
	};
	private static final Option[] PHANTOM = {
		opt("0", "普通"), opt("1", "较大"), opt("2", "大"), opt("3", "巨大")
	};
	private static final Option[] CREEP_R = {
		opt("1", "半径1"), opt("2", "半径2"), opt("3", "半径3"),
		opt("4", "半径4"), opt("5", "半径5"), opt("6", "半径6")
	};
	private static final Option[] GHAST_P = {
		opt("1", "威力1"), opt("2", "威力2"), opt("3", "威力3"),
		opt("4", "威力4"), opt("5", "威力5")
	};
	private static final Option[] LLAMA_STR = {
		opt("1", "负重1"), opt("2", "负重2"), opt("3", "负重3"),
		opt("4", "负重4"), opt("5", "负重5")
	};
	private static final String[] HORSE_COLOR = {"白色", "奶油色", "栗色", "棕色", "黑色", "灰色", "深褐色"};
	private static final String[] HORSE_MARK = {"无花纹", "白袜", "白斑", "白点", "黑点"};
	private static final Option[] HORSE = horseOptions();

	private static Option opt(String value, String label) {
		return new Option(value, label);
	}

	private static Option id(String path, String label) {
		return new Option("minecraft:" + path, label);
	}

	private static Option[] horseOptions() {
		Option[] out = new Option[HORSE_COLOR.length * HORSE_MARK.length];
		int i = 0;
		for (int mark = 0; mark < HORSE_MARK.length; mark++) {
			for (int color = 0; color < HORSE_COLOR.length; color++) {
				out[i++] = opt(String.valueOf(color | (mark << 8)), HORSE_COLOR[color] + " · " + HORSE_MARK[mark]);
			}
		}
		return out;
	}

	public static List<Option> options(String entityId, String key) {
		if (entityId == null || key == null) return List.of();
		return switch (key) {
			case "CollarColor", "Color" -> List.of(DYE);
			case "variant" -> switch (entityId) {
				case "minecraft:cat" -> List.of(CAT);
				case "minecraft:wolf" -> List.of(WOLF);
				case "minecraft:frog" -> List.of(FROG);
				default -> List.of();
			};
			case "Variant" -> switch (entityId) {
				case "minecraft:horse" -> List.of(HORSE);
				case "minecraft:parrot" -> List.of(PARROT);
				case "minecraft:axolotl" -> List.of(AXOLOTL);
				case "minecraft:llama", "minecraft:trader_llama" -> List.of(LLAMA);
				default -> List.of();
			};
			case "Type" -> switch (entityId) {
				case "minecraft:fox" -> List.of(FOX);
				case "minecraft:mooshroom" -> List.of(MOOSHROOM);
				default -> List.of();
			};
			case "RabbitType" -> List.of(RABBIT);
			case "MainGene", "HiddenGene" -> List.of(PANDA);
			case "Size" -> entityId.contains("phantom") ? List.of(PHANTOM) : List.of(SLIME);
			case "ExplosionRadius" -> List.of(CREEP_R);
			case "ExplosionPower" -> List.of(GHAST_P);
			case "Strength" -> List.of(LLAMA_STR);
			case "carriedBlockState" -> blockOptions();
			default -> List.of();
		};
	}

	private static List<Option> cachedBlocks;

	private static List<Option> blockOptions() {
		if (cachedBlocks != null) return cachedBlocks;
		List<Option> out = new ArrayList<>();
		for (ResourceLocation id : BuiltInRegistries.BLOCK.keySet()) {
			Block block = BuiltInRegistries.BLOCK.get(id);
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
