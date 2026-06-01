package typhonic.wavestone.blocks;

import typhonic.wavestone.Wavestone;
import typhonic.wavestone.util.FrequencyBands;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;

import java.util.HashMap;
import java.util.Map;

public class SignalWire extends Block {

    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty EAST  = BlockStateProperties.EAST;
    public static final BooleanProperty WEST  = BlockStateProperties.WEST;

    private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 1.5, 16.0);

    // FIX: signal state is per-position, not per-block-instance.
    // There is only ONE SignalWire Block object in memory shared by every wire
    // in the world, so instance fields get clobbered. Use a map keyed by pos.
    private final Map<BlockPos, byte[]> signalByPos = new HashMap<>();

    public SignalWire(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(NORTH, false)
                .setValue(SOUTH, false)
                .setValue(EAST,  false)
                .setValue(WEST,  false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, SOUTH, EAST, WEST);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return getUpdatedState(super.getStateForPlacement(ctx), ctx.getLevel(), ctx.getClickedPos());
    }

    @Override
    public BlockState updateShape(BlockState state, LevelReader levelReader,
                                  ScheduledTickAccess scheduledTicks,
                                  BlockPos pos, Direction direction, BlockPos neighborPos,
                                  BlockState neighborState, RandomSource random) {
        if (direction == Direction.DOWN && !state.canSurvive(levelReader, pos)) {
            return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        }
        return getUpdatedState(state, levelReader, pos);
    }

    private BlockState getUpdatedState(BlockState state, BlockGetter level, BlockPos pos) {
        return state
                .setValue(NORTH, connectsTo(level, pos, Direction.NORTH))
                .setValue(SOUTH, connectsTo(level, pos, Direction.SOUTH))
                .setValue(EAST,  connectsTo(level, pos, Direction.EAST))
                .setValue(WEST,  connectsTo(level, pos, Direction.WEST));
    }

    private boolean connectsTo(BlockGetter level, BlockPos pos, Direction dir) {
        Block neighbor = level.getBlockState(pos.relative(dir)).getBlock();
        return neighbor instanceof SignalWire
                || neighbor instanceof WaveformReader
                || neighbor instanceof WaveformComparator
                || neighbor instanceof SignalAmplifier
                || neighbor instanceof WaveLamp;
    }

    /** Store signal for this specific position. */
    public void receiveSignal(BlockPos pos, byte[] signal) {
        byte[] stored = signalByPos.computeIfAbsent(pos.immutable(), k -> new byte[FrequencyBands.BAND_COUNT]);
        System.arraycopy(signal, 0, stored, 0, FrequencyBands.BAND_COUNT);
        Wavestone.LOGGER.info("[SignalWire] {} received signal: {}", pos.toShortString(), summarize(signal));
    }

    /** Get signal for this specific position (zeros if never received). */
    public byte[] getLiveSignal(BlockPos pos) {
        return signalByPos.getOrDefault(pos.immutable(), new byte[FrequencyBands.BAND_COUNT]);
    }

    /** Clear stored signal when the block is removed so the map doesn't leak. */

    /**
     * Propagate a signal outward from `pos` to all connected wires/devices.
     * Uses a visited set so loops don't cause infinite recursion or stack overflow.
     */
    public static void propagateFrom(Level level, BlockPos origin, byte[] signal, int ttl) {
        Map<BlockPos, Integer> visitedTtl = new HashMap<>();
        propagateRecursive(level, origin, signal, ttl, visitedTtl);
        Wavestone.LOGGER.info("[SignalWire] Propagation from {} reached {} wire(s)", origin.toShortString(), visitedTtl.size());
    }

    public static void propagateRecursive(Level level, BlockPos pos, byte[] signal, int ttl, Map<BlockPos, Integer> visitedTtl) {
        if (ttl <= 0) {
            Wavestone.LOGGER.info("[SignalWire] TTL exhausted at {}", pos.toShortString());
            return;
        }
        
        int previousTtl = visitedTtl.getOrDefault(pos.immutable(), -1);
        if (ttl <= previousTtl) {
            return; // Already visited with an equal or stronger signal
        }
        visitedTtl.put(pos.immutable(), ttl);

        Block block = level.getBlockState(pos).getBlock();
        if (block instanceof SignalWire wire) {
            wire.receiveSignal(pos, signal);
        }

        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.relative(dir);
            int neighborPrevTtl = visitedTtl.getOrDefault(neighborPos.immutable(), -1);
            if (ttl - 1 <= neighborPrevTtl) continue; // Skip if neighbor already has equal or stronger signal

            Block neighbor = level.getBlockState(neighborPos).getBlock();

            if (neighbor instanceof SignalWire) {
                propagateRecursive(level, neighborPos, signal, ttl - 1, visitedTtl);
            } else if (neighbor instanceof WaveformComparator comparator) {
                Wavestone.LOGGER.info("[SignalWire] Signal reached WaveformComparator at {}", neighborPos.toShortString());
                comparator.receiveSignal(signal, level, neighborPos);
            } else if (neighbor instanceof SignalAmplifier amplifier) {
                Wavestone.LOGGER.info("[SignalWire] Signal reached SignalAmplifier at {}", neighborPos.toShortString());
                amplifier.receiveSignal(signal, level, neighborPos, ttl - 1, visitedTtl);
            } else if (neighbor instanceof WaveLamp) {
                // Find the peak band value across all bands and drive the lamp directly.
                // WaveLamp.setLevel handles mode (binary/analog) and only writes state when changed.
                int peak = 0;
                for (byte b : signal) {
                    int v = b & 0xFF;
                    if (v > peak) peak = v;
                }
                Wavestone.LOGGER.info("[SignalWire] Signal reached WaveLamp at {} — peak={}", neighborPos.toShortString(), peak);
                WaveLamp.setLevel(level, neighborPos, peak);
            }
        }
    }

    /** Summarise a signal array as "b0=N b1=N ..." skipping zero bands for readability. */
    public static String summarize(byte[] signal) {
        StringBuilder sb = new StringBuilder();
        boolean anyNonZero = false;
        for (int i = 0; i < signal.length; i++) {
            int v = signal[i] & 0xFF;
            if (v != 0) {
                if (sb.length() > 0) sb.append(' ');
                sb.append('b').append(i).append('=').append(v);
                anyNonZero = true;
            }
        }
        return anyNonZero ? sb.toString() : "silent";
    }

    public static final int MAX_PROPAGATION_TTL = 15;
}