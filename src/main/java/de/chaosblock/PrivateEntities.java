package de.chaosblock;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/**
 * Entities, die nur ein einziger Spieler sehen kann. Der Server schickt sie
 * keinem anderen Client (siehe EntityMixin#broadcastToPlayer).
 */
public final class PrivateEntities {
	public static final String GHOST_TAG = "chaosblock_ghost";
	private static final Map<UUID, UUID> VIEWERS = new ConcurrentHashMap<>();

	private PrivateEntities() {
	}

	public static void bind(Entity entity, ServerPlayer viewer) {
		VIEWERS.put(entity.getUUID(), viewer.getUUID());
		entity.addTag(GHOST_TAG);
	}

	/** null = für alle sichtbar, sonst nur für diesen Spieler. */
	public static UUID viewerOf(Entity entity) {
		return VIEWERS.get(entity.getUUID());
	}

	public static void unbind(Entity entity) {
		VIEWERS.remove(entity.getUUID());
	}

	public static void clear() {
		VIEWERS.clear();
	}

	/**
	 * Erzeugt eine reglose, unverwundbare Erscheinung eines Mobs, die nur
	 * {@code viewer} sieht und nach {@code lifetime} Ticks verschwindet.
	 */
	public static <T extends Mob> T apparition(EntityType<T> type, ServerPlayer viewer, Vec3 pos, int lifetime) {
		ServerLevel level = viewer.level();
		T mob = type.create(level, EntitySpawnReason.EVENT);
		if (mob == null) {
			return null;
		}
		mob.setPos(pos.x, pos.y, pos.z);
		Vec3 look = viewer.getEyePosition().subtract(pos);
		float yaw = (float) (Math.atan2(look.z, look.x) * 180.0 / Math.PI) - 90.0F;
		mob.setYRot(yaw);
		mob.setYHeadRot(yaw);
		mob.setYBodyRot(yaw);
		mob.setNoAi(true);
		mob.setSilent(true);
		mob.setPermanentlyInvulnerable(true);
		bind(mob, viewer);
		level.addFreshEntity(mob);
		Scheduler.later(lifetime, mob::discard);
		return mob;
	}
}
