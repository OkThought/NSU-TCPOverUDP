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

    static TOUSystemPacket createSYN(InetAddress srcAddr, int srcPort, InetAddress dstAddr, int dstPort) {
        LOGGER.traceEntry("source: {}:{} destination: {}:{}", srcAddr, srcPort, dstAddr, dstPort);
        return LOGGER.traceExit(new TOUSystemPacket(SYN, srcAddr, srcPort, dstAddr, dstPort, rand(), (short) 0));
    }

    static TOUSystemPacket createFIN(InetAddress srcAddr, int srcPort, InetAddress dstAddr, int dstPort) {
        LOGGER.traceEntry("source: {}:{} destination: {}:{}", srcAddr, srcPort, dstAddr, dstPort);
        return LOGGER.traceExit(new TOUSystemPacket(FIN, srcAddr, srcPort, dstAddr, dstPort, rand(), (short) 0));
    }

    static TOUSystemPacket createSYNACK(InetAddress localAddress, int localPort, TOUSystemPacket syn) {
        LOGGER.traceEntry("local address: {}:{} SYN: {}", localAddress, localPort, syn);

        TOUSystemPacket synack = new TOUSystemPacket(syn);
        synack.sourceAddress(localAddress);
        synack.sourcePort(localPort);
        synack.destinationAddress(syn.sourceAddress());
        synack.destinationPort(syn.sourcePort());
        synack.type(SYNACK);
        synack.ackNumber((short) (syn.sequenceNumber() + 1));
        synack.sequenceNumber(rand());

        return LOGGER.traceExit(synack);
    }

    static TOUSystemPacket createFINACK(InetAddress localAddress, int localPort, TOUSystemPacket fin) {
        LOGGER.traceEntry("local address: {}:{} FIN: {}", localAddress, localPort, fin);

        TOUSystemPacket finack = new TOUSystemPacket(fin);
        finack.sourceAddress(localAddress);
        finack.sourcePort(localPort);
        finack.destinationAddress(fin.sourceAddress());
        finack.destinationPort(fin.sourcePort());
        finack.type(FINACK);
        finack.ackNumber((short) (fin.sequenceNumber() + 1));
        finack.sequenceNumber(rand());

        return LOGGER.traceExit(finack);
    }

    static TOUSystemPacket createACK(TOUSystemPacket synackOrFinack) {
        LOGGER.traceEntry(()->synackOrFinack);

        TCPPacketType type = synackOrFinack.type();
        TOUSystemPacket ack = new TOUSystemPacket(synackOrFinack);
        ack.sourceAddress(synackOrFinack.destinationAddress());
        ack.sourcePort(synackOrFinack.destinationPort());
        ack.destinationAddress(synackOrFinack.sourceAddress());
        ack.destinationPort(synackOrFinack.sourcePort());
        ack.sequenceNumber(synackOrFinack.ackNumber());
        ack.ackNumber((short) (synackOrFinack.sequenceNumber() + 1));
        ack.type(ACK);

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

    static TOUSystemPacket createDataKeyPacket(TCPPacket packet, InetAddress srcAddr, InetAddress dstAddr) {
        TOUSystemPacket key = new TOUSystemPacket();
        key.sourceAddress(srcAddr);
        key.sourcePort(packet.sourcePort());
        key.destinationAddress(dstAddr);
        key.destinationPort(packet.destinationPort());
        key.sequenceNumber(packet.sequenceNumber());
        return key;
    }

    static TOUSystemPacket createSystemPacket(TCPPacket tcpPacket, TCPPacketType type, InetAddress srcAddr, InetAddress dstAddr) {
        TOUSystemPacket systemPacket = new TOUSystemPacket(type);
        systemPacket.sourceAddress(srcAddr);
        systemPacket.sourcePort(tcpPacket.sourcePort());
        systemPacket.destinationAddress(dstAddr);
        systemPacket.destinationPort(tcpPacket.destinationPort());
        systemPacket.sequenceNumber(tcpPacket.sequenceNumber());
        systemPacket.ackNumber(tcpPacket.ackNumber());
        return systemPacket;
    }

    static TOUSystemPacket createSystemKeyPacket(TCPPacket tcpPacket, TCPPacketType type, InetAddress srcAddr, InetAddress dstAddr) {
        TOUSystemPacket key = new TOUSystemPacket(type);
        key.destinationAddress(dstAddr);
        key.destinationPort(tcpPacket.destinationPort());
        switch (type) {
            case ACK:
                key.sourceAddress(srcAddr);
                key.sourcePort(tcpPacket.sourcePort());
                key.sequenceNumber(tcpPacket.sequenceNumber());
                key.ackNumber(tcpPacket.ackNumber());
                break;
            case SYN:
                break;
            case SYNACK:
                key.sourceAddress(srcAddr);
                key.sourcePort(tcpPacket.sourcePort());
                key.ackNumber(tcpPacket.ackNumber());
                break;
            case FIN:
                break;
            case FINACK:
                key.sourceAddress(srcAddr);
                key.sourcePort(tcpPacket.sourcePort());
                key.ackNumber(tcpPacket.ackNumber());
                break;
        }
        return key;
    }
}
