package dev.jarofcucumbers.prscraft.client;
import dev.jarofcucumbers.prscraft.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.network.PacketDistributor;
public final class CodebookScreen extends PanelScreen{
    private final BlockPos pos;private final boolean inDevice,offhand;private String mapping,error="";private MessageTextBox editor;
    public CodebookScreen(BlockPos pos,CompoundTag data){super(Component.translatable("item.prscraft.codebook_card"),720,500);this.pos=pos;inDevice=data.getBoolean("inDevice");offhand=data.getBoolean("offhand");mapping=data.getString("mapping");}
    public BlockPos pos(){return pos;}public void receive(CompoundTag data){error=data.getString("error");mapping=data.getString("mapping");}
    @Override protected void build(){String previous=editor==null?mapping:editor.getValue();editor=addRenderableWidget(new MessageTextBox(font,14,82,692,300,Component.translatable("item.prscraft.codebook_card"),false));editor.setCharacterLimit(8192);editor.setValue(previous);button(14,460,180,tr("back"),b->onClose());button(206,460,240,tr("apply"),b->{try{SymbolCodebook.parse(editor.getValue());RadioClient.codebook(new CodebookNetwork.Edit(pos,inDevice,offhand,editor.getValue()));error="";}catch(IllegalArgumentException e){error=e.getMessage();}});button(458,460,248,tr("book"),b->minecraft.setScreen(new HandbookScreen(this)));}
    @Override protected void draw(GuiGraphics g){g.drawWordWrap(font,tr("codebook_help"),14,42,692,0xffc6dfe5);String message=error;try{var book=SymbolCodebook.parse(editor.getValue());g.drawString(font,Component.translatable("tooltip.prscraft.codebook",book.codes().size()),14,402,0xff92e4b8,false);}catch(IllegalArgumentException e){message=e.getMessage();}if(!message.isEmpty())g.drawWordWrap(font,Component.translatable("error.prscraft."+message),14,422,692,0xffffb57e);}
    @Override public void tick(){if(minecraft.player==null){onClose();return;}if(inDevice){if(!RadioClient.inRange(pos))onClose();}else if(!minecraft.player.getItemInHand(offhand?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND).is(PRSCraft.CODEBOOK_CARD.get()))onClose();}
    @Override public void onClose(){mapping=editor==null?mapping:editor.getValue();minecraft.setScreen(null);if(inDevice){var tag=new CompoundTag();tag.putString("action","media");RadioClient.request(new RadioNetwork.Request(pos,false,tag));}}
}
