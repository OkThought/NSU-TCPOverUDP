package ru.nsu.ccfit.bogush.tcptransfer;

import ru.nsu.ccfit.bogush.tou.TOUSocketImplFactory;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

class Client extends Thread {
	private static final int BUFFER_SIZE = 2 << 20;
	private Socket socket;
	private String filename = null;
	private InputStream in;
	private OutputStream out;

	private Client(InetAddress address, int serverPort, String filename) throws IOException {
		super(Client.class.getSimpleName());
		this.filename = filename;
		socket = new Socket(address, serverPort);
	}

	@Override
	public synchronized void start() {
		if (filename == null) {
			System.out.println("Set a filename to send at first");
			return;
		}
		try {
			in = socket.getInputStream();
			out = socket.getOutputStream();
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		super.start();
	}

	@Override
	public void run() {
		try {
			Sender sender = new Sender(out);
			Path path = Paths.get(filename);
			File file = path.toFile();
			sender.sendString(filename);
			sender.send(Files.size(path));
			sender.send(new FileInputStream(file), BUFFER_SIZE);
			readMessages();
			interrupt();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void readMessages() throws IOException {
		Receiver receiver = new Receiver(in);
		try {
			while (!Thread.interrupted()) {
				System.out.println(receiver.receiveString());
			}
		} catch (EOFException e) {
			interrupt();
		}
	}

	@Override
	public void interrupt() {
		try {
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		super.interrupt();
	}



	private static final int MIN_ARGS_SIZE = 3;

	private static void usage() {
		System.out.println("Usage: client port file [frame]");
		System.out.println("\tfile - name of a file to send.");
		System.out.println("\tip - ip address or dns name of the server.");
		System.out.println("\tport - number representing the port on which the server runs.");
		System.out.println("");
		System.out.println("Description");
		System.out.println("\tSends a file to server using TCP.");
	}

	public static void main(String[] args) {
		if (args.length < MIN_ARGS_SIZE) {
			usage();
			return;
		}
		String filename = args[0];
		String serverAddress = args[1];
		int serverPort = Integer.parseInt(args[2]);
		try {
			Socket.setSocketImplFactory(new TOUSocketImplFactory());
			Client client = new Client(InetAddress.getByName(serverAddress), serverPort, filename);
			client.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}