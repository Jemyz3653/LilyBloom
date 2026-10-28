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
 * Большая кувшинка N×N блоков: средняя 2×2 ({@link MediumLilyPadBlock}), большая 3×3 ({@link LargeLilyPadBlock})
 * и огромная 4×4 ({@link GiantLilyPadBlock}). Каждый из N×N блоков — эта же кувшинка со своими координатами внутри
 * листа ({@link #partX()}, {@link #partZ()}); модель каждого блока рисует свой кусок большой текстуры листа.
 *
 * <p>Цветок ({@link #FLOWER} + {@link #STAGE}) хранится во всех блоках листа одинаково (так его видит лут любого
 * блока), а рисуется и растёт только в «якоре» — блоке ({@link #anchor()}, {@link #anchor()}): его модель стоит
 * ровно в центре листа (у 2×2 и 4×4 центр — угол якоря, у 3×3 — середина якоря).
 *
 * <p>Сломался любой блок (игроком, лодкой, взрывом, ушла вода) — исчезает весь лист, а предмет выпадает один раз:
 * из того блока, который сломали; остальные убираются без дропа.
 */
public abstract class BigLilyPadBlock extends LilyPadBlock {
    public static final EnumProperty<GiantFlower> FLOWER = EnumProperty.create("flower", GiantFlower.class);
    public static final EnumProperty<LilyStage> STAGE = BloomingLilyPadBlock.STAGE;

    private final int size;
    private final ResourceKey<LootTable> shearingLoot;

    /**
     * @param size сторона листа в блоках
     * @param name id блока: лут ножниц — lilybloom:shearing/&lt;name&gt;
     */
    protected BigLilyPadBlock(final int size, final String name, final Properties properties) {
        super(properties);
        this.size = size;
        this.shearingLoot = ResourceKey.create(Registries.LOOT_TABLE, LilyBloom.id("shearing/" + name));
        registerDefaultState(stateDefinition.any()
                .setValue(partX(), 0).setValue(partZ(), 0)
                .setValue(FLOWER, GiantFlower.NONE).setValue(STAGE, LilyStage.BLOOM));
    }

    /** Координата части по x, 0..size-1 (у каждого размера своё свойство с нужным диапазоном). */
    public abstract IntegerProperty partX();

    public abstract IntegerProperty partZ();

    /** Свойство part_x/part_z на 0..size-1. */
    protected static IntegerProperty partProperty(final String name, final int size) {
        return IntegerProperty.create(name, 0, size - 1);
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(partX(), partZ(), FLOWER, STAGE);
    }

    // ---------------------------------------------------------------- геометрия листа

    public int size() {
        return size;
    }

    /** Координата якоря внутри листа (по x и по z): 2×2 → 0, 3×3 → 1, 4×4 → 1. */
    public int anchor() {
        return (size - 1) / 2;
    }

    private static BigLilyPadBlock pad(final BlockState state) {
        return (BigLilyPadBlock) state.getBlock();
    }

    public static BlockPos origin(final BlockPos pos, final BlockState state) {
        BigLilyPadBlock pad = pad(state);
        return pos.offset(-state.getValue(pad.partX()), 0, -state.getValue(pad.partZ()));
    }

    public static BlockPos anchor(final BlockPos pos, final BlockState state) {
        int a = pad(state).anchor();
        return origin(pos, state).offset(a, 0, a);
    }

    public static boolean isAnchor(final BlockState state) {
        BigLilyPadBlock pad = pad(state);
        return state.getValue(pad.partX()) == pad.anchor() && state.getValue(pad.partZ()) == pad.anchor();
    }

    /** Блок на месте p — часть именно этого листа (тот же размер, правильные координаты). */
    private boolean isPartOf(final BlockState state, final BlockPos p, final BlockPos origin) {
        return state.getBlock() == this
                && state.getValue(partX()) == p.getX() - origin.getX()
                && state.getValue(partZ()) == p.getZ() - origin.getZ();
    }

    /** Состояние части (x, z) листа с данным цветком. */
    public BlockState part(final int x, final int z, final GiantFlower flower, final LilyStage stage) {
        return defaultBlockState().setValue(partX(), x).setValue(partZ(), z).setValue(FLOWER, flower).setValue(STAGE, stage);
    }

    /** Можно ли положить лист с углом в origin: везде над водой пусто и лист выживет. */
    public boolean canPlaceAt(final LevelReader level, final BlockPos origin) {
        for (int x = 0; x < size; x++) {
            for (int z = 0; z < size; z++) {
                BlockPos p = origin.offset(x, 0, z);
                if (!level.getBlockState(p).isAir() || !part(x, z, GiantFlower.NONE, LilyStage.BLOOM).canSurvive(level, p)) {
                    return false;
                }
            }
        }
        return true;
    }

    /** Ставит все части листа. */
    public void placeAt(final LevelAccessor level, final BlockPos origin, final GiantFlower flower, final LilyStage stage, final int flags) {
        for (int x = 0; x < size; x++) {
            for (int z = 0; z < size; z++) {
                level.setBlock(origin.offset(x, 0, z), part(x, z, flower, stage), flags);
            }
        }
    }

    /** Середина листа (для звуков и частиц). */
    public static double centerX(final BlockPos anyPart, final BlockState state) {
        return origin(anyPart, state).getX() + pad(state).size / 2.0;
    }

    public static double centerZ(final BlockPos anyPart, final BlockState state) {
        return origin(anyPart, state).getZ() + pad(state).size / 2.0;
    }

    /** Меняет цветок во всех частях листа. */
    public static void setFlower(final Level level, final BlockPos anyPart, final GiantFlower flower, final LilyStage stage) {
        BlockState state = level.getBlockState(anyPart);
        if (!(state.getBlock() instanceof BigLilyPadBlock pad)) {
            return;
        }
        BlockPos origin = origin(anyPart, state);
        for (int x = 0; x < pad.size; x++) {
            for (int z = 0; z < pad.size; z++) {
                BlockPos p = origin.offset(x, 0, z);
                BlockState s = level.getBlockState(p);
                if (pad.isPartOf(s, p, origin)) {
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
        particles(level, anyPart, state);
        return true;
    }

    /** Может ли костная мука что-то сделать (нет цветка или он ещё не распустился). */
    public static boolean canGrow(final BlockState state) {
        return state.getValue(FLOWER) == GiantFlower.NONE || state.getValue(STAGE) != LilyStage.BLOOM;
    }

    public static boolean isFullBloom(final BlockState state) {
        return state.getBlock() instanceof BigLilyPadBlock
                && state.getValue(FLOWER) != GiantFlower.NONE && state.getValue(STAGE) == LilyStage.BLOOM;
    }

    /** Искры над центром листа. */
    private static void particles(final ServerLevel level, final BlockPos anyPart, final BlockState state) {
        double spread = pad(state).size * 0.15;
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, centerX(anyPart, state), anyPart.getY() + 0.4, centerZ(anyPart, state),
                4 + pad(state).size * 3, spread, 0.2, spread, 0.0);
    }

    // ---------------------------------------------------------------- разрушение целиком

    @Override
    protected void affectNeighborsAfterRemoval(final BlockState state, final ServerLevel level, final BlockPos pos, final boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        BlockPos origin = origin(pos, state);
        for (int x = 0; x < size; x++) {
            for (int z = 0; z < size; z++) {
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
        return isAnchor(state) && canGrow(state);
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

    /** Срезать цветок: цветы кувшинки его цвета (лут-таблица lilybloom:shearing/&lt;размер&gt;), лист остаётся. */
    public static void shear(final ServerLevel level, final BlockPos anyPart, final BlockState state, final ItemStack tool, @Nullable final Entity source) {
        BlockPos anchor = anchor(anyPart, state);
        level.playSound(null, anchor, SoundEvents.GROWING_PLANT_CROP, SoundSource.BLOCKS, 1.0F, 0.9F);
        dropFromBlockInteractLootTable(level, pad(state).shearingLoot, anchor, state, null, tool, source,
                (serverLevel, drop) -> popResourceFromFace(serverLevel, anchor, Direction.UP, drop));
        setFlower(level, anyPart, GiantFlower.NONE, LilyStage.BLOOM);
        level.gameEvent(source, GameEvent.SHEAR, anchor);
    }
}
