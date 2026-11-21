package cn.erindax.bjmapedit.client.widget;

import com.mojang.brigadier.StringReader;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.arguments.item.ItemPredicateArgument;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public final class ShopCompat {
	public static final String META = "{\n  \"pack\": {\n    \"pack_format\": 48,\n    \"description\": \"BJ-MapEdit Shops\"\n  }\n}\n";
	public static final String FUNCTIONS = "function";
	public static final String FUNCTION_TAGS = "tags/function";
	public static final String BLOCK_TAGS = "tags/block";
	public static final String BOXES = "{\n  \"values\": [\n    \"minecraft:chest\",\n    \"minecraft:trapped_chest\",\n    \"minecraft:barrel\",\n    \"#minecraft:shulker_boxes\"\n  ]\n}\n";
	public static final String ICON_NBT = "components:{\"minecraft:custom_data\":{bjshop:{}}}";
	public static final String RETURNED_ITEM = "{Item:{id:\"minecraft:stone\",count:1},Tags:[\"bjshop.ret\"]}";
	public static final String CLEAR_NAME = "execute if data block ~ ~ ~ CustomName run data modify block ~ ~ ~ CustomName set value \"\"";
	public static final String DROPPED = "execute as @e[type=minecraft:item] if items entity @s contents *[minecraft:custom_data~{bjshop:{}}] run function bjshop:dropped";

	private ShopCompat() {}

	public static String anyIcon() {
		return "*[minecraft:custom_data~{bjshop:{}}]";
	}

	public static String shopIcon(String shop) {
		return "*[minecraft:custom_data~{bjshop:{s:\"" + shop + "\"}}]";
	}

	public static String pageIcon(String shop, int page) {
		return "*[minecraft:custom_data~{bjshop:{s:\"" + shop + "\",p:" + page + "}}]";
	}

	public static String slotIcon(String shop, int page, int slot, ItemStack icon) {
		return "*[minecraft:custom_data~{bjshop:{s:\"" + shop + "\",p:" + page + ",i:" + slot + "}}]";
	}

	public static String occupied(int slot) {
		return "if items block ~ ~ ~ container." + slot + " *";
	}

	public static String missing(int slot, String icon) {
		return "unless items block ~ ~ ~ container." + slot + " " + icon;
	}

	public static void globalFiles(Collection<String> shops, Map<String, String> out) {
	}

	public static void shopFiles(String shop, List<ItemStack> icons, Map<String, String> out) {
	}

	public static ItemStack icon(ItemStack item, int count, List<Component> lore, String shop, int page, int slot) {
		ItemStack stack = item.copyWithCount(count);
		if (!lore.isEmpty()) {
			List<Component> lines = new ArrayList<>(stack.getOrDefault(DataComponents.LORE, ItemLore.EMPTY).lines());
			lines.addAll(lore);
			stack.set(DataComponents.LORE, new ItemLore(lines));
		}
		CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
			CompoundTag mark = new CompoundTag();
			mark.putString("s", shop);
			mark.putInt("p", page);
			mark.putInt("i", slot);
			tag.put("bjshop", mark);
		});
		return stack;
	}

	public static ItemStack filler() {
		ItemStack stack = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
		stack.set(DataComponents.CUSTOM_NAME, Component.literal(" "));
		return stack;
	}

	public static boolean sameItem(ItemStack a, ItemStack b) {
		return ItemStack.isSameItemSameComponents(a, b);
	}

	public static String itemArg(ItemStack stack) {
		return GiveCommands.item(stack, null, true);
	}

	public static String payArg(ItemStack stack, boolean strict) {
		if (!strict) return GiveCommands.item(stack, null);
		String arg = GiveCommands.strict(stack);
		return arg != null && matches(arg, stack) ? arg : null;
	}

	private static boolean matches(String predicate, ItemStack stack) {
		var connection = Minecraft.getInstance().getConnection();
		if (connection == null) return true;
		try {
			CommandBuildContext context = CommandBuildContext.simple(connection.registryAccess(), connection.enabledFeatures());
			return ItemPredicateArgument.itemPredicate(context).parse(new StringReader(predicate)).test(stack);
		} catch (Exception e) {
			return false;
		}
	}

	public static String textArg(Component text) {
		Minecraft mc = Minecraft.getInstance();
		RegistryAccess regs = mc.level != null ? mc.level.registryAccess() : RegistryAccess.EMPTY;
		return Component.Serializer.toJson(text, regs);
	}

	public static String nameNbt(Component text) {
		return "\"" + textArg(text).replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
	}

	public static String stackSnbt(ItemStack stack) {
		Minecraft mc = Minecraft.getInstance();
		if (stack == null || stack.isEmpty() || mc.level == null) return "";
		try {
			return GiveCommands.snbt(stack.copyWithCount(1).save(mc.level.registryAccess()));
		} catch (RuntimeException e) {
			return "";
		}
	}

	public static ItemStack stackFromSnbt(String snbt) {
		Minecraft mc = Minecraft.getInstance();
		if (snbt == null || snbt.isEmpty() || mc.level == null) return ItemStack.EMPTY;
		try {
			return ItemStack.parse(mc.level.registryAccess(), TagParser.parseTag(snbt)).orElse(ItemStack.EMPTY);
		} catch (Exception e) {
			return ItemStack.EMPTY;
		}
	}
}
