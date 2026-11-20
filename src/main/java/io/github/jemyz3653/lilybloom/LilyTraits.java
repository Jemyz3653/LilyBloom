package io.github.jemyz3653.lilybloom;

import io.github.jemyz3653.lilybloom.block.*;
import io.github.jemyz3653.lilybloom.registry.ModBlocks;
import io.github.jemyz3653.lilybloom.registry.ModItems;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import java.util.*;

/** In-world genetics live in ordinary block states, so chunks and standard item components persist them. */
public final class LilyTraits {
    public enum Leaf implements StringRepresentable {
        ROUND, OVAL, NOTCHED, CURLED;
        public String getSerializedName(){return name().toLowerCase(Locale.ROOT);}
    }
    public enum Pattern implements StringRepresentable {
        PLAIN, SPECKLED, STRIPED;
        public String getSerializedName(){return name().toLowerCase(Locale.ROOT);}
    }
    public enum Decoration implements StringRepresentable {
        NONE, CANDLE, LANTERN, SOUL_LANTERN;
        public String getSerializedName(){return name().toLowerCase(Locale.ROOT);}
        public Item item(){return switch(this){case CANDLE->Items.CANDLE;case LANTERN->Items.LANTERN;case SOUL_LANTERN->Items.SOUL_LANTERN;default->Items.AIR;};}
    }
    public static final BooleanProperty NIGHT=BooleanProperty.create("nocturnal");
    public static final BooleanProperty GLOW=BooleanProperty.create("glowing");
    public static final EnumProperty<Leaf> LEAF=EnumProperty.create("leaf_shape",Leaf.class);
    public static final EnumProperty<Pattern> PATTERN=EnumProperty.create("pattern",Pattern.class);
    public static final EnumProperty<Decoration> DECOR=EnumProperty.create("decoration",Decoration.class);
    public static final List<Property<?>> GENES=List.of(NIGHT,GLOW,LEAF,PATTERN);
    private LilyTraits(){}
    public static boolean pad(BlockState s){return s.getBlock() instanceof BloomingLilyPadBlock || s.getBlock() instanceof BigLilyPadBlock;}
    public static BlockState defaults(BlockState s){return s.setValue(NIGHT,false).setValue(GLOW,false).setValue(LEAF,Leaf.ROUND).setValue(PATTERN,Pattern.PLAIN);}
    public static boolean flower(BlockState s){return s.getBlock() instanceof BloomingLilyPadBlock && s.getValue(BloomingLilyPadBlock.HAS_FLOWER) || s.getBlock() instanceof BigLilyPadBlock && s.getValue(BigLilyPadBlock.FLOWER)!=GiantFlower.NONE;}
    public static boolean full(BlockState s){return flower(s)&&s.getValue(BloomingLilyPadBlock.STAGE)==LilyStage.BLOOM;}
    public static LilyColor color(BlockState s){
        if(s.getBlock() instanceof BloomingLilyPadBlock b)return b.getColor();
        if(s.getBlock() instanceof BigLilyPadBlock && flower(s))return LilyColor.valueOf(s.getValue(BigLilyPadBlock.FLOWER).getSerializedName().toUpperCase(Locale.ROOT));
        return null;
    }
    public static int light(BlockState s){
        if(!s.hasProperty(GLOW))return 0;
        if(s.getBlock() instanceof BigLilyPadBlock){
            if(!BigLilyPadBlock.isAnchor(s))return 0;
            int decoration=switch(s.getValue(DECOR)){case CANDLE->12;case LANTERN->15;case SOUL_LANTERN->10;default->0;};
            if(decoration>0)return decoration;
        }
        return s.getValue(GLOW)&&flower(s)?(full(s)?10:2):0;
    }
    private static <T extends Comparable<T>> BlockState copy(BlockState from,BlockState to,Property<T> p){return to.setValue(p,from.getValue(p));}
    public static BlockState genes(BlockState from,BlockState to){for(var p:GENES)to=copy(from,to,p);return to;}
    public static void update(Level level,BlockPos pos,BlockState target){
        var old=level.getBlockState(pos);
        if(old.getBlock()!=target.getBlock())throw new IllegalArgumentException("Trait update must keep block identity");
        if(old.getBlock() instanceof BigLilyPadBlock b){
            var origin=BigLilyPadBlock.origin(pos,old);
            for(int x=0;x<b.size();x++)for(int z=0;z<b.size();z++){
                var p=origin.offset(x,0,z);var s=level.getBlockState(p);
                if(s.getBlock()==b && s.getValue(b.partX())==x&&s.getValue(b.partZ())==z)
                    level.setBlock(p,target.setValue(b.partX(),x).setValue(b.partZ(),z),Block.UPDATE_CLIENTS);
            }
            schedule(level,BigLilyPadBlock.anchor(pos,target),target);
        }else{level.setBlock(pos,target,Block.UPDATE_ALL);schedule(level,pos,target);}
    }
    public static void schedule(Level level,BlockPos pos,BlockState s){
        if(!level.isClientSide()&&s.getValue(NIGHT))level.scheduleTick(pos,s.getBlock(),100);
    }
    public static boolean isNightAt(long ticks){long t=Math.floorMod(ticks,24000L);return t>=13000&&t<23000;}
    public static boolean isNight(Level level){return isNightAt(level.getOverworldClockTime());}
    public static void animate(Level level,BlockPos pos,BlockState s,net.minecraft.util.RandomSource random){
        if(s.getBlock() instanceof BigLilyPadBlock&&!BigLilyPadBlock.isAnchor(s))return;
        double x=s.getBlock() instanceof BigLilyPadBlock?BigLilyPadBlock.centerX(pos,s):pos.getX()+.5;
        double z=s.getBlock() instanceof BigLilyPadBlock?BigLilyPadBlock.centerZ(pos,s):pos.getZ()+.5;
        if(s.hasProperty(DECOR)&&s.getValue(DECOR)==Decoration.CANDLE)
            level.addParticle(net.minecraft.core.particles.ParticleTypes.SMALL_FLAME,x,pos.getY()+.55,z,0,0,0);
        if(LilyBloomConfig.get().glowParticles&&s.getValue(GLOW)&&full(s)&&random.nextInt(20)==0)
            level.addParticle(net.minecraft.core.particles.ParticleTypes.GLOW,x+(random.nextDouble()-.5)*.6,pos.getY()+.35,z+(random.nextDouble()-.5)*.6,0,.015,0);
    }
    /** Scheduled, not random ticks: nocturnal flowers close within five seconds of daylight. */
    public static void nightTick(ServerLevel level,BlockPos pos,BlockState s){
        if(!pad(s)||!s.getValue(NIGHT))return;
        if(s.getBlock() instanceof BigLilyPadBlock&&!BigLilyPadBlock.isAnchor(s))return;
        if(flower(s)){
            var stage=s.getValue(BloomingLilyPadBlock.STAGE);
            var wanted=!isNight(level)?LilyStage.BUD:stage.next();
            if(wanted!=stage)update(level,pos,s.setValue(BloomingLilyPadBlock.STAGE,wanted));
        }
        level.scheduleTick(pos,s.getBlock(),100);
    }
    public static ItemStack snapshot(BlockState s){
        var result=new ItemStack(s.getBlock());var props=BlockItemStateProperties.EMPTY;
        for(var p:s.getProperties()){
            if(p.getName().equals("part_x")||p.getName().equals("part_z"))continue;
            props=with(props,p,s);
        }
        result.set(DataComponents.BLOCK_STATE,props);
        return result;
    }
    private static <T extends Comparable<T>> BlockItemStateProperties with(BlockItemStateProperties props,Property<T> p,BlockState s){return props.with(p,s);}
    public static ItemStack offspring(BlockState a,BlockState b,net.minecraft.util.RandomSource random){
        var chosen=random.nextBoolean()?a:b;
        var child=ModBlocks.BLOOMING_LILY_PADS.get(color(chosen)).defaultBlockState().setValue(BloomingLilyPadBlock.STAGE,LilyStage.BUD);
        child=child.setValue(NIGHT,chosen.getValue(NIGHT)).setValue(LEAF,chosen.getValue(LEAF));
        Pattern pattern=chosen.getValue(PATTERN);
        if(random.nextInt(8)==0)pattern=random.nextBoolean()?Pattern.SPECKLED:Pattern.STRIPED;
        return snapshot(child.setValue(PATTERN,pattern));
    }
    public static boolean breed(ServerLevel level,BlockPos pos,BlockState s,net.minecraft.world.entity.player.Player player){
        if(!full(s))return false;
        BlockPos anchor=s.getBlock() instanceof BigLilyPadBlock?BigLilyPadBlock.anchor(pos,s):pos;
        for(var p:BlockPos.betweenClosed(anchor.offset(-6,0,-6),anchor.offset(6,0,6))){
            if(!level.hasChunkAt(p))continue;
            var other=level.getBlockState(p);
            if(!pad(other)||!full(other)||color(other)==color(s))continue;
            if(other.getBlock() instanceof BigLilyPadBlock&&!BigLilyPadBlock.isAnchor(other))continue;
            Block.popResource(level,anchor,offspring(s,other,level.getRandom()));return true;
        }
        return false;
    }
    private static boolean editable(Level level,BlockPos pos,BlockState s,net.minecraft.world.entity.player.Player p){
        if(p.isSpectator()||!p.getAbilities().mayBuild)return false;
        if(!(level instanceof ServerLevel server))return true;
        if(s.getBlock() instanceof BigLilyPadBlock b){
            var origin=BigLilyPadBlock.origin(pos,s);
            for(int x=0;x<b.size();x++)for(int z=0;z<b.size();z++)if(!server.mayInteract(p,origin.offset(x,0,z)))return false;
        }
        return server.mayInteract(p,pos);
    }
    public static void init(){
        UseBlockCallback.EVENT.register((player,level,hand,hit)->{
            var pos=hit.getBlockPos();var s=level.getBlockState(pos);if(!pad(s)||!editable(level,pos,s,player))return InteractionResult.PASS;
            var stack=player.getItemInHand(hand);var target=s;
            if(stack.is(ModItems.TRANSPLANTER)){
                if(level instanceof ServerLevel server){
                    var picked=snapshot(s);BlockPos origin=s.getBlock() instanceof BigLilyPadBlock?BigLilyPadBlock.origin(pos,s):pos;
                    if(s.getBlock() instanceof BigLilyPadBlock b){
                        for(int x=0;x<b.size();x++)for(int z=0;z<b.size();z++)level.setBlock(origin.offset(x,0,z),Blocks.AIR.defaultBlockState(),Block.UPDATE_CLIENTS|Block.UPDATE_SUPPRESS_DROPS);
                    }else level.setBlock(pos,Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL|Block.UPDATE_SUPPRESS_DROPS);
                    if(!player.getInventory().add(picked))Block.popResource(server,pos,picked);
                    stack.hurtAndBreak(1,player,hand.asEquipmentSlot());
                }
                return InteractionResult.SUCCESS;
            }
            if(stack.is(Items.BONE_MEAL)&&full(s)){
                if(level instanceof ServerLevel server && breed(server,pos,s,player)){stack.consume(1,player);return InteractionResult.SUCCESS;}
                return InteractionResult.PASS;
            }
            boolean shear=BloomingLilyPadBlock.isShears(stack)&&player.isShiftKeyDown();
            if(shear)target=s.cycle(LEAF);
            else if(stack.is(Items.GLOW_INK_SAC)&&!s.getValue(GLOW))target=s.setValue(GLOW,true);
            else if(stack.is(Items.INK_SAC)&&s.getValue(GLOW))target=s.setValue(GLOW,false);
            else if(stack.is(ModItems.LUNAR_POLLEN)&&!s.getValue(NIGHT))target=s.setValue(NIGHT,true);
            else if(stack.is(Items.HONEYCOMB)&&s.getValue(NIGHT))target=s.setValue(NIGHT,false);
            else if(s.getBlock() instanceof BigLilyPadBlock){
                if(stack.isEmpty()&&player.isShiftKeyDown()&&s.getValue(DECOR)!=Decoration.NONE){
                    if(level instanceof ServerLevel server)Block.popResource(server,BigLilyPadBlock.anchor(pos,s),new ItemStack(s.getValue(DECOR).item()));
                    target=s.setValue(DECOR,Decoration.NONE);
                }else if(s.getValue(DECOR)==Decoration.NONE&&!flower(s)){
                    for(var d:Decoration.values())if(d!=Decoration.NONE&&stack.is(d.item()))target=s.setValue(DECOR,d);
                }
            }
            if(target==s)return InteractionResult.PASS;
            if(level instanceof ServerLevel){
                update(level,pos,target);
                if(shear)stack.hurtAndBreak(1,player,hand.asEquipmentSlot());
                else if(!stack.isEmpty())stack.consume(1,player);
            }
            return InteractionResult.SUCCESS;
        });
    }
}
