package eu.linksmart.gc.network.backbone.zmq;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.zeromq.ZMQ;

public class ZmqHandler {
	
	private static Logger LOG = Logger.getLogger(ZmqReceiver.class.getName());

	private String peerID = UUID.randomUUID().toString();
	private String xsubUri = "tcp://gando.fit.fraunhofer.de:6000";
	private String xpubUri = "tcp://gando.fit.fraunhofer.de:6001";
	
	private int heartBeatInterval = 2000;
	
	private HeartBeat heartBeat = null;
	private ZmqReceiver receiver = null;
	
	private ZMQ.Context pub_context = null;
	private ZMQ.Socket publisher = null;
	
	Map<String, String> remotePeers = Collections.synchronizedMap(new HashMap<String, String>());
	
	public ZmqHandler(String peerID) {
		this.peerID = peerID;
	}
	
	public ZmqHandler(String peerID, String xpubUri, String xsubUri) {
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
	
	public void publish(ZmqMessage zmqMessage) {
		publisher.sendMore(zmqMessage.getTopic());
		publisher.sendMore(new byte[]{zmqMessage.getProtocolVersion()});
		publisher.sendMore(new byte[]{zmqMessage.getType()});
		publisher.sendMore("" + zmqMessage.getTimeStamp());
		publisher.sendMore(zmqMessage.getSender());
		publisher.sendMore(zmqMessage.getRequestID());
		publisher.send(zmqMessage.getPayload(), 0);
	}
	
	public ZmqMessage createBroadcastMessage(byte[] payload) {
		return new ZmqMessage(ZmqConstants.BROADCAST_TOPIC, ZmqConstants.MESSAGE_TYPE_PEER_DISCOVERY, System.currentTimeMillis(), this.peerID, "request-id", payload);
	}
	
	public ZmqMessage createPeerMessage(String peerID, byte[] payload) {
		return new ZmqMessage(peerID, ZmqConstants.MESSAGE_TYPE_UNICAST, System.currentTimeMillis(), this.peerID, "request-id", payload);
	}
	
	public void notify(ZmqMessage zmqMessage) {
		if(zmqMessage.getTopic().equals(ZmqConstants.BROADCAST_TOPIC)) {
			if(zmqMessage.getType() == ZmqConstants.MESSAGE_TYPE_SERVICE_DISCOVERY) {
				processDiscovery(zmqMessage);
			} else if(zmqMessage.getType() == ZmqConstants.MESSAGE_TYPE_PEER_DOWN) {
				processPeerDown(zmqMessage);
			}
		} else if(zmqMessage.getTopic().equals(this.getPeerID())) {
			processMessage(zmqMessage);
		} else {
            LOG.warn("unknown topic detected.");
        }
	}
	
	private void processDiscovery(ZmqMessage zmqMessage) {
		LOG.info("recieved broadcast message from [" + zmqMessage.getSender() + "] - contents : " + new String(zmqMessage.getPayload()));
		if(!(zmqMessage.getSender().equals(this.peerID))) {
			remotePeers.put(zmqMessage.getSender(), "S1-VAD");
			LOG.info("adding peer [" + zmqMessage.getSender() + "] into list");
		}
	}
	
	private void processPeerDown(ZmqMessage zmqMessage) {
		LOG.info("recieved peerdown message from [" + zmqMessage.getSender() + "]");
		remotePeers.remove(zmqMessage.getSender());
		LOG.info("removing peer [" + zmqMessage.getSender() + "] from list");
	}
	
	private void processMessage(ZmqMessage zmqMessage) {
		LOG.info("recieved unicast message from [" + zmqMessage.getSender() + "] - contents : " + new String(zmqMessage.getPayload()));
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
