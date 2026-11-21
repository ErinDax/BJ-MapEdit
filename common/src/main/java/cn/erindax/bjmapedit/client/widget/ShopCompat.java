package cn.erindax.bjmapedit.client.widget;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

public final class ShopCompat {
	public static final String META = "{\n  \"pack\": {\n    \"pack_format\": 15,\n    \"description\": \"BJ-MapEdit Shops\"\n  }\n}\n";
	public static final String FUNCTIONS = "functions";
	public static final String FUNCTION_TAGS = "tags/functions";
	public static final String BLOCK_TAGS = "tags/blocks";
	public static final String BOXES = "{\n  \"values\": [\n    \"minecraft:chest\",\n    \"minecraft:trapped_chest\",\n    \"minecraft:barrel\",\n    \"#minecraft:shulker_boxes\"\n  ]\n}\n";
	public static final String ICON_NBT = "tag:{bjshop:{}}";
	public static final String RETURNED_ITEM = "{Item:{id:\"minecraft:stone\",Count:1b},Tags:[\"bjshop.ret\"]}";
	public static final String CLEAR_NAME = "execute if data block ~ ~ ~ CustomName run data remove block ~ ~ ~ CustomName";
	public static final String DROPPED = "execute as @e[type=minecraft:item,nbt={Item:{tag:{bjshop:{}}}}] run function bjshop:dropped";
	private static final String ICONS = "icons";

	private ShopCompat() {}

	public static String anyIcon() {
		return "#bjshop:" + ICONS + "{bjshop:{}}";
	}

	public static String shopIcon(String shop) {
		return "#bjshop_" + shop + ":" + ICONS + "{bjshop:{s:\"" + shop + "\"}}";
	}

	public static String pageIcon(String shop, int page) {
		return "#bjshop_" + shop + ":" + ICONS + "{bjshop:{s:\"" + shop + "\",p:" + page + "}}";
	}

	public static String slotIcon(String shop, int page, int slot, ItemStack icon) {
		return BuiltInRegistries.ITEM.getKey(icon.getItem()) + mark(shop, page, slot);
	}

	private static String mark(String shop, int page, int slot) {
		return "{bjshop:{s:\"" + shop + "\",p:" + page + ",i:" + slot + "}}";
	}

	public static String occupied(int slot) {
		return "if data block ~ ~ ~ Items[{Slot:" + slot + "b}]";
	}

	public static String missing(int slot, String icon) {
		int split = icon.indexOf('{');
		return "unless data block ~ ~ ~ Items[{Slot:" + slot + "b,id:\"" + icon.substring(0, split) + "\",tag:" + icon.substring(split) + "}]";
	}

	public static void globalFiles(Collection<String> shops, Map<String, String> out) {
		StringBuilder values = new StringBuilder();
		for (String shop : new TreeSet<>(shops)) {
			values.append(values.length() == 0 ? "" : ",\n").append("    {\"id\": \"#bjshop_").append(shop).append(':').append(ICONS).append("\", \"required\": false}");
		}
		out.put(ShopPack.PACK + "/data/bjshop/tags/items/" + ICONS + ".json", "{\n  \"values\": [\n" + values + (values.length() == 0 ? "" : "\n") + "  ]\n}\n");
	}

	public static void shopFiles(String shop, List<ItemStack> icons, Map<String, String> out) {
		TreeSet<String> ids = new TreeSet<>();
		for (ItemStack icon : icons) {
			if (!icon.isEmpty()) ids.add(BuiltInRegistries.ITEM.getKey(icon.getItem()).toString());
		}
		StringBuilder values = new StringBuilder();
		for (String id : ids) values.append(values.length() == 0 ? "" : ",\n").append("    \"").append(id).append('"');
		out.put(ShopPack.shopDir(shop) + "/tags/items/" + ICONS + ".json", "{\n  \"values\": [\n" + values + (values.length() == 0 ? "" : "\n") + "  ]\n}\n");
	}

	public static ItemStack icon(ItemStack item, int count, List<Component> lore, String shop, int page, int slot) {
		ItemStack stack = item.copyWithCount(count);
		if (!lore.isEmpty()) {
			CompoundTag display = stack.getOrCreateTagElement("display");
			ListTag lines = display.getList("Lore", Tag.TAG_STRING);
			for (Component line : lore) lines.add(StringTag.valueOf(Component.Serializer.toJson(line)));
			display.put("Lore", lines);
		}
		CompoundTag mark = new CompoundTag();
		mark.putString("s", shop);
		mark.putInt("p", page);
		mark.putInt("i", slot);
		stack.getOrCreateTag().put("bjshop", mark);
		return stack;
	}

	public static ItemStack filler() {
		ItemStack stack = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
		stack.setHoverName(Component.literal(" "));
		return stack;
	}

	public static boolean sameItem(ItemStack a, ItemStack b) {
		return ItemStack.isSameItemSameTags(a, b);
	}

	public static String itemArg(ItemStack stack) {
		return GiveCommands.item(stack);
	}

	public static String payArg(ItemStack stack, boolean strict) {
		return GiveCommands.item(stack);
	}

	public static String textArg(Component text) {
		return Component.Serializer.toJson(text);
	}

	public static String nameNbt(Component text) {
		return "\"" + textArg(text).replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
	}

	public static String stackSnbt(ItemStack stack) {
		if (stack == null || stack.isEmpty()) return "";
		return GiveCommands.snbt(stack.copyWithCount(1).save(new CompoundTag()));
	}

	public static ItemStack stackFromSnbt(String snbt) {
		if (snbt == null || snbt.isEmpty()) return ItemStack.EMPTY;
		try {
			return ItemStack.of(TagParser.parseTag(snbt));
		} catch (Exception e) {
			return ItemStack.EMPTY;
		}
	}
}
