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

    private final TOUReceiver receiver;
    private final InetAddress sourceAddress;
    private final int sourcePort;
    private ByteBuffer buffer;
    private short sequenceNumber = 0;

    TOUSocketInputStream(TOUReceiver receiver, InetAddress sourceAddress, int sourcePort) {
        LOGGER.traceEntry("receiver: {} sender address: {}:{}", ()->receiver, ()-> sourceAddress, ()-> sourcePort);

        this.receiver = receiver;
        this.sourceAddress = sourceAddress;
        this.sourcePort = sourcePort;

        LOGGER.traceExit();
    }

    @Override
    public int read () throws IOException {
        LOGGER.traceEntry();

        // TODO: 12/1/17 return -1 on EOF
        try {
            if (buffer == null) {
                buffer = ByteBuffer.wrap(receiver.takeData(sourceAddress, sourcePort, sequenceNumber++));
            }
            return LOGGER.traceExit(buffer.get());
        } catch (InterruptedException e) {
            LOGGER.catching(e);
            throw LOGGER.throwing(new IOException(e));
        }
    }
}
