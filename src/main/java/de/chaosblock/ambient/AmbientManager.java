package de.chaosblock.ambient;

import de.chaosblock.ChaosBlockMod;
import de.chaosblock.ChaosData;
import de.chaosblock.Fx;
import de.chaosblock.ModItems;
import de.chaosblock.Places;
import de.chaosblock.Scheduler;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.entity.SignTextSlot;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;

/**
 * Er beobachtet jeden Spieler einzeln. Je länger jemand spielt, desto näher kommt er.
 * Alles hier ist nur für den jeweils betroffenen Spieler sichtbar und hörbar.
 */
public final class AmbientManager {
	private static final RandomSource RANDOM = RandomSource.create();
	private static final Map<UUID, VisitorEntity> ACTIVE = new HashMap<>();
	private static final String NAME = "Herobrine";

	private AmbientManager() {
	}

	public static void clear() {
		ACTIVE.clear();
	}

	private static boolean enabled() {
		return ChaosBlockMod.config().ambientEntities;
	}

	/** 0 = noch nichts, 1 = Beobachten, 2 = Spuren, 3 = Psychoterror, 4 = Jagd. */
	public static int phase(ChaosData.PlayerData pd) {
		double minutes = pd.playTicks / 1200.0 * Math.max(0.01, ChaosBlockMod.config().ambientSpeed);
		if (minutes >= 180) {
			return 4;
		}
		if (minutes >= 120) {
			return 3;
		}
		if (minutes >= 60) {
			return 2;
		}
		if (minutes >= 30) {
			return 1;
		}
		return 0;
	}

	public static int phase(ServerPlayer player) {
		return phase(ChaosBlockMod.data().player(player.getUUID()));
	}

	/** Setzt die Spielzeit so, dass der Spieler in der gewünschten Phase ist (für Tests). */
	public static void setPhase(ServerPlayer player, int phase) {
		int[] minutes = {0, 30, 60, 120, 180};
		ChaosData.PlayerData pd = ChaosBlockMod.data().player(player.getUUID());
		pd.playTicks = (long) (minutes[Math.max(0, Math.min(4, phase))] * 1200 / Math.max(0.01, ChaosBlockMod.config().ambientSpeed));
		pd.ambientCooldown = 0;
		pd.ambientNextAction = 20 * 5;
	}

	public static boolean oracleIsWatched(ServerPlayer player) {
		return enabled() && phase(player) >= 2 && RANDOM.nextFloat() < 0.2F;
	}

	public static void tick(MinecraftServer server) {
		ChaosData data = ChaosBlockMod.data();
		boolean second = server.getTickCount() % 20 == 0;
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (player.isSpectator()) {
				continue;
			}
			ChaosData.PlayerData pd = data.player(player.getUUID());
			pd.playTicks++;
			if (!second || !enabled()) {
				continue;
			}
			if (pd.ambientCooldown > 0) {
				pd.ambientCooldown -= 20;
				continue;
			}
			int phase = phase(pd);
			if (phase == 0) {
				continue;
			}
			if (pd.ambientNextAction == 0) {
				pd.ambientNextAction = nextDelay(phase);
			}
			pd.ambientNextAction -= 20;
			if (pd.ambientNextAction <= 0) {
				pd.ambientNextAction = nextDelay(phase);
				if (!busy(player)) {
					act(player, phase);
				}
			}
		}
	}

	private static int nextDelay(int phase) {
		int min = switch (phase) {
			case 1 -> 4;
			case 2 -> 3;
			case 3 -> 2;
			default -> 2;
		};
		int max = switch (phase) {
			case 1 -> 8;
			case 2 -> 6;
			case 3 -> 5;
			default -> 4;
		};
		return (min * 60 + RANDOM.nextInt((max - min) * 60 + 1)) * 20;
	}

	private static boolean busy(ServerPlayer player) {
		VisitorEntity active = ACTIVE.get(player.getUUID());
		return active != null && !active.isRemoved();
	}

	public static void act(ServerPlayer player, int phase) {
		switch (phase) {
			case 1 -> watch(player, VisitorEntity.Mode.WATCH);
			case 2 -> {
				if (RANDOM.nextInt(3) == 0) {
					watch(player, VisitorEntity.Mode.WATCH);
				} else {
					trace(player);
				}
			}
			case 3 -> terror(player);
			default -> {
				if (RANDOM.nextFloat() < 0.2F) {
					hunt(player);
				} else {
					terror(player);
				}
			}
		}
	}

	private static void terror(ServerPlayer player) {
		switch (RANDOM.nextInt(6)) {
			case 0 -> watch(player, VisitorEntity.Mode.WATCH);
			case 1 -> trace(player);
			case 2 -> behind(player);
			case 3 -> fakeJoin(player);
			case 4 -> door(player);
			default -> {
				Fx.privateSound(player, SoundEvents.AMBIENT_CAVE, player.position().add(0, 2, 0), 1.0F, 0.5F);
				behind(player);
			}
		}
	}

	private static void track(ServerPlayer player, VisitorEntity visitor) {
		if (visitor != null) {
			ACTIVE.put(player.getUUID(), visitor);
		}
	}

	// ------------------------------------------------------------------
	// Erscheinungen

	/** Steht weit weg im Blickfeld und starrt. */
	public static void watch(ServerPlayer player, VisitorEntity.Mode mode) {
		float yaw = player.getYRot() + (RANDOM.nextFloat() * 70.0F - 35.0F);
		BlockPos pos = Places.atAngle(player, yaw, 32 + RANDOM.nextInt(16));
		VisitorEntity visitor = VisitorEntity.summon(player, Places.center(pos), mode, 20 * 40);
		track(player, visitor);
	}

	/** Steht plötzlich direkt hinter dir. */
	public static void behind(ServerPlayer player) {
		Vec3 back = player.position().subtract(player.getLookAngle().multiply(2.5, 0, 2.5));
		BlockPos pos = BlockPos.containing(back);
		if (!Places.standable(player.level(), pos)) {
			watch(player, VisitorEntity.Mode.WATCH);
			return;
		}
		Fx.privateSound(player, SoundEvents.STONE_STEP, back, 0.5F, 0.7F);
		VisitorEntity visitor = VisitorEntity.summon(player, new Vec3(back.x, pos.getY(), back.z), VisitorEntity.Mode.BEHIND, 20 * 8);
		track(player, visitor);
	}

	/** Kurzes Aufblitzen am Bildrand (z. B. als Halluzination). */
	public static void fakeGlimpse(ServerPlayer player) {
		if (busy(player)) {
			return;
		}
		float side = RANDOM.nextBoolean() ? 60.0F : -60.0F;
		BlockPos pos = Places.atAngle(player, player.getYRot() + side, 12 + RANDOM.nextInt(8));
		VisitorEntity visitor = VisitorEntity.summon(player, Places.center(pos), VisitorEntity.Mode.BEHIND, 20 * 3);
		track(player, visitor);
	}

	/** Phase 4: er greift wirklich an. */
	public static void hunt(ServerPlayer player) {
		Fx.privateSound(player, SoundEvents.AMBIENT_CAVE, player.position(), 1.0F, 0.4F);
		player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 20 * 8, 0, true, false));
		Fx.chat(player, Component.literal("<" + NAME + "> ").append(Component.literal("Gefunden.").withStyle(ChatFormatting.DARK_RED)));
		Scheduler.later(40, () -> {
			if (!Fx.online(player)) {
				return;
			}
			BlockPos pos = Places.around(player, 10, 16);
			VisitorEntity visitor = VisitorEntity.summon(player, Places.center(pos), VisitorEntity.Mode.HUNT, 20 * 90);
			track(player, visitor);
		});
	}

	public static void onDefeated(VisitorEntity visitor, ServerLevel level) {
		UUID target = visitor.targetId();
		ACTIVE.remove(target);
		visitor.spawnAtLocation(level, new ItemStack(ModItems.WHITE_EYE));
		ServerPlayer player = level.getServer().getPlayerList().getPlayer(target);
		ChaosData.PlayerData pd = ChaosBlockMod.data().player(target);
		pd.ambientCooldown = (60 + RANDOM.nextInt(61)) * 60 * 20;
		if (player != null) {
			Fx.title(player, "§f…", "§7Für den Moment ist er weg.");
		}
	}

	// ------------------------------------------------------------------
	// Spuren (Geisterblöcke, nur für diesen Spieler)

	public static void trace(ServerPlayer player) {
		switch (RANDOM.nextInt(5)) {
			case 0 -> {
				if (!leaflessTree(player)) {
					tunnel(player);
				}
			}
			case 1 -> tunnel(player);
			case 2 -> pyramid(player);
			case 3 -> sign(player);
			default -> {
				if (torchesOut(player, 14) == 0) {
					sign(player);
				}
			}
		}
	}

	private static boolean leaflessTree(ServerPlayer player) {
		ServerLevel level = player.level();
		BlockPos origin = player.blockPosition();
		List<BlockPos> leaves = new ArrayList<>();
		for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-20, -6, -20), origin.offset(20, 14, 20))) {
			if (level.getBlockState(pos).is(net.minecraft.tags.BlockTags.LEAVES)) {
				leaves.add(pos.immutable());
			}
		}
		if (leaves.isEmpty()) {
			return false;
		}
		BlockPos center = leaves.get(RANDOM.nextInt(leaves.size()));
		for (BlockPos leaf : leaves) {
			if (leaf.distSqr(center) <= 5 * 5) {
				Fx.ghostBlock(player, leaf, Blocks.AIR.defaultBlockState());
			}
		}
		return true;
	}

	private static void tunnel(ServerPlayer player) {
		ServerLevel level = player.level();
		BlockPos start = Places.around(player, 8, 14);
		Direction dir = Direction.Plane.HORIZONTAL.getRandomDirection(RANDOM);
		for (int i = 0; i < 14; i++) {
			BlockPos base = start.relative(dir, i).below(1 + i / 2);
			for (int a = 0; a < 2; a++) {
				for (int b = 0; b < 2; b++) {
					BlockPos pos = base.relative(dir.getClockWise(), a).above(b);
					if (!level.getBlockState(pos).isAir()) {
						Fx.ghostBlock(player, pos, Blocks.AIR.defaultBlockState());
					}
				}
			}
		}
	}

	private static void pyramid(ServerPlayer player) {
		ServerLevel level = player.level();
		BlockPos base = Places.around(player, 16, 26);
		for (int layer = 0; layer < 4; layer++) {
			int r = 3 - layer;
			for (int dx = -r; dx <= r; dx++) {
				for (int dz = -r; dz <= r; dz++) {
					BlockPos pos = base.offset(dx, layer, dz);
					if (level.getBlockState(pos).canBeReplaced()) {
						Fx.ghostBlock(player, pos, Blocks.SAND.defaultBlockState());
					}
				}
			}
		}
		Fx.ghostBlock(player, base.above(4), Blocks.REDSTONE_TORCH.defaultBlockState());
	}

	private static void sign(ServerPlayer player) {
		ServerLevel level = player.level();
		float yaw = player.getYRot() + (RANDOM.nextFloat() * 60.0F - 30.0F);
		BlockPos pos = Places.atAngle(player, yaw, 6 + RANDOM.nextInt(4));
		if (!level.getBlockState(pos).canBeReplaced()) {
			return;
		}
		// Schild zeigt zum Spieler.
		Vec3 diff = player.position().subtract(Places.center(pos));
		float facing = (float) (Math.atan2(diff.z, diff.x) * 180.0 / Math.PI) - 90.0F;
		int rotation = Math.floorMod(Math.round(facing / 22.5F) + 8, 16);
		BlockState state = Blocks.OAK_SIGN.defaultBlockState().setValue(StandingSignBlock.ROTATION, rotation);
		Fx.ghostBlock(player, pos, state);

		String[][] texts = {{"", "Ich sehe", "dich", ""}, {"", "Dreh dich", "nicht um", ""}, {"Warum", "bist du", "noch", "hier?"}, {"", "Er ist", "hier", ""}};
		String[] text = texts[RANDOM.nextInt(texts.length)];
		List<Component> lines = new ArrayList<>();
		for (String line : text) {
			lines.add(Component.literal(line));
		}
		SignBlockEntity sign = new SignBlockEntity(pos, state);
		sign.setLevel(level);
		sign.setText(new SignText(lines, lines, DyeColor.BLACK, false), SignTextSlot.FRONT);
		player.connection.send(ClientboundBlockEntityDataPacket.create(sign));
	}

	/** Macht Fackeln in der Nähe für diesen Spieler unsichtbar. Gibt die Anzahl zurück. */
	public static int torchesOut(ServerPlayer player, int radius) {
		ServerLevel level = player.level();
		BlockPos origin = player.blockPosition();
		int count = 0;
		for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-radius, -6, -radius), origin.offset(radius, 6, radius))) {
			BlockState state = level.getBlockState(pos);
			if (state.is(Blocks.TORCH) || state.is(Blocks.WALL_TORCH) || state.is(Blocks.LANTERN)) {
				Fx.ghostBlock(player, pos, Blocks.AIR.defaultBlockState());
				count++;
			}
		}
		if (count > 0) {
			Fx.privateSound(player, SoundEvents.FIRE_EXTINGUISH, player.position(), 0.7F, 0.6F);
		}
		return count;
	}

	private static void fakeJoin(ServerPlayer player) {
		Fx.chat(player, Component.translatable("multiplayer.player.joined", NAME).withStyle(ChatFormatting.YELLOW));
		Scheduler.later(20 * (20 + RANDOM.nextInt(40)), () ->
			Fx.chat(player, Component.translatable("multiplayer.player.left", NAME).withStyle(ChatFormatting.YELLOW)));
	}

	private static void door(ServerPlayer player) {
		ServerLevel level = player.level();
		BlockPos origin = player.blockPosition();
		for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-12, -3, -12), origin.offset(12, 3, 12))) {
			BlockState state = level.getBlockState(pos);
			if (state.getBlock() instanceof DoorBlock && state.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER) {
				BlockPos lower = pos.immutable();
				boolean open = !state.getValue(DoorBlock.OPEN);
				Fx.ghostBlock(player, lower, state.setValue(DoorBlock.OPEN, open));
				Fx.ghostBlock(player, lower.above(), level.getBlockState(lower.above()).setValue(DoorBlock.OPEN, open));
				Fx.privateSound(player, open ? SoundEvents.WOODEN_DOOR_OPEN : SoundEvents.WOODEN_DOOR_CLOSE, Places.center(lower), 1.0F, 0.8F);
				Scheduler.later(60, () -> {
					Fx.restoreBlock(player, lower);
					Fx.restoreBlock(player, lower.above());
				});
				return;
			}
		}
		// Keine Tür in der Nähe: dann eben Schritte und ein Blick über die Schulter.
		behind(player);
	}

	/** Für das Welt-Event "Herobrines Nacht". */
	public static void night(ServerPlayer player) {
		torchesOut(player, 16);
		player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 20 * 20, 0, true, false));
		Scheduler.later(20 * 4, () -> {
			if (Fx.online(player) && !busy(player)) {
				float yaw = player.getYRot() + (RANDOM.nextFloat() * 40.0F - 20.0F);
				BlockPos pos = Places.atAngle(player, yaw, 18 + RANDOM.nextInt(6));
				track(player, VisitorEntity.summon(player, Places.center(pos), VisitorEntity.Mode.WATCH, 20 * 30));
			}
		});
	}
}
