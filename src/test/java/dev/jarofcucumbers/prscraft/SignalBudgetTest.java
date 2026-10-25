package dev.jarofcucumbers.prscraft;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class SignalBudgetTest {
 @Test void attenuationIncreasesWithDistanceAndFrequency() {
  var m = SignalBudget.Medium.COAXIAL;
  assertTrue(m.lossDb(20,100)>m.lossDb(10,100));
  assertTrue(m.lossDb(10,400)>m.lossDb(10,100));
  assertEquals(17,SignalBudget.receivedDbm(20,m,10,100),1e-9);
 }
 @Test void fiberHasLowerLossAndNoCoupling() {
  assertTrue(SignalBudget.Medium.FIBER.lossDb(64,100)<SignalBudget.Medium.METALLIC.lossDb(64,100));
  assertEquals(0,SignalBudget.Medium.FIBER.coupling());
 }
 @Test void overlapAndInvalidDistances() {
  assertEquals(1,SignalBudget.overlap(100,100,40));
  assertEquals(0.375,SignalBudget.overlap(100,125,40));
  assertEquals(0,SignalBudget.overlap(100,150,40));
  assertThrows(IllegalArgumentException.class,()->SignalBudget.Medium.COAXIAL.lossDb(-1,100));
 }
}
