package eu.linksmart.gc.network.backbone.zmq;

import java.nio.ByteBuffer;

import org.apache.commons.lang.ArrayUtils;

import eu.linksmart.network.VirtualAddress;

public class ZmqUtil {
	
	public static ZmqMessage toZmqMessage(byte[][] multipart) {
		return null;
	}
	
	public static byte[][] toZmqBytes(ZmqMessage message) {
		return null;
	}
	
	public static void addSenderVAD(BackboneMessage bbMessage) {
		ByteBuffer aBuffer = ByteBuffer.allocate(VirtualAddress.VIRTUAL_ADDRESS_BYTE_LENGTH	+ bbMessage.getPayload().length);
		aBuffer.position(0);
		aBuffer.put(bbMessage.getSenderVirtualAddress().getBytes());
		aBuffer.put(bbMessage.getPayload());
		bbMessage.setPayload(aBuffer.array());
	}
	
	public static VirtualAddress getSenderVAD(byte[] origData) {
		byte[] ret = new byte[VirtualAddress.VIRTUAL_ADDRESS_BYTE_LENGTH];
		System.arraycopy(origData, 0, ret, 0, VirtualAddress.VIRTUAL_ADDRESS_BYTE_LENGTH - 0);
		return new VirtualAddress(ret);
	}
	
	public static byte[] removeSenderVAD(byte[] origData) {
		int lengthWithoutVirtualAddress = origData.length - VirtualAddress.VIRTUAL_ADDRESS_BYTE_LENGTH;
		byte[] ret = new byte[lengthWithoutVirtualAddress];
		System.arraycopy(origData, VirtualAddress.VIRTUAL_ADDRESS_BYTE_LENGTH, ret, 0, origData.length - VirtualAddress.VIRTUAL_ADDRESS_BYTE_LENGTH);
		return ret;
	}
	
	public static void addVADsToPayload(BackboneMessage bbMessage) {
		byte[] payloadWithReceiverVAD = ArrayUtils.addAll(bbMessage.getReceiverVirtualAddress().getBytes(),bbMessage.getPayload());
		byte[] payloadWithVADs = ArrayUtils.addAll(bbMessage.getSenderVirtualAddress().getBytes(),payloadWithReceiverVAD);
		bbMessage.setPayload(payloadWithVADs);
	}
	
}
