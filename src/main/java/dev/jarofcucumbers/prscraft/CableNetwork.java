package dev.jarofcucumbers.prscraft;

import net.minecraft.core.*;
import net.minecraft.world.level.Level;
import java.util.*;

/** Traversal is demand-driven, loaded-chunk-only and bounded to 256 nodes per medium. */
public final class CableNetwork {
    private CableNetwork() {}
    public record Link(RadioSettings source, int metres, SignalBudget.Medium medium, double lossDb, BlockPos sourcePos, List<SignalChain.Stage> processors) {}
    public static Link receive(Level level, BlockPos receiver, RadioSettings config) {return find(level,receiver,config,true);}
    public static Link monitor(Level level, BlockPos receiver, RadioSettings config) {return find(level,receiver,config,false);}
    private static Link find(Level level, BlockPos receiver, RadioSettings config, boolean matchCarrier) {
        Link best = null;
        if (!config.enabled() || config.transmit()) return null;
        for (SignalBudget.Medium medium : SignalBudget.Medium.values()) {
            // A virtual receiver root; all other vertices must be compatible ports/cables.
            Map<BlockPos,BlockPos> parents = new HashMap<>();
            Map<BlockPos,Integer> nodes = NetworkWalk.distances(receiver, node -> {
                List<BlockPos> neighbors = new ArrayList<>();
                if (level.getBlockState(node).getBlock() instanceof ProcessorBlock) {
                    BlockPos input=node.relative(level.getBlockState(node).getValue(TransmitterBlock.FACING).getOpposite());
                    if (level.hasChunkAt(input) && level.getBlockState(input).getBlock() instanceof PortBlock port && port.medium==medium) {
                        neighbors.add(input); parents.putIfAbsent(input,node);
                    }
                    return neighbors;
                }
                for (Direction d : Direction.values()) {
                    BlockPos next = node.relative(d);
                    if (next.equals(receiver) || !level.hasChunkAt(next)) continue;
                    var block = level.getBlockState(next).getBlock();
                    boolean port = block instanceof PortBlock portBlock && portBlock.medium == medium;
                    boolean cable = block instanceof CableBlock cableBlock && cableBlock.medium == medium;
                    boolean processor = !node.equals(receiver) && level.getBlockState(node).getBlock() instanceof PortBlock
                        && block instanceof ProcessorBlock && node.equals(next.relative(level.getBlockState(next).getValue(TransmitterBlock.FACING)))
                        && level.getBlockEntity(next) instanceof RadioBlockEntity device && device.settings().enabled();
                    if (port || (!node.equals(receiver) && cable) || processor) { neighbors.add(next); parents.putIfAbsent(next,node); }
                }
                return neighbors;
            }, 256);
            for (var entry : nodes.entrySet()) {
                BlockPos node = entry.getKey();
                if (node.equals(receiver) || !(level.getBlockState(node).getBlock() instanceof PortBlock)) continue;
                for (Direction d : Direction.values()) {
                    BlockPos next = node.relative(d);
                    if (next.equals(receiver) || !level.hasChunkAt(next)) continue;
                    if (level.getBlockEntity(next) instanceof RadioBlockEntity radio) {
                        if (!radio.isSource()) continue;
                        List<SignalChain.Stage> processors=new ArrayList<>();
                        BlockPos cursor=node;
                        while(cursor!=null && !cursor.equals(receiver)) {
                            if(level.getBlockEntity(cursor) instanceof RadioBlockEntity device && !device.kind().equals("radio"))
                                processors.add(new SignalChain.Stage(device.kind(),device.processor()));
                            cursor=parents.get(cursor);
                        }
                        if(processors.size()>8)continue;
                        RadioSettings source = radio.settings();
                        if (!source.enabled() || !source.transmit() || matchCarrier && Math.abs(source.carrierMHz() - config.carrierMHz()) > 0.001) continue;
                        int metres = entry.getValue() + 1;
                        double loss = medium.lossDb(metres, source.carrierMHz()) + medium.lossDb(0, source.carrierMHz()) * (1+2*processors.size());
                        Link candidate=new Link(source, metres, medium, loss, next.immutable(), List.copyOf(processors));
                        if (best == null || strength(candidate)>strength(best))best=candidate;
                    }
                }
            }
        }
        return best;
    }
    private static double strength(Link link){return link.source().powerDbm()-link.lossDb()+link.processors().stream()
        .filter(stage->stage.kind().equals("amplifier")).mapToDouble(stage->stage.settings().gainDb()).sum();}
    public static double interferenceRms(Level level, BlockPos receiver, Link link, RadioSettings settings) {
        if (link == null || !settings.thermal() || link.medium().coupling() == 0) return 0;
        double mw = 0;
        for (BlockPos pos : BlockPos.betweenClosed(receiver.offset(-4,-4,-4), receiver.offset(4,4,4))) {
            if (pos.equals(link.sourcePos()) || !level.hasChunkAt(pos)) continue;
            if (level.getBlockEntity(pos) instanceof RadioBlockEntity radio) {
                if (!radio.isSource()) continue;
                RadioSettings source = radio.settings();
                if (!source.transmit() || !source.enabled()) continue;
                double overlap = SignalBudget.overlap(settings.carrierMHz(), source.carrierMHz(), 40);
                mw += Math.pow(10, source.powerDbm() / 10) * 0.0001 * link.medium().coupling() * overlap / Math.max(1, pos.distSqr(receiver));
            }
        }
        return Math.sqrt(mw / 100);
    }
}
