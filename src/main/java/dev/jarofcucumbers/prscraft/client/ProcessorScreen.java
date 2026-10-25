package dev.jarofcucumbers.prscraft.client;

import dev.jarofcucumbers.prscraft.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public final class ProcessorScreen extends Screen {
    private static final int W=480,H=360;
    private final BlockPos pos;private final String kind;
    private CompoundTag snapshot;private RadioSettings radio;private ProcessorSettings saved;
    private EditBox gain,low,high,oscillator,symbol,depth;
    private boolean enabled;private String modulation;private String error="";private int ticks;
    private float scale;private int ox,oy;
    public ProcessorScreen(BlockPos pos,CompoundTag data){
        super(Component.translatable("block.prscraft."+data.getString("kind")));this.pos=pos;kind=data.getString("kind");
        snapshot=data;radio=RadioSettings.read(data.getCompound("settings"));saved=ProcessorSettings.read(data.getCompound("processor"));enabled=radio.enabled();modulation=saved.modulation();
    }
    public BlockPos pos(){return pos;}
    public void receive(CompoundTag data){snapshot=data;radio=RadioSettings.read(data.getCompound("settings"));saved=ProcessorSettings.read(data.getCompound("processor"));error=data.getString("error");}
    private Component tr(String key){return Component.translatable("screen.prscraft."+key);}
    private EditBox field(int x,int y,int width,double value,EditBox previous,String key){
        var box=new EditBox(font,x,y,width,20,tr(key));box.setMaxLength(16);box.setValue(previous==null?Double.toString(value):previous.getValue());return addRenderableWidget(box);
    }
    private void button(int x,int y,int width,Component label,Button.OnPress press){addRenderableWidget(new ScaledButton(x,y,width,label,press));}
    @Override protected void init(){
        scale=Math.min((width-8f)/W,(height-8f)/H);ox=(int)((width-W*scale)/2);oy=(int)((height-H*scale)/2);
        button(234,4,110,tr("spectrum"),b->minecraft.setScreen(new SpectrumScreen(this,snapshot)));
        button(352,4,110,tr("book"),b->minecraft.setScreen(new HandbookScreen(this)));
        gain=field(16,56,130,saved.gainDb(),gain,"gain");low=field(16,56,130,saved.lowHz(),low,"low");high=field(160,56,130,saved.highHz(),high,"high");
        oscillator=field(16,56,130,saved.oscillatorHz(),oscillator,"oscillator");symbol=field(160,56,130,saved.symbolHz(),symbol,"bit_rate");depth=field(304,56,150,saved.depth(),depth,"depth");
        gain.visible=kind.equals("amplifier");low.visible=high.visible=kind.equals("frequency_filter");oscillator.visible=symbol.visible=depth.visible=kind.equals("modulator");
        button(16,100,130,Component.literal(enabled?"ON":"OFF"),b->{enabled=!enabled;b.setMessage(Component.literal(enabled?"ON":"OFF"));});
        if(kind.equals("modulator"))button(160,100,130,Component.literal(modulation.toUpperCase()),b->{modulation=modulation.equals("bpsk")?"am":"bpsk";b.setMessage(Component.literal(modulation.toUpperCase()));});
        button(16,137,210,tr("apply"),b->save());button(240,137,214,tr("media"),b->{var tag=new CompoundTag();tag.putString("action","media");PacketDistributor.sendToServer(new RadioNetwork.Request(pos,false,tag));});
    }
    private static double number(EditBox box){return Double.parseDouble(box.getValue().replace(',','.'));}
    private void save(){try{
        var next=new ProcessorSettings(gain.visible?number(gain):saved.gainDb(),low.visible?number(low):saved.lowHz(),high.visible?number(high):saved.highHz(),modulation,
            oscillator.visible?number(oscillator):saved.oscillatorHz(),symbol.visible?number(symbol):saved.symbolHz(),depth.visible?number(depth):saved.depth(),saved.demodulate());
        var data=radio.save();data.putBoolean("enabled",enabled);RadioSettings.read(data);
        data.put("processor",next.save());error="";PacketDistributor.sendToServer(new RadioNetwork.Request(pos,true,data));
    }catch(NumberFormatException e){error="number";}catch(IllegalArgumentException e){error=e.getMessage();}}
    @Override public void tick(){
        if(minecraft.player==null||minecraft.level==null||minecraft.player.distanceToSqr(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5)>64||!(minecraft.level.getBlockState(pos).getBlock() instanceof ProcessorBlock)){onClose();return;}
        if(++ticks%20==0)PacketDistributor.sendToServer(new RadioNetwork.Request(pos,false,new CompoundTag()));
    }
    @Override public void renderBackground(GuiGraphics g,int x,int y,float partial){}
    @Override public void render(GuiGraphics g,int mx,int my,float partial){
        g.fill(0,0,width,height,0xb0101520);g.pose().pushPose();g.pose().translate(ox,oy,0);g.pose().scale(scale,scale,1);
        g.fill(0,0,W,H,0xff141e2b);g.fill(0,0,W,28,0xff253b50);g.drawString(font,title,16,10,0xffffffff,false);
        if(gain.visible)g.drawString(font,tr("gain"),16,42,0xffc3d6e4,false);
        if(low.visible){g.drawString(font,tr("low"),16,42,0xffc3d6e4,false);g.drawString(font,tr("high"),160,42,0xffc3d6e4,false);}
        if(oscillator.visible){g.drawString(font,tr("oscillator"),16,42,0xffc3d6e4,false);g.drawString(font,tr("bit_rate"),160,42,0xffc3d6e4,false);g.drawString(font,tr("depth"),304,42,0xffc3d6e4,false);}
        if(snapshot.contains("source"))g.drawString(font,"RF "+snapshot.getCompound("source").getDouble("carrierMHz")+" MHz",304,106,0xffc3d6e4,false);
        if(!error.isEmpty())g.drawString(font,Component.translatable("error.prscraft."+error),16,167,0xffff6b81,false);
        else g.drawString(font,snapshot.getString("fault").isEmpty()?tr("direction"):Component.translatable("fault.prscraft."+snapshot.getString("fault")),16,167,0xffa9c9dd,false);
        g.fill(16,188,454,314,0xff0a111c);g.fill(16,251,454,252,0xff344759);
        double[] input=SignalChain.unpack(snapshot.getIntArray("stageInput")),output=SignalChain.unpack(snapshot.getIntArray("emitted"));double range=1;
        for(double[] values:new double[][]{input,output})for(double v:values)range=Math.max(range,Math.abs(v)*1.1);
        draw(g,input,range,0xfff9ca24);draw(g,output,range,0xff44ef94);
        g.drawString(font,tr(snapshot.getBoolean("connected")?"processor_legend":"no_link"),16,326,0xffc3d6e4,false);
        super.render(g,(int)((mx-ox)/scale),(int)((my-oy)/scale),partial);g.pose().popPose();
    }
    private void draw(GuiGraphics g,double[] values,double range,int color){if(values.length!=512)return;int old=251;for(int x=0;x<438;x++){
        int y=251-(int)(Math.max(-1,Math.min(1,values[x*512/438]/range))*60);if(x>0)g.fill(16+x,Math.min(old,y),17+x,Math.max(old,y)+1,color);old=y;
    }}
    @Override public boolean mouseClicked(double x,double y,int button){return super.mouseClicked((x-ox)/scale,(y-oy)/scale,button);}
    @Override public boolean mouseReleased(double x,double y,int button){return super.mouseReleased((x-ox)/scale,(y-oy)/scale,button);}
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy){return super.mouseDragged((x-ox)/scale,(y-oy)/scale,button,dx/scale,dy/scale);}
    @Override public boolean isPauseScreen(){return false;}
}
