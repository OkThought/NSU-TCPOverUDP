package ru.nsu.ccfit.bogush.tou;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
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

        LOGGER.traceExit();
    }

    @Override
    public int read () throws IOException {
        LOGGER.traceEntry();

        if (eof) {
            return -1;
        }

        try {
            if (buffer == null) {
                short seq = sequenceNumber;
                buffer = ByteBuffer.wrap(impl.nextData(seq));
                incrementSequenceNumber();
            }
        } catch (InterruptedException e) {
            LOGGER.catching(e);
//            throw LOGGER.throwing(new IOException(e));
        }

        if (buffer.hasRemaining()) {
            return LOGGER.traceExit(buffer.get());
        }

        if (closing) {
            throw LOGGER.throwing(new IOException("Stream closed"));
        }

        /*
         * If we get here we are at EOF, the socket has been closed,
         * or the connection has been reset.
         */

        eof = true;

        return -1;
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
