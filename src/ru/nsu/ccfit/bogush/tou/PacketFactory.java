package ru.nsu.ccfit.bogush.tou;

import ru.nsu.ccfit.bogush.tcp.TCPPacket;
import ru.nsu.ccfit.bogush.tcp.TCPUnknownPacketTypeException;

import java.net.DatagramPacket;
import java.net.InetAddress;

import static ru.nsu.ccfit.bogush.tcp.TCPPacketType.*;

class PacketFactory {
    static DatagramPacket encapsulateIntoUDP (TCPPacket packet, InetAddress destinationAddress) {
        byte[] data = packet.bytes();
        return new DatagramPacket(data, data.length, destinationAddress, packet.destinationPort());
    }

    static DatagramPacket encapsulateIntoUDP (TOUPacket packet) {
        return encapsulateIntoUDP(packet.tcpPacket(), packet.destinationAddress());
    }


    static TOUPacket encapsulateIntoTOU(TCPPacket packet, InetAddress sourceAddress, InetAddress destinationAddress) {
        return new TOUPacket(packet, sourceAddress, destinationAddress);
    }

    static TCPPacket decapsulateTCP (DatagramPacket packet) {
        TCPPacket p = new TCPPacket(packet.getData());
        p.sourcePort((short) packet.getPort());

        return p;
    }

    static TOUPacket morphIntoTOU (DatagramPacket packet) {
        return encapsulateIntoTOU(decapsulateTCP(packet), null, packet.getAddress());
    }

    static DatagramPacket encapsulateIntoUDP(TOUSystemPacket systemPacket) {
        TCPPacket tcpPacket = new TCPPacket(TCPPacket.HEADER_SIZE);
        return encapsulateIntoUDP(tcpPacket, systemPacket.destinationAddress());
    }

    static boolean canMerge(TOUPacket dataPacket, TOUSystemPacket systemPacket) {
        if (dataPacket.typeByte() != 0) return false;
        if (dataPacket.destinationPort() != systemPacket.destinationPort()) return false;
        if (!dataPacket.destinationAddress().equals(systemPacket.destinationAddress())) return false;
        switch (systemPacket.type()) {
            case ACK: return true;
            case SYN:
            case FIN: return dataPacket.sequenceNumber() == systemPacket.systemMessage();
            case SYNACK:
            case FINACK: return dataPacket.sequenceNumber() == systemPacket.sequenceNumber();
        }
        return false;
    }

    static boolean isSystemPacket(TOUPacket dataPacket) {
        return dataPacket.typeByte() != 0;
    }

    static boolean isMergedWithSystemPacket(TOUPacket dataPacket, TOUSystemPacket systemPacket) throws TCPUnknownPacketTypeException {
        switch (dataPacket.type()) {
            case ACK: return dataPacket.ackNumber() == systemPacket.systemMessage();
            case SYN:
            case FIN: return dataPacket.sequenceNumber() == systemPacket.systemMessage();
            case SYNACK:
            case FINACK: return dataPacket.sequenceAndAckNumbers() == systemPacket.systemMessage();
        }
        return false;
    }

    static void mergeSystemPacket(TOUPacket dataPacket, TOUSystemPacket systemPacket) {
        dataPacket.typeByte(systemPacket.type().toByte());
        dataPacket.ackNumber(systemPacket.ackNumber());
    }

    static void unmergeSystemPacket(TOUPacket dataPacket) throws TCPUnknownPacketTypeException {
        dataPacket.type(ORDINARY);
        dataPacket.ackNumber((short) 0);
    }
}
