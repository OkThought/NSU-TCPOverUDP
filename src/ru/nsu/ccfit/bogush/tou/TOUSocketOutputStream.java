package ru.nsu.ccfit.bogush.tou;

import ru.nsu.ccfit.bogush.tcp.TCPPacket;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.nio.ByteBuffer;

class TOUSocketOutputStream extends OutputStream {
    private final TOUSender sender;
    private final InetAddress sourceAddress;
    private final int sourcePort;
    private final InetAddress destinationAddress;
    private final int destinationPort;
    private ByteBuffer buffer;

    public TOUSocketOutputStream(TOUSender sender,
                                 InetAddress sourceAddress, int sourcePort,
                                 InetAddress destinationAddress, int destinationPort,
                                 int bufferSize) {
        if (bufferSize <= 0) throw new IllegalArgumentException("bufferSize <= 0");
        this.sender = sender;
        this.sourceAddress = sourceAddress;
        this.sourcePort = sourcePort;
        this.destinationAddress = destinationAddress;
        this.destinationPort = destinationPort;
        this.buffer = ByteBuffer.allocate(bufferSize);
    }

    @Override
    public void write(int b) throws IOException {
        try {
            buffer.put((byte) b);
            if (buffer.remaining() == 0) {
                TCPPacket tcpPacket = new TCPPacket();
                tcpPacket.data(buffer.array());
                tcpPacket.sourcePort(sourcePort);
                tcpPacket.destinationPort(destinationPort);
                sender.putInQueue(new TOUPacket(tcpPacket, sourceAddress, destinationAddress));
                buffer.reset();
            }
        } catch (InterruptedException e) {
            throw new IOException(e);
        }
    }
}
