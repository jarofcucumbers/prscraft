package dev.jarofcucumbers.prscraft;
import java.util.*;
/** Handbook pages are part of the feature contract; update with every user-facing change. */
public final class HandbookContent {
    private HandbookContent(){}
    public static final String VERSION="0.4.0";
    public record Page(String id,String diagram,List<String> formulas){}
    public static final List<Page> PAGES=List.of(
        new Page("workflow","chain",List.of("TX → port → cable → port → RX","1 block = 1 m","A_RX = A_TX · 10^((Ptx−20−loss)/20)")),
        new Page("functions","sine",List.of("x(t) = Σ Ai sin(2πfi t) + Σ Bj cos(2πfj t)","sin(10) = sin(2π·10·t)")),
        new Page("sampling","sampling",List.of("fvz ≥ 2 fmax","Ts = 1/fvz · τ/T = duty","II: x[n] = x(nTs)")),
        new Page("quantization","quant",List.of("Δ = 2 Amax / (L−1)","q(x) = −Amax + round((x+Amax)/Δ)·Δ")),
        new Page("digital","bits",List.of("P(1)=p · P(0)=1−p","x[n] ∈ {A0,A1}","NRZ: hold one level for Tb=1/Rb")),
        new Page("processors","chain",List.of("gain = 10^(GdB/20)","amplifier clips at ±16 u","FFT bin: f[k] = k/T")),
        new Page("modulation","bpsk",List.of("AM: (1+m·x/peak) cos(2πfc t)","BPSK: b(t) cos(2πfc t), b∈{−A,+A}","2 ≤ fc/Rb ≤ 16")),
        new Page("gaussian","gaussian",List.of("N = μ + σ Z · Z ~ N(0,1)","variance = σ² · RMS = sqrt(μ²+σ²)","Z = (N−μ)/σ (σ>0)")),
        new Page("awgn","awgn",List.of("Y = Ai + N · P0+P1=1","η=(m0+m1)/2 + σ²/(m1−m0) ln(P0/P1)","Pe=P0 Q((η−m0)/σ)+P1 Q((m1−η)/σ)")),
        new Page("q","gaussian",List.of("Q(x)=∫[x,∞] exp(−t²/2)/sqrt(2π) dt","x>3: Q≈φ(x)/(x+1/(x+2/(x+...)))","equal antipodal BPSK: Pe=Q(sqrt(2Eb/N0))")),
        new Page("spectrum","spectrum",List.of("X[k]=Σ x[n] exp(−j2πkn/N)","Δf=1/T · fs=N/T","PSD[k]=c·0.1·dt²|X[k]|²/T [W/Hz]")),
        new Page("energy","spectrum",List.of("P=0.1·mean(x²) [W]","E=P·T [J] · ESD=T·PSD [J/Hz]","Σ PSD·Δf=P · Σ ESD·Δf=E")),
        new Page("presets","presets",List.of("paper + applied settings → reusable template","type / TX-RX / numeric values / icon")),
        new Page("recordings","spectrum",List.of("512 samples per capture","CSV: time, original, sampled, TX, channel, RX")),
        new Page("analyzer","chain",List.of("read only · local segment ≤256 nodes","select device → waveform / spectrum / CSV")),
        new Page("limits","chain",List.of("≤8 processors · ≤256 nodes per medium","finite-window simulation, not a real-time RF solver"))
    );
    public static final Set<String> COVERED_ITEMS=Set.of("transmitter","coaxial_port","fiber_port","metallic_port","coaxial_cable","fiber_cable","metallic_cable","amplifier","frequency_filter","modulator","bit_generator","line_analyzer","signal_template","data_card","handbook");
}
