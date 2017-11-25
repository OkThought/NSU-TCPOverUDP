package ru.nsu.ccfit.bogush.tou;

import java.io.IOException;
import java.io.OutputStream;

class TOUSocketOutputStream extends OutputStream {
    private TOUImpl impl;

    public TOUSocketOutputStream (TOUImpl impl) {
        this.impl = impl;
    }

    @Override
    public void write (int b) throws IOException {
        impl.writeByte(b);
    }
}
