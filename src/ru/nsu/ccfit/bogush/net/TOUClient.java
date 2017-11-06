package ru.nsu.ccfit.bogush.net;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * ToU - TCP over UDP
 */
class TOUClient extends TOUAbstractImpl {
    private final TOUConnectionManager connectionManager;

    TOUClient(InetAddress address, int port) throws IOException, InterruptedException {
        DatagramSocket socket = new DatagramSocket(port, address);
        TOUSender sender = new TOUSender(socket, QUEUE_CAPACITY);
        TOUReceiver receiver = new TOUReceiver(socket, PACKET_SIZE);
        connectionManager = new TOUConnectionManager(sender, receiver);
        connectionManager.connectToServer((short) port, (short) port, address);
    }

    @Override
    int readByte() {
        return 0;
    }

    @Override
    void writeByte(int b) {

    }

    void closeSocket() {
        connectionManager.closeConnection();
    }
}
