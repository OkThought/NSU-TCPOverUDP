package net;

interface AckHandler {
    void handleAck(short sequenceNumber);
}
