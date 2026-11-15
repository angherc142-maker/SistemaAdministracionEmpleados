package com.sistemaempleados.vista;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Consultas de solo lectura a MySQL para las pantallas de la interfaz.
 *
 * @author Javier
 */
public class RepositorioVista {

    // Define las variables de entorno DB_USER y DB_PASS, o cambia los valores por defecto.
    // No subas tu contraseña real al repositorio.
    private static final String URL =
            "jdbc:mysql://localhost:3306/remuneraciones_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String USUARIO = System.getenv().getOrDefault("DB_USER", "root");
    private static final String CLAVE = System.getenv().getOrDefault("DB_PASS", "");

    public record EmpleadoVista(String cedula, String nombre, String cargo,
                                double salarioBase, String tipo) {}

    public record RemuneracionVista(String cedula, String nombre, String tipo,
                                    String periodo, String detalle, double importe) {}

    private static Connection conectar() throws SQLException {
        return DriverManager.getConnection(URL, USUARIO, CLAVE);
    }

    /** Deduce el tipo a partir de los conceptos del cálculo (solución temporal). */
    private static String deducirTipo(String detalle) {
        String d = detalle == null ? "" : detalle.toLowerCase();
        if (d.contains("hora")) return "Por hora";
        if (d.contains("comision") || d.contains("comisión") || d.contains("venta")) return "Por comisión";
        return "Administrativo";
    }

    public static List<RemuneracionVista> remuneraciones() throws SQLException {
        String sql = """
            SELECT e.cedula, e.nombre, r.periodo, r.importe_calculado,
                   GROUP_CONCAT(CONCAT(v.concepto, ': ', v.monto) SEPARATOR ', ') AS detalle
            FROM remuneraciones r
            JOIN empleados e ON e.id = r.empleado_id
            LEFT JOIN remuneracion_valores v ON v.remuneracion_id = r.id
            GROUP BY r.id, e.cedula, e.nombre, r.periodo, r.importe_calculado
            ORDER BY r.periodo DESC, e.nombre
            """;
        List<RemuneracionVista> lista = new ArrayList<>();
        try (Connection c = conectar();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                String detalle = rs.getString("detalle");
                lista.add(new RemuneracionVista(
                        rs.getString("cedula"), rs.getString("nombre"),
                        deducirTipo(detalle), rs.getString("periodo"),
                        detalle == null ? "-" : detalle,
                        rs.getDouble("importe_calculado")));
            }
        }
        return lista;
    }

    public static List<EmpleadoVista> empleados() throws SQLException {
        Map<String, String> tipos = new HashMap<>();
        for (RemuneracionVista r : remuneraciones()) {
            tipos.putIfAbsent(r.cedula(), r.tipo());
        }
        List<EmpleadoVista> lista = new ArrayList<>();
        String sql = "SELECT cedula, nombre, cargo, salario_base FROM empleados ORDER BY nombre";
        try (Connection c = conectar();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                String ced = rs.getString("cedula");
                String cargo = rs.getString("cargo");
                lista.add(new EmpleadoVista(ced, rs.getString("nombre"),
                        cargo == null ? "-" : cargo, rs.getDouble("salario_base"),
                        tipos.getOrDefault(ced, "Sin definir")));
            }
        }
        return lista;
    }
}