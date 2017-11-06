package ru.nsu.ccfit.bogush.net;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * ToU - TCP over UDP
 */
class TOUClient extends TOUAbstractImpl {
    TOUClient(InetAddress address, int port) throws IOException, InterruptedException {
        DatagramSocket socket = new DatagramSocket(port, address);
        TOUSender sender = new TOUSender(socket, QUEUE_CAPACITY);
        TOUReceiver receiver = new TOUReceiver(socket, PACKET_SIZE);
        new TOUConnectionManager(sender, receiver).connectToServer((short) port, (short) port, address);
    }

    @Override
    int readByte() {
        return 0;
    }

    @Override
    void writeByte(int b) {

    }

    void closeSocket() {
        /*
        * TODO: wait until
        * - all packets in sender are sent
        * - all received packets are taken from receiver
        * close the udp socket
        * release all resources
        */
    }
}
