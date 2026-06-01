package typhonic.wavestone.client;

import typhonic.wavestone.network.JukeboxWaveformPayload;
import typhonic.wavestone.util.GranularAudioExtractor;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.resources.Identifier;
import net.minecraft.core.BlockPos;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ClientAudioPipeline {

    public static void handleJukeboxStart(BlockPos jukeboxPos, SoundEvent recordSound) {
        Identifier soundId = recordSound.location();

        CompletableFuture.runAsync(() -> {
            try {
                InputStream assetStream = Minecraft.getInstance().getResourceManager()
                        .getResourceOrThrow(soundId).open();

                AudioInputStream pcmStream = AudioSystem.getAudioInputStream(assetStream);
                AudioFormat format = pcmStream.getFormat();
                byte[] rawBytes = pcmStream.readAllBytes();
                pcmStream.close();

                List<byte[]> processedTimeline = GranularAudioExtractor.extractGranularTimeline(rawBytes, format);

                ClientPlayNetworking.send(new JukeboxWaveformPayload(jukeboxPos, processedTimeline));

            } catch (Exception e) {
                System.err.println("[Wavestone] Pipeline error processing " + soundId + ": " + e.getMessage());
            }
        });
    }
}
