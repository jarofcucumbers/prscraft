package dev.jarofcucumbers.prscraft;
import net.minecraft.core.*;
import net.minecraft.world.level.Level;
public final class ChannelNoise{
    private ChannelNoise(){}
    public static double interferenceRms(Level level, BlockPos receiver, BlockPos sourcePos, SignalBudget.Medium medium, RadioSettings settings) {
        if (!settings.thermal() || medium.coupling() == 0) return 0;
        double mw = 0;
        for (BlockPos pos : BlockPos.betweenClosed(receiver.offset(-4,-4,-4), receiver.offset(4,4,4))) {
            if (pos.equals(sourcePos) || !level.hasChunkAt(pos)) continue;
            if (level.getBlockEntity(pos) instanceof RadioBlockEntity radio) {
                if (!radio.isSource()) continue;
                RadioSettings source = radio.settings();
                if (!source.transmit() || !source.enabled()) continue;
                double overlap = SignalBudget.overlap(settings.carrierMHz(), source.carrierMHz(), 40);
                mw += Math.pow(10, source.powerDbm() / 10) * 0.0001 * medium.coupling() * overlap / Math.max(1, pos.distSqr(receiver));
            }
        }
        return Math.sqrt(mw / 100);
    }
}
