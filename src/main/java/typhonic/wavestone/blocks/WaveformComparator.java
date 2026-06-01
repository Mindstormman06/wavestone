package typhonic.wavestone.blocks;

import typhonic.wavestone.Wavestone;
import typhonic.wavestone.util.FrequencyBands;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.network.chat.Component;

import java.util.HashMap;
import java.util.Map;

public class WaveformComparator extends Block {

    public static final IntegerProperty BAND = IntegerProperty.create("band", 0, FrequencyBands.BAND_COUNT - 1);

    // FIX: same shared-instance problem — output is per-position.
    private final Map<BlockPos, Integer> outputByPos = new HashMap<>();

    private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 6.0, 16.0);

    public WaveformComparator(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(BAND, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BAND);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (!level.isClientSide()) {
            int currentBand = state.getValue(BAND);
            int nextBand = (currentBand + 1) % FrequencyBands.BAND_COUNT;
            level.setBlock(pos, state.setValue(BAND, nextBand), 3);

            int currentOutput = outputByPos.getOrDefault(pos.immutable(), 0);
            player.displayClientMessage(
                    Component.literal(
                            "[Wavestone] Band " + nextBand + ": " + FrequencyBands.BAND_NAMES[nextBand]
                                    + " (" + (int)FrequencyBands.getLowerHz(nextBand) + "–"
                                    + (int)FrequencyBands.getUpperHz(nextBand) + " Hz)"
                                    + "  current output=" + currentOutput
                    ), true
            );

            Wavestone.LOGGER.info("[WaveformComparator] {} switched to band {} ({}), current output={}",
                    pos.toShortString(), nextBand, FrequencyBands.BAND_NAMES[nextBand], currentOutput);
        }
        return InteractionResult.SUCCESS;
    }

    public void receiveSignal(byte[] signal, Level level, BlockPos pos) {
        int band = level.getBlockState(pos).getValue(BAND);
        int rawValue = (band < signal.length) ? (signal[band] & 0xFF) : 0;
        int newOutput = rawValue / 17; // map 0-255 to 0-15 for standard redstone
        int oldOutput = outputByPos.getOrDefault(pos.immutable(), 0);

        Wavestone.LOGGER.info("[WaveformComparator] {} band={} signal={} → raw {} → output {} → {}",
                pos.toShortString(), band, SignalWire.summarize(signal), rawValue, oldOutput, newOutput);

        if (newOutput != oldOutput) {
            outputByPos.put(pos.immutable(), newOutput);
            level.updateNeighborsAt(pos, level.getBlockState(pos).getBlock());
            Wavestone.LOGGER.info("[WaveformComparator] {} output changed: {} → {} (band {} / {})",
                    pos.toShortString(), oldOutput, newOutput,
                    band, FrequencyBands.BAND_NAMES[band]);

            // Drive any directly adjacent WaveLamps with no wire between them.
            for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.Plane.HORIZONTAL) {
                net.minecraft.core.BlockPos neighborPos = pos.relative(dir);
                if (level.getBlockState(neighborPos).getBlock() instanceof WaveLamp) {
                    WaveLamp.setLevel(level, neighborPos, rawValue); // Use raw 0-255 amplitude for WaveLamp
                }
            }
        }
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return outputByPos.getOrDefault(pos.immutable(), 0);
    }

    @Override
    public int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return outputByPos.getOrDefault(pos.immutable(), 0);
    }

}