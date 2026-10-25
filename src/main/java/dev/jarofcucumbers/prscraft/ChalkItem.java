package dev.jarofcucumbers.prscraft;
import net.minecraft.world.item.Item;
public final class ChalkItem extends Item{
    public static final int CAPACITY=4096;
    public static void upgrade(net.minecraft.world.item.ItemStack stack){if(stack.getItem() instanceof ChalkItem&&stack.getMaxDamage()==1024)stack.set(net.minecraft.core.component.DataComponents.MAX_DAMAGE,CAPACITY);}
    @Override public void inventoryTick(net.minecraft.world.item.ItemStack stack,net.minecraft.world.level.Level level,net.minecraft.world.entity.Entity entity,int slot,boolean selected){if(!level.isClientSide)upgrade(stack);super.inventoryTick(stack,level,entity,slot,selected);}
    public final int palette;
    public ChalkItem(Properties properties,int palette){super(properties);this.palette=palette;}
}
