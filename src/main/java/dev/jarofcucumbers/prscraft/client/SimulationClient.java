package dev.jarofcucumbers.prscraft.client;
import dev.jarofcucumbers.prscraft.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.network.PacketDistributor;
/** A virtual session always routes writes through its physical simulator. Never sends virtual positions to the world API. */
public final class SimulationClient{
    public static BlockPos root;public static int node;private static SimulatorScreen canvas;
    public static void graph(BlockPos pos,CompoundTag data,boolean open){if(open||root==null||!root.equals(pos)){root=pos;canvas=new SimulatorScreen(pos,data);}else canvas.receive(data);if(open)Minecraft.getInstance().setScreen(canvas);else if(Minecraft.getInstance().screen instanceof SimulatorScreen)Minecraft.getInstance().setScreen(canvas);}
    public static void context(CompoundTag data){root=BlockPos.of(data.getLong("simulatorRoot"));node=data.getInt("simulatorNode");}
    public static void send(String action,CompoundTag data){if(root!=null)PacketDistributor.sendToServer(new SimulatorNetwork.Request(root,node,action,data));}
    public static void back(){if(canvas!=null){Minecraft.getInstance().setScreen(canvas);send("refresh",new CompoundTag());}else{root=null;Minecraft.getInstance().setScreen(null);}}
    public static void clear(){root=null;canvas=null;}
}
