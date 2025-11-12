package com.code.noteswriter;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

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
        primaryStage.setScene(new Scene(root));
        primaryStage.show();
    }


}
