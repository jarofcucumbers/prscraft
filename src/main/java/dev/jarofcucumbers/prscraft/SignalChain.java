package dev.jarofcucumbers.prscraft;

import java.util.*;

/** Finite-window DSP, independent of rendering and networking. */
public final class SignalChain {
    private SignalChain() {}
    public record Stage(String kind,ProcessorSettings settings) {}
    public static double raw(RadioSettings s,double seconds) {
        double t=seconds+s.lab().offsetSeconds(),value;
        if(s.signalMode().equals("bits")) {
            long symbol=(long)Math.floor(t*s.bitRate()+1e-9);
            if(symbol<0)return 0;
            boolean one;
            if(s.lab().randomBits())one=SourceMath.randomBit(s.lab().seed(),symbol,s.lab().probabilityOne());
            else {if(!s.lab().repeat()&&symbol>=s.bits().length())return 0;one=s.bits().charAt((int)(symbol%s.bits().length()))=='1';}
            value=one?s.lab().amplitudeOne():s.lab().amplitudeZero();
        }else if(s.signalMode().equals("discrete")){
            double[] values=LabSettings.parseSamples(s.lab().samples());long index=(long)Math.floor(t*s.sampleHz()+1e-9);
            if(index<0||!s.lab().repeat()&&index>=values.length)return 0;value=values[(int)(index%values.length)];
        }else value=SignalProgram.parse(s.expression()).at(t);
        return value*SourceMath.scale(s);
    }
    public static double output(RadioSettings s,double seconds) {
        if(s.signalMode().equals("bits")||s.signalMode().equals("analog")||s.signalMode().equals("discrete"))return raw(s,seconds);
        var program=SignalProgram.parse(s.expression());double t=seconds+s.lab().offsetSeconds();
        double value=s.signalMode().equals("sampled")?program.sampled(t,s.sampleHz(),s.secondKind(),s.duty()):program.digital(t,s.sampleHz(),s.levels(),s.secondKind(),s.duty());
        return value*SourceMath.scale(s);
    }
    public static double span(RadioSettings source,List<Stage> stages) {
        double span=source.signalMode().equals("bits")?Math.min(8, (source.lab().randomBits()?8:source.bits().length())/source.bitRate()):
            source.signalMode().equals("discrete")?Math.min(8,LabSettings.parseSamples(source.lab().samples()).length/source.sampleHz()):Transmission.windowSeconds(SignalProgram.parse(source.expression()),source.sampleHz());
        double highest=0;
        for(Stage stage:stages)if(stage.kind.equals("modulator"))highest+=stage.settings.oscillatorHz();
        return highest==0?span:Math.min(span,128/highest);
    }
    public static double[] generate(RadioSettings s,double span,int count,boolean original) {
        double[] values=new double[count];double scale=SourceMath.scale(s);
        SignalProgram program=s.signalMode().equals("bits")||s.signalMode().equals("discrete")?null:SignalProgram.parse(s.expression());
        double[] discrete=s.signalMode().equals("discrete")?LabSettings.parseSamples(s.lab().samples()):null;
        for(int i=0;i<count;i++){
            double t=span*i/count+s.lab().offsetSeconds(),value=0;
            if(s.signalMode().equals("bits")){
                long symbol=(long)Math.floor(t*s.bitRate()+1e-9);
                if(s.lab().randomBits()||s.lab().repeat()||symbol<s.bits().length()){
                    boolean one=s.lab().randomBits()?SourceMath.randomBit(s.lab().seed(),symbol,s.lab().probabilityOne()):s.bits().charAt((int)(symbol%s.bits().length()))=='1';
                    value=one?s.lab().amplitudeOne():s.lab().amplitudeZero();
                }
            }else if(discrete!=null){long index=(long)Math.floor(t*s.sampleHz()+1e-9);if(s.lab().repeat()||index<discrete.length)value=discrete[(int)(index%discrete.length)];}
            else if(original||s.signalMode().equals("analog"))value=program.at(t);
            else if(s.signalMode().equals("sampled"))value=program.sampled(t,s.sampleHz(),s.secondKind(),s.duty());
            else value=program.digital(t,s.sampleHz(),s.levels(),s.secondKind(),s.duty());
            values[i]=value*scale;
        }
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
                            double symbol=incoming[Math.max(0,Math.min(incoming.length-1,start))];
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
    static void fft(double[] re,double[] im,boolean inverse) {
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
