package dev.jarofcucumbers.prscraft;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class SpectrumTest {
 @Test void parsevalGivesPowerEnergyAndCorrectPeak(){
  double[] x=new double[512];for(int i=0;i<x.length;i++)x[i]=2*Math.sin(2*Math.PI*10*i/512)+.5;
  var s=Spectrum.analyze(x,1);assertEquals(.225,s.powerW(),1e-12);assertEquals(.225,s.energyJ(),1e-12);
  assertEquals(s.powerW(),java.util.Arrays.stream(s.psd()).sum()*s.binHz(),1e-12);
  assertEquals(s.energyJ(),java.util.Arrays.stream(s.esd()).sum()*s.binHz(),1e-12);
  assertEquals(2*Math.sqrt(.1),s.amplitude()[10],1e-12);
 }
 @Test void handlesDcAndNyquistWithoutDoublingEndpoints(){
  double[] dc=new double[512],nyquist=new double[512];java.util.Arrays.fill(dc,1);for(int i=0;i<512;i++)nyquist[i]=i%2==0?1:-1;
  assertEquals(.1,Spectrum.analyze(dc,2).psd()[0]*.5,1e-12);
  var s=Spectrum.analyze(nyquist,2);assertEquals(.1,s.psd()[256]*.5,1e-12);assertEquals(.2,s.energyJ(),1e-12);
 }
 @Test void rectangularPulseHasFiniteEnergy(){
  double[] pulse=new double[512];for(int i=0;i<128;i++)pulse[i]=2;
  var s=Spectrum.analyze(pulse,2);assertEquals(.2,s.energyJ(),1e-12);
  assertEquals(.2,java.util.Arrays.stream(s.esd()).sum()*s.binHz(),1e-12);
 }
 @Test void frequencyCsvContainsUnitsAndAllOneSidedBins(){
  var result=Spectrum.analyze(new double[512],1);String[] lines=Spectrum.csv(result).split("\n");
  assertEquals(258,lines.length);assertEquals("frequency_hz,amplitude_sqrtW,psd_W_per_Hz,esd_J_per_Hz",lines[0]);
  assertEquals("256.0,0.0,0.0,0.0",lines[257]);
 }

}
