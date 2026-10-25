package dev.jarofcucumbers.prscraft;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.*;
/** Read-only local-segment inspection; a client cannot nominate an unrelated block entity. */
public final class AnalyzerService {
    private AnalyzerService(){}
    public static CompoundTag scan(Level level,BlockPos portPos,BlockPos selected){
        var data=new CompoundTag();data.putString("kind","analyzer");
        if(!level.hasChunkAt(portPos)||!(level.getBlockState(portPos).getBlock() instanceof PortBlock port))return data;
        var nodes=NetworkWalk.distances(portPos,node->{List<BlockPos> next=new ArrayList<>();for(Direction d:Direction.values()){
            BlockPos pos=node.relative(d);if(!level.hasChunkAt(pos))continue;var block=level.getBlockState(pos).getBlock();
            if(block instanceof CableBlock cable&&cable.medium==port.medium||block instanceof PortBlock other&&other.medium==port.medium)next.add(pos);
        }return next;},256);
        data.putString("medium",port.medium.name());data.putInt("nodes",nodes.size());data.putBoolean("truncated",nodes.size()>=256);
        ListTag devices=new ListTag();Set<BlockPos> seen=new HashSet<>();BlockPos chosen=null,first=null;
        for(var entry:nodes.entrySet()){
            if(!(level.getBlockState(entry.getKey()).getBlock() instanceof PortBlock))continue;
            for(Direction d:Direction.values()){
                BlockPos pos=entry.getKey().relative(d);if(!level.hasChunkAt(pos)||!seen.add(pos)||!(level.getBlockEntity(pos) instanceof RadioBlockEntity radio))continue;
                var device=new CompoundTag();device.putInt("x",pos.getX());device.putInt("y",pos.getY());device.putInt("z",pos.getZ());device.putString("kind",radio.kind());
                device.put("settings",radio.settings().save());device.put("processor",radio.processor().save());device.putInt("metres",entry.getValue()+1);
                device.putDouble("loss",port.medium.lossDb(entry.getValue()+1,radio.settings().carrierMHz())+port.medium.lossDb(0,radio.settings().carrierMHz()));
                devices.add(device);if(first==null)first=pos.immutable();if(pos.equals(selected))chosen=pos.immutable();
                if(devices.size()>=8)break;
            }if(devices.size()>=8)break;
        }
        data.put("devices",devices);if(chosen==null)chosen=first;
        if(chosen!=null){
            for(int i=0;i<devices.size();i++){var item=devices.getCompound(i);if(item.getInt("x")==chosen.getX()&&item.getInt("y")==chosen.getY()&&item.getInt("z")==chosen.getZ()){data.putInt("portMetres",item.getInt("metres"));data.putDouble("portLoss",item.getDouble("loss"));}}
            data.putInt("selectedX",chosen.getX());data.putInt("selectedY",chosen.getY());data.putInt("selectedZ",chosen.getZ());data.put("capture",RadioCapture.capture(level,chosen));}
        return data;
    }
    public static void send(ServerPlayer player,BlockPos port,BlockPos selected,boolean open){PacketDistributor.sendToPlayer(player,new RadioNetwork.Snapshot(port,open,scan(player.level(),port,selected)));}
}
