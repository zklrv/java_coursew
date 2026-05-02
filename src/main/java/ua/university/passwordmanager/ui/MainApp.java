package ua.university.passwordmanager.ui;

import javafx.application.Application;
import javafx.stage.Stage;

/**
 * Standalone JavaFX launcher (not used when running via PasswordManagerApp).
 */
public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Password Manager");
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
