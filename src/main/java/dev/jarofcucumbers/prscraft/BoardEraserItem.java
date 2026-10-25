package dev.jarofcucumbers.prscraft;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.*;
public final class BoardEraserItem extends Item{
    public static final int CAPACITY=16384;
    public static void upgrade(ItemStack stack){if(stack.getItem() instanceof BoardEraserItem&&stack.getMaxDamage()==512)stack.set(net.minecraft.core.component.DataComponents.MAX_DAMAGE,CAPACITY);}
    @Override public void inventoryTick(ItemStack stack,net.minecraft.world.level.Level level,net.minecraft.world.entity.Entity entity,int slot,boolean selected){if(!level.isClientSide)upgrade(stack);super.inventoryTick(stack,level,entity,slot,selected);}
    public BoardEraserItem(Properties p){super(p);}
    @Override public InteractionResult useOn(UseOnContext c){
        if(c.getPlayer()==null||!c.getPlayer().mayBuild()||!c.getLevel().mayInteract(c.getPlayer(),c.getClickedPos()))return InteractionResult.FAIL;
        upgrade(c.getItemInHand());
        var state=c.getLevel().getBlockState(c.getClickedPos());
        if(!state.is(Blocks.WATER_CAULDRON)||c.getItemInHand().getDamageValue()==0)return InteractionResult.PASS;
        if(!c.getLevel().isClientSide){if(BoardTools.remaining(c.getItemInHand())==0&&c.getPlayer() instanceof net.minecraft.server.level.ServerPlayer player)Milestones.award(player,"wash_eraser");c.getItemInHand().setDamageValue(0);LayeredCauldronBlock.lowerFillLevel(state,c.getLevel(),c.getClickedPos());c.getLevel().playSound(null,c.getClickedPos(),net.minecraft.sounds.SoundEvents.BUCKET_EMPTY,net.minecraft.sounds.SoundSource.BLOCKS,0.6f,1.2f);}
        return InteractionResult.sidedSuccess(c.getLevel().isClientSide);
    }
}
