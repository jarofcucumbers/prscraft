package dev.jarofcucumbers.prscraft.client;
import dev.jarofcucumbers.prscraft.*;
import net.minecraft.core.BlockPos;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
/** A shared, expanded oscilloscope for every instrument. Coordinates remain in seconds and amplitude units. */
public final class PlotScreen extends PanelScreen{
    private final Screen parent;private final BlockPos pos;private final boolean readOnly;private CompoundTag data;private PlotSettings view;private EditBox x0,x1,y0,y1;private String error="";private int ticks;
    private static final String[] KEYS={"original","sampled","sourceOutput","stageInput","inputB","emitted","received","correlation"};
    private static final int[] COLORS={0xffdfdfdf,0xffeccb61,0xffdb8fec,0xffeba862,0xff6ecce7,0xffcf93e6,0xff61e7a6,0xff94b4f2};
    public PlotScreen(Screen parent,CompoundTag data){super(Component.translatable("screen.prscraft.oscilloscope"),840,550);this.parent=parent;this.data=data;pos=BlockPos.of(data.getLong("device"));readOnly=data.getBoolean("recorded")||data.getBoolean("analysis")||parent instanceof AnalyzerScreen;view=data.contains("plot")?PlotSettings.read(data.getCompound("plot")):PlotSettings.DEFAULT;}
    public BlockPos pos(){return pos;}public void receive(CompoundTag next){if(!readOnly)data=next;}
    private boolean correlation(){return view.layers()==128;}
    private double[] bounds(){double span=Math.max(1e-12,data.getDouble(correlation()?"lagSpan":"span")),start=correlation()?-span/2:0;double max=.01;for(int k=0;k<8;k++)if((view.layers()&(1<<k))!=0)for(double value:SignalChain.unpack(data.getIntArray(KEYS[k])))if(Double.isFinite(value))max=Math.max(max,Math.abs(value)*1.1);return new double[]{view.autoX()?start:view.xMin(),view.autoX()?start+span:view.xMax(),view.autoY()?-max:view.yMin(),view.autoY()?max:view.yMax()};}
    private void changed(PlotSettings next){view=next;error="";if(!readOnly){var tag=new CompoundTag();tag.putString("action","view");tag.put("plot",view.save());RadioClient.request(new RadioNetwork.Request(pos,false,tag));}clearWidgets();build();}
    private void zoom(boolean x,double factor){double[] b=bounds();double center=x?(b[0]+b[1])/2:(b[2]+b[3])/2,half=(x?b[1]-b[0]:b[3]-b[2])*factor/2;try{changed(new PlotSettings(x?false:view.autoX(),x?view.autoY():false,x?center-half:view.xMin(),x?center+half:view.xMax(),x?view.yMin():center-half,x?view.yMax():center+half,view.layers()));}catch(IllegalArgumentException ignored){}}
    @Override protected void build(){double[] b=bounds();x0=field(14,58,150,Double.toString(b[0]),24,"plot_xmin");x1=field(176,58,150,Double.toString(b[1]),24,"plot_xmax");y0=field(338,58,150,Double.toString(b[2]),24,"plot_ymin");y1=field(500,58,150,Double.toString(b[3]),24,"plot_ymax");button(662,58,164,tr("apply"),w->{try{changed(new PlotSettings(false,false,number(x0),number(x1),number(y0),number(y1),view.layers()));}catch(NumberFormatException e){error="number";}catch(IllegalArgumentException e){error=e.getMessage();}});
        button(14,92,110,Component.literal("X ×2"),w->zoom(true,.5));button(134,92,110,Component.literal("X ÷2"),w->zoom(true,2));button(254,92,110,Component.literal("Y ×2"),w->zoom(false,.5));button(374,92,110,Component.literal("Y ÷2"),w->zoom(false,2));button(494,92,160,tr("plot_auto"),w->changed(new PlotSettings(true,true,0,1,-1,1,view.layers())));button(666,92,160,tr("back"),w->onClose());
        for(int i=0;i<8;i++){final int layer=i;var button=button(14+i*102,510,96,Component.literal(KEYS[i]).withColor(COLORS[i]&0xffffff),w->{int mask=iMask(layer);changed(new PlotSettings(layer==7||correlation()?true:view.autoX(),view.autoY(),view.xMin(),view.xMax(),view.yMin(),view.yMax(),mask));});button.active=data.getIntArray(KEYS[i]).length==512;}
    }
    private int iMask(int layer){if(layer==7)return correlation()?72:128;int mask=correlation()?0:view.layers();return mask^(1<<layer);}
    @Override public void tick(){if(readOnly)return;if(minecraft.player==null||!RadioClient.inRange(pos)){onClose();return;}if(++ticks%20==0)RadioClient.request(new RadioNetwork.Request(pos,false,new CompoundTag()));}
    @Override public void onClose(){RadioClient.closeDevice(parent);}
    @Override protected void draw(GuiGraphics g){String[] keys={"plot_xmin","plot_xmax","plot_ymin","plot_ymax"};for(int i=0;i<4;i++)label(g,keys[i],14+i*162,44);
        if(!error.isEmpty())g.drawString(font,Component.translatable("error.prscraft."+error),14,122,0xffffab7c,false);else label(g,"plot_hint",14,122);
        int left=72,top=150,w=728,h=310;double[] b=bounds();g.fill(left,top,left+w,top+h,0xff0a1420);
        for(int i=0;i<=4;i++){int x=left+i*w/4,y=top+i*h/4;g.fill(x,top,x+1,top+h,0xff2b4155);g.fill(left,y,left+w,y+1,0xff2b4155);g.drawString(font,fmt(b[0]+(b[1]-b[0])*i/4)+" s",x-18,top+h+7,0xffb9cfdf,false);g.drawString(font,fmt(b[3]-(b[3]-b[2])*i/4),2,y-3,0xffb9cfdf,false);}
        double span=Math.max(1e-12,data.getDouble(correlation()?"lagSpan":"span")),base=correlation()?-span/2:0;
        for(int k=0;k<8;k++)if((view.layers()&(1<<k))!=0){double[] values=SignalChain.unpack(data.getIntArray(KEYS[k]));if(values.length!=512)continue;int previous=0;boolean drawn=false;for(int x=0;x<w;x++){double t=b[0]+(b[1]-b[0])*x/(w-1),index=(t-base)/span*512;if(index<0||index>511){drawn=false;continue;}int at=(int)index,next=Math.min(511,at+1);double value=values[at]+(values[next]-values[at])*(index-at);int y=top+h-(int)(Math.max(0,Math.min(1,(value-b[2])/(b[3]-b[2])))*h);if(drawn)line(g,left+x-1,previous,left+x,y,COLORS[k]);previous=y;drawn=true;}}
        g.drawString(font,"units: "+data.getString("units")+" · "+(readOnly?"local view":"saved view"),14,488,0xffb9cfdf,false);
    }
}
