package dev.jarofcucumbers.prscraft;
import net.minecraft.core.*;
import net.minecraft.world.level.Level;
import java.util.*;
/** A passive segment ends at a device; active processing is evaluated separately. */
public final class CircuitNetwork{
    private CircuitNetwork(){}
    public record Route(BlockPos endpoint,double metres,double lossDb,double noiseVariance,double damageLoss,SignalBudget.Medium medium,List<BlockPos> path){}
    private record Visit(BlockPos pos,double distance){}
    public static boolean passive(Level level,BlockPos pos,SignalBudget.Medium medium){if(!level.hasChunkAt(pos))return false;var b=level.getBlockState(pos).getBlock();return b instanceof CableBlock c&&c.medium==medium||b instanceof PortBlock p&&p.medium==medium;}
    public static CableSettings cable(Level level,BlockPos pos){return level.getBlockEntity(pos) instanceof CableBlockEntity c?c.settings():CableSettings.DEFAULT;}
    public static List<Route> inputs(Level level,BlockPos root,Direction side,Double carrier){
        Map<BlockPos,Route> candidates=new HashMap<>();
        for(var medium:SignalBudget.Medium.values()){
            Map<BlockPos,Double> distances=new HashMap<>();Map<BlockPos,BlockPos> parent=new HashMap<>();Set<BlockPos> settled=new HashSet<>();
            PriorityQueue<Visit> queue=new PriorityQueue<>(Comparator.comparingDouble(Visit::distance));
            for(Direction d:Direction.values())if(side==null||d==side){var port=root.relative(d);if(level.hasChunkAt(port)&&level.getBlockState(port).getBlock() instanceof PortBlock p&&p.medium==medium){distances.put(port,1.0);queue.add(new Visit(port,1));}}
            while(!queue.isEmpty()&&settled.size()<512){var visit=queue.remove();var pos=visit.pos();if(!settled.add(pos))continue;
                if(level.getBlockState(pos).getBlock() instanceof PortBlock){
                    for(Direction d:Direction.values()){
                        var endpoint=pos.relative(d);if(endpoint.equals(root)||!level.hasChunkAt(endpoint)||!(level.getBlockEntity(endpoint) instanceof RadioBlockEntity device)||!device.settings().enabled())continue;
                        if(device.isSource()){
                            if(!device.settings().transmit()||carrier!=null&&Math.abs(carrier-device.settings().carrierMHz())>.001)continue;
                        }else if(!pos.equals(endpoint.relative(device.getBlockState().getValue(TransmitterBlock.FACING))))continue;
                        List<BlockPos> path=new ArrayList<>();BlockPos cursor=pos;double extra=0,variance=0;
                        while(cursor!=null){path.add(cursor);if(level.getBlockState(cursor).getBlock() instanceof CableBlock){var c=cable(level,cursor);extra+=c.extraLossDb();variance+=c.noiseVariance();}cursor=parent.get(cursor);}
                        double metres=visit.distance()+1,frequency=carrier==null?device.settings().carrierMHz():carrier;
                        double loss=medium.lossDb(metres,frequency)+medium.lossDb(0,frequency)+extra;
                        var candidate=new Route(endpoint.immutable(),metres,loss,variance,extra,medium,List.copyOf(path));
                        var old=candidates.get(endpoint);if(old==null||candidate.lossDb()<old.lossDb())candidates.put(endpoint,candidate);
                    }
                }
                for(Direction d:Direction.values()){
                    var next=pos.relative(d);if(!passive(level,next,medium)||settled.contains(next))continue;
                    double weight=level.getBlockState(next).getBlock() instanceof CableBlock?cable(level,next).metres():1;
                    double distance=visit.distance()+weight;
                    if(distance<distances.getOrDefault(next,Double.POSITIVE_INFINITY)&&distances.size()<1024){distances.put(next,distance);parent.put(next,pos);queue.add(new Visit(next,distance));}
                }
            }
        }
        return candidates.values().stream().sorted(Comparator.comparingDouble(Route::lossDb).thenComparing(r->r.endpoint().asLong())).limit(16).toList();
    }
}
