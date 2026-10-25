package dev.jarofcucumbers.prscraft;

import net.minecraft.nbt.CompoundTag;

public record RadioSettings(String expression, double sampleHz, int levels, boolean transmit,
                            boolean enabled, boolean thermal, double noiseRms, double carrierMHz, double powerDbm, boolean secondKind, double duty, String signalMode, String bits, double bitRate) {
    public static final RadioSettings DEFAULT = new RadioSettings("sin(10)+0.5*cos(20)", 100, 16, true, true, true, 0.02, 100, 20, true, 0.5);
    public RadioSettings(String expression, double sampleHz, int levels, boolean transmit, boolean enabled,
        boolean thermal, double noiseRms, double carrierMHz, double powerDbm, boolean secondKind, double duty) {
        this(expression,sampleHz,levels,transmit,enabled,thermal,noiseRms,carrierMHz,powerDbm,secondKind,duty,"quantized","10110010",10);
    }
    public RadioSettings {
        if (!java.util.Set.of("analog","sampled","quantized","bits").contains(signalMode)) throw new IllegalArgumentException("signal_mode");
        if (bits == null || !bits.matches("[01]{1,128}") || !Double.isFinite(bitRate) || bitRate < 1 || bitRate > 1000) throw new IllegalArgumentException("bits");
        if (!Double.isFinite(sampleHz) || sampleHz < 1 || sampleHz > 20000) throw new IllegalArgumentException("sample_range");
        if (transmit && !signalMode.equals("bits")) {
            SignalProgram program = SignalProgram.parse(expression);
            if (!signalMode.equals("analog")) program.validateSampling(sampleHz);
        }
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
        tag.putString("signalMode",signalMode); tag.putString("bits",bits); tag.putDouble("bitRate",bitRate);
        return tag;
    }
    public static RadioSettings read(CompoundTag tag) {
        if (tag == null) throw new IllegalArgumentException("expression");
        return new RadioSettings(tag.getString("expression"), tag.getDouble("sampleHz"), tag.getInt("levels"),
            tag.getBoolean("transmit"), tag.getBoolean("enabled"), tag.getBoolean("thermal"), tag.getDouble("noiseRms"),
            tag.getDouble("carrierMHz"), tag.getDouble("powerDbm"), tag.getBoolean("secondKind"), tag.getDouble("duty"), tag.contains("signalMode")?tag.getString("signalMode"):"quantized",
            tag.contains("bits")?tag.getString("bits"):"10110010", tag.contains("bitRate")?tag.getDouble("bitRate"):10);
    }
}
