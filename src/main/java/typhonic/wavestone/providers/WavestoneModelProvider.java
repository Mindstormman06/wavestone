package typhonic.wavestone.providers;

import net.fabricmc.fabric.api.client.datagen.v1.provider.FabricModelProvider;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.blockstates.MultiPartGenerator;
import net.minecraft.client.data.models.model.ModelLocationUtils;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.RedstoneSide;
import typhonic.wavestone.blocks.ModBlocks;

import static net.minecraft.client.data.models.BlockModelGenerators.*;

public class WavestoneModelProvider extends FabricModelProvider {
    public WavestoneModelProvider(FabricDataOutput output) {
        super(output);
    }

    @Override
    public void generateBlockStateModels(BlockModelGenerators blockStateModelGenerator) {
        blockStateModelGenerator.createTrivialCube(ModBlocks.WAVEFORM_READER);
        blockStateModelGenerator.createTrivialCube(ModBlocks.WAVEFORM_COMPARATOR);
        blockStateModelGenerator.createTrivialCube(ModBlocks.SIGNAL_AMPLIFIER);
        blockStateModelGenerator.createTrivialCube(ModBlocks.WAVE_LAMP);
        createWireBlock(blockStateModelGenerator, ModBlocks.SIGNAL_WIRE);
    }

    private void createWireBlock(BlockModelGenerators generators, Block block) {
        generators.blockStateOutput.accept(
                MultiPartGenerator.multiPart(block)
                        .with(or(
                                condition().term(BlockStateProperties.NORTH, false)
                                        .term(BlockStateProperties.EAST, false)
                                        .term(BlockStateProperties.SOUTH, false)
                                        .term(BlockStateProperties.WEST, false),
                                condition().term(BlockStateProperties.NORTH, true)
                                        .term(BlockStateProperties.EAST, true),
                                condition().term(BlockStateProperties.EAST, true)
                                        .term(BlockStateProperties.SOUTH, true),
                                condition().term(BlockStateProperties.SOUTH, true)
                                        .term(BlockStateProperties.WEST, true),
                                condition().term(BlockStateProperties.WEST, true)
                                        .term(BlockStateProperties.NORTH, true)
                        ), plainVariant(ModelLocationUtils.decorateBlockModelLocation("redstone_dust_dot")))
                        .with(condition().term(BlockStateProperties.NORTH, true),
                                plainVariant(ModelLocationUtils.decorateBlockModelLocation("redstone_dust_side0")))
                        .with(condition().term(BlockStateProperties.SOUTH, true),
                                plainVariant(ModelLocationUtils.decorateBlockModelLocation("redstone_dust_side_alt0")))
                        .with(condition().term(BlockStateProperties.EAST, true),
                                plainVariant(ModelLocationUtils.decorateBlockModelLocation("redstone_dust_side_alt1")).with(Y_ROT_270))
                        .with(condition().term(BlockStateProperties.WEST, true),
                                plainVariant(ModelLocationUtils.decorateBlockModelLocation("redstone_dust_side1")).with(Y_ROT_270))
        );
    }


    @Override
    public void generateItemModels(ItemModelGenerators itemModelGenerator) {
    }

    @Override
    public String getName() {
        return "WavestoneModelProvider";
    }
}
