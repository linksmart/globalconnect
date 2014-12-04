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
			// sending heartbeat
			//
			HeartBeat heartBeat = new HeartBeat(peerID, xsub_uri);
			heartBeat.start();
			
			//
			// prepare context and subscribers
			//
			ZMQ.Context context = ZMQ.context(1);
			ZMQ.Socket subscriber = context.socket(ZMQ.SUB);
			subscriber.connect(xpub_uri); 
			subscriber.subscribe("BROADCAST".getBytes());
			subscriber.subscribe(peerID.getBytes());
			
			System.out.println("subscribed to: BROADCAST");
			System.out.println("subscribed to: " + peerID);
			
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
				System.out.println("receiving message_counter: " + message_counter);
				System.out.println("topic: " + topic + " - type: " + type + " - time: " + time + " - sender: " + sender + " - contents : " + contents);
				message_counter++;
			}
			
			subscriber.setLinger(1000);
			subscriber.close ();
			context.term ();
			
			heartBeat.setIsRunning(false);
			
			System.out.println("PEER-2 is going down");
			
		} catch (Exception e) {
			// TODO: handle exception
		}
	}
}
