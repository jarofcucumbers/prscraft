package dev.jarofcucumbers.prscraft;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class CircuitMathTest{
 @Test void integratorUsesTimeAndHoldsAtT(){double[] x=new double[512];java.util.Arrays.fill(x,2);var y=CircuitMath.integral(x,1,.25);assertEquals(.5,y[128],1e-12);assertEquals(.5,y[511],1e-12);assertEquals(0,y[0]);}
 @Test void adderSupportsSubtraction(){assertArrayEquals(new double[]{0,5},CircuitMath.add(new double[]{1,3},new double[]{2,1},2,-1),1e-12);}
 @Test void gateClosesAtExactTAndCanReopenAfterPulse(){double[] x=new double[8];java.util.Arrays.fill(x,1);assertArrayEquals(new double[]{0,0,1,1,1,1,1,1},CircuitMath.gate(x,1,.25,.25,true));assertArrayEquals(new double[]{0,0,1,1,0,0,0,0},CircuitMath.gate(x,1,.25,.25,false));}
 @Test void fractionalDelayUsesZeroPaddingNotWrapping(){assertArrayEquals(new double[]{0,1.5,2.5,3.5},CircuitMath.delay(new double[]{1,2,3,4},1,.125),1e-12);assertArrayEquals(new double[4],CircuitMath.delay(new double[]{1,2,3,4},1,1));}
 @Test void correlationFindsKnownPositiveDelayAndBoundsNormalizedValues(){
  double[] a=new double[512];for(int i=30;i<400;i++)a[i]=SourceMath.randomBit(7,i,.5)?1:-1;
  double[] b=CircuitMath.delay(a,1,12.0/512);var r=CircuitMath.correlate(a,b,1,.1,true);
  assertEquals(12.0/512,r.peakLag(),1e-12);assertEquals(1,r.peakValue(),1e-12);
  var same=CircuitMath.correlate(a,a,1,.1,true);assertEquals(0,same.peakLag());assertEquals(1,same.values()[256],1e-12);
  for(double value:r.values())assertTrue(Math.abs(value)<=1+1e-12);
  assertEquals(0,CircuitMath.correlate(new double[512],new double[512],1,.1,true).peakValue());
 }
 @Test void productIntegralHasExpectedPhysicalScaling(){double[] a=new double[512],b=new double[512];java.util.Arrays.fill(a,2);java.util.Arrays.fill(b,3);assertEquals(3,CircuitMath.productIntegral(a,b,1,.5,false)[511],1e-12);assertEquals(1,CircuitMath.productIntegral(a,b,1,.5,true)[511],1e-12);}
 @Test void cableValidationAndDamageAreMonotonic(){var shortCable=new CableSettings(.25,0);var damaged=new CableSettings(.25,.5);assertEquals(.25,shortCable.metres());assertTrue(damaged.extraLossDb()>0&&damaged.noiseVariance()>0);assertThrows(IllegalArgumentException.class,()->new CableSettings(0,0));assertThrows(IllegalArgumentException.class,()->new CableSettings(Double.NaN,0));assertThrows(IllegalArgumentException.class,()->new CableSettings(1,1.1));}
}
