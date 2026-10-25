package dev.jarofcucumbers.prscraft.client;
import dev.jarofcucumbers.prscraft.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.neoforged.neoforge.network.PacketDistributor;
public final class DspScreen extends PanelScreen{
    private final BlockPos pos;private final String kind;private final boolean recorded;private CompoundTag data;private DspSettings saved;
    private EditBox first,second;private boolean enabled,normalized,automatic,hold,lagView;private String error="";private int ticks;
    public DspScreen(BlockPos pos,CompoundTag data){super(Component.translatable("block.prscraft."+(data.getBoolean("recorded")?data.getString("captureKind"):data.getString("kind"))),720,456);
        this.pos=pos;this.data=data;recorded=data.getBoolean("recorded");kind=recorded?data.getString("captureKind"):data.getString("kind");saved=data.contains("dsp")?DspSettings.read(data.getCompound("dsp")):DspSettings.DEFAULT;
        enabled=RadioSettings.read(data.getCompound("settings")).enabled();normalized=saved.normalized();automatic=saved.autoCorrelation();hold=saved.gateHold();lagView=data.getBoolean("correlationView");
    }
    public BlockPos pos(){return pos;}public void receive(CompoundTag data){if(recorded)return;this.data=data;error=data.getString("error");saved=DspSettings.read(data.getCompound("dsp"));}
    private String firstKey(){return switch(kind){case "adder"->"gain_a";case "time_gate"->"gate_time";case "delay_line"->"delay_time";case "correlator"->"max_lag";default->"integration_end";};}
    private String secondKey(){return kind.equals("adder")?"gain_b":kind.equals("correlator")?"integration_end":"gate_width";}
    @Override protected void build(){
        double a=switch(kind){case "adder"->saved.gainA();case "time_gate"->saved.gateSeconds();case "delay_line"->saved.delaySeconds();case "correlator"->saved.lagSeconds();default->saved.endSeconds();};
        double b=kind.equals("adder")?saved.gainB():kind.equals("correlator")?saved.endSeconds():saved.gateWidth();
        first=field(14,56,210,first==null?Double.toString(a):first.getValue(),18,firstKey());second=field(238,56,210,second==null?Double.toString(b):second.getValue(),18,secondKey());second.visible=kind.equals("adder")||kind.equals("time_gate")||kind.equals("correlator");
        if(kind.equals("time_gate"))button(462,56,244,tr(hold?"gate_hold":"gate_pulse"),w->{hold=!hold;w.setMessage(tr(hold?"gate_hold":"gate_pulse"));});
        if(kind.equals("correlator")){
            button(462,56,244,tr(normalized?"corr_normalized":"corr_raw"),w->{normalized=!normalized;w.setMessage(tr(normalized?"corr_normalized":"corr_raw"));});
            button(462,90,244,tr(automatic?"corr_auto":"corr_cross"),w->{automatic=!automatic;w.setMessage(tr(automatic?"corr_auto":"corr_cross"));});
        }
        button(14,96,100,Component.literal(enabled?"ON":"OFF"),b1->{enabled=!enabled;b1.setMessage(Component.literal(enabled?"ON":"OFF"));});button(126,96,150,tr("apply"),b1->save());button(288,96,160,tr("media"),b1->{var t=new CompoundTag();t.putString("action","media");PacketDistributor.sendToServer(new RadioNetwork.Request(pos,false,t));});
        if(kind.equals("correlator"))button(14,423,210,tr("corr_view"),b1->{lagView=!lagView;if(!recorded){var t=new CompoundTag();t.putString("action","view");t.putInt("graphLayers",data.getInt("graphLayers"));t.putInt("graphTerms",data.getInt("graphTerms"));t.putBoolean("correlationView",lagView);PacketDistributor.sendToServer(new RadioNetwork.Request(pos,false,t));}});
        button(468,4,110,tr("spectrum"),b1->minecraft.setScreen(new SpectrumScreen(this,data)));button(586,4,120,tr("book"),b1->minecraft.setScreen(new HandbookScreen(this)));
        if(recorded)for(var r:renderables)if(r instanceof AbstractWidget w&&w.getY()>28&&w.getY()<400)w.active=false;
    }
    private void save(){try{
        double a=number(first),b=second.visible?number(second):0;
        var next=new DspSettings(kind.equals("integrator")?a:kind.equals("correlator")?b:saved.endSeconds(),kind.equals("time_gate")?a:saved.gateSeconds(),kind.equals("time_gate")?b:saved.gateWidth(),hold,
            kind.equals("delay_line")?a:saved.delaySeconds(),kind.equals("correlator")?a:saved.lagSeconds(),normalized,automatic,kind.equals("adder")?a:saved.gainA(),kind.equals("adder")?b:saved.gainB());
        var t=data.getCompound("settings").copy();t.putBoolean("enabled",enabled);t.put("processor",data.getCompound("processor"));t.put("dsp",next.save());PacketDistributor.sendToServer(new RadioNetwork.Request(pos,true,t));error="";
    }catch(NumberFormatException e){error="number";}catch(IllegalArgumentException e){error=e.getMessage();}}
    @Override public void tick(){if(recorded)return;if(minecraft.player==null||minecraft.player.distanceToSqr(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5)>64){onClose();return;}if(++ticks%20==0)PacketDistributor.sendToServer(new RadioNetwork.Request(pos,false,new CompoundTag()));}
    @Override protected void draw(GuiGraphics g){
        label(g,firstKey(),14,42);if(second.visible)label(g,secondKey(),238,42);label(g,"dsp_ports",14,138);
        if(!error.isEmpty())g.drawString(font,Component.translatable("error.prscraft."+error),14,158,0xffff7788,false);
        else if(!data.getString("fault").isEmpty())g.drawString(font,Component.translatable("fault.prscraft."+data.getString("fault")),14,158,0xffffb070,false);
        double[] a=SignalChain.unpack(data.getIntArray("stageInput")),b=SignalChain.unpack(data.getIntArray("inputB")),out=SignalChain.unpack(data.getIntArray(lagView?"correlation":"received"));
        int left=44,top=192,w=650,h=190;g.fill(left,top,left+w,top+h,0xff0b1420);g.fill(left,top+h/2,left+w,top+h/2+1,0xff3a4f63);
        double range=lagView?1:0.01;for(double[] values:lagView?new double[][]{out}:new double[][]{a,b,out})for(double v:values)range=Math.max(range,Math.abs(v)*1.1);
        if(!lagView){curve(g,a,range,0xffffbd55);curve(g,b,range,0xff55cfee);}curve(g,out,range,0xff55edaa);
        double span=data.getDouble(lagView?"lagSpan":"span");for(int i=0;i<=4;i++)g.drawString(font,fmt((lagView?-span/2:0)+span*i/4)+" s",left+i*w/4-12,388,0xffb9d9e8,false);
        String status=lagView?"R(τ) · peak τ="+fmt(data.getDouble("peakLag"))+" s · R="+fmt(data.getDouble("peakValue")):"A: yellow · B: cyan · OUT: green · units="+data.getString("units");g.drawString(font,status,14,405,0xffb9d9e8,false);
    }
    private void curve(GuiGraphics g,double[] values,double range,int color){if(values.length!=512)return;int previous=0;for(int i=0;i<650;i++){int y=287-(int)(values[i*512/650]/range*90);if(i>0)line(g,44+i-1,previous,44+i,y,color);previous=y;}}
}
