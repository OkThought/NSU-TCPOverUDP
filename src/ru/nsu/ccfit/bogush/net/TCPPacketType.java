package ru.nsu.ccfit.bogush.net;

public enum TCPPacketType {
    ORDINARY, SYN, ACK, SYNACK;

    static TCPPacketType typeOf(TCPPacket p) {
        if (!p.isSYN() && !p.isACK())
            return ORDINARY;
        if (!p.isSYN() &&  p.isACK())
            return ACK;
        if ( p.isSYN() && !p.isACK())
            return SYN;
        return SYNACK;
    }
}
