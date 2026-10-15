package io.github.jemyz3653.lilybloom.block;

import io.github.jemyz3653.lilybloom.LilyBloom;
import io.github.jemyz3653.lilybloom.LilyBloomConfig;
import io.github.jemyz3653.lilybloom.LilyColor;
import io.github.jemyz3653.lilybloom.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
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
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * Цветущая кувшинка одного цвета — отдельный блок на каждый цвет ({@code lilybloom:blooming_lily_pad} — розовая,
 * {@code lilybloom:white_blooming_lily_pad} и т. д.), а не blockstate ванильной кувшинки.
 *
 * <p>Стадии ({@link #STAGE}): бутон → полураскрытый бутон → цветок. По умолчанию — цветок, поэтому
 * блоки из 1.0.x (без стадии) загружаются распустившимися. Бутоны сами дорастают от случайных тиков,
 * костная мука продвигает на одну стадию (см. BoneMealItemMixin). Ножницы срезают цветок только
 * с распустившейся кувшинки.
 *
 * <p>От ванильной кувшинки наследуется всё остальное: ставится на воду/лёд, лодка ломает и не тормозит.
 */
public class BloomingLilyPadBlock extends LilyPadBlock {
    public static final EnumProperty<LilyStage> STAGE = EnumProperty.create("stage", LilyStage.class);

    /** Ножницы из других модов (общий тег Fabric/NeoForge). */
    private static final TagKey<Item> C_SHEARS = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "tools/shear"));

    private final LilyColor color;

    public BloomingLilyPadBlock(final LilyColor color, final Properties properties) {
        super(properties);
        this.color = color;
        registerDefaultState(stateDefinition.any().setValue(STAGE, LilyStage.BLOOM));
    }

    public LilyColor getColor() {
        return color;
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(STAGE);
    }

    public static boolean isShears(final ItemStack stack) {
        return stack.is(Items.SHEARS) || stack.is(C_SHEARS);
    }

    public static boolean isFullBloom(final BlockState state) {
        return state.getBlock() instanceof BloomingLilyPadBlock && state.getValue(STAGE) == LilyStage.BLOOM;
    }

    // ---------------------------------------------------------------- рост бутонов

    @Override
    protected boolean isRandomlyTicking(final BlockState state) {
        return state.getValue(STAGE) != LilyStage.BLOOM;
    }

    @Override
    protected void randomTick(final BlockState state, final ServerLevel level, final BlockPos pos, final RandomSource random) {
        LilyBloomConfig config = LilyBloomConfig.get();
        if (!config.naturalBlooming || state.getValue(STAGE) == LilyStage.BLOOM || random.nextInt(config.growChance) != 0) {
            return;
        }
        if (!canGrowNaturally(level, pos, config)) {
            return;
        }
        advance(level, pos, state);
        if (state.getValue(STAGE).next() == LilyStage.BLOOM) {
            bloomParticles(level, pos);
        }
    }

    private static boolean canGrowNaturally(final ServerLevel level, final BlockPos pos, final LilyBloomConfig config) {
        return level.canSeeSky(pos) && (!config.onlyDuringDay || level.isBrightOutside());
    }

    /** Следующая стадия (бутон → полураскрытый → цветок). */
    public static void advance(final ServerLevel level, final BlockPos pos, final BlockState state) {
        LilyStage next = state.getValue(STAGE).next();
        level.setBlock(pos, state.setValue(STAGE, next), Block.UPDATE_ALL);
        level.gameEvent(null, GameEvent.BLOCK_CHANGE, pos);
    }

    /** Обычная кувшинка пускает бутон случайного цвета (самостоятельно или от костной муки). */
    public static void startBud(final ServerLevel level, final BlockPos pos, final RandomSource random) {
        BlockState bud = ModBlocks.BLOOMING_LILY_PADS.get(LilyColor.random(random)).defaultBlockState().setValue(STAGE, LilyStage.BUD);
        level.setBlock(pos, bud, Block.UPDATE_ALL);
        level.gameEvent(null, GameEvent.BLOCK_CHANGE, pos);
    }

    /** Сразу распустившийся цветок заданного цвета (когда цветок кувшинки возвращают на кувшинку). */
    public static void bloom(final ServerLevel level, final BlockPos pos, final LilyColor color, @Nullable final Entity source) {
        level.setBlock(pos, ModBlocks.BLOOMING_LILY_PADS.get(color).defaultBlockState(), Block.UPDATE_ALL);
        level.gameEvent(source, GameEvent.BLOCK_CHANGE, pos);
    }

    /**
     * Самостоятельное появление бутона на обычной кувшинке. Вызывается из случайного тика ванильной кувшинки
     * (см. LilyPadBlockMixin). Шанс — {@code budChance}, рядом не больше {@code maxBloomingNearby} кувшинок с бутоном или цветком.
     */
    public static void tryNaturalBud(final ServerLevel level, final BlockPos pos, final RandomSource random) {
        LilyBloomConfig config = LilyBloomConfig.get();
        if (!config.naturalBlooming || random.nextInt(config.budChance) != 0) {
            return;
        }
        if (!canGrowNaturally(level, pos, config)) {
            return;
        }
        if (config.maxBloomingNearby > 0 && countBloomingNearby(level, pos, config.maxBloomingNearby) >= config.maxBloomingNearby) {
            return;
        }
        startBud(level, pos, random);
    }

    private static int countBloomingNearby(final ServerLevel level, final BlockPos center, final int stopAt) {
        int count = 0;
        for (BlockPos p : BlockPos.betweenClosed(center.offset(-3, -1, -3), center.offset(3, 1, 3))) {
            if (level.getBlockState(p).getBlock() instanceof BloomingLilyPadBlock && ++count >= stopAt) {
                break;
            }
        }
        return count;
    }

    /** Зелёные искры, как от костной муки. */
    public static void bloomParticles(final ServerLevel level, final BlockPos pos) {
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.25, pos.getZ() + 0.5, 8, 0.3, 0.1, 0.3, 0.0);
    }

    // ---------------------------------------------------------------- ножницы

    @Override
    protected InteractionResult useItemOn(
            final ItemStack itemStack,
            final BlockState state,
            final Level level,
            final BlockPos pos,
            final Player player,
            final InteractionHand hand,
            final BlockHitResult hitResult) {
        if (!isShears(itemStack) || state.getValue(STAGE) != LilyStage.BLOOM) {
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
     * Срезать цветок: выпадает цветок кувшинки этого цвета (по лут-таблице
     * {@code lilybloom:shearing/<id блока>}, её можно менять датапаком), на месте остаётся обычная кувшинка.
     * Используется и игроком, и раздатчиком с ножницами.
     */
    public static void shear(final ServerLevel level, final BlockPos pos, final BlockState state, final ItemStack tool, @Nullable final Entity source) {
        Identifier blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        ResourceKey<LootTable> loot = ResourceKey.create(Registries.LOOT_TABLE, LilyBloom.id("shearing/" + blockId.getPath()));
        level.playSound(null, pos, SoundEvents.GROWING_PLANT_CROP, SoundSource.BLOCKS, 1.0F, 1.0F);
        dropFromBlockInteractLootTable(level, loot, pos, state, null, tool, source,
                (serverLevel, drop) -> popResourceFromFace(serverLevel, pos, Direction.UP, drop));
        level.setBlock(pos, Blocks.LILY_PAD.defaultBlockState(), Block.UPDATE_ALL);
        level.gameEvent(source, GameEvent.SHEAR, pos);
    }
}
