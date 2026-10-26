package dev.jarofcucumbers.prscraft.client;
import dev.jarofcucumbers.prscraft.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
@EventBusSubscriber(modid=PRSCraft.ID,value=Dist.CLIENT)
public final class CoatHud{
    private static BlockPos pos;private static CompoundTag data;private static long updated;private static int ticks;
    public static void receive(BlockPos at,CompoundTag next){pos=at;data=next;updated=System.currentTimeMillis();}
    private static boolean wearing(Minecraft mc){return mc.player!=null&&mc.player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST).is(PRSCraft.DOCTORAL_COAT.get());}
    @SubscribeEvent public static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post e){var mc=Minecraft.getInstance();if(!wearing(mc)||mc.screen!=null){data=null;return;}if(++ticks%5==0&&mc.hitResult instanceof BlockHitResult hit)PacketDistributor.sendToServer(new CoatNetwork.Inspect(hit.getBlockPos()));}
    @SubscribeEvent public static void draw(net.neoforged.neoforge.client.event.RenderGuiEvent.Post e){var mc=Minecraft.getInstance();if(!wearing(mc)||mc.screen!=null||mc.options.hideGui||data==null||System.currentTimeMillis()-updated>1000||!(mc.hitResult instanceof BlockHitResult hit)||!hit.getBlockPos().equals(pos))return;var lines=new java.util.ArrayList<Component>();String id=data.getString("block");lines.add(Component.translatable("block."+id.replace(':','.')));var template=data.getCompound("template");try{if(template.contains("settings")){var s=RadioSettings.read(template.getCompound("settings"));var p=ProcessorSettings.read(template.getCompound("processor"));lines.add(Component.literal(data.getString("role")+" · "+(s.enabled()?"ON":"OFF")+" · "+s.carrierMHz()+" MHz · "+s.powerDbm()+" dBm"));lines.add(Component.literal(TemplateItem.summary(template.getString("kind"),s,p,DspSettings.read(template.getCompound("dsp")))));if(template.contains("message")&&java.util.Set.of("message_source","message_codec","message_receiver","channel_coder","packet_sender","packet_receiver","digital_modem").contains(template.getString("kind"))){var m=MessageSettings.read(template.getCompound("message"));lines.add(Component.literal(m.operation()+" · "+m.bitRate()+" bit/s · threshold="+m.threshold()));}lines.add(Component.literal("μ="+s.lab().noiseMean()+" · σ²="+s.lab().noiseVariance()));}else if(data.contains("metres"))lines.add(Component.literal(data.getDouble("metres")+" m · damage="+(data.getDouble("damage")*100)+"% · τ/m="+data.getDouble("dispersion")));else if(data.contains("nodes"))lines.add(Component.literal(data.getInt("nodes")+" nodes · "+data.getInt("wires")+" wires"));else lines.add(Component.literal(data.getString("medium")));}catch(IllegalArgumentException ignored){return;}int w=0;for(var line:lines)w=Math.max(w,mc.font.width(line));w=Math.min(420,w+20);int x=(mc.getWindow().getGuiScaledWidth()-w)/2,y=8;var g=e.getGuiGraphics();g.fill(x,y,x+w,y+lines.size()*12+10,0xdd102331);for(int n=0;n<lines.size();n++)g.drawString(mc.font,mc.font.plainSubstrByWidth(lines.get(n).getString(),w-16),x+8,y+6+n*12,n==0?0xffffd892:0xffd3ece4,false);}
}
