package io.github.jemyz3653.lilybloom.mixin;

import io.github.jemyz3653.lilybloom.LilyColor;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LilyPadBlock;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Добавляет ванильной кувшинке:
 * <ul>
 *   <li>случайные тики — изредка сама пускает бутон случайного цвета (так цветы появляются и в уже сгенерированном мире);</li>
 *   <li>ПКМ цветком кувшинки — цветок этого цвета «возвращается» на кувшинку сразу распустившимся.</li>
 * </ul>
 * Всё строго только для {@code minecraft:lily_pad}: цветущая кувшинка наследует LilyPadBlock,
 * поэтому каждая проверка начинается с {@code state.is(Blocks.LILY_PAD)}.
 *
 * <p>Костная мука обрабатывается не здесь, а в {@link BoneMealItemMixin}: если добавлять кувшинке
 * методы BonemealableBlock, они конфликтуют с Carpet Extra (у него свой миксин на LilyPadBlock
 * для правила betterBonemeal), и Mixin молча пропускает наши методы.
 */
@Mixin(LilyPadBlock.class)
public abstract class LilyPadBlockMixin extends VegetationBlock {
    protected LilyPadBlockMixin(final Properties properties) {
        super(properties);
    }

    // ---------- самостоятельное цветение ----------

    @Override
    protected boolean isRandomlyTicking(final BlockState state) {
        return state.is(Blocks.LILY_PAD) || super.isRandomlyTicking(state);
    }

    @Override
    protected void randomTick(final BlockState state, final ServerLevel level, final BlockPos pos, final RandomSource random) {
        if (state.is(Blocks.LILY_PAD)) {
            BloomingLilyPadBlock.tryNaturalBud(level, pos, random);
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
        LilyColor color = ModItems.flowerColor(itemStack);
        if (!state.is(Blocks.LILY_PAD) || color == null) {
            return super.useItemOn(itemStack, state, level, pos, player, hand, hitResult);
        }
        if (level instanceof ServerLevel serverLevel) {
            Item flower = itemStack.getItem();
            BloomingLilyPadBlock.bloom(serverLevel, pos, color, player);
            level.playSound(null, pos, SoundEvents.LILY_PAD_PLACE, SoundSource.BLOCKS, 1.0F, 1.2F);
            itemStack.consume(1, player);
            player.awardStat(Stats.ITEM_USED.get(flower));
        }
        return InteractionResult.SUCCESS;
    }
}
