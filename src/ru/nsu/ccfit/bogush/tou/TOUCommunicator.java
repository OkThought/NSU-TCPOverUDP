package ru.nsu.ccfit.bogush.tou;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.nsu.ccfit.bogush.tcp.TCPSegmentType;
import ru.nsu.ccfit.bogush.tcp.TCPUnknownSegmentTypeException;

import java.io.IOException;
import java.net.*;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import static ru.nsu.ccfit.bogush.tcp.TCPSegmentType.*;
import static ru.nsu.ccfit.bogush.tou.TOUConstants.*;

class TOUCommunicator {
    static { TOULog4JUtils.initIfNotInitYet(); }
    private static final Logger LOGGER = LogManager.getLogger("TOUCommunicator");

    private final DatagramSocket udpSocket;
    private final DatagramPacket udpPacket;
    private final ArrayBlockingQueue<TOUSegment> segments;
    private final Sender sender;
    private final Receiver receiver;
    private final WeakHashMap<InetSocketAddress, TOUSocketImpl> implMap;

    TOUCommunicator(WeakHashMap<InetSocketAddress, TOUSocketImpl> implMap, DatagramSocket udpSocket) throws IOException {
        LOGGER.traceEntry();

        this.implMap = implMap;
        this.udpSocket = udpSocket;
        this.udpPacket = new DatagramPacket(new byte[MAX_PACKET_SIZE], MAX_PACKET_SIZE);
        this.segments = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
        this.sender = new Sender();
        this.receiver = new Receiver();

        LOGGER.traceExit();
    }

    private void processSegment(TOUSegment segment)
            throws TCPUnknownSegmentTypeException {
        LOGGER.traceEntry("{}", segment);

        InetSocketAddress local = new InetSocketAddress(segment.destinationAddress, segment.destinationPort());
        InetSocketAddress remote = new InetSocketAddress(segment.sourceAddress, segment.sourcePort());
        TOUSocketImpl serverImpl = implMap.get(local);
        TOUSocketImpl associatedImpl = implMap.get(remote);

        TCPSegmentType type = segment.type();
        int dataSize = segment.tcpSegment.dataSize();

        // check the presence of associated impls
        if (type == ACK) {
            if (associatedImpl == null && serverImpl == null) {
                LOGGER.warn("no associated impl with address: {}", local);
                LOGGER.warn("no associated impl with address: {}", remote);
                return;
            }

            TOUSystemMessage ack = new TOUSystemMessage(segment, type);

            removeByACK(ack);

            if (associatedImpl != null) {
                associatedImpl.setSystemMessage(ack);
            }

            if (serverImpl != null) {
                serverImpl.setSystemMessage(ack);
            }

            if (dataSize > 0) {
                if (associatedImpl == null) {
                    LOGGER.warn("no associated impl with address: {}", remote);
                } else {
                    LOGGER.trace("segment with data");
                    associatedImpl.processSegment(segment);
                }
            }

            return;
        }

        if (type == FIN || type == FINACK || type == SYNACK) {
            if (associatedImpl == null) {
                LOGGER.warn("no associated impl with address: {}", remote);
                return;
            }
        } else if (type == SYN) {
            if (serverImpl == null) {
                LOGGER.warn("no associated impl with address: {}", local);
                return;
            }
        }

        TOUSystemMessage systemMessage = new TOUSystemMessage(segment, type);

        if (type == SYN) {
            serverImpl.setSystemMessage(new TOUSystemMessage(segment, type));
        } else if (type == FIN) {
            associatedImpl.processFIN(systemMessage);
        } else if (type == FINACK || type == SYNACK) {
            associatedImpl.setSystemMessage(systemMessage);
        } else {
            throw new TCPUnknownSegmentTypeException();
        }
    }

    void send(TOUSegment segment) throws IOException, InterruptedException {
        LOGGER.traceEntry("segment: {}", () -> segment);

        LOGGER.debug("send segment: {}", segment);
        DatagramPacket packet = TOUFactory.packIntoUDP(segment);
        send(packet);
        if (segment.needsResending()) {
            synchronized (segments) {
                segments.put(segment);
                segments.notifyAll();
            }
        }

        LOGGER.traceExit();
    }

    private void removeByACK(TOUSystemMessage ack) {
        segments.removeIf(s ->
                Objects.equals(s.destinationAddress, ack.destinationAddress) &&
                Objects.equals(s.sourceAddress, ack.sourceAddress) &&
                s.destinationPort() == ack.destinationPort() &&
                s.sourcePort() == ack.sourcePort() &&
                s.sequenceNumber() == ack.ackNumber());
    }

    void removeByReference(Object o) {
        segments.removeIf(s -> s == o);
    }

    private void send(DatagramPacket packet) throws IOException {
        LOGGER.trace("waiting to socket.send({})", () -> TOULog4JUtils.toString(packet));
        udpSocket.send(packet);
        LOGGER.trace("sent {}", () -> TOULog4JUtils.toString(packet));
    }

    private void sendSegment() throws IOException, InterruptedException {
        LOGGER.traceEntry();

        TOUSegment segment = flushAvailableOutputStream();

        if (segment == null) {
            segment = segments.poll(SEGMENT_POLL_TIMEOUT, TimeUnit.MILLISECONDS);
            LOGGER.trace("polled {}", segment);
        }

        if (segment != null) {
            send(segment);
        }

        LOGGER.traceExit();
    }

    private TOUSegment flushAvailableOutputStream() {
        LOGGER.traceEntry();

        TOUSegment segment = null;
        for (TOUSocketImpl impl : implMap.values()) {
            if (impl == null) continue;

            if (impl.outputStream == null) continue;

            segment = impl.outputStream.flushIntoSegment();

            if (segment != null) {
                break;
            }
        }

        return LOGGER.traceExit(segment);
    }

    synchronized void startIfNotAlive() {
        LOGGER.traceEntry();

        if (!sender.isAlive()) {
            sender.start();
        }
        if (!receiver.isAlive()) {
            receiver.start();
        }

        LOGGER.traceExit();
    }

    synchronized void stop() {
        LOGGER.traceEntry();

        sender.interrupt();
        receiver.interrupt();

        LOGGER.traceExit();
    }

    @Override
    public String toString() {
        return "TOUCommunicator <" + TOULog4JUtils.toString(udpSocket) + '>';
    }

    private class Sender extends Thread {
        private final Logger logger = LogManager.getLogger("Sender");

        private Sender() {
            super("Sender");
        }

        @Override
        public void run() {
            logger.traceEntry();
            try {
                while (!Thread.interrupted()) {
                    sendSegment();
                }
            } catch (InterruptedException | IOException e) {
                logger.catching(e);
            }
            logger.traceExit();
        }
    }

    private class Receiver extends Thread {
        private final Logger logger = LogManager.getLogger("Receiver");

        private Receiver() {
            super("Receiver");
        }
        @Override
        public void run() {
            logger.traceEntry();
            try {
                while (!Thread.interrupted()) {
                    logger.trace("waiting to udpSocket.receive");
                    try {
                        udpSocket.receive(udpPacket);
                    } catch (SocketTimeoutException e) {
                        logger.trace("udpSocket.receive timed out");
                        continue;
                    }
                    TOUSegment segment = TOUFactory.unpackIntoTOU(udpPacket,
                            udpSocket.getLocalAddress(), udpPacket.getAddress());
                    logger.debug("received {}", segment);
                    processSegment(segment);
                }
            } catch (IOException e) {
                logger.catching(e);
            }
            logger.traceExit();
        }
    }
}
