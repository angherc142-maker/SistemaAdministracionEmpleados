/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.sistemaempleados.servicio;

/**
 *
 * @author Hércules
 */

import com.sistemaempleados.modelo.Usuario;
import com.sistemaempleados.mysql.persistencia.EsquemaBD;
import com.sistemaempleados.mysql.persistencia.MySqlConexion;
import com.sistemaempleados.mysql.persistencia.PersistenciaException;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Autenticación contra la tabla usuarios de MySQL. La contraseña se guarda
 * cifrada con PBKDF2 (HMAC-SHA256) y una sal aleatoria por usuario, con el
 * formato "iteraciones:sal:hash" (sal y hash en Base64).
 *
 * Si la tabla está vacía se crea el usuario inicial admin / 1234.
 */
public class AutenticacionService {

    private static final int ITERACIONES = 120_000;
    private static final int BITS_HASH = 256;
    private static final SecureRandom ALEATORIO = new SecureRandom();

    public AutenticacionService() {
        EsquemaBD.inicializar();
        crearAdminInicial();
    }

    /**
     * Verifica las credenciales.
     *
     * @return el usuario autenticado o null si usuario o contraseña no coinciden
     */
    public Usuario autenticar(String usuario, String clave) {

        if (usuario == null || usuario.isBlank() || clave == null || clave.isEmpty()) {
            return null;
        }

        String sql = "SELECT usuario, clave_hash FROM usuarios WHERE usuario = ?";

        try (Connection c = MySqlConexion.getConexion();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, usuario.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && verificar(clave, rs.getString("clave_hash"))) {
                    return new Usuario(rs.getString("usuario"));
                }
                return null;
            }
        } catch (SQLException ex) {
            throw new PersistenciaException("Error al consultar los usuarios en MySQL", ex);
        }
    }

    /** Crea un usuario nuevo con la contraseña cifrada. */
    public void crearUsuario(String usuario, String clave) {

        if (usuario == null || usuario.isBlank()) {
            throw new IllegalArgumentException("El usuario es obligatorio.");
        }
        if (clave == null || clave.isEmpty()) {
            throw new IllegalArgumentException("La contraseña es obligatoria.");
        }

        String sql = "INSERT INTO usuarios (usuario, clave_hash) VALUES (?, ?)";

        try (Connection c = MySqlConexion.getConexion();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, usuario.trim());
            ps.setString(2, cifrar(clave));
            ps.executeUpdate();
        } catch (java.sql.SQLIntegrityConstraintViolationException ex) {
            throw new IllegalArgumentException("El usuario ya existe.");
        } catch (SQLException ex) {
            throw new PersistenciaException("No se pudo crear el usuario", ex);
        }
    }

    private void crearAdminInicial() {
        try (Connection c = MySqlConexion.getConexion();
             PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM usuarios");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next() && rs.getInt(1) == 0) {
                crearUsuario("admin", "1234");
            }
        } catch (SQLException ex) {
            throw new PersistenciaException("No se pudo preparar la tabla usuarios", ex);
        }
    }

    // ---- Cifrado ----

    static String cifrar(String clave) {
        byte[] sal = new byte[16];
        ALEATORIO.nextBytes(sal);
        byte[] hash = derivar(clave, sal, ITERACIONES);
        return ITERACIONES + ":"
                + Base64.getEncoder().encodeToString(sal) + ":"
                + Base64.getEncoder().encodeToString(hash);
    }

    static boolean verificar(String clave, String almacenado) {
        if (almacenado == null) {
            return false;
        }
        String[] partes = almacenado.split(":");
        if (partes.length != 3) {
            return false;
        }
        try {
            int iteraciones = Integer.parseInt(partes[0]);
            byte[] sal = Base64.getDecoder().decode(partes[1]);
            byte[] esperado = Base64.getDecoder().decode(partes[2]);
            byte[] calculado = derivar(clave, sal, iteraciones);
            return MessageDigest.isEqual(esperado, calculado);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private static byte[] derivar(String clave, byte[] sal, int iteraciones) {
        try {
            PBEKeySpec spec = new PBEKeySpec(clave.toCharArray(), sal, iteraciones, BITS_HASH);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("No se pudo cifrar la contraseña", ex);
        }
    }
}