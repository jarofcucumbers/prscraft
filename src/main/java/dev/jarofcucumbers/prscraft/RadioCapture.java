package dev.jarofcucumbers.prscraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
public final class RadioCapture{
    private RadioCapture(){}
    public static CompoundTag capture(Level level,BlockPos pos){return CircuitCapture.capture(level,pos);}
}
