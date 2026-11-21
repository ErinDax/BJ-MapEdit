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
	public static final String[] CARRIERS = {"", "minecraft:pig", "minecraft:husk"};
	private static final int[] DEFAULT_TICKS = {0, 0, 20, 10, 20};
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
	public final String[] anims = {"", "", "", "", ""};
	public final String[] tags = {"", "", "", "", ""};
	public final int[] ticks = DEFAULT_TICKS.clone();

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

	public static int defaultTicks(int slot) {
		return DEFAULT_TICKS[slot];
	}

	public boolean mob() {
		return mode != STATIC;
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
