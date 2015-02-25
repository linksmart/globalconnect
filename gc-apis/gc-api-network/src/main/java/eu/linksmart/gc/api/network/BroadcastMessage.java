package eu.linksmart.network;

public class BroadcastMessage extends Message {
	public BroadcastMessage(String topic, VirtualAddress senderVirtualAddress, byte[] data){
		super(topic, senderVirtualAddress, null, data);
	}
}
