package dev.jarofcucumbers.prscraft.client;
import dev.jarofcucumbers.prscraft.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.*;
public final class MessageScreen extends PanelScreen{
    private final net.minecraft.client.gui.screens.Screen parent;private final BlockPos pos;private final String kind;private final boolean recorded,source;private CompoundTag data;private MessageSettings saved;private String operation,inputMode,error="";private boolean enabled,thermal,hexView;private int ticks;
    private EditBox rate,threshold,zero,one,carrier,power,mean,variance,offset;private MessageTextBox text;
    public MessageScreen(BlockPos pos,CompoundTag data){this(null,pos,data);}
    public MessageScreen(net.minecraft.client.gui.screens.Screen parent,BlockPos pos,CompoundTag data){super(Component.translatable("screen.prscraft.message_lab"),860,620);this.parent=parent;this.pos=pos;this.data=data;recorded=data.getBoolean("recorded")||data.getBoolean("analysis");kind=data.getBoolean("recorded")?data.getString("captureKind"):data.getString("kind");source=kind.equals("message_source");saved=MessageSettings.read(data.getCompound("message"));operation=saved.operation();inputMode=saved.inputMode();var radio=RadioSettings.read(data.getCompound("settings"));enabled=radio.enabled();thermal=radio.thermal();hexView=!data.getBoolean("messageUtf8");}
    public BlockPos pos(){return pos;}
    public void receive(CompoundTag next){if(recorded)return;data=next;saved=MessageSettings.read(next.getCompound("message"));if(!next.getString("error").isEmpty())error=next.getString("error");refreshOutput();}
    private String[] operations(){return kind.equals("digital_modem")?new String[]{"qpsk_encode","qpsk_decode","qam16_encode","qam16_decode"}:java.util.Set.of("packet_sender","packet_receiver").contains(kind)?new String[]{"raw"}:kind.equals("message_codec")?new String[]{"raw","rle","huffman","decode","custom_encode","custom_decode"}:kind.equals("channel_coder")?new String[]{"raw","crc_encode","crc_decode","hamming_encode","hamming_decode"}:new String[]{"raw","decode","crc_decode","hamming_decode"};}
    private EditBox box(int x,int y,int width,double value,EditBox old,String key){return field(x,y,width,old==null?Double.toString(value):old.getValue(),18,key);}
    @Override protected void build(){
        var radio=RadioSettings.read(data.getCompound("settings"));
        if(source)button(14,42,228,tr(data.getBoolean("codebookPresent")?"message_source_custom":"message_source"),b->{}).active=false;
        else button(14,42,228,tr("message_op_"+operation),b->{var options=operations();int index=Arrays.asList(options).indexOf(operation);operation=options[(index+1)%options.length];hexView=java.util.Set.of("rle","huffman","crc_encode","hamming_encode").contains(operation);refreshOutput();b.setMessage(tr("message_op_"+operation));});
        button(254,42,140,tr(source?"message_input_"+inputMode:hexView?"message_hex":"message_utf8"),b->{if(source){inputMode=inputMode.equals("utf8")?"hex":"utf8";b.setMessage(tr("message_input_"+inputMode));}else{hexView=!hexView;b.setMessage(tr(hexView?"message_hex":"message_utf8"));refreshOutput();}});
        button(406,42,90,Component.literal(enabled?"ON":"OFF"),b->{enabled=!enabled;b.setMessage(Component.literal(enabled?"ON":"OFF"));});button(508,42,150,tr("apply"),b->save());button(670,42,176,tr("media"),b->{var t=new CompoundTag();t.putString("action","media");RadioClient.request(new RadioNetwork.Request(pos,false,t));});
        rate=box(14,94,150,saved.bitRate(),rate,"bit_rate");threshold=box(180,94,150,saved.threshold(),threshold,"gray_threshold");zero=box(346,94,150,saved.zero(),zero,"gray_low");one=box(512,94,150,saved.one(),one,"gray_high");carrier=box(678,94,168,radio.carrierMHz(),carrier,"carrier");
        power=box(14,140,150,radio.powerDbm(),power,"power");mean=box(180,140,150,radio.lab().noiseMean(),mean,"noise_mean");variance=box(346,140,150,radio.lab().noiseVariance(),variance,"noise_variance");
        button(512,140,150,tr(thermal?"message_thermal_on":"message_thermal_off"),b->{thermal=!thermal;b.setMessage(tr(thermal?"message_thermal_on":"message_thermal_off"));});offset=box(678,140,168,saved.previewOffset(),offset,"message_offset");
        boolean receiver=kind.equals("message_receiver")||kind.equals("packet_receiver")||kind.equals("radio");zero.active=one.active=!receiver;power.active=!receiver;carrier.active=source||receiver;
        String previous=text==null?saved.text():text.getValue();text=addRenderableWidget(new MessageTextBox(font,14,188,832,72,tr("message_text"),!source||recorded));text.setValue(source?previous:"");if(source)text.setCharacterLimit(4096);refreshOutput();
        button(14,580,80,tr("back"),b->onClose());button(106,580,140,tr("message_waveform"),b->minecraft.setScreen(new PlotScreen(this,data)));button(258,580,140,tr("message_statistics"),b->minecraft.setScreen(new MessageStatsScreen(this,data)));button(410,580,130,tr("message_export"),b->RadioClient.exportMessage(data));if(kind.startsWith("packet_"))button(552,580,140,tr("protocol"),b->minecraft.setScreen(new ProtocolScreen(this,pos,data)));else button(552,580,140,tr("constellation"),b->minecraft.setScreen(new ConstellationScreen(this,data))).active=data.contains("iqMode");button(704,580,140,tr("book"),b->minecraft.setScreen(new HandbookScreen(this)));
        if(recorded)for(var r:renderables)if(r instanceof AbstractWidget w&&w!=text&&w.getY()<180&&(source||w.getX()!=254))w.active=false;
    }
    private void refreshOutput(){if(text==null||source)return;String value=hexView?java.util.HexFormat.ofDelimiter(" ").formatHex(data.getByteArray("messageBytes")):data.getBoolean("messageUtf8")?data.getString("messageText"):tr("message_not_utf8").getString();if(!text.getValue().equals(value))text.setValue(value);}
    private void save(){try{
        var next=new MessageSettings(inputMode,source?text.getValue():saved.text(),source?"raw":operation,number(rate),number(threshold),number(zero),number(one),(int)number(offset));
        if(number(offset)!=(int)number(offset))throw new IllegalArgumentException("data_settings");
        var tag=data.getCompound("settings").copy();tag.putBoolean("enabled",enabled);tag.putDouble("carrierMHz",number(carrier));tag.putDouble("powerDbm",number(power));tag.putBoolean("thermal",thermal);var lab=tag.getCompound("lab").copy();lab.putDouble("mean",number(mean));lab.putDouble("variance",number(variance));tag.put("lab",lab);tag.put("message",next.save());RadioSettings.read(tag);RadioClient.request(new RadioNetwork.Request(pos,true,tag));error="";
    }catch(NumberFormatException e){error="number";}catch(IllegalArgumentException e){error=e.getMessage();}}
    @Override public void onClose(){RadioClient.closeDevice(parent);}
    @Override public void tick(){if(recorded)return;if(minecraft.player==null||!RadioClient.inRange(pos)){onClose();return;}if(++ticks%20==0)RadioClient.request(new RadioNetwork.Request(pos,false,new CompoundTag()));}
    @Override protected void draw(GuiGraphics g){
        String[] keys={"bit_rate","gray_threshold","gray_low","gray_high","carrier","power","noise_mean","noise_variance","message_offset"};int[] xs={14,180,346,512,678,14,180,346,678};for(int i=0;i<keys.length;i++)label(g,keys[i],xs[i],i<5?80:126);
        label(g,source?"message_input_text":"message_received_text",14,174);String problem=!error.isEmpty()?error:data.getString("messageError");if(problem.isEmpty())problem=data.getString("fault");
        if(!problem.isEmpty())g.drawString(font,Component.translatable("error.prscraft."+problem).append(" "+data.getString("messageDetail")),14,272,0xffffa47b,false);
        else if(data.getBoolean("messageCodebookMissing"))label(g,"message_codebook_missing",14,272);
        else if(data.getBoolean("messageSink"))g.drawString(font,tr(data.getBoolean("messageMatch")?"message_match":"message_mismatch"),14,272,data.getBoolean("messageMatch")?0xff83e4ac:0xffffc574,false);
        else label(g,"message_model",14,272);
        g.drawString(font,"UTF-8/source: "+data.getInt("messageOriginalBytes")+" B · H="+fmt(data.getDouble("messageEntropy"))+" bit/byte",14,296,0xffc6d9e6,false);
        g.drawString(font,"wire: "+data.getInt("messageWireBits")+" bit · Rb="+fmt(data.getDouble("messageRate"))+" bit/s · T="+fmt(data.getDouble("messageDuration"))+" s",14,318,0xffc6d9e6,false);
        g.drawString(font,"result: "+data.getInt("messageResultBits")+" bit · BER(last segment)="+fmt(data.getDouble("messageBer"))+" · corrected words="+data.getInt("messageCorrectedWords"),14,340,0xffc6d9e6,false);
        var stages=data.getList("messageStages",10);if(!stages.isEmpty()){var stage=stages.getCompound(stages.size()-1);g.drawString(font,stage.getString("operation")+": "+stage.getInt("inputBits")+" → "+stage.getInt("outputBits")+" bit · header="+stage.getInt("headerBytes")+" B",14,364,0xff9bd9bd,false);}
        g.fill(14,396,846,528,0xff0a1420);double[] values=SignalChain.unpack(data.getIntArray("received"));double range=1;for(double v:values)range=Math.max(range,Math.abs(v)*1.1);if(values.length==512){int last=462;for(int i=0;i<832;i++){int y=462-(int)(values[i*512/832]/range*59);if(i>0)line(g,13+i,last,14+i,y,0xff65dfaf);last=y;}}
        g.drawString(font,font.plainSubstrByWidth(data.getString("messageBits"),830),14,540,0xffd8e8bc,false);g.drawString(font,Component.translatable("screen.prscraft.message_preview",data.getInt("messageOffset"),data.getInt("messageWindow")),14,558,0xffa9cbdc,false);
    }
}
