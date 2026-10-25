# Anzug-Werkstatt

Fabric-Mod für **Minecraft 26.3** mit vier craftbaren Anzügen. Die Fähigkeiten gibt es nur, wenn man **alle 4 Teile** eines Anzugs trägt. Die Fähigkeiten-Taste ist standardmäßig **R** und lässt sich unter Optionen → Steuerung → „Anzüge“ ändern.

## Installation

Wie beim Chaos-Block: Fabric Loader + Fabric API für 26.3, dann `suitcraft-1.0.0.jar` in den `mods`-Ordner. Server **und** alle Spieler brauchen den Mod.

Selbst bauen (Java 25): im Ordner `suitmod` `./gradlew build` ausführen. Die Jar liegt dann in `build/libs/`.

## Die Anzüge

### 🕸️ Netzläufer
Dunkelgrün-schwarz mit silbernem Netzmuster und Bernstein-Linsen.
- Wände hochklettern: einfach dagegen laufen
- **[R]**: Netz an den anvisierten Block schießen (bis 48 Blöcke) und hinziehen lassen. Schleichen lässt los.
- Höher springen, schneller laufen, weniger Fallschaden
- Gefahrensinn: Greift dich ein Monster von hinten an, flimmert der Bildschirmrand und das Monster leuchtet
- Dazu der **Netzwerfer** (eigenes Item): Rechtsklick schießt Netze, die Gegner festkleben

### 🔥 Glutpanzer
Basaltschwarz mit glühenden Rissen.
- Immun gegen Feuer und Lava
- Lava unter deinen Füßen wird zu Magma (wird nach ein paar Sekunden wieder Lava)
- **[R]**: 1,5 Sekunden Flammenstoß nach vorne (8 s Abklingzeit)
- Wer dich schlägt, fängt Feuer

### 🌑 Schattenmantel
Schwarz-violett mit leuchtenden Augen in der Kapuze.
- Schleichen: unsichtbar (inklusive Rüstung), Monster verlieren dich aus den Augen
- **[R]**: Teleport bis zu 16 Blöcke in Blickrichtung (3 s Abklingzeit)
- Nachtsicht, Doppelsprung

### 🌪️ Sturmflügel
Weiß mit himmelblauen Streifen.
- Die Weste gleitet wie Elytren (Leertaste in der Luft)
- Der erste Sprung in der Luft gibt Aufwind und startet das Gleiten
- **[R]**: Windstoß, der alles in der Nähe wegschleudert. Im Flug stattdessen ein Schub nach vorne.
- Weniger Fallschaden

## Rezepte

Alle Teile werden an der Werkbank gecraftet. X = Hauptmaterial, Y = Verstärkung, Z = Spezialteil:

| Anzug | X | Y | Z |
|---|---|---|---|
| Netzläufer | Faden | Eisenbarren | Spinnenauge |
| Glutpanzer | Magmacreme | Goldbarren | Lohenrute |
| Schattenmantel | Phantomhaut | Obsidian | Enderperle |
| Sturmflügel | Feder | Eisenbarren | Breeze-Rute |

```
Helm        Brustteil     Hose        Stiefel
X Y X       X . X         X Y X       X . X
Z . Z       Y Z Y         X . X       Y . Y
            X X X         X . X
```

**Netzwerfer:**
```
F F F       F = Faden, E = Eisenbarren
E S E       S = Schleimball
. R .       R = Redstone
```
