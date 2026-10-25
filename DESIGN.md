# Chaos-Block Mod – Design

Fabric-Mod für die **neueste Minecraft-Version**. Gespielt im Multiplayer unter Freunden.
Alle Zeiten und Chancen sind über eine Config-Datei einstellbar.

## 1. Geheimer Chaos-Block

- Alle **5 Minuten** wird serverweit heimlich ein Blocktyp ausgelost (nur sinnvolle,
  häufig abgebaute Blöcke: Holzarten, Stein, Cobblestone, Erde, Sand, Kies, Erze, …;
  kein Grundgestein o. Ä.).
- Es gibt **keine Ankündigung**, welcher Block aktiv ist.
- Wer einen Block dieses Typs abbaut, löst ein zufälliges **Block-Event** aus.
- Event-Name wird als Titel auf dem Bildschirm des Auslösers angezeigt.

## 2. Orakel-Auge (Item)

- Rezept: Enderauge + Gold + Amethyst.
- Rechtsklick: zeigt den aktuell aktiven Chaos-Block kurz im HUD.
- 5 Ladungen, danach kaputt.
- Herobrine-Phase ≥ 2: zeigt manchmal stattdessen „ER SIEHT DICH“ (nur für den Betroffenen).

## 3. Block-Events

Verteilung (Config): Gut 30 % · Schlecht 35 % · Abgespaced 25 % · Fluch 10 %

### 😇 Gut (10)
1. Diamanten-Regen
2. Zufälliges verzaubertes Werkzeug
3. 30 Sekunden Fliegen
4. Zahmer Wolf „Glückspilz“
5. Volle Heilung + Absorption
6. Trank-Mix (Speed, Stärke, Sprungkraft)
7. Kleine Schatztruhe spawnt
8. XP-Explosion
9. 1 Minute doppelte Block-Drops
10. Freundlicher Eisengolem folgt dir

### 😈 Schlecht (12) – bewusst hart
1. Creeper-Ring: 8 geladene Creeper im Kreis
2. Inventar-Raub: Fuchs-Dieb stiehlt 5 Stacks, wer ihn fängt, kriegt sie zurück
3. Gewitter des Zorns: 10 s Blitze direkt auf den Spieler
4. Bodenlos: Loch bis kurz über Grundgestein, unten Lava (1 s Vorwarnung)
5. Warden-Besuch: Warden in 20 Blöcken + Dunkelheit
6. Rüstungs-Fluch: getragene Rüstung verliert 50 % Haltbarkeit
7. Hungersnot: Hunger 0, Essen im Inventar → verrottetes Fleisch
8. Lebendig begraben: 10 Blöcke tief in Kies (1 s Vorwarnung)
9. Phantom-Schwarm: 6 Phantome, auch tagsüber
10. Werkzeug-Bruch: Item in der Hand geht kaputt (kleine Rettungschance bei Diamant/Netherite)
11. Wither-Hauch: 30 s Wither II
12. Zurück auf Los: Teleport zum Spawn, Items bleiben am alten Ort liegen

### 🤪 Abgespaced (10)
1. 50 Blöcke in die Luft geschossen + Wasser-Eimer
2. Es regnet Fische
3. Tiere in der Nähe werden zu Hühnern
4. Regenbogen-Schafe
5. Schreiender Villager folgt dir
6. 1 Minute Mini-Größe
7. 1 Minute Riesen-Größe
8. Schwerkraft umgedreht (Schwebe-Effekt)
9. Tag/Nacht tauschen sofort
10. Party-Modus: Feuerwerk, Musik, tanzende Mobs

### ☠️ Flüche (10) – dauern 10 Minuten
- Immer nur ein Fluch gleichzeitig. Bleibt nach dem Tod bestehen. **Kein Gegenmittel.**
- HUD-Anzeige mit Name und Restzeit.

1. Einarmig: Offhand gesperrt, Abbau langsamer
2. Nebelblick: Sichtweite nur wenige Blöcke
3. Ein Bein: stark verlangsamt, kein Sprint, minimaler Sprung, humpelnde Kamera
4. Bleifüße: kein Springen, kein Sprinten
5. Verdrehte Steuerung: W/S und A/D vertauscht
6. Farbenblind: Welt schwarz-weiß
7. Zittrige Hände: ständiges Kamera-Wackeln
8. Glasknochen: doppelter Schaden
9. Vergesslich: Hotbar-Slots tauschen alle 30 s
10. Schizophrenie: Fake-Creeper-Zischen, Mobs am Bildrand die verschwinden,
    Schritte von hinten, Chat-Nachrichten von nicht existierenden Spielern

## 4. Welt-Events

- Alle **10–20 Minuten** (zufällig), betreffen alle Spieler.
- 5 s vorher Warnung „⚠️ Etwas kommt…“.
- Erste 10 Minuten nach Join: Schonfrist. Immer nur ein Welt-Event gleichzeitig.

1. Blutmond: sofort Nacht, roter Himmel, 5 Min doppelte & stärkere Monster
2. Meteoritenschauer: Feuerbälle, Krater, teils mit Erz
3. Erdbeben: Kamera-Wackeln, Einstürze, fallender Sand/Kies
4. Die Jagd: ein Spieler markiert, 3 Min von Wolfsrudel mit Stärke gejagt
5. Säureregen: Schaden im Regen
6. Mob-Invasion: zwei Wellen, die zweite härter
7. Schwerkraft-Ausfall: 20 s nach oben schweben, dann Fall
8. Spieler-Tausch: alle Positionen zufällig vertauscht
9. Eiszeit: Wasser friert, Schnee, Langsamkeit bis man am Feuer ist
10. Der Fremde: spielerähnlicher Mob starrt aus der Ferne, steht später hinter dir
11. Hitzewelle: doppelter Hungerverlust, Wasser verdunstet
12. Glückssträhne: 2 Min nur gute Block-Events
13. Herobrines Nacht: Fackeln in der Nähe gehen aus (nur für den Betroffenen, siehe 5.)

## 5. Herobrine (geheim!)

**Geheimhaltung:** Nirgends in Mod-Beschreibung, Mod-Menü oder sichtbaren Item-Namen
erwähnt. Drop-Item heißt „???“ bis zum Aufheben. Config-Schalter mit unauffälligem Namen.

**Pro Spieler:** Jeder Spieler hat seinen eigenen Herobrine mit eigenem Phasen-Fortschritt
(basiert auf Spielzeit). Alles ist **nur für den betroffenen Spieler** sichtbar/hörbar:
Entity nur an diesen Client gesendet, Sounds/Chat nur an ihn, Spuren als Geisterblöcke
(nur clientseitig für ihn).

**Aussehen:** Steve-Skin mit leuchtend weißen Augen (auch im Dunkeln sichtbar).

| Phase | Ab Spielzeit | Verhalten |
|---|---|---|
| 1 Beobachten | ~30 Min | Steht weit weg, verschwindet beim Hinschauen/Annähern |
| 2 Spuren | ~1 Std | Geisterblöcke: blattlose Bäume, 2×2-Tunnel, Sandpyramiden, Schilder „Ich sehe dich“, Fackeln gehen aus |
| 3 Psychoterror | ~2 Std | Steht hinter dir, Fake-„Herobrine joined the game“, Türen öffnen sich, Höhlengeräusche |
| 4 Jagd | selten, ~3 Std | Greift **wirklich** an: Teleport, schnell, harter Schaden. Bei Verletzung verschwindet er mit Blitz und kommt wieder |

- Schaden in Phase 4 ist echt (für andere sieht man nur, dass der Spieler Schaden nimmt).
- Wer den Fluch „Schizophrenie“ hat, sieht ihn öfter (teils nur Fake-Erscheinungen).
- Besiegt man ihn in Phase 4: Drop „Weißes Auge“ → Orakel-Auge mit unendlich Ladungen.
  Danach 1–2 Std Ruhe vor ihm.

## Umsetzung

**Alles auf einmal bauen** (Wunsch des Nutzers) – keine Etappen. Vor dem Push muss
`./gradlew build` erfolgreich durchlaufen.
