package dev.jarofcucumbers.prscraft;

/** Simplified engineering model: one Minecraft block represents one metre. */
public final class SignalBudget {
    private SignalBudget() {}
    public enum Medium {
        COAXIAL(0.20, 1.0, 0.05), FIBER(0.003, 0.5, 0.0), METALLIC(0.50, 2.0, 0.35);
        private final double loss, connectorLoss, coupling;
        Medium(double loss, double connectorLoss, double coupling) {
            this.loss = loss; this.connectorLoss = connectorLoss; this.coupling = coupling;
        }
        public double lossDb(double metres, double frequencyMHz) {
            if (!Double.isFinite(metres) || metres < 0 || !Double.isFinite(frequencyMHz) || frequencyMHz <= 0)
                throw new IllegalArgumentException("Invalid distance or frequency");
            return connectorLoss + loss * metres * (this == FIBER ? 1 : Math.sqrt(frequencyMHz / 100.0));
        }
        public double coupling() { return coupling; }
    }
    public static double receivedDbm(double transmitDbm, Medium medium, double metres, double frequencyMHz) {
        return transmitDbm - medium.lossDb(metres, frequencyMHz);
    }
    /** Spectral overlap of equal-width channels, from zero to one. */
    public static double overlap(double aMHz, double bMHz, double bandwidthMHz) {
        if (bandwidthMHz <= 0) throw new IllegalArgumentException("Bandwidth must be positive");
        return Math.max(0, 1 - Math.abs(aMHz - bMHz) / bandwidthMHz);
    }
}
