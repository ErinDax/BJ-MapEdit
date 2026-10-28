package cn.erindax.bjmapedit.client.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public final class GiveCommands {
	private GiveCommands() {}

	public static String give(ItemStack stack) {
		if (stack == null || stack.isEmpty()) return "";
		String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
		StringBuilder cmd = new StringBuilder("/give @p ").append(itemId);
		CompoundTag tag = stack.getTag();
		if (tag != null && !tag.isEmpty()) {
			cmd.append(compactSnbt(tag));
		}
		return cmd.append(' ').append(Math.max(1, stack.getCount())).toString();
	}

	public static void copy(String command) {
		if (command == null || command.isEmpty()) return;
		Minecraft mc = Minecraft.getInstance();
		mc.keyboardHandler.setClipboard(command);
		if (mc.player != null) {
			mc.player.displayClientMessage(Component.translatable("screen.bj_mapedit.copied"), true);
		}
	}

	public static String snbt(Tag tag) {
		return compactSnbt(tag);
	}

	static String compactSnbt(Tag tag) {
		StringBuilder sb = new StringBuilder();
		writeSnbt(sb, tag);
		return sb.toString();
	}

	private static void writeSnbt(StringBuilder sb, Tag tag) {
		if (tag instanceof CompoundTag compound) {
			sb.append('{');
			boolean first = true;
			for (String key : compound.getAllKeys()) {
				Tag child = compound.get(key);
				if (child == null) continue;
				if (!first) sb.append(',');
				first = false;
				writeKey(sb, key);
				sb.append(':');
				writeSnbt(sb, child);
			}
			sb.append('}');
			return;
		}
		if (tag instanceof ListTag list) {
			sb.append('[');
			for (int i = 0; i < list.size(); i++) {
				if (i > 0) sb.append(',');
				writeSnbt(sb, list.get(i));
			}
			sb.append(']');
			return;
		}
		if (tag instanceof StringTag stringTag) {
			writeQuoted(sb, stringTag.getAsString());
			return;
		}
		if (tag instanceof ByteArrayTag bytes) {
			sb.append("[B;");
			byte[] arr = bytes.getAsByteArray();
			for (int i = 0; i < arr.length; i++) {
				if (i > 0) sb.append(',');
				sb.append(arr[i]).append('b');
			}
			sb.append(']');
			return;
		}
		if (tag instanceof IntArrayTag ints) {
			sb.append("[I;");
			int[] arr = ints.getAsIntArray();
			for (int i = 0; i < arr.length; i++) {
				if (i > 0) sb.append(',');
				sb.append(arr[i]);
			}
			sb.append(']');
			return;
		}
		if (tag instanceof LongArrayTag longs) {
			sb.append("[L;");
			long[] arr = longs.getAsLongArray();
			for (int i = 0; i < arr.length; i++) {
				if (i > 0) sb.append(',');
				sb.append(arr[i]).append('L');
			}
			sb.append(']');
			return;
		}
		sb.append(tag.toString().replace("\r", "").replace("\n", ""));
	}

	private static void writeKey(StringBuilder sb, String key) {
		if (isBareKey(key)) sb.append(key);
		else writeQuoted(sb, key);
	}

	private static boolean isBareKey(String key) {
		if (key.isEmpty()) return false;
		for (int i = 0; i < key.length(); i++) {
			char c = key.charAt(i);
			boolean ok = c >= '0' && c <= '9' || c >= 'A' && c <= 'Z' || c >= 'a' && c <= 'z'
				|| c == '_' || c == '-' || c == '+' || c == '.';
			if (!ok) return false;
		}
		return true;
	}

	private static void writeQuoted(StringBuilder sb, String raw) {
		boolean useSingle = raw.indexOf('"') >= 0;
		char quote = useSingle ? '\'' : '"';
		sb.append(quote);
		for (int i = 0; i < raw.length(); i++) {
			char c = raw.charAt(i);
			switch (c) {
				case '\\' -> sb.append("\\\\");
				case '\n' -> sb.append("\\n");
				case '\r' -> sb.append("\\r");
				case '\t' -> sb.append("\\t");
				default -> {
					if (c == quote) sb.append('\\');
					if (c >= 32) sb.append(c);
					else sb.append(String.format("\\u%04x", (int) c));
				}
			}
		}
		sb.append(quote);
	}
}
