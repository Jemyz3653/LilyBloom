package io.github.jemyz3653.lilybloom.mixin;

import io.github.jemyz3653.lilybloom.block.BloomingLilyPadBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.dispenser.ShearsDispenseItemBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Раздатчик с ножницами тоже срезает цветок с цветущей кувшинки (как стрижёт улей). */
@Mixin(ShearsDispenseItemBehavior.class)
public abstract class ShearsDispenseItemBehaviorMixin {
    @Inject(method = "tryShearBeehive", at = @At("HEAD"), cancellable = true)
    private static void lilybloom$shearBloomingLilyPad(final ServerLevel level, final ItemStack tool, final BlockPos pos,
                                                       final CallbackInfoReturnable<Boolean> cir) {
        BlockState state = level.getBlockState(pos);
        if (BloomingLilyPadBlock.isFullBloom(state)) {
            BloomingLilyPadBlock.shear(level, pos, state, tool, null);
            cir.setReturnValue(true);
        }
    }
}
