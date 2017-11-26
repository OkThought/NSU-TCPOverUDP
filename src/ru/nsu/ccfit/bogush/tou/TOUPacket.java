package ru.nsu.ccfit.bogush.tou;

import ru.nsu.ccfit.bogush.tcp.TCPPacket;

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

    TOUPacket(TOUPacket other) {
        this.tcpPacket = other.tcpPacket;
        this.address = other.address;
        this.sequenceNumber = other.sequenceNumber;
    }

    public void tcpPacket(TCPPacket tcpPacket) {
        this.tcpPacket = tcpPacket;
    }

    TCPPacket tcpPacket() {
        return tcpPacket;
    }

    public void sequenceNumber(short sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    short sequenceNumber() {
        return sequenceNumber;
    }

    public void address(InetAddress address) {
        this.address = address;
    }

    InetAddress address() {
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
