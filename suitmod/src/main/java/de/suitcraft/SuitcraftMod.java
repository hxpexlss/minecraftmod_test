package de.suitcraft;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SuitcraftMod implements ModInitializer {
	public static final String MOD_ID = "suitcraft";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		SuitItems.register();
		SuitEntities.register();
		SuitPayloads.register();

		ServerPlayNetworking.registerGlobalReceiver(SuitPayloads.Ability.TYPE, (payload, context) -> SuitAbilities.onAbility(context.player()));
		ServerPlayNetworking.registerGlobalReceiver(SuitPayloads.DoubleJump.TYPE, (payload, context) -> SuitAbilities.onDoubleJump(context.player()));

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			Tasks.tick();
			SuitAbilities.tick(server);
		});
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damage, blocked) -> SuitAbilities.onDamaged(entity, source));
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> SuitAbilities.onLeave(handler.getPlayer()));
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			Tasks.flush();
			SuitAbilities.clear();
		});

		LOGGER.info("Anzug-Werkstatt geladen.");
	}
}
