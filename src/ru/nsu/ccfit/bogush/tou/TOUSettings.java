package ru.nsu.ccfit.bogush.tou;

import ru.nsu.ccfit.bogush.tcp.TCPPacket;

public abstract class TOUSettings {
    static final int TOU_SENDER_PORT = 49999;
    static final int TOU_RECEIVER_PORT = 49998;
    static final int MAX_DATA_SIZE = 1024; // bytes
    static final int MAX_PACKET_SIZE = MAX_DATA_SIZE + TCPPacket.HEADER_SIZE;
    static final int QUEUE_CAPACITY = 512;
    static final int DATA_PACKET_POLL_TIMEOUT = 1000;
    static final int SYSTEM_PACKET_POLL_TIMEOUT = 1000;
}
