package com.sistemaempleados.vista;

import com.sistemaempleados.mysql.persistencia.MySqlConexion;
import com.sistemaempleados.servicio.EmpleadoService;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Consultas de solo lectura a MySQL para las pantallas de la interfaz.
 * El listado de empleados se obtiene con EmpleadoService (jerarquía de
 * clases); aquí solo se consulta el historial de remuneraciones.
 *
 * @author Javier
 */
public class RepositorioVista {

    // La conexión (usuario, contraseña y base de datos) se configura en MySqlConexion.

    public record RemuneracionVista(String codigo, String cedula, String nombre,
                                    String tipo, String periodo, String detalle,
                                    double importe) {}

    private static Connection conectar() throws SQLException {
        // Crea la base, las tablas y las columnas que falten antes de consultar.
        EmpleadoService.prepararEsquema();
        return MySqlConexion.getConexion();
    }

    /** Deduce el tipo a partir de los conceptos del cálculo (solo si el empleado no tiene tipo). */
    private static String deducirTipo(String detalle) {
        String d = detalle == null ? "" : detalle.toLowerCase();
        if (d.contains("hora")) return "Por hora";
        if (d.contains("comision") || d.contains("comisión") || d.contains("venta")) return "Por comisión";
        return "Administrativo";
    }

    public static List<RemuneracionVista> remuneraciones() throws SQLException {
        String sql = """
            SELECT e.codigo, e.cedula, e.nombre, e.tipo, r.periodo, r.importe_calculado,
                   GROUP_CONCAT(CONCAT(v.concepto, ': ', v.monto) SEPARATOR ', ') AS detalle
            FROM remuneraciones r
            JOIN empleados e ON e.id = r.empleado_id
            LEFT JOIN remuneracion_valores v ON v.remuneracion_id = r.id
            GROUP BY r.id, e.codigo, e.cedula, e.nombre, e.tipo, r.periodo, r.importe_calculado
            ORDER BY r.periodo DESC, e.nombre
            """;
        List<RemuneracionVista> lista = new ArrayList<>();
        try (Connection c = conectar();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                String detalle = rs.getString("detalle");
                String tipo = rs.getString("tipo");
                String codigo = rs.getString("codigo");
                lista.add(new RemuneracionVista(
                        codigo == null ? "-" : codigo,
                        rs.getString("cedula"), rs.getString("nombre"),
                        tipo != null ? tipo : deducirTipo(detalle), rs.getString("periodo"),
                        detalle == null ? "-" : detalle,
                        rs.getDouble("importe_calculado")));
            }
        }
        return lista;
    }
}