package de.suitcraft.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import de.suitcraft.Suit;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Schattenmantel: Wer unsichtbar ist, zeigt auch seine Rüstung nicht. */
@Mixin(HumanoidArmorLayer.class)
public abstract class HumanoidArmorLayerMixin {
	@Inject(method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/HumanoidRenderState;FF)V",
		at = @At("HEAD"), cancellable = true)
	private void suitcraft$hideShadowSuit(PoseStack poseStack, SubmitNodeCollector collector, int light, HumanoidRenderState state,
		float yRot, float xRot, CallbackInfo ci) {
		if (state.isInvisible && Suit.of(state.chestEquipment) == Suit.SHADOW) {
			ci.cancel();
		}
	}
}
