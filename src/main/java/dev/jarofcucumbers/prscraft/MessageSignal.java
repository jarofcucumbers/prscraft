package dev.jarofcucumbers.prscraft;
import java.util.*;
import net.minecraft.nbt.*;
/** One noisy decision-level value per transmitted NRZ bit, independent of the 512-point preview. */
public final class MessageSignal{
    public final double[] values;public final BitStream reference;public final byte[] original;public final double rate;public final ListTag stages;public final int correctedWords;public final boolean customCoded;
    private MessageSignal(double[] values,BitStream reference,byte[] original,double rate,ListTag stages,int correctedWords,boolean customCoded){this.values=values;this.reference=reference;this.original=original;this.rate=rate;this.stages=stages;this.correctedWords=correctedWords;this.customCoded=customCoded;}
    public static MessageSignal source(MessageSettings settings,boolean enabled,double powerGain){return source(settings,enabled,powerGain,null);}
    public static MessageSignal source(MessageSettings settings,boolean enabled,double powerGain,SymbolCodebook book){byte[] bytes=settings.sourceBytes();BitStream stream=book==null?BitStream.of(bytes):book.encode(bytes);var trace=new ListTag();if(book!=null){var stats=MessageProcessing.apply(BitStream.of(bytes),"custom_encode",book).statistics();stats.putString("kind","message_source");trace.add(stats);}return new MessageSignal(levels(stream,settings,enabled?powerGain:0),stream,bytes,settings.bitRate(),trace,0,book!=null);}
    private static double[] levels(BitStream stream,MessageSettings settings,double gain){double[] values=new double[stream.bitLength()];for(int i=0;i<values.length;i++)values[i]=(stream.bit(i)?settings.one():settings.zero())*gain;return values;}
    public MessageSignal channel(double gain,double mean,double sigma,long seed,boolean clip){double[] output=values.clone();Random random=sigma==0?null:new Random(seed);for(int i=0;i<output.length;i++){output[i]=output[i]*gain+mean+(random==null?0:sigma*random.nextGaussian());if(clip)output[i]=Math.max(-16,Math.min(16,output[i]));}return new MessageSignal(output,reference,original,rate,stages,correctedWords,customCoded);}
    public BitStream observed(double threshold){var writer=new BitStream.Writer();for(double value:values){if(!Double.isFinite(value))throw new IllegalArgumentException("signal_range");writer.bit(value>threshold);}return writer.finish();}
    public int bitErrors(double threshold){int errors=0;for(int i=0;i<values.length;i++)if((values[i]>threshold)!=reference.bit(i))errors++;return errors;}
    public MessageSignal process(MessageSettings settings,String kind){return process(settings,kind,null);}
    public MessageSignal process(MessageSettings settings,String kind,SymbolCodebook book){
        if(Math.abs(settings.bitRate()-rate)>1e-9)throw new IllegalArgumentException("data_clock");var input=observed(settings.threshold());var result=MessageProcessing.apply(input,settings.operation(),book);var trace=stages.copy();var stats=result.statistics();stats.putString("kind",kind);stats.putInt("inputBitErrors",bitErrors(settings.threshold()));trace.add(stats);
        return new MessageSignal(levels(result.stream(),settings,1),result.stream(),original,rate,trace,correctedWords+result.correctedWords(),settings.operation().equals("custom_encode")||!settings.operation().equals("custom_decode")&&customCoded);
    }
    public int offset(int requested){return Math.min(requested,Math.max(0,values.length-1));}
    public int windowBits(int requested){return Math.min(128,Math.max(1,values.length-offset(requested)));}
    public double[] preview(int requested){return previewWindow(offset(requested),windowBits(requested));}
    public double[] previewWindow(int start,int count){double[] output=new double[512];for(int i=0;i<512;i++){int bit=start+i*count/512;output[i]=bit<values.length?values[bit]:0;}return output;}
    public void describe(CompoundTag data,MessageSettings settings,boolean receiver){describe(data,settings,receiver,null);}
    public void describe(CompoundTag data,MessageSettings settings,boolean receiver,SymbolCodebook book){
        data.putBoolean("messageCustom",customCoded);data.putBoolean("messageCodebookMissing",receiver&&customCoded&&book==null);
        data.putBoolean("dataStream",true);data.putBoolean("messageSink",receiver);data.putDouble("messageRate",rate);data.putInt("messageBitCount",values.length);data.putDouble("messageDuration",values.length/rate);data.putInt("messageOffset",offset(settings.previewOffset()));data.putInt("messageWindow",windowBits(settings.previewOffset()));
        data.putByteArray("messageOriginal",original);data.putDouble("messageEntropy",SourceCompression.entropy(original));data.putInt("messageOriginalBytes",original.length);data.put("messageStages",stages.copy());data.putInt("messageCorrectedWords",correctedWords);
        BitStream wire=observed(settings.threshold());data.putByteArray("messageWire",wire.bytes());data.putInt("messageWireBits",wire.bitLength());data.putString("messageBits",wire.preview(offset(settings.previewOffset()),128));int errors=bitErrors(settings.threshold());data.putInt("messageBitErrors",errors);data.putDouble("messageBer",values.length==0?0:(double)errors/values.length);
        try{
            if(receiver&&Math.abs(settings.bitRate()-rate)>1e-9)throw new IllegalArgumentException("data_clock");
            BitStream result=wire;if(receiver){var processed=MessageProcessing.apply(wire,settings.operation(),book);result=processed.stream();data.putInt("messageCorrectedWords",correctedWords+processed.correctedWords());var trace=data.getList("messageStages",10);var stats=processed.statistics();stats.putString("kind","receiver");stats.putInt("inputBitErrors",errors);trace.add(stats);}
            boolean customOutput=customCoded;if(receiver&&book!=null){result=BitStream.of(book.decode(result));customOutput=false;}
            data.putByteArray("messageBytes",result.bytes());data.putInt("messageResultBits",result.bitLength());data.putBoolean("messageAligned",result.bitLength()%8==0);
            if(result.bitLength()%8==0&&!customOutput){byte[] bytes=result.aligned();data.putBoolean("messageMatch",Arrays.equals(bytes,original));try{data.putString("messageText",MessageSettings.utf8(bytes));data.putBoolean("messageUtf8",true);}catch(IllegalArgumentException e){data.putBoolean("messageUtf8",false);}}
            data.putBoolean("messageProcessed",true);
        }catch(IllegalArgumentException e){data.putString("messageError",e.getMessage());data.putByteArray("messageBytes",wire.bytes());data.putInt("messageResultBits",wire.bitLength());}
    }
}
