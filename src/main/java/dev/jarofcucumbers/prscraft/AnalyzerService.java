package dev.jarofcucumbers.prscraft;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.*;
/** Physical whole-circuit discovery is undirected; signal evaluation remains directional. */
public final class AnalyzerService{
    private AnalyzerService(){}
    public static String role(RadioBlockEntity device,Direction side){
        if(device.isSource())return device.settings().transmit()?"TX":"RX";
        Direction front=device.getBlockState().getValue(TransmitterBlock.FACING);
        if(side==front)return "OUT";if(side==front.getOpposite())return "A";
        if((device.kind().equals("adder")||device.kind().equals("correlator"))&&side==front.getCounterClockWise())return "B";
        return "unused";
    }
    private static SignalBudget.Medium medium(Level level,BlockPos pos){var b=level.getBlockState(pos).getBlock();return b instanceof CableBlock cable?cable.medium:b instanceof PortBlock port?port.medium:null;}
    public static CompoundTag scan(Level level,BlockPos origin,BlockPos selected){
        var data=new CompoundTag();data.putString("kind","analyzer");data.putString("scope","whole_circuit");
        if(!level.hasChunkAt(origin)||!(level.getBlockState(origin).getBlock() instanceof PortBlock start))return data;
        Set<BlockPos> nodes=new LinkedHashSet<>();ArrayDeque<BlockPos> queue=new ArrayDeque<>();nodes.add(origin);queue.add(origin);
        while(!queue.isEmpty()){
            var pos=queue.remove();var block=level.getBlockState(pos).getBlock();boolean device=level.getBlockEntity(pos) instanceof RadioBlockEntity;
            for(Direction d:Direction.values()){
                var next=pos.relative(d);if(!level.hasChunkAt(next)||nodes.contains(next)||nodes.size()>=1024)continue;
                var other=level.getBlockState(next).getBlock();boolean otherDevice=level.getBlockEntity(next) instanceof RadioBlockEntity;
                boolean connected=device?other instanceof PortBlock:otherDevice?block instanceof PortBlock:medium(level,pos)!=null&&medium(level,pos)==medium(level,next);
                if(connected){nodes.add(next.immutable());queue.add(next.immutable());}
            }
        }
        int ports=0,cables=0,deviceCount=0;double wireMetres=0;ListTag devices=new ListTag();List<BlockPos> devicePositions=new ArrayList<>();BlockPos chosen=null,first=null;
        for(var pos:nodes){
            var block=level.getBlockState(pos).getBlock();if(block instanceof PortBlock)ports++;
            if(block instanceof CableBlock){cables++;wireMetres+=CircuitNetwork.cable(level,pos).metres();}
            if(!(level.getBlockEntity(pos) instanceof RadioBlockEntity radio))continue;deviceCount++;if(devices.size()>=32)continue;
            var tag=new CompoundTag();tag.putInt("x",pos.getX());tag.putInt("y",pos.getY());tag.putInt("z",pos.getZ());tag.putString("kind",radio.kind());tag.put("settings",radio.settings().save());tag.put("processor",radio.processor().save());tag.put("dsp",radio.dsp().save());
            ListTag roles=new ListTag();StringJoiner text=new StringJoiner(" · ");
            for(Direction d:Direction.values()){var port=pos.relative(d);if(level.hasChunkAt(port)&&level.getBlockState(port).getBlock() instanceof PortBlock){var r=new CompoundTag();r.putString("side",d.getName());r.putString("role",role(radio,d));roles.add(r);text.add(d.getName()+":"+role(radio,d));}}
            tag.put("ports",roles);tag.putString("portRoles",text.toString());devices.add(tag);devicePositions.add(pos);
            if(first==null)first=pos;if(pos.equals(selected))chosen=pos;
        }
        // Collapse each connected passive bus into edges between its attached devices.
        Map<BlockPos,Integer> groups=new HashMap<>();int group=0;
        for(var pos:nodes){if(medium(level,pos)==null||groups.containsKey(pos))continue;var m=medium(level,pos);queue.clear();queue.add(pos);groups.put(pos,group);
            while(!queue.isEmpty()){var node=queue.remove();for(Direction d:Direction.values()){var next=node.relative(d);if(nodes.contains(next)&&!groups.containsKey(next)&&medium(level,next)==m){groups.put(next,group);queue.add(next);}}}group++;
        }
        ListTag connections=new ListTag();
        for(int g=0;g<group;g++){List<Integer> members=new ArrayList<>();for(int i=0;i<devicePositions.size();i++){
                for(Direction d:Direction.values())if(Objects.equals(groups.get(devicePositions.get(i).relative(d)),g)){members.add(i);break;}}
            for(int i=0;i<members.size();i++)for(int j=i+1;j<members.size();j++)if(connections.size()<128){var edge=new CompoundTag();edge.putInt("a",members.get(i));edge.putInt("b",members.get(j));edge.putInt("bus",g);connections.add(edge);}}
        data.put("connections",connections);data.putInt("nodes",nodes.size());data.putInt("ports",ports);data.putInt("cables",cables);data.putInt("deviceCount",deviceCount);data.putDouble("wireMetres",wireMetres);data.putString("medium",start.medium.name());data.putBoolean("truncated",nodes.size()>=1024||deviceCount>32);data.put("devices",devices);
        if(chosen==null)chosen=first;
        if(chosen!=null){data.putInt("selectedX",chosen.getX());data.putInt("selectedY",chosen.getY());data.putInt("selectedZ",chosen.getZ());
            for(int i=0;i<devices.size();i++){var t=devices.getCompound(i);if(t.getInt("x")==chosen.getX()&&t.getInt("y")==chosen.getY()&&t.getInt("z")==chosen.getZ())data.putString("portRoles",t.getString("portRoles"));}
            data.put("capture",RadioCapture.capture(level,chosen));}
        return data;
    }
    public static void send(ServerPlayer player,BlockPos port,BlockPos selected,boolean open){var data=scan(player.level(),port,selected);PacketDistributor.sendToPlayer(player,new RadioNetwork.Snapshot(port,open,data));if(data.getInt("deviceCount")>=3)Milestones.award(player,"survey");}
}
