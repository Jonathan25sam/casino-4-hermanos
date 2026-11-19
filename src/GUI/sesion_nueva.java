package GUI;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.effect.BlendMode;
import javafx.scene.effect.DropShadow;
import javafx.stage.Screen;
import javafx.stage.Stage;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class sesion_nueva extends Application {

    @Override
    public void start(Stage primaryStage) {
        // Get screen dimensions
        double screenWidth = Screen.getPrimary().getBounds().getWidth();
        double screenHeight = Screen.getPrimary().getBounds().getHeight();

        // Main layout
        VBox root = new VBox(30);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(30));
        root.getStyleClass().add("root");

        // Golden border container (90% of screen width, improved height)
        VBox formContainer = new VBox(25);
        formContainer.setAlignment(Pos.CENTER);
        formContainer.setPadding(new Insets(50));
        formContainer.setMaxWidth(screenWidth * 0.9);
        formContainer.setPrefHeight(screenHeight * 0.6); // Adjusted height for better proportion
        formContainer.setBackground(new Background(new BackgroundFill(
            Color.web("#2c2c2c", 0.95),
            new CornerRadii(20),
            Insets.EMPTY
        )));
        formContainer.setStyle("-fx-border-color: linear-gradient(to bottom, #ffd700, #b8860b); -fx-border-width: 4; -fx-border-radius: 20;");
        formContainer.setEffect(new DropShadow(20, Color.web("#ffd700", 0.3)));

        // Title
        Label titleLabel = new Label("CASINO 4 HERMANOS");
        titleLabel.getStyleClass().add("title");

        // Email field
        TextField txtCorreo = new TextField();
        txtCorreo.setPromptText("Correo electrónico");
        txtCorreo.getStyleClass().add("text-field");
        txtCorreo.setMaxWidth(400);

        // Password field
        PasswordField txtContrasena = new PasswordField();
        txtContrasena.setPromptText("Contraseña");
        txtContrasena.getStyleClass().add("text-field");
        txtContrasena.setMaxWidth(400);

        // Login button
        Button btnLogin = new Button("Iniciar Sesión");
        btnLogin.getStyleClass().add("login-btn");
        btnLogin.setMaxWidth(400);

        // Login button action
        btnLogin.setOnAction(e -> {
            String correo = txtCorreo.getText().trim();
            String contrasena = txtContrasena.getText();

            if (correo.isEmpty() || contrasena.isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Error", "Campos obligatorios", "Por favor, complete todos los campos.");
                return;
            }

            Connection conn = null;
            PreparedStatement pst = null;
            ResultSet rs = null;
            PreparedStatement pstSaldo = null;
            PreparedStatement insertSaldo = null;

            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                String contrasenaHash = generarHashSHA256(contrasena);
                conn = ConexionBD.obtener();

                pst = conn.prepareStatement("SELECT * FROM usuarios WHERE correo = ? AND contrasena = ?");
                pst.setString(1, correo);
                pst.setString(2, contrasenaHash);
                rs = pst.executeQuery();

                if (rs.next()) {
                    int userId = rs.getInt("id");
                    String userName = rs.getString("nombre");
                    String userCorreo = rs.getString("correo");
                    String userTelefono = rs.getString("telefono");

                    pstSaldo = conn.prepareStatement("SELECT * FROM saldos_usuarios WHERE id = ?");
                    pstSaldo.setInt(1, userId);
                    ResultSet rsSaldo = pstSaldo.executeQuery();

                    if (!rsSaldo.next()) {
                        insertSaldo = conn.prepareStatement("INSERT INTO saldos_usuarios (id, nombre, correo, telefono, dinero) VALUES (?, ?, ?, ?, ?)");
                        insertSaldo.setInt(1, userId);
                        insertSaldo.setString(2, userName);
                        insertSaldo.setString(3, userCorreo);
                        insertSaldo.setString(4, userTelefono);
                        insertSaldo.setDouble(5, 0.00);
                        insertSaldo.executeUpdate();
                    }

                    primaryStage.close();
                    Stage mainStage = new Stage();
                    InterfazPrincipal mainInterface = new InterfazPrincipal(userId);
                    mainInterface.start(mainStage);
                } else {
                    showAlert(Alert.AlertType.ERROR, "Error", "Credenciales incorrectas", "El correo o la contraseña son incorrectos.");
                }

            } catch (ClassNotFoundException ex) {
                showAlert(Alert.AlertType.ERROR, "Error de Driver", "Driver MySQL no encontrado",
                    "Asegúrate de tener el JAR del conector en el classpath.");
                ex.printStackTrace();
            } catch (NoSuchAlgorithmException ex) {
                showAlert(Alert.AlertType.ERROR, "Error de Seguridad", "Error en el algoritmo de encriptación",
                    "No se pudo generar el hash de la contraseña.");
                ex.printStackTrace();
            } catch (Exception ex) {
                showAlert(Alert.AlertType.ERROR, "Error de Base de Datos", "Error de conexión",
                    ex.getMessage());
                ex.printStackTrace();
            } finally {
                try { if (rs != null) rs.close(); } catch (Exception ex) {}
                try { if (pst != null) pst.close(); } catch (Exception ex) {}
                try { if (pstSaldo != null) pstSaldo.close(); } catch (Exception ex) {}
                try { if (insertSaldo != null) insertSaldo.close(); } catch (Exception ex) {}
                try { if (conn != null) conn.close(); } catch (Exception ex) {}
            }
        });

        // Add components to form container
        formContainer.getChildren().addAll(titleLabel, txtCorreo, txtContrasena, btnLogin);
        root.getChildren().add(formContainer);

        // Scene setup
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/styles/login.css").toExternalForm());

        // Stage setup
        primaryStage.setTitle("Inicio de Sesión - Casino 4 Hermanos");
        primaryStage.setWidth(screenWidth * 1.009);
        primaryStage.setHeight(screenHeight * 0.96);
        primaryStage.setResizable(false);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private String generarHashSHA256(String contrasena) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = md.digest(contrasena.getBytes(StandardCharsets.UTF_8));
        StringBuilder hexString = new StringBuilder();
        for (byte b : hashBytes) {
            hexString.append(String.format("%02x", b));
        }
        return hexString.toString();
    }

    private void showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.getDialogPane().setStyle("-fx-background-color: #2c2c2c; -fx-border-color: linear-gradient(to bottom, #ffd700, #b8860b); -fx-border-width: 2; -fx-border-radius: 10; -fx-text-fill: #ffd700;");
        alert.getDialogPane().lookup(".content.label").setStyle("-fx-text-fill: #ffd700; -fx-font-size: 14px;");
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}