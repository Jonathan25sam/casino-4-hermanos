/*
 * Click nbfs://SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
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
import javafx.util.Duration;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import netscape.javascript.JSObject;
import java.net.URL;
import javafx.application.Platform;
import javafx.concurrent.Worker;

public class Dados extends Application {

    private WebEngine webEngine;
    private BalanceUpdateCallback balanceUpdateCallback;

    // Interfaz para el callback
    public interface BalanceUpdateCallback {
        void onBalanceUpdate(double newBalance);
    }

    // Método para establecer el callback
    public void setOnBalanceUpdate(BalanceUpdateCallback callback) {
        this.balanceUpdateCallback = callback;
    }

    // Método start sobrecargado para recibir el saldo
    public void start(Stage stage, double initialBalance) {
        CasinoBalance.setBalance(initialBalance);
        System.out.println("Saldo inicial recibido en Dados: $" + initialBalance);
        start(stage);
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
        StackPane root = new StackPane();
        root.setPadding(new Insets(0));

        WebView webView = new WebView();
        webEngine = webView.getEngine();
        webView.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
        StackPane.setMargin(webView, new Insets(0));

        // Habilitar consola de JavaScript para depuración
        webEngine.setOnAlert(event -> System.out.println("JS Alert: " + event.getData()));
        webEngine.setJavaScriptEnabled(true);

        // Crear puente Java-JavaScript
        try {
            JSObject window = (JSObject) webEngine.executeScript("window");
            window.setMember("javaBridge", new JavaBridge());
            System.out.println("Puente Java-JavaScript establecido correctamente.");
        } catch (Exception e) {
            System.err.println("Error al establecer el puente Java-JavaScript: " + e.getMessage());
        }

        Button backButton = new Button("Regresar");
        backButton.setStyle("-fx-font-size: 14px; -fx-padding: 5 10; -fx-background-color: #e67e22; -fx-text-fill: white; -fx-background-radius: 5;");
        backButton.setCursor(Cursor.HAND);

        DropShadow dropShadow = new DropShadow();
        dropShadow.setRadius(10);
        dropShadow.setColor(Color.rgb(255, 255, 255, 0.5));

        ScaleTransition hoverTransition = new ScaleTransition(Duration.millis(200), backButton);
        hoverTransition.setToX(1.1);
        hoverTransition.setToY(1.1);
        backButton.setOnMouseEntered(e -> {
            hoverTransition.playFromStart();
            backButton.setStyle("-fx-font-size: 14px; -fx-padding: 5 10; -fx-background-color: #d35400; -fx-text-fill: white; -fx-background-radius: 5;");
            backButton.setEffect(dropShadow);
        });
        backButton.setOnMouseExited(e -> {
            hoverTransition.setToX(1.0);
            hoverTransition.setToY(1.0);
            hoverTransition.playFromStart();
            backButton.setStyle("-fx-font-size: 14px; -fx-padding: 5 10; -fx-background-color: #e67e22; -fx-text-fill: white; -fx-background-radius: 5;");
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
            // Obtener el saldo actual desde JavaScript
            try {
                Object result = webEngine.executeScript("getBalance()");
                double updatedBalance;
                if (result instanceof Number) {
                    updatedBalance = ((Number) result).doubleValue();
                    System.out.println("Saldo obtenido desde JavaScript: $" + updatedBalance);
                } else {
                    updatedBalance = CasinoBalance.getBalance();
                    System.out.println("No se pudo obtener el saldo desde JavaScript, usando CasinoBalance: $" + updatedBalance);
                }
                CasinoBalance.setBalance(updatedBalance);
                if (balanceUpdateCallback != null) {
                    balanceUpdateCallback.onBalanceUpdate(updatedBalance);
                }
            } catch (Exception ex) {
                System.err.println("Error al obtener el saldo desde JavaScript: " + ex.getMessage());
                if (balanceUpdateCallback != null) {
                    balanceUpdateCallback.onBalanceUpdate(CasinoBalance.getBalance());
                }
            }
            stage.close();
            try {
                new InterfazPrincipal().start(new Stage());
            } catch (Exception ex) {
                System.err.println("Error al abrir InterfazPrincipal: " + ex.getMessage());
                ex.printStackTrace();
            }
        });

        StackPane.setAlignment(backButton, Pos.TOP_RIGHT);
        StackPane.setMargin(backButton, new Insets(10, 30, 10, 0));
        root.getChildren().addAll(webView, backButton);

        URL url = getClass().getResource("/dados/index-dados.html");

        if (url != null) {
            System.out.println("Cargando HTML desde: " + url.toExternalForm());
            webEngine.load(url.toExternalForm());

            // Pasar el saldo inicial cuando la página se haya cargado
            webEngine.getLoadWorker().stateProperty().addListener((obs, old, newState) -> {
                if (newState == Worker.State.SUCCEEDED) {
                    System.out.println("Página HTML cargada correctamente.");
                    checkJavaScriptReady(0, 5);
                } else if (newState == Worker.State.FAILED) {
                    System.err.println("Error al cargar el HTML: " + webEngine.getLoadWorker().getException());
                    webEngine.loadContent(
                        "<h1>Error al cargar el juego</h1>" +
                        "<p>No se pudo cargar index-dados.html. Verifica la ruta:</p>" +
                        "<code>/dados/index-dados.html</code>"
                    );
                }
            });
        } else {
            System.err.println("Archivo HTML no encontrado en: /dados/index-dados.html");
            webEngine.loadContent(
                "<h1>Error: Archivo no encontrado</h1>" +
                "<p>Verifica que index-dados.html esté en:</p>" +
                "<code>/dados/index-dados.html</code>"
            );
        }

        Scene scene = new Scene(root);
        stage.setTitle("Juego de Dados - Par o Impar");
        double screenWidth = Screen.getPrimary().getBounds().getWidth();
        double screenHeight = Screen.getPrimary().getBounds().getHeight();
        stage.setWidth(screenWidth * 1.009);
        stage.setHeight(screenHeight * 0.96);
        stage.setResizable(false);
        stage.setScene(scene);
        stage.show();
    }

    // Método para verificar si JavaScript está listo antes de establecer el saldo
    private void checkJavaScriptReady(int attempt, int maxAttempts) {
        if (attempt >= maxAttempts) {
            System.err.println("No se pudo inicializar el saldo después de " + maxAttempts + " intentos.");
            Platform.runLater(() -> {
                webEngine.loadContent(
                    "<h1>Error de inicialización</h1>" +
                    "<p>No se pudo establecer el saldo inicial. Revisa los logs para más detalles.</p>" +
                    "<p>Posible causa: script.js no está cargado o setInitialBalance no está definida.</p>"
                );
            });
            return;
        }

        Platform.runLater(() -> {
            try {
                Object jsFunction = webEngine.executeScript("typeof setInitialBalance === 'function'");
                if (jsFunction instanceof Boolean && (Boolean) jsFunction) {
                    double currentBalance = CasinoBalance.getBalance();
                    System.out.println("JavaScript listo. Estableciendo saldo inicial: $" + currentBalance);
                    webEngine.executeScript("setInitialBalance(" + currentBalance + ")");
                    System.out.println("Saldo inicial establecido correctamente: $" + currentBalance);
                } else {
                    throw new Exception("setInitialBalance no está definida en JavaScript");
                }
            } catch (Exception ex) {
                System.err.println("Intento #" + (attempt + 1) + " fallido - Error al verificar JavaScript: " + ex.getMessage());
                new Thread(() -> {
                    try {
                        Thread.sleep(500);
                        checkJavaScriptReady(attempt + 1, maxAttempts);
                    } catch (InterruptedException ie) {
                        System.err.println("Error en el reintento: " + ie.getMessage());
                    }
                }).start();
            }
        });
    }

    // Clase para el puente Java-JavaScript
    public class JavaBridge {
        public void updateBalance(double newBalance) {
            CasinoBalance.setBalance(newBalance);
            System.out.println("Saldo actualizado desde JavaScript a CasinoBalance: $" + newBalance);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}