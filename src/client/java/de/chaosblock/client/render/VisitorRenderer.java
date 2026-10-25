package de.chaosblock.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import de.chaosblock.ChaosBlockMod;
import de.chaosblock.ambient.VisitorEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

public class VisitorRenderer extends HumanoidMobRenderer<VisitorEntity, VisitorRenderer.State, HumanoidModel<VisitorRenderer.State>> {
	private static final Identifier TEXTURE = ChaosBlockMod.id("textures/entity/visitor.png");
	private static final RenderType EYES = RenderTypes.eyes(ChaosBlockMod.id("textures/entity/visitor_eyes.png"));

	public static class State extends HumanoidRenderState {
		public boolean eyes = true;
	}

	public VisitorRenderer(EntityRendererProvider.Context context) {
		super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE)), 0.5F);
		this.addLayer(new Eyes(this));
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public Identifier getTextureLocation(State state) {
		return TEXTURE;
	}

	@Override
	public void extractRenderState(VisitorEntity entity, State state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.eyes = entity.hasGlowingEyes();
	}

	/** Leuchtende weiße Augen, auch in völliger Dunkelheit sichtbar. */
	private static class Eyes extends EyesLayer<State, HumanoidModel<State>> {
		Eyes(RenderLayerParent<State, HumanoidModel<State>> parent) {
			super(parent);
		}

		@Override
		public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, State state, float yRot, float xRot) {
			if (state.eyes) {
				super.submit(poseStack, collector, lightCoords, state, yRot, xRot);
			}
		}

		@Override
		public RenderType renderType() {
			return EYES;
		}
	}
}
