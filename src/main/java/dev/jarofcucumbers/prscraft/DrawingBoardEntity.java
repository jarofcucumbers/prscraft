package dev.jarofcucumbers.prscraft;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
public final class DrawingBoardEntity extends BlockEntity{
    private byte[] pixels=new byte[BoardPixels.COUNT];
    public DrawingBoardEntity(BlockPos pos,BlockState state){super(PRSCraft.BOARD_ENTITY.get(),pos,state);}
    public byte[] pixels(){return pixels.clone();}
    public void paint(int[] indices,int color){for(int p:indices)pixels[p]=(byte)color;setChanged();if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries){super.saveAdditional(tag,registries);tag.putByteArray("pixels",pixels);}
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries){super.loadAdditional(tag,registries);pixels=BoardPixels.read(tag.getByteArray("pixels"));}
    @Override protected void collectImplicitComponents(net.minecraft.core.component.DataComponentMap.Builder builder){super.collectImplicitComponents(builder);var tag=new CompoundTag();tag.putByteArray("pixels",pixels);BlockEntity.addEntityType(tag,getType());builder.set(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA,net.minecraft.world.item.component.CustomData.of(tag));}
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries){return saveWithoutMetadata(registries);}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
}
