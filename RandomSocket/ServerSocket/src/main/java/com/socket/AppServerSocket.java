package com.socket;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;

public class AppServerSocket 
{
    private final static int PORT = 7777;
    private static int numGen;
    // Variable para saber si estamos esperando "y" o "n"
    private static boolean esperandoRespuesta = false; 

    public static void main( String[] args ) 
    {
        // Generamos el primer número al arrancar
        generarNuevoNumero();
        
        try {
            ServerSocket srvSock = new ServerSocket(PORT);
            System.out.println("ServerSocket en puerto: " + PORT);

            // Abrimos el socket y escuchamos
            Socket client = srvSock.accept();
            mostrarInfoCliente(client);
            
            PrintWriter salida = new PrintWriter(client.getOutputStream(), true);
            BufferedReader entrada = new BufferedReader(new InputStreamReader(client.getInputStream()));
        
            String datoRec, datoEnv;
            
            // Bucle principal de lectura
            while((datoRec = entrada.readLine())!= null) {
  
                // --- LÓGICA PRINCIPAL ---
                if (esperandoRespuesta) {
                    // Si el juego acabó, procesamos la respuesta de reinicio (y/n)
                    datoEnv = procesarRespuestaReinicio(datoRec);
                    
                    // Si la respuesta fue "n", rompemos el bucle para cerrar
                    if (datoEnv.equals("EXIT")) {
                        salida.println("<server> Gracias por jugar. Adiós.");
                        break; 
                    }
                } else {
                    // Si estamos jugando, comprobamos el número
                    datoEnv = checkNumero(datoRec);
                }
                // ------------------------

                salida.println(datoEnv);
            }
            
            // Cerrar recursos al salir del bucle
            client.close();
            srvSock.close();
            
        } catch(IOException e) {
            System.err.println("Problemas en el socket");
            e.printStackTrace();
        }
    }

    // Método para generar un nuevo número aleatorio (1-10)
    private static void generarNuevoNumero() {
        numGen = (new Random()).nextInt(10) + 1;
        System.out.println("Nuevo número generado: " + numGen); // Chivato para el servidor
    }

    private static void mostrarInfoCliente(Socket client) {
            InetAddress clientAddress = client.getInetAddress();
            String clientIP = clientAddress.getHostAddress();
            String hostName = clientAddress.getHostName();
            System.out.println("IP:" + clientIP + ", HostName: "+ hostName);
    }

    // Lógica para procesar "y" o "n"
    private static String procesarRespuestaReinicio(String datoRec) {
        if (datoRec.equalsIgnoreCase("y") || datoRec.equalsIgnoreCase("yes")) {
            generarNuevoNumero();
            esperandoRespuesta = false; // Volvemos al estado de juego
            return "<server> ¡Nueva partida comenzada! Adivina el número del 1 al 10.";
        } else if (datoRec.equalsIgnoreCase("n") || datoRec.equalsIgnoreCase("no")) {
            return "EXIT"; // Palabra clave para cerrar el servidor
        } else {
            return "<server> Opción no válida. ¿Quieres jugar de nuevo? (y/n)";
        }
    }

    private static String checkNumero(String datoRec) {
        try {
            int numero = Integer.parseInt(datoRec);
            
            // Validación del rango 1-10
            if (numero < 1 || numero > 10) {
                return "<server> ¡Error! Te has salido del rango (debe ser entre 1 y 10)";
            }
            
            if(numero > numGen) {
                return "<server> El número es mayor que el número mágico";
            } else if(numero < numGen) {
                return "<server> El número es menor que el número mágico";
            } else {
                // ¡Ha ganado! Cambiamos el estado para esperar respuesta
                esperandoRespuesta = true; 
                return "<server> ¡Has acertado el número! ¿Quieres jugar otra vez? (y/n)";
            }
            
        } catch(NumberFormatException e) {
            return "<Server> Por favor, introduzca un número válido";
        }
    }
}
