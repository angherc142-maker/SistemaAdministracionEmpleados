/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package com.sistemaempleados;

import com.sistemaempleados.vista.LoginFrame;
import javax.swing.SwingUtilities;

/**
 * Punto de entrada: abre la ventana de inicio de sesión.
 *
 * @author hercu
 */
public class SistemaAdministracionEmpleados {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}