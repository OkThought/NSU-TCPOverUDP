package ru.nsu.ccfit.bogush.tou;

import ru.nsu.ccfit.bogush.tcp.TCPUnknownPacketTypeException;

import java.io.IOException;
import java.net.DatagramSocket;

public class TOUServerSocket {
    private final TOUImpl impl;
    private int port;

    public TOUServerSocket (int port) throws IOException {
        this.port = port;
        DatagramSocket socket = new DatagramSocket(port);
        impl = new TOUImpl(socket);
        impl.bind(port);
    }

    public TOUSocket accept () throws IOException, InterruptedException {
        try {
            return impl.accept();
        } catch (TCPUnknownPacketTypeException e) {
            throw new IOException(e);
        }
    }
}
