package dev.jarofcucumbers.prscraft.client;

import dev.jarofcucumbers.prscraft.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class PresetScreen extends AbstractContainerScreen<PresetMenu> {
    public PresetScreen(PresetMenu menu,Inventory inventory,Component title){super(menu,inventory,title);imageWidth=256;imageHeight=280;inventoryLabelY=183;}
    @Override protected void init(){super.init();
        add(52,31,196,"save_template",0);add(52,55,196,"apply_template",1);
        add(52,143,196,"codebook_edit",5);
        add(52,87,196,"record",2);add(52,111,94,"export_short",3);add(154,111,94,"view_capture",4);
    }
    private void add(int x,int y,int w,String key,int action){addRenderableWidget(Button.builder(Component.translatable("screen.prscraft."+key),b->{
        if(minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,action);
    }).bounds(leftPos+x,topPos+y,w,20).build());}
    @Override protected void renderBg(GuiGraphics g,float partial,int mouseX,int mouseY){
        g.fill(leftPos,topPos,leftPos+imageWidth,topPos+imageHeight,0xff172738);
        for(var slot:menu.slots)g.fill(leftPos+slot.x-1,topPos+slot.y-1,leftPos+slot.x+17,topPos+slot.y+17,0xff425468);
    }
    @Override protected void renderLabels(GuiGraphics g,int mouseX,int mouseY){
        g.drawString(font,title,8,6,0xffffffff,false);
        g.drawString(font,Component.translatable("screen.prscraft.paper_slot"),8,22,0xffc3d6e4,false);
        g.drawString(font,Component.translatable("screen.prscraft.card_slot"),8,78,0xffc3d6e4,false);
        g.drawString(font,Component.translatable("screen.prscraft.codebook_slot"),8,132,0xffc3d6e4,false);
        g.drawString(font,playerInventoryTitle,8,183,0xffc3d6e4,false);
    }
    @Override public void render(GuiGraphics g,int x,int y,float partial){super.render(g,x,y,partial);renderTooltip(g,x,y);}
    @Override public void onClose(){
        super.onClose();
        var tag=new net.minecraft.nbt.CompoundTag();tag.putString("action","reopen");
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(new RadioNetwork.Request(menu.radio.getBlockPos(),false,tag));
    }
}
