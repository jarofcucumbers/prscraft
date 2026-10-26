package dev.jarofcucumbers.prscraft;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
public final class CodebookCardItem extends Item{
    public CodebookCardItem(Properties properties){super(properties);}
    public static String mapping(ItemStack stack){var data=stack.get(DataComponents.CUSTOM_DATA);return data!=null&&data.copyTag().contains("codebook")?data.copyTag().getString("codebook"):SymbolCodebook.DEFAULT;}
    public static SymbolCodebook read(ItemStack stack){return SymbolCodebook.parse(mapping(stack));}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){if(player instanceof net.minecraft.server.level.ServerPlayer server)CodebookNetwork.open(server,player.blockPosition(),false,hand==InteractionHand.OFF_HAND,true,"");return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand),level.isClientSide);}
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,java.util.List<Component> lines,TooltipFlag flag){try{var book=read(stack);lines.add(Component.translatable("tooltip.prscraft.codebook",book.codes().size()).withStyle(net.minecraft.ChatFormatting.AQUA));lines.add(Component.translatable("tooltip.prscraft.codebook_edit").withStyle(net.minecraft.ChatFormatting.GRAY));}catch(IllegalArgumentException e){lines.add(Component.translatable("error.prscraft."+e.getMessage()).withStyle(net.minecraft.ChatFormatting.RED));}}
}
