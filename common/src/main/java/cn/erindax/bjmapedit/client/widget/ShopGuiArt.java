package cn.erindax.bjmapedit.client.widget;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public final class ShopGuiArt {
	public static final int GUI_W = 176;
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
	private static final ResourceLocation GENERIC_54 = new ResourceLocation("textures/gui/container/generic_54.png");
	private static final ResourceLocation SHULKER_BG = new ResourceLocation("textures/gui/container/shulker_box.png");

	public record Img(int w, int h, int[] pixels) {}

	private ShopGuiArt() {}

	public static int guiHeight(String container) {
		return container != null && container.startsWith("large_") ? 221 : 167;
	}

	public static int swapRb(int c) {
		return (c & 0xFF00FF00) | ((c >> 16) & 0xFF) | ((c & 0xFF) << 16);
	}

	public static NativeImage toNative(int w, int h, int[] argb) {
		NativeImage img = new NativeImage(NativeImage.Format.RGBA, w, h, false);
		for (int y = 0; y < h; y++) {
			int row = y * w;
			for (int x = 0; x < w; x++) {
				img.setPixelRGBA(x, y, swapRb(argb[row + x]));
			}
		}
		return img;
	}

	public static Img fromNative(NativeImage img) {
		int w = img.getWidth();
		int h = img.getHeight();
		int[] out = new int[w * h];
		for (int y = 0; y < h; y++) {
			int row = y * w;
			for (int x = 0; x < w; x++) {
				out[row + x] = swapRb(img.getPixelRGBA(x, y));
			}
		}
		return new Img(w, h, out);
	}

	public static void writePng(Path dest, int w, int h, int[] argb) throws IOException {
		Files.createDirectories(dest.getParent());
		try (NativeImage img = toNative(w, h, argb)) {
			img.writeToFile(dest);
		}
	}

	public static Img readImage(Path file) throws IOException {
		byte[] bytes = Files.readAllBytes(file);
		ByteBuffer buf = MemoryUtil.memAlloc(bytes.length);
		try (MemoryStack stack = MemoryStack.stackPush()) {
			buf.put(bytes).flip();
			IntBuffer pw = stack.mallocInt(1);
			IntBuffer ph = stack.mallocInt(1);
			IntBuffer pc = stack.mallocInt(1);
			ByteBuffer decoded = STBImage.stbi_load_from_memory(buf, pw, ph, pc, 4);
			if (decoded != null) {
				try {
					int w = pw.get(0);
					int h = ph.get(0);
					if (w > 0 && h > 0) {
						int[] out = new int[w * h];
						for (int i = 0; i < out.length; i++) {
							int r = decoded.get(i * 4) & 0xFF;
							int g = decoded.get(i * 4 + 1) & 0xFF;
							int b = decoded.get(i * 4 + 2) & 0xFF;
							int a = decoded.get(i * 4 + 3) & 0xFF;
							out[i] = (a << 24) | (r << 16) | (g << 8) | b;
						}
						return new Img(w, h, out);
					}
				} finally {
					STBImage.stbi_image_free(decoded);
				}
			}
		} finally {
			MemoryUtil.memFree(buf);
		}
		BufferedImage bi = ImageIO.read(new ByteArrayInputStream(bytes));
		if (bi == null || bi.getWidth() <= 0 || bi.getHeight() <= 0) {
			throw new IOException(file.getFileName() + " cannot be decoded");
		}
		int w = bi.getWidth();
		int h = bi.getHeight();
		int[] out = new int[w * h];
		bi.getRGB(0, 0, w, h, out, 0, w);
		return new Img(w, h, out);
	}

	private static Img readResourceImage(ResourceLocation loc, boolean vanillaOnly) {
		Minecraft mc = Minecraft.getInstance();
		try {
			if (vanillaOnly) {
				IoSupplier<InputStream> sup = mc.getVanillaPackResources().getResource(PackType.CLIENT_RESOURCES, loc);
				if (sup != null) {
					try (InputStream in = sup.get(); NativeImage ni = NativeImage.read(in)) {
						return fromNative(ni);
					}
				}
			}
			Optional<Resource> res = mc.getResourceManager().getResource(loc);
			if (res.isPresent()) {
				try (InputStream in = res.get().open(); NativeImage ni = NativeImage.read(in)) {
					return fromNative(ni);
				}
			}
		} catch (Exception ignored) {}
		return null;
	}

	public static Img grabContainerBg(String container, boolean vanillaOnly) {
		boolean shulker = "shulker_box".equals(container);
		boolean large = container != null && container.startsWith("large_");
		int outH = large ? 221 : 167;
		Img sheet = readResourceImage(shulker ? SHULKER_BG : GENERIC_54, vanillaOnly);
		if (sheet == null && !vanillaOnly) {
			sheet = readResourceImage(shulker ? SHULKER_BG : GENERIC_54, true);
		}
		int[] out = new int[GUI_W * outH];
		if (sheet == null) {
			Arrays.fill(out, 0xFFC6C6C6);
			return new Img(GUI_W, outH, out);
		}
		if (shulker) {
			blitSheetRegion(sheet, 0, 0, GUI_W, 167, out, GUI_W, 0, 0);
		} else {
			int topH = large ? 125 : 71;
			blitSheetRegion(sheet, 0, 0, GUI_W, topH, out, GUI_W, 0, 0);
			blitSheetRegion(sheet, 0, 126, GUI_W, 96, out, GUI_W, 0, topH);
		}
		return new Img(GUI_W, outH, out);
	}

	private static void blitSheetRegion(Img sheet, int u, int v, int rw, int rh, int[] dst, int dstW, int dx, int dy) {
		for (int y = 0; y < rh; y++) {
			int sy = Math.min(sheet.h - 1, (v + y) * sheet.h / 256);
			int sRow = sy * sheet.w;
			int dRow = (dy + y) * dstW + dx;
			for (int x = 0; x < rw; x++) {
				int sx = Math.min(sheet.w - 1, (u + x) * sheet.w / 256);
				dst[dRow + x] = sheet.pixels[sRow + sx];
			}
		}
	}

	public static int samplePanelColor(int[] refPixels, int w, int h) {
		if (refPixels != null && w > 5 && h > 5) {
			int c = refPixels[5 * w + 5];
			if (((c >>> 24) & 0xFF) >= 200) return c | 0xFF000000;
		}
		return 0xFFC6C6C6;
	}

	public static void eraseSlot(int[] pixels, int w, int h, int slot, int fillColor) {
		int sx = 7 + (slot % 9) * 18;
		int sy = 17 + (slot / 9) * 18;
		for (int y = 0; y < 18 && sy + y < h; y++) {
			int row = (sy + y) * w;
			for (int x = 0; x < 18 && sx + x < w; x++) {
				pixels[row + sx + x] = fillColor;
			}
		}
	}

	public static void restoreSlot(int[] pixels, int[] refPixels, int w, int h, int slot) {
		if (refPixels == null || refPixels.length < w * h) return;
		int sx = 7 + (slot % 9) * 18;
		int sy = 17 + (slot / 9) * 18;
		for (int y = 0; y < 18 && sy + y < h; y++) {
			int row = (sy + y) * w;
			for (int x = 0; x < 18 && sx + x < w; x++) {
				pixels[row + sx + x] = refPixels[row + sx + x];
			}
		}
	}

	public static Img grabSlotPatch(int[] srcPixels, int w, int h, int slot, int outSize) {
		int[] patch18 = new int[18 * 18];
		int sx = 7 + (slot % 9) * 18;
		int sy = 17 + (slot / 9) * 18;
		for (int y = 0; y < 18; y++) {
			for (int x = 0; x < 18; x++) {
				int px = sx + x;
				int py = sy + y;
				patch18[y * 18 + x] = px < w && py < h ? srcPixels[py * w + px] : 0xFF8B8B8B;
			}
		}
		if (outSize == 18) return new Img(18, 18, patch18);
		if (outSize == 16) {
			int[] inner = new int[16 * 16];
			for (int y = 0; y < 16; y++) {
				System.arraycopy(patch18, (y + 1) * 18 + 1, inner, y * 16, 16);
			}
			return new Img(16, 16, inner);
		}
		return scaleNearest(new Img(18, 18, patch18), outSize, outSize);
	}

	public static Img grabItemIcon(ItemStack stack) {
		if (stack == null || stack.isEmpty()) return null;
		Minecraft mc = Minecraft.getInstance();
		try {
			BakedModel model = mc.getItemRenderer().getModel(stack, mc.level, mc.player, 0);
			ResourceLocation sprite = model.getParticleIcon().contents().name();
			ResourceLocation file = new ResourceLocation(sprite.getNamespace(), "textures/" + sprite.getPath() + ".png");
			Img raw = readResourceImage(file, false);
			if (raw == null) return null;
			int frame = Math.min(raw.w, raw.h);
			if (raw.w == frame && raw.h == frame) return raw;
			int[] sq = new int[frame * frame];
			for (int y = 0; y < frame; y++) {
				System.arraycopy(raw.pixels, y * raw.w, sq, y * frame, frame);
			}
			return new Img(frame, frame, sq);
		} catch (Exception e) {
			return null;
		}
	}

	public static Img scaleNearest(Img src, int outW, int outH) {
		int[] out = new int[outW * outH];
		for (int y = 0; y < outH; y++) {
			int sy = Math.min(src.h - 1, y * src.h / outH);
			for (int x = 0; x < outW; x++) {
				int sx = Math.min(src.w - 1, x * src.w / outW);
				out[y * outW + x] = src.pixels[sy * src.w + sx];
			}
		}
		return new Img(outW, outH, out);
	}

	public static Img adaptHeight(Img src, String container) {
		int targetH = guiHeight(container);
		if (src.w == GUI_W && src.h == targetH) return new Img(src.w, src.h, src.pixels.clone());
		Img base = grabContainerBg(container, false);
		int copyW = Math.min(GUI_W, src.w);
		int copyTop = Math.min(targetH - 96, Math.max(0, src.h - 96));
		for (int y = 0; y < copyTop; y++) {
			System.arraycopy(src.pixels, y * src.w, base.pixels, y * GUI_W, copyW);
		}
		if (src.h >= 96 && targetH >= 96) {
			for (int y = 0; y < 96; y++) {
				System.arraycopy(src.pixels, (src.h - 96 + y) * src.w, base.pixels, (targetH - 96 + y) * GUI_W, copyW);
			}
		}
		return base;
	}

	public static Path guiTextureFile(Path packDir, String ns, String shopId) {
		return packDir.resolve("assets").resolve(ns).resolve("textures").resolve("gui").resolve("bjshop").resolve(shopId + ".png");
	}

	public static Path guiFontFile(Path packDir, String ns, String shopId) {
		return packDir.resolve("assets").resolve(ns).resolve("font").resolve("bjshop_" + shopId + ".json");
	}

	public static String writeGuiFont(Path packDir, String ns, String shopId, int w, int h, int[] argb) throws IOException {
		Path png = guiTextureFile(packDir, ns, shopId);
		writePng(png, w, h, argb);
		int actual = 0;
		for (int x = w - 1; x >= 0; x--) {
			boolean any = false;
			for (int y = 0; y < h; y++) {
				if (((argb[y * w + x] >>> 24) & 0xFF) != 0) {
					any = true;
					break;
				}
			}
			if (any) {
				actual = x + 1;
				break;
			}
		}
		int glyphAdvance = actual + 1;
		int backToTitle = 8 - glyphAdvance;
		JsonObject root = new JsonObject();
		JsonArray providers = new JsonArray();

		JsonObject space = new JsonObject();
		space.addProperty("type", "space");
		JsonObject advances = new JsonObject();
		advances.addProperty("\uE000", -8);
		advances.addProperty("\uE002", backToTitle);
		space.add("advances", advances);
		providers.add(space);

		JsonObject bitmap = new JsonObject();
		bitmap.addProperty("type", "bitmap");
		bitmap.addProperty("file", ns + ":gui/bjshop/" + shopId + ".png");
		bitmap.addProperty("height", h);
		bitmap.addProperty("ascent", 13);
		JsonArray chars = new JsonArray();
		chars.add("\uE001");
		bitmap.add("chars", chars);
		providers.add(bitmap);

		root.add("providers", providers);
		Path fontJson = guiFontFile(packDir, ns, shopId);
		Files.createDirectories(fontJson.getParent());
		Files.writeString(fontJson, GSON.toJson(root) + "\n", StandardCharsets.UTF_8);
		return ns + ":bjshop_" + shopId;
	}

	public static void removeGuiFont(Path packDir, String ns, String shopId) {
		try {
			Files.deleteIfExists(guiTextureFile(packDir, ns, shopId));
			Files.deleteIfExists(guiFontFile(packDir, ns, shopId));
		} catch (IOException ignored) {}
	}

	public static Img loadSavedGui(ShopData shop) {
		if (shop.draftGuiPixels != null && shop.draftGuiW > 0 && shop.draftGuiH > 0) {
			return new Img(shop.draftGuiW, shop.draftGuiH, shop.draftGuiPixels.clone());
		}
		if (shop.guiFont == null || shop.guiFont.isBlank()) return null;
		ResourceLocation fontId = ResourceLocation.tryParse(shop.guiFont);
		if (fontId == null) return null;
		String ns = fontId.getNamespace();
		String id = shop.id;
		if (id.isEmpty() && fontId.getPath().startsWith("bjshop_")) {
			id = fontId.getPath().substring("bjshop_".length());
		}
		if (id.isEmpty()) return null;
		if (!shop.guiPack.isBlank()) {
			Path file = guiTextureFile(ResourcePacks.root().resolve(shop.guiPack), ns, id);
			if (Files.isRegularFile(file)) {
				try {
					return readImage(file);
				} catch (IOException ignored) {}
			}
		}
		ResourceLocation resLoc = new ResourceLocation(ns, "textures/gui/bjshop/" + id + ".png");
		return readResourceImage(resLoc, false);
	}

	public static void flushPendingGui(ShopData shop) throws IOException {
		if (!ShopData.validId(shop.id)) return;
		boolean needWrite = shop.guiUnapplied;
		if (!needWrite && !shop.guiFont.isBlank() && !shop.guiFont.endsWith(":bjshop_" + shop.id)) {
			needWrite = true;
		}
		if (!needWrite && shop.lockHidden && shop.activeHiddenMask() != 0L && shop.blankCmd <= 0) {
			needWrite = true;
		}
		if (!needWrite) return;
		Img img = loadSavedGui(shop);
		if (img == null && shop.activeHiddenMask() != 0L) {
			img = grabContainerBg(shop.container, false);
			int fillCol = samplePanelColor(img.pixels(), img.w(), img.h());
			for (int i = 0; i < shop.size(); i++) {
				if (shop.isHidden(i)) eraseSlot(img.pixels(), img.w(), img.h(), i, fillCol);
			}
		}
		if (img == null) return;
		Img adapted = adaptHeight(img, shop.container);
		List<String> packs = ResourcePacks.folderPacks();
		String pack = !shop.guiPack.isBlank() && packs.contains(shop.guiPack) ? shop.guiPack : null;
		if (pack == null) {
			List<String> sel = new java.util.ArrayList<>(Minecraft.getInstance().getResourcePackRepository().getSelectedIds());
			for (int i = sel.size() - 1; i >= 0; i--) {
				String sid = sel.get(i);
				if (sid.startsWith("file/") && packs.contains(sid.substring(5))) {
					pack = sid.substring(5);
					break;
				}
			}
		}
		if (pack == null && !packs.isEmpty()) pack = packs.get(0);
		if (pack == null) pack = ResourcePacks.createPack("bjmapedit_models");
		Path dir = ResourcePacks.root().resolve(pack);
		String ns = ResourcePacks.namespace(pack);
		shop.guiFont = writeGuiFont(dir, ns, shop.id, adapted.w(), adapted.h(), adapted.pixels());
		shop.guiPack = pack;
		shop.blankCmd = shop.lockHidden && shop.activeHiddenMask() != 0L ? ItemModels.ensureBlankModel(dir, ns) : 0;
		shop.draftGuiW = adapted.w();
		shop.draftGuiH = adapted.h();
		shop.draftGuiPixels = adapted.pixels();
		shop.guiUnapplied = false;
		ResourcePacks.enableAndReload(pack);
	}

	public static int writeButtonAsset(Path packDir, String ns, String btnName, int w, int h, int[] argb, boolean fullSlot18, Item targetItem, int reuseCmd) throws IOException {
		String texId = ItemModels.resourceId(ns, btnName);
		Path pngDest = packDir.resolve("assets").resolve(ns).resolve("textures").resolve("item").resolve(btnName + ".png");
		if (w == 18 && h == 18) {
			int[] padded = new int[32 * 32];
			for (int y = 0; y < 18; y++) {
				System.arraycopy(argb, y * 18, padded, y * 32, 18);
			}
			writePng(pngDest, 32, 32, padded);
			float from = fullSlot18 ? -1.0f : 0.0f;
			float to = fullSlot18 ? 17.0f : 16.0f;
			ItemModels.writeGuiQuadModel(packDir, ns, btnName, texId, from, to, 9.0f);
		} else {
			writePng(pngDest, w, h, argb);
			if (fullSlot18) {
				ItemModels.writeGuiQuadModel(packDir, ns, btnName, texId, -1.0f, 17.0f, 16.0f);
			} else {
				ItemModels.writeGenerated(packDir, ns, btnName, ItemModels.baseParent(targetItem), texId);
			}
		}
		int max = 0;
		for (ItemModels.Mapping m : ItemModels.read(packDir, targetItem)) {
			if (texId.equals(m.model())) {
				ItemModels.put(packDir, targetItem, m.cmd(), texId);
				return m.cmd();
			}
			if (reuseCmd > 0 && m.cmd() == reuseCmd && m.model().startsWith(ns + ":item/bjshop_btn_")) {
				ItemModels.put(packDir, targetItem, reuseCmd, texId);
				return reuseCmd;
			}
			max = Math.max(max, m.cmd());
		}
		int cmd = max < Integer.MAX_VALUE ? max + 1 : max;
		ItemModels.put(packDir, targetItem, cmd, texId);
		return cmd;
	}
}
