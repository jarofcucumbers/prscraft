package dev.jarofcucumbers.prscraft;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import java.util.Set;
public final class Milestones{
    private Milestones(){}
    public static final Set<String> IDS=Set.of("root","first_link","calibrated","correlation","survey","math_lab");
    public static void award(ServerPlayer player,String name){var holder=player.server.getAdvancements().get(ResourceLocation.fromNamespaceAndPath(PRSCraft.ID,name));if(holder!=null)player.getAdvancements().award(holder,"complete");}
}
