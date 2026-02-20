package es.iescamas.socket;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

class HiloPorClienteServidorTest {

    private HiloPorClienteServidor server;
    private Thread serverThread;
    private int port;

    @BeforeEach
    void startServer() throws Exception {
        try (ServerSocket tmp = new ServerSocket(0)) {
            port = tmp.getLocalPort();
        }
        server = new HiloPorClienteServidor(port);
        serverThread = new Thread(server, "test-server");
        serverThread.start();
        waitUntilListening("127.0.0.1", port, 800);
    }

    @AfterEach
    void stopServer() throws Exception {
        server.stop();
        serverThread.join(500);
    }

    @Test
    @DisplayName("GET /nombre/Ana devuelve 200 OK y 'Hola Ana'")
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    @Tag("http")
    void shouldSayHelloFromNombreRoute() throws Exception {
        String response = httpGet("/nombre/Ana");
        assertTrue(response.contains("200 OK"), "Debe devolver 200 OK");
        assertTrue(response.contains("Hola Ana"), "Debe contener el saludo");
    }

    @Test
    @DisplayName("GET a ruta desconocida devuelve 404 Not Found")
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    @Tag("http")
    void shouldReturn404ForUnknownRoute() throws Exception {
        String response = httpGet("/noexiste");
        assertTrue(response.contains("404 Not Found"), "El header HTTP debe ser 404");
        assertTrue(response.contains("Error 404"), "El HTML debe mostrar el mensaje de error");
    }

    @Test
    @DisplayName("Concurrencia: Responde correctamente a 2 clientes simultáneos")
    @Timeout(value = 3, unit = TimeUnit.SECONDS)
    @Tag("concurrency")
    void shouldHandleTwoClientsConcurrently() throws Exception {
        // Lanzamos dos peticiones en paralelo usando CompletableFuture
        CompletableFuture<String> peticion1 = CompletableFuture.supplyAsync(() -> {
            try { return httpGet("/nombre/ClienteUno"); } 
            catch (Exception e) { throw new RuntimeException(e); }
        });

        CompletableFuture<String> peticion2 = CompletableFuture.supplyAsync(() -> {
            try { return httpGet("/nombre/ClienteDos"); } 
            catch (Exception e) { throw new RuntimeException(e); }
        });

        // Esperamos a que ambas terminen y recogemos su respuesta
        String respuesta1 = peticion1.join();
        String respuesta2 = peticion2.join();

        // Validamos que ambas llegaron correctamente
        assertTrue(respuesta1.contains("Hola ClienteUno"), "Falla la respuesta del Cliente 1");
        assertTrue(respuesta2.contains("Hola ClienteDos"), "Falla la respuesta del Cliente 2");
    }

    private String httpGet(String path) throws Exception {
        try (Socket s = new Socket("127.0.0.1", port);
             OutputStream out = s.getOutputStream();
             InputStream in = s.getInputStream()) {

            String req = "GET " + path + " HTTP/1.1\r\n" +
                         "Host: localhost\r\n" +
                         "Connection: close\r\n\r\n";

            out.write(req.getBytes(StandardCharsets.US_ASCII));
            out.flush();
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private void waitUntilListening(String host, int port, long maxMs) throws Exception {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < maxMs) {
            try (Socket ignored = new Socket(host, port)) {
                return;
            } catch (IOException e) {
                Thread.sleep(50);
            }
        }
        fail("El servidor no abrió el puerto a tiempo");
    }
}
