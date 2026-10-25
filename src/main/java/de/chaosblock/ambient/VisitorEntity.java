package de.chaosblock.ambient;

import de.chaosblock.Fx;
import de.chaosblock.Places;
import de.chaosblock.PrivateEntities;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Die Gestalt mit den weißen Augen. Existiert immer nur für genau einen Spieler.
 */
public class VisitorEntity extends Monster {
	public enum Mode {
		/** Steht in der Ferne und starrt. Verschwindet beim Hinschauen. */
		WATCH,
		/** Steht direkt hinter dem Spieler. Verschwindet beim Umdrehen. */
		BEHIND,
		/** Greift wirklich an. */
		HUNT,
		/** Wie WATCH, aber ohne leuchtende Augen ("Der Fremde"). */
		STRANGER
	}

	private static final EntityDataAccessor<Boolean> DATA_EYES = SynchedEntityData.defineId(VisitorEntity.class, EntityDataSerializers.BOOLEAN);

	private UUID targetId;
	private Mode mode = Mode.WATCH;
	private int life;
	private int maxLife = 20 * 30;
	private int lookTicks;
	private int nextBlink = 100;
	private Runnable onVanish;

	public VisitorEntity(EntityType<? extends Monster> type, Level level) {
		super(type, level);
		this.xpReward = 0;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
			.add(Attributes.MAX_HEALTH, 60.0)
			.add(Attributes.MOVEMENT_SPEED, 0.36)
			.add(Attributes.ATTACK_DAMAGE, 9.0)
			.add(Attributes.FOLLOW_RANGE, 64.0)
			.add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
			.add(Attributes.ARMOR, 6.0);
	}

	/** Erzeugt eine Gestalt, die nur {@code viewer} sieht. */
	public static VisitorEntity summon(ServerPlayer viewer, Vec3 pos, Mode mode, int lifeTicks) {
		ServerLevel level = viewer.level();
		VisitorEntity visitor = de.chaosblock.ModEntities.VISITOR.create(level, EntitySpawnReason.EVENT);
		if (visitor == null) {
			return null;
		}
		visitor.targetId = viewer.getUUID();
		visitor.mode = mode;
		visitor.maxLife = lifeTicks;
		visitor.entityData.set(DATA_EYES, mode != Mode.STRANGER);
		visitor.setPos(pos.x, pos.y, pos.z);
		visitor.faceTarget(viewer);
		visitor.setPersistenceRequired();
		if (mode != Mode.HUNT) {
			visitor.setNoAi(true);
			visitor.setPermanentlyInvulnerable(true);
			visitor.setSilent(true);
		} else {
			visitor.setTarget(viewer);
		}
		PrivateEntities.bind(visitor, viewer);
		level.addFreshEntity(visitor);
		return visitor;
	}

	public void setOnVanish(Runnable onVanish) {
		this.onVanish = onVanish;
	}

	public Mode mode() {
		return this.mode;
	}

	public UUID targetId() {
		return this.targetId;
	}

	public boolean hasGlowingEyes() {
		return this.entityData.get(DATA_EYES);
	}

	public void setGlowingEyes(boolean eyes) {
		this.entityData.set(DATA_EYES, eyes);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_EYES, true);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.15, true));
	}

	private ServerPlayer target() {
		if (this.targetId == null || !(this.level() instanceof ServerLevel level)) {
			return null;
		}
		ServerPlayer player = level.getServer().getPlayerList().getPlayer(this.targetId);
		return player != null && player.level() == this.level() && Fx.online(player) ? player : null;
	}

	private void faceTarget(ServerPlayer player) {
		Vec3 diff = player.getEyePosition().subtract(this.getEyePosition());
		float yaw = (float) (Math.atan2(diff.z, diff.x) * 180.0 / Math.PI) - 90.0F;
		float pitch = (float) -(Math.atan2(diff.y, diff.horizontalDistance()) * 180.0 / Math.PI);
		this.setYRot(yaw);
		this.setYHeadRot(yaw);
		this.setYBodyRot(yaw);
		this.setXRot(pitch);
	}

	@Override
	public void tick() {
		super.tick();
		if (this.level().isClientSide()) {
			return;
		}
		ServerPlayer player = this.target();
		if (player == null || player.isSpectator()) {
			this.discard();
			return;
		}
		if (++this.life > this.maxLife) {
			this.vanish(player, this.mode == Mode.HUNT);
			return;
		}

		switch (this.mode) {
			case WATCH, STRANGER -> {
				this.faceTarget(player);
				double dist = this.distanceTo(player);
				boolean looked = Places.isLookingAt(player, this.getEyePosition(), 10) && player.hasLineOfSight(this);
				this.lookTicks = looked ? this.lookTicks + 1 : 0;
				if (dist < 16 || this.lookTicks > 8) {
					this.vanish(player, false);
				}
			}
			case BEHIND -> {
				this.faceTarget(player);
				if (this.life > 10 && Places.isLookingAt(player, this.getEyePosition(), 35)) {
					this.vanish(player, false);
				}
			}
			case HUNT -> {
				if (this.getTarget() != player) {
					this.setTarget(player);
				}
				// Zwischendurch taucht er einfach hinter seinem Opfer auf.
				if (--this.nextBlink <= 0) {
					this.nextBlink = 80 + this.random.nextInt(80);
					if (this.distanceTo(player) > 5) {
						this.blinkBehind(player);
					}
				}
			}
		}
	}

	private void blinkBehind(ServerPlayer player) {
		Vec3 behind = player.position().subtract(player.getLookAngle().multiply(2.0, 0, 2.0));
		BlockPos pos = BlockPos.containing(behind);
		if (Places.standable((ServerLevel) this.level(), pos)) {
			this.teleportTo(behind.x, pos.getY(), behind.z);
			Fx.privateSound(player, SoundEvents.ENDERMAN_TELEPORT, behind, 0.6F, 0.5F);
		}
	}

	/** Verschwindet ohne Spur. */
	public void vanish(ServerPlayer player, boolean withLightning) {
		if (player != null) {
			Fx.privateSound(player, withLightning ? SoundEvents.LIGHTNING_BOLT_THUNDER : SoundEvents.AMBIENT_CAVE.value(),
				this.position(), withLightning ? 0.8F : 0.4F, 0.6F);
			if (withLightning) {
				this.strikeVisual(player);
			}
		}
		if (this.onVanish != null) {
			this.onVanish.run();
			this.onVanish = null;
		}
		this.discard();
	}

	private void strikeVisual(ServerPlayer player) {
		LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(this.level(), EntitySpawnReason.EVENT);
		if (bolt != null) {
			bolt.setPos(this.getX(), this.getY(), this.getZ());
			bolt.setVisualOnly(true);
			PrivateEntities.bind(bolt, player);
			this.level().addFreshEntity(bolt);
		}
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		if (this.mode != Mode.HUNT) {
			return false;
		}
		boolean hurt = super.hurtServer(level, source, damage);
		ServerPlayer player = this.target();
		// Manchmal verschwindet er getroffen im Blitz und taucht woanders wieder auf.
		if (hurt && this.isAlive() && player != null && this.random.nextFloat() < 0.3F) {
			this.strikeVisual(player);
			BlockPos away = Places.around(player, 12, 20);
			this.teleportTo(away.getX() + 0.5, away.getY(), away.getZ() + 0.5);
			this.nextBlink = 60;
		}
		return hurt;
	}

	@Override
	public void die(DamageSource source) {
		super.die(source);
		if (this.level() instanceof ServerLevel level && this.mode == Mode.HUNT) {
			AmbientManager.onDefeated(this, level);
		}
	}

	@Override
	public boolean shouldBeSaved() {
		return false;
	}

	@Override
	public boolean removeWhenFarAway(double distSqr) {
		return false;
	}

	@Override
	public boolean fireImmune() {
		return true;
	}

	@Override
	public boolean causeFallDamage(double fallDistance, float damageModifier, DamageSource damageSource) {
		return false;
	}

	@Override
	public boolean isPushable() {
		return this.mode == Mode.HUNT;
	}

	@Override
	public void checkDespawn() {
		// Verschwindet nur, wenn er selbst es will.
	}
}
