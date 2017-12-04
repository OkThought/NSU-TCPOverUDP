package ru.nsu.ccfit.bogush.tcptransfer;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class FileReceiver extends Thread implements SizeObserver {
	private static final int DEFAULT_BUFFER_SIZE = 2 << 20;
	private static final String REGEXP_FILE_PATTERN = "(?<name>.+?)(?>-(?<copies>\\d+))?(?<extension>\\..+)";
	private static final String UPLOADS = "uploads/";
	private ArrayList<StopListener> stopListeners = new ArrayList<>();
	private Socket client;
	private InputStream in;
	private OutputStream out;
	private Receiver receiver;
	private long fileSize;
	private long bytesReceived = 0;
	private int bufferSize;

	public FileReceiver(Socket client) throws IOException {
		this(client, DEFAULT_BUFFER_SIZE);
	}

	public FileReceiver(Socket client, int bufferSize) throws IOException {
		super(FileReceiver.class.getSimpleName());
		this.client = client;
		in = client.getInputStream();
		out = client.getOutputStream();
		receiver = new Receiver(in);
		this.bufferSize = bufferSize;
	}

	@Override
	public synchronized void start() {
		Path dirPath = Paths.get(UPLOADS);
		if (!Files.exists(dirPath)) {
			try {
				Files.createDirectory(dirPath);
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}
		}
		super.start();
	}

	private File getFile(String fullFilename) {
		String filename = UPLOADS + Paths.get(fullFilename).getFileName().toString();
		FilenameProcessor fp = new FilenameProcessor(filename);
		filename = fp.getName();
		if (filename == null) return null;
		fp.slideThroughExistingCopies();
		return fp.getFile();
	}

	private void fail() throws IOException {
		new Sender(out).sendString("FAILED");
		interrupt();
	}

	private void succeed() throws IOException {
		new Sender(out).sendString("SUCCEEDED");
		interrupt();
	}

	@Override
	public void run() {
		try {
			String filename = receiver.receiveString();
			fileSize = receiver.receiveLong();
			File file = getFile(filename);
			if (file == null) {
				System.out.println("Filename didn't match the pattern");
				fail();
				return;
			}
			try (FileOutputStream out = new FileOutputStream(file)) {
				receiver.receive(out, bufferSize, fileSize);
			} catch (IOException e) {
				e.printStackTrace();
				fail();
				return;
			}
			succeed();
		} catch (IOException e) {
			e.printStackTrace();
			interrupt();
		}
	}

	@Override
	public void interrupt() {
		try {
			for (StopListener stopListener: stopListeners) {
				stopListener.stopped();
			}
			client.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		super.interrupt();
	}

	@Override
	public long getTotalSize() {
		return fileSize;
	}

	@Override
	public long getCurrentSize() {
		return receiver.getReceived();
	}

	public void addStopListener(StopListener stopListener) {
		stopListeners.add(stopListener);
	}
}
