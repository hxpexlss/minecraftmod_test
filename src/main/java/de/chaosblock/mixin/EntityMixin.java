package de.chaosblock.mixin;

import de.chaosblock.PrivateEntities;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {
	@Inject(method = "broadcastToPlayer", at = @At("HEAD"), cancellable = true)
	private void chaosblock$onlyForViewer(ServerPlayer player, CallbackInfoReturnable<Boolean> cir) {
		UUID viewer = PrivateEntities.viewerOf((Entity) (Object) this);
		if (viewer != null && !viewer.equals(player.getUUID())) {
			cir.setReturnValue(false);
		}
	}
}
