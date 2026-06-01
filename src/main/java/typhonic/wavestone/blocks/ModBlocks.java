package typhonic.wavestone.blocks;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import typhonic.wavestone.Wavestone;

import java.util.function.Function;

public final class ModBlocks {
    private ModBlocks() {}

    public static final SignalWire SIGNAL_WIRE = register("signal_wire", SignalWire::new,
            BlockBehaviour.Properties.of()
                    .noCollision()
                    .instabreak()
                    .mapColor(MapColor.COLOR_GRAY)
    );

    public static final WaveformReader WAVEFORM_READER = register("waveform_reader", WaveformReader::new,
            BlockBehaviour.Properties.of()
                    .strength(1.5f)
                    .sound(SoundType.STONE)
                    .mapColor(MapColor.DEEPSLATE)
    );

    public static final WaveformComparator WAVEFORM_COMPARATOR = register("waveform_comparator", WaveformComparator::new,
            BlockBehaviour.Properties.of()
                    .strength(1.5f)
                    .sound(SoundType.STONE)
                    .mapColor(MapColor.DEEPSLATE)
    );

    public static final WaveLamp WAVE_LAMP = register("wave_lamp", WaveLamp::new,
            BlockBehaviour.Properties.of()
                    .strength(0.3f)
                    .sound(SoundType.GLASS)
                    .mapColor(MapColor.COLOR_YELLOW)
                    .lightLevel(state -> state.getValue(WaveLamp.LIGHT_LEVEL))
    );

    public static final SignalAmplifier SIGNAL_AMPLIFIER = register("signal_amplifier", SignalAmplifier::new,
            BlockBehaviour.Properties.of()
                    .strength(1.5f)
                    .sound(SoundType.STONE)
                    .mapColor(MapColor.DEEPSLATE)
    );

    private static <T extends Block> T register(String name, Function<BlockBehaviour.Properties, T> factory, BlockBehaviour.Properties props) {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK,
                Identifier.fromNamespaceAndPath(Wavestone.MOD_ID, name));
        T block = factory.apply(props.setId(key));
        Registry.register(BuiltInRegistries.BLOCK, key, block);

        // ✅ Register the BlockItem so /give and creative inventory work
        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM,
                Identifier.fromNamespaceAndPath(Wavestone.MOD_ID, name));
        Registry.register(BuiltInRegistries.ITEM, itemKey,
                new BlockItem(block, new Item.Properties().setId(itemKey)));

        return block;
    }

    public static void initialize() {
        Wavestone.LOGGER.info("ModBlocks initialized.");
    }
}