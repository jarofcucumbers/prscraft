package dev.jarofcucumbers.prscraft;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import java.util.*;

public final class RadioCapture {
    private RadioCapture() {}
    public static CompoundTag capture(Level level,BlockPos pos) {
        RadioBlockEntity radio=(RadioBlockEntity)level.getBlockEntity(pos);
        RadioSettings local=radio.settings();
        CompoundTag data=new CompoundTag();
        data.put("settings",local.save());data.put("processor",radio.processor().save());data.putString("kind",radio.kind());
        data.putInt("graphLayers",radio.graphLayers());data.putInt("graphTerms",radio.graphTerms());data.putLong("time",level.getGameTime());
        boolean processor=!radio.kind().equals("radio");
        RadioSettings receiver=processor?new RadioSettings(local.expression(),local.sampleHz(),local.levels(),false,local.enabled(),local.thermal(),local.noiseRms(),local.carrierMHz(),local.powerDbm(),local.secondKind(),local.duty()):local;
        CableNetwork.Link link=processor?CableNetwork.monitor(level,pos,receiver):CableNetwork.receive(level,pos,receiver);
        boolean receiving=processor||!local.transmit();
        boolean connected=link!=null;data.putBoolean("connected",connected);
        if(receiving&&!connected)return data;
        RadioSettings source=connected?link.source():local;
        List<SignalChain.Stage> stages=new ArrayList<>(connected?link.processors():List.of());
        List<SignalChain.Stage> before=List.copyOf(stages);
        if(processor)stages.add(new SignalChain.Stage(radio.kind(),radio.processor()));
        double span=SignalChain.span(source,stages);
        double[] original=SignalChain.generate(source,span,512,true), sampled=original.clone();
        if(source.signalMode().equals("sampled")||source.signalMode().equals("quantized")){
            var program=SignalProgram.parse(source.expression());
            for(int i=0;i<sampled.length;i++)sampled[i]=program.sampled(span*i/sampled.length,source.sampleHz(),source.secondKind(),source.duty());
        }
        double[] sourceOutput=source.enabled()?SignalChain.generate(source,span,512,false):new double[512];
        double[] emitted=SignalChain.process(sourceOutput,span,stages);
        if(processor)data.putIntArray("stageInput",SignalChain.pack(SignalChain.process(sourceOutput,span,before)));
        double[] received=emitted.clone();
        double loss=connected?link.lossDb():0;
        double sigma=connected?Transmission.rms(local,source,CableNetwork.interferenceRms(level,pos,link,local)):0;
        long seed=(connected?link.sourcePos().asLong():pos.asLong())^level.getGameTime();
        double gain=Transmission.gain(source.powerDbm(),loss);
        for(int i=0;i<received.length;i++)received[i]=emitted[i]*gain+sigma*SignalProgram.noise(seed,i);
        data.put("source",source.save());data.putIntArray("original",SignalChain.pack(original));data.putIntArray("sampled",SignalChain.pack(sampled));
        data.putIntArray("sourceOutput",SignalChain.pack(sourceOutput));data.putIntArray("emitted",SignalChain.pack(emitted));
        data.putIntArray("received",SignalChain.pack(received));data.putDouble("span",span);data.putDouble("sigma",sigma);data.putDouble("loss",loss);
        data.putString("pipeline",String.join(" → ",stages.stream().map(SignalChain.Stage::kind).toList()));
        if(connected){data.putInt("metres",link.metres());data.putString("medium",link.medium().name());}
        if(receiving&&radio.processor().demodulate()){
            var decoder=radio.processor();data.putString("decoded",SignalChain.decodeBpsk(received,span,decoder.oscillatorHz(),decoder.symbolHz()));
        }
        return data;
    }
}
