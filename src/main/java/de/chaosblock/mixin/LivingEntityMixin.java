package de.chaosblock.mixin;

import de.chaosblock.curse.CurseManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
	/** Fluch "Glasknochen": doppelter Schaden. */
	@ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true)
	private float chaosblock$glassBones(float damage, ServerLevel level, DamageSource source) {
		if ((Object) this instanceof ServerPlayer player && CurseManager.has(player, "glasknochen")) {
			return damage * 2.0F;
		}
		return damage;
	}
}
