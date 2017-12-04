package ru.nsu.ccfit.bogush.tcp;

import org.junit.Test;

import java.nio.ByteBuffer;

import static org.junit.Assert.*;
import static ru.nsu.ccfit.bogush.tcp.TCPSegment.*;

public class TCPSegmentTest {
    private static final TCPSegment empty = new TCPSegment();
    private static final TCPSegment syn = new TCPSegment();
    private static final TCPSegment ack = new TCPSegment();
    private static final TCPSegment synack = new TCPSegment();
    private static final TCPSegment fin = new TCPSegment();
    private static final TCPSegment finack = new TCPSegment();
    private static final String customString = "Some information";
    private static final byte[] customData = customString.getBytes();
    private static final TCPSegment customPacket = new TCPSegment(customData.length);

    static {
        syn.setSYN(true);
        synack.setSYN(true);
        ack.setACK(true);
        synack.setACK(true);
        finack.setACK(true);
        finack.setFIN(true);
        fin.setFIN(true);

        customPacket.data(customData);
    }

    @Test
    public void setACK() throws Exception {
        TCPSegment p = new TCPSegment();
        assertEquals(false, p.isACK());
        p.setACK(true);
        assertEquals(true, p.isACK());
        p.setACK(false);
        assertEquals(false, p.isACK());
        p.setSYN(true);
        assertEquals(false, p.isACK());
        p.setFIN(true);
        assertEquals(false, p.isACK());
        p.setSYN(false);
        assertEquals(false, p.isACK());
        p.setFIN(false);
        assertEquals(false, p.isACK());
    }

    @Test
    public void setSYN() throws Exception {
        TCPSegment p = new TCPSegment();
        assertEquals(false, p.isSYN());
        p.setSYN(true);
        assertEquals(true, p.isSYN());
        p.setSYN(false);
        assertEquals(false, p.isSYN());
        p.setACK(true);
        assertEquals(false, p.isSYN());
        p.setFIN(true);
        assertEquals(false, p.isSYN());
        p.setACK(false);
        assertEquals(false, p.isSYN());
        p.setFIN(false);
        assertEquals(false, p.isSYN());
    }

    @Test
    public void setFIN() throws Exception {
        TCPSegment p = new TCPSegment();
        assertEquals(false, p.isFIN());
        p.setFIN(true);
        assertEquals(true, p.isFIN());
        p.setFIN(false);
        assertEquals(false, p.isFIN());
        p.setSYN(true);
        assertEquals(false, p.isFIN());
        p.setACK(true);
        assertEquals(false, p.isFIN());
        p.setSYN(false);
        assertEquals(false, p.isFIN());
        p.setACK(false);
        assertEquals(false, p.isFIN());
    }

    @Test
    public void isACK() throws Exception {
        assertEquals(true, ack.isACK());
        assertEquals(true, synack.isACK());
        assertEquals(true, finack.isACK());
        assertEquals(false, syn.isACK());
        assertEquals(false, fin.isACK());
        assertEquals(false, empty.isACK());
    }

    @Test
    public void isSYN() throws Exception {
        assertEquals(true, syn.isSYN());
        assertEquals(true, synack.isSYN());
        assertEquals(false, ack.isSYN());
        assertEquals(false, fin.isSYN());
        assertEquals(false, finack.isSYN());
        assertEquals(false, empty.isSYN());
    }

    @Test
    public void isFIN() throws Exception {
        assertEquals(true, fin.isFIN());
        assertEquals(true, finack.isFIN());
        assertEquals(false, syn.isFIN());
        assertEquals(false, synack.isFIN());
        assertEquals(false, ack.isFIN());

    }

    @Test
    public void flags() throws Exception {
        assertEquals(0, empty.flags());
        assertEquals(ACK_BITMAP, ack.flags());
        assertEquals(FIN_BITMAP, fin.flags());
        assertEquals(SYN_BITMAP, syn.flags());
        assertEquals(SYN_BITMAP | ACK_BITMAP, synack.flags());
        assertEquals(FIN_BITMAP | ACK_BITMAP, finack.flags());
        TCPSegment p = new TCPSegment();
        assertEquals(0, p.flags());
        p.flags(ACK_BITMAP);
        assertEquals(ACK_BITMAP, p.flags());
        p.flags(SYN_BITMAP);
        assertEquals(SYN_BITMAP, p.flags());
        p.flags(FIN_BITMAP);
        assertEquals(FIN_BITMAP, p.flags());
    }

    @Test
    public void data() throws Exception {
        assertNotNull(empty.data());
        assertEquals(0, empty.data().length);

        assertNotNull(customPacket.data());
        assertArrayEquals(customData, customPacket.data());

        boolean wasException = false;
        try {
            customPacket.data((customString + customString).getBytes());
        } catch (ArrayIndexOutOfBoundsException e) {
            wasException = true;
        }
        assertTrue(wasException);
        assertNotNull(customPacket.data());
        assertArrayEquals(customData, customPacket.data());
    }

    @Test
    public void dataOffset() throws Exception {
        assertEquals(empty.dataOffset(), HEADER_SIZE);
        assertTrue(customPacket.dataOffset() >= HEADER_SIZE);
    }

    @Test
    public void sourcePort() throws Exception {
        TCPSegment p = new TCPSegment();
        assertEquals(0, p.sourcePort());
        p.sourcePort((short) 1984);
        assertEquals(1984, p.sourcePort());
        assertEquals(0, p.destinationPort());
        assertEquals(0, p.sequenceNumber());
        assertEquals(0, p.ackNumber());
        assertEquals(0, p.sequenceAndAckNumbers());
    }

    @Test
    public void destinationPort() throws Exception {
        TCPSegment p = new TCPSegment();
        assertEquals(0, p.destinationPort());
        p.destinationPort((short) 1984);
        assertEquals(1984, p.destinationPort());
        assertEquals(0, p.sourcePort());
        assertEquals(0, p.sequenceNumber());
        assertEquals(0, p.ackNumber());
        assertEquals(0, p.sequenceAndAckNumbers());
    }

    @Test
    public void bigPorts() throws Exception {
        TCPSegment p = new TCPSegment();
        p.sourcePort(50000);
        p.destinationPort(50000);
        assertEquals(50000, p.sourcePort());
        assertEquals(50000, p.destinationPort());
    }

    @Test
    public void sequenceNumber() throws Exception {
        TCPSegment p = new TCPSegment();
        assertEquals(0, p.sequenceNumber());
        p.sequenceNumber((short) 1984);
        assertEquals(1984, p.sequenceNumber());
        assertEquals(0, p.sourcePort());
        assertEquals(0, p.destinationPort());
        assertEquals(0, p.ackNumber());
    }

    @Test
    public void ackNumber() throws Exception {
        TCPSegment p = new TCPSegment();
        assertEquals(0, p.ackNumber());
        p.ackNumber((short) 1984);
        assertEquals(1984, p.ackNumber());
        assertEquals(0, p.sourcePort());
        assertEquals(0, p.destinationPort());
        assertEquals(0, p.sequenceNumber());
    }

    @Test
    public void sequenceAndAckNumbers() throws Exception {
        TCPSegment p = new TCPSegment();
        assertEquals(0, p.sequenceAndAckNumbers());
        int i = ByteBuffer.allocate(4).putShort((short) 1984).putShort((short) 4891).getInt(0);
        p.sequenceAndAckNumbers(i);
        assertEquals(i, p.sequenceAndAckNumbers());
        assertEquals(1984, p.sequenceNumber());
        assertEquals(4891, p.ackNumber());
        assertEquals(0, p.sourcePort());
        assertEquals(0, p.destinationPort());
    }

    @Test
    public void bytes() throws Exception {
        assertNotNull(empty.bytes());
        assertEquals(HEADER_SIZE, empty.bytes().length);
    }

    @Test
    public void capacity() throws Exception {
        assertEquals(0, empty.dataSize());
        assertEquals(customPacket.bytes().length - customPacket.dataOffset(), customPacket.dataSize());
    }
}