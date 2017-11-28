package ru.nsu.ccfit.bogush.tou;

import ru.nsu.ccfit.bogush.tcp.TCPPacket;
import ru.nsu.ccfit.bogush.tcp.TCPPacketType;
import ru.nsu.ccfit.bogush.tcp.TCPUnknownPacketTypeException;

import java.net.InetAddress;

class TOUPacket {
    private TCPPacket tcpPacket;
    private InetAddress sourceAddress;
    private InetAddress destinationAddress;

    TOUPacket(TCPPacket tcpPacket, InetAddress sourceAddress, InetAddress destinationAddress) {
        this.tcpPacket = tcpPacket;
        this.sourceAddress = sourceAddress;
        this.destinationAddress = destinationAddress;
    }

    TOUPacket(TOUPacket other) {
        this.tcpPacket = new TCPPacket(other.tcpPacket);
        this.sourceAddress = other.sourceAddress;
        this.destinationAddress = other.destinationAddress;
    }

    void tcpPacket(TCPPacket tcpPacket) {
        this.tcpPacket = tcpPacket;
    }

    TCPPacket tcpPacket() {
        return tcpPacket;
    }

    void type(TCPPacketType type) {
        tcpPacket.flags(type.toByte());
    }

    TCPPacketType type() throws TCPUnknownPacketTypeException {
        return TCPPacketType.typeOf(tcpPacket);
    }

    void typeByte(byte typeByte) {
        tcpPacket.flags(typeByte);
    }

    byte typeByte() {
        return tcpPacket.flags();
    }

    void sequenceNumber(short sequenceNumber) {
        tcpPacket.sequenceNumber(sequenceNumber);
    }

    short sequenceNumber() {
        return tcpPacket.sequenceNumber();
    }

    public void ackNumber(short ackNumber) {
        tcpPacket.ackNumber(ackNumber);
    }

    public short ackNumber() {
        return tcpPacket.ackNumber();
    }

    public int sequenceAndAckNumbers() {
        return tcpPacket.sequenceAndAckNumbers();
    }

    void sourceAddress(InetAddress sourceAddress) {
        this.sourceAddress = sourceAddress;
    }

    InetAddress sourceAddress() {
        return sourceAddress;
    }

    void sourcePort(short sourcePort) {
        tcpPacket.sourcePort(sourcePort);
    }

    int sourcePort() {
        return tcpPacket.sourcePort();
    }

    void destinationAddress(InetAddress destinationAddress) {
        this.destinationAddress = destinationAddress;
    }

    InetAddress destinationAddress() {
        return destinationAddress;
    }

    void destinationPort(short destinationPort) {
        tcpPacket.destinationPort(destinationPort);
    }

    int destinationPort() {
        return tcpPacket.destinationPort();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TOUPacket other = (TOUPacket) o;

        if (tcpPacket == null ? other.tcpPacket != null : other.tcpPacket == null) return false;

        if (sourcePort() != other.sourcePort() ||
            destinationPort() != other.destinationPort() ||
            sequenceNumber() != other.sequenceNumber()) return false;

        if (destinationAddress != null ? !destinationAddress.equals(other.destinationAddress) : other.destinationAddress != null)
            return false;

        return sourceAddress != null ? sourceAddress.equals(other.sourceAddress) : other.sourceAddress == null;
    }

    @Override
    public int hashCode() {
        int result = tcpPacket != null ? tcpPacket.sequenceNumber(): 0;
        result = 31 * result + (tcpPacket != null ? tcpPacket.sourcePort() : 0);
        result = 31 * result + (tcpPacket != null ? tcpPacket.destinationPort() : 0);
        result = 31 * result + (sourceAddress != null ? sourceAddress.hashCode() : 0);
        result = 31 * result + (destinationAddress != null ? destinationAddress.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "TOUPacket <" + "tcpPacket: " + tcpPacket +
                " source: " + sourceAddress +
                " destination: " + destinationAddress +
                '>';
    }
}
