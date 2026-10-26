package dev.jarofcucumbers.prscraft;
import net.minecraft.core.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import java.util.List;
/** The same signal engine reads either the physical level or a bounded virtual laboratory. */
public interface CircuitAccess{
    boolean hasChunkAt(BlockPos pos);BlockState getBlockState(BlockPos pos);BlockEntity getBlockEntity(BlockPos pos);long getGameTime();
    default boolean physicalInterference(){return true;}
    default List<CircuitNetwork.Route> routes(BlockPos root,Direction side,Double carrier){return null;}
    default EnvironmentEffect environment(BlockPos pos,SignalBudget.Medium medium,double metres){return EnvironmentEffect.NONE;}
    static CircuitAccess of(Level level){return new CircuitAccess(){
        public boolean hasChunkAt(BlockPos p){return level.hasChunkAt(p);}public BlockState getBlockState(BlockPos p){return level.getBlockState(p);}public BlockEntity getBlockEntity(BlockPos p){return level.getBlockEntity(p);}public long getGameTime(){return level.getGameTime();}
        public EnvironmentEffect environment(BlockPos p,SignalBudget.Medium m,double length){int wet=0,hot=0,em=0,shield=0;for(Direction d:Direction.values()){var at=p.relative(d);if(!level.hasChunkAt(at))continue;var s=level.getBlockState(at);if(s.is(net.minecraft.world.level.block.Blocks.WATER))wet++;if(s.is(net.minecraft.world.level.block.Blocks.LAVA))hot++;if(s.is(net.minecraft.world.level.block.Blocks.REDSTONE_BLOCK))em++;if(s.is(net.minecraft.world.level.block.Blocks.IRON_BLOCK))shield++;}boolean exposed=level.canSeeSky(p.above());boolean rain=exposed&&level.isRaining();boolean snow=rain&&level.getBiome(p).value().coldEnoughToSnow(p);return EnvironmentEffect.model(m,length,rain&&!snow,snow,wet,hot,em,shield);}
    };}
}
