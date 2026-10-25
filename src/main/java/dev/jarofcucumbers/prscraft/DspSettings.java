package dev.jarofcucumbers.prscraft;
import net.minecraft.nbt.CompoundTag;
public record DspSettings(double endSeconds,double gateSeconds,double gateWidth,boolean gateHold,double delaySeconds,
    double lagSeconds,boolean normalized,boolean autoCorrelation,double gainA,double gainB){
    public static final DspSettings DEFAULT=new DspSettings(8,.1,.01,true,.01,.1,true,false,1,1);
    public DspSettings{
        for(double time:new double[]{endSeconds,gateSeconds,delaySeconds,lagSeconds})if(!Double.isFinite(time)||time<0||time>8)throw new IllegalArgumentException("dsp_time");
        if(!Double.isFinite(gateWidth)||gateWidth<=0||gateWidth>8)throw new IllegalArgumentException("gate_width");
        if(!Double.isFinite(gainA)||!Double.isFinite(gainB)||Math.abs(gainA)>10||Math.abs(gainB)>10)throw new IllegalArgumentException("input_gain");
    }
    public CompoundTag save(){var t=new CompoundTag();t.putDouble("end",endSeconds);t.putDouble("gate",gateSeconds);t.putDouble("width",gateWidth);t.putBoolean("hold",gateHold);t.putDouble("delay",delaySeconds);t.putDouble("lag",lagSeconds);t.putBoolean("normalized",normalized);t.putBoolean("auto",autoCorrelation);t.putDouble("a",gainA);t.putDouble("b",gainB);return t;}
    public static DspSettings read(CompoundTag t){return new DspSettings(t.getDouble("end"),t.getDouble("gate"),t.getDouble("width"),t.getBoolean("hold"),t.getDouble("delay"),t.getDouble("lag"),t.getBoolean("normalized"),t.getBoolean("auto"),t.getDouble("a"),t.getDouble("b"));}
}
