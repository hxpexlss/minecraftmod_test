package de.chaosblock.network;

import de.chaosblock.ChaosBlockMod;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public final class ModPayloads {
	private ModPayloads() {
	}

	/** Zeigt dem Spieler kurz den geheimen Block (oder eine Nachricht, wenn blockId leer ist). */
	public record Oracle(String blockId, String message, int ticks) implements CustomPacketPayload {
		public static final Type<Oracle> TYPE = new Type<>(ChaosBlockMod.id("oracle"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Oracle> CODEC = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8, Oracle::blockId,
			ByteBufCodecs.STRING_UTF8, Oracle::message,
			ByteBufCodecs.VAR_INT, Oracle::ticks,
			Oracle::new
		);

		@Override
		public Type<Oracle> type() {
			return TYPE;
		}
	}

	/** Aktueller Fluch des Spielers (leer = keiner) und Restzeit in Ticks. */
	public record Curse(String curseId, int ticksLeft) implements CustomPacketPayload {
		public static final Type<Curse> TYPE = new Type<>(ChaosBlockMod.id("curse"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Curse> CODEC = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8, Curse::curseId,
			ByteBufCodecs.VAR_INT, Curse::ticksLeft,
			Curse::new
		);

		@Override
		public Type<Curse> type() {
			return TYPE;
		}
	}

	/** Bildschirm-Effekt für eine Dauer in Ticks: "shake", "quake" oder "bloodmoon". */
	public record ScreenFx(String effect, int ticks) implements CustomPacketPayload {
		public static final Type<ScreenFx> TYPE = new Type<>(ChaosBlockMod.id("screen_fx"));
		public static final StreamCodec<RegistryFriendlyByteBuf, ScreenFx> CODEC = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8, ScreenFx::effect,
			ByteBufCodecs.VAR_INT, ScreenFx::ticks,
			ScreenFx::new
		);

		@Override
		public Type<ScreenFx> type() {
			return TYPE;
		}
	}

	public static void register() {
		PayloadTypeRegistry.clientboundPlay().register(Oracle.TYPE, Oracle.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(Curse.TYPE, Curse.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(ScreenFx.TYPE, ScreenFx.CODEC);
	}
}
