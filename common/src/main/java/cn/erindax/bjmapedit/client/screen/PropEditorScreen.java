package cn.erindax.bjmapedit.client.screen;

import cn.erindax.bjmapedit.client.DatapackNet;
import cn.erindax.bjmapedit.client.EditorAccess;
import cn.erindax.bjmapedit.client.widget.CommandSuggestionHost;
import cn.erindax.bjmapedit.client.widget.GiveCommands;
import cn.erindax.bjmapedit.client.widget.InvPicker;
import cn.erindax.bjmapedit.client.widget.ItemNames;
import cn.erindax.bjmapedit.client.widget.PropData;
import cn.erindax.bjmapedit.client.widget.PropPack;
import cn.erindax.bjmapedit.client.widget.SuggestionPopup;
import cn.erindax.bjmapedit.client.widget.UiTheme;
import cn.erindax.bjmapedit.networking.DatapackOps;
import cn.erindax.bjmapedit.networking.payload.DatapackOpPayload;
import cn.erindax.bjmapedit.networking.payload.DatapackOpResultPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class PropEditorScreen extends Screen implements CommandSuggestionHost {
	private static final String K = "screen.bj_mapedit.";
	private static final int PAD = 8;
	private static final int ROW_H = 12;
	private static final int LINE = 20;
	private static final int FIELD_H = 16;
	private static final int FOOTER_H = 26;
	private static final int LIST_W = 116;
	private static final int OK = 0xFF7EE787;
	private static final int ERR = 0xFFFF7B72;
	private static final int HINT = 0x6E7681;
	private static final Map<String, String> PARTICLE_KEYS = Map.ofEntries(
		Map.entry("end_rod", "prop_pt_end_rod"),
		Map.entry("soul_fire_flame", "prop_pt_soul_fire"),
		Map.entry("flame", "prop_pt_flame"),
		Map.entry("squid_ink", "prop_pt_squid_ink"),
		Map.entry("electric_spark", "prop_pt_spark"),
		Map.entry("sonic_boom", "prop_pt_sonic"),
		Map.entry("dragon_breath", "prop_pt_dragon"),
		Map.entry("portal", "prop_pt_portal"),
		Map.entry("reverse_portal", "prop_pt_rev_portal"),
		Map.entry("cherry_leaves", "prop_pt_cherry"),
		Map.entry("totem_of_undying", "prop_pt_totem"),
		Map.entry("heart", "prop_pt_heart"),
		Map.entry("enchant", "prop_pt_enchant"),
		Map.entry("witch", "prop_pt_witch"),
		Map.entry("sculk_soul", "prop_pt_sculk_soul"),
		Map.entry("sweep_attack", "prop_pt_sweep"),
		Map.entry("crit", "prop_pt_crit"),
		Map.entry("enchanted_hit", "prop_pt_magic_crit"),
		Map.entry("cloud", "prop_pt_cloud"),
		Map.entry("snowflake", "prop_pt_snow"),
		Map.entry("glow", "prop_pt_glow"),
		Map.entry("firework", "prop_pt_firework"),
		Map.entry("lava", "prop_pt_lava"),
		Map.entry("happy_villager", "prop_pt_emerald")
	);

	private record ParticleOption(String id, String path, String name, String nameLower) {
		String label() {
			return name.isEmpty() ? id : name + "  " + id;
		}
	}

	private static PropData draft;
	private static String generatedJson;
	private static WeakReference<Object> draftOwner = new WeakReference<>(null);
	private static int tokenSeq;

	private PropData prop;
	private List<String> made = new ArrayList<>();
	private boolean listed;
	private boolean opened;
	private boolean syncing;
	private boolean busy;
	private int busyTicks;
	private String token = "";
	private String failKey;
	private Runnable afterReload;
	private String pendingJson;
	private String pendingId;
	private String loadingPath;
	private String loadingId;
	private String deletingId;
	private String confirmDelete;
	private Component status = Component.empty();
	private int statusColor = OK;
	private int madeScroll;
	private int skillIdx;

	private int panelW;
	private int panelH;
	private int leftPos;
	private int topPos;
	private int bodyY;
	private int bodyH;
	private int step;
	private int listX;
	private int rightX;
	private int rightW;
	private int footerY;
	private int slotPreviewX;
	private int slotPreviewY;
	private int madeTop;
	private int madeRows;
	private int labelW;
	private int fieldX;
	private int genX;
	private int genW;

	private Button newBtn;
	private Button autoLoreBtn;
	private Button pvpBtn;
	private Button pickInvBtn;
	private Button skillAddBtn;
	private Button skillDelBtn;
	private Button consumeBtn;
	private Button pierceBtn;
	private Button giveBtn;
	private Button copyBtn;
	private EditBox idField;
	private EditBox itemField;
	private EditBox nameField;
	private EditBox chargeField;
	private EditBox cdField;
	private EditBox rangeField;
	private EditBox powerField;
	private EditBox particleField;
	private EditBox cmdField;
	private CommandSuggestions cmdSuggestions;

	private int interactX;
	private int interactY;
	private int interactW;
	private int chargeLabelX;
	private int effectX;
	private int effectY;
	private int effectW;
	private int cdLabelX;
	private int powerLabelX;
	private int projKindX;
	private int projKindY;
	private int projKindW;

	private final InvPicker invPicker = new InvPicker();
	private List<ItemNames.Entry> itemHits = List.of();
	private int itemIdx;
	private int itemScroll;
	private List<ParticleOption> allParticles;
	private String particleLang;
	private List<ParticleOption> particleHits = List.of();
	private int particleIdx;
	private int particleScroll;
	private int popupKind;
	private int restX = Integer.MIN_VALUE;
	private int restY;
	private boolean pointerMoved;

	public PropEditorScreen() {
		super(Component.translatable(K + "prop_title"));
		Object owner = Minecraft.getInstance().getConnection();
		if (draft == null || draftOwner.get() != owner) {
			draft = new PropData();
			draftOwner = new WeakReference<>(owner);
			generatedJson = null;
		}
		prop = draft;
	}

	@Override
	protected void init() {
		layout();
		newBtn = button("prop_new", this::newProp, FIELD_H);
		idField = field(24, "aj_id_hint");
		idField.setFilter(s -> s.matches("[a-z0-9_]{0,24}"));
		idField.setResponder(s -> {
			if (!syncing) prop.id = s;
		});
		autoLoreBtn = button("prop_lore_on", this::toggleAutoLore, FIELD_H);
		pvpBtn = button("prop_pvp_off", this::togglePvp, FIELD_H);
		itemField = field(96, "prop_item_hint");
		itemField.setResponder(s -> {
			if (syncing) return;
			prop.baseSnbt = "";
			prop.item = resolveItemInput(s.trim());
			updateItemHits();
		});
		pickInvBtn = button("prop_pick_inv", () -> invPicker.open(), FIELD_H);
		nameField = field(128, "prop_name_hint");
		nameField.setResponder(s -> {
			if (!syncing) prop.name = s;
		});

		skillAddBtn = button("aj_skill_add", this::addSkill, FIELD_H);
		skillDelBtn = button("aj_skill_del", this::deleteSkill, FIELD_H);
		chargeField = field(4, null);
		chargeField.setFilter(s -> s.matches(PropData.DIGITS_4));
		chargeField.setResponder(s -> {
			PropData.Skill sk = syncing ? null : curSkill();
			if (sk != null) sk.charge = s;
		});
		consumeBtn = button("prop_consume_off", this::toggleConsume, FIELD_H);
		cdField = field(5, null);
		cdField.setFilter(s -> s.matches(PropData.DIGITS_5));
		cdField.setResponder(s -> {
			PropData.Skill sk = syncing ? null : curSkill();
			if (sk != null) sk.cooldown = s;
		});
		rangeField = field(3, null);
		rangeField.setFilter(s -> s.matches(PropData.DIGITS_3));
		rangeField.setResponder(s -> {
			PropData.Skill sk = syncing ? null : curSkill();
			if (sk != null) sk.range = s;
		});
		powerField = field(4, null);
		powerField.setFilter(s -> s.matches(PropData.DIGITS_4));
		powerField.setResponder(s -> {
			PropData.Skill sk = syncing ? null : curSkill();
			if (sk != null) sk.power = s;
		});
		pierceBtn = button("prop_pierce_off", this::togglePierce, FIELD_H);
		particleField = field(96, "prop_particle_hint");
		particleField.setResponder(s -> {
			if (syncing) return;
			PropData.Skill sk = curSkill();
			if (sk != null) sk.particle = resolveParticleInput(s.trim());
			updateParticleHits();
		});
		cmdField = field(1024, "prop_cmd_hint");
		cmdSuggestions = new CommandSuggestions(this.minecraft, this, cmdField, font, true, false, 1, 7, false, 0xD0000000);
		cmdSuggestions.setAllowSuggestions(false);
		cmdSuggestions.setAllowHiding(false);
		cmdField.setResponder(s -> {
			if (!syncing) {
				PropData.Skill sk = curSkill();
				if (sk != null) sk.command = s;
			}
			refreshCmdSuggestions();
		});

		giveBtn = button("prop_give", this::giveProp, 20);
		copyBtn = button("aj_copy", this::copyCommand, 20);

		placeWidgets();
		syncAll();
		if (!opened) {
			opened = true;
			requestList();
		}
	}

	private EditBox field(int maxLength, String hintKey) {
		EditBox box = new EditBox(font, 0, 0, 40, FIELD_H, Component.empty());
		box.setMaxLength(maxLength);
		if (hintKey != null) box.setHint(Component.translatable(K + hintKey).withStyle(Style.EMPTY.withColor(HINT)));
		return this.addRenderableWidget(box);
	}

	private Button button(String key, Runnable action, int height) {
		return this.addRenderableWidget(Button.builder(Component.translatable(K + key), b -> action.run()).bounds(0, 0, 40, height).build());
	}

	private void layout() {
		panelW = Mth.clamp(this.width - 16, 400, 460);
		panelH = Mth.clamp(this.height - 8, 232, 298);
		leftPos = (this.width - panelW) / 2;
		topPos = Math.max(4, (this.height - panelH) / 2);
		bodyY = topPos + UiTheme.HEADER_H + 6;
		bodyH = panelH - UiTheme.HEADER_H - FOOTER_H - 10;
		step = bodyH < 194 ? 18 : LINE;
		listX = leftPos + PAD;
		rightX = listX + LIST_W + 10;
		rightW = leftPos + panelW - PAD - rightX;
		footerY = topPos + panelH - FOOTER_H + 3;
		slotPreviewX = listX;
		slotPreviewY = bodyY;
		madeTop = bodyY + 34;
		madeRows = Math.max(3, (bodyY + bodyH - 4 - madeTop) / ROW_H);
	}

	private void put(AbstractWidget widget, int x, int y, int w) {
		widget.setPosition(x, y);
		widget.setWidth(w);
	}

	private int widest(String... keys) {
		int w = 0;
		for (String key : keys) w = Math.max(w, font.width(I18n.get(K + key)));
		return w;
	}

	private void placeWidgets() {
		int x1 = rightX + rightW;
		put(newBtn, listX + 24, bodyY + 1, LIST_W - 24);
		labelW = widest("prop_id", "prop_item", "prop_name", "prop_skills", "prop_interact", "prop_effect", "prop_range", "prop_radius", "prop_particle", "aj_skill_cmd");
		fieldX = rightX + labelW + 6;
		int y = bodyY;
		int pvpW = widest("prop_pvp_on", "prop_pvp_off") + 12;
		int loreW = widest("prop_lore_on", "prop_lore_off") + 12;
		put(pvpBtn, x1 - pvpW, y, pvpW);
		put(autoLoreBtn, x1 - pvpW - 4 - loreW, y, loreW);
		put(idField, fieldX, y, autoLoreBtn.getX() - 4 - fieldX);
		y += step;
		int invW = widest("prop_pick_inv") + 12;
		put(pickInvBtn, x1 - invW, y, invW);
		put(itemField, fieldX, y, x1 - invW - 4 - fieldX);
		y += step;
		put(nameField, fieldX, y, x1 - fieldX);
		y += step;
		int delW = widest("aj_skill_del") + 12;
		int addW = widest("aj_skill_add") + 12;
		put(skillDelBtn, x1 - delW, y, delW);
		put(skillAddBtn, x1 - delW - 4 - addW, y, addW);
		y += step;
		PropData.Skill sk = curSkill();
		int conW = widest("prop_consume_on", "prop_consume_off") + 12;
		put(consumeBtn, x1 - conW, y, conW);
		boolean hasCh = sk != null && PropData.usesCharge(sk.interact);
		int unitW = font.width(I18n.get(K + "aj_unit_tick")) + 4;
		if (hasCh) {
			int chLw = widest("prop_charge");
			put(chargeField, consumeBtn.getX() - 4 - unitW - 30, y, 30);
			chargeLabelX = chargeField.getX() - 4 - chLw;
			interactX = fieldX;
			interactY = y;
			interactW = chargeLabelX - 6 - fieldX;
		} else {
			interactX = fieldX;
			interactY = y;
			interactW = consumeBtn.getX() - 4 - fieldX;
		}
		y += step;
		int cdLw = widest("prop_cd", "prop_interval");
		put(cdField, x1 - unitW - 36, y, 36);
		cdLabelX = cdField.getX() - 4 - cdLw;
		effectX = fieldX;
		effectY = y;
		effectW = cdLabelX - 6 - fieldX;
		y += step;
		put(rangeField, fieldX, y, 28);
		int pwrLw = widest("aj_skill_damage", "aj_skill_burst", "prop_heal_amt");
		powerLabelX = fieldX + 28 + 8;
		put(powerField, powerLabelX + pwrLw + 4, y, 32);
		int pierceW = widest("prop_pierce_on", "prop_pierce_off") + 12;
		put(pierceBtn, x1 - pierceW, y, pierceW);
		projKindW = widest("prop_pj_0", "prop_pj_1", "prop_pj_2", "prop_pj_3") + 12;
		projKindX = x1 - projKindW;
		projKindY = y;
		y += step;
		put(particleField, fieldX, y, x1 - fieldX);
		put(cmdField, fieldX, y, x1 - fieldX);

		int bx = leftPos + panelW - PAD;
		genW = widest("aj_generate") + 20;
		genX = bx - genW;
		bx = genX - 4;
		for (Button b : List.of(giveBtn, copyBtn)) {
			int w = font.width(b.getMessage()) + 12;
			put(b, bx - w, footerY, w);
			bx -= w + 4;
		}
	}

	private static void setText(EditBox box, String text) {
		if (box.getValue().equals(text)) return;
		box.setValue(text);
		box.moveCursorToStart(false);
	}

	private void syncAll() {
		skillIdx = Mth.clamp(skillIdx, 0, Math.max(0, prop.skills.size() - 1));
		PropData.Skill sk = curSkill();
		syncing = true;
		setText(idField, prop.id);
		setText(itemField, prop.item);
		setText(nameField, prop.name);
		if (sk != null) {
			setText(chargeField, sk.charge);
			setText(cdField, sk.cooldown);
			setText(rangeField, sk.range);
			setText(powerField, sk.power);
			setText(particleField, sk.particle);
			setText(cmdField, sk.command);
		}
		syncing = false;
		autoLoreBtn.setMessage(Component.translatable(K + (prop.autoLore ? "prop_lore_on" : "prop_lore_off")));
		pvpBtn.setMessage(Component.translatable(K + (prop.pvp ? "prop_pvp_on" : "prop_pvp_off")));
		if (sk != null) {
			consumeBtn.setMessage(Component.translatable(K + (sk.consume ? "prop_consume_on" : "prop_consume_off")));
			pierceBtn.setMessage(Component.translatable(K + (sk.pierce ? "prop_pierce_on" : "prop_pierce_off")));
		}
		updateVisibility();
	}

	private void updateVisibility() {
		PropData.Skill sk = curSkill();
		boolean hasSkill = sk != null;
		skillAddBtn.active = prop.skills.size() < PropData.MAX_SKILLS;
		skillDelBtn.active = prop.skills.size() > 1;
		chargeField.visible = hasSkill && PropData.usesCharge(sk.interact);
		consumeBtn.visible = hasSkill;
		cdField.visible = hasSkill;
		rangeField.visible = hasSkill;
		powerField.visible = hasSkill && sk.effect != PropData.EFFECT_CUSTOM;
		pierceBtn.visible = hasSkill && sk.effect == PropData.EFFECT_RING_LASER;
		particleField.visible = hasSkill && PropData.usesParticle(sk.effect);
		cmdField.visible = hasSkill && sk.effect == PropData.EFFECT_CUSTOM;
		placeWidgets();
		refreshCmdSuggestions();
	}

	private PropData.Skill curSkill() {
		return skillIdx >= 0 && skillIdx < prop.skills.size() ? prop.skills.get(skillIdx) : null;
	}

	private String resolveItemInput(String text) {
		for (ItemNames.Entry e : ItemNames.all()) {
			if (e.name().equalsIgnoreCase(text)) return e.idText();
		}
		return text;
	}

	private static int itemRank(ItemNames.Entry e, String q) {
		if (e.nameLower().equals(q)) return 0;
		if (e.nameLower().startsWith(q)) return 1;
		if (e.pathLower().equals(q) || e.idLower().equals(q)) return 2;
		return e.pathLower().startsWith(q) || e.idLower().startsWith(q) ? 3 : 4;
	}

	private void updateItemHits() {
		String q = itemField.getValue().trim().toLowerCase(Locale.ROOT);
		List<ItemNames.Entry> out = new ArrayList<>();
		for (ItemNames.Entry e : ItemNames.all()) {
			if (e.nameLower().contains(q) || e.idLower().contains(q)) out.add(e);
		}
		out.sort(Comparator.comparingInt((ItemNames.Entry e) -> itemRank(e, q)).thenComparing(ItemNames.Entry::name).thenComparing(ItemNames.Entry::idText));
		itemHits = out;
		itemIdx = 0;
		itemScroll = 0;
		popupKind = 1;
	}

	private boolean itemOpen() {
		return popupKind == 1 && itemField.visible && itemField.isFocused() && !itemHits.isEmpty();
	}

	private int itemPopY() {
		return SuggestionPopup.listY(itemField, itemHits.size(), this.height);
	}

	private int popW() {
		return rightX + rightW - fieldX;
	}

	private boolean overItemPop(double mouseX, double mouseY) {
		return itemOpen() && SuggestionPopup.contains(mouseX, mouseY, fieldX, itemPopY(), popW(), itemHits.size());
	}

	private void pickItemHit(int idx) {
		prop.baseSnbt = "";
		itemField.setValue(itemHits.get(idx).idText());
		itemHits = List.of();
		popupKind = 0;
	}

	private boolean clickItemPop(double mouseX, double mouseY) {
		if (!overItemPop(mouseX, mouseY)) return false;
		int total = itemHits.size();
		Integer bar = SuggestionPopup.pressBar(fieldX, itemPopY(), popW(), total, itemScroll, mouseX, mouseY);
		int idx = SuggestionPopup.hitIndex(mouseX, mouseY, fieldX, itemPopY(), popW(), total, itemScroll);
		if (bar != null) itemScroll = bar;
		else if (idx >= 0) {
			UiTheme.playClick();
			pickItemHit(idx);
		}
		return true;
	}

	private List<ParticleOption> particles() {
		String lang = Minecraft.getInstance().getLanguageManager().getSelected();
		if (allParticles != null && lang.equals(particleLang)) return allParticles;
		List<ParticleOption> out = new ArrayList<>();
		for (ResourceLocation id : BuiltInRegistries.PARTICLE_TYPE.keySet()) {
			if (!(BuiltInRegistries.PARTICLE_TYPE.getValue(id) instanceof SimpleParticleType)) continue;
			String key = PARTICLE_KEYS.get(id.getPath());
			String name = key != null ? I18n.get(K + key) : "";
			out.add(new ParticleOption(id.toString(), id.getPath(), name, name.toLowerCase(Locale.ROOT)));
		}
		out.sort(Comparator.comparing((ParticleOption o) -> o.name.isEmpty()).thenComparing(ParticleOption::id));
		allParticles = out;
		particleLang = lang;
		return allParticles;
	}

	private String resolveParticleInput(String text) {
		for (ParticleOption p : particles()) {
			if (!p.name.isEmpty() && p.name.equalsIgnoreCase(text)) return p.id;
			if (p.path.equalsIgnoreCase(text)) return p.id;
		}
		return text;
	}

	private void updateParticleHits() {
		String q = particleField.getValue().trim().toLowerCase(Locale.ROOT);
		List<ParticleOption> out = new ArrayList<>();
		for (ParticleOption p : particles()) {
			if (p.nameLower.contains(q) || p.id.contains(q)) out.add(p);
		}
		particleHits = out;
		particleIdx = 0;
		particleScroll = 0;
		popupKind = 2;
	}

	private boolean particleOpen() {
		return popupKind == 2 && particleField.visible && particleField.isFocused() && !particleHits.isEmpty();
	}

	private int particlePopY() {
		return SuggestionPopup.listY(particleField, particleHits.size(), this.height);
	}

	private boolean overParticlePop(double mouseX, double mouseY) {
		return particleOpen() && SuggestionPopup.contains(mouseX, mouseY, fieldX, particlePopY(), popW(), particleHits.size());
	}

	private void pickParticleHit(int idx) {
		particleField.setValue(particleHits.get(idx).id());
		particleHits = List.of();
		popupKind = 0;
	}

	private boolean clickParticlePop(double mouseX, double mouseY) {
		if (!overParticlePop(mouseX, mouseY)) return false;
		int total = particleHits.size();
		Integer bar = SuggestionPopup.pressBar(fieldX, particlePopY(), popW(), total, particleScroll, mouseX, mouseY);
		int idx = SuggestionPopup.hitIndex(mouseX, mouseY, fieldX, particlePopY(), popW(), total, particleScroll);
		if (bar != null) particleScroll = bar;
		else if (idx >= 0) {
			UiTheme.playClick();
			pickParticleHit(idx);
		}
		return true;
	}

	private boolean cmdModeActive() {
		PropData.Skill sk = curSkill();
		return sk != null && sk.effect == PropData.EFFECT_CUSTOM && cmdSuggestions != null && cmdField != null && cmdField.visible;
	}

	private void refreshCmdSuggestions() {
		if (cmdSuggestions == null) return;
		if (cmdModeActive()) {
			cmdSuggestions.setAllowSuggestions(true);
			try {
				cmdSuggestions.updateCommandInfo();
			} catch (Exception ignored) {}
			return;
		}
		cmdSuggestions.setAllowSuggestions(false);
		cmdSuggestions.hide();
		if (cmdField != null) cmdField.setSuggestion(null);
	}

	@Override
	public int[] placeCommandSuggestions(int x, int w, int count) {
		int listH = Math.max(1, count) * 12;
		w = Mth.clamp(w, 20, Math.max(20, this.width - 4));
		x = Mth.clamp(x, 2, Math.max(2, this.width - 2 - w));
		int below = cmdField.getY() + cmdField.getHeight() + 1;
		int y = below + listH <= this.height - 2 ? below : Math.max(2, cmdField.getY() - listH - 1);
		return new int[]{x, y, w};
	}

	@Override
	public void renderCommandUsage(GuiGraphics g, List<FormattedCharSequence> lines, int x, int w, int color, Font usageFont, EditBox box) {
		if (lines == null || lines.isEmpty() || box == null) return;
		int boxH = lines.size() * 12;
		w = Mth.clamp(w, 20, Math.max(20, this.width - 4));
		x = Mth.clamp(x, 2, Math.max(2, this.width - 2 - w));
		int y = box.getY() - boxH - 2;
		if (y < 2) y = box.getY() + box.getHeight() + 1;
		for (int i = 0; i < lines.size(); i++) {
			int yy = y + i * 12;
			g.fill(x - 1, yy, x + w, yy + 12, color);
			g.drawString(usageFont, lines.get(i), x, yy + 2, 0xFFE6EDF3);
		}
	}

	private void newProp() {
		confirmDelete = null;
		prop = new PropData();
		prop.id = suggestId("star_sword");
		draft = prop;
		generatedJson = null;
		skillIdx = 0;
		setStatus(Component.empty(), OK);
		syncAll();
	}

	private String suggestId(String base) {
		if (!made.contains(base)) return base;
		for (int i = 2; i < 100; i++) {
			if (!made.contains(base + "_" + i)) return base + "_" + i;
		}
		return "";
	}

	private void pickFromInventory(ItemStack stack) {
		String snbt = PropPack.cleanImportSnbt(stack);
		if (snbt.isEmpty()) return;
		prop.baseSnbt = snbt;
		prop.item = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
		setStatus(Component.translatable(K + "prop_imported"), OK);
		syncAll();
	}

	private void toggleAutoLore() {
		prop.autoLore = !prop.autoLore;
		syncAll();
	}

	private void togglePvp() {
		prop.pvp = !prop.pvp;
		syncAll();
	}

	private void addSkill() {
		if (prop.skills.size() >= PropData.MAX_SKILLS) {
			error(K + "aj_skills_full", PropData.MAX_SKILLS);
			return;
		}
		prop.skills.add(new PropData.Skill());
		skillIdx = prop.skills.size() - 1;
		setStatus(Component.empty(), OK);
		syncAll();
	}

	private void deleteSkill() {
		if (prop.skills.size() <= 1) return;
		prop.skills.remove(skillIdx);
		skillIdx = Mth.clamp(skillIdx, 0, prop.skills.size() - 1);
		setFocused(null);
		syncAll();
	}

	private void cycleInteract(int dir) {
		PropData.Skill sk = curSkill();
		if (sk == null) return;
		sk.applyInteractDefaults(sk.interact + dir);
		syncAll();
	}

	private void cycleEffect(int dir) {
		PropData.Skill sk = curSkill();
		if (sk == null) return;
		sk.applyEffectDefaults(sk.effect + dir);
		setFocused(null);
		syncAll();
	}

	private void cycleProjKind(int dir) {
		PropData.Skill sk = curSkill();
		if (sk == null) return;
		sk.projKind = Math.floorMod(sk.projKind + dir, PropData.PROJ_COUNT);
	}

	private void toggleConsume() {
		PropData.Skill sk = curSkill();
		if (sk == null) return;
		sk.consume = !sk.consume;
		syncAll();
	}

	private void togglePierce() {
		PropData.Skill sk = curSkill();
		if (sk == null) return;
		sk.pierce = !sk.pierce;
		syncAll();
	}

	private void setStatus(Component text, int color) {
		status = text;
		statusColor = color;
	}

	private void error(String key, Object... args) {
		setStatus(Component.translatable(key, args), ERR);
	}

	private static boolean hit(double mouseX, double mouseY, int x, int y, int w, int h) {
		return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
	}

	private static boolean over(AbstractWidget widget, double mouseX, double mouseY) {
		return widget.visible && hit(mouseX, mouseY, widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight());
	}

	private boolean ready() {
		if (busy) {
			error(K + "aj_busy");
			return false;
		}
		if (!EditorAccess.canSendDatapack()) {
			error(K + "need_mod");
			return false;
		}
		return true;
	}

	private boolean canCommand() {
		String problem = EditorAccess.commandProblem();
		if (problem == null) return true;
		error(problem);
		return false;
	}

	private void requestList() {
		if (!EditorAccess.canSendDatapack()) {
			error(K + "need_mod");
			return;
		}
		DatapackNet.send(DatapackOps.LIST, "", "");
	}

	private void generate(Runnable then) {
		if (!ready()) return;
		confirmDelete = null;
		String bad = PropPack.problem(prop);
		if (bad != null) {
			error(K + bad);
			return;
		}
		String json = prop.toJson();
		Set<String> ids = new TreeSet<>(made);
		ids.add(prop.id);
		Map<String, String> files = new LinkedHashMap<>(PropPack.globalFiles(ids));
		files.putAll(PropPack.propFiles(prop));
		files.put(PropPack.projectFile(prop.id), json);
		for (Map.Entry<String, String> e : files.entrySet()) {
			if (!DatapackNet.fits(e.getValue())
				|| !EditorAccess.fits(DatapackOpPayload.CODEC, new DatapackOpPayload(DatapackOps.WRITE, e.getKey(), e.getValue()))) {
				error(K + "too_large");
				return;
			}
		}
		busy = true;
		busyTicks = 0;
		failKey = null;
		afterReload = then;
		pendingJson = json;
		pendingId = prop.id;
		token = "bjprop#" + (++tokenSeq);
		for (Map.Entry<String, String> e : files.entrySet()) DatapackNet.send(DatapackOps.WRITE, e.getKey(), e.getValue());
		DatapackNet.send(DatapackOps.RELOAD, token, PropPack.PACK);
		setStatus(Component.translatable(K + "aj_generating"), UiTheme.cMuted());
	}

	private void finish(String errorKey) {
		busy = false;
		Runnable then = afterReload;
		afterReload = null;
		if (errorKey != null) {
			error(errorKey);
			return;
		}
		generatedJson = pendingJson;
		if (!made.contains(pendingId)) {
			made.add(pendingId);
			made.sort(null);
		}
		setStatus(Component.translatable(K + "aj_generated", pendingId), OK);
		if (then != null) then.run();
	}

	private void whenGenerated(Runnable action) {
		if (!ready()) return;
		if (prop.toJson().equals(generatedJson) && made.contains(prop.id)) action.run();
		else generate(action);
	}

	private void giveProp() {
		if (!canCommand()) return;
		String id = prop.id;
		whenGenerated(() -> {
			if (EditorAccess.sendCommands(List.of(PropPack.giveFunctionCommand(id)))) {
				setStatus(Component.translatable(K + "prop_given", id), OK);
			}
		});
	}

	private void copyCommand() {
		String id = prop.id;
		whenGenerated(() -> {
			GiveCommands.copy("/" + PropPack.giveFunctionCommand(id));
			setStatus(Component.translatable(K + "copied"), OK);
		});
	}

	private void loadProp(String id) {
		if (busy) {
			error(K + "aj_busy");
			return;
		}
		confirmDelete = null;
		loadingId = id;
		loadingPath = PropPack.projectFile(id);
		DatapackNet.send(DatapackOps.READ, loadingPath, "");
	}

	private void deleteProp(String id) {
		if (!ready()) return;
		if (!id.equals(confirmDelete)) {
			confirmDelete = id;
			error(K + "prop_delete_confirm", id);
			return;
		}
		confirmDelete = null;
		List<String> rest = new ArrayList<>(made);
		rest.remove(id);
		deletingId = id;
		if (rest.isEmpty()) {
			DatapackNet.send(DatapackOps.DELETE, PropPack.PACK, "");
		} else {
			for (Map.Entry<String, String> e : PropPack.globalFiles(rest).entrySet()) DatapackNet.send(DatapackOps.WRITE, e.getKey(), e.getValue());
			DatapackNet.send(DatapackOps.DELETE, PropPack.propDir(id), "");
			DatapackNet.send(DatapackOps.DELETE, PropPack.projectFile(id), "");
		}
		made = rest;
		madeScroll = Mth.clamp(madeScroll, 0, Math.max(0, made.size() - madeRows));
		if (id.equals(prop.id)) generatedJson = null;
	}

	public void handleNet(DatapackOpResultPayload payload) {
		if (payload == null) return;
		int kind = payload.kind();
		String path = payload.path() == null ? "" : payload.path();
		String extra = payload.extra() == null ? "" : payload.extra();
		if (kind == DatapackOps.KIND_TREE) {
			made = PropPack.propIds(extra);
			listed = true;
			if (!made.contains(prop.id)) generatedJson = null;
			madeScroll = Mth.clamp(madeScroll, 0, Math.max(0, made.size() - madeRows));
		} else if (kind == DatapackOps.KIND_FILE) {
			if (!path.equals(loadingPath)) return;
			loadingPath = null;
			PropData loaded = PropData.fromJson(extra);
			if (loaded == null) {
				error(K + "aj_load_failed", loadingId);
				return;
			}
			prop = loaded;
			draft = prop;
			skillIdx = 0;
			generatedJson = loaded.toJson();
			syncAll();
			setStatus(Component.translatable(K + "aj_loaded", loaded.id), OK);
		} else if (kind == DatapackOps.KIND_ERR) {
			String key = extra.isEmpty() ? K + "file_op_failed" : extra;
			if (loadingPath != null && path.equals(loadingPath)) {
				loadingPath = null;
				error(K + "aj_load_failed", loadingId);
			} else if (!busy) {
				deletingId = null;
				error(key);
				requestList();
			} else if (path.equals(token)) {
				finish(failKey != null ? failKey : key);
			} else if (failKey == null) {
				failKey = key;
			}
		} else if (kind == DatapackOps.KIND_OK) {
			if (busy && path.equals(token)) {
				finish(failKey);
			} else if (deletingId != null && (K + "deleted").equals(extra)) {
				setStatus(Component.translatable(K + "aj_deleted", deletingId), OK);
				deletingId = null;
			}
		}
	}

	@Override
	public void tick() {
		super.tick();
		if (busy && ++busyTicks > 300) finish(K + "reload_failed");
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
		int fy = topPos + panelH - FOOTER_H;
		g.fill(leftPos + 1, fy, leftPos + panelW - 1, fy + 1, 0xFF2B2C2D);
		drawLeftRail(g, mouseX, mouseY);
		drawForm(g, mouseX, mouseY);
		UiTheme.drawButton(g, font, Component.translatable(K + "aj_generate"), genX, footerY, genW, 20, hit(mouseX, mouseY, genX, footerY, genW, 20), true, true);
		drawStatus(g);
		if (itemOpen()) {
			UiTheme.pushOverlay(g);
			SuggestionPopup.draw(g, font, fieldX, itemPopY(), popW(), itemHits.size(), itemScroll, itemIdx, mouseX, mouseY, i -> itemHits.get(i).name() + "  " + itemHits.get(i).idText());
			UiTheme.popOverlay(g);
		} else if (particleOpen()) {
			UiTheme.pushOverlay(g);
			SuggestionPopup.draw(g, font, fieldX, particlePopY(), popW(), particleHits.size(), particleScroll, particleIdx, mouseX, mouseY, i -> particleHits.get(i).label());
			UiTheme.popOverlay(g);
		} else if (cmdModeActive()) {
			g.pose().pushPose();
			g.pose().translate(0.0F, 0.0F, 300.0F);
			cmdSuggestions.render(g, mouseX, mouseY);
			g.pose().popPose();
		}
		if (restX == Integer.MIN_VALUE) {
			restX = mouseX;
			restY = mouseY;
		}
		pointerMoved |= mouseX != restX || mouseY != restY;
		if (invPicker.isOpen()) invPicker.render(g, font, this.width, this.height, mouseX, mouseY);
		else if (pointerMoved && !overItemPop(mouseX, mouseY) && !overParticlePop(mouseX, mouseY)) drawTip(g, mouseX, mouseY);
	}

	private void drawLeftRail(GuiGraphics g, int mouseX, int mouseY) {
		UiTheme.drawSlot(g, slotPreviewX, slotPreviewY);
		ItemStack preview = PropPack.buildStack(prop);
		if (!preview.isEmpty()) g.renderItem(preview, slotPreviewX + 1, slotPreviewY + 1);
		int x0 = listX;
		int x1 = listX + LIST_W;
		int h = madeRows * ROW_H;
		UiTheme.clipLabel(g, font, I18n.get(K + "aj_made"), x0, madeTop - 10, LIST_W, UiTheme.cMuted());
		g.fill(x0, madeTop, x1, madeTop + h, UiTheme.cInputBg());
		if (made.isEmpty()) {
			UiTheme.clipLabel(g, font, I18n.get(K + (listed ? "aj_made_empty" : "aj_scanning")), x0 + 4, madeTop + 2, LIST_W - 8, UiTheme.cMuted());
			return;
		}
		for (int i = 0; i < madeRows && i + madeScroll < made.size(); i++) {
			String id = made.get(i + madeScroll);
			int y = madeTop + i * ROW_H;
			boolean hover = hit(mouseX, mouseY, x0, y, LIST_W, ROW_H);
			if (id.equals(prop.id)) g.fill(x0, y, x1, y + ROW_H, 0xFF21262D);
			else if (hover) g.fill(x0, y, x1, y + ROW_H, 0xFF191A1B);
			UiTheme.clipLabel(g, font, id, x0 + 4, y + 2, LIST_W - 22, UiTheme.cLabel());
			boolean del = hover && mouseX >= x1 - 14;
			g.drawString(font, "\u00d7", x1 - 10, y + 2, del || id.equals(confirmDelete) ? ERR : UiTheme.cMuted(), false);
		}
		UiTheme.drawThinScrollBar(g, x1 - 3, madeTop, h, madeScroll, Math.max(0, made.size() - madeRows));
	}

	private void label(GuiGraphics g, String key, int x, int y) {
		g.drawString(font, I18n.get(K + key), x, y + 4, UiTheme.cLabel(), false);
	}

	private void drawCycleBox(GuiGraphics g, int x, int y, int w, String text, int mouseX, int mouseY) {
		boolean hover = hit(mouseX, mouseY, x, y, w, FIELD_H);
		UiTheme.drawButton(g, font, Component.empty(), x, y, w, FIELD_H, hover, true);
		int textW = Math.min(font.width(text), w - 8);
		UiTheme.clipLabel(g, font, text, x + (w - textW) / 2, y + 4, Math.max(8, w - 8), UiTheme.cButtonText());
	}

	private void drawForm(GuiGraphics g, int mouseX, int mouseY) {
		label(g, "prop_id", rightX, idField.getY());
		label(g, "prop_item", rightX, itemField.getY());
		label(g, "prop_name", rightX, nameField.getY());
		int pillY = skillAddBtn.getY();
		label(g, "prop_skills", rightX, pillY);
		for (int i = 0; i < prop.skills.size(); i++) {
			int px = fieldX + i * 19;
			boolean sel = i == skillIdx;
			boolean hov = hit(mouseX, mouseY, px, pillY, 17, FIELD_H);
			int bg = sel ? UiTheme.cAccent() : hov ? UiTheme.cButtonHover() : UiTheme.cInputBg();
			g.fill(px, pillY, px + 17, pillY + FIELD_H, bg);
			g.drawCenteredString(font, String.valueOf(i + 1), px + 8, pillY + 4, sel ? 0xFFFFFFFF : UiTheme.cLabel());
		}
		PropData.Skill sk = curSkill();
		if (sk == null) return;
		label(g, "prop_interact", rightX, interactY);
		drawCycleBox(g, interactX, interactY, interactW, I18n.get(K + "prop_in_" + sk.interact), mouseX, mouseY);
		if (PropData.usesCharge(sk.interact)) {
			label(g, "prop_charge", chargeLabelX, interactY);
			g.drawString(font, I18n.get(K + "aj_unit_tick"), chargeField.getX() + chargeField.getWidth() + 2, interactY + 4, UiTheme.cMuted(), false);
		}
		label(g, "prop_effect", rightX, effectY);
		drawCycleBox(g, effectX, effectY, effectW, I18n.get(K + "prop_ef_" + sk.effect), mouseX, mouseY);
		label(g, sk.interact == PropData.INTERACT_RIGHT_HOLD ? "prop_interval" : "prop_cd", cdLabelX, effectY);
		g.drawString(font, I18n.get(K + "aj_unit_tick"), cdField.getX() + cdField.getWidth() + 2, effectY + 4, UiTheme.cMuted(), false);

		int rY = rangeField.getY();
		boolean radiusKind = sk.effect == PropData.EFFECT_NOVA || sk.effect == PropData.EFFECT_HEAL_SHIELD;
		label(g, radiusKind ? "prop_radius" : "prop_range", rightX, rY);
		if (sk.effect != PropData.EFFECT_CUSTOM) {
			String pKey = sk.effect == PropData.EFFECT_PROJECTILE ? "aj_skill_burst"
				: sk.effect == PropData.EFFECT_HEAL_SHIELD ? "prop_heal_amt" : "aj_skill_damage";
			label(g, pKey, powerLabelX, rY);
		}
		if (sk.effect == PropData.EFFECT_PROJECTILE) {
			drawCycleBox(g, projKindX, projKindY, projKindW, I18n.get(K + "prop_pj_" + sk.projKind), mouseX, mouseY);
		}
		if (sk.effect == PropData.EFFECT_CUSTOM) {
			label(g, "aj_skill_cmd", rightX, cmdField.getY());
		} else {
			label(g, "prop_particle", rightX, particleField.getY());
		}
		int descY = cmdField.getY() + step;
		List<FormattedCharSequence> lines = font.split(Component.translatable(K + "prop_ef_desc_" + sk.effect), rightW);
		int maxBottom = bodyY + bodyH - 12;
		for (int i = 0; i < lines.size() && descY + i * 11 + 9 <= maxBottom; i++) {
			g.drawString(font, lines.get(i), rightX, descY + i * 11 + 2, UiTheme.cMuted(), false);
		}
	}

	private void drawStatus(GuiGraphics g) {
		String text = status.getString();
		int color = statusColor;
		if (text.isEmpty()) {
			color = UiTheme.cMuted();
			text = I18n.get(K + (prop.baseSnbt.isEmpty() ? "prop_hint_default" : "prop_hint_imported"));
		}
		UiTheme.clipLabel(g, font, text, rightX, bodyY + bodyH - 9, rightW, color);
	}

	private void drawTip(GuiGraphics g, int mouseX, int mouseY) {
		if (hit(mouseX, mouseY, slotPreviewX, slotPreviewY, 18, 18)) {
			ItemStack preview = PropPack.buildStack(prop);
			List<Component> lines = new ArrayList<>();
			if (!preview.isEmpty()) lines.addAll(getTooltipFromItem(this.minecraft, preview));
			lines.add(Component.translatable(K + "prop_tip_slot").withStyle(ChatFormatting.GRAY));
			g.renderComponentTooltip(font, lines, mouseX, mouseY);
			return;
		}
		PropData.Skill sk = curSkill();
		if (sk != null && (hit(mouseX, mouseY, interactX, interactY, interactW, FIELD_H)
			|| hit(mouseX, mouseY, effectX, effectY, effectW, FIELD_H)
			|| sk.effect == PropData.EFFECT_PROJECTILE && hit(mouseX, mouseY, projKindX, projKindY, projKindW, FIELD_H))) {
			g.renderComponentTooltip(font, List.of(Component.translatable(K + "aj_tip_cycle")), mouseX, mouseY);
		}
	}

	private boolean pressed(double mouseX, double mouseY, int button) {
		if (button != 0 && button != 1) return false;
		if (button == 0 && hit(mouseX, mouseY, slotPreviewX, slotPreviewY, 18, 18)) {
			UiTheme.playClick();
			invPicker.open();
			return true;
		}
		int dir = button == 0 ? 1 : -1;
		PropData.Skill sk = curSkill();
		if (button == 0) {
			int pillY = skillAddBtn.getY();
			for (int i = 0; i < prop.skills.size(); i++) {
				int px = fieldX + i * 19;
				if (hit(mouseX, mouseY, px, pillY, 17, FIELD_H)) {
					UiTheme.playClick();
					skillIdx = i;
					setFocused(null);
					syncAll();
					return true;
				}
			}
		}
		if (sk != null) {
			if (hit(mouseX, mouseY, interactX, interactY, interactW, FIELD_H)) {
				UiTheme.playClick();
				cycleInteract(dir);
				return true;
			}
			if (hit(mouseX, mouseY, effectX, effectY, effectW, FIELD_H)) {
				UiTheme.playClick();
				cycleEffect(dir);
				return true;
			}
			if (sk.effect == PropData.EFFECT_PROJECTILE && hit(mouseX, mouseY, projKindX, projKindY, projKindW, FIELD_H)) {
				UiTheme.playClick();
				cycleProjKind(dir);
				return true;
			}
		}
		if (button != 0) return false;
		if (hit(mouseX, mouseY, genX, footerY, genW, 20)) {
			UiTheme.playClick();
			generate(null);
			return true;
		}
		if (hit(mouseX, mouseY, listX, madeTop, LIST_W, madeRows * ROW_H)) {
			int idx = (int) ((mouseY - madeTop) / ROW_H) + madeScroll;
			if (idx < 0 || idx >= made.size()) return true;
			UiTheme.playClick();
			if (mouseX >= listX + LIST_W - 14) deleteProp(made.get(idx));
			else loadProp(made.get(idx));
			return true;
		}
		return false;
	}

	private boolean scrolled(double mouseX, double mouseY, double scrollY) {
		if (scrollY == 0) return false;
		int dir = scrollY > 0 ? -1 : 1;
		if (hit(mouseX, mouseY, listX, madeTop, LIST_W, madeRows * ROW_H)) {
			madeScroll = Mth.clamp(madeScroll + dir, 0, Math.max(0, made.size() - madeRows));
			return true;
		}
		PropData.Skill sk = curSkill();
		if (sk != null) {
			if (hit(mouseX, mouseY, interactX, interactY, interactW, FIELD_H)) {
				cycleInteract(dir);
				return true;
			}
			if (hit(mouseX, mouseY, effectX, effectY, effectW, FIELD_H)) {
				cycleEffect(dir);
				return true;
			}
			if (sk.effect == PropData.EFFECT_PROJECTILE && hit(mouseX, mouseY, projKindX, projKindY, projKindW, FIELD_H)) {
				cycleProjKind(dir);
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (invPicker.isOpen()) {
			ItemStack picked = button == 0 ? invPicker.stackAt(this.width, this.height, mouseX, mouseY) : ItemStack.EMPTY;
			if (!picked.isEmpty()) {
				UiTheme.playClick();
				invPicker.close();
				pickFromInventory(picked);
			} else if (button == 1 || button == 0 && !invPicker.inPanel(this.width, this.height, mouseX, mouseY)) {
				invPicker.close();
			}
			return true;
		}
		if (button == 0 && (clickItemPop(mouseX, mouseY) || clickParticlePop(mouseX, mouseY))) return true;
		if (cmdModeActive() && cmdSuggestions.mouseClicked(mouseX, mouseY, button)) return true;
		boolean handled = pressed(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button);
		if (button == 0 && over(itemField, mouseX, mouseY)) {
			particleHits = List.of();
			updateItemHits();
		} else if (button == 0 && over(particleField, mouseX, mouseY)) {
			itemHits = List.of();
			updateParticleHits();
		} else {
			itemHits = List.of();
			particleHits = List.of();
			popupKind = 0;
		}
		if (cmdModeActive() && !over(cmdField, mouseX, mouseY)) cmdSuggestions.hide();
		return handled;
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		if (button == 0 && SuggestionPopup.isDragging()) {
			if (popupKind == 1) itemScroll = SuggestionPopup.dragTo(mouseY);
			else if (popupKind == 2) particleScroll = SuggestionPopup.dragTo(mouseY);
			return true;
		}
		return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (button == 0) SuggestionPopup.endDrag();
		return super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (invPicker.isOpen()) return true;
		if (overItemPop(mouseX, mouseY)) {
			itemScroll = SuggestionPopup.scrollBy(itemScroll, itemHits.size(), scrollY);
			return true;
		}
		if (overParticlePop(mouseX, mouseY)) {
			particleScroll = SuggestionPopup.scrollBy(particleScroll, particleHits.size(), scrollY);
			return true;
		}
		if (cmdModeActive() && cmdSuggestions.mouseScrolled(Mth.clamp(scrollY, -1.0, 1.0))) return true;
		if (scrolled(mouseX, mouseY, scrollY)) return true;
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (invPicker.isOpen()) {
			if (keyCode == GLFW.GLFW_KEY_ESCAPE) invPicker.close();
			return true;
		}
		if (itemOpen()) {
			if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
				itemHits = List.of();
				popupKind = 0;
				return true;
			}
			if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER || keyCode == GLFW.GLFW_KEY_TAB) {
				pickItemHit(itemIdx);
				return true;
			}
			if (keyCode == GLFW.GLFW_KEY_DOWN || keyCode == GLFW.GLFW_KEY_UP) {
				itemIdx = Mth.clamp(itemIdx + (keyCode == GLFW.GLFW_KEY_DOWN ? 1 : -1), 0, itemHits.size() - 1);
				itemScroll = SuggestionPopup.keepVisible(itemIdx, itemScroll, itemHits.size());
				return true;
			}
		}
		if (particleOpen()) {
			if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
				particleHits = List.of();
				popupKind = 0;
				return true;
			}
			if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER || keyCode == GLFW.GLFW_KEY_TAB) {
				pickParticleHit(particleIdx);
				return true;
			}
			if (keyCode == GLFW.GLFW_KEY_DOWN || keyCode == GLFW.GLFW_KEY_UP) {
				particleIdx = Mth.clamp(particleIdx + (keyCode == GLFW.GLFW_KEY_DOWN ? 1 : -1), 0, particleHits.size() - 1);
				particleScroll = SuggestionPopup.keepVisible(particleIdx, particleScroll, particleHits.size());
				return true;
			}
		}
		if (cmdModeActive() && cmdField.isFocused() && cmdSuggestions.keyPressed(keyCode, scanCode, modifiers)) return true;
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public boolean charTyped(char codePoint, int modifiers) {
		if (invPicker.isOpen()) return true;
		return super.charTyped(codePoint, modifiers);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
