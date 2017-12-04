package ru.nsu.ccfit.bogush.tou;

import ru.nsu.ccfit.bogush.tcp.TCPSegment;
import ru.nsu.ccfit.bogush.tcp.TCPSegmentType;
import ru.nsu.ccfit.bogush.tcp.TCPUnknownSegmentTypeException;

import java.net.InetAddress;

class TOUSegment {
    private TCPSegment tcpSegment;
    private InetAddress sourceAddress;
    private InetAddress destinationAddress;
    private long timeExpires;

    TOUSegment(TCPSegment tcpSegment, InetAddress sourceAddress, InetAddress destinationAddress) {
        this.tcpSegment = tcpSegment;
        this.sourceAddress = sourceAddress;
        this.destinationAddress = destinationAddress;
    }

    TOUSegment(TOUSegment other) {
        this.tcpSegment = new TCPSegment(other.tcpSegment);
        this.sourceAddress = other.sourceAddress;
        this.destinationAddress = other.destinationAddress;
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

    public void ackNumber(short ackNumber) {
        tcpSegment.ackNumber(ackNumber);
    }

    public short ackNumber() {
        return tcpSegment.ackNumber();
    }

    public int sequenceAndAckNumbers() {
        return tcpSegment.sequenceAndAckNumbers();
    }

    void sourceAddress(InetAddress sourceAddress) {
        this.sourceAddress = sourceAddress;
    }

    InetAddress sourceAddress() {
        return sourceAddress;
    }

    void sourcePort(short sourcePort) {
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

    void destinationPort(short destinationPort) {
        tcpSegment.destinationPort(destinationPort);
    }

    int destinationPort() {
        return tcpSegment.destinationPort();
    }

    long timeExpires() {
        return timeExpires;
    }

    void timeExpires(long newValue) {
        timeExpires = newValue;
    }

    boolean needsResending() {
        return true;
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
        return "TOUSegment <" + "tcpSegment: " + tcpSegment +
                " source: " + sourceAddress +
                " destination: " + destinationAddress +
                '>';
    }
}
