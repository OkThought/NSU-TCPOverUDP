package ru.nsu.ccfit.bogush.tou;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.nsu.ccfit.bogush.tcp.TCPSegment;
import ru.nsu.ccfit.bogush.tcp.TCPSegmentType;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static ru.nsu.ccfit.bogush.tcp.TCPSegmentType.ORDINARY;
import static ru.nsu.ccfit.bogush.tou.TOUConstants.*;

class TOUCommunicator {
    static { TOULog4JUtils.initIfNotInitYet(); }
    private static final Logger LOGGER = LogManager.getLogger("TOUCommunicator");

    private final DatagramSocket udpSocket;
    private final DatagramPacket udpPacket;
    private final ConcurrentHashMap<InetSocketAddress, ArrayList<TOUSocketImpl>> socketMap;
    private final Thread sender;
    private final Thread receiver;

    TOUCommunicator(DatagramSocket datagramSocket) throws IOException {
        LOGGER.traceEntry();

        this.udpSocket = datagramSocket;
        this.udpPacket = new DatagramPacket(new byte[MAX_PACKET_SIZE], MAX_PACKET_SIZE);
        this.socketMap = new ConcurrentHashMap<>();
        this.sender = new Thread(() -> {
            LOGGER.traceEntry();
            try {
                while (!Thread.interrupted()) {
                    for (ArrayList<TOUSocketImpl> impls : socketMap.values()) {
                        // serialize access to impl list
                        synchronized (this) {
                            for (TOUSocketImpl impl : impls) {
                                sendSystemMessages(impl);
                                sendSegment(impl);
                            }
                        }
                    }
                }
            } catch (InterruptedException | IOException e) {
                LOGGER.catching(e);
//                e.printStackTrace();
            }
            LOGGER.traceExit();
        }, "sender");

        this.receiver = new Thread(() -> {
            LOGGER.traceEntry();

            try {
                while (!Thread.interrupted()) {
                    LOGGER.trace("waiting to datagramSocket.receive");
                    try {
                        udpSocket.receive(udpPacket);
                    } catch (SocketTimeoutException e) {
                        LOGGER.trace("datagramSocket.receive timed out");
                        continue;
                    }
                    processReceivedPacket(udpPacket);
                }
            } catch (IOException e) {
                LOGGER.catching(e);
//                e.printStackTrace();
            }

            LOGGER.traceExit();
        }, "receiver");

        LOGGER.traceExit();
    }

    private void processReceivedPacket(DatagramPacket packet) throws IOException {
        InetAddress srcAddr = packet.getAddress();
        InetAddress dstAddr = udpSocket.getLocalAddress();
        TCPSegment tcpSegment = TOUFactory.unpackTCP(packet);
        LOGGER.debug("received {}", tcpSegment);

        InetSocketAddress destination = new InetSocketAddress(dstAddr, tcpSegment.destinationPort());
        ArrayList<TOUSocketImpl> impls = socketMap.get(destination);
        if (impls == null || impls.isEmpty()) {
            LOGGER.warn("Received packet to unknown socket address: {}:{}",
                    destination.getAddress(), destination.getPort());
            return;
        }
        LOGGER.trace("got impls appropriate to {}: {}", destination, impls);

        if (tcpSegment.dataSize() > 0) {
            LOGGER.trace("this packet contains data");
            short key = tcpSegment.sequenceNumber();
            for (TOUSocketImpl impl : impls) {
                if (impl.isConnected()) {
                    LOGGER.trace("put data into segmentMap at the key: {}", key);
                    impl.segmentMap().put(key, tcpSegment.data());
                    sendOnce(impl.factory.createSegmentACK(key));
                }
            }
        }

        TCPSegmentType segmentType = TCPSegmentType.typeOf(tcpSegment);
        if (segmentType == ORDINARY) {
            return;
        }

        TOUSystemMessage systemMessage = TOUFactory.createSystemMessage(tcpSegment, segmentType, srcAddr, dstAddr);
        LOGGER.trace("received system message: {}", systemMessage);
        for (TOUSocketImpl impl : impls) {
            impl.systemMessageReceived(systemMessage);
        }
    }

    private void sendSystemMessages(TOUSocketImpl impl) throws InterruptedException, IOException {
        LOGGER.traceEntry();

        while (true) {
            TOUSystemMessage systemMessage = impl.systemMessageQueue().poll(SYSTEM_PACKET_POLL_TIMEOUT, TimeUnit.MILLISECONDS);
            if (systemMessage != null) LOGGER.trace("polled {}", systemMessage);
            if (systemMessage == null) break;

            TOUSegment segment = tryToMergeWithAnySegment(systemMessage, impl.segmentQueue());
            if (segment != null) {
                send(segment);
            } else {
                send(systemMessage);
            }
        }

        LOGGER.traceExit();
    }

    private TOUSegment tryToMergeWithAnySegment(TOUSystemMessage systemMessage, BlockingQueue<TOUSegment> segments)
            throws InterruptedException {
        LOGGER.traceEntry("system message: {}", () -> systemMessage);

        for (TOUSegment segment : segments) {
            if (TOUFactory.canMerge(segment, systemMessage)) {
                TOUFactory.merge(segment, systemMessage);
                return segment;
            }
        }

        return LOGGER.traceExit("null", null);
    }

    private void send(TOUSystemMessage systemMessage) throws IOException {
        LOGGER.traceEntry("system message: {}", () -> systemMessage);

        LOGGER.debug("send system message: {}", systemMessage);
        DatagramPacket packet = TOUFactory.packIntoUDP(systemMessage);
        send(packet);

        LOGGER.traceExit();
    }

    private void send(TOUSegment segment) throws IOException {
        LOGGER.traceEntry("data segment: {}", () -> segment);

        LOGGER.debug("send data segment: {}", segment);
        DatagramPacket packet = TOUFactory.packIntoUDP(segment);
        send(packet);

        LOGGER.traceExit();
    }

    private void send(DatagramPacket packet) throws IOException {
        LOGGER.trace("waiting to socket.send({})", () -> TOULog4JUtils.toString(packet));
        udpSocket.send(packet);
        LOGGER.trace("sent {}", () -> TOULog4JUtils.toString(packet));
    }

    private void sendSegment(TOUSocketImpl impl) throws IOException, InterruptedException {
        LOGGER.traceEntry();


        TOUSegment segment;
        segment = impl.segmentQueue().poll(DATA_PACKET_POLL_TIMEOUT, TimeUnit.MILLISECONDS);
        LOGGER.trace("polled {}", segment);
        if (segment != null) {
        } else if (impl.isConnected()) {
            segment = flushBufferIfAvailable(impl);
        }

        if (segment != null) {
            send(segment);
            if (segment.needsResending()) {
                impl.segmentQueue().put(segment);
            }
        }

        LOGGER.traceExit();
    }

    private TOUSegment flushBufferIfAvailable(TOUSocketImpl impl) {
        LOGGER.traceEntry();

        TOUSegment result = null;
        if (impl.outputStream != null && impl.outputStream.available() > 0) {
            result = impl.outputStream.flushIntoSegment();
        }

        return LOGGER.traceExit(result);
    }

    synchronized void addSocketImpl(TOUSocketImpl impl) {
        LOGGER.traceEntry("{}", impl);

        InetSocketAddress key = impl.localSocketAddress();
        if (!socketMap.containsKey(key)) {
            socketMap.put(key, new ArrayList<>());
        }
        socketMap.get(key).add(impl);

        LOGGER.traceExit();
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

    void sendOnce(TOUSegment segment) throws IOException {
        LOGGER.traceEntry(() -> segment);

        send(segment);

        LOGGER.traceExit();
    }

    void sendOnce(TOUSystemMessage systemMessage) throws IOException {
        LOGGER.traceEntry(() -> systemMessage);

        send(systemMessage);

        LOGGER.traceExit();
    }

    @Override
    public String toString() {
        return "TOUCommunicator <" + TOULog4JUtils.toString(udpSocket) + '>';
    }
}
