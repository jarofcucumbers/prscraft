package dev.jarofcucumbers.prscraft;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import java.util.*;
public final class TemplateItem extends Item {
    public TemplateItem(Properties properties){super(properties);}
    public static String summary(String kind,RadioSettings s,ProcessorSettings p){
        return switch(kind){
            case "radio" -> {
                String signal=s.signalMode().equals("bits")?(s.lab().randomBits()?"P1="+s.lab().probabilityOne():s.bits()):s.signalMode().equals("discrete")?"x[n]="+s.lab().samples():s.expression();
                if(signal.length()>22)signal=signal.substring(0,22)+"…";
                yield s.transmit()?"TX · "+signal+" · "+s.carrierMHz()+" MHz":"RX · "+s.carrierMHz()+" MHz";
            }
            case "digital_modem" -> "IQ · QPSK/16-QAM";case "packet_sender" -> "FRM · packet TX";case "packet_receiver" -> "ARQ · packet RX";case "orthogonality_meter" -> "ORTH · <A,B>";case "message_source" -> "TXT · UTF-8";case "message_codec" -> "ZIP · RLE/Huffman";case "channel_coder" -> "FEC · CRC/Hamming";case "message_receiver" -> "RX · data";
            case "gray_converter" -> "GRAY · Binary ↔ Gray";
            case "bit_generator" -> "BITS · P1="+s.lab().probabilityOne()+" · "+s.bitRate()+" bit/s";
            case "amplifier" -> "AMP · "+p.gainDb()+" dB";
            case "frequency_filter" -> "FILTER · "+p.lowHz()+"–"+p.highHz()+" Hz";
            case "modulator" -> p.modulation().toUpperCase(Locale.ROOT)+" · "+p.oscillatorHz()+" Hz";
            default -> kind;
        };
    }
    public static String summary(String kind,RadioSettings s,ProcessorSettings p,DspSettings dsp){return switch(kind){
        case "adder" -> "SUM · "+dsp.gainA()+"A + "+dsp.gainB()+"B";
        case "integrator" -> "INT · T="+dsp.endSeconds()+" s";
        case "correlator" -> "COR · lag="+dsp.lagSeconds()+" s";
        case "time_gate" -> "GATE · T="+dsp.gateSeconds()+" s";
        case "delay_line" -> "DELAY · "+dsp.delaySeconds()+" s";
        default -> summary(kind,s,p);
    };}
    public static int model(String kind,RadioSettings s){return switch(kind){case "radio"->s.transmit()?1:2;case "amplifier"->3;case "frequency_filter"->4;case "modulator"->5;case "bit_generator"->6;case "adder"->7;case "integrator"->8;case "correlator"->9;case "time_gate"->10;case "delay_line"->11;case "gray_converter"->12;case "digital_modem"->17;case "packet_sender"->18;case "packet_receiver"->19;case "orthogonality_meter"->20;case "message_source"->13;case "message_codec"->14;case "channel_coder"->15;case "message_receiver"->16;default->0;};}
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> lines,TooltipFlag flags){
        CustomData data=stack.get(DataComponents.CUSTOM_DATA);if(data==null){lines.add(Component.translatable("tooltip.prscraft.blank_template").withStyle(ChatFormatting.GRAY));return;}
        try{var tag=data.copyTag();var s=RadioSettings.read(tag.getCompound("settings"));var p=ProcessorSettings.read(tag.getCompound("processor"));
            lines.add(Component.literal(summary(tag.getString("kind"),s,p,tag.contains("dsp")?DspSettings.read(tag.getCompound("dsp")):DspSettings.DEFAULT)).withStyle(ChatFormatting.AQUA));
            String kind=tag.getString("kind");
            if(kind.equals("gray_converter")&&tag.contains("gray")){var gray=GraySettings.read(tag.getCompound("gray"));lines.add(Component.literal(gray.mode()+" · "+gray.wordBits()+" bit · "+(gray.levelInput()?gray.sampleHz()+" Hz · L="+gray.levels():gray.bitRate()+" bit/s")).withStyle(ChatFormatting.GRAY));}
            if(tag.contains("message")&&java.util.Set.of("message_source","message_codec","channel_coder","message_receiver","packet_sender","packet_receiver","digital_modem").contains(kind)){var m=MessageSettings.read(tag.getCompound("message"));lines.add(Component.literal(m.operation()+" · "+m.bitRate()+" bit/s · threshold="+m.threshold()).withStyle(ChatFormatting.GRAY));}
            if(kind.startsWith("packet_")&&tag.contains("protocol")){var protocol=ProtocolSettings.read(tag.getCompound("protocol"));lines.add(Component.literal("P="+protocol.payloadBytes()+" B · retries="+protocol.retries()+" · timeout="+protocol.timeout()+" s · ACK loss="+protocol.ackLoss()).withStyle(ChatFormatting.GRAY));}
            if(kind.equals("orthogonality_meter")&&tag.contains("orthogonality")){var ortho=OrthogonalitySettings.read(tag.getCompound("orthogonality"));lines.add(Component.literal("["+ortho.start()+", "+ortho.end()+"] s · ε="+ortho.tolerance()+" · "+(ortho.discrete()?ortho.sampleHz()+" Hz":"continuous")).withStyle(ChatFormatting.GRAY));}
            boolean source=kind.equals("radio")&&s.transmit()||kind.equals("bit_generator");
            if(source){
                lines.add(Component.literal(s.signalMode().equals("bits")?(s.lab().randomBits()?"Bernoulli seed="+s.lab().seed():s.bits()):s.signalMode().equals("discrete")?s.lab().samples():s.expression()).withStyle(ChatFormatting.GRAY));
                if(s.signalMode().equals("sampled")||s.signalMode().equals("quantized"))lines.add(Component.literal("fvz="+s.sampleHz()+" Hz · L="+s.levels()).withStyle(ChatFormatting.GRAY));
                if(s.signalMode().equals("bits"))lines.add(Component.literal(s.lab().lineCode().toUpperCase()+" · A0="+(s.lab().lineCode().equals("bipolar")?s.lab().amplitudeZero():0)+" · A1="+s.lab().amplitudeOne()+" · P1="+s.lab().probabilityOne()).withStyle(ChatFormatting.GRAY));
                if(!s.lab().quantity().equals("relative"))lines.add(Component.literal(s.lab().target()+" "+(s.lab().quantity().equals("energy")?"J":"W")).withStyle(ChatFormatting.GRAY));
            }else if(kind.equals("radio")){lines.add(Component.literal("AWGN μ="+s.lab().noiseMean()+" · σ²="+s.lab().noiseVariance()).withStyle(ChatFormatting.GRAY));if(p.demodulate())lines.add(Component.literal(p.modulation().toUpperCase()+" · "+p.symbolHz()+" bit/s · A_RX="+s.lab().amplitudeOne()).withStyle(ChatFormatting.GRAY));}
            if(kind.equals("modulator"))lines.add(Component.literal(p.modulation().equals("bpsk")?"Rb="+p.symbolHz()+" symbol/s":"AM m="+p.depth()).withStyle(ChatFormatting.GRAY));
            lines.add(Component.translatable("tooltip.prscraft.graph_layers",Integer.bitCount(tag.getInt("graphLayers")&15),Integer.bitCount(tag.getInt("graphTerms")&255)).withStyle(ChatFormatting.GRAY));
            lines.add(Component.translatable("tooltip.prscraft.template_apply").withStyle(ChatFormatting.YELLOW));
        }catch(IllegalArgumentException e){lines.add(Component.translatable("tooltip.prscraft.invalid_template").withStyle(ChatFormatting.RED));}
    }
}
