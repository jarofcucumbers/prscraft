package dev.jarofcucumbers.prscraft;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.*;
import net.neoforged.neoforge.gametest.*;

/** Development-only integration fixtures; excluded from the distributable jar. */
@GameTestHolder(PRSCraft.ID)
@PrefixGameTestTemplate(false)
public final class RadioGameTests {
    private static final BlockPos TX=new BlockPos(1,1,1), RX=new BlockPos(3,1,4), CORNER=new BlockPos(3,1,2);
    private static Block block(String name) { return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(PRSCraft.ID,name)); }
    private static RadioSettings receiver(double carrier) {
        var s=RadioSettings.DEFAULT;
        return new RadioSettings(s.expression(),s.sampleHz(),s.levels(),false,true,true,0,carrier,s.powerDbm(),true,0.5);
    }
    private static void setup(GameTestHelper h) {
        h.setBlock(TX,PRSCraft.TRANSMITTER.get()); h.setBlock(RX,PRSCraft.TRANSMITTER.get());
        h.setBlock(new BlockPos(2,1,1),block("coaxial_port"));h.setBlock(new BlockPos(3,1,1),block("coaxial_cable"));
        h.setBlock(CORNER,block("coaxial_cable"));h.setBlock(new BlockPos(3,1,3),block("coaxial_port"));
        ((RadioBlockEntity)h.getBlockEntity(RX)).configure(receiver(100));
    }
    @GameTest(template="empty",timeoutTicks=20)
    public static void cableTurnAndBrokenLink(GameTestHelper h) {
        setup(h);
        var link=CableNetwork.receive(h.getLevel(),h.absolutePos(RX),receiver(100));
        h.assertTrue(link!=null,"TX must be reachable around a cable corner");
        h.assertTrue(link.metres()==5,"Shortest route must be 5 metres");
        h.assertTrue(Math.abs(link.lossDb()-3)<1e-9,"Two coax connectors and 5 metres must lose 3 dB");
        h.setBlock(CORNER,Blocks.AIR);
        h.assertTrue(CableNetwork.receive(h.getLevel(),h.absolutePos(RX),receiver(100))==null,"Broken cable must disconnect RX");
        h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=20)
    public static void incompatibleCableAndCarrier(GameTestHelper h) {
        setup(h);
        h.assertTrue(CableNetwork.receive(h.getLevel(),h.absolutePos(RX),receiver(125))==null,"Different carrier must not connect");
        h.setBlock(CORNER,block("fiber_cable"));
        h.assertTrue(CableNetwork.receive(h.getLevel(),h.absolutePos(RX),receiver(100))==null,"Mixed media must not connect");
        h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=20)
    public static void settingsSurviveBlockEntitySerialization(GameTestHelper h) {
        setup(h);
        RadioBlockEntity radio=h.getBlockEntity(RX);
        var original=receiver(125);radio.configure(original);
        var tag=radio.saveWithFullMetadata(h.getLevel().registryAccess());
        RadioBlockEntity restored=new RadioBlockEntity(h.absolutePos(RX),radio.getBlockState());
        restored.loadWithComponents(tag,h.getLevel().registryAccess());
        h.assertTrue(restored.settings().equals(original),"All waveform, sampling, noise and RX settings must persist");
        h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=20)
    public static void analyzerIsReadOnlyAndNotSwallowedByPort(GameTestHelper h) {
        setup(h);
        var player=h.makeMockPlayer(net.minecraft.world.level.GameType.CREATIVE);
        var stack=new net.minecraft.world.item.ItemStack(PRSCraft.ANALYZER.get());
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,stack);
        var port=h.absolutePos(new BlockPos(2,1,1));
        var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(port),net.minecraft.core.Direction.UP,port,false);
        var state=h.getLevel().getBlockState(port);
        var tx=(RadioBlockEntity)h.getBlockEntity(TX);
        var before=tx.saveWithFullMetadata(h.getLevel().registryAccess());
        var result=state.useItemOn(stack,h.getLevel(),player,net.minecraft.world.InteractionHand.MAIN_HAND,hit);
        h.assertTrue(result==net.minecraft.world.ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION,"Port must allow analyzer item handling");
        PRSCraft.ANALYZER.get().useOn(new net.minecraft.world.item.context.UseOnContext(player,net.minecraft.world.InteractionHand.MAIN_HAND,hit));
        h.assertTrue(h.getLevel().getBlockState(port).equals(state),"Analyzer must not modify port state");
        h.assertTrue(before.equals(tx.saveWithFullMetadata(h.getLevel().registryAccess())),"Analyzer must not modify radio settings");
        h.succeed();
    }

    @GameTest(template="empty",timeoutTicks=20)
    public static void directionalProcessorsAreOnTheActualRoute(GameTestHelper h){
        var tx=new BlockPos(1,1,1);var device=new BlockPos(3,1,1);var rx=new BlockPos(7,1,1);
        h.setBlock(tx,PRSCraft.TRANSMITTER.get());h.setBlock(rx,PRSCraft.TRANSMITTER.get());
        h.setBlock(new BlockPos(2,1,1),block("coaxial_port"));h.setBlock(new BlockPos(4,1,1),block("coaxial_port"));
        h.setBlock(new BlockPos(5,1,1),block("coaxial_cable"));h.setBlock(new BlockPos(6,1,1),block("coaxial_port"));
        ((RadioBlockEntity)h.getBlockEntity(rx)).configure(receiver(100));
        for(String kind:java.util.List.of("amplifier","frequency_filter","modulator")){
            h.setBlock(device,block(kind).defaultBlockState().setValue(TransmitterBlock.FACING,net.minecraft.core.Direction.EAST));
            var link=CableNetwork.receive(h.getLevel(),h.absolutePos(rx),receiver(100));
            h.assertTrue(link!=null&&link.processors().size()==1&&link.processors().getFirst().kind().equals(kind),"Route must include "+kind);
            var monitor=RadioCapture.capture(h.getLevel(),h.absolutePos(device));
            h.assertTrue(monitor.getBoolean("connected")&&monitor.getIntArray("emitted").length==512,"Processor monitor must show input and processed output");
            h.setBlock(device,block(kind).defaultBlockState().setValue(TransmitterBlock.FACING,net.minecraft.core.Direction.WEST));
            h.assertTrue(CableNetwork.receive(h.getLevel(),h.absolutePos(rx),receiver(100))==null,"Reversed processor must break route");
        }
        h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=20)
    public static void paperPresetRestoresSettingsAndGraphAndCapturePersists(GameTestHelper h){
        setup(h);RadioBlockEntity radio=h.getBlockEntity(RX);
        radio.graph(5,17);var original=radio.settings();
        var player=h.makeMockPlayer(net.minecraft.world.level.GameType.CREATIVE);
        var center=net.minecraft.world.phys.Vec3.atCenterOf(h.absolutePos(RX));player.setPos(center.x,center.y,center.z);
        var menu=new PresetMenu(1,player.getInventory(),radio);
        radio.media.setStackInSlot(0,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.PAPER));
        h.assertTrue(menu.clickMenuButton(player,0),"Paper must become a saved template");
        var template=radio.media.extractItem(0,1,false);radio.configure(receiver(125));radio.graph(0,0);
        radio.media.setStackInSlot(0,template);
        h.assertTrue(radio.settings().equals(original)&&radio.graphLayers()==5&&radio.graphTerms()==17,"Inserting template must restore configuration and visual choices");
        radio.media.setStackInSlot(1,new net.minecraft.world.item.ItemStack(PRSCraft.DATA_CARD.get()));
        h.assertTrue(menu.clickMenuButton(player,2),"Connected RX must record a data card");
        var tag=radio.saveWithFullMetadata(h.getLevel().registryAccess());
        RadioBlockEntity restored=new RadioBlockEntity(h.absolutePos(RX),radio.getBlockState());restored.loadWithComponents(tag,h.getLevel().registryAccess());
        h.assertTrue(restored.graphLayers()==5&&restored.graphTerms()==17,"Graph choices must survive reload");
        var data=restored.media.getStackInSlot(1).get(net.minecraft.core.component.DataComponents.CUSTOM_DATA).copyTag();
        h.assertTrue(data.getIntArray("received").length==512,"Recorded samples must survive block reload");
        h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=20)
    public static void bpskReceptionDecodesTheTransmittedBits(GameTestHelper h){
        var tx=new BlockPos(1,1,1);var device=new BlockPos(3,1,1);var rx=new BlockPos(7,1,1);
        h.setBlock(tx,PRSCraft.TRANSMITTER.get());h.setBlock(rx,PRSCraft.TRANSMITTER.get());
        h.setBlock(new BlockPos(2,1,1),block("coaxial_port"));h.setBlock(new BlockPos(4,1,1),block("coaxial_port"));
        h.setBlock(new BlockPos(5,1,1),block("coaxial_cable"));h.setBlock(new BlockPos(6,1,1),block("coaxial_port"));
        h.setBlock(device,PRSCraft.MODULATOR.get().defaultBlockState().setValue(TransmitterBlock.FACING,net.minecraft.core.Direction.EAST));
        ((RadioBlockEntity)h.getBlockEntity(tx)).configure(new RadioSettings("sin(10)",100,16,true,true,false,0,100,20,true,.5,"bits","10110010",10));
        RadioBlockEntity receiver=h.getBlockEntity(rx);receiver.configure(new RadioSettings("",100,16,false,true,false,0,100,20,true,.5));
        receiver.configureProcessor(new ProcessorSettings(0,0,1000,"bpsk",100,10,.5,true));
        var capture=RadioCapture.capture(h.getLevel(),h.absolutePos(rx));
        h.assertTrue(capture.getBoolean("connected"),"Modulated source must reach RX");
        h.assertTrue(capture.getString("decoded").equals("10110010"),"Coherent decoder must recover the transmitted eight bits");
        h.succeed();
    }

    @GameTest(template="empty",timeoutTicks=20)
    public static void handbookCoversEveryItemAndMatchesVersion(GameTestHelper h){
        for(var item:PRSCraft.ITEMS.getEntries())h.assertTrue(HandbookContent.COVERED_ITEMS.contains(item.getId().getPath()),"Missing handbook coverage: "+item.getId());
        String version=net.neoforged.fml.ModList.get().getModContainerById(PRSCraft.ID).orElseThrow().getModInfo().getVersion().toString();
        h.assertTrue(version.equals(HandbookContent.VERSION),"Handbook and mod versions must agree");
        for(String language:java.util.List.of("en_us","uk_ua")){
            try(var stream=RadioGameTests.class.getResourceAsStream("/assets/prscraft/lang/"+language+".json")){
                var translations=com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(stream,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
                for(var page:HandbookContent.PAGES)for(String suffix:java.util.List.of("title","body")){
                    String key="book.prscraft."+page.id()+"."+suffix;h.assertTrue(translations.has(key)&&!translations.get(key).getAsString().isBlank(),"Missing handbook translation: "+key);
                }
            }catch(java.io.IOException e){throw new RuntimeException(e);}
        }
        h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=20)
    public static void dedicatedGeneratorAndAwgnReachReceiver(GameTestHelper h){
        setup(h);h.setBlock(TX,PRSCraft.BIT_GENERATOR.get());RadioBlockEntity generator=h.getBlockEntity(TX);
        h.assertTrue(generator.settings().signalMode().equals("bits")&&generator.settings().lab().randomBits(),"Generator defaults to Bernoulli bits");
        var sourceLab=new LabSettings(false,true,.8,-2,1,"0,1",42,0,0,0,false,"relative",.1);
        generator.configure(generator.settings().withLab(sourceLab));RadioBlockEntity rx=h.getBlockEntity(RX);
        var noise=new LabSettings(true,false,.5,-1,1,"0,1",42,0,.5,.25,true,"relative",.1);
        rx.configure(rx.settings().withLab(noise));var data=RadioCapture.capture(h.getLevel(),h.absolutePos(RX));
        h.assertTrue(data.getBoolean("connected"),"Dedicated generator must be a routable TX source");
        double[] sent=SignalChain.unpack(data.getIntArray("emitted")),received=SignalChain.unpack(data.getIntArray("received"));
        double gain=Transmission.gain(generator.settings().powerDbm(),data.getDouble("loss")),mean=0,squares=0;
        for(int i=0;i<received.length;i++){double n=received[i]-sent[i]*gain;mean+=n;squares+=n*n;}
        mean/=received.length;double variance=squares/received.length-mean*mean;
        h.assertTrue(Math.abs(mean-.5)<.12&&Math.abs(variance-.25)<.12,"AWGN samples must have the configured nonzero mean and variance");
        var spectrum=Spectrum.analyze(received,data.getDouble("span"));
        h.assertTrue(Math.abs(java.util.Arrays.stream(spectrum.psd()).sum()*spectrum.binHz()-spectrum.powerW())<1e-8,"Received spectrum must obey Parseval");
        h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=20)
    public static void analyzerUiDataIsReadOnlyAndCannotInspectUnconnectedDevice(GameTestHelper h){
        setup(h);var remote=new BlockPos(7,1,7);h.setBlock(remote,PRSCraft.TRANSMITTER.get());
        RadioBlockEntity tx=h.getBlockEntity(TX),rx=h.getBlockEntity(RX);
        var beforeTx=tx.saveWithFullMetadata(h.getLevel().registryAccess());var beforeRx=rx.saveWithFullMetadata(h.getLevel().registryAccess());
        var report=AnalyzerService.scan(h.getLevel(),h.absolutePos(new BlockPos(2,1,1)),h.absolutePos(remote));
        var selected=new BlockPos(report.getInt("selectedX"),report.getInt("selectedY"),report.getInt("selectedZ"));
        h.assertTrue(!selected.equals(h.absolutePos(remote)),"Analyzer must reject nomination of an unrelated device");
        h.assertTrue(report.getList("devices",10).size()==2,"Local segment must list its two attached devices");
        h.assertTrue(beforeTx.equals(tx.saveWithFullMetadata(h.getLevel().registryAccess()))&&beforeRx.equals(rx.saveWithFullMetadata(h.getLevel().registryAccess())),"Analyzer must not change settings, media or graph preferences");
        h.succeed();
    }

}
