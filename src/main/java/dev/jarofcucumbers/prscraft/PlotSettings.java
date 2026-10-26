package dev.jarofcucumbers.prscraft;
import net.minecraft.nbt.CompoundTag;
/** Display-only axis limits. Never changes samples, sampling clocks or signal properties. */
public record PlotSettings(boolean autoX,boolean autoY,double xMin,double xMax,double yMin,double yMax,int layers){
    public static final PlotSettings DEFAULT=new PlotSettings(true,true,0,1,-1,1,72);
    public PlotSettings{for(double x:new double[]{xMin,xMax,yMin,yMax})if(!Double.isFinite(x)||Math.abs(x)>1e12)throw new IllegalArgumentException("plot_range");if(xMin>=xMax||yMin>=yMax||xMax-xMin<1e-12||yMax-yMin<1e-12)throw new IllegalArgumentException("plot_range");layers&=255;}
    public CompoundTag save(){var t=new CompoundTag();t.putBoolean("autoX",autoX);t.putBoolean("autoY",autoY);t.putDouble("xMin",xMin);t.putDouble("xMax",xMax);t.putDouble("yMin",yMin);t.putDouble("yMax",yMax);t.putInt("layers",layers);return t;}
    public static PlotSettings read(CompoundTag t){return new PlotSettings(t.getBoolean("autoX"),t.getBoolean("autoY"),t.getDouble("xMin"),t.getDouble("xMax"),t.getDouble("yMin"),t.getDouble("yMax"),t.getInt("layers"));}
}
