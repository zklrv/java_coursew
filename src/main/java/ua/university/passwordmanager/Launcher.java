package ua.university.passwordmanager;
/**
 * Plain entry point for the application.
 * <p>
 * This class intentionally does NOT extend {@code javafx.application.Application}.
 * Without this indirection, running via {@code spring-boot:run} (or as an
 * executable JAR) fails with "JavaFX runtime components are missing" because
 * JavaFX 11+ requires its modules to be on the <em>module path</em> when the
 * JVM launches a subclass of {@code Application} directly.  Delegating through
 * this plain launcher loads JavaFX lazily via {@code Application.launch()},
 * which works correctly on the class path.
 * </p>
 */
public class Launcher {
    public static void main(String[] args) {
        PasswordManagerApp.main(args);
    }
}
