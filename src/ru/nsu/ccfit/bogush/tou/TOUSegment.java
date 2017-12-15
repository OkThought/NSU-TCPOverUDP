package ru.nsu.ccfit.bogush.tou;

import ru.nsu.ccfit.bogush.tcp.TCPSegment;
import ru.nsu.ccfit.bogush.tcp.TCPSegmentType;
import ru.nsu.ccfit.bogush.tcp.TCPUnknownSegmentTypeException;

import java.net.InetAddress;

class TOUSegment {
    TCPSegment tcpSegment;
    InetAddress sourceAddress;
    InetAddress destinationAddress;
    private long timeExpires = 0;
    private long timeout;

    TOUSegment(TOUSegment other) {
        this(new TCPSegment(other.tcpSegment), other.sourceAddress, other.destinationAddress);
    }

    TOUSegment(TCPSegment tcpSegment, InetAddress sourceAddress, InetAddress destinationAddress) {
        this(tcpSegment, sourceAddress, destinationAddress, 0);
    }

    TOUSegment(TCPSegment tcpSegment, InetAddress sourceAddress, InetAddress destinationAddress, long timeout) {
        this.tcpSegment = tcpSegment;
        this.sourceAddress = sourceAddress;
        this.destinationAddress = destinationAddress;
        this.timeout = timeout;
    }

    void tcpSegment(TCPSegment tcpSegment) {
        this.tcpSegment = tcpSegment;
    }

    TCPSegment tcpSegment() {
        return tcpSegment;
    }

    void type(TCPSegmentType type) {
        tcpSegment.flags(type.toByte());
    }

    TCPSegmentType type() throws TCPUnknownSegmentTypeException {
        return TCPSegmentType.typeOf(tcpSegment);
    }

    void typeByte(byte typeByte) {
        tcpSegment.flags(typeByte);
    }

    byte typeByte() {
        return tcpSegment.flags();
    }

    void sequenceNumber(short sequenceNumber) {
        tcpSegment.sequenceNumber(sequenceNumber);
    }

    short sequenceNumber() {
        return tcpSegment.sequenceNumber();
    }

    void ackNumber(short ackNumber) {
        tcpSegment.ackNumber(ackNumber);
    }

    short ackNumber() {
        return tcpSegment.ackNumber();
    }

    int sequenceAndAckNumbers() {
        return tcpSegment.sequenceAndAckNumbers();
    }

    void sourceAddress(InetAddress sourceAddress) {
        this.sourceAddress = sourceAddress;
    }

    InetAddress sourceAddress() {
        return sourceAddress;
    }

    void sourcePort(int sourcePort) {
        tcpSegment.sourcePort(sourcePort);
    }

    int sourcePort() {
        return tcpSegment.sourcePort();
    }

    void destinationAddress(InetAddress destinationAddress) {
        this.destinationAddress = destinationAddress;
    }

    InetAddress destinationAddress() {
        return destinationAddress;
    }

    void destinationPort(int destinationPort) {
        tcpSegment.destinationPort(destinationPort);
    }

    int destinationPort() {
        return tcpSegment.destinationPort();
    }

    boolean needsResending() {
        long currentTime = System.currentTimeMillis();
        if (timeExpires == 0) {
            timeExpires = currentTime + timeout;
        }
        return timeExpires >= currentTime;
    }

    void setTimeout(long timeout) {
        timeExpires = 0;
        this.timeout = timeout;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TOUSegment other = (TOUSegment) o;

        if (tcpSegment == null ? other.tcpSegment != null : other.tcpSegment == null) return false;

        if (sourcePort() != other.sourcePort() ||
            destinationPort() != other.destinationPort() ||
            sequenceNumber() != other.sequenceNumber()) return false;

        if (destinationAddress != null ? !destinationAddress.equals(other.destinationAddress) : other.destinationAddress != null)
            return false;

        return sourceAddress != null ? sourceAddress.equals(other.sourceAddress) : other.sourceAddress == null;
    }

    @Override
    public int hashCode() {
        int result = tcpSegment != null ? tcpSegment.sequenceNumber(): 0;
        result = 31 * result + (tcpSegment != null ? tcpSegment.sourcePort() : 0);
        result = 31 * result + (tcpSegment != null ? tcpSegment.destinationPort() : 0);
        result = 31 * result + (sourceAddress != null ? sourceAddress.hashCode() : 0);
        result = 31 * result + (destinationAddress != null ? destinationAddress.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return String.format("%16s[%s seq: %5d ack: %5d from %s:%5d to %s:%5d data offset: %3d size: %3d bytes]",
                TOUSegment.class.getSimpleName(), tcpSegment.typeByteToString(), sequenceNumber(), ackNumber(),
                sourceAddress, sourcePort(), destinationAddress, destinationPort(),
                tcpSegment.dataOffset(), tcpSegment.size());
    }
}
