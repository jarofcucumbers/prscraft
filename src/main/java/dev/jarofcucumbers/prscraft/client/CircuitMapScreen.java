package dev.jarofcucumbers.prscraft.client;
import dev.jarofcucumbers.prscraft.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
public final class CircuitMapScreen extends PanelScreen{
    private final AnalyzerScreen parent;private final CompoundTag data;
    public CircuitMapScreen(AnalyzerScreen parent,CompoundTag data){super(Component.translatable("screen.prscraft.circuit_map"),720,452);this.parent=parent;this.data=data;}
    private int x(int i){return 14+(i%4)*174;}private int y(int i){return 70+(i/4)*39;}
    @Override protected void build(){var devices=data.getList("devices",10);for(int i=0;i<devices.size();i++){
        var t=devices.getCompound(i);BlockPos pos=new BlockPos(t.getInt("x"),t.getInt("y"),t.getInt("z"));String kind=t.getString("kind");
        var settings=RadioSettings.read(t.getCompound("settings"));String label=kind.equals("radio")?(settings.transmit()?"TX":"RX"):kind;
        button(x(i),y(i),156,Component.literal(label),b->{parent.select(pos);onClose();}).setTooltip(Tooltip.create(Component.literal(pos.toShortString()+" · "+t.getString("portRoles"))));
    }button(14,416,150,tr("back"),b->onClose());}
    @Override protected void draw(GuiGraphics g){label(g,"circuit_map_hint",14,40);var edges=data.getList("connections",10);for(int i=0;i<edges.size();i++){var edge=edges.getCompound(i);int a=edge.getInt("a"),b=edge.getInt("b");line(g,x(a)+78,y(a)+10,x(b)+78,y(b)+10,0xff507b91);}}
    @Override public void onClose(){minecraft.setScreen(parent);}
}
