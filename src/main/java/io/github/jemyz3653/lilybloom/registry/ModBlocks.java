package io.github.jemyz3653.lilybloom.registry;

import io.github.jemyz3653.lilybloom.LilyBloom;
import io.github.jemyz3653.lilybloom.block.BloomingLilyPadBlock;
import io.github.jemyz3653.lilybloom.block.LilyFlowerBlock;
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

public final class ModBlocks {
    /** Цветущая кувшинка — отдельный блок (не вариант/blockstate обычной кувшинки). Свойства как у ванильной кувшинки. */
    public static final Block BLOOMING_LILY_PAD = register(
            "blooming_lily_pad",
            BloomingLilyPadBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .instabreak()
                    .sound(SoundType.LILY_PAD)
                    .noOcclusion()
                    .pushReaction(PushReaction.POPPED));

    /** Цветок кувшинки — ставится на землю как обычный цветок (трава, земля, мох, грядка...). */
    public static final Block LILY_FLOWER = register(
            "lily_flower",
            properties -> new LilyFlowerBlock(MobEffects.WATER_BREATHING, 8.0F, properties),
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PINK)
                    .noCollision()
                    .instabreak()
                    .sound(SoundType.GRASS)
                    .offsetType(BlockBehaviour.OffsetType.XZ)
                    .pushReaction(PushReaction.POPPED));

    /**
     * Цветок кувшинки в горшке. Конструктор FlowerPotBlock сам записывает связку «цветок → горшок»,
     * поэтому цветок сажается в обычный цветочный горшок ПКМ, как любой ванильный цветок.
     */
    public static final Block POTTED_LILY_FLOWER = register(
            "potted_lily_flower",
            properties -> new FlowerPotBlock(LILY_FLOWER, properties),
            BlockBehaviour.Properties.of()
                    .instabreak()
                    .noOcclusion()
                    .pushReaction(PushReaction.POPPED));

    private ModBlocks() {
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
