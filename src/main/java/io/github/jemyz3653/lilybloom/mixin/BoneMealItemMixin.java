package io.github.jemyz3653.lilybloom.mixin;

import io.github.jemyz3653.lilybloom.block.BloomingLilyPadBlock;
import io.github.jemyz3653.lilybloom.block.LilyStage;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Костная мука: обычная кувшинка пускает бутон случайного цвета, бутон и полураскрытый бутон
 * переходят на следующую стадию. Через growCrop идут и клик игрока, и раздатчик.
 *
 * <p>Сделано инъекцией в начало growCrop, а не через BonemealableBlock на LilyPadBlock, чтобы
 * не конфликтовать с Carpet Extra: его правило betterBonemeal добавляет кувшинке те же методы.
 * На распустившуюся кувшинку инъекция не влияет — там при включённом betterBonemeal работает Carpet Extra.
 */
@Mixin(BoneMealItem.class)
public abstract class BoneMealItemMixin {
    @Inject(method = "growCrop", at = @At("HEAD"), cancellable = true)
    private static void lilybloom$growLily(final ItemStack itemStack, final Level level, final BlockPos pos,
                                           final CallbackInfoReturnable<Boolean> cir) {
        BlockState state = level.getBlockState(pos);
        boolean plainPad = state.is(Blocks.LILY_PAD);
        boolean growingBud = state.getBlock() instanceof BloomingLilyPadBlock
                && state.getValue(BloomingLilyPadBlock.STAGE) != LilyStage.BLOOM;
        if (!plainPad && !growingBud) {
            return;
        }
        if (level instanceof ServerLevel serverLevel) {
            if (plainPad) {
                BloomingLilyPadBlock.startBud(serverLevel, pos, serverLevel.getRandom());
                BloomingLilyPadBlock.bloomParticles(serverLevel, pos);
            } else {
                BloomingLilyPadBlock.advance(serverLevel, pos, state);
                BloomingLilyPadBlock.bloomParticles(serverLevel, pos);
            }
            itemStack.shrink(1);
        }
        cir.setReturnValue(true);
    }
}
