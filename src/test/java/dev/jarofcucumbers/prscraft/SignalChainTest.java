package dev.jarofcucumbers.prscraft;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class SignalChainTest {
    private RadioSettings source(String mode,String bits,double rate){return new RadioSettings("sin(10)",100,16,true,true,false,0,100,20,true,.5,mode,bits,rate);}
    @Test void analogDoesNotAcquireSamplingOrQuantizationError(){
        var s=source("analog","10",10);assertEquals(Math.sin(2*Math.PI*10*.0123),SignalChain.output(s,.0123),1e-12);
        assertNotEquals(SignalChain.output(s,.0123),SignalChain.output(source("quantized","10",10),.0123));
    }
    @Test void binarySourceUsesNrzAndRepeatsThePattern(){
        var s=source("bits","1010",10);assertEquals(1,SignalChain.output(s,.05));assertEquals(-1,SignalChain.output(s,.15));assertEquals(1,SignalChain.output(s,.45));
    }
    @Test void amplifierHasCorrectGainAndClipping(){
        var p=new ProcessorSettings(20,0,1000,"bpsk",100,10,.5,false);
        double[] output=SignalChain.process(new double[]{.1,-.2,4},1,List.of(new SignalChain.Stage("amplifier",p)));
        assertArrayEquals(new double[]{1,-2,16},output,1e-9);
    }
    @Test void fftFilterPassesLowToneAndRejectsHighTone(){
        int n=512;double[] input=new double[n],expected=new double[n];
        for(int i=0;i<n;i++){double t=(double)i/n;expected[i]=Math.sin(2*Math.PI*10*t);input[i]=expected[i]+.5*Math.cos(2*Math.PI*80*t);}
        assertArrayEquals(expected,SignalChain.bandPass(input,1,5,20),1e-9);
        assertArrayEquals(input,SignalChain.bandPass(input,1,0,256),1e-9);
    }
    @Test void bpskChangesPhaseAndCoherentDecoderRecoversBits(){
        var s=source("bits","10110010",10);double span=.8;
        var p=new ProcessorSettings(0,0,1000,"bpsk",100,10,.5,false);
        double[] original=SignalChain.generate(s,span,512,false);
        double[] modulated=SignalChain.process(original,span,List.of(new SignalChain.Stage("modulator",p)));
        assertEquals(1,modulated[0],1e-12);
        assertEquals("10110010",SignalChain.decodeBpsk(modulated,span,100,10));
        double[] noisy=modulated.clone();for(int i=0;i<noisy.length;i++)noisy[i]=.2*noisy[i]+.02*SignalProgram.noise(123,i);
        assertEquals("10110010",SignalChain.decodeBpsk(noisy,span,100,10));
    }
    @Test void amDepthZeroGivesUnmodulatedCarrier(){
        var p=new ProcessorSettings(0,0,1000,"am",100,10,0,false);double[] input=new double[512];Arrays.fill(input,1);
        var output=SignalChain.process(input,1,List.of(new SignalChain.Stage("modulator",p)));
        for(int i=0;i<512;i++)assertEquals(Math.cos(2*Math.PI*100*i/512),output[i],1e-10);
    }
    @Test void processorOrderIsRespected(){
        var amp=new ProcessorSettings(20,0,1000,"am",100,10,0,false);
        var a=new SignalChain.Stage("amplifier",amp);var m=new SignalChain.Stage("modulator",amp);
        double[] input=new double[512];Arrays.fill(input,1);
        var first=SignalChain.process(input,1,List.of(a,m));var second=SignalChain.process(input,1,List.of(m,a));
        assertEquals(1,first[0],1e-9);assertEquals(10,second[0],1e-9);
    }
}
