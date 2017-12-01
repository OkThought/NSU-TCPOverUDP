package ru.nsu.ccfit.bogush.tou;

import java.net.SocketImpl;
import java.net.SocketImplFactory;

public class TOUSocketImplFactory implements SocketImplFactory {
    @Override
    public SocketImpl createSocketImpl() {
        return new TOUSocketImpl();
    }
}
