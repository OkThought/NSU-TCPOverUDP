package ru.nsu.ccfit.bogush.net;

abstract class TOUAbstractImpl {
    static final int PACKET_SIZE = 576; // bytes
    static final int QUEUE_CAPACITY = 512;

    abstract int readByte();

    abstract void writeByte(int b);
}
