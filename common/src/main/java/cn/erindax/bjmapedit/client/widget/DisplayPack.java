package cn.erindax.bjmapedit.client.widget;

import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.math.Transformation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Interaction;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public final class DisplayPack {
	public static final String PACK = "bjdisp";
	public static final String PROJECT_DIR = PACK + "/data/" + PACK + "/bjmapedit";
	private static final String DATA = PACK + "/data/";
	private static final String FN = DATA + PACK + "/function/";
	private static final String META = "{\n  \"pack\": {\n    \"pack_format\": 48,\n    \"description\": \"BJ-MapEdit Display & Interaction\"\n  }\n}\n";
	private static final String ADV_ATK = "{\"criteria\":{\"a\":{\"trigger\":\"minecraft:player_hurt_entity\",\"conditions\":{\"entity\":{\"nbt\":\"{Tags:[\\\"bjdisp.i\\\"]}\"}}}},\"rewards\":{\"function\":\"bjdisp:z/on_atk\"}}\n";
	private static final String ADV_USE = "{\"criteria\":{\"a\":{\"trigger\":\"minecraft:player_interacted_with_entity\",\"conditions\":{\"entity\":{\"nbt\":\"{Tags:[\\\"bjdisp.i\\\"]}\"}}}},\"rewards\":{\"function\":\"bjdisp:z/on_use\"}}\n";

	private DisplayPack() {}

	public static String projectFile(String id) {
		return PROJECT_DIR + "/" + id + ".json";
	}

	public static String modelDir(String id) {
		return FN + id;
	}

	public static String summonFnCommand(String id) {
		return "function bjdisp:" + id + "/summon";
	}

	public static String removeFnCommand(String id) {
		return "function bjdisp:" + id + "/remove";
	}

	public static String eggCommand(String id) {
		return "give @s minecraft:allay_spawn_egg[minecraft:entity_data={id:\"minecraft:marker\",Tags:[\"bjdisp.egg\",\"bjdisp.e." + id + "\"]},minecraft:item_name='\"" + id + "\"']";
	}

	public static List<String> dispIds(String tree) {
		Set<String> ids = new TreeSet<>();
		if (tree == null) return new ArrayList<>();
		String prefix = "F\t" + PROJECT_DIR + "/";
		for (String line : tree.split("\n")) {
			if (!line.startsWith(prefix) || !line.endsWith(".json")) continue;
			String id = line.substring(prefix.length(), line.length() - 5);
			if (DisplayData.validId(id)) ids.add(id);
		}
		return new ArrayList<>(ids);
	}

	public static Float parseFloatRange(String raw, float min, float max) {
		try {
			float v = Float.parseFloat(raw == null ? "" : raw.trim());
			if (Float.isNaN(v) || Float.isInfinite(v) || v < min || v > max) return null;
			return v;
		} catch (NumberFormatException e) {
			return null;
		}
	}

	public static int intRange(String raw, int min, int max) {
		String s = raw == null ? "" : raw.trim();
		if (!s.matches("\\d{1,5}")) return -1;
		int v = Integer.parseInt(s);
		return v < min || v > max ? -1 : v;
	}

	public static Integer parseRgb(String raw) {
		String s = raw == null ? "" : raw.trim();
		if (s.startsWith("#")) s = s.substring(1);
		if (!s.matches("[0-9a-fA-F]{6}")) return null;
		return Integer.parseInt(s, 16);
	}

	public static Integer parseArgb(String raw) {
		String s = raw == null ? "" : raw.trim();
		if (s.startsWith("#")) s = s.substring(1);
		if (s.matches("[0-9a-fA-F]{6}")) return (int) Long.parseLong("80" + s, 16);
		if (s.matches("[0-9a-fA-F]{8}")) return (int) Long.parseLong(s, 16);
		return null;
	}

	public static String cleanOptionalCommand(String raw) {
		String s = raw == null ? "" : raw.trim();
		if (s.isEmpty()) return "";
		if (s.startsWith("/")) s = s.substring(1).trim();
		if (s.isEmpty() || s.startsWith("#") || s.startsWith("$") || s.indexOf('\n') >= 0 || s.indexOf('\r') >= 0) return null;
		ClientPacketListener conn = Minecraft.getInstance().getConnection();
		if (conn != null && conn.getCommands().getRoot().getChild("function") != null) {
			ParseResults<SharedSuggestionProvider> res = conn.getCommands().parse(s, conn.getSuggestionsProvider());
			if (!res.getExceptions().isEmpty() || res.getReader().canRead()) return null;
		}
		return s;
	}

	public static String validItem(String raw) {
		ResourceLocation id = ResourceLocation.tryParse(raw == null ? "" : raw.trim());
		if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) return null;
		Item it = BuiltInRegistries.ITEM.getValue(id);
		return it == Items.AIR ? null : id.toString();
	}

	public static String blockStateNbt(String raw) {
		String s = raw == null ? "" : raw.trim();
		if (s.isEmpty()) return null;
		try {
			BlockState state = BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK, s, false).blockState();
			ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
			StringBuilder sb = new StringBuilder("{Name:\"").append(id).append("\"");
			if (!state.getValues().isEmpty()) {
				sb.append(",Properties:{");
				boolean first = true;
				for (Map.Entry<Property<?>, Comparable<?>> e : state.getValues().entrySet()) {
					if (!first) sb.append(',');
					first = false;
					sb.append(e.getKey().getName()).append(":\"").append(propName(e.getKey(), e.getValue())).append("\"");
				}
				sb.append('}');
			}
			sb.append('}');
			return sb.toString();
		} catch (CommandSyntaxException e) {
			return null;
		}
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private static String propName(Property prop, Comparable val) {
		return prop.getName(val);
	}

	public static String problem(DisplayData d) {
		if (!DisplayData.validId(d.id)) return "prop_bad_id";
		if (d.hasInteraction()) {
			if (parseFloatRange(d.width, 0.05F, 32.0F) == null || parseFloatRange(d.height, 0.05F, 32.0F) == null) return "disp_bad_size";
			if (intRange(d.cooldown, 0, 72000) < 0) return "disp_bad_cd";
			if (cleanOptionalCommand(d.leftCmd) == null) return "disp_bad_left_cmd";
			if (cleanOptionalCommand(d.rightCmd) == null) return "disp_bad_right_cmd";
		}
		if (d.kind != DisplayData.KIND_INTERACT) {
			if (parseFloatRange(d.tx, -64.0F, 64.0F) == null || parseFloatRange(d.ty, -64.0F, 64.0F) == null || parseFloatRange(d.tz, -64.0F, 64.0F) == null) return "disp_bad_offset";
			if (parseFloatRange(d.sx, 0.01F, 64.0F) == null || parseFloatRange(d.sy, 0.01F, 64.0F) == null || parseFloatRange(d.sz, 0.01F, 64.0F) == null) return "disp_bad_scale";
			if (parseFloatRange(d.yaw, -360.0F, 360.0F) == null || parseFloatRange(d.pitch, -360.0F, 360.0F) == null || parseFloatRange(d.roll, -360.0F, 360.0F) == null) return "disp_bad_rot";
		}
		if (d.kind == DisplayData.KIND_TEXT) {
			if (parseRgb(d.textColor) == null) return "disp_bad_color";
			if (intRange(d.lineWidth, 10, 2000) < 0) return "disp_bad_line_w";
			if (d.bgMode == DisplayData.BG_CUSTOM && parseArgb(d.bgHex) == null) return "disp_bad_bg";
		} else if (d.kind == DisplayData.KIND_ITEM) {
			if (d.itemSnbt.isEmpty() && validItem(d.item) == null) return "prop_bad_item";
			if (!d.itemSnbt.isEmpty() && ShopCompat.stackFromSnbt(d.itemSnbt).isEmpty()) return "prop_bad_item";
		} else if (d.kind == DisplayData.KIND_BLOCK) {
			if (blockStateNbt(d.block) == null) return "disp_bad_block";
		}
		return null;
	}

	public static Component buildTextComponent(DisplayData d) {
		Integer rgb = parseRgb(d.textColor);
		Style style = Style.EMPTY;
		if (rgb != null) style = style.withColor(rgb);
		if (d.bold) style = style.withBold(true);
		if (d.italic) style = style.withItalic(true);
		if (d.underline) style = style.withUnderlined(true);
		if (d.strikethrough) style = style.withStrikethrough(true);
		if (d.obfuscated) style = style.withObfuscated(true);
		return SectionText.toComponent(PropPack.ampersandCodes(d.text), style);
	}

	public static ItemStack buildItemStack(DisplayData d) {
		if (!d.itemSnbt.isEmpty()) return ShopCompat.stackFromSnbt(d.itemSnbt).copyWithCount(1);
		String id = validItem(d.item);
		return id == null ? ItemStack.EMPTY : new ItemStack(BuiltInRegistries.ITEM.getValue(ResourceLocation.parse(id)));
	}

	private static String fmt(float v) {
		String s = new BigDecimal(String.format(Locale.ROOT, "%.4f", v)).stripTrailingZeros().toPlainString();
		return ("-0".equals(s) ? "0" : s) + "f";
	}

	public static String interactionNbt(DisplayData d, boolean withTags) {
		float w = Math.max(0.05F, parseFloatRange(d.width, 0.05F, 32.0F));
		float h = Math.max(0.05F, parseFloatRange(d.height, 0.05F, 32.0F));
		StringBuilder sb = new StringBuilder("{width:").append(fmt(w)).append(",height:").append(fmt(h))
			.append(",response:").append(d.response ? "1b" : "0b");
		if (withTags) sb.append(",Tags:[\"bjdisp.all\",\"bjdisp.i\",\"bjdisp.c.").append(d.id).append("\"]");
		sb.append('}');
		return sb.toString();
	}

	public static String displayEntityType(int kind) {
		return switch (kind) {
			case DisplayData.KIND_TEXT -> "minecraft:text_display";
			case DisplayData.KIND_ITEM -> "minecraft:item_display";
			case DisplayData.KIND_BLOCK -> "minecraft:block_display";
			default -> "minecraft:interaction";
		};
	}

	public static String displayNbt(DisplayData d, boolean withTags) {
		float tx = parseFloatRange(d.tx, -64.0F, 64.0F);
		float ty = parseFloatRange(d.ty, -64.0F, 64.0F);
		float tz = parseFloatRange(d.tz, -64.0F, 64.0F);
		float sx = parseFloatRange(d.sx, 0.01F, 64.0F);
		float sy = parseFloatRange(d.sy, 0.01F, 64.0F);
		float sz = parseFloatRange(d.sz, 0.01F, 64.0F);
		float yaw = parseFloatRange(d.yaw, -360.0F, 360.0F);
		float pitch = parseFloatRange(d.pitch, -360.0F, 360.0F);
		float roll = parseFloatRange(d.roll, -360.0F, 360.0F);
		Quaternionf q = new Quaternionf().rotateYXZ((float) Math.toRadians(yaw), (float) Math.toRadians(pitch), (float) Math.toRadians(roll));
		StringBuilder sb = new StringBuilder("{transformation:{translation:[")
			.append(fmt(tx)).append(',').append(fmt(ty)).append(',').append(fmt(tz))
			.append("],left_rotation:[").append(fmt(q.x)).append(',').append(fmt(q.y)).append(',').append(fmt(q.z)).append(',').append(fmt(q.w))
			.append("],scale:[").append(fmt(sx)).append(',').append(fmt(sy)).append(',').append(fmt(sz))
			.append("],right_rotation:[0f,0f,0f,1f]}");
		sb.append(",billboard:\"").append(DisplayData.BILLBOARDS[d.billboard]).append("\"");
		if (d.fullBright) sb.append(",brightness:{block:15,sky:15}");
		sb.append(",Glowing:").append(d.glowing ? "1b" : "0b");
		if (withTags) sb.append(",Tags:[\"bjdisp.all\",\"bjdisp.d\",\"bjdisp.c.").append(d.id).append("\"]");

		if (d.kind == DisplayData.KIND_TEXT) {
			sb.append(",text:").append(ShopCompat.nameNbt(buildTextComponent(d)));
			sb.append(",alignment:\"").append(DisplayData.ALIGNS[d.align]).append("\"");
			sb.append(",line_width:").append(Math.max(10, intRange(d.lineWidth, 10, 2000)));
			sb.append(",shadow:").append(d.textShadow ? "1b" : "0b");
			sb.append(",see_through:").append(d.seeThrough ? "1b" : "0b");
			if (d.bgMode == DisplayData.BG_DEFAULT) {
				sb.append(",default_background:1b");
			} else if (d.bgMode == DisplayData.BG_CLEAR) {
				sb.append(",default_background:0b,background:0");
			} else {
				Integer argb = parseArgb(d.bgHex);
				sb.append(",default_background:0b,background:").append(argb == null ? 0 : argb);
			}
		} else if (d.kind == DisplayData.KIND_ITEM) {
			Minecraft mc = Minecraft.getInstance();
			ItemStack stack = buildItemStack(d);
			if (!stack.isEmpty() && mc.level != null) {
				Tag saved = stack.save(mc.level.registryAccess());
				sb.append(",item:").append(GiveCommands.snbt(saved));
			} else {
				sb.append(",item:{id:\"").append(validItem(d.item)).append("\",count:1}");
			}
			sb.append(",item_display:\"").append(DisplayData.ITEM_CONTEXTS[d.itemContext]).append("\"");
		} else if (d.kind == DisplayData.KIND_BLOCK) {
			sb.append(",block_state:").append(blockStateNbt(d.block));
		}
		sb.append('}');
		return sb.toString();
	}

	public static List<String> directSummonCommands(DisplayData d) {
		List<String> cmds = new ArrayList<>();
		String posPrefix = d.centerSnap ? "execute align xyz positioned ~0.5 ~ ~0.5 run " : "";
		if (d.kind == DisplayData.KIND_INTERACT) {
			cmds.add(posPrefix + "summon minecraft:interaction ~ ~ ~ " + interactionNbt(d, true));
			if (!d.label.isBlank()) {
				float h = Math.max(0.05F, parseFloatRange(d.height, 0.05F, 32.0F));
				String lblY = String.format(Locale.ROOT, "~%.2f", h + 0.25F);
				Component lblComp = SectionText.toComponent(PropPack.ampersandCodes(d.label), Style.EMPTY);
				cmds.add(posPrefix + "summon minecraft:text_display ~ " + lblY + " ~ {billboard:\"center\",alignment:\"center\",brightness:{block:15,sky:15},text:"
					+ ShopCompat.nameNbt(lblComp) + ",Tags:[\"bjdisp.all\",\"bjdisp.c." + d.id + "\"]}");
			}
		} else {
			cmds.add(posPrefix + "summon " + displayEntityType(d.kind) + " ~ ~ ~ " + displayNbt(d, true));
			if (d.bindInteract) {
				float tx = parseFloatRange(d.tx, -64.0F, 64.0F);
				float ty = parseFloatRange(d.ty, -64.0F, 64.0F);
				float tz = parseFloatRange(d.tz, -64.0F, 64.0F);
				String off = String.format(Locale.ROOT, "~%.3f ~%.3f ~%.3f", tx, ty, tz);
				cmds.add(posPrefix + "summon minecraft:interaction " + off + " " + interactionNbt(d, true));
			}
		}
		return cmds;
	}

	public static List<String> outlineNearbyCommands() {
		return List.of(
			"execute as @e[type=minecraft:interaction,distance=..16] at @s run particle minecraft:end_rod ~ ~0.05 ~ 0.35 0.02 0.35 0 12 force",
			"execute as @e[type=minecraft:interaction,distance=..16] at @s run particle minecraft:electric_spark ~ ~0.8 ~ 0.35 0.5 0.35 0 18 force"
		);
	}

	public static Entity findNearestTarget(int preferredKind) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null) return null;
		Vec3 eye = mc.player.getEyePosition();
		Vec3 look = mc.player.getViewVector(1.0F);
		AABB box = mc.player.getBoundingBox().inflate(10.0);
		List<Entity> candidates = mc.level.getEntities(mc.player, box, e -> e instanceof Interaction || e instanceof Display);
		Entity bestAim = null;
		double bestAimDist = Double.MAX_VALUE;
		Entity bestNear = null;
		double bestNearDist = Double.MAX_VALUE;
		for (Entity e : candidates) {
			if (preferredKind >= 0 && entityKind(e) != preferredKind) continue;
			double dist = e.position().distanceTo(eye);
			if (dist < bestNearDist) {
				bestNearDist = dist;
				bestNear = e;
			}
			Vec3 to = e.getBoundingBox().getCenter().subtract(eye);
			double len = to.length();
			if (len > 0.001 && to.normalize().dot(look) > 0.92 && len < bestAimDist) {
				bestAimDist = len;
				bestAim = e;
			}
		}
		return bestAim != null ? bestAim : bestNear;
	}

	public static int entityKind(Entity e) {
		if (e instanceof Display.TextDisplay) return DisplayData.KIND_TEXT;
		if (e instanceof Display.ItemDisplay) return DisplayData.KIND_ITEM;
		if (e instanceof Display.BlockDisplay) return DisplayData.KIND_BLOCK;
		return DisplayData.KIND_INTERACT;
	}

	public static boolean readFromEntity(Entity e, DisplayData out) {
		Minecraft mc = Minecraft.getInstance();
		if (e == null || mc.level == null) return false;
		CompoundTag nbt = e.saveWithoutId(new CompoundTag());
		if (e instanceof Interaction) {
			out.kind = DisplayData.KIND_INTERACT;
			if (nbt.contains("width")) out.width = trimFloat(nbt.getFloat("width"));
			if (nbt.contains("height")) out.height = trimFloat(nbt.getFloat("height"));
			out.response = nbt.getBoolean("response");
			return true;
		}
		if (!(e instanceof Display)) return false;
		if (nbt.contains("transformation")) {
			Transformation tr = Transformation.EXTENDED_CODEC.parse(NbtOps.INSTANCE, nbt.get("transformation")).result().orElse(Transformation.identity());
			Vector3f t = tr.getTranslation();
			Vector3f s = tr.getScale();
			Vector3f euler = new Vector3f();
			tr.getLeftRotation().getEulerAnglesYXZ(euler);
			out.tx = trimFloat(t.x);
			out.ty = trimFloat(t.y);
			out.tz = trimFloat(t.z);
			out.sx = trimFloat(s.x);
			out.sy = trimFloat(s.y);
			out.sz = trimFloat(s.z);
			out.yaw = trimFloat((float) Math.toDegrees(euler.y));
			out.pitch = trimFloat((float) Math.toDegrees(euler.x));
			out.roll = trimFloat((float) Math.toDegrees(euler.z));
		}
		String bb = nbt.getString("billboard");
		for (int i = 0; i < DisplayData.BILLBOARDS.length; i++) {
			if (DisplayData.BILLBOARDS[i].equals(bb)) out.billboard = i;
		}
		out.fullBright = nbt.contains("brightness", 10);
		out.glowing = nbt.getBoolean("Glowing");
		if (e instanceof Display.TextDisplay) {
			out.kind = DisplayData.KIND_TEXT;
			if (nbt.contains("text", 8)) {
				Component parsed = Component.Serializer.fromJson(nbt.getString("text"), mc.level.registryAccess());
				if (parsed != null) out.text = SectionText.fromComponent(parsed);
			}
			if (nbt.contains("line_width")) out.lineWidth = String.valueOf(nbt.getInt("line_width"));
			out.textShadow = nbt.getBoolean("shadow");
			out.seeThrough = nbt.getBoolean("see_through");
			boolean defBg = nbt.getBoolean("default_background");
			int bg = nbt.contains("background") ? nbt.getInt("background") : 0x40000000;
			if (defBg) {
				out.bgMode = DisplayData.BG_DEFAULT;
			} else if (bg == 0) {
				out.bgMode = DisplayData.BG_CLEAR;
			} else {
				out.bgMode = DisplayData.BG_CUSTOM;
				out.bgHex = String.format(Locale.ROOT, "%08X", bg);
			}
			String al = nbt.getString("alignment");
			for (int i = 0; i < DisplayData.ALIGNS.length; i++) {
				if (DisplayData.ALIGNS[i].equals(al)) out.align = i;
			}
			return true;
		}
		if (e instanceof Display.ItemDisplay) {
			out.kind = DisplayData.KIND_ITEM;
			if (nbt.contains("item", 10)) {
				String snbt = GiveCommands.snbt(nbt.getCompound("item"));
				ItemStack st = ShopCompat.stackFromSnbt(snbt);
				if (!st.isEmpty()) {
					out.item = BuiltInRegistries.ITEM.getKey(st.getItem()).toString();
					out.itemSnbt = snbt;
				}
			}
			String ctx = nbt.getString("item_display");
			for (int i = 0; i < DisplayData.ITEM_CONTEXTS.length; i++) {
				if (DisplayData.ITEM_CONTEXTS[i].equals(ctx)) out.itemContext = i;
			}
			return true;
		}
		if (e instanceof Display.BlockDisplay) {
			out.kind = DisplayData.KIND_BLOCK;
			if (nbt.contains("block_state", 10)) {
				CompoundTag bs = nbt.getCompound("block_state");
				StringBuilder sb = new StringBuilder(bs.getString("Name"));
				if (bs.contains("Properties", 10)) {
					CompoundTag props = bs.getCompound("Properties");
					if (!props.getAllKeys().isEmpty()) {
						sb.append('[');
						boolean first = true;
						for (String k : props.getAllKeys()) {
							if (!first) sb.append(',');
							first = false;
							sb.append(k).append('=').append(props.getString(k));
						}
						sb.append(']');
					}
				}
				out.block = sb.toString();
			}
			return true;
		}
		return false;
	}

	private static String trimFloat(float v) {
		if (Math.abs(v) < 0.0005F) return "0";
		return new BigDecimal(String.format(Locale.ROOT, "%.3f", v)).stripTrailingZeros().toPlainString();
	}

	public static Map<String, String> globalFiles(Collection<String> ids) {
		Map<String, String> out = new LinkedHashMap<>();
		StringBuilder eggs = new StringBuilder();
		StringBuilder lefts = new StringBuilder();
		StringBuilder rights = new StringBuilder();
		for (String id : new TreeSet<>(ids)) {
			if (eggs.length() > 0) {
				eggs.append(',');
				lefts.append(',');
				rights.append(',');
			}
			eggs.append("{\"id\":\"bjdisp:").append(id).append("/z/egg\",\"required\":false}");
			lefts.append("{\"id\":\"bjdisp:").append(id).append("/z/left_chk\",\"required\":false}");
			rights.append("{\"id\":\"bjdisp:").append(id).append("/z/right_chk\",\"required\":false}");
		}
		out.put(PACK + "/pack.mcmeta", META);
		out.put(DATA + "minecraft/tags/function/load.json", "{\"values\":[\"bjdisp:z/load\"]}\n");
		out.put(DATA + "minecraft/tags/function/tick.json", "{\"values\":[\"bjdisp:z/tick\"]}\n");
		out.put(DATA + PACK + "/tags/function/egg.json", "{\"values\":[" + eggs + "]}\n");
		out.put(DATA + PACK + "/tags/function/left.json", "{\"values\":[" + lefts + "]}\n");
		out.put(DATA + PACK + "/tags/function/right.json", "{\"values\":[" + rights + "]}\n");
		out.put(DATA + PACK + "/advancement/z/atk.json", ADV_ATK);
		out.put(DATA + PACK + "/advancement/z/use.json", ADV_USE);

		out.put(FN + "z/load.mcfunction", "scoreboard objectives add bjdisp.cd dummy\n");
		out.put(FN + "z/egg.mcfunction", "function #bjdisp:egg\nkill @s\n");
		out.put(FN + "z/tick.mcfunction",
			"execute as @e[type=minecraft:marker,tag=bjdisp.egg] at @s run function bjdisp:z/egg\n"
			+ "execute as @e[type=minecraft:interaction,tag=bjdisp.i,scores={bjdisp.cd=1..}] run scoreboard players remove @s bjdisp.cd 1\n");
		out.put(FN + "z/on_atk.mcfunction",
			"advancement revoke @s only bjdisp:z/atk\n"
			+ "tag @s add bjdisp.actor\n"
			+ "execute as @e[type=minecraft:interaction,tag=bjdisp.i,distance=..8] if data entity @s attack at @s run function bjdisp:z/do_left\n"
			+ "tag @s remove bjdisp.actor\n");
		out.put(FN + "z/do_left.mcfunction",
			"data remove entity @s attack\n"
			+ "execute if score @s bjdisp.cd matches 1.. run return 0\n"
			+ "function #bjdisp:left\n");
		out.put(FN + "z/on_use.mcfunction",
			"advancement revoke @s only bjdisp:z/use\n"
			+ "tag @s add bjdisp.actor\n"
			+ "execute as @e[type=minecraft:interaction,tag=bjdisp.i,distance=..8] if data entity @s interaction at @s run function bjdisp:z/do_right\n"
			+ "tag @s remove bjdisp.actor\n");
		out.put(FN + "z/do_right.mcfunction",
			"data remove entity @s interaction\n"
			+ "execute if score @s bjdisp.cd matches 1.. run return 0\n"
			+ "function #bjdisp:right\n");
		return out;
	}

	public static Map<String, String> modelFiles(DisplayData d) {
		Map<String, String> out = new LinkedHashMap<>();
		String dir = FN + d.id + "/";
		String p = "bjdisp:" + d.id + "/z/";
		StringBuilder sum = new StringBuilder();
		for (String c : directSummonCommands(d)) sum.append(c).append('\n');
		out.put(dir + "summon.mcfunction", sum.toString());
		out.put(dir + "remove.mcfunction", "kill @e[tag=bjdisp.c." + d.id + "]\n");
		out.put(dir + "z/egg.mcfunction", "execute if entity @s[tag=bjdisp.e." + d.id + "] run function bjdisp:" + d.id + "/summon\n");

		String lCmd = d.hasInteraction() ? cleanOptionalCommand(d.leftCmd) : "";
		String rCmd = d.hasInteraction() ? cleanOptionalCommand(d.rightCmd) : "";
		int cd = d.hasInteraction() ? Math.max(0, intRange(d.cooldown, 0, 72000)) : 0;

		if (lCmd != null && !lCmd.isEmpty()) {
			out.put(dir + "z/left.mcfunction", lCmd + "\n");
			out.put(dir + "z/left_chk.mcfunction",
				(cd > 0 ? "execute if entity @s[tag=bjdisp.c." + d.id + "] run scoreboard players set @s bjdisp.cd " + cd + "\n" : "")
				+ "execute if entity @s[tag=bjdisp.c." + d.id + "] as @p[tag=bjdisp.actor,limit=1] run function " + p + "left\n");
		} else {
			out.put(dir + "z/left_chk.mcfunction", "");
		}
		if (rCmd != null && !rCmd.isEmpty()) {
			out.put(dir + "z/right.mcfunction", rCmd + "\n");
			out.put(dir + "z/right_chk.mcfunction",
				(cd > 0 ? "execute if entity @s[tag=bjdisp.c." + d.id + "] run scoreboard players set @s bjdisp.cd " + cd + "\n" : "")
				+ "execute if entity @s[tag=bjdisp.c." + d.id + "] as @p[tag=bjdisp.actor,limit=1] run function " + p + "right\n");
		} else {
			out.put(dir + "z/right_chk.mcfunction", "");
		}
		return out;
	}
}
