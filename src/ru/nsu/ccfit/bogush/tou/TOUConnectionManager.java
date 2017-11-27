package ru.nsu.ccfit.bogush.tou;

import ru.nsu.ccfit.bogush.tcp.TCPPacket;
import ru.nsu.ccfit.bogush.tcp.TCPPacketType;
import ru.nsu.ccfit.bogush.tcp.TCPUnknownPacketTypeException;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.ThreadLocalRandom;

import static ru.nsu.ccfit.bogush.tcp.TCPPacketType.*;
import static ru.nsu.ccfit.bogush.tou.TOUConnectionState.*;

/**
 *      SYN: The active open is performed by the client sending a SYN to the server.
 *      The client sets the segment's sequence number to a random value A.
 * <p>
 *      SYN-ACK: In response, the server replies with a SYN-ACK.
 *      The acknowledgment number is set to one more than the received sequence number i.e. A+1,
 *      and the sequence number that the server chooses for the packet is another random number, B.
 * <p>
 *      ACK: Finally, the client sends an ACK back to the server.
 *      The sequence number is set to the received acknowledgement value i.e. A+1,
 *      and the acknowledgement number is set to one more than the received sequence number i.e. B+1.
 * <p>
 *      FIN: Termination initiator sends FIN with
 *      sequence number set to random value A
 * <p>
 *      FIN-ACK: Receiver of FIN sends FIN-ACK with
 *      ack number set to A+1
 *      and sequence number set to random value B
 * <p>
 *      ACK: Termination initiator sends ACK with
 *      ack number set to B+1
 *      and sequence number set to A+1
 */
class TOUConnectionManager {
    private DatagramSocket datagramSocket;
    private TOUSender sender;
    private TOUReceiver receiver;
    private volatile TOUConnectionState state = CLOSED;

    TOUConnectionManager() {}

    void sender(TOUSender sender) {
        this.sender = sender;
    }

    TOUSender sender() {
        return sender;
    }

    void receiver(TOUReceiver receiver) {
        this.receiver = receiver;
    }

    TOUReceiver receiver() {
        return receiver;
    }

    void bind(DatagramSocket datagramSocket) {
        checkState(CLOSED);
        this.datagramSocket = datagramSocket;
        state = BOUND;
    }

    void listen() {
        checkState(BOUND);
        state = LISTEN;
    }

    void connect(InetAddress serverAddress, int serverPort)
            throws InterruptedException, IOException, TCPUnknownPacketTypeException {
        checkState(BOUND);
        datagramSocket.connect(serverAddress, serverPort);

        TOUSystemPacket synack = sendSynOrFin(SYN,
                datagramSocket.getLocalAddress(), datagramSocket.getLocalPort(),
                serverPort, serverAddress);
        state = SYN_SENT;

        sendACK(synack);
        state = ESTABLISHED;
    }

    TOUSocket accept()
            throws InterruptedException, IOException, TCPUnknownPacketTypeException {
        checkState(LISTEN);
        startThreadsIfNotAlive();

        TOUSystemPacket syn = receiveSynOrFin(SYN, datagramSocket.getLocalAddress(), datagramSocket.getLocalPort());
        state = SYN_RECEIVED;

        TOUSystemPacket ack = sendSynackOrFinack(SYNACK, syn, datagramSocket.getLocalPort());
        state = ESTABLISHED;

        return new TOUSocket(syn.sourceAddress(), syn.sourcePort());
    }

    void close() {

         // TODO: wait until all packets in sender are sent
         // TODO: wait until all received packets are taken from receiver
         // TODO: close the udp socket
         // TODO: release all resources
    }

    private void checkState(TOUConnectionState expectedState) {
        if (state != expectedState) {
            throw new IllegalStateException("State found: " + state + ". State expected: " + expectedState);
        }
    }

    private TOUSystemPacket receiveSynOrFin(TCPPacketType type, InetAddress localAddress, int localPort)
            throws InterruptedException {
        assert type == SYN || type == FIN;
        TOUSystemPacket expectedPacket = new TOUSystemPacket(type);
        expectedPacket.destinationAddress(localAddress);
        expectedPacket.destinationPort(localPort);
        return receiver.receiveSystemPacket(expectedPacket);
    }

    private TOUSystemPacket receiveSynackOrFinack(TCPPacketType type, TOUSystemPacket synOrFin)
            throws InterruptedException {
        assert type == SYNACK || type == FINACK;
        TOUSystemPacket expectedPacket = new TOUSystemPacket(type);
        expectedPacket.destinationAddress(synOrFin.sourceAddress());
        expectedPacket.destinationPort(synOrFin.sourcePort());
        expectedPacket.sourceAddress(synOrFin.destinationAddress());
        expectedPacket.sourcePort(synOrFin.destinationPort());
        expectedPacket.ackNumber((short) (synOrFin.sequenceNumber() + 1)); // A + 1
        return receiver.receiveSystemPacket(expectedPacket);
    }

    private TOUSystemPacket receiveACK(TOUSystemPacket synOrFin, TOUSystemPacket synackOrFinack)
            throws InterruptedException {
        TCPPacketType type = synOrFin.type();
        assert type == SYN || type == FIN;
        TOUSystemPacket expectedPacket = new TOUSystemPacket(synOrFin);
        expectedPacket.type(ACK);
        expectedPacket.ackNumber((short) (synackOrFinack.sequenceNumber() + 1)); // B + 1
        expectedPacket.sequenceNumber(synackOrFinack.ackNumber()); // A + 1
        return receiver.receiveSystemPacket(expectedPacket);
    }

    private TOUSystemPacket sendSynOrFin(TCPPacketType type, InetAddress sourceAddress, int sourcePort, int destinationPort, InetAddress destinationAddress)
            throws InterruptedException, TCPUnknownPacketTypeException {
        assert type == SYN || type == FIN;
        TOUSystemPacket synOrFin = createSynOrFin(type, sourceAddress, sourcePort, destinationAddress, destinationPort);
        sender.putInQueue(synOrFin);
        TOUSystemPacket synackOrFinack = receiveSynackOrFinack(type == SYN ? SYNACK : FINACK, synOrFin);
        sender.removeFromQueue(synOrFin);
        return synackOrFinack;
    }

    private TOUSystemPacket sendSynackOrFinack(TCPPacketType type, TOUSystemPacket synOrFin, int localPort)
            throws InterruptedException, IOException, TCPUnknownPacketTypeException {
        assert type == SYNACK || type == FINACK;
        TOUSystemPacket synackOrFinack = createSynackOrFinack(type, datagramSocket.getLocalAddress(), localPort, synOrFin);
        sender.putInQueue(synackOrFinack);
        TOUSystemPacket ack = receiveACK(synOrFin, synackOrFinack);
        sender.removeFromQueue(synackOrFinack);
        return ack;
    }

    private void sendACK(TOUSystemPacket synackOrFinack)
            throws InterruptedException, IOException, TCPUnknownPacketTypeException {
        TCPPacketType type = synackOrFinack.type();
        assert type == SYNACK || type == FINACK;
        TOUSystemPacket ack = createACK(synackOrFinack);

        sender.sendOnce(ack);
//        sender.putInQueue(ack);
//        TOUSystemPacket expectedPacket = new TOUSystemPacket(synackOrFinack);
//        expectedPacket.type(ACK);
//        TOUSystemPacket ackToAck = receiver.receiveSystemPacket(expectedPacket);
//        sender.removeFromQueue(ack);
    }

    private void startThreadsIfNotAlive() {
        if (!sender.isAlive()) sender.start();
        if (!receiver.isAlive()) receiver.start();
    }

    private static short rand() {
        return (short) ThreadLocalRandom.current().nextInt();
    }

    private static void swapSourceAndDestination(TCPPacket packet) {
        short dstPort = packet.sourcePort();
        short srcPort = packet.destinationPort();
        packet.destinationPort(dstPort);
        packet.sourcePort(srcPort);
    }

    private static TOUSystemPacket createSynOrFin(TCPPacketType type, InetAddress srcAddr, int srcPort, InetAddress dstAddr, int dstPort) {
        assert type == SYN || type == FIN;
        return new TOUSystemPacket(type, srcAddr, srcPort, dstAddr, dstPort, rand(), (short) 0);
    }

    private static TOUSystemPacket createSynackOrFinack(TCPPacketType type, InetAddress localAddress, int localPort, TOUSystemPacket synOrFin) {
        assert type == SYNACK || type == FINACK;
        TOUSystemPacket synack = new TOUSystemPacket(synOrFin);
        synack.sourceAddress(localAddress);
        synack.sourcePort(localPort);
        synack.type(type);
        synack.ackNumber((short) (synOrFin.sequenceNumber() + 1));
        synack.sequenceNumber(rand());
        return synack;
    }

    private static TOUSystemPacket createACK(TOUSystemPacket synackOrFinack) {
        TCPPacketType type = synackOrFinack.type();
        assert type == SYNACK || type == FINACK;
        TOUSystemPacket ack = new TOUSystemPacket(synackOrFinack);
        ack.sourceAddress(synackOrFinack.destinationAddress());
        ack.sourcePort(synackOrFinack.destinationPort());
        ack.destinationAddress(synackOrFinack.sourceAddress());
        ack.destinationPort(synackOrFinack.sourcePort());
        ack.sequenceNumber(synackOrFinack.ackNumber()); // A + 1
        ack.ackNumber((short) (synackOrFinack.sequenceNumber() + 1)); // B + 1
        return ack;
    }
}
