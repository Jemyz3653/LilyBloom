package io.github.jemyz3653.lilybloom.test;
import io.github.jemyz3653.lilybloom.*;
import io.github.jemyz3653.lilybloom.block.*;
import io.github.jemyz3653.lilybloom.registry.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.GameType;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
public class LilyClientVerification implements ClientModInitializer {
 int phase=0,ticks=0,passes=0;boolean opening=false;volatile boolean sceneReady=false;CompletableFuture<Void> reload;
 static void check(boolean b,String why){if(!b)throw new AssertionError(why);}
 static void validate(Minecraft mc,boolean all)throws Exception{
  var names=Set.of("lilybloom","bloomdepth","coloredm","civiccraft","articrystals","museummannequins","dyeworkshop","doordesigner","illustratedbooks");
  var set=mc.getModelManager().getBlockStateModelSet();int states=0,items=0;var parts=new ArrayList<BlockStateModelPart>();var random=RandomSource.create(42);
  for(var block:BuiltInRegistries.BLOCK){
   var ns=BuiltInRegistries.BLOCK.getKey(block).getNamespace();if(!names.contains(ns)||!all&&!ns.equals("lilybloom"))continue;
   for(var s:block.getStateDefinition().getPossibleStates()){
    var m=set.get(s);check(m!=set.missingModel(),"Missing model "+s);check(!m.particleMaterial().sprite().contents().name().getPath().equals("missingno"),"Missing particle "+s);
    if(ns.equals("lilybloom")){
     parts.clear();m.collectParts(random,parts);check(!parts.isEmpty(),"Empty lily geometry "+s);
     for(var p:parts){
      for(var q:p.getQuads(null))check(!q.materialInfo().sprite().contents().name().getPath().equals("missingno"),"Missing lily texture "+s);
      for(var side:Direction.values())for(var q:p.getQuads(side))check(!q.materialInfo().sprite().contents().name().getPath().equals("missingno"),"Missing culled lily texture "+s);
     }
    }states++;
   }
  }
  for(var item:BuiltInRegistries.ITEM){if(!names.contains(BuiltInRegistries.ITEM.getKey(item).getNamespace()))continue;
   var rs=new ItemStackRenderState();mc.getItemModelResolver().updateForTopItem(rs,new ItemStack(item),ItemDisplayContext.GUI,mc.level,mc.player,0);
   check(!rs.isEmpty()&&rs.pickParticleMaterial(random)!=null,"Missing item "+item);items++;
  }
  for(var block:ModBlocks.BLOOMING_LILY_PADS.values())for(var leaf:LilyTraits.Leaf.values())for(var pattern:LilyTraits.Pattern.values())for(var stage:LilyStage.values()){
   var s=block.defaultBlockState().setValue(LilyTraits.LEAF,leaf).setValue(LilyTraits.PATTERN,pattern).setValue(BloomingLilyPadBlock.STAGE,stage).setValue(LilyTraits.GLOW,true);
   var item=LilyTraits.snapshot(s);var rs=new ItemStackRenderState();mc.getItemModelResolver().updateForTopItem(rs,item,ItemDisplayContext.GUI,mc.level,mc.player,0);
   check(!rs.isEmpty()&&rs.pickParticleMaterial(random)!=null&&!rs.pickParticleMaterial(random).sprite().contents().name().getPath().equals("missingno"),"Missing genetic item");items++;
  }
  String mode=LilyBloomConfig.get().hdTextures;System.out.println("LILY CLIENT PASS "+mode+": "+states+" block states, "+items+" item models");
  Files.writeString(Path.of("lily-client-results.txt"),mode+": "+states+" states, "+items+" items\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);
 }
 static void picture(Minecraft mc,String name){Screenshot.grab(mc.gameDirectory,name,mc.gameRenderer.mainRenderTarget(),1,c->System.out.println(c.getString()));}
 void scene(Minecraft mc){mc.getSingleplayerServer().execute(()->{
  var server=mc.getSingleplayerServer();var w=server.overworld();
  server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"time set midnight");
  for(int x=98;x<=124;x++)for(int z=100;z<=121;z++){
   w.setBlock(new BlockPos(x,83,z),Blocks.SMOOTH_QUARTZ.defaultBlockState(),Block.UPDATE_ALL);
   w.setBlock(new BlockPos(x,84,z),(x==98||x==124||z==100||z==121?Blocks.SMOOTH_QUARTZ:Blocks.WATER).defaultBlockState(),Block.UPDATE_ALL);
   for(int y=85;y<90;y++)w.setBlock(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL|Block.UPDATE_SUPPRESS_DROPS);
  }
  for(int i=0;i<4;i++){
   var s=ModBlocks.BLOOMING_LILY_PADS.get(LilyColor.values()[i]).defaultBlockState().setValue(LilyTraits.LEAF,LilyTraits.Leaf.values()[i]).setValue(LilyTraits.GLOW,true).setValue(LilyTraits.NIGHT,true).setValue(LilyTraits.PATTERN,i%2==0?LilyTraits.Pattern.SPECKLED:LilyTraits.Pattern.STRIPED);
   w.setBlock(new BlockPos(102+i*5,85,104),s,Block.UPDATE_ALL);
  }
  for(int i=0;i<3;i++){
   var b=ModBlocks.bigLilyPad(i+2);var o=new BlockPos(101+i*7,85,108);
   b.placeAt(w,o,GiantFlower.of(LilyColor.values()[i]),LilyStage.BLOOM,Block.UPDATE_ALL);
   LilyTraits.update(w,o,w.getBlockState(o).setValue(LilyTraits.LEAF,LilyTraits.Leaf.values()[i+1]).setValue(LilyTraits.GLOW,true).setValue(LilyTraits.PATTERN,i%2==0?LilyTraits.Pattern.SPECKLED:LilyTraits.Pattern.STRIPED));
   var d=o.offset(0,0,7);b.placeAt(w,d,GiantFlower.NONE,LilyStage.BLOOM,Block.UPDATE_ALL);
   LilyTraits.update(w,d,w.getBlockState(d).setValue(LilyTraits.DECOR,LilyTraits.Decoration.values()[i+1]));
  }
  var p=server.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.CREATIVE);p.getAbilities().flying=true;p.onUpdateAbilities();
  p.teleportTo(w,111.5,94,99.5,Set.of(),0,38,true);sceneReady=true;
 });}
 public void onInitializeClient(){ClientTickEvents.END_CLIENT_TICK.register(mc->{try{
  if(!mc.isGameLoadFinished())return;
  if(!opening){opening=true;mc.options.pauseOnLostFocus=false;mc.options.renderDistance().set(6);mc.createWorldOpenFlows().openWorld("PondChecks",()->{});return;}
  if(mc.level==null||mc.player==null)return;
  if(mc.gui.screen()!=null||mc.gui.overlay()!=null)return;
  if(phase==0){validate(mc,true);scene(mc);if(!mc.gui.hud.isHidden())mc.gui.hud.toggle();phase=1;ticks=0;return;}
  if(phase==1&&sceneReady&&++ticks>300){picture(mc,"lily-bloom-2.0-night.png");mc.getSingleplayerServer().execute(()->mc.getSingleplayerServer().getCommands().performPrefixedCommand(mc.getSingleplayerServer().createCommandSourceStack(),"time set noon"));phase=2;ticks=0;return;}
  if(phase==2&&++ticks>120){picture(mc,"lily-bloom-2.0-day.png");phase=3;ticks=0;return;}
  if(phase==3){LilyBloomConfig.get().hdTextures="32x";var repo=mc.getResourcePackRepository();repo.setSelected(repo.getSelectedIds());reload=mc.reloadResourcePacks();phase=4;return;}
  if(phase==4&&reload.isDone()&&++ticks>160){
   reload.join();validate(mc,false);picture(mc,"lily-bloom-2.0-32x.png");LilyBloomConfig.get().hdTextures="64x";var repo=mc.getResourcePackRepository();repo.setSelected(repo.getSelectedIds());reload=mc.reloadResourcePacks();phase=5;ticks=0;return;
  }
  if(phase==5&&reload.isDone()&&++ticks>240){
   reload.join();validate(mc,false);picture(mc,"lily-bloom-2.0-64x.png");phase=6;ticks=0;return;
  }
  if(phase==6&&++ticks>30){Files.writeString(Path.of("lily-client-passed.txt"),"PASS: default, 32x and 64x; all nine mods; patterned items and full block geometry; scene rendered");mc.stop();phase=7;}
 }catch(Throwable e){e.printStackTrace();try{Files.writeString(Path.of("lily-client-failed.txt"),e.toString());}catch(Exception ignored){}mc.stop();phase=7;}});}
}
