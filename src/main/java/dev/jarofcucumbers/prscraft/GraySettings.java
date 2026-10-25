package dev.jarofcucumbers.prscraft;
import net.minecraft.nbt.CompoundTag;
public record GraySettings(String mode,int wordBits,double bitRate,double threshold,double low,double high,int levels,double minimum,double maximum,double sampleHz){
    public static final GraySettings DEFAULT=new GraySettings("binary_to_gray",4,10,0,-1,1,16,-1,1,100);
    public GraySettings{
        if(!java.util.Set.of("binary_to_gray","gray_to_binary","levels_to_gray","levels_to_binary").contains(mode)||wordBits<1||wordBits>16)throw new IllegalArgumentException("gray_settings");
        for(double x:new double[]{bitRate,threshold,low,high,minimum,maximum,sampleHz})if(!Double.isFinite(x))throw new IllegalArgumentException("gray_settings");
        if(bitRate<1||bitRate>20000||sampleHz<1||sampleHz>20000||Math.abs(threshold)>100||low>=high||low < -100||high>100||minimum>=maximum||minimum < -100||maximum>100||levels<2||levels>4096||mode.startsWith("levels_")&&(1<<wordBits)<levels)throw new IllegalArgumentException("gray_settings");
    }
    public boolean levelInput(){return mode.startsWith("levels_");}
    public CompoundTag save(){var t=new CompoundTag();t.putString("mode",mode);t.putInt("wordBits",wordBits);t.putDouble("bitRate",bitRate);t.putDouble("threshold",threshold);t.putDouble("low",low);t.putDouble("high",high);t.putInt("levels",levels);t.putDouble("minimum",minimum);t.putDouble("maximum",maximum);t.putDouble("sampleHz",sampleHz);return t;}
    public static GraySettings read(CompoundTag t){return new GraySettings(t.getString("mode"),t.getInt("wordBits"),t.getDouble("bitRate"),t.getDouble("threshold"),t.getDouble("low"),t.getDouble("high"),t.getInt("levels"),t.getDouble("minimum"),t.getDouble("maximum"),t.getDouble("sampleHz"));}
}
