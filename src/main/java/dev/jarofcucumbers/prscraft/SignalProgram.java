package dev.jarofcucumbers.prscraft;

import java.util.*;
import java.util.regex.*;

/** A bounded sum of sinusoids. Frequencies in the expression are in Hz, not rad/s. */
public record SignalProgram(List<Term> terms) {
    private static final Pattern TERM = Pattern.compile("([+-]?)(?:(\\d+(?:\\.\\d+)?)\\*)?(sin|cos)\\((\\d+(?:\\.\\d+)?)\\)");
    public record Term(boolean cosine, double amplitude, double hz) {
        public double at(double seconds) { return amplitude * (cosine ? Math.cos(2 * Math.PI * hz * seconds) : Math.sin(2 * Math.PI * hz * seconds)); }
    }
    public SignalProgram { terms = List.copyOf(terms); }
    public static SignalProgram parse(String expression) {
        if (expression == null) throw new IllegalArgumentException("expression");
        if (expression.length() > 256) throw new IllegalArgumentException("expression_length");
        String input = expression.replace(" ", "")
            .replaceAll("(sin|cos)\\(2\\*(?:pi|π)\\*(\\d+(?:\\.\\d+)?)\\*t\\)", "$1($2)");
        Matcher matcher = TERM.matcher(input);
        List<Term> terms = new ArrayList<>();
        int end = 0;
        while (matcher.find()) {
            if (matcher.start() != end || (end > 0 && matcher.group(1).isEmpty())) throw new IllegalArgumentException("expression");
            double amp = matcher.group(2) == null ? 1 : Double.parseDouble(matcher.group(2));
            double hz = Double.parseDouble(matcher.group(4));
            if (amp > 10 || hz <= 0 || hz > 1000 || !Double.isFinite(hz)) throw new IllegalArgumentException("term_range");
            terms.add(new Term(matcher.group(3).equals("cos"), matcher.group(1).equals("-") ? -amp : amp, hz));
            if (terms.size() > 8) throw new IllegalArgumentException("too_many_terms");
            end = matcher.end();
        }
        if (end != input.length() || terms.isEmpty()) throw new IllegalArgumentException("expression");
        return new SignalProgram(terms);
    }
    public double at(double seconds) { return terms.stream().mapToDouble(t -> t.at(seconds)).sum(); }
    public double maxHz() { return terms.stream().filter(t -> t.amplitude() != 0).mapToDouble(Term::hz).max().orElse(0); }
    public double range() { return Math.max(0.01, terms.stream().mapToDouble(t -> Math.abs(t.amplitude())).sum()); }
    public void validateSampling(double sampleHz) {
        if (!Double.isFinite(sampleHz) || sampleHz < 1 || sampleHz > 20000) throw new IllegalArgumentException("sample_range");
        if (sampleHz < 2 * maxHz()) throw new IllegalArgumentException("nyquist");
    }
    public double quantize(double value, int levels) {
        if (levels < 2 || levels > 4096) throw new IllegalArgumentException("levels");
        double range = range();
        double clamped = Math.max(-range, Math.min(range, value));
        return -range + Math.round((clamped + range) * (levels - 1) / (2 * range)) * 2 * range / (levels - 1);
    }
    public double sampled(double seconds, double sampleHz, boolean secondKind, double duty) {
        validateSampling(sampleHz);
        double phase = seconds * sampleHz - Math.floor(seconds * sampleHz);
        if (phase >= duty) return 0;
        return at(secondKind ? Math.floor(seconds * sampleHz) / sampleHz : seconds);
    }
    public double digital(double seconds, double sampleHz, int levels, boolean secondKind, double duty) {
        validateSampling(sampleHz);
        double phase = seconds * sampleHz - Math.floor(seconds * sampleHz);
        return phase >= duty ? 0 : quantize(sampled(seconds, sampleHz, secondKind, duty), levels);
    }
    /** Deterministic Gaussian white noise: stable when the same sample is drawn again. */
    public static double noise(long seed, long sample) {
        return new Random(seed ^ (sample * 0x9E3779B97F4A7C15L)).nextGaussian();
    }
    public static double thermalRms(double bandwidthHz) {
        // 1 signal unit references 100 mW (20 dBm). Johnson-Nyquist noise at 290 K.
        return Math.sqrt(1.380649e-23 * 290 * bandwidthHz / 0.1);
    }
}
