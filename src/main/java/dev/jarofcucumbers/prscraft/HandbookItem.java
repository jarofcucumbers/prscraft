package dev.jarofcucumbers.prscraft;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
public final class HandbookItem extends Item {
    public HandbookItem(Properties properties){super(properties);}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        if(level.isClientSide)dev.jarofcucumbers.prscraft.client.RadioClient.openHandbook();
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand),level.isClientSide);
    }
}
