package net;

import java.io.IOException;
import java.io.OutputStream;

class TOUSocketOutputStream extends OutputStream {
    private TOUAbstractImpl impl;

    public TOUSocketOutputStream(TOUAbstractImpl impl) {
        this.impl = impl;
    }

    @Override
    public void write(int b) throws IOException {
        impl.writeByte(b);
    }
}
