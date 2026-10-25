package de.suitcraft.mixin;

import de.suitcraft.Suit;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
	/** Netzläufer: Wände zählen als Leiter, solange man dagegen läuft. */
	@Inject(method = "onClimbable", at = @At("HEAD"), cancellable = true)
	private void suitcraft$wallClimb(CallbackInfoReturnable<Boolean> cir) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (self instanceof Player player && player.horizontalCollision && !player.isSpectator()
			&& !player.getAbilities().flying && Suit.wears(player, Suit.WEBRUNNER)) {
			cir.setReturnValue(true);
		}
	}

	/** Schattenmantel: Schleichende Träger sind für Monster praktisch unsichtbar. */
	@Inject(method = "getVisibilityPercent", at = @At("HEAD"), cancellable = true)
	private void suitcraft$shadow(ServerLevel level, Entity targetingEntity, CallbackInfoReturnable<Double> cir) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (self instanceof Player player && player.isShiftKeyDown() && Suit.wears(player, Suit.SHADOW)) {
			cir.setReturnValue(0.0);
		}
	}
}
