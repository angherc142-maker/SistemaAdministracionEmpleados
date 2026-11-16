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
import com.sistemaempleados.modelo.EmpleadoPorHora;
import com.sistemaempleados.modelo.EmpleadoPorComision;
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
    private DefaultTableModel modeloEmpleados, modeloRemuneraciones;
    private JLabel lblCantidadEmpleados, lblTotalEmpleados, lblCantidad, lblTotal, lblPromedio;

    public ReportesFrame() {
        setTitle("Reportes de empleados y remuneraciones");
        setSize(1250, 620);
        setMinimumSize(new java.awt.Dimension(1000, 520));
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        JPanel arriba = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        cmbTipo = new JComboBox<>(new String[]{"Todos", "Administrativo", "Por hora", "Por comisión"});
        JButton btnGenerar = new JButton("Actualizar reportes");
        arriba.add(new JLabel("Tipo de empleado:")); arriba.add(cmbTipo); arriba.add(btnGenerar);
        panel.add(arriba, BorderLayout.NORTH);

        JTabbedPane pestanas = new JTabbedPane();
        Font negrita = new Font("Segoe UI", Font.BOLD, 13);
        modeloEmpleados = crearModelo(new String[]{"Código", "Nombre", "Documento", "Cargo", "Tipo", "Estado",
            "Salario base", "Tarifa por hora", "Horas del período", "Monto de ventas", "% comisión", "Remuneración estimada actual"});
        JPanel panelEmpleados = new JPanel(new BorderLayout(5, 5));
        panelEmpleados.add(new JScrollPane(crearTabla(modeloEmpleados)), BorderLayout.CENTER);
        JPanel totalesEmpleados = new JPanel(new GridLayout(1, 2, 10, 0));
        lblCantidadEmpleados = new JLabel(); lblTotalEmpleados = new JLabel();
        for (JLabel l : new JLabel[]{lblCantidadEmpleados, lblTotalEmpleados}) { l.setFont(negrita); totalesEmpleados.add(l); }
        panelEmpleados.add(totalesEmpleados, BorderLayout.SOUTH);
        pestanas.addTab("Listado y datos salariales actuales", panelEmpleados);

        modeloRemuneraciones = crearModelo(new String[]{"Código", "Empleado", "Documento", "Tipo", "Período",
            "Valores utilizados en el cálculo", "Remuneración registrada", "Fecha del cálculo"});
        JPanel panelResumen = new JPanel(new BorderLayout(5, 5));
        panelResumen.add(new JScrollPane(crearTabla(modeloRemuneraciones)), BorderLayout.CENTER);
        JPanel totales = new JPanel(new GridLayout(1, 3, 10, 0));
        lblCantidad = new JLabel(); lblTotal = new JLabel(); lblPromedio = new JLabel();
        for (JLabel l : new JLabel[]{lblCantidad, lblTotal, lblPromedio}) { l.setFont(negrita); totales.add(l); }
        panelResumen.add(totales, BorderLayout.SOUTH);
        pestanas.addTab("Historial de remuneraciones", panelResumen);
        panel.add(pestanas, BorderLayout.CENTER); add(panel);
        btnGenerar.addActionListener(e -> generar()); cmbTipo.addActionListener(e -> generar());
        generar();
    }

    private DefaultTableModel crearModelo(String[] columnas) {
        return new DefaultTableModel(columnas, 0) { @Override public boolean isCellEditable(int f, int c) { return false; } };
    }
    private JTable crearTabla(DefaultTableModel modelo) {
        JTable t = new JTable(modelo); t.setRowHeight(25); t.setAutoCreateRowSorter(true); t.setAutoResizeMode(JTable.AUTO_RESIZE_OFF); return t;
    }
    private String dinero(double valor) { return String.format("$%,.2f", valor); }

    private void generar() {
        String tipo = (String) cmbTipo.getSelectedItem();
        modeloEmpleados.setRowCount(0); modeloRemuneraciones.setRowCount(0);
        try {
            List<Empleado> empleados = servicio.listar();
            int cantidadEmpleados = 0, cantidadActivos = 0;
            double totalEstimadoActivos = 0;
            for (Empleado e : empleados) {
                String tipoEmpleado = EmpleadoService.nombreTipo(e);
                if (!"Todos".equals(tipo) && !tipoEmpleado.equals(tipo)) continue;
                String salarioBase = "-", tarifaHora = "-", horas = "-", ventas = "-", porcentaje = "-";
                if (e instanceof EmpleadoAdministrativo a) salarioBase = dinero(a.getSalarioBase());
                else if (e instanceof EmpleadoPorHora h) {
                    tarifaHora = dinero(h.getTarifaPorHora()); horas = String.format("%,.2f", h.getHorasTrabajadas());
                } else if (e instanceof EmpleadoPorComision c) {
                    ventas = dinero(c.getMontoVentas()); porcentaje = String.format("%,.2f%%", c.getPorcentajeComision());
                }
                double estimada = e.calcularRemuneracion();
                modeloEmpleados.addRow(new Object[]{e.getCodigo(), e.getNombre(), e.getDocumento(), e.getCargo(), tipoEmpleado,
                    e.isActivo() ? "Activo" : "Inactivo", salarioBase, tarifaHora, horas, ventas, porcentaje, dinero(estimada)});
                cantidadEmpleados++;
                if (e.isActivo()) { cantidadActivos++; totalEstimadoActivos += estimada; }
            }
            lblCantidadEmpleados.setText("Empleados encontrados: " + cantidadEmpleados + " | Activos: " + cantidadActivos);
            lblTotalEmpleados.setText("Estimación actual de activos: " + dinero(totalEstimadoActivos));

            // Estos importes son históricos: no se recalculan con los datos actuales del empleado.
            double totalHistorico = 0; int cantidadHistorica = 0;
            for (RepositorioVista.RemuneracionVista r : RepositorioVista.remuneraciones()) {
                if ("Todos".equals(tipo) || r.tipo().equals(tipo)) {
                    modeloRemuneraciones.addRow(new Object[]{r.codigo(), r.nombre(), r.cedula(), r.tipo(), r.periodo(),
                        r.detalle(), dinero(r.importe()), r.fechaCalculo()});
                    totalHistorico += r.importe(); cantidadHistorica++;
                }
            }
            double promedio = cantidadHistorica == 0 ? 0 : totalHistorico / cantidadHistorica;
            lblCantidad.setText("Cálculos históricos: " + cantidadHistorica);
            lblTotal.setText("Total histórico registrado: " + dinero(totalHistorico));
            lblPromedio.setText("Promedio por cálculo: " + dinero(promedio));
            if (cantidadEmpleados == 0 && cantidadHistorica == 0)
                JOptionPane.showMessageDialog(this, "No hay resultados para el tipo seleccionado.", "Sin resultados", JOptionPane.INFORMATION_MESSAGE);
        } catch (java.sql.SQLException | IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, "No se pudieron generar los reportes:\n" + ex.getMessage(), "Error de consulta", JOptionPane.ERROR_MESSAGE);
        }
    }
}