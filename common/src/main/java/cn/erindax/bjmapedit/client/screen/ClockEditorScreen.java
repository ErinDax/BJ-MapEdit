package cn.erindax.bjmapedit.client.screen;

import cn.erindax.bjmapedit.client.DatapackNet;
import cn.erindax.bjmapedit.client.EditorAccess;
import cn.erindax.bjmapedit.client.widget.UiTheme;
import cn.erindax.bjmapedit.networking.ClockOps;
import cn.erindax.bjmapedit.networking.DatapackOps;
import cn.erindax.bjmapedit.networking.payload.DatapackOpResultPayload;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class ClockEditorScreen extends Screen {
	private static final String K = "screen.bj_mapedit.";
	private static final int PAD = 8;
	private static final int LINE = 22;
	private static final int BTN_H = 16;
	private static final int GAP = 3;
	private static final int ARROW_W = 14;
	private static final int FIELD_W = 46;
	private static final int OK = 0xFF7EE787;
	private static final int ERR = 0xFFFF7B72;
	private static final int HINT = 0xFF6E7681;
	private static final int CHIP = 0xFF3C3C3C;
	private static final int CHIP_HOVER = 0xFF4A4A4A;
	private static final int CHIP_ON = 0xFF2E7D32;
	private static final int CHIP_ON_HOVER = 0xFF388E3C;
	private static final String[] ROWS = {"clock_preview", "clock_hud", "clock_layout", "clock_day", "clock_flow", "clock_time"};
	private static final String[] ORDERS = {"dts", "dst", "tds", "tsd", "sdt", "std"};
	private static final int[] FLOWS = {0, -8, -4, -2, 1, 2, 4, 8};
	private static final String[] FLOW_LABELS = {"", "1/8", "1/4", "1/2", "1x", "2x", "4x", "8x"};
	private static final String[] TIME_LABELS = {"06:00", "09:00", "12:00", "15:00", "18:00", "21:00", "00:00", "03:00"};
	private static final String[] SEGMENTS = {"深夜", "凌晨", "清晨", "上午", "正午", "下午", "黄昏", "夜晚"};
	private static final ChatFormatting[] SEGMENT_COLORS = {
		ChatFormatting.DARK_BLUE, ChatFormatting.AQUA, ChatFormatting.YELLOW, ChatFormatting.GOLD,
		ChatFormatting.WHITE, ChatFormatting.GOLD, ChatFormatting.RED, ChatFormatting.BLUE
	};

	private record Btn(int x, int y, int w, Component label, boolean chip, boolean on, boolean active, Runnable action) {}

	private final List<Btn> buttons = new ArrayList<>();
	private EditBox dayField;
	private String dayText = "";
	private boolean loaded;
	private boolean installed;
	private boolean busy;
	private int busyTicks;
	private int pollIn;
	private int ord = 1;
	private boolean showD = true;
	private boolean showT = true;
	private boolean showS = true;
	private int dayOff;
	private int speed = 1;
	private int slow = 1;
	private boolean cycle = true;
	private boolean me;
	private int on;
	private int online;
	private Component status = Component.empty();
	private int statusColor;
	private int leftPos;
	private int topPos;
	private int panelW;
	private int panelH;
	private int contentX;
	private int contentW;
	private int rowY;
	private int hudTextX;
	private int dayTextX;

	public ClockEditorScreen() {
		super(Component.translatable(K + "clock_title"));
	}

	@Override
	protected void init() {
		panelW = Mth.clamp(this.width - 16, 360, 430);
		panelH = 184;
		leftPos = (this.width - panelW) / 2;
		topPos = Math.max(4, (this.height - panelH) / 2);
		int labelW = 0;
		for (String key : ROWS) labelW = Math.max(labelW, font.width(I18n.get(K + key)));
		contentX = leftPos + PAD + labelW + 8;
		contentW = leftPos + panelW - PAD - contentX;
		rowY = topPos + UiTheme.HEADER_H + 8;
		dayField = new EditBox(font, contentX, rowY + 3 * LINE, FIELD_W, BTN_H, Component.empty());
		dayField.setMaxLength(6);
		dayField.setFilter(s -> s.matches("[0-9]{0,6}"));
		dayField.setHint(Component.translatable(K + "clock_day_hint").withStyle(Style.EMPTY.withColor(HINT)));
		dayField.setValue(dayText);
		dayField.setResponder(s -> dayText = s);
		this.addRenderableWidget(dayField);
		rebuild();
		if (!loaded) requestState();
	}

	private Component tr(String key, Object... args) {
		return Component.translatable(K + key, args);
	}

	private int fit(Component label) {
		return font.width(label) + 12;
	}

	private int add(int x, int y, int w, Component label, boolean chip, boolean on, boolean active, Runnable action) {
		buttons.add(new Btn(x, y, w, label, chip, on, active, action));
		return x + w + GAP;
	}

	private void rebuild() {
		buttons.clear();
		dayField.visible = loaded && installed;
		if (!loaded) return;
		if (!installed) {
			Component label = tr("clock_install");
			int w = font.width(label) + 28;
			add(leftPos + (panelW - w) / 2, topPos + panelH / 2 + 6, w, label, false, false, !busy && EditorAccess.canSendDatapack(), this::install);
			return;
		}
		int y = rowY + LINE;
		Component allOn = tr("clock_hud_all_on");
		Component allOff = tr("clock_hud_all_off");
		int x = add(contentX, y, fit(allOn), allOn, false, false, true, () -> send("hud", "on"));
		x = add(x, y, fit(allOff), allOff, false, false, true, () -> send("hud", "off"));
		boolean mine = me;
		hudTextX = add(x, y, Math.max(fit(tr("clock_hud_me_on")), fit(tr("clock_hud_me_off"))), tr(mine ? "clock_hud_me_on" : "clock_hud_me_off"), true, mine, true, () -> send("me", mine ? "off" : "on")) + 4;

		y += LINE;
		String order = ORDERS[ord - 1];
		int cellW = (contentW - 12) / 3;
		for (int i = 0; i < 3; i++) {
			char c = order.charAt(i);
			int pos = i;
			boolean shown = shown(c);
			int cx = contentX + i * (cellW + 6);
			add(cx, y, ARROW_W, Component.literal("<"), false, false, i > 0, () -> move(pos, -1));
			add(cx + ARROW_W + GAP, y, cellW - 2 * (ARROW_W + GAP), tr(shown ? "clock_el_on" : "clock_el_off", tr("clock_el_" + c)), true, shown, true, () -> toggle(c));
			add(cx + cellW - ARROW_W, y, ARROW_W, Component.literal(">"), false, false, i < 2, () -> move(pos, 1));
		}

		y += LINE;
		Component set = tr("clock_day_set");
		Component reset = tr("clock_day_reset");
		Component next = tr("clock_day_next");
		x = add(contentX + FIELD_W + GAP + 1, y, fit(set), set, false, false, true, this::setDay);
		x = add(x, y, 22, Component.literal("-1"), false, false, true, () -> send("dayadd", "-1"));
		x = add(x, y, 22, Component.literal("+1"), false, false, true, () -> send("dayadd", "1"));
		x = add(x, y, fit(reset), reset, false, false, true, () -> send("dayreset", ""));
		dayTextX = add(x, y, fit(next), next, false, false, true, () -> send("nextday", "")) + 4;

		y += LINE;
		int stepW = (contentW - 7 * GAP) / 8;
		int flow = flowIndex();
		for (int i = 0; i < 8; i++) {
			String value = Integer.toString(FLOWS[i]);
			Component label = i == 0 ? tr("clock_flow_pause") : Component.literal(FLOW_LABELS[i]);
			add(contentX + i * (stepW + GAP), y, stepW, label, true, i == flow, true, () -> send("flow", value));
		}

		y += LINE;
		int segment = (hour() + 18) % 24 / 3;
		for (int i = 0; i < 8; i++) {
			String value = ClockOps.TIMES.get(i);
			add(contentX + i * (stepW + GAP), y, stepW, Component.literal(TIME_LABELS[i]), true, i == segment, true, () -> send("time", value));
		}
	}

	private boolean shown(char c) {
		return c == 'd' ? showD : c == 't' ? showT : showS;
	}

	private int flowIndex() {
		if (speed == 0) return 0;
		if (speed == 2) return 5;
		if (speed == 4) return 6;
		if (speed == 8) return 7;
		if (slow == 8) return 1;
		if (slow == 4) return 2;
		if (slow == 2) return 3;
		return cycle ? 4 : 0;
	}

	private int dayTime() {
		return this.minecraft == null || this.minecraft.level == null ? 0 : (int) Math.floorMod(this.minecraft.level.getDayTime(), 24000L);
	}

	private int hour() {
		return (dayTime() / 1000 + 6) % 24;
	}

	private int day() {
		if (this.minecraft == null || this.minecraft.level == null) return 1 + dayOff;
		return (int) (this.minecraft.level.getGameTime() % 2147483647L) / 24000 + 1 + dayOff;
	}

	private Component preview() {
		int hour = hour();
		int minute = dayTime() % 1000 * 60 / 1000;
		boolean all = !showD && !showT && !showS;
		MutableComponent out = Component.empty();
		boolean first = true;
		for (char c : ORDERS[ord - 1].toCharArray()) {
			if (!all && !shown(c)) continue;
			if (!first) out.append("  ");
			first = false;
			if (c == 'd') {
				out.append("第").append(Component.literal(Integer.toString(day())).withStyle(ChatFormatting.GRAY)).append("天");
			} else if (c == 't') {
				out.append(Component.literal(String.format("%02d:%02d", hour, minute)).withStyle(ChatFormatting.GOLD));
			} else {
				out.append(Component.literal(SEGMENTS[hour / 3]).withStyle(SEGMENT_COLORS[hour / 3]));
			}
		}
		return out;
	}

	private void setStatus(Component text, int color) {
		status = text;
		statusColor = color;
	}

	private void error(String key) {
		setStatus(Component.translatable(key), ERR);
	}

	private void requestState() {
		pollIn = 40;
		if (EditorAccess.canSendDatapack()) DatapackNet.send(DatapackOps.CLOCK_STATE, "", "");
	}

	private void send(String action, String value) {
		setStatus(Component.empty(), 0);
		DatapackNet.send(DatapackOps.CLOCK_APPLY, action, value);
	}

	private void install() {
		busy = true;
		busyTicks = 0;
		setStatus(tr("clock_installing"), UiTheme.cMuted());
		DatapackNet.send(DatapackOps.CLOCK_INSTALL, "", "");
	}

	private void move(int pos, int dir) {
		char[] chars = ORDERS[ord - 1].toCharArray();
		char held = chars[pos];
		chars[pos] = chars[pos + dir];
		chars[pos + dir] = held;
		String target = new String(chars);
		for (int i = 0; i < ORDERS.length; i++) {
			if (ORDERS[i].equals(target)) send("ord", Integer.toString(i + 1));
		}
	}

	private void toggle(char c) {
		int count = (showD ? 1 : 0) + (showT ? 1 : 0) + (showS ? 1 : 0);
		if (shown(c) && count <= 1) {
			error(K + "clock_keep_one");
			return;
		}
		send("show" + c, shown(c) ? "0" : "1");
	}

	private void setDay() {
		int day;
		try {
			day = Integer.parseInt(dayText.trim());
		} catch (NumberFormatException e) {
			day = 0;
		}
		if (day < 1 || day > ClockOps.MAX_DAY) {
			error(K + "clock_day_range");
			return;
		}
		send("day", Integer.toString(day));
	}

	public void handleNet(DatapackOpResultPayload payload) {
		if (payload == null) return;
		String path = payload.path() == null ? "" : payload.path();
		String extra = payload.extra() == null ? "" : payload.extra();
		if (payload.kind() == DatapackOps.KIND_ERR) {
			busy = false;
			if (!extra.isEmpty()) error(extra);
		} else if (payload.kind() == DatapackOps.KIND_CLOCK && read(extra) && !path.isEmpty()) {
			busy = false;
			if (path.equals(ClockOps.INSTALLED)) {
				setStatus(Component.translatable(path), OK);
				pollIn = 5;
			} else {
				error(path);
			}
		}
		rebuild();
	}

	private boolean read(String json) {
		try {
			JsonObject o = JsonParser.parseString(json).getAsJsonObject();
			installed = o.get("installed").getAsBoolean();
			ord = Mth.clamp(o.get("ord").getAsInt(), 1, 6);
			showD = o.get("showd").getAsInt() != 0;
			showT = o.get("showt").getAsInt() != 0;
			showS = o.get("shows").getAsInt() != 0;
			dayOff = o.get("dayoff").getAsInt();
			speed = o.get("speed").getAsInt();
			slow = o.get("slow").getAsInt();
			cycle = o.get("cycle").getAsBoolean();
			me = o.get("me").getAsBoolean();
			on = o.get("on").getAsInt();
			online = o.get("online").getAsInt();
			loaded = true;
			return true;
		} catch (RuntimeException e) {
			return false;
		}
	}

	@Override
	public void tick() {
		super.tick();
		if (busy && ++busyTicks > 300) {
			busy = false;
			error(K + "reload_failed");
		}
		if (!busy && --pollIn <= 0) requestState();
		rebuild();
	}

	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		super.renderBackground(g, mouseX, mouseY, partialTick);
		UiTheme.drawPanel(g, leftPos, topPos, panelW, panelH);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		super.render(g, mouseX, mouseY, partialTick);
		UiTheme.drawHeader(g, font, this.title, leftPos, topPos, panelW);
		if (loaded && installed) drawRows(g);
		else drawMissing(g);
		for (Btn b : buttons) drawButton(g, b, mouseX, mouseY);
		String text = status.getString();
		int color = statusColor;
		if (text.isEmpty() && loaded && installed) {
			text = I18n.get(K + "clock_hint");
			color = UiTheme.cMuted();
		}
		UiTheme.clipLabel(g, font, text, leftPos + PAD, topPos + panelH - 14, panelW - 2 * PAD, color);
	}

	private void drawRows(GuiGraphics g) {
		for (int i = 0; i < ROWS.length; i++) {
			g.drawString(font, I18n.get(K + ROWS[i]), leftPos + PAD, rowY + i * LINE + 4, UiTheme.cLabel(), false);
		}
		g.fill(contentX, rowY, contentX + contentW, rowY + BTN_H, UiTheme.cPreviewWell());
		g.drawCenteredString(font, preview(), contentX + contentW / 2, rowY + 4, 0xFFFFFFFF);
		note(g, I18n.get(K + "clock_hud_count", on, online), hudTextX, rowY + LINE + 4);
		note(g, I18n.get(K + "clock_day_now", day()), dayTextX, rowY + 3 * LINE + 4);
	}

	private void note(GuiGraphics g, String text, int x, int y) {
		int room = contentX + contentW - x;
		if (room >= 24) UiTheme.clipLabel(g, font, text, x, y, room, UiTheme.cMuted());
	}

	private void drawMissing(GuiGraphics g) {
		int cx = leftPos + panelW / 2;
		int y = topPos + panelH / 2 - 28;
		if (!loaded) {
			g.drawCenteredString(font, tr(EditorAccess.canSendDatapack() ? "clock_loading" : "need_mod"), cx, y + 14, UiTheme.cMuted());
			return;
		}
		g.drawCenteredString(font, tr("clock_missing"), cx, y, UiTheme.cLabel());
		g.drawCenteredString(font, tr("clock_missing_hint"), cx, y + 14, UiTheme.cMuted());
	}

	private void drawButton(GuiGraphics g, Btn b, int mouseX, int mouseY) {
		boolean hovered = b.active() && hit(mouseX, mouseY, b);
		if (!b.chip()) {
			UiTheme.drawButton(g, font, b.label(), b.x(), b.y(), b.w(), BTN_H, hovered, b.active());
			return;
		}
		int fill = b.on() ? (hovered ? CHIP_ON_HOVER : CHIP_ON) : (hovered ? CHIP_HOVER : CHIP);
		g.fill(b.x(), b.y(), b.x() + b.w(), b.y() + BTN_H, fill);
		g.drawCenteredString(font, b.label(), b.x() + b.w() / 2, b.y() + (BTN_H - 8) / 2, b.on() ? 0xFFFFFFFF : UiTheme.cLabel());
	}

	private static boolean hit(double mouseX, double mouseY, Btn b) {
		return mouseX >= b.x() && mouseX < b.x() + b.w() && mouseY >= b.y() && mouseY < b.y() + BTN_H;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == 0) {
			for (Btn b : buttons) {
				if (!b.active() || !hit(mouseX, mouseY, b)) continue;
				UiTheme.playClick();
				b.action().run();
				return true;
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if ((keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) && dayField.visible && dayField.isFocused()) {
			setDay();
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
