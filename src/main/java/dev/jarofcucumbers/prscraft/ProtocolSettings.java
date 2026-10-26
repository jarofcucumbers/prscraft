package dev.jarofcucumbers.prscraft;
import net.minecraft.nbt.CompoundTag;
/** Fixed slot size is pre-agreed for this finite synchronous link, not inferred from source text. */
public record ProtocolSettings(int payloadBytes,int retries,double ackLoss,double timeout){
    public static final ProtocolSettings DEFAULT=new ProtocolSettings(32,3,0,.05);
    public ProtocolSettings{if(payloadBytes<16||payloadBytes>128||retries<0||retries>8||!Double.isFinite(ackLoss)||ackLoss<0||ackLoss>1||!Double.isFinite(timeout)||timeout<.001||timeout>10)throw new IllegalArgumentException("protocol");}
    public CompoundTag save(){var t=new CompoundTag();t.putInt("payload",payloadBytes);t.putInt("retries",retries);t.putDouble("ackLoss",ackLoss);t.putDouble("timeout",timeout);return t;}
    public static ProtocolSettings read(CompoundTag t){return new ProtocolSettings(t.getInt("payload"),t.getInt("retries"),t.getDouble("ackLoss"),t.getDouble("timeout"));}
}
