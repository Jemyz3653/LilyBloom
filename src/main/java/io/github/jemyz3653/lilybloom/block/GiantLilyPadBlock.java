package io.github.jemyz3653.lilybloom.block;

import io.github.jemyz3653.lilybloom.LilyBloom;
import io.github.jemyz3653.lilybloom.LilyBloomConfig;
import io.github.jemyz3653.lilybloom.LilyColor;
import io.github.jemyz3653.lilybloom.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LilyPadBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * Крупная кувшинка 4×4 блока. Каждый из 16 блоков — эта же кувшинка со своими координатами внутри листа
 * ({@link #PART_X}, {@link #PART_Z}); модель каждого блока рисует свою четверть-восьмушку большой текстуры.
 *
 * <p>Цветок ({@link #FLOWER} + {@link #STAGE}) хранится во всех 16 блоках одинаково (так его видит лут любого
 * блока), а рисуется и растёт только в «якоре» — блоке (1, 1): его модель выходит за границы блока и стоит
 * ровно в центре листа.
 *
 * <p>Сломался любой блок (игроком, лодкой, взрывом, ушла вода) — исчезает весь лист, а предмет выпадает один раз:
 * из того блока, который сломали; остальные убираются без дропа.
 */
public class GiantLilyPadBlock extends LilyPadBlock {
    public static final int SIZE = 4;
    public static final int ANCHOR = 1;

    public static final IntegerProperty PART_X = IntegerProperty.create("part_x", 0, SIZE - 1);
    public static final IntegerProperty PART_Z = IntegerProperty.create("part_z", 0, SIZE - 1);
    public static final EnumProperty<GiantFlower> FLOWER = EnumProperty.create("flower", GiantFlower.class);
    public static final EnumProperty<LilyStage> STAGE = BloomingLilyPadBlock.STAGE;

    private static final ResourceKey<LootTable> SHEARING_LOOT =
            ResourceKey.create(Registries.LOOT_TABLE, LilyBloom.id("shearing/giant_lily_pad"));

    public GiantLilyPadBlock(final Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(PART_X, 0).setValue(PART_Z, 0)
                .setValue(FLOWER, GiantFlower.NONE).setValue(STAGE, LilyStage.BLOOM));
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(PART_X, PART_Z, FLOWER, STAGE);
    }

    // ---------------------------------------------------------------- геометрия листа

    public static BlockPos origin(final BlockPos pos, final BlockState state) {
        return pos.offset(-state.getValue(PART_X), 0, -state.getValue(PART_Z));
    }

    public static BlockPos anchor(final BlockPos pos, final BlockState state) {
        return origin(pos, state).offset(ANCHOR, 0, ANCHOR);
    }

    public static boolean isAnchor(final BlockState state) {
        return state.getValue(PART_X) == ANCHOR && state.getValue(PART_Z) == ANCHOR;
    }

    /** Блок на месте p — часть именно этого листа (наш блок с правильными координатами). */
    private static boolean isPartOf(final BlockState state, final BlockPos p, final BlockPos origin) {
        return state.getBlock() instanceof GiantLilyPadBlock
                && state.getValue(PART_X) == p.getX() - origin.getX()
                && state.getValue(PART_Z) == p.getZ() - origin.getZ();
    }

    /** Состояние части (x, z) листа с данным цветком. */
    public BlockState part(final int x, final int z, final GiantFlower flower, final LilyStage stage) {
        return defaultBlockState().setValue(PART_X, x).setValue(PART_Z, z).setValue(FLOWER, flower).setValue(STAGE, stage);
    }

    /** Можно ли положить лист с углом в origin: везде над водой пусто и лист выживет. */
    public boolean canPlaceAt(final LevelReader level, final BlockPos origin) {
        for (int x = 0; x < SIZE; x++) {
            for (int z = 0; z < SIZE; z++) {
                BlockPos p = origin.offset(x, 0, z);
                if (!level.getBlockState(p).isAir() || !part(x, z, GiantFlower.NONE, LilyStage.BLOOM).canSurvive(level, p)) {
                    return false;
                }
            }
        }
        return true;
    }

    /** Ставит все 16 частей. */
    public void placeAt(final LevelAccessor level, final BlockPos origin, final GiantFlower flower, final LilyStage stage, final int flags) {
        for (int x = 0; x < SIZE; x++) {
            for (int z = 0; z < SIZE; z++) {
                level.setBlock(origin.offset(x, 0, z), part(x, z, flower, stage), flags);
            }
        }
    }

    /** Меняет цветок во всех частях листа. */
    public static void setFlower(final Level level, final BlockPos anyPart, final GiantFlower flower, final LilyStage stage) {
        BlockState state = level.getBlockState(anyPart);
        if (!(state.getBlock() instanceof GiantLilyPadBlock)) {
            return;
        }
        BlockPos origin = origin(anyPart, state);
        for (int x = 0; x < SIZE; x++) {
            for (int z = 0; z < SIZE; z++) {
                BlockPos p = origin.offset(x, 0, z);
                BlockState s = level.getBlockState(p);
                if (isPartOf(s, p, origin)) {
                    level.setBlock(p, s.setValue(FLOWER, flower).setValue(STAGE, stage), Block.UPDATE_CLIENTS);
                }
            }
        }
        level.gameEvent(null, GameEvent.BLOCK_CHANGE, anchor(anyPart, state));
    }

    /** Цветок по костной муке: нет цветка → бутон случайного цвета, бутон → полураскрытый → цветок. false — расти некуда. */
    public static boolean grow(final ServerLevel level, final BlockPos anyPart, final BlockState state, final RandomSource random) {
        GiantFlower flower = state.getValue(FLOWER);
        if (flower == GiantFlower.NONE) {
            setFlower(level, anyPart, GiantFlower.of(LilyColor.random(random)), LilyStage.BUD);
        } else if (state.getValue(STAGE) != LilyStage.BLOOM) {
            setFlower(level, anyPart, flower, state.getValue(STAGE).next());
        } else {
            return false;
        }
        particles(level, anchor(anyPart, state));
        return true;
    }

    public static boolean isFullBloom(final BlockState state) {
        return state.getBlock() instanceof GiantLilyPadBlock
                && state.getValue(FLOWER) != GiantFlower.NONE && state.getValue(STAGE) == LilyStage.BLOOM;
    }

    /** Искры над центром листа (угол якорного блока). */
    private static void particles(final ServerLevel level, final BlockPos anchor) {
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, anchor.getX() + 1.0, anchor.getY() + 0.4, anchor.getZ() + 1.0,
                14, 0.6, 0.2, 0.6, 0.0);
    }

    // ---------------------------------------------------------------- разрушение целиком

    @Override
    protected void affectNeighborsAfterRemoval(final BlockState state, final ServerLevel level, final BlockPos pos, final boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        BlockPos origin = origin(pos, state);
        for (int x = 0; x < SIZE; x++) {
            for (int z = 0; z < SIZE; z++) {
                BlockPos p = origin.offset(x, 0, z);
                if (!p.equals(pos) && isPartOf(level.getBlockState(p), p, origin)) {
                    // без дропа: предмет уже выпал из того блока, который сломали
                    level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                }
            }
        }
    }

    // ---------------------------------------------------------------- самостоятельный рост цветка

    @Override
    protected boolean isRandomlyTicking(final BlockState state) {
        return isAnchor(state) && (state.getValue(FLOWER) == GiantFlower.NONE || state.getValue(STAGE) != LilyStage.BLOOM);
    }

    @Override
    protected void randomTick(final BlockState state, final ServerLevel level, final BlockPos pos, final RandomSource random) {
        LilyBloomConfig config = LilyBloomConfig.get();
        if (!config.naturalBlooming || !isAnchor(state)) {
            return;
        }
        boolean bud = state.getValue(FLOWER) == GiantFlower.NONE;
        if (random.nextInt(bud ? config.budChance : config.growChance) != 0) {
            return;
        }
        if (!level.canSeeSky(pos) || (config.onlyDuringDay && !level.isBrightOutside())) {
            return;
        }
        grow(level, pos, state, random);
    }

    // ---------------------------------------------------------------- цветок и ножницы

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
        if (color != null && state.getValue(FLOWER) == GiantFlower.NONE) {
            if (level instanceof ServerLevel) {
                Item flower = itemStack.getItem();
                setFlower(level, pos, GiantFlower.of(color), LilyStage.BLOOM);
                level.playSound(null, anchor(pos, state), SoundEvents.LILY_PAD_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
                itemStack.consume(1, player);
                player.awardStat(Stats.ITEM_USED.get(flower));
            }
            return InteractionResult.SUCCESS;
        }
        if (BloomingLilyPadBlock.isShears(itemStack) && isFullBloom(state)) {
            if (level instanceof ServerLevel serverLevel) {
                Item shears = itemStack.getItem();
                shear(serverLevel, pos, state, itemStack, player);
                itemStack.hurtAndBreak(1, player, hand.asEquipmentSlot());
                player.awardStat(Stats.ITEM_USED.get(shears));
            }
            return InteractionResult.SUCCESS;
        }
        return super.useItemOn(itemStack, state, level, pos, player, hand, hitResult);
    }

    /** Срезать цветок: два цветка кувшинки его цвета (лут-таблица lilybloom:shearing/giant_lily_pad), лист остаётся. */
    public static void shear(final ServerLevel level, final BlockPos anyPart, final BlockState state, final ItemStack tool, @Nullable final Entity source) {
        BlockPos anchor = anchor(anyPart, state);
        level.playSound(null, anchor, SoundEvents.GROWING_PLANT_CROP, SoundSource.BLOCKS, 1.0F, 0.9F);
        dropFromBlockInteractLootTable(level, SHEARING_LOOT, anchor, state, null, tool, source,
                (serverLevel, drop) -> popResourceFromFace(serverLevel, anchor, Direction.UP, drop));
        setFlower(level, anyPart, GiantFlower.NONE, LilyStage.BLOOM);
        level.gameEvent(source, GameEvent.SHEAR, anchor);
    }
}
