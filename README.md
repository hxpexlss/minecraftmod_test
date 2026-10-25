# Chaos-Block

Fabric-Mod für **Minecraft 26.3**.

Alle 5 Minuten wird heimlich ein Blocktyp ausgelost. Wer ihn abbaut, löst ein zufälliges Event aus: gut, richtig schlecht, völlig abgespaced oder ein 10-minütiger Fluch. Welcher Block gerade aktiv ist, verrät nur das **Orakel-Auge**. Dazu kommen alle 10–20 Minuten Welt-Events, die alle Spieler treffen.

## Installation

1. [Fabric Loader](https://fabricmc.net/use/installer/) für Minecraft 26.3 installieren (Loader 0.20.1 oder neuer).
2. [Fabric API](https://modrinth.com/mod/fabric-api) für 26.3 in den `mods`-Ordner legen.
3. `chaosblock-<version>.jar` in den `mods`-Ordner legen.

Für Multiplayer braucht **der Server und jeder Spieler** den Mod. Ein Teil der Flüche, die Anzeigen und die Bildschirm-Effekte laufen im Client.

## Selbst bauen

Du brauchst Java 25.

```
./gradlew build
```

Die fertige Datei liegt danach in `build/libs/chaosblock-1.0.0.jar` (nicht die `-sources.jar`).

## Orakel-Auge

Rezept (Werkbank):

```
 . A .
 G E G
 . A .
```

A = Amethystsplitter, G = Goldbarren, E = Enderauge

Mit Rechtsklick wird der aktuelle Chaos-Block kurz oben auf dem Bildschirm angezeigt. Das Auge hat 5 Ladungen.

## Inhalt

- **32 Block-Events**: 10 gute, 12 schlechte, 10 abgespacte
- **10 Flüche** (je 10 Minuten, bleiben auch nach dem Tod, kein Gegenmittel): Einarmig, Nebelblick, Ein Bein, Bleifüße, Verdrehte Steuerung, Farbenblind, Zittrige Hände, Glasknochen, Vergesslich, Schizophrenie
- **13 Welt-Events**: Blutmond, Meteoritenschauer, Erdbeben, Die Jagd, Säureregen, Mob-Invasion, Schwerkraft-Ausfall, Spieler-Tausch, Eiszeit, Der Fremde, Hitzewelle, Glückssträhne und noch eins…

## Einstellungen

`config/chaosblock.json` wird beim ersten Start angelegt:

| Einstellung | Standard | Bedeutung |
|---|---|---|
| `rotationMinutes` | 5 | Wie oft der geheime Block wechselt |
| `eventCooldownSeconds` | 4 | Mindestabstand zwischen zwei Events pro Spieler |
| `chanceGood` / `chanceBad` / `chanceWeird` / `chanceCurse` | 30 / 35 / 25 / 10 | Gewichtung der Kategorien |
| `curseMinutes` | 10 | Dauer eines Fluchs |
| `worldEventsEnabled` | true | Welt-Events an/aus |
| `worldEventMinMinutes` / `worldEventMaxMinutes` | 10 / 20 | Abstand zwischen Welt-Events |
| `worldEventGraceMinutes` | 10 | Schonfrist nach dem Joinen |
| `oracleCharges` | 5 | Ladungen des Orakel-Auges |
| `ambientEntities` | true | Atmosphärische Umgebung |
| `ambientSpeed` | 1.0 | Wie schnell sich die Atmosphäre entwickelt |

## Befehle (nur für Operatoren)

| Befehl | Was er macht |
|---|---|
| `/chaos block` | Zeigt den aktuellen Chaos-Block |
| `/chaos wechseln` | Lost sofort einen neuen Block aus |
| `/chaos event <id>` | Löst ein bestimmtes Block-Event bei dir aus |
| `/chaos welt <id>` | Startet ein Welt-Event (mit 5 s Vorwarnung) |
| `/chaos fluch <id>` / `/chaos fluch weg` | Fluch geben oder entfernen |
