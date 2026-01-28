package es.iescamas.socket;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@DisplayName("SingleThreadedServer (Sockets + HTTP) - Pruebas unitarias JUnit 5")
class SingleThreadedServerTest {

    private int port;
    private HiloPorClienteServidor server;
    private Thread serverThread;

    @BeforeEach
    void setUp() throws IOException {
        port = findFreePort();
        server = new HiloPorClienteServidor(port);
        serverThread = new Thread(server, "server-test-thread");
    }

    // =========================================================
    // 1) ARRANQUE DEL SERVIDOR
    // =========================================================
    // ✅ Debe comprobarse:
    // Al arrancar el servidor en un hilo:
    // - abre el puerto
    // - acepta conexiones
    @Test
    @DisplayName("run(): al arrancar, el servidor abre el puerto y acepta conexiones")
    void run_abrePuerto_y_aceptaConexion() {
        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {

            serverThread.start();

            // Esperamos a que el puerto esté disponible (servidor escuchando)
            assertDoesNotThrow(() -> waitUntilPortIsOpen("127.0.0.1", port, 1500),
                    "El servidor debería abrir el puerto y aceptar conexiones");

            // Si conecta, ya sabemos que está escuchando
            assertDoesNotThrow(() -> connect("127.0.0.1", port),
                    "Debería poder conectarse al puerto si el servidor está activo");

            server.stop();
        });
    }

    // =========================================================
    // 2) RESPUESTA HTTP 200 OK
    // =========================================================
    // ✅ Debe comprobarse:
    // Al conectar y enviar una petición "tipo HTTP":
    // - responde con "HTTP/1.1 200 OK"
    // - el body incluye "Server:" + timestamp
    @Test
    @DisplayName("processClientRequest(): responde HTTP/1.1 200 OK e incluye 'Server:' en el body")
    void responde_200OK_y_incluyeServer() {
        assertTimeoutPreemptively(Duration.ofSeconds(15), () -> {

            serverThread.start();
            waitUntilPortIsOpen("127.0.0.1", port, 1500);

            String response = sendHttpRequestAndReadAll("127.0.0.1", port);

            assertTrue(response.startsWith("HTTP/1.1 200 OK"),
                    () -> "Debe empezar por 'HTTP/1.1 200 OK' pero fue:\n" + response);

            assertTrue(response.contains("Server:"),
                    () -> "Debe incluir 'Server:' en el body pero fue:\n" + response);

            server.stop();
        });
    }

    // =========================================================
    // 3) STOP CIERRA EL PUERTO
    // =========================================================
    // ✅ Debe comprobarse:
    // Tras stop():
    // - el puerto deja de aceptar conexiones
    @Test
    @DisplayName("stop(): al detener, el servidor cierra el ServerSocket y el puerto deja de aceptar conexiones")
    void stop_cierraPuerto() {
        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {

            serverThread.start();
            waitUntilPortIsOpen("127.0.0.1", port, 1500);

            server.stop();

            assertDoesNotThrow(() -> waitUntilPortIsClosed("127.0.0.1", port, 1500),
                    "Tras stop(), el servidor debería cerrar el puerto y no aceptar conexiones");
        });
    }

    // =========================================================
    // Helpers (utilidades internas del test)
    // =========================================================

    private static Socket connect(String host, int port) throws IOException {
        Socket s = new Socket();
        s.connect(new InetSocketAddress(host, port), 1000);
        s.setSoTimeout(1000);
        s.close();
        return s;
    }

    private static String sendHttpRequestAndReadAll(String host, int port) throws IOException {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 1000);
            socket.setSoTimeout(1000);

            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();

            // Tu server NO parsea la petición: basta con enviar algo y leer su respuesta.
            String req =
                    "GET / HTTP/1.1\r\n" +
                    "Host: " + host + ":" + port + "\r\n" +
                    "Connection: close\r\n" +
                    "\r\n";

            out.write(req.getBytes(StandardCharsets.UTF_8));
            out.flush();

            return readAll(in);
        }
    }

    private static String readAll(InputStream in) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = in.read(buf)) != -1) {
            bos.write(buf, 0, n);
        }
        return bos.toString(StandardCharsets.UTF_8);
    }

    private static int findFreePort() throws IOException {
        try (ServerSocket ss = new ServerSocket(0)) {
            ss.setReuseAddress(true);
            return ss.getLocalPort();
        }
    }

    private static void waitUntilPortIsOpen(String host, int port, long timeoutMs) throws InterruptedException {
        long end = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < end) {
            try (Socket s = new Socket()) {
                s.connect(new InetSocketAddress(host, port), 200);
                return;
            } catch (IOException ignored) {
                Thread.sleep(50);
            }
        }
        fail("El servidor no abrió el puerto " + port + " en " + timeoutMs + "ms");
    }

    private static void waitUntilPortIsClosed(String host, int port, long timeoutMs) throws InterruptedException {
        long end = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < end) {
            try (Socket s = new Socket()) {
                s.connect(new InetSocketAddress(host, port), 200);
                Thread.sleep(50);
            } catch (IOException closed) {
                return; // ya no conecta => cerrado
            }
        }
        fail("El puerto " + port + " seguía abierto tras stop() después de " + timeoutMs + "ms");
    }
}
