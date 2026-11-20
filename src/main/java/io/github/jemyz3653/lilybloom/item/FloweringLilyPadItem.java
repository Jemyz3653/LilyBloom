package io.github.jemyz3653.lilybloom.item;
import io.github.jemyz3653.lilybloom.LilyTraits;
import net.minecraft.world.item.PlaceOnWaterBlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.Block;
public final class FloweringLilyPadItem extends PlaceOnWaterBlockItem {
 public FloweringLilyPadItem(Block block,Properties properties){super(block,properties);}
 @Override public InteractionResult place(BlockPlaceContext context){
  var result=super.place(context);var level=context.getLevel();var pos=context.getClickedPos();var state=level.getBlockState(pos);
  if(LilyTraits.pad(state))LilyTraits.schedule(level,pos,state);
  return result;
 }
}
