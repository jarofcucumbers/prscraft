package dev.jarofcucumbers.prscraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
public final class ChalkDyeRecipe extends CustomRecipe{
    public ChalkDyeRecipe(CraftingBookCategory category){super(category);}
    @Override public boolean matches(CraftingInput input,Level level){int chalk=0,dye=0;for(int i=0;i<input.size();i++){var s=input.getItem(i);if(s.isEmpty())continue;if(s.getItem() instanceof ChalkItem)chalk++;else if(s.getItem() instanceof DyeItem)dye++;else return false;}return chalk==1&&dye==1;}
    @Override public ItemStack assemble(CraftingInput input,HolderLookup.Provider provider){ItemStack chalk=ItemStack.EMPTY;DyeItem dye=null;for(int i=0;i<input.size();i++){var s=input.getItem(i);if(s.isEmpty())continue;if(s.getItem() instanceof ChalkItem){if(!chalk.isEmpty())return ItemStack.EMPTY;chalk=s;}else if(s.getItem() instanceof DyeItem d){if(dye!=null)return ItemStack.EMPTY;dye=d;}else return ItemStack.EMPTY;}
        if(chalk.isEmpty()||dye==null)return ItemStack.EMPTY;
        int palette=java.util.Arrays.asList(BoardPixels.COLORS).indexOf(dye.getDyeColor().getName());if(palette<0)return ItemStack.EMPTY;
        return chalk.transmuteCopy(PRSCraft.CHALKS.get(palette).get(),1);
    }
    @Override public boolean canCraftInDimensions(int w,int h){return w*h>=2;}
    @Override public RecipeSerializer<?> getSerializer(){return PRSCraft.CHALK_DYE.get();}
}
