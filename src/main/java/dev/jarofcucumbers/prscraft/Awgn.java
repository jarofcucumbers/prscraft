package dev.jarofcucumbers.prscraft;
import java.util.Random;
/** Binary scalar AWGN experiment. This is a symbol-space model, not a general RF-chain BER estimator. */
public final class Awgn {
    private Awgn(){}
    public record Result(double threshold,double theory,double empirical,double measuredMean,double measuredVariance,double z0,double z1){}
    public static double q(double x){
        if(Double.isNaN(x))throw new IllegalArgumentException("q");
        if(x==Double.POSITIVE_INFINITY)return 0;if(x==Double.NEGATIVE_INFINITY)return 1;if(x<0)return 1-q(-x);if(x==0)return .5;
        double density=Math.exp(-.5*x*x)/Math.sqrt(2*Math.PI);
        if(x>3){double fraction=0;for(int k=64;k>=1;k--)fraction=k/(x+fraction);return density/(x+fraction);}
        double t=1/(1+.2316419*x);
        return density*t*(.319381530+t*(-.356563782+t*(1.781477937+t*(-1.821255978+t*1.330274429))));
    }
    public static Result experiment(double a0,double a1,double p1,double mean,double variance,int trials,long seed){
        if(!Double.isFinite(a0)||!Double.isFinite(a1)||a0>=a1||!Double.isFinite(p1)||p1<0||p1>1||!Double.isFinite(mean)||!Double.isFinite(variance)||variance<0||trials<1||trials>100000)throw new IllegalArgumentException("awgn");
        double p0=1-p1,sigma=Math.sqrt(variance),m0=a0+mean,m1=a1+mean;
        double threshold=p1==0?Double.POSITIVE_INFINITY:p1==1?Double.NEGATIVE_INFINITY:(m0+m1)/2+variance/(m1-m0)*Math.log(p0/p1);
        double z0=sigma==0?Double.POSITIVE_INFINITY:(threshold-m0)/sigma,z1=sigma==0?Double.POSITIVE_INFINITY:(m1-threshold)/sigma;
        double theory=p1==0||p1==1||variance==0?0:p0*q(z0)+p1*q(z1);
        Random random=new Random(seed);int errors=0;double sum=0,squares=0;
        for(int i=0;i<trials;i++){boolean one=random.nextDouble()<p1;double noise=mean+sigma*random.nextGaussian();double y=(one?a1:a0)+noise;
            if((y>=threshold)!=one)errors++;sum+=noise;squares+=noise*noise;}
        double average=sum/trials;
        return new Result(threshold,theory,(double)errors/trials,average,Math.max(0,squares/trials-average*average),z0,z1);
    }
}
