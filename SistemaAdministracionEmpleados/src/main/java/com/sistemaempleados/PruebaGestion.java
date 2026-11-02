/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.sistemaempleados;

/**
 *
 * @author Hércules
 */
import com.sistemaempleados.modelo.EmpleadoPrueba;
import com.sistemaempleados.servicio.EmpleadoService;
import java.time.LocalDate;

public class PruebaGestion {

    public static void main(String[] args) {

        EmpleadoService servicio = new EmpleadoService();

        System.out.println("================================");
        System.out.println(" SISTEMA DE GESTION DE EMPLEADOS");
        System.out.println("================================");

        // PRUEBA 1: Registrar empleado
        System.out.println("\nPRUEBA 1: REGISTRAR EMPLEADO");

        EmpleadoPrueba empleado1 = new EmpleadoPrueba(
                "EMP001",
                "Carlos Perez",
                "12345678-9",
                "Contador",
                LocalDate.of(2026, 1, 15)
        );

        servicio.registrar(empleado1);

        System.out.println("Empleado registrado correctamente.");

        // PRUEBA 2: Buscar empleado
        System.out.println("\nPRUEBA 2: BUSCAR EMPLEADO");

        System.out.println(servicio.buscar("EMP001"));

        // PRUEBA 3: Evitar código duplicado
        System.out.println("\nPRUEBA 3: CODIGO DUPLICADO");

        try {
            EmpleadoPrueba duplicado = new EmpleadoPrueba(
                    "EMP001",
                    "Maria Lopez",
                    "98765432-1",
                    "Secretaria",
                    LocalDate.of(2026, 2, 10)
            );

            servicio.registrar(duplicado);

        } catch (IllegalArgumentException e) {
            System.out.println("Validacion correcta: " + e.getMessage());
        }

        // PRUEBA 4: Modificar empleado
        System.out.println("\nPRUEBA 4: MODIFICAR EMPLEADO");

        empleado1.setCargo("Jefe de contabilidad");
        servicio.modificar(empleado1);

        System.out.println(servicio.buscar("EMP001"));

        // PRUEBA 5: Inactivar empleado
        System.out.println("\nPRUEBA 5: INACTIVAR EMPLEADO");

        servicio.inactivar("EMP001");

        System.out.println(servicio.buscar("EMP001"));

        System.out.println("\n================================");
        System.out.println(" FIN DE LAS PRUEBAS");
        System.out.println("================================");
    }
}
