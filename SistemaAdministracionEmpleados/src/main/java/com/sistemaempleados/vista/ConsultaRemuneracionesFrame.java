package com.sistemaempleados.vista;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

/**
 * Ventana para consultar el historial de remuneraciones,
 * con búsqueda por código, documento, nombre o período (AAAA-MM).
 *
 * @author Javier
 */
public class ConsultaRemuneracionesFrame extends JFrame {

    private DefaultTableModel modelo;
    private JTextField txtBuscar;
    private JLabel lblTotal;

    public ConsultaRemuneracionesFrame() {
        setTitle("Consulta de remuneraciones");
        setSize(900, 450);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel arriba = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        txtBuscar = new JTextField(20);
        JButton btnBuscar = new JButton("Buscar");
        JButton btnLimpiar = new JButton("Mostrar todo");
        arriba.add(new JLabel("Código, documento, nombre o período:"));
        arriba.add(txtBuscar);
        arriba.add(btnBuscar);
        arriba.add(btnLimpiar);
        panel.add(arriba, BorderLayout.NORTH);

        String[] columnas = {"Código", "Documento", "Empleado", "Tipo", "Período", "Valores utilizados", "Importe"};
        modelo = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int fila, int columna) {
                return false;
            }
        };
        JTable tabla = new JTable(modelo);
        tabla.setRowHeight(24);
        panel.add(new JScrollPane(tabla), BorderLayout.CENTER);

        lblTotal = new JLabel(" ");
        lblTotal.setFont(new Font("Segoe UI", Font.BOLD, 13));
        panel.add(lblTotal, BorderLayout.SOUTH);

        add(panel);

        btnBuscar.addActionListener(e -> buscar());
        btnLimpiar.addActionListener(e -> {
            txtBuscar.setText("");
            cargar("");
        });

        cargar("");
    }

    private void buscar() {
        String texto = txtBuscar.getText().trim();
        if (texto.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Escriba un código, documento, nombre o período (AAAA-MM) para buscar.",
                    "Campo vacío", JOptionPane.WARNING_MESSAGE);
            return;
        }
        cargar(texto);
        if (modelo.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "No se encontraron resultados.",
                    "Sin resultados", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void cargar(String filtro) {
        modelo.setRowCount(0);
        double total = 0;

        List<RepositorioVista.RemuneracionVista> lista;
        try {
            lista = RepositorioVista.remuneraciones();
        } catch (java.sql.SQLException ex) {
            JOptionPane.showMessageDialog(this,
                    "No se pudo conectar a la base de datos:\n" + ex.getMessage(),
                    "Error de conexión", JOptionPane.ERROR_MESSAGE);
            lblTotal.setText("Sin conexión");
            return;
        }

        for (RepositorioVista.RemuneracionVista r : lista) {
            boolean coincide = filtro.isEmpty()
                    || r.codigo().equalsIgnoreCase(filtro)
                    || r.cedula().equalsIgnoreCase(filtro)
                    || r.periodo().equals(filtro)
                    || r.nombre().toLowerCase().contains(filtro.toLowerCase());
            if (coincide) {
                modelo.addRow(new Object[]{r.codigo(), r.cedula(), r.nombre(), r.tipo(), r.periodo(),
                        r.detalle(), String.format("$%,.2f", r.importe())});
                total += r.importe();
            }
        }
        lblTotal.setText("Registros: " + modelo.getRowCount()
                + "   |   Total mostrado: " + String.format("$%,.2f", total));
    }
}