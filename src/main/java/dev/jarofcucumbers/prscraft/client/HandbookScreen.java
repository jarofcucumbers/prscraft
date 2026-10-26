package dev.jarofcucumbers.prscraft.client;
import dev.jarofcucumbers.prscraft.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
public final class HandbookScreen extends PanelScreen {
    private final Screen parent;
    private HandbookCurriculum.Chapter chapter;private String entry;private int contentsPage,entryPage;private static final float READING_SCALE=1.65f;private static final int READING_ROWS=23;private boolean contents,revealed;
    public HandbookScreen(Screen parent){super(Component.translatable("item.prscraft.handbook"),720,600);this.parent=parent;var bookmark=HandbookBookmarks.load();chapter=bookmark.chapter();entry=bookmark.entry();contents=bookmark.contents();contentsPage=bookmark.contentsPage();entryPage=bookmark.entryPage();}
    @Override protected net.minecraft.client.gui.components.Button button(int x,int y,int width,Component label,net.minecraft.client.gui.components.Button.OnPress press){return addRenderableWidget(new ScaledButton(x,y,width,32,1.2f,label,press));}
    private java.util.List<String> entries(){return HandbookCurriculum.entries(chapter);}
    private void remember(){HandbookBookmarks.save(new HandbookBookmark(chapter,entry,contents,contentsPage,entryPage));}
    private void refresh(){clearWidgets();build();}
    private void open(String id){entry=id;entryPage=0;contentsPage=entries().indexOf(id)/18;contents=false;revealed=false;remember();refresh();}
    private void chapter(HandbookCurriculum.Chapter next){chapter=next;entry=entries().getFirst();contents=true;contentsPage=0;entryPage=0;revealed=false;remember();refresh();}
    private java.util.List<net.minecraft.util.FormattedCharSequence> readingLines(){
        Component text=chapter==HandbookCurriculum.Chapter.TASKS?Component.translatable("advancements.prscraft."+entry+".description"):Component.translatable("book.prscraft."+entry+".body").copy().append("\n\n").append(Component.translatable("book.prscraft."+entry+".symbols"));
        if(chapter==HandbookCurriculum.Chapter.TASKS&&revealed)text=text.copy().append("\n\n").append(Component.translatable("book.task."+entry+".solution"));
        return font.split(text,414);
    }
    private record ReadingPiece(int kind,net.minecraft.util.FormattedCharSequence line,int height){}
    private java.util.List<java.util.List<ReadingPiece>> contentPages(){
        var pieces=new java.util.ArrayList<ReadingPiece>();for(var line:readingLines())pieces.add(new ReadingPiece(0,line,17));
        if(chapter==HandbookCurriculum.Chapter.TASKS){if(revealed)pieces.add(new ReadingPiece(2,null,410));}
        else{int height=196;for(String formula:HandbookCurriculum.page(entry).formulas())height+=font.split(Component.literal(formula),410).size()*15+7;pieces.add(new ReadingPiece(1,null,height));}
        var pages=new java.util.ArrayList<java.util.List<ReadingPiece>>();var current=new java.util.ArrayList<ReadingPiece>();int height=0;
        for(var piece:pieces){if(height+piece.height()>410&&!current.isEmpty()){pages.add(current);current=new java.util.ArrayList<>();height=0;}current.add(piece);height+=piece.height();}
        if(!current.isEmpty())pages.add(current);return pages;
    }
    private int pageCount(){return contentPages().size();}
    private void move(int direction){
        int next=entryPage+direction;
        if(next>=0&&next<pageCount())entryPage=next;
        else{entry=entries().get(Math.floorMod(entries().indexOf(entry)+direction,entries().size()));revealed=false;entryPage=direction<0?pageCount()-1:0;contentsPage=entries().indexOf(entry)/18;}
        remember();refresh();
    }
    @Override protected void build(){
        if(!contents)entryPage=Math.min(entryPage,pageCount()-1);
        for(var value:HandbookCurriculum.Chapter.values()){int x=14+value.ordinal()*234;var b=button(x,42,224,tr("chapter_"+value.name().toLowerCase(java.util.Locale.ROOT)),w->chapter(value));b.active=value!=chapter;}
        button(14,562,110,tr("back"),b->onClose());button(132,562,150,tr("contents"),b->{contents=true;revealed=false;remember();refresh();});
        if(contents){int total=(entries().size()+17)/18;
            button(460,562,110,Component.literal("←"),b->{contentsPage=Math.floorMod(contentsPage-1,total);remember();refresh();});button(582,562,110,Component.literal("→"),b->{contentsPage=(contentsPage+1)%total;remember();refresh();});
            for(int i=contentsPage*18;i<Math.min(entries().size(),(contentsPage+1)*18);i++){String id=entries().get(i);button(18+((i%18)/9)*350,116+(i%9)*44,334,Component.translatable(HandbookCurriculum.titleKey(chapter,id)),b->open(id));}
        }else{
            button(460,562,110,Component.literal("←"),b->move(-1));button(582,562,110,Component.literal("→"),b->move(1));
            if(chapter==HandbookCurriculum.Chapter.TASKS)button(14,526,250,tr(revealed?"hide_solution":"show_solution"),b->{revealed=!revealed;entryPage=0;remember();refresh();});
        }
    }
    @Override public void removed(){remember();super.removed();}
    @Override public void onClose(){remember();minecraft.setScreen(parent);}
    @Override protected void draw(GuiGraphics g){
        g.drawString(font,"PRSCraft "+HandbookContent.VERSION,500,10,0xff9bd7ec,false);
        if(contents){label(g,"book_chapter_intro",18,84);g.drawString(font,(contentsPage+1)+" / "+((entries().size()+17)/18),332,574,0xffbacbdb,false);return;}
        g.pose().pushPose();g.pose().translate(18,82,0);g.pose().scale(1.2f,1.2f,1);g.drawString(font,Component.translatable(HandbookCurriculum.titleKey(chapter,entry)),0,0,0xfff2d494,false);g.pose().popPose();
        g.drawString(font,(entries().indexOf(entry)+1)+"/"+entries().size()+" · "+(entryPage+1)+"/"+pageCount(),300,574,0xffbacbdb,false);
        int top=112;for(var piece:contentPages().get(entryPage)){
            if(piece.kind()==0){g.pose().pushPose();g.pose().translate(18,top,0);g.pose().scale(READING_SCALE,READING_SCALE,1);g.drawString(font,piece.line(),0,0,0xffd5e1ed,false);g.pose().popPose();}
            else if(piece.kind()==2)drawTask(g);
            else{
                var content=HandbookCurriculum.page(entry);g.fill(206,top,514,top+185,0xff0b1420);g.pose().pushPose();g.pose().translate(-63,top-59,0);g.pose().scale(.8f,.8f,1);diagram(g,content.diagram());g.pose().popPose();
                int y=top+196;for(String formula:content.formulas()){g.pose().pushPose();g.pose().translate(18,y,0);g.pose().scale(1.65f,1.65f,1);g.drawWordWrap(font,Component.literal(formula),0,0,410,0xffa7f5c9);g.pose().popPose();y+=font.split(Component.literal(formula),410).size()*15+7;}
            }top+=piece.height();
        }
    }
    private void drawTask(GuiGraphics g){
        if(!revealed)return;
        var task=HandbookCurriculum.task(entry);
        for(var edge:task.edges()){
            var a=task.nodes().get(edge.from());var b=task.nodes().get(edge.to());double angle=Math.atan2(b.y()-a.y(),b.x()-a.x());
            int sx=a.x()+96,sy=diagramY(a.y())+78,ex=b.x()+96,ey=diagramY(b.y())+78;
            if(Math.abs(b.x()-a.x())>=Math.abs(b.y()-a.y())){sx+=b.x()>a.x()?96:-96;ex+=b.x()>a.x()?-96:96;}else{sy+=b.y()>a.y()?78:-78;ey+=b.y()>a.y()?-56:56;}
            line(g,sx,sy,ex,ey,0xff56b6b5);for(double delta:new double[]{-.5,.5})line(g,ex,ey,ex-(int)(8*Math.cos(angle+delta)),ey-(int)(8*Math.sin(angle+delta)),0xff56b6b5);
            if(!edge.label().isEmpty())g.drawString(font,edge.label(),(sx+ex)/2-14,(sy+ey)/2-10,0xfff4d188,false);
        }
        for(var node:task.nodes()){
            int x=node.x(),y=diagramY(node.y());g.fill(x,y,x+192,y+156,0xff22394b);g.fill(x,y,x+192,y+2,0xff68c7b4);
            var item=net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse(node.item()));var stack=new ItemStack(item);g.renderItem(stack,x+6,y+9);
            g.pose().pushPose();g.pose().translate(x+28,y+12,0);g.pose().scale(1.25f,1.25f,1);g.drawString(font,font.plainSubstrByWidth(stack.getHoverName().getString(),126),0,0,0xfff2d494,false);g.pose().popPose();
            g.pose().pushPose();g.pose().translate(x+8,y+38,0);g.pose().scale(1.35f,1.35f,1);int row=0;for(String parameter:node.parameters()){g.drawWordWrap(font,Component.literal(parameter),0,row,130,0xffdae8ec);row+=font.split(Component.literal(parameter),130).size()*10+2;}g.pose().popPose();
        }
        boolean signal=task.nodes().stream().anyMatch(n->n.item().equals("prscraft:transmitter")||n.item().equals("prscraft:bit_generator"));
        if(signal)g.drawWordWrap(font,tr("task_common").copy().append(" ").append(tr("task_wiring")),18,499,680,0xffb7cad7);
    }
    private static int diagramY(int original){return 130+(original-225)*21/16;}
    private void diagram(GuiGraphics g,String kind){
        if(java.util.Set.of("bytes","compression","coding","custom_codes","axes").contains(kind)){
            String[] rows=switch(kind){
                case "bytes" -> new String[]{"text → UTF-8 → bits", "A → 0x41 → 01000001", "NRZ → cable → threshold", "received bits → bytes → text"};
                case "compression" -> new String[]{"AAAAAA → 6 × A (RLE)","P(A)=0.75 → short code", "payload + header + dictionary", "compare TOTAL wire bits"};
                case "coding" -> new String[]{"data → CRC32 → Hamming", "noisy cable: flip decisions", "Hamming → CRC32 → data", "1 error / word: correctable"};
                case "custom_codes" -> new String[]{"A=0   B=10   SPACE=11", "ABBA → 0|10|10|0", "card at RX → ABBA", "no card → raw 010100"};
                default -> new String[]{"Ymax ┌──────────────┐", "     │ amplitude    │", "Ymin └──────────────┘", "     Xmin   time   Xmax"};
            };for(int i=0;i<rows.length;i++)g.drawString(font,rows[i],386,112+i*43,i%2==0?0xff9fe5c4:0xfff2d494,false);return;
        }
        if(kind.equals("analyzer")){
            g.pose().pushPose();g.pose().translate(487,130,0);g.pose().scale(4,4,1);g.renderItem(new ItemStack(PRSCraft.ANALYZER.get()),0,0);g.pose().popPose();g.drawString(font,"READ ONLY · A / B / OUT",404,255,0xff91e0c4,false);return;
        }
        if(kind.equals("gray")){
            g.drawString(font,"Binary    →    Gray",390,105,0xffb2eddb,false);
            String[] b={"00","01","10","11"},v={"00","01","11","10"};for(int i=0;i<4;i++)g.drawString(font,b[i]+"        →        "+v[i],410,145+i*30,0xffb2eddb,false);return;
        }
        if(kind.equals("equipment")){
            var blocks=new net.minecraft.world.item.Item[]{PRSCraft.TRANSMITTER.get().asItem(),PRSCraft.AMPLIFIER.get().asItem(),PRSCraft.CORRELATOR.get().asItem()};
            for(int i=0;i<3;i++){g.pose().pushPose();g.pose().translate(392+i*100,135,0);g.pose().scale(3,3,1);g.renderItem(new ItemStack(blocks[i]),0,0);g.pose().popPose();}
            g.drawString(font,"A → [ DSP ] → OUT",404,244,0xff99ebc0,false);g.drawString(font,"B → SUM / COR",404,268,0xff8ec9ef,false);return;
        }
        if(kind.equals("drawing")){
            g.fill(380,105,680,270,0xff926e43);g.fill(386,111,674,264,0xff20372f);
            for(int i=0;i<220;i++){int yy=185-(int)(45*Math.sin(i*.07));g.fill(405+i,yy,408+i,yy+3,BoardPixels.RGB[(i/16)%16]);}
            g.drawString(font,"128 × 128 · world + editor",396,279,0xffe7d7b4,false);return;
        }
        if(kind.equals("urz")){
            line(g,380,240,680,240,0xff526d80);int old=240;
            for(int i=0;i<280;i++){int bit=i/70;int yy=(bit==0||bit==2||bit==3)&&i%70<35?140:240;if(i>0)line(g,390+i-1,old,390+i,yy,0xff8ceab5);old=yy;}
            g.drawString(font,"URZ: 1     0     1     1",394,104,0xffe7d7b4,false);g.drawString(font,"Tb = 0.1 s; duty = 0.5",394,275,0xffe7d7b4,false);return;
        }
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
