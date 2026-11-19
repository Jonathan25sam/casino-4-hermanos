package GUI;

import javafx.animation.ScaleTransition;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.scene.image.Image;
import netscape.javascript.JSObject;
import javafx.concurrent.Worker;
import javafx.application.Platform;

public class PokerTexas extends Application {

    private WebEngine webEngine;
    private BalanceManager balanceManager;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        try {
        Image icon = new Image(getClass().getResource("/images/icono.jpg").toString());
        stage.getIcons().add(icon);
        System.out.println("Ícono cargado correctamente: /images/icono.jpg");
    } catch (Exception e) {
        System.err.println("Error al cargar el ícono /images/icono.jpg: " + e.getMessage());
    }
        balanceManager = new BalanceManager();
        
        StackPane root = new StackPane();
        root.setPadding(new Insets(0));

        WebView webView = new WebView();
        webView.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
        StackPane.setMargin(webView, new Insets(0));

        Button backButton = createBackButton(stage);
        StackPane.setAlignment(backButton, Pos.TOP_RIGHT);
        StackPane.setMargin(backButton, new Insets(10, 30, 10, 0));

        root.getChildren().addAll(webView, backButton);

        webEngine = webView.getEngine();
        setupWebEngine();

        Scene scene = new Scene(root);
        stage.setTitle("Poker Texas Hold'em");
        double screenWidth = Screen.getPrimary().getBounds().getWidth();
        double screenHeight = Screen.getPrimary().getBounds().getHeight();
        stage.setWidth(screenWidth * 1.009);
        stage.setHeight(screenHeight * 0.96);
        stage.setResizable(false);
        stage.setScene(scene);
        stage.show();
        
        // Forzar guardado al cerrar
        stage.setOnCloseRequest(e -> {
            balanceManager.forceSave();
            System.out.println("Saldo guardado al cerrar: $" + CasinoBalance.getBalance());
        });
    }

    private Button createBackButton(Stage stage) {
        Button backButton = new Button("Regresar");
        backButton.setStyle("-fx-font-size: 14px; -fx-padding: 5 10; -fx-background-color: #c0392b; -fx-text-fill: white; -fx-background-radius: 5;");
        backButton.setCursor(Cursor.HAND);

        DropShadow dropShadow = new DropShadow();
        dropShadow.setRadius(10);
        dropShadow.setColor(Color.rgb(255, 255, 255, 0.5));

        ScaleTransition hoverTransition = new ScaleTransition(Duration.millis(200), backButton);
        hoverTransition.setToX(1.1);
        hoverTransition.setToY(1.1);
        
        backButton.setOnMouseEntered(e -> {
            hoverTransition.playFromStart();
            backButton.setStyle("-fx-font-size: 14px; -fx-padding: 5 10; -fx-background-color: #a93226; -fx-text-fill: white; -fx-background-radius: 5;");
            backButton.setEffect(dropShadow);
        });
        
        backButton.setOnMouseExited(e -> {
            hoverTransition.setToX(1.0);
            hoverTransition.setToY(1.0);
            hoverTransition.playFromStart();
            backButton.setStyle("-fx-font-size: 14px; -fx-padding: 5 10; -fx-background-color: #c0392b; -fx-text-fill: white; -fx-background-radius: 5;");
            backButton.setEffect(null);
        });

        ScaleTransition clickTransition = new ScaleTransition(Duration.millis(100), backButton);
        clickTransition.setToX(0.95);
        clickTransition.setToY(0.95);
        
        backButton.setOnMousePressed(e -> clickTransition.playFromStart());
        backButton.setOnMouseReleased(e -> {
            clickTransition.setToX(1.0);
            clickTransition.setToY(1.0);
            clickTransition.playFromStart();
        });

        backButton.setOnAction(e -> {
            // Forzar guardado antes de cerrar
            balanceManager.forceSave();
            stage.close();
            try {
                new InterfazPrincipal().start(new Stage());
            } catch (Exception ex) {
                System.err.println("Error al abrir InterfazPrincipal: " + ex.getMessage());
                ex.printStackTrace();
            }
        });

        return backButton;
    }

    private void setupWebEngine() {
        java.net.URL url = getClass().getResource("/PokerTexas/index-poker.html");
        
        if (url != null) {
            webEngine.load(url.toExternalForm());
            webEngine.setOnAlert(event -> System.out.println("JS Alert: " + event.getData()));

            webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
                if (newState == Worker.State.SUCCEEDED) {
                    Platform.runLater(() -> initializeJavaScriptConnection());
                }
            });
        } else {
            webEngine.loadContent(
                "<h1>Error: Archivo no encontrado</h1>" +
                "<p>Verifica que index-poker.html esté en:</p>" +
                "<code>/PokerTexas/index-poker.html</code>"
            );
        }
    }

    private void initializeJavaScriptConnection() {
        try {
            // Esperar a que JavaScript esté completamente cargado
            Thread.sleep(200);
            
            JSObject window = (JSObject) webEngine.executeScript("window");
            window.setMember("balanceManager", balanceManager);
            
            // Establecer saldo inicial con verificación
            double initialBalance = CasinoBalance.getBalance();
            webEngine.executeScript("setInitialBalance(" + initialBalance + ")");
            
            System.out.println("★ Poker Texas conectado exitosamente");
            System.out.println("★ Saldo inicial: $" + initialBalance);
            System.out.println("★ BalanceManager activo");
            
        } catch (Exception e) {
            System.err.println("✗ Error en conexión inicial: " + e.getMessage());
            
            // Reintento con más tiempo
            new Thread(() -> {
                try {
                    Thread.sleep(1000);
                    Platform.runLater(() -> {
                        try {
                            JSObject window = (JSObject) webEngine.executeScript("window");
                            window.setMember("balanceManager", balanceManager);
                            double balance = CasinoBalance.getBalance();
                            webEngine.executeScript("setInitialBalance(" + balance + ")");
                            System.out.println("★ Reintento exitoso - Balance: $" + balance);
                        } catch (Exception ex) {
                            System.err.println("✗ Fallo crítico en conexión: " + ex.getMessage());
                        }
                    });
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }
    }

    // Clase interna para manejar el balance de forma robusta
    public class BalanceManager {
        private double currentBalance;
        private boolean needsSave = false;

        public BalanceManager() {
            this.currentBalance = CasinoBalance.getBalance();
        }

        public boolean saveBalance(double newBalance) {
            try {
                if (newBalance >= 0) {  // Validación básica
                    this.currentBalance = newBalance;
                    CasinoBalance.setBalance(newBalance);
                    this.needsSave = true;
                    
                    // Verificar que se guardó correctamente
                    double savedBalance = CasinoBalance.getBalance();
                    boolean success = Math.abs(savedBalance - newBalance) < 0.01;
                    
                    if (success) {
                        System.out.println("★ Balance guardado: $" + newBalance);
                    } else {
                        System.err.println("✗ Error en verificación: esperado $" + newBalance + ", actual $" + savedBalance);
                    }
                    
                    return success;
                } else {
                    System.err.println("✗ Balance inválido: $" + newBalance);
                    return false;
                }
            } catch (Exception e) {
                System.err.println("✗ Error crítico guardando balance: " + e.getMessage());
                return false;
            }
        }

        public double getBalance() {
            return CasinoBalance.getBalance();
        }

        public void forceBalance(double balance) {
            try {
                CasinoBalance.setBalance(balance);
                this.currentBalance = balance;
                System.out.println("★ Balance forzado: $" + balance);
            } catch (Exception e) {
                System.err.println("✗ Error forzando balance: " + e.getMessage());
            }
        }

        public void forceSave() {
            if (needsSave) {
                try {
                    CasinoBalance.setBalance(currentBalance);
                    System.out.println("★ Guardado forzado completado: $" + currentBalance);
                } catch (Exception e) {
                    System.err.println("✗ Error en guardado forzado: " + e.getMessage());
                }
            }
        }
    }
}
