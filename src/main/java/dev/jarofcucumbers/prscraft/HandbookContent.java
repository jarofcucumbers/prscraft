package dev.jarofcucumbers.prscraft;
import java.util.*;
/** Handbook pages are part of the feature contract; update with every user-facing change. */
public final class HandbookContent {
    private HandbookContent(){}
    public static final String VERSION="0.7.2.1";
    public record Page(String id,String diagram,List<String> formulas){}
    private static final List<Page> ORIGINAL_PAGES=List.of(
        new Page("workflow","chain",List.of("TX → port → cable → port → RX","cables: 1 m by default; wrench adjusts length","A_RX = A_TX · 10^((Ptx−20−loss)/20)")),
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
        new Page("analyzer","chain",List.of("read only · physical topology ≤1024 nodes","select device → waveform / spectrum / CSV")),
        new Page("circuit","circuit",List.of("physical topology ≠ directed signal flow","A: back · B: left · OUT: front")),
        new Page("math_blocks","math",List.of("sum: y(t)=gA·A(t)+gB·B(t)","integrator: y(t)=∫[0,min(t,T)] x(s) ds","u → u*s; unlike units cannot be added")),
        new Page("correlation","correlation",List.of("R_AB(τ)=∫ A(t) B(t+τ) dt","ρ_AB=ΣAB/sqrt(ΣA²·ΣB²)","τpeak>0: B is delayed relative to A")),
        new Page("timing","gate",List.of("gate: pass for t≥T, or T≤t<T+width","delay: y(t)=x(t−D), zero outside the window")),
        new Page("cables","cable",List.of("L: 0.01…1000 m per cable block","damage loss=3·d·L dB","damage noise variance=0.0001·d²·L u²")),
        new Page("advancements","achievements",List.of("link · calibration · correlation · whole-circuit survey")),
        new Page("line_coding","urz",List.of("UNRZ: 1 → +A; 0 → 0 for one Tb","URZ: 1 → +A for duty·Tb, then 0","Tb = 1 / Rb; decision threshold = A_RX / 2")),
        new Page("drawing","drawing",List.of("128 × 128 = 16384 pixels per board","chalk: 4096 painted pixels; eraser: 16384 touched pixels","16 × 16 brush = 256 durability per dab")),
        new Page("challenges","achievements",List.of("valid circuit + measured result → challenge","changing settings without a working link is insufficient")),
        new Page("challenges_rx","chain",List.of("cables < 1 m; decoded bits ≥ 4","filter output energy < 0.25 × input energy")),
        new Page("challenges_math","correlation",List.of("TX + TX → SUM → INTEGRATOR","|τpeak| ≥ 0.005 s; normalized peak > 0.9")),
        new Page("challenges_more","equipment",List.of("amplifier energy ratio ≥ 4; autocorrelation peak > 0.99","≥6 active devices; ≥100 m cable; ≥4 tones")),
        new Page("learning_tx","sampling",List.of("sampled → quantized → L ≥ 256","Bernoulli: 0.75 ≤ P1 < 1")),
        new Page("learning_rx","urz",List.of("UNRZ / URZ: match Rb, duty and received A","W: average power; J: finite sequence energy")),
        new Page("gray","gray",List.of("Gray: g = b XOR (b >> 1)","inverse: b[i] = b[i−1] XOR g[i]; b[0]=g[0]","level index: round((x−min)·(L−1)/(max−min))")),
        new Page("limits","chain",List.of("≤16 depth · ≤32 active nodes · ≤512 nodes per segment","finite-window simulation, not a real-time RF solver"))
    );
    public static final List<Page> PAGES=java.util.stream.Stream.concat(ORIGINAL_PAGES.stream(),HandbookCurriculum.extraPages().stream()).toList();
    private static final Set<String> EQUIPMENT=Set.of("transmitter","coaxial_port","fiber_port","metallic_port","coaxial_cable","fiber_cable","metallic_cable","amplifier","frequency_filter","modulator","bit_generator","line_analyzer","signal_template","data_card","handbook","adder","integrator","correlator","time_gate","delay_line","cable_wrench","gray_converter","drawing_board","board_eraser");
    public static final Set<String> COVERED_ITEMS;
    static {var items=new HashSet<>(EQUIPMENT);for(String color:BoardPixels.COLORS)items.add(color+"_chalk");COVERED_ITEMS=Set.copyOf(items);}
}
