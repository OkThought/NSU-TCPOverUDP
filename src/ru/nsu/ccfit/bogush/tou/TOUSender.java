package ru.nsu.ccfit.bogush.tou;

import ru.nsu.ccfit.bogush.tcp.TCPUnknownPacketTypeException;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

class TOUSender extends Thread {
    private final DatagramSocket udpSocket;
    private final BlockingQueue<TOUPacket> dataPackets;
    private final BlockingQueue<TOUSystemPacket> systemPackets;

    TOUSender(DatagramSocket udpSocket, int queueCapacity) throws IOException {
        this.udpSocket = udpSocket;
        dataPackets = new ArrayBlockingQueue<>(queueCapacity);
        systemPackets = new ArrayBlockingQueue<>(queueCapacity);
    }

    @Override
    public void run() {
        try {
            while (!Thread.interrupted()) {
                DatagramPacket udpPacket;
                if (systemPackets.isEmpty()) {
                    TOUPacket dataPacket;
                    synchronized (dataPackets) {
                        dataPacket = dataPackets.take();
                        dataPackets.put(dataPacket);
                    }
                    udpPacket = TOUPacketFactory.encapsulateIntoUDP(dataPacket);
                } else {
                    TOUSystemPacket systemPacket = systemPackets.take();
                    udpPacket = prepareSystemPacketForSending(systemPacket);
                }
                udpSocket.send(udpPacket);
            }
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
    }

    DatagramPacket prepareSystemPacketForSending(TOUSystemPacket systemPacket) throws InterruptedException {
        for (TOUPacket dataPacket: dataPackets) {
            if (TOUPacketFactory.canMerge(dataPacket, systemPacket)) {
                TOUPacketFactory.mergeSystemPacket(dataPacket, systemPacket);
//                dataPackets.removeFromQueue(dataPacket);
                return TOUPacketFactory.encapsulateIntoUDP(dataPacket);
            }
        }
        // couldn't merge -> put back into queue
        systemPackets.put(systemPacket);

        return TOUPacketFactory.encapsulateIntoUDP(systemPacket);
    }

    boolean tryToMergeWithAnyDataPacket(TOUSystemPacket systemPacket) {
        for (TOUPacket dataPacket: dataPackets) {
            if (TOUPacketFactory.canMerge(dataPacket, systemPacket)) {
                TOUPacketFactory.mergeSystemPacket(dataPacket, systemPacket);
                return true;
            }
        }
        return false;
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
}
