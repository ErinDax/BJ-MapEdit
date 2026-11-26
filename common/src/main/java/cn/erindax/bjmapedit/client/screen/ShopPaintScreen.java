package cn.erindax.bjmapedit.client.screen;

import cn.erindax.bjmapedit.client.widget.EditHistory;
import cn.erindax.bjmapedit.client.widget.ItemModels;
import cn.erindax.bjmapedit.client.widget.KeyCombos;
import cn.erindax.bjmapedit.client.widget.ResourcePacks;
import cn.erindax.bjmapedit.client.widget.ShopData;
import cn.erindax.bjmapedit.client.widget.ShopGuiArt;
import cn.erindax.bjmapedit.client.widget.ShopPack;
import cn.erindax.bjmapedit.client.widget.UiTheme;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.awt.Color;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Locale;

public class ShopPaintScreen extends Screen {
	private static final String K = "screen.bj_mapedit.";
	private static final int MODE_GUI = 0;
	private static final int MODE_BTN = 1;
	private static final int TOOL_PEN = 0;
	private static final int TOOL_ERASE = 1;
	private static final int TOOL_FILL = 2;
	private static final int TOOL_PICK = 3;
	private static final int TOOL_LINE = 4;
	private static final int TOOL_RECT = 5;
	private static final int TOOL_BOX = 6;
	private static final int TOOL_SLOT = 7;
	private static final int OK = 0xFF7EE787;
	private static final int WARN = 0xFFE3B341;
	private static final int ERR = 0xFFFF7B72;
	private static final int HINT = 0x6E7681;
	private static final int[] BTN_SIZES = {16, 18, 32};
	private static final int[] PALETTE = {
		0xFFC6C6C6, 0xFFFFFFFF, 0xFF555555, 0xFF8B8B8B, 0xFF373737, 0xFF000000,
		0xFF1F2328, 0xFF3B4252, 0xFF6E4B2A, 0xFFA67545, 0xFFD9B382, 0xFFF2E2C4,
		0xFFCF222E, 0xFFFF6B6B, 0xFFE88827, 0xFFF2CC60, 0xFF2EA043, 0xFF7EE787,
		0xFF1B9AAA, 0xFF58A6FF, 0xFF1F6FEB, 0xFF8957E5, 0xFFDB61A2, 0xFFA5D6FF
	};
	private static int texSeq;

	private record Snap(int w, int h, int[] pixels, long hiddenMask) {
		@Override
		public boolean equals(Object o) {
			return o instanceof Snap s && w == s.w && h == s.h && hiddenMask == s.hiddenMask && Arrays.equals(pixels, s.pixels);
		}

		@Override
		public int hashCode() {
			return Arrays.hashCode(pixels) * 31 + Long.hashCode(hiddenMask);
		}
	}

	private final ShopEditorScreen parent;
	private final ShopData shop;
	private final int slotIndex;
	private final EditHistory<Snap> guiHistory = new EditHistory<>();
	private final EditHistory<Snap> btnHistory = new EditHistory<>();

	private List<String> packs = new ArrayList<>();
	private int packIdx;
	private int mode = MODE_GUI;
	private int tool = TOOL_PEN;
	private int brush = 1;
	private int btnSizeIdx = 1;
	private boolean fullSlot18 = true;
	private boolean showSlots = true;
	private boolean showItems = true;
	private boolean showGrid = false;

	private int guiW;
	private int guiH;
	private int[] guiPixels;
	private int[] refBgPixels;

	private int btnW = 18;
	private int btnH = 18;
	private int[] btnPixels;

	private DynamicTexture dynTex;
	private ResourceLocation dynId;
	private int dynW;
	private int dynH;
	private boolean texDirty = true;

	private float zoom = 1.0f;
	private float panX;
	private float panY;
	private boolean viewInit;

	private float hue = 0.58f;
	private float sat = 0.72f;
	private float val = 0.92f;
	private int alpha = 255;
	private int color = 0xFF42A5EB;
	private boolean syncingHex;

	private boolean painting;
	private int strokeBtn = -1;
	private int startPx;
	private int startPy;
	private int lastPx;
	private int lastPy;
	private int curPx = -1;
	private int curPy = -1;
	private int[] shapeBase;
	private boolean slotEraseState;

	private boolean panning;
	private double panStartMx;
	private double panStartMy;
	private float panStartX;
	private float panStartY;

	private boolean dragSv;
	private boolean dragHue;
	private boolean dragAlpha;

	private ShopGuiArt.Img floatImg;
	private int floatX;
	private int floatY;
	private int floatW;
	private int floatH;
	private boolean dragFloatMove;
	private boolean dragFloatScale;
	private int dragFloatOffX;
	private int dragFloatOffY;

	private EditBox idField;
	private EditBox hexField;
	private Component status = Component.empty();
	private int statusColor = OK;
	private boolean opened;
	private Path initialDrop;

	private int leftPos;
	private int topPos;
	private int panelW;
	private int panelH;
	private int topBarY;
	private int bodyTop;
	private int bodyBottom;
	private int footerY;
	private int leftColX;
	private int leftColW = 104;
	private int rightColX;
	private int rightColW = 86;
	private int viewX;
	private int viewY;
	private int viewW;
	private int viewH;
	private int svX, svY, svW, svH;
	private int hueX, hueY, hueW, hueH;
	private int alX, alY, alW, alH;
	private int palX, palY;
	private int packMinusX, packPlusX, packBoxX, packBoxW;

	public ShopPaintScreen(ShopEditorScreen parent, ShopData shop, int slotIndex) {
		this(parent, shop, slotIndex, null);
	}

	public ShopPaintScreen(ShopEditorScreen parent, ShopData shop, int slotIndex, Path droppedFile) {
		super(Component.translatable(K + "sp_title"));
		this.parent = parent;
		this.shop = shop;
		this.slotIndex = Mth.clamp(slotIndex, 0, shop.size() - 1);
		this.initialDrop = droppedFile;
	}

	@Override
	protected void init() {
		boolean first = !opened;
		opened = true;
		String keepPack = first ? (shop.guiPack.isBlank() ? null : shop.guiPack) : currentPack();
		packs = ResourcePacks.folderPacks();
		int found = keepPack == null ? -1 : packs.indexOf(keepPack);
		packIdx = found >= 0 ? found : defaultPackIdx();
		if (first) {
			initCanvases();
			setColorFromArgb(0xFFC6C6C6, true);
		}
		computeLayout();
		idField = new EditBox(font, rightColX - 66, topBarY + 1, 66, 15, Component.empty());
		idField.setMaxLength(24);
		idField.setFilter(s -> s.matches("[a-z0-9_]{0,24}"));
		idField.setHint(Component.translatable(K + "shop_id_hint").withStyle(Style.EMPTY.withColor(HINT)));
		idField.setValue(shop.id);
		idField.setResponder(s -> {
			if (!s.equals(shop.id)) {
				shop.id = s;
				parent.onPaintMetaChanged();
			}
		});
		this.addRenderableWidget(idField);

		hexField = new EditBox(font, rightColX + 22, bodyTop, rightColW - 22, 15, Component.empty());
		hexField.setMaxLength(9);
		hexField.setFilter(s -> s.matches("#?[0-9A-Fa-f]{0,8}"));
		hexField.setResponder(this::onHexTyped);
		this.addRenderableWidget(hexField);
		syncHexBox();
		updateWidgetVis();
		if (!viewInit) {
			fitView();
			viewInit = true;
		}
		if (initialDrop != null) {
			Path p = initialDrop;
			initialDrop = null;
			importFile(p);
		}
	}

	private void initCanvases() {
		ShopGuiArt.Img vanilla = ShopGuiArt.grabContainerBg(shop.container, false);
		guiW = vanilla.w();
		guiH = vanilla.h();
		refBgPixels = vanilla.pixels().clone();
		ShopGuiArt.Img existing = ShopGuiArt.loadSavedGui(shop);
		if (existing != null) {
			ShopGuiArt.Img adapted = ShopGuiArt.adaptHeight(existing, shop.container);
			guiPixels = adapted.pixels();
		} else {
			guiPixels = vanilla.pixels().clone();
			for (int i = 0; i < shop.size(); i++) {
				if (shop.isHidden(i)) {
					ShopGuiArt.eraseSlot(guiPixels, guiW, guiH, i, ShopGuiArt.samplePanelColor(refBgPixels, guiW, guiH));
				}
			}
		}
		btnW = BTN_SIZES[btnSizeIdx];
		btnH = btnW;
		btnPixels = new int[btnW * btnH];
		ShopData.Slot sl = shop.slots[slotIndex];
		if (sl.shown()) {
			ShopGuiArt.Img icon = ShopGuiArt.grabItemIcon(sl.item);
			if (icon != null) {
				btnPixels = ShopGuiArt.scaleNearest(icon, btnW, btnH).pixels();
			} else {
				btnPixels = ShopGuiArt.grabSlotPatch(refBgPixels, guiW, guiH, slotIndex, btnW).pixels();
			}
		} else {
			btnPixels = ShopGuiArt.grabSlotPatch(refBgPixels, guiW, guiH, slotIndex, btnW).pixels();
		}
		texDirty = true;
	}

	private void computeLayout() {
		panelW = Mth.clamp(this.width - 12, 408, 610);
		panelH = Mth.clamp(this.height - 8, 232, 360);
		leftPos = (this.width - panelW) / 2;
		topPos = Math.max(4, (this.height - panelH) / 2);
		topBarY = topPos + UiTheme.HEADER_H + 2;
		footerY = topPos + panelH - 22;
		bodyTop = topBarY + 19;
		bodyBottom = footerY - 4;
		leftColX = leftPos + 6;
		rightColX = leftPos + panelW - 6 - rightColW;
		viewX = leftColX + leftColW + 6;
		viewY = bodyTop;
		viewW = Math.max(80, rightColX - 6 - viewX);
		viewH = Math.max(80, bodyBottom - bodyTop);

		svX = rightColX;
		svY = bodyTop + 18;
		svW = rightColW;
		svH = 46;
		hueX = rightColX;
		hueY = svY + svH + 3;
		hueW = rightColW;
		hueH = 7;
		alX = rightColX;
		alY = hueY + hueH + 3;
		alW = rightColW;
		alH = 8;
		palX = rightColX;
		palY = alY + alH + 4;

		int modeEnd = leftColX + 114;
		int rightReserve = mode == MODE_GUI ? 92 : 98;
		int avail = (leftPos + panelW - 6 - rightReserve) - modeEnd;
		packMinusX = modeEnd + 4;
		packBoxX = packMinusX + 14;
		packBoxW = Mth.clamp(avail - 34, 48, 160);
		packPlusX = packBoxX + packBoxW + 2;
	}

	private void updateWidgetVis() {
		computeLayout();
		if (idField != null) {
			idField.visible = mode == MODE_GUI;
			idField.setPosition(leftPos + panelW - 6 - 68, topBarY + 1);
			idField.setWidth(68);
		}
		if (hexField != null) {
			hexField.setPosition(rightColX + 22, bodyTop);
			hexField.setWidth(rightColW - 22);
		}
	}

	private int defaultPackIdx() {
		List<String> selected = new ArrayList<>(Minecraft.getInstance().getResourcePackRepository().getSelectedIds());
		for (int i = selected.size() - 1; i >= 0; i--) {
			String id = selected.get(i);
			if (!id.startsWith("file/")) continue;
			int idx = packs.indexOf(id.substring("file/".length()));
			if (idx >= 0) return idx;
		}
		return 0;
	}

	private String currentPack() {
		return packs.isEmpty() ? null : packs.get(Mth.clamp(packIdx, 0, packs.size() - 1));
	}

	private int canvasW() {
		return mode == MODE_GUI ? guiW : btnW;
	}

	private int canvasH() {
		return mode == MODE_GUI ? guiH : btnH;
	}

	private int[] canvasPixels() {
		return mode == MODE_GUI ? guiPixels : btnPixels;
	}

	private EditHistory<Snap> history() {
		return mode == MODE_GUI ? guiHistory : btnHistory;
	}

	private Snap currentSnap() {
		return new Snap(canvasW(), canvasH(), canvasPixels().clone(), mode == MODE_GUI ? shop.hiddenMask : 0L);
	}

	private void restoreSnap(Snap s) {
		if (s == null) return;
		if (mode == MODE_GUI) {
			guiW = s.w;
			guiH = s.h;
			guiPixels = s.pixels.clone();
			shop.hiddenMask = s.hiddenMask;
			syncDraftToShop();
		} else {
			btnW = s.w;
			btnH = s.h;
			btnPixels = s.pixels.clone();
			for (int i = 0; i < BTN_SIZES.length; i++) {
				if (BTN_SIZES[i] == btnW) btnSizeIdx = i;
			}
		}
		texDirty = true;
	}

	private void recordHistory(String kind) {
		history().record(currentSnap(), kind);
	}

	private void syncDraftToShop() {
		shop.draftGuiW = guiW;
		shop.draftGuiH = guiH;
		shop.draftGuiPixels = guiPixels.clone();
		shop.guiUnapplied = true;
		parent.onPaintMetaChanged();
	}

	private void fitView() {
		int cw = canvasW();
		int ch = canvasH();
		float zx = (viewW - 12.0f) / Math.max(1, cw);
		float zy = (viewH - 12.0f) / Math.max(1, ch);
		zoom = Mth.clamp(Math.min(zx, zy), 0.5f, mode == MODE_GUI ? 4.0f : 10.0f);
		if (zoom > 1.05f && zoom < 1.95f && viewW >= cw + 4 && viewH >= ch + 4) {
			zoom = 1.0f;
		}
		panX = viewX + (viewW - cw * zoom) * 0.5f;
		panY = viewY + (viewH - ch * zoom) * 0.5f;
	}

	private void switchMode(int nextMode) {
		if (mode == nextMode) return;
		commitFloatIfAny();
		mode = nextMode;
		if (mode == MODE_BTN && tool == TOOL_SLOT) tool = TOOL_PEN;
		updateWidgetVis();
		fitView();
		texDirty = true;
	}

	private void changeBtnSize(int idx) {
		if (idx == btnSizeIdx && btnW == BTN_SIZES[idx]) return;
		commitFloatIfAny();
		recordHistory("resize");
		btnSizeIdx = idx;
		int nw = BTN_SIZES[idx];
		ShopGuiArt.Img scaled = ShopGuiArt.scaleNearest(new ShopGuiArt.Img(btnW, btnH, btnPixels), nw, nw);
		btnW = nw;
		btnH = nw;
		btnPixels = scaled.pixels();
		fitView();
		texDirty = true;
	}

	private void setColorFromArgb(int argb, boolean updateHex) {
		alpha = (argb >>> 24) & 0xFF;
		int r = (argb >>> 16) & 0xFF;
		int g = (argb >>> 8) & 0xFF;
		int b = argb & 0xFF;
		float[] hsb = Color.RGBtoHSB(r, g, b, null);
		if (hsb[1] > 0.001f) hue = hsb[0];
		sat = hsb[1];
		val = hsb[2];
		color = argb;
		if (updateHex) syncHexBox();
	}

	private void rebuildColorFromHsv() {
		int rgb = Color.HSBtoRGB(hue, sat, val) & 0xFFFFFF;
		color = (alpha << 24) | rgb;
		syncHexBox();
	}

	private void syncHexBox() {
		if (hexField == null) return;
		syncingHex = true;
		if (alpha == 255) {
			hexField.setValue(String.format(Locale.ROOT, "#%06X", color & 0xFFFFFF));
		} else {
			hexField.setValue(String.format(Locale.ROOT, "#%08X", color));
		}
		syncingHex = false;
	}

	private void onHexTyped(String raw) {
		if (syncingHex) return;
		String s = raw.startsWith("#") ? raw.substring(1) : raw;
		if (s.length() == 6) {
			try {
				int rgb = Integer.parseInt(s, 16);
				setColorFromArgb((alpha << 24) | rgb, false);
			} catch (NumberFormatException ignored) {}
		} else if (s.length() == 8) {
			try {
				int argb = Integer.parseUnsignedInt(s, 16);
				setColorFromArgb(argb, false);
			} catch (NumberFormatException ignored) {}
		}
	}

	private void setStatus(Component msg, int col) {
		status = msg;
		statusColor = col;
	}

	private void ensureTexture() {
		int cw = canvasW();
		int ch = canvasH();
		if (dynTex == null || dynW != cw || dynH != ch) {
			releaseTex();
			dynW = cw;
			dynH = ch;
			dynTex = new DynamicTexture(new NativeImage(NativeImage.Format.RGBA, cw, ch, false));
			dynTex.setFilter(false, false);
			dynId = ResourceLocation.fromNamespaceAndPath("bj_mapedit", "shop_paint/" + (++texSeq));
			Minecraft.getInstance().getTextureManager().register(dynId, dynTex);
			texDirty = true;
		}
		if (!texDirty) return;
		texDirty = false;
		NativeImage img = dynTex.getPixels();
		if (img == null) return;
		int[] src = canvasPixels();
		int[] work = floatImg != null ? compositeFloat(src, cw, ch) : src;
		for (int y = 0; y < ch; y++) {
			int row = y * cw;
			for (int x = 0; x < cw; x++) {
				img.setPixelRGBA(x, y, ShopGuiArt.swapRb(work[row + x]));
			}
		}
		dynTex.upload();
	}

	private int[] compositeFloat(int[] base, int cw, int ch) {
		int[] out = base.clone();
		if (floatImg == null || floatW <= 0 || floatH <= 0) return out;
		for (int dy = 0; dy < floatH; dy++) {
			int py = floatY + dy;
			if (py < 0 || py >= ch) continue;
			int sy = Math.min(floatImg.h() - 1, dy * floatImg.h() / floatH);
			for (int dx = 0; dx < floatW; dx++) {
				int px = floatX + dx;
				if (px < 0 || px >= cw) continue;
				int sx = Math.min(floatImg.w() - 1, dx * floatImg.w() / floatW);
				int fg = floatImg.pixels()[sy * floatImg.w() + sx];
				out[py * cw + px] = blendOver(out[py * cw + px], fg);
			}
		}
		return out;
	}

	private static int blendOver(int dst, int src) {
		int sa = (src >>> 24) & 0xFF;
		if (sa == 0) return dst;
		if (sa == 255) return src;
		int da = (dst >>> 24) & 0xFF;
		int inv = 255 - sa;
		int outA = sa + (da * inv + 127) / 255;
		if (outA == 0) return 0;
		int sr = (src >>> 16) & 0xFF;
		int sg = (src >>> 8) & 0xFF;
		int sb = src & 0xFF;
		int dr = (dst >>> 16) & 0xFF;
		int dg = (dst >>> 8) & 0xFF;
		int db = dst & 0xFF;
		int r = (sr * sa + (dr * da * inv + 127) / 255) / outA;
		int g = (sg * sa + (dg * da * inv + 127) / 255) / outA;
		int b = (sb * sa + (db * da * inv + 127) / 255) / outA;
		return (Mth.clamp(outA, 0, 255) << 24) | (Mth.clamp(r, 0, 255) << 16) | (Mth.clamp(g, 0, 255) << 8) | Mth.clamp(b, 0, 255);
	}

	private void commitFloatIfAny() {
		if (floatImg == null) return;
		recordHistory("stamp");
		int cw = canvasW();
		int ch = canvasH();
		int[] merged = compositeFloat(canvasPixels(), cw, ch);
		System.arraycopy(merged, 0, canvasPixels(), 0, merged.length);
		floatImg = null;
		dragFloatMove = false;
		dragFloatScale = false;
		if (mode == MODE_GUI) syncDraftToShop();
		texDirty = true;
	}

	private void cancelFloat() {
		floatImg = null;
		dragFloatMove = false;
		dragFloatScale = false;
		texDirty = true;
	}

	private void importFile(Path file) {
		try {
			ShopGuiArt.Img img = ShopGuiArt.readImage(file);
			commitFloatIfAny();
			floatImg = img;
			fitFloatAspect();
			texDirty = true;
			setStatus(Component.translatable(K + "sp_imported", file.getFileName().toString(), img.w() + "x" + img.h()), OK);
		} catch (Exception e) {
			setStatus(Component.translatable(K + "sp_import_err", String.valueOf(e.getMessage())), ERR);
		}
	}

	private void fitFloatFull() {
		if (floatImg == null) return;
		floatX = 0;
		floatY = 0;
		floatW = canvasW();
		floatH = canvasH();
		texDirty = true;
	}

	private void fitFloatAspect() {
		if (floatImg == null) return;
		int cw = canvasW();
		int ch = canvasH();
		float scale = Math.min((float) cw / Math.max(1, floatImg.w()), (float) ch / Math.max(1, floatImg.h()));
		floatW = Math.max(1, Math.round(floatImg.w() * scale));
		floatH = Math.max(1, Math.round(floatImg.h() * scale));
		floatX = (cw - floatW) / 2;
		floatY = (ch - floatH) / 2;
		texDirty = true;
	}

	private void fitFloatNative() {
		if (floatImg == null) return;
		floatW = Math.min(512, floatImg.w());
		floatH = Math.min(512, floatImg.h());
		floatX = (canvasW() - floatW) / 2;
		floatY = (canvasH() - floatH) / 2;
		texDirty = true;
	}

	@Override
	public void onFilesDrop(List<Path> paths) {
		for (Path p : paths) {
			String n = p.getFileName().toString().toLowerCase(Locale.ROOT);
			if (n.endsWith(".png") || n.endsWith(".jpg") || n.endsWith(".jpeg")) {
				importFile(p);
				return;
			}
		}
		setStatus(Component.translatable(K + "sp_drop_bad"), ERR);
	}

	private void grabBg(boolean vanillaOnly) {
		commitFloatIfAny();
		recordHistory("grab");
		ShopGuiArt.Img grabbed = ShopGuiArt.grabContainerBg(shop.container, vanillaOnly);
		guiW = grabbed.w();
		guiH = grabbed.h();
		refBgPixels = grabbed.pixels().clone();
		guiPixels = grabbed.pixels().clone();
		shop.hiddenMask = 0L;
		syncDraftToShop();
		texDirty = true;
		setStatus(Component.translatable(K + (vanillaOnly ? "sp_grabbed_vanilla" : "sp_grabbed_active")), OK);
	}

	private void keepOnlyOccupiedSlots() {
		commitFloatIfAny();
		recordHistory("keep_occ");
		int fillCol = ShopGuiArt.samplePanelColor(refBgPixels, guiW, guiH);
		int size = shop.size();
		ShopData.Slot[] curPage = shop.slots;
		int hiddenCount = 0;
		for (int i = 0; i < size; i++) {
			boolean has = false;
			for (ShopData.Slot[] p : shop.pages) {
				if (p[i].shown()) {
					has = true;
					break;
				}
			}
			if (!has && !curPage[i].shown()) {
				ShopGuiArt.eraseSlot(guiPixels, guiW, guiH, i, fillCol);
				shop.setHidden(i, true);
				hiddenCount++;
			} else {
				ShopGuiArt.restoreSlot(guiPixels, refBgPixels, guiW, guiH, i);
				shop.setHidden(i, false);
			}
		}
		syncDraftToShop();
		texDirty = true;
		setStatus(Component.translatable(K + "sp_kept_occupied", hiddenCount), OK);
	}

	private void eraseAllSlots() {
		commitFloatIfAny();
		recordHistory("erase_all");
		int fillCol = ShopGuiArt.samplePanelColor(refBgPixels, guiW, guiH);
		int size = shop.size();
		for (int i = 0; i < size; i++) {
			ShopGuiArt.eraseSlot(guiPixels, guiW, guiH, i, fillCol);
			shop.setHidden(i, true);
		}
		syncDraftToShop();
		texDirty = true;
		setStatus(Component.translatable(K + "sp_erased_all"), OK);
	}

	private void restoreAllSlots() {
		commitFloatIfAny();
		recordHistory("restore_all");
		int size = shop.size();
		for (int i = 0; i < size; i++) {
			ShopGuiArt.restoreSlot(guiPixels, refBgPixels, guiW, guiH, i);
			shop.setHidden(i, false);
		}
		syncDraftToShop();
		texDirty = true;
		setStatus(Component.translatable(K + "sp_restored_all"), OK);
	}

	private void grabCurrentSlotItem() {
		commitFloatIfAny();
		ShopData.Slot sl = shop.slots[slotIndex];
		if (!sl.shown()) {
			setStatus(Component.translatable(K + "sp_slot_empty"), WARN);
			return;
		}
		ShopGuiArt.Img icon = ShopGuiArt.grabItemIcon(sl.item);
		if (icon == null) {
			setStatus(Component.translatable(K + "sp_no_item_tex"), ERR);
			return;
		}
		recordHistory("grab_item");
		btnPixels = ShopGuiArt.scaleNearest(icon, btnW, btnH).pixels();
		texDirty = true;
		setStatus(Component.translatable(K + "sp_grabbed_item"), OK);
	}

	private void grabSlotFrame() {
		commitFloatIfAny();
		recordHistory("grab_slot");
		btnPixels = ShopGuiArt.grabSlotPatch(refBgPixels, guiW, guiH, slotIndex, btnW).pixels();
		texDirty = true;
		setStatus(Component.translatable(K + "sp_grabbed_slot"), OK);
	}

	private void makeBevelButton() {
		commitFloatIfAny();
		recordHistory("bevel");
		int base = (color & 0xFFFFFF) | 0xFF000000;
		int r = (base >>> 16) & 0xFF;
		int g = (base >>> 8) & 0xFF;
		int b = base & 0xFF;
		int hi = 0xFF000000 | (Math.min(255, r + 52) << 16) | (Math.min(255, g + 52) << 8) | Math.min(255, b + 52);
		int lo = 0xFF000000 | (Math.max(0, r - 56) << 16) | (Math.max(0, g - 56) << 8) | Math.max(0, b - 56);
		int edge = 0xFF222222;
		for (int y = 0; y < btnH; y++) {
			for (int x = 0; x < btnW; x++) {
				int c;
				if (x == 0 || y == 0 || x == btnW - 1 || y == btnH - 1) c = edge;
				else if (y == 1 || x == 1) c = hi;
				else if (y == btnH - 2 || x == btnW - 2) c = lo;
				else c = base;
				btnPixels[y * btnW + x] = c;
			}
		}
		texDirty = true;
	}

	private void outlineIcon() {
		commitFloatIfAny();
		recordHistory("outline");
		int[] copy = btnPixels.clone();
		int outCol = (color & 0xFFFFFF) | 0xFF000000;
		int[] dx = {-1, 1, 0, 0};
		int[] dy = {0, 0, -1, 1};
		for (int y = 0; y < btnH; y++) {
			for (int x = 0; x < btnW; x++) {
				if (((copy[y * btnW + x] >>> 24) & 0xFF) > 16) continue;
				for (int d = 0; d < 4; d++) {
					int nx = x + dx[d];
					int ny = y + dy[d];
					if (nx >= 0 && nx < btnW && ny >= 0 && ny < btnH && ((copy[ny * btnW + nx] >>> 24) & 0xFF) > 64) {
						btnPixels[y * btnW + x] = outCol;
						break;
					}
				}
			}
		}
		texDirty = true;
	}

	private void clearBtnCanvas() {
		commitFloatIfAny();
		recordHistory("clear_btn");
		Arrays.fill(btnPixels, 0);
		texDirty = true;
	}

	private String ensurePackReady() throws Exception {
		String pack = currentPack();
		if (pack == null) {
			pack = ResourcePacks.createPack("bjmapedit_models");
			packs = ResourcePacks.folderPacks();
			packIdx = Math.max(0, packs.indexOf(pack));
		}
		return pack;
	}

	private void applyGui() {
		commitFloatIfAny();
		if (!ShopData.validId(shop.id)) {
			setFocused(idField);
			setStatus(Component.translatable(K + "sp_need_id"), ERR);
			return;
		}
		try {
			String pack = ensurePackReady();
			Path dir = ResourcePacks.root().resolve(pack);
			String ns = ResourcePacks.namespace(pack);
			String fontId = ShopGuiArt.writeGuiFont(dir, ns, shop.id, guiW, guiH, guiPixels);
			int blankCmd = 0;
			if (shop.lockHidden && shop.activeHiddenMask() != 0L) {
				blankCmd = ItemModels.ensureBlankModel(dir, ns);
			}
			shop.guiPack = pack;
			shop.guiFont = fontId;
			shop.blankCmd = blankCmd;
			syncDraftToShop();
			shop.guiUnapplied = false;
			ResourcePacks.enableAndReload(pack);
			setStatus(Component.translatable(K + "sp_applied_gui", pack), OK);
		} catch (Exception e) {
			setStatus(Component.translatable(K + "rp_failed", String.valueOf(e.getMessage())), ERR);
		}
	}

	private void clearCustomGui() {
		commitFloatIfAny();
		if (!shop.guiPack.isBlank() && ShopData.validId(shop.id)) {
			Path dir = ResourcePacks.root().resolve(shop.guiPack);
			ShopGuiArt.removeGuiFont(dir, ResourcePacks.namespace(shop.guiPack), shop.id);
		}
		shop.guiFont = "";
		shop.blankCmd = 0;
		shop.hiddenMask = 0L;
		shop.draftGuiPixels = null;
		shop.guiUnapplied = false;
		guiPixels = refBgPixels.clone();
		parent.onPaintMetaChanged();
		texDirty = true;
		setStatus(Component.translatable(K + "sp_cleared_gui"), OK);
	}

	private void applyButtonToSlot() {
		commitFloatIfAny();
		boolean anyPixel = false;
		for (int p : btnPixels) {
			if (((p >>> 24) & 0xFF) != 0) {
				anyPixel = true;
				break;
			}
		}
		if (!anyPixel) {
			setStatus(Component.translatable(K + "sp_btn_blank"), ERR);
			return;
		}
		try {
			String pack = ensurePackReady();
			Path dir = ResourcePacks.root().resolve(pack);
			String ns = ResourcePacks.namespace(pack);
			String sid = ShopData.validId(shop.id) ? shop.id : "draft";
			String btnName = "bjshop_btn_" + sid + "_p" + shop.page + "_s" + slotIndex;
			ShopData.Slot sl = shop.slots[slotIndex];
			ItemStack baseStack;
			int oldCmd = 0;
			if (sl.shown()) {
				baseStack = sl.item.copyWithCount(1);
				oldCmd = ItemModels.cmdOf(baseStack);
			} else {
				baseStack = new ItemStack(Items.PAPER);
				baseStack.set(DataComponents.CUSTOM_NAME, Component.translatable(K + "sp_default_btn_name").withStyle(Style.EMPTY.withItalic(false)));
			}
			Item targetItem = baseStack.getItem();
			int cmd = ShopGuiArt.writeButtonAsset(dir, ns, btnName, btnW, btnH, btnPixels, fullSlot18, targetItem, oldCmd);
			sl.item = ItemModels.withCmd(baseStack, cmd);
			sl.count = ShopData.clampCount(sl.item, Math.max(1, sl.count));
			if (sl.type == ShopData.EMPTY) sl.type = ShopData.ACTION;
			shop.setHidden(slotIndex, false);
			parent.onButtonApplied(slotIndex);
			ResourcePacks.enableAndReload(pack);
			setStatus(Component.translatable(K + "sp_applied_btn", slotIndex + 1, pack), OK);
		} catch (Exception e) {
			setStatus(Component.translatable(K + "rp_failed", String.valueOf(e.getMessage())), ERR);
		}
	}

	private void exportPng() {
		commitFloatIfAny();
		String def = mode == MODE_GUI
			? ((ShopData.validId(shop.id) ? shop.id : "chest_gui") + ".png")
			: ("slot_" + (slotIndex + 1) + "_btn.png");
		int w = canvasW();
		int h = canvasH();
		int[] copy = canvasPixels().clone();
		ResourcePacks.savePng(I18n.get(K + "sp_export"), def, "PNG (*.png)", chosen -> {
			try {
				ShopGuiArt.writePng(chosen, w, h, copy);
				setStatus(Component.translatable(K + "sp_exported", chosen.getFileName().toString()), OK);
			} catch (Exception e) {
				setStatus(Component.translatable(K + "rp_failed", String.valueOf(e.getMessage())), ERR);
			}
		});
	}

	private int toPixelX(double mx) {
		return (int) Math.floor((mx - panX) / zoom);
	}

	private int toPixelY(double my) {
		return (int) Math.floor((my - panY) / zoom);
	}

	private int slotAtPixel(int px, int py) {
		if (px < 7 || px >= 7 + 9 * 18 || py < 17) return -1;
		int col = (px - 7) / 18;
		int row = (py - 17) / 18;
		int rows = shop.size() / 9;
		if (col < 0 || col >= 9 || row < 0 || row >= rows) return -1;
		return row * 9 + col;
	}

	private void paintDot(int[] buf, int cw, int ch, int cx, int cy, int col, boolean erase) {
		int r = brush - 1;
		int x0 = cx - r / 2;
		int y0 = cy - r / 2;
		for (int dy = 0; dy < brush; dy++) {
			int py = y0 + dy;
			if (py < 0 || py >= ch) continue;
			for (int dx = 0; dx < brush; dx++) {
				int px = x0 + dx;
				if (px < 0 || px >= cw) continue;
				if (erase) {
					buf[py * cw + px] = mode == MODE_GUI ? 0 : 0;
				} else {
					buf[py * cw + px] = col;
				}
			}
		}
	}

	private void paintLine(int[] buf, int cw, int ch, int x0, int y0, int x1, int y1, int col, boolean erase) {
		int dx = Math.abs(x1 - x0);
		int dy = Math.abs(y1 - y0);
		int sx = x0 < x1 ? 1 : -1;
		int sy = y0 < y1 ? 1 : -1;
		int err = dx - dy;
		int x = x0;
		int y = y0;
		while (true) {
			paintDot(buf, cw, ch, x, y, col, erase);
			if (x == x1 && y == y1) break;
			int e2 = err * 2;
			if (e2 > -dy) {
				err -= dy;
				x += sx;
			}
			if (e2 < dx) {
				err += dx;
				y += sy;
			}
		}
	}

	private void paintRect(int[] buf, int cw, int ch, int x0, int y0, int x1, int y1, int col, boolean filled) {
		int lx = Math.min(x0, x1);
		int rx = Math.max(x0, x1);
		int ty = Math.min(y0, y1);
		int by = Math.max(y0, y1);
		if (filled) {
			int sx = Math.max(0, lx);
			int ex = Math.min(cw - 1, rx);
			int sy = Math.max(0, ty);
			int ey = Math.min(ch - 1, by);
			for (int y = sy; y <= ey; y++) {
				for (int x = sx; x <= ex; x++) {
					buf[y * cw + x] = col;
				}
			}
		} else {
			paintLine(buf, cw, ch, lx, ty, rx, ty, col, false);
			paintLine(buf, cw, ch, lx, by, rx, by, col, false);
			paintLine(buf, cw, ch, lx, ty, lx, by, col, false);
			paintLine(buf, cw, ch, rx, ty, rx, by, col, false);
		}
	}

	private void floodFill(int[] buf, int cw, int ch, int sx, int sy, int targetCol) {
		if (sx < 0 || sx >= cw || sy < 0 || sy >= ch) return;
		int orig = buf[sy * cw + sx];
		if (orig == targetCol) return;
		Deque<Integer> q = new ArrayDeque<>();
		boolean[] seen = new boolean[cw * ch];
		int start = sy * cw + sx;
		seen[start] = true;
		q.add(start);
		while (!q.isEmpty()) {
			int idx = q.poll();
			buf[idx] = targetCol;
			int x = idx % cw;
			int y = idx / cw;
			if (x > 0 && !seen[idx - 1] && buf[idx - 1] == orig) {
				seen[idx - 1] = true;
				q.add(idx - 1);
			}
			if (x + 1 < cw && !seen[idx + 1] && buf[idx + 1] == orig) {
				seen[idx + 1] = true;
				q.add(idx + 1);
			}
			if (y > 0 && !seen[idx - cw] && buf[idx - cw] == orig) {
				seen[idx - cw] = true;
				q.add(idx - cw);
			}
			if (y + 1 < ch && !seen[idx + cw] && buf[idx + cw] == orig) {
				seen[idx + cw] = true;
				q.add(idx + cw);
			}
		}
	}

	private void applySlotToggle(int slot, boolean hide) {
		if (slot < 0 || slot >= shop.size()) return;
		if (hide) {
			ShopGuiArt.eraseSlot(guiPixels, guiW, guiH, slot, ShopGuiArt.samplePanelColor(refBgPixels, guiW, guiH));
			shop.setHidden(slot, true);
		} else {
			ShopGuiArt.restoreSlot(guiPixels, refBgPixels, guiW, guiH, slot);
			shop.setHidden(slot, false);
		}
		syncDraftToShop();
		texDirty = true;
	}

	private void shiftPixels(int dx, int dy) {
		int cw = canvasW();
		int ch = canvasH();
		int[] src = canvasPixels().clone();
		int[] dst = canvasPixels();
		Arrays.fill(dst, 0);
		for (int y = 0; y < ch; y++) {
			int ny = y + dy;
			if (ny < 0 || ny >= ch) continue;
			for (int x = 0; x < cw; x++) {
				int nx = x + dx;
				if (nx < 0 || nx >= cw) continue;
				dst[ny * cw + nx] = src[y * cw + x];
			}
		}
		texDirty = true;
	}

	private void undo() {
		commitFloatIfAny();
		Snap prev = history().undo(currentSnap());
		if (prev != null) restoreSnap(prev);
	}

	private void redo() {
		commitFloatIfAny();
		Snap next = history().redo(currentSnap());
		if (next != null) restoreSnap(next);
	}

	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		super.renderBackground(g, mouseX, mouseY, partialTick);
		UiTheme.drawPanel(g, leftPos, topPos, panelW, panelH);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		ensureTexture();
		super.render(g, mouseX, mouseY, partialTick);
		UiTheme.drawHeader(g, font, this.title, leftPos, topPos, panelW);
		g.fill(leftPos + 1, topBarY + 17, leftPos + panelW - 1, topBarY + 18, 0xFF2B2C2D);
		g.fill(leftPos + 1, footerY - 3, leftPos + panelW - 1, footerY - 2, 0xFF2B2C2D);

		drawTopBar(g, mouseX, mouseY);
		drawLeftTools(g, mouseX, mouseY);
		drawCanvasView(g, mouseX, mouseY);
		drawRightPalette(g, mouseX, mouseY);
		drawFooterBar(g, mouseX, mouseY);
	}

	private void drawTopBar(GuiGraphics g, int mx, int my) {
		UiTheme.drawButton(g, font, Component.translatable(K + "sp_mode_gui"), leftColX, topBarY + 1, 54, 15, hit(mx, my, leftColX, topBarY + 1, 54, 15), true, mode == MODE_GUI);
		UiTheme.drawButton(g, font, Component.translatable(K + "sp_mode_btn"), leftColX + 56, topBarY + 1, 54, 15, hit(mx, my, leftColX + 56, topBarY + 1, 54, 15), true, mode == MODE_BTN);

		boolean canCycle = packs.size() > 1;
		UiTheme.drawButton(g, font, Component.literal("<"), packMinusX, topBarY + 1, 12, 15, hit(mx, my, packMinusX, topBarY + 1, 12, 15), canCycle);
		UiTheme.drawInputWell(g, packBoxX, topBarY + 1, packBoxW, 15, false);
		String pk = currentPack();
		String pkText = pk == null ? I18n.get(K + "sp_pack_auto") : pk;
		UiTheme.clipLabel(g, font, pkText, packBoxX + 4, topBarY + 4, packBoxW - 8, pk == null ? UiTheme.cMuted() : UiTheme.cInputText());
		UiTheme.drawButton(g, font, Component.literal(">"), packPlusX, topBarY + 1, 12, 15, hit(mx, my, packPlusX, topBarY + 1, 12, 15), canCycle);

		if (mode == MODE_GUI) {
			String idLbl = I18n.get(K + "sp_id_short");
			UiTheme.label(g, font, idLbl, idField.getX() - 4 - font.width(idLbl), topBarY + 4);
		} else {
			int rx = leftPos + panelW - 6 - 93;
			for (int i = 0; i < BTN_SIZES.length; i++) {
				int x = rx + i * 31;
				String lbl = BTN_SIZES[i] + "x";
				UiTheme.drawButton(g, font, Component.literal(lbl), x, topBarY + 1, 29, 15, hit(mx, my, x, topBarY + 1, 29, 15), true, btnSizeIdx == i);
			}
		}
	}

	private void drawLeftTools(GuiGraphics g, int mx, int my) {
		String[] tKeys = {"sp_t_pen", "sp_t_erase", "sp_t_fill", "sp_t_pick", "sp_t_line", "sp_t_rect", "sp_t_box", mode == MODE_GUI ? "sp_t_slot" : "sp_t_move"};
		for (int i = 0; i < 8; i++) {
			int tx = leftColX + (i % 4) * 26;
			int ty = bodyTop + (i / 4) * 16;
			UiTheme.drawButton(g, font, Component.translatable(K + tKeys[i]), tx, ty, 24, 14, hit(mx, my, tx, ty, 24, 14), true, tool == i);
		}
		int y3 = bodyTop + 33;
		UiTheme.drawButton(g, font, Component.literal("-"), leftColX, y3, 13, 14, hit(mx, my, leftColX, y3, 13, 14), brush > 1);
		g.drawCenteredString(font, brush + "px", leftColX + 25, y3 + 3, UiTheme.cLabel());
		UiTheme.drawButton(g, font, Component.literal("+"), leftColX + 37, y3, 13, 14, hit(mx, my, leftColX + 37, y3, 13, 14), brush < 12);
		UiTheme.drawButton(g, font, Component.translatable(K + "sp_undo"), leftColX + 52, y3, 24, 14, hit(mx, my, leftColX + 52, y3, 24, 14), true);
		UiTheme.drawButton(g, font, Component.translatable(K + "sp_redo"), leftColX + 78, y3, 24, 14, hit(mx, my, leftColX + 78, y3, 24, 14), true);

		int y4 = bodyTop + 50;
		int y5 = bodyTop + 66;
		int y6 = bodyTop + 82;
		int y7 = bodyTop + 98;
		int y8 = bodyTop + 114;
		int y9 = bodyTop + 130;
		if (mode == MODE_GUI) {
			UiTheme.drawButton(g, font, Component.translatable(K + "sp_grab_van"), leftColX, y4, 50, 14, hit(mx, my, leftColX, y4, 50, 14), true);
			UiTheme.drawButton(g, font, Component.translatable(K + "sp_grab_cur"), leftColX + 52, y4, 50, 14, hit(mx, my, leftColX + 52, y4, 50, 14), true);
			UiTheme.drawButton(g, font, Component.translatable(K + "sp_keep_occ"), leftColX, y5, 50, 14, hit(mx, my, leftColX, y5, 50, 14), true);
			UiTheme.drawButton(g, font, Component.translatable(K + "sp_hide_all"), leftColX + 52, y5, 50, 14, hit(mx, my, leftColX + 52, y5, 50, 14), true);
			UiTheme.drawButton(g, font, Component.translatable(K + "sp_show_all"), leftColX, y6, 50, 14, hit(mx, my, leftColX, y6, 50, 14), true);
			UiTheme.drawButton(g, font, Component.translatable(K + "sp_import"), leftColX + 52, y6, 50, 14, hit(mx, my, leftColX + 52, y6, 50, 14), true);

			UiTheme.drawButton(g, font, Component.translatable(K + "sp_ov_slots"), leftColX, y7, 32, 14, hit(mx, my, leftColX, y7, 32, 14), true, showSlots);
			UiTheme.drawButton(g, font, Component.translatable(K + "sp_ov_items"), leftColX + 34, y7, 34, 14, hit(mx, my, leftColX + 34, y7, 34, 14), true, showItems);
			UiTheme.drawButton(g, font, Component.translatable(K + "sp_ov_grid"), leftColX + 70, y7, 32, 14, hit(mx, my, leftColX + 70, y7, 32, 14), true, showGrid);

			String lkKey = shop.lockHidden ? "sp_lock_hid_on" : "sp_lock_hid_off";
			UiTheme.drawButton(g, font, Component.translatable(K + lkKey), leftColX, y8, 102, 14, hit(mx, my, leftColX, y8, 102, 14), true, shop.lockHidden);
			UiTheme.drawButton(g, font, Component.translatable(K + "sp_fit_view"), leftColX, y9, 102, 14, hit(mx, my, leftColX, y9, 102, 14), true);
		} else {
			UiTheme.drawButton(g, font, Component.translatable(K + "sp_grab_item"), leftColX, y4, 50, 14, hit(mx, my, leftColX, y4, 50, 14), true);
			UiTheme.drawButton(g, font, Component.translatable(K + "sp_grab_slot"), leftColX + 52, y4, 50, 14, hit(mx, my, leftColX + 52, y4, 50, 14), true);
			UiTheme.drawButton(g, font, Component.translatable(K + "sp_bevel"), leftColX, y5, 50, 14, hit(mx, my, leftColX, y5, 50, 14), true);
			UiTheme.drawButton(g, font, Component.translatable(K + "sp_clear_c"), leftColX + 52, y5, 50, 14, hit(mx, my, leftColX + 52, y5, 50, 14), true);
			UiTheme.drawButton(g, font, Component.translatable(K + "sp_outline"), leftColX, y6, 50, 14, hit(mx, my, leftColX, y6, 50, 14), true);
			UiTheme.drawButton(g, font, Component.translatable(K + "sp_import"), leftColX + 52, y6, 50, 14, hit(mx, my, leftColX + 52, y6, 50, 14), true);

			String f18Key = fullSlot18 ? "sp_full18_on" : "sp_full18_off";
			UiTheme.drawButton(g, font, Component.translatable(K + f18Key), leftColX, y7, 68, 14, hit(mx, my, leftColX, y7, 68, 14), true, fullSlot18);
			UiTheme.drawButton(g, font, Component.translatable(K + "sp_ov_grid"), leftColX + 70, y7, 32, 14, hit(mx, my, leftColX + 70, y7, 32, 14), true, showGrid);
			UiTheme.drawButton(g, font, Component.translatable(K + "sp_fit_view"), leftColX, y8, 102, 14, hit(mx, my, leftColX, y8, 102, 14), true);
		}
	}

	private void drawCanvasView(GuiGraphics g, int mx, int my) {
		UiTheme.drawPreviewFrame(g, viewX, viewY, viewW, viewH);
		g.enableScissor(viewX + 1, viewY + 1, viewX + viewW - 1, viewY + viewH - 1);

		int cw = canvasW();
		int ch = canvasH();
		int drawX = Math.round(panX);
		int drawY = Math.round(panY);
		int drawW = Math.max(1, Math.round(cw * zoom));
		int drawH = Math.max(1, Math.round(ch * zoom));

		drawChecker(g, Math.max(viewX + 1, drawX), Math.max(viewY + 1, drawY),
			Math.min(viewX + viewW - 1, drawX + drawW), Math.min(viewY + viewH - 1, drawY + drawH), 6);

		if (dynId != null) {
			RenderSystem.enableBlend();
			RenderSystem.defaultBlendFunc();
			g.pose().pushPose();
			g.pose().translate(panX, panY, 0.0f);
			g.pose().scale(zoom, zoom, 1.0f);
			g.blit(dynId, 0, 0, 0, 0, cw, ch, cw, ch);
			g.pose().popPose();
			RenderSystem.disableBlend();
		}

		if (mode == MODE_GUI && showItems) {
			int size = shop.size();
			for (int i = 0; i < size; i++) {
				ShopData.Slot sl = shop.slots[i];
				if (!sl.shown()) continue;
				ItemStack st = ShopPack.icon(shop, i);
				float ix = panX + (8 + (i % 9) * 18) * zoom;
				float iy = panY + (18 + (i / 9) * 18) * zoom;
				g.pose().pushPose();
				g.pose().translate(ix, iy, 0.0f);
				g.pose().scale(zoom, zoom, 1.0f);
				g.renderItem(st, 0, 0);
				g.pose().popPose();
			}
		}

		if (showGrid && zoom >= 4.0f) {
			int xStart = Math.max(0, (int) Math.floor((viewX - panX) / zoom));
			int xEnd = Math.min(cw, (int) Math.ceil((viewX + viewW - panX) / zoom));
			int yStart = Math.max(0, (int) Math.floor((viewY - panY) / zoom));
			int yEnd = Math.min(ch, (int) Math.ceil((viewY + viewH - panY) / zoom));
			for (int x = xStart; x <= xEnd; x++) {
				int sx = Math.round(panX + x * zoom);
				g.fill(sx, drawY, sx + 1, drawY + drawH, 0x26FFFFFF);
			}
			for (int y = yStart; y <= yEnd; y++) {
				int sy = Math.round(panY + y * zoom);
				g.fill(drawX, sy, drawX + drawW, sy + 1, 0x26FFFFFF);
			}
		}

		if (mode == MODE_GUI && (showSlots || tool == TOOL_SLOT)) {
			int size = shop.size();
			int hovSlot = hit(mx, my, viewX + 1, viewY + 1, viewW - 2, viewH - 2) ? slotAtPixel(toPixelX(mx), toPixelY(my)) : -1;
			UiTheme.pushOverlay(g);
			for (int i = 0; i < size; i++) {
				int x0 = Math.round(panX + (7 + (i % 9) * 18) * zoom);
				int y0 = Math.round(panY + (17 + (i / 9) * 18) * zoom);
				int x1 = Math.round(panX + (25 + (i % 9) * 18) * zoom);
				int y1 = Math.round(panY + (35 + (i / 9) * 18) * zoom);
				boolean hid = shop.isHidden(i);
				int borderCol = i == hovSlot && tool == TOOL_SLOT
					? (hid ? 0xFFFF7B72 : 0xFF7EE787)
					: (hid ? 0x90FF7B72 : 0x4558A6FF);
				strokeRect(g, x0, y0, x1 - x0, y1 - y0, borderCol);
				if (hid && tool == TOOL_SLOT) {
					g.fill(x0 + 1, y0 + 1, x1 - 1, y1 - 1, 0x28FF4444);
				}
			}
			UiTheme.popOverlay(g);
		}

		if (floatImg != null) {
			int fx0 = Math.round(panX + floatX * zoom);
			int fy0 = Math.round(panY + floatY * zoom);
			int fx1 = Math.round(panX + (floatX + floatW) * zoom);
			int fy1 = Math.round(panY + (floatY + floatH) * zoom);
			UiTheme.pushOverlay(g);
			strokeRect(g, fx0, fy0, Math.max(1, fx1 - fx0), Math.max(1, fy1 - fy0), 0xFF58A6FF);
			g.fill(fx1 - 4, fy1 - 4, fx1 + 2, fy1 + 2, 0xFFFFFFFF);
			g.fill(fx1 - 3, fy1 - 3, fx1 + 1, fy1 + 1, 0xFF1F6FEB);
			drawFloatBar(g, mx, my);
			UiTheme.popOverlay(g);
		} else if (hit(mx, my, viewX + 1, viewY + 1, viewW - 2, viewH - 2) && tool != TOOL_SLOT) {
			int px = toPixelX(mx);
			int py = toPixelY(my);
			if (px >= 0 && px < cw && py >= 0 && py < ch) {
				int r = brush - 1;
				int bx0 = Math.round(panX + (px - r / 2) * zoom);
				int by0 = Math.round(panY + (py - r / 2) * zoom);
				int bx1 = Math.round(panX + (px - r / 2 + brush) * zoom);
				int by1 = Math.round(panY + (py - r / 2 + brush) * zoom);
				UiTheme.pushOverlay(g);
				strokeRect(g, bx0, by0, Math.max(1, bx1 - bx0), Math.max(1, by1 - by0), 0xB0FFFFFF);
				UiTheme.popOverlay(g);
			}
		}

		strokeRect(g, drawX - 1, drawY - 1, drawW + 2, drawH + 2, 0xFF454C54);
		g.disableScissor();
	}

	private void drawFloatBar(GuiGraphics g, int mx, int my) {
		int by = viewY + 3;
		int bx = viewX + 4;
		String[] keys = {"sp_fl_full", "sp_fl_fit", "sp_fl_1x", "sp_fl_ok", "sp_fl_cancel"};
		for (int i = 0; i < keys.length; i++) {
			Component c = Component.translatable(K + keys[i]);
			int bw = font.width(c) + 8;
			UiTheme.drawButton(g, font, c, bx, by, bw, 13, hit(mx, my, bx, by, bw, 13), true, i == 3);
			bx += bw + 2;
		}
	}

	private boolean clickFloatBar(double mx, double my) {
		if (floatImg == null) return false;
		int by = viewY + 3;
		int bx = viewX + 4;
		String[] keys = {"sp_fl_full", "sp_fl_fit", "sp_fl_1x", "sp_fl_ok", "sp_fl_cancel"};
		for (int i = 0; i < keys.length; i++) {
			int bw = font.width(Component.translatable(K + keys[i])) + 8;
			if (hit(mx, my, bx, by, bw, 13)) {
				UiTheme.playClick();
				if (i == 0) fitFloatFull();
				else if (i == 1) fitFloatAspect();
				else if (i == 2) fitFloatNative();
				else if (i == 3) commitFloatIfAny();
				else cancelFloat();
				return true;
			}
			bx += bw + 2;
		}
		return false;
	}

	private void drawRightPalette(GuiGraphics g, int mx, int my) {
		drawChecker(g, rightColX, bodyTop, rightColX + 19, bodyTop + 15, 4);
		g.fill(rightColX, bodyTop, rightColX + 10, bodyTop + 15, color | 0xFF000000);
		g.fill(rightColX + 10, bodyTop, rightColX + 19, bodyTop + 15, color);
		strokeRect(g, rightColX, bodyTop, 19, 15, 0xFF454C54);

		int pureHue = Color.HSBtoRGB(hue, 1.0f, 1.0f) | 0xFF000000;
		int cols = 22;
		int rows = 12;
		for (int ry = 0; ry < rows; ry++) {
			int y0 = svY + ry * svH / rows;
			int y1 = svY + (ry + 1) * svH / rows;
			float v = 1.0f - (ry + 0.5f) / rows;
			for (int cx = 0; cx < cols; cx++) {
				int x0 = svX + cx * svW / cols;
				int x1 = svX + (cx + 1) * svW / cols;
				float s = (cx + 0.5f) / cols;
				int c = Color.HSBtoRGB(hue, s, v) | 0xFF000000;
				g.fill(x0, y0, x1, y1, c);
			}
		}
		strokeRect(g, svX, svY, svW, svH, 0xFF3C444D);
		int curSvX = svX + Math.round(sat * (svW - 1));
		int curSvY = svY + Math.round((1.0f - val) * (svH - 1));
		strokeRect(g, curSvX - 2, curSvY - 2, 5, 5, val > 0.6f && sat < 0.4f ? 0xFF000000 : 0xFFFFFFFF);
		letUnused(pureHue);

		for (int i = 0; i < hueW; i++) {
			int hc = Color.HSBtoRGB(i / (float) hueW, 1.0f, 1.0f) | 0xFF000000;
			g.fill(hueX + i, hueY, hueX + i + 1, hueY + hueH, hc);
		}
		strokeRect(g, hueX, hueY, hueW, hueH, 0xFF3C444D);
		int hx = hueX + Math.round(hue * (hueW - 1));
		g.fill(hx - 1, hueY - 1, hx + 2, hueY + hueH + 1, 0xFFFFFFFF);

		drawChecker(g, alX, alY, alX + alW, alY + alH, 4);
		int baseRgb = color & 0xFFFFFF;
		for (int i = 0; i < alW; i++) {
			int a = i * 255 / Math.max(1, alW - 1);
			g.fill(alX + i, alY, alX + i + 1, alY + alH, (a << 24) | baseRgb);
		}
		strokeRect(g, alX, alY, alW, alH, 0xFF3C444D);
		int ax = alX + Math.round((alpha / 255.0f) * (alW - 1));
		g.fill(ax - 1, alY - 1, ax + 2, alY + alH + 1, 0xFFFFFFFF);

		for (int i = 0; i < PALETTE.length; i++) {
			int px = palX + (i % 6) * 14;
			int py = palY + (i / 6) * 13;
			g.fill(px, py, px + 13, py + 11, PALETTE[i]);
			boolean sel = (color & 0xFFFFFF) == (PALETTE[i] & 0xFFFFFF);
			strokeRect(g, px, py, 13, 11, sel ? 0xFFFFFFFF : (hit(mx, my, px, py, 13, 11) ? 0xFF8B949E : 0xFF2B2C2D));
		}
	}

	private static void letUnused(int v) {}

	private void drawFooterBar(GuiGraphics g, int mx, int my) {
		int backW = font.width(Component.translatable(K + "rp_back")) + 14;
		UiTheme.drawButton(g, font, Component.translatable(K + "rp_back"), leftColX, footerY, backW, 18, hit(mx, my, leftColX, footerY, backW, 18), true);

		int rightEdge = leftPos + panelW - 6;
		int leftLimit;
		if (mode == MODE_GUI) {
			Component appC = Component.translatable(K + "sp_apply_gui");
			int appW = font.width(appC) + 14;
			int appX = rightEdge - appW;
			UiTheme.drawButton(g, font, appC, appX, footerY, appW, 18, hit(mx, my, appX, footerY, appW, 18), true, true);

			Component expC = Component.translatable(K + "sp_export");
			int expW = font.width(expC) + 12;
			int expX = appX - 4 - expW;
			UiTheme.drawButton(g, font, expC, expX, footerY, expW, 18, hit(mx, my, expX, footerY, expW, 18), true);

			Component clrC = Component.translatable(K + "sp_clear_gui");
			int clrW = font.width(clrC) + 12;
			int clrX = expX - 4 - clrW;
			UiTheme.drawButton(g, font, clrC, clrX, footerY, clrW, 18, hit(mx, my, clrX, footerY, clrW, 18), true);
			leftLimit = clrX - 6;
		} else {
			Component appC = Component.translatable(K + "sp_apply_btn", slotIndex + 1);
			int appW = font.width(appC) + 14;
			int appX = rightEdge - appW;
			UiTheme.drawButton(g, font, appC, appX, footerY, appW, 18, hit(mx, my, appX, footerY, appW, 18), true, true);

			Component expC = Component.translatable(K + "sp_export");
			int expW = font.width(expC) + 12;
			int expX = appX - 4 - expW;
			UiTheme.drawButton(g, font, expC, expX, footerY, expW, 18, hit(mx, my, expX, footerY, expW, 18), true);
			leftLimit = expX - 6;
		}

		int infoX = leftColX + backW + 6;
		String info = status.getString();
		int infoCol = statusColor;
		if (info.isEmpty()) {
			int pct = Math.round(zoom * 100.0f);
			if (curPx >= 0 && curPx < canvasW() && curPy >= 0 && curPy < canvasH()) {
				info = "(" + curPx + ", " + curPy + ")  " + canvasW() + "x" + canvasH() + "  " + pct + "%";
			} else {
				info = I18n.get(K + (mode == MODE_GUI ? "sp_hint_gui" : "sp_hint_btn"));
			}
			infoCol = UiTheme.cMuted();
		}
		UiTheme.clipLabel(g, font, info, infoX, footerY + 5, Math.max(16, leftLimit - infoX), infoCol);
	}

	private static void drawChecker(GuiGraphics g, int x0, int y0, int x1, int y1, int cell) {
		if (x1 <= x0 || y1 <= y0) return;
		g.fill(x0, y0, x1, y1, 0xFF22252A);
		for (int y = y0; y < y1; y += cell) {
			int row = (y - y0) / cell;
			int ye = Math.min(y1, y + cell);
			for (int x = x0 + (row & 1) * cell; x < x1; x += cell * 2) {
				g.fill(x, y, Math.min(x1, x + cell), ye, 0xFF2F343C);
			}
		}
	}

	private static void strokeRect(GuiGraphics g, int x, int y, int w, int h, int col) {
		if (w <= 0 || h <= 0) return;
		g.fill(x, y, x + w, y + 1, col);
		g.fill(x, y + h - 1, x + w, y + h, col);
		g.fill(x, y, x + 1, y + h, col);
		g.fill(x + w - 1, y, x + w, y + h, col);
	}

	private static boolean hit(double mx, double my, int x, int y, int w, int h) {
		return mx >= x && mx < x + w && my >= y && my < y + h;
	}

	@Override
	public void mouseMoved(double mouseX, double mouseY) {
		super.mouseMoved(mouseX, mouseY);
		if (hit(mouseX, mouseY, viewX + 1, viewY + 1, viewW - 2, viewH - 2)) {
			curPx = toPixelX(mouseX);
			curPy = toPixelY(mouseY);
		} else {
			curPx = -1;
			curPy = -1;
		}
	}

	@Override
	public boolean mouseClicked(double mx, double my, int button) {
		if (super.mouseClicked(mx, my, button)) return true;

		if (button == 0) {
			if (hit(mx, my, leftColX, topBarY + 1, 54, 15)) {
				UiTheme.playClick();
				switchMode(MODE_GUI);
				return true;
			}
			if (hit(mx, my, leftColX + 56, topBarY + 1, 54, 15)) {
				UiTheme.playClick();
				switchMode(MODE_BTN);
				return true;
			}
			if (packs.size() > 1 && hit(mx, my, packMinusX, topBarY + 1, 12, 15)) {
				UiTheme.playClick();
				packIdx = Math.floorMod(packIdx - 1, packs.size());
				return true;
			}
			if (packs.size() > 1 && hit(mx, my, packPlusX, topBarY + 1, 12, 15)) {
				UiTheme.playClick();
				packIdx = Math.floorMod(packIdx + 1, packs.size());
				return true;
			}
			if (mode == MODE_BTN) {
				int rx = leftPos + panelW - 6 - 93;
				for (int i = 0; i < BTN_SIZES.length; i++) {
					if (hit(mx, my, rx + i * 31, topBarY + 1, 29, 15)) {
						UiTheme.playClick();
						changeBtnSize(i);
						return true;
					}
				}
			}
			if (clickLeftTools(mx, my)) return true;
			if (clickRightPalette(mx, my)) return true;
			if (clickFooter(mx, my)) return true;
		}

		if (hit(mx, my, viewX + 1, viewY + 1, viewW - 2, viewH - 2)) {
			setFocused(null);
			if (button == 0 && clickFloatBar(mx, my)) return true;
			if (button == 2 || (button == 1 && (mode == MODE_BTN || tool != TOOL_SLOT))) {
				panning = true;
				panStartMx = mx;
				panStartMy = my;
				panStartX = panX;
				panStartY = panY;
				return true;
			}
			if (floatImg != null && button == 0) {
				int fx1 = Math.round(panX + (floatX + floatW) * zoom);
				int fy1 = Math.round(panY + (floatY + floatH) * zoom);
				if (Math.abs(mx - fx1) <= 6 && Math.abs(my - fy1) <= 6) {
					dragFloatScale = true;
					return true;
				}
				int px = toPixelX(mx);
				int py = toPixelY(my);
				if (px >= floatX && px < floatX + floatW && py >= floatY && py < floatY + floatH) {
					dragFloatMove = true;
					dragFloatOffX = px - floatX;
					dragFloatOffY = py - floatY;
					return true;
				}
				commitFloatIfAny();
			}
			return startCanvasStroke(mx, my, button);
		}
		return false;
	}

	private boolean clickLeftTools(double mx, double my) {
		for (int i = 0; i < 8; i++) {
			int tx = leftColX + (i % 4) * 26;
			int ty = bodyTop + (i / 4) * 16;
			if (hit(mx, my, tx, ty, 24, 14)) {
				UiTheme.playClick();
				commitFloatIfAny();
				tool = i;
				return true;
			}
		}
		int y3 = bodyTop + 33;
		if (hit(mx, my, leftColX, y3, 13, 14)) {
			UiTheme.playClick();
			brush = Math.max(1, brush - 1);
			return true;
		}
		if (hit(mx, my, leftColX + 37, y3, 13, 14)) {
			UiTheme.playClick();
			brush = Math.min(12, brush + 1);
			return true;
		}
		if (hit(mx, my, leftColX + 52, y3, 24, 14)) {
			UiTheme.playClick();
			undo();
			return true;
		}
		if (hit(mx, my, leftColX + 78, y3, 24, 14)) {
			UiTheme.playClick();
			redo();
			return true;
		}
		int y4 = bodyTop + 50;
		int y5 = bodyTop + 66;
		int y6 = bodyTop + 82;
		int y7 = bodyTop + 98;
		int y8 = bodyTop + 114;
		int y9 = bodyTop + 130;
		if (mode == MODE_GUI) {
			if (hit(mx, my, leftColX, y4, 50, 14)) {
				UiTheme.playClick();
				grabBg(true);
				return true;
			}
			if (hit(mx, my, leftColX + 52, y4, 50, 14)) {
				UiTheme.playClick();
				grabBg(false);
				return true;
			}
			if (hit(mx, my, leftColX, y5, 50, 14)) {
				UiTheme.playClick();
				keepOnlyOccupiedSlots();
				return true;
			}
			if (hit(mx, my, leftColX + 52, y5, 50, 14)) {
				UiTheme.playClick();
				eraseAllSlots();
				return true;
			}
			if (hit(mx, my, leftColX, y6, 50, 14)) {
				UiTheme.playClick();
				restoreAllSlots();
				return true;
			}
			if (hit(mx, my, leftColX + 52, y6, 50, 14)) {
				UiTheme.playClick();
				ResourcePacks.chooseImage(I18n.get(K + "sp_import"), "PNG/JPG (*.png;*.jpg;*.jpeg)", this::importFile);
				return true;
			}
			if (hit(mx, my, leftColX, y7, 32, 14)) {
				UiTheme.playClick();
				showSlots = !showSlots;
				return true;
			}
			if (hit(mx, my, leftColX + 34, y7, 34, 14)) {
				UiTheme.playClick();
				showItems = !showItems;
				return true;
			}
			if (hit(mx, my, leftColX + 70, y7, 32, 14)) {
				UiTheme.playClick();
				showGrid = !showGrid;
				return true;
			}
			if (hit(mx, my, leftColX, y8, 102, 14)) {
				UiTheme.playClick();
				shop.lockHidden = !shop.lockHidden;
				parent.onPaintMetaChanged();
				return true;
			}
			if (hit(mx, my, leftColX, y9, 102, 14)) {
				UiTheme.playClick();
				fitView();
				return true;
			}
		} else {
			if (hit(mx, my, leftColX, y4, 50, 14)) {
				UiTheme.playClick();
				grabCurrentSlotItem();
				return true;
			}
			if (hit(mx, my, leftColX + 52, y4, 50, 14)) {
				UiTheme.playClick();
				grabSlotFrame();
				return true;
			}
			if (hit(mx, my, leftColX, y5, 50, 14)) {
				UiTheme.playClick();
				makeBevelButton();
				return true;
			}
			if (hit(mx, my, leftColX + 52, y5, 50, 14)) {
				UiTheme.playClick();
				clearBtnCanvas();
				return true;
			}
			if (hit(mx, my, leftColX, y6, 50, 14)) {
				UiTheme.playClick();
				outlineIcon();
				return true;
			}
			if (hit(mx, my, leftColX + 52, y6, 50, 14)) {
				UiTheme.playClick();
				ResourcePacks.chooseImage(I18n.get(K + "sp_import"), "PNG/JPG (*.png;*.jpg;*.jpeg)", this::importFile);
				return true;
			}
			if (hit(mx, my, leftColX, y7, 68, 14)) {
				UiTheme.playClick();
				fullSlot18 = !fullSlot18;
				return true;
			}
			if (hit(mx, my, leftColX + 70, y7, 32, 14)) {
				UiTheme.playClick();
				showGrid = !showGrid;
				return true;
			}
			if (hit(mx, my, leftColX, y8, 102, 14)) {
				UiTheme.playClick();
				fitView();
				return true;
			}
		}
		return false;
	}

	private boolean clickRightPalette(double mx, double my) {
		if (hit(mx, my, svX, svY, svW, svH)) {
			dragSv = true;
			updateSv(mx, my);
			return true;
		}
		if (hit(mx, my, hueX, hueY, hueW, hueH)) {
			dragHue = true;
			updateHue(mx);
			return true;
		}
		if (hit(mx, my, alX, alY, alW, alH)) {
			dragAlpha = true;
			updateAlpha(mx);
			return true;
		}
		for (int i = 0; i < PALETTE.length; i++) {
			int px = palX + (i % 6) * 14;
			int py = palY + (i / 6) * 13;
			if (hit(mx, my, px, py, 13, 11)) {
				UiTheme.playClick();
				setColorFromArgb((alpha << 24) | (PALETTE[i] & 0xFFFFFF), true);
				return true;
			}
		}
		return false;
	}

	private boolean clickFooter(double mx, double my) {
		int backW = font.width(Component.translatable(K + "rp_back")) + 14;
		if (hit(mx, my, leftColX, footerY, backW, 18)) {
			UiTheme.playClick();
			onClose();
			return true;
		}
		int rightEdge = leftPos + panelW - 6;
		if (mode == MODE_GUI) {
			int appW = font.width(Component.translatable(K + "sp_apply_gui")) + 14;
			int appX = rightEdge - appW;
			if (hit(mx, my, appX, footerY, appW, 18)) {
				UiTheme.playClick();
				applyGui();
				return true;
			}
			int expW = font.width(Component.translatable(K + "sp_export")) + 12;
			int expX = appX - 4 - expW;
			if (hit(mx, my, expX, footerY, expW, 18)) {
				UiTheme.playClick();
				exportPng();
				return true;
			}
			int clrW = font.width(Component.translatable(K + "sp_clear_gui")) + 12;
			int clrX = expX - 4 - clrW;
			if (hit(mx, my, clrX, footerY, clrW, 18)) {
				UiTheme.playClick();
				clearCustomGui();
				return true;
			}
		} else {
			int appW = font.width(Component.translatable(K + "sp_apply_btn", slotIndex + 1)) + 14;
			int appX = rightEdge - appW;
			if (hit(mx, my, appX, footerY, appW, 18)) {
				UiTheme.playClick();
				applyButtonToSlot();
				return true;
			}
			int expW = font.width(Component.translatable(K + "sp_export")) + 12;
			int expX = appX - 4 - expW;
			if (hit(mx, my, expX, footerY, expW, 18)) {
				UiTheme.playClick();
				exportPng();
				return true;
			}
		}
		return false;
	}

	private boolean startCanvasStroke(double mx, double my, int button) {
		int cw = canvasW();
		int ch = canvasH();
		int px = toPixelX(mx);
		int py = toPixelY(my);
		if (hasAltDown() && button == 0) {
			if (px >= 0 && px < cw && py >= 0 && py < ch) {
				int picked = canvasPixels()[py * cw + px];
				if (((picked >>> 24) & 0xFF) > 0) setColorFromArgb(picked, true);
			}
			return true;
		}
		if (mode == MODE_GUI && tool == TOOL_SLOT) {
			int sl = slotAtPixel(px, py);
			if (sl >= 0) {
				recordHistory("slot");
				painting = true;
				strokeBtn = button;
				slotEraseState = button == 0 ? !shop.isHidden(sl) : false;
				applySlotToggle(sl, slotEraseState);
			}
			return true;
		}
		if (button != 0) return false;
		if (tool == TOOL_PICK) {
			if (px >= 0 && px < cw && py >= 0 && py < ch) {
				int picked = canvasPixels()[py * cw + px];
				if (((picked >>> 24) & 0xFF) > 0) setColorFromArgb(picked, true);
			}
			return true;
		}
		if (tool == TOOL_FILL) {
			if (px >= 0 && px < cw && py >= 0 && py < ch) {
				recordHistory("fill");
				floodFill(canvasPixels(), cw, ch, px, py, color);
				if (mode == MODE_GUI) syncDraftToShop();
				texDirty = true;
			}
			return true;
		}
		recordHistory("stroke");
		painting = true;
		strokeBtn = button;
		startPx = px;
		startPy = py;
		lastPx = px;
		lastPy = py;
		if (tool == TOOL_LINE || tool == TOOL_RECT || tool == TOOL_BOX || (mode == MODE_BTN && tool == TOOL_SLOT)) {
			shapeBase = canvasPixels().clone();
		} else {
			paintDot(canvasPixels(), cw, ch, px, py, color, tool == TOOL_ERASE);
			if (mode == MODE_GUI) syncDraftToShop();
			texDirty = true;
		}
		return true;
	}

	@Override
	public boolean mouseDragged(double mx, double my, int button, double dragX, double dragY) {
		if (dragSv) {
			updateSv(mx, my);
			return true;
		}
		if (dragHue) {
			updateHue(mx);
			return true;
		}
		if (dragAlpha) {
			updateAlpha(mx);
			return true;
		}
		if (panning) {
			panX = panStartX + (float) (mx - panStartMx);
			panY = panStartY + (float) (my - panStartMy);
			return true;
		}
		if (dragFloatScale && floatImg != null) {
			floatW = Math.max(1, toPixelX(mx) - floatX + 1);
			floatH = Math.max(1, toPixelY(my) - floatY + 1);
			texDirty = true;
			return true;
		}
		if (dragFloatMove && floatImg != null) {
			floatX = toPixelX(mx) - dragFloatOffX;
			floatY = toPixelY(my) - dragFloatOffY;
			texDirty = true;
			return true;
		}
		if (painting) {
			int cw = canvasW();
			int ch = canvasH();
			int px = toPixelX(mx);
			int py = toPixelY(my);
			curPx = px;
			curPy = py;
			if (mode == MODE_GUI && tool == TOOL_SLOT) {
				int sl = slotAtPixel(px, py);
				if (sl >= 0) applySlotToggle(sl, slotEraseState);
				return true;
			}
			if (mode == MODE_BTN && tool == TOOL_SLOT) {
				System.arraycopy(shapeBase, 0, btnPixels, 0, btnPixels.length);
				shiftPixels(px - startPx, py - startPy);
				return true;
			}
			if (tool == TOOL_PEN || tool == TOOL_ERASE) {
				paintLine(canvasPixels(), cw, ch, lastPx, lastPy, px, py, color, tool == TOOL_ERASE);
				lastPx = px;
				lastPy = py;
				if (mode == MODE_GUI) syncDraftToShop();
				texDirty = true;
				return true;
			}
			if (shapeBase != null && (tool == TOOL_LINE || tool == TOOL_RECT || tool == TOOL_BOX)) {
				int[] dst = canvasPixels();
				System.arraycopy(shapeBase, 0, dst, 0, shapeBase.length);
				if (tool == TOOL_LINE) paintLine(dst, cw, ch, startPx, startPy, px, py, color, false);
				else paintRect(dst, cw, ch, startPx, startPy, px, py, color, tool == TOOL_BOX);
				if (mode == MODE_GUI) syncDraftToShop();
				texDirty = true;
				return true;
			}
		}
		return super.mouseDragged(mx, my, button, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(double mx, double my, int button) {
		dragSv = false;
		dragHue = false;
		dragAlpha = false;
		panning = false;
		dragFloatMove = false;
		dragFloatScale = false;
		if (painting && button == strokeBtn) {
			painting = false;
			strokeBtn = -1;
			shapeBase = null;
			history().cut();
		}
		return super.mouseReleased(mx, my, button);
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
		if (hit(mx, my, viewX, viewY, viewW, viewH) && scrollY != 0) {
			if (floatImg != null && hasControlDown()) {
				float factor = scrollY > 0 ? 1.12f : (1.0f / 1.12f);
				int nw = Math.max(1, Math.round(floatW * factor));
				int nh = Math.max(1, Math.round(floatH * factor));
				floatX += (floatW - nw) / 2;
				floatY += (floatH - nh) / 2;
				floatW = nw;
				floatH = nh;
				texDirty = true;
				return true;
			}
			float oldZoom = zoom;
			float factor = scrollY > 0 ? 1.2f : (1.0f / 1.2f);
			zoom = Mth.clamp(zoom * factor, 0.5f, 20.0f);
			panX = (float) (mx - (mx - panX) * (zoom / oldZoom));
			panY = (float) (my - (my - panY) * (zoom / oldZoom));
			return true;
		}
		if (hit(mx, my, leftColX, bodyTop + 33, 50, 14) && scrollY != 0) {
			brush = Mth.clamp(brush + (scrollY > 0 ? 1 : -1), 1, 12);
			return true;
		}
		return super.mouseScrolled(mx, my, scrollX, scrollY);
	}

	private void updateSv(double mx, double my) {
		sat = Mth.clamp((float) (mx - svX) / Math.max(1, svW - 1), 0.0f, 1.0f);
		val = 1.0f - Mth.clamp((float) (my - svY) / Math.max(1, svH - 1), 0.0f, 1.0f);
		rebuildColorFromHsv();
	}

	private void updateHue(double mx) {
		hue = Mth.clamp((float) (mx - hueX) / Math.max(1, hueW - 1), 0.0f, 1.0f);
		rebuildColorFromHsv();
	}

	private void updateAlpha(double mx) {
		alpha = Mth.clamp((int) Math.round((mx - alX) * 255.0 / Math.max(1, alW - 1)), 0, 255);
		rebuildColorFromHsv();
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (!(getFocused() instanceof EditBox)) {
			if (KeyCombos.undo(keyCode, modifiers)) {
				undo();
				return true;
			}
			if (KeyCombos.redo(keyCode, modifiers)) {
				redo();
				return true;
			}
			if (floatImg != null) {
				if (keyCode == 257 || keyCode == 335) {
					commitFloatIfAny();
					return true;
				}
				if (keyCode == 256) {
					cancelFloat();
					return true;
				}
			}
		}
		if (keyCode == 256) {
			onClose();
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	private void releaseTex() {
		if (dynId != null) {
			Minecraft.getInstance().getTextureManager().release(dynId);
			dynId = null;
			dynTex = null;
		}
	}

	@Override
	public void removed() {
		releaseTex();
		super.removed();
	}

	@Override
	public void onClose() {
		commitFloatIfAny();
		releaseTex();
		if (this.minecraft != null) {
			this.minecraft.setScreen(parent);
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
