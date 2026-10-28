package io.github.jemyz3653.lilybloom.block;

import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * Огромная кувшинка 4×4 блока (id giant_lily_pad остался с 1.2.0). Вся логика — в {@link BigLilyPadBlock};
 * якорь — блок (1, 1), цветок стоит в его углу, то есть ровно в центре листа.
 */
public class GiantLilyPadBlock extends BigLilyPadBlock {
    public static final int SIZE = 4;
    public static final int ANCHOR = 1;

    public static final IntegerProperty PART_X = partProperty("part_x", SIZE);
    public static final IntegerProperty PART_Z = partProperty("part_z", SIZE);

    public GiantLilyPadBlock(final Properties properties) {
        super(SIZE, "giant_lily_pad", properties);
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
