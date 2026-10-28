package io.github.jemyz3653.lilybloom.block;

import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * Средняя кувшинка 2×2 блока. Вся логика — в {@link BigLilyPadBlock}; якорь — блок (0, 0),
 * цветок стоит в его углу, то есть ровно в центре листа.
 */
public class MediumLilyPadBlock extends BigLilyPadBlock {
    public static final int SIZE = 2;

    public static final IntegerProperty PART_X = partProperty("part_x", SIZE);
    public static final IntegerProperty PART_Z = partProperty("part_z", SIZE);

    public MediumLilyPadBlock(final Properties properties) {
        super(SIZE, "medium_lily_pad", properties);
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
