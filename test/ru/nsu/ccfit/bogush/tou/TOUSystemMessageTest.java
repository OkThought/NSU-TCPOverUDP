package ru.nsu.ccfit.bogush.tou;


import org.junit.Test;
import ru.nsu.ccfit.bogush.tcp.TCPSegment;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;
import static ru.nsu.ccfit.bogush.tcp.TCPSegmentType.*;

public class TOUSystemMessageTest {
    private static InetAddress IP_A;
    private static InetAddress IP_B;
    private static InetAddress IP_C;
    private static final int PORT_A = 59595;
    private static final int PORT_B = 59596;

    static {
        try {
            IP_A = InetAddress.getByName("192.168.0.1");
            IP_B = InetAddress.getByName("192.168.0.2");
            IP_C = InetAddress.getByName("192.168.0.3");
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    private static final byte[] data = "data".getBytes();
    private static final TCPSegment dataSegment = new TCPSegment(data.length);
    static {
        dataSegment.data(data);
        dataSegment.sourcePort(PORT_A);
        dataSegment.destinationPort(PORT_A);
        dataSegment.sequenceNumber((short) 42);
    }
    private static final TOUSegment segment = TOUFactory.packIntoTOU(dataSegment, IP_A, IP_B);

    private static final TOUSystemMessage syn = TOUFactory.createSYNorFIN(SYN, IP_A, PORT_A, IP_B, PORT_B);
    private static final TOUSystemMessage synack = TOUFactory.createSYNACKorFINACK(IP_B, PORT_B, syn);
    private static final TOUSystemMessage ack2synack = TOUFactory.createACK(synack);
    private static final TOUSystemMessage fin = TOUFactory.createSYNorFIN(FIN, IP_A, PORT_A, IP_B, PORT_B);
    private static final TOUSystemMessage finack = TOUFactory.createSYNACKorFINACK(IP_B, PORT_B, fin);
    private static final TOUSystemMessage ack2finack = TOUFactory.createACK(finack);
    private static final TOUSystemMessage ack2segment = TOUFactory.createACK(segment);
    private static final List<TOUSystemMessage> all = Arrays.asList(syn, synack, ack2synack, fin, finack, ack2finack, ack2segment);

    @Test
    public void equalsSelf() throws Exception {
        assertTrue(syn.isEqualTo(new TOUSystemMessage(syn)));
        assertTrue(synack.isEqualTo(new TOUSystemMessage(synack)));
        assertTrue(ack2synack.isEqualTo(new TOUSystemMessage(ack2synack)));

        assertTrue(fin.isEqualTo(new TOUSystemMessage(fin)));
        assertTrue(finack.isEqualTo(new TOUSystemMessage(finack)));
        assertTrue(ack2finack.isEqualTo(new TOUSystemMessage(ack2finack)));
    }

    @Test
    public void equalsSYN() throws Exception {
        TOUSystemMessage s;

        s = new TOUSystemMessage(SYN);
        s.destinationAddress(syn.destinationAddress);
        s.destinationPort(syn.destinationPort());
        assertTrue(syn.isEqualTo(s));
        assertTrue(s.isEqualTo(syn));
    }

    @Test
    public void equalsSYNACK() throws Exception {
        TOUSystemMessage s;

        s = new TOUSystemMessage(SYNACK);
        s.destinationAddress(synack.destinationAddress);
        s.destinationPort(synack.destinationPort());
        s.ackNumber(synack.ackNumber());
        assertTrue(synack.isEqualTo(s));
        assertTrue(s.isEqualTo(synack));
    }

    @Test
    public void equalsACK2SYNACK() throws Exception {
        TOUSystemMessage s;

        s = new TOUSystemMessage(ACK);
        s.destinationAddress(ack2synack.destinationAddress);
        s.destinationPort(ack2synack.destinationPort());
        s.ackNumber(ack2synack.ackNumber());
        s.sequenceNumber(ack2synack.sequenceNumber());
        assertTrue(ack2synack.isEqualTo(s));
        assertTrue(s.isEqualTo(ack2synack));
    }

    @Test
    public void equalsFIN() throws Exception {
        TOUSystemMessage s;

        s = new TOUSystemMessage(FIN);
        s.destinationAddress(fin.destinationAddress);
        s.destinationPort(fin.destinationPort());
        assertTrue(fin.isEqualTo(s));
        assertTrue(s.isEqualTo(fin));
    }

    @Test
    public void equalsFINACK() throws Exception {
        TOUSystemMessage s;

        s = new TOUSystemMessage(FINACK);
        s.destinationAddress(finack.destinationAddress);
        s.destinationPort(finack.destinationPort());
        s.ackNumber(finack.ackNumber());
        assertTrue(finack.isEqualTo(s));
        assertTrue(s.isEqualTo(finack));
    }

    @Test
    public void equalsACK2FINACK() throws Exception {
        TOUSystemMessage s;

        s = new TOUSystemMessage(ACK);
        s.destinationAddress(ack2finack.destinationAddress);
        s.destinationPort(ack2finack.destinationPort());
        s.ackNumber(ack2finack.ackNumber());
        s.sequenceNumber(ack2finack.sequenceNumber());
        assertTrue(ack2finack.isEqualTo(s));
        assertTrue(s.isEqualTo(ack2finack));
    }

    @Test
    public void notEqualByType() throws Exception {
        for (TOUSystemMessage a : all) {
            for (TOUSystemMessage b : all) {
                if (a != b) {
                    assertFalse(a.isEqualTo(b));
                    assertFalse(b.isEqualTo(a));
                }
            }
        }
    }

    @Test
    public void notEqualSYNoFIN() throws Exception {
        TOUSystemMessage diffDestPort = new TOUSystemMessage(syn);
        diffDestPort.destinationPort(syn.destinationPort() + 1);

        TOUSystemMessage diffDestAddr = new TOUSystemMessage(syn);
        diffDestAddr.destinationAddress(IP_C);

        TOUSystemMessage[] different = new TOUSystemMessage[] {
                syn,
                diffDestPort,
                diffDestAddr
        };

        for (TOUSystemMessage a : different) {
            for (TOUSystemMessage b : different) {
                if (a != b) {
                    assertFalse(a + " == " + b, a.isEqualTo(b));
                    assertFalse(b + " == " + a, b.isEqualTo(a));
                }
            }
        }
    }

    @Test
    public void notEqualSYNoFINACK() throws Exception {
        for (TOUSystemMessage s : Arrays.asList(synack, finack)) {
            TOUSystemMessage diffSrc = new TOUSystemMessage(s);
            diffSrc.sourceAddress(IP_C);
            diffSrc.sourcePort(s.sourcePort() + 1);

            TOUSystemMessage diffDst = new TOUSystemMessage(s);
            diffDst.destinationAddress(IP_C);
            diffDst.destinationPort(s.destinationPort() + 1);

            TOUSystemMessage diffAck = new TOUSystemMessage(s);
            diffAck.ackNumber((short) (s.ackNumber() + 1));

            TOUSystemMessage[] different = new TOUSystemMessage[]{
                    s,
                    diffSrc,
                    diffDst,
                    diffAck
            };

            for (TOUSystemMessage a : different) {
                for (TOUSystemMessage b : different) {
                    if (a != b) {
                        assertFalse(a + " == " + b, a.isEqualTo(b));
                        assertFalse(b + " == " + a, b.isEqualTo(a));
                    }
                }
            }
        }
    }

    @Test
    public void notEqualACK2SYNoFINACK() throws Exception {
        for (TOUSystemMessage s : Arrays.asList(ack2synack, ack2finack)) {
            TOUSystemMessage diffSrc = new TOUSystemMessage(s);
            diffSrc.sourceAddress(IP_C);
            diffSrc.sourcePort(s.sourcePort() + 1);

            TOUSystemMessage diffDst = new TOUSystemMessage(s);
            diffDst.destinationAddress(IP_C);
            diffDst.destinationPort(s.destinationPort() + 1);

            TOUSystemMessage diffAck = new TOUSystemMessage(s);
            diffAck.ackNumber((short) (s.ackNumber() + 1));

            TOUSystemMessage diffSeq = new TOUSystemMessage(s);
            diffSeq.sequenceNumber((short) (s.sequenceNumber() + 1));

            TOUSystemMessage[] different = new TOUSystemMessage[]{
                    s,
                    diffSrc,
                    diffDst,
                    diffAck,
                    diffSeq
            };

            for (TOUSystemMessage a : different) {
                for (TOUSystemMessage b : different) {
                    if (a != b) {
                        assertFalse(a + " == " + b, a.isEqualTo(b));
                        assertFalse(b + " == " + a, b.isEqualTo(a));
                    }
                }
            }
        }
    }

}