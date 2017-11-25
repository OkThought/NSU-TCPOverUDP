package ru.nsu.ccfit.bogush.tou;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class TOUSocket {
    private TOUImpl impl;

    public TOUSocket (InetAddress address, int port) throws IOException, InterruptedException {
        DatagramSocket socket = new DatagramSocket(port, address);
        impl = new TOUImpl(socket);
        impl.connect(address, port);
    }

    public InputStream getInputStream () throws IOException {
        return new TOUSocketInputStream(impl);
    }


    public OutputStream getOutputStream () throws IOException {
        return new TOUSocketOutputStream(impl);
    }

    public void close () throws IOException {
        impl.closeSocket();
    }
}
