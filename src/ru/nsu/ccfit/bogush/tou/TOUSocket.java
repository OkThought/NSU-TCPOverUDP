package ru.nsu.ccfit.bogush.tou;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;

public class TOUSocket {
    private TOUClient client;

    public TOUSocket (InetAddress address, int port) throws IOException, InterruptedException {
        client = new TOUClient(address, port);
    }

    public InputStream getInputStream () throws IOException {
        return new TOUSocketInputStream(client);
    }


    public OutputStream getOutputStream () throws IOException {
        return new TOUSocketOutputStream(client);
    }

    public void close () throws IOException {
        client.closeSocket();
    }
}
