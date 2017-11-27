package ru.nsu.ccfit.bogush.tou;

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
    private final DatagramSocket udpSocket;
    private final BlockingQueue<TOUPacket> dataPackets;
    private final BlockingQueue<TOUSystemPacket> systemPackets;
    private final ArrayList<TOUBufferHolder> bufferHolders;
    private final int timeout;

    TOUSender(DatagramSocket udpSocket, int queueCapacity, int timeout) throws IOException {
        this.udpSocket = udpSocket;
        dataPackets = new ArrayBlockingQueue<>(queueCapacity);
        systemPackets = new ArrayBlockingQueue<>(queueCapacity);
        bufferHolders = new ArrayList<>();
        this.timeout = timeout;
    }

    @Override
    public void run() {
        try {
            while (!Thread.interrupted()) {
                while (!systemPackets.isEmpty()) {
                    TOUPacket dataPacket = tryToMergeWithAnyDataPacket(systemPackets.peek());
                    if (dataPacket != null) {
                        send(dataPacket);
                    } else {
                        sendSystemPacket();
                    }
                }
                sendDataPacket();
            }
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
    }

    void sendOnce(TOUPacket packet) throws IOException {
        udpSocket.send(TOUPacketFactory.encapsulateIntoUDP(packet));
    }

    void sendOnce(TOUSystemPacket systemPacket) throws IOException {
        udpSocket.send(TOUPacketFactory.encapsulateIntoUDP(systemPacket));
    }

    void putInQueue(TOUPacket packet) throws InterruptedException {
        dataPackets.put(packet);
    }

    void putInQueue(TOUSystemPacket systemPacket) throws InterruptedException {
        systemPackets.put(systemPacket);
    }

    void removeDataPacket(short sequenceNumber) {
        dataPackets.removeIf(packet -> packet.sequenceNumber() == sequenceNumber);
    }

    boolean removeFromQueue(TOUSystemPacket systemPacket) throws TCPUnknownPacketTypeException {
        boolean removed = systemPackets.remove(systemPacket);
        if (removed) return true;
        for (TOUPacket dataPacket: dataPackets) {
            if (TOUPacketFactory.isMergedWithSystemPacket(dataPacket, systemPacket)) {
                TOUPacketFactory.unmergeSystemPacket(dataPacket);
                return true;
            }
        }
        return false;
    }

    void removeFromQueue(TOUPacket packet) {
        dataPackets.remove(packet);
    }

    void addBufferHolder(TOUBufferHolder bufferHolder) {
        bufferHolders.add(bufferHolder);
    }

    private void sendDataPacket() throws IOException, InterruptedException {
        TOUPacket dataPacket;
        Optional<TOUPacket> dataPacketOptional = Optional.empty();
        synchronized (dataPackets) {
            if (dataPackets.isEmpty()) {
                dataPacketOptional = flushAvailableBuffer();
            }
            if (dataPacketOptional.isPresent()) {
                dataPacket = dataPacketOptional.get();
                dataPackets.put(dataPacket);
            } else {
                dataPacket = dataPackets.poll(timeout, TimeUnit.MILLISECONDS);
                if (dataPacket == null) return;
                dataPackets.put(dataPacket);
            }
        }
        send(dataPacket);
    }

    private Optional<TOUPacket> flushAvailableBuffer() {
        Optional<TOUBufferHolder> b = bufferHolders.stream().max(Comparator.comparingInt(TOUBufferHolder::available));
        return Optional.ofNullable(b.isPresent() ? b.get().flushIntoPacket() : null);
    }

    private void sendSystemPacket() throws InterruptedException, IOException {
        TOUSystemPacket systemPacket;
        synchronized (systemPackets) {
            systemPacket = systemPackets.poll(timeout, TimeUnit.MILLISECONDS);
            if (systemPacket == null) return;
            systemPackets.put(systemPacket);
        }
        send(systemPacket);
    }

    private void send(TOUSystemPacket systemPacket) throws IOException {
        DatagramPacket udpPacket = TOUPacketFactory.encapsulateIntoUDP(systemPacket);
        udpSocket.send(udpPacket);
    }

    private void send(TOUPacket dataPacket) throws IOException {
        DatagramPacket udpPacket = TOUPacketFactory.encapsulateIntoUDP(dataPacket);
        udpSocket.send(udpPacket);
    }

    private TOUPacket tryToMergeWithAnyDataPacket(TOUSystemPacket systemPacket) throws InterruptedException {
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
        return null;
    }

    @Override
    public String toString() {
        return "TOUSender <" + udpSocket + '>';
    }
}
