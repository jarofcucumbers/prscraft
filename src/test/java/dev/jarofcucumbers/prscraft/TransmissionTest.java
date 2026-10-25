package dev.jarofcucumbers.prscraft;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TransmissionTest {
    @Test void noiselessChannelPreservesQuantizedSignalWithVoltageLoss() {
        var config=RadioSettings.DEFAULT;var p=SignalProgram.parse(config.expression());
        double t=0.032,gain=Transmission.gain(20,20);
        assertEquals(0.1,gain,1e-12);
        assertEquals(p.digital(t,config.sampleHz(),config.levels(),config.secondKind(),config.duty())*0.1,
            Transmission.received(p,config,t,gain,0,123),1e-12);
    }
    @Test void additiveNoiseChangesReceptionReproducibly() {
        var config=RadioSettings.DEFAULT;var p=SignalProgram.parse(config.expression());
        double clean=Transmission.received(p,config,0.032,1,0,123);
        double noisy=Transmission.received(p,config,0.032,1,0.2,123);
        assertNotEquals(clean,noisy);
        assertEquals(noisy,Transmission.received(p,config,0.032,1,0.2,123));
    }
}
