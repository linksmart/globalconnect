package eu.linksmart.gc.network.backbone.zmq;

import org.junit.Test;
import org.zeromq.ZMQ;

public class TestPeer2 {
	
	String peerID = "PEER-2";
	String xsub_uri = "tcp://gando.fit.fraunhofer.de:6000";
	String xpub_uri = "tcp://gando.fit.fraunhofer.de:6001";
	
	@Test
	public void testPeer2() {
		try {
			
			//
			// sending heartbeats
			//
			HeartBeat heartBeat = new HeartBeat(peerID, xsub_uri);
			heartBeat.start();
			
			//
			// prepare our context
			//
			ZMQ.Context context = ZMQ.context(1);
			
			//
			// sending (just one) broadcast message
			//
			ZMQ.Socket publisher = context.socket(ZMQ.PUB);
			publisher.connect(xsub_uri); 
			
			Thread.sleep(100);
	
			System.out.println("[" + peerID + "] is sending a broadcast message");
			publisher.sendMore(ZmqConstants.BROADCAST_TOPIC);
			publisher.sendMore(ZmqConstants.MESSAGE_TYPE_DISCOVERY);
			publisher.sendMore("" + System.currentTimeMillis());
			publisher.sendMore(peerID);
			publisher.send(peerID + "[S3,S4]", 0);
			
			//
			// prepare subscribers
			//
			ZMQ.Socket subscriber = context.socket(ZMQ.SUB);
			subscriber.connect(xpub_uri); 
			subscriber.subscribe(ZmqConstants.BROADCAST_TOPIC.getBytes());
			subscriber.subscribe(peerID.getBytes());
			
			System.out.println("[" + peerID + "] is subscribed to topic: BROADCAST");
			System.out.println("subscribed to topic: [" + peerID + "]");
			
			Thread.sleep(100);
			
			//
			// reading messages
			//
			int message_counter = 1;
			while (!Thread.currentThread().isInterrupted()) {
				String topic = subscriber.recvStr();
				String type = new String(subscriber.recvStr());
				String time = subscriber.recvStr();
				String sender = subscriber.recvStr();
				String contents = subscriber.recvStr();
				System.out.println("message_counter: " + message_counter);
				if(topic.equals(ZmqConstants.BROADCAST_TOPIC)) {
					System.out.println("recieved broadcast message from [" + sender + "] - topic: " + topic + " - type: " + type + " - time: " + time + " - contents : " + contents);
				} else
					System.out.println("topic: " + topic + " - type: " + type + " - time: " + time + " - sender: " + sender + " - contents : " + contents);
				message_counter++;
			}
			
			subscriber.setLinger(1000);
			subscriber.close ();
			context.term ();
			
			heartBeat.setIsRunning(false);
			
			System.out.println("[" + peerID + "] is going down");
			
		} catch (Exception e) {
			// TODO: handle exception
		}
	}
}
