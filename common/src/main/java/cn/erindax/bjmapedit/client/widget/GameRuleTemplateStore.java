package cn.erindax.bjmapedit.client.widget;

import cn.erindax.bjmapedit.BjMapEdit;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class GameRuleTemplateStore {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
	private static final List<Entry> TEMPLATES = new ArrayList<>();
	private static final List<String> FOLDERS = new ArrayList<>();
	private static boolean loaded;

	private GameRuleTemplateStore() {}

	public static List<Entry> all() {
		load();
		return TEMPLATES;
	}

	public static List<String> folders() {
		load();
		return FOLDERS;
	}

	public static List<TemplateOrg.Row> rows(java.util.Set<String> collapsed) {
		load();
		List<String> eFolders = new ArrayList<>();
		List<String> eNames = new ArrayList<>();
		for (Entry e : TEMPLATES) {
			eFolders.add(e.folder);
			eNames.add(e.name);
		}
		return TemplateOrg.flatten(FOLDERS, eFolders, eNames, collapsed);
	}

	public static String addFolder() {
		load();
		String name = TemplateOrg.uniqueFolder(FOLDERS, "文件夹");
		FOLDERS.add(name);
		save();
		return name;
	}

	public static void renameFolder(String oldName, String newName) {
		load();
		String from = TemplateOrg.norm(oldName);
		String to = TemplateOrg.norm(newName);
		if (from.isEmpty() || to.isEmpty() || from.equals(to)) return;
		if (FOLDERS.stream().anyMatch(f -> to.equals(TemplateOrg.norm(f)))) return;
		for (int i = 0; i < FOLDERS.size(); i++) {
			if (from.equals(TemplateOrg.norm(FOLDERS.get(i)))) FOLDERS.set(i, to);
		}
		for (Entry e : TEMPLATES) {
			if (from.equals(TemplateOrg.norm(e.folder))) e.folder = to;
		}
		save();
	}

	public static void removeFolder(String name) {
		load();
		String n = TemplateOrg.norm(name);
		if (n.isEmpty()) return;
		TEMPLATES.removeIf(e -> n.equals(TemplateOrg.norm(e.folder)));
		FOLDERS.removeIf(f -> n.equals(TemplateOrg.norm(f)));
		save();
	}

	public static boolean add(Entry entry) {
		load();
		if (entry == null) return false;
		if (entry.name == null || entry.name.isBlank()) {
			entry.name = "模板";
		}
		if (entry.values == null) entry.values = new HashMap<>();
		entry.folder = TemplateOrg.norm(entry.folder);
		if (!entry.folder.isEmpty() && FOLDERS.stream().noneMatch(f -> entry.folder.equals(TemplateOrg.norm(f)))) {
			FOLDERS.add(entry.folder);
		}
		TEMPLATES.add(entry);
		save();
		return false;
	}

	public static void rename(int index, String name) {
		load();
		if (index < 0 || index >= TEMPLATES.size()) return;
		String trimmed = name == null ? "" : name.trim();
		if (trimmed.isEmpty()) return;
		TEMPLATES.get(index).name = trimmed;
		save();
	}

	public static void remove(int index) {
		load();
		if (index < 0 || index >= TEMPLATES.size()) return;
		TEMPLATES.remove(index);
		save();
	}

	public static void moveToFolder(int index, String folder) {
		load();
		if (index < 0 || index >= TEMPLATES.size()) return;
		Entry e = TEMPLATES.get(index);
		e.folder = TemplateOrg.norm(folder);
		if (!e.folder.isEmpty() && FOLDERS.stream().noneMatch(f -> e.folder.equals(TemplateOrg.norm(f)))) {
			FOLDERS.add(e.folder);
		}
		save();
	}

	public static Entry get(int index) {
		load();
		if (index < 0 || index >= TEMPLATES.size()) return null;
		return TEMPLATES.get(index);
	}

	private static void load() {
		if (loaded) return;
		loaded = true;
		TEMPLATES.clear();
		FOLDERS.clear();
		try {
			String json = TemplateFiles.read("rule_templates.json");
			if (json == null) return;
			FileData data = GSON.fromJson(json, FileData.class);
			if (data == null) return;
			if (data.folders != null) {
				for (String f : data.folders) TemplateFiles.addFolder(FOLDERS, f);
			}
			if (data.templates != null) {
				for (Entry e : data.templates) {
					if (e == null) continue;
					if (e.values == null) e.values = new HashMap<>();
					e.folder = TemplateOrg.norm(e.folder);
					TEMPLATES.add(e);
					if (!e.folder.isEmpty()) TemplateFiles.addFolder(FOLDERS, e.folder);
				}
			}
		} catch (Exception e) {
			BjMapEdit.LOGGER.warn("Failed to load game rule templates", e);
		}
	}

	private static void save() {
		try {
			FileData data = new FileData();
			data.folders = new ArrayList<>(FOLDERS);
			data.templates = new ArrayList<>(TEMPLATES);
			TemplateFiles.write("rule_templates.json", GSON.toJson(data));
		} catch (Exception e) {
			BjMapEdit.LOGGER.warn("Failed to save game rule templates", e);
		}
	}

	private static final class FileData {
		List<String> folders = new ArrayList<>();
		List<Entry> templates = new ArrayList<>();
	}

	public static final class Entry {
		public String name = "模板";
		public String folder = "";
		public Map<String, String> values = new HashMap<>();
	}
}
