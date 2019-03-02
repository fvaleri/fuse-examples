package it.fvaleri.integ;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import javax.websocket.ClientEndpoint;
import javax.websocket.CloseReason;
import javax.websocket.DeploymentException;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;

import org.glassfish.tyrus.client.ClientManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ClientEndpoint
public class Application {
    private static final Logger LOG = LoggerFactory.getLogger(Application.class);
    private static CountDownLatch latch;

    private static final int PING_PERIOD = 1_000;
    private static final int PONG_PERIOD = 1_000;

    private static final String BROKER_URL_PROPERTY = "brokerURL";
    private static final String QUEUE_NAME_PROPERTY = "queueName";
    private static final String LOGIN_PROPERTY = "brokerUserLogin";
    private static final String PASSCODE_PROPERTY = "brokerUserPasscode";

    private static final String DEFAULT_LOGIN = "Not specified";
    private static final String DEFAULT_PASSCODE = "Not specified";
    private static final String DEFAULT_BROKER_URL = "Not specified";
    private static final String DEFAULT_QUEUE_NAME = "Not Specified";

    private static final String SUBSCRIPTION_ID_0 = "id-0";

    private final String login;
    private final String passcode;
    private final String queueName;
    private Timer pinger;
    private Session wsSession;

    public Application(String login, String passcode, String queueName) {
        this.login = login;
        this.passcode = passcode;
        this.queueName = queueName;
        wsSession = null;
    }

    public static void main(String[] args) {
        latch = new CountDownLatch(1);

        String login = System.getProperty(LOGIN_PROPERTY, DEFAULT_LOGIN);
        String passcode = System.getProperty(PASSCODE_PROPERTY, DEFAULT_PASSCODE);
        String queueName = System.getProperty(QUEUE_NAME_PROPERTY, DEFAULT_QUEUE_NAME);
        String brokerURL = System.getProperty(BROKER_URL_PROPERTY, DEFAULT_BROKER_URL);

        Application app = new Application(login, passcode, queueName);
        app.connectToBroker(brokerURL);
        try {
            app.stompSend("TestQueue", "Hello World");
        } catch (Exception e1) {
            LOG.error("Test error", e1);
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @OnOpen
    public void onOpen(Session aWsSession) throws IOException {
        LOG.debug("Websocket connection opened");
        wsSession = aWsSession;
        LOG.info("Opened websocket session with id " + aWsSession.getId());
        stompConnect(Command.CONNECT);
    }

    @OnMessage
    public String onMessage(String message, Session aWsSession) throws IOException {
        if (wsSession != aWsSession) {
            throw new IllegalStateException(
                    "WsSession in onMessage callback not the same as when initiating the ws connection");
        }

        if (message.equals("\n")) {
            LOG.debug("STOMP: PONG <---");
            return null;
        }

        Frame frame = Frame.parse(message);
        onMessage(frame);
        return null;
    }

    @OnMessage
    public String onMessage(ByteBuffer buffer, Session aWsSession) throws IOException {
        if (wsSession != aWsSession) {
            throw new IllegalStateException(
                    "WsSession in onMessage callback not the same as when initiating the ws connection");
        }
        Frame frame = Frame.parse(Frame.toString(buffer));
        onMessage(frame);
        return null;
    }

    // worker for both OnMessage methods
    private void onMessage(Frame frame) throws IOException {
        LOG.debug("Received STOMP frame:\n{}\n", frame);
        switch (frame.getCommand()) {
        case CONNECTED:
            setupHeartbeat(PING_PERIOD);
            stompSubscribe(SUBSCRIPTION_ID_0, queueName, Ack.auto);
            break;
        case DISCONNECTED: // intentional fall-through as no special handling of these frames is implemented
        case MESSAGE:
        case RECEIPT:
        case ERROR:
            break;
        default:
            LOG.warn("UNKNOWN FRAME");
            break;
        }
    }

    @OnClose
    public void onClose(Session aWsSession, CloseReason closeReason) {
        if (wsSession != aWsSession) {
            throw new IllegalStateException(
                    "WsSession in onClose callback not the same as when initiating the ws connection");
        }
        if (pinger != null) {
            pinger.cancel();
        }
        latch.countDown();
        LOG.info("Session {} close because of {}", aWsSession.getId(), closeReason);
    }

    private String connectToBroker(String aBrokerURL) {
        if (wsSession != null && wsSession.isOpen()) {
            throw new IllegalStateException("A WebSocket connection is already opened for this instance: " + this);
        }

        ClientManager clientManager = ClientManager.createClient();
        // tyrus is optimized for small messages, enable the following row to increase the incoming buffer size
        //clientManager.getProperties().put(ClientProperties.INCOMING_BUFFER_SIZE, 4_194_304+1_024); // 4M +
        Session newWsSession;
        try {
            LOG.info("Websocket connection initiated towards [" + aBrokerURL + "]");
            newWsSession = clientManager.connectToServer(this, new URI(aBrokerURL));
        } catch (URISyntaxException | DeploymentException | IOException ex) {
            LOG.error("Error setting up WebSocket connection :", ex);
            throw new IllegalStateException("Error setting up WebSocket connection : " + ex);
        }
        return newWsSession.getId();
    }

    private void stompConnect(Command stompCommand) throws IOException {
        if (stompCommand == Command.STOMP || stompCommand == Command.CONNECT) {
            Frame frame = new Frame(stompCommand);
            frame.getHeader().put("accept-version", "1.2,1.1,1.0");
            frame.getHeader().put("login", login);
            frame.getHeader().put("passcode", passcode);
            frame.getHeader().put("heart-beat", PING_PERIOD + "," + PONG_PERIOD);
            send(frame);
        } else {
            LOG.error("STOMP: Illegal stomp command for connect {}" + stompCommand);
        }
    }

    private void stompSubscribe(String subscribeId, String queueName, Ack ack) throws IOException {
        Frame frame = new Frame(Command.SUBSCRIBE);
        frame.getHeader().put("id", subscribeId); // Mandatory
        frame.getHeader().put("destination", queueName); // Mandatory
        frame.getHeader().put("ack", ack.nameWithConversion()); // Optional
        send(frame);
        LOG.info("STOMP: subscribed to {}", queueName);
    }

    private void send(Frame stompFrame) throws IOException {
        if (wsSession != null && wsSession.isOpen()) {
            LOG.debug("Send STOMP frame:\n{}\n", stompFrame);
            wsSession.getBasicRemote().sendText(stompFrame.getText());
        } else {
            LOG.error("Send failed, WsSession is not active");
        }
    }

    private void setupHeartbeat(int heartbeat) {
        if (heartbeat == 0) {
            return;
        }
        LOG.info("STOMP: PING every {} ms", heartbeat);
        HeartBeatTimer heartBeatTimer = new HeartBeatTimer(wsSession);
        pinger = new Timer();
        pinger.scheduleAtFixedRate(heartBeatTimer, heartbeat, heartbeat);
    }

    static class HeartBeatTimer extends TimerTask {
        private final Session wsSession;
        private final Logger logger = LoggerFactory.getLogger(this.getClass().getSimpleName());

        HeartBeatTimer(Session wsSession) {
            this.wsSession = wsSession;
        }

        @Override
        public void run() {
            try {
                wsSession.getBasicRemote().sendText("\n");
            } catch (Exception ex) {
                LOG.error("Heartbeat error", ex);
            }
            logger.debug(">>> PING");
        }
    }

    private void stompUnsubscribe(String aSubscribeId) throws IOException {
        Frame frame = new Frame(Command.UNSUBSCRIBE);
        frame.getHeader().put("id", aSubscribeId);
        LOG.debug("STOMP: sendAck {}", frame);
        send(frame);
    }

    private void stompAck(String anAckHeader) throws IOException {
        Frame frame = new Frame(Command.ACK);
        frame.getHeader().put("id", anAckHeader);
        LOG.debug("STOMP: sendAck {}", frame);
        send(frame);
    }

    private void stompNack(String anAckHeader) throws IOException {
        Frame frame = new Frame(Command.NACK);
        frame.getHeader().put("id", anAckHeader);
        LOG.debug("STOMP: sendNack {}", frame);
        send(frame);
    }

    private void stompSend(String aQueueName, String aText) throws IOException {
        Frame frame = new Frame(Command.SEND);
        frame.getHeader().put("destination", aQueueName);
        frame.setBody(aText);
        LOG.debug("STOMP: send {}", frame);
        send(frame);
    }

    private void stompDisconnect() throws IOException {
        Frame frame = new Frame(Command.DISCONNECT);
        if (pinger != null) {
            pinger.cancel();
        }
        LOG.debug("STOMP: sendDisconnect {}", frame);
        send(frame);
    }
}

