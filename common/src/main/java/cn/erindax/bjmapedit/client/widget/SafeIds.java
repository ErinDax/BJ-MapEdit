package cn.erindax.bjmapedit.client.widget;

import net.minecraft.resources.ResourceLocation;

public final class SafeIds {
	private SafeIds() {}

	public static ResourceLocation tryParse(String raw) {
		if (raw == null) return null;
		String s = raw.trim();
		if (s.isEmpty() || !looksLikeId(s)) return null;
		try {
			return ResourceLocation.tryParse(s);
		} catch (Throwable ignored) {
			return null;
		}
	}

	public static ResourceLocation tryParseItem(String raw) {
		ResourceLocation id = tryParse(raw);
		if (id != null) return id;
		if (raw == null) return null;
		String s = raw.trim();
		if (s.isEmpty() || s.indexOf(':') >= 0 || !looksLikeId(s)) return null;
		try {
			return ResourceLocation.withDefaultNamespace(s);
		} catch (Throwable ignored) {
			return null;
		}
	}

	public static ResourceLocation tryParseOrDefault(String raw, String fallbackPath) {
		ResourceLocation id = tryParse(raw);
		if (id != null) return id;
		try {
			return ResourceLocation.withDefaultNamespace(fallbackPath);
		} catch (Exception e) {
			return ResourceLocation.withDefaultNamespace("air");
		}
	}

	public static boolean isValidPathChar(char c) {
		return (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '-' || c == '/' || c == '.' || c == ':';
	}

	public static boolean looksLikeId(String raw) {
		if (raw == null || raw.isEmpty()) return false;
		for (int i = 0; i < raw.length(); i++) {
			char c = raw.charAt(i);
			if (!isValidPathChar(c) && c != ' ') return false;
		}
		return true;
	}
}