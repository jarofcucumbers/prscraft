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
    public static final DeferredBlock<BitGeneratorBlock> BIT_GENERATOR=BLOCKS.register("bit_generator",()->new BitGeneratorBlock(BlockBehaviour.Properties.of().strength(3)));
    public static final DeferredItem<HandbookItem> HANDBOOK=ITEMS.register("handbook",()->new HandbookItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<TemplateItem> TEMPLATE=ITEMS.register("signal_template",()->new TemplateItem(new Item.Properties().stacksTo(1)));
    public static final DeferredBlock<DrawingBoardBlock> BOARD=BLOCKS.register("drawing_board",()->new DrawingBoardBlock(BlockBehaviour.Properties.of().strength(2).noOcclusion()));
    public static final java.util.List<DeferredItem<ChalkItem>> CHALKS=new java.util.ArrayList<>();
    public static final DeferredItem<BoardEraserItem> ERASER=ITEMS.register("board_eraser",()->new BoardEraserItem(new Item.Properties().durability(BoardEraserItem.CAPACITY).setNoRepair()));
    static {ITEMS.registerSimpleBlockItem(BOARD);for(int i=0;i<BoardPixels.COLORS.length;i++){final int palette=i+1;CHALKS.add(ITEMS.register(BoardPixels.COLORS[i]+"_chalk",()->new ChalkItem(new Item.Properties().durability(ChalkItem.CAPACITY).setNoRepair(),palette)));}}
    public static final java.util.List<DeferredBlock<CableBlock>> CABLES=new java.util.ArrayList<>();
    public static final DeferredBlock<ProcessorBlock> GRAY=BLOCKS.register("gray_converter",()->new ProcessorBlock("gray_converter",BlockBehaviour.Properties.of().strength(3)));
    public static final DeferredBlock<ProcessorBlock> ADDER=BLOCKS.register("adder",()->new ProcessorBlock("adder",BlockBehaviour.Properties.of().strength(3)));
    public static final DeferredBlock<ProcessorBlock> INTEGRATOR=BLOCKS.register("integrator",()->new ProcessorBlock("integrator",BlockBehaviour.Properties.of().strength(3)));
    public static final DeferredBlock<ProcessorBlock> CORRELATOR=BLOCKS.register("correlator",()->new ProcessorBlock("correlator",BlockBehaviour.Properties.of().strength(3)));
    public static final DeferredBlock<ProcessorBlock> TIME_GATE=BLOCKS.register("time_gate",()->new ProcessorBlock("time_gate",BlockBehaviour.Properties.of().strength(3)));
    public static final DeferredBlock<ProcessorBlock> DELAY=BLOCKS.register("delay_line",()->new ProcessorBlock("delay_line",BlockBehaviour.Properties.of().strength(3)));
    public static final DeferredItem<CableWrenchItem> WRENCH=ITEMS.register("cable_wrench",()->new CableWrenchItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> DATA_CARD=ITEMS.registerSimpleItem("data_card",new Item.Properties().stacksTo(1));
    static {
        ITEMS.registerSimpleBlockItem(GRAY);ITEMS.registerSimpleBlockItem(ADDER);ITEMS.registerSimpleBlockItem(INTEGRATOR);ITEMS.registerSimpleBlockItem(CORRELATOR);ITEMS.registerSimpleBlockItem(TIME_GATE);ITEMS.registerSimpleBlockItem(DELAY);ITEMS.registerSimpleBlockItem(TRANSMITTER);ITEMS.registerSimpleBlockItem(BIT_GENERATOR); ITEMS.registerSimpleBlockItem(AMPLIFIER); ITEMS.registerSimpleBlockItem(FILTER); ITEMS.registerSimpleBlockItem(MODULATOR);
        for (SignalBudget.Medium medium : SignalBudget.Medium.values()) {
            String name = medium.name().toLowerCase(java.util.Locale.ROOT) + "_port";
            var block = BLOCKS.register(name, () -> new PortBlock(medium,
                BlockBehaviour.Properties.of().strength(2).requiresCorrectToolForDrops().noOcclusion()));
            ITEMS.registerSimpleBlockItem(block);
            var cable = BLOCKS.register(medium.name().toLowerCase(java.util.Locale.ROOT) + "_cable",
                () -> new CableBlock(medium, BlockBehaviour.Properties.of().strength(1).noOcclusion()));
            CABLES.add(cable);ITEMS.registerSimpleBlockItem(cable);
        }
    }
    public static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ID);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RadioBlockEntity>> RADIO_ENTITY =
        ENTITIES.register("radio", () -> BlockEntityType.Builder.of(RadioBlockEntity::new, TRANSMITTER.get(),AMPLIFIER.get(),FILTER.get(),MODULATOR.get(),BIT_GENERATOR.get(),ADDER.get(),INTEGRATOR.get(),CORRELATOR.get(),TIME_GATE.get(),DELAY.get(),GRAY.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<CableBlockEntity>> CABLE_ENTITY=ENTITIES.register("cable",()->BlockEntityType.Builder.of(CableBlockEntity::new,CABLES.stream().map(DeferredBlock::get).toArray(Block[]::new)).build(null));
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<DrawingBoardEntity>> BOARD_ENTITY=ENTITIES.register("drawing_board",()->BlockEntityType.Builder.of(DrawingBoardEntity::new,BOARD.get()).build(null));
    public static final DeferredRegister<net.minecraft.world.inventory.MenuType<?>> MENUS=DeferredRegister.create(Registries.MENU,ID);
    public static final DeferredHolder<net.minecraft.world.inventory.MenuType<?>,net.minecraft.world.inventory.MenuType<PresetMenu>> MEDIA_MENU=
        MENUS.register("media",()->net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create((id,inventory,buf)->
            new PresetMenu(id,inventory,(RadioBlockEntity)inventory.player.level().getBlockEntity(buf.readBlockPos()))));
    public static final DeferredRegister<net.minecraft.world.item.crafting.RecipeSerializer<?>> RECIPES=DeferredRegister.create(Registries.RECIPE_SERIALIZER,ID);
    public static final DeferredHolder<net.minecraft.world.item.crafting.RecipeSerializer<?>,net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer<ChalkDyeRecipe>> CHALK_DYE=RECIPES.register("chalk_dye",()->new net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer<>(ChalkDyeRecipe::new));
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ID);
    static {
        TABS.register("equipment", () -> CreativeModeTab.builder()
            .title(net.minecraft.network.chat.Component.translatable("itemGroup.prscraft"))
            .icon(() -> new ItemStack(TRANSMITTER.get()))
            .displayItems((parameters, output) -> ITEMS.getEntries().forEach(item -> output.accept(item.get())))
            .build());
    }
    public PRSCraft(IEventBus bus) { BLOCKS.register(bus); ITEMS.register(bus); TABS.register(bus); ENTITIES.register(bus); MENUS.register(bus); RECIPES.register(bus); bus.addListener(RadioNetwork::register); bus.addListener(BoardNetwork::register); }
}
