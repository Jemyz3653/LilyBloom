package io.github.jemyz3653.lilybloom.mixin.client;

import io.github.jemyz3653.lilybloom.registry.ModBlocks;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Лист цветущей кувшинки рисуется ванильной текстурой {@code minecraft:block/lily_pad} (её меняют ресурспаки,
 * в том числе Faithful) и подкрашивается ровно так же, как обычная кувшинка.
 */
@Mixin(BlockColors.class)
public abstract class BlockColorsMixin {
    @Inject(method = "createDefault", at = @At("RETURN"))
    private static void lilybloom$tintBloomingLilyPads(final CallbackInfoReturnable<BlockColors> cir) {
        BlockColors colors = cir.getReturnValue();
        colors.register(colors.getTintSources(Blocks.LILY_PAD.defaultBlockState()),
                ModBlocks.BLOOMING_LILY_PADS.values().toArray(new Block[0]));
    }
}
