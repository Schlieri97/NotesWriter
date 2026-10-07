package com.code.noteswriter;

import javafx.application.Platform;
import javafx.event.Event;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
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
    void selectedOlderNotesAreNeitherReplacedNorDeletedByTyping() throws Exception {
        onFx(() -> {
            tap(KeyCode.D);
            tap(KeyCode.E);
            call("selectNote", slot(controller.Gridpane1, "D|NORMAL"), controller.Gridpane1);
            tap(KeyCode.G);
            assertEquals(List.of("D|NORMAL", "E|NORMAL", "G|NORMAL"), tokens(controller.Gridpane1));
            call("selectNote", slot(controller.Gridpane1, "D|NORMAL"), controller.Gridpane1);
            tap(KeyCode.BACK_SPACE);
            assertEquals(List.of("D|NORMAL", "E|NORMAL"), tokens(controller.Gridpane1));
            tap(KeyCode.DELETE);
            assertEquals(List.of("D|NORMAL"), tokens(controller.Gridpane1));
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
            for (int i = 0; i < 246; i++) tap(KeyCode.D);
            tap(KeyCode.ENTER);
            assertEquals(246, tokens(controller.Gridpane1).size());
            for (int i = 0; i < 41; i++) tap(KeyCode.D);
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
