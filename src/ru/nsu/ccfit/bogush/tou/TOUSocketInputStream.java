package ru.nsu.ccfit.bogush.tou;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.nio.ByteBuffer;

class TOUSocketInputStream extends InputStream {
    private final TOUReceiver receiver;
    private final InetAddress senderAddress;
    private final int senderPort;
    private ByteBuffer buffer;
    private short sequenceNumber = 0;

    TOUSocketInputStream(TOUReceiver receiver, InetAddress senderAddress, int senderPort) {
        this.receiver = receiver;
        this.senderAddress = senderAddress;
        this.senderPort = senderPort;
    }

    @Override
    public int read () throws IOException {
        try {
            if (buffer == null) {
                buffer = ByteBuffer.wrap(receiver.takeData(senderAddress, senderPort, sequenceNumber++));
            }
            return buffer.get();
        } catch (InterruptedException e) {
            throw new IOException(e);
        }
    }
}
