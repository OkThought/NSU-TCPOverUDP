package ru.nsu.ccfit.bogush.tou;

import java.io.IOException;

public class TOUServerSocket {
    private final TOUServer server;

    public TOUServerSocket (int port) throws IOException {
        server = new TOUServer(port);

    }

    public TOUSocket accept () throws IOException, InterruptedException {
        return server.accept();
    }
}
