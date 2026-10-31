package cn.erindax.bjmapedit.client.widget;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class TextClicks {
	public static final String RUN_COMMAND = "run_command";
	public static final String CHANGE_PAGE = "change_page";
	public static final String OPEN_URL = "open_url";
	public static final String COPY_TO_CLIPBOARD = "copy_to_clipboard";
	public static final String[] ACTIONS = {RUN_COMMAND, CHANGE_PAGE, OPEN_URL, COPY_TO_CLIPBOARD};
	private static final char END = '\uE000';
	private static final char FIRST = '\uE001';
	private static final char LAST = '\uF8FF';

	private TextClicks() {}

	public static final class Click {
		public String action;
		public String value;

		public Click() {}

		public Click(String action, String value) {
			this.action = action;
			this.value = value;
		}

		@Override
		public boolean equals(Object o) {
			return o instanceof Click c && Objects.equals(action, c.action) && Objects.equals(value, c.value);
		}

		@Override
		public int hashCode() {
			return Objects.hash(action, value);
		}
	}

	public static boolean isMarker(char c) {
		return c >= END && c <= LAST;
	}

	public static int indexOf(char marker) {
		return marker == END ? -1 : marker - FIRST;
	}

	public static String marker(int index) {
		char c = index < 0 || index > LAST - FIRST ? END : (char) (FIRST + index);
		return "\u00a7" + c;
	}

	public static Click at(List<Click> clicks, int index) {
		return clicks != null && index >= 0 && index < clicks.size() ? clicks.get(index) : null;
	}

	public static int codeLength(String text, int i) {
		if (i + 1 >= text.length() || text.charAt(i) != '\u00a7' || text.charAt(i + 1) == '\n') return 0;
		char code = text.charAt(i + 1);
		if ((code == 'x' || code == 'X') && i + 13 < text.length()) {
			for (int k = 2; k <= 12; k += 2) {
				if (text.charAt(i + k) != '\u00a7') return 2;
			}
			return 14;
		}
		return 2;
	}

	private static boolean markerAt(String text, int i, int len) {
		return len == 2 && isMarker(text.charAt(i + 1));
	}

	public static int stateAfter(String text, int initial) {
		int state = initial;
		for (int i = 0; i < text.length(); ) {
			int len = codeLength(text, i);
			if (len == 0) {
				i++;
				continue;
			}
			if (markerAt(text, i, len)) state = indexOf(text.charAt(i + 1));
			i += len;
		}
		return state;
	}

	public static int visibleIndex(String text, int offset) {
		int v = 0;
		int limit = Math.min(offset, text.length());
		for (int i = 0; i < limit; ) {
			int len = codeLength(text, i);
			if (len > 0) {
				i += len;
				continue;
			}
			v++;
			i++;
		}
		return v;
	}

	public static int offsetOfVisible(String text, int index) {
		int v = 0;
		for (int i = 0; i < text.length(); ) {
			int len = codeLength(text, i);
			if (len > 0) {
				i += len;
				continue;
			}
			if (v == index) return i;
			v++;
			i++;
		}
		return text.length();
	}

	public static int[] states(String text) {
		int[] out = new int[visibleIndex(text, text.length())];
		int state = -1;
		int v = 0;
		for (int i = 0; i < text.length(); ) {
			int len = codeLength(text, i);
			if (len > 0) {
				if (markerAt(text, i, len)) state = indexOf(text.charAt(i + 1));
				i += len;
				continue;
			}
			out[v++] = state;
			i++;
		}
		return out;
	}

	public static String rebuild(String text, int[] states) {
		StringBuilder sb = new StringBuilder(text.length() + 8);
		int emitted = -1;
		int v = 0;
		for (int i = 0; i < text.length(); ) {
			int len = codeLength(text, i);
			if (len > 0) {
				if (!markerAt(text, i, len)) sb.append(text, i, i + len);
				i += len;
				continue;
			}
			int want = v < states.length ? states[v] : -1;
			if (want != emitted) {
				sb.append(marker(want));
				emitted = want;
			}
			sb.append(text.charAt(i));
			v++;
			i++;
		}
		return sb.toString();
	}

	public static String tidy(String text) {
		if (text == null || text.indexOf('\u00a7') < 0) return text;
		return rebuild(text, states(text));
	}

	public static String lastMarker(String text) {
		String found = "";
		for (int i = 0; i < text.length(); ) {
			int len = codeLength(text, i);
			if (len == 0) {
				i++;
				continue;
			}
			if (markerAt(text, i, len)) found = text.substring(i, i + 2);
			i += len;
		}
		return found;
	}

	public static List<Click> compact(List<List<String>> groups, List<Click> clicks) {
		List<Click> out = new ArrayList<>();
		int[] map = new int[clicks == null ? 0 : clicks.size()];
		Arrays.fill(map, -2);
		for (List<String> group : groups) {
			for (int i = 0; i < group.size(); i++) {
				group.set(i, remap(group.get(i), clicks, map, out));
			}
		}
		return out;
	}

	private static String remap(String text, List<Click> clicks, int[] map, List<Click> out) {
		if (text == null || text.indexOf('\u00a7') < 0) return text;
		StringBuilder sb = new StringBuilder(text.length());
		for (int i = 0; i < text.length(); ) {
			int len = codeLength(text, i);
			if (len == 0) {
				sb.append(text.charAt(i));
				i++;
				continue;
			}
			if (markerAt(text, i, len)) {
				int idx = indexOf(text.charAt(i + 1));
				sb.append(marker(idx < 0 ? -1 : target(idx, clicks, map, out)));
			} else {
				sb.append(text, i, i + len);
			}
			i += len;
		}
		return sb.toString();
	}

	private static int target(int idx, List<Click> clicks, int[] map, List<Click> out) {
		if (idx >= map.length) return -1;
		if (map[idx] == -2) {
			Click c = clicks.get(idx);
			if (c == null || cleanValue(c.action, c.value) == null) {
				map[idx] = -1;
			} else {
				int found = out.indexOf(c);
				if (found < 0) {
					out.add(c);
					found = out.size() - 1;
				}
				map[idx] = found;
			}
		}
		return map[idx];
	}

	public static String cleanValue(String action, String value) {
		if (action == null || value == null || value.trim().isEmpty()) return null;
		String v = value.trim();
		switch (action) {
			case RUN_COMMAND: {
				String cmd = v.startsWith("/") ? v.substring(1).trim() : v;
				return cmd.isEmpty() ? null : "/" + cmd;
			}
			case CHANGE_PAGE: {
				try {
					int page = Integer.parseInt(v);
					return page >= 1 ? String.valueOf(page) : null;
				} catch (NumberFormatException e) {
					return null;
				}
			}
			case OPEN_URL: {
				String url = v.contains("://") ? v : "https://" + v;
				try {
					String scheme = new URI(url).getScheme();
					if (scheme == null) return null;
					String s = scheme.toLowerCase(Locale.ROOT);
					return s.equals("http") || s.equals("https") ? url : null;
				} catch (URISyntaxException e) {
					return null;
				}
			}
			case COPY_TO_CLIPBOARD:
				return value;
			default:
				return null;
		}
	}

	public static ClickEvent toEvent(Click c) {
		if (c == null) return null;
		String v = cleanValue(c.action, c.value);
		if (v == null) return null;
		try {
			return switch (c.action) {
				case RUN_COMMAND -> new ClickEvent.RunCommand(v);
				case CHANGE_PAGE -> new ClickEvent.ChangePage(Integer.parseInt(v));
				case OPEN_URL -> new ClickEvent.OpenUrl(new URI(v));
				case COPY_TO_CLIPBOARD -> new ClickEvent.CopyToClipboard(v);
				default -> null;
			};
		} catch (URISyntaxException | NumberFormatException e) {
			return null;
		}
	}

	public static Click fromEvent(ClickEvent e) {
		return switch (e) {
			case ClickEvent.RunCommand r -> new Click(RUN_COMMAND, r.command());
			case ClickEvent.ChangePage p -> new Click(CHANGE_PAGE, String.valueOf(p.page()));
			case ClickEvent.OpenUrl u -> new Click(OPEN_URL, u.uri().toString());
			case ClickEvent.CopyToClipboard c -> new Click(COPY_TO_CLIPBOARD, c.value());
			case null, default -> null;
		};
	}

	public static List<Component> tooltip(Click c) {
		List<Component> lines = new ArrayList<>();
		lines.add(Component.translatable("screen.bj_mapedit.click." + c.action).withStyle(ChatFormatting.GOLD));
		lines.add(Component.literal(c.value == null ? "" : c.value).withStyle(ChatFormatting.GRAY));
		return lines;
	}
}
