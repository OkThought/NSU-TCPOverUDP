package ru.nsu.ccfit.bogush.tou;

import ru.nsu.ccfit.bogush.tcp.TCPPacket;
import ru.nsu.ccfit.bogush.tcp.TCPUnknownPacketTypeException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

public class TOUSocket {
    private static final int MAX_DATA_SIZE = 1024; // bytes
    private static final int MAX_PACKET_SIZE = MAX_DATA_SIZE + TCPPacket.HEADER_SIZE;
    private static final int QUEUE_CAPACITY = 512;
    private final TOUConnectionManager connectionManager = new TOUConnectionManager();
    private DatagramSocket socket;

    public TOUSocket() throws IOException {
        bind(new InetSocketAddress(0));
    }

    public TOUSocket (InetAddress address, int port) throws IOException {
        socket = new DatagramSocket();
        connectionManager.bind(socket);
        connect(address, port);
    }

    public boolean isBound() {
        return socket != null;
    }

    public void bind(SocketAddress socketAddress) throws IOException {
        checkBound(true);
        socket = new DatagramSocket(socketAddress);
        connectionManager.bind(socket);
    }


    public void connect (InetAddress address, int port) throws IOException {
        checkBound(false);
        connectionManager.sender(new TOUSender(socket, QUEUE_CAPACITY));
        connectionManager.receiver(new TOUReceiver(socket, MAX_PACKET_SIZE));
        try {
            connectionManager.connect(address, port);
        } catch (InterruptedException | TCPUnknownPacketTypeException e) {
            throw new IOException(e);
        }
    }

    public InputStream getInputStream () throws IOException {
        return new TOUSocketInputStream(connectionManager.receiver(), socket.getInetAddress(), socket.getPort());
    }


    public OutputStream getOutputStream () throws IOException {
        return new TOUSocketOutputStream(connectionManager.sender(),
                socket.getLocalAddress(), socket.getLocalPort(),
                socket.getInetAddress(), socket.getPort(), MAX_DATA_SIZE);
    }

    public void close () throws IOException {

    }

    private void checkBound(boolean bound) throws IOException {
        if (isBound() == bound) throw new IOException("Socket is " + (isBound() ? "" : "not ") + "bound");
    }
}
