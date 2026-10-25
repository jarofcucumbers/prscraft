package dev.jarofcucumbers.prscraft.client;
import dev.jarofcucumbers.prscraft.PRSCraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
@EventBusSubscriber(modid=PRSCraft.ID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class ClientSetup {
    @SubscribeEvent public static void renderers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers event){event.registerBlockEntityRenderer(PRSCraft.BOARD_ENTITY.get(),DrawingBoardRenderer::new);}
    @SubscribeEvent public static void menus(RegisterMenuScreensEvent event){event.register(PRSCraft.MEDIA_MENU.get(),PresetScreen::new);}
    @SubscribeEvent public static void properties(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event){event.enqueueWork(()->{
        net.minecraft.client.renderer.item.ItemProperties.register(PRSCraft.ERASER.get(),net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(PRSCraft.ID,"wear"),(stack,level,entity,seed)->(float)stack.getDamageValue()/stack.getMaxDamage());
        net.minecraft.client.renderer.item.ItemProperties.register(PRSCraft.TEMPLATE.get(),net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(PRSCraft.ID,"profile"),(stack,level,entity,seed)->{
            var custom=stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);if(custom==null)return 0;
            try{var tag=custom.copyTag();return dev.jarofcucumbers.prscraft.TemplateItem.model(tag.getString("kind"),dev.jarofcucumbers.prscraft.RadioSettings.read(tag.getCompound("settings")));}catch(IllegalArgumentException e){return 0;}
        });
    });}

}
