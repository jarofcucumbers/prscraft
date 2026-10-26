package dev.jarofcucumbers.prscraft;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
public final class CoatNetwork{
    public record Inspect(BlockPos pos) implements CustomPacketPayload{public static final Type<Inspect> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(PRSCraft.ID,"coat_inspect"));public static final StreamCodec<RegistryFriendlyByteBuf,Inspect> CODEC=StreamCodec.composite(BlockPos.STREAM_CODEC,Inspect::pos,Inspect::new);public Type<? extends CustomPacketPayload> type(){return TYPE;}}
    public static void register(RegisterPayloadHandlersEvent e){e.registrar("1").playToServer(Inspect.TYPE,Inspect.CODEC,(request,context)->context.enqueueWork(()->{if(!(context.player() instanceof ServerPlayer player)||!player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST).is(PRSCraft.DOCTORAL_COAT.get()))return;var pos=request.pos;if(player.distanceToSqr(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5)>64||!player.serverLevel().hasChunkAt(pos))return;long tick=player.level().getGameTime();var budget=player.getPersistentData();if(budget.contains("prscraft_coat_tick")&&tick-budget.getLong("prscraft_coat_tick")<5)return;budget.putLong("prscraft_coat_tick",tick);var data=new CompoundTag();data.putString("kind","coat_hud");var block=player.level().getBlockState(pos).getBlock();data.putString("block",net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block).toString());if(player.level().getBlockEntity(pos) instanceof RadioBlockEntity radio){data.put("template",DeviceConfiguration.template(radio));data.putString("role",radio.isSource()?(radio.settings().transmit()?"TX":"RX"):java.util.Set.of("adder","correlator","orthogonality_meter").contains(radio.kind())?"A/B → OUT":"A → OUT");}else if(player.level().getBlockEntity(pos) instanceof CableBlockEntity cable){data.putDouble("metres",cable.settings().metres());data.putDouble("damage",cable.settings().damage());data.putDouble("dispersion",cable.settings().dispersion());}else if(block instanceof PortBlock port){data.putString("medium",port.medium.name());}else if(player.level().getBlockEntity(pos) instanceof SimulatorBlockEntity sim){data.putInt("nodes",sim.document().nodes().size());data.putInt("wires",sim.document().wires().size());}else return;Milestones.award(player,"lab_coat");PacketDistributor.sendToPlayer(player,new RadioNetwork.Snapshot(pos,false,data));}));}
}
