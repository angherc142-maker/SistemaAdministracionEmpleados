package com.sistemaempleados.vista;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * Menú principal del sistema. Da acceso a la gestión de empleados,
 * la consulta de remuneraciones, los reportes y la salida.
 *
 * @author Javier
 */
public class MenuPrincipalFrame extends JFrame {

    public MenuPrincipalFrame(String usuario) {
        setTitle("Sistema de Administración de Empleados");
        setSize(420, 380);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                salir();
            }
        });

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        JLabel titulo = new JLabel("Menú principal - Usuario: " + usuario, SwingConstants.CENTER);
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 16));
        panel.add(titulo, BorderLayout.NORTH);

        JPanel botones = new JPanel(new GridLayout(4, 1, 10, 10));
        JButton btnEmpleados = new JButton("Gestión de empleados");
        JButton btnConsulta = new JButton("Consultar remuneraciones");
        JButton btnReportes = new JButton("Reportes");
        JButton btnSalir = new JButton("Salir");

        botones.add(btnEmpleados);
        botones.add(btnConsulta);
        botones.add(btnReportes);
        botones.add(btnSalir);
        panel.add(botones, BorderLayout.CENTER);

        add(panel);

        btnEmpleados.addActionListener(e -> abrirGestionEmpleados());
        btnConsulta.addActionListener(e -> new ConsultaRemuneracionesFrame().setVisible(true));
        btnReportes.addActionListener(e -> new ReportesFrame().setVisible(true));
        btnSalir.addActionListener(e -> salir());
    }

    private void abrirGestionEmpleados() {
        // TODO: cuando el Integrante 1 termine su formulario, abrirlo aquí:
        // new GestionEmpleadosFrame().setVisible(true);
        JOptionPane.showMessageDialog(this,
                "El formulario de gestión de empleados (Módulo 1) aún no está integrado.",
                "Información", JOptionPane.INFORMATION_MESSAGE);
    }

    private void salir() {
        int opcion = JOptionPane.showConfirmDialog(this,
                "¿Seguro que desea salir del sistema?",
                "Confirmar salida", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (opcion == JOptionPane.YES_OPTION) {
            System.exit(0);
        }
    }
}