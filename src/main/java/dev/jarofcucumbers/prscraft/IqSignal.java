package dev.jarofcucumbers.prscraft;
import java.util.Random;
/** Coherent normalized complex-symbol laboratory, Es=1 before gain, variance per quadrature. */
public record IqSignal(String mode,int bitLength,double[] i,double[] q){
    public IqSignal{if(!java.util.Set.of("qpsk","qam16").contains(mode)||bitLength<0||bitLength>BitStream.MAX_BYTES*8||i.length!=q.length||i.length!=(bitLength+(mode.equals("qpsk")?1:3))/(mode.equals("qpsk")?2:4))throw new IllegalArgumentException("modem");i=i.clone();q=q.clone();for(int k=0;k<i.length;k++)if(!Double.isFinite(i[k])||!Double.isFinite(q[k]))throw new IllegalArgumentException("signal_range");}
    public int bitsPerSymbol(){return mode.equals("qpsk")?2:4;}
    private static int bits(BitStream b,int offset,int n){int v=0;for(int k=0;k<n;k++)v=(v<<1)|(offset+k<b.bitLength()&&b.bit(offset+k)?1:0);return v;}
    private static double axis(int label){return new double[]{-3,-1,3,1}[label]/Math.sqrt(10);}
    private static int nearest(double value){int selected=0;double distance=Double.POSITIVE_INFINITY;for(int c=0;c<4;c++){double d=Math.abs(value-axis(c));if(d<distance){selected=c;distance=d;}}return selected;}
    public static IqSignal encode(BitStream bits,String mode){boolean qpsk=mode.equals("qpsk");int width=qpsk?2:4,n=(bits.bitLength()+width-1)/width;double[] i=new double[n],q=new double[n];for(int k=0;k<n;k++){int at=k*width;if(qpsk){i[k]=(bits(bits,at,1)==0?-1:1)/Math.sqrt(2);q[k]=(bits(bits,at+1,1)==0?-1:1)/Math.sqrt(2);}else{i[k]=axis(bits(bits,at,2));q[k]=axis(bits(bits,at+2,2));}}return new IqSignal(mode,bits.bitLength(),i,q);}
    public BitStream decisions(){var writer=new BitStream.Writer();int at=0;for(int k=0;k<i.length;k++){int symbol=mode.equals("qpsk")?((i[k]>0?2:0)|(q[k]>0?1:0)):((nearest(i[k])<<2)|nearest(q[k]));for(int b=bitsPerSymbol()-1;b>=0;b--)if(at++<bitLength)writer.bit((symbol&(1<<b))!=0);}return writer.finish();}
    public double[] bitLevels(){var bits=decisions();double[] values=new double[bitLength];for(int k=0;k<values.length;k++)values[k]=bits.bit(k)?1:-1;return values;}
    public IqSignal channel(double gain,double mean,double sigma,long seed,boolean clip){var random=new Random(seed);double[] a=i.clone(),b=q.clone();for(int k=0;k<a.length;k++){a[k]=a[k]*gain+mean+sigma*random.nextGaussian();b[k]=b[k]*gain+mean+sigma*random.nextGaussian();if(clip){a[k]=Math.max(-16,Math.min(16,a[k]));b[k]=Math.max(-16,Math.min(16,b[k]));}}return new IqSignal(mode,bitLength,a,b);}
    public IqSignal disperse(double tau,double bitRate){return new IqSignal(mode,bitLength,CableSettings.disperse(i,bitsPerSymbol()/bitRate,tau),CableSettings.disperse(q,bitsPerSymbol()/bitRate,tau));}
}
