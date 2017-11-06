package ru.nsu.ccfit.bogush.tcp;

public enum TCPPacketType {
    ORDINARY, SYN, ACK, SYNACK;

    public static TCPPacketType typeOf (TCPPacket p) {
        if (!p.isSYN() && !p.isACK())
            return ORDINARY;
        if (!p.isSYN() &&  p.isACK())
            return ACK;
        if ( p.isSYN() && !p.isACK())
            return SYN;
        return SYNACK;
    }
}
