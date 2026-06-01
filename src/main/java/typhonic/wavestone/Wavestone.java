package typhonic.wavestone;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import typhonic.wavestone.blocks.ModBlocks;
import typhonic.wavestone.blocks.entity.AudioJukeboxData;
import typhonic.wavestone.network.JukeboxWaveformPayload;
import typhonic.wavestone.registry.ModBlockEntityTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Wavestone implements ModInitializer {
    public static final String MOD_ID = "wavestone";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ModBlocks.initialize();
        ModBlockEntityTypes.initialize();

        PayloadTypeRegistry.playC2S().register(JukeboxWaveformPayload.TYPE, JukeboxWaveformPayload.STREAM_CODEC);
        AudioJukeboxData.registerNetworkReceiver();

        LOGGER.info("Wavestone Loaded!");
    }
}
