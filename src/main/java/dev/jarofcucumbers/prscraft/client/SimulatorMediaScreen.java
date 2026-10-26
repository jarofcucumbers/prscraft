package dev.jarofcucumbers.prscraft.client;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
public final class SimulatorMediaScreen extends PanelScreen{
    private final CompoundTag data;public SimulatorMediaScreen(CompoundTag data){super(Component.translatable("screen.prscraft.sim_media"),620,360);this.data=data;}
    @Override protected void build(){String[] actions={"save_template","apply_template","record","codebook","clear_card"};for(int i=0;i<actions.length;i++){final String action=actions[i];button(14,48+i*44,350,tr(action.equals("codebook")?"codebook_edit":action.equals("clear_card")?"sim_clear_card":action),b->SimulationClient.send(action,new CompoundTag())).active=!action.equals("apply_template")||data.getBoolean("templatePresent");}button(380,136,226,tr("view_capture"),b->{var capture=data.getCompound("recording").copy();if(capture.contains("settings")){capture.putBoolean("recorded",true);capture.putString("captureKind",capture.getString("kind"));capture.putString("kind","recording");RadioClient.receive(new dev.jarofcucumbers.prscraft.RadioNetwork.Snapshot(dev.jarofcucumbers.prscraft.SimulatorDocument.position(SimulationClient.node),true,capture));}});button(380,222,226,tr("notebook_capture"),b->net.neoforged.neoforge.network.PacketDistributor.sendToServer(new dev.jarofcucumbers.prscraft.NotebookNetwork.Request(-1,0,"capture_sim","","",SimulationClient.root,SimulationClient.node)));button(380,180,226,tr("message_export"),b->RadioClient.export(data.getCompound("recording")));button(14,316,350,tr("back"),b->{var t=new CompoundTag();t.putBoolean("save",false);var request=new CompoundTag();request.putString("action","reopen");t.put("request",request);SimulationClient.send("device",t);});}
    @Override protected void draw(GuiGraphics g){g.drawWordWrap(font,tr("sim_media_help"),14,276,590,0xffbfdde0);}
    @Override public void onClose(){SimulationClient.back();}
}
