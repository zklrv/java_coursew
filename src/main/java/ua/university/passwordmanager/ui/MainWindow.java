package ua.university.passwordmanager.ui;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.springframework.stereotype.Component;
import ua.university.passwordmanager.dto.PasswordCreateRequest;
import ua.university.passwordmanager.dto.PasswordResponse;
import ua.university.passwordmanager.dto.PasswordUpdateRequest;
import ua.university.passwordmanager.service.MasterPasswordService;
import ua.university.passwordmanager.service.PasswordGeneratorService;
import ua.university.passwordmanager.service.PasswordService;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class MainWindow {

    private final MasterPasswordService masterPasswordService;
    private final PasswordService passwordService;
    private final PasswordGeneratorService generatorService;

    private Stage primaryStage;
    private final ObservableList<PasswordResponse> passwordList = FXCollections.observableArrayList();

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public MainWindow(MasterPasswordService masterPasswordService,
                      PasswordService passwordService,
                      PasswordGeneratorService generatorService) {
        this.masterPasswordService = masterPasswordService;
        this.passwordService = passwordService;
        this.generatorService = generatorService;
    }

    public void show(Stage stage) {
        this.primaryStage = stage;
        primaryStage.setTitle("Password Manager");
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(600);
        navigateToAppropriateScreen();
        primaryStage.show();
    }

    private void navigateToAppropriateScreen() {
        if (masterPasswordService.isFirstRun()) {
            showSetupScreen();
        } else if (!masterPasswordService.isUnlocked()) {
            showLoginScreen();
        } else {
            showMainScreen();
        }
    }

    // ==================== SETUP SCREEN ====================

    private void showSetupScreen() {
        VBox root = new VBox(15);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(40));
        root.setMaxWidth(400);

        Label title = new Label("Create Master Password");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        Label info = new Label("Set a strong master password to protect your vault.\nMin 8 chars, uppercase, lowercase, digit.");
        info.setWrapText(true);

        PasswordField masterPasswordField = new PasswordField();
        masterPasswordField.setPromptText("Master Password");

        ProgressBar strengthBar = new ProgressBar(0);
        strengthBar.setPrefWidth(300);

        Label strengthLabel = new Label("Password Strength: —");

        masterPasswordField.textProperty().addListener((obs, old, text) -> {
            int score = masterPasswordService.calculateStrength(text);
            strengthBar.setProgress(score / 100.0);
            strengthLabel.setText("Strength: " + score + "/100");
            String color = score < 40 ? "red" : score < 70 ? "orange" : "green";
            strengthBar.setStyle("-fx-accent: " + color + ";");
        });

        PasswordField confirmPasswordField = new PasswordField();
        confirmPasswordField.setPromptText("Confirm Password");

        Button createBtn = new Button("Create Master Password");
        createBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-size: 14px;");
        createBtn.setPrefWidth(250);

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: red;");

        createBtn.setOnAction(e -> {
            String pw = masterPasswordField.getText();
            String confirm = confirmPasswordField.getText();
            if (!pw.equals(confirm)) {
                errorLabel.setText("Passwords do not match!");
                return;
            }
            if (!masterPasswordService.isPasswordStrong(pw)) {
                errorLabel.setText("Password too weak. Need uppercase, lowercase, digit, min 8 chars.");
                return;
            }
            try {
                masterPasswordService.setupMasterPassword(pw);
                showMainScreen();
            } catch (Exception ex) {
                errorLabel.setText("Error: " + ex.getMessage());
            }
        });

        root.getChildren().addAll(title, info, masterPasswordField, strengthBar, strengthLabel,
                confirmPasswordField, createBtn, errorLabel);

        StackPane centered = new StackPane(root);
        primaryStage.setScene(new Scene(centered, 900, 600));
    }

    // ==================== LOGIN SCREEN ====================

    private void showLoginScreen() {
        VBox root = new VBox(15);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(40));
        root.setMaxWidth(400);

        Label title = new Label("Password Manager");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");

        Label subtitle = new Label("Enter Master Password to unlock your vault");

        PasswordField loginPasswordField = new PasswordField();
        loginPasswordField.setPromptText("Master Password");
        loginPasswordField.setPrefWidth(300);

        Label attemptsLabel = new Label();
        attemptsLabel.setStyle("-fx-text-fill: orange;");
        updateAttemptsLabel(attemptsLabel);

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: red;");

        Button unlockBtn = new Button("Unlock");
        unlockBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 14px;");
        unlockBtn.setPrefWidth(200);

        unlockBtn.setOnAction(e -> {
            try {
                boolean success = masterPasswordService.unlock(loginPasswordField.getText());
                if (success) {
                    showMainScreen();
                } else {
                    loginPasswordField.clear();
                    errorLabel.setText("Wrong password. Failed attempts: " + masterPasswordService.getFailedAttempts() + "/5");
                    updateAttemptsLabel(attemptsLabel);
                }
            } catch (IllegalStateException ex) {
                errorLabel.setText("Account locked after too many attempts.");
                unlockBtn.setDisable(true);
            } catch (Exception ex) {
                errorLabel.setText("Error: " + ex.getMessage());
            }
        });

        loginPasswordField.setOnAction(e -> unlockBtn.fire());

        root.getChildren().addAll(title, subtitle, loginPasswordField, attemptsLabel, unlockBtn, errorLabel);

        StackPane centered = new StackPane(root);
        primaryStage.setScene(new Scene(centered, 900, 600));
    }

    private void updateAttemptsLabel(Label label) {
        int remaining = 5 - masterPasswordService.getFailedAttempts();
        if (masterPasswordService.getFailedAttempts() > 0) {
            label.setText("Remaining attempts: " + remaining);
        }
    }

    // ==================== MAIN SCREEN ====================

    private void showMainScreen() {
        BorderPane root = new BorderPane();

        ToolBar toolbar = buildToolbar();
        root.setTop(toolbar);

        ListView<PasswordResponse> listView = new ListView<>(passwordList);
        listView.setPrefWidth(280);
        listView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(PasswordResponse item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getServiceName() + "\n  " + item.getUsername());
                    setStyle("-fx-font-size: 12px;");
                }
            }
        });

        VBox detailPanel = buildDetailPanel(listView);

        listView.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected != null) {
                refreshDetailPanel(detailPanel, selected);
            }
        });

        SplitPane splitPane = new SplitPane(listView, detailPanel);
        splitPane.setDividerPositions(0.3);
        root.setCenter(splitPane);

        Scene scene = new Scene(root, 900, 600);
        primaryStage.setScene(scene);

        refreshPasswordList(listView);
    }

    private ToolBar buildToolbar() {
        Button addBtn = new Button("+ Add Password");
        addBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white;");
        addBtn.setOnAction(e -> showAddEditDialog(null));

        Button generateBtn = new Button("⚡ Generate");
        generateBtn.setOnAction(e -> showGeneratorDialog());

        Button refreshBtn = new Button("↺ Refresh");
        refreshBtn.setOnAction(e -> {
            if (primaryStage.getScene().getRoot() instanceof BorderPane bp) {
                if (bp.getCenter() instanceof SplitPane sp && sp.getItems().get(0) instanceof ListView<?> lv) {
                    @SuppressWarnings("unchecked")
                    ListView<PasswordResponse> listView = (ListView<PasswordResponse>) lv;
                    refreshPasswordList(listView);
                }
            }
        });

        Button exportBtn = new Button("Export");
        exportBtn.setOnAction(e -> {
            try {
                String json = passwordService.exportToJson();
                showTextDialog("Export JSON", json);
            } catch (Exception ex) {
                showError("Export failed: " + ex.getMessage());
            }
        });

        TextField searchField = new TextField();
        searchField.setPromptText("Search...");
        searchField.setPrefWidth(200);
        searchField.setOnAction(e -> {
            String q = searchField.getText().trim();
            if (!q.isEmpty()) {
                List<PasswordResponse> results = passwordService.searchByService(q);
                passwordList.setAll(results);
            }
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button lockBtn = new Button("🔒 Lock");
        lockBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white;");
        lockBtn.setOnAction(e -> {
            masterPasswordService.lock();
            showLoginScreen();
        });

        return new ToolBar(addBtn, generateBtn, refreshBtn, exportBtn,
                new Separator(), searchField, spacer, lockBtn);
    }

    private VBox buildDetailPanel(ListView<PasswordResponse> listView) {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(20));

        Label placeholder = new Label("Select a password to view details");
        placeholder.setStyle("-fx-font-size: 14px; -fx-text-fill: gray;");
        panel.getChildren().add(placeholder);

        return panel;
    }

    private void refreshDetailPanel(VBox panel, PasswordResponse item) {
        panel.getChildren().clear();

        Label serviceLabel = createDetailRow("Service:", item.getServiceName());
        Label userLabel = createDetailRow("Username:", item.getUsername());
        Label categoryLabel = createDetailRow("Category:", item.getCategory() != null ? item.getCategory() : "—");
        Label notesLabel = createDetailRow("Notes:", item.getNotes() != null ? item.getNotes() : "—");
        Label createdLabel = createDetailRow("Created:", item.getCreatedAt() != null ? item.getCreatedAt().format(DATE_FMT) : "—");
        Label lastUsedLabel = createDetailRow("Last Used:", item.getLastUsedAt() != null ? item.getLastUsedAt().format(DATE_FMT) : "—");

        HBox pwRow = new HBox(10);
        pwRow.setAlignment(Pos.CENTER_LEFT);
        Label pwLabel = new Label("Password:");
        pwLabel.setMinWidth(90);
        PasswordField pwField = new PasswordField();
        pwField.setEditable(false);
        pwField.setPrefWidth(200);
        TextField pwVisible = new TextField();
        pwVisible.setEditable(false);
        pwVisible.setPrefWidth(200);
        pwVisible.setVisible(false);
        pwVisible.setManaged(false);

        Button showHideBtn = new Button("Show");
        showHideBtn.setOnAction(e -> {
            if ("Show".equals(showHideBtn.getText())) {
                try {
                    PasswordResponse full = passwordService.getPassword(item.getId());
                    pwField.setText(full.getDecryptedPassword());
                    pwVisible.setText(full.getDecryptedPassword());
                    pwField.setVisible(false);
                    pwField.setManaged(false);
                    pwVisible.setVisible(true);
                    pwVisible.setManaged(true);
                    showHideBtn.setText("Hide");
                } catch (Exception ex) {
                    showError("Cannot decrypt: " + ex.getMessage());
                }
            } else {
                pwField.setVisible(true);
                pwField.setManaged(true);
                pwVisible.setVisible(false);
                pwVisible.setManaged(false);
                showHideBtn.setText("Show");
            }
        });

        Button copyBtn = new Button("Copy");
        copyBtn.setOnAction(e -> {
            try {
                PasswordResponse full = passwordService.getPassword(item.getId());
                javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
                javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
                content.putString(full.getDecryptedPassword());
                clipboard.setContent(content);
                showInfo("Password copied to clipboard!");
            } catch (Exception ex) {
                showError("Cannot copy: " + ex.getMessage());
            }
        });

        pwRow.getChildren().addAll(pwLabel, pwField, pwVisible, showHideBtn, copyBtn);

        HBox actions = new HBox(10);
        Button editBtn = new Button("✏ Edit");
        editBtn.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white;");

        Button deleteBtn = new Button("🗑 Delete");
        deleteBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white;");

        editBtn.setOnAction(e -> showAddEditDialog(item));

        deleteBtn.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete password for " + item.getServiceName() + "?");
            confirm.showAndWait().ifPresent(resp -> {
                if (resp == ButtonType.OK) {
                    try {
                        passwordService.deletePassword(item.getId());
                        passwordList.remove(item);
                        panel.getChildren().clear();
                        panel.getChildren().add(new Label("Select a password to view details"));
                    } catch (Exception ex) {
                        showError("Delete failed: " + ex.getMessage());
                    }
                }
            });
        });

        actions.getChildren().addAll(editBtn, deleteBtn);

        panel.getChildren().addAll(serviceLabel, userLabel, categoryLabel, notesLabel,
                createdLabel, lastUsedLabel, pwRow, new Separator(), actions);
    }

    private Label createDetailRow(String labelText, String value) {
        Label lbl = new Label(labelText + "  " + value);
        lbl.setStyle("-fx-font-size: 13px;");
        return lbl;
    }

    private void refreshPasswordList(ListView<PasswordResponse> listView) {
        try {
            List<PasswordResponse> all = passwordService.getAllPasswords();
            passwordList.setAll(all);
        } catch (Exception e) {
            showError("Failed to load passwords: " + e.getMessage());
        }
    }

    // ==================== ADD/EDIT DIALOG ====================

    private void showAddEditDialog(PasswordResponse existing) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(existing == null ? "Add Password" : "Edit Password");

        VBox form = new VBox(10);
        form.setPadding(new Insets(20));
        form.setPrefWidth(400);

        TextField serviceField = new TextField(existing != null ? existing.getServiceName() : "");
        serviceField.setPromptText("Service Name *");

        TextField usernameField = new TextField(existing != null ? existing.getUsername() : "");
        usernameField.setPromptText("Username *");

        TextField passwordField = new TextField();
        passwordField.setPromptText("Password *");

        Button generatePwBtn = new Button("Generate");
        generatePwBtn.setOnAction(e -> {
            String generated = generatorService.generate(16, true, true, true, true);
            passwordField.setText(generated);
        });

        HBox pwRow = new HBox(10, passwordField, generatePwBtn);
        HBox.setHgrow(passwordField, Priority.ALWAYS);

        TextField categoryField = new TextField(existing != null && existing.getCategory() != null ? existing.getCategory() : "");
        categoryField.setPromptText("Category");

        TextArea notesArea = new TextArea(existing != null && existing.getNotes() != null ? existing.getNotes() : "");
        notesArea.setPromptText("Notes");
        notesArea.setPrefRowCount(3);

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: red;");

        Button saveBtn = new Button(existing == null ? "Save" : "Update");
        saveBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setOnAction(e -> dialog.close());

        saveBtn.setOnAction(e -> {
            String svc = serviceField.getText().trim();
            String usr = usernameField.getText().trim();
            String pw = passwordField.getText().trim();

            if (svc.isEmpty() || usr.isEmpty()) {
                errorLabel.setText("Service and Username are required.");
                return;
            }
            if (existing == null && pw.isEmpty()) {
                errorLabel.setText("Password is required.");
                return;
            }
            try {
                if (existing == null) {
                    PasswordCreateRequest req = new PasswordCreateRequest();
                    req.setServiceName(svc);
                    req.setUsername(usr);
                    req.setPassword(pw);
                    req.setCategory(categoryField.getText().trim().isEmpty() ? null : categoryField.getText().trim());
                    req.setNotes(notesArea.getText().trim().isEmpty() ? null : notesArea.getText().trim());
                    PasswordResponse created = passwordService.createPassword(req);
                    passwordList.add(created);
                } else {
                    PasswordUpdateRequest req = new PasswordUpdateRequest();
                    req.setServiceName(svc);
                    req.setUsername(usr);
                    if (!pw.isEmpty()) req.setPassword(pw);
                    req.setCategory(categoryField.getText().trim().isEmpty() ? null : categoryField.getText().trim());
                    req.setNotes(notesArea.getText().trim().isEmpty() ? null : notesArea.getText().trim());
                    PasswordResponse updated = passwordService.updatePassword(existing.getId(), req);
                    int idx = -1;
                    for (int i = 0; i < passwordList.size(); i++) {
                        if (passwordList.get(i).getId().equals(existing.getId())) { idx = i; break; }
                    }
                    if (idx >= 0) passwordList.set(idx, updated);
                }
                dialog.close();
            } catch (Exception ex) {
                errorLabel.setText("Error: " + ex.getMessage());
            }
        });

        HBox buttons = new HBox(10, saveBtn, cancelBtn);

        form.getChildren().addAll(
                new Label(existing == null ? "Add New Password" : "Edit Password"),
                new Label("Service Name:"), serviceField,
                new Label("Username:"), usernameField,
                new Label("Password:"), pwRow,
                new Label("Category:"), categoryField,
                new Label("Notes:"), notesArea,
                errorLabel, buttons
        );

        dialog.setScene(new Scene(form));
        dialog.showAndWait();
    }

    // ==================== GENERATOR DIALOG ====================

    private void showGeneratorDialog() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Password Generator");

        VBox root = new VBox(10);
        root.setPadding(new Insets(20));
        root.setPrefWidth(400);

        Slider lengthSlider = new Slider(8, 64, 16);
        lengthSlider.setShowTickLabels(true);
        lengthSlider.setShowTickMarks(true);
        lengthSlider.setMajorTickUnit(8);
        lengthSlider.setSnapToTicks(true);

        Label lengthLabel = new Label("Length: 16");
        lengthSlider.valueProperty().addListener((obs, old, val) ->
                lengthLabel.setText("Length: " + val.intValue()));

        CheckBox upperCb = new CheckBox("Uppercase (A-Z)");
        upperCb.setSelected(true);
        CheckBox lowerCb = new CheckBox("Lowercase (a-z)");
        lowerCb.setSelected(true);
        CheckBox digitsCb = new CheckBox("Digits (0-9)");
        digitsCb.setSelected(true);
        CheckBox specialCb = new CheckBox("Special (!@#...)");
        specialCb.setSelected(true);

        TextField resultField = new TextField();
        resultField.setEditable(false);
        resultField.setPromptText("Generated password...");

        ProgressBar strengthBar = new ProgressBar(0);
        Label strengthLabel = new Label("Strength: —");

        Button generateBtn = new Button("Generate");
        generateBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white;");
        generateBtn.setOnAction(e -> {
            try {
                String pw = generatorService.generate(
                        (int) lengthSlider.getValue(),
                        upperCb.isSelected(), lowerCb.isSelected(),
                        digitsCb.isSelected(), specialCb.isSelected());
                resultField.setText(pw);
                int score = generatorService.calculateStrength(pw);
                strengthBar.setProgress(score / 100.0);
                strengthLabel.setText("Strength: " + score + "/100");
            } catch (Exception ex) {
                showError("Generation error: " + ex.getMessage());
            }
        });

        Button copyBtn = new Button("Copy");
        copyBtn.setOnAction(e -> {
            if (!resultField.getText().isEmpty()) {
                javafx.scene.input.Clipboard cb = javafx.scene.input.Clipboard.getSystemClipboard();
                javafx.scene.input.ClipboardContent cc = new javafx.scene.input.ClipboardContent();
                cc.putString(resultField.getText());
                cb.setContent(cc);
                showInfo("Password copied!");
            }
        });

        HBox btnRow = new HBox(10, generateBtn, copyBtn);
        root.getChildren().addAll(
                new Label("Password Generator"),
                lengthLabel, lengthSlider,
                upperCb, lowerCb, digitsCb, specialCb,
                btnRow, resultField, strengthBar, strengthLabel
        );

        dialog.setScene(new Scene(root));
        dialog.show();
    }

    // ==================== HELPERS ====================

    private void showTextDialog(String title, String content) {
        Stage dialog = new Stage();
        dialog.setTitle(title);
        TextArea ta = new TextArea(content);
        ta.setEditable(false);
        ta.setPrefSize(600, 400);
        dialog.setScene(new Scene(new VBox(ta)));
        dialog.show();
    }

    private void showError(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
            alert.setHeaderText("Error");
            alert.showAndWait();
        });
    }

    private void showInfo(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
            alert.showAndWait();
        });
    }
}
