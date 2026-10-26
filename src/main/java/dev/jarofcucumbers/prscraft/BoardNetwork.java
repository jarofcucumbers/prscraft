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
            public Stroke decode(RegistryFriendlyByteBuf b){return new Stroke(b.readBlockPos(),b.readBoolean(),java.util.BitSet.valueOf(b.readByteArray(2048)).stream().toArray());}
            public void encode(RegistryFriendlyByteBuf b,Stroke s){b.writeBlockPos(s.pos);b.writeBoolean(s.offhand);var bits=new java.util.BitSet(BoardPixels.COUNT);for(int pixel:BoardPixels.checked(s.pixels))bits.set(pixel);b.writeByteArray(bits.toByteArray());}
        };
        public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public record Clear(BlockPos pos,boolean offhand) implements CustomPacketPayload{
        public static final Type<Clear> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(PRSCraft.ID,"board_clear"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Clear> CODEC=new StreamCodec<>(){public Clear decode(RegistryFriendlyByteBuf b){return new Clear(b.readBlockPos(),b.readBoolean());}public void encode(RegistryFriendlyByteBuf b,Clear p){b.writeBlockPos(p.pos);b.writeBoolean(p.offhand);}};
        public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public static void register(RegisterPayloadHandlersEvent e){var registrar=e.registrar("3");registrar.playToServer(Clear.TYPE,Clear.CODEC,(request,context)->context.enqueueWork(()->{
        if(!(context.player() instanceof ServerPlayer player))return;var level=player.serverLevel();var pos=request.pos();
        if(player.distanceToSqr(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5)>64||!level.hasChunkAt(pos)||!player.mayBuild()||!level.mayInteract(player,pos)||!(level.getBlockEntity(pos) instanceof DrawingBoardEntity board))return;
        var held=player.getItemInHand(request.offhand()?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND);if(!held.is(PRSCraft.ERASER.get()))return;
        boolean cleared=BoardTools.clearAll(board,held);player.inventoryMenu.broadcastChanges();send(player,pos,false,cleared?"":"board_clear_resource");
    }));registrar.playToServer(Stroke.TYPE,Stroke.CODEC,(s,c)->c.enqueueWork(()->{
        if(!(c.player() instanceof ServerPlayer player))return;var level=player.serverLevel();
        if(player.distanceToSqr(s.pos.getX()+.5,s.pos.getY()+.5,s.pos.getZ()+.5)>64||!level.hasChunkAt(s.pos)||!player.mayBuild()||!level.mayInteract(player,s.pos)||!(level.getBlockEntity(s.pos) instanceof DrawingBoardEntity board))return;
        var held=player.getItemInHand(s.offhand?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND);if(!BoardTools.isTool(held))return;
        int[] pixels;try{pixels=BoardPixels.checked(s.pixels);}catch(IllegalArgumentException ignored){return;}
        long tick=level.getGameTime();var budget=player.getPersistentData();
        if(budget.getLong("prscraft_draw_tick")!=tick){budget.putLong("prscraft_draw_tick",tick);budget.putInt("prscraft_draw_cost",0);}
        int spent=budget.getInt("prscraft_draw_cost");if(spent+pixels.length>BoardPixels.COUNT)return;budget.putInt("prscraft_draw_cost",spent+pixels.length);
        BoardTools.apply(board,held,pixels);player.inventoryMenu.broadcastChanges();
        if(ChallengeRules.mural(board.pixels()))Milestones.award(player,"board_artist");
        if(ChallengeRules.fullPalette(board.pixels()))Milestones.award(player,"full_palette");
        send(player,s.pos,false);
    }));}
    public static void send(ServerPlayer p,BlockPos pos,boolean open){send(p,pos,open,"");}
    public static void send(ServerPlayer p,BlockPos pos,boolean open,String error){
        if(!(p.level().getBlockEntity(pos) instanceof DrawingBoardEntity board))return;
        var data=new CompoundTag();data.putString("kind","drawing_board");data.putString("error",error);data.putByteArray("pixels",board.pixels());
        PacketDistributor.sendToPlayer(p,new RadioNetwork.Snapshot(pos,open,data));
    }
}
