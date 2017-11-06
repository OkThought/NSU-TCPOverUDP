package ru.nsu.ccfit.bogush.net;

import java.io.IOException;
import java.io.InputStream;

class TOUSocketInputStream extends InputStream {
    private TOUAbstractImpl impl;

    public TOUSocketInputStream(TOUAbstractImpl impl) {
        this.impl = impl;
    }

    @Override
    public int read() throws IOException {
        return impl.readByte();
    }


}
