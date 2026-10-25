package dev.jarofcucumbers.prscraft;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class RadioNetwork {
    private RadioNetwork() {}
    public record Request(BlockPos pos, boolean save, CompoundTag data) implements CustomPacketPayload {
        public static final Type<Request> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(PRSCraft.ID,"radio_request"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Request> CODEC = new StreamCodec<>() {
            public Request decode(RegistryFriendlyByteBuf b) { return new Request(b.readBlockPos(), b.readBoolean(), b.readNbt()); }
            public void encode(RegistryFriendlyByteBuf b, Request p) { b.writeBlockPos(p.pos); b.writeBoolean(p.save); b.writeNbt(p.data); }
        };
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    public record Snapshot(BlockPos pos, boolean open, CompoundTag data) implements CustomPacketPayload {
        public static final Type<Snapshot> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(PRSCraft.ID,"radio_snapshot"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Snapshot> CODEC = new StreamCodec<>() {
            public Snapshot decode(RegistryFriendlyByteBuf b) { return new Snapshot(b.readBlockPos(), b.readBoolean(), b.readNbt()); }
            public void encode(RegistryFriendlyByteBuf b, Snapshot p) { b.writeBlockPos(p.pos); b.writeBoolean(p.open); b.writeNbt(p.data); }
        };
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    public record Export(CompoundTag data) implements CustomPacketPayload {
        public static final Type<Export> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(PRSCraft.ID,"export"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Export> CODEC=new StreamCodec<>() {
            public Export decode(RegistryFriendlyByteBuf b){return new Export(b.readNbt());}
            public void encode(RegistryFriendlyByteBuf b,Export p){b.writeNbt(p.data);}
        };
        public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("3");
        registrar.playToServer(Request.TYPE, Request.CODEC, (request, context) -> context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            var level = player.serverLevel();
            if (player.distanceToSqr(request.pos.getX()+0.5, request.pos.getY()+0.5, request.pos.getZ()+0.5) > 64
                || !level.hasChunkAt(request.pos) || !(level.getBlockEntity(request.pos) instanceof RadioBlockEntity radio)) return;
            // Per-player rate limiting is persisted in the player object, not an unbounded static map.
            long now = level.getGameTime();
            var persistent = player.getPersistentData();
            if(request.data==null)return;
            String action=request.data.getString("action");
            if(!java.util.Set.of("","media","view","reopen").contains(action))return;
            if(action.equals("view")){
                if(player.mayBuild()&&level.mayInteract(player,request.pos))radio.graph(request.data.getInt("graphLayers"),request.data.getInt("graphTerms"));
                return;
            }
            String rateKey = request.save ? "prscraft_save" : action.isEmpty()?"prscraft_refresh":"prscraft_"+action;
            if (persistent.contains(rateKey) && now - persistent.getLong(rateKey) < (request.save || !action.isEmpty() ? 2 : 10)) {
                if (request.save) send(player, request.pos, false, "busy");
                return;
            }
            persistent.putLong(rateKey, now);
            String error = "";
            if(!action.isEmpty()) {
                if(!player.mayBuild() || !level.mayInteract(player,request.pos)){send(player,request.pos,false,"permission");return;}
                switch(action){
                    case "reopen" -> {send(player,request.pos,true,"");return;}
                    case "media" -> { player.openMenu(new net.minecraft.world.SimpleMenuProvider((id,inventory,p)->new PresetMenu(id,inventory,radio),
                        net.minecraft.network.chat.Component.translatable("screen.prscraft.media")),buf->buf.writeBlockPos(request.pos)); return; }
                    case "view" -> radio.graph(request.data.getInt("graphLayers"),request.data.getInt("graphTerms"));
                    default -> {return;}
                }
            }
            if (request.save) {
                if (!player.mayBuild() || !level.mayInteract(player, request.pos)) error = "permission";
                else try {
                    RadioSettings next=RadioSettings.read(request.data);
                    ProcessorSettings processor=request.data.contains("processor")?ProcessorSettings.read(request.data.getCompound("processor")):radio.processor();
                    radio.configure(next);radio.configureProcessor(processor);
                    if(request.data.contains("graphLayers"))radio.graph(request.data.getInt("graphLayers"),request.data.getInt("graphTerms"));
                }
                catch (IllegalArgumentException e) { error = e.getMessage(); }
            }
            send(player, request.pos, false, error);
        }));
        registrar.playToClient(Export.TYPE,Export.CODEC,(packet,context)->context.enqueueWork(()->dev.jarofcucumbers.prscraft.client.RadioClient.export(packet.data)));
        registrar.playToClient(Snapshot.TYPE, Snapshot.CODEC, (snapshot, context) ->
            context.enqueueWork(() -> dev.jarofcucumbers.prscraft.client.RadioClient.receive(snapshot)));
    }
    public static void send(ServerPlayer player, BlockPos pos, boolean open, String error) {
        var level = player.serverLevel();
        if (!(level.getBlockEntity(pos) instanceof RadioBlockEntity radio)) return;
        CompoundTag data=RadioCapture.capture(level,pos);data.putString("error",error);
        PacketDistributor.sendToPlayer(player, new Snapshot(pos, open, data));
    }
}
