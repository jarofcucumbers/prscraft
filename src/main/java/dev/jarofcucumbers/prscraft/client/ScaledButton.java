package dev.jarofcucumbers.prscraft.client;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.components.*;
import net.minecraft.network.chat.Component;
/** Vanilla scrolling scissors ignore the outer panel pose in 1.21.1. Fit text instead. */
public final class ScaledButton extends Button{
    private final float textScale;
    public ScaledButton(int x,int y,int width,Component message,OnPress action){this(x,y,width,20,1,message,action);}
    public ScaledButton(int x,int y,int width,int height,float textScale,Component message,OnPress action){super(x,y,width,height,message,action,DEFAULT_NARRATION);this.textScale=textScale;setTooltip(Tooltip.create(message));}
    @Override public void setMessage(Component message){super.setMessage(message);setTooltip(Tooltip.create(message));}
    @Override public void renderString(GuiGraphics graphics,Font font,int color){
        int available=(int)((getWidth()-8)/textScale);String text=getMessage().getString();if(font.width(text)>available)text=font.plainSubstrByWidth(text,Math.max(0,available-font.width("…")))+"…";
        if(textScale==1){graphics.drawCenteredString(font,Component.literal(text).setStyle(getMessage().getStyle()),getX()+getWidth()/2,getY()+(getHeight()-8)/2,color);return;}
        graphics.pose().pushPose();graphics.pose().translate(getX()+getWidth()/2f,getY()+(getHeight()-8*textScale)/2f,0);graphics.pose().scale(textScale,textScale,1);
        graphics.drawCenteredString(font,Component.literal(text).setStyle(getMessage().getStyle()),0,0,color);graphics.pose().popPose();
    }
}
