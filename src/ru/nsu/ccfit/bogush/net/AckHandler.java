package ru.nsu.ccfit.bogush.net;

interface AckHandler {
    void handleAck(short sequenceNumber);
}
