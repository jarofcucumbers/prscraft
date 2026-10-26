package dev.jarofcucumbers.prscraft;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class OrthogonalityTest{
 private double[] wave(boolean cosine){double[] a=new double[512];for(int i=0;i<a.length;i++)a[i]=cosine?Math.cos(2*Math.PI*i/512):Math.sin(2*Math.PI*i/512);return a;}
 @Test void sineAndCosineAreOrthogonalOnFullPeriodButNotQuarterPeriod(){var cfg=new OrthogonalitySettings(0,1,.01,false,100);assertTrue(Orthogonality.measure(wave(false),wave(true),1,cfg).orthogonal());var quarter=Orthogonality.measure(wave(false),wave(true),1,new OrthogonalitySettings(0,.25,.01,false,100));assertFalse(quarter.orthogonal());assertTrue(quarter.normalized()>.6);}
 @Test void identicalAndZeroInputsAreNotAnOrthogonalSignallingBasis(){assertEquals(1,Orthogonality.measure(wave(false),wave(false),1,OrthogonalitySettings.DEFAULT).normalized(),1e-12);assertFalse(Orthogonality.measure(new double[512],wave(false),1,OrthogonalitySettings.DEFAULT).valid());}
 @Test void discreteAndContinuousHaveExplicitUnitsAndLimits(){var r=Orthogonality.measure(wave(false),wave(true),1,new OrthogonalitySettings(0,1,.01,true,100));assertTrue(r.orthogonal());assertEquals(100,r.count());assertThrows(IllegalArgumentException.class,()->Orthogonality.measure(wave(false),wave(true),1,new OrthogonalitySettings(0,1,.01,true,10000)));assertEquals(OrthogonalitySettings.DEFAULT,OrthogonalitySettings.read(OrthogonalitySettings.DEFAULT.save()));}
}
