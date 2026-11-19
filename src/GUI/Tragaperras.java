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

import java.io.File;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.scene.control.Alert;

public class Tragaperras extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        StackPane root = new StackPane();
        root.setPadding(new Insets(0));
// Set application icon
    try {
        Image icon = new Image(getClass().getResource("/images/icono.jpg").toString());
        stage.getIcons().add(icon);
        System.out.println("Ícono cargado correctamente: /images/icono.jpg");
    } catch (Exception e) {
        System.err.println("Error al cargar el ícono /images/icono.jpg: " + e.getMessage());
    }
        WebView webView = new WebView();
        WebEngine webEngine = webView.getEngine();
        webView.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
        StackPane.setMargin(webView, new Insets(0));

        // Configurar manejo de alertas para depuración
        webEngine.setOnAlert(event -> System.out.println("JS Alert: " + event.getData()));

        // Exponer javaBridge al JavaScript
        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                try {
                    JSObject window = (JSObject) webEngine.executeScript("window");
                    window.setMember("javaBridge", new CasinoBalanceBridge());
                    System.out.println("Puente javaBridge configurado correctamente en: " + new java.util.Date());

                    // Inicializar el saldo desde CasinoBalance
                    double initialBalance = CasinoBalance.getBalance();
                    window.call("setInitialBalance", initialBalance);
                    System.out.println("Saldo inicial enviado a JavaScript: $" + initialBalance);
                } catch (Exception e) {
                    System.err.println("Error al configurar el puente javaBridge o inicializar saldo: " + e.getMessage());
                    e.printStackTrace();
                }
            } else if (newState == javafx.concurrent.Worker.State.FAILED) {
                System.err.println("Error al cargar el contenido del WebView: " + webEngine.getLoadWorker().getException());
            }
        });

        Button backButton = new Button("Regresar");
        backButton.setStyle("-fx-font-size: 14px; -fx-padding: 5 10; -fx-background-color: #196F3D; -fx-text-fill: white; -fx-background-radius: 5;");
        backButton.setCursor(Cursor.HAND);

        DropShadow dropShadow = new DropShadow();
        dropShadow.setRadius(10);
        dropShadow.setColor(Color.rgb(255, 255, 255, 0.5));

        ScaleTransition hoverTransition = new ScaleTransition(Duration.millis(200), backButton);
        hoverTransition.setToX(1.1);
        hoverTransition.setToY(1.1);
        backButton.setOnMouseEntered(e -> {
            hoverTransition.playFromStart();
            backButton.setStyle("-fx-font-size: 14px; -fx-padding: 5 10; -fx-background-color: #229954; -fx-text-fill: white; -fx-background-radius: 5;");
            backButton.setEffect(dropShadow);
        });
        backButton.setOnMouseExited(e -> {
            hoverTransition.setToX(1.0);
            hoverTransition.setToY(1.0);
            hoverTransition.playFromStart();
            backButton.setStyle("-fx-font-size: 14px; -fx-padding: 5 10; -fx-background-color: #196F3D; -fx-text-fill: white; -fx-background-radius: 5;");
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
    // 1. Primero intentamos sincronizar el balance
    try {
        // Verificamos si el WebEngine está listo
        if (webEngine.getLoadWorker().getState() == Worker.State.SUCCEEDED) {
            // Obtenemos el balance de forma segura
            Object result = webEngine.executeScript(
                "(function() { " +
                "   try { " +
                "       return window.getBalance ? window.getBalance() : " + CasinoBalance.getBalance() + "; " +
                "   } catch(e) { " +
                "       console.error('Error en JS:', e); " +
                "       return " + CasinoBalance.getBalance() + "; " +
                "   } " +
                "})()"
            );
            
            // Convertimos el resultado a double de forma segura
            double finalBalance;
            if (result instanceof Number) {
                finalBalance = ((Number)result).doubleValue();
            } else {
                System.err.println("Warning: El balance devuelto no es número. Usando valor actual.");
                finalBalance = CasinoBalance.getBalance();
            }
            
            // Validamos el balance antes de guardarlo
            if (!Double.isNaN(finalBalance) && finalBalance >= 0) {
                CasinoBalance.setBalance(finalBalance);
                System.out.println("Balance sincronizado al salir: " + finalBalance);
            } else {
                System.err.println("Balance inválido recibido: " + finalBalance);
            }
        } else {
            System.err.println("WebEngine no está listo. No se pudo sincronizar el balance.");
        }
    } catch (Exception ex) {
        System.err.println("Error al sincronizar balance final: " + ex.getMessage());
        ex.printStackTrace();
    }

    // 2. Cerramos la ventana actual
    stage.close();

    // 3. Abrimos la nueva interfaz con manejo de errores
    Platform.runLater(() -> {
        try {
            new InterfazPrincipal().start(new Stage());
        } catch (Exception ex) {
            System.err.println("Error crítico al abrir InterfazPrincipal: " + ex.getMessage());
            ex.printStackTrace();
            
            // Mostrar alerta al usuario si falla
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("No se pudo abrir el menú principal");
            alert.setContentText("Por favor, reinicie la aplicación.");
            alert.showAndWait();
            
            Platform.exit();
        }
    });
});

        StackPane.setAlignment(backButton, Pos.TOP_RIGHT);
        StackPane.setMargin(backButton, new Insets(10, 30, 10, 0));

        root.getChildren().addAll(webView, backButton);

        String htmlPath = "src/TragaPerras/index.html";
        File htmlFile = new File(htmlPath);

        if (htmlFile.exists()) {
            webEngine.load(htmlFile.toURI().toString());
            System.out.println("Cargando HTML desde: " + htmlFile.toURI().toString());
        } else {
            webEngine.loadContent(
                "<h1>Error: Archivo no encontrado</h1>" +
                "<p>Verifica que index.html esté en:</p>" +
                "<code>" + htmlFile.getAbsolutePath() + "</code>"
            );
            System.err.println("Archivo HTML no encontrado en: " + htmlFile.getAbsolutePath());
        }

        Scene scene = new Scene(root);
        stage.setTitle("Tragaperras");

        double screenWidth = Screen.getPrimary().getBounds().getWidth();
        double screenHeight = Screen.getPrimary().getBounds().getHeight();

        stage.setWidth(screenWidth * 1.009);
        stage.setHeight(screenHeight * 0.96);
        stage.setResizable(false);
        stage.setScene(scene);
        stage.show();
    }

    // Clase puente para exponer métodos a JavaScript
public static class CasinoBalanceBridge {
    // Método para obtener el balance - versión robusta
    public double getBalance() {
        try {
            double currentBalance = CasinoBalance.getBalance();
            System.out.println("[JS->Java] Obteniendo balance: " + currentBalance);
            return currentBalance;
        } catch (Exception e) {
            System.err.println("[ERROR] Fallo al obtener balance: " + e.getMessage());
            return 600.0; // Valor por defecto seguro
        }
    }

    // Método para actualizar el balance - versión robusta
    public void updateBalance(double amount) {
        try {
            System.out.println("[JS->Java] Actualizando balance a: " + amount);
            CasinoBalance.setBalance(amount);
            
            // Verificación inmediata para confirmar la actualización
            double verifiedBalance = CasinoBalance.getBalance();
            if (verifiedBalance != amount) {
                System.err.println("[ERROR] Discrepancia en balance! Esperado: " + amount + 
                                  " | Actual: " + verifiedBalance);
                // Forzar la actualización correcta
                CasinoBalance.setBalance(amount);
            }
        } catch (Exception e) {
            System.err.println("[ERROR] Fallo al actualizar balance: " + e.getMessage());
            // Intento de recuperación
            try {
                CasinoBalance.setBalance(amount);
            } catch (Exception ex) {
                System.err.println("[ERROR CRÍTICO] No se pudo recuperar el balance: " + ex.getMessage());
            }
        }
    }
}
}