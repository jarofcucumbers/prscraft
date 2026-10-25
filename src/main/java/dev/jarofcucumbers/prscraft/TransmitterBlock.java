package dev.jarofcucumbers.prscraft;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;

public final class TransmitterBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<TransmitterBlock> CODEC = simpleCodec(TransmitterBlock::new);
    public static final IntegerProperty CHANNEL = IntegerProperty.create("channel", 0, 3);
    public static final BooleanProperty POWERED = BooleanProperty.create("powered");
    public TransmitterBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.NORTH)
            .setValue(CHANNEL, 0).setValue(POWERED, true));
    }
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING, CHANNEL, POWERED); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) { return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()); }
    public static double frequency(BlockState state) { return 100 + 25 * state.getValue(CHANNEL); }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) {
            BlockState next = player.isShiftKeyDown() ? state.cycle(POWERED) : state.cycle(CHANNEL);
            level.setBlock(pos, next, 3);
            player.displayClientMessage(Component.translatable("message.prscraft.transmitter", frequency(next), 20,
                Component.translatable(next.getValue(POWERED) ? "message.prscraft.on" : "message.prscraft.off")), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
