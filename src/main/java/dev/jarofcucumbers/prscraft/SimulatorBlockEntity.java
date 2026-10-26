package dev.jarofcucumbers.prscraft;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
public final class SimulatorBlockEntity extends BlockEntity{
    private SimulatorDocument document=new SimulatorDocument();public SimulatorDocument document(){return document;}
    public SimulatorBlockEntity(BlockPos pos,BlockState state){super(PRSCraft.SIMULATOR_ENTITY.get(),pos,state);}
    public void commit(SimulatorDocument next){try{var bytes=new java.io.ByteArrayOutputStream();net.minecraft.nbt.NbtIo.write(next.save(),new java.io.DataOutputStream(bytes));if(bytes.size()>512*1024)throw new IllegalArgumentException("sim_memory");}catch(java.io.IOException e){throw new IllegalArgumentException("sim_memory");}document=next;setChanged();}
    @Override protected void collectImplicitComponents(net.minecraft.core.component.DataComponentMap.Builder builder){super.collectImplicitComponents(builder);var tag=new CompoundTag();tag.put("document",document.save());BlockEntity.addEntityType(tag,getType());builder.set(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA,net.minecraft.world.item.component.CustomData.of(tag));}
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider r){super.saveAdditional(tag,r);tag.put("document",document.save());}
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider r){super.loadAdditional(tag,r);if(tag.contains("document"))try{document=new SimulatorDocument(tag.getCompound("document"));}catch(IllegalArgumentException ignored){document=new SimulatorDocument();}}
}
