package dev.jarofcucumbers.prscraft;
import net.minecraft.world.item.ItemStack;
public final class BoardTools{
    private BoardTools(){}
    public static boolean isTool(ItemStack stack){return stack.getItem() instanceof ChalkItem||stack.is(PRSCraft.ERASER.get());}
    public static int remaining(ItemStack stack){return isTool(stack)?Math.max(0,stack.getMaxDamage()-stack.getDamageValue()):0;}
    public static int apply(DrawingBoardEntity board,ItemStack held,int[] requested){
        int[] pixels=BoardPixels.checked(requested);int count=Math.min(pixels.length,remaining(held));
        if(count>0){int color=held.getItem() instanceof ChalkItem chalk?chalk.palette:0;board.paint(java.util.Arrays.copyOf(pixels,count),color);consume(held,count);}return count;
    }
    /** Chalk breaks at zero; the eraser deliberately remains at maxDamage. Creative players also pay pixel costs. */
    public static void consume(ItemStack stack,int pixels){int damage=Math.min(stack.getMaxDamage(),stack.getDamageValue()+pixels);stack.setDamageValue(damage);if(stack.getItem() instanceof ChalkItem&&damage>=stack.getMaxDamage())stack.shrink(1);}
}
