package dev.jarofcucumbers.prscraft;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;

/** Six-way cable with matching visual and collision arms. */
public final class CableBlock extends PipeBlock implements EntityBlock {
    public final SignalBudget.Medium medium;
    public CableBlock(SignalBudget.Medium medium, Properties properties) {
        super(0.125f, properties); this.medium = medium;
        BlockState state = stateDefinition.any();
        for (var property : PROPERTY_BY_DIRECTION.values()) state = state.setValue(property, false);
        registerDefaultState(state);
    }
    @Override public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new CableBlockEntity(pos,state);}
    @Override protected MapCodec<? extends PipeBlock> codec() { return simpleCodec(p -> new CableBlock(medium, p)); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        PROPERTY_BY_DIRECTION.values().forEach(builder::add);
    }
    private boolean connects(BlockState state) {
        return state.getBlock() instanceof CableBlock cable && cable.medium == medium
            || state.getBlock() instanceof PortBlock port && port.medium == medium;
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState();
        for (Direction d : Direction.values()) state = state.setValue(PROPERTY_BY_DIRECTION.get(d), connects(context.getLevel().getBlockState(context.getClickedPos().relative(d))));
        return state;
    }
    @Override protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return state.setValue(PROPERTY_BY_DIRECTION.get(direction), connects(neighbor));
    }
}
