package typhonic.wavestone.client;

import typhonic.wavestone.blocks.WaveLamp;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Tints WaveLamp quads from pure black (LIGHT_LEVEL=0) to pure white (LIGHT_LEVEL=15).
 *
 * The block texture should be solid white (#FFFFFF). This provider multiplies
 * each texel by the tint color, so white × tint = tint exactly.
 *
 * Registration: call WaveLampColorProvider.register() from your ClientModInitializer.
 */
public final class WaveLampColorProvider implements BlockColor {

    private static final WaveLampColorProvider INSTANCE = new WaveLampColorProvider();
    private WaveLampColorProvider() {}

    @Override
    public int getColor(BlockState state, BlockAndTintGetter level, BlockPos pos, int tintIndex) {
        // tintIndex 0 is the only one we use (matches the model's "tintindex": 0)
        if (tintIndex != 0) return 0xFFFFFF;

        int light = state.getValue(WaveLamp.LIGHT_LEVEL); // 0–15
        // Map 0–15 linearly to 0–255
        int channel = (light * 255) / 15;
        return (channel << 16) | (channel << 8) | channel; // packed RGB gray
    }

    /** Call once from your ClientModInitializer. */
    public static void register(typhonic.wavestone.blocks.WaveLamp block) {
        ColorProviderRegistry.BLOCK.register(INSTANCE, block);
    }
}
