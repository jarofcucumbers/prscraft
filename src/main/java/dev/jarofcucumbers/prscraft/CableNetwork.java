package dev.jarofcucumbers.prscraft;

import net.minecraft.core.*;
import net.minecraft.world.level.Level;
import java.util.*;

/** Traversal is demand-driven, loaded-chunk-only and bounded to 256 nodes per medium. */
public final class CableNetwork {
    private CableNetwork() {}
    public record Link(RadioSettings source, int metres, SignalBudget.Medium medium, double lossDb, BlockPos sourcePos) {}
    public static Link receive(Level level, BlockPos receiver, RadioSettings config) {
        Link best = null;
        if (!config.enabled() || config.transmit()) return null;
        for (SignalBudget.Medium medium : SignalBudget.Medium.values()) {
            // A virtual receiver root; all other vertices must be compatible ports/cables.
            Map<BlockPos,Integer> nodes = NetworkWalk.distances(receiver, node -> {
                List<BlockPos> neighbors = new ArrayList<>();
                for (Direction d : Direction.values()) {
                    BlockPos next = node.relative(d);
                    if (next.equals(receiver) || !level.hasChunkAt(next)) continue;
                    var block = level.getBlockState(next).getBlock();
                    boolean port = block instanceof PortBlock portBlock && portBlock.medium == medium;
                    boolean cable = block instanceof CableBlock cableBlock && cableBlock.medium == medium;
                    if (port || (!node.equals(receiver) && cable)) neighbors.add(next);
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
                        RadioSettings source = radio.settings();
                        if (!source.enabled() || !source.transmit() || Math.abs(source.carrierMHz() - config.carrierMHz()) > 0.001) continue;
                        int metres = entry.getValue() + 1;
                        double loss = medium.lossDb(metres, source.carrierMHz()) + medium.lossDb(0, source.carrierMHz());
                        if (best == null || source.powerDbm() - loss > best.source().powerDbm() - best.lossDb())
                            best = new Link(source, metres, medium, loss, next.immutable());
                    }
                }
            }
        }
        return best;
    }
    public static double interferenceRms(Level level, BlockPos receiver, Link link, RadioSettings settings) {
        if (link == null || !settings.thermal() || link.medium().coupling() == 0) return 0;
        double mw = 0;
        for (BlockPos pos : BlockPos.betweenClosed(receiver.offset(-4,-4,-4), receiver.offset(4,4,4))) {
            if (pos.equals(link.sourcePos()) || !level.hasChunkAt(pos)) continue;
            if (level.getBlockEntity(pos) instanceof RadioBlockEntity radio) {
                RadioSettings source = radio.settings();
                if (!source.transmit() || !source.enabled()) continue;
                double overlap = SignalBudget.overlap(settings.carrierMHz(), source.carrierMHz(), 40);
                mw += Math.pow(10, source.powerDbm() / 10) * 0.0001 * link.medium().coupling() * overlap / Math.max(1, pos.distSqr(receiver));
            }
        }
        return Math.sqrt(mw / 100);
    }
}
