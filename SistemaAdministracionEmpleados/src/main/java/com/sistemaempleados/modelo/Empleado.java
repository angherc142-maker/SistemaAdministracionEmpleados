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

    public Empleado(String codigo, String nombre, String documento,
                    String cargo, LocalDate fechaIngreso) {
        validarCodigo(codigo);
        validarNombre(nombre);
        validarDocumento(documento);
        validarCargo(cargo);
        validarFechaIngreso(fechaIngreso);

        this.codigo = codigo.trim();
        this.nombre = nombre.trim();
        this.documento = documento.trim();
        this.cargo = cargo.trim();
        this.fechaIngreso = fechaIngreso;
        this.activo = true;
    }

    public String getCodigo() { return codigo; }
    public String getNombre() { return nombre; }
    public String getDocumento() { return documento; }
    public String getCargo() { return cargo; }
    public LocalDate getFechaIngreso() { return fechaIngreso; }
    public boolean isActivo() { return activo; }

    public void setNombre(String nombre) {
        validarNombre(nombre);
        this.nombre = nombre.trim();
    }

    public void setDocumento(String documento) {
        validarDocumento(documento);
        this.documento = documento.trim();
    }

    public void setCargo(String cargo) {
        validarCargo(cargo);
        this.cargo = cargo.trim();
    }

    public void setFechaIngreso(LocalDate fechaIngreso) {
        validarFechaIngreso(fechaIngreso);
        this.fechaIngreso = fechaIngreso;
    }

    public void setActivo(boolean activo) { this.activo = activo; }

    private static void validarCodigo(String codigo) {
        if (codigo == null || !codigo.trim().matches("[A-Za-z]{2}\\d{3}")) {
            throw new IllegalArgumentException(
                    "El código debe tener 2 letras y 3 números (por ejemplo AB123).");
        }
    }

    private static void validarNombre(String nombre) {
        if (nombre == null || !nombre.trim().matches("\\p{L}+( \\p{L}+)*")) {
            throw new IllegalArgumentException(
                    "El nombre es obligatorio y solo puede contener letras y espacios.");
        }
    }

    private static void validarDocumento(String documento) {
        if (documento == null || !documento.trim().matches("\\d{8}-\\d")) {
            throw new IllegalArgumentException(
                    "El documento debe tener el formato 00000000-0.");
        }
    }

    private static void validarCargo(String cargo) {
        if (cargo == null || cargo.isBlank()) {
            throw new IllegalArgumentException("El cargo es obligatorio.");
        }
    }

    private static void validarFechaIngreso(LocalDate fechaIngreso) {
        if (fechaIngreso == null) {
            throw new IllegalArgumentException("La fecha de ingreso es obligatoria.");
        }
        if (fechaIngreso.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha de ingreso no puede ser futura.");
        }
    }

    public abstract double calcularRemuneracion();
    //agregado pam fin
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
