package Client;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;

public class Cliente {
    public static void main(String[] args) {
        try {
            // Cambiar "localhost" por la IP del servidor si están en computadoras diferentes
            // Ejemplo: "192.168.1.100" 
            Socket cliente = new Socket("localhost", 5001);
            
            System.out.println("=== CLIENTE CONECTADO ===");
            System.out.println("Conectado al servidor: " + cliente.getRemoteSocketAddress());
            
            // Streams para comunicación
            ObjectInputStream entrada = new ObjectInputStream(cliente.getInputStream());
            ObjectOutputStream salida = new ObjectOutputStream(cliente.getOutputStream());
            Scanner scanner = new Scanner(System.in);
            
            // Recibir mensaje de bienvenida
            String mensajeBienvenida = (String) entrada.readObject();
            System.out.println("Servidor dice: " + mensajeBienvenida);
            System.out.println("\n--- Chat iniciado (escribe 'salir' para terminar) ---\n");
            
            // Ciclo infinito de conversación
            while (true) {
                // Cliente escribe mensaje
                System.out.print("Tu mensaje: ");
                String mensaje = scanner.nextLine();
                
                // Enviar mensaje al servidor
                salida.writeObject(mensaje);
                
                // Si el cliente escribe "salir", terminar
                if (mensaje.equalsIgnoreCase("salir")) {
                    // Recibir mensaje de despedida
                    String despedida = (String) entrada.readObject();
                    System.out.println("Servidor dice: " + despedida);
                    break;
                }
                
                try {
                    // Recibir respuesta del servidor
                    String respuestaServidor = (String) entrada.readObject();
                    System.out.println("Servidor dice: " + respuestaServidor);
                    
                    // Si el servidor se desconecta
                    if (respuestaServidor.contains("se desconecta")) {
                        break;
                    }
                    
                } catch (Exception e) {
                    System.out.println("Servidor se desconectó");
                    break;
                }
            }
            
            // Cerrar conexiones
            scanner.close();
            cliente.close();
            
        } catch (Exception e) {
            System.out.println("Error al conectar con el servidor: " + e.getMessage());
            System.out.println("Verifica que el servidor esté ejecutándose y la IP sea correcta");
        }
    }
}