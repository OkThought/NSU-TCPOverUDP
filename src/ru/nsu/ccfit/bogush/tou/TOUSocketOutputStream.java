package ru.nsu.ccfit.bogush.tou;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.nsu.ccfit.bogush.tcp.TCPSegment;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import static ru.nsu.ccfit.bogush.tou.TOUConstants.MAX_DATA_SIZE;

class TOUSocketOutputStream extends OutputStream {
    static { TOULog4JUtils.initIfNotInitYet(); }
    private static final Logger LOGGER = LogManager.getLogger(TOUSocketOutputStream.class.getSimpleName());

    private final TOUSocketImpl impl;
    private final ByteBuffer buffer;
    private short sequenceNumber = 0;

    public TOUSocketOutputStream(TOUSocketImpl impl) {
        LOGGER.traceEntry("impl: {}", ()->impl);

        this.impl = impl;
        this.buffer = ByteBuffer.allocate(MAX_DATA_SIZE);

        LOGGER.traceExit();
    }

    @Override
    public void write(byte[] b) throws IOException {
        if (closing || impl.isClosedOrPending()) {
            throw LOGGER.throwing(new IOException("Stream closed"));
        }
        super.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if (closing || impl.isClosedOrPending()) {
            throw LOGGER.throwing(new IOException("Stream closed"));
        }
        super.write(b, off, len);
    }

    @Override
    public void write(int b) throws IOException {
        LOGGER.traceEntry("byte: {}", (byte) b);

        if (closing || impl.isClosedOrPending()) {
            throw LOGGER.throwing(new IOException("Stream closed"));
        }

        try {
            synchronized (buffer) {
                buffer.put((byte) b);
                if (buffer.remaining() == 0) {
                    impl.segmentQueue().put(wrap(buffer.array().clone()));
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

    private TOUSegment wrap(byte[] data) {
        TCPSegment tcpSegment = new TCPSegment(data.length);
        tcpSegment.sequenceNumber(sequenceNumber);
        tcpSegment.data(data);
        tcpSegment.sourcePort(impl.localPort());
        tcpSegment.destinationPort(impl.port());
        return new TOUSegment(tcpSegment, impl.localAddress(), impl.address());
    }

    private void incrementSequenceNumber() {
        LOGGER.trace("Increment sequenceNumber: {}->{}", sequenceNumber, (short) (sequenceNumber + 1));
        ++sequenceNumber;
    }

    int available() {
        return buffer.position();
    }

    TOUSegment flushIntoSegment() {
        LOGGER.traceEntry();

        byte[] data;
        synchronized (buffer) {
            int size = buffer.position();
            buffer.position(0);
            data = new byte[size];
            System.arraycopy(buffer.array(), 0, data, 0, size);
        }

        synchronized (this) {
            this.notifyAll();
        }

        TOUSegment touSegment = wrap(data);
        incrementSequenceNumber();

        return LOGGER.traceExit(touSegment);
    }

    @Override
    public void flush() throws IOException {
        LOGGER.traceEntry();
//        if (impl.isClosedOrPending()) return;

        if (available() > 0) synchronized (this) {
            while (available() > 0) {
                try {
                    this.wait();
                } catch (InterruptedException e) {
                    LOGGER.catching(e);
                    break;
                }
            }
        }

        LOGGER.traceExit();
    }

    private boolean closing = false;
    @Override
    public void close() throws IOException {
        if (closing) return;

        closing = true;

        flush();
        impl.close();
    }
}
