package ru.nsu.ccfit.bogush.net;

import java.io.IOException;
import java.net.DatagramSocket;
import java.util.concurrent.LinkedBlockingQueue;

class TOUSender extends Thread {
    private DatagramSocket socket;
    private LinkedBlockingQueue<TOUPacket> packetQueue;
    private final Object lockTheQueue = new Object();

    TOUSender(DatagramSocket socket, int queueCapacity) throws IOException {
        this.socket = socket;
        packetQueue = new LinkedBlockingQueue<>(queueCapacity);
    }

    @Override
    public void run() {
        try {
            while (!Thread.interrupted()) {
                TOUPacket packet;
                synchronized (lockTheQueue) {
                    packet = packetQueue.take();
                    packetQueue.put(packet);
                }
                socket.send(PacketFactory.encapsulateIntoUDP(packet));
            }
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
    }

    void sendOnce(TOUPacket packet) throws IOException {
        socket.send(PacketFactory.encapsulateIntoUDP(packet));
    }

    void put(TOUPacket packet) throws InterruptedException {
        packetQueue.put(packet);
    }

    void remove(short sequenceNumber) {
        synchronized (lockTheQueue) {
            packetQueue.remove(new TOUPacket(null, null, sequenceNumber));
        }
    }

    void remove(TCPPacket packet) {
        remove(packet.getSequenceNumber());
    }

    void remove(TOUPacket packet) {
        remove(packet.getSequenceNumber());
    }
}
