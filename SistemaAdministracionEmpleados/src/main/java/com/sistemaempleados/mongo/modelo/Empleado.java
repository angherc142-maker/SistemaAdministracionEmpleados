package com.sistemaempleados.mongo.modelo;

import org.bson.Document;
import org.bson.types.ObjectId;

public class Empleado {
    private String id;          // ObjectId en formato hexadecimal
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

    public Document toDocument() {
        Document d = new Document("cedula", cedula)
                .append("nombre", nombre)
                .append("cargo", cargo)
                .append("salarioBase", salarioBase);
        if (id != null) d.append("_id", new ObjectId(id));
        return d;
    }

    public static Empleado fromDocument(Document d) {
        if (d == null) return null;
        Empleado e = new Empleado(
                d.getString("cedula"),
                d.getString("nombre"),
                d.getString("cargo"),
                ((Number) d.get("salarioBase")).doubleValue());
        e.id = d.getObjectId("_id").toHexString();
        return e;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
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
