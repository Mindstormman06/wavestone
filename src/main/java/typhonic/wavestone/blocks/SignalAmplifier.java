package typhonic.wavestone.blocks;

import typhonic.wavestone.Wavestone;
import typhonic.wavestone.util.FrequencyBands;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.HashMap;
import java.util.Map;

public class SignalAmplifier extends Block {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 6.0, 16.0);

    // FIX: same issue as SignalWire — use a per-position map instead of an instance field.
    private final Map<BlockPos, byte[]> signalByPos = new HashMap<>();

    public SignalAmplifier(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection());
    }

    public void receiveSignal(byte[] signal, Level level, BlockPos pos, int currentTtl, Map<BlockPos, Integer> visitedTtl) {
        int previousTtl = visitedTtl.getOrDefault(pos.immutable(), -1);
        if (currentTtl <= previousTtl) {
            return; // Already processed this amplifier with equal or stronger signal
        }
        visitedTtl.put(pos.immutable(), currentTtl);

        byte[] stored = signalByPos.computeIfAbsent(pos.immutable(), k -> new byte[FrequencyBands.BAND_COUNT]);
        System.arraycopy(signal, 0, stored, 0, FrequencyBands.BAND_COUNT);

        Direction outputDir = level.getBlockState(pos).getValue(FACING);
        BlockPos outputPos = pos.relative(outputDir);
        Block outputNeighbor = level.getBlockState(outputPos).getBlock();

        Wavestone.LOGGER.info("[SignalAmplifier] {} received signal: {} — forwarding {} to {}",
                pos.toShortString(), SignalWire.summarize(signal),
                outputDir.getName(), outputPos.toShortString());

        if (outputNeighbor instanceof SignalWire) {
            SignalWire.propagateRecursive(level, outputPos, stored, SignalWire.MAX_PROPAGATION_TTL, visitedTtl);
        } else if (outputNeighbor instanceof WaveformComparator comparator) {
            comparator.receiveSignal(stored, level, outputPos);
        } else {
            Wavestone.LOGGER.info("[SignalAmplifier] Output at {} is {} — nothing to forward to",
                    outputPos.toShortString(),
                    level.getBlockState(outputPos).getBlock().getClass().getSimpleName());
        }
    }

    public byte[] getLiveSignal(BlockPos pos) {
        return signalByPos.getOrDefault(pos.immutable(), new byte[FrequencyBands.BAND_COUNT]);
    }

}