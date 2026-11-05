package com.sistemaempleados.mysql.persistencia;
import com.sistemaempleados.mysql.modelo.Empleado;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class EmpleadoDAO {

    public EmpleadoDAO() {
        EsquemaBD.inicializar();
    }

    public Empleado insertar(Empleado e) {
        String sql = "INSERT INTO empleados (cedula, nombre, cargo, salario_base) VALUES (?, ?, ?, ?)";
        try (Connection c = MySqlConexion.getConexion();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, e.getCedula());
            ps.setString(2, e.getNombre());
            ps.setString(3, e.getCargo());
            ps.setDouble(4, e.getSalarioBase());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) e.setId(rs.getInt(1));
            }
            return e;
        } catch (SQLException ex) {
            throw new PersistenciaException("No se pudo insertar el empleado (¿cédula repetida?)", ex);
        }
    }

    public Empleado buscarPorId(int id) {
        return buscarUno("SELECT * FROM empleados WHERE id = ?", id);
    }

    public Empleado buscarPorCedula(String cedula) {
        return buscarUno("SELECT * FROM empleados WHERE cedula = ?", cedula);
    }

    public List<Empleado> listar() {
        List<Empleado> lista = new ArrayList<>();
        try (Connection c = MySqlConexion.getConexion();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM empleados ORDER BY id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapear(rs));
            return lista;
        } catch (SQLException ex) {
            throw new PersistenciaException("No se pudo listar los empleados", ex);
        }
    }

    public boolean actualizar(Empleado e) {
        String sql = "UPDATE empleados SET cedula = ?, nombre = ?, cargo = ?, salario_base = ? WHERE id = ?";
        try (Connection c = MySqlConexion.getConexion(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, e.getCedula());
            ps.setString(2, e.getNombre());
            ps.setString(3, e.getCargo());
            ps.setDouble(4, e.getSalarioBase());
            ps.setInt(5, e.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            throw new PersistenciaException("No se pudo actualizar el empleado", ex);
        }
    }

    public boolean eliminar(int id) {
        try (Connection c = MySqlConexion.getConexion();
             PreparedStatement ps = c.prepareStatement("DELETE FROM empleados WHERE id = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            throw new PersistenciaException("No se pudo eliminar el empleado (¿tiene remuneraciones?)", ex);
        }
    }

    private Empleado buscarUno(String sql, Object parametro) {
        try (Connection c = MySqlConexion.getConexion(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, parametro);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        } catch (SQLException ex) {
            throw new PersistenciaException("No se pudo buscar el empleado", ex);
        }
    }

    private Empleado mapear(ResultSet rs) throws SQLException {
        Empleado e = new Empleado(rs.getString("cedula"), rs.getString("nombre"),
                rs.getString("cargo"), rs.getDouble("salario_base"));
        e.setId(rs.getInt("id"));
        return e;
    }
}
