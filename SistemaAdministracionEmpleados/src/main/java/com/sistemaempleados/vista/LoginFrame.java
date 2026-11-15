package com.sistemaempleados.vista;

import com.sistemaempleados.modelo.Usuario;
import com.sistemaempleados.servicio.AutenticacionService;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/**
 * Ventana de inicio de sesión. Verifica las credenciales contra la tabla
 * usuarios de MySQL (contraseña cifrada).
 *
 * @author Javier
 */
public class LoginFrame extends JFrame {

    private JTextField txtUsuario;
    private JPasswordField txtClave;
    private JButton btnIngresar;
    private AutenticacionService autenticacion;

    public LoginFrame() {
        setTitle("Inicio de sesión");
        setSize(350, 220);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel panel = new JPanel(new GridLayout(3, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        txtUsuario = new JTextField();
        txtClave = new JPasswordField();
        btnIngresar = new JButton("Ingresar");

        panel.add(new JLabel("Usuario:"));
        panel.add(txtUsuario);
        panel.add(new JLabel("Contraseña:"));
        panel.add(txtClave);
        panel.add(new JLabel());
        panel.add(btnIngresar);

        add(panel);

        btnIngresar.addActionListener(e -> validarLogin());
        getRootPane().setDefaultButton(btnIngresar);
    }

    private void validarLogin() {
        String usuario = txtUsuario.getText().trim();
        String clave = new String(txtClave.getPassword());

        if (usuario.isEmpty() || clave.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Complete todos los campos.",
                    "Campos vacíos", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            if (autenticacion == null) {
                autenticacion = new AutenticacionService();
            }

            Usuario autenticado = autenticacion.autenticar(usuario, clave);

            if (autenticado != null) {
                new MenuPrincipalFrame(autenticado.getNombreUsuario()).setVisible(true);
                dispose();
            } else {
                JOptionPane.showMessageDialog(this,
                        "Usuario o contraseña incorrectos.",
                        "Error", JOptionPane.ERROR_MESSAGE);
                txtClave.setText("");
            }

        } catch (RuntimeException ex) {
            autenticacion = null;
            JOptionPane.showMessageDialog(this,
                    "No se pudo verificar el acceso (error de conexión con MySQL):\n"
                    + ex.getMessage(),
                    "Error de conexión", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}