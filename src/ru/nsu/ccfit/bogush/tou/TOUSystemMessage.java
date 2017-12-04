package ru.nsu.ccfit.bogush.tou;

import ru.nsu.ccfit.bogush.tcp.TCPSegment;
import ru.nsu.ccfit.bogush.tcp.TCPSegmentType;

import java.net.InetAddress;

public class TOUSystemMessage extends TOUSegment {
    private static final TCPSegmentType DEFAULT_TYPE = TCPSegmentType.ACK;

    private TCPSegmentType type;

    TOUSystemMessage(TOUSystemMessage that) {
        this(   that.type,
                that.sourceAddress, that.sourcePort(),
                that.destinationAddress, that.destinationPort(),
                that.sequenceNumber(), that.ackNumber());
    }

    TOUSystemMessage() {
        this(DEFAULT_TYPE);
    }

    TOUSystemMessage(TCPSegmentType type) {
        this(type, null, 0, null, 0, 0);
    }

    TOUSystemMessage(TCPSegmentType type,
                            InetAddress sourceAddress, int sourcePort,
                            InetAddress destinationAddress, int destinationPort,
                            int systemMessage) {
        this(type, sourceAddress, sourcePort, destinationAddress, destinationPort,
                sequencePart(systemMessage), ackPart(systemMessage));
    }

    TOUSystemMessage(TCPSegmentType type,
                            InetAddress sourceAddress, int sourcePort,
                            InetAddress destinationAddress, int destinationPort,
                            short sequenceNumber, short ackNumber) {
        super(new TCPSegment(), sourceAddress, destinationAddress);
        this.type = type;
        super.type(type);
        super.sourcePort(sourcePort);
        super.destinationPort(destinationPort);
        super.sequenceNumber(sequenceNumber);
        super.ackNumber(ackNumber);
    }

    private static short sequencePart(int systemMessage) {
        return (short) (systemMessage >> 16);
    }

    private static short ackPart(int systemMessage) {
        return (short) systemMessage;
    }

    @Override
    public void type(TCPSegmentType type) {
        this.type = type;
        super.type(type);
    }

    @Override
    public TCPSegmentType type() {
        return type;
    }

    public void destinationAddress(InetAddress destinationAddress) {
        this.destinationAddress = destinationAddress;
    }

    public InetAddress destinationAddress() {
        return destinationAddress;
    }


    public int systemMessage() {
        return tcpSegment.sequenceAndAckNumbers();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TOUSystemMessage that = (TOUSystemMessage) o;

        if (type != that.type) return false;
        if (sourcePort() != 0 && that.sourcePort() != 0 && sourcePort() != that.sourcePort()) return false;
        if (destinationPort() != 0 && that.destinationPort() != 0 && destinationPort() != that.destinationPort()) return false;
        if (sequenceNumber() != that.sequenceNumber()) return false;
        if (ackNumber() != that.ackNumber()) return false;
        if (sourceAddress != null && that.sourceAddress != null && !sourceAddress.equals(that.sourceAddress)) return false;
        return destinationAddress == null || that.destinationAddress == null || destinationAddress.equals(that.destinationAddress);
    }

    @Override
    public int hashCode() {
        int result = type != null ? type.hashCode() : 0;
        result = 31 * result + (sourceAddress != null ? sourceAddress.hashCode() : 0);
        result = 31 * result + sourcePort();
        result = 31 * result + (destinationAddress != null ? destinationAddress.hashCode() : 0);
        result = 31 * result + destinationPort();
        result = 31 * result + (int) sequenceNumber();
        result = 31 * result + (int) ackNumber();
        return result;
    }

    @Override
    public String toString() {
        return "TOUSystemMessage <" + type +
                " sequence: " + sequenceNumber() +
                " ack: " + ackNumber() +
                " source: " + sourceAddress + ":" + sourcePort() +
                " destination: " + destinationAddress + ":" + destinationPort() +
                '>';
    }
}
