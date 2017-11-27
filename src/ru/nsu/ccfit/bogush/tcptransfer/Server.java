package ru.nsu.ccfit.bogush.tcptransfer;

import ru.nsu.ccfit.bogush.tou.TOUServerSocket;
import ru.nsu.ccfit.bogush.tou.TOUSocket;

import java.io.IOException;

public class Server extends Thread {
	private TOUServerSocket socket;

	public Server(int port) throws IOException {
		super(Server.class.getName());
		socket = new TOUServerSocket(port);
	}

	@Override
	public synchronized void start() {
		super.start();
	}

	@Override
	public void run() {
		try {
			while (!Thread.interrupted()) {
				TOUSocket client = socket.accept();
				FileReceiver fileReceiver = new FileReceiver(client);
				fileReceiver.start();
				SpeedTester speedTester = new SpeedTester(fileReceiver, 3000);
				fileReceiver.addStopListener(speedTester);
				speedTester.start();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	private static final int MIN_ARGS_SIZE = 1;

	private static void usage() {
		System.out.println("Usage: server port");
		System.out.println("\tport - number representing the port on which the server runs.");
		System.out.println("");
		System.out.println("Description");
		System.out.println("\tReceives a file from client using TCP.");
	}

	public static void main(String[] args) {
		if (args.length < MIN_ARGS_SIZE) {
			usage();
			return;
		}

		int port = Integer.parseInt(args[0]);
		try {
			Server server = new Server(port);
			server.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
