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
    public record RemuneracionVista(String codigo, String cedula, String nombre,
            String tipo, String periodo, String detalle, double importe, String fechaCalculo) { }

    private static Connection conectar() throws SQLException {
        EmpleadoService.prepararEsquema();
        return MySqlConexion.getConexion();
    }

    private static String deducirTipo(String detalle) {
        String d = detalle == null ? "" : detalle.toLowerCase();
        if (d.contains("tarifahora") || d.contains("horastrabajadas")) return "Por hora";
        if (d.contains("porcentajecomision") || d.contains("montoventas")) return "Por comisión";
        return "Administrativo";
    }

    public static List<RemuneracionVista> remuneraciones() throws SQLException {
        String sql = """
            SELECT e.codigo, e.cedula, e.nombre, e.tipo, r.periodo, r.importe_calculado,
                   r.fecha_calculo,
                   GROUP_CONCAT(CONCAT(
                       CASE v.concepto
                         WHEN 'salarioBase' THEN 'Salario base'
                         WHEN 'tarifaPorHora' THEN 'Tarifa por hora'
                         WHEN 'horasTrabajadas' THEN 'Horas trabajadas'
                         WHEN 'montoVentas' THEN 'Monto de ventas'
                         WHEN 'porcentajeComision' THEN 'Porcentaje de comisión'
                         ELSE v.concepto END,
                       ': ', FORMAT(v.monto, 2)
                   ) ORDER BY v.concepto SEPARATOR ' | ') AS detalle
            FROM remuneraciones r
            JOIN empleados e ON e.id = r.empleado_id
            LEFT JOIN remuneracion_valores v ON v.remuneracion_id = r.id
            GROUP BY r.id, e.codigo, e.cedula, e.nombre, e.tipo, r.periodo,
                     r.importe_calculado, r.fecha_calculo
            ORDER BY r.periodo DESC, e.nombre, r.fecha_calculo DESC
            """;
        List<RemuneracionVista> lista = new ArrayList<>();
        try (Connection c = conectar(); Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                String detalle = rs.getString("detalle");
                String tipo = rs.getString("tipo");
                String codigo = rs.getString("codigo");
                java.sql.Timestamp fecha = rs.getTimestamp("fecha_calculo");
                lista.add(new RemuneracionVista(
                    codigo == null ? "-" : codigo,
                    rs.getString("cedula"), rs.getString("nombre"),
                    tipo != null ? tipo : deducirTipo(detalle), rs.getString("periodo"),
                    detalle == null ? "Sin detalle" : detalle,
                    rs.getDouble("importe_calculado"), fecha == null ? "-" : fecha.toString()
                ));
            }
        }
        return lista;
    }
}
