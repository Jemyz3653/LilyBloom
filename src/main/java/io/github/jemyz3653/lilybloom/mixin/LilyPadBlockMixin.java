package io.github.jemyz3653.lilybloom.mixin;

import io.github.jemyz3653.lilybloom.block.BloomingLilyPadBlock;
import io.github.jemyz3653.lilybloom.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BonemealSource;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LilyPadBlock;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Добавляет ванильной кувшинке:
 * <ul>
 *   <li>реакцию на костную муку (рукой и из раздатчика) — превращается в цветущую кувшинку;</li>
 *   <li>случайные тики — сама изредка расцветает (так цветущие появляются и в уже сгенерированном мире);</li>
 *   <li>ПКМ цветком кувшинки — цветок «возвращается» на кувшинку.</li>
 * </ul>
 * Всё строго только для {@code minecraft:lily_pad}: цветущая кувшинка наследует LilyPadBlock,
 * поэтому каждая проверка начинается с {@code state.is(Blocks.LILY_PAD)}.
 */
@Mixin(LilyPadBlock.class)
public abstract class LilyPadBlockMixin extends VegetationBlock implements BonemealableBlock {
    protected LilyPadBlockMixin(final Properties properties) {
        super(properties);
    }

    // ---------- костная мука ----------

    @Override
    public boolean isValidBonemealTarget(final LevelReader level, final BlockPos pos, final BlockState state, final BonemealSource source) {
        return state.is(Blocks.LILY_PAD);
    }

    @Override
    public boolean isBonemealSuccess(final Level level, final RandomSource random, final BlockPos pos, final BlockState state, final BonemealSource source) {
        return true;
    }

    @Override
    public void performBonemeal(final ServerLevel level, final RandomSource random, final BlockPos pos, final BlockState state, final BonemealSource source) {
        if (state.is(Blocks.LILY_PAD)) {
            BloomingLilyPadBlock.bloom(level, pos, null);
        }
    }

    // ---------- самостоятельное цветение ----------

    @Override
    protected boolean isRandomlyTicking(final BlockState state) {
        return state.is(Blocks.LILY_PAD) || super.isRandomlyTicking(state);
    }

    @Override
    protected void randomTick(final BlockState state, final ServerLevel level, final BlockPos pos, final RandomSource random) {
        if (state.is(Blocks.LILY_PAD)) {
            BloomingLilyPadBlock.tryNaturalBloom(level, pos, random);
        } else {
            super.randomTick(state, level, pos, random);
        }
    }

    // ---------- цветок обратно на кувшинку ----------

    @Override
    protected InteractionResult useItemOn(
            final ItemStack itemStack,
            final BlockState state,
            final Level level,
            final BlockPos pos,
            final Player player,
            final InteractionHand hand,
            final BlockHitResult hitResult) {
        if (!state.is(Blocks.LILY_PAD) || !itemStack.is(ModItems.LILY_FLOWER)) {
            return super.useItemOn(itemStack, state, level, pos, player, hand, hitResult);
        }
        if (level instanceof ServerLevel serverLevel) {
            BloomingLilyPadBlock.bloom(serverLevel, pos, player);
            level.playSound(null, pos, SoundEvents.LILY_PAD_PLACE, SoundSource.BLOCKS, 1.0F, 1.2F);
            itemStack.consume(1, player);
            player.awardStat(Stats.ITEM_USED.get(ModItems.LILY_FLOWER));
        }
        return InteractionResult.SUCCESS;
    }
}
