package cn.erindax.bjmapedit.client.widget;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.util.List;
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
		return fromComponent(comp, Style.EMPTY);
	}

	public static String fromComponent(Component comp, Style inherited) {
		return fromComponent(comp, inherited, null);
	}

	public static String fromComponent(Component comp, Style inherited, List<TextClicks.Click> clicks) {
		StringBuilder sb = new StringBuilder();
		String[] prev = {""};
		int[] open = {-1};
		comp.visit((style, text) -> {
			if (text.isEmpty()) return Optional.empty();
			if (clicks != null) {
				int idx = clickIndex(clicks, TextClicks.fromEvent(style.getClickEvent()));
				if (idx != open[0]) {
					sb.append(TextClicks.marker(idx));
					open[0] = idx;
				}
			}
			String codes = styleCodes(style);
			if (!codes.equals(prev[0])) {
				if (!prev[0].isEmpty()) sb.append("\u00a7r");
				sb.append(codes);
				prev[0] = codes;
			}
			sb.append(text);
			return Optional.empty();
		}, inherited);
		return sb.toString();
	}

	private static int clickIndex(List<TextClicks.Click> clicks, TextClicks.Click click) {
		if (click == null) return -1;
		int idx = clicks.indexOf(click);
		if (idx >= 0) return idx;
		clicks.add(click);
		return clicks.size() - 1;
	}

	public static Component toComponent(String text, Style baseStyle) {
		return toComponent(text, baseStyle, null);
	}

	public static Component toComponent(String text, Style baseStyle, List<TextClicks.Click> clicks) {
		if (text == null || text.isEmpty()) return Component.empty();
		MutableComponent result = Component.empty();
		Style currentStyle = baseStyle;
		ClickEvent click = null;
		StringBuilder currentText = new StringBuilder();
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			if (c != '\u00a7' || i + 1 >= text.length()) {
				currentText.append(c);
				continue;
			}
			if (currentText.length() > 0) {
				result.append(Component.literal(currentText.toString()).withStyle(withClick(currentStyle, click)));
				currentText.setLength(0);
			}
			char code = text.charAt(i + 1);
			if (clicks != null && TextClicks.isMarker(code)) {
				click = TextClicks.toEvent(TextClicks.at(clicks, TextClicks.indexOf(code)));
				i++;
				continue;
			}
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
			result.append(Component.literal(currentText.toString()).withStyle(withClick(currentStyle, click)));
		}
		return result;
	}

	private static Style withClick(Style style, ClickEvent click) {
		return click == null ? style : style.withClickEvent(click);
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

	public record Repair(String text, int cursor) {}

	public static Repair keepCodesWhole(String before, String after) {
		if (before == null || after == null || before.equals(after) || before.indexOf('\u00a7') < 0) return null;
		int max = Math.min(before.length(), after.length());
		int p = 0;
		while (p < max && before.charAt(p) == after.charAt(p)) p++;
		int s = 0;
		while (s < max - p && before.charAt(before.length() - 1 - s) == after.charAt(after.length() - 1 - s)) s++;
		int delStart = p;
		int delEnd = before.length() - s;
		String inserted = after.substring(p, after.length() - s);
		int start = delStart;
		int end = delEnd;
		for (int i = 0; i < before.length(); ) {
			int len = TextClicks.codeLength(before, i);
			if (len <= 0) {
				i++;
				continue;
			}
			int tokenEnd = i + len;
			if (i < delStart && delStart < tokenEnd) start = Math.min(start, i);
			if (i < delEnd && delEnd < tokenEnd) end = Math.max(end, tokenEnd);
			i = tokenEnd;
		}
		if (start == delStart && end == delEnd) return null;
		if (delStart == delEnd) {
			String text = before.substring(0, end) + inserted + before.substring(end);
			return new Repair(text, end + inserted.length());
		}
		String text = before.substring(0, start) + inserted + before.substring(end);
		return new Repair(text, start + inserted.length());
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
