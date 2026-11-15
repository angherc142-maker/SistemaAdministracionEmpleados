/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.sistemaempleados.modelo;

/**
 *
 * @author hercu
 *
 * Usuario del sistema (administrador de nómina). La contraseña nunca se
 * guarda en texto: solo se conserva su hash con sal.
 */
public class Usuario {
 
    private final String nombreUsuario;
 
    public Usuario(String nombreUsuario) {
        if (nombreUsuario == null || nombreUsuario.isBlank()) {
            throw new IllegalArgumentException("El usuario es obligatorio.");
        }
        this.nombreUsuario = nombreUsuario.trim();
    }
 
    public String getNombreUsuario() {
        return nombreUsuario;
    }
}
 
