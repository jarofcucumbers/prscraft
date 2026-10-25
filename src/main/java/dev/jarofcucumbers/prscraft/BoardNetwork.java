package dev.jarofcucumbers.prscraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
public final class BoardNetwork{
    private BoardNetwork(){}
    public record Stroke(BlockPos pos,boolean offhand,int[] pixels) implements CustomPacketPayload{
        public static final Type<Stroke> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(PRSCraft.ID,"board_stroke"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Stroke> CODEC=new StreamCodec<>(){
            public Stroke decode(RegistryFriendlyByteBuf b){return new Stroke(b.readBlockPos(),b.readBoolean(),b.readVarIntArray(256));}
            public void encode(RegistryFriendlyByteBuf b,Stroke s){b.writeBlockPos(s.pos);b.writeBoolean(s.offhand);b.writeVarIntArray(s.pixels);}
        };
        public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public static void register(RegisterPayloadHandlersEvent e){e.registrar("1").playToServer(Stroke.TYPE,Stroke.CODEC,(s,c)->c.enqueueWork(()->{
        if(!(c.player() instanceof ServerPlayer player))return;var level=player.serverLevel();
        if(player.distanceToSqr(s.pos.getX()+.5,s.pos.getY()+.5,s.pos.getZ()+.5)>64||!level.hasChunkAt(s.pos)||!player.mayBuild()||!level.mayInteract(player,s.pos)||!(level.getBlockEntity(s.pos) instanceof DrawingBoardEntity board))return;
        var held=player.getItemInHand(s.offhand?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND);if(!BoardTools.isTool(held))return;
        int[] pixels;try{pixels=BoardPixels.checked(s.pixels);}catch(IllegalArgumentException ignored){return;}
        long tick=level.getGameTime();var budget=player.getPersistentData();
        if(budget.getLong("prscraft_draw_tick")!=tick){budget.putLong("prscraft_draw_tick",tick);budget.putInt("prscraft_draw_cost",0);}
        int spent=budget.getInt("prscraft_draw_cost");if(spent+pixels.length>1024)return;budget.putInt("prscraft_draw_cost",spent+pixels.length);
        BoardTools.apply(board,held,pixels);player.inventoryMenu.broadcastChanges();
        if(ChallengeRules.mural(board.pixels()))Milestones.award(player,"board_artist");
        if(ChallengeRules.fullPalette(board.pixels()))Milestones.award(player,"full_palette");
        send(player,s.pos,false);
    }));}
    public static void send(ServerPlayer p,BlockPos pos,boolean open){
        if(!(p.level().getBlockEntity(pos) instanceof DrawingBoardEntity board))return;
        var data=new CompoundTag();data.putString("kind","drawing_board");data.putByteArray("pixels",board.pixels());
        PacketDistributor.sendToPlayer(p,new RadioNetwork.Snapshot(pos,open,data));
    }
}
