package typhonic.wavestone.blocks.entity;

import typhonic.wavestone.Wavestone;
import typhonic.wavestone.network.JukeboxWaveformPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Not a BlockEntity — a simple per-position store for jukebox waveform timelines.
 * The client processes audio via ClientAudioPipeline and sends the timeline here.
 */
public class AudioJukeboxData {

    private static final Map<BlockPos, AudioJukeboxData> ACTIVE = new ConcurrentHashMap<>();

    private final List<byte[]> timeline;
    private int currentTick = 0;

    private AudioJukeboxData(List<byte[]> timeline) {
        this.timeline = timeline;
    }

    public static void startPlayback(BlockPos pos, List<byte[]> timeline) {
        ACTIVE.put(pos.immutable(), new AudioJukeboxData(timeline));
        Wavestone.LOGGER.info("[AudioJukeboxData] Started playback at {} ({} ticks of band data)",
            pos.toShortString(), timeline.size());
    }

    public static void stopPlayback(BlockPos pos) {
        if (ACTIVE.remove(pos.immutable()) != null) {
            Wavestone.LOGGER.info("[AudioJukeboxData] Stopped playback at {}", pos.toShortString());
        }
    }

    /**
     * Returns this tick's band snapshot for the jukebox at pos, advancing the counter.
     * Returns null if no data or playback has finished.
     */
    public static byte[] getCurrentSnapshot(BlockPos jukeboxPos) {
        AudioJukeboxData data = ACTIVE.get(jukeboxPos.immutable());
        if (data == null) return null;

        if (data.currentTick >= data.timeline.size()) {
            ACTIVE.remove(jukeboxPos.immutable());
            return null;
        }

        return data.timeline.get(data.currentTick++);
    }

    public static boolean isPlaying(BlockPos pos) {
        return ACTIVE.containsKey(pos.immutable());
    }

    public static void registerNetworkReceiver() {
        ServerPlayNetworking.registerGlobalReceiver(JukeboxWaveformPayload.TYPE, (payload, context) ->
            context.server().execute(() -> startPlayback(payload.pos(), payload.timeline()))
        );
    }
}
