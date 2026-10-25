package dev.jarofcucumbers.prscraft;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
public final class BitGeneratorBlock extends TransmitterBlock {
    public BitGeneratorBlock(Properties properties){super(properties);}
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec(){return simpleCodec(BitGeneratorBlock::new);}
}
