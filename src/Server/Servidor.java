package Server;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Servidor {
    private static List<ClienteConectado> clientes = new ArrayList<>();
    
    public static void main(String[] args) {
        try {
            ServerSocket servidor = new ServerSocket(5000);
            System.out.println("=== SERVIDOR INICIADO ===");
            System.out.println("Esperando clientes en puerto 5001...");
            System.out.println("Los clientes podrán chatear entre sí");
            
            while (true) {
                Socket clienteSocket = servidor.accept();
                System.out.println("¡Cliente conectado desde: " + clienteSocket.getInetAddress());
                
                ClienteConectado cliente = new ClienteConectado(clienteSocket);
                clientes.add(cliente);
                
                Thread hiloCliente = new Thread(cliente);
                hiloCliente.start();
                
                System.out.println("Total clientes conectados: " + clientes.size());
            }
            
        } catch (Exception e) {
            System.out.println("Error en el servidor: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // Método para reenviar mensaje a todos los demás clientes
    public static synchronized void reenviarMensaje(String mensaje, ClienteConectado remitente) {
        System.out.println("Reenviando mensaje: " + mensaje);
        
        for (ClienteConectado cliente : clientes) {
            if (cliente != remitente && cliente.isActivo()) {
                cliente.enviarMensaje(mensaje);
            }
        }
    }
    
    // Remover cliente cuando se desconecta
    public static synchronized void removerCliente(ClienteConectado cliente) {
        clientes.remove(cliente);
        System.out.println("Cliente desconectado. Clientes restantes: " + clientes.size());
        
        // Notificar a otros que alguien se desconectó
        reenviarMensaje("SISTEMA: Un usuario se desconectó", cliente);
    }
    
    // Notificar cuando alguien se conecta
    public static synchronized void notificarNuevaConexion(String nombreUsuario, ClienteConectado nuevoCliente) {
        String mensaje = "SISTEMA: " + nombreUsuario + " se unió al chat";
        reenviarMensaje(mensaje, nuevoCliente);
    }
}

// Clase interna para manejar cada cliente
class ClienteConectado implements Runnable {
    private Socket socket;
    private ObjectInputStream entrada;
    private ObjectOutputStream salida;
    private String nombreUsuario;
    private boolean activo = true;
    
    public ClienteConectado(Socket socket) {
        this.socket = socket;
    }
    
    @Override
    public void run() {
        try {
            salida = new ObjectOutputStream(socket.getOutputStream());
            entrada = new ObjectInputStream(socket.getInputStream());
            
            // Mensaje de bienvenida
            salida.writeObject("¡Bienvenido al chat grupal!");
            
            // Recibir mensajes del cliente
            while (activo) {
                try {
                    String mensajeRecibido = (String) entrada.readObject();
                    
                    if (mensajeRecibido.equalsIgnoreCase("salir")) {
                        break;
                    }
                    
                    // Si el mensaje contiene "NOMBRE:" es el nombre del usuario
                    if (mensajeRecibido.startsWith("NOMBRE:")) {
                        nombreUsuario = mensajeRecibido.substring(7);
                        System.out.println("Usuario registrado: " + nombreUsuario);
                        Servidor.notificarNuevaConexion(nombreUsuario, this);
                    } else {
                        // Es un mensaje normal, reenviarlo
                        String mensajeCompleto = nombreUsuario + ": " + mensajeRecibido;
                        Servidor.reenviarMensaje(mensajeCompleto, this);
                    }
                    
                } catch (Exception e) {
                    System.out.println("Error recibiendo mensaje del cliente");
                    break;
                }
            }
            
        } catch (Exception e) {
            System.out.println("Error en manejo del cliente: " + e.getMessage());
        } finally {
            cerrarConexion();
        }
    }
    
    public void enviarMensaje(String mensaje) {
        try {
            if (salida != null && activo) {
                salida.writeObject(mensaje);
            }
        } catch (Exception e) {
            System.out.println("Error enviando mensaje al cliente");
            activo = false;
        }
    }
    
    private void cerrarConexion() {
        try {
            activo = false;
            if (entrada != null) entrada.close();
            if (salida != null) salida.close();
            if (socket != null) socket.close();
            Servidor.removerCliente(this);
        } catch (Exception e) {
            System.out.println("Error cerrando conexión");
        }
    }
    
    public boolean isActivo() {
        return activo;
    }
}