
package com.sistemaempleados.modelo;

import java.time.LocalDate;

public class EmpleadoPorComision extends Empleado {

    private double montoVentas;
    private double porcentajeComision;

    // Constructor
    public EmpleadoPorComision(String codigo, String nombre,
            String documento, String cargo, LocalDate fechaIngreso,
            double montoVentas, double porcentajeComision) {

        super(codigo, nombre, documento, cargo, fechaIngreso);

        setMontoVentas(montoVentas);
        setPorcentajeComision(porcentajeComision);
    }

    // Métodos Get
    public double getMontoVentas() {
        return montoVentas;
    }

    public double getPorcentajeComision() {
        return porcentajeComision;
    }

    // Métodos Set con validaciones
    public void setMontoVentas(double montoVentas) {
        if (!Double.isFinite(montoVentas) || montoVentas < 0) {
            throw new IllegalArgumentException(
                    "El monto de ventas no puede ser negativo.");
        }

        this.montoVentas = montoVentas;
    }

    public void setPorcentajeComision(double porcentajeComision) {
        if (!Double.isFinite(porcentajeComision)
                || porcentajeComision <= 0
                || porcentajeComision > 100) {
            throw new IllegalArgumentException(
                    "La comisión debe estar entre 0 y 100.");
        }

        this.porcentajeComision = porcentajeComision;
    }

    // Cálculo de remuneración
    @Override
    public double calcularRemuneracion() {
        return montoVentas * (porcentajeComision / 100);
    }

    // Mostrar desglose de remuneración
    public String mostrarDesglose() {
        return "===== REMUNERACIÓN POR COMISIÓN ====="
                + "\nEmpleado: " + getNombre()
                + "\nMonto de ventas: $"
                + String.format("%.2f", montoVentas)
                + "\nPorcentaje de comisión: "
                + porcentajeComision + "%"
                + "\nRemuneración total: $"
                + String.format("%.2f", calcularRemuneracion());
    }
}
