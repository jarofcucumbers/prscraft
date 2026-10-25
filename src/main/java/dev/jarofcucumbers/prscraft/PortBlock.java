package dev.jarofcucumbers.prscraft;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class PortBlock extends Block {
    public static final net.minecraft.world.level.block.state.properties.DirectionProperty FACING=net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING;
    public final SignalBudget.Medium medium;
    public PortBlock(SignalBudget.Medium medium, Properties properties) { super(properties); this.medium = medium;registerDefaultState(stateDefinition.any().setValue(FACING,net.minecraft.core.Direction.UP)); }
    @Override protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<Block,BlockState> builder){builder.add(FACING);}
    @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context){return defaultBlockState().setValue(FACING,context.getClickedFace());}
    @Override protected BlockState rotate(BlockState state,net.minecraft.world.level.block.Rotation rotation){return state.setValue(FACING,rotation.rotate(state.getValue(FACING)));}
    @Override protected BlockState mirror(BlockState state,net.minecraft.world.level.block.Mirror mirror){return rotate(state,mirror.getRotation(state.getValue(FACING)));}
    @Override protected net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state,net.minecraft.world.level.BlockGetter level,BlockPos pos,net.minecraft.world.phys.shapes.CollisionContext context){
        net.minecraft.world.phys.shapes.VoxelShape result=net.minecraft.world.phys.shapes.Shapes.empty();
        for(double[] b:new double[][]{{3,0,3,13,3,13},{5,3,5,11,12,11},{4,5,4,12,8,12},{6,12,6,10,15,10}}){
            double[] lo={16,16,16},hi={0,0,0};for(int i=0;i<8;i++){
                double x=b[(i&1)==0?0:3],y=b[(i&2)==0?1:4],z=b[(i&4)==0?2:5];
                double[] v=switch(state.getValue(FACING)){case UP->new double[]{x,y,z};case DOWN->new double[]{x,16-y,16-z};case NORTH->new double[]{x,z,16-y};case SOUTH->new double[]{x,16-z,y};case EAST->new double[]{y,16-x,z};case WEST->new double[]{16-y,x,z};};
                for(int a=0;a<3;a++){lo[a]=Math.min(lo[a],v[a]);hi[a]=Math.max(hi[a],v[a]);}
            }result=net.minecraft.world.phys.shapes.Shapes.or(result,Block.box(lo[0],lo[1],lo[2],hi[0],hi[1],hi[2]));
        }return result;
    }
    @Override protected net.minecraft.world.ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack stack,
        BlockState state, Level level, BlockPos pos, Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
        return stack.isEmpty() ? net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
            : net.minecraft.world.ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) player.displayClientMessage(Component.translatable("message.prscraft.port", medium.name(),Component.translatable("direction.prscraft."+state.getValue(FACING).getName())), true);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
