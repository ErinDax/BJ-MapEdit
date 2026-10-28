package cn.erindax.bjmapedit.client.widget;

import net.minecraft.client.Minecraft;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
		Files.writeString(path, json, StandardCharsets.UTF_8);
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
