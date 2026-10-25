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
        if(kind.equals("gray_converter")&&!data.getString("grayOutput").isEmpty())result.add(data.getCompound("gray").getString("mode").startsWith("levels_")?"gray_levels":"gray_binary");
        if(data.getInt("activeNodes")>=6)result.add("deep_chain");
        if(kind.equals("adder")&&data.getCompound("dsp").getDouble("b")<0&&energy(data.getIntArray("inputB"))>1e-8)result.add("subtractor");
        if(kind.equals("amplifier")){double in=energy(data.getIntArray("stageInput")),out=energy(data.getIntArray("received"));if(in>1e-8&&out>=4*in)result.add("signal_boost");}
        if(kind.equals("correlator")&&data.getCompound("dsp").getBoolean("auto")&&data.getCompound("dsp").getBoolean("normalized")&&data.getDouble("peakValue")>.99)result.add("self_similarity");
        if(kind.equals("radio")&&!local.transmit()){
            switch(source.signalMode()){
                case "analog" -> result.add("analog_link");
                case "sampled" -> result.add("sampled_link");
                case "quantized" -> {result.add("quantized_link");if(source.levels()>=256)result.add("fine_quantization");}
                default -> {}
            }
            if(Set.of("sampled","quantized").contains(source.signalMode()))result.add(source.secondKind()?"flat_sampling":"natural_sampling");
            if(source.signalMode().equals("bits")&&source.lab().randomBits()&&source.lab().probabilityOne()>=.75&&source.lab().probabilityOne()<1)result.add("biased_bits");
            if(Set.of("bits","discrete").contains(source.signalMode())&&!source.lab().repeat()&&!source.lab().randomBits())result.add("finite_burst");
            if(source.lab().offsetSeconds()>0&&energy(data.getIntArray("received"))>1e-10)result.add("offset_scan");
            if(source.lab().target()>0&&source.lab().quantity().equals("power"))result.add("power_source");
            if(source.lab().target()>0&&source.lab().quantity().equals("energy"))result.add("energy_source");
            if(!local.thermal()&&local.lab().noiseMean()==0&&local.lab().noiseVariance()==0&&energy(data.getIntArray("received"))>1e-10)result.add("quiet_receiver");
            if(data.getString("medium").equals("FIBER"))result.add("fiber_link");
            if(data.getString("medium").equals("METALLIC"))result.add("copper_link");
            if(data.getDouble("wireMetres")>=100)result.add("long_haul");
            if(java.util.Set.of("analog","sampled","quantized").contains(source.signalMode())&&SignalProgram.parse(source.expression()).terms().stream().filter(t->t.amplitude()!=0).count()>=4)result.add("four_tones");
            if(data.getInt("cables")>0&&data.getDouble("wireMetres")<1)result.add("microlink");
            String decoded=data.getString("decoded");boolean correct=source.signalMode().equals("bits")&&!source.lab().randomBits()&&source.lab().offsetSeconds()==0&&decoded.length()>=4&&decoded.indexOf('1')>=0&&decoded.indexOf('0')>=0;
            if(correct)for(int i=0;i<decoded.length();i++)if(decoded.charAt(i)!=source.bits().charAt(i%source.bits().length())){correct=false;break;}
            if(correct&&decoder.demodulate()&&Set.of("urz","unrz").contains(decoder.modulation())&&decoder.modulation().equals(source.lab().lineCode())){result.add("unipolar_link");result.add(decoder.modulation().equals("urz")?"urz_decoder":"unrz_decoder");}
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
    public static boolean fullPalette(byte[] pixels){int count=0;Set<Byte> colors=new HashSet<>();for(byte p:pixels)if(p>0&&p<=16){count++;colors.add(p);}return count>=8192&&colors.size()>=8;}
    public static boolean mural(byte[] pixels){int count=0;Set<Byte> colors=new HashSet<>();for(byte p:pixels)if(p>0&&p<=16){count++;colors.add(p);}return count>=256&&colors.size()>=3;}
}
