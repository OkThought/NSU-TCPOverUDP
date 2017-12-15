package ru.nsu.ccfit.bogush.tou;

import ru.nsu.ccfit.bogush.tcp.TCPSegment;

abstract class TOUConstants {
    static final int MAX_DATA_SIZE = 1024; // bytes
    static final int MAX_PACKET_SIZE = MAX_DATA_SIZE + TCPSegment.HEADER_SIZE;
    static final int QUEUE_CAPACITY = 512;
    static final int SEGMENT_POLL_TIMEOUT = 300;
    static final int SEGMENT_TIMEOUT = 30;
    static final int SYSTEM_MESSAGE_TIMEOUT = 5 * SEGMENT_TIMEOUT;
    static final int UDP_RECV_TIMEOUT = 300;
}
