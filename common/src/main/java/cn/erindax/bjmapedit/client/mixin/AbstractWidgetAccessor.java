package cn.erindax.bjmapedit.client.mixin;

import net.minecraft.client.gui.components.AbstractWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractWidget.class)
public interface AbstractWidgetAccessor {
	@Accessor("height")
	void mapedit$setHeight(int height);
}
