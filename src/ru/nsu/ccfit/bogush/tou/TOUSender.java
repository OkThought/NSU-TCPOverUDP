package ru.nsu.ccfit.bogush.tou;

import ru.nsu.ccfit.bogush.tcp.TCPUnknownPacketTypeException;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

class TOUSender extends Thread {
    private DatagramSocket udpSocket;
    private BlockingQueue<TOUPacket> dataPackets;
    private BlockingQueue<TOUSystemPacket> systemPackets;

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
                    TOUPacket dataPacket = dataPackets.take();
                    udpPacket = PacketFactory.encapsulateIntoUDP(dataPacket);
                    dataPackets.put(dataPacket);
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
            if (PacketFactory.canMerge(dataPacket, systemPacket)) {
                PacketFactory.mergeSystemPacket(dataPacket, systemPacket);
//                dataPackets.remove(dataPacket);
                return PacketFactory.encapsulateIntoUDP(dataPacket);
            }
        }
        // couldn't merge -> put back into queue
        systemPackets.put(systemPacket);

        return PacketFactory.encapsulateIntoUDP(systemPacket);
    }

    boolean tryToMergeWithAnyDataPacket(TOUSystemPacket systemPacket) {
        for (TOUPacket dataPacket: dataPackets) {
            if (PacketFactory.canMerge(dataPacket, systemPacket)) {
                PacketFactory.mergeSystemPacket(dataPacket, systemPacket);
                return true;
            }
        }
        return false;
    }

    void sendOnce(TOUPacket packet) throws IOException {
        udpSocket.send(PacketFactory.encapsulateIntoUDP(packet));
    }

    void sendOnce(TOUSystemPacket systemPacket) throws IOException {
        udpSocket.send(PacketFactory.encapsulateIntoUDP(systemPacket));
    }

    void putInQueue(TOUPacket packet) throws InterruptedException {
        dataPackets.put(packet);
    }

    void putInQueue(TOUSystemPacket systemPacket) throws InterruptedException {
//        for (TOUPacket dataPacket: dataPackets) {
//            if (PacketFactory.canMerge(dataPacket, systemPacket)) {
//                PacketFactory.mergeSystemPacket(dataPacket, systemPacket);
//                return;
//            }
//        }
        systemPackets.put(systemPacket);
    }

    void removeDataPacket(short sequenceNumber) {
        dataPackets.removeIf(packet -> packet.sequenceNumber() == sequenceNumber);
    }

    boolean remove(TOUSystemPacket systemPacket) throws TCPUnknownPacketTypeException {
        boolean removed = systemPackets.remove(systemPacket);
        if (removed) return true;
        for (TOUPacket dataPacket: dataPackets) {
            if (PacketFactory.isMergedWithSystemPacket(dataPacket, systemPacket)) {
                PacketFactory.unmergeSystemPacket(dataPacket);
                return true;
            }
        }
        return false;
    }

    void remove(TOUPacket packet) {
        dataPackets.remove(packet);
    }
}
