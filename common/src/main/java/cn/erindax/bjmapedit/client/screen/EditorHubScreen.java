package cn.erindax.bjmapedit.client.screen;

import cn.erindax.bjmapedit.client.widget.UiTheme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class EditorHubScreen extends Screen {
	private static final int BTN_H = 20;
	private static final int GAP = 6;
	private static final int PAD = 10;

	private record Entry(String title, Item icon, Supplier<Screen> open) {}

	private static final List<Entry> ENTRIES = List.of(
		new Entry("screen.bj_mapedit.title", Items.NAME_TAG, ItemEditorScreen::new),
		new Entry("screen.bj_mapedit.book_title", Items.WRITTEN_BOOK, BookEditorScreen::new),
		new Entry("screen.bj_mapedit.gamerule_title", Items.COMMAND_BLOCK, GameRuleEditorScreen::new),
		new Entry("screen.bj_mapedit.entity_title", Items.ITEM_FRAME, FrameStandEditorScreen::new),
		new Entry("screen.bj_mapedit.entity2_title", Items.PIG_SPAWN_EGG, EntityEditorScreen::new),
		new Entry("screen.bj_mapedit.villager_title", Items.EMERALD, VillagerEditorScreen::new),
		new Entry("screen.bj_mapedit.datapack_title", Items.PAPER, DatapackEditorScreen::new),
		new Entry("screen.bj_mapedit.recipe_title", Items.CRAFTING_TABLE, RecipeEditorScreen::new),
		new Entry("screen.bj_mapedit.adv_title", Items.KNOWLEDGE_BOOK, AdvancementEditorScreen::new)
	);

	private final List<Button> buttons = new ArrayList<>();
	private int leftPos;
	private int topPos;
	private int panelW;
	private int panelH;

	public EditorHubScreen() {
		super(Component.translatable("screen.bj_mapedit.hub_title"));
	}

	@Override
	protected void init() {
		buttons.clear();
		int textW = font.width(this.title);
		for (Entry e : ENTRIES) textW = Math.max(textW, font.width(Component.translatable(e.title())));
		int btnW = Mth.clamp(textW + 48, 110, 200);
		int cols = Mth.clamp((this.width - 16 - PAD * 2 + GAP) / (btnW + GAP), 1, 3);
		int rows = (ENTRIES.size() + cols - 1) / cols;
		panelW = PAD * 2 + cols * btnW + (cols - 1) * GAP;
		panelH = UiTheme.HEADER_H + PAD * 2 + rows * BTN_H + (rows - 1) * GAP;
		leftPos = (this.width - panelW) / 2;
		topPos = Math.max(8, (this.height - panelH) / 2);
		for (int i = 0; i < ENTRIES.size(); i++) {
			Entry e = ENTRIES.get(i);
			int x = leftPos + PAD + (i % cols) * (btnW + GAP);
			int y = topPos + UiTheme.HEADER_H + PAD + (i / cols) * (BTN_H + GAP);
			buttons.add(this.addRenderableWidget(Button.builder(Component.translatable(e.title()), b -> open(e))
				.bounds(x, y, btnW, BTN_H).build()));
		}
	}

	private void open(Entry e) {
		if (this.minecraft != null) this.minecraft.setScreen(e.open().get());
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
		for (int i = 0; i < buttons.size() && i < ENTRIES.size(); i++) {
			Button b = buttons.get(i);
			g.renderItem(new ItemStack(ENTRIES.get(i).icon()), b.getX() + 3, b.getY() + 2);
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
