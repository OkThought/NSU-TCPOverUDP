package ru.nsu.ccfit.bogush.tou;

import java.io.IOException;
import java.net.DatagramSocket;

public class TOUServerSocket {
    private final TOUImpl impl;

    public TOUServerSocket (int port) throws IOException, InterruptedException {
        DatagramSocket socket = new DatagramSocket(port);
        impl = new TOUImpl(socket);
        impl.listen();
    }

    public TOUSocket accept () throws IOException, InterruptedException {
        return impl.accept();
    }
}
