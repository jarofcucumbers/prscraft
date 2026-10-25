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
        var link=RadioCapture.capture(h.getLevel(),h.absolutePos(RX));
        h.assertTrue(link.getBoolean("connected"),"TX must be reachable around a cable corner");
        h.assertTrue(link.getDouble("metres")==5,"Shortest route must be 5 metres");
        h.assertTrue(Math.abs(link.getDouble("loss")-3)<1e-9,"Two coax connectors and 5 metres must lose 3 dB");
        h.setBlock(CORNER,Blocks.AIR);
        h.assertTrue(!RadioCapture.capture(h.getLevel(),h.absolutePos(RX)).getBoolean("connected"),"Broken cable must disconnect RX");
        h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=20)
    public static void incompatibleCableAndCarrier(GameTestHelper h) {
        setup(h);
        ((RadioBlockEntity)h.getBlockEntity(RX)).configure(receiver(125));
        h.assertTrue(!RadioCapture.capture(h.getLevel(),h.absolutePos(RX)).getBoolean("connected"),"Different carrier must not connect");
        ((RadioBlockEntity)h.getBlockEntity(RX)).configure(receiver(100));
        h.setBlock(CORNER,block("fiber_cable"));
        h.assertTrue(!RadioCapture.capture(h.getLevel(),h.absolutePos(RX)).getBoolean("connected"),"Mixed media must not connect");
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
            var link=RadioCapture.capture(h.getLevel(),h.absolutePos(rx));
            h.assertTrue(link.getBoolean("connected")&&link.getString("pipeline").contains(kind),"Route must include "+kind);
            var monitor=RadioCapture.capture(h.getLevel(),h.absolutePos(device));
            h.assertTrue(monitor.getBoolean("connected")&&monitor.getIntArray("emitted").length==512,"Processor monitor must show input and processed output");
            h.setBlock(device,block(kind).defaultBlockState().setValue(TransmitterBlock.FACING,net.minecraft.core.Direction.WEST));
            h.assertTrue(!RadioCapture.capture(h.getLevel(),h.absolutePos(rx)).getBoolean("connected"),"Reversed processor must break route");
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
                for(var page:HandbookContent.PAGES)for(String suffix:java.util.List.of("title","body","symbols")){
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
        double[] sent=SignalChain.unpack(data.getIntArray("sourceOutput")),received=SignalChain.unpack(data.getIntArray("received"));
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

    private static RadioSettings cleanSource(String expression){return new RadioSettings(expression,100,16,true,true,false,0,100,20,true,.5,"analog","10110010",10);}
    private static RadioSettings cleanRx(){return new RadioSettings("",100,16,false,true,false,0,100,20,true,.5);}
    private static final BlockPos PA=new BlockPos(4,1,6),PB=new BlockPos(2,1,4),DSP=new BlockPos(4,1,4),PR=new BlockPos(4,1,2);
    private static void pairSetup(GameTestHelper h,String kind){
        h.setBlock(PA,PRSCraft.TRANSMITTER.get());h.setBlock(PB,PRSCraft.TRANSMITTER.get());h.setBlock(PR,PRSCraft.TRANSMITTER.get());
        h.setBlock(DSP,block(kind).defaultBlockState().setValue(TransmitterBlock.FACING,net.minecraft.core.Direction.NORTH));
        for(BlockPos port:java.util.List.of(new BlockPos(4,1,5),new BlockPos(3,1,4),new BlockPos(4,1,3)))h.setBlock(port,block("coaxial_port"));
        ((RadioBlockEntity)h.getBlockEntity(PA)).configure(cleanSource("sin(10)"));((RadioBlockEntity)h.getBlockEntity(PB)).configure(cleanSource("cos(10)"));((RadioBlockEntity)h.getBlockEntity(PR)).configure(cleanRx());
    }
    @GameTest(template="empty",timeoutTicks=20)
    public static void adderReallyCombinesTwoPhysicalInputs(GameTestHelper h){
        pairSetup(h,"adder");var capture=RadioCapture.capture(h.getLevel(),h.absolutePos(PR));h.assertTrue(capture.getBoolean("connected"),"Two-source sum must reach RX");
        var y=SignalChain.unpack(capture.getIntArray("received"));double span=capture.getDouble("span"),gain=Math.pow(10,-4.8/20);
        for(int i=0;i<512;i++){double t=span*i/512;double expected=(Math.sin(2*Math.PI*10*t)+Math.cos(2*Math.PI*10*t))*gain;h.assertTrue(Math.abs(y[i]-expected)<1e-6,"Adder must use both separately routed, attenuated sources");}
        h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=20)
    public static void analyzerCrossesActiveDevicesAndCountsEveryPort(GameTestHelper h){
        pairSetup(h,"adder");var report=AnalyzerService.scan(h.getLevel(),h.absolutePos(new BlockPos(4,1,5)),h.absolutePos(PR));
        h.assertTrue(report.getInt("deviceCount")==4&&report.getInt("ports")==3,"Analyzer must include all four devices and three ports across the active adder");
        h.assertTrue(report.getList("connections",10).size()==3,"Circuit map must include all three passive buses");
        h.assertTrue(report.getCompound("capture").getBoolean("connected"),"Far-side RX must be selectable from the first input port");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=20)
    public static void cableLengthDamageAndReloadAffectTheActualChannel(GameTestHelper h){
        setup(h);RadioBlockEntity rx=h.getBlockEntity(RX);rx.configure(cleanRx());RadioBlockEntity tx=h.getBlockEntity(TX);tx.configure(cleanSource("sin(10)"));
        CableBlockEntity cable=h.getBlockEntity(new BlockPos(3,1,1));cable.configure(new CableSettings(.25,0));
        var clean=RadioCapture.capture(h.getLevel(),h.absolutePos(RX));h.assertTrue(Math.abs(clean.getDouble("metres")-4.25)<1e-9,"Fractional cable length must replace one default metre");
        cable.configure(new CableSettings(.25,.5));var damaged=RadioCapture.capture(h.getLevel(),h.absolutePos(RX));
        h.assertTrue(damaged.getDouble("loss")>clean.getDouble("loss")&&damaged.getDouble("cableVariance")>0,"Damage must add both loss and noise");
        double[] original=SignalChain.unpack(damaged.getIntArray("sourceOutput")),received=SignalChain.unpack(damaged.getIntArray("received"));double gain=Math.pow(10,-damaged.getDouble("loss")/20),squares=0;
        for(int i=0;i<512;i++){double noise=received[i]-original[i]*gain;squares+=noise*noise;}
        h.assertTrue(squares/512>1e-6,"Damage noise must actually reach waveform samples");
        var restored=new CableBlockEntity(cable.getBlockPos(),cable.getBlockState());restored.loadWithComponents(cable.saveWithFullMetadata(h.getLevel().registryAccess()),h.getLevel().registryAccess());
        h.assertTrue(restored.settings().equals(cable.settings()),"Cable length and damage must survive reload");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=20)
    public static void correlationFindsTheExtraDelayBlock(GameTestHelper h){
        pairSetup(h,"correlator");var pattern=new RadioSettings("sin(10)",100,16,true,true,false,0,100,20,true,.5,"bits","10110010",10);
        ((RadioBlockEntity)h.getBlockEntity(PA)).configure(pattern);
        h.setBlock(PB,PRSCraft.DELAY.get().defaultBlockState().setValue(TransmitterBlock.FACING,net.minecraft.core.Direction.EAST));
        var source=new BlockPos(0,1,4);h.setBlock(source,PRSCraft.TRANSMITTER.get());((RadioBlockEntity)h.getBlockEntity(source)).configure(pattern);h.setBlock(new BlockPos(1,1,4),block("coaxial_port"));
        RadioBlockEntity delay=h.getBlockEntity(PB);var d=DspSettings.DEFAULT;delay.configureDsp(new DspSettings(d.endSeconds(),d.gateSeconds(),d.gateWidth(),d.gateHold(),.02,.1,true,false,1,1));
        var result=RadioCapture.capture(h.getLevel(),h.absolutePos(DSP));h.assertTrue(result.getBoolean("connected")&&result.getIntArray("correlation").length==512,"Correlator needs two real inputs");
        h.assertTrue(Math.abs(result.getDouble("peakLag")-.02)<.003,"Correlation must recover the known 20 ms delay");h.assertTrue(result.getDouble("peakValue")>.9,"Delayed pattern should correlate strongly");h.assertTrue(ChallengeRules.achieved(result).contains("delay_hunt"),"Known physical delay must fulfill its challenge");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=20)
    public static void activeFeedbackFailsWithoutHangingAndAdvancementsExist(GameTestHelper h){
        var a=new BlockPos(2,1,2);var b=new BlockPos(2,1,4);
        h.setBlock(a,PRSCraft.AMPLIFIER.get().defaultBlockState().setValue(TransmitterBlock.FACING,net.minecraft.core.Direction.EAST));h.setBlock(b,PRSCraft.AMPLIFIER.get().defaultBlockState().setValue(TransmitterBlock.FACING,net.minecraft.core.Direction.WEST));
        for(BlockPos port:java.util.List.of(new BlockPos(1,1,2),new BlockPos(1,1,4),new BlockPos(3,1,2),new BlockPos(3,1,4)))h.setBlock(port,block("coaxial_port"));
        h.setBlock(new BlockPos(1,1,3),block("coaxial_cable"));h.setBlock(new BlockPos(3,1,3),block("coaxial_cable"));
        var capture=RadioCapture.capture(h.getLevel(),h.absolutePos(a));h.assertTrue(!capture.getBoolean("connected")&&capture.getString("fault").equals("feedback"),"Unsupported feedback must report an explicit fault");
        for(String id:Milestones.IDS)h.assertTrue(h.getLevel().getServer().getAdvancements().get(ResourceLocation.fromNamespaceAndPath(PRSCraft.ID,id))!=null,"Missing advancement: "+id);h.succeed();
    }

    @GameTest(template="empty",timeoutTicks=20)
    public static void boardPixelCostsEmptyEraserAndSerialization(GameTestHelper h){
        var pos=new BlockPos(2,1,2);h.setBlock(pos,PRSCraft.BOARD.get());DrawingBoardEntity board=h.getBlockEntity(pos);
        var chalk=new net.minecraft.world.item.ItemStack(PRSCraft.CHALKS.get(0).get());chalk.setDamageValue(4092);
        h.assertTrue(BoardTools.apply(board,chalk,BoardPixels.brush(64,64,16))==4&&chalk.isEmpty(),"Last chalk must draw only its four remaining pixels and break");
        int painted=0;for(byte b:board.pixels())if(b!=0)painted++;h.assertTrue(painted==4,"No unpaid pixels may be drawn");
        var eraser=new net.minecraft.world.item.ItemStack(PRSCraft.ERASER.get());
        int[] empty=java.util.stream.IntStream.range(0,256).toArray();for(int i=0;i<64;i++)BoardTools.apply(board,eraser,empty);
        h.assertTrue(!eraser.isEmpty()&&eraser.getDamageValue()==16384,"Blank pixels must spend eraser durability without destroying it");
        h.assertTrue(BoardTools.apply(board,eraser,new int[]{64*128+64})==0,"Empty eraser cannot modify a board");
        var copy=new DrawingBoardEntity(board.getBlockPos(),board.getBlockState());copy.loadWithComponents(board.saveWithFullMetadata(h.getLevel().registryAccess()),h.getLevel().registryAccess());
        h.assertTrue(java.util.Arrays.equals(board.pixels(),copy.pixels()),"World reload must preserve the complete canvas");
        h.assertTrue(java.util.Arrays.equals(board.pixels(),board.getUpdateTag(h.getLevel().registryAccess()).getByteArray("pixels")),"Tracking clients must receive the same canvas");
        var drops=Block.getDrops(board.getBlockState(),h.getLevel(),board.getBlockPos(),board);
        h.assertTrue(drops.size()==1&&drops.getFirst().has(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA),"Collected board must retain its drawing in the dropped item");
        var dropped=drops.getFirst().get(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA).copyTag();h.assertTrue(java.util.Arrays.equals(board.pixels(),dropped.getByteArray("pixels")),"Dropped board must contain exact pixel data");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=20)
    public static void washingConsumesWaterAndDyePreservesWear(GameTestHelper h){
        var pos=new BlockPos(2,1,2);h.setBlock(pos,Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL,3));
        var player=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);var eraser=new net.minecraft.world.item.ItemStack(PRSCraft.ERASER.get());eraser.setDamageValue(16384);player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,eraser);
        var worldPos=h.absolutePos(pos);var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(worldPos),net.minecraft.core.Direction.UP,worldPos,false);
        PRSCraft.ERASER.get().useOn(new net.minecraft.world.item.context.UseOnContext(player,net.minecraft.world.InteractionHand.MAIN_HAND,hit));
        h.assertTrue(eraser.getDamageValue()==0&&h.getLevel().getBlockState(worldPos).getValue(LayeredCauldronBlock.LEVEL)==2,"Washing must restore 16384 uses and spend one water level");
        PRSCraft.ERASER.get().useOn(new net.minecraft.world.item.context.UseOnContext(player,net.minecraft.world.InteractionHand.MAIN_HAND,hit));h.assertTrue(h.getLevel().getBlockState(worldPos).getValue(LayeredCauldronBlock.LEVEL)==2,"Clean eraser must not waste water");
        var chalk=new net.minecraft.world.item.ItemStack(PRSCraft.CHALKS.get(0).get());chalk.setDamageValue(567);
        var input=net.minecraft.world.item.crafting.CraftingInput.of(2,1,java.util.List.of(chalk,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.RED_DYE)));
        var recipe=new ChalkDyeRecipe(net.minecraft.world.item.crafting.CraftingBookCategory.MISC);
        h.assertTrue(recipe.matches(input,h.getLevel()),"One chalk and one dye must match");var colored=recipe.assemble(input,h.getLevel().registryAccess());
        h.assertTrue(colored.getItem() instanceof ChalkItem c&&c.palette==15&&colored.getDamageValue()==567,"Dye changes palette but never repairs used chalk");
        h.assertTrue(!eraser.isRepairable()&&!chalk.isRepairable(),"Drawing tools must not bypass their pixel costs through vanilla repair");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=20)
    public static void unipolarReturnToZeroReachesAndDecodesAtRx(GameTestHelper h){
        setup(h);RadioBlockEntity tx=h.getBlockEntity(TX),rx=h.getBlockEntity(RX);
        var lab=new LabSettings(true,false,.5,-1,1,"0,1",42,0,0,0,false,"relative",.1,"urz");
        tx.configure(new RadioSettings("sin(10)",100,16,true,true,false,0,100,20,true,.5,"bits","10110010",10,lab));rx.configure(cleanRx());
        rx.configureProcessor(new ProcessorSettings(0,0,1000,"urz",100,10,.5,true));
        var capture=RadioCapture.capture(h.getLevel(),h.absolutePos(RX));h.assertTrue(capture.getBoolean("connected"),"URZ must travel through the real cable path");
        h.assertTrue(capture.getString("decoded").equals("10110010"),"RX must decode URZ using active pulse averages");
        h.assertTrue(ChallengeRules.achieved(capture).contains("unipolar_link"),"Correct real unipolar link must fulfill its challenge");
        rx.configureProcessor(new ProcessorSettings(0,0,1000,"urz",100,20,.5,true));h.assertTrue(!ChallengeRules.achieved(RadioCapture.capture(h.getLevel(),h.absolutePos(RX))).contains("unipolar_link"),"Wrong symbol timing must not win a decoding challenge");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=20)
    public static void realFilterAndSumIntegralChallenges(GameTestHelper h){
        pairSetup(h,"adder");h.setBlock(PR,PRSCraft.INTEGRATOR.get().defaultBlockState().setValue(TransmitterBlock.FACING,net.minecraft.core.Direction.NORTH));
        var summed=RadioCapture.capture(h.getLevel(),h.absolutePos(PR));h.assertTrue(ChallengeRules.achieved(summed).contains("superposition"),"Real two-source sum followed by integration must qualify");
        h.setBlock(DSP,PRSCraft.FILTER.get().defaultBlockState().setValue(TransmitterBlock.FACING,net.minecraft.core.Direction.NORTH));RadioBlockEntity filter=h.getBlockEntity(DSP);
        filter.configureProcessor(new ProcessorSettings(0,0,20,"bpsk",100,10,.5,false));((RadioBlockEntity)h.getBlockEntity(PA)).configure(cleanSource("sin(10)+3*cos(80)"));
        var result=RadioCapture.capture(h.getLevel(),h.absolutePos(DSP));h.assertTrue(ChallengeRules.achieved(result).contains("filter_lab"),"Real filter with low retained energy must qualify");h.setBlock(new BlockPos(4,1,5),Blocks.AIR);
        h.assertTrue(ChallengeRules.achieved(RadioCapture.capture(h.getLevel(),h.absolutePos(DSP))).isEmpty(),"Disconnected filter must never fulfill a challenge");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=20)
    public static void noisyBpskAndGatedIntegrationChallenges(GameTestHelper h){
        pairSetup(h,"modulator");RadioBlockEntity source=h.getBlockEntity(PA),rx=h.getBlockEntity(PR);
        source.configure(new RadioSettings("sin(10)",100,16,true,true,false,0,100,20,true,.5,"bits","10110010",10));
        rx.configure(cleanRx().withLab(LabSettings.defaults(.02)));rx.configureProcessor(new ProcessorSettings(0,0,1000,"bpsk",100,10,.5,true));
        var data=RadioCapture.capture(h.getLevel(),h.absolutePos(PR));h.assertTrue(data.getString("decoded").equals("10110010")&&ChallengeRules.achieved(data).contains("noise_recovery"),"Coherent BPSK must decode through actual AWGN and fulfill its challenge");
        h.setBlock(DSP,PRSCraft.TIME_GATE.get().defaultBlockState().setValue(TransmitterBlock.FACING,net.minecraft.core.Direction.NORTH));
        h.setBlock(PR,PRSCraft.INTEGRATOR.get().defaultBlockState().setValue(TransmitterBlock.FACING,net.minecraft.core.Direction.NORTH));
        h.assertTrue(ChallengeRules.achieved(RadioCapture.capture(h.getLevel(),h.absolutePos(PR))).contains("timing_lab"),"A real gate followed by nonzero integration must fulfill its challenge");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=20)
    public static void fullCanvasEraserAndLegacyToolsUpgrade(GameTestHelper h){
        var pos=new BlockPos(2,1,2);h.setBlock(pos,PRSCraft.BOARD.get());DrawingBoardEntity board=h.getBlockEntity(pos);
        board.paint(java.util.stream.IntStream.range(0,16384).toArray(),2);var eraser=new net.minecraft.world.item.ItemStack(PRSCraft.ERASER.get());
        for(int i=0;i<64;i++)h.assertTrue(BoardTools.apply(board,eraser,java.util.stream.IntStream.range(i*256,(i+1)*256).toArray())==256,"Fresh eraser must clear every full-board batch");
        h.assertTrue(!eraser.isEmpty()&&BoardTools.remaining(eraser)==0,"16384 uses must cover exactly one full canvas");for(byte p:board.pixels())h.assertTrue(p==0,"Every pixel must be cleared");
        var old=new net.minecraft.world.item.ItemStack(PRSCraft.ERASER.get());old.set(net.minecraft.core.component.DataComponents.MAX_DAMAGE,512);old.setDamageValue(123);BoardEraserItem.upgrade(old);
        h.assertTrue(old.getMaxDamage()==16384&&old.getDamageValue()==123,"Legacy eraser must preserve spent pixels while receiving new capacity");
        var chalk=new net.minecraft.world.item.ItemStack(PRSCraft.CHALKS.getFirst().get());chalk.set(net.minecraft.core.component.DataComponents.MAX_DAMAGE,1024);chalk.setDamageValue(567);ChalkItem.upgrade(chalk);
        h.assertTrue(chalk.getMaxDamage()==4096&&chalk.getDamageValue()==567,"Legacy chalk must preserve spent pixels and upgrade to 4096");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=20)
    public static void materialGainSubtractionAndAutocorrelationGoals(GameTestHelper h){
        setup(h);((RadioBlockEntity)h.getBlockEntity(RX)).configure(cleanRx());
        for(String medium:java.util.List.of("fiber","metallic")){
            h.setBlock(new BlockPos(2,1,1),block(medium+"_port"));h.setBlock(new BlockPos(3,1,1),block(medium+"_cable"));h.setBlock(CORNER,block(medium+"_cable"));h.setBlock(new BlockPos(3,1,3),block(medium+"_port"));
            var goals=ChallengeRules.achieved(RadioCapture.capture(h.getLevel(),h.absolutePos(RX)));h.assertTrue(goals.contains(medium.equals("fiber")?"fiber_link":"copper_link"),"Actual RX medium must fulfill matching goal");
        }
        CableBlockEntity cable=h.getBlockEntity(CORNER);cable.configure(new CableSettings(100,0));h.assertTrue(ChallengeRules.achieved(RadioCapture.capture(h.getLevel(),h.absolutePos(RX))).contains("long_haul"),"Effective cable metres must qualify long haul");
        pairSetup(h,"adder");RadioBlockEntity sum=h.getBlockEntity(DSP);var settings=sum.dsp().save();settings.putDouble("b",-1);sum.configureDsp(DspSettings.read(settings));h.assertTrue(ChallengeRules.achieved(RadioCapture.capture(h.getLevel(),h.absolutePos(DSP))).contains("subtractor"),"Working negative-B sum must qualify");
        h.setBlock(DSP,PRSCraft.AMPLIFIER.get().defaultBlockState().setValue(TransmitterBlock.FACING,net.minecraft.core.Direction.NORTH));RadioBlockEntity amp=h.getBlockEntity(DSP);amp.configureProcessor(new ProcessorSettings(12,0,1000,"bpsk",100,10,.5,false));h.assertTrue(ChallengeRules.achieved(RadioCapture.capture(h.getLevel(),h.absolutePos(DSP))).contains("signal_boost"),"Measured unclipped gain must qualify");
        h.setBlock(DSP,PRSCraft.CORRELATOR.get().defaultBlockState().setValue(TransmitterBlock.FACING,net.minecraft.core.Direction.NORTH));RadioBlockEntity cor=h.getBlockEntity(DSP);settings=cor.dsp().save();settings.putBoolean("auto",true);settings.putBoolean("normalized",true);cor.configureDsp(DspSettings.read(settings));h.assertTrue(ChallengeRules.achieved(RadioCapture.capture(h.getLevel(),h.absolutePos(DSP))).contains("self_similarity"),"Measured normalized autocorrelation must qualify");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=20)
    public static void transmitterLearningGoalsRequireWorkingReception(GameTestHelper h){
        setup(h);RadioBlockEntity tx=h.getBlockEntity(TX),rx=h.getBlockEntity(RX);rx.configure(cleanRx());
        for(String mode:java.util.List.of("analog","sampled","quantized")){
            tx.configure(new RadioSettings("sin(10)+cos(20)+sin(30)+cos(40)",100,256,true,true,false,0,100,20,false,.5,mode,"1010",10));
            var goals=ChallengeRules.achieved(RadioCapture.capture(h.getLevel(),h.absolutePos(RX)));h.assertTrue(goals.contains(mode+"_link")&&goals.contains("four_tones")&&goals.contains("quiet_receiver"),"Matching mode, multitone and quiet-RX goals must qualify");
            if(mode.equals("quantized"))h.assertTrue(goals.contains("fine_quantization")&&goals.contains("natural_sampling"),"256 levels and first-kind sampling must qualify");
        }
        var lab=new LabSettings(false,false,.5,-1,1,"0,1",42,.01,0,0,false,"energy",.2);
        tx.configure(new RadioSettings("sin(10)",100,16,true,true,false,0,100,20,true,.5,"bits","10110010",10,lab));
        var goals=ChallengeRules.achieved(RadioCapture.capture(h.getLevel(),h.absolutePos(RX)));h.assertTrue(goals.containsAll(java.util.Set.of("finite_burst","offset_scan","energy_source")),"Finite J burst and offset must qualify on a real received waveform");
        h.setBlock(CORNER,Blocks.AIR);h.assertTrue(ChallengeRules.achieved(RadioCapture.capture(h.getLevel(),h.absolutePos(RX))).isEmpty(),"No learning goals may be earned from a disconnected RX");h.succeed();
    }
}
