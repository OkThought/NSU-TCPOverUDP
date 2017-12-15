package ru.nsu.ccfit.bogush.tou;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.nsu.ccfit.bogush.tcp.TCPSegmentType;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.function.Predicate;

import static ru.nsu.ccfit.bogush.tcp.TCPSegmentType.*;

class TOUSocketImpl extends SocketImpl {
    private static final int PENDING_ACKS_QUEUE_CAPACITY = 32;

    static { TOULog4JUtils.initIfNotInitYet(); }
    private static final Logger LOGGER = LogManager.getLogger(TOUSocketImpl.class.getSimpleName());


    private DatagramSocket udpSocket;
    final TOUFactory factory;

    private final Object dataSegmentMonitor = new Object();
    private final Object systemMessageMonitor = new Object();
    private final Object pendingAcksMutex = new Object();
    private TOUSystemMessage lastSystemMessage;
    private HashMap<Short, byte[]> dataSegmentMap;
    private WeakHashMap<InetSocketAddress, TOUSocketImpl> implMap;
    private ArrayBlockingQueue<TOUSystemMessage> pendingAcks;
    TOUSocketOutputStream outputStream = null;
    private TOUSocketInputStream inputStream = null;
    private TOUCommunicator communicator;
    private InetAddress localAddress;
    private boolean closePending = false;
    private boolean connected = false;
    short initialSequenceNumber = 0;

    TOUSocketImpl() {
        LOGGER.traceEntry();

        try {
            this.localAddress = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            LOGGER.catching(e);
            e.printStackTrace();
        }

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

    private InetSocketAddress localSocketAddress() {
        return new InetSocketAddress(localAddress, localport);
    }

    private InetSocketAddress remoteSocketAddress() {
        return new InetSocketAddress(address, port);
    }

    private boolean isConnected() {
        return connected;
    }

    boolean isClosedOrPending() {
        return closePending;
    }

    byte[] nextDataSegment(short sequenceNumber) throws InterruptedException {
        LOGGER.trace("Request next data segment with seq: {}", sequenceNumber);

        byte[] dataSegment;
        synchronized (dataSegmentMonitor) {
            while (!dataSegmentMap.containsKey(sequenceNumber)) {
                dataSegmentMonitor.wait();
                if (isClosedOrPending()) return null;
            }
            dataSegment = dataSegmentMap.get(sequenceNumber);
        }

        return dataSegment;
    }

    @Override
    protected void create(boolean stream) throws IOException {
        LOGGER.traceEntry("stream: {}", stream);
        LOGGER.traceExit();
    }

    @Override
    protected void connect(String host, int port) throws IOException {
        connect(InetAddress.getByName(host), port);
    }

    @Override
    protected void connect(SocketAddress address, int timeout) throws IOException {
        LOGGER.traceEntry("address: {} timeout: {}", ()->address, ()->timeout);

        // ignores timeout

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

        this.address = address;
        this.port = port;

        implMap = new WeakHashMap<>();
        implMap.put(remoteSocketAddress(), this);
        communicator = new TOUCommunicator(implMap, udpSocket);
        communicator.startIfNotAlive();

        TOUSystemMessage syn = sendSYNorFIN(SYN);
        TOUSystemMessage synack = receiveSYNACKorFINACK(syn);
        if (synack == null) return;
        communicator.removeByReference(syn);
        pendingAcks = new ArrayBlockingQueue<>(PENDING_ACKS_QUEUE_CAPACITY);
        TOUSystemMessage ack = sendACK(synack);

        dataSegmentMap = new HashMap<>();

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

        this.udpSocket = new DatagramSocket(port, address);
        this.localport = udpSocket.getLocalPort();
        this.localAddress = udpSocket.getLocalAddress();
        udpSocket.setSoTimeout(TOUConstants.UDP_RECV_TIMEOUT);

        LOGGER.trace("bound successfully to {}:{}", localAddress, localport);

        LOGGER.traceExit();
    }

    @Override
    protected void listen(int backlog) throws IOException {
        LOGGER.traceEntry("backlog: {}", backlog);

        implMap = new WeakHashMap<>();
        LOGGER.trace("put this into implMap at key {}", this::localSocketAddress);
        implMap.put(localSocketAddress(), this);
        communicator = new TOUCommunicator(implMap, udpSocket);
        communicator.startIfNotAlive();

        LOGGER.traceExit();
    }

    @Override
    protected void accept(SocketImpl s) throws IOException {
        TOUSocketImpl impl = (TOUSocketImpl) s;
        LOGGER.traceEntry(()->impl);

        TOUSystemMessage syn = receiveSYN(localAddress, localport);
        if (syn == null) return;

        TOUSystemMessage synack = sendSYNACKorFINACK(syn);
        if (synack == null) return;

        TOUSystemMessage ack = receiveACK(syn, synack);
        if (ack == null) return;

        communicator.removeByReference(synack);

        impl.localAddress = localAddress;
        impl.localport = localport;
        impl.address = ack.sourceAddress();
        impl.port = ack.sourcePort();
        impl.communicator = communicator;
        impl.connected = true;
        impl.dataSegmentMap = new HashMap<>();
        impl.pendingAcks = new ArrayBlockingQueue<>(PENDING_ACKS_QUEUE_CAPACITY);

        implMap.put(impl.remoteSocketAddress(), impl);

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

        LOGGER.debug("close connection");

        if (outputStream != null) {
            LOGGER.debug("wait for communicator to flush output buffer");
            outputStream.flush();
        }

//        LOGGER.debug("wait until maps and queues are empty");
//        try {
//            blockUntilContainersEmpty();
//        } catch (InterruptedException e) {
//            LOGGER.catching(e);
//        }

        if (isConnected()) {
            LOGGER.debug("process 3-way tear down handshake");
            activeClose();
        }

        synchronized (systemMessageMonitor) {
            systemMessageMonitor.notifyAll();
        }

        synchronized (dataSegmentMonitor) {
            dataSegmentMonitor.notifyAll();
        }

        if (udpSocket != null) {
            LOGGER.debug("close UDP socket: {}", ()->TOULog4JUtils.toString(udpSocket));
            udpSocket.close();
        }

        if (communicator != null) {
            communicator.stop();
            communicator = null;
        }

        LOGGER.traceExit();
    }

    private void activeClose()  {
        LOGGER.traceEntry();

        try {
            TOUSystemMessage fin = sendSYNorFIN(FIN);
            if (fin == null) return;
            TOUSystemMessage finack = receiveSYNACKorFINACK(fin);
            if (finack == null) return;
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
            communicator.removeByReference(finack);
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
        LOGGER.debug("waiting for segment map");
        if (!dataSegmentMap.isEmpty()) synchronized (dataSegmentMonitor) {
            while (!dataSegmentMap.isEmpty()) {
                dataSegmentMap.wait();
            }
        }
        LOGGER.debug("segment map is empty");
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

    void processFIN(TOUSystemMessage fin) {
        if (isConnected()) {
            passiveClose(fin);
        }
    }

    private TOUSystemMessage sendSYNorFIN(TCPSegmentType type)
            throws IOException {
        LOGGER.traceEntry();
        try {
            TOUSystemMessage synOrFin = TOUFactory.createSYNorFIN(type, localAddress, localport, address, port);
            communicator.send(synOrFin);
            return LOGGER.traceExit(synOrFin);
        } catch (InterruptedException e) {
            throw LOGGER.throwing(new IOException(e));
        }
    }

    private TOUSystemMessage receiveSYN(InetAddress localAddress, int localPort) throws IOException {
        LOGGER.traceEntry("local address: {} local port: {}", localAddress, localPort);

        try {
            TOUSystemMessage syn = receiveSystemMessage(s ->
                s != null &&
                s.type() == SYN &&
                s.destinationAddress().equals(localAddress) &&
                s.destinationPort() == localPort);
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
            communicator.send(synackOrFinack);
            return LOGGER.traceExit(synackOrFinack);
        } catch (InterruptedException e) {
            throw LOGGER.throwing(new IOException(e));
        }
    }

    private TOUSystemMessage receiveSYNACKorFINACK(TOUSystemMessage synOrFin)
            throws IOException {
        LOGGER.traceEntry("{}", synOrFin);

        try {
            TOUSystemMessage synackOrFinack = receiveSystemMessage(s ->
                s != null &&
                s.type() == (synOrFin.type() == SYN ? SYNACK : FINACK) &&
                Objects.equals(s.destinationAddress, synOrFin.sourceAddress) &&
                s.destinationPort() == synOrFin.sourcePort() &&
                Objects.equals(s.sourceAddress, synOrFin.destinationAddress) &&
                s.sourcePort() == synOrFin.destinationPort() &&
                s.ackNumber() == synOrFin.sequenceNumber() + 1
            );
            return LOGGER.traceExit(synackOrFinack);
        } catch (InterruptedException e) {
            throw LOGGER.throwing(new IOException(e));
        }
    }

    private TOUSystemMessage sendACK(TOUSystemMessage synackOrFinack)
            throws IOException {
        LOGGER.traceEntry();

        TOUSystemMessage ack = TOUFactory.createACK(synackOrFinack);
        initialSequenceNumber = ack.sequenceNumber();
        try {
            pendingAcks.put(ack);
            return LOGGER.traceExit(ack);
        } catch (InterruptedException e) {
            LOGGER.catching(e);
            throw new IOException(e);
        }
    }

    private TOUSystemMessage receiveACK(TOUSystemMessage synOrFin, TOUSystemMessage synackOrFinack)
            throws IOException {
        LOGGER.traceEntry("{} {}", synOrFin, synackOrFinack);

        try {
            TOUSystemMessage ack = receiveSystemMessage(s ->
                s != null &&
                s.type() == ACK &&
                s.destinationPort() == synOrFin.destinationPort() &&
                s.sourcePort() == synOrFin.sourcePort() &&
                Objects.equals(s.sourceAddress, synOrFin.sourceAddress) &&
                Objects.equals(s.destinationAddress, synOrFin.destinationAddress) &&
                s.ackNumber() == synackOrFinack.sequenceNumber() + 1 &&
                s.sequenceNumber() == synackOrFinack.ackNumber()
            );
            return LOGGER.traceExit(ack);
        } catch (InterruptedException e) {
            throw LOGGER.throwing(new IOException(e));
        }
    }

    void processSegment(TOUSegment segment) {
        try {
            pendingAcks.put(TOUFactory.createACK(segment));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        putDataSegmentIntoMap(segment.tcpSegment.data(), segment.sequenceNumber());
    }

    private void putDataSegmentIntoMap(byte[] dataSegment, short sequenceNumber) {
        synchronized (dataSegmentMonitor) {
            dataSegmentMap.put(sequenceNumber, dataSegment);
            dataSegmentMonitor.notifyAll();
        }
    }

    void setSystemMessage(TOUSystemMessage systemMessage) {
        LOGGER.traceEntry("{}", systemMessage);

        synchronized (systemMessageMonitor) {
            lastSystemMessage = systemMessage;
            systemMessageMonitor.notifyAll();
        }

        LOGGER.traceExit();
    }

    private TOUSystemMessage receiveSystemMessage(Predicate<TOUSystemMessage> isExpected)
            throws InterruptedException {
        LOGGER.traceEntry();

        TOUSystemMessage result;
        synchronized (systemMessageMonitor) {
            while (!isExpected.test(lastSystemMessage)) {
                systemMessageMonitor.wait();
            }
            result = lastSystemMessage;
            lastSystemMessage = null;
        }

        return LOGGER.traceExit(result);
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

    void mergeWithAckIfPending(TOUSegment segment) {
        if (pendingAcks.isEmpty()) return;

        TOUSystemMessage ack;

        synchronized (pendingAcksMutex) {
            if (pendingAcks.isEmpty()) return;
            try {
                ack = pendingAcks.take();
            } catch (InterruptedException e) {
                e.printStackTrace();
                return;
            }
        }

        if (TOUFactory.canMerge(segment, ack)) {
            TOUFactory.merge(segment, ack);
        }
    }
}
