package eu.linksmart.gc.network.backbone.zmq;

public class ZmqConstants {
	
	public static final String HEARTBEAT_TOPIC = "HEARTBEAT";
	public static final String BROADCAST_TOPIC = "BROADCAST";
	
	public static final String MESSAGE_TYPE_UNICAST = "0x01";
	public static final String MESSAGE_TYPE_DISCOVERY = "0x02";
	public static final String MESSAGE_TYPE_HEART_BEAT = "0x03";
	public static final String MESSAGE_TYPE_PEER_DOWN = "0x04";

}
