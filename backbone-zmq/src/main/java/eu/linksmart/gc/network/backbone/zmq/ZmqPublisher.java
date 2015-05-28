package eu.linksmart.gc.network.backbone.zmq;

import org.apache.log4j.Logger;
import org.zeromq.ZMQ;

import java.nio.ByteBuffer;

public class ZmqPublisher {
	
private static Logger LOG = Logger.getLogger(ZmqPublisher.class.getName());
	
	private ZmqHandler zmqHandler = null;
	
	private ZMQ.Context pub_context = null;
	private ZMQ.Socket publisher = null;
	
	public ZmqPublisher(ZmqHandler zmqHandler) {
		this.zmqHandler = zmqHandler;
	}
	
	public void startPublisher() {
		try {
			pub_context = ZMQ.context(1);
			publisher = pub_context.socket(ZMQ.PUB);
			publisher.connect(this.zmqHandler.getXSubUri());
		} catch (Exception e) {
			LOG.error("error in initializing ZmqPublisher: " + e.getMessage(), e);
		}	
	}
	
	public void stopPublisher() {
		try {
			if(pub_context != null && publisher != null) {
				publisher.setLinger(100);
				publisher.close();
				pub_context.term();
			}
		} catch (Exception e) {
			LOG.error("error in stopping ZmqPublisher: " + e.getMessage(), e);
		}
		
	}
	
	public boolean publish(ZmqMessage zmqMessage) {
		try {
			LOG.info("publishing topic: "+zmqMessage.getTopic());
			publisher.sendMore(zmqMessage.getTopic());
			LOG.info("publishing protocol version: " + zmqMessage.getProtocolVersion());
			publisher.sendMore(new byte[]{zmqMessage.getProtocolVersion()});
			LOG.info("publishing type: " + zmqMessage.getType());
			publisher.sendMore(new byte[]{zmqMessage.getType()});
			LOG.info("publishing timestamp: " + zmqMessage.getTimeStamp());
			publisher.sendMore(ByteBuffer.allocate(8).putLong(zmqMessage.getTimeStamp()).array());
			LOG.info("publishing sender: " + zmqMessage.getSender());
			publisher.sendMore(zmqMessage.getSender());
			LOG.info("publishing requestID: " + zmqMessage.getRequestID());
			publisher.sendMore(zmqMessage.getRequestID());
			LOG.info("publishing payload: "+zmqMessage.getPayload().toString());
			publisher.send(zmqMessage.getPayload(), 0);
			return true;
		} catch (Exception e) {
			LOG.error("error in publishing message: " + e.getMessage(), e);
		}
		return false;
	}

}
