package cn.erindax.bjmapedit.client.widget;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public final class ShopData {
	public static final int SLOTS = 27;
	public static final int BIG_SLOTS = 54;
	public static final int MAX_PAGES = 12;
	public static final int MAX_OPTIONS = 8;
	public static final int MAX_GROUP = 9;
	public static final int EMPTY = 0;
	public static final int DECOR = 1;
	public static final int SELL = 2;
	public static final int BUY = 3;
	public static final int NAV = 4;
	public static final int ACTION = 5;
	public static final int CYCLE = 6;
	public static final int SELECT = 7;
	public static final int RESET = 8;
	public static final int NAV_PREV = -1;
	public static final int NAV_NEXT = -2;
	public static final int NAV_NONE = -3;
	public static final int PRICE_ITEM = 0;
	public static final int PRICE_XP = 1;
	public static final int PRICE_SCORE = 2;
	public static final int MAX_AMOUNT = 999999;
	public static final String[] CONTAINERS = {"chest", "large_chest", "barrel", "trapped_chest", "large_trapped_chest", "shulker_box"};
	private static final String LARGE = "large_";

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

	public static final class Price {
		public int type;
		public ItemStack item = ItemStack.EMPTY;
		public String objective = "";
		public int amount = 1;
	}

	public static final class Option {
		public ItemStack item = ItemStack.EMPTY;
		public int count = 1;
		public String label = "";
		public String command = "";
	}

	public static final class Slot {
		public int type = EMPTY;
		public ItemStack item = ItemStack.EMPTY;
		public int count = 1;
		public final List<Price> prices = new ArrayList<>();
		public final List<Option> options = new ArrayList<>();
		public String command = "";
		public String offCommand = "";
		public int target = NAV_NEXT;
		public int group = 1;
		public boolean defaultOn = false;

		public boolean trading() {
			return type == SELL || type == BUY;
		}

		public boolean shown() {
			return type != EMPTY && !item.isEmpty();
		}

		public boolean nav() {
			return type == NAV && !item.isEmpty();
		}

		public boolean clickable() {
			return shown() && type != DECOR;
		}

		public void ensureCycleSeeded() {
			if (!options.isEmpty() || item.isEmpty()) return;
			Option o = new Option();
			o.item = item.copyWithCount(1);
			o.count = count;
			o.label = item.getHoverName().getString();
			o.command = command;
			options.add(o);
		}
	}

	public String id = "";
	public String title = "";
	public String container = CONTAINERS[0];
	public String okText = "";
	public String failText = "";
	public boolean sound = true;
	public boolean strict = true;
	public final List<Slot[]> pages = new ArrayList<>();
	public Slot[] slots;
	public int page;

	public ShopData() {
		pages.add(blankPage());
		slots = pages.get(0);
	}

	private static Slot[] blankPage() {
		Slot[] page = new Slot[BIG_SLOTS];
		for (int i = 0; i < BIG_SLOTS; i++) page[i] = new Slot();
		return page;
	}

	public boolean large() {
		return container.startsWith(LARGE);
	}

	public int size() {
		return large() ? BIG_SLOTS : SLOTS;
	}

	public String block() {
		return large() ? container.substring(LARGE.length()) : container;
	}

	public void setPage(int index) {
		page = Math.max(0, Math.min(pages.size() - 1, index));
		slots = pages.get(page);
	}

	public boolean addPage() {
		if (pages.size() >= MAX_PAGES) return false;
		int at = page + 1;
		for (Slot[] p : pages) {
			for (Slot s : p) {
				if (s.target >= at) s.target++;
			}
		}
		pages.add(at, blankPage());
		setPage(at);
		return true;
	}

	public boolean removePage() {
		if (pages.size() <= 1) return false;
		int gone = page;
		pages.remove(gone);
		for (Slot[] p : pages) {
			for (Slot s : p) {
				if (s.target == gone) s.target = NAV_NONE;
				else if (s.target > gone) s.target--;
			}
		}
		setPage(gone);
		return true;
	}

	public boolean pageEmpty(int index) {
		Slot[] p = pages.get(index);
		int size = size();
		for (int i = 0; i < size; i++) {
			if (p[i].type != EMPTY || !p[i].item.isEmpty()) return false;
		}
		return true;
	}

	public boolean allEmpty() {
		for (int i = 0; i < pages.size(); i++) {
			if (!pageEmpty(i)) return false;
		}
		return title.isEmpty() && id.isEmpty();
	}

	public int target(int pageIndex, Slot slot) {
		if (slot.target == NAV_PREV) return Math.max(0, pageIndex - 1);
		if (slot.target == NAV_NEXT) return Math.min(pages.size() - 1, pageIndex + 1);
		return slot.target;
	}

	public int unreachablePage() {
		boolean[] seen = new boolean[pages.size()];
		Deque<Integer> queue = new ArrayDeque<>();
		int size = size();
		seen[0] = true;
		queue.add(0);
		while (!queue.isEmpty()) {
			int p = queue.poll();
			for (int i = 0; i < size; i++) {
				Slot s = pages.get(p)[i];
				if (!s.nav()) continue;
				int t = target(p, s);
				if (t < 0 || t >= seen.length || seen[t]) continue;
				seen[t] = true;
				queue.add(t);
			}
		}
		for (int i = 0; i < seen.length; i++) {
			if (!seen[i]) return i;
		}
		return -1;
	}

	public static boolean validId(String id) {
		return id != null && id.matches("[a-z0-9_]{1,24}");
	}

	public static boolean validObjective(String name) {
		return name != null && name.matches("[A-Za-z0-9_.+-]{1,32}");
	}

	public String toJson() {
		JsonObject root = new JsonObject();
		root.addProperty("id", id);
		root.addProperty("title", title);
		root.addProperty("container", container);
		root.addProperty("ok", okText);
		root.addProperty("fail", failText);
		root.addProperty("sound", sound);
		root.addProperty("strict", strict);
		JsonArray all = new JsonArray();
		int size = size();
		for (Slot[] p : pages) {
			JsonArray arr = new JsonArray();
			for (int i = 0; i < size; i++) {
				Slot slot = p[i];
				if (slot.type == EMPTY && slot.item.isEmpty()) continue;
				JsonObject o = new JsonObject();
				o.addProperty("slot", i);
				o.addProperty("type", slot.type);
				o.addProperty("item", ShopCompat.stackSnbt(slot.item));
				o.addProperty("count", slot.count);
				o.addProperty("command", slot.command);
				if (!slot.offCommand.isEmpty()) o.addProperty("offCommand", slot.offCommand);
				if (slot.type == NAV) o.addProperty("target", slot.target);
				if (slot.type == SELECT) {
					o.addProperty("group", slot.group);
					o.addProperty("defaultOn", slot.defaultOn);
				}
				JsonArray prices = new JsonArray();
				for (Price price : slot.prices) {
					JsonObject po = new JsonObject();
					po.addProperty("type", price.type);
					po.addProperty("amount", price.amount);
					if (price.type == PRICE_ITEM) po.addProperty("item", ShopCompat.stackSnbt(price.item));
					if (price.type == PRICE_SCORE) po.addProperty("objective", price.objective);
					prices.add(po);
				}
				o.add("prices", prices);
				if (!slot.options.isEmpty()) {
					JsonArray opts = new JsonArray();
					for (Option opt : slot.options) {
						JsonObject eo = new JsonObject();
						eo.addProperty("item", ShopCompat.stackSnbt(opt.item));
						eo.addProperty("count", opt.count);
						eo.addProperty("label", opt.label);
						eo.addProperty("command", opt.command);
						opts.add(eo);
					}
					o.add("options", opts);
				}
				arr.add(o);
			}
			all.add(arr);
		}
		root.add("pages", all);
		return GSON.toJson(root) + "\n";
	}

	public static ShopData fromJson(String json) {
		try {
			JsonObject root = JsonParser.parseString(json).getAsJsonObject();
			ShopData shop = new ShopData();
			shop.id = str(root, "id");
			if (!validId(shop.id)) return null;
			shop.title = str(root, "title");
			String container = str(root, "container");
			for (String c : CONTAINERS) {
				if (c.equals(container)) shop.container = c;
			}
			shop.okText = str(root, "ok");
			shop.failText = str(root, "fail");
			shop.sound = flag(root, "sound", true);
			shop.strict = flag(root, "strict", true);
			int size = shop.size();
			JsonElement all = root.get("pages");
			if (all != null && all.isJsonArray()) {
				shop.pages.clear();
				for (JsonElement el : all.getAsJsonArray()) {
					if (shop.pages.size() >= MAX_PAGES) break;
					Slot[] page = blankPage();
					if (el.isJsonArray()) readSlots(page, el.getAsJsonArray(), size);
					shop.pages.add(page);
				}
				if (shop.pages.isEmpty()) shop.pages.add(blankPage());
			} else {
				JsonElement slots = root.get("slots");
				if (slots != null && slots.isJsonArray()) readSlots(shop.pages.get(0), slots.getAsJsonArray(), size);
			}
			shop.setPage(0);
			return shop;
		} catch (RuntimeException e) {
			return null;
		}
	}

	private static void readSlots(Slot[] page, JsonArray slots, int size) {
		for (JsonElement el : slots) {
			if (!el.isJsonObject()) continue;
			JsonObject o = el.getAsJsonObject();
			int index = num(o, "slot", -1);
			if (index < 0 || index >= size) continue;
			Slot slot = page[index];
			int type = num(o, "type", EMPTY);
			slot.type = type < EMPTY || type > RESET ? EMPTY : type;
			slot.item = ShopCompat.stackFromSnbt(str(o, "item"));
			slot.count = clampCount(slot.item, num(o, "count", 1));
			slot.command = str(o, "command");
			slot.offCommand = str(o, "offCommand");
			int target = num(o, "target", NAV_NEXT);
			slot.target = target < NAV_NONE || target >= MAX_PAGES ? NAV_NONE : target;
			slot.group = Math.max(0, Math.min(MAX_GROUP, num(o, "group", 1)));
			slot.defaultOn = flag(o, "defaultOn", false);
			JsonElement opts = o.get("options");
			if (opts != null && opts.isJsonArray()) {
				for (JsonElement oe : opts.getAsJsonArray()) {
					if (slot.options.size() >= MAX_OPTIONS || !oe.isJsonObject()) break;
					JsonObject eo = oe.getAsJsonObject();
					Option opt = new Option();
					opt.item = ShopCompat.stackFromSnbt(str(eo, "item"));
					if (opt.item.isEmpty()) continue;
					opt.count = clampCount(opt.item, num(eo, "count", 1));
					opt.label = str(eo, "label");
					opt.command = str(eo, "command");
					slot.options.add(opt);
				}
			}
			JsonElement prices = o.get("prices");
			if (prices == null || !prices.isJsonArray()) continue;
			for (JsonElement pe : prices.getAsJsonArray()) {
				if (!pe.isJsonObject()) continue;
				JsonObject po = pe.getAsJsonObject();
				Price p = new Price();
				p.type = num(po, "type", PRICE_ITEM);
				p.amount = Math.max(1, Math.min(MAX_AMOUNT, num(po, "amount", 1)));
				if (p.type == PRICE_ITEM) {
					p.item = ShopCompat.stackFromSnbt(str(po, "item"));
					if (p.item.isEmpty()) continue;
				} else if (p.type == PRICE_SCORE) {
					p.objective = str(po, "objective");
					if (!validObjective(p.objective)) continue;
				} else if (p.type != PRICE_XP) {
					continue;
				}
				slot.prices.add(p);
			}
		}
	}

	public static int clampCount(ItemStack item, int count) {
		int max = item.isEmpty() ? 64 : item.getMaxStackSize();
		return Math.max(1, Math.min(max, count));
	}

	private static String str(JsonObject o, String key) {
		JsonElement e = o.get(key);
		return e != null && e.isJsonPrimitive() ? e.getAsString() : "";
	}

	private static boolean flag(JsonObject o, String key, boolean def) {
		JsonElement e = o.get(key);
		return e != null && e.isJsonPrimitive() ? e.getAsBoolean() : def;
	}

	private static int num(JsonObject o, String key, int fallback) {
		JsonElement e = o.get(key);
		if (e == null || !e.isJsonPrimitive()) return fallback;
		try {
			return e.getAsInt();
		} catch (RuntimeException ex) {
			return fallback;
		}
	}
}
