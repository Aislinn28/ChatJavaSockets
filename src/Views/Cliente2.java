package Views;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.Toolkit;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.JTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JButton;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class Cliente2 extends JFrame {

    private static final long serialVersionUID = 1L;
    private JPanel contentPane;
    private JTextField campoNombre;
    private JTextField campoMensaje;
    private JTextArea areaHistorial;
    private JButton btnEnviar;
    private JButton btnConectar;
    private JButton btnDesconectar;
    
    // Variables de conexión
    private Socket socket;
    private ObjectOutputStream salida;
    private ObjectInputStream entrada;
    private boolean conectado = false;
    private String nombreUsuario;

    /**
     * Launch the application.
     */
    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    Cliente2 frame = new Cliente2();
                    frame.setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Create the frame.
     */
    public Cliente2() {
        setTitle("Cliente 2 - Chat");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        
        // Manejar cierre de ventana
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (conectado) {
                    desconectar();
                }
                System.exit(0);
            }
        });
        
        setBounds(600, 100, 550, 400); // Posición diferente para no solaparse
        contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        setContentPane(contentPane);
        contentPane.setLayout(null);

        // Campo nombre
        campoNombre = new JTextField();
        campoNombre.setBounds(117, 10, 96, 19);
        contentPane.add(campoNombre);
        campoNombre.setColumns(10);

        JLabel lblNombre = new JLabel("Nombre");
        lblNombre.setBounds(56, 13, 44, 12);
        contentPane.add(lblNombre);

        // Campo mensaje
        campoMensaje = new JTextField();
        campoMensaje.setBounds(113, 51, 161, 80);
        campoMensaje.setEnabled(false);
        contentPane.add(campoMensaje);
        campoMensaje.setColumns(10);

        JLabel lblMensaje = new JLabel("Mensaje:");
        lblMensaje.setBounds(56, 84, 44, 12);
        contentPane.add(lblMensaje);

        // Botones
        btnEnviar = new JButton("Enviar");
        btnEnviar.setEnabled(false);
        btnEnviar.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                enviarMensaje();
            }
        });
        btnEnviar.setBounds(113, 150, 84, 25);
        contentPane.add(btnEnviar);
        
        btnConectar = new JButton("Conectar");
        btnConectar.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                conectar();
            }
        });
        btnConectar.setBounds(230, 9, 90, 23);
        contentPane.add(btnConectar);
        
        btnDesconectar = new JButton("Desconectar");
        btnDesconectar.setEnabled(false);
        btnDesconectar.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                desconectar();
            }
        });
        btnDesconectar.setBounds(330, 9, 100, 23);
        contentPane.add(btnDesconectar);

        // Área de historial
        areaHistorial = new JTextArea();
        areaHistorial.setEditable(false);
        areaHistorial.setLineWrap(true);
        areaHistorial.setWrapStyleWord(true);
        
        JScrollPane scrollPane = new JScrollPane(areaHistorial);
        scrollPane.setBounds(300, 40, 200, 280);
        contentPane.add(scrollPane);

        JLabel lblHistorial = new JLabel("HISTORIAL");
        lblHistorial.setBounds(360, 20, 65, 12);
        contentPane.add(lblHistorial);
        
        // Event listener para Enter en campo mensaje
        campoMensaje.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                enviarMensaje();
            }
        });
        
        // Event listener para Enter en campo nombre
        campoNombre.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                conectar();
            }
        });
    }
    
    private void conectar() {
        nombreUsuario = campoNombre.getText().trim();
        
        if (nombreUsuario.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Por favor ingresa tu nombre", 
                                        "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        try {
            socket = new Socket("localhost", 5001);
            salida = new ObjectOutputStream(socket.getOutputStream());
            entrada = new ObjectInputStream(socket.getInputStream());
            
            conectado = true;
            actualizarInterfaz();
            
            // Recibir mensaje de bienvenida
            String bienvenida = (String) entrada.readObject();
            agregarMensaje("SISTEMA: " + bienvenida);
            
            // Enviar nombre al servidor
            salida.writeObject("NOMBRE:" + nombreUsuario);
            
            agregarMensaje("SISTEMA: Conectado como " + nombreUsuario);
            
            // Iniciar hilo para recibir mensajes
            Thread hiloRecepcion = new Thread(this::recibirMensajes);
            hiloRecepcion.setDaemon(true);
            hiloRecepcion.start();
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error al conectar: " + e.getMessage(), 
                                        "Error de Conexión", JOptionPane.ERROR_MESSAGE);
            conectado = false;
            actualizarInterfaz();
        }
    }
    
    private void desconectar() {
        try {
            if (conectado && salida != null) {
                salida.writeObject("salir");
            }
            
            conectado = false;
            
            if (entrada != null) entrada.close();
            if (salida != null) salida.close();
            if (socket != null) socket.close();
            
            actualizarInterfaz();
            agregarMensaje("SISTEMA: Desconectado del chat");
            
        } catch (Exception e) {
            System.out.println("Error al desconectar: " + e.getMessage());
        }
    }
    
    private void enviarMensaje() {
        String mensaje = campoMensaje.getText().trim();
        
        if (mensaje.isEmpty() || !conectado) {
            return;
        }
        
        try {
            salida.writeObject(mensaje);
            
            // Mostrar nuestro propio mensaje en el historial
            String tiempo = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
            agregarMensaje("[" + tiempo + "] Tú: " + mensaje);
            
            campoMensaje.setText("");
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error al enviar mensaje: " + e.getMessage(), 
                                        "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void recibirMensajes() {
        while (conectado) {
            try {
                String mensaje = (String) entrada.readObject();
                
                String tiempo = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
                agregarMensaje("[" + tiempo + "] " + mensaje);
                
                // Reproducir sonido de notificación
                reproducirSonido();
                
                // Hacer parpadear la ventana si no tiene foco
                if (!hasFocus()) {
                    parpadearVentana();
                }
                
            } catch (Exception e) {
                if (conectado) {
                    agregarMensaje("SISTEMA: Se perdió la conexión con el servidor");
                    conectado = false;
                    actualizarInterfaz();
                }
                break;
            }
        }
    }
    
    private void agregarMensaje(String mensaje) {
        SwingUtilities.invokeLater(() -> {
            areaHistorial.append(mensaje + "\n");
            areaHistorial.setCaretPosition(areaHistorial.getDocument().getLength());
        });
    }
    
    private void actualizarInterfaz() {
        SwingUtilities.invokeLater(() -> {
            btnConectar.setEnabled(!conectado);
            btnDesconectar.setEnabled(conectado);
            campoNombre.setEnabled(!conectado);
            campoMensaje.setEnabled(conectado);
            btnEnviar.setEnabled(conectado);
            
            if (conectado) {
                campoMensaje.requestFocus();
            }
        });
    }
    
    private void reproducirSonido() {
        try {
            Toolkit.getDefaultToolkit().beep();
        } catch (Exception e) {
            System.out.println("No se pudo reproducir sonido");
        }
    }
    
    private void parpadearVentana() {
        Timer timer = new Timer(500, null);
        timer.addActionListener(new ActionListener() {
            private int contador = 0;
            private String tituloOriginal = getTitle();
            
            @Override
            public void actionPerformed(ActionEvent e) {
                if (contador % 2 == 0) {
                    setTitle("*** NUEVO MENSAJE *** - " + tituloOriginal);
                } else {
                    setTitle(tituloOriginal);
                }
                
                contador++;
                if (contador >= 6) {
                    timer.stop();
                    setTitle(tituloOriginal);
                }
            }
        });
        timer.start();
    }
}