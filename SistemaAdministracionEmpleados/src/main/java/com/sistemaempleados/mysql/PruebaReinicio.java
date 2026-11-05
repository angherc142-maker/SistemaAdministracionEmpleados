package com.sistemaempleados.mysql;
import com.sistemaempleados.mysql.modelo.Empleado;
import com.sistemaempleados.mysql.modelo.RegistroRemuneracion;
import com.sistemaempleados.mysql.persistencia.EmpleadoDAO;
import com.sistemaempleados.mysql.persistencia.MySqlConexion;
import com.sistemaempleados.mysql.persistencia.RemuneracionDAO;

import java.util.Map;

public class PruebaReinicio {

    private static final String MODO = "leer";

    public static void main(String[] args) {
        if (!MySqlConexion.probarConexion()) return;
        String modo = args.length > 0 ? args[0] : MODO;

        EmpleadoDAO empDao = new EmpleadoDAO();
        RemuneracionDAO remDao = new RemuneracionDAO();

        if (modo.equals("guardar")) {
            Empleado e = empDao.buscarPorCedula("0801-1990-00001");
            if (e == null) {
                e = empDao.insertar(new Empleado("0801-1990-00001", "Ana Pérez", "Analista", 800));
            }
            remDao.guardar(new RegistroRemuneracion(e.getId(), "2026-11",
                    Map.of("salarioBase", 800.0, "horasExtra", 120.0, "deducciones", 90.0), 830.0));
            System.out.println("Datos guardados. Cierra la app y ejecuta con 'leer'.");
        } else {
            for (Empleado e : empDao.listar()) {
                System.out.println(e);
                remDao.historialPorEmpleado(e.getId()).forEach(r -> System.out.println("   " + r));
            }
        }
    }
}
