package eu.linksmart.gc.network.backbone.zmq;

import org.junit.Test;

public class TestZmqHandler {
	
	String p1 = "P1";
	String p2 = "P2";
	
	@Test
	public void testHandler() {
		
		try {
			ZmqHandler peer1 = new ZmqHandler("P1");
			peer1.start();
			
			ZmqHandler peer2 = new ZmqHandler("P2");
			peer2.start();
			
			Thread.sleep(1000);
			
			peer1.publish(peer1.createBroadcastMessage("S1".getBytes()));
			peer2.publish(peer2.createBroadcastMessage("S2".getBytes()));
			
			Thread.sleep(3000);
			
			peer1.publish(peer1.createPeerMessage(peer2.getPeerID(), "M1".getBytes()));
			
			Thread.sleep(3000);
			
			peer1.stop();
			
			Thread.sleep(15000);
			
			peer2.stop();
				
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
		
}
