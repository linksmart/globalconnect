package eu.linksmart.gc.network.backbone.zmq;

import org.junit.Test;
import org.zeromq.ZMQ;

public class TestPeer2 {
	
	String peerID = "PEER-2";
	
	@Test
	public void testPeer2() {
		try {
			
			//
			// prepare our context
			//
			ZMQ.Context context = ZMQ.context(1);
			
			//
			// sending heartbeat
			//
			ZMQ.Socket publisher = context.socket(ZMQ.PUB);
			publisher.connect("tcp://gando.fit.fraunhofer.de:6000"); // XSUB socket
			System.out.println("PEER-2 is sending hearbeat to proxy");
			publisher.sendMore("HEARTBEAT");
			publisher.sendMore("0x03");
			publisher.sendMore(("" + System.currentTimeMillis()).getBytes());
			publisher.sendMore(peerID);
			publisher.send("", 0);
			
			//
			// prepare subscribers
			//
			ZMQ.Socket subscriber = context.socket(ZMQ.SUB);
			subscriber.connect("tcp://gando.fit.fraunhofer.de:6001"); // XPUB socket
			subscriber.subscribe("BROADCAST".getBytes());
			subscriber.subscribe(peerID.getBytes());
			
			System.out.println("subscribed to: BROADCAST");
			System.out.println("subscribed to: " + peerID);
			
			//
			// reading messages
			//
			int message_counter = 1;
			while (!Thread.currentThread ().isInterrupted()) {
				String topic = subscriber.recvStr();
				String type = new String(subscriber.recvStr());
				String time = subscriber.recvStr();
				String sender = subscriber.recvStr();
				String contents = subscriber.recvStr();
				System.out.println("receiving message_counter: " + message_counter);
				System.out.println("topic: " + topic + " - type: " + type + " - time: " + time + " - sender: " + sender + " - contents : " + contents);
				message_counter++;
			}
			subscriber.close ();
			context.term ();
		} catch (Exception e) {
			// TODO: handle exception
		}
	}	

}
