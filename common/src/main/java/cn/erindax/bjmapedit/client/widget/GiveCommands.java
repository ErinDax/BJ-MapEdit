package cn.erindax.bjmapedit.client.widget;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.component.Consumable;

public final class GiveCommands {
	private GiveCommands() {}

	public static String give(ItemStack stack) {
		return give(stack, null);
	}

	public static String give(ItemStack stack, List<String> skipped) {
		if (stack == null || stack.isEmpty()) return "";
		return "/give @p " + item(stack, skipped) + " " + Math.max(1, stack.getCount());
	}

	public static String item(ItemStack stack, List<String> skipped) {
		return item(stack, skipped, false);
	}

	public static String item(ItemStack stack, List<String> skipped, boolean removals) {
		HolderLookup.Provider regs = registries();
		DynamicOps<Tag> ops = regs != null
			? regs.createSerializationContext(NbtOps.INSTANCE)
			: NbtOps.INSTANCE;
		String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
		StringBuilder cmd = new StringBuilder(itemId);
		DataComponentPatch patch = stack.getComponentsPatch();
		if (!patch.isEmpty()) {
			List<String> parts = new ArrayList<>();
			for (var entry : patch.entrySet()) {
				if (entry.getValue().isEmpty() && !removals) continue;
				Identifier key = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(entry.getKey());
				if (key == null) {
					if (skipped != null) skipped.add(String.valueOf(entry.getKey()));
					continue;
				}
				if (entry.getValue().isEmpty()) {
					parts.add("!" + componentKey(key));
					continue;
				}
				Tag tag = encode(ops, entry.getKey(), entry.getValue().get());
				if (tag == null) {
					if (skipped != null) skipped.add(componentKey(key));
					continue;
				}
				parts.add(componentKey(key) + "=" + compactSnbt(tag));
			}
			if (!parts.isEmpty()) {
				cmd.append('[').append(String.join(",", parts)).append(']');
			}
		}
		return cmd.toString();
	}

	public static String strict(ItemStack stack) {
		HolderLookup.Provider regs = registries();
		DynamicOps<Tag> ops = regs != null
			? regs.createSerializationContext(NbtOps.INSTANCE)
			: NbtOps.INSTANCE;
		DataComponentPatch patch = stack.getComponentsPatch();
		List<String> parts = new ArrayList<>();
		for (DataComponentType<?> type : BuiltInRegistries.DATA_COMPONENT_TYPE) {
			if (type.isTransient()) continue;
			Identifier key = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(type);
			if (key == null) continue;
			boolean vanilla = "minecraft".equals(key.getNamespace());
			Object value = stack.get(type);
			if (value == null) {
				if (vanilla) parts.add("!" + key.getPath());
				continue;
			}
			if (!vanilla && patch.get(type) == null) continue;
			Tag tag = encode(ops, type, value);
			if (tag == null) return null;
			parts.add(comparable(ops, type, tag, value) ? componentKey(key) + "=" + compactSnbt(tag) : componentKey(key));
		}
		return BuiltInRegistries.ITEM.getKey(stack.getItem()) + "[" + String.join(",", parts) + "]";
	}

	public static void copy(String command) {
		if (command == null || command.isEmpty()) return;
		Minecraft mc = Minecraft.getInstance();
		mc.keyboardHandler.setClipboard(command);
		if (mc.player != null) {
			mc.player.displayClientMessage(Component.translatable("screen.bj_mapedit.copied"), true);
		}
	}

	private static String componentKey(Identifier key) {
		return "minecraft".equals(key.getNamespace()) ? key.getPath() : key.toString();
	}

	static HolderLookup.Provider registries() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level != null) return mc.level.registryAccess();
		if (mc.getConnection() != null) return mc.getConnection().registryAccess();
		return null;
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static Tag encode(DynamicOps<Tag> ops, DataComponentType<?> type, Object value) {
		try {
			Codec codec = type.codecOrThrow();
			return (Tag) codec.encodeStart(ops, value).result().orElse(null);
		} catch (Exception ignored) {
			return null;
		}
	}

	private static boolean comparable(DynamicOps<Tag> ops, DataComponentType<?> type, Tag tag, Object value) {
		if (value instanceof Consumable consumable && !consumable.onConsumeEffects().isEmpty()) return false;
		return readsBackEqual(ops, type, tag, value);
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static boolean readsBackEqual(DynamicOps<Tag> ops, DataComponentType<?> type, Tag tag, Object value) {
		try {
			Codec codec = type.codecOrThrow();
			return value.equals(codec.parse(ops, tag).result().orElse(null));
		} catch (Exception ignored) {
			return false;
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
			for (String key : compound.keySet()) {
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
			writeQuoted(sb, stringTag.value());
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
