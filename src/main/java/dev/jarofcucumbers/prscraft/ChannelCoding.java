package dev.jarofcucumbers.prscraft;
import java.util.*;
/** CRC-32 detects; Hamming(7,4) corrects one bit per word, without double-error guarantees. */
public final class ChannelCoding{
    private ChannelCoding(){}
    public record Decoded(BitStream stream,int correctedWords){}
    public static BitStream crcEncode(BitStream input){byte[] data=input.aligned();if(data.length+4>BitStream.MAX_BYTES)throw new IllegalArgumentException("data_size");var crc=new java.util.zip.CRC32();crc.update(data);byte[] output=Arrays.copyOf(data,data.length+4);long sum=crc.getValue();for(int i=0;i<4;i++)output[data.length+i]=(byte)(sum>>>(24-i*8));return BitStream.of(output);}
    public static BitStream crcDecode(BitStream input){byte[] data=input.aligned();if(data.length<4)throw new IllegalArgumentException("data_crc");int count=data.length-4;long expected=0;for(int i=count;i<data.length;i++)expected=(expected<<8)|(data[i]&255);var crc=new java.util.zip.CRC32();crc.update(data,0,count);if(crc.getValue()!=expected)throw new IllegalArgumentException("data_crc");return BitStream.of(Arrays.copyOf(data,count));}
    public static BitStream hammingEncode(BitStream input){byte[] data=input.aligned();var writer=new BitStream.Writer();for(byte b:data)for(int shift:new int[]{4,0}){int n=(b>>>shift)&15,d1=(n>>>3)&1,d2=(n>>>2)&1,d3=(n>>>1)&1,d4=n&1;for(int bit:new int[]{d1^d2^d4,d1^d3^d4,d1,d2^d3^d4,d2,d3,d4})writer.bit(bit!=0);}return writer.finish();}
    public static Decoded hammingDecode(BitStream input){if(input.bitLength()%14!=0)throw new IllegalArgumentException("data_alignment");var writer=new BitStream.Writer();int corrected=0;for(int offset=0;offset<input.bitLength();offset+=7){boolean[] word=new boolean[8];int syndrome=0;for(int i=1;i<=7;i++){word[i]=input.bit(offset+i-1);if(word[i])syndrome^=i;}if(syndrome!=0){word[syndrome]=!word[syndrome];corrected++;}for(int i:new int[]{3,5,6,7})writer.bit(word[i]);}return new Decoded(writer.finish(),corrected);}
}
