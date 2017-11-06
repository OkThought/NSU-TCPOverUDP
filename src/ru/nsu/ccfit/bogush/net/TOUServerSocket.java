package ru.nsu.ccfit.bogush.net;

import java.net.SocketException;

public class TOUServerSocket {
    private final TOUServer server;

    public TOUServerSocket(int port) throws SocketException {
        server = new TOUServer(port);

    }

    public TOUSocket accept() {
        return server.accept();
    }
}
