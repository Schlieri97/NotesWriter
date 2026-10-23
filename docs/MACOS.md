# macOS-Build

Der Workflow **Mac-App bauen** erzeugt DMG-Dateien für Apple-Silicon- und
Intel-Macs. Das Paket enthält Java und JavaFX; auf dem Zielgerät ist keine
Java-Installation erforderlich.

Auf GitHub unter **Actions → Mac-App bauen → Run workflow** starten. Die
Artifacts `SZ-Notenschreiber-macOS-arm64` und
`SZ-Notenschreiber-macOS-x86_64` enthalten jeweils DMG und SHA-256-Prüfsumme.

Die Pakete sind lokal signiert, aber nicht Apple-notarisiert. Beim ersten Start
kann macOS deshalb eine Bestätigung unter Datenschutz & Sicherheit verlangen.
