package com.sistemaempleados.vista;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.List;

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

import com.sistemaempleados.modelo.Empleado;
import com.sistemaempleados.modelo.EmpleadoAdministrativo;
import com.sistemaempleados.servicio.EmpleadoService;

/**
 * Ventana de reportes: listado de empleados (con salario base y remuneración
 * calculada por separado) y resumen de remuneraciones guardadas, con filtro
 * por tipo de empleado.
 *
 * @author Javier
 */
public class ReportesFrame extends JFrame {

    private final EmpleadoService servicio = new EmpleadoService();

    private JComboBox<String> cmbTipo;
    private DefaultTableModel modeloEmpleados;
    private DefaultTableModel modeloRemuneraciones;
    private JLabel lblCantidadEmpleados, lblTotalEmpleados;
    private JLabel lblCantidad, lblTotal, lblPromedio;

    public ReportesFrame() {
        setTitle("Reportes");
        setSize(950, 520);
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
        Font negrita = new Font("Segoe UI", Font.BOLD, 13);

        // Listado de empleados
        modeloEmpleados = crearModelo(new String[]{
            "Código", "Nombre", "Documento", "Cargo", "Tipo", "Estado",
            "Salario base", "Remuneración calculada"});
        JPanel panelEmpleados = new JPanel(new BorderLayout(5, 5));
        panelEmpleados.add(new JScrollPane(crearTabla(modeloEmpleados)), BorderLayout.CENTER);

        JPanel totalesEmpleados = new JPanel(new GridLayout(1, 2, 10, 0));
        lblCantidadEmpleados = new JLabel();
        lblTotalEmpleados = new JLabel();
        for (JLabel l : new JLabel[]{lblCantidadEmpleados, lblTotalEmpleados}) {
            l.setFont(negrita);
            totalesEmpleados.add(l);
        }
        panelEmpleados.add(totalesEmpleados, BorderLayout.SOUTH);
        pestanas.addTab("Listado de empleados", panelEmpleados);

        // Resumen de remuneraciones guardadas
        modeloRemuneraciones = crearModelo(
                new String[]{"Código", "Empleado", "Tipo", "Período", "Importe"});
        JPanel panelResumen = new JPanel(new BorderLayout(5, 5));
        panelResumen.add(new JScrollPane(crearTabla(modeloRemuneraciones)), BorderLayout.CENTER);

        JPanel totales = new JPanel(new GridLayout(1, 3, 10, 0));
        lblCantidad = new JLabel();
        lblTotal = new JLabel();
        lblPromedio = new JLabel();
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
        t.setAutoCreateRowSorter(true);
        return t;
    }

    private void generar() {
        String tipo = (String) cmbTipo.getSelectedItem();
        modeloEmpleados.setRowCount(0);
        modeloRemuneraciones.setRowCount(0);

        try {
            // Listado: salario base (solo administrativos) y remuneración calculada
            List<Empleado> empleados = servicio.listar();
            int cantidadEmpleados = 0;
            double totalCalculado = 0;

            for (Empleado e : empleados) {
                String tipoEmpleado = EmpleadoService.nombreTipo(e);
                if (!tipo.equals("Todos") && !tipoEmpleado.equals(tipo)) {
                    continue;
                }

                String salarioBase = e instanceof EmpleadoAdministrativo a
                        ? String.format("$%,.2f", a.getSalarioBase())
                        : "-";
                double remuneracion = e.calcularRemuneracion();

                modeloEmpleados.addRow(new Object[]{
                        e.getCodigo(), e.getNombre(), e.getDocumento(), e.getCargo(),
                        tipoEmpleado, e.isActivo() ? "Activo" : "Inactivo",
                        salarioBase, String.format("$%,.2f", remuneracion)});

                cantidadEmpleados++;
                if (e.isActivo()) {
                    totalCalculado += remuneracion;
                }
            }
            lblCantidadEmpleados.setText("Empleados: " + cantidadEmpleados);
            lblTotalEmpleados.setText("Total remuneración calculada (activos): "
                    + String.format("$%,.2f", totalCalculado));

            // Resumen de los cálculos guardados en el historial
            double total = 0;
            int cantidad = 0;
            for (RepositorioVista.RemuneracionVista r : RepositorioVista.remuneraciones()) {
                if (tipo.equals("Todos") || r.tipo().equals(tipo)) {
                    modeloRemuneraciones.addRow(new Object[]{r.codigo(), r.nombre(), r.tipo(),
                            r.periodo(), String.format("$%,.2f", r.importe())});
                    total += r.importe();
                    cantidad++;
                }
            }
            double promedio = cantidad == 0 ? 0 : total / cantidad;
            lblCantidad.setText("Pagos: " + cantidad);
            lblTotal.setText("Total pagado: " + String.format("$%,.2f", total));
            lblPromedio.setText("Promedio: " + String.format("$%,.2f", promedio));

            if (cantidadEmpleados == 0 && cantidad == 0) {
                JOptionPane.showMessageDialog(this,
                        "No hay resultados para el tipo seleccionado.",
                        "Sin resultados", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (java.sql.SQLException | IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this,
                    "No se pudo consultar la base de datos:\n" + ex.getMessage(),
                    "Error de conexión", JOptionPane.ERROR_MESSAGE);
        }
    }
}