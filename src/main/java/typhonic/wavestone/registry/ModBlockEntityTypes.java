package typhonic.wavestone.registry;

import typhonic.wavestone.blocks.ModBlocks;
import typhonic.wavestone.blocks.entity.WaveformReaderBlockEntity;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import typhonic.wavestone.Wavestone;

public final class ModBlockEntityTypes {
    private ModBlockEntityTypes() {}

    public static final BlockEntityType<WaveformReaderBlockEntity> WAVEFORM_READER =
            Registry.register(
                    BuiltInRegistries.BLOCK_ENTITY_TYPE,
                    Identifier.fromNamespaceAndPath(Wavestone.MOD_ID, "waveform_reader"),
                    FabricBlockEntityTypeBuilder.<WaveformReaderBlockEntity>create(
                            WaveformReaderBlockEntity::new,
                            ModBlocks.WAVEFORM_READER
                    ).build()
            );

    public static void initialize() {
        Wavestone.LOGGER.info("ModBlockEntityTypes initialized.");
    }
}
