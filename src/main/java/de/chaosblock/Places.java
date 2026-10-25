package de.chaosblock;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * Hilfen zum Finden von Positionen und Erzeugen von Entities.
 */
public final class Places {
	private Places() {
	}

	/** Höchster begehbarer Punkt an x/z (über dem Boden). */
	public static BlockPos surface(ServerLevel level, int x, int z) {
		int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
		return new BlockPos(x, y, z);
	}

	/**
	 * Ein Punkt in {@code min..max} Blöcken Entfernung. Unter der Erde (keine
	 * Sicht zum Himmel) bleibt er auf Spielerhöhe, sonst auf der Oberfläche.
	 */
	public static BlockPos around(ServerPlayer player, double min, double max) {
		ServerLevel level = player.level();
		RandomSource random = player.getRandom();
		double angle = random.nextDouble() * Math.PI * 2;
		double dist = min + random.nextDouble() * (max - min);
		int x = (int) Math.floor(player.getX() + Math.cos(angle) * dist);
		int z = (int) Math.floor(player.getZ() + Math.sin(angle) * dist);
		return groundNear(level, player, x, z);
	}

	/** Wie {@link #around}, aber in einem bestimmten Winkel (Grad, Minecraft-Yaw). */
	public static BlockPos atAngle(ServerPlayer player, float yawDegrees, double dist) {
		double rad = Math.toRadians(yawDegrees);
		int x = (int) Math.floor(player.getX() - Math.sin(rad) * dist);
		int z = (int) Math.floor(player.getZ() + Math.cos(rad) * dist);
		return groundNear(player.level(), player, x, z);
	}

	private static BlockPos groundNear(ServerLevel level, ServerPlayer player, int x, int z) {
		if (level.canSeeSky(player.blockPosition())) {
			return surface(level, x, z);
		}
		BlockPos start = new BlockPos(x, player.getBlockY(), z);
		for (int dy = 0; dy <= 6; dy++) {
			for (int sign : new int[]{1, -1}) {
				BlockPos pos = start.above(dy * sign);
				if (standable(level, pos)) {
					return pos;
				}
			}
		}
		return player.blockPosition();
	}

	public static boolean standable(ServerLevel level, BlockPos pos) {
		return level.getBlockState(pos).isAir()
			&& level.getBlockState(pos.above()).isAir()
			&& level.getBlockState(pos.below()).isSolidRender();
	}

	public static Vec3 center(BlockPos pos) {
		return new Vec3(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
	}

	public static <T extends Entity> T spawn(EntityType<T> type, ServerLevel level, Vec3 pos) {
		T entity = type.create(level, EntitySpawnReason.EVENT);
		if (entity == null) {
			return null;
		}
		entity.setPos(pos.x, pos.y, pos.z);
		entity.setYRot(level.getRandom().nextFloat() * 360.0F);
		level.addFreshEntity(entity);
		return entity;
	}

	public static void dropItem(ServerLevel level, Vec3 pos, ItemStack stack) {
		ItemEntity item = new ItemEntity(level, pos.x, pos.y, pos.z, stack);
		item.setDefaultPickUpDelay();
		level.addFreshEntity(item);
	}

	/** Ins Inventar, oder vor die Füße, wenn es voll ist. */
	public static void give(ServerPlayer player, ItemStack stack) {
		if (!player.getInventory().add(stack) && !stack.isEmpty()) {
			dropItem(player.level(), player.position(), stack);
		}
	}

	/** Ob der Spieler (ungefähr) in Richtung {@code target} schaut. */
	public static boolean isLookingAt(ServerPlayer player, Vec3 target, double maxDegrees) {
		Vec3 toTarget = target.subtract(player.getEyePosition()).normalize();
		double dot = player.getLookAngle().normalize().dot(toTarget);
		return dot > Math.cos(Math.toRadians(maxDegrees));
	}
}
