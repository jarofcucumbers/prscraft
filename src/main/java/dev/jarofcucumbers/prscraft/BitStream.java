package dev.jarofcucumbers.prscraft;
import java.util.*;
/** MSB-first finite bit stream. Length/clock are known by the simulation, not packet framing. */
public record BitStream(byte[] bytes,int bitLength){
    public static final int MAX_BYTES=4096;
    public BitStream{if(bytes==null||bitLength<0||bitLength>MAX_BYTES*8||bytes.length!=(bitLength+7)/8)throw new IllegalArgumentException("data_size");bytes=bytes.clone();if(bitLength%8!=0&&bytes.length>0)bytes[bytes.length-1]&=(byte)(0xff<<(8-bitLength%8));}
    @Override public byte[] bytes(){return bytes.clone();}
    public static BitStream of(byte[] bytes){return new BitStream(bytes,bytes.length*8);}
    public boolean bit(int i){if(i<0||i>=bitLength)throw new IllegalArgumentException("data_size");return (bytes[i/8]&(1<<(7-i%8)))!=0;}
    public byte[] aligned(){if(bitLength%8!=0)throw new IllegalArgumentException("data_alignment");return bytes();}
    public String preview(int offset,int count){StringBuilder s=new StringBuilder();for(int i=Math.max(0,offset);i<Math.min(bitLength,offset+count);i++)s.append(bit(i)?'1':'0');return s.toString();}
    public static final class Writer{
        private final java.io.ByteArrayOutputStream output=new java.io.ByteArrayOutputStream();private int current,count,length;
        public void bit(boolean one){if(length>=MAX_BYTES*8)throw new IllegalArgumentException("data_size");current=(current<<1)|(one?1:0);length++;if(++count==8){output.write(current);current=count=0;}}
        public void number(int number,int bits){for(int i=bits-1;i>=0;i--)bit((number&(1<<i))!=0);}
        public BitStream finish(){byte[] full=output.toByteArray();if(count>0){full=Arrays.copyOf(full,full.length+1);full[full.length-1]=(byte)(current<<(8-count));}return new BitStream(full,length);}
    }
}
