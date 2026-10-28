package cn.erindax.bjmapedit.client.mixin;

import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.MultilineTextField;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MultiLineEditBox.class)
public interface MultiLineEditBoxAccessor {
	@Accessor("textField")
	MultilineTextField mapedit$textField();

	@Invoker("scrollToCursor")
	void mapedit$scrollToCursor();
}
