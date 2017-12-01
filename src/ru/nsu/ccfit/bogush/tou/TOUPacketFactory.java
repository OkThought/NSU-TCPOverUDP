package ru.nsu.ccfit.bogush.tou;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.nsu.ccfit.bogush.tcp.TCPPacket;
import ru.nsu.ccfit.bogush.tcp.TCPPacketType;
import ru.nsu.ccfit.bogush.tcp.TCPUnknownPacketTypeException;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.concurrent.ThreadLocalRandom;

import static ru.nsu.ccfit.bogush.tcp.TCPPacketType.*;

class TOUPacketFactory {
    private static final Logger LOGGER = LogManager.getLogger("PacketFactory");

    static {
        TOULog4JUtils.initIfNotInitYet();
    }

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
        TCPPacket p = new TCPPacket(packet.getData(), packet.getOffset(), packet.getLength());
        p.sourcePort(packet.getPort());
        return p;
    }

    static TOUPacket morphIntoTOU (DatagramPacket packet) {
        return encapsulateIntoTOU(decapsulateTCP(packet), null, packet.getAddress());
    }

    static DatagramPacket encapsulateIntoUDP(TOUSystemPacket systemPacket) {
        TCPPacket tcpPacket = createTCPPacket(systemPacket);
        return encapsulateIntoUDP(tcpPacket, systemPacket.destinationAddress());
    }

    static TCPPacket createTCPPacket(TOUSystemPacket systemPacket) {
        TCPPacket p = new TCPPacket();
        p.flags(systemPacket.type().toByte());
        p.ackNumber(systemPacket.ackNumber());
        p.sequenceNumber(systemPacket.sequenceNumber());
        p.sourcePort(systemPacket.sourcePort());
        p.destinationPort(systemPacket.destinationPort());
        return p;
    }

    static TOUPacket createTOUPacketByAck(TOUSystemPacket systemPacket) {
        TCPPacket p = new TCPPacket();
        p.sourcePort(systemPacket.destinationPort());
        p.destinationPort(systemPacket.sourcePort());
        p.sequenceNumber(systemPacket.ackNumber());
        return new TOUPacket(p, systemPacket.destinationAddress(), systemPacket.sourceAddress());
    }

    static TOUSystemPacket createSynOrFin(TCPPacketType type, InetAddress srcAddr, int srcPort, InetAddress dstAddr, int dstPort) {
        LOGGER.traceEntry("create {} source: {}:{} destination: {}:{}", type, srcAddr, srcPort, dstAddr, dstPort);

        assert type == SYN || type == FIN;

        return LOGGER.traceExit(new TOUSystemPacket(type, srcAddr, srcPort, dstAddr, dstPort, rand(), (short) 0));
    }

    static TOUSystemPacket createSynackOrFinack(TCPPacketType type, InetAddress localAddress, int localPort, TOUSystemPacket synOrFin) {
        LOGGER.traceEntry("create {}", type);

        assert type == SYNACK || type == FINACK;
        TOUSystemPacket synackOrFinack = new TOUSystemPacket(synOrFin);
        synackOrFinack.sourceAddress(localAddress);
        synackOrFinack.sourcePort(localPort);
        synackOrFinack.destinationAddress(synOrFin.sourceAddress());
        synackOrFinack.destinationPort(synOrFin.sourcePort());
        synackOrFinack.type(type);
        synackOrFinack.ackNumber((short) (synOrFin.sequenceNumber() + 1));
        synackOrFinack.sequenceNumber(rand());

        return LOGGER.traceExit(synackOrFinack);
    }

    static TOUSystemPacket createAck(TOUSystemPacket synackOrFinack) {
        LOGGER.traceEntry(()->synackOrFinack);

        TCPPacketType type = synackOrFinack.type();
        assert type == SYNACK || type == FINACK;
        TOUSystemPacket ack = new TOUSystemPacket(synackOrFinack);
        ack.sourceAddress(synackOrFinack.destinationAddress());
        ack.sourcePort(synackOrFinack.destinationPort());
        ack.destinationAddress(synackOrFinack.sourceAddress());
        ack.destinationPort(synackOrFinack.sourcePort());
        ack.sequenceNumber(synackOrFinack.ackNumber()); // A + 1
        ack.ackNumber((short) (synackOrFinack.sequenceNumber() + 1)); // B + 1

        return LOGGER.traceExit(ack);
    }

    static TOUSystemPacket createAckToDataPacket(TOUSystemPacket dataKeyPacket) {
        LOGGER.traceEntry(()->dataKeyPacket);

        TOUSystemPacket ack = new TOUSystemPacket();
        ack.destinationAddress(dataKeyPacket.sourceAddress());
        ack.destinationPort(dataKeyPacket.sourcePort());
        ack.sourceAddress(dataKeyPacket.destinationAddress());
        ack.sourcePort(dataKeyPacket.destinationPort());
        ack.ackNumber(dataKeyPacket.sequenceNumber());
        ack.type(ACK);

        return LOGGER.traceExit(ack);
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
        dataPacket.type(systemPacket.type());
        dataPacket.ackNumber(systemPacket.ackNumber());
    }

    static void unmergeSystemPacket(TOUPacket dataPacket) throws TCPUnknownPacketTypeException {
        dataPacket.type(ORDINARY);
        dataPacket.ackNumber((short) 0);
    }

    private static short rand() {
        LOGGER.traceEntry();
        return LOGGER.traceExit((short) ThreadLocalRandom.current().nextInt());
    }
}
