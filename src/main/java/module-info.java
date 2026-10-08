module com.example.noteswriter {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;


    opens com.code.noteswriter to javafx.fxml;
    exports com.code.noteswriter;
}
