package dev.jarofcucumbers.prscraft.client;
import dev.jarofcucumbers.prscraft.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
public final class HandbookScreen extends PanelScreen {
    private final Screen parent;private int page;private boolean contents=true;
    public HandbookScreen(Screen parent){super(Component.translatable("item.prscraft.handbook"),720,452);this.parent=parent;}
    private void refresh(){clearWidgets();build();}
    @Override protected void build(){
        button(14,418,110,tr("back"),b->onClose());button(132,418,150,tr("contents"),b->{contents=!contents;refresh();});
        if(contents){for(int i=0;i<HandbookContent.PAGES.size();i++){final int index=i;var p=HandbookContent.PAGES.get(i);button(14+(i/8)*234,68+(i%8)*40,220,Component.translatable("book.prscraft."+p.id()+".title"),b->{page=index;contents=false;refresh();});}}
        else {button(460,418,110,Component.literal("←"),b->{page=Math.floorMod(page-1,HandbookContent.PAGES.size());});button(582,418,110,Component.literal("→"),b->{page=(page+1)%HandbookContent.PAGES.size();});}
    }
    @Override public void onClose(){minecraft.setScreen(parent);}
    @Override protected void draw(GuiGraphics g){
        g.drawString(font,"PRSCraft "+HandbookContent.VERSION,520,10,0xff9bd7ec,false);
        if(contents){label(g,"book_intro",18,42);return;}
        var content=HandbookContent.PAGES.get(page);
        g.drawString(font,Component.translatable("book.prscraft."+content.id()+".title"),18,45,0xfff2d494,false);
        g.drawWordWrap(font,Component.translatable("book.prscraft."+content.id()+".body"),18,78,320,0xffd5e1ed);
        g.fill(356,74,702,300,0xff0b1420);diagram(g,content.diagram());
        int y=322;for(String formula:content.formulas()){g.drawString(font,font.plainSubstrByWidth(formula,680),18,y,0xff92e8c1,false);y+=21;}
        g.drawString(font,(page+1)+" / "+HandbookContent.PAGES.size(),330,424,0xffbacbdb,false);
    }
    private void diagram(GuiGraphics g,String kind){
        if(kind.equals("circuit")){
            int[][] nodes={{385,115},{510,115},{635,115},{385,235},{510,235},{635,235}};
            for(int[] edge:new int[][]{{0,1},{1,2},{2,5},{5,4},{4,3},{3,0},{1,4}})line(g,nodes[edge[0]][0]+15,nodes[edge[0]][1]+10,nodes[edge[1]][0]+15,nodes[edge[1]][1]+10,0xff4faecd);
            for(int i=0;i<nodes.length;i++){g.fill(nodes[i][0],nodes[i][1],nodes[i][0]+35,nodes[i][1]+25,0xff365669);g.drawString(font,new String[]{"TX","SUM","RX","DLY","COR","RX"}[i],nodes[i][0]+3,nodes[i][1]+8,0xffd5eef5,false);}return;
        }
        if(kind.equals("cable")){
            g.fill(380,170,680,185,0xff31bfc6);g.fill(500,170,530,185,0xffba5c4d);g.drawString(font,"L = 0.25 m",402,121,0xff9fe5c4,false);g.drawString(font,"damage = 20% → loss + noise",378,236,0xffffc783,false);return;
        }
        if(kind.equals("correlation")){
            line(g,380,250,685,250,0xff6c92a8);line(g,530,100,530,265,0xff3b5d73);
            int old=250;for(int x=0;x<300;x++){int y=250-(int)(130*Math.exp(-Math.pow((x-190)/35.0,2)));if(x>0)line(g,380+x-1,old,380+x,y,0xff73e4b1);old=y;}
            line(g,570,105,570,250,0xfff9c15a);g.drawString(font,"R(τ)",388,100,0xffd5eef5,false);g.drawString(font,"0      +τpeak",516,278,0xfff9c15a,false);return;
        }
        if(kind.equals("gate")){
            line(g,380,245,530,245,0xff73e4b1);line(g,530,245,530,130,0xff73e4b1);line(g,530,130,680,130,0xff73e4b1);g.drawString(font,"t = T",515,270,0xfff9c15a,false);return;
        }
        if(kind.equals("math")){
            g.drawString(font,"A + B → SUM → ∫dt",390,112,0xfff9c15a,false);line(g,385,250,680,250,0xff53738a);line(g,385,250,680,140,0xff73e4b1);g.drawString(font,"constant input → ramp",388,278,0xffd5eef5,false);return;
        }
        if(kind.equals("achievements")){
            var icons=new net.minecraft.world.item.Item[]{PRSCraft.HANDBOOK.get(),PRSCraft.WRENCH.get(),PRSCraft.CORRELATOR.get().asItem()};for(int i=0;i<3;i++){g.pose().pushPose();g.pose().translate(395+i*110,150,0);g.pose().scale(3,3,1);g.renderItem(new ItemStack(icons[i]),0,0);g.pose().popPose();}g.drawString(font,"READ → CALIBRATE → CORRELATE",377,240,0xfff9c15a,false);return;
        }
        if(kind.equals("chain")){
            var items=new net.minecraft.world.item.Item[]{PRSCraft.TRANSMITTER.get().asItem(),PRSCraft.AMPLIFIER.get().asItem(),PRSCraft.FILTER.get().asItem(),PRSCraft.MODULATOR.get().asItem(),PRSCraft.TRANSMITTER.get().asItem()};
            String[] labels={"TX","AMP","FILTER","MOD","RX"};
            for(int i=0;i<items.length;i++){int x=375+i*63;g.pose().pushPose();g.pose().translate(x,145,0);g.pose().scale(2,2,1);g.renderItem(new ItemStack(items[i]),0,0);g.pose().popPose();g.drawString(font,labels[i],x,196,0xffb9d9e8,false);if(i<4){line(g,x+35,162,x+56,162,0xff61dbad);line(g,x+52,158,x+56,162,0xff61dbad);line(g,x+52,166,x+56,162,0xff61dbad);}}
            g.drawString(font,"ports + matching cable",400,246,0xfff2d494,false);return;
        }
        if(kind.equals("presets")){
            for(int i=0;i<3;i++){int x=390+i*100;g.fill(x,120,x+55,204,0xffeee1be);g.fill(x,120,x+55,137,new int[]{0xff31b66a,0xff36a6c4,0xffcf8b39}[i]);g.drawString(font,new String[]{"TX","RX","AMP"}[i],x+10,147,0xff1b3546,false);g.drawString(font,new String[]{"100 MHz","100 MHz","+6 dB"}[i],x+2,180,0xff1b3546,false);}return;
        }
        if(kind.equals("spectrum")){
            line(g,380,265,680,265,0xff8dabc1);line(g,380,95,380,265,0xff8dabc1);
            int[] positions={420,500,600};int[] heights={125,80,40};for(int i=0;i<3;i++)g.fill(positions[i],265-heights[i],positions[i]+14,265,0xff54e0ab);
            g.drawString(font,"PSD [W/Hz] / ESD [J/Hz]",390,91,0xffb9d9e8,false);g.drawString(font,"f [Hz]",633,278,0xffb9d9e8,false);return;
        }
        line(g,374,210,687,210,0xff3f556d);
        int previous=0;
        for(int x=0;x<300;x++){
            double t=x/299.0,value;
            switch(kind){
                case "gaussian","awgn" -> {double z=t*8-4;value=Math.exp(-z*z/2)*1.5;}
                case "bits" -> value=((int)(t*8)%3==0)?1:-1;
                case "bpsk" -> value=((int)(t*8)%3==0?1:-1)*Math.cos(2*Math.PI*32*t);
                case "quant" -> value=Math.round(Math.sin(2*Math.PI*2*t)*3)/3.0;
                case "sampling" -> value=(t*16-Math.floor(t*16))<.55?Math.sin(2*Math.PI*2*t):0;
                default -> value=Math.sin(2*Math.PI*2*t)+.3*Math.cos(2*Math.PI*4*t);
            }
            int y=210-(int)(value*65);if(x>0)line(g,380+x-1,previous,380+x,y,0xff6ce1b6);previous=y;
            if(kind.equals("gaussian")&&t>.67)g.fill(380+x,y,381+x,210,0x665f95f0);
        }
        g.drawString(font,kind.equals("gaussian")?"Q(x) = tail probability":kind,386,104,0xfff2d494,false);
    }
}
