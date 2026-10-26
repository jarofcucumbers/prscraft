package dev.jarofcucumbers.prscraft;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class EnvironmentEffectTest{
 @Test void fibreIsImmuneAndShieldingReducesLocalNoise(){assertEquals(EnvironmentEffect.NONE,EnvironmentEffect.model(SignalBudget.Medium.FIBER,100,true,true,6,6,6,0));var open=EnvironmentEffect.model(SignalBudget.Medium.METALLIC,10,true,false,0,0,1,0);var shielded=EnvironmentEffect.model(SignalBudget.Medium.METALLIC,10,true,false,0,0,1,2);assertTrue(open.lossDb()>0&&shielded.variance()<open.variance());assertEquals(open.lossDb(),shielded.lossDb());}
 @Test void dispersionIsCausalIdentityAtZeroAndBroadensAnImpulse(){double[] impulse={1,0,0,0,0};assertArrayEquals(impulse,CableSettings.disperse(impulse,.01,0));double[] blurred=CableSettings.disperse(impulse,.01,.02);assertTrue(blurred[0]<1&&blurred[0]>0&&blurred[1]>0&&blurred[2]<blurred[1]);assertThrows(IllegalArgumentException.class,()->new CableSettings(1,0,-1,true));}
}
