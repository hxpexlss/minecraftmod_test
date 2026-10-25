package de.chaosblock.client.mixin;

import de.chaosblock.client.ClientState;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Flüche "Ein Bein" und "Bleifüße": kein Sprinten. */
@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {
	@Inject(method = "isSprintingPossible", at = @At("HEAD"), cancellable = true)
	private void chaosblock$noSprint(boolean allowedInShallowWater, CallbackInfoReturnable<Boolean> cir) {
		if (ClientState.noSprint()) {
			cir.setReturnValue(false);
		}
	}
}
