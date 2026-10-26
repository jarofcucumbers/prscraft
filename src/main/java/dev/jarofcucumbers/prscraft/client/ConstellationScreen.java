package dev.jarofcucumbers.prscraft.client;
import dev.jarofcucumbers.prscraft.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
public final class ConstellationScreen extends PanelScreen{
    private final Screen parent;private final CompoundTag data;private double range=1.5;public ConstellationScreen(Screen parent,CompoundTag data){super(Component.translatable("screen.prscraft.constellation"),760,570);this.parent=parent;this.data=data;}
    @Override protected void build(){button(14,526,200,tr("back"),b->onClose());button(238,526,170,Component.literal("×2"),b->range=Math.max(.05,range/2));button(432,526,170,Component.literal("÷2"),b->range=Math.min(100,range*2));}
    @Override protected void draw(GuiGraphics g){int left=170,top=68,size=420;g.fill(left,top,left+size,top+size,0xff0a1420);g.fill(left+size/2,top,left+size/2+1,top+size,0xff395a70);g.fill(left,top+size/2,left+size,top+size/2+1,0xff395a70);boolean qpsk=data.getString("iqMode").equals("qpsk");for(int a=0;a<(qpsk?2:4);a++)for(int b=0;b<(qpsk?2:4);b++){double i=qpsk?(a==0?-1:1)/Math.sqrt(2):new double[]{-3,-1,3,1}[a]/Math.sqrt(10),q=qpsk?(b==0?-1:1)/Math.sqrt(2):new double[]{-3,-1,3,1}[b]/Math.sqrt(10);int x=left+size/2+(int)(i/range*size/2),y=top+size/2-(int)(q/range*size/2);if(x<left||x>left+size||y<top||y>top+size)continue;g.fill(x-4,y-4,x+5,y+5,0xffd8b775);String label=qpsk?""+a+b:String.format(java.util.Locale.ROOT,"%2s%2s",Integer.toBinaryString(a),Integer.toBinaryString(b)).replace(' ','0');g.drawString(font,label,x+7,y-13,0xffe8d3ac,false);}double[] is=SignalChain.unpack(data.getIntArray("iqI")),qs=SignalChain.unpack(data.getIntArray("iqQ"));for(int k=0;k<Math.min(is.length,qs.length);k++){int x=left+size/2+(int)(is[k]/range*size/2),y=top+size/2-(int)(qs[k]/range*size/2);if(x>=left&&x<left+size&&y>=top&&y<top+size)g.fill(x-1,y-1,x+2,y+2,0xff63e6b1);}g.drawString(font,"I",left+size+9,top+size/2,0xffc7e0df,false);g.drawString(font,"Q",left+size/2,top-16,0xffc7e0df,false);g.drawString(font,data.getString("iqMode")+" · Rs="+fmt(data.getDouble("symbolRate"))+" sym/s · range ±"+fmt(range),14,42,0xffd8e7df,false);g.drawWordWrap(font,tr("constellation_help"),14,496,732,0xffb5d2de);}
    @Override public void onClose(){minecraft.setScreen(parent);}
}
