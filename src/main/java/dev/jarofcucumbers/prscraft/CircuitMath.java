package dev.jarofcucumbers.prscraft;
public final class CircuitMath{
    private CircuitMath(){}
    public record Correlation(double[] values,double lagSpan,double peakLag,double peakValue){}
    public static double[] add(double[] a,double[] b,double gainA,double gainB){double[] out=new double[a.length];for(int i=0;i<a.length;i++)out[i]=gainA*a[i]+gainB*b[i];return out;}
    public static double[] integral(double[] input,double span,double end){
        double[] out=new double[input.length];double dt=span/input.length;
        for(int i=1;i<input.length;i++){double left=(i-1)*dt,right=Math.min(i*dt,end),width=Math.max(0,right-left);
            double fraction=width/dt,next=input[i-1]+(input[i]-input[i-1])*fraction;
            out[i]=out[i-1]+(input[i-1]+next)*.5*width;}
        return out;
    }
    public static double[] delay(double[] input,double span,double seconds){
        double[] out=new double[input.length];double shift=seconds*input.length/span;
        for(int i=0;i<out.length;i++){double index=i-shift;if(index<0||index>input.length-1)continue;int left=(int)Math.floor(index),right=Math.min(input.length-1,left+1);out[i]=input[left]+(input[right]-input[left])*(index-left);}return out;
    }
    public static double[] gate(double[] input,double span,double start,double width,boolean hold){double[] out=input.clone();for(int i=0;i<out.length;i++){double t=span*i/out.length;if(t+1e-12<start||!hold&&t>=start+width-1e-12)out[i]=0;}return out;}
    public static double[] productIntegral(double[] a,double[] b,double span,double end,boolean normalized){
        double[] product=new double[a.length];for(int i=0;i<a.length;i++)product[i]=a[i]*b[i];double[] result=integral(product,span,end);
        if(normalized){double[] aa=new double[a.length],bb=new double[a.length];for(int i=0;i<a.length;i++){aa[i]=a[i]*a[i];bb[i]=b[i]*b[i];}
            aa=integral(aa,span,end);bb=integral(bb,span,end);for(int i=0;i<result.length;i++){double denominator=Math.sqrt(aa[i]*bb[i]);result[i]=denominator==0?0:Math.max(-1,Math.min(1,result[i]/denominator));}}
        return result;
    }
    /** Positive peak lag means that B is delayed relative to A. Zero padding, never circular wrap. */
    public static Correlation correlate(double[] a,double[] b,double span,double requestedLag,boolean normalized){
        int n=a.length;double dt=span/n;int limit=Math.max(1,Math.min(n/2,(int)Math.floor(requestedLag/dt)));
        double denominator=0,aa=0,bb=0;for(int i=0;i<n;i++){aa+=a[i]*a[i];bb+=b[i]*b[i];}denominator=Math.sqrt(aa*bb);
        double[] out=new double[n];double best=0,peak=0;
        for(int i=0;i<n;i++){
            int lag=(int)Math.round((i-n/2.0)*limit/(n/2.0));double value=0;
            for(int j=0;j<n;j++){int shifted=j+lag;if(shifted>=0&&shifted<n)value+=a[j]*b[shifted];}
            value=normalized?(denominator==0?0:Math.max(-1,Math.min(1,value/denominator))):value*dt;out[i]=value;
            double seconds=lag*dt;if(Math.abs(value)>Math.abs(best)+1e-12||Math.abs(Math.abs(value)-Math.abs(best))<1e-12&&Math.abs(seconds)<Math.abs(peak)){best=value;peak=seconds;}
        }
        return new Correlation(out,2*limit*dt,peak,best);
    }
}
