package io.github.jemyz3653.lilybloom.block;

import io.github.jemyz3653.lilybloom.LilyBloom;
import io.github.jemyz3653.lilybloom.LilyBloomConfig;
import io.github.jemyz3653.lilybloom.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LilyPadBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * Цветущая кувшинка — отдельный блок {@code lilybloom:blooming_lily_pad}, а не blockstate обычной.
 *
 * <p>Наследует поведение обычной кувшинки (ставится на воду/лёд, лодка её ломает и не тормозит об неё,
 * та же форма), поверх добавляет срезание цветка ножницами.
 */
public class BloomingLilyPadBlock extends LilyPadBlock {
    /** Ножницы из других модов (общий тег Fabric/NeoForge). */
    private static final TagKey<Item> C_SHEARS = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "tools/shear"));

    /** Что выпадает при срезании ножницами. Таблица в data/lilybloom/loot_table/shearing/blooming_lily_pad.json — можно менять датапаком. */
    public static final ResourceKey<LootTable> SHEARING_LOOT =
            ResourceKey.create(Registries.LOOT_TABLE, LilyBloom.id("shearing/blooming_lily_pad"));

    public BloomingLilyPadBlock(final Properties properties) {
        super(properties);
    }

    public static boolean isShears(final ItemStack stack) {
        return stack.is(Items.SHEARS) || stack.is(C_SHEARS);
    }

    @Override
    protected InteractionResult useItemOn(
            final ItemStack itemStack,
            final BlockState state,
            final Level level,
            final BlockPos pos,
            final Player player,
            final InteractionHand hand,
            final BlockHitResult hitResult) {
        if (!isShears(itemStack)) {
            return super.useItemOn(itemStack, state, level, pos, player, hand, hitResult);
        }
        if (level instanceof ServerLevel serverLevel) {
            Item shears = itemStack.getItem();
            shear(serverLevel, pos, state, itemStack, player);
            itemStack.hurtAndBreak(1, player, hand.asEquipmentSlot());
            player.awardStat(Stats.ITEM_USED.get(shears));
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * Срезать цветок: выпадает цветок кувшинки (по лут-таблице), на месте остаётся обычная кувшинка.
     * Используется и игроком, и раздатчиком с ножницами.
     */
    public static void shear(final ServerLevel level, final BlockPos pos, final BlockState state, final ItemStack tool, @Nullable final Entity source) {
        level.playSound(null, pos, SoundEvents.GROWING_PLANT_CROP, SoundSource.BLOCKS, 1.0F, 1.0F);
        dropFromBlockInteractLootTable(level, SHEARING_LOOT, pos, state, null, tool, source,
                (serverLevel, drop) -> popResourceFromFace(serverLevel, pos, Direction.UP, drop));
        level.setBlock(pos, Blocks.LILY_PAD.defaultBlockState(), Block.UPDATE_ALL);
        level.gameEvent(source, GameEvent.SHEAR, pos);
    }

    /** Превратить обычную кувшинку в цветущую (костная мука, цветок кувшинки или само по себе). */
    public static void bloom(final ServerLevel level, final BlockPos pos, @Nullable final Entity source) {
        level.setBlock(pos, ModBlocks.BLOOMING_LILY_PAD.defaultBlockState(), Block.UPDATE_ALL);
        level.gameEvent(source, GameEvent.BLOCK_CHANGE, pos);
    }

    /** Самостоятельное цветение обычной кувшинки. Вызывается из случайного тика ванильной кувшинки (см. LilyPadBlockMixin). */
    public static void tryNaturalBloom(final ServerLevel level, final BlockPos pos, final RandomSource random) {
        LilyBloomConfig config = LilyBloomConfig.get();
        if (!config.naturalBlooming || random.nextInt(config.bloomChance) != 0) {
            return;
        }
        if (!level.canSeeSky(pos)) {
            return;
        }
        if (config.onlyDuringDay && !level.isBrightOutside()) {
            return;
        }
        if (config.maxBloomingNearby > 0 && countBloomingNearby(level, pos, config.maxBloomingNearby) >= config.maxBloomingNearby) {
            return;
        }
        bloom(level, pos, null);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.25, pos.getZ() + 0.5, 6, 0.3, 0.1, 0.3, 0.0);
    }

    private static int countBloomingNearby(final ServerLevel level, final BlockPos center, final int stopAt) {
        int count = 0;
        for (BlockPos p : BlockPos.betweenClosed(center.offset(-3, -1, -3), center.offset(3, 1, 3))) {
            if (level.getBlockState(p).is(ModBlocks.BLOOMING_LILY_PAD) && ++count >= stopAt) {
                break;
            }
        }
        return count;
    }
}
