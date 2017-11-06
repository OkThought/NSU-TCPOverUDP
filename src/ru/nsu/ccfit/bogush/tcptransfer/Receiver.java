package ru.nsu.ccfit.bogush.tcptransfer;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;

public class Receiver {
	private DataInputStream in;
	private long received = 0;

	public Receiver(InputStream in) {
		this.in = new DataInputStream(in);
	}

	public String receiveString() throws IOException {
		int len = in.readInt();
		byte[] bytes = new byte[len];
		in.readFully(bytes);
		return new String(bytes, Charset.forName("UTF-8"));
	}

	public int receiveInt() throws IOException {
		return in.readInt();
	}

	public long receiveLong() throws IOException {
		return in.readLong();
	}

	private synchronized void reset() {
		received = 0;
	}

	private synchronized void increase(int val) {
		received += val;
	}

	public void receive(OutputStream out, int bufferSize, long totalSize) throws IOException {
		byte[] buffer = new byte[bufferSize];
		reset();
		int len;
		while(totalSize > received && (len = in.read(buffer)) > 0) {
			increase(len);
			out.write(buffer, 0, len);
		}
	}

	public synchronized long getReceived() {
		return received;
	}
}
