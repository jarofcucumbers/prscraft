package dev.jarofcucumbers.prscraft;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import java.util.Locale;

/** Straight, same-medium links; bounded loaded-chunk scan performed only on interaction. */
public final class PortBlock extends Block {
    private final SignalBudget.Medium medium;
    public PortBlock(SignalBudget.Medium medium, Properties properties) { super(properties); this.medium = medium; }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) {
            double best = Double.NEGATIVE_INFINITY;
            double frequency = 0;
            int length = 0;
            BlockPos source = null;
            double interferenceMw = 0;
            for (Direction direction : Direction.values()) {
                for (int distance = 1; distance <= 64; distance++) {
                    BlockPos target = pos.relative(direction, distance);
                    if (!level.hasChunkAt(target)) break;
                    BlockState other = level.getBlockState(target);
                    if (other.getBlock() instanceof TransmitterBlock && other.getValue(TransmitterBlock.POWERED)) {
                        double f = TransmitterBlock.frequency(other);
                        double dbm = SignalBudget.receivedDbm(20, medium, distance, f);
                        if (dbm > best) { best = dbm; frequency = f; length = distance; source = target; }
                        break;
                    }
                    if (!(other.getBlock() instanceof PortBlock port) || port.medium != medium) break;
                }
            }
            if (Double.isFinite(best)) {
                // Nearby transmitters couple into unshielded links according to channel overlap.
                for (BlockPos target : BlockPos.betweenClosed(pos.offset(-4, -4, -4), pos.offset(4, 4, 4))) {
                    if (!level.hasChunkAt(target)) continue;
                    BlockState other = level.getBlockState(target);
                    if (other.getBlock() instanceof TransmitterBlock && other.getValue(TransmitterBlock.POWERED)
                        && !target.equals(source)) {
                        double overlap = SignalBudget.overlap(frequency, TransmitterBlock.frequency(other), 40);
                        interferenceMw += medium.coupling() * overlap / Math.max(1, target.distSqr(pos));
                    }
                }
                double noiseDbm = 10 * Math.log10(1e-9 + interferenceMw);
                player.displayClientMessage(Component.translatable("message.prscraft.signal", length,
                    String.format(Locale.ROOT, "%.1f", best), String.format(Locale.ROOT, "%.1f", best - noiseDbm)), false);
            } else player.displayClientMessage(Component.translatable("message.prscraft.no_signal"), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
