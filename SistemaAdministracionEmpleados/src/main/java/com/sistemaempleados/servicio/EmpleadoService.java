package com.sistemaempleados.servicio;

import com.sistemaempleados.modelo.Empleado;
import com.sistemaempleados.modelo.EmpleadoAdministrativo;
import com.sistemaempleados.modelo.EmpleadoPorComision;
import com.sistemaempleados.modelo.EmpleadoPorHora;
import com.sistemaempleados.modelo.EmpleadoPrueba;
import com.sistemaempleados.mysql.modelo.RegistroRemuneracion;
import com.sistemaempleados.mysql.persistencia.EsquemaBD;
import com.sistemaempleados.mysql.persistencia.MySqlConexion;
import com.sistemaempleados.mysql.persistencia.PersistenciaException;
import com.sistemaempleados.mysql.persistencia.RemuneracionDAO;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Servicio de gestión de empleados. Valida los datos, los guarda en MySQL
 * (tabla empleados) y calcula y registra las remuneraciones según el tipo
 * de empleado (administrativo, por hora o por comisión).
 *
 * Correspondencia con la tabla: documento -> cedula. Las columnas codigo,
 * fecha_ingreso, activo, tipo, tarifa_hora, horas_trabajadas, monto_ventas
 * y porcentaje_comision se agregan automáticamente si todavía no existen.
 *
 * @author Hércules
 */
public class EmpleadoService {

    public static final String TIPO_ADMINISTRATIVO = "Administrativo";
    public static final String TIPO_POR_HORA = "Por hora";
    public static final String TIPO_POR_COMISION = "Por comisión";

    private static final String COLUMNAS =
            "codigo, nombre, cedula, cargo, fecha_ingreso, activo, tipo, "
            + "salario_base, tarifa_hora, horas_trabajadas, monto_ventas, porcentaje_comision";

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
                + "(codigo, cedula, nombre, cargo, fecha_ingreso, activo, "
                + "tipo, salario_base, tarifa_hora, horas_trabajadas, "
                + "monto_ventas, porcentaje_comision) "
                + "VALUES (?, ?, ?, ?, ?, 1, ?, ?, ?, ?, ?, ?)";

        try (Connection c = MySqlConexion.getConexion();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, empleado.getCodigo().trim());
            ps.setString(2, empleado.getDocumento().trim());
            ps.setString(3, empleado.getNombre().trim());
            ps.setString(4, empleado.getCargo().trim());
            ps.setDate(5, Date.valueOf(empleado.getFechaIngreso()));
            asignarTipo(ps, empleado, 6);
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

        String sql = "SELECT " + COLUMNAS + " FROM empleados WHERE codigo = ?";

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
        String sql = "SELECT " + COLUMNAS + " FROM empleados "
                + "WHERE codigo IS NOT NULL AND fecha_ingreso IS NOT NULL "
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
                + "fecha_ingreso = ?, tipo = ?, salario_base = ?, tarifa_hora = ?, "
                + "horas_trabajadas = ?, monto_ventas = ?, porcentaje_comision = ? "
                + "WHERE codigo = ?";

        try (Connection c = MySqlConexion.getConexion();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, empleado.getNombre().trim());
            ps.setString(2, empleado.getDocumento().trim());
            ps.setString(3, empleado.getCargo().trim());
            ps.setDate(4, Date.valueOf(empleado.getFechaIngreso()));
            asignarTipo(ps, empleado, 5);
            ps.setString(11, empleado.getCodigo().trim());
            ps.executeUpdate();
        } catch (SQLIntegrityConstraintViolationException ex) {
            throw new IllegalArgumentException(
                "Ya existe otro empleado con ese documento."
            );
        } catch (SQLException ex) {
            throw errorBD("modificar el empleado", ex);
        }
    }

    // Inactivar empleado (se conserva su información histórica)
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

    /**
     * Calcula la remuneración del empleado según su tipo y la registra en el
     * historial (tablas remuneraciones y remuneracion_valores).
     *
     * @param codigo  código del empleado
     * @param periodo período con formato AAAA-MM, por ejemplo 2026-11
     * @return importe calculado
     */
    public double calcularYRegistrar(String codigo, String periodo) {

        if (periodo == null || !periodo.matches("\\d{4}-(0[1-9]|1[0-2])")) {
            throw new IllegalArgumentException(
                "El período debe tener el formato AAAA-MM (por ejemplo 2026-11)."
            );
        }

        Empleado empleado = buscar(codigo);

        if (empleado == null) {
            throw new IllegalArgumentException(
                "No se puede calcular: el empleado no existe."
            );
        }

        if (!empleado.isActivo()) {
            throw new IllegalArgumentException(
                "No se puede calcular la remuneración de un empleado inactivo."
            );
        }

        Map<String, Double> valores = new LinkedHashMap<>();

        if (empleado instanceof EmpleadoAdministrativo a) {
            valores.put("salarioBase", a.getSalarioBase());
        } else if (empleado instanceof EmpleadoPorHora h) {
            valores.put("tarifaPorHora", h.getTarifaPorHora());
            valores.put("horasTrabajadas", h.getHorasTrabajadas());
        } else if (empleado instanceof EmpleadoPorComision c) {
            valores.put("montoVentas", c.getMontoVentas());
            valores.put("porcentajeComision", c.getPorcentajeComision());
        } else {
            throw new IllegalArgumentException(
                "El empleado no tiene tipo definido. Modifíquelo y elija su tipo."
            );
        }

        double importe = empleado.calcularRemuneracion();

        try {
            new RemuneracionDAO().guardar(
                new RegistroRemuneracion(obtenerId(codigo), periodo, valores, importe)
            );
        } catch (PersistenciaException ex) {
            throw new IllegalArgumentException(
                "No se pudo registrar la remuneración: " + ex.getMessage()
            );
        }

        return importe;
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

        // Formato del documento: ajústelo si el equipo define otro formato.
        if (!empleado.getDocumento().trim().matches("[A-Za-z0-9-]{5,20}")) {
            throw new IllegalArgumentException(
                "El documento debe tener de 5 a 20 caracteres "
                + "(letras, números o guiones)."
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

    // Convierte una fila de la tabla en el tipo de empleado que corresponde
    private Empleado aEmpleado(ResultSet rs) throws SQLException {

        String codigo = rs.getString("codigo");
        String nombre = rs.getString("nombre");
        String documento = rs.getString("cedula");
        String cargo = rs.getString("cargo");
        Date f = rs.getDate("fecha_ingreso");
        LocalDate fecha = f == null ? LocalDate.now() : f.toLocalDate();
        String tipo = rs.getString("tipo");

        Empleado e;

        try {
            if (TIPO_ADMINISTRATIVO.equals(tipo)) {
                e = new EmpleadoAdministrativo(codigo, nombre, documento, cargo, fecha,
                        rs.getDouble("salario_base"));
            } else if (TIPO_POR_HORA.equals(tipo)) {
                e = new EmpleadoPorHora(codigo, nombre, documento, cargo, fecha,
                        rs.getDouble("tarifa_hora"), rs.getDouble("horas_trabajadas"));
            } else if (TIPO_POR_COMISION.equals(tipo)) {
                e = new EmpleadoPorComision(codigo, nombre, documento, cargo, fecha,
                        rs.getDouble("monto_ventas"), rs.getDouble("porcentaje_comision"));
            } else {
                e = new EmpleadoPrueba(codigo, nombre, documento, cargo, fecha);
            }
        } catch (IllegalArgumentException ex) {
            // Datos guardados que no cumplen las reglas del tipo: se muestran sin tipo.
            e = new EmpleadoPrueba(codigo, nombre, documento, cargo, fecha);
        }

        e.setActivo(rs.getBoolean("activo"));
        return e;
    }

    // Asigna 6 parámetros: tipo, salario_base, tarifa_hora, horas, ventas, porcentaje
    private static void asignarTipo(PreparedStatement ps, Empleado e, int i)
            throws SQLException {

        String tipo = null;
        double salario = 0;
        Double tarifa = null;
        Double horas = null;
        Double ventas = null;
        Double porcentaje = null;

        if (e instanceof EmpleadoAdministrativo a) {
            tipo = TIPO_ADMINISTRATIVO;
            salario = a.getSalarioBase();
        } else if (e instanceof EmpleadoPorHora h) {
            tipo = TIPO_POR_HORA;
            tarifa = h.getTarifaPorHora();
            horas = h.getHorasTrabajadas();
        } else if (e instanceof EmpleadoPorComision c) {
            tipo = TIPO_POR_COMISION;
            ventas = c.getMontoVentas();
            porcentaje = c.getPorcentajeComision();
        }

        ps.setString(i, tipo);
        ps.setDouble(i + 1, salario);
        asignarDecimal(ps, i + 2, tarifa);
        asignarDecimal(ps, i + 3, horas);
        asignarDecimal(ps, i + 4, ventas);
        asignarDecimal(ps, i + 5, porcentaje);
    }

    private static void asignarDecimal(PreparedStatement ps, int indice, Double valor)
            throws SQLException {
        if (valor == null) {
            ps.setNull(indice, Types.DECIMAL);
        } else {
            ps.setDouble(indice, valor);
        }
    }

    private int obtenerId(String codigo) {
        String sql = "SELECT id FROM empleados WHERE codigo = ?";
        try (Connection c = MySqlConexion.getConexion();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, codigo.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
                throw new IllegalArgumentException("El empleado no existe.");
            }
        } catch (SQLException ex) {
            throw errorBD("consultar el empleado", ex);
        }
    }

    private IllegalArgumentException errorBD(String accion, SQLException ex) {
        return new IllegalArgumentException(
            "No se pudo " + accion + ": error de base de datos ("
            + ex.getMessage() + ")."
        );
    }

    // Crea la base y las tablas si faltan, y agrega las columnas nuevas una sola vez
    public static synchronized void prepararEsquema() {

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
                agregarColumna(c, st, "fecha_ingreso", "DATE NULL");
                agregarColumna(c, st, "activo", "TINYINT(1) NOT NULL DEFAULT 1");
                agregarColumna(c, st, "tipo", "VARCHAR(20) NULL");
                agregarColumna(c, st, "tarifa_hora", "DECIMAL(12,2) NULL");
                agregarColumna(c, st, "horas_trabajadas", "DECIMAL(8,2) NULL");
                agregarColumna(c, st, "monto_ventas", "DECIMAL(12,2) NULL");
                agregarColumna(c, st, "porcentaje_comision", "DECIMAL(5,2) NULL");
            }

            esquemaListo = true;

        } catch (SQLException | PersistenciaException ex) {
            System.err.println("No se pudo preparar la tabla empleados: " + ex.getMessage());
        }
    }

    private static void agregarColumna(Connection c, Statement st,
            String columna, String definicion) throws SQLException {
        if (!existeColumna(c, columna)) {
            st.execute("ALTER TABLE empleados ADD COLUMN " + columna + " " + definicion);
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
