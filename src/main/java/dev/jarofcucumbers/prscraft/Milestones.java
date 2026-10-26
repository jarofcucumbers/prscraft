package dev.jarofcucumbers.prscraft;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import java.util.Set;
public final class Milestones{
    private Milestones(){}
    public static final Set<String> IDS=Set.of("root","first_link","calibrated","correlation","survey","math_lab","microlink","unipolar_link","noise_recovery","superposition","timing_lab","delay_hunt","filter_lab","board_artist","wash_eraser","deep_chain","subtractor","signal_boost","self_similarity","fiber_link","copper_link","long_haul","four_tones","full_palette","analog_link","sampled_link","quantized_link","fine_quantization","natural_sampling","flat_sampling","unrz_decoder","urz_decoder","biased_bits","finite_burst","offset_scan","power_source","energy_source","quiet_receiver","gray_binary","gray_levels","message_link","utf8_message","rle_savings","huffman_savings","crc_verified","hamming_corrected","crc_rejected","codebook_saved","custom_codes","raw_code_stream","qpsk_link","qam16_link","packets_verified","arq_recovery","ack_ambiguity","orthogonal_pair","nonorthogonal_pair","simulator_lab","lab_coat","protocol_entry","weather_lab","dispersive_link");
    public static void award(ServerPlayer player,String name){var holder=player.server.getAdvancements().get(ResourceLocation.fromNamespaceAndPath(PRSCraft.ID,name));if(holder!=null)player.getAdvancements().award(holder,"complete");}
}
