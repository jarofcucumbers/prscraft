package dev.jarofcucumbers.prscraft;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.server.level.ServerPlayer;
public final class CableWrenchItem extends Item{
    public CableWrenchItem(Properties properties){super(properties);}
    @Override public InteractionResult useOn(UseOnContext context){
        var state=context.getLevel().getBlockState(context.getClickedPos());
        if(state.getBlock() instanceof PortBlock){
            var player=context.getPlayer();if(player==null||!player.mayBuild()||!context.getLevel().mayInteract(player,context.getClickedPos()))return InteractionResult.FAIL;
            if(!context.getLevel().isClientSide){var directions=net.minecraft.core.Direction.values();var current=state.getValue(PortBlock.FACING);context.getLevel().setBlock(context.getClickedPos(),state.setValue(PortBlock.FACING,directions[(current.ordinal()+1)%directions.length]),3);}
            return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
        }
        if(!(context.getLevel().getBlockState(context.getClickedPos()).getBlock() instanceof CableBlock))return InteractionResult.PASS;
        if(!context.getLevel().isClientSide&&context.getPlayer() instanceof ServerPlayer player)CableTuning.send(player,context.getClickedPos(),true,"");
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }
}
