
package com.sistemaempleados.modelo;

import java.time.LocalDate;

public class EmpleadoAdministrativo extends Empleado {

    private double salarioBase;

    // Constructor
    public EmpleadoAdministrativo(String codigo, String nombre,
            String documento, String cargo, LocalDate fechaIngreso,
            double salarioBase) {

        super(codigo, nombre, documento, cargo, fechaIngreso);
        setSalarioBase(salarioBase);
    }

    // Método Get
    public double getSalarioBase() {
        return salarioBase;
    }

    // Método Set con validación
    public void setSalarioBase(double salarioBase) {
        if (!Double.isFinite(salarioBase) || salarioBase <= 0) {
            throw new IllegalArgumentException(
                    "El salario base debe ser mayor que cero.");
        }

        this.salarioBase = salarioBase;
    }

    // Cálculo de remuneración
    @Override
    public double calcularRemuneracion() {
        return salarioBase;
    }

    // Mostrar desglose de remuneración
    public String mostrarDesglose() {
        return "===== REMUNERACIÓN ADMINISTRATIVA ====="
                + "\nEmpleado: " + getNombre()
                + "\nSalario base: $" + String.format("%.2f", salarioBase)
                + "\nRemuneración total: $"
                + String.format("%.2f", calcularRemuneracion());
    }
}
