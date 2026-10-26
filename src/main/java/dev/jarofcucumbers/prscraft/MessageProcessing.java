package dev.jarofcucumbers.prscraft;
import net.minecraft.nbt.CompoundTag;
public final class MessageProcessing{
    private MessageProcessing(){}
    public record Result(BitStream stream,CompoundTag statistics,int correctedWords){}
    public static Result apply(BitStream input,String operation){return apply(input,operation,null);}
    public static Result apply(BitStream input,String operation,SymbolCodebook book){
        var stats=new CompoundTag();stats.putString("operation",operation);stats.putInt("inputBits",input.bitLength());stats.putInt("payloadBits",input.bitLength());BitStream output=input;int corrected=0;
        switch(operation){
            case "raw"->{}
            case "custom_encode"->{if(book==null)throw new IllegalArgumentException("codebook_missing");output=book.encode(input.aligned());stats.merge(book.statistics(input.aligned()));}
            case "custom_decode"->{if(book==null)throw new IllegalArgumentException("codebook_missing");output=BitStream.of(book.decode(input));}
            case "rle","huffman"->{var encoded=SourceCompression.encode(input.aligned(),operation);output=BitStream.of(encoded.bytes());stats.putInt("headerBytes",encoded.headerBytes());stats.putInt("payloadBits",encoded.payloadBits());stats.putDouble("entropy",encoded.entropy());stats.putDouble("averageCodeLength",encoded.averageCodeLength());}
            case "decode"->output=BitStream.of(SourceCompression.decode(input.aligned()));
            case "crc_encode"->output=ChannelCoding.crcEncode(input);
            case "crc_decode"->{output=ChannelCoding.crcDecode(input);stats.putBoolean("crcValid",true);}
            case "hamming_encode"->output=ChannelCoding.hammingEncode(input);
            case "hamming_decode"->{var decoded=ChannelCoding.hammingDecode(input);output=decoded.stream();corrected=decoded.correctedWords();stats.putInt("correctedWords",corrected);}
            default->throw new IllegalArgumentException("data_settings");
        }stats.putInt("outputBits",output.bitLength());stats.putInt("overheadBits",output.bitLength()-input.bitLength());return new Result(output,stats,corrected);
    }
}
