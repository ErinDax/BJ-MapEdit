package cn.erindax.bjmapedit.client.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;

public final class FacePlayer {
	private FacePlayer() {}

	public static float yaw(Vec3 at) {
		var player = Minecraft.getInstance().player;
		if (player == null) return 0f;
		double dx = player.getX() - at.x;
		double dz = player.getZ() - at.z;
		if (dx * dx + dz * dz < 1.0E-4) return Mth.wrapDegrees(player.getYRot() + 180f);
		return Mth.wrapDegrees((float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90f);
	}

	public static String withRotation(String nbt, Vec3 at) {
		String rotation = String.format(Locale.ROOT, "Rotation:[%.1ff,0.0f]", yaw(at));
		String body = nbt == null ? "" : nbt.trim();
		int end = body.lastIndexOf('}');
		if (!body.startsWith("{") || end < 0) return "{" + rotation + "}";
		String inner = body.substring(1, end).trim();
		return "{" + (inner.isEmpty() ? "" : inner + ",") + rotation + "}";
	}
}
