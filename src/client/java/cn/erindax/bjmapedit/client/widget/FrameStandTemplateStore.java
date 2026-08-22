package cn.erindax.bjmapedit.client.widget;

import cn.erindax.bjmapedit.BjMapEdit;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class FrameStandTemplateStore {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
	private static final Bucket FRAMES = new Bucket("frame_templates.json");
	private static final Bucket STANDS = new Bucket("stand_templates.json");

	private FrameStandTemplateStore() {}

	public static List<Entry> all(int tab) {
		return bucket(tab).all();
	}

	public static List<String> folders(int tab) {
		return bucket(tab).folders();
	}

	public static List<TemplateOrg.Row> rows(int tab, Set<String> collapsed) {
		return bucket(tab).rows(collapsed);
	}

	public static String addFolder(int tab) {
		return bucket(tab).addFolder();
	}

	public static void renameFolder(int tab, String oldName, String newName) {
		bucket(tab).renameFolder(oldName, newName);
	}

	public static void removeFolder(int tab, String name) {
		bucket(tab).removeFolder(name);
	}

	public static boolean add(int tab, Entry entry) {
		return bucket(tab).add(entry);
	}

	public static void rename(int tab, int index, String name) {
		bucket(tab).rename(index, name);
	}

	public static void remove(int tab, int index) {
		bucket(tab).remove(index);
	}

	public static void moveToFolder(int tab, int index, String folder) {
		bucket(tab).moveToFolder(index, folder);
	}

	public static Entry get(int tab, int index) {
		return bucket(tab).get(index);
	}

	private static Bucket bucket(int tab) {
		return tab == 0 ? FRAMES : STANDS;
	}

	private static final class Bucket {
		private final String fileName;
		private final List<Entry> templates = new ArrayList<>();
		private final List<String> folders = new ArrayList<>();
		private boolean loaded;

		private Bucket(String fileName) {
			this.fileName = fileName;
		}

		private List<Entry> all() {
			load();
			return templates;
		}

		private List<String> folders() {
			load();
			return folders;
		}

		private List<TemplateOrg.Row> rows(Set<String> collapsed) {
			load();
			List<String> eFolders = new ArrayList<>();
			List<String> eNames = new ArrayList<>();
			for (Entry e : templates) {
				eFolders.add(e.folder);
				eNames.add(e.name);
			}
			return TemplateOrg.flatten(folders, eFolders, eNames, collapsed);
		}

		private String addFolder() {
			load();
			String name = TemplateOrg.uniqueFolder(folders, "文件夹");
			folders.add(name);
			save();
			return name;
		}

		private void renameFolder(String oldName, String newName) {
			load();
			String from = TemplateOrg.norm(oldName);
			String to = TemplateOrg.norm(newName);
			if (from.isEmpty() || to.isEmpty() || from.equals(to)) return;
			if (folders.stream().anyMatch(f -> to.equals(TemplateOrg.norm(f)))) return;
			for (int i = 0; i < folders.size(); i++) {
				if (from.equals(TemplateOrg.norm(folders.get(i)))) folders.set(i, to);
			}
			for (Entry e : templates) {
				if (from.equals(TemplateOrg.norm(e.folder))) e.folder = to;
			}
			save();
		}

		private void removeFolder(String name) {
			load();
			String n = TemplateOrg.norm(name);
			if (n.isEmpty()) return;
			templates.removeIf(e -> n.equals(TemplateOrg.norm(e.folder)));
			folders.removeIf(f -> n.equals(TemplateOrg.norm(f)));
			save();
		}

		private boolean add(Entry entry) {
			load();
			if (entry == null) return false;
			if (entry.name == null || entry.name.isBlank()) {
				entry.name = "模板";
			}
			entry.folder = TemplateOrg.norm(entry.folder);
			if (!entry.folder.isEmpty() && folders.stream().noneMatch(f -> entry.folder.equals(TemplateOrg.norm(f)))) {
				folders.add(entry.folder);
			}
			templates.add(entry);
			save();
			return false;
		}

		private void rename(int index, String name) {
			load();
			if (index < 0 || index >= templates.size()) return;
			String trimmed = name == null ? "" : name.trim();
			if (trimmed.isEmpty()) return;
			templates.get(index).name = trimmed;
			save();
		}

		private void remove(int index) {
			load();
			if (index < 0 || index >= templates.size()) return;
			templates.remove(index);
			save();
		}

		private void moveToFolder(int index, String folder) {
			load();
			if (index < 0 || index >= templates.size()) return;
			Entry e = templates.get(index);
			e.folder = TemplateOrg.norm(folder);
			if (!e.folder.isEmpty() && folders.stream().noneMatch(f -> e.folder.equals(TemplateOrg.norm(f)))) {
				folders.add(e.folder);
			}
			save();
		}

		private Entry get(int index) {
			load();
			if (index < 0 || index >= templates.size()) return null;
			return templates.get(index);
		}

		private void load() {
			if (loaded) return;
			loaded = true;
			templates.clear();
			folders.clear();
			try {
				String json = TemplateFiles.read(fileName);
				if (json == null) return;
				FileData data = GSON.fromJson(json, FileData.class);
				if (data == null) return;
				if (data.folders != null) {
					for (String f : data.folders) TemplateFiles.addFolder(folders, f);
				}
				if (data.templates != null) {
					for (Entry e : data.templates) {
						if (e == null) continue;
						e.folder = TemplateOrg.norm(e.folder);
						templates.add(e);
						if (!e.folder.isEmpty()) TemplateFiles.addFolder(folders, e.folder);
					}
				}
			} catch (Exception e) {
				BjMapEdit.LOGGER.warn("Failed to load {}", fileName, e);
			}
		}

		private void save() {
			try {
				FileData data = new FileData();
				data.folders = new ArrayList<>(folders);
				data.templates = new ArrayList<>(templates);
				TemplateFiles.write(fileName, GSON.toJson(data));
			} catch (Exception e) {
				BjMapEdit.LOGGER.warn("Failed to save {}", fileName, e);
			}
		}
	}

	private static final class FileData {
		List<String> folders = new ArrayList<>();
		List<Entry> templates = new ArrayList<>();
	}

	public static final class Entry {
		public String name = "模板";
		public String folder = "";
		public boolean glow;
		public boolean frameInvis;
		public boolean fixed;
		public int facing = 3;
		public int rotation;
		public String item = "";
		public boolean arms;
		public boolean noBase;
		public boolean small;
		public boolean standInvis;
		public boolean marker;
		public boolean noGrav;
		public String[] poses = new String[18];
	}
}
