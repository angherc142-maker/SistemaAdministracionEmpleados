package com.sistemaempleados.mysql.modelo;

public class Empleado {
    private int id;             // lo asigna MySQL (AUTO_INCREMENT); 0 = aún no guardado
    private String cedula;
    private String nombre;
    private String cargo;
    private double salarioBase;

    public Empleado() { }

    public Empleado(String cedula, String nombre, String cargo, double salarioBase) {
        this.cedula = cedula;
        this.nombre = nombre;
        this.cargo = cargo;
        this.salarioBase = salarioBase;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getCedula() { return cedula; }
    public void setCedula(String cedula) { this.cedula = cedula; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getCargo() { return cargo; }
    public void setCargo(String cargo) { this.cargo = cargo; }
    public double getSalarioBase() { return salarioBase; }
    public void setSalarioBase(double salarioBase) { this.salarioBase = salarioBase; }

    @Override
    public String toString() {
        return "Empleado{id=" + id + ", cedula=" + cedula + ", nombre=" + nombre
                + ", cargo=" + cargo + ", salarioBase=" + salarioBase + "}";
    }
}
