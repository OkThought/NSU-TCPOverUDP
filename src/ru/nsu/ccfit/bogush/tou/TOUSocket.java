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
    private static final Logger logger = LogManager.getLogger("Socket");

    static final int MAX_DATA_SIZE = 1024; // bytes
    static final int MAX_PACKET_SIZE = MAX_DATA_SIZE + TCPPacket.HEADER_SIZE;
    static final int QUEUE_CAPACITY = 512;
    static final int TIMEOUT = 10;
    private final TOUConnectionManager connectionManager = new TOUConnectionManager();
    private DatagramSocket socket;

    public TOUSocket() throws IOException {
        this(new InetSocketAddress(0));
    }

    public TOUSocket(InetAddress address, int port) throws IOException {
        this(new InetSocketAddress(InetAddress.getLocalHost(), 0));
        connect(address, port);
    }

    public TOUSocket(SocketAddress socketAddress) throws SocketException {
        logger.traceEntry(() -> TOULog4JUtils.toString(socketAddress));

        socket = new DatagramSocket(socketAddress);
        connectionManager.bind(socket);

        logger.traceExit();
    }

    public boolean isBound() {
        logger.traceEntry();
        return logger.traceExit(socket != null);
    }

    public void bind(SocketAddress socketAddress) throws IOException {
        logger.traceEntry();

        checkBound(true);
        socket = new DatagramSocket(socketAddress);
        connectionManager.bind(socket);

        logger.traceExit();
    }


    public void connect (InetAddress address, int port) throws IOException {
        logger.traceEntry("address: {} port: {}", address, port);

        checkBound(false);
        connectionManager.sender(new TOUSender(socket, QUEUE_CAPACITY, TIMEOUT));
        connectionManager.receiver(new TOUReceiver(socket, MAX_PACKET_SIZE));
        try {
            connectionManager.connect(address, port);
        } catch (InterruptedException | TCPUnknownPacketTypeException e) {
            logger.catching(e);
            throw logger.throwing(new IOException(e));
        }

        logger.traceExit();
    }

    public InputStream getInputStream () throws IOException {
        logger.traceEntry();
        TOUSocketInputStream is = new TOUSocketInputStream(
                connectionManager.receiver(), socket.getInetAddress(), socket.getPort());
        return logger.traceExit(is);
    }


    public OutputStream getOutputStream () throws IOException {
        logger.traceEntry();
        return logger.traceExit(new TOUSocketOutputStream(connectionManager.sender(),
                socket.getLocalAddress(), socket.getLocalPort(),
                socket.getInetAddress(), socket.getPort(), MAX_DATA_SIZE));
    }

    public void close () throws IOException {
        logger.traceEntry();
        logger.traceExit();
    }

    private void checkBound(boolean bound) throws IOException {
        logger.traceEntry();
        if (isBound() == bound) {
            throw logger.throwing(new IOException("Socket is " + (isBound() ? "" : "not ") + "bound"));
        }
        logger.traceExit();
    }
}
