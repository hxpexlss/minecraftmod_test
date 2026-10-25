package de.chaosblock.client;

import de.chaosblock.ChaosBlockMod;
import de.chaosblock.ModEntities;
import de.chaosblock.client.render.VisitorRenderer;
import de.chaosblock.curse.CurseManager;
import de.chaosblock.network.ModPayloads;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

public class ChaosBlockClient implements ClientModInitializer {
	private static final Identifier GRAYSCALE = ChaosBlockMod.id("grayscale");
	private static final Identifier BLOODMOON = ChaosBlockMod.id("bloodmoon");
	private static final RandomSource RANDOM = RandomSource.create();

	private float lastYawOffset;
	private float lastPitchOffset;
	private int limpTicks;

	@Override
	public void onInitializeClient() {
		EntityRenderers.register(ModEntities.VISITOR, VisitorRenderer::new);

		ClientPlayNetworking.registerGlobalReceiver(ModPayloads.Oracle.TYPE, (payload, context) -> {
			ClientState.oracleBlock = payload.blockId();
			ClientState.oracleMessage = payload.message();
			ClientState.oracleTicks = payload.ticks();
			ClientState.oracleTotal = payload.ticks();
		});
		ClientPlayNetworking.registerGlobalReceiver(ModPayloads.Curse.TYPE, (payload, context) -> {
			ClientState.curse = payload.curseId();
			ClientState.curseTicks = payload.ticksLeft();
		});
		ClientPlayNetworking.registerGlobalReceiver(ModPayloads.ScreenFx.TYPE, (payload, context) ->
			ClientState.SCREEN.put(payload.effect(), payload.ticks()));

		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ClientState.reset());
		ClientTickEvents.END_CLIENT_TICK.register(this::tick);
		HudElementRegistry.addLast(ChaosBlockMod.id("hud"), this::renderHud);
	}

	private void tick(Minecraft client) {
		LocalPlayer player = client.player;
		if (player == null || client.isPaused()) {
			return;
		}
		ClientState.tick();
		this.updatePostEffects(player);
		this.shakeCamera(player);
	}

	private void updatePostEffects(LocalPlayer player) {
		List<Identifier> current = player.getActivePostEffects();
		List<Identifier> wanted = new ArrayList<>(current);
		wanted.remove(GRAYSCALE);
		wanted.remove(BLOODMOON);
		if (ClientState.cursed("farbenblind")) {
			wanted.add(GRAYSCALE);
		}
		if (ClientState.screen("bloodmoon")) {
			wanted.add(BLOODMOON);
		}
		if (!wanted.equals(current)) {
			player.setActivePostEffects(wanted);
		}
	}

	/**
	 * Wackeln der Kamera. Es wird immer nur die Änderung gegenüber dem letzten
	 * Tick angewendet, damit der Blick nicht dauerhaft wegdriftet.
	 */
	private void shakeCamera(LocalPlayer player) {
		float yaw = 0.0F;
		float pitch = 0.0F;
		if (ClientState.cursed("zittrig")) {
			yaw += (RANDOM.nextFloat() - 0.5F) * 3.0F;
			pitch += (RANDOM.nextFloat() - 0.5F) * 2.0F;
		}
		if (ClientState.screen("quake")) {
			yaw += (RANDOM.nextFloat() - 0.5F) * 6.0F;
			pitch += (RANDOM.nextFloat() - 0.5F) * 5.0F;
		}
		if (ClientState.cursed("einbein") && player.onGround() && player.getDeltaMovement().horizontalDistanceSqr() > 0.0004) {
			this.limpTicks++;
			pitch += Mth.sin(this.limpTicks * 0.55F) * 4.0F;
			yaw += Mth.sin(this.limpTicks * 0.275F) * 1.5F;
		}
		float dYaw = yaw - this.lastYawOffset;
		float dPitch = pitch - this.lastPitchOffset;
		if (dYaw != 0.0F || dPitch != 0.0F) {
			player.setYRot(player.getYRot() + dYaw);
			player.setXRot(Mth.clamp(player.getXRot() + dPitch, -90.0F, 90.0F));
		}
		this.lastYawOffset = yaw;
		this.lastPitchOffset = pitch;
	}

	private void renderHud(GuiGraphicsExtractor graphics, DeltaTracker delta) {
		Minecraft client = Minecraft.getInstance();
		if (client.player == null) {
			return;
		}
		Font font = client.font;
		int width = graphics.guiWidth();

		if (ClientState.oracleTicks > 0) {
			int alpha = Mth.clamp((int) (255 * Math.min(1.0F, ClientState.oracleTicks / 20.0F)), 8, 255);
			if (!ClientState.oracleMessage.isEmpty()) {
				int color = (alpha << 24) | 0xAA0000;
				graphics.centeredText(font, Component.literal(ClientState.oracleMessage), width / 2, 50, color);
			} else {
				Identifier id = Identifier.tryParse(ClientState.oracleBlock);
				Block block = id == null ? null : BuiltInRegistries.BLOCK.getOptional(id).orElse(null);
				if (block != null) {
					Component name = Component.literal("Chaos-Block: ").append(block.getName());
					int textWidth = font.width(name);
					int x = width / 2 - (textWidth + 20) / 2;
					graphics.fill(x - 4, 44, x + textWidth + 24, 66, (Math.min(alpha, 160) << 24));
					graphics.item(new ItemStack(block.asItem()), x, 47);
					graphics.text(font, name, x + 20, 51, (alpha << 24) | 0xE0B0FF);
				}
			}
		}

		if (ClientState.curseTicks > 0 && !ClientState.curse.isEmpty()) {
			CurseManager.Curse curse = CurseManager.byId(ClientState.curse);
			String name = curse == null ? ClientState.curse : curse.name();
			int seconds = ClientState.curseTicks / 20;
			String text = "☠ " + name + "  " + seconds / 60 + ":" + String.format("%02d", seconds % 60);
			int x = width - font.width(text) - 6;
			graphics.fill(x - 4, 4, width - 2, 18, 0x90000000);
			graphics.text(font, text, x, 7, 0xFFFF5555);
		}
	}
}
