package cn.erindax.bjmapedit.client.widget;

import com.mojang.serialization.DynamicOps;
import net.minecraft.client.gui.Font;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.HangingSignItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SignItem;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.properties.WoodType;

import java.util.ArrayList;
import java.util.List;

public final class SignTexts {
	public static final int LINE_HEIGHT = 10;
	public static final int MAX_WIDTH = 90;
	private static List<Item> items;

	private SignTexts() {}

	public static List<Item> items() {
		if (items == null) {
			List<Item> list = new ArrayList<>();
			for (Item item : BuiltInRegistries.ITEM) {
				if (item instanceof SignItem && !(item instanceof HangingSignItem)) list.add(item);
			}
			if (list.isEmpty()) list.add(Items.OAK_SIGN);
			items = list;
		}
		return items;
	}

	public static String id(Item item) {
		return BuiltInRegistries.ITEM.getKey(item).toString();
	}

	public static int index(String id) {
		List<Item> list = items();
		for (int i = 0; i < list.size(); i++) {
			if (id(list.get(i)).equals(id)) return i;
		}
		for (int i = 0; i < list.size(); i++) {
			if (list.get(i) == Items.OAK_SIGN) return i;
		}
		return 0;
	}

	public static Item item(String id) {
		return items().get(index(id));
	}

	public static String cycle(String id, int delta) {
		List<Item> list = items();
		return id(list.get(Math.floorMod(index(id) + delta, list.size())));
	}

	public static String blockEntityId() {
		return String.valueOf(BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(BlockEntityType.SIGN));
	}

	public static WoodType wood(Item item) {
		if (item instanceof BlockItem block && block.getBlock() instanceof SignBlock sign) return sign.type();
		return WoodType.OAK;
	}

	public static String idFor(WoodType wood) {
		for (Item item : items()) {
			if (wood(item) == wood) return id(item);
		}
		return "";
	}

	public static ResourceLocation texture(Item item) {
		ResourceLocation wood = ResourceLocation.tryParse(wood(item).name());
		if (wood == null) wood = ResourceLocation.withDefaultNamespace("oak");
		return ResourceLocation.fromNamespaceAndPath(wood.getNamespace(), "textures/entity/signs/" + wood.getPath() + ".png");
	}

	public static DyeColor dye(String name) {
		return DyeColor.byName(name == null ? "" : name, DyeColor.BLACK);
	}

	public static String cycleDye(String name, int delta) {
		DyeColor[] all = DyeColor.values();
		return all[Math.floorMod(dye(name).ordinal() + delta, all.length)].getName();
	}

	public static int textColor(DyeColor dye) {
		return 0xFF000000 | dye.getTextColor();
	}

	public static int darkColor(DyeColor dye, boolean glow) {
		int c = dye.getTextColor();
		if (glow && c == DyeColor.BLACK.getTextColor()) return 0xFFF0EBCC;
		int r = (int) ((c >> 16 & 255) * 0.4);
		int g = (int) ((c >> 8 & 255) * 0.4);
		int b = (int) ((c & 255) * 0.4);
		return 0xFF000000 | r << 16 | g << 8 | b;
	}

	public static Component line(String raw, String command) {
		Component body = SectionText.toComponent(raw == null ? "" : raw, Style.EMPTY);
		MutableComponent root = body instanceof MutableComponent m ? m : Component.empty().append(body);
		ClickEvent click = TextClicks.toEvent(new TextClicks.Click(TextClicks.RUN_COMMAND, command));
		if (click != null) root.setStyle(root.getStyle().withClickEvent(click));
		return root;
	}

	public static String command(Component line) {
		TextClicks.Click c = TextClicks.fromEvent(line.getStyle().getClickEvent());
		return c != null && TextClicks.RUN_COMMAND.equals(c.action) ? c.value : "";
	}

	public static String plain(String raw) {
		return SectionText.toComponent(raw == null ? "" : raw, Style.EMPTY).getString().trim();
	}

	public static FormattedCharSequence renderLine(Font font, String raw) {
		List<FormattedCharSequence> parts = font.split(SectionText.toComponent(raw == null ? "" : raw, Style.EMPTY), MAX_WIDTH);
		return parts.isEmpty() ? FormattedCharSequence.EMPTY : parts.get(0);
	}

	public static FormattedCharSequence recolor(FormattedCharSequence seq, int rgb) {
		int color = rgb & 0xFFFFFF;
		return sink -> seq.accept((index, style, codePoint) -> sink.accept(index, style.withColor(color), codePoint));
	}

	public static SignText text(SignData.Side side) {
		Component[] lines = new Component[SignData.LINES];
		for (int i = 0; i < SignData.LINES; i++) lines[i] = line(side.line(i), side.command(i));
		return new SignText(lines, lines, dye(side.color), side.glow);
	}

	public static CompoundTag blockTag(SignData data, boolean replaceFiltered) {
		CompoundTag tag = new CompoundTag();
		tag.put("front_text", sideTag(data.side(0), replaceFiltered));
		tag.put("back_text", sideTag(data.side(1), replaceFiltered));
		tag.putBoolean("is_waxed", data.waxed);
		return tag;
	}

	private static CompoundTag sideTag(SignData.Side side, boolean replaceFiltered) {
		Tag encoded = SignText.DIRECT_CODEC.encodeStart(ops(), text(side)).result().orElse(null);
		CompoundTag tag = encoded instanceof CompoundTag c ? c : new CompoundTag();
		Tag messages = tag.get("messages");
		if (replaceFiltered && messages != null) tag.put("filtered_messages", messages.copy());
		return tag;
	}

	private static DynamicOps<Tag> ops() {
		HolderLookup.Provider regs = GiveCommands.registries();
		return regs != null ? regs.createSerializationContext(NbtOps.INSTANCE) : NbtOps.INSTANCE;
	}

	public static SignData read(SignBlockEntity sign) {
		SignData d = new SignData();
		d.wood = idFor(SignBlock.getWoodType(sign.getBlockState().getBlock()));
		d.waxed = sign.isWaxed();
		readSide(sign.getFrontText(), d.side(0));
		readSide(sign.getBackText(), d.side(1));
		return d;
	}

	private static void readSide(SignText text, SignData.Side side) {
		side.color = text.getColor().getName();
		side.glow = text.hasGlowingText();
		for (int i = 0; i < SignData.LINES; i++) {
			Component c = text.getMessage(i, false);
			side.set(i, SectionText.fromComponent(c), command(c));
		}
	}
}
