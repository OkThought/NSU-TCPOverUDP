package ru.nsu.ccfit.bogush.tou;

import ru.nsu.ccfit.bogush.tcp.TCPPacket;
import ru.nsu.ccfit.bogush.tcp.TCPPacketType;

import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.ThreadLocalRandom;

/**
 * SYN: The active open is performed by the client sending a SYN to the server.
 * The client sets the segment's sequence number to a random value A.
 * <p>
 * SYN-ACK: In response, the server replies with a SYN-ACK.
 * The acknowledgment number is set to one more than the received sequence number i.e. A+1,
 * and the sequence number that the server chooses for the packet is another random number, B.
 * <p>
 * ACK: Finally, the client sends an ACK back to the server.
 * The sequence number is set to the received acknowledgement value i.e. A+1,
 * and the acknowledgement number is set to one more than the received sequence number i.e. B+1.
 */
class TOUConnectionManager {
    private static int rand () {
        return ThreadLocalRandom.current().nextInt();
    }

    private static void swapSourceAndDestination (TCPPacket packet) {
        short dstPort = packet.getSourcePort();
        short srcPort = packet.getDestinationPort();
        packet.setDestinationPort(dstPort);
        packet.setSourcePort(srcPort);
    }

    private static TCPPacket createSYN (short srcPort, short dstPort) {
        TCPPacket syn = new TCPPacket(TCPPacket.HEADER_SIZE);
        syn.setSequenceNumber((short) rand()); // A
        syn.setSYN(true);
        syn.setDestinationPort(dstPort);
        syn.setSourcePort(srcPort);
        return syn;
    }

    private static TCPPacket createSYNACK (TCPPacket syn) {
        TCPPacket synack = new TCPPacket(syn.getBytes());
        synack.setAckNumber((short) (syn.getSequenceNumber() + 1)); // A + 1
        synack.setSequenceNumber((short) rand()); // B
        synack.setSYN(true);
        synack.setACK(true);
        swapSourceAndDestination(synack);
        return synack;
    }

    private static TCPPacket createACK (TCPPacket synack) {
        TCPPacket ack = new TCPPacket(synack.getBytes());
        ack.setSYN(false);
        ack.setSequenceNumber(synack.getAckNumber()); // A + 1
        ack.setAckNumber((short) (synack.getSequenceNumber() + 1)); // B + 1
        swapSourceAndDestination(ack);
        return ack;
    }


    private TOUSender sender;
    private TOUReceiver receiver;

    public TOUConnectionManager (TOUSender sender, TOUReceiver receiver) {
        this.sender = sender;
        this.receiver = receiver;
    }

    private TOUPacket sendSYN (short sourcePort, short destinationPort, InetAddress destinationAddress)
            throws InterruptedException {
        TOUPacket syn = PacketFactory.encapsulateIntoTOU(
                createSYN(sourcePort, destinationPort),
                destinationAddress
        );
        sender.put(syn);

        int ackNum = syn.getSequenceNumber(); // A + 1
        TOUPacket synack = receiver.takePacket(TCPPacketType.SYNACK, ackNum);

        sender.remove(syn);
        return synack;
    }

    private TOUPacket receiveSYN ()
            throws InterruptedException {
        return receiver.packetsOfType(TCPPacketType.SYN).iterator().next();
    }

    private TOUPacket sendSYNACK (TOUPacket syn)
            throws InterruptedException {
        TOUPacket synack = PacketFactory.encapsulateIntoTOU(createSYNACK(syn.getTcpPacket()), syn.getAddress());
        sender.put(synack);

        int ackNum = synack.getSequenceNumber() + 1; // B + 1
        int seqNum = synack.getTcpPacket().getAckNumber(); // A + 1
        TOUPacket ack = receiver.takePacket(TCPPacketType.ACK, (seqNum << 16) | ackNum);

        sender.remove(synack.getSequenceNumber());

        return ack;
    }

    private void sendACK (TOUPacket synack)
            throws InterruptedException, IOException {
        TOUPacket ack = PacketFactory.encapsulateIntoTOU(createACK(synack.getTcpPacket()), synack.getAddress());
        sender.sendOnce(ack);
    }

    private void startThreadsIfNotAlive () {
        if (!sender.isAlive()) sender.start();
        if (!receiver.isAlive()) receiver.start();
    }

    void connectToServer (short clientPort, short serverPort, InetAddress serverAddress)
            throws InterruptedException, IOException {
        startThreadsIfNotAlive();
        TOUPacket synack = sendSYN(clientPort, serverPort, serverAddress);
        sendACK(synack);
    }

    TOUSocket acceptConnection ()
            throws InterruptedException, IOException {
        startThreadsIfNotAlive();
        TOUPacket syn = receiveSYN();
        TOUPacket ack = sendSYNACK(syn);
        return new TOUSocket(ack.getAddress(), ack.getTcpPacket().getDestinationPort());
    }

    void closeConnection () {
        /*
         * TODO: wait until
         * - all packets in sender are sent
         * - all received packets are taken from receiver
         * close the udp socket
         * release all resources
         */
    }
}
