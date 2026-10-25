package dev.jarofcucumbers.prscraft;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.server.level.ServerPlayer;
public final class CableWrenchItem extends Item{
    public CableWrenchItem(Properties properties){super(properties);}
    @Override public InteractionResult useOn(UseOnContext context){
        if(!(context.getLevel().getBlockState(context.getClickedPos()).getBlock() instanceof CableBlock))return InteractionResult.PASS;
        if(!context.getLevel().isClientSide&&context.getPlayer() instanceof ServerPlayer player)CableTuning.send(player,context.getClickedPos(),true,"");
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }
}
