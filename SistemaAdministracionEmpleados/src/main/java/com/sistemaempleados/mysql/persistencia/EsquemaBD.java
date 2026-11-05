package com.sistemaempleados.mysql.persistencia;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

public final class EsquemaBD {

    private static final Set<String> LISTAS = new HashSet<>();

    private EsquemaBD() { }

    public static synchronized void inicializar() {
        String bd = MySqlConexion.getBaseDatos();
        if (LISTAS.contains(bd)) return;
        try {
            try (Connection c = MySqlConexion.getConexionServidor(); Statement st = c.createStatement()) {
                st.execute("CREATE DATABASE IF NOT EXISTS `" + bd
                        + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
            }
            try (Connection c = MySqlConexion.getConexion(); Statement st = c.createStatement()) {
                st.execute("CREATE TABLE IF NOT EXISTS empleados ("
                        + " id INT AUTO_INCREMENT PRIMARY KEY,"
                        + " cedula VARCHAR(30) NOT NULL UNIQUE,"
                        + " nombre VARCHAR(100) NOT NULL,"
                        + " cargo VARCHAR(100),"
                        + " salario_base DECIMAL(12,2) NOT NULL)");
                st.execute("CREATE TABLE IF NOT EXISTS remuneraciones ("
                        + " id INT AUTO_INCREMENT PRIMARY KEY,"
                        + " empleado_id INT NOT NULL,"
                        + " periodo CHAR(7) NOT NULL,"
                        + " importe_calculado DECIMAL(12,2) NOT NULL,"
                        + " fecha_calculo DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                        + " UNIQUE KEY uq_empleado_periodo (empleado_id, periodo),"
                        + " FOREIGN KEY (empleado_id) REFERENCES empleados(id))");
                st.execute("CREATE TABLE IF NOT EXISTS remuneracion_valores ("
                        + " remuneracion_id INT NOT NULL,"
                        + " concepto VARCHAR(50) NOT NULL,"
                        + " monto DECIMAL(12,2) NOT NULL,"
                        + " PRIMARY KEY (remuneracion_id, concepto),"
                        + " FOREIGN KEY (remuneracion_id) REFERENCES remuneraciones(id) ON DELETE CASCADE)");
            }
            LISTAS.add(bd);
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo preparar la base de datos " + bd, e);
        }
    }
}
