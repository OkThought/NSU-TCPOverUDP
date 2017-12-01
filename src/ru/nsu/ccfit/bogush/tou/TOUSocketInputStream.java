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
    private final InetAddress senderAddress;
    private final int senderPort;
    private ByteBuffer buffer;
    private short sequenceNumber = 0;

    TOUSocketInputStream(TOUReceiver receiver, InetAddress senderAddress, int senderPort) {
        LOGGER.traceEntry("receiver: {} sender address: {}:{}", ()->receiver, ()->senderAddress, ()->senderPort);

        this.receiver = receiver;
        this.senderAddress = senderAddress;
        this.senderPort = senderPort;

        LOGGER.traceExit();
    }

    @Override
    public int read () throws IOException {
        LOGGER.traceEntry();
        try {
            if (buffer == null) {
                buffer = ByteBuffer.wrap(receiver.takeData(senderAddress, senderPort, sequenceNumber++));
            }
            return LOGGER.traceExit(buffer.get());
        } catch (InterruptedException e) {
            LOGGER.catching(e);
            throw LOGGER.throwing(new IOException(e));
        }
    }
}
