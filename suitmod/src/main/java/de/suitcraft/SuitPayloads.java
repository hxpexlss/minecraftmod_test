package de.suitcraft;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public final class SuitPayloads {
	private SuitPayloads() {
	}

	/** Client -> Server: Fähigkeiten-Taste gedrückt. */
	public record Ability() implements CustomPacketPayload {
		public static final Ability INSTANCE = new Ability();
		public static final Type<Ability> TYPE = new Type<>(SuitcraftMod.id("ability"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Ability> CODEC = StreamCodec.unit(INSTANCE);

		@Override
		public Type<Ability> type() {
			return TYPE;
		}
	}

	/** Client -> Server: Doppelsprung ausgeführt. */
	public record DoubleJump() implements CustomPacketPayload {
		public static final DoubleJump INSTANCE = new DoubleJump();
		public static final Type<DoubleJump> TYPE = new Type<>(SuitcraftMod.id("double_jump"));
		public static final StreamCodec<RegistryFriendlyByteBuf, DoubleJump> CODEC = StreamCodec.unit(INSTANCE);

		@Override
		public Type<DoubleJump> type() {
			return TYPE;
		}
	}

	/** Server -> Client: Gefahrensinn schlägt an. */
	public record Danger() implements CustomPacketPayload {
		public static final Danger INSTANCE = new Danger();
		public static final Type<Danger> TYPE = new Type<>(SuitcraftMod.id("danger"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Danger> CODEC = StreamCodec.unit(INSTANCE);

		@Override
		public Type<Danger> type() {
			return TYPE;
		}
	}

	public static void register() {
		PayloadTypeRegistry.serverboundPlay().register(Ability.TYPE, Ability.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(DoubleJump.TYPE, DoubleJump.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(Danger.TYPE, Danger.CODEC);
	}
}
