package typhonic.wavestone.client;

import typhonic.wavestone.network.JukeboxWaveformPayload;
import typhonic.wavestone.util.GranularAudioExtractor;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.sounds.JOrbisAudioStream;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.resources.Identifier;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;

import java.io.InputStream;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ClientAudioPipeline {

    private static final RandomSource RANDOM = RandomSource.create();

    private static final ExecutorService AUDIO_EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "wavestone-audio-pipeline");
        t.setDaemon(true);
        return t;
    });

    public static void handleJukeboxStart(BlockPos jukeboxPos, SoundEvent recordSound) {
        Identifier soundId = recordSound.location();

        AUDIO_EXECUTOR.submit(() -> {
            try {
                SoundManager soundManager = Minecraft.getInstance().getSoundManager();
                WeighedSoundEvents soundEvents = soundManager.getSoundEvent(soundId);

                if (soundEvents == null) {
                    System.err.println("[Wavestone] No sound event registered for: " + soundId
                            + " — disc may not have a sounds.json entry.");
                    return;
                }

                Sound sound = soundEvents.getSound(RANDOM);
                if (sound == null || sound == SoundManager.EMPTY_SOUND) {
                    System.err.println("[Wavestone] Sound event " + soundId + " resolved to empty sound.");
                    return;
                }

                Identifier resourcePath = sound.getPath();
                InputStream assetStream = Minecraft.getInstance().getResourceManager()
                        .getResourceOrThrow(resourcePath).open();

                JOrbisAudioStream oggStream = new JOrbisAudioStream(assetStream);

                List<byte[]> timeline = GranularAudioExtractor.extractGranularTimeline(
                        oggStream, oggStream.getFormat());

                ClientPlayNetworking.send(new JukeboxWaveformPayload(jukeboxPos, timeline));

            } catch (Exception e) {
                System.err.println("[Wavestone] Pipeline error processing " + soundId + ": " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
}
