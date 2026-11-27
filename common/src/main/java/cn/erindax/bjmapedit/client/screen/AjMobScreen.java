package cn.erindax.bjmapedit.client.screen;

import cn.erindax.bjmapedit.client.DatapackNet;
import cn.erindax.bjmapedit.client.EditorAccess;
import cn.erindax.bjmapedit.client.widget.AjData;
import cn.erindax.bjmapedit.client.widget.AjPack;
import cn.erindax.bjmapedit.client.widget.CommandSuggestionHost;
import cn.erindax.bjmapedit.client.widget.GiveCommands;
import cn.erindax.bjmapedit.client.widget.InvPicker;
import cn.erindax.bjmapedit.client.widget.ShopCompat;
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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class AjMobScreen extends Screen implements CommandSuggestionHost {
	private static final String K = "screen.bj_mapedit.";
	private static final int PAD = 8;
	private static final int ROW_H = 12;
	private static final int LINE = 20;
	private static final int FIELD_H = 16;
	private static final int FOOTER_H = 26;
	private static final int LIST_W = 124;
	private static final int TICK_W = 34;
	private static final int COUNT_W = 28;
	private static final int CHANCE_W = 36;
	private static final int DROP_ROWS = 9;
	private static final int PAGE_BASE = 0;
	private static final int PAGE_MELEE = 1;
	private static final int PAGE_SKILLS = 2;
	private static final int PAGE_BOSS = 3;
	private static final int PAGE_DROPS = 4;
	private static final int OK = 0xFF7EE787;
	private static final int ERR = 0xFFFF7B72;
	private static final int HINT = 0x6E7681;
	private static final String[] MODES = {"aj_mode_static", "aj_mode_passive", "aj_mode_hostile"};
	private static final String[] SLOTS = {"aj_idle", "aj_walk", "aj_attack", "aj_hurt", "aj_death"};
	private static final String[] LOOTS = {"aj_loot_follow", "aj_loot_none", "aj_loot_custom"};
	private static final String[] LABELS = {"aj_name", "aj_rig", "aj_carrier", "aj_health", "aj_nbt"};

	private record Carrier(String id, String path, String name, String nameLower) {
		String label() {
			return name + "  " + id;
		}
	}

	private record TransChoice(String name, String tag, int length, boolean onNew, String label) {}

	private static AjData draft;
	private static String generatedJson;
	private static WeakReference<Object> draftOwner = new WeakReference<>(null);
	private static int tokenSeq;

	private AjData model;
	private List<AjData.Rig> rigs = List.of();
	private List<String> made = new ArrayList<>();
	private boolean scanned;
	private boolean more;
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
	private int rigScroll;
	private int madeScroll;
	private int panelW;
	private int panelH;
	private int leftPos;
	private int topPos;
	private int bodyY;
	private int bodyH;
	private int step;
	private int formTop;
	private int listX;
	private int rightX;
	private int rightW;
	private int footerY;
	private int rigTop;
	private int rigRows;
	private int madeTop;
	private int madeRows;
	private int labelW;
	private int fieldX;
	private int animY;
	private int colW;
	private int genX;
	private int genW;
	private int page = PAGE_BASE;
	private int skillIdx;
	private int phaseIdx;
	private int dropScroll;
	private int dropRows;
	private int dropTop;
	private int dropMinX;
	private int dropMaxX;
	private int dropChanceX;
	private int dropDelX;
	private int pickIndex;
	private final int[] tabX = new int[4];
	private final int[] tabW = new int[4];
	private final int[] slotX = new int[AjData.SLOTS];
	private final int[] slotY = new int[AjData.SLOTS];
	private final int[] slotW = new int[AjData.SLOTS];
	private final EditBox[] tickFields = new EditBox[AjData.SLOTS];
	private final EditBox[] phaseTickFields = new EditBox[AjData.SLOTS];
	private final EditBox[][] dropFields = new EditBox[DROP_ROWS][3];
	private final InvPicker invPicker = new InvPicker();
	private final Map<String, ItemStack> stacks = new HashMap<>();
	private final Set<String> quietPaths = new HashSet<>();
	private EditBox idField;
	private EditBox carrierField;
	private EditBox scaleField;
	private EditBox healthField;
	private EditBox nbtField;
	private Button modeBtn;
	private Button variantBtn;
	private Button silentBtn;
	private Button lootBtn;
	private Button dropsBtn;
	private Button addBtn;
	private Button backBtn;
	private Button loopBtn;
	private Button summonBtn;
	private Button eggBtn;
	private Button copyBtn;
	private Button clearBtn;

	private Button meleeModeBtn;
	private Button meleeStopBtn;
	private EditBox meleeDmgField;
	private EditBox meleeRangeField;
	private EditBox meleeCdField;
	private EditBox meleeHitsField;
	private int mLabelW;
	private int mFieldX;
	private int meleeRangeLabelX;
	private int meleeCdLabelX;

	private Button skillAddBtn;
	private Button skillDelBtn;
	private EditBox skillValField;
	private EditBox skillRangeField;
	private EditBox skillPowerField;
	private EditBox skillWindupField;
	private EditBox skillAnimTicksField;
	private EditBox skillCmdField;
	private CommandSuggestions skillSuggestions;
	private int sLabelW;
	private int sFieldX;
	private int skillKindX;
	private int skillKindY;
	private int skillKindW;
	private int skillPhaseX;
	private int skillPhaseY;
	private int skillPhaseW;
	private int skillTrigX;
	private int skillTrigY;
	private int skillTrigW;
	private int skillPowerLabelX;
	private int skillWindupLabelX;
	private int skillAnimX;
	private int skillAnimY;
	private int skillAnimW;

	private Button bossBtn;
	private Button bossBarBtn;
	private Button phaseSubBtn;
	private Button phaseAddBtn;
	private Button phaseSwapBtn;
	private Button phaseVariantBtn;
	private EditBox bossNameField;
	private EditBox phaseHpField;
	private EditBox phaseTransTicksField;
	private EditBox phaseRigField;
	private int bLabelW;
	private int bFieldX;
	private int bossColorX;
	private int bossColorY;
	private int bossColorW;
	private int bossStyleX;
	private int bossStyleY;
	private int bossStyleW;
	private int phaseTabsX;
	private int phaseTabsY;
	private int phaseTransLabelX;
	private int phaseTransX;
	private int phaseTransY;
	private int phaseTransW;

	private int restX = Integer.MIN_VALUE;
	private int restY;
	private boolean pointerMoved;
	private List<Carrier> carriers;
	private List<Carrier> carrierHits = List.of();
	private int carrierIdx;
	private int carrierScroll;
	private List<AjData.Rig> phaseRigHits = List.of();
	private int phaseRigIdx;
	private int phaseRigScroll;
	private int popupKind;

	public AjMobScreen() {
		super(Component.translatable(K + "aj_title"));
		Object owner = Minecraft.getInstance().getConnection();
		if (draft == null || draftOwner.get() != owner) {
			draft = new AjData();
			draftOwner = new WeakReference<>(owner);
			generatedJson = null;
		}
		model = draft;
	}

	@Override
	protected void init() {
		layout();
		idField = field(24, "aj_id_hint");
		idField.setFilter(s -> s.matches("[a-z0-9_]{0,24}"));
		idField.setResponder(s -> {
			if (!syncing) model.id = s;
		});
		modeBtn = button(MODES[0], this::cycleMode, FIELD_H);
		variantBtn = button("aj_variant_default", this::cycleVariant, FIELD_H);
		carrierField = field(96, "aj_carrier_hint");
		carrierField.setResponder(s -> {
			if (syncing) return;
			model.carrier = carrierId(s.trim());
			updateCarrierHits();
		});
		scaleField = field(8, null);
		scaleField.setFilter(s -> s.matches("[0-9.]{0,8}"));
		scaleField.setResponder(s -> {
			if (!syncing) model.scale = s;
		});
		silentBtn = button("aj_silent_on", this::toggleSilent, FIELD_H);
		healthField = field(4, "aj_health_hint");
		healthField.setFilter(s -> s.matches(AjData.HEALTH));
		healthField.setResponder(s -> {
			if (!syncing) model.health = s;
		});
		lootBtn = button(LOOTS[0], this::cycleLoot, FIELD_H);
		dropsBtn = button("aj_drops_edit", () -> switchPage(PAGE_DROPS), FIELD_H);
		nbtField = field(2048, "aj_nbt_hint");
		nbtField.setResponder(s -> {
			if (!syncing) model.nbt = s;
		});
		for (int i = AjData.ATTACK; i < AjData.SLOTS; i++) {
			int slot = i;
			EditBox box = field(5, null);
			box.setFilter(s -> s.matches(AjData.DIGITS_5));
			box.setResponder(s -> {
				if (!syncing) {
					model.ticks[slot] = Mth.clamp(parse(s, AjData.defaultTicks(slot)), 1, AjData.MAX_TICKS);
					if (slot == AjData.ATTACK) updateMeleeHitsHint();
				}
			});
			tickFields[i] = box;

			EditBox pBox = field(5, null);
			pBox.setFilter(s -> s.matches(AjData.DIGITS_5));
			pBox.setResponder(s -> {
				AjData.Phase ph = syncing ? null : curPhase();
				if (ph != null) ph.ticks[slot] = Mth.clamp(parse(s, AjData.defaultTicks(slot)), 1, AjData.MAX_TICKS);
			});
			phaseTickFields[i] = pBox;
		}
		loopBtn = button("aj_loop_on", this::toggleLoop, FIELD_H);
		summonBtn = button("aj_summon", this::summon, 20);
		eggBtn = button("aj_egg", this::giveEgg, 20);
		copyBtn = button("aj_copy", this::copyCommand, 20);
		clearBtn = button("aj_clear", this::clearEntities, 20);
		addBtn = button("aj_drop_add", this::addDrop, FIELD_H);
		backBtn = button("done", () -> switchPage(PAGE_BASE), FIELD_H);
		for (int row = 0; row < DROP_ROWS; row++) {
			for (int col = 0; col < 3; col++) {
				int r = row;
				int c = col;
				EditBox box = field(c == 2 ? 6 : 3, null);
				box.setFilter(s -> s.matches(c == 2 ? AjData.CHANCE : AjData.COUNT));
				box.setResponder(s -> {
					AjData.Drop d = syncing ? null : dropAt(r);
					if (d == null) return;
					if (c == 0) d.min = s;
					else if (c == 1) d.max = s;
					else d.chance = s;
				});
				dropFields[row][col] = box;
			}
		}

		meleeModeBtn = button("aj_melee_sync", this::toggleMeleeMode, FIELD_H);
		meleeStopBtn = button("aj_melee_stop_on", this::toggleMeleeStop, FIELD_H);
		meleeDmgField = field(4, null);
		meleeDmgField.setFilter(s -> s.matches(AjData.HEALTH));
		meleeDmgField.setResponder(s -> {
			if (!syncing) model.meleeDamage = s;
		});
		meleeRangeField = field(5, null);
		meleeRangeField.setFilter(s -> s.matches(AjData.RANGE_DEC));
		meleeRangeField.setResponder(s -> {
			if (!syncing) model.meleeRange = s;
		});
		meleeCdField = field(5, null);
		meleeCdField.setFilter(s -> s.matches(AjData.DIGITS_5));
		meleeCdField.setResponder(s -> {
			if (!syncing) model.meleeCd = s;
		});
		meleeHitsField = field(32, null);
		meleeHitsField.setFilter(s -> s.matches(AjData.HIT_TICKS));
		meleeHitsField.setResponder(s -> {
			if (!syncing) model.meleeHits = s;
		});

		skillAddBtn = button("aj_skill_add", this::addSkill, FIELD_H);
		skillDelBtn = button("aj_skill_del", this::deleteSkill, FIELD_H);
		skillValField = field(5, null);
		skillValField.setFilter(s -> s.matches(AjData.DIGITS_5));
		skillValField.setResponder(s -> {
			AjData.Skill sk = syncing ? null : curSkill();
			if (sk != null) sk.value = s;
		});
		skillRangeField = field(3, null);
		skillRangeField.setFilter(s -> s.matches(AjData.COUNT));
		skillRangeField.setResponder(s -> {
			AjData.Skill sk = syncing ? null : curSkill();
			if (sk != null) sk.range = s;
		});
		skillPowerField = field(4, null);
		skillPowerField.setFilter(s -> s.matches(AjData.HEALTH));
		skillPowerField.setResponder(s -> {
			AjData.Skill sk = syncing ? null : curSkill();
			if (sk != null) sk.power = s;
		});
		skillWindupField = field(4, null);
		skillWindupField.setFilter(s -> s.matches(AjData.HEALTH));
		skillWindupField.setResponder(s -> {
			AjData.Skill sk = syncing ? null : curSkill();
			if (sk != null) sk.windup = s;
		});
		skillAnimTicksField = field(5, null);
		skillAnimTicksField.setFilter(s -> s.matches(AjData.DIGITS_5));
		skillAnimTicksField.setResponder(s -> {
			AjData.Skill sk = syncing ? null : curSkill();
			if (sk != null) sk.animTicks = Mth.clamp(parse(s, 20), 1, AjData.MAX_TICKS);
		});
		skillCmdField = field(1024, "aj_skill_cmd_hint");
		skillSuggestions = new CommandSuggestions(this.minecraft, this, skillCmdField, font, true, false, 1, 7, false, 0xD0000000);
		skillSuggestions.setAllowSuggestions(false);
		skillCmdField.setResponder(s -> {
			if (!syncing) {
				AjData.Skill sk = curSkill();
				if (sk != null) sk.command = s;
			}
			refreshSkillSuggestions();
		});

		bossBtn = button("aj_boss_off", this::toggleBoss, FIELD_H);
		bossBarBtn = button("aj_boss_bar_on", this::toggleBossBar, FIELD_H);
		bossNameField = field(128, "aj_boss_name_hint");
		bossNameField.setResponder(s -> {
			if (!syncing) model.bossName = s;
		});
		phaseSubBtn = this.addRenderableWidget(Button.builder(Component.literal("-"), b -> removePhase()).bounds(0, 0, 16, FIELD_H).build());
		phaseAddBtn = this.addRenderableWidget(Button.builder(Component.literal("+"), b -> addPhase()).bounds(0, 0, 16, FIELD_H).build());
		phaseHpField = field(2, null);
		phaseHpField.setFilter(s -> s.matches(AjData.DIGITS_2));
		phaseHpField.setResponder(s -> {
			AjData.Phase ph = syncing ? null : curPhase();
			if (ph != null) ph.hp = s;
		});
		phaseTransTicksField = field(5, null);
		phaseTransTicksField.setFilter(s -> s.matches(AjData.DIGITS_5));
		phaseTransTicksField.setResponder(s -> {
			AjData.Phase ph = syncing ? null : curPhase();
			if (ph != null) ph.transTicks = Mth.clamp(parse(s, 40), 1, AjData.MAX_TICKS);
		});
		phaseSwapBtn = button("aj_phase_swap_off", this::togglePhaseSwap, FIELD_H);
		phaseRigField = field(96, "aj_phase_rig_hint");
		phaseRigField.setResponder(s -> {
			if (syncing) return;
			AjData.Phase ph = curPhase();
			if (ph == null) return;
			String trimmed = s.trim();
			AjData.Rig matched = matchRigInput(trimmed);
			if (matched != null) {
				boolean changed = !matched.id().equals(ph.rig);
				ph.useRig(matched, changed);
			} else {
				ph.rig = trimmed;
			}
			updatePhaseRigHits();
			updateVariantVisibility();
		});
		phaseVariantBtn = button("aj_variant_default", this::cyclePhaseVariant, FIELD_H);

		placeWidgets();
		syncAll();
		if (!opened) {
			opened = true;
			requestScan();
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
		int rows = Math.max(4, (bodyH - 28) / ROW_H);
		rigRows = (rows + 1) / 2;
		madeRows = rows - rigRows;
		rigTop = bodyY + 12;
		madeTop = rigTop + rigRows * ROW_H + 16;
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
		int gap = 3;
		int fourth = (rightW - gap * 3) / 4;
		for (int i = 0; i < 4; i++) {
			tabX[i] = rightX + i * (fourth + gap);
			tabW[i] = i == 3 ? x1 - tabX[3] : fourth;
		}

		formTop = model.mob() ? bodyY + step : bodyY;
		labelW = Math.max(widest(LABELS), widest(SLOTS));
		fieldX = rightX + labelW + 6;
		int y = formTop;
		int modeW = widest(MODES) + 14;
		put(modeBtn, x1 - modeW, y, modeW);
		put(idField, fieldX, y, x1 - modeW - 6 - fieldX);
		y += step;
		put(variantBtn, x1 - 96, y, 96);
		y += step;
		int silentW = widest("aj_silent_on", "aj_silent_off") + 14;
		put(silentBtn, x1 - silentW, y, silentW);
		put(carrierField, fieldX, y, x1 - silentW - 6 - fieldX);
		y += step;
		int editW = font.width(I18n.get(K + "aj_drops_edit", 88)) + 14;
		int lootW = widest(LOOTS) + 14;
		put(dropsBtn, x1 - editW, y, editW);
		put(lootBtn, x1 - editW - 4 - lootW, y, lootW);
		put(healthField, fieldX, y, Math.min(96, lootBtn.getX() - 6 - fieldX));
		y += step;
		put(nbtField, fieldX, y, x1 - fieldX);
		y += step;
		animY = y;
		colW = (rightW - 10) / 2;

		int backW = widest("done") + 14;
		int addW = widest("aj_drop_add") + 14;
		put(backBtn, x1 - backW, bodyY, backW);
		put(addBtn, x1 - backW - 4 - addW, bodyY, addW);
		dropTop = bodyY + LINE + 12;
		dropRows = Mth.clamp((bodyY + bodyH - 12 - dropTop) / LINE, 1, DROP_ROWS);
		dropDelX = x1 - 16;
		dropChanceX = dropDelX - 10 - CHANCE_W;
		dropMaxX = dropChanceX - 6 - COUNT_W;
		dropMinX = dropMaxX - 10 - COUNT_W;
		for (int row = 0; row < DROP_ROWS; row++) {
			int rowY = dropTop + row * LINE + 2;
			put(dropFields[row][0], dropMinX, rowY, COUNT_W);
			put(dropFields[row][1], dropMaxX, rowY, COUNT_W);
			put(dropFields[row][2], dropChanceX, rowY, CHANCE_W);
		}

		placeMeleeWidgets(x1);
		placeSkillWidgets(x1);
		placeBossWidgets(x1);

		int bx = leftPos + panelW - PAD;
		genW = widest("aj_generate") + 20;
		genX = bx - genW;
		bx = genX - 4;
		for (Button b : List.of(summonBtn, eggBtn, copyBtn, clearBtn)) {
			int w = font.width(b.getMessage()) + 12;
			put(b, bx - w, footerY, w);
			bx -= w + 4;
		}
	}

	private void placeMeleeWidgets(int x1) {
		mLabelW = widest("aj_skill_damage", "aj_attack", "aj_melee_hits");
		mFieldX = rightX + mLabelW + 6;
		int y = bodyY + step;
		int modeW = widest("aj_melee_sync", "aj_melee_vanilla") + 12;
		int stopW = widest("aj_melee_stop_on", "aj_melee_stop_off") + 12;
		put(meleeModeBtn, rightX, y, modeW);
		put(meleeStopBtn, rightX + modeW + 4, y, stopW);
		y += step;
		put(meleeDmgField, mFieldX, y, 32);
		meleeRangeLabelX = mFieldX + 32 + 8;
		int rngLw = widest("aj_skill_range");
		put(meleeRangeField, meleeRangeLabelX + rngLw + 4, y, 34);
		int blockW = font.width(I18n.get(K + "aj_unit_block"));
		meleeCdLabelX = meleeRangeField.getX() + 34 + 3 + blockW + 8;
		int cdLw = widest("aj_melee_cd");
		put(meleeCdField, meleeCdLabelX + cdLw + 4, y, 36);
		y += 2 * step;
		put(meleeHitsField, mFieldX, y, x1 - mFieldX);
	}

	private void placeSkillWidgets(int x1) {
		sLabelW = widest("aj_skill_kind", "aj_skill_trig", "aj_skill_range", "aj_skill_anim", "aj_skill_cmd");
		sFieldX = rightX + sLabelW + 6;
		int y = bodyY + step;
		int delW = widest("aj_skill_del") + 12;
		int addW = widest("aj_skill_add") + 12;
		put(skillDelBtn, x1 - delW, y, delW);
		put(skillAddBtn, x1 - delW - 4 - addW, y, addW);
		y += step;
		boolean showPhase = model.boss && !model.phases.isEmpty();
		skillPhaseW = Math.max(font.width(I18n.get(K + "aj_skill_phase_all")), font.width(I18n.get(K + "aj_skill_phase_n", 6))) + 12;
		skillPhaseX = x1 - skillPhaseW;
		skillPhaseY = y;
		skillKindX = sFieldX;
		skillKindY = y;
		skillKindW = (showPhase ? skillPhaseX - 4 : x1) - skillKindX;
		y += step;
		skillTrigX = sFieldX;
		skillTrigY = y;
		skillTrigW = Math.min(116, rightW - sLabelW - 76);
		put(skillValField, skillTrigX + skillTrigW + 4, y, 44);
		y += step;
		put(skillRangeField, sFieldX, y, 28);
		int pwrLw = widest("aj_skill_damage", "aj_skill_burst");
		skillPowerLabelX = sFieldX + 28 + 8;
		put(skillPowerField, skillPowerLabelX + pwrLw + 4, y, 32);
		AjData.Skill sk = curSkill();
		boolean showPwr = sk != null && sk.kind != AjData.SKILL_CUSTOM;
		skillWindupLabelX = showPwr ? skillPowerField.getX() + 32 + 8 : sFieldX + 28 + 10;
		int winLw = widest("aj_skill_windup");
		put(skillWindupField, skillWindupLabelX + winLw + 4, y, 32);
		y += step;
		int unitW = font.width(I18n.get(K + "aj_unit_tick")) + 4;
		skillAnimX = sFieldX;
		skillAnimY = y;
		skillAnimW = x1 - unitW - TICK_W - 4 - skillAnimX;
		put(skillAnimTicksField, skillAnimX + skillAnimW + 4, y, TICK_W);
		y += step;
		put(skillCmdField, sFieldX, y, x1 - sFieldX);
	}

	private void placeBossWidgets(int x1) {
		bLabelW = Math.max(widest("aj_boss_name", "aj_phases", "aj_phase_hp", "aj_phase_swap"), widest(SLOTS));
		bFieldX = rightX + bLabelW + 6;
		int y = bodyY + step;
		int bossW = widest("aj_boss_on", "aj_boss_off") + 12;
		int barW = widest("aj_boss_bar_on", "aj_boss_bar_off") + 12;
		put(bossBtn, rightX, y, bossW);
		put(bossBarBtn, rightX + bossW + 4, y, barW);
		int styleW = widest("aj_bar_s_0", "aj_bar_s_1", "aj_bar_s_2", "aj_bar_s_3", "aj_bar_s_4") + 12;
		int colorW = widest("aj_bar_c_0", "aj_bar_c_1", "aj_bar_c_2", "aj_bar_c_3", "aj_bar_c_4", "aj_bar_c_5", "aj_bar_c_6") + 12;
		bossStyleW = styleW;
		bossStyleX = x1 - styleW;
		bossStyleY = y;
		bossColorW = colorW;
		bossColorX = bossStyleX - 4 - colorW;
		bossColorY = y;
		y += step;
		put(bossNameField, bFieldX, y, x1 - bFieldX);
		y += step;
		put(phaseSubBtn, bFieldX, y, 16);
		put(phaseAddBtn, bFieldX + 38, y, 16);
		phaseTabsX = bFieldX + 60;
		phaseTabsY = y;
		y += step;
		put(phaseHpField, bFieldX, y, 24);
		int pctW = font.width("%");
		phaseTransLabelX = bFieldX + 24 + pctW + 8;
		int trLw = widest("aj_phase_trans");
		phaseTransX = phaseTransLabelX + trLw + 4;
		phaseTransY = y;
		phaseTransW = x1 - TICK_W - 4 - phaseTransX;
		put(phaseTransTicksField, x1 - TICK_W, y, TICK_W);
		y += step;
		int swapW = widest("aj_phase_swap_on", "aj_phase_swap_off") + 12;
		put(phaseSwapBtn, bFieldX, y, swapW);
		AjData.Phase ph = curPhase();
		AjData.Rig pr = ph != null && ph.changeRig ? AjData.findRig(rigs, ph.rig) : null;
		boolean hasVar = pr != null && !pr.variants().isEmpty();
		int varW = 76;
		put(phaseVariantBtn, x1 - varW, y, varW);
		int rigX = bFieldX + swapW + 4;
		put(phaseRigField, rigX, y, (hasVar ? x1 - varW - 4 : x1) - rigX);
	}

	private void placeSlots() {
		if (page == PAGE_MELEE) {
			int unitW = font.width(I18n.get(K + "aj_unit_tick")) + 4;
			slotX[AjData.ATTACK] = mFieldX;
			slotY[AjData.ATTACK] = bodyY + 3 * step;
			slotW[AjData.ATTACK] = rightX + rightW - unitW - TICK_W - 4 - mFieldX;
			put(tickFields[AjData.ATTACK], slotX[AjData.ATTACK] + slotW[AjData.ATTACK] + 4, slotY[AjData.ATTACK], TICK_W);
			return;
		}
		if (page == PAGE_BOSS) {
			int top = bodyY + 6 * step;
			for (int i = 0; i < AjData.SLOTS; i++) {
				int col = i == AjData.WALK || i == AjData.HURT ? 1 : 0;
				int row = i <= AjData.WALK ? 0 : i <= AjData.HURT ? 1 : 2;
				boolean timed = i >= AjData.ATTACK;
				slotX[i] = rightX + col * (colW + 10) + bLabelW + 6;
				slotY[i] = top + row * step;
				slotW[i] = colW - bLabelW - 6 - (timed ? TICK_W + 4 : 0);
				if (timed) put(phaseTickFields[i], slotX[i] + slotW[i] + 4, slotY[i], TICK_W);
			}
			return;
		}
		int top = model.mob() ? animY : formTop + 2 * step;
		for (int i = 0; i < AjData.SLOTS; i++) {
			int col = i == AjData.WALK || i == AjData.HURT ? 1 : 0;
			int row = i <= AjData.WALK ? 0 : i <= AjData.HURT ? 1 : 2;
			boolean timed = i >= AjData.ATTACK;
			slotX[i] = rightX + col * (colW + 10) + labelW + 6;
			slotY[i] = top + row * step;
			slotW[i] = colW - labelW - 6 - (timed ? TICK_W + 4 : 0);
			if (timed) put(tickFields[i], slotX[i] + slotW[i] + 4, slotY[i], TICK_W);
		}
		put(loopBtn, rightX + colW + 10, model.mob() ? top + 2 * step : top, colW);
	}

	private void updateVariantVisibility() {
		AjData.Phase ph = curPhase();
		AjData.Rig pr = ph != null && ph.changeRig ? AjData.findRig(rigs, ph.rig) : null;
		boolean showVar = page == PAGE_BOSS && model.mob() && model.boss && ph != null && ph.changeRig && pr != null && !pr.variants().isEmpty();
		phaseVariantBtn.visible = showVar;
		if (page == PAGE_BOSS && ph != null) {
			phaseVariantBtn.setMessage(phaseVariantLabel(ph, pr));
			int x1 = rightX + rightW;
			int rigX = phaseSwapBtn.getX() + phaseSwapBtn.getWidth() + 4;
			put(phaseRigField, rigX, phaseSwapBtn.getY(), (showVar ? x1 - 76 - 4 : x1) - rigX);
		}
	}

	private void updateVisibility() {
		if (!model.mob()) page = PAGE_BASE;
		else if (page == PAGE_DROPS && model.loot != AjData.LOOT_CUSTOM) page = PAGE_BASE;

		boolean base = page == PAGE_BASE;
		boolean mobBase = base && model.mob();
		boolean meleePage = page == PAGE_MELEE && model.mob();
		boolean meleeHostile = meleePage && model.mode == AjData.HOSTILE;
		boolean meleeSyncOn = meleeHostile && model.meleeMode == AjData.MELEE_SYNC;
		boolean drops = page == PAGE_DROPS;
		boolean skills = page == PAGE_SKILLS && model.mob();
		boolean bossPage = page == PAGE_BOSS && model.mob();
		AjData.Rig rig = currentRig();

		idField.visible = base;
		modeBtn.visible = base;
		carrierField.visible = mobBase;
		scaleField.visible = false;
		silentBtn.visible = mobBase;
		healthField.visible = mobBase;
		lootBtn.visible = mobBase;
		dropsBtn.visible = mobBase;
		dropsBtn.active = model.loot == AjData.LOOT_CUSTOM;
		nbtField.visible = mobBase;
		for (int i = AjData.ATTACK; i < AjData.SLOTS; i++) {
			tickFields[i].visible = mobBase || (i == AjData.ATTACK && meleeSyncOn);
		}
		variantBtn.visible = base && rig != null && !rig.variants().isEmpty();
		loopBtn.visible = base;

		meleeModeBtn.visible = meleeHostile;
		meleeStopBtn.visible = meleeSyncOn;
		meleeDmgField.visible = meleeSyncOn;
		meleeRangeField.visible = meleeSyncOn;
		meleeCdField.visible = meleeSyncOn;
		meleeHitsField.visible = meleeSyncOn;

		addBtn.visible = drops;
		backBtn.visible = drops;
		for (int row = 0; row < DROP_ROWS; row++) {
			boolean shown = drops && dropAt(row) != null;
			for (EditBox box : dropFields[row]) box.visible = shown;
		}

		AjData.Skill sk = curSkill();
		boolean hasSkill = skills && sk != null;
		skillAddBtn.visible = skills;
		skillAddBtn.active = model.skills.size() < AjData.MAX_SKILLS;
		skillDelBtn.visible = skills;
		skillDelBtn.active = hasSkill;
		skillValField.visible = hasSkill;
		skillRangeField.visible = hasSkill;
		skillPowerField.visible = hasSkill && sk.kind != AjData.SKILL_CUSTOM;
		skillWindupField.visible = hasSkill;
		skillAnimTicksField.visible = hasSkill && !sk.anim.isEmpty();
		skillCmdField.visible = hasSkill && sk.kind == AjData.SKILL_CUSTOM;

		AjData.Phase ph = curPhase();
		boolean bossOn = bossPage && model.boss;
		boolean hasPhase = bossOn && ph != null;
		boolean hasSwap = hasPhase && ph.changeRig;
		bossBtn.visible = bossPage;
		bossBarBtn.visible = bossOn;
		bossNameField.visible = bossOn;
		bossNameField.setEditable(model.bossBar);
		phaseSubBtn.visible = bossOn;
		phaseSubBtn.active = !model.phases.isEmpty();
		phaseAddBtn.visible = bossOn;
		phaseAddBtn.active = model.phases.size() < AjData.MAX_TRANSITIONS;
		phaseHpField.visible = hasPhase;
		phaseTransTicksField.visible = hasPhase && !ph.transAnim.isEmpty();
		phaseSwapBtn.visible = hasPhase;
		phaseRigField.visible = hasSwap;
		for (int i = AjData.ATTACK; i < AjData.SLOTS; i++) phaseTickFields[i].visible = hasSwap;

		placeWidgets();
		updateVariantVisibility();
		placeSlots();
		refreshSkillSuggestions();
	}

	private static void setText(EditBox box, String text) {
		if (box.getValue().equals(text)) return;
		box.setValue(text);
		box.moveCursorToStart();
	}

	private void syncAll() {
		if (!model.mob()) page = PAGE_BASE;
		if (page == PAGE_DROPS && (!model.mob() || model.loot != AjData.LOOT_CUSTOM)) page = PAGE_BASE;
		skillIdx = Mth.clamp(skillIdx, 0, Math.max(0, model.skills.size() - 1));
		phaseIdx = Mth.clamp(phaseIdx, 0, Math.max(0, model.phases.size() - 1));
		dropScroll = Mth.clamp(dropScroll, 0, Math.max(0, model.drops.size() - dropRows));

		syncing = true;
		setText(idField, model.id);
		setText(carrierField, model.carrier);
		setText(scaleField, model.scale);
		setText(healthField, model.health);
		setText(nbtField, model.nbt);
		setText(meleeDmgField, model.meleeDamage);
		setText(meleeRangeField, model.meleeRange);
		setText(meleeCdField, model.meleeCd);
		setText(meleeHitsField, model.meleeHits);
		for (int i = AjData.ATTACK; i < AjData.SLOTS; i++) setText(tickFields[i], String.valueOf(model.ticks[i]));
		for (int row = 0; row < DROP_ROWS; row++) {
			AjData.Drop d = dropAt(row);
			if (d == null) continue;
			setText(dropFields[row][0], d.min);
			setText(dropFields[row][1], d.max);
			setText(dropFields[row][2], d.chance);
		}
		AjData.Skill sk = curSkill();
		if (sk != null) {
			setText(skillValField, sk.value);
			setText(skillRangeField, sk.range);
			setText(skillPowerField, sk.power);
			setText(skillWindupField, sk.windup);
			setText(skillAnimTicksField, String.valueOf(sk.animTicks));
			setText(skillCmdField, sk.command);
		}
		setText(bossNameField, model.bossName);
		AjData.Phase ph = curPhase();
		if (ph != null) {
			setText(phaseHpField, ph.hp);
			setText(phaseTransTicksField, String.valueOf(ph.transTicks));
			setText(phaseRigField, ph.rig);
			for (int i = AjData.ATTACK; i < AjData.SLOTS; i++) setText(phaseTickFields[i], String.valueOf(ph.ticks[i]));
		}
		syncing = false;

		modeBtn.setMessage(Component.translatable(K + MODES[model.mode]));
		silentBtn.setMessage(Component.translatable(K + (model.silent ? "aj_silent_on" : "aj_silent_off")));
		lootBtn.setMessage(Component.translatable(K + LOOTS[model.loot]));
		dropsBtn.setMessage(Component.translatable(K + "aj_drops_edit", model.drops.size()));
		loopBtn.setMessage(Component.translatable(K + (model.loop ? "aj_loop_on" : "aj_loop_off")));
		meleeModeBtn.setMessage(Component.translatable(K + (model.meleeMode == AjData.MELEE_SYNC ? "aj_melee_sync" : "aj_melee_vanilla")));
		meleeStopBtn.setMessage(Component.translatable(K + (model.meleeStop ? "aj_melee_stop_on" : "aj_melee_stop_off")));
		updateMeleeHitsHint();
		variantBtn.setMessage(variantLabel());
		bossBtn.setMessage(Component.translatable(K + (model.boss ? "aj_boss_on" : "aj_boss_off")));
		bossBarBtn.setMessage(Component.translatable(K + (model.bossBar ? "aj_boss_bar_on" : "aj_boss_bar_off")));
		if (ph != null) phaseSwapBtn.setMessage(Component.translatable(K + (ph.changeRig ? "aj_phase_swap_on" : "aj_phase_swap_off")));
		updateVisibility();
	}

	private void switchPage(int next) {
		page = next;
		dropScroll = 0;
		carrierHits = List.of();
		phaseRigHits = List.of();
		popupKind = 0;
		invPicker.close();
		setFocused(null);
		setStatus(Component.empty(), OK);
		syncAll();
	}

	private AjData.Skill curSkill() {
		return skillIdx >= 0 && skillIdx < model.skills.size() ? model.skills.get(skillIdx) : null;
	}

	private AjData.Phase curPhase() {
		return phaseIdx >= 0 && phaseIdx < model.phases.size() ? model.phases.get(phaseIdx) : null;
	}

	private Component variantLabel() {
		AjData.Rig rig = currentRig();
		if (rig != null) {
			for (AjData.Variant v : rig.variants()) {
				if (v.function().equals(model.variant)) return Component.translatable(K + "aj_variant", v.name());
			}
		}
		return Component.translatable(K + "aj_variant_default");
	}

	private Component phaseVariantLabel(AjData.Phase ph, AjData.Rig rig) {
		if (rig != null) {
			for (AjData.Variant v : rig.variants()) {
				if (v.function().equals(ph.variant)) return Component.translatable(K + "aj_variant", v.name());
			}
		}
		return Component.translatable(K + "aj_variant_default");
	}

	private AjData.Rig currentRig() {
		return AjData.findRig(rigs, model.rig);
	}

	private AjData.Rig phaseRig() {
		AjData.Phase ph = curPhase();
		return ph != null && ph.changeRig ? AjData.findRig(rigs, ph.rig) : null;
	}

	private AjData.Rig skillRig(AjData.Skill s) {
		if (s == null) return currentRig();
		String targetId = model.boss && s.phase >= 1 ? model.rigForPhase(s.phase - 1) : model.rig;
		return AjData.findRig(rigs, targetId);
	}

	private boolean slotShown(int slot) {
		if (page == PAGE_BASE) return slot == AjData.IDLE || model.mob();
		if (page == PAGE_MELEE) return slot == AjData.ATTACK && model.mob() && model.mode == AjData.HOSTILE && model.meleeMode == AjData.MELEE_SYNC;
		if (page == PAGE_BOSS) {
			AjData.Phase ph = curPhase();
			return model.mob() && model.boss && ph != null && ph.changeRig;
		}
		return false;
	}

	private void updateMeleeHitsHint() {
		if (meleeHitsField == null) return;
		int autoTick = AjData.autoHitTick(model.ticks[AjData.ATTACK]);
		meleeHitsField.setHint(Component.translatable(K + "aj_melee_hits_hint", autoTick).withStyle(Style.EMPTY.withColor(HINT)));
	}

	private void toggleMeleeMode() {
		model.meleeMode = model.meleeMode == AjData.MELEE_SYNC ? AjData.MELEE_VANILLA : AjData.MELEE_SYNC;
		syncAll();
	}

	private void toggleMeleeStop() {
		model.meleeStop = !model.meleeStop;
		syncAll();
	}

	private AjData.Drop dropAt(int row) {
		int idx = dropScroll + row;
		return row < dropRows && idx < model.drops.size() ? model.drops.get(idx) : null;
	}

	private ItemStack stack(AjData.Drop d) {
		return stacks.computeIfAbsent(d.item, ShopCompat::stackFromSnbt);
	}

	private static boolean over(AbstractWidget widget, double mouseX, double mouseY) {
		return widget.visible && hit(mouseX, mouseY, widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight());
	}

	private List<Carrier> carriers() {
		if (carriers != null) return carriers;
		carriers = new ArrayList<>();
		for (ResourceLocation id : BuiltInRegistries.ENTITY_TYPE.keySet()) {
			EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(id);
			if (!type.canSummon()) continue;
			String name = type.getDescription().getString().trim();
			carriers.add(new Carrier(id.toString(), id.getPath(), name, name.toLowerCase(Locale.ROOT)));
		}
		return carriers;
	}

	private String carrierId(String text) {
		for (Carrier c : carriers()) if (c.name().equalsIgnoreCase(text)) return c.id();
		return text;
	}

	private static int carrierRank(Carrier c, String q) {
		if (c.nameLower().equals(q)) return 0;
		if (c.nameLower().startsWith(q)) return 1;
		if (c.path().equals(q) || c.id().equals(q)) return 2;
		return c.path().startsWith(q) || c.id().startsWith(q) ? 3 : 4;
	}

	private void updateCarrierHits() {
		String q = carrierField.getValue().trim().toLowerCase(Locale.ROOT);
		List<Carrier> out = new ArrayList<>();
		for (Carrier c : carriers()) {
			if (c.nameLower().contains(q) || c.id().contains(q)) out.add(c);
		}
		out.sort(Comparator.comparingInt((Carrier c) -> carrierRank(c, q)).thenComparing(Carrier::name).thenComparing(Carrier::id));
		carrierHits = out;
		carrierIdx = 0;
		carrierScroll = 0;
		popupKind = 1;
	}

	private boolean carrierOpen() {
		return popupKind == 1 && carrierField.visible && carrierField.isFocused() && !carrierHits.isEmpty();
	}

	private int carrierPopW() {
		return rightX + rightW - fieldX;
	}

	private int carrierPopY() {
		return SuggestionPopup.listY(carrierField, carrierHits.size(), this.height);
	}

	private boolean overCarrierPop(double mouseX, double mouseY) {
		return carrierOpen() && SuggestionPopup.contains(mouseX, mouseY, fieldX, carrierPopY(), carrierPopW(), carrierHits.size());
	}

	private void pickCarrier(int idx) {
		carrierField.setValue(carrierHits.get(idx).id());
		carrierHits = List.of();
		popupKind = 0;
	}

	private boolean clickCarrierPop(double mouseX, double mouseY) {
		if (!overCarrierPop(mouseX, mouseY)) return false;
		int total = carrierHits.size();
		Integer bar = SuggestionPopup.pressBar(fieldX, carrierPopY(), carrierPopW(), total, carrierScroll, mouseX, mouseY);
		int idx = SuggestionPopup.hitIndex(mouseX, mouseY, fieldX, carrierPopY(), carrierPopW(), total, carrierScroll);
		if (bar != null) {
			carrierScroll = bar;
		} else if (idx >= 0) {
			UiTheme.playClick();
			pickCarrier(idx);
		}
		return true;
	}

	private AjData.Rig matchRigInput(String q) {
		if (q.isEmpty()) return null;
		for (AjData.Rig r : rigs) {
			if (r.id().equals(q) || shortName(r.id()).equals(q)) return r;
		}
		return null;
	}

	private void updatePhaseRigHits() {
		String q = phaseRigField.getValue().trim().toLowerCase(Locale.ROOT);
		List<AjData.Rig> out = new ArrayList<>();
		for (AjData.Rig r : rigs) {
			String s = shortName(r.id()).toLowerCase(Locale.ROOT);
			if (s.contains(q) || r.id().toLowerCase(Locale.ROOT).contains(q)) out.add(r);
		}
		phaseRigHits = out;
		phaseRigIdx = 0;
		phaseRigScroll = 0;
		popupKind = 2;
	}

	private boolean phaseRigOpen() {
		return popupKind == 2 && phaseRigField.visible && phaseRigField.isFocused() && !phaseRigHits.isEmpty();
	}

	private int phaseRigPopX() {
		return phaseRigField.getX();
	}

	private int phaseRigPopW() {
		return rightX + rightW - phaseRigPopX();
	}

	private int phaseRigPopY() {
		return SuggestionPopup.listY(phaseRigField, phaseRigHits.size(), this.height);
	}

	private boolean overPhaseRigPop(double mouseX, double mouseY) {
		return phaseRigOpen() && SuggestionPopup.contains(mouseX, mouseY, phaseRigPopX(), phaseRigPopY(), phaseRigPopW(), phaseRigHits.size());
	}

	private void pickPhaseRig(int idx) {
		AjData.Rig picked = phaseRigHits.get(idx);
		AjData.Phase ph = curPhase();
		if (ph != null) {
			boolean changed = !picked.id().equals(ph.rig);
			ph.useRig(picked, changed);
		}
		phaseRigHits = List.of();
		popupKind = 0;
		syncAll();
	}

	private boolean clickPhaseRigPop(double mouseX, double mouseY) {
		if (!overPhaseRigPop(mouseX, mouseY)) return false;
		int total = phaseRigHits.size();
		Integer bar = SuggestionPopup.pressBar(phaseRigPopX(), phaseRigPopY(), phaseRigPopW(), total, phaseRigScroll, mouseX, mouseY);
		int idx = SuggestionPopup.hitIndex(mouseX, mouseY, phaseRigPopX(), phaseRigPopY(), phaseRigPopW(), total, phaseRigScroll);
		if (bar != null) {
			phaseRigScroll = bar;
		} else if (idx >= 0) {
			UiTheme.playClick();
			pickPhaseRig(idx);
		}
		return true;
	}

	private boolean skillCommandActive() {
		AjData.Skill sk = curSkill();
		return page == PAGE_SKILLS && model.mob() && sk != null && sk.kind == AjData.SKILL_CUSTOM && skillSuggestions != null && skillCmdField != null && skillCmdField.visible;
	}

	private void refreshSkillSuggestions() {
		if (skillSuggestions == null) return;
		if (skillCommandActive()) {
			skillSuggestions.setAllowSuggestions(true);
			try {
				skillSuggestions.updateCommandInfo();
			} catch (Exception ignored) {}
			return;
		}
		skillSuggestions.setAllowSuggestions(false);
		skillSuggestions.hide();
		if (skillCmdField != null) skillCmdField.setSuggestion(null);
	}

	@Override
	public int[] placeCommandSuggestions(int x, int w, int count) {
		int listH = Math.max(1, count) * 12;
		w = Mth.clamp(w, 20, Math.max(20, this.width - 4));
		x = Mth.clamp(x, 2, Math.max(2, this.width - 2 - w));
		int below = skillCmdField.getY() + skillCmdField.getHeight() + 1;
		int y = below + listH <= this.height - 2 ? below : Math.max(2, skillCmdField.getY() - listH - 1);
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

	private void addSkill() {
		if (model.skills.size() >= AjData.MAX_SKILLS) {
			error(K + "aj_skills_full", AjData.MAX_SKILLS);
			return;
		}
		AjData.Skill sk = new AjData.Skill();
		model.skills.add(sk);
		skillIdx = model.skills.size() - 1;
		setStatus(Component.empty(), OK);
		syncAll();
	}

	private void deleteSkill() {
		AjData.Skill sk = curSkill();
		if (sk == null) return;
		model.skills.remove(skillIdx);
		skillIdx = Mth.clamp(skillIdx, 0, Math.max(0, model.skills.size() - 1));
		setFocused(null);
		syncAll();
	}

	private void cycleSkillKind(int dir) {
		AjData.Skill sk = curSkill();
		if (sk == null) return;
		sk.applyKindDefaults(sk.kind + dir);
		setFocused(null);
		syncAll();
	}

	private void cycleSkillPhase(int dir) {
		AjData.Skill sk = curSkill();
		if (sk == null || !model.boss || model.phases.isEmpty()) return;
		int total = model.phaseCount() + 1;
		sk.phase = Math.floorMod(sk.phase + dir, total);
		model.syncRigs(rigs);
		syncAll();
	}

	private void cycleSkillTrigger(int dir) {
		AjData.Skill sk = curSkill();
		if (sk == null) return;
		sk.applyTriggerDefaults(sk.trigger + dir);
		syncAll();
	}

	private void cycleSkillAnim(int dir) {
		AjData.Skill sk = curSkill();
		if (sk == null) return;
		AjData.Rig rig = skillRig(sk);
		if (rig == null) {
			error(K + "aj_no_rig");
			return;
		}
		List<AjData.Anim> anims = rig.anims();
		int idx = -1;
		for (int i = 0; i < anims.size(); i++) if (anims.get(i).name().equals(sk.anim)) idx = i;
		int next = Math.floorMod(idx + 1 + dir, anims.size() + 1) - 1;
		if (next < 0) {
			sk.anim = "";
		} else {
			AjData.Anim a = anims.get(next);
			sk.anim = a.name();
			sk.animTicks = a.length() > 0 ? Math.min(AjData.MAX_TICKS, a.length()) : 20;
		}
		syncAll();
	}

	private void toggleBoss() {
		model.boss = !model.boss;
		if (!model.boss) {
			for (AjData.Skill s : model.skills) s.phase = 0;
		}
		syncAll();
	}

	private void toggleBossBar() {
		model.bossBar = !model.bossBar;
		syncAll();
	}

	private void cycleBossColor(int dir) {
		if (!model.bossBar) return;
		model.bossColor = Math.floorMod(model.bossColor + dir, AjData.BOSS_COLORS.length);
	}

	private void cycleBossStyle(int dir) {
		if (!model.bossBar) return;
		model.bossStyle = Math.floorMod(model.bossStyle + dir, AjData.BOSS_STYLES.length);
	}

	private void addPhase() {
		if (model.phases.size() >= AjData.MAX_TRANSITIONS) return;
		AjData.Phase ph = new AjData.Phase();
		if (model.phases.isEmpty()) {
			ph.hp = "50";
		} else {
			int prev = AjPack.intRange(model.phases.get(model.phases.size() - 1).hp, 1, 99);
			ph.hp = String.valueOf(prev > 1 ? Math.max(1, prev / 2) : 1);
		}
		model.phases.add(ph);
		phaseIdx = model.phases.size() - 1;
		syncAll();
	}

	private void removePhase() {
		if (model.phases.isEmpty()) return;
		model.phases.remove(model.phases.size() - 1);
		phaseIdx = Mth.clamp(phaseIdx, 0, Math.max(0, model.phases.size() - 1));
		model.syncRigs(rigs);
		syncAll();
	}

	private void togglePhaseSwap() {
		AjData.Phase ph = curPhase();
		if (ph == null) return;
		ph.changeRig = !ph.changeRig;
		if (ph.changeRig && ph.rig.isEmpty() && !rigs.isEmpty()) {
			AjData.Rig def = currentRig() != null ? currentRig() : rigs.get(0);
			ph.useRig(def, true);
		}
		if (!ph.changeRig) ph.transOnNew = false;
		model.syncRigs(rigs);
		syncAll();
	}

	private void cyclePhaseVariant() {
		AjData.Phase ph = curPhase();
		AjData.Rig rig = phaseRig();
		if (ph == null || rig == null || rig.variants().isEmpty()) return;
		List<AjData.Variant> variants = rig.variants();
		int idx = -1;
		for (int i = 0; i < variants.size(); i++) if (variants.get(i).function().equals(ph.variant)) idx = i;
		ph.variant = idx + 1 >= variants.size() ? "" : variants.get(idx + 1).function();
		syncAll();
	}

	private List<TransChoice> transChoices(AjData.Phase ph) {
		List<TransChoice> out = new ArrayList<>();
		out.add(new TransChoice("", "", 40, false, I18n.get(K + "aj_none")));
		AjData.Rig prev = AjData.findRig(rigs, model.rigForPhase(phaseIdx));
		if (prev != null) {
			for (AjData.Anim a : prev.anims()) {
				String lbl = ph.changeRig ? I18n.get(K + "aj_phase_old", a.name()) : a.name();
				out.add(new TransChoice(a.name(), a.tag(), a.length(), false, lbl));
			}
		}
		if (ph.changeRig) {
			AjData.Rig next = AjData.findRig(rigs, ph.rig);
			if (next != null) {
				for (AjData.Anim a : next.anims()) {
					out.add(new TransChoice(a.name(), a.tag(), a.length(), true, I18n.get(K + "aj_phase_new", a.name())));
				}
			}
		}
		return out;
	}

	private String transLabel(AjData.Phase ph) {
		if (ph.transAnim.isEmpty()) return I18n.get(K + "aj_none");
		if (!ph.changeRig) return ph.transAnim;
		return I18n.get(K + (ph.transOnNew ? "aj_phase_new" : "aj_phase_old"), ph.transAnim);
	}

	private void cyclePhaseTrans(int dir) {
		AjData.Phase ph = curPhase();
		if (ph == null) return;
		List<TransChoice> choices = transChoices(ph);
		if (choices.size() <= 1) {
			error(K + "aj_no_rig");
			return;
		}
		int cur = 0;
		for (int i = 1; i < choices.size(); i++) {
			TransChoice c = choices.get(i);
			if (c.name().equals(ph.transAnim) && (!ph.changeRig || c.onNew() == ph.transOnNew)) {
				cur = i;
				break;
			}
		}
		TransChoice picked = choices.get(Math.floorMod(cur + dir, choices.size()));
		ph.transAnim = picked.name();
		ph.transTag = picked.tag();
		ph.transOnNew = picked.onNew();
		if (!picked.name().isEmpty()) ph.transTicks = picked.length() > 0 ? Math.min(AjData.MAX_TICKS, picked.length()) : 40;
		syncAll();
	}

	private void cycleLoot() {
		model.loot = (model.loot + 1) % LOOTS.length;
		syncAll();
	}

	private void addDrop() {
		if (model.drops.size() >= AjData.MAX_DROPS) {
			error(K + "aj_drops_full", AjData.MAX_DROPS);
			return;
		}
		pickIndex = model.drops.size();
		invPicker.open();
	}

	private void pick(ItemStack stack) {
		String snbt = ShopCompat.stackSnbt(stack);
		if (snbt.isEmpty()) {
			error(K + "aj_drop_invalid");
			return;
		}
		if (pickIndex < model.drops.size()) {
			model.drops.get(pickIndex).item = snbt;
		} else {
			AjData.Drop d = new AjData.Drop();
			d.item = snbt;
			model.drops.add(d);
			dropScroll = model.drops.size();
		}
		setStatus(Component.empty(), OK);
		syncAll();
	}

	private void removeDrop(int idx) {
		model.drops.remove(idx);
		setFocused(null);
		syncAll();
	}

	private void setStatus(Component text, int color) {
		status = text;
		statusColor = color;
	}

	private void error(String key, Object... args) {
		setStatus(Component.translatable(key, args), ERR);
	}

	private static int parse(String text, int fallback) {
		try {
			return Integer.parseInt(text.trim());
		} catch (NumberFormatException e) {
			return fallback;
		}
	}

	private static boolean hit(double mouseX, double mouseY, int x, int y, int w, int h) {
		return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
	}

	private void cycleMode() {
		model.mode = (model.mode + 1) % MODES.length;
		boolean preset = model.carrier.isBlank();
		for (String carrier : AjData.CARRIERS) preset |= carrier.equals(model.carrier);
		if (model.mob() && preset) model.carrier = AjData.CARRIERS[model.mode];
		syncAll();
	}

	private void cycleVariant() {
		AjData.Rig rig = currentRig();
		if (rig == null || rig.variants().isEmpty()) return;
		List<AjData.Variant> variants = rig.variants();
		int idx = -1;
		for (int i = 0; i < variants.size(); i++) if (variants.get(i).function().equals(model.variant)) idx = i;
		model.variant = idx + 1 >= variants.size() ? "" : variants.get(idx + 1).function();
		syncAll();
	}

	private void toggleSilent() {
		model.silent = !model.silent;
		syncAll();
	}

	private void toggleLoop() {
		model.loop = !model.loop;
		syncAll();
	}

	private void cycleAnim(int slot, int dir) {
		if (page == PAGE_BOSS) {
			AjData.Phase ph = curPhase();
			AjData.Rig rig = phaseRig();
			if (ph == null || rig == null) {
				error(K + "aj_bad_phase_rig");
				return;
			}
			List<AjData.Anim> anims = rig.anims();
			int idx = -1;
			for (int i = 0; i < anims.size(); i++) if (anims.get(i).name().equals(ph.anims[slot])) idx = i;
			int next = Math.floorMod(idx + 1 + dir, anims.size() + 1) - 1;
			ph.setAnim(slot, next < 0 ? null : anims.get(next));
			syncAll();
			return;
		}
		AjData.Rig rig = currentRig();
		if (rig == null) {
			error(K + "aj_no_rig");
			return;
		}
		List<AjData.Anim> anims = rig.anims();
		int idx = -1;
		for (int i = 0; i < anims.size(); i++) if (anims.get(i).name().equals(model.anims[slot])) idx = i;
		int next = Math.floorMod(idx + 1 + dir, anims.size() + 1) - 1;
		model.setAnim(slot, next < 0 ? null : anims.get(next));
		syncAll();
	}

	private void selectRig(AjData.Rig rig) {
		confirmDelete = null;
		if (page == PAGE_BOSS) {
			AjData.Phase ph = curPhase();
			if (model.boss && ph != null && ph.changeRig) {
				boolean changed = !rig.id().equals(ph.rig);
				ph.useRig(rig, changed);
				setStatus(Component.empty(), OK);
				syncAll();
				return;
			}
		}
		if (!rig.id().equals(model.rig)) {
			model = new AjData();
			draft = model;
			generatedJson = null;
			page = PAGE_BASE;
			model.use(rig);
			for (int slot = 0; slot < AjData.SLOTS; slot++) {
				for (String word : AjData.GUESS[slot]) {
					for (AjData.Anim a : rig.anims()) {
						if (model.anims[slot].isEmpty() && a.name().contains(word)) model.setAnim(slot, a);
					}
				}
			}
			model.id = suggestId(rig.id());
		} else {
			model.syncRigs(rigs);
		}
		setStatus(Component.empty(), OK);
		syncAll();
	}

	private String suggestId(String rigId) {
		String name = rigId.substring(Math.max(rigId.lastIndexOf(':'), rigId.lastIndexOf('/')) + 1).replaceAll("[^a-z0-9_]", "_");
		if (name.length() > 21) name = name.substring(0, 21);
		if (!AjData.validId(name)) return "";
		if (!made.contains(name)) return name;
		for (int i = 2; i < 100; i++) {
			if (!made.contains(name + "_" + i)) return name + "_" + i;
		}
		return "";
	}

	private void requestScan() {
		if (!EditorAccess.canSendDatapack()) {
			error(K + "need_mod");
			return;
		}
		DatapackNet.send(DatapackOps.AJ_SCAN, "", "");
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

	private void generate(Runnable then) {
		if (!ready()) return;
		confirmDelete = null;
		String problem = AjPack.problem(model);
		if (problem != null) {
			error(K + problem, model.rig);
			return;
		}
		if (scanned && !more) {
			String miss = AjPack.missingRig(model, rigs);
			if (miss != null) {
				error(K + "aj_rig_missing", miss);
				return;
			}
		}
		model.syncRigs(rigs);
		String json = model.toJson();
		Set<String> ids = new TreeSet<>(made);
		ids.add(model.id);
		Map<String, String> files = new LinkedHashMap<>(AjPack.globalFiles(ids));
		files.putAll(AjPack.modelFiles(model, rigs, r -> I18n.get(K + "aj_missing", r)));
		files.put(AjPack.projectFile(model.id), json);
		for (Map.Entry<String, String> e : files.entrySet()) {
			if (!DatapackNet.fits(e.getValue())
				|| !EditorAccess.fitsServerbound(new DatapackOpPayload(DatapackOps.WRITE, e.getKey(), e.getValue()))) {
				error(K + "too_large");
				return;
			}
		}
		busy = true;
		busyTicks = 0;
		failKey = null;
		afterReload = then;
		pendingJson = json;
		pendingId = model.id;
		token = "bjaj#" + (++tokenSeq);
		for (Map.Entry<String, String> e : files.entrySet()) DatapackNet.send(DatapackOps.WRITE, e.getKey(), e.getValue());
		quietPaths.remove(AjPack.lootFile(model.id));
		DatapackNet.send(DatapackOps.RELOAD, token, AjPack.PACK);
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
		if (model.toJson().equals(generatedJson) && made.contains(model.id)) action.run();
		else generate(action);
	}

	private void summon() {
		if (!canCommand()) return;
		String id = model.id;
		whenGenerated(() -> {
			if (EditorAccess.sendCommands(List.of(AjPack.summonAhead(id)))) setStatus(Component.translatable(K + "aj_summoned", id), OK);
		});
	}

	private void giveEgg() {
		if (!canCommand()) return;
		String id = model.id;
		whenGenerated(() -> {
			if (EditorAccess.sendCommands(List.of(AjPack.eggCommand(id)))) setStatus(Component.translatable(K + "aj_egg_given", id), OK);
		});
	}

	private void copyCommand() {
		String id = model.id;
		whenGenerated(() -> {
			GiveCommands.copy("/" + AjPack.summonCommand(id));
			setStatus(Component.translatable(K + "copied"), OK);
		});
	}

	private void clearEntities() {
		if (!ready() || !canCommand()) return;
		if (!made.contains(model.id)) {
			error(K + "aj_need_generate");
			return;
		}
		if (EditorAccess.sendCommands(List.of(AjPack.removeCommand(model.id)))) setStatus(Component.translatable(K + "aj_cleared", model.id), OK);
	}

	private void loadModel(String id) {
		if (busy) {
			error(K + "aj_busy");
			return;
		}
		confirmDelete = null;
		loadingId = id;
		loadingPath = AjPack.projectFile(id);
		DatapackNet.send(DatapackOps.READ, loadingPath, "");
	}

	private void deleteModel(String id) {
		if (!ready()) return;
		if (!id.equals(confirmDelete)) {
			confirmDelete = id;
			error(K + "aj_delete_confirm", id);
			return;
		}
		confirmDelete = null;
		if (EditorAccess.commandProblem() == null) EditorAccess.sendCommands(List.of(AjPack.removeCommand(id)));
		List<String> rest = new ArrayList<>(made);
		rest.remove(id);
		deletingId = id;
		if (rest.isEmpty()) {
			DatapackNet.send(DatapackOps.DELETE, AjPack.PACK, "");
		} else {
			for (Map.Entry<String, String> e : AjPack.globalFiles(rest).entrySet()) DatapackNet.send(DatapackOps.WRITE, e.getKey(), e.getValue());
			DatapackNet.send(DatapackOps.DELETE, AjPack.modelDir(id), "");
			quietPaths.add(AjPack.lootFile(id));
			DatapackNet.send(DatapackOps.DELETE, AjPack.lootFile(id), "");
			DatapackNet.send(DatapackOps.DELETE, AjPack.projectFile(id), "");
		}
		made = rest;
		madeScroll = Mth.clamp(madeScroll, 0, Math.max(0, made.size() - madeRows));
		if (id.equals(model.id)) generatedJson = null;
	}

	public void handleNet(DatapackOpResultPayload payload) {
		if (payload == null) return;
		int kind = payload.kind();
		String path = payload.path() == null ? "" : payload.path();
		String extra = payload.extra() == null ? "" : payload.extra();
		if (kind == DatapackOps.KIND_AJ) {
			AjData.Scan scan = AjData.parseScan(extra);
			rigs = scan.rigs();
			made = new ArrayList<>(scan.made());
			more = scan.more();
			scanned = true;
			model.syncRigs(rigs);
			if (!made.contains(model.id)) generatedJson = null;
			rigScroll = Mth.clamp(rigScroll, 0, Math.max(0, rigs.size() - rigRows));
			madeScroll = Mth.clamp(madeScroll, 0, Math.max(0, made.size() - madeRows));
			syncAll();
		} else if (kind == DatapackOps.KIND_FILE) {
			if (!path.equals(loadingPath)) return;
			loadingPath = null;
			AjData loaded = AjData.fromJson(extra);
			if (loaded == null) {
				error(K + "aj_load_failed", loadingId);
				return;
			}
			model = loaded;
			draft = model;
			page = PAGE_BASE;
			skillIdx = 0;
			phaseIdx = 0;
			generatedJson = loaded.toJson();
			model.syncRigs(rigs);
			syncAll();
			setStatus(Component.translatable(K + "aj_loaded", loaded.id), OK);
		} else if (kind == DatapackOps.KIND_ERR) {
			if (quietPaths.remove(path)) return;
			String key = extra.isEmpty() ? K + "file_op_failed" : extra;
			if (loadingPath != null && path.equals(loadingPath)) {
				loadingPath = null;
				error(K + "aj_load_failed", loadingId);
			} else if (!busy) {
				deletingId = null;
				error(key);
				requestScan();
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
	public void renderBackground(GuiGraphics g) {
		super.renderBackground(g);
		UiTheme.drawPanel(g, leftPos, topPos, panelW, panelH);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		this.renderBackground(g);
		super.render(g, mouseX, mouseY, partialTick);
		UiTheme.drawHeader(g, font, this.title, leftPos, topPos, panelW);
		int fy = topPos + panelH - FOOTER_H;
		g.fill(leftPos + 1, fy, leftPos + panelW - 1, fy + 1, 0xFF2B2C2D);
		drawRigs(g, mouseX, mouseY);
		drawMade(g, mouseX, mouseY);
		if (model.mob() && page != PAGE_DROPS) drawTabs(g, mouseX, mouseY);
		if (page == PAGE_DROPS) drawDrops(g, mouseX, mouseY);
		else if (page == PAGE_MELEE && model.mob()) drawMelee(g, mouseX, mouseY);
		else if (page == PAGE_SKILLS && model.mob()) drawSkills(g, mouseX, mouseY);
		else if (page == PAGE_BOSS && model.mob()) drawBoss(g, mouseX, mouseY);
		else drawForm(g, mouseX, mouseY);
		UiTheme.drawButton(g, font, Component.translatable(K + "aj_generate"), genX, footerY, genW, 20, hit(mouseX, mouseY, genX, footerY, genW, 20), true, true);
		drawStatus(g);
		if (carrierOpen()) {
			UiTheme.pushOverlay(g);
			SuggestionPopup.draw(g, font, fieldX, carrierPopY(), carrierPopW(), carrierHits.size(), carrierScroll, carrierIdx, mouseX, mouseY, i -> carrierHits.get(i).label());
			UiTheme.popOverlay(g);
		} else if (phaseRigOpen()) {
			UiTheme.pushOverlay(g);
			SuggestionPopup.draw(g, font, phaseRigPopX(), phaseRigPopY(), phaseRigPopW(), phaseRigHits.size(), phaseRigScroll, phaseRigIdx, mouseX, mouseY, i -> shortName(phaseRigHits.get(i).id()) + "  " + phaseRigHits.get(i).id());
			UiTheme.popOverlay(g);
		} else if (skillCommandActive()) {
			g.pose().pushPose();
			g.pose().translate(0.0F, 0.0F, 300.0F);
			skillSuggestions.render(g, mouseX, mouseY);
			g.pose().popPose();
		}
		if (restX == Integer.MIN_VALUE) {
			restX = mouseX;
			restY = mouseY;
		}
		pointerMoved |= mouseX != restX || mouseY != restY;
		if (invPicker.isOpen()) invPicker.render(g, font, this.width, this.height, mouseX, mouseY);
		else if (pointerMoved && !overCarrierPop(mouseX, mouseY) && !overPhaseRigPop(mouseX, mouseY)) drawTip(g, mouseX, mouseY);
	}

	private void drawTabs(GuiGraphics g, int mouseX, int mouseY) {
		String[] labels = {
			I18n.get(K + "aj_tab_base"),
			I18n.get(K + "aj_tab_melee"),
			I18n.get(K + "aj_tab_skills", model.skills.size()),
			model.boss ? I18n.get(K + "aj_tab_boss_on", model.phaseCount()) : I18n.get(K + "aj_tab_boss_off")
		};
		for (int i = 0; i < 4; i++) {
			boolean selected = page == i;
			boolean hover = hit(mouseX, mouseY, tabX[i], bodyY, tabW[i], FIELD_H);
			int bg = selected ? UiTheme.cAccent() : hover ? UiTheme.cButtonHover() : UiTheme.cInputBg();
			g.fill(tabX[i], bodyY, tabX[i] + tabW[i], bodyY + FIELD_H, bg);
			int tw = Math.min(font.width(labels[i]), tabW[i] - 4);
			UiTheme.clipLabel(g, font, labels[i], tabX[i] + (tabW[i] - tw) / 2, bodyY + 4, Math.max(8, tabW[i] - 4), selected ? 0xFFFFFFFF : UiTheme.cLabel());
		}
	}

	private void drawCycleBox(GuiGraphics g, int x, int y, int w, String text, boolean active, boolean muted, int mouseX, int mouseY) {
		boolean hover = hit(mouseX, mouseY, x, y, w, FIELD_H);
		UiTheme.drawButton(g, font, Component.empty(), x, y, w, FIELD_H, hover, active);
		int textW = Math.min(font.width(text), w - 8);
		int color = !active ? UiTheme.cButtonTextDisabled() : muted ? UiTheme.cMuted() : UiTheme.cButtonText();
		UiTheme.clipLabel(g, font, text, x + (w - textW) / 2, y + 4, Math.max(8, w - 8), color);
	}

	private int rescanW() {
		return font.width(I18n.get(K + "aj_rescan"));
	}

	private boolean overRescan(double mouseX, double mouseY) {
		int w = rescanW();
		return hit(mouseX, mouseY, listX + LIST_W - w - 2, rigTop - 12, w + 2, 11);
	}

	private static String shortName(String rig) {
		return rig.startsWith("animated_java:") ? rig.substring(14) : rig;
	}

	private void drawRigs(GuiGraphics g, int mouseX, int mouseY) {
		int x0 = listX;
		int x1 = listX + LIST_W;
		int h = rigRows * ROW_H;
		String title = I18n.get(K + "aj_rigs") + (more ? " +" : "");
		UiTheme.clipLabel(g, font, title, x0, rigTop - 10, LIST_W - rescanW() - 6, UiTheme.cMuted());
		g.drawString(font, I18n.get(K + "aj_rescan"), x1 - rescanW(), rigTop - 10, overRescan(mouseX, mouseY) ? UiTheme.cLabel() : UiTheme.cMuted(), false);
		g.fill(x0, rigTop, x1, rigTop + h, UiTheme.cInputBg());
		if (rigs.isEmpty()) {
			UiTheme.clipLabel(g, font, I18n.get(K + (scanned ? "aj_rigs_empty" : "aj_scanning")), x0 + 4, rigTop + 2, LIST_W - 8, UiTheme.cMuted());
			return;
		}
		AjData.Phase ph = page == PAGE_BOSS && model.boss ? curPhase() : null;
		String activeRig = ph != null && ph.changeRig ? ph.rig : model.rig;
		for (int i = 0; i < rigRows && i + rigScroll < rigs.size(); i++) {
			AjData.Rig rig = rigs.get(i + rigScroll);
			int y = rigTop + i * ROW_H;
			if (rig.id().equals(activeRig)) g.fill(x0, y, x1, y + ROW_H, 0xFF21262D);
			else if (hit(mouseX, mouseY, x0, y, LIST_W, ROW_H)) g.fill(x0, y, x1, y + ROW_H, 0xFF191A1B);
			UiTheme.clipLabel(g, font, shortName(rig.id()), x0 + 4, y + 2, LIST_W - 10, UiTheme.cLabel());
		}
		UiTheme.drawThinScrollBar(g, x1 - 3, rigTop, h, rigScroll, Math.max(0, rigs.size() - rigRows));
	}

	private void drawMade(GuiGraphics g, int mouseX, int mouseY) {
		int x0 = listX;
		int x1 = listX + LIST_W;
		int h = madeRows * ROW_H;
		UiTheme.clipLabel(g, font, I18n.get(K + "aj_made"), x0, madeTop - 10, LIST_W, UiTheme.cMuted());
		g.fill(x0, madeTop, x1, madeTop + h, UiTheme.cInputBg());
		if (made.isEmpty()) {
			UiTheme.clipLabel(g, font, I18n.get(K + "aj_made_empty"), x0 + 4, madeTop + 2, LIST_W - 8, UiTheme.cMuted());
			return;
		}
		for (int i = 0; i < madeRows && i + madeScroll < made.size(); i++) {
			String id = made.get(i + madeScroll);
			int y = madeTop + i * ROW_H;
			boolean hover = hit(mouseX, mouseY, x0, y, LIST_W, ROW_H);
			if (id.equals(model.id)) g.fill(x0, y, x1, y + ROW_H, 0xFF21262D);
			else if (hover) g.fill(x0, y, x1, y + ROW_H, 0xFF191A1B);
			UiTheme.clipLabel(g, font, id, x0 + 4, y + 2, LIST_W - 24, UiTheme.cLabel());
			boolean del = hover && mouseX >= x1 - 14;
			g.drawString(font, "\u00d7", x1 - 10, y + 2, del || id.equals(confirmDelete) ? ERR : UiTheme.cMuted(), false);
		}
		UiTheme.drawThinScrollBar(g, x1 - 3, madeTop, h, madeScroll, Math.max(0, made.size() - madeRows));
	}

	private void label(GuiGraphics g, String key, int x, int y) {
		g.drawString(font, I18n.get(K + key), x, y + 4, UiTheme.cLabel(), false);
	}

	private void drawAnimGrid(GuiGraphics g, int mouseX, int mouseY, String[] animNames, boolean active, int lw) {
		for (int i = 0; i < AjData.SLOTS; i++) {
			if (!slotShown(i)) continue;
			label(g, SLOTS[i], slotX[i] - lw - 6, slotY[i]);
			boolean none = animNames[i].isEmpty();
			String name = none ? I18n.get(K + "aj_none") : animNames[i];
			drawCycleBox(g, slotX[i], slotY[i], slotW[i], name, active, none, mouseX, mouseY);
		}
	}

	private void drawForm(GuiGraphics g, int mouseX, int mouseY) {
		int y = formTop;
		label(g, "aj_name", rightX, y);
		y += step;
		label(g, "aj_rig", rightX, y);
		int rigW = rightX + rightW - fieldX - (variantBtn.visible ? 102 : 0);
		if (model.rig.isEmpty()) UiTheme.clipLabel(g, font, I18n.get(K + "aj_rig_pick"), fieldX, y + 4, rigW, UiTheme.cMuted());
		else UiTheme.clipLabel(g, font, model.rig, fieldX, y + 4, rigW, scanned && currentRig() == null ? ERR : UiTheme.cLabel());
		y += step;
		if (model.mob()) {
			label(g, "aj_carrier", rightX, y);
			label(g, "aj_health", rightX, y + step);
			label(g, "aj_nbt", rightX, y + 2 * step);
		}
		drawAnimGrid(g, mouseX, mouseY, model.anims, currentRig() != null, labelW);
	}

	private void drawMelee(GuiGraphics g, int mouseX, int mouseY) {
		int maxBottom = bodyY + bodyH - 12;
		if (model.mode != AjData.HOSTILE) {
			List<FormattedCharSequence> lines = font.split(Component.translatable(K + "aj_melee_passive"), rightW);
			int y = bodyY + 2 * step;
			for (int i = 0; i < lines.size() && y + i * 12 + 9 <= maxBottom; i++) {
				g.drawString(font, lines.get(i), rightX, y + i * 12, UiTheme.cMuted(), false);
			}
			return;
		}
		if (model.meleeMode == AjData.MELEE_VANILLA) {
			List<FormattedCharSequence> lines = font.split(Component.translatable(K + "aj_melee_vanilla_desc"), rightW);
			int y = bodyY + 2 * step + 2;
			for (int i = 0; i < lines.size() && y + i * 12 + 9 <= maxBottom; i++) {
				g.drawString(font, lines.get(i), rightX, y + i * 12, UiTheme.cMuted(), false);
			}
			return;
		}
		int rY = meleeDmgField.getY();
		label(g, "aj_skill_damage", rightX, rY);
		label(g, "aj_skill_range", meleeRangeLabelX, rY);
		g.drawString(font, I18n.get(K + "aj_unit_block"), meleeRangeField.getX() + meleeRangeField.getWidth() + 3, rY + 4, UiTheme.cMuted(), false);
		label(g, "aj_melee_cd", meleeCdLabelX, rY);
		g.drawString(font, I18n.get(K + "aj_unit_tick"), meleeCdField.getX() + meleeCdField.getWidth() + 3, rY + 4, UiTheme.cMuted(), false);

		drawAnimGrid(g, mouseX, mouseY, model.anims, currentRig() != null, mLabelW);
		g.drawString(font, I18n.get(K + "aj_unit_tick"), tickFields[AjData.ATTACK].getX() + tickFields[AjData.ATTACK].getWidth() + 3, slotY[AjData.ATTACK] + 4, UiTheme.cMuted(), false);

		label(g, "aj_melee_hits", rightX, meleeHitsField.getY());
		int descY = meleeHitsField.getY() + step + 2;
		List<FormattedCharSequence> lines = font.split(Component.translatable(K + "aj_melee_sync_desc"), rightW);
		for (int i = 0; i < lines.size() && descY + i * 11 + 9 <= maxBottom; i++) {
			g.drawString(font, lines.get(i), rightX, descY + i * 11, UiTheme.cMuted(), false);
		}
	}

	private void drawSkills(GuiGraphics g, int mouseX, int mouseY) {
		int pillY = bodyY + step;
		for (int i = 0; i < model.skills.size(); i++) {
			int px = rightX + i * 19;
			boolean sel = i == skillIdx;
			boolean hov = hit(mouseX, mouseY, px, pillY, 17, FIELD_H);
			int bg = sel ? UiTheme.cAccent() : hov ? UiTheme.cButtonHover() : UiTheme.cInputBg();
			g.fill(px, pillY, px + 17, pillY + FIELD_H, bg);
			g.drawCenteredString(font, String.valueOf(i + 1), px + 8, pillY + 4, sel ? 0xFFFFFFFF : UiTheme.cLabel());
		}
		AjData.Skill sk = curSkill();
		if (sk == null) {
			UiTheme.clipLabel(g, font, I18n.get(K + "aj_skills_empty"), rightX, pillY + step + 4, rightW, UiTheme.cMuted());
			return;
		}
		label(g, "aj_skill_kind", rightX, skillKindY);
		drawCycleBox(g, skillKindX, skillKindY, skillKindW, I18n.get(K + "aj_sk_" + sk.kind), true, false, mouseX, mouseY);
		if (model.boss && !model.phases.isEmpty()) {
			String phLbl = sk.phase == 0 ? I18n.get(K + "aj_skill_phase_all") : I18n.get(K + "aj_skill_phase_n", sk.phase);
			drawCycleBox(g, skillPhaseX, skillPhaseY, skillPhaseW, phLbl, true, sk.phase == 0, mouseX, mouseY);
		}
		label(g, "aj_skill_trig", rightX, skillTrigY);
		drawCycleBox(g, skillTrigX, skillTrigY, skillTrigW, I18n.get(K + "aj_tr_" + sk.trigger), true, false, mouseX, mouseY);
		String unitKey = sk.trigger == AjData.TRIG_HURT || sk.trigger == AjData.TRIG_HIT ? "aj_unit_hits" : sk.trigger == AjData.TRIG_HP ? "aj_unit_pct" : "aj_unit_tick";
		g.drawString(font, I18n.get(K + unitKey), skillValField.getX() + skillValField.getWidth() + 3, skillTrigY + 4, UiTheme.cMuted(), false);

		int rY = skillRangeField.getY();
		label(g, "aj_skill_range", rightX, rY);
		if (sk.kind != AjData.SKILL_CUSTOM) {
			label(g, AjData.usesDamage(sk.kind) ? "aj_skill_damage" : "aj_skill_burst", skillPowerLabelX, rY);
		}
		label(g, "aj_skill_windup", skillWindupLabelX, rY);
		g.drawString(font, I18n.get(K + "aj_unit_tick"), skillWindupField.getX() + skillWindupField.getWidth() + 3, rY + 4, UiTheme.cMuted(), false);

		label(g, "aj_skill_anim", rightX, skillAnimY);
		boolean hasRig = skillRig(sk) != null;
		boolean noAnim = sk.anim.isEmpty();
		drawCycleBox(g, skillAnimX, skillAnimY, skillAnimW, noAnim ? I18n.get(K + "aj_none") : sk.anim, hasRig, noAnim, mouseX, mouseY);
		if (!noAnim) {
			g.drawString(font, I18n.get(K + "aj_unit_tick"), skillAnimTicksField.getX() + skillAnimTicksField.getWidth() + 3, skillAnimY + 4, UiTheme.cMuted(), false);
		}

		int descY = skillAnimY + step;
		if (sk.kind == AjData.SKILL_CUSTOM) {
			label(g, "aj_skill_cmd", rightX, skillCmdField.getY());
			descY = skillCmdField.getY() + step;
		}
		List<FormattedCharSequence> lines = font.split(Component.translatable(K + "aj_sk_desc_" + sk.kind), rightW);
		int maxBottom = bodyY + bodyH - 12;
		for (int i = 0; i < lines.size() && descY + i * 11 + 9 <= maxBottom; i++) {
			g.drawString(font, lines.get(i), rightX, descY + i * 11 + 2, UiTheme.cMuted(), false);
		}
	}

	private void drawBoss(GuiGraphics g, int mouseX, int mouseY) {
		if (!model.boss) {
			List<FormattedCharSequence> lines = font.split(Component.translatable(K + "aj_boss_disabled"), rightW);
			int y = bodyY + 2 * step + 2;
			for (int i = 0; i < lines.size(); i++) g.drawString(font, lines.get(i), rightX, y + i * 12, UiTheme.cMuted(), false);
			return;
		}
		drawCycleBox(g, bossColorX, bossColorY, bossColorW, I18n.get(K + "aj_bar_c_" + model.bossColor), model.bossBar, false, mouseX, mouseY);
		drawCycleBox(g, bossStyleX, bossStyleY, bossStyleW, I18n.get(K + "aj_bar_s_" + model.bossStyle), model.bossBar, false, mouseX, mouseY);
		label(g, "aj_boss_name", rightX, bossNameField.getY());
		label(g, "aj_phases", rightX, phaseTabsY);
		g.drawCenteredString(font, String.valueOf(model.phaseCount()), bFieldX + 27, phaseTabsY + 4, UiTheme.cLabel());
		for (int i = 0; i < model.phases.size(); i++) {
			int px = phaseTabsX + i * 26;
			boolean sel = i == phaseIdx;
			boolean hov = hit(mouseX, mouseY, px, phaseTabsY, 24, FIELD_H);
			int bg = sel ? UiTheme.cAccent() : hov ? UiTheme.cButtonHover() : UiTheme.cInputBg();
			g.fill(px, phaseTabsY, px + 24, phaseTabsY + FIELD_H, bg);
			g.drawCenteredString(font, I18n.get(K + "aj_phase_tab", i + 2), px + 12, phaseTabsY + 4, sel ? 0xFFFFFFFF : UiTheme.cLabel());
		}
		AjData.Phase ph = curPhase();
		if (ph == null) {
			UiTheme.clipLabel(g, font, I18n.get(K + "aj_phase_single"), rightX, phaseTabsY + step + 4, rightW, UiTheme.cMuted());
			return;
		}
		label(g, "aj_phase_hp", rightX, phaseTransY);
		g.drawString(font, "%", phaseHpField.getX() + phaseHpField.getWidth() + 2, phaseTransY + 4, UiTheme.cMuted(), false);
		label(g, "aj_phase_trans", phaseTransLabelX, phaseTransY);
		boolean hasTrRig = transChoices(ph).size() > 1;
		drawCycleBox(g, phaseTransX, phaseTransY, phaseTransW, transLabel(ph), hasTrRig, ph.transAnim.isEmpty(), mouseX, mouseY);
		label(g, "aj_phase_swap", rightX, phaseSwapBtn.getY());
		if (!ph.changeRig) {
			int kx = phaseSwapBtn.getX() + phaseSwapBtn.getWidth() + 6;
			UiTheme.clipLabel(g, font, I18n.get(K + "aj_phase_keep"), kx, phaseSwapBtn.getY() + 4, rightX + rightW - kx, UiTheme.cMuted());
			return;
		}
		drawAnimGrid(g, mouseX, mouseY, ph.anims, phaseRig() != null, bLabelW);
	}

	private void drawDrops(GuiGraphics g, int mouseX, int mouseY) {
		int size = model.drops.size();
		UiTheme.clipLabel(g, font, I18n.get(K + "aj_drops_title", size, AjData.MAX_DROPS), rightX, bodyY + 4, addBtn.getX() - 6 - rightX, UiTheme.cLabel());
		if (size == 0) {
			UiTheme.clipLabel(g, font, I18n.get(K + "aj_drops_empty"), rightX, dropTop + 6, rightW, UiTheme.cMuted());
			return;
		}
		g.drawString(font, I18n.get(K + "aj_drop_item"), rightX, dropTop - 10, UiTheme.cMuted(), false);
		g.drawString(font, I18n.get(K + "aj_drop_count"), dropMinX, dropTop - 10, UiTheme.cMuted(), false);
		g.drawString(font, I18n.get(K + "aj_drop_chance"), dropChanceX, dropTop - 10, UiTheme.cMuted(), false);
		int nameX = rightX + 22;
		for (int row = 0; row < dropRows; row++) {
			AjData.Drop d = dropAt(row);
			if (d == null) break;
			int y = dropTop + row * LINE;
			ItemStack stack = stack(d);
			UiTheme.drawSlot(g, rightX, y + 1);
			if (!stack.isEmpty()) g.renderItem(stack, rightX + 1, y + 2);
			String name = stack.isEmpty() ? I18n.get(K + "aj_drop_invalid") : stack.getHoverName().getString();
			UiTheme.clipLabel(g, font, name, nameX, y + 6, dropMinX - 6 - nameX, stack.isEmpty() ? ERR : UiTheme.cLabel());
			g.drawString(font, "~", dropMaxX - 8, y + 6, UiTheme.cMuted(), false);
			g.drawString(font, "%", dropChanceX + CHANCE_W + 2, y + 6, UiTheme.cMuted(), false);
			boolean del = hit(mouseX, mouseY, dropDelX, y + 2, 12, FIELD_H);
			g.drawString(font, "\u00d7", dropDelX + 3, y + 6, del ? ERR : UiTheme.cMuted(), false);
		}
		UiTheme.drawThinScrollBar(g, rightX + rightW - 3, dropTop, dropRows * LINE, dropScroll, Math.max(0, size - dropRows));
	}

	private void drawStatus(GuiGraphics g) {
		String text = status.getString();
		int color = statusColor;
		if (text.isEmpty()) {
			color = UiTheme.cMuted();
			if (!scanned) text = I18n.get(K + "aj_scanning");
			else if (rigs.isEmpty()) text = I18n.get(K + "aj_hint_empty");
			else if (model.rig.isEmpty()) text = I18n.get(K + "aj_hint_pick");
			else if (page == PAGE_DROPS) text = I18n.get(K + "aj_hint_drops");
			else if (page == PAGE_MELEE) text = I18n.get(K + "aj_hint_melee");
			else if (page == PAGE_SKILLS) text = I18n.get(K + "aj_hint_skills");
			else if (page == PAGE_BOSS) text = I18n.get(K + "aj_hint_boss");
			else text = I18n.get(K + (model.mob() ? "aj_hint_ticks" : "aj_hint_static"));
		}
		UiTheme.clipLabel(g, font, text, rightX, bodyY + bodyH - 9, rightW, color);
	}

	private void drawTip(GuiGraphics g, int mouseX, int mouseY) {
		if (hit(mouseX, mouseY, listX, rigTop, LIST_W, rigRows * ROW_H)) {
			int idx = (mouseY - rigTop) / ROW_H + rigScroll;
			if (idx < 0 || idx >= rigs.size()) return;
			AjData.Rig rig = rigs.get(idx);
			g.renderComponentTooltip(font, List.of(Component.literal(rig.id()), Component.translatable(K + "aj_tip_pack", rig.pack()), Component.translatable(K + "aj_tip_anims", rig.anims().size())), mouseX, mouseY);
			return;
		}
		if (page == PAGE_DROPS) {
			for (int row = 0; row < dropRows; row++) {
				AjData.Drop d = dropAt(row);
				if (d == null) return;
				if (!hit(mouseX, mouseY, rightX, dropTop + row * LINE + 1, 18, 18)) continue;
				ItemStack stack = stack(d);
				List<Component> lines = new ArrayList<>();
				if (!stack.isEmpty()) lines.addAll(getTooltipFromItem(this.minecraft, stack));
				lines.add(Component.translatable(K + "aj_tip_drop").withStyle(ChatFormatting.GRAY));
				g.renderComponentTooltip(font, lines, mouseX, mouseY);
				return;
			}
			return;
		}
		if (page == PAGE_SKILLS) {
			AjData.Skill sk = curSkill();
			if (sk != null && (hit(mouseX, mouseY, skillKindX, skillKindY, skillKindW, FIELD_H)
				|| hit(mouseX, mouseY, skillTrigX, skillTrigY, skillTrigW, FIELD_H)
				|| hit(mouseX, mouseY, skillAnimX, skillAnimY, skillAnimW, FIELD_H))) {
				g.renderComponentTooltip(font, List.of(Component.translatable(K + "aj_tip_cycle")), mouseX, mouseY);
			}
			return;
		}
		if (page == PAGE_BASE) {
			String tip = over(healthField, mouseX, mouseY) && !healthField.isFocused() ? "aj_tip_health" : over(lootBtn, mouseX, mouseY) ? "aj_tip_loot" : null;
			if (tip != null) {
				g.renderTooltip(font, font.split(Component.translatable(K + tip), 210), mouseX, mouseY);
				return;
			}
		}
		AjData.Rig rig = page == PAGE_BOSS ? phaseRig() : currentRig();
		if (rig == null) return;
		String[] anims = page == PAGE_BOSS && curPhase() != null ? curPhase().anims : model.anims;
		for (int i = 0; i < AjData.SLOTS; i++) {
			if (!slotShown(i) || !hit(mouseX, mouseY, slotX[i], slotY[i], slotW[i], FIELD_H)) continue;
			List<Component> lines = new ArrayList<>();
			AjData.Anim anim = rig.anim(anims[i]);
			if (anim != null) lines.add(Component.translatable(K + "aj_tip_length", anim.name(), anim.length()));
			lines.add(Component.translatable(K + "aj_tip_cycle"));
			g.renderComponentTooltip(font, lines, mouseX, mouseY);
			return;
		}
	}

	private boolean pressed(double mouseX, double mouseY, int button) {
		if (button != 0 && button != 1) return false;
		if (button == 0 && model.mob() && page != PAGE_DROPS) {
			for (int i = 0; i < 4; i++) {
				if (hit(mouseX, mouseY, tabX[i], bodyY, tabW[i], FIELD_H)) {
					if (page != i) {
						UiTheme.playClick();
						switchPage(i);
					}
					return true;
				}
			}
		}
		for (int row = 0; page == PAGE_DROPS && button == 0 && dropAt(row) != null; row++) {
			int y = dropTop + row * LINE;
			if (hit(mouseX, mouseY, rightX, y + 1, 18, 18)) {
				UiTheme.playClick();
				pickIndex = dropScroll + row;
				invPicker.open();
				return true;
			}
			if (hit(mouseX, mouseY, dropDelX, y + 2, 12, FIELD_H)) {
				UiTheme.playClick();
				removeDrop(dropScroll + row);
				return true;
			}
		}
		int dir = button == 0 ? 1 : -1;
		if (page == PAGE_SKILLS && model.mob()) {
			int pillY = bodyY + step;
			if (button == 0) {
				for (int i = 0; i < model.skills.size(); i++) {
					int px = rightX + i * 19;
					if (hit(mouseX, mouseY, px, pillY, 17, FIELD_H)) {
						UiTheme.playClick();
						skillIdx = i;
						setFocused(null);
						syncAll();
						return true;
					}
				}
			}
			if (curSkill() != null) {
				if (hit(mouseX, mouseY, skillKindX, skillKindY, skillKindW, FIELD_H)) {
					UiTheme.playClick();
					cycleSkillKind(dir);
					return true;
				}
				if (model.boss && !model.phases.isEmpty() && hit(mouseX, mouseY, skillPhaseX, skillPhaseY, skillPhaseW, FIELD_H)) {
					UiTheme.playClick();
					cycleSkillPhase(dir);
					return true;
				}
				if (hit(mouseX, mouseY, skillTrigX, skillTrigY, skillTrigW, FIELD_H)) {
					UiTheme.playClick();
					cycleSkillTrigger(dir);
					return true;
				}
				if (hit(mouseX, mouseY, skillAnimX, skillAnimY, skillAnimW, FIELD_H)) {
					UiTheme.playClick();
					cycleSkillAnim(dir);
					return true;
				}
			}
		}
		if (page == PAGE_BOSS && model.mob() && model.boss) {
			if (model.bossBar && hit(mouseX, mouseY, bossColorX, bossColorY, bossColorW, FIELD_H)) {
				UiTheme.playClick();
				cycleBossColor(dir);
				return true;
			}
			if (model.bossBar && hit(mouseX, mouseY, bossStyleX, bossStyleY, bossStyleW, FIELD_H)) {
				UiTheme.playClick();
				cycleBossStyle(dir);
				return true;
			}
			if (button == 0) {
				for (int i = 0; i < model.phases.size(); i++) {
					int px = phaseTabsX + i * 26;
					if (hit(mouseX, mouseY, px, phaseTabsY, 24, FIELD_H)) {
						UiTheme.playClick();
						phaseIdx = i;
						setFocused(null);
						syncAll();
						return true;
					}
				}
			}
			if (curPhase() != null && hit(mouseX, mouseY, phaseTransX, phaseTransY, phaseTransW, FIELD_H)) {
				UiTheme.playClick();
				cyclePhaseTrans(dir);
				return true;
			}
		}
		for (int i = 0; i < AjData.SLOTS; i++) {
			if (slotShown(i) && hit(mouseX, mouseY, slotX[i], slotY[i], slotW[i], FIELD_H)) {
				UiTheme.playClick();
				cycleAnim(i, dir);
				return true;
			}
		}
		if (button != 0) return false;
		if (hit(mouseX, mouseY, genX, footerY, genW, 20)) {
			UiTheme.playClick();
			generate(null);
			return true;
		}
		if (overRescan(mouseX, mouseY)) {
			UiTheme.playClick();
			scanned = false;
			rigs = List.of();
			setStatus(Component.empty(), OK);
			requestScan();
			return true;
		}
		if (hit(mouseX, mouseY, listX, rigTop, LIST_W, rigRows * ROW_H)) {
			int idx = (int) ((mouseY - rigTop) / ROW_H) + rigScroll;
			if (idx < 0 || idx >= rigs.size()) return true;
			UiTheme.playClick();
			selectRig(rigs.get(idx));
			return true;
		}
		if (hit(mouseX, mouseY, listX, madeTop, LIST_W, madeRows * ROW_H)) {
			int idx = (int) ((mouseY - madeTop) / ROW_H) + madeScroll;
			if (idx < 0 || idx >= made.size()) return true;
			UiTheme.playClick();
			if (mouseX >= listX + LIST_W - 14) deleteModel(made.get(idx));
			else loadModel(made.get(idx));
			return true;
		}
		return false;
	}

	private boolean scrolled(double mouseX, double mouseY, double scrollY) {
		if (scrollY == 0) return false;
		int dir = scrollY > 0 ? -1 : 1;
		if (page == PAGE_DROPS && hit(mouseX, mouseY, rightX, dropTop, rightW, dropRows * LINE)) {
			dropScroll += dir;
			syncAll();
			return true;
		}
		if (hit(mouseX, mouseY, listX, rigTop, LIST_W, rigRows * ROW_H)) {
			rigScroll = Mth.clamp(rigScroll + dir, 0, Math.max(0, rigs.size() - rigRows));
			return true;
		}
		if (hit(mouseX, mouseY, listX, madeTop, LIST_W, madeRows * ROW_H)) {
			madeScroll = Mth.clamp(madeScroll + dir, 0, Math.max(0, made.size() - madeRows));
			return true;
		}
		if (page == PAGE_SKILLS && model.mob() && curSkill() != null) {
			if (hit(mouseX, mouseY, skillKindX, skillKindY, skillKindW, FIELD_H)) {
				cycleSkillKind(dir);
				return true;
			}
			if (model.boss && !model.phases.isEmpty() && hit(mouseX, mouseY, skillPhaseX, skillPhaseY, skillPhaseW, FIELD_H)) {
				cycleSkillPhase(dir);
				return true;
			}
			if (hit(mouseX, mouseY, skillTrigX, skillTrigY, skillTrigW, FIELD_H)) {
				cycleSkillTrigger(dir);
				return true;
			}
			if (hit(mouseX, mouseY, skillAnimX, skillAnimY, skillAnimW, FIELD_H)) {
				cycleSkillAnim(dir);
				return true;
			}
		}
		if (page == PAGE_BOSS && model.mob() && model.boss) {
			if (model.bossBar && hit(mouseX, mouseY, bossColorX, bossColorY, bossColorW, FIELD_H)) {
				cycleBossColor(dir);
				return true;
			}
			if (model.bossBar && hit(mouseX, mouseY, bossStyleX, bossStyleY, bossStyleW, FIELD_H)) {
				cycleBossStyle(dir);
				return true;
			}
			if (curPhase() != null && hit(mouseX, mouseY, phaseTransX, phaseTransY, phaseTransW, FIELD_H)) {
				cyclePhaseTrans(dir);
				return true;
			}
		}
		for (int i = 0; i < AjData.SLOTS; i++) {
			if (slotShown(i) && hit(mouseX, mouseY, slotX[i], slotY[i], slotW[i], FIELD_H)) {
				cycleAnim(i, dir);
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
				pick(picked);
			} else if (button == 1 || button == 0 && !invPicker.inPanel(this.width, this.height, mouseX, mouseY)) {
				invPicker.close();
			}
			return true;
		}
		if (button == 0 && (clickCarrierPop(mouseX, mouseY) || clickPhaseRigPop(mouseX, mouseY))) return true;
		if (skillCommandActive() && skillSuggestions.mouseClicked(mouseX, mouseY, button)) return true;
		boolean handled = pressed(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button);
		if (button == 0 && over(carrierField, mouseX, mouseY)) {
			phaseRigHits = List.of();
			updateCarrierHits();
		} else if (button == 0 && over(phaseRigField, mouseX, mouseY)) {
			carrierHits = List.of();
			updatePhaseRigHits();
		} else {
			carrierHits = List.of();
			phaseRigHits = List.of();
			popupKind = 0;
		}
		if (skillCommandActive() && !over(skillCmdField, mouseX, mouseY)) skillSuggestions.hide();
		return handled;
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		if (button == 0 && SuggestionPopup.isDragging()) {
			if (popupKind == 1) carrierScroll = SuggestionPopup.dragTo(mouseY);
			else if (popupKind == 2) phaseRigScroll = SuggestionPopup.dragTo(mouseY);
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
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
		if (invPicker.isOpen()) return true;
		if (overCarrierPop(mouseX, mouseY)) {
			carrierScroll = SuggestionPopup.scrollBy(carrierScroll, carrierHits.size(), scrollY);
			return true;
		}
		if (overPhaseRigPop(mouseX, mouseY)) {
			phaseRigScroll = SuggestionPopup.scrollBy(phaseRigScroll, phaseRigHits.size(), scrollY);
			return true;
		}
		if (skillCommandActive() && skillSuggestions.mouseScrolled(Mth.clamp(scrollY, -1.0, 1.0))) return true;
		if (scrolled(mouseX, mouseY, scrollY)) return true;
		return super.mouseScrolled(mouseX, mouseY, scrollY);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (invPicker.isOpen()) {
			if (keyCode == GLFW.GLFW_KEY_ESCAPE) invPicker.close();
			return true;
		}
		if (carrierOpen()) {
			if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
				carrierHits = List.of();
				popupKind = 0;
				return true;
			}
			if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER || keyCode == GLFW.GLFW_KEY_TAB) {
				pickCarrier(carrierIdx);
				return true;
			}
			if (keyCode == GLFW.GLFW_KEY_DOWN || keyCode == GLFW.GLFW_KEY_UP) {
				carrierIdx = Mth.clamp(carrierIdx + (keyCode == GLFW.GLFW_KEY_DOWN ? 1 : -1), 0, carrierHits.size() - 1);
				carrierScroll = SuggestionPopup.keepVisible(carrierIdx, carrierScroll, carrierHits.size());
				return true;
			}
		}
		if (phaseRigOpen()) {
			if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
				phaseRigHits = List.of();
				popupKind = 0;
				return true;
			}
			if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER || keyCode == GLFW.GLFW_KEY_TAB) {
				pickPhaseRig(phaseRigIdx);
				return true;
			}
			if (keyCode == GLFW.GLFW_KEY_DOWN || keyCode == GLFW.GLFW_KEY_UP) {
				phaseRigIdx = Mth.clamp(phaseRigIdx + (keyCode == GLFW.GLFW_KEY_DOWN ? 1 : -1), 0, phaseRigHits.size() - 1);
				phaseRigScroll = SuggestionPopup.keepVisible(phaseRigIdx, phaseRigScroll, phaseRigHits.size());
				return true;
			}
		}
		if (skillCommandActive() && skillCmdField.isFocused() && skillSuggestions.keyPressed(keyCode, scanCode, modifiers)) return true;
		if (page != PAGE_BASE && keyCode == GLFW.GLFW_KEY_ESCAPE) {
			switchPage(PAGE_BASE);
			return true;
		}
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
