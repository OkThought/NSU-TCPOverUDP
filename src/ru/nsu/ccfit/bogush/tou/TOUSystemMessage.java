package ru.nsu.ccfit.bogush.tou;

import ru.nsu.ccfit.bogush.tcp.TCPSegmentType;

import java.net.InetAddress;

public class TOUSystemMessage {
    public static final TCPSegmentType DEFAULT_TYPE = TCPSegmentType.ORDINARY;

    private TCPSegmentType type;
    private InetAddress sourceAddress;
    private int sourcePort;
    private InetAddress destinationAddress;
    private int destinationPort;
    private short sequenceNumber;
    private short ackNumber;

    public TOUSystemMessage(TOUSystemMessage that) {
        this.type = that.type;
        this.sourceAddress = that.sourceAddress;
        this.sourcePort = that.sourcePort;
        this.destinationAddress = that.destinationAddress;
        this.destinationPort = that.destinationPort;
        this.sequenceNumber = that.sequenceNumber;
        this.ackNumber = that.ackNumber;
    }

    public TOUSystemMessage() {
        this(DEFAULT_TYPE);
    }

    public TOUSystemMessage(TCPSegmentType type) {
        this(type, null, 0, null, 0, 0);
    }

    public TOUSystemMessage(TCPSegmentType type,
                            InetAddress sourceAddress, int sourcePort,
                            InetAddress destinationAddress, int destinationPort,
                            int systemMessage) {
        this(type, sourceAddress, sourcePort, destinationAddress, destinationPort,
                sequencePart(systemMessage), ackPart(systemMessage));
    }

    public TOUSystemMessage(TCPSegmentType type,
                            InetAddress sourceAddress, int sourcePort,
                            InetAddress destinationAddress, int destinationPort,
                            short sequenceNumber, short ackNumber) {
        this.type = type;
        this.sourceAddress = sourceAddress;
        this.sourcePort = sourcePort;
        this.destinationAddress = destinationAddress;
        this.destinationPort = destinationPort;
        this.sequenceNumber = sequenceNumber;
        this.ackNumber = ackNumber;
    }

    private static short sequencePart(int systemMessage) {
        return (short) (systemMessage >> 16);
    }

    private static short ackPart(int systemMessage) {
        return (short) systemMessage;
    }

    public void type(TCPSegmentType type) {
        this.type = type;
    }

    public TCPSegmentType type() {
        return type;
    }

    public void sourceAddress(InetAddress sourceAddress) {
        this.sourceAddress = sourceAddress;
    }

    public InetAddress sourceAddress() {
        return sourceAddress;
    }

    public void sourcePort(int sourcePort) {
        this.sourcePort = sourcePort;
    }

    public int sourcePort() {
        return sourcePort;
    }

    public void destinationAddress(InetAddress destinationAddress) {
        this.destinationAddress = destinationAddress;
    }

    public InetAddress destinationAddress() {
        return destinationAddress;
    }

    public void destinationPort(int destinationPort) {
        this.destinationPort = destinationPort;
    }

    public int destinationPort() {
        return destinationPort;
    }

    public short sequenceNumber() {
        return sequenceNumber;
    }

    public void sequenceNumber(short sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public short ackNumber() {
        return ackNumber;
    }

    public void ackNumber(short ackNumber) {
        this.ackNumber = ackNumber;
    }

    public void systemMessage(int systemMessage) {
        this.sequenceNumber = sequencePart(systemMessage);
        this.ackNumber = ackPart(systemMessage);
    }

    public int systemMessage() {
        return (sequenceNumber << 16) | ackNumber;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TOUSystemMessage that = (TOUSystemMessage) o;

        if (type != that.type) return false;
        if (sourcePort != 0 && that.sourcePort != 0 && sourcePort != that.sourcePort) return false;
        if (destinationPort != 0 && that.destinationPort != 0 && destinationPort != that.destinationPort) return false;
        if (sequenceNumber != that.sequenceNumber) return false;
        if (ackNumber != that.ackNumber) return false;
        if (sourceAddress != null && that.sourceAddress != null && !sourceAddress.equals(that.sourceAddress)) return false;
        return destinationAddress == null || that.destinationAddress == null || destinationAddress.equals(that.destinationAddress);
    }

    @Override
    public int hashCode() {
        int result = type != null ? type.hashCode() : 0;
        result = 31 * result + (sourceAddress != null ? sourceAddress.hashCode() : 0);
        result = 31 * result + sourcePort;
        result = 31 * result + (destinationAddress != null ? destinationAddress.hashCode() : 0);
        result = 31 * result + destinationPort;
        result = 31 * result + (int) sequenceNumber;
        result = 31 * result + (int) ackNumber;
        return result;
    }

    @Override
    public String toString() {
        return "TOUSystemMessage <" + type +
                " sequence: " + sequenceNumber +
                " ack: " + ackNumber +
                " source: " + sourceAddress + ":" + sourcePort +
                " destination: " + destinationAddress + ":" + destinationPort +
                '>';
    }
}
