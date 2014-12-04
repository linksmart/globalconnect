package eu.linksmart.gc.network.backbone.zmq;

import org.junit.Test;
import org.zeromq.ZMQ;

public class TestPeer1 {
	
	String peerID = "PEER-1";
	String xsub_uri = "tcp://gando.fit.fraunhofer.de:6000";
	
	@Test
	public void testPeer1() {
		
		try {
			
			//
			// sending heartbeat messages
			//
			HeartBeat heartBeat = new HeartBeat(peerID, xsub_uri);
			heartBeat.start();
			
			//
			// prepare publisher context
			//
			ZMQ.Context context = ZMQ.context(1);
			ZMQ.Socket publisher = context.socket(ZMQ.PUB);
			publisher.connect(xsub_uri); 
			
			Thread.sleep(100);
			
			//
			// sending broadcast message
			//
			System.out.println("broadcast from PEER-1");
			publisher.sendMore("BROADCAST");
			publisher.sendMore("0x02");
			publisher.sendMore("" + System.currentTimeMillis());
			publisher.sendMore(peerID);
			publisher.send(peerID + "[S1,S2]", 0);
				
			int message_counter = 1;
			
			while (message_counter <= 5) {
				
				//
				// sending messages to PEER-2
				//
				System.out.println("sending message to PEER-2: " + message_counter);
				publisher.sendMore("PEER-2");
				publisher.sendMore("0x01");
				publisher.sendMore("" + System.currentTimeMillis());
				publisher.sendMore(peerID);
				publisher.send(peerID + ":M-" + message_counter, 0);
				
				message_counter++;
				//Thread.sleep(1000);
			}
			
			publisher.setLinger(100);
			publisher.close();
			context.term();
			
			heartBeat.setIsRunning(false);
			
			System.out.println("PEER-1 is going down");
			
		} catch (Exception e) {
			// TODO: handle exception
		}
	}
}
