package ru.nsu.ccfit.bogush.net;

import java.util.Arrays;

class TCPPacket {
    private static final int SOURCE_PORT_POSITION           = 0;
    private static final int DESTINATION_PORT_POSITION      = SOURCE_PORT_POSITION + 2;
    private static final int DATA_OFFSET_POSITION           = DESTINATION_PORT_POSITION + 2;
    private static final int SEQUENCE_NUMBER_POSITION       = DATA_OFFSET_POSITION + 2;
    private static final int ACK_NUMBER_POSITION            = SEQUENCE_NUMBER_POSITION + 2;
    private static final int FLAGS_POSITION                 = ACK_NUMBER_POSITION + 2;
    private static final int ID_POSITION                    = FLAGS_POSITION + 1;
    private static final int DATA_OFFSET_MIN                = ID_POSITION + 16;

    private static final byte ACK = (byte) 0b10000000;
    private static final byte SYN = (byte) 0b01000000;
    private static final byte FIN = (byte) 0b00100000;

    static final int HEADER_SIZE = DATA_OFFSET_MIN; // bytes;

    private byte[] bytes;

    private static byte byteAtPos (long x, int pos) {
        return (byte) (x >> 8 * pos);
    }

    private static byte byte7 (long x) {
        return (byte) (x >> 56);
    }

    private static byte byte6 (long x) {
        return (byte) (x >> 48);
    }

    private static byte byte5 (long x) {
        return (byte) (x >> 40);
    }

    private static byte byte4 (long x) {
        return (byte) (x >> 32);
    }

    private static byte byte3 (long x) {
        return (byte) (x >> 24);
    }

    private static byte byte2 (long x) {
        return (byte) (x >> 16);
    }

    private static byte byte1 (long x) {
        return (byte) (x >> 8);
    }

    private static byte byte0 (long x) {
        return (byte) (x     );
    }

    private static byte setFlagActive (byte b, byte flag) {
        return (byte) (b | flag);
    }

    private static byte setFlagInactive (byte b, byte flag) {
        return (byte) (b & ~flag);
    }

    private static long getLong (byte[] bytes, int at) {
        return  (long) (bytes[at    ]) << 56 +
                (long) (bytes[at + 1]) << 48 +
                (long) (bytes[at + 2]) << 40 +
                (long) (bytes[at + 3]) << 32 +
                (long) (bytes[at + 4]) << 24 +
                (long) (bytes[at + 5]) << 16 +
                (long) (bytes[at + 6]) <<  8 +
                (long) (bytes[at + 7])      ;
    }

    private static int getInt (byte[] bytes, int at) {
        return  (bytes[at    ] << 24) +
                (bytes[at + 1] << 16) +
                (bytes[at + 2] <<  8) +
                (bytes[at + 3]      );
    }

    private static short getShort (byte[] bytes, int at) {
        return (short) ((bytes[at] << 8) + bytes[at + 1]);
    }

    private static boolean getFlag (byte b, byte flag) {
        return (b & flag) != 0;
    }

    TCPPacket (int size) {
        if (size < HEADER_SIZE)
            throw new IllegalArgumentException("size < HEADER_SIZE");

        bytes = new byte[size];
        Arrays.fill(bytes, (byte) 0);
    }

    TCPPacket (byte[] bytes) {
        this.bytes = bytes;
    }

    private void setFlag (byte flag, boolean active) {
        if (active) {
            bytes[FLAGS_POSITION] = setFlagActive(bytes[FLAGS_POSITION], flag);
        } else {
            bytes[FLAGS_POSITION] = setFlagInactive(bytes[FLAGS_POSITION], flag);
        }
    }

    private boolean getFlag (byte flag) {
        return getFlag(bytes[FLAGS_POSITION], flag);
    }

    private void setLong (int at, long x) {
        bytes[at    ] = byte7(x);
        bytes[at + 1] = byte6(x);
        bytes[at + 2] = byte5(x);
        bytes[at + 3] = byte4(x);
        bytes[at + 4] = byte3(x);
        bytes[at + 5] = byte2(x);
        bytes[at + 6] = byte1(x);
        bytes[at + 7] = byte0(x);
    }

    private void setInt (int at, int x) {
        bytes[at    ] = byte3(x);
        bytes[at + 1] = byte2(x);
        bytes[at + 2] = byte1(x);
        bytes[at + 3] = byte0(x);
    }

    private void setShort (int at, short x) {
        bytes[at    ] = byte1(x);
        bytes[at + 1] = byte0(x);
    }

    boolean isACK () {
        return getFlag(ACK);
    }

    void setACK (boolean value) {
        setFlag(ACK, value);
    }

    boolean isSYN () {
        return getFlag(SYN);
    }

    void setSYN (boolean value) {
        setFlag(SYN, value);
    }

    boolean isFIN () {
        return getFlag(FIN);
    }

    void setFIN (boolean value) {
        setFlag(FIN, value);
    }

    public void setData (byte[] data) {
        System.arraycopy(data, 0, this.bytes, getDataOffset(), data.length);
    }

    public byte[] getData () {
        return Arrays.copyOfRange(bytes, getDataOffset(), bytes.length);
    }

    public void setDataOffset (short dataOffset) {
        if (dataOffset < DATA_OFFSET_MIN)
            throw new IllegalArgumentException("data offset is too small");

        setShort(DATA_OFFSET_POSITION, dataOffset);
    }

    private short getDataOffset () {
        return getShort(bytes, DATA_OFFSET_POSITION);
    }

    void setSourcePort (short sourcePort) {
        setShort(SOURCE_PORT_POSITION, sourcePort);
    }

    public short getSourcePort () {
        return getShort(bytes, SOURCE_PORT_POSITION);
    }

    void setDestinationPort (short destinationPort) {
        setShort(DESTINATION_PORT_POSITION, destinationPort);
    }

    short getDestinationPort () {
        return getShort(bytes, DESTINATION_PORT_POSITION);
    }

    void setSequenceNumber (short sequenceNumber) {
        setShort(SEQUENCE_NUMBER_POSITION, sequenceNumber);
    }

    short getSequenceNumber () {
        return getShort(bytes, SEQUENCE_NUMBER_POSITION);
    }

    int getAckSeq () {
        return getInt(bytes, SEQUENCE_NUMBER_POSITION);
    }

    void setAckNumber (short ackNumber) {
        setShort(ACK_NUMBER_POSITION, ackNumber);
    }

    short getAckNumber () {
        return getShort(bytes, ACK_NUMBER_POSITION);
    }

    byte[] getBytes () {
        return bytes;
    }
}
