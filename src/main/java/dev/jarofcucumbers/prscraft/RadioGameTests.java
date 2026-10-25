package dev.jarofcucumbers.prscraft;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.*;
import net.neoforged.neoforge.gametest.*;

/** Development-only integration fixtures; excluded from the distributable jar. */
@GameTestHolder(PRSCraft.ID)
@PrefixGameTestTemplate(false)
public final class RadioGameTests {
    private static final BlockPos TX=new BlockPos(1,1,1), RX=new BlockPos(3,1,4), CORNER=new BlockPos(3,1,2);
    private static Block block(String name) { return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(PRSCraft.ID,name)); }
    private static RadioSettings receiver(double carrier) {
        var s=RadioSettings.DEFAULT;
        return new RadioSettings(s.expression(),s.sampleHz(),s.levels(),false,true,true,0,carrier,s.powerDbm(),true,0.5);
    }
    private static void setup(GameTestHelper h) {
        h.setBlock(TX,PRSCraft.TRANSMITTER.get()); h.setBlock(RX,PRSCraft.TRANSMITTER.get());
        h.setBlock(new BlockPos(2,1,1),block("coaxial_port"));h.setBlock(new BlockPos(3,1,1),block("coaxial_cable"));
        h.setBlock(CORNER,block("coaxial_cable"));h.setBlock(new BlockPos(3,1,3),block("coaxial_port"));
        ((RadioBlockEntity)h.getBlockEntity(RX)).configure(receiver(100));
    }
    @GameTest(template="empty",timeoutTicks=20)
    public static void cableTurnAndBrokenLink(GameTestHelper h) {
        setup(h);
        var link=CableNetwork.receive(h.getLevel(),h.absolutePos(RX),receiver(100));
        h.assertTrue(link!=null,"TX must be reachable around a cable corner");
        h.assertTrue(link.metres()==5,"Shortest route must be 5 metres");
        h.assertTrue(Math.abs(link.lossDb()-3)<1e-9,"Two coax connectors and 5 metres must lose 3 dB");
        h.setBlock(CORNER,Blocks.AIR);
        h.assertTrue(CableNetwork.receive(h.getLevel(),h.absolutePos(RX),receiver(100))==null,"Broken cable must disconnect RX");
        h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=20)
    public static void incompatibleCableAndCarrier(GameTestHelper h) {
        setup(h);
        h.assertTrue(CableNetwork.receive(h.getLevel(),h.absolutePos(RX),receiver(125))==null,"Different carrier must not connect");
        h.setBlock(CORNER,block("fiber_cable"));
        h.assertTrue(CableNetwork.receive(h.getLevel(),h.absolutePos(RX),receiver(100))==null,"Mixed media must not connect");
        h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=20)
    public static void settingsSurviveBlockEntitySerialization(GameTestHelper h) {
        setup(h);
        RadioBlockEntity radio=h.getBlockEntity(RX);
        var original=receiver(125);radio.configure(original);
        var tag=radio.saveWithFullMetadata(h.getLevel().registryAccess());
        RadioBlockEntity restored=new RadioBlockEntity(h.absolutePos(RX),radio.getBlockState());
        restored.loadWithComponents(tag,h.getLevel().registryAccess());
        h.assertTrue(restored.settings().equals(original),"All waveform, sampling, noise and RX settings must persist");
        h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=20)
    public static void analyzerIsReadOnlyAndNotSwallowedByPort(GameTestHelper h) {
        setup(h);
        var player=h.makeMockPlayer(net.minecraft.world.level.GameType.CREATIVE);
        var stack=new net.minecraft.world.item.ItemStack(PRSCraft.ANALYZER.get());
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,stack);
        var port=h.absolutePos(new BlockPos(2,1,1));
        var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(port),net.minecraft.core.Direction.UP,port,false);
        var state=h.getLevel().getBlockState(port);
        var tx=(RadioBlockEntity)h.getBlockEntity(TX);
        var before=tx.saveWithFullMetadata(h.getLevel().registryAccess());
        var result=state.useItemOn(stack,h.getLevel(),player,net.minecraft.world.InteractionHand.MAIN_HAND,hit);
        h.assertTrue(result==net.minecraft.world.ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION,"Port must allow analyzer item handling");
        PRSCraft.ANALYZER.get().useOn(new net.minecraft.world.item.context.UseOnContext(player,net.minecraft.world.InteractionHand.MAIN_HAND,hit));
        h.assertTrue(h.getLevel().getBlockState(port).equals(state),"Analyzer must not modify port state");
        h.assertTrue(before.equals(tx.saveWithFullMetadata(h.getLevel().registryAccess())),"Analyzer must not modify radio settings");
        h.succeed();
    }

}
