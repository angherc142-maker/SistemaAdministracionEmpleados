package com.sistemaempleados.mongo.modelo;

import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Historial de un cálculo de remuneración.
 * "valores" guarda los datos usados en el cálculo (salarioBase, horasExtra, bonos, deducciones, etc.)
 */
public class RegistroRemuneracion {
    private String id;
    private String empleadoId;
    private String periodo;                       // formato "AAAA-MM", ej. "2026-11"
    private Map<String, Double> valores = new LinkedHashMap<>();
    private double importeCalculado;
    private Date fechaCalculo = new Date();

    public RegistroRemuneracion() { }

    public RegistroRemuneracion(String empleadoId, String periodo,
                                Map<String, Double> valores, double importeCalculado) {
        this.empleadoId = empleadoId;
        this.periodo = periodo;
        this.valores = new LinkedHashMap<>(valores);
        this.importeCalculado = importeCalculado;
    }

    public Document toDocument() {
        Document v = new Document();
        valores.forEach(v::append);
        Document d = new Document("empleadoId", new ObjectId(empleadoId))
                .append("periodo", periodo)
                .append("valores", v)
                .append("importeCalculado", importeCalculado)
                .append("fechaCalculo", fechaCalculo);
        if (id != null) d.append("_id", new ObjectId(id));
        return d;
    }

    public static RegistroRemuneracion fromDocument(Document d) {
        if (d == null) return null;
        RegistroRemuneracion r = new RegistroRemuneracion();
        r.id = d.getObjectId("_id").toHexString();
        r.empleadoId = d.getObjectId("empleadoId").toHexString();
        r.periodo = d.getString("periodo");
        Document v = d.get("valores", Document.class);
        if (v != null) {
            v.forEach((k, val) -> r.valores.put(k, ((Number) val).doubleValue()));
        }
        r.importeCalculado = ((Number) d.get("importeCalculado")).doubleValue();
        r.fechaCalculo = d.getDate("fechaCalculo");
        return r;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getEmpleadoId() { return empleadoId; }
    public void setEmpleadoId(String empleadoId) { this.empleadoId = empleadoId; }
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
