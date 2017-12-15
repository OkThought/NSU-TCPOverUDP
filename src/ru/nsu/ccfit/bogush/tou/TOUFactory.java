package ru.nsu.ccfit.bogush.tou;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.nsu.ccfit.bogush.tcp.TCPSegment;
import ru.nsu.ccfit.bogush.tcp.TCPSegmentType;
import ru.nsu.ccfit.bogush.tcp.TCPUnknownSegmentTypeException;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.ThreadLocalRandom;

import static ru.nsu.ccfit.bogush.tcp.TCPSegmentType.*;
import static ru.nsu.ccfit.bogush.tou.TOUConstants.SEGMENT_TIMEOUT;
import static ru.nsu.ccfit.bogush.tou.TOUConstants.SYSTEM_MESSAGE_TIMEOUT;

class TOUFactory {
    static { TOULog4JUtils.initIfNotInitYet(); }
    private static final Logger LOGGER = LogManager.getLogger("SegmentFactory");

    private final TOUSocketImpl impl;

    TOUFactory(TOUSocketImpl impl) {
        this.impl = impl;
    }

    static DatagramPacket packIntoUDP(TOUSegment segment) {
        return packIntoUDP(segment.tcpSegment, segment.destinationAddress());
    }

    static DatagramPacket packIntoUDP(TCPSegment segment, InetAddress destinationAddress) {
        byte[] data = segment.bytes();
        return new DatagramPacket(data, data.length, destinationAddress, segment.destinationPort());
    }

    static TCPSegment unpackIntoTCP(DatagramPacket segment) {
        TCPSegment p = new TCPSegment(segment.getData(), segment.getOffset(), segment.getLength());
        p.sourcePort(segment.getPort());
        return p;
    }

    static TOUSegment unpackIntoTOU(DatagramPacket packet, InetAddress srcAddr, InetAddress dstAddr) {
        return packIntoTOU(unpackIntoTCP(packet), srcAddr, dstAddr);
    }

    static TOUSegment packIntoTOU(TCPSegment segment, InetAddress srcAddr, InetAddress dstAddr) {
        return new TOUSegment(segment, srcAddr, dstAddr, 0);
    }

    static TOUSystemMessage createSYNorFIN(TCPSegmentType type,
                                           InetAddress srcAddr, int srcPort,
                                           InetAddress dstAddr, int dstPort) {
        return new TOUSystemMessage(type, srcAddr, srcPort, dstAddr, dstPort, rand(), (short) 0, SYSTEM_MESSAGE_TIMEOUT);
    }

    private static short rand() {
        return (short) ThreadLocalRandom.current().nextInt();
    }

    static TOUSystemMessage createSYNACKorFINACK(InetAddress localAddress, int localPort, TOUSystemMessage synOrFin) {
        LOGGER.traceEntry("local address: {}:{} SYN: {}", localAddress, localPort, synOrFin);

        TOUSystemMessage synack = new TOUSystemMessage(synOrFin);
        synack.sourceAddress(localAddress);
        synack.sourcePort(localPort);
        synack.destinationAddress(synOrFin.sourceAddress());
        synack.destinationPort(synOrFin.sourcePort());
        synack.type(synOrFin.type() == SYN ? SYNACK : FINACK);
        synack.ackNumber((short) (synOrFin.sequenceNumber() + 1));
        synack.sequenceNumber(rand());
        synack.setTimeout(SYSTEM_MESSAGE_TIMEOUT);

        return LOGGER.traceExit(synack);
    }

    static TOUSystemMessage createACK(TOUSystemMessage synackOrFinack) {
        LOGGER.traceEntry(() -> synackOrFinack);

        TCPSegmentType type = synackOrFinack.type();
        TOUSystemMessage ack = new TOUSystemMessage(synackOrFinack);
        swapSourceAndDestination(ack);
        ack.sequenceNumber(synackOrFinack.ackNumber());
        ack.ackNumber((short) (synackOrFinack.sequenceNumber() + 1));
        ack.setTimeout(0);
        ack.type(ACK);

        return LOGGER.traceExit(ack);
    }

    static TOUSystemMessage createACK(TOUSegment segment) {
        TOUSystemMessage ack = new TOUSystemMessage(segment, ACK);
        swapSourceAndDestination(ack);
        ack.sequenceNumber((short) 0);
        ack.ackNumber(segment.sequenceNumber());
        ack.setTimeout(0);
        return ack;
    }

    private static void swapSourceAndDestination(TOUSegment segment) {
        InetAddress srcAddr = segment.sourceAddress;
        segment.sourceAddress = segment.destinationAddress;
        segment.destinationAddress = srcAddr;
        int srcPort = segment.sourcePort();
        segment.sourcePort(segment.destinationPort());
        segment.destinationPort(srcPort);
    }

    private static TOUSystemMessage createACK(short sequenceNumber, InetSocketAddress local, InetSocketAddress remote) {
        return createACK(sequenceNumber, local.getAddress(), local.getPort(), remote.getAddress(), remote.getPort());
    }

    private static TOUSystemMessage createACK(short sequenceNumber,
                                              InetAddress localAddress, int localPort,
                                              InetAddress remoteAddress, int remotePort) {
        TOUSystemMessage ack = new TOUSystemMessage();
        ack.destinationAddress(remoteAddress);
        ack.destinationPort(remotePort);
        ack.sourceAddress(localAddress);
        ack.sourcePort(localPort);
        ack.ackNumber(sequenceNumber);
        ack.setTimeout(0);
        ack.type(ACK);
        return ack;
    }

    static boolean canMerge(TOUSegment segment, TOUSystemMessage systemMessage) {
        // allow only ACK + ORDINARY merging
        return  segment.typeByte() == 0 &&
                systemMessage.type() == ACK;
    }

    static boolean isMergedWithSystemMessage(TOUSegment dataSegment, TOUSystemMessage systemMessage) {
        return  dataSegment.typeByte() == ACK.toByte() &&
                dataSegment.ackNumber() == systemMessage.ackNumber();
    }

    static void merge(TOUSegment dataSegment, TOUSystemMessage systemMessage) {
        dataSegment.type(systemMessage.type());
        dataSegment.ackNumber(systemMessage.ackNumber());
    }

    static void unmerge(TOUSegment dataSegment)  {
        dataSegment.type(ORDINARY);
        dataSegment.ackNumber((short) 0);
    }

    static TOUSystemMessage generateDataSegmentKey(TCPSegment segment, InetAddress srcAddr, InetAddress dstAddr) {
        TOUSystemMessage key = new TOUSystemMessage();
        key.sourceAddress(srcAddr);
        key.sourcePort(segment.sourcePort());
        key.destinationAddress(dstAddr);
        key.destinationPort(segment.destinationPort());
        key.sequenceNumber(segment.sequenceNumber());
        return key;
    }

    static TOUSystemMessage generateSystemMessageKey(TCPSegment tcpSegment, InetAddress srcAddr, InetAddress dstAddr)
            throws TCPUnknownSegmentTypeException {
        return generateSystemMessageKey(TCPSegmentType.typeOf(tcpSegment),
                srcAddr, tcpSegment.sourcePort(),
                dstAddr, tcpSegment.destinationPort(),
                tcpSegment.sequenceNumber(), tcpSegment.ackNumber());
    }

    static TOUSystemMessage generateSystemMessageKey(TCPSegment tcpSegment, TCPSegmentType type,
                                                    InetAddress srcAddr, InetAddress dstAddr) {
        return generateSystemMessageKey(type,
                srcAddr, tcpSegment.sourcePort(),
                dstAddr, tcpSegment.destinationPort(),
                tcpSegment.sequenceNumber(), tcpSegment.ackNumber());
    }

    static TOUSystemMessage generateSystemMessageKey(TOUSystemMessage systemMessage) {
        return generateSystemMessageKey(systemMessage.type(),
                systemMessage.sourceAddress(), systemMessage.sourcePort(),
                systemMessage.destinationAddress(), systemMessage.destinationPort(),
                systemMessage.sequenceNumber(), systemMessage.ackNumber());
    }

    static TOUSystemMessage generateSystemMessageKey(TCPSegmentType type,
                                                    InetAddress srcAddr, int srcPort,
                                                    InetAddress dstAddr, int dstPort,
                                                    short seq, short ack) {
        TOUSystemMessage key = new TOUSystemMessage(type);
        key.destinationAddress(dstAddr);
        key.destinationPort(dstPort);
        switch (type) {
            case ACK:
                key.sourceAddress(srcAddr);
                key.sourcePort(srcPort);
                key.sequenceNumber(seq);
                key.ackNumber(ack);
                break;
            case SYN:
                break;
            case SYNACK:
                key.sourceAddress(srcAddr);
                key.sourcePort(srcPort);
                key.ackNumber(ack);
                break;
            case FIN:
                break;
            case FINACK:
                key.sourceAddress(srcAddr);
                key.sourcePort(srcPort);
                key.ackNumber(ack);
                break;
        }
        return key;
    }

    static TOUSystemMessage createSystemMessage(TCPSegment tcpSegment, InetAddress srcAddr, InetAddress dstAddr)
            throws TCPUnknownSegmentTypeException {
        return createSystemMessage(tcpSegment, TCPSegmentType.typeOf(tcpSegment), srcAddr, dstAddr);
    }

    static TOUSystemMessage createSystemMessage(TCPSegment tcpSegment, TCPSegmentType type, InetAddress srcAddr, InetAddress dstAddr) {
        TOUSystemMessage systemMessage = new TOUSystemMessage(type);
        systemMessage.sourceAddress(srcAddr);
        systemMessage.sourcePort(tcpSegment.sourcePort());
        systemMessage.destinationAddress(dstAddr);
        systemMessage.destinationPort(tcpSegment.destinationPort());
        systemMessage.sequenceNumber(tcpSegment.sequenceNumber());
        systemMessage.ackNumber(tcpSegment.ackNumber());
        return systemMessage;
    }

    TOUSystemMessage createSYNACKorFINACK(TOUSystemMessage synOrFin) {
        return createSYNACKorFINACK(impl.localAddress(), impl.localPort(), synOrFin);
    }

    TOUSegment createTOUSegment(byte[] data, short sequenceNumber) {
        TCPSegment tcpSegment = new TCPSegment(data.length);
        tcpSegment.sequenceNumber(sequenceNumber);
        tcpSegment.data(data);
        tcpSegment.sourcePort(impl.localPort());
        tcpSegment.destinationPort(impl.port());
        return new TOUSegment(tcpSegment, impl.localAddress(), impl.address(), SEGMENT_TIMEOUT);
    }
}
