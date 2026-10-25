package dev.jarofcucumbers.prscraft.client;

import dev.jarofcucumbers.prscraft.RadioNetwork;
import net.minecraft.client.Minecraft;

/** Only resolved by the clientbound packet handler on the physical client. */
public final class RadioClient {
    private RadioClient() {}
    public static void receive(RadioNetwork.Snapshot packet) {
        Minecraft minecraft = Minecraft.getInstance();
        if (packet.open()) minecraft.setScreen(packet.data().getString("kind").equals("radio") ? new RadioScreen(packet.pos(), packet.data()) : new ProcessorScreen(packet.pos(),packet.data()));
        else if(minecraft.screen instanceof ProcessorScreen processor && processor.pos().equals(packet.pos()))processor.receive(packet.data());
        else if (minecraft.screen instanceof RadioScreen screen && screen.pos().equals(packet.pos())) screen.receive(packet.data());
    }
    public static void export(net.minecraft.nbt.CompoundTag data){
        var minecraft=Minecraft.getInstance();
        try{
            String[] keys={"original","sampled","sourceOutput","emitted","received"};
            int[][] values=new int[keys.length][];
            for(int k=0;k<keys.length;k++)values[k]=data.getIntArray(keys[k]);
            String csv=dev.jarofcucumbers.prscraft.CaptureCsv.encode(data.getDouble("span"),values);
            var folder=minecraft.gameDirectory.toPath().resolve("prscraft/recordings");java.nio.file.Files.createDirectories(folder);
            var file=folder.resolve("capture-"+java.time.Instant.now().toEpochMilli()+"-"+java.util.UUID.randomUUID()+".csv");
            java.nio.file.Files.writeString(file,csv,java.nio.charset.StandardCharsets.UTF_8,java.nio.file.StandardOpenOption.CREATE_NEW);
            if(minecraft.player!=null)minecraft.player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.prscraft.exported",file.toString()),false);
        }catch(java.io.IOException | IllegalArgumentException e){if(minecraft.player!=null)minecraft.player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.prscraft.export_error"),false);}
    }

}
