package ru.nsu.ccfit.bogush.tou;


public enum TOUConnectionState {
    /**
     * (server) represents waiting for a connection request from any
     * remote TCP and port.
     */
    LISTEN,

    /**
     * (client) represents waiting for a matching connection request
     * after having sent a connection request.
     */
    SYN_SENT,

    /**
     * (server) represents waiting for a confirming connection request
     * acknowledgment after having both received and sent a connection
     * request.
     */
    SYN_RECEIVED,

    /**
     * (both server and client) represents an open connection, data
     * received can be delivered to the user. The normal state for the
     * data transfer phase of the connection.
     */
    ESTABLISHED,

    /**
     * (both server and client) represents waiting for a connection
     * termination request from the remote TCP, or an acknowledgment of
     * the connection termination request previously sent.
     */
    FIN_WAIT_1,

    /**
     * (both server and client) represents waiting for a connection
     * termination request from the remote TCP.
     */
    FIN_WAIT_2,

    /**
     * (both server and client) represents waiting for a connection
     * termination request from the local user.
     */
    CLOSE_WAIT,

    /**
     * (both server and client) represents waiting for a connection
     * termination request acknowledgment from the remote TCP.
     */
    CLOSING,

    /**
     * (both server and client) represents waiting for an
     * acknowledgment of the connection termination request previously
     * sent to the remote TCP (which includes an acknowledgment of its
     * connection termination request).
     */
    LAST_ACK,

    /**
     * (either server or client) represents waiting for enough time to
     * pass to be sure the remote TCP received the acknowledgment of its
     * connection termination request. [According to RFC 793 a
     * connection can stay in TIME-WAIT for a maximum of four minutes
     * known as two MSL (maximum segment lifetime).]
     */
    TIME_WAIT,

    /**
     * (both server and client) represents no connection state at all.
     */
    CLOSED;
}
