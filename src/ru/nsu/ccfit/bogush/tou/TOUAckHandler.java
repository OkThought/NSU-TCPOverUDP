package ru.nsu.ccfit.bogush.tou;

public interface TOUAckHandler {
    void ackReceived(TOUSystemPacket packet);
}
