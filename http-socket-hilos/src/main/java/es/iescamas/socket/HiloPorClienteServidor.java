package es.iescamas.socket;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Servidor TCP monohilo (single-thread) que atiende peticiones de forma secuencial.
 *
 * Idea clave:
 * - Acepta una conexión (accept)
 * - Procesa la petición completa
 * - Cierra la conexión
 * - Vuelve a aceptar otra
 *
 * Esto es intencionalmente simple (didáctico). En un servidor real:
 * - se usaría pool de hilos (ExecutorService) o I/O no bloqueante (NIO)
 * - se manejarían timeouts, parsing HTTP, cabeceras correctas, etc.
 */
public class HiloPorClienteServidor implements Runnable {

    /**
     * Puerto donde escucha el servidor.
     * Nota: "protected" permite herencia, pero en un proyecto real suele ser "private final".
     */
    protected int serverPort = 9001;

    /**
     * ServerSocket: socket que permanece escuchando conexiones entrantes.
     * Se inicializa en openServerSocket().
     */
    protected ServerSocket serversocket = null;

    /**
     * Flag de parada.
     * IMPORTANTE: al ser leído/escrito desde hilos distintos, lo ideal es hacerlo "volatile"
     * o acceder a él siempre sincronizado para garantizar visibilidad.
     */
    protected boolean isStopped;

    /**
     * Referencia al hilo que está ejecutando run().
     * Útil para depuración / logging / diagnósticos.
     */
    protected Thread runningThread = null;

    public HiloPorClienteServidor(int serverPort) {
        this.serverPort = serverPort;
    }

    /**
     * Punto de entrada del hilo del servidor.
     *
     * Flujo:
     * 1) Guardar el hilo actual (para debug)
     * 2) Abrir el ServerSocket
     * 3) Bucle: accept() -> processClientRequest() hasta stop()
     *
     * accept() es BLOQUEANTE: el hilo se queda esperando hasta que llegue un cliente.
     */
    @Override
    public void run() {
        // Guardamos el hilo que ejecuta el servidor (no imprescindible, pero útil).
        synchronized (this) {
            this.runningThread = Thread.currentThread();
        }

        // Abre el puerto y empieza a escuchar.
        openServerSocket();

        // Bucle principal del servidor.
        while (!isStopped()) {
            try {
                // Espera un cliente. Devuelve un Socket (conexión) cuando alguien conecta.
                Socket clientSocket = this.serversocket.accept();

                // Procesa la petición en ESTE MISMO hilo.
                // Por eso este servidor es "single-thread": hasta que no termina,
                // no atiende a otro cliente.
                //processClientRequest(clientSocket);
                
                new Thread(() -> {
                    try {
                        processClientRequest(clientSocket);
                    
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }, "client-" + clientSocket.getPort()).start();

            } catch (IOException e) {
                // Caso típico: cuando llamas a stop(), cierras el ServerSocket.
                // Eso hace que accept() lance IOException y salgamos limpiamente.
                if (isStopped()) {
                    System.out.println("Server stopped.");
                    return;
                }

                // Si no está parado, el fallo es real (puerto roto, error de red, etc.)
                throw new RuntimeException("Error accepting client connection", e);
            }
        }

        System.out.println("Server Stopped");
    }

    /**
     * Procesa la conexión de un cliente.
     */
    private void processClientRequest(Socket clientSocket) throws IOException {
        try (clientSocket;
             InputStream in = clientSocket.getInputStream();
             BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.US_ASCII));
             OutputStream out = clientSocket.getOutputStream()) {

            // 1) Leer la primera línea: "GET /ruta HTTP/1.1"
            String requestLine = br.readLine(); // puede ser null si el cliente corta
            String path = "/";

            if (requestLine != null && requestLine.startsWith("GET ")) {
                // Extrae lo que hay entre "GET " y el siguiente espacio
                int start = 4;
                int end = requestLine.indexOf(' ', start);
                if (end > start) path = requestLine.substring(start, end);
            }

            // (Opcional) Evitar “doble log” por favicon
            boolean isFavicon = path.equals("/favicon.ico");

            long time = System.currentTimeMillis();
            String fecha = new SimpleDateFormat("dd/MM/yy HH:mm:ss").format(new Date(time));

            String body = "<html><body style='background-color: coral;'>"
                    + "<h3>Servidor OK</h3>"
                    + "<p>Path: " + path + "</p>"
                    + "<p>Server: " + fecha + "</p>"
                    + "</body></html>";

            byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);

            String headers =
                    "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: text/html; charset=UTF-8\r\n" +
                    "Content-Length: " + bodyBytes.length + "\r\n" +
                    "Connection: close\r\n" +
                    "\r\n";

            out.write(headers.getBytes(StandardCharsets.US_ASCII));
            out.write(bodyBytes);
            out.flush();

            if (!isFavicon) {
                System.out.println("[" + Thread.currentThread().getName() + "] " + requestLine);
                System.out.println("[" + Thread.currentThread().getName() + "] Petición procesada: " + fecha);
            }
        }
    }


    /**
     * Devuelve si el servidor está parado.
     * synchronized fuerza visibilidad y orden entre hilos
     *
     */
    private synchronized boolean isStopped() {
        return isStopped;
    }

    /**
     * Abre el ServerSocket en el puerto configurado.
     * Si el puerto está ocupado, se lanza excepción.
     *
     * Recomendación: incluir la IOException original en el RuntimeException
     * para tener trazas completas.
     */
    private void openServerSocket() {
        try {
            this.serversocket = new ServerSocket(this.serverPort);
        } catch (IOException ex) {
            throw new RuntimeException("Cannot open port " + serverPort, ex);
        }
    }

    /**
     * Detiene el servidor.
     *
     * Estrategia:
     * - Marca el flag de parada
     * - Cierra el ServerSocket para desbloquear accept()
     *
     * La sincronización aquí ayuda, pero para consistencia total se recomienda:
     * - isStopped como volatile
     * - y/o isStopped() también synchronized
     */
    public synchronized void stop() {
        this.isStopped = true;

        try {
            // Cerrar el ServerSocket es clave: hace que accept() salga con IOException.
            if (this.serversocket != null) {
                this.serversocket.close();
            }
        } catch (IOException e) {
            System.err.println(e);
        }
    }
}
