package com.sistemaempleados.servicio;

import com.sistemaempleados.modelo.Empleado;
import com.sistemaempleados.modelo.EmpleadoAdministrativo;
import com.sistemaempleados.modelo.EmpleadoPorComision;
import com.sistemaempleados.modelo.EmpleadoPorHora;
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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    /** Formato del documento: 8 dígitos, guion y dígito verificador (00000000-0). */
    private static final String FORMATO_DOCUMENTO = "\\d{8}-\\d";

    /** Formato del código: 2 letras y 3 números (por ejemplo AB123). */
    private static final String FORMATO_CODIGO = "[A-Za-z]{2}\\d{3}";
    
        /** Nombre: solo letras (con tildes y ñ) separadas por espacios. */
    private static final String FORMATO_NOMBRE = "\\p{L}+( \\p{L}+)*";

    private static final String COLUMNAS =
            "codigo, nombre, cedula, cargo, fecha_ingreso, activo, tipo, "
            + "salario_base, tarifa_hora, horas_trabajadas, monto_ventas, porcentaje_comision";

    // Bases de datos cuyo esquema ya fue preparado (puede cambiar en las pruebas)
    private static final Set<String> ESQUEMAS_LISTOS = new HashSet<>();

    public EmpleadoService() {
        prepararEsquema();
    }

    // Registrar empleado
    public void registrar(Empleado empleado) {

        validarCampos(empleado);

        if (existeCodigo(empleado.getCodigo())) {
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

    // Buscar por código (no distingue mayúsculas)
    public Empleado buscar(String codigo) {

        if (codigo == null || codigo.isBlank()) {
            return null;
        }

        return buscarUno("codigo = ? COLLATE utf8mb4_unicode_ci", codigo.trim());
    }

    // Buscar por documento (formato 00000000-0)
    public Empleado buscarPorDocumento(String documento) {

        if (documento == null || documento.isBlank()) {
            return null;
        }

        return buscarUno("cedula = ?", documento.trim());
    }

    // Buscar por nombre (coincidencia parcial, sin distinguir mayúsculas)
    public List<Empleado> buscarPorNombre(String nombre) {

        List<Empleado> lista = new ArrayList<>();

        if (nombre == null || nombre.isBlank()) {
            return lista;
        }

        String sql = "SELECT " + COLUMNAS + " FROM empleados "
                + "WHERE nombre LIKE ? AND codigo IS NOT NULL ORDER BY nombre";

        try (Connection c = MySqlConexion.getConexion();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, "%" + nombre.trim() + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    agregarSiEsValido(lista, rs);
                }
            }
        } catch (SQLException ex) {
            throw errorBD("buscar por nombre", ex);
        }

        return lista;
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
                agregarSiEsValido(lista, rs);
            }
        } catch (SQLException ex) {
            throw errorBD("listar los empleados", ex);
        }

        return lista;
    }

    // Modificar empleado (el tipo no puede cambiarse una vez registrado)
    public void modificar(Empleado empleado) {

        validarCampos(empleado);

        Empleado actual = buscar(empleado.getCodigo());

        if (actual == null) {
            throw new IllegalArgumentException(
                "El empleado no existe."
            );
        }

        if (!nombreTipo(actual).equals(nombreTipo(empleado))) {
            throw new IllegalArgumentException(
                "No se puede cambiar el tipo de un empleado ya registrado ("
                + nombreTipo(actual) + ")."
            );
        }

        String sql = "UPDATE empleados SET nombre = ?, cedula = ?, cargo = ?, "
                + "fecha_ingreso = ?, tipo = ?, salario_base = ?, tarifa_hora = ?, "
                + "horas_trabajadas = ?, monto_ventas = ?, porcentaje_comision = ? "
                + "WHERE codigo = ? COLLATE utf8mb4_unicode_ci";

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

        String sql = "UPDATE empleados SET activo = 0 "
                + "WHERE codigo = ? COLLATE utf8mb4_unicode_ci";

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
     * historial (tablas remuneraciones y remuneracion_valores). Un empleado
     * tiene a lo sumo un cálculo por período: los cálculos guardados no se
     * sobrescriben.
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
                "Datos incompletos: el empleado no tiene tipo definido."
            );
        }

        double importe = empleado.calcularRemuneracion();

        try {
            RemuneracionDAO dao = new RemuneracionDAO();
            int id = obtenerId(codigo);

            if (dao.buscarPorEmpleadoYPeriodo(id, periodo) != null) {
                throw new IllegalArgumentException(
                    "Ya existe un cálculo de este empleado para el período "
                    + periodo + "."
                );
            }

            dao.guardar(new RegistroRemuneracion(id, periodo, valores, importe));

        } catch (PersistenciaException ex) {
            throw new IllegalArgumentException(
                "No se pudo registrar la remuneración: " + ex.getMessage()
            );
        }

        return importe;
    }

    // Nombre del tipo de un empleado (o "Sin definir")
    public static String nombreTipo(Empleado empleado) {

        if (empleado instanceof EmpleadoAdministrativo) {
            return TIPO_ADMINISTRATIVO;
        }
        if (empleado instanceof EmpleadoPorHora) {
            return TIPO_POR_HORA;
        }
        if (empleado instanceof EmpleadoPorComision) {
            return TIPO_POR_COMISION;
        }
        return "Sin definir";
    }

    // Validar información
    private void validarCampos(Empleado empleado) {

        if (empleado == null) {
            throw new IllegalArgumentException(
                "El empleado no puede ser nulo."
            );
        }

        if (!(empleado instanceof EmpleadoAdministrativo
                || empleado instanceof EmpleadoPorHora
                || empleado instanceof EmpleadoPorComision)) {
            throw new IllegalArgumentException(
                "Debe seleccionar un tipo de empleado válido "
                + "(administrativo, por hora o por comisión)."
            );
        }

        if (empleado.getCodigo() == null ||
            empleado.getCodigo().isBlank()) {
            throw new IllegalArgumentException(
                "El código es obligatorio."
            );
        }

        if (empleado.getCodigo().trim().length() > 30) {
            throw new IllegalArgumentException(
                "El código no puede superar 30 caracteres."
            );
        }
        if (!empleado.getCodigo().trim().matches(FORMATO_CODIGO)) {
            throw new IllegalArgumentException(
                "El código debe tener 2 letras y 3 números (por ejemplo AB123)."
            );
        }
        if (empleado.getNombre() == null ||
            empleado.getNombre().isBlank()) {
            throw new IllegalArgumentException(
                "El nombre es obligatorio."
            );
        }
        if (!empleado.getNombre().trim().matches(FORMATO_NOMBRE)) {
            throw new IllegalArgumentException(
                "El nombre solo puede contener letras y espacios."
            );
        }

        if (empleado.getDocumento() == null ||
            empleado.getDocumento().isBlank()) {
            throw new IllegalArgumentException(
                "El documento es obligatorio."
            );
        }

        if (!empleado.getDocumento().trim().matches(FORMATO_DOCUMENTO)) {
            throw new IllegalArgumentException(
                "El documento debe tener el formato 00000000-0 "
                + "(8 dígitos, guion y dígito verificador)."
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

    private boolean existeCodigo(String codigo) {
        String sql = "SELECT 1 FROM empleados WHERE codigo = ? COLLATE utf8mb4_unicode_ci";
        try (Connection c = MySqlConexion.getConexion();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, codigo.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException ex) {
            throw errorBD("verificar el código", ex);
        }
    }

    private Empleado buscarUno(String condicion, String valor) {

        String sql = "SELECT " + COLUMNAS + " FROM empleados WHERE " + condicion;

        try (Connection c = MySqlConexion.getConexion();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, valor);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? aEmpleado(rs) : null;
            }
        } catch (SQLException ex) {
            throw errorBD("buscar el empleado", ex);
        }
    }

    // Las filas con datos incompletos o tipo indefinido no se listan
    private void agregarSiEsValido(List<Empleado> lista, ResultSet rs) throws SQLException {
        try {
            lista.add(aEmpleado(rs));
        } catch (IllegalArgumentException ex) {
            System.err.println("Se omitió un empleado con datos incompletos: "
                    + ex.getMessage());
        }
    }

    // Convierte una fila de la tabla en el tipo de empleado que corresponde
    private Empleado aEmpleado(ResultSet rs) throws SQLException {

        String codigo = rs.getString("codigo");
        String nombre = rs.getString("nombre");
        String documento = rs.getString("cedula");
        String cargo = rs.getString("cargo");
        Date f = rs.getDate("fecha_ingreso");
        String tipo = rs.getString("tipo");

        if (codigo == null || f == null) {
            throw new IllegalArgumentException(
                "Datos incompletos del empleado " + documento + "."
            );
        }

        LocalDate fecha = f.toLocalDate();
        double salario = rs.getDouble("salario_base");

        Empleado e;

        try {
            // Filas del prototipo anterior: sin tipo pero con salario base
            if (TIPO_ADMINISTRATIVO.equals(tipo) || (tipo == null && salario > 0)) {
                e = new EmpleadoAdministrativo(codigo, nombre, documento, cargo, fecha,
                        salario);
            } else if (TIPO_POR_HORA.equals(tipo)) {
                e = new EmpleadoPorHora(codigo, nombre, documento, cargo, fecha,
                        rs.getDouble("tarifa_hora"), rs.getDouble("horas_trabajadas"));
            } else if (TIPO_POR_COMISION.equals(tipo)) {
                e = new EmpleadoPorComision(codigo, nombre, documento, cargo, fecha,
                        rs.getDouble("monto_ventas"), rs.getDouble("porcentaje_comision"));
            } else {
                throw new IllegalArgumentException(
                    "El empleado " + codigo + " no tiene tipo definido."
                );
            }
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                "Datos incompletos o inválidos del empleado " + codigo
                + ": " + ex.getMessage()
            );
        }

        e.setActivo(rs.getBoolean("activo"));
        return e;
    }

    // Asigna 6 parámetros: tipo, salario_base, tarifa_hora, horas, ventas, porcentaje
    private static void asignarTipo(PreparedStatement ps, Empleado e, int i)
            throws SQLException {

        String tipo = nombreTipo(e);
        Double salario = null;
        Double tarifa = null;
        Double horas = null;
        Double ventas = null;
        Double porcentaje = null;

        if (e instanceof EmpleadoAdministrativo a) {
            salario = a.getSalarioBase();
        } else if (e instanceof EmpleadoPorHora h) {
            tarifa = h.getTarifaPorHora();
            horas = h.getHorasTrabajadas();
        } else if (e instanceof EmpleadoPorComision c) {
            ventas = c.getMontoVentas();
            porcentaje = c.getPorcentajeComision();
        }

        ps.setString(i, tipo);
        asignarDecimal(ps, i + 1, salario);
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
        String sql = "SELECT id FROM empleados WHERE codigo = ? COLLATE utf8mb4_unicode_ci";
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

    // Crea la base y las tablas si faltan y agrega las columnas nuevas (una vez por base)
    public static synchronized void prepararEsquema() {

        String bd = MySqlConexion.getBaseDatos();

        if (ESQUEMAS_LISTOS.contains(bd)) {
            return;
        }

        try {
            EsquemaBD.inicializar();

            try (Connection c = MySqlConexion.getConexion();
                 Statement st = c.createStatement()) {

                if (!existeColumna(c, "codigo")) {
                    st.execute("ALTER TABLE empleados "
                            + "ADD COLUMN codigo VARCHAR(30) "
                            + "CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL, "
                            + "ADD UNIQUE KEY uq_empleados_codigo (codigo)");
                }
                agregarColumna(c, st, "fecha_ingreso", "DATE NULL");
                agregarColumna(c, st, "activo", "TINYINT(1) NOT NULL DEFAULT 1");
                agregarColumna(c, st, "tipo", "VARCHAR(20) NULL");
                agregarColumna(c, st, "tarifa_hora", "DECIMAL(12,2) NULL");
                agregarColumna(c, st, "horas_trabajadas", "DECIMAL(8,2) NULL");
                agregarColumna(c, st, "monto_ventas", "DECIMAL(12,2) NULL");
                agregarColumna(c, st, "porcentaje_comision", "DECIMAL(5,2) NULL");

                // El salario base solo aplica al tipo administrativo
                st.execute("ALTER TABLE empleados MODIFY salario_base DECIMAL(12,2) NULL");
                st.executeUpdate("UPDATE empleados SET salario_base = NULL "
                        + "WHERE tipo IS NOT NULL AND tipo <> '" + TIPO_ADMINISTRATIVO + "'");

                // Las mismas validaciones de las clases, repetidas en la base
                agregarCheck(c, st, "chk_salario_base",
                        "salario_base IS NULL OR salario_base > 0");
                agregarCheck(c, st, "chk_tarifa_hora",
                        "tarifa_hora IS NULL OR tarifa_hora > 0");
                agregarCheck(c, st, "chk_horas_trabajadas",
                        "horas_trabajadas IS NULL OR (horas_trabajadas > 0 AND horas_trabajadas <= 744)");
                agregarCheck(c, st, "chk_monto_ventas",
                        "monto_ventas IS NULL OR monto_ventas >= 0");
                agregarCheck(c, st, "chk_porcentaje_comision",
                        "porcentaje_comision IS NULL OR (porcentaje_comision > 0 AND porcentaje_comision <= 100)");
            }

            ESQUEMAS_LISTOS.add(bd);

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

    // Si algún dato antiguo no cumple la regla, se omite la restricción sin detener la aplicación
    private static void agregarCheck(Connection c, Statement st,
            String nombre, String condicion) {
        try {
            if (!existeRestriccion(c, nombre)) {
                st.execute("ALTER TABLE empleados ADD CONSTRAINT " + nombre
                        + " CHECK (" + condicion + ")");
            }
        } catch (SQLException ex) {
            System.err.println("No se pudo crear la restricción " + nombre
                    + ": " + ex.getMessage());
        }
    }

    private static boolean existeRestriccion(Connection c, String nombre) throws SQLException {
        String sql = "SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS "
                + "WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = 'empleados' "
                + "AND CONSTRAINT_NAME = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, nombre);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
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