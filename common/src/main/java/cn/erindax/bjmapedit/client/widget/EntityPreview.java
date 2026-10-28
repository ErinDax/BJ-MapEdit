package cn.erindax.bjmapedit.client.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class EntityPreview {
	private EntityPreview() {
	}

	public static void render(GuiGraphics g, Entity entity, int centerX, int centerY, int width, int height, float scale,
			Vector3f translation, Quaternionf rotation, Quaternionf camera) {
		EntityRenderState state = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entity).createRenderState(entity, 1.0F);
		state.lightCoords = LightTexture.FULL_BRIGHT;
		state.shadowPieces.clear();
		state.outlineColor = 0;
		int x0 = centerX - width / 2;
		int y0 = centerY - height / 2;
		g.submitEntityRenderState(state, scale, translation, rotation, camera, x0, y0, x0 + width, y0 + height);
	}
}
