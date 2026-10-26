package dev.jarofcucumbers.prscraft.client;
import dev.jarofcucumbers.prscraft.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.*;
public final class CircuitMapScreen extends PanelScreen{
    private static final int VX=14,VY=78,VW=872,VH=388,NW=180,NH=88;
    private final AnalyzerScreen parent;private CompoundTag data;private CircuitLayout.Plan plan;private final MapViewport view=new MapViewport();
    private String signature="";private final Set<String> active=new HashSet<>();private boolean dragging,moved;private double dragX,dragY,hoverX=-1,hoverY=-1;private int pressed=-1,ticks;
    public CircuitMapScreen(AnalyzerScreen parent,CompoundTag data){super(Component.translatable("screen.prscraft.circuit_map"),900,540);this.parent=parent;receive(data);}
    public BlockPos pos(){return parent.pos();}
    private static BlockPos position(CompoundTag tag){return new BlockPos(tag.getInt("x"),tag.getInt("y"),tag.getInt("z"));}
    private static String key(long from,long to,String input){return from+":"+to+":"+input;}
    public void receive(CompoundTag next){
        if(!next.getString("kind").equals("analyzer"))return;data=next;var nodes=data.getList("devices",10);var edges=data.getList("connections",10);var links=new ArrayList<CircuitLayout.Link>();StringBuilder identity=new StringBuilder();
        for(int i=0;i<nodes.size();i++)identity.append(position(nodes.getCompound(i))).append(nodes.getCompound(i).getString("kind"));identity.append(edges);
        for(int i=0;i<edges.size();i++){var edge=edges.getCompound(i);if(edge.getString("direction").equals("ab"))links.add(new CircuitLayout.Link(edge.getInt("a"),edge.getInt("b")));if(edge.getString("direction").equals("ba"))links.add(new CircuitLayout.Link(edge.getInt("b"),edge.getInt("a")));}
        if(plan==null||!signature.equals(identity.toString())){plan=CircuitLayout.arrange(nodes.size(),links);signature=identity.toString();fit();}
        active.clear();var chosen=data.getCompound("capture").getList("signalEdges",10);for(int i=0;i<chosen.size();i++){var edge=chosen.getCompound(i);active.add(key(edge.getLong("from"),edge.getLong("to"),edge.getString("input")));}
    }
    private void fit(){if(plan!=null)view.fit(plan.width()+50,plan.height()+20,VW,VH);}
    @Override protected void build(){button(14,504,128,tr("back"),b->onClose());button(154,504,154,tr("map_fit"),b->fit());button(320,504,54,Component.literal("−"),b->view.zoomAt(1/1.25,VW/2.,VH/2.));button(386,504,54,Component.literal("+"),b->view.zoomAt(1.25,VW/2.,VH/2.));button(452,504,140,tr("refresh"),b->request());button(734,504,152,tr("book"),b->minecraft.setScreen(new HandbookScreen(this)));}
    private void request(){PacketDistributor.sendToServer(new RadioNetwork.Analyze(pos(),new BlockPos(data.getInt("selectedX"),data.getInt("selectedY"),data.getInt("selectedZ"))));}
    @Override public void tick(){if(minecraft.player==null||minecraft.player.distanceToSqr(pos().getX()+.5,pos().getY()+.5,pos().getZ()+.5)>64){onClose();return;}if(++ticks%40==0)request();}
    protected double localX(double x){return (x-(int)((width-panelWidth*scale)/2))/scale;}protected double localY(double y){return (y-(int)((height-panelHeight*scale)/2))/scale;}
    private boolean inside(double x,double y){return x>=VX&&x<VX+VW&&y>=VY&&y<VY+VH;}
    private int nodeAt(double x,double y){if(!inside(x,y))return -1;double wx=view.worldX(x-VX),wy=view.worldY(y-VY);for(int i=0;i<plan.points().size();i++){var p=plan.points().get(i);if(wx>=p.x()&&wx<p.x()+NW&&wy>=p.y()&&wy<p.y()+NH)return i;}return -1;}
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical){double xx=localX(x),yy=localY(y);if(inside(xx,yy)){view.zoomAt(Math.pow(1.2,vertical),xx-VX,yy-VY);return true;}return super.mouseScrolled(x,y,horizontal,vertical);}
    @Override public void mouseMoved(double x,double y){hoverX=localX(x);hoverY=localY(y);super.mouseMoved(x,y);}
    @Override public boolean mouseClicked(double x,double y,int button){double xx=localX(x),yy=localY(y);if((button==0||button==2)&&inside(xx,yy)){dragging=true;moved=false;dragX=xx;dragY=yy;pressed=button==0?nodeAt(xx,yy):-1;return true;}return super.mouseClicked(x,y,button);}
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy){if(dragging){double xx=localX(x),yy=localY(y);if(Math.abs(xx-dragX)+Math.abs(yy-dragY)>3)moved=true;view.pan(dx/scale,dy/scale);return true;}return super.mouseDragged(x,y,button,dx,dy);}
    @Override public boolean mouseReleased(double x,double y,int button){if(dragging){dragging=false;if(button==0&&!moved&&pressed>=0&&nodeAt(localX(x),localY(y))==pressed){parent.select(position(data.getList("devices",10).getCompound(pressed)));onClose();}return true;}return super.mouseReleased(x,y,button);}
    private void arrow(GuiGraphics g,int x,int y,boolean directed,int color){if(directed){line(g,x-7,y-4,x,y,color);line(g,x-7,y+4,x,y,color);}}
    private void edge(GuiGraphics g,CompoundTag edge,ListTag nodes){int a=edge.getInt("a"),b=edge.getInt("b");if(a<0||b<0||a>=plan.points().size()||b>=plan.points().size())return;
        boolean directed=!edge.getString("direction").equals("physical")&&!edge.getString("direction").isEmpty();boolean reverse=edge.getString("direction").equals("ba");int from=reverse?b:a,to=reverse?a:b;
        String outRole=edge.getString(reverse?"bRole":"aRole"),inRole=edge.getString(reverse?"aRole":"bRole");var p=plan.points().get(from);var q=plan.points().get(to);
        boolean selected=directed&&active.contains(key(position(nodes.getCompound(from)).asLong(),position(nodes.getCompound(to)).asLong(),inRole));int color=selected?0xff67e7a8:directed?0xff579fb9:0xff566978;
        int sx=(int)p.x()+NW,sy=(int)p.y()+NH/2,ex=(int)q.x(),ey=(int)q.y()+(inRole.equals("B")?66:inRole.equals("A")?28:44);
        int bend=sx<ex?(sx+ex)/2:sx+30;
        if(from==to){int top=(int)p.y()-18;line(g,sx,sy,bend,sy,color);line(g,bend,sy,bend,top,color);line(g,bend,top,ex-20,top,color);line(g,ex-20,top,ex-20,ey,color);line(g,ex-20,ey,ex,ey,color);}
        else if(sx>=ex){int track=(sy+ey)/2;line(g,sx,sy,bend,sy,color);line(g,bend,sy,bend,track,color);line(g,bend,track,ex-24,track,color);line(g,ex-24,track,ex-24,ey,color);line(g,ex-24,ey,ex,ey,color);}
        else{line(g,sx,sy,bend,sy,color);line(g,bend,sy,bend,ey,color);line(g,bend,ey,ex,ey,color);}
        arrow(g,ex,ey,directed,color);
        if(directed){g.drawString(font,outRole,sx+3,sy-12,0xff9bd9bb,false);g.drawString(font,inRole,ex-18,ey-12,inRole.equals("B")?0xff7fc7ee:0xffe9b573,false);}
    }
    @Override protected void draw(GuiGraphics g){
        g.drawWordWrap(font,tr("map_help"),14,38,872,0xffc3d6e4);g.fill(VX,VY,VX+VW,VY+VH,0xff0b1520);g.flush();
        int ox=(int)((width-panelWidth*scale)/2),oy=(int)((height-panelHeight*scale)/2);g.enableScissor((int)(ox+VX*scale),(int)(oy+VY*scale),(int)(ox+(VX+VW)*scale),(int)(oy+(VY+VH)*scale));
        g.pose().pushPose();g.pose().translate(VX+view.x(),VY+view.y(),0);g.pose().scale((float)view.zoom(),(float)view.zoom(),1);var nodes=data.getList("devices",10);var edges=data.getList("connections",10);
        for(int i=0;i<edges.size();i++)edge(g,edges.getCompound(i),nodes);
        for(int i=0;i<nodes.size();i++){var tag=nodes.getCompound(i);var p=plan.points().get(i);int x=(int)p.x(),y=(int)p.y();String kind=tag.getString("kind");var settings=tag.getCompound("settings");boolean source=java.util.Set.of("radio","bit_generator","message_source","message_receiver","packet_receiver").contains(kind);boolean tx=settings.getBoolean("transmit");boolean selected=tag.getInt("x")==data.getInt("selectedX")&&tag.getInt("y")==data.getInt("selectedY")&&tag.getInt("z")==data.getInt("selectedZ");
            int border=selected?0xfff0ce78:source?(tx?0xff48bf8f:0xff5a9cda):0xff6499b0;g.fill(x-1,y-1,x+NW+1,y+NH+1,border);g.fill(x,y,x+NW,y+NH,0xff203447);
            Component name=kind.equals("radio")?Component.literal(tx?"TX":"RX"):Component.translatable("block.prscraft."+kind);g.drawString(font,font.plainSubstrByWidth(name.getString(),164),x+8,y+10,0xffe4edf1,false);
            g.drawString(font,source?(tx?"TX":"RX")+" · "+fmt(settings.getDouble("carrierMHz"))+" MHz":kind.equals("adder")||kind.equals("correlator")?"A / B → OUT":"A → OUT",x+8,y+31,0xffa7d4df,false);
            g.drawString(font,position(tag).toShortString(),x+8,y+49,0xffb8c9d3,false);g.drawString(font,settings.getBoolean("enabled")?"ON":"OFF",x+8,y+68,settings.getBoolean("enabled")?0xff7adba9:0xfff58b81,false);
        }
        g.pose().popPose();g.flush();g.disableScissor();
        int hover=nodeAt(hoverX,hoverY);if(hover>=0)g.drawString(font,font.plainSubstrByWidth(data.getList("devices",10).getCompound(hover).getString("portRoles"),870),14,478,0xffb8d2e0,false);
        else if(data.getBoolean("truncated"))g.drawString(font,tr("map_truncated"),14,478,0xffedb778,false);
        g.drawString(font,String.format(java.util.Locale.ROOT,"%.0f%%",view.zoom()*100),614,510,0xffc9dfdf,false);
    }
    @Override public void onClose(){parent.receive(data);minecraft.setScreen(parent);}
}
