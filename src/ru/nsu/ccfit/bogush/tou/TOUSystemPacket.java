package ru.nsu.ccfit.bogush.tou;

import ru.nsu.ccfit.bogush.tcp.TCPPacketType;

import java.net.InetAddress;

public class TOUSystemPacket {
    private TCPPacketType type;
    private InetAddress sourceAddress;
    private int sourcePort;
    private InetAddress destinationAddress;
    private int destinationPort;
    private short sequenceNumber;
    private short ackNumber;


    public TOUSystemPacket(TCPPacketType type,
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

    public TOUSystemPacket(TCPPacketType type,
                           InetAddress sourceAddress, int sourcePort,
                           InetAddress destinationAddress, int destinationPort,
                           int systemMessage) {
        this.type = type;
        this.sourceAddress = sourceAddress;
        this.sourcePort = sourcePort;
        this.destinationAddress = destinationAddress;
        this.destinationPort = destinationPort;
        systemMessage(systemMessage);
    }

    public void type(TCPPacketType type) {
        this.type = type;
    }

    public TCPPacketType type() {
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
        this.sequenceNumber = (short) (systemMessage >> 16);
        this.ackNumber = (short) (systemMessage & 0x0000ffff);
    }

    public int systemMessage() {
        return (sequenceNumber << 16) | ackNumber;
    }
}
