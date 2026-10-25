package dev.jarofcucumbers.prscraft;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class LineCodingTest{
    private RadioSettings source(String code,String quantity,double duty){
        var lab=new LabSettings(false,false,.5,-1,2,"0,1",42,0,0,0,false,quantity,.2,code);
        return new RadioSettings("sin(10)",100,16,true,true,false,0,100,20,true,duty,"bits","1010",10,lab);
    }
    @Test void unrzUsesZeroInsteadOfNegativeLevel(){var s=source("unrz","relative",.5);assertEquals(2,SignalChain.output(s,.075));assertEquals(0,SignalChain.output(s,.15));assertEquals(0,SignalChain.output(s,.45));}
    @Test void urzReturnsToZeroAtTheDutyBoundary(){var s=source("urz","relative",.5);assertEquals(2,SignalChain.output(s,.049));assertEquals(0,SignalChain.output(s,.05));assertEquals(0,SignalChain.output(s,.15));assertEquals(2,SignalChain.output(s,.2));}
    @Test void pointAndVectorGeneratorsAgreeForBothCodes(){for(String code:new String[]{"unrz","urz"}){var s=source(code,"relative",.35);var values=SignalChain.generate(s,.4,512,false);for(int i=0;i<values.length;i++)assertEquals(SignalChain.output(s,.4*i/512),values[i],1e-12);}}
    @Test void powerAndEnergyNormalizationIncludeRzDuty(){
        var s=source("urz","power",.5);var spectrum=Spectrum.analyze(SignalChain.generate(s,.4,512,false),.4);assertEquals(.2,spectrum.powerW(),1e-10);
        var energy=source("urz","energy",.5);assertEquals(.2,Spectrum.analyze(SignalChain.generate(energy,.4,512,false),.4).energyJ(),1e-10);
    }
    @Test void coherentReceiversDecodeUnipolarPatterns(){for(String code:new String[]{"unrz","urz"}){var s=source(code,"relative",.5);var values=SignalChain.generate(s,.4,512,false);assertEquals("1010",SignalChain.decodeUnipolar(values,.4,10,2,.5,code.equals("urz")));}}
    @Test void oldNbtDefaultsToBipolarAndNewTemplatesPreserveCode(){var s=source("urz","relative",.5);assertEquals(s,RadioSettings.read(s.save()));var old=s.lab().save();old.remove("lineCode");assertEquals("bipolar",LabSettings.read(old).lineCode());assertEquals("urz",s.lab().variance(.2).lineCode());}
}
