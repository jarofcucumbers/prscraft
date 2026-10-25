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
        String units="u";double length,loss,damageVariance,sigma;CircuitMath.Correlation correlation;GrayCodec.Result gray;
    }
    private static final class Engine{
        final Level level;final Double carrier;final Map<BlockPos,Node> nodes=new HashMap<>();final Set<BlockPos> stack=new HashSet<>();
        final Map<Node,Frame> frames=new IdentityHashMap<>();int attempts;String fault="missing_input";double span;
        Engine(Level level,Double carrier){this.level=level;this.carrier=carrier;}
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
                boolean pair=device.kind().equals("adder")||device.kind().equals("correlator");
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
            double result=8;for(Node node:used)if(node.a==null)result=Math.min(result,SignalChain.span(node.device.settings(),stages));return Math.max(.000001,result);
        }
        void collect(Node node,Set<Node> visited,List<Node> list){if(!visited.add(node))return;list.add(node);if(node.a!=null)collect(node.a.node(),visited,list);if(node.b!=null)collect(node.b.node(),visited,list);}
        Frame transmit(Input input,BlockPos destination){
            Frame child=evaluate(input.node());var route=input.route();Frame f=copy(child);f.samples=child.samples.clone();
            double actualLoss=route.medium().lossDb(route.metres(),child.source.carrierMHz())+route.medium().lossDb(0,child.source.carrierMHz())+route.damageLoss();
            double gain=Math.pow(10,-actualLoss/20),sigma=Math.sqrt(route.noiseVariance());
            long seed=level.getGameTime()^destination.asLong()^route.endpoint().asLong()^route.path().hashCode();
            for(int i=0;i<f.samples.length;i++)f.samples[i]=f.samples[i]*gain+sigma*SignalProgram.noise(seed,i);
            f.length+=route.metres();f.loss+=actualLoss;f.damageVariance+=route.noiseVariance();return f;
        }
        Frame copy(Frame source){Frame f=new Frame();f.source=source.source;f.sourcePos=source.sourcePos;f.original=source.original;f.sampled=source.sampled;f.sourceOutput=source.sourceOutput;f.units=source.units;f.length=source.length;f.loss=source.loss;f.damageVariance=source.damageVariance;return f;}
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
            }else{
                Frame a=transmit(node.a,device.getBlockPos());Frame b=node.b==null?null:node.b==node.a?a:transmit(node.b,device.getBlockPos());
                f=copy(a);f.inputA=a.samples;f.inputB=b==null?new double[0]:b.samples;
                if(b!=null){if(!a.units.equals(b.units))throw new IllegalArgumentException("incompatible_units");f.length=Math.max(a.length,b.length);f.loss=Math.max(a.loss,b.loss);f.damageVariance=Math.max(a.damageVariance,b.damageVariance);}
                var dsp=device.dsp();
                switch(device.kind()){
                    case "gray_converter" -> {if(!a.units.equals("u"))throw new IllegalArgumentException("incompatible_units");f.gray=GrayCodec.process(a.samples,span,device.gray());f.samples=f.gray.samples();}
                    case "adder" -> f.samples=CircuitMath.add(a.samples,b.samples,dsp.gainA(),dsp.gainB());
                    case "integrator" -> {f.samples=CircuitMath.integral(a.samples,span,dsp.endSeconds());f.units=a.units+"*s";}
                    case "delay_line" -> f.samples=CircuitMath.delay(a.samples,span,dsp.delaySeconds());
                    case "time_gate" -> f.samples=CircuitMath.gate(a.samples,span,dsp.gateSeconds(),dsp.gateWidth(),dsp.gateHold());
                    case "correlator" -> {f.samples=CircuitMath.productIntegral(a.samples,b.samples,span,dsp.endSeconds(),dsp.normalized());f.correlation=CircuitMath.correlate(a.samples,b.samples,span,dsp.lagSeconds(),dsp.normalized());f.units=dsp.normalized()?"1":"("+a.units+")²*s";}
                    case "radio" -> {
                        f.samples=a.samples.clone();double physical=ChannelNoise.interferenceRms(level,device.getBlockPos(),a.sourcePos,node.a.route().medium(),settings);
                        double sigma=Transmission.rms(settings,a.source,physical);f.sigma=sigma;long seed=level.getGameTime()^device.getBlockPos().asLong();
                        for(int i=0;i<512;i++)f.samples[i]+=settings.lab().noiseMean()+sigma*SignalProgram.noise(seed,i);
                    }
                    default -> {f.samples=SignalChain.process(a.samples,span,List.of(new SignalChain.Stage(device.kind(),device.processor())));if(device.kind().equals("modulator")&&device.processor().modulation().equals("am"))f.units="u";}
                }
            }
            for(double value:f.samples)if(!Double.isFinite(value)||Math.abs(value)>1e12)throw new IllegalArgumentException("signal_range");
            frames.put(node,f);return f;
        }
    }
    public static CompoundTag capture(Level level,BlockPos pos){
        RadioBlockEntity device=(RadioBlockEntity)level.getBlockEntity(pos);var data=new CompoundTag();RadioSettings local=device.settings();
        data.putBoolean("correlationView",device.correlationView());data.put("settings",local.save());data.put("processor",device.processor().save());data.put("dsp",device.dsp().save());data.put("gray",device.gray().save());data.putString("kind",device.kind());
        data.putInt("graphLayers",device.graphLayers());data.putInt("graphTerms",device.graphTerms());data.putLong("time",level.getGameTime());data.putDouble("referenceWatts",.1);
        boolean rootSource=device.isSource()&&local.transmit();Engine engine=new Engine(level,device.isSource()&&!local.transmit()?local.carrierMHz():null);
        Node root=engine.build(pos);data.putBoolean("connected",root!=null&&!rootSource);
        if(root==null){data.putString("fault",engine.fault);return data;}
        try{
            engine.span=engine.chooseSpan(root);Frame f=engine.evaluate(root);
            data.put("source",f.source.save());data.putIntArray("original",SignalChain.pack(f.original));data.putIntArray("sampled",SignalChain.pack(f.sampled));data.putIntArray("sourceOutput",SignalChain.pack(f.sourceOutput));
            data.putIntArray("stageInput",SignalChain.pack(f.inputA==null?f.samples:f.inputA));data.putIntArray("inputB",SignalChain.pack(f.inputB==null?new double[0]:f.inputB));
            // RX overlays its input with its received output; processor overlays pre/post processing.
            data.putIntArray("emitted",SignalChain.pack(device.kind().equals("radio")&&!local.transmit()?f.inputA:f.samples));data.putIntArray("received",SignalChain.pack(f.samples));
            data.putDouble("span",engine.span);data.putString("units",f.units);data.putDouble("metres",f.length);data.putDouble("loss",f.loss);data.putDouble("sigma",f.sigma);data.putDouble("cableVariance",f.damageVariance);data.putDouble("noiseMean",local.lab().noiseMean());
            if(root.a!=null)data.putString("medium",root.a.route().medium().name());
            List<Node> used=new ArrayList<>();engine.collect(root,Collections.newSetFromMap(new IdentityHashMap<Node,Boolean>()),used);
            data.putString("pipeline",String.join(" → ",used.stream().filter(n->!n.device.isSource()).map(n->n.device.kind()).toList().reversed()));data.putInt("activeNodes",used.size());
            Set<BlockPos> cables=new HashSet<>();ListTag sources=new ListTag();
            for(Node n:used){for(Input input:new Input[]{n.a,n.b})if(input!=null)for(BlockPos node:input.route().path())if(level.getBlockState(node).getBlock() instanceof CableBlock)cables.add(node);
                if(n.a==null){var source=n.device.settings().save();source.putString("position",n.device.getBlockPos().toShortString());sources.add(source);}}
            data.put("sources",sources);data.putDouble("wireMetres",cables.stream().mapToDouble(c->CircuitNetwork.cable(level,c).metres()).sum());data.putInt("cables",cables.size());
            if(f.gray!=null){data.putString("grayInput",f.gray.input());data.putString("grayOutput",f.gray.output());}
            if(f.correlation!=null){data.putIntArray("correlation",SignalChain.pack(f.correlation.values()));data.putDouble("lagSpan",f.correlation.lagSpan());data.putDouble("peakLag",f.correlation.peakLag());data.putDouble("peakValue",f.correlation.peakValue());}
            if(!rootSource&&device.processor().demodulate()&&f.units.equals("u"))data.putString("decoded",device.processor().modulation().equals("urz")||device.processor().modulation().equals("unrz")?SignalChain.decodeUnipolar(f.samples,engine.span,device.processor().symbolHz(),local.lab().amplitudeOne(),local.duty(),device.processor().modulation().equals("urz")):SignalChain.decodeBpsk(f.samples,engine.span,device.processor().oscillatorHz(),device.processor().symbolHz()));
        }catch(IllegalArgumentException e){data.putBoolean("connected",false);data.putString("fault",e.getMessage());}
        return data;
    }
}
