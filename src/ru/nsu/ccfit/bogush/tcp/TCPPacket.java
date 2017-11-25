package ru.nsu.ccfit.bogush.tcp;

import java.util.Arrays;

public class TCPPacket {
    public TCPPacket (int size) {
        if (size < HEADER_SIZE)
            throw new IllegalArgumentException("size < HEADER_SIZE");

        bytes = new byte[size];
        Arrays.fill(bytes, (byte) 0);
    }

    public TCPPacket (TCPPacket other) {
        this(other.bytes);
    }

    public TCPPacket (byte[] bytes) {
        this.bytes = bytes;
    }

    public void setACK (boolean value) {
        setFlag(ACK, value);
    }

    public boolean isACK () {
        return getFlag(ACK);
    }

    public void setSYN (boolean value) {
        setFlag(SYN, value);
    }

    public boolean isSYN () {
        return getFlag(SYN);
    }

    public void setFIN (boolean value) {
        setFlag(FIN, value);
    }

    public boolean isFIN () {
        return getFlag(FIN);
    }

    public void data(byte[] data) {
        System.arraycopy(data, 0, this.bytes, dataOffset(), data.length);
    }

    public byte[] data() {
        return Arrays.copyOfRange(bytes, dataOffset(), bytes.length);
    }

    public void dataOffset(short dataOffset) {
        if (dataOffset < DATA_OFFSET_MIN)
            throw new IllegalArgumentException("data offset is too small");

        setShort(DATA_OFFSET_POSITION, dataOffset);
    }

    public  short dataOffset() {
        return getShort(bytes, DATA_OFFSET_POSITION);
    }

    public void sourcePort(short sourcePort) {
        setShort(SOURCE_PORT_POSITION, sourcePort);
    }

    public short sourcePort() {
        return getShort(bytes, SOURCE_PORT_POSITION);
    }

    public void destinationPort(short destinationPort) {
        setShort(DESTINATION_PORT_POSITION, destinationPort);
    }

    public short destinationPort() {
        return getShort(bytes, DESTINATION_PORT_POSITION);
    }

    public void sequenceNumber(short sequenceNumber) {
        setShort(SEQUENCE_NUMBER_POSITION, sequenceNumber);
    }

    public short sequenceNumber() {
        return getShort(bytes, SEQUENCE_NUMBER_POSITION);
    }

    public void ackNumber(short ackNumber) {
        setShort(ACK_NUMBER_POSITION, ackNumber);
    }

    public short ackNumber() {
        return getShort(bytes, ACK_NUMBER_POSITION);
    }

    public int sequenceAndAckNumbers() {
        return getInt(bytes, SEQUENCE_NUMBER_POSITION);
    }

    public byte[] bytes() {
        return bytes;
    }

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

    public static final int HEADER_SIZE = DATA_OFFSET_MIN; // bytes;

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
}
