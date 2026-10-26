package dev.jarofcucumbers.prscraft.client;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.*;
import net.minecraft.network.chat.Component;
public abstract class PanelScreen extends Screen {
    protected final int panelWidth,panelHeight;protected float scale;private int ox,oy;
    protected PanelScreen(Component title,int w,int h){super(title);panelWidth=w;panelHeight=h;}
    protected Component tr(String key){return Component.translatable("screen.prscraft."+key);}
    protected Button button(int x,int y,int w,Component label,Button.OnPress press){return addRenderableWidget(new ScaledButton(x,y,w,label,press));}
    protected EditBox field(int x,int y,int w,String value,int limit,String key){var f=new EditBox(font,x,y,w,20,tr(key));f.setMaxLength(limit);f.setValue(value);return addRenderableWidget(f);}
    protected static double number(EditBox box){return Double.parseDouble(box.getValue().replace(',','.'));}
    protected static String fmt(double value){return String.format(java.util.Locale.ROOT,"%.5g",value);}
    @Override protected final void init(){scale=Math.min((width-8f)/panelWidth,(height-8f)/panelHeight);ox=(int)((width-panelWidth*scale)/2);oy=(int)((height-panelHeight*scale)/2);build();}
    protected abstract void build();protected abstract void draw(GuiGraphics g);
    @Override public void renderBackground(GuiGraphics g,int x,int y,float p){}
    @Override public final void render(GuiGraphics g,int x,int y,float partial){g.fill(0,0,width,height,0xc0101520);g.pose().pushPose();g.pose().translate(ox,oy,0);g.pose().scale(scale,scale,1);
        g.fill(0,0,panelWidth,panelHeight,0xff162333);g.fill(0,0,panelWidth,28,0xff294258);g.drawString(font,title,12,10,0xffffffff,false);draw(g);super.render(g,(int)((x-ox)/scale),(int)((y-oy)/scale),partial);g.pose().popPose();}
    protected void label(GuiGraphics g,String key,int x,int y){g.drawString(font,tr(key),x,y,0xffc3d6e4,false);}
    protected void line(GuiGraphics g,int x0,int y0,int x1,int y1,int color){int dx=Math.abs(x1-x0),sx=x0<x1?1:-1,dy=-Math.abs(y1-y0),sy=y0<y1?1:-1,err=dx+dy;while(true){g.fill(x0,y0,x0+1,y0+1,color);if(x0==x1&&y0==y1)break;int e=2*err;if(e>=dy){err+=dy;x0+=sx;}if(e<=dx){err+=dx;y0+=sy;}}}
    @Override public boolean mouseClicked(double x,double y,int b){return super.mouseClicked((x-ox)/scale,(y-oy)/scale,b);}
    @Override public boolean mouseReleased(double x,double y,int b){return super.mouseReleased((x-ox)/scale,(y-oy)/scale,b);}
    @Override public boolean mouseDragged(double x,double y,int b,double dx,double dy){return super.mouseDragged((x-ox)/scale,(y-oy)/scale,b,dx/scale,dy/scale);}
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical){return super.mouseScrolled((x-ox)/scale,(y-oy)/scale,horizontal,vertical);}
    @Override public boolean isPauseScreen(){return false;}
}
