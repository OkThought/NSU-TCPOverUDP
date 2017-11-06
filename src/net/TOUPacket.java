package net;

import java.net.InetAddress;

class TOUPacket {
    private TCPPacket tcpPacket;
    private InetAddress address;
    private short sequenceNumber;

    TOUPacket(TCPPacket tcpPacket, InetAddress address, short sequenceNumber) {
        this.tcpPacket = tcpPacket;
        this.address = address;
        this.sequenceNumber = sequenceNumber;
    }

    public TCPPacket getTcpPacket() {
        return tcpPacket;
    }

    public short getSequenceNumber() {
        return sequenceNumber;
    }

    public InetAddress getAddress() {
        return address;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TOUPacket touPacket = (TOUPacket) o;

        return sequenceNumber == touPacket.sequenceNumber;
    }

    @Override
    public int hashCode() {
        return sequenceNumber;
    }
}
