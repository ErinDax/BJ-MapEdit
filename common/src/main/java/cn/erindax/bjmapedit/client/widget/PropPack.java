package cn.erindax.bjmapedit.client.widget;

import com.google.gson.JsonPrimitive;
import com.mojang.brigadier.ParseResults;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public final class PropPack {
	public static final String PACK = "bjprop";
	public static final String PROJECT_DIR = PACK + "/data/" + PACK + "/bjmapedit";
	private static final String K = "screen.bj_mapedit.";
	private static final String DATA = PACK + "/data/";
	private static final String FN = DATA + PACK + "/function/";
	private static final String META = "{\n  \"pack\": {\n    \"pack_format\": 48,\n    \"description\": \"BJ-MapEdit Skill Props\"\n  }\n}\n";
	private static final String PASSABLE = "{\n  \"values\": [\n    \"#minecraft:replaceable\",\n    \"#minecraft:flowers\",\n    \"#minecraft:crops\",\n    \"#minecraft:buttons\",\n    \"#minecraft:pressure_plates\",\n    \"#minecraft:rails\",\n    \"#minecraft:all_signs\",\n    \"#minecraft:banners\",\n    \"minecraft:torch\",\n    \"minecraft:wall_torch\",\n    \"minecraft:soul_torch\",\n    \"minecraft:soul_wall_torch\",\n    \"minecraft:redstone_torch\",\n    \"minecraft:redstone_wall_torch\"\n  ]\n}\n";
	private static final String IGNORE = "{\n  \"values\": [\n    \"minecraft:item\",\n    \"minecraft:experience_orb\",\n    \"minecraft:area_effect_cloud\",\n    \"minecraft:marker\",\n    \"minecraft:item_display\",\n    \"minecraft:block_display\",\n    \"minecraft:text_display\",\n    \"minecraft:interaction\",\n    \"minecraft:item_frame\",\n    \"minecraft:glow_item_frame\",\n    \"minecraft:painting\",\n    \"minecraft:leash_knot\",\n    \"minecraft:armor_stand\",\n    \"minecraft:falling_block\",\n    \"minecraft:tnt\",\n    \"minecraft:lightning_bolt\",\n    \"minecraft:evoker_fangs\",\n    \"minecraft:eye_of_ender\",\n    \"minecraft:firework_rocket\",\n    \"minecraft:fishing_bobber\",\n    \"#minecraft:impact_projectiles\",\n    \"#minecraft:arrows\"\n  ]\n}\n";
	private static final String SNEAK_PRED = "{\n  \"condition\": \"minecraft:entity_properties\",\n  \"entity\": \"this\",\n  \"predicate\": {\n    \"flags\": {\n      \"is_sneaking\": true\n    }\n  }\n}\n";
	private static final String ADV_USE = "{\"criteria\":{\"a\":{\"trigger\":\"minecraft:using_item\",\"conditions\":{\"item\":{\"predicates\":{\"minecraft:custom_data\":\"{bjprop:{}}\"}}}}},\"rewards\":{\"function\":\"bjprop:z/on_use\"}}\n";
	private static final String ADV_HIT = "{\"criteria\":{\"a\":{\"trigger\":\"minecraft:player_hurt_entity\",\"conditions\":{\"player\":[{\"condition\":\"minecraft:entity_properties\",\"entity\":\"this\",\"predicate\":{\"equipment\":{\"mainhand\":{\"predicates\":{\"minecraft:custom_data\":\"{bjprop:{}}\"}}}}}]}}},\"rewards\":{\"function\":\"bjprop:z/on_hit\"}}\n";
	private static final String ID = " bjprop.id";
	private static final double[][] RING_12 = {
		{0.75, 0.00}, {0.65, 0.38}, {0.38, 0.65}, {0.00, 0.75},
		{-0.38, 0.65}, {-0.65, 0.38}, {-0.75, 0.00}, {-0.65, -0.38},
		{-0.38, -0.65}, {0.00, -0.75}, {0.38, -0.65}, {0.65, -0.38}
	};

	private PropPack() {}

	public static String projectFile(String id) {
		return PROJECT_DIR + "/" + id + ".json";
	}

	public static String propDir(String id) {
		return FN + id;
	}

	public static String giveFunctionCommand(String id) {
		return "function bjprop:" + id + "/give";
	}

	public static List<String> propIds(String tree) {
		Set<String> ids = new TreeSet<>();
		if (tree == null) return new ArrayList<>();
		String prefix = "F\t" + PROJECT_DIR + "/";
		for (String line : tree.split("\n")) {
			if (!line.startsWith(prefix) || !line.endsWith(".json")) continue;
			String id = line.substring(prefix.length(), line.length() - 5);
			if (PropData.validId(id)) ids.add(id);
		}
		return new ArrayList<>(ids);
	}

	public static String validItem(String raw) {
		Identifier id = Identifier.tryParse(raw == null ? "" : raw.trim());
		if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) return null;
		Item item = BuiltInRegistries.ITEM.getValue(id);
		return item == Items.AIR ? null : id.toString();
	}

	public static String validParticle(String raw) {
		Identifier id = Identifier.tryParse(raw == null ? "" : raw.trim());
		if (id == null || !BuiltInRegistries.PARTICLE_TYPE.containsKey(id)) return null;
		ParticleType<?> type = BuiltInRegistries.PARTICLE_TYPE.getValue(id);
		return type instanceof SimpleParticleType ? id.toString() : null;
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
			ParseResults<?> res = conn.getCommands().parse(s, conn.getSuggestionsProvider());
			if (!res.getExceptions().isEmpty() || res.getReader().canRead()) return null;
		}
		return s;
	}

	public static String problem(PropData p) {
		if (!PropData.validId(p.id)) return "prop_bad_id";
		if (p.baseSnbt.isEmpty() && validItem(p.item) == null) return "prop_bad_item";
		if (!p.baseSnbt.isEmpty() && ShopCompat.stackFromSnbt(p.baseSnbt).isEmpty()) return "prop_bad_item";
		if (p.skills.isEmpty()) return "prop_no_skills";
		for (PropData.Skill s : p.skills) {
			if (PropData.usesCharge(s.interact) && intRange(s.charge, 1, 1200) < 0) return "prop_bad_charge";
			int minCd = s.interact == PropData.INTERACT_RIGHT_HOLD ? 1 : 0;
			if (intRange(s.cooldown, minCd, PropData.MAX_TICKS) < 0) return "prop_bad_cd";
			if (intRange(s.range, 1, 64) < 0) return "prop_bad_range";
			if (s.effect == PropData.EFFECT_PROJECTILE) {
				if (intRange(s.power, 1, 5) < 0) return "aj_bad_skill_burst";
			} else if (s.effect == PropData.EFFECT_BLINK) {
				if (intRange(s.power, 0, 500) < 0) return "prop_bad_power";
			} else if (s.effect != PropData.EFFECT_CUSTOM) {
				if (intRange(s.power, 1, 500) < 0) return "prop_bad_power";
			}
			if (PropData.usesParticle(s.effect) && validParticle(s.particle) == null) return "prop_bad_particle";
			if (s.effect == PropData.EFFECT_CUSTOM && cleanCommand(s.command) == null) return "prop_bad_cmd";
		}
		return null;
	}

	public static String cleanImportSnbt(ItemStack raw) {
		if (raw == null || raw.isEmpty()) return "";
		ItemStack copy = raw.copyWithCount(1);
		CustomData cd = copy.get(DataComponents.CUSTOM_DATA);
		if (cd != null) {
			CompoundTag tag = cd.copyTag();
			tag.remove("bjprop");
			if (tag.isEmpty()) copy.remove(DataComponents.CUSTOM_DATA);
			else copy.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
		}
		Consumable cons = copy.get(DataComponents.CONSUMABLE);
		if (cons != null && cons.consumeSeconds() >= 99999.0F && cons.animation() == ItemUseAnimation.NONE && !cons.hasConsumeParticles()) {
			copy.remove(DataComponents.CONSUMABLE);
		}
		return ShopCompat.stackSnbt(copy);
	}

	public static ItemStack buildStack(PropData p) {
		ItemStack stack;
		if (!p.baseSnbt.isEmpty()) {
			stack = ShopCompat.stackFromSnbt(p.baseSnbt).copyWithCount(1);
		} else {
			String itemId = validItem(p.item);
			if (itemId == null) return ItemStack.EMPTY;
			stack = new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(itemId)));
		}
		if (stack.isEmpty()) return ItemStack.EMPTY;
		if (!p.name.isBlank()) {
			stack.set(DataComponents.CUSTOM_NAME, SectionText.toComponent(ampersandCodes(p.name), Style.EMPTY.withItalic(false)));
		}
		if (p.autoLore && !p.skills.isEmpty()) {
			List<Component> lines = new ArrayList<>(stack.getOrDefault(DataComponents.LORE, ItemLore.EMPTY).lines());
			for (PropData.Skill s : p.skills) {
				String in = I18n.get(K + "prop_in_" + s.interact);
				String ef = I18n.get(K + "prop_ef_" + s.effect);
				int cd = Math.max(0, intRange(s.cooldown, 0, PropData.MAX_TICKS));
				String tail = cd > 0 && s.interact != PropData.INTERACT_RIGHT_HOLD
					? " (" + String.format(Locale.ROOT, "%.1fs", cd / 20.0) + ")"
					: "";
				lines.add(Component.literal("[" + in + "] ").withStyle(Style.EMPTY.withItalic(false).withColor(ChatFormatting.GOLD))
					.append(Component.literal(ef).withStyle(ChatFormatting.AQUA))
					.append(Component.literal(tail).withStyle(ChatFormatting.DARK_GRAY)));
			}
			stack.set(DataComponents.LORE, new ItemLore(lines));
		}
		boolean drop = p.needsDrop();
		String id = p.id;
		CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
			CompoundTag mark = new CompoundTag();
			mark.putString("id", id);
			if (drop) mark.putByte("drop", (byte) 1);
			tag.put("bjprop", mark);
		});
		if (p.needsRightClick() && stack.get(DataComponents.CONSUMABLE) == null
			&& !stack.is(Items.CARROT_ON_A_STICK) && !stack.is(Items.WARPED_FUNGUS_ON_A_STICK)) {
			stack.set(DataComponents.CONSUMABLE, Consumable.builder()
				.consumeSeconds(100000.0F)
				.animation(ItemUseAnimation.NONE)
				.sound(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.EMPTY))
				.hasConsumeParticles(false)
				.build());
		}
		return stack;
	}

	public static Map<String, String> globalFiles(Collection<String> props) {
		Map<String, String> out = new LinkedHashMap<>();
		StringBuilder loads = new StringBuilder();
		StringBuilder ticks = new StringBuilder();
		StringBuilder drops = new StringBuilder();
		for (String id : new TreeSet<>(props)) {
			if (loads.length() > 0) {
				loads.append(",");
				ticks.append(",");
				drops.append(",");
			}
			loads.append("\"bjprop:").append(id).append("/z/load\"");
			ticks.append("\"bjprop:").append(id).append("/z/tick\"");
			drops.append("\"bjprop:").append(id).append("/z/drop\"");
		}
		out.put(PACK + "/pack.mcmeta", META);
		out.put(DATA + "minecraft/tags/function/load.json", "{\"values\":[\"bjprop:z/load\"]}\n");
		out.put(DATA + "minecraft/tags/function/tick.json", "{\"values\":[\"bjprop:z/tick\"]}\n");
		out.put(DATA + PACK + "/tags/function/load.json", "{\"values\":[" + loads + "]}\n");
		out.put(DATA + PACK + "/tags/function/tick.json", "{\"values\":[" + ticks + "]}\n");
		out.put(DATA + PACK + "/tags/function/drop.json", "{\"values\":[" + drops + "]}\n");
		out.put(DATA + PACK + "/tags/block/passable.json", PASSABLE);
		out.put(DATA + PACK + "/tags/entity_type/ignore.json", IGNORE);
		out.put(DATA + PACK + "/predicate/sneaking.json", SNEAK_PRED);
		out.put(DATA + PACK + "/advancement/z/use.json", ADV_USE);
		out.put(DATA + PACK + "/advancement/z/hit.json", ADV_HIT);

		out.put(FN + "z/load.mcfunction",
			"scoreboard objectives add bjprop.id dummy\n"
			+ "scoreboard objectives add bjprop.u dummy\n"
			+ "scoreboard objectives add bjprop.up dummy\n"
			+ "scoreboard objectives add bjprop.uh dummy\n"
			+ "scoreboard objectives add bjprop.lh dummy\n"
			+ "scoreboard objectives add bjprop.dt dummy\n"
			+ "scoreboard objectives add bjprop.cs0 minecraft.used:minecraft.carrot_on_a_stick\n"
			+ "scoreboard objectives add bjprop.cs1 minecraft.used:minecraft.warped_fungus_on_a_stick\n"
			+ "function #bjprop:load\n");
		out.put(FN + "z/on_use.mcfunction", "advancement revoke @s only bjprop:z/use\nscoreboard players set @s bjprop.u 1\n");
		out.put(FN + "z/on_hit.mcfunction", "advancement revoke @s only bjprop:z/hit\nexecute unless entity @s[tag=bjprop.self] run scoreboard players set @s bjprop.lh 1\n");
		out.put(FN + "z/pre_player.mcfunction",
			"execute unless score @s bjprop.id matches 1.. run scoreboard players add #next bjprop.id 1\n"
			+ "execute unless score @s bjprop.id matches 1.. run scoreboard players operation @s bjprop.id = #next bjprop.id\n"
			+ "execute if score @s bjprop.u matches 1 run scoreboard players add @s bjprop.uh 1\n");
		out.put(FN + "z/post_player.mcfunction",
			"execute if score @s bjprop.u matches 0 run scoreboard players set @s bjprop.uh 0\n"
			+ "scoreboard players operation @s bjprop.up = @s bjprop.u\n"
			+ "scoreboard players set @s bjprop.u 0\n"
			+ "scoreboard players set @s bjprop.lh 0\n");
		out.put(FN + "z/dropped.mcfunction",
			"tag @s add bjprop.d\n"
			+ "execute unless data entity @s Thrower run return 0\n"
			+ "tag @s add bjprop.curd\n"
			+ "function #bjprop:drop\n"
			+ "execute if entity @s run data merge entity @s {PickupDelay:0s}\n"
			+ "execute if entity @s on origin if entity @s[type=minecraft:player] at @s run tp @e[type=minecraft:item,tag=bjprop.curd,limit=1] ~ ~ ~\n"
			+ "tag @s remove bjprop.curd\n");
		out.put(FN + "z/tick.mcfunction",
			"execute as @a[scores={bjprop.cs0=1..}] if items entity @s weapon.* *[minecraft:custom_data~{bjprop:{}}] run scoreboard players set @s bjprop.u 1\n"
			+ "scoreboard players reset @a[scores={bjprop.cs0=1..}] bjprop.cs0\n"
			+ "execute as @a[scores={bjprop.cs1=1..}] if items entity @s weapon.* *[minecraft:custom_data~{bjprop:{}}] run scoreboard players set @s bjprop.u 1\n"
			+ "scoreboard players reset @a[scores={bjprop.cs1=1..}] bjprop.cs1\n"
			+ "execute as @a run function bjprop:z/pre_player\n"
			+ "execute as @e[type=minecraft:item,tag=!bjprop.d] if items entity @s contents *[minecraft:custom_data~{bjprop:{drop:1b}}] at @s run function bjprop:z/dropped\n"
			+ "function #bjprop:tick\n"
			+ "execute as @a run function bjprop:z/post_player\n");
		return out;
	}

	public static Map<String, String> propFiles(PropData m) {
		Map<String, String> out = new LinkedHashMap<>();
		String dir = FN + m.id + "/";
		String p = "bjprop:" + m.id + "/z/";
		ItemStack built = buildStack(m);
		String itemArg = GiveCommands.item(built, null, true);
		out.put(dir + "give.mcfunction", "give @s " + itemArg + " 1\n");

		String itemMatch = "*[minecraft:custom_data~{bjprop:{id:\"" + m.id + "\"}}]";
		StringBuilder load = new StringBuilder();
		for (int i = 0; i < m.skills.size(); i++) {
			load.append("scoreboard objectives add bjprop.c.").append(m.id).append(".").append(i).append(" dummy\n");
		}
		out.put(dir + "z/load.mcfunction", load.toString());

		StringBuilder dropFn = new StringBuilder();
		boolean hasSneakRight = m.hasSneakRight();
		StringBuilder playerFn = new StringBuilder();
		for (int i = 0; i < m.skills.size(); i++) {
			String cdObj = "bjprop.c." + m.id + "." + i;
			playerFn.append("execute if score @s ").append(cdObj).append(" matches 1.. run scoreboard players remove @s ").append(cdObj).append(" 1\n");
		}
		playerFn.append("execute if items entity @s weapon.mainhand ").append(itemMatch).append(" run function ").append(p).append("held\n");

		StringBuilder heldFn = new StringBuilder();
		StringBuilder tickFn = new StringBuilder();
		tickFn.append("execute as @a at @s run function ").append(p).append("player\n");

		for (int i = 0; i < m.skills.size(); i++) {
			PropData.Skill s = m.skills.get(i);
			String cdObj = "bjprop.c." + m.id + "." + i;
			int cd = Math.max(0, intRange(s.cooldown, 0, PropData.MAX_TICKS));
			int charge = Math.max(1, intRange(s.charge, 1, 1200));
			String tryFn = "s" + i + "_try";
			String castFn = "s" + i + "_cast";

			if (s.interact == PropData.INTERACT_RIGHT_CLICK) {
				String sneakGuard = hasSneakRight ? "unless predicate bjprop:sneaking " : "";
				heldFn.append("execute ").append(sneakGuard).append("if score @s bjprop.u matches 1 if score @s bjprop.up matches 0 run function ").append(p).append(tryFn).append("\n");
			} else if (s.interact == PropData.INTERACT_SNEAK_RIGHT) {
				heldFn.append("execute if predicate bjprop:sneaking if score @s bjprop.u matches 1 if score @s bjprop.up matches 0 run function ").append(p).append(tryFn).append("\n");
			} else if (s.interact == PropData.INTERACT_RIGHT_HOLD) {
				heldFn.append("execute if score @s bjprop.u matches 1 if score @s ").append(cdObj).append(" matches ..0 run function ").append(p).append(castFn).append("\n");
			} else if (s.interact == PropData.INTERACT_RIGHT_RELEASE || s.interact == PropData.INTERACT_RIGHT_CHARGE) {
				String chFn = "s" + i + "_charging";
				out.put(dir + "z/" + chFn + ".mcfunction", chargeHudFn(s, charge, s.interact == PropData.INTERACT_RIGHT_RELEASE));
				heldFn.append("execute if score @s bjprop.u matches 1 if score @s ").append(cdObj).append(" matches ..0 run function ").append(p).append(chFn).append("\n");
				heldFn.append("execute if score @s bjprop.u matches 1 if score @s bjprop.up matches 0 if score @s ").append(cdObj).append(" matches 1.. run function ").append(p).append("s").append(i).append("_cdmsg\n");
				if (s.interact == PropData.INTERACT_RIGHT_RELEASE) {
					heldFn.append("execute if score @s bjprop.u matches 0 if score @s bjprop.up matches 1 if score @s bjprop.uh matches ").append(charge).append(".. if score @s ").append(cdObj).append(" matches ..0 run function ").append(p).append(castFn).append("\n");
				} else {
					heldFn.append("execute if score @s bjprop.u matches 1 if score @s bjprop.uh matches ").append(charge).append(" if score @s ").append(cdObj).append(" matches ..0 run function ").append(p).append(castFn).append("\n");
				}
			} else if (s.interact == PropData.INTERACT_LEFT_HIT) {
				heldFn.append("execute if score @s bjprop.lh matches 1 run function ").append(p).append(tryFn).append("\n");
			} else if (s.interact == PropData.INTERACT_DROP) {
				String dCast = "s" + i + "_drop";
				StringBuilder df = new StringBuilder();
				df.append("execute on origin if entity @s[type=minecraft:player] at @s run function ").append(p).append(tryFn).append("\n");
				if (s.consume) {
					df.append("execute if score #fired bjprop.id matches 1 run kill @s\n");
				}
				out.put(dir + "z/" + dCast + ".mcfunction", df.toString());
				dropFn.append("scoreboard players set #fired bjprop.id 0\n");
				dropFn.append("execute if items entity @s contents ").append(itemMatch).append(" run function ").append(p).append(dCast).append("\n");
			}

			String cdMsgFn = "s" + i + "_cdmsg";
			String cdTpl = I18n.get(K + "prop_cd_msg");
			out.put(dir + "z/" + cdMsgFn + ".mcfunction",
				"scoreboard players operation #sec bjprop.id = @s " + cdObj + "\n"
				+ "scoreboard players add #sec bjprop.id 19\n"
				+ "scoreboard players set #twenty bjprop.id 20\n"
				+ "scoreboard players operation #sec bjprop.id /= #twenty bjprop.id\n"
				+ "title @s actionbar {\"translate\":" + new JsonPrimitive(cdTpl) + ",\"color\":\"red\",\"with\":[{\"score\":{\"name\":\"#sec\",\"objective\":\"bjprop.id\"}}]}\n"
				+ "playsound minecraft:block.note_block.bass player @s ~ ~ ~ 0.6 0.8\n");

			out.put(dir + "z/" + tryFn + ".mcfunction",
				"execute if score @s " + cdObj + " matches 1.. run return run function " + p + cdMsgFn + "\n"
				+ "function " + p + castFn + "\n");

			writeEffectFiles(out, dir, p, m, i, s, cdObj, cd, itemMatch, tickFn);
		}

		out.put(dir + "z/player.mcfunction", playerFn.toString());
		out.put(dir + "z/held.mcfunction", heldFn.toString());
		out.put(dir + "z/drop.mcfunction", dropFn.toString());
		out.put(dir + "z/tick.mcfunction", tickFn.toString());
		return out;
	}

	private static String chargeHudFn(PropData.Skill s, int charge, boolean releaseMode) {
		StringBuilder sb = new StringBuilder();
		String pt = validParticle(s.particle);
		if (pt != null) {
			sb.append("particle ").append(pt).append(" ~ ~1.0 ~ 0.35 0.45 0.35 0.02 2 force\n");
		}
		sb.append("scoreboard players operation #ch bjprop.id = @s bjprop.uh\n");
		sb.append("scoreboard players set #ten bjprop.id 10\n");
		sb.append("scoreboard players set #max bjprop.id ").append(charge).append("\n");
		sb.append("scoreboard players operation #ch bjprop.id *= #ten bjprop.id\n");
		sb.append("scoreboard players operation #ch bjprop.id /= #max bjprop.id\n");
		for (int b = 0; b < 10; b++) {
			String bars = "\u25a0".repeat(b) + "\u25a1".repeat(10 - b);
			String msg = I18n.get(K + "prop_charging_bar", bars);
			sb.append("execute if score #ch bjprop.id matches ").append(b)
				.append(" run title @s actionbar [{\"text\":").append(new JsonPrimitive(msg)).append(",\"color\":\"yellow\"}]\n");
		}
		if (releaseMode) {
			String ready = I18n.get(K + "prop_charge_ready");
			sb.append("execute if score @s bjprop.uh matches ").append(charge).append(" run playsound minecraft:block.note_block.chime player @s ~ ~ ~ 0.9 1.8\n");
			sb.append("execute if score #ch bjprop.id matches 10.. run title @s actionbar {\"text\":").append(new JsonPrimitive(ready)).append(",\"color\":\"gold\",\"bold\":true}\n");
		}
		return sb.toString();
	}

	private static void writeEffectFiles(Map<String, String> out, String dir, String p, PropData m, int idx, PropData.Skill s, String cdObj, int cd, String itemMatch, StringBuilder tickFn) {
		int range = Math.max(1, intRange(s.range, 1, 64));
		int power = Math.max(0, intRange(s.power, 0, 500));
		String pt = PropData.usesParticle(s.effect) ? validParticle(s.particle) : "minecraft:end_rod";
		if (pt == null) pt = "minecraft:end_rod";
		String targetFilter = "tag=!bjprop.self,type=!#bjprop:ignore" + (m.pvp ? "" : ",type=!minecraft:player");

		StringBuilder cast = new StringBuilder();
		cast.append("scoreboard players set #fired bjprop.id 1\n");
		if (cd > 0) cast.append("scoreboard players set @s ").append(cdObj).append(" ").append(cd).append("\n");
		if (s.consume && s.interact != PropData.INTERACT_DROP) {
			cast.append("clear @s ").append(itemMatch).append(" 1\n");
		}
		cast.append("tag @s add bjprop.self\n");

		switch (s.effect) {
			case PropData.EFFECT_RING_LASER -> {
				String rayFn = "s" + idx + "_ray";
				String ringFn = "s" + idx + "_ring";
				String hitFn = "s" + idx + "_lhit";
				StringBuilder ring = new StringBuilder();
				for (double[] xy : RING_12) {
					ring.append(String.format(Locale.ROOT, "particle %s ^%.2f ^%.2f ^ 0 0 0 0 1 force\n", pt, xy[0], xy[1]));
				}
				out.put(dir + "z/" + ringFn + ".mcfunction", ring.toString());
				out.put(dir + "z/" + hitFn + ".mcfunction",
					"tag @s add bjprop.hit\n"
					+ "damage @s " + Math.max(1, power) + " minecraft:player_attack by @p[tag=bjprop.self,limit=1]\n");
				String blockCheck = s.pierce ? "" : "if block ~ ~ ~ #bjprop:passable ";
				out.put(dir + "z/" + rayFn + ".mcfunction",
					"particle " + pt + " ~ ~ ~ 0.03 0.03 0.03 0 2 force\n"
					+ "execute if score #rg bjprop.id matches 0 run function " + p + ringFn + "\n"
					+ "scoreboard players add #rg bjprop.id 1\n"
					+ "execute if score #rg bjprop.id matches 6.. run scoreboard players set #rg bjprop.id 0\n"
					+ "execute as @e[" + targetFilter + ",tag=!bjprop.hit,distance=..1.5] run function " + p + hitFn + "\n"
					+ "scoreboard players remove #st bjprop.id 1\n"
					+ "execute if score #st bjprop.id matches 1.. positioned ^ ^ ^0.5 " + blockCheck + "run function " + p + rayFn + "\n");
				cast.append("playsound minecraft:entity.warden.sonic_boom player @a ~ ~ ~ 1.5 1.4\n");
				cast.append("playsound minecraft:block.beacon.activate player @a ~ ~ ~ 1.5 1.8\n");
				cast.append("scoreboard players set #st bjprop.id ").append(range * 2).append("\n");
				cast.append("scoreboard players set #rg bjprop.id 0\n");
				cast.append("execute anchored eyes positioned ^ ^-0.15 ^0.8 ").append(blockCheck).append("run function ").append(p).append(rayFn).append("\n");
				cast.append("tag @e[tag=bjprop.hit] remove bjprop.hit\n");
			}
			case PropData.EFFECT_DASH_SLASH -> {
				String dTag = "bjprop.ds." + m.id + "." + idx;
				String dStepFn = "s" + idx + "_dstep";
				String dTickFn = "s" + idx + "_dtick";
				int subSteps = Math.max(1, Math.round(range / 3.0F));
				StringBuilder step = new StringBuilder();
				for (int k = 0; k < subSteps; k++) {
					step.append("execute anchored feet positioned ^ ^ ^0.6 if block ~ ~ ~ #bjprop:passable if block ~ ~1 ~ #bjprop:passable run tp @s ~ ~ ~\n");
				}
				out.put(dir + "z/" + dStepFn + ".mcfunction", step.toString());
				String dTickBody =
					"tag @s add bjprop.self\n"
					+ "effect give @s minecraft:resistance 1 4 true\n"
					+ "execute rotated ~ 0 run function " + p + dStepFn + "\n"
					+ "execute at @s run particle " + pt + " ~ ~0.9 ~ 0.45 0.55 0.45 0.04 18 force\n"
					+ "execute at @s anchored eyes positioned ^ ^-0.2 ^1.2 run particle minecraft:sweep_attack ~ ~ ~ 0 0 0 0 1 force\n"
					+ "execute at @s as @e[" + targetFilter + ",distance=..2.4] run damage @s " + Math.max(1, power) + " minecraft:player_attack by @p[tag=bjprop.self,limit=1]\n"
					+ "scoreboard players remove @s bjprop.dt 1\n"
					+ "execute if score @s bjprop.dt matches ..0 at @s run playsound minecraft:entity.generic.explode player @a ~ ~ ~ 1.0 1.4\n"
					+ "execute if score @s bjprop.dt matches ..0 at @s run particle " + pt + " ~ ~0.8 ~ 1.1 0.5 1.1 0.12 36 force\n"
					+ "execute if score @s bjprop.dt matches ..0 run tag @s remove " + dTag + "\n"
					+ "tag @s remove bjprop.self\n";
				out.put(dir + "z/" + dTickFn + ".mcfunction", dTickBody);
				tickFn.append("execute as @a[tag=").append(dTag).append(",scores={bjprop.dt=1..}] at @s run function ").append(p).append(dTickFn).append("\n");
				cast.append("playsound minecraft:entity.ender_dragon.flap player @a ~ ~ ~ 1.5 1.5\n");
				cast.append("playsound minecraft:entity.player.attack.sweep player @a ~ ~ ~ 1.5 1.2\n");
				cast.append("tag @s add ").append(dTag).append("\n");
				cast.append("scoreboard players set @s bjprop.dt 5\n");
			}
			case PropData.EFFECT_SWORD_WAVE -> {
				String wTag = "bjprop.wv." + m.id + "." + idx;
				String wTickFn = "s" + idx + "_wtick";
				String wArcFn = "s" + idx + "_warc";
				int lifeTicks = Math.max(2, (int) Math.ceil(range / 1.5));
				StringBuilder arc = new StringBuilder();
				double[] xs = {-1.5, -1.1, -0.7, -0.35, 0.0, 0.35, 0.7, 1.1, 1.5};
				for (double x : xs) {
					double z = -0.22 * x * x;
					arc.append(String.format(Locale.ROOT, "particle %s ^%.2f ^0.0 ^%.2f 0.04 0.04 0.04 0.01 3 force\n", pt, x, z));
				}
				arc.append("particle minecraft:sweep_attack ^ ^ ^ 0 0 0 0 1 force\n");
				out.put(dir + "z/" + wArcFn + ".mcfunction", arc.toString());
				out.put(dir + "z/" + wTickFn + ".mcfunction",
					"scoreboard players operation #oid bjprop.id = @s bjprop.id\n"
					+ "execute as @a if score @s bjprop.id = #oid bjprop.id run tag @s add bjprop.self\n"
					+ "execute unless block ~ ~ ~ #bjprop:passable run kill @s\n"
					+ "execute unless entity @s run tag @a[tag=bjprop.self] remove bjprop.self\n"
					+ "execute unless entity @s run return 0\n"
					+ "function " + p + wArcFn + "\n"
					+ "execute if entity @a[tag=bjprop.self] as @e[" + targetFilter + ",distance=..2.2] run damage @s " + Math.max(1, power) + " minecraft:player_attack by @p[tag=bjprop.self,limit=1]\n"
					+ "execute unless entity @a[tag=bjprop.self] as @e[" + targetFilter + ",distance=..2.2] run damage @s " + Math.max(1, power) + " minecraft:magic\n"
					+ "tag @a[tag=bjprop.self] remove bjprop.self\n"
					+ "tp @s ^ ^ ^1.5\n"
					+ "scoreboard players remove @s bjprop.dt 1\n"
					+ "execute if score @s bjprop.dt matches ..0 run kill @s\n");
				tickFn.append("execute as @e[type=minecraft:marker,tag=").append(wTag).append("] at @s run function ").append(p).append(wTickFn).append("\n");
				cast.append("playsound minecraft:entity.player.attack.sweep player @a ~ ~ ~ 1.5 0.9\n");
				cast.append("playsound minecraft:item.trident.throw player @a ~ ~ ~ 1.5 1.3\n");
				cast.append("scoreboard players operation #oid bjprop.id = @s bjprop.id\n");
				cast.append("execute anchored eyes positioned ^ ^-0.2 ^1.0 run summon minecraft:marker ~ ~ ~ {Tags:[\"bjprop.wv\",\"").append(wTag).append("\",\"bjprop.nw\"]}\n");
				cast.append("data modify entity @e[type=minecraft:marker,tag=bjprop.nw,limit=1] Rotation set from entity @s Rotation\n");
				cast.append("execute as @e[type=minecraft:marker,tag=bjprop.nw] run scoreboard players operation @s bjprop.id = #oid bjprop.id\n");
				cast.append("execute as @e[type=minecraft:marker,tag=bjprop.nw] run scoreboard players set @s bjprop.dt ").append(lifeTicks).append("\n");
				cast.append("tag @e[type=minecraft:marker,tag=bjprop.nw] remove bjprop.nw\n");
			}
			case PropData.EFFECT_PROJECTILE -> {
				int burst = Math.max(1, Math.min(5, power));
				String projId = switch (s.projKind) {
					case PropData.PROJ_FIREBALL -> "minecraft:fireball";
					case PropData.PROJ_DRAGON -> "minecraft:dragon_fireball";
					case PropData.PROJ_WITHER -> "minecraft:wither_skull";
					default -> "minecraft:wind_charge";
				};
				String extra = s.projKind == PropData.PROJ_FIREBALL ? ",ExplosionPower:0b" : "";
				String sound = switch (s.projKind) {
					case PropData.PROJ_FIREBALL -> "minecraft:entity.blaze.shoot";
					case PropData.PROJ_DRAGON -> "minecraft:entity.ender_dragon.shoot";
					case PropData.PROJ_WITHER -> "minecraft:entity.wither.shoot";
					default -> "minecraft:entity.breeze.shoot";
				};
				String projFn = "s" + idx + "_proj";
				out.put(dir + "z/" + projFn + ".mcfunction",
					"summon " + projId + " ~ ~ ~ {Tags:[\"bjprop.pj\"],acceleration_power:0.12d" + extra + "}\n"
					+ "execute store result score #x0" + ID + " run data get entity @e[tag=bjprop.pj,limit=1] Pos[0] 500\n"
					+ "execute store result score #y0" + ID + " run data get entity @e[tag=bjprop.pj,limit=1] Pos[1] 500\n"
					+ "execute store result score #z0" + ID + " run data get entity @e[tag=bjprop.pj,limit=1] Pos[2] 500\n"
					+ "execute positioned ^ ^ ^1 run tp @e[tag=bjprop.pj,limit=1] ~ ~ ~\n"
					+ "execute store result score #x1" + ID + " run data get entity @e[tag=bjprop.pj,limit=1] Pos[0] 500\n"
					+ "execute store result score #y1" + ID + " run data get entity @e[tag=bjprop.pj,limit=1] Pos[1] 500\n"
					+ "execute store result score #z1" + ID + " run data get entity @e[tag=bjprop.pj,limit=1] Pos[2] 500\n"
					+ "scoreboard players operation #x1" + ID + " -= #x0" + ID + "\n"
					+ "scoreboard players operation #y1" + ID + " -= #y0" + ID + "\n"
					+ "scoreboard players operation #z1" + ID + " -= #z0" + ID + "\n"
					+ "execute store result entity @e[tag=bjprop.pj,limit=1] Motion[0] double 0.0024 run scoreboard players get #x1" + ID + "\n"
					+ "execute store result entity @e[tag=bjprop.pj,limit=1] Motion[1] double 0.0024 run scoreboard players get #y1" + ID + "\n"
					+ "execute store result entity @e[tag=bjprop.pj,limit=1] Motion[2] double 0.0024 run scoreboard players get #z1" + ID + "\n"
					+ "data modify entity @e[tag=bjprop.pj,limit=1] Owner set from entity @s UUID\n"
					+ "particle " + pt + " ^ ^ ^1 0.15 0.15 0.15 0.03 8 force\n"
					+ "tag @e[tag=bjprop.pj] remove bjprop.pj\n");
				cast.append("playsound ").append(sound).append(" player @a ~ ~ ~ 1.5 1.1\n");
				for (int yaw : spreadOffsets(burst)) {
					cast.append("execute anchored eyes rotated ~").append(yaw).append(" ~ run function ").append(p).append(projFn).append("\n");
				}
			}
			case PropData.EFFECT_STARFALL -> {
				String rayFn = "s" + idx + "_sray";
				String boomFn = "s" + idx + "_sboom";
				out.put(dir + "z/" + boomFn + ".mcfunction",
					"playsound minecraft:entity.lightning_bolt.thunder weather @a ~ ~ ~ 2.0 1.2\n"
					+ "playsound minecraft:entity.generic.explode player @a ~ ~ ~ 1.5 1.1\n"
					+ "particle minecraft:flash ~ ~1 ~ 0 0 0 0 1 force\n"
					+ "particle " + pt + " ~ ~7 ~ 0.25 7 0.25 0.03 80 force\n"
					+ "particle " + pt + " ~ ~0.3 ~ 1.8 0.2 1.8 0.18 55 force\n"
					+ "execute as @e[" + targetFilter + ",distance=..3.6] run damage @s " + Math.max(1, power) + " minecraft:player_attack by @p[tag=bjprop.self,limit=1]\n");
				out.put(dir + "z/" + rayFn + ".mcfunction",
					"scoreboard players remove #st bjprop.id 1\n"
					+ "execute if entity @e[" + targetFilter + ",distance=..1.4] run scoreboard players set #st bjprop.id 0\n"
					+ "execute unless block ^ ^ ^0.6 #bjprop:passable run scoreboard players set #st bjprop.id 0\n"
					+ "execute if score #st bjprop.id matches 1.. positioned ^ ^ ^0.6 run function " + p + rayFn + "\n"
					+ "execute if score #st bjprop.id matches 0 run function " + p + boomFn + "\n");
				cast.append("scoreboard players set #st bjprop.id ").append((int) Math.ceil(range / 0.6)).append("\n");
				cast.append("execute anchored eyes positioned ^ ^ ^0.8 run function ").append(p).append(rayFn).append("\n");
			}
			case PropData.EFFECT_NOVA -> {
				String spread = String.format(Locale.ROOT, "%.2f", Math.max(0.5, range * 0.4));
				cast.append("playsound minecraft:entity.breeze.wind_burst player @a ~ ~ ~ 1.8 1.0\n");
				cast.append("particle minecraft:gust_emitter_large ~ ~0.8 ~ 0 0 0 0 1 force\n");
				cast.append("particle ").append(pt).append(" ~ ~0.4 ~ ").append(spread).append(" 0.25 ").append(spread).append(" 0.2 65 force\n");
				for (double[] xy : RING_12) {
					cast.append(String.format(Locale.ROOT, "particle %s ~%.2f ~0.3 ~%.2f 0.1 0.1 0.1 0.08 4 force\n", pt, xy[0] * range * 0.7, xy[1] * range * 0.7));
				}
				cast.append("execute as @e[").append(targetFilter).append(",distance=..").append(range).append("] run damage @s ").append(Math.max(1, power)).append(" minecraft:player_attack by @p[tag=bjprop.self,limit=1]\n");
			}
			case PropData.EFFECT_VORTEX -> {
				String vTag = "bjprop.vx." + m.id + "." + idx;
				String vPlaceFn = "s" + idx + "_vray";
				String vSpawnFn = "s" + idx + "_vspawn";
				String vTickFn = "s" + idx + "_vtick";
				out.put(dir + "z/" + vSpawnFn + ".mcfunction",
					"summon minecraft:marker ~ ~ ~ {Tags:[\"bjprop.vx\",\"" + vTag + "\",\"bjprop.nv\"]}\n"
					+ "execute as @e[type=minecraft:marker,tag=bjprop.nv] run scoreboard players operation @s bjprop.id = #oid bjprop.id\n"
					+ "execute as @e[type=minecraft:marker,tag=bjprop.nv] run scoreboard players set @s bjprop.dt 60\n"
					+ "tag @e[type=minecraft:marker,tag=bjprop.nv] remove bjprop.nv\n"
					+ "playsound minecraft:block.end_portal_frame.fill player @a ~ ~ ~ 1.6 0.7\n");
				out.put(dir + "z/" + vPlaceFn + ".mcfunction",
					"scoreboard players remove #st bjprop.id 1\n"
					+ "execute unless block ^ ^ ^0.6 #bjprop:passable run scoreboard players set #st bjprop.id 0\n"
					+ "execute if score #st bjprop.id matches 1.. positioned ^ ^ ^0.6 run function " + p + vPlaceFn + "\n"
					+ "execute if score #st bjprop.id matches 0 run function " + p + vSpawnFn + "\n");
				out.put(dir + "z/" + vTickFn + ".mcfunction",
					"tag @s add bjprop.vcur\n"
					+ "scoreboard players operation #oid bjprop.id = @s bjprop.id\n"
					+ "execute as @a if score @s bjprop.id = #oid bjprop.id run tag @s add bjprop.self\n"
					+ "particle " + pt + " ~ ~0.5 ~ 1.2 0.8 1.2 0.25 18 force\n"
					+ "particle minecraft:squid_ink ~ ~0.5 ~ 0.25 0.25 0.25 0.02 4 force\n"
					+ "execute as @e[" + targetFilter + ",type=!minecraft:player,distance=0.6..6.5] at @s facing entity @e[type=minecraft:marker,tag=bjprop.vcur,limit=1] feet run tp @s ^ ^ ^0.32 ~ ~\n"
					+ "scoreboard players operation #vmod bjprop.id = @s bjprop.dt\n"
					+ "scoreboard players set #fifteen bjprop.id 15\n"
					+ "scoreboard players operation #vmod bjprop.id %= #fifteen bjprop.id\n"
					+ "execute if score #vmod bjprop.id matches 0 if entity @a[tag=bjprop.self] as @e[" + targetFilter + ",distance=..3.5] run damage @s " + Math.max(1, power) + " minecraft:player_attack by @p[tag=bjprop.self,limit=1]\n"
					+ "tag @a[tag=bjprop.self] remove bjprop.self\n"
					+ "tag @s remove bjprop.vcur\n"
					+ "scoreboard players remove @s bjprop.dt 1\n"
					+ "execute if score @s bjprop.dt matches ..0 run playsound minecraft:entity.generic.explode player @a ~ ~ ~ 1.2 1.3\n"
					+ "execute if score @s bjprop.dt matches ..0 run particle " + pt + " ~ ~0.5 ~ 1.4 0.8 1.4 0.3 45 force\n"
					+ "execute if score @s bjprop.dt matches ..0 run kill @s\n");
				tickFn.append("execute as @e[type=minecraft:marker,tag=").append(vTag).append("] at @s run function ").append(p).append(vTickFn).append("\n");
				cast.append("scoreboard players operation #oid bjprop.id = @s bjprop.id\n");
				cast.append("scoreboard players set #st bjprop.id ").append((int) Math.ceil(range / 0.6)).append("\n");
				cast.append("execute anchored eyes positioned ^ ^ ^0.8 run function ").append(p).append(vPlaceFn).append("\n");
			}
			case PropData.EFFECT_HEAL_SHIELD -> {
				int amp = Math.max(0, (Math.max(1, power) - 1) / 4);
				cast.append("playsound minecraft:block.amethyst_block.chime player @a ~ ~ ~ 1.5 1.4\n");
				cast.append("particle ").append(pt).append(" ~ ~1.0 ~ 0.6 0.8 0.6 0.15 35 force\n");
				cast.append("particle minecraft:heart ~ ~1.8 ~ 0.4 0.3 0.4 0.02 6 force\n");
				String targets = range <= 1 ? "@s" : "@a[distance=.." + range + ",gamemode=!spectator]";
				cast.append("effect give ").append(targets).append(" minecraft:instant_health 1 ").append(Math.min(10, amp)).append(" true\n");
				cast.append("effect give ").append(targets).append(" minecraft:absorption 15 ").append(Math.min(10, amp)).append(" true\n");
				cast.append("effect give ").append(targets).append(" minecraft:regeneration 5 1 true\n");
			}
			case PropData.EFFECT_BLINK -> {
				String bRayFn = "s" + idx + "_bray";
				out.put(dir + "z/" + bRayFn + ".mcfunction",
					"scoreboard players remove #st bjprop.id 1\n"
					+ "execute if score #st bjprop.id matches 1.. positioned ^ ^ ^0.6 if block ~ ~ ~ #bjprop:passable if block ~ ~1 ~ #bjprop:passable run function " + p + bRayFn + "\n"
					+ "execute unless score #tp bjprop.id matches 1 run tp @s ~ ~ ~\n"
					+ "scoreboard players set #tp bjprop.id 1\n");
				cast.append("particle ").append(pt).append(" ~ ~1.0 ~ 0.4 0.6 0.4 0.12 28 force\n");
				cast.append("playsound minecraft:entity.enderman.teleport player @a ~ ~ ~ 1.2 1.2\n");
				cast.append("scoreboard players set #st bjprop.id ").append((int) Math.ceil(range / 0.6)).append("\n");
				cast.append("scoreboard players set #tp bjprop.id 0\n");
				cast.append("execute anchored eyes positioned ^ ^-0.4 ^0.6 if block ~ ~ ~ #bjprop:passable if block ~ ~1 ~ #bjprop:passable run function ").append(p).append(bRayFn).append("\n");
				cast.append("execute at @s run particle ").append(pt).append(" ~ ~1.0 ~ 0.4 0.6 0.4 0.12 28 force\n");
				cast.append("execute at @s run playsound minecraft:entity.enderman.teleport player @a ~ ~ ~ 1.2 1.4\n");
				if (power > 0) {
					cast.append("execute at @s as @e[").append(targetFilter).append(",distance=..3.0] run damage @s ").append(power).append(" minecraft:player_attack by @p[tag=bjprop.self,limit=1]\n");
				}
			}
			case PropData.EFFECT_CUSTOM -> {
				String cmd = cleanCommand(s.command);
				if (cmd != null) {
					out.put(dir + "z/s" + idx + "_cmd.mcfunction", cmd + "\n");
					cast.append("execute anchored eyes run function ").append(p).append("s").append(idx).append("_cmd\n");
				}
			}
			default -> {}
		}

		cast.append("tag @s remove bjprop.self\n");
		out.put(dir + "z/s" + idx + "_cast.mcfunction", cast.toString());
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

	public static String ampersandCodes(String s) {
		if (s == null) return "";
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
}
