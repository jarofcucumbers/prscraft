package dev.jarofcucumbers.prscraft.client;
import dev.jarofcucumbers.prscraft.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
public final class GrayScreen extends PanelScreen{
    private final BlockPos pos;private CompoundTag data;private final boolean recorded;private GraySettings saved;private String mode,error="";private boolean enabled;private int ticks;
    private EditBox width,rate,threshold,low,high,levels,minimum,maximum,sample;
    public GrayScreen(BlockPos pos,CompoundTag data){super(Component.translatable("block.prscraft.gray_converter"),720,500);this.pos=pos;this.data=data;recorded=data.getBoolean("recorded");saved=GraySettings.read(data.getCompound("gray"));mode=saved.mode();enabled=RadioSettings.read(data.getCompound("settings")).enabled();}
    public BlockPos pos(){return pos;}public void receive(CompoundTag data){if(recorded)return;this.data=data;error=data.getString("error");saved=GraySettings.read(data.getCompound("gray"));}
    private EditBox input(int x,int y,double value,EditBox previous,String key){return field(x,y,158,previous==null?(key.equals("gray_width")||key.equals("levels")?Integer.toString((int)value):fmt(value)):previous.getValue(),16,key);}
    private void visibility(){boolean level=mode.startsWith("levels_");rate.visible=threshold.visible=!level;levels.visible=minimum.visible=maximum.visible=sample.visible=level;}
    @Override protected void build(){
        button(14,42,280,tr("gray_"+mode),b->{String[] modes={"binary_to_gray","gray_to_binary","levels_to_binary","levels_to_gray"};mode=modes[(java.util.Arrays.asList(modes).indexOf(mode)+1)%4];b.setMessage(tr("gray_"+mode));visibility();});
        button(308,42,90,Component.literal(enabled?"ON":"OFF"),b->{enabled=!enabled;b.setMessage(Component.literal(enabled?"ON":"OFF"));});button(412,42,132,tr("apply"),b->save());button(558,42,148,tr("media"),b->{var t=new CompoundTag();t.putString("action","media");PacketDistributor.sendToServer(new RadioNetwork.Request(pos,false,t));});
        width=input(14,96,saved.wordBits(),width,"gray_width");rate=input(190,96,saved.bitRate(),rate,"bit_rate");threshold=input(366,96,saved.threshold(),threshold,"gray_threshold");low=input(14,142,saved.low(),low,"gray_low");high=input(190,142,saved.high(),high,"gray_high");
        levels=input(366,96,saved.levels(),levels,"levels");sample=input(542,96,saved.sampleHz(),sample,"sample");minimum=input(366,142,saved.minimum(),minimum,"gray_min");maximum=input(542,142,saved.maximum(),maximum,"gray_max");visibility();
        button(350,4,110,tr("oscilloscope"),b->minecraft.setScreen(new PlotScreen(this,data)));
        button(468,4,110,tr("spectrum"),b->minecraft.setScreen(new SpectrumScreen(this,data)));button(586,4,120,tr("book"),b->minecraft.setScreen(new HandbookScreen(this)));
        if(recorded)for(var r:renderables)if(r instanceof net.minecraft.client.gui.components.AbstractWidget w&&w.getY()>28)w.active=false;
    }
    private void save(){try{
        var next=new GraySettings(mode,Integer.parseInt(width.getValue()),rate.visible?number(rate):saved.bitRate(),threshold.visible?number(threshold):saved.threshold(),number(low),number(high),levels.visible?Integer.parseInt(levels.getValue()):saved.levels(),minimum.visible?number(minimum):saved.minimum(),maximum.visible?number(maximum):saved.maximum(),sample.visible?number(sample):saved.sampleHz());
        var tag=data.getCompound("settings").copy();tag.putBoolean("enabled",enabled);tag.put("gray",next.save());PacketDistributor.sendToServer(new RadioNetwork.Request(pos,true,tag));error="";
    }catch(NumberFormatException e){error="number";}catch(IllegalArgumentException e){error=e.getMessage();}}
    @Override public void tick(){if(recorded)return;if(minecraft.player==null||minecraft.player.distanceToSqr(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5)>64){onClose();return;}if(++ticks%20==0)PacketDistributor.sendToServer(new RadioNetwork.Request(pos,false,new CompoundTag()));}
    @Override protected void draw(GuiGraphics g){
        label(g,"gray_width",14,82);if(rate.visible)label(g,"bit_rate",190,82);if(threshold.visible)label(g,"gray_threshold",366,82);label(g,"gray_low",14,128);label(g,"gray_high",190,128);
        if(levels.visible){label(g,"levels",366,82);label(g,"sample",542,82);label(g,"gray_min",366,128);label(g,"gray_max",542,128);}
        String fault=error.isEmpty()?data.getString("fault"):error;if(!fault.isEmpty())g.drawString(font,Component.translatable((error.isEmpty()?"fault.prscraft.":"error.prscraft.")+fault),14,178,0xffffb078,false);else label(g,"gray_ports",14,178);
        g.drawWordWrap(font,Component.translatable("screen.prscraft.gray_input",data.getString("grayInput")),14,202,692,0xfff2bd68);g.drawWordWrap(font,Component.translatable("screen.prscraft.gray_output",data.getString("grayOutput")),14,240,692,0xff71edb6);
        g.fill(14,286,706,444,0xff0a1420);double[] a=SignalChain.unpack(data.getIntArray("stageInput")),b=SignalChain.unpack(data.getIntArray("received"));double range=1;for(double[] line:new double[][]{a,b})for(double v:line)range=Math.max(range,Math.abs(v)*1.1);
        curve(g,a,range,0xfff2bd68);curve(g,b,range,0xff71edb6);g.drawString(font,"0 s                                                "+fmt(data.getDouble("span"))+" s",14,451,0xffb9d9e8,false);label(g,"gray_legend",14,475);
    }
    private void curve(GuiGraphics g,double[] values,double range,int color){if(values.length!=512)return;int previous=365;for(int i=0;i<692;i++){int y=365-(int)(values[i*512/692]/range*72);if(i>0)line(g,13+i,previous,14+i,y,color);previous=y;}}
}
