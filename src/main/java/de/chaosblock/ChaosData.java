package de.chaosblock;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

/**
 * Spielstand des Mods, liegt als chaosblock.json im Weltordner.
 */
public class ChaosData {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	public String currentBlock = "";
	public int ticksUntilRotation = 0;
	public int ticksUntilWorldEvent = -1;
	public Map<String, PlayerData> players = new HashMap<>();

	public static class PlayerData {
		public String curse = "";
		public int curseTicks = 0;
		public long playTicks = 0;
		public int ambientCooldown = 0;
		public int ambientNextAction = 0;
	}

	public PlayerData player(UUID uuid) {
		return this.players.computeIfAbsent(uuid.toString(), k -> new PlayerData());
	}

	private static Path path(MinecraftServer server) {
		return server.getWorldPath(LevelResource.ROOT).resolve("chaosblock.json");
	}

	public static ChaosData load(MinecraftServer server) {
		Path path = path(server);
		if (Files.exists(path)) {
			try (Reader reader = Files.newBufferedReader(path)) {
				ChaosData data = GSON.fromJson(reader, ChaosData.class);
				if (data != null) {
					if (data.players == null) {
						data.players = new HashMap<>();
					}
					return data;
				}
			} catch (Exception e) {
				ChaosBlockMod.LOGGER.error("Konnte Chaos-Spielstand nicht lesen", e);
			}
		}
		return new ChaosData();
	}

	public void save(MinecraftServer server) {
		try (Writer writer = Files.newBufferedWriter(path(server))) {
			GSON.toJson(this, writer);
		} catch (Exception e) {
			ChaosBlockMod.LOGGER.error("Konnte Chaos-Spielstand nicht speichern", e);
		}
	}
}
