package io.github.jemyz3653.lilybloom.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.jemyz3653.lilybloom.LilyColor;
import io.github.jemyz3653.lilybloom.block.BigLilyPadBlock;
import io.github.jemyz3653.lilybloom.block.GiantFlower;
import io.github.jemyz3653.lilybloom.block.LilyStage;
import io.github.jemyz3653.lilybloom.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * Генерация крупной кувшинки (тип lilybloom:giant_lily_pad, общий для всех размеров): точка из placed feature —
 * воздух над поверхностью воды; лист size×size кладётся так, чтобы эта точка была одним из центральных блоков,
 * и только если под всеми его блоками стоячая вода.
 *
 * @param flowerChance шанс, что на листе уже есть цветок (случайного цвета и стадии)
 * @param size         сторона листа: 2 (средняя), 3 (большая) или 4 (огромная, по умолчанию — как в 1.2.x)
 */
public record GiantLilyPadFeature(float flowerChance, int size) implements Feature {
    public static final MapCodec<GiantLilyPadFeature> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.floatRange(0.0F, 1.0F).optionalFieldOf("flower_chance", 0.35F).forGetter(GiantLilyPadFeature::flowerChance),
            Codec.intRange(2, 4).optionalFieldOf("size", 4).forGetter(GiantLilyPadFeature::size)
    ).apply(instance, GiantLilyPadFeature::new));

    @Override
    public MapCodec<GiantLilyPadFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(final WorldGenLevel level, final ChunkGenerator generator, final RandomSource random, final BlockPos pos) {
        BigLilyPadBlock pad = ModBlocks.bigLilyPad(size);
        // точка — один из центральных блоков листа: у 4×4 это 1 или 2, у 3×3 — 1, у 2×2 — 0 или 1
        int lo = (size - 1) / 2;
        int spread = size / 2 - lo + 1;
        BlockPos origin = pos.offset(-lo - random.nextInt(spread), 0, -lo - random.nextInt(spread));
        if (!pad.canPlaceAt(level, origin)) {
            return false;
        }
        GiantFlower flower = GiantFlower.NONE;
        LilyStage stage = LilyStage.BLOOM;
        if (random.nextFloat() < flowerChance) {
            flower = GiantFlower.of(LilyColor.random(random));
            int r = random.nextInt(5);          // как у обычных: чаще уже распустился
            stage = r == 0 ? LilyStage.BUD : r == 1 ? LilyStage.HALF_OPEN : LilyStage.BLOOM;
        }
        pad.placeAt(level, origin, flower, stage, Block.UPDATE_CLIENTS);
        return true;
    }
}
