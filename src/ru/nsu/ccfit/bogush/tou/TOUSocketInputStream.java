package ru.nsu.ccfit.bogush.tou;

import java.io.IOException;
import java.io.InputStream;

class TOUSocketInputStream extends InputStream {
    private TOUImpl impl;

    public TOUSocketInputStream (TOUImpl impl) {
        this.impl = impl;
    }

    @Override
    public int read () throws IOException {
        try {
            return impl.readByte();
        } catch (InterruptedException e) {
            throw new IOException(e);
        }
    }
}
