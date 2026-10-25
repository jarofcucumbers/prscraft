package dev.jarofcucumbers.prscraft.client;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.components.*;
import net.minecraft.network.chat.Component;
/** Vanilla scrolling scissors ignore the outer panel pose in 1.21.1. Fit text instead. */
public final class ScaledButton extends Button{
    public ScaledButton(int x,int y,int width,Component message,OnPress action){super(x,y,width,20,message,action,DEFAULT_NARRATION);setTooltip(Tooltip.create(message));}
    @Override public void setMessage(Component message){super.setMessage(message);setTooltip(Tooltip.create(message));}
    @Override public void renderString(GuiGraphics graphics,Font font,int color){
        String text=getMessage().getString();if(font.width(text)>getWidth()-8)text=font.plainSubstrByWidth(text,Math.max(0,getWidth()-8-font.width("…")))+"…";
        graphics.drawCenteredString(font,Component.literal(text).setStyle(getMessage().getStyle()),getX()+getWidth()/2,getY()+(getHeight()-8)/2,color);
    }
}
