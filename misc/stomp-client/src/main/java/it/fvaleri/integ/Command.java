package it.fvaleri.integ;

enum Command {
    // client-commands
    CONNECT, STOMP, SEND, SUBSCRIBE, UNSUBSCRIBE, BEGIN, COMMIT, ABORT, ACK, NACK, DISCONNECT,
    // server-commands
    CONNECTED, MESSAGE, RECEIPT, ERROR, DISCONNECTED
}
