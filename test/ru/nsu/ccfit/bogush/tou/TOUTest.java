package ru.nsu.ccfit.bogush.tou;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.*;

import static org.junit.Assert.*;

public class TOUTest {
    private static final long ACCEPT_TIMEOUT = 10;
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private Socket acceptedSocket;
    private InetAddress localHost;
    private final int serverPort = 50000;
    private Thread acceptor;

    @Before
    public void setUp() throws Exception {
        SocketImplFactory implFactory = new TOUSocketImplFactory();
        Socket.setSocketImplFactory(implFactory);
        ServerSocket.setSocketFactory(implFactory);
        serverSocket = new ServerSocket();
        localHost = InetAddress.getLocalHost();
        acceptor = new Thread(() -> {
            try {
                acceptedSocket = serverSocket.accept();
                acceptedSocket.getOutputStream().write("TEST".getBytes());
            } catch (IOException e) {
                e.printStackTrace();
                assertFalse(true);
            }
        }, "acceptor");
    }

    @Test
    public void test() throws IOException, InterruptedException {
        bind();
        connect();
//        write();
        read();
        close(clientSocket);
        close(acceptedSocket);
    }

    private void bind() throws IOException {
        assertFalse(serverSocket.isBound());
        serverSocket.bind(new InetSocketAddress(localHost, serverPort));
        assertTrue(serverSocket.isBound());
    }

    private void connect() throws IOException, InterruptedException {
//        assertFalse(clientSocket.isConnected());
        acceptor.start();
        assertTrue(acceptor.isAlive());
        clientSocket = new Socket(localHost, serverPort);
        acceptor.join(ACCEPT_TIMEOUT);
        assertFalse(acceptor.isAlive());
        assertTrue(clientSocket.isConnected());
    }

    private void write() {

    }

    private void read() throws IOException {
        byte[] bytes = new byte[10];
        int bytesRead = clientSocket.getInputStream().read(bytes);
        System.out.println(new String(bytes, 0, bytesRead));
    }

    private void close(Socket c) throws IOException {
        assertNotNull(c);
        assertFalse(c.isClosed());
        c.close();
        assertTrue(c.isClosed());
    }
}
