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

public abstract class Empleado {

    // Atributos comunes
    private String codigo;
    private String nombre;
    private String documento;
    private String cargo;
    private LocalDate fechaIngreso;
    private boolean activo;

    // Constructor
    public Empleado(String codigo, String nombre,
                    String documento, String cargo,
                    LocalDate fechaIngreso) {

        this.codigo = codigo;
        this.nombre = nombre;
        this.documento = documento;
        this.cargo = cargo;
        this.fechaIngreso = fechaIngreso;
        this.activo = true;
    }

    // Métodos Get mjkd
    public String getCodigo() {
        return codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDocumento() {
        return documento;
    }

    public String getCargo() {
        return cargo;
    }

    public LocalDate getFechaIngreso() {
        return fechaIngreso;
    }

    public boolean isActivo() {
        return activo;
    }

    // Métodos Set
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public void setDocumento(String documento) {
        this.documento = documento;
    }

    public void setCargo(String cargo) {
        this.cargo = cargo;
    }

    public void setFechaIngreso(LocalDate fechaIngreso) {
        this.fechaIngreso = fechaIngreso;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }
    // agregado Pam inicio
    // Calcular remuneración según el tipo de empleado
public abstract double calcularRemuneracion();
    // agregado pam fin

    // Mostrar información del empleado
    @Override
    public String toString() {
        return "Código: " + codigo
                + "\nNombre: " + nombre
                + "\nDocumento: " + documento
                + "\nCargo: " + cargo
                + "\nFecha de ingreso: " + fechaIngreso
                + "\nEstado: " + (activo ? "Activo" : "Inactivo");
    }
}
