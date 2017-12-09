package ru.nsu.ccfit.bogush.tcp;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class TCPSegment {
    private static final int SOURCE_PORT_POSITION           = 0;
    private static final int DESTINATION_PORT_POSITION      = SOURCE_PORT_POSITION + 2;
    private static final int DATA_OFFSET_POSITION           = DESTINATION_PORT_POSITION + 2;
    private static final int SEQUENCE_NUMBER_POSITION       = DATA_OFFSET_POSITION + 2;
    private static final int ACK_NUMBER_POSITION            = SEQUENCE_NUMBER_POSITION + 2;
    private static final int FLAGS_POSITION                 = ACK_NUMBER_POSITION + 2;
    private static final int DATA_OFFSET_MIN                = FLAGS_POSITION + 1;

    public static final byte ACK_BITMAP = (byte) 0b10000000;
    public static final byte SYN_BITMAP = (byte) 0b01000000;
    public static final byte FIN_BITMAP = (byte) 0b00100000;

    public static final int HEADER_SIZE = DATA_OFFSET_MIN; // bytes;

    private ByteBuffer bb;
    private byte[] bytes;

    public TCPSegment() {
        this(0);
    }

    public TCPSegment(int capacity) {
        this(new byte[capacity + HEADER_SIZE]);
        Arrays.fill(bytes, (byte) 0);
        dataOffset((short) DATA_OFFSET_MIN);
    }

    public TCPSegment(TCPSegment other) {
        this(other.bytes.clone());
    }

    public TCPSegment(byte[] bytes) {
        if (bytes.length < HEADER_SIZE) {
            throw new IllegalArgumentException("Byte array too small: " + bytes.length + " < " + HEADER_SIZE);
        }
        this.bytes = bytes;
        this.bb = ByteBuffer.wrap(this.bytes);
    }

    public TCPSegment(byte[] bytes, int offset, int length) {
        this.bytes = new byte[length];
        System.arraycopy(bytes, offset, this.bytes, 0, length);
        this.bb = ByteBuffer.wrap(this.bytes);
    }

    public void setACK (boolean value) {
        setFlag(ACK_BITMAP, value);
    }

    public boolean isACK () {
        return getFlag(ACK_BITMAP);
    }

    public void setSYN (boolean value) {
        setFlag(SYN_BITMAP, value);
    }

    public boolean isSYN () {
        return getFlag(SYN_BITMAP);
    }

    public void setFIN (boolean value) {
        setFlag(FIN_BITMAP, value);
    }

    public boolean isFIN () {
        return getFlag(FIN_BITMAP);
    }

    public byte flags() {
        return bytes[FLAGS_POSITION];
    }

    public void flags(byte flags) {
        bytes[FLAGS_POSITION] = flags;
    }

    public void header(byte[] header) {
        System.arraycopy(header, 0, bytes, 0, header.length);
    }

    public byte[] header() {
        return Arrays.copyOfRange(bytes, 0, dataOffset());
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

        bb.putShort(DATA_OFFSET_POSITION, dataOffset);
    }

    public  short dataOffset() {
        return bb.getShort(DATA_OFFSET_POSITION);
    }

    public void sourcePort(int sourcePort) {
        bb.putShort(SOURCE_PORT_POSITION, (short) sourcePort);
    }

    public int sourcePort() {
        return unsignedShortToInt(bb.getShort(SOURCE_PORT_POSITION));
    }

    public void destinationPort(int destinationPort) {
        bb.putShort(DESTINATION_PORT_POSITION, (short) destinationPort);
    }

    public int destinationPort() {
        return unsignedShortToInt(bb.getShort(DESTINATION_PORT_POSITION));
    }

    public void sequenceNumber(short sequenceNumber) {
        bb.putShort(SEQUENCE_NUMBER_POSITION, sequenceNumber);
    }

    public short sequenceNumber() {
        return bb.getShort(SEQUENCE_NUMBER_POSITION);
    }

    public void ackNumber(short ackNumber) {
        bb.putShort(ACK_NUMBER_POSITION, ackNumber);
    }

    public short ackNumber() {
        return bb.getShort(ACK_NUMBER_POSITION);
    }

    public int sequenceAndAckNumbers() {
        return bb.getInt(SEQUENCE_NUMBER_POSITION);
    }

    public void sequenceAndAckNumbers(int value) {
        bb.putInt(SEQUENCE_NUMBER_POSITION, value);
    }

    public byte[] bytes() {
        return bytes;
    }

    public int dataSize() {
        return bytes.length - dataOffset();
    }

    @Override
    public String toString() {
        String type;
        try {
            type = String.valueOf(TCPSegmentType.typeOf(this));
        } catch (TCPUnknownSegmentTypeException ignored) {
            type = "UNKNOWN";
        }
        return String.format("TCPSegment <%s sequence: %d ack: %d src: %d dst: %d data size: %d>",
                type, sequenceNumber(), ackNumber(), sourcePort(), destinationPort(), dataSize());
    }

    private static int unsignedShortToInt(short value) {
        return value & 0xffff;
    }

    private static byte setFlagActive (byte b, byte flag) {
        return (byte) (b | flag);
    }

    private static byte setFlagInactive (byte b, byte flag) {
        return (byte) (b & ~flag);
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
}
