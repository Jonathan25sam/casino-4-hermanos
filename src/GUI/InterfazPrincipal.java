package GUI;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.scene.control.Alert.AlertType;
import java.util.UUID;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.nio.file.Paths;
import java.time.Year;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.io.File;
import java.awt.Desktop;

public class InterfazPrincipal extends Application {

    private static final double MAX_BALANCE = 20000.0; // Límite de saldo máximo
    private Label balanceLabel; // Referencia a la etiqueta de saldo
    private int userId; // ID del usuario
    private GridPane gameGrid; // Referencia al grid de juegos
    private HBox menu; // Referencia al contenedor de botones de menú
    private static final double WITHDRAWAL_FEE_PERCENTAGE = 0.15; // 15% fee for withdrawals

    // Constructor con userId
    public InterfazPrincipal(int userId) {
        this.userId = userId;
        CasinoBalance.setUserId(userId);
        loadUserData();
    }

    // Constructor por defecto
    public InterfazPrincipal() {
        this.userId = -1;
    }

    private void loadUserData() {
        System.out.println("loadUserData llamado - Datos ya cargados en CasinoBalance para userId " + userId);
    }

    @Override
    public void start(Stage primaryStage) {
        // Set application icon
        try {
            Image icon = new Image(getClass().getResource("/images/icono.jpg").toString());
            primaryStage.getIcons().add(icon);
        } catch (Exception e) {
            System.err.println("Error al cargar el icono: " + e.getMessage());
        }

        // Layout principal
        BorderPane root = new BorderPane();
        root.getStyleClass().add("root");

        // Contenedor superior
        VBox topContainer = new VBox();
        topContainer.getStyleClass().add("top-container");

        // Título y botón de cerrar sesión
        HBox titleBox = new HBox();
        titleBox.setAlignment(Pos.CENTER);
        titleBox.setPadding(new Insets(15, 30, 15, 30));

        Button logoutBtn = new Button("CERRAR SESIÓN");
        logoutBtn.getStyleClass().add("logout-btn");
        logoutBtn.setOnAction(e -> {
            primaryStage.close();
            Stage loginStage = new Stage();
            sesion_nueva loginApp = new sesion_nueva();
            loginApp.start(loginStage);
        });

        Region spacerLeft = new Region();
        HBox.setHgrow(spacerLeft, Priority.ALWAYS);

        Label title = new Label("CASINO 4 HERMANOS");
        title.getStyleClass().add("title");

        Region spacerRight = new Region();
        HBox.setHgrow(spacerRight, Priority.ALWAYS);

        titleBox.getChildren().addAll(logoutBtn, spacerLeft, title, spacerRight);

        // Controles
        HBox controlsBox = new HBox(30);
        controlsBox.setAlignment(Pos.CENTER);
        controlsBox.setPadding(new Insets(0, 30, 15, 30));

        menu = new HBox(15);
        menu.setAlignment(Pos.CENTER);

        String[] menuItems = {"TODOS", "SLOTS", "CARTAS", "CULTURA GENERAL", "DADOS", "NUEVOS JUEGOS"};
        for (String item : menuItems) {
            Button btn = new Button(item);
            btn.getStyleClass().add("menu-btn");
            if (item.equals("TODOS")) btn.getStyleClass().add("selected");
            btn.setOnAction(e -> filterGames(item, primaryStage));
            menu.getChildren().add(btn);
        }

        HBox balanceBox = new HBox(15);
        balanceBox.setAlignment(Pos.CENTER);

        balanceLabel = new Label(String.format("$%.2f", CasinoBalance.getBalance()));
        balanceLabel.getStyleClass().add("balance-label");

        Button depositBtn = new Button("DEPOSITAR");
        depositBtn.getStyleClass().add("deposit-btn");
        depositBtn.setOnAction(e -> showDepositDialog());

        Button withdrawBtn = new Button("RETIRAR");
        withdrawBtn.getStyleClass().add("withdraw-btn");
        withdrawBtn.setOnAction(e -> showWithdrawDialog());

        balanceBox.getChildren().addAll(balanceLabel, depositBtn, withdrawBtn);
        controlsBox.getChildren().addAll(menu, balanceBox);

        topContainer.getChildren().addAll(titleBox, controlsBox);

        // Contenido central
        ScrollPane mainContent = createMainContent(primaryStage, "TODOS");

        root.setTop(topContainer);
        root.setCenter(mainContent);

        Scene scene = new Scene(root, 1280, 800);
        scene.getStylesheets().add(getClass().getResource("/styles/casino.css").toExternalForm());

        // Configurar stage
        primaryStage.setTitle("Casino 4 Hermanos");
        primaryStage.setWidth(Screen.getPrimary().getBounds().getWidth() * 1.009);
        primaryStage.setHeight(Screen.getPrimary().getBounds().getHeight() * 0.96);
        primaryStage.setResizable(false);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // Método para filtrar juegos según la categoría seleccionada
    private void filterGames(String category, Stage primaryStage) {
        // Actualizar estilo de los botones
        for (javafx.scene.Node node : menu.getChildren()) {
            if (node instanceof Button) {
                Button btn = (Button) node;
                btn.getStyleClass().remove("selected");
                if (btn.getText().equals(category)) {
                    btn.getStyleClass().add("selected");
                }
            }
        }
        // Actualizar contenido del grid
        ScrollPane newContent = createMainContent(primaryStage, category);
        ((BorderPane) primaryStage.getScene().getRoot()).setCenter(newContent);
    }

    private ScrollPane createMainContent(Stage primaryStage, String category) {
        gameGrid = new GridPane();
        gameGrid.getStyleClass().add("games-grid");
        gameGrid.setAlignment(Pos.TOP_CENTER);
        gameGrid.setHgap(30);
        gameGrid.setVgap(30);
        gameGrid.setPadding(new Insets(30));

        // Lista de juegos con su categoría
        List<String[]> games = new ArrayList<>(Arrays.asList(
            new String[]{"TRAGAPERRAS CLÁSICA", "Frutas y símbolos clásicos", "JUGAR", "Tragaperras.png", "SLOTS"},
            new String[]{"BLACKJACK", "21 es el número mágico", "JUGAR", "Blackjack.png", "CARTAS"},
            new String[]{"PREGUNTAS DE CULTURA GENERAL", "Pon a prueba tus conocimientos", "JUGAR", "world.png", "CULTURA GENERAL"},
            new String[]{"POKER TEXAS", "El clásico juego de cartas", "JUGAR", "PokerTexas.png", "CARTAS"},
            new String[]{"DADOS", "Lanza los dados a la suerte", "JUGAR", "Dados.png", "DADOS"},
            new String[]{"NUEVOS JUEGOS PRÓXIMAMENTE", "¡Pronto más diversión!", "PRÓXIMAMENTE", "NuevosJuegos.png", "NUEVOS JUEGOS"}
        ));

        // Filtrar juegos según la categoría
        List<String[]> filteredGames = new ArrayList<>();
        if (category.equals("TODOS")) {
            filteredGames = games;
        } else {
            for (String[] game : games) {
                if (game[4].equals(category)) {
                    filteredGames.add(game);
                }
            }
        }

        int col = 0;
        int row = 0;
        for (String[] game : filteredGames) {
            gameGrid.add(createGameSection(game[0], game[1], game[2], game[3], primaryStage), col, row);
            col++;
            if (col >= 3) {
                col = 0;
                row++;
            }
        }

        ScrollPane scrollPane = new ScrollPane(gameGrid);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        scrollPane.getStyleClass().add("scroll-pane");

        return scrollPane;
    }

    private VBox createGameSection(String title, String description, String buttonText, String imageName, Stage primaryStage) {
        VBox section = new VBox(15);
        section.getStyleClass().add("game-section");
        section.setAlignment(Pos.CENTER);
        section.setPadding(new Insets(20));
        section.setMinWidth(260);
        section.setMaxWidth(300);

        ImageView imageView = new ImageView();
        try {
            Image image = new Image(getClass().getResource("/images/" + imageName).toString());
            imageView.setImage(image);
            imageView.setFitWidth(200);
            imageView.setFitHeight(140);
            imageView.setPreserveRatio(true);
        } catch (Exception e) {
            System.err.println("Error al cargar la imagen: " + imageName);
            imageView.setStyle("-fx-background-color: #333; -fx-min-width: 200px; -fx-min-height: 140px;");
        }

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("game-title");

        Label descLabel = new Label(description);
        descLabel.getStyleClass().add("game-desc");
        descLabel.setAlignment(Pos.CENTER);
        descLabel.setWrapText(true);

        Button playBtn = new Button(buttonText);
        playBtn.getStyleClass().add("play-btn");

        if (title.equals("NUEVOS JUEGOS PRÓXIMAMENTE")) {
            playBtn.setDisable(true);
        } else {
            if (title.equals("TRAGAPERRAS CLÁSICA")) {
                playBtn.setOnAction(e -> {
                    try {
                        primaryStage.close();
                        Tragaperras tragaperras = new Tragaperras();
                        Stage gameStage = new Stage();
                        tragaperras.start(gameStage);
                        gameStage.setOnCloseRequest(closeEvent -> {
                            Stage mainStage = new Stage();
                            InterfazPrincipal mainApp = new InterfazPrincipal(userId);
                            mainApp.start(mainStage);
                        });
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });
            }
            if (title.equals("BLACKJACK")) {
                playBtn.setOnAction(e -> {
                    try {
                        primaryStage.close();
                        BlackJack blackJack = new BlackJack();
                        blackJack.setOnBalanceUpdate(newBalance -> updateBalance(newBalance));
                        Stage gameStage = new Stage();
                        blackJack.start(gameStage, CasinoBalance.getBalance());
                        gameStage.setOnCloseRequest(closeEvent -> {
                            Stage mainStage = new Stage();
                            InterfazPrincipal mainApp = new InterfazPrincipal(userId);
                            mainApp.start(mainStage);
                        });
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });
            }
            if (title.equals("PREGUNTAS DE CULTURA GENERAL")) {
                playBtn.setOnAction(e -> {
                    try {
                        primaryStage.close();
                        Preguntas preguntasApp = new Preguntas();
                        Stage gameStage = new Stage();
                        preguntasApp.start(gameStage);
                        gameStage.setOnCloseRequest(closeEvent -> {
                            Stage mainStage = new Stage();
                            InterfazPrincipal mainApp = new InterfazPrincipal(userId);
                            mainApp.start(mainStage);
                        });
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });
            }
            if (title.equals("POKER TEXAS")) {
                playBtn.setOnAction(e -> {
                    try {
                        primaryStage.close();
                        PokerTexas pokerTexas = new PokerTexas();
                        Stage gameStage = new Stage();
                        pokerTexas.start(gameStage);
                        gameStage.setOnCloseRequest(closeEvent -> {
                            Stage mainStage = new Stage();
                            InterfazPrincipal mainApp = new InterfazPrincipal(userId);
                            mainApp.start(mainStage);
                        });
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });
            }
            if (title.equals("DADOS")) {
                playBtn.setOnAction(e -> {
                    try {
                        primaryStage.close();
                        Dados dados = new Dados();
                        Stage gameStage = new Stage();
                        dados.start(gameStage);
                        gameStage.setOnCloseRequest(closeEvent -> {
                            Stage mainStage = new Stage();
                            InterfazPrincipal mainApp = new InterfazPrincipal(userId);
                            mainApp.start(mainStage);
                        });
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });
            }
        }

        section.getChildren().addAll(imageView, titleLabel, descLabel, playBtn);
        return section;
    }

    public void updateBalance(double newBalance) {
        System.out.println("Actualizando saldo a: $" + newBalance);
        if (Double.isNaN(newBalance) || Double.isInfinite(newBalance)) {
            System.out.println("Error: Nuevo saldo inválido (NaN o infinito). Restaurando saldo actual.");
            newBalance = CasinoBalance.getBalance();
        }
        try {
            CasinoBalance.setBalance(newBalance);
            double updatedBalance = CasinoBalance.getBalance();
            balanceLabel.setText(String.format("$%.2f", updatedBalance));
            System.out.println("Saldo actualizado exitosamente a: $" + updatedBalance);
        } catch (Exception e) {
            System.out.println("Error al actualizar saldo: " + e.getMessage());
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText("Error al actualizar el saldo: " + e.getMessage());
            alert.getDialogPane().setStyle("-fx-background-color: #f4f4f4; -fx-border-color: #333; -fx-border-radius: 10;");
            alert.showAndWait();
            double currentBalance = CasinoBalance.getBalance();
            balanceLabel.setText(String.format("$%.2f", currentBalance));
        }
    }

    private void showDepositDialog() {
        Dialog<Double> dialog = new Dialog<>();
        dialog.setTitle("Depósito");
        dialog.setHeaderText("Ingrese los detalles de la tarjeta y el monto a depositar");

        ButtonType depositButtonType = new ButtonType("Depositar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(depositButtonType, ButtonType.CANCEL);

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: #f4f4f4; -fx-border-color: #333; -fx-border-radius: 10; -fx-background-radius: 10;");

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setAlignment(Pos.CENTER);

        Label cardNumberLabel = new Label("Número de tarjeta:");
        cardNumberLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #333;");
        TextField cardNumber = new TextField();
        cardNumber.setPromptText("16 dígitos");
        cardNumber.setStyle("-fx-background-color: #fff; -fx-border-color: #666; -fx-border-radius: 5; -fx-padding: 8;");

        Label bankLabel = new Label("Banco emisor:");
        bankLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #333;");
        ComboBox<String> bankComboBox = new ComboBox<>();
        bankComboBox.getItems().addAll(
            "BBVA", "Santander", "Banorte", "Citibanamex", "HSBC", "Scotiabank", "Inbursa",
            "Banco Azteca", "Banregio", "Afirme", "BanBajío", "Ve por Más", "Banca Mifel",
            "Barclays Bank México", "Intercam Banco", "Mercado Pago", "Nu", "Otro"
        );
        bankComboBox.setValue("BBVA");
        bankComboBox.setStyle("-fx-background-color: #fff; -fx-border-color: #666; -fx-border-radius: 5;");

        Label cardHolderLabel = new Label("Nombre del titular:");
        cardHolderLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #333;");
        TextField cardHolder = new TextField();
        cardHolder.setPromptText("Solo letras");
        cardHolder.setStyle("-fx-background-color: #fff; -fx-border-color: #666; -fx-border-radius: 5; -fx-padding: 8;");

        Label expiryDateLabel = new Label("Fecha de expiración:");
        expiryDateLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #333;");
        HBox expiryBox = new HBox(5);
        ComboBox<String> expiryMonth = new ComboBox<>();
        for (int i = 1; i <= 12; i++) {
            expiryMonth.getItems().add(String.format("%02d", i));
        }
        expiryMonth.setValue("01");
        ComboBox<String> expiryYear = new ComboBox<>();
        int currentYear = Year.now().getValue() % 100;
        for (int i = currentYear; i <= currentYear + 10; i++) {
            expiryYear.getItems().add(String.format("%02d", i));
        }
        expiryYear.setValue(String.format("%02d", currentYear));
        expiryBox.getChildren().addAll(expiryMonth, new Label("/"), expiryYear);
        expiryBox.setAlignment(Pos.CENTER_LEFT);

        Label cvvLabel = new Label("CVV:");
        cvvLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #333;");
        TextField cvv = new TextField();
        cvv.setPromptText("3 dígitos");
        cvv.setStyle("-fx-background-color: #fff; -fx-border-color: #666; -fx-border-radius: 5; -fx-padding: 8;");

        Label amountLabel = new Label("Monto:");
        amountLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #333;");
        TextField amount = new TextField();
        amount.setPromptText("Monto a depositar");
        amount.setStyle("-fx-background-color: #fff; -fx-border-color: #666; -fx-border-radius: 5; -fx-padding: 8;");

        grid.add(cardNumberLabel, 0, 0);
        grid.add(cardNumber, 1, 0);
        grid.add(bankLabel, 0, 1);
        grid.add(bankComboBox, 1, 1);
        grid.add(cardHolderLabel, 0, 2);
        grid.add(cardHolder, 1, 2);
        grid.add(expiryDateLabel, 0, 3);
        grid.add(expiryBox, 1, 3);
        grid.add(cvvLabel, 0, 4);
        grid.add(cvv, 1, 4);
        grid.add(amountLabel, 0, 5);
        grid.add(amount, 1, 5);

        content.getChildren().add(grid);
        dialog.getDialogPane().setContent(content);

        dialog.getDialogPane().lookupButton(depositButtonType).setStyle(
            "-fx-background-color: #28a745; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20; -fx-background-radius: 5;"
        );
        dialog.getDialogPane().lookupButton(ButtonType.CANCEL).setStyle(
            "-fx-background-color: #dc3545; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20; -fx-background-radius: 5;"
        );

        Button depositButton = (Button) dialog.getDialogPane().lookupButton(depositButtonType);
        depositButton.setDisable(true);

        cardNumber.textProperty().addListener((obs, old, newValue) -> {
            if (!newValue.matches("\\d{0,16}")) {
                cardNumber.setText(old);
            }
            updateDepositButtonState(depositButton, cardNumber, bankComboBox, cardHolder, expiryMonth, expiryYear, cvv, amount);
        });

        bankComboBox.valueProperty().addListener((obs, old, newValue) -> {
            updateDepositButtonState(depositButton, cardNumber, bankComboBox, cardHolder, expiryMonth, expiryYear, cvv, amount);
        });

        cardHolder.textProperty().addListener((obs, old, newValue) -> {
            if (!newValue.matches("[a-zA-Z\\s]*")) {
                cardHolder.setText(old);
            }
            updateDepositButtonState(depositButton, cardNumber, bankComboBox, cardHolder, expiryMonth, expiryYear, cvv, amount);
        });

        expiryMonth.valueProperty().addListener((obs, old, newValue) -> {
            updateDepositButtonState(depositButton, cardNumber, bankComboBox, cardHolder, expiryMonth, expiryYear, cvv, amount);
        });

        expiryYear.valueProperty().addListener((obs, old, newValue) -> {
            updateDepositButtonState(depositButton, cardNumber, bankComboBox, cardHolder, expiryMonth, expiryYear, cvv, amount);
        });

        cvv.textProperty().addListener((obs, old, newValue) -> {
            if (!newValue.matches("\\d{0,3}")) {
                cvv.setText(old);
            }
            updateDepositButtonState(depositButton, cardNumber, bankComboBox, cardHolder, expiryMonth, expiryYear, cvv, amount);
        });

        amount.textProperty().addListener((obs, old, newValue) -> {
            if (!newValue.matches("\\d*\\.?\\d{0,2}")) {
                amount.setText(old);
            }
            updateDepositButtonState(depositButton, cardNumber, bankComboBox, cardHolder, expiryMonth, expiryYear, cvv, amount);
        });

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == depositButtonType) {
                try {
                    double depositAmount = Double.parseDouble(amount.getText());
                    System.out.println("Depósito procesado: $" + depositAmount);
                    return depositAmount;
                } catch (NumberFormatException e) {
                    System.out.println("Error en formato de depósito: " + e.getMessage());
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(depositAmount -> {
            System.out.println("Ejecutando depósito de: $" + depositAmount);
            if (depositAmount > 0) {
                try {
                    CasinoBalance.addBalance(depositAmount);
                    double updatedBalance = CasinoBalance.getBalance();
                    balanceLabel.setText(String.format("$%.2f", updatedBalance));
                    Alert alert = new Alert(AlertType.INFORMATION);
                    alert.setTitle("Depósito Exitoso");
                    alert.setHeaderText(null);
                    alert.setContentText(String.format("Se han depositado $%.2f exitosamente.", depositAmount));
                    alert.getDialogPane().setStyle("-fx-background-color: #f4f4f4; -fx-border-color: #333; -fx-border-radius: 10;");
                    alert.showAndWait();
                    System.out.println("Depósito completado exitosamente");
                } catch (Exception e) {
                    System.out.println("Error al procesar depósito: " + e.getMessage());
                    Alert alert = new Alert(AlertType.ERROR);
                    alert.setTitle("Error");
                    alert.setHeaderText(null);
                    alert.setContentText("Error al procesar el depósito: " + e.getMessage());
                    alert.getDialogPane().setStyle("-fx-background-color: #f4f4f4; -fx-border-color: #333; -fx-border-radius: 10;");
                    alert.showAndWait();
                }
            } else {
                System.out.println("Monto de depósito inválido: " + depositAmount);
                Alert alert = new Alert(AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText(null);
                alert.setContentText("El monto debe ser mayor que 0.");
                alert.getDialogPane().setStyle("-fx-background-color: #f4f4f4; -fx-border-color: #333; -fx-border-radius: 10;");
                alert.showAndWait();
            }
        });
    }

    private void showWithdrawDialog() {
        Dialog<Double> dialog = new Dialog<>();
        dialog.setTitle("Retiro");
        dialog.setHeaderText("Ingrese el monto a retirar");

        ButtonType withdrawButtonType = new ButtonType("Retirar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(withdrawButtonType, ButtonType.CANCEL);

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: #f4f4f4; -fx-border-color: #333; -fx-border-radius: 10; -fx-background-radius: 10;");

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setAlignment(Pos.CENTER);

        Label amountLabel = new Label("Monto:");
        amountLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #333;");
        TextField amount = new TextField();
        amount.setPromptText("Monto a retirar");
        amount.setStyle("-fx-background-color: #fff; -fx-border-color: #666; -fx-border-radius: 5; -fx-padding: 8;");
        amount.setText("");

        Label feeNotice = new Label("Nota: Al retirar, se deducirá un 15% para fundaciones benéficas.");
        feeNotice.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");
        feeNotice.setWrapText(true);

        grid.add(amountLabel, 0, 0);
        grid.add(amount, 1, 0);
        grid.add(feeNotice, 0, 1, 2, 1);

        content.getChildren().add(grid);
        dialog.getDialogPane().setContent(content);

        dialog.getDialogPane().lookupButton(withdrawButtonType).setStyle(
            "-fx-background-color: #28a745; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20; -fx-background-radius: 5;"
        );
        dialog.getDialogPane().lookupButton(ButtonType.CANCEL).setStyle(
            "-fx-background-color: #dc3545; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20; -fx-background-radius: 5;"
        );

        Button withdrawButton = (Button) dialog.getDialogPane().lookupButton(withdrawButtonType);
        withdrawButton.setDisable(true);

        double currentBalance = CasinoBalance.getBalance();
        System.out.println("Saldo actual antes de abrir diálogo de retiro: $" + currentBalance);

        amount.textProperty().addListener((obs, old, newValue) -> {
            System.out.println("Monto ingresado: " + (newValue != null ? newValue : "null"));
            if (newValue == null || !newValue.matches("\\d*\\.?\\d{0,2}")) {
                amount.setText(old != null ? old : "");
                System.out.println("Monto inválido o nulo, restaurado a: " + (old != null ? old : "vacío"));
                withdrawButton.setDisable(true);
            } else {
                try {
                    double withdrawAmount = newValue.isEmpty() ? 0 : Double.parseDouble(newValue);
                    double balance = CasinoBalance.getBalance();
                    System.out.println("Saldo verificado en listener: $" + balance);
                    boolean isValid = withdrawAmount > 0 && withdrawAmount <= balance;
                    withdrawButton.setDisable(!isValid);
                    System.out.println("Monto válido: " + isValid + ", Monto: " + withdrawAmount + ", Saldo: " + balance);
                    if (isValid) {
                        double fee = withdrawAmount * WITHDRAWAL_FEE_PERCENTAGE;
                        double netAmount = withdrawAmount - fee;
                        feeNotice.setText(String.format("Nota: Al retirar $%.2f, se deducirá $%.2f (15%%) para fundaciones benéficas. Recibirá $%.2f.", 
                            withdrawAmount, fee, netAmount));
                    } else {
                        feeNotice.setText("Nota: Al retirar, se deducirá un 15% para fundaciones benéficas.");
                    }
                } catch (NumberFormatException e) {
                    withdrawButton.setDisable(true);
                    System.out.println("Error en formato de monto: " + e.getMessage());
                }
            }
        });

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == withdrawButtonType) {
                try {
                    String text = amount.getText();
                    System.out.println("Texto del monto en resultConverter: " + (text != null ? text : "null"));
                    if (text == null || text.trim().isEmpty()) {
                        System.out.println("Monto nulo o vacío en resultConverter");
                        return null;
                    }
                    double withdrawAmount = Double.parseDouble(text);
                    double balance = CasinoBalance.getBalance();
                    System.out.println("Procesando retiro de: $" + withdrawAmount + ", Saldo actual: $" + balance);
                    if (withdrawAmount <= 0 || withdrawAmount > balance) {
                        System.out.println("Monto inválido: " + withdrawAmount + ", Saldo: " + balance);
                        return null;
                    }
                    return withdrawAmount;
                } catch (NumberFormatException e) {
                    System.out.println("Error en formato de monto en resultConverter: " + e.getMessage());
                    return null;
                }
            }
            System.out.println("Diálogo cancelado o botón inválido");
            return null;
        });

        dialog.showAndWait().ifPresent(withdrawAmount -> {
            System.out.println("Ejecutando retiro de: $" + withdrawAmount);
            try {
                double balance = CasinoBalance.getBalance();
                if (withdrawAmount > 0 && withdrawAmount <= balance) {
                    double fee = withdrawAmount * WITHDRAWAL_FEE_PERCENTAGE;
                    double netAmount = withdrawAmount - fee;
                    String ticketCode = UUID.randomUUID().toString().substring(0, 8);
                    String dateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
                    System.out.println("Generando ticket con código: " + ticketCode);

                    // Generar la ruta del PDF
                    String safeDateTime = dateTime.replaceAll("[/:\\s]", "_");
                    String downloadsPath = Paths.get(System.getProperty("user.home"), "Downloads", "ticket_retiro_" + safeDateTime + ".pdf").toString();
                    generateTicketPDF(withdrawAmount, fee, netAmount, ticketCode, dateTime, downloadsPath);
                    System.out.println("PDF generado exitosamente en: " + downloadsPath);

                    CasinoBalance.setBalance(balance - withdrawAmount);
                    double updatedBalance = CasinoBalance.getBalance();
                    balanceLabel.setText(String.format("$%.2f", updatedBalance));
                    System.out.println("Saldo actualizado a: $" + updatedBalance);

                    // Crear el diálogo de confirmación
                    Alert alert = new Alert(AlertType.INFORMATION);
                    alert.setTitle("Retiro Solicitado");
                    alert.setHeaderText(null);

                    VBox contentBox = new VBox(10);
                    contentBox.setAlignment(Pos.CENTER);
                    contentBox.setPadding(new Insets(15));
                    contentBox.setStyle("-fx-background-color: #1a1a1a; -fx-border-color: #d4af37; -fx-border-width: 2; -fx-border-radius: 10; -fx-background-radius: 10;");

                    TextArea textArea = new TextArea(String.format(
                        "Retiro de $%.2f solicitado exitosamente.\n" +
                        "Deducción (15%%): $%.2f\n" +
                        "Monto neto a recibir: $%.2f\n" +
                        "Código de ticket: %s\n" +
                        "Diríjase al Casino 4 Hermanos para retirar su dinero con este código.\n" +
                        "El ticket se ha guardado en su carpeta de Descargas como 'ticket_retiro_%s.pdf'.",
                        withdrawAmount, fee, netAmount, ticketCode, safeDateTime));
                    textArea.setEditable(false);
                    textArea.setWrapText(true);
                    textArea.setMaxWidth(400);
                    textArea.setMaxHeight(200);
                    textArea.setMouseTransparent(true);
                    textArea.setStyle(
                        "-fx-control-inner-background: #2b2b2b; " +
                        "-fx-text-fill: #ffffff; " +
                        "-fx-font-family: 'Arial'; " +
                        "-fx-font-size: 14px; " +
                        "-fx-border-color: #d4af37; " +
                        "-fx-border-radius: 5; " +
                        "-fx-background-radius: 5; " +
                        "-fx-focus-color: transparent; " +
                        "-fx-faint-focus-color: transparent;"
                    );

                    Label titleLabel = new Label("Casino 4 Hermanos");
                    titleLabel.setStyle("-fx-font-family: 'Arial'; -fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #d4af37;");

                    // Botón para abrir el PDF
                    Button openPdfButton = new Button("Abrir Ticket PDF");
                    openPdfButton.setStyle(
                        "-fx-background-color: #d4af37; -fx-text-fill: #1a1a1a; -fx-font-weight: bold; -fx-padding: 8 20; -fx-background-radius: 5;"
                    );
                    openPdfButton.setOnAction(e -> {
                        try {
                            File pdfFile = new File(downloadsPath);
                            if (pdfFile.exists()) {
                                Desktop.getDesktop().open(pdfFile);
                                System.out.println("Abriendo PDF: " + downloadsPath);
                            } else {
                                System.err.println("El archivo PDF no existe en: " + downloadsPath);
                                Alert errorAlert = new Alert(AlertType.ERROR);
                                errorAlert.setTitle("Error");
                                errorAlert.setHeaderText(null);
                                errorAlert.setContentText("No se encontró el archivo PDF en la carpeta de Descargas.");
                                errorAlert.getDialogPane().setStyle("-fx-background-color: #f4f4f4; -fx-border-color: #333; -fx-border-radius: 10;");
                                errorAlert.showAndWait();
                            }
                        } catch (IOException ex) {
                            System.err.println("Error al abrir el PDF: " + ex.getMessage());
                            Alert errorAlert = new Alert(AlertType.ERROR);
                            errorAlert.setTitle("Error");
                            errorAlert.setHeaderText(null);
                            errorAlert.setContentText("Error al abrir el archivo PDF: " + ex.getMessage());
                            errorAlert.getDialogPane().setStyle("-fx-background-color: #f4f4f4; -fx-border-color: #333; -fx-border-radius: 10;");
                            errorAlert.showAndWait();
                        }
                    });

                    contentBox.getChildren().addAll(titleLabel, textArea, openPdfButton);
                    alert.getDialogPane().setContent(contentBox);

                    alert.getDialogPane().setStyle("-fx-background-color: #1a1a1a;");
                    alert.getDialogPane().lookupButton(ButtonType.OK).setStyle(
                        "-fx-background-color: #d4af37; -fx-text-fill: #1a1a1a; -fx-font-weight: bold; -fx-padding: 8 20; -fx-background-radius: 5;"
                    );

                    alert.showAndWait();
                    System.out.println("Retiro completado exitosamente");
                } else {
                    System.out.println("Monto inválido en ifPresent: " + withdrawAmount + ", Saldo: " + balance);
                    throw new IllegalArgumentException("Monto inválido o insuficiente saldo.");
                }
            } catch (IOException e) {
                System.out.println("Error al generar el ticket PDF: " + e.getMessage());
                Alert alert = new Alert(AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText(null);
                alert.setContentText("Error al generar el ticket PDF: " + e.getMessage());
                alert.getDialogPane().setStyle("-fx-background-color: #f4f4f4; -fx-border-color: #333; -fx-border-radius: 10;");
                alert.showAndWait();
            } catch (Exception e) {
                System.out.println("Error al procesar retiro: " + e.getMessage());
                Alert alert = new Alert(AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText(null);
                alert.setContentText("Error al procesar el retiro: " + e.getMessage());
                alert.getDialogPane().setStyle("-fx-background-color: #f4f4f4; -fx-border-color: #333; -fx-border-radius: 10;");
                alert.showAndWait();
            }
        });
    }

    private void generateTicketPDF(double amount, double fee, double netAmount, String ticketCode, String dateTime, String downloadsPath) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                // Fondo
                contentStream.setNonStrokingColor(240, 240, 240);
                contentStream.addRect(0, 0, page.getMediaBox().getWidth(), page.getMediaBox().getHeight());
                contentStream.fill();

                // Barra superior
                contentStream.setNonStrokingColor(212, 175, 55);
                contentStream.addRect(0, page.getMediaBox().getHeight() - 100, page.getMediaBox().getWidth(), 100);
                contentStream.fill();

                // Título
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 20);
                contentStream.setNonStrokingColor(0, 0, 0);
                contentStream.beginText();
                contentStream.newLineAtOffset(50, page.getMediaBox().getHeight() - 50);
                contentStream.showText("CASINO 4 HERMANOS");
                contentStream.endText();

                // Subtítulo
                contentStream.setFont(PDType1Font.HELVETICA, 14);
                contentStream.beginText();
                contentStream.newLineAtOffset(50, page.getMediaBox().getHeight() - 80);
                contentStream.showText("TICKET DE RETIRO");
                contentStream.endText();

                // Línea separadora
                contentStream.setLineWidth(1);
                contentStream.setStrokingColor(0, 0, 0);
                contentStream.moveTo(50, page.getMediaBox().getHeight() - 110);
                contentStream.lineTo(page.getMediaBox().getWidth() - 50, page.getMediaBox().getHeight() - 110);
                contentStream.stroke();

                // Detalles del ticket
                contentStream.setFont(PDType1Font.HELVETICA, 12);
                float yPosition = page.getMediaBox().getHeight() - 150;
                float labelX = 50;
                float valueX = 200;

                String userName = CasinoBalance.getUserName() != null ? CasinoBalance.getUserName() : "Usuario Anónimo";
                String userCorreo = CasinoBalance.getUserCorreo() != null ? CasinoBalance.getUserCorreo() : "No disponible";
                String userTelefono = CasinoBalance.getUserTelefono() != null ? CasinoBalance.getUserTelefono() : "No disponible";

                System.out.println("Valores para el ticket - Nombre: " + userName + ", Correo: " + userCorreo + ", Teléfono: " + userTelefono +
                                   ", Monto: " + amount + ", Deducción: " + fee + ", Neto: " + netAmount + ", Código: " + ticketCode + ", Fecha: " + dateTime);

                String[][] ticketDetails = {
                    {"Nombre del usuario:", userName},
                    {"Correo:", userCorreo},
                    {"Teléfono:", userTelefono},
                    {"Monto solicitado:", String.format("$%.2f", amount)},
                    {"Deducción (15%):", String.format("$%.2f", fee)},
                    {"Monto neto a recibir:", String.format("$%.2f", netAmount)},
                    {"Código de ticket:", ticketCode != null ? ticketCode : "N/A"},
                    {"Fecha y hora:", dateTime != null ? dateTime : LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))}
                };

                for (String[] detail : ticketDetails) {
                    if (yPosition < 100) { // Ajustado para dejar espacio para el sello
                        break;
                    }
                    contentStream.beginText();
                    contentStream.newLineAtOffset(labelX, yPosition);
                    contentStream.showText(detail[0] != null ? detail[0] : "");
                    contentStream.endText();

                    contentStream.beginText();
                    contentStream.newLineAtOffset(valueX, yPosition);
                    contentStream.showText(detail[1] != null ? detail[1] : "");
                    contentStream.endText();

                    yPosition -= 25;
                }

                // Instrucciones
                if (yPosition >= 120) {
                    contentStream.setFont(PDType1Font.HELVETICA_OBLIQUE, 10);
                    contentStream.beginText();
                    contentStream.newLineAtOffset(50, yPosition - 20);
                    contentStream.showText("Instrucciones: Presente este ticket en el Casino 4 Hermanos para retirar su dinero.");
                    contentStream.endText();
                }

                // Sello (icono.png)
                try {
                    PDImageXObject sealImage = PDImageXObject.createFromFile(
                        getClass().getResource("/images/icono.png").getFile(), document);
                    float sealWidth = 100;
                    float sealHeight = 100;
                    float sealX = page.getMediaBox().getWidth() - sealWidth - 50;
                    float sealY = page.getMediaBox().getHeight() - sealHeight - 10;
                    contentStream.drawImage(sealImage, sealX, sealY, sealWidth, sealHeight);
                    System.out.println("Sello icono.png añadido correctamente al PDF");
                } catch (Exception e) {
                    System.err.println("Error al cargar el sello icono.png: " + e.getMessage());
                }

                // Línea inferior
                contentStream.setLineWidth(1);
                contentStream.moveTo(50, 50);
                contentStream.lineTo(page.getMediaBox().getWidth() - 50, 50);
                contentStream.stroke();

                // Pie de página
                contentStream.setFont(PDType1Font.HELVETICA, 8);
                contentStream.beginText();
                contentStream.newLineAtOffset(50, 30);
                contentStream.showText("Casino 4 Hermanos - Todos los derechos reservados © 2025");
                contentStream.endText();
            }

            try {
                document.save(downloadsPath);
                System.out.println("PDF guardado en: " + downloadsPath);
            } catch (IOException e) {
                System.err.println("Error al guardar el PDF en " + downloadsPath + ": " + e.getMessage());
                throw e;
            }
        }
    }

    private boolean isValidCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() != 16 || !cardNumber.matches("\\d{16}")) {
            return false;
        }
        // Check for all zeros
        if (cardNumber.matches("0{16}")) {
            return false;
        }
        // Luhn Algorithm
        int sum = 0;
        boolean alternate = false;
        for (int i = cardNumber.length() - 1; i >= 0; i--) {
            int digit = Integer.parseInt(cardNumber.substring(i, i + 1));
            if (alternate) {
                digit *= 2;
                if (digit > 9) {
                    digit -= 9;
                }
            }
            sum += digit;
            alternate = !alternate;
        }
        return sum % 10 == 0;
    }

    private boolean isValidExpiryDate(String month, String year) {
        try {
            int m = Integer.parseInt(month);
            int y = Integer.parseInt(year);
            int currentYear = Year.now().getValue() % 100;
            int currentMonth = LocalDateTime.now().getMonthValue();

            if (m < 1 || m > 12) return false;
            if (y < currentYear || y > currentYear + 10) return false;
            if (y == currentYear && m < currentMonth) return false;

            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void updateDepositButtonState(Button depositButton, TextField cardNumber, ComboBox<String> bankComboBox, 
                                         TextField cardHolder, ComboBox<String> expiryMonth, ComboBox<String> expiryYear, 
                                         TextField cvv, TextField amount) {
        boolean isValid = isValidCardNumber(cardNumber.getText()) &&
                       bankComboBox.getValue() != null &&
                       cardHolder.getText().matches("[a-zA-Z\\s]{2,}") &&
                       isValidExpiryDate(expiryMonth.getValue(), expiryYear.getValue()) &&
                       cvv.getText().matches("\\d{3}") &&
                       amount.getText().matches("\\d+\\.?\\d{0,2}");
        depositButton.setDisable(!isValid);
    }

    public static void main(String[] args) {
        launch(args);
    }
}