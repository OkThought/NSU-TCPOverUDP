package ru.nsu.ccfit.bogush.tou;

import ru.nsu.ccfit.bogush.tcp.TCPUnknownPacketTypeException;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;

class TOUImpl {
    private static final int PACKET_SIZE = 576; // bytes
    private static final int QUEUE_CAPACITY = 512;

    private final TOUConnectionManager connectionManager;
    private final TOUReceiver receiver;
    private DatagramSocket datagramSocket;
    private final TOUSender sender;

    private byte[] bytesToRead;
    private int readingPosition = -1;
    private short sequenceNumber = 0;

    private byte[] bytesToWrite;
    private int writingPosition = -1;

    TOUImpl(DatagramSocket socket) throws IOException {
        sender = new TOUSender(socket, QUEUE_CAPACITY);
        receiver = new TOUReceiver(socket, PACKET_SIZE);
        this.datagramSocket = socket;
        connectionManager = new TOUConnectionManager(socket, sender, receiver);
    }

    int readByte () throws InterruptedException {
        if (readingPosition < 0 || bytesToRead == null || readingPosition >= bytesToRead.length) {
            bytesToRead = receiver.takeDataBySequenceNumber(sequenceNumber++);
            readingPosition = 0;
        }
        return bytesToRead[readingPosition++];
    }

    void writeByte (int b) {

    }

    void closeSocket () {
        connectionManager.closeConnection();
    }

    TOUSocket accept(int port) throws IOException, InterruptedException, TCPUnknownPacketTypeException {
        return connectionManager.acceptConnection(port);
    }

    void connect (InetAddress address, int port) throws IOException, InterruptedException, TCPUnknownPacketTypeException {
        connectionManager.connectToServer((short) port, (short) port, address);
    }
}
