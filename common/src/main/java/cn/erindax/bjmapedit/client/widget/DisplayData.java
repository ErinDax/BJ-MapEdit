package cn.erindax.bjmapedit.client.widget;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public final class DisplayData {
	public static final int KIND_INTERACT = 0;
	public static final int KIND_TEXT = 1;
	public static final int KIND_ITEM = 2;
	public static final int KIND_BLOCK = 3;
	public static final int KIND_COUNT = 4;

	public static final String[] BILLBOARDS = {"fixed", "center", "vertical", "horizontal"};
	public static final String[] ALIGNS = {"center", "left", "right"};
	public static final String[] ITEM_CONTEXTS = {"fixed", "ground", "gui", "thirdperson_righthand", "head", "none"};
	public static final int BG_DEFAULT = 0;
	public static final int BG_CLEAR = 1;
	public static final int BG_CUSTOM = 2;

	public static final String FLOAT_PAT = "-?[0-9.]{0,8}";
	public static final String UFLOAT_PAT = "[0-9.]{0,8}";
	public static final String DIGITS_5 = "\\d{0,5}";
	public static final String HEX_PAT = "#?[0-9a-fA-F]{0,8}";
	private static final String ID = "[a-z0-9_]{1,24}";

	public String id = "disp_1";
	public int kind = KIND_INTERACT;
	public boolean centerSnap = true;

	public String width = "1";
	public String height = "1";
	public boolean response = true;
	public String cooldown = "5";
	public String label = "";
	public String leftCmd = "";
	public String rightCmd = "";
	public boolean bindInteract = false;

	public String tx = "0";
	public String ty = "0";
	public String tz = "0";
	public String sx = "1";
	public String sy = "1";
	public String sz = "1";
	public String yaw = "0";
	public String pitch = "0";
	public String roll = "0";
	public int billboard = 0;
	public boolean fullBright = true;
	public boolean glowing = false;

	public String text = "欢迎来到地图\n点击查看详情";
	public String textColor = "#FFFFFF";
	public boolean bold = false;
	public boolean italic = false;
	public boolean underline = false;
	public boolean strikethrough = false;
	public boolean obfuscated = false;
	public int align = 0;
	public String lineWidth = "200";
	public boolean textShadow = true;
	public boolean seeThrough = false;
	public int bgMode = BG_DEFAULT;
	public String bgHex = "80000000";

	public String item = "minecraft:diamond_sword";
	public String itemSnbt = "";
	public int itemContext = 0;

	public String block = "minecraft:gold_block";

	public static boolean validId(String id) {
		return id != null && id.matches(ID) && !id.equals("z");
	}

	public boolean hasInteraction() {
		return kind == KIND_INTERACT || bindInteract;
	}

	public boolean needsDatapack() {
		return hasInteraction() && (!leftCmd.isBlank() || !rightCmd.isBlank());
	}

	public int lineCount() {
		if (text == null || text.isEmpty()) return 1;
		int n = 1;
		for (int i = 0; i < text.length(); i++) if (text.charAt(i) == '\n') n++;
		return n;
	}

	public String toJson() {
		JsonObject o = new JsonObject();
		o.addProperty("id", id);
		o.addProperty("kind", kind);
		o.addProperty("centerSnap", centerSnap);
		o.addProperty("width", width);
		o.addProperty("height", height);
		o.addProperty("response", response);
		o.addProperty("cooldown", cooldown);
		o.addProperty("label", label);
		o.addProperty("leftCmd", leftCmd);
		o.addProperty("rightCmd", rightCmd);
		o.addProperty("bindInteract", bindInteract);
		o.addProperty("tx", tx);
		o.addProperty("ty", ty);
		o.addProperty("tz", tz);
		o.addProperty("sx", sx);
		o.addProperty("sy", sy);
		o.addProperty("sz", sz);
		o.addProperty("yaw", yaw);
		o.addProperty("pitch", pitch);
		o.addProperty("roll", roll);
		o.addProperty("billboard", billboard);
		o.addProperty("fullBright", fullBright);
		o.addProperty("glowing", glowing);
		o.addProperty("text", text);
		o.addProperty("textColor", textColor);
		o.addProperty("bold", bold);
		o.addProperty("italic", italic);
		o.addProperty("underline", underline);
		o.addProperty("strikethrough", strikethrough);
		o.addProperty("obfuscated", obfuscated);
		o.addProperty("align", align);
		o.addProperty("lineWidth", lineWidth);
		o.addProperty("textShadow", textShadow);
		o.addProperty("seeThrough", seeThrough);
		o.addProperty("bgMode", bgMode);
		o.addProperty("bgHex", bgHex);
		o.addProperty("item", item);
		o.addProperty("itemSnbt", itemSnbt);
		o.addProperty("itemContext", itemContext);
		o.addProperty("block", block);
		return o.toString();
	}

	public static DisplayData fromJson(String json) {
		try {
			JsonObject o = JsonParser.parseString(json).getAsJsonObject();
			DisplayData d = new DisplayData();
			d.id = o.get("id").getAsString();
			if (!validId(d.id)) return null;
			d.kind = Math.max(0, Math.min(KIND_COUNT - 1, o.get("kind").getAsInt()));
			if (o.has("centerSnap")) d.centerSnap = o.get("centerSnap").getAsBoolean();
			if (o.has("width")) d.width = o.get("width").getAsString();
			if (o.has("height")) d.height = o.get("height").getAsString();
			if (o.has("response")) d.response = o.get("response").getAsBoolean();
			if (o.has("cooldown")) d.cooldown = o.get("cooldown").getAsString();
			if (o.has("label")) d.label = o.get("label").getAsString();
			if (o.has("leftCmd")) d.leftCmd = o.get("leftCmd").getAsString();
			if (o.has("rightCmd")) d.rightCmd = o.get("rightCmd").getAsString();
			if (o.has("bindInteract")) d.bindInteract = o.get("bindInteract").getAsBoolean();
			if (o.has("tx")) d.tx = o.get("tx").getAsString();
			if (o.has("ty")) d.ty = o.get("ty").getAsString();
			if (o.has("tz")) d.tz = o.get("tz").getAsString();
			if (o.has("sx")) d.sx = o.get("sx").getAsString();
			if (o.has("sy")) d.sy = o.get("sy").getAsString();
			if (o.has("sz")) d.sz = o.get("sz").getAsString();
			if (o.has("yaw")) d.yaw = o.get("yaw").getAsString();
			if (o.has("pitch")) d.pitch = o.get("pitch").getAsString();
			if (o.has("roll")) d.roll = o.get("roll").getAsString();
			if (o.has("billboard")) d.billboard = Math.max(0, Math.min(BILLBOARDS.length - 1, o.get("billboard").getAsInt()));
			if (o.has("fullBright")) d.fullBright = o.get("fullBright").getAsBoolean();
			if (o.has("glowing")) d.glowing = o.get("glowing").getAsBoolean();
			if (o.has("text")) d.text = o.get("text").getAsString();
			if (o.has("textColor")) d.textColor = o.get("textColor").getAsString();
			if (o.has("bold")) d.bold = o.get("bold").getAsBoolean();
			if (o.has("italic")) d.italic = o.get("italic").getAsBoolean();
			if (o.has("underline")) d.underline = o.get("underline").getAsBoolean();
			if (o.has("strikethrough")) d.strikethrough = o.get("strikethrough").getAsBoolean();
			if (o.has("obfuscated")) d.obfuscated = o.get("obfuscated").getAsBoolean();
			if (o.has("align")) d.align = Math.max(0, Math.min(ALIGNS.length - 1, o.get("align").getAsInt()));
			if (o.has("lineWidth")) d.lineWidth = o.get("lineWidth").getAsString();
			if (o.has("textShadow")) d.textShadow = o.get("textShadow").getAsBoolean();
			if (o.has("seeThrough")) d.seeThrough = o.get("seeThrough").getAsBoolean();
			if (o.has("bgMode")) d.bgMode = Math.max(0, Math.min(BG_CUSTOM, o.get("bgMode").getAsInt()));
			if (o.has("bgHex")) d.bgHex = o.get("bgHex").getAsString();
			if (o.has("item")) d.item = o.get("item").getAsString();
			if (o.has("itemSnbt")) d.itemSnbt = o.get("itemSnbt").getAsString();
			if (o.has("itemContext")) d.itemContext = Math.max(0, Math.min(ITEM_CONTEXTS.length - 1, o.get("itemContext").getAsInt()));
			if (o.has("block")) d.block = o.get("block").getAsString();
			return d;
		} catch (RuntimeException e) {
			return null;
		}
	}
}
