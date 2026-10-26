package dev.jarofcucumbers.prscraft;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
public final class ResearchNotebookItem extends Item{
    public ResearchNotebookItem(Properties p){super(p);}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){if(player instanceof net.minecraft.server.level.ServerPlayer server)NotebookNetwork.open(server,NotebookNetwork.find(server),true,"");return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand),level.isClientSide);}
    @Override public InteractionResult useOn(UseOnContext ctx){if(ctx.getPlayer() instanceof net.minecraft.server.level.ServerPlayer server){NotebookNetwork.capture(server,NotebookNetwork.find(server),ctx.getClickedPos(),-1);return InteractionResult.SUCCESS;}return InteractionResult.sidedSuccess(ctx.getLevel().isClientSide);}
}
