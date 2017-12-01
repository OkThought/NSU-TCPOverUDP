package ru.nsu.ccfit.bogush.tou;

import java.io.IOException;

public interface TOUPacketHandler {
    void ackReceived(TOUSystemPacket systemPacket);
    void dataReceived(TOUSystemPacket dataKeyPacket) throws IOException;
}
