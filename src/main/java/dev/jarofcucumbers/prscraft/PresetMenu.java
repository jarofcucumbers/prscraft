package dev.jarofcucumbers.prscraft;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.neoforged.neoforge.network.PacketDistributor;

public final class PresetMenu extends AbstractContainerMenu {
    public final RadioBlockEntity radio;
    public PresetMenu(int id,Inventory inventory,RadioBlockEntity radio) {
        super(PRSCraft.MEDIA_MENU.get(),id);this.radio=radio;
        addSlot(new SlotItemHandler(radio.media,0,24,38));addSlot(new SlotItemHandler(radio.media,1,24,92));
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inventory,col+row*9+9,8+col*18,155+row*18));
        for(int col=0;col<9;col++)addSlot(new Slot(inventory,col,8+col*18,213));
    }
    @Override public boolean stillValid(Player player) {
        return radio.getLevel()!=null && player.level()==radio.getLevel() && player.mayBuild()
            && radio.getLevel().mayInteract(player,radio.getBlockPos())
            && radio.getLevel().getBlockEntity(radio.getBlockPos())==radio
            && player.distanceToSqr(radio.getBlockPos().getX()+.5,radio.getBlockPos().getY()+.5,radio.getBlockPos().getZ()+.5)<=64;
    }
    @Override public ItemStack quickMoveStack(Player player,int index) {
        if(index<0||index>=slots.size())return ItemStack.EMPTY;
        var slot=slots.get(index);if(!slot.hasItem())return ItemStack.EMPTY;
        var stack=slot.getItem();var original=stack.copy();
        boolean moved=index<2?moveItemStackTo(stack,2,slots.size(),true):
            stack.is(PRSCraft.DATA_CARD.get())?moveItemStackTo(stack,1,2,false):moveItemStackTo(stack,0,1,false);
        if(!moved)return ItemStack.EMPTY;
        if(stack.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();
        return original;
    }
    @Override public boolean clickMenuButton(Player player,int action) {
        if(player.level().isClientSide||!stillValid(player))return false;
        var template=radio.media.getStackInSlot(0);var card=radio.media.getStackInSlot(1);
        if(action==0 && (template.is(Items.PAPER)||template.is(PRSCraft.TEMPLATE.get()))) {
            var tag=new net.minecraft.nbt.CompoundTag();tag.putString("kind",radio.kind());tag.put("settings",radio.settings().save());
            tag.put("gray",radio.gray().save());tag.put("processor",radio.processor().save());tag.put("dsp",radio.dsp().save());tag.putBoolean("correlationView",radio.correlationView());tag.putInt("graphLayers",radio.graphLayers());tag.putInt("graphTerms",radio.graphTerms());
            var result=new ItemStack(PRSCraft.TEMPLATE.get());result.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
            result.set(DataComponents.CUSTOM_NAME,Component.literal(radio.kind().equals("gray_converter")?"GRAY · "+radio.gray().mode()+" · "+radio.gray().wordBits()+" bit":TemplateItem.summary(radio.kind(),radio.settings(),radio.processor(),radio.dsp())));
            result.set(DataComponents.CUSTOM_MODEL_DATA,new net.minecraft.world.item.component.CustomModelData(TemplateItem.model(radio.kind(),radio.settings())));
            radio.media.setStackInSlot(0,result);return true;
        }
        if(action==1)return radio.applyTemplate();
        if(action==2 && card.is(PRSCraft.DATA_CARD.get())) {
            var data=RadioCapture.capture(player.level(),radio.getBlockPos());
            if(data.getIntArray("received").length!=512){player.displayClientMessage(Component.translatable("message.prscraft.no_capture"),true);return false;}
            var result=card.copy();result.set(DataComponents.CUSTOM_DATA,CustomData.of(data));
            result.set(DataComponents.CUSTOM_NAME,Component.translatable("item.prscraft.saved_capture",data.getLong("time")));
            radio.media.setStackInSlot(1,result);return true;
        }
        if(action==3 && card.is(PRSCraft.DATA_CARD.get()) && card.has(DataComponents.CUSTOM_DATA) && player instanceof ServerPlayer serverPlayer) {
            var data=card.get(DataComponents.CUSTOM_DATA).copyTag();
            if(data.getIntArray("received").length!=512)return false;
            PacketDistributor.sendToPlayer(serverPlayer,new RadioNetwork.Export(data));return true;
        }
        if(action==4 && card.is(PRSCraft.DATA_CARD.get()) && card.has(DataComponents.CUSTOM_DATA) && player instanceof ServerPlayer serverPlayer){
            var data=card.get(DataComponents.CUSTOM_DATA).copyTag();
            try{
                RadioSettings.read(data.getCompound("settings"));
                if(!RadioSettings.read(data.getCompound("source")).transmit())return false;
                ProcessorSettings.read(data.getCompound("processor"));
                CaptureCsv.encode(data.getDouble("span"),new int[][]{data.getIntArray("original"),data.getIntArray("sampled"),data.getIntArray("sourceOutput"),data.getIntArray("emitted"),data.getIntArray("received")});
            }catch(IllegalArgumentException e){return false;}
            data.putString("captureKind",data.getString("kind"));data.putBoolean("recorded",true);data.putBoolean("connected",true);data.putString("kind","radio");
            player.closeContainer();
            PacketDistributor.sendToPlayer(serverPlayer,new RadioNetwork.Snapshot(radio.getBlockPos(),true,data));return true;
        }
        return false;
    }
}
