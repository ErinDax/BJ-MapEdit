package cn.erindax.bjmapedit.client.widget;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;

public final class PropData {
	public static final int MAX_SKILLS = 6;
	public static final int MAX_TICKS = 72000;
	public static final int INTERACT_RIGHT_CLICK = 0;
	public static final int INTERACT_RIGHT_HOLD = 1;
	public static final int INTERACT_RIGHT_RELEASE = 2;
	public static final int INTERACT_RIGHT_CHARGE = 3;
	public static final int INTERACT_SNEAK_RIGHT = 4;
	public static final int INTERACT_LEFT_HIT = 5;
	public static final int INTERACT_DROP = 6;
	public static final int INTERACT_COUNT = 7;

	public static final int EFFECT_RING_LASER = 0;
	public static final int EFFECT_DASH_SLASH = 1;
	public static final int EFFECT_SWORD_WAVE = 2;
	public static final int EFFECT_PROJECTILE = 3;
	public static final int EFFECT_STARFALL = 4;
	public static final int EFFECT_NOVA = 5;
	public static final int EFFECT_VORTEX = 6;
	public static final int EFFECT_HEAL_SHIELD = 7;
	public static final int EFFECT_BLINK = 8;
	public static final int EFFECT_CUSTOM = 9;
	public static final int EFFECT_COUNT = 10;

	public static final int PROJ_WIND = 0;
	public static final int PROJ_FIREBALL = 1;
	public static final int PROJ_DRAGON = 2;
	public static final int PROJ_WITHER = 3;
	public static final int PROJ_COUNT = 4;

	public static final String DIGITS_5 = "\\d{0,5}";
	public static final String DIGITS_4 = "\\d{0,4}";
	public static final String DIGITS_3 = "\\d{0,3}";
	private static final String ID = "[a-z0-9_]{1,24}";
	private static final int[] DEFAULT_RANGE = {28, 10, 18, 24, 32, 6, 12, 4, 14, 16};
	private static final int[] DEFAULT_POWER = {12, 10, 10, 1, 14, 10, 4, 8, 0, 1};
	private static final String[] DEFAULT_PARTICLE = {
		"minecraft:end_rod",
		"minecraft:squid_ink",
		"minecraft:soul_fire_flame",
		"minecraft:flame",
		"minecraft:electric_spark",
		"minecraft:soul_fire_flame",
		"minecraft:portal",
		"minecraft:totem_of_undying",
		"minecraft:reverse_portal",
		"minecraft:end_rod"
	};

	public String id = "star_sword";
	public String item = "minecraft:diamond_sword";
	public String baseSnbt = "";
	public String name = "";
	public boolean autoLore = true;
	public boolean pvp = false;
	public final List<Skill> skills = new ArrayList<>();

	public PropData() {
		skills.add(new Skill());
	}

	public static final class Skill {
		public int interact = INTERACT_RIGHT_CLICK;
		public String charge = "20";
		public String cooldown = "40";
		public boolean consume = false;
		public int effect = EFFECT_RING_LASER;
		public String range = "28";
		public String power = "12";
		public int projKind = PROJ_WIND;
		public boolean pierce = false;
		public String particle = "minecraft:end_rod";
		public String command = "";

		public void applyInteractDefaults(int next) {
			interact = Math.floorMod(next, INTERACT_COUNT);
			if (interact == INTERACT_RIGHT_HOLD && ("0".equals(cooldown) || "40".equals(cooldown))) cooldown = "4";
			else if (interact != INTERACT_RIGHT_HOLD && "4".equals(cooldown)) cooldown = "40";
		}

		public void applyEffectDefaults(int next) {
			effect = Math.floorMod(next, EFFECT_COUNT);
			range = String.valueOf(DEFAULT_RANGE[effect]);
			power = String.valueOf(DEFAULT_POWER[effect]);
			particle = DEFAULT_PARTICLE[effect];
		}
	}

	public static boolean validId(String id) {
		return id != null && id.matches(ID) && !id.equals("z");
	}

	public static boolean isRightInteract(int interact) {
		return interact >= INTERACT_RIGHT_CLICK && interact <= INTERACT_SNEAK_RIGHT;
	}

	public static boolean usesCharge(int interact) {
		return interact == INTERACT_RIGHT_RELEASE || interact == INTERACT_RIGHT_CHARGE;
	}

	public static boolean usesParticle(int effect) {
		return effect != EFFECT_CUSTOM;
	}

	public boolean needsRightClick() {
		for (Skill s : skills) if (isRightInteract(s.interact)) return true;
		return false;
	}

	public boolean needsDrop() {
		for (Skill s : skills) if (s.interact == INTERACT_DROP) return true;
		return false;
	}

	public boolean hasSneakRight() {
		for (Skill s : skills) if (s.interact == INTERACT_SNEAK_RIGHT) return true;
		return false;
	}

	public String toJson() {
		JsonObject o = new JsonObject();
		o.addProperty("id", id);
		o.addProperty("item", item);
		o.addProperty("baseSnbt", baseSnbt);
		o.addProperty("name", name);
		o.addProperty("autoLore", autoLore);
		o.addProperty("pvp", pvp);
		JsonArray arr = new JsonArray();
		for (Skill s : skills) {
			JsonObject e = new JsonObject();
			e.addProperty("interact", s.interact);
			e.addProperty("charge", s.charge);
			e.addProperty("cooldown", s.cooldown);
			e.addProperty("consume", s.consume);
			e.addProperty("effect", s.effect);
			e.addProperty("range", s.range);
			e.addProperty("power", s.power);
			e.addProperty("projKind", s.projKind);
			e.addProperty("pierce", s.pierce);
			e.addProperty("particle", s.particle);
			e.addProperty("command", s.command);
			arr.add(e);
		}
		o.add("skills", arr);
		return o.toString();
	}

	public static PropData fromJson(String json) {
		try {
			JsonObject o = JsonParser.parseString(json).getAsJsonObject();
			PropData d = new PropData();
			d.skills.clear();
			d.id = o.get("id").getAsString();
			d.item = o.get("item").getAsString();
			if (o.has("baseSnbt")) d.baseSnbt = o.get("baseSnbt").getAsString();
			if (o.has("name")) d.name = o.get("name").getAsString();
			if (o.has("autoLore")) d.autoLore = o.get("autoLore").getAsBoolean();
			if (o.has("pvp")) d.pvp = o.get("pvp").getAsBoolean();
			if (o.has("skills")) {
				for (JsonElement el : o.getAsJsonArray("skills")) {
					if (d.skills.size() >= MAX_SKILLS) break;
					JsonObject r = el.getAsJsonObject();
					Skill s = new Skill();
					s.interact = Math.max(0, Math.min(INTERACT_COUNT - 1, r.get("interact").getAsInt()));
					String ch = r.get("charge").getAsString();
					String cd = r.get("cooldown").getAsString();
					if (ch.matches(DIGITS_4)) s.charge = ch;
					if (cd.matches(DIGITS_5)) s.cooldown = cd;
					s.consume = r.get("consume").getAsBoolean();
					s.effect = Math.max(0, Math.min(EFFECT_COUNT - 1, r.get("effect").getAsInt()));
					String rng = r.get("range").getAsString();
					String pwr = r.get("power").getAsString();
					if (rng.matches(DIGITS_3)) s.range = rng;
					if (pwr.matches(DIGITS_4)) s.power = pwr;
					if (o.has("skills") && r.has("projKind")) s.projKind = Math.max(0, Math.min(PROJ_COUNT - 1, r.get("projKind").getAsInt()));
					if (r.has("pierce")) s.pierce = r.get("pierce").getAsBoolean();
					s.particle = r.get("particle").getAsString();
					s.command = r.get("command").getAsString();
					d.skills.add(s);
				}
			}
			return validId(d.id) ? d : null;
		} catch (RuntimeException e) {
			return null;
		}
	}
}
