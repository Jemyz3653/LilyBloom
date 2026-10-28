package io.github.jemyz3653.lilybloom.item;

import io.github.jemyz3653.lilybloom.block.BigLilyPadBlock;
import io.github.jemyz3653.lilybloom.block.GiantFlower;
import io.github.jemyz3653.lilybloom.block.LilyStage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.PlaceOnWaterBlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jspecify.annotations.Nullable;

/**
 * Средняя (2×2), большая (3×3) и огромная (4×4) кувшинка: ставится на воду, как обычная (ПКМ по воде), и занимает
 * N×N блоков. Лист кладётся так, чтобы блок, на который смотрит игрок, был у ближнего к нему края и лист уходил
 * вперёд; если там не помещается — пробуются остальные положения, в которые этот блок входит.
 */
public class BigLilyPadItem extends PlaceOnWaterBlockItem {
    public BigLilyPadItem(final Block block, final Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult place(final BlockPlaceContext context) {
        if (!(getBlock() instanceof BigLilyPadBlock pad)) {
            return InteractionResult.FAIL;
        }
        Level level = context.getLevel();
        Player player = context.getPlayer();
        BlockPos clicked = context.getClickedPos();
        BlockPos origin = findOrigin(pad, level, clicked, context.getHorizontalDirection(), player);
        if (origin == null) {
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide()) {
            pad.placeAt(level, origin, GiantFlower.NONE, LilyStage.BLOOM, Block.UPDATE_ALL);
            BlockState placed = level.getBlockState(clicked);
            SoundType sound = placed.getSoundType();
            Vec3 center = new Vec3(origin.getX() + pad.size() / 2.0, origin.getY(), origin.getZ() + pad.size() / 2.0);
            level.playSound(null, center.x, center.y, center.z, sound.getPlaceSound(), SoundSource.BLOCKS,
                    (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * (1.0F - 0.075F * pad.size()));
            level.gameEvent(GameEvent.BLOCK_PLACE, center, GameEvent.Context.of(player, placed));
            if (player instanceof ServerPlayer serverPlayer) {
                CriteriaTriggers.PLACED_BLOCK.trigger(serverPlayer, clicked, context.getItemInHand());
            }
        }
        context.getItemInHand().consume(1, player);
        return InteractionResult.SUCCESS;
    }

    /** Угол листа (часть 0, 0), в который входит clicked, или null, если лист нигде не помещается. */
    static @Nullable BlockPos findOrigin(final BigLilyPadBlock pad, final Level level, final BlockPos clicked,
                                         final Direction facing, @Nullable final Player player) {
        int size = pad.size();
        // центр листа хотим на (size - 1) / 2 блока впереди от выбранного блока, по бокам — посередине
        double ahead = (size - 1) / 2.0;
        double tx = clicked.getX() + 0.5 + facing.getStepX() * ahead;
        double tz = clicked.getZ() + 0.5 + facing.getStepZ() * ahead;
        List<BlockPos> candidates = new ArrayList<>();
        for (int ix = 0; ix < size; ix++) {
            for (int iz = 0; iz < size; iz++) {
                candidates.add(clicked.offset(-ix, 0, -iz));
            }
        }
        candidates.sort(Comparator.comparingDouble(o -> {
            double cx = o.getX() + size / 2.0 - tx;
            double cz = o.getZ() + size / 2.0 - tz;
            return cx * cx + cz * cz;
        }));
        for (BlockPos origin : candidates) {
            if (fits(pad, level, origin, player)) {
                return origin;
            }
        }
        return null;
    }

    private static boolean fits(final BigLilyPadBlock pad, final Level level, final BlockPos origin, @Nullable final Player player) {
        if (!pad.canPlaceAt(level, origin)) {
            return false;
        }
        CollisionContext collision = player == null ? CollisionContext.empty() : CollisionContext.of(player);
        for (int x = 0; x < pad.size(); x++) {
            for (int z = 0; z < pad.size(); z++) {
                BlockPos p = origin.offset(x, 0, z);
                if (!level.getWorldBorder().isWithinBounds(p)
                        || (player != null && !level.mayInteract(player, p))
                        || !level.isUnobstructed(pad.part(x, z, GiantFlower.NONE, LilyStage.BLOOM), p, collision)) {
                    return false;
                }
            }
        }
        return true;
    }
}
