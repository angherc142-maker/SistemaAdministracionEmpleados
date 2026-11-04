package com.sistemaempleados.mongo.persistencia;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.sistemaempleados.mongo.modelo.Empleado;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Filters.eq;

public class EmpleadoDAO {

    private final MongoCollection<Document> coleccion;

    public EmpleadoDAO() {
        this(MongoConexion.getBaseDatos());
    }

    public EmpleadoDAO(MongoDatabase bd) {
        this.coleccion = bd.getCollection("empleados");
        // La cédula no puede repetirse
        coleccion.createIndex(Indexes.ascending("cedula"), new IndexOptions().unique(true));
    }

    /** Inserta y asigna el id generado al objeto. */
    public Empleado insertar(Empleado e) {
        Document d = e.toDocument();
        coleccion.insertOne(d);
        e.setId(d.getObjectId("_id").toHexString());
        return e;
    }

    public Empleado buscarPorId(String id) {
        return Empleado.fromDocument(coleccion.find(eq("_id", new ObjectId(id))).first());
    }

    public Empleado buscarPorCedula(String cedula) {
        return Empleado.fromDocument(coleccion.find(eq("cedula", cedula)).first());
    }

    public List<Empleado> listar() {
        List<Empleado> lista = new ArrayList<>();
        for (Document d : coleccion.find()) lista.add(Empleado.fromDocument(d));
        return lista;
    }

    /** @return true si se modificó algún documento */
    public boolean actualizar(Empleado e) {
        Document cambios = new Document("cedula", e.getCedula())
                .append("nombre", e.getNombre())
                .append("cargo", e.getCargo())
                .append("salarioBase", e.getSalarioBase());
        return coleccion.updateOne(eq("_id", new ObjectId(e.getId())),
                new Document("$set", cambios)).getMatchedCount() > 0;
    }

    public boolean eliminar(String id) {
        return coleccion.deleteOne(eq("_id", new ObjectId(id))).getDeletedCount() > 0;
    }
}
