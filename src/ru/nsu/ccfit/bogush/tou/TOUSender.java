package ru.nsu.ccfit.bogush.tou;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.nsu.ccfit.bogush.tcp.TCPUnknownPacketTypeException;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

class TOUSender extends Thread {
    static {
        TOULog4JUtils.initIfNotInitYet();
    }
    private static final Logger LOGGER = LogManager.getLogger("TOUSender");

    private final TOUSocketImpl impl;
    private final DatagramSocket udpSocket;
    private final BlockingQueue<TOUPacket> dataPackets;
    private final BlockingQueue<TOUSystemPacket> systemPackets;
    private final int dataPacketPollTimeout;
    private final int systemPacketPollTimeout;

    TOUSender(TOUSocketImpl impl) throws IOException {
        super("TOUSender");
        LOGGER.traceEntry("impl: {}", ()->impl);

        this.impl = impl;
        this.udpSocket = impl.datagramSocket;
        dataPackets = new ArrayBlockingQueue<>(TOUSocketImpl.QUEUE_CAPACITY);
        systemPackets = new ArrayBlockingQueue<>(TOUSocketImpl.QUEUE_CAPACITY);
        this.dataPacketPollTimeout = TOUSocketImpl.DATA_PACKET_POLL_TIMEOUT;
        this.systemPacketPollTimeout = TOUSocketImpl.SYSTEM_PACKET_POLL_TIMEOUT;

        LOGGER.traceExit();
    }

    @Override
    public synchronized void start() {
        LOGGER.traceEntry();

        super.start();

        LOGGER.traceExit();
    }

    @Override
    public void run() {
        LOGGER.traceEntry();

        try {
            while (!Thread.interrupted()) {
                sendSystemPackets();
                sendDataPacket();
            }
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }

        LOGGER.traceExit();
    }

    void sendOnce(TOUPacket packet) throws IOException {
        LOGGER.traceEntry(()->packet);

        udpSocket.send(TOUPacketFactory.encapsulateIntoUDP(packet));

        LOGGER.traceExit();
    }

    void sendOnce(TOUSystemPacket systemPacket) throws IOException {
        LOGGER.traceEntry(()->systemPacket);

        udpSocket.send(TOUPacketFactory.encapsulateIntoUDP(systemPacket));

        LOGGER.traceExit();
    }

    void putInQueue(TOUPacket packet) throws InterruptedException {
        LOGGER.traceEntry(()->packet);

        dataPackets.put(packet);

        LOGGER.traceExit();
    }

    void putInQueue(TOUSystemPacket systemPacket) throws InterruptedException {
        LOGGER.traceEntry(()->systemPacket);

        systemPackets.put(systemPacket);

        LOGGER.traceExit();
    }

    boolean removeFromQueue(TOUSystemPacket systemPacket) throws TCPUnknownPacketTypeException {
        LOGGER.traceEntry();

        boolean removed = systemPackets.remove(systemPacket);
        if (removed) return true;
        for (TOUPacket dataPacket: dataPackets) {
            if (TOUPacketFactory.isMergedWithSystemPacket(dataPacket, systemPacket)) {
                TOUPacketFactory.unmergeSystemPacket(dataPacket);
                return true;
            }
        }

        return LOGGER.traceExit(false);
    }

    void removeFromQueue(TOUPacket packet) {
        LOGGER.traceEntry(()->packet);

        dataPackets.remove(packet);

        LOGGER.traceExit();
    }

    private TOUSystemPacket peekSystemPacketIfQueueIsNotEmpty() {
        LOGGER.traceEntry();

        TOUSystemPacket systemPacket = null;
        LOGGER.trace("lock the system packet queue");
        synchronized (systemPackets) {
            if (!systemPackets.isEmpty()) {
                systemPacket = systemPackets.peek();
            }
        }
        LOGGER.trace("unlock the system packet queue");

        return LOGGER.traceExit(systemPacket);
    }

    private void sendDataPacket() throws IOException, InterruptedException {
        LOGGER.traceEntry();

        TOUPacket dataPacket;
        dataPacket = dataPackets.poll(dataPacketPollTimeout, TimeUnit.MILLISECONDS);
        if (dataPacket != null) LOGGER.debug("polled {}", dataPacket);

        if (dataPacket == null) {
            dataPacket = flushBufferIfAvailable();
        }

        if (dataPacket != null) {
            dataPackets.put(dataPacket);
            send(dataPacket);
        }

        LOGGER.traceExit();
    }

    private TOUPacket flushBufferIfAvailable() throws IOException {
        LOGGER.traceEntry();

        TOUPacket result = null;
        if (impl.outputStream != null && impl.outputStream.available() > 0) {
            result = impl.outputStream.flushIntoPacket();
        }

        return LOGGER.traceExit(result);
    }

    private void sendSystemPackets() throws InterruptedException, IOException {
        LOGGER.traceEntry();

        while (true) {
            TOUSystemPacket systemPacket = systemPackets.poll(systemPacketPollTimeout, TimeUnit.MILLISECONDS);
            if (systemPacket != null) LOGGER.debug("polled {}", systemPacket);
            if (systemPacket == null) break;

            TOUPacket dataPacket = tryToMergeWithAnyDataPacket(systemPacket);
            if (dataPacket != null) {
                send(dataPacket);
            } else {
                send(systemPacket);
            }
        }

        LOGGER.traceExit();
    }

    private void send(TOUSystemPacket systemPacket) throws IOException {
        LOGGER.traceEntry(()->systemPacket);

        DatagramPacket udpPacket = TOUPacketFactory.encapsulateIntoUDP(systemPacket);
        LOGGER.trace("waiting to socket.send({})", ()->TOULog4JUtils.toString(udpPacket));
        udpSocket.send(udpPacket);
        LOGGER.debug("sent {}", ()->TOULog4JUtils.toString(udpPacket));

        LOGGER.traceExit();
    }

    private void send(TOUPacket dataPacket) throws IOException {
        LOGGER.traceEntry(()->dataPacket);

        DatagramPacket udpPacket = TOUPacketFactory.encapsulateIntoUDP(dataPacket);
        udpSocket.send(udpPacket);

        LOGGER.traceExit();
    }

    private TOUPacket tryToMergeWithAnyDataPacket(TOUSystemPacket systemPacket) throws InterruptedException {
        LOGGER.traceEntry();

        for (TOUPacket dataPacket: dataPackets) {
            if (TOUPacketFactory.canMerge(dataPacket, systemPacket)) {
                TOUPacketFactory.mergeSystemPacket(dataPacket, systemPacket);
                synchronized (dataPackets) {
                    dataPackets.remove(dataPacket);
                    dataPackets.put(dataPacket);
                }
                return dataPacket;
            }
        }

        return LOGGER.traceExit("null", null);
    }

    @Override
    public String toString() {
        return "TOUSender <" + TOULog4JUtils.toString(udpSocket) + '>';
    }
}
