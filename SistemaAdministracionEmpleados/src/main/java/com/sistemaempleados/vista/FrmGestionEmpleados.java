package com.sistemaempleados.vista;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;

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
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

import com.sistemaempleados.modelo.Empleado;
import com.sistemaempleados.modelo.EmpleadoAdministrativo;
import com.sistemaempleados.modelo.EmpleadoPorComision;
import com.sistemaempleados.modelo.EmpleadoPorHora;
import com.sistemaempleados.servicio.EmpleadoService;

/**
 * Gestión de empleados: registrar, buscar, modificar, inactivar y calcular
 * la remuneración según el tipo de empleado.
 */
public class FrmGestionEmpleados extends JFrame {

    private static final String SIN_TIPO = "Seleccione...";

    private static final String[] TIPOS = {
        SIN_TIPO,
        EmpleadoService.TIPO_ADMINISTRATIVO,
        EmpleadoService.TIPO_POR_HORA,
        EmpleadoService.TIPO_POR_COMISION
    };

    private final EmpleadoService servicio = new EmpleadoService();

    private JTextField txtCodigo;
    private JTextField txtNombre;
    private JTextField txtDocumento;
    private JTextField txtCargo;
    private JTextField txtFechaIngreso;
    private JComboBox<String> cmbTipo;

    // Campos propios de cada tipo de empleado
    private JTextField txtSalario;
    private JTextField txtTarifa;
    private JTextField txtHoras;
    private JTextField txtVentas;
    private JTextField txtPorcentaje;

    private JPanel panelDatos;

    // Búsqueda y filtro
    private JTextField txtFiltro;
    private JComboBox<String> cmbFiltroTipo;

    private JLabel lblEstadoTabla;

    private JTable tablaEmpleados;
    private DefaultTableModel modeloTabla;

    public FrmGestionEmpleados() {

        setTitle("Gestión de empleados");
        setSize(1050, 720);
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

        // Campos
        txtCodigo = new JTextField();
        txtNombre = new JTextField();
        txtDocumento = new JTextField();
        txtCargo = new JTextField();
        txtFechaIngreso = new JTextField();
        cmbTipo = new JComboBox<>(TIPOS);

        txtSalario = new JTextField();
        txtTarifa = new JTextField();
        txtHoras = new JTextField();
        txtVentas = new JTextField();
        txtPorcentaje = new JTextField();

        panelDatos = new JPanel(new GridLayout(0, 2, 10, 8));
        reconstruirCampos();

        cmbTipo.addActionListener(e -> reconstruirCampos());

        // Botones
        JButton btnRegistrar = new JButton("Registrar");
        JButton btnBuscar = new JButton("Buscar");
        JButton btnModificar = new JButton("Modificar");
        JButton btnInactivar = new JButton("Inactivar");
        JButton btnCalcular = new JButton("Calcular remuneración");
        JButton btnLimpiar = new JButton("Limpiar");

        JPanel panelBotones = new JPanel(new FlowLayout());

        panelBotones.add(btnRegistrar);
        panelBotones.add(btnBuscar);
        panelBotones.add(btnModificar);
        panelBotones.add(btnInactivar);
        panelBotones.add(btnCalcular);
        panelBotones.add(btnLimpiar);

        JPanel panelSuperior = new JPanel(new BorderLayout(10, 10));
        panelSuperior.add(panelDatos, BorderLayout.CENTER);
        panelSuperior.add(panelBotones, BorderLayout.SOUTH);

        // Filtro de la tabla
        txtFiltro = new JTextField(18);
        cmbFiltroTipo = new JComboBox<>(new String[]{
            "Todos",
            EmpleadoService.TIPO_ADMINISTRATIVO,
            EmpleadoService.TIPO_POR_HORA,
            EmpleadoService.TIPO_POR_COMISION
        });
        JButton btnFiltrar = new JButton("Filtrar");
        JButton btnVerTodos = new JButton("Ver todos");

        JPanel panelFiltro = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        panelFiltro.add(new JLabel("Buscar por nombre, documento o código:"));
        panelFiltro.add(txtFiltro);
        panelFiltro.add(new JLabel("Tipo:"));
        panelFiltro.add(cmbFiltroTipo);
        panelFiltro.add(btnFiltrar);
        panelFiltro.add(btnVerTodos);

        // Tabla
        modeloTabla = new DefaultTableModel(
            new String[]{
                "Código", "Nombre", "Documento", "Cargo", "Tipo",
                "Fecha de ingreso", "Estado", "Remuneración"
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
        // Permite ordenar por nombre, código o tipo al hacer clic en el encabezado
        tablaEmpleados.setAutoCreateRowSorter(true);

        JScrollPane scroll = new JScrollPane(tablaEmpleados);

        JPanel panelTabla = new JPanel(new BorderLayout(5, 5));
        panelTabla.add(panelFiltro, BorderLayout.NORTH);
        panelTabla.add(scroll, BorderLayout.CENTER);

        lblEstadoTabla = new JLabel(" ");
        panelTabla.add(lblEstadoTabla, BorderLayout.SOUTH);

        JPanel panelCentro = new JPanel(new BorderLayout(10, 10));
        panelCentro.add(panelSuperior, BorderLayout.NORTH);
        panelCentro.add(panelTabla, BorderLayout.CENTER);

        panelPrincipal.add(panelCentro, BorderLayout.CENTER);

        add(panelPrincipal);

        // Acciones de los botones
        btnRegistrar.addActionListener(e -> registrar());
        btnBuscar.addActionListener(e -> buscar());
        btnModificar.addActionListener(e -> modificar());
        btnInactivar.addActionListener(e -> inactivar());
        btnCalcular.addActionListener(e -> calcular());
        btnLimpiar.addActionListener(e -> limpiar());
        btnFiltrar.addActionListener(e -> actualizarTabla());
        txtFiltro.addActionListener(e -> actualizarTabla());
        cmbFiltroTipo.addActionListener(e -> actualizarTabla());
        btnVerTodos.addActionListener(e -> {
            txtFiltro.setText("");
            cmbFiltroTipo.setSelectedIndex(0);
            actualizarTabla();
        });

        // Seleccionar una fila de la tabla
        tablaEmpleados.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()
                    && tablaEmpleados.getSelectedRow() != -1) {
                cargarSeleccion();
            }
        });
    }

    // Muestra solo los campos que corresponden al tipo de empleado elegido
    private void reconstruirCampos() {

        panelDatos.removeAll();

        agregarCampo("Código:", txtCodigo);
        agregarCampo("Nombre completo:", txtNombre);
        agregarCampo("Documento:", txtDocumento);
        agregarCampo("Cargo:", txtCargo);
        agregarCampo("Fecha de ingreso (AAAA-MM-DD):", txtFechaIngreso);
        agregarCampo("Tipo de empleado:", cmbTipo);

        switch (cmbTipo.getSelectedIndex()) {
            case 0 -> { }
            case 1 -> agregarCampo("Salario base mensual:", txtSalario);
            case 2 -> {
                agregarCampo("Tarifa por hora:", txtTarifa);
                agregarCampo("Horas trabajadas del período:", txtHoras);
            }
            default -> {
                agregarCampo("Monto de ventas:", txtVentas);
                agregarCampo("Porcentaje de comisión (0-100):", txtPorcentaje);
            }
        }

        panelDatos.revalidate();
        panelDatos.repaint();
    }

    private void agregarCampo(String etiqueta, java.awt.Component campo) {
        panelDatos.add(new JLabel(etiqueta));
        panelDatos.add(campo);
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

    // Busca por código; si está vacío, por documento; y si también, por nombre
    private void buscar() {

        String codigo = txtCodigo.getText().trim();
        String documento = txtDocumento.getText().trim();
        String nombre = txtNombre.getText().trim();

        try {
            Empleado empleado = null;

            if (!codigo.isEmpty()) {
                empleado = servicio.buscar(codigo);

            } else if (!documento.isEmpty()) {
                empleado = servicio.buscarPorDocumento(documento);

            } else if (!nombre.isEmpty()) {
                List<Empleado> encontrados = servicio.buscarPorNombre(nombre);

                if (encontrados.size() > 1) {
                    txtFiltro.setText(nombre);
                    cmbFiltroTipo.setSelectedIndex(0);
                    actualizarTabla();
                    JOptionPane.showMessageDialog(
                        this,
                        "Se encontraron " + encontrados.size()
                            + " empleados. Seleccione uno de la tabla."
                    );
                    return;
                }

                if (!encontrados.isEmpty()) {
                    empleado = encontrados.get(0);
                }

            } else {
                JOptionPane.showMessageDialog(
                    this, "Ingrese el código, el documento o el nombre a buscar."
                );
                return;
            }

            if (empleado == null) {
                JOptionPane.showMessageDialog(
                    this, "No se encontró el empleado."
                );
                return;
            }

            cargarDatos(empleado);

        } catch (IllegalArgumentException ex) {

            JOptionPane.showMessageDialog(
                this, ex.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE
            );
        }
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

    // Calcula la remuneración del empleado indicado y la guarda en el historial
    private void calcular() {

        String codigo = txtCodigo.getText().trim();

        if (codigo.isBlank()) {
            JOptionPane.showMessageDialog(
                this,
                "Ingrese o seleccione el código del empleado."
            );
            return;
        }

        String periodo = JOptionPane.showInputDialog(
            this,
            "Período a calcular (AAAA-MM):",
            YearMonth.now().toString()
        );

        if (periodo == null) {
            return;
        }

        try {
            servicio.calcularYRegistrar(codigo, periodo.trim());

            Empleado empleado = servicio.buscar(codigo);

            JOptionPane.showMessageDialog(
                this,
                desglose(empleado) + "\nPeríodo: " + periodo.trim()
                    + "\n\nSe guardó en el historial de remuneraciones.",
                "Remuneración calculada",
                JOptionPane.INFORMATION_MESSAGE
            );

        } catch (IllegalArgumentException ex) {

            JOptionPane.showMessageDialog(
                this, ex.getMessage(),
                "No se pudo calcular",
                JOptionPane.WARNING_MESSAGE
            );
        }
    }

    private String desglose(Empleado empleado) {

        if (empleado instanceof EmpleadoAdministrativo a) {
            return a.mostrarDesglose();
        }
        if (empleado instanceof EmpleadoPorHora h) {
            return h.mostrarDesglose();
        }
        if (empleado instanceof EmpleadoPorComision c) {
            return c.mostrarDesglose();
        }
        return "Remuneración calculada.";
    }

    // Arma el empleado del tipo elegido a partir del formulario
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

        switch (cmbTipo.getSelectedIndex()) {
            case 0:
                throw new IllegalArgumentException(
                    "Debe seleccionar el tipo de empleado."
                );
            case 1:
                return new EmpleadoAdministrativo(
                    codigo, nombre, documento, cargo, fecha,
                    leerNumero(txtSalario, "El salario base")
                );
            case 2:
                return new EmpleadoPorHora(
                    codigo, nombre, documento, cargo, fecha,
                    leerNumero(txtTarifa, "La tarifa por hora"),
                    leerNumero(txtHoras, "Las horas trabajadas")
                );
            default:
                return new EmpleadoPorComision(
                    codigo, nombre, documento, cargo, fecha,
                    leerNumero(txtVentas, "El monto de ventas"),
                    leerNumero(txtPorcentaje, "El porcentaje de comisión")
                );
        }
    }

    private double leerNumero(JTextField campo, String nombre) {

        String texto = campo.getText().trim().replace(',', '.');

        if (texto.isEmpty()) {
            throw new IllegalArgumentException(nombre + " es obligatorio.");
        }

        try {
            return Double.parseDouble(texto);

        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(nombre + " debe ser un número.");
        }
    }

    private String nombreTipo(Empleado empleado) {
        return EmpleadoService.nombreTipo(empleado);
    }

    private void actualizarTabla() {

        modeloTabla.setRowCount(0);

        String filtro = txtFiltro.getText().trim().toLowerCase();
        String tipoFiltro = (String) cmbFiltroTipo.getSelectedItem();

        List<Empleado> empleados;

        try {
            empleados = servicio.listar();

        } catch (IllegalArgumentException ex) {
            lblEstadoTabla.setText("Error al consultar la base de datos.");
            JOptionPane.showMessageDialog(
                this, ex.getMessage(),
                "Error de conexión",
                JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        for (Empleado empleado : empleados) {

            String tipo = nombreTipo(empleado);

            if (!"Todos".equals(tipoFiltro) && !tipo.equals(tipoFiltro)) {
                continue;
            }

            if (!filtro.isEmpty()
                    && !empleado.getNombre().toLowerCase().contains(filtro)
                    && !empleado.getDocumento().toLowerCase().contains(filtro)
                    && !empleado.getCodigo().toLowerCase().contains(filtro)) {
                continue;
            }

            String remuneracion = "Sin definir".equals(tipo)
                    ? "-"
                    : String.format("$%,.2f", empleado.calcularRemuneracion());

            modeloTabla.addRow(new Object[]{
                empleado.getCodigo(),
                empleado.getNombre(),
                empleado.getDocumento(),
                empleado.getCargo(),
                tipo,
                empleado.getFechaIngreso(),
                empleado.isActivo() ? "Activo" : "Inactivo",
                remuneracion
            });
        }

        if (modeloTabla.getRowCount() == 0) {
            lblEstadoTabla.setText(
                empleados.isEmpty()
                    ? "No hay empleados registrados."
                    : "Sin resultados para la búsqueda."
            );
        } else {
            lblEstadoTabla.setText(
                "Empleados mostrados: " + modeloTabla.getRowCount()
            );
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

        txtSalario.setText("");
        txtTarifa.setText("");
        txtHoras.setText("");
        txtVentas.setText("");
        txtPorcentaje.setText("");

        if (empleado instanceof EmpleadoAdministrativo a) {
            cmbTipo.setSelectedIndex(1);
            txtSalario.setText(String.valueOf(a.getSalarioBase()));

        } else if (empleado instanceof EmpleadoPorHora h) {
            cmbTipo.setSelectedIndex(2);
            txtTarifa.setText(String.valueOf(h.getTarifaPorHora()));
            txtHoras.setText(String.valueOf(h.getHorasTrabajadas()));

        } else if (empleado instanceof EmpleadoPorComision c) {
            cmbTipo.setSelectedIndex(3);
            txtVentas.setText(String.valueOf(c.getMontoVentas()));
            txtPorcentaje.setText(String.valueOf(c.getPorcentajeComision()));
        }
    }

    private void cargarSeleccion() {

        int fila = tablaEmpleados.getSelectedRow();

        if (fila == -1) {
            return;
        }

        // Se convierte el índice de la vista al del modelo (la tabla puede estar ordenada)
        int filaModelo = tablaEmpleados.convertRowIndexToModel(fila);

        txtCodigo.setText(
            modeloTabla.getValueAt(filaModelo, 0).toString()
        );

        buscar();
    }

    private void limpiar() {

        txtCodigo.setText("");
        txtNombre.setText("");
        txtDocumento.setText("");
        txtCargo.setText("");
        txtFechaIngreso.setText("");
        txtSalario.setText("");
        txtTarifa.setText("");
        txtHoras.setText("");
        txtVentas.setText("");
        txtPorcentaje.setText("");
        cmbTipo.setSelectedIndex(0);

        tablaEmpleados.clearSelection();
        txtCodigo.requestFocus();
    }

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {
            new FrmGestionEmpleados().setVisible(true);
        });
    }
}