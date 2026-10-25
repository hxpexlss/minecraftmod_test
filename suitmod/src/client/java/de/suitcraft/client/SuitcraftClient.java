package de.suitcraft.client;

import com.mojang.blaze3d.platform.InputConstants;
import de.suitcraft.Suit;
import de.suitcraft.SuitEntities;
import de.suitcraft.SuitItems;
import de.suitcraft.SuitPayloads;
import de.suitcraft.SuitcraftMod;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

public class SuitcraftClient implements ClientModInitializer {
	private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(SuitcraftMod.id("suits"));
	private static KeyMapping abilityKey;

	private boolean lastJumpDown;
	private boolean doubleJumpUsed;
	private boolean lastFallFlying;
	private int airTicks;
	private int dangerTicks;

	@Override
	public void onInitializeClient() {
		abilityKey = KeyMappingHelper.registerKeyMapping(
			new KeyMapping("key.suitcraft.ability", InputConstants.KEY_R, CATEGORY));

		EntityRenderers.register(SuitEntities.WEB_BALL, ThrownItemRenderer::new);

		ClientPlayNetworking.registerGlobalReceiver(SuitPayloads.Danger.TYPE, (payload, context) -> this.dangerTicks = 20);
		ClientTickEvents.END_CLIENT_TICK.register(this::tick);
		HudElementRegistry.addLast(SuitcraftMod.id("danger"), this::renderDanger);

		ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
			String key = abilityKey.getTranslatedKeyMessage().getString();
			Suit suit = Suit.of(stack);
			if (suit != null) {
				lines.add(Component.literal("§6Set-Bonus (alle 4 Teile):"));
				for (String ability : suit.abilities) {
					lines.add(Component.literal("§7• " + ability.replace("[Taste]", "[" + key + "]")));
				}
			} else if (stack.is(SuitItems.WEB_SHOOTER)) {
				lines.add(Component.literal("§7Rechtsklick: Netz schießen"));
				lines.add(Component.literal("§7Getroffene Gegner bleiben kleben"));
			}
		});
	}

	private void tick(Minecraft client) {
		LocalPlayer player = client.player;
		if (player == null) {
			return;
		}
		if (this.dangerTicks > 0) {
			this.dangerTicks--;
		}
		while (abilityKey.consumeClick()) {
			ClientPlayNetworking.send(SuitPayloads.Ability.INSTANCE);
		}
		this.doubleJump(client, player);
	}

	/**
	 * Doppelsprung für Schattenmantel und Sturmflügel. Beim Sturmflügel startet derselbe
	 * Tastendruck auch das Gleiten, der Sprung gibt dann einen Aufwind dazu.
	 */
	private void doubleJump(Minecraft client, LocalPlayer player) {
		boolean jumpDown = client.options.keyJump.isDown();
		boolean pressed = jumpDown && !this.lastJumpDown;
		this.lastJumpDown = jumpDown;
		boolean startedGliding = player.isFallFlying() && !this.lastFallFlying;
		this.lastFallFlying = player.isFallFlying();

		if (player.onGround() || player.isInWater() || player.onClimbable()) {
			this.doubleJumpUsed = false;
			this.airTicks = 0;
			return;
		}
		this.airTicks++;

		Suit suit = Suit.worn(player);
		if (suit != Suit.SHADOW && suit != Suit.STORM) {
			return;
		}
		if (!pressed || this.doubleJumpUsed || this.airTicks < 3 || player.getAbilities().flying) {
			return;
		}
		if (player.isFallFlying() && !startedGliding) {
			return;
		}
		Vec3 motion = player.getDeltaMovement();
		double up = suit == Suit.STORM ? 0.85 : 0.62;
		player.setDeltaMovement(motion.x * 1.1, up, motion.z * 1.1);
		player.resetFallDistance();
		this.doubleJumpUsed = true;
		ClientPlayNetworking.send(SuitPayloads.DoubleJump.INSTANCE);
	}

	/** Gefahrensinn: kurzes bernsteinfarbenes Flimmern am Bildschirmrand. */
	private void renderDanger(GuiGraphicsExtractor graphics, DeltaTracker delta) {
		if (this.dangerTicks <= 0) {
			return;
		}
		int alpha = (int) (150 * (this.dangerTicks / 20.0F) * (0.6F + 0.4F * (float) Math.sin(this.dangerTicks * 1.3)));
		alpha = Math.max(0, Math.min(255, alpha));
		int edge = (alpha << 24) | 0xFFAA00;
		int clear = 0x00FFAA00;
		int w = graphics.guiWidth();
		int h = graphics.guiHeight();
		int size = 28;
		graphics.fillGradient(0, 0, w, size, edge, clear);
		graphics.fillGradient(0, h - size, w, h, clear, edge);
	}
}
