package dev.jarofcucumbers.prscraft;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SignalProgramTest {
    @Test void sumsSignedSinesAndCosinesInHertz() {
        var program=SignalProgram.parse("sin(10) + 0.5*cos(20) - 2*sin(5)");
        assertEquals(program,SignalProgram.parse("sin(2*pi*10*t)+0.5*cos(2*pi*20*t)-2*sin(2*pi*5*t)"));
        assertEquals(3,program.terms().size());
        assertEquals(0.5,program.at(0),1e-12);
        assertEquals(20,program.maxHz());
        assertEquals(Math.sin(2*Math.PI*10*0.03)+0.5*Math.cos(2*Math.PI*20*0.03)-2*Math.sin(2*Math.PI*5*0.03),program.at(0.03),1e-12);
    }
    @Test void rejectsUnboundedOrMalformedExpressions() {
        for(String s:new String[]{"", "sin(10)cos(20)","tan(10)","sin(-1)","sin(1001)","11*sin(10)","sin(10)+garbage","sin(10)/cos(20)"})
            assertThrows(IllegalArgumentException.class,()->SignalProgram.parse(s),s);
        assertThrows(IllegalArgumentException.class,()->SignalProgram.parse("sin(1)+".repeat(8)+"sin(1)"));
    }
    @Test void nyquistIncludesBoundaryAndIgnoresZeroAmplitude() {
        var p=SignalProgram.parse("sin(20)+0*cos(1000)");
        assertThrows(IllegalArgumentException.class,()->p.validateSampling(39.9));
        assertDoesNotThrow(()->p.validateSampling(40));
        assertThrows(IllegalArgumentException.class,()->p.validateSampling(Double.NaN));
    }
    @Test void distinguishesNaturalAndFlatTopSamplingAndPulseGaps() {
        var p=SignalProgram.parse("sin(10)");
        assertEquals(p.at(0.012),p.sampled(0.012,100,false,0.5),1e-12);
        assertEquals(p.at(0.01),p.sampled(0.012,100,true,0.5),1e-12);
        assertNotEquals(p.sampled(0.012,100,false,0.5),p.sampled(0.012,100,true,0.5));
        assertEquals(0,p.sampled(0.018,100,false,0.5));
        assertEquals(0,p.digital(0.018,100,4,true,0.5));
    }
    @Test void quantizationUsesExactlyTheSelectedLevelsAndSaturates() {
        var p=SignalProgram.parse("sin(10)");
        assertEquals(-1,p.quantize(-10,4),1e-12);
        assertEquals(1,p.quantize(10,4),1e-12);
        assertEquals(1.0/3,p.quantize(0.2,4),1e-12);
        assertEquals(-1.0/3,p.quantize(-0.2,4),1e-12);
        double step=2.0/15;
        for(int i=0;i<16;i++)assertEquals(-1+i*step,p.quantize(-1+i*step,16),1e-12);
        assertThrows(IllegalArgumentException.class,()->p.quantize(0,1));
    }
    @Test void noiseIsReproducibleAndApproximatelyUnitGaussian() {
        assertEquals(SignalProgram.noise(12,99),SignalProgram.noise(12,99));
        double sum=0,squares=0;int count=10000;
        for(int i=0;i<count;i++){double n=SignalProgram.noise(42,i);sum+=n;squares+=n*n;}
        assertEquals(0,sum/count,0.04);assertEquals(1,squares/count,0.06);
        assertTrue(SignalProgram.thermalRms(100)>SignalProgram.thermalRms(10));
    }
}
