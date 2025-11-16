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
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Ellipse;
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
    public Button D_Oktav, Dis_Es_Oktav, E_Oktav, F_Oktav, Cis_Des, D, Dis_Es, E,
            F, F_1, Fis_Ges, G, Gis_As, A, Ais_B, H, C1, Cis_Des1, D1, Dis_Es1, E1, Fis_Ges1, G1, Gis_As1, A1,
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
    public void setModel(Model model) {
        this.model = model;
        this.activeGridPane = Gridpane1;
    }

    @FXML
    public void initialize() {
        styleButtonsNotes(D_Oktav);
        styleButtonsNotes(Dis_Es_Oktav);
        styleButtonsNotes(E_Oktav);
        styleButtonsNotes(F_Oktav);
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
    private void handleNoteAction(ActionEvent event) {
        String note1 = ((Button) event.getSource()).getText();
        boolean[] griff = model.getGriff(note1);

        if (!selectedNotes.isEmpty()) {
//            for (javafx.scene.Node note : selectedNotes) {
//                Integer col = GridPane.getColumnIndex(note);
//                Integer row = GridPane.getRowIndex(note);
//
//                Gridpane1.getChildren().removeIf(nodeInGrid ->
//                        GridPane.getColumnIndex(nodeInGrid) != null &&
//                                GridPane.getRowIndex(nodeInGrid) != null &&
//                                GridPane.getColumnIndex(nodeInGrid).equals(col) &&
//                                GridPane.getRowIndex(nodeInGrid) >= row &&
//                                GridPane.getRowIndex(nodeInGrid) <= row + 8
//                );
//
//                for (int i = 0; i < griff.length; i++) {
//                    javafx.scene.shape.Ellipse newEllipse = new javafx.scene.shape.Ellipse(4.2, 4.5);
//
//                    if (griff[i]) {
//                        newEllipse.setFill(Color.BLACK);
//                    } else {
//                        newEllipse.setFill(Color.WHITE);
//                        newEllipse.setStroke(Color.BLACK);
//                        newEllipse.setStrokeWidth(1.4);
//                        newEllipse.setRadiusX(newEllipse.getRadiusX() - (newEllipse.getStrokeWidth() / 2));
//                        newEllipse.setRadiusY(newEllipse.getRadiusY() - (newEllipse.getStrokeWidth() / 2));
//                    }
//
//                    if (i < 3) {
//                        activeGridPane.add(newEllipse, col, row + i);
//                    } else {
//                        activeGridPane.add(newEllipse, col, row + i + 2);
//                    }
//
//                    if (i == 0) {
//                        javafx.scene.shape.Ellipse ref = newEllipse;
//                        ref.setOnMouseClicked(e -> {
//                            if (selectedNotes.contains(ref)) {
//                                ref.setStroke(Color.BLACK);
//                                selectedNotes.remove(ref);
//                            } else {
//                                ref.setStroke(Color.RED);
//                                ref.setStrokeWidth(1.4);
//                                selectedNotes.add(ref);
//                            }
//                        });
//                    }
//                }
//            }
//
//            selectedNotes.clear(); // Nach Bearbeitung zurücksetzen
        } else {
            String note2 = ((Button) event.getSource()).getText();
            drawGriffOnGrid(griff, note2); // Normales Hinzufügen
        }
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

    private void drawGriffOnGrid(boolean[] griff, String noteName) {
        // Überprüfe, ob wir maximal 119 Zeilen erreicht haben
        if (rowOffset >= 119) {
            return; // Keine weiteren Noten zeichnen, wenn die maximale Zeilenanzahl erreicht ist
        }

        // Zeichne die neue Note in einer neuen Spalte (basierend auf columnOffset und rowOffset)
        for (int i = 0; i < griff.length; i++) {
            Ellipse ellipse = new Ellipse(); // Erstelle eine Ellipse
            ellipse.setRadiusX(4.2);  // Horizontaler Radius (Breite des Ovals)
            ellipse.setRadiusY(4.5); // Vertikaler Radius (Höhe des Ovals)

            if (griff[i]) {
                ellipse.setFill(Color.BLACK); // Schwarzes Loch
            } else {
                ellipse.setFill(Color.WHITE); // Weißes Loch
                ellipse.setStroke(Color.BLACK); // Schwarze Umrandung für weiße Löcher
                ellipse.setStrokeWidth(1.4);   // Breite der Umrandung

                // Verkleinere den weißen Kreis leicht, um die Umrandung auszugleichen
                ellipse.setRadiusX(ellipse.getRadiusX() - (ellipse.getStrokeWidth() / 2));
                ellipse.setRadiusY(ellipse.getRadiusY() - (ellipse.getStrokeWidth() / 2));
            }
            ellipse.setUserData(noteName); // ← Speichert den Namen der Note an der Ellipse

            // 👉 Nur griff[0] klickbar machen
            if (i == 0) {
                ellipse.setUserData(noteName);
                ellipse.setOnMouseClicked(e -> {
                    if (selectedNotes.contains(ellipse)) {
                        ellipse.setRadiusX(3.5);
                        ellipse.setRadiusY(3.8);
                        ellipse.setStroke(Color.BLACK);
                        selectedNotes.remove(ellipse);
                    } else {
                        ellipse.setStroke(Color.RED);
                        selectedNotes.add(ellipse);
                    }
                });
            } else {
                ellipse.setUserData(null);
            }

            // Füge die Ellipse zur GridPane hinzu
            if (i < 3) {
                activeGridPane.add(ellipse, columnOffset, rowOffset + i); // Erste drei Löcher in der aktuellen Spalte und Zeile
            } else {
                activeGridPane.add(ellipse, columnOffset, rowOffset + i + 2); // Restliche Löcher, verschoben um 2 Zeilen
            }

            // Füge "x" für Oktav-Noten über der ersten Note hinzu
            if (noteName.contains("Oktav") && i == 0) {
                // Erstelle einen StackPane, um das "x" über der Note zu platzieren
                StackPane noteContainer = new StackPane();
                noteContainer.setAlignment(Pos.TOP_CENTER);

                // Füge die Ellipse zum Container hinzu
                noteContainer.getChildren().add(ellipse);

                // Erstelle das "x" und positioniere es über der Note
                Text xMark = new Text("x");
                xMark.setFont(Font.font("Arial", FontWeight.BOLD, 15));
                xMark.setFill(Color.BLACK);


                // Positioniere das "x" über der Note
                StackPane.setAlignment(xMark, Pos.TOP_CENTER);
                StackPane.setMargin(xMark, new Insets(-20, 0, 0, 0)); // Verschiebe nach oben

                noteContainer.getChildren().add(xMark);

                // Füge den Container zur GridPane hinzu
                activeGridPane.add(noteContainer, columnOffset, rowOffset);

                // Überspringe die normale Ellipse-Platzierung, da sie jetzt im Container ist
                continue;
            }

        }

        // Überprüfe, ob wir maximal 40 Spalten erreicht haben
        if (columnOffset >= 40) {
            columnOffset = 0;  // Zurücksetzen, wenn 40 Spalten erreicht sind
            rowOffset += 17;   // Nächste Zeile, dabei nach 40 Spalten um 17 Zeilen erhöhen
        } else {
            columnOffset++;    // Andernfalls die Spalte weiter erhöhen
        }

        //Horizontaler Abstand zwischen Noten
        activeGridPane.setHgap(5.5);

        // Am Ende von drawGriffOnGrid()
        if (activeGridPane == Gridpane1) {
            notesSeite1.add(noteName);
        } else if (activeGridPane == Gridpane2) {
            notesSeite2.add(noteName);
        }
    }
    //FUNKTIONIERENDER FRÜPR EINE SEITE
//    // Fülle alle Noten mit Cis/Des für das Drucken
//    private void fillRemainingRowsWithCisDes() {
//        // Muster für "Cis/Des": Array, das die Positionen der Ovale beschreibt
//        boolean[] cisDesGriff = model.getGriff("Cis_Des"); // Hol dir das Muster für "Cis/Des"
//
//        while (rowOffset < 119) { // Solange freie Zeilen vorhanden sind
//            drawGriffOnGrid(cisDesGriff, "Cis_Des"); // Zeichne das Muster ins GridPane
//        }
//        selectedNotes.clear();
//    }
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
                ellipse.setUserData("Cis_Des");

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
                for (Node node : Gridpane1.getChildren()) {
                    if (node instanceof Ellipse ellipse) {
                        Object data = ellipse.getUserData();
                        if (data != null) {
                            writer.write("Seite1;" + data.toString());
                            writer.newLine();
                        }
                    }
                    else if (node instanceof StackPane sp) {
                        for (Node child : sp.getChildren()) {
                            if (child instanceof Ellipse childEllipse) {
                                Object data = childEllipse.getUserData();
                                if (data != null) {
                                    writer.write("Seite1;" + data.toString());
                                    writer.newLine();
                                }
                                break;
                            }
                        }
                    }
                }

                for (Node node : Gridpane2.getChildren()) {
                    if (node instanceof Ellipse ellipse) {
                        Object data = ellipse.getUserData();
                        if (data != null) {
                            writer.write("Seite2;" + data.toString());
                            writer.newLine();
                        }
                    } else if (node instanceof StackPane sp) {
                        for (Node child : sp.getChildren()) {
                            if (child instanceof Ellipse childEllipse) {
                                Object data = childEllipse.getUserData();
                                if (data != null) {
                                    writer.write("Seite2;" + data.toString());
                                    writer.newLine();
                                }
                                break;
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
                        String noteName = parts[1];
                        boolean[] griff = model.getGriff(noteName);

                        if (seite.equals("Seite1")) {
                            activeGridPane = Gridpane1;
                            rowOffset = rowOffset1;
                            columnOffset = columnOffset1;
                            drawGriffOnGrid(griff, noteName);
                            rowOffset1 = rowOffset;
                            columnOffset1 = columnOffset;
                        } else if (seite.equals("Seite2")) {
                            activeGridPane = Gridpane2;
                            rowOffset = rowOffset2;
                            columnOffset = columnOffset2;
                            drawGriffOnGrid(griff, noteName);
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
                        String noteName = parts[1];
                        boolean[] griff = model.getGriff(noteName);

                        if (seite.equals("Seite1")) {
                            activeGridPane = Gridpane1;
                            rowOffset = rowOffset1;
                            columnOffset = columnOffset1;

                            drawGriffOnGrid(griff, noteName);

                            rowOffset1 = rowOffset;
                            columnOffset1 = columnOffset;
                        } else if (seite.equals("Seite2")) {
                            activeGridPane = Gridpane2;
                            rowOffset = rowOffset2;
                            columnOffset = columnOffset2;

                            drawGriffOnGrid(griff, noteName);

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
                Map.entry(D_Oktav, "D_Oktav.jpg"),
                Map.entry(Dis_Es_Oktav, "Dis_Es_Oktav.jpg"),
                Map.entry(E_Oktav, "E_Oktav.jpg"),
                Map.entry(F_Oktav, "F_Oktav.jpg"),
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
                Map.entry(G1, "G1.jpg"),
                Map.entry(Gis_As1, "Gis_As1.jpg"),
                Map.entry(A1, "A1.jpg")
        );

        final double ICON_MAX_WIDTH = 75;
        final double ICON_MAX_HEIGHT = 75;

        for (Map.Entry<Button, String> e : mapping.entrySet()) {
            Button btn = e.getKey();
            String fileName = e.getValue();

            if (btn == null) continue;

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
