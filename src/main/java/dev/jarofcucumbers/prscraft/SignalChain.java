package dev.jarofcucumbers.prscraft;

import java.util.*;

/** Finite-window DSP, independent of rendering and networking. */
public final class SignalChain {
    private SignalChain() {}
    public record Stage(String kind,ProcessorSettings settings) {}
    public static double raw(RadioSettings s,double t) {
        if(s.signalMode().equals("bits")) {
            int bit=Math.floorMod((long)Math.floor(t*s.bitRate()+1e-9),s.bits().length());
            return s.bits().charAt(bit)=='1'?1:-1;
        }
        return SignalProgram.parse(s.expression()).at(t);
    }
    public static double output(RadioSettings s,double t) {
        if(s.signalMode().equals("bits")||s.signalMode().equals("analog"))return raw(s,t);
        var program=SignalProgram.parse(s.expression());
        return s.signalMode().equals("sampled")?program.sampled(t,s.sampleHz(),s.secondKind(),s.duty()):
            program.digital(t,s.sampleHz(),s.levels(),s.secondKind(),s.duty());
    }
    public static double span(RadioSettings source,List<Stage> stages) {
        double span=source.signalMode().equals("bits")?8/source.bitRate():Transmission.windowSeconds(SignalProgram.parse(source.expression()),source.sampleHz());
        double highest=0;
        for(Stage stage:stages)if(stage.kind.equals("modulator"))highest+=stage.settings.oscillatorHz();
        return highest==0?span:Math.min(span,128/highest);
    }
    public static double[] generate(RadioSettings s,double span,int count,boolean original) {
        double[] values=new double[count];
        // t=i*span/count forms an exact periodic grid for FFT processing.
        for(int i=0;i<count;i++)values[i]=original?raw(s,span*i/count):output(s,span*i/count);
        return values;
    }
    public static double[] process(double[] input,double span,List<Stage> stages) {
        double[] values=input.clone();
        for(Stage stage:stages) {
            ProcessorSettings s=stage.settings;
            switch(stage.kind) {
                case "amplifier" -> {double gain=Math.pow(10,s.gainDb()/20);for(int i=0;i<values.length;i++)values[i]=Math.max(-16,Math.min(16,values[i]*gain));}
                case "frequency_filter" -> values=bandPass(values,span,s.lowHz(),s.highHz());
                case "modulator" -> {
                    double peak=Arrays.stream(values).map(Math::abs).max().orElse(1);peak=Math.max(1e-12,peak);
                    double[] incoming=values.clone();
                    for(int i=0;i<values.length;i++) {
                        double t=span*i/values.length;
                        if(s.modulation().equals("bpsk")) {
                            int start=(int)Math.ceil(Math.floor(t*s.symbolHz()+1e-9)/s.symbolHz()/span*values.length-1e-9);
                            double symbol=incoming[Math.max(0,Math.min(incoming.length-1,start))]>=0?1:-1;
                            values[i]=symbol*Math.cos(2*Math.PI*s.oscillatorHz()*t);
                        } else values[i]=(1+s.depth()*incoming[i]/peak)*Math.cos(2*Math.PI*s.oscillatorHz()*t);
                    }
                }
                default -> throw new IllegalArgumentException("processor");
            }
        }
        return values;
    }
    /** Ideal brick-wall FFT filter over this finite periodic window; no persistent state. */
    public static double[] bandPass(double[] input,double span,double low,double high) {
        int n=input.length;if(Integer.bitCount(n)!=1)throw new IllegalArgumentException("Power-of-two FFT required");
        double[] re=input.clone(),im=new double[n];fft(re,im,false);
        for(int k=0;k<n;k++){double f=Math.min(k,n-k)/span;if(f<low||f>high){re[k]=0;im[k]=0;}}
        fft(re,im,true);return re;
    }
    private static void fft(double[] re,double[] im,boolean inverse) {
        int n=re.length;
        for(int i=1,j=0;i<n;i++){int bit=n>>1;for(;(j&bit)!=0;bit>>=1)j^=bit;j^=bit;if(i<j){double t=re[i];re[i]=re[j];re[j]=t;t=im[i];im[i]=im[j];im[j]=t;}}
        for(int len=2;len<=n;len<<=1){double angle=2*Math.PI/len*(inverse?1:-1),wr=Math.cos(angle),wi=Math.sin(angle);
            for(int start=0;start<n;start+=len){double ar=1,ai=0;for(int j=0;j<len/2;j++){
                int a=start+j,b=a+len/2;double br=re[b]*ar-im[b]*ai,bi=re[b]*ai+im[b]*ar;
                re[b]=re[a]-br;im[b]=im[a]-bi;re[a]+=br;im[a]+=bi;
                double next=ar*wr-ai*wi;ai=ar*wi+ai*wr;ar=next;
            }}
        }
        if(inverse)for(int i=0;i<n;i++){re[i]/=n;im[i]/=n;}
    }
    public static String decodeBpsk(double[] received,double span,double oscillatorHz,double symbolHz) {
        int symbols=Math.min(128,(int)Math.floor(span*symbolHz+1e-9));StringBuilder bits=new StringBuilder();
        for(int symbol=0;symbol<symbols;symbol++){
            double sum=0;int count=0;
            for(int i=0;i<received.length;i++){double t=span*i/received.length;if((int)Math.floor(t*symbolHz+1e-9)==symbol){sum+=received[i]*Math.cos(2*Math.PI*oscillatorHz*t);count++;}}
            bits.append(count==0?'?':sum>=0?'1':'0');
        }
        return bits.toString();
    }
    public static int[] pack(double[] samples){int[] bits=new int[samples.length];for(int i=0;i<bits.length;i++)bits[i]=Float.floatToIntBits((float)samples[i]);return bits;}
    public static double[] unpack(int[] bits){double[] samples=new double[bits.length];for(int i=0;i<bits.length;i++)samples[i]=Float.intBitsToFloat(bits[i]);return samples;}
}
