package ru.nsu.ccfit.bogush.net;

import java.net.DatagramPacket;
import java.net.InetAddress;

class PacketFactory {
    static DatagramPacket encapsulateIntoUDP (TCPPacket packet, InetAddress address) {
        byte[] data = packet.getBytes();
        return new DatagramPacket(data, data.length, address, packet.getDestinationPort());
    }

    static DatagramPacket encapsulateIntoUDP (TOUPacket packet) {
        return encapsulateIntoUDP(packet.getTcpPacket(), packet.getAddress());
    }


    static TOUPacket encapsulateIntoTOU (TCPPacket packet, InetAddress address) {
        return new TOUPacket(packet, address, packet.getSequenceNumber());
    }

    static TCPPacket decapsulateTCP (DatagramPacket packet) {
        TCPPacket p = new TCPPacket(packet.getData());
        p.setSourcePort((short) packet.getPort());

        return p;
    }

    static TOUPacket morphIntoTOU (DatagramPacket packet) {
        return encapsulateIntoTOU(decapsulateTCP(packet), packet.getAddress());
    }
}
