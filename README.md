# 🎵 NotesWriter   

**NotesWriter** ist eine JavaFX-Anwendung zur grafischen Darstellung und Verwaltung von Ces-Querflöten-Grifftabellen.  
Das Programm erlaubt das Zeichnen, Speichern, Laden und Drucken von Notenseiten mit Griffbildern.  

_**NotesWriter** is a JavaFX application for visualizing and managing fingering charts for the transverse flute._
_The program allows you to draw, save, load, and print note pages with fingering diagrams._

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
  - Fehlende Felder werden automatisch mit dem Griff „Cis/Des“ aufgefüllt._
  
  - _Prints the complete note page (A4 landscape).
  - _Missing slots are automatically filled with the “C#/Db” fingering.

- 🧹 **Löschen:** / _**Delete:**_
  - Entferne einzelne oder alle Noten über Schaltflächen.
  - _Remove individual notes or clear all notes using buttons._

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
- **„Linke Seite“ / „Rechte Seite“** → wählt die gewünschte Notenseite aus
- **„Pausenstrich“** → fügt einen waagerechten Pausenstrich ein
- **„Löschen“-Buttons** → letzte oder alle Noten entfernen
- **„Drucken“** → druckt beide Seiten (füllt leere Felder automatisch mit Griffen)

- _Click a note → adds the fingering image to the current page_
- _**"Switch Page"** → toggles between left and right page_
- _**"W-Open"** → inserts dividing lines_
- _**"Delete" buttons** → remove the last or all notes_
- _**"Print"** → prints both pages (auto-fills empty fields with fingering images)_

---

## 🖼️ Screenshots  
<img width="1811" height="981" alt="grafik" src="https://github.com/user-attachments/assets/fd59f518-c5dd-46cd-b215-36a423164035" />

---

## 👨‍💻 Author  

**Schlieri97**  
📧 [View GitHub Profile](https://github.com/Schlieri97)  
