package dev.jarofcucumbers.prscraft.client;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
/** Multiline editor with pose-aware scissors, including selection/copy for received text. */
final class MessageTextBox extends MultiLineEditBox{
    private final boolean readOnly;
    MessageTextBox(Font font,int x,int y,int width,int height,Component label,boolean readOnly){super(font,x,y,width,height,Component.empty(),label);this.readOnly=readOnly;setCharacterLimit(16384);}
    @Override public boolean charTyped(char c,int modifiers){return !readOnly&&super.charTyped(c,modifiers);}
    @Override public boolean keyPressed(int key,int scan,int modifiers){if(readOnly&&!Screen.isCopy(key)&&!Screen.isSelectAll(key)&&!(key>=262&&key<=269))return false;return super.keyPressed(key,scan,modifiers);}
    @Override public void renderWidget(GuiGraphics g,int mx,int my,float partial){if(!visible)return;renderBackground(g);g.flush();var pose=g.pose().last().pose();var a=pose.transformPosition(getX()+1,getY()+1,0,new org.joml.Vector3f());var b=pose.transformPosition(getX()+getWidth()-1,getY()+getHeight()-1,0,new org.joml.Vector3f());g.enableScissor((int)a.x,(int)a.y,(int)Math.ceil(b.x),(int)Math.ceil(b.y));g.pose().pushPose();g.pose().translate(0,-scrollAmount(),0);renderContents(g,mx,my,partial);g.pose().popPose();g.flush();g.disableScissor();renderDecorations(g);}
}
