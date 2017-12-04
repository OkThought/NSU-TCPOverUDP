package ru.nsu.ccfit.bogush.tcp;

import static ru.nsu.ccfit.bogush.tcp.TCPSegment.ACK_BITMAP;
import static ru.nsu.ccfit.bogush.tcp.TCPSegment.SYN_BITMAP;
import static ru.nsu.ccfit.bogush.tcp.TCPSegment.FIN_BITMAP;

public enum TCPSegmentType {
    ORDINARY,
    ACK (ACK_BITMAP),
    SYN (SYN_BITMAP),
    FIN (FIN_BITMAP),
    SYNACK ((byte) (SYN_BITMAP | ACK_BITMAP)),
    FINACK ((byte) (FIN_BITMAP | ACK_BITMAP));

    private final byte typeBitMap;

    TCPSegmentType() {
        this.typeBitMap = 0;
    }

    TCPSegmentType(byte typeBitMap) {
        this.typeBitMap = typeBitMap;
    }



    @SuppressWarnings({"ConstantConditions"})
    public static TCPSegmentType typeOf (TCPSegment p) throws TCPUnknownSegmentTypeException {
        boolean a = p.isACK();
        boolean s = p.isSYN();
        boolean f = p.isFIN();
        if (!s && !a && !f) return ORDINARY;
        if (!s &&  a && !f) return ACK;
        if ( s && !a && !f) return SYN;
        if ( s &&  a && !f) return SYNACK;
        if (!s && !a &&  f) return FIN;
        if (!s &&  a &&  f) return FINACK;
        throw new TCPUnknownSegmentTypeException();
    }

    public byte toByte() {
        return typeBitMap;
    }
}
