package de.chaosblock;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Einstellungen in config/chaosblock.json. Wird beim Start gelesen und mit
 * fehlenden Werten wieder zurückgeschrieben.
 */
public class ModConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	/** Alle wie viele Minuten der geheime Block wechselt. */
	public int rotationMinutes = 5;
	/** Mindestabstand in Sekunden zwischen zwei Block-Events desselben Spielers. */
	public int eventCooldownSeconds = 4;

	/** Gewichtung der Event-Kategorien (muss nicht 100 ergeben). */
	public int chanceGood = 30;
	public int chanceBad = 35;
	public int chanceWeird = 25;
	public int chanceCurse = 10;

	/** Wie lange ein Fluch anhält. */
	public int curseMinutes = 10;

	public boolean worldEventsEnabled = true;
	public int worldEventMinMinutes = 10;
	public int worldEventMaxMinutes = 20;
	/** Schonfrist nach dem Joinen, in der man von Welt-Events verschont bleibt. */
	public int worldEventGraceMinutes = 10;

	public int oracleCharges = 5;

	/** Atmosphärische Umgebungs-Entities. */
	public boolean ambientEntities = true;
	/** Zeitfaktor für Umgebungs-Entities (2.0 = doppelt so schnell). */
	public double ambientSpeed = 1.0;

	public static ModConfig load() {
		Path path = FabricLoader.getInstance().getConfigDir().resolve("chaosblock.json");
		ModConfig config = new ModConfig();
		if (Files.exists(path)) {
			try (Reader reader = Files.newBufferedReader(path)) {
				ModConfig loaded = GSON.fromJson(reader, ModConfig.class);
				if (loaded != null) {
					config = loaded;
				}
			} catch (Exception e) {
				ChaosBlockMod.LOGGER.error("Konnte chaosblock.json nicht lesen, nutze Standardwerte", e);
			}
		}

		try (Writer writer = Files.newBufferedWriter(path)) {
			GSON.toJson(config, writer);
		} catch (IOException e) {
			ChaosBlockMod.LOGGER.error("Konnte chaosblock.json nicht schreiben", e);
		}

		return config;
	}

	public int curseTicks() {
		return this.curseMinutes * 60 * 20;
	}
}
