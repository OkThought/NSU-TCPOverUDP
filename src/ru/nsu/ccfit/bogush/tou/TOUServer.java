package ru.nsu.ccfit.bogush.tou;

import java.io.IOException;
import java.net.DatagramSocket;

class TOUServer extends TOUAbstractImpl {
    private TOUConnectionManager connectionManager;

    TOUServer (int port) throws IOException {
        DatagramSocket socket = new DatagramSocket(port);
        TOUSender sender = new TOUSender(socket, QUEUE_CAPACITY);
        TOUReceiver receiver = new TOUReceiver(socket, PACKET_SIZE);
        connectionManager = new TOUConnectionManager(sender, receiver);
    }

    TOUSocket accept () throws IOException, InterruptedException {
        return connectionManager.acceptConnection();
    }


    @Override
    int readByte () {
        return 0;
    }

    @Override
    void writeByte (int b) {

    }
}
