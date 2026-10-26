package dev.jarofcucumbers.prscraft.client;
import dev.jarofcucumbers.prscraft.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.neoforged.neoforge.network.PacketDistributor;
public final class CableScreen extends PanelScreen{
    private final BlockPos pos;private CompoundTag data;private EditBox length,damage,dispersion;private boolean environment;private String error="";
    public CableScreen(BlockPos pos,CompoundTag data){super(Component.translatable("item.prscraft.cable_wrench"),460,320);this.pos=pos;this.data=data;environment=data.getBoolean("environment");}
    public BlockPos pos(){return pos;}public void receive(CompoundTag data){this.data=data;error=data.getString("error");}
    @Override protected void build(){length=field(16,65,200,length==null?Double.toString(data.getDouble("metres")):length.getValue(),18,"cable_length");damage=field(240,65,200,damage==null?Double.toString(data.getDouble("damage")*100):damage.getValue(),18,"cable_damage");
        dispersion=field(16,130,200,dispersion==null?Double.toString(data.getDouble("dispersion")):dispersion.getValue(),18,"dispersion");button(240,130,200,tr(environment?"sim_environment_on":"sim_environment_off"),b->{environment=!environment;b.setMessage(tr(environment?"sim_environment_on":"sim_environment_off"));});
        button(16,276,200,tr("apply"),b->{try{var settings=new CableSettings(number(length),number(damage)/100,number(dispersion),environment);PacketDistributor.sendToServer(new RadioNetwork.TuneCable(pos,settings.metres(),settings.damage(),settings.dispersion(),settings.environment()));error="";}catch(NumberFormatException e){error="number";}catch(IllegalArgumentException e){error=e.getMessage();}});
        button(240,276,200,tr("book"),b->minecraft.setScreen(new HandbookScreen(this)));
    }
    @Override public void tick(){if(minecraft.player==null||minecraft.player.distanceToSqr(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5)>64)onClose();}
    @Override protected void draw(GuiGraphics g){label(g,"cable_length",16,45);label(g,"cable_damage",240,45);g.drawString(font,data.getString("medium")+" · "+pos.toShortString(),16,105,0xffc3d6e4,false);
        label(g,"dispersion",16,112);label(g,"cable_model",16,176);g.drawString(font,"extra loss = 3·damage·length dB",16,196,0xffa9c9dd,false);g.drawString(font,"extra variance = 0.0001·damage²·length u²",16,218,0xffa9c9dd,false);
        if(!error.isEmpty())g.drawString(font,Component.translatable("error.prscraft."+error),16,250,0xffff7788,false);
    }
}
