# NotesWriter für Mac – ohne eigenen Mac bauen

Der GitHub-Workflow **Mac-App bauen** erstellt zwei DMG-Dateien. Jede enthält
die App **SZ-Notenschreiber** samt Java- und JavaFX-Laufzeit. Die Empfänger
benötigen weder Java noch Maven oder IntelliJ.

## Einmalig einrichten

Die neuen Dateien unter `.github/workflows/`, `scripts/` und `docs/` sowie `.gitattributes` zusammen
mit den übrigen Projektänderungen committen und auf GitHub pushen. Der Workflow
muss für den manuellen Start auf dem Standardbranch des Repositorys liegen.
Falls GitHub danach fragt, GitHub Actions für das Repository aktivieren.
Es werden keine Apple-Zertifikate oder zusätzlichen Repository-Secrets benötigt.

## Mac-Dateien erzeugen und herunterladen

1. Auf GitHub das Repository öffnen und **Actions → Mac-App bauen** wählen.
2. **Run workflow** anklicken, den gewünschten Branch wählen und starten.
3. Warten, bis beide Jobs erfolgreich abgeschlossen sind.
4. Im abgeschlossenen Lauf unter **Artifacts** die gewünschten Downloads wählen:

| Download | Zielgerät |
| --- | --- |
| `SZ-Notenschreiber-macOS-arm64` | Mac mit Apple Silicon (M1, M2, M3 usw.) |
| `SZ-Notenschreiber-macOS-x86_64` | Mac mit Intel-Prozessor |

Die ZIP-Datei des GitHub-Downloads entpacken und die darin enthaltene **DMG**
weitergeben. Die `.sha256` enthält die Prüfsumme, `MACOS.md` diese Anleitung.
Die Testberichte sind ein separater Download und werden nicht zum Starten benötigt.
GitHub benötigt für den Artifact-Download eine Anmeldung. Für einen öffentlichen
Download ohne Anmeldung können die DMG-Dateien manuell an einen GitHub-Release
angehängt werden. Der Workflow veröffentlicht selbst keinen Release.

Die Pakete bleiben 30 Tage als Artifacts gespeichert. Für dauerhafte Downloads
rechtzeitig herunterladen oder an einen Release anhängen.

## Auf dem Mac installieren

1. Unter **Apple-Menü → Über diesen Mac** den Chip bzw. Prozessor nachsehen
   und das passende Paket auswählen.
2. DMG öffnen und **SZ-Notenschreiber.app** in **Programme** ziehen.
3. Die App aus **Programme** starten.
4. Für die Flöteneingabe den Mikrofonzugriff erlauben. Eine abgelehnte Freigabe
   lässt sich unter **Systemeinstellungen → Datenschutz & Sicherheit → Mikrofon** ändern.

Die Pakete sind lokal (ad hoc) signiert, aber **nicht mit einer Apple Developer ID
signiert und nicht notarisiert**. macOS kann den ersten Start deshalb blockieren.
Nur bei einer vertrauenswürdigen Datei nach dem Startversuch unter
**Systemeinstellungen → Datenschutz & Sicherheit → Dennoch öffnen** bestätigen.
Die systemweiten Sicherheitsfunktionen müssen dafür nicht deaktiviert werden.
Siehe [Apples Anleitung zum Öffnen von Apps](https://support.apple.com/de-de/102445).
Für eine reguläre Verteilung ohne diese zusätzliche Freigabe werden eine
Developer-ID-Signatur und Apple-Notarisierung benötigt; das ist hier nicht eingerichtet.

Die bestehenden Tastenkürzel verwenden weiterhin **Control (⌃)**, auch auf dem
Mac; **Alt** entspricht **Option (⌥)**. Beim Drucken kann im macOS-Druckdialog
über das PDF-Menü gespeichert werden.

## Neue Version bauen

Die Version in `src/main/java/com/code/noteswriter/AppInfo.java` ändern,
committen und pushen. Danach den Workflow erneut manuell starten.
Alternativ löst das Pushen eines passenden Tags (beispielsweise `v1.0.1` bei
App-Version `1.0.1`) den Build automatisch aus. Abweichende Tags werden abgelehnt.

Der Workflow nutzt macOS 15, JDK 22.0.2 und die im Projekt festgelegte
JavaFX-Version. Er führt die vorhandenen Tests aus, kontrolliert die App-Signatur
und Prozessorarchitektur und startet die App mit der mitgelieferten Laufzeit aus
einem leeren Arbeitsordner. Ein erfolgreicher Starttest ersetzt keine manuelle
Prüfung von Fensterdarstellung, Drucker und echtem Mikrofon auf einem Mac.
Eine Kompatibilität mit älteren macOS-Versionen ist noch nicht verifiziert.

Auf einem Mac mit JDK 22 lässt sich derselbe Build auch lokal ausführen:

```bash
bash scripts/build-macos.sh
```

Die Ergebnisse liegen unter `dist/macos/`. Windows kann diesen Paketierungsschritt
nicht lokal ausführen; die Mac-Rechner von GitHub übernehmen ihn.

Technische Grundlagen: [GitHub-Mac-Runner](https://docs.github.com/en/actions/reference/runners/github-hosted-runners),
[jpackage](https://docs.oracle.com/en/java/javase/22/docs/specs/man/jpackage.html).
