package eu.linksmart.gc.network.backbone.zmq;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.zeromq.ZMQ;

import eu.linksmart.network.NMResponse;
import eu.linksmart.network.VirtualAddress;

public class ZmqHandler {
	
	private static Logger LOG = Logger.getLogger(ZmqReceiver.class.getName());

	private String peerID = null;
	//private String xsubUri = "tcp://gando.fit.fraunhofer.de:6000";
	//private String xpubUri = "tcp://gando.fit.fraunhofer.de:6001";
	private String xsubUri = "tcp://127.0.0.1:7000";
	private String xpubUri = "tcp://127.0.0.1:7001";
	
	private int heartBeatInterval = 2000;
	
	private HeartBeat heartBeat = null;
	private ZmqReceiver receiver = null;
	
	private ZMQ.Context pub_context = null;
	private ZMQ.Socket publisher = null;
	
	private BackboneZMQImpl zmqBackbone = null;
	
	Map<VirtualAddress, String> remotePeers = Collections.synchronizedMap(new HashMap<VirtualAddress, String>());
	
	Map<String, BackboneMessage> requests = Collections.synchronizedMap(new HashMap<String, BackboneMessage>());
	
	
	public ZmqHandler(BackboneZMQImpl zmqBackbone) {
		this.zmqBackbone = zmqBackbone;
		this.peerID = UUID.randomUUID().toString();
	}
	
	public ZmqHandler(BackboneZMQImpl zmqBackbone, String peerID) {
		this.zmqBackbone = zmqBackbone;
		this.peerID = peerID;
	}
	
	public ZmqHandler(BackboneZMQImpl zmqBackbone, String xpubUri, String xsubUri) {
		this.zmqBackbone = zmqBackbone;
		this.peerID = UUID.randomUUID().toString();
		this.xpubUri = xpubUri;
		this.xsubUri = xsubUri;
	}
	
	public ZmqHandler(BackboneZMQImpl zmqBackbone, String peerID, String xpubUri, String xsubUri) {
		this.zmqBackbone = zmqBackbone;
		this.peerID = peerID;
		this.xpubUri = xpubUri;
		this.xsubUri = xsubUri;
	}
	
	public void start() {
		heartBeat = new HeartBeat(this);
		heartBeat.start();
		receiver = new ZmqReceiver(this);
		receiver.start();
		initZmqPublisher();
		LOG.info("ZmqPeer [" + this.peerID + "] is started");
	}
	
	public void stop() {
		heartBeat.setIsRunning(false);
		receiver.stopReceiver();
		stopZmqPublisher();
		LOG.info("ZmqPeer [" + this.peerID + "] is stopped");
	}
	
	private void initZmqPublisher() {
		try {
			pub_context = ZMQ.context(1);
			publisher = pub_context.socket(ZMQ.PUB);
			publisher.connect(this.getXSubUri());
		} catch (Exception e) {
			LOG.error("error in initializing ZmqPublisher: " + e.getMessage(), e);
		}	
	}
	
	private void stopZmqPublisher() {
		if(pub_context != null && publisher != null) {
			publisher.setLinger(100);
			publisher.close();
			pub_context.term();
		}
	}
	
	public NMResponse broadcast(BackboneMessage bbMessage) {
		
		publish(createBroadcastMessage(ZmqUtil.addSenderVADToPayload(bbMessage)));
		
		NMResponse response = new NMResponse();
		response.setStatus(NMResponse.STATUS_SUCCESS);
		response.setMessage("Broadcast successful");
		
		return response;
	}
	
	public NMResponse send(BackboneMessage bbMessage, boolean synchronous) {
		
		NMResponse response = new NMResponse();
		
		if(synchronous) {
			LOG.error("synchronous invocation is not supported yet.");
			response.setStatus(NMResponse.STATUS_ERROR);
			response.setMessage("synchronous invocation is not supported yet");
			return response;
		}
		
		String receiverPeerID = this.remotePeers.get(bbMessage.getReceiverVirtualAddress());
		if(receiverPeerID == null) {
			response.setStatus(NMResponse.STATUS_ERROR);
			response.setMessage("unable to find PeerID for receiver virtual address: " + bbMessage.getReceiverVirtualAddress());
			return response;
		}
		
		publish(createPeerMessage(receiverPeerID, ZmqUtil.addVADsToPayload(bbMessage)));
		
		requests.put(UUID.randomUUID().toString(), bbMessage);
		
		response.setStatus(NMResponse.STATUS_SUCCESS);
		response.setMessage("sending message is successful");
		
		return response;
	}
	
	public NMResponse receive(ZmqMessage zmqMessage) {
		
		byte[] originalPayload = ZmqUtil.removeVADsFromPayload(zmqMessage.getPayload());
		VirtualAddress senderVA = ZmqUtil.getSenderVAD(zmqMessage.getPayload());
		VirtualAddress recieverVA = ZmqUtil.getReceiverVAD(zmqMessage.getPayload());
		
		BackboneMessage originalRequest = requests.get(zmqMessage.getRequestID());
		
		if(originalRequest != null) {
			return zmqBackbone.receiveDataAsynch(originalRequest.getReceiverVirtualAddress(), originalRequest.getSenderVirtualAddress(), originalPayload);
		}
		
		return zmqBackbone.receiveDataAsynch(senderVA, recieverVA, originalPayload);
	}
	
	public void notify(ZmqMessage zmqMessage) {
		if(zmqMessage.getTopic().equals(ZmqConstants.BROADCAST_TOPIC)) {
			if(zmqMessage.getType() == ZmqConstants.MESSAGE_TYPE_PEER_DISCOVERY) {
				processDiscovery(zmqMessage);
			} else if(zmqMessage.getType() == ZmqConstants.MESSAGE_TYPE_PEER_DOWN) {
				processPeerDown(zmqMessage);
			}
		} else if(zmqMessage.getTopic().equals(this.getPeerID())) {
			processMessage(zmqMessage);
		} else {
            LOG.warn("unknown topic [" + zmqMessage.getTopic() + "] detected. ignoring");
        }
	}
	
	private void processDiscovery(ZmqMessage zmqMessage) {
		
		if(zmqMessage.getSender().equals(this.peerID)) 
			return;
		
		VirtualAddress senderVirtualAddress = ZmqUtil.getSenderVAD(zmqMessage.getPayload());
		LOG.info("recieved broadcast message from [" + zmqMessage.getSender() + "] - virtual-address: " + senderVirtualAddress.toString());
		
		if(!(remotePeers.containsKey(senderVirtualAddress))) {
			remotePeers.put(senderVirtualAddress, zmqMessage.getSender());
			LOG.info("added peer [" + zmqMessage.getSender() + "] into list of known peers");
		}
			
		byte[] payload = ZmqUtil.removeSenderVAD(zmqMessage.getPayload());
		zmqBackbone.receiveDataAsynch(senderVirtualAddress, null, payload);
	}
	
	private void processPeerDown(ZmqMessage zmqMessage) {
		LOG.info("recieved peerdown message from [" + zmqMessage.getSender() + "]");
		Iterator<String> iterator = remotePeers.values().iterator();
		while(iterator.hasNext()) {
		    String value = iterator.next();
		    if(value.equals(zmqMessage.getSender())) {
				iterator.remove();
				LOG.info("removing peer [" + zmqMessage.getSender() + "] from list");
				break;
			}
		}
	}
	
	private void processMessage(ZmqMessage zmqMessage) {
		LOG.info("recieved unicast message from [" + zmqMessage.getSender() + "] - request-ID : " + zmqMessage.getRequestID());
		receive(zmqMessage);
	}
	
	public void publish(ZmqMessage zmqMessage) {
		publisher.sendMore(zmqMessage.getTopic());
		publisher.sendMore(new byte[]{zmqMessage.getProtocolVersion()});
		publisher.sendMore(new byte[]{zmqMessage.getType()});
		publisher.sendMore(ByteBuffer.allocate(8).putLong(zmqMessage.getTimeStamp()).array());
		publisher.sendMore(zmqMessage.getSender());
		publisher.sendMore(zmqMessage.getRequestID());
		publisher.send(zmqMessage.getPayload(), 0);
	}
	
	public ZmqMessage createBroadcastMessage(byte[] payload) {
		return new ZmqMessage(ZmqConstants.BROADCAST_TOPIC, ZmqConstants.MESSAGE_TYPE_PEER_DISCOVERY, System.currentTimeMillis(), this.peerID, "", payload);
	}
	
	public ZmqMessage createPeerMessage(String peerID, byte[] payload) {
		return new ZmqMessage(peerID, ZmqConstants.MESSAGE_TYPE_UNICAST, System.currentTimeMillis(), this.peerID, UUID.randomUUID().toString(), payload);
	}

	public String getPeerID() {
		return this.peerID;
	}
	
	public String getXPubUri() {
		return this.xpubUri;
	}
	
	public String getXSubUri() {
		return this.xsubUri;
	}
	
	public int getHeartBeatInterval() {
		return this.heartBeatInterval;
	}
	
	public void setHeartBeatInterval(int heartBeatTimer) {
		this.heartBeatInterval = heartBeatTimer;
	}
}
