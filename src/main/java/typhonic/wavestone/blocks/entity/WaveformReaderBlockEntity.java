package typhonic.wavestone.blocks.entity;

import typhonic.wavestone.Wavestone;
import typhonic.wavestone.blocks.SignalWire;
import typhonic.wavestone.blocks.WaveformComparator;
import typhonic.wavestone.registry.ModBlockEntityTypes;
import typhonic.wavestone.util.FrequencyBands;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

public class WaveformReaderBlockEntity extends BlockEntity {

    private boolean wasPlayingLastTick = false;

    public WaveformReaderBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.WAVEFORM_READER, pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, WaveformReaderBlockEntity be) {
        if (level.isClientSide()) return;

        BlockPos jukeboxPos = findAdjacentJukebox(level, pos);

        if (jukeboxPos != null && AudioJukeboxData.isPlaying(jukeboxPos)) {
            byte[] signal = AudioJukeboxData.getCurrentSnapshot(jukeboxPos);
            if (signal != null) {
                be.wasPlayingLastTick = true;
                Wavestone.LOGGER.info("[WaveformReader] {} tick signal: {}",
                    pos.toShortString(), SignalWire.summarize(signal));
                propagateSignal(level, pos, signal);
                return;
            }
        }

        // No signal this tick — if we were playing, send one final silence to clear downstream
        if (be.wasPlayingLastTick) {
            Wavestone.LOGGER.info("[WaveformReader] {} playback ended, clearing signal", pos.toShortString());
            be.wasPlayingLastTick = false;
            propagateSignal(level, pos, new byte[FrequencyBands.BAND_COUNT]);
        }
    }

    private static BlockPos findAdjacentJukebox(Level level, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = pos.relative(dir);
            if (level.getBlockEntity(neighbor) instanceof JukeboxBlockEntity) {
                return neighbor;
            }
        }
        return null;
    }

    private static void propagateSignal(Level level, BlockPos pos, byte[] signal) {
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.relative(dir);
            Block neighbor = level.getBlockState(neighborPos).getBlock();

            if (neighbor instanceof SignalWire) {
                SignalWire.propagateFrom(level, neighborPos, signal, SignalWire.MAX_PROPAGATION_TTL);
            } else if (neighbor instanceof WaveformComparator comparator) {
                comparator.receiveSignal(signal, level, neighborPos);
            }
        }
    }
}
