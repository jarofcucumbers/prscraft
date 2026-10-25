package dev.jarofcucumbers.prscraft.client;
import dev.jarofcucumbers.prscraft.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.neoforged.neoforge.network.PacketDistributor;
public final class CableScreen extends PanelScreen{
    private final BlockPos pos;private CompoundTag data;private EditBox length,damage;private String error="";
    public CableScreen(BlockPos pos,CompoundTag data){super(Component.translatable("item.prscraft.cable_wrench"),460,250);this.pos=pos;this.data=data;}
    public BlockPos pos(){return pos;}public void receive(CompoundTag data){this.data=data;error=data.getString("error");}
    @Override protected void build(){length=field(16,65,200,length==null?Double.toString(data.getDouble("metres")):length.getValue(),18,"cable_length");damage=field(240,65,200,damage==null?Double.toString(data.getDouble("damage")*100):damage.getValue(),18,"cable_damage");
        button(16,206,200,tr("apply"),b->{try{var settings=new CableSettings(number(length),number(damage)/100);PacketDistributor.sendToServer(new RadioNetwork.TuneCable(pos,settings.metres(),settings.damage()));error="";}catch(NumberFormatException e){error="number";}catch(IllegalArgumentException e){error=e.getMessage();}});
        button(240,206,200,tr("book"),b->minecraft.setScreen(new HandbookScreen(this)));
    }
    @Override public void tick(){if(minecraft.player==null||minecraft.player.distanceToSqr(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5)>64)onClose();}
    @Override protected void draw(GuiGraphics g){label(g,"cable_length",16,45);label(g,"cable_damage",240,45);g.drawString(font,data.getString("medium")+" · "+pos.toShortString(),16,105,0xffc3d6e4,false);
        label(g,"cable_model",16,132);g.drawString(font,"extra loss = 3·damage·length dB",16,151,0xffa9c9dd,false);g.drawString(font,"extra variance = 0.0001·damage²·length u²",16,168,0xffa9c9dd,false);
        if(!error.isEmpty())g.drawString(font,Component.translatable("error.prscraft."+error),16,187,0xffff7788,false);
    }
}
