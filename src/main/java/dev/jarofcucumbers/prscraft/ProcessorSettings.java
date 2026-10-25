package dev.jarofcucumbers.prscraft;

import net.minecraft.nbt.CompoundTag;
import java.util.Set;

public record ProcessorSettings(double gainDb, double lowHz, double highHz, String modulation,
                                double oscillatorHz, double symbolHz, double depth, boolean demodulate) {
    public static final ProcessorSettings DEFAULT = new ProcessorSettings(6,0,1000,"bpsk",100,10,0.5,false);
    public ProcessorSettings {
        if(!Double.isFinite(gainDb)||gainDb < -40||gainDb > 60)throw new IllegalArgumentException("gain");
        if(!Double.isFinite(lowHz)||!Double.isFinite(highHz)||lowHz<0||highHz<=lowHz||highHz>20000)throw new IllegalArgumentException("filter");
        if(!Set.of("am","bpsk").contains(modulation))throw new IllegalArgumentException("modulation");
        if(!Double.isFinite(oscillatorHz)||oscillatorHz<1||oscillatorHz>10000||!Double.isFinite(symbolHz)||symbolHz<1||symbolHz>1000)throw new IllegalArgumentException("oscillator");
        if((modulation.equals("bpsk")||demodulate)&&(oscillatorHz<2*symbolHz||oscillatorHz>16*symbolHz))throw new IllegalArgumentException("bpsk_clock");
        if(!Double.isFinite(depth)||depth<0||depth>1)throw new IllegalArgumentException("depth");
    }
    public CompoundTag save() {
        CompoundTag t=new CompoundTag(); t.putDouble("gainDb",gainDb);t.putDouble("lowHz",lowHz);t.putDouble("highHz",highHz);
        t.putString("modulation",modulation);t.putDouble("oscillatorHz",oscillatorHz);t.putDouble("symbolHz",symbolHz);
        t.putDouble("depth",depth);t.putBoolean("demodulate",demodulate);return t;
    }
    public static ProcessorSettings read(CompoundTag t) {
        return new ProcessorSettings(t.getDouble("gainDb"),t.getDouble("lowHz"),t.getDouble("highHz"),t.getString("modulation"),
            t.getDouble("oscillatorHz"),t.getDouble("symbolHz"),t.getDouble("depth"),t.getBoolean("demodulate"));
    }
}
