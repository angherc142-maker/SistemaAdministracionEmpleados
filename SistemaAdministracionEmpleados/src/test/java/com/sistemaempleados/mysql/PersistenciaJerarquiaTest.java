package com.sistemaempleados.mysql;

import com.sistemaempleados.modelo.Empleado;
import com.sistemaempleados.modelo.EmpleadoAdministrativo;
import com.sistemaempleados.modelo.EmpleadoPorComision;
import com.sistemaempleados.modelo.EmpleadoPorHora;
import com.sistemaempleados.mysql.modelo.RegistroRemuneracion;
import com.sistemaempleados.mysql.persistencia.MySqlConexion;
import com.sistemaempleados.mysql.persistencia.RemuneracionDAO;
import com.sistemaempleados.servicio.EmpleadoService;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de almacenamiento y recuperación con la jerarquía REAL del proyecto:
 * EmpleadoAdministrativo, EmpleadoPorHora y EmpleadoPorComision, guardadas con
 * EmpleadoService y con el historial de RemuneracionDAO.
 *
 * Requiere MySQL corriendo. Usa la base remuneraciones_test para no tocar los
 * datos reales de remuneraciones_db.
 */
class PersistenciaJerarquiaTest {

    private static final double DELTA = 0.001;

    private EmpleadoService servicio;
    private RemuneracionDAO remuneraciones;

    @BeforeAll
    static void preparar() {
        Assertions.assertTrue(MySqlConexion.probarConexion(),
                "MySQL no responde: revisa que esté encendido y la contraseña en MySqlConexion");
        // Primero se apunta a la base de pruebas y después se prepara el esquema,
        // porque EmpleadoService lo prepara una sola vez por ejecución.
        MySqlConexion.setBaseDatos("remuneraciones_test");
        EmpleadoService.prepararEsquema();
    }

    @AfterAll
    static void restaurar() {
        MySqlConexion.setBaseDatos("remuneraciones_db");
    }

    @BeforeEach
    void limpiar() throws SQLException {
        // DELETE (no DROP) para conservar las columnas que agrega prepararEsquema()
        try (Connection c = MySqlConexion.getConexion(); Statement st = c.createStatement()) {
            st.execute("DELETE FROM remuneracion_valores");
            st.execute("DELETE FROM remuneraciones");
            st.execute("DELETE FROM empleados");
        }
        servicio = new EmpleadoService();
        remuneraciones = new RemuneracionDAO();
    }

    // ------------------------------------------------------------------
    // Datos de ejemplo (uno por cada tipo de empleado)
    // ------------------------------------------------------------------

    private static EmpleadoAdministrativo ana() {
        return new EmpleadoAdministrativo("EMP-001", "Ana Perez", "0801-1990-00001",
                "Analista", LocalDate.of(2023, 3, 15), 800);
    }

    private static EmpleadoPorHora luis() {
        return new EmpleadoPorHora("EMP-002", "Luis Martinez", "0801-1992-00002",
                "Tecnico de soporte", LocalDate.of(2024, 1, 8), 6.5, 160);
    }

    private static EmpleadoPorComision marta() {
        return new EmpleadoPorComision("EMP-003", "Marta Gomez", "0801-1995-00003",
                "Vendedora", LocalDate.of(2022, 6, 20), 12500, 5);
    }

    private void registrarLosTres() {
        servicio.registrar(ana());
        servicio.registrar(luis());
        servicio.registrar(marta());
    }

    /** Id interno de la tabla (EmpleadoService no lo expone). */
    private static int idDe(String codigo) throws SQLException {
        try (Connection c = MySqlConexion.getConexion();
             PreparedStatement ps = c.prepareStatement("SELECT id FROM empleados WHERE codigo = ?")) {
            ps.setString(1, codigo);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next(), "No se encontró el empleado " + codigo);
                return rs.getInt(1);
            }
        }
    }

    // ------------------------------------------------------------------
    // 1. Almacenar y recuperar los tres tipos
    // ------------------------------------------------------------------

    @Test
    void registraYRecuperaLosTresTiposConSusAtributos() {
        registrarLosTres();

        Empleado a = servicio.buscar("EMP-001");
        assertInstanceOf(EmpleadoAdministrativo.class, a);
        assertEquals("Ana Perez", a.getNombre());
        assertEquals("0801-1990-00001", a.getDocumento());
        assertEquals("Analista", a.getCargo());
        assertEquals(LocalDate.of(2023, 3, 15), a.getFechaIngreso());
        assertTrue(a.isActivo());
        assertEquals(800.0, ((EmpleadoAdministrativo) a).getSalarioBase(), DELTA);

        Empleado h = servicio.buscar("EMP-002");
        assertInstanceOf(EmpleadoPorHora.class, h);
        assertEquals(6.5, ((EmpleadoPorHora) h).getTarifaPorHora(), DELTA);
        assertEquals(160.0, ((EmpleadoPorHora) h).getHorasTrabajadas(), DELTA);

        Empleado c = servicio.buscar("EMP-003");
        assertInstanceOf(EmpleadoPorComision.class, c);
        assertEquals(12500.0, ((EmpleadoPorComision) c).getMontoVentas(), DELTA);
        assertEquals(5.0, ((EmpleadoPorComision) c).getPorcentajeComision(), DELTA);
    }

    @Test
    void listarDevuelveLosTresTiposEnOrdenDeRegistro() {
        registrarLosTres();

        List<Empleado> lista = servicio.listar();
        assertEquals(3, lista.size());
        assertInstanceOf(EmpleadoAdministrativo.class, lista.get(0));
        assertInstanceOf(EmpleadoPorHora.class, lista.get(1));
        assertInstanceOf(EmpleadoPorComision.class, lista.get(2));
    }

    // ------------------------------------------------------------------
    // 2. Calcular y guardar el historial de remuneraciones
    // ------------------------------------------------------------------

    @Test
    void calculaElImporteSegunElTipoYGuardaElHistorial() throws SQLException {
        registrarLosTres();

        assertEquals(800.0, servicio.calcularYRegistrar("EMP-001", "2026-11"), DELTA);
        assertEquals(1040.0, servicio.calcularYRegistrar("EMP-002", "2026-11"), DELTA);   // 6.50 x 160
        assertEquals(625.0, servicio.calcularYRegistrar("EMP-003", "2026-11"), DELTA);    // 12500 x 5 %

        RegistroRemuneracion ra = remuneraciones.buscarPorEmpleadoYPeriodo(idDe("EMP-001"), "2026-11");
        assertNotNull(ra);
        assertEquals(800.0, ra.getImporteCalculado(), DELTA);
        assertEquals(800.0, ra.getValores().get("salarioBase"), DELTA);

        RegistroRemuneracion rh = remuneraciones.buscarPorEmpleadoYPeriodo(idDe("EMP-002"), "2026-11");
        assertNotNull(rh);
        assertEquals(1040.0, rh.getImporteCalculado(), DELTA);
        assertEquals(6.5, rh.getValores().get("tarifaPorHora"), DELTA);
        assertEquals(160.0, rh.getValores().get("horasTrabajadas"), DELTA);

        RegistroRemuneracion rc = remuneraciones.buscarPorEmpleadoYPeriodo(idDe("EMP-003"), "2026-11");
        assertNotNull(rc);
        assertEquals(625.0, rc.getImporteCalculado(), DELTA);
        assertEquals(12500.0, rc.getValores().get("montoVentas"), DELTA);
        assertEquals(5.0, rc.getValores().get("porcentajeComision"), DELTA);
    }

    @Test
    void historialConVariosPeriodosVieneConElMasRecientePrimero() throws SQLException {
        servicio.registrar(ana());
        servicio.calcularYRegistrar("EMP-001", "2026-10");
        servicio.calcularYRegistrar("EMP-001", "2026-11");

        List<RegistroRemuneracion> historial = remuneraciones.historialPorEmpleado(idDe("EMP-001"));
        assertEquals(2, historial.size());
        assertEquals("2026-11", historial.get(0).getPeriodo());
        assertEquals("2026-10", historial.get(1).getPeriodo());
    }

    @Test
    void recalcularElMismoPeriodoActualizaElRegistroSinDuplicar() throws SQLException {
        servicio.registrar(luis());
        assertEquals(1040.0, servicio.calcularYRegistrar("EMP-002", "2026-11"), DELTA);

        // Cambian las horas del empleado y se recalcula el mismo período
        servicio.modificar(new EmpleadoPorHora("EMP-002", "Luis Martinez", "0801-1992-00002",
                "Tecnico de soporte", LocalDate.of(2024, 1, 8), 6.5, 100));
        assertEquals(650.0, servicio.calcularYRegistrar("EMP-002", "2026-11"), DELTA);

        int id = idDe("EMP-002");
        assertEquals(1, remuneraciones.historialPorEmpleado(id).size());
        RegistroRemuneracion r = remuneraciones.buscarPorEmpleadoYPeriodo(id, "2026-11");
        assertEquals(650.0, r.getImporteCalculado(), DELTA);
        assertEquals(100.0, r.getValores().get("horasTrabajadas"), DELTA);
    }

    // ------------------------------------------------------------------
    // 3. Los datos siguen disponibles después de "reiniciar"
    //    (objetos y conexiones nuevos; lo único compartido es MySQL).
    //    El reinicio real del programa se demuestra con PruebaReinicio.
    // ------------------------------------------------------------------

    @Test
    void losDatosSiguenDisponiblesConObjetosNuevos() throws SQLException {
        registrarLosTres();
        servicio.calcularYRegistrar("EMP-002", "2026-11");

        EmpleadoService otroServicio = new EmpleadoService();
        RemuneracionDAO otroDao = new RemuneracionDAO();

        assertEquals(3, otroServicio.listar().size());
        assertInstanceOf(EmpleadoPorHora.class, otroServicio.buscar("EMP-002"));
        RegistroRemuneracion r = otroDao.buscarPorEmpleadoYPeriodo(idDe("EMP-002"), "2026-11");
        assertNotNull(r);
        assertEquals(1040.0, r.getImporteCalculado(), DELTA);
    }

    @Test
    void inactivarConservaElHistorial() throws SQLException {
        servicio.registrar(ana());
        servicio.calcularYRegistrar("EMP-001", "2026-10");

        servicio.inactivar("EMP-001");

        assertFalse(servicio.buscar("EMP-001").isActivo());
        assertEquals(1, remuneraciones.historialPorEmpleado(idDe("EMP-001")).size());
    }

    // ------------------------------------------------------------------
    // 4. Reglas y errores
    // ------------------------------------------------------------------

    @Test
    void codigoDuplicadoEsRechazado() {
        servicio.registrar(ana());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> servicio.registrar(ana()));
        assertTrue(ex.getMessage().contains("ya existe"));
    }

    @Test
    void documentoDuplicadoEsRechazado() {
        servicio.registrar(ana());
        EmpleadoAdministrativo otro = new EmpleadoAdministrativo("EMP-999", "Otra Persona",
                "0801-1990-00001", "Cajera", LocalDate.of(2024, 5, 1), 500);   // mismo documento que Ana

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> servicio.registrar(otro));
        assertTrue(ex.getMessage().contains("registrados"));
        assertNull(servicio.buscar("EMP-999"));
    }

    @Test
    void periodoConFormatoInvalidoEsRechazado() {
        servicio.registrar(ana());

        for (String periodo : new String[]{"2026-13", "2026-00", "26-11", "2026/11", "", null}) {
            assertThrows(IllegalArgumentException.class,
                    () -> servicio.calcularYRegistrar("EMP-001", periodo),
                    "Debió rechazar el período: " + periodo);
        }
    }

    @Test
    void empleadoInactivoNoSeCalcula() {
        servicio.registrar(ana());
        servicio.inactivar("EMP-001");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> servicio.calcularYRegistrar("EMP-001", "2026-11"));
        assertTrue(ex.getMessage().contains("inactivo"));
        // y no se puede inactivar dos veces
        assertThrows(IllegalArgumentException.class, () -> servicio.inactivar("EMP-001"));
    }

    @Test
    void empleadoInexistenteNoSeCalcula() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> servicio.calcularYRegistrar("NO-EXISTE", "2026-11"));
        assertTrue(ex.getMessage().contains("no existe"));
    }

    @Test
    void elModeloRechazaValoresInvalidosAntesDeGuardar() {
        LocalDate fecha = LocalDate.of(2024, 1, 1);

        assertThrows(IllegalArgumentException.class,
                () -> new EmpleadoAdministrativo("X-1", "Nombre", "DOC-12345", "Cargo", fecha, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new EmpleadoPorHora("X-2", "Nombre", "DOC-12345", "Cargo", fecha, 6.5, 800));
        assertThrows(IllegalArgumentException.class,
                () -> new EmpleadoPorComision("X-3", "Nombre", "DOC-12345", "Cargo", fecha, 1000, 150));
    }

    @Test
    void elServicioRechazaDocumentoCortoYFechaFutura() {
        EmpleadoAdministrativo docCorto = new EmpleadoAdministrativo("EMP-010", "Nombre",
                "12", "Cargo", LocalDate.of(2024, 1, 1), 500);
        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class,
                () -> servicio.registrar(docCorto));
        assertTrue(ex1.getMessage().contains("5 a 20"));

        EmpleadoAdministrativo futuro = new EmpleadoAdministrativo("EMP-011", "Nombre",
                "DOC-12345", "Cargo", LocalDate.now().plusDays(1), 500);
        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class,
                () -> servicio.registrar(futuro));
        assertTrue(ex2.getMessage().contains("futura"));
    }
}