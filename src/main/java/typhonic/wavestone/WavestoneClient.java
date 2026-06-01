package typhonic.wavestone;

import typhonic.wavestone.blocks.ModBlocks;
import typhonic.wavestone.client.WaveLampColorProvider;
import net.fabricmc.api.ClientModInitializer;

public class WavestoneClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        WaveLampColorProvider.register(ModBlocks.WAVE_LAMP);
    }
}
