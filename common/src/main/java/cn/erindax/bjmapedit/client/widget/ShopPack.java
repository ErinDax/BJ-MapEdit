package cn.erindax.bjmapedit.client.widget;

import net.minecraft.ChatFormatting;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public final class ShopPack {
	public static final String PACK = "bjshop";
	public static final String ANY_TAG = "bjshop.any";
	private static final String DATA_DIR = PACK + "/data/";
	private static final String SHOP_PREFIX = DATA_DIR + "bjshop_";
	private static final String PROJECT = "/shop.json";
	private static final String TAKEN = "execute if score #st bjshop matches 1 ";
	private static final String FAILED = "execute if score #st bjshop matches 2 run ";
	private static final String FAIL = " run scoreboard players set #st bjshop 2\n";
	private static final String PAGE = "bjshop.page";
	private static final String ID = "bjshop.id";
	private static final String VIEW = "bjshop.view";
	private static final String PITCH = "bjshop.rx";
	private static final String YAW = "bjshop.ry";
	private static final String OPENED = "bjshop.o";
	private static final String MARKER = "@e[type=minecraft:marker,tag=";
	private static final String HERE = "execute align xyz positioned ~0.5 ~0.5 ~0.5 as " + MARKER;
	private static final String ROTATION = "data modify storage bjshop:tmp rot set from entity @s Rotation\n";
	private static final String[] OPEN_STATS = {"open_chest", "trigger_trapped_chest", "open_barrel", "open_shulker_box"};

	private ShopPack() {}

	public static String shopDir(String id) {
		return SHOP_PREFIX + id;
	}

	public static String projectFile(String id) {
		return SHOP_PREFIX + id + PROJECT;
	}

	public static String function(String id, String name) {
		return "bjshop_" + id + ":" + name;
	}

	public static String shopTag(String id) {
		return "bjshop_" + id;
	}

	public static List<String> shopIds(String tree) {
		Set<String> ids = new TreeSet<>();
		if (tree == null) return new ArrayList<>();
		String prefix = "F\t" + SHOP_PREFIX;
		for (String line : tree.split("\n")) {
			if (!line.startsWith(prefix) || !line.endsWith(PROJECT)) continue;
			String id = line.substring(prefix.length(), line.length() - PROJECT.length());
			if (ShopData.validId(id)) ids.add(id);
		}
		return new ArrayList<>(ids);
	}

	public static Map<String, String> globalFiles(Collection<String> shops) {
		Map<String, String> out = new LinkedHashMap<>();
		String functions = DATA_DIR + "bjshop/" + ShopCompat.FUNCTIONS + "/";
		String tags = DATA_DIR + "minecraft/" + ShopCompat.FUNCTION_TAGS + "/";
		out.put(PACK + "/pack.mcmeta", ShopCompat.META);
		out.put(tags + "load.json", "{\n  \"values\": [\n    \"bjshop:load\"\n  ]\n}\n");
		out.put(tags + "tick.json", "{\n  \"values\": [\n    \"bjshop:tick\"\n  ]\n}\n");
		out.put(DATA_DIR + "bjshop/" + ShopCompat.BLOCK_TAGS + "/boxes.json", ShopCompat.BOXES);
		StringBuilder load = new StringBuilder("scoreboard objectives add bjshop dummy\n");
		for (String name : new String[]{PAGE, ID, VIEW, PITCH, YAW}) load.append("scoreboard objectives add ").append(name).append(" dummy\n");
		StringBuilder opened = new StringBuilder();
		StringBuilder tick = new StringBuilder("execute as @a store result score @s bjshop run clear @s ").append(ShopCompat.anyIcon()).append(" 0\n")
			.append("execute as @a[scores={bjshop=1..}] at @s run function bjshop:detect\n")
			.append(ShopCompat.DROPPED).append('\n')
			.append("execute as @a[scores={").append(VIEW).append("=1..}] run function bjshop:viewing\n");
		for (int i = 0; i < OPEN_STATS.length; i++) {
			load.append("scoreboard objectives add ").append(OPENED).append(i).append(" minecraft.custom:minecraft.").append(OPEN_STATS[i]).append('\n');
			tick.append("execute as @a[scores={").append(OPENED).append(i).append("=1..}] at @s run function bjshop:opened\n");
			opened.append("scoreboard players set @s ").append(OPENED).append(i).append(" 0\n");
		}
		tick.append("scoreboard players add #timer bjshop 1\n")
			.append("execute if score #timer bjshop matches 20.. run function bjshop:second\n")
			.append("execute if score #refill bjshop matches 1 run function bjshop:refill\n");
		StringBuilder detect = new StringBuilder();
		StringBuilder fillOne = new StringBuilder();
		StringBuilder refill = new StringBuilder("scoreboard players set #refill bjshop 0\n")
			.append("execute as ").append(MARKER).append(ANY_TAG).append("] at @s unless block ~ ~ ~ #bjshop:boxes run kill @s\n");
		for (String id : shops) {
			load.append("function ").append(function(id, "init")).append('\n');
			detect.append("execute store result score #n bjshop run clear @s ").append(ShopCompat.shopIcon(id)).append(" 0\n");
			detect.append("execute if score #n bjshop matches 1.. run function ").append(function(id, "detect")).append('\n');
			refill.append("execute as ").append(MARKER).append(shopTag(id)).append("] at @s run function ").append(function(id, "fill")).append('\n');
			fillOne.append("execute if entity @s[tag=").append(shopTag(id)).append("] run function ").append(function(id, "fill")).append('\n');
		}
		detect.append("clear @s ").append(ShopCompat.anyIcon()).append('\n');
		detect.append("scoreboard players set #refill bjshop 1\n");
		opened.append("scoreboard players set @s ").append(VIEW).append(" 0\n")
			.append("tag @s add bjshop.me\n")
			.append("scoreboard players set #hit bjshop 0\n")
			.append("scoreboard players set #ray bjshop 70\n")
			.append("execute anchored eyes positioned ^ ^ ^ run function bjshop:ray\n")
			.append("tag @s remove bjshop.me\n")
			.append(ROTATION)
			.append("execute store result score @s ").append(PITCH).append(" run data get storage bjshop:tmp rot[1] 100\n")
			.append("execute store result score @s ").append(YAW).append(" run data get storage bjshop:tmp rot[0] 100\n");
		out.put(functions + "load.mcfunction", load.toString());
		out.put(functions + "tick.mcfunction", tick.toString());
		out.put(functions + "second.mcfunction", "scoreboard players set #timer bjshop 0\nscoreboard players set #refill bjshop 1\n");
		out.put(functions + "detect.mcfunction", detect.toString());
		out.put(functions + "dropped.mcfunction", "scoreboard players set #refill bjshop 1\nkill @s\n");
		out.put(functions + "refill.mcfunction", refill.toString());
		out.put(functions + "fill_one.mcfunction", fillOne.toString());
		out.put(functions + "opened.mcfunction", opened.toString());
		out.put(functions + "ray.mcfunction",
			"scoreboard players remove #ray bjshop 1\n"
				+ "execute if block ~ ~ ~ #bjshop:boxes run function bjshop:ray_hit\n"
				+ "execute if score #hit bjshop matches 0 if score #ray bjshop matches 1.. positioned ^ ^ ^0.1 run function bjshop:ray\n");
		out.put(functions + "ray_hit.mcfunction",
			"scoreboard players set #hit bjshop 1\n"
				+ HERE + ANY_TAG + ",distance=..0.4,limit=1] at @s run function bjshop:reopen\n");
		out.put(functions + "viewing.mcfunction",
			ROTATION
				+ "execute store result score #rx bjshop run data get storage bjshop:tmp rot[1] 100\n"
				+ "execute store result score #ry bjshop run data get storage bjshop:tmp rot[0] 100\n"
				+ "execute unless score #rx bjshop = @s " + PITCH + " run scoreboard players set @s " + VIEW + " 0\n"
				+ "execute unless score #ry bjshop = @s " + YAW + " run scoreboard players set @s " + VIEW + " 0\n");
		out.put(functions + "reopen.mcfunction",
			"execute unless score @s " + ID + " matches 1.. run function bjshop:assign\n"
				+ "scoreboard players operation #id bjshop = @s " + ID + "\n"
				+ "scoreboard players set #others bjshop 0\n"
				+ "execute as @a[tag=!bjshop.me] if score @s " + VIEW + " = #id bjshop run scoreboard players set #others bjshop 1\n"
				+ "scoreboard players operation @a[tag=bjshop.me] " + VIEW + " = #id bjshop\n"
				+ "execute if score #others bjshop matches 0 unless score @s " + PAGE + " matches 0 run function bjshop:first\n");
		out.put(functions + "first.mcfunction", "scoreboard players set @s " + PAGE + " 0\nfunction bjshop:fill_one\n");
		out.put(functions + "assign.mcfunction", "scoreboard players add #next bjshop 1\nscoreboard players operation @s " + ID + " = #next bjshop\n");
		out.put(functions + "unbind.mcfunction",
			"execute align xyz run kill " + MARKER + ANY_TAG + ",dx=0,dy=0,dz=0]\n"
				+ "data remove block ~ ~ ~ Items[{" + ShopCompat.ICON_NBT + "}]\n"
				+ ShopCompat.CLEAR_NAME + "\n");
		ShopCompat.globalFiles(shops, out);
		return out;
	}

	public static Map<String, String> shopFiles(ShopData shop) {
		Map<String, String> out = new LinkedHashMap<>();
		String id = shop.id;
		String dir = shopDir(id) + "/" + ShopCompat.FUNCTIONS + "/";
		int pages = shop.pages.size();
		StringBuilder detect = new StringBuilder();
		StringBuilder fillAll = new StringBuilder("data remove block ~ ~ ~ Items[{").append(ShopCompat.ICON_NBT).append("}]\n")
			.append("execute unless score @s ").append(PAGE).append(" matches 0..").append(pages - 1).append(" run scoreboard players set @s ").append(PAGE).append(" 0\n");
		Set<String> objectives = new LinkedHashSet<>();
		List<ItemStack> icons = new ArrayList<>();
		for (int p = 0; p < pages; p++) {
			ShopData.Slot[] slots = shop.pages.get(p);
			String name = "p" + p + "_";
			StringBuilder pageDetect = new StringBuilder();
			StringBuilder nav = new StringBuilder();
			StringBuilder[] trade = {new StringBuilder(), new StringBuilder(), new StringBuilder()};
			StringBuilder[] fill = {new StringBuilder(), new StringBuilder(), new StringBuilder()};
			for (int i = 0; i < ShopData.SLOTS; i++) {
				ShopData.Slot slot = slots[i];
				if (!slot.shown()) continue;
				ItemStack icon = icon(shop, p, i);
				icons.add(icon);
				StringBuilder f = fill[i / 9];
				f.append("execute ").append(ShopCompat.occupied(i)).append(" run summon minecraft:item ~ ~1 ~ ").append(ShopCompat.RETURNED_ITEM).append('\n');
				f.append("execute as @e[type=minecraft:item,tag=bjshop.ret] run data modify entity @s Item set from block ~ ~ ~ Items[{Slot:").append(i).append("b}]\n");
				f.append("tag @e[type=minecraft:item,tag=bjshop.ret] remove bjshop.ret\n");
				f.append("item replace block ~ ~ ~ container.").append(i).append(" with ").append(ShopCompat.itemArg(icon)).append(' ').append(icon.getCount()).append('\n');
				if (!slot.trading() && !slot.nav()) continue;
				String mark = ShopCompat.slotIcon(id, p, i, icon);
				pageDetect.append("execute store result score #s").append(i).append(" bjshop run clear @s ").append(mark).append('\n');
				if (slot.trading()) trade(trade[i / 9], shop, slot, i, objectives);
				else nav(nav, shop, p, i, mark);
			}
			for (int r = 0; r < 3; r++) {
				if (fill[r].length() > 0) {
					fillAll.append("execute if score @s ").append(PAGE).append(" matches ").append(p).append(" run function ").append(function(id, name + "fill_" + r)).append('\n');
					out.put(dir + name + "fill_" + r + ".mcfunction", fill[r].toString());
				}
				if (trade[r].length() > 0) {
					pageDetect.append("function ").append(function(id, name + "trade_" + r)).append('\n');
					out.put(dir + name + "trade_" + r + ".mcfunction", trade[r].toString());
				}
			}
			if (pageDetect.length() == 0) continue;
			pageDetect.append(nav);
			detect.append("execute store result score #n bjshop run clear @s ").append(ShopCompat.pageIcon(id, p)).append(" 0\n");
			detect.append("execute if score #n bjshop matches 1.. run function ").append(function(id, name + "detect")).append('\n');
			out.put(dir + name + "detect.mcfunction", pageDetect.toString());
		}
		detect.append("clear @s ").append(ShopCompat.shopIcon(id)).append('\n');
		StringBuilder init = new StringBuilder();
		for (String objective : objectives) init.append("scoreboard objectives add ").append(objective).append(" dummy\n");
		String marker = HERE + shopTag(id) + ",distance=..0.4] at @s run function ";
		StringBuilder bind = new StringBuilder("execute align xyz run kill ").append(MARKER).append(ANY_TAG).append(",dx=0,dy=0,dz=0]\n");
		if (shop.title.isBlank()) {
			bind.append(ShopCompat.CLEAR_NAME).append('\n');
		} else {
			bind.append("data modify block ~ ~ ~ CustomName set value ")
				.append(ShopCompat.nameNbt(SectionText.toComponent(shop.title, Style.EMPTY))).append('\n');
		}
		bind.append("execute align xyz run summon minecraft:marker ~0.5 ~0.5 ~0.5 {Tags:[\"").append(ANY_TAG).append("\",\"").append(shopTag(id)).append("\"]}\n");
		bind.append(marker).append("bjshop:assign\n");
		bind.append(marker).append(function(id, "fill")).append('\n');
		out.put(dir + "init.mcfunction", init.toString());
		out.put(dir + "detect.mcfunction", detect.toString());
		out.put(dir + "fill.mcfunction", fillAll.toString());
		out.put(dir + "bind.mcfunction", bind.toString());
		out.put(dir + "place.mcfunction", "setblock ~ ~ ~ minecraft:" + shop.container + "\nfunction " + function(id, "bind") + "\n");
		ShopCompat.shopFiles(id, icons, out);
		return out;
	}

	public static ItemStack icon(ShopData shop, int index) {
		return icon(shop, shop.page, index);
	}

	public static ItemStack icon(ShopData shop, int page, int index) {
		ShopData.Slot slot = shop.pages.get(page)[index];
		List<Component> lore = slot.trading() ? lore(slot) : List.of();
		return ShopCompat.icon(slot.item, ShopData.clampCount(slot.item, slot.count), lore, shop.id, page, index);
	}

	public static List<Component> lore(ShopData.Slot slot) {
		boolean sell = slot.type == ShopData.SELL;
		MutableComponent prices = Component.literal(I18n.get(sell ? "screen.bj_mapedit.shop_lore_price" : "screen.bj_mapedit.shop_lore_pays"))
			.withStyle(plain(sell ? ChatFormatting.RED : ChatFormatting.GOLD));
		for (int i = 0; i < slot.prices.size(); i++) {
			if (i > 0) prices.append(Component.literal(" + ").withStyle(plain(ChatFormatting.WHITE)));
			prices.append(priceText(slot.prices.get(i)).withStyle(plain(i % 2 == 0 ? ChatFormatting.GREEN : ChatFormatting.AQUA)));
		}
		Component action = Component.literal(I18n.get(sell ? "screen.bj_mapedit.shop_lore_buy" : "screen.bj_mapedit.shop_lore_sell"))
			.withStyle(plain(ChatFormatting.GREEN).withUnderlined(true));
		return List.of(prices, action);
	}

	public static MutableComponent priceText(ShopData.Price price) {
		if (price.type == ShopData.PRICE_XP) return Component.literal(I18n.get("screen.bj_mapedit.shop_levels", price.amount));
		if (price.type == ShopData.PRICE_SCORE) return Component.literal(price.objective + " * " + price.amount);
		return Component.empty().append(price.item.getHoverName()).append(" * " + price.amount);
	}

	private static Style plain(ChatFormatting color) {
		return Style.EMPTY.withItalic(false).withColor(color);
	}

	private static ShopData.Price goods(ShopData.Slot slot) {
		ShopData.Price goods = new ShopData.Price();
		goods.type = ShopData.PRICE_ITEM;
		goods.item = slot.item;
		goods.amount = ShopData.clampCount(slot.item, slot.count);
		return goods;
	}

	public static List<String> itemCommands(ShopData shop, ShopData.Slot slot) {
		List<String> out = new ArrayList<>();
		if (!slot.trading()) return out;
		boolean sell = slot.type == ShopData.SELL;
		ShopData.Price goods = goods(slot);
		for (ShopData.Price p : sell ? slot.prices : List.of(goods)) {
			if (p.type != ShopData.PRICE_ITEM) continue;
			String arg = ShopCompat.payArg(p.item, shop.strict);
			out.add(arg == null ? null : "clear @s " + arg + " 0");
		}
		for (ShopData.Price p : sell ? List.of(goods) : slot.prices) {
			if (p.type == ShopData.PRICE_ITEM) out.add("give @s " + ShopCompat.itemArg(p.item) + " 1");
		}
		return out;
	}

	private static String payArg(ShopData shop, ItemStack item) {
		String arg = ShopCompat.payArg(item, shop.strict);
		return arg != null ? arg : ShopCompat.payArg(item, false);
	}

	private static void nav(StringBuilder sb, ShopData shop, int page, int index, String mark) {
		int target = shop.target(page, shop.pages.get(page)[index]);
		String taken = "execute if score #s" + index + " bjshop matches 1.. ";
		if (target >= 0 && target < shop.pages.size() && target != page) {
			sb.append(taken).append("as ").append(MARKER).append(shopTag(shop.id)).append(",scores={").append(PAGE).append('=').append(page).append("},distance=..10] at @s ")
				.append(ShopCompat.missing(index, mark)).append(" run scoreboard players set @s ").append(PAGE).append(' ').append(target).append('\n');
		}
		if (shop.sound) sb.append(taken).append("run playsound minecraft:item.book.page_turn player @s ~ ~ ~ 1 1\n");
	}

	private static void trade(StringBuilder sb, ShopData shop, ShopData.Slot slot, int index, Set<String> objectives) {
		boolean sell = slot.type == ShopData.SELL;
		ShopData.Price goods = goods(slot);
		List<ShopData.Price> pay = sell ? slot.prices : List.of(goods);
		List<ShopData.Price> gain = sell ? List.of(goods) : slot.prices;
		Map<String, Integer> items = new LinkedHashMap<>();
		Map<String, Integer> scores = new LinkedHashMap<>();
		int levels = 0;
		for (ShopData.Price p : pay) {
			if (p.type == ShopData.PRICE_ITEM) items.merge(payArg(shop, p.item), p.amount, Integer::sum);
			else if (p.type == ShopData.PRICE_SCORE) scores.merge(p.objective, p.amount, Integer::sum);
			else levels += p.amount;
		}
		for (ShopData.Price p : slot.prices) {
			if (p.type == ShopData.PRICE_SCORE) objectives.add(p.objective);
		}
		sb.append("scoreboard players set #st bjshop 0\n");
		sb.append("execute if score #s").append(index).append(" bjshop matches 1.. run scoreboard players set #st bjshop 1\n");
		for (Map.Entry<String, Integer> e : items.entrySet()) {
			sb.append(TAKEN).append("store result score #have bjshop run clear @s ").append(e.getKey()).append(" 0\n");
			sb.append(TAKEN).append("unless score #have bjshop matches ").append(e.getValue()).append("..").append(FAIL);
		}
		if (levels > 0) {
			sb.append(TAKEN).append("store result score #have bjshop run experience query @s levels\n");
			sb.append(TAKEN).append("unless score #have bjshop matches ").append(levels).append("..").append(FAIL);
		}
		for (Map.Entry<String, Integer> e : scores.entrySet()) {
			sb.append(TAKEN).append("unless score @s ").append(e.getKey()).append(" matches ").append(e.getValue()).append("..").append(FAIL);
		}
		sb.append(FAILED).append("title @s actionbar ").append(message(shop.failText, "screen.bj_mapedit.shop_fail_default", ChatFormatting.RED)).append('\n');
		if (shop.sound) sb.append(FAILED).append("playsound minecraft:entity.villager.no player @s ~ ~ ~ 1 1\n");
		for (Map.Entry<String, Integer> e : items.entrySet()) {
			sb.append(TAKEN).append("run clear @s ").append(e.getKey()).append(' ').append(e.getValue()).append('\n');
		}
		if (levels > 0) sb.append(TAKEN).append("run experience add @s -").append(levels).append(" levels\n");
		for (Map.Entry<String, Integer> e : scores.entrySet()) {
			sb.append(TAKEN).append("run scoreboard players remove @s ").append(e.getKey()).append(' ').append(e.getValue()).append('\n');
		}
		for (ShopData.Price p : gain) {
			if (p.type == ShopData.PRICE_ITEM) {
				int amount = Math.min(p.amount, p.item.getMaxStackSize() * 100);
				sb.append(TAKEN).append("run give @s ").append(ShopCompat.itemArg(p.item)).append(' ').append(amount).append('\n');
			} else if (p.type == ShopData.PRICE_SCORE) {
				sb.append(TAKEN).append("run scoreboard players add @s ").append(p.objective).append(' ').append(p.amount).append('\n');
			} else {
				sb.append(TAKEN).append("run experience add @s ").append(p.amount).append(" levels\n");
			}
		}
		sb.append(TAKEN).append("run title @s actionbar ").append(message(shop.okText, "screen.bj_mapedit.shop_ok_default", ChatFormatting.GREEN)).append('\n');
		if (shop.sound) sb.append(TAKEN).append("run playsound minecraft:entity.experience_orb.pickup player @s ~ ~ ~ 1 1\n");
		String command = command(slot.command);
		if (!command.isEmpty()) sb.append(TAKEN).append("run ").append(command).append('\n');
	}

	public static String command(String raw) {
		String command = raw == null ? "" : raw.replace('\n', ' ').replace('\r', ' ').trim();
		while (command.startsWith("/")) command = command.substring(1).trim();
		return command;
	}

	private static String message(String custom, String fallbackKey, ChatFormatting color) {
		String raw = custom == null || custom.isBlank() ? I18n.get(fallbackKey) : custom;
		return ShopCompat.textArg(SectionText.toComponent(raw, Style.EMPTY.withColor(color)));
	}
}
