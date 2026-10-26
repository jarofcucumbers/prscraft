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
    public static final DeferredBlock<ProcessorBlock> MESSAGE_SOURCE=BLOCKS.register("message_source",()->new ProcessorBlock("message_source",BlockBehaviour.Properties.of().strength(3)));
    public static final DeferredBlock<ProcessorBlock> MESSAGE_CODEC=BLOCKS.register("message_codec",()->new ProcessorBlock("message_codec",BlockBehaviour.Properties.of().strength(3)));
    public static final DeferredBlock<ProcessorBlock> CHANNEL_CODER=BLOCKS.register("channel_coder",()->new ProcessorBlock("channel_coder",BlockBehaviour.Properties.of().strength(3)));
    public static final DeferredBlock<ProcessorBlock> MESSAGE_RECEIVER=BLOCKS.register("message_receiver",()->new ProcessorBlock("message_receiver",BlockBehaviour.Properties.of().strength(3)));
    public static final DeferredBlock<ProcessorBlock> DIGITAL_MODEM=BLOCKS.register("digital_modem",()->new ProcessorBlock("digital_modem",BlockBehaviour.Properties.of().strength(3)));
    public static final DeferredBlock<ProcessorBlock> PACKET_SENDER=BLOCKS.register("packet_sender",()->new ProcessorBlock("packet_sender",BlockBehaviour.Properties.of().strength(3)));
    public static final DeferredBlock<ProcessorBlock> PACKET_RECEIVER=BLOCKS.register("packet_receiver",()->new ProcessorBlock("packet_receiver",BlockBehaviour.Properties.of().strength(3)));
    public static final DeferredBlock<ProcessorBlock> ORTHOGONALITY_METER=BLOCKS.register("orthogonality_meter",()->new ProcessorBlock("orthogonality_meter",BlockBehaviour.Properties.of().strength(3)));
    public static final DeferredBlock<SimulatorBlock> SIMULATOR=BLOCKS.register("simulator",()->new SimulatorBlock(BlockBehaviour.Properties.of().strength(3).noOcclusion()));
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS=DeferredRegister.create(Registries.ARMOR_MATERIAL,ID);
    public static final DeferredHolder<ArmorMaterial,ArmorMaterial> COAT_MATERIAL=ARMOR_MATERIALS.register("doctoral_coat",()->new ArmorMaterial(java.util.Map.of(ArmorItem.Type.CHESTPLATE,3),12,net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_LEATHER,()->net.minecraft.world.item.crafting.Ingredient.of(Items.WHITE_WOOL),java.util.List.of(new ArmorMaterial.Layer(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(ID,"doctoral_coat"))),0,0));
    public static final DeferredItem<ArmorItem> DOCTORAL_COAT=ITEMS.register("doctoral_coat",()->new ArmorItem(COAT_MATERIAL,ArmorItem.Type.CHESTPLATE,new Item.Properties().durability(256)));
    public static final DeferredBlock<ProcessorBlock> GRAY=BLOCKS.register("gray_converter",()->new ProcessorBlock("gray_converter",BlockBehaviour.Properties.of().strength(3)));
    public static final DeferredBlock<ProcessorBlock> ADDER=BLOCKS.register("adder",()->new ProcessorBlock("adder",BlockBehaviour.Properties.of().strength(3)));
    public static final DeferredBlock<ProcessorBlock> INTEGRATOR=BLOCKS.register("integrator",()->new ProcessorBlock("integrator",BlockBehaviour.Properties.of().strength(3)));
    public static final DeferredBlock<ProcessorBlock> CORRELATOR=BLOCKS.register("correlator",()->new ProcessorBlock("correlator",BlockBehaviour.Properties.of().strength(3)));
    public static final DeferredBlock<ProcessorBlock> TIME_GATE=BLOCKS.register("time_gate",()->new ProcessorBlock("time_gate",BlockBehaviour.Properties.of().strength(3)));
    public static final DeferredBlock<ProcessorBlock> DELAY=BLOCKS.register("delay_line",()->new ProcessorBlock("delay_line",BlockBehaviour.Properties.of().strength(3)));
    public static final DeferredItem<CableWrenchItem> WRENCH=ITEMS.register("cable_wrench",()->new CableWrenchItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<CodebookCardItem> CODEBOOK_CARD=ITEMS.register("codebook_card",()->new CodebookCardItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<ResearchNotebookItem> NOTEBOOK=ITEMS.register("research_notebook",()->new ResearchNotebookItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> DATA_CARD=ITEMS.registerSimpleItem("data_card",new Item.Properties().stacksTo(1));
    static {
        ITEMS.registerSimpleBlockItem(SIMULATOR);ITEMS.registerSimpleBlockItem(DIGITAL_MODEM);ITEMS.registerSimpleBlockItem(PACKET_SENDER);ITEMS.registerSimpleBlockItem(PACKET_RECEIVER);ITEMS.registerSimpleBlockItem(ORTHOGONALITY_METER);ITEMS.registerSimpleBlockItem(MESSAGE_SOURCE);ITEMS.registerSimpleBlockItem(MESSAGE_CODEC);ITEMS.registerSimpleBlockItem(CHANNEL_CODER);ITEMS.registerSimpleBlockItem(MESSAGE_RECEIVER);ITEMS.registerSimpleBlockItem(GRAY);ITEMS.registerSimpleBlockItem(ADDER);ITEMS.registerSimpleBlockItem(INTEGRATOR);ITEMS.registerSimpleBlockItem(CORRELATOR);ITEMS.registerSimpleBlockItem(TIME_GATE);ITEMS.registerSimpleBlockItem(DELAY);ITEMS.registerSimpleBlockItem(TRANSMITTER);ITEMS.registerSimpleBlockItem(BIT_GENERATOR); ITEMS.registerSimpleBlockItem(AMPLIFIER); ITEMS.registerSimpleBlockItem(FILTER); ITEMS.registerSimpleBlockItem(MODULATOR);
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
        ENTITIES.register("radio", () -> BlockEntityType.Builder.of(RadioBlockEntity::new, TRANSMITTER.get(),AMPLIFIER.get(),FILTER.get(),MODULATOR.get(),BIT_GENERATOR.get(),ADDER.get(),INTEGRATOR.get(),CORRELATOR.get(),TIME_GATE.get(),DELAY.get(),GRAY.get(),MESSAGE_SOURCE.get(),MESSAGE_CODEC.get(),CHANNEL_CODER.get(),MESSAGE_RECEIVER.get(),DIGITAL_MODEM.get(),PACKET_SENDER.get(),PACKET_RECEIVER.get(),ORTHOGONALITY_METER.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<SimulatorBlockEntity>> SIMULATOR_ENTITY=ENTITIES.register("simulator",()->BlockEntityType.Builder.of(SimulatorBlockEntity::new,SIMULATOR.get()).build(null));
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
    public PRSCraft(IEventBus bus) { ARMOR_MATERIALS.register(bus);BLOCKS.register(bus); ITEMS.register(bus); TABS.register(bus); ENTITIES.register(bus); MENUS.register(bus); RECIPES.register(bus); bus.addListener(RadioNetwork::register); bus.addListener(BoardNetwork::register);bus.addListener(CodebookNetwork::register);bus.addListener(SimulatorNetwork::register);bus.addListener(CoatNetwork::register);bus.addListener(NotebookNetwork::register); }
}
