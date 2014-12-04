package eu.linksmart.gc.network.backbone.zmq;

import org.junit.Test;
import org.zeromq.ZMQ;

public class TestPeer1 {
	
	String peerID = "PEER-1";
	
	@Test
	public void testPeer1() {
		
		try {
			
			ZMQ.Context context = ZMQ.context(1);
			ZMQ.Socket publisher = context.socket(ZMQ.PUB);
			publisher.connect("tcp://gando.fit.fraunhofer.de:6000"); // XSUB socket
			
			int message_counter = 1;
			boolean broadcasted = false;
			
			while (message_counter <= 3) {
				
				//
				// sending broadcast
				//
				if(!broadcasted) {
					System.out.println("broadcasting PEER-1");
					publisher.sendMore("BROADCAST");
					publisher.sendMore("0x02".getBytes());
					publisher.sendMore(("" + System.currentTimeMillis()).getBytes());
					publisher.sendMore(peerID);
					String contents = peerID + ":S1";
					publisher.send(contents.getBytes(), 0);
					broadcasted = true;
				}
				
				//
				// sending heartbeat
				//
				System.out.println("PEER-1 is sending hearbeat to proxy");
				publisher.sendMore("HEARTBEAT");
				publisher.sendMore("0x03");
				publisher.sendMore(("" + System.currentTimeMillis()).getBytes());
				publisher.sendMore(peerID);
				publisher.send("", 0);
				
				Thread.sleep(100);
				
				//
				// sending messages to other peer
				//
				System.out.println("sending message to peer-2: " + message_counter);
				publisher.sendMore("PEER-2");
				publisher.sendMore("0x01");
				publisher.sendMore("" + System.currentTimeMillis());
				publisher.sendMore(peerID);
				publisher.send(peerID + ":M-" + message_counter, 0);
				
				message_counter++;
				
				Thread.sleep(5000);
			}
			
			publisher.close();
			context.term();
			
			System.out.println("PEER-1 is going down");
			
		} catch (Exception e) {
			// TODO: handle exception
		}
	}
}
