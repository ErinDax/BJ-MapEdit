package cn.erindax.bjmapedit.client.widget;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;

public final class AjData {
	public static final int STATIC = 0;
	public static final int PASSIVE = 1;
	public static final int HOSTILE = 2;
	public static final int IDLE = 0;
	public static final int WALK = 1;
	public static final int ATTACK = 2;
	public static final int HURT = 3;
	public static final int DEATH = 4;
	public static final int SLOTS = 5;
	public static final int MAX_TICKS = 72000;
	public static final int LOOT_FOLLOW = 0;
	public static final int LOOT_NONE = 1;
	public static final int LOOT_CUSTOM = 2;
	public static final int MAX_DROPS = 16;
	public static final int MAX_SKILLS = 8;
	public static final int MAX_TRANSITIONS = 5;
	public static final int SKILL_DRAGON_BREATH = 0;
	public static final int SKILL_SONIC_BOOM = 1;
	public static final int SKILL_GUARDIAN_LASER = 2;
	public static final int SKILL_EVOKER_FANGS = 3;
	public static final int SKILL_WITHER_SKULL = 4;
	public static final int SKILL_FIREBALL = 5;
	public static final int SKILL_SHULKER_BULLET = 6;
	public static final int SKILL_SHOCKWAVE = 7;
	public static final int SKILL_LIGHTNING = 8;
	public static final int SKILL_CUSTOM = 9;
	public static final int SKILL_COUNT = 10;
	public static final int TRIG_TIMER = 0;
	public static final int TRIG_HURT = 1;
	public static final int TRIG_HIT = 2;
	public static final int TRIG_NEAR = 3;
	public static final int TRIG_HP = 4;
	public static final int TRIG_COUNT = 5;
	public static final String HEALTH = "\\d{0,4}";
	public static final String COUNT = "\\d{0,3}";
	public static final String CHANCE = "[0-9.]{0,6}";
	public static final String DIGITS_5 = "\\d{0,5}";
	public static final String DIGITS_2 = "\\d{0,2}";
	public static final String[] CARRIERS = {"", "minecraft:pig", "minecraft:husk"};
	public static final String[] BOSS_COLORS = {"red", "purple", "pink", "blue", "green", "yellow", "white"};
	public static final String[] BOSS_STYLES = {"progress", "notched_6", "notched_10", "notched_12", "notched_20"};
	public static final String[][] GUESS = {{"idle", "stand"}, {"walk", "run", "move"}, {"attack", "atk"}, {"hurt", "damage"}, {"death", "die", "dead"}};
	private static final int[] DEFAULT_TICKS = {0, 0, 20, 10, 20};
	private static final int[] DEFAULT_SKILL_RANGE = {20, 16, 18, 12, 20, 20, 20, 5, 18, 16};
	private static final int[] DEFAULT_SKILL_POWER = {1, 10, 8, 1, 1, 3, 1, 8, 10, 1};
	private static final int[] DEFAULT_SKILL_WINDUP = {10, 20, 20, 10, 10, 10, 10, 10, 20, 0};
	private static final int[] DEFAULT_TRIG_VALUE = {100, 3, 3, 80, 30};
	private static final String ID = "[a-z0-9_]{1,24}";
	private static final String RIG = "[a-z0-9_.-]+:[a-z0-9_.-]+(/[a-z0-9_.-]+)*";
	private static final String NAME = "[a-z0-9_.-]+";
	private static final String PATH = "[a-z0-9_.-]+(/[a-z0-9_.-]+)*";
	private static final String TAG = "[A-Za-z0-9_.+-]+";

	public String id = "";
	public String rig = "";
	public boolean args = true;
	public boolean pause = true;
	public int mode = HOSTILE;
	public String carrier = CARRIERS[HOSTILE];
	public String nbt = "";
	public boolean silent = true;
	public boolean loop = true;
	public String scale = "1";
	public String variant = "";
	public String health = "";
	public int loot = LOOT_FOLLOW;
	public final List<Drop> drops = new ArrayList<>();
	public final List<Skill> skills = new ArrayList<>();
	public boolean boss = false;
	public boolean bossBar = true;
	public String bossName = "";
	public int bossColor = 0;
	public int bossStyle = 0;
	public final List<Phase> phases = new ArrayList<>();
	public final String[] anims = {"", "", "", "", ""};
	public final String[] tags = {"", "", "", "", ""};
	public final int[] ticks = DEFAULT_TICKS.clone();

	public static final class Drop {
		public String item = "";
		public String min = "1";
		public String max = "1";
		public String chance = "100";
	}

	public static final class Skill {
		public int kind = SKILL_SONIC_BOOM;
		public int trigger = TRIG_TIMER;
		public String value = "100";
		public String range = "16";
		public String power = "10";
		public String windup = "20";
		public int phase = 0;
		public String anim = "";
		public int animTicks = 20;
		public String command = "";

		public void applyKindDefaults(int nextKind) {
			kind = Math.floorMod(nextKind, SKILL_COUNT);
			range = String.valueOf(DEFAULT_SKILL_RANGE[kind]);
			power = String.valueOf(DEFAULT_SKILL_POWER[kind]);
			windup = String.valueOf(DEFAULT_SKILL_WINDUP[kind]);
		}

		public void applyTriggerDefaults(int nextTrigger) {
			trigger = Math.floorMod(nextTrigger, TRIG_COUNT);
			value = String.valueOf(DEFAULT_TRIG_VALUE[trigger]);
		}
	}

	public static final class Phase {
		public String hp = "50";
		public String transAnim = "";
		public String transTag = "";
		public int transTicks = 40;
		public boolean transOnNew = false;
		public boolean changeRig = false;
		public String rig = "";
		public boolean args = true;
		public boolean pause = true;
		public String variant = "";
		public final String[] anims = {"", "", "", "", ""};
		public final String[] tags = {"", "", "", "", ""};
		public final int[] ticks = DEFAULT_TICKS.clone();

		public void setAnim(int slot, Anim a) {
			anims[slot] = a == null ? "" : a.name();
			tags[slot] = a == null ? "" : a.tag();
			if (slot >= ATTACK) ticks[slot] = a != null && a.length() > 0 ? Math.min(MAX_TICKS, a.length()) : DEFAULT_TICKS[slot];
		}

		public void useRig(Rig r, boolean guess) {
			rig = r.id();
			args = r.args();
			pause = r.pause();
			if (guess) {
				for (int i = 0; i < SLOTS; i++) {
					anims[i] = "";
					tags[i] = "";
					ticks[i] = DEFAULT_TICKS[i];
					for (String word : GUESS[i]) {
						for (Anim a : r.anims()) {
							if (anims[i].isEmpty() && a.name().contains(word)) setAnim(i, a);
						}
					}
				}
				variant = "";
				return;
			}
			for (int i = 0; i < SLOTS; i++) {
				Anim a = r.anim(anims[i]);
				if (a == null) anims[i] = "";
				tags[i] = a == null ? "" : a.tag();
			}
			boolean keep = false;
			for (Variant v : r.variants()) keep |= v.function().equals(variant);
			if (!keep) variant = "";
		}
	}

	public record Anim(String name, int length, String tag) {}

	public record Variant(String name, String function) {}

	public record Rig(String id, String pack, boolean args, boolean pause, List<Anim> anims, List<Variant> variants) {
		public Anim anim(String name) {
			for (Anim a : anims) if (a.name().equals(name)) return a;
			return null;
		}
	}

	public record Scan(List<Rig> rigs, List<String> made, boolean more) {
		public Rig rig(String id) {
			for (Rig r : rigs) if (r.id().equals(id)) return r;
			return null;
		}
	}

	public static boolean validId(String id) {
		return id != null && id.matches(ID) && !id.equals("z");
	}

	public static boolean validRig(String rig) {
		return rig != null && rig.matches(RIG);
	}

	public static boolean validAnimName(String name) {
		return name != null && name.matches(NAME);
	}

	public static int defaultTicks(int slot) {
		return DEFAULT_TICKS[slot];
	}

	public static boolean usesDamage(int kind) {
		return kind == SKILL_SONIC_BOOM || kind == SKILL_GUARDIAN_LASER || kind == SKILL_SHOCKWAVE || kind == SKILL_LIGHTNING;
	}

	public static boolean usesBurst(int kind) {
		return kind != SKILL_CUSTOM && !usesDamage(kind);
	}

	public static Rig findRig(List<Rig> rigs, String id) {
		if (rigs == null || id == null) return null;
		for (Rig r : rigs) if (r.id().equals(id)) return r;
		return null;
	}

	public boolean mob() {
		return mode != STATIC;
	}

	public int phaseCount() {
		return boss ? phases.size() + 1 : 1;
	}

	public String rigForPhase(int phaseIndex) {
		String cur = rig;
		for (int i = 0; i < phaseIndex && i < phases.size(); i++) {
			Phase ph = phases.get(i);
			if (ph.changeRig && validRig(ph.rig)) cur = ph.rig;
		}
		return cur;
	}

	public void use(Rig r) {
		rig = r.id();
		args = r.args();
		pause = r.pause();
		for (int i = 0; i < SLOTS; i++) {
			Anim a = r.anim(anims[i]);
			if (a == null) anims[i] = "";
			tags[i] = a == null ? "" : a.tag();
		}
		boolean keep = false;
		for (Variant v : r.variants()) keep |= v.function().equals(variant);
		if (!keep) variant = "";
	}

	public void syncRigs(List<Rig> rigs) {
		Rig r0 = findRig(rigs, rig);
		if (r0 != null) use(r0);
		Rig prev = r0;
		for (Phase ph : phases) {
			Rig pr = ph.changeRig ? findRig(rigs, ph.rig) : null;
			if (pr != null) ph.useRig(pr, false);
			if (!ph.changeRig) ph.transOnNew = false;
			Rig tr = ph.changeRig && ph.transOnNew ? pr : prev;
			if (tr != null) {
				Anim ta = tr.anim(ph.transAnim);
				if (ta == null) ph.transAnim = "";
				ph.transTag = ta == null ? "" : ta.tag();
			}
			if (ph.changeRig) prev = pr;
		}
		int maxPhase = boss ? phases.size() + 1 : 0;
		for (Skill s : skills) {
			if (s.phase > maxPhase) s.phase = 0;
			if (s.anim.isEmpty() || rigs == null || rigs.isEmpty()) continue;
			if (s.phase >= 1) {
				Rig sr = findRig(rigs, rigForPhase(s.phase - 1));
				if (sr != null && sr.anim(s.anim) == null) s.anim = "";
			} else {
				boolean any = r0 != null && r0.anim(s.anim) != null;
				for (int i = 0; boss && i < phases.size() && !any; i++) {
					Rig sr = findRig(rigs, rigForPhase(i + 1));
					if (sr != null && sr.anim(s.anim) != null) any = true;
				}
				if (!any && r0 != null) s.anim = "";
			}
		}
	}

	public void setAnim(int slot, Anim a) {
		anims[slot] = a == null ? "" : a.name();
		tags[slot] = a == null ? "" : a.tag();
		if (slot >= ATTACK) ticks[slot] = a != null && a.length() > 0 ? Math.min(MAX_TICKS, a.length()) : DEFAULT_TICKS[slot];
	}

	public String toJson() {
		JsonObject o = new JsonObject();
		o.addProperty("id", id);
		o.addProperty("rig", rig);
		o.addProperty("args", args);
		o.addProperty("pause", pause);
		o.addProperty("mode", mode);
		o.addProperty("carrier", carrier);
		o.addProperty("nbt", nbt);
		o.addProperty("silent", silent);
		o.addProperty("loop", loop);
		o.addProperty("scale", scale);
		o.addProperty("variant", variant);
		o.addProperty("health", health);
		o.addProperty("loot", loot);
		JsonArray d = new JsonArray();
		for (Drop drop : drops) {
			JsonObject e = new JsonObject();
			e.addProperty("item", drop.item);
			e.addProperty("min", drop.min);
			e.addProperty("max", drop.max);
			e.addProperty("chance", drop.chance);
			d.add(e);
		}
		o.add("drops", d);
		JsonArray a = new JsonArray();
		JsonArray g = new JsonArray();
		JsonArray t = new JsonArray();
		for (int i = 0; i < SLOTS; i++) {
			a.add(anims[i]);
			g.add(tags[i]);
			t.add(ticks[i]);
		}
		o.add("anims", a);
		o.add("tags", g);
		o.add("ticks", t);
		JsonArray sk = new JsonArray();
		for (Skill s : skills) {
			JsonObject e = new JsonObject();
			e.addProperty("kind", s.kind);
			e.addProperty("trigger", s.trigger);
			e.addProperty("value", s.value);
			e.addProperty("range", s.range);
			e.addProperty("power", s.power);
			e.addProperty("windup", s.windup);
			e.addProperty("phase", s.phase);
			e.addProperty("anim", s.anim);
			e.addProperty("animTicks", s.animTicks);
			e.addProperty("command", s.command);
			sk.add(e);
		}
		o.add("skills", sk);
		o.addProperty("boss", boss);
		o.addProperty("bossBar", bossBar);
		o.addProperty("bossName", bossName);
		o.addProperty("bossColor", bossColor);
		o.addProperty("bossStyle", bossStyle);
		JsonArray phArr = new JsonArray();
		for (Phase ph : phases) {
			JsonObject e = new JsonObject();
			e.addProperty("hp", ph.hp);
			e.addProperty("transAnim", ph.transAnim);
			e.addProperty("transTag", ph.transTag);
			e.addProperty("transTicks", ph.transTicks);
			e.addProperty("transOnNew", ph.transOnNew);
			e.addProperty("changeRig", ph.changeRig);
			e.addProperty("rig", ph.rig);
			e.addProperty("args", ph.args);
			e.addProperty("pause", ph.pause);
			e.addProperty("variant", ph.variant);
			JsonArray pa = new JsonArray();
			JsonArray pg = new JsonArray();
			JsonArray pt = new JsonArray();
			for (int i = 0; i < SLOTS; i++) {
				pa.add(ph.anims[i]);
				pg.add(ph.tags[i]);
				pt.add(ph.ticks[i]);
			}
			e.add("anims", pa);
			e.add("tags", pg);
			e.add("ticks", pt);
			phArr.add(e);
		}
		o.add("phases", phArr);
		return o.toString();
	}

	public static AjData fromJson(String json) {
		try {
			JsonObject o = JsonParser.parseString(json).getAsJsonObject();
			AjData d = new AjData();
			d.id = o.get("id").getAsString();
			d.rig = o.get("rig").getAsString();
			d.args = o.get("args").getAsBoolean();
			d.pause = o.get("pause").getAsBoolean();
			d.mode = Math.max(STATIC, Math.min(HOSTILE, o.get("mode").getAsInt()));
			d.carrier = o.get("carrier").getAsString();
			d.nbt = o.get("nbt").getAsString();
			d.silent = o.get("silent").getAsBoolean();
			d.loop = o.get("loop").getAsBoolean();
			d.scale = o.get("scale").getAsString();
			d.variant = o.get("variant").getAsString();
			if (o.has("health")) d.health = o.get("health").getAsString();
			if (!d.health.matches(HEALTH)) d.health = "";
			if (o.has("loot")) d.loot = Math.max(LOOT_FOLLOW, Math.min(LOOT_CUSTOM, o.get("loot").getAsInt()));
			if (o.has("drops")) {
				for (JsonElement e : o.getAsJsonArray("drops")) {
					if (d.drops.size() >= MAX_DROPS) break;
					JsonObject r = e.getAsJsonObject();
					Drop drop = new Drop();
					drop.item = r.get("item").getAsString();
					String min = r.get("min").getAsString();
					String max = r.get("max").getAsString();
					String chance = r.get("chance").getAsString();
					if (min.matches(COUNT)) drop.min = min;
					if (max.matches(COUNT)) drop.max = max;
					if (chance.matches(CHANCE)) drop.chance = chance;
					d.drops.add(drop);
				}
			}
			JsonArray a = o.getAsJsonArray("anims");
			JsonArray g = o.getAsJsonArray("tags");
			JsonArray t = o.getAsJsonArray("ticks");
			for (int i = 0; i < SLOTS; i++) {
				String name = a.get(i).getAsString();
				String tag = g.get(i).getAsString();
				d.anims[i] = name.matches(NAME) ? name : "";
				d.tags[i] = !d.anims[i].isEmpty() && tag.matches(TAG) ? tag : "";
				d.ticks[i] = Math.max(0, Math.min(MAX_TICKS, t.get(i).getAsInt()));
			}
			if (o.has("skills")) {
				for (JsonElement e : o.getAsJsonArray("skills")) {
					if (d.skills.size() >= MAX_SKILLS) break;
					JsonObject r = e.getAsJsonObject();
					Skill s = new Skill();
					s.kind = Math.max(0, Math.min(SKILL_COUNT - 1, r.get("kind").getAsInt()));
					s.trigger = Math.max(0, Math.min(TRIG_COUNT - 1, r.get("trigger").getAsInt()));
					String val = r.get("value").getAsString();
					String rng = r.get("range").getAsString();
					String pwr = r.get("power").getAsString();
					String wup = r.get("windup").getAsString();
					if (val.matches(DIGITS_5)) s.value = val;
					if (rng.matches(COUNT)) s.range = rng;
					if (pwr.matches(HEALTH)) s.power = pwr;
					if (wup.matches(HEALTH)) s.windup = wup;
					s.phase = Math.max(0, Math.min(MAX_TRANSITIONS + 1, r.get("phase").getAsInt()));
					String anim = r.get("anim").getAsString();
					s.anim = anim.matches(NAME) ? anim : "";
					s.animTicks = Math.max(1, Math.min(MAX_TICKS, r.get("animTicks").getAsInt()));
					s.command = r.get("command").getAsString();
					d.skills.add(s);
				}
			}
			if (o.has("boss")) d.boss = o.get("boss").getAsBoolean();
			if (o.has("bossBar")) d.bossBar = o.get("bossBar").getAsBoolean();
			if (o.has("bossName")) d.bossName = o.get("bossName").getAsString();
			if (o.has("bossColor")) d.bossColor = Math.max(0, Math.min(BOSS_COLORS.length - 1, o.get("bossColor").getAsInt()));
			if (o.has("bossStyle")) d.bossStyle = Math.max(0, Math.min(BOSS_STYLES.length - 1, o.get("bossStyle").getAsInt()));
			if (o.has("phases")) {
				for (JsonElement e : o.getAsJsonArray("phases")) {
					if (d.phases.size() >= MAX_TRANSITIONS) break;
					JsonObject r = e.getAsJsonObject();
					Phase ph = new Phase();
					String hp = r.get("hp").getAsString();
					if (hp.matches(DIGITS_2)) ph.hp = hp;
					String ta = r.get("transAnim").getAsString();
					String tt = r.get("transTag").getAsString();
					ph.transAnim = ta.matches(NAME) ? ta : "";
					ph.transTag = !ph.transAnim.isEmpty() && tt.matches(TAG) ? tt : "";
					ph.transTicks = Math.max(1, Math.min(MAX_TICKS, r.get("transTicks").getAsInt()));
					ph.transOnNew = r.get("transOnNew").getAsBoolean();
					ph.changeRig = r.get("changeRig").getAsBoolean();
					String pr = r.get("rig").getAsString();
					ph.rig = validRig(pr) ? pr : "";
					ph.args = r.get("args").getAsBoolean();
					ph.pause = r.get("pause").getAsBoolean();
					String pv = r.get("variant").getAsString();
					ph.variant = pv.matches(PATH) ? pv : "";
					JsonArray pa = r.getAsJsonArray("anims");
					JsonArray pg = r.getAsJsonArray("tags");
					JsonArray pt = r.getAsJsonArray("ticks");
					for (int i = 0; i < SLOTS; i++) {
						String name = pa.get(i).getAsString();
						String tag = pg.get(i).getAsString();
						ph.anims[i] = name.matches(NAME) ? name : "";
						ph.tags[i] = !ph.anims[i].isEmpty() && tag.matches(TAG) ? tag : "";
						ph.ticks[i] = Math.max(0, Math.min(MAX_TICKS, pt.get(i).getAsInt()));
					}
					d.phases.add(ph);
				}
			}
			if (!validId(d.id) || !validRig(d.rig)) return null;
			if (!d.variant.isEmpty() && !d.variant.matches(PATH)) d.variant = "";
			return d;
		} catch (RuntimeException e) {
			return null;
		}
	}

	public static Scan parseScan(String json) {
		List<Rig> rigs = new ArrayList<>();
		List<String> made = new ArrayList<>();
		boolean more = false;
		try {
			JsonObject o = JsonParser.parseString(json).getAsJsonObject();
			more = o.get("more").getAsBoolean();
			for (JsonElement e : o.getAsJsonArray("made")) {
				String id = e.getAsString();
				if (validId(id)) made.add(id);
			}
			for (JsonElement e : o.getAsJsonArray("rigs")) {
				JsonObject r = e.getAsJsonObject();
				String id = r.get("id").getAsString();
				if (!validRig(id)) continue;
				List<Anim> anims = new ArrayList<>();
				for (JsonElement ae : r.getAsJsonArray("anims")) {
					JsonObject a = ae.getAsJsonObject();
					String name = a.get("n").getAsString();
					String tag = a.get("t").getAsString();
					if (name.matches(NAME)) anims.add(new Anim(name, a.get("l").getAsInt(), tag.matches(TAG) ? tag : ""));
				}
				List<Variant> variants = new ArrayList<>();
				for (JsonElement ve : r.getAsJsonArray("vars")) {
					JsonObject v = ve.getAsJsonObject();
					String name = v.get("n").getAsString();
					String function = v.get("f").getAsString();
					if (name.matches(NAME) && function.matches(PATH)) variants.add(new Variant(name, function));
				}
				rigs.add(new Rig(id, r.get("pack").getAsString(), r.get("args").getAsBoolean(), r.get("pause").getAsBoolean(), anims, variants));
			}
		} catch (RuntimeException e) {
			return new Scan(rigs, made, more);
		}
		return new Scan(rigs, made, more);
	}
}
