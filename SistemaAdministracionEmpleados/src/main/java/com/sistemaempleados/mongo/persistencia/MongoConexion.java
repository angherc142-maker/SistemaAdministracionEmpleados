package com.sistemaempleados.mongo.persistencia;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

/** Conexión única (singleton) a MongoDB. */
public final class MongoConexion {

    private static final String URI =
            System.getenv().getOrDefault("MONGO_URI", "mongodb://localhost:27017");
    public static final String NOMBRE_BD = "remuneraciones_db";

    private static MongoClient cliente;

    private MongoConexion() { }

    public static synchronized MongoClient getCliente() {
        if (cliente == null) {
            cliente = MongoClients.create(URI);
        }
        return cliente;
    }

    public static MongoDatabase getBaseDatos() {
        return getCliente().getDatabase(NOMBRE_BD);
    }

    /** Verifica que el servidor responda. */
    public static boolean probarConexion() {
        try {
            getBaseDatos().runCommand(new org.bson.Document("ping", 1));
            return true;
        } catch (Exception e) {
            System.err.println("No se pudo conectar a MongoDB: " + e.getMessage());
            return false;
        }
    }

    public static synchronized void cerrar() {
        if (cliente != null) {
            cliente.close();
            cliente = null;
        }
    }
}
