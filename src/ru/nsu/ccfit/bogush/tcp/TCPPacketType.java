package ru.nsu.ccfit.bogush.tcp;

import static ru.nsu.ccfit.bogush.tcp.TCPPacket.ACK_BITMAP;
import static ru.nsu.ccfit.bogush.tcp.TCPPacket.SYN_BITMAP;
import static ru.nsu.ccfit.bogush.tcp.TCPPacket.FIN_BITMAP;

public enum TCPPacketType {
    ORDINARY,
    ACK (ACK_BITMAP),
    SYN (SYN_BITMAP),
    FIN (FIN_BITMAP),
    SYNACK ((byte) (SYN_BITMAP | ACK_BITMAP)),
    FINACK ((byte) (FIN_BITMAP | ACK_BITMAP));

    private final byte typeBitMap;

    TCPPacketType() {
        this.typeBitMap = 0;
    }

    TCPPacketType(byte typeBitMap) {
        this.typeBitMap = typeBitMap;
    }



    @SuppressWarnings({"ConstantConditions"})
    public static TCPPacketType typeOf (TCPPacket p) throws TCPUnknownPacketTypeException {
        boolean a = p.isACK();
        boolean s = p.isSYN();
        boolean f = p.isFIN();
        if (!s && !a && !f) return ORDINARY;
        if (!s &&  a && !f) return ACK;
        if ( s && !a && !f) return SYN;
        if ( s &&  a && !f) return SYNACK;
        if (!s && !a &&  f) return FIN;
        if (!s &&  a &&  f) return FINACK;
        throw new TCPUnknownPacketTypeException();
    }

    public byte toByte() {
        return typeBitMap;
    }
}
