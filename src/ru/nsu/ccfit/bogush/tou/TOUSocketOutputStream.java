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
    private short sequenceNumber;

    public TOUSocketOutputStream(TOUSocketImpl impl) {
        LOGGER.traceEntry("impl: {}", ()->impl);

        this.impl = impl;
        this.sequenceNumber = impl.initialSequenceNumber;
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
            if (!buffer.hasRemaining()) {
                synchronized (buffer) {
                    while (!buffer.hasRemaining()) {
                        buffer.wait();
                    }
                }
            }
            buffer.put((byte) b);
        } catch (InterruptedException e) {
            LOGGER.catching(e);
            throw LOGGER.throwing(new IOException(e));
        }
        LOGGER.traceExit();
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

        TOUSegment segment;
        synchronized (buffer) {
            if (buffer.position() == 0) return null;
            int size = buffer.position();
            buffer.position(0);
            byte[] data = new byte[size];
            System.arraycopy(buffer.array(), 0, data, 0, size);
            segment = impl.factory.createTOUSegment(data, sequenceNumber);
            impl.mergeWithAckIfPending(segment);
            incrementSequenceNumber();
            buffer.notify();
        }

        return LOGGER.traceExit(segment);
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
