package de.suitcraft;

import java.util.ArrayList;
import java.util.List;

/** Führt Aufgaben nach einer Anzahl Server-Ticks aus. */
public final class Tasks {
	private static final List<Object[]> TASKS = new ArrayList<>();
	private static final List<Object[]> PENDING = new ArrayList<>();

	private Tasks() {
	}

	public static void later(int ticks, Runnable action) {
		PENDING.add(new Object[]{Math.max(1, ticks), action});
	}

	static void tick() {
		TASKS.addAll(PENDING);
		PENDING.clear();
		TASKS.removeIf(task -> {
			int left = (int) task[0] - 1;
			task[0] = left;
			if (left > 0) {
				return false;
			}
			try {
				((Runnable) task[1]).run();
			} catch (Exception e) {
				SuitcraftMod.LOGGER.error("Fehler in geplanter Aufgabe", e);
			}
			return true;
		});
	}

	/** Beim Server-Stopp: verbleibende Aufgaben sofort ausführen (z. B. Netze/Magma zurücksetzen). */
	static void flush() {
		TASKS.addAll(PENDING);
		PENDING.clear();
		for (Object[] task : TASKS) {
			try {
				((Runnable) task[1]).run();
			} catch (Exception e) {
				SuitcraftMod.LOGGER.error("Fehler beim Aufräumen", e);
			}
		}
		TASKS.clear();
	}
}
