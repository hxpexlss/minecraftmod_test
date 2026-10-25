package de.chaosblock.client;

import java.util.HashMap;
import java.util.Map;

/**
 * Was der Client über Flüche, Orakel und Bildschirm-Effekte weiß.
 */
public final class ClientState {
	public static String curse = "";
	public static int curseTicks;

	public static String oracleBlock = "";
	public static String oracleMessage = "";
	public static int oracleTicks;
	public static int oracleTotal;

	/** Bildschirm-Effekt -> verbleibende Ticks. */
	public static final Map<String, Integer> SCREEN = new HashMap<>();

	private ClientState() {
	}

	public static boolean cursed(String id) {
		return curseTicks > 0 && id.equals(curse);
	}

	public static boolean noSprint() {
		return cursed("einbein") || cursed("bleifuesse");
	}

	public static boolean screen(String effect) {
		return SCREEN.getOrDefault(effect, 0) > 0;
	}

	public static void reset() {
		curse = "";
		curseTicks = 0;
		oracleTicks = 0;
		SCREEN.clear();
	}

	static void tick() {
		if (curseTicks > 0) {
			curseTicks--;
		}
		if (oracleTicks > 0) {
			oracleTicks--;
		}
		SCREEN.replaceAll((k, v) -> v - 1);
		SCREEN.values().removeIf(v -> v <= 0);
	}
}
