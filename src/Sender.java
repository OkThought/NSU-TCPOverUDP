import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;

public class Sender {
	private DataOutputStream out;

	public Sender(OutputStream out) {
		this.out = new DataOutputStream(out);
	}

	public void send(long size) throws IOException {
		out.writeLong(size);
	}

	public void send(int size) throws IOException {
		out.writeInt(size);
	}

	public void sendString(String str) throws IOException {
		byte[] bytes = str.getBytes(Charset.forName("UTF-8"));
		out.writeInt(bytes.length);
		out.write(bytes);
	}

	public void send(InputStream in, int bufferSize) throws IOException {
		byte[] buffer = new byte[bufferSize];
		int bytesRead;
		while ((bytesRead = in.read(buffer)) > 0) {
			out.write(buffer, 0, bytesRead);
		}
	}
}
