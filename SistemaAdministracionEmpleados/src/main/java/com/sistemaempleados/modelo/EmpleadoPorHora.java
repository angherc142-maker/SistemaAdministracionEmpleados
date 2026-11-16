
package com.sistemaempleados.modelo;

import java.time.LocalDate;

public class EmpleadoPorHora extends Empleado {

    private double tarifaPorHora;
    private double horasTrabajadas;

    // Constructor
    public EmpleadoPorHora(String codigo, String nombre,
            String documento, String cargo, LocalDate fechaIngreso,
            double tarifaPorHora, double horasTrabajadas) {

        super(codigo, nombre, documento, cargo, fechaIngreso);

        setTarifaPorHora(tarifaPorHora);
        setHorasTrabajadas(horasTrabajadas);
    }

    // Métodos Get
    public double getTarifaPorHora() {
        return tarifaPorHora;
    }

    public double getHorasTrabajadas() {
        return horasTrabajadas;
    }

    // Métodos Set con validaciones
    public void setTarifaPorHora(double tarifaPorHora) {
        if (!Double.isFinite(tarifaPorHora) || tarifaPorHora < 0) {
            throw new IllegalArgumentException(
                    "La tarifa por hora no puede ser negativa.");
        }

        this.tarifaPorHora = tarifaPorHora;
    }

    public void setHorasTrabajadas(double horasTrabajadas) {
        if (!Double.isFinite(horasTrabajadas)
                || horasTrabajadas < 0
                || horasTrabajadas > 744) {
            throw new IllegalArgumentException(
                    "Las horas trabajadas deben estar entre 0 y 744.");
        }

        this.horasTrabajadas = horasTrabajadas;
    }

    // Cálculo de remuneración
    @Override
    public double calcularRemuneracion() {
        return tarifaPorHora * horasTrabajadas;
    }

    // Mostrar desglose de remuneración
    public String mostrarDesglose() {
        return "===== REMUNERACIÓN POR HORA ====="
                + "\nEmpleado: " + getNombre()
                + "\nTarifa por hora: $"
                + String.format("%.2f", tarifaPorHora)
                + "\nHoras trabajadas: " + horasTrabajadas
                + "\nRemuneración total: $"
                + String.format("%.2f", calcularRemuneracion());
    }
}
