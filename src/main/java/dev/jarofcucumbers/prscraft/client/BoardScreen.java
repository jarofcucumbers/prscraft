package dev.jarofcucumbers.prscraft.client;
import dev.jarofcucumbers.prscraft.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.*;
public final class BoardScreen extends PanelScreen{
    private final BlockPos pos;private byte[] pixels;private int brush=1,ticks,textScale=1;private boolean offhand,dragging;private int lastX,lastY,startX,startY,hoverX=-1,hoverY=-1;
    private BoardDrawing.Shape mode=BoardDrawing.Shape.FREEHAND;private EditBox text;private int[] preview=new int[0];
    private final LinkedHashSet<Integer> pending=new LinkedHashSet<>();private static final int X=16,Y=48,ZOOM=3;
    public BoardScreen(BlockPos pos,CompoundTag data){super(Component.translatable("block.prscraft.drawing_board"),610,454);this.pos=pos;pixels=BoardPixels.read(data.getByteArray("pixels"));var p=net.minecraft.client.Minecraft.getInstance().player;offhand=p!=null&&!BoardTools.isTool(p.getMainHandItem())&&BoardTools.isTool(p.getOffhandItem());}
    public BlockPos pos(){return pos;}public void receive(CompoundTag data){pixels=BoardPixels.read(data.getByteArray("pixels"));int color=color();for(int p:pending)pixels[p]=(byte)color;}
    private String modeKey(){return "board_mode_"+mode.name().toLowerCase(java.util.Locale.ROOT);}
    @Override protected void build(){
        button(418,48,174,tr("brush_size").copy().append(": "+brush),b->{int[] sizes={1,2,4,8,16};brush=sizes[(java.util.Arrays.binarySearch(sizes,brush)+1)%sizes.length];b.setMessage(tr("brush_size").copy().append(": "+brush));});
        button(418,80,174,tr(offhand?"offhand":"mainhand"),b->{flush();offhand=!offhand;cancelPreview();b.setMessage(tr(offhand?"offhand":"mainhand"));});
        button(418,112,174,tr(modeKey()),b->{flush();cancelPreview();mode=BoardDrawing.Shape.values()[(mode.ordinal()+1)%BoardDrawing.Shape.values().length];b.setMessage(tr(modeKey()));text.visible=mode==BoardDrawing.Shape.TEXT;textSize.visible=text.visible;if(text.visible)setFocused(text);});
        text=field(418,174,174,text==null?"":text.getValue(),64,"board_text");text.visible=mode==BoardDrawing.Shape.TEXT;
        textSize=button(418,206,174,tr("board_text_size").copy().append(": "+textScale+"x"),b->{textScale=textScale%4+1;b.setMessage(tr("board_text_size").copy().append(": "+textScale+"x"));});textSize.visible=text.visible;
        button(418,374,174,tr("book"),b->{flush();minecraft.setScreen(new HandbookScreen(this));});button(418,406,174,tr("back"),b->onClose());
    }
    private net.minecraft.client.gui.components.Button textSize;
    private ItemStack tool(){return minecraft.player==null?ItemStack.EMPTY:minecraft.player.getItemInHand(offhand?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND);}
    private int color(){return tool().getItem() instanceof ChalkItem c?c.palette:0;}
    private boolean usable(){return BoardTools.isTool(tool())&&BoardTools.remaining(tool())>0;}
    @Override protected void draw(GuiGraphics g){
        g.fill(X-2,Y-2,X+386,Y+386,0xff937348);
        for(int yy=0;yy<128;yy++)for(int xx=0;xx<128;){int start=xx,color=pixels[yy*128+xx];while(xx<128&&pixels[yy*128+xx]==color)xx++;g.fill(X+start*ZOOM,Y+yy*ZOOM,X+xx*ZOOM,Y+(yy+1)*ZOOM,BoardPixels.color(color));}
        if(mode==BoardDrawing.Shape.TEXT)preview=hoverX>=0&&usable()?BoardDrawing.text(text.getValue(),hoverX,hoverY,textScale):new int[0];
        if(usable())for(int p:preview){int x=p%128,y=p/128;g.fill(X+x*ZOOM,Y+y*ZOOM,X+(x+1)*ZOOM,Y+(y+1)*ZOOM,(BoardPixels.color(color())&0xffffff)|0x99000000);}
        if(text.visible)label(g,"board_text",418,158);
        g.renderItem(tool(),420,242);g.drawString(font,Component.translatable("screen.prscraft.tool_left",BoardTools.remaining(tool())),444,246,0xffe4dfbf,false);
        String help=!BoardTools.isTool(tool())?"board_need_tool":!usable()?"board_empty":mode==BoardDrawing.Shape.FREEHAND?"board_help":mode==BoardDrawing.Shape.TEXT?"board_text_help":"board_shape_help";
        g.drawWordWrap(font,tr(help),418,278,174,0xffc3d6e4);g.drawString(font,Component.translatable("screen.prscraft.preview_cost",preview.length),418,348,0xffa8d6b4,false);g.drawString(font,"128 × 128",16,438,0xffbdcabb,false);
    }
    private boolean inside(double x,double y){return x>=X&&y>=Y&&x<X+384&&y<Y+384;}
    private void commit(int[] points){if(!usable())return;int color=color();for(int p:points){pending.add(p);pixels[p]=(byte)color;}}
    private void cancelPreview(){dragging=false;preview=new int[0];hoverX=hoverY=-1;}
    private double px(double x){return (x-(int)((width-panelWidth*scale)/2))/scale;}
    private double py(double y){return (y-(int)((height-panelHeight*scale)/2))/scale;}
    private int canvas(double value,int origin){return Math.max(0,Math.min(127,(int)Math.floor((value-origin)/ZOOM)));}
    @Override public void mouseMoved(double x,double y){double xx=px(x),yy=py(y);if(inside(xx,yy)){hoverX=canvas(xx,X);hoverY=canvas(yy,Y);}else hoverX=hoverY=-1;super.mouseMoved(x,y);}
    @Override public boolean mouseClicked(double x,double y,int b){double xx=px(x),yy=py(y);if(b==0&&inside(xx,yy)){
        if(!usable())return true;startX=lastX=canvas(xx,X);startY=lastY=canvas(yy,Y);
        if(mode==BoardDrawing.Shape.TEXT){commit(BoardDrawing.text(text.getValue(),lastX,lastY,textScale));flush();return true;}
        dragging=true;if(mode==BoardDrawing.Shape.FREEHAND)commit(BoardPixels.brush(lastX,lastY,brush));else preview=BoardDrawing.shape(mode,startX,startY,lastX,lastY,brush);return true;
    }return super.mouseClicked(x,y,b);}
    @Override public boolean mouseDragged(double x,double y,int b,double dx,double dy){if(dragging&&b==0){int nx=canvas(px(x),X),ny=canvas(py(y),Y);
        if(mode==BoardDrawing.Shape.FREEHAND)commit(BoardDrawing.shape(BoardDrawing.Shape.LINE,lastX,lastY,nx,ny,brush));else preview=BoardDrawing.shape(mode,startX,startY,nx,ny,brush);lastX=nx;lastY=ny;return true;
    }return super.mouseDragged(x,y,b,dx,dy);}
    @Override public boolean mouseReleased(double x,double y,int b){if(b==0&&dragging){if(mode!=BoardDrawing.Shape.FREEHAND)commit(preview);cancelPreview();flush();return true;}return super.mouseReleased(x,y,b);}
    private void flush(){if(pending.isEmpty())return;int[] batch=pending.stream().mapToInt(Integer::intValue).toArray();pending.clear();PacketDistributor.sendToServer(new BoardNetwork.Stroke(pos,offhand,batch));}
    @Override public void tick(){if(minecraft.player==null||minecraft.level==null||minecraft.player.distanceToSqr(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5)>64||!(minecraft.level.getBlockEntity(pos) instanceof DrawingBoardEntity)){pending.clear();onClose();return;}if(++ticks%2==0){if(!dragging&&pending.isEmpty())pixels=((DrawingBoardEntity)minecraft.level.getBlockEntity(pos)).pixels();flush();}}
    @Override public void onClose(){cancelPreview();flush();super.onClose();}
}
