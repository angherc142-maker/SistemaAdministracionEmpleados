package com.sistemaempleados.mysql.persistencia;
import com.sistemaempleados.mysql.modelo.RegistroRemuneracion;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RemuneracionDAO {

    public RemuneracionDAO() {
        EsquemaBD.inicializar();
    }

    public void guardar(RegistroRemuneracion r) {
        String upsert = "INSERT INTO remuneraciones (empleado_id, periodo, importe_calculado, fecha_calculo) "
                + "VALUES (?, ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE importe_calculado = VALUES(importe_calculado), "
                + "fecha_calculo = VALUES(fecha_calculo)";
        try (Connection c = MySqlConexion.getConexion()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement ps = c.prepareStatement(upsert)) {
                    ps.setInt(1, r.getEmpleadoId());
                    ps.setString(2, r.getPeriodo());
                    ps.setDouble(3, r.getImporteCalculado());
                    ps.setTimestamp(4, new Timestamp(r.getFechaCalculo().getTime()));
                    ps.executeUpdate();
                }
                int id;
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT id FROM remuneraciones WHERE empleado_id = ? AND periodo = ?")) {
                    ps.setInt(1, r.getEmpleadoId());
                    ps.setString(2, r.getPeriodo());
                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        id = rs.getInt(1);
                    }
                }
                try (PreparedStatement ps = c.prepareStatement(
                        "DELETE FROM remuneracion_valores WHERE remuneracion_id = ?")) {
                    ps.setInt(1, id);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO remuneracion_valores (remuneracion_id, concepto, monto) VALUES (?, ?, ?)")) {
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
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException ex) {
            throw new PersistenciaException("No se pudo guardar la remuneración (¿existe el empleado?)", ex);
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

    public boolean actualizarImporte(int empleadoId, String periodo, double nuevoImporte) {
        String sql = "UPDATE remuneraciones SET importe_calculado = ? WHERE empleado_id = ? AND periodo = ?";
        try (Connection c = MySqlConexion.getConexion(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDouble(1, nuevoImporte);
            ps.setInt(2, empleadoId);
            ps.setString(3, periodo);
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            throw new PersistenciaException("No se pudo actualizar el importe", ex);
        }
    }

    public boolean eliminar(int empleadoId, String periodo) {
        String sql = "DELETE FROM remuneraciones WHERE empleado_id = ? AND periodo = ?";
        try (Connection c = MySqlConexion.getConexion(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, empleadoId);
            ps.setString(2, periodo);
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            throw new PersistenciaException("No se pudo eliminar la remuneración", ex);
        }
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
