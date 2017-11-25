package ru.nsu.ccfit.bogush.tou;

import ru.nsu.ccfit.bogush.tcp.TCPPacketType;

import java.net.InetAddress;

public class TOUSystemPacket {
    private TCPPacketType type;
    private InetAddress address;
    private int port;
    private int systemMessage;

    public TOUSystemPacket(TCPPacketType type, InetAddress address, int port, int systemMessage) {
        this.type = type;
        this.address = address;
        this.port = port;
        this.systemMessage = systemMessage;
    }

    public TCPPacketType type() {
        return type;
    }

    public void type(TCPPacketType type) {
        this.type = type;
    }

    public InetAddress address() {
        return address;
    }

    public void address(InetAddress address) {
        this.address = address;
    }

    public int port() {
        return port;
    }

    public void port(int port) {
        this.port = port;
    }

    public int systemMessage() {
        return systemMessage;
    }

    public void systemMessage(int systemMessage) {
        this.systemMessage = systemMessage;
    }
}
