package com.sistemaempleados.mysql.persistencia;

import com.sistemaempleados.mysql.modelo.RegistroRemuneracion;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Acceso al historial. Los registros se insertan y consultan, pero no se sobrescriben. */
public class RemuneracionDAO {
    public RemuneracionDAO() { EsquemaBD.inicializar(); }

    /** Inserta un cálculo nuevo; la clave única empleado/período evita duplicados. */
    public void guardar(RegistroRemuneracion r) {
        String sql = "INSERT INTO remuneraciones (empleado_id, periodo, importe_calculado, fecha_calculo) VALUES (?, ?, ?, ?)";
        try (Connection c = MySqlConexion.getConexion()) {
            c.setAutoCommit(false);
            try {
                int id;
                try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, r.getEmpleadoId());
                    ps.setString(2, r.getPeriodo());
                    ps.setDouble(3, r.getImporteCalculado());
                    ps.setTimestamp(4, new Timestamp(r.getFechaCalculo().getTime()));
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (!keys.next()) throw new SQLException("MySQL no devolvió el identificador del cálculo.");
                        id = keys.getInt(1);
                    }
                }
                String detalleSql = "INSERT INTO remuneracion_valores (remuneracion_id, concepto, monto) VALUES (?, ?, ?)";
                try (PreparedStatement ps = c.prepareStatement(detalleSql)) {
                    for (Map.Entry<String, Double> v : r.getValores().entrySet()) {
                        ps.setInt(1, id);
                        ps.setString(2, v.getKey());
                        ps.setDouble(3, v.getValue());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
                c.commit();
                r.setId(id);
            } catch (SQLException | RuntimeException ex) {
                c.rollback();
                throw ex;
            } finally {
                c.setAutoCommit(true);
            }
        } catch (SQLException ex) {
            throw new PersistenciaException("No se pudo registrar el cálculo. Verifique que el empleado exista y que no haya otro cálculo para ese período.", ex);
        }
    }

    public RegistroRemuneracion buscarPorEmpleadoYPeriodo(int empleadoId, String periodo) {
        List<RegistroRemuneracion> lista = consultar(
                "SELECT * FROM remuneraciones WHERE empleado_id = ? AND periodo = ?", empleadoId, periodo);
        return lista.isEmpty() ? null : lista.get(0);
    }

    public List<RegistroRemuneracion> historialPorEmpleado(int empleadoId) {
        return consultar("SELECT * FROM remuneraciones WHERE empleado_id = ? ORDER BY periodo DESC", empleadoId);
    }

    public List<RegistroRemuneracion> listarPorPeriodo(String periodo) {
        return consultar("SELECT * FROM remuneraciones WHERE periodo = ? ORDER BY empleado_id", periodo);
    }

    private List<RegistroRemuneracion> consultar(String sql, Object... parametros) {
        List<RegistroRemuneracion> lista = new ArrayList<>();
        try (Connection c = MySqlConexion.getConexion(); PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < parametros.length; i++) ps.setObject(i + 1, parametros[i]);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    RegistroRemuneracion r = new RegistroRemuneracion();
                    r.setId(rs.getInt("id"));
                    r.setEmpleadoId(rs.getInt("empleado_id"));
                    r.setPeriodo(rs.getString("periodo"));
                    r.setImporteCalculado(rs.getDouble("importe_calculado"));
                    r.setFechaCalculo(rs.getTimestamp("fecha_calculo"));
                    r.setValores(cargarValores(c, r.getId()));
                    lista.add(r);
                }
            }
            return lista;
        } catch (SQLException ex) {
            throw new PersistenciaException("No se pudo consultar las remuneraciones", ex);
        }
    }

    private Map<String, Double> cargarValores(Connection c, int remuneracionId) throws SQLException {
        Map<String, Double> valores = new LinkedHashMap<>();
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT concepto, monto FROM remuneracion_valores WHERE remuneracion_id = ? ORDER BY concepto")) {
            ps.setInt(1, remuneracionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) valores.put(rs.getString("concepto"), rs.getDouble("monto"));
            }
        }
        return valores;
    }
}
