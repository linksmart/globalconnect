package eu.linksmart.gc.network.backbone.zmq;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import org.apache.log4j.Logger;

import eu.linksmart.network.NMResponse;
import eu.linksmart.network.VirtualAddress;

public class ZmqHandler {
	
	private static Logger LOG = Logger.getLogger(ZmqReceiver.class.getName());

	private String peerID = null;
	//private String xsubUri = "tcp://gando.fit.fraunhofer.de:7000";
	//private String xpubUri = "tcp://gando.fit.fraunhofer.de:7001";
	private String xsubUri = "tcp://129.26.162.10:7000";
	private String xpubUri = "tcp://129.26.162.10:7001";
	
	private int heartBeatInterval = 2000;
	
	private HeartBeat heartBeat = null;
	private ZmqReceiver receiver = null;
	private ZmqPublisher publisher = null;
	
	private BackboneZMQImpl zmqBackbone = null;
	
	private Map<VirtualAddress, String> remotePeers = Collections.synchronizedMap(new HashMap<VirtualAddress, String>());
	
	private Map<Integer, MessageSender> requestIdSenders = Collections.synchronizedMap(new HashMap<Integer, MessageSender>());
	
	Integer counter = 0;

	private int MAX_RESPONSE_TIME = 60000;
	
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
		publisher = new ZmqPublisher(this);
		publisher.startPublisher();
		LOG.info("ZmqPeer [" + this.peerID + "] is started");
	}
	
	public void stop() {
		heartBeat.setIsRunning(false);
		receiver.stopReceiver();
		publisher.stopPublisher();
		LOG.info("ZmqPeer [" + this.peerID + "] is stopped");
	}
	
	public void notify(ZmqMessage zmqMessage) {
		new MessageProcessor(zmqMessage).start();
	}
	
	public NMResponse broadcast(BackboneMessage bbMessage) {
		
		LOG.info("sending broadcast message from virtual-address: " + bbMessage.getSenderVirtualAddress());
		
		publisher.publish(ZmqUtil.createBroadcastMessage(this.peerID, ZmqUtil.addSenderVADToPayload(bbMessage)));
		
		NMResponse response = new NMResponse();
		response.setStatus(NMResponse.STATUS_SUCCESS);
		response.setMessage("Broadcast successful");
		
		return response;
	}
	
	public NMResponse sendData(BackboneMessage bbMessage) {
		
		NMResponse response = new NMResponse();
		
		String receiverPeerID = this.remotePeers.get(bbMessage.getReceiverVirtualAddress());
		
		if(receiverPeerID == null) {
			LOG.error("unable to find PeerID for receiver virtual address: " + bbMessage.getReceiverVirtualAddress());
			response.setStatus(NMResponse.STATUS_ERROR);
			response.setMessage("unable to find PeerID for receiver virtual address: " + bbMessage.getReceiverVirtualAddress());
			return response;
		}
		
		Integer requestID = getRequestIdCounter();
		
		MessageSender messageSender = new MessageSender(requestID);
		
		//
		// store requestid to identify response
		//
		if(bbMessage.isSync()) {
			requestIdSenders.put(requestID, messageSender);
		}
		
		response = messageSender.send(bbMessage, receiverPeerID);

		//
		//block until response comes and then remove id
		//
		if(bbMessage.isSync()) {
			response = messageSender.waitForResponseFromPeer();
			requestIdSenders.remove(requestID);
		}
		
		return response;
	}
	
	private synchronized Integer getRequestIdCounter() {
		counter = counter + 1;
		return counter;
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
	
	public Map<VirtualAddress, String> getRemotePeers() {
		return this.remotePeers;
	}
	
	////////////////////////////////////////////////////////
	
	public class MessageProcessor extends Thread {
		
		private ZmqMessage zmqMessage = null;
		
		public MessageProcessor(ZmqMessage zmqMessage) {
			this.zmqMessage = zmqMessage;
		}

		public void run() {
			if(zmqMessage.getTopic().equals(ZmqConstants.BROADCAST_TOPIC)) {
				if(zmqMessage.getType() == ZmqConstants.MESSAGE_TYPE_SERVICE_DISCOVERY) {
					processDiscovery();
				} else if(zmqMessage.getType() == ZmqConstants.MESSAGE_TYPE_PEER_DOWN) {
					processPeerDown();
				}
			} else if(zmqMessage.getTopic().equals(getPeerID())) {
				processPeerMessage();
			} else {
	            LOG.warn("unknown topic [" + zmqMessage.getTopic() + "] detected. ignoring");
	        }
		}
		
		private void processDiscovery() {
			
			if(zmqMessage.getSender().equals(getPeerID())) 
				return;
			
			VirtualAddress senderVirtualAddress = ZmqUtil.getSenderVAD(zmqMessage.getPayload());
			LOG.info("recieved broadcast message from [" + zmqMessage.getSender() + "] - virtual-address: " + senderVirtualAddress.toString());
			
			if(!(getRemotePeers().containsKey(senderVirtualAddress))) {
				getRemotePeers().put(senderVirtualAddress, zmqMessage.getSender());
				LOG.info("added peer [" + zmqMessage.getSender() + "] into list of known peers");
			} 
			// TODO check if new virtual address is received for already added peer	
			byte[] payload = ZmqUtil.removeSenderVAD(zmqMessage.getPayload());
			zmqBackbone.receiveDataAsynch(senderVirtualAddress, null, payload);
		}
		
		private void processPeerDown() {
			LOG.info("recieved peerdown message from [" + zmqMessage.getSender() + "]");
			Iterator<String> iterator = getRemotePeers().values().iterator();
			while(iterator.hasNext()) {
			    String value = iterator.next();
			    if(value.equals(zmqMessage.getSender())) {
					iterator.remove();
					LOG.info("removing peer [" + zmqMessage.getSender() + "] from list");
					break;
				}
			}
		}
		
		private void processPeerMessage() {
			
			byte[] originalPayload = ZmqUtil.removeVADsFromPayload(zmqMessage.getPayload());
			VirtualAddress senderVA = ZmqUtil.getSenderVAD(zmqMessage.getPayload());
			VirtualAddress recieverVA = ZmqUtil.getReceiverVAD(zmqMessage.getPayload());
			String requestID = zmqMessage.getRequestID();
			
			LOG.info("recieved message from [" + zmqMessage.getSender() + "] - virtualaddress: " + senderVA.toString() + " - request-ID : " + zmqMessage.getRequestID());
			
			if(zmqMessage.getType() == ZmqConstants.MESSAGE_TYPE_UNICAST_REQUEST) {
				
				/*
				 * MESSAGE request arrived. Call the zmqBackbone.receiveData(). Once it has received the status send it as a response
				 */
				LOG.info("REQ: " + " sender-VAD: " + senderVA.toString() + " - receiver-VAD: " + recieverVA.toString() + " - requestID: " + requestID);
				
				NMResponse response = new NMResponse();

				response = zmqBackbone.receiveDataSynch(senderVA, recieverVA, originalPayload);
				LOG.info("REQ: received response from nm/router for requestID: " + requestID);
				 				
				//
				// reverse source and destination because we (dest) send response back to source
				//
				BackboneMessage bbMessage = new BackboneMessage(recieverVA, senderVA, response.getMessageBytes(), true);
				
				LOG.info("REQ: sending response: " + " sender-VAD: " + bbMessage.getSenderVirtualAddress().toString() + " receiver-VAD: " + bbMessage.getReceiverVirtualAddress().toString() + " - requestID: " + requestID);
				
				String receiverPeerID = remotePeers.get(bbMessage.getReceiverVirtualAddress());
				
				if(receiverPeerID == null) {
					LOG.error("REQ: unable to find PeerID for receiver-VAD: " + bbMessage.getReceiverVirtualAddress());
					return;
				}
				
				if(bbMessage.isSync()) {
					if(response.getMessage() == null) 
						response.setMessage("");
					//
					// sending response to incoming request
					//
					ZmqUtil.createResponseMessage(peerID, receiverPeerID, requestID, ZmqUtil.addVADsToPayload(bbMessage));
					boolean success = publisher.publish(ZmqUtil.createResponseMessage(peerID, receiverPeerID, requestID, ZmqUtil.addVADsToPayload(bbMessage)));
					
					if(!success) {
						LOG.error("REQ: Unable to send response to requestID: " + requestID + " from " + recieverVA.toString());
					}
				}

			} else if(zmqMessage.getType() == ZmqConstants.MESSAGE_TYPE_UNICAST_RESPONSE) {
				/*
				 * RESPONSE MESSAGE. NOTIFY the lock (in MessageSender.send(..)). 
				 */
				LOG.info("RES: response message from peer [" + zmqMessage.getSender() + "] - sender-VAD: " + senderVA.toString() + " - requestID: " + Integer.valueOf(requestID));
				MessageSender sender = requestIdSenders.get(Integer.valueOf(requestID));
				if (sender != null) {
					sender.notification(originalPayload);
				}
			} else {
				LOG.error("Received incompatible zmq message with type: ");
			}
		}
	}
	
	/////////////////////////////////////////////////////////////////////
	
	public class MessageSender {
		
		private NMResponse resp = null;
		private Integer requestID;
		private boolean responseReceived = false;

		public MessageSender(int requestID) {
			this.requestID = requestID;
			resp = new NMResponse();
		}

		public void notification(byte[] payload) {
			responseReceived = true;
			LOG.info("received notification for requestID: " + requestID);
			resp.setBytesPrimary(true);
			resp.setMessageBytes(payload);
			synchronized (requestID) {
				requestID.notify();
			}
		}

		public NMResponse waitForResponseFromPeer() {
			synchronized (requestID) {
				try {
					LOG.info("waiting for response to recieve for requestID: " + requestID);
					requestID.wait(MAX_RESPONSE_TIME);
				} catch (InterruptedException e) {
					LOG.error("request timeout for repsonse from remote peer for requestID: " + requestID, e);
				}
			}
			if(!responseReceived) {
				resp.setStatus(NMResponse.STATUS_ERROR);
				resp.setMessage("request timeout for repsonse from remote peer for requestID: " + requestID);
			}
			return resp;
		}

		public NMResponse send(BackboneMessage bbMessage, String receiverPeerID) {

			LOG.info("sending request-message to peer [" + receiverPeerID + "] - receiver-VAD: " + bbMessage.getReceiverVirtualAddress().toString() + " - requestID: " + requestID);
			
			//ZmqMessage zmqMessage = new ZmqMessage(receiverPeerID, ZmqConstants.MESSAGE_TYPE_UNICAST_REQUEST, System.currentTimeMillis(), peerID, requestID.toString(), ZmqUtil.addVADsToPayload(bbMessage));
			
			if(publisher.publish(ZmqUtil.createRequestMessage(peerID, receiverPeerID, requestID.toString(), ZmqUtil.addVADsToPayload(bbMessage)))) {
				resp.setStatus(NMResponse.STATUS_SUCCESS);
				resp.setMessage("<Response>Success sending data to VirtualAddress = " + bbMessage.getReceiverVirtualAddress().toString() + "</Response>");
				LOG.info("message send successfully for requestID: " + requestID);
			} else {
				resp.setStatus(NMResponse.STATUS_ERROR);
				resp.setMessage("<Response>Error in MessageSender</Response>");
				LOG.error("unable to send message for requestID: " + requestID);
			} 
			return resp;
		}

	}
}
