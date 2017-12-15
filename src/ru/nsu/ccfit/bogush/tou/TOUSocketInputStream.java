package ru.nsu.ccfit.bogush.tou;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

class TOUSocketInputStream extends InputStream {
    static { TOULog4JUtils.initIfNotInitYet(); }
    private static final Logger LOGGER = LogManager.getLogger("TOUSocketInputStream");

    private final TOUSocketImpl impl;
    private ByteBuffer buffer;
    private short sequenceNumber = 0;
    private boolean eof = false;
    final Object lock = new Object();

    TOUSocketInputStream(TOUSocketImpl impl) {
        LOGGER.traceEntry("impl: {}", ()->impl);

        this.impl = impl;
        this.sequenceNumber = impl.initialSequenceNumber;

        LOGGER.traceExit();
    }

    @Override
    public int read () throws IOException {
        LOGGER.traceEntry();

        if (eof) {
            return LOGGER.traceExit(-1);
        }

        try {
            if (buffer == null || !buffer.hasRemaining()) {
                if (closing || impl.isClosedOrPending()) {
                    throw LOGGER.throwing(new IOException("Stream closed"));
                }
                short seq = sequenceNumber;
                byte[] data = impl.nextDataSegment(seq);
                if (data == null) {
                    eof = true;
                    return LOGGER.traceExit(-1);
                }
                buffer = ByteBuffer.wrap(data);
                incrementSequenceNumber();
            }
        } catch (InterruptedException e) {
            LOGGER.catching(e);
            e.printStackTrace();
//            throw LOGGER.throwing(new IOException(e));
        }

        if (buffer.hasRemaining()) {
            return LOGGER.traceExit(buffer.get());
        }

        /*
         * If we get here we are at EOF, the socket has been closed,
         * or the connection has been reset.
         */

        eof = true;

        return LOGGER.traceExit(-1);
    }

    private void incrementSequenceNumber() {
        LOGGER.trace("Increment sequenceNumber: {}->{}", sequenceNumber, (short) (sequenceNumber + 1));
        ++sequenceNumber;
    }

    private boolean closing = false;
    @Override
    public void close() throws IOException {
        if (closing) return;

        closing = true;

        impl.close();
    }
}
