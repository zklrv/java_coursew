package ua.university.passwordmanager;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import ua.university.passwordmanager.ui.MainWindow;

@SpringBootApplication
public class PasswordManagerApp extends Application {

    private static ConfigurableApplicationContext applicationContext;

    public static void main(String[] args) {
        Application.launch(PasswordManagerApp.class, args);
    }

    @Override
    public void init() {
        applicationContext = SpringApplication.run(PasswordManagerApp.class);
    }

    @Override
    public void start(Stage primaryStage) {
        MainWindow mainWindow = applicationContext.getBean(MainWindow.class);
        mainWindow.show(primaryStage);
    }

    @Override
    public void stop() {
        applicationContext.close();
        Platform.exit();
    }
}
