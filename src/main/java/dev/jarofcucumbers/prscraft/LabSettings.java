package dev.jarofcucumbers.prscraft;
import net.minecraft.nbt.CompoundTag;
import java.util.*;
/** Advanced source parameters and additive Gaussian noise in amplitude units. */
public record LabSettings(boolean repeat,boolean randomBits,double probabilityOne,double amplitudeZero,double amplitudeOne,
    String samples,long seed,double offsetSeconds,double noiseMean,double noiseVariance,boolean normalized,String quantity,double target) {
    public static LabSettings defaults(double rms){return new LabSettings(true,false,.5,-1,1,"0,1,0,-1",42,0,0,rms*rms,false,"relative",.1);}
    public LabSettings {
        if(!Double.isFinite(probabilityOne)||probabilityOne<0||probabilityOne>1)throw new IllegalArgumentException("probability");
        if(!Double.isFinite(amplitudeZero)||!Double.isFinite(amplitudeOne)||amplitudeZero < -100||amplitudeOne>100||amplitudeZero>=amplitudeOne)throw new IllegalArgumentException("symbol_amplitudes");
        parseSamples(samples);
        if(!Double.isFinite(offsetSeconds)||offsetSeconds<0||offsetSeconds>120)throw new IllegalArgumentException("offset");
        if(!Double.isFinite(noiseMean)||Math.abs(noiseMean)>100||!Double.isFinite(noiseVariance)||noiseVariance<0||noiseVariance>100)throw new IllegalArgumentException("variance");
        if(!Set.of("relative","power","energy").contains(quantity)||!Double.isFinite(target)||target<0||target>1000)throw new IllegalArgumentException("quantity");
    }
    public static double[] parseSamples(String text){
        if(text==null||text.length()>2048||text.isBlank())throw new IllegalArgumentException("sample_list");
        String[] parts=text.trim().split("[,;\\s]+");if(parts.length>128)throw new IllegalArgumentException("sample_list");
        double[] values=new double[parts.length];
        try{for(int i=0;i<parts.length;i++){values[i]=Double.parseDouble(parts[i]);if(!Double.isFinite(values[i])||Math.abs(values[i])>100)throw new IllegalArgumentException("sample_list");}}
        catch(NumberFormatException e){throw new IllegalArgumentException("sample_list");}return values;
    }
    public LabSettings variance(double value){return new LabSettings(repeat,randomBits,probabilityOne,amplitudeZero,amplitudeOne,samples,seed,offsetSeconds,noiseMean,value,normalized,quantity,target);}
    public CompoundTag save(){var t=new CompoundTag();t.putBoolean("repeat",repeat);t.putBoolean("random",randomBits);t.putDouble("p1",probabilityOne);t.putDouble("a0",amplitudeZero);t.putDouble("a1",amplitudeOne);t.putString("samples",samples);t.putLong("seed",seed);t.putDouble("offset",offsetSeconds);t.putDouble("mean",noiseMean);t.putDouble("variance",noiseVariance);t.putBoolean("normalized",normalized);t.putString("quantity",quantity);t.putDouble("target",target);return t;}
    public static LabSettings read(CompoundTag t){return new LabSettings(t.getBoolean("repeat"),t.getBoolean("random"),t.getDouble("p1"),t.getDouble("a0"),t.getDouble("a1"),t.getString("samples"),t.getLong("seed"),t.getDouble("offset"),t.getDouble("mean"),t.getDouble("variance"),t.getBoolean("normalized"),t.getString("quantity"),t.getDouble("target"));}
}
