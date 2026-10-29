package cn.erindax.bjmapedit.client.widget;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.util.Locale;
import java.util.Optional;

public final class SectionText {
	public static final int[] MC_COLORS = {
		0xFF000000, 0xFF0000AA, 0xFF00AA00, 0xFF00AAAA,
		0xFFAA0000, 0xFFAA00AA, 0xFFFFAA00, 0xFFAAAAAA,
		0xFF555555, 0xFF5555FF, 0xFF55FF55, 0xFF55FFFF,
		0xFFFF5555, 0xFFFF55FF, 0xFFFFFF55, 0xFFFFFFFF
	};
	public static final char[] MC_COLOR_CODES = {
		'0', '1', '2', '3', '4', '5', '6', '7',
		'8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
	};

	private SectionText() {}

	public static String fromComponent(Component comp) {
		StringBuilder sb = new StringBuilder();
		String[] prev = {""};
		comp.visit((style, text) -> {
			if (text.isEmpty()) return Optional.empty();
			String codes = styleCodes(style);
			if (!codes.equals(prev[0])) {
				if (!prev[0].isEmpty()) sb.append("\u00a7r");
				sb.append(codes);
				prev[0] = codes;
			}
			sb.append(text);
			return Optional.empty();
		}, Style.EMPTY);
		return sb.toString();
	}

	public static Component toComponent(String text, Style baseStyle) {
		if (text == null || text.isEmpty()) return Component.empty();
		MutableComponent result = Component.empty();
		Style currentStyle = baseStyle;
		StringBuilder currentText = new StringBuilder();
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			if (c != '\u00a7' || i + 1 >= text.length()) {
				currentText.append(c);
				continue;
			}
			if (currentText.length() > 0) {
				result.append(Component.literal(currentText.toString()).withStyle(currentStyle));
				currentText.setLength(0);
			}
			char code = text.charAt(i + 1);
			if (code == 'x' || code == 'X') {
				if (isHexRun(text, i)) {
					StringBuilder rgb = new StringBuilder(6);
					for (int k = 3; k <= 13; k += 2) rgb.append(text.charAt(i + k));
					try {
						currentStyle = baseStyle.withColor(Integer.parseInt(rgb.toString(), 16));
					} catch (NumberFormatException ignored) {}
					i += 13;
				} else {
					i++;
				}
				continue;
			}
			ChatFormatting formatting = ChatFormatting.getByCode(code);
			if (formatting != null) {
				if (formatting == ChatFormatting.RESET) {
					currentStyle = baseStyle;
				} else if (formatting.isColor()) {
					Integer color = formatting.getColor();
					currentStyle = color != null ? baseStyle.withColor(color) : baseStyle;
				} else {
					currentStyle = applyFormat(currentStyle, formatting);
				}
			}
			i++;
		}
		if (currentText.length() > 0) {
			result.append(Component.literal(currentText.toString()).withStyle(currentStyle));
		}
		return result;
	}

	public static Style applyFormat(Style style, ChatFormatting formatting) {
		return switch (formatting) {
			case BOLD -> style.withBold(true);
			case ITALIC -> style.withItalic(true);
			case UNDERLINE -> style.withUnderlined(true);
			case STRIKETHROUGH -> style.withStrikethrough(true);
			case OBFUSCATED -> style.withObfuscated(true);
			default -> style;
		};
	}

	private static boolean isHexRun(String text, int i) {
		if (i + 13 >= text.length()) return false;
		for (int k = 2; k <= 12; k += 2) {
			if (text.charAt(i + k) != '\u00a7') return false;
		}
		return true;
	}

	public static String styleCodes(Style style) {
		StringBuilder sb = new StringBuilder();
		TextColor color = style.getColor();
		if (color != null) {
			int rgb = color.getValue() & 0xFFFFFF;
			int idx = -1;
			for (int i = 0; i < MC_COLORS.length; i++) {
				if ((MC_COLORS[i] & 0xFFFFFF) == rgb) idx = i;
			}
			if (idx >= 0) {
				sb.append('\u00a7').append(MC_COLOR_CODES[idx]);
			} else {
				sb.append("\u00a7x");
				for (char h : String.format(Locale.ROOT, "%06x", rgb).toCharArray()) sb.append('\u00a7').append(h);
			}
		}
		if (style.isBold()) sb.append("\u00a7l");
		if (style.isItalic()) sb.append("\u00a7o");
		if (style.isUnderlined()) sb.append("\u00a7n");
		if (style.isStrikethrough()) sb.append("\u00a7m");
		if (style.isObfuscated()) sb.append("\u00a7k");
		return sb.toString();
	}

}
