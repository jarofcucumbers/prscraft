package dev.jarofcucumbers.prscraft;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.*;

@Mod(PRSCraft.ID)
public final class PRSCraft {
    public static final String ID = "prscraft";
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ID);
    public static final DeferredItem<AnalyzerItem> ANALYZER = ITEMS.register("line_analyzer", () -> new AnalyzerItem(new Item.Properties().stacksTo(1)));
    public static final DeferredBlock<TransmitterBlock> TRANSMITTER = BLOCKS.register("transmitter",
        () -> new TransmitterBlock(BlockBehaviour.Properties.of().strength(3).requiresCorrectToolForDrops()));
    public static final DeferredBlock<ProcessorBlock> AMPLIFIER=BLOCKS.register("amplifier",()->new ProcessorBlock("amplifier",BlockBehaviour.Properties.of().strength(3)));
    public static final DeferredBlock<ProcessorBlock> FILTER=BLOCKS.register("frequency_filter",()->new ProcessorBlock("frequency_filter",BlockBehaviour.Properties.of().strength(3)));
    public static final DeferredBlock<ProcessorBlock> MODULATOR=BLOCKS.register("modulator",()->new ProcessorBlock("modulator",BlockBehaviour.Properties.of().strength(3)));
    public static final DeferredItem<Item> TEMPLATE=ITEMS.registerSimpleItem("signal_template",new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> DATA_CARD=ITEMS.registerSimpleItem("data_card",new Item.Properties().stacksTo(1));
    static {
        ITEMS.registerSimpleBlockItem(TRANSMITTER); ITEMS.registerSimpleBlockItem(AMPLIFIER); ITEMS.registerSimpleBlockItem(FILTER); ITEMS.registerSimpleBlockItem(MODULATOR);
        for (SignalBudget.Medium medium : SignalBudget.Medium.values()) {
            String name = medium.name().toLowerCase(java.util.Locale.ROOT) + "_port";
            var block = BLOCKS.register(name, () -> new PortBlock(medium,
                BlockBehaviour.Properties.of().strength(2).requiresCorrectToolForDrops().noOcclusion()));
            ITEMS.registerSimpleBlockItem(block);
            var cable = BLOCKS.register(medium.name().toLowerCase(java.util.Locale.ROOT) + "_cable",
                () -> new CableBlock(medium, BlockBehaviour.Properties.of().strength(1).noOcclusion()));
            ITEMS.registerSimpleBlockItem(cable);
        }
    }
    public static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ID);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RadioBlockEntity>> RADIO_ENTITY =
        ENTITIES.register("radio", () -> BlockEntityType.Builder.of(RadioBlockEntity::new, TRANSMITTER.get(),AMPLIFIER.get(),FILTER.get(),MODULATOR.get()).build(null));
    public static final DeferredRegister<net.minecraft.world.inventory.MenuType<?>> MENUS=DeferredRegister.create(Registries.MENU,ID);
    public static final DeferredHolder<net.minecraft.world.inventory.MenuType<?>,net.minecraft.world.inventory.MenuType<PresetMenu>> MEDIA_MENU=
        MENUS.register("media",()->net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create((id,inventory,buf)->
            new PresetMenu(id,inventory,(RadioBlockEntity)inventory.player.level().getBlockEntity(buf.readBlockPos()))));
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ID);
    static {
        TABS.register("equipment", () -> CreativeModeTab.builder()
            .title(net.minecraft.network.chat.Component.translatable("itemGroup.prscraft"))
            .icon(() -> new ItemStack(TRANSMITTER.get()))
            .displayItems((parameters, output) -> ITEMS.getEntries().forEach(item -> output.accept(item.get())))
            .build());
    }
    public PRSCraft(IEventBus bus) { BLOCKS.register(bus); ITEMS.register(bus); TABS.register(bus); ENTITIES.register(bus); MENUS.register(bus); bus.addListener(RadioNetwork::register); }
}
