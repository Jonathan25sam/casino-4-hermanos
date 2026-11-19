package GUI;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class CasinoBalance {
    private static double balance = 0;
    private static int userId = -1; // ID del usuario para identificar el saldo en la base de datos
    private static String userName; // Nombre del usuario
    private static String userCorreo; // Correo del usuario
    private static String userTelefono; // Teléfono del usuario
    private static final double MAX_BALANCE = 20000.0; // Límite de saldo máximo

    // Método para establecer el userId y cargar datos del usuario
    public static void setUserId(int id) {
        userId = id;
        // Cargar el saldo y los datos del usuario desde la base de datos
        loadUserDataFromDatabase();
    }

    // Método para cargar el saldo y los datos del usuario desde la base de datos
    private static void loadUserDataFromDatabase() {
        if (userId == -1) {
            System.err.println("Error: userId no establecido en CasinoBalance.");
            return;
        }

        Connection conn = null;
        PreparedStatement pst = null;
        ResultSet rs = null;

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = ConexionBD.obtener();

            // Cargar saldo
            pst = conn.prepareStatement("SELECT dinero FROM saldos_usuarios WHERE id = ?");
            pst.setInt(1, userId);
            rs = pst.executeQuery();

            if (rs.next()) {
                balance = rs.getDouble("dinero");
                System.out.println("Saldo cargado desde la base de datos para userId " + userId + ": $" + balance);
                if (balance > MAX_BALANCE) {
                    balance = MAX_BALANCE;
                    updateBalanceInDatabase();
                    System.out.println("Saldo ajustado al máximo permitido ($" + MAX_BALANCE + ")");
                }
            } else {
                System.err.println("Error: No se encontró saldo para el usuario con ID " + userId);
                balance = 0; // Valor por defecto si no se encuentra el usuario
                // Insertar un registro inicial si no existe
                insertInitialBalance(conn);
            }

            // Cerrar ResultSet y PreparedStatement para reutilizarlos
            try { if (rs != null) rs.close(); } catch (Exception ex) {}
            try { if (pst != null) pst.close(); } catch (Exception ex) {}

            // Cargar datos del usuario
            pst = conn.prepareStatement("SELECT nombre, correo, telefono FROM usuarios WHERE id = ?");
            pst.setInt(1, userId);
            rs = pst.executeQuery();

            if (rs.next()) {
                userName = rs.getString("nombre");
                userCorreo = rs.getString("correo");
                userTelefono = rs.getString("telefono");
                // Manejar valores null
                userName = (userName != null) ? userName : "Usuario Anónimo";
                userCorreo = (userCorreo != null) ? userCorreo : "No disponible";
                userTelefono = (userTelefono != null) ? userTelefono : "No disponible";
                System.out.println("Datos del usuario cargados - Nombre: " + userName + ", Correo: " + userCorreo + ", Teléfono: " + userTelefono);
            } else {
                System.err.println("Error: No se encontraron datos del usuario con ID " + userId);
                userName = "Usuario Anónimo";
                userCorreo = "No disponible";
                userTelefono = "No disponible";
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            System.err.println("Error al cargar datos desde la base de datos: " + ex.getMessage());
            balance = 0; // Establecer valor por defecto en caso de error
            userName = "Usuario Anónimo";
            userCorreo = "No disponible";
            userTelefono = "No disponible";
        } finally {
            try { if (rs != null) rs.close(); } catch (Exception ex) {}
            try { if (pst != null) pst.close(); } catch (Exception ex) {}
            try { if (conn != null) conn.close(); } catch (Exception ex) {}
        }
    }

    // Método para insertar un saldo inicial si el usuario no tiene registro
    private static void insertInitialBalance(Connection conn) throws Exception {
        PreparedStatement pst = null;
        try {
            pst = conn.prepareStatement("INSERT INTO saldos_usuarios (id, dinero) VALUES (?, ?)");
            pst.setInt(1, userId);
            pst.setDouble(2, 0.0);
            pst.executeUpdate();
            System.out.println("Saldo inicial ($0.0) insertado para userId " + userId);
        } finally {
            try { if (pst != null) pst.close(); } catch (Exception ex) {}
        }
    }

    // Método para actualizar el saldo en la base de datos
    private static void updateBalanceInDatabase() {
        if (userId == -1) {
            System.err.println("Error: userId no establecido en CasinoBalance.");
            return;
        }

        Connection conn = null;
        PreparedStatement pst = null;

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = ConexionBD.obtener();

            pst = conn.prepareStatement("UPDATE saldos_usuarios SET dinero = ? WHERE id = ?");
            pst.setDouble(1, balance);
            pst.setInt(2, userId);
            int rowsAffected = pst.executeUpdate();
            if (rowsAffected == 0) {
                System.err.println("Advertencia: No se actualizó el saldo para el usuario con ID " + userId);
            } else {
                System.out.println("Saldo actualizado en la base de datos para userId " + userId + ": $" + balance);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            System.err.println("Error al actualizar saldo en la base de datos: " + ex.getMessage());
        } finally {
            try { if (pst != null) pst.close(); } catch (Exception ex) {}
            try { if (conn != null) conn.close(); } catch (Exception ex) {}
        }
    }

    public static double getBalance() {
        // Cargar el saldo más reciente desde la base de datos cada vez que se accede
        loadUserDataFromDatabase(); // Esto también recargará los datos del usuario
        if (Double.isNaN(balance) || Double.isInfinite(balance)) {
            System.err.println("Error: Saldo inválido detectado (NaN o infinito). Restaurando a 0.");
            balance = 0;
            updateBalanceInDatabase();
        }
        return balance;
    }

    public static void setBalance(double newBalance) {
        if (Double.isNaN(newBalance) || Double.isInfinite(newBalance)) {
            System.err.println("Error: Intento de establecer saldo inválido (NaN o infinito). Manteniendo saldo actual.");
            return;
        }
        if (newBalance < 0) {
            System.err.println("Advertencia: Intento de establecer saldo negativo. Se ajustará a 0.");
            newBalance = 0;
        }
        if (newBalance > MAX_BALANCE) {
            System.err.println("Advertencia: Intento de establecer saldo mayor a 20,000. Se ajustará al máximo.");
            newBalance = MAX_BALANCE;
        }
        balance = newBalance;
        updateBalanceInDatabase(); // Actualizar en la base de datos
        System.out.println("Saldo establecido a: $" + balance + " para userId " + userId);
    }

    public static void addBalance(double amount) {
        if (amount < 0) {
            System.err.println("Advertencia: Intento de añadir monto negativo ($" + amount + "). Operación ignorada.");
            return;
        }
        double newBalance = balance + amount;
        setBalance(newBalance); // Esto también actualiza la base de datos
    }

    // Métodos para acceder a los datos del usuario
    public static String getUserName() {
        return userName;
    }

    public static String getUserCorreo() {
        return userCorreo;
    }

    public static String getUserTelefono() {
        return userTelefono;
    }
}