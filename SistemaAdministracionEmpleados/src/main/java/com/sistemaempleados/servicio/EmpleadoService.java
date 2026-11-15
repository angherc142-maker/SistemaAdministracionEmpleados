package com.sistemaempleados.servicio;

import com.sistemaempleados.modelo.Empleado;
import com.sistemaempleados.modelo.EmpleadoPrueba;
import com.sistemaempleados.mysql.persistencia.EsquemaBD;
import com.sistemaempleados.mysql.persistencia.MySqlConexion;
import com.sistemaempleados.mysql.persistencia.PersistenciaException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Servicio de gestión de empleados. Valida los datos y los guarda en MySQL
 * (tabla empleados), de modo que se conserven al cerrar la aplicación y
 * aparezcan en los reportes.
 *
 * Correspondencia con la tabla: documento -> cedula, y se agregan las
 * columnas codigo, fecha_ingreso y activo si todavía no existen.
 *
 * @author Hércules
 */
public class EmpleadoService {

    private static boolean esquemaListo = false;

    public EmpleadoService() {
        prepararEsquema();
    }

    // Registrar empleado
    public void registrar(Empleado empleado) {

        validarCampos(empleado);

        if (buscar(empleado.getCodigo()) != null) {
            throw new IllegalArgumentException(
                "El código del empleado ya existe."
            );
        }

        String sql = "INSERT INTO empleados "
                + "(codigo, cedula, nombre, cargo, salario_base, fecha_ingreso, activo) "
                + "VALUES (?, ?, ?, ?, 0, ?, 1)";

        try (Connection c = MySqlConexion.getConexion();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, empleado.getCodigo().trim());
            ps.setString(2, empleado.getDocumento().trim());
            ps.setString(3, empleado.getNombre().trim());
            ps.setString(4, empleado.getCargo().trim());
            ps.setDate(5, Date.valueOf(empleado.getFechaIngreso()));
            ps.executeUpdate();
        } catch (SQLIntegrityConstraintViolationException ex) {
            throw new IllegalArgumentException(
                "El código o el documento ya están registrados."
            );
        } catch (SQLException ex) {
            throw errorBD("registrar el empleado", ex);
        }
    }

    // Buscar por código
    public Empleado buscar(String codigo) {

        if (codigo == null || codigo.isBlank()) {
            return null;
        }

        String sql = "SELECT codigo, nombre, cedula, cargo, fecha_ingreso, activo "
                + "FROM empleados WHERE codigo = ?";

        try (Connection c = MySqlConexion.getConexion();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, codigo.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? aEmpleado(rs) : null;
            }
        } catch (SQLException ex) {
            throw errorBD("buscar el empleado", ex);
        }
    }

    // Consultar todos los empleados
    public List<Empleado> listar() {

        List<Empleado> lista = new ArrayList<>();
        String sql = "SELECT codigo, nombre, cedula, cargo, fecha_ingreso, activo "
                + "FROM empleados WHERE codigo IS NOT NULL AND fecha_ingreso IS NOT NULL "
                + "ORDER BY id";

        try (Connection c = MySqlConexion.getConexion();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                lista.add(aEmpleado(rs));
            }
        } catch (SQLException ex) {
            // Se devuelve la lista vacía para que la ventana pueda abrirse.
            System.err.println("No se pudo listar los empleados: " + ex.getMessage());
        }

        return lista;
    }

    // Modificar empleado
    public void modificar(Empleado empleado) {

        validarCampos(empleado);

        if (buscar(empleado.getCodigo()) == null) {
            throw new IllegalArgumentException(
                "El empleado no existe."
            );
        }

        String sql = "UPDATE empleados SET nombre = ?, cedula = ?, cargo = ?, "
                + "fecha_ingreso = ? WHERE codigo = ?";

        try (Connection c = MySqlConexion.getConexion();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, empleado.getNombre().trim());
            ps.setString(2, empleado.getDocumento().trim());
            ps.setString(3, empleado.getCargo().trim());
            ps.setDate(4, Date.valueOf(empleado.getFechaIngreso()));
            ps.setString(5, empleado.getCodigo().trim());
            ps.executeUpdate();
        } catch (SQLIntegrityConstraintViolationException ex) {
            throw new IllegalArgumentException(
                "Ya existe otro empleado con ese documento."
            );
        } catch (SQLException ex) {
            throw errorBD("modificar el empleado", ex);
        }
    }

    // Inactivar empleado
    public void inactivar(String codigo) {

        Empleado empleado = buscar(codigo);

        if (empleado == null) {
            throw new IllegalArgumentException(
                "No se encontró el empleado."
            );
        }

        if (!empleado.isActivo()) {
            throw new IllegalArgumentException(
                "El empleado ya está inactivo."
            );
        }

        String sql = "UPDATE empleados SET activo = 0 WHERE codigo = ?";

        try (Connection c = MySqlConexion.getConexion();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, codigo.trim());
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw errorBD("inactivar el empleado", ex);
        }
    }

    // Validar información
    private void validarCampos(Empleado empleado) {

        if (empleado == null) {
            throw new IllegalArgumentException(
                "El empleado no puede ser nulo."
            );
        }

        if (empleado.getCodigo() == null ||
            empleado.getCodigo().isBlank()) {
            throw new IllegalArgumentException(
                "El código es obligatorio."
            );
        }

        if (empleado.getNombre() == null ||
            empleado.getNombre().isBlank()) {
            throw new IllegalArgumentException(
                "El nombre es obligatorio."
            );
        }

        if (empleado.getDocumento() == null ||
            empleado.getDocumento().isBlank()) {
            throw new IllegalArgumentException(
                "El documento es obligatorio."
            );
        }

        if (empleado.getCargo() == null ||
            empleado.getCargo().isBlank()) {
            throw new IllegalArgumentException(
                "El cargo es obligatorio."
            );
        }

        if (empleado.getFechaIngreso() == null) {
            throw new IllegalArgumentException(
                "La fecha de ingreso es obligatoria."
            );
        }

        if (empleado.getFechaIngreso().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException(
                "La fecha de ingreso no puede ser futura."
            );
        }
    }

    // Convierte una fila de la tabla en un empleado
    private Empleado aEmpleado(ResultSet rs) throws SQLException {
        Date fecha = rs.getDate("fecha_ingreso");
        Empleado e = new EmpleadoPrueba(
            rs.getString("codigo"),
            rs.getString("nombre"),
            rs.getString("cedula"),
            rs.getString("cargo"),
            fecha == null ? LocalDate.now() : fecha.toLocalDate()
        );
        e.setActivo(rs.getBoolean("activo"));
        return e;
    }

    private IllegalArgumentException errorBD(String accion, SQLException ex) {
        return new IllegalArgumentException(
            "No se pudo " + accion + ": error de base de datos ("
            + ex.getMessage() + ")."
        );
    }

    // Crea la base y la tabla si faltan, y agrega las columnas nuevas una sola vez
    private static synchronized void prepararEsquema() {

        if (esquemaListo) {
            return;
        }

        try {
            EsquemaBD.inicializar();

            try (Connection c = MySqlConexion.getConexion();
                 Statement st = c.createStatement()) {

                if (!existeColumna(c, "codigo")) {
                    st.execute("ALTER TABLE empleados "
                            + "ADD COLUMN codigo VARCHAR(30) NULL, "
                            + "ADD UNIQUE KEY uq_empleados_codigo (codigo)");
                }
                if (!existeColumna(c, "fecha_ingreso")) {
                    st.execute("ALTER TABLE empleados ADD COLUMN fecha_ingreso DATE NULL");
                }
                if (!existeColumna(c, "activo")) {
                    st.execute("ALTER TABLE empleados "
                            + "ADD COLUMN activo TINYINT(1) NOT NULL DEFAULT 1");
                }
            }

            esquemaListo = true;

        } catch (SQLException | PersistenciaException ex) {
            System.err.println("No se pudo preparar la tabla empleados: " + ex.getMessage());
        }
    }

    private static boolean existeColumna(Connection c, String columna) throws SQLException {
        String sql = "SELECT COUNT(*) FROM information_schema.COLUMNS "
                + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'empleados' "
                + "AND COLUMN_NAME = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, columna);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }
}
