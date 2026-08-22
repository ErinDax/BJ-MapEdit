package cn.erindax.bjmapedit.client.widget;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.APIUtil;
import org.lwjgl.system.JNI;
import org.lwjgl.system.SharedLibrary;

import java.lang.reflect.Method;

public final class ImeCaret {
	private static final int GLFW_IME = 0x00033005;

	private static Method win32Hwnd;
	private static SharedLibrary imm32;
	private static long pImmAssociateContext;
	private static long pImmCreateContext;
	private static boolean resolved;
	private static boolean enabled;
	private static long ownedHimc;

	private ImeCaret() {}

	public static void setEnabled(boolean on) {
		if (Util.getPlatform() != Util.OS.WINDOWS) return;
		resolve();
		disableGlfwIme();
		long hwnd = hwnd();
		if (hwnd == 0 || pImmAssociateContext == 0) {
			enabled = on;
			return;
		}
		if (on) {
			if (ownedHimc == 0 && pImmCreateContext != 0) {
				ownedHimc = JNI.callP(pImmCreateContext);
			}
			if (ownedHimc != 0) {
				JNI.callPPP(hwnd, ownedHimc, pImmAssociateContext);
			}
		} else if (enabled) {
			JNI.callPPP(hwnd, 0L, pImmAssociateContext);
		}
		enabled = on;
	}

	public static void moveTo(int guiX, int guiY) {
		if (!enabled) setEnabled(true);
	}

	public static void moveToEditBox(EditBox box) {
		if (box == null || !box.isFocused() || !box.isVisible()) return;
		if (!enabled) setEnabled(true);
	}

	public static void moveToMultiLine(MultiLineEditBox box, Font font) {
		if (box == null || !box.isFocused() || !box.active) return;
		if (!enabled) setEnabled(true);
	}

	private static void disableGlfwIme() {
		try {
			Minecraft mc = Minecraft.getInstance();
			if (mc.getWindow() == null) return;
			GLFW.glfwSetInputMode(mc.getWindow().getWindow(), GLFW_IME, GLFW.GLFW_FALSE);
		} catch (Throwable ignored) {
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
			pImmAssociateContext = imm32.getFunctionAddress("ImmAssociateContext");
			pImmCreateContext = imm32.getFunctionAddress("ImmCreateContext");
		} catch (Throwable ignored) {
			imm32 = null;
		}
	}
}
