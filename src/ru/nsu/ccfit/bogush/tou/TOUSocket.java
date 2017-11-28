package ru.nsu.ccfit.bogush.tou;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.nsu.ccfit.bogush.tcp.TCPPacket;
import ru.nsu.ccfit.bogush.tcp.TCPUnknownPacketTypeException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;

public class TOUSocket {
    static {
        TOULog4JUtils.initIfNotInitYet();
    }
    private static final Logger LOGGER = LogManager.getLogger("Socket");

    static final int MAX_DATA_SIZE = 1024; // bytes
    static final int MAX_PACKET_SIZE = MAX_DATA_SIZE + TCPPacket.HEADER_SIZE;
    static final int QUEUE_CAPACITY = 512;
    static final int TIMEOUT = 1000;
    private final TOUConnectionManager connectionManager = new TOUConnectionManager();
    private DatagramSocket socket;
    private InetAddress address;
    private int port;

    public TOUSocket() throws IOException {
        this(new InetSocketAddress(0));
    }

    public TOUSocket(InetAddress address, int port) throws IOException {
        this(new InetSocketAddress(InetAddress.getLocalHost(), 0));
        connect(address, port);
    }

    public TOUSocket(SocketAddress socketAddress) throws SocketException {
        LOGGER.traceEntry(() -> TOULog4JUtils.toString(socketAddress));

        socket = new DatagramSocket(socketAddress);
        connectionManager.bind(socket);

        LOGGER.traceExit();
    }

    public boolean isBound() {
        LOGGER.traceEntry();
        return LOGGER.traceExit(socket != null);
    }

    public boolean isConnected() {
        LOGGER.traceEntry();
        return LOGGER.traceExit(address != null && port != 0);
    }

    public void bind(SocketAddress socketAddress) throws IOException {
        LOGGER.traceEntry();

        checkBound(false);
        socket = new DatagramSocket(socketAddress);
        connectionManager.bind(socket);

        LOGGER.traceExit();
    }


    public void connect (InetAddress address, int port) throws IOException {
        LOGGER.traceEntry("address: {} port: {}", address, port);

        this.address = address;
        this.port = port;
        connectionManager.sender(new TOUSender(socket, QUEUE_CAPACITY, TIMEOUT));
        connectionManager.receiver(new TOUReceiver(socket, MAX_PACKET_SIZE));
        try {
            connectionManager.connect(address, port);
        } catch (InterruptedException | TCPUnknownPacketTypeException e) {
            LOGGER.catching(e);
            throw LOGGER.throwing(new IOException(e));
        }

        LOGGER.traceExit();
    }

    public InputStream getInputStream () throws IOException {
        LOGGER.traceEntry();

        checkBound(true);
        checkConnected(true);

        TOUSocketInputStream is = new TOUSocketInputStream(
                connectionManager.receiver(), address, port);
        return LOGGER.traceExit(is);
    }


    public OutputStream getOutputStream () throws IOException {
        LOGGER.traceEntry();

        checkBound(true);
        checkConnected(true);

        return LOGGER.traceExit(new TOUSocketOutputStream(connectionManager.sender(),
                socket.getLocalAddress(), socket.getLocalPort(),
                address, port, MAX_DATA_SIZE));
    }

    public void close () throws IOException {
        LOGGER.traceEntry();
        LOGGER.traceExit();
    }

    private void checkBound(boolean shouldBeBound) throws IOException {
        LOGGER.traceEntry();

        if (isBound() != shouldBeBound) {
            throw LOGGER.throwing(new IOException("Socket is " + (isBound() ? "" : "not ") + "bound"));
        }

        LOGGER.traceExit();
    }

    private void checkConnected(boolean shouldBeConnected) throws IOException {
        LOGGER.traceEntry();

        if (isConnected() != shouldBeConnected) {
            throw LOGGER.throwing(new IOException("Socket is " + (isConnected() ? "" : "not ") + "connected"));
        }

        LOGGER.traceExit();
    }
}
