package de.chaosblock;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import de.chaosblock.ambient.AmbientManager;
import de.chaosblock.curse.CurseManager;
import de.chaosblock.event.BlockEvents;
import de.chaosblock.event.WorldEventManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * /chaos – nur für Operatoren, zum Testen und Verwalten.
 */
public final class ChaosCommand {
	private ChaosCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("chaos")
			.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
			.then(Commands.literal("block").executes(c -> {
				ChaosBlockManager manager = ChaosBlockMod.manager();
				int seconds = ChaosBlockMod.data().ticksUntilRotation / 20;
				c.getSource().sendSuccess(() -> Component.literal("Chaos-Block: " + manager.currentBlockId() + " (wechselt in " + seconds + " s)"), false);
				return 1;
			}))
			.then(Commands.literal("wechseln").executes(c -> {
				ChaosBlockMod.manager().rotate();
				c.getSource().sendSuccess(() -> Component.literal("Chaos-Block neu ausgelost."), true);
				return 1;
			}))
			.then(Commands.literal("event")
				.then(Commands.argument("id", StringArgumentType.word())
					.suggests((c, b) -> SharedSuggestionProvider.suggest(BlockEvents.ids(), b))
					.executes(c -> {
						ServerPlayer player = c.getSource().getPlayerOrException();
						String id = StringArgumentType.getString(c, "id");
						if (!BlockEvents.run(id, player, player.blockPosition())) {
							c.getSource().sendFailure(Component.literal("Unbekanntes Event: " + id));
							return 0;
						}
						return 1;
					})))
			.then(Commands.literal("welt")
				.then(Commands.argument("id", StringArgumentType.word())
					.suggests((c, b) -> SharedSuggestionProvider.suggest(WorldEventManager.ids(), b))
					.executes(c -> {
						String id = StringArgumentType.getString(c, "id");
						if (!WorldEventManager.announce(c.getSource().getServer(), id)) {
							c.getSource().sendFailure(Component.literal("Unbekanntes Welt-Event: " + id));
							return 0;
						}
						return 1;
					})))
			.then(Commands.literal("fluch")
				.then(Commands.literal("weg").executes(c -> {
					CurseManager.clear(c.getSource().getPlayerOrException(), true);
					return 1;
				}))
				.then(Commands.argument("id", StringArgumentType.word())
					.suggests((c, b) -> SharedSuggestionProvider.suggest(CurseManager.CURSES.stream().map(CurseManager.Curse::id), b))
					.executes(c -> {
						String id = StringArgumentType.getString(c, "id");
						CurseManager.Curse curse = CurseManager.byId(id);
						if (curse == null) {
							c.getSource().sendFailure(Component.literal("Unbekannter Fluch: " + id));
							return 0;
						}
						CurseManager.apply(c.getSource().getPlayerOrException(), curse, ChaosBlockMod.config().curseTicks());
						return 1;
					})))
			// Unauffällig benannt, damit niemand beim Durchblättern stutzig wird.
			.then(Commands.literal("ambient")
				.then(Commands.argument("stufe", IntegerArgumentType.integer(0, 4))
					.executes(c -> ambient(c.getSource(), IntegerArgumentType.getInteger(c, "stufe"), false))
					.then(Commands.literal("jetzt")
						.executes(c -> ambient(c.getSource(), IntegerArgumentType.getInteger(c, "stufe"), true))))));
	}

	private static int ambient(CommandSourceStack source, int stage, boolean now) throws CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		AmbientManager.setPhase(player, stage);
		if (now && stage > 0) {
			if (stage == 4) {
				AmbientManager.hunt(player);
			} else {
				AmbientManager.act(player, stage);
			}
		}
		source.sendSuccess(() -> Component.literal("Umgebungsstufe " + stage), false);
		return 1;
	}
}
