package cn.erindax.bjmapedit.client.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class ItemNames {

	public record Entry(ResourceLocation id, String idText, String idLower, String pathLower, String name, String nameLower) {}

	private static List<Entry> cache;
	private static String cacheLang;

	private ItemNames() {}

	public static List<Entry> all() {
		String lang = Minecraft.getInstance().getLanguageManager().getSelected();
		if (cache != null && lang.equals(cacheLang)) return cache;
		List<Entry> out = new ArrayList<>();
		for (ResourceLocation id : BuiltInRegistries.ITEM.keySet()) {
			Item item = BuiltInRegistries.ITEM.getValue(id);
			if (item == Items.AIR) continue;
			String name;
			try {
				name = item.getName(new ItemStack(item)).getString();
			} catch (Throwable t) {
				name = id.toString();
			}
			String idText = id.toString();
			out.add(new Entry(id, idText, idText.toLowerCase(Locale.ROOT), id.getPath().toLowerCase(Locale.ROOT),
				name, name.toLowerCase(Locale.ROOT)));
		}
		out.sort(Comparator.comparing(Entry::idText));
		cache = List.copyOf(out);
		cacheLang = lang;
		return cache;
	}
}
