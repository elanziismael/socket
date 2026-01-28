package es.iescamas.socket;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;


class MainTest {

    @Test
    void serverShouldRespond200OkAndStop() {
        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {

            int port = findFreePort();

            HiloPorClienteServidor server = new HiloPorClienteServidor(port);
            Thread serverThread = new Thread(server, "test-server-thread");
            serverThread.start();

            waitUntilPortIsOpen("127.0.0.1", port, 1500);

            String response = httpGetRaw("127.0.0.1", port, "/");

            assertTrue(response.startsWith("HTTP/1.1 200 OK"),
                    () -> "Expected HTTP 200 response, but got:\n" + response);

            assertTrue(response.contains("<html>") || response.contains("Server:"),
                    () -> "Expected body content, but got:\n" + response);

            server.stop();

            // Tras stop(), el puerto debería cerrarse pronto
            waitUntilPortIsClosed("127.0.0.1", port, 1500);
        });
    }

    // --- Helpers ---

    private static String httpGetRaw(String host, int port, String path) throws IOException {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 1000);
            socket.setSoTimeout(1000);

            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();

            String request =
                    "GET " + path + " HTTP/1.1\r\n" +
                    "Host: " + host + ":" + port + "\r\n" +
                    "Connection: close\r\n" +
                    "\r\n";

            out.write(request.getBytes(StandardCharsets.US_ASCII));
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
        fail("Server did not open port " + port + " within " + timeoutMs + "ms");
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
        fail("Server port " + port + " was still open after stop() within " + timeoutMs + "ms");
    }
}
