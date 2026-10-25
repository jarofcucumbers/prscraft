package dev.jarofcucumbers.prscraft;

import net.minecraft.nbt.CompoundTag;

public record RadioSettings(String expression, double sampleHz, int levels, boolean transmit,
                            boolean enabled, boolean thermal, double noiseRms, double carrierMHz, double powerDbm, boolean secondKind, double duty) {
    public static final RadioSettings DEFAULT = new RadioSettings("sin(10)+0.5*cos(20)", 100, 16, true, true, true, 0.02, 100, 20, true, 0.5);
    public RadioSettings {
        SignalProgram program = SignalProgram.parse(expression);
        program.validateSampling(sampleHz);
        if (!Double.isFinite(duty) || duty < 0.05 || duty > 1) throw new IllegalArgumentException("duty");
        if (levels < 2 || levels > 4096) throw new IllegalArgumentException("levels");
        if (!Double.isFinite(noiseRms) || noiseRms < 0 || noiseRms > 2) throw new IllegalArgumentException("noise_range");
        if (!Double.isFinite(carrierMHz) || carrierMHz < 1 || carrierMHz > 10000) throw new IllegalArgumentException("carrier_range");
        if (!Double.isFinite(powerDbm) || powerDbm < -30 || powerDbm > 40) throw new IllegalArgumentException("power_range");
    }
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("expression", expression); tag.putDouble("sampleHz", sampleHz); tag.putInt("levels", levels);
        tag.putBoolean("transmit", transmit); tag.putBoolean("enabled", enabled); tag.putBoolean("thermal", thermal);
        tag.putDouble("noiseRms", noiseRms); tag.putDouble("carrierMHz", carrierMHz); tag.putDouble("powerDbm", powerDbm);
        tag.putBoolean("secondKind", secondKind); tag.putDouble("duty", duty);
        return tag;
    }
    public static RadioSettings read(CompoundTag tag) {
        if (tag == null) throw new IllegalArgumentException("expression");
        return new RadioSettings(tag.getString("expression"), tag.getDouble("sampleHz"), tag.getInt("levels"),
            tag.getBoolean("transmit"), tag.getBoolean("enabled"), tag.getBoolean("thermal"), tag.getDouble("noiseRms"),
            tag.getDouble("carrierMHz"), tag.getDouble("powerDbm"), tag.getBoolean("secondKind"), tag.getDouble("duty"));
    }
}
