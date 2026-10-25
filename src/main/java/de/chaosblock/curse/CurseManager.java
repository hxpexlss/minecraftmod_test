package de.chaosblock.curse;

import de.chaosblock.ChaosBlockMod;
import de.chaosblock.ChaosData;
import de.chaosblock.Fx;
import de.chaosblock.Places;
import de.chaosblock.PrivateEntities;
import de.chaosblock.Scheduler;
import de.chaosblock.ambient.AmbientManager;
import de.chaosblock.network.ModPayloads;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Flüche: dauern ein paar Minuten (Standard 10), überstehen den Tod und lassen
 * sich nicht heilen. Ein Teil wirkt auf dem Server, ein Teil nur im Client.
 */
public final class CurseManager {
	public record Curse(String id, String name, String description) {
	}

	public static final List<Curse> CURSES = List.of(
		new Curse("einarmig", "Einarmig", "Zweite Hand gesperrt, alles dauert länger"),
		new Curse("nebelblick", "Nebelblick", "Du siehst kaum noch etwas"),
		new Curse("einbein", "Ein Bein", "Humpeln statt rennen"),
		new Curse("bleifuesse", "Bleifüße", "Kein Springen, kein Sprinten"),
		new Curse("verdreht", "Verdrehte Steuerung", "Vorne ist hinten, links ist rechts"),
		new Curse("farbenblind", "Farbenblind", "Die Welt hat ihre Farbe verloren"),
		new Curse("zittrig", "Zittrige Hände", "Ruhig zielen? Vergiss es"),
		new Curse("glasknochen", "Glasknochen", "Doppelter Schaden"),
		new Curse("vergesslich", "Vergesslich", "Wo war nochmal dein Schwert?"),
		new Curse("schizophrenie", "Schizophrenie", "Nicht alles, was du siehst, ist echt")
	);

	private static final Identifier MOD_BREAK = ChaosBlockMod.id("curse_break_speed");
	private static final Identifier MOD_ATTACK = ChaosBlockMod.id("curse_attack_speed");
	private static final Identifier MOD_SPEED = ChaosBlockMod.id("curse_speed");
	private static final Identifier MOD_JUMP = ChaosBlockMod.id("curse_jump");

	private static final RandomSource RANDOM = RandomSource.create();
	private static final String[] FAKE_NAMES = {"Steve", "Alex", "Notch", "xXSlayerXx", "Kevin", "jeb_", "Dinnerbone", "Mama"};
	private static final String[] FAKE_MESSAGES = {
		"hinter dir", "dreh dich nicht um", "hast du das auch gehört?", "wer ist da bei dir?", "ich sehe dich",
		"lauf", "warum stehst du da so rum?", "da war gerade jemand", "hallo?", "komm her", "er ist hier"
	};

	private CurseManager() {
	}

	public static Curse byId(String id) {
		for (Curse curse : CURSES) {
			if (curse.id().equals(id)) {
				return curse;
			}
		}
		return null;
	}

	public static boolean has(ServerPlayer player, String curseId) {
		ChaosData data = ChaosBlockMod.data();
		if (data == null) {
			return false;
		}
		ChaosData.PlayerData pd = data.players.get(player.getUUID().toString());
		return pd != null && pd.curseTicks > 0 && curseId.equals(pd.curse);
	}

	public static boolean hasAny(ServerPlayer player) {
		ChaosData.PlayerData pd = ChaosBlockMod.data().player(player.getUUID());
		return pd.curseTicks > 0 && !pd.curse.isEmpty();
	}

	/** Verflucht den Spieler zufällig. false, wenn er schon verflucht ist. */
	public static boolean applyRandom(ServerPlayer player) {
		if (hasAny(player)) {
			return false;
		}
		apply(player, CURSES.get(RANDOM.nextInt(CURSES.size())), ChaosBlockMod.config().curseTicks());
		return true;
	}

	public static void apply(ServerPlayer player, Curse curse, int ticks) {
		clear(player, false);
		ChaosData.PlayerData pd = ChaosBlockMod.data().player(player.getUUID());
		pd.curse = curse.id();
		pd.curseTicks = ticks;
		applyAttributes(player, curse.id());
		sync(player);
		Fx.title(player, "§4☠ VERFLUCHT: " + curse.name(), "§c" + curse.description());
		Fx.sound(player.level(), player.position(), SoundEvents.ELDER_GUARDIAN_CURSE, 1.0F, 0.8F);
	}

	public static void clear(ServerPlayer player, boolean announce) {
		ChaosData.PlayerData pd = ChaosBlockMod.data().player(player.getUUID());
		boolean had = pd.curseTicks > 0 && !pd.curse.isEmpty();
		pd.curse = "";
		pd.curseTicks = 0;
		removeAttributes(player);
		sync(player);
		if (had && announce) {
			Fx.title(player, "§a✔ Fluch vorbei", "§7Du hast es überstanden");
			Fx.sound(player.level(), player.position(), SoundEvents.PLAYER_LEVELUP, 1.0F, 1.2F);
		}
	}

	public static void onJoin(ServerPlayer player) {
		ChaosData.PlayerData pd = ChaosBlockMod.data().player(player.getUUID());
		removeAttributes(player);
		if (pd.curseTicks > 0 && !pd.curse.isEmpty()) {
			applyAttributes(player, pd.curse);
		}
		sync(player);
	}

	public static void sync(ServerPlayer player) {
		if (!Fx.online(player)) {
			return;
		}
		ChaosData.PlayerData pd = ChaosBlockMod.data().player(player.getUUID());
		boolean active = pd.curseTicks > 0 && !pd.curse.isEmpty();
		ServerPlayNetworking.send(player, new ModPayloads.Curse(active ? pd.curse : "", active ? pd.curseTicks : 0));
	}

	private static void modifier(ServerPlayer player, Holder<Attribute> attribute, Identifier id, double amount) {
		AttributeInstance instance = player.getAttribute(attribute);
		if (instance != null) {
			instance.addOrUpdateTransientModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
		}
	}

	private static void applyAttributes(ServerPlayer player, String curse) {
		switch (curse) {
			case "einarmig" -> {
				modifier(player, Attributes.BLOCK_BREAK_SPEED, MOD_BREAK, -0.6);
				modifier(player, Attributes.ATTACK_SPEED, MOD_ATTACK, -0.35);
			}
			case "einbein" -> {
				modifier(player, Attributes.MOVEMENT_SPEED, MOD_SPEED, -0.55);
				modifier(player, Attributes.JUMP_STRENGTH, MOD_JUMP, -0.55);
			}
			case "bleifuesse" -> modifier(player, Attributes.JUMP_STRENGTH, MOD_JUMP, -1.0);
			default -> {
			}
		}
	}

	private static void removeAttributes(ServerPlayer player) {
		for (var entry : Map.of(
			Attributes.BLOCK_BREAK_SPEED, MOD_BREAK, Attributes.ATTACK_SPEED, MOD_ATTACK,
			Attributes.MOVEMENT_SPEED, MOD_SPEED, Attributes.JUMP_STRENGTH, MOD_JUMP).entrySet()) {
			AttributeInstance instance = player.getAttribute(entry.getKey());
			if (instance != null) {
				instance.removeModifier(entry.getValue());
			}
		}
	}

	public static void tick(MinecraftServer server) {
		ChaosData data = ChaosBlockMod.data();
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			ChaosData.PlayerData pd = data.players.get(player.getUUID().toString());
			if (pd == null || pd.curseTicks <= 0 || pd.curse.isEmpty()) {
				continue;
			}
			pd.curseTicks--;
			if (pd.curseTicks <= 0) {
				clear(player, true);
				continue;
			}
			tickCurse(player, pd);
		}
	}

	private static void tickCurse(ServerPlayer player, ChaosData.PlayerData pd) {
		int t = pd.curseTicks;
		switch (pd.curse) {
			case "einarmig" -> {
				ItemStack off = player.getOffhandItem();
				if (!off.isEmpty()) {
					player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND, ItemStack.EMPTY);
					Places.give(player, off);
				}
			}
			case "nebelblick" -> {
				if (t % 20 == 0) {
					player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0, true, false));
				}
			}
			case "einbein", "bleifuesse" -> {
				if (player.isSprinting()) {
					player.setSprinting(false);
				}
			}
			case "vergesslich" -> {
				if (t % (30 * 20) == 0) {
					shuffleHotbar(player);
				}
			}
			case "schizophrenie" -> {
				if (t % 20 == 0 && RANDOM.nextInt(20) == 0) {
					hallucinate(player);
				}
			}
			default -> {
			}
		}
		// Restzeit regelmäßig nachsenden, damit die Anzeige im Client nicht wegdriftet.
		if (t % 200 == 0) {
			sync(player);
		}
	}

	private static void shuffleHotbar(ServerPlayer player) {
		Inventory inv = player.getInventory();
		List<ItemStack> hotbar = new ArrayList<>();
		for (int i = 0; i < 9; i++) {
			hotbar.add(inv.getItem(i));
		}
		Collections.shuffle(hotbar, new java.util.Random(RANDOM.nextLong()));
		for (int i = 0; i < 9; i++) {
			inv.setItem(i, hotbar.get(i));
		}
		inv.setChanged();
		Fx.actionBar(player, "§7§o…moment, wo war das nochmal?");
	}

	/** Eine zufällige Halluzination, die nur dieser Spieler wahrnimmt. */
	public static void hallucinate(ServerPlayer player) {
		Vec3 behind = player.position().subtract(player.getLookAngle().multiply(2.5, 0, 2.5));
		switch (RANDOM.nextInt(7)) {
			case 0 -> Fx.privateSound(player, SoundEvents.CREEPER_PRIMED, behind, 1.0F, 1.0F);
			case 1 -> footsteps(player);
			case 2 -> {
				String name = FAKE_NAMES[RANDOM.nextInt(FAKE_NAMES.length)];
				String msg = FAKE_MESSAGES[RANDOM.nextInt(FAKE_MESSAGES.length)];
				Fx.chat(player, Component.literal("<" + name + "> " + msg));
			}
			case 3 -> Fx.privateSound(player, SoundEvents.AMBIENT_CAVE, player.position().add(0, 1, 0), 1.0F, 0.8F + RANDOM.nextFloat() * 0.4F);
			case 4 -> {
				if (ChaosBlockMod.config().ambientEntities && RANDOM.nextBoolean()) {
					AmbientManager.fakeGlimpse(player);
				} else {
					peripheralMob(player);
				}
			}
			case 5 -> {
				Fx.privateSound(player, SoundEvents.PLAYER_HURT, player.position(), 1.0F, 1.0F);
				Fx.chat(player, Component.literal(player.getName().getString() + " was slain by Zombie").withStyle(ChatFormatting.WHITE));
			}
			default -> peripheralMob(player);
		}
	}

	private static void footsteps(ServerPlayer player) {
		SoundEvent step = player.level().getBlockState(player.blockPosition().below()).getSoundType().getStepSound();
		for (int i = 0; i < 6; i++) {
			final double dist = 7.0 - i;
			Scheduler.later(i * 7, () -> {
				Vec3 at = player.position().subtract(player.getLookAngle().multiply(dist, 0, dist));
				Fx.privateSound(player, step, at, 0.6F, 1.0F);
			});
		}
	}

	/** Ein Mob am Bildrand, der verschwindet, sobald man hinschaut. */
	private static void peripheralMob(ServerPlayer player) {
		List<EntityType<? extends Mob>> types = List.of(EntityTypes.CREEPER, EntityTypes.ZOMBIE, EntityTypes.SKELETON, EntityTypes.ENDERMAN);
		EntityType<? extends Mob> type = types.get(RANDOM.nextInt(types.size()));
		float side = RANDOM.nextBoolean() ? 55.0F : -55.0F;
		BlockPos pos = Places.atAngle(player, player.getYRot() + side, 10 + RANDOM.nextInt(8));
		Mob mob = PrivateEntities.apparition(type, player, Places.center(pos), 20 * 6);
		if (mob == null) {
			return;
		}
		Scheduler.repeat(2, 20 * 3, () -> {
			if (!mob.isRemoved() && Places.isLookingAt(player, mob.position().add(0, 1, 0), 18)) {
				mob.discard();
			}
		});
	}
}
