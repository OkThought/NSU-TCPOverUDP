package ru.nsu.ccfit.bogush.tou;

import ru.nsu.ccfit.bogush.tcp.TCPSegment;

abstract class TOUConstants {
    static final int MAX_DATA_SIZE = 1024; // bytes
    static final int MAX_PACKET_SIZE = MAX_DATA_SIZE + TCPSegment.HEADER_SIZE;
    static final int QUEUE_CAPACITY = 512;
    static final int DATA_PACKET_POLL_TIMEOUT = 1;
    static final int SYSTEM_PACKET_POLL_TIMEOUT = 1;
    public static final int UDP_RECV_TIMEOUT = 1;
}
