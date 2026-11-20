package io.github.jemyz3653.lilybloom.test;
import io.github.jemyz3653.lilybloom.*;
import io.github.jemyz3653.lilybloom.block.*;
import io.github.jemyz3653.lilybloom.registry.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import java.nio.file.*;
import java.util.*;
public class LilyRegression implements ModInitializer {
 static int checks;
 static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
 static BlockPos P=new BlockPos(100,85,100);
 static void water(ServerLevel w,BlockPos o,int n){for(int x=-2;x<n+2;x++)for(int z=-2;z<n+2;z++){
  var p=o.offset(x,0,z);w.setBlock(p,Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL|Block.UPDATE_SUPPRESS_DROPS);
  w.setBlock(p.below(),Blocks.WATER.defaultBlockState(),Block.UPDATE_ALL);
 }}
 static void clearDrops(ServerLevel w){for(var e:w.getEntitiesOfClass(ItemEntity.class,new AABB(P).inflate(50)))e.discard();}
 static BlockHitResult hit(BlockPos p){return new BlockHitResult(Vec3.atCenterOf(p),Direction.UP,p,false);}
 static void interact(FakePlayer p,ServerLevel w,BlockPos pos,ItemStack stack,boolean shift){
  p.setShiftKeyDown(shift);p.setItemInHand(InteractionHand.MAIN_HAND,stack);
  UseBlockCallback.EVENT.invoker().interact(p,w,InteractionHand.MAIN_HAND,hit(pos));p.setShiftKeyDown(false);
 }
 public void onInitialize(){ServerLifecycleEvents.SERVER_STARTED.register(server->{try{
  var w=server.overworld();var player=FakePlayer.get(w);player.getAbilities().mayBuild=true;player.getAbilities().instabuild=false;player.setPos(100,86,100);
  var fixture=new BlockPos(300,85,300);
  if(Files.exists(Path.of("lily-persistence-created.txt"))){
   var saved=w.getBlockState(fixture);check(saved.getBlock()==ModBlocks.BLOOMING_LILY_PAD,"Saved pad missing");
   check(saved.getValue(LilyTraits.NIGHT)&&saved.getValue(LilyTraits.GLOW)&&saved.getValue(LilyTraits.PATTERN)==LilyTraits.Pattern.STRIPED&&saved.getValue(LilyTraits.LEAF)==LilyTraits.Leaf.CURLED,"Traits lost after save/reopen");
  }
  water(w,P,30);clearDrops(w);
  w.setBlock(P,ModBlocks.BLOOMING_LILY_PAD.defaultBlockState(),Block.UPDATE_ALL);
  for(var item:new Item[]{Items.GLOW_INK_SAC,Items.INK_SAC,ModItems.LUNAR_POLLEN,Items.HONEYCOMB}){
   var stack=new ItemStack(item,2);interact(player,w,P,stack,false);check(stack.getCount()==1,"Trait application cost");
   if(item==Items.GLOW_INK_SAC||item==Items.INK_SAC)check(w.getBlockState(P).getValue(LilyTraits.GLOW)==(item==Items.GLOW_INK_SAC),"Glow coating application/removal");
   else check(w.getBlockState(P).getValue(LilyTraits.NIGHT)==(item==ModItems.LUNAR_POLLEN),"Night trait application/removal");
  }
  var shears=new ItemStack(Items.SHEARS);interact(player,w,P,shears,true);
  check(w.getBlockState(P).getValue(LilyTraits.LEAF)==LilyTraits.Leaf.OVAL&&LilyTraits.full(w.getBlockState(P))&&shears.getDamageValue()==1,"Shape interaction sheared flower");
  var protectedState=w.getBlockState(P);player.getAbilities().mayBuild=false;var deniedTool=new ItemStack(ModItems.TRANSPLANTER);
  interact(player,w,P,deniedTool,false);check(w.getBlockState(P)==protectedState&&deniedTool.getDamageValue()==0,"Adventure permission bypass");player.getAbilities().mayBuild=true;

  for(long t:new long[]{0,12999,13000,18000,22999,23000,24000,-6000})check(LilyTraits.isNightAt(t)==(Math.floorMod(t,24000)>=13000&&Math.floorMod(t,24000)<23000),"Night boundary "+t);
  for(var base:ModBlocks.BLOOMING_LILY_PADS.values()){
   var s=base.defaultBlockState();check(!s.getValue(LilyTraits.NIGHT)&&!s.getValue(LilyTraits.GLOW),"Legacy defaults");
   s=s.setValue(LilyTraits.NIGHT,true).setValue(LilyTraits.GLOW,true).setValue(LilyTraits.PATTERN,LilyTraits.Pattern.SPECKLED).setValue(LilyTraits.LEAF,LilyTraits.Leaf.CURLED).setValue(BloomingLilyPadBlock.STAGE,LilyStage.HALF_OPEN);
   var snap=LilyTraits.snapshot(s);var back=snap.get(DataComponents.BLOCK_STATE).apply(base.defaultBlockState());check(back.equals(s),"Small transplant snapshot");
   w.setBlock(P,s,Block.UPDATE_ALL);
   server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"time set noon");
   LilyTraits.nightTick(w,P,s);check(w.getBlockState(P).getValue(BloomingLilyPadBlock.STAGE)==LilyStage.BUD,"Night pad did not close");
   server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"time set midnight");
   LilyTraits.nightTick(w,P,w.getBlockState(P));LilyTraits.nightTick(w,P,w.getBlockState(P));
   check(LilyTraits.full(w.getBlockState(P)),"Night pad did not open");check(LilyTraits.light(w.getBlockState(P))==10,"Glow level");
   var full=w.getBlockState(P);BloomingLilyPadBlock.shear(w,P,full,new ItemStack(Items.SHEARS),player);
   check(LilyTraits.pad(w.getBlockState(P))&&!LilyTraits.flower(w.getBlockState(P)),"Shearing lost special plant");
   BloomingLilyPadBlock.advance(w,P,w.getBlockState(P));check(LilyTraits.flower(w.getBlockState(P)),"Regrowing bare plant");
   clearDrops(w);
  }
  int shapes=0;
  for(int n:new int[]{2,3,4}){
   var b=ModBlocks.bigLilyPad(n);water(w,P,n);
   b.placeAt(w,P,GiantFlower.NONE,LilyStage.BLOOM,Block.UPDATE_ALL);
   for(var leaf:LilyTraits.Leaf.values()){
    LilyTraits.update(w,P,w.getBlockState(P).setValue(LilyTraits.LEAF,leaf));
    for(int x=0;x<n;x++)for(int z=0;z<n;z++)check(w.getBlockState(P.offset(x,0,z)).getValue(LilyTraits.LEAF)==leaf,"Unsynchronized shape");shapes++;
   }
   for(var decor:new LilyTraits.Decoration[]{LilyTraits.Decoration.CANDLE,LilyTraits.Decoration.LANTERN,LilyTraits.Decoration.SOUL_LANTERN}){
    var stack=new ItemStack(decor.item(),3);interact(player,w,P,stack,false);
    check(stack.getCount()==2,"Decoration cost");check(w.getBlockState(P).getValue(LilyTraits.DECOR)==decor,"Decoration not set");
    var anchor=P.offset(b.anchor(),0,b.anchor());check(LilyTraits.light(w.getBlockState(anchor))>0,"Decoration dark");
    var flower=new ItemStack(ModItems.LILY_FLOWER);w.getBlockState(P).useItemOn(flower,w,player,InteractionHand.MAIN_HAND,hit(P));check(!LilyTraits.flower(w.getBlockState(P)),"Flower overlapped lantern");
    check(!BigLilyPadBlock.grow(w,P,w.getBlockState(P),w.getRandom()),"Bone meal overlapped lantern");
    interact(player,w,P,ItemStack.EMPTY,true);check(w.getBlockState(P).getValue(LilyTraits.DECOR)==LilyTraits.Decoration.NONE,"Decoration removal");
    check(w.getEntitiesOfClass(ItemEntity.class,new AABB(P).inflate(8)).stream().mapToInt(e->e.getItem().is(decor.item())?e.getItem().getCount():0).sum()==1,"Decoration duplicated");clearDrops(w);
   }
   for(var decor:new LilyTraits.Decoration[]{LilyTraits.Decoration.CANDLE,LilyTraits.Decoration.LANTERN,LilyTraits.Decoration.SOUL_LANTERN}){
    water(w,P,n);b.placeAt(w,P,GiantFlower.NONE,LilyStage.BLOOM,Block.UPDATE_ALL);
    LilyTraits.update(w,P,w.getBlockState(P).setValue(LilyTraits.DECOR,decor));clearDrops(w);
    w.destroyBlock(P.offset(n-1,0,n-1),true);
    var drops=w.getEntitiesOfClass(ItemEntity.class,new AABB(P).inflate(10));
    check(drops.stream().mapToInt(e->e.getItem().is(decor.item())?e.getItem().getCount():0).sum()==1,"Break lost or duplicated decoration");
    check(drops.stream().mapToInt(e->e.getItem().is(b.asItem())?e.getItem().getCount():0).sum()==1,"Decorated break duplicated pad");clearDrops(w);
   }
   water(w,P,n);b.placeAt(w,P,GiantFlower.NONE,LilyStage.BLOOM,Block.UPDATE_ALL);
   BigLilyPadBlock.setFlower(w,P,GiantFlower.BLUE,LilyStage.HALF_OPEN);
   LilyTraits.update(w,P,w.getBlockState(P).setValue(LilyTraits.NIGHT,true).setValue(LilyTraits.GLOW,true).setValue(LilyTraits.PATTERN,LilyTraits.Pattern.STRIPED));
   var before=w.getBlockState(P);var snap=LilyTraits.snapshot(before);
   player.getInventory().clearContent();var tool=new ItemStack(ModItems.TRANSPLANTER);interact(player,w,P.offset(n-1,0,n-1),tool,false);
   for(int x=0;x<n;x++)for(int z=0;z<n;z++)check(w.getBlockState(P.offset(x,0,z)).isAir(),"Transplant left fragment");
   check(w.getEntitiesOfClass(ItemEntity.class,new AABB(P).inflate(8)).isEmpty(),"Transplant created extra drops");
   check(tool.getDamageValue()==1,"Transplanter durability");ItemStack picked=ItemStack.EMPTY;
   for(int i=0;i<player.getInventory().getContainerSize();i++)if(player.getInventory().getItem(i).is(b.asItem()))picked=player.getInventory().getItem(i);
   check(!picked.isEmpty()&&picked.getCount()==1,"Transplant item count");
   var restored=picked.get(DataComponents.BLOCK_STATE).apply(b.defaultBlockState());
   for(var prop:LilyTraits.GENES)check(restored.getValue(prop).equals(before.getValue(prop)),"Lost transplant gene "+prop);
   check(restored.getValue(BigLilyPadBlock.FLOWER)==GiantFlower.BLUE&&restored.getValue(BigLilyPadBlock.STAGE)==LilyStage.HALF_OPEN,"Lost flower or stage");
   var context=new BlockPlaceContext(w,player,InteractionHand.MAIN_HAND,picked,hit(P.below())){@Override public BlockPos getClickedPos(){return P;}@Override public Direction getHorizontalDirection(){return Direction.NORTH;}};
   ((io.github.jemyz3653.lilybloom.item.BigLilyPadItem)b.asItem()).place(context);
   var result=w.getBlockState(P);check(result.getBlock()==b&&result.getValue(BigLilyPadBlock.FLOWER)==GiantFlower.BLUE,"Real placement lost flower");
   clearDrops(w);
   w.destroyBlock(P,true);check(w.getEntitiesOfClass(ItemEntity.class,new AABB(P).inflate(10)).stream().mapToInt(e->e.getItem().is(b.asItem())?e.getItem().getCount():0).sum()==1,"Big pad break duplicated");clearDrops(w);
  }
  water(w,P,8);var a=ModBlocks.BLOOMING_LILY_PAD.defaultBlockState();var b=ModBlocks.BLOOMING_LILY_PADS.get(LilyColor.BLUE).defaultBlockState().setValue(LilyTraits.NIGHT,true);
  w.setBlock(P,a,Block.UPDATE_ALL);w.setBlock(P.offset(2,0,0),b,Block.UPDATE_ALL);var bone=new ItemStack(Items.BONE_MEAL,4);interact(player,w,P,bone,false);
  check(bone.getCount()==3,"Breeding did not cost bone meal");check(w.getEntitiesOfClass(ItemEntity.class,new AABB(P).inflate(8)).size()==1,"Breeding offspring count");clearDrops(w);
  var rng=net.minecraft.util.RandomSource.create(7321);var patterns=EnumSet.noneOf(LilyTraits.Pattern.class);int rare=0;
  for(int i=0;i<1024;i++){
   var child=LilyTraits.offspring(a,b,rng);var s=child.get(DataComponents.BLOCK_STATE).apply(((BlockItem)child.getItem()).getBlock().defaultBlockState());
   patterns.add(s.getValue(LilyTraits.PATTERN));if(s.getValue(LilyTraits.PATTERN)!=LilyTraits.Pattern.PLAIN)rare++;
   check(s.getValue(BloomingLilyPadBlock.STAGE)==LilyStage.BUD,"Child is not a bud");
  }
  check(patterns.size()==3&&rare>70&&rare<190,"Rare pattern distribution");
  water(w,fixture,1);w.setBlock(fixture,ModBlocks.BLOOMING_LILY_PAD.defaultBlockState().setValue(LilyTraits.NIGHT,true).setValue(LilyTraits.GLOW,true).setValue(LilyTraits.LEAF,LilyTraits.Leaf.CURLED).setValue(LilyTraits.PATTERN,LilyTraits.Pattern.STRIPED),Block.UPDATE_ALL);
  Files.writeString(Path.of("lily-persistence-created.txt"),"Saved fixture at 300,85,300");
  String msg="PASS: "+checks+" assertions; night cycle; four shapes across all sizes; lights and item costs; shearing/regrowth; whole-pad transplantation and placement; breeding ("+rare+" rare / 1024); no duplicate drops";
  Files.writeString(Path.of("lily-tests-passed.txt"),msg);System.out.println(msg);
 }catch(Throwable e){e.printStackTrace();try{Files.writeString(Path.of("lily-tests-failed.txt"),e.toString());}catch(Exception ignored){}}});}
}
