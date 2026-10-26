package dev.jarofcucumbers.prscraft.client;

import dev.jarofcucumbers.prscraft.RadioNetwork;
import net.minecraft.client.Minecraft;

/** Only resolved by the clientbound packet handler on the physical client. */
public final class RadioClient {
    private RadioClient() {}
    public static void openHandbook(){Minecraft.getInstance().setScreen(new HandbookScreen(null));}
    public static void receive(RadioNetwork.Snapshot packet) {
        Minecraft minecraft = Minecraft.getInstance();
        if(!packet.open()&&packet.data().getString("kind").equals("codebook_editor")){if(minecraft.screen instanceof CodebookScreen editor&&editor.pos().equals(packet.pos()))editor.receive(packet.data());return;}
        if (packet.open()) {
            String kind=packet.data().getString("kind");
            String displayKind=packet.data().getBoolean("recorded")?packet.data().getString("captureKind"):kind;
            boolean dsp=java.util.Set.of("adder","integrator","correlator","time_gate","delay_line").contains(displayKind);
            minecraft.setScreen(kind.equals("codebook_editor")?new CodebookScreen(packet.pos(),packet.data()):java.util.Set.of("message_source","message_codec","channel_coder","message_receiver").contains(displayKind)||displayKind.equals("radio")&&packet.data().getBoolean("dataStream")?new MessageScreen(packet.pos(),packet.data()):displayKind.equals("gray_converter")?new GrayScreen(packet.pos(),packet.data()):kind.equals("drawing_board")?new BoardScreen(packet.pos(),packet.data()):dsp?new DspScreen(packet.pos(),packet.data()):kind.equals("cable")?new CableScreen(packet.pos(),packet.data()):kind.equals("analyzer")?new AnalyzerScreen(packet.pos(),packet.data()):kind.equals("radio")||kind.equals("bit_generator")?new RadioScreen(packet.pos(),packet.data()):new ProcessorScreen(packet.pos(),packet.data()));
        }
        else if(minecraft.screen instanceof PlotScreen plot&&plot.pos().equals(packet.pos()))plot.receive(packet.data());
        else if(minecraft.screen instanceof MessageScreen message&&message.pos().equals(packet.pos()))message.receive(packet.data());
        else if(minecraft.screen instanceof CircuitMapScreen map&&map.pos().equals(packet.pos())&&packet.data().getString("kind").equals("analyzer"))map.receive(packet.data());
        else if(minecraft.screen instanceof GrayScreen gray&&gray.pos().equals(packet.pos()))gray.receive(packet.data());
        else if(minecraft.screen instanceof BoardScreen board&&board.pos().equals(packet.pos()))board.receive(packet.data());
        else if(minecraft.screen instanceof CableScreen cable&&cable.pos().equals(packet.pos()))cable.receive(packet.data());
        else if(minecraft.screen instanceof DspScreen dsp&&dsp.pos().equals(packet.pos()))dsp.receive(packet.data());
        else if(minecraft.screen instanceof LabScreen lab&&lab.pos().equals(packet.pos()))lab.receive(packet.data());
        else if(minecraft.screen instanceof AnalyzerScreen analyzer&&analyzer.pos().equals(packet.pos()))analyzer.receive(packet.data());
        else if(minecraft.screen instanceof ProcessorScreen processor && processor.pos().equals(packet.pos()))processor.receive(packet.data());
        else if (minecraft.screen instanceof RadioScreen screen && screen.pos().equals(packet.pos())) screen.receive(packet.data());
    }
    public static void export(net.minecraft.nbt.CompoundTag data){
        if(data.getBoolean("dataStream")){exportMessage(data);return;}
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

    public static void exportMessage(net.minecraft.nbt.CompoundTag data){
        var mc=Minecraft.getInstance();try{var folder=mc.gameDirectory.toPath().resolve("prscraft/messages");java.nio.file.Files.createDirectories(folder);String base="message-"+java.util.UUID.randomUUID();var file=folder.resolve(base+".bin");java.nio.file.Files.write(file,data.getByteArray("messageBytes"),java.nio.file.StandardOpenOption.CREATE_NEW);var metadata=new com.google.gson.JsonObject();metadata.addProperty("bitLength",data.getInt("messageResultBits"));metadata.addProperty("bitRate",data.getDouble("messageRate"));metadata.addProperty("paddingBits",(8-data.getInt("messageResultBits")%8)%8);metadata.addProperty("error",data.getString("messageError"));metadata.addProperty("utf8",data.getBoolean("messageUtf8"));java.nio.file.Files.writeString(folder.resolve(base+".json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(metadata),java.nio.charset.StandardCharsets.UTF_8,java.nio.file.StandardOpenOption.CREATE_NEW);if(data.getBoolean("messageUtf8"))java.nio.file.Files.writeString(folder.resolve(base+".txt"),data.getString("messageText"),java.nio.charset.StandardCharsets.UTF_8,java.nio.file.StandardOpenOption.CREATE_NEW);if(mc.player!=null)mc.player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.prscraft.exported",file.toString()),false);}catch(java.io.IOException e){if(mc.player!=null)mc.player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.prscraft.export_error"),false);}
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
