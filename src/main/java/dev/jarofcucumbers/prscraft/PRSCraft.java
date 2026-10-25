package dev.jarofcucumbers.prscraft;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.*;

@Mod(PRSCraft.ID)
public final class PRSCraft {
    public static final String ID = "prscraft";
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ID);
    public static final DeferredBlock<TransmitterBlock> TRANSMITTER = BLOCKS.register("transmitter",
        () -> new TransmitterBlock(BlockBehaviour.Properties.of().strength(3).requiresCorrectToolForDrops()));
    static {
        ITEMS.registerSimpleBlockItem(TRANSMITTER);
        for (SignalBudget.Medium medium : SignalBudget.Medium.values()) {
            String name = medium.name().toLowerCase(java.util.Locale.ROOT) + "_port";
            var block = BLOCKS.register(name, () -> new PortBlock(medium,
                BlockBehaviour.Properties.of().strength(2).requiresCorrectToolForDrops().noOcclusion()));
            ITEMS.registerSimpleBlockItem(block);
        }
    }
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ID);
    static {
        TABS.register("equipment", () -> CreativeModeTab.builder()
            .title(net.minecraft.network.chat.Component.translatable("itemGroup.prscraft"))
            .icon(() -> new ItemStack(TRANSMITTER.get()))
            .displayItems((parameters, output) -> ITEMS.getEntries().forEach(item -> output.accept(item.get())))
            .build());
    }
    public PRSCraft(IEventBus bus) { BLOCKS.register(bus); ITEMS.register(bus); TABS.register(bus); }
}
