package ru.nsu.ccfit.bogush.tou;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import static org.junit.Assert.*;

public class TOUTest {
    private static final long CONNECT_TIMEOUT = 1;
    private TOUServerSocket serverSocket;
    private TOUSocket clientSocket;
    private TOUSocket acceptedSocket;
    private InetAddress localHost;
    private final int serverPort = 50000;
    private final int clientPort = serverPort + 1;
    private Thread clientThread;

    @Before
    public void setUp() throws Exception {
        serverSocket = new TOUServerSocket();
        clientSocket = new TOUSocket();
        localHost = InetAddress.getLocalHost();
        clientThread = new Thread(() -> {
            try {
                clientSocket.connect(localHost, serverPort);
            } catch (IOException e) {
                e.printStackTrace();
            }
            assertTrue(clientSocket.isConnected());
        }, "client");
    }

    @Test
    public void test() throws IOException, InterruptedException {
        bind();
        accept();
        connect();
        close();
    }

    public void bind() throws IOException {
        assertFalse(serverSocket.isBound());
        serverSocket.bind(new InetSocketAddress(localHost, serverPort));
        assertTrue(serverSocket.isBound());

        assertFalse(clientSocket.isBound());
        clientSocket.bind(new InetSocketAddress(localHost, clientPort));
        assertTrue(clientSocket.isBound());
    }

    public void connect() throws IOException, InterruptedException {
        assertFalse(clientSocket.isConnected());
        clientThread.start();
        assertTrue(clientThread.isAlive());
        clientThread.join(CONNECT_TIMEOUT);
        assertFalse(clientThread.isAlive());
        assertTrue(clientSocket.isConnected());
    }

    public void accept() throws IOException {
        acceptedSocket = serverSocket.accept();
        assertNotNull(acceptedSocket);
        assertTrue(acceptedSocket.isConnected());
    }

    public void close() {

    }
}
