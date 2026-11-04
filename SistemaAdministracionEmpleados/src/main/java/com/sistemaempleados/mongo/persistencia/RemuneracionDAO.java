package com.sistemaempleados.mongo.persistencia;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.Sorts;
import com.sistemaempleados.mongo.modelo.RegistroRemuneracion;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

public class RemuneracionDAO {

    private final MongoCollection<Document> coleccion;

    public RemuneracionDAO() {
        this(MongoConexion.getBaseDatos());
    }

    public RemuneracionDAO(MongoDatabase bd) {
        this.coleccion = bd.getCollection("remuneraciones");
        // Un solo registro por empleado y período
        coleccion.createIndex(Indexes.ascending("empleadoId", "periodo"),
                new IndexOptions().unique(true));
    }

    /**
     * Guarda el cálculo. Si ya existe uno para ese empleado y período,
     * lo reemplaza (recálculo); si no, lo crea.
     */
    public void guardar(RegistroRemuneracion r) {
        Document d = r.toDocument();
        d.remove("_id");
        coleccion.replaceOne(
                and(eq("empleadoId", new ObjectId(r.getEmpleadoId())), eq("periodo", r.getPeriodo())),
                d, new ReplaceOptions().upsert(true));
    }

    public RegistroRemuneracion buscarPorEmpleadoYPeriodo(String empleadoId, String periodo) {
        return RegistroRemuneracion.fromDocument(coleccion.find(
                and(eq("empleadoId", new ObjectId(empleadoId)), eq("periodo", periodo))).first());
    }

    /** Historial completo de un empleado, del período más reciente al más antiguo. */
    public List<RegistroRemuneracion> historialPorEmpleado(String empleadoId) {
        List<RegistroRemuneracion> lista = new ArrayList<>();
        for (Document d : coleccion.find(eq("empleadoId", new ObjectId(empleadoId)))
                .sort(Sorts.descending("periodo"))) {
            lista.add(RegistroRemuneracion.fromDocument(d));
        }
        return lista;
    }

    public List<RegistroRemuneracion> listarPorPeriodo(String periodo) {
        List<RegistroRemuneracion> lista = new ArrayList<>();
        for (Document d : coleccion.find(eq("periodo", periodo))) {
            lista.add(RegistroRemuneracion.fromDocument(d));
        }
        return lista;
    }

    public boolean actualizarImporte(String empleadoId, String periodo, double nuevoImporte) {
        return coleccion.updateOne(
                and(eq("empleadoId", new ObjectId(empleadoId)), eq("periodo", periodo)),
                new Document("$set", new Document("importeCalculado", nuevoImporte)))
                .getMatchedCount() > 0;
    }

    public boolean eliminar(String empleadoId, String periodo) {
        return coleccion.deleteOne(
                and(eq("empleadoId", new ObjectId(empleadoId)), eq("periodo", periodo)))
                .getDeletedCount() > 0;
    }
}
