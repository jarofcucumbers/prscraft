package dev.jarofcucumbers.prscraft.client;
import dev.jarofcucumbers.prscraft.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.neoforged.neoforge.network.PacketDistributor;
public final class AnalyzerScreen extends PanelScreen {
    private final BlockPos port;private BlockPos desired;private CompoundTag data;private int ticks;private Button spectrum,export;
    public AnalyzerScreen(BlockPos pos,CompoundTag data){super(Component.translatable("screen.prscraft.analyzer"),720,452);port=pos;desired=pos;this.data=data;}
    public BlockPos pos(){return port;}
    public void receive(CompoundTag data){this.data=data;clearWidgets();build();}
    private void refresh(){PacketDistributor.sendToServer(new RadioNetwork.Analyze(port,desired));}
    @Override protected void build(){
        ListTag devices=data.getList("devices",Tag.TAG_COMPOUND);
        for(int i=0;i<devices.size();i++){var device=devices.getCompound(i);BlockPos pos=new BlockPos(device.getInt("x"),device.getInt("y"),device.getInt("z"));
            String kind=device.getString("kind");String name=Component.translatable("block.prscraft."+(kind.equals("radio")?"transmitter":kind)).getString();var settings=RadioSettings.read(device.getCompound("settings"));
            button(14,91+i*31,250,Component.literal((settings.enabled()?"● ":"○ ")+name+" · "+(kind.equals("radio")||kind.equals("bit_generator")?(settings.transmit()?"TX":"RX"):"DSP")),b->{desired=pos;refresh();});
        }
        button(14,416,120,tr("refresh"),b->refresh());button(142,416,122,tr("book"),b->minecraft.setScreen(new HandbookScreen(this)));
        spectrum=button(282,416,200,tr("spectrum"),b->minecraft.setScreen(new SpectrumScreen(this,data.getCompound("capture"))));
        export=button(494,416,212,Component.literal("CSV"),b->RadioClient.export(data.getCompound("capture")));
    }
    @Override public void tick(){if(minecraft.player==null||minecraft.player.distanceToSqr(port.getX()+.5,port.getY()+.5,port.getZ()+.5)>64){onClose();return;}if(++ticks%20==0)refresh();}
    @Override protected void draw(GuiGraphics g){
        g.drawString(font,data.getString("medium")+" · "+data.getInt("nodes")+" nodes · "+port.toShortString(),14,42,0xffb9d9e8,false);label(g,"analyzer_readonly",14,62);
        if(data.getBoolean("truncated"))label(g,"network_truncated",14,352);
        var capture=data.getCompound("capture");int[] packed=capture.getIntArray("received");spectrum.active=export.active=packed.length==512;
        if(!capture.contains("settings")){label(g,"analyzer_empty",282,94);return;}
        var settings=RadioSettings.read(capture.getCompound("settings"));var processor=ProcessorSettings.read(capture.getCompound("processor"));
        var source=capture.contains("source")?RadioSettings.read(capture.getCompound("source")):settings;
        String[] rows={"["+data.getInt("selectedX")+", "+data.getInt("selectedY")+", "+data.getInt("selectedZ")+"]",
            TemplateItem.summary(capture.getString("kind"),settings,processor),
            "RF="+fmt(source.carrierMHz())+" MHz · TX="+fmt(source.powerDbm())+" dBm",
            "loss="+fmt(capture.getDouble("loss"))+" dB · distance="+capture.getInt("metres")+" m",
            "port: "+data.getInt("portMetres")+" m · loss="+fmt(data.getDouble("portLoss"))+" dB",
            "AWGN μ="+fmt(settings.lab().noiseMean())+" · σ²="+fmt(settings.lab().noiseVariance()),
            "P1="+fmt(source.lab().probabilityOne())+" · A0="+fmt(source.lab().amplitudeZero())+" · A1="+fmt(source.lab().amplitudeOne()),
            source.signalMode().equals("discrete")?source.lab().samples():source.signalMode().equals("bits")?(source.lab().randomBits()?"Bernoulli seed="+source.lab().seed():source.bits()):source.expression(),
            "BPSK: "+capture.getString("decoded")};
        for(int i=0;i<rows.length;i++)g.drawString(font,font.plainSubstrByWidth(rows[i],420),282,91+i*17,0xffc3d6e4,false);
        g.fill(282,242,706,383,0xff0b1420);double[] input=SignalChain.unpack(capture.getIntArray("emitted")),received=SignalChain.unpack(packed);double range=1;
        for(double[] values:new double[][]{input,received})for(double value:values)range=Math.max(range,Math.abs(value)*1.1);
        plot(g,input,range,0xffffb555);plot(g,received,range,0xff55e4af);label(g,"processor_legend",282,392);
    }
    private void plot(GuiGraphics g,double[] values,double range,int color){if(values.length!=512)return;int previous=312;for(int x=0;x<420;x++){int y=312-(int)(values[x*512/420]/range*65);if(x>0)line(g,283+x-1,previous,283+x,y,color);previous=y;}}
}
