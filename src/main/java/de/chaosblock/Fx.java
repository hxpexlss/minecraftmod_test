package de.chaosblock;

import de.chaosblock.network.ModPayloads;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Hilfsfunktionen für Dinge, die nur ein einzelner Spieler sehen oder hören soll.
 */
public final class Fx {
	private Fx() {
	}

	public static boolean online(ServerPlayer player) {
		return player != null && !player.isRemoved() && !player.hasDisconnected();
	}

	public static void title(ServerPlayer player, String title, String subtitle) {
		if (!online(player)) {
			return;
		}
		player.connection.send(new ClientboundSetTitlesAnimationPacket(5, 50, 15));
		player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(subtitle)));
		player.connection.send(new ClientboundSetTitleTextPacket(Component.literal(title)));
	}

	public static void actionBar(ServerPlayer player, String text) {
		if (online(player)) {
			player.sendSystemMessage(Component.literal(text), true);
		}
	}

	public static void chat(ServerPlayer player, Component text) {
		if (online(player)) {
			player.sendSystemMessage(text);
		}
	}

	/** Spielt einen Sound, den nur dieser Spieler hört. */
	public static void privateSound(ServerPlayer player, SoundEvent sound, Vec3 pos, float volume, float pitch) {
		privateSound(player, BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound), pos, volume, pitch);
	}

	public static void privateSound(ServerPlayer player, Holder<SoundEvent> sound, Vec3 pos, float volume, float pitch) {
		if (online(player)) {
			player.connection.send(new ClientboundSoundPacket(sound, SoundSource.HOSTILE, pos.x, pos.y, pos.z, volume, pitch, player.getRandom().nextLong()));
		}
	}

	/** Sound, den alle in der Nähe hören. */
	public static void sound(ServerLevel level, Vec3 pos, SoundEvent sound, float volume, float pitch) {
		level.playSound(null, pos.x, pos.y, pos.z, sound, SoundSource.PLAYERS, volume, pitch);
	}

	/** Zeigt diesem Spieler an einer Stelle einen anderen Block, ohne die Welt zu verändern. */
	public static void ghostBlock(ServerPlayer player, BlockPos pos, BlockState state) {
		if (online(player)) {
			player.connection.send(new ClientboundBlockUpdatePacket(pos.immutable(), state));
		}
	}

	/** Setzt eine Geisterblock-Änderung zurück auf den echten Block. */
	public static void restoreBlock(ServerPlayer player, BlockPos pos) {
		if (online(player)) {
			player.connection.send(new ClientboundBlockUpdatePacket(player.level(), pos.immutable()));
		}
	}

	public static void screen(ServerPlayer player, String effect, int ticks) {
		if (online(player)) {
			ServerPlayNetworking.send(player, new ModPayloads.ScreenFx(effect, ticks));
		}
	}
}
