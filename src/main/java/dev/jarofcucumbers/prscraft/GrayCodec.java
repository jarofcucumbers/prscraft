package dev.jarofcucumbers.prscraft;
/** MSB first, coherent capture-local word boundaries, hard-decision baseband conversion. */
public final class GrayCodec{
    private GrayCodec(){}
    public static int encode(int binary){return binary^(binary>>>1);}
    public static int decode(int gray){int binary=0;for(;gray!=0;gray>>>=1)binary^=gray;return binary;}
    public static String convert(String bits,int width,boolean reverse){
        if(bits==null||!bits.matches("[01]{1,128}")||width<1||width>16)throw new IllegalArgumentException("gray_settings");
        StringBuilder out=new StringBuilder();
        for(int start=0;start<bits.length();start+=width){int end=Math.min(start+width,bits.length()),n=Integer.parseInt(bits.substring(start,end),2);out.append(word(reverse?decode(n):encode(n),end-start));}return out.toString();
    }
    public static String word(int value,int width){String s=Integer.toBinaryString(value);return "0".repeat(Math.max(0,width-s.length()))+s;}
    public record Result(double[] samples,String input,String output){}
    public static Result process(double[] input,double span,GraySettings s){
        if(input.length!=512||!Double.isFinite(span)||span<=0)throw new IllegalArgumentException("gray_clock");
        double clock=s.levelInput()?s.sampleHz():s.bitRate();int symbols=(int)Math.ceil(span*clock-1e-9);
        if(symbols<1||symbols*(s.levelInput()?s.wordBits():1)>128)throw new IllegalArgumentException("gray_clock");
        String incoming,outgoing;
        if(s.levelInput()){
            StringBuilder indices=new StringBuilder(),encoded=new StringBuilder();
            for(int n=0;n<symbols;n++){
                int at=Math.min(511,(int)Math.ceil(n/clock/span*512-1e-9));double value=input[at];if(!Double.isFinite(value))throw new IllegalArgumentException("signal_range");
                int level=(int)Math.round(Math.max(0,Math.min(1,(value-s.minimum())/(s.maximum()-s.minimum())))*(s.levels()-1));
                if(n>0)indices.append(",");indices.append(level);encoded.append(word(s.mode().equals("levels_to_gray")?encode(level):level,s.wordBits()));
            }incoming=indices.toString();outgoing=encoded.toString();
        }else{
            StringBuilder bits=new StringBuilder();
            for(int n=0;n<symbols;n++){
                int begin=(int)Math.ceil(n/clock/span*512-1e-9),end=Math.min(512,(int)Math.ceil((n+1)/clock/span*512-1e-9));
                if(end<=begin)throw new IllegalArgumentException("gray_clock");double sum=0;for(int i=begin;i<end;i++)sum+=input[i];
                bits.append(sum/(end-begin)>s.threshold()?'1':'0');
            }incoming=bits.toString();outgoing=convert(incoming,s.wordBits(),s.mode().equals("gray_to_binary"));
        }
        double rate=s.levelInput()?clock*s.wordBits():clock;double[] output=new double[512];
        for(int i=0;i<512;i++){int bit=Math.min(outgoing.length()-1,(int)Math.floor(span*i/512*rate+1e-9));output[i]=outgoing.charAt(bit)=='1'?s.high():s.low();}
        return new Result(output,incoming,outgoing);
    }
}
