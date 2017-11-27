package ru.nsu.ccfit.bogush.tou;

import ru.nsu.ccfit.bogush.tcp.TCPUnknownPacketTypeException;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

public class TOUServerSocket {
    private final TOUConnectionManager connectionManager = new TOUConnectionManager();
    private DatagramSocket socket;

    public TOUServerSocket() {}

    public TOUServerSocket(int port) throws IOException {
        bind(new InetSocketAddress(port));
    }

    public boolean isBound() {
        return socket != null;
    }

    public void bind(SocketAddress socketAddress) throws IOException {
        checkBound(false);
        socket = new DatagramSocket(socketAddress);
        connectionManager.bind(socket);
        connectionManager.listen();
    }

    public TOUSocket accept() throws IOException {
        checkBound(true);
        try {
            return connectionManager.accept();
        } catch (TCPUnknownPacketTypeException | InterruptedException e) {
            throw new IOException(e);
        }
    }

    private void checkBound(boolean bound) throws IOException {
        if (isBound() == bound) throw new IOException("Socket is " + (isBound() ? "" : "not ") + "bound");
    }
}
