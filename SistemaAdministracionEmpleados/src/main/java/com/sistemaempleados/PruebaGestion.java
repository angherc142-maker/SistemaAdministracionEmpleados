/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.sistemaempleados;

import com.sistemaempleados.modelo.Empleado;
import com.sistemaempleados.modelo.EmpleadoAdministrativo;
import com.sistemaempleados.modelo.EmpleadoPorComision;
import com.sistemaempleados.modelo.EmpleadoPorHora;
import com.sistemaempleados.servicio.EmpleadoService;
import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Prueba manual por consola (requiere MySQL activo): registra un empleado de
 * cada tipo, prueba duplicados y calcula la remuneración de cada uno.
 * Los empleados quedan en la base de datos configurada en MySqlConexion.
 *
 * @author Hércules
 */
public class PruebaGestion {

    public static void main(String[] args) {

        EmpleadoService servicio = new EmpleadoService();
        String periodo = YearMonth.now().toString();

        System.out.println("================================");
        System.out.println(" SISTEMA DE GESTION DE EMPLEADOS");
        System.out.println("================================");

        System.out.println("\nPRUEBA 1: REGISTRAR UN EMPLEADO DE CADA TIPO");
        Empleado[] empleados = {
            new EmpleadoAdministrativo("EMP001", "Carlos Perez", "12345678-9",
                    "Contador", LocalDate.of(2024, 1, 15), 800),
            new EmpleadoPorHora("EMP002", "Ana Lopez", "23456789-0",
                    "Operaria", LocalDate.of(2024, 3, 1), 5.5, 160),
            new EmpleadoPorComision("EMP003", "Luis Mena", "34567890-1",
                    "Vendedor", LocalDate.of(2024, 6, 10), 10000, 5)
        };

        for (Empleado e : empleados) {
            try {
                servicio.registrar(e);
                System.out.println("Registrado: " + e.getCodigo());
            } catch (IllegalArgumentException ex) {
                System.out.println("No registrado (" + e.getCodigo() + "): " + ex.getMessage());
            }
        }

        System.out.println("\nPRUEBA 2: CODIGO DUPLICADO (sin distinguir mayusculas)");
        try {
            servicio.registrar(new EmpleadoAdministrativo("emp001", "Otro", "45678901-2",
                    "Cargo", LocalDate.of(2024, 1, 1), 500));
            System.out.println("ERROR: se permitio un codigo duplicado");
        } catch (IllegalArgumentException ex) {
            System.out.println("Correcto: " + ex.getMessage());
        }

        System.out.println("\nPRUEBA 3: DOCUMENTO CON FORMATO INVALIDO");
        try {
            servicio.registrar(new EmpleadoAdministrativo("EMP004", "Otro", "ABC123",
                    "Cargo", LocalDate.of(2024, 1, 1), 500));
            System.out.println("ERROR: se permitio un documento invalido");
        } catch (IllegalArgumentException ex) {
            System.out.println("Correcto: " + ex.getMessage());
        }

        System.out.println("\nPRUEBA 4: CALCULAR REMUNERACIONES (" + periodo + ")");
        for (Empleado e : empleados) {
            try {
                double importe = servicio.calcularYRegistrar(e.getCodigo(), periodo);
                System.out.printf("%s: %.2f%n", e.getCodigo(), importe);
            } catch (IllegalArgumentException ex) {
                System.out.println(e.getCodigo() + ": " + ex.getMessage());
            }
        }

        System.out.println("\nPRUEBA 5: INACTIVAR Y CALCULAR");
        try {
            servicio.inactivar("EMP001");
            servicio.calcularYRegistrar("EMP001", "2000-01");
            System.out.println("ERROR: se calculo un empleado inactivo");
        } catch (IllegalArgumentException ex) {
            System.out.println("Correcto: " + ex.getMessage());
        }
    }
}