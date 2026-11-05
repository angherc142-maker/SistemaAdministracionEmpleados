package com.sistemaempleados.mysql;

import com.sistemaempleados.mysql.modelo.Empleado;
import com.sistemaempleados.mysql.modelo.RegistroRemuneracion;
import com.sistemaempleados.mysql.persistencia.EmpleadoDAO;
import com.sistemaempleados.mysql.persistencia.EsquemaBD;
import com.sistemaempleados.mysql.persistencia.MySqlConexion;
import com.sistemaempleados.mysql.persistencia.PersistenciaException;
import com.sistemaempleados.mysql.persistencia.RemuneracionDAO;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Requiere MySQL corriendo. Usa la base remuneraciones_test para no tocar los datos reales. */
class PersistenciaTest {

    private EmpleadoDAO empleados;
    private RemuneracionDAO remuneraciones;

    @BeforeAll
    static void preparar() {
        Assertions.assertTrue(MySqlConexion.probarConexion(),
                "MySQL no responde: revisa que esté encendido y la contraseña en MySqlConexion");
        MySqlConexion.setBaseDatos("remuneraciones_test");
        EsquemaBD.inicializar();
    }

    @AfterAll
    static void restaurar() {
        MySqlConexion.setBaseDatos("remuneraciones_db");
    }

    @BeforeEach
    void limpiar() throws SQLException {
        try (Connection c = MySqlConexion.getConexion(); Statement st = c.createStatement()) {
            st.execute("DELETE FROM remuneracion_valores");
            st.execute("DELETE FROM remuneraciones");
            st.execute("DELETE FROM empleados");
        }
        empleados = new EmpleadoDAO();
        remuneraciones = new RemuneracionDAO();
    }

    @Test
    void guardaYRecuperaEmpleado() {
        Empleado e = empleados.insertar(new Empleado("001", "Ana", "Analista", 800));
        assertTrue(e.getId() > 0);
        Empleado leido = empleados.buscarPorId(e.getId());
        assertEquals("Ana", leido.getNombre());
        assertEquals(800, leido.getSalarioBase());
        assertEquals(e.getId(), empleados.buscarPorCedula("001").getId());
    }

    @Test
    void actualizaYEliminaEmpleado() {
        Empleado e = empleados.insertar(new Empleado("002", "Luis", "Soporte", 600));
        e.setSalarioBase(700);
        assertTrue(empleados.actualizar(e));
        assertEquals(700, empleados.buscarPorId(e.getId()).getSalarioBase());
        assertTrue(empleados.eliminar(e.getId()));
        assertNull(empleados.buscarPorId(e.getId()));
    }

    @Test
    void cedulaDuplicadaEsRechazada() {
        empleados.insertar(new Empleado("003", "Uno", "X", 500));
        assertThrows(PersistenciaException.class,
                () -> empleados.insertar(new Empleado("003", "Dos", "Y", 500)));
    }

    @Test
    void guardaYRecuperaHistorialDeRemuneraciones() {
        Empleado e = empleados.insertar(new Empleado("004", "Marta", "Gerente", 1500));
        remuneraciones.guardar(new RegistroRemuneracion(e.getId(), "2026-10",
                Map.of("salarioBase", 1500.0, "bonos", 100.0), 1600.0));
        remuneraciones.guardar(new RegistroRemuneracion(e.getId(), "2026-11",
                Map.of("salarioBase", 1500.0, "bonos", 200.0), 1700.0));

        List<RegistroRemuneracion> historial = remuneraciones.historialPorEmpleado(e.getId());
        assertEquals(2, historial.size());
        assertEquals("2026-11", historial.get(0).getPeriodo());      // más reciente primero
        assertEquals(200.0, historial.get(0).getValores().get("bonos"));
        assertEquals(1700.0, historial.get(0).getImporteCalculado());
    }

    @Test
    void recalcularMismoPeriodoReemplazaElRegistro() {
        Empleado e = empleados.insertar(new Empleado("005", "Pedro", "Chofer", 500));
        remuneraciones.guardar(new RegistroRemuneracion(e.getId(), "2026-11",
                Map.of("salarioBase", 500.0, "bonos", 10.0), 510.0));
        remuneraciones.guardar(new RegistroRemuneracion(e.getId(), "2026-11",
                Map.of("salarioBase", 500.0), 550.0));

        assertEquals(1, remuneraciones.historialPorEmpleado(e.getId()).size());
        RegistroRemuneracion r = remuneraciones.buscarPorEmpleadoYPeriodo(e.getId(), "2026-11");
        assertEquals(550.0, r.getImporteCalculado());
        assertEquals(1, r.getValores().size());                       // los valores viejos se reemplazaron
    }

    @Test
    void remuneracionDeEmpleadoInexistenteEsRechazada() {
        assertThrows(PersistenciaException.class, () -> remuneraciones.guardar(
                new RegistroRemuneracion(999999, "2026-11", Map.of("salarioBase", 1.0), 1.0)));
    }

    @Test
    void datosSiguenDisponiblesConNuevosDAO() {
        Empleado e = empleados.insertar(new Empleado("006", "Rosa", "Contadora", 900));
        remuneraciones.guardar(new RegistroRemuneracion(e.getId(), "2026-11",
                Map.of("salarioBase", 900.0), 900.0));

        // Objetos nuevos y conexiones nuevas: lo único que comparten es MySQL
        EmpleadoDAO otroEmp = new EmpleadoDAO();
        RemuneracionDAO otroRem = new RemuneracionDAO();
        assertNotNull(otroEmp.buscarPorCedula("006"));
        assertEquals(1, otroRem.historialPorEmpleado(e.getId()).size());
    }
}
