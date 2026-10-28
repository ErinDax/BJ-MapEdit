package cn.erindax.bjmapedit.client.widget;

import com.mojang.blaze3d.platform.Lighting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

public final class InvPicker {
	private static final int COLS = 9;
	private static final int PAD = 12;
	private static final int SLOTS = 36;

	private boolean open;

	public boolean isOpen() {
		return open;
	}

	public void open() {
		open = true;
	}

	public void close() {
		open = false;
	}

	public boolean inPanel(int screenW, int screenH, double mx, double my) {
		int px = panelX(screenW, screenH);
		int py = panelY(screenW, screenH);
		return mx >= px && mx < px + panelW(screenW, screenH) && my >= py && my < py + panelH(screenW, screenH);
	}

	public ItemStack stackAt(int screenW, int screenH, double mx, double my) {
		var player = Minecraft.getInstance().player;
		if (player == null) return ItemStack.EMPTY;
		int slot = slotAt(screenW, screenH, mx, my);
		if (slot < 0) return ItemStack.EMPTY;
		ItemStack stack = player.getInventory().getItem(slot);
		return stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
	}

	public void render(GuiGraphics g, Font font, int screenW, int screenH, int mouseX, int mouseY) {
		var player = Minecraft.getInstance().player;
		if (!open || player == null) return;
		var inventory = player.getInventory();
		int cell = cell(screenW, screenH);
		int px = panelX(screenW, screenH);
		int py = panelY(screenW, screenH);
		UiTheme.pushOverlay(g);
		g.fill(0, 0, screenW, screenH, 0xB0121314);
		UiTheme.drawPanel(g, px, py, panelW(screenW, screenH), panelH(screenW, screenH));
		Lighting.setupFor3DItems();
		ItemStack tip = ItemStack.EMPTY;
		for (int i = 0; i < SLOTS; i++) {
			int cx = slotX(screenW, screenH, i);
			int cy = slotY(screenW, screenH, i);
			boolean hot = mouseX >= cx && mouseX < cx + cell && mouseY >= cy && mouseY < cy + cell;
			if (hot) g.fill(cx, cy, cx + cell, cy + cell, 0xFF30363D);
			UiTheme.drawSlot(g, cx + 1, cy + 1, cell - 2);
			ItemStack stack = inventory.getItem(i);
			if (!stack.isEmpty()) {
				int ix = cx + (cell - 16) / 2;
				int iy = cy + (cell - 16) / 2;
				g.renderItem(stack, ix, iy);
				g.renderItemDecorations(font, stack, ix, iy);
				if (hot) tip = stack;
			}
		}
		if (!tip.isEmpty()) g.renderTooltip(font, tip.getHoverName(), mouseX, mouseY);
		UiTheme.popOverlay(g);
		Lighting.setupFor3DItems();
	}

	private int slotAt(int screenW, int screenH, double mx, double my) {
		if (!inPanel(screenW, screenH, mx, my)) return -1;
		int cell = cell(screenW, screenH);
		for (int i = 0; i < SLOTS; i++) {
			int cx = slotX(screenW, screenH, i);
			int cy = slotY(screenW, screenH, i);
			if (mx >= cx && mx < cx + cell && my >= cy && my < cy + cell) return i;
		}
		return -1;
	}

	private static int cell(int screenW, int screenH) {
		return Mth.clamp(Math.min(screenW, screenH) / 30, 22, 26);
	}

	private static int gap(int screenW, int screenH) {
		return Math.max(4, cell(screenW, screenH) / 4);
	}

	private static int panelW(int screenW, int screenH) {
		return COLS * cell(screenW, screenH) + PAD * 2;
	}

	private static int panelH(int screenW, int screenH) {
		return PAD * 2 + 4 * cell(screenW, screenH) + gap(screenW, screenH);
	}

	private static int panelX(int screenW, int screenH) {
		return (screenW - panelW(screenW, screenH)) / 2;
	}

	private static int panelY(int screenW, int screenH) {
		return (screenH - panelH(screenW, screenH)) / 2;
	}

	private static int slotX(int screenW, int screenH, int slot) {
		int col = slot < 9 ? slot : (slot - 9) % COLS;
		return panelX(screenW, screenH) + PAD + col * cell(screenW, screenH);
	}

	private static int slotY(int screenW, int screenH, int slot) {
		int cell = cell(screenW, screenH);
		int gy = panelY(screenW, screenH) + PAD;
		if (slot < 9) return gy + 3 * cell + gap(screenW, screenH);
		return gy + ((slot - 9) / COLS) * cell;
	}
}
