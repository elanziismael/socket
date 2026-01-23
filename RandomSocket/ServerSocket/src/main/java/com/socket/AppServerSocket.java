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
    
    // Estados del juego
    private static boolean esperandoRespuesta = false; 
    private static boolean pistaDada = false; // NUEVO: Controla si ya dimos la pista

    public static void main( String[] args ) 
    {
        generarNuevoNumero();
        
        try {
            ServerSocket srvSock = new ServerSocket(PORT);
            System.out.println("ServerSocket en puerto: " + PORT);

            Socket client = srvSock.accept();
            mostrarInfoCliente(client);
            
            PrintWriter salida = new PrintWriter(client.getOutputStream(), true);
            BufferedReader entrada = new BufferedReader(new InputStreamReader(client.getInputStream()));
        
            // Mensaje de bienvenida simple (sin pista todavía)
            salida.println("<server> Bienvenido. Escribe un número del 1 al 10 para empezar:");

            String datoRec, datoEnv;
            
            while((datoRec = entrada.readLine())!= null) {
  
                if (esperandoRespuesta) {
                    datoEnv = procesarRespuestaReinicio(datoRec);
                    if (datoEnv.equals("EXIT")) {
                        salida.println("<server> Gracias por jugar. Adiós.");
                        break; 
                    }
                } else {
                    datoEnv = checkNumero(datoRec);
                }
                salida.println(datoEnv);
            }
            
            client.close();
            srvSock.close();
            
        } catch(IOException e) {
            System.err.println("Problemas en el socket");
            e.printStackTrace();
        }
    }

    private static void generarNuevoNumero() {
        numGen = (new Random()).nextInt(10) + 1;
        System.out.println("Nuevo número: " + numGen); 
        // Reseteamos la pista para la nueva partida
        pistaDada = false; 
    }

    private static void mostrarInfoCliente(Socket client) {
            InetAddress clientAddress = client.getInetAddress();
            String clientIP = clientAddress.getHostAddress();
            System.out.println("Cliente conectado desde: " + clientIP);
    }

    private static String obtenerPista() {
        if (numGen % 2 == 0) {
            return "(Pista: Es PAR)";
        } else {
            return "(Pista: Es IMPAR)";
        }
    }

    private static String procesarRespuestaReinicio(String datoRec) {
        if (datoRec.equalsIgnoreCase("y") || datoRec.equalsIgnoreCase("yes")) {
            generarNuevoNumero();
            esperandoRespuesta = false; 
            return "<server> ¡Nueva partida! Escribe un número del 1 al 10.";
        } else if (datoRec.equalsIgnoreCase("n") || datoRec.equalsIgnoreCase("no")) {
            return "EXIT"; 
        } else {
            return "<server> Opción no válida. ¿Quieres jugar de nuevo? (y/n)";
        }
    }

    private static String checkNumero(String datoRec) {
        try {
            int numero = Integer.parseInt(datoRec);
            
            if (numero < 1 || numero > 10) {
                return "<server> ¡Error! Te has salido del rango (1-10)";
            }
            
            // LÓGICA PRINCIPAL MODIFICADA
            if(numero > numGen) {
                String msg = "<server> El número es menor";
                // Si es la primera vez que falla, le pegamos la pista al mensaje
                if (!pistaDada) {
                    msg += ". " + obtenerPista();
                    pistaDada = true; // Marcamos para que no salga más veces
                }
                return msg;
                
            } else if(numero < numGen) {
                String msg = "<server> El número es mayor";
                // Si es la primera vez que falla, le pegamos la pista al mensaje
                if (!pistaDada) {
                    msg += ". " + obtenerPista();
                    pistaDada = true;
                }
                return msg;
                
            } else {
                esperandoRespuesta = true; 
                return "<server> ¡Has acertado! ¿Quieres jugar otra vez? (y/n)";
            }
            
        } catch(NumberFormatException e) {
            return "<Server> Por favor, introduzca un número válido";
        }
    }
}