package ru.nsu.ccfit.bogush.tou;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * ToU - TCP over UDP
 */
class TOUClient extends TOUAbstractImpl {
    private final TOUConnectionManager connectionManager;
    private final TOUReceiver receiver;
    private final TOUSender sender;

    private byte[] bytesToRead;
    private int readingPosition = -1;

    private byte[] bytesToWrite;
    private int writingPosition = -1;

    TOUClient (InetAddress address, int port) throws IOException, InterruptedException {
        DatagramSocket socket = new DatagramSocket(port, address);
        sender = new TOUSender(socket, QUEUE_CAPACITY);
        receiver = new TOUReceiver(socket, PACKET_SIZE);
        connectionManager = new TOUConnectionManager(sender, receiver);
        connectionManager.connectToServer((short) port, (short) port, address);
    }

    @Override
    int readByte () throws InterruptedException {
        if (readingPosition < 0 || bytesToRead == null || readingPosition >= bytesToRead.length) {
            TOUPacket p;
            p = receiver.takeSubsequentOrdinaryPacket();
            bytesToRead = p.getTcpPacket().getBytes();
            readingPosition = 0;
        }
        return bytesToRead[readingPosition++];
    }

    @Override
    void writeByte (int b) {

    }

    void closeSocket () {
        connectionManager.closeConnection();
    }
}
