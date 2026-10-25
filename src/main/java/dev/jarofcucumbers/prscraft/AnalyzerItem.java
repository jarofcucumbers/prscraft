package dev.jarofcucumbers.prscraft;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.server.level.ServerPlayer;
public final class AnalyzerItem extends Item {
    public AnalyzerItem(Properties properties){super(properties);}
    @Override public InteractionResult useOn(UseOnContext context){
        if(!(context.getLevel().getBlockState(context.getClickedPos()).getBlock() instanceof PortBlock))return InteractionResult.PASS;
        if(!context.getLevel().isClientSide && context.getPlayer() instanceof ServerPlayer player)AnalyzerService.send(player,context.getClickedPos(),context.getClickedPos(),true);
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }
}
