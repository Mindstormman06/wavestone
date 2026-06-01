package typhonic.wavestone.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.JukeboxPlayable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import typhonic.wavestone.client.ClientAudioPipeline;

@Mixin(JukeboxBlock.class)
public class JukeboxMixin {

	@Inject(at = @At("RETURN"), method = "useItemOn")
	private void onUseItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
	                         Player player, InteractionHand hand, BlockHitResult hit,
	                         CallbackInfoReturnable<InteractionResult> cir) {
		if (!level.isClientSide()) return;
		if (!cir.getReturnValue().consumesAction()) return;

		JukeboxPlayable playable = stack.get(DataComponents.JUKEBOX_PLAYABLE);
		if (playable == null) return;

		playable.song().unwrap(level.registryAccess()).ifPresent(songHolder ->
				ClientAudioPipeline.handleJukeboxStart(pos, songHolder.value().soundEvent().value())
		);
	}
}