package io.github.jemyz3653.lilybloom.block;

import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * Большая кувшинка 3×3 блока. Вся логика — в {@link BigLilyPadBlock}; якорь — средний блок (1, 1),
 * цветок стоит в его середине.
 */
public class LargeLilyPadBlock extends BigLilyPadBlock {
    public static final int SIZE = 3;

    public static final IntegerProperty PART_X = partProperty("part_x", SIZE);
    public static final IntegerProperty PART_Z = partProperty("part_z", SIZE);

    public LargeLilyPadBlock(final Properties properties) {
        super(SIZE, "large_lily_pad", properties);
    }

    @Override
    public IntegerProperty partX() {
        return PART_X;
    }

    @Override
    public IntegerProperty partZ() {
        return PART_Z;
    }
}
