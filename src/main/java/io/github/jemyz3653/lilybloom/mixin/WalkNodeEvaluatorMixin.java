package io.github.jemyz3653.lilybloom.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.jemyz3653.lilybloom.block.BloomingLilyPadBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Мобы прокладывают путь по цветущей кувшинке так же, как по обычной (ванилла проверяет именно Blocks.LILY_PAD). */
@Mixin(WalkNodeEvaluator.class)
public abstract class WalkNodeEvaluatorMixin {
    @WrapOperation(
            method = "getPathTypeFromState",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;is(Ljava/lang/Object;)Z"))
    private static boolean lilybloom$bloomingCountsAsLilyPad(final BlockState state, final Object type, final Operation<Boolean> original) {
        return original.call(state, type) || (type == Blocks.LILY_PAD && state.getBlock() instanceof BloomingLilyPadBlock);
    }
}
