package dev.jarofcucumbers.prscraft.client;
import dev.jarofcucumbers.prscraft.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.neoforged.neoforge.network.PacketDistributor;
public final class AnalyzerScreen extends PanelScreen {
    private final BlockPos port;private BlockPos desired;private CompoundTag data;private int ticks,page;private Button spectrum,export;
    public AnalyzerScreen(BlockPos pos,CompoundTag data){super(Component.translatable("screen.prscraft.analyzer"),720,452);port=pos;desired=pos;this.data=data;}
    public BlockPos pos(){return port;}
    public void receive(CompoundTag data){this.data=data;clearWidgets();build();}
    public void select(BlockPos pos){desired=pos;refresh();}
    private void refresh(){PacketDistributor.sendToServer(new RadioNetwork.Analyze(port,desired));}
    @Override protected void build(){
        ListTag devices=data.getList("devices",Tag.TAG_COMPOUND);
        for(int i=page*8;i<Math.min(devices.size(),page*8+8);i++){var device=devices.getCompound(i);BlockPos pos=new BlockPos(device.getInt("x"),device.getInt("y"),device.getInt("z"));
            String kind=device.getString("kind");String name=Component.translatable("block.prscraft."+(kind.equals("radio")?"transmitter":kind)).getString();var settings=RadioSettings.read(device.getCompound("settings"));
            button(14,91+(i%8)*31,250,Component.literal((settings.enabled()?"● ":"○ ")+name+" · "+(java.util.Set.of("radio","bit_generator","message_source","message_receiver").contains(kind)?(settings.transmit()?"TX":"RX"):"DSP")),b->{desired=pos;refresh();});
        }
        button(14,346,120,Component.literal("←"),b->{page=Math.max(0,page-1);clearWidgets();build();});
        button(142,346,122,Component.literal("→"),b->{page=Math.min(Math.max(0,(devices.size()-1)/8),page+1);clearWidgets();build();});
        button(14,379,250,tr("circuit_map"),b->minecraft.setScreen(new CircuitMapScreen(this,data)));
        button(14,416,120,tr("refresh"),b->refresh());button(142,416,122,tr("book"),b->minecraft.setScreen(new HandbookScreen(this)));
        button(282,240,150,tr("oscilloscope"),b->minecraft.setScreen(new PlotScreen(this,data.getCompound("capture"))));
        var messages=button(448,240,258,tr("message_lab"),b->minecraft.setScreen(new MessageScreen(this,new BlockPos(data.getInt("selectedX"),data.getInt("selectedY"),data.getInt("selectedZ")),data.getCompound("capture"))));messages.active=data.getCompound("capture").getBoolean("dataStream");
        spectrum=button(282,416,200,tr("spectrum"),b->minecraft.setScreen(new SpectrumScreen(this,data.getCompound("capture"))));
        export=button(494,416,212,Component.literal("CSV"),b->RadioClient.export(data.getCompound("capture")));
    }
    @Override public void tick(){if(minecraft.player==null||minecraft.player.distanceToSqr(port.getX()+.5,port.getY()+.5,port.getZ()+.5)>64){onClose();return;}if(++ticks%20==0)refresh();}
    @Override protected void draw(GuiGraphics g){
        g.drawString(font,font.plainSubstrByWidth(data.getInt("deviceCount")+" devices · "+data.getInt("ports")+" ports · "+data.getInt("cables")+" cables · Σwire="+fmt(data.getDouble("wireMetres"))+" m · page "+(page+1),690),14,42,0xffb9d9e8,false);label(g,"analyzer_whole",14,62);
        if(data.getBoolean("truncated"))g.drawString(font,"LIMIT: 1024 nodes / 32 devices",14,332,0xffffb055,false);
        var capture=data.getCompound("capture");export.setMessage(Component.literal(capture.getBoolean("dataStream")?"BIN / UTF-8":"CSV"));int[] packed=capture.getIntArray("received");spectrum.active=export.active=packed.length==512;
        if(!capture.contains("settings")){label(g,"analyzer_empty",282,94);return;}
        var settings=RadioSettings.read(capture.getCompound("settings"));var processor=ProcessorSettings.read(capture.getCompound("processor"));
        var source=capture.contains("source")?RadioSettings.read(capture.getCompound("source")):settings;
        String[] rows={"["+data.getInt("selectedX")+", "+data.getInt("selectedY")+", "+data.getInt("selectedZ")+"]",
            TemplateItem.summary(capture.getString("kind"),settings,processor,capture.contains("dsp")?DspSettings.read(capture.getCompound("dsp")):DspSettings.DEFAULT),
            "RF="+fmt(source.carrierMHz())+" MHz · TX="+fmt(source.powerDbm())+" dBm",
            "loss="+fmt(capture.getDouble("loss"))+" dB · distance="+fmt(capture.getDouble("metres"))+" m",
            "wires="+fmt(capture.getDouble("wireMetres"))+" m · "+capture.getInt("activeNodes")+" active",
            "AWGN μ="+fmt(settings.lab().noiseMean())+" · σ²="+fmt(settings.lab().noiseVariance()),
            "P1="+fmt(source.lab().probabilityOne())+" · A0="+fmt(source.lab().amplitudeZero())+" · A1="+fmt(source.lab().amplitudeOne()),
            source.signalMode().equals("discrete")?source.lab().samples():source.signalMode().equals("bits")?(source.lab().randomBits()?"Bernoulli seed="+source.lab().seed():source.bits()):source.expression(),
            "BPSK: "+capture.getString("decoded"),data.getString("portRoles")};
        for(int i=0;i<rows.length;i++)g.drawString(font,font.plainSubstrByWidth(rows[i],420),282,91+i*15,0xffc3d6e4,false);
        g.fill(282,270,706,383,0xff0b1420);double[] input=SignalChain.unpack(capture.getIntArray("stageInput")),received=SignalChain.unpack(packed);double range=1;
        for(double[] values:new double[][]{input,received})for(double value:values)range=Math.max(range,Math.abs(value)*1.1);
        plot(g,input,range,0xffffb555);plot(g,received,range,0xff55e4af);label(g,"processor_legend",282,392);
    }
    private void plot(GuiGraphics g,double[] values,double range,int color){if(values.length!=512)return;int previous=326;for(int x=0;x<420;x++){int y=326-(int)(values[x*512/420]/range*50);if(x>0)line(g,283+x-1,previous,283+x,y,color);previous=y;}}
}
