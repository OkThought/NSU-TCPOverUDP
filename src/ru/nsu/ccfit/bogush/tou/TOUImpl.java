package ru.nsu.ccfit.bogush.tou;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;

class TOUImpl {
    static final int PACKET_SIZE = 576; // bytes
    static final int QUEUE_CAPACITY = 512;

    private final TOUConnectionManager connectionManager;
    private final TOUReceiver receiver;
    private DatagramSocket datagramSocket;
    private final TOUSender sender;

    private byte[] bytesToRead;
    private int readingPosition = -1;

    private byte[] bytesToWrite;
    private int writingPosition = -1;

    public TOUImpl (DatagramSocket socket) throws IOException {
        sender = new TOUSender(socket, QUEUE_CAPACITY);
        receiver = new TOUReceiver(socket, PACKET_SIZE);
        this.datagramSocket = socket;
        connectionManager = new TOUConnectionManager(sender, receiver);
    }

    int readByte () throws InterruptedException {
        if (readingPosition < 0 || bytesToRead == null || readingPosition >= bytesToRead.length) {
            TOUPacket p;
            p = receiver.takeSubsequentOrdinaryPacket();
            bytesToRead = p.getTcpPacket().getBytes();
            readingPosition = 0;
        }
        return bytesToRead[readingPosition++];
    }

    void writeByte (int b) {

    }

    void closeSocket () {
        connectionManager.closeConnection();
    }

    TOUSocket accept () throws IOException, InterruptedException {
        return connectionManager.acceptConnection();
    }

    void connect (InetAddress address, int port) throws IOException, InterruptedException {
        connectionManager.connectToServer((short) port, (short) port, address);
    }

    public void listen () {
        connectionManager.listenToIncomingConnections();
    }
}
