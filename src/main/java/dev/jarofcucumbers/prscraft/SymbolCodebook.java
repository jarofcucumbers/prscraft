package dev.jarofcucumbers.prscraft;
import java.util.*;
/** User-defined prefix-free Unicode scalar codes; never infers a table from the original text. */
public final class SymbolCodebook{
    public static final String DEFAULT="A = 0\nB = 10\nSPACE = 11";
    public static final class MissingSymbol extends IllegalArgumentException{public final int codepoint;MissingSymbol(int cp){super("codebook_symbol");codepoint=cp;}}
    private static final class Node{Node zero,one;int symbol=-1;}
    private final Map<Integer,String> codes;private final Node root;
    private SymbolCodebook(Map<Integer,String> codes,Node root){this.codes=Map.copyOf(codes);this.root=root;}
    public Map<Integer,String> codes(){return codes;}
    public static SymbolCodebook parse(String text){
        if(text==null||text.length()>8192)throw new IllegalArgumentException("codebook_size");var codes=new LinkedHashMap<Integer,String>();var root=new Node();
        for(String row:text.split("\\R")){row=row.strip();if(row.isEmpty()||row.startsWith("#"))continue;int separator=row.indexOf('=');if(separator<1)throw new IllegalArgumentException("codebook_syntax");String label=row.substring(0,separator).strip(),bits=row.substring(separator+1).strip();
            int cp;try{if(label.equals("SPACE")||label.equals("\\s"))cp=' ';else if(label.equals("\\n"))cp='\n';else if(label.equals("\\t"))cp='\t';else if(label.startsWith("U+"))cp=Integer.parseInt(label.substring(2),16);else{if(label.codePointCount(0,label.length())!=1)throw new IllegalArgumentException("codebook_syntax");cp=label.codePointAt(0);}}catch(NumberFormatException e){throw new IllegalArgumentException("codebook_syntax");}
            if(!Character.isValidCodePoint(cp)||cp>=0xd800&&cp<=0xdfff||!bits.matches("[01]{1,32}"))throw new IllegalArgumentException("codebook_syntax");if(codes.putIfAbsent(cp,bits)!=null)throw new IllegalArgumentException("codebook_duplicate");if(codes.size()>128)throw new IllegalArgumentException("codebook_size");
            Node node=root;for(char bit:bits.toCharArray()){if(node.symbol>=0)throw new IllegalArgumentException("codebook_prefix");if(bit=='0'){if(node.zero==null)node.zero=new Node();node=node.zero;}else{if(node.one==null)node.one=new Node();node=node.one;}}
            if(node.symbol>=0||node.zero!=null||node.one!=null)throw new IllegalArgumentException("codebook_prefix");node.symbol=cp;
        }if(codes.isEmpty())throw new IllegalArgumentException("codebook_size");return new SymbolCodebook(codes,root);
    }
    public net.minecraft.nbt.CompoundTag statistics(byte[] utf8){String text=MessageSettings.utf8(utf8);var counts=new HashMap<Integer,Integer>();text.codePoints().forEach(cp->counts.merge(cp,1,Integer::sum));int total=text.codePointCount(0,text.length());double entropy=0,average=0;for(var entry:counts.entrySet()){String code=codes.get(entry.getKey());if(code==null)throw new MissingSymbol(entry.getKey());double p=(double)entry.getValue()/total;entropy-=p*Math.log(p)/Math.log(2);average+=p*code.length();}var t=new net.minecraft.nbt.CompoundTag();t.putDouble("entropy",entropy);t.putString("entropyUnit","symbol");t.putDouble("averageCodeLength",average);return t;}
    public BitStream encode(byte[] utf8){String text=MessageSettings.utf8(utf8);var writer=new BitStream.Writer();for(int cp:text.codePoints().toArray()){String code=codes.get(cp);if(code==null)throw new MissingSymbol(cp);for(char bit:code.toCharArray())writer.bit(bit=='1');}return writer.finish();}
    public byte[] decode(BitStream stream){StringBuilder out=new StringBuilder();Node node=root;int utf8Bytes=0;for(int i=0;i<stream.bitLength();i++){node=stream.bit(i)?node.one:node.zero;if(node==null)throw new IllegalArgumentException("codebook_bits");if(node.symbol>=0){int cp=node.symbol;utf8Bytes+=cp<128?1:cp<2048?2:cp<65536?3:4;if(utf8Bytes>BitStream.MAX_BYTES)throw new IllegalArgumentException("data_size");out.appendCodePoint(cp);node=root;}}
        if(node!=root)throw new IllegalArgumentException("codebook_truncated");return out.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }
}
