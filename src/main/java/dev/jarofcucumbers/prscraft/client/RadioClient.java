package dev.jarofcucumbers.prscraft.client;

import dev.jarofcucumbers.prscraft.RadioNetwork;
import net.minecraft.client.Minecraft;

/** Only resolved by the clientbound packet handler on the physical client. */
public final class RadioClient {
    private RadioClient() {}
    public static void receive(RadioNetwork.Snapshot packet) {
        Minecraft minecraft = Minecraft.getInstance();
        if (packet.open()) minecraft.setScreen(new RadioScreen(packet.pos(), packet.data()));
        else if (minecraft.screen instanceof RadioScreen screen && screen.pos().equals(packet.pos())) screen.receive(packet.data());
    }
}
