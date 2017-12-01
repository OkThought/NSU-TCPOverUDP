package ru.nsu.ccfit.bogush.tou;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.nsu.ccfit.bogush.tcp.TCPPacket;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.nio.ByteBuffer;

class TOUSocketOutputStream extends OutputStream implements TOUBufferHolder {
    static {
        TOULog4JUtils.initIfNotInitYet();
    }
    private static final Logger LOGGER = LogManager.getLogger(TOUSocketOutputStream.class.getSimpleName());

    private final TOUSender sender;
    private final InetAddress sourceAddress;
    private final int sourcePort;
    private final InetAddress destinationAddress;
    private final int destinationPort;
    private final ByteBuffer buffer;
    private short sequenceNumber = 0;

    public TOUSocketOutputStream(TOUSender sender,
                                 InetAddress sourceAddress, int sourcePort,
                                 InetAddress destinationAddress, int destinationPort,
                                 int bufferSize) {
        LOGGER.traceEntry("sender: {} source: {}:{} destination: {}:{} buffer size: {}",
                sender, sourceAddress, sourcePort, destinationAddress, destinationPort, bufferSize);

        if (bufferSize <= 0)
            throw LOGGER.throwing(new IllegalArgumentException("bufferSize <= 0"));
        if (sourceAddress == null)
            throw LOGGER.throwing(new IllegalArgumentException("source address is null"));
        if (destinationAddress == null)
            throw LOGGER.throwing(new IllegalArgumentException("destination address is null"));

        this.sender = sender;
        this.sourceAddress = sourceAddress;
        this.sourcePort = sourcePort;
        this.destinationAddress = destinationAddress;
        this.destinationPort = destinationPort;
        this.buffer = ByteBuffer.allocate(bufferSize);
        sender.addBufferHolder(this);

        LOGGER.traceExit();
    }

    @Override
    public void write(int b) throws IOException {
        LOGGER.traceEntry("byte: {}", (byte) b);
        try {
            synchronized (buffer) {
                buffer.put((byte) b);
                if (buffer.remaining() == 0) {
                    sender.putInQueue(wrap(buffer.array().clone()));
                    incrementSequenceNumber();
                    buffer.position(0);
                }
            }
        } catch (InterruptedException e) {
            LOGGER.catching(e);
            throw LOGGER.throwing(new IOException(e));
        }
        LOGGER.traceExit();
    }

    private TOUPacket wrap(byte[] data) {
        TCPPacket tcpPacket = new TCPPacket(data.length);
        tcpPacket.sequenceNumber(sequenceNumber);
        tcpPacket.data(data);
        tcpPacket.sourcePort(sourcePort);
        tcpPacket.destinationPort(destinationPort);
        return new TOUPacket(tcpPacket, sourceAddress, destinationAddress);
    }

    private void incrementSequenceNumber() {
        LOGGER.trace("Increment sequenceNumber: {}->{}", sequenceNumber, (short) (sequenceNumber + 1));
        ++sequenceNumber;
    }

    @Override
    public int available() {
        synchronized (buffer) {
            return buffer.position();
        }
    }

    @Override
    public TOUPacket flushIntoPacket() {
        LOGGER.traceEntry();

        byte[] data;
        synchronized (buffer) {
            int size = buffer.position();
            buffer.position(0);
            data = new byte[size];
            System.arraycopy(buffer.array(), 0, data, 0, size);
        }
        TOUPacket touPacket = wrap(data);
        incrementSequenceNumber();

        return LOGGER.traceExit(touPacket);
    }
}
