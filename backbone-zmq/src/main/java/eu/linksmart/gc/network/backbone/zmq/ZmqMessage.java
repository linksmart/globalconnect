package eu.linksmart.gc.network.backbone.zmq;

import java.util.Date;

public class ZmqMessage {
	
	private String topic;
	private byte type;
	private Date timeStamp;
	private String sender;
	private byte[] payload;
	
	public ZmqMessage() {	
	}
	
	public ZmqMessage(String topic, byte type, Date timeStamp, String sender, byte[] payload) {
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
	
	public void setType(byte type) {
		this.type = type;
	}
	
	public byte getType() {
		return this.type;
	}
	
	public void setTimeStamp(Date timeStamp) {
		this.timeStamp = timeStamp;
	}
	
	public Date getTimeStamp() {
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
