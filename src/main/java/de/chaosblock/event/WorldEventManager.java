package de.chaosblock.event;

import de.chaosblock.ChaosBlockMod;
import de.chaosblock.ChaosData;
import de.chaosblock.Fx;
import de.chaosblock.ModConfig;
import de.chaosblock.Places;
import de.chaosblock.Scheduler;
import de.chaosblock.ambient.AmbientManager;
import de.chaosblock.ambient.VisitorEntity;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.clock.ClockTimeMarkers;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;

/**
 * Welt-Events, die alle 10–20 Minuten einfach so passieren und alle Spieler treffen.
 */
public final class WorldEventManager {
	public record WorldEvent(String id, String title, String subtitle, int durationTicks, Consumer<List<ServerPlayer>> start) {
	}

	private static final RandomSource RANDOM = RandomSource.create();
	private static final List<WorldEvent> ALL = new ArrayList<>();
	private static final Map<UUID, Integer> JOIN_TICK = new HashMap<>();

	private static ChaosData data;
	private static ModConfig config;
	private static String active = "";
	private static int activeTicks;
	private static int pendingStart = -1;
	private static WorldEvent pending;
	private static List<ServerPlayer> affected = List.of();

	private WorldEventManager() {
	}

	public static void init(ChaosData chaosData, ModConfig modConfig) {
		data = chaosData;
		config = modConfig;
		active = "";
		activeTicks = 0;
		pending = null;
		pendingStart = -1;
		JOIN_TICK.clear();
		if (data.ticksUntilWorldEvent < 0) {
			data.ticksUntilWorldEvent = nextDelay();
		}
	}

	private static int nextDelay() {
		int min = Math.max(1, config.worldEventMinMinutes);
		int max = Math.max(min, config.worldEventMaxMinutes);
		return (min * 60 + RANDOM.nextInt((max - min) * 60 + 1)) * 20;
	}

	public static List<String> ids() {
		return ALL.stream().map(WorldEvent::id).toList();
	}

	public static boolean isActive(String id) {
		return activeTicks > 0 && active.equals(id);
	}

	public static boolean isLuckyStreak() {
		return isActive("glueck");
	}

	public static void onJoin(ServerPlayer player) {
		MinecraftServer server = ChaosBlockMod.server();
		JOIN_TICK.put(player.getUUID(), server == null ? 0 : server.getTickCount());
	}

	private static List<ServerPlayer> eligible(MinecraftServer server) {
		int grace = config.worldEventGraceMinutes * 60 * 20;
		List<ServerPlayer> list = new ArrayList<>();
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			Integer joined = JOIN_TICK.get(player.getUUID());
			if (joined == null) {
				JOIN_TICK.put(player.getUUID(), server.getTickCount());
				continue;
			}
			if (!player.isSpectator() && server.getTickCount() - joined >= grace) {
				list.add(player);
			}
		}
		return list;
	}

	public static void tick(MinecraftServer server) {
		if (data == null) {
			return;
		}
		if (pending != null && --pendingStart <= 0) {
			begin(server, pending);
			pending = null;
		}
		if (activeTicks > 0) {
			activeTicks--;
			whileActive(server);
			if (activeTicks == 0) {
				active = "";
			}
			return;
		}
		if (!config.worldEventsEnabled || pending != null) {
			return;
		}
		if (--data.ticksUntilWorldEvent <= 0) {
			data.ticksUntilWorldEvent = nextDelay();
			List<ServerPlayer> players = eligible(server);
			if (!players.isEmpty()) {
				List<WorldEvent> pool = new ArrayList<>(ALL);
				if (!config.ambientEntities) {
					pool.removeIf(e -> e.id().equals("nacht"));
				}
				announce(server, pool.get(RANDOM.nextInt(pool.size())));
			}
		}
	}

	/** Startet ein Event mit 5 Sekunden Vorwarnung. */
	public static boolean announce(MinecraftServer server, String id) {
		for (WorldEvent event : ALL) {
			if (event.id().equals(id)) {
				announce(server, event);
				return true;
			}
		}
		return false;
	}

	private static void announce(MinecraftServer server, WorldEvent event) {
		activeTicks = 0;
		active = "";
		pending = event;
		pendingStart = 100;
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			Fx.title(player, "§c⚠ Etwas kommt…", "§7");
			Fx.privateSound(player, SoundEvents.WARDEN_HEARTBEAT, player.position(), 1.0F, 0.7F);
		}
	}

	private static void begin(MinecraftServer server, WorldEvent event) {
		affected = eligible(server);
		if (affected.isEmpty()) {
			affected = new ArrayList<>(server.getPlayerList().getPlayers());
		}
		if (affected.isEmpty()) {
			return;
		}
		active = event.id();
		activeTicks = event.durationTicks();
		if (!event.id().equals("nacht")) {
			for (ServerPlayer player : affected) {
				Fx.title(player, event.title(), event.subtitle());
			}
		}
		try {
			event.start().accept(affected);
		} catch (Exception e) {
			ChaosBlockMod.LOGGER.error("Welt-Event {} ist fehlgeschlagen", event.id(), e);
		}
	}

	/** Verdunstendes Wasser während der Hitzewelle. true = Benutzung abbrechen. */
	public static boolean blockWaterBucket(ServerPlayer player, InteractionHand hand) {
		if (!isActive("hitze")) {
			return false;
		}
		ItemStack stack = player.getItemInHand(hand);
		if (!stack.is(Items.WATER_BUCKET)) {
			return false;
		}
		player.setItemInHand(hand, new ItemStack(Items.BUCKET));
		ServerLevel level = player.level();
		Vec3 at = player.getEyePosition().add(player.getLookAngle().scale(1.5));
		level.sendParticles(ParticleTypes.LARGE_SMOKE, at.x, at.y, at.z, 12, 0.3, 0.3, 0.3, 0.02);
		Fx.sound(level, at, SoundEvents.FIRE_EXTINGUISH, 1.0F, 1.2F);
		Fx.actionBar(player, "§6Das Wasser ist verdampft…");
		return true;
	}

	// ------------------------------------------------------------------

	private static List<ServerPlayer> online() {
		return affected.stream().filter(Fx::online).toList();
	}

	private static void whileActive(MinecraftServer server) {
		int t = activeTicks;
		switch (active) {
			case "blutmond" -> {
				if (t % 200 == 0) {
					for (ServerPlayer player : online()) {
						for (int i = 0; i < 2; i++) {
							spawnHostile(player, randomMonster(), 14, 24, true);
						}
					}
				}
			}
			case "saeureregen" -> {
				if (t % 20 == 0) {
					for (ServerPlayer player : online()) {
						ServerLevel level = player.level();
						if (level.isRainingAt(BlockPos.containing(player.getEyePosition()))) {
							player.hurtServer(level, level.damageSources().magic(), 1.5F);
							level.sendParticles(ParticleTypes.SMOKE, player.getX(), player.getY() + 2, player.getZ(), 6, 0.3, 0.2, 0.3, 0.01);
						}
					}
				}
			}
			case "eiszeit" -> {
				if (t % 20 == 0) {
					for (ServerPlayer player : online()) {
						if (!nearHeat(player)) {
							player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 1, true, true));
							player.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 40, 0, true, true));
							player.setTicksFrozen(Math.min(player.getTicksRequiredToFreeze(), player.getTicksFrozen() + 25));
						}
						ServerLevel level = player.level();
						level.sendParticles(ParticleTypes.SNOWFLAKE, player.getX(), player.getY() + 4, player.getZ(), 20, 6, 2, 6, 0.0);
					}
				}
			}
			case "erdbeben" -> {
				if (t % 10 == 0) {
					for (ServerPlayer player : online()) {
						quake(player);
					}
				}
			}
			default -> {
			}
		}
	}

	private static boolean nearHeat(ServerPlayer player) {
		ServerLevel level = player.level();
		for (BlockPos pos : BlockPos.betweenClosed(player.blockPosition().offset(-4, -2, -4), player.blockPosition().offset(4, 2, 4))) {
			BlockState state = level.getBlockState(pos);
			if (state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE) || state.is(Blocks.CAMPFIRE) || state.is(Blocks.SOUL_CAMPFIRE)
				|| state.is(Blocks.LAVA) || state.is(Blocks.MAGMA_BLOCK)) {
				return true;
			}
		}
		return false;
	}

	private static EntityType<? extends Mob> randomMonster() {
		List<EntityType<? extends Mob>> types = List.of(EntityTypes.ZOMBIE, EntityTypes.SKELETON, EntityTypes.SPIDER, EntityTypes.CREEPER);
		return types.get(RANDOM.nextInt(types.size()));
	}

	private static Mob spawnHostile(ServerPlayer player, EntityType<? extends Mob> type, int min, int max, boolean buffed) {
		Mob mob = Places.spawn(type, player.level(), Places.center(Places.around(player, min, max)));
		if (mob != null) {
			mob.setTarget(player);
			if (buffed) {
				mob.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 20 * 60 * 5, 0));
				mob.addEffect(new MobEffectInstance(MobEffects.SPEED, 20 * 60 * 5, 0));
				mob.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 20 * 60 * 5, 0));
			}
		}
		return mob;
	}

	private static void quake(ServerPlayer player) {
		ServerLevel level = player.level();
		BlockPos origin = player.blockPosition();
		for (int i = 0; i < 6; i++) {
			BlockPos pos = origin.offset(RANDOM.nextInt(17) - 8, RANDOM.nextInt(9) - 2, RANDOM.nextInt(17) - 8);
			BlockState state = level.getBlockState(pos);
			if (!state.isAir() && level.getBlockState(pos.below()).isAir() && level.getBlockEntity(pos) == null
				&& state.getDestroySpeed(level, pos) >= 0 && state.getFluidState().isEmpty()) {
				FallingBlockEntity.fall(level, pos, state);
			}
		}
		if (RANDOM.nextInt(3) == 0) {
			Fx.sound(level, player.position().add(0, -2, 0), SoundEvents.GENERIC_EXPLODE.value(), 0.4F, 0.4F);
		}
	}

	private static void add(String id, String title, String subtitle, int seconds, Consumer<List<ServerPlayer>> start) {
		ALL.add(new WorldEvent(id, title, subtitle, seconds * 20, start));
	}

	static {
		add("blutmond", "§4🌕 BLUTMOND", "§cDie Nacht gehört den Monstern", 5 * 60, players -> {
			MinecraftServer server = ChaosBlockMod.server();
			ServerLevel overworld = server.overworld();
			overworld.dimensionType().defaultClock().ifPresent(clock -> server.clockManager().moveToTimeMarker(clock, ClockTimeMarkers.NIGHT));
			for (ServerPlayer player : players) {
				Fx.screen(player, "bloodmoon", 5 * 60 * 20);
				Fx.privateSound(player, SoundEvents.WITHER_SPAWN, player.position(), 0.5F, 0.5F);
			}
		});

		add("meteoriten", "§6☄ METEORITENSCHAUER", "§eIn Deckung!", 25, players -> {
			for (ServerPlayer player : players) {
				Scheduler.repeat(15, 25, () -> {
					if (!Fx.online(player)) {
						return;
					}
					ServerLevel level = player.level();
					BlockPos target = Places.around(player, 4, 28);
					LargeFireball fireball = EntityTypes.FIREBALL.create(level, net.minecraft.world.entity.EntitySpawnReason.EVENT);
					if (fireball == null) {
						return;
					}
					fireball.setPos(target.getX() + 0.5, target.getY() + 40, target.getZ() + 0.5);
					fireball.setDeltaMovement(new Vec3(RANDOM.nextDouble() * 0.4 - 0.2, -1.6, RANDOM.nextDouble() * 0.4 - 0.2));
					level.addFreshEntity(fireball);
					if (RANDOM.nextInt(4) == 0) {
						// Manche Meteoriten bringen etwas mit.
						Scheduler.later(60, () -> {
							BlockPos crater = Places.surface(level, target.getX(), target.getZ()).below();
							BlockState ore = switch (RANDOM.nextInt(10)) {
								case 0 -> Blocks.DIAMOND_ORE.defaultBlockState();
								case 1, 2 -> Blocks.GOLD_ORE.defaultBlockState();
								default -> Blocks.IRON_ORE.defaultBlockState();
							};
							if (level.getBlockEntity(crater) == null && level.getBlockState(crater).getDestroySpeed(level, crater) >= 0) {
								level.setBlockAndUpdate(crater, ore);
							}
						});
					}
				});
			}
		});

		add("erdbeben", "§8🌋 ERDBEBEN", "§7Alles wackelt!", 15, players -> {
			for (ServerPlayer player : players) {
				Fx.screen(player, "quake", 15 * 20);
			}
		});

		add("jagd", "§c🐺 DIE JAGD", "§7Einer von euch ist die Beute…", 3 * 60, players -> {
			ServerPlayer prey = players.get(RANDOM.nextInt(players.size()));
			prey.addEffect(new MobEffectInstance(MobEffects.GLOWING, 3 * 60 * 20, 0));
			for (ServerPlayer player : players) {
				if (player != prey) {
					Fx.title(player, "§c🐺 DIE JAGD", "§7" + prey.getName().getString() + " wird gejagt!");
				}
			}
			Fx.title(prey, "§c🐺 DU BIST DIE BEUTE", "§7Lauf!");
			List<Wolf> pack = new ArrayList<>();
			for (int i = 0; i < 4; i++) {
				Wolf wolf = Places.spawn(EntityTypes.WOLF, prey.level(), Places.center(Places.around(prey, 14, 20)));
				if (wolf != null) {
					wolf.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 3 * 60 * 20, 1));
					wolf.addEffect(new MobEffectInstance(MobEffects.SPEED, 3 * 60 * 20, 0));
					wolf.setTarget(prey);
					pack.add(wolf);
				}
			}
			Scheduler.repeat(40, 3 * 60 * 20 / 40, () -> pack.forEach(w -> {
				if (!w.isRemoved() && Fx.online(prey) && w.level() == prey.level()) {
					w.setTarget(prey);
				}
			}));
			Scheduler.later(3 * 60 * 20 + 2, () -> pack.forEach(w -> {
				if (!w.isRemoved()) {
					((ServerLevel) w.level()).sendParticles(ParticleTypes.POOF, w.getX(), w.getY() + 0.5, w.getZ(), 10, 0.3, 0.3, 0.3, 0.02);
					w.discard();
				}
			}));
		});

		add("saeureregen", "§a☔ SÄUREREGEN", "§2Schnell unter ein Dach!", 2 * 60, players ->
			ChaosBlockMod.server().setWeatherParameters(0, 2 * 60 * 20 + 100, true, false));

		add("invasion", "§4⚔ MOB-INVASION", "§cSie kommen. Zwei Wellen.", 90, players -> {
			for (ServerPlayer player : players) {
				for (int i = 0; i < 6; i++) {
					spawnHostile(player, randomMonster(), 16, 24, false);
				}
			}
			Scheduler.later(40 * 20, () -> {
				for (ServerPlayer player : online()) {
					Fx.title(player, "§4⚔ ZWEITE WELLE", "§cDiesmal sind sie gerüstet");
					for (int i = 0; i < 8; i++) {
						Mob mob = spawnHostile(player, RANDOM.nextBoolean() ? EntityTypes.ZOMBIE : EntityTypes.SKELETON, 14, 22, true);
						if (mob != null) {
							mob.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
							mob.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
							mob.setDropChance(EquipmentSlot.HEAD, 0.0F);
							mob.setDropChance(EquipmentSlot.CHEST, 0.0F);
						}
					}
				}
			});
		});

		add("schwerkraft", "§d🌌 SCHWERKRAFT-AUSFALL", "§5Festhalten!", 20, players -> {
			for (ServerPlayer player : players) {
				player.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 20 * 20, 0));
			}
		});

		add("tausch", "§b🔀 SPIELER-TAUSCH", "§3Wo bin ich?!", 1, players -> {
			if (players.size() < 2) {
				ServerPlayer player = players.get(0);
				BlockPos pos = Places.around(player, 80, 150);
				player.teleportTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
				return;
			}
			List<Vec3> positions = new ArrayList<>();
			List<ServerLevel> levels = new ArrayList<>();
			for (ServerPlayer player : players) {
				positions.add(player.position());
				levels.add(player.level());
			}
			List<Integer> order = new ArrayList<>();
			for (int i = 0; i < players.size(); i++) {
				order.add(i);
			}
			// Jeder landet garantiert woanders.
			Collections.rotate(order, 1 + RANDOM.nextInt(players.size() - 1));
			for (int i = 0; i < players.size(); i++) {
				int j = order.get(i);
				Vec3 pos = positions.get(j);
				players.get(i).teleportTo(levels.get(j), pos.x, pos.y, pos.z, Set.of(), players.get(i).getYRot(), players.get(i).getXRot(), true);
			}
		});

		add("eiszeit", "§b❄ EISZEIT", "§3Such dir ein Feuer!", 3 * 60, players -> {
			for (ServerPlayer player : players) {
				ServerLevel level = player.level();
				BlockPos origin = player.blockPosition();
				for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-12, -4, -12), origin.offset(12, 4, 12))) {
					BlockState state = level.getBlockState(pos);
					if (state.getFluidState().is(Fluids.WATER) && state.is(Blocks.WATER) && level.getBlockState(pos.above()).isAir()) {
						level.setBlockAndUpdate(pos, Blocks.ICE.defaultBlockState());
					} else if (state.isAir() && level.canSeeSky(pos) && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), net.minecraft.core.Direction.UP)
						&& RANDOM.nextInt(3) == 0) {
						level.setBlockAndUpdate(pos, Blocks.SNOW.defaultBlockState());
					}
				}
			}
		});

		add("fremder", "§7👤 …", "§8Du bist nicht allein", 60, players -> {
			for (ServerPlayer player : players) {
				float yaw = player.getYRot() + (RANDOM.nextFloat() * 50.0F - 25.0F);
				BlockPos pos = Places.atAngle(player, yaw, 30 + RANDOM.nextInt(10));
				VisitorEntity stranger = VisitorEntity.summon(player, Places.center(pos), VisitorEntity.Mode.STRANGER, 20 * 45);
				if (stranger != null) {
					stranger.setOnVanish(() -> Scheduler.later(20 * (6 + RANDOM.nextInt(8)), () -> {
						if (!Fx.online(player)) {
							return;
						}
						Vec3 back = player.position().subtract(player.getLookAngle().multiply(2.5, 0, 2.5));
						VisitorEntity behind = VisitorEntity.summon(player, back, VisitorEntity.Mode.BEHIND, 20 * 8);
						if (behind != null) {
							behind.setGlowingEyes(false);
							Fx.privateSound(player, SoundEvents.STONE_STEP, back, 0.6F, 0.8F);
						}
					}));
				}
			}
		});

		add("hitze", "§6🔥 HITZEWELLE", "§eWasser verdunstet, Hunger steigt", 3 * 60, players -> {
			for (ServerPlayer player : players) {
				player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 3 * 60 * 20, 1));
			}
		});

		add("glueck", "§a🍀 GLÜCKSSTRÄHNE", "§22 Minuten lang nur gute Events!", 2 * 60, players -> {
			for (ServerPlayer player : players) {
				Fx.sound(player.level(), player.position(), SoundEvents.PLAYER_LEVELUP, 1.0F, 1.5F);
			}
		});

		add("nacht", "", "", 30, players -> {
			for (ServerPlayer player : players) {
				AmbientManager.night(player);
			}
		});
	}
}
