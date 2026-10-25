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
    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("2");
        registrar.playToServer(Request.TYPE, Request.CODEC, (request, context) -> context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            var level = player.serverLevel();
            if (player.distanceToSqr(request.pos.getX()+0.5, request.pos.getY()+0.5, request.pos.getZ()+0.5) > 64
                || !level.hasChunkAt(request.pos) || !(level.getBlockEntity(request.pos) instanceof RadioBlockEntity radio)) return;
            // Per-player rate limiting is persisted in the player object, not an unbounded static map.
            long now = level.getGameTime();
            var persistent = player.getPersistentData();
            String rateKey = request.save ? "prscraft_save" : "prscraft_refresh";
            if (persistent.contains(rateKey) && now - persistent.getLong(rateKey) < (request.save ? 2 : 10)) {
                if (request.save) send(player, request.pos, false, "busy");
                return;
            }
            persistent.putLong(rateKey, now);
            String error = "";
            if (request.save) {
                if (!player.mayBuild() || !level.mayInteract(player, request.pos)) error = "permission";
                else try { radio.configure(RadioSettings.read(request.data)); }
                catch (IllegalArgumentException e) { error = e.getMessage(); }
            }
            send(player, request.pos, false, error);
        }));
        registrar.playToClient(Snapshot.TYPE, Snapshot.CODEC, (snapshot, context) ->
            context.enqueueWork(() -> dev.jarofcucumbers.prscraft.client.RadioClient.receive(snapshot)));
    }
    public static void send(ServerPlayer player, BlockPos pos, boolean open, String error) {
        var level = player.serverLevel();
        if (!(level.getBlockEntity(pos) instanceof RadioBlockEntity radio)) return;
        RadioSettings settings = radio.settings();
        CompoundTag data = new CompoundTag();
        data.put("settings", settings.save()); data.putString("error", error); data.putLong("time", level.getGameTime());
        CableNetwork.Link link = CableNetwork.receive(level, pos, settings);
        data.putBoolean("connected", link != null);
        if (link != null) {
            data.put("source", link.source().save()); data.putDouble("loss", link.lossDb()); data.putInt("metres", link.metres());
            data.putString("medium", link.medium().name()); data.putDouble("interference", CableNetwork.interferenceRms(level, pos, link, settings));
            long seed = link.sourcePos().asLong() ^ level.getGameTime();
            SignalProgram program = SignalProgram.parse(link.source().expression());
            double span = Transmission.windowSeconds(program, link.source().sampleHz());
            double gain = Transmission.gain(link.source().powerDbm(), link.lossDb());
            double sigma = Transmission.rms(settings, link.source(), data.getDouble("interference"));
            int[] samples = new int[512];
            for (int i = 0; i < samples.length; i++) samples[i] = Float.floatToIntBits((float)
                Transmission.received(program, link.source(), span*i/(samples.length-1), gain, sigma, seed));
            data.putIntArray("received", samples); data.putDouble("span", span); data.putDouble("sigma", sigma);
        }
        PacketDistributor.sendToPlayer(player, new Snapshot(pos, open, data));
    }
}
