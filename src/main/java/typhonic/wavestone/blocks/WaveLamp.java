package typhonic.wavestone.blocks;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.redstone.Orientation;
import org.jetbrains.annotations.Nullable;

/**
 * A lamp block that reacts instantly to signal changes.
 *
 * Vanilla redstone lamps schedule a block tick for turn-off (4 ticks / 200ms),
 * which is too slow to track per-tick audio band data. WaveLamp skips ticking
 * entirely — brightness is stored directly in blockstate as LIGHT_LEVEL (0–15)
 * and the level's light engine updates immediately on setState().
 *
 * Two display modes (right-click to toggle):
 *   BINARY — any signal > 0 = full brightness (15); no signal = off (0).
 *   ANALOG — brightness scales linearly with the 0–255 band value → 0–15.
 *
 * Drive it by calling WaveLamp.setLevel(level, pos, bandValue) from
 * WaveformComparator or SignalWire output logic. bandValue is the raw
 * 0–255 byte from the audio pipeline; mode conversion is done here.
 */
public class WaveLamp extends Block {

    // ── blockstate properties ────────────────────────────────────────────────

    public static final IntegerProperty LIGHT_LEVEL =
            IntegerProperty.create("light_level", 0, 15);

    public enum Mode implements StringRepresentable {
        BINARY("binary"),
        ANALOG("analog");

        private final String name;
        Mode(String name) { this.name = name; }

        @Override public String getSerializedName() { return name; }

        public Mode next() {
            return this == BINARY ? ANALOG : BINARY;
        }
    }

    public static final EnumProperty<Mode> MODE =
            EnumProperty.create("mode", Mode.class);

    // ── constructor / state definition ──────────────────────────────────────

    public WaveLamp(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(LIGHT_LEVEL, 0)
                .setValue(MODE, Mode.BINARY));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIGHT_LEVEL, MODE);
    }

    // ── interaction: right-click cycles mode ─────────────────────────────────

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (!level.isClientSide()) {
            Mode next = state.getValue(MODE).next();
            level.setBlock(pos, state.setValue(MODE, next), Block.UPDATE_ALL);
            player.displayClientMessage(
                    Component.literal("[WaveLamp] Mode: " + next.getSerializedName()), true);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos,
                                   Block block, @Nullable Orientation wireOrientation, boolean isMoving) {
        if (!level.isClientSide()) {
            // 1. Check if standard vanilla redstone is powering this block
            int redstoneSignal = level.getBestNeighborSignal(pos);
            int calculatedAmplitude = redstoneSignal * 17;

            // 2. Check adjacent SignalWires for their live signal
            int wirePeak = 0;
            for (net.minecraft.core.Direction direction : net.minecraft.core.Direction.values()) {
                BlockPos neighborPos = pos.relative(direction);
                BlockState neighborState = level.getBlockState(neighborPos);

                if (neighborState.getBlock() instanceof typhonic.wavestone.blocks.SignalWire) {
                    typhonic.wavestone.blocks.SignalWire wire = (typhonic.wavestone.blocks.SignalWire) neighborState.getBlock();
                    byte[] signal = wire.getLiveSignal(neighborPos);
                    for (byte b : signal) {
                        int v = b & 0xFF;
                        if (v > wirePeak) wirePeak = v;
                    }
                }
            }

            // 3. Apply the maximum amplitude to this WaveLamp
            int finalAmplitude = Math.max(calculatedAmplitude, wirePeak);
            WaveLamp.setLevel(level, pos, finalAmplitude);
        }
        super.neighborChanged(state, level, pos, block, wireOrientation, isMoving);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        if (!level.isClientSide() && !state.is(oldState.getBlock())) {
            int signal = level.getBestNeighborSignal(pos);
            int targetLight = (state.getValue(MODE) == Mode.BINARY) ? ((signal > 0) ? 15 : 0) : signal;

            if (targetLight > 0) {
                level.setBlock(pos, state.setValue(LIGHT_LEVEL, targetLight), Block.UPDATE_ALL);
            }
        }
        super.onPlace(state, level, pos, oldState, isMoving);
    }

    // ── static API ───────────────────────────────────────────────────────────

    /**
     * Update the brightness of a WaveLamp at the given position.
     *
     * @param level     the server-side level
     * @param pos       position of the WaveLamp
     * @param bandValue raw audio band amplitude, 0–255 (from the signal pipeline)
     *
     * No-ops if the block at pos isn't a WaveLamp.
     * Safe to call every tick — only writes state when the value changes.
     */
    public static void setLevel(Level level, BlockPos pos, int bandValue) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof WaveLamp)) return;

        Mode mode = state.getValue(MODE);
        int target = toLight(mode, bandValue);
        if (state.getValue(LIGHT_LEVEL) != target) {
            level.setBlock(pos, state.setValue(LIGHT_LEVEL, target), Block.UPDATE_ALL);
        }
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    /**
     * Convert a raw 0–255 band amplitude to a 0–15 light level for the given mode.
     *
     * BINARY: 0 → 0, anything else → 15.
     * ANALOG: linear scale, floor(bandValue * 15 / 255).
     */
    private static int toLight(Mode mode, int bandValue) {
        int v = Math.max(0, Math.min(255, bandValue));
        return switch (mode) {
            case BINARY -> (v > 0) ? 15 : 0;
            case ANALOG -> (v * 15) / 255;
        };
    }
}
