package dev.jarofcucumbers.prscraft;
import java.util.*;
import java.io.*;
/** Self-contained educational compression container. Dictionary/header bytes are transmitted and counted. */
public final class SourceCompression{
    private SourceCompression(){}
    private static final int MAGIC=0x50524338;
    public record Encoded(byte[] bytes,int payloadBits,int headerBytes,double entropy,double averageCodeLength){}
    private record Node(int frequency,int minimum,int symbol,Node left,Node right){}
    public static int[] frequencies(byte[] input){int[] count=new int[256];for(byte b:input)count[b&255]++;return count;}
    public static String[] codeWords(byte[] bytes){var result=new String[256];codes(tree(frequencies(bytes)),"",result);return result;}
    public static double entropy(byte[] input){double h=0;for(int count:frequencies(input))if(count>0){double p=(double)count/input.length;h-=p*Math.log(p)/Math.log(2);}return h;}
    private static Node tree(int[] counts){var queue=new PriorityQueue<Node>(Comparator.comparingInt(Node::frequency).thenComparingInt(Node::minimum));for(int i=0;i<256;i++)if(counts[i]>0)queue.add(new Node(counts[i],i,i,null,null));while(queue.size()>1){var a=queue.remove();var b=queue.remove();queue.add(new Node(a.frequency+b.frequency,Math.min(a.minimum,b.minimum),-1,a,b));}return queue.poll();}
    private static void codes(Node n,String prefix,String[] result){if(n==null)return;if(n.symbol>=0){result[n.symbol]=prefix.isEmpty()?"0":prefix;return;}codes(n.left,prefix+"0",result);codes(n.right,prefix+"1",result);}
    public static Encoded encode(byte[] input,String algorithm){
        if(input.length>BitStream.MAX_BYTES)throw new IllegalArgumentException("data_size");if(!Set.of("rle","huffman").contains(algorithm))throw new IllegalArgumentException("data_settings");
        try{byte[] body;int bits;int[] counts=frequencies(input);double average=0;
            if(algorithm.equals("rle")){var runs=new ByteArrayOutputStream();for(int i=0;i<input.length;){int end=i+1;while(end<input.length&&input[end]==input[i]&&end-i<255)end++;runs.write(end-i);runs.write(input[i]);i=end;}body=runs.toByteArray();bits=body.length*8;}
            else{String[] codes=new String[256];codes(tree(counts),"",codes);var writer=new BitStream.Writer();for(byte b:input)for(char bit:codes[b&255].toCharArray())writer.bit(bit=='1');var stream=writer.finish();body=stream.bytes();bits=stream.bitLength();average=input.length==0?0:(double)bits/input.length;}
            var bytes=new ByteArrayOutputStream();var out=new DataOutputStream(bytes);out.writeInt(MAGIC);out.writeByte(algorithm.equals("rle")?1:2);out.writeShort(input.length);out.writeInt(bits);
            if(algorithm.equals("huffman")){out.writeShort((int)Arrays.stream(counts).filter(n->n>0).count());for(int i=0;i<256;i++)if(counts[i]>0){out.writeByte(i);out.writeInt(counts[i]);}}
            int header=bytes.size();out.write(body);if(bytes.size()>BitStream.MAX_BYTES)throw new IllegalArgumentException("data_size");return new Encoded(bytes.toByteArray(),bits,header,entropy(input),average);
        }catch(IOException e){throw new IllegalStateException(e);}
    }
    public static byte[] decode(byte[] container){
        if(container.length>BitStream.MAX_BYTES)throw new IllegalArgumentException("data_size");
        try{var in=new DataInputStream(new ByteArrayInputStream(container));if(in.readInt()!=MAGIC)throw new IllegalArgumentException("data_container");int algorithm=in.readUnsignedByte(),length=in.readUnsignedShort(),bits=in.readInt();if(length>BitStream.MAX_BYTES||bits<0||bits>BitStream.MAX_BYTES*8)throw new IllegalArgumentException("data_container");
            int[] counts=new int[256];if(algorithm==2){int symbols=in.readUnsignedShort();if(symbols>256)throw new IllegalArgumentException("data_container");long total=0;for(int i=0;i<symbols;i++){int symbol=in.readUnsignedByte(),count=in.readInt();if(count<=0||count>length||counts[symbol]!=0)throw new IllegalArgumentException("data_container");counts[symbol]=count;total+=count;}if(total!=length||(length==0)!=(symbols==0))throw new IllegalArgumentException("data_container");}
            byte[] payload=in.readAllBytes();if(payload.length!=(bits+7)/8||bits%8!=0&&payload.length>0&&(payload[payload.length-1]&((1<<(8-bits%8))-1))!=0)throw new IllegalArgumentException("data_container");
            byte[] result=new byte[length];
            if(algorithm==1){if(bits%16!=0)throw new IllegalArgumentException("data_container");int at=0;for(int i=0;i<payload.length;i+=2){int count=payload[i]&255;if(count==0||count>length-at)throw new IllegalArgumentException("data_container");Arrays.fill(result,at,at+count,payload[i+1]);at+=count;}if(at!=length)throw new IllegalArgumentException("data_container");}
            else if(algorithm==2){var root=tree(counts);var stream=new BitStream(payload,bits);int at=0;for(int i=0;i<length;i++){Node n=root;if(n.symbol>=0){if(at>=bits||stream.bit(at++))throw new IllegalArgumentException("data_container");}else while(n.symbol<0){if(at>=bits)throw new IllegalArgumentException("data_container");n=stream.bit(at++)?n.right:n.left;}result[i]=(byte)n.symbol;}if(at!=bits||!Arrays.equals(counts,frequencies(result)))throw new IllegalArgumentException("data_container");}
            else throw new IllegalArgumentException("data_container");return result;
        }catch(IOException|IndexOutOfBoundsException e){throw new IllegalArgumentException("data_container");}
    }
}
