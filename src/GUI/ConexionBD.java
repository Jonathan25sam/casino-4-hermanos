package GUI;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConexionBD {

    private static final String URL = valor("CASINO_DB_URL",
            "jdbc:mysql://localhost:3306/4hermanos?useSSL=false&serverTimezone=UTC");
    private static final String USUARIO = valor("CASINO_DB_USER", "root");
    private static final String CONTRASENA = valor("CASINO_DB_PASSWORD", "");

    private ConexionBD() {
    }

    public static Connection obtener() throws SQLException {
        return DriverManager.getConnection(URL, USUARIO, CONTRASENA);
    }

    private static String valor(String variable, String porDefecto) {
        String v = System.getenv(variable);
        return (v == null || v.isEmpty()) ? porDefecto : v;
    }
}
