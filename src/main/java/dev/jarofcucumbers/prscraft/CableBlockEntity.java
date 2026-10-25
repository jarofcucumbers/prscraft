package dev.jarofcucumbers.prscraft;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
public final class CableBlockEntity extends BlockEntity{
    private CableSettings settings=CableSettings.DEFAULT;
    public CableBlockEntity(BlockPos pos,BlockState state){super(PRSCraft.CABLE_ENTITY.get(),pos,state);}
    public CableSettings settings(){return settings;}
    public void configure(CableSettings settings){this.settings=settings;setChanged();}
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries){super.saveAdditional(tag,registries);tag.putDouble("metres",settings.metres());tag.putDouble("damage",settings.damage());}
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries){super.loadAdditional(tag,registries);if(tag.contains("metres"))try{settings=new CableSettings(tag.getDouble("metres"),tag.getDouble("damage"));}catch(IllegalArgumentException ignored){settings=CableSettings.DEFAULT;}}
}
