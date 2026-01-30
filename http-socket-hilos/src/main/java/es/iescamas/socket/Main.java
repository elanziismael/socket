package es.iescamas.socket;
/**
 * ############################################
 * # CREA EL SERVIDOR: HiloPorClienteServidor #
 * ############################################
 */
public class Main {
		
	//Puerto donde escucha el servidor
	final static int PORT = 9001;
	//Tiempo
	final static int TIME = 260; // 1 minuto
	
	public static void main(String[] args) {
		// Creamos el servidor indicando el puerto.
		HiloPorClienteServidor server = new HiloPorClienteServidor(PORT);
		// Arrancamos el servidor en un hilo aparte.
		new Thread(server, "Hilo-Servidor-principal").start();
		
		System.out.println("Servidor en http://localhost:"+PORT + 
				" durante " + TIME + " segundos");
	
		try {
			//OJO: 60 * 1000 = 60_000 ms = 60 segundos 			
			Thread.sleep(TIME * 1000);
		}catch(InterruptedException ex) {
			Thread.currentThread().interrupt();
			System.err.print(ex);
		}
		
		System.out.println("Stopping server");
		server.stop();
	}

}
