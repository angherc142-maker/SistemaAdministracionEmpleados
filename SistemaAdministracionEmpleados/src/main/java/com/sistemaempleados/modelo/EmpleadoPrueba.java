/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.sistemaempleados.modelo;

/**
 *
 * @author Hércules
 */

import java.time.LocalDate;

public class EmpleadoPrueba extends Empleado {

    public EmpleadoPrueba(String codigo, String nombre,
                          String documento, String cargo,
                          LocalDate fechaIngreso) {

        super(codigo, nombre, documento, cargo, fechaIngreso);
    }
    // agregado pam 
        @Override
    public double calcularRemuneracion() {
        return 0.0;
    }
}
