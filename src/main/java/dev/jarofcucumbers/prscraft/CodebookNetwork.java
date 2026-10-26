package dev.jarofcucumbers.prscraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
public final class CodebookNetwork{
    private CodebookNetwork(){}
    public record Edit(BlockPos pos,boolean inDevice,boolean offhand,String mapping) implements CustomPacketPayload{
        public static final Type<Edit> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(PRSCraft.ID,"codebook_edit"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Edit> CODEC=new StreamCodec<>(){public Edit decode(RegistryFriendlyByteBuf b){return new Edit(b.readBlockPos(),b.readBoolean(),b.readBoolean(),b.readUtf(8192));}public void encode(RegistryFriendlyByteBuf b,Edit p){b.writeBlockPos(p.pos);b.writeBoolean(p.inDevice);b.writeBoolean(p.offhand);b.writeUtf(p.mapping,8192);}};
        public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    private static ItemStack card(ServerPlayer p,BlockPos pos,boolean inDevice,boolean offhand){
        if(!inDevice)return p.getItemInHand(offhand?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND);
        if(p.distanceToSqr(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5)>64||!p.serverLevel().hasChunkAt(pos)||!p.mayBuild()||!p.serverLevel().mayInteract(p,pos)||!(p.level().getBlockEntity(pos) instanceof RadioBlockEntity radio))return ItemStack.EMPTY;
        return radio.media.getStackInSlot(2);
    }
    public static void register(RegisterPayloadHandlersEvent e){e.registrar("1").playToServer(Edit.TYPE,Edit.CODEC,(request,context)->context.enqueueWork(()->{
        if(!(context.player() instanceof ServerPlayer player))return;var held=card(player,request.pos,request.inDevice,request.offhand);if(!held.is(PRSCraft.CODEBOOK_CARD.get()))return;String error="";
        try{SymbolCodebook.parse(request.mapping);var old=held.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);var tag=old==null?new CompoundTag():old.copyTag();tag.putString("codebook",request.mapping);held.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(tag));if(request.inDevice&&player.level().getBlockEntity(request.pos) instanceof RadioBlockEntity radio)radio.setChanged();player.inventoryMenu.broadcastChanges();Milestones.award(player,"codebook_saved");}catch(IllegalArgumentException ex){error=ex.getMessage();}
        open(player,request.pos,request.inDevice,request.offhand,false,error);
    }));}
    public static void open(ServerPlayer player,BlockPos pos,boolean inDevice,boolean offhand,boolean open,String error){var stack=card(player,pos,inDevice,offhand);if(!stack.is(PRSCraft.CODEBOOK_CARD.get()))return;var data=new CompoundTag();data.putString("kind","codebook_editor");data.putString("mapping",CodebookCardItem.mapping(stack));data.putBoolean("inDevice",inDevice);data.putBoolean("offhand",offhand);data.putString("error",error);PacketDistributor.sendToPlayer(player,new RadioNetwork.Snapshot(pos,open,data));}
}
