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
            case "bit_generator" -> "BITS · P1="+s.lab().probabilityOne()+" · "+s.bitRate()+" bit/s";
            case "amplifier" -> "AMP · "+p.gainDb()+" dB";
            case "frequency_filter" -> "FILTER · "+p.lowHz()+"–"+p.highHz()+" Hz";
            case "modulator" -> p.modulation().toUpperCase(Locale.ROOT)+" · "+p.oscillatorHz()+" Hz";
            default -> kind;
        };
    }
    public static int model(String kind,RadioSettings s){return switch(kind){case "radio"->s.transmit()?1:2;case "amplifier"->3;case "frequency_filter"->4;case "modulator"->5;case "bit_generator"->6;default->0;};}
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> lines,TooltipFlag flags){
        CustomData data=stack.get(DataComponents.CUSTOM_DATA);if(data==null){lines.add(Component.translatable("tooltip.prscraft.blank_template").withStyle(ChatFormatting.GRAY));return;}
        try{var tag=data.copyTag();var s=RadioSettings.read(tag.getCompound("settings"));var p=ProcessorSettings.read(tag.getCompound("processor"));
            lines.add(Component.literal(summary(tag.getString("kind"),s,p)).withStyle(ChatFormatting.AQUA));
            String kind=tag.getString("kind");
            boolean source=kind.equals("radio")&&s.transmit()||kind.equals("bit_generator");
            if(source){
                lines.add(Component.literal(s.signalMode().equals("bits")?(s.lab().randomBits()?"Bernoulli seed="+s.lab().seed():s.bits()):s.signalMode().equals("discrete")?s.lab().samples():s.expression()).withStyle(ChatFormatting.GRAY));
                if(s.signalMode().equals("sampled")||s.signalMode().equals("quantized"))lines.add(Component.literal("fvz="+s.sampleHz()+" Hz · L="+s.levels()).withStyle(ChatFormatting.GRAY));
                if(s.signalMode().equals("bits"))lines.add(Component.literal("A0="+s.lab().amplitudeZero()+" · A1="+s.lab().amplitudeOne()+" · P1="+s.lab().probabilityOne()).withStyle(ChatFormatting.GRAY));
                if(!s.lab().quantity().equals("relative"))lines.add(Component.literal(s.lab().target()+" "+(s.lab().quantity().equals("energy")?"J":"W")).withStyle(ChatFormatting.GRAY));
            }else if(kind.equals("radio"))lines.add(Component.literal("AWGN μ="+s.lab().noiseMean()+" · σ²="+s.lab().noiseVariance()).withStyle(ChatFormatting.GRAY));
            if(kind.equals("modulator"))lines.add(Component.literal(p.modulation().equals("bpsk")?"Rb="+p.symbolHz()+" symbol/s":"AM m="+p.depth()).withStyle(ChatFormatting.GRAY));
            lines.add(Component.translatable("tooltip.prscraft.graph_layers",Integer.bitCount(tag.getInt("graphLayers")&15),Integer.bitCount(tag.getInt("graphTerms")&255)).withStyle(ChatFormatting.GRAY));
            lines.add(Component.translatable("tooltip.prscraft.template_apply").withStyle(ChatFormatting.YELLOW));
        }catch(IllegalArgumentException e){lines.add(Component.translatable("tooltip.prscraft.invalid_template").withStyle(ChatFormatting.RED));}
    }
}
