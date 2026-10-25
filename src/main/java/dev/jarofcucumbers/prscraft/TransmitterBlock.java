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

public class TransmitterBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final MapCodec<TransmitterBlock> CODEC = simpleCodec(TransmitterBlock::new);
    public static final IntegerProperty CHANNEL = IntegerProperty.create("channel", 0, 3);
    public static final BooleanProperty POWERED = BooleanProperty.create("powered");
    public TransmitterBlock(Properties properties) {
        super(properties.noOcclusion());
        registerDefaultState(stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.NORTH)
            .setValue(CHANNEL, 0).setValue(POWERED, true));
    }
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING, CHANNEL, POWERED); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) { return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()); }
    @Override public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new RadioBlockEntity(pos, state); }
    public static double frequency(BlockState state) { return 100 + 25 * state.getValue(CHANNEL); }
    @Override protected net.minecraft.world.ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack stack,
        BlockState state, Level level, BlockPos pos, Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
        return stack.isEmpty() ? net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
            : net.minecraft.world.ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
    }
    @Override protected void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){
        if(!state.is(next.getBlock()) && level.getBlockEntity(pos) instanceof RadioBlockEntity radio){
            for(int slot=0;slot<2;slot++)net.minecraft.world.Containers.dropItemStack(level,pos.getX(),pos.getY(),pos.getZ(),radio.media.getStackInSlot(slot));
        }
        super.onRemove(state,level,pos,next,moving);
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) {
            if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) RadioNetwork.send(serverPlayer, pos, true, "");
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
