package com.code.noteswriter;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.print.PageLayout;
import javafx.print.PageOrientation;
import javafx.print.Paper;
import javafx.print.Printer;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;

import java.io.*;
import java.util.*;

public class Controller {
    @FXML
    public StackPane printArea;
    public Label pageIndicator;

    private Model model;
    private int rowOffset = 1;
    private int columnOffset = 0;
    private final List<Node> selectedNotes = new ArrayList<>();
    private final List<String> notesSeite1 = new ArrayList<>();
    private final List<String> notesSeite2 = new ArrayList<>();
    @FXML
    public Button Cis_Des, D, Dis_Es, E,
            F, F_1, Fis_Ges, G, Gis_As, A, Ais_B, H, C1, Cis_Des1, D1, Dis_Es1, E1, Fis_Ges1, F1, G1, Gis_As1, A1,
            Seitenwechsel, Speichern, Laden, printGridPaneButton,  W_Offen, deleteLastNoteButton, deleteAllNotesButton;

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
    public void setModel(Model model) {
        this.model = model;
        this.activeGridPane = Gridpane1;
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
        styleButtonsOtherButtons(Seitenwechsel);
        styleButtonsOtherButtons(Speichern);
        styleButtonsOtherButtons(Laden);
        styleButtonsOtherButtons(printGridPaneButton);
        styleButtonsOtherButtons(W_Offen);
        styleButtonsOtherButtons(deleteLastNoteButton);
        styleButtonsOtherButtons(deleteAllNotesButton);
        pageIndicator.setTextFill(Color.BLUE);
        addIconsToNoteButtons();
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

        button.setOnMouseClicked(event -> {

            String noteName = button.getText();

            if (event.getButton() == MouseButton.PRIMARY) {
                addNote(noteName, NoteModifier.NORMAL);
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
        activeContextMenu = menu;

        boolean allowOktav = !NO_OKTAV_NOTES.contains(baseNote);

        MenuItem lang = new MenuItem("Lang (-)");
        MenuItem kurz = new MenuItem("Kurz (•)");
        MenuItem feste = new MenuItem("Feste (f)");

        lang.setOnAction(e -> addNote(baseNote, NoteModifier.LANG));
        kurz.setOnAction(e -> addNote(baseNote, NoteModifier.KURZ));
        feste.setOnAction(e -> addNote(baseNote, NoteModifier.FESTE));
        menu.getItems().addAll(lang, kurz, feste);
        if (allowOktav) {
            MenuItem oktav = new MenuItem("Oktav (x)");
            MenuItem oktavLang = new MenuItem("Oktav + Lang (x + -)");
            MenuItem oktavKurz = new MenuItem("Oktav + Kurz (x + •)");

            oktavLang.setOnAction(e -> addNote(baseNote, NoteModifier.OKTAV_LANG));
            oktavKurz.setOnAction(e -> addNote(baseNote, NoteModifier.OKTAV_KURZ));

            oktav.setOnAction(e -> addNote(baseNote, NoteModifier.OKTAV));
            oktavLang.setOnAction(e -> addNote(baseNote, NoteModifier.OKTAV_LANG));
            oktavKurz.setOnAction(e -> addNote(baseNote, NoteModifier.OKTAV_KURZ));


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
        button.setStyle("-fx-background-color: #E0E0E0; " +  // hellgrauer Hintergrund
                "-fx-border-color: #B0B0B0; " +      // grauer Rand
                "-fx-border-width: 1px; " +
                "-fx-font-size: 14px; " +            // größere Schrift
                "-fx-text-fill: #333333; " +         // dunkle Schriftfarbe
                "-fx-background-radius: 6px; " +     // abgerundete Ecken
                "-fx-padding: 6 12;");               // etwas Innenabstand
        button.setOnMouseEntered(e -> button.setStyle(
                "-fx-background-color: #D0D0D0; " +
                        "-fx-border-color: #A0A0A0; " +
                        "-fx-font-size: 14px; " +
                        "-fx-text-fill: #222222; " +
                        "-fx-background-radius: 6px;" +
                        "-fx-padding: 6 12;"
        ));
        button.setPrefWidth(150);
        button.setPrefHeight(40);
    }

    private void styleButtonsNotes(Button button) {
        button.setStyle("-fx-background-color: #E0E0E0; " +  // hellgrauer Hintergrund
                "-fx-border-color: #B0B0B0; " +      // grauer Rand
                "-fx-border-width: 1px; " +
                "-fx-font-size: 14px; " +            // größere Schrift
                "-fx-text-fill: #333333; " +         // dunkle Schriftfarbe
                "-fx-background-radius: 6px; " +     // abgerundete Ecken
                "-fx-padding: 6 12;");               // etwas Innenabstand
        button.setOnMouseEntered(e -> button.setStyle(
                "-fx-background-color: #D0D0D0; " +
                        "-fx-border-color: #A0A0A0; " +
                        "-fx-font-size: 14px; " +
                        "-fx-text-fill: #222222; " +
                        "-fx-background-radius: 6px;" +
                        "-fx-padding: 6 12;"
        ));
        button.setPrefWidth(80);
        button.setPrefHeight(40);
        button.setOnMouseExited(e -> styleButtonsNotes(button));
        pageIndicator.setBackground(new Background(
                new BackgroundFill(Color.LIGHTGRAY, new CornerRadii(5), Insets.EMPTY)));
        pageIndicator.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
    }

    @FXML
    private void handleSwitchGrid(ActionEvent event) {
        if (activeGridPane == Gridpane1) {
            rowOffset1 = rowOffset;
            columnOffset1 = columnOffset;

            activeGridPane = Gridpane2;

            rowOffset = rowOffset2;
            columnOffset = columnOffset2;
        } else {
            rowOffset2 = rowOffset;
            columnOffset2 = columnOffset;

            activeGridPane = Gridpane1;

            rowOffset = rowOffset1;
            columnOffset = columnOffset1;
        }
        updatePageIndicatorPosition();
    }

    private void updatePageIndicatorPosition() {
        if (activeGridPane == Gridpane1) {
            pageIndicator.setText("Linke Seite");
            pageIndicator.setTextFill(Color.BLUE);
            pageIndicator.setLayoutX(300);
            pageIndicator.setLayoutY(5);
        } else {
            pageIndicator.setText("Rechte Seite");
            pageIndicator.setTextFill(Color.RED);
            pageIndicator.setLayoutX(930);
            pageIndicator.setLayoutY(5);
        }

        pageIndicator.toFront();
    }

    @FXML
    private void handleWOffenAction(ActionEvent event) {
        // Überprüfe, ob wir maximal 119 Zeilen erreicht haben
        if (rowOffset >= 119) {
            return; // Keine weiteren Noten zeichnen, wenn die maximale Zeilenanzahl erreicht ist
        }

        // Zeichne den vertikalen Strich
        javafx.scene.shape.Line line = new javafx.scene.shape.Line();
        line.setStartX(0);
        line.setStartY(5);
        line.setEndX(0);
        line.setEndY(85); // Länge des Strichs, passend für 7 Zeilen
        line.setStroke(Color.BLACK);
        line.setStrokeWidth(2.0);

        // Füge die Linie zur GridPane hinzu (die gesamte Spalte, 7 Zeilen hoch)
        activeGridPane.add(line, columnOffset, rowOffset); // Positioniere in der aktuellen Spalte und Zeile
        GridPane.setRowSpan(line, 9); // Mache die Linie 7 Zeilen hoch

        // Überprüfe, ob wir maximal 40 Spalten erreicht haben
        if (columnOffset >= 40) {
            columnOffset = 0;  // Zurücksetzen, wenn 40 Spalten erreicht sind
            rowOffset += 17;   // Nächste Zeile, dabei nach 40 Spalten um 17 Zeilen erhöhen
        } else {
            columnOffset++;    // Andernfalls die Spalte weiter erhöhen
        }

        activeGridPane.setHgap(5.5);
    }

    @FXML
    private void handleDeleteAllNotesAction(ActionEvent event) {
        boolean isLeft = (activeGridPane == Gridpane1);
        String pageName = isLeft ? "Linke Seite" : "Rechte Seite";

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Alle Noten löschen");
        alert.setHeaderText("Bist du sicher, dass du alle Noten löschen möchtest?");
        alert.setContentText("Es werden alle Noten auf der folgenden Seite gelöscht: " + pageName + "\nDieser Vorgang kann nicht rückgängig gemacht werden.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (activeGridPane == Gridpane1) {
                Gridpane1.getChildren().clear();
                notesSeite1.clear();
                rowOffset1 = 1;
                columnOffset1 = 0;
                rowOffset = rowOffset1;
                columnOffset = columnOffset1;
            } else {
                Gridpane2.getChildren().clear();
                notesSeite2.clear();
                rowOffset2 = 1;
                columnOffset2 = 0;
                rowOffset = rowOffset2;
                columnOffset = columnOffset2;
            }
        }
    }

    @FXML
    private void handleDeleteLastNoteAction(ActionEvent event) {
        // Prüfen, ob überhaupt etwas zu löschen ist
        if (rowOffset == 0 && columnOffset == 0) {
            return; // Bereits ganz am Anfang → nichts mehr zu löschen
        }

        // Spalten und Zeilen zuerst korrekt zurücksetzen
        if (columnOffset > 0) {
            columnOffset--; // Gehe eine Spalte zurück
        } else {
            if (rowOffset >= 17) {
                columnOffset = 40;
                rowOffset -= 17;
            } else {
                // Wir sind am Anfang → nichts mehr da
                rowOffset = 0;
                columnOffset = 0;
                return;
            }
        }

        // Jetzt sicher löschen
        int cellsToDelete = 9;
        for (int i = 0; i < cellsToDelete; i++) {
            int rowToDelete = rowOffset + i;
            int colToDelete = columnOffset;

            activeGridPane.getChildren().removeIf(node ->
                    GridPane.getRowIndex(node) != null &&
                            GridPane.getColumnIndex(node) != null &&
                            GridPane.getRowIndex(node) == rowToDelete &&
                            GridPane.getColumnIndex(node) == colToDelete
            );
        }
    }

    // Zeichne die neue Note in einer neuen Spalte (basierend auf columnOffset und rowOffset)
    private void drawGriffOnGrid(boolean[] griff, String baseNote, String finalNoteName, NoteModifier modifier) {

        if (rowOffset >= 119) return;

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
                }
                int targetRow = (i < 3) ? rowOffset + i : rowOffset + i + 2;
                activeGridPane.add(ellipse, columnOffset, targetRow);
            } else {
                int targetRow = (i < 3) ? rowOffset + i : rowOffset + i + 2;
                activeGridPane.add(ellipse, columnOffset, targetRow);
            }
        }

        if (columnOffset >= 40) {
            columnOffset = 0;
            rowOffset += 17;
        } else {
            columnOffset++;
        }

        activeGridPane.setHgap(5.5);
    }

    private List<Node> fillRemainingRowsWithCisDes(GridPane grid, int startRow, int startCol) {
        boolean[] cisDesGriff = model.getGriff("Cis_Des");
        int row = startRow;
        int col = startCol;
        List<Node> insertedNodes = new ArrayList<>();

        while (row < 119) {
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
                int finalCol = col;
                boolean cellOccupied = grid.getChildren().stream().anyMatch(node ->
                        GridPane.getColumnIndex(node) != null &&
                                GridPane.getRowIndex(node) != null &&
                                GridPane.getColumnIndex(node) == finalCol &&
                                GridPane.getRowIndex(node) == targetRow
                );

                if (!cellOccupied) {
                    grid.add(ellipse, col, targetRow);
                    insertedNodes.add(ellipse);
                }

                insertedNodes.add(ellipse);
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
        List<Node> tempNodes1 = fillRemainingRowsWithCisDes(Gridpane1, rowOffset1, columnOffset1);
        List<Node> tempNodes2 = fillRemainingRowsWithCisDes(Gridpane2, rowOffset2, columnOffset2);

        WritableImage snapshot = printArea.snapshot(null, null);

        Printer printer = Printer.getDefaultPrinter();
        if (printer == null) {
            System.out.println("Kein Drucker gefunden.");
            return;
        }

        PageLayout layout = printer.createPageLayout(
                Paper.A4,
                PageOrientation.LANDSCAPE,
                0, 0, 0, 0
        );

        javafx.print.PrinterJob job = javafx.print.PrinterJob.createPrinterJob();

        if (job != null && job.showPrintDialog(printGridPaneButton.getScene().getWindow())) {
            ImageView printView = new ImageView(snapshot);
            printView.setFitWidth(layout.getPrintableWidth());
            printView.setFitHeight(layout.getPrintableHeight());
            printView.setPreserveRatio(true);

            boolean success = job.printPage(layout, printView);
            if (success) {
                job.endJob();
            }
        }

        // 🧹 Temp-Noten aus beiden Grids löschen
        Gridpane1.getChildren().removeAll(tempNodes1);
        Gridpane2.getChildren().removeAll(tempNodes2);

        // Aktive Offsets je nach Grid
        if (activeGridPane == Gridpane1) {
            rowOffset1 = rowOffset;
            columnOffset1 = columnOffset;
        } else {
            rowOffset2 = rowOffset;
            columnOffset2 = columnOffset;
        }
    }

    @FXML
    private void handleSaveToFile(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Speichern unter");
        File file = fileChooser.showSaveDialog(printGridPaneButton.getScene().getWindow());

        if (file != null) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                // Schreibe pro Note genau ein Mal: nur StackPanes mit einer Ellipse (erste Kreis)
                for (Node node : Gridpane1.getChildren()) {
                    if (node instanceof StackPane sp) {
                        Ellipse found = null;
                        for (Node child : sp.getChildren()) {
                            if (child instanceof Ellipse e) {
                                found = e;
                                break;
                            }
                        }
                        if (found != null) {
                            Object data = found.getUserData();
                            if (data != null) {
                                writer.write("Seite1;" + data);
                                writer.newLine();
                            }
                        }
                    }
                }

                for (Node node : Gridpane2.getChildren()) {
                    if (node instanceof StackPane sp) {
                        Ellipse found = null;
                        for (Node child : sp.getChildren()) {
                            if (child instanceof Ellipse e) {
                                found = e;
                                break;
                            }
                        }
                        if (found != null) {
                            Object data = found.getUserData();
                            if (data != null) {
                                writer.write("Seite2;" + data);
                                writer.newLine();
                            }
                        }
                    }
                }

                System.out.println("Datei gespeichert: " + file.getAbsolutePath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void handleLoadChoice(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Datei laden");
        alert.setHeaderText("Wie sollen die Noten geladen werden?");
        alert.setContentText("Bitte wähle eine Option:");

        ButtonType buttonCancel = new ButtonType("Abbrechen", ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType buttonOverwrite = new ButtonType("Alle Noten überladen");
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

    @FXML
    private void handleLoadFromFile(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Datei laden");
        File file = fileChooser.showOpenDialog(printGridPaneButton.getScene().getWindow());

        if (file != null) {
            Gridpane1.getChildren().clear();
            Gridpane2.getChildren().clear();
            notesSeite1.clear();
            notesSeite2.clear();
            rowOffset1 = 1;
            columnOffset1 = 0;
            rowOffset2 = 1;
            columnOffset2 = 0;

            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(";");
                    if (parts.length == 2) {
                        String seite = parts[0];
                        String token = parts[1];
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
                System.out.println("Datei geladen: " + file.getAbsolutePath());

                activeGridPane = Gridpane1;
                rowOffset = rowOffset1;
                columnOffset = columnOffset1;
                pageIndicator.setText("Linke Seite");
                pageIndicator.setTextFill(Color.BLUE);
                updatePageIndicatorPosition();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void handleLoadFromFileAndAdd(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Datei laden");
        File file = fileChooser.showOpenDialog(printGridPaneButton.getScene().getWindow());

        if (file != null) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(";");
                    if (parts.length == 2) {
                        String seite = parts[0];
                        String token = parts[1];
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
                System.out.println("Datei hinzugefügt: " + file.getAbsolutePath());

                activeGridPane = Gridpane1;
                rowOffset = rowOffset1;
                columnOffset = columnOffset1;
                pageIndicator.setText("Linke Seite");
                pageIndicator.setTextFill(Color.BLUE);
                updatePageIndicatorPosition();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void addIconsToNoteButtons() {
        Map<Button, String> mapping = Map.ofEntries(
                Map.entry(Cis_Des, "Cis_Des.jpg"),
                Map.entry(D, "D.jpg"),
                Map.entry(Dis_Es, "Dis_Es.jpg"),
                Map.entry(E, "E.jpg"),
                Map.entry(F, "F.jpg"),
                Map.entry(F_1, "F_1.jpg"),
                Map.entry(Fis_Ges, "Fis_Ges.jpg"),
                Map.entry(G, "G.jpg"),
                Map.entry(Gis_As, "Gis_As.jpg"),
                Map.entry(A, "A.jpg"),
                Map.entry(Ais_B, "Ais_B.jpg"),
                Map.entry(H, "H.jpg"),
                Map.entry(C1, "C1.jpg"),
                Map.entry(Cis_Des1, "Cis_Des1.jpg"),
                Map.entry(D1, "D1.jpg"),
                Map.entry(Dis_Es1, "Dis_Es1.jpg"),
                Map.entry(E1, "E1.jpg"),
                Map.entry(Fis_Ges1, "Fis_Ges1.jpg"),
                Map.entry(F1, "F1.jpg"),
                Map.entry(G1, "G1.jpg"),
                Map.entry(Gis_As1, "Gis_As1.jpg"),
                Map.entry(A1, "A1.jpg")
        );

        final double ICON_MAX_WIDTH = 75;
        final double ICON_MAX_HEIGHT = 75;

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
                btn.setGraphicTextGap(8); // Abstand zwischen Bild und Text
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
}
