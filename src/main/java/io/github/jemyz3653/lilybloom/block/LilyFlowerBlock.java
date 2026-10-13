package io.github.jemyz3653.lilybloom.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Цветок кувшинки: обычный цветок (ставится на траву/землю, сажается в горшок, годится для
 * подозрительного рагу и красителя), только рамка выделения под объёмную головку цветка.
 */
public class LilyFlowerBlock extends FlowerBlock {
    private static final VoxelShape SHAPE = Block.column(10.0, 0.0, 12.0);

    public LilyFlowerBlock(final Holder<MobEffect> suspiciousStewEffect, final float effectSeconds, final Properties properties) {
        super(suspiciousStewEffect, effectSeconds, properties);
    }

    @Override
    protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
        return SHAPE.move(state.getOffset(pos));
    }
}
