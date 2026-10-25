package dev.jarofcucumbers.prscraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
public final class CableTuning{
    private CableTuning(){}
    public static void send(ServerPlayer player,BlockPos pos,boolean open,String error){
        if(!(player.level().getBlockEntity(pos) instanceof CableBlockEntity cable))return;
        var data=new CompoundTag();data.putString("kind","cable");data.putDouble("metres",cable.settings().metres());data.putDouble("damage",cable.settings().damage());data.putString("error",error);
        data.putString("medium",((CableBlock)cable.getBlockState().getBlock()).medium.name());
        PacketDistributor.sendToPlayer(player,new RadioNetwork.Snapshot(pos,open,data));
    }
}
