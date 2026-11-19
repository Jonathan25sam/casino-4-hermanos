package GUI;

import javafx.animation.ScaleTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;
import netscape.javascript.JSObject;
import java.net.URL;

public class BlackJack extends Application {

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

    // Método start sobrecargado para recibir el saldo inicial
    public void start(Stage stage, double initialBalance) {
        CasinoBalance.setBalance(initialBalance);
        System.out.println("Saldo inicial recibido en BlackJack: $" + initialBalance);
        start(stage);
    }

    @Override
    public void start(Stage stage) {
        // Set application icon
        try {
            Image icon = new Image(getClass().getResource("/images/icono.jpg").toString());
            stage.getIcons().add(icon);
            System.out.println("Ícono cargado correctamente: /images/icono.jpg");
        } catch (Exception e) {
            System.err.println("Error al cargar el ícono /images/icono.jpg: " + e.getMessage());
        }

        // Crear el contenedor principal
        StackPane root = new StackPane();
        root.setPadding(new Insets(0));

        // Crear WebView y obtener su motor
        WebView webView = new WebView();
        webEngine = webView.getEngine();
        webView.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
        StackPane.setMargin(webView, new Insets(0));

        // Habilitar alertas de JavaScript para depuración
        webEngine.setOnAlert(event -> System.out.println("JS Alert: " + event.getData()));
        webEngine.setJavaScriptEnabled(true);

        // Forzar recarga para evitar caché
        webEngine.setJavaScriptEnabled(false); // Deshabilitar temporalmente JS
        webEngine.setJavaScriptEnabled(true);  // Rehabilitar JS para forzar recarga

        // Crear puente Java-JavaScript cuando la página esté lista
        webEngine.getLoadWorker().stateProperty().addListener((obs, old, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                try {
                    JSObject window = (JSObject) webEngine.executeScript("window");
                    window.setMember("javaBridge", new JavaBridge());
                    System.out.println("Puente Java-JavaScript establecido correctamente en estado SUCCEEDED.");
                    // Establecer saldo inicial en JavaScript
                    checkJavaScriptReady(0, 5);
                } catch (Exception e) {
                    System.err.println("Error al establecer puente Java-JavaScript en estado SUCCEEDED: " + e.getMessage());
                }
            } else if (newState == Worker.State.FAILED) {
                System.err.println("Error al cargar la página HTML: " + webEngine.getLoadWorker().getException());
            }
        });

        // Botón "Regresar"
        Button backButton = new Button("Regresar");
        backButton.setStyle("-fx-font-size: 14px; -fx-padding: 5 10; -fx-background-color: #2c3e50; -fx-text-fill: white; -fx-background-radius: 5;");
        backButton.setCursor(Cursor.HAND);

        DropShadow dropShadow = new DropShadow();
        dropShadow.setRadius(10);
        dropShadow.setColor(Color.rgb(255, 255, 255, 0.5));

        ScaleTransition hoverTransition = new ScaleTransition(Duration.millis(200), backButton);
        hoverTransition.setToX(1.1);
        hoverTransition.setToY(1.1);
        backButton.setOnMouseEntered(e -> {
            hoverTransition.playFromStart();
            backButton.setStyle("-fx-font-size: 14px; -fx-padding: 5 10; -fx-background-color: #354d61; -fx-text-fill: white; -fx-background-radius: 5;");
            backButton.setEffect(dropShadow);
        });
        backButton.setOnMouseExited(e -> {
            hoverTransition.setToX(1.0);
            hoverTransition.setToY(1.0);
            hoverTransition.playFromStart();
            backButton.setStyle("-fx-font-size: 14px; -fx-padding: 5 10; -fx-background-color: #2c3e50; -fx-text-fill: white; -fx-background-radius: 5;");
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
            // Obtener saldo desde JavaScript y actualizar en Java
            try {
                Object result = webEngine.executeScript("getBalance()");
                double updatedBalance;
                if (result instanceof Number) {
                    updatedBalance = ((Number) result).doubleValue();
                    System.out.println("Saldo obtenido desde JavaScript: $" + updatedBalance);
                } else {
                    updatedBalance = CasinoBalance.getBalance();
                    System.out.println("No se pudo obtener saldo desde JS, usando CasinoBalance: $" + updatedBalance);
                }
                CasinoBalance.setBalance(updatedBalance);
                if (balanceUpdateCallback != null) {
                    balanceUpdateCallback.onBalanceUpdate(updatedBalance);
                }
            } catch (Exception ex) {
                System.err.println("Error al obtener saldo desde JavaScript: " + ex.getMessage());
                if (balanceUpdateCallback != null) {
                    balanceUpdateCallback.onBalanceUpdate(CasinoBalance.getBalance());
                }
            }
            stage.close();
            try {
                InterfazPrincipal mainInterface = new InterfazPrincipal();
                mainInterface.start(new Stage());
            } catch (Exception ex) {
                System.err.println("Error al abrir InterfazPrincipal: " + ex.getMessage());
                ex.printStackTrace();
            }
        });

        // Añadir elementos a root
        root.getChildren().addAll(webView, backButton);
        StackPane.setAlignment(backButton, Pos.TOP_RIGHT);
        StackPane.setMargin(backButton, new Insets(10));

        // Cargar archivo HTML usando getResource()
        URL url = getClass().getResource("/BlackJack/index.html");
        if (url != null) {
            System.out.println("Cargando HTML desde: " + url.toExternalForm());
            webEngine.load(url.toExternalForm());
        } else {
            System.err.println("Archivo HTML no encontrado en /BlackJack/index.html dentro del classpath.");
            webEngine.loadContent(
                "<h1>Error: Archivo no encontrado</h1>" +
                "<p>Verifica que index.html esté en:</p>" +
                "<code>/BlackJack/index.html</code>"
            );
        }

        // Configurar escena
        Scene scene = new Scene(root);

        // Configurar ventana
        stage.setTitle("BlackJack en JavaFX");
        double screenWidth = Screen.getPrimary().getBounds().getWidth();
        double screenHeight = Screen.getPrimary().getBounds().getHeight();
        stage.setWidth(screenWidth * 1.009);
        stage.setHeight(screenHeight * 0.96);
        stage.setResizable(false);
        stage.setScene(scene);
        stage.show();
    }

    // Método para verificar que JS esté listo para recibir saldo inicial
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

    // Clase para puente Java-JavaScript
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