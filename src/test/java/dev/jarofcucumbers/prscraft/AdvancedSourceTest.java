package dev.jarofcucumbers.prscraft;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class AdvancedSourceTest {
 private RadioSettings source(String mode,boolean repeat,boolean random,String quantity,double target){
  var l=new LabSettings(repeat,random,.8,-2,1,"1,0,-1,0",42,0,.5,.25,true,quantity,target);
  return new RadioSettings("sin(10)",100,16,true,true,false,.5,100,20,true,.5,mode,"1010",10,l);
 }
 @Test void generatorHasReproduciblePriorsAndAmplitudeLevels(){
  int ones=0;for(int i=0;i<100000;i++)if(SourceMath.randomBit(42,i,.8))ones++;
  assertEquals(.8,ones/100000.0,.006);
  var s=source("bits",false,true,"relative",1);
  for(int i=0;i<100;i++)assertEquals(SourceMath.randomBit(42,i,.8)?1:-2,SignalChain.raw(s,(i+.1)/10));
 }
 @Test void periodicAndFiniteDiscreteSignalsHaveDifferentTails(){
  var finite=source("discrete",false,false,"relative",1);var periodic=source("discrete",true,false,"relative",1);
  assertEquals(0,SignalChain.output(finite,.04));assertEquals(1,SignalChain.output(periodic,.04));
  assertEquals(0,SignalChain.output(source("bits",false,false,"relative",1),.41));
 }
 @Test void powerInWattsAndEnergyInJoulesAreDimensionallyCorrect(){
  var tone=source("analog",true,false,"power",2);var spectrum=Spectrum.analyze(SignalChain.generate(tone,1,512,false),1);
  assertEquals(2,spectrum.powerW(),1e-10);
  var finite=source("discrete",false,false,"energy",.5);var pulse=Spectrum.analyze(SignalChain.generate(finite,.04,512,false),.04);
  assertEquals(.5,pulse.energyJ(),1e-10);
  assertThrows(IllegalArgumentException.class,()->source("analog",true,false,"energy",1));
 }
 @Test void settingsRoundTripKeepsAllAdvancedValues(){
  var original=source("discrete",false,false,"energy",.5);assertEquals(original,RadioSettings.read(original.save()));
 }
}
