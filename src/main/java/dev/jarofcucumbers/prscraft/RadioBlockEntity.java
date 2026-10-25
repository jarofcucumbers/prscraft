package dev.jarofcucumbers.prscraft;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class RadioBlockEntity extends BlockEntity {
    private RadioSettings settings = RadioSettings.DEFAULT;
    private ProcessorSettings processor=ProcessorSettings.DEFAULT;
    private int graphLayers=15,graphTerms=255;
    public final net.neoforged.neoforge.items.ItemStackHandler media=new net.neoforged.neoforge.items.ItemStackHandler(2){
        @Override public int getSlotLimit(int slot){return 1;}
        @Override public boolean isItemValid(int slot,net.minecraft.world.item.ItemStack stack){
            return slot==0?(stack.is(net.minecraft.world.item.Items.PAPER)||stack.is(PRSCraft.TEMPLATE.get())):stack.is(PRSCraft.DATA_CARD.get());
        }
        @Override protected void onContentsChanged(int slot){
            setChanged();
            if(slot==0 && level!=null && !level.isClientSide) applyTemplate();
        }
    };
    public String kind(){return getBlockState().getBlock() instanceof ProcessorBlock block?block.kind:"radio";}
    public ProcessorSettings processor(){return processor;}
    public void configureProcessor(ProcessorSettings value){processor=value;setChanged();}
    public int graphLayers(){return graphLayers;}
    public int graphTerms(){return graphTerms;}
    public void graph(int layers,int terms){graphLayers=layers&31;graphTerms=terms&255;setChanged();}
    public boolean applyTemplate(){
        var stack=media.getStackInSlot(0);
        var component=stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        if(!stack.is(PRSCraft.TEMPLATE.get())||component==null)return false;
        var tag=component.copyTag();
        if(!kind().equals(tag.getString("kind")))return false;
        try {
            var next=RadioSettings.read(tag.getCompound("settings"));
            var nextProcessor=ProcessorSettings.read(tag.getCompound("processor"));
            configure(next);configureProcessor(nextProcessor);graph(tag.getInt("graphLayers"),tag.getInt("graphTerms"));return true;
        }catch(IllegalArgumentException ignored){return false;}
    }
    public RadioBlockEntity(BlockPos pos, BlockState state) { super(PRSCraft.RADIO_ENTITY.get(), pos, state); }
    public RadioSettings settings() { return settings; }
    public void configure(RadioSettings settings) {
        this.settings = settings; setChanged();
        if (level != null) level.setBlock(worldPosition, getBlockState().setValue(TransmitterBlock.POWERED, settings.enabled()), 3);
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries); tag.put("radio", settings.save());
        tag.put("processor",processor.save());tag.putInt("graphLayers",graphLayers);tag.putInt("graphTerms",graphTerms);
        tag.put("media",media.serializeNBT(registries));
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if(tag.contains("processor"))try{processor=ProcessorSettings.read(tag.getCompound("processor"));}catch(IllegalArgumentException ignored){processor=ProcessorSettings.DEFAULT;}
        if(tag.contains("graphLayers")){graphLayers=tag.getInt("graphLayers")&31;graphTerms=tag.getInt("graphTerms")&255;}
        if(tag.contains("media"))media.deserializeNBT(registries,tag.getCompound("media"));
        if (tag.contains("radio")) {
            try { settings = RadioSettings.read(tag.getCompound("radio")); }
            catch (IllegalArgumentException ignored) { settings = RadioSettings.DEFAULT; }
        }
    }
}
