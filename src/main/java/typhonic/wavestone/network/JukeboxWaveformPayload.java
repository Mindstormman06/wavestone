package typhonic.wavestone.network;

import typhonic.wavestone.util.FrequencyBands;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;

public record JukeboxWaveformPayload(BlockPos pos, List<byte[]> timeline) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<JukeboxWaveformPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("wavestone", "jukebox_waveform"));

    public static final StreamCodec<RegistryFriendlyByteBuf, JukeboxWaveformPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, value) -> {
                        buf.writeBlockPos(value.pos());
                        buf.writeInt(value.timeline().size());
                        for (byte[] tickData : value.timeline()) {
                            buf.writeBytes(tickData);
                        }
                    },
                    buf -> {
                        BlockPos pos = buf.readBlockPos();
                        int totalTicks = buf.readInt();
                        List<byte[]> timeline = new ArrayList<>(totalTicks);
                        for (int i = 0; i < totalTicks; i++) {
                            byte[] tickData = new byte[FrequencyBands.BAND_COUNT];
                            buf.readBytes(tickData);
                            timeline.add(tickData);
                        }
                        return new JukeboxWaveformPayload(pos, timeline);
                    }
            );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
