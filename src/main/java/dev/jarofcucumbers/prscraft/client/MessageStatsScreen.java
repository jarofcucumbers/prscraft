package dev.jarofcucumbers.prscraft.client;
import dev.jarofcucumbers.prscraft.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import java.util.*;
public final class MessageStatsScreen extends PanelScreen{
    private final Screen parent;private final CompoundTag data;private int page,stagePage;private boolean trace;
    public MessageStatsScreen(Screen parent,CompoundTag data){super(Component.translatable("screen.prscraft.message_statistics"),800,550);this.parent=parent;this.data=data;}
    @Override protected void build(){button(14,514,140,tr("back"),b->onClose());button(168,514,250,tr(trace?"message_distribution":"message_stages"),b->{trace=!trace;b.setMessage(tr(trace?"message_distribution":"message_stages"));});button(560,514,100,Component.literal("←"),b->{if(trace)stagePage=Math.max(0,stagePage-1);else page=Math.max(0,page-1);});button(674,514,110,Component.literal("→"),b->{if(trace)stagePage=Math.min(Math.max(0,(data.getList("messageStages",10).size()-1)/6),stagePage+1);else page=Math.min(15,page+1);});}
    @Override public void onClose(){minecraft.setScreen(parent);}
    @Override protected void draw(GuiGraphics g){
        byte[] bytes=data.getByteArray("messageOriginal");g.drawString(font,"source: "+bytes.length+" B · H="+fmt(SourceCompression.entropy(bytes))+" bit/byte",14,44,0xffc7e5d7,false);
        if(trace){var stages=data.getList("messageStages",10);for(int i=stagePage*6;i<Math.min(stages.size(),stagePage*6+6);i++){var t=stages.getCompound(i);int y=84+(i%6)*66;g.drawString(font,(i+1)+". "+t.getString("kind")+" / "+t.getString("operation"),14,y,0xfff0ce88,false);g.drawString(font,t.getInt("inputBits")+" → "+t.getInt("outputBits")+" bit · header="+t.getInt("headerBytes")+" B · payload="+t.getInt("payloadBits")+" bit",14,y+17,0xffc7dce9,false);g.drawString(font,"H="+fmt(t.getDouble("entropy"))+(t.getString("entropyUnit").equals("symbol")?" bit/symbol":" bit/byte")+" · avg.code="+fmt(t.getDouble("averageCodeLength"))+" · input errors="+t.getInt("inputBitErrors")+" · corrected="+t.getInt("correctedWords")+" · CRC="+(t.getBoolean("crcValid")?"OK":"—"),14,y+34,0xff9edab9,false);}return;}
        label(g,"message_histogram_hint",14,66);int[] counts=SourceCompression.frequencies(bytes);String[] codes=SourceCompression.codeWords(bytes);g.drawString(font,"hex / ASCII     count          P(byte)           Huffman (analysis)",14,98,0xfff0ce88,false);
        for(int row=0;row<16;row++){int symbol=page*16+row,y=125+row*22;String glyph=symbol>=32&&symbol<127?Character.toString(symbol):"·";g.drawString(font,String.format(java.util.Locale.ROOT,"%02X / %s",symbol,glyph),14,y,0xffc7dce9,false);g.drawString(font,Integer.toString(counts[symbol]),152,y,0xffc7dce9,false);g.drawString(font,fmt(bytes.length==0?0:(double)counts[symbol]/bytes.length),268,y,0xffc7dce9,false);g.drawString(font,codes[symbol]==null?"—":font.plainSubstrByWidth(codes[symbol],350),420,y,0xff9edab9,false);}
    }
}
