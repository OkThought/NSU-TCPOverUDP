package ru.nsu.ccfit.bogush.tou;

import ru.nsu.ccfit.bogush.tcp.TCPPacket;
import ru.nsu.ccfit.bogush.tcp.TCPPacketType;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

class TOUReceiver extends Thread {
    private DatagramSocket socket;
    private DatagramPacket packet;

    private boolean sequenceNotStarted = true;
    private short sequenceNumber;
    private final Object sequenceLock = new Object();

    private ConcurrentHashMap<TCPPacketType, ConcurrentHashMap<Integer, TOUPacket>> packetMapMap;
    private final Object lockPacketMap = new Object();

    TOUReceiver (DatagramSocket socket, int packetSize) {
        this.socket = socket;
        packet = new DatagramPacket(new byte[packetSize], packetSize);
        packetMapMap = new ConcurrentHashMap<>();
        packetMapMap.put(TCPPacketType.ORDINARY, new ConcurrentHashMap<>());
        packetMapMap.put(TCPPacketType.ACK, new ConcurrentHashMap<>());
        packetMapMap.put(TCPPacketType.SYN, new ConcurrentHashMap<>());
        packetMapMap.put(TCPPacketType.SYNACK, new ConcurrentHashMap<>());
    }

    @Override
    public void run () {
        try {
            while (!Thread.interrupted()) {
                socket.receive(packet);
                TCPPacket tcpPacket = PacketFactory.decapsulateTCP(packet);
                TOUPacket touPacket = PacketFactory.encapsulateIntoTOU(tcpPacket, packet.getAddress());

                int key = tcpPacket.getAckSeq();

                TCPPacketType packetType = TCPPacketType.typeOf(tcpPacket);

                if (packetType == TCPPacketType.ORDINARY && sequenceNotStarted) {
                    synchronized (sequenceLock) {
                        sequenceNumber = tcpPacket.getSequenceNumber();
                        sequenceNotStarted = false;
                        sequenceLock.notifyAll();
                    }
                }

                ConcurrentHashMap<Integer, TOUPacket> packetMap = packetMapMap.get(packetType);
                synchronized (lockPacketMap) {
                    packetMap.putIfAbsent(key, touPacket);
                    lockPacketMap.notifyAll();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    TOUPacket takePacket (TCPPacketType type, int id) throws InterruptedException {
        ConcurrentHashMap<Integer, TOUPacket> packetMap = packetMapMap.get(type);
        synchronized (lockPacketMap) {
            while (!packetMap.containsKey(id)) {
                lockPacketMap.wait();
            }
            return packetMap.remove(id);
        }
    }

    Collection<TOUPacket> packetsOfType (TCPPacketType type) {
        return packetMapMap.get(type).values();
    }


    TOUPacket takeSubsequentOrdinaryPacket () throws InterruptedException {
        int currentSequence;
        synchronized (sequenceLock) {
            while (sequenceNotStarted) {
                sequenceLock.wait();
            }
            currentSequence = sequenceNumber++;
        }
        return takePacket(TCPPacketType.ORDINARY, currentSequence);
    }
}
