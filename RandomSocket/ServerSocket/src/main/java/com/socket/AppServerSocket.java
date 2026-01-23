package com.socket;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;

/**
 * Hello world!
 *
 */
public class AppServerSocket 
{
	private final static int PORT = 7777;
	private static int numGen;
    public static void main( String[] args ) 
    {
    	//Generar número
    	numGen = (new Random()).nextInt(10)+1;
    	
    	try {
    		ServerSocket srvSock = new ServerSocket(PORT);
    		System.out.println("ServerSocket en puerto: " + PORT);

    		//Abrimos el socket y escuchamos
    		Socket client = srvSock.accept();
    		mostrarInfoCliente(client);
    		
    		
    		//Para mandar datos al cliente >>>
    		PrintWriter salida = new PrintWriter(client.getOutputStream(), true);
    		//Para recibir datos al cliente <<<
    		BufferedReader entrada = new BufferedReader(new InputStreamReader(client.getInputStream()));
    	
    		
    		String datoRec, datoEnv;
    		
    		//leeremos todos los mensajes recibidos
    		//comprobamos si es el número mágico
    		while((datoRec = entrada.readLine())!= null) {
  
    			datoEnv = checkNumero(datoRec);
    			
    			//Retornamos al cliente el resultado 
    			//de la comprobación
    			salida.println(datoEnv);
    		}
    		
    	}catch(IOException e) {
    		System.err.println("Problemas en el socket");
    		e.printStackTrace();
    		System.exit(1);
    	}
    }
	private static void mostrarInfoCliente(Socket client) {
			// Obtener la IP del cliente
			InetAddress clientAddress = client.getInetAddress();
			String clientIP = clientAddress.getHostAddress();
			String hostName = clientAddress.getHostName();
		
			System.out.println("IP:" + clientIP + ", HostName: "+ hostName);
	}
	private static String checkNumero(String datoRec) {
		try {
			int numero = Integer.parseInt(datoRec);
			
			if (numero < 1 || numero > 10) {
                return "<server>¡Error! Te has salido del rango (debe ser entre 1 y 10)";
            }
			
			if(numero > numGen) {
				return "<server>El número es mayor que el número mágico";
			}else if(numero < numGen) {
				return "<server>El número es menor que el número mágico";
			}else {
				return "<server>Ha adivinado el número";
			}
			
			
		}catch(NumberFormatException e) {
			return "<Server>Por favor, introduzca un número";
		}
		
	}
}
