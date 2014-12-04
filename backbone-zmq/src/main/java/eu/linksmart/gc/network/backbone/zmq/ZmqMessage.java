package eu.linksmart.gc.network.backbone.zmq;

public class ZmqMessage {
	
	private String topic;
	private String type;
	private long timeStamp;
	private String sender;
	private byte[] payload;
	
	public ZmqMessage() {	
	}
	
	public ZmqMessage(String topic, String type, long timeStamp, String sender, byte[] payload) {
		this.topic = topic;
		this.type = type;
		this.timeStamp = timeStamp;
		this.sender = sender;
		this.payload = payload;
	}
	
	public void setTopic(String topic) {
		this.topic = topic;
	}
	
	public String getTopic() {
		return this.topic;
	}
	
	public void setType(String type) {
		this.type = type;
	}
	
	public String getType() {
		return this.type;
	}
	
	public void setTimeStamp(long timeStamp) {
		this.timeStamp = timeStamp;
	}
	
	public long getTimeStamp() {
		return this.timeStamp;
	}
	
	public void setSender(String sender) {
		this.sender = sender;
	}
	
	public String getSender() {
		return this.sender;
	}
	
	public void setPayload(byte[] payload) {
		this.payload = payload;
	}
	
	public byte[] getPayload() {
		return this.payload;
	}
}
