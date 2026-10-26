package dev.jarofcucumbers.prscraft;
import net.minecraft.nbt.CompoundTag;
public record OrthogonalitySettings(double start,double end,double tolerance,boolean discrete,double sampleHz){
    public static final OrthogonalitySettings DEFAULT=new OrthogonalitySettings(0,1,.01,false,100);
    public OrthogonalitySettings{for(double v:new double[]{start,end,tolerance,sampleHz})if(!Double.isFinite(v))throw new IllegalArgumentException("orthogonality");if(start<0||end<=start||end>8||tolerance<0||tolerance>1||sampleHz<1||sampleHz>20000)throw new IllegalArgumentException("orthogonality");}
    public CompoundTag save(){var t=new CompoundTag();t.putDouble("start",start);t.putDouble("end",end);t.putDouble("tolerance",tolerance);t.putBoolean("discrete",discrete);t.putDouble("sampleHz",sampleHz);return t;}
    public static OrthogonalitySettings read(CompoundTag t){return new OrthogonalitySettings(t.getDouble("start"),t.getDouble("end"),t.getDouble("tolerance"),t.getBoolean("discrete"),t.getDouble("sampleHz"));}
}
