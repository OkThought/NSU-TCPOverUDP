package ru.nsu.ccfit.bogush.tou;

import ru.nsu.ccfit.bogush.tcp.TCPPacket;

import java.net.DatagramPacket;
import java.net.InetAddress;

class PacketFactory {
    static DatagramPacket encapsulateIntoUDP (TCPPacket packet, InetAddress address) {
        byte[] data = packet.bytes();
        return new DatagramPacket(data, data.length, address, packet.destinationPort());
    }

    static DatagramPacket encapsulateIntoUDP (TOUPacket packet) {
        return encapsulateIntoUDP(packet.getTcpPacket(), packet.getAddress());
    }


    static TOUPacket encapsulateIntoTOU (TCPPacket packet, InetAddress address) {
        return new TOUPacket(packet, address, packet.sequenceNumber());
    }

    static TCPPacket decapsulateTCP (DatagramPacket packet) {
        TCPPacket p = new TCPPacket(packet.getData());
        p.sourcePort((short) packet.getPort());

        return p;
    }

    static TOUPacket morphIntoTOU (DatagramPacket packet) {
        return encapsulateIntoTOU(decapsulateTCP(packet), packet.getAddress());
    }
}
