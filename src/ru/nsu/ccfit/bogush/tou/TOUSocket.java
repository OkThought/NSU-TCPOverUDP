package ru.nsu.ccfit.bogush.tou;

import ru.nsu.ccfit.bogush.tcp.TCPUnknownPacketTypeException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class TOUSocket {
    private TOUImpl impl;

    public TOUSocket (InetAddress address, int port) throws IOException {
        DatagramSocket socket = new DatagramSocket();
        impl = new TOUImpl(socket);
        try {
            impl.connect(address, port);
        } catch (InterruptedException | TCPUnknownPacketTypeException e) {
            throw new IOException(e);
        }
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
