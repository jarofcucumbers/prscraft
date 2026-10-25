package dev.jarofcucumbers.prscraft;

import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import java.util.*;

/** Read-only instrument. Never writes a block state or a block entity. */
public final class AnalyzerItem extends Item {
    public AnalyzerItem(Properties properties) { super(properties); }
    @Override public InteractionResult useOn(UseOnContext context) {
        var level=context.getLevel();var player=context.getPlayer();var pos=context.getClickedPos();
        if (!(level.getBlockState(pos).getBlock() instanceof PortBlock port)) return InteractionResult.PASS;
        if (level.isClientSide || player==null) return InteractionResult.sidedSuccess(level.isClientSide);
        var nodes=NetworkWalk.distances(pos,node->{
            List<BlockPos> neighbors=new ArrayList<>();
            for(Direction d:Direction.values()) {
                var next=node.relative(d);if(!level.hasChunkAt(next))continue;
                var block=level.getBlockState(next).getBlock();
                if(block instanceof CableBlock cable&&cable.medium==port.medium || block instanceof PortBlock other&&other.medium==port.medium) neighbors.add(next);
            }
            return neighbors;
        },256);
        player.displayClientMessage(Component.translatable("message.prscraft.analyzer",Component.translatable("medium.prscraft."+port.medium.name().toLowerCase(Locale.ROOT)),nodes.size()),false);
        int found=0;
        Set<BlockPos> devices=new HashSet<>();
        for(var entry:nodes.entrySet()) {
            if(!(level.getBlockState(entry.getKey()).getBlock() instanceof PortBlock))continue;
            for(Direction d:Direction.values()) {
                var next=entry.getKey().relative(d);
                if(!level.hasChunkAt(next)||!devices.add(next)||!(level.getBlockEntity(next) instanceof RadioBlockEntity radio))continue;
                RadioSettings settings=radio.settings();found++;
                double loss=2*port.medium.lossDb(0,settings.carrierMHz())
                    +port.medium.lossDb(entry.getValue()+1,settings.carrierMHz())-port.medium.lossDb(0,settings.carrierMHz());
                player.displayClientMessage(Component.translatable("message.prscraft.device",next.toShortString(),
                    settings.transmit()?"TX":"RX",settings.enabled()?"ON":"OFF",settings.carrierMHz(),entry.getValue()+1,
                    String.format(Locale.ROOT,"%.2f",loss),settings.transmit()?String.format(Locale.ROOT,"%.2f",settings.powerDbm()-loss):"—"),false);
                if(found>=8)break;
            }
            if(found>=8)break;
        }
        if(found==0)player.displayClientMessage(Component.translatable("message.prscraft.analyzer_empty"),false);
        if(nodes.size()>=256)player.displayClientMessage(Component.translatable("message.prscraft.limit"),false);
        return InteractionResult.SUCCESS;
    }
}
