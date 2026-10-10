# 🎵 NotesWriter   

**NotesWriter** ist eine JavaFX-Anwendung zur grafischen Darstellung und Verwaltung von Ces-Querflöten-Grifftabellen.  
Das Programm erlaubt das Zeichnen, Speichern, Laden und Drucken von Notenseiten mit Griffbildern.  

_**NotesWriter** is a JavaFX application for visualizing and managing fingering charts for the transverse flute._
_The program allows you to draw, save, load, and print note pages with fingering diagrams._

---

## Flöte per Mikrofon eingeben

Im Bereich **Noten einfügen → Flöte per Mikrofon eingeben** das Mikrofon und
das Instrument wählen, danach **Mikrofon starten** drücken.
Voreingestellt ist **Ces-Querflöte (Halbton tiefer)**: Zur gehörten Tonhöhe wird
ein Halbton addiert, um den notierten Ton zu erhalten. Zusätzlich stehen C-Querflöte,
C-Piccolo und die höher klingende Ces-Sopranflöte zur Auswahl. Letztere wird
um elf Halbtöne nach unten auf die notierte Tonhöhe umgesetzt.

Die separate **Grifflage (Oktaven)** passt diesen Ton an die Register der
Grifftabelle an. Für die hier verwendete Ces-Flöte ist sie anhand des gemeldeten
Messungen auf **−1** voreingestellt: ca. 550 Hz ergibt D mit Oktav-X,
ca. 1124 Hz normales D und ca. 2207 Hz das hohe D1. Die Oktav-Markierung
bezeichnet in dieser Grifftabelle die tiefere Lage. Das ist eine Anpassung an diese
Flöte und keine allgemeine Eigenschaft aller Ces-Flöten. Bei einer anderen
Flöte kann die Grifflage vor dem Start zwischen −2 und +2 geändert werden.
Ein Instrumentwechsel setzt sie auf −1 für Ces-Querflöte, sonst auf 0 zurück.
Die Anzeige „Grifflage“ bezeichnet die interne Zuordnung, nicht die klingende Oktave.

Einen einzelnen Ton etwa 0,3 Sekunden halten: Das erkannte Griffbild wird auf
der aktiven Seite eingefügt. Ein gehaltener Ton wird nur einmal übernommen;
für denselben Ton erneut etwa 0,2 Sekunden pausieren. Stabile Tonwechsel können
auch ohne Pause übernommen werden. **Mikrofon stoppen** beendet die Aufnahme.

Es werden keine Referenz-Tondateien benötigt, keine Aufnahmen gespeichert und
keine Audiodaten hochgeladen. Die Tonhöhe wird lokal aus dem Mikrofonsignal
berechnet. Notenlängen, Pausen und alternative Griffe werden weiterhin manuell
bearbeitet. Begleitmusik oder mehrere Instrumente gleichzeitig sind nicht vorgesehen.

Der Kammerton ist auf A = 440 Hz eingestellt und kann vor dem Start zwischen
430 und 450 Hz geändert werden. Bei Störgeräuschen den Empfindlichkeitsregler
nach rechts bewegen; bei leisen, nicht erkannten Tönen nach links. Einstellungen
können bei gestopptem Mikrofon geändert werden. Ist kein Zugriff möglich, in den
Windows-Datenschutzeinstellungen den Mikrofonzugriff für Desktop-Apps erlauben
und das gewählte Eingabegerät prüfen.

Nach Instrument- und Grifflagenkorrektur gilt intern: Die gemessenen
Ces-Referenzen für F_Oktav, Fis/Ges_Oktav, G_Oktav, F, Fis/Ges, F1 und G1 liegen bei
ca. 670, 718, 736, 1350, 1400, 2888 und 2972 Hz. Die Grenze zwischen
Fis/Ges_Oktav und G_Oktav liegt bei 727 Hz. Die übrigen Noten verwenden
weiterhin die MIDI-Zuordnung.
Bei Piccolo wird der gehörte Ton zuvor um eine Oktave nach unten versetzt.
Außerhalb dieses Bereichs wird keine Note eingefügt; insbesondere fehlt im
bisherigen Griffmodell ein tiefes C4. Die Alternative F_1 wird nicht automatisch
gewählt, da sich verschiedene Griffe derselben Tonhöhe akustisch nicht unterscheiden lassen.

---

## 🚀 Funktionen / _Features_

- 🎶 **Interaktive Notenanzeige:** / _**Interactive Note Display:**_
  - Darstellung der Griffe für verschiedene Töne (inklusive Oktav-Noten).
  - _Shows fingering diagrams for different tones (including octave notes)._

- 🧩 **Zwei Seiten („Links“ & „Rechts“):** / _**Two Pages ("Left" & "Right"):**_
  - Umschalten zwischen zwei Notenseiten innerhalb des Programms.
  - _Switch between two separate note pages within the program._

- 💾 **Speichern & Laden:** / _**Save & Load:**_
  - Exportiere deine Noten in eine Textdatei und lade sie später wieder.  
  - „Alle Noten überladen“ → ersetzt die vorhandenen Noten
  - „Noten hinzufügen“ → fügt neue Noten an die bestehenden an
    
  - _Export your notes to a text file and load them later._
  - _“Overwrite all notes” → replaces existing ones_
  - _“Add notes” → appends new notes to the existing list_

- 🖨️ **Drucken:** / _**Printing:**_
  - Druckt die komplette Notenseite (A4-Querformat)._
  - Wahlweise freie Felder mit „Cis/Des“ auffüllen oder Leerstellen beibehalten.
  - Bewusst eingefügte leere Notenblöcke bleiben in beiden Varianten frei.
  
  - _Prints the complete note page (A4 landscape).
  - _Choose whether to fill unused slots with “C#/Db” or preserve empty spaces. Explicit blank blocks stay empty._

- 🧹 **Löschen:** / _**Delete:**_
  - Entferne markierte Noten über das Kontextmenü oder mit Entf/Backspace.
  - Entf/Backspace löscht zuerst markierte Noten, Pausenstriche oder Wiederholungszeichen; ohne Auswahl die letzte Eingabe.
  - Markierter Freitext lässt sich ebenfalls mit Entf/Backspace löschen. Escape hebt jede Markierung auf.
  - _Delete selected elements with Backspace/Delete or the context menu._

- 🪶 **Design:**
  - JavaFX-Buttons mit Icons
  - Farbliche Seitenanzeige (Blau = linke Seite, Rot = rechte Seite)
  - Abgerundete, stilisierte Bedienelemente

  - _JavaFX buttons with icons_ 
  - _Colored page indication (blue = left page, red = right page)_
  - _Rounded, stylized controls_

---

## 🛠️ Voraussetzungen / _Requirements_

- Java **17** oder neuer
- JavaFX (z. B. über [OpenJFX](https://openjfx.io/))
- Maven oder IntelliJ IDEA (Projekt basiert auf `pom.xml`)
  
- _Java **17** or newer_
- _JavaFX (e.g., via [OpenJFX](https://openjfx.io/))_
- _Maven or IntelliJ IDEA (project based on `pom.xml`)_

---

## ▶️ Starten / How to Run
### **1. Repository klonen** / _Clone the repository_
```bash
git clone https://github.com/Schlieri97/NotesWriter.git
cd NotesWriter
```

### **2. Starten mit Maven** / _Run with Maven_
```bash
mvn clean javafx:run
```

Oder in **IntelliJ IDEA**:
1. Öffne das Projektverzeichnis.
2. Stelle sicher, dass „JavaFX“ korrekt eingebunden ist.
3. Starte die `Main`-Klasse.
   
_Or in **IntelliJ IDEA**:_
1. _Open the project directory._
2. _Make sure JavaFX is properly configured._
3. _Run the `Main` class._

---

## 📂 Projektstruktur / _Project Structure_

```
NotesWriter/
 ├── src/
 │   ├── main/
 │   │   ├── java/com/code/noteswriter/
 │   │   │   ├── Controller.java
 │   │   │   ├── Model.java
 │   │   │   └── Main.java
 │   │   └── resources/com/code/noteswriter/Bilder/
 │   │       ├── D.jpg
 │   │       ├── Cis_Des.jpg
 │   │       └── ...
 │   └── test/
 ├── pom.xml
 ├── mvnw, mvnw.cmd
 └── .gitignore
```

---

## 💾 Dateiformat / _File Format_  

Beim Speichern entsteht eine Textdatei mit folgendem Aufbau:  
_When saving, a text file is created with the following format:_

```
Seite1;C
Seite1;D
Seite2;E
```
Jede Zeile steht für eine Note und die zugehörige Seite.  
_Each line represents a note and its corresponding page._

---

## 🧠 Hinweise zur Bedienung / _Usage Tips_

- Klick auf Note → fügt Notenbild in die aktuelle Seite ein
- Note per Drag-and-Drop auf eine Seite ziehen → an der Zeigerposition einfügen, auch zwischen vorhandenen Noten
- „Tastenkürzel & Tonvarianten“ → Details bei Bedarf aufklappen
- **„Linke Seite“ / „Rechte Seite“** → wählt die gewünschte Notenseite aus
- **„Pausenstrich“** → fügt einen waagerechten Pausenstrich ein
- **↶ Rückgängig / ↷ Wiederherstellen** → Bearbeitungsschritte zurücknehmen oder erneut ausführen; auch mit **Strg+Z** und **Strg+Umschalt+Z**
- **„Leerer Notenblock“ / Enter** → beendet den begonnenen Block und lässt eine ganze Zeile frei, auch als letzte Zeile der Seite; der freie Block ist im Editor beschriftet, die Beschriftung erscheint nicht im Druck/PDF
- **Freitext** → nach dem Einfügen per Rechtsklick die Schriftgröße wählen oder eine eigene Größe zwischen 8 und 72 pt eingeben
- **Backspace / Entf** → löscht die letzte Eingabe der aktiven Seite; ein zuletzt eingefügter Leerblock wird als Ganzes entfernt
- **„Drucken / PDF exportieren“** → wahlweise mit Cis/Des auffüllen oder Leerstellen beibehalten; für PDF einen PDF-Drucker auswählen

- _Click a note → adds the fingering image to the current page_
- _**"Switch Page"** → toggles between left and right page_
- _**"W-Open"** → inserts dividing lines_
- _**Backspace / Delete** → delete the selection, or the last entry when nothing is selected_
- _**"Print / export PDF"** → choose whether to fill unused slots; select a PDF printer to export PDF_

### Tastatur

Tastenkürzel gelten außerhalb von Textfeldern und hängen neue Noten an die aktive Seite an.
Ein gehaltener Ton wird nicht wiederholt eingefügt. Die Tasten stehen auch in den Tooltips der Notenbuttons.

| Taste | Ton |
| --- | --- |
| C, D, E, F, G, A, H | C1, D, E, F, G, A, H |
| S, R, T, Z, B | Cis/Des, Dis/Es, Fis/Ges, Gis/As, Ais/B |
| V | Alternativer F-Griff (F_1) |
| 1, 2, 3, 4, 5 | C1, Cis/Des1, D1, Dis/Es1, E1 |
| 6, 7, 8, 9, 0 | F1, Fis/Ges1, G1, Gis/As1, A1 |

| Kombination mit Tontaste | Variante |
| --- | --- |
| Strg | Lang |
| Alt | Kurz |
| Umschalt | Oktav |
| Strg + Umschalt | Oktav lang |
| Alt + Umschalt | Oktav kurz |
| Strg + Alt | Feste |

Beispiel: **D** schreibt D, **Strg+D** ein langes D und **Alt+Umschalt+D** ein kurzes Oktav-D.
Nicht verfügbare Oktav-Varianten werden mit einem Hinweis abgewiesen.
Ein Leerblock wird als `Seite1;LEERBLOCK` bzw. `Seite2;LEERBLOCK` gespeichert.

---

## 🖼️ Screenshots  
<img width="1811" height="981" alt="grafik" src="https://github.com/user-attachments/assets/fd59f518-c5dd-46cd-b215-36a423164035" />

---

## 👨‍💻 Author  

**Schlieri97**  
📧 [View GitHub Profile](https://github.com/Schlieri97)  
