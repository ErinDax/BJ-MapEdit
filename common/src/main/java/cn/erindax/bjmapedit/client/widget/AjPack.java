package cn.erindax.bjmapedit.client.widget;

import cn.erindax.bjmapedit.networking.AjScan;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
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
	private static final String FN = DATA + PACK + "/functions/";
	private static final String META = "{\n  \"pack\": {\n    \"pack_format\": 48,\n    \"description\": \"BJ-MapEdit AJ Mobs\"\n  }\n}\n";
	private static final String PASSABLE = "{\n  \"values\": [\n    \"#minecraft:replaceable\",\n    \"#minecraft:flowers\",\n    \"#minecraft:crops\",\n    \"#minecraft:buttons\",\n    \"#minecraft:pressure_plates\",\n    \"#minecraft:rails\",\n    \"#minecraft:all_signs\",\n    \"#minecraft:banners\",\n    \"minecraft:torch\",\n    \"minecraft:wall_torch\",\n    \"minecraft:soul_torch\",\n    \"minecraft:soul_wall_torch\",\n    \"minecraft:redstone_torch\",\n    \"minecraft:redstone_wall_torch\"\n  ]\n}\n";
	private static final String ID = " bjaj.id";
	private static final String NEW_ROOT = "execute as @e[type=minecraft:item_display,tag=aj.rig_root,tag=!bjaj.root,distance=..0.01,limit=1,sort=nearest] run function ";
	private static final String CARRIER_NBT = "{Tags:[\\\"bjaj.carrier\\\"]}";
	private static final String[] OBJECTIVES = {
		"bjaj.id", "bjaj.st", "bjaj.at", "bjaj.t", "bjaj.live", "bjaj.still", "bjaj.mv", "bjaj.ev",
		"bjaj.yaw", "bjaj.px", "bjaj.pz", "bjaj.mt", "bjaj.mcd",
		"bjaj.ph", "bjaj.pt", "bjaj.sk", "bjaj.sw",
		"bjaj.s0", "bjaj.s1", "bjaj.s2", "bjaj.s3", "bjaj.s4", "bjaj.s5", "bjaj.s6", "bjaj.s7"
	};

	private record Form(int index, String rig, boolean args, boolean pause, boolean defPose, String variant, String[] anims, String[] tags, int[] ticks, AjData.Rig rigInfo) {
		boolean hasAnim(String name) {
			return !name.isEmpty() && (rigInfo == null || rigInfo.anim(name) != null);
		}

		AjData.Anim anim(String name) {
			return rigInfo == null || name.isEmpty() ? null : rigInfo.anim(name);
		}

		boolean animExcl(String name) {
			AjData.Anim a = anim(name);
			return a != null && a.excl();
		}

		boolean animCtrl(String name) {
			AjData.Anim a = anim(name);
			return a == null || a.ctrl();
		}

		boolean animStop(String name) {
			AjData.Anim a = anim(name);
			return a != null && a.stop();
		}

		boolean hasDefPose() {
			return rigInfo != null ? rigInfo.defPose() : defPose;
		}
	}

	private record ActEntry(int code, String fn, String anim, int ticks) {}

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

	public static int intRange(String raw, int min, int max) {
		String s = raw == null ? "" : raw.trim();
		if (!s.matches("\\d{1,5}")) return -1;
		int v = Integer.parseInt(s);
		return v < min || v > max ? -1 : v;
	}

	public static double meleeRange(String raw) {
		try {
			BigDecimal value = new BigDecimal(raw == null ? "" : raw.trim());
			if (value.compareTo(new BigDecimal("0.5")) < 0 || value.compareTo(new BigDecimal("16")) > 0) return -1;
			return value.doubleValue();
		} catch (NumberFormatException e) {
			return -1;
		}
	}

	public static int maxAttackTicks(AjData m) {
		int max = !m.anims[AjData.ATTACK].isEmpty() ? Math.max(1, m.ticks[AjData.ATTACK]) : 0;
		if (m.boss) {
			for (AjData.Phase ph : m.phases) {
				if (ph.changeRig && !ph.anims[AjData.ATTACK].isEmpty()) {
					max = Math.max(max, Math.max(1, ph.ticks[AjData.ATTACK]));
				}
			}
		}
		return max > 0 ? max : AjData.MAX_TICKS;
	}

	public static List<Integer> parseHitTicks(String raw, int maxAllowed) {
		String s = raw == null ? "" : raw.trim();
		if (s.isEmpty()) return List.of();
		String[] parts = s.split("[,\\s]+");
		if (parts.length == 0 || parts.length > 6) return null;
		TreeSet<Integer> out = new TreeSet<>();
		for (String part : parts) {
			if (!part.matches("\\d{1,5}")) return null;
			int v = Integer.parseInt(part);
			if (v < 1 || v > maxAllowed) return null;
			out.add(v);
		}
		return out.isEmpty() ? null : new ArrayList<>(out);
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
		if (health(m.health) == null) return "aj_bad_health";
		if (m.mode == AjData.HOSTILE && m.meleeMode == AjData.MELEE_SYNC) {
			if (intRange(m.meleeDamage, 1, 500) < 0) return "aj_bad_melee_damage";
			if (meleeRange(m.meleeRange) < 0) return "aj_bad_melee_range";
			if (intRange(m.meleeCd, 0, AjData.MAX_TICKS) < 0) return "aj_bad_melee_cd";
			if (parseHitTicks(m.meleeHits, maxAttackTicks(m)) == null) return "aj_bad_melee_hits";
		}
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
		out.put(DATA + PACK + "/tags/blocks/passable.json", PASSABLE);
		StringBuilder load = new StringBuilder();
		for (String objective : OBJECTIVES) load.append("scoreboard objectives add ").append(objective).append(" dummy\n");
		load.append("scoreboard players set #c360").append(ID).append(" 36000\n");
		out.put(FN + "z/load.mcfunction", load.toString());
		out.put(FN + "z/tick.mcfunction",
			"execute as @e[type=minecraft:marker,tag=bjaj.egg] at @s run function bjaj:z/egg\n"
			+ "execute at @e[tag=bjaj.carrier] as @e[type=minecraft:area_effect_cloud,tag=!bjaj.ac,distance=..48,nbt={Age:0,Duration:600}] run data merge entity @s {Duration:160,RadiusPerTick:0.0125f,Tags:[\"bjaj.ac\"]}\n"
			+ "function #bjaj:tick\n");
		out.put(FN + "z/egg.mcfunction", "function #bjaj:egg\nkill @s\n");
		out.put(FN + "z/discard.mcfunction", "data merge entity @s {DeathLootTable:\"minecraft:empty\"}\ntp @s ~ -1000 ~\nkill @s\n");
		out.put(FN + "z/hit.mcfunction", "advancement revoke @s only bjaj:z/hit\nexecute on attacker run scoreboard players set @s[tag=bjaj.carrier] bjaj.ev 1\n");
		out.put(FN + "z/hurt.mcfunction", "advancement revoke @s only bjaj:z/hurt\nexecute as @e[tag=bjaj.carrier,distance=..64,nbt={HurtTime:10s}] run scoreboard players set @s bjaj.ev 2\n");
		out.put(DATA + PACK + "/advancements/z/hit.json", "{\"criteria\":{\"a\":{\"trigger\":\"minecraft:entity_hurt_player\",\"conditions\":{\"damage\":{\"source_entity\":{\"nbt\":\"" + CARRIER_NBT + "\"}}}}},\"rewards\":{\"function\":\"bjaj:z/hit\"}}\n");
		out.put(DATA + PACK + "/advancements/z/hurt.json", "{\"criteria\":{\"a\":{\"trigger\":\"minecraft:player_hurt_entity\",\"conditions\":{\"entity\":{\"nbt\":\"" + CARRIER_NBT + "\"}}}},\"rewards\":{\"function\":\"bjaj:z/hurt\"}}\n");
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
			Form f0 = new Form(0, m.rig, m.args, m.pause, m.defPose, m.variant, m.anims, m.tags, m.ticks, AjData.findRig(rigs, m.rig));
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
		forms.add(new Form(0, m.rig, m.args, m.pause, m.defPose, m.variant, m.anims, m.tags, m.ticks, AjData.findRig(rigs, m.rig)));
		int phaseCount = m.boss ? m.phases.size() + 1 : 1;
		int[] phaseForm = new int[phaseCount];
		for (int phIdx = 1; phIdx < phaseCount; phIdx++) {
			AjData.Phase ph = m.phases.get(phIdx - 1);
			if (ph.changeRig) {
				int fIdx = forms.size();
				forms.add(new Form(fIdx, ph.rig, ph.args, ph.pause, ph.defPose, ph.variant, ph.anims, ph.tags, ph.ticks, AjData.findRig(rigs, ph.rig)));
				phaseForm[phIdx] = fIdx;
			} else {
				phaseForm[phIdx] = phaseForm[phIdx - 1];
			}
		}
		String carrier = carrier(m.carrier);
		String nbt = nbt(m.nbt);
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
		summon.append("scoreboard players set #c360").append(ID).append(" 36000\n");
		summon.append("scoreboard players add #next").append(ID).append(" 1\n");
		summon.append("scoreboard players operation #id").append(ID).append(" = #next").append(ID).append("\n");
		summon.append("scoreboard players set #ok").append(ID).append(" 0\n");
		summon.append("summon ").append(carrier).append(" ~ ~ ~ {Tags:[\"bjaj.new\"]}\n");
		summon.append("execute as @e[type=").append(carrier).append(",tag=bjaj.new,distance=..1,limit=1,sort=nearest] run function ").append(p).append("carrier_init\n");
		out.put(dir + "summon.mcfunction", summon.toString());

		boolean syncMelee = m.mode == AjData.HOSTILE && m.meleeMode == AjData.MELEE_SYNC;
		boolean anySyncMelee = false;
		if (syncMelee) {
			for (Form f : forms) anySyncMelee |= !f.anims[AjData.ATTACK].isEmpty();
		}
		boolean needMoveLock = phaseCount > 1 || anySyncMelee;
		for (AjData.Skill sk : m.skills) {
			needMoveLock |= !sk.anim.isEmpty() && intRange(sk.windup, 0, 1200) > 0;
		}
		if (needMoveLock) {
			out.put(dir + "z/lock_on.mcfunction",
				"attribute @s minecraft:generic.movement_speed modifier add d8499b04-0e66-4726-ab29-64469d734e0d bjaj_lock -1 multiply\n"
				+ "tag @s add bjaj.locked\n");
			out.put(dir + "z/lock_off.mcfunction",
				"attribute @s minecraft:generic.movement_speed modifier remove d8499b04-0e66-4726-ab29-64469d734e0d\n"
				+ "tag @s remove bjaj.locked\n");
		}
		if (anySyncMelee) {
			out.put(dir + "z/nom_on.mcfunction",
				"attribute @s minecraft:generic.attack_damage modifier add d8499b04-0e66-4726-ab29-64469d734e0e bjaj_no_melee -1 multiply\n"
				+ "tag @s add bjaj.nom\n");
			out.put(dir + "z/nom_off.mcfunction",
				"attribute @s minecraft:generic.attack_damage modifier remove d8499b04-0e66-4726-ab29-64469d734e0e\n"
				+ "tag @s remove bjaj.nom\n");
		}

		StringBuilder cInit = new StringBuilder();
		cInit.append("scoreboard players operation @s").append(ID).append(" = #id").append(ID).append("\n");
		cInit.append("scoreboard players set @s bjaj.ph 0\n");
		cInit.append("scoreboard players set @s bjaj.pt 0\n");
		cInit.append("scoreboard players set @s bjaj.sk -1\n");
		cInit.append("scoreboard players set @s bjaj.sw 0\n");
		cInit.append("scoreboard players set @s bjaj.mt 0\n");
		cInit.append("scoreboard players set @s bjaj.mcd 0\n");
		for (int i = 0; i < m.skills.size(); i++) {
			AjData.Skill sk = m.skills.get(i);
			int initVal = sk.trigger == AjData.TRIG_TIMER ? intRange(sk.value, 1, AjData.MAX_TICKS) : 0;
			cInit.append("scoreboard players set @s bjaj.s").append(i).append(" ").append(Math.max(0, initVal)).append("\n");
		}
		cInit.append("tp @s ~ ~ ~ ~ 0\n");
		if (!nbt.isEmpty()) cInit.append("data merge entity @s ").append(nbt).append("\n");
		if (!health.isEmpty()) cInit.append("attribute @s minecraft:generic.max_health base set ").append(health).append("\n");
		cInit.append("data merge entity @s {PersistenceRequired:1b").append(m.silent ? ",Silent:1b" : "").append(health.isEmpty() ? "" : ",Health:" + health + "f").append(loot).append("}\n");
		cInit.append("effect give @s minecraft:invisibility infinite 0 true\n");
		if (anySyncMelee && !forms.get(0).anims[AjData.ATTACK].isEmpty()) cInit.append("function ").append(p).append("nom_on\n");
		cInit.append("execute store result score @s bjaj.px run data get entity @s Pos[0] 1000\n");
		cInit.append("execute store result score @s bjaj.pz run data get entity @s Pos[2] 1000\n");
		cInit.append("execute store result score #tyaw").append(ID).append(" run data get entity @s Rotation[0] 100\n");
		cInit.append("tag @s add bjaj.carrier\n");
		cInit.append("tag @s add bjaj.c.").append(m.id).append("\n");
		cInit.append("tag @s remove bjaj.new\n");
		cInit.append("function ").append(p).append("f0/remodel\n");
		out.put(dir + "z/carrier_init.mcfunction", cInit.toString());

		for (Form f : forms) writeFormFiles(out, dir, p, m, f, forms, mine, syncMelee, missingText);

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
				+ "execute store result bossbar " + barId + " max run attribute @s minecraft:generic.max_health get 10\n"
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

		double scaleNum = 1.0;
		double extraRad = Math.max(0.0, (scaleNum - 1.0) * 0.3);
		double mRange = syncMelee ? Math.max(0.5, meleeRange(m.meleeRange)) : 3.0;
		double trigRange = mRange + extraRad;
		double closeHold = 1.05 + extraRad;

		StringBuilder carrierFn = new StringBuilder();
		carrierFn.append("scoreboard players operation #id").append(ID).append(" = @s").append(ID).append("\n");
		carrierFn.append("scoreboard players operation #ev").append(ID).append(" = @s bjaj.ev\n");
		carrierFn.append("scoreboard players set @s bjaj.ev 0\n");
		carrierFn.append("scoreboard players set #act").append(ID).append(" 0\n");
		carrierFn.append("execute store result score #hp").append(ID).append(" run data get entity @s Health 1000\n");
		if (hasLightning) {
			carrierFn.append("execute if score #hp").append(ID).append(" matches ..0 run function ").append(p).append("lt_clear\n");
			carrierFn.append("execute if score #hp").append(ID).append(" matches ..0 run return 0\n");
		} else {
			carrierFn.append("execute if score #hp").append(ID).append(" matches ..0 run return 0\n");
		}
		carrierFn.append("execute store result score #cx").append(ID).append(" run data get entity @s Pos[0] 1000\n");
		carrierFn.append("execute store result score #cz").append(ID).append(" run data get entity @s Pos[2] 1000\n");
		carrierFn.append("scoreboard players operation #dx").append(ID).append(" = #cx").append(ID).append("\n");
		carrierFn.append("scoreboard players operation #dx").append(ID).append(" -= @s bjaj.px\n");
		carrierFn.append("scoreboard players operation #dz").append(ID).append(" = #cz").append(ID).append("\n");
		carrierFn.append("scoreboard players operation #dz").append(ID).append(" -= @s bjaj.pz\n");
		carrierFn.append("scoreboard players operation @s bjaj.px = #cx").append(ID).append("\n");
		carrierFn.append("scoreboard players operation @s bjaj.pz = #cz").append(ID).append("\n");
		carrierFn.append("scoreboard players operation #dx").append(ID).append(" *= #dx").append(ID).append("\n");
		carrierFn.append("scoreboard players operation #dz").append(ID).append(" *= #dz").append(ID).append("\n");
		carrierFn.append("scoreboard players operation #dx").append(ID).append(" += #dz").append(ID).append("\n");
		carrierFn.append("scoreboard players set #moved").append(ID).append(" 0\n");
		carrierFn.append("execute if score #dx").append(ID).append(" matches 36.. run scoreboard players set #moved").append(ID).append(" 1\n");
		carrierFn.append("execute store result score #tyaw").append(ID).append(" run data get entity @s Rotation[0] 100\n");
		carrierFn.append("scoreboard players set #aim").append(ID).append(" 0\n");
		if (m.mode == AjData.HOSTILE) {
			String closeAim = fmtDist(Math.max(3.5, trigRange));
			carrierFn.append("execute if entity @p[distance=..").append(closeAim).append(",gamemode=!creative,gamemode=!spectator] run scoreboard players set #aim").append(ID).append(" 1\n");
			carrierFn.append("execute if score #aim").append(ID).append(" matches 0 if entity @p[distance=..28,gamemode=!creative,gamemode=!spectator] anchored eyes facing entity @p[distance=..28,gamemode=!creative,gamemode=!spectator] eyes if block ^ ^ ^1 #bjaj:passable if block ^ ^ ^2.2 #bjaj:passable run scoreboard players set #aim").append(ID).append(" 1\n");
		}
		if (needPct) {
			carrierFn.append("execute store result score #mx").append(ID).append(" run attribute @s minecraft:generic.max_health get 10\n");
			carrierFn.append("scoreboard players set #one").append(ID).append(" 1\n");
			carrierFn.append("scoreboard players operation #pct").append(ID).append(" = #hp").append(ID).append("\n");
			carrierFn.append("scoreboard players operation #pct").append(ID).append(" += #mx").append(ID).append("\n");
			carrierFn.append("scoreboard players operation #pct").append(ID).append(" -= #one").append(ID).append("\n");
			carrierFn.append("scoreboard players operation #pct").append(ID).append(" /= #mx").append(ID).append("\n");
		}
		if (phaseCount > 1) {
			writePhases(out, dir, p, m, forms, phaseForm, mine, hasLightning, carrierFn);
		}
		carrierFn.append("scoreboard players set #fm").append(ID).append(" 0\n");
		if (forms.size() > 1) {
			for (int phIdx = 1; phIdx < phaseCount; phIdx++) {
				carrierFn.append("execute if score @s bjaj.ph matches ").append(phIdx).append(" run scoreboard players set #fm").append(ID).append(" ").append(phaseForm[phIdx]).append("\n");
				AjData.Phase ph = m.phases.get(phIdx - 1);
				if (ph.changeRig && !ph.transOnNew && !ph.transAnim.isEmpty()) {
					carrierFn.append("execute if score @s bjaj.ph matches ").append(phIdx).append(" if score @s bjaj.pt matches 1.. run scoreboard players set #fm").append(ID).append(" ").append(phaseForm[phIdx - 1]).append("\n");
				}
			}
		}
		if (anySyncMelee) {
			carrierFn.append("scoreboard players set #msync").append(ID).append(" 0\n");
			for (Form f : forms) {
				if (!f.anims[AjData.ATTACK].isEmpty()) {
					carrierFn.append("execute if score #fm").append(ID).append(" matches ").append(f.index).append(" run scoreboard players set #msync").append(ID).append(" 1\n");
				}
			}
		}
		if (forms.size() == 1) {
			carrierFn.append("function ").append(p).append("f0/sync\n");
		} else {
			for (Form f : forms) {
				carrierFn.append("execute if score #fm").append(ID).append(" matches ").append(f.index).append(" run function ").append(p).append("f").append(f.index).append("/sync\n");
			}
		}
		if (anySyncMelee) {
			carrierFn.append("function ").append(p).append("melee\n");
			writeMelee(out, dir, p, m, forms, mine, mRange, extraRad, trigRange);
		}
		if (!m.skills.isEmpty()) {
			carrierFn.append("execute if score @s bjaj.pt matches 0 run function ").append(p).append("skills\n");
			writeSkills(out, dir, p, m, forms, phaseForm, mine, mineAt, hasLightning);
		}
		carrierFn.append("scoreboard players set #hold").append(ID).append(" 0\n");
		if (phaseCount > 1) {
			carrierFn.append("execute if score @s bjaj.pt matches 1.. run scoreboard players set #hold").append(ID).append(" 1\n");
		}
		if (anySyncMelee) {
			carrierFn.append("execute if score #msync").append(ID).append(" matches 1 if entity @p[distance=..").append(fmtDist(closeHold)).append(",gamemode=!creative,gamemode=!spectator] run scoreboard players set #hold").append(ID).append(" 1\n");
			if (m.meleeStop) {
				String holdSel = "@p[distance=.." + fmtDist(trigRange) + ",gamemode=!creative,gamemode=!spectator]";
				if (trigRange > 2.2) {
					carrierFn.append("execute if score #msync").append(ID).append(" matches 1 if entity ").append(holdSel)
						.append(" anchored eyes facing entity ").append(holdSel).append(" eyes if block ^ ^ ^1 #bjaj:passable run scoreboard players set #hold").append(ID).append(" 1\n");
				} else {
					carrierFn.append("execute if score #msync").append(ID).append(" matches 1 if entity ").append(holdSel)
						.append(" run scoreboard players set #hold").append(ID).append(" 1\n");
				}
				carrierFn.append("execute if score @s bjaj.mt matches 1.. run scoreboard players set #hold").append(ID).append(" 1\n");
			}
		}
		if (needMoveLock) {
			for (int i = 0; i < m.skills.size(); i++) {
				AjData.Skill sk = m.skills.get(i);
				if (!sk.anim.isEmpty() && intRange(sk.windup, 0, 1200) > 0) {
					carrierFn.append("execute if score @s bjaj.sk matches ").append(i).append(" run scoreboard players set #hold").append(ID).append(" 1\n");
				}
			}
			carrierFn.append("execute if score #hold").append(ID).append(" matches 1 if entity @s[tag=!bjaj.locked] run function ").append(p).append("lock_on\n");
			carrierFn.append("execute if score #hold").append(ID).append(" matches 0 if entity @s[tag=bjaj.locked] run function ").append(p).append("lock_off\n");
		}
		if (forms.size() == 1) {
			carrierFn.append("execute as @e[type=minecraft:item_display,tag=bjaj.r.").append(m.id).append(".0,distance=..8").append(mineAt).append("function ").append(p).append("f0/anim\n");
		} else {
			for (Form f : forms) {
				carrierFn.append("execute if score #fm").append(ID).append(" matches ").append(f.index).append(" as @e[type=minecraft:item_display,tag=bjaj.r.").append(m.id).append(".").append(f.index).append(",distance=..8").append(mineAt).append("function ").append(p).append("f").append(f.index).append("/anim\n");
			}
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

	private static String fmtDist(double v) {
		return String.format(Locale.ROOT, "%.2f", v);
	}

	private static List<ActEntry> formActions(AjData m, Form f) {
		List<ActEntry> out = new ArrayList<>();
		if (!f.anims[AjData.ATTACK].isEmpty()) out.add(new ActEntry(2, "st_attack", f.anims[AjData.ATTACK], Math.max(1, f.ticks[AjData.ATTACK])));
		if (!f.anims[AjData.HURT].isEmpty()) out.add(new ActEntry(3, "st_hurt", f.anims[AjData.HURT], Math.max(1, f.ticks[AjData.HURT])));
		for (int i = 0; i < m.skills.size(); i++) {
			AjData.Skill s = m.skills.get(i);
			if (!s.anim.isEmpty() && f.hasAnim(s.anim)) {
				out.add(new ActEntry(6 + i, "sk_" + i + "_anim", s.anim, Math.max(1, s.animTicks)));
			}
		}
		return out;
	}

	private static void appendResumeActions(StringBuilder sb, String rig, Form f, List<ActEntry> acts, String skipAnim) {
		for (ActEntry a : acts) {
			if (!a.anim.equals(skipAnim) && f.animCtrl(a.anim)) {
				sb.append("execute if score @s bjaj.at matches ").append(a.code).append(" run ").append(rig).append("animations/").append(a.anim).append("/resume\n");
			}
		}
	}

	private static void writeFormFiles(Map<String, String> out, String dir, String p, AjData m, Form f, List<Form> allForms, String mine, boolean syncMelee, java.util.function.Function<String, String> missingText) {
		String fd = dir + "z/f" + f.index + "/";
		String fp = p + "f" + f.index + "/";
		String rig = "function " + f.rig + "/";
		String summonRig = rig + "summon" + (f.args ? " {args:{}}" : "") + "\n";
		String roots = "@e[type=minecraft:item_display,tag=bjaj.r." + m.id + "." + f.index;
		String variant = f.variant.isEmpty() ? "" : rig + f.variant + "\n";
		boolean formSyncMelee = syncMelee && !f.anims[AjData.ATTACK].isEmpty();
		List<ActEntry> acts = formActions(m, f);
		out.put(fd + "remodel.mcfunction",
			"scoreboard players set #ok" + ID + " 0\n"
			+ summonRig
			+ NEW_ROOT + fp + "root_init\n"
			+ "execute if score #ok" + ID + " matches 0 run tellraw @a[distance=..32] {\"text\":" + new JsonPrimitive(missingText.apply(f.rig)) + ",\"color\":\"red\"}\n"
			+ "execute if score #ok" + ID + " matches 0 run function bjaj:z/discard\n");
		out.put(fd + "root_init.mcfunction",
			"scoreboard players operation @s" + ID + " = #id" + ID + "\n"
			+ "scoreboard players operation @s bjaj.yaw = #tyaw" + ID + "\n"
			+ "scoreboard players set @s bjaj.live 2\n"
			+ "scoreboard players set @s bjaj.st -1\n"
			+ "scoreboard players set @s bjaj.at 0\n"
			+ "scoreboard players set @s bjaj.t 0\n"
			+ "scoreboard players set @s bjaj.still 7\n"
			+ "scoreboard players set @s bjaj.mv 0\n"
			+ "execute store result entity @s Rotation[0] float 0.01 run scoreboard players get @s bjaj.yaw\n"
			+ "execute rotated as @s run tp @s ~ ~ ~ ~ 0\n"
			+ "tag @s add bjaj.root\n"
			+ "tag @s add bjaj.r." + m.id + "\n"
			+ "tag @s add bjaj.r." + m.id + "." + f.index + "\n"
			+ variant
			+ "scoreboard players set #ok" + ID + " 1\n");
		StringBuilder sync = new StringBuilder();
		sync.append("scoreboard players set #found").append(ID).append(" 0\n");
		sync.append("execute as ").append(roots).append(",distance=..24").append(mine).append("function ").append(fp).append("follow\n");
		sync.append("execute if score #found").append(ID).append(" matches 0 as ").append(roots).append(",distance=0..").append(mine).append("function ").append(fp).append("follow\n");
		for (Form other : allForms) {
			if (other.index == f.index) continue;
			sync.append("execute if score #found").append(ID).append(" matches 0 as @e[type=minecraft:item_display,tag=bjaj.r.").append(m.id).append(".").append(other.index).append("] if score @s").append(ID).append(" = #id").append(ID).append(" run scoreboard players operation #tyaw").append(ID).append(" = @s bjaj.yaw\n");
			sync.append("execute if score #found").append(ID).append(" matches 0 as @e[type=minecraft:item_display,tag=bjaj.r.").append(m.id).append(".").append(other.index).append("] if score @s").append(ID).append(" = #id").append(ID).append(" at @s run function ").append(other.rig).append("/remove/this\n");
		}
		sync.append("execute if score #found").append(ID).append(" matches 0 run function ").append(fp).append("remodel\n");
		out.put(fd + "sync.mcfunction", sync.toString());

		StringBuilder follow = new StringBuilder();
		follow.append("scoreboard players set #found").append(ID).append(" 1\n");
		follow.append("scoreboard players set @s bjaj.live 2\n");
		follow.append("scoreboard players set #c360").append(ID).append(" 36000\n");
		if (m.mode == AjData.HOSTILE) {
			follow.append("execute if score #aim").append(ID).append(" matches 1 facing entity @p[distance=..28,gamemode=!creative,gamemode=!spectator] feet rotated ~ 0 run tp @s ~ ~ ~ ~ 0\n");
			follow.append("execute if score #aim").append(ID).append(" matches 1 store result score #tyaw").append(ID).append(" run data get entity @s Rotation[0] 100\n");
		}
		follow.append("scoreboard players operation #dyaw").append(ID).append(" = #tyaw").append(ID).append("\n");
		follow.append("scoreboard players operation #dyaw").append(ID).append(" -= @s bjaj.yaw\n");
		follow.append("scoreboard players operation #dyaw").append(ID).append(" %= #c360").append(ID).append("\n");
		follow.append("execute if score #dyaw").append(ID).append(" matches 18000.. run scoreboard players remove #dyaw").append(ID).append(" 36000\n");
		follow.append("scoreboard players operation #step").append(ID).append(" = #dyaw").append(ID).append("\n");
		follow.append("scoreboard players set #div").append(ID).append(" 6\n");
		if (m.mode == AjData.HOSTILE) {
			follow.append("execute if score #aim").append(ID).append(" matches 1 run scoreboard players set #div").append(ID).append(" 3\n");
		}
		follow.append("execute unless score #dyaw").append(ID).append(" matches -8..8 run scoreboard players operation #step").append(ID).append(" /= #div").append(ID).append("\n");
		follow.append("execute if score #step").append(ID).append(" matches 1201.. run scoreboard players set #step").append(ID).append(" 1200\n");
		follow.append("execute if score #step").append(ID).append(" matches ..-1201 run scoreboard players set #step").append(ID).append(" -1200\n");
		follow.append("scoreboard players operation @s bjaj.yaw += #step").append(ID).append("\n");
		follow.append("scoreboard players operation @s bjaj.yaw %= #c360").append(ID).append("\n");
		follow.append("execute if score @s bjaj.yaw matches 18000.. run scoreboard players remove @s bjaj.yaw 36000\n");
		follow.append("execute store result entity @s Rotation[0] float 0.01 run scoreboard players get @s bjaj.yaw\n");
		follow.append("execute rotated as @s run tp @s ~ ~ ~ ~ 0\n");
		out.put(fd + "follow.mcfunction", follow.toString());

		String idle = f.anims[AjData.IDLE];
		String walk = f.anims[AjData.WALK];
		if (!acts.isEmpty()) {
			StringBuilder end = new StringBuilder();
			for (ActEntry a : acts) {
				if (f.animCtrl(a.anim)) {
					end.append("execute if score @s bjaj.at matches ").append(a.code).append(" run ").append(rig).append("animations/").append(a.anim).append("/pause\n");
				} else if (f.pause) {
					end.append("execute if score @s bjaj.at matches ").append(a.code).append(" run ").append(rig).append("animations/pause_all\n");
					end.append("execute if score @s bjaj.at matches ").append(a.code).append(" run scoreboard players set @s bjaj.st -1\n");
				}
			}
			end.append("scoreboard players set @s bjaj.at 0\n");
			if (m.animLayer) {
				if (idle.isEmpty()) {
					if (f.hasDefPose()) end.append("execute if score @s bjaj.st matches 0 run ").append(rig).append("set_default_pose\n");
				} else if (!f.tags[AjData.IDLE].isEmpty()) {
					end.append("execute if score @s bjaj.st matches 0 unless entity @s[tag=").append(f.tags[AjData.IDLE]).append("] run ").append(rig).append("animations/").append(idle).append("/play\n");
				}
			}
			out.put(fd + "act_end.mcfunction", end.toString());
			for (ActEntry a : acts) {
				StringBuilder af = new StringBuilder();
				if (m.animLayer) {
					if (f.animExcl(a.anim)) {
						for (ActEntry prev : acts) {
							if (!prev.anim.equals(a.anim) && f.animCtrl(prev.anim)) {
								af.append("execute if score @s bjaj.at matches ").append(prev.code).append(" run ").append(rig).append("animations/").append(prev.anim).append("/pause\n");
							}
						}
					}
					af.append("scoreboard players set @s bjaj.at ").append(a.code).append("\n");
					af.append("scoreboard players set @s bjaj.t ").append(a.ticks).append("\n");
					af.append(rig).append("animations/").append(a.anim).append("/play\n");
					if (!f.animExcl(a.anim)) {
						if (!idle.isEmpty() && !idle.equals(a.anim) && f.animCtrl(idle)) {
							af.append("execute if score @s bjaj.st matches 0 run ").append(rig).append("animations/").append(idle).append("/resume\n");
						}
						if (!walk.isEmpty() && !walk.equals(a.anim) && f.animCtrl(walk)) {
							af.append("execute if score @s bjaj.st matches 1 run ").append(rig).append("animations/").append(walk).append("/resume\n");
						}
					}
				} else {
					if (f.pause) af.append(rig).append("animations/pause_all\n");
					if (!idle.isEmpty() && !idle.equals(a.anim)) {
						if (f.animStop(idle)) {
							af.append(rig).append("animations/").append(idle).append("/stop\n");
						} else {
							af.append(rig).append("animations/").append(idle).append("/play\n");
							if (f.pause) af.append(rig).append("animations/pause_all\n");
						}
					} else if (f.hasDefPose()) {
						af.append(rig).append("set_default_pose\n");
					}
					af.append("scoreboard players set @s bjaj.st -1\n");
					af.append("scoreboard players set @s bjaj.at ").append(a.code).append("\n");
					af.append("scoreboard players set @s bjaj.t ").append(a.ticks).append("\n");
					if (f.animExcl(a.anim)) af.append(rig).append("animations/").append(a.anim).append("/play_exclusive\n");
					else af.append(rig).append("animations/").append(a.anim).append("/play\n");
				}
				out.put(fd + a.fn + ".mcfunction", af.toString());
			}
		}

		StringBuilder stIdle = new StringBuilder();
		stIdle.append("scoreboard players set @s bjaj.st 0\n");
		if (m.animLayer) {
			if (!walk.isEmpty() && !walk.equals(idle)) {
				if (f.animCtrl(walk)) stIdle.append(rig).append("animations/").append(walk).append("/pause\n");
				else if (f.pause) stIdle.append(rig).append("animations/pause_all\n");
			}
			if (!idle.isEmpty()) {
				if (f.hasDefPose()) {
					stIdle.append("execute if score @s bjaj.at matches 0 if score #act").append(ID).append(" matches 0 run ").append(rig).append("set_default_pose\n");
				}
				stIdle.append(rig).append("animations/").append(idle).append("/play\n");
				if (!f.animExcl(idle)) appendResumeActions(stIdle, rig, f, acts, idle);
			} else if (f.hasDefPose()) {
				stIdle.append(rig).append("set_default_pose\n");
			}
		} else {
			if (f.pause) stIdle.append(rig).append("animations/pause_all\n");
			if (f.hasDefPose()) stIdle.append(rig).append("set_default_pose\n");
			if (!idle.isEmpty()) stIdle.append(rig).append("animations/").append(idle).append("/play\n");
		}
		out.put(fd + "st_idle.mcfunction", stIdle.toString());

		if (!walk.isEmpty()) {
			StringBuilder stWalk = new StringBuilder();
			stWalk.append("scoreboard players set @s bjaj.st 1\n");
			if (m.animLayer) {
				if (!idle.isEmpty() && !idle.equals(walk)) {
					if (f.animCtrl(idle)) stWalk.append(rig).append("animations/").append(idle).append("/pause\n");
					else if (f.pause) stWalk.append(rig).append("animations/pause_all\n");
				}
				stWalk.append(rig).append("animations/").append(walk).append("/play\n");
				if (!f.animExcl(walk)) appendResumeActions(stWalk, rig, f, acts, walk);
			} else {
				if (f.pause) stWalk.append(rig).append("animations/pause_all\n");
				stWalk.append(rig).append("animations/").append(walk).append("/play\n");
			}
			out.put(fd + "st_walk.mcfunction", stWalk.toString());
		}

		StringBuilder anim = new StringBuilder();
		anim.append("execute if score @s bjaj.st matches 4 run return 0\n");
		anim.append("scoreboard players set #tr").append(ID).append(" 0\n");
		anim.append("execute if score @s bjaj.st matches 5 if score @s bjaj.t matches 1.. run scoreboard players set #tr").append(ID).append(" 1\n");
		anim.append("execute if score #tr").append(ID).append(" matches 1 run scoreboard players remove @s bjaj.t 1\n");
		anim.append("execute if score #tr").append(ID).append(" matches 1 run return 0\n");
		anim.append("execute if score @s bjaj.st matches 5 run scoreboard players set @s bjaj.st -1\n");
		if (!f.anims[AjData.ATTACK].isEmpty() && !formSyncMelee) {
			anim.append("execute if score #act").append(ID).append(" matches 0 if score #ev").append(ID).append(" matches 1 run scoreboard players set #act").append(ID).append(" 2\n");
		}
		if (!f.anims[AjData.HURT].isEmpty()) {
			String hurtGuard = formSyncMelee ? "unless score @s bjaj.at matches 2 unless score @s bjaj.at matches 6.. " : "unless score @s bjaj.at matches 6.. ";
			anim.append("execute if score #act").append(ID).append(" matches 0 ").append(hurtGuard).append("if score #ev").append(ID).append(" matches 2 run scoreboard players set #act").append(ID).append(" 3\n");
		}
		if (!walk.isEmpty()) {
			anim.append("execute if score #hold").append(ID).append(" matches 1 run scoreboard players set @s bjaj.mv 0\n");
			anim.append("execute if score #hold").append(ID).append(" matches 1 run scoreboard players set @s bjaj.still 7\n");
			anim.append("execute if score #hold").append(ID).append(" matches 0 if score #moved").append(ID).append(" matches 1 run scoreboard players set @s bjaj.still 0\n");
			anim.append("execute if score #hold").append(ID).append(" matches 0 if score #moved").append(ID).append(" matches 1 if score @s bjaj.mv matches ..9 run scoreboard players add @s bjaj.mv 1\n");
			anim.append("execute if score #hold").append(ID).append(" matches 0 if score #moved").append(ID).append(" matches 0 run scoreboard players set @s bjaj.mv 0\n");
			anim.append("execute if score #hold").append(ID).append(" matches 0 if score #moved").append(ID).append(" matches 0 if score @s bjaj.still matches ..99 run scoreboard players add @s bjaj.still 1\n");
		}
		if (m.animLayer) {
			if (!acts.isEmpty()) {
				anim.append("execute if score @s bjaj.t matches 1.. run scoreboard players remove @s bjaj.t 1\n");
				anim.append("execute if score @s bjaj.t matches 0 if score @s bjaj.at matches 1.. run function ").append(fp).append("act_end\n");
			}
			appendBaseLocomotion(out, fd, fp, rig, m.loop, true, f, acts, anim);
			for (ActEntry a : acts) {
				anim.append("execute if score #act").append(ID).append(" matches ").append(a.code).append(" run function ").append(fp).append(a.fn).append("\n");
			}
		} else {
			for (ActEntry a : acts) {
				anim.append("execute if score #act").append(ID).append(" matches ").append(a.code).append(" run function ").append(fp).append(a.fn).append("\n");
				anim.append("execute if score #act").append(ID).append(" matches ").append(a.code).append(" run return 0\n");
			}
			if (!acts.isEmpty()) {
				anim.append("execute if score @s bjaj.t matches 1.. run scoreboard players remove @s bjaj.t 1\n");
				anim.append("execute if score @s bjaj.t matches 1.. run return 0\n");
				anim.append("execute if score @s bjaj.at matches 1.. run function ").append(fp).append("act_end\n");
			}
			appendBaseLocomotion(out, fd, fp, rig, m.loop, false, f, acts, anim);
		}
		out.put(fd + "anim.mcfunction", anim.toString());
		out.put(fd + "root.mcfunction",
			"execute if score @s bjaj.st matches 4 run function " + fp + "dying\n"
			+ "execute if score @s bjaj.st matches 4 run return 0\n"
			+ "scoreboard players remove @s bjaj.live 1\n"
			+ "execute if score @s bjaj.live matches ..0 run function " + fp + "die\n");
		out.put(fd + "die.mcfunction", f.anims[AjData.DEATH].isEmpty() ? rig + "remove/this\n" : "scoreboard players set @s bjaj.at 0\n" + timed(f, AjData.DEATH, 4));
		out.put(fd + "dying.mcfunction", "scoreboard players remove @s bjaj.t 1\nexecute if score @s bjaj.t matches ..0 run " + rig + "remove/this\n");
	}

	private static void appendBaseLocomotion(Map<String, String> out, String fd, String fp, String rig, boolean loop, boolean layered, Form f, List<ActEntry> acts, StringBuilder anim) {
		if (f.anims[AjData.WALK].isEmpty()) {
			anim.append("execute unless score @s bjaj.st matches 0 run function ").append(fp).append("st_idle\n");
		} else {
			anim.append("execute if score @s bjaj.still matches ..6 if score @s bjaj.mv matches 2.. unless score @s bjaj.st matches 1 run function ").append(fp).append("st_walk\n");
			if (!layered) {
				anim.append("execute if score @s bjaj.st matches -1 if score @s bjaj.still matches ..6 if score #moved").append(ID).append(" matches 1 run function ").append(fp).append("st_walk\n");
			}
			anim.append("execute if score @s bjaj.still matches 7.. unless score @s bjaj.st matches 0 run function ").append(fp).append("st_idle\n");
			anim.append("execute if score @s bjaj.st matches -1 run function ").append(fp).append("st_idle\n");
		}
		appendBaseReplay(out, fd, fp, rig, loop, layered, f, acts, AjData.IDLE, 0, "re_idle", anim);
		appendBaseReplay(out, fd, fp, rig, loop, layered, f, acts, AjData.WALK, 1, "re_walk", anim);
	}

	private static void appendBaseReplay(Map<String, String> out, String fd, String fp, String rig, boolean loop, boolean layered, Form f, List<ActEntry> acts, int slot, int state, String fnName, StringBuilder anim) {
		String name = f.anims[slot];
		String tag = f.tags[slot];
		if (!loop || name.isEmpty() || tag.isEmpty()) return;
		if (layered && !f.animExcl(name) && !acts.isEmpty()) {
			StringBuilder re = new StringBuilder();
			re.append(rig).append("animations/").append(name).append("/play\n");
			appendResumeActions(re, rig, f, acts, name);
			out.put(fd + fnName + ".mcfunction", re.toString());
			anim.append("execute if score @s bjaj.st matches ").append(state).append(" unless entity @s[tag=").append(tag).append("] run function ").append(fp).append(fnName).append("\n");
		} else {
			anim.append("execute if score @s bjaj.st matches ").append(state).append(" unless entity @s[tag=").append(tag).append("] run ").append(rig).append("animations/").append(name).append("/play\n");
		}
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
			enter.append("scoreboard players set @s bjaj.mt 0\n");
			if (hasLightning) enter.append("function ").append(p).append("lt_clear\n");
			int dur = Math.max(1, ph.transTicks);
			if (!ph.changeRig) {
				if (!ph.transAnim.isEmpty() && oldForm.hasAnim(ph.transAnim)) {
					enter.append("scoreboard players set @s bjaj.pt ").append(dur).append("\n");
					enter.append("effect give @s minecraft:resistance 1 4 true\n");
					out.put(dir + "z/tr_" + phIdx + ".mcfunction", playCustom(oldForm, ph.transAnim, dur, 5));
					enter.append("execute as @e[type=minecraft:item_display,tag=bjaj.r.").append(m.id).append(".").append(oldForm.index).append(mine).append("function ").append(p).append("tr_").append(phIdx).append("\n");
				} else if (!formActions(m, oldForm).isEmpty()) {
					enter.append("execute as @e[type=minecraft:item_display,tag=bjaj.r.").append(m.id).append(".").append(oldForm.index).append(mine).append("scoreboard players set @s bjaj.t 0\n");
					enter.append("execute as @e[type=minecraft:item_display,tag=bjaj.r.").append(m.id).append(".").append(oldForm.index).append(mine).append("function ").append(p).append("f").append(oldForm.index).append("/act_end\n");
				}
			} else if (!ph.transOnNew && !ph.transAnim.isEmpty() && oldForm.hasAnim(ph.transAnim)) {
				enter.append("scoreboard players set @s bjaj.pt ").append(dur).append("\n");
				enter.append("effect give @s minecraft:resistance 1 4 true\n");
				out.put(dir + "z/tr_" + phIdx + ".mcfunction", playCustom(oldForm, ph.transAnim, dur, 5));
				enter.append("execute as @e[type=minecraft:item_display,tag=bjaj.r.").append(m.id).append(".").append(oldForm.index).append(mine).append("function ").append(p).append("tr_").append(phIdx).append("\n");
				out.put(dir + "z/swap_" + phIdx + ".mcfunction",
					"execute as @e[type=minecraft:item_display,tag=bjaj.r." + m.id + "." + oldForm.index + "] if score @s" + ID + " = #id" + ID + " run scoreboard players operation #tyaw" + ID + " = @s bjaj.yaw\n"
					+ "execute as @e[type=minecraft:item_display,tag=bjaj.r." + m.id + "." + oldForm.index + "] if score @s" + ID + " = #id" + ID + " at @s run function " + oldForm.rig + "/remove/this\n"
					+ "function " + p + "f" + newForm.index + "/remodel\n");
			} else {
				enter.append("execute as @e[type=minecraft:item_display,tag=bjaj.r.").append(m.id).append(".").append(oldForm.index).append("] if score @s").append(ID).append(" = #id").append(ID).append(" run scoreboard players operation #tyaw").append(ID).append(" = @s bjaj.yaw\n");
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

	private static List<Integer> formHitTicks(AjData m, Form f) {
		int animLen = Math.max(1, f.ticks[AjData.ATTACK]);
		List<Integer> raw = parseHitTicks(m.meleeHits, AjData.MAX_TICKS);
		if (raw == null || raw.isEmpty()) return List.of(AjData.autoHitTick(animLen));
		TreeSet<Integer> clamped = new TreeSet<>();
		for (int t : raw) clamped.add(Math.max(1, Math.min(animLen, t)));
		return new ArrayList<>(clamped);
	}

	private static void writeMelee(Map<String, String> out, String dir, String p, AjData m, List<Form> forms, String mine, double mRange, double extraRad, double trigRange) {
		int dmg = Math.max(1, intRange(m.meleeDamage, 1, 500));
		int cd = Math.max(0, intRange(m.meleeCd, 0, AjData.MAX_TICKS));
		String trigStr = fmtDist(trigRange);
		String closeStr = fmtDist(Math.max(1.4, mRange * 0.55) + extraRad);
		String fwdStr = fmtDist(mRange * 0.45 + extraRad * 0.5);
		String cleaveStr = fmtDist(mRange * 0.75 + 0.4 + extraRad * 0.6);

		StringBuilder meleeFn = new StringBuilder();
		meleeFn.append("execute if score #msync").append(ID).append(" matches 1 if entity @s[tag=!bjaj.nom] run function ").append(p).append("nom_on\n");
		meleeFn.append("execute if score #msync").append(ID).append(" matches 0 if entity @s[tag=bjaj.nom] run function ").append(p).append("nom_off\n");
		meleeFn.append("execute if score @s bjaj.pt matches 1.. run scoreboard players set @s bjaj.mt 0\n");
		meleeFn.append("execute if score @s bjaj.pt matches 1.. run return 0\n");
		meleeFn.append("execute if score @s bjaj.sk matches 0.. run scoreboard players set @s bjaj.mt 0\n");
		meleeFn.append("execute if score @s bjaj.sk matches 0.. run return 0\n");
		meleeFn.append("execute if score #msync").append(ID).append(" matches 0 run scoreboard players set @s bjaj.mt 0\n");
		meleeFn.append("execute if score #msync").append(ID).append(" matches 0 run return 0\n");
		for (Form f : forms) {
			if (f.anims[AjData.ATTACK].isEmpty()) continue;
			String fd = dir + "z/f" + f.index + "/";
			String fp = p + "f" + f.index + "/";
			String roots = "@e[type=minecraft:item_display,tag=bjaj.r." + m.id + "." + f.index;
			int animLen = Math.max(1, f.ticks[AjData.ATTACK]);
			meleeFn.append("execute if score #fm").append(ID).append(" matches ").append(f.index).append(" run function ").append(fp).append("melee\n");

			out.put(fd + "m_start.mcfunction",
				"scoreboard players set @s bjaj.mt 1\n"
				+ "scoreboard players set @s bjaj.mcd " + cd + "\n"
				+ "scoreboard players set #act" + ID + " 2\n");
			out.put(fd + "m_hit.mcfunction",
				"tag @a[tag=bjaj.mhit] remove bjaj.mhit\n"
				+ "tag @a[distance=.." + closeStr + ",gamemode=!creative,gamemode=!spectator] add bjaj.mhit\n"
				+ "execute as " + roots + "] if score @s" + ID + " = #id" + ID + " rotated as @s positioned ^ ^ ^" + fwdStr + " run tag @a[distance=.." + cleaveStr + ",gamemode=!creative,gamemode=!spectator] add bjaj.mhit\n"
				+ "execute unless entity @a[tag=bjaj.mhit] run return 0\n"
				+ "tag @s add bjaj.cast\n"
				+ "execute as @a[tag=bjaj.mhit] run damage @s " + dmg + " minecraft:mob_attack by @e[tag=bjaj.cast,limit=1]\n"
				+ "tag @s remove bjaj.cast\n"
				+ "tag @a[tag=bjaj.mhit] remove bjaj.mhit\n");

			StringBuilder fm = new StringBuilder();
			fm.append("execute if score @s bjaj.mt matches 0 if score @s bjaj.mcd matches 1.. run scoreboard players remove @s bjaj.mcd 1\n");
			String targetSel = "@p[distance=.." + trigStr + ",gamemode=!creative,gamemode=!spectator]";
			if (trigRange > 2.2) {
				fm.append("execute if score @s bjaj.mt matches 0 if score @s bjaj.mcd matches ..0 if entity ").append(targetSel)
					.append(" anchored eyes facing entity ").append(targetSel).append(" eyes if block ^ ^ ^1 #bjaj:passable run function ").append(fp).append("m_start\n");
			} else {
				fm.append("execute if score @s bjaj.mt matches 0 if score @s bjaj.mcd matches ..0 if entity ").append(targetSel)
					.append(" run function ").append(fp).append("m_start\n");
			}
			for (int ht : formHitTicks(m, f)) {
				fm.append("execute if score @s bjaj.mt matches ").append(ht).append(" run function ").append(fp).append("m_hit\n");
			}
			fm.append("execute if score @s bjaj.mt matches 1.. run scoreboard players add @s bjaj.mt 1\n");
			fm.append("execute if score @s bjaj.mt matches ").append(animLen + 1).append(".. run scoreboard players set @s bjaj.mt 0\n");
			out.put(fd + "melee.mcfunction", fm.toString());
		}
		out.put(dir + "z/melee.mcfunction", meleeFn.toString());
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
		skillsFn.append("scoreboard players operation #sk").append(ID).append(" = @s bjaj.sk\n");
		skillsFn.append("execute if score #sk").append(ID).append(" matches 0.. run function ").append(p).append("sk_wind\n");
		skillsFn.append("execute if score #sk").append(ID).append(" matches 0.. run return 0\n");
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
			if (!s.anim.isEmpty() || windup > 0) {
				cast.append("scoreboard players set @s bjaj.mt 0\n");
			}
			if (!s.anim.isEmpty()) {
				cast.append("scoreboard players set #act").append(ID).append(" ").append(6 + i).append("\n");
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
			fire.append("playsound minecraft:entity.ender_dragon.flap hostile @a ~ ~ ~ 2 1\n");
			fire.append("particle minecraft:explosion ~ ~0.6 ~ 0 0 0 0 1 force\n");
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
		return "summon " + entityId + " ~ ~ ~ {Tags:[\"bjaj.pj\"],power:[0.0d,0.0d,0.0d]" + extraNbt + "}\n"
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
			+ "execute store result entity @e[tag=bjaj.pj,limit=1] power[0] double 0.0002 run scoreboard players get #x1" + ID + "\n"
			+ "execute store result entity @e[tag=bjaj.pj,limit=1] power[1] double 0.0002 run scoreboard players get #y1" + ID + "\n"
			+ "execute store result entity @e[tag=bjaj.pj,limit=1] power[2] double 0.0002 run scoreboard players get #z1" + ID + "\n"
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
		String playCmd = f.animExcl(anim) ? rig + anim + "/play_exclusive\n" : (f.pause ? rig + "pause_all\n" : "") + rig + anim + "/play\n";
		return "scoreboard players set @s bjaj.st " + state + "\nscoreboard players set @s bjaj.at 0\nscoreboard players set @s bjaj.t " + Math.max(1, ticks) + "\n" + playCmd;
	}

	private static String play(Form f, int slot) {
		String name = f.anims[slot];
		String rig = "function " + f.rig + "/animations/";
		if (name.isEmpty()) return f.pause ? rig + "pause_all\n" : "";
		return f.animExcl(name) ? rig + name + "/play_exclusive\n" : (f.pause ? rig + "pause_all\n" : "") + rig + name + "/play\n";
	}
}
