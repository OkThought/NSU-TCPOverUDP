package ru.nsu.ccfit.bogush.tou;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.nsu.ccfit.bogush.tcp.TCPUnknownPacketTypeException;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

class TOUSender extends Thread {
    static {
        TOULog4JUtils.initIfNotInitYet();
    }
    private static final Logger LOGGER = LogManager.getLogger("TOUSender");

    private final DatagramSocket udpSocket;
    private final BlockingQueue<TOUPacket> dataPackets;
    private final BlockingQueue<TOUSystemPacket> systemPackets;
    private final ArrayList<TOUBufferHolder> bufferHolders;
    private final int timeout;

    TOUSender(DatagramSocket udpSocket, int queueCapacity, int timeout) throws IOException {
        super("TOUSender");
        LOGGER.traceEntry("socket: {} queue capacity: {} timeout: {}",
                ()->TOULog4JUtils.toString(udpSocket), ()->queueCapacity, ()->timeout);

        this.udpSocket = udpSocket;
        dataPackets = new ArrayBlockingQueue<>(queueCapacity);
        systemPackets = new ArrayBlockingQueue<>(queueCapacity);
        bufferHolders = new ArrayList<>();
        this.timeout = timeout;

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

    void addBufferHolder(TOUBufferHolder bufferHolder) {
        LOGGER.traceEntry("bufferHolder: ", ()->bufferHolder);

        bufferHolders.add(bufferHolder);

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
        dataPacket = dataPackets.poll(timeout, TimeUnit.MILLISECONDS);
        if (dataPacket != null) LOGGER.debug("polled {}", dataPacket);

        if (dataPacket == null) {
            dataPacket = flushBiggestAvailableBuffer();
        }

        if (dataPacket != null) {
            dataPackets.put(dataPacket);
            send(dataPacket);
        }

        LOGGER.traceExit();
    }

    private TOUPacket flushBiggestAvailableBuffer() {
        LOGGER.traceEntry();

        Optional<TOUBufferHolder> b = bufferHolders.stream().max(Comparator.comparingInt(TOUBufferHolder::available));

        return LOGGER.traceExit(b.isPresent() ? b.get().flushIntoPacket() : null);
    }

    private void sendSystemPackets() throws InterruptedException, IOException {
        LOGGER.traceEntry();

        while (true) {
            TOUSystemPacket systemPacket = systemPackets.poll(timeout, TimeUnit.MILLISECONDS);
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
