package de.suitcraft;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Was die Anzüge können (alles serverseitig, außer Klettern und Doppelsprung-Eingabe).
 */
public final class SuitAbilities {
	private static final Identifier MOD_JUMP = SuitcraftMod.id("suit_jump");
	private static final Identifier MOD_FALL = SuitcraftMod.id("suit_fall");
	private static final Identifier MOD_SPEED = SuitcraftMod.id("suit_speed");

	private static final Map<UUID, Integer> COOLDOWN = new HashMap<>();
	private static final Map<UUID, Integer> DANGER_COOLDOWN = new HashMap<>();
	private static final Map<UUID, Swing> SWINGS = new HashMap<>();
	private static final Map<UUID, Integer> FLAMES = new HashMap<>();
	private static final Map<UUID, Suit> LAST_SUIT = new HashMap<>();

	private record Swing(Vec3 anchor, int[] ticks) {
	}

	private SuitAbilities() {
	}

	public static void clear() {
		COOLDOWN.clear();
		DANGER_COOLDOWN.clear();
		SWINGS.clear();
		FLAMES.clear();
		LAST_SUIT.clear();
	}

	// ------------------------------------------------------------------
	// Laufende Effekte

	public static void tick(MinecraftServer server) {
		int now = server.getTickCount();
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			Suit suit = Suit.worn(player);
			updateAttributes(player, suit);
			tickSwing(player);
			tickFlames(player);
			if (suit == null || player.isSpectator()) {
				continue;
			}
			switch (suit) {
				case WEBRUNNER -> {
					if (now % 10 == 0) {
						dangerSense(player, now);
					}
				}
				case EMBER -> {
					if (now % 20 == 0) {
						hiddenEffect(player, MobEffects.FIRE_RESISTANCE, 60);
					}
					lavaWalker(player);
				}
				case SHADOW -> {
					if (now % 20 == 0) {
						hiddenEffect(player, MobEffects.NIGHT_VISION, 20 * 15);
					}
					if (player.isShiftKeyDown()) {
						if (now % 5 == 0) {
							hiddenEffect(player, MobEffects.INVISIBILITY, 15);
						}
						if (now % 10 == 0) {
							loseAggro(player);
						}
					}
				}
				case STORM -> {
				}
			}
		}
	}

	private static void hiddenEffect(ServerPlayer player, Holder<MobEffect> effect, int ticks) {
		player.addEffect(new MobEffectInstance(effect, ticks, 0, true, false, false));
	}

	private static void modifier(ServerPlayer player, Holder<Attribute> attribute, Identifier id, double amount, AttributeModifier.Operation op) {
		AttributeInstance instance = player.getAttribute(attribute);
		if (instance != null) {
			instance.addOrUpdateTransientModifier(new AttributeModifier(id, amount, op));
		}
	}

	private static void removeModifier(ServerPlayer player, Holder<Attribute> attribute, Identifier id) {
		AttributeInstance instance = player.getAttribute(attribute);
		if (instance != null) {
			instance.removeModifier(id);
		}
	}

	/** Set-Boni über Attribute, nur wenn sich der getragene Anzug ändert. */
	private static void updateAttributes(ServerPlayer player, Suit suit) {
		if (LAST_SUIT.get(player.getUUID()) == suit && LAST_SUIT.containsKey(player.getUUID())) {
			return;
		}
		LAST_SUIT.put(player.getUUID(), suit);
		removeModifier(player, Attributes.JUMP_STRENGTH, MOD_JUMP);
		removeModifier(player, Attributes.SAFE_FALL_DISTANCE, MOD_FALL);
		removeModifier(player, Attributes.MOVEMENT_SPEED, MOD_SPEED);
		if (suit == Suit.WEBRUNNER) {
			modifier(player, Attributes.JUMP_STRENGTH, MOD_JUMP, 0.45, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
			modifier(player, Attributes.SAFE_FALL_DISTANCE, MOD_FALL, 12.0, AttributeModifier.Operation.ADD_VALUE);
			modifier(player, Attributes.MOVEMENT_SPEED, MOD_SPEED, 0.15, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
		} else if (suit == Suit.STORM) {
			modifier(player, Attributes.SAFE_FALL_DISTANCE, MOD_FALL, 8.0, AttributeModifier.Operation.ADD_VALUE);
		} else if (suit == Suit.SHADOW) {
			modifier(player, Attributes.SAFE_FALL_DISTANCE, MOD_FALL, 4.0, AttributeModifier.Operation.ADD_VALUE);
		}
	}

	public static void onLeave(ServerPlayer player) {
		UUID id = player.getUUID();
		LAST_SUIT.remove(id);
		SWINGS.remove(id);
		FLAMES.remove(id);
	}

	private static void sendMotion(ServerPlayer player) {
		player.connection.send(new ClientboundSetEntityMotionPacket(player));
	}

	private static void line(ServerLevel level, SimpleParticleType type, Vec3 from, Vec3 to, int points) {
		for (int i = 0; i <= points; i++) {
			Vec3 at = from.lerp(to, i / (double) points);
			level.sendParticles(type, at.x, at.y, at.z, 1, 0, 0, 0, 0);
		}
	}

	// ------------------------------------------------------------------
	// Netzläufer

	private static void dangerSense(ServerPlayer player, int now) {
		Integer until = DANGER_COOLDOWN.get(player.getUUID());
		if (until != null && until > now) {
			return;
		}
		Vec3 look = player.getLookAngle().multiply(1, 0, 1).normalize();
		for (Mob mob : player.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(14))) {
			if (!(mob instanceof Enemy) || mob.getTarget() != player) {
				continue;
			}
			Vec3 toMob = mob.position().subtract(player.position()).multiply(1, 0, 1).normalize();
			if (look.dot(toMob) < -0.2) {
				mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, 50, 0, false, false));
				ServerPlayNetworking.send(player, SuitPayloads.Danger.INSTANCE);
				player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE,
					SoundSource.PLAYERS, 0.6F, 1.8F);
				DANGER_COOLDOWN.put(player.getUUID(), now + 60);
				return;
			}
		}
	}

	private static void webSwing(ServerPlayer player) {
		HitResult hit = player.pick(48.0, 0.0F, false);
		if (hit.getType() != HitResult.Type.BLOCK) {
			player.sendSystemMessage(Component.literal("§7Nichts zum Festhalten in Reichweite"), true);
			return;
		}
		Vec3 anchor = ((BlockHitResult) hit).getLocation();
		SWINGS.put(player.getUUID(), new Swing(anchor, new int[]{0}));
		ServerLevel level = player.level();
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SLIME_SQUISH_SMALL, SoundSource.PLAYERS, 1.0F, 0.6F);
		line(level, ParticleTypes.WHITE_ASH, player.getEyePosition(), anchor, 24);
		setCooldown(player, 10);
	}

	private static void tickSwing(ServerPlayer player) {
		Swing swing = SWINGS.get(player.getUUID());
		if (swing == null) {
			return;
		}
		Vec3 toAnchor = swing.anchor().subtract(player.position().add(0, 1, 0));
		double dist = toAnchor.length();
		if (++swing.ticks()[0] > 50 || dist < 2.5 || player.isShiftKeyDown() || !Suit.wears(player, Suit.WEBRUNNER)) {
			SWINGS.remove(player.getUUID());
			return;
		}
		Vec3 pull = toAnchor.normalize().scale(0.32);
		Vec3 motion = player.getDeltaMovement().scale(0.92).add(pull);
		if (motion.length() > 1.8) {
			motion = motion.normalize().scale(1.8);
		}
		player.setDeltaMovement(motion);
		player.resetFallDistance();
		sendMotion(player);
		if (swing.ticks()[0] % 2 == 0) {
			line(player.level(), ParticleTypes.WHITE_ASH, player.position().add(0, 1.2, 0), swing.anchor(), (int) Math.max(4, dist / 2));
		}
	}

	// ------------------------------------------------------------------
	// Glutpanzer

	private static void lavaWalker(ServerPlayer player) {
		if (!player.onGround() || player.isInLava()) {
			return;
		}
		ServerLevel level = player.level();
		BlockPos below = player.blockPosition().below();
		for (BlockPos pos : BlockPos.betweenClosed(below.offset(-2, 0, -2), below.offset(2, 0, 2))) {
			if (pos.distSqr(below) > 5) {
				continue;
			}
			BlockState state = level.getBlockState(pos);
			if (state.is(Blocks.LAVA) && state.getFluidState().isSource() && level.getBlockState(pos.above()).isAir()) {
				BlockPos at = pos.immutable();
				level.setBlockAndUpdate(at, Blocks.MAGMA_BLOCK.defaultBlockState());
				Tasks.later(20 * 8 + level.getRandom().nextInt(40), () -> {
					if (level.getBlockState(at).is(Blocks.MAGMA_BLOCK)) {
						level.setBlockAndUpdate(at, Blocks.LAVA.defaultBlockState());
					}
				});
			}
		}
	}

	private static void flamethrower(ServerPlayer player) {
		FLAMES.put(player.getUUID(), 30);
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0F, 0.7F);
		setCooldown(player, 20 * 8);
	}

	private static void tickFlames(ServerPlayer player) {
		Integer left = FLAMES.get(player.getUUID());
		if (left == null) {
			return;
		}
		if (left <= 0 || !Suit.wears(player, Suit.EMBER)) {
			FLAMES.remove(player.getUUID());
			return;
		}
		FLAMES.put(player.getUUID(), left - 1);
		ServerLevel level = player.level();
		Vec3 eye = player.getEyePosition().subtract(0, 0.3, 0);
		Vec3 look = player.getLookAngle();
		for (int i = 0; i < 6; i++) {
			Vec3 spread = look.add((level.getRandom().nextDouble() - 0.5) * 0.3, (level.getRandom().nextDouble() - 0.5) * 0.3,
				(level.getRandom().nextDouble() - 0.5) * 0.3).normalize();
			double d = 1 + level.getRandom().nextDouble() * 7;
			Vec3 at = eye.add(spread.scale(d));
			level.sendParticles(i % 3 == 0 ? ParticleTypes.LARGE_SMOKE : ParticleTypes.FLAME, at.x, at.y, at.z, 1, 0.05, 0.05, 0.05, 0.01);
		}
		if (left % 2 != 0) {
			return;
		}
		for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(9))) {
			if (target == player) {
				continue;
			}
			Vec3 to = target.position().add(0, target.getBbHeight() / 2, 0).subtract(eye);
			if (to.length() <= 9 && look.dot(to.normalize()) > 0.88) {
				target.igniteForTicks(100);
				target.hurtServer(level, level.damageSources().playerAttack(player), 2.0F);
			}
		}
	}

	/** Wer einen Glutpanzer-Träger schlägt, fängt Feuer. */
	public static void onDamaged(LivingEntity victim, DamageSource source) {
		if (victim instanceof ServerPlayer player && Suit.wears(player, Suit.EMBER)
			&& source.getEntity() instanceof LivingEntity attacker && source.getDirectEntity() == attacker && attacker != player) {
			attacker.igniteForTicks(80);
		}
	}

	// ------------------------------------------------------------------
	// Schattenmantel

	private static void loseAggro(ServerPlayer player) {
		for (Mob mob : player.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(32))) {
			if (mob.getTarget() == player) {
				mob.setTarget(null);
			}
		}
	}

	private static void blink(ServerPlayer player) {
		ServerLevel level = player.level();
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getLookAngle();
		HitResult hit = player.pick(16.0, 0.0F, false);
		Vec3 target = hit.getType() == HitResult.Type.BLOCK ? hit.getLocation().subtract(look.scale(0.6)) : eye.add(look.scale(16));
		BlockPos feet = BlockPos.containing(target.x, target.y - 1.0, target.z);
		BlockPos spot = null;
		for (int dy = 0; dy <= 2 && spot == null; dy++) {
			for (int sign : new int[]{1, -1}) {
				BlockPos pos = feet.above(dy * sign);
				if (level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
					&& level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty()) {
					spot = pos;
					break;
				}
			}
		}
		if (spot == null) {
			player.sendSystemMessage(Component.literal("§7Kein Platz zum Teleportieren"), true);
			return;
		}
		Vec3 from = player.position();
		level.sendParticles(ParticleTypes.REVERSE_PORTAL, from.x, from.y + 1, from.z, 30, 0.3, 0.6, 0.3, 0.05);
		player.teleportTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5);
		player.resetFallDistance();
		level.sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY() + 1, player.getZ(), 30, 0.3, 0.6, 0.3, 0.05);
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8F, 1.3F);
		setCooldown(player, 20 * 3);
	}

	// ------------------------------------------------------------------
	// Sturmflügel

	private static void gust(ServerPlayer player) {
		ServerLevel level = player.level();
		if (player.isFallFlying()) {
			Vec3 boost = player.getLookAngle().scale(1.4);
			player.setDeltaMovement(player.getDeltaMovement().add(boost));
			sendMotion(player);
			level.sendParticles(ParticleTypes.GUST, player.getX(), player.getY(), player.getZ(), 1, 0, 0, 0, 0);
			level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BREEZE_JUMP, SoundSource.PLAYERS, 1.0F, 1.0F);
			setCooldown(player, 20 * 2);
			return;
		}
		for (Entity entity : level.getEntities(player, player.getBoundingBox().inflate(8))) {
			if (!(entity instanceof LivingEntity) || entity.isSpectator()) {
				continue;
			}
			Vec3 away = entity.position().subtract(player.position()).multiply(1, 0, 1);
			double dist = Math.max(0.5, away.length());
			Vec3 push = away.normalize().scale(2.2 * (1.0 - dist / 10.0)).add(0, 0.7, 0);
			entity.push(push.x, push.y, push.z);
			entity.needsSync = true;
		}
		level.sendParticles(ParticleTypes.GUST_EMITTER_SMALL, player.getX(), player.getY() + 0.5, player.getZ(), 1, 0, 0, 0, 0);
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WIND_CHARGE_BURST.value(), SoundSource.PLAYERS, 1.0F, 0.9F);
		setCooldown(player, 20 * 5);
	}

	// ------------------------------------------------------------------
	// Eingaben vom Client

	private static boolean onCooldown(ServerPlayer player) {
		Integer until = COOLDOWN.get(player.getUUID());
		int now = player.level().getServer().getTickCount();
		if (until != null && until > now) {
			int seconds = (until - now + 19) / 20;
			player.sendSystemMessage(Component.literal("§7Noch " + seconds + " s"), true);
			return true;
		}
		return false;
	}

	private static void setCooldown(ServerPlayer player, int ticks) {
		COOLDOWN.put(player.getUUID(), player.level().getServer().getTickCount() + ticks);
	}

	public static void onAbility(ServerPlayer player) {
		Suit suit = Suit.worn(player);
		if (suit == null) {
			player.sendSystemMessage(Component.literal("§7Du brauchst alle 4 Teile eines Anzugs"), true);
			return;
		}
		if (player.isSpectator() || onCooldown(player)) {
			return;
		}
		switch (suit) {
			case WEBRUNNER -> webSwing(player);
			case EMBER -> flamethrower(player);
			case SHADOW -> blink(player);
			case STORM -> gust(player);
		}
	}

	public static void onDoubleJump(ServerPlayer player) {
		Suit suit = Suit.worn(player);
		if (suit != Suit.SHADOW && suit != Suit.STORM) {
			return;
		}
		player.resetFallDistance();
		ServerLevel level = player.level();
		level.sendParticles(suit == Suit.STORM ? ParticleTypes.GUST : ParticleTypes.SMOKE,
			player.getX(), player.getY(), player.getZ(), suit == Suit.STORM ? 1 : 12, 0.2, 0.0, 0.2, 0.02);
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
			suit == Suit.STORM ? SoundEvents.BREEZE_JUMP : SoundEvents.PHANTOM_FLAP, SoundSource.PLAYERS, 0.6F, 1.2F);
	}
}
