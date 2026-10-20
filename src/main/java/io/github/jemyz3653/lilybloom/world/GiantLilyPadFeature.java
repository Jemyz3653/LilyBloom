package io.github.jemyz3653.lilybloom.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.jemyz3653.lilybloom.LilyColor;
import io.github.jemyz3653.lilybloom.block.GiantFlower;
import io.github.jemyz3653.lilybloom.block.GiantLilyPadBlock;
import io.github.jemyz3653.lilybloom.block.LilyStage;
import io.github.jemyz3653.lilybloom.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * Генерация крупной кувшинки: точка из placed feature — воздух над поверхностью воды; лист 4×4 кладётся так,
 * чтобы эта точка была одним из центральных блоков, и только если под всеми 16 блоками стоячая вода.
 *
 * @param flowerChance шанс, что на листе уже есть цветок (случайного цвета и стадии)
 */
public record GiantLilyPadFeature(float flowerChance) implements Feature {
    public static final MapCodec<GiantLilyPadFeature> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.floatRange(0.0F, 1.0F).optionalFieldOf("flower_chance", 0.35F).forGetter(GiantLilyPadFeature::flowerChance)
    ).apply(instance, GiantLilyPadFeature::new));

    @Override
    public MapCodec<GiantLilyPadFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(final WorldGenLevel level, final ChunkGenerator generator, final RandomSource random, final BlockPos pos) {
        GiantLilyPadBlock giant = (GiantLilyPadBlock) ModBlocks.GIANT_LILY_PAD;
        BlockPos origin = pos.offset(-1 - random.nextInt(2), 0, -1 - random.nextInt(2));
        if (!giant.canPlaceAt(level, origin)) {
            return false;
        }
        GiantFlower flower = GiantFlower.NONE;
        LilyStage stage = LilyStage.BLOOM;
        if (random.nextFloat() < flowerChance) {
            flower = GiantFlower.of(LilyColor.random(random));
            int r = random.nextInt(5);          // как у обычных: чаще уже распустился
            stage = r == 0 ? LilyStage.BUD : r == 1 ? LilyStage.HALF_OPEN : LilyStage.BLOOM;
        }
        giant.placeAt(level, origin, flower, stage, Block.UPDATE_CLIENTS);
        return true;
    }
}
