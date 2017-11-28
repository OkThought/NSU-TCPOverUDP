package ru.nsu.ccfit.bogush.tou;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.nsu.ccfit.bogush.tcp.TCPUnknownPacketTypeException;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

import static ru.nsu.ccfit.bogush.tou.TOUSocket.MAX_PACKET_SIZE;
import static ru.nsu.ccfit.bogush.tou.TOUSocket.QUEUE_CAPACITY;
import static ru.nsu.ccfit.bogush.tou.TOUSocket.TIMEOUT;

public class TOUServerSocket {
    static {
        TOULog4JUtils.initIfNotInitYet();
    }
    private static final Logger logger = LogManager.getLogger("ServerSocket");

    private final TOUConnectionManager connectionManager = new TOUConnectionManager();
    private DatagramSocket socket;

    public TOUServerSocket() throws IOException {
        this(0);
    }

    public TOUServerSocket(int port) throws IOException {
        logger.traceEntry();

        bind(new InetSocketAddress(InetAddress.getLocalHost(), port));

        logger.traceExit();
    }

    public boolean isBound() {
        logger.traceEntry();
        return logger.traceExit(socket != null);
    }

    public void bind(SocketAddress socketAddress) throws IOException {
        logger.traceEntry(() -> TOULog4JUtils.toString(socketAddress));

        checkBound(false);
        socket = new DatagramSocket(socketAddress);
        connectionManager.bind(socket);
        connectionManager.sender(new TOUSender(socket, QUEUE_CAPACITY, TIMEOUT));
        connectionManager.receiver(new TOUReceiver(socket, MAX_PACKET_SIZE));
        connectionManager.listen();

        logger.traceExit();
    }

    public TOUSocket accept() throws IOException {
        logger.traceEntry();

        checkBound(true);
        try {
            return logger.traceExit(connectionManager.accept());
        } catch (TCPUnknownPacketTypeException | InterruptedException e) {
            logger.catching(e);
            throw logger.throwing(new IOException(e));
        }
    }

    private void checkBound(boolean shouldBeBound) throws IOException {
        logger.traceEntry(String.valueOf(shouldBeBound));

        if (isBound() != shouldBeBound) {
            throw logger.throwing(new IOException("Socket is " + (isBound() ? "" : "not ") + "bound"));
        }

        logger.traceExit();
    }
}
