package ru.nsu.ccfit.bogush.tou;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.nio.ByteBuffer;

class TOUSocketInputStream extends InputStream {
    static {
        TOULog4JUtils.initIfNotInitYet();
    }
    private static final Logger LOGGER = LogManager.getLogger("TOUSocketInputStream");

    private final TOUSocketImpl impl;
    private final TOUReceiver receiver;
    private final InetAddress sourceAddress;
    private final int sourcePort;
    private ByteBuffer buffer;
    private short sequenceNumber = 0;
    private boolean eof = false;

    TOUSocketInputStream(TOUSocketImpl impl) {
        LOGGER.traceEntry("impl: {}", ()->impl);

        this.impl = impl;
        this.receiver = impl.receiver;
        this.sourceAddress = impl.localAddress();
        this.sourcePort = impl.localPort();

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
                buffer = ByteBuffer.wrap(receiver.takeData(sourceAddress, sourcePort, sequenceNumber++));
            }
        } catch (InterruptedException e) {
            LOGGER.catching(e);
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

        return -1;
    }


}
