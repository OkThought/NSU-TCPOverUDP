package ru.nsu.ccfit.bogush.tou;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

public class TOULog4JUtils {
    private static final String CONFIG_FILE = "logging.xml";
    private static volatile boolean init = false;

    private TOULog4JUtils() {}

    static void initIfNotInitYet() {
        if (!init) {
            init = true;
            System.setProperty("log4j.configurationFile", CONFIG_FILE);
        }
    }

    public static String toString(DatagramSocket socket) {
        return String.format("udp socket <%s:%d, %s:%s>",
                socket.getLocalAddress(), socket.getLocalPort(), socket.getInetAddress(), socket.getPort());
    }

    public static Object toString(DatagramPacket packet) {
        return String.format("udp packet <%s:%d size: %d>", packet.getAddress(), packet.getPort(), packet.getLength());
    }

    public static String toString(SocketAddress socketAddress) {
        return String.format("%s:%s",
                ((InetSocketAddress) socketAddress).getAddress(),
                ((InetSocketAddress) socketAddress).getPort());
    }
}
