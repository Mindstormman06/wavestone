package typhonic.wavestone.providers;

import net.fabricmc.fabric.api.client.datagen.v1.provider.FabricModelProvider;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import typhonic.wavestone.blocks.ModBlocks;

public class WavestoneModelProvider extends FabricModelProvider {
    public WavestoneModelProvider(FabricDataOutput output) {
        super(output);
    }

    @Override
    public void generateBlockStateModels(BlockModelGenerators blockStateModelGenerator) {
        blockStateModelGenerator.createTrivialCube(ModBlocks.WAVEFORM_READER);
        blockStateModelGenerator.createTrivialCube(ModBlocks.WAVEFORM_COMPARATOR);
        blockStateModelGenerator.createTrivialCube(ModBlocks.SIGNAL_AMPLIFIER);
    }

    @Override
    public void generateItemModels(ItemModelGenerators itemModelGenerator) {

    }

    @Override
    public String getName() {
        return "WavestoneModelProvider";
    }
}
