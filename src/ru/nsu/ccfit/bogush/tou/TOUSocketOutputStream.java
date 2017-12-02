package ru.nsu.ccfit.bogush.tou;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.nsu.ccfit.bogush.tcp.TCPPacket;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.nio.ByteBuffer;

class TOUSocketOutputStream extends OutputStream {
    static {
        TOULog4JUtils.initIfNotInitYet();
    }
    private static final Logger LOGGER = LogManager.getLogger(TOUSocketOutputStream.class.getSimpleName());

    private final TOUSender sender;
    private final InetAddress localAddress;
    private final int localPort;
    private final InetAddress address;
    private final int port;
    private final ByteBuffer buffer;
    private short sequenceNumber = 0;

    public TOUSocketOutputStream(TOUSocketImpl impl) {
        LOGGER.traceEntry("impl: {}", ()->impl);

        this.sender = impl.sender;
        this.localAddress = impl.localAddress();
        this.localPort = impl.localPort();
        this.address = impl.address();
        this.port = impl.port();
        this.buffer = ByteBuffer.allocate(TOUSocketImpl.MAX_DATA_SIZE);

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
        tcpPacket.sourcePort(localPort);
        tcpPacket.destinationPort(port);
        return new TOUPacket(tcpPacket, localAddress, address);
    }

    private void incrementSequenceNumber() {
        LOGGER.trace("Increment sequenceNumber: {}->{}", sequenceNumber, (short) (sequenceNumber + 1));
        ++sequenceNumber;
    }

    int available() {
        synchronized (buffer) {
            return buffer.position();
        }
    }

    TOUPacket flushIntoPacket() {
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
