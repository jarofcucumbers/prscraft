package dev.jarofcucumbers.prscraft;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class RadioBlockEntity extends BlockEntity {
    private RadioSettings settings = RadioSettings.DEFAULT;
    public RadioBlockEntity(BlockPos pos, BlockState state) { super(PRSCraft.RADIO_ENTITY.get(), pos, state); }
    public RadioSettings settings() { return settings; }
    public void configure(RadioSettings settings) {
        this.settings = settings; setChanged();
        if (level != null) level.setBlock(worldPosition, getBlockState().setValue(TransmitterBlock.POWERED, settings.enabled()), 3);
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries); tag.put("radio", settings.save());
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("radio")) {
            try { settings = RadioSettings.read(tag.getCompound("radio")); }
            catch (IllegalArgumentException ignored) { settings = RadioSettings.DEFAULT; }
        }
    }
}
