package com.code.noteswriter;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.print.PageLayout;
import javafx.print.PageOrientation;
import javafx.print.Paper;
import javafx.print.Printer;
import javafx.scene.Node;
import javafx.scene.Group;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.CubicCurveTo;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Line;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.stage.WindowEvent;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Controller {
    @FXML private ComboBox<MicrophoneInput.Device> microphoneDevice;
    @FXML private ComboBox<FluteInstrument> microphoneInstrument;
    @FXML private Slider microphoneThreshold;
    @FXML private Spinner<Integer> concertPitch;
    @FXML private Spinner<Integer> microphoneOctave;
    @FXML private Button microphoneButton;
    @FXML private Label microphoneStatus;
    private MicrophoneInput microphone;
    private String lastMicrophoneDetection;

    private void setupMicrophone() {
        if (microphoneDevice == null) return;
        microphoneDevice.getItems().setAll(MicrophoneInput.devices());
        microphoneDevice.getSelectionModel().selectFirst();
        microphoneInstrument.getItems().setAll(FluteInstrument.values());
        microphoneInstrument.getSelectionModel().select(FluteInstrument.CES_FLUTE);
        concertPitch.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(430, 450, 440));
        // Measured Ces registers: ~550 Hz = D_Oktav, ~1124 Hz = D, ~2207 Hz = D1.
        microphoneOctave.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(-2, 2, -1));
        microphoneInstrument.valueProperty().addListener((observable, previous, instrument) -> {
            microphoneOctave.getValueFactory().setValue(instrument == FluteInstrument.CES_FLUTE ? -1 : 0);
        });
    }

    @FXML private void handleMicrophoneAction() {
        if (microphone != null) {
            stopMicrophone();
            return;
        }
        MicrophoneInput input = new MicrophoneInput();
        lastMicrophoneDetection = null;
        microphone = input;
        setMicrophoneControls(true);
        microphoneStatus.setText("Mikrofon wird geöffnet …");
        input.start(microphoneDevice.getValue(), microphoneInstrument.getValue(),
                concertPitch.getValue(), microphoneOctave.getValue(), Math.pow(10, microphoneThreshold.getValue() / 20),
                reading -> javafx.application.Platform.runLater(() -> {
                    if (microphone != input) return;
                    acceptMicrophoneReading(reading);
                }), message -> javafx.application.Platform.runLater(() -> {
                    if (microphone != input) return;
                    stopMicrophone();
                    microphoneStatus.setText(message);
                }));
    }

    void acceptMicrophoneReading(MicrophoneInput.Reading reading) {
        if (reading.midi() < 0) {
            microphoneStatus.setText(lastMicrophoneDetection == null
                    ? "Bereit – bitte einen einzelnen Ton spielen."
                    : lastMicrophoneDetection + " · Pause");
            return;
        }
        FlutePitchMapping.Note note = FlutePitchMapping.forMidi(reading.midi());
        String detected = String.format(Locale.GERMAN, "Gemessen: %.2f Hz · Grifflage: %s",
                reading.frequency(), FlutePitchMapping.name(reading.midi()));
        if (note == null) {
            lastMicrophoneDetection = detected + ": kein Griffbild vorhanden (Grifflage D4–A6).";
            microphoneStatus.setText(lastMicrophoneDetection);
            return;
        }
        if (reading.insert() && model != null) {
            if (rowOffset >= 119) {
                stopMicrophone();
                microphoneStatus.setText("Seite voll. Andere Seite wählen und Mikrofon erneut starten.");
                return;
            }
            clearAllSelection();
            addNote(note.base(), note.modifier());
        }
        lastMicrophoneDetection = detected + " → " + note;
        microphoneStatus.setText(lastMicrophoneDetection + (reading.insert() ? " · übernommen" : " · erkannt"));
    }

    public void stopMicrophone() {
        MicrophoneInput previous = microphone;
        microphone = null;
        if (previous != null) previous.close();
        if (microphoneButton != null) {
            setMicrophoneControls(false);
            microphoneStatus.setText("Mikrofon aus. Es werden keine Tondateien gespeichert.");
        }
    }

    private void setMicrophoneControls(boolean running) {
        microphoneButton.setText(running ? "Mikrofon stoppen" : "Mikrofon starten");
        microphoneDevice.setDisable(running);
        microphoneInstrument.setDisable(running);
        microphoneThreshold.setDisable(running);
        concertPitch.setDisable(running);
        microphoneOctave.setDisable(running);
    }
    @FXML
    public StackPane printArea;
    @FXML
    private Group pageContent;
    @FXML
    private StackPane pageCanvas;
    @FXML
    private Pane emptyBlockOverlay;
    @FXML
    private Pane pauseOverlay;
    @FXML
    private Pane annotationOverlay;
    @FXML
    private Label slurSelectionLabel;
    @FXML
    private Button clearNoteSelectionButton;
    @FXML
    private Button deleteSelectedNotesButton;
    @FXML
    private Button undoButton;
    @FXML
    private Button redoButton;
    @FXML
    private TextField leftCircleText, rightCircleText, leftMarchText, rightMarchText, freeTextInput;

    // DIN-A4-Landschaftsfläche des bisherigen Hintergrunds (1290 x 910 px).
    // Bild und Noten werden nicht mehr gestreckt, sondern gemeinsam skaliert.
    private static final double PAGE_WIDTH = 1290.0;
    private static final double PAGE_HEIGHT = 910.0;
    private static final double NOTE_SLOT_WIDTH = 8.4;
    private static final double NOTE_GAP = 5.5;
    private static final String EMPTY_BLOCK = "LEERBLOCK";
    private static final String EMPTY_BLOCK_OWNER = "emptyBlockOwner";
    private static final String EMPTY_BLOCK_GUIDE = "LEERER NOTENBLOCK – WIRD NICHT GEDRUCKT";
    private static final double EMPTY_BLOCK_GUIDE_WIDTH = 41 * (NOTE_SLOT_WIDTH + NOTE_GAP) - NOTE_GAP;
    private static final int DEFAULT_CIRCLE_FONT_SIZE = 18;
    private static final int DEFAULT_MARCH_FONT_SIZE = 22;
    private static final Map<KeyCode, String> NOTE_KEYS = Map.ofEntries(
            Map.entry(KeyCode.C, "C1"), Map.entry(KeyCode.D, "D"),
            Map.entry(KeyCode.E, "E"), Map.entry(KeyCode.F, "F"),
            Map.entry(KeyCode.G, "G"), Map.entry(KeyCode.A, "A"),
            Map.entry(KeyCode.H, "H"), Map.entry(KeyCode.B, "Ais_B"),
            Map.entry(KeyCode.S, "Cis_Des"), Map.entry(KeyCode.R, "Dis_Es"),
            Map.entry(KeyCode.T, "Fis_Ges"), Map.entry(KeyCode.Z, "Gis_As"),
            Map.entry(KeyCode.V, "F_1"),
            Map.entry(KeyCode.DIGIT1, "C1"), Map.entry(KeyCode.DIGIT2, "Cis_Des1"),
            Map.entry(KeyCode.DIGIT3, "D1"), Map.entry(KeyCode.DIGIT4, "Dis_Es1"),
            Map.entry(KeyCode.DIGIT5, "E1"), Map.entry(KeyCode.DIGIT6, "F1"),
            Map.entry(KeyCode.DIGIT7, "Fis_Ges1"), Map.entry(KeyCode.DIGIT8, "G1"),
            Map.entry(KeyCode.DIGIT9, "Gis_As1"), Map.entry(KeyCode.DIGIT0, "A1"));
    private final Set<KeyCode> pressedKeys = EnumSet.noneOf(KeyCode.class);
    private final Set<Button> noteInputButtons = new HashSet<>();
    private final EventHandler<KeyEvent> keyPressedHandler = this::handleNoteKeyPressed;
    private final EventHandler<KeyEvent> keyReleasedHandler = event -> pressedKeys.remove(event.getCode());
    private final EventHandler<MouseEvent> selectionMouseHandler = this::clearSelectionWhenClickedOutsideNotes;

    private Model model;
    private int rowOffset = 1;
    private int columnOffset = 0;
    private final List<Node> selectedNotes = new ArrayList<>();
    private final List<String> notesSeite1 = new ArrayList<>();
    private final List<String> notesSeite2 = new ArrayList<>();
    private final List<PauseMarker> pauseMarkers = new ArrayList<>();
    private final List<PageTextAnnotation> textAnnotations = new ArrayList<>();
    private final List<SlurAnnotation> slurAnnotations = new ArrayList<>();
    private final Map<TextField, Integer> pageHeadingSizes = new IdentityHashMap<>();
    private boolean documentDirty;
    private static final int HISTORY_LIMIT = 100;
    private final Deque<DocumentSnapshot> undoHistory = new ArrayDeque<>();
    private final Deque<DocumentSnapshot> redoHistory = new ArrayDeque<>();
    private DocumentSnapshot savedDocumentSnapshot;
    private boolean historySuspended;
    private boolean draggingTextHistoryRecorded;

    private record DocumentSnapshot(String leftCircle, String leftMarch, String rightCircle,
                                    String rightMarch, int leftCircleSize, int leftMarchSize,
                                    int rightCircleSize, int rightMarchSize,
                                    String body, boolean rightPage, boolean dirty) {
    }

    private static final class PauseMarker {
        private GridPane grid;
        private final Line line;
        private int column;
        private int row;
        private boolean free;
        private boolean dragging;

        private PauseMarker(GridPane grid, Line line, int column, int row) {
            this.grid = grid;
            this.line = line;
            this.column = column;
            this.row = row;
        }
    }

    private static final class PageTextAnnotation {
        private final GridPane grid;
        private final Text text;

        private PageTextAnnotation(GridPane grid, Text text) {
            this.grid = grid;
            this.text = text;
        }
    }

    private static final class SlurAnnotation {
        private final GridPane grid;
        private final Path path;
        private final List<NoteSlot> slots;

        private SlurAnnotation(GridPane grid, Path path, List<NoteSlot> slots) {
            this.grid = grid;
            this.path = path;
            this.slots = slots;
        }
    }

    private record NoteSlot(int column, int row) {
    }
    @FXML
    public Button Cis_Des, D, Dis_Es, E,
            F, F_1, Fis_Ges, G, Gis_As, A, Ais_B, H, C1, Cis_Des1, D1, Dis_Es1, E1, Fis_Ges1, F1, G1, Gis_As1, A1,
            Speichern, Laden, printGridPaneButton, Wiederholungszeichen, leftPageButton, rightPageButton,
            pauseStrokeButton, repeatStartButton, repeatEndButton,
            addFreeTextButton, helpButton, emptyBlockButton;
    @FXML
    private Label keyboardStatus;

    @FXML
    public GridPane Gridpane1;
    @FXML
    public GridPane Gridpane2;
    private GridPane activeGridPane;
    private int rowOffset1 = 1;
    private int columnOffset1 = 0;
    private int rowOffset2 = 1;
    private int columnOffset2 = 0;
    private ContextMenu activeContextMenu;
    private StackPane selectedNoteContainer;
    private GridPane selectedNoteGrid;
    private final List<StackPane> selectedNoteContainers = new ArrayList<>();
    private final Set<Text> selectedTextAnnotations = new LinkedHashSet<>();
    private Path selectedSlurPath;
    private final List<PauseMarker> selectedPauseMarkers = new ArrayList<>();
    private Rectangle selectionRectangle;
    private Point2D selectionStart;
    private boolean draggingSelection;
    public void setModel(Model model) {
        this.model = model;
        selectPage(Gridpane1);
        savedDocumentSnapshot = captureDocumentSnapshot();
    }

    private void markDocumentDirty() {
        recordUndoCheckpoint();
        documentDirty = true;
    }

    private DocumentSnapshot captureDocumentSnapshot() {
        StringWriter buffer = new StringWriter();
        try {
            BufferedWriter writer = new BufferedWriter(buffer);
            writeGridTokens(writer, "Seite1", Gridpane1);
            writeGridTokens(writer, "Seite2", Gridpane2);
            writeAnnotations(writer, "Seite1", Gridpane1);
            writeAnnotations(writer, "Seite2", Gridpane2);
            writer.flush();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
        return new DocumentSnapshot(leftCircleText.getText(), leftMarchText.getText(),
                rightCircleText.getText(), rightMarchText.getText(),
                pageHeadingSize(leftCircleText), pageHeadingSize(leftMarchText),
                pageHeadingSize(rightCircleText), pageHeadingSize(rightMarchText), buffer.toString(),
                activeGridPane == Gridpane2, documentDirty);
    }

    private void recordUndoCheckpoint() {
        if (historySuspended || model == null) return;
        pushUndoSnapshot(captureDocumentSnapshot());
    }

    private void pushUndoSnapshot(DocumentSnapshot snapshot) {
        if (!sameDocumentContent(snapshot, undoHistory.peekLast())) {
            undoHistory.addLast(snapshot);
            if (undoHistory.size() > HISTORY_LIMIT) undoHistory.removeFirst();
        }
        redoHistory.clear();
        updateHistoryButtons();
    }

    private void recordPageTextEdit(TextField field, String previousValue) {
        if (!historySuspended && model != null) {
            DocumentSnapshot current = captureDocumentSnapshot();
            pushUndoSnapshot(new DocumentSnapshot(
                    field == leftCircleText ? previousValue : current.leftCircle(),
                    field == leftMarchText ? previousValue : current.leftMarch(),
                    field == rightCircleText ? previousValue : current.rightCircle(),
                    field == rightMarchText ? previousValue : current.rightMarch(),
                    current.leftCircleSize(), current.leftMarchSize(),
                    current.rightCircleSize(), current.rightMarchSize(),
                    current.body(), current.rightPage(), current.dirty()));
        }
        documentDirty = true;
    }

    private boolean sameDocumentContent(DocumentSnapshot first, DocumentSnapshot second) {
        return sameDocumentData(first, second) && first.rightPage() == second.rightPage();
    }

    private boolean sameDocumentData(DocumentSnapshot first, DocumentSnapshot second) {
        return second != null && Objects.equals(first.leftCircle(), second.leftCircle())
                && Objects.equals(first.leftMarch(), second.leftMarch())
                && Objects.equals(first.rightCircle(), second.rightCircle())
                && Objects.equals(first.rightMarch(), second.rightMarch())
                && first.leftCircleSize() == second.leftCircleSize()
                && first.leftMarchSize() == second.leftMarchSize()
                && first.rightCircleSize() == second.rightCircleSize()
                && first.rightMarchSize() == second.rightMarchSize()
                && Objects.equals(first.body(), second.body());
    }

    private void updateHistoryButtons() {
        if (undoButton != null) undoButton.setDisable(undoHistory.isEmpty());
        if (redoButton != null) redoButton.setDisable(redoHistory.isEmpty());
    }

    @FXML
    private void handleUndoAction(ActionEvent event) {
        if (undoHistory.isEmpty()) return;
        DocumentSnapshot current = captureDocumentSnapshot();
        DocumentSnapshot previous = undoHistory.removeLast();
        redoHistory.addLast(current);
        restoreDocumentSnapshot(previous);
        updateHistoryButtons();
    }

    @FXML
    private void handleRedoAction(ActionEvent event) {
        if (redoHistory.isEmpty()) return;
        DocumentSnapshot current = captureDocumentSnapshot();
        DocumentSnapshot next = redoHistory.removeLast();
        undoHistory.addLast(current);
        restoreDocumentSnapshot(next);
        updateHistoryButtons();
    }

    private void restoreDocumentSnapshot(DocumentSnapshot snapshot) {
        historySuspended = true;
        try {
            if (activeContextMenu != null) activeContextMenu.hide();
            clearAllPagesBeforeOverwrite();
            leftCircleText.setText(snapshot.leftCircle());
            leftMarchText.setText(snapshot.leftMarch());
            rightCircleText.setText(snapshot.rightCircle());
            rightMarchText.setText(snapshot.rightMarch());
            applyPageHeadingSize(leftCircleText, snapshot.leftCircleSize());
            applyPageHeadingSize(leftMarchText, snapshot.leftMarchSize());
            applyPageHeadingSize(rightCircleText, snapshot.rightCircleSize());
            applyPageHeadingSize(rightMarchText, snapshot.rightMarchSize());
            List<PendingSlur> pendingSlurs = new ArrayList<>();
            for (String line : snapshot.body().lines().toList()) {
                String[] parts = splitSerializedLine(line);
                if (parts == null || !selectPageForSerializedToken(parts[0])) continue;
                if (parts[1].startsWith("BOGEN|")) {
                    pendingSlurs.add(new PendingSlur(parts[0], parts[1]));
                } else {
                    appendSerializedToken(parts[1]);
                }
            }
            restorePendingSlurs(pendingSlurs);
            selectPage(snapshot.rightPage() ? Gridpane2 : Gridpane1);
            documentDirty = savedDocumentSnapshot == null ? snapshot.dirty()
                    : !sameDocumentData(snapshot, savedDocumentSnapshot);
        } finally {
            historySuspended = false;
        }
    }

    /**
     * Wird vom Hauptfenster vor dem Schließen aufgerufen.
     * Bei Abbrechen oder einem abgebrochenen/fehlgeschlagenen Speichern bleibt
     * das Fenster geöffnet.
     */
    public void handleCloseRequest(WindowEvent event) {
        stopMicrophone();
        if (!documentDirty) {
            return;
        }

        Window owner = event.getSource() instanceof Window window ? window : null;
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Ungespeicherte Änderungen");
        alert.setHeaderText("Möchtest du die Änderungen vor dem Schließen speichern?");
        alert.setContentText("Nicht gespeicherte Noten und Texte gehen sonst verloren.");
        if (owner != null) {
            alert.initOwner(owner);
        }

        ButtonType save = new ButtonType("Speichern", ButtonBar.ButtonData.YES);
        ButtonType discard = new ButtonType("Nicht speichern", ButtonBar.ButtonData.NO);
        ButtonType cancel = new ButtonType("Abbrechen", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(save, discard, cancel);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isEmpty() || result.get() == cancel) {
            event.consume();
        } else if (result.get() == save && !saveToFile(owner)) {
            event.consume();
        }
    }

    private static final Set<String> NO_OKTAV_NOTES = Set.of(
            "Cis_Des",
            "Dis_Es1",
            "E1",
            "Fis_Ges1",
            "F1",
            "G1",
            "Gis_As1",
            "A1"
    );

    @FXML
    public void initialize() {
        setupMicrophone();
        setupResponsivePage();
        setupAllNoteButtons();
        styleButtonsNotes(Cis_Des);
        styleButtonsNotes(D);
        styleButtonsNotes(Dis_Es);
        styleButtonsNotes(E);
        styleButtonsNotes(F);
        styleButtonsNotes(F_1);
        styleButtonsNotes(Fis_Ges);
        styleButtonsNotes(G);
        styleButtonsNotes(Gis_As);
        styleButtonsNotes(A);
        styleButtonsNotes(Ais_B);
        styleButtonsNotes(H);
        styleButtonsNotes(C1);
        styleButtonsNotes(Cis_Des1);
        styleButtonsNotes(D1);
        styleButtonsNotes(Dis_Es1);
        styleButtonsNotes(E1);
        styleButtonsNotes(Fis_Ges1);
        styleButtonsNotes(F1);
        styleButtonsNotes(G1);
        styleButtonsNotes(Gis_As1);
        styleButtonsNotes(A1);
        styleButtonsOtherButtons(Speichern);
        styleButtonsOtherButtons(Laden);
        styleButtonsOtherButtons(printGridPaneButton);
        styleButtonsOtherButtons(undoButton);
        styleButtonsOtherButtons(redoButton);
        styleButtonsOtherButtons(Wiederholungszeichen);
        styleButtonsOtherButtons(pauseStrokeButton);
        styleButtonsOtherButtons(addFreeTextButton);
        styleButtonsOtherButtons(emptyBlockButton);
        styleHelpButton(helpButton);
        stylePageButton(leftPageButton);
        stylePageButton(rightPageButton);
        setupPageTextFields();
        addIconsToNoteButtons();
        setupTooltips();
        setupDragAndDrop();
        setupMouseSelection();
        updateHistoryButtons();
    }

    private void setupTooltips() {
        helpButton.setTooltip(new Tooltip("Kurzanleitung und Kontaktinformationen öffnen"));
        leftPageButton.setTooltip(new Tooltip("Linke Notenseite aktivieren"));
        rightPageButton.setTooltip(new Tooltip("Rechte Notenseite aktivieren"));
        undoButton.setTooltip(new Tooltip("Letzte Änderung rückgängig machen (Strg+Z)"));
        redoButton.setTooltip(new Tooltip("Rückgängig gemachte Änderung wiederherstellen (Strg+Umschalt+Z)"));
        emptyBlockButton.setTooltip(new Tooltip("Aktuellen Block abschließen und eine ganze Zeile frei lassen, auch am Seitenende · Enter"));
        Wiederholungszeichen.setTooltip(new Tooltip("Normalen Strich oder Wiederholungszeichen auswählen"));
        pauseStrokeButton.setTooltip(new Tooltip("Pausenstrich an der aktuellen Position einfügen"));
        addFreeTextButton.setTooltip(new Tooltip("Den Text aus dem Eingabefeld auf der Seite platzieren"));
        Speichern.setTooltip(new Tooltip("Die aktuelle Notenschrift als Datei speichern"));
        Laden.setTooltip(new Tooltip("Eine gespeicherte Notenschrift öffnen"));
        printGridPaneButton.setTooltip(new Tooltip("Drucken oder als PDF ausgeben – mit oder ohne Cis/Des-Auffüllung"));
        freeTextInput.setTooltip(new Tooltip("Text eingeben, der auf der Seite erscheinen soll"));

        for (Button noteButton : List.of(
                Cis_Des, D, Dis_Es, E, F, F_1, Fis_Ges, G, Gis_As,
                A, Ais_B, H, C1, Cis_Des1, D1, Dis_Es1, E1,
                Fis_Ges1, F1, G1, Gis_As1, A1)) {
            String keys = NOTE_KEYS.entrySet().stream()
                    .filter(entry -> entry.getValue().equals(noteButton.getText()))
                    .map(entry -> entry.getKey().getName()).sorted()
                    .collect(java.util.stream.Collectors.joining(" / "));
            noteButton.setTooltip(new Tooltip("Taste: " + keys +
                    "\nStrg: lang · Alt: kurz · Umschalt: Oktav · Strg+Alt: feste" +
                    "\nLinksklick: einfügen · Rechtsklick: Varianten"));
        }
    }

    @FXML
    private void handleHelpAction(ActionEvent event) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Hilfe und Kontakt – " + AppInfo.NAME);
        dialog.setHeaderText("Bedienung und Kontakt – Version " + AppInfo.VERSION);
        if (helpButton.getScene() != null) {
            dialog.initOwner(helpButton.getScene().getWindow());
        }

        TextArea helpText = new TextArea("""
                %s
                Version: %s

                Bedienung – Schritt für Schritt

                Seite auswählen
                • Oben mit „Linke Seite“ oder „Rechte Seite“ die aktive Notenseite auswählen.
                • Neue Noten, Pausen, Wiederholungszeichen und Texte werden auf der aktiven Seite eingefügt.
                • Das aktuelle Element wird automatisch an der nächsten freien Position platziert.

                Noten einfügen
                • Einen Noten-Button mit der linken Maustaste anklicken, um die normale Note einzufügen.
                • Einen Noten-Button mit der rechten Maustaste anklicken, um eine Variante auszuwählen:
                  Lang (-), Kurz (•), Feste (f), Oktav (x) sowie Oktav + Lang oder Oktav + Kurz.
                • Nicht bei jeder Note stehen Oktav-Varianten zur Verfügung.
                • Noten-Buttons können auch mit gedrückter linker Maustaste auf die Seite gezogen werden.
                  Beim Ablegen auf der linken oder rechten Seitenhälfte wird diese Seite aktiv.
                • Wenn eine Note oder ein Wiederholungszeichen markiert ist, ersetzt ein Klick auf einen
                  Noten-Button das markierte Element beziehungsweise alle markierten Elemente.

                Noten mit der Tastatur schreiben
                • C = C1, D = D, E = E, F = F, G = G, A = A, H = H und B = Ais/B.
                • S = Cis/Des, R = Dis/Es, T = Fis/Ges, Z = Gis/As, V = alternativer F-Griff (F_1).
                • Hohe Töne auf der Zahlenreihe:
                  1 = C1, 2 = Cis/Des1, 3 = D1, 4 = Dis/Es1, 5 = E1,
                  6 = F1, 7 = Fis/Ges1, 8 = G1, 9 = Gis/As1, 0 = A1.
                • Strg + Tontaste = lang; Alt + Tontaste = kurz; Umschalt + Tontaste = Oktav.
                • Strg + Umschalt + Tontaste = Oktav lang; Alt + Umschalt + Tontaste = Oktav kurz.
                • Strg + Alt + Tontaste = feste. Nicht verfügbare Oktav-Varianten werden nicht eingefügt.
                • Beispiel: D schreibt D, Strg+D schreibt ein langes D, Alt+Umschalt+D ein kurzes Oktav-D.
                • Tastatureingaben hängen neue Noten an, auch wenn eine ältere Note markiert ist.
                • Pro Tastendruck entsteht eine Note. In Textfeldern gelten die normalen Texteingaben.

                Einen neuen Marsch beginnen
                • „Leerer Notenblock“ oder Enter schließt den begonnenen Block ab und lässt danach einen
                  ganzen waagerechten Notenblock leer. Der nächste Marsch beginnt im folgenden Block.
                • Am Anfang eines Blocks wird direkt dieser Block frei gelassen.
                • Der Abstand bleibt beim Speichern, Laden und beim Auffüllen mit Cis/Des erhalten.
                • Backspace / Entf nimmt einen zuletzt eingefügten Leerblock vollständig zurück.

                Elemente markieren
                • Eine Note oder ein Wiederholungszeichen mit einem einfachen Linksklick markieren.
                • Ein erneuter Linksklick auf das bereits markierte Element hebt die Auswahl wieder auf.
                • Mehrere Elemente markieren: In einem freien Bereich der Notenseite die linke Maustaste
                  drücken, gedrückt halten und ein Auswahlrechteck über die gewünschten Elemente ziehen.
                  Erst beim Loslassen der linken Maustaste wird die Auswahl übernommen.
                • Ein Linksklick außerhalb der markierten Noten hebt die Auswahl auf und schließt das
                  Auswahlmenü. Ein Klick auf eine andere Note wählt diese Note aus.
                • Mit einem Rechtsklick auf ein markiertes Element öffnet sich das Auswahlmenü.
                  Dort stehen „Auswahl aufheben“ und „Ausgewählte Elemente löschen“ zur Verfügung.

                Bögen einfügen
                • Mindestens zwei Noten auf derselben Seite markieren.
                • Mit der rechten Maustaste auf eine markierte Note klicken und „Bogen über Auswahl“ wählen.
                • Ein Bogen kann nicht über Wiederholungszeichen oder über zwei verschiedene Seiten gelegt werden.
                • Einen vorhandenen Bogen mit der rechten Maustaste anklicken und „Bogen löschen“ wählen.

                Wiederholungszeichen und Pausenstrich
                • „Wiederholungszeichen“ anklicken und eine Variante auswählen:
                  Normaler Strich |, Doppelstrich ||, Gestrichelter Strich ⋮,
                  Wiederholungsanfang :|| oder Wiederholungsende ||:.
                • Ohne Auswahl wird das Zeichen an der nächsten freien Position eingefügt.
                  Bei einer Auswahl ersetzt es die markierten Noten oder Zeichen.
                • „Pausenstrich“ fügt einen Pausenstrich an der aktuellen Einfügeposition ein.
                • Einen markierten Pausenstrich mit gedrückter linker Maustaste zu einer Note ziehen:
                  In ihrer Nähe rastet er ein.
                • Rechtsklick auf den Pausenstrich und „Pausenstrich frei platzieren“ wählen,
                  dann mit der linken Maustaste an eine beliebige Stelle der Seite ziehen.
                • Ein Pausenstrich kann mit der rechten Maustaste und „Pausenstrich löschen“ entfernt werden.
                • Das Wiederholungszeichen kann auf die Seite gezogen werden; dadurch wird ein
                  Wiederholungsanfang eingefügt. Der Pausenstrich kann ebenfalls auf die Seite gezogen werden.

                Freien Text verwenden
                • Text in „Freien Text eingeben“ schreiben und „Text auf Seite einfügen“ anklicken.
                • Der Text wird auf der aktiven Seite platziert.
                • Einen Text mit gedrückter linker Maustaste an eine andere Stelle ziehen und dort loslassen.
                • Mit der rechten Maustaste auf einen Text klicken, um die Schriftgröße zu ändern
                  oder „Text löschen“ auszuwählen.

                Kopfzeilen ausfüllen
                • In den beiden Kreisen können Buchstaben eingetragen werden.
                • In den beiden langen Feldern können zum Beispiel Marschnamen eingetragen werden.
                • Die Eingaben werden zusammen mit der Notenschrift gespeichert und gedruckt.

                Löschen
                • Markierte Elemente lassen sich über das Rechtsklick-Menü löschen.
                • Backspace und Entf löschen markierte Noten, Pausenstriche, Wiederholungszeichen oder Freitext.
                  Ohne Auswahl löschen sie die letzte Eingabe der aktiven Seite,
                  auch einen Pausenstrich an der aktuellen Position.
                  In Textfeldern löschen diese Tasten nur Text.
                • Escape hebt eine Auswahl auf, ohne Elemente zu löschen.

                Speichern, Laden und Drucken
                • „Speichern“ öffnet die Dateiauswahl und speichert Noten, Zeichen, Bögen, Texte und Kopfzeilen.
                • „Laden“ bietet „Alle Noten überschreiben“ oder „Noten hinzufügen“ an.
                • Beim Überschreiben werden beide Seiten geleert und anschließend die Datei geladen.
                • Beim Hinzufügen werden die Inhalte der Datei zu den vorhandenen Noten ergänzt.
                • „Drucken / PDF exportieren“ bietet „Leerstellen beibehalten“ und „Mit Cis/Des auffüllen“.
                • Eingefügte Leerblöcke bleiben immer frei. Die gespeicherte Notenfolge wird nicht verändert.
                • Im anschließenden Druckdialog kann ein PDF-Drucker gewählt werden. Ausgabe: A4 quer.

                Kontakt
                Erstellt und gebaut von Schlieri97.
                """.formatted(AppInfo.NAME, AppInfo.VERSION));
        helpText.setEditable(false);
        helpText.setWrapText(true);
        helpText.setPrefRowCount(36);
        helpText.setPrefColumnCount(76);
        helpText.setStyle("-fx-font-size: 14px;");

        dialog.getDialogPane().setContent(helpText);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    /**
     * Seite und Noten liegen in einer gemeinsamen, festen Seitenfläche.
     * Nur diese Fläche wird an den verfügbaren Platz angepasst; dadurch
     * behalten die Noten ihre Position relativ zur Seite.
     */
    private void setupResponsivePage() {
        pageCanvas.resize(PAGE_WIDTH, PAGE_HEIGHT);

        var pageScale = Bindings.createDoubleBinding(() -> {
            double availableWidth = printArea.getWidth();
            double availableHeight = printArea.getHeight();

            if (availableWidth <= 0 || availableHeight <= 0) {
                return 1.0;
            }

            return Math.min(availableWidth / PAGE_WIDTH, availableHeight / PAGE_HEIGHT);
        }, printArea.widthProperty(), printArea.heightProperty());

        pageContent.scaleXProperty().bind(pageScale);
        pageContent.scaleYProperty().bind(pageScale);
    }

    private void setupPageTextFields() {
        leftCircleText.setPromptText("Buchstabe");
        rightCircleText.setPromptText("Buchstabe");
        leftMarchText.setPromptText("Marschname");
        rightMarchText.setPromptText("Marschname");

        for (TextField field : List.of(leftCircleText, rightCircleText, leftMarchText, rightMarchText)) {
            // Die Kopfzeilenfelder müssen vor den vollflächigen Zeichenebenen liegen.
            field.toFront();
            field.setFocusTraversable(true);
            field.textProperty().addListener((observable, oldValue, newValue) -> recordPageTextEdit(field, oldValue));
            field.setOnMousePressed(event -> field.requestFocus());
            field.setOnAction(event -> field.getParent().requestFocus());
            applyPageHeadingSize(field, defaultPageHeadingSize(field));
            installPageHeadingContextMenu(field);
        }

        // Die leere Overlay-Fläche darf keine Klicks auf darunterliegende Felder abfangen.
        annotationOverlay.setPickOnBounds(false);
    }

    private int defaultPageHeadingSize(TextField field) {
        return field == leftCircleText || field == rightCircleText
                ? DEFAULT_CIRCLE_FONT_SIZE : DEFAULT_MARCH_FONT_SIZE;
    }

    private int pageHeadingSize(TextField field) {
        return pageHeadingSizes.getOrDefault(field, defaultPageHeadingSize(field));
    }

    private void installPageHeadingContextMenu(TextField field) {
        ContextMenu menu = new ContextMenu();
        registerContextMenuEscape(menu);
        Menu sizeMenu = new Menu("Schriftgröße");
        for (int size : List.of(12, 14, 18, 22, 24, 30, 36, 48)) {
            MenuItem option = new MenuItem();
            option.setUserData(size);
            option.setOnAction(event -> setPageHeadingSize(field, size));
            sizeMenu.getItems().add(option);
        }
        MenuItem customSize = new MenuItem("Andere Größe …");
        customSize.setOnAction(event -> {
            TextInputDialog dialog = new TextInputDialog(String.valueOf(pageHeadingSize(field)));
            dialog.setTitle("Schriftgröße ändern");
            dialog.setHeaderText("Schriftgröße für diesen Seitentitel (8–72 pt)");
            dialog.setContentText("Größe:");
            dialog.showAndWait().ifPresent(value -> {
                try {
                    setPageHeadingSize(field, Integer.parseInt(value.trim()));
                } catch (NumberFormatException ignored) {
                    keyboardStatus.setText("Bitte eine ganze Zahl zwischen 8 und 72 eingeben.");
                }
            });
        });
        sizeMenu.getItems().add(customSize);

        MenuItem cut = new MenuItem("Ausschneiden");
        cut.setOnAction(event -> field.cut());
        MenuItem copy = new MenuItem("Kopieren");
        copy.setOnAction(event -> field.copy());
        MenuItem paste = new MenuItem("Einfügen");
        paste.setOnAction(event -> field.paste());
        MenuItem selectAll = new MenuItem("Alles auswählen");
        selectAll.setOnAction(event -> field.selectAll());
        menu.getItems().addAll(sizeMenu, new SeparatorMenuItem(),
                cut, copy, paste, selectAll);
        menu.setOnShowing(event -> {
            if (activeContextMenu != null && activeContextMenu != menu) activeContextMenu.hide();
            activeContextMenu = menu;
            for (MenuItem item : sizeMenu.getItems()) {
                if (item.getUserData() instanceof Integer size) {
                    item.setText(size + " pt" + (pageHeadingSize(field) == size ? " ✓" : ""));
                }
            }
            cut.setDisable(field.getSelectedText().isEmpty());
            copy.setDisable(field.getSelectedText().isEmpty());
        });
        menu.setOnHidden(event -> {
            if (activeContextMenu == menu) activeContextMenu = null;
        });
        field.setContextMenu(menu);
    }

    private void setPageHeadingSize(TextField field, int fontSize) {
        if (fontSize < 8 || fontSize > 72) {
            keyboardStatus.setText("Bitte eine Schriftgröße zwischen 8 und 72 pt eingeben.");
            return;
        }
        if (pageHeadingSize(field) == fontSize) return;
        markDocumentDirty();
        applyPageHeadingSize(field, fontSize);
    }

    private void applyPageHeadingSize(TextField field, int fontSize) {
        pageHeadingSizes.put(field, fontSize);
        field.setStyle(field.getStyle().replaceFirst("-fx-font-size\\s*:\\s*\\d+px",
                "-fx-font-size: " + fontSize + "px"));
        double originalHeight = field == leftCircleText || field == rightCircleText ? 52 : 36;
        double originalY = field == leftCircleText || field == rightCircleText ? 38 : 43;
        double height = Math.max(originalHeight, fontSize * 1.5 + 8);
        field.setPrefHeight(height);
        field.setMaxHeight(height);
        field.setLayoutY(originalY - (height - originalHeight) / 2);
    }

    private Map<TextField, String> hidePromptTextsForPrinting() {
        Map<TextField, String> prompts = new LinkedHashMap<>();
        for (TextField field : List.of(leftCircleText, rightCircleText, leftMarchText, rightMarchText)) {
            prompts.put(field, field.getPromptText());
            field.setPromptText("");
        }
        return prompts;
    }

    private void restorePromptTexts(Map<TextField, String> prompts) {
        prompts.forEach(TextField::setPromptText);
    }

    @FXML
    private void handleAddFreeTextAction(ActionEvent event) {
        String value = freeTextInput.getText() == null ? "" : freeTextInput.getText().trim();
        if (value.isEmpty()) {
            return;
        }
        addTextAnnotation(value, defaultAnnotationPosition());
        freeTextInput.clear();
    }

    private void addTextAnnotation(String value, Point2D position) {
        addTextAnnotation(value, position, 18);
    }

    private void addTextAnnotation(String value, Point2D position, int fontSize) {
        markDocumentDirty();
        Text text = new Text(value);
        text.setFont(Font.font("Arial", FontWeight.BOLD, fontSize));
        text.setFill(Color.BLACK);
        text.setLayoutX(position.getX());
        text.setLayoutY(position.getY());
        text.setUserData("TEXT|" + encodeText(value));
        installTextDragHandler(text);
        installTextContextMenu(text);
        annotationOverlay.getChildren().add(text);
        textAnnotations.add(new PageTextAnnotation(activeGridPane, text));
    }

    private Point2D defaultAnnotationPosition() {
        Node previous = findGridSlotNode(activeGridPane, columnOffset - 1, rowOffset);
        if (previous != null) {
            Bounds bounds = previous.getBoundsInParent();
            Point2D point = activeGridPane.localToParent(bounds.getMinX(), bounds.getMinY() - 12);
            return new Point2D(point.getX(), Math.max(16, point.getY()));
        }
        double x = activeGridPane == Gridpane1 ? 80 : 710;
        return new Point2D(x, 96);
    }

    private void installTextDragHandler(Text text) {
        text.setOnMousePressed(event -> {
            if (event.getButton() != MouseButton.PRIMARY) {
                return;
            }
            draggingTextHistoryRecorded = false;
            selectTextAnnotation(text);
            Point2D point = annotationOverlay.sceneToLocal(event.getSceneX(), event.getSceneY());
            text.setUserData("TEXT|" + encodeText(text.getText()));
            text.setTranslateX(point.getX() - text.getLayoutX());
            text.setTranslateY(point.getY() - text.getLayoutY());
            event.consume();
        });
        text.setOnMouseDragged(event -> {
            if (!event.isPrimaryButtonDown()) {
                return;
            }
            if (!draggingTextHistoryRecorded) {
                markDocumentDirty();
                draggingTextHistoryRecorded = true;
            }
            Point2D point = annotationOverlay.sceneToLocal(event.getSceneX(), event.getSceneY());
            double offsetX = text.getTranslateX();
            double offsetY = text.getTranslateY();
            double x = point.getX() - offsetX;
            double y = point.getY() - offsetY;
            double width = text.getLayoutBounds().getWidth();
            text.setLayoutX(Math.max(4, Math.min(PAGE_WIDTH - width - 4, x)));
            text.setLayoutY(Math.max(16, Math.min(PAGE_HEIGHT - 4, y)));
            event.consume();
        });
        text.setOnMouseReleased(event -> {
            if (event.getButton() != MouseButton.PRIMARY) {
                return;
            }
            text.setTranslateX(0);
            text.setTranslateY(0);
            draggingTextHistoryRecorded = false;
            event.consume();
        });
        text.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                editTextAnnotation(text);
                event.consume();
            }
        });
    }

    private void editTextAnnotation(Text text) {
        TextInputDialog dialog = new TextInputDialog(text.getText());
        dialog.setTitle("Freitext bearbeiten");
        dialog.setHeaderText("Text auf der Notenseite ändern");
        dialog.setContentText("Text:");
        dialog.showAndWait().ifPresent(value -> setTextAnnotationText(text, value));
    }

    private void setTextAnnotationText(Text text, String value) {
        if (value == null || value.isBlank()) {
            keyboardStatus.setText("Der Freitext darf nicht leer sein.");
            return;
        }
        if (text.getText().equals(value)) return;
        markDocumentDirty();
        text.setText(value);
        text.setUserData("TEXT|" + encodeText(value));
        text.setLayoutX(Math.max(4, Math.min(text.getLayoutX(),
                PAGE_WIDTH - text.getLayoutBounds().getWidth() - 4)));
    }

    private void installTextContextMenu(Text text) {
        text.setOnContextMenuRequested(event -> {
            if (activeContextMenu != null) {
                activeContextMenu.hide();
            }

            ContextMenu menu = new ContextMenu();
            registerContextMenuEscape(menu);
            Menu sizeMenu = new Menu("Schriftgröße");
            for (int size : List.of(12, 14, 18, 24, 30, 36, 48)) {
                MenuItem option = new MenuItem(size + " pt" +
                        (Math.round(text.getFont().getSize()) == size ? " ✓" : ""));
                option.setOnAction(actionEvent -> setTextAnnotationSize(text, size));
                sizeMenu.getItems().add(option);
            }
            MenuItem customSize = new MenuItem("Andere Größe …");
            customSize.setOnAction(actionEvent -> {
                TextInputDialog dialog = new TextInputDialog(
                        String.valueOf(Math.round(text.getFont().getSize())));
                dialog.setTitle("Schriftgröße ändern");
                dialog.setHeaderText("Schriftgröße für diesen Text (8–72 pt)");
                dialog.setContentText("Größe:");
                dialog.showAndWait().ifPresent(value -> {
                    try {
                        setTextAnnotationSize(text, Integer.parseInt(value.trim()));
                    } catch (NumberFormatException ignored) {
                        keyboardStatus.setText("Bitte eine ganze Zahl zwischen 8 und 72 eingeben.");
                    }
                });
            });
            sizeMenu.getItems().add(customSize);
            MenuItem delete = new MenuItem("Text löschen");
            delete.setOnAction(actionEvent -> removeTextAnnotation(text));
            menu.getItems().addAll(sizeMenu, new SeparatorMenuItem(), delete);
            activeContextMenu = menu;
            menu.setOnHidden(hiddenEvent -> {
                if (activeContextMenu == menu) {
                    activeContextMenu = null;
                }
            });
            menu.show(text, event.getScreenX(), event.getScreenY());
            event.consume();
        });
    }

    private void setTextAnnotationSize(Text text, int fontSize) {
        if (fontSize < 8 || fontSize > 72) {
            keyboardStatus.setText("Bitte eine Schriftgröße zwischen 8 und 72 pt eingeben.");
            return;
        }
        if (Math.round(text.getFont().getSize()) == fontSize) return;
        markDocumentDirty();
        text.setFont(Font.font("Arial", FontWeight.BOLD, fontSize));
        text.setLayoutX(Math.max(4, Math.min(text.getLayoutX(),
                PAGE_WIDTH - text.getLayoutBounds().getWidth() - 4)));
    }

    private void removeTextAnnotation(Text text) {
        textAnnotations.removeIf(annotation -> {
            if (annotation.text == text) {
                markDocumentDirty();
                annotationOverlay.getChildren().remove(text);
                return true;
            }
            return false;
        });
        selectedTextAnnotations.remove(text);
    }

    private void removeSelectedTextAnnotations() {
        if (selectedTextAnnotations.isEmpty()) return;
        markDocumentDirty();
        textAnnotations.removeIf(annotation -> selectedTextAnnotations.contains(annotation.text));
        annotationOverlay.getChildren().removeAll(selectedTextAnnotations);
        selectedTextAnnotations.clear();
    }

    private void selectTextAnnotation(Text text) {
        clearNoteSelection();
        clearNonNoteSelection();
        addTextAnnotationToSelection(text);
    }

    private void addTextAnnotationToSelection(Text text) {
        selectedTextAnnotations.add(text);
        text.setFill(Color.web("#1976D2"));
    }

    private void selectSlur(Path path) {
        clearNoteSelection();
        clearNonNoteSelection();
        selectedSlurPath = path;
        path.setStroke(Color.web("#1976D2"));
    }

    private void selectPauseMarker(PauseMarker marker) {
        clearNoteSelection();
        clearNonNoteSelection();
        selectedPauseMarkers.add(marker);
        marker.line.setStroke(Color.web("#1976D2"));
    }

    private void clearNonNoteSelection() {
        for (Text text : selectedTextAnnotations) text.setFill(Color.BLACK);
        if (selectedSlurPath != null) {
            selectedSlurPath.setStroke(Color.BLACK);
        }
        for (PauseMarker marker : selectedPauseMarkers) {
            marker.line.setStroke(Color.BLACK);
        }
        selectedTextAnnotations.clear();
        selectedSlurPath = null;
        selectedPauseMarkers.clear();
    }

    @FXML
    private void handleAddSlurAction(ActionEvent event) {
        if (selectedNoteContainers.size() < 2 ||
                selectedNoteContainers.stream().anyMatch(this::isRepeatMarker)) {
            updateSlurSelectionLabel("Mindestens zwei Noten auswählen.");
            return;
        }

        GridPane grid = findNoteGrid(selectedNoteContainers.get(0));
        if (grid == null || selectedNoteContainers.stream().anyMatch(note -> findNoteGrid(note) != grid)) {
            updateSlurSelectionLabel("Der Bogen muss auf einer Seite liegen.");
            return;
        }

        List<NoteSlot> slots = selectedNoteContainers.stream()
                .map(note -> new NoteSlot(GridPane.getColumnIndex(note), GridPane.getRowIndex(note)))
                .filter(slot -> slot.column() >= 0 && slot.row() >= 0)
                .distinct()
                .sorted(Comparator.comparingInt(NoteSlot::row).thenComparingInt(NoteSlot::column))
                .toList();

        Path path = createSlurPath(grid, slots);
        if (path.getElements().isEmpty()) {
            updateSlurSelectionLabel("Die Auswahl enthält keine passende Notengruppe.");
            return;
        }

        markDocumentDirty();
        annotationOverlay.getChildren().add(path);
        slurAnnotations.add(new SlurAnnotation(grid, path, slots));
        updateSlurSelectionLabel("Bogen eingefügt – erneut Noten markieren für den nächsten Bogen.");
        clearNoteSelection();
    }

    private Path createSlurPath(GridPane grid, List<NoteSlot> slots) {
        Path path = new Path();
        path.setFill(null);
        path.setStroke(Color.BLACK);
        path.setStrokeWidth(2.0);
        path.setMouseTransparent(false);
        path.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                selectSlur(path);
                event.consume();
            } else if (event.getButton() == MouseButton.SECONDARY) {
                showSlurContextMenu(path, event.getScreenX(), event.getScreenY());
                event.consume();
            }
        });

        // Beim Laden können die Noten gerade erst in das Grid eingefügt
        // worden sein. Vor dem Auslesen der Bounds muss JavaFX das Grid daher
        // einmal layouten, sonst bleibt der Bogen unsichtbar.
        grid.applyCss();
        grid.layout();

        Map<Integer, List<NoteSlot>> byRow = new TreeMap<>();
        for (NoteSlot slot : slots) {
            byRow.computeIfAbsent(slot.row(), ignored -> new ArrayList<>()).add(slot);
        }

        for (List<NoteSlot> rowSlots : byRow.values()) {
            rowSlots.sort(Comparator.comparingInt(NoteSlot::column));
            Node first = findGridSlotNode(grid, rowSlots.get(0).column(), rowSlots.get(0).row());
            Node last = findGridSlotNode(grid, rowSlots.get(rowSlots.size() - 1).column(), rowSlots.get(rowSlots.size() - 1).row());
            if (first == null || last == null) {
                continue;
            }

            Bounds firstBounds = first.getBoundsInParent();
            Bounds lastBounds = last.getBoundsInParent();
            double firstCenterX = firstBounds.getMinX() + firstBounds.getWidth() / 2.0;
            double lastCenterX = lastBounds.getMinX() + lastBounds.getWidth() / 2.0;
            if (rowSlots.size() == 1) {
                // Bei einem Zeilenwechsel bleibt so auch der Anfang bzw.
                // das Ende des Bogens auf der jeweils anderen Zeile sichtbar.
                firstCenterX -= 7;
                lastCenterX += 7;
            }
            Point2D start = grid.localToParent(
                    firstCenterX,
                    firstBounds.getMinY() - 4
            );
            Point2D end = grid.localToParent(
                    lastCenterX,
                    lastBounds.getMinY() - 4
            );
            double span = Math.max(18, end.getX() - start.getX());
            double archHeight = Math.max(10, Math.min(22, span * 0.14));
            double archY = Math.min(start.getY(), end.getY()) - archHeight;

            path.getElements().add(new MoveTo(start.getX(), start.getY()));
            path.getElements().add(new CubicCurveTo(
                    start.getX() + span * 0.28, archY,
                    end.getX() - span * 0.28, archY,
                    end.getX(), end.getY()
            ));
        }
        return path;
    }

    private void showSlurContextMenu(Path path, double screenX, double screenY) {
        if (activeContextMenu != null) {
            activeContextMenu.hide();
        }

        ContextMenu menu = new ContextMenu();
        registerContextMenuEscape(menu);
        MenuItem delete = new MenuItem("Bogen löschen");
        delete.setOnAction(event -> deleteSlur(path));
        menu.getItems().add(delete);
        activeContextMenu = menu;
        menu.setOnHidden(event -> activeContextMenu = null);
        menu.show(annotationOverlay, screenX, screenY);
    }

    private void deleteSlur(Path path) {
        if (slurAnnotations.stream().anyMatch(annotation -> annotation.path == path)) {
            markDocumentDirty();
        }
        slurAnnotations.removeIf(annotation -> annotation.path == path);
        annotationOverlay.getChildren().remove(path);
        if (selectedSlurPath == path) {
            selectedSlurPath = null;
        }
    }

    private void updateSlurSelectionLabel(String message) {
        if (slurSelectionLabel != null) {
            slurSelectionLabel.setText(message);
        }
    }

    private void setupDragAndDrop() {
        List<Button> noteButtons = List.of(
                Cis_Des, D, Dis_Es, E, F, F_1, Fis_Ges, G, Gis_As,
                A, Ais_B, H, C1, Cis_Des1, D1, Dis_Es1, E1,
                Fis_Ges1, F1, G1, Gis_As1, A1
        );

        for (Button button : noteButtons) {
            installDragSource(button, "NOTE|" + button.getText() + "|NORMAL");
        }
        installDragSource(Wiederholungszeichen, "SPECIAL|REPEAT_START");
        installDragSource(pauseStrokeButton, "SPECIAL|PAUSE");

        pageCanvas.setOnDragOver(event -> {
            Dragboard dragboard = event.getDragboard();
            if (isSupportedDrag(dragboard)) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });

        pageCanvas.setOnDragDropped(event -> {
            boolean completed = dropElement(event);
            event.setDropCompleted(completed);
            event.consume();
        });
    }

    private void installDragSource(Node source, String payload) {
        source.setOnDragDetected(event -> {
            Dragboard dragboard = source.startDragAndDrop(TransferMode.COPY);
            ClipboardContent content = new ClipboardContent();
            content.putString(payload);
            dragboard.setContent(content);
            event.consume();
        });
    }

    private void setupMouseSelection() {
        pageCanvas.setFocusTraversable(true);
        selectionRectangle = new Rectangle();
        selectionRectangle.setManaged(false);
        selectionRectangle.setMouseTransparent(true);
        selectionRectangle.setFill(Color.web("#1976D2", 0.16));
        selectionRectangle.setStroke(Color.web("#1976D2", 0.85));
        selectionRectangle.setStrokeWidth(1.5);
        selectionRectangle.setVisible(false);
        pageCanvas.getChildren().add(selectionRectangle);

        // Klicks außerhalb heben die Auswahl auf. Notenbuttons behalten sie,
        // damit ihre anschließende Klickaktion die markierte Note ersetzen kann.
        pageCanvas.sceneProperty().addListener((observable, oldScene, newScene) -> {
            pressedKeys.clear();
            if (oldScene != null) {
                oldScene.removeEventFilter(MouseEvent.MOUSE_PRESSED, selectionMouseHandler);
                oldScene.removeEventFilter(KeyEvent.KEY_PRESSED, keyPressedHandler);
                oldScene.removeEventFilter(KeyEvent.KEY_RELEASED, keyReleasedHandler);
            }
            if (newScene != null) {
                newScene.addEventFilter(MouseEvent.MOUSE_PRESSED, selectionMouseHandler);
                newScene.addEventFilter(KeyEvent.KEY_PRESSED, keyPressedHandler);
                newScene.addEventFilter(KeyEvent.KEY_RELEASED, keyReleasedHandler);
                newScene.windowProperty().addListener((obs, oldWindow, newWindow) -> {
                    if (newWindow != null) {
                        newWindow.focusedProperty().addListener((o, wasFocused, focused) -> pressedKeys.clear());
                    }
                });
            }
        });

        pageCanvas.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            if (event.getButton() == MouseButton.PRIMARY && !isTextInputTarget(event.getTarget())) {
                // Nach einer Beschriftung reicht ein Klick auf die Seite zum Weiterschreiben.
                pageCanvas.requestFocus();
            }
            if (event.getButton() == MouseButton.PRIMARY
                    && !isElementSelectionTarget(event.getTarget())
                    && !isPageTextFieldTarget(event.getTarget())) {
                selectionStart = pageCanvas.sceneToLocal(event.getSceneX(), event.getSceneY());
                draggingSelection = false;
            }
        });

        pageCanvas.addEventFilter(MouseEvent.MOUSE_DRAGGED, event -> {
            if (selectionStart == null || !event.isPrimaryButtonDown()) {
                return;
            }

            Point2D point = pageCanvas.sceneToLocal(event.getSceneX(), event.getSceneY());
            double width = Math.abs(point.getX() - selectionStart.getX());
            double height = Math.abs(point.getY() - selectionStart.getY());
            if (!draggingSelection && (width > 6 || height > 6)) {
                draggingSelection = true;
                clearNoteSelection();
            }
            if (draggingSelection) {
                updateSelectionRectangle(point.getX(), point.getY());
                event.consume();
            }
        });

        pageCanvas.addEventFilter(MouseEvent.MOUSE_RELEASED, event -> {
            if (event.getButton() == MouseButton.PRIMARY && draggingSelection) {
                Point2D point = pageCanvas.sceneToLocal(event.getSceneX(), event.getSceneY());
                selectNotesInRectangle(currentSelectionBounds(point.getX(), point.getY()));
                selectionRectangle.setVisible(false);
                event.consume();
            }
            selectionStart = null;
            draggingSelection = false;
        });
    }

    private void clearSelectionWhenClickedOutsideNotes(MouseEvent event) {
        if (event.getButton() == MouseButton.PRIMARY && activeContextMenu != null) {
            activeContextMenu.hide();
        }
        if (event.getButton() == MouseButton.PRIMARY
                && !isElementSelectionTarget(event.getTarget())
                && !isNoteInputButtonTarget(event.getTarget())) {
            clearAllSelection();
        }
    }

    private void registerContextMenuEscape(ContextMenu menu) {
        menu.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                menu.hide();
                clearAllSelection();
                event.consume();
            }
        });
    }

    private boolean isNoteInputButtonTarget(Object target) {
        if (!(target instanceof Node node)) return false;
        while (node != null) {
            if (noteInputButtons.contains(node)) return true;
            node = node.getParent();
        }
        return false;
    }

    private void handleNoteKeyPressed(KeyEvent event) {
        if (event.isMetaDown()) {
            return;
        }
        KeyCode code = event.getCode();
        if (code == KeyCode.Z && event.isControlDown() && !event.isAltDown()
                && !isDescendantOf(event.getTarget(), freeTextInput)) {
            event.consume();
            if (pressedKeys.add(code)) {
                if (event.isShiftDown()) handleRedoAction(null);
                else handleUndoAction(null);
            }
            return;
        }
        if (isTextInputTarget(event.getTarget())) return;
        if (code == KeyCode.ESCAPE && (!selectedNoteContainers.isEmpty()
                || !selectedPauseMarkers.isEmpty() || !selectedTextAnnotations.isEmpty()
                || selectedSlurPath != null || activeContextMenu != null)) {
            if (activeContextMenu != null) activeContextMenu.hide();
            clearAllSelection();
            event.consume();
            return;
        }
        if (activeContextMenu != null && activeContextMenu.isShowing()) return;
        String note = NOTE_KEYS.get(code);
        boolean plainKey = !event.isControlDown() && !event.isAltDown() && !event.isShiftDown();
        boolean delete = plainKey && (code == KeyCode.DELETE || code == KeyCode.BACK_SPACE);
        boolean emptyBlock = plainKey && code == KeyCode.ENTER;
        if (note == null && !delete && !emptyBlock) {
            return;
        }
        event.consume();
        // Noten und Leerblöcke bleiben einmalig; gehaltenes Löschen nutzt die
        // Tastenwiederholung des Betriebssystems.
        if (!pressedKeys.add(code) && !delete) {
            return;
        }
        if (delete) {
            if (!selectedTextAnnotations.isEmpty()) {
                removeSelectedTextAnnotations();
            } else if (!selectedNoteContainers.isEmpty() || !selectedPauseMarkers.isEmpty()) {
                deleteSelectedNotesAndCompact();
            } else {
                handleDeleteLastNoteAction(null);
            }
        } else if (emptyBlock) {
            handleEmptyBlockAction(null);
        } else {
            if (event.isShiftDown() && (NO_OKTAV_NOTES.contains(note)
                    || (event.isControlDown() && event.isAltDown()))) {
                keyboardStatus.setText("Für " + note.replace('_', '/') + " ist diese Oktav-Variante nicht verfügbar.");
                return;
            }
            NoteModifier modifier;
            if (event.isControlDown() && event.isAltDown()) {
                modifier = NoteModifier.FESTE;
            } else if (event.isControlDown()) {
                modifier = event.isShiftDown() ? NoteModifier.OKTAV_LANG : NoteModifier.LANG;
            } else if (event.isAltDown()) {
                modifier = event.isShiftDown() ? NoteModifier.OKTAV_KURZ : NoteModifier.KURZ;
            } else {
                modifier = event.isShiftDown() ? NoteModifier.OKTAV : NoteModifier.NORMAL;
            }
            if (rowOffset >= 119) {
                keyboardStatus.setText("Diese Seite ist voll. Bitte die andere Seite wählen.");
                return;
            }
            clearAllSelection();
            addNote(note, modifier);
            keyboardStatus.setText("Eingefügt: " + note.replace('_', '/') + " · " + modifier.name().toLowerCase(Locale.GERMAN).replace('_', ' '));
        }
    }

    private boolean isDescendantOf(Object target, Node ancestor) {
        if (!(target instanceof Node node) || ancestor == null) return false;
        while (node != null) {
            if (node == ancestor) return true;
            node = node.getParent();
        }
        return false;
    }

    private boolean isTextInputTarget(Object target) {
        if (!(target instanceof Node node)) {
            return false;
        }
        while (node != null) {
            if (node instanceof TextInputControl || node instanceof ComboBoxBase<?> || node instanceof Spinner<?> || node instanceof Slider) {
                return true;
            }
            node = node.getParent();
        }
        return false;
    }

    private boolean isElementSelectionTarget(Object target) {
        if (!(target instanceof Node node)) {
            return false;
        }

        while (node != null) {
            if (node instanceof StackPane stackPane && isSelectableContainer(stackPane)) {
                return true;
            }
            if (node instanceof Text text && textAnnotations.stream()
                    .anyMatch(annotation -> annotation.text == text)) {
                return true;
            }
            if (node instanceof Path path && slurAnnotations.stream()
                    .anyMatch(annotation -> annotation.path == path)) {
                return true;
            }
            if (node instanceof Line line && pauseMarkers.stream()
                    .anyMatch(marker -> marker.line == line)) {
                return true;
            }
            node = node.getParent();
        }
        return false;
    }

    private void clearAllSelection() {
        clearNoteSelection();
        clearNonNoteSelection();
    }

    private boolean isNoteSelectionTarget(Object target) {
        if (!(target instanceof Node node)) {
            return false;
        }

        while (node != null) {
            if (node instanceof StackPane stackPane && isSelectableContainer(stackPane)) {
                return true;
            }
            node = node.getParent();
        }
        return false;
    }

    private boolean isPageTextFieldTarget(Object target) {
        if (!(target instanceof Node node)) {
            return false;
        }

        while (node != null) {
            if (node == leftCircleText || node == rightCircleText
                    || node == leftMarchText || node == rightMarchText) {
                return true;
            }
            node = node.getParent();
        }
        return false;
    }

    private void updateSelectionRectangle(double currentX, double currentY) {
        double minX = Math.min(selectionStart.getX(), currentX);
        double minY = Math.min(selectionStart.getY(), currentY);
        selectionRectangle.setX(minX);
        selectionRectangle.setY(minY);
        selectionRectangle.setWidth(Math.abs(currentX - selectionStart.getX()));
        selectionRectangle.setHeight(Math.abs(currentY - selectionStart.getY()));
        selectionRectangle.setVisible(true);
    }

    private Bounds currentSelectionBounds(double currentX, double currentY) {
        double minX = Math.min(selectionStart.getX(), currentX);
        double minY = Math.min(selectionStart.getY(), currentY);
        return new javafx.geometry.BoundingBox(
                minX,
                minY,
                Math.abs(currentX - selectionStart.getX()),
                Math.abs(currentY - selectionStart.getY())
        );
    }

    private void selectNotesInRectangle(Bounds selectionBounds) {
        List<StackPane> notes = new ArrayList<>();
        for (GridPane grid : List.of(Gridpane1, Gridpane2)) {
            for (Node node : grid.getChildren()) {
                if (!(node instanceof StackPane noteContainer) || !isSelectableContainer(noteContainer)) {
                    continue;
                }
                Bounds noteBounds = noteBoundsInPage(noteContainer, grid);
                if (selectionBounds.intersects(noteBounds)) {
                    notes.add(noteContainer);
                }
            }
        }
        setSelectedNotes(notes);
        for (PageTextAnnotation annotation : textAnnotations) {
            Bounds textBounds = pageCanvas.sceneToLocal(
                    annotation.text.localToScene(annotation.text.getBoundsInLocal()));
            if (selectionBounds.intersects(textBounds)) {
                addTextAnnotationToSelection(annotation.text);
            }
        }
        for (PauseMarker marker : pauseMarkers) {
            Bounds pauseBounds = pageCanvas.sceneToLocal(
                    marker.line.localToScene(marker.line.getBoundsInLocal()));
            if (selectionBounds.intersects(pauseBounds)) {
                addPauseMarkerToSelection(marker);
            }
        }
    }

    private void addPauseMarkerToSelection(PauseMarker marker) {
        if (!selectedPauseMarkers.contains(marker)) {
            selectedPauseMarkers.add(marker);
            marker.line.setStroke(Color.web("#1976D2"));
        }
    }

    private Bounds noteBoundsInPage(StackPane noteContainer, GridPane grid) {
        int column = GridPane.getColumnIndex(noteContainer);
        int row = GridPane.getRowIndex(noteContainer);
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;

        for (Node node : grid.getChildren()) {
            Integer nodeColumn = GridPane.getColumnIndex(node);
            Integer nodeRow = GridPane.getRowIndex(node);
            boolean belongsToSlot = nodeColumn != null && nodeRow != null &&
                    nodeColumn == column && nodeRow >= row && nodeRow <= row + 8 &&
                    (node instanceof Ellipse || node == noteContainer ||
                            (node instanceof StackPane && node.getUserData() == null));
            if (!belongsToSlot) {
                continue;
            }

            Bounds bounds = pageCanvas.sceneToLocal(node.localToScene(node.getBoundsInLocal()));
            minX = Math.min(minX, bounds.getMinX());
            minY = Math.min(minY, bounds.getMinY());
            maxX = Math.max(maxX, bounds.getMaxX());
            maxY = Math.max(maxY, bounds.getMaxY());
        }

        if (minX == Double.POSITIVE_INFINITY) {
            return pageCanvas.sceneToLocal(noteContainer.localToScene(noteContainer.getBoundsInLocal()));
        }
        return new javafx.geometry.BoundingBox(minX, minY, maxX - minX, maxY - minY);
    }

    private boolean isSelectableContainer(StackPane node) {
        return node.getUserData() instanceof String token &&
                isSelectableToken(token);
    }

    private boolean isRepeatMarker(StackPane node) {
        return node.getUserData() instanceof String token && token.startsWith("WIEDERHOLUNG|");
    }

    private boolean isSupportedDrag(Dragboard dragboard) {
        return dragboard.hasString() &&
                (dragboard.getString().startsWith("NOTE|") || dragboard.getString().startsWith("SPECIAL|"));
    }

    private boolean dropElement(DragEvent event) {
        if (!isSupportedDrag(event.getDragboard())) {
            return false;
        }

        // Die Seite wird anhand der Drop-Position gewählt.
        double pageX = pageCanvas.sceneToLocal(event.getSceneX(), event.getSceneY()).getX();
        selectPage(pageX < PAGE_WIDTH / 2.0 ? Gridpane1 : Gridpane2);

        String[] payload = event.getDragboard().getString().split("\\|", 3);
        if (payload[0].equals("NOTE") && payload.length >= 2) {
            return insertDroppedNote(activeGridPane, payload[1],
                    event.getSceneX(), event.getSceneY());
        }
        if (payload[0].equals("SPECIAL") && payload.length >= 2) {
            switch (payload[1]) {
                case "PAUSE" -> addPauseStroke();
                case "REPEAT_START" -> addWiederholungszeichen(Wiederholungsart.ANFANG);
                case "REPEAT_END" -> addWiederholungszeichen(Wiederholungsart.ENDE);
                default -> {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    private boolean insertDroppedNote(GridPane grid, String baseNote, double sceneX, double sceneY) {
        List<SerializedGridToken> tokens = collectGridTokens(grid);
        int insertionIndex = insertionIndexAt(grid, tokens, sceneX, sceneY);
        List<SerializedGridToken> inserted = new ArrayList<>(tokens);
        inserted.add(insertionIndex, new SerializedGridToken(baseNote + "|NORMAL", -1, -1, false));
        if (!canFitGridTokens(inserted)) {
            keyboardStatus.setText("Für diese Note ist auf der Seite kein Platz mehr.");
            return false;
        }

        markDocumentDirty();
        clearAllSelection();
        int noteInsertionIndex = (int) tokens.subList(0, insertionIndex).stream()
                .filter(token -> isNoteToken(token.token())).count();
        rebuildGridKeepingSlurs(grid, tokens, inserted, noteInsertionIndex);
        return true;
    }

    private void rebuildGridKeepingSlurs(GridPane grid, List<SerializedGridToken> previous,
                                         List<SerializedGridToken> replacement, int noteInsertionIndex) {
        // Die ausgewählten Noten eines Bogens werden über ihre Reihenfolge
        // auf die neuen Rasterpositionen abgebildet.
        List<SerializedGridToken> oldNotes = previous.stream()
                .filter(token -> isNoteToken(token.token())).toList();
        List<List<Integer>> slurIndices = slurAnnotations.stream()
                .filter(annotation -> annotation.grid == grid)
                .map(annotation -> annotation.slots.stream()
                        .map(slot -> {
                            for (int i = 0; i < oldNotes.size(); i++) {
                                SerializedGridToken token = oldNotes.get(i);
                                if (token.column() == slot.column() && token.row() == slot.row()) return i;
                            }
                            return -1;
                        })
                        .filter(index -> index >= 0).toList())
                .filter(indices -> !indices.isEmpty()).toList();
        rebuildGridCompact(grid, replacement);
        List<SerializedGridToken> newNotes = collectGridTokens(grid).stream()
                .filter(token -> isNoteToken(token.token())).toList();
        for (List<Integer> indices : slurIndices) {
            List<NoteSlot> slots = indices.stream()
                    .map(index -> newNotes.get(noteInsertionIndex >= 0 && index >= noteInsertionIndex
                            ? index + 1 : index))
                    .map(token -> new NoteSlot(token.column(), token.row())).toList();
            Path path = createSlurPath(grid, slots);
            if (!path.getElements().isEmpty()) {
                annotationOverlay.getChildren().add(path);
                slurAnnotations.add(new SlurAnnotation(grid, path, slots));
            }
        }
    }

    private int insertionIndexAt(GridPane grid, List<SerializedGridToken> tokens,
                                 double sceneX, double sceneY) {
        List<SerializedGridToken> slots = tokens.stream().filter(token -> !token.pause()).toList();
        if (slots.isEmpty()) return tokens.size();
        grid.applyCss();
        grid.layout();
        Point2D point = grid.sceneToLocal(sceneX, sceneY);
        Map<Integer, Double> rowCenters = new TreeMap<>();
        for (SerializedGridToken slot : slots) {
            Node node = findGridSlotNode(grid, slot.column(), slot.row());
            if (node != null) {
                Bounds bounds = node.getBoundsInParent();
                rowCenters.putIfAbsent(slot.row(), bounds.getMinY() + bounds.getHeight() / 2.0);
            }
        }
        if (rowCenters.isEmpty()) return tokens.size();
        int nearestRow = rowCenters.entrySet().stream()
                .min(Comparator.comparingDouble(entry -> Math.abs(entry.getValue() - point.getY())))
                .orElseThrow().getKey();
        double lastCenter = rowCenters.values().stream().mapToDouble(Double::doubleValue).max().orElse(0);
        if (point.getY() > lastCenter + 48) return tokens.size();

        for (SerializedGridToken slot : slots) {
            if (slot.row() != nearestRow) continue;
            Node node = findGridSlotNode(grid, slot.column(), slot.row());
            if (node == null) continue;
            Bounds bounds = node.getBoundsInParent();
            if (point.getX() < bounds.getMinX() + bounds.getWidth() / 2.0) {
                return tokens.indexOf(slot);
            }
        }
        for (int i = 0; i < tokens.size(); i++) {
            if (tokens.get(i).row() > nearestRow) return i;
        }
        return tokens.size();
    }

    private boolean canFitGridTokens(List<SerializedGridToken> tokens) {
        int row = 1;
        int column = 0;
        for (SerializedGridToken token : tokens) {
            if (token.pause()) continue;
            if (EMPTY_BLOCK.equals(token.token())) {
                row += column == 0 ? 17 : 34;
                column = 0;
                if (row > 120) return false;
            } else {
                if (row >= 119) return false;
                if (column == 40) {
                    column = 0;
                    row += 17;
                } else {
                    column++;
                }
            }
        }
        return true;
    }

    @FXML
    private void handlePauseStrokeAction(ActionEvent event) {
        addPauseStroke();
    }

    @FXML
    private void handleRepeatStartAction(ActionEvent event) {
        addWiederholungszeichen(Wiederholungsart.ANFANG);
    }

    @FXML
    private void handleRepeatEndAction(ActionEvent event) {
        addWiederholungszeichen(Wiederholungsart.ENDE);
    }

    private void addPauseStroke() {
        if (rowOffset >= 119) {
            return;
        }

        markDocumentDirty();
        // Der Pausenstrich bekommt bewusst keinen eigenen Grid-Slot. Er wird
        // als freies Overlay genau in die Lücke zwischen zwei Noten gelegt.
        PauseMarker marker = createPauseMarker(activeGridPane, columnOffset, rowOffset);
        positionPauseMarker(marker);
    }

    private PauseMarker createPauseMarker(GridPane grid, int column, int row) {
        Line pauseLine = new Line(0, 0, 0, 16);
        pauseLine.setStroke(Color.BLACK);
        pauseLine.setStrokeWidth(2.0);
        pauseLine.setManaged(false);
        pauseLine.setPickOnBounds(true);
        PauseMarker marker = new PauseMarker(grid, pauseLine, column, row);
        installPauseHandlers(marker);
        pauseMarkers.add(marker);
        pauseOverlay.getChildren().add(pauseLine);
        return marker;
    }

    private void installPauseHandlers(PauseMarker marker) {
        Line pauseLine = marker.line;
        pauseLine.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                selectPauseMarker(marker);
                event.consume();
            } else if (event.getButton() == MouseButton.SECONDARY) {
                showPauseContextMenu(marker, event.getScreenX(), event.getScreenY());
                event.consume();
            }
        });
        pauseLine.setOnMouseDragged(event -> {
            if (!event.isPrimaryButtonDown()) return;
            if (!marker.dragging) {
                markDocumentDirty();
                marker.dragging = true;
            }
            Point2D position = pauseOverlay.sceneToLocal(event.getSceneX(), event.getSceneY());
            pauseLine.setLayoutX(Math.max(0, Math.min(PAGE_WIDTH, position.getX())));
            pauseLine.setLayoutY(Math.max(0, Math.min(PAGE_HEIGHT - 16, position.getY())));
            event.consume();
        });
        pauseLine.setOnMouseReleased(event -> {
            if (event.getButton() != MouseButton.PRIMARY || !marker.dragging) return;
            if (marker.free) {
                marker.grid = pauseLine.getLayoutX() < PAGE_WIDTH / 2 ? Gridpane1 : Gridpane2;
                documentDirty = true;
            } else if (!snapPauseMarkerToNearestNote(marker)) {
                positionPauseMarker(marker);
            }
            marker.dragging = false;
            event.consume();
        });
    }

    private boolean snapPauseMarkerToNearestNote(PauseMarker marker) {
        double bestDistance = Double.POSITIVE_INFINITY;
        GridPane bestGrid = null;
        int bestColumn = -1, bestRow = -1;
        for (GridPane grid : List.of(Gridpane1, Gridpane2)) {
            for (Node node : grid.getChildren()) {
                if (!(node instanceof StackPane pane) || !isSelectableContainer(pane) || isRepeatMarker(pane)) continue;
                Integer column = GridPane.getColumnIndex(node), row = GridPane.getRowIndex(node);
                if (column == null || row == null) continue;
                Bounds bounds = pauseOverlay.sceneToLocal(node.localToScene(node.getBoundsInLocal()));
                double x = bounds.getMinX() - grid.getHgap() / 2.0;
                double y = bounds.getMinY() - 6;
                double distance = Math.hypot(marker.line.getLayoutX() - x, marker.line.getLayoutY() - y);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    bestGrid = grid;
                    bestColumn = column;
                    bestRow = row;
                }
            }
        }
        if (bestGrid == null || bestDistance > 35) return false;
        if (marker.dragging) documentDirty = true;
        else markDocumentDirty();
        marker.grid = bestGrid;
        marker.column = bestColumn;
        marker.row = bestRow;
        positionPauseMarker(marker);
        return true;
    }

    private void positionPauseMarker(PauseMarker marker) {
        if (marker.free) return;
        GridPane grid = marker.grid;
        grid.applyCss();
        grid.layout();

        Node previous = findGridSlotNode(grid, marker.column - 1, marker.row);
        Node next = findGridSlotNode(grid, marker.column, marker.row);

        double localX;
        if (previous != null) {
            Bounds bounds = previous.getBoundsInParent();
            localX = bounds.getMaxX() + grid.getHgap() / 2.0;
        } else if (next != null) {
            Bounds bounds = next.getBoundsInParent();
            localX = bounds.getMinX() - grid.getHgap() / 2.0;
        } else {
            localX = marker.column * (NOTE_SLOT_WIDTH + NOTE_GAP) - grid.getHgap() / 2.0;
        }

        double localY = 0;
        Node reference = previous != null ? previous : next;
        if (reference != null) {
            // Etwas oberhalb des ersten Kreises wie in der Vorlage.
            localY = reference.getBoundsInParent().getMinY() - 6;
        }

        Point2D parentPoint = grid.localToParent(localX, localY);
        marker.line.setLayoutX(parentPoint.getX());
        marker.line.setLayoutY(parentPoint.getY());
    }

    private Node findGridSlotNode(GridPane grid, int column, int row) {
        if (column < 0) {
            return null;
        }
        return grid.getChildren().stream()
                .filter(node -> Objects.equals(GridPane.getColumnIndex(node), column))
                .filter(node -> Objects.equals(GridPane.getRowIndex(node), row))
                .findFirst()
                .orElse(null);
    }

    private void removePauseMarker(GridPane grid, int column, int row) {
        if (pauseMarkers.stream().anyMatch(marker -> marker.grid == grid
                && marker.column == column && marker.row == row)) {
            markDocumentDirty();
        }
        pauseMarkers.removeIf(marker -> {
            boolean matches = marker.grid == grid && marker.column == column && marker.row == row;
            if (matches) {
                pauseOverlay.getChildren().remove(marker.line);
            }
            return matches;
        });
    }

    private void showPauseContextMenu(PauseMarker marker, double screenX, double screenY) {
        if (activeContextMenu != null) {
            activeContextMenu.hide();
        }

        ContextMenu menu = new ContextMenu();
        registerContextMenuEscape(menu);
        MenuItem placement = new MenuItem(marker.free ? "Pausenstrich an Note einrasten" : "Pausenstrich frei platzieren");
        placement.setOnAction(event -> {
            if (marker.free) {
                markDocumentDirty();
                marker.free = false;
                boolean wasHistorySuspended = historySuspended;
                historySuspended = true;
                try {
                    if (!snapPauseMarkerToNearestNote(marker)) marker.free = true;
                } finally {
                    historySuspended = wasHistorySuspended;
                }
            } else {
                markDocumentDirty();
                marker.free = true;
            }
        });
        MenuItem delete = new MenuItem("Pausenstrich löschen");
        delete.setOnAction(event -> removePauseMarker(marker));
        menu.getItems().addAll(placement, delete);
        activeContextMenu = menu;
        menu.setOnHidden(event -> {
            if (activeContextMenu == menu) {
                activeContextMenu = null;
            }
        });
        menu.show(pauseOverlay, screenX, screenY);
    }

    private void removePauseMarker(PauseMarker marker) {
        if (pauseMarkers.contains(marker)) markDocumentDirty();
        if (pauseMarkers.remove(marker)) {
            pauseOverlay.getChildren().remove(marker.line);
            selectedPauseMarkers.remove(marker);
        }
    }

    private void removePauseMarkersForGrid(GridPane grid) {
        pauseMarkers.removeIf(marker -> {
            if (marker.grid == grid) {
                pauseOverlay.getChildren().remove(marker.line);
                selectedPauseMarkers.remove(marker);
                return true;
            }
            return false;
        });
    }

    private void removeAnnotationsForGrid(GridPane grid) {
        textAnnotations.removeIf(annotation -> {
            if (annotation.grid == grid) {
                annotationOverlay.getChildren().remove(annotation.text);
                return true;
            }
            return false;
        });
        slurAnnotations.removeIf(annotation -> {
            if (annotation.grid == grid) {
                annotationOverlay.getChildren().remove(annotation.path);
                if (selectedSlurPath == annotation.path) {
                    selectedSlurPath = null;
                }
                return true;
            }
            return false;
        });
    }

    private enum Wiederholungsart {
        EINZELSTRICH,
        DOPPELSTRICH,
        GESTRICHELT,
        ANFANG,
        ENDE
    }

    private void addHorizontalRepeatLine(int startColumn, int startRow) {

        if (startColumn < 0) {
            return;
        }

        StackPane container = new StackPane();

        container.setAlignment(Pos.TOP_LEFT);
        container.setMinWidth(0);
        container.setMaxWidth(Double.MAX_VALUE);

        Line line = new Line();

        line.setStroke(Color.BLACK);
        line.setStrokeWidth(2.0);

        line.setStartX(0);
        line.setEndX(100); // wird nach dem Layout angepasst
        line.setStartY(0);
        line.setEndY(0);

        container.getChildren().add(line);

        activeGridPane.add(container, startColumn, startRow);

        // Über zwei Spalten spannen
        GridPane.setColumnSpan(container, 2);
        GridPane.setRowSpan(container, 1);

        GridPane.setHgrow(container, Priority.ALWAYS);

        // Die Linie sitzt oberhalb der Noten
        container.setTranslateY(-8);

        // Nach dem Layout die tatsächliche Breite verwenden
        container.layoutBoundsProperty().addListener((obs, oldBounds, newBounds) -> {
            line.setEndX(newBounds.getWidth());
        });
    }

    @FXML
    private void handleWiederholungszeichenAction(ActionEvent event) {

        ContextMenu menu = new ContextMenu();
        registerContextMenuEscape(menu);

        MenuItem einzelstrich = new MenuItem("Normaler Strich |");
        MenuItem doppelstrich = new MenuItem("Doppelstrich ||");
        MenuItem gestrichelt = new MenuItem("Gestrichelter Strich ⋮");
        MenuItem anfang = new MenuItem("Wiederholungsanfang :||");
        MenuItem ende = new MenuItem("Wiederholungsende ||:");

        einzelstrich.setOnAction(e -> addOrReplaceRepeatMarker(Wiederholungsart.EINZELSTRICH));
        doppelstrich.setOnAction(e -> addOrReplaceRepeatMarker(Wiederholungsart.DOPPELSTRICH));
        gestrichelt.setOnAction(e -> addOrReplaceRepeatMarker(Wiederholungsart.GESTRICHELT));
        anfang.setOnAction(e -> addOrReplaceRepeatMarker(Wiederholungsart.ANFANG));
        ende.setOnAction(e -> addOrReplaceRepeatMarker(Wiederholungsart.ENDE));

        menu.getItems().addAll(
                einzelstrich,
                doppelstrich,
                gestrichelt,
                new SeparatorMenuItem(),
                anfang,
                ende
        );

        menu.show(Wiederholungszeichen, Wiederholungszeichen.getScene().getWindow().getX()
                        + Wiederholungszeichen.localToScene(Wiederholungszeichen.getBoundsInLocal()).getMinX(),
                Wiederholungszeichen.getScene().getWindow().getY()
                        + Wiederholungszeichen.localToScene(Wiederholungszeichen.getBoundsInLocal()).getMaxY());
    }

    private void addOrReplaceRepeatMarker(Wiederholungsart art) {
        if (selectedNoteContainers.isEmpty()) {
            addWiederholungszeichen(art);
            return;
        }
        markDocumentDirty();

        Set<StackPane> selected = Set.copyOf(selectedNoteContainers);
        Set<GridPane> affectedGrids = selected.stream()
                .map(this::findNoteGrid)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        String replacementToken = "WIEDERHOLUNG|" + art.name();

        for (GridPane grid : affectedGrids) {
            Set<NoteSlot> selectedSlots = selected.stream()
                    .filter(element -> findNoteGrid(element) == grid)
                    .map(element -> new NoteSlot(GridPane.getColumnIndex(element), GridPane.getRowIndex(element)))
                    .collect(java.util.stream.Collectors.toSet());
            List<SerializedGridToken> replacedTokens = collectGridTokens(grid).stream()
                    .map(token -> isSelectableToken(token.token()) &&
                            selectedSlots.contains(new NoteSlot(token.column(), token.row()))
                            ? new SerializedGridToken(replacementToken, token.column(), token.row(), false)
                            : token)
                    .toList();
            rebuildGridCompact(grid, replacedTokens);
        }
        clearNoteSelection();
    }

    private void addWiederholungszeichen(Wiederholungsart art) {

        if (rowOffset >= 119) {
            return;
        }

        markDocumentDirty();
        StackPane container = createRepeatMarker(art);
        container.setUserData("WIEDERHOLUNG|" + art.name());
        installNoteSelectionHandler(container, container, activeGridPane);

        activeGridPane.add(container, columnOffset, rowOffset);
        GridPane.setRowSpan(container, 9);

        advanceGridPosition();

        activeGridPane.setHgap(5.5);
    }

    /**
     * Creates one self-contained repeat marker. The open Cis/Des fingering,
     * the bar and the small directional cap stay in the same grid slot;
     * otherwise the cap becomes a second grid child and moves neighbouring
     * fingerings.
     */
    private StackPane createRepeatMarker(Wiederholungsart art) {
        StackPane marker = new StackPane();
        // Exakt die Breite einer normalen Note verwenden. So entsteht nach
        // einem Wiederholungszeichen keine zusätzliche Spalte bzw. Lücke.
        marker.setMinWidth(NOTE_SLOT_WIDTH);
        marker.setPrefWidth(NOTE_SLOT_WIDTH);
        marker.setMaxWidth(NOTE_SLOT_WIDTH);
        marker.setMinHeight(87);
        marker.setPrefHeight(87);
        marker.setMaxHeight(87);
        marker.setAlignment(Pos.TOP_CENTER);

        marker.getChildren().add(createCisDesFingering());

        if (art == Wiederholungsart.GESTRICHELT) {
            addDashedLine(marker);
        } else if (art == Wiederholungsart.DOPPELSTRICH) {
            addVerticalLine(marker, -3);
            addVerticalLine(marker, 3);
        } else if (art == Wiederholungsart.ANFANG || art == Wiederholungsart.ENDE) {
            // Der Wiederholungsstrich reicht über den ersten Kreis hinaus
            // nach oben und trifft dort direkt auf den Querstrich.
            addRepeatVerticalLine(marker);
        } else {
            addVerticalLine(marker, 0);
        }

        if (art == Wiederholungsart.ANFANG || art == Wiederholungsart.ENDE) {
            addRepeatCap(marker, art == Wiederholungsart.ANFANG);
        }
        return marker;
    }

    private Pane createCisDesFingering() {
        // Feste Zeichenfläche statt einer verschachtelten GridPane: Die
        // Cystest-Löcher können damit keine Parent-Zeilen verschieben.
        Pane fingering = new Pane();
        fingering.setMinSize(NOTE_SLOT_WIDTH, 87);
        fingering.setPrefSize(NOTE_SLOT_WIDTH, 87);
        fingering.setMaxSize(NOTE_SLOT_WIDTH, 87);
        fingering.setMouseTransparent(true);

        boolean[] cisDesGriff = model.getGriff("Cis_Des");
        double[] holeY = {4.5, 16.5, 28.5, 46.5, 58.5, 70.5, 82.5};
        for (int i = 0; i < cisDesGriff.length; i++) {
            Ellipse ellipse = createGriffEllipse(cisDesGriff[i], "Cis_Des|NORMAL");
            ellipse.setLayoutX(NOTE_SLOT_WIDTH / 2.0);
            ellipse.setLayoutY(holeY[i]);
            fingering.getChildren().add(ellipse);
        }
        return fingering;
    }

    private Ellipse createGriffEllipse(boolean closed, String userData) {
        Ellipse ellipse = new Ellipse(4.2, 4.5);
        if (closed) {
            ellipse.setFill(Color.BLACK);
        } else {
            ellipse.setFill(Color.WHITE);
            ellipse.setStroke(Color.BLACK);
            ellipse.setStrokeWidth(1.4);
            ellipse.setRadiusX(ellipse.getRadiusX() - ellipse.getStrokeWidth() / 2);
            ellipse.setRadiusY(ellipse.getRadiusY() - ellipse.getStrokeWidth() / 2);
        }
        ellipse.setUserData(userData);
        return ellipse;
    }

    private void addRepeatCap(StackPane marker, boolean pointsRight) {
        // The fixed-size overlay keeps the overhanging cap out of GridPane's
        // column measurement, so neighbouring notes do not move.
        Pane overlay = new Pane();
        overlay.setMinSize(NOTE_SLOT_WIDTH, 0);
        overlay.setPrefSize(NOTE_SLOT_WIDTH, 0);
        overlay.setMaxSize(NOTE_SLOT_WIDTH, 0);
        overlay.setMouseTransparent(true);

        Line cap = pointsRight
                ? new Line(NOTE_SLOT_WIDTH / 2.0, 0, NOTE_SLOT_WIDTH / 2.0 + 20, 0)
                : new Line(NOTE_SLOT_WIDTH / 2.0 - 20, 0, NOTE_SLOT_WIDTH / 2.0, 0);
        cap.setStroke(Color.BLACK);
        cap.setStrokeWidth(2.0);
        overlay.getChildren().add(cap);
        StackPane.setAlignment(overlay, Pos.TOP_LEFT);
        overlay.setTranslateY(-8);
        marker.getChildren().add(overlay);
    }

    private void addVerticalLine(StackPane container, double offsetX) {

        Line line = new Line(0, 2, 0, 85);

        line.setStroke(Color.BLACK);
        line.setStrokeWidth(2.0);

        StackPane.setAlignment(line, Pos.TOP_CENTER);
        StackPane.setMargin(line, new Insets(0, 0, 0, offsetX));

        container.getChildren().add(line);
    }

    private void addRepeatVerticalLine(StackPane container) {
        // Die Translation sorgt dafür, dass der Strich trotz StackPane-
        // Layout wirklich oberhalb des ersten Kreises beginnt.
        Line line = new Line(0, 0, 0, 95);
        line.setStroke(Color.BLACK);
        line.setStrokeWidth(2.0);
        StackPane.setAlignment(line, Pos.TOP_CENTER);
        line.setTranslateY(-10);
        container.getChildren().add(line);
    }

    private void addDashedLine(StackPane container) {

        Line line = new Line(0, 5, 0, 82);

        line.setStroke(Color.BLACK);
        line.setStrokeWidth(3.0);
        line.getStrokeDashArray().addAll(7.0, 5.0);

        StackPane.setAlignment(line, Pos.TOP_CENTER);

        container.getChildren().add(line);
    }

    private void advanceGridPosition() {

        if (columnOffset >= 40) {
            columnOffset = 0;
            rowOffset += 17;
        } else {
            columnOffset++;
        }
        syncActivePageOffset();
    }

    private void syncActivePageOffset() {
        if (activeGridPane == Gridpane1) {
            rowOffset1 = rowOffset;
            columnOffset1 = columnOffset;
        } else if (activeGridPane == Gridpane2) {
            rowOffset2 = rowOffset;
            columnOffset2 = columnOffset;
        }
    }

    @FXML
    private void handleEmptyBlockAction(ActionEvent event) {
        // Der leere Block selbst muss vollständig auf die Seite passen.
        // Ein weiterer Marsch danach ist in der letzten Zeile nicht erforderlich.
        int nextRow = rowOffset + (columnOffset == 0 ? 17 : 34);
        if (nextRow > 120) {
            keyboardStatus.setText("Für einen vollständigen leeren Notenblock reicht der Platz nicht. Bitte die andere Seite wählen.");
            return;
        }
        clearAllSelection();
        addEmptyBlock();
        keyboardStatus.setText(nextRow == 120
                ? "Die letzte Zeile bleibt als Notenblock leer. Für weitere Noten bitte die andere Seite wählen."
                : "Ein Notenblock bleibt leer. Der nächste Marsch beginnt im folgenden Block.");
    }

    private void addEmptyBlock() {
        int nextRow = rowOffset + (columnOffset == 0 ? 17 : 34);
        if (rowOffset >= 119 || nextRow > 120) {
            return;
        }
        markDocumentDirty();
        StackPane owner = null;
        // Unsichtbare Slots bewahren Breite und Höhe auch ohne Druck-Auffüllung.
        // Nur der erste Slot trägt den Dateitoken; der ganze Abstand ist eine Eingabe.
        while (rowOffset < nextRow) {
            StackPane slot = new StackPane();
            slot.setMinSize(NOTE_SLOT_WIDTH, 87);
            slot.setPrefSize(NOTE_SLOT_WIDTH, 87);
            slot.setMaxSize(NOTE_SLOT_WIDTH, 87);
            slot.setMouseTransparent(true);
            if (owner == null) {
                owner = slot;
                owner.setUserData(EMPTY_BLOCK);
            }
            slot.getProperties().put(EMPTY_BLOCK_OWNER, owner);
            activeGridPane.add(slot, columnOffset, rowOffset, 1, 9);
            advanceGridPosition();
        }
        activeGridPane.setHgap(NOTE_GAP);
        refreshEmptyBlockGuides();
    }

    private void refreshEmptyBlockGuides() {
        emptyBlockOverlay.getChildren().clear();
        for (GridPane grid : List.of(Gridpane1, Gridpane2)) {
            for (Node node : grid.getChildren()) {
                if (!(node instanceof StackPane owner) || !EMPTY_BLOCK.equals(owner.getUserData())) {
                    continue;
                }
                int ownerColumn = GridPane.getColumnIndex(owner);
                int blankRow = GridPane.getRowIndex(owner) + (ownerColumn == 0 ? 0 : 17);
                Node rowStart = grid.getChildren().stream()
                        .filter(slot -> slot.getProperties().get(EMPTY_BLOCK_OWNER) == owner
                                && Integer.valueOf(0).equals(GridPane.getColumnIndex(slot))
                                && Integer.valueOf(blankRow).equals(GridPane.getRowIndex(slot)))
                        .findFirst().orElse(null);
                if (rowStart == null) {
                    continue;
                }

                Label guide = new Label(EMPTY_BLOCK_GUIDE);
                guide.setAlignment(Pos.CENTER);
                guide.setPrefSize(EMPTY_BLOCK_GUIDE_WIDTH, 30);
                guide.setMouseTransparent(true);
                guide.setManaged(false);
                guide.resize(EMPTY_BLOCK_GUIDE_WIDTH, 30);
                guide.setStyle("-fx-background-color: #E9F3FF; -fx-border-color: #4677A8; "
                        + "-fx-border-style: dashed; -fx-border-radius: 5px; -fx-background-radius: 5px; "
                        + "-fx-text-fill: #173B61; -fx-font-size: 14px; -fx-font-weight: bold;");
                guide.layoutXProperty().bind(grid.layoutXProperty());
                guide.layoutYProperty().bind(Bindings.createDoubleBinding(
                        () -> grid.getLayoutY() + rowStart.getBoundsInParent().getMinY() + 28,
                        grid.layoutYProperty(), rowStart.boundsInParentProperty()));
                emptyBlockOverlay.getChildren().add(guide);
            }
        }
    }

    private void setupAllNoteButtons() {
        List<Button> noteButtons = List.of(
                Cis_Des, D, Dis_Es, E, F, F_1, Fis_Ges, G, Gis_As,
                A, Ais_B, H, C1, Cis_Des1, D1, Dis_Es1, E1,
                Fis_Ges1, F1, G1, Gis_As1, A1
        );

        for (Button btn : noteButtons) {
            setupNoteButton(btn);
        }
    }
    private void setupNoteButton(Button button) {
        noteInputButtons.add(button);
        button.setOnMouseClicked(event -> {

            String noteName = button.getText();

            if (event.getButton() == MouseButton.PRIMARY) {
                addOrReplaceNote(noteName, NoteModifier.NORMAL);
            }

            else if (event.getButton() == MouseButton.SECONDARY) {
                showNoteContextMenu(button, noteName,
                        event.getScreenX(), event.getScreenY());
            }
        });
    }

    private void showNoteContextMenu(Button source, String baseNote, double x, double y) {
        if (activeContextMenu != null) {
            activeContextMenu.hide();
        }

        ContextMenu menu = new ContextMenu();
        registerContextMenuEscape(menu);
        activeContextMenu = menu;

        boolean allowOktav = !NO_OKTAV_NOTES.contains(baseNote);

        MenuItem lang = new MenuItem("Lang (-)");
        MenuItem kurz = new MenuItem("Kurz (•)");
        MenuItem feste = new MenuItem("Feste (f)");

        lang.setOnAction(e -> addOrReplaceNote(baseNote, NoteModifier.LANG));
        kurz.setOnAction(e -> addOrReplaceNote(baseNote, NoteModifier.KURZ));
        feste.setOnAction(e -> addOrReplaceNote(baseNote, NoteModifier.FESTE));
        menu.getItems().addAll(lang, kurz, feste);
        if (allowOktav) {
            MenuItem oktav = new MenuItem("Oktav (x)");
            MenuItem oktavLang = new MenuItem("Oktav + Lang (x + -)");
            MenuItem oktavKurz = new MenuItem("Oktav + Kurz (x + •)");

            oktavLang.setOnAction(e -> addOrReplaceNote(baseNote, NoteModifier.OKTAV_LANG));
            oktavKurz.setOnAction(e -> addOrReplaceNote(baseNote, NoteModifier.OKTAV_KURZ));

            oktav.setOnAction(e -> addOrReplaceNote(baseNote, NoteModifier.OKTAV));


            menu.getItems().addAll(
                    oktav,
                    new SeparatorMenuItem(),
                    oktavLang,
                    oktavKurz
            );
        }

        menu.setOnHidden(e -> activeContextMenu = null);

        menu.show(source, x, y);
    }

    /**
     * Fügt eine neue Note ein oder ersetzt die zuvor auf der Seite
     * ausgewählte Note. Die Auswahl wird nach dem Ersetzen aufgehoben.
     */
    private void addOrReplaceNote(String baseNote, NoteModifier modifier) {
        if (selectedNoteContainer == null) {
            addNote(baseNote, modifier);
            return;
        }
        replaceSelectedNote(baseNote, modifier);
    }

    private void selectNote(StackPane noteContainer, GridPane grid) {
        if (selectedNoteContainer == noteContainer) {
            clearNoteSelection();
            return;
        }

        setSelectedNotes(List.of(noteContainer));
        selectedNoteGrid = grid;
    }

    private void setSelectedNotes(Collection<StackPane> notes) {
        clearNoteSelection();
        clearNonNoteSelection();
        selectedNoteContainers.addAll(notes);
        if (selectedNoteContainers.isEmpty()) {
            return;
        }
        selectedNoteContainer = selectedNoteContainers.get(0);
        selectedNoteContainer.setEffect(new DropShadow(8, Color.web("#1976D2")));
        for (StackPane note : selectedNoteContainers) {
            if (note != selectedNoteContainer) {
                note.setEffect(new DropShadow(8, Color.web("#1976D2")));
            }
        }
        if (selectedNoteContainers.size() >= 2) {
            updateSlurSelectionLabel(selectedNoteContainers.size() +
                    " Noten markiert – jetzt Bogen einfügen.");
        }
    }

    private void installNoteSelectionHandler(Node node, StackPane noteContainer, GridPane grid) {
        node.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                selectNote(noteContainer, grid);
                event.consume();
            } else if (event.getButton() == MouseButton.SECONDARY) {
                if (!selectedNoteContainers.contains(noteContainer)) {
                    setSelectedNotes(List.of(noteContainer));
                }
                showSelectionContextMenu(event.getScreenX(), event.getScreenY());
                event.consume();
            }
        });
    }

    private void showSelectionContextMenu(double screenX, double screenY) {
        if (activeContextMenu != null) {
            activeContextMenu.hide();
        }

        ContextMenu menu = new ContextMenu();
        registerContextMenuEscape(menu);
        MenuItem clear = new MenuItem("Auswahl aufheben");
        MenuItem delete = new MenuItem("Ausgewählte Elemente löschen");
        MenuItem slur = new MenuItem("Bogen über Auswahl");

        clear.setOnAction(event -> clearNoteSelection());
        delete.setOnAction(event -> deleteSelectedNotesAndCompact());
        slur.setDisable(selectedNoteContainers.size() < 2 ||
                selectedNoteContainers.stream().anyMatch(this::isRepeatMarker));
        slur.setOnAction(this::handleAddSlurAction);

        menu.getItems().addAll(clear, delete, new SeparatorMenuItem(), slur);
        activeContextMenu = menu;
        menu.setOnHidden(event -> activeContextMenu = null);
        menu.show(pageCanvas, screenX, screenY);
    }

    @FXML
    private void handleClearNoteSelectionAction(ActionEvent event) {
        clearNoteSelection();
    }

    @FXML
    private void handleDeleteSelectedNotesAction(ActionEvent event) {
        deleteSelectedNotesAndCompact();
    }

    private void deleteSelectedNotesAndCompact() {
        if (selectedNoteContainers.isEmpty() && selectedPauseMarkers.isEmpty()) {
            return;
        }

        markDocumentDirty();

        Set<StackPane> selected = Set.copyOf(selectedNoteContainers);
        Set<PauseMarker> selectedPauses = Set.copyOf(selectedPauseMarkers);
        for (PauseMarker marker : selectedPauses) {
            removePauseMarker(marker);
        }
        if (selected.isEmpty()) {
            clearAllSelection();
            return;
        }
        Set<GridPane> affectedGrids = selected.stream()
                .map(this::findNoteGrid)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

        for (GridPane grid : affectedGrids) {
            Set<NoteSlot> selectedSlots = selected.stream()
                    .filter(note -> findNoteGrid(note) == grid)
                    .map(note -> new NoteSlot(GridPane.getColumnIndex(note), GridPane.getRowIndex(note)))
                    .collect(java.util.stream.Collectors.toSet());
            List<SerializedGridToken> remainingTokens = collectGridTokens(grid).stream()
                    .filter(token -> token.pause() || !isSelectableToken(token.token()) ||
                            !selectedSlots.contains(new NoteSlot(token.column(), token.row())))
                    .toList();
            rebuildGridCompact(grid, remainingTokens);
        }
        clearAllSelection();
    }

    private boolean isNoteToken(String token) {
        return token != null && token.contains("|") && !token.startsWith("WIEDERHOLUNG|");
    }

    private boolean isSelectableToken(String token) {
        return isNoteToken(token) || (token != null && token.startsWith("WIEDERHOLUNG|"));
    }

    private List<SerializedGridToken> collectGridTokens(GridPane grid) {
        List<SerializedGridToken> tokens = new ArrayList<>();
        for (Node node : grid.getChildren()) {
            if (!(node instanceof StackPane)) {
                continue;
            }
            String token = tokenForGridNode(node);
            Integer column = GridPane.getColumnIndex(node);
            Integer row = GridPane.getRowIndex(node);
            if (token != null && column != null && row != null) {
                tokens.add(new SerializedGridToken(token, column, row, false));
            }
        }
        for (PauseMarker marker : pauseMarkers) {
            if (marker.grid == grid && !marker.free) {
                tokens.add(new SerializedGridToken("PAUSENSTRICH", marker.column, marker.row, true));
            }
        }
        tokens.sort(Comparator.comparingInt(SerializedGridToken::row)
                .thenComparingInt(SerializedGridToken::column)
                .thenComparingInt(token -> token.pause() ? 0 : 1));
        return tokens;
    }

    private void rebuildGridCompact(GridPane grid, List<SerializedGridToken> tokens) {
        boolean wasHistorySuspended = historySuspended;
        historySuspended = true;
        try {
            GridPane oldActiveGrid = activeGridPane;
            int oldRowOffset = rowOffset;
            int oldColumnOffset = columnOffset;

            List<PauseMarker> freeMarkers = pauseMarkers.stream()
                    .filter(marker -> marker.grid == grid && marker.free).toList();
            removePauseMarkersForGrid(grid);
            removeSlursForGrid(grid);
            grid.getChildren().clear();

            activeGridPane = grid;
            rowOffset = 1;
            columnOffset = 0;
            syncActivePageOffset();
            for (SerializedGridToken token : tokens) {
                appendSerializedToken(token.token());
            }
            for (PauseMarker marker : freeMarkers) {
                pauseMarkers.add(marker);
                pauseOverlay.getChildren().add(marker.line);
            }

            if (oldActiveGrid != grid) {
                activeGridPane = oldActiveGrid;
                rowOffset = oldRowOffset;
                columnOffset = oldColumnOffset;
                if (oldActiveGrid != null) {
                    syncActivePageOffset();
                }
            }
            refreshEmptyBlockGuides();
        } finally {
            historySuspended = wasHistorySuspended;
        }
    }

    private void appendSerializedToken(String token) {
        if (restoreSpecialToken(token)) {
            syncActivePageOffset();
            return;
        }

        String[] parts = token.split("\\|", 2);
        String baseNote = parts[0];
        NoteModifier modifier = NoteModifier.NORMAL;
        if (parts.length == 2) {
            try {
                modifier = NoteModifier.valueOf(parts[1]);
            } catch (IllegalArgumentException ignored) {
                // Alte oder unbekannte Dateien werden als normale Note geladen.
            }
        }
        addNote(baseNote, modifier);
    }

    private void removeSlursForGrid(GridPane grid) {
        slurAnnotations.removeIf(annotation -> {
            if (annotation.grid == grid) {
                annotationOverlay.getChildren().remove(annotation.path);
                return true;
            }
            return false;
        });
    }

    private void clearNoteSelection() {
        for (StackPane note : selectedNoteContainers) {
            note.setEffect(null);
        }
        selectedNoteContainers.clear();
        selectedNoteContainer = null;
        selectedNoteGrid = null;
    }

    private void replaceSelectedNote(String baseNote, NoteModifier modifier) {
        List<SelectedNoteTarget> targets = selectedNoteContainers.stream()
                .map(note -> new SelectedNoteTarget(
                        note,
                        findNoteGrid(note),
                        GridPane.getColumnIndex(note),
                        GridPane.getRowIndex(note)))
                .filter(target -> target.grid() != null && target.column() != null &&
                        target.row() != null && target.row() < 119)
                .toList();
        if (targets.isEmpty()) {
            addNote(baseNote, modifier);
            return;
        }

        recordUndoCheckpoint();
        boolean wasHistorySuspended = historySuspended;
        historySuspended = true;
        try {
            // Den Einfügezeiger und beide Seitenpositionen sichern: Das Ersetzen
            // darf nicht beeinflussen, wo die nächste neue Note landet.
            GridPane oldActiveGrid = activeGridPane;
            int oldRowOffset = rowOffset;
            int oldColumnOffset = columnOffset;
            int oldRowOffset1 = rowOffset1;
            int oldColumnOffset1 = columnOffset1;
            int oldRowOffset2 = rowOffset2;
            int oldColumnOffset2 = columnOffset2;

            for (SelectedNoteTarget target : targets) {
                removeNoteSlot(target.grid(), target.container(), target.column(), target.row());
            }
            clearNoteSelection();

            for (SelectedNoteTarget target : targets) {
                activeGridPane = target.grid();
                rowOffset = target.row();
                columnOffset = target.column();
                addNote(baseNote, modifier);
            }

            activeGridPane = oldActiveGrid;
            rowOffset = oldRowOffset;
            columnOffset = oldColumnOffset;
            rowOffset1 = oldRowOffset1;
            columnOffset1 = oldColumnOffset1;
            rowOffset2 = oldRowOffset2;
            columnOffset2 = oldColumnOffset2;
            if (oldActiveGrid != null) {
                syncActivePageOffset();
            }
        } finally {
            historySuspended = wasHistorySuspended;
        }
    }

    private GridPane findNoteGrid(StackPane noteContainer) {
        if (Gridpane1.getChildren().contains(noteContainer)) {
            return Gridpane1;
        }
        if (Gridpane2.getChildren().contains(noteContainer)) {
            return Gridpane2;
        }
        return null;
    }

    private record SelectedNoteTarget(StackPane container, GridPane grid,
                                      Integer column, Integer row) {
    }

    private void removeNoteSlot(GridPane grid, StackPane noteContainer, int column, int row) {
        grid.getChildren().removeIf(node -> {
            if (node == noteContainer) {
                return true;
            }
            Integer nodeColumn = GridPane.getColumnIndex(node);
            Integer nodeRow = GridPane.getRowIndex(node);
            if (!Objects.equals(nodeColumn, column) || nodeRow == null ||
                    nodeRow < row || nodeRow > row + 8) {
                return false;
            }
            // Ellipsen und das optionale Oktav-X gehören zu diesem Notenslot.
            // Wiederholungszeichen tragen eigene UserData und bleiben erhalten.
            return node instanceof Ellipse ||
                    (node instanceof StackPane && node.getUserData() == null);
        });
        removePauseMarker(grid, column, row);
    }


    private void addModifierVisual(StackPane container, NoteModifier modifier) {

        switch (modifier) {

            case OKTAV -> addX(container);

            case LANG -> addLine(container, -8);

            case KURZ -> addDot(container, -8);

            case FESTE -> addText(container, "f", -14);

            case OKTAV_LANG -> {
                addX(container);
                addLine(container, -3);
            }

            case OKTAV_KURZ -> {
                addX(container);
                addDot(container, -4);
            }

            default -> {}
        }
    }

    private void addX(StackPane container) {
        Text x = new Text("x");
        x.setFont(Font.font("Arial", FontWeight.BOLD, 15));
        StackPane.setAlignment(x, Pos.TOP_CENTER);
        StackPane.setMargin(x, new Insets(-18, 0, 0, 0));
        container.getChildren().add(x);
    }

    private void addLine(StackPane container, int offsetY) {
        Line line = new Line(-3.5, 0, 3.5, 0);
        line.setStrokeWidth(1.5);
        StackPane.setAlignment(line, Pos.TOP_CENTER);
        StackPane.setMargin(line, new Insets(offsetY, 0, 0, 0));
        container.getChildren().add(line);
    }

    private void addDot(StackPane container, int offsetY) {
        Circle dot = new Circle(1.8, Color.BLACK);
        StackPane.setAlignment(dot, Pos.TOP_CENTER);
        StackPane.setMargin(dot, new Insets(offsetY, 0, 0, 0));
        container.getChildren().add(dot);
    }

    private void addText(StackPane container, String text, int offsetY) {
        Text t = new Text(text);
        t.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        StackPane.setAlignment(t, Pos.TOP_CENTER);
        StackPane.setMargin(t, new Insets(offsetY, 0, 0, 0));
        container.getChildren().add(t);
    }

    private void addNote(String baseNote, NoteModifier modifier) {

        String finalNoteName = baseNote;
        // Oktav-Modifier sollten die Oktav-Griffbildung verwenden
        if (modifier == NoteModifier.OKTAV || modifier == NoteModifier.OKTAV_LANG || modifier == NoteModifier.OKTAV_KURZ) {
            finalNoteName = baseNote + "_Oktav";
        }

        boolean[] griff = model.getGriff(finalNoteName);
        drawGriffOnGrid(griff, baseNote, finalNoteName, modifier);
    }

    private void styleButtonsOtherButtons(Button button) {
        boolean primary = button == Speichern || button == Laden || button == printGridPaneButton;
        boolean tool = button == Wiederholungszeichen || button == pauseStrokeButton || button == addFreeTextButton;

        String normalBackground = primary ? "#2563EB" : tool ? "#0F766E" : "#FFFFFF";
        String normalBorder = primary ? "#1D4ED8" : tool ? "#0D9488" : "#D5E0EC";
        String normalText = (primary || tool) ? "#FFFFFF" : "#203040";
        String hoverBackground = primary ? "#1D4ED8" : tool ? "#115E59" : "#EEF4FB";
        String hoverBorder = primary ? "#1E40AF" : tool ? "#0F766E" : "#3B82F6";
        String weight = "-fx-font-weight: 600; ";

        String normalStyle = "-fx-background-color: " + normalBackground + "; " +
                "-fx-border-color: " + normalBorder + "; " +
                "-fx-border-width: 1px; " +
                "-fx-font-size: 13px; " +
                weight +
                "-fx-text-fill: " + normalText + "; " +
                "-fx-background-radius: 10px; " +
                "-fx-border-radius: 10px; " +
                "-fx-padding: 8 12;";
        String hoverStyle = "-fx-background-color: " + hoverBackground + "; " +
                "-fx-border-color: " + hoverBorder + "; " +
                "-fx-border-width: 1px; " +
                "-fx-font-size: 13px; " +
                weight +
                "-fx-text-fill: " + normalText + "; " +
                "-fx-background-radius: 10px; " +
                "-fx-border-radius: 10px; " +
                "-fx-padding: 8 12;";

        button.setStyle(normalStyle);
        button.setOnMouseEntered(e -> button.setStyle(hoverStyle));
        button.setOnMouseExited(e -> button.setStyle(normalStyle));
        button.setCursor(Cursor.HAND);
        button.setPrefWidth(150);
        button.setPrefHeight(42);
    }

    private void styleHelpButton(Button button) {
        String normalStyle = "-fx-background-color: transparent; " +
                "-fx-border-color: transparent; " +
                "-fx-text-fill: #1565C0; " +
                "-fx-font-size: 13px; " +
                "-fx-font-weight: bold; " +
                "-fx-underline: true; " +
                "-fx-padding: 2 4; " +
                "-fx-alignment: CENTER_LEFT; " +
                "-fx-cursor: hand;";
        // Der Hilfe-/Kontakt-Link bleibt auch beim Überfahren ohne Hintergrund.
        String hoverStyle = normalStyle;
        button.setStyle(normalStyle);
        button.setPrefWidth(Region.USE_COMPUTED_SIZE);
        button.setMaxWidth(Region.USE_COMPUTED_SIZE);
        button.setPrefHeight(28);
        button.setOnMouseEntered(event -> button.setStyle(hoverStyle));
        button.setOnMouseExited(event -> button.setStyle(normalStyle));
    }

    private void styleButtonsNotes(Button button) {
        String normalStyle = "-fx-background-color: #FFFFFF; " +
                "-fx-border-color: #D5E0EC; " +
                "-fx-border-width: 1px; " +
                "-fx-font-size: 11px; " +
                "-fx-font-weight: 600; " +
                "-fx-text-fill: #203040; " +
                "-fx-background-radius: 10px; " +
                "-fx-border-radius: 10px; " +
                "-fx-padding: 3 3 4 3;";
        String hoverStyle = "-fx-background-color: #EAF3FF; " +
                "-fx-border-color: #3B82F6; " +
                "-fx-border-width: 1px; " +
                "-fx-font-size: 11px; " +
                "-fx-font-weight: 600; " +
                "-fx-text-fill: #0F4C81; " +
                "-fx-background-radius: 10px; " +
                "-fx-border-radius: 10px; " +
                "-fx-padding: 3 3 4 3;";
        button.setStyle(normalStyle);
        button.setOnMouseEntered(e -> button.setStyle(hoverStyle));
        button.setOnMouseExited(e -> button.setStyle(normalStyle));
        button.setCursor(Cursor.HAND);
        // Kompakte Notenkacheln: die Beschriftung bleibt lesbar, die
        // Notenbuttons nehmen aber deutlich weniger Platz ein.
        button.setMinWidth(68);
        button.setPrefWidth(68);
        button.setMaxWidth(68);
        button.setPrefHeight(100);
        button.setOnMouseExited(e -> styleButtonsNotes(button));
    }

    @FXML
    private void handleSelectLeftPage(ActionEvent event) {
        selectPage(Gridpane1);
    }

    @FXML
    private void handleSelectRightPage(ActionEvent event) {
        selectPage(Gridpane2);
    }

    private void selectPage(GridPane targetGrid) {
        if (targetGrid == null) {
            return;
        }

        if (activeGridPane != null) {
            if (activeGridPane == Gridpane1) {
                rowOffset1 = rowOffset;
                columnOffset1 = columnOffset;
            } else {
                rowOffset2 = rowOffset;
                columnOffset2 = columnOffset;
            }
        }

        activeGridPane = targetGrid;
        if (targetGrid == Gridpane1) {
            rowOffset = rowOffset1;
            columnOffset = columnOffset1;
        } else {
            rowOffset = rowOffset2;
            columnOffset = columnOffset2;
        }

        updatePageButtonStyles();
    }

    private void stylePageButton(Button button) {
        button.setPrefWidth(145);
        button.setPrefHeight(38);
        button.setStyle("-fx-background-color: #ECEFF1; -fx-border-color: #CFD8DC; " +
                "-fx-border-radius: 6px; -fx-background-radius: 6px; -fx-font-weight: bold;");
    }

    private void updatePageButtonStyles() {
        if (leftPageButton == null || rightPageButton == null || activeGridPane == null) {
            return;
        }

        String selectedStyle = "-fx-background-color: #1976D2; -fx-text-fill: white; " +
                "-fx-border-color: #1565C0; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-font-weight: bold;";
        String normalStyle = "-fx-background-color: #ECEFF1; -fx-text-fill: #263238; " +
                "-fx-border-color: #CFD8DC; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-font-weight: bold;";

        leftPageButton.setStyle(activeGridPane == Gridpane1 ? selectedStyle : normalStyle);
        rightPageButton.setStyle(activeGridPane == Gridpane2 ? selectedStyle : normalStyle);
    }

    private void handleDeleteLastNoteAction(ActionEvent event) {
        // Ein Pausenstrich verbraucht keine Spalte. Wenn er das letzte
        // eingefügte Element ist, liegt der Cursor daher noch genau auf ihm.
        boolean pauseAtCursor = pauseMarkers.stream().anyMatch(marker ->
                marker.grid == activeGridPane &&
                        marker.column == columnOffset &&
                        marker.row == rowOffset);
        if (pauseAtCursor) {
            removePauseMarker(activeGridPane, columnOffset, rowOffset);
            syncActivePageOffset();
            return;
        }

        StackPane lastSlot = activeGridPane.getChildren().stream()
                .filter(node -> node instanceof StackPane && tokenForGridNode(node) != null)
                .map(node -> (StackPane) node)
                .max(Comparator.comparingInt((StackPane node) -> GridPane.getRowIndex(node))
                        .thenComparingInt(node -> GridPane.getColumnIndex(node)))
                .orElse(null);
        if (lastSlot == null) {
            return;
        }
        markDocumentDirty();
        rowOffset = GridPane.getRowIndex(lastSlot);
        columnOffset = GridPane.getColumnIndex(lastSlot);
        if (EMPTY_BLOCK.equals(tokenForGridNode(lastSlot))) {
            activeGridPane.getChildren().removeIf(node -> node.getProperties().get(EMPTY_BLOCK_OWNER) == lastSlot);
        } else {
            removeNoteSlot(activeGridPane, lastSlot, columnOffset, rowOffset);
        }
        NoteSlot removedSlot = new NoteSlot(columnOffset, rowOffset);
        slurAnnotations.removeIf(annotation -> {
            if (annotation.grid == activeGridPane && annotation.slots.contains(removedSlot)) {
                annotationOverlay.getChildren().remove(annotation.path);
                return true;
            }
            return false;
        });
        clearAllSelection();
        syncActivePageOffset();
        refreshEmptyBlockGuides();
    }

    // Zeichne die neue Note in einer neuen Spalte (basierend auf columnOffset und rowOffset)
    private void drawGriffOnGrid(boolean[] griff, String baseNote, String finalNoteName, NoteModifier modifier) {

        if (rowOffset >= 119) return;

        markDocumentDirty();

        StackPane noteContainer = null;
        StackPane oktavContainer = null;

        for (int i = 0; i < griff.length; i++) {

            Ellipse ellipse = new Ellipse(4.2, 4.5);

            if (griff[i]) {
                ellipse.setFill(Color.BLACK);
            } else {
                ellipse.setFill(Color.WHITE);
                ellipse.setStroke(Color.BLACK);
                ellipse.setStrokeWidth(1.4);
                ellipse.setRadiusX(ellipse.getRadiusX() - ellipse.getStrokeWidth() / 2);
                ellipse.setRadiusY(ellipse.getRadiusY() - ellipse.getStrokeWidth() / 2);
            }

            // UserData: baseNote|Modifier - wird zum Speichern/ Laden verwendet
            ellipse.setUserData(baseNote + "|" + modifier.name());

            if (i == 0) {
                noteContainer = new StackPane();
                noteContainer.setAlignment(Pos.CENTER);
                noteContainer.setUserData(baseNote + "|" + modifier.name());
                installNoteSelectionHandler(noteContainer, noteContainer, activeGridPane);
                noteContainer.getChildren().add(ellipse);

                // Nur Punkt/Strich/Feste über erstem Kreis
                if (modifier == NoteModifier.LANG) addLine(noteContainer, -8);
                if (modifier == NoteModifier.KURZ) addDot(noteContainer, -8);
                if (modifier == NoteModifier.FESTE) addText(noteContainer, "f", -14);
                if (modifier == NoteModifier.OKTAV_LANG) addLine(noteContainer, -8);
                if (modifier == NoteModifier.OKTAV_KURZ) addDot(noteContainer, -8);

                activeGridPane.add(noteContainer, columnOffset, rowOffset);
            } else if (i == 5) {
                // Oktav-X zwischen 3. und 4. Kreis
                if (modifier == NoteModifier.OKTAV || modifier == NoteModifier.OKTAV_LANG || modifier == NoteModifier.OKTAV_KURZ) {
                    oktavContainer = new StackPane();
                    oktavContainer.setAlignment(Pos.CENTER);
                    addX(oktavContainer);
                    // Position: zwischen 3 und 4, etwas weiter unten
                    GridPane.setMargin(oktavContainer, new Insets(3.5, 0, 0, 0)); // Verschiebung nach unten
                    activeGridPane.add(oktavContainer, columnOffset, rowOffset + 5);
                    installNoteSelectionHandler(oktavContainer, noteContainer, activeGridPane);
                }
                int targetRow = (i < 3) ? rowOffset + i : rowOffset + i + 2;
                activeGridPane.add(ellipse, columnOffset, targetRow);
            } else {
                int targetRow = (i < 3) ? rowOffset + i : rowOffset + i + 2;
                activeGridPane.add(ellipse, columnOffset, targetRow);
            }

            // Alle Kreise einer Note sind auswählbar, nicht nur der oberste.
            installNoteSelectionHandler(ellipse, noteContainer, activeGridPane);
        }

        advanceGridPosition();

        activeGridPane.setHgap(5.5);
    }

    private List<Node> fillRemainingRowsWithCisDes(GridPane grid, int startRow, int startCol) {
        boolean[] cisDesGriff = model.getGriff("Cis_Des");
        int row = startRow;
        int col = startCol;
        List<Node> insertedNodes = new ArrayList<>();

        while (row < 119) {
            int finalRow = row;
            int finalCol = col;
            boolean slotOccupied = grid.getChildren().stream().anyMatch(node ->
                    GridPane.getColumnIndex(node) != null &&
                            GridPane.getRowIndex(node) != null &&
                            GridPane.getColumnIndex(node) == finalCol &&
                            GridPane.getRowIndex(node) == finalRow
            );

            if (!slotOccupied) {
                for (int i = 0; i < cisDesGriff.length; i++) {
                    javafx.scene.shape.Ellipse ellipse = new javafx.scene.shape.Ellipse(4.2, 4.5);
                    if (cisDesGriff[i]) {
                        ellipse.setFill(Color.BLACK);
                    } else {
                        ellipse.setFill(Color.WHITE);
                        ellipse.setStroke(Color.BLACK);
                        ellipse.setStrokeWidth(1.4);
                        ellipse.setRadiusX(ellipse.getRadiusX() - (ellipse.getStrokeWidth() / 2));
                        ellipse.setRadiusY(ellipse.getRadiusY() - (ellipse.getStrokeWidth() / 2));
                    }
                    ellipse.setUserData("Cis_Des|NORMAL");

                    int targetRow = (i < 3) ? row + i : row + i + 2;
                    grid.add(ellipse, col, targetRow);
                    insertedNodes.add(ellipse);
                }
            }

            if (col >= 40) {
                col = 0;
                row += 17;
            } else {
                col++;
            }
        }

        grid.setHgap(5.5);
        return insertedNodes;
    }

    //Drucken von Noten
    @FXML
    private void handlePrintGridPaneAction(ActionEvent event) {
        Alert options = new Alert(Alert.AlertType.CONFIRMATION);
        options.initOwner(printGridPaneButton.getScene().getWindow());
        options.setTitle("Drucken / PDF exportieren");
        options.setHeaderText("Wie sollen freie Notenplätze ausgegeben werden?");
        options.setContentText("Bewusst eingefügte leere Notenblöcke bleiben bei beiden Optionen frei.\n"
                + "Für einen PDF-Export im nächsten Dialog einen PDF-Drucker auswählen.");
        ButtonType unchanged = new ButtonType("Leerstellen beibehalten", ButtonBar.ButtonData.OK_DONE);
        ButtonType fill = new ButtonType("Mit Cis/Des auffüllen", ButtonBar.ButtonData.OTHER);
        options.getButtonTypes().setAll(unchanged, fill, ButtonType.CANCEL);
        Optional<ButtonType> choice = options.showAndWait();
        if (choice.isEmpty() || choice.get() == ButtonType.CANCEL) {
            return;
        }
        printPage(choice.get() == fill);
    }

    private void printPage(boolean fillWithCisDes) {
        syncActivePageOffset();
        clearAllSelection();
        // Reservierte Leerblöcke sind belegte, unsichtbare Slots und bleiben frei.
        List<Node> tempNodes1 = prepareGridForPrint(Gridpane1, fillWithCisDes);
        List<Node> tempNodes2 = prepareGridForPrint(Gridpane2, fillWithCisDes);
        Map<TextField, String> prompts = hidePromptTextsForPrinting();

        try {
            // Immer die feste DIN-A4-Seitenfläche drucken, nicht den variablen
            // Bildschirmbereich. Dadurch entstehen keine Randflächen vom UI.
            WritableImage snapshot = snapshotPageForPrint();

            Printer printer = Printer.getDefaultPrinter();
            if (printer == null) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION,
                        "Kein Drucker gefunden. Bitte einen Drucker oder PDF-Drucker einrichten.");
                alert.initOwner(printGridPaneButton.getScene().getWindow());
                alert.showAndWait();
                return;
            }

            PageLayout layout = printer.createPageLayout(
                    Paper.A4,
                    PageOrientation.LANDSCAPE,
                    0, 0, 0, 0
            );

            javafx.print.PrinterJob job = javafx.print.PrinterJob.createPrinterJob();

            if (job != null) {
                // Windows-Druckdialoge starten teilweise mit Hochformat. Das
                // Layout deshalb vor und nach dem Dialog nochmals fest vorgeben.
                job.getJobSettings().setPageLayout(layout);
            }

            if (job != null && job.showPrintDialog(printGridPaneButton.getScene().getWindow())) {
                job.getJobSettings().setPageLayout(layout);
                ImageView printView = new ImageView(snapshot);
                printView.setFitWidth(layout.getPrintableWidth());
                printView.setFitHeight(layout.getPrintableHeight());
                printView.setPreserveRatio(false);
                printView.setSmooth(true);

                boolean success = job.printPage(layout, printView);
                if (success) {
                    job.endJob();
                }
            }
        } finally {
            // Die Cystest-Noten existieren nur während des Drucks - auch bei
            // Abbruch, fehlendem Drucker oder einem Druckfehler werden sie entfernt.
            Gridpane1.getChildren().removeAll(tempNodes1);
            Gridpane2.getChildren().removeAll(tempNodes2);
            restorePromptTexts(prompts);
            syncActivePageOffset();
        }
    }

    private WritableImage snapshotPageForPrint() {
        boolean guidesWereVisible = emptyBlockOverlay.isVisible();
        emptyBlockOverlay.setVisible(false);
        try {
            pageCanvas.applyCss();
            pageCanvas.layout();
            return pageCanvas.snapshot(null, null);
        } finally {
            emptyBlockOverlay.setVisible(guidesWereVisible);
        }
    }

    private List<Node> prepareGridForPrint(GridPane grid, boolean fillWithCisDes) {
        return fillWithCisDes ? fillRemainingRowsWithCisDes(grid, 1, 0) : List.of();
    }

    @FXML
    private void handleSaveToFile(ActionEvent event) {
        saveToFile(printGridPaneButton.getScene().getWindow());
    }

    private boolean saveToFile(Window owner) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Speichern unter");
        File file = fileChooser.showSaveDialog(owner);

        if (file == null) {
            return false;
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            // Seitentexte werden ebenfalls gespeichert. Base64 verhindert,
            // dass Sonderzeichen oder Semikolons das Dateiformat stören.
            writePageHeadings(writer);

            writeGridTokens(writer, "Seite1", Gridpane1);
            writeGridTokens(writer, "Seite2", Gridpane2);
            writeAnnotations(writer, "Seite1", Gridpane1);
            writeAnnotations(writer, "Seite2", Gridpane2);

            documentDirty = false;
            savedDocumentSnapshot = captureDocumentSnapshot();
            System.out.println("Datei gespeichert: " + file.getAbsolutePath());
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR, "Die Datei konnte nicht gespeichert werden.");
            alert.setTitle("Speichern fehlgeschlagen");
            if (owner != null) {
                alert.initOwner(owner);
            }
            alert.showAndWait();
            return false;
        }
    }

    private void writeGridTokens(BufferedWriter writer, String page, GridPane grid) throws IOException {
        List<SerializedGridToken> tokens = new ArrayList<>();
        for (Node node : grid.getChildren()) {
            // Nur der oberste StackPane ist der eigentliche Noten-Slot. Die
            // einzelnen Ellipsen darunter würden sonst mehrfach gespeichert.
            if (!(node instanceof StackPane)) {
                continue;
            }
            String token = tokenForGridNode(node);
            Integer column = GridPane.getColumnIndex(node);
            Integer row = GridPane.getRowIndex(node);
            if (token != null && column != null && row != null) {
                tokens.add(new SerializedGridToken(token, column, row, false));
            }
        }

        for (PauseMarker marker : pauseMarkers) {
            if (marker.grid == grid && !marker.free) {
                tokens.add(new SerializedGridToken("PAUSENSTRICH", marker.column, marker.row, true));
            }
        }

        tokens.sort(Comparator.comparingInt(SerializedGridToken::row)
                .thenComparingInt(SerializedGridToken::column)
                // Pause und folgende Note teilen sich dieselbe Spalte. Beim
                // Laden muss der Pausenstrich deshalb zuerst wiederkommen.
                .thenComparingInt(token -> token.pause() ? 0 : 1));
        for (SerializedGridToken token : tokens) {
            writer.write(page + ";" + token.token());
            writer.newLine();
        }
    }

    private record SerializedGridToken(String token, int column, int row, boolean pause) {
    }

    private record PendingSlur(String page, String token) {
    }

    private void writePageHeadings(BufferedWriter writer) throws IOException {
        writer.write("Kreis1;" + encodeText(leftCircleText.getText()));
        writer.newLine();
        writer.write("Marsch1;" + encodeText(leftMarchText.getText()));
        writer.newLine();
        writer.write("Kreis2;" + encodeText(rightCircleText.getText()));
        writer.newLine();
        writer.write("Marsch2;" + encodeText(rightMarchText.getText()));
        writer.newLine();
        writer.write("Kreis1Groesse;" + pageHeadingSize(leftCircleText));
        writer.newLine();
        writer.write("Marsch1Groesse;" + pageHeadingSize(leftMarchText));
        writer.newLine();
        writer.write("Kreis2Groesse;" + pageHeadingSize(rightCircleText));
        writer.newLine();
        writer.write("Marsch2Groesse;" + pageHeadingSize(rightMarchText));
        writer.newLine();
    }

    private void writeAnnotations(BufferedWriter writer, String page, GridPane grid) throws IOException {
        for (PauseMarker marker : pauseMarkers) {
            if (marker.grid == grid && marker.free) {
                writer.write(page + ";PAUSE_FREI|" + marker.line.getLayoutX() + "|" + marker.line.getLayoutY());
                writer.newLine();
            }
        }
        for (PageTextAnnotation annotation : textAnnotations) {
            if (annotation.grid == grid) {
                Text text = annotation.text;
                writer.write(page + ";TEXT|" + encodeText(text.getText()) + "|" +
                        text.getLayoutX() + "|" + text.getLayoutY() + "|" +
                        Math.round(text.getFont().getSize()));
                writer.newLine();
            }
        }
        for (SlurAnnotation annotation : slurAnnotations) {
            if (annotation.grid == grid) {
                String slots = annotation.slots.stream()
                        .map(slot -> slot.column() + "," + slot.row())
                        .collect(java.util.stream.Collectors.joining(";"));
                writer.write(page + ";BOGEN|" + slots);
                writer.newLine();
            }
        }
    }

    private String tokenForGridNode(Node node) {
        // Repeat markers and pause strokes carry their token on the top-level
        // node. Notes keep their legacy token on the first ellipse.
        if (node.getUserData() instanceof String token) {
            return token;
        }
        if (node instanceof StackPane stackPane) {
            for (Node child : stackPane.getChildren()) {
                if (child instanceof Ellipse ellipse && ellipse.getUserData() instanceof String token) {
                    return token;
                }
            }
        }
        return null;
    }

    private static String encodeText(String text) {
        return Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));
    }

    private static String decodeText(String encodedText) {
        try {
            return new String(Base64.getDecoder().decode(encodedText), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            // Lesbare Fallback-Daten aus einer älteren oder manuell bearbeiteten Datei.
            return encodedText;
        }
    }

    private boolean restorePageText(String key, String encodedText) {
        TextField sizeField = switch (key) {
            case "Kreis1Groesse" -> leftCircleText;
            case "Marsch1Groesse" -> leftMarchText;
            case "Kreis2Groesse" -> rightCircleText;
            case "Marsch2Groesse" -> rightMarchText;
            default -> null;
        };
        if (sizeField != null) {
            try {
                int size = Integer.parseInt(encodedText);
                if (size >= 8 && size <= 72) applyPageHeadingSize(sizeField, size);
            } catch (NumberFormatException ignored) {
                // Ungültige Größenangaben behalten die bisherige Größe bei.
            }
            return true;
        }
        String text = decodeText(encodedText);
        switch (key) {
            case "Kreis1" -> leftCircleText.setText(text);
            case "Marsch1" -> leftMarchText.setText(text);
            case "Kreis2" -> rightCircleText.setText(text);
            case "Marsch2" -> rightMarchText.setText(text);
            default -> {
                return false;
            }
        }
        return true;
    }

    private void clearPageTexts() {
        leftCircleText.clear();
        rightCircleText.clear();
        leftMarchText.clear();
        rightMarchText.clear();
        for (TextField field : List.of(leftCircleText, rightCircleText, leftMarchText, rightMarchText)) {
            applyPageHeadingSize(field, defaultPageHeadingSize(field));
        }
    }

    private boolean restoreSpecialToken(String token) {
        if (EMPTY_BLOCK.equals(token) || token.startsWith(EMPTY_BLOCK + "|")) {
            // Dateien mit kürzlich gespeicherter variabler Länge werden wieder
            // als der ursprüngliche feste Leerblock geladen.
            addEmptyBlock();
            return true;
        }
        if ("PAUSENSTRICH".equals(token)) {
            addPauseStroke();
            return true;
        }
        if (token.startsWith("PAUSE_FREI|")) {
            String[] parts = token.split("\\|", 3);
            if (parts.length != 3 || activeGridPane == null) return false;
            try {
                double x = Double.parseDouble(parts[1]);
                double y = Double.parseDouble(parts[2]);
                if (!Double.isFinite(x) || !Double.isFinite(y)) return false;
                PauseMarker marker = createPauseMarker(activeGridPane, columnOffset, rowOffset);
                marker.free = true;
                marker.line.setLayoutX(Math.max(0, Math.min(PAGE_WIDTH, x)));
                marker.line.setLayoutY(Math.max(0, Math.min(PAGE_HEIGHT - 16, y)));
                return true;
            } catch (NumberFormatException ignored) {
                return false;
            }
        }
        if (token.startsWith("TEXT|")) {
            String[] parts = token.split("\\|", 5);
            if (parts.length == 4 || parts.length == 5) {
                try {
                    addTextAnnotation(
                            decodeText(parts[1]),
                            new Point2D(Double.parseDouble(parts[2]), Double.parseDouble(parts[3])),
                            parts.length == 5 ? Math.max(8, Math.min(72, Integer.parseInt(parts[4]))) : 18
                    );
                    return true;
                } catch (NumberFormatException ignored) {
                    return false;
                }
            }
        }
        if (token.startsWith("BOGEN|")) {
            if (activeGridPane == null) {
                return false;
            }
            List<NoteSlot> slots = new ArrayList<>();
            for (String part : token.substring("BOGEN|".length()).split(";")) {
                String[] coordinates = part.split(",", 2);
                if (coordinates.length != 2) {
                    return false;
                }
                try {
                    slots.add(new NoteSlot(
                            Integer.parseInt(coordinates[0]),
                            Integer.parseInt(coordinates[1])
                    ));
                } catch (NumberFormatException ignored) {
                    return false;
                }
            }
            activeGridPane.applyCss();
            activeGridPane.layout();
            Path path = createSlurPath(activeGridPane, slots);
            if (!path.getElements().isEmpty()) {
                annotationOverlay.getChildren().add(path);
                slurAnnotations.add(new SlurAnnotation(activeGridPane, path, slots));
            }
            return true;
        }
        if (token.startsWith("WIEDERHOLUNG|")) {
            String type = token.substring("WIEDERHOLUNG|".length());
            try {
                addWiederholungszeichen(Wiederholungsart.valueOf(type));
                return true;
            } catch (IllegalArgumentException ignored) {
                // Unknown future marker: leave it ignored rather than
                // interpreting it as a note.
            }
        }
        return false;
    }

    private void restorePendingSlurs(List<PendingSlur> pendingSlurs) {
        for (PendingSlur pending : pendingSlurs) {
            if (selectPageForSerializedToken(pending.page())) {
                restoreSpecialToken(pending.token());
            }
        }
    }

    private static String[] splitSerializedLine(String line) {
        int separator = line.indexOf(';');
        if (separator < 0) {
            return null;
        }
        // Anders als String.split(...) behält diese Variante auch einen
        // leeren Text hinter dem Semikolon bei.
        return new String[]{line.substring(0, separator), line.substring(separator + 1)};
    }

    private boolean selectPageForSerializedToken(String page) {
        if ("Seite1".equals(page)) {
            selectPage(Gridpane1);
            return true;
        }
        if ("Seite2".equals(page)) {
            selectPage(Gridpane2);
            return true;
        }
        return false;
    }

    @FXML
    private void handleLoadChoice(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Datei laden");
        alert.setHeaderText("Wie sollen die Noten geladen werden?");
        alert.setContentText("Bitte wähle eine Option:");

        ButtonType buttonCancel = new ButtonType("Abbrechen", ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType buttonOverwrite = new ButtonType("Alle Noten überschreiben");
        ButtonType buttonAdd = new ButtonType("Noten hinzufügen");

        alert.getButtonTypes().setAll(buttonCancel, buttonOverwrite, buttonAdd);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent()) {
            if (result.get() == buttonOverwrite) {
                handleLoadFromFile(event); // deine vorhandene Methode
            } else if (result.get() == buttonAdd) {
                handleLoadFromFileAndAdd(event); // neue Methode
            } else {
                // Abbrechen → nichts tun
            }
        }
    }

    private void clearAllPagesBeforeOverwrite() {
        clearAllSelection();
        removePauseMarkersForGrid(Gridpane1);
        removePauseMarkersForGrid(Gridpane2);
        removeAnnotationsForGrid(Gridpane1);
        removeAnnotationsForGrid(Gridpane2);
        Gridpane1.getChildren().clear();
        Gridpane2.getChildren().clear();
        refreshEmptyBlockGuides();
        notesSeite1.clear();
        notesSeite2.clear();
        clearPageTexts();

        rowOffset1 = 1;
        columnOffset1 = 0;
        rowOffset2 = 1;
        columnOffset2 = 0;
        activeGridPane = Gridpane1;
        rowOffset = rowOffset1;
        columnOffset = columnOffset1;
    }

    @FXML
    private void handleLoadFromFile(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Datei laden");
        File file = fileChooser.showOpenDialog(printGridPaneButton.getScene().getWindow());

        if (file != null) {
            recordUndoCheckpoint();
            historySuspended = true;
            // Überschreiben bedeutet wirklich: erst beide Seiten leeren,
            // danach ausschließlich die Datei neu einlesen.
            clearAllPagesBeforeOverwrite();

            List<PendingSlur> pendingSlurs = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = splitSerializedLine(line);
                    if (parts != null && parts.length == 2) {
                        if (restorePageText(parts[0], parts[1])) {
                            continue;
                        }

                        String seite = parts[0];
                        String token = parts[1];
                        if (seite.startsWith("Seite") && token.startsWith("BOGEN|")) {
                            pendingSlurs.add(new PendingSlur(seite, token));
                            continue;
                        }
                        if (seite.startsWith("Seite") && selectPageForSerializedToken(seite)) {
                            if (restoreSpecialToken(token)) {
                                syncActivePageOffset();
                                continue;
                            }
                        }
                        String baseNote;
                        NoteModifier modifier = NoteModifier.NORMAL;

                        if (token.contains("|")) {
                            String[] t = token.split("\\|");
                            baseNote = t[0];
                            try {
                                modifier = NoteModifier.valueOf(t[1]);
                            } catch (Exception ex) {
                                modifier = NoteModifier.NORMAL;
                            }
                        } else {
                            baseNote = token;
                        }

                        String finalNoteName = baseNote;
                        if (modifier == NoteModifier.OKTAV || modifier == NoteModifier.OKTAV_LANG || modifier == NoteModifier.OKTAV_KURZ) {
                            finalNoteName = baseNote + "_Oktav";
                        }

                        boolean[] griff = model.getGriff(finalNoteName);

                        if (seite.equals("Seite1")) {
                            activeGridPane = Gridpane1;
                            rowOffset = rowOffset1;
                            columnOffset = columnOffset1;
                            drawGriffOnGrid(griff, baseNote, finalNoteName, modifier);
                            rowOffset1 = rowOffset;
                            columnOffset1 = columnOffset;
                        } else if (seite.equals("Seite2")) {
                            activeGridPane = Gridpane2;
                            rowOffset = rowOffset2;
                            columnOffset = columnOffset2;
                            drawGriffOnGrid(griff, baseNote, finalNoteName, modifier);
                            rowOffset2 = rowOffset;
                            columnOffset2 = columnOffset;
                        }
                    }
                }
                restorePendingSlurs(pendingSlurs);
                documentDirty = false;
                savedDocumentSnapshot = captureDocumentSnapshot();
                System.out.println("Datei geladen: " + file.getAbsolutePath());

                activeGridPane = Gridpane1;
                rowOffset = rowOffset1;
                columnOffset = columnOffset1;
                updatePageButtonStyles();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                historySuspended = false;
            }
        }
    }

    @FXML
    private void handleLoadFromFileAndAdd(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Datei laden");
        File file = fileChooser.showOpenDialog(printGridPaneButton.getScene().getWindow());

        if (file != null) {
            recordUndoCheckpoint();
            historySuspended = true;
            List<PendingSlur> pendingSlurs = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = splitSerializedLine(line);
                    if (parts != null && parts.length == 2) {
                        if (restorePageText(parts[0], parts[1])) {
                            continue;
                        }

                        String seite = parts[0];
                        String token = parts[1];
                        if (seite.startsWith("Seite") && token.startsWith("BOGEN|")) {
                            pendingSlurs.add(new PendingSlur(seite, token));
                            continue;
                        }
                        if (seite.startsWith("Seite") && selectPageForSerializedToken(seite)) {
                            if (restoreSpecialToken(token)) {
                                syncActivePageOffset();
                                continue;
                            }
                        }
                        String baseNote;
                        NoteModifier modifier = NoteModifier.NORMAL;

                        if (token.contains("|")) {
                            String[] t = token.split("\\|");
                            baseNote = t[0];
                            try {
                                modifier = NoteModifier.valueOf(t[1]);
                            } catch (Exception ex) {
                                modifier = NoteModifier.NORMAL;
                            }
                        } else {
                            baseNote = token;
                        }

                        String finalNoteName = baseNote;
                        if (modifier == NoteModifier.OKTAV || modifier == NoteModifier.OKTAV_LANG || modifier == NoteModifier.OKTAV_KURZ) {
                            finalNoteName = baseNote + "_Oktav";
                        }

                        boolean[] griff = model.getGriff(finalNoteName);

                        if (seite.equals("Seite1")) {
                            activeGridPane = Gridpane1;
                            rowOffset = rowOffset1;
                            columnOffset = columnOffset1;

                            drawGriffOnGrid(griff, baseNote, finalNoteName, modifier);

                            rowOffset1 = rowOffset;
                            columnOffset1 = columnOffset;
                        } else if (seite.equals("Seite2")) {
                            activeGridPane = Gridpane2;
                            rowOffset = rowOffset2;
                            columnOffset = columnOffset2;

                            drawGriffOnGrid(griff, baseNote, finalNoteName, modifier);

                            rowOffset2 = rowOffset;
                            columnOffset2 = columnOffset;
                        }
                    }
                }
                restorePendingSlurs(pendingSlurs);
                markDocumentDirty();
                System.out.println("Datei hinzugefügt: " + file.getAbsolutePath());

                activeGridPane = Gridpane1;
                rowOffset = rowOffset1;
                columnOffset = columnOffset1;
                updatePageButtonStyles();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                historySuspended = false;
            }
        }
    }

    private void addIconsToNoteButtons() {
        Map<Button, String> mapping = Map.ofEntries(
                Map.entry(Cis_Des, "Cis_Des.png"),
                Map.entry(D, "D.png"),
                Map.entry(Dis_Es, "Dis_Es.png"),
                Map.entry(E, "E.png"),
                Map.entry(F, "F.png"),
                Map.entry(F_1, "F_1.png"),
                Map.entry(Fis_Ges, "Fis_Ges.png"),
                Map.entry(G, "G.png"),
                Map.entry(Gis_As, "Gis_As.png"),
                Map.entry(A, "A.png"),
                Map.entry(Ais_B, "Ais_B.png"),
                Map.entry(H, "H.png"),
                Map.entry(C1, "C1.png"),
                Map.entry(Cis_Des1, "Cis_Des1.png"),
                Map.entry(D1, "D1.png"),
                Map.entry(Dis_Es1, "Dis_Es1.png"),
                Map.entry(E1, "E1.png"),
                Map.entry(Fis_Ges1, "Fis_Ges1.png"),
                Map.entry(F1, "F1.png"),
                Map.entry(G1, "G1.png"),
                Map.entry(Gis_As1, "Gis_As1.png"),
                Map.entry(A1, "A1.png")
        );

        final double ICON_MAX_WIDTH = 54;
        final double ICON_MAX_HEIGHT = 54;

        for (Map.Entry<Button, String> e : mapping.entrySet()) {
            Button btn = e.getKey();
            String fileName = e.getValue();

            if (btn == null) {
                System.err.println("⚠ Button ist null für Bild: " + fileName);
                continue;
            }

            try (InputStream is = getClass().getResourceAsStream("Bilder/" + fileName)) {
                if (is == null) {
                    System.err.println("Bild nicht gefunden: " + fileName + " (Pfad: Bilder/)");
                    continue;
                }

                javafx.scene.image.Image img = new javafx.scene.image.Image(is);
                ImageView iv = new ImageView(img);

                // Größenbegrenzung & Verhalten
                iv.setPreserveRatio(true);
                iv.setFitWidth(ICON_MAX_WIDTH);
                iv.setFitHeight(ICON_MAX_HEIGHT);
                iv.setSmooth(true);
                iv.setPickOnBounds(false);

                // Lege grafisches Icon links neben dem Text
                btn.setGraphic(iv);
                btn.setContentDisplay(ContentDisplay.BOTTOM); // Bild links vom Text
                btn.setGraphicTextGap(3); // kompakter Abstand zwischen Bild und Text
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
}
