package eu.linksmart.gc.network.backbone.zmq;

import eu.linksmart.network.VirtualAddress;

public class BackboneMessage {
	
	private VirtualAddress senderVirtualAddress = null;
	private VirtualAddress receiverVirtualAddress = null;
	private byte[] data = null;
	
	public BackboneMessage(VirtualAddress senderVirtualAddress, VirtualAddress receiverVirtualAddress, byte[] data) {
		this.senderVirtualAddress = senderVirtualAddress;
		this.receiverVirtualAddress = receiverVirtualAddress;
		this.data = data;
	}
	
	public VirtualAddress getSenderVirtualAddress() {
		return this.senderVirtualAddress;
	}
	
	public VirtualAddress getReceiverVirtualAddress() {
		return this.receiverVirtualAddress;
	}
	
	public byte[] getPayload() {
		return this.data;
	}
	
}
