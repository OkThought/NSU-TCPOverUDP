package ru.nsu.ccfit.bogush.tou;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.nsu.ccfit.bogush.tcp.TCPPacket;
import ru.nsu.ccfit.bogush.tcp.TCPPacketType;
import ru.nsu.ccfit.bogush.tcp.TCPUnknownPacketTypeException;
import ru.nsu.ccfit.bogush.util.BlockingHashMap;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import static ru.nsu.ccfit.bogush.tcp.TCPPacketType.*;

class TOUReceiver extends Thread {
    static {
        TOULog4JUtils.initIfNotInitYet();
    }
    private static final Logger LOGGER = LogManager.getLogger("TOUReceiver");

    private final BlockingHashMap<TOUSystemPacket, byte[]> dataMap = new BlockingHashMap<>();
    private final BlockingHashMap<TOUSystemPacket, TOUSystemPacket> systemPacketMap = new BlockingHashMap<>();

    private DatagramSocket socket;
    private DatagramPacket packet;
    private TOUAckHandler ackHandler;

    private volatile boolean ignoreDataPackets = true;

    TOUReceiver (DatagramSocket socket, int packetSize) {
        super("TOUReceiver");
        LOGGER.traceEntry();

        this.socket = socket;
        packet = new DatagramPacket(new byte[packetSize], packetSize);

        LOGGER.traceExit();
    }

    @Override
    public void run () {
        LOGGER.traceEntry();

        try {
            while (!Thread.interrupted()) {
                LOGGER.trace("waiting to socket.receive");
                socket.receive(packet);
                LOGGER.debug("received {}", ()->TOULog4JUtils.toString(packet));
                TCPPacket tcpPacket = TOUPacketFactory.decapsulateTCP(packet);

                if (!ignoreDataPackets && tcpPacket.data().length > 0) {
                    LOGGER.trace("this packet contains data, put it in dataMap");
                    TOUSystemPacket key = new TOUSystemPacket();
                    key.sourceAddress(packet.getAddress());
                    key.sourcePort(tcpPacket.sourcePort());
                    key.sequenceNumber(tcpPacket.sequenceNumber());
                    LOGGER.debug("put data into dataMap at the key: {}", key);
                    dataMap.put(key, tcpPacket.data());
                }

                TCPPacketType packetType = TCPPacketType.typeOf(tcpPacket);
                if (packetType == ORDINARY) continue;

                InetAddress sourceAddress = packet.getAddress();
                int sourcePort = tcpPacket.sourcePort();
                int destinationPort = tcpPacket.destinationPort();
                InetAddress destinationAddress = socket.getLocalAddress();

                TOUSystemPacket systemPacket = new TOUSystemPacket(packetType,
                        sourceAddress, sourcePort,
                        destinationAddress, destinationPort,
                        tcpPacket.sequenceNumber(), tcpPacket.ackNumber());

                TOUSystemPacket key = new TOUSystemPacket(packetType);
                key.destinationAddress(destinationAddress);
                key.destinationPort(destinationPort);
                switch (packetType) {
                    case ACK:
                        key.sourceAddress(sourceAddress);
                        key.sourcePort(sourcePort);
                        key.sequenceNumber(tcpPacket.sequenceNumber());
                        key.ackNumber(tcpPacket.ackNumber());
                        break;
                    case SYN:
                        break;
                    case SYNACK:
                        key.sourceAddress(sourceAddress);
                        key.sourcePort(sourcePort);
                        key.ackNumber(tcpPacket.ackNumber());
                        break;
                    case FIN:
                        break;
                    case FINACK:
                        key.sourceAddress(sourceAddress);
                        key.sourcePort(sourcePort);
                        key.ackNumber(tcpPacket.ackNumber());
                        break;
                }
                if (packetType == ACK && ackHandler != null) {
                    ackHandler.ackReceived(systemPacket);
                } else {
                    LOGGER.debug("put {} into map with key: {}", systemPacket, key);
                    systemPacketMap.put(key, systemPacket);
                }
            }
        } catch (IOException | TCPUnknownPacketTypeException e) {
            LOGGER.catching(e);
            e.printStackTrace();
        }

        LOGGER.traceExit();
    }

    void ignoreDataPackets(boolean newValue) {
        LOGGER.traceEntry(String.valueOf(newValue));

        ignoreDataPackets = newValue;

        LOGGER.traceExit();
    }

    byte[] takeData(InetAddress sourceAddress, int sourcePort, short sequenceNumber) throws InterruptedException {
        LOGGER.traceEntry("source: {}:{} sequence: {}", sourceAddress, sourcePort, sequenceNumber);

        TOUSystemPacket key = new TOUSystemPacket();
        key.sourceAddress(sourceAddress);
        key.sourcePort(sourcePort);
        key.sequenceNumber(sequenceNumber);

        return LOGGER.traceExit(dataMap.take(key));
    }

    TOUSystemPacket receiveSystemPacket(TOUSystemPacket systemPacketKey) throws InterruptedException {
        LOGGER.traceEntry("key: {}", systemPacketKey);

        return LOGGER.traceExit(systemPacketMap.take(systemPacketKey));
    }

    @Override
    public String toString() {
        return "TOUReceiver <" + TOULog4JUtils.toString(socket) + '>';
    }

    public void setAckHandler(TOUAckHandler ackHandler) {
        this.ackHandler = ackHandler;
    }
}
