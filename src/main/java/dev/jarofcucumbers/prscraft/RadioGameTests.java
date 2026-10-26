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
        h.assertTrue(h.getLevel().getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath(PRSCraft.ID,"chalk_from_calcite")).isPresent(),"Calcite chalk recipe must load successfully");
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
    @GameTest(template="empty",timeoutTicks=20)
    public static void grayConverterRoundTripsThroughRealPortsAndPersists(GameTestHelper h){
        pairSetup(h,"gray_converter");RadioBlockEntity source=h.getBlockEntity(PA),converter=h.getBlockEntity(DSP),receiver=h.getBlockEntity(PR);
        source.configure(new RadioSettings("sin(10)",100,16,true,true,false,0,100,20,true,1,"bits","10110010",10));
        var first=RadioCapture.capture(h.getLevel(),h.absolutePos(DSP));h.assertTrue(ChallengeRules.achieved(first).contains("gray_binary"),"Working Gray serial conversion earns its exercise");h.assertTrue(first.getBoolean("connected")&&first.getString("grayOutput").equals("11100011"),"Physical converter must encode received bits in four-bit words");
        h.setBlock(PR,PRSCraft.GRAY.get().defaultBlockState().setValue(TransmitterBlock.FACING,net.minecraft.core.Direction.NORTH));RadioBlockEntity reverse=h.getBlockEntity(PR);
        reverse.configureGray(new GraySettings("gray_to_binary",4,10,0,-1,1,16,-1,1,100));var roundTrip=RadioCapture.capture(h.getLevel(),h.absolutePos(PR));h.assertTrue(roundTrip.getString("grayOutput").equals("10110010"),"Two converters must round-trip through lossy physical segments");
        var restored=new RadioBlockEntity(reverse.getBlockPos(),reverse.getBlockState());restored.loadWithComponents(reverse.saveWithFullMetadata(h.getLevel().registryAccess()),h.getLevel().registryAccess());h.assertTrue(restored.gray().equals(reverse.gray()),"Gray settings must survive world reload");
        var template=new net.minecraft.nbt.CompoundTag();template.putString("kind","gray_converter");template.put("settings",reverse.settings().save());template.put("processor",reverse.processor().save());template.put("gray",reverse.gray().save());var paper=new net.minecraft.world.item.ItemStack(PRSCraft.TEMPLATE.get());paper.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(template));converter.media.setStackInSlot(0,paper);h.assertTrue(converter.gray().equals(reverse.gray()),"Template slot must apply Gray direction and timing");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=20)
    public static void quantizedSourceFeedsGrayLevelEncoder(GameTestHelper h){
        pairSetup(h,"gray_converter");RadioBlockEntity source=h.getBlockEntity(PA),converter=h.getBlockEntity(DSP);
        source.configure(new RadioSettings("sin(10)",100,4,true,true,false,0,100,20,true,1,"quantized","10",10));
        double gain=Math.pow(10,-2.4/20);converter.configureGray(new GraySettings("levels_to_gray",2,10,0,-1,1,4,-gain,gain,100));
        var capture=RadioCapture.capture(h.getLevel(),h.absolutePos(DSP));h.assertTrue(capture.getBoolean("connected")&&!capture.getString("grayInput").isEmpty()&&!capture.getString("grayOutput").isEmpty(),"Quantized source must produce serial Gray words through actual attenuation");
        h.assertTrue(ChallengeRules.achieved(capture).contains("gray_levels"),"Working Gray level encoding earns its exercise");String[] indices=capture.getString("grayInput").split(",");StringBuilder expected=new StringBuilder();for(String index:indices)expected.append(GrayCodec.word(GrayCodec.encode(Integer.parseInt(index)),2));h.assertTrue(expected.toString().equals(capture.getString("grayOutput")),"Every detected quantization level must use its Gray label");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=20)
    public static void allDevicePortRolesAndRotatedPortConnections(GameTestHelper h){
        var pos=new BlockPos(3,1,3);
        for(var item:PRSCraft.BLOCKS.getEntries())if(item.get() instanceof ProcessorBlock block)for(var front:net.minecraft.core.Direction.Plane.HORIZONTAL){
            h.setBlock(pos,block.defaultBlockState().setValue(TransmitterBlock.FACING,front));RadioBlockEntity device=h.getBlockEntity(pos);
            for(var side:net.minecraft.core.Direction.values()){
                String expected=device.isSource()?(device.settings().transmit()?"TX":"RX"):side==front?"OUT":side==front.getOpposite()?"A":(block.kind.equals("adder")||block.kind.equals("correlator")||block.kind.equals("orthogonality_meter"))&&side==front.getCounterClockWise()?"B":"unused";
                h.assertTrue(AnalyzerService.role(device,side).equals(expected),"Port role mismatch: "+block.kind+" facing "+front+" side "+side);
            }
        }
        setup(h);for(var facing:net.minecraft.core.Direction.values()){
            var port=block("coaxial_port").defaultBlockState().setValue(PortBlock.FACING,facing);h.setBlock(new BlockPos(2,1,1),port);
            h.assertTrue(RadioCapture.capture(h.getLevel(),h.absolutePos(RX)).getBoolean("connected"),"Port rotation must retain passive connectivity");
            h.assertTrue(!port.getShape(h.getLevel(),h.absolutePos(new BlockPos(2,1,1))).isEmpty(),"Each port orientation needs a matching selection shape");
        }h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=40)
    public static void everyProcessorRoutesInEveryHorizontalOrientation(GameTestHelper h){
        var center=new BlockPos(3,1,3);
        for(var holder:PRSCraft.BLOCKS.getEntries())if(holder.get() instanceof ProcessorBlock processor&&!java.util.Set.of("message_source","message_receiver","message_codec","channel_coder","packet_sender","packet_receiver","digital_modem").contains(processor.kind))for(var front:net.minecraft.core.Direction.Plane.HORIZONTAL){
            for(var d:net.minecraft.core.Direction.Plane.HORIZONTAL)for(int step=1;step<=2;step++)h.setBlock(center.relative(d,step),Blocks.AIR);
            h.setBlock(center,processor.defaultBlockState().setValue(TransmitterBlock.FACING,front));
            var source=center.relative(front.getOpposite(),2);h.setBlock(source,PRSCraft.TRANSMITTER.get());((RadioBlockEntity)h.getBlockEntity(source)).configure(cleanSource("sin(10)"));h.setBlock(center.relative(front.getOpposite()),block("coaxial_port"));
            var rx=center.relative(front,2);h.setBlock(rx,PRSCraft.TRANSMITTER.get());((RadioBlockEntity)h.getBlockEntity(rx)).configure(cleanRx());h.setBlock(center.relative(front),block("coaxial_port"));
            if(processor.kind.equals("adder")||processor.kind.equals("correlator")||processor.kind.equals("orthogonality_meter")){var side=front.getCounterClockWise();var b=center.relative(side,2);h.setBlock(b,PRSCraft.TRANSMITTER.get());((RadioBlockEntity)h.getBlockEntity(b)).configure(cleanSource("cos(10)"));h.setBlock(center.relative(side),block("coaxial_port"));}
            var result=RadioCapture.capture(h.getLevel(),h.absolutePos(rx));h.assertTrue(result.getBoolean("connected")&&result.getString("pipeline").contains(processor.kind),"Wrong real input/output route for "+processor.kind+" facing "+front+": "+result.getString("fault"));
        }h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=20)
    public static void insetDevicesNeverHideTheGroundFace(GameTestHelper h){
        var ground=new BlockPos(3,1,3);var above=ground.above();h.setBlock(ground,Blocks.SAND);
        for(var holder:PRSCraft.BLOCKS.getEntries())if(holder.get() instanceof TransmitterBlock){
            h.setBlock(above,holder.get());var state=h.getLevel().getBlockState(h.absolutePos(above));h.assertTrue(!state.canOcclude(),"Inset equipment must not claim full-cube occlusion");
            h.assertTrue(Block.shouldRenderFace(Blocks.SAND.defaultBlockState(),h.getLevel(),h.absolutePos(ground),net.minecraft.core.Direction.UP,h.absolutePos(above)),"Ground must render under "+holder.getId());
        }h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=20)
    public static void curriculumSolutionsAndSchematicsCoverEveryGoal(GameTestHelper h){
        var covered=HandbookCurriculum.components().stream().flatMap(c->c.items().stream()).collect(java.util.stream.Collectors.toSet());
        for(var item:PRSCraft.ITEMS.getEntries())h.assertTrue(covered.contains(item.getId().getPath()),"No component page: "+item.getId());
        for(String lang:java.util.List.of("uk_ua","en_us"))try(var stream=RadioGameTests.class.getResourceAsStream("/assets/prscraft/lang/"+lang+".json")){
            var text=com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(stream,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
            for(String id:Milestones.IDS){h.assertTrue(text.has("book.task."+id+".solution"),"No solution: "+id);var task=HandbookCurriculum.task(id);h.assertTrue(!task.nodes().isEmpty(),"No schematic: "+id);
                for(var node:task.nodes()){h.assertTrue(BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(node.item())),"Missing diagram item "+node.item());h.assertTrue(!node.parameters().isEmpty(),"Diagram lacks settings");}
                for(var edge:task.edges())h.assertTrue(edge.from()>=0&&edge.from()<task.nodes().size()&&edge.to()>=0&&edge.to()<task.nodes().size(),"Invalid schematic connection");
            }
        }catch(java.io.IOException e){throw new RuntimeException(e);}h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=20)
    public static void largeShapePayloadRoundTripsAndPaysForEveryPixel(GameTestHelper h){
        int[] points=BoardDrawing.shape(BoardDrawing.Shape.RECTANGLE,0,0,127,127,16);var packet=new BoardNetwork.Stroke(h.absolutePos(new BlockPos(2,1,2)),false,points);
        var buffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),h.getLevel().registryAccess());BoardNetwork.Stroke decoded;
        try{BoardNetwork.Stroke.CODEC.encode(buffer,packet);h.assertTrue(buffer.readableBytes()<2100,"Whole canvas must fit a bounded bitset payload");decoded=BoardNetwork.Stroke.CODEC.decode(buffer);}finally{buffer.release();}
        h.assertTrue(java.util.Arrays.equals(java.util.Arrays.stream(points).sorted().toArray(),decoded.pixels()),"Large shape may not lose pixels in transport");
        var pos=new BlockPos(2,1,2);h.setBlock(pos,PRSCraft.BOARD.get());DrawingBoardEntity board=h.getBlockEntity(pos);var eraser=new net.minecraft.world.item.ItemStack(PRSCraft.ERASER.get());int count=BoardTools.apply(board,eraser,decoded.pixels());
        h.assertTrue(count==points.length&&eraser.getDamageValue()==count,"Large eraser shapes pay for every touched blank pixel");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=20)
    public static void analyzerMapLabelsBothAdderBranchesAndActualSignalRoute(GameTestHelper h){
        pairSetup(h,"adder");var report=AnalyzerService.scan(h.getLevel(),h.absolutePos(new BlockPos(4,1,5)),h.absolutePos(PR));var nodes=report.getList("devices",10);var edges=report.getList("connections",10);var found=new java.util.HashSet<String>();
        for(int i=0;i<edges.size();i++){var edge=edges.getCompound(i);h.assertTrue(!edge.getString("direction").equals("physical"),"Each separate adder bus has a valid source-to-input direction");boolean reverse=edge.getString("direction").equals("ba");var a=nodes.getCompound(edge.getInt(reverse?"b":"a"));var b=nodes.getCompound(edge.getInt(reverse?"a":"b"));BlockPos from=new BlockPos(a.getInt("x"),a.getInt("y"),a.getInt("z")),to=new BlockPos(b.getInt("x"),b.getInt("y"),b.getInt("z"));found.add(from.toShortString()+">"+to.toShortString()+":"+edge.getString(reverse?"aRole":"bRole"));}
        h.assertTrue(found.contains(h.absolutePos(PA).toShortString()+">"+h.absolutePos(DSP).toShortString()+":A"),"First TX must enter A");h.assertTrue(found.contains(h.absolutePos(PB).toShortString()+">"+h.absolutePos(DSP).toShortString()+":B"),"Second TX must enter B");h.assertTrue(found.contains(h.absolutePos(DSP).toShortString()+">"+h.absolutePos(PR).toShortString()+":RX"),"Adder OUT must lead to RX");
        h.assertTrue(report.getCompound("capture").getList("signalEdges",10).size()==3,"Selected receiver must expose exactly its three computed input routes");h.succeed();
    }
    private static MessageSettings message(String text,String operation){return new MessageSettings("utf8",text,operation,1000,0,-1,1,0);}
    private static net.minecraft.world.item.ItemStack codeCard(String mapping){var card=new net.minecraft.world.item.ItemStack(PRSCraft.CODEBOOK_CARD.get());var tag=new net.minecraft.nbt.CompoundTag();tag.putString("codebook",mapping);card.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(tag));return card;}
    private static RadioBlockEntity messageDevice(GameTestHelper h,BlockPos at,String kind){h.setBlock(at,block(kind).defaultBlockState().setValue(TransmitterBlock.FACING,net.minecraft.core.Direction.EAST));return h.getBlockEntity(at);}
    @GameTest(template="empty",timeoutTicks=40)
    public static void wholeMessageBytesCompressionAndRealCrcFailure(GameTestHelper h){
        var source=messageDevice(h,new BlockPos(1,1,1),"message_source");var codec=messageDevice(h,new BlockPos(3,1,1),"message_codec");var rx=messageDevice(h,new BlockPos(5,1,1),"message_receiver");h.setBlock(new BlockPos(2,1,1),block("coaxial_port"));h.setBlock(new BlockPos(4,1,1),block("coaxial_port"));
        String text="Привіт! "+"A".repeat(600);source.configureMessage(message(text,"raw"));
        for(String op:java.util.List.of("raw","rle","huffman")){codec.configureMessage(message("",op));rx.configureMessage(message("",op.equals("raw")?"raw":"decode"));var result=RadioCapture.capture(h.getLevel(),rx.getBlockPos());h.assertTrue(result.getBoolean("connected")&&result.getBoolean("messageMatch"),"Full actual bytes must round-trip through "+op+": "+result.getString("messageError"));h.assertTrue(result.getString("messageText").equals(text)&&result.getIntArray("received").length==512,"Preview must not truncate full UTF-8 message");if(!op.equals("raw"))h.assertTrue(ChallengeRules.achieved(result).contains(op+"_savings"),"Header-inclusive savings must be measured");}
        codec=messageDevice(h,new BlockPos(3,1,1),"channel_coder");codec.configureMessage(message("","crc_encode"));rx.configureMessage(message("","crc_decode"));var clean=RadioCapture.capture(h.getLevel(),rx.getBlockPos());h.assertTrue(clean.getBoolean("messageMatch")&&ChallengeRules.achieved(clean).contains("crc_verified"),"Actual CRC frame must verify");
        var noisy=rx.settings().save();var lab=noisy.getCompound("lab").copy();lab.putDouble("mean",20);lab.putDouble("variance",0);noisy.put("lab",lab);rx.configure(RadioSettings.read(noisy));var corrupt=RadioCapture.capture(h.getLevel(),rx.getBlockPos());h.assertTrue(corrupt.getString("messageError").equals("data_crc")&&!corrupt.contains("messageText"),"Noise must alter actual bits and CRC must reject without substituting source");h.assertTrue(ChallengeRules.achieved(corrupt).contains("crc_rejected"),"CRC rejection is a measured challenge");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=40)
    public static void physicalCustomCardAndBareReceiver(GameTestHelper h){
        var source=messageDevice(h,new BlockPos(1,1,1),"message_source");var rx=messageDevice(h,new BlockPos(3,1,1),"message_receiver");h.setBlock(new BlockPos(2,1,1),block("coaxial_port"));source.configureMessage(message("ABBA","raw"));source.media.setStackInSlot(2,codeCard(SymbolCodebook.DEFAULT));var raw=RadioCapture.capture(h.getLevel(),rx.getBlockPos());h.assertTrue(raw.getBoolean("connected")&&raw.getInt("messageWireBits")==6&&!raw.contains("messageText"),"No RX card: actual six bits, no fabricated UTF-8");h.assertTrue(ChallengeRules.achieved(raw).contains("raw_code_stream"),"Raw symbol stream challenge");rx.media.setStackInSlot(2,codeCard(SymbolCodebook.DEFAULT));var decoded=RadioCapture.capture(h.getLevel(),rx.getBlockPos());h.assertTrue(decoded.getString("messageText").equals("ABBA")&&ChallengeRules.achieved(decoded).contains("custom_codes"),"Matching local card must decode actual bits");rx.media.setStackInSlot(2,codeCard("C=0\nD=10\nSPACE=11"));var wrong=RadioCapture.capture(h.getLevel(),rx.getBlockPos());h.assertTrue(wrong.getString("messageText").equals("CDDC")&&!wrong.getBoolean("messageMatch"),"Wrong card must produce its own result");
        h.setBlock(new BlockPos(3,1,1),PRSCraft.TRANSMITTER.get());rx=h.getBlockEntity(new BlockPos(3,1,1));rx.configure(cleanRx());rx.configureMessage(message("","raw"));rx.media.setStackInSlot(2,codeCard(SymbolCodebook.DEFAULT));h.assertTrue(RadioCapture.capture(h.getLevel(),rx.getBlockPos()).getString("messageText").equals("ABBA"),"Existing radio RX accepts symbol cards too");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=40)
    public static void messageCodecsRouteInEveryOrientation(GameTestHelper h){
        var center=new BlockPos(3,1,3);
        for(String kind:java.util.List.of("message_codec","channel_coder"))for(var front:net.minecraft.core.Direction.Plane.HORIZONTAL){for(var d:net.minecraft.core.Direction.Plane.HORIZONTAL)for(int i=1;i<=2;i++)h.setBlock(center.relative(d,i),Blocks.AIR);var source=messageDevice(h,center.relative(front.getOpposite(),2),"message_source");source.configureMessage(message("real bytes "+"A".repeat(200),"raw"));h.setBlock(center,block(kind).defaultBlockState().setValue(TransmitterBlock.FACING,front));RadioBlockEntity coder=h.getBlockEntity(center);coder.configureMessage(message("",kind.equals("message_codec")?"huffman":"hamming_encode"));var rx=messageDevice(h,center.relative(front,2),"message_receiver");rx.configureMessage(message("",kind.equals("message_codec")?"decode":"hamming_decode"));h.setBlock(center.relative(front.getOpposite()),block("coaxial_port"));h.setBlock(center.relative(front),block("coaxial_port"));var result=RadioCapture.capture(h.getLevel(),rx.getBlockPos());h.assertTrue(result.getBoolean("messageMatch"),"Actual "+kind+" route failed facing "+front+": "+result.getString("messageError"));}
        h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=20)
    public static void thirdMediaSlotAndPlotSurviveLegacyAndTemplates(GameTestHelper h){
        var device=messageDevice(h,new BlockPos(3,1,3),"message_source");device.configureMessage(message("AB BA","raw"));var plot=new PlotSettings(false,false,.002,.008,-2,2,65);device.configurePlot(plot);device.media.setStackInSlot(1,new net.minecraft.world.item.ItemStack(PRSCraft.DATA_CARD.get()));device.media.setStackInSlot(2,codeCard(SymbolCodebook.DEFAULT));var tag=device.saveWithFullMetadata(h.getLevel().registryAccess());var restored=new RadioBlockEntity(device.getBlockPos(),device.getBlockState());restored.loadWithComponents(tag,h.getLevel().registryAccess());h.assertTrue(restored.media.getSlots()==3&&restored.codebook().codes().equals(device.codebook().codes())&&restored.plot().equals(plot)&&restored.message().equals(device.message()),"Third card slot, message and view must persist");
        var legacy=tag.copy();legacy.remove("plot");legacy.remove("message");var inventory=legacy.getCompound("media").copy();inventory.putInt("Size",2);var entries=inventory.getList("Items",10);entries.removeIf(t->((net.minecraft.nbt.CompoundTag)t).getInt("Slot")==2);legacy.put("media",inventory);var old=new RadioBlockEntity(device.getBlockPos(),device.getBlockState());old.loadWithComponents(legacy,h.getLevel().registryAccess());h.assertTrue(old.media.getSlots()==3&&old.media.getStackInSlot(1).is(PRSCraft.DATA_CARD.get())&&old.media.getStackInSlot(2).isEmpty()&&old.plot().equals(PlotSettings.DEFAULT),"Legacy two-slot inventory must migrate without losing recordings");
        var player=h.makeMockPlayer(net.minecraft.world.level.GameType.CREATIVE);player.setPos(device.getBlockPos().getX()+.5,device.getBlockPos().getY()+.5,device.getBlockPos().getZ()+.5);device.media.setStackInSlot(0,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.PAPER));var menu=new PresetMenu(1,player.getInventory(),device);h.assertTrue(menu.clickMenuButton(player,0),"Save message template");var template=device.media.extractItem(0,1,false);device.configureMessage(message("changed","raw"));device.configurePlot(PlotSettings.DEFAULT);device.media.setStackInSlot(0,template);h.assertTrue(device.message().text().equals("AB BA")&&device.plot().equals(plot),"Template restores source and view without changing card");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=20)
    public static void bulkEraserChargesOnlyPaintedPixelsWithPremium(GameTestHelper h){
        var pos=new BlockPos(3,1,3);h.setBlock(pos,PRSCraft.BOARD.get());DrawingBoardEntity board=h.getBlockEntity(pos);board.paint(new int[]{0,1,127,1024,16383},2);var sponge=new net.minecraft.world.item.ItemStack(PRSCraft.ERASER.get());sponge.setDamageValue(sponge.getMaxDamage()-5);h.assertTrue(BoardTools.clearAllCost(board.pixels())==6&&!BoardTools.clearAll(board,sponge)&&board.pixels()[0]==2,"ceil(5×1.1)=6; insufficient charge must be atomic");sponge.setDamageValue(sponge.getMaxDamage()-6);h.assertTrue(BoardTools.clearAll(board,sponge)&&!sponge.isEmpty()&&BoardTools.remaining(sponge)==0,"Exact premium empties but never destroys eraser");h.assertTrue(BoardTools.clearAllCost(board.pixels())==0&&BoardTools.clearAll(board,sponge),"Blank board is free even at zero charge");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=40)
    public static void noisyHammingReceiverRepairsActualWireDecisions(GameTestHelper h){
        var source=messageDevice(h,new BlockPos(1,1,1),"message_source");source.configureMessage(message("A","raw"));var coder=messageDevice(h,new BlockPos(3,1,1),"channel_coder");coder.configureMessage(message("","hamming_encode"));var rx=messageDevice(h,new BlockPos(5,1,1),"message_receiver");rx.configureMessage(message("","hamming_decode"));h.setBlock(new BlockPos(2,1,1),block("coaxial_port"));h.setBlock(new BlockPos(4,1,1),block("coaxial_port"));boolean repaired=false;
        for(int i=1;i<=180&&!repaired;i++){double sigma=.1+i*.005;var settings=rx.settings().save();var lab=settings.getCompound("lab").copy();lab.putDouble("mean",0);lab.putDouble("variance",sigma*sigma);settings.put("lab",lab);rx.configure(RadioSettings.read(settings));var result=RadioCapture.capture(h.getLevel(),rx.getBlockPos());repaired=result.getInt("messageBitErrors")>0&&result.getInt("messageCorrectedWords")>0&&result.getBoolean("messageMatch");}
        h.assertTrue(repaired,"At least one noisy actual decision must be corrected to exact original bytes");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=40)
    public static void virtualAndPhysicalCircuitsUseIdenticalEngineAndSettings(GameTestHelper h){
        setup(h);RadioBlockEntity source=h.getBlockEntity(TX),sink=h.getBlockEntity(RX);source.configure(cleanSource("sin(10)"));sink.configure(cleanRx());var real=RadioCapture.capture(h.getLevel(),sink.getBlockPos());var doc=new SimulatorDocument();int a=doc.add("transmitter",10,20),b=doc.add("transmitter",300,20);doc.wire(a,b,"A",SignalBudget.Medium.COAXIAL,new CableSettings(5,0));var world=doc.world(h.getLevel().registryAccess(),h.getLevel().getGameTime());((RadioBlockEntity)world.getBlockEntity(SimulatorDocument.position(a))).configure(source.settings());((RadioBlockEntity)world.getBlockEntity(SimulatorDocument.position(b))).configure(sink.settings());world.persist();var restored=new SimulatorDocument(doc.save()).world(h.getLevel().registryAccess(),h.getLevel().getGameTime());var virtual=CircuitCapture.capture(restored,SimulatorDocument.position(b));h.assertTrue(virtual.getBoolean("connected")&&java.util.Arrays.equals(real.getIntArray("received"),virtual.getIntArray("received")),"Virtual instrument must use exact physical waveform/loss engine");h.assertTrue(real.getDouble("loss")==virtual.getDouble("loss"),"Port-cable-port loss must agree");h.assertTrue(doc.node(a).getDouble("x")==10&&doc.node(b).getDouble("x")==300,"Canvas placement is independent of electrical settings");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=80)
    public static void packetArqAndIqReallyDecodeThroughVirtualComponents(GameTestHelper h){
        var doc=new SimulatorDocument();String[] kinds={"message_source","packet_sender","digital_modem","digital_modem","packet_receiver"};for(int i=0;i<kinds.length;i++){doc.add(kinds[i],i*200,10);if(i>0)doc.wire(i-1,i,"A",SignalBudget.Medium.COAXIAL,new CableSettings(1,0));}var world=doc.world(h.getLevel().registryAccess(),20);var source=(RadioBlockEntity)world.getBlockEntity(SimulatorDocument.position(0));source.configureMessage(message("Привіт з QPSK!","raw"));var sender=(RadioBlockEntity)world.getBlockEntity(SimulatorDocument.position(1));sender.configureProtocol(new ProtocolSettings(32,3,0,.05));var encoder=(RadioBlockEntity)world.getBlockEntity(SimulatorDocument.position(2));var decoder=(RadioBlockEntity)world.getBlockEntity(SimulatorDocument.position(3));var rx=(RadioBlockEntity)world.getBlockEntity(SimulatorDocument.position(4));
        for(String mode:java.util.List.of("qpsk","qam16")){encoder.configureMessage(message("",mode+"_encode"));decoder.configureMessage(message("",mode+"_decode"));var capture=CircuitCapture.capture(world,rx.getBlockPos());h.assertTrue(capture.getBoolean("messageMatch")&&capture.getCompound("protocolStats").getBoolean("acknowledged"),"Actual frames through "+mode+" must decode: "+capture.getString("messageError"));}
        rx.configureProtocol(new ProtocolSettings(32,0,1,.05));var lost=CircuitCapture.capture(world,rx.getBlockPos());h.assertTrue(lost.getBoolean("messageMatch")&&lost.getString("messageError").equals("packet_arq")&&lost.getCompound("protocolStats").getInt("attempts")==4,"Lost ACK must preserve received text but fail sender confirmation; retry bound belongs to sender");h.assertTrue(lost.getCompound("protocolStats").getInt("duplicates")>0,"Repeated frames must be deduplicated");
        rx.configureProtocol(ProtocolSettings.DEFAULT);encoder.configureMessage(message("","qpsk_encode"));decoder.configureMessage(message("","qpsk_decode"));var noisy=decoder.settings().save();var lab=noisy.getCompound("lab").copy();lab.putDouble("variance",.23*.23);lab.putDouble("mean",0);noisy.put("lab",lab);decoder.configure(RadioSettings.read(noisy));world.persist();boolean recovered=false;for(int tick=1;tick<=40&&!recovered;tick++){var capture=CircuitCapture.capture(doc.world(h.getLevel().registryAccess(),tick),rx.getBlockPos());var stats=capture.getCompound("protocolStats");recovered=capture.getBoolean("messageMatch")&&stats.getBoolean("acknowledged")&&stats.getInt("rejected")>0&&stats.getInt("retries")>0;}h.assertTrue(recovered,"A CRC-rejected noisy frame must recover from a fresh channel attempt, not original metadata");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=40)
    public static void orthogonalityMeterUsesBothRealInputsAndWindow(GameTestHelper h){
        var at=new BlockPos(3,1,3);h.setBlock(at,PRSCraft.ORTHOGONALITY_METER.get().defaultBlockState().setValue(TransmitterBlock.FACING,net.minecraft.core.Direction.EAST));h.setBlock(new BlockPos(1,1,3),PRSCraft.TRANSMITTER.get());h.setBlock(new BlockPos(3,1,1),PRSCraft.TRANSMITTER.get());h.setBlock(new BlockPos(2,1,3),block("coaxial_port"));h.setBlock(new BlockPos(3,1,2),block("coaxial_port"));((RadioBlockEntity)h.getBlockEntity(new BlockPos(1,1,3))).configure(cleanSource("sin(10)"));((RadioBlockEntity)h.getBlockEntity(new BlockPos(3,1,1))).configure(cleanSource("cos(10)"));RadioBlockEntity meter=h.getBlockEntity(at);var full=RadioCapture.capture(h.getLevel(),meter.getBlockPos());h.assertTrue(full.getCompound("orthogonalityResult").getBoolean("orthogonal"),"sin/cos must be orthogonal across full periods: fault="+full.getString("fault")+" span="+full.getDouble("span")+" result="+full.getCompound("orthogonalityResult"));meter.configureOrthogonality(new OrthogonalitySettings(0,.025,.01,false,100));var quarter=RadioCapture.capture(h.getLevel(),meter.getBlockPos());h.assertTrue(quarter.getCompound("orthogonalityResult").getBoolean("valid")&&!quarter.getCompound("orthogonalityResult").getBoolean("orthogonal"),"Quarter-period window must not be reported orthogonal");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=40)
    public static void localWaterDispersionAndLegacyCablePersistence(GameTestHelper h){
        setup(h);RadioBlockEntity tx=h.getBlockEntity(TX),rx=h.getBlockEntity(RX);tx.configure(cleanSource("sin(10)"));rx.configure(cleanRx());CableBlockEntity cable=h.getBlockEntity(CORNER);cable.configure(new CableSettings(1,0,.002,true));h.setBlock(CORNER.above(),Blocks.WATER);var wet=RadioCapture.capture(h.getLevel(),rx.getBlockPos());h.assertTrue(wet.getDouble("environmentalLoss")>0&&wet.getDouble("environmentalVariance")>0&&wet.getDouble("dispersion")>0,"Adjacent water and configured dispersion must affect actual route");var saved=cable.saveWithFullMetadata(h.getLevel().registryAccess());var loaded=new CableBlockEntity(cable.getBlockPos(),cable.getBlockState());loaded.loadWithComponents(saved,h.getLevel().registryAccess());h.assertTrue(loaded.settings().equals(cable.settings()),"Dispersion and environment switches persist");saved.remove("dispersion");saved.remove("environment");loaded.loadWithComponents(saved,h.getLevel().registryAccess());h.assertTrue(loaded.settings().dispersion()==0&&loaded.settings().environment(),"Old cable NBT retains zero-dispersion default");cable.configure(new CableSettings(1,0,.002,false));var insulated=RadioCapture.capture(h.getLevel(),rx.getBlockPos());h.assertTrue(insulated.getDouble("environmentalLoss")==0&&insulated.getDouble("environmentalVariance")==0,"Environmental model can be isolated without disabling dispersion");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=40)
    public static void simulatorDocumentAndVirtualBoardSurviveWorldAndItemSerialization(GameTestHelper h){
        var pos=new BlockPos(3,1,3);h.setBlock(pos,PRSCraft.SIMULATOR.get());SimulatorBlockEntity sim=h.getBlockEntity(pos);var doc=new SimulatorDocument();int source=doc.add("message_source",17,38),board=doc.add("drawing_board",300,90);var world=doc.world(h.getLevel().registryAccess(),42);((RadioBlockEntity)world.getBlockEntity(SimulatorDocument.position(source))).media.setStackInSlot(2,codeCard(SymbolCodebook.DEFAULT));((DrawingBoardEntity)world.getBlockEntity(SimulatorDocument.position(board))).paint(new int[]{0,127,16383},5);world.persist();sim.commit(doc);var tag=sim.saveWithFullMetadata(h.getLevel().registryAccess());var restored=new SimulatorBlockEntity(sim.getBlockPos(),sim.getBlockState());restored.loadWithComponents(tag,h.getLevel().registryAccess());var next=restored.document().world(h.getLevel().registryAccess(),43);h.assertTrue(((DrawingBoardEntity)next.getBlockEntity(SimulatorDocument.position(board))).pixels()[16383]==5,"Virtual canvas survives world serialization");h.assertTrue(((RadioBlockEntity)next.getBlockEntity(SimulatorDocument.position(source))).codebook().codes().size()==3,"Virtual card survives without creating real item entities");h.assertTrue(sim.collectComponents().has(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA),"Dropped simulator item carries its document");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=40)
    public static void protocolAndOrthogonalityTemplatesMigrateAndCoatOccupiesChest(GameTestHelper h){
        var at=new BlockPos(3,1,3);var device=messageDevice(h,at,"packet_sender");var protocol=new ProtocolSettings(64,7,.3,.2);device.configureProtocol(protocol);var template=DeviceConfiguration.template(device);h.assertTrue(template.getCompound("protocol").getInt("retries")==7,"Templates retain frame protocol parameters");var restored=new RadioBlockEntity(device.getBlockPos(),device.getBlockState());restored.loadWithComponents(device.saveWithFullMetadata(h.getLevel().registryAccess()),h.getLevel().registryAccess());h.assertTrue(restored.protocol().equals(protocol),"Protocol survives world reload");var legacy=device.saveWithFullMetadata(h.getLevel().registryAccess());legacy.remove("protocol");var old=new RadioBlockEntity(device.getBlockPos(),device.getBlockState());old.loadWithComponents(legacy,h.getLevel().registryAccess());h.assertTrue(old.protocol().equals(ProtocolSettings.DEFAULT),"Legacy devices use default protocol");h.assertTrue(PRSCraft.DOCTORAL_COAT.get().getEquipmentSlot()==net.minecraft.world.entity.EquipmentSlot.CHEST&&PRSCraft.DOCTORAL_COAT.get().getDefense()==3,"Coat is genuine chest armor");h.assertTrue(new net.minecraft.world.item.ItemStack(PRSCraft.DOCTORAL_COAT.get()).getMaxDamage()==256,"Coat durability is real equipment data");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=40)
    public static void notebookCarriesExactImmutableTemplateAndCapture(GameTestHelper h){
        setup(h);RadioBlockEntity rx=h.getBlockEntity(RX);rx.configure(cleanRx());var snapshot=RadioCapture.capture(h.getLevel(),rx.getBlockPos());var entry=new net.minecraft.nbt.CompoundTag();NotebookRecords.annotate(entry,"sin(10) experiment","Compare before and after gain");entry.put("capture",snapshot);entry.put("template",DeviceConfiguration.template(rx));var entries=new net.minecraft.nbt.ListTag();NotebookRecords.append(entries,entry);var tag=new net.minecraft.nbt.CompoundTag();tag.put("experiments",entries);var notebook=new net.minecraft.world.item.ItemStack(PRSCraft.NOTEBOOK.get());notebook.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(tag));rx.configure(receiver(125));snapshot.putIntArray("received",new int[512]);var saved=NotebookNetwork.data(notebook).getList("experiments",10).getCompound(0);h.assertTrue(saved.getCompound("template").getCompound("settings").getDouble("carrierMHz")==100,"Saved template must not follow later instrument settings");h.assertTrue(!java.util.Arrays.equals(saved.getCompound("capture").getIntArray("received"),new int[512]),"Saved graph must not follow later capture mutation");var serialized=notebook.save(h.getLevel().registryAccess());var loaded=net.minecraft.world.item.ItemStack.parse(h.getLevel().registryAccess(),serialized).orElseThrow();h.assertTrue(NotebookNetwork.data(loaded).equals(NotebookNetwork.data(notebook)),"Item serialization retains notes, graph and template");h.succeed();
    }
}
