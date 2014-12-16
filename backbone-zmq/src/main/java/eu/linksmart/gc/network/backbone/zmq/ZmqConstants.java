package eu.linksmart.gc.network.backbone.zmq;

public class ZmqConstants {
	
	public static final byte PROTOCOL_VERSION = 0x01;
	
	public static final String HEARTBEAT_TOPIC = "HEARTBEAT";
	public static final String BROADCAST_TOPIC = "BROADCAST";
	
	public static final byte MESSAGE_TYPE_UNICAST = 0x01;
	public static final byte MESSAGE_TYPE_PEER_DISCOVERY = 0x02;
	public static final byte MESSAGE_TYPE_HEART_BEAT = 0x03;
	public static final byte MESSAGE_TYPE_PEER_DOWN = 0x04;
	public static final byte MESSAGE_TYPE_SERVICE_DISCOVERY = 0x05;
	
	public static final String TYPE_REQUEST = "REQUEST";
	public static final String TYPE_RESPONSE = "RESPONSE";
	
}
