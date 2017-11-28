package ru.nsu.ccfit.bogush.tou;

import ru.nsu.ccfit.bogush.tcp.TCPPacket;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.nio.ByteBuffer;

class TOUSocketOutputStream extends OutputStream implements TOUBufferHolder {
    private final TOUSender sender;
    private final InetAddress sourceAddress;
    private final int sourcePort;
    private final InetAddress destinationAddress;
    private final int destinationPort;
    private final ByteBuffer buffer;

    public TOUSocketOutputStream(TOUSender sender,
                                 InetAddress sourceAddress, int sourcePort,
                                 InetAddress destinationAddress, int destinationPort,
                                 int bufferSize) {
        if (bufferSize <= 0) throw new IllegalArgumentException("bufferSize <= 0");
        if (sourceAddress == null) throw new IllegalArgumentException("source address is null");
        if (destinationAddress == null) throw new IllegalArgumentException("destination address is null");
        this.sender = sender;
        this.sourceAddress = sourceAddress;
        this.sourcePort = sourcePort;
        this.destinationAddress = destinationAddress;
        this.destinationPort = destinationPort;
        this.buffer = ByteBuffer.allocate(bufferSize);
        sender.addBufferHolder(this);
    }

    @Override
    public void write(int b) throws IOException {
        try {
            synchronized (buffer) {
                buffer.put((byte) b);
                if (buffer.remaining() == 0) {
                    sender.putInQueue(wrap(buffer.array().clone()));
                    buffer.position(0);
                }
            }
        } catch (InterruptedException e) {
            throw new IOException(e);
        }
    }

    private TOUPacket wrap(byte[] data) {
        TCPPacket tcpPacket = new TCPPacket(data.length);
        tcpPacket.data(data);
        tcpPacket.sourcePort(sourcePort);
        tcpPacket.destinationPort(destinationPort);
        return new TOUPacket(tcpPacket, sourceAddress, destinationAddress);
    }

    @Override
    public int available() {
        synchronized (buffer) {
            return buffer.position() + 1;
        }
    }

    @Override
    public TOUPacket flushIntoPacket() {
        byte[] data;
        synchronized (buffer) {
            int size = buffer.position() + 1;
            data = new byte[size];
            System.arraycopy(buffer.array(), 0, data, 0, size);
        }
        return wrap(data);
    }
}
