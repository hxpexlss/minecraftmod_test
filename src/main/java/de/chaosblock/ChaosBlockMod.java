package de.chaosblock;

import de.chaosblock.ambient.AmbientManager;
import de.chaosblock.curse.CurseManager;
import de.chaosblock.event.BlockEvents;
import de.chaosblock.event.WorldEventManager;
import de.chaosblock.network.ModPayloads;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChaosBlockMod implements ModInitializer {
	public static final String MOD_ID = "chaosblock";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static ModConfig config;
	private static ChaosData data;
	private static ChaosBlockManager manager;
	private static MinecraftServer server;
	private static int saveTimer;

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	public static ModConfig config() {
		return config;
	}

	public static ChaosData data() {
		return data;
	}

	public static ChaosBlockManager manager() {
		return manager;
	}

	public static MinecraftServer server() {
		return server;
	}

	@Override
	public void onInitialize() {
		config = ModConfig.load();
		ModPayloads.register();
		ModItems.register(config);
		ModEntities.register();

		ServerLifecycleEvents.SERVER_STARTED.register(s -> {
			server = s;
			data = ChaosData.load(s);
			manager = new ChaosBlockManager(s, data, config);
			manager.init();
			WorldEventManager.init(data, config);
		});

		ServerLifecycleEvents.SERVER_STOPPING.register(s -> {
			if (data != null) {
				data.save(s);
			}
			Scheduler.clear();
			PrivateEntities.clear();
			AmbientManager.clear();
		});

		ServerLifecycleEvents.SERVER_STOPPED.register(s -> {
			server = null;
			data = null;
			manager = null;
		});

		ServerTickEvents.END_SERVER_TICK.register(s -> {
			if (manager == null) {
				return;
			}
			Scheduler.tick();
			manager.tick();
			CurseManager.tick(s);
			WorldEventManager.tick(s);
			AmbientManager.tick(s);
			if (++saveTimer >= 20 * 60) {
				saveTimer = 0;
				data.save(s);
			}
		});

		PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
			if (manager != null && player instanceof ServerPlayer serverPlayer) {
				manager.onBlockBroken(serverPlayer, pos, state, blockEntity);
			}
		});

		UseItemCallback.EVENT.register((player, level, hand) -> {
			if (player instanceof ServerPlayer serverPlayer && WorldEventManager.blockWaterBucket(serverPlayer, hand)) {
				return InteractionResult.FAIL;
			}
			return InteractionResult.PASS;
		});

		ServerPlayerEvents.JOIN.register(player -> {
			if (manager != null) {
				CurseManager.onJoin(player);
				WorldEventManager.onJoin(player);
			}
		});

		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
			if (manager != null) {
				CurseManager.onJoin(newPlayer);
			}
		});

		// Erscheinungen sollen nie dauerhaft in der Welt landen.
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			if (entity.entityTags().contains(PrivateEntities.GHOST_TAG) && PrivateEntities.viewerOf(entity) == null) {
				entity.discard();
			}
		});
		ServerEntityEvents.ENTITY_UNLOAD.register((entity, level) -> PrivateEntities.unbind(entity));
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> BlockEvents.onDeath(entity));

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> ChaosCommand.register(dispatcher));

		LOGGER.info("Chaos-Block geladen. Viel Glück.");
	}
}
