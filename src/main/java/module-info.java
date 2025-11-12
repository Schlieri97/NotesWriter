module com.example.noteswriter {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.code.noteswriter to javafx.fxml;
    exports com.code.noteswriter;
}