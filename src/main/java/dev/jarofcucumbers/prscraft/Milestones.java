package dev.jarofcucumbers.prscraft;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import java.util.Set;
public final class Milestones{
    private Milestones(){}
    public static final Set<String> IDS=Set.of("root","first_link","calibrated","correlation","survey","math_lab","microlink","unipolar_link","noise_recovery","superposition","timing_lab","delay_hunt","filter_lab","board_artist","wash_eraser","deep_chain","subtractor","signal_boost","self_similarity","fiber_link","copper_link","long_haul","four_tones","full_palette","analog_link","sampled_link","quantized_link","fine_quantization","natural_sampling","flat_sampling","unrz_decoder","urz_decoder","biased_bits","finite_burst","offset_scan","power_source","energy_source","quiet_receiver");
    public static void award(ServerPlayer player,String name){var holder=player.server.getAdvancements().get(ResourceLocation.fromNamespaceAndPath(PRSCraft.ID,name));if(holder!=null)player.getAdvancements().award(holder,"complete");}
}
