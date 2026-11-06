package cn.erindax.bjmapedit.client.widget;

import net.minecraft.client.Minecraft;

import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

public final class TemplateFiles {
	private TemplateFiles() {}

	public static String read(String suffix) throws Exception {
		Path path = file(suffix);
		if (!Files.isRegularFile(path)) return null;
		return Files.readString(path, StandardCharsets.UTF_8);
	}

	public static void write(String suffix, String json) throws Exception {
		Path path = file(suffix);
		Files.createDirectories(path.getParent());
		Path tmp = path.resolveSibling(path.getFileName() + ".tmp");
		Files.writeString(tmp, withoutLoneSurrogates(json), StandardCharsets.UTF_8);
		try {
			Files.move(tmp, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
		} catch (AtomicMoveNotSupportedException e) {
			Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING);
		}
	}

	private static String withoutLoneSurrogates(String text) {
		StringBuilder sb = new StringBuilder(text.length());
		text.codePoints().filter(cp -> cp < Character.MIN_SURROGATE || cp > Character.MAX_SURROGATE).forEach(sb::appendCodePoint);
		return sb.toString();
	}

	public static void backup(String suffix) {
		Path path = file(suffix);
		if (!Files.isRegularFile(path)) return;
		try {
			Files.move(path, path.resolveSibling(path.getFileName() + "." + System.currentTimeMillis() + ".bak"));
		} catch (Exception ignored) {
		}
	}
	static void addFolder(List<String> folders, String raw) {
		String n = TemplateOrg.norm(raw);
		if (!n.isEmpty() && !folders.contains(n)) folders.add(n);
	}

	private static Path file(String suffix) {
		Path dir = Minecraft.getInstance().gameDirectory.toPath().resolve("config");
		Path neu = dir.resolve("bj_mapedit_" + suffix);
		Path old = dir.resolve("mapedit_bj_" + suffix);
		if (!Files.isRegularFile(neu) && Files.isRegularFile(old)) {
			try {
				Files.createDirectories(dir);
				Files.copy(old, neu);
			} catch (Exception ignored) {
			}
		}
		return neu;
	}
}
