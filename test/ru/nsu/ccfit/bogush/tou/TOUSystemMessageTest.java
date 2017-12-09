package ru.nsu.ccfit.bogush.tou;


import org.junit.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;
import static ru.nsu.ccfit.bogush.tcp.TCPSegmentType.*;

public class TOUSystemMessageTest {
    private static InetAddress IP_A;
    private static InetAddress IP_B;
    private static final int PORT_A = 59595;
    private static final int PORT_B = 59596;

    static {
        try {
            IP_A = InetAddress.getByName("192.168.0.1");
            IP_B = InetAddress.getByName("192.168.0.2");
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    private static final TOUSystemMessage syn = TOUFactory.createSYNorFIN(SYN, IP_A, PORT_A, IP_B, PORT_B);
    private static final TOUSystemMessage synack = TOUFactory.createSYNACKorFINACK(IP_B, PORT_B, syn);
    private static final TOUSystemMessage ack2synack = TOUFactory.createACK(synack);
    private static final TOUSystemMessage fin = TOUFactory.createSYNorFIN(FIN, IP_A, PORT_A, IP_B, PORT_B);
    private static final TOUSystemMessage finack = TOUFactory.createSYNACKorFINACK(IP_B, PORT_B, fin);
    private static final TOUSystemMessage ack2finack = TOUFactory.createACK(finack);
    private static final List<TOUSystemMessage> all = Arrays.asList(syn, synack, ack2synack, fin, finack, ack2finack);

    @Test
    public void equalsSelf() throws Exception {
        assertEquals(syn, new TOUSystemMessage(syn));
        assertEquals(synack, new TOUSystemMessage(synack));
        assertEquals(ack2synack, new TOUSystemMessage(ack2synack));

        assertEquals(fin, new TOUSystemMessage(fin));
        assertEquals(finack, new TOUSystemMessage(finack));
        assertEquals(ack2finack, new TOUSystemMessage(ack2finack));
    }

    @Test
    public void equalsSYN() throws Exception {
        TOUSystemMessage s;

        s = new TOUSystemMessage(SYN);
        s.destinationAddress(syn.destinationAddress);
        s.destinationPort(syn.destinationPort());
        assertEquals(syn, s);
    }

    @Test
    public void equalsSYNACK() throws Exception {
        TOUSystemMessage s;

        s = new TOUSystemMessage(SYNACK);
        s.destinationAddress(synack.destinationAddress);
        s.destinationPort(synack.destinationPort());
        s.ackNumber(synack.ackNumber());
        assertEquals(synack, s);
    }

    @Test
    public void equalsACK2SYNACK() throws Exception {
        TOUSystemMessage s;

        s = new TOUSystemMessage(ACK);
        s.destinationAddress(ack2synack.destinationAddress);
        s.destinationPort(ack2synack.destinationPort());
        s.ackNumber(ack2synack.ackNumber());
        s.sequenceNumber(ack2synack.sequenceNumber());
        assertEquals(ack2synack, s);
    }

    @Test
    public void equalsFIN() throws Exception {
        TOUSystemMessage s;

        s = new TOUSystemMessage(FIN);
        s.destinationAddress(fin.destinationAddress);
        s.destinationPort(fin.destinationPort());
        assertEquals(fin, s);
    }

    @Test
    public void equalsFINACK() throws Exception {
        TOUSystemMessage s;

        s = new TOUSystemMessage(FINACK);
        s.destinationAddress(finack.destinationAddress);
        s.destinationPort(finack.destinationPort());
        s.ackNumber(finack.ackNumber());
        assertEquals(finack, s);
    }

    @Test
    public void equalsACK2FINACK() throws Exception {
        TOUSystemMessage s;

        s = new TOUSystemMessage(ACK);
        s.destinationAddress(ack2finack.destinationAddress);
        s.destinationPort(ack2finack.destinationPort());
        s.ackNumber(ack2finack.ackNumber());
        s.sequenceNumber(ack2finack.sequenceNumber());
        assertEquals(ack2finack, s);
    }

    @Test
    public void shouldntBeEqual() throws Exception {
        for (TOUSystemMessage sm1 : all) {
            for (TOUSystemMessage sm2 : all) {
                if (sm1 != sm2) {
                    assertNotEquals(sm1, sm2);
                }
            }
        }
    }
}