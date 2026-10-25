package de.chaosblock;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Führt Aufgaben nach einer bestimmten Anzahl Server-Ticks aus.
 */
public final class Scheduler {
	private static final List<Task> TASKS = new ArrayList<>();
	private static final List<Task> PENDING = new ArrayList<>();

	private record Task(int[] remaining, Runnable action) {
	}

	private Scheduler() {
	}

	public static void later(int ticks, Runnable action) {
		PENDING.add(new Task(new int[]{Math.max(1, ticks)}, action));
	}

	/** Führt {@code action} {@code times}-mal im Abstand von {@code interval} Ticks aus. */
	public static void repeat(int interval, int times, Runnable action) {
		for (int i = 0; i < times; i++) {
			later(interval * (i + 1), action);
		}
	}

	static void tick() {
		TASKS.addAll(PENDING);
		PENDING.clear();
		Iterator<Task> it = TASKS.iterator();
		while (it.hasNext()) {
			Task task = it.next();
			if (--task.remaining()[0] <= 0) {
				it.remove();
				try {
					task.action().run();
				} catch (Exception e) {
					ChaosBlockMod.LOGGER.error("Fehler in geplanter Aufgabe", e);
				}
			}
		}
	}

	static void clear() {
		TASKS.clear();
		PENDING.clear();
	}
}
