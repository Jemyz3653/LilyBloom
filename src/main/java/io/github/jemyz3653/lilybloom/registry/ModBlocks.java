package io.github.jemyz3653.lilybloom.registry;

import io.github.jemyz3653.lilybloom.LilyBloom;
import io.github.jemyz3653.lilybloom.LilyColor;
import io.github.jemyz3653.lilybloom.block.BloomingLilyPadBlock;
import io.github.jemyz3653.lilybloom.block.BigLilyPadBlock;
import io.github.jemyz3653.lilybloom.block.GiantLilyPadBlock;
import io.github.jemyz3653.lilybloom.block.LargeLilyPadBlock;
import io.github.jemyz3653.lilybloom.block.LilyFlowerBlock;
import io.github.jemyz3653.lilybloom.block.MediumLilyPadBlock;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

/**
 * Блоки по цветам. Розовые сохраняют ID из 1.0.x: blooming_lily_pad, lily_flower, potted_lily_flower.
 * Остальные: white_blooming_lily_pad, white_lily_flower, potted_white_lily_flower и т. д.
 */
public final class ModBlocks {
    /** Цветущая кувшинка каждого цвета — отдельный блок со стадиями бутон / полураскрытый / цветок. */
    public static final Map<LilyColor, Block> BLOOMING_LILY_PADS;
    /** Цветок кувшинки каждого цвета — ставится на землю как обычный цветок. */
    public static final Map<LilyColor, Block> LILY_FLOWERS;
    /** Цветок кувшинки в горшке. Конструктор FlowerPotBlock сам связывает цветок с горшком. */
    public static final Map<LilyColor, Block> POTTED_LILY_FLOWERS;

    static {
        Map<LilyColor, Block> blooming = new EnumMap<>(LilyColor.class);
        Map<LilyColor, Block> flowers = new EnumMap<>(LilyColor.class);
        Map<LilyColor, Block> potted = new EnumMap<>(LilyColor.class);
        for (LilyColor color : LilyColor.values()) {
            blooming.put(color, register(color.id("blooming_lily_pad"),
                    properties -> new BloomingLilyPadBlock(color, properties),
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.PLANT)
                            .instabreak()
                            .sound(SoundType.LILY_PAD)
                            .noOcclusion()
                            .pushReaction(PushReaction.POPPED)));
            Block flower = register(color.id("lily_flower"),
                    properties -> new LilyFlowerBlock(MobEffects.WATER_BREATHING, 8.0F, properties),
                    BlockBehaviour.Properties.of()
                            .mapColor(flowerMapColor(color))
                            .noCollision()
                            .instabreak()
                            .sound(SoundType.GRASS)
                            .offsetType(BlockBehaviour.OffsetType.XZ)
                            .pushReaction(PushReaction.POPPED));
            flowers.put(color, flower);
            potted.put(color, register(color.pottedId(),
                    properties -> new FlowerPotBlock(flower, properties),
                    BlockBehaviour.Properties.of()
                            .instabreak()
                            .noOcclusion()
                            .pushReaction(PushReaction.POPPED)));
        }
        BLOOMING_LILY_PADS = Collections.unmodifiableMap(blooming);
        LILY_FLOWERS = Collections.unmodifiableMap(flowers);
        POTTED_LILY_FLOWERS = Collections.unmodifiableMap(potted);
    }

    /** Розовые — для кода, написанного под 1.0.x. */
    public static final Block BLOOMING_LILY_PAD = BLOOMING_LILY_PADS.get(LilyColor.PINK);
    public static final Block LILY_FLOWER = LILY_FLOWERS.get(LilyColor.PINK);
    public static final Block POTTED_LILY_FLOWER = POTTED_LILY_FLOWERS.get(LilyColor.PINK);

    /** Средняя кувшинка 2×2 (4 одинаковых блока с координатами части внутри листа). */
    public static final Block MEDIUM_LILY_PAD = register("medium_lily_pad", MediumLilyPadBlock::new, bigPadProperties());
    /** Большая кувшинка 3×3. */
    public static final Block LARGE_LILY_PAD = register("large_lily_pad", LargeLilyPadBlock::new, bigPadProperties());
    /** Огромная кувшинка 4×4 (id с 1.2.0, когда она была единственной крупной). */
    public static final Block GIANT_LILY_PAD = register("giant_lily_pad", GiantLilyPadBlock::new, bigPadProperties());

    private static BlockBehaviour.Properties bigPadProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.PLANT)
                .instabreak()
                .sound(SoundType.LILY_PAD)
                .noOcclusion()
                .pushReaction(PushReaction.POPPED);
    }

    /** Крупная кувшинка со стороной size (2, 3 или 4). */
    public static BigLilyPadBlock bigLilyPad(final int size) {
        return (BigLilyPadBlock) switch (size) {
            case MediumLilyPadBlock.SIZE -> MEDIUM_LILY_PAD;
            case LargeLilyPadBlock.SIZE -> LARGE_LILY_PAD;
            case GiantLilyPadBlock.SIZE -> GIANT_LILY_PAD;
            default -> throw new IllegalArgumentException("no lily pad of size " + size);
        };
    }

    private ModBlocks() {
    }

    private static MapColor flowerMapColor(final LilyColor color) {
        return switch (color) {
            case PINK -> MapColor.COLOR_PINK;
            case WHITE -> MapColor.SNOW;
            case YELLOW -> MapColor.COLOR_YELLOW;
            case BLUE -> MapColor.COLOR_LIGHT_BLUE;
        };
    }

    private static Block register(final String name, final Function<BlockBehaviour.Properties, Block> factory, final BlockBehaviour.Properties properties) {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, LilyBloom.id(name));
        Block block = factory.apply(properties.setId(key));
        return Registry.register(BuiltInRegistries.BLOCK, key, block);
    }

    public static void init() {
        // загрузка класса регистрирует блоки
    }
}
