package dev.jarofcucumbers.prscraft;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class AwgnTest {
 @Test void qMatchesCentralValuesAndAsymptoticTail(){
  assertEquals(.5,Awgn.q(0),1e-12);assertEquals(.158655253931457,Awgn.q(1),8e-8);
  assertEquals(.00134989803163,Awgn.q(3),8e-8);
  assertEquals(3.167124183311998e-5,Awgn.q(4),1e-15);
  assertEquals(2.866515718791933e-7,Awgn.q(5),1e-17);
  assertEquals(6.22096057427178e-16,Awgn.q(8),1e-26);
  assertEquals(1-Awgn.q(2),Awgn.q(-2),1e-12);
 }
 @Test void unequalPriorsUseMapAndMonteCarloMatchesTheory(){
  var r=Awgn.experiment(-1,2,.2,.4,1,100000,123);
  assertEquals(.9+Math.log(4)/3,r.threshold(),1e-12);
  assertEquals(r.theory(),r.empirical(),.003);
  assertEquals(.4,r.measuredMean(),.012);assertEquals(1,r.measuredVariance(),.02);
 }
 @Test void normalizationPreservesDecisionProbability(){
  var physical=Awgn.experiment(-2,3,.7,1.5,4,10000,3);
  var normalized=Awgn.experiment(-1,1.5,.7,0,1,10000,3);
  assertEquals(physical.theory(),normalized.theory(),1e-12);
  assertEquals(physical.empirical(),normalized.empirical(),1e-12);
 }
 @Test void handlesZeroNoiseAndDegeneratePriors(){
  assertEquals(0,Awgn.experiment(-1,1,.5,30,0,1000,1).theory());
  assertEquals(0,Awgn.experiment(-1,1,1,0,100,1000,1).empirical());
  assertEquals(0,Awgn.experiment(-1,1,0,0,100,1000,1).empirical());
 }
}
