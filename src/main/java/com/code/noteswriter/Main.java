package com.code.noteswriter;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.geometry.Rectangle2D;
import javafx.stage.Stage;
import javafx.stage.Screen;
import javafx.stage.StageStyle;

public class Main extends Application {

    Model m;
    Model model;
    Controller c;

    public static void main(String[] args) {
        launch(args);
    }

    public void setModel(Model model) {
        this.model = model;
    }

//    @Override
//    public void start(Stage primaryStage) throws Exception{
//
//
//        Parent root = FXMLLoader.load(getClass().getResource("SzFenster.fxml"));
//
//        // Title of the main window
//        primaryStage.setTitle("Button Press FXML");
//
//        Scene scene = new Scene(root);
//        primaryStage.setScene(scene);
//        primaryStage.show();
//    }
//
//
//    //Pop-Up Fenster öffnet sich
//    private void closeWindow(Stage stage){
//        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
//        alert.setTitle("WARNUNG");
//        alert.setHeaderText("Möchtest du wirklich das Spiel beenden?");
//        alert.setContentText("Beide Spielerfenster werden sich schließen!");
//        if(alert.showAndWait().get() == ButtonType.OK){
//            stage.close();
//        }
//    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("SzFenster.fxml"));
        Parent root = loader.load();

        // Inject model into the controller
        Controller controller = loader.getController();
        controller.setModel(new Model());

        // Setup and show the stage
        primaryStage.setTitle("Sz-Notenschreiber");
        var logoUrl = getClass().getResource("logo.png");
        if (logoUrl != null) {
            primaryStage.getIcons().add(new Image(logoUrl.toExternalForm()));
        }
//        primaryStage.setMinWidth(1200);
//        primaryStage.setMinHeight(800);

        // Explicitly request the normal decorated window. This keeps the
        // title bar and the minimize, maximize and close buttons available
        // on the first display as well as after a relaunch.
        primaryStage.initStyle(StageStyle.DECORATED);
        primaryStage.setResizable(true);
        primaryStage.setScene(new Scene(root));
        primaryStage.setOnCloseRequest(controller::handleCloseRequest);

        // The FXML page is intentionally large for printing. If its
        // preferred size is used directly on a smaller monitor, centering
        // puts the title bar outside the screen. Start inside the visual
        // desktop area instead; the page itself scales down responsively.
        Rectangle2D visualBounds = Screen.getPrimary().getVisualBounds();
        double initialWidth = Math.min(visualBounds.getWidth() * 0.92, 1700);
        double initialHeight = Math.min(visualBounds.getHeight() * 0.92, 950);
        primaryStage.setMinWidth(Math.min(1000, initialWidth));
        primaryStage.setMinHeight(Math.min(600, initialHeight));
        primaryStage.setWidth(initialWidth);
        primaryStage.setHeight(initialHeight);
        primaryStage.setX(visualBounds.getMinX() + (visualBounds.getWidth() - initialWidth) / 2);
        primaryStage.setY(visualBounds.getMinY() + (visualBounds.getHeight() - initialHeight) / 2);
        primaryStage.setMaximized(false);
        primaryStage.setFullScreen(false);
        primaryStage.show();
    }


}
