# Neos RS Disk Accelerator

Neos RS Disk Accelerator erhöht die Itemmenge pro Arbeitsschritt des originalen Refined-Storage-Disk-Manipulators. Keine eigene Maschine, keine neue GUI und kein eigenes Festplattenformat.

## Versionen

- Release: **1.0.0**
- Minecraft: **1.21.1**
- NeoForge: **21.1.251 bis kleiner 21.2**; gezielte lokale Tests mit 21.1.251
- Refined Storage: **exakt 2.0.9**, erforderlich
- Java: **21**
- Extra Storage 5.0.10 und Extra Disks 4.0.15: im Einzelspieler-Test des Projektinhabers verwendet, optionale Ergänzungen; keine eigenen Pflichtabhängigkeiten dieses Add-ons.

Die versionsgebundenen Mixins unterstützen keine anderen RS-Versionen. Forge und Fabric werden nicht unterstützt.

## Download und Installation

[Release v1.0.0](https://github.com/Matzenko/Neos-RS-Disk-Accelerator/releases/tag/v1.0.0)

[JAR herunterladen](https://github.com/Matzenko/Neos-RS-Disk-Accelerator/releases/download/v1.0.0/neos-rs-disk-accelerator-1.0.0.jar)

1. Minecraft/Server beenden und eine Sicherung der Welt erstellen.
2. Passende NeoForge- und Refined-Storage-Version installieren.
3. `neos-rs-disk-accelerator-1.0.0.jar` in den `mods`-Ordner der tatsächlich verwendeten Installation legen. Ältere Accelerator-JARs dort ersetzen, nicht parallel laden.
4. Neu starten und `/neosdisk accelerator status` als OP prüfen.

**Installationsseite:** Die Beschleunigung benötigt die Mod auf dem logischen Server. Im Einzelspieler muss sie deshalb in der Clientinstallation liegen, die den integrierten Server startet. Bei einem dedizierten Server ist sie dort erforderlich; eine zusätzliche Clientinstallation ist für die Funktion nach Codeprüfung nicht erforderlich: Das Add-on registriert keine eigenen Netzwerkpakete, Items, Blöcke oder Menüs. Ein Verbindungsversuch mit asymmetrischer Client-/Serverinstallation wurde nicht durchgeführt. Eine reine Clientinstallation beschleunigt einen entfernten Server nicht. Die Metadatenangabe `side="BOTH"` betrifft die Abhängigkeiten und ist kein Beleg für eine beidseitige Installationspflicht.

Die separate Testfill-Mod und die alte Disk Transfer Station sind nicht Bestandteil dieses Releases und werden nicht benötigt.

## Konfiguration

NeoForge-SERVER-Konfiguration: `config/neos_rs_disk_accelerator-server.toml`; gegebenenfalls vorhandene weltbezogene Overrides unter `<Welt>/serverconfig/` berücksichtigen. Den effektiven Stand zeigt der Statusbefehl.

```toml
enabled = true
itemsPerStep = 8192
maxItemsPerServerTick = 65536
maxItemsPerDeviceTick = 16384
maxWorkStepsPerServerTick = 256
fallbackAboveMspt = 45.0
```

| Einstellung | Bedeutung |
| --- | --- |
| `enabled` | `false` verwendet die originale RS-Quote. |
| `itemsPerStep` | Default **8192** mit Stack-Upgrade; Bereich 64–16384. Ohne Stack-Upgrade 1/64 davon, mindestens 1. |
| `maxItemsPerServerTick` | Gemeinsames Reservierungsbudget aller beschleunigten Geräte pro Servertick. |
| `maxItemsPerDeviceTick` | Reservierungsbudget je Gerät und Servertick. |
| `maxWorkStepsPerServerTick` | Maximale Quotenfreigaben pro Servertick; erzeugt keine zusätzlichen Arbeitsschritte. |
| `fallbackAboveMspt` | Oberhalb dieser durchschnittlichen Tickzeit Rückfall auf die originale RS-Quote. |

**Bestehende Konfigurationen werden nicht auf 8192 überschrieben.** Wer bereits `itemsPerStep=512` konfiguriert hat, behält diesen Wert. Für den neuen Default muss der Betreiber den vorhandenen Eintrag selbst ändern.

Speed-Upgrades behalten die originalen Intervalle: ohne Speed-Upgrade 9 Ticks, mit 1/2/3 Speed-Upgrades 7/5/3 Ticks. Der Accelerator erhöht die Menge, nicht die Häufigkeit. Fluidplatten werden nicht beschleunigt.

Budgetreservierungen können auch bei erfolglosen Versuchen verbraucht werden. Sicherheitsbudgets und MSPT-Rückfall bleiben aktiv. Es gibt keine eigene UUID-Verwaltung; Simulation, Extraktion, Einlagerung, Filter, Vollzielverhalten und Rückgabe bleiben im Original-RS-Pfad. Es wird kein zusätzlicher Energiepreis eingeführt. Durch weniger Arbeitsschritte können die Gesamtkosten pro transferierter Itemmenge sinken; eine identische Gesamtenergie pro Item wird nicht zugesichert.

## Admin-Diagnose

Permission-Level mindestens 2:

```text
/neosdisk accelerator status
/neosdisk accelerator measure <x> <y> <z>
```

`measure` beobachtet genau einen geladenen originalen Manipulator in der aktuellen Dimension für 200 natürliche Serverticks. Gemeldet werden reale Transfermengen, Items/s, erfolgreiche Transfers/s, Transferabstände, Quoten, Budget-/MSPT-Rückfallgründe und Zeit im nativen Transferpfad. Leerlauf innerhalb des Messfensters zählt mit. Die Messung erzeugt und verschiebt selbst keine Items. Es werden keine Chunks zum Messen geladen. Das Ergebnis wird auch geloggt.

## Teststand und Serverlast

Bisher liegt **nur ein Einzelspieler-Ingame-Teststand** des Projektinhabers vor, keine Multiplayer-/Hauptserverfreigabe und kein repräsentativer großer Serverlasttest. Der Projektinhaber berichtet bei 1.000.000 Cobblestone:

- 512 Items/Schritt: 4 Minuten 53 Sekunden.
- 8192 Items/Schritt: knapp 21 Sekunden, erfolgreich.

Dies sind gemeldete Ingame-Ergebnisse, keine vom Coding-Agenten durchgeführten Clienttests. Zusätzliche automatisierte Tests laufen in isolierten dedizierten lokalen Testwelten und ersetzen diese Abnahme nicht. 

Höhere Quoten können Serverlast und Tickspitzen erhöhen, insbesondere bei vielen Geräten, vielen Ressourcentypen oder komplexen Netzwerken. Die Budgets begrenzen reservierte Mengen, nicht die Laufzeit einzelner RS-Speicherzugriffe. Ein MSPT-Rückfall ist keine Garantie gegen jede einzelne Spitze. Bei Bedarf `itemsPerStep` reduzieren. Keine allgemeine Zusicherung von Lagfreiheit oder absoluter Verlust-/Duplikationsfreiheit bei beliebigen Abstürzen und Fremdmods.

## Aus Quellen bauen

Java 21 und Internetzugang für die offiziellen Build-Abhängigkeiten:

```powershell
.\gradlew.bat clean build
```

Unter Linux/macOS: `sh gradlew clean build`. Ergebnis: `build/libs/neos-rs-disk-accelerator-1.0.0.jar`.

Der eigenständige Build verwendet ModDevGradle 2.0.147, NeoForge 21.1.251 und die offizielle RS-2.0.9-Release-JAR als reine Compile-Abhängigkeit. Keine fremden Mod-JARs werden eingebettet. Der Download unter `downloads/1.0.0/` ist identisch mit dem Release-Anhang.

## Lizenz

MIT, siehe `LICENSE`. Refined Storage und NeoForge werden separat bezogen und nicht mitgeliefert.