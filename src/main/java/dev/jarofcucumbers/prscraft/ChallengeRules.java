package dev.jarofcucumbers.prscraft;
import java.util.*;
import net.minecraft.nbt.CompoundTag;
/** Conditions use server captures, never client-submitted goals. */
public final class ChallengeRules{
    private ChallengeRules(){}
    public static Set<String> achieved(CompoundTag data){
        if(!data.getBoolean("connected"))return Set.of();var result=new HashSet<String>();
        var source=RadioSettings.read(data.getCompound("source"));var local=RadioSettings.read(data.getCompound("settings"));
        var decoder=ProcessorSettings.read(data.getCompound("processor"));String pipeline=data.getString("pipeline");String kind=data.getString("kind");
        if(kind.equals("radio")&&!local.transmit()){
            if(data.getInt("cables")>0&&data.getDouble("wireMetres")<1)result.add("microlink");
            String decoded=data.getString("decoded");boolean correct=source.signalMode().equals("bits")&&!source.lab().randomBits()&&source.lab().offsetSeconds()==0&&decoded.length()>=4&&decoded.indexOf('1')>=0&&decoded.indexOf('0')>=0;
            if(correct)for(int i=0;i<decoded.length();i++)if(decoded.charAt(i)!=source.bits().charAt(i%source.bits().length())){correct=false;break;}
            if(correct&&decoder.demodulate()&&Set.of("urz","unrz").contains(decoder.modulation())&&decoder.modulation().equals(source.lab().lineCode()))result.add("unipolar_link");
            if(correct&&decoder.demodulate()&&decoder.modulation().equals("bpsk")&&pipeline.contains("modulator")&&local.lab().noiseVariance()>=.0001)result.add("noise_recovery");
        }
        if(pipeline.contains("adder")&&pipeline.contains("integrator")&&data.getString("units").equals("u*s"))result.add("superposition");
        if(pipeline.contains("time_gate")&&pipeline.contains("integrator")&&energy(data.getIntArray("received"))>1e-10)result.add("timing_lab");
        if(kind.equals("correlator")&&pipeline.contains("delay_line")&&!data.getCompound("dsp").getBoolean("auto")&&data.getCompound("dsp").getBoolean("normalized")&&Math.abs(data.getDouble("peakLag"))>=.005&&data.getDouble("peakValue")>.9)result.add("delay_hunt");
        if(kind.equals("frequency_filter")){
            double in=energy(data.getIntArray("stageInput")),out=energy(data.getIntArray("received"));
            if(in>1e-8&&out>1e-8&&out<in*.25)result.add("filter_lab");
        }
        return Set.copyOf(result);
    }
    private static double energy(int[] packed){double sum=0;for(double x:SignalChain.unpack(packed))sum+=x*x;return sum;}
    public static boolean mural(byte[] pixels){int count=0;Set<Byte> colors=new HashSet<>();for(byte p:pixels)if(p>0&&p<=16){count++;colors.add(p);}return count>=256&&colors.size()>=3;}
}
