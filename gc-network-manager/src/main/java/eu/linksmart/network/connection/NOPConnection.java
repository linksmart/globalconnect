package eu.linksmart.network.connection;

import eu.linksmart.network.VirtualAddress;
import eu.linksmart.network.Message;

/**
 * {@link Connection} which does not encode provided messages or data.
 * @author Vinkovits
 *
 */
public class NOPConnection extends Connection {

	public NOPConnection(VirtualAddress clientVirtualAddress, VirtualAddress serverVirtualAddress) {
		super(clientVirtualAddress, serverVirtualAddress);
	}
	
	public byte[] processMessage(Message msg) throws Exception{
		return msg.getData();
	}
	
	public Message processData(VirtualAddress senderVirtualAddress, VirtualAddress receiverVirtualAddress, byte[] data){
		return new Message(Message.TOPIC_APPLICATION, senderVirtualAddress, receiverVirtualAddress, data);
	}

}
