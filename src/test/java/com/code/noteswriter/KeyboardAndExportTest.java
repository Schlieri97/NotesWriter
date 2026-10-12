package com.code.noteswriter;

import javafx.application.Platform;
import javafx.event.Event;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.geometry.BoundingBox;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Line;
import javafx.scene.shape.Path;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class KeyboardAndExportTest {
    private Controller controller;
    private Parent root;

    @BeforeAll
    static void startJavaFx() throws Exception {
        FutureTask<Void> started = new FutureTask<>(() -> {
            Platform.setImplicitExit(false);
            return null;
        });
        Platform.startup(started);
        started.get(15, TimeUnit.SECONDS);
    }

    @BeforeEach
    void loadPage() throws Exception {
        onFx(() -> {
            FXMLLoader loader = new FXMLLoader(Controller.class.getResource("SzFenster.fxml"));
            root = loader.load();
            controller = loader.getController();
            controller.setModel(new Model());
            new Scene(root, 1400, 900);
            root.applyCss();
            root.layout();
        });
    }

    @Test
    void keyboardWritesAllSevenModifiers() throws Exception {
        onFx(() -> {
            tap(KeyCode.D, false, false, false);
            tap(KeyCode.D, false, true, false);
            tap(KeyCode.D, false, false, true);
            tap(KeyCode.D, true, false, false);
            tap(KeyCode.D, true, true, false);
            tap(KeyCode.D, true, false, true);
            tap(KeyCode.D, false, true, true);
            assertEquals(List.of("D|NORMAL", "D|LANG", "D|KURZ", "D|OKTAV",
                    "D|OKTAV_LANG", "D|OKTAV_KURZ", "D|FESTE"), tokens(controller.Gridpane1));
        });
    }

    @Test
    void microphoneInsertsOnlyStableSupportedNotesOnActivePage() throws Exception {
        onFx(() -> {
            controller.acceptMicrophoneReading(new MicrophoneInput.Reading(1479.98, 79, false));
            assertTrue(tokens(controller.Gridpane1).isEmpty());
            controller.acceptMicrophoneReading(new MicrophoneInput.Reading(1479.98, 79, true));
            assertEquals(List.of("G|NORMAL"), tokens(controller.Gridpane1));
            controller.acceptMicrophoneReading(new MicrophoneInput.Reading(Double.NaN, -1, false));
            Label microphoneStatus = (Label) root.lookup("#microphoneStatus");
            assertTrue(microphoneStatus.getText().contains("1479,98 Hz"));
            assertTrue(microphoneStatus.getText().contains("Grifflage: G5"));
            controller.rightPageButton.fire();
            controller.acceptMicrophoneReading(new MicrophoneInput.Reading(1661.22, 81, true));
            controller.acceptMicrophoneReading(new MicrophoneInput.Reading(261.63, 60, true));
            assertEquals(List.of("A|NORMAL"), tokens(controller.Gridpane2));
            assertEquals(List.of("G|NORMAL"), tokens(controller.Gridpane1));
        });
    }

    @Test
    void microphoneDistinguishesBasicOctaveAndHighA() throws Exception {
        onFx(() -> {
            controller.acceptMicrophoneReading(new MicrophoneInput.Reading(830.609, 69, true));
            controller.acceptMicrophoneReading(new MicrophoneInput.Reading(1661.219, 81, true));
            controller.acceptMicrophoneReading(new MicrophoneInput.Reading(3322.438, 93, true));
            assertEquals(List.of("A|OKTAV", "A|NORMAL", "A1|NORMAL"), tokens(controller.Gridpane1));
        });
    }

    @Test
    void everyAvailablePitchHasAKey() throws Exception {
        onFx(() -> {
            KeyCode[] keys = {KeyCode.S, KeyCode.D, KeyCode.R, KeyCode.E, KeyCode.F, KeyCode.V,
                    KeyCode.T, KeyCode.G, KeyCode.Z, KeyCode.A, KeyCode.B, KeyCode.H,
                    KeyCode.DIGIT1, KeyCode.DIGIT2, KeyCode.DIGIT3, KeyCode.DIGIT4, KeyCode.DIGIT5,
                    KeyCode.DIGIT6, KeyCode.DIGIT7, KeyCode.DIGIT8, KeyCode.DIGIT9, KeyCode.DIGIT0};
            for (KeyCode key : keys) tap(key);
            assertEquals(List.of("Cis_Des|NORMAL", "D|NORMAL", "Dis_Es|NORMAL", "E|NORMAL",
                    "F|NORMAL", "F_1|NORMAL", "Fis_Ges|NORMAL", "G|NORMAL", "Gis_As|NORMAL",
                    "A|NORMAL", "Ais_B|NORMAL", "H|NORMAL", "C1|NORMAL", "Cis_Des1|NORMAL",
                    "D1|NORMAL", "Dis_Es1|NORMAL", "E1|NORMAL", "F1|NORMAL", "Fis_Ges1|NORMAL",
                    "G1|NORMAL", "Gis_As1|NORMAL", "A1|NORMAL"), tokens(controller.Gridpane1));
        });
    }

    @Test
    void unsupportedOctavesAreRejectedWithoutConsumingASlot() throws Exception {
        onFx(() -> {
            tap(KeyCode.S, true, false, false);
            tap(KeyCode.DIGIT5, true, true, false);
            assertTrue(tokens(controller.Gridpane1).isEmpty());
            assertTrue(((Label) root.lookup("#keyboardStatus")).getText().contains("nicht verfügbar"));
            tap(KeyCode.D);
            assertPosition(slot(controller.Gridpane1, "D|NORMAL"), 0, 1);
        });
    }

    @Test
    void holdingAKeyDoesNotCreateRepeatedNotes() throws Exception {
        onFx(() -> {
            key(root, KeyEvent.KEY_PRESSED, KeyCode.D, false, false, false);
            key(root, KeyEvent.KEY_PRESSED, KeyCode.D, false, false, false);
            assertEquals(1, tokens(controller.Gridpane1).size());
            key(root, KeyEvent.KEY_RELEASED, KeyCode.D, false, false, false);
            tap(KeyCode.D);
            assertEquals(2, tokens(controller.Gridpane1).size());
        });
    }

    @Test
    void textFieldsKeepNormalTypingAndDeletion() throws Exception {
        onFx(() -> {
            tap(KeyCode.D);
            for (String id : List.of("leftCircleText", "rightCircleText", "leftMarchText", "rightMarchText", "freeTextInput")) {
                TextField field = (TextField) root.lookup("#" + id);
                field.setText("Marsch");
                field.positionCaret(field.getLength());
                for (KeyCode code : List.of(KeyCode.D, KeyCode.BACK_SPACE, KeyCode.DELETE, KeyCode.ENTER)) {
                    key(field, KeyEvent.KEY_PRESSED, code, false, false, false);
                    key(field, KeyEvent.KEY_RELEASED, code, false, false, false);
                }
                assertEquals("Marsc", field.getText());
            }
            assertEquals(List.of("D|NORMAL"), tokens(controller.Gridpane1));
        });
    }

    @Test
    void selectedOlderNotesAreDeletedByBackspaceAndDelete() throws Exception {
        onFx(() -> {
            tap(KeyCode.D);
            tap(KeyCode.E);
            call("selectNote", slot(controller.Gridpane1, "D|NORMAL"), controller.Gridpane1);
            tap(KeyCode.G);
            assertEquals(List.of("D|NORMAL", "E|NORMAL", "G|NORMAL"), tokens(controller.Gridpane1));
            call("selectNote", slot(controller.Gridpane1, "D|NORMAL"), controller.Gridpane1);
            tap(KeyCode.BACK_SPACE);
            assertEquals(List.of("E|NORMAL", "G|NORMAL"), tokens(controller.Gridpane1));
            assertPosition(slot(controller.Gridpane1, "E|NORMAL"), 0, 1);
            call("selectNote", slot(controller.Gridpane1, "E|NORMAL"), controller.Gridpane1);
            tap(KeyCode.DELETE);
            assertEquals(List.of("G|NORMAL"), tokens(controller.Gridpane1));
        });
    }

    @Test
    void holdingDeleteKeysRemovesSeveralNotes() throws Exception {
        onFx(() -> {
            for (int i = 0; i < 5; i++) tap(KeyCode.D);
            key(root, KeyEvent.KEY_PRESSED, KeyCode.BACK_SPACE, false, false, false);
            key(root, KeyEvent.KEY_PRESSED, KeyCode.BACK_SPACE, false, false, false);
            key(root, KeyEvent.KEY_PRESSED, KeyCode.BACK_SPACE, false, false, false);
            key(root, KeyEvent.KEY_RELEASED, KeyCode.BACK_SPACE, false, false, false);
            assertEquals(2, tokens(controller.Gridpane1).size());

            key(root, KeyEvent.KEY_PRESSED, KeyCode.DELETE, false, false, false);
            key(root, KeyEvent.KEY_PRESSED, KeyCode.DELETE, false, false, false);
            key(root, KeyEvent.KEY_RELEASED, KeyCode.DELETE, false, false, false);
            assertTrue(tokens(controller.Gridpane1).isEmpty());
        });
    }

    @Test
    void deleteAndBackspaceRemoveSelectedPauseAndRepeat() throws Exception {
        onFx(() -> {
            Pane pauseOverlay = (Pane) root.lookup("#pauseOverlay");
            tap(KeyCode.D);
            controller.pauseStrokeButton.fire();
            assertEquals(1, pauseOverlay.getChildren().size());
            mouseClick(pauseOverlay.getChildren().get(0));
            tap(KeyCode.BACK_SPACE);
            assertTrue(pauseOverlay.getChildren().isEmpty());
            assertEquals(List.of("D|NORMAL"), tokens(controller.Gridpane1));

            call("appendSerializedToken", "WIEDERHOLUNG|EINZELSTRICH");
            StackPane repeat = slot(controller.Gridpane1, "WIEDERHOLUNG|EINZELSTRICH");
            mouseClick(repeat);
            tap(KeyCode.DELETE);
            assertEquals(List.of("D|NORMAL"), tokens(controller.Gridpane1));
        });
    }

    @Test
    void selectedFreeTextIsDeletedByBackspaceAndDeleteWithoutRemovingNotes() throws Exception {
        onFx(() -> {
            tap(KeyCode.D);
            TextField input = (TextField) root.lookup("#freeTextInput");
            Pane overlay = (Pane) root.lookup("#annotationOverlay");
            for (KeyCode key : List.of(KeyCode.BACK_SPACE, KeyCode.DELETE)) {
                input.setText("Marschname");
                controller.addFreeTextButton.fire();
                Text text = (Text) overlay.getChildren().stream()
                        .filter(Text.class::isInstance).findFirst().orElseThrow();
                mouseClick(text);
                assertEquals(Color.web("#1976D2"), text.getFill());
                tap(key);
                assertFalse(overlay.getChildren().contains(text));
                assertEquals(List.of("D|NORMAL"), tokens(controller.Gridpane1));
            }
        });
    }

    @Test
    void selectionRectangleAlsoSelectsFreeText() throws Exception {
        onFx(() -> {
            TextField input = (TextField) root.lookup("#freeTextInput");
            Pane overlay = (Pane) root.lookup("#annotationOverlay");
            Pane page = (Pane) root.lookup("#pageCanvas");
            for (String value : List.of("Erster Text", "Zweiter Text")) {
                input.setText(value);
                controller.addFreeTextButton.fire();
            }
            List<Text> texts = overlay.getChildren().stream().filter(Text.class::isInstance)
                    .map(Text.class::cast).toList();
            texts.get(0).setLayoutX(100);
            texts.get(0).setLayoutY(150);
            texts.get(1).setLayoutX(220);
            texts.get(1).setLayoutY(150);
            root.layout();
            Bounds first = page.sceneToLocal(texts.get(0).localToScene(texts.get(0).getBoundsInLocal()));
            Bounds second = page.sceneToLocal(texts.get(1).localToScene(texts.get(1).getBoundsInLocal()));
            double minX = Math.min(first.getMinX(), second.getMinX()) - 5;
            double minY = Math.min(first.getMinY(), second.getMinY()) - 5;
            double maxX = Math.max(first.getMaxX(), second.getMaxX()) + 5;
            double maxY = Math.max(first.getMaxY(), second.getMaxY()) + 5;
            var sceneStart = page.localToScene(minX, minY);
            var sceneEnd = page.localToScene(maxX, maxY);
            Event.fireEvent(page, new MouseEvent(MouseEvent.MOUSE_PRESSED,
                    sceneStart.getX(), sceneStart.getY(), sceneStart.getX(), sceneStart.getY(),
                    MouseButton.PRIMARY, 1, false, false, false, false,
                    true, false, false, false, false, true,
                    new PickResult(page, minX, minY)));
            var startField = Controller.class.getDeclaredField("selectionStart");
            startField.setAccessible(true);
            assertNotNull(startField.get(controller));
            Event.fireEvent(page, new MouseEvent(MouseEvent.MOUSE_DRAGGED,
                    sceneEnd.getX(), sceneEnd.getY(), sceneEnd.getX(), sceneEnd.getY(),
                    MouseButton.PRIMARY, 1, false, false, false, false,
                    true, false, false, false, false, true,
                    new PickResult(page, maxX, maxY)));
            var rectangleField = Controller.class.getDeclaredField("selectionRectangle");
            rectangleField.setAccessible(true);
            assertTrue(((javafx.scene.shape.Rectangle) rectangleField.get(controller)).isVisible());
            Event.fireEvent(page, new MouseEvent(MouseEvent.MOUSE_RELEASED,
                    sceneEnd.getX(), sceneEnd.getY(), sceneEnd.getX(), sceneEnd.getY(),
                    MouseButton.PRIMARY, 1, false, false, false, false,
                    false, false, false, false, false, true,
                    new PickResult(page, maxX, maxY)));
            assertEquals(Color.web("#1976D2"), texts.get(0).getFill());
            assertEquals(Color.web("#1976D2"), texts.get(1).getFill());
            tap(KeyCode.DELETE);
            assertTrue(overlay.getChildren().stream().noneMatch(Text.class::isInstance));
            tap(KeyCode.Z, false, true, false);
            assertEquals(2, overlay.getChildren().stream().filter(Text.class::isInstance).count());
        });
    }

    @Test
    void editedFreeTextKeepsSizeAndCanBeUndone() throws Exception {
        onFx(() -> {
            ((TextField) root.lookup("#freeTextInput")).setText("Alt");
            controller.addFreeTextButton.fire();
            Pane overlay = (Pane) root.lookup("#annotationOverlay");
            Text text = (Text) overlay.getChildren().stream()
                    .filter(Text.class::isInstance).findFirst().orElseThrow();
            assertNotNull(text.getOnMouseClicked());
            call("setTextAnnotationSize", text, 30);
            call("setTextAnnotationText", text, "Neu");
            assertEquals("Neu", text.getText());
            assertEquals(30, text.getFont().getSize());
            StringWriter data = new StringWriter();
            BufferedWriter writer = new BufferedWriter(data);
            call("writeAnnotations", writer, "Seite1", controller.Gridpane1);
            writer.flush();
            assertTrue(data.toString().contains("TEXT|TmV1|"));
            tap(KeyCode.Z, false, true, false);
            Text restored = (Text) overlay.getChildren().stream()
                    .filter(Text.class::isInstance).findFirst().orElseThrow();
            assertEquals("Alt", restored.getText());
            assertEquals(30, restored.getFont().getSize());
        });
    }

    @Test
    void textSizeSurvivesSerializationAndUndo() throws Exception {
        onFx(() -> {
            ((TextField) root.lookup("#freeTextInput")).setText("Marschname");
            controller.addFreeTextButton.fire();
            Pane overlay = (Pane) root.lookup("#annotationOverlay");
            Text text = (Text) overlay.getChildren().stream()
                    .filter(Text.class::isInstance).findFirst().orElseThrow();
            call("setTextAnnotationSize", text, 30);
            assertEquals(30, text.getFont().getSize());
            StringWriter data = new StringWriter();
            BufferedWriter writer = new BufferedWriter(data);
            call("writeAnnotations", writer, "Seite1", controller.Gridpane1);
            writer.flush();
            assertTrue(data.toString().trim().endsWith("|30"));
            tap(KeyCode.Z, false, true, false);
            Text restored = (Text) overlay.getChildren().stream()
                    .filter(Text.class::isInstance).findFirst().orElseThrow();
            assertEquals(18, restored.getFont().getSize());
            tap(KeyCode.Z, true, true, false);
            Text redone = (Text) overlay.getChildren().stream()
                    .filter(Text.class::isInstance).findFirst().orElseThrow();
            assertEquals(30, redone.getFont().getSize());
        });
    }

    @Test
    void oldTextFormatLoadsCorrectly() throws Exception {
        onFx(() -> {
            call("restoreSpecialToken", "TEXT|VGVzdA==|100|120");
            call("restoreSpecialToken", "TEXT|TmV1|160|120|30");
            Pane overlay = (Pane) root.lookup("#annotationOverlay");
            List<Text> texts = overlay.getChildren().stream()
                    .filter(Text.class::isInstance).map(Text.class::cast).toList();
            assertEquals(List.of(18.0, 30.0), texts.stream()
                    .map(text -> text.getFont().getSize()).toList());
        });
    }

    @Test
    void escapeClearsEverySelectionWithoutDeletingElements() throws Exception {
        onFx(() -> {
            Pane pauseOverlay = (Pane) root.lookup("#pauseOverlay");
            Pane annotationOverlay = (Pane) root.lookup("#annotationOverlay");
            tap(KeyCode.D);
            tap(KeyCode.E);
            StackPane note = slot(controller.Gridpane1, "D|NORMAL");
            mouseClick(note);
            tap(KeyCode.ESCAPE);
            assertNull(note.getEffect());

            controller.pauseStrokeButton.fire();
            Line pause = (Line) pauseOverlay.getChildren().get(0);
            mouseClick(pause);
            tap(KeyCode.ESCAPE);
            assertEquals(Color.BLACK, pause.getStroke());
            assertTrue(pauseOverlay.getChildren().contains(pause));

            call("appendSerializedToken", "WIEDERHOLUNG|EINZELSTRICH");
            StackPane repeat = slot(controller.Gridpane1, "WIEDERHOLUNG|EINZELSTRICH");
            mouseClick(repeat);
            tap(KeyCode.ESCAPE);
            assertNull(repeat.getEffect());

            ((TextField) root.lookup("#freeTextInput")).setText("Titel");
            controller.addFreeTextButton.fire();
            Text text = (Text) annotationOverlay.getChildren().stream()
                    .filter(Text.class::isInstance).findFirst().orElseThrow();
            mouseClick(text);
            tap(KeyCode.ESCAPE);
            assertEquals(Color.BLACK, text.getFill());
            assertTrue(annotationOverlay.getChildren().contains(text));

            call("setSelectedNotes", List.of(note, slot(controller.Gridpane1, "E|NORMAL")));
            call("handleAddSlurAction", (Object) null);
            Path slur = (Path) annotationOverlay.getChildren().stream()
                    .filter(Path.class::isInstance).findFirst().orElseThrow();
            mouseClick(slur);
            tap(KeyCode.ESCAPE);
            assertEquals(Color.BLACK, slur.getStroke());
            assertEquals(List.of("D|NORMAL", "E|NORMAL", "WIEDERHOLUNG|EINZELSTRICH"),
                    tokens(controller.Gridpane1));
        });
    }

    @Test
    void freePauseKeepsItsPositionAcrossCompactionAndSerialization() throws Exception {
        onFx(() -> {
            Pane pauseOverlay = (Pane) root.lookup("#pauseOverlay");
            tap(KeyCode.D);
            tap(KeyCode.E);
            controller.pauseStrokeButton.fire();
            Line pause = (Line) pauseOverlay.getChildren().get(0);
            pause.setLayoutX(350);
            pause.setLayoutY(210);
            var markersField = Controller.class.getDeclaredField("pauseMarkers");
            markersField.setAccessible(true);
            Object marker = ((List<?>) markersField.get(controller)).get(0);
            var freeField = marker.getClass().getDeclaredField("free");
            freeField.setAccessible(true);
            freeField.setBoolean(marker, true);

            call("selectNote", slot(controller.Gridpane1, "D|NORMAL"), controller.Gridpane1);
            tap(KeyCode.DELETE);
            assertEquals(1, pauseOverlay.getChildren().size());
            assertEquals(350, pause.getLayoutX());
            assertEquals(210, pause.getLayoutY());

            StringWriter data = new StringWriter();
            BufferedWriter writer = new BufferedWriter(data);
            call("writeAnnotations", writer, "Seite1", controller.Gridpane1);
            writer.flush();
            assertTrue(data.toString().contains("Seite1;PAUSE_FREI|350.0|210.0"));
            call("restoreSpecialToken", "PAUSE_FREI|350.0|210.0");
            assertEquals(2, pauseOverlay.getChildren().size());
            assertEquals(350, ((Line) pauseOverlay.getChildren().get(1)).getLayoutX());
        });
    }

    @Test
    void pauseSnapsToAnExistingNoteAndSerializesAtThatSlot() throws Exception {
        onFx(() -> {
            Pane overlay = (Pane) root.lookup("#pauseOverlay");
            tap(KeyCode.D);
            tap(KeyCode.E);
            controller.pauseStrokeButton.fire();
            Line line = (Line) overlay.getChildren().get(0);
            StackPane first = slot(controller.Gridpane1, "D|NORMAL");
            var bounds = overlay.sceneToLocal(first.localToScene(first.getBoundsInLocal()));
            line.setLayoutX(bounds.getMinX() - controller.Gridpane1.getHgap() / 2.0);
            line.setLayoutY(bounds.getMinY() - 6);
            var markersField = Controller.class.getDeclaredField("pauseMarkers");
            markersField.setAccessible(true);
            Object marker = ((List<?>) markersField.get(controller)).get(0);
            assertTrue((boolean) call("snapPauseMarkerToNearestNote", marker));
            var column = marker.getClass().getDeclaredField("column");
            column.setAccessible(true);
            assertEquals(0, column.getInt(marker));
            StringWriter data = new StringWriter();
            BufferedWriter writer = new BufferedWriter(data);
            call("writeGridTokens", writer, "Seite1", controller.Gridpane1);
            writer.flush();
            assertEquals(List.of("Seite1;PAUSENSTRICH", "Seite1;D|NORMAL", "Seite1;E|NORMAL"),
                    data.toString().lines().toList());
        });
    }

    @Test
    void selectionMenuClosesOnOutsideClickAndPauseMenuOffersFreePlacement() throws Exception {
        onFx(() -> {
            Stage stage = new Stage();
            stage.setScene(root.getScene());
            stage.show();
            try {
                tap(KeyCode.D);
                call("selectNote", slot(controller.Gridpane1, "D|NORMAL"), controller.Gridpane1);
                call("showSelectionContextMenu", 100.0, 100.0);
                var menuField = Controller.class.getDeclaredField("activeContextMenu");
                menuField.setAccessible(true);
                ContextMenu menu = (ContextMenu) menuField.get(controller);
                assertTrue(menu.isShowing());
                mouseClick(root);
                assertFalse(menu.isShowing());

                StackPane selected = slot(controller.Gridpane1, "D|NORMAL");
                call("selectNote", selected, controller.Gridpane1);
                call("showSelectionContextMenu", 100.0, 100.0);
                ContextMenu escapeMenu = (ContextMenu) menuField.get(controller);
                assertTrue(escapeMenu.isShowing());
                Event.fireEvent(escapeMenu, new KeyEvent(KeyEvent.KEY_PRESSED, "", "",
                        KeyCode.ESCAPE, false, false, false, false));
                assertFalse(escapeMenu.isShowing());
                assertNull(selected.getEffect());

                controller.pauseStrokeButton.fire();
                Line line = (Line) ((Pane) root.lookup("#pauseOverlay")).getChildren().get(0);
                mouseClick(line, MouseButton.SECONDARY);
                ContextMenu pauseMenu = (ContextMenu) menuField.get(controller);
                assertEquals("Pausenstrich frei platzieren", pauseMenu.getItems().get(0).getText());
                pauseMenu.getItems().get(0).fire();
                var markersField = Controller.class.getDeclaredField("pauseMarkers");
                markersField.setAccessible(true);
                Object marker = ((List<?>) markersField.get(controller)).get(0);
                var freeField = marker.getClass().getDeclaredField("free");
                freeField.setAccessible(true);
                assertTrue(freeField.getBoolean(marker));
            } finally {
                stage.close();
            }
        });
    }

    @Test
    void typingAndDeletionOnlyAffectTheActivePage() throws Exception {
        onFx(() -> {
            tap(KeyCode.D);
            controller.rightPageButton.fire();
            tap(KeyCode.E);
            tap(KeyCode.DELETE);
            assertTrue(tokens(controller.Gridpane2).isEmpty());
            assertEquals(List.of("D|NORMAL"), tokens(controller.Gridpane1));
            tap(KeyCode.F);
            controller.leftPageButton.fire();
            tap(KeyCode.G);
            assertPosition(slot(controller.Gridpane1, "G|NORMAL"), 1, 1);
            assertPosition(slot(controller.Gridpane2, "F|NORMAL"), 0, 1);
        });
    }

    @Test
    void deletionOnAnEmptyPageKeepsTheFirstRowAndCrossesRowBoundaries() throws Exception {
        onFx(() -> {
            tap(KeyCode.BACK_SPACE);
            tap(KeyCode.DELETE);
            for (int i = 0; i < 41; i++) tap(KeyCode.D);
            tap(KeyCode.E);
            assertPosition(slot(controller.Gridpane1, "E|NORMAL"), 0, 18);
            tap(KeyCode.BACK_SPACE);
            tap(KeyCode.DELETE);
            tap(KeyCode.F);
            assertPosition(slot(controller.Gridpane1, "F|NORMAL"), 40, 1);
        });
    }

    @Test
    void blankBlockClosesCurrentRowAndLeavesOneWholeRowEmpty() throws Exception {
        onFx(() -> {
            tap(KeyCode.D);
            tap(KeyCode.ENTER);
            tap(KeyCode.E);
            assertEquals(List.of("D|NORMAL", "LEERBLOCK", "E|NORMAL"), tokens(controller.Gridpane1));
            assertPosition(slot(controller.Gridpane1, "E|NORMAL"), 0, 35);
            assertEquals(41, controller.Gridpane1.getChildren().stream()
                    .filter(node -> Integer.valueOf(18).equals(GridPane.getRowIndex(node))).count());
            root.layout();
            assertTrue(slot(controller.Gridpane1, "E|NORMAL").getBoundsInParent().getMinY()
                    > slot(controller.Gridpane1, "D|NORMAL").getBoundsInParent().getMinY() + 174);
        });
    }

    @Test
    void blankBlockAtRowStartSkipsExactlyOneRowAndCanBeUndone() throws Exception {
        onFx(() -> {
            controller.emptyBlockButton.fire();
            tap(KeyCode.E);
            assertPosition(slot(controller.Gridpane1, "E|NORMAL"), 0, 18);
            tap(KeyCode.BACK_SPACE);
            tap(KeyCode.BACK_SPACE);
            assertTrue(controller.Gridpane1.getChildren().isEmpty());
            tap(KeyCode.D);
            assertPosition(slot(controller.Gridpane1, "D|NORMAL"), 0, 1);
            tap(KeyCode.ENTER);
            tap(KeyCode.DELETE);
            tap(KeyCode.F);
            assertPosition(slot(controller.Gridpane1, "F|NORMAL"), 1, 1);
        });
    }

    @Test
    void blankBlocksSurviveSerializationAndAppendingOnBothPages() throws Exception {
        onFx(() -> {
            for (GridPane grid : List.of(controller.Gridpane1, controller.Gridpane2)) {
                call("selectPage", grid);
                tap(KeyCode.D);
                tap(KeyCode.ENTER);
                tap(KeyCode.E);
                StringWriter data = new StringWriter();
                BufferedWriter writer = new BufferedWriter(data);
                call("writeGridTokens", writer, "Seite1", grid);
                writer.flush();
                assertEquals(List.of("Seite1;D|NORMAL", "Seite1;LEERBLOCK", "Seite1;E|NORMAL"), data.toString().lines().toList());
                for (String line : data.toString().lines().toList()) {
                    call("appendSerializedToken", line.split(";", 2)[1]);
                }
                assertEquals(List.of("D|NORMAL", "LEERBLOCK", "E|NORMAL", "D|NORMAL", "LEERBLOCK", "E|NORMAL"), tokens(grid));
                assertPosition(grid.getChildren().stream().filter(node -> node instanceof StackPane && "E|NORMAL".equals(node.getUserData()))
                        .reduce((first, second) -> second).orElseThrow(), 0, 69);
            }
        });
    }

    @Test
    void deletingEarlierNotesStillPreservesAWholeBlankBlock() throws Exception {
        onFx(() -> {
            tap(KeyCode.D);
            tap(KeyCode.ENTER);
            tap(KeyCode.E);
            call("selectNote", slot(controller.Gridpane1, "D|NORMAL"), controller.Gridpane1);
            call("deleteSelectedNotesAndCompact");
            assertEquals(List.of("LEERBLOCK", "E|NORMAL"), tokens(controller.Gridpane1));
            assertPosition(slot(controller.Gridpane1, "E|NORMAL"), 0, 18);
            assertEquals(1, ((Pane) root.lookup("#emptyBlockOverlay")).getChildren().size());
        });
    }

    @Test
    void printOptionsPreserveBlankBlocksAndDoNotChangeTheDocument() throws Exception {
        onFx(() -> {
            tap(KeyCode.D);
            tap(KeyCode.ENTER);
            tap(KeyCode.E);
            for (GridPane grid : List.of(controller.Gridpane1, controller.Gridpane2)) {
                List<Node> original = new ArrayList<>(grid.getChildren());
                List<?> unchanged = (List<?>) call("prepareGridForPrint", grid, false);
                assertTrue(unchanged.isEmpty());
                assertEquals(original, grid.getChildren());
                List<?> filled = (List<?>) call("prepareGridForPrint", grid, true);
                int expectedSlots = grid == controller.Gridpane1 ? 287 - 1 - 40 - 41 - 1 : 287;
                assertEquals(expectedSlots * 7, filled.size());
                if (grid == controller.Gridpane1) {
                    assertTrue(filled.stream().map(node -> (Node) node)
                            .allMatch(node -> GridPane.getRowIndex(node) >= 35));
                }
                grid.getChildren().removeAll(filled);
                assertEquals(original, grid.getChildren());
            }
            tap(KeyCode.F);
            assertPosition(slot(controller.Gridpane1, "F|NORMAL"), 1, 35);
        });
    }

    @Test
    void fullPageRejectsExtraNotesAndBlocksWithoutMovingCursor() throws Exception {
        onFx(() -> {
            for (int i = 0; i < 247; i++) tap(KeyCode.D);
            tap(KeyCode.ENTER);
            assertEquals(247, tokens(controller.Gridpane1).size());
            for (int i = 0; i < 40; i++) tap(KeyCode.D);
            tap(KeyCode.E);
            assertEquals(287, tokens(controller.Gridpane1).size());
            tap(KeyCode.BACK_SPACE);
            tap(KeyCode.F);
            assertPosition(slot(controller.Gridpane1, "F|NORMAL"), 40, 103);
        });
    }

    private void tap(KeyCode code) {
        tap(code, false, false, false);
    }

    @Test
    void deleteButtonsAreGoneWhileKeyboardDeletionRemainsAvailable() throws Exception {
        onFx(() -> {
            assertNull(root.lookup("#deleteLastNoteButton"));
            assertNull(root.lookup("#deleteAllNotesButton"));
            assertTrue(((Pane) root.lookup("#emptyBlockOverlay")).isMouseTransparent());
            tap(KeyCode.D);
            tap(KeyCode.BACK_SPACE);
            assertTrue(tokens(controller.Gridpane1).isEmpty());
        });
    }

    @Test
    void pageButtonsShowSelectionWithoutSeparateIndicator() throws Exception {
        onFx(() -> {
            assertNull(root.lookup("#pageIndicator"));
            assertTrue(controller.leftPageButton.getStyle().contains("#1976D2"));
            controller.rightPageButton.fire();
            assertTrue(controller.rightPageButton.getStyle().contains("#1976D2"));
            assertFalse(controller.leftPageButton.getStyle().contains("#1976D2"));
        });
    }

    @Test
    void activePageIndicatorFollowsSelectedPageAndIsHiddenForPrinting() throws Exception {
        onFx(() -> {
            Rectangle indicator = (Rectangle) root.lookup("#activePageIndicator");
            StackPane pageCanvas = (StackPane) root.lookup("#pageCanvas");

            assertTrue(indicator.isVisible());
            assertEquals(10.0, indicator.getX(), 0.01);
            assertEquals(630.0, indicator.getWidth(), 0.01);

            controller.rightPageButton.fire();
            assertTrue(indicator.isVisible());
            assertEquals(650.0, indicator.getX(), 0.01);

            WritableImage editor = pageCanvas.snapshot(null, null);
            WritableImage printed = (WritableImage) call("snapshotPageForPrint");
            assertTrue(indicator.isVisible());
            assertNotEquals(editor.getPixelReader().getColor(650, 100),
                    printed.getPixelReader().getColor(650, 100));
        });
    }

    @Test
    void formerVariableBlankTokenLoadsAsFixedBlankBlock() throws Exception {
        onFx(() -> {
            call("appendSerializedToken", "LEERBLOCK|5");
            tap(KeyCode.E);
            assertEquals(List.of("LEERBLOCK", "E|NORMAL"), tokens(controller.Gridpane1));
            assertPosition(slot(controller.Gridpane1, "E|NORMAL"), 0, 18);
        });
    }

    @Test
    void emptyBlockGuideAppearsOnBothPagesButNotInPrintSnapshot() throws Exception {
        onFx(() -> {
            Pane guides = (Pane) root.lookup("#emptyBlockOverlay");
            StackPane pageCanvas = (StackPane) root.lookup("#pageCanvas");

            tap(KeyCode.D);
            tap(KeyCode.ENTER);
            tap(KeyCode.E);
            controller.rightPageButton.fire();
            tap(KeyCode.ENTER);
            root.layout();

            assertEquals(2, guides.getChildren().size());
            Label leftGuide = (Label) guides.getChildren().get(0);
            Label rightGuide = (Label) guides.getChildren().get(1);
            assertTrue(leftGuide.getText().contains("LEERER NOTENBLOCK"));
            assertTrue(leftGuide.getText().contains("NICHT GEDRUCKT"));
            assertEquals(controller.Gridpane1.getLayoutX(), leftGuide.getLayoutX(), 0.01);
            assertEquals(controller.Gridpane2.getLayoutX(), rightGuide.getLayoutX(), 0.01);
            assertTrue(leftGuide.getLayoutY() > rightGuide.getLayoutY());

            WritableImage editor = pageCanvas.snapshot(null, null);
            WritableImage printed = (WritableImage) call("snapshotPageForPrint");
            assertTrue(guides.isVisible());
            int x = (int) leftGuide.getLayoutX() + 8;
            int y = (int) leftGuide.getLayoutY() + 8;
            assertNotEquals(editor.getPixelReader().getColor(x, y),
                    printed.getPixelReader().getColor(x, y));

            tap(KeyCode.BACK_SPACE);
            assertEquals(1, guides.getChildren().size());
            controller.leftPageButton.fire();
            tap(KeyCode.BACK_SPACE);
            tap(KeyCode.BACK_SPACE);
            assertTrue(guides.getChildren().isEmpty());
        });
    }

    @Test
    void lastRowCanBeLeftEmptyAndUndone() throws Exception {
        onFx(() -> {
            for (int i = 0; i < 246; i++) tap(KeyCode.D);
            tap(KeyCode.ENTER);
            assertEquals(247, tokens(controller.Gridpane1).size());
            assertPosition(slot(controller.Gridpane1, "LEERBLOCK"), 0, 103);
            assertEquals(41, controller.Gridpane1.getChildren().stream()
                    .filter(node -> Integer.valueOf(103).equals(GridPane.getRowIndex(node))).count());
            assertEquals(1, ((Pane) root.lookup("#emptyBlockOverlay")).getChildren().size());
            tap(KeyCode.E);
            assertEquals(247, tokens(controller.Gridpane1).size());
            tap(KeyCode.Z, false, true, false);
            assertEquals(246, tokens(controller.Gridpane1).size());
            tap(KeyCode.E);
            assertPosition(slot(controller.Gridpane1, "E|NORMAL"), 0, 103);
        });
    }

    @Test
    void blockCanFillLastRowAfterPartlyWrittenPreviousRow() throws Exception {
        onFx(() -> {
            for (int i = 0; i < 206; i++) tap(KeyCode.D);
            tap(KeyCode.ENTER);
            assertPosition(slot(controller.Gridpane1, "LEERBLOCK"), 1, 86);
            assertEquals(41, controller.Gridpane1.getChildren().stream()
                    .filter(node -> Integer.valueOf(103).equals(GridPane.getRowIndex(node))).count());
            assertEquals(1, ((Pane) root.lookup("#emptyBlockOverlay")).getChildren().size());
        });
    }

    @Test
    void undoRedoShortcutsAndButtonsRestoreNotesAndInvalidateRedoAfterNewEdit() throws Exception {
        onFx(() -> {
            var undo = (javafx.scene.control.Button) root.lookup("#undoButton");
            var redo = (javafx.scene.control.Button) root.lookup("#redoButton");
            assertTrue(undo.isDisabled());
            assertTrue(redo.isDisabled());

            tap(KeyCode.D);
            tap(KeyCode.E);
            assertFalse(undo.isDisabled());
            tap(KeyCode.Z, false, true, false);
            assertEquals(List.of("D|NORMAL"), tokens(controller.Gridpane1));
            assertFalse(redo.isDisabled());
            tap(KeyCode.Z, true, true, false);
            assertEquals(List.of("D|NORMAL", "E|NORMAL"), tokens(controller.Gridpane1));

            mouseClick(slot(controller.Gridpane1, "D|NORMAL"));
            mouseClick(controller.F);
            assertPosition(slot(controller.Gridpane1, "F|NORMAL"), 0, 1);
            assertPosition(slot(controller.Gridpane1, "E|NORMAL"), 1, 1);
            undo.fire();
            assertEquals(List.of("D|NORMAL", "E|NORMAL"), tokens(controller.Gridpane1));
            redo.fire();
            assertPosition(slot(controller.Gridpane1, "F|NORMAL"), 0, 1);
            assertPosition(slot(controller.Gridpane1, "E|NORMAL"), 1, 1);

            undo.fire();
            tap(KeyCode.G);
            assertEquals(List.of("D|NORMAL", "E|NORMAL", "G|NORMAL"), tokens(controller.Gridpane1));
            assertTrue(redo.isDisabled());
        });
    }

    @Test
    void undoRedoRestoresBlankBlockPauseRepeatAndFreeText() throws Exception {
        onFx(() -> {
            Pane guides = (Pane) root.lookup("#emptyBlockOverlay");
            Pane pauses = (Pane) root.lookup("#pauseOverlay");
            Pane annotations = (Pane) root.lookup("#annotationOverlay");
            controller.emptyBlockButton.fire();
            controller.pauseStrokeButton.fire();
            call("appendSerializedToken", "WIEDERHOLUNG|EINZELSTRICH");
            ((TextField) root.lookup("#freeTextInput")).setText("Marsch");
            controller.addFreeTextButton.fire();

            assertEquals(1, annotations.getChildren().stream().filter(Text.class::isInstance).count());
            call("handleUndoAction", (Object) null);
            assertTrue(annotations.getChildren().stream().noneMatch(Text.class::isInstance));
            call("handleUndoAction", (Object) null);
            assertEquals(List.of("LEERBLOCK"), tokens(controller.Gridpane1));
            call("handleUndoAction", (Object) null);
            assertTrue(pauses.getChildren().isEmpty());
            call("handleUndoAction", (Object) null);
            assertTrue(tokens(controller.Gridpane1).isEmpty());
            assertTrue(guides.getChildren().isEmpty());

            for (int i = 0; i < 4; i++) call("handleRedoAction", (Object) null);
            assertEquals(List.of("LEERBLOCK", "WIEDERHOLUNG|EINZELSTRICH"), tokens(controller.Gridpane1));
            assertEquals(1, pauses.getChildren().size());
            assertEquals(1, guides.getChildren().size());
            assertEquals(1, annotations.getChildren().stream().filter(Text.class::isInstance).count());
        });
    }

    @Test
    void undoButtonRestoresPageHeadingText() throws Exception {
        onFx(() -> {
            TextField heading = (TextField) root.lookup("#leftMarchText");
            heading.setText("Marsch A");
            ((javafx.scene.control.Button) root.lookup("#undoButton")).fire();
            assertEquals("", heading.getText());
            ((javafx.scene.control.Button) root.lookup("#redoButton")).fire();
            assertEquals("Marsch A", heading.getText());
        });
    }

    @Test
    void pageHeadingFontSizesCanBeChangedUndoneAndSaved() throws Exception {
        onFx(() -> {
            String[] ids = {"leftCircleText", "leftMarchText", "rightCircleText", "rightMarchText"};
            int[] defaults = {18, 22, 18, 22};
            for (int i = 0; i < ids.length; i++) {
                TextField field = (TextField) root.lookup("#" + ids[i]);
                assertNotNull(field.getContextMenu());
                Menu sizeMenu = (Menu) field.getContextMenu().getItems().getFirst();
                assertEquals("Schriftgröße", sizeMenu.getText());
                sizeMenu.getItems().stream()
                        .filter(item -> Integer.valueOf(30).equals(item.getUserData()))
                        .findFirst().orElseThrow().fire();
                assertTrue(field.getStyle().contains("-fx-font-size: 30px"));
                assertTrue(field.getPrefHeight() >= 53);
                call("handleUndoAction", (Object) null);
                assertTrue(field.getStyle().contains("-fx-font-size: " + defaults[i] + "px"));
                call("handleRedoAction", (Object) null);
                assertTrue(field.getStyle().contains("-fx-font-size: 30px"));
            }

            StringWriter output = new StringWriter();
            BufferedWriter writer = new BufferedWriter(output);
            call("writePageHeadings", writer);
            writer.flush();
            assertTrue(output.toString().contains("Kreis1Groesse;30"));
            assertTrue(output.toString().contains("Marsch1Groesse;30"));
            assertTrue(output.toString().contains("Kreis2Groesse;30"));
            assertTrue(output.toString().contains("Marsch2Groesse;30"));

            call("clearPageTexts");
            assertTrue(((TextField) root.lookup("#leftMarchText")).getStyle().contains("22px"));
            for (String line : output.toString().lines().toList()) {
                int separator = line.indexOf(';');
                assertTrue((Boolean) call("restorePageText", line.substring(0, separator), line.substring(separator + 1)));
            }
            for (String id : ids) {
                assertTrue(((TextField) root.lookup("#" + id)).getStyle().contains("30px"));
            }
            call("clearPageTexts");
            call("restorePageText", "Marsch1", "TWFyc2NoIEE=");
            assertEquals("Marsch A", ((TextField) root.lookup("#leftMarchText")).getText());
            assertTrue(((TextField) root.lookup("#leftMarchText")).getStyle().contains("22px"));
        });
    }

    @Test
    void undoBackToSavedStateClearsUnsavedChangesFlag() throws Exception {
        onFx(() -> {
            var dirtyField = Controller.class.getDeclaredField("documentDirty");
            dirtyField.setAccessible(true);
            tap(KeyCode.D);
            assertTrue(dirtyField.getBoolean(controller));
            tap(KeyCode.Z, false, true, false);
            assertFalse(dirtyField.getBoolean(controller));
            tap(KeyCode.Z, true, true, false);
            assertTrue(dirtyField.getBoolean(controller));
        });
    }

    @Test
    void undoRedoKeepsBothPagesAndRestoresSlurAfterDeletingSelectedNote() throws Exception {
        onFx(() -> {
            Pane annotations = (Pane) root.lookup("#annotationOverlay");
            tap(KeyCode.D);
            tap(KeyCode.E);
            call("setSelectedNotes", List.of(slot(controller.Gridpane1, "D|NORMAL"),
                    slot(controller.Gridpane1, "E|NORMAL")));
            call("handleAddSlurAction", (Object) null);
            assertEquals(1, annotations.getChildren().stream().filter(Path.class::isInstance).count());

            controller.rightPageButton.fire();
            tap(KeyCode.G);
            controller.leftPageButton.fire();
            call("selectNote", slot(controller.Gridpane1, "D|NORMAL"), controller.Gridpane1);
            tap(KeyCode.DELETE);
            assertEquals(List.of("E|NORMAL"), tokens(controller.Gridpane1));
            assertTrue(annotations.getChildren().stream().noneMatch(Path.class::isInstance));

            tap(KeyCode.Z, false, true, false);
            assertEquals(List.of("D|NORMAL", "E|NORMAL"), tokens(controller.Gridpane1));
            assertEquals(List.of("G|NORMAL"), tokens(controller.Gridpane2));
            assertEquals(1, annotations.getChildren().stream().filter(Path.class::isInstance).count());
            tap(KeyCode.Z, true, true, false);
            assertEquals(List.of("E|NORMAL"), tokens(controller.Gridpane1));
            assertTrue(annotations.getChildren().stream().noneMatch(Path.class::isInstance));
        });
    }

    @Test
    void clickingNoteButtonReplacesSelectedMiddleNoteAndKeepsAppendPosition() throws Exception {
        onFx(() -> {
            for (GridPane grid : List.of(controller.Gridpane1, controller.Gridpane2)) {
                call("selectPage", grid);
                tap(KeyCode.D);
                tap(KeyCode.E);
                tap(KeyCode.F);
                StackPane middle = slot(grid, "E|NORMAL");
                mouseClick(middle);
                assertNotNull(middle.getEffect());
                Node buttonTarget = controller.G.getGraphic() == null ? controller.G : controller.G.getGraphic();
                mouseClick(buttonTarget);
                assertEquals(3, tokens(grid).size());
                assertFalse(tokens(grid).contains("E|NORMAL"));
                assertPosition(slot(grid, "D|NORMAL"), 0, 1);
                assertPosition(slot(grid, "G|NORMAL"), 1, 1);
                assertPosition(slot(grid, "F|NORMAL"), 2, 1);
                mouseClick(controller.A);
                assertEquals(4, tokens(grid).size());
                assertPosition(slot(grid, "A|NORMAL"), 3, 1);
            }
        });
    }

    @Test
    void droppedNoteInsertsAtPointerOnEitherPageAndSupportsUndo() throws Exception {
        onFx(() -> {
            TitledPane shortcuts = (TitledPane) root.lookup(".titled-pane");
            assertNotNull(shortcuts);
            assertFalse(shortcuts.isExpanded());
            for (GridPane grid : List.of(controller.Gridpane1, controller.Gridpane2)) {
                call("selectPage", grid);
                tap(KeyCode.D);
                tap(KeyCode.E);
                tap(KeyCode.F);
                root.layout();
                var bounds = slot(grid, "E|NORMAL").localToScene(
                        slot(grid, "E|NORMAL").getBoundsInLocal());
                assertTrue((Boolean) call("insertDroppedNote", grid, "G",
                        bounds.getMinX() + 1, bounds.getMinY() + bounds.getHeight() / 2));
                assertEquals(List.of("D|NORMAL", "G|NORMAL", "E|NORMAL", "F|NORMAL"), tokens(grid));
                assertPosition(slot(grid, "G|NORMAL"), 1, 1);
                tap(KeyCode.Z, false, true, false);
                assertEquals(List.of("D|NORMAL", "E|NORMAL", "F|NORMAL"), tokens(grid));
            }
        });
    }

    @Test
    void droppedNoteKeepsExistingSlurAttachedToOriginalNotes() throws Exception {
        onFx(() -> {
            tap(KeyCode.D);
            tap(KeyCode.E);
            call("setSelectedNotes", List.of(slot(controller.Gridpane1, "D|NORMAL"),
                    slot(controller.Gridpane1, "E|NORMAL")));
            call("handleAddSlurAction", (Object) null);
            root.layout();
            var bounds = slot(controller.Gridpane1, "E|NORMAL").localToScene(
                    slot(controller.Gridpane1, "E|NORMAL").getBoundsInLocal());
            assertTrue((Boolean) call("insertDroppedNote", controller.Gridpane1, "F",
                    bounds.getMinX() + 1, bounds.getMinY() + bounds.getHeight() / 2));
            assertEquals(List.of("D|NORMAL", "F|NORMAL", "E|NORMAL"), tokens(controller.Gridpane1));
            Pane annotations = (Pane) root.lookup("#annotationOverlay");
            assertEquals(1, annotations.getChildren().stream().filter(Path.class::isInstance).count());
        });
    }

    @Test
    void selectedNoteCanBeMovedToAnotherSlot() throws Exception {
        onFx(() -> {
            tap(KeyCode.D);
            tap(KeyCode.E);
            tap(KeyCode.F);
            call("selectNote", slot(controller.Gridpane1, "E|NORMAL"), controller.Gridpane1);
            root.layout();

            StackPane lastNote = slot(controller.Gridpane1, "F|NORMAL");
            Bounds bounds = lastNote.localToScene(lastNote.getBoundsInLocal());
            assertTrue((Boolean) call("moveNoteTo", controller.Gridpane1, 1, 1,
                    bounds.getMaxX() + 20, bounds.getMinY() + bounds.getHeight() / 2));

            assertEquals(List.of("D|NORMAL", "F|NORMAL", "E|NORMAL"), tokens(controller.Gridpane1));
            assertPosition(slot(controller.Gridpane1, "E|NORMAL"), 2, 1);
            assertNotNull(slot(controller.Gridpane1, "E|NORMAL").getEffect());
        });
    }

    @Test
    void selectedNotesCanBeMovedAsOneBlockToCursorPosition() throws Exception {
        onFx(() -> {
            tap(KeyCode.D);
            tap(KeyCode.E);
            tap(KeyCode.F);
            tap(KeyCode.G);
            call("setSelectedNotes", List.of(
                    slot(controller.Gridpane1, "D|NORMAL"),
                    slot(controller.Gridpane1, "E|NORMAL")));
            root.layout();

            StackPane lastNote = slot(controller.Gridpane1, "G|NORMAL");
            Bounds bounds = lastNote.localToScene(lastNote.getBoundsInLocal());
            assertTrue((Boolean) call("moveSelectedNotesTo", controller.Gridpane1,
                    bounds.getMaxX() + 20, bounds.getMinY() + bounds.getHeight() / 2));

            assertEquals(List.of("F|NORMAL", "G|NORMAL", "D|NORMAL", "E|NORMAL"),
                    tokens(controller.Gridpane1));
            assertNotNull(slot(controller.Gridpane1, "D|NORMAL").getEffect());
            assertNotNull(slot(controller.Gridpane1, "E|NORMAL").getEffect());
        });
    }

    @Test
    void selectedNoteCanBeMovedToTheOtherPage() throws Exception {
        onFx(() -> {
            tap(KeyCode.D);
            call("selectNote", slot(controller.Gridpane1, "D|NORMAL"), controller.Gridpane1);
            controller.rightPageButton.fire();
            tap(KeyCode.G);
            root.layout();

            StackPane target = slot(controller.Gridpane2, "G|NORMAL");
            Bounds bounds = target.localToScene(target.getBoundsInLocal());
            assertTrue((Boolean) call("moveNoteTo", controller.Gridpane1, 0, 1,
                    bounds.getMaxX() + 20, bounds.getMinY() + bounds.getHeight() / 2));

            assertTrue(tokens(controller.Gridpane1).isEmpty());
            assertEquals(List.of("G|NORMAL", "D|NORMAL"), tokens(controller.Gridpane2));
            assertNotNull(slot(controller.Gridpane2, "D|NORMAL").getEffect());
        });
    }

    @Test
    void selectedRepeatMarkerCanBeMovedToAnotherSlot() throws Exception {
        onFx(() -> {
            tap(KeyCode.D);
            call("appendSerializedToken", "WIEDERHOLUNG|EINZELSTRICH");
            tap(KeyCode.E);
            call("selectNote", slot(controller.Gridpane1, "WIEDERHOLUNG|EINZELSTRICH"), controller.Gridpane1);
            root.layout();

            StackPane lastNote = slot(controller.Gridpane1, "E|NORMAL");
            Bounds bounds = lastNote.localToScene(lastNote.getBoundsInLocal());
            assertTrue((Boolean) call("moveNoteTo", controller.Gridpane1, 1, 1,
                    bounds.getMaxX() + 20, bounds.getMinY() + bounds.getHeight() / 2));

            assertEquals(List.of("D|NORMAL", "E|NORMAL", "WIEDERHOLUNG|EINZELSTRICH"),
                    tokens(controller.Gridpane1));
            assertPosition(slot(controller.Gridpane1, "WIEDERHOLUNG|EINZELSTRICH"), 2, 1);
            assertNotNull(slot(controller.Gridpane1, "WIEDERHOLUNG|EINZELSTRICH").getEffect());
        });
    }

    @Test
    void noteSelectionDoesNotAlsoSelectRepeatBarButBarCanBeClicked() throws Exception {
        onFx(() -> {
            tap(KeyCode.D);
            call("appendSerializedToken", "WIEDERHOLUNG|ANFANG");
            tap(KeyCode.E);
            root.layout();

            StackPane repeat = slot(controller.Gridpane1, "WIEDERHOLUNG|ANFANG");
            StackPane note = slot(controller.Gridpane1, "E|NORMAL");
            Line repeatBar = repeat.getChildren().stream()
                    .filter(Line.class::isInstance)
                    .map(Line.class::cast)
                    .findFirst().orElseThrow();
            Pane page = (Pane) root.lookup("#pageCanvas");
            Bounds noteBounds = page.sceneToLocal(note.localToScene(note.getBoundsInLocal()));
            Bounds barBounds = page.sceneToLocal(repeatBar.localToScene(repeatBar.getBoundsInLocal()));
            double minX = Math.min(noteBounds.getMinX(), barBounds.getMinX()) - 2;
            double minY = Math.min(noteBounds.getMinY(), barBounds.getMinY()) - 2;
            double maxX = Math.max(noteBounds.getMaxX(), barBounds.getMaxX()) + 2;
            double maxY = Math.max(noteBounds.getMaxY(), barBounds.getMaxY()) + 2;

            call("selectNotesInRectangle", new BoundingBox(minX, minY,
                    maxX - minX, maxY - minY));
            assertNotNull(note.getEffect());
            assertNull(repeat.getEffect());

            mouseClick(repeatBar);
            assertNull(note.getEffect());
            assertNotNull(repeat.getEffect());
        });
    }

    @Test
    void clickingBlankPageStillClearsSelectionBeforeNextNoteClick() throws Exception {
        onFx(() -> {
            tap(KeyCode.D);
            mouseClick(slot(controller.Gridpane1, "D|NORMAL"));
            mouseClick(root);
            mouseClick(controller.G);
            assertEquals(List.of("D|NORMAL", "G|NORMAL"), tokens(controller.Gridpane1));
        });
    }

    private static void mouseClick(Node target) {
        mouseClick(target, MouseButton.PRIMARY);
    }

    private static void mouseClick(Node target, MouseButton button) {
        for (var type : List.of(MouseEvent.MOUSE_PRESSED, MouseEvent.MOUSE_RELEASED, MouseEvent.MOUSE_CLICKED)) {
            Event.fireEvent(target, new MouseEvent(type, 1, 1, 1, 1, button, 1,
                    false, false, false, false, button == MouseButton.PRIMARY && type == MouseEvent.MOUSE_PRESSED,
                    false, button == MouseButton.SECONDARY && type == MouseEvent.MOUSE_PRESSED,
                    false, false, true, new PickResult(target, 1, 1)));
        }
    }

    private void tap(KeyCode code, boolean shift, boolean control, boolean alt) {
        key(root, KeyEvent.KEY_PRESSED, code, shift, control, alt);
        key(root, KeyEvent.KEY_RELEASED, code, shift, control, alt);
    }

    private static void key(Node target, javafx.event.EventType<KeyEvent> type, KeyCode code,
                            boolean shift, boolean control, boolean alt) {
        Event.fireEvent(target, new KeyEvent(type, "", "", code, shift, control, alt, false));
    }

    private static List<String> tokens(GridPane grid) {
        return grid.getChildren().stream().filter(node -> node instanceof StackPane)
                .map(Node::getUserData).filter(data -> data instanceof String).map(Object::toString).toList();
    }

    private static StackPane slot(GridPane grid, String token) {
        return (StackPane) grid.getChildren().stream().filter(node -> token.equals(node.getUserData()))
                .findFirst().orElseThrow();
    }

    private static void assertPosition(Node node, int column, int row) {
        assertEquals(column, GridPane.getColumnIndex(node));
        assertEquals(row, GridPane.getRowIndex(node));
    }

    private Object call(String name, Object... arguments) throws Exception {
        for (Method method : Controller.class.getDeclaredMethods()) {
            if (method.getName().equals(name) && method.getParameterCount() == arguments.length) {
                method.setAccessible(true);
                return method.invoke(controller, arguments);
            }
        }
        throw new NoSuchMethodException(name);
    }

    private static void onFx(FxAction action) throws Exception {
        FutureTask<Void> task = new FutureTask<>(() -> { action.run(); return null; });
        Platform.runLater(task);
        task.get(30, TimeUnit.SECONDS);
    }

    @FunctionalInterface
    private interface FxAction {
        void run() throws Exception;
    }
}
