package dev.jarofcucumbers.prscraft;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
public final class ProcessorBlock extends TransmitterBlock {
    public final String kind;
    public ProcessorBlock(String kind,Properties properties){super(properties);this.kind=kind;}
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec(){return simpleCodec(p->new ProcessorBlock(kind,p));}
}
