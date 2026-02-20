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
 * Servidor multihilo que atiende peticiones HTTP básicas.
 * * @author Ismail El Anzi
 * @version 1.0
 * @since 2026-02
 * @apiNote Ejemplo de uso para la ruta de saludo:
 * <pre>
 * http://localhost:9001/nombre/Ana
 * </pre>
 */
public class HiloPorClienteServidor implements Runnable {

    protected int serverPort;
    protected ServerSocket serversocket = null;
    protected boolean isStopped;
    protected Thread runningThread = null;

    /**
     * Constructor del servidor.
     * * @param serverPort El puerto donde el servidor escuchará las conexiones.
     */
    public HiloPorClienteServidor(int serverPort) {
        this.serverPort = serverPort;
    }

    @Override
    public void run() {
        synchronized (this) {
            this.runningThread = Thread.currentThread();
        }
        openServerSocket();
        while (!isStopped()) {
            try {
                Socket clientSocket = this.serversocket.accept();
                new Thread(() -> {
                    try {
                        processClientRequest(clientSocket);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }).start();
            } catch (IOException e) {
                if (isStopped()) return;
                throw new RuntimeException("Error aceptando conexión", e);
            }
        }
    }

    /**
     * Lee la petición HTTP del cliente, procesa la ruta y envía la respuesta HTML.
     * * @param clientSocket El socket que representa la conexión con el cliente.
     * @throws IOException Si ocurre un error de lectura/escritura en los flujos del socket.
     * @apiNote
     * Ejemplos de rutas admitidas:
     * - /nombre/Ana
     * - /rutainventada (Devolverá error 404)
     */
    private void processClientRequest(Socket clientSocket) throws IOException {
        try (InputStream input = clientSocket.getInputStream();
             OutputStream out = clientSocket.getOutputStream();
             BufferedReader in = new BufferedReader(new InputStreamReader(input))) {

            String line = in.readLine();
            if (line == null) return;

            String[] requestParts = line.split(" ");
            String path = requestParts.length > 1 ? requestParts[1] : "/";

            if ("/favicon.ico".equals(path)) {
                serveFavicon(out);
                return;
            }

            // 1. Calculamos la fecha/hora actual
            String horaActual = new SimpleDateFormat("HH:mm:ss").format(new Date());
            String fechaActual = new SimpleDateFormat("dd/MM/yyyy").format(new Date());

            // 2. Lógica del nombre y 404
            String statusHttp = "200 OK";
            String mensajePrincipal = "Servidor OK";

            if (path.equals("/")) {
                mensajePrincipal = "Servidor OK";
            } else if (path.startsWith("/nombre/")) {
                String nombre = path.substring(8);
                if (!nombre.isEmpty()) {
                    mensajePrincipal = "Hola " + nombre.replace("%20", " ");
                }
            } else {
                statusHttp = "404 Not Found";
                mensajePrincipal = "Error 404: Ruta no encontrada";
            }

            String clientIp = clientSocket.getInetAddress().getHostAddress();
            String nombreHilo = Thread.currentThread().getName();

            // 3. HTML con la hora debajo del nombre
            String body = "<html>"
                    + "<head>"
                    + "<link rel='icon' href='/favicon.ico'>"
                    + "<title>PSP - Saludo</title>"
                    + "</head>"
                    + "<body style='background-color: coral; font-family: sans-serif; text-align: center; padding-top: 50px;'>"
                    + "<h1 style='color:blue; margin-bottom: 0;'>" + mensajePrincipal + "</h1>"
                    + "<h2 style='color: #333; margin-top: 5px;'>Hora actual: " + horaActual + "</h2>"
                    + "<p style='color: #555;'>Fecha: " + fechaActual + "</p>"
                    + "<hr style='width: 50%;'>"
                    + "<p>IP del Cliente: " + clientIp + "</p>"
                    + "<p>Ruta: " + path + "</p>"
                    + "<p>Atendido por: " + nombreHilo + "</p>"
                    + "</body></html>";

            // 4. Construcción de cabeceras HTTP
            String header = "HTTP/1.1 " + statusHttp + "\r\n" +
                    "Content-Type: text/html; charset=UTF-8\r\n" +
                    "Content-Length: " + body.getBytes(StandardCharsets.UTF_8).length + "\r\n" +
                    "Connection: close\r\n\r\n";

            out.write(header.getBytes(StandardCharsets.UTF_8));
            out.write(body.getBytes(StandardCharsets.UTF_8));
            out.flush();
        } finally {
            clientSocket.close();
        }
    }

    private void serveFavicon(OutputStream out) throws IOException {
        try (InputStream iconStream = HiloPorClienteServidor.class.getResourceAsStream("/favicon.ico")) {
            if (iconStream == null) {
                out.write(("HTTP/1.1 404 Not Found\r\n\r\n").getBytes());
                return;
            }
            byte[] iconBytes = iconStream.readAllBytes();
            String headers = "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: image/x-icon\r\n" +
                    "Content-Length: " + iconBytes.length + "\r\n" +
                    "Connection: close\r\n\r\n";
            out.write(headers.getBytes());
            out.write(iconBytes);
            out.flush();
        }
    }

    /**
     * Comprueba si el servidor ha recibido la orden de detenerse.
     * * @return true si se ha llamado al método stop().
     */
    private synchronized boolean isStopped() { return isStopped; }

    private void openServerSocket() {
        try {
            this.serversocket = new ServerSocket(this.serverPort);
        } catch (IOException ex) {
            throw new RuntimeException("Error en puerto " + serverPort, ex);
        }
    }

    public synchronized void stop() {
        this.isStopped = true;
        try {
            if (this.serversocket != null) this.serversocket.close();
        } catch (IOException e) {
            System.err.println(e);
        }
    }
}