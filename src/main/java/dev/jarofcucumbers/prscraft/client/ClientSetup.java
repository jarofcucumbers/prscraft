package dev.jarofcucumbers.prscraft.client;
import dev.jarofcucumbers.prscraft.PRSCraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
@EventBusSubscriber(modid=PRSCraft.ID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class ClientSetup {
    @SubscribeEvent public static void menus(RegisterMenuScreensEvent event){event.register(PRSCraft.MEDIA_MENU.get(),PresetScreen::new);}
}
