package ru.nsu.ccfit.bogush.tou;

interface AckHandler {
    void handleAck(short sequenceNumber);
}
