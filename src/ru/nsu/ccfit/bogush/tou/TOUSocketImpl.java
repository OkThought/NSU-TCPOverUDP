package ru.nsu.ccfit.bogush.tou;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.nsu.ccfit.bogush.tcp.TCPPacket;
import ru.nsu.ccfit.bogush.tcp.TCPUnknownPacketTypeException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;

import static ru.nsu.ccfit.bogush.tcp.TCPPacketType.*;

class TOUSocketImpl extends SocketImpl {
    static final int MAX_DATA_SIZE = 1024; // bytes
    static final int MAX_PACKET_SIZE = MAX_DATA_SIZE + TCPPacket.HEADER_SIZE;
    static final int QUEUE_CAPACITY = 512;
    static final int DATA_PACKET_POLL_TIMEOUT = 1000;
    static final int SYSTEM_PACKET_POLL_TIMEOUT = 1000;

    static {
        TOULog4JUtils.initIfNotInitYet();
    }
    private static final Logger LOGGER = LogManager.getLogger(TOUSocketImpl.class.getSimpleName());

    private InetAddress localAddress;
    DatagramSocket datagramSocket;
    TOUSender sender;
    TOUReceiver receiver;
    TOUSocketOutputStream outputStream = null;
    TOUSocketInputStream inputStream = null;

    TOUSocketImpl() {
        LOGGER.traceEntry();
        try {
            this.localAddress = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            LOGGER.catching(e);
            e.printStackTrace();
        }
        LOGGER.traceExit();
    }

    InetAddress localAddress() {
        return localAddress;
    }

    int localPort() {
        return localport;
    }

    InetAddress address() {
        return address;
    }

    int port() {
        return port;
    }

    @Override
    protected void create(boolean stream) throws IOException {
        LOGGER.traceEntry("stream: ", stream);

        LOGGER.traceExit();
    }

    @Override
    protected void connect(String host, int port) throws IOException {
        this.connect(InetAddress.getByName(host), port);
    }

    @Override
    protected void connect(InetAddress address, int port) throws IOException {
        LOGGER.traceEntry("{}:{}", address, port);

        bind(localAddress, 0);

        initSenderAndReceiver();

        this.sender.start();
        this.receiver.start();

        TOUSystemPacket syn = sendSYN();
        TOUSystemPacket synack = receiveSYNACK(syn);
        sendACK(synack);
        this.receiver.ignoreDataPackets(false);

        LOGGER.info("================ Successfully connected to {}:{} ================", address, port);

        LOGGER.traceExit();
    }

    @Override
    protected void connect(SocketAddress address, int timeout) throws IOException {
        LOGGER.traceEntry("address: {} timeout: {}", ()->address, ()->timeout);


        InetSocketAddress socketAddress = (InetSocketAddress) address;
        super.address = socketAddress.getAddress();
        super.port = socketAddress.getPort();

        connect(super.address, super.port);

        LOGGER.traceExit();
    }

    @Override
    protected void bind(InetAddress host, int port) throws IOException {
        LOGGER.traceEntry("{}:{}", host, port);

        if (host.isAnyLocalAddress()) {
            host = localAddress;
        }
        datagramSocket = new DatagramSocket(new InetSocketAddress(host, port));
//        datagramSocket.bind(new InetSocketAddress(host, port));
        super.localport = datagramSocket.getLocalPort();

        LOGGER.traceExit();
    }

    @Override
    protected void listen(int backlog) throws IOException {
        LOGGER.traceEntry("backlog: {}", backlog);

        initSenderAndReceiver();

        LOGGER.traceExit();
    }

    @Override
    protected void accept(SocketImpl s) throws IOException {
        TOUSocketImpl impl = (TOUSocketImpl) s;
        LOGGER.traceEntry(()->impl);

        if (!sender.isAlive()) {
            sender.start();
        }
        if (!receiver.isAlive()) {
            receiver.start();
        }

        TOUSystemPacket syn = receiveSYN(datagramSocket.getLocalAddress(), datagramSocket.getLocalPort());

        TOUSystemPacket synack = sendSYNACK(syn);
        receiveACK(syn, synack);

        impl.datagramSocket = datagramSocket;
        impl.localAddress = localAddress;
        impl.localport = localport;
        impl.address = syn.sourceAddress();
        impl.port = syn.sourcePort();
        impl.receiver = receiver;
        impl.sender = sender;

        LOGGER.trace("Accepted impl: {}", impl);

        LOGGER.info("================ Successfully accepted connection from {}:{} ================", address, port);

        LOGGER.traceExit();
    }

    @Override
    protected InputStream getInputStream() throws IOException {
        LOGGER.traceEntry();
        if (inputStream == null) {
            inputStream = new TOUSocketInputStream(this);
        }
        return LOGGER.traceExit(inputStream);
    }

    @Override
    protected OutputStream getOutputStream() throws IOException {
        LOGGER.traceEntry();
        if (outputStream == null) {
            outputStream = new TOUSocketOutputStream(this);
        }
        return LOGGER.traceExit(outputStream);
    }

    @Override
    protected int available() throws IOException {
        LOGGER.traceEntry();
        return LOGGER.traceExit(outputStream == null ? 0 : outputStream.available());
    }

    @Override
    protected void close() throws IOException {
        // TODO: wait until all packets in sender are sent
        // TODO: wait until all received packets are taken from receiver
        // TODO: close the udp socket
        // TODO: release all resources
        LOGGER.traceEntry();
        LOGGER.traceExit();
    }

    @Override
    protected void sendUrgentData(int data) throws IOException {
        LOGGER.traceEntry();
        LOGGER.traceExit();
    }

    boolean isConnected() {
        return address != null;
    }

    void ackReceived(TOUSystemPacket packet) {
        LOGGER.traceEntry("system packet: {}", packet);

        if (isConnected()) {
            LOGGER.debug("Handle ACK {}", packet.ackNumber());
            TOUPacket touPacket = TOUPacketFactory.createTOUPacketByAck(packet);
            this.receiver.deleteSystemPacketFromMap(packet);
            sender.removeFromQueue(touPacket);
        }

        LOGGER.traceExit();
    }

    void dataReceived(TOUSystemPacket key) throws IOException {
        LOGGER.traceEntry("key: {}", key);

        /*
         * ignore data when connection is not established yet
         */
        if (isConnected()) {
            LOGGER.debug("Handle data packet of sequence {}", key.sequenceNumber());
            TOUSystemPacket ack = TOUPacketFactory.createAckToDataPacket(key);
            this.sender.sendOnce(ack);
        }

        LOGGER.traceExit();
    }

    @Override
    public void setOption(int optID, Object value) throws SocketException {
        LOGGER.traceEntry();
        LOGGER.traceExit();
    }

    @Override
    public Object getOption(int optID) throws SocketException {
        LOGGER.traceEntry();
        return LOGGER.traceExit("{}", null);
    }

    private void initSenderAndReceiver() throws IOException {
        sender = new TOUSender(this);
        receiver = new TOUReceiver(this);
    }

    private TOUSystemPacket receiveSYN(InetAddress localAddress, int localPort) throws IOException {
        LOGGER.traceEntry("local address: {} local port: {}", localAddress, localPort);

        TOUSystemPacket expectedPacket = new TOUSystemPacket(SYN);
        expectedPacket.destinationAddress(localAddress);
        expectedPacket.destinationPort(localPort);

        try {
            TOUSystemPacket syn = receiver.receiveSystemPacket(expectedPacket);
            return LOGGER.traceExit(syn);
        } catch (InterruptedException e) {
            throw LOGGER.throwing(new IOException(e));
        }
    }

    private TOUSystemPacket receiveSYNACK(TOUSystemPacket syn)
            throws IOException {
        LOGGER.traceEntry("{}", syn);

        TOUSystemPacket expectedPacket = new TOUSystemPacket(SYNACK);
        expectedPacket.destinationAddress(syn.sourceAddress());
        expectedPacket.destinationPort(syn.sourcePort());
        expectedPacket.sourceAddress(syn.destinationAddress());
        expectedPacket.sourcePort(syn.destinationPort());
        expectedPacket.ackNumber((short) (syn.sequenceNumber() + 1));

        TOUSystemPacket synack;
        try {
            synack = receiver.receiveSystemPacket(expectedPacket);
            sender.removeFromQueue(syn);
        } catch (InterruptedException | TCPUnknownPacketTypeException e) {
            throw LOGGER.throwing(new IOException(e));
        }
        return LOGGER.traceExit(synack);
    }

    private TOUSystemPacket receiveFINACK(TOUSystemPacket fin)
            throws InterruptedException {
        LOGGER.traceEntry("{}", fin);

        TOUSystemPacket expectedPacket = new TOUSystemPacket(FINACK);
        expectedPacket.destinationAddress(fin.sourceAddress());
        expectedPacket.destinationPort(fin.sourcePort());
        expectedPacket.sourceAddress(fin.destinationAddress());
        expectedPacket.sourcePort(fin.destinationPort());
        expectedPacket.ackNumber((short) (fin.sequenceNumber() + 1));

        return LOGGER.traceExit(receiver.receiveSystemPacket(expectedPacket));
    }

    private TOUSystemPacket receiveACK(TOUSystemPacket synOrFin, TOUSystemPacket synackOrFinack)
            throws IOException {
        LOGGER.traceEntry();

        TOUSystemPacket expectedPacket = new TOUSystemPacket(synOrFin);
        expectedPacket.type(ACK);
        expectedPacket.ackNumber((short) (synackOrFinack.sequenceNumber() + 1)); // B + 1
        expectedPacket.sequenceNumber(synackOrFinack.ackNumber()); // A + 1
        TOUSystemPacket ack;
        try {
            ack = receiver.receiveSystemPacket(expectedPacket);
            sender.removeFromQueue(synackOrFinack);
            return LOGGER.traceExit(ack);
        } catch (InterruptedException | TCPUnknownPacketTypeException e) {
            throw LOGGER.throwing(new IOException(e));
        }
    }

    private TOUSystemPacket sendSYN()
            throws IOException {
        LOGGER.traceEntry();
        try {
            TOUSystemPacket syn = TOUPacketFactory.createSYN(localAddress, localport, address, port);
            sender.putInQueue(syn);
            return LOGGER.traceExit(syn);
        } catch (InterruptedException e) {
            throw LOGGER.throwing(new IOException(e));
        }
    }

    private TOUSystemPacket sendSYNACK(TOUSystemPacket synOrFin)
            throws IOException {
        LOGGER.traceEntry(()->synOrFin);
        try {
            TOUSystemPacket synack = TOUPacketFactory.createSYNACK(datagramSocket.getLocalAddress(), localport, synOrFin);
            sender.putInQueue(synack);
            receiver.ignoreDataPackets(false);
            return LOGGER.traceExit(synack);
        } catch (InterruptedException e) {
            throw LOGGER.throwing(new IOException(e));
        }
    }

    private void sendACK(TOUSystemPacket synackOrFinack)
            throws IOException {
        LOGGER.traceEntry();

        TOUSystemPacket ack = TOUPacketFactory.createACK(synackOrFinack);
        sender.sendOnce(ack);

        LOGGER.traceExit();
    }

    @Override
    public String toString() {
        return String.format("TOUSocketImpl <local: %s:%d remote: %s:%d>", localAddress, localport, address, port);
    }
}
