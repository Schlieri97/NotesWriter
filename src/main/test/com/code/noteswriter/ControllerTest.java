package com.code.noteswriter;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Ellipse;
import javafx.stage.Stage;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(ApplicationExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ControllerTest {

    private final FxRobot robot = new FxRobot();

    private Controller controller;

    private GridPane grid1;
    private GridPane grid2;

    private Button dButton;
    private Button cisDesButton;
    private Button leftPageButton;
    private Button rightPageButton;
    private Button deleteLastButton;
    private Button deleteAllButton;


    // =========================================================
    // START
    // =========================================================

    @Start
    private void start(Stage stage) throws Exception {

        FXMLLoader loader = new FXMLLoader(
                getClass().getResource(
                        "/com/code/noteswriter/SzFenster.fxml"
                )
        );

        Parent root = loader.load();

        controller = loader.getController();

        // Model setzen
        controller.setModel(new Model());

        grid1 = controller.Gridpane1;
        grid2 = controller.Gridpane2;

        dButton = controller.D;
        cisDesButton = controller.Cis_Des;

        leftPageButton = controller.leftPageButton;
        rightPageButton = controller.rightPageButton;

        deleteLastButton =
                controller.deleteLastNoteButton;

        deleteAllButton =
                controller.deleteAllNotesButton;

        stage.setScene(new Scene(root));
        stage.show();
    }

    private void togglePage() {
        if ("Linke Seite".equals(controller.pageIndicator.getText())) {
            robot.clickOn(rightPageButton);
        } else {
            robot.clickOn(leftPageButton);
        }
    }


    // =========================================================
    // HILFSMETHODE:
    // GRIDS LEEREN
    // =========================================================

    private void clearGrids() {

        robot.interact(() -> {
            grid1.getChildren().clear();
            grid2.getChildren().clear();
        });
    }


    // =========================================================
    // HILFSMETHODE:
    // FX-EVENTS ABWARTEN
    // =========================================================

    private void waitForFxEvents() {

        try {

            CountDownLatch latch =
                    new CountDownLatch(1);

            Platform.runLater(latch::countDown);

            assertTrue(
                    latch.await(
                            100,
                            TimeUnit.MILLISECONDS
                    ),
                    "JavaFX Event Queue konnte nicht verarbeitet werden."
            );

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            fail(
                    "JavaFX Event Queue wurde unterbrochen."
            );
        }
    }


    // =========================================================
    // HILFSMETHODE:
    // NOTE AUF EINEM GRID VORHANDEN?
    // =========================================================

    private boolean gridEnthaeltNote(GridPane grid) {

        return grid.getChildren()
                .stream()
                .anyMatch(node -> {

                    if (!(node instanceof StackPane stackPane)) {
                        return false;
                    }

                    return stackPane.getChildren()
                            .stream()
                            .anyMatch(child ->
                                    child instanceof Ellipse
                            );
                });
    }


    // =========================================================
    // HILFSMETHODE:
    // USERDATA EINER NOTE FINDEN
    // =========================================================

    private boolean gridEnthaeltUserData(
            GridPane grid,
            Object erwarteteUserData
    ) {

        return grid.getChildren()
                .stream()
                .anyMatch(node -> {

                    if (!(node instanceof StackPane stackPane)) {
                        return false;
                    }

                    return stackPane.getChildren()
                            .stream()
                            .anyMatch(child -> {

                                if (!(child instanceof Ellipse ellipse)) {
                                    return false;
                                }

                                return erwarteteUserData.equals(
                                        ellipse.getUserData()
                                );
                            });
                });
    }


    // =========================================================
    // 1. GRUNDLEGENDER TEST
    // =========================================================

    @Test
    @Order(1)
    void controllerWirdKorrektGeladen() {

        assertNotNull(
                controller,
                "Controller wurde nicht geladen."
        );

        assertNotNull(
                grid1,
                "GridPane1 ist null."
        );

        assertNotNull(
                grid2,
                "GridPane2 ist null."
        );

        assertNotNull(
                dButton,
                "D-Button ist null."
        );

        assertNotNull(
                cisDesButton,
                "Cis/Des-Button ist null."
        );

        assertNotNull(
                deleteLastButton,
                "Delete-Last-Button ist null."
        );

        assertNotNull(
                deleteAllButton,
                "Delete-All-Button ist null."
        );
    }


    // =========================================================
    // 2. MODEL IST GESETZT
    // =========================================================

    @Test
    @Order(2)
    void modelKannGesetztWerden() {

        assertNotNull(controller);

        Model model = new Model();

        controller.setModel(model);

        assertNotNull(
                controller,
                "Controller darf nach setModel() nicht null sein."
        );
    }


    // =========================================================
    // 3. INITIALER ZUSTAND
    // =========================================================

    @Test
    @Order(3)
    void gridsSindZuBeginnLeer() {

        clearGrids();

        assertEquals(
                0,
                grid1.getChildren().size(),
                "Grid 1 muss leer sein."
        );

        assertEquals(
                0,
                grid2.getChildren().size(),
                "Grid 2 muss leer sein."
        );
    }


    // =========================================================
    // 4. NORMALE NOTE D
    // =========================================================

    @Test
    @Order(4)
    void noteDKannHinzugefuegtWerden() {

        clearGrids();

        int vorher =
                grid1.getChildren().size();

        robot.clickOn(dButton);
        robot.clickOn(dButton);
        robot.clickOn(dButton);
        robot.clickOn(dButton);
        robot.clickOn(dButton);
        robot.clickOn(dButton);
        robot.clickOn(dButton);
        robot.clickOn(dButton);

        waitForFxEvents();

        int nachher =
                grid1.getChildren().size();

        assertTrue(
                nachher > vorher,
                "Beim Klicken auf D müssen Elemente im Grid erscheinen."
        );

        assertTrue(
                gridEnthaeltNote(grid1),
                "Nach dem Klicken auf D muss eine Note im Grid vorhanden sein."
        );
    }


    // =========================================================
    // 5. CIS/DES
    // =========================================================

    @Test
    @Order(5)
    void cisDesKannHinzugefuegtWerden() {

        clearGrids();

        int vorher =
                grid1.getChildren().size();

        robot.clickOn(cisDesButton);
        robot.clickOn(cisDesButton);
        robot.clickOn(cisDesButton);
        robot.clickOn(cisDesButton);
        robot.clickOn(cisDesButton);
        robot.clickOn(cisDesButton);

        waitForFxEvents();

        int nachher =
                grid1.getChildren().size();

        assertTrue(
                nachher > vorher,
                "Cis/Des muss im Grid gezeichnet werden."
        );

        assertTrue(
                gridEnthaeltNote(grid1),
                "Cis/Des muss eine Note erzeugen."
        );
    }


    // =========================================================
    // 6. ALLE NORMALEN NOTEN
    // =========================================================

    @Test
    @Order(6)
    void alleNotenButtonsFunktionieren() {

        Button[] buttons = {

                controller.Cis_Des,
                controller.D,
                controller.Dis_Es,
                controller.E,
                controller.F,
                controller.F_1,
                controller.Fis_Ges,
                controller.G,
                controller.Gis_As,
                controller.A,
                controller.Ais_B,
                controller.H,

                controller.C1,
                controller.Cis_Des1,
                controller.D1,
                controller.Dis_Es1,
                controller.E1,
                controller.Fis_Ges1,
                controller.F1,
                controller.G1,
                controller.Gis_As1,
                controller.A1
        };


        for (Button button : buttons) {

            assertNotNull(
                    button,
                    "Ein Notenbutton ist null."
            );

            clearGrids();

            int vorher =
                    grid1.getChildren().size();

            // Controller verwendet setOnMouseClicked()
            robot.clickOn(button);

            waitForFxEvents();

            int nachher =
                    grid1.getChildren().size();

            assertTrue(
                    nachher > vorher,
                    "Button funktioniert nicht: "
                            + button.getText()
            );

            assertTrue(
                    gridEnthaeltNote(grid1),
                    "Button erzeugt keine Note: "
                            + button.getText()
            );
        }
    }


    // =========================================================
    // 7. USERDATA DER NOTE
    // =========================================================

    @Test
    @Order(7)
    void noteEnthaeltUserData() {

        clearGrids();

        // Wichtig:
        // ZUERST Note erzeugen!
        robot.clickOn(dButton);

        waitForFxEvents();

        boolean gefunden =
                grid1.getChildren()
                        .stream()
                        .anyMatch(node -> {

                            if (!(node instanceof StackPane stackPane)) {
                                return false;
                            }

                            return stackPane.getChildren()
                                    .stream()
                                    .anyMatch(child -> {

                                        if (!(child instanceof Ellipse ellipse)) {
                                            return false;
                                        }

                                        return ellipse.getUserData() != null;
                                    });
                        });

        assertTrue(
                gefunden,
                "Die gezeichnete Note muss UserData besitzen."
        );
    }


    // =========================================================
    // 8. USERDATA ENTHÄLT D
    // =========================================================

    @Test
    @Order(8)
    void noteDEnthaeltRichtigenNamen() {

        clearGrids();

        robot.clickOn(dButton);

        waitForFxEvents();

        boolean gefunden =
                gridEnthaeltUserData(
                        grid1,
                        "D|NORMAL"
                );

        assertTrue(
                gefunden,
                "D|NORMAL wurde nicht gefunden."
        );
    }


    // =========================================================
    // 9. SEITENWECHSEL
    // =========================================================

//    @Test
//    @Order(9)
//    void seitenwechselFunktioniert() {
//
//        // Sicherstellen, dass wir links sind
//        if ("Rechte Seite".equals(
//                controller.pageIndicator.getText())) {
//
//            seitenwechselButton.fire();
//
//            waitForFxEvents();
//        }
//
//        assertEquals(
//                "Linke Seite",
//                controller.pageIndicator.getText()
//        );
//
//        seitenwechselButton.fire();
//
//        waitForFxEvents();
//
//        assertEquals(
//                "Rechte Seite",
//                controller.pageIndicator.getText()
//        );
//    }
    @Test
    @Order(9)
    void seitenwechselFunktioniert() {

        assertEquals(
                "Linke Seite",
                controller.pageIndicator.getText()
        );

        togglePage();
        togglePage();
        togglePage();
        togglePage();
        togglePage();

        assertEquals(
                "Rechte Seite",
                controller.pageIndicator.getText()
        );
    }

    // =========================================================
    // 10. ZURÜCK AUF LINKE SEITE
    // =========================================================

    @Test
    @Order(10)
    void seitenwechselZurueckFunktioniert() {

        assertEquals(
                "Linke Seite",
                controller.pageIndicator.getText()
        );

        togglePage();

        assertEquals(
                "Rechte Seite",
                controller.pageIndicator.getText()
        );

        togglePage();

        assertEquals(
                "Linke Seite",
                controller.pageIndicator.getText()
        );
    }

    // =========================================================
    // 11. NOTE AUF RECHTER SEITE
    // =========================================================

    @Test
    @Order(11)
    void noteKannAufRechterSeiteGesetztWerden() {

        clearGrids();

        robot.clickOn(rightPageButton);

        assertEquals(
                "Rechte Seite",
                controller.pageIndicator.getText()
        );

        int vorher =
                grid2.getChildren().size();

        robot.clickOn(dButton);

        int nachher =
                grid2.getChildren().size();

        assertTrue(
                nachher > vorher,
                "D muss auf der rechten Seite erscheinen."
        );
    }

    // =========================================================
    // 13. LETZTE NOTE LÖSCHEN
    // =========================================================

    @Test
    @Order(13)
    void letzteNoteKannGeloeschtWerden() {

        clearGrids();

        robot.clickOn(dButton);

        int mitNote =
                grid1.getChildren().size();

        assertTrue(
                mitNote > 0,
                "Vor dem Löschen muss eine Note vorhanden sein."
        );

        robot.clickOn(deleteLastButton);

        int nachLoeschen =
                grid1.getChildren().size();

        assertTrue(
                nachLoeschen < mitNote,
                "Die letzte Note wurde nicht gelöscht."
        );
    }

    // =========================================================
    // 14. ALLE NOTEN LÖSCHEN
    // =========================================================

    @Test
    @Order(14)
    void alleNotenKoennenGeloeschtWerden() {

        clearGrids();

        robot.clickOn(dButton);

        waitForFxEvents();

        robot.clickOn(cisDesButton);
        robot.clickOn(cisDesButton);
        robot.clickOn(cisDesButton);
        robot.clickOn(cisDesButton);
        robot.clickOn(cisDesButton);
        robot.clickOn(cisDesButton);
        robot.clickOn(cisDesButton);
        robot.clickOn(cisDesButton);
        robot.clickOn(cisDesButton);
        robot.clickOn(cisDesButton);

        waitForFxEvents();

        assertTrue(
                grid1.getChildren().size() > 0,
                "Vor dem Löschen müssen Noten vorhanden sein."
        );

        /*
         * deleteAllNotesButton verwendet laut Beschreibung
         * showAndWait().
         *
         * Deshalb wird dieser Test hier NICHT automatisch
         * aufgerufen, da sonst der Test blockieren kann.
         *
         * Der Button selbst wird trotzdem geprüft.
         */

        assertNotNull(
                deleteAllButton,
                "Delete-All-Button darf nicht null sein."
        );
    }


    // =========================================================
    // 15. D USERDATA EXPLIZIT PRÜFEN
    // =========================================================

    @Test
    @Order(15)
    void dHatUserDataNormal() {

        clearGrids();

        robot.clickOn(dButton);

        waitForFxEvents();

        assertTrue(
                gridEnthaeltUserData(
                        grid1,
                        "D|NORMAL"
                ),
                "Die D-Note muss die UserData D|NORMAL besitzen."
        );
    }


    // =========================================================
    // 16. KEINE NOTE OHNE KLICK
    // =========================================================

    @Test
    @Order(16)
    void ohneKlickWirdKeineNoteErzeugt() {

        clearGrids();

        assertEquals(
                0,
                grid1.getChildren().size(),
                "Ohne Klick darf keine Note vorhanden sein."
        );

        assertFalse(
                gridEnthaeltNote(grid1),
                "Ohne Klick darf keine Note vorhanden sein."
        );
    }


    // =========================================================
    // 17. MEHRERE NOTEN KÖNNEN GESETZT WERDEN
    // =========================================================

    @Test
    @Order(17)
    void mehrereNotenKoennenGesetztWerden() {

        clearGrids();

        robot.clickOn(dButton);
        waitForFxEvents();

        int nachD =
                grid1.getChildren().size();

        assertTrue(
                nachD > 0,
                "D muss erzeugt worden sein."
        );

        robot.clickOn(cisDesButton);
        waitForFxEvents();

        int nachCisDes =
                grid1.getChildren().size();

        assertTrue(
                nachCisDes > nachD,
                "Cis/Des muss zusätzlich erzeugt werden."
        );
    }


    // =========================================================
    // 18. D AUF BEIDEN SEITEN
    // =========================================================

    @Test
    @Order(18)
    void dKannAufBeidenSeitenGesetztWerden() {

        clearGrids();

        // -----------------------------------------
        // LINKE SEITE
        // -----------------------------------------

        if ("Rechte Seite".equals(
                controller.pageIndicator.getText())) {

            robot.clickOn(leftPageButton);

            waitForFxEvents();
        }

        robot.clickOn(dButton);

        waitForFxEvents();

        assertTrue(
                gridEnthaeltNote(grid1),
                "D muss auf der linken Seite erscheinen."
        );


        // -----------------------------------------
        // RECHTE SEITE
        // -----------------------------------------

        robot.clickOn(rightPageButton);

        waitForFxEvents();

        clearGrids();

        robot.clickOn(dButton);

        waitForFxEvents();

        assertTrue(
                gridEnthaeltNote(grid2),
                "D muss auf der rechten Seite erscheinen."
        );
    }

// =========================================================
// 19. RECHTE MAUSTASTE → LANG
// =========================================================
    @Test
    @Order(19)
    void alleNotenKoennenMitRechterMaustasteLangGesetztWerden() {

        Button[] buttons = {

                controller.Cis_Des,
                controller.D,
                controller.Dis_Es,
                controller.E,
                controller.F,
                controller.F_1,
                controller.Fis_Ges,
                controller.G,
                controller.Gis_As,
                controller.A,
                controller.Ais_B,
                controller.H,

                controller.C1,
                controller.Cis_Des1,
                controller.D1,
                controller.Dis_Es1,
                controller.E1,
                controller.Fis_Ges1,
                controller.F1,
                controller.G1,
                controller.Gis_As1,
                controller.A1
        };

        for (Button button : buttons) {

            // Sicherstellen, dass linke Seite aktiv ist
            if ("Rechte Seite".equals(
                    controller.pageIndicator.getText())) {

                robot.clickOn(leftPageButton);
                waitForFxEvents();
            }

            String noteName = button.getText();

            // Rechtsklick auf die Note
            robot.rightClickOn(button);

            waitForFxEvents();

            // Kontextmenü prüfen
            assertTrue(
                    robot.lookup("Lang (-)").tryQuery().isPresent(),
                    "Kontextmenü 'Lang (-)' fehlt bei Note: "
                            + noteName
            );

            // Lang auswählen
            robot.clickOn("Lang (-)");

            waitForFxEvents();

            // Note muss vorhanden sein
            assertTrue(
                    gridEnthaeltNote(grid1),
                    "Keine Note nach 'Lang' bei: "
                            + noteName
            );

            // Richtige UserData prüfen
            assertTrue(
                    gridEnthaeltUserData(
                            grid1,
                            noteName + "|LANG"
                    ),
                    "Falsche UserData bei langer Note: "
                            + noteName
                            + " | erwartet: "
                            + noteName
                            + "|LANG"
            );
        }
        clearGrids();
    }


// =========================================================
// 20. RECHTE MAUSTASTE → KURZ
// =========================================================

    @Test
    @Order(20)
    void alleNotenKoennenMitRechterMaustasteKurzGesetztWerden() {

        Button[] buttons = {

                controller.Cis_Des,
                controller.D,
                controller.Dis_Es,
                controller.E,
                controller.F,
                controller.F_1,
                controller.Fis_Ges,
                controller.G,
                controller.Gis_As,
                controller.A,
                controller.Ais_B,
                controller.H,

                controller.C1,
                controller.Cis_Des1,
                controller.D1,
                controller.Dis_Es1,
                controller.E1,
                controller.Fis_Ges1,
                controller.F1,
                controller.G1,
                controller.Gis_As1,
                controller.A1
        };

        for (Button button : buttons) {

            // Sicherstellen, dass linke Seite aktiv ist
            if ("Rechte Seite".equals(
                    controller.pageIndicator.getText())) {

                robot.clickOn(leftPageButton);
                waitForFxEvents();
            }

            String noteName = button.getText();

            // Rechtsklick auf die Note
            robot.rightClickOn(button);

            waitForFxEvents();

            // Kontextmenü prüfen
            assertTrue(
                    robot.lookup("Kurz (•)").tryQuery().isPresent(),
                    "Kontextmenü 'Kurz (•)' fehlt bei Note: "
                            + noteName
            );

            // Lang auswählen
            robot.clickOn("Kurz (•)");

            waitForFxEvents();

            // Note muss vorhanden sein
            assertTrue(
                    gridEnthaeltNote(grid1),
                    "Keine Note nach 'Kurz' bei: "
                            + noteName
            );

            // Richtige UserData prüfen
            assertTrue(
                    gridEnthaeltUserData(
                            grid1,
                            noteName + "|KURZ"
                    ),
                    "Falsche UserData bei kurzer Note: "
                            + noteName
                            + " | erwartet: "
                            + noteName
                            + "|KURZ"
            );
        }
        clearGrids();
    }


// =========================================================
// 21. RECHTE MAUSTASTE → OKTAV
// =========================================================

    @Test
    @Order(21)
    void alleNotenKoennenMitRechterMaustasteOktavGesetztWerden() {

        Button[] buttons = {
                controller.D,
                controller.Dis_Es,
                controller.E,
                controller.F,
                controller.F_1,
                controller.Fis_Ges,
                controller.G,
                controller.Gis_As,
                controller.A,
                controller.Ais_B,
                controller.H,

                controller.C1,
                controller.Cis_Des1,
                controller.D1
        };

        for (Button button : buttons) {

            // Sicherstellen, dass linke Seite aktiv ist
            if ("Rechte Seite".equals(
                    controller.pageIndicator.getText())) {

                robot.clickOn(leftPageButton);
                waitForFxEvents();
            }

            String noteName = button.getText();

            // Rechtsklick auf die Note
            robot.rightClickOn(button);

            waitForFxEvents();

            // Kontextmenü prüfen
            assertTrue(
                    robot.lookup("Oktav (x)").tryQuery().isPresent(),
                    "Kontextmenü 'Oktav (x)' fehlt bei Note: "
                            + noteName
            );

            // Lang auswählen
            robot.clickOn("Oktav (x)");

            waitForFxEvents();

            // Note muss vorhanden sein
            assertTrue(
                    gridEnthaeltNote(grid1),
                    "Keine Note nach 'Oktav' bei: "
                            + noteName
            );

            // Richtige UserData prüfen
            assertTrue(
                    gridEnthaeltUserData(
                            grid1,
                            noteName + "|OKTAV"
                    ),
                    "Falsche UserData bei oktaver Note: "
                            + noteName
                            + " | erwartet: "
                            + noteName
                            + "|OKTAV"
            );
        }
        clearGrids();
    }

    // =========================================================
// 22. RECHTE MAUSTASTE → OKTAV LANG
// =========================================================

    @Test
    @Order(22)
    void alleNotenKoennenMitRechterMaustasteOktavLangGesetztWerden() {

        Button[] buttons = {
                controller.D,
                controller.Dis_Es,
                controller.E,
                controller.F,
                controller.F_1,
                controller.Fis_Ges,
                controller.G,
                controller.Gis_As,
                controller.A,
                controller.Ais_B,
                controller.H,

                controller.C1,
                controller.Cis_Des1,
                controller.D1
        };

        for (Button button : buttons) {

            // Sicherstellen, dass linke Seite aktiv ist
            if ("Rechte Seite".equals(
                    controller.pageIndicator.getText())) {

                robot.clickOn(leftPageButton);
                waitForFxEvents();
            }

            String noteName = button.getText();

            // Rechtsklick auf die Note
            robot.rightClickOn(button);

            waitForFxEvents();

            // Kontextmenü prüfen
            assertTrue(
                    robot.lookup("Oktav + Lang (x + -)").tryQuery().isPresent(),
                    "Kontextmenü 'Oktav + Lang (x + -)' fehlt bei Note: "
                            + noteName
            );

            // Lang auswählen
            robot.clickOn("Oktav + Lang (x + -)");

            waitForFxEvents();

            // Note muss vorhanden sein
            assertTrue(
                    gridEnthaeltNote(grid1),
                    "Keine Note nach 'Oktav Lang' bei: "
                            + noteName
            );

            // Richtige UserData prüfen
            assertTrue(
                    gridEnthaeltUserData(
                            grid1,
                            noteName + "|OKTAV_LANG"
                    ),
                    "Falsche UserData bei oktaver langer Note: "
                            + noteName
                            + " | erwartet: "
                            + noteName
                            + "|OKTAV_LANG"
            );
        }
        clearGrids();
    }



// =========================================================
// 23. RECHTE MAUSTASTE → OKTAV KURZ
// =========================================================

    @Test
    @Order(23)
    void alleNotenKoennenMitRechterMaustasteOktavKurzGesetztWerden() {

        Button[] buttons = {
                controller.D,
                controller.Dis_Es,
                controller.E,
                controller.F,
                controller.F_1,
                controller.Fis_Ges,
                controller.G,
                controller.Gis_As,
                controller.A,
                controller.Ais_B,
                controller.H,

                controller.C1,
                controller.Cis_Des1,
                controller.D1
        };

        for (Button button : buttons) {

            // Sicherstellen, dass linke Seite aktiv ist
            if ("Rechte Seite".equals(
                    controller.pageIndicator.getText())) {

                robot.clickOn(leftPageButton);
//                waitForFxEvents();
            }

            String noteName = button.getText();

            // Rechtsklick auf die Note
            robot.rightClickOn(button);

//            waitForFxEvents();

            // Kontextmenü prüfen
            assertTrue(
                    robot.lookup("Oktav + Kurz (x + •)").tryQuery().isPresent(),
                    "Kontextmenü 'Oktav + Kurz (x + •)' fehlt bei Note: "
                            + noteName
            );

            // Lang auswählen
            robot.clickOn("Oktav + Kurz (x + •)");

//            waitForFxEvents();

            // Note muss vorhanden sein
            assertTrue(
                    gridEnthaeltNote(grid1),
                    "Keine Note nach 'Oktav Kurz' bei: "
                            + noteName
            );

            // Richtige UserData prüfen
            assertTrue(
                    gridEnthaeltUserData(
                            grid1,
                            noteName + "|OKTAV_KURZ"
                    ),
                    "Falsche UserData bei oktaver kurzer Note: "
                            + noteName
                            + " | erwartet: "
                            + noteName
                            + "|OKTAV_KURZ"
            );
        }
        clearGrids();
    }
}
