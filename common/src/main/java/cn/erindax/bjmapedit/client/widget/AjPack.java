package cn.erindax.bjmapedit.client.widget;

import cn.erindax.bjmapedit.networking.AjScan;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeSet;

public final class AjPack {
	public static final String PACK = AjScan.PACK;
	private static final String DATA = PACK + "/data/";
	private static final String FN = DATA + PACK + "/functions/";
	private static final String META = "{\n  \"pack\": {\n    \"pack_format\": 15,\n    \"description\": \"BJ-MapEdit AJ Mobs\"\n  }\n}\n";
	private static final String ID = " bjaj.id";
	private static final String NEW_ROOT = "execute as @e[type=minecraft:item_display,tag=aj.rig_root,tag=!bjaj.root,distance=..0.01,limit=1,sort=nearest] run function ";
	private static final String CARRIER_NBT = "{Tags:[\\\"bjaj.carrier\\\"]}";
	private static final String[] OBJECTIVES = {"bjaj.id", "bjaj.st", "bjaj.t", "bjaj.live", "bjaj.still", "bjaj.ev"};

	private AjPack() {}

	public static String projectFile(String id) {
		return AjScan.PROJECT_DIR + "/" + id + ".json";
	}

	public static String modelDir(String id) {
		return FN + id;
	}

	public static String lootFile(String id) {
		return DATA + PACK + "/loot_tables/" + id + ".json";
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
		return "give @s minecraft:pig_spawn_egg{EntityTag:{id:\"minecraft:marker\",Tags:[\"bjaj.egg\",\"bjaj.e." + id + "\"]},display:{Name:'\"" + id + "\"'}}";
	}

	public static String carrier(String raw) {
		ResourceLocation id = ResourceLocation.tryParse(raw == null ? "" : raw.trim());
		if (id == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(id)) return null;
		return BuiltInRegistries.ENTITY_TYPE.get(id).canSummon() ? id.toString() : null;
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

	public static String problem(AjData m) {
		if (!AjData.validId(m.id)) return "aj_bad_id";
		if (!AjData.validRig(m.rig)) return "aj_no_rig";
		if (!m.mob()) return null;
		if (carrier(m.carrier) == null) return "aj_bad_carrier";
		if (nbt(m.nbt) == null) return "aj_bad_nbt";
		if (scale(m.scale) == null) return "aj_bad_scale";
		if (health(m.health) == null) return "aj_bad_health";
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
			CompoundTag tag = stack.getTag();
			if (tag != null && !tag.isEmpty()) {
				JsonObject set = new JsonObject();
				set.addProperty("function", "minecraft:set_nbt");
				set.addProperty("tag", tag.toString());
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
		out.put(DATA + "minecraft/tags/functions/load.json", "{\"values\":[\"bjaj:z/load\"]}\n");
		out.put(DATA + "minecraft/tags/functions/tick.json", "{\"values\":[\"bjaj:z/tick\"]}\n");
		out.put(DATA + PACK + "/tags/functions/tick.json", "{\"values\":[" + ticks + "]}\n");
		out.put(DATA + PACK + "/tags/functions/egg.json", "{\"values\":[" + eggs + "]}\n");
		StringBuilder load = new StringBuilder();
		for (String objective : OBJECTIVES) load.append("scoreboard objectives add ").append(objective).append(" dummy\n");
		out.put(FN + "z/load.mcfunction", load.toString());
		out.put(FN + "z/tick.mcfunction", "execute as @e[type=minecraft:marker,tag=bjaj.egg] at @s run function bjaj:z/egg\nfunction #bjaj:tick\n");
		out.put(FN + "z/egg.mcfunction", "function #bjaj:egg\nkill @s\n");
		out.put(FN + "z/discard.mcfunction", "data merge entity @s {DeathLootTable:\"minecraft:empty\"}\ntp @s ~ -1000 ~\nkill @s\n");
		out.put(FN + "z/hit.mcfunction", "advancement revoke @s only bjaj:z/hit\nexecute on attacker run scoreboard players set @s[tag=bjaj.carrier] bjaj.ev 1\n");
		out.put(FN + "z/hurt.mcfunction", "advancement revoke @s only bjaj:z/hurt\nexecute as @e[tag=bjaj.carrier,distance=..64,nbt={HurtTime:10s}] run scoreboard players set @s bjaj.ev 2\n");
		out.put(DATA + PACK + "/advancements/z/hit.json", "{\"criteria\":{\"a\":{\"trigger\":\"minecraft:entity_hurt_player\",\"conditions\":{\"damage\":{\"source_entity\":{\"nbt\":\"" + CARRIER_NBT + "\"}}}}},\"rewards\":{\"function\":\"bjaj:z/hit\"}}\n");
		out.put(DATA + PACK + "/advancements/z/hurt.json", "{\"criteria\":{\"a\":{\"trigger\":\"minecraft:player_hurt_entity\",\"conditions\":{\"entity\":{\"nbt\":\"" + CARRIER_NBT + "\"}}}},\"rewards\":{\"function\":\"bjaj:z/hurt\"}}\n");
		return out;
	}

	public static Map<String, String> modelFiles(AjData m, String missingText) {
		Map<String, String> out = new LinkedHashMap<>();
		String dir = FN + m.id + "/";
		String p = "bjaj:" + m.id + "/z/";
		String rig = "function " + m.rig + "/";
		String summonRig = rig + "summon" + (m.args ? " {args:{}}" : "") + "\n";
		String roots = "@e[type=minecraft:item_display,tag=bjaj.r." + m.id;
		String variant = m.variant.isEmpty() ? "" : rig + m.variant + "\n";
		out.put(dir + "z/egg.mcfunction", "execute if entity @s[tag=bjaj.e." + m.id + "] facing entity @p feet rotated ~ 0 run function bjaj:" + m.id + "/summon\n");
		if (!m.mob()) {
			out.put(dir + "summon.mcfunction", summonRig + NEW_ROOT + p + "root_init\n");
			out.put(dir + "z/root_init.mcfunction", "tag @s add bjaj.root\ntag @s add bjaj.r." + m.id + "\n" + variant + play(m, AjData.IDLE));
			out.put(dir + "z/tick.mcfunction", replay(m, AjData.IDLE, "as " + roots + "] "));
			out.put(dir + "remove.mcfunction", "execute as " + roots + "] run " + rig + "remove/this\n");
			return out;
		}
		String carrier = carrier(m.carrier);
		String nbt = nbt(m.nbt);
		String health = health(m.health);
		String loot = m.loot == AjData.LOOT_FOLLOW ? "" : ",DeathLootTable:\"" + (m.loot == AjData.LOOT_NONE ? "minecraft:empty" : PACK + ":" + m.id) + "\"";
		if (m.loot == AjData.LOOT_CUSTOM) out.put(lootFile(m.id), lootTable(m));
		String mine = "] if score @s" + ID + " = #id" + ID + " run ";
		out.put(dir + "summon.mcfunction",
			"scoreboard players add #next" + ID + " 1\n"
			+ "scoreboard players operation #id" + ID + " = #next" + ID + "\n"
			+ "scoreboard players set #ok" + ID + " 0\n"
			+ "summon " + carrier + " ~ ~ ~ {Tags:[\"bjaj.new\"]}\n"
			+ "execute as @e[type=" + carrier + ",tag=bjaj.new,distance=..1,limit=1,sort=nearest] run function " + p + "carrier_init\n"
			+ "execute if score #ok" + ID + " matches 0 run tellraw @a[distance=..32] {\"text\":" + new JsonPrimitive(missingText) + ",\"color\":\"red\"}\n");
		out.put(dir + "z/carrier_init.mcfunction",
			"scoreboard players operation @s" + ID + " = #id" + ID + "\n"
			+ "tp @s ~ ~ ~ ~ 0\n"
			+ (nbt.isEmpty() ? "" : "data merge entity @s " + nbt + "\n")
			+ (health.isEmpty() ? "" : "attribute @s minecraft:generic.max_health base set " + health + "\n")
			+ "data merge entity @s {PersistenceRequired:1b" + (m.silent ? ",Silent:1b" : "") + (health.isEmpty() ? "" : ",Health:" + health + "f") + loot + "}\n"
			+ "effect give @s minecraft:invisibility infinite 0 true\n"
			+ "tag @s add bjaj.carrier\n"
			+ "tag @s add bjaj.c." + m.id + "\n"
			+ "tag @s remove bjaj.new\n"
			+ "function " + p + "remodel\n");
		out.put(dir + "z/remodel.mcfunction",
			"scoreboard players set #ok" + ID + " 0\n"
			+ summonRig
			+ NEW_ROOT + p + "root_init\n"
			+ "execute if score #ok" + ID + " matches 0 run function bjaj:z/discard\n");
		out.put(dir + "z/root_init.mcfunction",
			"scoreboard players operation @s" + ID + " = #id" + ID + "\n"
			+ "scoreboard players set @s bjaj.live 2\n"
			+ "scoreboard players set @s bjaj.st -1\n"
			+ "scoreboard players set @s bjaj.t 0\n"
			+ "scoreboard players set @s bjaj.still 4\n"
			+ "tag @s add bjaj.root\n"
			+ "tag @s add bjaj.r." + m.id + "\n"
			+ variant
			+ "scoreboard players set #ok" + ID + " 1\n");
		out.put(dir + "z/tick.mcfunction",
			"execute as @e[tag=bjaj.c." + m.id + "] at @s run function " + p + "carrier\n"
			+ "execute as " + roots + "] at @s run function " + p + "root\n");
		out.put(dir + "z/carrier.mcfunction",
			"scoreboard players operation #id" + ID + " = @s" + ID + "\n"
			+ "scoreboard players operation #ev" + ID + " = @s bjaj.ev\n"
			+ "scoreboard players set @s bjaj.ev 0\n"
			+ "scoreboard players set #moved" + ID + " 1\n"
			+ "scoreboard players set #found" + ID + " 0\n"
			+ "execute as " + roots + ",distance=..0.03" + mine + "scoreboard players set #moved" + ID + " 0\n"
			+ "execute as " + roots + ",distance=..24" + mine + "function " + p + "follow\n"
			+ "execute if score #found" + ID + " matches 0 as " + roots + ",distance=0.." + mine + "function " + p + "follow\n"
			+ "execute if score #found" + ID + " matches 0 run function " + p + "remodel\n");
		StringBuilder follow = new StringBuilder();
		follow.append("scoreboard players set #found").append(ID).append(" 1\n");
		follow.append("tp @s ~ ~ ~ ~ 0\n");
		follow.append("scoreboard players set @s bjaj.live 2\n");
		if (!m.anims[AjData.ATTACK].isEmpty()) follow.append("execute if score #ev").append(ID).append(" matches 1 run function ").append(p).append("st_attack\n");
		if (!m.anims[AjData.HURT].isEmpty()) follow.append("execute if score #ev").append(ID).append(" matches 2 run function ").append(p).append("st_hurt\n");
		follow.append("scoreboard players operation #t").append(ID).append(" = @s bjaj.t\n");
		follow.append("execute if score #t").append(ID).append(" matches 1.. run scoreboard players remove @s bjaj.t 1\n");
		follow.append("execute if score #t").append(ID).append(" matches ..0 run function ").append(p).append("move\n");
		StringBuilder move = new StringBuilder();
		if (m.anims[AjData.WALK].isEmpty()) {
			move.append("execute unless score @s bjaj.st matches 0 run function ").append(p).append("st_idle\n");
		} else {
			move.append("execute if score #moved").append(ID).append(" matches 1 run scoreboard players set @s bjaj.still 0\n");
			move.append("execute if score #moved").append(ID).append(" matches 0 if score @s bjaj.still matches ..99 run scoreboard players add @s bjaj.still 1\n");
			move.append("execute if score @s bjaj.still matches ..3 unless score @s bjaj.st matches 1 run function ").append(p).append("st_walk\n");
			move.append("execute if score @s bjaj.still matches 4.. unless score @s bjaj.st matches 0 run function ").append(p).append("st_idle\n");
			out.put(dir + "z/st_walk.mcfunction", "scoreboard players set @s bjaj.st 1\n" + play(m, AjData.WALK));
		}
		move.append(replay(m, AjData.IDLE, "if score @s bjaj.st matches 0 "));
		move.append(replay(m, AjData.WALK, "if score @s bjaj.st matches 1 "));
		out.put(dir + "z/follow.mcfunction", follow.toString());
		out.put(dir + "z/move.mcfunction", move.toString());
		out.put(dir + "z/st_idle.mcfunction", "scoreboard players set @s bjaj.st 0\n" + play(m, AjData.IDLE));
		if (!m.anims[AjData.ATTACK].isEmpty()) out.put(dir + "z/st_attack.mcfunction", timed(m, AjData.ATTACK, 2));
		if (!m.anims[AjData.HURT].isEmpty()) out.put(dir + "z/st_hurt.mcfunction", timed(m, AjData.HURT, 3));
		out.put(dir + "z/root.mcfunction",
			"scoreboard players operation #s" + ID + " = @s bjaj.st\n"
			+ "execute if score #s" + ID + " matches 4 run function " + p + "dying\n"
			+ "execute unless score #s" + ID + " matches 4 run scoreboard players remove @s bjaj.live 1\n"
			+ "execute unless score #s" + ID + " matches 4 if score @s bjaj.live matches ..0 run function " + p + "die\n");
		out.put(dir + "z/die.mcfunction", m.anims[AjData.DEATH].isEmpty() ? rig + "remove/this\n" : timed(m, AjData.DEATH, 4));
		out.put(dir + "z/dying.mcfunction", "scoreboard players remove @s bjaj.t 1\nexecute if score @s bjaj.t matches ..0 run " + rig + "remove/this\n");
		out.put(dir + "remove.mcfunction",
			"execute as @e[tag=bjaj.c." + m.id + "] run function bjaj:z/discard\n"
			+ "execute as " + roots + "] run " + rig + "remove/this\n");
		return out;
	}

	private static String replay(AjData m, int slot, String condition) {
		if (!m.loop || m.anims[slot].isEmpty() || m.tags[slot].isEmpty()) return "";
		return "execute " + condition + "unless entity @s[tag=" + m.tags[slot] + "] run function " + m.rig + "/animations/" + m.anims[slot] + "/play\n";
	}

	private static String timed(AjData m, int slot, int state) {
		return "scoreboard players set @s bjaj.st " + state + "\nscoreboard players set @s bjaj.t " + Math.max(1, m.ticks[slot]) + "\n" + play(m, slot);
	}

	private static String play(AjData m, int slot) {
		String rig = "function " + m.rig + "/animations/";
		return (m.pause ? rig + "pause_all\n" : "") + (m.anims[slot].isEmpty() ? "" : rig + m.anims[slot] + "/play\n");
	}
}
