package dev.jarofcucumbers.prscraft;
import java.util.*;
public final class SourceMath {
    private SourceMath(){}
    public static double reference(String mode,String expression,String bits,double bitRate,double sampleRate,LabSettings lab){return reference(mode,expression,bits,bitRate,sampleRate,lab,1);}
    public static double reference(String mode,String expression,String bits,double bitRate,double sampleRate,LabSettings lab,double duty){
        double square=0,duration=1;
        if(mode.equals("bits")){
            if(lab.randomBits())square=(1-lab.probabilityOne())*Math.pow(lab.lineCode().equals("bipolar")?lab.amplitudeZero():0,2)+lab.probabilityOne()*lab.amplitudeOne()*lab.amplitudeOne();
            else {for(char c:bits.toCharArray()){double a=c=='1'?lab.amplitudeOne():lab.lineCode().equals("bipolar")?lab.amplitudeZero():0;square+=a*a;}square/=bits.length();}
            if(lab.lineCode().equals("urz"))square*=duty;
            duration=bits.length()/bitRate;
        }else if(mode.equals("discrete")){
            double[] values=LabSettings.parseSamples(lab.samples());for(double value:values)square+=value*value;square/=values.length;duration=values.length/sampleRate;
        }else{
            // Equal-frequency components must combine before computing average power.
            Map<String,Double> coefficients=new HashMap<>();
            for(var term:SignalProgram.parse(expression).terms())coefficients.merge(term.cosine()+":"+term.hz(),term.amplitude(),Double::sum);
            for(double amplitude:coefficients.values())square+=amplitude*amplitude/2;
        }
        return .1*square*(lab.quantity().equals("energy")?duration:1);
    }
    public static void validate(String mode,String expression,String bits,double bitRate,double sampleRate,LabSettings lab){
        if(lab.quantity().equals("relative"))return;
        if(lab.quantity().equals("energy")&&(!mode.equals("bits")&&!mode.equals("discrete")||lab.repeat()||mode.equals("bits")&&lab.randomBits()))throw new IllegalArgumentException("finite_energy");
        if(reference(mode,expression,bits,bitRate,sampleRate,lab)==0&&lab.target()>0)throw new IllegalArgumentException("zero_power");
    }
    public static double scale(RadioSettings s){
        if(s.lab().quantity().equals("relative"))return 1;
        double reference=reference(s.signalMode(),s.expression(),s.bits(),s.bitRate(),s.sampleHz(),s.lab(),s.duty());
        return reference==0?0:Math.sqrt(s.lab().target()/reference);
    }
    /** Counter-based reproducible Bernoulli sequence: changing the preview grid does not change bits. */
    public static boolean randomBit(long seed,long symbol,double probability){
        long z=seed+symbol*0x9E3779B97F4A7C15L;z=(z^(z>>>30))*0xBF58476D1CE4E5B9L;z=(z^(z>>>27))*0x94D049BB133111EBL;z^=z>>>31;
        return (z>>>11)*0x1.0p-53<probability;
    }
}
