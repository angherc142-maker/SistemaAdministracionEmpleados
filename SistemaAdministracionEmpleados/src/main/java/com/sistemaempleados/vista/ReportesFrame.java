package com.sistemaempleados.vista;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

/**
 * Ventana de reportes: listado general de empleados y resumen
 * de remuneraciones, con filtro por tipo de empleado.
 *
 * @author Javier
 */
public class ReportesFrame extends JFrame {

    private JComboBox<String> cmbTipo;
    private DefaultTableModel modeloEmpleados;
    private DefaultTableModel modeloRemuneraciones;
    private JLabel lblCantidad, lblTotal, lblPromedio;

    public ReportesFrame() {
        setTitle("Reportes");
        setSize(850, 500);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel arriba = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        cmbTipo = new JComboBox<>(new String[]{"Todos", "Administrativo", "Por hora", "Por comisión"});
        JButton btnGenerar = new JButton("Generar reporte");
        arriba.add(new JLabel("Tipo de empleado:"));
        arriba.add(cmbTipo);
        arriba.add(btnGenerar);
        panel.add(arriba, BorderLayout.NORTH);

        JTabbedPane pestanas = new JTabbedPane();

        modeloEmpleados = crearModelo(
                new String[]{"Cédula", "Nombre", "Cargo", "Salario base", "Tipo"});
        pestanas.addTab("Listado de empleados", new JScrollPane(crearTabla(modeloEmpleados)));

        modeloRemuneraciones = crearModelo(
                new String[]{"Cédula", "Empleado", "Tipo", "Período", "Importe"});
        JPanel panelResumen = new JPanel(new BorderLayout(5, 5));
        panelResumen.add(new JScrollPane(crearTabla(modeloRemuneraciones)), BorderLayout.CENTER);

        JPanel totales = new JPanel(new GridLayout(1, 3, 10, 0));
        lblCantidad = new JLabel();
        lblTotal = new JLabel();
        lblPromedio = new JLabel();
        Font negrita = new Font("Segoe UI", Font.BOLD, 13);
        for (JLabel l : new JLabel[]{lblCantidad, lblTotal, lblPromedio}) {
            l.setFont(negrita);
            totales.add(l);
        }
        panelResumen.add(totales, BorderLayout.SOUTH);
        pestanas.addTab("Resumen de remuneraciones", panelResumen);

        panel.add(pestanas, BorderLayout.CENTER);
        add(panel);

        btnGenerar.addActionListener(e -> generar());
        cmbTipo.addActionListener(e -> generar());

        generar();
    }

    private DefaultTableModel crearModelo(String[] columnas) {
        return new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int f, int c) {
                return false;
            }
        };
    }

    private JTable crearTabla(DefaultTableModel modelo) {
        JTable t = new JTable(modelo);
        t.setRowHeight(24);
        return t;
    }

    private void generar() {
        String tipo = (String) cmbTipo.getSelectedItem();
        modeloEmpleados.setRowCount(0);
        modeloRemuneraciones.setRowCount(0);

        try {
            for (RepositorioVista.EmpleadoVista e : RepositorioVista.empleados()) {
                if (tipo.equals("Todos") || e.tipo().equals(tipo)) {
                    modeloEmpleados.addRow(new Object[]{e.cedula(), e.nombre(), e.cargo(),
                            String.format("$%,.2f", e.salarioBase()), e.tipo()});
                }
            }

            double total = 0;
            int cantidad = 0;
            for (RepositorioVista.RemuneracionVista r : RepositorioVista.remuneraciones()) {
                if (tipo.equals("Todos") || r.tipo().equals(tipo)) {
                    modeloRemuneraciones.addRow(new Object[]{r.cedula(), r.nombre(), r.tipo(),
                            r.periodo(), String.format("$%,.2f", r.importe())});
                    total += r.importe();
                    cantidad++;
                }
            }
            double promedio = cantidad == 0 ? 0 : total / cantidad;
            lblCantidad.setText("Pagos: " + cantidad);
            lblTotal.setText("Total pagado: " + String.format("$%,.2f", total));
            lblPromedio.setText("Promedio: " + String.format("$%,.2f", promedio));
        } catch (java.sql.SQLException ex) {
            JOptionPane.showMessageDialog(this,
                    "No se pudo conectar a la base de datos:\n" + ex.getMessage(),
                    "Error de conexión", JOptionPane.ERROR_MESSAGE);
        }
    }
}
