package dev.jarofcucumbers.prscraft;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.world.level.Level;
import java.util.*;
/** Bounded acyclic signal graph with aligned grids for independent input branches. */
public final class CircuitCapture{
    private CircuitCapture(){}
    private record Input(Node node,CircuitNetwork.Route route){}
    private static final class Node{
        final RadioBlockEntity device;Input a,b;Node(RadioBlockEntity device){this.device=device;}
    }
    private static final class Frame{
        double[] samples,original,sampled,sourceOutput,inputA,inputB;RadioSettings source;BlockPos sourcePos;
        String units="u";double length,loss,damageVariance,sigma,dispersion,environmentalLoss,environmentalVariance;CircuitMath.Correlation correlation;Orthogonality.Result orthogonality;GrayCodec.Result gray;MessageSignal message,messageSource,messageInput;
    }
    private static final class Engine{
        final CircuitAccess level;final Double carrier;final Map<BlockPos,Node> nodes=new HashMap<>();final Set<BlockPos> stack=new HashSet<>();
        final Map<Node,Frame> frames=new IdentityHashMap<>();int attempts,previewOffset,transmissionAttempt;final Map<Node,Frame> packetBuffers=new IdentityHashMap<>();String fault="missing_input";double span;
        Engine(CircuitAccess level,Double carrier){this.level=level;this.carrier=carrier;}
        Node build(BlockPos pos){
            if(nodes.containsKey(pos))return nodes.get(pos);
            if(stack.contains(pos)){fault="feedback";return null;}if(++attempts>256||stack.size()>=16||nodes.size()>=32){fault="circuit_limit";return null;}
            if(!level.hasChunkAt(pos)||!(level.getBlockEntity(pos) instanceof RadioBlockEntity device))return null;
            Node node=new Node(device);
            if(device.isSource()&&device.settings().transmit()){nodes.put(pos,node);return node;}
            if(!device.settings().enabled()){fault="disabled";return null;}
            stack.add(pos);
            try{
                Direction facing=device.getBlockState().getValue(TransmitterBlock.FACING);
                node.a=input(pos,device.isSource()?null:facing.getOpposite());if(node.a==null)return null;
                boolean pair=device.kind().equals("adder")||device.kind().equals("correlator")||device.kind().equals("orthogonality_meter");
                if(pair){
                    if(device.kind().equals("correlator")&&device.dsp().autoCorrelation())node.b=node.a;
                    else {node.b=input(pos,facing.getCounterClockWise());if(node.b==null){fault="missing_b";return null;}}
                }
                nodes.put(pos,node);return node;
            }finally{stack.remove(pos);}
        }
        Input input(BlockPos pos,Direction side){
            for(var route:CircuitNetwork.inputs(level,pos,side,carrier)){
                Node endpoint=build(route.endpoint());if(endpoint!=null)return new Input(endpoint,route);
            }
            return null;
        }
        double chooseSpan(Node root){
            var visited=Collections.newSetFromMap(new IdentityHashMap<Node,Boolean>());List<Node> used=new ArrayList<>();collect(root,visited,used);
            List<SignalChain.Stage> stages=used.stream().filter(n->n.device.kind().equals("modulator")).map(n->new SignalChain.Stage("modulator",n.device.processor())).toList();
            double result=8;for(Node node:used)if(node.a==null)result=Math.min(result,node.device.kind().equals("message_source")?Math.min(128,Math.max(1,node.device.message().sourceBytes().length*8))/node.device.message().bitRate():SignalChain.span(node.device.settings(),stages));double requested=0;for(Node n:used)if(n.device.kind().equals("orthogonality_meter"))requested=Math.max(requested,n.device.orthogonality().end());if(requested>0){result=Math.max(result,requested);for(Node n:used)if(n.a==null&&java.util.Set.of("analog","sampled","quantized").contains(n.device.settings().signalMode())&&SignalProgram.parse(n.device.settings().expression()).maxHz()>256/result)throw new IllegalArgumentException("orthogonality_resolution");}return Math.max(.000001,result);
        }
        void collect(Node node,Set<Node> visited,List<Node> list){if(!visited.add(node))return;list.add(node);if(node.a!=null)collect(node.a.node(),visited,list);if(node.b!=null)collect(node.b.node(),visited,list);}
        Frame transmit(Input input,BlockPos destination){
            Frame child=evaluate(input.node());var route=input.route();Frame f=copy(child);f.samples=child.samples.clone();
            double actualLoss=route.medium().lossDb(route.metres(),child.source.carrierMHz())+route.medium().lossDb(0,child.source.carrierMHz())+route.damageLoss()+route.environmentalLoss();
            double gain=Math.pow(10,-actualLoss/20),sigma=Math.sqrt(route.noiseVariance());
            long seed=level.getGameTime()^(0x9e3779b97f4a7c15L*transmissionAttempt)^destination.asLong()^route.endpoint().asLong()^route.path().hashCode();
            for(int i=0;i<f.samples.length;i++)f.samples[i]=f.samples[i]*gain+sigma*SignalProgram.noise(seed,i);
            if(f.message!=null){f.message=f.message.channel(gain,0,sigma,seed,false);f.message=f.message.disperse(route.dispersion());f.samples=f.message.preview(previewOffset);}
            else f.samples=CableSettings.disperse(f.samples,span/f.samples.length,route.dispersion());
            f.dispersion+=route.dispersion();f.environmentalLoss+=route.environmentalLoss();f.environmentalVariance+=route.environmentalVariance();f.length+=route.metres();f.loss+=actualLoss;f.damageVariance+=route.noiseVariance();return f;
        }
        Frame copy(Frame source){Frame f=new Frame();f.source=source.source;f.sourcePos=source.sourcePos;f.original=source.original;f.sampled=source.sampled;f.sourceOutput=source.sourceOutput;f.units=source.units;f.length=source.length;f.loss=source.loss;f.damageVariance=source.damageVariance;f.dispersion=source.dispersion;f.environmentalLoss=source.environmentalLoss;f.environmentalVariance=source.environmentalVariance;f.message=source.message;f.messageSource=source.messageSource;return f;}
        Frame evaluate(Node node){
            if(frames.containsKey(node))return frames.get(node);RadioBlockEntity device=node.device;RadioSettings settings=device.settings();Frame f;
            if(node.a==null){
                f=new Frame();f.source=settings;f.sourcePos=device.getBlockPos();f.original=SignalChain.generate(settings,span,512,true);f.sampled=f.original.clone();
                if(settings.signalMode().equals("sampled")||settings.signalMode().equals("quantized")){
                    var program=SignalProgram.parse(settings.expression());double scale=SourceMath.scale(settings);
                    for(int i=0;i<512;i++)f.sampled[i]=program.sampled(span*i/512+settings.lab().offsetSeconds(),settings.sampleHz(),settings.secondKind(),settings.duty())*scale;
                }
                f.sourceOutput=settings.enabled()?SignalChain.generate(settings,span,512,false):new double[512];f.samples=f.sourceOutput.clone();
                double gain=Transmission.gain(settings.powerDbm(),0);for(int i=0;i<512;i++)f.samples[i]*=gain;
                if(device.kind().equals("message_source")){f.message=MessageSignal.source(device.message(),settings.enabled(),gain,device.codebook());f.messageSource=f.message;if(settings.enabled())f.message=f.message.channel(1,settings.lab().noiseMean(),Math.hypot(Math.sqrt(settings.lab().noiseVariance()),settings.thermal()?SignalProgram.thermalRms(f.message.rate/2):0),level.getGameTime()^(0x9e3779b97f4a7c15L*transmissionAttempt)^device.getBlockPos().asLong(),false);f.samples=f.message.preview(previewOffset);f.original=f.samples.clone();f.sampled=f.original.clone();f.sourceOutput=f.original.clone();}
            }else{
                Frame a=transmit(node.a,device.getBlockPos());Frame b=node.b==null?null:node.b==node.a?a:transmit(node.b,device.getBlockPos());
                f=copy(a);f.messageInput=a.message;f.inputA=a.samples;f.inputB=b==null?new double[0]:b.samples;
                if(b!=null){if(!a.units.equals(b.units))throw new IllegalArgumentException("incompatible_units");f.length=Math.max(a.length,b.length);f.loss=Math.max(a.loss,b.loss);f.damageVariance=Math.max(a.damageVariance,b.damageVariance);}
                if(a.message!=null&&!Set.of("message_codec","channel_coder","amplifier","radio","message_receiver","digital_modem","packet_sender","packet_receiver").contains(device.kind())||b!=null&&b.message!=null)throw new IllegalArgumentException("data_processor");
                var dsp=device.dsp();
                switch(device.kind()){
                    case "packet_sender" -> {if(a.message==null)throw new IllegalArgumentException("data_input");f.message=a.message.packetEncode(device.protocol(),device.message()).channel(Transmission.gain(settings.powerDbm(),0),0,0,0,false);f.samples=f.message.preview(previewOffset);packetBuffers.put(node,f);}
                    case "digital_modem" -> {if(a.message==null)throw new IllegalArgumentException("data_input");double sigma=Math.hypot(Math.sqrt(settings.lab().noiseVariance()),settings.thermal()?SignalProgram.thermalRms(a.message.rate/(a.message.iq==null?2:2*a.message.iq.bitsPerSymbol())):0);var incoming=a.message.channel(1,settings.lab().noiseMean(),sigma,level.getGameTime()^(0x9e3779b97f4a7c15L*transmissionAttempt)^device.getBlockPos().asLong(),false);f.messageInput=incoming;f.message=incoming.modem(device.message()).channel(Transmission.gain(settings.powerDbm(),0),0,0,0,false);f.units=f.message.iq==null?"u":"bit decisions";f.samples=f.message.preview(previewOffset);}
                    case "packet_receiver" -> {if(a.message==null||!a.message.packetCoded||a.message.iq!=null)throw new IllegalArgumentException("packet_mode");if(Math.abs(device.message().bitRate()-a.message.rate)>1e-9)throw new IllegalArgumentException("data_clock");var senderCfg=a.message.packetStats.contains("senderConfig")?ProtocolSettings.read(a.message.packetStats.getCompound("senderConfig")):device.protocol();var arq=new ProtocolSettings(device.protocol().payloadBytes(),senderCfg.retries(),device.protocol().ackLoss(),senderCfg.timeout());var reception=new PacketCodec.Reception(arq);MessageSignal incoming=a.message;for(int attempt=0;attempt<=arq.retries();attempt++){if(attempt>0){transmissionAttempt=attempt;frames.clear();frames.putAll(packetBuffers);a=transmit(node.a,device.getBlockPos());incoming=a.message;}incoming=incoming.channel(1,settings.lab().noiseMean(),Math.hypot(Math.sqrt(settings.lab().noiseVariance()),settings.thermal()?SignalProgram.thermalRms(incoming.rate/2):0),level.getGameTime()^(0x9e3779b97f4a7c15L*attempt)^device.getBlockPos().asLong(),false);if(reception.accept(incoming.observed(device.message().threshold()),level.getGameTime()^(0x632be59bd9b4e019L*attempt)^device.getBlockPos().asLong()))break;}f.messageInput=incoming;f.message=incoming.packetResult(reception,device.message());f.inputA=incoming.preview(previewOffset);f.samples=f.message.preview(previewOffset);}
                    case "message_codec","channel_coder" -> {if(a.message==null)throw new IllegalArgumentException("data_input");double sigma=Math.hypot(Math.sqrt(settings.lab().noiseVariance()),settings.thermal()?SignalProgram.thermalRms(a.message.rate/2):0);var incoming=a.message.channel(1,settings.lab().noiseMean(),sigma,level.getGameTime()^(0x9e3779b97f4a7c15L*transmissionAttempt)^device.getBlockPos().asLong(),false);f.messageInput=incoming;f.inputA=incoming.preview(previewOffset);f.message=incoming.process(device.message(),device.kind(),device.codebook()).channel(Transmission.gain(settings.powerDbm(),0),0,0,0,false);f.samples=f.message.preview(previewOffset);}
                    case "amplifier" -> {if(a.message!=null){f.message=a.message.channel(Math.pow(10,device.processor().gainDb()/20),0,0,0,true);f.samples=f.message.preview(previewOffset);}else f.samples=SignalChain.process(a.samples,span,List.of(new SignalChain.Stage(device.kind(),device.processor())));}
                    case "gray_converter" -> {if(!a.units.equals("u"))throw new IllegalArgumentException("incompatible_units");f.gray=GrayCodec.process(a.samples,span,device.gray());f.samples=f.gray.samples();}
                    case "orthogonality_meter" -> {f.orthogonality=Orthogonality.measure(a.samples,b.samples,span,device.orthogonality());f.samples=a.samples.clone();}
                    case "adder" -> f.samples=CircuitMath.add(a.samples,b.samples,dsp.gainA(),dsp.gainB());
                    case "integrator" -> {f.samples=CircuitMath.integral(a.samples,span,dsp.endSeconds());f.units=a.units+"*s";}
                    case "delay_line" -> f.samples=CircuitMath.delay(a.samples,span,dsp.delaySeconds());
                    case "time_gate" -> f.samples=CircuitMath.gate(a.samples,span,dsp.gateSeconds(),dsp.gateWidth(),dsp.gateHold());
                    case "correlator" -> {f.samples=CircuitMath.productIntegral(a.samples,b.samples,span,dsp.endSeconds(),dsp.normalized());f.correlation=CircuitMath.correlate(a.samples,b.samples,span,dsp.lagSeconds(),dsp.normalized());f.units=dsp.normalized()?"1":"("+a.units+")²*s";}
                    case "radio","message_receiver" -> {
                        if(device.kind().equals("message_receiver")&&a.message==null)throw new IllegalArgumentException("data_input");
                        f.samples=a.samples.clone();double physical=ChannelNoise.interferenceRms(level,device.getBlockPos(),a.sourcePos,node.a.route().medium(),settings);
                        double sigma=Transmission.rms(settings,a.source,physical);f.sigma=sigma;long seed=level.getGameTime()^(0x9e3779b97f4a7c15L*transmissionAttempt)^device.getBlockPos().asLong();
                        for(int i=0;i<512;i++)f.samples[i]+=settings.lab().noiseMean()+sigma*SignalProgram.noise(seed,i);
                        if(a.message!=null){sigma=Math.hypot(Math.sqrt(settings.lab().noiseVariance()),settings.thermal()?Math.hypot(SignalProgram.thermalRms(a.message.rate/2),physical):0);f.sigma=sigma;f.message=a.message.channel(1,settings.lab().noiseMean(),sigma,seed,false);f.samples=f.message.preview(previewOffset);}
                    }
                    default -> {f.samples=SignalChain.process(a.samples,span,List.of(new SignalChain.Stage(device.kind(),device.processor())));if(device.kind().equals("modulator")&&device.processor().modulation().equals("am"))f.units="u";}
                }
            }
            for(double value:f.samples)if(!Double.isFinite(value)||Math.abs(value)>1e12)throw new IllegalArgumentException("signal_range");
            frames.put(node,f);return f;
        }
    }
    public static CompoundTag capture(Level level,BlockPos pos){return capture(CircuitAccess.of(level),pos);}
    public static CompoundTag capture(CircuitAccess level,BlockPos pos){
        RadioBlockEntity device=(RadioBlockEntity)level.getBlockEntity(pos);var data=new CompoundTag();RadioSettings local=device.settings();
        data.putLong("device",pos.asLong());data.put("plot",device.plot().save());data.putBoolean("correlationView",device.correlationView());data.put("settings",local.save());data.put("message",device.message().save());data.put("protocol",device.protocol().save());data.put("orthogonality",device.orthogonality().save());data.putBoolean("codebookPresent",device.media.getStackInSlot(2).is(PRSCraft.CODEBOOK_CARD.get()));data.put("processor",device.processor().save());data.put("dsp",device.dsp().save());data.put("gray",device.gray().save());data.putString("kind",device.kind());
        data.putInt("graphLayers",device.graphLayers());data.putInt("graphTerms",device.graphTerms());data.putLong("time",level.getGameTime());data.putDouble("referenceWatts",.1);
        boolean rootSource=device.isSource()&&local.transmit();Engine engine=new Engine(level,device.isSource()&&!local.transmit()?local.carrierMHz():null);
        engine.previewOffset=device.message().previewOffset();Node root=engine.build(pos);data.putBoolean("dataStream",engine.nodes.values().stream().anyMatch(n->n.device.kind().equals("message_source"))||Set.of("message_source","message_codec","channel_coder","message_receiver","digital_modem","packet_sender","packet_receiver").contains(device.kind()));data.putBoolean("physicalConnected",root!=null&&!rootSource);data.putBoolean("connected",root!=null&&!rootSource);
        if(root==null){data.putString("fault",engine.fault);if(data.getBoolean("dataStream"))data.putString("messageError",engine.fault);return data;}
        try{
            engine.span=engine.chooseSpan(root);Frame f=engine.evaluate(root);
            if(f.message!=null){int start=f.message.offset(engine.previewOffset),count=f.message.windowBits(engine.previewOffset);engine.span=count/f.message.rate;f.samples=f.message.previewWindow(start,count);f.original=f.messageSource.previewWindow(start,count);f.sampled=f.original.clone();f.sourceOutput=f.original.clone();f.inputA=f.messageInput==null?f.samples:f.messageInput.previewWindow(start,count);}
            data.put("source",f.source.save());data.putIntArray("original",SignalChain.pack(f.original));data.putIntArray("sampled",SignalChain.pack(f.sampled));data.putIntArray("sourceOutput",SignalChain.pack(f.sourceOutput));
            data.putIntArray("stageInput",SignalChain.pack(f.inputA==null?f.samples:f.inputA));data.putIntArray("inputB",SignalChain.pack(f.inputB==null?new double[0]:f.inputB));
            // RX overlays its input with its received output; processor overlays pre/post processing.
            data.putIntArray("emitted",SignalChain.pack(device.isSource()&&!local.transmit()?f.inputA:f.samples));data.putIntArray("received",SignalChain.pack(f.samples));
            data.putDouble("span",engine.span);data.putString("units",f.units);data.putDouble("metres",f.length);data.putDouble("loss",f.loss);data.putDouble("sigma",f.sigma);data.putDouble("dispersion",f.dispersion);data.putDouble("environmentalLoss",f.environmentalLoss);data.putDouble("environmentalVariance",f.environmentalVariance);data.putDouble("cableVariance",f.damageVariance);data.putDouble("noiseMean",local.lab().noiseMean());
            if(root.a!=null)data.putString("medium",root.a.route().medium().name());
            List<Node> used=new ArrayList<>();engine.collect(root,Collections.newSetFromMap(new IdentityHashMap<Node,Boolean>()),used);
            data.putString("pipeline",String.join(" → ",used.stream().filter(n->!n.device.isSource()).map(n->n.device.kind()).toList().reversed()));data.putInt("activeNodes",used.size());
            ListTag signalEdges=new ListTag();for(Node n:used)for(int side=0;side<2;side++){
                Input input=side==0?n.a:n.b;if(input==null||side==1&&n.b==n.a)continue;
                var edge=new CompoundTag();edge.putLong("from",input.route().endpoint().asLong());edge.putLong("to",n.device.getBlockPos().asLong());edge.putString("input",n.device.isSource()?"RX":side==0?"A":"B");signalEdges.add(edge);
            }data.put("signalEdges",signalEdges);
            Set<BlockPos> cables=new HashSet<>();ListTag sources=new ListTag();
            for(Node n:used){for(Input input:new Input[]{n.a,n.b})if(input!=null)for(BlockPos node:input.route().path())if(level.getBlockState(node).getBlock() instanceof CableBlock)cables.add(node);
                if(n.a==null){var source=n.device.settings().save();source.putString("position",n.device.getBlockPos().toShortString());sources.add(source);}}
            data.put("sources",sources);data.putDouble("wireMetres",cables.stream().mapToDouble(c->CircuitNetwork.cable(level,c).metres()).sum());data.putInt("cables",cables.size());
            if(f.message!=null)f.message.describe(data,device.message(),device.isSource()&&!local.transmit(),device.codebook());
            IqSignal constellation=f.messageInput!=null&&f.messageInput.iq!=null?f.messageInput.iq:f.message==null?null:f.message.iq;if(constellation!=null){data.putString("iqMode",constellation.mode());data.putIntArray("iqI",SignalChain.pack(Arrays.copyOf(constellation.i(),Math.min(512,constellation.i().length))));data.putIntArray("iqQ",SignalChain.pack(Arrays.copyOf(constellation.q(),Math.min(512,constellation.q().length))));data.putDouble("symbolRate",f.message.rate/constellation.bitsPerSymbol());}
            if(f.gray!=null){data.putString("grayInput",f.gray.input());data.putString("grayOutput",f.gray.output());}
            if(f.orthogonality!=null)data.put("orthogonalityResult",f.orthogonality.save());
            if(f.correlation!=null){data.putIntArray("correlation",SignalChain.pack(f.correlation.values()));data.putDouble("lagSpan",f.correlation.lagSpan());data.putDouble("peakLag",f.correlation.peakLag());data.putDouble("peakValue",f.correlation.peakValue());}
            if(f.message==null&&!rootSource&&device.processor().demodulate()&&f.units.equals("u"))data.putString("decoded",device.processor().modulation().equals("urz")||device.processor().modulation().equals("unrz")?SignalChain.decodeUnipolar(f.samples,engine.span,device.processor().symbolHz(),local.lab().amplitudeOne(),local.duty(),device.processor().modulation().equals("urz")):SignalChain.decodeBpsk(f.samples,engine.span,device.processor().oscillatorHz(),device.processor().symbolHz()));
        }catch(IllegalArgumentException e){data.putBoolean("connected",false);data.putString("fault",e.getMessage());if(data.getBoolean("dataStream"))data.putString("messageError",e.getMessage());if(e instanceof SymbolCodebook.MissingSymbol missing)data.putString("messageDetail",String.format(java.util.Locale.ROOT,"U+%04X",missing.codepoint));}
        return data;
    }
}
