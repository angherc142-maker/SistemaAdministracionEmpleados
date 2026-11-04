package com.sistemaempleados.mongo;

import com.mongodb.client.MongoDatabase;
import com.sistemaempleados.mongo.modelo.Empleado;
import com.sistemaempleados.mongo.modelo.RegistroRemuneracion;
import com.sistemaempleados.mongo.persistencia.EmpleadoDAO;
import com.sistemaempleados.mongo.persistencia.MongoConexion;
import com.sistemaempleados.mongo.persistencia.RemuneracionDAO;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Requiere MongoDB corriendo. Usa una BD aparte para no tocar los datos reales. */
class PersistenciaTest {

    private static MongoDatabase bd;
    private EmpleadoDAO empleados;
    private RemuneracionDAO remuneraciones;

    @BeforeAll
    static void conectar() {
        Assertions.assertTrue(MongoConexion.probarConexion(), "MongoDB no está disponible");
        bd = MongoConexion.getCliente().getDatabase("remuneraciones_test");
    }

    @BeforeEach
    void limpiar() {
        bd.drop();
        empleados = new EmpleadoDAO(bd);
        remuneraciones = new RemuneracionDAO(bd);
    }

    @AfterAll
    static void cerrar() {
        bd.drop();
        MongoConexion.cerrar();
    }

    @Test
    void guardaYRecuperaEmpleado() {
        Empleado e = empleados.insertar(new Empleado("001", "Ana", "Analista", 800));
        assertNotNull(e.getId());
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
        assertThrows(com.mongodb.MongoWriteException.class,
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
        assertEquals("2026-11", historial.get(0).getPeriodo()); // más reciente primero
        assertEquals(200.0, historial.get(0).getValores().get("bonos"));
        assertEquals(1700.0, historial.get(0).getImporteCalculado());
    }

    @Test
    void recalcularMismoPeriodoReemplazaElRegistro() {
        Empleado e = empleados.insertar(new Empleado("005", "Pedro", "Chofer", 500));
        remuneraciones.guardar(new RegistroRemuneracion(e.getId(), "2026-11", Map.of("salarioBase", 500.0), 500.0));
        remuneraciones.guardar(new RegistroRemuneracion(e.getId(), "2026-11", Map.of("salarioBase", 500.0), 550.0));
        assertEquals(1, remuneraciones.historialPorEmpleado(e.getId()).size());
        assertEquals(550.0, remuneraciones.buscarPorEmpleadoYPeriodo(e.getId(), "2026-11").getImporteCalculado());
    }

    @Test
    void persisteDespuesDeCerrarYReabrirLaConexion() {
        Empleado e = empleados.insertar(new Empleado("006", "Rosa", "Contadora", 900));
        remuneraciones.guardar(new RegistroRemuneracion(e.getId(), "2026-11", Map.of("salarioBase", 900.0), 900.0));

        // Simula reinicio de la aplicación: se cierra el cliente y se abre uno nuevo
        MongoConexion.cerrar();
        MongoDatabase bdNueva = MongoConexion.getCliente().getDatabase("remuneraciones_test");

        assertNotNull(new EmpleadoDAO(bdNueva).buscarPorCedula("006"));
        assertEquals(1, new RemuneracionDAO(bdNueva).historialPorEmpleado(e.getId()).size());
        bd = bdNueva;
    }
}
