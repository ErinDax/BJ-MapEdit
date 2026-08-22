package cn.erindax.bjmapedit.client.screen;

import cn.erindax.bjmapedit.client.widget.GameRuleTemplateStore;
import cn.erindax.bjmapedit.client.widget.TemplateDrag;
import cn.erindax.bjmapedit.client.widget.TemplateOrg;
import cn.erindax.bjmapedit.client.widget.RenameDeleteMenu;
import cn.erindax.bjmapedit.client.widget.TemplateRailUi;
import cn.erindax.bjmapedit.client.widget.UiTheme;
import cn.erindax.bjmapedit.networking.payload.ExecuteCommandsPayload;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.GameRules;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Environment(EnvType.CLIENT)
public class GameRuleEditorScreen extends Screen {

	private int WIDTH = 500;
	private int HEIGHT = 400;
	private static final int FOOTER_H = 30;
	private static final int NAME_X = 12;
	private static final int ROW_H = 22;
	private static final int VALUE_W = 64;
	private static final int TEMPLATE_ROW_H = 22;
	private int CONTENT_TOP = 30;
	private int CONTENT_BOTTOM;
	private int templateRailX = 8;
	private int templateRailW = 148;
	private int templateScroll;
	private int templateMaxScroll;
	private boolean draggingTemplateScroll;
	private double templateScrollGrab;
	private int selectedTemplate = -1;
	private String selectedFolder = "";
	private static int sessionSelectedTemplate = -1;
	private static String sessionSelectedFolder = "";
	private static final Set<String> collapsedFolders = new HashSet<>();
	private static Map<String, String> sessionValues;
	private boolean opened;
	private EditBox templateRenameBox;
	private int renamingTemplate = -1;
	private String renamingFolder = "";
	private final TemplateDrag templateDrag = new TemplateDrag();
	private final RenameDeleteMenu templateMenu = new RenameDeleteMenu();
	private TemplateOrg.Row templateMenuRow;
	private int refreshX, refreshY;

	private static final Map<String, String> GAMERULE_CN = new HashMap<>();
	static {
		GAMERULE_CN.put("announceAdvancements", "公告进度");
		GAMERULE_CN.put("blockExplosionDropDecay", "方块爆炸掉落衰减");
		GAMERULE_CN.put("commandBlockOutput", "命令方块输出");
		GAMERULE_CN.put("commandModificationBlockLimit", "命令修改方块限制");
		GAMERULE_CN.put("disableElytraMovementCheck", "禁用鞘翅移动检查");
		GAMERULE_CN.put("disableRaids", "禁用袭击");
		GAMERULE_CN.put("doDaylightCycle", "日夜循环");
		GAMERULE_CN.put("doEntityDrops", "实体掉落");
		GAMERULE_CN.put("doFireTick", "火势蔓延");
		GAMERULE_CN.put("doImmediateRespawn", "立即重生");
		GAMERULE_CN.put("doInsomnia", "幻翼生成");
		GAMERULE_CN.put("doLimitedCrafting", "限制合成");
		GAMERULE_CN.put("doMobLoot", "生物战利品");
		GAMERULE_CN.put("doMobSpawning", "生物生成");
		GAMERULE_CN.put("doPatrolSpawning", "巡逻队生成");
		GAMERULE_CN.put("doTileDrops", "方块掉落");
		GAMERULE_CN.put("doTraderSpawning", "流浪商人生成");
		GAMERULE_CN.put("doVinesSpread", "藤蔓蔓延");
		GAMERULE_CN.put("doWardenSpawning", "监守者生成");
		GAMERULE_CN.put("doWeatherCycle", "天气循环");
		GAMERULE_CN.put("drowningDamage", "溺水伤害");
		GAMERULE_CN.put("enderPearlsVanishOnDeath", "末影珍珠死亡消失");
		GAMERULE_CN.put("fallDamage", "坠落伤害");
		GAMERULE_CN.put("fireDamage", "火焰伤害");
		GAMERULE_CN.put("forgiveDeadPlayers", "原谅死亡玩家");
		GAMERULE_CN.put("freezeDamage", "冰冻伤害");
		GAMERULE_CN.put("globalSoundEvents", "全局声音事件");
		GAMERULE_CN.put("keepInventory", "保留物品栏");
		GAMERULE_CN.put("lavaSourceConversion", "熔岩源转换");
		GAMERULE_CN.put("logAdminCommands", "记录管理员命令");
		GAMERULE_CN.put("maxCommandChainLength", "最大命令链长度");
		GAMERULE_CN.put("maxCommandForkCount", "最大命令分支数");
		GAMERULE_CN.put("maxEntityCramming", "最大实体挤压");
		GAMERULE_CN.put("mobExplosionDropDecay", "生物爆炸掉落衰减");
		GAMERULE_CN.put("mobGriefing", "生物破坏");
		GAMERULE_CN.put("naturalRegeneration", "自然恢复");
		GAMERULE_CN.put("playersNetherPortalCreativeDelay", "创造模式传送门延迟");
		GAMERULE_CN.put("playersNetherPortalDefaultDelay", "默认传送门延迟");
		GAMERULE_CN.put("playersSleepingPercentage", "玩家睡眠百分比");
		GAMERULE_CN.put("projectilesCanBreakBlocks", "投射物破坏方块");
		GAMERULE_CN.put("randomTickSpeed", "随机刻速度");
		GAMERULE_CN.put("reducedDebugInfo", "简化调试信息");
		GAMERULE_CN.put("sendCommandFeedback", "发送命令反馈");
		GAMERULE_CN.put("showDeathMessages", "显示死亡消息");
		GAMERULE_CN.put("snowAccumulationHeight", "积雪累积高度");
		GAMERULE_CN.put("spawnChunkRadius", "生成区块半径");
		GAMERULE_CN.put("spawnRadius", "重生点半径");
		GAMERULE_CN.put("spectatorsGenerateChunks", "旁观者生成区块");
		GAMERULE_CN.put("tntExplosionDropDecay", "TNT爆炸掉落衰减");
		GAMERULE_CN.put("universalAnger", "全局愤怒");
		GAMERULE_CN.put("waterSourceConversion", "水源转换");
		GAMERULE_CN.put("minecartMaxSpeed", "矿车最大速度");
		GAMERULE_CN.put("maxAnvilCost", "铁砧最大花费");
		GAMERULE_CN.put("tntExplodes", "TNT爆炸");
		GAMERULE_CN.put("locatorBar", "定位栏");
		GAMERULE_CN.put("spawnerBlocksEnabled", "刷怪笼启用");
	}

	private int leftPos;
	private int topPos;
	private int scrollOffset;
	private int maxScroll;
	private boolean draggingScroll;
	private double scrollGrabOffset;

	private Button applyBtn;
	private Button saveBtn;
	private final List<GameRuleEntry> entries = new ArrayList<>();

	private static class GameRuleEntry {
		final GameRules.Key<?> key;
		final String name;
		boolean isBoolean;
		AbstractWidget valueWidget;
		Button copyBtn;
		int y;

		GameRuleEntry(GameRules.Key<?> key) {
			this.key = key;
			this.name = key.getId();
		}
	}

	public GameRuleEditorScreen() {
		super(Component.translatable("screen.bj_mapedit.gamerule_title"));
	}

	@Override
	protected void init() {
		boolean first = !opened;
		opened = true;
		super.init();
		layoutPanel();

		Map<String, String> snap = captureValues();
		int snapScroll = scrollOffset;
		buildEntries(snap);
		scrollOffset = snapScroll;
		if (first && sessionValues != null) {
			applyValueMap(sessionValues, false);
			selectedTemplate = sessionSelectedTemplate;
			selectedFolder = sessionSelectedFolder;
		}

		applyBtn = this.addRenderableWidget(Button.builder(
			Component.translatable("screen.bj_mapedit.apply_gamerules"),
			b -> applyRules()
		).bounds(0, 0, 70, 20).build());
		saveBtn = this.addRenderableWidget(Button.builder(
			Component.translatable("screen.bj_mapedit.save_template"),
			b -> saveTemplate()
		).bounds(0, 0, 70, 20).build());

		templateRenameBox = new EditBox(font, 0, -1000, 80, 16, Component.empty());
		templateRenameBox.setMaxLength(24);
		templateRenameBox.visible = false;
		this.addRenderableWidget(templateRenameBox);

		repositionAll();
	}

	private void layoutPanel() {
		int margin = 8;
		int gap = 8;
		int availW = Math.max(32, this.width - margin * 2);
		int availH = Math.max(32, this.height - margin * 2);
		templateRailW = Mth.clamp(availW * 20 / 100, 112, 220);
		if (availW < 420) {
			templateRailW = Mth.clamp(availW * 26 / 100, 88, 140);
		}
		WIDTH = Math.max(8, availW - templateRailW - gap);
		HEIGHT = availH;
		templateRailX = margin;
		leftPos = margin + templateRailW + gap;
		topPos = margin;
		CONTENT_TOP = UiTheme.HEADER_H + 8;
		CONTENT_BOTTOM = HEIGHT - FOOTER_H - 4;
		updateTemplateScroll();
		layoutRefreshIcon();
	}

	private void layoutRefreshIcon() {
		refreshX = UiTheme.headerIconX(leftPos, WIDTH);
		refreshY = UiTheme.headerIconY(topPos);
	}

	private Map<String, String> captureValues() {
		Map<String, String> snap = new HashMap<>();
		for (GameRuleEntry entry : entries) {
			String value = currentValue(entry);
			if (value != null) snap.put(entry.name, value);
		}
		return snap;
	}

	private String currentValue(GameRuleEntry entry) {
		if (entry.valueWidget instanceof Checkbox cb) {
			return String.valueOf(cb.selected());
		}
		if (entry.valueWidget instanceof EditBox eb) {
			return eb.getValue();
		}
		return null;
	}

	private void buildEntries(Map<String, String> snap) {
		entries.clear();

		GameRules.visitGameRuleTypes(new GameRules.GameRuleTypeVisitor() {
			@Override
			public void visitBoolean(GameRules.Key<GameRules.BooleanValue> key, GameRules.Type<GameRules.BooleanValue> type) {
				GameRuleEntry entry = new GameRuleEntry(key);
				entry.isBoolean = true;
				entries.add(entry);
			}

			@Override
			public void visitInteger(GameRules.Key<GameRules.IntegerValue> key, GameRules.Type<GameRules.IntegerValue> type) {
				GameRuleEntry entry = new GameRuleEntry(key);
				entry.isBoolean = false;
				entries.add(entry);
			}
		});

		entries.sort(Comparator.comparing(e -> e.name));

		Minecraft mc = Minecraft.getInstance();
		GameRules rules = mc.level != null ? mc.level.getGameRules() : null;

		for (GameRuleEntry entry : entries) {
			String snapped = snap.get(entry.name);
			AbstractWidget widget;
			if (entry.isBoolean) {
				@SuppressWarnings("unchecked")
				GameRules.Key<GameRules.BooleanValue> bk = (GameRules.Key<GameRules.BooleanValue>) entry.key;
				boolean currentVal = snapped != null ? Boolean.parseBoolean(snapped) : rules != null && rules.getBoolean(bk);
				widget = Checkbox.builder(Component.empty(), font)
					.pos(0, 0)
					.selected(currentVal)
					.build();
			} else {
				@SuppressWarnings("unchecked")
				GameRules.Key<GameRules.IntegerValue> ik = (GameRules.Key<GameRules.IntegerValue>) entry.key;
				String currentVal = snapped != null
					? snapped
					: String.valueOf(rules != null ? rules.getInt(ik) : 0);
				EditBox field = new EditBox(font, 0, 0, VALUE_W, 16, Component.empty());
				field.setValue(currentVal);
				widget = field;
			}
			entry.valueWidget = widget;
			this.addRenderableWidget(widget);

			Button copyBtn = Button.builder(Component.translatable("screen.bj_mapedit.copy"), b -> copyRuleCommand(entry))
				.bounds(0, 0, 72, 16).build();
			entry.copyBtn = copyBtn;
			this.addRenderableWidget(copyBtn);
		}
	}

	private void copyRuleCommand(GameRuleEntry entry) {
		String value;
		if (entry.isBoolean) {
			Checkbox cb = (Checkbox) entry.valueWidget;
			value = String.valueOf(cb.selected());
		} else {
			EditBox eb = (EditBox) entry.valueWidget;
			value = eb.getValue().trim();
			if (value.isEmpty()) value = "0";
		}
		String cmd = "/gamerule " + entry.name + " " + value;
		Minecraft.getInstance().keyboardHandler.setClipboard(cmd);
		if (Minecraft.getInstance().player != null) {
			Minecraft.getInstance().player.displayClientMessage(
				Component.translatable("screen.bj_mapedit.copied"), true);
		}
	}

	private void applyRules() {
		if (Minecraft.getInstance().player == null) return;
		List<String> commands = new ArrayList<>();
		for (GameRuleEntry entry : entries) {
			if (entry.valueWidget == null) continue;
			String value;
			if (entry.isBoolean) {
				value = String.valueOf(((Checkbox) entry.valueWidget).selected());
			} else {
				value = ((EditBox) entry.valueWidget).getValue().trim();
				if (value.isEmpty()) value = "0";
			}
			commands.add("gamerule " + entry.name + " " + value);
		}
		if (!commands.isEmpty()) {
			ClientPlayNetworking.send(new ExecuteCommandsPayload(commands));
		}
	}

	private void resetFields() {
		applyValueMap(null, true);
		selectedTemplate = -1;
		selectedFolder = "";
		sessionSelectedTemplate = -1;
		sessionSelectedFolder = "";
		sessionValues = captureValues();
		scrollOffset = 0;
		repositionAll();
	}

	private void saveTemplate() {
		commitTemplateRename();
		GameRuleTemplateStore.Entry entry = new GameRuleTemplateStore.Entry();
		entry.name = defaultTemplateName();
		entry.values = captureValues();
		entry.folder = TemplateOrg.norm(selectedFolder);
		GameRuleTemplateStore.add(entry);
		selectedTemplate = GameRuleTemplateStore.all().size() - 1;
		updateTemplateScroll();
	}

	private void loadTemplate(int index) {
		GameRuleTemplateStore.Entry entry = GameRuleTemplateStore.get(index);
		if (entry == null) return;
		selectedTemplate = index;
		selectedFolder = TemplateOrg.norm(entry.folder);
		applyValueMap(entry.values, true);
		scrollOffset = 0;
		repositionAll();
	}

	private void applyValueMap(Map<String, String> values, boolean missingUseDefault) {
		GameRules defaults = new GameRules();
		for (GameRuleEntry entry : entries) {
			String v = values != null ? values.get(entry.name) : null;
			if (v == null && missingUseDefault) v = defaultValue(entry, defaults);
			if (v == null) continue;
			setEntryValue(entry, v);
		}
	}

	private String defaultValue(GameRuleEntry entry, GameRules defaults) {
		if (entry.isBoolean) {
			@SuppressWarnings("unchecked")
			GameRules.Key<GameRules.BooleanValue> bk = (GameRules.Key<GameRules.BooleanValue>) entry.key;
			return String.valueOf(defaults.getBoolean(bk));
		}
		@SuppressWarnings("unchecked")
		GameRules.Key<GameRules.IntegerValue> ik = (GameRules.Key<GameRules.IntegerValue>) entry.key;
		return String.valueOf(defaults.getInt(ik));
	}

	private void setEntryValue(GameRuleEntry entry, String value) {
		if (entry.valueWidget instanceof Checkbox cb) {
			boolean want = Boolean.parseBoolean(value);
			if (cb.selected() != want) cb.onPress();
		} else if (entry.valueWidget instanceof EditBox eb) {
			eb.setValue(value);
		}
	}

	private String defaultTemplateName() {
		String name = "规则";
		int n = 2;
		while (templateNameTaken(name)) {
			name = "规则" + n++;
		}
		return name;
	}

	private boolean templateNameTaken(String name) {
		String folder = TemplateOrg.norm(selectedFolder);
		for (GameRuleTemplateStore.Entry e : GameRuleTemplateStore.all()) {
			if (TemplateOrg.folderEq(e.folder, folder) && e.name != null && e.name.equals(name)) return true;
		}
		return false;
	}

	private void repositionAll() {
		layoutPanel();
		if (applyBtn == null || saveBtn == null) return;
		int copyW = Math.max(52, this.font.width(Component.translatable("screen.bj_mapedit.copy")) + 16);
		int valueW = VALUE_W;
		int pad = 12;
		int bar = 8;
		int copyX = leftPos + WIDTH - pad - copyW - bar;
		int valueX = copyX - 8 - valueW;
		int y = topPos + CONTENT_TOP + 2 - scrollOffset;
		int totalHeight = 0;

		for (GameRuleEntry entry : entries) {
			entry.y = y;
			if (entry.valueWidget != null) {
				entry.valueWidget.setX(valueX);
				entry.valueWidget.setY(y + 3);
				if (entry.valueWidget instanceof EditBox eb) {
					eb.setWidth(valueW);
				}
			}
			if (entry.copyBtn != null) {
				entry.copyBtn.setX(copyX);
				entry.copyBtn.setY(y + 3);
				entry.copyBtn.setWidth(copyW);
				entry.copyBtn.setHeight(16);
			}
			y += ROW_H;
			totalHeight += ROW_H;
		}

		maxScroll = Math.max(0, totalHeight + 8 - (CONTENT_BOTTOM - CONTENT_TOP));
		scrollOffset = Mth.clamp(scrollOffset, 0, maxScroll);

		int btnH = 20;
		int footerY = topPos + HEIGHT - FOOTER_H + (FOOTER_H - btnH) / 2;
		int saveW = Math.max(72, this.font.width(saveBtn.getMessage()) + 22);
		int applyW = Math.max(72, this.font.width(applyBtn.getMessage()) + 22);
		applyBtn.setPosition(leftPos + 8, footerY);
		applyBtn.setWidth(applyW);
		applyBtn.setHeight(btnH);
		saveBtn.setPosition(leftPos + WIDTH - pad - saveW, footerY);
		saveBtn.setWidth(saveW);
		saveBtn.setHeight(btnH);
		syncTemplateRenameBox();
	}

	private boolean handleFooterClick(double mx, double my, int button) {
		if (!UiTheme.inFooterBar(mx, my, leftPos, topPos, WIDTH, HEIGHT, FOOTER_H)) return false;
		if (UiTheme.clickWidgets(mx, my, button, applyBtn, saveBtn)) return true;
		if (button == 0) setFocused(null);
		return true;
	}

	private int nameMaxW() {
		int copyW = Math.max(52, this.font.width(Component.translatable("screen.bj_mapedit.copy")) + 16);
		int copyX = leftPos + WIDTH - 12 - copyW - 8;
		int valueX = copyX - 8 - VALUE_W;
		return Math.max(40, valueX - (leftPos + NAME_X) - 8);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (inTemplateRail(mouseX, mouseY)) {
			templateScroll = Mth.clamp(templateScroll - (int) (scrollY * 20), 0, templateMaxScroll);
			syncTemplateRenameBox();
			return true;
		}
		if (mouseX >= leftPos && mouseX < leftPos + WIDTH
			&& mouseY >= topPos + CONTENT_TOP && mouseY < topPos + CONTENT_BOTTOM) {
			scrollOffset = Mth.clamp(scrollOffset - (int) (scrollY * 20), 0, maxScroll);
			repositionAll();
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		super.renderBackground(g, mouseX, mouseY, partialTick);
		UiTheme.drawPanel(g, leftPos, topPos, WIDTH, HEIGHT);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		this.renderBackground(g, mouseX, mouseY, partialTick);
		UiTheme.drawHeader(g, font, this.title, leftPos, topPos, WIDTH);
		layoutRefreshIcon();
		boolean refreshHover = UiTheme.hitIcon(refreshX, refreshY, mouseX, mouseY);
		UiTheme.drawRefreshIcon(g, refreshX, refreshY, refreshHover);
		drawTemplateRail(g, mouseX, mouseY);
		int fy = topPos + HEIGHT - FOOTER_H;
		g.fill(leftPos + 1, fy, leftPos + WIDTH - 1, topPos + HEIGHT - 1, 0xFF191A1B);
		g.fill(leftPos + 1, fy, leftPos + WIDTH - 1, fy + 1, 0xFF2B2C2D);

		g.enableScissor(leftPos + 1, topPos + CONTENT_TOP - 2, leftPos + WIDTH - 1, topPos + CONTENT_BOTTOM + 2);
		for (var child : this.children()) {
			if (child instanceof AbstractWidget w && w != applyBtn && w != saveBtn && w != templateRenameBox
				&& w.getY() < topPos + HEIGHT - FOOTER_H) {
				w.render(g, mouseX, mouseY, partialTick);
			}
		}
		int labelW = nameMaxW();
		for (GameRuleEntry entry : entries) {
			int wy = entry.y;
			if (wy + ROW_H > topPos + CONTENT_TOP && wy < topPos + CONTENT_BOTTOM) {
				String key = "gamerule." + entry.name;
				Component label = Component.translatable(key);
				String shown = label.getString();
				if (shown.equals(key) || shown.isEmpty()) {
					shown = GAMERULE_CN.getOrDefault(entry.name, entry.name);
				}
				UiTheme.clipLabel(g, font, shown, leftPos + NAME_X, wy + 6, labelW, UiTheme.cLabel());
			}
		}
		g.disableScissor();

		if (saveBtn != null) saveBtn.render(g, mouseX, mouseY, partialTick);
		if (applyBtn != null) applyBtn.render(g, mouseX, mouseY, partialTick);
		UiTheme.drawThinScrollBar(g, leftPos + WIDTH - 8, topPos + CONTENT_TOP, CONTENT_BOTTOM - CONTENT_TOP, scrollOffset, maxScroll);
		templateMenu.draw(g, font, mouseX, mouseY);
		if (refreshHover) {
			g.renderTooltip(font, Component.translatable("screen.bj_mapedit.reset"), mouseX, mouseY);
		}
	}

	@Override
	public void removed() {
		if (!entries.isEmpty()) {
			sessionValues = captureValues();
			sessionSelectedTemplate = selectedTemplate;
			sessionSelectedFolder = selectedFolder;
		}
		super.removed();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == 0 && UiTheme.consumeRefreshClick(refreshX, refreshY, mouseX, mouseY, this::resetFields)) {
			return true;
		}
		if (handleTemplateMenuClick(mouseX, mouseY)) return true;
		if (button == 0) {
			UiTheme.ScrollClick tsc = UiTheme.clickBar(templateScrollBarX(), templateListTop(), templateListH(), templateScroll, templateMaxScroll, mouseX, mouseY);
			if (tsc != null) {
				draggingTemplateScroll = true;
				templateScrollGrab = tsc.grab;
				if (tsc.scroll != templateScroll) {
					templateScroll = tsc.scroll;
					syncTemplateRenameBox();
				}
				return true;
			}
		}
		if (handleTemplateRailClick(mouseX, mouseY, button)) return true;
		if (button == 0) {
			UiTheme.ScrollClick sc = UiTheme.clickBar(leftPos + WIDTH - 8, topPos + CONTENT_TOP, CONTENT_BOTTOM - CONTENT_TOP, scrollOffset, maxScroll, mouseX, mouseY);
			if (sc != null) {
				draggingScroll = true;
				scrollGrabOffset = sc.grab;
				if (sc.scroll != scrollOffset) {
					scrollOffset = sc.scroll;
					repositionAll();
				}
				return true;
			}
		}
		if (handleFooterClick(mouseX, mouseY, button)) return true;
		boolean result = super.mouseClicked(mouseX, mouseY, button);
		if (button == 0 && !result) setFocused(null);
		return result;
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
		if (draggingTemplateScroll && button == 0) {
			int next = UiTheme.scrollAtGrab(templateListTop(), templateListH(), templateMaxScroll, mouseY, templateScrollGrab);
			if (next != templateScroll) {
				templateScroll = next;
				syncTemplateRenameBox();
			}
			return true;
		}
		if (draggingScroll && button == 0) {
			int next = UiTheme.scrollAtGrab(topPos + CONTENT_TOP, CONTENT_BOTTOM - CONTENT_TOP, maxScroll, mouseY, scrollGrabOffset);
			if (next != scrollOffset) {
				scrollOffset = next;
				repositionAll();
			}
			return true;
		}
		if (button == 0 && updateTemplateDrag(mouseX, mouseY)) return true;
		return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (button == 0) {
			boolean moved = finishTemplateDrag();
			draggingScroll = false;
			draggingTemplateScroll = false;
			if (moved) return true;
		}
		return super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (templateMenu.isOpen() && keyCode == 256) {
			templateMenu.close();
			templateMenuRow = null;
			return true;
		}
		if (isRenamingTemplate() && templateRenameBox != null && templateRenameBox.visible) {
			if (keyCode == 257 || keyCode == 335) {
				commitTemplateRename();
				return true;
			}
			if (keyCode == 256) {
				cancelTemplateRename();
				return true;
			}
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	private boolean inTemplateRail(double mouseX, double mouseY) {
		return mouseX >= templateRailX && mouseX < templateRailX + templateRailW
			&& mouseY >= topPos && mouseY < topPos + HEIGHT;
	}

	private int templateListTop() {
		return topPos + UiTheme.HEADER_H + 6;
	}

	private int templateListBottom() {
		return topPos + HEIGHT - 8;
	}

	private int templateListH() {
		return Math.max(0, templateListBottom() - templateListTop());
	}

	private List<TemplateOrg.Row> templateRows() {
		return GameRuleTemplateStore.rows(collapsedFolders);
	}

	private boolean isRenamingTemplate() {
		return renamingTemplate >= 0 || !renamingFolder.isEmpty();
	}

	private void updateTemplateScroll() {
		int content = templateRows().size() * TEMPLATE_ROW_H;
		templateMaxScroll = Math.max(0, content - templateListH());
		templateScroll = Mth.clamp(templateScroll, 0, templateMaxScroll);
	}

	private int templateScrollBarX() {
		return templateRailX + templateRailW - 5;
	}

	private int templateActionRight() {
		return templateRailX + templateRailW - 8 - (templateMaxScroll > 0 ? 6 : 0);
	}

	private int templateRowY(int index) {
		return templateListTop() + index * TEMPLATE_ROW_H - templateScroll;
	}

	private int templateIndexAt(double mouseY) {
		return TemplateRailUi.indexAt(mouseY, templateListTop(), templateListBottom(), templateScroll, TEMPLATE_ROW_H, templateRows().size());
	}

	private void drawTemplateRail(GuiGraphics g, int mouseX, int mouseY) {
		int hx = mouseX;
		int hy = mouseY;
		if (templateMenu.covers(mouseX, mouseY)) {
			hx = -10000;
			hy = -10000;
		}
		int x = templateRailX;
		int w = templateRailW;
		updateTemplateScroll();
		UiTheme.drawPanel(g, x, topPos, w, HEIGHT);
		boolean hasSel = TemplateOrg.hasSel(selectedTemplate, selectedFolder);
		TemplateRailUi.drawHeader(g, font, Component.translatable("screen.bj_mapedit.rule_templates").getString(), x, w, topPos, hx, hy, hasSel);
		TemplateRailUi.drawRows(
			g, font, x, w, templateListTop(), templateListBottom(), TEMPLATE_ROW_H, templateScroll,
			templateRows(), selectedTemplate, selectedFolder, renamingTemplate, renamingFolder, templateRenameBox,
			templateActionRight(), hx, hy,
			templateDrag.active ? templateDrag.hoverFolder : null,
			templateDrag.active ? templateDrag.storeIndex : -1
		);
		UiTheme.drawThinScrollBar(g, templateScrollBarX(), templateListTop(), templateListH(), templateScroll, templateMaxScroll);
		TemplateRailUi.drawDragOverlay(g, font, templateDrag, hx, hy, x, w, topPos);
	}

	private boolean handleTemplateMenuClick(double mouseX, double mouseY) {
		RenameDeleteMenu.Action a = templateMenu.pick(mouseX, mouseY);
		TemplateOrg.Row row = templateMenuRow;
		if (a == RenameDeleteMenu.Action.RENAME && row != null) {
			templateMenuRow = null;
			if (row.folder) startFolderRename(row.folderName);
			else startTemplateRename(row.storeIndex);
			return true;
		}
		if (a == RenameDeleteMenu.Action.DELETE && row != null) {
			templateMenuRow = null;
			if (row.folder) deleteTemplateFolder(row.folderName);
			else deleteTemplate(row.storeIndex);
			return true;
		}
		if (a == RenameDeleteMenu.Action.DISMISS) templateMenuRow = null;
		return false;
	}

	private boolean handleTemplateRailClick(double mouseX, double mouseY, int button) {
		if (!inTemplateRail(mouseX, mouseY)) {
			if (isRenamingTemplate()) commitTemplateRename();
			return false;
		}
		if (UiTheme.hitScrollBar(templateScrollBarX(), templateListTop(), templateListH(), mouseX, mouseY)) {
			return false;
		}
		boolean hasSel = TemplateOrg.hasSel(selectedTemplate, selectedFolder);
		if (mouseY >= topPos && mouseY < topPos + UiTheme.HEADER_H) {
			if (button == 0 && UiTheme.hitNewFolder(templateRailX, templateRailW, topPos, font, mouseX, mouseY, hasSel)) {
				createTemplateFolder();
				return true;
			}
			if (isRenamingTemplate()) commitTemplateRename();
			return true;
		}
		if (isRenamingTemplate() && templateRenameBox != null && templateRenameBox.visible
			&& templateRenameBox.isMouseOver(mouseX, mouseY)) {
			return false;
		}
		List<TemplateOrg.Row> rows = templateRows();
		int idx = templateIndexAt(mouseY);
		if (idx < 0) {
			commitTemplateRename();
			setFocused(null);
			return true;
		}
		TemplateOrg.Row row = rows.get(idx);
		if (button == 1) {
			if (isRenamingTemplate()) commitTemplateRename();
			templateMenuRow = row;
			templateMenu.show(font, (int) mouseX, (int) mouseY, this.width, this.height);
			return true;
		}
		if (button != 0) return true;
		if (row.folder ? TemplateOrg.folderEq(renamingFolder, row.folderName) : renamingTemplate == row.storeIndex) {
			return true;
		}
		commitTemplateRename();
		if (row.folder) {
			clickTemplateFolder(row.folderName);
		} else {
			templateDrag.press(row.storeIndex, row.folderName, row.name, row.storeIndex == selectedTemplate, mouseX, mouseY);
		}
		setFocused(null);
		return true;
	}

	private boolean updateTemplateDrag(double mouseX, double mouseY) {
		if (!templateDrag.move(mouseX, mouseY)) return false;
		templateDrag.hoverFolder = TemplateRailUi.dropTarget(
			mouseX, mouseY, templateRailX, templateRailW, topPos, HEIGHT,
			templateListTop(), templateListBottom(), templateScroll, TEMPLATE_ROW_H, templateRows()
		);
		return true;
	}

	private boolean finishTemplateDrag() {
		if (!templateDrag.busy()) return false;
		int index = templateDrag.storeIndex;
		boolean click = templateDrag.wasClick();
		if (templateDrag.shouldMove()) {
			GameRuleTemplateStore.moveToFolder(index, templateDrag.hoverFolder);
			selectedFolder = TemplateOrg.norm(templateDrag.hoverFolder);
			if (!selectedFolder.isEmpty()) collapsedFolders.remove(selectedFolder);
			updateTemplateScroll();
		} else if (click) {
			loadTemplate(index);
		}
		templateDrag.reset();
		return true;
	}

	private void clearTemplateSelection() {
		selectedTemplate = -1;
		selectedFolder = "";
		sessionSelectedTemplate = -1;
		sessionSelectedFolder = "";
	}

	private void createTemplateFolder() {
		commitTemplateRename();
		String name = GameRuleTemplateStore.addFolder();
		collapsedFolders.remove(name);
		selectedFolder = name;
		selectedTemplate = -1;
		updateTemplateScroll();
	}

	private void clickTemplateFolder(String name) {
		String folder = TemplateOrg.norm(name);
		selectedTemplate = -1;
		selectedFolder = folder;
		if (!collapsedFolders.add(folder)) collapsedFolders.remove(folder);
	}

	private void deleteTemplateFolder(String name) {
		if (TemplateOrg.folderEq(renamingFolder, name)) cancelTemplateRename();
		else commitTemplateRename();
		GameRuleTemplateStore.Entry kept = selectedTemplate >= 0 ? GameRuleTemplateStore.get(selectedTemplate) : null;
		GameRuleTemplateStore.removeFolder(name);
		collapsedFolders.remove(TemplateOrg.norm(name));
		if (TemplateOrg.folderEq(selectedFolder, name)) {
			selectedFolder = "";
			selectedTemplate = -1;
		} else if (kept != null) {
			selectedTemplate = GameRuleTemplateStore.all().indexOf(kept);
		}
		updateTemplateScroll();
	}

	private void deleteTemplate(int index) {
		if (renamingTemplate == index) cancelTemplateRename();
		else commitTemplateRename();
		GameRuleTemplateStore.remove(index);
		if (selectedTemplate == index) selectedTemplate = -1;
		else if (selectedTemplate > index) selectedTemplate--;
		updateTemplateScroll();
	}

	private void startFolderRename(String name) {
		if (templateRenameBox == null) return;
		if (isRenamingTemplate() && !TemplateOrg.folderEq(renamingFolder, name)) {
			commitTemplateRename();
		}
		renamingTemplate = -1;
		renamingFolder = TemplateOrg.norm(name);
		templateRenameBox.setValue(renamingFolder);
		templateRenameBox.visible = true;
		syncTemplateRenameBox();
		setFocused(templateRenameBox);
		templateRenameBox.setFocused(true);
		templateRenameBox.moveCursorToEnd(false);
		setHighlightPos(templateRenameBox, 0);
	}

	private void startTemplateRename(int index) {
		GameRuleTemplateStore.Entry entry = GameRuleTemplateStore.get(index);
		if (entry == null || templateRenameBox == null) return;
		if (isRenamingTemplate() && renamingTemplate != index) {
			commitTemplateRename();
		}
		renamingFolder = "";
		renamingTemplate = index;
		templateRenameBox.setValue(entry.name == null ? "" : entry.name);
		templateRenameBox.visible = true;
		syncTemplateRenameBox();
		setFocused(templateRenameBox);
		templateRenameBox.setFocused(true);
		templateRenameBox.moveCursorToEnd(false);
		setHighlightPos(templateRenameBox, 0);
	}

	private void syncTemplateRenameBox() {
		List<TemplateOrg.Row> rows = templateRows();
		int rowIdx = TemplateRailUi.renamingRow(rows, renamingTemplate, renamingFolder);
		if (templateRenameBox == null || rowIdx < 0) {
			if (templateRenameBox != null) {
				templateRenameBox.visible = false;
				templateRenameBox.setY(-1000);
			}
			return;
		}
		int x = TemplateOrg.rowNameX(templateRailX, rows.get(rowIdx).depth);
		int y = templateRowY(rowIdx) + 3;
		int w = Math.max(24, templateActionRight() - x - 4);
		templateRenameBox.setX(x);
		templateRenameBox.setY(y);
		templateRenameBox.setWidth(w);
		templateRenameBox.visible = true;
	}

	private void commitTemplateRename() {
		if (templateRenameBox == null) return;
		if (!renamingFolder.isEmpty()) {
			String from = renamingFolder;
			String to = templateRenameBox.getValue();
			GameRuleTemplateStore.renameFolder(from, to);
			String normTo = TemplateOrg.norm(to);
			if (!normTo.isEmpty() && !TemplateOrg.folderEq(from, normTo)) {
				TemplateOrg.renameCollapsed(collapsedFolders, from, normTo);
				if (TemplateOrg.folderEq(selectedFolder, from)) selectedFolder = normTo;
			}
			cancelTemplateRename();
			return;
		}
		if (renamingTemplate < 0) return;
		GameRuleTemplateStore.rename(renamingTemplate, templateRenameBox.getValue());
		cancelTemplateRename();
	}

	private void cancelTemplateRename() {
		renamingTemplate = -1;
		renamingFolder = "";
		if (templateRenameBox != null) {
			templateRenameBox.visible = false;
			templateRenameBox.setY(-1000);
			if (templateRenameBox.isFocused()) setFocused(null);
		}
	}

	private static void setHighlightPos(EditBox box, int pos) {
		try {
			java.lang.reflect.Field f = EditBox.class.getDeclaredField("highlightPos");
			f.setAccessible(true);
			f.set(box, pos);
		} catch (Exception ignored) {}
	}
}
