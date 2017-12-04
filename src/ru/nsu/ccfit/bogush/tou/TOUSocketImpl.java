package ru.nsu.ccfit.bogush.tou;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.nsu.ccfit.bogush.tcp.TCPSegmentType;
import ru.nsu.ccfit.bogush.tcp.TCPUnknownSegmentTypeException;
import ru.nsu.ccfit.bogush.util.BlockingHashMap;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import static ru.nsu.ccfit.bogush.tcp.TCPSegmentType.*;
import static ru.nsu.ccfit.bogush.tou.TOUConstants.QUEUE_CAPACITY;

class TOUSocketImpl extends SocketImpl {
    static { TOULog4JUtils.initIfNotInitYet(); }
    private static final Logger LOGGER = LogManager.getLogger(TOUSocketImpl.class.getSimpleName());

    private final BlockingQueue<TOUSegment> segmentQueue;
    private final BlockingQueue<TOUSystemMessage> systemMessageQueue;
    private final BlockingHashMap<Short, byte[]> segmentMap;
    private final BlockingHashMap<TOUSystemMessage, TOUSystemMessage> systemMessageMap;

    TOUFactory factory;
    TOUSocketOutputStream outputStream = null;
    TOUSocketInputStream inputStream = null;

    private DatagramSocket datagramSocket;
    private TOUCommunicator communicator;
    private InetAddress localAddress;
    private boolean closePending = false;
    private boolean connected = false;
    private boolean bound = false;

    TOUSocketImpl() {
        LOGGER.traceEntry();

        try {
            this.localAddress = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            LOGGER.catching(e);
            e.printStackTrace();
        }

        this.segmentQueue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
        this.systemMessageQueue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
        this.segmentMap = new BlockingHashMap<>();
        this.systemMessageMap = new BlockingHashMap<>();
        this.factory = new TOUFactory(this);

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

    BlockingHashMap<Short, byte[]> segmentMap() {
        return segmentMap;
    }

    BlockingHashMap<TOUSystemMessage, TOUSystemMessage> systemMessageMap() {
        return systemMessageMap;
    }

    BlockingQueue<TOUSegment> segmentQueue() {
        return segmentQueue;
    }

    BlockingQueue<TOUSystemMessage> systemMessageQueue() {
        return systemMessageQueue;
    }

    InetSocketAddress localSocketAddress() {
        return new InetSocketAddress(localAddress, localport);
    }

    InetSocketAddress remoteSocketAddress() {
        return new InetSocketAddress(address, port);
    }

    boolean isBound() {
        return bound;
    }

    boolean isConnected() {
        return connected;
    }

    boolean isClosedOrPending() {
        return closePending;
    }

    byte[] nextData(short sequenceNumber) throws InterruptedException {
        return segmentMap.take(sequenceNumber);
    }

    @Override
    protected void create(boolean stream) throws IOException {
        LOGGER.traceEntry("stream: ", stream);

        LOGGER.traceExit();
    }

    @Override
    protected void connect(String host, int port) throws IOException {
        connect(InetAddress.getByName(host), port);
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
    protected void connect(InetAddress address, int port) throws IOException {
        LOGGER.traceEntry("{}:{}", address, port);

        bind(localAddress, 0);

        this.communicator.startIfNotAlive();

        TOUSystemMessage syn = sendSYNorFIN(SYN);
        TOUSystemMessage synack = receiveSYNACKorFINACK(syn);
        sendACK(synack);

        connected = true;
        LOGGER.info("================ Successfully connected to {}:{} ================", address, port);

        LOGGER.traceExit();
    }

    private void disconnect() {
        connected = false;
    }

    @Override
    protected void bind(InetAddress address, int port) throws IOException {
        LOGGER.traceEntry("{}:{}", address, port);

        if (isClosedOrPending()) {
            throw LOGGER.throwing(new SocketException("Socket closed"));
        }

        if (address.isAnyLocalAddress()) {
            address = localAddress;
        }

        this.datagramSocket = new DatagramSocket(port, address);
        this.localport = datagramSocket.getLocalPort();
        this.localAddress = datagramSocket.getLocalAddress();
        datagramSocket.setSoTimeout(TOUConstants.UDP_RECV_TIMEOUT);
        this.communicator = new TOUCommunicator(datagramSocket);
        communicator.addSocketImpl(this);

        bound = true;
        LOGGER.trace("bound successfully to {}:{}", localAddress, localport);

        LOGGER.traceExit();
    }

    @Override
    protected void listen(int backlog) throws IOException {
        LOGGER.traceEntry("backlog: {}", backlog);

        communicator.startIfNotAlive();

        LOGGER.traceExit();
    }

    @Override
    protected void accept(SocketImpl s) throws IOException {
        TOUSocketImpl impl = (TOUSocketImpl) s;
        LOGGER.traceEntry(()->impl);

        TOUSystemMessage syn = receiveSYN(localAddress, localport);
        TOUSystemMessage synack = sendSYNACKorFINACK(syn);
        TOUSystemMessage ack = receiveACK(syn, synack);

        impl.localAddress = localAddress;
        impl.localport = localport;
        impl.address = ack.sourceAddress();
        impl.port = ack.sourcePort();
        impl.communicator = communicator;
        impl.connected = true;
        communicator.addSocketImpl(impl);

        LOGGER.trace("Accepted impl: {}", impl);

        LOGGER.info("================ Successfully accepted connection from {}:{} ================",
                ack.sourceAddress(), ack.sourcePort());

        LOGGER.traceExit();
    }

    @Override
    protected void close()
            throws IOException {
        LOGGER.traceEntry();

        if (closePending) {
            LOGGER.warn("Already closing");
            LOGGER.traceExit();
            return;
        } else {
            closePending = true;
        }

        // wait for communicator to flush output buffer
        if (outputStream != null) {
            outputStream.flush();
        }

        // wait for app to read all bytes from input stream
        // TODO: 12/3/17 decide if it is necessary

        // wait until maps and queues are empty
        try {
            blockUntilContainersEmpty();
        } catch (InterruptedException e) {
            LOGGER.catching(e);
        }

        if (isConnected()) {
            activeClose();
        }

        if (datagramSocket != null) {
            datagramSocket.close();
        }

        communicator.stop();
        communicator = null;

        LOGGER.traceExit();
    }

    private void activeClose()  {
        LOGGER.traceEntry();

        try {
            TOUSystemMessage fin = sendSYNorFIN(FIN);
            TOUSystemMessage finack = receiveSYNACKorFINACK(fin);
            sendACK(finack);
        } catch (IOException e) {
            LOGGER.catching(e);
            e.printStackTrace();
        }

        LOGGER.traceExit();
    }

    private void passiveClose(TOUSystemMessage fin) {
        LOGGER.traceEntry("{}", fin);

        if (closePending) return;

        try {
            TOUSystemMessage finack = sendSYNACKorFINACK(fin);
            TOUSystemMessage ack = receiveACK(fin, finack);
            disconnect();
            close();
        } catch (IOException e) {
            LOGGER.catching(e);
            e.printStackTrace();
        }

        LOGGER.traceExit();
    }

    private void blockUntilContainersEmpty()
            throws InterruptedException {
        if (!segmentQueue.isEmpty()) synchronized (segmentQueue) {
            while (!segmentQueue.isEmpty()) {
                segmentQueue.wait();
            }
        }

        if (!segmentMap.isEmpty()) synchronized (segmentMap) {
            while (!segmentMap.isEmpty()) {
                segmentMap.wait();
            }
        }

        if (!systemMessageQueue.isEmpty()) synchronized (systemMessageQueue) {
            while (!systemMessageQueue.isEmpty()) {
                systemMessageQueue.wait();
            }
        }

        if (!systemMessageMap.isEmpty()) synchronized (systemMessageMap) {
            while (!systemMessageMap.isEmpty()) {
                systemMessageMap.wait();
            }
        }
    }

    @Override
    protected InputStream getInputStream()
            throws IOException {
        LOGGER.traceEntry();

        if (isClosedOrPending()) {
            throw new SocketException("Socket Closed");
        }

        if (inputStream == null) {
            inputStream = new TOUSocketInputStream(this);
        }
        return LOGGER.traceExit(inputStream);
    }

    @Override
    protected OutputStream getOutputStream()
            throws IOException {
        LOGGER.traceEntry();

        if (isClosedOrPending()) {
            throw new SocketException("Socket Closed");
        }

        if (outputStream == null) {
            outputStream = new TOUSocketOutputStream(this);
        }
        return LOGGER.traceExit(outputStream);
    }

    @Override
    protected int available()
            throws IOException {
        LOGGER.traceEntry();

        if (isClosedOrPending()) {
            throw new IOException("Stream closed");
        }

        int available = outputStream == null ? 0 : outputStream.available();

        return LOGGER.traceExit(available);
    }

    private boolean removeFromQueue(TOUSystemMessage systemPacket) throws TCPUnknownSegmentTypeException {
        LOGGER.traceEntry();

        boolean removed = systemMessageQueue.remove(systemPacket);

        if (!removed) for (TOUSegment dataPacket : segmentQueue) {
            if (TOUFactory.isMergedWithSystemMessage(dataPacket, systemPacket)) {
                TOUFactory.unmerge(dataPacket);
                removed = true;
            }
        }

        return LOGGER.traceExit(removed);
    }

    void systemMessageReceived(TOUSystemMessage systemMessage) {
        LOGGER.traceEntry("system message: {}", systemMessage);

        TCPSegmentType type = systemMessage.type();
        if (type == ACK && isConnected()) {
            LOGGER.debug("Handle ACK {}", systemMessage.ackNumber());
            TOUSegment touSegment = TOUFactory.createTOUSegmentByAck(systemMessage);
            systemMessageMap.remove(systemMessage);
            segmentQueue.remove(touSegment);
        } else if (type == FIN){
            if (isConnected()) {
                passiveClose(systemMessage);
            }

        } else {
            TOUSystemMessage key = TOUFactory.generateSystemMessageKey(systemMessage);
//            LOGGER.debug("put {} into map with key: {}", systemMessage, key);
            systemMessageMap.put(key, systemMessage);
        }

        LOGGER.traceExit();
    }

    private TOUSystemMessage sendSYNorFIN(TCPSegmentType type)
            throws IOException {
        LOGGER.traceEntry();
        try {
            TOUSystemMessage synOrFin = TOUFactory.createSYNorFIN(type, localAddress, localport, address, port);
            systemMessageQueue.put(synOrFin);
            return LOGGER.traceExit(synOrFin);
        } catch (InterruptedException e) {
            throw LOGGER.throwing(new IOException(e));
        }
    }

    private TOUSystemMessage receiveSYN(InetAddress localAddress, int localPort) throws IOException {
        LOGGER.traceEntry("local address: {} local port: {}", localAddress, localPort);

        TOUSystemMessage expected = new TOUSystemMessage(SYN);
        expected.destinationAddress(localAddress);
        expected.destinationPort(localPort);

        try {
            TOUSystemMessage syn = systemMessageMap.take(expected);
            return LOGGER.traceExit(syn);
        } catch (InterruptedException e) {
            throw LOGGER.throwing(new IOException(e));
        }
    }

    private TOUSystemMessage sendSYNACKorFINACK(TOUSystemMessage synOrFin)
            throws IOException {
        LOGGER.traceEntry(()->synOrFin);
        try {
            TOUSystemMessage synackOrFinack = factory.createSYNACKorFINACK(synOrFin);
            systemMessageQueue.put(synackOrFinack);
            return LOGGER.traceExit(synackOrFinack);
        } catch (InterruptedException e) {
            throw LOGGER.throwing(new IOException(e));
        }
    }

    private TOUSystemMessage receiveSYNACKorFINACK(TOUSystemMessage synOrFin)
            throws IOException {
        LOGGER.traceEntry("{}", synOrFin);

        TOUSystemMessage expected = new TOUSystemMessage(synOrFin.type() == SYN ? SYNACK : FINACK);
        expected.destinationAddress(synOrFin.sourceAddress());
        expected.destinationPort(synOrFin.sourcePort());
        expected.sourceAddress(synOrFin.destinationAddress());
        expected.sourcePort(synOrFin.destinationPort());
        expected.ackNumber((short) (synOrFin.sequenceNumber() + 1));

        TOUSystemMessage synackOrFinack;
        try {
            synackOrFinack = systemMessageMap.take(expected);
            removeFromQueue(synOrFin);
        } catch (InterruptedException | TCPUnknownSegmentTypeException e) {
            throw LOGGER.throwing(new IOException(e));
        }
        return LOGGER.traceExit(synackOrFinack);
    }

    private void sendACK(TOUSystemMessage synackOrFinack)
            throws IOException {
        LOGGER.traceEntry();

        TOUSystemMessage ack = TOUFactory.createACK(synackOrFinack);
        communicator.sendOnce(ack);

        LOGGER.traceExit();
    }

    private TOUSystemMessage receiveACK(TOUSystemMessage synOrFin, TOUSystemMessage synackOrFinack)
            throws IOException {
        LOGGER.traceEntry();

        TOUSystemMessage expectedPacket = new TOUSystemMessage(synOrFin);
        expectedPacket.type(ACK);
        expectedPacket.ackNumber((short) (synackOrFinack.sequenceNumber() + 1)); // B + 1
        expectedPacket.sequenceNumber(synackOrFinack.ackNumber()); // A + 1
        TOUSystemMessage ack;
        try {
            ack = systemMessageMap.take(expectedPacket);
            removeFromQueue(synackOrFinack);
            return LOGGER.traceExit(ack);
        } catch (InterruptedException | TCPUnknownSegmentTypeException e) {
            throw LOGGER.throwing(new IOException(e));
        }
    }

    @Override
    protected void sendUrgentData(int data) throws IOException {
        LOGGER.traceEntry();
        LOGGER.traceExit();
    }

    @Override
    public String toString() {
        return String.format("TOUSocketImpl <local: %s:%d remote: %s:%d>", localAddress, localport, address, port);
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
}
