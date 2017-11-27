package ru.nsu.ccfit.bogush.tou;

public interface TOUBufferHolder {
    int available();
    TOUPacket flushIntoPacket();
}
