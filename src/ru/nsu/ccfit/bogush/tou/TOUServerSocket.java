package ru.nsu.ccfit.bogush.tou;

import ru.nsu.ccfit.bogush.tcp.TCPUnknownPacketTypeException;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

import static ru.nsu.ccfit.bogush.tou.TOUSocket.MAX_PACKET_SIZE;
import static ru.nsu.ccfit.bogush.tou.TOUSocket.QUEUE_CAPACITY;
import static ru.nsu.ccfit.bogush.tou.TOUSocket.TIMEOUT;

public class TOUServerSocket {
    private final TOUConnectionManager connectionManager = new TOUConnectionManager();
    private DatagramSocket socket;

    public TOUServerSocket() throws IOException {}

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
        connectionManager.sender(new TOUSender(socket, QUEUE_CAPACITY, TIMEOUT));
        connectionManager.receiver(new TOUReceiver(socket, MAX_PACKET_SIZE));
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
        if (isBound() != bound) throw new IOException("Socket is " + (isBound() ? "" : "not ") + "bound");
    }
}
