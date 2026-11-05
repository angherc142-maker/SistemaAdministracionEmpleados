package com.sistemaempleados.mysql.modelo;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Historial de un cálculo de remuneración.
 * "valores" guarda los datos usados (salarioBase, horasExtra, bonos, deducciones...).
 */
public class RegistroRemuneracion {
    private int id;
    private int empleadoId;
    private String periodo;                        // formato "AAAA-MM", ej. "2026-11"
    private Map<String, Double> valores = new LinkedHashMap<>();
    private double importeCalculado;
    private Date fechaCalculo = new Date();

    public RegistroRemuneracion() { }

    public RegistroRemuneracion(int empleadoId, String periodo,
                                Map<String, Double> valores, double importeCalculado) {
        this.empleadoId = empleadoId;
        this.periodo = periodo;
        this.valores = new LinkedHashMap<>(valores);
        this.importeCalculado = importeCalculado;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getEmpleadoId() { return empleadoId; }
    public void setEmpleadoId(int empleadoId) { this.empleadoId = empleadoId; }
    public String getPeriodo() { return periodo; }
    public void setPeriodo(String periodo) { this.periodo = periodo; }
    public Map<String, Double> getValores() { return valores; }
    public void setValores(Map<String, Double> valores) { this.valores = valores; }
    public double getImporteCalculado() { return importeCalculado; }
    public void setImporteCalculado(double importeCalculado) { this.importeCalculado = importeCalculado; }
    public Date getFechaCalculo() { return fechaCalculo; }
    public void setFechaCalculo(Date fechaCalculo) { this.fechaCalculo = fechaCalculo; }

    @Override
    public String toString() {
        return "Remuneracion{empleadoId=" + empleadoId + ", periodo=" + periodo
                + ", valores=" + valores + ", importe=" + importeCalculado + "}";
    }
}
