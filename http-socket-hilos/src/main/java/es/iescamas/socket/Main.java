package es.iescamas.socket;

public class Main {

    final static int PORT = 9001;
    final static int TIME = 600; 
    
    public static void main(String[] args) {
        HiloPorClienteServidor server = new HiloPorClienteServidor(PORT);
        new Thread(server, "Hilo-Servidor-principal").start();
        
        System.out.println("Servidor corriendo en http://localhost:" + PORT);
        System.out.println("Prueba: http://localhost:" + PORT + "/nombre/Ismail");
    
        try {
            Thread.sleep(TIME * 1000);
        } catch(InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        
        server.stop();
    }
}
