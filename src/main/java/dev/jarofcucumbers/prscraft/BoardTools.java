package dev.jarofcucumbers.prscraft;
import net.minecraft.world.item.ItemStack;
public final class BoardTools{
    private BoardTools(){}
    public static boolean isTool(ItemStack stack){return stack.getItem() instanceof ChalkItem||stack.is(PRSCraft.ERASER.get());}
    public static int remaining(ItemStack stack){return isTool(stack)?Math.max(0,stack.getMaxDamage()-stack.getDamageValue()):0;}
    public static int apply(DrawingBoardEntity board,ItemStack held,int[] requested){
        int[] pixels=BoardPixels.checked(requested);BoardEraserItem.upgrade(held);ChalkItem.upgrade(held);int count=Math.min(pixels.length,remaining(held));
        if(count>0){int color=held.getItem() instanceof ChalkItem chalk?chalk.palette:0;board.paint(java.util.Arrays.copyOf(pixels,count),color);consume(held,count);}return count;
    }
    public static int clearAllCost(byte[] pixels){int painted=0;for(byte pixel:pixels)if(pixel!=0)painted++;return (painted*11+9)/10;}
    /** Atomic bulk erase, charging only colored pixels plus rounded-up ten percent. */
    public static boolean clearAll(DrawingBoardEntity board,ItemStack held){
        if(!held.is(PRSCraft.ERASER.get()))return false;BoardEraserItem.upgrade(held);byte[] pixels=board.pixels();int cost=clearAllCost(pixels);if(remaining(held)<cost)return false;
        if(cost>0){var indices=java.util.stream.IntStream.range(0,pixels.length).filter(i->pixels[i]!=0).toArray();board.paint(indices,0);consume(held,cost);}return true;
    }
    /** Chalk breaks at zero; the eraser deliberately remains at maxDamage. Creative players also pay pixel costs. */
    public static void consume(ItemStack stack,int pixels){int damage=Math.min(stack.getMaxDamage(),stack.getDamageValue()+pixels);stack.setDamageValue(damage);if(stack.getItem() instanceof ChalkItem&&damage>=stack.getMaxDamage())stack.shrink(1);}
}
