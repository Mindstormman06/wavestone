package typhonic.wavestone;

import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import typhonic.wavestone.blocks.ModBlocks;
import typhonic.wavestone.client.WaveLampColorProvider;
import net.fabricmc.api.ClientModInitializer;

public class WavestoneClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        WaveLampColorProvider.register(ModBlocks.WAVE_LAMP);
        BlockRenderLayerMap.putBlock(
                ModBlocks.WAVEFORM_READER,
                ChunkSectionLayer.CUTOUT
        );
    }
}
