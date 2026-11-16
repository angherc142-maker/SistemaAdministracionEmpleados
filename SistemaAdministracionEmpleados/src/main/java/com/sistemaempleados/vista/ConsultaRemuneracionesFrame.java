package com.sistemaempleados.vista;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.List;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
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
    private JComboBox<String> cmbCampo;
    private JLabel lblResumen;
    private List<RepositorioVista.RemuneracionVista> registros;

    public ConsultaRemuneracionesFrame() {
        setTitle("Consulta e historial de remuneraciones");
        setSize(1200, 560);
        setMinimumSize(new java.awt.Dimension(950, 450));
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        JPanel arriba = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        cmbCampo = new JComboBox<>(new String[]{"Todos", "Código", "Documento", "Nombre", "Período"});
        txtBuscar = new JTextField(18);
        JButton btnBuscar = new JButton("Buscar");
        JButton btnTodo = new JButton("Mostrar todo");
        JButton btnDetalle = new JButton("Ver detalle");
        arriba.add(new JLabel("Buscar por:")); arriba.add(cmbCampo); arriba.add(txtBuscar);
        arriba.add(btnBuscar); arriba.add(btnTodo); arriba.add(btnDetalle);
        panel.add(arriba, BorderLayout.NORTH);

        modelo = new DefaultTableModel(new String[]{"Código", "Documento", "Empleado", "Tipo", "Período",
            "Valores utilizados", "Remuneración registrada", "Fecha del cálculo"}, 0) {
            @Override public boolean isCellEditable(int fila, int columna) { return false; }
        };
        JTable tabla = new JTable(modelo);
        tabla.setRowHeight(25);
        tabla.setAutoCreateRowSorter(true);
        tabla.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        panel.add(new JScrollPane(tabla), BorderLayout.CENTER);

        lblResumen = new JLabel(" ");
        lblResumen.setFont(new Font("Segoe UI", Font.BOLD, 13));
        panel.add(lblResumen, BorderLayout.SOUTH);
        add(panel);

        btnBuscar.addActionListener(e -> buscar());
        btnTodo.addActionListener(e -> { txtBuscar.setText(""); cargar("", "Todos"); });
        btnDetalle.addActionListener(e -> mostrarDetalle(tabla));
        tabla.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) mostrarDetalle(tabla);
            }
        });
        cargar("", "Todos");
    }

    private void buscar() {
        String texto = txtBuscar.getText().trim();
        if (texto.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Escriba el dato que desea buscar.", "Búsqueda vacía", JOptionPane.WARNING_MESSAGE);
            return;
        }
        cargar(texto, (String) cmbCampo.getSelectedItem());
        if (modelo.getRowCount() == 0)
            JOptionPane.showMessageDialog(this, "No se encontraron cálculos para ese criterio.", "Sin resultados", JOptionPane.INFORMATION_MESSAGE);
    }

    private void cargar(String filtro, String campo) {
        modelo.setRowCount(0);
        try {
            registros = RepositorioVista.remuneraciones();
            double total = 0;
            int cantidad = 0;
            for (RepositorioVista.RemuneracionVista r : registros) {
                boolean coincide = filtro.isEmpty();
                String f = filtro.toLowerCase(Locale.ROOT);
                if (!coincide) {
                    coincide = switch (campo) {
                        case "Código" -> r.codigo().equalsIgnoreCase(filtro);
                        case "Documento" -> r.cedula() != null && r.cedula().equalsIgnoreCase(filtro);
                        case "Nombre" -> r.nombre() != null && r.nombre().toLowerCase(Locale.ROOT).contains(f);
                        case "Período" -> r.periodo().equalsIgnoreCase(filtro);
                        default -> r.codigo().equalsIgnoreCase(filtro)
                                || (r.cedula() != null && r.cedula().equalsIgnoreCase(filtro))
                                || (r.nombre() != null && r.nombre().toLowerCase(Locale.ROOT).contains(f))
                                || r.periodo().equalsIgnoreCase(filtro);
                    };
                }
                if (coincide) {
                    modelo.addRow(new Object[]{r.codigo(), r.cedula(), r.nombre(), r.tipo(), r.periodo(),
                        r.detalle(), String.format("$%,.2f", r.importe()), r.fechaCalculo()});
                    total += r.importe(); cantidad++;
                }
            }
            double promedio = cantidad == 0 ? 0 : total / cantidad;
            lblResumen.setText("Cálculos encontrados: " + cantidad + "   |   Total registrado: "
                    + String.format("$%,.2f", total) + "   |   Promedio: " + String.format("$%,.2f", promedio));
        } catch (java.sql.SQLException ex) {
            JOptionPane.showMessageDialog(this, "No se pudo consultar el historial:\n" + ex.getMessage(),
                    "Error de conexión o lectura", JOptionPane.ERROR_MESSAGE);
            lblResumen.setText("No se pudo cargar el historial.");
        }
    }

    private void mostrarDetalle(JTable tabla) {
        int filaVista = tabla.getSelectedRow();
        if (filaVista < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione un cálculo de la tabla.", "Sin selección", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int fila = tabla.convertRowIndexToModel(filaVista);
        StringBuilder detalle = new StringBuilder();
        String[] etiquetas = {"Código", "Documento", "Empleado", "Tipo", "Período", "Valores utilizados", "Remuneración registrada", "Fecha del cálculo"};
        for (int i = 0; i < etiquetas.length; i++)
            detalle.append(etiquetas[i]).append(": ").append(modelo.getValueAt(fila, i)).append('\n');
        JOptionPane.showMessageDialog(this, detalle.toString(), "Detalle del cálculo histórico", JOptionPane.INFORMATION_MESSAGE);
    }
}
