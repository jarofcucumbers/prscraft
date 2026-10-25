package dev.jarofcucumbers.prscraft;
public final class Spectrum {
    private Spectrum(){}
    public record Result(double[] hz,double[] amplitude,double[] psd,double[] esd,double powerW,double energyJ,double binHz,double sampleHz){}
    public static Result analyze(double[] samples,double seconds){
        int n=samples.length;if(n<2||n>4096||Integer.bitCount(n)!=1||!Double.isFinite(seconds)||seconds<=0)throw new IllegalArgumentException("spectrum");
        double power=0;for(double sample:samples){if(!Double.isFinite(sample))throw new IllegalArgumentException("spectrum");power+=.1*sample*sample/n;}
        double[] re=samples.clone(),im=new double[n];SignalChain.fft(re,im,false);
        double[] hz=new double[n/2+1],amplitude=new double[hz.length],psd=new double[hz.length],esd=new double[hz.length];
        double dt=seconds/n;
        for(int k=0;k<hz.length;k++){
            double factor=k==0||k==n/2?1:2,square=re[k]*re[k]+im[k]*im[k];hz[k]=k/seconds;
            amplitude[k]=Math.sqrt(.1*square)/n*factor;
            esd[k]=factor*.1*dt*dt*square;psd[k]=esd[k]/seconds;
        }
        return new Result(hz,amplitude,psd,esd,power,power*seconds,1/seconds,n/seconds);
    }
    public static String csv(Result result){
        StringBuilder text=new StringBuilder("frequency_hz,amplitude_sqrtW,psd_W_per_Hz,esd_J_per_Hz\n");
        for(int i=0;i<result.hz().length;i++)text.append(result.hz()[i]).append(',').append(result.amplitude()[i]).append(',').append(result.psd()[i]).append(',').append(result.esd()[i]).append('\n');
        return text.toString();
    }

}
