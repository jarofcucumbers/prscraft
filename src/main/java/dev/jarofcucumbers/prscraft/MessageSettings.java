package dev.jarofcucumbers.prscraft;
import net.minecraft.nbt.CompoundTag;
import java.nio.*;
import java.nio.charset.*;
public record MessageSettings(String inputMode,String text,String operation,double bitRate,double threshold,double zero,double one,int previewOffset){
    public static final int MAX_TEXT_BYTES=1024;
    public static final MessageSettings DEFAULT=new MessageSettings("utf8","Привіт, Minecraft! AAAAAAAAAAAAAAAA","raw",1000,0,-1,1,0);
    public MessageSettings{
        if(!java.util.Set.of("utf8","hex").contains(inputMode)||!java.util.Set.of("raw","rle","huffman","decode","crc_encode","crc_decode","hamming_encode","hamming_decode","custom_encode","custom_decode").contains(operation))throw new IllegalArgumentException("data_settings");
        sourceBytes(inputMode,text);
        for(double value:new double[]{bitRate,threshold,zero,one})if(!Double.isFinite(value))throw new IllegalArgumentException("data_settings");
        if(bitRate<1||bitRate>20000||zero>=one||zero < -100||one>100||Math.abs(threshold)>100||previewOffset<0||previewOffset>=BitStream.MAX_BYTES*8)throw new IllegalArgumentException("data_settings");
    }
    public MessageSettings operation(String value){return new MessageSettings(inputMode,text,value,bitRate,threshold,zero,one,previewOffset);}
    public byte[] sourceBytes(){return sourceBytes(inputMode,text);}
    public static byte[] sourceBytes(String mode,String text){
        if(text==null||text.length()>4096)throw new IllegalArgumentException("data_size");
        try{byte[] bytes;if(mode.equals("hex")){String compact=text.replaceAll("\\s+","");if(compact.length()%2!=0||!compact.matches("[0-9a-fA-F]*"))throw new IllegalArgumentException("data_hex");bytes=java.util.HexFormat.of().parseHex(compact);}else{ByteBuffer buffer=StandardCharsets.UTF_8.newEncoder().onMalformedInput(CodingErrorAction.REPORT).encode(CharBuffer.wrap(text));bytes=new byte[buffer.remaining()];buffer.get(bytes);}
            if(bytes.length>MAX_TEXT_BYTES)throw new IllegalArgumentException("data_size");return bytes;
        }catch(CharacterCodingException e){throw new IllegalArgumentException("data_utf8");}
    }
    public static String utf8(byte[] bytes){try{return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();}catch(CharacterCodingException e){throw new IllegalArgumentException("data_utf8");}}
    public CompoundTag save(){var t=new CompoundTag();t.putString("inputMode",inputMode);t.putString("text",text);t.putString("operation",operation);t.putDouble("bitRate",bitRate);t.putDouble("threshold",threshold);t.putDouble("zero",zero);t.putDouble("one",one);t.putInt("offset",previewOffset);return t;}
    public static MessageSettings read(CompoundTag t){return new MessageSettings(t.getString("inputMode"),t.getString("text"),t.getString("operation"),t.getDouble("bitRate"),t.getDouble("threshold"),t.getDouble("zero"),t.getDouble("one"),t.getInt("offset"));}
}
