package cn.erindax.bjmapedit.client.mixin;

import net.minecraft.advancements.criterion.BlockPredicate;
import net.minecraft.world.item.AdventureModePredicate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(AdventureModePredicate.class)
public interface AdventureModePredicateAccessor {
	@Accessor("predicates")
	List<BlockPredicate> mapedit$predicates();
}
