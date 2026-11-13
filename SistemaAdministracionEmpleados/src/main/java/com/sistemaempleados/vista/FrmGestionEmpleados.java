package com.sistemaempleados.vista;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
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
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

import com.sistemaempleados.modelo.Empleado;
import com.sistemaempleados.modelo.EmpleadoPrueba;
import com.sistemaempleados.servicio.EmpleadoService;

public class FrmGestionEmpleados extends JFrame {

    private final EmpleadoService servicio = new EmpleadoService();

    private JTextField txtCodigo;
    private JTextField txtNombre;
    private JTextField txtDocumento;
    private JTextField txtCargo;
    private JTextField txtFechaIngreso;

    private JTable tablaEmpleados;
    private DefaultTableModel modeloTabla;

    public FrmGestionEmpleados() {

        setTitle("Sistema de Administración de Empleados");
        setSize(950, 600);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        crearInterfaz();
        actualizarTabla();
    }

    private void crearInterfaz() {

        JPanel panelPrincipal = new JPanel(new BorderLayout(10, 10));
        panelPrincipal.setBorder(
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        );

        JLabel titulo = new JLabel(
            "GESTIÓN DE EMPLEADOS",
            SwingConstants.CENTER
        );

        titulo.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 22));

        panelPrincipal.add(titulo, BorderLayout.NORTH);

        // Panel de datos
        JPanel panelDatos = new JPanel(new GridLayout(5, 2, 10, 10));

        txtCodigo = new JTextField();
        txtNombre = new JTextField();
        txtDocumento = new JTextField();
        txtCargo = new JTextField();
        txtFechaIngreso = new JTextField();

        panelDatos.add(new JLabel("Código:"));
        panelDatos.add(txtCodigo);

        panelDatos.add(new JLabel("Nombre completo:"));
        panelDatos.add(txtNombre);

        panelDatos.add(new JLabel("Documento:"));
        panelDatos.add(txtDocumento);

        panelDatos.add(new JLabel("Cargo:"));
        panelDatos.add(txtCargo);

        panelDatos.add(new JLabel("Fecha de ingreso (AAAA-MM-DD):"));
        panelDatos.add(txtFechaIngreso);

        // Botones
        JButton btnRegistrar = new JButton("Registrar");
        JButton btnBuscar = new JButton("Buscar");
        JButton btnModificar = new JButton("Modificar");
        JButton btnInactivar = new JButton("Inactivar");
        JButton btnLimpiar = new JButton("Limpiar");

        JPanel panelBotones = new JPanel(new FlowLayout());

        panelBotones.add(btnRegistrar);
        panelBotones.add(btnBuscar);
        panelBotones.add(btnModificar);
        panelBotones.add(btnInactivar);
        panelBotones.add(btnLimpiar);

        JPanel panelSuperior = new JPanel(new BorderLayout(10, 10));
        panelSuperior.add(panelDatos, BorderLayout.CENTER);
        panelSuperior.add(panelBotones, BorderLayout.SOUTH);

        // Tabla
        modeloTabla = new DefaultTableModel(
            new String[]{
                "Código", "Nombre", "Documento",
                "Cargo", "Fecha de ingreso", "Estado"
            }, 0
        ) {
            @Override
            public boolean isCellEditable(int fila, int columna) {
                return false;
            }
        };

        tablaEmpleados = new JTable(modeloTabla);
        tablaEmpleados.setSelectionMode(
            ListSelectionModel.SINGLE_SELECTION
        );

        JScrollPane scroll = new JScrollPane(tablaEmpleados);

        JPanel panelCentro = new JPanel(new BorderLayout(10, 10));
        panelCentro.add(panelSuperior, BorderLayout.NORTH);
        panelCentro.add(scroll, BorderLayout.CENTER);

        panelPrincipal.add(panelCentro, BorderLayout.CENTER);

        add(panelPrincipal);

        // Acciones de los botones
        btnRegistrar.addActionListener(e -> registrar());
        btnBuscar.addActionListener(e -> buscar());
        btnModificar.addActionListener(e -> modificar());
        btnInactivar.addActionListener(e -> inactivar());
        btnLimpiar.addActionListener(e -> limpiar());

        // Seleccionar una fila de la tabla
        tablaEmpleados.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()
                    && tablaEmpleados.getSelectedRow() != -1) {
                cargarSeleccion();
            }
        });
    }

    private void registrar() {

        try {
            Empleado empleado = obtenerDatosFormulario();

            servicio.registrar(empleado);

            actualizarTabla();
            limpiar();

            JOptionPane.showMessageDialog(
                this,
                "Empleado registrado correctamente."
            );

        } catch (IllegalArgumentException ex) {

            JOptionPane.showMessageDialog(
                this, ex.getMessage(),
                "Error de validación",
                JOptionPane.WARNING_MESSAGE
            );
        }
    }

    private void buscar() {

        String codigo = txtCodigo.getText().trim();

        Empleado empleado = servicio.buscar(codigo);

        if (empleado == null) {
            JOptionPane.showMessageDialog(
                this, "No se encontró el empleado."
            );
            return;
        }

        cargarDatos(empleado);
    }

    private void modificar() {

        try {
            Empleado empleado = obtenerDatosFormulario();

            servicio.modificar(empleado);

            actualizarTabla();
            limpiar();

            JOptionPane.showMessageDialog(
                this, "Empleado modificado correctamente."
            );

        } catch (IllegalArgumentException ex) {

            JOptionPane.showMessageDialog(
                this, ex.getMessage(),
                "Error de validación",
                JOptionPane.WARNING_MESSAGE
            );
        }
    }

    private void inactivar() {

        String codigo = txtCodigo.getText().trim();

        if (codigo.isBlank()) {
            JOptionPane.showMessageDialog(
                this, "Ingrese el código del empleado."
            );
            return;
        }

        int respuesta = JOptionPane.showConfirmDialog(
            this,
            "¿Desea inactivar al empleado " + codigo + "?",
            "Confirmar inactivación",
            JOptionPane.YES_NO_OPTION
        );

        if (respuesta != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            servicio.inactivar(codigo);

            actualizarTabla();
            limpiar();

            JOptionPane.showMessageDialog(
                this, "Empleado inactivado correctamente."
            );

        } catch (IllegalArgumentException ex) {

            JOptionPane.showMessageDialog(
                this, ex.getMessage()
            );
        }
    }

    private Empleado obtenerDatosFormulario() {

        String codigo = txtCodigo.getText().trim();
        String nombre = txtNombre.getText().trim();
        String documento = txtDocumento.getText().trim();
        String cargo = txtCargo.getText().trim();

        LocalDate fecha;

        try {
            fecha = LocalDate.parse(txtFechaIngreso.getText().trim());

        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException(
                "La fecha debe tener el formato AAAA-MM-DD."
            );
        }

        return new EmpleadoPrueba(
            codigo, nombre, documento, cargo, fecha
        );
    }

    private void actualizarTabla() {

        modeloTabla.setRowCount(0);

        List<Empleado> empleados = servicio.listar();

        for (Empleado empleado : empleados) {

            modeloTabla.addRow(new Object[]{
                empleado.getCodigo(),
                empleado.getNombre(),
                empleado.getDocumento(),
                empleado.getCargo(),
                empleado.getFechaIngreso(),
                empleado.isActivo() ? "Activo" : "Inactivo"
            });
        }
    }

    private void cargarDatos(Empleado empleado) {

        txtCodigo.setText(empleado.getCodigo());
        txtNombre.setText(empleado.getNombre());
        txtDocumento.setText(empleado.getDocumento());
        txtCargo.setText(empleado.getCargo());
        txtFechaIngreso.setText(
            empleado.getFechaIngreso().toString()
        );
    }

    private void cargarSeleccion() {

        int fila = tablaEmpleados.getSelectedRow();

        if (fila == -1) {
            return;
        }

        txtCodigo.setText(
            modeloTabla.getValueAt(fila, 0).toString()
        );

        buscar();
    }

    private void limpiar() {

        txtCodigo.setText("");
        txtNombre.setText("");
        txtDocumento.setText("");
        txtCargo.setText("");
        txtFechaIngreso.setText("");

        tablaEmpleados.clearSelection();
        txtCodigo.requestFocus();
    }

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {
            new FrmGestionEmpleados().setVisible(true);
        });
    }
}
