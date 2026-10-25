package dev.jarofcucumbers.prscraft.client;

import dev.jarofcucumbers.prscraft.RadioNetwork;
import net.minecraft.client.Minecraft;

/** Only resolved by the clientbound packet handler on the physical client. */
public final class RadioClient {
    private RadioClient() {}
    public static void openHandbook(){Minecraft.getInstance().setScreen(new HandbookScreen(null));}
    public static void receive(RadioNetwork.Snapshot packet) {
        Minecraft minecraft = Minecraft.getInstance();
        if (packet.open()) {
            String kind=packet.data().getString("kind");
            String displayKind=packet.data().getBoolean("recorded")?packet.data().getString("captureKind"):kind;
            boolean dsp=java.util.Set.of("adder","integrator","correlator","time_gate","delay_line").contains(displayKind);
            minecraft.setScreen(dsp?new DspScreen(packet.pos(),packet.data()):kind.equals("cable")?new CableScreen(packet.pos(),packet.data()):kind.equals("analyzer")?new AnalyzerScreen(packet.pos(),packet.data()):kind.equals("radio")||kind.equals("bit_generator")?new RadioScreen(packet.pos(),packet.data()):new ProcessorScreen(packet.pos(),packet.data()));
        }
        else if(minecraft.screen instanceof CableScreen cable&&cable.pos().equals(packet.pos()))cable.receive(packet.data());
        else if(minecraft.screen instanceof DspScreen dsp&&dsp.pos().equals(packet.pos()))dsp.receive(packet.data());
        else if(minecraft.screen instanceof LabScreen lab&&lab.pos().equals(packet.pos()))lab.receive(packet.data());
        else if(minecraft.screen instanceof AnalyzerScreen analyzer&&analyzer.pos().equals(packet.pos()))analyzer.receive(packet.data());
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

    public static void exportSpectrum(net.minecraft.nbt.CompoundTag data,String stage){
        var minecraft=Minecraft.getInstance();
        try{
            if(!stage.equals("sourceOutput")&&data.contains("units")&&!data.getString("units").equals("u"))throw new IllegalArgumentException("units");
            int[] samples=data.getIntArray(stage);if(samples.length!=512)throw new IllegalArgumentException("capture");
            var spectrum=dev.jarofcucumbers.prscraft.Spectrum.analyze(dev.jarofcucumbers.prscraft.SignalChain.unpack(samples),data.getDouble("span"));
            var folder=minecraft.gameDirectory.toPath().resolve("prscraft/recordings");java.nio.file.Files.createDirectories(folder);
            var file=folder.resolve("spectrum-"+java.util.UUID.randomUUID()+".csv");
            java.nio.file.Files.writeString(file,dev.jarofcucumbers.prscraft.Spectrum.csv(spectrum),java.nio.charset.StandardCharsets.UTF_8,java.nio.file.StandardOpenOption.CREATE_NEW);
            if(minecraft.player!=null)minecraft.player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.prscraft.exported",file.toString()),false);
        }catch(java.io.IOException|IllegalArgumentException e){if(minecraft.player!=null)minecraft.player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.prscraft.export_error"),false);}
    }

}
