package cn.erindax.bjmapedit.client.widget;

import net.minecraft.client.input.InputQuirks;
import org.lwjgl.glfw.GLFW;

public final class KeyCombos {

	private KeyCombos() {}

	public static boolean shortcut(int modifiers) {
		return (modifiers & InputQuirks.EDIT_SHORTCUT_KEY_MODIFIER) != 0 && (modifiers & GLFW.GLFW_MOD_ALT) == 0;
	}

	public static boolean shift(int modifiers) {
		return (modifiers & GLFW.GLFW_MOD_SHIFT) != 0;
	}

	public static boolean undo(int keyCode, int modifiers) {
		return shortcut(modifiers) && keyCode == GLFW.GLFW_KEY_Z && !shift(modifiers);
	}

	public static boolean redo(int keyCode, int modifiers) {
		return shortcut(modifiers) && (keyCode == GLFW.GLFW_KEY_Y || keyCode == GLFW.GLFW_KEY_Z && shift(modifiers));
	}
}
