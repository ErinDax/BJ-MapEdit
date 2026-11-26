package cn.erindax.bjmapedit.client.widget;

import cn.erindax.bjmapedit.networking.AjScan;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;

public final class AjPack {
	public static final String PACK = AjScan.PACK;
	private static final String DATA = PACK + "/data/";
	private static final String FN = DATA + PACK + "/function/";
	private static final String META = "{\n  \"pack\": {\n    \"pack_format\": 48,\n    \"description\": \"BJ-MapEdit AJ Mobs\"\n  }\n}\n";
	private static final String PASSABLE = "{\n  \"values\": [\n    \"#minecraft:replaceable\",\n    \"#minecraft:flowers\",\n    \"#minecraft:crops\",\n    \"#minecraft:buttons\",\n    \"#minecraft:pressure_plates\",\n    \"#minecraft:rails\",\n    \"#minecraft:all_signs\",\n    \"#minecraft:banners\",\n    \"minecraft:torch\",\n    \"minecraft:wall_torch\",\n    \"minecraft:soul_torch\",\n    \"minecraft:soul_wall_torch\",\n    \"minecraft:redstone_torch\",\n    \"minecraft:redstone_wall_torch\"\n  ]\n}\n";
	private static final String ID = " bjaj.id";
	private static final String NEW_ROOT = "execute as @e[type=minecraft:item_display,tag=aj.global.root,tag=!bjaj.root,distance=..0.01,limit=1,sort=nearest] run function ";
	private static final String CARRIER_NBT = "{Tags:[\\\"bjaj.carrier\\\"]}";
	private static final String[] OBJECTIVES = {
		"bjaj.id", "bjaj.st", "bjaj.t", "bjaj.live", "bjaj.still", "bjaj.ev",
		"bjaj.ph", "bjaj.pt", "bjaj.sk", "bjaj.sw",
		"bjaj.s0", "bjaj.s1", "bjaj.s2", "bjaj.s3", "bjaj.s4", "bjaj.s5", "bjaj.s6", "bjaj.s7"
	};

	private record Form(int index, String rig, boolean args, boolean pause, String variant, String[] anims, String[] tags, int[] ticks, AjData.Rig rigInfo) {
		boolean hasAnim(String name) {
			return !name.isEmpty() && (rigInfo == null || rigInfo.anim(name) != null);
		}
	}

	private AjPack() {}

	public static String projectFile(String id) {
		return AjScan.PROJECT_DIR + "/" + id + ".json";
	}

	public static String modelDir(String id) {
		return FN + id;
	}

	public static String lootFile(String id) {
		return DATA + PACK + "/loot_table/" + id + ".json";
	}

	public static String summonCommand(String id) {
		return "function bjaj:" + id + "/summon";
	}

	public static String summonAhead(String id) {
		return "execute rotated ~ 0 positioned ^ ^ ^3 rotated ~180 0 run function bjaj:" + id + "/summon";
	}

	public static String removeCommand(String id) {
		return "function bjaj:" + id + "/remove";
	}

	public static String eggCommand(String id) {
		return "give @s minecraft:pig_spawn_egg[minecraft:entity_data={id:\"minecraft:marker\",Tags:[\"bjaj.egg\",\"bjaj.e." + id + "\"]},minecraft:item_name='\"" + id + "\"']";
	}

	public static String carrier(String raw) {
		ResourceLocation id = ResourceLocation.tryParse(raw == null ? "" : raw.trim());
		if (id == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(id)) return null;
		return BuiltInRegistries.ENTITY_TYPE.getValue(id).canSummon() ? id.toString() : null;
	}

	public static String nbt(String raw) {
		String s = raw == null ? "" : raw.trim();
		if (s.isEmpty()) return "";
		try {
			return TagParser.parseTag(s).toString();
		} catch (CommandSyntaxException e) {
			return null;
		}
	}

	public static String scale(String raw) {
		try {
			BigDecimal value = new BigDecimal(raw == null ? "" : raw.trim());
			if (value.compareTo(new BigDecimal("0.0625")) < 0 || value.compareTo(new BigDecimal("16")) > 0) return null;
			String s = value.stripTrailingZeros().toPlainString();
			return s.length() > 12 ? null : s;
		} catch (NumberFormatException e) {
			return null;
		}
	}

	public static String health(String raw) {
		String s = raw == null ? "" : raw.trim();
		if (s.isEmpty()) return "";
		if (!s.matches("\\d{1,4}")) return null;
		int value = Integer.parseInt(s);
		return value < 1 || value > 1024 ? null : String.valueOf(value);
	}

	public static int count(String raw) {
		String s = raw == null ? "" : raw.trim();
		return s.matches("\\d{1,3}") ? Integer.parseInt(s) : -1;
	}

	public static BigDecimal chance(String raw) {
		try {
			BigDecimal value = new BigDecimal(raw == null ? "" : raw.trim());
			if (value.signum() <= 0 || value.compareTo(new BigDecimal("100")) > 0) return null;
			return value.movePointLeft(2).stripTrailingZeros();
		} catch (NumberFormatException e) {
			return null;
		}
	}

	public static String dropProblem(AjData.Drop d) {
		if (ShopCompat.stackFromSnbt(d.item).isEmpty()) return "aj_bad_drop_item";
		int min = count(d.min);
		int max = count(d.max);
		if (min < 0 || max < 1 || min > max) return "aj_bad_drop_count";
		return chance(d.chance) == null ? "aj_bad_drop_chance" : null;
	}

	public static int intRange(String raw, int min, int max) {
		String s = raw == null ? "" : raw.trim();
		if (!s.matches("\\d{1,5}")) return -1;
		int v = Integer.parseInt(s);
		return v < min || v > max ? -1 : v;
	}

	public static String cleanCommand(String raw) {
		String s = raw == null ? "" : raw.trim();
		if (s.startsWith("/")) s = s.substring(1).trim();
		if (s.isEmpty() || s.startsWith("#") || s.startsWith("$") || s.indexOf('\n') >= 0 || s.indexOf('\r') >= 0) return null;
		ClientPacketListener conn = Minecraft.getInstance().getConnection();
		if (conn != null && conn.getCommands().getRoot().getChild("function") != null) {
			ParseResults<SharedSuggestionProvider> res = conn.getCommands().parse(s, conn.getSuggestionsProvider());
			if (!res.getExceptions().isEmpty() || res.getReader().canRead()) return null;
		}
		return s;
	}

	public static String skillProblem(AjData.Skill s) {
		int trigVal = switch (s.trigger) {
			case AjData.TRIG_TIMER, AjData.TRIG_NEAR -> intRange(s.value, 1, AjData.MAX_TICKS);
			case AjData.TRIG_HURT, AjData.TRIG_HIT -> intRange(s.value, 1, 999);
			case AjData.TRIG_HP -> intRange(s.value, 1, 99);
			default -> -1;
		};
		if (trigVal < 0) {
			return switch (s.trigger) {
				case AjData.TRIG_HURT, AjData.TRIG_HIT -> "aj_bad_skill_count";
				case AjData.TRIG_HP -> "aj_bad_skill_hp";
				default -> "aj_bad_skill_ticks";
			};
		}
		if (intRange(s.range, 1, 64) < 0) return "aj_bad_skill_range";
		if (intRange(s.windup, 0, 1200) < 0) return "aj_bad_skill_windup";
		if (AjData.usesDamage(s.kind) && intRange(s.power, 1, 500) < 0) return "aj_bad_skill_damage";
		if (AjData.usesBurst(s.kind) && intRange(s.power, 1, 5) < 0) return "aj_bad_skill_burst";
		if (s.kind == AjData.SKILL_CUSTOM && cleanCommand(s.command) == null) return "aj_bad_skill_cmd";
		return null;
	}

	public static String missingRig(AjData m, List<AjData.Rig> rigs) {
		if (AjData.findRig(rigs, m.rig) == null) return m.rig;
		if (m.mob() && m.boss) {
			for (AjData.Phase ph : m.phases) {
				if (ph.changeRig && AjData.findRig(rigs, ph.rig) == null) return ph.rig;
			}
		}
		return null;
	}

	public static String problem(AjData m) {
		if (!AjData.validId(m.id)) return "aj_bad_id";
		if (!AjData.validRig(m.rig)) return "aj_no_rig";
		if (!m.mob()) return null;
		if (carrier(m.carrier) == null) return "aj_bad_carrier";
		if (nbt(m.nbt) == null) return "aj_bad_nbt";
		if (scale(m.scale) == null) return "aj_bad_scale";
		if (health(m.health) == null) return "aj_bad_health";
		for (AjData.Skill s : m.skills) {
			String bad = skillProblem(s);
			if (bad != null) return bad;
		}
		if (m.boss) {
			int prevHp = 100;
			for (AjData.Phase ph : m.phases) {
				int hp = intRange(ph.hp, 1, 99);
				if (hp < 0) return "aj_bad_phase_hp";
				if (hp >= prevHp) return "aj_bad_phase_order";
				prevHp = hp;
				if (ph.changeRig && !AjData.validRig(ph.rig)) return "aj_bad_phase_rig";
			}
		}
		if (m.loot != AjData.LOOT_CUSTOM) return null;
		for (AjData.Drop d : m.drops) {
			String bad = dropProblem(d);
			if (bad != null) return bad;
		}
		return lootTable(m) == null ? "aj_bad_drop_item" : null;
	}

	public static String lootTable(AjData m) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) return null;
		DynamicOps<JsonElement> ops = mc.level.registryAccess().createSerializationContext(JsonOps.INSTANCE);
		JsonArray pools = new JsonArray();
		for (AjData.Drop d : m.drops) {
			ItemStack stack = ShopCompat.stackFromSnbt(d.item);
			int min = count(d.min);
			int max = count(d.max);
			BigDecimal chance = chance(d.chance);
			if (stack.isEmpty() || min < 0 || max < 1 || min > max || chance == null) return null;
			JsonArray functions = new JsonArray();
			if (min != 1 || max != 1) {
				JsonObject count = new JsonObject();
				count.addProperty("function", "minecraft:set_count");
				if (min == max) {
					count.addProperty("count", min);
				} else {
					JsonObject range = new JsonObject();
					range.addProperty("type", "minecraft:uniform");
					range.addProperty("min", min);
					range.addProperty("max", max);
					count.add("count", range);
				}
				functions.add(count);
			}
			DataComponentPatch patch = stack.getComponentsPatch();
			if (!patch.isEmpty()) {
				JsonElement components = DataComponentPatch.CODEC.encodeStart(ops, patch).result().orElse(null);
				if (components == null) return null;
				JsonObject set = new JsonObject();
				set.addProperty("function", "minecraft:set_components");
				set.add("components", components);
				functions.add(set);
			}
			JsonObject entry = new JsonObject();
			entry.addProperty("type", "minecraft:item");
			entry.addProperty("name", BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
			if (!functions.isEmpty()) entry.add("functions", functions);
			JsonArray entries = new JsonArray();
			entries.add(entry);
			JsonObject pool = new JsonObject();
			pool.addProperty("rolls", 1);
			pool.add("entries", entries);
			if (chance.compareTo(BigDecimal.ONE) < 0) {
				JsonObject condition = new JsonObject();
				condition.addProperty("condition", "minecraft:random_chance");
				condition.addProperty("chance", chance);
				JsonArray conditions = new JsonArray();
				conditions.add(condition);
				pool.add("conditions", conditions);
			}
			pools.add(pool);
		}
		JsonObject table = new JsonObject();
		table.addProperty("type", "minecraft:entity");
		table.add("pools", pools);
		return table + "\n";
	}

	public static Map<String, String> globalFiles(Collection<String> models) {
		Map<String, String> out = new LinkedHashMap<>();
		StringBuilder ticks = new StringBuilder();
		StringBuilder eggs = new StringBuilder();
		for (String id : new TreeSet<>(models)) {
			ticks.append(ticks.length() == 0 ? "" : ",").append("\"bjaj:").append(id).append("/z/tick\"");
			eggs.append(eggs.length() == 0 ? "" : ",").append("\"bjaj:").append(id).append("/z/egg\"");
		}
		out.put(PACK + "/pack.mcmeta", META);
		out.put(DATA + "minecraft/tags/function/load.json", "{\"values\":[\"bjaj:z/load\"]}\n");
		out.put(DATA + "minecraft/tags/function/tick.json", "{\"values\":[\"bjaj:z/tick\"]}\n");
		out.put(DATA + PACK + "/tags/function/tick.json", "{\"values\":[" + ticks + "]}\n");
		out.put(DATA + PACK + "/tags/function/egg.json", "{\"values\":[" + eggs + "]}\n");
		out.put(DATA + PACK + "/tags/block/passable.json", PASSABLE);
		StringBuilder load = new StringBuilder();
		for (String objective : OBJECTIVES) load.append("scoreboard objectives add ").append(objective).append(" dummy\n");
		out.put(FN + "z/load.mcfunction", load.toString());
		out.put(FN + "z/tick.mcfunction",
			"execute as @e[type=minecraft:marker,tag=bjaj.egg] at @s run function bjaj:z/egg\n"
			+ "execute at @e[tag=bjaj.carrier] as @e[type=minecraft:area_effect_cloud,tag=!bjaj.ac,distance=..48,nbt={Age:0,Duration:600}] run data merge entity @s {Duration:160,RadiusPerTick:0.0125f,Tags:[\"bjaj.ac\"]}\n"
			+ "function #bjaj:tick\n");
		out.put(FN + "z/egg.mcfunction", "function #bjaj:egg\nkill @s\n");
		out.put(FN + "z/discard.mcfunction", "data merge entity @s {DeathLootTable:\"minecraft:empty\"}\ntp @s ~ -1000 ~\nkill @s\n");
		out.put(FN + "z/hit.mcfunction", "advancement revoke @s only bjaj:z/hit\nexecute on attacker run scoreboard players set @s[tag=bjaj.carrier] bjaj.ev 1\n");
		out.put(FN + "z/hurt.mcfunction", "advancement revoke @s only bjaj:z/hurt\nexecute as @e[tag=bjaj.carrier,distance=..64,nbt={HurtTime:10s}] run scoreboard players set @s bjaj.ev 2\n");
		out.put(DATA + PACK + "/advancement/z/hit.json", "{\"criteria\":{\"a\":{\"trigger\":\"minecraft:entity_hurt_player\",\"conditions\":{\"damage\":{\"source_entity\":{\"nbt\":\"" + CARRIER_NBT + "\"}}}}},\"rewards\":{\"function\":\"bjaj:z/hit\"}}\n");
		out.put(DATA + PACK + "/advancement/z/hurt.json", "{\"criteria\":{\"a\":{\"trigger\":\"minecraft:player_hurt_entity\",\"conditions\":{\"entity\":{\"nbt\":\"" + CARRIER_NBT + "\"}}}},\"rewards\":{\"function\":\"bjaj:z/hurt\"}}\n");
		return out;
	}

	public static Map<String, String> modelFiles(AjData m, String missingText) {
		return modelFiles(m, List.of(), r -> missingText);
	}

	public static Map<String, String> modelFiles(AjData m, List<AjData.Rig> rigs, java.util.function.Function<String, String> missingText) {
		Map<String, String> out = new LinkedHashMap<>();
		String dir = FN + m.id + "/";
		String p = "bjaj:" + m.id + "/z/";
		out.put(dir + "z/egg.mcfunction", "execute if entity @s[tag=bjaj.e." + m.id + "] facing entity @p feet rotated ~ 0 run function bjaj:" + m.id + "/summon\n");
		if (!m.mob()) {
			Form f0 = new Form(0, m.rig, m.args, m.pause, m.variant, m.anims, m.tags, m.ticks, AjData.findRig(rigs, m.rig));
			String rig = "function " + f0.rig + "/";
			String summonRig = rig + "summon" + (f0.args ? " {args:{}}" : "") + "\n";
			String roots = "@e[type=minecraft:item_display,tag=bjaj.r." + m.id;
			String variant = f0.variant.isEmpty() ? "" : rig + f0.variant + "\n";
			out.put(dir + "summon.mcfunction", summonRig + NEW_ROOT + p + "root_init\n");
			out.put(dir + "z/root_init.mcfunction", "tag @s add bjaj.root\ntag @s add bjaj.r." + m.id + "\n" + variant + play(f0, AjData.IDLE));
			out.put(dir + "z/tick.mcfunction", replay(m.loop, f0, AjData.IDLE, "as " + roots + "] "));
			out.put(dir + "remove.mcfunction", "bossbar remove bjaj:" + m.id + "\nexecute as " + roots + "] at @s run " + rig + "remove/this\n");
			return out;
		}
		List<Form> forms = new ArrayList<>();
		forms.add(new Form(0, m.rig, m.args, m.pause, m.variant, m.anims, m.tags, m.ticks, AjData.findRig(rigs, m.rig)));
		int phaseCount = m.boss ? m.phases.size() + 1 : 1;
		int[] phaseForm = new int[phaseCount];
		for (int phIdx = 1; phIdx < phaseCount; phIdx++) {
			AjData.Phase ph = m.phases.get(phIdx - 1);
			if (ph.changeRig) {
				int fIdx = forms.size();
				forms.add(new Form(fIdx, ph.rig, ph.args, ph.pause, ph.variant, ph.anims, ph.tags, ph.ticks, AjData.findRig(rigs, ph.rig)));
				phaseForm[phIdx] = fIdx;
			} else {
				phaseForm[phIdx] = phaseForm[phIdx - 1];
			}
		}
		String carrier = carrier(m.carrier);
		String nbt = nbt(m.nbt);
		String scale = scale(m.scale);
		String health = health(m.health);
		String loot = m.loot == AjData.LOOT_FOLLOW ? "" : ",DeathLootTable:\"" + (m.loot == AjData.LOOT_NONE ? "minecraft:empty" : PACK + ":" + m.id) + "\"";
		if (m.loot == AjData.LOOT_CUSTOM) out.put(lootFile(m.id), lootTable(m));
		String mine = "] if score @s" + ID + " = #id" + ID + " run ";
		String mineAt = "] if score @s" + ID + " = #id" + ID + " at @s run ";
		boolean hasBar = m.boss && m.bossBar;
		String barId = "bjaj:" + m.id;
		StringBuilder summon = new StringBuilder();
		if (hasBar) {
			Component title = m.bossName.isBlank() ? Component.literal(m.id) : SectionText.toComponent(ampersandCodes(m.bossName), Style.EMPTY);
			String titleArg = ShopCompat.textArg(title);
			summon.append("bossbar add ").append(barId).append(" ").append(titleArg).append("\n");
			summon.append("bossbar set ").append(barId).append(" name ").append(titleArg).append("\n");
			summon.append("bossbar set ").append(barId).append(" color ").append(AjData.BOSS_COLORS[m.bossColor]).append("\n");
			summon.append("bossbar set ").append(barId).append(" style ").append(AjData.BOSS_STYLES[m.bossStyle]).append("\n");
		}
		summon.append("scoreboard players add #next").append(ID).append(" 1\n");
		summon.append("scoreboard players operation #id").append(ID).append(" = #next").append(ID).append("\n");
		summon.append("scoreboard players set #ok").append(ID).append(" 0\n");
		summon.append("summon ").append(carrier).append(" ~ ~ ~ {Tags:[\"bjaj.new\"]}\n");
		summon.append("execute as @e[type=").append(carrier).append(",tag=bjaj.new,distance=..1,limit=1,sort=nearest] run function ").append(p).append("carrier_init\n");
		out.put(dir + "summon.mcfunction", summon.toString());

		StringBuilder cInit = new StringBuilder();
		cInit.append("scoreboard players operation @s").append(ID).append(" = #id").append(ID).append("\n");
		cInit.append("scoreboard players set @s bjaj.ph 0\n");
		cInit.append("scoreboard players set @s bjaj.pt 0\n");
		cInit.append("scoreboard players set @s bjaj.sk -1\n");
		cInit.append("scoreboard players set @s bjaj.sw 0\n");
		for (int i = 0; i < m.skills.size(); i++) {
			AjData.Skill sk = m.skills.get(i);
			int initVal = sk.trigger == AjData.TRIG_TIMER ? intRange(sk.value, 1, AjData.MAX_TICKS) : 0;
			cInit.append("scoreboard players set @s bjaj.s").append(i).append(" ").append(Math.max(0, initVal)).append("\n");
		}
		cInit.append("tp @s ~ ~ ~ ~ 0\n");
		if (!nbt.isEmpty()) cInit.append("data merge entity @s ").append(nbt).append("\n");
		if (!health.isEmpty()) cInit.append("attribute @s minecraft:max_health base set ").append(health).append("\n");
		cInit.append("data merge entity @s {PersistenceRequired:1b").append(m.silent ? ",Silent:1b" : "").append(health.isEmpty() ? "" : ",Health:" + health + "f").append(loot).append("}\n");
		cInit.append("effect give @s minecraft:invisibility infinite 0 true\n");
		if (!scale.equals("1")) cInit.append("attribute @s minecraft:scale base set ").append(scale).append("\n");
		cInit.append("tag @s add bjaj.carrier\n");
		cInit.append("tag @s add bjaj.c.").append(m.id).append("\n");
		cInit.append("tag @s remove bjaj.new\n");
		cInit.append("function ").append(p).append("f0/remodel\n");
		out.put(dir + "z/carrier_init.mcfunction", cInit.toString());

		for (Form f : forms) writeFormFiles(out, dir, p, m, f, forms, mine, missingText);

		StringBuilder tickFn = new StringBuilder();
		if (hasBar) {
			String bTag = "bjaj.b." + m.id;
			tickFn.append("tag @a[tag=").append(bTag).append("] remove ").append(bTag).append("\n");
			tickFn.append("execute as @e[tag=bjaj.c.").append(m.id).append("] at @s run function ").append(p).append("bar\n");
			tickFn.append("execute if entity @a[tag=").append(bTag).append("] run bossbar set ").append(barId).append(" players @a[tag=").append(bTag).append("]\n");
			tickFn.append("execute if entity @a[tag=").append(bTag).append("] run bossbar set ").append(barId).append(" visible true\n");
			tickFn.append("execute unless entity @a[tag=").append(bTag).append("] run bossbar set ").append(barId).append(" players\n");
			tickFn.append("execute unless entity @a[tag=").append(bTag).append("] run bossbar set ").append(barId).append(" visible false\n");
			out.put(dir + "z/bar.mcfunction",
				"tag @a[distance=..64] add " + bTag + "\n"
				+ "execute store result bossbar " + barId + " max run attribute @s minecraft:max_health get 10\n"
				+ "execute store result bossbar " + barId + " value run data get entity @s Health 10\n");
		}
		tickFn.append("execute as @e[tag=bjaj.c.").append(m.id).append("] at @s run function ").append(p).append("carrier\n");
		for (Form f : forms) {
			tickFn.append("execute as @e[type=minecraft:item_display,tag=bjaj.r.").append(m.id).append(".").append(f.index).append("] at @s run function ").append(p).append("f").append(f.index).append("/root\n");
		}
		out.put(dir + "z/tick.mcfunction", tickFn.toString());

		boolean needPct = phaseCount > 1;
		boolean hasLightning = false;
		for (AjData.Skill sk : m.skills) {
			needPct |= sk.trigger == AjData.TRIG_HP;
			hasLightning |= sk.kind == AjData.SKILL_LIGHTNING && intRange(sk.windup, 0, 1200) > 0;
		}
		if (hasLightning) {
			out.put(dir + "z/lt_clear.mcfunction", "execute as @e[type=minecraft:marker,tag=bjaj.lt." + m.id + mine + "kill @s\n");
		}

		StringBuilder carrierFn = new StringBuilder();
		carrierFn.append("scoreboard players operation #id").append(ID).append(" = @s").append(ID).append("\n");
		carrierFn.append("scoreboard players operation #ev").append(ID).append(" = @s bjaj.ev\n");
		carrierFn.append("scoreboard players set @s bjaj.ev 0\n");
		carrierFn.append("execute store result score #hp").append(ID).append(" run data get entity @s Health 1000\n");
		if (hasLightning) {
			carrierFn.append("execute if score #hp").append(ID).append(" matches ..0 run return run function ").append(p).append("lt_clear\n");
		} else {
			carrierFn.append("execute if score #hp").append(ID).append(" matches ..0 run return 0\n");
		}
		if (needPct) {
			carrierFn.append("execute store result score #mx").append(ID).append(" run attribute @s minecraft:max_health get 10\n");
			carrierFn.append("scoreboard players set #one").append(ID).append(" 1\n");
			carrierFn.append("scoreboard players operation #pct").append(ID).append(" = #hp").append(ID).append("\n");
			carrierFn.append("scoreboard players operation #pct").append(ID).append(" += #mx").append(ID).append("\n");
			carrierFn.append("scoreboard players operation #pct").append(ID).append(" -= #one").append(ID).append("\n");
			carrierFn.append("scoreboard players operation #pct").append(ID).append(" /= #mx").append(ID).append("\n");
		}
		if (phaseCount > 1) {
			writePhases(out, dir, p, m, forms, phaseForm, mine, hasLightning, carrierFn);
		}
		if (forms.size() == 1) {
			carrierFn.append("function ").append(p).append("f0/sync\n");
		} else {
			carrierFn.append("scoreboard players set #fm").append(ID).append(" 0\n");
			for (int phIdx = 1; phIdx < phaseCount; phIdx++) {
				carrierFn.append("execute if score @s bjaj.ph matches ").append(phIdx).append(" run scoreboard players set #fm").append(ID).append(" ").append(phaseForm[phIdx]).append("\n");
				AjData.Phase ph = m.phases.get(phIdx - 1);
				if (ph.changeRig && !ph.transOnNew && !ph.transAnim.isEmpty()) {
					carrierFn.append("execute if score @s bjaj.ph matches ").append(phIdx).append(" if score @s bjaj.pt matches 1.. run scoreboard players set #fm").append(ID).append(" ").append(phaseForm[phIdx - 1]).append("\n");
				}
			}
			for (Form f : forms) {
				carrierFn.append("execute if score #fm").append(ID).append(" matches ").append(f.index).append(" run function ").append(p).append("f").append(f.index).append("/sync\n");
			}
		}
		if (!m.skills.isEmpty()) {
			carrierFn.append("execute if score @s bjaj.pt matches 0 run function ").append(p).append("skills\n");
			writeSkills(out, dir, p, m, forms, phaseForm, mine, mineAt, hasLightning);
		}
		out.put(dir + "z/carrier.mcfunction", carrierFn.toString());

		StringBuilder rm = new StringBuilder();
		rm.append("bossbar remove ").append(barId).append("\n");
		rm.append("execute as @e[tag=bjaj.c.").append(m.id).append("] run function bjaj:z/discard\n");
		rm.append("kill @e[type=minecraft:marker,tag=bjaj.lt.").append(m.id).append("]\n");
		for (Form f : forms) {
			rm.append("execute as @e[type=minecraft:item_display,tag=bjaj.r.").append(m.id).append(".").append(f.index).append("] at @s run function ").append(f.rig).append("/remove/this\n");
		}
		out.put(dir + "remove.mcfunction", rm.toString());
		return out;
	}

	private static void writeFormFiles(Map<String, String> out, String dir, String p, AjData m, Form f, List<Form> allForms, String mine, java.util.function.Function<String, String> missingText) {
		String fd = dir + "z/f" + f.index + "/";
		String fp = p + "f" + f.index + "/";
		String rig = "function " + f.rig + "/";
		String summonRig = rig + "summon" + (f.args ? " {args:{}}" : "") + "\n";
		String roots = "@e[type=minecraft:item_display,tag=bjaj.r." + m.id + "." + f.index;
		String variant = f.variant.isEmpty() ? "" : rig + f.variant + "\n";
		out.put(fd + "remodel.mcfunction",
			"scoreboard players set #ok" + ID + " 0\n"
			+ summonRig
			+ NEW_ROOT + fp + "root_init\n"
			+ "execute if score #ok" + ID + " matches 0 run tellraw @a[distance=..32] {\"text\":" + new JsonPrimitive(missingText.apply(f.rig)) + ",\"color\":\"red\"}\n"
			+ "execute if score #ok" + ID + " matches 0 run function bjaj:z/discard\n");
		out.put(fd + "root_init.mcfunction",
			"scoreboard players operation @s" + ID + " = #id" + ID + "\n"
			+ "scoreboard players set @s bjaj.live 2\n"
			+ "scoreboard players set @s bjaj.st -1\n"
			+ "scoreboard players set @s bjaj.t 0\n"
			+ "scoreboard players set @s bjaj.still 4\n"
			+ "data modify entity @s teleport_duration set value 1\n"
			+ "tag @s add bjaj.root\n"
			+ "tag @s add bjaj.r." + m.id + "\n"
			+ "tag @s add bjaj.r." + m.id + "." + f.index + "\n"
			+ variant
			+ "scoreboard players set #ok" + ID + " 1\n");
		StringBuilder sync = new StringBuilder();
		sync.append("scoreboard players set #moved").append(ID).append(" 1\n");
		sync.append("scoreboard players set #found").append(ID).append(" 0\n");
		sync.append("execute as ").append(roots).append(",distance=..0.03").append(mine).append("scoreboard players set #moved").append(ID).append(" 0\n");
		sync.append("execute as ").append(roots).append(",distance=..24").append(mine).append("function ").append(fp).append("follow\n");
		sync.append("execute if score #found").append(ID).append(" matches 0 as ").append(roots).append(",distance=0..").append(mine).append("function ").append(fp).append("follow\n");
		for (Form other : allForms) {
			if (other.index == f.index) continue;
			sync.append("execute if score #found").append(ID).append(" matches 0 as @e[type=minecraft:item_display,tag=bjaj.r.").append(m.id).append(".").append(other.index).append("] if score @s").append(ID).append(" = #id").append(ID).append(" at @s run function ").append(other.rig).append("/remove/this\n");
		}
		sync.append("execute if score #found").append(ID).append(" matches 0 run function ").append(fp).append("remodel\n");
		out.put(fd + "sync.mcfunction", sync.toString());

		StringBuilder follow = new StringBuilder();
		follow.append("scoreboard players set #found").append(ID).append(" 1\n");
		follow.append("tp @s ~ ~ ~ ~ 0\n");
		follow.append("scoreboard players set @s bjaj.live 2\n");
		if (!f.anims[AjData.ATTACK].isEmpty()) follow.append("execute unless score @s bjaj.st matches 5..6 if score #ev").append(ID).append(" matches 1 run function ").append(fp).append("st_attack\n");
		if (!f.anims[AjData.HURT].isEmpty()) follow.append("execute unless score @s bjaj.st matches 5..6 if score #ev").append(ID).append(" matches 2 run function ").append(fp).append("st_hurt\n");
		follow.append("execute if score @s bjaj.t matches 1.. run return run scoreboard players remove @s bjaj.t 1\n");
		if (f.anims[AjData.WALK].isEmpty()) {
			follow.append("execute unless score @s bjaj.st matches 0 run function ").append(fp).append("st_idle\n");
		} else {
			follow.append("execute if score #moved").append(ID).append(" matches 1 run scoreboard players set @s bjaj.still 0\n");
			follow.append("execute if score #moved").append(ID).append(" matches 0 if score @s bjaj.still matches ..99 run scoreboard players add @s bjaj.still 1\n");
			follow.append("execute if score @s bjaj.still matches ..3 unless score @s bjaj.st matches 1 run return run function ").append(fp).append("st_walk\n");
			follow.append("execute if score @s bjaj.still matches 4.. unless score @s bjaj.st matches 0 run function ").append(fp).append("st_idle\n");
			out.put(fd + "st_walk.mcfunction", "scoreboard players set @s bjaj.st 1\n" + play(f, AjData.WALK));
		}
		follow.append(replay(m.loop, f, AjData.IDLE, "if score @s bjaj.st matches 0 "));
		follow.append(replay(m.loop, f, AjData.WALK, "if score @s bjaj.st matches 1 "));
		out.put(fd + "follow.mcfunction", follow.toString());
		out.put(fd + "st_idle.mcfunction", "scoreboard players set @s bjaj.st 0\n" + play(f, AjData.IDLE));
		if (!f.anims[AjData.ATTACK].isEmpty()) out.put(fd + "st_attack.mcfunction", timed(f, AjData.ATTACK, 2));
		if (!f.anims[AjData.HURT].isEmpty()) out.put(fd + "st_hurt.mcfunction", timed(f, AjData.HURT, 3));
		out.put(fd + "root.mcfunction",
			"execute if score @s bjaj.st matches 4 run return run function " + fp + "dying\n"
			+ "scoreboard players remove @s bjaj.live 1\n"
			+ "execute if score @s bjaj.live matches ..0 run function " + fp + "die\n");
		out.put(fd + "die.mcfunction", f.anims[AjData.DEATH].isEmpty() ? rig + "remove/this\n" : timed(f, AjData.DEATH, 4));
		out.put(fd + "dying.mcfunction", "scoreboard players remove @s bjaj.t 1\nexecute if score @s bjaj.t matches ..0 run " + rig + "remove/this\n");
	}

	private static void writePhases(Map<String, String> out, String dir, String p, AjData m, List<Form> forms, int[] phaseForm, String mine, boolean hasLightning, StringBuilder carrierFn) {
		boolean anyDelayedSwap = false;
		for (int phIdx = 1; phIdx <= m.phases.size(); phIdx++) {
			AjData.Phase ph = m.phases.get(phIdx - 1);
			anyDelayedSwap |= ph.changeRig && !ph.transOnNew && !ph.transAnim.isEmpty();
		}
		carrierFn.append("execute if score @s bjaj.pt matches 1.. run effect give @s minecraft:resistance 1 4 true\n");
		carrierFn.append("execute if score @s bjaj.pt matches 1.. run function ").append(p).append("pt_tick\n");
		StringBuilder ptTick = new StringBuilder();
		ptTick.append("scoreboard players remove @s bjaj.pt 1\n");
		if (anyDelayedSwap) {
			for (int phIdx = 1; phIdx <= m.phases.size(); phIdx++) {
				AjData.Phase ph = m.phases.get(phIdx - 1);
				if (ph.changeRig && !ph.transOnNew && !ph.transAnim.isEmpty()) {
					ptTick.append("execute if score @s bjaj.pt matches 0 if score @s bjaj.ph matches ").append(phIdx).append(" run function ").append(p).append("swap_").append(phIdx).append("\n");
				}
			}
		}
		out.put(dir + "z/pt_tick.mcfunction", ptTick.toString());

		for (int phIdx = 1; phIdx <= m.phases.size(); phIdx++) {
			AjData.Phase ph = m.phases.get(phIdx - 1);
			int hpPct = Math.max(1, Math.min(99, intRange(ph.hp, 1, 99)));
			carrierFn.append("execute if score @s bjaj.pt matches 0 if score @s bjaj.ph matches ").append(phIdx - 1)
				.append(" if score #pct").append(ID).append(" matches ..").append(hpPct)
				.append(" run function ").append(p).append("enter_").append(phIdx).append("\n");
			Form oldForm = forms.get(phaseForm[phIdx - 1]);
			Form newForm = forms.get(phaseForm[phIdx]);
			StringBuilder enter = new StringBuilder();
			enter.append("scoreboard players set @s bjaj.ph ").append(phIdx).append("\n");
			enter.append("scoreboard players set @s bjaj.sk -1\n");
			enter.append("scoreboard players set @s bjaj.sw 0\n");
			if (hasLightning) enter.append("function ").append(p).append("lt_clear\n");
			int dur = Math.max(1, ph.transTicks);
			if (!ph.changeRig) {
				if (!ph.transAnim.isEmpty() && oldForm.hasAnim(ph.transAnim)) {
					enter.append("scoreboard players set @s bjaj.pt ").append(dur).append("\n");
					enter.append("effect give @s minecraft:resistance 1 4 true\n");
					out.put(dir + "z/tr_" + phIdx + ".mcfunction", playCustom(oldForm, ph.transAnim, dur, 5));
					enter.append("execute as @e[type=minecraft:item_display,tag=bjaj.r.").append(m.id).append(".").append(oldForm.index).append(mine).append("function ").append(p).append("tr_").append(phIdx).append("\n");
				}
			} else if (!ph.transOnNew && !ph.transAnim.isEmpty() && oldForm.hasAnim(ph.transAnim)) {
				enter.append("scoreboard players set @s bjaj.pt ").append(dur).append("\n");
				enter.append("effect give @s minecraft:resistance 1 4 true\n");
				out.put(dir + "z/tr_" + phIdx + ".mcfunction", playCustom(oldForm, ph.transAnim, dur, 5));
				enter.append("execute as @e[type=minecraft:item_display,tag=bjaj.r.").append(m.id).append(".").append(oldForm.index).append(mine).append("function ").append(p).append("tr_").append(phIdx).append("\n");
				out.put(dir + "z/swap_" + phIdx + ".mcfunction",
					"execute as @e[type=minecraft:item_display,tag=bjaj.r." + m.id + "." + oldForm.index + "] if score @s" + ID + " = #id" + ID + " at @s run function " + oldForm.rig + "/remove/this\n"
					+ "function " + p + "f" + newForm.index + "/remodel\n");
			} else {
				enter.append("execute as @e[type=minecraft:item_display,tag=bjaj.r.").append(m.id).append(".").append(oldForm.index).append("] if score @s").append(ID).append(" = #id").append(ID).append(" at @s run function ").append(oldForm.rig).append("/remove/this\n");
				enter.append("function ").append(p).append("f").append(newForm.index).append("/remodel\n");
				if (!ph.transAnim.isEmpty() && newForm.hasAnim(ph.transAnim)) {
					enter.append("scoreboard players set @s bjaj.pt ").append(dur).append("\n");
					enter.append("effect give @s minecraft:resistance 1 4 true\n");
					out.put(dir + "z/tr_" + phIdx + ".mcfunction", playCustom(newForm, ph.transAnim, dur, 5));
					enter.append("execute as @e[type=minecraft:item_display,tag=bjaj.r.").append(m.id).append(".").append(newForm.index).append(mine).append("function ").append(p).append("tr_").append(phIdx).append("\n");
				}
			}
			out.put(dir + "z/enter_" + phIdx + ".mcfunction", enter.toString());
		}
	}

	private static void writeSkills(Map<String, String> out, String dir, String p, AjData m, List<Form> forms, int[] phaseForm, String mine, String mineAt, boolean hasLightning) {
		StringBuilder skillsFn = new StringBuilder();
		for (int i = 0; i < m.skills.size(); i++) {
			AjData.Skill s = m.skills.get(i);
			String phaseGuard = s.phase > 0 && m.boss ? "if score @s bjaj.ph matches " + (s.phase - 1) + " " : "";
			String obj = "bjaj.s" + i;
			if (s.trigger == AjData.TRIG_TIMER || s.trigger == AjData.TRIG_NEAR) {
				skillsFn.append("execute ").append(phaseGuard).append("if score @s ").append(obj).append(" matches 1.. run scoreboard players remove @s ").append(obj).append(" 1\n");
			} else if (s.trigger == AjData.TRIG_HURT) {
				skillsFn.append("execute ").append(phaseGuard).append("if score #ev").append(ID).append(" matches 2 run scoreboard players add @s ").append(obj).append(" 1\n");
			} else if (s.trigger == AjData.TRIG_HIT) {
				skillsFn.append("execute ").append(phaseGuard).append("if score #ev").append(ID).append(" matches 1 run scoreboard players add @s ").append(obj).append(" 1\n");
			}
		}
		skillsFn.append("execute if score @s bjaj.sk matches 0.. run return run function ").append(p).append("sk_wind\n");
		skillsFn.append("scoreboard players set #cast").append(ID).append(" 0\n");

		StringBuilder windFn = new StringBuilder();
		if (hasLightning) {
			windFn.append("execute as @e[type=minecraft:marker,tag=bjaj.lt.").append(m.id).append(mineAt).append("particle minecraft:electric_spark ~ ~0.15 ~ 0.65 0.1 0.65 0.02 6 force\n");
		}
		windFn.append("scoreboard players remove @s bjaj.sw 1\n");
		for (int i = 0; i < m.skills.size(); i++) {
			windFn.append("execute if score @s bjaj.sw matches ..0 if score @s bjaj.sk matches ").append(i).append(" run function ").append(p).append("sk_").append(i).append("_fire\n");
		}
		windFn.append("execute if score @s bjaj.sw matches ..0 run scoreboard players set @s bjaj.sk -1\n");
		out.put(dir + "z/sk_wind.mcfunction", windFn.toString());

		for (int i = 0; i < m.skills.size(); i++) {
			AjData.Skill s = m.skills.get(i);
			int val = Math.max(1, intRange(s.value, 1, AjData.MAX_TICKS));
			int range = Math.max(1, intRange(s.range, 1, 64));
			int windup = Math.max(0, intRange(s.windup, 0, 1200));
			String phaseGuard = s.phase > 0 && m.boss ? "if score @s bjaj.ph matches " + (s.phase - 1) + " " : "";
			String gamemode = s.trigger == AjData.TRIG_HURT || s.trigger == AjData.TRIG_HIT ? "gamemode=!spectator" : "gamemode=!creative,gamemode=!spectator";
			String nearTarget = "@p[distance=.." + range + "," + gamemode + "]";
			String obj = "bjaj.s" + i;
			String cond = switch (s.trigger) {
				case AjData.TRIG_TIMER, AjData.TRIG_NEAR -> "if score @s " + obj + " matches ..0 if entity " + nearTarget;
				case AjData.TRIG_HURT, AjData.TRIG_HIT -> "if score @s " + obj + " matches " + val + ".. if entity " + nearTarget;
				case AjData.TRIG_HP -> "if score @s " + obj + " matches 0 if score #pct" + ID + " matches .." + val + " if entity " + nearTarget;
				default -> "if entity " + nearTarget;
			};
			skillsFn.append("execute if score #cast").append(ID).append(" matches 0 ").append(phaseGuard).append(cond).append(" run function ").append(p).append("sk_").append(i).append("_cast\n");

			StringBuilder cast = new StringBuilder();
			cast.append("scoreboard players set #cast").append(ID).append(" 1\n");
			if (s.trigger == AjData.TRIG_TIMER || s.trigger == AjData.TRIG_NEAR) {
				cast.append("scoreboard players set @s ").append(obj).append(" ").append(val).append("\n");
			} else if (s.trigger == AjData.TRIG_HURT || s.trigger == AjData.TRIG_HIT) {
				cast.append("scoreboard players set @s ").append(obj).append(" 0\n");
			} else if (s.trigger == AjData.TRIG_HP) {
				cast.append("scoreboard players set @s ").append(obj).append(" 1\n");
			}
			cast.append("execute facing entity ").append(nearTarget).append(" feet rotated ~ 0 run tp @s ~ ~ ~ ~ ~\n");
			if (!s.anim.isEmpty()) {
				int aTicks = Math.max(1, s.animTicks);
				for (Form f : forms) {
					if (!f.hasAnim(s.anim)) continue;
					String animFn = "sk_" + i + "_a" + f.index;
					out.put(dir + "z/" + animFn + ".mcfunction", playCustom(f, s.anim, aTicks, 6));
					cast.append("execute as @e[type=minecraft:item_display,tag=bjaj.r.").append(m.id).append(".").append(f.index).append(mine).append("function ").append(p).append(animFn).append("\n");
				}
			}
			if (windup <= 0) {
				cast.append("function ").append(p).append("sk_").append(i).append("_fire\n");
			} else {
				cast.append("scoreboard players set @s bjaj.sk ").append(i).append("\n");
				cast.append("scoreboard players set @s bjaj.sw ").append(windup).append("\n");
				switch (s.kind) {
					case AjData.SKILL_SONIC_BOOM -> cast.append("playsound minecraft:entity.warden.sonic_charge hostile @a ~ ~ ~ 3 1\n");
					case AjData.SKILL_GUARDIAN_LASER -> cast.append("playsound minecraft:entity.guardian.attack hostile @a ~ ~ ~ 2 0.8\nparticle minecraft:electric_spark ~ ~1.3 ~ 0.3 0.3 0.3 0.05 12 force\n");
					case AjData.SKILL_EVOKER_FANGS -> cast.append("playsound minecraft:entity.evoker.prepare_attack hostile @a ~ ~ ~ 2 1\nparticle minecraft:enchant ~ ~1.2 ~ 0.4 0.5 0.4 0.2 20 force\n");
					case AjData.SKILL_DRAGON_BREATH -> cast.append("playsound minecraft:entity.ender_dragon.growl hostile @a ~ ~ ~ 2 1.2\nparticle minecraft:dragon_breath ~ ~1.3 ~ 0.3 0.3 0.3 0.03 15 force\n");
					case AjData.SKILL_SHOCKWAVE -> cast.append("particle minecraft:poof ~ ~0.2 ~ 0.8 0.1 0.8 0.02 16 force\n");
					case AjData.SKILL_LIGHTNING -> {
						cast.append("playsound minecraft:block.beacon.power_select hostile @a ~ ~ ~ 2 1.6\n");
						cast.append("execute at ").append(nearTarget).append(" run summon minecraft:marker ~ ~ ~ {Tags:[\"bjaj.lt\",\"bjaj.lt.").append(m.id).append("\",\"bjaj.newlt\"]}\n");
						cast.append("execute as @e[type=minecraft:marker,tag=bjaj.newlt] run scoreboard players operation @s").append(ID).append(" = #id").append(ID).append("\n");
						cast.append("tag @e[type=minecraft:marker,tag=bjaj.newlt] remove bjaj.newlt\n");
					}
					default -> {}
				}
			}
			out.put(dir + "z/sk_" + i + "_cast.mcfunction", cast.toString());
			writeSkillFire(out, dir, p, m, i, s, range, windup, gamemode, mineAt);
		}
		out.put(dir + "z/skills.mcfunction", skillsFn.toString());
	}

	private static void writeSkillFire(Map<String, String> out, String dir, String p, AjData m, int idx, AjData.Skill s, int range, int windup, String gamemode, String mineAt) {
		int power = Math.max(1, intRange(s.power, 1, 500));
		int burst = Math.max(1, Math.min(5, power));
		String nearTarget = "@p[distance=.." + range + "," + gamemode + "]";
		StringBuilder fire = new StringBuilder();
		if (s.kind == AjData.SKILL_LIGHTNING && windup > 0) {
			String strike = "sk_" + idx + "_strike";
			out.put(dir + "z/" + strike + ".mcfunction",
				"playsound minecraft:entity.lightning_bolt.thunder weather @a ~ ~ ~ 2 1\n"
				+ "playsound minecraft:entity.lightning_bolt.impact weather @a ~ ~ ~ 2 1\n"
				+ "particle minecraft:flash ~ ~1 ~ 0 0 0 0 1 force\n"
				+ "particle minecraft:end_rod ~ ~6 ~ 0.15 6 0.15 0.02 60 force\n"
				+ "particle minecraft:electric_spark ~ ~1 ~ 0.8 1 0.8 0.25 45 force\n"
				+ "execute as @a[distance=..2.4," + gamemode + "] run damage @s " + power + " minecraft:lightning_bolt by @e[tag=bjaj.cast,limit=1]\n"
				+ "kill @s\n");
			fire.append("tag @s add bjaj.cast\n");
			fire.append("execute as @e[type=minecraft:marker,tag=bjaj.lt.").append(m.id).append(mineAt).append("function ").append(p).append(strike).append("\n");
			fire.append("tag @s remove bjaj.cast\n");
			out.put(dir + "z/sk_" + idx + "_fire.mcfunction", fire.toString());
			return;
		}
		if (s.kind == AjData.SKILL_SHOCKWAVE) {
			double spread = Math.max(0.4, range * 0.35);
			String spreadStr = String.format(Locale.ROOT, "%.2f", spread);
			fire.append("playsound minecraft:entity.breeze.wind_burst hostile @a ~ ~ ~ 2 1\n");
			fire.append("particle minecraft:gust_emitter_large ~ ~0.6 ~ 0 0 0 0 1 force\n");
			fire.append("particle minecraft:cloud ~ ~0.2 ~ ").append(spreadStr).append(" 0.1 ").append(spreadStr).append(" 0.15 30 force\n");
			fire.append("particle minecraft:crit ~ ~0.5 ~ ").append(spreadStr).append(" 0.2 ").append(spreadStr).append(" 0.25 25 force\n");
			fire.append("tag @s add bjaj.cast\n");
			fire.append("execute as @a[distance=..").append(range).append(",").append(gamemode).append("] run damage @s ").append(power).append(" minecraft:mob_attack by @e[tag=bjaj.cast,limit=1]\n");
			fire.append("tag @s remove bjaj.cast\n");
			out.put(dir + "z/sk_" + idx + "_fire.mcfunction", fire.toString());
			return;
		}
		fire.append("tag @a[tag=bjaj.tgt] remove bjaj.tgt\n");
		fire.append("execute as ").append(nearTarget).append(" run tag @s add bjaj.tgt\n");
		fire.append("execute unless entity @a[tag=bjaj.tgt] run return 0\n");
		fire.append("execute facing entity @p[tag=bjaj.tgt,limit=1] feet rotated ~ 0 run tp @s ~ ~ ~ ~ ~\n");
		switch (s.kind) {
			case AjData.SKILL_DRAGON_BREATH -> {
				String projFn = "sk_" + idx + "_proj";
				out.put(dir + "z/" + projFn + ".mcfunction", projectileLaunch("minecraft:dragon_fireball", "", "0.0018"));
				fire.append("playsound minecraft:entity.ender_dragon.shoot hostile @a ~ ~ ~ 2 1\n");
				for (int yaw : spreadOffsets(burst)) {
					fire.append("execute anchored feet positioned ~ ~1.3 ~ facing entity @p[tag=bjaj.tgt,limit=1] feet rotated ~").append(yaw).append(" ~ run function ").append(p).append(projFn).append("\n");
				}
			}
			case AjData.SKILL_SONIC_BOOM -> {
				String rayFn = "sk_" + idx + "_ray";
				out.put(dir + "z/" + rayFn + ".mcfunction",
					"particle minecraft:sonic_boom ~ ~ ~ 0 0 0 0 1 force\n"
					+ "scoreboard players remove #step" + ID + " 1\n"
					+ "execute positioned ~ ~-1.5 ~ if entity @p[tag=bjaj.tgt,distance=..1.2] run scoreboard players set #step" + ID + " 0\n"
					+ "execute if score #step" + ID + " matches 1.. positioned ^ ^ ^1 run function " + p + rayFn + "\n");
				fire.append("playsound minecraft:entity.warden.sonic_boom hostile @a ~ ~ ~ 3 1\n");
				fire.append("scoreboard players set #step").append(ID).append(" ").append(range + 2).append("\n");
				fire.append("execute anchored feet positioned ~ ~1.4 ~ facing entity @p[tag=bjaj.tgt,limit=1] eyes positioned ^ ^ ^1 run function ").append(p).append(rayFn).append("\n");
				fire.append("damage @p[tag=bjaj.tgt,limit=1] ").append(power).append(" minecraft:sonic_boom by @s\n");
			}
			case AjData.SKILL_GUARDIAN_LASER -> {
				String rayFn = "sk_" + idx + "_ray";
				out.put(dir + "z/" + rayFn + ".mcfunction",
					"particle minecraft:bubble_pop ~ ~ ~ 0.04 0.04 0.04 0 3 force\n"
					+ "particle minecraft:electric_spark ~ ~ ~ 0.02 0.02 0.02 0 2 force\n"
					+ "scoreboard players remove #step" + ID + " 1\n"
					+ "execute positioned ~ ~-1.5 ~ if entity @p[tag=bjaj.tgt,distance=..0.9] run scoreboard players set #hit" + ID + " 1\n"
					+ "execute positioned ~ ~-0.6 ~ if entity @p[tag=bjaj.tgt,distance=..0.9] run scoreboard players set #hit" + ID + " 1\n"
					+ "execute if score #hit" + ID + " matches 0 if score #step" + ID + " matches 1.. positioned ^ ^ ^0.5 if block ~ ~ ~ #bjaj:passable run function " + p + rayFn + "\n");
				fire.append("playsound minecraft:entity.guardian.attack hostile @a ~ ~ ~ 2 1.2\n");
				fire.append("scoreboard players set #step").append(ID).append(" ").append(range * 2 + 2).append("\n");
				fire.append("scoreboard players set #hit").append(ID).append(" 0\n");
				fire.append("execute anchored feet positioned ~ ~1.3 ~ facing entity @p[tag=bjaj.tgt,limit=1] eyes positioned ^ ^ ^0.6 if block ~ ~ ~ #bjaj:passable run function ").append(p).append(rayFn).append("\n");
				fire.append("execute if score #hit").append(ID).append(" matches 1 run damage @p[tag=bjaj.tgt,limit=1] ").append(power).append(" minecraft:indirect_magic by @s\n");
				fire.append("execute if score #hit").append(ID).append(" matches 1 at @p[tag=bjaj.tgt,limit=1] run playsound minecraft:block.conduit.attack.target hostile @a ~ ~ ~ 1.5 1.4\n");
			}
			case AjData.SKILL_EVOKER_FANGS -> {
				int maxFangs = Math.max(1, (range * 4) / 5);
				String fangFn = "sk_" + idx + "_fang";
				String putFn = "sk_" + idx + "_fput";
				out.put(dir + "z/" + putFn + ".mcfunction",
					"scoreboard players set #fg" + ID + " 1\n"
					+ "summon minecraft:evoker_fangs ~ ~ ~ {Tags:[\"bjaj.pj\"]}\n"
					+ "execute store result entity @e[type=minecraft:evoker_fangs,tag=bjaj.pj,limit=1] Warmup int 2 run scoreboard players get #step" + ID + "\n"
					+ "data modify entity @e[type=minecraft:evoker_fangs,tag=bjaj.pj,limit=1] Owner set from entity @s UUID\n"
					+ "tag @e[type=minecraft:evoker_fangs,tag=bjaj.pj] remove bjaj.pj\n");
				out.put(dir + "z/" + fangFn + ".mcfunction",
					"scoreboard players set #fg" + ID + " 0\n"
					+ "execute positioned ~ ~2 ~ if block ~ ~ ~ #bjaj:passable unless block ~ ~-0.2 ~ #bjaj:passable run function " + p + putFn + "\n"
					+ "execute if score #fg" + ID + " matches 0 positioned ~ ~1 ~ if block ~ ~ ~ #bjaj:passable unless block ~ ~-0.2 ~ #bjaj:passable run function " + p + putFn + "\n"
					+ "execute if score #fg" + ID + " matches 0 if block ~ ~ ~ #bjaj:passable unless block ~ ~-0.2 ~ #bjaj:passable run function " + p + putFn + "\n"
					+ "execute if score #fg" + ID + " matches 0 positioned ~ ~-1 ~ if block ~ ~ ~ #bjaj:passable unless block ~ ~-0.2 ~ #bjaj:passable run function " + p + putFn + "\n"
					+ "execute if score #fg" + ID + " matches 0 positioned ~ ~-2 ~ if block ~ ~ ~ #bjaj:passable unless block ~ ~-0.2 ~ #bjaj:passable run function " + p + putFn + "\n"
					+ "scoreboard players add #step" + ID + " 1\n"
					+ "execute if score #step" + ID + " matches .." + (maxFangs - 1) + " positioned ^ ^ ^1.25 run function " + p + fangFn + "\n");
				fire.append("playsound minecraft:entity.evoker.cast_spell hostile @a ~ ~ ~ 2 1\n");
				for (int yaw : spreadOffsets(burst)) {
					fire.append("scoreboard players set #step").append(ID).append(" 0\n");
					fire.append("execute anchored feet facing entity @p[tag=bjaj.tgt,limit=1] feet rotated ~").append(yaw).append(" 0 positioned ^ ^ ^1.5 run function ").append(p).append(fangFn).append("\n");
				}
			}
			case AjData.SKILL_WITHER_SKULL -> {
				String projFn = "sk_" + idx + "_proj";
				out.put(dir + "z/" + projFn + ".mcfunction", projectileLaunch("minecraft:wither_skull", "", "0.0018"));
				fire.append("playsound minecraft:entity.wither.shoot hostile @a ~ ~ ~ 2 1\n");
				for (int yaw : spreadOffsets(burst)) {
					fire.append("execute anchored feet positioned ~ ~1.4 ~ facing entity @p[tag=bjaj.tgt,limit=1] eyes rotated ~").append(yaw).append(" ~ run function ").append(p).append(projFn).append("\n");
				}
			}
			case AjData.SKILL_FIREBALL -> {
				String projFn = "sk_" + idx + "_proj";
				out.put(dir + "z/" + projFn + ".mcfunction", projectileLaunch("minecraft:fireball", ",ExplosionPower:0b", "0.002"));
				fire.append("playsound minecraft:entity.blaze.shoot hostile @a ~ ~ ~ 2 1\n");
				for (int yaw : spreadOffsets(burst)) {
					fire.append("execute anchored feet positioned ~ ~1.3 ~ facing entity @p[tag=bjaj.tgt,limit=1] eyes rotated ~").append(yaw).append(" ~ run function ").append(p).append(projFn).append("\n");
				}
			}
			case AjData.SKILL_SHULKER_BULLET -> {
				String sbFn = "sk_" + idx + "_sb";
				out.put(dir + "z/" + sbFn + ".mcfunction",
					"summon minecraft:shulker_bullet ~ ~ ~ {Tags:[\"bjaj.pj\"]}\n"
					+ "data modify entity @e[type=minecraft:shulker_bullet,tag=bjaj.pj,limit=1] Owner set from entity @s UUID\n"
					+ "data modify entity @e[type=minecraft:shulker_bullet,tag=bjaj.pj,limit=1] Target set from entity @p[tag=bjaj.tgt,limit=1] UUID\n"
					+ "tag @e[type=minecraft:shulker_bullet,tag=bjaj.pj] remove bjaj.pj\n");
				fire.append("playsound minecraft:entity.shulker.shoot hostile @a ~ ~ ~ 2 1\n");
				for (int b = 0; b < burst; b++) {
					double off = (b - (burst - 1) * 0.5) * 0.6;
					String offStr = String.format(Locale.ROOT, "%.2f", off);
					fire.append("execute anchored feet facing entity @p[tag=bjaj.tgt,limit=1] eyes positioned ^").append(offStr).append(" ^1.5 ^0.8 run function ").append(p).append(sbFn).append("\n");
				}
			}
			case AjData.SKILL_LIGHTNING -> {
				fire.append("execute at @p[tag=bjaj.tgt,limit=1] run playsound minecraft:entity.lightning_bolt.thunder weather @a ~ ~ ~ 2 1\n");
				fire.append("execute at @p[tag=bjaj.tgt,limit=1] run playsound minecraft:entity.lightning_bolt.impact weather @a ~ ~ ~ 2 1\n");
				fire.append("execute at @p[tag=bjaj.tgt,limit=1] run particle minecraft:flash ~ ~1 ~ 0 0 0 0 1 force\n");
				fire.append("execute at @p[tag=bjaj.tgt,limit=1] run particle minecraft:end_rod ~ ~6 ~ 0.15 6 0.15 0.02 60 force\n");
				fire.append("execute at @p[tag=bjaj.tgt,limit=1] run particle minecraft:electric_spark ~ ~1 ~ 0.8 1 0.8 0.25 45 force\n");
				fire.append("damage @p[tag=bjaj.tgt,limit=1] ").append(power).append(" minecraft:lightning_bolt by @s\n");
			}
			case AjData.SKILL_CUSTOM -> {
				String cmd = cleanCommand(s.command);
				if (cmd != null) {
					out.put(dir + "z/sk_" + idx + "_cmd.mcfunction", cmd + "\n");
					fire.append("execute anchored feet facing entity @p[tag=bjaj.tgt,limit=1] eyes run function ").append(p).append("sk_").append(idx).append("_cmd\n");
				}
			}
			default -> {}
		}
		fire.append("tag @a[tag=bjaj.tgt] remove bjaj.tgt\n");
		out.put(dir + "z/sk_" + idx + "_fire.mcfunction", fire.toString());
	}

	private static int[] spreadOffsets(int burst) {
		return switch (burst) {
			case 2 -> new int[]{-7, 7};
			case 3 -> new int[]{-12, 0, 12};
			case 4 -> new int[]{-18, -6, 6, 18};
			case 5 -> new int[]{-24, -12, 0, 12, 24};
			default -> new int[]{0};
		};
	}

	private static String projectileLaunch(String entityId, String extraNbt, String motionScale) {
		return "summon " + entityId + " ~ ~ ~ {Tags:[\"bjaj.pj\"],acceleration_power:0.1d" + extraNbt + "}\n"
			+ "execute store result score #x0" + ID + " run data get entity @e[tag=bjaj.pj,limit=1] Pos[0] 500\n"
			+ "execute store result score #y0" + ID + " run data get entity @e[tag=bjaj.pj,limit=1] Pos[1] 500\n"
			+ "execute store result score #z0" + ID + " run data get entity @e[tag=bjaj.pj,limit=1] Pos[2] 500\n"
			+ "execute positioned ^ ^ ^1 run tp @e[tag=bjaj.pj,limit=1] ~ ~ ~\n"
			+ "execute store result score #x1" + ID + " run data get entity @e[tag=bjaj.pj,limit=1] Pos[0] 500\n"
			+ "execute store result score #y1" + ID + " run data get entity @e[tag=bjaj.pj,limit=1] Pos[1] 500\n"
			+ "execute store result score #z1" + ID + " run data get entity @e[tag=bjaj.pj,limit=1] Pos[2] 500\n"
			+ "scoreboard players operation #x1" + ID + " -= #x0" + ID + "\n"
			+ "scoreboard players operation #y1" + ID + " -= #y0" + ID + "\n"
			+ "scoreboard players operation #z1" + ID + " -= #z0" + ID + "\n"
			+ "execute store result entity @e[tag=bjaj.pj,limit=1] Motion[0] double " + motionScale + " run scoreboard players get #x1" + ID + "\n"
			+ "execute store result entity @e[tag=bjaj.pj,limit=1] Motion[1] double " + motionScale + " run scoreboard players get #y1" + ID + "\n"
			+ "execute store result entity @e[tag=bjaj.pj,limit=1] Motion[2] double " + motionScale + " run scoreboard players get #z1" + ID + "\n"
			+ "data modify entity @e[tag=bjaj.pj,limit=1] Owner set from entity @s UUID\n"
			+ "tag @e[tag=bjaj.pj] remove bjaj.pj\n";
	}

	private static String ampersandCodes(String s) {
		StringBuilder sb = new StringBuilder(s.length() + 8);
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c == '&' && i + 1 < s.length()) {
				if (s.charAt(i + 1) == '#' && i + 7 < s.length() && s.substring(i + 2, i + 8).matches("[0-9a-fA-F]{6}")) {
					sb.append("\u00a7x");
					for (int k = 2; k < 8; k++) sb.append('\u00a7').append(Character.toLowerCase(s.charAt(i + k)));
					i += 7;
					continue;
				}
				char next = Character.toLowerCase(s.charAt(i + 1));
				if ("0123456789abcdefklmnor".indexOf(next) >= 0) {
					sb.append('\u00a7').append(next);
					i++;
					continue;
				}
			}
			sb.append(c);
		}
		return sb.toString();
	}

	private static String replay(boolean loop, Form f, int slot, String condition) {
		if (!loop || f.anims[slot].isEmpty() || f.tags[slot].isEmpty()) return "";
		return "execute " + condition + "unless entity @s[tag=" + f.tags[slot] + "] run function " + f.rig + "/animations/" + f.anims[slot] + "/play\n";
	}

	private static String timed(Form f, int slot, int state) {
		return "scoreboard players set @s bjaj.st " + state + "\nscoreboard players set @s bjaj.t " + Math.max(1, f.ticks[slot]) + "\n" + play(f, slot);
	}

	private static String playCustom(Form f, String anim, int ticks, int state) {
		String rig = "function " + f.rig + "/animations/";
		return "scoreboard players set @s bjaj.st " + state + "\nscoreboard players set @s bjaj.t " + Math.max(1, ticks) + "\n"
			+ (f.pause ? rig + "pause_all\n" : "") + rig + anim + "/play\n";
	}

	private static String play(Form f, int slot) {
		String rig = "function " + f.rig + "/animations/";
		return (f.pause ? rig + "pause_all\n" : "") + (f.anims[slot].isEmpty() ? "" : rig + f.anims[slot] + "/play\n");
	}
}
