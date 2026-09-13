package cn.erindax.bjmapedit.client.widget;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import org.lwjgl.system.APIUtil;
import org.lwjgl.system.JNI;
import org.lwjgl.system.SharedLibrary;

import java.lang.reflect.Method;

public final class ImeCaret {
	private static final int IACE_DEFAULT = 0x0010;

	private static Method win32Hwnd;
	private static SharedLibrary imm32;
	private static long pImmAssociateContextEx;
	private static long pImmGetContext;
	private static long pImmReleaseContext;
	private static long pImmSetOpenStatus;
	private static boolean resolved;
	private static boolean enabled;

	private ImeCaret() {}

	public static void setEnabled(boolean on) {
		if (Util.getPlatform() != Util.OS.WINDOWS) return;
		resolve();
		enabled = on;
		if (on) {
			attachDefault();
		} else {
			closeIme();
		}
	}

	public static void moveTo(int guiX, int guiY) {
		if (!enabled) {
			setEnabled(true);
			return;
		}
		attachDefault();
	}

	public static void moveToEditBox(EditBox box) {
		if (box == null || !box.isFocused() || !box.isVisible()) return;
		moveTo(box.getX(), box.getY() + box.getHeight());
	}

	public static void moveToMultiLine(MultiLineEditBox box, Font font) {
		if (box == null || !box.isFocused() || !box.active) return;
		moveTo(box.getX(), box.getY() + Math.min(24, box.getHeight()));
	}

	private static void attachDefault() {
		if (Util.getPlatform() != Util.OS.WINDOWS) return;
		resolve();
		long hwnd = hwnd();
		if (hwnd == 0 || pImmAssociateContextEx == 0) return;
		JNI.callPPI(hwnd, 0L, IACE_DEFAULT, pImmAssociateContextEx);
	}

	private static void closeIme() {
		long hwnd = hwnd();
		if (hwnd == 0 || pImmGetContext == 0 || pImmSetOpenStatus == 0) return;
		long himc = JNI.callPP(hwnd, pImmGetContext);
		if (himc == 0) return;
		JNI.callPI(himc, 0, pImmSetOpenStatus);
		if (pImmReleaseContext != 0) {
			JNI.callPPI(hwnd, himc, pImmReleaseContext);
		}
	}

	private static long hwnd() {
		try {
			Minecraft mc = Minecraft.getInstance();
			if (mc.getWindow() == null || win32Hwnd == null) return 0;
			Object raw = win32Hwnd.invoke(null, mc.getWindow().getWindow());
			return raw instanceof Long l ? l : 0;
		} catch (Throwable ignored) {
			return 0;
		}
	}

	private static void resolve() {
		if (resolved) return;
		resolved = true;
		if (Util.getPlatform() != Util.OS.WINDOWS) return;
		try {
			Class<?> n = Class.forName("org.lwjgl.glfw.GLFWNativeWin32");
			win32Hwnd = n.getMethod("glfwGetWin32Window", long.class);
		} catch (Throwable ignored) {
		}
		try {
			imm32 = APIUtil.apiCreateLibrary("imm32");
			pImmAssociateContextEx = imm32.getFunctionAddress("ImmAssociateContextEx");
			pImmGetContext = imm32.getFunctionAddress("ImmGetContext");
			pImmReleaseContext = imm32.getFunctionAddress("ImmReleaseContext");
			pImmSetOpenStatus = imm32.getFunctionAddress("ImmSetOpenStatus");
		} catch (Throwable ignored) {
			imm32 = null;
		}
	}
}
