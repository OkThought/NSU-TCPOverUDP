package ru.nsu.ccfit.bogush.tou;

import ru.nsu.ccfit.bogush.tcp.TCPPacket;
import ru.nsu.ccfit.bogush.tcp.TCPPacketType;
import ru.nsu.ccfit.bogush.tcp.TCPUnknownPacketTypeException;
import ru.nsu.ccfit.bogush.util.BlockingHashMap;
import ru.nsu.ccfit.bogush.util.BlockingHashSet;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import static ru.nsu.ccfit.bogush.tcp.TCPPacketType.ORDINARY;

class TOUReceiver extends Thread {
    private DatagramSocket socket;
    private DatagramPacket packet;

    private volatile boolean ignoreDataPackets = true;

    private final BlockingHashMap<TOUSystemPacket, byte[]> dataMap = new BlockingHashMap<>();
    private final BlockingHashMap<TOUSystemPacket, TOUSystemPacket> systemPacketMap = new BlockingHashMap<>();


    TOUReceiver (DatagramSocket socket, int packetSize) {
        this.socket = socket;
        packet = new DatagramPacket(new byte[packetSize], packetSize);
    }

    @Override
    public void run () {
        try {
            while (!Thread.interrupted()) {
                socket.receive(packet);
                TCPPacket tcpPacket = TOUPacketFactory.decapsulateTCP(packet);

                if (!ignoreDataPackets && tcpPacket.data().length > 0) {
                    TOUSystemPacket key = new TOUSystemPacket();
                    key.sourceAddress(packet.getAddress());
                    key.sourcePort(tcpPacket.sourcePort());
                    key.sequenceNumber(tcpPacket.sequenceNumber());
                    dataMap.put(key, tcpPacket.data());
                }

                TCPPacketType packetType = TCPPacketType.typeOf(tcpPacket);
                if (packetType == ORDINARY) continue;

                InetAddress sourceAddress = packet.getAddress();
                int sourcePort = tcpPacket.sourcePort();
                int destinationPort = tcpPacket.destinationPort();
                InetAddress destinationAddress = socket.getInetAddress();

                TOUSystemPacket systemPacket = new TOUSystemPacket(packetType,
                        sourceAddress, sourcePort,
                        destinationAddress, destinationPort,
                        tcpPacket.sequenceNumber(), tcpPacket.ackNumber());

                TOUSystemPacket key = new TOUSystemPacket(packetType);
                key.destinationAddress(destinationAddress);
                key.destinationPort(destinationPort);
                switch (packetType) {
                    case ACK:
                        key.sourceAddress(sourceAddress);
                        key.sourcePort(sourcePort);
                        key.sequenceNumber(tcpPacket.sequenceNumber());
                        key.ackNumber(tcpPacket.ackNumber());
                        break;
                    case SYN:
                        break;
                    case SYNACK:
                        key.sourceAddress(sourceAddress);
                        key.sourcePort(sourcePort);
                        key.ackNumber(tcpPacket.ackNumber());
                        break;
                    case FIN:
                        break;
                    case FINACK:
                        key.sourceAddress(sourceAddress);
                        key.sourcePort(sourcePort);
                        key.ackNumber(tcpPacket.ackNumber());
                        break;
                }
                systemPacketMap.put(key, systemPacket);
            }
        } catch (IOException | TCPUnknownPacketTypeException e) {
            e.printStackTrace();
        }
    }

    void ignoreDataPackets(boolean newValue) {
        ignoreDataPackets = newValue;
    }

    byte[] takeData(InetAddress sourceAddress, int sourcePort, short sequenceNumber) throws InterruptedException {
        TOUSystemPacket key = new TOUSystemPacket();
        key.sourceAddress(sourceAddress);
        key.sourcePort(sourcePort);
        key.sequenceNumber(sequenceNumber);
        return dataMap.take(key);
    }

    TOUSystemPacket receiveSystemPacket(TOUSystemPacket systemPacketKey) throws InterruptedException {
        return systemPacketMap.take(systemPacketKey);
    }
}
