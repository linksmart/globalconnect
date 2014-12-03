package eu.linksmart.global.backbone.zmq;

/**
 * Created by carlos on 25.11.14.
 */
public class Constants {

    public static final String mXSUB = "tcp://localhost:7000";
    public static final String mXPUB = "tcp://localhost:7001";
    public static final String CAPTURE_SOCKET = "ipc://backbone";
    public static final String mTopic = "AAA";

    public static final String HEARTBEAT_TOPIC            = "HEARTBEAT";
    public static final String BROADCAST_TOPIC            = "BROADCAST";
    public static final long   HEARTBEAT_INTERVAL         = 2000;
    public static final long   HEARTBEAT_TIMEOUT          = 5000;
    public static final byte   MSG_UNICAST                = 0x01;
    public static final byte   MSG_DISCOVERY              = 0x02;
    public static final byte   MSG_HEARTBEAT              = 0x03;
    public static final byte   MSG_PEERDOWN               = 0x04;

}
