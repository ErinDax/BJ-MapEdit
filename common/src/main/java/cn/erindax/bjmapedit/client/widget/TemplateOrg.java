package cn.erindax.bjmapedit.client.widget;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class TemplateOrg {
	private TemplateOrg() {}

	public static String norm(String folder) {
		return folder == null ? "" : folder.trim();
	}

	public static boolean folderEq(String a, String b) {
		return norm(a).equals(norm(b));
	}

	public static boolean hasSel(int selectedTemplate, String selectedFolder) {
		return selectedTemplate >= 0 || !norm(selectedFolder).isEmpty();
	}

	public static void renameCollapsed(Set<String> collapsed, String from, String to) {
		if (collapsed == null) return;
		String oldName = norm(from);
		String newName = norm(to);
		if (oldName.isEmpty() || newName.isEmpty() || oldName.equals(newName)) return;
		if (collapsed.remove(oldName)) collapsed.add(newName);
	}

	public static final class Row {
		public final boolean folder;
		public final String folderName;
		public final int storeIndex;
		public final String name;
		public final int depth;
		public final boolean open;

		private Row(boolean folder, String folderName, int storeIndex, String name, int depth, boolean open) {
			this.folder = folder;
			this.folderName = folderName;
			this.storeIndex = storeIndex;
			this.name = name;
			this.depth = depth;
			this.open = open;
		}

		public static Row dir(String name, boolean open) {
			return new Row(true, name, -1, name, 0, open);
		}

		public static Row item(String folder, int storeIndex, String name, int depth) {
			return new Row(false, folder, storeIndex, name, depth, false);
		}
	}

	public static List<Row> flatten(List<String> folders, List<String> entryFolders, List<String> entryNames, Set<String> collapsed) {
		List<Row> rows = new ArrayList<>();
		LinkedHashSet<String> dirs = new LinkedHashSet<>();
		if (folders != null) {
			for (String f : folders) {
				String n = norm(f);
				if (!n.isEmpty()) dirs.add(n);
			}
		}
		if (entryFolders != null) {
			for (String f : entryFolders) {
				String n = norm(f);
				if (!n.isEmpty()) dirs.add(n);
			}
		}
		List<String> sorted = new ArrayList<>(dirs);
		sorted.sort(String.CASE_INSENSITIVE_ORDER);
		for (String dir : sorted) {
			boolean open = collapsed == null || !collapsed.contains(dir);
			rows.add(Row.dir(dir, open));
			if (!open) continue;
			for (int i = 0; i < entryFolders.size(); i++) {
				if (dir.equals(norm(entryFolders.get(i)))) {
					String name = i < entryNames.size() && entryNames.get(i) != null && !entryNames.get(i).isBlank()
						? entryNames.get(i) : "模板";
					rows.add(Row.item(dir, i, name, 1));
				}
			}
		}
		for (int i = 0; i < entryFolders.size(); i++) {
			if (!norm(entryFolders.get(i)).isEmpty()) continue;
			String name = i < entryNames.size() && entryNames.get(i) != null && !entryNames.get(i).isBlank()
				? entryNames.get(i) : "模板";
			rows.add(Row.item("", i, name, 0));
		}
		return rows;
	}

	public static String uniqueFolder(List<String> folders, String base) {
		String want = base == null || base.isBlank() ? "文件夹" : base.trim();
		Set<String> have = new LinkedHashSet<>();
		if (folders != null) {
			for (String f : folders) {
				String n = norm(f);
				if (!n.isEmpty()) have.add(n);
			}
		}
		if (!have.contains(want)) return want;
		int n = 2;
		while (have.contains(want + n)) n++;
		return want + n;
	}

	public static int rowIconX(int railX, int depth) {
		return railX + 6 + depth * 10;
	}

	public static int rowNameX(int railX, int depth) {
		return rowIconX(railX, depth) + UiTheme.FILE_ICON + 3;
	}
}
