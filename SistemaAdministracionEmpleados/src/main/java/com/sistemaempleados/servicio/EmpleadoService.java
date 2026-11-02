/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.sistemaempleados.servicio;

/**
 *
 * @author Hércules
 */
import com.sistemaempleados.modelo.Empleado;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class EmpleadoService {

    // Lista temporal de empleados
    private final List<Empleado> empleados = new ArrayList<>();

    // Registrar empleado
    public void registrar(Empleado empleado) {

        validarCampos(empleado);

        if (buscar(empleado.getCodigo()) != null) {
            throw new IllegalArgumentException(
                "El código del empleado ya existe."
            );
        }

        empleados.add(empleado);
    }

    // Buscar por código
    public Empleado buscar(String codigo) {

        if (codigo == null || codigo.isBlank()) {
            return null;
        }

        for (Empleado empleado : empleados) {
            if (empleado.getCodigo().equalsIgnoreCase(codigo.trim())) {
                return empleado;
            }
        }

        return null;
    }

    // Consultar todos los empleados
    public List<Empleado> listar() {
        return new ArrayList<>(empleados);
    }

    // Modificar empleado
    public void modificar(Empleado empleado) {

        validarCampos(empleado);

        Empleado existente = buscar(empleado.getCodigo());

        if (existente == null) {
            throw new IllegalArgumentException(
                "El empleado no existe."
            );
        }

        existente.setNombre(empleado.getNombre());
        existente.setDocumento(empleado.getDocumento());
        existente.setCargo(empleado.getCargo());
        existente.setFechaIngreso(empleado.getFechaIngreso());
    }

    // Inactivar empleado
    public void inactivar(String codigo) {

        Empleado empleado = buscar(codigo);

        if (empleado == null) {
            throw new IllegalArgumentException(
                "No se encontró el empleado."
            );
        }

        if (!empleado.isActivo()) {
            throw new IllegalArgumentException(
                "El empleado ya está inactivo."
            );
        }

        empleado.setActivo(false);
    }

    // Validar información
    private void validarCampos(Empleado empleado) {

        if (empleado == null) {
            throw new IllegalArgumentException(
                "El empleado no puede ser nulo."
            );
        }

        if (empleado.getCodigo() == null ||
            empleado.getCodigo().isBlank()) {
            throw new IllegalArgumentException(
                "El código es obligatorio."
            );
        }

        if (empleado.getNombre() == null ||
            empleado.getNombre().isBlank()) {
            throw new IllegalArgumentException(
                "El nombre es obligatorio."
            );
        }

        if (empleado.getDocumento() == null ||
            empleado.getDocumento().isBlank()) {
            throw new IllegalArgumentException(
                "El documento es obligatorio."
            );
        }

        if (empleado.getCargo() == null ||
            empleado.getCargo().isBlank()) {
            throw new IllegalArgumentException(
                "El cargo es obligatorio."
            );
        }

        if (empleado.getFechaIngreso() == null) {
            throw new IllegalArgumentException(
                "La fecha de ingreso es obligatoria."
            );
        }

        if (empleado.getFechaIngreso().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException(
                "La fecha de ingreso no puede ser futura."
            );
        }
    }
}
