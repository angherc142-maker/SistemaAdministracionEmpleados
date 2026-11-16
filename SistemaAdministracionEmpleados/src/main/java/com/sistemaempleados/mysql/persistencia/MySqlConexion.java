package com.sistemaempleados.mysql.persistencia;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class MySqlConexion {

    private static final String URL_BASE =
            System.getenv().getOrDefault("MYSQL_URL_BASE", "jdbc:mysql://localhost:3306/");
    private static final String USUARIO =
            System.getenv().getOrDefault("MYSQL_USER", "root");

    private static final String CLAVE =
            System.getenv().getOrDefault("MYSQL_PASSWORD", "CAMBIAR_CONTRA_AQUÍ"); //  <--- cambia la contra de tu local

    private static final String OPCIONES =
            "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8";

    private static String baseDatos = "remuneraciones_db";

    private MySqlConexion() { }

    public static String getBaseDatos() { return baseDatos; }

    public static void setBaseDatos(String nombre) { baseDatos = nombre; }

    public static Connection getConexion() throws SQLException {
        return DriverManager.getConnection(URL_BASE + baseDatos + OPCIONES, USUARIO, CLAVE);
    }

    static Connection getConexionServidor() throws SQLException {
        return DriverManager.getConnection(URL_BASE + OPCIONES, USUARIO, CLAVE);
    }

    public static boolean probarConexion() {
        try (Connection c = getConexionServidor()) {
            return c.isValid(2);
        } catch (SQLException e) {
            System.err.println("No se pudo conectar a MySQL: " + e.getMessage());
            return false;
        }
    }
}
