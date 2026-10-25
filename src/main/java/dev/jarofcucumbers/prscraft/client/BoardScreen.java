package dev.jarofcucumbers.prscraft.client;
import dev.jarofcucumbers.prscraft.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.*;
public final class BoardScreen extends PanelScreen{
    private final BlockPos pos;private byte[] pixels;private int brush=1,ticks;private boolean offhand,dragging;private int lastX,lastY;
    private final LinkedHashSet<Integer> pending=new LinkedHashSet<>();
    private static final int X=16,Y=48,ZOOM=3;
    public BoardScreen(BlockPos pos,CompoundTag data){super(Component.translatable("block.prscraft.drawing_board"),610,454);this.pos=pos;pixels=BoardPixels.read(data.getByteArray("pixels"));var p=net.minecraft.client.Minecraft.getInstance().player;offhand=p!=null&&!BoardTools.isTool(p.getMainHandItem())&&BoardTools.isTool(p.getOffhandItem());}
    public BlockPos pos(){return pos;}
    public void receive(CompoundTag data){pixels=BoardPixels.read(data.getByteArray("pixels"));int color=tool().getItem() instanceof ChalkItem c?c.palette:0;for(int p:pending)pixels[p]=(byte)color;}
    @Override protected void build(){button(418,48,174,tr("brush_size").copy().append(": "+brush),b->{int[] sizes={1,2,4,8,16};brush=sizes[(java.util.Arrays.binarySearch(sizes,brush)+1)%sizes.length];b.setMessage(tr("brush_size").copy().append(": "+brush));});
        button(418,80,174,tr(offhand?"offhand":"mainhand"),b->{flushAll();offhand=!offhand;b.setMessage(tr(offhand?"offhand":"mainhand"));});
        button(418,374,174,tr("book"),b->{flushAll();minecraft.setScreen(new HandbookScreen(this));});button(418,406,174,tr("back"),b->onClose());}
    private ItemStack tool(){return minecraft.player==null?ItemStack.EMPTY:minecraft.player.getItemInHand(offhand?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND);}
    @Override protected void draw(GuiGraphics g){
        g.fill(X-2,Y-2,X+386,Y+386,0xff937348);
        for(int yy=0;yy<128;yy++)for(int xx=0;xx<128;){int start=xx,color=pixels[yy*128+xx];while(xx<128&&pixels[yy*128+xx]==color)xx++;g.fill(X+start*ZOOM,Y+yy*ZOOM,X+xx*ZOOM,Y+(yy+1)*ZOOM,BoardPixels.color(color));}
        var held=tool();g.renderItem(held,420,118);g.drawString(font,Component.translatable("screen.prscraft.tool_left",BoardTools.remaining(held)),444,122,0xffe4dfbf,false);
        g.drawWordWrap(font,tr(!BoardTools.isTool(held)?"board_need_tool":BoardTools.remaining(held)==0?"board_empty":"board_help"),418,154,174,0xffc3d6e4);
        g.drawWordWrap(font,tr("board_wash"),418,250,174,0xff91cbb1);g.drawString(font,"128 × 128",16,438,0xffbdcabb,false);
    }
    private boolean inside(double x,double y){return x>=X&&y>=Y&&x<X+384&&y<Y+384;}
    private void dab(int x,int y){if(!BoardTools.isTool(tool())||BoardTools.remaining(tool())==0)return;
        int color=tool().getItem() instanceof ChalkItem c?c.palette:0;
        for(int p:BoardPixels.brush(x,y,brush)){if(pending.size()>=1024)flush();pending.add(p);pixels[p]=(byte)color;}
    }
    // PanelScreen transforms coordinates only for its widgets. The canvas uses the same inverse transform.
    private double px(double x){return (x-(width-panelWidth*scale)/2)/scale;}
    private double py(double y){return (y-(height-panelHeight*scale)/2)/scale;}
    @Override public boolean mouseClicked(double x,double y,int b){double xx=px(x),yy=py(y);if(b==0&&inside(xx,yy)){dragging=true;lastX=(int)(xx-X)/ZOOM;lastY=(int)(yy-Y)/ZOOM;dab(lastX,lastY);return true;}return super.mouseClicked(x,y,b);}
    @Override public boolean mouseDragged(double x,double y,int b,double dx,double dy){double xx=px(x),yy=py(y);if(dragging&&b==0){if(inside(xx,yy)){
        int nx=(int)(xx-X)/ZOOM,ny=(int)(yy-Y)/ZOOM,steps=Math.max(Math.abs(nx-lastX),Math.abs(ny-lastY));
        for(int i=1;i<=steps;i++)dab(lastX+(nx-lastX)*i/steps,lastY+(ny-lastY)*i/steps);lastX=nx;lastY=ny;
    }return true;}return super.mouseDragged(x,y,b,dx,dy);}
    @Override public boolean mouseReleased(double x,double y,int b){if(b==0&&dragging){dragging=false;flushAll();return true;}return super.mouseReleased(x,y,b);}
    private void flush(){if(pending.isEmpty())return;int[] batch=pending.stream().limit(256).mapToInt(Integer::intValue).toArray();for(int p:batch)pending.remove(p);PacketDistributor.sendToServer(new BoardNetwork.Stroke(pos,offhand,batch));}
    private void flushAll(){while(!pending.isEmpty())flush();}
    @Override public void tick(){if(minecraft.player==null||minecraft.level==null||minecraft.player.distanceToSqr(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5)>64||!(minecraft.level.getBlockEntity(pos) instanceof DrawingBoardEntity)){pending.clear();onClose();return;}if(++ticks%2==0){if(!dragging&&pending.isEmpty())pixels=((DrawingBoardEntity)minecraft.level.getBlockEntity(pos)).pixels();flush();}}
    @Override public void onClose(){flushAll();super.onClose();}
}
