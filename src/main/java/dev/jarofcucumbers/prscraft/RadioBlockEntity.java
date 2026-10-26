package dev.jarofcucumbers.prscraft;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class RadioBlockEntity extends BlockEntity {
    private RadioSettings settings = RadioSettings.DEFAULT;
    private PlotSettings plot=PlotSettings.DEFAULT;
    public PlotSettings plot(){return plot;}
    public void configurePlot(PlotSettings value){plot=value;setChanged();}
    public SymbolCodebook codebook(){var card=media.getStackInSlot(2);return card.is(PRSCraft.CODEBOOK_CARD.get())?CodebookCardItem.read(card):null;}
    private MessageSettings message=MessageSettings.DEFAULT;
    public MessageSettings message(){return message;}
    public void validateMessage(MessageSettings value){
        if(kind().equals("message_receiver")&&!java.util.Set.of("raw","decode","crc_decode","hamming_decode").contains(value.operation())||kind().equals("message_source")&&!value.operation().equals("raw")||kind().equals("message_codec")&&!java.util.Set.of("raw","rle","huffman","decode","custom_encode","custom_decode").contains(value.operation())||kind().equals("channel_coder")&&!java.util.Set.of("raw","crc_encode","crc_decode","hamming_encode","hamming_decode").contains(value.operation()))throw new IllegalArgumentException("data_settings");
    }
    public void configureMessage(MessageSettings value){validateMessage(value);message=value;setChanged();}
    private GraySettings gray=GraySettings.DEFAULT;
    public GraySettings gray(){return gray;}
    public void configureGray(GraySettings value){gray=value;setChanged();}
    private DspSettings dsp=DspSettings.DEFAULT;
    public DspSettings dsp(){return dsp;}
    public void configureDsp(DspSettings value){dsp=value;setChanged();}
    private ProcessorSettings processor=ProcessorSettings.DEFAULT;
    private boolean correlationView;
    public boolean correlationView(){return correlationView;}
    public void correlationView(boolean value){correlationView=value;setChanged();}
    private int graphLayers=15,graphTerms=255;
    public final net.neoforged.neoforge.items.ItemStackHandler media=new net.neoforged.neoforge.items.ItemStackHandler(3){
        @Override public int getSlotLimit(int slot){return 1;}
        @Override public boolean isItemValid(int slot,net.minecraft.world.item.ItemStack stack){
            return slot==2?stack.is(PRSCraft.CODEBOOK_CARD.get()):slot==0?(stack.is(net.minecraft.world.item.Items.PAPER)||stack.is(PRSCraft.TEMPLATE.get())):stack.is(PRSCraft.DATA_CARD.get());
        }
        @Override protected void onContentsChanged(int slot){
            setChanged();
            if(slot==0 && level!=null && !level.isClientSide) applyTemplate();
        }
    };
    public String kind(){return getBlockState().getBlock() instanceof ProcessorBlock block?block.kind:getBlockState().getBlock() instanceof BitGeneratorBlock?"bit_generator":"radio";}
    public boolean isSource(){return kind().equals("radio")||kind().equals("bit_generator")||kind().equals("message_source")||kind().equals("message_receiver");}
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
            DspSettings nextDsp=tag.contains("dsp")?DspSettings.read(tag.getCompound("dsp")):DspSettings.DEFAULT;
            GraySettings nextGray=tag.contains("gray")?GraySettings.read(tag.getCompound("gray")):GraySettings.DEFAULT;
            PlotSettings nextPlot=tag.contains("plot")?PlotSettings.read(tag.getCompound("plot")):PlotSettings.DEFAULT;
            MessageSettings nextMessage=tag.contains("message")?MessageSettings.read(tag.getCompound("message")):message;
            validateMessage(nextMessage);validateRadio(next);configureMessage(nextMessage);configureGray(nextGray);configure(next);configureProcessor(nextProcessor);configureDsp(nextDsp);configurePlot(nextPlot);correlationView(tag.getBoolean("correlationView"));graph(tag.getInt("graphLayers"),tag.getInt("graphTerms"));return true;
        }catch(IllegalArgumentException ignored){return false;}
    }
    public RadioBlockEntity(BlockPos pos, BlockState state) {
        super(PRSCraft.RADIO_ENTITY.get(), pos, state);
        if(java.util.Set.of("message_source","message_codec","channel_coder","message_receiver").contains(kind())){var t=settings.save();t.putBoolean("thermal",false);t.putDouble("noiseRms",0);t.put("lab",LabSettings.defaults(0).save());settings=RadioSettings.read(t);}
        if(kind().equals("message_codec"))message=MessageSettings.DEFAULT.operation("rle");
        if(kind().equals("channel_coder"))message=MessageSettings.DEFAULT.operation("hamming_encode");
        if(kind().equals("message_receiver")){var t=settings.save();t.putBoolean("transmit",false);t.putBoolean("thermal",false);t.putDouble("noiseRms",0);t.put("lab",LabSettings.defaults(0).save());settings=RadioSettings.read(t);}
        if(state.getBlock() instanceof BitGeneratorBlock){
            var lab=LabSettings.defaults(.02);
            settings=new RadioSettings("sin(10)",100,16,true,true,true,.02,100,20,true,.5,"bits","10110010",10,
                new LabSettings(false,true,.5,-1,1,lab.samples(),42,0,0,.0004,false,"relative",.1));
        }
    }
    public RadioSettings settings() { return settings; }
    public void validateRadio(RadioSettings settings) {
        if(kind().equals("bit_generator")&&(!settings.transmit()||!settings.signalMode().equals("bits")))throw new IllegalArgumentException("generator_mode");
        if(kind().equals("message_source")&&!settings.transmit()||kind().equals("message_receiver")&&settings.transmit())throw new IllegalArgumentException("data_role");
    }
    public void configure(RadioSettings settings){validateRadio(settings);this.settings = settings; setChanged();
        if (level != null) level.setBlock(worldPosition, getBlockState().setValue(TransmitterBlock.POWERED, settings.enabled()), 3);
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries); tag.put("radio", settings.save());tag.put("message",message.save());tag.put("plot",plot.save());
        tag.put("gray",gray.save());tag.putBoolean("correlationView",correlationView);tag.put("dsp",dsp.save());tag.put("processor",processor.save());tag.putInt("graphLayers",graphLayers);tag.putInt("graphTerms",graphTerms);
        tag.put("media",media.serializeNBT(registries));
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if(tag.contains("gray"))try{gray=GraySettings.read(tag.getCompound("gray"));}catch(IllegalArgumentException ignored){gray=GraySettings.DEFAULT;}
        if(tag.contains("message"))try{configureMessage(MessageSettings.read(tag.getCompound("message")));}catch(IllegalArgumentException ignored){}
        if(tag.contains("plot"))try{plot=PlotSettings.read(tag.getCompound("plot"));}catch(IllegalArgumentException ignored){plot=PlotSettings.DEFAULT;}
        correlationView=tag.getBoolean("correlationView");
        if(tag.contains("dsp"))try{dsp=DspSettings.read(tag.getCompound("dsp"));}catch(IllegalArgumentException ignored){dsp=DspSettings.DEFAULT;}
        if(tag.contains("processor"))try{processor=ProcessorSettings.read(tag.getCompound("processor"));}catch(IllegalArgumentException ignored){processor=ProcessorSettings.DEFAULT;}
        if(tag.contains("graphLayers")){graphLayers=tag.getInt("graphLayers")&31;graphTerms=tag.getInt("graphTerms")&255;}
        if(tag.contains("media")){var inventory=tag.getCompound("media").copy();inventory.putInt("Size",3);media.deserializeNBT(registries,inventory);}
        if (tag.contains("radio")) {
            try { var next=RadioSettings.read(tag.getCompound("radio"));
                if(kind().equals("bit_generator")&&(!next.transmit()||!next.signalMode().equals("bits")))throw new IllegalArgumentException("generator_mode");
                if(kind().equals("message_source")&&!next.transmit()||kind().equals("message_receiver")&&next.transmit())throw new IllegalArgumentException("data_role");
                settings=next;
            }
            catch (IllegalArgumentException ignored) { /* Keep valid constructor defaults. */ }
        }
    }
}
